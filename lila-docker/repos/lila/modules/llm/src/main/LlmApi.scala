package lila.llm

import scala.concurrent.Future
import scala.util.control.NonFatal
import java.util.concurrent.atomic.{ AtomicLong, AtomicReference }
import java.time.Instant
import java.util.UUID
import lila.llm.analysis.{ ActiveBranchDossierBuilder, ActiveStrategicNoteValidator, AuthoringEvidenceSummaryBuilder, BookmakerPolishSlots, BookmakerPolishSlotsBuilder, BookmakerProseContract, BookmakerSoftRepair, BookmakerStrategicLedgerBuilder, BookStyleRenderer, CommentaryEngine, CommentaryOpsBoard, CommentaryOpsSignals, CommentaryPayloadNormalizer, NarrativeContextBuilder, NarrativeUtils, OpeningExplorerClient, PlanEvidenceEvaluator, StrategicIdeaSelector, StrategicSignalMatcher, StrategyPackBuilder }
import lila.llm.model.OpeningReference
import lila.llm.model.structure.StructureId
import lila.llm.model.strategic.{ VariationLine, TheoreticalOutcomeHint }

/** Pipeline: CommentaryEngine → BookStyleRenderer (rule-based only). */
final class LlmApi(
    openingExplorer: OpeningExplorerClient,
    geminiClient: GeminiClient,
    openAiClient: OpenAiClient,
    commentaryCache: CommentaryCache,
    llmConfig: LlmConfig = LlmConfig.fromEnv,
    providerConfig: LlmProviderConfig = LlmProviderConfig.fromEnv,
    ccaHistoryRepo: Option[CcaHistoryRepo] = None
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
  private val planRecallHypothesisFirstTotal = new AtomicLong(0L)
  private val planRecallHypothesisFirstHit = new AtomicLong(0L)
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
  private val bookmakerCompareObservedCount = new AtomicLong(0L)
  private val bookmakerCompareConsistentCount = new AtomicLong(0L)
  private val fullGameCompareObservedCount = new AtomicLong(0L)
  private val fullGameCompareConsistentCount = new AtomicLong(0L)
  private val activeThesisAgreementTotal = new AtomicLong(0L)
  private val activeThesisAgreementHit = new AtomicLong(0L)
  private val activeNoteSelectedCount = new AtomicLong(0L)
  private val activeNoteAttemptCount = new AtomicLong(0L)
  private val activeNoteAttachedCount = new AtomicLong(0L)
  private val activeNoteOmittedCount = new AtomicLong(0L)
  private val activeNotePrimaryAcceptedCount = new AtomicLong(0L)
  private val activeNoteRepairAttemptCount = new AtomicLong(0L)
  private val activeNoteRepairRecoveredCount = new AtomicLong(0L)
  private val activeRouteRedeployCount = new AtomicLong(0L)
  private val activeRouteMoveRefCount = new AtomicLong(0L)
  private val activeRouteHiddenSafetyCount = new AtomicLong(0L)
  private val activeRouteTowardOnlyCount = new AtomicLong(0L)
  private val activeRouteExactSurfaceCount = new AtomicLong(0L)
  private val activeRouteOpponentHiddenCount = new AtomicLong(0L)
  private val activeDossierEligibleCount = new AtomicLong(0L)
  private val activeDossierAttachedCount = new AtomicLong(0L)
  private val activeDossierCompareObservedCount = new AtomicLong(0L)
  private val activeDossierCompareHitCount = new AtomicLong(0L)
  private val activeDossierRouteObservedCount = new AtomicLong(0L)
  private val activeDossierRouteHitCount = new AtomicLong(0L)
  private val activeDossierReferenceFailureCount = new AtomicLong(0L)
  private val activeNoteFailureReasonCount = scala.collection.concurrent.TrieMap.empty[String, AtomicLong]
  private val activeNoteWarningReasonCount = scala.collection.concurrent.TrieMap.empty[String, AtomicLong]
  private val activeNoteObservedModelCount = scala.collection.concurrent.TrieMap.empty[String, AtomicLong]
  private val activeNoteProviderRef = new AtomicReference("")
  private val activeNoteConfiguredModelRef = new AtomicReference("")
  private val activeNoteFallbackModelRef = new AtomicReference("")
  private val activeNoteReasoningEffortRef = new AtomicReference("")
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
      meta: Option[PolishMetaV1],
      promptUsages: List[(String, OpenAiPolishResult)] = Nil
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
        purpose == "theme_plan_validation" ||
        purpose == "free_tempo_branches" ||
        purpose == "latent_plan_immediate" ||
        purpose == "latent_plan_refutation"
      }

  private def recordStrategicMetrics(
      data: lila.llm.model.ExtendedAnalysisData,
      ctx: lila.llm.model.NarrativeContext,
      probeResults: List[lila.llm.model.ProbeResult]
  ): Unit =
    val topRulePlan = data.plans.headOption.map(_.plan.id.toString)
    val topRulePlansAt3 = data.plans.take(3).map(_.plan.id.toString).toSet
    val topHypothesis = ctx.mainStrategicPlans.headOption.map(_.planId)

    if topHypothesis.isDefined then
      planRecallTotal.incrementAndGet()
      if topRulePlan.isDefined && topRulePlan == topHypothesis then planRecallHit.incrementAndGet()
      planRecallHypothesisFirstTotal.incrementAndGet()
      if topHypothesis.exists(topRulePlansAt3.contains) then planRecallHypothesisFirstHit.incrementAndGet()

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

  private def incActiveNoteReason(reason: String): Unit =
    activeNoteFailureReasonCount.getOrElseUpdate(reason, new AtomicLong(0L)).incrementAndGet()

  private def shouldLogOpsSample(kind: String): Boolean =
    val count = opsSampleCount.getOrElseUpdate(kind, new AtomicLong(0L)).incrementAndGet()
    count <= 5 || count % 50 == 0

  private def logOpsSample(kind: String, fields: (String, String)*): Unit =
    lila.mon.llm.commentary.sample(kind).increment()
    if shouldLogOpsSample(kind) then
      commentaryOpsBoard.recordSample(kind = kind, fields = fields.toMap)
      val rendered =
        fields
          .map { case (k, v) =>
            val safe = Option(v).getOrElse("").replaceAll("""[\r\n\t]+""", " ").trim
            s"$k=${if safe.nonEmpty then safe else "-"}"
          }
          .mkString(" ")
      logger.warn(s"llm.ops.sample kind=$kind $rendered")

  private def recordComparisonObservation(
      path: String,
      digest: Option[DecisionComparisonDigest],
      whyAbsent: List[String],
      authorEvidence: List[AuthorEvidenceSummary],
      probeRequests: List[lila.llm.model.ProbeRequest],
      strategyPack: Option[StrategyPack]
  ): Unit =
    CommentaryOpsSignals
      .decisionComparisonConsistency(
        digest = digest,
        whyAbsent = whyAbsent,
        authorEvidence = authorEvidence,
        probeRequests = probeRequests,
        strategyPack = strategyPack
      )
      .foreach { obs =>
        val (total, hit) =
          path match
            case "bookmaker" => (bookmakerCompareObservedCount, bookmakerCompareConsistentCount)
            case _           => (fullGameCompareObservedCount, fullGameCompareConsistentCount)
        total.incrementAndGet()
        if obs.consistent then hit.incrementAndGet()
        lila.mon.llm.commentary.compare(path, obs.consistent).increment()
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

  private def traceBookmakerFallbackResponse(
      response: CommentResponse,
      phase: String,
      ply: Int,
      lastMove: Option[String],
      cacheHit: Boolean,
      openingRefPresent: Boolean,
      incomingProbeCount: Int
  ): Unit =
    val sourceMode = Option(response.sourceMode).map(_.trim).filter(_.nonEmpty).getOrElse("rule")
    val shouldTrace =
      sourceMode == "rule_circuit_open" || sourceMode.startsWith("fallback_rule")
    if shouldTrace then
      val reasons =
        response.polishMeta
          .map(_.validationReasons.map(_.trim).filter(_.nonEmpty).distinct)
          .getOrElse(Nil)
      val provider = response.polishMeta.map(_.provider).getOrElse("")
      val model = response.polishMeta.flatMap(_.model).orElse(response.model).getOrElse("")
      val sampleKind =
        sourceMode match
          case "fallback_rule_invalid" => "bookmaker_fallback.invalid"
          case "fallback_rule_empty"   => "bookmaker_fallback.empty"
          case "rule_circuit_open"     => "bookmaker_fallback.circuit_open"
          case other                   => s"bookmaker_fallback.${other.replaceAll("""[^a-zA-Z0-9]+""", "_")}"
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
      logger.warn(s"llm.bookmaker.fallback $rendered")

  private def recordActiveThesisAgreement(
      moment: GameNarrativeMoment,
      note: String
  ): Unit =
    val observation =
      CommentaryOpsSignals.activeThesisAgreement(
        baseNarrative = moment.narrative,
        activeNote = note,
        digest = moment.signalDigest
      )
    activeThesisAgreementTotal.incrementAndGet()
    if observation.agreed then activeThesisAgreementHit.incrementAndGet()
    lila.mon.llm.commentary.thesisAgreement("active", observation.agreed).increment()
    updateCommentaryOpsGauges()
    if !observation.agreed then
      logOpsSample(
        kind = "thesis_disagreement.active",
        "momentType" -> moment.momentType,
        "labels" -> observation.dominantLabels.take(4).mkString(" | "),
        "sourceMode" -> moment.activeStrategicSourceMode.getOrElse("")
      )

  private def recordActiveDossierOutcome(
      dossier: Option[ActiveBranchDossier],
      note: Option[String],
      hardReasons: List[String],
      warningReasons: List[String],
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef]
  ): Unit =
    dossier.foreach { value =>
      activeDossierEligibleCount.incrementAndGet()
      if note.exists(_.trim.nonEmpty) then activeDossierAttachedCount.incrementAndGet()
      activeDossierCompareObservedCount.incrementAndGet()
      val normalizedNote = note.map(_.toLowerCase).getOrElse("")
      val comparison = value.moveCue.map { cue =>
        DecisionComparisonDigest(
          chosenMove = cue.san,
          engineBestMove = cue.san.orElse(Some(cue.uci))
        )
      }
      val compareHit =
        List(
          Some(value.chosenBranchLabel),
          value.engineBranchLabel,
          value.deferredBranchLabel
        ).flatten.exists { label =>
          StrategicSignalMatcher.containsComparablePhrase(normalizedNote, label)
        }
      if compareHit then activeDossierCompareHitCount.incrementAndGet()

      val routeObserved = value.routeCue.nonEmpty || routeRefs.nonEmpty || moveRefs.nonEmpty
      val routeHit =
        routeObserved && (
          value.routeCue.exists(cue => StrategicSignalMatcher.routeCueMentioned(normalizedNote, cue)) ||
            routeRefs.exists(ref => StrategicSignalMatcher.routeRefMentioned(normalizedNote, ref)) ||
            value.moveCue.exists(cue => StrategicSignalMatcher.moveCueMentioned(normalizedNote, cue, comparison)) ||
            moveRefs.exists(ref => StrategicSignalMatcher.moveRefMentioned(normalizedNote, ref, comparison))
        )
      if routeObserved then
        activeDossierRouteObservedCount.incrementAndGet()
        if routeHit then activeDossierRouteHitCount.incrementAndGet()
        else activeDossierReferenceFailureCount.incrementAndGet()

      updateCommentaryOpsGauges()

      val hardTags = hardReasons.map(_.trim).filter(_.nonEmpty).distinct
      val warningTags = warningReasons.map(_.trim).filter(_.nonEmpty).distinct
      if hardTags.nonEmpty || warningTags.nonEmpty || !compareHit || (routeObserved && !routeHit) then
        logOpsSample(
          kind = "active_dossier.sample",
          "lens" -> value.dominantLens,
          "chosen" -> value.chosenBranchLabel,
          "deferred" -> value.deferredBranchLabel.getOrElse(""),
          "route" -> value.routeCue.map(_.routeId).getOrElse(""),
          "move" -> value.moveCue.map(_.label).getOrElse(""),
          "hardReasons" -> hardTags.mkString(","),
          "warningReasons" -> warningTags.mkString(","),
          "note" -> note.getOrElse("").take(320)
        )
    }

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
    updateCommentaryOpsGauges()

  private def recordLatencyMetrics(totalLatencyMs: Long, structureEvalLatencyMs: Option[Long]): Unit =
    latencyWindowLock.synchronized {
      totalLatencyWindowMs = pushWindow(totalLatencyWindowMs, totalLatencyMs.max(0L))
      structureEvalLatencyMs.foreach { ms =>
        structureEvalLatencyWindowMs = pushWindow(structureEvalLatencyWindowMs, ms.max(0L))
      }
    }

  private def recordActiveNoteOutcome(
      attached: Boolean,
      hardReasons: List[String],
      warningReasons: List[String],
      primaryAccepted: Boolean,
      repairAttempted: Boolean,
      repairRecovered: Boolean,
      provider: Option[String],
      configuredModel: Option[String],
      fallbackModel: Option[String],
      reasoningEffort: Option[String],
      observedModels: List[String]
  ): Unit =
    val attempt = activeNoteAttemptCount.incrementAndGet()
    if attached then activeNoteAttachedCount.incrementAndGet()
    else activeNoteOmittedCount.incrementAndGet()
    if primaryAccepted then activeNotePrimaryAcceptedCount.incrementAndGet()
    if repairAttempted then activeNoteRepairAttemptCount.incrementAndGet()
    if repairRecovered then activeNoteRepairRecoveredCount.incrementAndGet()
    hardReasons.map(_.trim).filter(_.nonEmpty).distinct.foreach(incActiveNoteReason)
    warningReasons
      .map(_.trim)
      .filter(_.nonEmpty)
      .distinct
      .foreach(reason => activeNoteWarningReasonCount.getOrElseUpdate(reason, new AtomicLong(0L)).incrementAndGet())
    provider.flatMap(trimmedOption).foreach(activeNoteProviderRef.set)
    configuredModel.flatMap(trimmedOption).foreach(activeNoteConfiguredModelRef.set)
    fallbackModel.flatMap(trimmedOption).foreach(activeNoteFallbackModelRef.set)
    reasoningEffort.flatMap(trimmedOption).foreach(activeNoteReasoningEffortRef.set)
    observedModels
      .flatMap(trimmedOption)
      .distinct
      .foreach(model => activeNoteObservedModelCount.getOrElseUpdate(model, new AtomicLong(0L)).incrementAndGet())
    updateCommentaryOpsGauges()
    if attempt > 0 && attempt % 100 == 0 then maybeLogActiveNoteMetrics()

  private def maybeLogActiveNoteMetrics(): Unit =
    val selected = activeNoteSelectedCount.get()
    val attempts = activeNoteAttemptCount.get()
    val attached = activeNoteAttachedCount.get()
    val omitted = activeNoteOmittedCount.get()
    val primaryAccepted = activeNotePrimaryAcceptedCount.get()
    val repairAttempts = activeNoteRepairAttemptCount.get()
    val repairRecovered = activeNoteRepairRecoveredCount.get()
    val thesisAgreementRate =
      if activeThesisAgreementTotal.get() == 0 then 0.0
      else activeThesisAgreementHit.get().toDouble / activeThesisAgreementTotal.get().toDouble
    val attachRate =
      if attempts == 0 then 0.0
      else attached.toDouble / attempts.toDouble
    val dossierAttachRate =
      if activeDossierEligibleCount.get() == 0 then 0.0
      else activeDossierAttachedCount.get().toDouble / activeDossierEligibleCount.get().toDouble
    val dossierCompareRate =
      if activeDossierCompareObservedCount.get() == 0 then 0.0
      else activeDossierCompareHitCount.get().toDouble / activeDossierCompareObservedCount.get().toDouble
    val dossierRouteRate =
        if activeDossierRouteObservedCount.get() == 0 then 0.0
        else activeDossierRouteHitCount.get().toDouble / activeDossierRouteObservedCount.get().toDouble
    val reasonDist = activeNoteFailureReasonCount.toList.map { case (k, v) => k -> v.get() }.toMap
    val warningDist = activeNoteWarningReasonCount.toList.map { case (k, v) => k -> v.get() }.toMap
    val observedModelDist = activeNoteObservedModelCount.toList.map { case (k, v) => k -> v.get() }.toMap
    val provider = trimmedOption(activeNoteProviderRef.get()).getOrElse("-")
    val configuredModel = trimmedOption(activeNoteConfiguredModelRef.get()).getOrElse("-")
    val fallbackModel = trimmedOption(activeNoteFallbackModelRef.get()).getOrElse("-")
    val reasoningEffort = trimmedOption(activeNoteReasoningEffortRef.get()).getOrElse("-")
    val routeRedeployCount = activeRouteRedeployCount.get()
    val routeMoveRefCount = activeRouteMoveRefCount.get()
    val routeHiddenSafetyCount = activeRouteHiddenSafetyCount.get()
    val routeTowardOnlyCount = activeRouteTowardOnlyCount.get()
    val routeExactSurfaceCount = activeRouteExactSurfaceCount.get()
    val routeOpponentHiddenCount = activeRouteOpponentHiddenCount.get()
    val activePromptUsage = promptUsageSnapshot(prefix = Some("active_note"))
    logger.info(
      s"active_note.metrics active_note_selected_count=$selected " +
        s"active_note_attempt_count=$attempts active_note_attached_count=$attached active_note_omitted_count=$omitted " +
        s"active_note_primary_accepted_count=$primaryAccepted active_note_repair_attempt_count=$repairAttempts " +
        s"active_note_repair_recovered_count=$repairRecovered " +
        s"route_redeploy_count=$routeRedeployCount route_move_ref_count=$routeMoveRefCount " +
        s"route_hidden_safety_count=$routeHiddenSafetyCount route_toward_only_count=$routeTowardOnlyCount " +
        s"route_exact_surface_count=$routeExactSurfaceCount route_opponent_hidden_count=$routeOpponentHiddenCount " +
        f"attach_rate=$attachRate%.3f thesis_agreement_rate=$thesisAgreementRate%.3f " +
        f"dossier_attach_rate=$dossierAttachRate%.3f dossier_compare_rate=$dossierCompareRate%.3f dossier_route_ref_rate=$dossierRouteRate%.3f " +
        s"active_provider=$provider active_model=$configuredModel active_fallback=$fallbackModel active_reasoning=$reasoningEffort " +
        s"active_observed_models=$observedModelDist " +
        s"active_note_failure_reason_count=$reasonDist active_note_warning_reason_count=$warningDist " +
        s"active_prompt_usage=$activePromptUsage"
    )

  def commentaryOpsSnapshot(limit: Int = 20): CommentaryOpsBoard.Snapshot =
    val requests = bookmakerRequests.get()
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
    val bookmakerCompareRate =
      if bookmakerCompareObservedCount.get() == 0 then 1.0
      else bookmakerCompareConsistentCount.get().toDouble / bookmakerCompareObservedCount.get().toDouble
    val fullGameCompareRate =
      if fullGameCompareObservedCount.get() == 0 then 1.0
      else fullGameCompareConsistentCount.get().toDouble / fullGameCompareObservedCount.get().toDouble
    val avgCostUsd =
      if attempts == 0 then 0.0
      else polishEstimatedCostMicros.get().toDouble / attempts.toDouble / 1000000.0
    val thesisAgreementRate =
      if activeThesisAgreementTotal.get() == 0 then 1.0
      else activeThesisAgreementHit.get().toDouble / activeThesisAgreementTotal.get().toDouble
    val activeAttempts = activeNoteAttemptCount.get()
    val attachRate =
      if activeAttempts == 0 then 0.0
      else activeNoteAttachedCount.get().toDouble / activeAttempts.toDouble
    val dossierAttachRate =
      if activeDossierEligibleCount.get() == 0 then 0.0
      else activeDossierAttachedCount.get().toDouble / activeDossierEligibleCount.get().toDouble
    val dossierCompareRate =
      if activeDossierCompareObservedCount.get() == 0 then 1.0
      else activeDossierCompareHitCount.get().toDouble / activeDossierCompareObservedCount.get().toDouble
    val dossierRouteRate =
      if activeDossierRouteObservedCount.get() == 0 then 1.0
      else activeDossierRouteHitCount.get().toDouble / activeDossierRouteObservedCount.get().toDouble
    val dossierReferenceFailureRate =
      if activeDossierRouteObservedCount.get() == 0 then 0.0
      else activeDossierReferenceFailureCount.get().toDouble / activeDossierRouteObservedCount.get().toDouble

    CommentaryOpsBoard.Snapshot(
      generatedAtMs = System.currentTimeMillis(),
      bookmaker = CommentaryOpsBoard.BookmakerMetrics(
        requests = requests,
        polishAttempts = attempts,
        polishAccepted = accepted,
        polishFallbackRate = fallbackRate,
        softRepairAnyRate = softRepairAnyRate,
        softRepairMaterialRate = softRepairMaterialRate,
        compareObserved = bookmakerCompareObservedCount.get(),
        compareConsistencyRate = bookmakerCompareRate,
        avgCostUsd = avgCostUsd
      ),
      fullgame = CommentaryOpsBoard.FullGameMetrics(
        compareObserved = fullGameCompareObservedCount.get(),
        compareConsistencyRate = fullGameCompareRate
      ),
      active = CommentaryOpsBoard.ActiveMetrics(
        selectedMoments = activeNoteSelectedCount.get(),
        attempts = activeAttempts,
        attached = activeNoteAttachedCount.get(),
        omitted = activeNoteOmittedCount.get(),
        primaryAccepted = activeNotePrimaryAcceptedCount.get(),
        repairAttempts = activeNoteRepairAttemptCount.get(),
        repairRecovered = activeNoteRepairRecoveredCount.get(),
        attachRate = attachRate,
        thesisAgreementRate = thesisAgreementRate,
        dossierAttachRate = dossierAttachRate,
        dossierCompareRate = dossierCompareRate,
        dossierRouteRefRate = dossierRouteRate,
        dossierReferenceFailureRate = dossierReferenceFailureRate,
        provider = trimmedOption(activeNoteProviderRef.get()),
        configuredModel = trimmedOption(activeNoteConfiguredModelRef.get()),
        fallbackModel = trimmedOption(activeNoteFallbackModelRef.get()),
        reasoningEffort = trimmedOption(activeNoteReasoningEffortRef.get()),
        observedModelDistribution = activeNoteObservedModelCount.view.mapValues(_.get()).toMap,
        omitReasons = activeNoteFailureReasonCount.view.mapValues(_.get()).toMap,
        warningReasons = activeNoteWarningReasonCount.view.mapValues(_.get()).toMap,
        routeRedeployCount = activeRouteRedeployCount.get(),
        routeMoveRefCount = activeRouteMoveRefCount.get(),
        routeHiddenSafetyCount = activeRouteHiddenSafetyCount.get(),
        routeTowardOnlyCount = activeRouteTowardOnlyCount.get(),
        routeExactSurfaceCount = activeRouteExactSurfaceCount.get(),
        routeOpponentHiddenCount = activeRouteOpponentHiddenCount.get()
      ),
      promptUsage = promptUsageSnapshot(),
      recentSamples = commentaryOpsBoard.recentSamples(limit)
    )

  private def updateCommentaryOpsGauges(): Unit =
    val attempts = polishAttemptCount.get()
    val accepted = polishAcceptedCount.get()
    val bookmakerFallbackRate =
      if attempts == 0 then 0.0
      else polishFallbackCount.get().toDouble / attempts.toDouble
    val bookmakerSoftRepairMaterialRate =
      if accepted == 0 then 0.0
      else softRepairMaterialCount.get().toDouble / accepted.toDouble
    val bookmakerCompareRate =
      if bookmakerCompareObservedCount.get() == 0 then 1.0
      else bookmakerCompareConsistentCount.get().toDouble / bookmakerCompareObservedCount.get().toDouble
    val fullGameCompareRate =
      if fullGameCompareObservedCount.get() == 0 then 1.0
      else fullGameCompareConsistentCount.get().toDouble / fullGameCompareObservedCount.get().toDouble
    val activeThesisRate =
      if activeThesisAgreementTotal.get() == 0 then 1.0
      else activeThesisAgreementHit.get().toDouble / activeThesisAgreementTotal.get().toDouble
    val activeAttempts = activeNoteAttemptCount.get()
    val activeAttachRate =
      if activeAttempts == 0 then 0.0
      else activeNoteAttachedCount.get().toDouble / activeAttempts.toDouble
    val activeDossierAttachRate =
      if activeDossierEligibleCount.get() == 0 then 0.0
      else activeDossierAttachedCount.get().toDouble / activeDossierEligibleCount.get().toDouble
    val activeDossierCompareRate =
      if activeDossierCompareObservedCount.get() == 0 then 1.0
      else activeDossierCompareHitCount.get().toDouble / activeDossierCompareObservedCount.get().toDouble
    val activeDossierRouteRate =
      if activeDossierRouteObservedCount.get() == 0 then 1.0
      else activeDossierRouteHitCount.get().toDouble / activeDossierRouteObservedCount.get().toDouble
    val activeDossierReferenceFailureRate =
      if activeDossierRouteObservedCount.get() == 0 then 0.0
      else activeDossierReferenceFailureCount.get().toDouble / activeDossierRouteObservedCount.get().toDouble
    val routeRedeployCount = activeRouteRedeployCount.get().toDouble
    val routeMoveRefCount = activeRouteMoveRefCount.get().toDouble
    val routeHiddenSafetyCount = activeRouteHiddenSafetyCount.get().toDouble
    val routeTowardOnlyCount = activeRouteTowardOnlyCount.get().toDouble
    val routeExactSurfaceCount = activeRouteExactSurfaceCount.get().toDouble
    val routeOpponentHiddenCount = activeRouteOpponentHiddenCount.get().toDouble
    val avgCostUsd =
      if attempts == 0 then 0.0
      else polishEstimatedCostMicros.get().toDouble / attempts.toDouble / 1000000.0
    val latencyP95 = polishLatencyP95Ms

    lila.mon.llm.commentary.metric("polish_fallback_rate", "bookmaker").update(bookmakerFallbackRate)
    lila.mon.llm.commentary.metric("soft_repair_material_rate", "bookmaker").update(bookmakerSoftRepairMaterialRate)
    lila.mon.llm.commentary.metric("compare_consistency_rate", "bookmaker").update(bookmakerCompareRate)
    lila.mon.llm.commentary.metric("compare_consistency_rate", "fullgame").update(fullGameCompareRate)
    lila.mon.llm.commentary.metric("thesis_agreement_rate", "active").update(activeThesisRate)
    lila.mon.llm.commentary.metric("attach_rate", "active").update(activeAttachRate)
    lila.mon.llm.commentary.metric("dossier_attach_rate", "active").update(activeDossierAttachRate)
    lila.mon.llm.commentary.metric("dossier_compare_rate", "active").update(activeDossierCompareRate)
    lila.mon.llm.commentary.metric("dossier_route_ref_rate", "active").update(activeDossierRouteRate)
    lila.mon.llm.commentary.metric("dossier_reference_failure_rate", "active").update(activeDossierReferenceFailureRate)
    lila.mon.llm.commentary.metric("route_redeploy_count", "active").update(routeRedeployCount)
    lila.mon.llm.commentary.metric("route_move_ref_count", "active").update(routeMoveRefCount)
    lila.mon.llm.commentary.metric("route_hidden_safety_count", "active").update(routeHiddenSafetyCount)
    lila.mon.llm.commentary.metric("route_toward_only_count", "active").update(routeTowardOnlyCount)
    lila.mon.llm.commentary.metric("route_exact_surface_count", "active").update(routeExactSurfaceCount)
    lila.mon.llm.commentary.metric("route_opponent_hidden_count", "active").update(routeOpponentHiddenCount)
    lila.mon.llm.commentary.metric("avg_cost_usd", "bookmaker").update(avgCostUsd)
    lila.mon.llm.commentary.metric("polish_latency_p95_ms", "bookmaker").update(latencyP95)

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
    "enabled"

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
      val bookmakerCompareRate =
        if bookmakerCompareObservedCount.get() == 0 then 1.0
        else bookmakerCompareConsistentCount.get().toDouble / bookmakerCompareObservedCount.get().toDouble
      val fullGameCompareRate =
        if fullGameCompareObservedCount.get() == 0 then 1.0
        else fullGameCompareConsistentCount.get().toDouble / fullGameCompareObservedCount.get().toDouble
      val activeThesisAgreementRateValue =
        if activeThesisAgreementTotal.get() == 0 then 1.0
        else activeThesisAgreementHit.get().toDouble / activeThesisAgreementTotal.get().toDouble
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
          f"bookmaker_compare_consistency_rate=$bookmakerCompareRate%.3f " +
          f"fullgame_compare_consistency_rate=$fullGameCompareRate%.3f " +
          f"active_thesis_agreement_rate=$activeThesisAgreementRateValue%.3f " +
          f"plan_recall_at3=$planRecallAt3%.3f " +
          f"plan_recall_at3_legacy=$planRecallAt3Legacy%.3f " +
          f"plan_recall_at3_hypothesis_first=$planRecallAt3HypothesisFirst%.3f " +
          f"latent_precision=$latentPrecision%.3f " +
          f"pv_coupling_ratio=$pvCouplingRatio%.3f " +
          f"evidence_reason_coverage=$evidenceReasonCoverage%.3f " +
          f"hypothesis_probe_hit_rate=$hypothesisProbeHitRate%.3f " +
          f"contract_drop_rate=$contractDropRate%.3f " +
          f"legacy_fen_missing_rate=$legacyFenMissingRate%.3f " +
          f"polish_avg_cost_usd=$avgCostUsd%.6f " +
          s"polish_reason_dist=$polishReasonDist " +
          s"prompt_usage=$promptUsage " +
          s"struct_dist=$structureDist " +
          s"alignment_band_dist=$bandDist"
      )

  private def isLlmPolishEnabledForRequest(allowLlmPolish: Boolean): Boolean =
    if providerConfig.premiumOnly then allowLlmPolish else true

  private def normalizeLang(lang: String): String =
    Option(lang).map(_.trim.toLowerCase).filter(_.nonEmpty).map(_.take(8)).getOrElse(providerConfig.defaultLang)

  private def normalizePlanTier(raw: String): String =
    PlanTier.normalize(raw)

  private def normalizeLlmLevel(raw: String): String =
    LlmLevel.normalize(raw)

  private def effectiveLlmLevel(
      planTier: String,
      requestedLevel: String,
      allowLlmPolish: Boolean
  ): String =
    val tier = normalizePlanTier(planTier)
    val requested = normalizeLlmLevel(requestedLevel)
    if tier == PlanTier.Pro && requested == LlmLevel.Active && allowLlmPolish then LlmLevel.Active
    else LlmLevel.Polish

  private def strategyHints(pack: Option[StrategyPack]): List[String] =
    pack.toList.flatMap(StrategyPackBuilder.promptHints)

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

  private def unwrapCommentaryPayload(text: String): String =
    CommentaryPayloadNormalizer.normalize(text)

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
      allowedSans: List[String],
      slotMode: Boolean = false
  ): PolishValidation.ValidationResult =
    val trimmed = Option(polished).getOrElse("").trim
    if trimmed.isEmpty then PolishValidation.ValidationResult(isValid = false, reasons = List("empty_polish"))
    else if looksJsonWrapper(trimmed) then
      PolishValidation.ValidationResult(isValid = false, reasons = List("json_wrapper_unparsed"))
    else if looksTruncated(trimmed) then
      PolishValidation.ValidationResult(isValid = false, reasons = List("truncated_output"))
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
      s"llm.polish.invalid provider=$provider phase=$phase attempt=$attempt model=${model.getOrElse("-")} reasons=$reasonStr"
    )

  private def trimmedOption(value: String): Option[String] =
    Option(value).map(_.trim).filter(_.nonEmpty)

  private def trimmedOption(value: Option[String]): Option[String] =
    value.flatMap(trimmedOption)

  private def resolvedActiveNoteRouteMeta(
      asyncTier: Boolean,
      planTier: String,
      llmLevel: String
  ): ActiveNoteRouteMeta =
    providerConfig.activeNoteProviderResolved match
      case "openai" =>
        val route = openAiClient.activeNoteRouteSummary(asyncTier, planTier, llmLevel)
        ActiveNoteRouteMeta(
          provider = "openai",
          configuredModel = trimmedOption(route.primary),
          fallbackModel = trimmedOption(route.fallback),
          reasoningEffort = trimmedOption(route.reasoningEffort)
        )
      case "gemini" =>
        ActiveNoteRouteMeta(
          provider = "gemini",
          configuredModel = trimmedOption(geminiClient.activeModelName),
          fallbackModel = None,
          reasoningEffort = None
        )
      case other =>
        ActiveNoteRouteMeta(
          provider = other,
          configuredModel = None,
          fallbackModel = None,
          reasoningEffort = None
        )

  private def activeNoteCacheFingerprint(
      asyncTier: Boolean,
      planTier: String,
      llmLevel: String
  ): String =
    if planTier != PlanTier.Pro || llmLevel != LlmLevel.Active then "-"
    else
      val route = resolvedActiveNoteRouteMeta(asyncTier, planTier, llmLevel)
      List(
        route.provider,
        route.configuredModel.getOrElse("-"),
        route.fallbackModel.getOrElse("-"),
        route.reasoningEffort.getOrElse("-")
      ).mkString(":")

  private def llmCacheContext(
      allowLlmPolish: Boolean,
      lang: String,
      asyncTier: Boolean,
      planTier: String,
      llmLevel: String
  ): Option[LlmCacheContext] =
    if !isLlmPolishEnabledForRequest(allowLlmPolish) then None
    else
      val normalizedLang = normalizeLang(lang)
      val normalizedPlanTier = normalizePlanTier(planTier)
      val normalizedLlmLevel = normalizeLlmLevel(llmLevel)
      val baseModel =
        providerConfig.provider match
          case "openai" if openAiClient.isEnabled =>
            Some(
              if asyncTier then openAiClient.asyncModelName(normalizedPlanTier, normalizedLlmLevel)
              else openAiClient.syncModelName(normalizedPlanTier, normalizedLlmLevel)
            )
          case "gemini" if geminiClient.isEnabled => Some(geminiClient.modelName)
          case "none"                             => Some("rule")
          case other                              => Some(s"${other}_unavailable")
      baseModel.map { model =>
        LlmCacheContext(
          model = model,
          promptVersion = providerConfig.promptVersion,
          lang = normalizedLang,
          planTier = normalizedPlanTier,
          llmLevel = normalizedLlmLevel,
          activeNoteFingerprint = activeNoteCacheFingerprint(asyncTier, normalizedPlanTier, normalizedLlmLevel)
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
      case None                                                     => "bookmaker"

  private def shouldTrySegmentPolish(
      prose: String,
      refs: Option[BookmakerRefsV1],
      allowedSans: List[String],
      momentType: Option[String],
      bookmakerSlots: Option[BookmakerPolishSlots]
  ): Boolean =
    val normalizedMomentType = momentType.map(_.trim.toLowerCase).getOrElse("")
    val introOrConclusion =
      normalizedMomentType == "game intro" || normalizedMomentType == "game conclusion"
    val hasConcreteEvidence = refs.exists(_.variations.nonEmpty) || allowedSans.distinct.size >= SegmentPolishMinAllowedSans
    bookmakerSlots.isEmpty &&
      !introOrConclusion &&
      hasConcreteEvidence &&
      wordCount(prose) >= SegmentPolishMinWords

  private def adaptiveOutputTokenCap(
      prose: String,
      refs: Option[BookmakerRefsV1],
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

  // Pro(active) uses stronger models, so keep the gate lenient to reduce over-fallback.
  private val StrategyCoverageThreshold = 0.34

  private case class StrategyCoverageEvaluation(
      meta: Option[StrategyCoverageMetaV1],
      reasons: List[String]
  )

  private def evaluateStrategyCoverage(
      commentary: String,
      strategyPack: Option[StrategyPack],
      planTier: String,
      llmLevel: String
  ): StrategyCoverageEvaluation =
    val normalizedTier = normalizePlanTier(planTier)
    val normalizedLevel = normalizeLlmLevel(llmLevel)
    val shouldEnforce = normalizedTier == PlanTier.Pro && normalizedLevel == LlmLevel.Active
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
              StrategyCoverageMetaV1(
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

          val planCandidates =
            val ideaCandidates =
              pack.strategicIdeas
                .map(idea => s"${StrategicIdeaSelector.humanizedKind(idea.kind)} ${StrategicIdeaSelector.focusSummary(idea)}")
                .filter(_.trim.nonEmpty)
                .distinct
                .take(2)
            if ideaCandidates.nonEmpty then ideaCandidates
            else pack.plans.map(_.planName).filter(_.trim.nonEmpty).distinct.take(2)
          val planSignals = planCandidates.size
          val planHits = planCandidates.count { planName =>
            val planTokens = StrategicSignalMatcher.signalTokens(planName)
            StrategicSignalMatcher.phraseMentioned(commentaryLower, planName) ||
            (planTokens.nonEmpty && planTokens.intersect(commentaryTokens).nonEmpty)
          }

          val routeCandidates = pack.pieceRoutes.take(2)
          val routeSignals = routeCandidates.size
          val routeHits = routeCandidates.count(StrategicSignalMatcher.routeMentioned(commentaryLower, _))

          val focusCandidates = pack.longTermFocus.map(_.trim).filter(_.nonEmpty).distinct.take(2)
          val focusSignals = focusCandidates.size
          val focusHits = focusCandidates.count { focus =>
            val focusTokens = StrategicSignalMatcher.signalTokens(focus)
            StrategicSignalMatcher.phraseMentioned(commentaryLower, focus) ||
            (focusTokens.nonEmpty && focusTokens.intersect(commentaryTokens).nonEmpty)
          }

          val availableCategories = List(planSignals, routeSignals, focusSignals).count(_ > 0)
          val coveredCategories = List(planHits, routeHits, focusHits).count(_ > 0)
          // Lenient gate: at least one strategic axis is enough to pass.
          val requiredCategories =
            if availableCategories == 0 then 0
            else 1
          val coverageScore =
            if availableCategories == 0 then 1.0
            else coveredCategories.toDouble / availableCategories.toDouble
          val passesThreshold = coveredCategories >= requiredCategories
          val missingReasons =
            if passesThreshold then Nil
            else
              (
                List(
                  Option.when(planSignals > 0 && planHits == 0)("strategy_plan_missing"),
                  Option.when(routeSignals > 0 && routeHits == 0)("strategy_route_missing"),
                  Option.when(focusSignals > 0 && focusHits == 0)("strategy_focus_missing"),
                  Some("strategy_coverage_low")
                ).flatten.distinct
              )

          StrategyCoverageEvaluation(
            meta = Some(
              StrategyCoverageMetaV1(
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
      strategyCoverage: Option[StrategyCoverageMetaV1],
      softRepairApplied: Boolean = false,
      softRepairActions: List[String] = Nil,
      softRepairMaterialApplied: Boolean = false,
      softRepairMaterialActions: List[String] = Nil
  )

  private case class ActiveStrategicNoteDecision(
      note: Option[String],
      sourceMode: Option[String],
      hardReasons: List[String],
      warningReasons: List[String] = Nil,
      provider: Option[String] = None,
      configuredModel: Option[String] = None,
      fallbackModel: Option[String] = None,
      reasoningEffort: Option[String] = None,
      observedModels: List[String] = Nil,
      primaryAccepted: Boolean = false,
      repairAttempted: Boolean = false,
      repairRecovered: Boolean = false,
      promptUsages: List[(String, OpenAiPolishResult)] = Nil
  )

  private case class ActiveNoteRouteMeta(
      provider: String,
      configuredModel: Option[String],
      fallbackModel: Option[String],
      reasoningEffort: Option[String]
  )

  private def validateCandidate(
      candidateText: String,
      originalProse: String,
      allowedSans: List[String],
      anchors: MoveAnchorCodec.EncodedCommentary,
      extraReasons: List[String],
      strategyPack: Option[StrategyPack],
      planTier: String,
      llmLevel: String,
      bookmakerSlots: Option[BookmakerPolishSlots]
  ): CandidateValidation =
    val normalizedCandidate = unwrapCommentaryPayload(candidateText)
    val anchorReasons =
      if anchors.hasAnchors && bookmakerSlots.isEmpty then
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
    val repaired = bookmakerSlots match
      case Some(slots) =>
        val repair = BookmakerSoftRepair.repair(decoded, slots)
        repair
      case None =>
        BookmakerSoftRepair.RepairResult(
          text = decoded,
          applied = false,
          actions = Nil,
          materialApplied = false,
          materialActions = Nil,
          evaluation = BookmakerProseContract.Evaluation(Nil, claimLikeFirstParagraph = true, paragraphBudgetOk = true, placeholderHits = Nil, genericHits = Nil)
        )
    if repaired.applied then
      softRepairAppliedCount.incrementAndGet()
      lila.mon.llm.commentary.repair("bookmaker", "any").increment()
      logger.debug(s"llm.polish.soft_repair applied=true actions=${repaired.actions.mkString(",")}")
    if repaired.materialApplied then
      softRepairMaterialCount.incrementAndGet()
      lila.mon.llm.commentary.repair("bookmaker", "material").increment()
      logOpsSample(
        kind = "material_repair.bookmaker",
        "actions" -> repaired.materialActions.mkString(","),
        "lens" -> bookmakerSlots.map(_.lens.toString).getOrElse("")
      )
    val proseValidation = validatePolishedCommentary(repaired.text, originalProse, allowedSans, slotMode = bookmakerSlots.isDefined)
    val strategyValidation = evaluateStrategyCoverage(repaired.text, strategyPack, planTier, llmLevel)
    val reasons = (anchorReasons ++ proseValidation.reasons ++ strategyValidation.reasons ++ extraReasons).distinct
    CandidateValidation(
      isValid = reasons.isEmpty,
      decodedText = repaired.text,
      reasons = reasons,
      strategyCoverage = strategyValidation.meta,
      softRepairApplied = repaired.applied,
      softRepairActions = repaired.actions,
      softRepairMaterialApplied = repaired.materialApplied,
      softRepairMaterialActions = repaired.materialActions
    )

  private def validateActiveStrategicNote(
      candidateText: String,
      baseNarrative: String,
      dossier: Option[ActiveBranchDossier],
      strategyPack: Option[StrategyPack],
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef]
  ): ActiveStrategicNoteValidator.Result =
    val trimmed = Option(candidateText).map(_.trim).getOrElse("")
    val strategyReasons =
      if trimmed.isEmpty then Nil
      else evaluateStrategyCoverage(trimmed, strategyPack, PlanTier.Pro, LlmLevel.Active).reasons
    ActiveStrategicNoteValidator.validate(
      candidateText = candidateText,
      baseNarrative = baseNarrative,
      dossier = dossier,
      strategyPack = strategyPack,
      routeRefs = routeRefs,
      moveRefs = moveRefs,
      strategyReasons = strategyReasons
    )

  private def maybeGenerateActiveStrategicNote(
      moment: GameNarrativeMoment,
      dossier: Option[ActiveBranchDossier],
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef],
      allowLlmPolish: Boolean,
      lang: String,
      asyncTier: Boolean,
      planTier: String,
      llmLevel: String
  ): Future[ActiveStrategicNoteDecision] =
    val normalizedPlanTier = normalizePlanTier(planTier)
    val normalizedLlmLevel = normalizeLlmLevel(llmLevel)
    val routeMeta = resolvedActiveNoteRouteMeta(asyncTier, normalizedPlanTier, normalizedLlmLevel)
    val canAttempt =
      isLlmPolishEnabledForRequest(allowLlmPolish) &&
        !providerConfig.isActiveNoteNone &&
        normalizedPlanTier == PlanTier.Pro &&
        normalizedLlmLevel == LlmLevel.Active

    if !canAttempt then
      Future.successful(
        ActiveStrategicNoteDecision(
          note = None,
          sourceMode = Some("omitted"),
          hardReasons = List("active_note_not_enabled"),
          provider = Some(routeMeta.provider),
          configuredModel = routeMeta.configuredModel,
          fallbackModel = routeMeta.fallbackModel,
          reasoningEffort = routeMeta.reasoningEffort
        )
      )
    else
      val phase = phaseFromPly(moment.ply)
      val momentType = Option(moment.momentType).map(_.trim).filter(_.nonEmpty).getOrElse("Strategic Moment")
      val concepts = moment.concepts.map(_.trim).filter(_.nonEmpty).distinct.take(8)
      val maxTokens = Some(if asyncTier then 560 else 420)
      val activePolishFamily = "active_note_polish"
      val activeRepairFamily = "active_note_repair"
      def observedModelsForOpenAi(results: OpenAiPolishResult*): List[String] =
        (routeMeta.configuredModel.toList ++ results.toList.flatMap(result => trimmedOption(result.model))).distinct

      providerConfig.activeNoteProviderResolved match
        case "openai" if openAiClient.isEnabled =>
          val op =
            if asyncTier then
              openAiClient.activeStrategicNoteAsync(
                baseNarrative = moment.narrative,
                phase = phase,
                momentType = momentType,
                concepts = concepts,
                fen = moment.fen,
                strategyPack = moment.strategyPack,
                dossier = dossier,
                routeRefs = routeRefs,
                moveRefs = moveRefs,
                lang = normalizeLang(lang),
                maxOutputTokens = maxTokens,
                planTier = normalizedPlanTier,
                llmLevel = normalizedLlmLevel
              )
            else
              openAiClient.activeStrategicNoteSync(
                baseNarrative = moment.narrative,
                phase = phase,
                momentType = momentType,
                concepts = concepts,
                fen = moment.fen,
                strategyPack = moment.strategyPack,
                dossier = dossier,
                routeRefs = routeRefs,
                moveRefs = moveRefs,
                lang = normalizeLang(lang),
                maxOutputTokens = maxTokens,
                planTier = normalizedPlanTier,
                llmLevel = normalizedLlmLevel
              )
          op.flatMap {
            case Some(primary) =>
              val primaryValidation = validateActiveStrategicNote(
                candidateText = primary.commentary,
                baseNarrative = moment.narrative,
                dossier = dossier,
                strategyPack = moment.strategyPack,
                routeRefs = routeRefs,
                moveRefs = moveRefs
              )
              if primaryValidation.isAccepted then
                Future.successful(
                  ActiveStrategicNoteDecision(
                    note = Some(primaryValidation.text),
                    sourceMode = Some("llm_polished"),
                    hardReasons = Nil,
                    warningReasons = primaryValidation.warningReasons,
                    provider = Some(routeMeta.provider),
                    configuredModel = routeMeta.configuredModel,
                    fallbackModel = routeMeta.fallbackModel,
                    reasoningEffort = routeMeta.reasoningEffort,
                    observedModels = observedModelsForOpenAi(primary),
                    primaryAccepted = true,
                    promptUsages = List(activePolishFamily -> primary)
                  )
                )
              else if !ActiveStrategicNoteValidator.shouldRepair(primaryValidation) then
                Future.successful(
                  ActiveStrategicNoteDecision(
                    note = Some(primaryValidation.text),
                    sourceMode = Some("llm_polished"),
                    hardReasons = Nil,
                    warningReasons = primaryValidation.warningReasons,
                    provider = Some(routeMeta.provider),
                    configuredModel = routeMeta.configuredModel,
                    fallbackModel = routeMeta.fallbackModel,
                    reasoningEffort = routeMeta.reasoningEffort,
                    observedModels = observedModelsForOpenAi(primary),
                    primaryAccepted = true,
                    promptUsages = List(activePolishFamily -> primary)
                  )
                )
              else
                val repairOp =
                  if asyncTier then
                    openAiClient.repairActiveStrategicNoteAsync(
                      baseNarrative = moment.narrative,
                      rejectedNote = primary.commentary,
                      failureReasons = primaryValidation.hardReasons,
                      phase = phase,
                      momentType = momentType,
                      concepts = concepts,
                      fen = moment.fen,
                      strategyPack = moment.strategyPack,
                      dossier = dossier,
                      routeRefs = routeRefs,
                      moveRefs = moveRefs,
                      lang = normalizeLang(lang),
                      maxOutputTokens = maxTokens,
                      planTier = normalizedPlanTier,
                      llmLevel = normalizedLlmLevel
                    )
                  else
                    openAiClient.repairActiveStrategicNoteSync(
                      baseNarrative = moment.narrative,
                      rejectedNote = primary.commentary,
                      failureReasons = primaryValidation.hardReasons,
                      phase = phase,
                      momentType = momentType,
                      concepts = concepts,
                      fen = moment.fen,
                      strategyPack = moment.strategyPack,
                      dossier = dossier,
                      routeRefs = routeRefs,
                      moveRefs = moveRefs,
                      lang = normalizeLang(lang),
                      maxOutputTokens = maxTokens,
                      planTier = normalizedPlanTier,
                      llmLevel = normalizedLlmLevel
                    )
                repairOp.map {
                  case Some(repaired) =>
                    val repairedValidation = validateActiveStrategicNote(
                      candidateText = repaired.commentary,
                      baseNarrative = moment.narrative,
                      dossier = dossier,
                      strategyPack = moment.strategyPack,
                      routeRefs = routeRefs,
                      moveRefs = moveRefs
                    )
                    if repairedValidation.isAccepted then
                      ActiveStrategicNoteDecision(
                        note = Some(repairedValidation.text),
                        sourceMode = Some("llm_polished"),
                        hardReasons = Nil,
                        warningReasons = repairedValidation.warningReasons,
                        provider = Some(routeMeta.provider),
                        configuredModel = routeMeta.configuredModel,
                        fallbackModel = routeMeta.fallbackModel,
                        reasoningEffort = routeMeta.reasoningEffort,
                        observedModels = observedModelsForOpenAi(primary, repaired),
                        repairAttempted = true,
                        repairRecovered = true,
                        promptUsages = List(activePolishFamily -> primary, activeRepairFamily -> repaired)
                      )
                    else
                      ActiveStrategicNoteDecision(
                        note = None,
                        sourceMode = Some("omitted"),
                        hardReasons = repairedValidation.hardReasons.distinct,
                        warningReasons = repairedValidation.warningReasons,
                        provider = Some(routeMeta.provider),
                        configuredModel = routeMeta.configuredModel,
                        fallbackModel = routeMeta.fallbackModel,
                        reasoningEffort = routeMeta.reasoningEffort,
                        observedModels = observedModelsForOpenAi(primary, repaired),
                        repairAttempted = true,
                        promptUsages = List(activePolishFamily -> primary, activeRepairFamily -> repaired)
                      )
                  case None =>
                    ActiveStrategicNoteDecision(
                      note = None,
                      sourceMode = Some("omitted"),
                      hardReasons = (primaryValidation.hardReasons :+ "active_note_repair_empty").distinct,
                      warningReasons = primaryValidation.warningReasons,
                      provider = Some(routeMeta.provider),
                      configuredModel = routeMeta.configuredModel,
                      fallbackModel = routeMeta.fallbackModel,
                      reasoningEffort = routeMeta.reasoningEffort,
                      observedModels = observedModelsForOpenAi(primary),
                      repairAttempted = true,
                      promptUsages = List(activePolishFamily -> primary)
                    )
                }
            case None =>
              Future.successful(
                ActiveStrategicNoteDecision(
                  note = None,
                  sourceMode = Some("omitted"),
                  hardReasons = List("empty_polish"),
                  provider = Some(routeMeta.provider),
                  configuredModel = routeMeta.configuredModel,
                  fallbackModel = routeMeta.fallbackModel,
                  reasoningEffort = routeMeta.reasoningEffort
                )
              )
          }
        case "gemini" if geminiClient.isEnabled =>
          geminiClient
            .activeStrategicNote(
              baseNarrative = moment.narrative,
              phase = phase,
              momentType = momentType,
              concepts = concepts,
              fen = moment.fen,
              strategyPack = moment.strategyPack,
              dossier = dossier,
              routeRefs = routeRefs,
              moveRefs = moveRefs
            )
            .flatMap {
              case Some(primary) =>
                val primaryValidation = validateActiveStrategicNote(
                  candidateText = primary,
                  baseNarrative = moment.narrative,
                  dossier = dossier,
                  strategyPack = moment.strategyPack,
                  routeRefs = routeRefs,
                  moveRefs = moveRefs
                )
                if primaryValidation.isAccepted then
                  Future.successful(
                    ActiveStrategicNoteDecision(
                      note = Some(primaryValidation.text),
                      sourceMode = Some("llm_polished"),
                      hardReasons = Nil,
                      warningReasons = primaryValidation.warningReasons,
                      provider = Some(routeMeta.provider),
                      configuredModel = routeMeta.configuredModel,
                      fallbackModel = routeMeta.fallbackModel,
                      reasoningEffort = routeMeta.reasoningEffort,
                      observedModels = routeMeta.configuredModel.toList,
                      primaryAccepted = true
                    )
                  )
                else if !ActiveStrategicNoteValidator.shouldRepair(primaryValidation) then
                  Future.successful(
                    ActiveStrategicNoteDecision(
                      note = Some(primaryValidation.text),
                      sourceMode = Some("llm_polished"),
                      hardReasons = Nil,
                      warningReasons = primaryValidation.warningReasons,
                      provider = Some(routeMeta.provider),
                      configuredModel = routeMeta.configuredModel,
                      fallbackModel = routeMeta.fallbackModel,
                      reasoningEffort = routeMeta.reasoningEffort,
                      observedModels = routeMeta.configuredModel.toList,
                      primaryAccepted = true
                    )
                  )
                else
                  geminiClient
                    .repairActiveStrategicNote(
                      baseNarrative = moment.narrative,
                      rejectedNote = primary,
                      failureReasons = primaryValidation.hardReasons,
                      phase = phase,
                      momentType = momentType,
                      concepts = concepts,
                      fen = moment.fen,
                      strategyPack = moment.strategyPack,
                      dossier = dossier,
                      routeRefs = routeRefs,
                      moveRefs = moveRefs
                    )
                    .map {
                      case Some(repaired) =>
                        val repairedValidation = validateActiveStrategicNote(
                          candidateText = repaired,
                          baseNarrative = moment.narrative,
                          dossier = dossier,
                          strategyPack = moment.strategyPack,
                          routeRefs = routeRefs,
                          moveRefs = moveRefs
                        )
                        if repairedValidation.isAccepted then
                          ActiveStrategicNoteDecision(
                            note = Some(repairedValidation.text),
                            sourceMode = Some("llm_polished"),
                            hardReasons = Nil,
                            warningReasons = repairedValidation.warningReasons,
                            provider = Some(routeMeta.provider),
                            configuredModel = routeMeta.configuredModel,
                            fallbackModel = routeMeta.fallbackModel,
                            reasoningEffort = routeMeta.reasoningEffort,
                            observedModels = routeMeta.configuredModel.toList,
                            repairAttempted = true,
                            repairRecovered = true
                          )
                        else
                          ActiveStrategicNoteDecision(
                            note = None,
                            sourceMode = Some("omitted"),
                            hardReasons = repairedValidation.hardReasons.distinct,
                            warningReasons = repairedValidation.warningReasons,
                            provider = Some(routeMeta.provider),
                            configuredModel = routeMeta.configuredModel,
                            fallbackModel = routeMeta.fallbackModel,
                            reasoningEffort = routeMeta.reasoningEffort,
                            observedModels = routeMeta.configuredModel.toList,
                            repairAttempted = true
                          )
                      case None =>
                        ActiveStrategicNoteDecision(
                          note = None,
                          sourceMode = Some("omitted"),
                          hardReasons = (primaryValidation.hardReasons :+ "active_note_repair_empty").distinct,
                          warningReasons = primaryValidation.warningReasons,
                          provider = Some(routeMeta.provider),
                          configuredModel = routeMeta.configuredModel,
                          fallbackModel = routeMeta.fallbackModel,
                          reasoningEffort = routeMeta.reasoningEffort,
                          observedModels = routeMeta.configuredModel.toList,
                          repairAttempted = true
                        )
                    }
              case None =>
                Future.successful(
                  ActiveStrategicNoteDecision(
                    note = None,
                    sourceMode = Some("omitted"),
                    hardReasons = List("empty_polish"),
                    provider = Some(routeMeta.provider),
                    configuredModel = routeMeta.configuredModel,
                    fallbackModel = routeMeta.fallbackModel,
                    reasoningEffort = routeMeta.reasoningEffort
                  )
                )
            }
        case _ =>
          Future.successful(
            ActiveStrategicNoteDecision(
              note = None,
              sourceMode = Some("omitted"),
              hardReasons = List("provider_unavailable"),
              provider = Some(routeMeta.provider),
              configuredModel = routeMeta.configuredModel,
              fallbackModel = routeMeta.fallbackModel,
              reasoningEffort = routeMeta.reasoningEffort
            )
          )

  private def buildPolishMetaOpenAi(
      model: Option[String],
      sourceMode: String,
      phase: String,
      validationReasons: List[String],
      attempts: List[(String, OpenAiPolishResult)],
      strategyCoverage: Option[StrategyCoverageMetaV1]
  ): PolishMetaV1 =
    val promptAttempts = attempts.map(_._2)
    PolishMetaV1(
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
      strategyCoverage: Option[StrategyCoverageMetaV1]
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
      salience: Option[lila.llm.model.strategic.StrategicSalience],
      momentType: Option[String],
      lang: String,
      allowedSans: List[String],
      asyncTier: Boolean,
      strategyPack: Option[StrategyPack],
      planTier: String,
      llmLevel: String
  ): Future[Option[PolishDecision]] =
    val segmentation = PolishSegmenter.segment(prose)
    val editable = segmentation.editableSegments
    if editable.size < 2 then Future.successful(None)
    else
      val segmentCap = adaptiveOutputTokenCap(prose, refs = None, asyncTier = asyncTier).min(if asyncTier then 520 else 420).max(192)
      val maxSegments = if asyncTier then 4 else 2
      val segmentPolishFamily = "segment_polish"
      val segmentRepairFamily = "segment_repair"
      val candidateEditable =
        editable
          .sortBy(s => -wordCount(s.text))
          .take(maxSegments)
      if candidateEditable.size < 2 then Future.successful(None)
      else
        final case class SegmentRewrite(
            id: String,
            commentary: String,
            attempts: List[(String, OpenAiPolishResult)]
        )

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
                salience = salience,
                momentType = momentType,
                lang = lang,
                maxOutputTokens = Some(segmentCap),
                planTier = planTier,
                llmLevel = llmLevel
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
                llmLevel = llmLevel
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
                llmLevel = llmLevel
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
                llmLevel = llmLevel
              )

          def acceptedRewrite(
              attempt: OpenAiPolishResult,
              attempts: List[(String, OpenAiPolishResult)]
          ): Option[SegmentRewrite] =
            validateSegmentCandidate(attempt, seg.text, masked)
              .map(rewritten => SegmentRewrite(seg.id, rewritten, attempts))

          def fallbackToOriginal(attempts: List[(String, OpenAiPolishResult)]): Option[SegmentRewrite] =
            Some(SegmentRewrite(seg.id, seg.text, attempts))

          callPolish(modelInput).flatMap {
            case Some(primary) =>
              val primaryAttempts = List(segmentPolishFamily -> primary)
              acceptedRewrite(primary, primaryAttempts) match
                case Some(rewritten) =>
                  Future.successful(Some(rewritten))
                case None =>
                  callRepair(modelInput, primary.commentary).map {
                    case Some(repaired) =>
                      val attempts = primaryAttempts :+ (segmentRepairFamily -> repaired)
                      acceptedRewrite(repaired, attempts)
                        .orElse(fallbackToOriginal(attempts))
                    case None =>
                      // Prefer structural safety over aggressive rewrite: keep original segment.
                      fallbackToOriginal(primaryAttempts)
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
            val strategyValidation = evaluateStrategyCoverage(merged, strategyPack, planTier, llmLevel)
            val mergedReasons = (mergedValidation.reasons ++ strategyValidation.reasons).distinct
            val attempts = successes.flatMap(_.attempts).toList
            if mergedReasons.nonEmpty then
              recordPromptUsageAttempts(attempts)
              None
            else
              Some(
                PolishDecision(
                  commentary = merged,
                  sourceMode = "llm_polished",
                  model = attempts.lastOption.map(_._2.model),
                  cacheHit = attempts.exists(_._2.promptCacheHit),
                  meta = Some(
                    buildPolishMetaOpenAi(
                      model = attempts.lastOption.map(_._2.model),
                      sourceMode = "llm_polished",
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
      salience: Option[lila.llm.model.strategic.StrategicSalience],
      allowLlmPolish: Boolean,
      lang: String,
      allowedSans: List[String],
      asyncTier: Boolean,
      refs: Option[BookmakerRefsV1],
      planTier: String,
      llmLevel: String,
      momentType: Option[String] = None,
      bookmakerSlots: Option[BookmakerPolishSlots] = None,
      disablePolishCircuit: Boolean = false
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
    else if !disablePolishCircuit && isPolishCircuitOpen(System.currentTimeMillis()) then
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
              estimatedCostUsd = None,
              strategyCoverage = evaluateStrategyCoverage(prose, strategyPack, planTier, llmLevel).meta
            )
            )
          )
      )
    else
      val normalizedPlanTier = normalizePlanTier(planTier)
      val normalizedLlmLevel = normalizeLlmLevel(llmLevel)
      val baselineCoverage =
        evaluateStrategyCoverage(prose, strategyPack, normalizedPlanTier, normalizedLlmLevel).meta
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
              bookmakerSlots = bookmakerSlots
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
                llmLevel = normalizedLlmLevel
              )
            else Future.successful(None)

          segmentAttemptFut.flatMap {
            case Some(segmentDecision) =>
              Future.successful(withRecordedPolishMetrics(requestStartNs, segmentDecision))
            case None =>
              val anchorSeed = if bookmakerSlots.isDefined then validationSeed else prose
              val anchors = MoveAnchorCodec.encode(anchorSeed, refs)
              val slotsForModel =
                bookmakerSlots.map(slots => slots.withFactGuardrails(anchors.anchoredText.split('\n').toList))
              val proseForModel = if bookmakerSlots.isDefined then prose else if anchors.hasAnchors then anchors.anchoredText else prose
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
                    llmLevel = normalizedLlmLevel,
                    bookmakerSlots = slotsForModel
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
                    llmLevel = normalizedLlmLevel,
                    bookmakerSlots = slotsForModel
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
                    llmLevel = normalizedLlmLevel,
                    bookmakerSlots = bookmakerSlots
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
                          llmLevel = normalizedLlmLevel,
                          bookmakerSlots = slotsForModel
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
                          llmLevel = normalizedLlmLevel,
                          bookmakerSlots = slotsForModel
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
                          llmLevel = normalizedLlmLevel,
                          bookmakerSlots = bookmakerSlots
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
          val anchorSeed = if bookmakerSlots.isDefined then validationSeed else prose
          val anchors = MoveAnchorCodec.encode(anchorSeed, refs)
          val slotsForModel =
            bookmakerSlots.map(slots => slots.withFactGuardrails(anchors.anchoredText.split('\n').toList))
          val proseForModel = if bookmakerSlots.isDefined then prose else if anchors.hasAnchors then anchors.anchoredText else prose
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
              bookmakerSlots = slotsForModel
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
                  llmLevel = normalizedLlmLevel,
                  bookmakerSlots = bookmakerSlots
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
                      bookmakerSlots = slotsForModel
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
                          llmLevel = normalizedLlmLevel,
                          bookmakerSlots = bookmakerSlots
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
                            commentary = prose,
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
                      commentary = prose,
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

  private def selectFullGamePolishTargetPlies(
      moments: List[GameNarrativeMoment]
  ): Set[Int] =
    val strategicRepresentatives = moments.filter(_.strategicThread.isDefined)
    val tacticalPivots =
      moments.filter(moment =>
        !moment.strategicThread.isDefined &&
          ((moment.momentType == "AdvantageSwing" &&
            moment.moveClassification.exists(label =>
              label.equalsIgnoreCase("Blunder") || label.equalsIgnoreCase("MissedWin")
            )) ||
            moment.momentType == "MatePivot")
      )
    val openingBranches =
      moments.filter(moment =>
        !moment.strategicThread.isDefined &&
          Set("OpeningBranchPoint", "OpeningOutOfBook", "OpeningTheoryEnds", "OpeningNovelty").contains(moment.momentType)
      )
    (strategicRepresentatives ++ tacticalPivots ++ openingBranches)
      .distinctBy(_.ply)
      .take(10)
      .map(_.ply)
      .toSet

  private def maybePolishGameNarrative(
      response: GameNarrativeResponse,
      allowLlmPolish: Boolean,
      lang: String,
      asyncTier: Boolean,
      planTier: String,
      llmLevel: String,
      disablePolishCircuit: Boolean
  ): Future[GameNarrativeResponse] =
    if !isLlmPolishEnabledForRequest(allowLlmPolish) || providerConfig.isNone then
      Future.successful(response.copy(sourceMode = "rule", model = None))
    else
      val normalizedPlanTier = normalizePlanTier(planTier)
      val normalizedLlmLevel = normalizeLlmLevel(llmLevel)
      val basePolishLevel = LlmLevel.Polish
      val polishTargetPlies = selectFullGamePolishTargetPlies(response.moments)

      val basePolishFut =
        for
          introPolish <- maybePolishCommentary(
            prose = response.intro,
            validationSeed = response.intro,
            phase = "opening",
            evalDelta = None,
            concepts = response.themes,
            strategyHints = Nil,
            strategyPack = None,
            fen = "",
            openingName = None,
            salience = None,
            allowLlmPolish = allowLlmPolish,
            lang = lang,
            allowedSans = Nil,
            asyncTier = asyncTier,
            refs = None,
            planTier = normalizedPlanTier,
            llmLevel = basePolishLevel,
            momentType = Some("Game Intro"),
            disablePolishCircuit = disablePolishCircuit
          )
          conclusionPolish <- maybePolishCommentary(
            prose = response.conclusion,
            validationSeed = response.conclusion,
            phase = "endgame",
            evalDelta = None,
            concepts = response.themes,
            strategyHints = Nil,
            strategyPack = None,
            fen = "",
            openingName = None,
            salience = None,
            allowLlmPolish = allowLlmPolish,
            lang = lang,
            allowedSans = Nil,
            asyncTier = asyncTier,
            refs = None,
            planTier = normalizedPlanTier,
            llmLevel = basePolishLevel,
            momentType = Some("Game Conclusion"),
            disablePolishCircuit = disablePolishCircuit
          )
          polishedMoments <- Future.traverse(response.moments) { moment =>
            if polishTargetPlies.contains(moment.ply) then
              val allowedSans = moment.variations.flatMap(v => NarrativeUtils.uciListToSan(moment.fen, v.moves))
              maybePolishCommentary(
                prose = moment.narrative,
                validationSeed = moment.narrative,
                phase = phaseFromPly(moment.ply),
                evalDelta = None,
                concepts = moment.concepts,
                strategyHints = Nil,
                strategyPack = moment.strategyPack,
                fen = moment.fen,
                openingName = None,
                salience = None,
                allowLlmPolish = allowLlmPolish,
                lang = lang,
                allowedSans = allowedSans,
                asyncTier = asyncTier,
                refs = None,
                planTier = normalizedPlanTier,
                llmLevel = basePolishLevel,
                momentType = Some(moment.momentType),
                disablePolishCircuit = disablePolishCircuit
              ).map { decision =>
                (moment.copy(narrative = decision.commentary), decision.sourceMode, decision.model, decision.cacheHit)
              }
            else
              Future.successful((moment, "rule", None, false))
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

      val shouldRunActiveNotes =
        allowLlmPolish &&
          normalizedPlanTier == PlanTier.Pro &&
          normalizedLlmLevel == LlmLevel.Active

      basePolishFut.flatMap { basePolished =>
        val finalResponseFut =
          if shouldRunActiveNotes then
            attachActiveStrategicNotes(
              response = basePolished,
              allowLlmPolish = allowLlmPolish,
              lang = lang,
              asyncTier = asyncTier,
              planTier = normalizedPlanTier,
              llmLevel = normalizedLlmLevel
            )
          else Future.successful(basePolished)

        finalResponseFut.map { finalResponse =>
          finalResponse.moments.foreach { moment =>
            recordComparisonObservation(
              path = "fullgame",
              digest = moment.signalDigest.flatMap(_.decisionComparison),
              whyAbsent = moment.whyAbsentFromTopMultiPV,
              authorEvidence = moment.authorEvidence,
              probeRequests = moment.probeRequests,
              strategyPack = moment.strategyPack
            )
            moment.activeStrategicNote.foreach(note => recordActiveThesisAgreement(moment, note))
          }
          finalResponse
        }
      }

  private def attachActiveStrategicNotes(
      response: GameNarrativeResponse,
      allowLlmPolish: Boolean,
      lang: String,
      asyncTier: Boolean,
      planTier: String,
      llmLevel: String
  ): Future[GameNarrativeResponse] =
    val selectedByPly = response.moments.filter(_.strategicBranch).map(_.ply).toSet
    val threadsById = response.strategicThreads.map(thread => thread.threadId -> thread).toMap
    activeNoteSelectedCount.addAndGet(selectedByPly.size.toLong)

    Future.traverse(response.moments) { moment =>
      val threadRef = moment.strategicThread
      val thread = threadRef.flatMap(ref => threadsById.get(ref.threadId))
      if selectedByPly.contains(moment.ply) then
        val ideaRefs = buildActiveStrategicIdeaRefs(moment)
        val routeRefs = buildActiveStrategicRouteRefs(moment)
        val moveRefs = buildActiveStrategicMoveRefs(moment)
        val directionalTargets = buildActiveDirectionalTargets(moment, ideaRefs)
        val dossier = ActiveBranchDossierBuilder.build(moment, routeRefs, moveRefs, threadRef, thread)
        maybeGenerateActiveStrategicNote(
          moment = moment,
          dossier = dossier,
          routeRefs = routeRefs,
          moveRefs = moveRefs,
          allowLlmPolish = allowLlmPolish,
          lang = lang,
          asyncTier = asyncTier,
          planTier = planTier,
          llmLevel = llmLevel
        ).map { decision =>
          val isAttached = decision.note.exists(_.trim.nonEmpty)
          recordPromptUsageAttempts(decision.promptUsages)
          recordActiveNoteOutcome(
            attached = isAttached,
            hardReasons = decision.hardReasons,
            warningReasons = decision.warningReasons,
            primaryAccepted = decision.primaryAccepted,
            repairAttempted = decision.repairAttempted,
            repairRecovered = decision.repairRecovered,
            provider = decision.provider,
            configuredModel = decision.configuredModel,
            fallbackModel = decision.fallbackModel,
            reasoningEffort = decision.reasoningEffort,
            observedModels = decision.observedModels
          )
          recordActiveDossierOutcome(
            dossier = dossier,
            note = decision.note,
            hardReasons = decision.hardReasons,
            warningReasons = decision.warningReasons,
            routeRefs = routeRefs,
            moveRefs = moveRefs
          )
          moment.copy(
            strategicBranch = true,
            activeStrategicNote = decision.note,
            activeStrategicSourceMode = decision.sourceMode.orElse(Some("omitted")),
            activeStrategicIdeas = ideaRefs,
            activeStrategicRoutes = routeRefs,
            activeStrategicMoves = moveRefs,
            activeDirectionalTargets = directionalTargets,
            activeBranchDossier = dossier,
            strategicThread = threadRef
          )
        }
      else
        Future.successful(
          moment.copy(
            strategicBranch = false,
            activeStrategicNote = None,
            activeStrategicSourceMode = None,
            activeStrategicIdeas = Nil,
            activeStrategicRoutes = Nil,
            activeStrategicMoves = Nil,
            activeDirectionalTargets = Nil,
            activeBranchDossier = None,
            strategicThread = threadRef
          )
        )
    }.map(polishedMoments => response.copy(moments = polishedMoments))

  private def buildActiveStrategicIdeaRefs(moment: GameNarrativeMoment): List[ActiveStrategicIdeaRef] =
    moment.strategyPack.toList
      .flatMap(_.strategicIdeas)
      .sortBy(idea => (-idea.confidence, idea.kind))
      .take(2)
      .map { idea =>
        ActiveStrategicIdeaRef(
          ideaId = idea.ideaId,
          ownerSide = idea.ownerSide,
          kind = idea.kind,
          group = idea.group,
          readiness = idea.readiness,
          focusSummary = StrategicIdeaSelector.focusSummary(idea),
          confidence = idea.confidence
        )
      }

  private def buildActiveStrategicRouteRefs(moment: GameNarrativeMoment): List[ActiveStrategicRouteRef] =
    val routeRegex = "^[a-h][1-8]$".r
    val pack = moment.strategyPack.toList
    val routes = pack.flatMap(_.pieceRoutes)
    activeRouteRedeployCount.addAndGet(routes.size.toLong)
    activeRouteMoveRefCount.addAndGet(pack.flatMap(_.pieceMoveRefs).size.toLong)
    activeRouteHiddenSafetyCount.addAndGet(routes.count(_.surfaceMode == RouteSurfaceMode.Hidden).toLong)
    activeRouteTowardOnlyCount.addAndGet(routes.count(_.surfaceMode == RouteSurfaceMode.Toward).toLong)
    activeRouteExactSurfaceCount.addAndGet(routes.count(_.surfaceMode == RouteSurfaceMode.Exact).toLong)
    activeRouteOpponentHiddenCount.addAndGet(
      routes.count(route => route.surfaceMode != RouteSurfaceMode.Hidden && route.ownerSide != moment.side).toLong
    )
    updateCommentaryOpsGauges()
    routes
      .zipWithIndex
      .flatMap { case (route, idx) =>
        val squares =
          route.route
            .map(_.trim.toLowerCase)
            .filter(s => routeRegex.matches(s))
            .distinct
        Option.when(squares.size >= 2 && route.surfaceMode != RouteSurfaceMode.Hidden)(
          ActiveStrategicRouteRef(
            routeId = s"route_${idx + 1}",
            ownerSide = route.ownerSide,
            piece = route.piece,
            route = squares,
            purpose = route.purpose,
            strategicFit = route.strategicFit,
            tacticalSafety = route.tacticalSafety,
            surfaceConfidence = route.surfaceConfidence,
            surfaceMode = route.surfaceMode
          )
        )
      }
      .take(3)

  private def buildActiveDirectionalTargets(
      moment: GameNarrativeMoment,
      ideaRefs: List[ActiveStrategicIdeaRef]
  ): List[StrategyDirectionalTarget] =
    val preferredSide = ideaRefs.headOption.map(_.ownerSide).getOrElse(moment.side)
    moment.strategyPack.toList
      .flatMap(_.directionalTargets)
      .filter(_.ownerSide == preferredSide)
      .sortBy(target => (target.readiness, target.targetSquare))
      .take(2)

  private def buildActiveStrategicMoveRefs(moment: GameNarrativeMoment): List[ActiveStrategicMoveRef] =
    val fromEngine =
      moment.topEngineMove.toList
        .map(_.uci.trim.toLowerCase)
        .filter(_.matches("^[a-h][1-8][a-h][1-8][qrbn]?$"))
        .take(1)
        .map { uci =>
          ActiveStrategicMoveRef(
            label = "Engine preference",
            source = "top_engine_move",
            uci = uci,
            san = NarrativeUtils.uciToSan(moment.fen, uci),
            fenAfter = Some(NarrativeUtils.uciListToFen(moment.fen, List(uci)))
          )
        }

    val fromVariation =
      moment.variations.headOption
        .flatMap(_.moves.headOption)
        .map(_.trim.toLowerCase)
        .filter(_.matches("^[a-h][1-8][a-h][1-8][qrbn]?$"))
        .toList
        .map { uci =>
          ActiveStrategicMoveRef(
            label = "Principal line",
            source = "top_variation",
            uci = uci,
            san = NarrativeUtils.uciToSan(moment.fen, uci),
            fenAfter = Some(NarrativeUtils.uciListToFen(moment.fen, List(uci)))
          )
        }

    (fromEngine ++ fromVariation).groupBy(_.uci).values.map(_.head).toList.take(2)

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
      prevEndgameStateToken: Option[lila.llm.model.strategic.EndgamePatternState] = None,
      allowLlmPolish: Boolean = false,
      lang: String = "en",
      planTier: String = PlanTier.Basic,
      llmLevel: String = LlmLevel.Polish
  ): Future[Option[BookmakerResult]] =
    val requestStartNs = System.nanoTime()
    val normalizedPlanTier = normalizePlanTier(planTier)
    val effectiveLevel = effectiveLlmLevel(normalizedPlanTier, llmLevel, allowLlmPolish)
    val resolvedPly = NarrativeUtils.resolveAnnotationPly(fen, lastMove, ply)
    val incomingProbes = probeResults.getOrElse(Nil)
    val cacheCtx =
      llmCacheContext(
        allowLlmPolish = allowLlmPolish,
        lang = lang,
        asyncTier = false,
        planTier = normalizedPlanTier,
        llmLevel = effectiveLevel
      )
    bookmakerRequests.incrementAndGet()
    if prevStateToken.isDefined || prevEndgameStateToken.isDefined then tokenPresentCount.incrementAndGet()
    commentaryCache.get(fen, lastMove, incomingProbes, prevStateToken, prevEndgameStateToken, cacheCtx) match
      case Some(cached) =>
        stateAwareCacheHitCount.incrementAndGet()
        logger.debug(s"Cache hit: ${fen.take(20)}...")
        traceBookmakerFallbackResponse(
          response = cached,
          phase = phase,
          ply = resolvedPly,
          lastMove = lastMove,
          cacheHit = true,
          openingRefPresent = openingData.isDefined,
          incomingProbeCount = incomingProbes.size
        )
        recordLatencyMetrics(totalLatencyMs = elapsedMs(requestStartNs), structureEvalLatencyMs = None)
        maybeLogBookmakerMetrics()
        Future.successful(Some(BookmakerResult(response = cached, cacheHit = true)))
      case None =>
        stateAwareCacheMissCount.incrementAndGet()
        computeBookmakerResponse(
          fen, lastMove, eval, variations, probeResults, openingData,
          afterFen, afterEval, afterVariations, opening, phase, ply, prevStateToken, prevEndgameStateToken, allowLlmPolish, lang, cacheCtx, normalizedPlanTier, effectiveLevel
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
      prevEndgameStateToken: Option[lila.llm.model.strategic.EndgamePatternState],
      allowLlmPolish: Boolean,
      lang: String,
      cacheCtx: Option[LlmCacheContext],
      planTier: String,
      llmLevel: String
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
              lila.llm.model.strategic.EndgamePatternState.evolve(
                prevEndgameStateToken,
                data.endgameFeatures,
                effectivePly
              )
            val dataWithContinuity = data.copy(planContinuity = nextTracker.getContinuity(movingColor))
            if dataWithContinuity.planContinuity.exists(_.consecutivePlies >= 2) then
              continuityAppliedCount.incrementAndGet()
            dataWithContinuity.planSequence.foreach(ps => incTransition(ps.transitionType.toString))
            if llmConfig.shouldEvaluateStructureKb then recordStructureMetrics(dataWithContinuity)
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

            val ctx = NarrativeContextBuilder.build(
              data = dataWithContinuity,
              ctx = dataWithContinuity.toContext,
              probeResults = probeResults.getOrElse(Nil),
              openingRef = openingRef,
              afterAnalysis = afterDataOpt,
              renderMode = lila.llm.model.NarrativeRenderMode.Bookmaker
            )
            recordStrategicMetrics(
              data = dataWithContinuity,
              ctx = ctx,
              probeResults = probeResults.getOrElse(Nil)
            )
            val outline = BookStyleRenderer.validatedOutline(ctx)
            val proseRaw = BookStyleRenderer.renderValidatedOutline(outline, ctx)
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
            val strategyPack = StrategyPackBuilder.build(dataWithContinuity, ctx)
            val bookmakerLedger = BookmakerStrategicLedgerBuilder.build(
              ctx = ctx,
              strategyPack = strategyPack,
              refs = refs,
              probeResults = probeResults.getOrElse(Nil),
              planStateToken = prevStateToken,
              endgameStateToken = prevEndgameStateToken
            )
            val bookmakerSlots = BookmakerPolishSlotsBuilder.build(ctx, outline, refs)
            val authorQuestions = AuthoringEvidenceSummaryBuilder.summarizeQuestions(ctx)
            val authorEvidence = AuthoringEvidenceSummaryBuilder.summarizeEvidence(ctx)
            val strategyPromptHints = strategyHints(strategyPack)
            val validationSeed = bookmakerSlots.map(_.validationSeedText).filter(_.nonEmpty).getOrElse(prose)

            maybePolishCommentary(
              prose = prose,
              validationSeed = validationSeed,
              phase = phase,
              evalDelta = None,
              concepts = baseConcepts,
              strategyHints = strategyPromptHints,
              strategyPack = strategyPack,
              fen = fen,
              openingName = opening,
              salience = Some(dataWithContinuity.strategicSalience),
              allowLlmPolish = allowLlmPolish,
              lang = lang,
              allowedSans = allowedSans,
              asyncTier = false,
              refs = refs,
              planTier = planTier,
              llmLevel = llmLevel,
              bookmakerSlots = bookmakerSlots
            ).map { decision =>
              val response = CommentResponse(
                commentary = decision.commentary,
                concepts = baseConcepts,
                variations = dataWithContinuity.alternatives,
                probeRequests = if probeResults.exists(_.nonEmpty) then Nil else ctx.probeRequests,
                authorQuestions = authorQuestions,
                authorEvidence = authorEvidence,
                mainStrategicPlans = ctx.mainStrategicPlans,
                latentPlans = ctx.latentPlans,
                whyAbsentFromTopMultiPV = ctx.whyAbsentFromTopMultiPV,
                planStateToken = Some(nextTracker),
                endgameStateToken = nextEndgameState,
                sourceMode = decision.sourceMode,
                model = decision.model,
                refs = refs,
                polishMeta = decision.meta,
                planTier = planTier,
                llmLevel = llmLevel,
                strategyPack = strategyPack,
                signalDigest = strategyPack.flatMap(_.signalDigest),
                bookmakerLedger = bookmakerLedger
              )
              recordComparisonObservation(
                path = "bookmaker",
                digest = response.signalDigest.flatMap(_.decisionComparison),
                whyAbsent = response.whyAbsentFromTopMultiPV,
                authorEvidence = response.authorEvidence,
                probeRequests = response.probeRequests,
                strategyPack = response.strategyPack
              )
              traceBookmakerFallbackResponse(
                response = response,
                phase = phase,
                ply = effectivePly,
                lastMove = lastMove,
                cacheHit = decision.cacheHit,
                openingRefPresent = openingRef.isDefined,
                incomingProbeCount = probeResults.getOrElse(Nil).size
              )
              if response.planStateToken.isDefined then tokenEmitCount.incrementAndGet()
              commentaryCache.put(
                fen = fen,
                lastMove = lastMove,
                response = response,
                probeResults = probeResults.getOrElse(Nil),
                planStateToken = prevStateToken,
                endgameStateToken = prevEndgameStateToken,
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
      lang: String = "en",
      planTier: String = PlanTier.Basic,
      llmLevel: String = LlmLevel.Polish,
      disablePolishCircuit: Boolean = false
  ): Future[Option[GameNarrativeResponse]] =
    val normalizedPlanTier = normalizePlanTier(planTier)
    val styleHint = Option(style).map(_.trim.toLowerCase).getOrElse("book")
    val focusHints = focusOn.map(_.trim.toLowerCase)
    val requestedLevel =
      if focusHints.exists(f => f.contains("long_plan") || f.contains("piece_route") || f.contains("strategy")) ||
          styleHint.contains("active") ||
          styleHint.contains("coach")
      then
        LlmLevel.Active
      else normalizeLlmLevel(llmLevel)
    val effectiveLevel = effectiveLlmLevel(normalizedPlanTier, requestedLevel, allowLlmPolish)
    val evalMap = evals.map(e => e.ply -> e.getVariations).toMap
    fetchOpeningRefsForPgn(pgn)
      .map { openingRefsByFen =>
        CommentaryEngine.generateFullGameNarrative(
          pgn = pgn,
          evals = evalMap,
          openingRefsByFen = openingRefsByFen,
          llmLevel = effectiveLevel
        )
      }
      .recover { case NonFatal(e) =>
        logger.warn(s"Opening reference fetch failed for full game analysis: ${e.getMessage}")
        CommentaryEngine.generateFullGameNarrative(
          pgn = pgn,
          evals = evalMap,
          llmLevel = effectiveLevel
        )
      }
      .flatMap { narrative =>
        val base = GameNarrativeResponse.fromNarrative(
          narrative = narrative,
          review = None,
          sourceMode = "rule",
          model = None,
          planTier = normalizedPlanTier,
          llmLevel = effectiveLevel
        )
        maybePolishGameNarrative(
          response = base,
          allowLlmPolish = allowLlmPolish,
          lang = normalizeLang(lang),
          asyncTier = asyncTier,
          planTier = normalizedPlanTier,
          llmLevel = effectiveLevel,
          disablePolishCircuit = disablePolishCircuit
        ).map { finalResponse =>
          finalResponse.copy(
            review = Some(
              buildGameReview(
                response = finalResponse,
                pgn = pgn,
                evals = evals,
                internalMomentCount = narrative.internalMomentCount
              )
            )
          ).some
        }
      }

  def submitGameAnalysisAsync(
      req: FullAnalysisRequest,
      allowLlmPolish: Boolean,
      lang: String,
      planTier: String = PlanTier.Basic,
      llmLevel: String = LlmLevel.Polish
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
      lang = lang,
      planTier = planTier,
      llmLevel = llmLevel
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

  /** Stash CCA results from a completed game analysis for the Defeat DNA aggregation. */
  def stashCcaResults(userId: String, response: GameNarrativeResponse): Funit =
    ccaHistoryRepo.fold(funit) { repo =>
      val newCollapses = response.moments.flatMap(_.collapse)
      if newCollapses.nonEmpty then repo.insert(userId, newCollapses)
      else funit
    }

  /** Retrieve accumulated CCA history for a user. */
  def getCcaHistory(userId: String): Fu[List[lila.llm.model.CollapseAnalysis]] =
    ccaHistoryRepo.fold(fuccess(Nil))(_.recent(userId))

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
      response: GameNarrativeResponse,
      pgn: String,
      evals: List[MoveEval],
      internalMomentCount: Int
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
    val selectedMomentPlies = response.moments.map(_.ply).filter(_ > 0).distinct.sorted
    val visibleMomentCount = selectedMomentPlies.size
    val polishedMomentCount = selectFullGamePolishTargetPlies(response.moments).size

    GameNarrativeReview(
      schemaVersion = 5,
      reviewPerspective = "both",
      totalPlies = inferredTotalPlies,
      evalCoveredPlies = evalCoveredPlies,
      evalCoveragePct = evalCoveragePct.max(0).min(100),
      selectedMoments = visibleMomentCount,
      selectedMomentPlies = selectedMomentPlies,
      internalMomentCount = internalMomentCount.max(visibleMomentCount),
      visibleMomentCount = visibleMomentCount,
      polishedMomentCount = polishedMomentCount,
      visibleStrategicMomentCount = response.moments.count(_.strategicThread.isDefined),
      visibleBridgeMomentCount = response.moments.count(_.selectionKind == "thread_bridge"),
      blundersCount = response.moments.count(m => m.momentType == "AdvantageSwing" && m.moveClassification.contains("Blunder")),
      missedWinsCount = response.moments.count(m => m.momentType == "AdvantageSwing" && m.moveClassification.contains("MissedWin")),
      brilliantMovesCount = 0,
      accuracyWhite = None,
      accuracyBlack = None,
      momentTypeCounts = response.moments.groupBy(_.momentType).map { case (k, v) => k -> v.size }
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
