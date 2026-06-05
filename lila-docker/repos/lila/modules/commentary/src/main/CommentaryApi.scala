package lila.commentary

import scala.concurrent.Future
import scala.annotation.unused
import java.util.concurrent.atomic.AtomicLong
import java.time.Instant
import lila.commentary.analysis.{ AuthoringEvidenceSummaryBuilder, MoveReviewCompressionPolicy, MoveReviewPlayerPayloadBuilder, MoveReviewPolishSlots, MoveReviewProseContract, MoveReviewPvLine, MoveReviewSoftRepair, MoveReviewStrategicLedgerBuilder, MoveReviewSupportedLocalSurfaceRows, BookStyleRenderer, CommentaryEngine, CommentaryOpsBoard, CommentaryOpsSignals, CommentaryPayloadNormalizer, DecisiveTruth, EarlyOpeningNarrationPolicy, FullGameDraftNormalizer, LineScopedCitation, LiveNarrativeCompressionCore, NarrativeContextBuilder, NarrativeDedupCore, NarrativeUtils, OpeningExplorerClient, PlanEvidenceEvaluator, ProbePurposeClassifier, StrategicSignalMatcher, StrategyPackBuilder, StrategyPackSurface, PlayerProseBoundary, UserFacingSignalSanitizer }
import lila.commentary.model.OpeningReference
import lila.commentary.model.structure.StructureId
import lila.commentary.model.strategic.{ VariationLine, TheoreticalOutcomeHint }

