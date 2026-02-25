package lila.llm

import scala.concurrent.Future
import scala.util.control.NonFatal
import java.util.concurrent.atomic.AtomicLong
import java.time.Instant
import java.util.UUID
import lila.llm.analysis.{ BookStyleRenderer, CommentaryEngine, NarrativeContextBuilder, NarrativeLexicon, NarrativeUtils, OpeningExplorerClient, PlanEvidenceEvaluator }
import lila.llm.analysis.NarrativeLexicon.Style
import lila.llm.model.{ FullGameNarrative, OpeningReference }
import lila.llm.model.structure.StructureId
import lila.llm.model.strategic.{ VariationLine, TheoreticalOutcomeHint }

/** Pipeline: CommentaryEngine → BookStyleRenderer (rule-based only). */
final class LlmApi(
    openingExplorer: OpeningExplorerClient,
    geminiClient: GeminiClient,
    openAiClient: OpenAiClient,
    commentaryCache: CommentaryCache,
    llmConfig: LlmConfig = LlmConfig.fromEnv,
    providerConfig: LlmProviderConfig = LlmProviderConfig.fromEnv
)(using Executor):

  private val logger = lila.log("llm.api")
  private val OpeningRefMinPly = 3
  private val OpeningRefMaxPly = 24
  private val bookmakerRequests = new AtomicLong(0L)
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
  private val latentPrecisionTotal = new AtomicLong(0L)
  private val latentPrecisionHit = new AtomicLong(0L)
  private val pvCouplingTotal = new AtomicLong(0L)
  private val pvCouplingHit = new AtomicLong(0L)
  private val evidenceReasonTotal = new AtomicLong(0L)
  private val evidenceReasonHit = new AtomicLong(0L)
  private val hypothesisProbeTotal = new AtomicLong(0L)
  private val hypothesisProbeHit = new AtomicLong(0L)
  private val contractProbeTotal = new AtomicLong(0L)
  private val contractProbeDropped = new AtomicLong(0L)
  private val probeFenTotal = new AtomicLong(0L)
  private val probeFenMissing = new AtomicLong(0L)
  private val ShadowWindowSize = 2000
  private val AsyncJobTtlMs = 24L * 60L * 60L * 1000L
  private val AsyncJobMaxSize = 256
  private val latencyWindowLock = new Object
  private var totalLatencyWindowMs = Vector.empty[Long]
  private var structureEvalLatencyWindowMs = Vector.empty[Long]
  private val asyncJobs = scala.collection.concurrent.TrieMap.empty[String, AsyncGameAnalysisStatusResponse]
  private val leakTokens = List("PA_MATCH", "PRECOND_MISS", "REQ_", "SUP_", "BLK_")
  private val PolishWindowSize = 200
  private val PolishCircuitCooldownMs = 10L * 60L * 1000L
  private val polishWindowLock = new Object
  private var polishLatencyWindowMs = Vector.empty[Long]
  private var polishFallbackWindow = Vector.empty[Boolean]
  private var polishCircuitOpenUntilMs = 0L
  private val polishAttemptCount = new AtomicLong(0L)
  private val polishAcceptedCount = new AtomicLong(0L)
  private val polishFallbackCount = new AtomicLong(0L)
  private val polishCacheHitCount = new AtomicLong(0L)
  private val polishReasonCount = scala.collection.concurrent.TrieMap.empty[String, AtomicLong]
  private val polishEstimatedCostMicros = new AtomicLong(0L)

  private case class PolishDecision(
      commentary: String,
      sourceMode: String,
      model: Option[String],
      cacheHit: Boolean,
      meta: Option[PolishMetaV1]
  )

  private def incTransition(kind: String): Unit =
    transitionCountByType.getOrElseUpdate(kind, new AtomicLong(0L)).incrementAndGet()

  private def incStructure(kind: String): Unit =
    structureCountByType.getOrElseUpdate(kind, new AtomicLong(0L)).incrementAndGet()

  private def incBand(kind: String): Unit =
    alignmentBandCount.getOrElseUpdate(kind, new AtomicLong(0L)).incrementAndGet()

  private def recordStructureMetrics(data: lila.llm.model.ExtendedAnalysisData): Unit =
    data.structureProfile.foreach { profile =>
      structureProfileCount.incrementAndGet()
      incStructure(profile.primary.toString)
      if profile.primary == StructureId.Unknown then structureUnknownCount.incrementAndGet()
      if profile.confidence < llmConfig.structKbMinConfidence then structureLowConfidenceCount.incrementAndGet()
    }
    data.planAlignment.foreach(pa => incBand(pa.band.toString))

  private def recordEndgameMetrics(data: lila.llm.model.ExtendedAnalysisData): Unit =
    data.endgameFeatures.foreach { eg =>
      endgameFeatureCount.incrementAndGet()
      if eg.theoreticalOutcomeHint == TheoreticalOutcomeHint.Win then endgameWinHintCount.incrementAndGet()
      if eg.confidence >= 0.75 then endgameHighConfidenceCount.incrementAndGet()
    }

  private def isHypothesisProbeRequest(req: lila.llm.model.ProbeRequest): Boolean =
    req.seedId.exists(_.trim.nonEmpty) ||
      req.purpose.exists { purpose =>
        purpose == "free_tempo_branches" ||
        purpose == "latent_plan_immediate" ||
        purpose == "latent_plan_refutation"
      }

  private def recordStrategicMetrics(
      data: lila.llm.model.ExtendedAnalysisData,
      ctx: lila.llm.model.NarrativeContext,
      probeResults: List[lila.llm.model.ProbeResult]
  ): Unit =
    if ctx.mainStrategicPlans.nonEmpty then
      planRecallTotal.incrementAndGet()
      val topRulePlan = data.plans.headOption.map(_.plan.id.toString)
      val topHypothesis = ctx.mainStrategicPlans.headOption.map(_.planId)
      if topRulePlan.isDefined && topRulePlan == topHypothesis then planRecallHit.incrementAndGet()

    val latent = ctx.latentPlans
    if latent.nonEmpty then
      latentPrecisionTotal.addAndGet(latent.size.toLong)
      latent.foreach { lp =>
        val support = probeResults.exists { pr =>
          val seedOk = pr.seedId.contains(lp.seedId) || pr.id.contains(lp.seedId)
          val purposeOk = pr.purpose.exists(p => p == "free_tempo_branches" || p == "latent_plan_immediate")
          seedOk && purposeOk && pr.deltaVsBaseline >= -80
        }
        if support then latentPrecisionHit.incrementAndGet()
      }

    pvCouplingTotal.incrementAndGet()
    if ctx.whyAbsentFromTopMultiPV.isEmpty then pvCouplingHit.incrementAndGet()

    if ctx.whyAbsentFromTopMultiPV.nonEmpty then
      evidenceReasonTotal.incrementAndGet()
      if ctx.absentReasonSource == "evidence" then evidenceReasonHit.incrementAndGet()

    if probeResults.nonEmpty then
      probeFenTotal.addAndGet(probeResults.size.toLong)
      probeFenMissing.addAndGet(probeResults.count(_.fen.isEmpty).toLong)

    val rootProbeResultsRaw = probeResults.filter(pr => pr.fen.forall(_ == data.fen))
    if rootProbeResultsRaw.nonEmpty then
      val validation = PlanEvidenceEvaluator.validateProbeResults(rootProbeResultsRaw, ctx.probeRequests)

      contractProbeTotal.addAndGet(rootProbeResultsRaw.size.toLong)
      contractProbeDropped.addAndGet(validation.droppedCount.toLong)

      val hypothesisRequests = ctx.probeRequests.filter(isHypothesisProbeRequest)
      if hypothesisRequests.nonEmpty then
        val validIds = validation.validResults.map(_.id).toSet
        hypothesisProbeTotal.addAndGet(hypothesisRequests.size.toLong)
        hypothesisProbeHit.addAndGet(hypothesisRequests.count(req => validIds.contains(req.id)).toLong)

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

  private def recordPolishMetrics(
      sourceMode: String,
      latencyMs: Long,
      cacheHit: Boolean,
      reasons: List[String],
      estimatedCostUsd: Option[Double]
  ): Unit =
    val fallback = sourceMode.startsWith("fallback_rule")
    polishAttemptCount.incrementAndGet()
    if sourceMode == "llm_polished" then polishAcceptedCount.incrementAndGet()
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

  private def recordLatencyMetrics(totalLatencyMs: Long, structureEvalLatencyMs: Option[Long]): Unit =
    latencyWindowLock.synchronized {
      totalLatencyWindowMs = pushWindow(totalLatencyWindowMs, totalLatencyMs.max(0L))
      structureEvalLatencyMs.foreach { ms =>
        structureEvalLatencyWindowMs = pushWindow(structureEvalLatencyWindowMs, ms.max(0L))
      }
    }

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
    if llmConfig.structKbEnabled then "enabled"
    else if llmConfig.structKbShadowMode then "shadow"
    else "off"

  private def endgameMode: String =
    if llmConfig.endgameOracleEnabled then "enabled"
    else if llmConfig.endgameOracleShadowMode then "shadow"
    else "off"

  private def strategicPriorMode: String =
    if llmConfig.strategicPriorEnabled then "enabled"
    else if llmConfig.strategicPriorCanaryRate > 0.0 then "canary"
    else if llmConfig.strategicPriorShadowMode then "shadow"
    else "off"

  private def maybeLogBookmakerMetrics(): Unit =
    val total = bookmakerRequests.get()
    if total > 0 && total % 100 == 0 then
      val tokenPresentRate = tokenPresentCount.get().toDouble / total.toDouble
      val tokenEmitRate = tokenEmitCount.get().toDouble / total.toDouble
      val continuityAppliedRate = continuityAppliedCount.get().toDouble / total.toDouble
      val transitionDist = transitionCountByType.toList.map { case (k, v) => k -> v.get() }.toMap
      val structureCoverage =
        if !llmConfig.shouldEvaluateStructureKb || total == 0 then 0.0
        else structureProfileCount.get().toDouble / total.toDouble
      val structureUnknownRate =
        if structureProfileCount.get() == 0 then 0.0
        else structureUnknownCount.get().toDouble / structureProfileCount.get().toDouble
      val structureLowConfRate =
        if structureProfileCount.get() == 0 then 0.0
        else structureLowConfidenceCount.get().toDouble / structureProfileCount.get().toDouble
      val endgameCoverage =
        if !llmConfig.shouldEvaluateEndgameOracle || total == 0 then 0.0
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
      val avgCostUsd =
        if polishAttempts == 0 then 0.0
        else polishEstimatedCostMicros.get().toDouble / polishAttempts.toDouble / 1000000.0
      val polishReasonDist = polishReasonCount.toList.map { case (k, v) => k -> v.get() }.toMap
      val polishFallbackRateWindow = polishFallbackRate
      val polishLatencyP95Window = polishLatencyP95Ms
      val planRecallAt3 =
        if planRecallTotal.get() == 0 then 0.0
        else planRecallHit.get().toDouble / planRecallTotal.get().toDouble
      val latentPrecision =
        if latentPrecisionTotal.get() == 0 then 0.0
        else latentPrecisionHit.get().toDouble / latentPrecisionTotal.get().toDouble
      val pvCouplingRatio =
        if pvCouplingTotal.get() == 0 then 0.0
        else pvCouplingHit.get().toDouble / pvCouplingTotal.get().toDouble
      val evidenceReasonCoverage =
        if evidenceReasonTotal.get() == 0 then 1.0
        else evidenceReasonHit.get().toDouble / evidenceReasonTotal.get().toDouble
      val hypothesisProbeHitRate =
        if hypothesisProbeTotal.get() == 0 then 1.0
        else hypothesisProbeHit.get().toDouble / hypothesisProbeTotal.get().toDouble
      val contractDropRate =
        if contractProbeTotal.get() == 0 then 0.0
        else contractProbeDropped.get().toDouble / contractProbeTotal.get().toDouble
      val legacyFenMissingRate =
        if probeFenTotal.get() == 0 then 0.0
        else probeFenMissing.get().toDouble / probeFenTotal.get().toDouble
      val (totalWindowSize, totalP50, totalP95, structWindowSize, structP50, structP95) = latencySnapshot
      val epochSec = Instant.now().getEpochSecond
      logger.info(
        s"bookmaker.metrics epoch=$epochSec total=$total " +
          s"struct_mode=${structureMode} " +
          s"endgame_mode=${endgameMode} " +
          s"strategic_prior_mode=${strategicPriorMode} " +
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
          f"polish_fallback_window_rate=$polishFallbackRateWindow%.3f " +
          f"polish_latency_p95_ms=$polishLatencyP95Window%.3f " +
          f"plan_recall_at3=$planRecallAt3%.3f " +
          f"latent_precision=$latentPrecision%.3f " +
          f"pv_coupling_ratio=$pvCouplingRatio%.3f " +
          f"evidence_reason_coverage=$evidenceReasonCoverage%.3f " +
          f"hypothesis_probe_hit_rate=$hypothesisProbeHitRate%.3f " +
          f"contract_drop_rate=$contractDropRate%.3f " +
          f"legacy_fen_missing_rate=$legacyFenMissingRate%.3f " +
          f"polish_avg_cost_usd=$avgCostUsd%.6f " +
          s"polish_reason_dist=$polishReasonDist " +
          s"struct_dist=$structureDist " +
          s"alignment_band_dist=$bandDist"
      )

  def isGeminiEnabled: Boolean = geminiClient.isEnabled
  def isOpenAiEnabled: Boolean = openAiClient.isEnabled

  private def isLlmPolishEnabledForRequest(allowLlmPolish: Boolean): Boolean =
    if providerConfig.premiumOnly then allowLlmPolish else true

  private def normalizeLang(lang: String): String =
    Option(lang).map(_.trim.toLowerCase).filter(_.nonEmpty).map(_.take(8)).getOrElse(providerConfig.defaultLang)

  private def templateQualityScore(prose: String): Double =
    val t = Option(prose).getOrElse("")
    if t.isBlank then 0.0
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
        "practically, this should influence"
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

  private def wordCount(text: String): Int =
    Option(text).getOrElse("").split("\\s+").count(_.nonEmpty)

  private def lengthRatioReasonable(polished: String, original: String): Boolean =
    val originalWords = wordCount(original)
    val polishedWords = wordCount(polished)
    if originalWords <= 0 then polishedWords > 0
    else if originalWords < 35 then polishedWords <= math.max(70, (originalWords.toDouble * 2.2).toInt)
    else
      val lowerBound = (originalWords.toDouble * 0.45).toInt.max(18)
      val upperBound = (originalWords.toDouble * 1.35).toInt.max(originalWords + 8)
      polishedWords >= lowerBound && polishedWords <= upperBound

  private def looksJsonWrapper(text: String): Boolean =
    val t = Option(text).map(_.trim).getOrElse("")
    t.startsWith("{") && t.contains("\"commentary\"")

  private def looksTruncated(text: String): Boolean =
    val t = Option(text).map(_.trim).getOrElse("")
    if t.isEmpty then false
    else
      def balanced(open: Char, close: Char): Boolean =
        t.count(_ == open) == t.count(_ == close)
      val quoteCount = t.count(_ == '"')
      !balanced('{', '}') || !balanced('[', ']') || (quoteCount % 2 != 0) || t.endsWith("\\") ||
        MoveAnchorCodec.hasBrokenAnchorPrefix(t)

  private def shouldSkipPolish(prose: String): Boolean =
    templateQualityScore(prose) >= providerConfig.polishGateThreshold

  private def validatePolishedCommentary(
      polished: String,
      original: String,
      allowedSans: List[String]
  ): PolishValidation.ValidationResult =
    val trimmed = Option(polished).getOrElse("").trim
    if trimmed.isEmpty then PolishValidation.ValidationResult(isValid = false, reasons = List("empty_polish"))
    else if looksJsonWrapper(trimmed) then
      PolishValidation.ValidationResult(isValid = false, reasons = List("json_wrapper_unparsed"))
    else if looksTruncated(trimmed) then
      PolishValidation.ValidationResult(isValid = false, reasons = List("truncated_output"))
    else if leakTokens.exists(trimmed.contains) then
      PolishValidation.ValidationResult(isValid = false, reasons = List("leak_token_detected"))
    else if !lengthRatioReasonable(trimmed, original) then
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
      s"llm.polish.invalid provider=$provider phase=$phase attempt=$attempt model=${model.getOrElse("-")} reasons=$reasonStr"
    )

  private def llmCacheContext(
      allowLlmPolish: Boolean,
      lang: String,
      asyncTier: Boolean
  ): Option[LlmCacheContext] =
    if !isLlmPolishEnabledForRequest(allowLlmPolish) then None
    else
      val normalizedLang = normalizeLang(lang)
      providerConfig.provider match
        case "openai" if openAiClient.isEnabled =>
          val model = if asyncTier then openAiClient.asyncModelName else openAiClient.syncModelName
          Some(LlmCacheContext(model = model, promptVersion = providerConfig.promptVersion, lang = normalizedLang))
        case "gemini" if geminiClient.isEnabled =>
          Some(LlmCacheContext(model = geminiClient.modelName, promptVersion = providerConfig.promptVersion, lang = normalizedLang))
        case _ => None

  private def sumIntOptions(values: List[Option[Int]]): Option[Int] =
    if values.exists(_.isDefined) then Some(values.map(_.getOrElse(0)).sum)
    else None

  private def sumDoubleOptions(values: List[Option[Double]]): Option[Double] =
    if values.exists(_.isDefined) then Some(values.map(_.getOrElse(0.0)).sum)
    else None

  private def adaptiveOutputTokenCap(
      prose: String,
      refs: Option[BookmakerRefsV1],
      asyncTier: Boolean
  ): Int =
    // Structured polish prompts can become long once anchors (MV/MK/EV/VB) are embedded.
    // Keep a higher floor/ceiling to reduce wrapper truncation and empty-polish fallbacks.
    val minCap = if asyncTier then 420 else 320
    val maxCap = if asyncTier then 720 else 560
    val proseWords = wordCount(prose)
    val anchorCount = refs.toList.flatMap(_.variations).flatMap(_.moves).size
    val estimated = (proseWords.toDouble * 2.4 + anchorCount.toDouble * 5.0 + 96.0).toInt
    estimated.max(minCap).min(maxCap)

  private case class CandidateValidation(
      isValid: Boolean,
      decodedText: String,
      reasons: List[String]
  )

  private def validateCandidate(
      candidateText: String,
      originalProse: String,
      allowedSans: List[String],
      anchors: MoveAnchorCodec.EncodedCommentary,
      extraReasons: List[String]
  ): CandidateValidation =
    val anchorReasons =
      if anchors.hasAnchors then
        MoveAnchorCodec.validateAnchors(
          text = candidateText,
          expectedMoveOrder = anchors.expectedMoveOrder,
          expectedMarkerOrder = anchors.expectedMarkerOrder,
          expectedEvalOrder = anchors.expectedEvalOrder,
          expectedBranchOrder = anchors.expectedBranchOrder
        )
      else Nil
    val decoded =
      if anchors.hasAnchors then
        MoveAnchorCodec.decode(
          text = candidateText,
          refById = anchors.refById,
          evalById = anchors.evalById,
          branchById = anchors.branchById
        )
      else candidateText
    val proseValidation = validatePolishedCommentary(decoded, originalProse, allowedSans)
    val reasons = (anchorReasons ++ proseValidation.reasons ++ extraReasons).distinct
    CandidateValidation(
      isValid = reasons.isEmpty,
      decodedText = decoded,
      reasons = reasons
    )

  private def buildPolishMetaOpenAi(
      model: Option[String],
      sourceMode: String,
      phase: String,
      validationReasons: List[String],
      attempts: List[OpenAiPolishResult]
  ): PolishMetaV1 =
    PolishMetaV1(
      provider = "openai",
      model = model.orElse(attempts.lastOption.map(_.model)),
      sourceMode = sourceMode,
      validationPhase = phase,
      validationReasons = validationReasons,
      cacheHit = attempts.exists(_.promptCacheHit),
      promptTokens = sumIntOptions(attempts.map(_.promptTokens)),
      cachedTokens = sumIntOptions(attempts.map(_.cachedTokens)),
      completionTokens = sumIntOptions(attempts.map(_.completionTokens)),
      estimatedCostUsd = sumDoubleOptions(attempts.map(_.estimatedCostUsd))
    )

  private def buildPolishMetaGemini(
      model: Option[String],
      sourceMode: String,
      phase: String,
      validationReasons: List[String]
  ): PolishMetaV1 =
    PolishMetaV1(
      provider = "gemini",
      model = model,
      sourceMode = sourceMode,
      validationPhase = phase,
      validationReasons = validationReasons,
      cacheHit = false,
      promptTokens = None,
      cachedTokens = None,
      completionTokens = None,
      estimatedCostUsd = None
    )

  private def withRecordedPolishMetrics(startNs: Long, decision: PolishDecision): PolishDecision =
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
      lang: String,
      allowedSans: List[String],
      asyncTier: Boolean
  ): Future[Option[PolishDecision]] =
    val segmentation = PolishSegmenter.segment(prose)
    val editable = segmentation.editableSegments
    if editable.isEmpty then Future.successful(None)
    else
      val segmentCap = adaptiveOutputTokenCap(prose, refs = None, asyncTier = asyncTier).min(if asyncTier then 360 else 280).max(128)
      val maxSegments = if asyncTier then 6 else 4
      val candidateEditable =
        editable
          .sortBy(s => -wordCount(s.text))
          .take(maxSegments)
      final case class SegmentRewrite(id: String, commentary: String, attempt: OpenAiPolishResult)

      def validateSegmentCandidate(
          candidate: OpenAiPolishResult,
          originalSegment: String,
          masked: PolishSegmenter.MaskedSegment
      ): Option[String] =
        val lockReasons =
          if masked.hasLocks then PolishSegmenter.validateLocks(candidate.commentary, masked.expectedOrder)
          else Nil
        val unmasked =
          if masked.hasLocks then PolishSegmenter.unmask(candidate.commentary, masked.tokenById)
          else candidate.commentary
        val proseValidation = validatePolishedCommentary(unmasked, originalSegment, Nil)
        val reasons = (lockReasons ++ proseValidation.reasons ++ candidate.parseWarnings).distinct
        if reasons.isEmpty then Some(unmasked) else None

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
              lang = lang,
              maxOutputTokens = Some(segmentCap)
            )
          else
            openAiClient.polishSync(
              prose = input,
              phase = phase,
              evalDelta = evalDelta,
              concepts = concepts,
              fen = fen,
              openingName = openingName,
              lang = lang,
              maxOutputTokens = Some(segmentCap)
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
              maxOutputTokens = Some(segmentCap)
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
              maxOutputTokens = Some(segmentCap)
            )

        def acceptedRewrite(
            attempt: OpenAiPolishResult
        ): Option[SegmentRewrite] =
          validateSegmentCandidate(attempt, seg.text, masked)
            .map(rewritten => SegmentRewrite(seg.id, rewritten, attempt))

        def fallbackToOriginal(attempt: OpenAiPolishResult): Option[SegmentRewrite] =
          Some(SegmentRewrite(seg.id, seg.text, attempt))

        callPolish(modelInput).flatMap {
          case Some(primary) =>
            acceptedRewrite(primary) match
              case Some(rewritten) =>
                Future.successful(Some(rewritten))
              case None =>
                callRepair(modelInput, primary.commentary).map {
                  case Some(repaired) =>
                    acceptedRewrite(repaired)
                      .orElse(fallbackToOriginal(repaired))
                  case None =>
                    // Prefer structural safety over aggressive rewrite: keep original segment.
                    fallbackToOriginal(primary)
                }
          case None => Future.successful(None)
        }

      val rewritesFut =
        candidateEditable.foldLeft(Future.successful(Vector.empty[Option[SegmentRewrite]])) { (accFut, seg) =>
          accFut.flatMap(acc => polishSegment(seg).map(r => acc :+ r))
        }

      rewritesFut.map { results =>
        val successes = results.flatten
        if successes.isEmpty then None
        else
          val rewrites = successes.map(s => s.id -> s.commentary).toMap
          val merged = segmentation.merge(rewrites)
          val mergedValidation = validatePolishedCommentary(merged, prose, allowedSans)
          if !mergedValidation.isValid then None
          else
            val attempts = successes.map(_.attempt).toList
            Some(
              PolishDecision(
                commentary = merged,
                sourceMode = "llm_polished",
                model = attempts.lastOption.map(_.model),
                cacheHit = attempts.exists(_.promptCacheHit),
                meta = Some(
                  buildPolishMetaOpenAi(
                    model = attempts.lastOption.map(_.model),
                    sourceMode = "llm_polished",
                    phase = phase,
                    validationReasons = Nil,
                    attempts = attempts
                  )
                )
              )
            )
      }

  private def maybePolishCommentary(
      prose: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String],
      allowLlmPolish: Boolean,
      lang: String,
      allowedSans: List[String],
      asyncTier: Boolean,
      refs: Option[BookmakerRefsV1]
  ): Future[PolishDecision] =
    if prose.isBlank then
      Future.successful(
        PolishDecision(
          commentary = prose,
          sourceMode = "rule",
          model = None,
          cacheHit = false,
          meta = None
        )
      )
    else if !isLlmPolishEnabledForRequest(allowLlmPolish) || providerConfig.isNone then
      Future.successful(
        PolishDecision(
          commentary = prose,
          sourceMode = "rule",
          model = None,
          cacheHit = false,
          meta = None
        )
      )
    else if shouldSkipPolish(prose) then
      Future.successful(
        PolishDecision(
          commentary = prose,
          sourceMode = "rule",
          model = None,
          cacheHit = false,
          meta = None
        )
      )
    else if isPolishCircuitOpen(System.currentTimeMillis()) then
      Future.successful(
        PolishDecision(
          commentary = prose,
          sourceMode = "rule_circuit_open",
          model = None,
          cacheHit = false,
          meta = Some(
            PolishMetaV1(
              provider = providerConfig.provider,
              model = None,
              sourceMode = "rule_circuit_open",
              validationPhase = phase,
              validationReasons = List("circuit_open"),
              cacheHit = false,
              promptTokens = None,
              cachedTokens = None,
              completionTokens = None,
              estimatedCostUsd = None
            )
          )
        )
      )
    else
      providerConfig.provider match
        case "openai" if openAiClient.isEnabled =>
          val normalizedLang = normalizeLang(lang)
          val requestStartNs = System.nanoTime()
          val shouldTrySegmentPolish = refs.exists(_.variations.nonEmpty) || allowedSans.nonEmpty
          val segmentAttemptFut =
            if shouldTrySegmentPolish then
              maybePolishBySegmentsOpenAi(
                prose = prose,
                phase = phase,
                evalDelta = evalDelta,
                concepts = concepts,
                fen = fen,
                openingName = openingName,
                lang = normalizedLang,
                allowedSans = allowedSans,
                asyncTier = asyncTier
              )
            else Future.successful(None)

          segmentAttemptFut.flatMap {
            case Some(segmentDecision) =>
              Future.successful(withRecordedPolishMetrics(requestStartNs, segmentDecision))
            case None =>
              val anchors = MoveAnchorCodec.encode(prose, refs)
              val proseForModel = if anchors.hasAnchors then anchors.anchoredText else prose
              val adaptiveCap = adaptiveOutputTokenCap(prose, refs, asyncTier)
              val op =
                if asyncTier then
                  openAiClient.polishAsync(
                    prose = proseForModel,
                    phase = phase,
                    evalDelta = evalDelta,
                    concepts = concepts,
                    fen = fen,
                    openingName = openingName,
                    lang = normalizedLang,
                    maxOutputTokens = Some(adaptiveCap)
                  )
                else
                  openAiClient.polishSync(
                    prose = proseForModel,
                    phase = phase,
                    evalDelta = evalDelta,
                    concepts = concepts,
                    fen = fen,
                    openingName = openingName,
                    lang = normalizedLang,
                    maxOutputTokens = Some(adaptiveCap)
                  )
              op.flatMap {
                case Some(polished) =>
                  val firstValidation = validateCandidate(
                    candidateText = polished.commentary,
                    originalProse = prose,
                    allowedSans = allowedSans,
                    anchors = anchors,
                    extraReasons = polished.parseWarnings
                  )
                  if firstValidation.isValid then
                    Future.successful(
                      withRecordedPolishMetrics(
                        requestStartNs,
                        PolishDecision(
                          commentary = firstValidation.decodedText,
                          sourceMode = "llm_polished",
                          model = Some(polished.model),
                          cacheHit = polished.promptCacheHit,
                          meta = Some(
                            buildPolishMetaOpenAi(
                              model = Some(polished.model),
                              sourceMode = "llm_polished",
                              phase = phase,
                              validationReasons = Nil,
                              attempts = List(polished)
                            )
                          )
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
                    val repairFut =
                      if asyncTier then
                        openAiClient.repairAsync(
                          originalProse = proseForModel,
                          rejectedPolish = polished.commentary,
                          phase = phase,
                          evalDelta = evalDelta,
                          concepts = concepts,
                          fen = fen,
                          openingName = openingName,
                          allowedSans = allowedSans,
                          lang = normalizedLang,
                          maxOutputTokens = Some(adaptiveCap)
                        )
                      else
                        openAiClient.repairSync(
                          originalProse = proseForModel,
                          rejectedPolish = polished.commentary,
                          phase = phase,
                          evalDelta = evalDelta,
                          concepts = concepts,
                          fen = fen,
                          openingName = openingName,
                          allowedSans = allowedSans,
                          lang = normalizedLang,
                          maxOutputTokens = Some(adaptiveCap)
                        )
                    repairFut.map {
                      case Some(repaired) =>
                        val repairedValidation = validateCandidate(
                          candidateText = repaired.commentary,
                          originalProse = prose,
                          allowedSans = allowedSans,
                          anchors = anchors,
                          extraReasons = repaired.parseWarnings
                        )
                        if repairedValidation.isValid then
                          withRecordedPolishMetrics(
                            requestStartNs,
                            PolishDecision(
                              commentary = repairedValidation.decodedText,
                              sourceMode = "llm_polished",
                              model = Some(repaired.model),
                              cacheHit = repaired.promptCacheHit,
                              meta = Some(
                                buildPolishMetaOpenAi(
                                  model = Some(repaired.model),
                                  sourceMode = "llm_polished",
                                  phase = phase,
                                  validationReasons = Nil,
                                  attempts = List(polished, repaired)
                                )
                              )
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
                          withRecordedPolishMetrics(
                            requestStartNs,
                            PolishDecision(
                              commentary = prose,
                              sourceMode = "fallback_rule_invalid",
                              model = None,
                              cacheHit = false,
                              meta = Some(
                                buildPolishMetaOpenAi(
                                  model = Some(repaired.model),
                                  sourceMode = "fallback_rule_invalid",
                                  phase = phase,
                                  validationReasons = reasons,
                                  attempts = List(polished, repaired)
                                )
                              )
                            )
                          )
                      case None =>
                        withRecordedPolishMetrics(
                          requestStartNs,
                          PolishDecision(
                            commentary = prose,
                            sourceMode = "fallback_rule_invalid",
                            model = None,
                            cacheHit = false,
                            meta = Some(
                              buildPolishMetaOpenAi(
                                model = Some(polished.model),
                                sourceMode = "fallback_rule_invalid",
                                phase = phase,
                                validationReasons = firstValidation.reasons.distinct,
                                attempts = List(polished)
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
                        commentary = prose,
                        sourceMode = "fallback_rule_empty",
                        model = None,
                        cacheHit = false,
                        meta = Some(
                          buildPolishMetaOpenAi(
                            model = None,
                            sourceMode = "fallback_rule_empty",
                            phase = phase,
                            validationReasons = List("empty_polish"),
                            attempts = Nil
                          )
                        )
                      )
                    )
                  )
              }
          }
        case "gemini" if geminiClient.isEnabled =>
          val requestStartNs = System.nanoTime()
          val anchors = MoveAnchorCodec.encode(prose, refs)
          val proseForModel = if anchors.hasAnchors then anchors.anchoredText else prose
          geminiClient
            .polish(
              prose = proseForModel,
              phase = phase,
              evalDelta = evalDelta,
              concepts = concepts,
              fen = fen,
              openingName = openingName
            )
            .flatMap {
              case Some(polished) =>
                val validation = validateCandidate(
                  candidateText = polished,
                  originalProse = prose,
                  allowedSans = allowedSans,
                  anchors = anchors,
                  extraReasons = Nil
                )
                if validation.isValid then
                  Future.successful(
                    withRecordedPolishMetrics(
                      requestStartNs,
                      PolishDecision(
                        commentary = validation.decodedText,
                        sourceMode = "llm_polished",
                        model = Some(geminiClient.modelName),
                        cacheHit = false,
                        meta = Some(
                          buildPolishMetaGemini(
                            model = Some(geminiClient.modelName),
                            sourceMode = "llm_polished",
                            phase = phase,
                            validationReasons = Nil
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
                  geminiClient
                    .repair(
                      originalProse = proseForModel,
                      rejectedPolish = polished,
                      phase = phase,
                      evalDelta = evalDelta,
                      concepts = concepts,
                      fen = fen,
                      openingName = openingName,
                      allowedSans = allowedSans
                    )
                    .map {
                      case Some(repaired) =>
                        val repairedValidation = validateCandidate(
                          candidateText = repaired,
                          originalProse = prose,
                          allowedSans = allowedSans,
                          anchors = anchors,
                          extraReasons = Nil
                        )
                        if repairedValidation.isValid then
                          withRecordedPolishMetrics(
                            requestStartNs,
                            PolishDecision(
                              commentary = repairedValidation.decodedText,
                              sourceMode = "llm_polished",
                              model = Some(geminiClient.modelName),
                              cacheHit = false,
                              meta = Some(
                                buildPolishMetaGemini(
                                  model = Some(geminiClient.modelName),
                                  sourceMode = "llm_polished",
                                  phase = phase,
                                  validationReasons = Nil
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
                          withRecordedPolishMetrics(
                            requestStartNs,
                            PolishDecision(
                              commentary = prose,
                              sourceMode = "fallback_rule_invalid",
                              model = None,
                              cacheHit = false,
                              meta = Some(
                                buildPolishMetaGemini(
                                  model = Some(geminiClient.modelName),
                                  sourceMode = "fallback_rule_invalid",
                                  phase = phase,
                                  validationReasons = reasons
                                )
                              )
                            )
                          )
                      case None =>
                        withRecordedPolishMetrics(
                          requestStartNs,
                          PolishDecision(
                            commentary = prose,
                            sourceMode = "fallback_rule_invalid",
                            model = None,
                            cacheHit = false,
                            meta = Some(
                              buildPolishMetaGemini(
                                model = Some(geminiClient.modelName),
                                sourceMode = "fallback_rule_invalid",
                                phase = phase,
                                validationReasons = primaryReasons
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
                      commentary = prose,
                      sourceMode = "fallback_rule_empty",
                      model = None,
                      cacheHit = false,
                      meta = Some(
                        buildPolishMetaGemini(
                          model = None,
                          sourceMode = "fallback_rule_empty",
                          phase = phase,
                          validationReasons = List("empty_polish")
                        )
                      )
                    )
                  )
                )
            }
        case _ =>
          Future.successful(
            PolishDecision(
              commentary = prose,
              sourceMode = "rule",
              model = None,
              cacheHit = false,
              meta = None
            )
          )

  private def phaseFromPly(ply: Int): String =
    if ply <= 16 then "opening"
    else if ply <= 60 then "middlegame"
    else "endgame"

  private def maybePolishGameNarrative(
      response: GameNarrativeResponse,
      allowLlmPolish: Boolean,
      lang: String,
      asyncTier: Boolean
  ): Future[GameNarrativeResponse] =
    if !isLlmPolishEnabledForRequest(allowLlmPolish) || providerConfig.isNone then
      Future.successful(response.copy(sourceMode = "rule", model = None))
    else
      for
        introPolish <- maybePolishCommentary(
          prose = response.intro,
          phase = "opening",
          evalDelta = None,
          concepts = response.themes,
          fen = "",
          openingName = None,
          allowLlmPolish = allowLlmPolish,
          lang = lang,
          allowedSans = Nil,
          asyncTier = asyncTier,
          refs = None
        )
        conclusionPolish <- maybePolishCommentary(
          prose = response.conclusion,
          phase = "endgame",
          evalDelta = None,
          concepts = response.themes,
          fen = "",
          openingName = None,
          allowLlmPolish = allowLlmPolish,
          lang = lang,
          allowedSans = Nil,
          asyncTier = asyncTier,
          refs = None
        )
        polishedMoments <- Future.traverse(response.moments) { moment =>
          val allowedSans = moment.variations.flatMap(v => NarrativeUtils.uciListToSan(moment.fen, v.moves))
          maybePolishCommentary(
            prose = moment.narrative,
            phase = phaseFromPly(moment.ply),
            evalDelta = None,
            concepts = moment.concepts,
            fen = moment.fen,
            openingName = None,
            allowLlmPolish = allowLlmPolish,
            lang = lang,
            allowedSans = allowedSans,
            asyncTier = asyncTier,
            refs = None
          ).map { decision =>
            (moment.copy(narrative = decision.commentary), decision.sourceMode, decision.model, decision.cacheHit)
          }
        }
      yield
        val sourceModes =
          (List(introPolish.sourceMode, conclusionPolish.sourceMode) ++ polishedMoments.map(_._2)).distinct
        val model =
          (List(introPolish.model, conclusionPolish.model) ++ polishedMoments.map(_._3)).flatten.headOption
        val sourceMode =
          if sourceModes.contains("llm_polished") then "llm_polished"
          else if sourceModes.exists(_.startsWith("fallback_rule")) then "fallback_rule"
          else if sourceModes.contains("rule_circuit_open") then "rule_circuit_open"
          else "rule"
        response.copy(
          intro = introPolish.commentary,
          conclusion = conclusionPolish.commentary,
          moments = polishedMoments.map(_._1),
          sourceMode = sourceMode,
          model = model
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

  private def buildBookmakerRefs(
      fenBefore: String,
      variations: List[VariationLine]
  ): Option[BookmakerRefsV1] =
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
          MoveRefV1(
            refId = f"l${lineIdx + 1}%02d_m${i + 1}%02d",
            san = sanList(i),
            uci = uciList(i),
            fenAfter = fensAfter(i),
            ply = ply,
            moveNo = moveNo,
            marker = Some(markerForPly(ply))
          )
        }
        VariationRefV1(
          lineId = f"line_${lineIdx + 1}%02d",
          scoreCp = line.scoreCp,
          mate = line.mate,
          depth = line.depth,
          moves = moves
        )
      }
      Some(
        BookmakerRefsV1(
          startFen = fenBefore,
          startPly = startPly,
          variations = lines
        )
      )


  def fetchOpeningMasters(fen: String): Future[Option[OpeningReference]] =
    openingExplorer.fetchMasters(fen)


  def fetchOpeningMasterPgn(gameId: String): Future[Option[String]] =
    openingExplorer.fetchMasterPgn(gameId)

  /** Generate an instant rule-based briefing for a position. */
  def briefCommentPosition(
      fen: String,
      lastMove: Option[String],
      eval: Option[EvalData],
      ply: Int
  ): Future[Option[CommentResponse]] = Future {
    val _ = ply
    val pv = eval.flatMap(_.pv).getOrElse(Nil)
    CommentaryEngine.assess(fen, pv).map { assessment =>
      val bead = Math.abs(fen.hashCode)
      val intro = NarrativeLexicon.intro(bead, assessment.nature.natureType.toString, assessment.nature.tension, Style.Book)

      val bestPlan = assessment.plans.topPlans.headOption.map(_.plan.name).getOrElse("strategic improvement")
      val intent = lastMove.map(m => NarrativeLexicon.intent(bead, m, bestPlan, Style.Book)).getOrElse("")

      val briefing = s"$intro\n\n$intent"

      CommentResponse(
        commentary = briefing,
        concepts = assessment.plans.topPlans.map(_.plan.name),
        variations = Nil
      )
    }
  }

  /** Generate deep bookmaker commentary (rule-based). */
  def bookmakerCommentPosition(
      fen: String,
      lastMove: Option[String],
      eval: Option[EvalData],
      variations: Option[List[VariationLine]],
      probeResults: Option[List[lila.llm.model.ProbeResult]] = None,
      openingData: Option[OpeningReference] = None,
      afterFen: Option[String] = None,
      afterEval: Option[EvalData] = None,
      afterVariations: Option[List[VariationLine]] = None,
      opening: Option[String],
      phase: String,
      ply: Int,
      prevStateToken: Option[lila.llm.analysis.PlanStateTracker] = None,
      allowLlmPolish: Boolean = false,
      lang: String = "en"
  ): Future[Option[BookmakerResult]] =
    val requestStartNs = System.nanoTime()
    val incomingProbes = probeResults.getOrElse(Nil)
    val cacheCtx = llmCacheContext(allowLlmPolish = allowLlmPolish, lang = lang, asyncTier = false)
    bookmakerRequests.incrementAndGet()
    if prevStateToken.isDefined then tokenPresentCount.incrementAndGet()
    commentaryCache.get(fen, lastMove, incomingProbes, prevStateToken, cacheCtx) match
      case Some(cached) =>
        stateAwareCacheHitCount.incrementAndGet()
        logger.debug(s"Cache hit: ${fen.take(20)}...")
        recordLatencyMetrics(totalLatencyMs = elapsedMs(requestStartNs), structureEvalLatencyMs = None)
        maybeLogBookmakerMetrics()
        Future.successful(Some(BookmakerResult(response = cached, cacheHit = true)))
      case None =>
        stateAwareCacheMissCount.incrementAndGet()
        computeBookmakerResponse(
          fen, lastMove, eval, variations, probeResults, openingData,
          afterFen, afterEval, afterVariations, opening, phase, ply, prevStateToken, allowLlmPolish, lang, cacheCtx
        ).map:
          case (responseOpt, structEvalMsOpt) =>
          recordLatencyMetrics(totalLatencyMs = elapsedMs(requestStartNs), structureEvalLatencyMs = structEvalMsOpt)
          maybeLogBookmakerMetrics()
          responseOpt
        


  private def computeBookmakerResponse(
      fen: String,
      lastMove: Option[String],
      eval: Option[EvalData],
      variations: Option[List[VariationLine]],
      probeResults: Option[List[lila.llm.model.ProbeResult]],
      openingData: Option[OpeningReference],
      afterFen: Option[String],
      afterEval: Option[EvalData],
      afterVariations: Option[List[VariationLine]],
      opening: Option[String],
      phase: String,
      ply: Int,
      prevStateToken: Option[lila.llm.analysis.PlanStateTracker],
      allowLlmPolish: Boolean,
      lang: String,
      cacheCtx: Option[LlmCacheContext]
  ): Future[(Option[BookmakerResult], Option[Long])] =
    val effectivePly = NarrativeUtils.resolveAnnotationPly(fen, lastMove, ply)

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
      val shouldFetchMasters =
        phase.trim.equalsIgnoreCase("opening") &&
          effectivePly >= OpeningRefMinPly &&
          effectivePly <= OpeningRefMaxPly

      val mastersFut =
        if openingData.isDefined then Future.successful(Some(openingExplorer.enrichWithLocalPgn(fen, openingData.get)))
        else if shouldFetchMasters then openingExplorer.fetchMasters(fen)
        else Future.successful(None)

      mastersFut.flatMap { openingRef =>
        val isWhiteTurn = fen.contains(" w ")
        val movingColor = if (isWhiteTurn) _root_.chess.Color.White else _root_.chess.Color.Black
        val tracker = prevStateToken.getOrElse(lila.llm.analysis.PlanStateTracker.empty)

        val dataOpt = CommentaryEngine.assessExtended(
          fen = fen,
          variations = vars,
          playedMove = lastMove,
          opening = opening,
          phase = Some(phase),
          ply = effectivePly,
          prevMove = lastMove,
          prevPlanContinuity = tracker.getContinuity(movingColor),
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
            val dataWithContinuity = data.copy(planContinuity = nextTracker.getContinuity(movingColor))
            if dataWithContinuity.planContinuity.exists(_.consecutivePlies >= 2) then
              continuityAppliedCount.incrementAndGet()
            dataWithContinuity.planSequence.foreach(ps => incTransition(ps.transitionType.toString))
            if llmConfig.shouldEvaluateStructureKb then recordStructureMetrics(dataWithContinuity)
            if llmConfig.shouldEvaluateEndgameOracle then recordEndgameMetrics(dataWithContinuity)

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

            val ctx = NarrativeContextBuilder.build(
              data = dataWithContinuity,
              ctx = dataWithContinuity.toContext,
              probeResults = probeResults.getOrElse(Nil),
              openingRef = openingRef,
              afterAnalysis = afterDataOpt
            )
            recordStrategicMetrics(
              data = dataWithContinuity,
              ctx = ctx,
              probeResults = probeResults.getOrElse(Nil)
            )
            val proseRaw = BookStyleRenderer.render(ctx)
            val prose = RuleTemplateSanitizer.sanitize(
              proseRaw,
              opening = opening,
              phase = phase,
              ply = effectivePly,
              fen = Some(fen)
            )
            val baseConcepts = ctx.semantic.map(_.conceptSummary).getOrElse(Nil)
            val allowedSans =
              dataWithContinuity.alternatives.flatMap(v => NarrativeUtils.uciListToSan(fen, v.moves))
            val refs = buildBookmakerRefs(fen, dataWithContinuity.alternatives)

            maybePolishCommentary(
              prose = prose,
              phase = phase,
              evalDelta = None,
              concepts = baseConcepts,
              fen = fen,
              openingName = opening,
              allowLlmPolish = allowLlmPolish,
              lang = lang,
              allowedSans = allowedSans,
              asyncTier = false,
              refs = refs
            ).map { decision =>
              val response = CommentResponse(
                commentary = decision.commentary,
                concepts = baseConcepts,
                variations = dataWithContinuity.alternatives,
                probeRequests = if probeResults.exists(_.nonEmpty) then Nil else ctx.probeRequests,
                mainStrategicPlans = ctx.mainStrategicPlans,
                latentPlans = ctx.latentPlans,
                whyAbsentFromTopMultiPV = ctx.whyAbsentFromTopMultiPV,
                planStateToken = Some(nextTracker),
                sourceMode = decision.sourceMode,
                model = decision.model,
                refs = refs,
                polishMeta = decision.meta
              )
              if response.planStateToken.isDefined then tokenEmitCount.incrementAndGet()
              commentaryCache.put(
                fen = fen,
                lastMove = lastMove,
                response = response,
                probeResults = probeResults.getOrElse(Nil),
                planStateToken = prevStateToken,
                llmContext = cacheCtx
              )
              Some(BookmakerResult(response = response, cacheHit = decision.cacheHit)) -> dataWithContinuity.structureEvalLatencyMs
            }
      }


  def analyzeFullGameLocal(
      pgn: String,
      evals: List[MoveEval],
      style: String = "book",
      focusOn: List[String] = List("mistakes", "turning_points"),
      allowLlmPolish: Boolean = false,
      asyncTier: Boolean = false,
      lang: String = "en"
  ): Future[Option[GameNarrativeResponse]] =
    val _ = (style, focusOn)
    val evalMap = evals.map(e => e.ply -> e.getVariations).toMap
    fetchOpeningRefsForPgn(pgn)
      .map { openingRefsByFen =>
        CommentaryEngine.generateFullGameNarrative(
          pgn = pgn,
          evals = evalMap,
          openingRefsByFen = openingRefsByFen
        )
      }
      .recover { case NonFatal(e) =>
        logger.warn(s"Opening reference fetch failed for full game analysis: ${e.getMessage}")
        CommentaryEngine.generateFullGameNarrative(
          pgn = pgn,
          evals = evalMap
        )
      }
      .flatMap { narrative =>
        val base = GameNarrativeResponse.fromNarrative(
          narrative = narrative,
          review = Some(buildGameReview(narrative, pgn, evals)),
          sourceMode = "rule",
          model = None
        )
        maybePolishGameNarrative(
          response = base,
          allowLlmPolish = allowLlmPolish,
          lang = normalizeLang(lang),
          asyncTier = asyncTier
        ).map(_.some)
      }

  def submitGameAnalysisAsync(
      req: FullAnalysisRequest,
      allowLlmPolish: Boolean,
      lang: String
  ): AsyncGameAnalysisSubmitResponse =
    cleanupAsyncJobs()
    val jobId = UUID.randomUUID().toString.replace("-", "")
    val now = System.currentTimeMillis()
    asyncJobs.put(
      jobId,
      AsyncGameAnalysisStatusResponse(
        jobId = jobId,
        status = "queued",
        createdAtMs = now,
        updatedAtMs = now
      )
    )
    updateAsyncJob(jobId): current =>
      current.copy(status = "running", updatedAtMs = System.currentTimeMillis())

    analyzeFullGameLocal(
      pgn = req.pgn,
      evals = req.evals,
      style = req.options.style,
      focusOn = req.options.focusOn,
      allowLlmPolish = allowLlmPolish,
      asyncTier = true,
      lang = lang
    ).map {
      case Some(result) =>
        updateAsyncJob(jobId): current =>
          current.copy(
            status = "completed",
            updatedAtMs = System.currentTimeMillis(),
            result = Some(result),
            error = None
          )
      case None =>
        updateAsyncJob(jobId): current =>
          current.copy(
            status = "failed",
            updatedAtMs = System.currentTimeMillis(),
            error = Some("Narrative Analysis unavailable")
          )
    }.recover { case NonFatal(e) =>
      updateAsyncJob(jobId): current =>
        current.copy(
          status = "failed",
          updatedAtMs = System.currentTimeMillis(),
          error = Some(e.getMessage.take(240))
        )
    }
    AsyncGameAnalysisSubmitResponse(jobId = jobId, status = "running")

  def getGameAnalysisAsyncStatus(jobId: String): Option[AsyncGameAnalysisStatusResponse] =
    cleanupAsyncJobs()
    asyncJobs.get(jobId)

  private def updateAsyncJob(
      jobId: String
  )(f: AsyncGameAnalysisStatusResponse => AsyncGameAnalysisStatusResponse): Unit =
    asyncJobs.get(jobId).foreach { current =>
      asyncJobs.update(jobId, f(current))
    }

  private def cleanupAsyncJobs(): Unit =
    val now = System.currentTimeMillis()
    asyncJobs.foreach { case (jobId, status) =>
      if now - status.updatedAtMs > AsyncJobTtlMs then asyncJobs.remove(jobId)
    }
    val overflow = asyncJobs.size - AsyncJobMaxSize
    if overflow > 0 then
      asyncJobs
        .toList
        .sortBy(_._2.updatedAtMs)
        .take(overflow)
        .foreach { case (jobId, _) => asyncJobs.remove(jobId) }

  private def fetchOpeningRefsForPgn(pgn: String): Future[Map[String, OpeningReference]] =
    val openingFens = PgnAnalysisHelper.extractPlyData(pgn) match
      case Left(err) =>
        logger.warn(s"Failed to parse PGN for opening references: $err")
        Nil
      case Right(plyData) =>
        plyData
          .collect {
            case pd if pd.ply >= OpeningRefMinPly && pd.ply <= OpeningRefMaxPly => pd.fen
          }
          .distinct

    if openingFens.isEmpty then Future.successful(Map.empty)
    else
      Future
        .traverse(openingFens) { fen =>
          openingExplorer
            .fetchMasters(fen)
            .map(refOpt => fen -> refOpt)
            .recover { case NonFatal(e) =>
              logger.warn(s"Opening explorer fetch failed for FEN `${fen.take(32)}...`: ${e.getMessage}")
              fen -> None
            }
        }
        .map(_.collect { case (fen, Some(ref)) => fen -> ref }.toMap)

  private def buildGameReview(
      narrative: FullGameNarrative,
      pgn: String,
      evals: List[MoveEval]
  ): GameNarrativeReview =
    val totalPliesFromPgn = PgnAnalysisHelper.extractPlyData(pgn).toOption.map(_.size).getOrElse(0)
    val evalPlies = evals.map(_.ply).filter(_ > 0).distinct
    val inferredTotalPlies =
      if totalPliesFromPgn > 0 then totalPliesFromPgn
      else evalPlies.maxOption.getOrElse(0)
    val evalCoveredPlies =
      if inferredTotalPlies > 0 then evalPlies.count(_ <= inferredTotalPlies)
      else evalPlies.size
    val evalCoveragePct =
      if inferredTotalPlies <= 0 then 0
      else Math.round((evalCoveredPlies.toDouble * 100.0) / inferredTotalPlies.toDouble).toInt
    val selectedMomentPlies = narrative.keyMomentNarratives.map(_.ply).filter(_ > 0).distinct.sorted

    GameNarrativeReview(
      totalPlies = inferredTotalPlies,
      evalCoveredPlies = evalCoveredPlies,
      evalCoveragePct = evalCoveragePct.max(0).min(100),
      selectedMoments = selectedMomentPlies.size,
      selectedMomentPlies = selectedMomentPlies
    )

private[llm] object RuleTemplateSanitizer:

  private case class OpeningFamily(
      id: String,
      aliases: List[String],
      markers: List[String]
  )

  // Opening-stage guard: phase labels can drift, so we still sanitize early middle games.
  private val OpeningStageMaxPly = 24

  private val openingFamilies = List(
    OpeningFamily(
      id = "open_games",
      aliases = List(
        "open game",
        "king's pawn",
        "kings pawn",
        "italian",
        "ruy lopez",
        "spanish",
        "scotch",
        "four knights",
        "petrov",
        "philidor",
        "vienna",
        "bishop's opening",
        "bishops opening",
        "ponziani"
      ),
      markers = List(
        "open games (1.e4 e5)",
        "open games",
        "central e4-e5 structure",
        "e4-e5 structure"
      )
    ),
    OpeningFamily(
      id = "sicilian",
      aliases = List(
        "sicilian",
        "najdorf",
        "dragon",
        "scheveningen",
        "sveshnikov",
        "taimanov",
        "kan",
        "rossolimo",
        "alapin",
        "smith-morra",
        "closed sicilian"
      ),
      markers = List("sicilian")
    ),
    OpeningFamily(
      id = "french",
      aliases = List(
        "french",
        "winawer",
        "tarrasch",
        "rubinstein",
        "maccutcheon",
        "steinitz"
      ),
      markers = List("french")
    ),
    OpeningFamily(
      id = "caro_kann",
      aliases = List("caro-kann", "caro kann"),
      markers = List("caro-kann", "caro kann")
    ),
    OpeningFamily(
      id = "scandinavian",
      aliases = List("scandinavian", "center counter"),
      markers = List("scandinavian")
    ),
    OpeningFamily(
      id = "nimzo_indian",
      aliases = List("nimzo", "nimzo-indian", "nimzo indian"),
      markers = List("nimzo")
    ),
    OpeningFamily(
      id = "kings_indian",
      aliases = List("king's indian", "kings indian", "k.i.d", "kid"),
      markers = List("king's indian", "kings indian")
    ),
    OpeningFamily(
      id = "benoni",
      aliases = List("benoni", "modern benoni"),
      markers = List("benoni")
    ),
    OpeningFamily(
      id = "catalan",
      aliases = List("catalan"),
      markers = List("catalan")
    ),
    OpeningFamily(
      id = "queens_gambit",
      aliases = List("queen's gambit", "queens gambit", "qgd", "qga"),
      markers = List("queen's gambit", "queens gambit")
    ),
    OpeningFamily(
      id = "london",
      aliases = List("london"),
      markers = List("london")
    ),
    OpeningFamily(
      id = "english",
      aliases = List("english"),
      markers = List("english")
    ),
    OpeningFamily(
      id = "austrian",
      aliases = List("austrian attack", "pirc", "modern defense"),
      markers = List("austrian attack", "pirc", "modern defense")
    )
  )

  private val familiesById = openingFamilies.map(f => f.id -> f).toMap
  private val sentenceBoundaryRegex = java.util.regex.Pattern.compile("""(?<=[.!?])\s+""")
  private val requiresStructureRegex =
    """(?i)\brequires?\s+an?\s+([^,.;:!?]{2,60}?)\s+structure\b""".r
  private val typicalInRegex =
    """(?i)\btypical in\s+([^,.;:!?]{2,60})""".r

  private def normalizeOpening(opening: Option[String]): String =
    opening.getOrElse("").trim.toLowerCase

  private def isOpeningStage(phase: String, ply: Int): Boolean =
    phase.equalsIgnoreCase("opening") ||
      (phase.equalsIgnoreCase("middlegame") && ply > 0 && ply <= OpeningStageMaxPly)

  private def familyIdFromPhrase(phrase: String): Option[String] =
    val normalized = phrase.trim.toLowerCase
    openingFamilies
      .find { family =>
        family.aliases.exists(alias => normalized.contains(alias) || alias.contains(normalized))
      }
      .map(_.id)

  private def detectMentionedFamilyIds(sentence: String): Set[String] =
    val lower = sentence.toLowerCase
    val direct = openingFamilies.collect {
      case family if family.markers.exists(lower.contains) => family.id
    }
    val fromPatterns =
      (requiresStructureRegex.findAllMatchIn(lower).map(_.group(1)).toList ++
        typicalInRegex.findAllMatchIn(lower).map(_.group(1)).toList)
        .flatMap(familyIdFromPhrase)
    (direct ++ fromPatterns).toSet

  private def openingMatchesFamily(openingLower: String, familyId: String): Boolean =
    familiesById
      .get(familyId)
      .exists(_.aliases.exists(alias => openingLower.contains(alias)))

  private def parseFenPieces(fen: String): Option[Map[String, Char]] =
    val boardPart = Option(fen).map(_.trim).filter(_.nonEmpty).flatMap(_.split(" ").headOption).getOrElse("")
    val ranks = boardPart.split("/")
    if ranks.length != 8 then None
    else
      val pieces = scala.collection.mutable.Map.empty[String, Char]
      var valid = true
      ranks.zipWithIndex.foreach { case (rankStr, rankIdx) =>
        if valid then
          var file = 0
          rankStr.foreach { ch =>
            if ch.isDigit then file += ch.asDigit
            else
              if file >= 0 && file < 8 then
                val sq = s"${('a' + file).toChar}${8 - rankIdx}"
                pieces.update(sq, ch)
              else valid = false
              file += 1
          }
          if file != 8 then valid = false
      }
      if valid then Some(pieces.toMap) else None

  private def hasPiece(board: Map[String, Char], square: String, piece: Char): Boolean =
    board.get(square).contains(piece)

  private def structureMatchesFamily(fen: Option[String], familyId: String): Boolean =
    val boardOpt = fen.flatMap(parseFenPieces)
    boardOpt.exists { board =>
      familyId match
        case "open_games" =>
          hasPiece(board, "e4", 'P') && hasPiece(board, "e5", 'p')
        case "sicilian" =>
          hasPiece(board, "c5", 'p')
        case "french" =>
          hasPiece(board, "e6", 'p') && hasPiece(board, "d5", 'p')
        case "caro_kann" =>
          hasPiece(board, "c6", 'p') && hasPiece(board, "d5", 'p')
        case "scandinavian" =>
          hasPiece(board, "e4", 'P') && hasPiece(board, "d5", 'p')
        case "catalan" =>
          hasPiece(board, "d4", 'P') && hasPiece(board, "c4", 'P') && hasPiece(board, "g2", 'B')
        case "queens_gambit" =>
          hasPiece(board, "d4", 'P') && hasPiece(board, "c4", 'P')
        case "london" =>
          hasPiece(board, "d4", 'P') && hasPiece(board, "e3", 'P') && hasPiece(board, "f4", 'B')
        case "english" =>
          hasPiece(board, "c4", 'P')
        case "kings_indian" =>
          hasPiece(board, "f6", 'n') && hasPiece(board, "g7", 'b') && hasPiece(board, "g6", 'p')
        case "nimzo_indian" =>
          hasPiece(board, "f6", 'n') && hasPiece(board, "b4", 'b')
        case "benoni" =>
          hasPiece(board, "c5", 'p') && hasPiece(board, "d6", 'p') && hasPiece(board, "d5", 'P')
        case "austrian" =>
          hasPiece(board, "e4", 'P') && hasPiece(board, "f4", 'P')
        case _ => true
    }

  private def shouldNeutralizeFamilySentence(
      sentence: String,
      opening: Option[String],
      phase: String,
      ply: Int,
      fen: Option[String]
  ): Boolean =
    val openingLower = normalizeOpening(opening)
    if openingLower.isEmpty || !isOpeningStage(phase, ply) then false
    else
      val mentionedFamilies = detectMentionedFamilyIds(sentence)
      val labelMismatch =
        mentionedFamilies.nonEmpty &&
          mentionedFamilies.forall(familyId => !openingMatchesFamily(openingLower, familyId))
      val structureMismatch =
        mentionedFamilies.nonEmpty &&
          mentionedFamilies.forall(familyId => !structureMatchesFamily(fen, familyId))
      labelMismatch && structureMismatch

  private def neutralizeUnsupportedFamilyClaims(
      text: String,
      opening: Option[String],
      phase: String,
      ply: Int,
      fen: Option[String]
  ): String =
    val matcher = sentenceBoundaryRegex.matcher(text)
    val sb = new StringBuilder()
    var start = 0

    while matcher.find() do
      val sentence = text.substring(start, matcher.start())
      if shouldNeutralizeFamilySentence(sentence, opening, phase, ply, fen) then
        sb.append("This move follows a thematic idea, but that specific opening-family claim does not match the current pawn structure.")
      else sb.append(sentence)
      sb.append(matcher.group())
      start = matcher.end()

    val tail = text.substring(start)
    if shouldNeutralizeFamilySentence(tail, opening, phase, ply, fen) then
      sb.append("This move follows a thematic idea, but that specific opening-family claim does not match the current pawn structure.")
    else sb.append(tail)

    sb.toString

  private def collapseRepeatedDots(input: String): String =
    @annotation.tailrec
    def loop(text: String): String =
      if text.contains("..") then loop(text.replace("..", "."))
      else text
    loop(input)

  def sanitize(text: String, opening: Option[String], phase: String, ply: Int, fen: Option[String] = None): String =
    val src = Option(text).getOrElse("")
    val noInvalidFamilyClaim = neutralizeUnsupportedFamilyClaims(src, opening, phase, ply, fen)

    collapseRepeatedDots(noInvalidFamilyClaim)
      .replaceAll(""":\s*:""", ": ")
      .replaceAll("""\.\s*:""", ". ")
      .replaceAll("""\s+\.""", ".")

private[llm] object PolishValidation:

  case class ValidationResult(isValid: Boolean, reasons: List[String])

  private case class MoveMarker(number: Int, style: String):
    inline def token: String = s"$number$style"

  private val moveTokenRegex =
    """(?<![A-Za-z0-9])((?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?)(?:[+#])?(?:[!?]{1,2})?)""".r
  private val moveMarkerRegex =
    """(?<![A-Za-z0-9])(\d+)(\.\.\.|\.|)(?=\s*(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?(?:[+#])?(?:[!?]{1,2})?))""".r
  private val evalTokenRegex =
    """(?i)\(\s*(?:[+-]?\d+(?:\.\d+)?|#?[+-]?\d+|mate\s+in\s+\d+|mated\s+in\s+\d+)\s*\)""".r
  private val variationBranchRegex =
    """(?m)^\s*([a-z]\))""".r

  private def canonicalSanToken(token: String): String =
    Option(token).getOrElse("").trim
      .replaceAll("""[!?]+$""", "")
      .replaceAll("""[+#]+$""", "")

  private def canonicalEvalToken(token: String): String =
    Option(token).getOrElse("").trim.toLowerCase.replaceAll("""\s+""", "")

  private def extractMoveTokensOrdered(text: String): List[String] =
    val t = Option(text).getOrElse("")
    moveTokenRegex
      .findAllMatchIn(t)
      .map(m => canonicalSanToken(m.group(1)))
      .filter(_.nonEmpty)
      .toList

  private def extractMoveMarkersOrdered(text: String): List[MoveMarker] =
    val t = Option(text).getOrElse("")
    moveMarkerRegex
      .findAllMatchIn(t)
      .flatMap { m =>
        m.group(1).toIntOption.map(n => MoveMarker(number = n, style = m.group(2)))
      }
      .toList

  private def extractEvalTokensOrdered(text: String): List[String] =
    val t = Option(text).getOrElse("")
    evalTokenRegex.findAllMatchIn(t).map(m => canonicalEvalToken(m.group(0))).toList

  private def extractVariationBranchesOrdered(text: String): List[String] =
    val t = Option(text).getOrElse("")
    variationBranchRegex.findAllMatchIn(t).map(_.group(1).toLowerCase).toList

  private def isSubsequence(xs: List[String], ys: List[String]): Boolean =
    if xs.isEmpty then true
    else
      @annotation.tailrec
      def loop(restXs: List[String], restYs: List[String]): Boolean =
        (restXs, restYs) match
          case (Nil, _) => true
          case (_, Nil) => false
          case (xh :: xt, yh :: yt) =>
            if xh == yh then loop(xt, yt) else loop(restXs, yt)
      loop(xs, ys)

  private def tokenCounts(xs: List[String]): Map[String, Int] =
    xs.groupMapReduce(identity)(_ => 1)(_ + _)

  def validatePolishedCommentary(
      polished: String,
      original: String,
      allowedSans: List[String]
  ): ValidationResult =
    val polishedMoves = extractMoveTokensOrdered(polished)
    val originalMoves = extractMoveTokensOrdered(original)
    val originalMarkers = extractMoveMarkersOrdered(original)
    val polishedMarkers = extractMoveMarkersOrdered(polished)
    val allowedMoves = originalMoves ++ allowedSans.map(canonicalSanToken).filter(_.nonEmpty)
    val allowedCount = tokenCounts(allowedMoves)
    val polishedCount = tokenCounts(polishedMoves)
    val originalHasMoves = originalMoves.nonEmpty

    val withinCountBudget = polishedCount.forall { case (san, count) =>
      count <= allowedCount.getOrElse(san, 0)
    }
    val sanPresenceSatisfied = !originalHasMoves || polishedMoves.nonEmpty
    val preservesOriginalSequence = isSubsequence(originalMoves, polishedMoves)
    val respectsAllowedOrder = isSubsequence(polishedMoves, allowedMoves)
    val numberingRequired = originalMarkers.nonEmpty && originalHasMoves
    val numberingSatisfied = !numberingRequired || polishedMarkers.nonEmpty
    val originalNumberSet = originalMarkers.map(_.number).toSet
    val polishedMarkersOnOriginalNumbers = polishedMarkers.filter(m => originalNumberSet.contains(m.number))
    val originalMarkerTokens = originalMarkers.map(_.token)
    val polishedMarkerTokensOnOriginalNumbers = polishedMarkersOnOriginalNumbers.map(_.token)
    val markerStyleOrderSatisfied =
      polishedMarkerTokensOnOriginalNumbers.isEmpty ||
        isSubsequence(polishedMarkerTokensOnOriginalNumbers, originalMarkerTokens)

    val originalEvalTokens = extractEvalTokensOrdered(original)
    val polishedEvalTokens = extractEvalTokensOrdered(polished)
    val evalAllowedCount = tokenCounts(originalEvalTokens)
    val evalPolishedCount = tokenCounts(polishedEvalTokens)
    val evalWithinCountBudget = evalPolishedCount.forall { case (token, count) =>
      count <= evalAllowedCount.getOrElse(token, 0)
    }
    val evalOrderSatisfied = isSubsequence(originalEvalTokens, polishedEvalTokens)

    val originalBranches = extractVariationBranchesOrdered(original)
    val polishedBranches = extractVariationBranchesOrdered(polished)
    val branchAllowedCount = tokenCounts(originalBranches)
    val branchPolishedCount = tokenCounts(polishedBranches)
    val branchWithinCountBudget = branchPolishedCount.forall { case (label, count) =>
      count <= branchAllowedCount.getOrElse(label, 0)
    }
    val branchOrderSatisfied = isSubsequence(originalBranches, polishedBranches)

    val reasons = List.newBuilder[String]
    if !sanPresenceSatisfied then reasons += "san_missing"
    if !withinCountBudget then reasons += "count_budget_exceeded"
    if !preservesOriginalSequence then reasons += "san_core_missing"
    if !respectsAllowedOrder then reasons += "san_order_violation"
    if !numberingSatisfied then reasons += "numbering_missing"
    if !markerStyleOrderSatisfied then reasons += "marker_style_mismatch"
    if originalEvalTokens.nonEmpty && polishedEvalTokens.isEmpty then reasons += "eval_missing"
    if !evalWithinCountBudget then reasons += "eval_count_budget_exceeded"
    if !evalOrderSatisfied then reasons += "eval_order_violation"
    if !branchWithinCountBudget then reasons += "variation_branch_count_exceeded"
    if !branchOrderSatisfied then reasons += "variation_branch_violation"

    val reasonList = reasons.result()
    ValidationResult(
      isValid = reasonList.isEmpty,
      reasons = reasonList
    )

  def isPolishedCommentaryValid(
      polished: String,
      original: String,
      allowedSans: List[String]
  ): Boolean =
    validatePolishedCommentary(polished, original, allowedSans).isValid
