package lila.commentary.tools.moveReview

import java.nio.file.{ Files, Path, Paths }

import scala.concurrent.{ Await, ExecutionContext }
import akka.actor.ActorSystem
import play.api.libs.json.Json
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient

import lila.commentary.*
import lila.commentary.analysis.OpeningExplorerClient
import lila.commentary.tools.review.CommentaryPlayerQcSupport
import lila.commentary.tools.quality.CommentaryQualitySupport

object MoveReviewCorpusRunner:

  import CommentaryPlayerQcSupport.*

  final case class Config(
      manifestPath: Path = DefaultManifestDir.resolve("slice_manifest.jsonl"),
      outPath: Path = DefaultMoveReviewRunDir.resolve("move_review_outputs.jsonl"),
      rawDir: Path = DefaultMoveReviewRunDir.resolve("raw"),
      catalogPath: Path = DefaultCatalogDir.resolve("catalog.jsonl"),
      depth: Int = 10,
      multiPv: Int = 3,
      enginePath: Path
  )

  def main(args: Array[String]): Unit =
    given Executor = ExecutionContext.global
    given ActorSystem = ActorSystem("move-review-corpus-runner")

    val config = parseConfig(args.toList)
    val entries =
      readJsonLines[SliceManifestEntry](config.manifestPath) match
        case Right(value) => value.filter(_.surface == "moveReview")
        case Left(err) =>
          System.err.println(s"[move-review-corpus] failed to read manifest `${config.manifestPath}`: $err")
          sys.exit(1)

    if entries.isEmpty then
      System.err.println(s"[move-review-corpus] no moveReview entries in `${config.manifestPath}`")
      sys.exit(1)

    ensureDir(config.rawDir)
    val manifestCatalog = entries.map(loadCatalogEntry).distinctBy(_.gameKey)
    val externalCatalog =
      readJsonLines[CatalogEntry](config.catalogPath) match
        case Right(value) if value.nonEmpty =>
          println(s"[move-review-corpus] loaded opening catalog `${config.catalogPath}` (games=${value.size})")
          value
        case Right(_) =>
          println(s"[move-review-corpus] opening catalog `${config.catalogPath}` is empty; using manifest PGNs only")
          Nil
        case Left(err) =>
          println(s"[move-review-corpus] opening catalog unavailable `${config.catalogPath}`: $err; using manifest PGNs only")
          Nil
    val openingCatalog = (manifestCatalog ++ externalCatalog).distinctBy(_.gameKey)
    val openingIndex = buildOpeningIndex(openingCatalog)
    val catalogByGameKey = openingCatalog.map(entry => entry.gameKey -> entry).toMap
    val engine = new LocalUciEngine(config.enginePath)
    val ws = new StandaloneAhcWSClient(new DefaultAsyncHttpClient())
    val api =
      CommentaryApi(
        openingExplorer = OpeningExplorerClient(ws),
        geminiClient = GeminiClient(ws, GeminiConfig.fromEnv),
        openAiClient = OpenAiClient(ws, OpenAiConfig.fromEnv),
        commentaryCache = CommentaryCache(),
        commentaryConfig = CommentaryConfig.fromEnv,
        providerConfig = AiProviderConfig.fromEnv
      )

    try
      val outputs =
        entries.map { entry =>
          val catalogEntry = catalogByGameKey.getOrElse(entry.gameKey, loadCatalogEntry(entry))
          val openingReference = corpusBackedOpeningReference(catalogEntry, openingIndex, maxSampleGames = 5)
          val openingReferenceSampleGameIds =
            openingReference.toList.flatMap(_.sampleGames.map(_.id))
          val openingReferencePeerSampleIds =
            openingReferenceSampleGameIds.filterNot(_ == catalogEntry.gameKey)
          val pgn = Files.readString(Paths.get(entry.pgnPath))
          val plyData = PgnAnalysisHelper.extractPlyDataStrict(pgn) match
            case Right(value) =>
              value.find(_.ply == entry.targetPly).getOrElse {
                throw new IllegalArgumentException(s"${entry.sampleId}: missing ply ${entry.targetPly}")
              }
            case Left(err) =>
              throw new IllegalArgumentException(s"${entry.sampleId}: invalid PGN: $err")

          engine.newGame()
          val beforeVars = engine.analyze(entry.fen, config.depth, config.multiPv)
          val afterFen = lila.commentary.analysis.NarrativeUtils.uciListToFen(entry.fen, List(entry.playedUci))
          engine.newGame()
          val afterVars = engine.analyze(afterFen, config.depth, config.multiPv)
          val afterBest = afterVars.headOption
          val analyzedPly =
            analyzePlyWithOpeningRef(
              entry = catalogEntry,
              plyData = plyData,
              beforeVariations = beforeVars,
              afterEval = afterBest.map(best =>
                MoveEval(
                  ply = entry.targetPly,
                  cp = best.scoreCp,
                  mate = best.mate,
                  pv = best.moves,
                  variations = afterVars
                )
              ),
              openingRef = openingReference
            )
          val runtimeTrace = analyzedPly.map(moveReviewPlannerRuntime)
          val plannerTrace = runtimeTrace.map(_.planner).getOrElse(MoveReviewPlannerTrace())
          val quietSupportTrace = runtimeTrace.map(_.quietSupport).getOrElse(MoveReviewQuietSupportTrace())
          val coverageTrace = runtimeTrace.map(_.coverage).getOrElse(MoveReviewCoverageDiagnostics.Result.empty)
          val digestHashes =
            analyzedPly
            .map(CommentaryQualitySupport.moveReviewDigests)
            .getOrElse(CommentaryQualitySupport.SurfaceDigestHashes())
          val result =
            Await.result(
              api.moveReviewPosition(
                fen = entry.fen,
                lastMove = Some(entry.playedUci),
                eval = beforeVars.headOption.map(v => EvalData(cp = v.scoreCp, mate = v.mate, pv = Some(v.moves))),
                variations = Some(beforeVars),
                probeResults = None,
                openingData = openingReference,
                afterFen = Some(afterFen),
                afterEval = afterBest.map(v => EvalData(cp = v.scoreCp, mate = v.mate, pv = Some(v.moves))),
                afterVariations = Some(afterVars),
                opening = entry.opening.orElse(catalogEntry.opening),
                phase = guessPhase(entry.fen, entry.targetPly),
                ply = entry.targetPly,
                variant = Some(entry.variant),
                prevStateToken = None,
                prevEndgameStateToken = None,
                allowAiPolish = true,
                lang = "en",
                planTier = PlanTier.Pro
              ),
              180.seconds
            ).getOrElse(throw new IllegalStateException(s"${entry.sampleId}: empty moveReview response"))

          val rawPath = config.rawDir.resolve(s"${entry.sampleId.replace(':', '_')}.json")
          writeJson(rawPath, Json.toJson[CommentResponse](result.response))
          val (supportRows, advancedRows) = buildMoveReviewRows(result.response)

          MoveReviewOutputEntry(
            sampleId = entry.sampleId,
            gameKey = entry.gameKey,
            sliceKind = entry.sliceKind,
            targetPly = entry.targetPly,
            fen = entry.fen,
            playedSan = entry.playedSan,
            playedUci = entry.playedUci,
            opening = entry.opening.orElse(catalogEntry.opening),
            openingReferenceTotalGames = openingReference.map(_.totalGames),
            openingReferenceSampleGameIds = openingReferenceSampleGameIds,
            openingReferencePeerSampleIds = openingReferencePeerSampleIds,
            commentary = result.response.commentary,
            supportRows = supportRows,
            advancedRows = advancedRows,
            sourceMode = result.response.sourceMode,
            model = result.response.model,
            rawResponsePath = rawPath.toAbsolutePath.normalize.toString,
            variationCount = result.response.variations.size,
            cacheHit = result.cacheHit,
            plannerPrimaryKind = plannerTrace.primaryKind,
            plannerPrimaryFallbackMode = plannerTrace.primaryFallbackMode,
            plannerSecondaryKind = plannerTrace.secondaryKind,
            plannerSecondarySurfaced = plannerTrace.secondarySurfaced,
            moveReviewFallbackMode = plannerTrace.moveReviewFallbackMode,
            plannerSceneType = plannerTrace.sceneType,
            plannerSceneReasons = plannerTrace.sceneReasons,
            plannerOwnerCandidates = plannerTrace.ownerCandidates,
            plannerAdmittedOwners = plannerTrace.admittedOwners,
            plannerDroppedOwners = plannerTrace.droppedOwners,
            plannerSupportMaterialSeparation = plannerTrace.supportMaterialSeparation,
            plannerProposedOwnerMappings = plannerTrace.proposedOwnerMappings,
            plannerDemotionReasons = plannerTrace.demotionReasons,
            plannerSelectedQuestion = plannerTrace.selectedQuestion,
            plannerSelectedOwnerKind = plannerTrace.selectedOwnerKind,
            plannerSelectedSource = plannerTrace.selectedSource,
            rawChoiceType = plannerTrace.rawChoiceType,
            rawDecisionPresent = plannerTrace.rawDecisionPresent,
            rawDecisionIngressReason = plannerTrace.rawDecisionIngressReason,
            rawPvDeltaAvailable = plannerTrace.rawPvDeltaAvailable,
            rawPvDeltaIngressReason = plannerTrace.rawPvDeltaIngressReason,
            rawPvDeltaResolvedThreatsPresent = plannerTrace.rawPvDeltaResolvedThreatsPresent,
            rawPvDeltaNewOpportunitiesPresent = plannerTrace.rawPvDeltaNewOpportunitiesPresent,
            rawPvDeltaPlanAdvancementsPresent = plannerTrace.rawPvDeltaPlanAdvancementsPresent,
            rawPvDeltaConcessionsPresent = plannerTrace.rawPvDeltaConcessionsPresent,
            sanitizedDecisionPresent = plannerTrace.sanitizedDecisionPresent,
            sanitizedDecisionIngressReason = plannerTrace.sanitizedDecisionIngressReason,
            sanitizedPvDeltaAvailable = plannerTrace.sanitizedPvDeltaAvailable,
            sanitizedPvDeltaIngressReason = plannerTrace.sanitizedPvDeltaIngressReason,
            truthClass = plannerTrace.truthClass,
            truthReasonFamily = plannerTrace.truthReasonFamily,
            truthFailureMode = plannerTrace.truthFailureMode,
            truthChosenMatchesBest = plannerTrace.truthChosenMatchesBest,
            truthOnlyMoveDefense = plannerTrace.truthOnlyMoveDefense,
            truthBenchmarkCriticalMove = plannerTrace.truthBenchmarkCriticalMove,
            plannerTacticalFailureSources = plannerTrace.tacticalFailureSources,
            plannerForcingDefenseSources = plannerTrace.forcingDefenseSources,
            plannerMoveDeltaSources = plannerTrace.moveDeltaSources,
            surfaceReplayOutcome = plannerTrace.surfaceReplayOutcome,
            contrast_source_kind = plannerTrace.contrastSourceKind,
            contrast_anchor = plannerTrace.contrastAnchor,
            contrast_consequence = plannerTrace.contrastConsequence,
            contrast_admissible = Some(plannerTrace.contrastAdmissible),
            contrast_reject_reason = plannerTrace.contrastRejectReason,
            contrast_replacement_used = Some(plannerTrace.contrastReplacementUsed),
            moveReviewCausalClaimStatus = plannerTrace.causalClaimStatus,
            moveReviewCausalClaimQuestion = plannerTrace.causalClaimQuestion,
            moveReviewCausalClaimSubject = plannerTrace.causalClaimSubject,
            moveReviewCausalClaimEvidence = plannerTrace.causalClaimEvidence,
            moveReviewCausalClaimRelations = plannerTrace.causalClaimRelations,
            moveReviewCausalClaimRejectReasons = plannerTrace.causalClaimRejectReasons,
            moveReviewCausalClaimSupportEmbedded = plannerTrace.causalClaimSupportEmbedded,
            moveReviewCausalClaimGuardrail = plannerTrace.causalClaimGuardrail,
            moveReviewSnapshotDigestHash = digestHashes.snapshotDigestHash,
            moveReviewCarryDigestHash = digestHashes.carryDigestHash,
            moveReviewAugmentationDigestHash = digestHashes.augmentationDigestHash,
            moveReviewBundleDigestHash = digestHashes.bundleDigestHash,
            moveReviewSourceKind = coverageTrace.moveReviewSourceKind,
            basicEvidenceStatus = coverageTrace.basicEvidenceStatus,
            basicEvidenceRejectReasons = coverageTrace.basicEvidenceRejectReasons,
            supportedLocalCandidateFamilies = coverageTrace.supportedLocalCandidateFamilies,
            supportedLocalAdmittedFamilies = coverageTrace.supportedLocalAdmittedFamilies,
            supportedLocalRejectReasons = coverageTrace.supportedLocalRejectReasons,
            moveReviewLocalFactStatus = coverageTrace.moveReviewLocalFactStatus,
            moveReviewLocalFactFamilies = coverageTrace.moveReviewLocalFactFamilies,
            moveReviewLocalFactAuthorities = coverageTrace.moveReviewLocalFactAuthorities,
            moveReviewLocalFactStrictFallbackEligible = coverageTrace.moveReviewLocalFactStrictFallbackEligible,
            moveReviewLocalFactRejectReasons = coverageTrace.moveReviewLocalFactRejectReasons
          ).withQuietSupportTrace(quietSupportTrace)
        }

      writeJsonLines(config.outPath, outputs)
      println(s"[move-review-corpus] wrote `${config.outPath}` (samples=${outputs.size})")
    finally
      engine.close()
      ws.close()
      summon[ActorSystem].terminate()

  private def loadCatalogEntry(entry: SliceManifestEntry): CatalogEntry =
    val pgn = Files.readString(Paths.get(entry.pgnPath))
    val headers = parseHeaders(pgn)
    val totalPlies = PgnAnalysisHelper.extractPlyDataStrict(pgn).toOption.map(_.size).getOrElse(0)
    CatalogEntry(
      gameKey = entry.gameKey,
      source = entry.pgnPath,
      sourceId = entry.gameKey,
      pgnPath = entry.pgnPath,
      mixBucket = entry.mixBucket.getOrElse(MixBucket.Club),
      familyTags = entry.tags,
      ratingBucket = ratingBucketOf(headers),
      timeControlBucket = timeControlBucketOf(headers),
      openingMacroFamily = openingMacroFamily(headers),
      opening = openingLabel(headers),
      eco = headers.get("ECO"),
      variation = headers.get("Variation"),
      white = headers.get("White"),
      black = headers.get("Black"),
      event = headers.get("Event"),
      date = headers.get("Date"),
      round = headers.get("Round"),
      result = headers.get("Result"),
      timeControl = headers.get("TimeControl"),
      variant = headers.getOrElse("Variant", entry.variant),
      totalPlies = totalPlies
    )

  private def parseConfig(args: List[String]): Config =
    val positional = positionalArgs(args)
    val enginePath =
      optionString(args, "--engine")
        .orElse(sys.env.get("AI_ACTIVE_CORPUS_ENGINE_PATH").map(_.trim).filter(_.nonEmpty))
        .orElse(sys.env.get("STOCKFISH_BIN").map(_.trim).filter(_.nonEmpty))
        .map(Paths.get(_))
        .getOrElse {
          System.err.println("[move-review-corpus] missing engine path. Pass `--engine /path/to/uci-engine`.")
          sys.exit(1)
        }
    Config(
      manifestPath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultManifestDir.resolve("slice_manifest.jsonl")),
      outPath = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultMoveReviewRunDir.resolve("move_review_outputs.jsonl")),
      rawDir = positional.lift(2).map(Paths.get(_)).getOrElse(DefaultMoveReviewRunDir.resolve("raw")),
      catalogPath = optionString(args, "--catalog").map(Paths.get(_)).getOrElse(DefaultCatalogDir.resolve("catalog.jsonl")),
      depth = optionInt(args, "--depth").getOrElse(10).max(8),
      multiPv = optionInt(args, "--multi-pv").orElse(optionInt(args, "--multiPv")).getOrElse(3).max(1),
      enginePath = enginePath
    )

  private def positionalArgs(args: List[String]): List[String] =
    val optionsWithValue = Set("--engine", "--catalog", "--depth", "--multi-pv", "--multiPv")
    val out = scala.collection.mutable.ListBuffer.empty[String]
    var idx = 0
    while idx < args.length do
      val current = args(idx)
      if current.startsWith("--") then idx += (if optionsWithValue.contains(current) then 2 else 1)
      else
        out += current
        idx += 1
    out.toList

  private def optionString(args: List[String], name: String): Option[String] =
    args.sliding(2).collectFirst { case List(flag, value) if flag == name => value.trim }.filter(_.nonEmpty)

  private def optionInt(args: List[String], name: String): Option[Int] =
    optionString(args, name).flatMap(_.toIntOption)