/** Pipeline: CommentaryEngine → BookStyleRenderer (rule-based only). */
final class CommentaryApi(
    openingExplorer: OpeningExplorerClient,
    geminiClient: GeminiClient,
    openAiClient: OpenAiClient,
    commentaryCache: CommentaryCache,
    commentaryConfig: CommentaryConfig = CommentaryConfig.fromEnv,
    providerConfig: AiProviderConfig = AiProviderConfig.fromEnv
)(using Executor):

  private val logger = lila.log("commentary.api")
  private val OpeningRefMinPly = 3
  private val OpeningRefMaxPly = 24
  private val moveReviewRequests = new AtomicLong(0L)
  private val tokenPresentCount = new AtomicLong(0L)
  private val tokenEmitCount = new AtomicLong(0L)
  private val continuityAppliedCount = new AtomicLong(0L)
  private val stateAwareCacheHitCount = new AtomicLong(0L)
  private val stateAwareCacheMissCount = new AtomicLong(0L)
  private val samePlyIdempotentHitCount = new AtomicLong(0L)
  private val transitionCountByType = scala.collection.concurrent.TrieMap.empty[String, AtomicLong]
  private val structureProfileCount = new AtomicLong(0L)
  private val structureUnknownCount = new AtomicLong(0L)
  private val structureLowConfidenceCount = new AtomicLong(0L)
  private val structureCountByType = scala.collection.concurrent.TrieMap.empty[String, AtomicLong]
  private val alignmentBandCount = scala.collection.concurrent.TrieMap.empty[String, AtomicLong]
  private val endgameFeatureCount = new AtomicLong(0L)
  private val endgameWinHintCount = new AtomicLong(0L)
  private val endgameHighConfidenceCount = new AtomicLong(0L)
  private val planRecallTotal = new AtomicLong(0L)
  private val planRecallHit = new AtomicLong(0L)
  private val planRecallHypothesisFirstTotal = new AtomicLong(0L)
  private val planRecallHypothesisFirstHit = new AtomicLong(0L)
  private val hypothesisProbeTotal = new AtomicLong(0L)
  private val hypothesisProbeHit = new AtomicLong(0L)
  private val contractProbeTotal = new AtomicLong(0L)
  private val contractProbeDropped = new AtomicLong(0L)
  private val claimCertificationObservedTotal = new AtomicLong(0L)
  private val claimCertificationBlockedStrongTotal = new AtomicLong(0L)
  private val claimCertificationDowngradedWeakTotal = new AtomicLong(0L)
  private val claimCertificationAttributionFailureTotal = new AtomicLong(0L)
  private val claimCertificationQuantifierFailureTotal = new AtomicLong(0L)
  private val claimCertificationStabilityFailureTotal = new AtomicLong(0L)
  private val ShadowWindowSize = 2000
  private val latencyWindowLock = new Object
  private var totalLatencyWindowMs = Vector.empty[Long]
  private var structureEvalLatencyWindowMs = Vector.empty[Long]
  private val leakTokens = List("PA_MATCH", "PRECOND_MISS", "REQ_", "SUP_", "BLK_")
  private val PolishWindowSize = 200
  private val PolishCircuitCooldownMs = 10L * 60L * 1000L
  private val SegmentPolishMinWords = 80
  private val SegmentPolishMinAllowedSans = 2
  private val polishWindowLock = new Object
  private var polishLatencyWindowMs = Vector.empty[Long]
  private var polishFallbackWindow = Vector.empty[Boolean]
  private var polishCircuitOpenUntilMs = 0L
  private val polishAttemptCount = new AtomicLong(0L)
  private val polishAcceptedCount = new AtomicLong(0L)
  private val polishFallbackCount = new AtomicLong(0L)
  private val polishCacheHitCount = new AtomicLong(0L)
  private val softRepairAppliedCount = new AtomicLong(0L)
  private val softRepairMaterialCount = new AtomicLong(0L)
  private val polishReasonCount = scala.collection.concurrent.TrieMap.empty[String, AtomicLong]
  private val polishEstimatedCostMicros = new AtomicLong(0L)
  private val moveReviewCompareObservedCount = new AtomicLong(0L)
  private val moveReviewCompareConsistentCount = new AtomicLong(0L)
  private val fullGameCompareObservedCount = new AtomicLong(0L)
  private val fullGameCompareConsistentCount = new AtomicLong(0L)
  private val fullGameSegmentRepairAttemptCount = new AtomicLong(0L)
  private val fullGameSegmentRepairBypassedCount = new AtomicLong(0L)
  private val fullGameSegmentSoftRepairAppliedCount = new AtomicLong(0L)
  private val fullGameMergedRetrySkippedCount = new AtomicLong(0L)
  private val fullGameInvalidReasonCount = scala.collection.concurrent.TrieMap.empty[String, AtomicLong]
  private val opsSampleCount = scala.collection.concurrent.TrieMap.empty[String, AtomicLong]
  private val commentaryOpsBoard = new CommentaryOpsBoard()

  private final case class PromptUsageStats(
      attempts: AtomicLong = new AtomicLong(0L),
      cacheHits: AtomicLong = new AtomicLong(0L),
      promptTokens: AtomicLong = new AtomicLong(0L),
      cachedTokens: AtomicLong = new AtomicLong(0L),
      completionTokens: AtomicLong = new AtomicLong(0L),
      estimatedCostMicros: AtomicLong = new AtomicLong(0L)
  )
  private val promptUsageByFamily = scala.collection.concurrent.TrieMap.empty[String, PromptUsageStats]

  private case class PolishDecision(
      commentary: String,
      sourceMode: String,
      model: Option[String],
      cacheHit: Boolean,
      meta: Option[MoveReviewPolishMeta],
      promptUsages: List[(String, OpenAiPolishResult)] = Nil
  )

  private def incTransition(kind: String): Unit =
    transitionCountByType.getOrElseUpdate(kind, new AtomicLong(0L)).incrementAndGet()

  private def incStructure(kind: String): Unit =
    structureCountByType.getOrElseUpdate(kind, new AtomicLong(0L)).incrementAndGet()

  private def incBand(kind: String): Unit =
    alignmentBandCount.getOrElseUpdate(kind, new AtomicLong(0L)).incrementAndGet()

  private def recordStructureMetrics(data: lila.commentary.model.ExtendedAnalysisData): Unit =
    data.structureProfile.foreach { profile =>
      structureProfileCount.incrementAndGet()
      incStructure(profile.primary.toString)
      if profile.primary == StructureId.Unknown then structureUnknownCount.incrementAndGet()
      if profile.confidence < commentaryConfig.structKbMinConfidence then structureLowConfidenceCount.incrementAndGet()
    }
    data.planAlignment.foreach(pa => incBand(pa.band.toString))

  private def recordEndgameMetrics(data: lila.commentary.model.ExtendedAnalysisData): Unit =
    data.endgameFeatures.foreach { eg =>
      endgameFeatureCount.incrementAndGet()
      if eg.theoreticalOutcomeHint == TheoreticalOutcomeHint.Win then endgameWinHintCount.incrementAndGet()
      if eg.confidence >= 0.75 then endgameHighConfidenceCount.incrementAndGet()
    }

  private def isHypothesisProbeRequest(req: lila.commentary.model.ProbeRequest): Boolean =
    req.seedId.exists(_.trim.nonEmpty) ||
      req.purpose.exists(ProbePurposeClassifier.isHypothesisPurpose)

  private def recordStrategicMetrics(
      data: lila.commentary.model.ExtendedAnalysisData,
      ctx: lila.commentary.model.NarrativeContext,
      probeResults: List[lila.commentary.model.ProbeResult],
      diagnosticPlanSidecar: PlanEvidenceEvaluator.DiagnosticPlanSidecar
  ): Unit =
    val topRulePlan = data.plans.headOption.map(_.plan.id.toString)
    val topRulePlansAt3 = data.plans.take(3).map(_.plan.id.toString).toSet
    val topHypothesis = ctx.mainStrategicPlans.headOption.map(_.planId)

    if topHypothesis.isDefined then
      planRecallTotal.incrementAndGet()
      if topRulePlan.isDefined && topRulePlan == topHypothesis then planRecallHit.incrementAndGet()
      planRecallHypothesisFirstTotal.incrementAndGet()
      if topHypothesis.exists(topRulePlansAt3.contains) then planRecallHypothesisFirstHit.incrementAndGet()

    val rootProbeResultsRaw = probeResults.filter(pr => pr.fen.forall(_ == data.fen))
    val validation = PlanEvidenceEvaluator.validateProbeResults(rootProbeResultsRaw, ctx.probeRequests)

    contractProbeTotal.addAndGet(rootProbeResultsRaw.size.toLong)
    contractProbeDropped.addAndGet(validation.droppedCount.toLong)

    val hypothesisRequests = ctx.probeRequests.filter(isHypothesisProbeRequest)
    if hypothesisRequests.nonEmpty then
      val validIds = validation.validResults.map(_.id).toSet
      hypothesisProbeTotal.addAndGet(hypothesisRequests.size.toLong)
      hypothesisProbeHit.addAndGet(hypothesisRequests.count(req => validIds.contains(req.id)).toLong)

    claimCertificationObservedTotal.addAndGet(diagnosticPlanSidecar.entries.size.toLong)
    claimCertificationBlockedStrongTotal.addAndGet(diagnosticPlanSidecar.blockedStrongClaims.toLong)
    claimCertificationDowngradedWeakTotal.addAndGet(diagnosticPlanSidecar.downgradedWeakClaims.toLong)
    claimCertificationAttributionFailureTotal.addAndGet(diagnosticPlanSidecar.attributionFailures.toLong)
    claimCertificationQuantifierFailureTotal.addAndGet(diagnosticPlanSidecar.quantifierFailures.toLong)
    claimCertificationStabilityFailureTotal.addAndGet(diagnosticPlanSidecar.stabilityFailures.toLong)

  private def pushWindow(values: Vector[Long], value: Long): Vector[Long] =
    val next = values :+ value
    if next.size <= ShadowWindowSize then next else next.takeRight(ShadowWindowSize)

  private def pushBoundedWindow[T](values: Vector[T], value: T, size: Int): Vector[T] =
    val next = values :+ value
    if next.size <= size then next else next.takeRight(size)

  private def percentile(values: Vector[Long], p: Double): Double =
    if values.isEmpty then 0.0
    else
      val sorted = values.sorted
      val idx = math.ceil(p * sorted.size.toDouble).toInt - 1
      sorted(idx.max(0).min(sorted.size - 1)).toDouble

  private def polishLatencyP95Ms: Double =
    polishWindowLock.synchronized {
      percentile(polishLatencyWindowMs, 0.95)
    }

  private def polishFallbackRate: Double =
    polishWindowLock.synchronized {
      if polishFallbackWindow.isEmpty then 0.0
      else polishFallbackWindow.count(identity).toDouble / polishFallbackWindow.size.toDouble
    }

  private def shouldOpenPolishCircuit: Boolean =
    polishWindowLock.synchronized {
      val enoughSamples = polishFallbackWindow.size >= PolishWindowSize
      val p95 = percentile(polishLatencyWindowMs, 0.95)
      val fallbackRate =
        if polishFallbackWindow.isEmpty then 0.0
        else polishFallbackWindow.count(identity).toDouble / polishFallbackWindow.size.toDouble
      enoughSamples && (fallbackRate > 0.08 || p95 > 900.0)
    }

  private def isPolishCircuitOpen(nowMs: Long): Boolean =
    polishWindowLock.synchronized {
      nowMs < polishCircuitOpenUntilMs
    }

  private def maybeOpenPolishCircuit(nowMs: Long): Unit =
    if shouldOpenPolishCircuit then
      polishWindowLock.synchronized {
        if nowMs >= polishCircuitOpenUntilMs then
          polishCircuitOpenUntilMs = nowMs + PolishCircuitCooldownMs
      }

  private def incPolishReason(reason: String): Unit =
    polishReasonCount.getOrElseUpdate(reason, new AtomicLong(0L)).incrementAndGet()

  private def incFullGameInvalidReason(stage: String, reason: String): Unit =
    fullGameInvalidReasonCount.getOrElseUpdate(s"$stage:$reason", new AtomicLong(0L)).incrementAndGet()

  private def shouldLogOpsSample(kind: String): Boolean =
    val count = opsSampleCount.getOrElseUpdate(kind, new AtomicLong(0L)).incrementAndGet()
    count <= 5 || count % 50 == 0

  private def logOpsSample(kind: String, fields: (String, String)*): Unit =
    lila.mon.commentary.commentary.sample(kind).increment()
    if shouldLogOpsSample(kind) then
      commentaryOpsBoard.recordSample(kind = kind, fields = fields.toMap)
      val rendered =
        fields
          .map { case (k, v) =>
            val safe = Option(v).getOrElse("").replaceAll("""[\r\n\t]+""", " ").trim
            s"$k=${if safe.nonEmpty then safe else "-"}"
          }
          .mkString(" ")
      logger.warn(s"commentary.ops.sample kind=$kind $rendered")

  private def recordFullGameInvalid(
      stage: String,
      phase: String,
      model: Option[String],
      reasons: List[String]
  ): Unit =
    val normalized = reasons.map(_.trim).filter(_.nonEmpty).distinct
    normalized.foreach(reason => incFullGameInvalidReason(stage, reason))
    if normalized.nonEmpty then
      logOpsSample(
        kind = "fullgame_polish.invalid",
        "stage" -> stage,
        "phase" -> phase,
        "model" -> model.getOrElse(""),
        "reasons" -> normalized.mkString(",")
      )

  private def recordFullGameRepairAttempt(): Unit =
    fullGameSegmentRepairAttemptCount.incrementAndGet()

  private def recordFullGameRepairBypass(
      phase: String,
      reasons: List[String]
  ): Unit =
    fullGameSegmentRepairBypassedCount.incrementAndGet()
    logOpsSample(
      kind = "fullgame_segment_repair_bypassed",
      "phase" -> phase,
      "reasons" -> reasons.map(_.trim).filter(_.nonEmpty).distinct.mkString(",")
    )

  private def recordFullGameSoftRepair(
      phase: String,
      actions: List[String],
      reasons: List[String]
  ): Unit =
    fullGameSegmentSoftRepairAppliedCount.incrementAndGet()
    logOpsSample(
      kind = "fullgame_segment_soft_repair",
      "phase" -> phase,
      "actions" -> actions.mkString(","),
      "remainingReasons" -> reasons.map(_.trim).filter(_.nonEmpty).distinct.mkString(",")
    )

  private def recordFullGameMergedRetrySkipped(
      phase: String,
      reasons: List[String]
  ): Unit =
    fullGameMergedRetrySkippedCount.incrementAndGet()
    logOpsSample(
      kind = "fullgame_segment_retry_skipped",
      "phase" -> phase,
      "reasons" -> reasons.map(_.trim).filter(_.nonEmpty).distinct.mkString(",")
    )

  private def recordComparisonObservation(
      path: String,
      digest: Option[DecisionComparisonDigest],
      authorEvidence: List[AuthorEvidenceSummary],
      probeRequests: List[lila.commentary.model.ProbeRequest],
      strategyPack: Option[StrategyPack]
  ): Unit =
    CommentaryOpsSignals
      .decisionComparisonConsistency(
        digest = digest,
        authorEvidence = authorEvidence,
        probeRequests = probeRequests,
        strategyPack = strategyPack
      )
      .foreach { obs =>
        val (total, hit) =
          path match
            case "moveReview" => (moveReviewCompareObservedCount, moveReviewCompareConsistentCount)
            case _           => (fullGameCompareObservedCount, fullGameCompareConsistentCount)
        total.incrementAndGet()
        if obs.consistent then hit.incrementAndGet()
        lila.mon.commentary.commentary.compare(path, obs.consistent).increment()
        updateCommentaryOpsGauges()
        if !obs.consistent then
          logOpsSample(
            kind = s"compare_inconsistent.$path",
            "reasons" -> obs.reasons.mkString(","),
            "chosen" -> digest.flatMap(_.chosenMove).getOrElse(""),
            "best" -> digest.flatMap(_.engineBestMove).getOrElse(""),
            "deferred" -> digest.flatMap(_.deferredMove).getOrElse(""),
            "source" -> digest.flatMap(_.deferredSource).getOrElse("")
          )
      }

  private def traceMoveReviewFallbackResponse(
      response: CommentResponse,
      phase: String,
      ply: Int,
      lastMove: Option[String],
      cacheHit: Boolean,
      openingRefPresent: Boolean,
      incomingProbeCount: Int
  ): Unit =
    val sourceMode = Option(response.sourceMode).map(_.trim).filter(_.nonEmpty).getOrElse("rule")
    val shouldTrace = MoveReviewResponseDiagnostics.isFallbackSourceMode(sourceMode)
    if shouldTrace then
      val reasons =
        response.polishMeta
          .map(_.validationReasons.map(_.trim).filter(_.nonEmpty).distinct)
          .getOrElse(Nil)
      val provider = response.polishMeta.map(_.provider).getOrElse("")
      val model = response.polishMeta.flatMap(_.model).orElse(response.model).getOrElse("")
      val sampleKind =
        sourceMode match
          case "fallback_rule_invalid" => "move_review_fallback.invalid"
          case "fallback_rule_empty"   => "move_review_fallback.empty"
          case "rule_circuit_open"     => "move_review_fallback.circuit_open"
          case other                   => s"move_review_fallback.${other.replaceAll("""[^a-zA-Z0-9]+""", "_")}"
      val fields = Seq(
        "sourceMode" -> sourceMode,
        "reasons" -> reasons.mkString(","),
        "phase" -> phase,
        "ply" -> ply.toString,
        "lastMove" -> lastMove.getOrElse(""),
        "provider" -> provider,
        "model" -> model,
        "cacheHit" -> cacheHit.toString,
        "openingRef" -> openingRefPresent.toString,
        "probeCount" -> incomingProbeCount.toString
      )
      logOpsSample(kind = sampleKind, fields = fields*)
      val rendered =
        fields
          .map { case (k, v) =>
            val safe = Option(v).getOrElse("").replaceAll("""[\r\n\t]+""", " ").trim
            s"$k=${if safe.nonEmpty then safe else "-"}"
          }
          .mkString(" ")
      logger.warn(s"commentary.move_review.fallback $rendered")

  private def recordPolishMetrics(
      sourceMode: String,
      latencyMs: Long,
      cacheHit: Boolean,
      reasons: List[String],
      estimatedCostUsd: Option[Double]
  ): Unit =
    val fallback = MoveReviewResponseDiagnostics.isFallbackSourceMode(sourceMode)
    polishAttemptCount.incrementAndGet()
    if sourceMode == "ai_polished" then polishAcceptedCount.incrementAndGet()
    if fallback then polishFallbackCount.incrementAndGet()
    if cacheHit then polishCacheHitCount.incrementAndGet()
    reasons.foreach(incPolishReason)
    estimatedCostUsd.foreach { usd =>
      val micros = Math.round(usd * 1000000.0)
      polishEstimatedCostMicros.addAndGet(micros.max(0L))
    }
    polishWindowLock.synchronized {
      polishLatencyWindowMs = pushBoundedWindow(polishLatencyWindowMs, latencyMs.max(0L), PolishWindowSize)
      polishFallbackWindow = pushBoundedWindow(polishFallbackWindow, fallback, PolishWindowSize)
    }
    maybeOpenPolishCircuit(System.currentTimeMillis())
    updateCommentaryOpsGauges()

  private def recordLatencyMetrics(totalLatencyMs: Long, structureEvalLatencyMs: Option[Long]): Unit =
    latencyWindowLock.synchronized {
      totalLatencyWindowMs = pushWindow(totalLatencyWindowMs, totalLatencyMs.max(0L))
      structureEvalLatencyMs.foreach { ms =>
        structureEvalLatencyWindowMs = pushWindow(structureEvalLatencyWindowMs, ms.max(0L))
      }
    }

  def commentaryOpsSnapshot(limit: Int = 20): CommentaryOpsBoard.Snapshot =
    val requests = moveReviewRequests.get()
    val attempts = polishAttemptCount.get()
    val accepted = polishAcceptedCount.get()
    val fallbackRate =
      if attempts == 0 then 0.0
      else polishFallbackCount.get().toDouble / attempts.toDouble
    val softRepairAnyRate =
      if accepted == 0 then 0.0
      else softRepairAppliedCount.get().toDouble / accepted.toDouble
    val softRepairMaterialRate =
      if accepted == 0 then 0.0
      else softRepairMaterialCount.get().toDouble / accepted.toDouble
    val moveReviewCompareRate =
      if moveReviewCompareObservedCount.get() == 0 then 1.0
      else moveReviewCompareConsistentCount.get().toDouble / moveReviewCompareObservedCount.get().toDouble
    val avgCostUsd =
      if attempts == 0 then 0.0
      else polishEstimatedCostMicros.get().toDouble / attempts.toDouble / 1000000.0

    CommentaryOpsBoard.Snapshot(
      generatedAtMs = System.currentTimeMillis(),
      moveReview = CommentaryOpsBoard.MoveReviewMetrics(
        requests = requests,
        polishAttempts = attempts,
        polishAccepted = accepted,
        polishFallbackRate = fallbackRate,
        softRepairAnyRate = softRepairAnyRate,
        softRepairMaterialRate = softRepairMaterialRate,
        compareObserved = moveReviewCompareObservedCount.get(),
        compareConsistencyRate = moveReviewCompareRate,
        avgCostUsd = avgCostUsd
      ),
      promptUsage = promptUsageSnapshot(),
      recentSamples = commentaryOpsBoard.recentSamples(limit)
    )

  private def updateCommentaryOpsGauges(): Unit =
    val attempts = polishAttemptCount.get()
    val accepted = polishAcceptedCount.get()
    val moveReviewFallbackRate =
      if attempts == 0 then 0.0
      else polishFallbackCount.get().toDouble / attempts.toDouble
    val moveReviewSoftRepairMaterialRate =
      if accepted == 0 then 0.0
      else softRepairMaterialCount.get().toDouble / accepted.toDouble
    val moveReviewCompareRate =
      if moveReviewCompareObservedCount.get() == 0 then 1.0
      else moveReviewCompareConsistentCount.get().toDouble / moveReviewCompareObservedCount.get().toDouble
    val avgCostUsd =
      if attempts == 0 then 0.0
      else polishEstimatedCostMicros.get().toDouble / attempts.toDouble / 1000000.0
    val latencyP95 = polishLatencyP95Ms

    lila.mon.commentary.commentary.metric("polish_fallback_rate", "moveReview").update(moveReviewFallbackRate)
    lila.mon.commentary.commentary.metric("soft_repair_material_rate", "moveReview").update(moveReviewSoftRepairMaterialRate)
    lila.mon.commentary.commentary.metric("compare_consistency_rate", "moveReview").update(moveReviewCompareRate)
    lila.mon.commentary.commentary.metric("avg_cost_usd", "moveReview").update(avgCostUsd)
    lila.mon.commentary.commentary.metric("polish_latency_p95_ms", "moveReview").update(latencyP95)

  private def latencySnapshot: (Int, Double, Double, Int, Double, Double) =
    latencyWindowLock.synchronized {
      val totalWindow = totalLatencyWindowMs
      val structWindow = structureEvalLatencyWindowMs
      (
        totalWindow.size,
        percentile(totalWindow, 0.50),
        percentile(totalWindow, 0.95),
        structWindow.size,
        percentile(structWindow, 0.50),
        percentile(structWindow, 0.95)
      )
    }

  private inline def elapsedMs(startNs: Long): Long =
    ((System.nanoTime() - startNs) / 1000000L).max(0L)

  private def structureMode: String =
    if commentaryConfig.structKbEnabled then "enabled"
    else if commentaryConfig.structKbShadowMode then "shadow"
    else "off"

  private def endgameMode: String =
    "enabled"

  private def maybeLogMoveReviewMetrics(): Unit =
    val total = moveReviewRequests.get()
    if total > 0 && total % 100 == 0 then
      val tokenPresentRate = tokenPresentCount.get().toDouble / total.toDouble
      val tokenEmitRate = tokenEmitCount.get().toDouble / total.toDouble
      val continuityAppliedRate = continuityAppliedCount.get().toDouble / total.toDouble
      val transitionDist = transitionCountByType.toList.map { case (k, v) => k -> v.get() }.toMap
      val structureCoverage =
        if !commentaryConfig.shouldEvaluateStructureKb || total == 0 then 0.0
        else structureProfileCount.get().toDouble / total.toDouble
      val structureUnknownRate =
        if structureProfileCount.get() == 0 then 0.0
        else structureUnknownCount.get().toDouble / structureProfileCount.get().toDouble
      val structureLowConfRate =
        if structureProfileCount.get() == 0 then 0.0
        else structureLowConfidenceCount.get().toDouble / structureProfileCount.get().toDouble
      val endgameCoverage =
        if total == 0 then 0.0
        else endgameFeatureCount.get().toDouble / total.toDouble
      val endgameWinHintRate =
        if endgameFeatureCount.get() == 0 then 0.0
        else endgameWinHintCount.get().toDouble / endgameFeatureCount.get().toDouble
      val endgameHighConfRate =
        if endgameFeatureCount.get() == 0 then 0.0
        else endgameHighConfidenceCount.get().toDouble / endgameFeatureCount.get().toDouble
      val structureDist = structureCountByType.toList.map { case (k, v) => k -> v.get() }.toMap
      val bandDist = alignmentBandCount.toList.map { case (k, v) => k -> v.get() }.toMap
      val polishAttempts = polishAttemptCount.get()
      val polishAccepted = polishAcceptedCount.get()
      val polishAcceptRate =
        if polishAttempts == 0 then 0.0
        else polishAccepted.toDouble / polishAttempts.toDouble
      val softRepairRate =
        if polishAccepted == 0 then 0.0
        else softRepairAppliedCount.get().toDouble / polishAccepted.toDouble
      val softRepairMaterialRate =
        if polishAccepted == 0 then 0.0
        else softRepairMaterialCount.get().toDouble / polishAccepted.toDouble
      val moveReviewCompareRate =
        if moveReviewCompareObservedCount.get() == 0 then 1.0
        else moveReviewCompareConsistentCount.get().toDouble / moveReviewCompareObservedCount.get().toDouble
      val avgCostUsd =
        if polishAttempts == 0 then 0.0
        else polishEstimatedCostMicros.get().toDouble / polishAttempts.toDouble / 1000000.0
      val polishReasonDist = polishReasonCount.toList.map { case (k, v) => k -> v.get() }.toMap
      val polishFallbackRateWindow = polishFallbackRate
      val polishLatencyP95Window = polishLatencyP95Ms
      val promptUsage = promptUsageSnapshot()
      val planRecallAt3Legacy =
        if planRecallTotal.get() == 0 then 0.0
        else planRecallHit.get().toDouble / planRecallTotal.get().toDouble
      val planRecallAt3HypothesisFirst =
        if planRecallHypothesisFirstTotal.get() == 0 then 0.0
        else planRecallHypothesisFirstHit.get().toDouble / planRecallHypothesisFirstTotal.get().toDouble
      val planRecallAt3 = planRecallAt3Legacy
      val hypothesisProbeHitRate =
        if hypothesisProbeTotal.get() == 0 then 1.0
        else hypothesisProbeHit.get().toDouble / hypothesisProbeTotal.get().toDouble
      val contractDropRate =
        if contractProbeTotal.get() == 0 then 0.0
        else contractProbeDropped.get().toDouble / contractProbeTotal.get().toDouble
      val claimCertificationObserved =
        claimCertificationObservedTotal.get().toDouble
      val blockedStrongClaimRate =
        if claimCertificationObserved <= 0 then 0.0
        else claimCertificationBlockedStrongTotal.get().toDouble / claimCertificationObserved
      val downgradedWeakClaimRate =
        if claimCertificationObserved <= 0 then 0.0
        else claimCertificationDowngradedWeakTotal.get().toDouble / claimCertificationObserved
      val attributionFailureRate =
        if claimCertificationObserved <= 0 then 0.0
        else claimCertificationAttributionFailureTotal.get().toDouble / claimCertificationObserved
      val quantifierFailureRate =
        if claimCertificationObserved <= 0 then 0.0
        else claimCertificationQuantifierFailureTotal.get().toDouble / claimCertificationObserved
      val stabilityFailureRate =
        if claimCertificationObserved <= 0 then 0.0
        else claimCertificationStabilityFailureTotal.get().toDouble / claimCertificationObserved
      val (totalWindowSize, totalP50, totalP95, structWindowSize, structP50, structP95) = latencySnapshot
      val epochSec = Instant.now().getEpochSecond
      logger.info(
        s"move_review.metrics epoch=$epochSec total=$total " +
          s"struct_mode=${structureMode} " +
          s"endgame_mode=${endgameMode} " +
          s"shadow_window=$totalWindowSize " +
          f"token_present_rate=$tokenPresentRate%.3f " +
          f"token_emit_rate=$tokenEmitRate%.3f " +
          f"continuity_applied_rate=$continuityAppliedRate%.3f " +
          s"state_cache_hit=${stateAwareCacheHitCount.get()} " +
          s"state_cache_miss=${stateAwareCacheMissCount.get()} " +
          s"same_ply_idempotent_hits=${samePlyIdempotentHitCount.get()} " +
          f"total_latency_p50_ms=$totalP50%.3f " +
          f"total_latency_p95_ms=$totalP95%.3f " +
          s"struct_latency_samples=$structWindowSize " +
          f"struct_eval_p50_ms=$structP50%.3f " +
          f"struct_eval_p95_ms=$structP95%.3f " +
          s"transition_dist=$transitionDist " +
          f"struct_coverage=$structureCoverage%.3f " +
          f"struct_unknown_rate=$structureUnknownRate%.3f " +
          f"struct_low_conf_rate=$structureLowConfRate%.3f " +
          f"endgame_coverage=$endgameCoverage%.3f " +
          f"endgame_win_hint_rate=$endgameWinHintRate%.3f " +
          f"endgame_high_conf_rate=$endgameHighConfRate%.3f " +
          f"polish_acceptance_ratio=$polishAcceptRate%.3f " +
          f"polish_soft_repair_rate=$softRepairRate%.3f " +
          f"polish_soft_repair_material_rate=$softRepairMaterialRate%.3f " +
          f"polish_fallback_window_rate=$polishFallbackRateWindow%.3f " +
          f"polish_latency_p95_ms=$polishLatencyP95Window%.3f " +
          f"move_review_compare_consistency_rate=$moveReviewCompareRate%.3f " +
          f"plan_recall_at3=$planRecallAt3%.3f " +
          f"plan_recall_at3_legacy=$planRecallAt3Legacy%.3f " +
          f"plan_recall_at3_hypothesis_first=$planRecallAt3HypothesisFirst%.3f " +
          f"hypothesis_probe_hit_rate=$hypothesisProbeHitRate%.3f " +
          f"contract_drop_rate=$contractDropRate%.3f " +
          f"claim_cert_blocked_strong_rate=$blockedStrongClaimRate%.3f " +
          f"claim_cert_downgraded_weak_rate=$downgradedWeakClaimRate%.3f " +
          f"claim_cert_attribution_failure_rate=$attributionFailureRate%.3f " +
          f"claim_cert_quantifier_failure_rate=$quantifierFailureRate%.3f " +
          f"claim_cert_stability_failure_rate=$stabilityFailureRate%.3f " +
          f"polish_avg_cost_usd=$avgCostUsd%.6f " +
          s"polish_reason_dist=$polishReasonDist " +
          s"prompt_usage=$promptUsage " +
          s"struct_dist=$structureDist " +
          s"alignment_band_dist=$bandDist"
      )

  private def isAiPolishEnabledForRequest(allowAiPolish: Boolean): Boolean =
    if providerConfig.premiumOnly then allowAiPolish else true

  private def normalizeLang(lang: String): String =
    Option(lang).map(_.trim.toLowerCase).filter(_.nonEmpty).map(_.take(8)).getOrElse(providerConfig.defaultLang)

  private def normalizePlanTier(raw: String): String =
    PlanTier.normalize(raw)

  private def normalizeCommentaryMode(@unused raw: String): String =
    CommentaryMode.Polish

  private def strategyHints(pack: Option[StrategyPack]): List[String] =
    pack.toList.flatMap(StrategyPackBuilder.promptHints)

  private def templateQualityScore(prose: String, lang: String): Double =
    val t = Option(prose).getOrElse("")
    if t.isBlank then 0.0
    else if !isEnglishLang(lang) then 0.0
    else
      val lower = t.toLowerCase
      val markers = List(
        "additionally",
        "furthermore",
        "moreover",
        "it is worth noting",
        "engine-wise",
        "observed directly:",
        "initial board read:",
        "working hypothesis:",
        "strongest read is that",
        "clearest read is that",
        "validation evidence",
        "validation lines up",
        "supporting that, we see",
        "a corroborating idea",
        "another key pillar is",
        "this adds weight to",
        "the practical burden",
        "the practical immediate future",
        "the practical medium-horizon task",
        "practically, this should influence",
        "the leading route is",
        "the backup strategic stack is",
        "the main signals are",
        "under the current evidence threshold",
        "no strong refutation signal was found",
        "strategic claims are held until"
      )
      val repeatedMarkerCount = markers.map(m => lower.sliding(m.length).count(_ == m)).sum
      val shortSentencePenalty =
        lower
          .split("[.!?]")
          .map(_.trim)
          .count(s => s.nonEmpty && s.length < 20)
          .toDouble * 0.01
      val score = 1.0 - repeatedMarkerCount.toDouble * 0.08 - shortSentencePenalty
      score.max(0.0).min(1.0)

  private def isEnglishLang(lang: String): Boolean =
    val normalized = normalizeLang(lang).replace('_', '-')
    normalized == "en" || normalized.startsWith("en-") || normalized == "english"

  private def wordCount(text: String): Int =
    Option(text).getOrElse("").split("\\s+").count(_.nonEmpty)

  private def excerptForOps(text: String, maxChars: Int = 220): String =
    val cleaned = Option(text).getOrElse("").replaceAll("""\s+""", " ").trim
    if cleaned.length <= maxChars then cleaned
    else cleaned.take(maxChars - 1).trim + "…"

  private def recordFullGameInvalidPair(
      stage: String,
      phase: String,
      model: Option[String],
      original: String,
      candidate: String,
      reasons: List[String]
  ): Unit =
    val normalized = reasons.map(_.trim).filter(_.nonEmpty).distinct
    val pairRelevant =
      normalized.exists(reason =>
        reason == "length_ratio_out_of_bounds" ||
          reason == "placeholder_leak_detected" ||
          reason == "meta_label_leak_detected" ||
          reason == "helper_symbol_leak_detected" ||
          reason == "broken_fragment_detected"
      )
    if pairRelevant then
      val originalWords = wordCount(original)
      val candidateWords = wordCount(candidate)
      val ratio =
        if originalWords <= 0 then 0.0
        else candidateWords.toDouble / originalWords.toDouble
      logOpsSample(
        kind = "fullgame_polish.invalid_pair",
        "stage" -> stage,
        "phase" -> phase,
        "model" -> model.getOrElse(""),
        "reasons" -> normalized.mkString(","),
        "hits" -> FullGameDraftNormalizer.leakHits(candidate).mkString(","),
        "originalWords" -> originalWords.toString,
        "candidateWords" -> candidateWords.toString,
        "ratio" -> f"$ratio%.2f",
        "original" -> excerptForOps(original),
        "candidate" -> excerptForOps(candidate)
      )

  private def lengthRatioReasonable(polished: String, original: String): Boolean =
    val originalWords = wordCount(original)
    val polishedWords = wordCount(polished)
    if originalWords <= 0 then polishedWords > 0
    else if originalWords < 35 then polishedWords <= math.max(70, (originalWords.toDouble * 2.2).toInt)
    else
      val lowerBound = (originalWords.toDouble * 0.45).toInt.max(18)
      val upperBound = (originalWords.toDouble * 1.35).toInt.max(originalWords + 8)
      polishedWords >= lowerBound && polishedWords <= upperBound

  private def unwrapCommentaryPayload(text: String): String =
    CommentaryPayloadNormalizer.normalize(text)

  private def safeDeterministicFallbackProse(
      prose: String,
      moveReviewSlots: Option[MoveReviewPolishSlots]
  ): String =
    val candidates =
      List(
        LiveNarrativeCompressionCore.deterministicFallbackProse(prose, moveReviewSlots),
        moveReviewSlots.map(LiveNarrativeCompressionCore.deterministicProse).getOrElse(""),
        moveReviewSlots.flatMap(slots => Option(slots.claim).map(_.trim).filter(_.nonEmpty)).getOrElse(""),
        PlayerProseBoundary.sanitize(prose)
      )
        .map(PlayerProseBoundary.sanitize)
        .map(_.trim)
        .filter(_.nonEmpty)
        .distinct
    candidates
      .find(candidate => PlayerProseBoundary.validateSanitized(candidate).isAccepted)
      .getOrElse("")

  private def shouldSkipPolish(prose: String, lang: String): Boolean =
    isEnglishLang(lang) && templateQualityScore(prose, lang) >= providerConfig.polishGateThreshold

  private def validatePolishedCommentary(
      polished: String,
      original: String,
      allowedSans: List[String],
      slotMode: Boolean = false
  ): PolishValidation.ValidationResult =
    val surfaceValidation = PlayerProseBoundary.validate(polished)
    val trimmed = surfaceValidation.text
    if trimmed.isEmpty then PolishValidation.ValidationResult(isValid = false, reasons = List("empty_polish"))
    else if surfaceValidation.reasons.nonEmpty then
      PolishValidation.ValidationResult(isValid = false, reasons = surfaceValidation.reasons)
    else if leakTokens.exists(trimmed.contains) then
      PolishValidation.ValidationResult(isValid = false, reasons = List("leak_token_detected"))
    else if !slotMode && !lengthRatioReasonable(trimmed, original) then
      PolishValidation.ValidationResult(isValid = false, reasons = List("length_ratio_out_of_bounds"))
    else PolishValidation.validatePolishedCommentary(trimmed, original, allowedSans)

  private def logInvalidPolish(
      provider: String,
      phase: String,
      attempt: String,
      model: Option[String],
      reasons: List[String]
  ): Unit =
    val reasonStr = if reasons.isEmpty then "unknown" else reasons.mkString(",")
    logger.debug(
      s"commentary.polish.invalid provider=$provider phase=$phase attempt=$attempt model=${model.getOrElse("-")} reasons=$reasonStr"
    )

  private def commentaryCacheContext(
      allowAiPolish: Boolean,
      lang: String,
      asyncTier: Boolean,
      planTier: String,
      commentaryMode: String
  ): Option[CommentaryCacheContext] =
    if !isAiPolishEnabledForRequest(allowAiPolish) then None
    else
      val normalizedLang = normalizeLang(lang)
      val normalizedPlanTier = normalizePlanTier(planTier)
      val normalizedCommentaryMode = normalizeCommentaryMode(commentaryMode)
      val baseModel =
        providerConfig.provider match
          case "openai" if openAiClient.isEnabled =>
            Some(
              if asyncTier then openAiClient.asyncModelName(normalizedPlanTier, normalizedCommentaryMode)
              else openAiClient.syncModelName(normalizedPlanTier, normalizedCommentaryMode)
            )
          case "gemini" if geminiClient.isEnabled => Some(geminiClient.modelName)
          case "none"                             => Some("rule")
          case other                              => Some(s"${other}_unavailable")
      baseModel.map { model =>
        CommentaryCacheContext(
          model = model,
          promptVersion = providerConfig.promptVersion,
          lang = normalizedLang,
          planTier = normalizedPlanTier,
          commentaryMode = normalizedCommentaryMode,
          authorityFingerprint = "-"
        )
      }

  private def sumIntOptions(values: List[Option[Int]]): Option[Int] =
    if values.exists(_.isDefined) then Some(values.map(_.getOrElse(0)).sum)
    else None

  private def sumDoubleOptions(values: List[Option[Double]]): Option[Double] =
    if values.exists(_.isDefined) then Some(values.map(_.getOrElse(0.0)).sum)
    else None

  private def normalizePromptUsageFamily(family: String): String =
    Option(family)
      .map(_.trim.toLowerCase.replaceAll("""[^a-z0-9]+""", "_"))
      .map(_.stripPrefix("_").stripSuffix("_"))
      .filter(_.nonEmpty)
      .getOrElse("unknown")

  private def promptUsageStats(family: String): PromptUsageStats =
    promptUsageByFamily.getOrElseUpdate(normalizePromptUsageFamily(family), PromptUsageStats())

  private def recordPromptUsageAttempts(attempts: List[(String, OpenAiPolishResult)]): Unit =
    attempts.foreach { case (family, attempt) =>
      val stats = promptUsageStats(family)
      stats.attempts.incrementAndGet()
      if attempt.promptCacheHit then stats.cacheHits.incrementAndGet()
      attempt.promptTokens.foreach(v => stats.promptTokens.addAndGet(v.max(0).toLong))
      attempt.cachedTokens.foreach(v => stats.cachedTokens.addAndGet(v.max(0).toLong))
      attempt.completionTokens.foreach(v => stats.completionTokens.addAndGet(v.max(0).toLong))
      attempt.estimatedCostUsd.foreach { usd =>
        val micros = Math.round(usd * 1000000.0)
        stats.estimatedCostMicros.addAndGet(micros.max(0L))
      }
    }

  private def promptUsageSnapshot(prefix: Option[String] = None): Map[String, CommentaryOpsBoard.PromptUsageMetrics] =
    val normalizedPrefix = prefix.map(normalizePromptUsageFamily)
    promptUsageByFamily.toList
      .map { case (family, stats) =>
        family -> CommentaryOpsBoard.PromptUsageMetrics(
          attempts = stats.attempts.get(),
          cacheHits = stats.cacheHits.get(),
          promptTokens = stats.promptTokens.get(),
          cachedTokens = stats.cachedTokens.get(),
          completionTokens = stats.completionTokens.get(),
          estimatedCostUsd = stats.estimatedCostMicros.get().toDouble / 1000000.0
        )
      }
      .filter { case (family, _) => normalizedPrefix.forall(family.startsWith) }
      .sortBy(_._1)
      .toMap

  private def polishPromptFamilyBase(momentType: Option[String]): String =
    momentType.map(_.trim).filter(_.nonEmpty) match
      case Some(value) if value.equalsIgnoreCase("Game Intro")      => "fullgame_intro"
      case Some(value) if value.equalsIgnoreCase("Game Conclusion") => "fullgame_conclusion"
      case Some(_)                                                  => "fullgame_moment"
      case None                                                     => "moveReview"

  private def shouldTrySegmentPolish(
      prose: String,
      refs: Option[MoveReviewRefs],
      allowedSans: List[String],
      momentType: Option[String],
      moveReviewSlots: Option[MoveReviewPolishSlots]
  ): Boolean =
    val normalizedMomentType = momentType.map(_.trim.toLowerCase).getOrElse("")
    val introOrConclusion =
      normalizedMomentType == "game intro" || normalizedMomentType == "game conclusion"
    val hasConcreteEvidence = refs.exists(_.variations.nonEmpty) || allowedSans.distinct.size >= SegmentPolishMinAllowedSans
    moveReviewSlots.isEmpty &&
      !introOrConclusion &&
      hasConcreteEvidence &&
      wordCount(prose) >= SegmentPolishMinWords

  private def adaptiveOutputTokenCap(
      prose: String,
      refs: Option[MoveReviewRefs],
      asyncTier: Boolean
  ): Int =
    // Structured polish prompts can become long once anchors (MV/MK/EV/VB) are embedded.
    // Keep a higher floor/ceiling to reduce wrapper truncation and empty-polish fallbacks.
    val minCap = if asyncTier then 640 else 480
    val maxCap = if asyncTier then 1200 else 840
    val proseWords = wordCount(prose)
    val anchorCount = refs.toList.flatMap(_.variations).flatMap(_.moves).size
    val estimated = (proseWords.toDouble * 2.4 + anchorCount.toDouble * 5.0 + 96.0).toInt
    estimated.max(minCap).min(maxCap)

  // MoveReview-only authority keeps this metadata diagnostic; no Active-mode enforcement path remains.
  private val StrategyCoverageThreshold = 0.34

  private case class StrategyCoverageEvaluation(
      meta: Option[MoveReviewStrategyCoverageMeta],
      reasons: List[String]
  )

  private def evaluateStrategyCoverage(
      commentary: String,
      strategyPack: Option[StrategyPack],
      @unused planTier: String,
      @unused commentaryMode: String
  ): StrategyCoverageEvaluation =
    val shouldEnforce = false
    if !shouldEnforce then StrategyCoverageEvaluation(meta = None, reasons = Nil)
    else
      val packOpt =
        strategyPack.filter(pack =>
          pack.strategicIdeas.nonEmpty || pack.plans.nonEmpty || pack.pieceRoutes.nonEmpty || pack.directionalTargets.nonEmpty || pack.longTermFocus.nonEmpty
        )
      packOpt match
        case None =>
          StrategyCoverageEvaluation(
            meta = Some(
              MoveReviewStrategyCoverageMeta(
                mode = "no_signals",
                enforced = false,
                threshold = StrategyCoverageThreshold,
                availableCategories = 0,
                coveredCategories = 0,
                requiredCategories = 0,
                coverageScore = 1.0,
                passesThreshold = true,
                planSignals = 0,
                planHits = 0,
                routeSignals = 0,
                routeHits = 0,
                focusSignals = 0,
                focusHits = 0
              )
            ),
            reasons = Nil
          )
        case Some(pack) =>
          val commentaryLower = Option(commentary).getOrElse("").toLowerCase
          val commentaryTokens = StrategicSignalMatcher.signalTokens(commentaryLower)
          val surface = StrategyPackSurface.from(Some(pack))
          val digest = pack.signalDigest

          def signalMentioned(text: String): Boolean =
            val normalized = Option(text).map(_.trim).getOrElse("")
            val tokens = StrategicSignalMatcher.signalTokens(normalized)
            StrategicSignalMatcher.phraseMentioned(commentaryLower, normalized.toLowerCase) ||
              (tokens.nonEmpty && tokens.intersect(commentaryTokens).nonEmpty)

          val planCandidates =
            (
              surface.dominantIdeaText.toList ++
                surface.secondaryIdeaText.toList ++
                pack.plans.map(_.planName).filter(_.trim.nonEmpty)
            ).distinct.take(2)
          val planSignals = planCandidates.size
          val planHits = planCandidates.count(signalMentioned)

          val routeCandidates = (surface.topRoute.toList ++ pack.pieceRoutes.take(2)).distinct.take(2)
          val routeSignals = routeCandidates.size
          val routeHits =
            routeCandidates.count(route =>
              StrategicSignalMatcher.routeMentioned(commentaryLower, route) ||
                surface.topMoveRef.exists(moveRef =>
                  moveRef.ownerSide == route.ownerSide &&
                    signalMentioned(s"${moveRef.piece} ${moveRef.from} ${moveRef.target} ${moveRef.idea}")
                )
            )

          val focusCandidates =
            (
              surface.objectiveText.toList ++
                surface.focusText.toList ++
                pack.longTermFocus.map(_.trim).filter(_.nonEmpty)
            ).distinct.take(2)
          val focusSignals = focusCandidates.size
          val focusHits = focusCandidates.count(signalMentioned)

          val availableCategories = List(planSignals, routeSignals, focusSignals).count(_ > 0)
          val coveredCategories = List(planHits, routeHits, focusHits).count(_ > 0)
          val compensationPosition =
            surface.compensationPosition ||
              digest.exists(d => d.compensation.exists(_.trim.nonEmpty) || d.investedMaterial.exists(_ > 0))
          val ownerMentioned =
            surface.campaignOwner.exists(owner => commentaryLower.contains(owner)) ||
              surface.campaignOwnerText.exists(label => commentaryLower.contains(label.toLowerCase))
          val compensationMentioned =
            digest.flatMap(_.compensation).exists(signalMentioned) ||
              commentaryLower.contains("initiative") ||
              commentaryLower.contains("compensation") ||
              commentaryLower.contains("sacrifice") ||
              commentaryLower.contains("invest")
          val compensationReturnMentioned =
            digest.toList.flatMap(_.compensationVectors).exists(signalMentioned) ||
              surface.executionText.exists(signalMentioned) ||
              surface.objectiveText.exists(signalMentioned) ||
              surface.focusText.exists(signalMentioned)
          val dominantIdeaMentioned = surface.dominantIdeaText.exists(signalMentioned)
          val generalPass =
            availableCategories == 0 ||
              coveredCategories >= Math.min(2, availableCategories) ||
              (dominantIdeaMentioned && focusHits > 0)
          val compensationPass =
            !compensationPosition || (compensationMentioned && compensationReturnMentioned)
          val ownerPass = !surface.ownerMismatch || ownerMentioned
          val requiredCategories =
            if availableCategories == 0 then 0
            else Math.min(2, availableCategories)
          val coverageScore =
            if availableCategories == 0 then 1.0
            else coveredCategories.toDouble / availableCategories.toDouble
          val passesThreshold = (if compensationPosition then compensationPass else generalPass) && ownerPass
          val missingReasons =
            if passesThreshold then Nil
            else
              val reasons = List.newBuilder[String]
              if planSignals > 0 && planHits == 0 then reasons += "strategy_plan_missing"
              if routeSignals > 0 && routeHits == 0 then reasons += "strategy_route_missing"
              if focusSignals > 0 && focusHits == 0 then reasons += "strategy_focus_missing"
              if compensationPosition && !compensationMentioned then reasons += "strategy_compensation_missing"
              if compensationPosition && !compensationReturnMentioned then
                reasons += "strategy_execution_or_objective_missing"
              if surface.ownerMismatch && !ownerMentioned then reasons += "strategy_campaign_owner_missing"
              reasons += "strategy_coverage_low"
              reasons.result().distinct

          StrategyCoverageEvaluation(
            meta = Some(
              MoveReviewStrategyCoverageMeta(
                mode = "active_enforced",
                enforced = true,
                threshold = StrategyCoverageThreshold,
                availableCategories = availableCategories,
                coveredCategories = coveredCategories,
                requiredCategories = requiredCategories,
                coverageScore = coverageScore,
                passesThreshold = passesThreshold,
                planSignals = planSignals,
                planHits = planHits,
                routeSignals = routeSignals,
                routeHits = routeHits,
                focusSignals = focusSignals,
                focusHits = focusHits
              )
            ),
            reasons = missingReasons
          )

  private case class CandidateValidation(
      isValid: Boolean,
      decodedText: String,
      reasons: List[String],
      strategyCoverage: Option[MoveReviewStrategyCoverageMeta],
      softRepairApplied: Boolean = false,
      softRepairActions: List[String] = Nil,
      softRepairMaterialApplied: Boolean = false,
      softRepairMaterialActions: List[String] = Nil
  )

  private def validateCandidate(
      candidateText: String,
      originalProse: String,
      allowedSans: List[String],
      anchors: MoveAnchorCodec.EncodedCommentary,
      extraReasons: List[String],
      strategyPack: Option[StrategyPack],
      planTier: String,
      commentaryMode: String,
      moveReviewSlots: Option[MoveReviewPolishSlots]
  ): CandidateValidation =
    val normalizedCandidate = unwrapCommentaryPayload(candidateText)
    val anchorReasons =
      if anchors.hasAnchors && moveReviewSlots.isEmpty then
        MoveAnchorCodec.validateAnchors(
          text = normalizedCandidate,
          expectedMoveOrder = anchors.expectedMoveOrder,
          expectedMarkerOrder = anchors.expectedMarkerOrder,
          expectedEvalOrder = anchors.expectedEvalOrder,
          expectedBranchOrder = anchors.expectedBranchOrder
        )
      else Nil
    val decoded =
      if anchors.hasAnchors then
        MoveAnchorCodec.decode(
          text = normalizedCandidate,
          refById = anchors.refById,
          evalById = anchors.evalById,
          branchById = anchors.branchById
        )
      else normalizedCandidate
    val repaired = moveReviewSlots match
      case Some(slots) =>
        MoveReviewSoftRepair.repair(decoded, slots)
      case None =>
        MoveReviewSoftRepair.RepairResult(
          text = decoded,
          applied = false,
          actions = Nil,
          materialApplied = false,
          materialActions = Nil,
          evaluation =
            MoveReviewProseContract.Evaluation(
              paragraphs = Nil,
              sentenceCount = 0,
              claimLikeFirstParagraph = true,
              paragraphBudgetOk = true,
              placeholderHits = Nil,
              genericHits = Nil
            )
        )
    val surfacedText = PlayerProseBoundary.sanitize(repaired.text)
    if repaired.applied then
      softRepairAppliedCount.incrementAndGet()
      lila.mon.commentary.commentary.repair("moveReview", "any").increment()
      logger.debug(s"commentary.polish.soft_repair applied=true actions=${repaired.actions.mkString(",")}")
    if repaired.materialApplied then
      softRepairMaterialCount.incrementAndGet()
      lila.mon.commentary.commentary.repair("moveReview", "material").increment()
      logOpsSample(
        kind = "material_repair.moveReview",
        "actions" -> repaired.materialActions.mkString(","),
        "lens" -> moveReviewSlots.map(_.lens.toString).getOrElse("")
      )
    val proseValidation =
      validatePolishedCommentary(surfacedText, originalProse, allowedSans, slotMode = moveReviewSlots.isDefined)
    val strategyValidation = evaluateStrategyCoverage(surfacedText, strategyPack, planTier, commentaryMode)
    val reasons =
      (anchorReasons ++ proseValidation.reasons ++ strategyValidation.reasons ++ extraReasons).distinct
    CandidateValidation(
      isValid = reasons.isEmpty,
      decodedText = surfacedText,
      reasons = reasons,
      strategyCoverage = strategyValidation.meta,
      softRepairApplied = repaired.applied,
      softRepairActions = repaired.actions,
      softRepairMaterialApplied = repaired.materialApplied,
      softRepairMaterialActions = repaired.materialActions
    )

  private def buildPolishMetaOpenAi(
      model: Option[String],
      sourceMode: String,
      phase: String,
      validationReasons: List[String],
      attempts: List[(String, OpenAiPolishResult)],
      strategyCoverage: Option[MoveReviewStrategyCoverageMeta]
  ): MoveReviewPolishMeta =
    val promptAttempts = attempts.map(_._2)
    MoveReviewPolishMeta(
      provider = "openai",
      model = model.orElse(promptAttempts.lastOption.map(_.model)),
      sourceMode = sourceMode,
      validationPhase = phase,
      validationReasons = validationReasons,
      cacheHit = promptAttempts.exists(_.promptCacheHit),
      promptTokens = sumIntOptions(promptAttempts.map(_.promptTokens)),
      cachedTokens = sumIntOptions(promptAttempts.map(_.cachedTokens)),
      completionTokens = sumIntOptions(promptAttempts.map(_.completionTokens)),
      estimatedCostUsd = sumDoubleOptions(promptAttempts.map(_.estimatedCostUsd)),
      strategyCoverage = strategyCoverage
    )

  private def buildPolishMetaGemini(
      model: Option[String],
      sourceMode: String,
      phase: String,
      validationReasons: List[String],
      strategyCoverage: Option[MoveReviewStrategyCoverageMeta]
  ): MoveReviewPolishMeta =
    MoveReviewPolishMeta(
      provider = "gemini",
      model = model,
      sourceMode = sourceMode,
      validationPhase = phase,
      validationReasons = validationReasons,
      cacheHit = false,
      promptTokens = None,
      cachedTokens = None,
      completionTokens = None,
      estimatedCostUsd = None,
      strategyCoverage = strategyCoverage
    )

  private def withRecordedPolishMetrics(startNs: Long, decision: PolishDecision): PolishDecision =
    recordPromptUsageAttempts(decision.promptUsages)
    recordPolishMetrics(
      sourceMode = decision.sourceMode,
      latencyMs = elapsedMs(startNs),
      cacheHit = decision.cacheHit,
      reasons = decision.meta.map(_.validationReasons).getOrElse(Nil),
      estimatedCostUsd = decision.meta.flatMap(_.estimatedCostUsd)
    )
    decision

  private def maybePolishBySegmentsOpenAi(
      prose: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String],
      salience: Option[lila.commentary.model.strategic.StrategicSalience],
      momentType: Option[String],
      lang: String,
      allowedSans: List[String],
      asyncTier: Boolean,
      strategyPack: Option[StrategyPack],
      planTier: String,
      commentaryMode: String
  ): Future[Option[PolishDecision]] =
    val segmentation = PolishSegmenter.segment(prose)
    val editable = segmentation.editableSegments
    if editable.isEmpty then Future.successful(None)
    else
      val segmentCap = adaptiveOutputTokenCap(prose, refs = None, asyncTier = asyncTier).min(if asyncTier then 520 else 420).max(192)
      val maxSegments = if asyncTier then 2 else 1
      val segmentPolishFamily = "segment_polish"
      val segmentRepairFamily = "segment_repair"
      val candidateEditable =
        editable
          .filter(seg => FullGameSegmentEligibility.isEligible(seg.text))
          .sortBy(s => -wordCount(s.text))
          .take(maxSegments)
      if candidateEditable.isEmpty then Future.successful(None)
      else
        final case class SegmentRewrite(
            id: String,
            original: String,
            commentary: String,
            attempts: List[(String, OpenAiPolishResult)]
        )

        final case class SegmentValidation(
            decodedText: String,
            text: Option[String],
            reasons: List[String]
        )

        def validateSegmentCandidate(
            candidate: OpenAiPolishResult,
            originalSegment: String,
            masked: PolishSegmenter.MaskedSegment
        ): SegmentValidation =
          val lockReasons =
            if masked.hasLocks then PolishSegmenter.validateLocks(candidate.commentary, masked.expectedOrder)
            else Nil
          val unmasked =
            if masked.hasLocks then PolishSegmenter.unmask(candidate.commentary, masked.tokenById)
            else candidate.commentary
          val surfaced = PlayerProseBoundary.sanitize(unmasked)
          val proseValidation = validatePolishedCommentary(surfaced, originalSegment, Nil)
          val reasons = (lockReasons ++ proseValidation.reasons ++ candidate.parseWarnings).distinct
          SegmentValidation(
            decodedText = surfaced,
            text = if reasons.isEmpty then Some(surfaced) else None,
            reasons = reasons
          )

        def polishSegment(seg: PolishSegmenter.Segment): Future[Option[SegmentRewrite]] =
          val masked = PolishSegmenter.maskStructuralTokens(seg.text)
          val modelInput = if masked.hasLocks then masked.maskedText else seg.text

          def callPolish(input: String): Future[Option[OpenAiPolishResult]] =
            if asyncTier then
              openAiClient.polishAsync(
                prose = input,
                phase = phase,
                evalDelta = evalDelta,
                concepts = concepts,
                fen = fen,
                openingName = openingName,
                salience = salience,
                momentType = momentType,
                lang = lang,
                maxOutputTokens = Some(segmentCap),
                planTier = planTier,
                commentaryMode = commentaryMode
              )
            else
              openAiClient.polishSync(
                prose = input,
                phase = phase,
                evalDelta = evalDelta,
                concepts = concepts,
                fen = fen,
                openingName = openingName,
                salience = salience,
                momentType = momentType,
                lang = lang,
                maxOutputTokens = Some(segmentCap),
                planTier = planTier,
                commentaryMode = commentaryMode
              )

          def callRepair(input: String, rejected: String): Future[Option[OpenAiPolishResult]] =
            if asyncTier then
              openAiClient.repairAsync(
                originalProse = input,
                rejectedPolish = rejected,
                phase = phase,
                evalDelta = evalDelta,
                concepts = concepts,
                fen = fen,
                openingName = openingName,
                allowedSans = Nil,
                lang = lang,
                maxOutputTokens = Some(segmentCap),
                planTier = planTier,
                commentaryMode = commentaryMode,
                segmentMode = true
              )
            else
              openAiClient.repairSync(
                originalProse = input,
                rejectedPolish = rejected,
                phase = phase,
                evalDelta = evalDelta,
                concepts = concepts,
                fen = fen,
                openingName = openingName,
                allowedSans = Nil,
                lang = lang,
                maxOutputTokens = Some(segmentCap),
                planTier = planTier,
                commentaryMode = commentaryMode,
                segmentMode = true
              )

          def fallbackToOriginal(attempts: List[(String, OpenAiPolishResult)]): SegmentRewrite =
            SegmentRewrite(
              seg.id,
              seg.text,
              PlayerProseBoundary.sanitize(seg.text),
              attempts
            )

          callPolish(modelInput).flatMap {
            case Some(primary) =>
              val primaryAttempts = List(segmentPolishFamily -> primary)
              val primaryValidation = validateSegmentCandidate(primary, seg.text, masked)
              primaryValidation.text match
                case Some(rewritten) =>
                  Future.successful(Some(SegmentRewrite(seg.id, seg.text, rewritten, primaryAttempts)))
                case None =>
                  recordFullGameInvalid(
                    stage = "segment_primary",
                    phase = phase,
                    model = Some(primary.model),
                    reasons = primaryValidation.reasons
                  )
                  recordFullGameInvalidPair(
                    stage = "segment_primary",
                    phase = phase,
                    model = Some(primary.model),
                    original = seg.text,
                    candidate = primaryValidation.decodedText,
                    reasons = primaryValidation.reasons
                  )
                  if !FullGamePolishRepairPolicy.shouldAttemptSegmentRepair(primaryValidation.reasons) then
                    recordFullGameRepairBypass(phase = phase, reasons = primaryValidation.reasons)
                    Future.successful(Some(fallbackToOriginal(primaryAttempts)))
                  else
                    recordFullGameRepairAttempt()
                    callRepair(modelInput, primary.commentary).map {
                      case Some(repaired) =>
                        val attempts = primaryAttempts :+ (segmentRepairFamily -> repaired)
                        val repairedValidation = validateSegmentCandidate(repaired, seg.text, masked)
                        repairedValidation.text
                          .map(rewritten => SegmentRewrite(seg.id, seg.text, rewritten, attempts))
                          .orElse {
                            recordFullGameInvalid(
                              stage = "segment_repair",
                              phase = phase,
                              model = Some(repaired.model),
                              reasons = repairedValidation.reasons
                            )
                            recordFullGameInvalidPair(
                              stage = "segment_repair",
                              phase = phase,
                              model = Some(repaired.model),
                              original = seg.text,
                              candidate = repairedValidation.decodedText,
                              reasons = repairedValidation.reasons
                            )
                            Some(fallbackToOriginal(attempts))
                          }
                      case None =>
                        // Prefer structural safety over aggressive rewrite: keep original segment.
                        Some(fallbackToOriginal(primaryAttempts))
                    }
            case None => Future.successful(None)
          }

        val rewritesFut =
          candidateEditable.foldLeft(Future.successful(Vector.empty[SegmentRewrite])) { (accFut, seg) =>
            accFut.flatMap(acc =>
              polishSegment(seg).map {
                case Some(rewrite) => acc :+ rewrite
                case None          => acc
              }
            )
          }

        rewritesFut.map { successes =>
          if successes.isEmpty then None
          else
            val rewrites = successes.map(s => s.id -> s.commentary).toMap
            val merged = segmentation.merge(rewrites)
            val mergedValidation = validatePolishedCommentary(merged, prose, allowedSans)
            val strategyValidation = evaluateStrategyCoverage(merged, strategyPack, planTier, commentaryMode)
            val mergedReasons = (mergedValidation.reasons ++ strategyValidation.reasons).distinct
            val attempts = successes.flatMap(_.attempts).toList
            if mergedReasons.nonEmpty then
              val softRepair =
                FullGameSegmentSoftRepair.repairMerged(
                  segmentation = segmentation,
                  rewrites =
                    successes.toList.map(rewrite =>
                      FullGameSegmentSoftRepair.Rewrite(
                        id = rewrite.id,
                        rewritten = rewrite.commentary,
                        original = rewrite.original
                      )
                    ),
                  originalProse = prose,
                  allowedSans = allowedSans
                )
              val repairedText = softRepair.text
              val repairedValidation = validatePolishedCommentary(repairedText, prose, allowedSans)
              val repairedStrategyValidation = evaluateStrategyCoverage(repairedText, strategyPack, planTier, commentaryMode)
              val repairedReasons =
                (repairedValidation.reasons ++ repairedStrategyValidation.reasons).distinct
              val observedReasons =
                if softRepair.applied then repairedReasons else mergedReasons

              recordFullGameInvalid(
                stage = "segment_merged",
                phase = phase,
                model = attempts.lastOption.map(_._2.model),
                reasons = observedReasons
              )
              recordFullGameInvalidPair(
                stage = "segment_merged",
                phase = phase,
                model = attempts.lastOption.map(_._2.model),
                original = prose,
                candidate = if softRepair.applied then repairedText else merged,
                reasons = observedReasons
              )

              if softRepair.applied then
                recordFullGameSoftRepair(
                  phase = phase,
                  actions = softRepair.actions,
                  reasons = observedReasons
                )

              if repairedReasons.isEmpty then
                Some(
                  PolishDecision(
                    commentary = repairedText,
                    sourceMode = "ai_polished",
                    model = attempts.lastOption.map(_._2.model),
                    cacheHit = attempts.exists(_._2.promptCacheHit),
                    meta = Some(
                      buildPolishMetaOpenAi(
                        model = attempts.lastOption.map(_._2.model),
                        sourceMode = "ai_polished",
                        phase = phase,
                        validationReasons = Nil,
                        attempts = attempts,
                        strategyCoverage = repairedStrategyValidation.meta
                      )
                    ),
                    promptUsages = attempts
                  )
                )
              else if !FullGamePolishRepairPolicy.shouldRetryWholeProseAfterMergedFailure(observedReasons, softRepair.applied) then
                recordFullGameMergedRetrySkipped(phase = phase, reasons = observedReasons)
                Some(
                  PolishDecision(
                    commentary = safeDeterministicFallbackProse(prose, None),
                    sourceMode = "fallback_rule_invalid",
                    model = None,
                    cacheHit = false,
                    meta = Some(
                      buildPolishMetaOpenAi(
                        model = attempts.lastOption.map(_._2.model),
                        sourceMode = "fallback_rule_invalid",
                        phase = phase,
                        validationReasons = observedReasons,
                        attempts = attempts,
                        strategyCoverage = repairedStrategyValidation.meta
                      )
                    ),
                    promptUsages = attempts
                  )
                )
              else
                recordPromptUsageAttempts(attempts)
                None
            else
              Some(
                PolishDecision(
                  commentary = merged,
                  sourceMode = "ai_polished",
                  model = attempts.lastOption.map(_._2.model),
                  cacheHit = attempts.exists(_._2.promptCacheHit),
                  meta = Some(
                    buildPolishMetaOpenAi(
                      model = attempts.lastOption.map(_._2.model),
                      sourceMode = "ai_polished",
                      phase = phase,
                      validationReasons = Nil,
                      attempts = attempts,
                      strategyCoverage = strategyValidation.meta
                    )
                  ),
                  promptUsages = attempts
                )
              )
        }

  private def maybePolishCommentary(
      prose: String,
      validationSeed: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      strategyHints: List[String],
      strategyPack: Option[StrategyPack],
      fen: String,
      openingName: Option[String],
      salience: Option[lila.commentary.model.strategic.StrategicSalience],
      allowAiPolish: Boolean,
      lang: String,
      allowedSans: List[String],
      asyncTier: Boolean,
      refs: Option[MoveReviewRefs],
      planTier: String,
      commentaryMode: String,
      momentType: Option[String] = None,
      moveReviewSlots: Option[MoveReviewPolishSlots],
      disablePolishCircuit: Boolean = false
  ): Future[PolishDecision] =
    val deterministicFallback =
      safeDeterministicFallbackProse(prose, moveReviewSlots)
    if prose.isBlank then
      Future.successful(
        PolishDecision(
          commentary = deterministicFallback,
          sourceMode = "rule",
          model = None,
          cacheHit = false,
          meta = None
        )
      )
    else if !isAiPolishEnabledForRequest(allowAiPolish) || providerConfig.isNone then
      Future.successful(
        PolishDecision(
          commentary = deterministicFallback,
          sourceMode = "rule",
          model = None,
          cacheHit = false,
          meta = None
        )
      )
    else if momentType.isEmpty && shouldSkipPolish(prose, lang) then
      Future.successful(
        PolishDecision(
          commentary = deterministicFallback,
          sourceMode = "rule",
          model = None,
          cacheHit = false,
          meta = None
        )
      )
    else if !disablePolishCircuit && isPolishCircuitOpen(System.currentTimeMillis()) then
      Future.successful(
        PolishDecision(
          commentary = deterministicFallback,
          sourceMode = "rule_circuit_open",
          model = None,
          cacheHit = false,
          meta = Some(
            MoveReviewPolishMeta(
              provider = providerConfig.provider,
              model = None,
              sourceMode = "rule_circuit_open",
              validationPhase = phase,
              validationReasons = List("circuit_open"),
              cacheHit = false,
              promptTokens = None,
              cachedTokens = None,
              completionTokens = None,
              estimatedCostUsd = None,
              strategyCoverage = evaluateStrategyCoverage(prose, strategyPack, planTier, commentaryMode).meta
            )
            )
          )
      )
    else
      val normalizedPlanTier = normalizePlanTier(planTier)
      val normalizedCommentaryMode = normalizeCommentaryMode(commentaryMode)
      val baselineCoverage =
        evaluateStrategyCoverage(prose, strategyPack, normalizedPlanTier, normalizedCommentaryMode).meta
      val promptConcepts = (concepts ++ strategyHints).map(_.trim).filter(_.nonEmpty).distinct.take(12)
      val polishFamilyBase = polishPromptFamilyBase(momentType)
      val primaryPromptFamily = s"${polishFamilyBase}_polish"
      val repairPromptFamily = s"${polishFamilyBase}_repair"
      providerConfig.provider match
        case "openai" if openAiClient.isEnabled =>
          val normalizedLang = normalizeLang(lang)
          val requestStartNs = System.nanoTime()
          val shouldTrySegmentPolishNow =
            shouldTrySegmentPolish(
              prose = prose,
              refs = refs,
              allowedSans = allowedSans,
              momentType = momentType,
              moveReviewSlots = moveReviewSlots
            )
          val segmentAttemptFut =
            if shouldTrySegmentPolishNow then
              maybePolishBySegmentsOpenAi(
                prose = prose,
                phase = phase,
                evalDelta = evalDelta,
                concepts = promptConcepts,
                fen = fen,
                openingName = openingName,
                salience = salience,
                momentType = momentType,
                lang = normalizedLang,
                allowedSans = allowedSans,
                asyncTier = asyncTier,
                strategyPack = strategyPack,
                planTier = normalizedPlanTier,
                commentaryMode = normalizedCommentaryMode
              )
            else Future.successful(None)

          segmentAttemptFut.flatMap {
            case Some(segmentDecision) =>
              Future.successful(withRecordedPolishMetrics(requestStartNs, segmentDecision))
            case None =>
              val anchorSeed = if moveReviewSlots.isDefined then validationSeed else prose
              val anchors = MoveAnchorCodec.encode(anchorSeed, refs)
              val slotsForModel =
                moveReviewSlots.map(slots => slots.withFactGuardrails(anchors.anchoredText.split('\n').toList))
              val proseForModel = if moveReviewSlots.isDefined then prose else if anchors.hasAnchors then anchors.anchoredText else prose
              val adaptiveCap = adaptiveOutputTokenCap(prose, refs, asyncTier)
              val op =
                if asyncTier then
                  openAiClient.polishAsync(
                    prose = proseForModel,
                    phase = phase,
                    evalDelta = evalDelta,
                    concepts = promptConcepts,
                    fen = fen,
                    openingName = openingName,
                    salience = salience,
                    momentType = momentType,
                    lang = normalizedLang,
                    maxOutputTokens = Some(adaptiveCap),
                    planTier = normalizedPlanTier,
                    commentaryMode = normalizedCommentaryMode,
                    moveReviewSlots = slotsForModel
                  )
                else
                  openAiClient.polishSync(
                    prose = proseForModel,
                    phase = phase,
                    evalDelta = evalDelta,
                    concepts = promptConcepts,
                    fen = fen,
                    openingName = openingName,
                    salience = salience,
                    momentType = momentType,
                    lang = normalizedLang,
                    maxOutputTokens = Some(adaptiveCap),
                    planTier = normalizedPlanTier,
                    commentaryMode = normalizedCommentaryMode,
                    moveReviewSlots = slotsForModel
                  )
              op.flatMap {
                case Some(polished) =>
                  val firstValidation = validateCandidate(
                    candidateText = polished.commentary,
                    originalProse = validationSeed,
                    allowedSans = allowedSans,
                    anchors = anchors,
                    extraReasons = polished.parseWarnings,
                    strategyPack = strategyPack,
                    planTier = normalizedPlanTier,
                    commentaryMode = normalizedCommentaryMode,
                    moveReviewSlots = moveReviewSlots
                  )
                  if firstValidation.isValid then
                    Future.successful(
                      withRecordedPolishMetrics(
                        requestStartNs,
                        PolishDecision(
                          commentary = firstValidation.decodedText,
                          sourceMode = "ai_polished",
                          model = Some(polished.model),
                          cacheHit = polished.promptCacheHit,
                          meta = Some(
                            buildPolishMetaOpenAi(
                              model = Some(polished.model),
                              sourceMode = "ai_polished",
                              phase = phase,
                              validationReasons = Nil,
                              attempts = List(primaryPromptFamily -> polished),
                              strategyCoverage = firstValidation.strategyCoverage
                            )
                          ),
                          promptUsages = List(primaryPromptFamily -> polished)
                        )
                      )
                    )
                  else
                    logInvalidPolish(
                      provider = "openai",
                      phase = phase,
                      attempt = "primary",
                      model = Some(polished.model),
                      reasons = firstValidation.reasons.distinct
                    )
                    if moveReviewSlots.isEmpty then
                      recordFullGameInvalidPair(
                        stage = "fullgame_primary",
                        phase = phase,
                        model = Some(polished.model),
                        original = validationSeed,
                        candidate = firstValidation.decodedText,
                        reasons = firstValidation.reasons.distinct
                      )
                    val repairFut =
                      if asyncTier then
                        openAiClient.repairAsync(
                          originalProse = validationSeed,
                          rejectedPolish = polished.commentary,
                          phase = phase,
                          evalDelta = evalDelta,
                          concepts = promptConcepts,
                          fen = fen,
                          openingName = openingName,
                          allowedSans = allowedSans,
                          lang = normalizedLang,
                          maxOutputTokens = Some(adaptiveCap),
                          planTier = normalizedPlanTier,
                          commentaryMode = normalizedCommentaryMode,
                          moveReviewSlots = slotsForModel
                        )
                      else
                        openAiClient.repairSync(
                          originalProse = validationSeed,
                          rejectedPolish = polished.commentary,
                          phase = phase,
                          evalDelta = evalDelta,
                          concepts = promptConcepts,
                          fen = fen,
                          openingName = openingName,
                          allowedSans = allowedSans,
                          lang = normalizedLang,
                          maxOutputTokens = Some(adaptiveCap),
                          planTier = normalizedPlanTier,
                          commentaryMode = normalizedCommentaryMode,
                          moveReviewSlots = slotsForModel
                        )
                    repairFut.map {
                      case Some(repaired) =>
                        val repairedValidation = validateCandidate(
                          candidateText = repaired.commentary,
                          originalProse = validationSeed,
                          allowedSans = allowedSans,
                          anchors = anchors,
                          extraReasons = repaired.parseWarnings,
                          strategyPack = strategyPack,
                          planTier = normalizedPlanTier,
                          commentaryMode = normalizedCommentaryMode,
                          moveReviewSlots = moveReviewSlots
                        )
                        if repairedValidation.isValid then
                          withRecordedPolishMetrics(
                            requestStartNs,
                            PolishDecision(
                              commentary = repairedValidation.decodedText,
                              sourceMode = "ai_polished",
                              model = Some(repaired.model),
                              cacheHit = repaired.promptCacheHit,
                              meta = Some(
                                buildPolishMetaOpenAi(
                                  model = Some(repaired.model),
                                  sourceMode = "ai_polished",
                                  phase = phase,
                                  validationReasons = Nil,
                                  attempts = List(primaryPromptFamily -> polished, repairPromptFamily -> repaired),
                                  strategyCoverage = repairedValidation.strategyCoverage
                                )
                              ),
                              promptUsages = List(primaryPromptFamily -> polished, repairPromptFamily -> repaired)
                            )
                          )
                        else
                          val reasons = repairedValidation.reasons.distinct
                          logInvalidPolish(
                            provider = "openai",
                            phase = phase,
                            attempt = "repair",
                            model = Some(repaired.model),
                            reasons = reasons
                          )
                          if moveReviewSlots.isEmpty then
                            recordFullGameInvalidPair(
                              stage = "fullgame_repair",
                              phase = phase,
                              model = Some(repaired.model),
                              original = validationSeed,
                              candidate = repairedValidation.decodedText,
                              reasons = reasons
                            )
                          withRecordedPolishMetrics(
                            requestStartNs,
                            PolishDecision(
                              commentary = deterministicFallback,
                              sourceMode = "fallback_rule_invalid",
                              model = None,
                              cacheHit = false,
                              meta = Some(
                                buildPolishMetaOpenAi(
                                  model = Some(repaired.model),
                                  sourceMode = "fallback_rule_invalid",
                                  phase = phase,
                                  validationReasons = reasons,
                                  attempts = List(primaryPromptFamily -> polished, repairPromptFamily -> repaired),
                                  strategyCoverage =
                                    repairedValidation.strategyCoverage.orElse(firstValidation.strategyCoverage)
                                )
                              ),
                              promptUsages = List(primaryPromptFamily -> polished, repairPromptFamily -> repaired)
                            )
                          )
                      case None =>
                        withRecordedPolishMetrics(
                        requestStartNs,
                        PolishDecision(
                          commentary = deterministicFallback,
                          sourceMode = "fallback_rule_invalid",
                          model = None,
                            cacheHit = false,
                              meta = Some(
                                buildPolishMetaOpenAi(
                                  model = Some(polished.model),
                                  sourceMode = "fallback_rule_invalid",
                                  phase = phase,
                                  validationReasons = firstValidation.reasons.distinct,
                                  attempts = List(primaryPromptFamily -> polished),
                                  strategyCoverage = firstValidation.strategyCoverage
                                )
                              ),
                              promptUsages = List(primaryPromptFamily -> polished)
                            )
                          )
                    }
                case None =>
                  Future.successful(
                    withRecordedPolishMetrics(
                      requestStartNs,
                      PolishDecision(
                        commentary = deterministicFallback,
                        sourceMode = "fallback_rule_empty",
                        model = None,
                        cacheHit = false,
                        meta = Some(
                          buildPolishMetaOpenAi(
                            model = None,
                            sourceMode = "fallback_rule_empty",
                            phase = phase,
                            validationReasons = List("empty_polish"),
                            attempts = Nil,
                            strategyCoverage = baselineCoverage
                          )
                        )
                      )
                    )
                  )
              }
          }
        case "gemini" if geminiClient.isEnabled =>
          val requestStartNs = System.nanoTime()
          val anchorSeed = if moveReviewSlots.isDefined then validationSeed else prose
          val anchors = MoveAnchorCodec.encode(anchorSeed, refs)
          val slotsForModel =
            moveReviewSlots.map(slots => slots.withFactGuardrails(anchors.anchoredText.split('\n').toList))
          val proseForModel = if moveReviewSlots.isDefined then prose else if anchors.hasAnchors then anchors.anchoredText else prose
          geminiClient
            .polish(
              prose = proseForModel,
              phase = phase,
              evalDelta = evalDelta,
              concepts = promptConcepts,
              fen = fen,
              openingName = openingName,
              salience = salience,
              momentType = momentType,
              moveReviewSlots = slotsForModel
            )
            .flatMap {
              case Some(polished) =>
                val validation = validateCandidate(
                  candidateText = polished,
                  originalProse = validationSeed,
                  allowedSans = allowedSans,
                  anchors = anchors,
                  extraReasons = Nil,
                  strategyPack = strategyPack,
                  planTier = normalizedPlanTier,
                  commentaryMode = normalizedCommentaryMode,
                  moveReviewSlots = moveReviewSlots
                )
                if validation.isValid then
                  Future.successful(
                    withRecordedPolishMetrics(
                      requestStartNs,
                      PolishDecision(
                        commentary = validation.decodedText,
                        sourceMode = "ai_polished",
                        model = Some(geminiClient.modelName),
                        cacheHit = false,
                        meta = Some(
                          buildPolishMetaGemini(
                            model = Some(geminiClient.modelName),
                            sourceMode = "ai_polished",
                            phase = phase,
                            validationReasons = Nil,
                            strategyCoverage = validation.strategyCoverage
                          )
                        )
                      )
                    )
                  )
                else
                  val primaryReasons = validation.reasons.distinct
                  logInvalidPolish(
                    provider = "gemini",
                    phase = phase,
                    attempt = "primary",
                    model = Some(geminiClient.modelName),
                    reasons = primaryReasons
                  )
                  if moveReviewSlots.isEmpty then
                    recordFullGameInvalidPair(
                      stage = "fullgame_primary",
                      phase = phase,
                      model = Some(geminiClient.modelName),
                      original = validationSeed,
                      candidate = validation.decodedText,
                      reasons = primaryReasons
                    )
                  geminiClient
                    .repair(
                      originalProse = validationSeed,
                      rejectedPolish = polished,
                      phase = phase,
                      evalDelta = evalDelta,
                      concepts = promptConcepts,
                      fen = fen,
                      openingName = openingName,
                      allowedSans = allowedSans,
                      moveReviewSlots = slotsForModel
                    )
                    .map {
                      case Some(repaired) =>
                        val repairedValidation = validateCandidate(
                          candidateText = repaired,
                          originalProse = validationSeed,
                          allowedSans = allowedSans,
                          anchors = anchors,
                          extraReasons = Nil,
                          strategyPack = strategyPack,
                          planTier = normalizedPlanTier,
                          commentaryMode = normalizedCommentaryMode,
                          moveReviewSlots = moveReviewSlots
                        )
                        if repairedValidation.isValid then
                          withRecordedPolishMetrics(
                            requestStartNs,
                            PolishDecision(
                              commentary = repairedValidation.decodedText,
                              sourceMode = "ai_polished",
                              model = Some(geminiClient.modelName),
                              cacheHit = false,
                              meta = Some(
                                buildPolishMetaGemini(
                                  model = Some(geminiClient.modelName),
                                  sourceMode = "ai_polished",
                                  phase = phase,
                                  validationReasons = Nil,
                                  strategyCoverage = repairedValidation.strategyCoverage
                                )
                              )
                            )
                          )
                        else
                          val reasons = repairedValidation.reasons.distinct
                          logInvalidPolish(
                            provider = "gemini",
                            phase = phase,
                            attempt = "repair",
                            model = Some(geminiClient.modelName),
                            reasons = reasons
                          )
                          if moveReviewSlots.isEmpty then
                            recordFullGameInvalidPair(
                              stage = "fullgame_repair",
                              phase = phase,
                              model = Some(geminiClient.modelName),
                              original = validationSeed,
                              candidate = repairedValidation.decodedText,
                              reasons = reasons
                            )
                          withRecordedPolishMetrics(
                            requestStartNs,
                            PolishDecision(
                              commentary = deterministicFallback,
                              sourceMode = "fallback_rule_invalid",
                              model = None,
                              cacheHit = false,
                              meta = Some(
                                buildPolishMetaGemini(
                                  model = Some(geminiClient.modelName),
                                  sourceMode = "fallback_rule_invalid",
                                  phase = phase,
                                  validationReasons = reasons,
                                  strategyCoverage =
                                    repairedValidation.strategyCoverage.orElse(validation.strategyCoverage)
                                )
                              )
                            )
                          )
                      case None =>
                        withRecordedPolishMetrics(
                          requestStartNs,
                          PolishDecision(
                            commentary = deterministicFallback,
                            sourceMode = "fallback_rule_invalid",
                            model = None,
                            cacheHit = false,
                            meta = Some(
                              buildPolishMetaGemini(
                                model = Some(geminiClient.modelName),
                                sourceMode = "fallback_rule_invalid",
                                phase = phase,
                                validationReasons = primaryReasons,
                                strategyCoverage = validation.strategyCoverage
                              )
                            )
                          )
                        )
                    }
              case None =>
                Future.successful(
                  withRecordedPolishMetrics(
                    requestStartNs,
                    PolishDecision(
                      commentary = deterministicFallback,
                      sourceMode = "fallback_rule_empty",
                      model = None,
                      cacheHit = false,
                      meta = Some(
                        buildPolishMetaGemini(
                          model = None,
                          sourceMode = "fallback_rule_empty",
                          phase = phase,
                          validationReasons = List("empty_polish"),
                          strategyCoverage = baselineCoverage
                        )
                      )
                    )
                )
                )
            }
        case _ =>
          Future.successful(
            PolishDecision(
              commentary = deterministicFallback,
              sourceMode = "rule",
              model = None,
              cacheHit = false,
              meta = None
            )
          )

  private def dedupeNarrativeSurface(
      prose: String,
      surface: String,
      role: String,
      priorityBase: Int,
      familyOverride: Option[NarrativeDedupCore.NarrativeClaimFamily] = None
  ): String =
    val sanitized = PlayerProseBoundary.sanitize(prose).trim
    if sanitized.isEmpty then sanitized
    else
      NarrativeDedupCore.dedupeStandaloneProse(
        prose = sanitized,
        surface = surface,
        role = role,
        priorityBase = priorityBase,
        familyOverride = familyOverride
      )

  private def dedupeMoveReviewCommentary(
      commentary: String
  ): String =
    LiveNarrativeCompressionCore.slotsFromCompressedProse(commentary).map { slots =>
      val claim =
        dedupeNarrativeSurface(
          prose = slots.claim,
          surface = "moveReview",
          role = "plan_lead",
          priorityBase = 100,
          familyOverride = Some(NarrativeDedupCore.NarrativeClaimFamily.PlanLead)
        )
      val support =
        slots.supportPrimary
          .map(text => dedupeNarrativeSurface(text, "moveReview", "support", 80))
          .filter(text => !NarrativeDedupCore.sameSemanticSentence(text, claim))
      val third =
        slots.evidenceHook
          .orElse(slots.tension)
          .map(text => dedupeNarrativeSurface(text, "moveReview", "support", 70))
          .filter(text =>
            !NarrativeDedupCore.sameSemanticSentence(text, claim) &&
              support.forall(other => !NarrativeDedupCore.sameSemanticSentence(text, other))
          )
      val evidenceHook = third.filter(LineScopedCitation.hasConcreteSanLine)
      val tension = third.filterNot(LineScopedCitation.hasConcreteSanLine)
      val paragraphPlan =
        "p1=claim" :: support.map(_ => "p2=support").toList :::
          evidenceHook.map(_ => "p3=cited_line").orElse(tension.map(_ => "p3=practical_nuance")).toList

      LiveNarrativeCompressionCore.deterministicProse(
        slots.copy(
          claim = claim,
          supportPrimary = support,
          supportSecondary = None,
          evidenceHook = evidenceHook,
          tension = tension,
          factGuardrails = evidenceHook.toList,
          paragraphPlan = paragraphPlan
        )
      )
    }.getOrElse(dedupeNarrativeSurface(commentary, "moveReview", "plan_lead", 80))

  private def dedupeMoveReviewResponse(
      response: CommentResponse
  ): CommentResponse =
    response.copy(
      commentary = dedupeMoveReviewCommentary(response.commentary)
    )

  private def fenAfterEachMove(startFen: String, ucis: List[String]): List[String] =
    var current = startFen
    ucis.map { uci =>
      current = NarrativeUtils.uciListToFen(current, List(uci))
      current
    }

  private def markerForPly(ply: Int): String =
    val moveNo = (ply + 1) / 2
    if ply % 2 == 1 then s"$moveNo."
    else s"$moveNo..."

  private def buildMoveReviewRefs(
      fenBefore: String,
      variations: List[VariationLine]
  ): Option[MoveReviewRefs] =
    if variations.isEmpty then None
    else
      val startPly = NarrativeUtils.plyFromFen(fenBefore).map(_ + 1).getOrElse(1)
      val lines = variations.zipWithIndex.map { case (line, lineIdx) =>
        val sanList = NarrativeUtils.uciListToSan(fenBefore, line.moves)
        val uciList = line.moves.take(sanList.size)
        val fensAfter = fenAfterEachMove(fenBefore, uciList).take(sanList.size)
        val size = List(sanList.size, uciList.size, fensAfter.size).min
        val moves = (0 until size).toList.map { i =>
          val ply = startPly + i
          val moveNo = (ply + 1) / 2
          MoveReviewMoveRef(
            refId = f"l${lineIdx + 1}%02d_m${i + 1}%02d",
            san = sanList(i),
            uci = uciList(i),
            fenAfter = fensAfter(i),
            ply = ply,
            moveNo = moveNo,
            marker = Some(markerForPly(ply))
          )
        }
        MoveReviewVariationRef(
          lineId = f"line_${lineIdx + 1}%02d",
          scoreCp = line.scoreCp,
          mate = line.mate,
          depth = line.depth,
          moves = moves
        )
      }
      Some(
        MoveReviewRefs(
          startFen = fenBefore,
          startPly = startPly,
          variations = lines
        )
      )


  def fetchOpeningMasters(fen: String): Future[Option[OpeningReference]] =
    openingExplorer.fetchMasters(fen)


  def fetchOpeningMasterPgn(gameId: String): Future[Option[String]] =
    openingExplorer.fetchMasterPgn(gameId)

  /** Generate move-review commentary (rule-based). */
  def moveReviewPosition(
      fen: String,
      lastMove: Option[String],
      eval: Option[EvalData],
      variations: Option[List[VariationLine]],
      probeResults: Option[List[lila.commentary.model.ProbeResult]] = None,
      openingData: Option[OpeningReference] = None,
      afterFen: Option[String] = None,
      afterEval: Option[EvalData] = None,
      afterVariations: Option[List[VariationLine]] = None,
      opening: Option[String],
      phase: String,
      ply: Int,
      variant: Option[String] = None,
      prevStateToken: Option[lila.commentary.analysis.PlanStateTracker] = None,
      prevEndgameStateToken: Option[lila.commentary.model.strategic.EndgamePatternState] = None,
      allowAiPolish: Boolean = false,
      lang: String = "en",
      planTier: String = PlanTier.Basic
  ): Future[Option[MoveReviewResult]] =
    val requestStartNs = System.nanoTime()
    val normalizedPlanTier = normalizePlanTier(planTier)
    val effectiveLevel = CommentaryMode.Polish
    val resolvedPly = NarrativeUtils.resolveAnnotationPly(fen, lastMove, ply)
    val incomingProbes = probeResults.getOrElse(Nil)
    val cacheCtx =
      commentaryCacheContext(
        allowAiPolish = allowAiPolish,
        lang = lang,
        asyncTier = false,
        planTier = normalizedPlanTier,
        commentaryMode = effectiveLevel
      )
    moveReviewRequests.incrementAndGet()
    if prevStateToken.isDefined || prevEndgameStateToken.isDefined then tokenPresentCount.incrementAndGet()
    val shouldFetchMasters =
      phase.trim.equalsIgnoreCase("opening") &&
        resolvedPly >= OpeningRefMinPly &&
        resolvedPly <= OpeningRefMaxPly
    val openingRefFuture =
      if openingData.isDefined then Future.successful(Some(openingExplorer.enrichWithLocalPgn(fen, openingData.get)))
      else if shouldFetchMasters then openingExplorer.fetchMasters(fen)
      else Future.successful(None)

    openingRefFuture.flatMap { openingRef =>
      val openingCacheFingerprint = CommentaryCache.openingFingerprint(openingRef)
      val moveReviewCacheKey =
        commentaryCache.key(fen, lastMove, incomingProbes, prevStateToken, prevEndgameStateToken, cacheCtx, openingCacheFingerprint)
      commentaryCache.get(moveReviewCacheKey) match
        case Some(cached) =>
          val sanitizedCached =
            dedupeMoveReviewResponse(
              UserFacingPayloadSanitizer.sanitizeCachedMoveReview(cached)
            )
          stateAwareCacheHitCount.incrementAndGet()
          logger.debug(s"Cache hit: ${fen.take(20)}...")
          traceMoveReviewFallbackResponse(
            response = sanitizedCached,
            phase = phase,
            ply = resolvedPly,
            lastMove = lastMove,
            cacheHit = true,
            openingRefPresent = openingRef.isDefined,
            incomingProbeCount = incomingProbes.size
          )
          recordLatencyMetrics(totalLatencyMs = elapsedMs(requestStartNs), structureEvalLatencyMs = None)
          maybeLogMoveReviewMetrics()
          Future.successful(Some(MoveReviewResult(response = sanitizedCached, cacheHit = true)))
        case None =>
          stateAwareCacheMissCount.incrementAndGet()
          computeMoveReviewResponse(
            fen, lastMove, eval, variations, probeResults, openingRef,
            afterFen, afterEval, afterVariations, opening, phase, ply, variant, prevStateToken, prevEndgameStateToken, allowAiPolish, lang, moveReviewCacheKey, normalizedPlanTier
          ).map:
            case (responseOpt, structEvalMsOpt) =>
            recordLatencyMetrics(totalLatencyMs = elapsedMs(requestStartNs), structureEvalLatencyMs = structEvalMsOpt)
            maybeLogMoveReviewMetrics()
            responseOpt
    }
        


  private def computeMoveReviewResponse(
      fen: String,
      lastMove: Option[String],
      eval: Option[EvalData],
      variations: Option[List[VariationLine]],
      probeResults: Option[List[lila.commentary.model.ProbeResult]],
      openingRef: Option[OpeningReference],
      afterFen: Option[String],
      afterEval: Option[EvalData],
      afterVariations: Option[List[VariationLine]],
      opening: Option[String],
      phase: String,
      ply: Int,
      variant: Option[String],
      prevStateToken: Option[lila.commentary.analysis.PlanStateTracker],
      prevEndgameStateToken: Option[lila.commentary.model.strategic.EndgamePatternState],
      allowAiPolish: Boolean,
      lang: String,
      cacheKey: CommentaryCache.Key,
      planTier: String
  ): Future[(Option[MoveReviewResult], Option[Long])] =
    val commentaryMode = CommentaryMode.Polish
    val effectivePly = NarrativeUtils.resolveAnnotationPly(fen, lastMove, ply)
    val normalizedVariant = EarlyOpeningNarrationPolicy.normalizeVariantKey(variant)

    val varsFromEval =
      eval.flatMap(_.pv).filter(_.nonEmpty).map { pv =>
        List(
          VariationLine(
            moves = pv,
            scoreCp = eval.map(_.cp).getOrElse(0),
            mate = eval.flatMap(_.mate),
            depth = 0
          )
        )
      }

    val vars = variations.filter(_.nonEmpty).orElse(varsFromEval).getOrElse(Nil)

    val afterVarsFromEval =
      afterEval.flatMap(_.pv).filter(_.nonEmpty).map { pv =>
        List(
          VariationLine(
            moves = pv,
            scoreCp = afterEval.map(_.cp).getOrElse(0),
            mate = afterEval.flatMap(_.mate),
            depth = 0
          )
        )
      }

    val afterVars = afterVariations.filter(_.nonEmpty).orElse(afterVarsFromEval).getOrElse(Nil)
    if vars.isEmpty then Future.successful(None -> None)
    else
        val isWhiteTurn = fen.contains(" w ")
        val movingColor = if (isWhiteTurn) _root_.chess.Color.White else _root_.chess.Color.Black
        val tracker = prevStateToken.getOrElse(lila.commentary.analysis.PlanStateTracker.empty)

        val dataOpt = CommentaryEngine.assessExtended(
          fen = fen,
          variations = vars,
          playedMove = lastMove,
          opening = opening,
          phase = Some(phase),
          ply = effectivePly,
          prevMove = lastMove,
          prevPlanContinuity = tracker.getContinuity(movingColor),
          prevEndgameState = prevEndgameStateToken,
          probeResults = probeResults.getOrElse(Nil)
        )

        dataOpt match
          case None => Future.successful(None -> None)
          case Some(data) =>
            if tracker.getColorState(movingColor).lastPly.contains(effectivePly) then
              samePlyIdempotentHitCount.incrementAndGet()

            val nextTracker = tracker.update(
              movingColor = movingColor,
              ply = effectivePly,
              primaryPlan = data.plans.headOption,
              secondaryPlan = data.plans.lift(1),
              sequence = data.planSequence
            )
            val nextEndgameState =
              lila.commentary.model.strategic.EndgamePatternState.evolve(
                prevEndgameStateToken,
                data.endgameFeatures,
                effectivePly
              )
            val dataWithContinuity = data.copy(planContinuity = nextTracker.getContinuity(movingColor))
            if dataWithContinuity.planContinuity.exists(_.consecutivePlies >= 2) then
              continuityAppliedCount.incrementAndGet()
            dataWithContinuity.planSequence.foreach(ps => incTransition(ps.transitionType.toString))
            if commentaryConfig.shouldEvaluateStructureKb then recordStructureMetrics(dataWithContinuity)
            recordEndgameMetrics(dataWithContinuity)

            val afterDataOpt =
              afterFen
                .filter(_.nonEmpty)
                .filter(_ => afterVars.nonEmpty)
                .flatMap { f =>
                  CommentaryEngine.assessExtended(
                    fen = f,
                    variations = afterVars,
                    playedMove = None,
                    opening = opening,
                    phase = Some(phase),
                    ply = effectivePly,
                    prevMove = None
                  )
                }

            val contextBuild =
              NarrativeContextBuilder.buildWithDiagnostics(
                data = dataWithContinuity,
                ctx = dataWithContinuity.toContext,
                probeResults = probeResults.getOrElse(Nil),
                openingRef = openingRef,
                afterAnalysis = afterDataOpt,
                renderMode = lila.commentary.model.NarrativeRenderMode.MoveReview,
                variantKey = normalizedVariant
              )
            val rawCtx = contextBuild.context
            recordStrategicMetrics(
              data = dataWithContinuity,
              ctx = rawCtx,
              probeResults = probeResults.getOrElse(Nil),
              diagnosticPlanSidecar = contextBuild.diagnosticPlanSidecar
            )
            val rawStrategyPack = StrategyPackBuilder.build(dataWithContinuity, rawCtx)
            val truthContract =
              DecisiveTruth.derive(
                ctx = rawCtx,
                transitionType = dataWithContinuity.planSequence.map(_.transitionType.toString),
                strategyPack = rawStrategyPack
              )
            val ctx = DecisiveTruth.sanitizeContext(rawCtx, truthContract)
            val strategyPack =
              DecisiveTruth.sanitizeStrategyPack(
                rawStrategyPack,
                truthContract
              )
            val moveReviewRefVariations =
              CommentaryApi.appendMoveReviewAfterPvProofVariation(
                fenBefore = fen,
                lastMove = lastMove,
                afterFen = afterFen,
                base = dataWithContinuity.alternatives,
                afterVars = afterVars
              )
            val refs = buildMoveReviewRefs(fen, moveReviewRefVariations)
            val outline = BookStyleRenderer.validatedOutline(ctx, Some(truthContract), strategyPack)
            val moveReviewRuntime =
              MoveReviewCompressionPolicy.buildSlotsOrFallbackWithRuntime(ctx, outline, refs, strategyPack, Some(truthContract))
            val moveReviewSlots = moveReviewRuntime.slots
            val moveReviewExplanation = moveReviewSlots.moveReviewExplanation
            val proseRaw = LiveNarrativeCompressionCore.deterministicProse(moveReviewSlots)
            val prose = CommentaryApi.sanitizeMoveReviewProse(proseRaw)
            val compactProse = EarlyOpeningNarrationPolicy.clampNarrative(ctx, prose, Some(truthContract))
            val baseConcepts = ctx.semantic.map(_.conceptSummary).getOrElse(Nil)
            val allowedSans =
              moveReviewRefVariations.flatMap(v => NarrativeUtils.uciListToSan(fen, v.moves))
            val moveReviewLedger = MoveReviewStrategicLedgerBuilder.build(
              ctx = ctx,
              strategyPack = strategyPack,
              refs = refs,
              probeResults = probeResults.getOrElse(Nil),
              planStateToken = prevStateToken,
              endgameStateToken = prevEndgameStateToken
            )
            val authoringSurface = AuthoringEvidenceSummaryBuilder.build(ctx)
            val outboundProbeRequests = if probeResults.exists(_.nonEmpty) then Nil else ctx.probeRequests
            val supportedLocalRows =
              MoveReviewSupportedLocalSurfaceRows.build(
                ctx = ctx,
                inputs = moveReviewRuntime.inputs,
                rankedPlans = moveReviewRuntime.rankedPlans,
                truthContract = Some(truthContract)
              )
            val decisionComparisonSurface =
              MoveReviewPlayerPayloadBuilder.decisionComparisonSurface(ctx, refs)
            val playerSurfaceEvaluatedPlans =
              if ctx.strategicPlanEvidence.evaluatedPlans.nonEmpty then ctx.strategicPlanEvidence.evaluatedPlans
              else contextBuild.selectedMainEvaluatedPlans
            val moveReviewPlayerSurface =
              MoveReviewPlayerPayloadBuilder.build(
                ctx = ctx,
                moveReviewExplanation = moveReviewExplanation,
                moveReviewLedger = moveReviewLedger,
                refs = refs,
                evaluatedPlans = playerSurfaceEvaluatedPlans,
                authoringSurface = authoringSurface,
                supportedLocalRows = supportedLocalRows,
                decisionComparisonSurface = decisionComparisonSurface,
                strategyPack = strategyPack,
                truthContract = Some(truthContract)
              )
            val strategyPromptHints = strategyHints(strategyPack)
            val validationSeed = Option(moveReviewSlots.validationSeedText).filter(_.nonEmpty).getOrElse(compactProse)

            maybePolishCommentary(
              prose = compactProse,
              validationSeed = validationSeed,
              phase = phase,
              evalDelta = None,
              concepts = baseConcepts,
              strategyHints = strategyPromptHints,
              strategyPack = strategyPack,
              fen = fen,
              openingName = opening,
              salience = Some(dataWithContinuity.strategicSalience),
              allowAiPolish = allowAiPolish,
              lang = lang,
              allowedSans = allowedSans,
              asyncTier = false,
              refs = refs,
              planTier = planTier,
              commentaryMode = commentaryMode,
              moveReviewSlots = Some(moveReviewSlots)
            ).map { decision =>
              val response =
                dedupeMoveReviewResponse(
                  UserFacingPayloadSanitizer.sanitize(
                    CommentResponse(
                      commentary = EarlyOpeningNarrationPolicy.clampNarrative(ctx, decision.commentary, Some(truthContract)),
                      concepts = baseConcepts,
                      variations = dataWithContinuity.alternatives,
                      probeRequests = outboundProbeRequests,
                      authorQuestions = authoringSurface.questions,
                      authorEvidence = authoringSurface.evidence,
                      mainStrategicPlans = ctx.mainStrategicPlans,
                      strategicPlanExperiments =
                        ctx.strategicPlanExperiments.filter(experiment =>
                          ctx.mainStrategicPlans.exists(plan =>
                            plan.planId.equalsIgnoreCase(experiment.planId) &&
                              plan.subplanId.getOrElse("").equalsIgnoreCase(experiment.subplanId.getOrElse(""))
                          )
                        ),
                      planStateToken = Some(nextTracker),
                      endgameStateToken = nextEndgameState,
                      sourceMode = decision.sourceMode,
                      model = decision.model,
                      refs = refs,
                      polishMeta = decision.meta,
                      planTier = planTier,
                      commentaryMode = commentaryMode,
                      strategyPack = strategyPack,
                      signalDigest = strategyPack.flatMap(_.signalDigest),
                      moveReviewLedger = moveReviewLedger,
                      moveReviewExplanation = moveReviewExplanation,
                      moveReviewPlayerSurface = Some(moveReviewPlayerSurface)
                    ),
                    admittedPlans = contextBuild.selectedMainEvaluatedPlans
                  )
                )
              recordComparisonObservation(
                path = "moveReview",
                digest = response.signalDigest.flatMap(_.decisionComparison),
                authorEvidence = response.authorEvidence,
                probeRequests = response.probeRequests,
                strategyPack = response.strategyPack
              )
              traceMoveReviewFallbackResponse(
                response = response,
                phase = phase,
                ply = effectivePly,
                lastMove = lastMove,
                cacheHit = decision.cacheHit,
                openingRefPresent = openingRef.isDefined,
                incomingProbeCount = probeResults.getOrElse(Nil).size
              )
              if response.planStateToken.isDefined then tokenEmitCount.incrementAndGet()
              commentaryCache.put(cacheKey, response)
              Some(
                MoveReviewResult(
                  response = response,
                  cacheHit = decision.cacheHit,
                  diagnosticPlanSidecar = Some(contextBuild.diagnosticPlanSidecar)
                )
              ) -> dataWithContinuity.structureEvalLatencyMs
            }


private[commentary] object CommentaryApi:

  private def collapseRepeatedDots(input: String): String =
    @annotation.tailrec
    def loop(text: String): String =
      if text.contains("..") then loop(text.replace("..", "."))
      else text
    loop(input)

  private[commentary] def sanitizeMoveReviewProse(text: String): String =
    val src = Option(text).getOrElse("")
    UserFacingSignalSanitizer.sanitize(
      collapseRepeatedDots(src)
        .replaceAll(""":\s*:""", ": ")
        .replaceAll("""\.\s*:""", ". ")
        .replaceAll("""\s+\.""", ".")
    )

  private[commentary] def appendMoveReviewAfterPvProofVariation(
      fenBefore: String,
      lastMove: Option[String],
      afterFen: Option[String],
      base: List[VariationLine],
      afterVars: List[VariationLine]
  ): List[VariationLine] =
    val normalizedPlayed =
      lastMove.map(MoveReviewPvLine.normalizeUci).filter(_.matches("^[a-h][1-8][a-h][1-8][qrbn]?$"))
    normalizedPlayed match
      case None => base
      case Some(played) if base.exists(line => line.moves.headOption.exists(move => MoveReviewPvLine.normalizeUci(move) == played) && line.moves.size >= 2) =>
        base
      case Some(played) =>
        val legalAfterMatches =
          for
            declaredAfter <- afterFen.map(_.trim).filter(_.nonEmpty)
            actualAfter <- MoveReviewPvLine.legalFenAfter(fenBefore, played)
          yield sameBoardStateFen(actualAfter, declaredAfter)
        val proofLine =
          for
            afterLine <- afterVars.headOption
            if legalAfterMatches.contains(true)
            normalizedTail = afterLine.moves.map(MoveReviewPvLine.normalizeUci).filter(_.nonEmpty)
            tail = if normalizedTail.headOption.contains(played) then normalizedTail.drop(1) else normalizedTail
            if tail.nonEmpty
          yield afterLine.copy(moves = played :: tail)
        proofLine.map(line => base :+ line).getOrElse(base)

  private def sameBoardStateFen(left: String, right: String): Boolean =
    def boardState(fen: String): String =
      Option(fen).getOrElse("").trim.split("\\s+").filter(_.nonEmpty).take(4).mkString(" ")
    boardState(left) == boardState(right)
