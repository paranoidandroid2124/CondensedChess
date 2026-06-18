package lila.commentary.tools.moveReview

import java.nio.file.{ Files, Path, Paths }

import scala.concurrent.{ Await, ExecutionContext }
import akka.actor.ActorSystem
import play.api.libs.json.{ Format, Json }
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient

import lila.commentary.*
import lila.commentary.analysis.OpeningExplorerClient
import lila.commentary.model.strategic.VariationLine
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
      multiPv: Int = 2,
      enginePath: Option[Path],
      engineCachePath: Option[Path] = None,
      cacheOnly: Boolean = false,
      timings: Boolean = false
  )

  private final case class EngineCacheEntry(
      fen: String,
      depth: Int,
      multiPv: Int,
      engineName: String,
      variations: List[VariationLine]
  )
  private object EngineCacheEntry:
    given Format[EngineCacheEntry] = Json.format[EngineCacheEntry]

  private final case class EngineCacheFullKey(fen: String, depth: Int, multiPv: Int, engineName: String)
  private final case class EngineCacheLookupKey(fen: String, depth: Int, multiPv: Int)

  private final class EngineCache private (
      path: Path,
      initialEntries: List[EngineCacheEntry],
      cacheOnly: Boolean
  ):
    private val entriesByFullKey =
      scala.collection.mutable.Map.from(
        initialEntries.map(entry => fullKey(entry) -> entry)
      )
    private val entriesByLookupKey =
      scala.collection.mutable.Map.from(
        entriesByFullKey.values.toList.groupMap(lookupKey)(identity)
      )
    private val pendingEntries = scala.collection.mutable.ListBuffer.empty[EngineCacheEntry]
    private var hitCount = 0
    private var missCount = 0

    def resolve(
        sampleId: String,
        site: String,
        fen: String,
        depth: Int,
        multiPv: Int,
        engine: Option[LocalUciEngine]
    ): List[VariationLine] =
      if cacheOnly then cacheOnlyResolve(sampleId, site, fen, depth, multiPv)
      else
        val activeEngine =
          engine.getOrElse(throw new IllegalStateException("engine cache fill requires a live engine"))
        val key = EngineCacheFullKey(fen, depth, multiPv, activeEngine.engineName)
        entriesByFullKey.get(key) match
          case Some(entry) =>
            hitCount += 1
            entry.variations
          case None =>
            missCount += 1
            activeEngine.newGame()
            val variations = activeEngine.analyze(fen, depth, multiPv)
            val entry =
              EngineCacheEntry(
                fen = fen,
                depth = depth,
                multiPv = multiPv,
                engineName = activeEngine.engineName,
                variations = variations
            )
            entriesByFullKey.update(key, entry)
            entriesByLookupKey.update(
              lookupKey(entry),
              entriesByLookupKey.getOrElse(lookupKey(entry), Nil).filterNot(_.engineName == entry.engineName) :+ entry
            )
            pendingEntries += entry
            variations

    def writeIfNeeded(): Unit =
      if pendingEntries.nonEmpty then
        writeJsonLines(path, entriesByFullKey.values.toList.sortBy(entry =>
          (entry.fen, entry.depth, entry.multiPv, entry.engineName)
        ))

    def printSummary(): Unit =
      println(
        s"[move-review-corpus] engine cache `${path}` cacheOnly=$cacheOnly hits=$hitCount misses=$missCount written=${pendingEntries.size}"
      )

    private def cacheOnlyResolve(
        sampleId: String,
        site: String,
        fen: String,
        depth: Int,
        multiPv: Int
    ): List[VariationLine] =
      val matches =
        entriesByLookupKey.getOrElse(EngineCacheLookupKey(fen, depth, multiPv), Nil)
      val engineNames = matches.map(_.engineName).distinct.sorted
      if matches.isEmpty then
        missCount += 1
        throw new IllegalStateException(
          s"$sampleId:$site cache miss for fen=`$fen` depth=$depth multiPv=$multiPv"
        )
      else if engineNames.size > 1 then
        throw new IllegalStateException(
          s"$sampleId:$site ambiguous engine cache for fen=`$fen` depth=$depth multiPv=$multiPv engines=${engineNames.mkString(",")}"
        )
      else
        hitCount += 1
        matches.last.variations

    private def fullKey(entry: EngineCacheEntry): EngineCacheFullKey =
      EngineCacheFullKey(entry.fen, entry.depth, entry.multiPv, entry.engineName)

    private def lookupKey(entry: EngineCacheEntry): EngineCacheLookupKey =
      EngineCacheLookupKey(entry.fen, entry.depth, entry.multiPv)

  private object EngineCache:
    def load(path: Path, cacheOnly: Boolean): EngineCache =
      if !Files.exists(path) then
        if cacheOnly then
          System.err.println(s"[move-review-corpus] missing engine cache `${path.toAbsolutePath.normalize}`")
          sys.exit(1)
        else new EngineCache(path, Nil, cacheOnly = false)
      else
        readJsonLines[EngineCacheEntry](path) match
          case Right(entries) => new EngineCache(path, entries, cacheOnly)
          case Left(err) =>
            System.err.println(s"[move-review-corpus] failed to read engine cache `${path}`: $err")
            sys.exit(1)

  private final case class PgnMetadata(
      headers: Map[String, String],
      strictPlyData: Either[String, List[PgnAnalysisHelper.PlyData]],
      plyDataByPly: Map[Int, PgnAnalysisHelper.PlyData]
  )

  private final case class ReplayAnalysisKey(
      fen: String,
      playedUci: String,
      playedSan: String,
      targetPly: Int,
      opening: Option[String],
      variant: String,
      beforeVariations: List[VariationLine],
      afterVariations: List[VariationLine],
      openingReferenceJson: Option[String]
  )

  private final case class ReplayAnalysisArtifacts(
      runtimeTrace: Option[MoveReviewRuntimeTrace],
      digestHashes: CommentaryQualitySupport.SurfaceDigestHashes
  )

  private final class PgnMetadataCache(timings: RunTimings):
    private val byPath = scala.collection.mutable.Map.empty[String, PgnMetadata]

    def metadata(entry: SliceManifestEntry): PgnMetadata =
      byPath.getOrElseUpdate(
        entry.pgnPath,
        timings.time("pgn_metadata_load") {
          val pgn = Files.readString(Paths.get(entry.pgnPath))
          val strictPlyData = PgnAnalysisHelper.extractPlyDataStrict(pgn)
          PgnMetadata(
            headers = parseHeaders(pgn),
            strictPlyData = strictPlyData,
            plyDataByPly = strictPlyData.toOption.map(_.map(ply => ply.ply -> ply).toMap).getOrElse(Map.empty)
          )
        }
      )

    def targetPly(entry: SliceManifestEntry): PgnAnalysisHelper.PlyData =
      metadata(entry).strictPlyData match
        case Right(_) =>
          metadata(entry).plyDataByPly.getOrElse(
            entry.targetPly,
            throw new IllegalArgumentException(s"${entry.sampleId}: missing ply ${entry.targetPly}")
          )
        case Left(err) =>
          throw new IllegalArgumentException(s"${entry.sampleId}: invalid PGN: $err")

  private final class RunTimings(enabled: Boolean):
    private val totals = scala.collection.mutable.LinkedHashMap.empty[String, Long]
    private val counts = scala.collection.mutable.LinkedHashMap.empty[String, Long]

    def time[A](stage: String)(body: => A): A =
      if !enabled then body
      else
        val start = System.nanoTime()
        try body
        finally
          val elapsed = ((System.nanoTime() - start) / 1000000L).max(0L)
          totals.update(stage, totals.getOrElse(stage, 0L) + elapsed)
          counts.update(stage, counts.getOrElse(stage, 0L) + 1L)

    def printSummary(): Unit =
      if enabled then
        println("[move-review-corpus] stage timings:")
        totals.foreach { case (stage, totalMs) =>
          val count = counts.getOrElse(stage, 0L).max(1L)
          val avg = totalMs.toDouble / count.toDouble
          println(f"[move-review-corpus]   $stage%-28s total=${totalMs}%dms count=${count}%d avg=${avg}%.1fms")
        }

  def main(args: Array[String]): Unit =
    given Executor = ExecutionContext.global
    given ActorSystem = ActorSystem("move-review-corpus-runner")

    val config = parseConfig(args.toList)
    val timings = new RunTimings(config.timings)
    val entries =
      timings.time("read_manifest") {
        readJsonLines[SliceManifestEntry](config.manifestPath)
      } match
        case Right(value) => value.filter(_.surface == "moveReview")
        case Left(err) =>
          System.err.println(s"[move-review-corpus] failed to read manifest `${config.manifestPath}`: $err")
          sys.exit(1)

    if entries.isEmpty then
      System.err.println(s"[move-review-corpus] no moveReview entries in `${config.manifestPath}`")
      sys.exit(1)

    ensureDir(config.rawDir)
    val pgnMetadataCache = new PgnMetadataCache(timings)
    val manifestCatalog = timings.time("manifest_catalog") {
      entries.distinctBy(_.gameKey).map(entry => loadCatalogEntry(entry, pgnMetadataCache.metadata(entry)))
    }
    val externalCatalog =
      timings.time("read_opening_catalog") {
        readJsonLines[CatalogEntry](config.catalogPath)
      } match
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
    val explorerGameByGameKey =
      scala.collection.mutable.Map.empty[String, Option[lila.commentary.model.ExplorerGame]]
    def cachedExplorerGame(entry: CatalogEntry): Option[lila.commentary.model.ExplorerGame] =
      explorerGameByGameKey.getOrElseUpdate(entry.gameKey, catalogEntryToExplorerGame(entry))
    val openingReferenceByGameKey =
      scala.collection.mutable.Map.empty[String, Option[lila.commentary.model.OpeningReference]]
    val replayAnalysisByKey =
      scala.collection.mutable.Map.empty[ReplayAnalysisKey, ReplayAnalysisArtifacts]
    var replayAnalysisCacheHits = 0
    var replayAnalysisCacheMisses = 0
    val apiStageTotals = scala.collection.mutable.LinkedHashMap.empty[String, Long]
    val apiStageCounts = scala.collection.mutable.LinkedHashMap.empty[String, Long]
    val engineCache = config.engineCachePath.map(path => EngineCache.load(path, config.cacheOnly))
    val engine = config.enginePath.map(path => new LocalUciEngine(path))
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
          val catalogEntry = timings.time("catalog_entry") {
            catalogByGameKey.getOrElse(entry.gameKey, loadCatalogEntry(entry, pgnMetadataCache.metadata(entry)))
          }
          val openingReference = timings.time("opening_reference") {
            openingReferenceByGameKey.getOrElseUpdate(
              catalogEntry.gameKey,
              corpusBackedOpeningReference(catalogEntry, openingIndex, maxSampleGames = 5, cachedExplorerGame)
            )
          }
          val openingReferenceSampleGameIds =
            openingReference.toList.flatMap(_.sampleGames.map(_.id))
          val openingReferencePeerSampleIds =
            openingReferenceSampleGameIds.filterNot(_ == catalogEntry.gameKey)
          val beforeVars = timings.time("engine_before") {
            analyzeWithOptionalCache(engineCache, engine, config, entry.sampleId, "before", entry.fen)
          }
          val afterFen = timings.time("after_fen") {
            lila.commentary.analysis.NarrativeUtils.uciListToFen(entry.fen, List(entry.playedUci))
          }
          val afterVars = timings.time("engine_after") {
            analyzeWithOptionalCache(engineCache, engine, config, entry.sampleId, "after", afterFen)
          }
          val afterBest = afterVars.headOption
          val replayAnalysisKey =
            ReplayAnalysisKey(
              fen = entry.fen,
              playedUci = entry.playedUci,
              playedSan = entry.playedSan,
              targetPly = entry.targetPly,
              opening = entry.opening.orElse(catalogEntry.opening),
              variant = entry.variant,
              beforeVariations = beforeVars,
              afterVariations = afterVars,
              openingReferenceJson = openingReference.map(ref => Json.stringify(Json.toJson(ref)))
            )
          val result =
            timings.time("api_move_review") {
              Await.result(
                api.moveReviewPositionWithRuntimeDiagnostics(
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
            }
          result.runtime.foreach { diagnostics =>
            diagnostics.stageTimingsMs.foreach { case (stage, ms) =>
              apiStageTotals.update(stage, apiStageTotals.getOrElse(stage, 0L) + ms)
              apiStageCounts.update(stage, apiStageCounts.getOrElse(stage, 0L) + 1L)
            }
          }
          val replayAnalysis =
            replayAnalysisByKey.get(replayAnalysisKey) match
              case Some(cached) =>
                replayAnalysisCacheHits += 1
                cached
              case None =>
                replayAnalysisCacheMisses += 1
                val plyData = timings.time("pgn_target_ply") {
                  pgnMetadataCache.targetPly(entry)
                }
                val analyzedPly =
                  timings.time("analyze_ply") {
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
                  }
                val runtimeTrace = timings.time("planner_runtime_trace") {
                  analyzedPly.map(snapshot => moveReviewPlannerRuntime(snapshot, result.runtime))
                }
                val digestHashes =
                  timings.time("digest_hashes") {
                    analyzedPly
                      .map(CommentaryQualitySupport.moveReviewDigests)
                      .getOrElse(CommentaryQualitySupport.SurfaceDigestHashes())
                  }
                val artifacts = ReplayAnalysisArtifacts(runtimeTrace, digestHashes)
                replayAnalysisByKey.update(replayAnalysisKey, artifacts)
                artifacts
          val runtimeTrace = replayAnalysis.runtimeTrace
          val plannerTrace = runtimeTrace.map(_.planner).getOrElse(MoveReviewPlannerTrace())
          val quietSupportTrace = runtimeTrace.map(_.quietSupport).getOrElse(MoveReviewQuietSupportTrace())
          val coverageTrace = runtimeTrace.map(_.coverage).getOrElse(MoveReviewCoverageDiagnostics.Result.empty)
          val digestHashes = replayAnalysis.digestHashes
          val response = result.result.response

          val rawPath = config.rawDir.resolve(s"${entry.sampleId.replace(':', '_')}.json")
          timings.time("write_raw_response") {
            writeJson(rawPath, Json.toJson[CommentResponse](response))
          }
          val (supportRows, advancedRows) = timings.time("build_output_rows") {
            buildMoveReviewRows(response)
          }
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
            commentary = response.commentary,
            supportRows = supportRows,
            advancedRows = advancedRows,
            sourceMode = response.sourceMode,
            model = response.model,
            rawResponsePath = rawPath.toAbsolutePath.normalize.toString,
            variationCount = response.variations.size,
            cacheHit = result.result.cacheHit,
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
            plannerCandidateEvidenceLines = plannerTrace.candidateEvidenceLines,
            plannerPvCoupledPlanNames = plannerTrace.pvCoupledPlanNames,
            plannerPvCoupledPlanSupport = plannerTrace.pvCoupledPlanSupport,
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
            plannerConcreteTacticalSources = plannerTrace.concreteTacticalSources,
            plannerLineConsequenceSources = plannerTrace.lineConsequenceSources,
            plannerAlternativeComparisonSources = plannerTrace.alternativeComparisonSources,
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
            moveReviewCausalClaimEvidenceSources = plannerTrace.causalClaimEvidenceSources,
            moveReviewCausalClaimEvidenceSubjects = plannerTrace.causalClaimEvidenceSubjects,
            moveReviewCausalClaimLineBindings = plannerTrace.causalClaimLineBindings,
            moveReviewCausalClaimRelations = plannerTrace.causalClaimRelations,
            moveReviewCausalFrameIntent = plannerTrace.causalFrameIntent,
            moveReviewCausalFrameRoles = plannerTrace.causalFrameRoles,
            moveReviewCausalFrameSurfaceContract = plannerTrace.causalFrameSurfaceContract,
            moveReviewCausalClaimRejectReasons = plannerTrace.causalClaimRejectReasons,
            moveReviewCausalClaimSupportEmbedded = plannerTrace.causalClaimSupportEmbedded,
            moveReviewCausalClaimGuardrail = plannerTrace.causalClaimGuardrail,
            moveReviewCausalClaimLocalFactFamily = plannerTrace.causalClaimLocalFactFamily,
            moveReviewCausalClaimLocalFactAuthority = plannerTrace.causalClaimLocalFactAuthority,
            moveReviewCausalClaimLocalFactProducer = plannerTrace.causalClaimLocalFactProducer,
            moveReviewCausalClaimLocalFactEvidenceRefs = plannerTrace.causalClaimLocalFactEvidenceRefs,
            moveReviewCausalClaimLocalFactStrictFallbackEligible = plannerTrace.causalClaimLocalFactStrictFallbackEligible,
            moveReviewCausalClaimLocalFactGuardrails = plannerTrace.causalClaimLocalFactGuardrails,
            moveReviewCausalClaimLocalFactRejectReasons = plannerTrace.causalClaimLocalFactRejectReasons,
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
            moveReviewLocalFactProducers = coverageTrace.moveReviewLocalFactProducers,
            moveReviewLocalFactEvidenceRefs = coverageTrace.moveReviewLocalFactEvidenceRefs,
            moveReviewLocalFactStrictFallbackEligible = coverageTrace.moveReviewLocalFactStrictFallbackEligible,
            moveReviewLocalFactRejectReasons = coverageTrace.moveReviewLocalFactRejectReasons
          ).withQuietSupportTrace(quietSupportTrace)
        }

      timings.time("write_outputs") {
        writeJsonLines(config.outPath, outputs)
      }
      println(s"[move-review-corpus] wrote `${config.outPath}` (samples=${outputs.size})")
    finally
      println(
        s"[move-review-corpus] replay analysis cache hits=$replayAnalysisCacheHits misses=$replayAnalysisCacheMisses"
      )
      if apiStageTotals.nonEmpty then
        println("[move-review-corpus] API stage timings:")
        apiStageTotals.foreach { case (stage, totalMs) =>
          val count = apiStageCounts.getOrElse(stage, 1L).max(1L)
          val avg = totalMs.toDouble / count.toDouble
          println(f"[move-review-corpus]   $stage%-28s total=${totalMs}%dms count=${count}%d avg=${avg}%.1fms")
        }
      engineCache.foreach(_.writeIfNeeded())
      engineCache.foreach(_.printSummary())
      timings.printSummary()
      engine.foreach(_.close())
      ws.close()
      summon[ActorSystem].terminate()

  private def analyzeWithOptionalCache(
      engineCache: Option[EngineCache],
      engine: Option[LocalUciEngine],
      config: Config,
      sampleId: String,
      site: String,
      fen: String
  ): List[VariationLine] =
    engineCache match
      case Some(cache) =>
        cache.resolve(sampleId, site, fen, config.depth, config.multiPv, engine)
      case None =>
        val activeEngine =
          engine.getOrElse(throw new IllegalStateException("move-review corpus run requires an engine"))
        activeEngine.newGame()
        activeEngine.analyze(fen, config.depth, config.multiPv)

  private def loadCatalogEntry(entry: SliceManifestEntry, metadata: PgnMetadata): CatalogEntry =
    val headers = metadata.headers
    val totalPlies = metadata.strictPlyData.toOption.map(_.size).getOrElse(0)
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
    val cacheOnly = args.contains("--cache-only")
    val engineCachePath = optionString(args, "--engine-cache").map(Paths.get(_))
    if cacheOnly && engineCachePath.isEmpty then
      System.err.println("[move-review-corpus] --cache-only requires `--engine-cache <path>`.")
      sys.exit(1)
    val resolvedEnginePath =
      optionString(args, "--engine")
        .orElse(sys.env.get("AI_ACTIVE_CORPUS_ENGINE_PATH").map(_.trim).filter(_.nonEmpty))
        .orElse(sys.env.get("STOCKFISH_BIN").map(_.trim).filter(_.nonEmpty))
        .map(Paths.get(_))
    val enginePath =
      if cacheOnly then None
      else
        Some(resolvedEnginePath.getOrElse {
          System.err.println("[move-review-corpus] missing engine path. Pass `--engine /path/to/uci-engine`.")
          sys.exit(1)
        })
    Config(
      manifestPath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultManifestDir.resolve("slice_manifest.jsonl")),
      outPath = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultMoveReviewRunDir.resolve("move_review_outputs.jsonl")),
      rawDir = positional.lift(2).map(Paths.get(_)).getOrElse(DefaultMoveReviewRunDir.resolve("raw")),
      catalogPath = optionString(args, "--catalog").map(Paths.get(_)).getOrElse(DefaultCatalogDir.resolve("catalog.jsonl")),
      depth = optionInt(args, "--depth").getOrElse(10).max(8),
      multiPv = optionInt(args, "--multi-pv").orElse(optionInt(args, "--multiPv")).getOrElse(2).max(1),
      enginePath = enginePath,
      engineCachePath = engineCachePath,
      cacheOnly = cacheOnly,
      timings = args.contains("--timings")
    )

  private def positionalArgs(args: List[String]): List[String] =
    val optionsWithValue = Set("--engine", "--catalog", "--depth", "--multi-pv", "--multiPv", "--engine-cache")
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
