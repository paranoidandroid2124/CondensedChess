package lila.llm.tools

import chess.Color
import chess.format.Fen

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.control.NonFatal

import akka.actor.ActorSystem
import play.api.libs.json.*
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient

import lila.llm.*
import lila.llm.analysis.{ CommentaryEngine, EarlyOpeningNarrationPolicy, NarrativeUtils, OpeningExplorerClient, StrategyPackSurface }
import lila.llm.model.OpeningReference
import lila.llm.model.{ FutureSnapshot, L1DeltaSnapshot, TargetsDelta }
import lila.llm.model.strategic.VariationLine

object RealPgnNarrativeEvalTruthTraceRunner:

  private val DefaultInventoryPath = CommentaryPlayerQcSupport.DefaultTruthInventoryPath
  private val DefaultDepth = 10
  private val DefaultMultiPv = 3
  private val OpeningRefMinPly = 3
  private val OpeningRefMaxPly = 24
  private val MaxProbeMoments = 3
  private val EngineEnvVars = List("LLM_ACTIVE_CORPUS_ENGINE_PATH", "STOCKFISH_BIN")
  private val TimestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm").withZone(ZoneOffset.UTC)

  final case class Config(
      inventoryPath: Path,
      outDir: Path,
      depth: Int,
      multiPv: Int,
      enginePath: Path,
      gameIds: Set[String],
      keys: Set[String],
      limit: Option[Int]
  )

  final case class TraceInventoryEntryReport(
      key: String,
      ply: Int,
      moveNumber: Int,
      side: String,
      tier: String,
      family: String,
      label: String,
      inventoryMomentType: String,
      inventoryMoveClassification: Option[String],
      cpLossVsPlayed: Option[Int],
      tags: List[String],
      notes: List[String],
      canonicalTracePresent: Boolean,
      witnessTracePresent: Boolean,
      canonicalTrace: Option[CommentaryEngine.TruthTraceMoment],
      witnessTrace: Option[CommentaryEngine.TruthTraceMoment]
  )
  object TraceInventoryEntryReport:
    given OFormat[TraceInventoryEntryReport] = Json.format[TraceInventoryEntryReport]

  final case class TraceGameReport(
      id: String,
      tier: String,
      family: String,
      label: String,
      usedProbeRefinement: Boolean,
      probeCandidateMoments: Int,
      probeExecutedRequests: Int,
      probeUnsupportedRequests: Int,
      anchorPlies: List[Int],
      candidateBridgePlies: List[Int],
      selectedBridgePlies: List[Int],
      finalInternalPlies: List[Int],
      visibleMomentPlies: List[Int],
      activeNotePlies: List[Int],
      promotedWholeGamePly: Option[Int],
      canonicalStateCounts: Map[String, Int],
      bottleneckCounts: Map[String, Int],
      entries: List[TraceInventoryEntryReport]
  )
  object TraceGameReport:
    given OFormat[TraceGameReport] = Json.format[TraceGameReport]

  final case class TraceBottleneckSummary(
      canonicalStateCounts: Map[String, Int],
      bottleneckCounts: Map[String, Int],
      bottleneckSamples: Map[String, List[String]],
      tagCounts: Map[String, Int],
      truthClassCounts: Map[String, Int],
      ownershipRoleCounts: Map[String, Int],
      exemplarRoleCounts: Map[String, Int]
  )
  object TraceBottleneckSummary:
    given OFormat[TraceBottleneckSummary] = Json.format[TraceBottleneckSummary]

  final case class TraceSummary(
      totalGames: Int,
      totalEntries: Int,
      gamesUsingProbeRefinement: Int,
      entriesWithCanonicalTrace: Int,
      entriesWithoutCanonicalTrace: Int,
      entriesWithWitnessTrace: Int,
      entriesWithoutWitnessTrace: Int,
      entriesCanonicallyVisible: Int,
      entriesCanonicallyExemplarVisible: Int,
      bottlenecks: TraceBottleneckSummary
  )
  object TraceSummary:
    given OFormat[TraceSummary] = Json.format[TraceSummary]

  final case class TraceRunReport(
      version: Int = 1,
      generatedAt: String,
      inventoryPath: String,
      depth: Int,
      multiPv: Int,
      enginePath: String,
      summary: TraceSummary,
      games: List[TraceGameReport]
  )
  object TraceRunReport:
    given OFormat[TraceRunReport] = Json.format[TraceRunReport]

  private final case class ProbeMomentBundle(
      ply: Int,
      requests: List[lila.llm.model.ProbeRequest]
  )

  private given OFormat[CommentaryEngine.TruthTraceMoment] = Json.format[CommentaryEngine.TruthTraceMoment]

  def main(args: Array[String]): Unit =
    given Executor = ExecutionContext.global
    given ActorSystem = ActorSystem("real-pgn-truth-trace-runner")

    val config = parseConfig(args.toList)
    val ws = new StandaloneAhcWSClient(new DefaultAsyncHttpClient())
    val api =
      LlmApi(
        openingExplorer = OpeningExplorerClient(ws),
        geminiClient = GeminiClient(ws, GeminiConfig.fromEnv),
        openAiClient = OpenAiClient(ws, OpenAiConfig.fromEnv),
        commentaryCache = CommentaryCache(),
        llmConfig = LlmConfig.fromEnv,
        providerConfig = LlmProviderConfig.fromEnv.copy(provider = "none")
      )
    val engine = new CommentaryPlayerQcSupport.LocalUciEngine(config.enginePath, timeoutMs = 30000L)

    try
      val inventory = readInventory(config.inventoryPath)
      val filteredEntries = filterEntries(inventory.entries, config)
      val entriesByGameId = filteredEntries.groupBy(_.gameId).toList.sortBy(_._1)
      val gamesById = inventory.games.map(game => game.id -> game).toMap
      val reports =
        entriesByGameId.map { case (gameId, entries) =>
          val game =
            gamesById.getOrElse(
              gameId,
              throw new IllegalArgumentException(s"missing inventory game for `$gameId`")
            )
          val report = runSingleGameTrace(game, entries, api, engine, config)
          writeJson(config.outDir.resolve("games").resolve(s"${game.id}.json"), Json.toJson(report))
          report
        }
      val report =
        TraceRunReport(
          generatedAt = Instant.now().toString,
          inventoryPath = config.inventoryPath.toAbsolutePath.normalize.toString,
          depth = config.depth,
          multiPv = config.multiPv,
          enginePath = config.enginePath.toAbsolutePath.normalize.toString,
          summary = buildSummary(reports),
          games = reports
        )
      writeJson(config.outDir.resolve("report.json"), Json.toJson(report))
      println(
        s"[truth-trace-runner] wrote `${config.outDir.resolve("report.json")}` (games=${reports.size}, entries=${report.summary.totalEntries})"
      )
    finally
      engine.close()
      ws.close()
      summon[ActorSystem].terminate()

  private def runSingleGameTrace(
      game: RealPgnNarrativeEvalRunner.CorpusGame,
      entries: List[RealPgnNarrativeEvalTruthInventoryBuilder.TruthInventoryEntry],
      api: LlmApi,
      engine: CommentaryPlayerQcSupport.LocalUciEngine,
      config: Config
  )(using Executor): TraceGameReport =
    val plyData =
      PgnAnalysisHelper.extractPlyDataStrict(game.pgn) match
        case Right(value) => value
        case Left(err)    => throw new IllegalArgumentException(s"${game.id}: PGN validation failed: $err")
    val afterMoveEvals = CommentaryPlayerQcSupport.buildAfterMoveEvals(plyData, engine, config.depth, config.multiPv)
    val evalMap = afterMoveEvals.map(eval => eval.ply -> eval.getVariations).toMap
    val openingRefsByFen = fetchOpeningRefsForPgn(api, game.pgn)
    val witnessSpecs =
      entries
        .sortBy(_.ply)
        .distinctBy(_.ply)
        .map(entry =>
          CommentaryEngine.DiagnosticWitness(
            ply = entry.ply,
            momentType = Some(entry.momentType),
            label = Some(entry.label)
          )
        )

    val initialDiagnostic =
      CommentaryEngine.generateGameArcDiagnostic(
        pgn = game.pgn,
        evals = evalMap,
        openingRefsByFen = openingRefsByFen,
        variantKey = EarlyOpeningNarrationPolicy.StandardVariant,
        diagnosticWitnesses = witnessSpecs
      )
    val initialResponse =
      GameChronicleResponse.fromGameArc(
        arc = initialDiagnostic.arc,
        sourceMode = "rule",
        model = None,
        planTier = PlanTier.Pro,
        llmLevel = LlmLevel.Active
      )
    val probeBundles = collectProbeMomentBundles(initialResponse, MaxProbeMoments)
    val (probeResultsByPly, unsupportedProbeCount, executedProbeCount) =
      executeSupportedProbeRequests(probeBundles, engine)
    val finalDiagnostic =
      if probeResultsByPly.isEmpty then initialDiagnostic
      else
        CommentaryEngine.generateGameArcDiagnostic(
          pgn = game.pgn,
          evals = evalMap,
          openingRefsByFen = openingRefsByFen,
          probeResultsByPly = probeResultsByPly,
          variantKey = EarlyOpeningNarrationPolicy.StandardVariant,
          diagnosticWitnesses = witnessSpecs
        )

    val canonicalByPly = finalDiagnostic.canonicalTraceMoments.map(trace => trace.ply -> trace).toMap
    val witnessByPly = finalDiagnostic.witnessTraceMoments.map(trace => trace.ply -> trace).toMap
    val entryReports =
      entries
        .sortBy(entry => (entry.ply, entry.key))
        .map { entry =>
          val canonicalTrace = canonicalByPly.get(entry.ply)
          val witnessTrace = witnessByPly.get(entry.ply)
          TraceInventoryEntryReport(
            key = entry.key,
            ply = entry.ply,
            moveNumber = entry.moveNumber,
            side = entry.side,
            tier = entry.tier,
            family = entry.family,
            label = entry.label,
            inventoryMomentType = entry.momentType,
            inventoryMoveClassification = entry.moveClassification,
            cpLossVsPlayed = entry.cpLossVsPlayed,
            tags = entry.tags,
            notes = entry.notes,
            canonicalTracePresent = canonicalTrace.nonEmpty,
            witnessTracePresent = witnessTrace.nonEmpty,
            canonicalTrace = canonicalTrace,
            witnessTrace = witnessTrace
          )
        }

    TraceGameReport(
      id = game.id,
      tier = game.tier,
      family = game.family,
      label = game.label,
      usedProbeRefinement = probeResultsByPly.nonEmpty,
      probeCandidateMoments = probeBundles.size,
      probeExecutedRequests = executedProbeCount,
      probeUnsupportedRequests = unsupportedProbeCount,
      anchorPlies = finalDiagnostic.anchorPlies,
      candidateBridgePlies = finalDiagnostic.candidateBridgePlies,
      selectedBridgePlies = finalDiagnostic.selectedBridgePlies,
      finalInternalPlies = finalDiagnostic.finalInternalPlies,
      visibleMomentPlies = finalDiagnostic.visibleMomentPlies,
      activeNotePlies = finalDiagnostic.activeNotePlies,
      promotedWholeGamePly = finalDiagnostic.promotedWholeGamePly,
      canonicalStateCounts = countMap(entryReports.map(canonicalState)),
      bottleneckCounts = countMap(entryReports.flatMap(bottleneckLabels)),
      entries = entryReports
    )

  private def buildSummary(reports: List[TraceGameReport]): TraceSummary =
    val entries = reports.flatMap(_.entries)
    val canonicalStateCounts = countMap(entries.map(canonicalState))
    val bottleneckCounts = countMap(entries.flatMap(bottleneckLabels))
    val bottleneckSamples =
      entries
        .flatMap(entry => bottleneckLabels(entry).map(label => label -> entry.key))
        .groupBy(_._1)
        .view
        .mapValues(_.map(_._2).distinct.sorted.take(5))
        .toMap
    TraceSummary(
      totalGames = reports.size,
      totalEntries = entries.size,
      gamesUsingProbeRefinement = reports.count(_.usedProbeRefinement),
      entriesWithCanonicalTrace = entries.count(_.canonicalTracePresent),
      entriesWithoutCanonicalTrace = entries.count(entry => !entry.canonicalTracePresent),
      entriesWithWitnessTrace = entries.count(_.witnessTracePresent),
      entriesWithoutWitnessTrace = entries.count(entry => !entry.witnessTracePresent),
      entriesCanonicallyVisible = entries.count(_.canonicalTrace.exists(_.visibleMoment)),
      entriesCanonicallyExemplarVisible =
        entries.count(_.canonicalTrace.exists(trace =>
          trace.visibleMoment &&
            (trace.exemplarRole == "VerifiedExemplar" || trace.exemplarRole == "ProvisionalExemplar")
        )),
      bottlenecks =
        TraceBottleneckSummary(
          canonicalStateCounts = canonicalStateCounts,
          bottleneckCounts = bottleneckCounts,
          bottleneckSamples = bottleneckSamples,
          tagCounts = countMap(entries.flatMap(_.tags)),
          truthClassCounts = countMap(entries.flatMap(traceForSummary(_).map(_.truthClass))),
          ownershipRoleCounts = countMap(entries.flatMap(traceForSummary(_).map(_.ownershipRole))),
          exemplarRoleCounts = countMap(entries.flatMap(traceForSummary(_).map(_.exemplarRole)))
        )
    )

  private def traceForSummary(
      entry: TraceInventoryEntryReport
  ): Option[CommentaryEngine.TruthTraceMoment] =
    entry.canonicalTrace.orElse(entry.witnessTrace)

  private def canonicalState(entry: TraceInventoryEntryReport): String =
    entry.canonicalTrace match
      case None => "no_canonical_trace"
      case Some(trace) if !trace.finalInternal && !trace.visibleMoment =>
        "canonical_non_internal"
      case Some(trace) if trace.finalInternal && !trace.visibleMoment =>
        "canonical_internal_hidden"
      case Some(trace) if trace.visibleMoment && trace.exemplarRole == "VerifiedExemplar" =>
        "canonical_verified_exemplar"
      case Some(trace) if trace.visibleMoment && trace.exemplarRole == "ProvisionalExemplar" =>
        "canonical_provisional_exemplar"
      case Some(trace) if trace.visibleMoment =>
        "canonical_visible_non_exemplar"
      case Some(_) =>
        "canonical_other"

  private def currentMaterialSeed(trace: CommentaryEngine.TruthTraceMoment): Boolean =
    trace.sacrificeKind.nonEmpty || trace.increasesDeficit || trace.valueDownCapture

  private def moveLocalAnchorCarrierPresent(trace: CommentaryEngine.TruthTraceMoment): Boolean =
    trace.rawDominantIdea.exists(_.trim.nonEmpty) ||
      trace.rawSecondaryIdea.exists(_.trim.nonEmpty) ||
      trace.rawExecution.exists(_.trim.nonEmpty) ||
      trace.rawObjective.exists(_.trim.nonEmpty) ||
      trace.rawFocus.exists(_.trim.nonEmpty) ||
      trace.rawLongTermFocus.exists(_.trim.nonEmpty) ||
      trace.rawDirectionalTargets.exists(_.trim.nonEmpty) ||
      trace.rawPieceRoutes.exists(_.trim.nonEmpty) ||
      trace.rawPieceMoveRefs.exists(_.trim.nonEmpty)

  private def usesLegacyShellOnly(trace: CommentaryEngine.TruthTraceMoment): Boolean =
    trace.legacyVisibleOnly ||
      (trace.evidenceProvenance.nonEmpty &&
        trace.evidenceProvenance.forall(_ == "LegacyShell"))

  private def bottleneckLabels(entry: TraceInventoryEntryReport): List[String] =
    val canonical = entry.canonicalTrace
    val witness = entry.witnessTrace
    val trace = canonical.orElse(witness)
    val labels = scala.collection.mutable.ListBuffer.empty[String]

    if canonical.isEmpty then labels += "no_canonical_trace"
    if canonical.isEmpty && witness.nonEmpty then labels += "witness_only_trace"

    trace.foreach { moment =>
      if !currentMaterialSeed(moment) then labels += "no_current_material_seed"
      if !moveLocalAnchorCarrierPresent(moment) then labels += "no_move_local_anchor"
      if currentMaterialSeed(moment) && !moveLocalAnchorCarrierPresent(moment) then
        labels += "current_material_without_anchor"
      if moveLocalAnchorCarrierPresent(moment) && !currentMaterialSeed(moment) then
        labels += "anchor_without_current_material"
      if usesLegacyShellOnly(moment) then labels += "legacy_visible_only"
      if moment.ownershipRole == "MaintenanceEcho" then labels += "maintenance_echo"
      if moment.maintenanceExemplarCandidate then labels += "maintenance_exemplar_candidate"
      if !moment.ownerEligible then labels += "owner_not_eligible"
      if moment.finalInternal && !moment.visibleMoment then labels += "canonical_hidden_after_internal"
      if moment.visibleMoment &&
        moment.exemplarRole != "VerifiedExemplar" &&
        moment.exemplarRole != "ProvisionalExemplar"
      then labels += "visible_non_exemplar"
      if moment.truthClass == "TensionPeak" || moment.truthClass == "Neutral" then
        labels += "high_cp_loss_unresolved_truth_class"
      if moment.currentMoveEvidence && !moment.freshCommitmentCandidate then
        if moment.ownershipRole == "MaintenanceEcho" && moment.currentConcreteCarrier then
          labels += "maintenance_current_carrier_without_fresh_commitment"
        else labels += "current_evidence_without_fresh_commitment"
      if moment.freshCommitmentCandidate && !moment.ownerEligible then
        labels += "fresh_commitment_blocked_before_owner"
      if moment.ownershipRole == "BlunderOwner" && !moment.failureInterpretationAllowed then
        labels += "failure_no_clear_plan"
    }

    labels.toList.distinct

  private def countMap(values: List[String]): Map[String, Int] =
    values.groupBy(identity).view.mapValues(_.size).toMap

  private def fetchOpeningRefsForPgn(api: LlmApi, pgn: String)(using Executor): Map[String, OpeningReference] =
    val openingFens =
      PgnAnalysisHelper.extractPlyData(pgn) match
        case Left(_) => Nil
        case Right(plyData) =>
          plyData
            .collect { case pd if pd.ply >= OpeningRefMinPly && pd.ply <= OpeningRefMaxPly => pd.fen }
            .distinct

    if openingFens.isEmpty then Map.empty
    else
      Await.result(
        Future
          .sequence(openingFens.map(fen => api.fetchOpeningMasters(fen).map(refOpt => fen -> refOpt)))
          .map(_.collect { case (fen, Some(ref)) => fen -> ref }.toMap)
          .recover { case NonFatal(_) => Map.empty[String, OpeningReference] },
        180.seconds
      )

  private def collectProbeMomentBundles(
      response: GameChronicleResponse,
      maxMoments: Int
  ): List[ProbeMomentBundle] =
    val seen = scala.collection.mutable.Set.empty[String]
    response.moments
      .sortBy(probeMomentRank)
      .flatMap { moment =>
        val requests =
          if moment.probeRefinementRequests.nonEmpty then moment.probeRefinementRequests
          else moment.probeRequests
        requests
          .find { request =>
            val key = probeDedupKey(request)
            if seen.contains(key) then false
            else
              seen += key
              true
          }
          .map(req => ProbeMomentBundle(moment.ply, List(req)))
      }
      .take(maxMoments)

  private def probeMomentRank(moment: GameChronicleMoment): (Int, Int, Int, Int, Int) =
    val surface = StrategyPackSurface.from(moment.strategyPack)
    val quietCompensation = surface.strictCompensationPosition && surface.quietCompensationPosition
    val durableCompensation = surface.strictCompensationPosition && surface.durableCompensationPosition
    val compensation =
      surface.strictCompensationPosition ||
        moment.signalDigest.exists(_.compensation.exists(_.trim.nonEmpty))
    val strategicCarrier =
      compensation ||
        surface.dominantIdeaText.nonEmpty ||
        surface.executionText.nonEmpty ||
        surface.objectiveText.nonEmpty ||
        moment.signalDigest.exists(_.dominantIdeaKind.isDefined) ||
        moment.strategyPack.exists(pack =>
          pack.strategicIdeas.nonEmpty || pack.longTermFocus.nonEmpty || pack.pieceRoutes.nonEmpty
        )
    val priority =
      if quietCompensation && moment.strategicBranch then 0
      else if durableCompensation && moment.strategicBranch then 1
      else if compensation && moment.strategicBranch then 2
      else if moment.strategicBranch && strategicCarrier then 3
      else if quietCompensation then 4
      else if moment.selectionKind == "key" && strategicCarrier then 5
      else if compensation then 6
      else if strategicCarrier then 7
      else 8
    val salience =
      moment.strategicSalience match
        case Some(level) if level.equalsIgnoreCase("High")   => 0
        case Some(level) if level.equalsIgnoreCase("Medium") => 1
        case _                                               => 2
    val selectionKind =
      if moment.selectionKind == "key" then 0
      else if moment.selectionKind == "thread_bridge" then 1
      else 2
    val refinementBias = if moment.probeRefinementRequests.nonEmpty then 0 else 1
    (priority, salience, selectionKind, refinementBias, moment.ply)

  private def probeDedupKey(request: lila.llm.model.ProbeRequest): String =
    List(
      request.purpose.getOrElse(""),
      request.fen,
      request.moves.mkString(","),
      request.requiredSignals.sorted.mkString(","),
      request.planId.getOrElse(""),
      request.seedId.getOrElse("")
    ).mkString("|")

  private def executeSupportedProbeRequests(
      bundles: List[ProbeMomentBundle],
      engine: CommentaryPlayerQcSupport.LocalUciEngine
  ): (Map[Int, List[lila.llm.model.ProbeResult]], Int, Int) =
    val supportedSignals = Set("replyPvs", "keyMotifs", "l1Delta", "futureSnapshot")
    var unsupported = 0
    val byPly = scala.collection.mutable.Map.empty[Int, List[lila.llm.model.ProbeResult]].withDefaultValue(Nil)

    bundles.foreach { bundle =>
      bundle.requests.foreach { request =>
        val required = request.requiredSignals.toSet
        if required.subsetOf(supportedSignals) then
          val afterFen = NarrativeUtils.uciListToFen(request.fen, request.moves)
          val multiPv = request.multiPv.getOrElse(2).max(1)
          val vars = engine.analyze(afterFen, request.depth.max(8), multiPv)
          vars.headOption.foreach { best =>
            val deltaVsBaseline = best.scoreCp - request.baselineEvalCp.getOrElse(best.scoreCp)
            val moverColor = PgnAnalysisHelper.sideToMoveFromFen(request.fen).getOrElse(Color.White)
            val moverLoss = moverLossCp(moverColor, deltaVsBaseline)
            val keyMotifs =
              if required.contains("keyMotifs") then synthesizeKeyMotifs(request, best, moverLoss) else Nil
            val l1Delta =
              if required.contains("l1Delta") then
                synthesizeL1Delta(request.fen, afterFen, moverLoss, best)
              else None
            val futureSnapshot =
              if required.contains("futureSnapshot") then
                synthesizeFutureSnapshot(request, afterFen, best, moverLoss, keyMotifs)
              else None
            val result =
              lila.llm.model.ProbeResult(
                id = request.id,
                fen = Some(request.fen),
                evalCp = best.scoreCp,
                bestReplyPv = best.moves,
                replyPvs = Some(vars.map(_.moves)),
                deltaVsBaseline = deltaVsBaseline,
                keyMotifs = keyMotifs,
                purpose = request.purpose,
                questionId = request.questionId,
                questionKind = request.questionKind,
                probedMove = request.moves.headOption,
                mate = best.mate,
                depth = Some(best.depth),
                l1Delta = l1Delta,
                futureSnapshot = futureSnapshot,
                objective = request.objective,
                seedId = request.seedId
              )
            byPly.update(bundle.ply, byPly(bundle.ply) :+ result)
          }
        else unsupported += 1
      }
    }

    (byPly.toMap.view.mapValues(_.take(1)).toMap, unsupported, byPly.values.map(_.size).sum)

  private def moverLossCp(moverColor: Color, deltaVsBaseline: Int): Int =
    if moverColor.white then -deltaVsBaseline else deltaVsBaseline

  private def synthesizeKeyMotifs(
      request: lila.llm.model.ProbeRequest,
      best: VariationLine,
      moverLoss: Int
  ): List[String] =
    val raw =
      List(
        request.id,
        request.planId.getOrElse(""),
        request.planName.getOrElse(""),
        request.objective.getOrElse(""),
        request.purpose.getOrElse("")
      ).mkString(" ").toLowerCase
    val motifs = scala.collection.mutable.ListBuffer.empty[String]
    if raw.contains("counterplay") then motifs += "counterplay"
    if raw.contains("break") then motifs += "counter_break"
    if raw.contains("pawnstorm") || raw.contains("rook_pawn_march") || raw.contains("hook") then motifs += "pawnstorm"
    if raw.contains("simplification") || raw.contains("convert") || raw.contains("endgame") then motifs += "conversion"
    if raw.contains("weakpawnattack") || raw.contains("fixed_pawn") || raw.contains("weakness") then motifs += "weakness_fixation"
    if raw.contains("pieceactivation") || raw.contains("piece_activation") || raw.contains("coordination") then motifs += "coordination"
    if raw.contains("prophylaxis") then motifs += "prophylaxis"
    if raw.contains("line") || raw.contains("file") || raw.contains("occupation") then motifs += "line_pressure"
    if raw.contains("kingattack") || raw.contains("king_attack") then motifs += "king_attack"
    if raw.contains("outpost") then motifs += "outpost"
    if best.mate.exists(_ < 0) then motifs += "Mate"
    else if moverLoss >= 150 then motifs += "Material"
    motifs.toList.distinct.take(4)

  private def synthesizeL1Delta(
      beforeFen: String,
      afterFen: String,
      moverLoss: Int,
      best: VariationLine
  ): Option[L1DeltaSnapshot] =
    val materialDelta = whiteMaterialScore(afterFen) - whiteMaterialScore(beforeFen)
    val mobilityDelta = legalMoveCount(afterFen) - legalMoveCount(beforeFen)
    val kingSafetyDelta =
      if best.mate.exists(_ < 0) then -80
      else if positionInCheck(afterFen) then -35
      else 0
    val collapseReason =
      if best.mate.exists(_ < 0) then Some("King exposed")
      else if moverLoss >= 180 then Some("Lost initiative")
      else if materialDelta <= -100 then Some("Material deficit deepens")
      else if mobilityDelta <= -10 then Some("Piece activity stalls")
      else None
    Some(
      L1DeltaSnapshot(
        materialDelta = materialDelta,
        kingSafetyDelta = kingSafetyDelta,
        centerControlDelta = 0,
        openFilesDelta = 0,
        mobilityDelta = mobilityDelta,
        collapseReason = collapseReason
      )
    )

  private def synthesizeFutureSnapshot(
      request: lila.llm.model.ProbeRequest,
      afterFen: String,
      best: VariationLine,
      moverLoss: Int,
      keyMotifs: List[String]
  ): Option[FutureSnapshot] =
    val supportive = moverLoss <= 80 && !best.mate.exists(_ < 0)
    val planHints = planProgressHints(request, keyMotifs)
    val blockersRemoved =
      if supportive && keyMotifs.contains("counterplay") then List("counterplay")
      else if supportive && keyMotifs.contains("line_pressure") then List("line access")
      else Nil
    val prereqsMet =
      if supportive then planHints.take(2)
      else Nil
    val strategicAdded =
      if supportive then
        keyMotifs
          .filter(m => Set("line_pressure", "weakness_fixation", "pawnstorm", "conversion", "coordination").contains(m))
          .map(_.replace('_', ' '))
          .take(2)
      else Nil
    val tacticalAdded =
      if supportive then request.moves.takeRight(1).map(NarrativeUtils.uciToSanOrFormat(afterFen, _))
      else Nil
    val newThreatKinds =
      if best.mate.exists(_ < 0) then List("Mate")
      else if moverLoss >= 150 then List("Material")
      else Nil
    val resolvedThreatKinds =
      if supportive && blockersRemoved.nonEmpty then List("Counterplay")
      else Nil
    val positive =
      prereqsMet.nonEmpty || blockersRemoved.nonEmpty || strategicAdded.nonEmpty || resolvedThreatKinds.nonEmpty || newThreatKinds.nonEmpty
    Option.when(positive) {
      FutureSnapshot(
        resolvedThreatKinds = resolvedThreatKinds,
        newThreatKinds = newThreatKinds,
        targetsDelta =
          TargetsDelta(
            tacticalAdded = tacticalAdded,
            tacticalRemoved = Nil,
            strategicAdded = strategicAdded,
            strategicRemoved = if !supportive && moverLoss >= 150 then List("initiative") else Nil
          ),
        planBlockersRemoved = blockersRemoved,
        planPrereqsMet = prereqsMet
      )
    }

  private def planProgressHints(request: lila.llm.model.ProbeRequest, keyMotifs: List[String]): List[String] =
    val raw =
      List(request.planName, request.objective, request.purpose, request.planId).flatten.mkString(" ").toLowerCase
    val hints = scala.collection.mutable.ListBuffer.empty[String]
    if keyMotifs.contains("line_pressure") || raw.contains("line") then hints += "line pressure lane secured"
    if keyMotifs.contains("weakness_fixation") || raw.contains("weak") then hints += "target fixation is maintained"
    if keyMotifs.contains("pawnstorm") then hints += "attack lane remains available"
    if keyMotifs.contains("conversion") || raw.contains("convert") then hints += "conversion window remains open"
    if keyMotifs.contains("coordination") then hints += "piece coordination is improved"
    if hints.isEmpty && raw.nonEmpty then hints += "strategic trajectory remains intact"
    hints.toList.distinct

  private def whiteMaterialScore(fen: String): Int =
    fen
      .takeWhile(_ != ' ')
      .foldLeft(0) {
        case (acc, ch) =>
          acc + (ch match
            case 'P' => 100
            case 'N' => 320
            case 'B' => 330
            case 'R' => 500
            case 'Q' => 900
            case 'p' => -100
            case 'n' => -320
            case 'b' => -330
            case 'r' => -500
            case 'q' => -900
            case _   => 0)
      }

  private def legalMoveCount(fen: String): Int =
    Fen.read(chess.variant.Standard, Fen.Full(fen)).map(_.legalMoves.toList.size).getOrElse(0)

  private def positionInCheck(fen: String): Boolean =
    Fen.read(chess.variant.Standard, Fen.Full(fen)).exists(_.check.yes)

  private def readInventory(
      path: Path
  ): RealPgnNarrativeEvalTruthInventoryBuilder.TruthInventory =
    val absolute = path.toAbsolutePath.normalize
    val raw = Files.readString(absolute, StandardCharsets.UTF_8)
    Json
      .parse(raw)
      .validate[RealPgnNarrativeEvalTruthInventoryBuilder.TruthInventory]
      .fold(
        err => throw new IllegalArgumentException(s"invalid truth inventory `${absolute}`: $err"),
        identity
      )

  private def filterEntries(
      entries: List[RealPgnNarrativeEvalTruthInventoryBuilder.TruthInventoryEntry],
      config: Config
  ): List[RealPgnNarrativeEvalTruthInventoryBuilder.TruthInventoryEntry] =
    entries
      .filter(entry => config.gameIds.isEmpty || config.gameIds.contains(entry.gameId))
      .filter(entry => config.keys.isEmpty || config.keys.contains(entry.key))
      .sortBy(entry => (entry.gameId, entry.ply, entry.key))
      .pipe(list => config.limit.fold(list)(limit => list.take(limit)))

  private def parseConfig(args: List[String]): Config =
    val now = Instant.now()
    val defaultOutDir =
      CommentaryPlayerQcSupport.DefaultManifestDir.resolve(s"truth_trace_inventory_${TimestampFormatter.format(now)}")
    val enginePath =
      optionString(args, "--engine")
        .orElse(EngineEnvVars.view.flatMap(name => sys.env.get(name).map(_.trim).filter(_.nonEmpty)).headOption)
        .map(Paths.get(_))
        .getOrElse {
          System.err.println("[truth-trace-runner] missing engine path. Pass `--engine /path/to/uci-engine`.")
          sys.exit(1)
        }
    Config(
      inventoryPath =
        optionString(args, "--inventory")
          .map(Paths.get(_))
          .getOrElse(resolveDefaultInventoryPath())
          .toAbsolutePath
          .normalize,
      outDir =
        optionString(args, "--out")
          .map(Paths.get(_))
          .getOrElse(defaultOutDir)
          .toAbsolutePath
          .normalize,
      depth = optionInt(args, "--depth").getOrElse(DefaultDepth).max(8),
      multiPv = optionInt(args, "--multi-pv").orElse(optionInt(args, "--multiPv")).getOrElse(DefaultMultiPv).max(1),
      enginePath = enginePath.toAbsolutePath.normalize,
      gameIds = optionStrings(args, "--game").toSet,
      keys = optionStrings(args, "--key").toSet,
      limit = optionInt(args, "--limit").filter(_ > 0)
    )

  private def resolveDefaultInventoryPath(): Path =
    CommentaryPlayerQcSupport.TruthInventoryLookupPaths
      .map(_.toAbsolutePath.normalize)
      .find(Files.isRegularFile(_))
      .getOrElse(DefaultInventoryPath.toAbsolutePath.normalize)

  private def optionString(args: List[String], flag: String): Option[String] =
    args.sliding(2).collectFirst { case List(`flag`, value) => value.trim }.filter(_.nonEmpty)

  private def optionStrings(args: List[String], flag: String): List[String] =
    args
      .sliding(2)
      .collect { case List(`flag`, value) => value.trim }
      .filter(_.nonEmpty)
      .toList

  private def optionInt(args: List[String], flag: String): Option[Int] =
    optionString(args, flag).flatMap(value => value.toIntOption)

  private def writeJson(path: Path, value: JsValue): Unit =
    Files.createDirectories(path.getParent)
    Files.writeString(path, Json.prettyPrint(value), StandardCharsets.UTF_8)

  extension [A](value: A)
    private def pipe[B](f: A => B): B = f(value)
