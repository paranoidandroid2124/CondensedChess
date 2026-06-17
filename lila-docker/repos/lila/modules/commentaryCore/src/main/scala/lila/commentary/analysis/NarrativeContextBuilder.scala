package lila.commentary.analysis

import chess.{ Color, Square }
import lila.commentary.CommentaryConfig
import lila.commentary.model._
import lila.commentary.model.authoring.{ AuthorQuestion, PlanHypothesis }
import lila.commentary.model.strategic._
import lila.commentary.analysis.L3._
import lila.commentary.analysis.semantic.RelationObservationCatalog
import lila.commentary.model.structure.AlignmentBand
import lila.commentary.analysis.structure.TranspositionPvAligner

object NarrativeContextBuilder:
  private val commentaryConfig = CommentaryConfig.fromEnv
  private val OpeningDataGoalPlyCutoff = 20

  final case class BuildResult(
      context: NarrativeContext,
      diagnosticPlanSidecar: PlanEvidenceEvaluator.DiagnosticPlanSidecar,
      selectedMainEvaluatedPlans: List[PlanEvidenceEvaluator.EvaluatedPlan] = Nil
  )

  private case class NarrativeFactSets(
      facts: List[Fact],
      mainPvFacts: List[Fact],
      threatLineFacts: List[Fact],
      counterfactualFacts: List[Fact]
  )

  private case class MoveDeltaSelection(
      delta: Option[MoveDelta],
      afterMove: Boolean
  )

  private case class StrategicPlanContext(
      partition: PlanEvidenceEvaluator.PartitionedPlans,
      experiments: List[StrategicPlanExperiment],
      restrictedDefenseConversion: Option[RestrictedDefenseConversionProof.Contract],
      dualAxisBind: Option[TwoAxisBindProof.Contract],
      mainPlans: List[PlanHypothesis],
      evidence: PlanEvidenceEvaluator.StrategicPlanEvidenceView
  )

  private case class StrategicPlanExperimentResults(
      experiments: List[StrategicPlanExperiment],
      restrictedDefenseConversion: Option[RestrictedDefenseConversionProof.Contract],
      dualAxisBind: Option[TwoAxisBindProof.Contract]
  )

  private case class ContextSupportSections(
      semantic: Option[SemanticSection],
      meta: Option[MetaSignals],
      strategicFlow: Option[String]
  )

  private case class AuthorProbeContext(
      playedSan: Option[String],
      authorQuestions: List[AuthorQuestion],
      probeRequests: List[ProbeRequest],
      probeValidation: PlanEvidenceEvaluator.ProbeValidation,
      rootProbeResults: List[ProbeResult],
      enrichedCandidates: List[CandidateInfo],
      targets: Targets
  )

  private case class BuildRootMaterial(
      color: Color,
      board: chess.Board,
      topSan: Option[String],
      topUci: Option[String]
  )

  private case class OpeningCandidateMaterial(
      openingEvent: Option[OpeningEvent],
      candidates: List[CandidateInfo],
      updatedBudget: OpeningEventBudget
  )

  private case class CandidateScoreContext(
      bestScore: Int,
      secondScore: Int,
      isWhiteToMove: Boolean
  ):
    def lossFromBest(score: Int): Int =
      if isWhiteToMove then (bestScore - score).max(0)
      else (score - bestScore).max(0)

    def bestGap: Int =
      lossFromBest(secondScore)

  def build(
    data: ExtendedAnalysisData,
    ctx: IntegratedContext,
    prevAnalysis: Option[ExtendedAnalysisData] = None,
    probeResults: List[ProbeResult] = Nil,
    openingRef: Option[OpeningReference] = None,
    prevOpeningRef: Option[OpeningReference] = None,  // For BranchPoint/TheoryEnds detection
    openingBudget: OpeningEventBudget = OpeningEventBudget(),  // Passed from game-level tracker
    afterAnalysis: Option[ExtendedAnalysisData] = None,
    renderMode: NarrativeRenderMode = NarrativeRenderMode.FullGame,
    variantKey: String = EarlyOpeningNarrationPolicy.StandardVariant
  ): NarrativeContext =
    buildWithDiagnostics(
      data = data,
      ctx = ctx,
      prevAnalysis = prevAnalysis,
      probeResults = probeResults,
      openingRef = openingRef,
      prevOpeningRef = prevOpeningRef,
      openingBudget = openingBudget,
      afterAnalysis = afterAnalysis,
      renderMode = renderMode,
      variantKey = variantKey
    ).context

  def buildWithDiagnostics(
    data: ExtendedAnalysisData,
    ctx: IntegratedContext,
    prevAnalysis: Option[ExtendedAnalysisData] = None,
    probeResults: List[ProbeResult] = Nil,
    openingRef: Option[OpeningReference] = None,
    prevOpeningRef: Option[OpeningReference] = None,  // For BranchPoint/TheoryEnds detection
    openingBudget: OpeningEventBudget = OpeningEventBudget(),  // Passed from game-level tracker
    afterAnalysis: Option[ExtendedAnalysisData] = None,
    renderMode: NarrativeRenderMode = NarrativeRenderMode.FullGame,
    variantKey: String = EarlyOpeningNarrationPolicy.StandardVariant
  ): BuildResult = {
    val root = buildRootMaterial(data)
    val header = buildHeader(ctx)
    val summary = buildSummary(data, ctx)
    val threats = buildThreatTable(ctx, root.topSan, root.topUci, data.fen)
    val pawnPlay = buildPawnPlayTable(ctx)
    val plans = buildPlanTable(data)
    val l1 = buildL1Snapshot(ctx)
    val phase = buildPhaseWithAfterTransition(ctx, data, prevAnalysis, afterAnalysis)
    val moveDelta = selectMoveDelta(data, prevAnalysis, afterAnalysis)
    val alignmentForTone = if commentaryConfig.structKbEnabled then ctx.planAlignment else None
    val authorProbeContext = buildAuthorProbeContext(data, ctx, probeResults, alignmentForTone)
    val strategicPlans = buildStrategicPlanContext(data, authorProbeContext.probeRequests, authorProbeContext.probeValidation)
    val supportSections =
      buildContextSupportSections(data, ctx, authorProbeContext.targets, authorProbeContext.rootProbeResults, afterAnalysis)

    val opponentPlan = buildOpponentPlan(data, ctx)


    val decision =
      buildDecisionIfNeeded(data, ctx, header, authorProbeContext, supportSections)

    val opening = buildOpeningCandidateMaterial(
      data = data,
      ctx = ctx,
      authorProbeContext = authorProbeContext,
      decision = decision,
      openingRef = openingRef,
      prevOpeningRef = prevOpeningRef,
      openingBudget = openingBudget
    )
    val authorEvidence = buildAuthorEvidence(data, authorProbeContext, opening.candidates)
    val factSets = buildNarrativeFactSets(data, root.board, root.color)

    val baseContext =
      assembleNarrativeContext(
        data = data,
        header = header,
        summary = summary,
        threats = threats,
        pawnPlay = pawnPlay,
        plans = plans,
        l1 = l1,
        phase = phase,
        moveDelta = moveDelta,
        candidates = opening.candidates,
        authorProbeContext = authorProbeContext,
        authorEvidence = authorEvidence,
        factSets = factSets,
        strategicPlans = strategicPlans,
        supportSections = supportSections,
        opponentPlan = opponentPlan,
        decision = decision,
        openingEvent = opening.openingEvent,
        openingRef = openingRef,
        updatedBudget = opening.updatedBudget,
        renderMode = renderMode,
        variantKey = variantKey
    )
    buildResult(baseContext, strategicPlans)
  }

  private def buildRootMaterial(data: ExtendedAnalysisData): BuildRootMaterial = {
    val baseCandidates = buildCandidates(data)
    BuildRootMaterial(
      color = if (data.isWhiteToMove) Color.White else Color.Black,
      board = boardFromFen(data.fen),
      topSan = baseCandidates.headOption.map(_.move),
      topUci = baseCandidates.headOption.flatMap(_.uci)
    )
  }

  private def boardFromFen(fen: String): chess.Board =
    chess.format.Fen.read(chess.variant.Standard, chess.format.Fen.Full(fen)).map(_.board).getOrElse(chess.Board.empty)

  private def buildOpeningCandidateMaterial(
      data: ExtendedAnalysisData,
      ctx: IntegratedContext,
      authorProbeContext: AuthorProbeContext,
      decision: Option[DecisionRationale],
      openingRef: Option[OpeningReference],
      prevOpeningRef: Option[OpeningReference],
      openingBudget: OpeningEventBudget
  ): OpeningCandidateMaterial = {
    val openingEvent = detectOpeningEvent(data, openingRef, decision, openingBudget, prevOpeningRef)
    OpeningCandidateMaterial(
      openingEvent = openingEvent,
      candidates =
        attachHypothesisCards(
          data = data,
          ctx = ctx,
          candidates = authorProbeContext.enrichedCandidates,
          probeResults = authorProbeContext.rootProbeResults,
          probeRequests = authorProbeContext.probeRequests,
          openingEvent = openingEvent
        ),
      updatedBudget = updatedOpeningBudget(openingBudget, data.ply, openingEvent)
    )
  }

  private def buildAuthorEvidence(
      data: ExtendedAnalysisData,
      authorProbeContext: AuthorProbeContext,
      candidates: List[CandidateInfo]
  ): List[lila.commentary.model.authoring.QuestionEvidence] =
    val bestEngineMove = data.alternatives.headOption.flatMap(_.moves.headOption)
    AuthorEvidenceBuilder.build(
      fen = data.fen,
      ply = data.ply,
      playedMove = data.prevMove,
      bestMove = bestEngineMove.orElse(candidates.headOption.flatMap(_.uci)),
      authorQuestions = authorProbeContext.authorQuestions,
      probeResults = authorProbeContext.rootProbeResults
    )

  private def buildResult(
      baseContext: NarrativeContext,
      strategicPlans: StrategicPlanContext
  ): BuildResult =
    BuildResult(
      context = baseContext.copy(openingGoalEvaluation = openingGoalEvaluation(baseContext)),
      diagnosticPlanSidecar = strategicPlans.partition.diagnosticSidecar,
      selectedMainEvaluatedPlans = strategicPlans.partition.selectedMainEvaluatedPlans
    )

  private def assembleNarrativeContext(
      data: ExtendedAnalysisData,
      header: ContextHeader,
      summary: NarrativeSummary,
      threats: ThreatTable,
      pawnPlay: PawnPlayTable,
      plans: PlanTable,
      l1: L1Snapshot,
      phase: PhaseContext,
      moveDelta: MoveDeltaSelection,
      candidates: List[CandidateInfo],
      authorProbeContext: AuthorProbeContext,
      authorEvidence: List[lila.commentary.model.authoring.QuestionEvidence],
      factSets: NarrativeFactSets,
      strategicPlans: StrategicPlanContext,
      supportSections: ContextSupportSections,
      opponentPlan: Option[PlanRow],
      decision: Option[DecisionRationale],
      openingEvent: Option[OpeningEvent],
      openingRef: Option[OpeningReference],
      updatedBudget: OpeningEventBudget,
      renderMode: NarrativeRenderMode,
      variantKey: String
  ): NarrativeContext =
    NarrativeContext(
      fen = data.fen,
      header = header,
      ply = data.ply,
      playedMove = data.prevMove,
      playedSan = authorProbeContext.playedSan,
      counterfactual = data.counterfactual,
      summary = summary,
      threats = threats,
      pawnPlay = pawnPlay,
      plans = plans,
      planContinuity = data.planContinuity,
      snapshots = List(l1),
      delta = moveDelta.delta,
      phase = phase,
      candidates = candidates,
      authorQuestions = authorProbeContext.authorQuestions,
      authorEvidence = authorEvidence,
      facts = factSets.facts,
      mainPvFacts = factSets.mainPvFacts,
      threatLineFacts = factSets.threatLineFacts,
      counterfactualFacts = factSets.counterfactualFacts,
      probeRequests = authorProbeContext.probeRequests,
      validatedRootProbeResults = authorProbeContext.rootProbeResults,
      mainStrategicPlans = strategicPlans.mainPlans,
      strategicPlanExperiments = strategicPlans.experiments,
      restrictedDefenseConversion = strategicPlans.restrictedDefenseConversion,
      dualAxisBind = strategicPlans.dualAxisBind,
      strategicPlanEvidence = strategicPlans.evidence,
      meta = supportSections.meta,
      strategicFlow = supportSections.strategicFlow,
      semantic = supportSections.semantic,
      opponentPlan = opponentPlan,
      decision = decision,
      openingEvent = openingEvent,
      openingData = openingRef,
      updatedBudget = updatedBudget,
      engineEvidence = Some(lila.commentary.model.strategic.EngineEvidence(
        depth = data.alternatives.map(_.depth).maxOption.getOrElse(0),
        variations = data.alternatives
      )),
      deltaAfterMove = moveDelta.afterMove,
      strategicSalience = data.strategicSalience,
      renderMode = renderMode,
      variantKey = EarlyOpeningNarrationPolicy.normalizeVariantKey(Some(variantKey))
    )

  private def buildDecisionIfNeeded(
      data: ExtendedAnalysisData,
      ctx: IntegratedContext,
      header: ContextHeader,
      authorProbeContext: AuthorProbeContext,
      supportSections: ContextSupportSections
  ): Option[DecisionRationale] =
    Option.when(shouldBuildDecisionRationale(header.choiceType, authorProbeContext.enrichedCandidates, authorProbeContext.rootProbeResults)) {
      calculateDecisionRationale(
        data,
        ctx,
        authorProbeContext.enrichedCandidates,
        supportSections.semantic,
        authorProbeContext.targets,
        authorProbeContext.rootProbeResults
      )
    }

  private def rootProbeResultsFor(fen: String, results: List[ProbeResult]): List[ProbeResult] =
    results.filter(result => result.fen.forall(_ == fen))

  private def buildPhaseWithAfterTransition(
      ctx: IntegratedContext,
      data: ExtendedAnalysisData,
      prevAnalysis: Option[ExtendedAnalysisData],
      afterAnalysis: Option[ExtendedAnalysisData]
  ): PhaseContext = {
    val phase = buildPhaseContext(ctx, prevAnalysis)
    val transition =
      afterAnalysis
        .filter(_ => data.prevMove.isDefined)
        .flatMap(after =>
          Option.when(after.phase != data.phase)(
            s"Transition from ${data.phase.capitalize} to ${after.phase.capitalize}"
          )
        )
    transition.fold(phase)(trigger => phase.copy(transitionTrigger = Some(trigger)))
  }

  private def selectMoveDelta(
      data: ExtendedAnalysisData,
      prevAnalysis: Option[ExtendedAnalysisData],
      afterAnalysis: Option[ExtendedAnalysisData]
  ): MoveDeltaSelection = {
    val afterDelta =
      afterAnalysis
        .filter(_ => data.prevMove.isDefined)
        .map(after => buildDelta(data, after))
    MoveDeltaSelection(
      delta = afterDelta.orElse(prevAnalysis.map(prev => buildDelta(prev, data))),
      afterMove = afterDelta.isDefined
    )
  }

  private def playedSanFromPrevMove(data: ExtendedAnalysisData): Option[String] =
    data.prevMove.flatMap { uci =>
      NarrativeUtils
        .uciListToSan(data.fen, List(uci))
        .headOption
        .orElse(Some(NarrativeUtils.formatUciAsSan(uci)))
    }

  private def updatedOpeningBudget(
      openingBudget: OpeningEventBudget,
      ply: Int,
      openingEvent: Option[OpeningEvent]
  ): OpeningEventBudget =
    openingEvent match {
      case Some(OpeningEvent.Intro(_, _, _, _)) => openingBudget.afterIntro
      case Some(OpeningEvent.TheoryEnds(_, _)) => openingBudget.afterTheoryEnds
      case Some(_) => openingBudget.afterEvent
      case None => openingBudget.updatePly(ply)
    }

  private def buildAuthorProbeContext(
      data: ExtendedAnalysisData,
      ctx: IntegratedContext,
      probeResults: List[ProbeResult],
      alignmentForTone: Option[lila.commentary.model.structure.PlanAlignment]
  ): AuthorProbeContext = {
    val prevalidatedRootProbeResults =
      rootProbeResultsFor(data.fen, probeResults)
        .filter(pr => ProbeContractValidator.validate(pr).isValid)
    val questionCandidates = buildCandidatesEnriched(data, prevalidatedRootProbeResults, alignmentForTone)
    val playedSan = playedSanFromPrevMove(data)
    val authorQuestions =
      AuthorQuestionGenerator.generate(
        data = data,
        ctx = ctx,
        candidates = questionCandidates,
        playedSan = playedSan
      )
    val probeRequests = buildProbeRequests(data, ctx, questionCandidates, authorQuestions)
    val probeValidation =
      PlanEvidenceEvaluator.validateProbeResults(probeResults, probeRequests)
    val rootProbeResults = rootProbeResultsFor(data.fen, probeValidation.validResults)
    val enrichedCandidates = buildCandidatesEnriched(data, rootProbeResults, alignmentForTone)
    AuthorProbeContext(
      playedSan = playedSan,
      authorQuestions = authorQuestions,
      probeRequests = probeRequests,
      probeValidation = probeValidation,
      rootProbeResults = rootProbeResults,
      enrichedCandidates = enrichedCandidates,
      targets = buildTargets(data, ctx, rootProbeResults)
    )
  }

  private def buildProbeRequests(
      data: ExtendedAnalysisData,
      ctx: IntegratedContext,
      candidates: List[CandidateInfo],
      authorQuestions: List[AuthorQuestion]
  ): List[ProbeRequest] = {
    val planScoring = PlanScoringResult(
      topPlans = data.plans,
      confidence = 0.0,
      phase = ctx.phase
    )
    val multiPv = data.alternatives.map(v => PvLine(v.moves, v.scoreCp, v.mate, v.depth))
    ProbeDetector
      .detect(
        ctx = ctx,
        planScoring = planScoring,
        planHypotheses = data.planHypotheses,
        multiPv = multiPv,
        fen = data.fen,
        playedMove = data.prevMove,
        candidates = candidates,
        authorQuestions = authorQuestions
      )
      .distinctBy(_.id)
  }

  private def buildStrategicPlanContext(
      data: ExtendedAnalysisData,
      probeRequests: List[ProbeRequest],
      probeValidation: PlanEvidenceEvaluator.ProbeValidation
  ): StrategicPlanContext = {
    val transpositionProofs =
      TranspositionPvAligner.alignPlans(
        fen = data.fen,
        lines = data.alternatives,
        hypotheses = data.planHypotheses
      )
    val partition =
      PlanEvidenceEvaluator.partition(
        hypotheses = data.planHypotheses,
        probeRequests = probeRequests,
        validatedProbeResults = probeValidation.validResults,
        rulePlanIds = data.plans.map(_.plan.id.toString.toLowerCase).toSet,
        isWhiteToMove = data.isWhiteToMove,
        droppedProbeCount = probeValidation.droppedCount,
        droppedProbeReasons = probeValidation.droppedReasons,
        invalidByRequestId = probeValidation.invalidByRequestId,
        softByRequestId = probeValidation.softByRequestId,
        transpositionProofs = transpositionProofs
      )
    val experimentResults =
      buildStrategicPlanExperimentResults(
        evaluated = partition.evaluated,
        validatedProbeResults = probeValidation.validResults,
        preventedPlans = data.preventedPlans,
        evalCp = data.evalCp,
        isWhiteToMove = data.isWhiteToMove,
        phase = data.phase,
        ply = data.ply,
        fen = data.fen
      )
    StrategicPlanContext(
      partition = partition,
      experiments = experimentResults.experiments,
      restrictedDefenseConversion = experimentResults.restrictedDefenseConversion,
      dualAxisBind = experimentResults.dualAxisBind,
      mainPlans = partition.selectedMainEvaluatedPlans.map(_.hypothesis),
      evidence = PlanEvidenceEvaluator.StrategicPlanEvidenceView.from(partition)
    )
  }

  private def buildContextSupportSections(
      data: ExtendedAnalysisData,
      ctx: IntegratedContext,
      targets: Targets,
      rootProbeResults: List[ProbeResult],
      afterAnalysis: Option[ExtendedAnalysisData]
  ): ContextSupportSections = {
    val lowStrategicSalience = data.strategicSalience == StrategicSalience.Low
    val hasCurrentBreakCarrier =
      data.preventedPlans.exists(plan =>
        plan.sourceScope == FactScope.Now &&
          plan.deniedResourceClass.contains("break") &&
          plan.deniedEntryScope.contains("file") &&
          plan.breakNeutralized.exists(_.trim.nonEmpty)
      )
    val semantic =
      if lowStrategicSalience && data.endgameFeatures.isEmpty && !hasCurrentBreakCarrier then None
      else buildSemanticSection(data, afterAnalysis)
    ContextSupportSections(
      semantic = semantic,
      meta = Option.unless(lowStrategicSalience)(buildMetaSignals(data, ctx, targets, rootProbeResults)).flatten,
      strategicFlow = Option.unless(lowStrategicSalience)(buildStrategicFlow(data)).flatten
    )
  }

  private def buildNarrativeFactSets(
      data: ExtendedAnalysisData,
      board: chess.Board,
      color: Color
  ): NarrativeFactSets = {
    val motifFactsRaw = FactExtractor.fromMotifs(board, data.motifs, FactScope.Now)
    val motifFacts =
      if data.endgameFeatures.isDefined then
        motifFactsRaw.filter {
          case _: Fact.Opposition => false
          case _: Fact.Zugzwang   => false
          case _                  => true
        }
      else motifFactsRaw
    val staticFacts = FactExtractor.extractStaticFacts(board, color)
    val endgameFacts = FactExtractor.extractEndgameFacts(board, color, data.endgameFeatures)
    val counterfactualFacts =
      data.counterfactual.toList.flatMap(cf => FactExtractor.fromMotifs(board, cf.missedMotifs, FactScope.Counterfactual))
    val threatLineFacts =
      data.preventedPlans.flatMap(_.sourceLine.toList).flatMap { line =>
        FactExtractor.fromMotifs(board, MoveAnalyzer.tokenizePv(data.fen, line.moves), FactScope.ThreatLine)
      }
    NarrativeFactSets(
      facts = motifFacts ++ staticFacts ++ endgameFacts,
      mainPvFacts = FactExtractor.fromMotifs(board, data.motifs, FactScope.MainPv),
      threatLineFacts = threatLineFacts,
      counterfactualFacts = counterfactualFacts
    )
  }

  private def openingGoalEvaluation(ctx: NarrativeContext): Option[OpeningGoals.Evaluation] =
    Option
      .when(openingContext(ctx)) {
        ctx.playedMove
          .flatMap(uci => MoveReviewPvLine.legalFenAfter(ctx.fen, uci))
          .flatMap(afterFen => OpeningGoals.analyze(ctx.copy(fen = afterFen, openingGoalEvaluation = None)))
      }
      .flatten

  private def openingContext(ctx: NarrativeContext): Boolean =
    ctx.header.phase.equalsIgnoreCase("Opening") ||
      ctx.phase.current.equalsIgnoreCase("Opening") ||
      ctx.openingEvent.nonEmpty ||
      (ctx.ply <= OpeningDataGoalPlyCutoff && ctx.openingData.nonEmpty)

  private final case class PlanExperimentCertifications(
      restrictedDefense: Option[RestrictedDefenseConversionProof.Contract],
      counterplayAxis: Option[CounterplayRestraintProof.Contract],
      dualAxisBind: Option[TwoAxisBindProof.Contract],
      localFileEntryBind: Option[LocalFileEntryProof.Contract],
      namedRouteNetworkBind: Option[RouteNetworkBindProof.Contract],
      heavyPieceLocalBind: Option[HeavyPieceLocalBindValidation.Contract]
  )

  private final case class PlanExperimentCertificationContext(
    plan: PlanEvidenceEvaluator.EvaluatedPlan,
    probeResultsById: Map[String, ProbeResult],
    preventedPlans: List[PreventedPlan],
    evalCp: Int,
    isWhiteToMove: Boolean,
    phase: String,
    ply: Int,
    fen: String
  )

  private final case class PlanExperimentProbeEvidence(
    supportResults: List[ProbeResult],
    refuteResults: List[ProbeResult]
  )

  private final case class PlanExperimentSignals(
    bestReplyStable: Boolean,
    futureSnapshotAligned: Boolean,
    counterBreakNeutralized: Boolean,
    moveOrderSensitive: Boolean
  )

  private[commentary] def buildStrategicPlanExperiments(
      evaluated: List[PlanEvidenceEvaluator.EvaluatedPlan],
      validatedProbeResults: List[ProbeResult],
      preventedPlans: List[PreventedPlan],
      evalCp: Int,
      isWhiteToMove: Boolean,
      phase: String = "middlegame",
      ply: Int = 0,
      fen: String = ""
  ): List[StrategicPlanExperiment] =
    buildStrategicPlanExperimentResults(
      evaluated = evaluated,
      validatedProbeResults = validatedProbeResults,
      preventedPlans = preventedPlans,
      evalCp = evalCp,
      isWhiteToMove = isWhiteToMove,
      phase = phase,
      ply = ply,
      fen = fen
    ).experiments

  private def buildStrategicPlanExperimentResults(
      evaluated: List[PlanEvidenceEvaluator.EvaluatedPlan],
      validatedProbeResults: List[ProbeResult],
      preventedPlans: List[PreventedPlan],
      evalCp: Int,
      isWhiteToMove: Boolean,
      phase: String,
      ply: Int,
      fen: String
  ): StrategicPlanExperimentResults =
    val resultsById = validatedProbeResults.groupBy(_.id).view.mapValues(_.head).toMap
    val experimentPairs = evaluated.map { plan =>
      val probeEvidence = planExperimentProbeEvidence(plan, resultsById)
      val certifications =
        planExperimentCertifications(
          plan = plan,
          probeResultsById = resultsById,
          preventedPlans = preventedPlans,
          evalCp = evalCp,
          isWhiteToMove = isWhiteToMove,
          phase = phase,
          ply = ply,
          fen = fen
        )
      val signals = planExperimentSignals(plan, probeEvidence, certifications)
      strategicPlanExperiment(plan, probeEvidence, certifications, signals) -> certifications
    }
    StrategicPlanExperimentResults(
      experiments = experimentPairs.map(_._1),
      restrictedDefenseConversion =
        experimentPairs
          .flatMap(_._2.restrictedDefense)
          .filter(_.certified)
          .sortBy(cert => -cert.confidence)
          .headOption,
      dualAxisBind =
        experimentPairs
          .flatMap(_._2.dualAxisBind)
          .filter(_.certified)
          .sortBy(cert => -cert.confidence)
          .headOption
    )

  private def planExperimentProbeEvidence(
    plan: PlanEvidenceEvaluator.EvaluatedPlan,
    resultsById: Map[String, ProbeResult]
  ): PlanExperimentProbeEvidence =
    PlanExperimentProbeEvidence(
      supportResults = plan.supportProbeIds.flatMap(resultsById.get).distinctBy(_.id),
      refuteResults = plan.refuteProbeIds.flatMap(resultsById.get).distinctBy(_.id)
    )

  private def planExperimentSignals(
    plan: PlanEvidenceEvaluator.EvaluatedPlan,
    probeEvidence: PlanExperimentProbeEvidence,
    certifications: PlanExperimentCertifications
  ): PlanExperimentSignals = {
    val bestReplyStable = experimentBestReplyStable(probeEvidence)
    val futureSnapshotAligned = experimentFutureSnapshotAligned(probeEvidence)
    PlanExperimentSignals(
      bestReplyStable = bestReplyStable,
      futureSnapshotAligned = futureSnapshotAligned,
      counterBreakNeutralized = probeEvidence.supportResults.exists(counterBreakNeutralizedBy),
      moveOrderSensitive =
        experimentMoveOrderSensitive(
          plan = plan,
          supportResults = probeEvidence.supportResults,
          refuteResults = probeEvidence.refuteResults,
          bestReplyStable = bestReplyStable,
          futureSnapshotAligned = futureSnapshotAligned,
          certifications = certifications
        )
    )
  }

  private def experimentBestReplyStable(probeEvidence: PlanExperimentProbeEvidence): Boolean =
    probeEvidence.supportResults.nonEmpty &&
      probeEvidence.refuteResults.isEmpty &&
      probeEvidence.supportResults.exists(hasReplyCoverage) &&
      probeEvidence.supportResults.forall(_.l1Delta.flatMap(_.collapseReason).forall(_.trim.isEmpty))

  private def experimentFutureSnapshotAligned(probeEvidence: PlanExperimentProbeEvidence): Boolean =
    probeEvidence.supportResults.exists(result => result.futureSnapshot.exists(isPositiveFutureSnapshot)) &&
      probeEvidence.refuteResults.isEmpty

  private def strategicPlanExperiment(
    plan: PlanEvidenceEvaluator.EvaluatedPlan,
    probeEvidence: PlanExperimentProbeEvidence,
    certifications: PlanExperimentCertifications,
    signals: PlanExperimentSignals
  ): StrategicPlanExperiment =
    StrategicPlanExperiment(
      planId = plan.hypothesis.planId,
      themeL1 = plan.themeL1,
      subplanId = plan.subplanId,
      evidenceTier = experimentEvidenceTier(plan.status, certifications),
      supportProbeCount = probeEvidence.supportResults.size,
      refuteProbeCount = probeEvidence.refuteResults.size,
      bestReplyStable = signals.bestReplyStable,
      futureSnapshotAligned = signals.futureSnapshotAligned,
      counterBreakNeutralized = signals.counterBreakNeutralized,
      moveOrderSensitive = signals.moveOrderSensitive,
      experimentConfidence =
        experimentConfidence(
          tier = plan.status,
          supportCount = probeEvidence.supportResults.size,
          refuteCount = probeEvidence.refuteResults.size,
          bestReplyStable = signals.bestReplyStable,
          futureSnapshotAligned = signals.futureSnapshotAligned,
          counterBreakNeutralized = signals.counterBreakNeutralized,
          moveOrderSensitive = signals.moveOrderSensitive,
          certifications = certifications
        )
    )

  private def planExperimentCertifications(
      plan: PlanEvidenceEvaluator.EvaluatedPlan,
      probeResultsById: Map[String, ProbeResult],
      preventedPlans: List[PreventedPlan],
      evalCp: Int,
      isWhiteToMove: Boolean,
      phase: String,
      ply: Int,
      fen: String
  ): PlanExperimentCertifications =
    val certificationContext =
      PlanExperimentCertificationContext(
        plan = plan,
        probeResultsById = probeResultsById,
        preventedPlans = preventedPlans,
        evalCp = evalCp,
        isWhiteToMove = isWhiteToMove,
        phase = phase,
        ply = ply,
        fen = fen
      )
    val restrictedDefense = restrictedDefenseCertification(certificationContext)
    val localFileEntryBind = localFileEntryCertification(certificationContext)
    PlanExperimentCertifications(
      restrictedDefense = restrictedDefense,
      counterplayAxis = counterplayAxisCertification(certificationContext),
      dualAxisBind = dualAxisBindCertification(certificationContext),
      localFileEntryBind = localFileEntryBind,
      namedRouteNetworkBind = namedRouteNetworkCertification(certificationContext, localFileEntryBind),
      heavyPieceLocalBind = heavyPieceLocalCertification(certificationContext)
    )

  private def restrictedDefenseCertification(
    ctx: PlanExperimentCertificationContext
  ): Option[RestrictedDefenseConversionProof.Contract] =
    RestrictedDefenseConversionProof.evaluate(
      plan = ctx.plan,
      probeResultsById = ctx.probeResultsById,
      preventedPlans = ctx.preventedPlans,
      evalCp = ctx.evalCp,
      isWhiteToMove = ctx.isWhiteToMove
    )

  private def counterplayAxisCertification(
    ctx: PlanExperimentCertificationContext
  ): Option[CounterplayRestraintProof.Contract] =
    CounterplayRestraintProof.evaluate(
      plan = ctx.plan,
      probeResultsById = ctx.probeResultsById,
      preventedPlans = ctx.preventedPlans,
      evalCp = ctx.evalCp,
      isWhiteToMove = ctx.isWhiteToMove,
      phase = ctx.phase,
      ply = ctx.ply,
      fen = ctx.fen
    )

  private def dualAxisBindCertification(
    ctx: PlanExperimentCertificationContext
  ): Option[TwoAxisBindProof.Contract] =
    TwoAxisBindProof.evaluate(
      plan = ctx.plan,
      probeResultsById = ctx.probeResultsById,
      preventedPlans = ctx.preventedPlans,
      evalCp = ctx.evalCp,
      isWhiteToMove = ctx.isWhiteToMove,
      phase = ctx.phase,
      ply = ctx.ply,
      fen = ctx.fen
    )

  private def localFileEntryCertification(
    ctx: PlanExperimentCertificationContext
  ): Option[LocalFileEntryProof.Contract] =
    LocalFileEntryProof.evaluate(
      plan = ctx.plan,
      probeResultsById = ctx.probeResultsById,
      preventedPlans = ctx.preventedPlans,
      evalCp = ctx.evalCp,
      isWhiteToMove = ctx.isWhiteToMove,
      phase = ctx.phase,
      ply = ctx.ply,
      fen = ctx.fen
    )

  private def namedRouteNetworkCertification(
    ctx: PlanExperimentCertificationContext,
    localFileEntryBind: Option[LocalFileEntryProof.Contract]
  ): Option[RouteNetworkBindProof.Contract] =
    RouteNetworkBindProof.evaluate(
      plan = ctx.plan,
      probeResultsById = ctx.probeResultsById,
      preventedPlans = ctx.preventedPlans,
      evalCp = ctx.evalCp,
      isWhiteToMove = ctx.isWhiteToMove,
      phase = ctx.phase,
      ply = ctx.ply,
      fen = ctx.fen,
      localFileEntryBindCertification = localFileEntryBind
    )

  private def heavyPieceLocalCertification(
    ctx: PlanExperimentCertificationContext
  ): Option[HeavyPieceLocalBindValidation.Contract] =
    HeavyPieceLocalBindValidation.evaluate(
      plan = ctx.plan,
      probeResultsById = ctx.probeResultsById,
      preventedPlans = ctx.preventedPlans,
      evalCp = ctx.evalCp,
      isWhiteToMove = ctx.isWhiteToMove,
      phase = ctx.phase,
      ply = ctx.ply,
      fen = ctx.fen
    )

  private def experimentEvidenceTier(
      status: PlanEvidenceEvaluator.PlanEvidenceStatus,
      certifications: PlanExperimentCertifications
  ): String =
    val restrictedDefenseTier =
      RestrictedDefenseConversionProof.playerFacingEvidenceTier(
        evidenceTierOf(status),
        certifications.restrictedDefense
      )
    val fileEntryTier =
      LocalFileEntryProof.playerFacingEvidenceTier(
        restrictedDefenseTier,
        certifications.localFileEntryBind
      )
    val namedRouteTier =
      RouteNetworkBindProof.playerFacingEvidenceTier(
        fileEntryTier,
        certifications.namedRouteNetworkBind
      )
    val heavyPieceTier =
      HeavyPieceLocalBindValidation.playerFacingEvidenceTier(
        namedRouteTier,
        certifications.heavyPieceLocalBind
      )
    experimentEvidenceTierOverride(
      certifications = certifications,
      restrictedDefenseTier = restrictedDefenseTier,
      fileEntryTier = fileEntryTier,
      namedRouteTier = namedRouteTier,
      heavyPieceTier = heavyPieceTier
    ).getOrElse(
      CounterplayRestraintProof.playerFacingEvidenceTier(
        restrictedDefenseTier,
        certifications.counterplayAxis
      )
    )

  private def experimentEvidenceTierOverride(
    certifications: PlanExperimentCertifications,
    restrictedDefenseTier: String,
    fileEntryTier: String,
    namedRouteTier: String,
    heavyPieceTier: String
  ): Option[String] =
    certifications.heavyPieceLocalBind
      .filter(_ => namedRouteTier == "evidence_backed")
      .map(_ => heavyPieceTier)
      .orElse(
        certifications.namedRouteNetworkBind
          .filter(_ => fileEntryTier == "evidence_backed")
          .map(_ => namedRouteTier)
      )
      .orElse(
        certifications.localFileEntryBind
          .filter(_ => restrictedDefenseTier == "evidence_backed")
          .map(_ => fileEntryTier)
      )
      .orElse(
        certifications.dualAxisBind
          .filter(_ => restrictedDefenseTier == "evidence_backed")
          .map(cert => if cert.certified then "evidence_backed" else "deferred")
      )

  private def experimentMoveOrderSensitive(
      plan: PlanEvidenceEvaluator.EvaluatedPlan,
      supportResults: List[ProbeResult],
      refuteResults: List[ProbeResult],
      bestReplyStable: Boolean,
      futureSnapshotAligned: Boolean,
      certifications: PlanExperimentCertifications
  ): Boolean =
    val replyCoverageUnstable =
      supportResults.nonEmpty &&
        supportResults.exists(hasReplyCoverage) &&
        !bestReplyStable &&
        !futureSnapshotAligned
    plan.status == PlanEvidenceEvaluator.PlanEvidenceStatus.PlayablePvCoupled ||
      (plan.pvCoupled && plan.missingSignals.nonEmpty) ||
      refuteResults.exists(_.l1Delta.flatMap(_.collapseReason).exists(_.trim.nonEmpty)) ||
      certifications.restrictedDefense.exists(_.moveOrderFragility.fragile) ||
      certifications.localFileEntryBind.exists(_.moveOrderFragility.fragile) ||
      certifications.namedRouteNetworkBind.exists(_.moveOrderFragility.fragile) ||
      certifications.heavyPieceLocalBind.exists(_.moveOrderFragility.fragile) ||
      certifications.dualAxisBind.exists(_.moveOrderFragility.fragile) ||
      replyCoverageUnstable

  private def counterBreakNeutralizedBy(result: ProbeResult): Boolean =
    result.futureSnapshot.exists(snapshot =>
      snapshot.planBlockersRemoved.exists(mentionsCounterBreak) ||
        snapshot.planPrereqsMet.exists(mentionsCounterBreak) ||
        snapshot.resolvedThreatKinds.exists(mentionsCounterplay)
    ) ||
      result.motifTags.exists(motif => mentionsCounterBreak(motif) || mentionsCounterplay(motif))

  private def detectOpeningEvent(
      data: ExtendedAnalysisData,
      openingRef: Option[OpeningReference],
      decision: Option[DecisionRationale],
      budget: OpeningEventBudget,
      prevRef: Option[OpeningReference]
  ): Option[OpeningEvent] = {
    if (!data.phase.equalsIgnoreCase("opening")) return None
    
    val cpLoss = data.counterfactual match {
      case Some(cf) => Some(cf.cpLoss)
      case None => Some(0)
    }
    
    val hasConstructiveEvidence = 
      decision.exists(d => d.delta.newOpportunities.nonEmpty || d.delta.planAdvancements.nonEmpty) ||
      data.plans.exists(_.score > 0.6) ||
      data.motifs.exists(m => m.isInstanceOf[Motif.Outpost] || m.isInstanceOf[Motif.Centralization])
    
    OpeningEventDetector.detect(
      ply = data.ply,
      playedMove = data.prevMove,
      fen = data.fen,
      ref = openingRef,
      budget = budget,
      cpLoss = cpLoss,
      hasConstructiveEvidence = hasConstructiveEvidence,
      prevRef = prevRef
    )
  }

  private def evidenceTierOf(status: PlanEvidenceEvaluator.PlanEvidenceStatus): String =
    status match
      case PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableEvidenceBacked  => "evidence_backed"
      case PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableTranspositionAligned => "transposition_aligned"
      case PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableStructuralOnly  => "structural_only"
      case PlanEvidenceEvaluator.PlanEvidenceStatus.PlayablePvCoupled       => "pv_coupled"
      case PlanEvidenceEvaluator.PlanEvidenceStatus.Deferred                => "deferred"
      case PlanEvidenceEvaluator.PlanEvidenceStatus.Refuted                 => "refuted"

  private def hasReplyCoverage(result: ProbeResult): Boolean =
    result.bestReplyPv.nonEmpty || result.replyPvs.exists(_.exists(_.nonEmpty))

  private def isPositiveFutureSnapshot(snapshot: FutureSnapshot): Boolean =
    snapshot.planPrereqsMet.nonEmpty ||
      snapshot.planBlockersRemoved.nonEmpty ||
      snapshot.targetsDelta.strategicAdded.nonEmpty ||
      snapshot.resolvedThreatKinds.nonEmpty

  private def mentionsCounterBreak(raw: String): Boolean =
    val normalized = normalizeExperimentToken(raw)
    normalized.contains("counter_break") ||
      normalized.contains("break_prevention") ||
      normalized.contains("deny_break") ||
      normalized.contains("break_denial")

  private def mentionsCounterplay(raw: String): Boolean =
    normalizeExperimentToken(raw).contains("counterplay")

  private def normalizeExperimentToken(raw: String): String =
    Option(raw)
      .map(_.trim.toLowerCase.replaceAll("[^a-z0-9]+", "_"))
      .getOrElse("")

  private def experimentConfidence(
      tier: PlanEvidenceEvaluator.PlanEvidenceStatus,
      supportCount: Int,
      refuteCount: Int,
      bestReplyStable: Boolean,
      futureSnapshotAligned: Boolean,
      counterBreakNeutralized: Boolean,
      moveOrderSensitive: Boolean,
      certifications: PlanExperimentCertifications
  ): Double =
    val base = experimentBaseConfidence(tier)
    val supportBonus = experimentSupportBonus(supportCount)
    val refutePenalty = experimentRefutePenalty(refuteCount)
    val stabilityBonus = if bestReplyStable then 0.05 else 0.0
    val futureBonus = if futureSnapshotAligned then 0.04 else 0.0
    val counterBreakBonus = if counterBreakNeutralized then 0.05 else 0.0
    val sensitivityPenalty = if moveOrderSensitive then 0.08 else 0.0
    val restrictedDefenseBonus = restrictedDefenseConfidenceAdjustment(certifications.restrictedDefense)
    val counterplayAxisBonus = counterplayAxisConfidenceAdjustment(certifications.counterplayAxis)
    val dualAxisBindBonus = dualAxisBindConfidenceAdjustment(certifications.dualAxisBind)
    val localFileEntryBonus = localFileEntryConfidenceAdjustment(certifications.localFileEntryBind)
    val namedRouteNetworkBonus = namedRouteNetworkConfidenceAdjustment(certifications.namedRouteNetworkBind)
    val heavyPiecePenalty = heavyPieceLocalConfidenceAdjustment(certifications.heavyPieceLocalBind)
    (base + supportBonus + stabilityBonus + futureBonus + counterBreakBonus +
      restrictedDefenseBonus + counterplayAxisBonus + dualAxisBindBonus +
      localFileEntryBonus + namedRouteNetworkBonus + heavyPiecePenalty -
      refutePenalty - sensitivityPenalty)
      .max(0.0)
      .min(0.98)

  private def experimentBaseConfidence(tier: PlanEvidenceEvaluator.PlanEvidenceStatus): Double =
    tier match
      case PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableEvidenceBacked       => 0.82
      case PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableTranspositionAligned => 0.74
      case PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableStructuralOnly       => 0.52
      case PlanEvidenceEvaluator.PlanEvidenceStatus.PlayablePvCoupled            => 0.62
      case PlanEvidenceEvaluator.PlanEvidenceStatus.Deferred                     => 0.38
      case PlanEvidenceEvaluator.PlanEvidenceStatus.Refuted                      => 0.10

  private def experimentSupportBonus(supportCount: Int): Double =
    math.min(0.08, supportCount * 0.03)

  private def experimentRefutePenalty(refuteCount: Int): Double =
    math.min(0.18, refuteCount * 0.08)

  private def restrictedDefenseConfidenceAdjustment(
    certification: Option[RestrictedDefenseConversionProof.Contract]
  ): Double =
    certification
      .map(cert =>
        if cert.certified then 0.05
        else if cert.routePersistence.bestDefenseStable && cert.routePersistence.futureSnapshotPersistent then -0.05
        else -0.12
      )
      .getOrElse(0.0)

  private def counterplayAxisConfidenceAdjustment(
    certification: Option[CounterplayRestraintProof.Contract]
  ): Double =
    certification
      .map(cert =>
        if cert.certified then 0.04
        else if cert.routePersistence.axisStillSuppressed then -0.04
        else -0.10
      )
      .getOrElse(0.0)

  private def dualAxisBindConfidenceAdjustment(
    certification: Option[TwoAxisBindProof.Contract]
  ): Double =
    certification
      .map(cert =>
        if cert.certified then 0.06
        else if cert.persistenceAfterBestDefense && cert.routeContinuity.futureSnapshotPersistent then -0.06
        else -0.14
      )
      .getOrElse(0.0)

  private def localFileEntryConfidenceAdjustment(
    certification: Option[LocalFileEntryProof.Contract]
  ): Double =
    certification
      .map(cert =>
        if cert.certified then 0.05
        else if cert.pressurePersistence && cert.routeContinuity.futureSnapshotPersistent then -0.05
        else -0.12
      )
      .getOrElse(0.0)

  private def namedRouteNetworkConfidenceAdjustment(
    certification: Option[RouteNetworkBindProof.Contract]
  ): Double =
    certification
      .map(cert =>
        if cert.certified then 0.04
        else if cert.pressurePersistence && cert.routeContinuity.futureSnapshotPersistent then -0.06
        else -0.12
      )
      .getOrElse(0.0)

  private def heavyPieceLocalConfidenceAdjustment(
    validation: Option[HeavyPieceLocalBindValidation.Contract]
  ): Double =
    validation
      .map(cert =>
        if cert.certified then -0.10
        else if cert.pressurePersistence && cert.routeContinuity.directBestDefensePresent then -0.12
        else -0.16
      )
      .getOrElse(0.0)

  private def buildHeader(ctx: IntegratedContext): ContextHeader = {
    val classification = ctx.classification.getOrElse(defaultClassification)
    
    ContextHeader(
      phase = classification.gamePhase.phaseType.toString,
      criticality = classification.criticality.criticalityType match {
        case CriticalityType.Normal         => "Normal"
        case CriticalityType.CriticalMoment => "Critical"
        case CriticalityType.ForcedSequence => "Forced"
      },
      choiceType = classification.choiceTopology.topologyType.toString,
      riskLevel = classification.riskProfile.riskLevel.toString,
      taskMode = classification.taskMode.taskMode.toString
    )
  }
  
  private def buildSummary(
    data: ExtendedAnalysisData,
    ctx: IntegratedContext
  ): NarrativeSummary = {
    val primaryPlan = summaryPrimaryPlan(data)
    val keyThreat = summaryKeyThreat(data, ctx)
    val hasMateThreatToUs = ctx.threatsToUs.exists(_.threats.exists(_.kind == ThreatKind.Mate))
    val hasMateThreatToThem = ctx.threatsToThem.exists(_.threats.exists(_.kind == ThreatKind.Mate))
    val hasMateCandidate = summaryHasMateCandidate(data)
    val hasForcedMateForUs = summaryHasForcedMateForUs(data, hasMateCandidate)
    val isMate =
      data.alternatives.exists(_.mate.isDefined) ||
        hasMateThreatToUs ||
        hasMateThreatToThem ||
        hasMateCandidate
    val (finalPrimaryPlan, finalKeyThreat) =
      summaryPrimaryPlanAndThreat(
        data = data,
        primaryPlan = primaryPlan,
        keyThreat = keyThreat,
        hasForcedMateForUs = hasForcedMateForUs,
        hasMateThreatToUs = hasMateThreatToUs
      )

    NarrativeSummary(
      primaryPlan = finalPrimaryPlan,
      keyThreat = finalKeyThreat,
      choiceType = summaryChoiceType(ctx),
      tensionPolicy = summaryTensionPolicy(ctx, isMate),
      evalDelta = summaryEvalDelta(data)
    )
  }

  private def summaryPrimaryPlan(data: ExtendedAnalysisData): String = {
    val alignmentHint = data.planAlignment.map(summaryHintFromAlignment).getOrElse("")
    data.plans.headOption
      .map(p => s"${p.plan.name} (${f"${p.score}%.2f"})$alignmentHint")
      .getOrElse(s"No clear plan$alignmentHint")
  }

  private def summaryKeyThreat(data: ExtendedAnalysisData, ctx: IntegratedContext): Option[String] =
    ctx.threatsToUs.flatMap { ta =>
      ta.threats
        .find(t =>
          t.attackSquares.headOption
            .flatMap(str => chess.Square.fromKey(str))
            .exists(sq =>
              NarrativeUtils.isVerifiedThreat(
                data.fen,
                sq,
                if (!data.isWhiteToMove) chess.Color.White else chess.Color.Black
              )
            )
        )
        .map(summaryThreatLine)
    }

  private def summaryThreatLine(threat: Threat): String = {
    val urgency =
      if (threat.lossIfIgnoredCp >= 800 || threat.kind == ThreatKind.Mate) "URGENT"
      else if (threat.lossIfIgnoredCp >= 300) "IMPORTANT"
      else "LOW"
    val attacker = threat.motifs.headOption.flatMap(publicThreatMotifLabel).getOrElse("attacker")
    s"$urgency: $attacker on ${threat.attackSquares.head}"
  }

  private def summaryChoiceType(ctx: IntegratedContext): String =
    ctx.classification
      .map(c => c.choiceTopology.topologyType.toString)
      .getOrElse("Unknown")

  private def summaryHasMateCandidate(data: ExtendedAnalysisData): Boolean =
    data.candidates.exists { cand =>
      cand.line.mate.isDefined || NarrativeUtils.uciListToSan(data.fen, cand.line.moves.take(1)).exists(_.contains("#"))
    }

  private def summaryHasForcedMateForUs(data: ExtendedAnalysisData, hasMateCandidate: Boolean): Boolean =
    data.alternatives.exists(_.mate.exists(_ > 0)) || hasMateCandidate

  private def summaryTensionPolicy(ctx: IntegratedContext, isMate: Boolean): String =
    if (isMate) "Maximum (shattered)" else ctx.pawnAnalysis
      .map(p => s"${p.tensionPolicy} (${p.primaryDriver})")
      .getOrElse("N/A")

  private def summaryEvalDelta(data: ExtendedAnalysisData): String =
    data.practicalAssessment
      .map { pa => 
        val score = pa.engineScore / 100.0
        val sign = if (score > 0) "+" else ""
        f"Eval: $sign$score%.1f"
      }
      .getOrElse("No eval")

  private def summaryPrimaryPlanAndThreat(
    data: ExtendedAnalysisData,
    primaryPlan: String,
    keyThreat: Option[String],
    hasForcedMateForUs: Boolean,
    hasMateThreatToUs: Boolean
  ): (String, Option[String]) =
    if (hasForcedMateForUs) {
      val mateText = summaryForcedMateText(data)
      (mateText, Some(s"URGENT: $mateText available"))
    } else if (hasMateThreatToUs) {
      (primaryPlan, Some("URGENT: Mate threat against us"))
    } else {
      (primaryPlan, keyThreat)
    }

  private def summaryForcedMateText(data: ExtendedAnalysisData): String =
    data.alternatives.flatMap(_.mate).filter(_ > 0).headOption
      .orElse(data.candidates.flatMap(_.line.mate).filter(_ > 0).headOption)
      .map(m => s"Forced Mate in $m")
      .getOrElse("Forced Mate")

  private def publicThreatMotifLabel(raw: String): Option[String] =
    val motif = Option(raw).map(_.takeWhile(_ != '(').trim).filter(_.nonEmpty)
    motif.flatMap { raw =>
      RelationObservationCatalog
        .relationWitnessOnlyFallbackLabelForMotifTag(raw)
        .orElse {
          if RelationObservationCatalog.relationWitnessOnlyMotifTag(raw) ||
            RelationObservationCatalog.pvDrawResourceOnlyMotifTag(raw)
          then None
          else
            RelationObservationCatalog.deferredFallbackForMotifTag(raw) match
              case Some(fallback) if fallback.allowsNonRelationText =>
                fallback.label.orElse(Some("practical threat"))
              case Some(_) => None
              case None    => Some(raw)
        }
    }
  
  private def buildThreatTable(ctx: IntegratedContext, topSan: Option[String], topUci: Option[String], fen: String): ThreatTable = {
    // TO US threats: bestDefense is valid (how we can defend against opponent's threat)
    val toUs = ctx.threatsToUs.map(ta => buildThreatRowsToUs(ta, topSan, topUci, ctx.isWhiteToMove, fen)).getOrElse(Nil)
    // TO THEM threats: bestDefense should be None (we don't know their defensive moves)
    val toThem = ctx.threatsToThem.map(ta => buildThreatRowsToThem(ta)).getOrElse(Nil)
    ThreatTable(toUs = toUs, toThem = toThem)
  }
  
  // For threats TO US: bestDefense represents how we can defend
  private def buildThreatRowsToUs(ta: ThreatAnalysis, topSan: Option[String], topUci: Option[String], isWhiteToMove: Boolean, fen: String): List[ThreatRow] = {
    val victimColor: Color = if (isWhiteToMove) Color.White else Color.Black
    val uniqueThreats = ta.threats
      .filter(_.lossIfIgnoredCp >= 80) // Noise gate
      .filter { t =>
        val attackSq = t.attackSquares.headOption.flatMap(k => Square.fromKey(k))
        attackSq.exists(sq => NarrativeUtils.isVerifiedThreat(fen, sq, victimColor))
      }
      .distinctBy(t => (t.kind, t.attackSquares.headOption, t.bestDefense))
      .take(3)
    
    uniqueThreats.map { t =>
      val isTopDefense = (topUci.isDefined && t.bestDefense == topUci) || 
                         (topSan.exists(s => t.bestDefense.exists(_.startsWith(s))))

      ThreatRow(
        kind = t.kind.toString,
        side = "US",
        square = t.attackSquares.headOption,
        lossIfIgnoredCp = t.lossIfIgnoredCp,
        turnsToImpact = t.turnsToImpact,
        bestDefense = t.bestDefense, // Valid: how we defend against this threat
        defenseCount = t.defenseCount,
        insufficientData = ta.insufficientData,
        isTopCandidateDefense = isTopDefense,
        targetPieces = t.targetPieces.map(_.trim).filter(_.nonEmpty).distinct.take(3),
        motifs = t.motifs.flatMap(publicThreatMotifLabel).distinct.take(3)
      )
    }
  }
  
  // For threats TO THEM: we don't know their defensive moves, so bestDefense = None
  private def buildThreatRowsToThem(ta: ThreatAnalysis): List[ThreatRow] = {
    val uniqueThreats = ta.threats
      .filter(_.lossIfIgnoredCp >= 80) // Noise gate: skip insignificant threats
      .distinctBy(t => (t.kind, t.attackSquares.headOption))
      .take(3)
    
    uniqueThreats.map { t =>
      ThreatRow(
        kind = t.kind.toString,
        side = "THEM",
        square = t.attackSquares.headOption,
        lossIfIgnoredCp = t.lossIfIgnoredCp,
        turnsToImpact = t.turnsToImpact,
        bestDefense = None, // Semantic fix: we don't know their defensive options
        defenseCount = 0,   // Unknown
        insufficientData = ta.insufficientData,
        targetPieces = t.targetPieces.map(_.trim).filter(_.nonEmpty).distinct.take(3),
        motifs = t.motifs.flatMap(publicThreatMotifLabel).distinct.take(3)
      )
    }
  }
  
  private def buildPawnPlayTable(ctx: IntegratedContext): PawnPlayTable = {
    ctx.pawnAnalysis match {
      case Some(pa) =>
        PawnPlayTable(
          breakReady = pa.pawnBreakReady,
          breakFile = pa.breakFile,
          breakImpact = if (pa.breakImpact >= 200) "High" else if (pa.breakImpact >= 100) "Medium" else "Low",
          tensionPolicy = pa.tensionPolicy.toString,
          tensionReason = deriveTensionReason(pa, ctx),
          passedPawnUrgency = pa.passedPawnUrgency.toString,
          passerBlockade = pa.blockadeSquare.map(sq => 
            s"${sq.key}${pa.blockadeRole.map(r => s" by ${r.name}").getOrElse("")}"
          ),
          counterBreak = pa.counterBreak,
          primaryDriver = pa.primaryDriver
        )
      case None =>
        PawnPlayTable(
          breakReady = false,
          breakFile = None,
          breakImpact = "Low",
          tensionPolicy = "Ignore",
          tensionReason = "No pawn analysis available",
          passedPawnUrgency = "Background",
          passerBlockade = None,
          counterBreak = false,
          primaryDriver = "quiet"
        )
    }
  }
  
  private def deriveTensionReason(pa: PawnPlayAnalysis, ctx: IntegratedContext): String = {
    pa.tensionPolicy match {
      case TensionPolicy.Maintain =>
        if (pa.pawnBreakReady) "preparing break"
        else if (ctx.tacticalThreatToThem) "attack preparation"
        else "flexibility needed"
      case TensionPolicy.Release =>
        if (pa.advanceOrCapture) "must resolve"
        else if (pa.passedPawnUrgency == PassedPawnUrgency.Critical) "passer advance"
        else "favorable exchange"
      case TensionPolicy.Ignore =>
        "no significant tension"
    }
  }
  
  private def buildPlanTable(
    data: ExtendedAnalysisData
  ): PlanTable = {
    val basePlans = basePlanRows(data)
    val top5 =
      if summaryHasForcedMateForUs(data, summaryHasMateCandidate(data)) then forcedMatePlanRows(data, basePlans)
      else basePlans
    val suppressed = List.empty[SuppressedPlan]

    PlanTable(top5 = top5, suppressed = suppressed)
  }

  private def basePlanRows(data: ExtendedAnalysisData): List[PlanRow] = {
    val establishedKey =
      data.planContinuity
        .filter(_.consecutivePlies >= 2)
        .map(c => c.planId.map(_.toLowerCase).getOrElse(c.planName.toLowerCase))

    data.plans.take(5).zipWithIndex.map { case (planMatch, index) =>
      planTableRow(planMatch, index, establishedKey)
    }
  }

  private def planTableRow(
    planMatch: PlanMatch,
    index: Int,
    establishedKey: Option[String]
  ): PlanRow = {
    val isEstablished =
      establishedKey.exists(k => k == planMatch.plan.id.toString.toLowerCase || k == planMatch.plan.name.toLowerCase)
    PlanRow(
      rank = index + 1,
      name = planMatch.plan.name,
      score = planMatch.score,
      evidence = planMatch.evidence.take(2).map(_.description),
      supports = planMatch.supports,
      blockers = planMatch.blockers,
      missingPrereqs = planMatch.missingPrereqs,
      isEstablished = isEstablished
    )
  }

  private def forcedMatePlanRows(data: ExtendedAnalysisData, basePlans: List[PlanRow]): List[PlanRow] =
    forcedMatePlanRow(data) :: basePlans.take(4).map(plan => plan.copy(rank = plan.rank + 1))

  private def forcedMatePlanRow(data: ExtendedAnalysisData): PlanRow =
    PlanRow(
      rank = 1,
      name = summaryForcedMateText(data),
      score = 1.0,
      evidence = List("Checkmate is available"),
      confidence = ConfidenceLevel.Engine
    )

  private def buildStrategicFlow(data: ExtendedAnalysisData): Option[String] = {
    val side = if (data.isWhiteToMove) "White" else "Black"
    val anchorPlan =
      data.planSequence
        .flatMap(_.primaryPlanName)
        .orElse(data.plans.headOption.map(_.plan.name))
        .orElse(data.planContinuity.map(_.planName))
    val continuitySnippet =
      data.planContinuity
        .filter(_.consecutivePlies >= 2)
        .map(c => s" This idea has held for ${c.consecutivePlies} plies.")
        .getOrElse("")

    data.planSequence.map { seq =>
      val planLabel = anchorPlan.getOrElse("the current plan")
      val alignmentClause = data.planAlignment.flatMap(flowHintFromAlignment).map(h => s" $h").getOrElse("")
      val base = seq.transitionType match {
        case TransitionType.Continuation =>
          s"$side continues with $planLabel."
        case TransitionType.NaturalShift =>
          s"$side's plan context shifts toward $planLabel."
        case TransitionType.ForcedPivot =>
          s"$side pivots into $planLabel while tactical pressure is present."
        case TransitionType.Opportunistic =>
          s"$side switches to $planLabel with a new chance in the position."
        case TransitionType.Opening =>
          s"$side starts with $planLabel."
      }
      s"$base$alignmentClause$continuitySnippet".trim
    }.orElse {
      data.planContinuity.flatMap { continuity =>
        Option.when(continuity.consecutivePlies >= 2)(s"$side continues ${continuity.planName}.")
      }
    }
  }
  
  private def buildL1Snapshot(ctx: IntegratedContext): L1Snapshot =
    ctx.features
      .map(features =>
        l1SnapshotFromFeatures(
          features = features,
          isWhiteToMove = ctx.isWhiteToMove,
          isEndgame = ctx.classification.exists(_.gamePhase.isEndgame)
        )
      )
      .getOrElse(emptyL1Snapshot)

  private def l1SnapshotFromFeatures(
    features: PositionFeatures,
    isWhiteToMove: Boolean,
    isEndgame: Boolean
  ): L1Snapshot = {
    val materialPhase = features.materialPhase
    val isPawnEndgame = (materialPhase.whiteMaterial + materialPhase.blackMaterial) <= 6 && isEndgame
    val (kingSafetyUs, kingSafetyThem) = l1KingSafety(features.kingSafety, isWhiteToMove)

    L1Snapshot(
      material = l1MaterialString(materialPhase.materialDiff, isWhiteToMove),
      imbalance = l1Imbalance(features.imbalance),
      kingSafetyUs = kingSafetyUs,
      kingSafetyThem = kingSafetyThem,
      mobility = l1Mobility(features.activity, isWhiteToMove),
      centerControl = Option.unless(isPawnEndgame)(l1CenterControl(features.centralSpace, isWhiteToMove)).flatten,
      openFiles = if isPawnEndgame then Nil else computeOpenFiles(features.fen)
    )
  }

  private def emptyL1Snapshot: L1Snapshot =
    L1Snapshot(
      material = "=",
      imbalance = None,
      kingSafetyUs = None,
      kingSafetyThem = None,
      mobility = None,
      centerControl = None,
      openFiles = Nil
    )

  private def l1MaterialString(materialDiff: Int, isWhiteToMove: Boolean): String = {
    val sideMaterial = if isWhiteToMove then materialDiff else -materialDiff
    if sideMaterial > 0 then s"+$sideMaterial"
    else if sideMaterial < 0 then s"$sideMaterial"
    else "="
  }

  private def l1Imbalance(imbalance: MaterialImbalanceFeatures): Option[String] =
    if imbalance.whiteBishopPair && !imbalance.blackBishopPair then Some("Bishop pair (us)")
    else if imbalance.blackBishopPair && !imbalance.whiteBishopPair then Some("Bishop pair (them)")
    else if imbalance.whiteRooks != imbalance.blackRooks then
      Some(s"Rook imbalance (${imbalance.whiteRooks}v${imbalance.blackRooks})")
    else None

  private def l1KingSafety(
    kingSafety: KingSafetyFeatures,
    isWhiteToMove: Boolean
  ): (Option[String], Option[String]) = {
    val (ourAttackers, ourEscapes) =
      if isWhiteToMove then (kingSafety.whiteAttackersCount, kingSafety.whiteEscapeSquares)
      else (kingSafety.blackAttackersCount, kingSafety.blackEscapeSquares)
    val (theirAttackers, theirEscapes) =
      if isWhiteToMove then (kingSafety.blackAttackersCount, kingSafety.blackEscapeSquares)
      else (kingSafety.whiteAttackersCount, kingSafety.whiteEscapeSquares)
    (l1KingExposure(ourAttackers, ourEscapes), l1KingExposure(theirAttackers, theirEscapes))
  }

  private def l1KingExposure(attackers: Int, escapes: Int): Option[String] =
    Option.when(attackers >= 2 || escapes <= 1)(s"Exposed ($attackers attackers, $escapes escapes)")

  private def l1Mobility(activity: ActivityFeatures, isWhiteToMove: Boolean): Option[String] = {
    val mobilityDiff =
      if isWhiteToMove then activity.whitePseudoMobility - activity.blackPseudoMobility
      else activity.blackPseudoMobility - activity.whitePseudoMobility
    if mobilityDiff > 10 then Some(s"Advantage (+$mobilityDiff)")
    else if mobilityDiff < -10 then Some(s"Disadvantage ($mobilityDiff)")
    else None
  }

  private def l1CenterControl(centralSpace: CentralSpaceFeatures, isWhiteToMove: Boolean): Option[String] = {
    val centerDiff =
      if isWhiteToMove then centralSpace.whiteCenterControl - centralSpace.blackCenterControl
      else centralSpace.blackCenterControl - centralSpace.whiteCenterControl
    if centerDiff >= 2 then Some("Dominant")
    else if centerDiff <= -2 then Some("Weak")
    else None
  }
  
  private def buildCandidates(data: ExtendedAnalysisData): List[CandidateInfo] = {
    val scoreContext = candidateScoreContext(data)
    data.candidates.zipWithIndex
      .map { case (candidate, index) => buildCandidateInfo(data, candidate, index, scoreContext) }
      .filterNot(_.move.isEmpty)
  }

  private def candidateScoreContext(data: ExtendedAnalysisData): CandidateScoreContext =
    val bestScore = data.candidates.headOption.map(_.score).getOrElse(0)
    CandidateScoreContext(
      bestScore = bestScore,
      secondScore = data.candidates.lift(1).map(_.score).getOrElse(bestScore),
      isWhiteToMove = data.isWhiteToMove
    )

  private def buildCandidateInfo(
      data: ExtendedAnalysisData,
      candidate: AnalyzedCandidate,
      index: Int,
      scoreContext: CandidateScoreContext
  ): CandidateInfo =
    val sanMoves = NarrativeUtils.uciListToSan(data.fen, candidate.line.moves.take(2))
    val cpDiff = scoreContext.lossFromBest(candidate.score)
    CandidateInfo(
      move = sanMoves.headOption.getOrElse(candidate.move),
      uci = Some(candidate.move),
      annotation = candidateAnnotation(index, cpDiff, scoreContext.bestGap),
      planAlignment = candidate.moveIntent.immediate,
      structureGuidance = None,
      alignmentBand = None,
      downstreamTactic = candidate.moveIntent.downstream,
      tacticalAlert = candidateResponseAlert(candidate.motifs.filter(_.plyIndex == 1), sanMoves),
      practicalDifficulty = if candidate.line.moves.length > 6 then "complex" else "clean",
      whyNot = candidateWhyNot(index, cpDiff, sanMoves),
      tags = mapVariationTags(candidate.line.tags),
      tacticEvidence = candidate.motifs.filter(_.plyIndex == 0).flatMap(candidateTacticEvidenceLine),
      facts = candidate.facts,
      lineSanMoves = NarrativeUtils.uciListToSan(data.fen, candidate.line.moves.take(6)),
      lineMotifs = candidate.motifs
    )

  private def candidateAnnotation(index: Int, cpDiff: Int, bestGap: Int): String =
    if index == 0 then
      if bestGap >= Thresholds.MISTAKE_CP then "!" else ""
    else if cpDiff >= Thresholds.DOUBLE_QUESTION_CP then "??"
    else if cpDiff >= Thresholds.SINGLE_QUESTION_CP then "?"
    else if cpDiff >= Thresholds.DUBIOUS_CP then "?!"
    else ""

  private def candidateResponseAlert(
      responseMotifs: List[Motif],
      sanMoves: List[String]
  ): Option[String] =
    val replySan = sanMoves.lift(1).getOrElse("")
    responseMotifs.collectFirst {
      case _: Motif.Check => s"allows $replySan check"
      case _: Motif.Fork => s"allows $replySan fork"
      case m: Motif.Capture if m.captureType == Motif.CaptureType.Winning =>
        s"allows $replySan to gain material"
      case _: Motif.DiscoveredAttack => "reveals discovered attack"
    }

  private def candidateTacticEvidenceLine(motif: Motif): Option[String] =
    motif match {
      case f: Motif.Fork =>
        Some(s"Fork pressure from ${roleLabel(f.attackingPiece)} on ${f.square} against ${f.targets.map(roleLabel).mkString(" and ")}.")
      case p: Motif.Pin =>
        val pinnedSquare = p.pinnedSq.map(_.key).getOrElse("?")
        Some(s"Pin pressure on $pinnedSquare against the ${roleLabel(p.targetBehind)}.")
      case c: Motif.Capture if c.captureType == Motif.CaptureType.Winning =>
        Some(s"Material-gain capture by ${roleLabel(c.piece)} on ${c.square}.")
      case d: Motif.DiscoveredAttack =>
        val targetSquare = d.targetSq.map(_.key).getOrElse("?")
        Some(s"Discovered attack against the ${roleLabel(d.target)} on $targetSquare.")
      case ch: Motif.Check =>
        Some(s"Check on ${ch.targetSquare}.")
      case sk: Motif.Skewer =>
        Some(s"Skewer pressure through the ${roleLabel(sk.frontPiece)}.")
      case o: Motif.Outpost =>
        Some(s"Outpost for the ${roleLabel(o.piece)} on ${o.square}.")
      case of: Motif.OpenFileControl =>
        Some(s"Open-file pressure on the ${of.file}-file.")
      case c: Motif.Centralization =>
        Some(s"Centralization toward ${c.square}.")
      case rl: Motif.RookLift =>
        Some(s"Rook lift toward rank ${rl.toRank}.")
      case dom: Motif.Domination =>
        RelationObservationCatalog
          .relationWitnessOnlyFallbackLabelForMotifTag(MoveReviewExchangeAnalyzer.RelationKind.Domination)
          .map(label => s"${label.capitalize} by the ${roleLabel(dom.dominatingPiece)}.")
      case man: Motif.Maneuver =>
        Some(s"Maneuver by the ${roleLabel(man.piece)} for ${man.purpose.replace('_', ' ')}.")
      case tp: Motif.TrappedPiece =>
        RelationObservationCatalog
          .relationWitnessOnlyFallbackLabelForMotifTag(MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece)
          .map(label => s"${label.capitalize} for the ${roleLabel(tp.trappedRole)}.")
      case _: Motif.KnightVsBishop =>
        Some("Knight-versus-bishop imbalance.")
      case b: Motif.Blockade =>
        Some(s"Blockade on ${b.pawnSquare}.")
      case _: Motif.SmotheredMate =>
        Some("Smothered-mate ideas.")
      case xr: Motif.XRay =>
        Some(s"X-ray pressure against the ${roleLabel(xr.target)} on ${xr.square}.")
      case _ => None
    }

  private def candidateWhyNot(index: Int, cpDiff: Int, sanMoves: List[String]): Option[String] =
    if index > 0 && cpDiff >= 50 then
      val diff = cpDiff / 100.0
      val refutation = sanMoves.lift(1).map(move => s" after $move").getOrElse("")
      val reason =
        if diff > 2.0 then "decisive loss"
        else if diff > 0.8 then "significant disadvantage"
        else "slight inaccuracy"
      Some(f"$reason ($diff%.1f)$refutation")
    else None
  
  private def mapVariationTags(varTags: List[lila.commentary.model.strategic.VariationTag]): List[CandidateTag] = {
    import lila.commentary.model.strategic.VariationTag
    varTags.flatMap {
      case VariationTag.Sharp        => Some(CandidateTag.Sharp)
      case VariationTag.Solid        => Some(CandidateTag.Solid)
      case VariationTag.Prophylaxis  => Some(CandidateTag.Prophylactic)
      case VariationTag.Simplification => Some(CandidateTag.Converting)
      case _ => None // Mistake, Good, Excellent, Inaccuracy, Blunder, Forced are not strategic tags
    }
  }

  private def roleLabel(role: chess.Role): String =
    Option(role).map(_.name.toLowerCase).getOrElse("piece")
  
  private val defaultClassification = PositionClassification(
    nature = NatureResult(lila.commentary.analysis.L3.NatureType.Static, 0, 0, 0, false),
    criticality = CriticalityResult(CriticalityType.Normal, 0, None, 0),
    choiceTopology = ChoiceTopologyResult(ChoiceTopologyType.StyleChoice, 0, 0, None, 0.0, 0.0, None),
    gamePhase = GamePhaseResult(GamePhaseType.Middlegame, 0, false, 0),
    simplifyBias = SimplifyBiasResult(false, 0, false, false),
    drawBias = DrawBiasResult(false, false, false, false, false),
    riskProfile = RiskProfileResult(RiskLevel.Medium, 0, 0, 0),
    taskMode = TaskModeResult(TaskModeType.ExplainPlan, "default")
  )
  
  private def computeOpenFiles(fen: String): List[String] = {
    val boardPart = fen.split(" ").headOption.getOrElse("")
    val ranks = boardPart.split("/")
    
    def expandRank(rank: String): String = {
      rank.flatMap { c =>
        if (c.isDigit) " " * c.asDigit
        else c.toString
      }
    }
    
    val expandedRanks = ranks.map(expandRank)
    val files = "abcdefgh"
    
    files.zipWithIndex.collect {
      case (fileChar, fileIdx) if expandedRanks.forall { rank =>
        rank.lift(fileIdx).forall(c => c != 'p' && c != 'P')
      } => fileChar.toString
    }.toList
  }
  
  private def buildPhaseContext(
    ctx: IntegratedContext,
    prevAnalysis: Option[ExtendedAnalysisData]
  ): PhaseContext = {
    val phaseResult = ctx.classification.map(_.gamePhase)
    val currentPhase = ctx.phase.capitalize
    
    val reason = phaseResult.map { pr =>
      val queens = if (pr.queensOnBoard) "Queens present" else "Queens traded"
      s"Material: ${pr.totalMaterial}, Minors: ${pr.minorPiecesCount}, $queens"
    }.getOrElse("Insufficient data")
    
    val transitionTrigger = prevAnalysis.flatMap { prev =>
      if (prev.phase != ctx.phase) {
        val from = prev.phase.capitalize
        val to = currentPhase
        Some(s"Transition from $from to $to")
      } else None
    }
    
    PhaseContext(
      current = currentPhase,
      reason = reason,
      transitionTrigger = transitionTrigger
    )
  }

  private def buildDelta(
    prev: ExtendedAnalysisData,
    current: ExtendedAnalysisData
  ): MoveDelta = {
    val evalChange = current.evalCp - prev.evalCp

    val prevOpenFiles = prev.positionalFeatures.collect { case PositionalTag.OpenFile(f, _) => f }.toSet
    val currOpenFiles = current.positionalFeatures.collect { case PositionalTag.OpenFile(f, _) => f }.toSet
    val openFileCreated = (currOpenFiles -- prevOpenFiles).toList.sortBy(_.char).headOption.map(_.char.toString)
    
    val prevMotifs = prev.motifs.map(_.getClass.getSimpleName).toSet
    val currMotifs = current.motifs.map(_.getClass.getSimpleName).toSet
    
    val newMotifs = (currMotifs -- prevMotifs).toList.sorted
    val lostMotifs = (prevMotifs -- currMotifs).toList.sorted
    
    val structureChange = if (newMotifs.contains("PawnChain") && !prevMotifs.contains("PawnChain"))
      Some("Pawn chain established")
    else if (lostMotifs.contains("PawnChain"))
      Some("Pawn chain broken")
    else if (current.structuralWeaknesses.size > prev.structuralWeaknesses.size)
      Some("New structural weakness appeared")
    else None

    MoveDelta(
      evalChange = evalChange,
      newMotifs = newMotifs,
      lostMotifs = lostMotifs,
      structureChange = structureChange,
      openFileCreated = openFileCreated,
      phaseChange = None
    )
  }
  private def buildMetaSignals(data: ExtendedAnalysisData, ctx: IntegratedContext, targets: Targets, probeResults: List[ProbeResult]): Option[MetaSignals] = {
    val hasClassification = ctx.classification.isDefined
    val hasThreats = ctx.threatsToUs.isDefined || ctx.threatsToThem.isDefined
    val hasPlans = data.plans.nonEmpty
    val hasProbeResults = probeResults.nonEmpty
    
    if (!hasClassification && !hasThreats && !hasPlans && !hasProbeResults) None
    else {
      val choiceType = buildChoiceType(ctx)
      val planConcurrency = buildPlanConcurrency(data)
      val divergence = buildDivergence(data)
      val errorClass = buildErrorClassification(data)
      val whyNot = buildWhyNotSummary(probeResults, ctx.isWhiteToMove)

      Some(MetaSignals(
        choiceType = choiceType,
        targets = targets,
        planConcurrency = planConcurrency,
        divergence = divergence,
        errorClass = errorClass,
        whyNot = whyNot
      ))
    }
  }

  private def buildWhyNotSummary(probeResults: List[ProbeResult], isWhiteToMove: Boolean): Option[String] = {
    if (probeResults.isEmpty) None
    else {
      val summaries = probeResults.flatMap { pr =>
        val moverLoss = moverLossFor(pr, isWhiteToMove)
        if (moverLoss >= 200) {
          val moveText = pr.probedMove.getOrElse("this move")
          Some(s"'$moveText' is refuted losing $moverLoss cp${collapseNoteOf(pr)} [Verified]")
        } else None
      }
      if (summaries.isEmpty) None else Some(summaries.mkString(". "))
    }
  }

  private def moverLossFor(result: ProbeResult, isWhiteToMove: Boolean): Int =
    if (isWhiteToMove) -result.deltaVsBaseline else result.deltaVsBaseline

  private def collapseNoteOf(result: ProbeResult): String =
    result.l1Delta.flatMap(_.collapseReason).map(r => s" ($r)").getOrElse("")

  private def replyPvsOf(result: ProbeResult): List[List[String]] =
    result.replyPvs.getOrElse(if (result.bestReplyPv.nonEmpty) List(result.bestReplyPv) else Nil)

  private def replySanAfter(fen: String, moveUci: String, replyUci: String): String =
    NarrativeUtils.uciListToSan(fen, List(moveUci, replyUci)).lift(1).getOrElse(replyUci)

  private def probeReplySanLines(fen: String, moveUci: String, replyPvs: List[List[String]]): List[String] =
    replyPvs.take(2).flatMap { pv =>
      val san = NarrativeUtils.uciListToSan(fen, moveUci :: pv.take(6)).drop(1).mkString(" ")
      Option(san).map(_.trim).filter(_.nonEmpty)
    }

  private def strategicLineCitation(
      data: ExtendedAnalysisData,
      moveUci: String,
      replyPv: List[String]
  ): Option[String] =
    LineScopedCitation.strategicCitation(
      data.fen,
      data.ply + 1,
      lila.commentary.model.strategic.VariationLine(
        moves = moveUci :: replyPv.take(5),
        scoreCp = 0
      )
    )

  private def buildCandidatesEnriched(
    data: ExtendedAnalysisData,
    probeResults: List[ProbeResult],
    planAlignment: Option[lila.commentary.model.structure.PlanAlignment]
  ): List[CandidateInfo] = {
    val isWhiteToMove = data.isWhiteToMove
    val existing = buildCandidates(data)
    val existingUcis = existing.flatMap(_.uci).toSet

    val enriched = existing.zipWithIndex.map { case (candidate, index) =>
      enrichCandidateWithProbe(
        data = data,
        candidate = candidate,
        index = index,
        probe = probeResults.find(_.probedMove == candidate.uci),
        isWhiteToMove = isWhiteToMove,
        planAlignment = planAlignment
      )
    }

    enriched ++ ghostProbeCandidates(data, probeResults, existingUcis, isWhiteToMove)
  }

  private def enrichCandidateWithProbe(
    data: ExtendedAnalysisData,
    candidate: CandidateInfo,
    index: Int,
    probe: Option[ProbeResult],
    isWhiteToMove: Boolean,
    planAlignment: Option[lila.commentary.model.structure.PlanAlignment]
  ): CandidateInfo = {
    val baseCandidate = candidate.copy(
      whyNot = probe.flatMap(pr => probeCandidateWhyNot(data, candidate, pr, isWhiteToMove)).orElse(candidate.whyNot),
      tags = (candidate.tags ++ probe.toList.flatMap(pr => probeCandidateTags(pr, isWhiteToMove))).distinct,
      probeLines = probeCandidateLines(data, candidate, probe)
    )
    toneCandidateByAlignment(baseCandidate, index, planAlignment)
  }

  private def probeCandidateLines(
    data: ExtendedAnalysisData,
    candidate: CandidateInfo,
    probe: Option[ProbeResult]
  ): List[String] =
    probe
      .flatMap(pr => candidate.uci.map(uci => probeReplySanLines(data.fen, uci, replyPvsOf(pr))))
      .getOrElse(Nil)

  private def probeCandidateTags(pr: ProbeResult, isWhiteToMove: Boolean): List[CandidateTag] =
    if pr.id.startsWith("competitive") then List(CandidateTag.Competitive)
    else if pr.id.startsWith("aggressive") && moverLossFor(pr, isWhiteToMove) >= 50 then List(CandidateTag.TacticalGamble)
    else Nil

  private def probeCandidateWhyNot(
    data: ExtendedAnalysisData,
    candidate: CandidateInfo,
    pr: ProbeResult,
    isWhiteToMove: Boolean
  ): Option[String] = {
    val moverLoss = moverLossFor(pr, isWhiteToMove)
    val lineCitation = candidate.uci.flatMap(uci => strategicLineCitation(data, uci, pr.bestReplyPv))
    if (moverLoss >= 50) {
      val bestReply = pr.bestReplyPv.headOption.getOrElse("?")
      val replySan = candidate.uci.map(uci => replySanAfter(data.fen, uci, bestReply)).getOrElse(bestReply)
      val collapseNote = collapseNoteOf(pr)

      if pr.id.startsWith("aggressive") then
        lineCitation
          .map(citation => s"the line $citation is clearly refuted$collapseNote")
          .orElse(Some(s"refuted: $replySan is a clear answer$collapseNote"))
      else
        lineCitation
          .map(citation => s"the line $citation is inferior by $moverLoss cp$collapseNote")
          .orElse(Some(s"inferior by $moverLoss cp after $replySan$collapseNote"))
    } else if pr.id.startsWith("competitive") && moverLoss.abs < 30 then Some("verified as a strong alternative")
    else None
  }

  private def ghostProbeCandidates(
    data: ExtendedAnalysisData,
    probeResults: List[ProbeResult],
    existingUcis: Set[String],
    isWhiteToMove: Boolean
  ): List[CandidateInfo] =
    probeResults.flatMap(pr =>
      pr.probedMove.filterNot(existingUcis.contains).map(uci => ghostProbeCandidate(data, pr, uci, isWhiteToMove))
    )

  private def ghostProbeCandidate(
    data: ExtendedAnalysisData,
    pr: ProbeResult,
    uci: String,
    isWhiteToMove: Boolean
  ): CandidateInfo = {
    val san = NarrativeUtils.uciListToSan(data.fen, List(uci)).headOption.getOrElse(uci)
    val moverLoss = moverLossFor(pr, isWhiteToMove)
    val bestReply = pr.bestReplyPv.headOption.getOrElse("?")
    val replySan = replySanAfter(data.fen, uci, bestReply)
    val collapseNote = collapseNoteOf(pr)
    val ghostCitation = strategicLineCitation(data, uci, pr.bestReplyPv)

    CandidateInfo(
      move = san,
      uci = Some(uci),
      annotation = if (moverLoss >= 200) "??" else "?",
      planAlignment = if (pr.id.startsWith("aggressive")) "Tactical Gamble" else "Alternative Path",
      structureGuidance = None,
      alignmentBand = None,
      tacticalAlert = None,
      practicalDifficulty = "complex",
      whyNot =
        ghostCitation
          .map(citation => s"the line $citation is refuted (-$moverLoss cp)$collapseNote")
          .orElse(Some(s"refuted by $replySan (-$moverLoss cp)$collapseNote")),
      tags = ghostProbeTags(pr),
      lineSanMoves = NarrativeUtils.uciListToSan(data.fen, uci :: pr.bestReplyPv.take(5)),
      probeLines = probeReplySanLines(data.fen, uci, replyPvsOf(pr))
    )
  }

  private def ghostProbeTags(pr: ProbeResult): List[CandidateTag] =
    if pr.id.startsWith("aggressive") then List(CandidateTag.TacticalGamble, CandidateTag.Sharp)
    else if pr.id.startsWith("competitive") then List(CandidateTag.Competitive)
    else Nil

  private def toneCandidateByAlignment(
    candidate: CandidateInfo,
    index: Int,
    planAlignment: Option[lila.commentary.model.structure.PlanAlignment]
  ): CandidateInfo =
    if index > 1 then candidate
    else
      planAlignment match
        case Some(pa) =>
          val guidance = pa.narrativeIntent.orElse(Some(defaultGuidance(pa.band)))
          val riskNote = riskHintFromAlignment(pa)
          val whyNotWithRisk = mergeWhyNot(candidate.whyNot, riskNote)
          pa.band match
            case AlignmentBand.OnBook =>
              candidate.copy(
                structureGuidance = guidance,
                alignmentBand = Some(pa.band.toString),
                whyNot = if index == 0 then candidate.whyNot else whyNotWithRisk
              )
            case AlignmentBand.Playable =>
              candidate.copy(
                planAlignment = s"${candidate.planAlignment} (practical)",
                structureGuidance = guidance,
                alignmentBand = Some(pa.band.toString),
                whyNot = whyNotWithRisk
              )
            case AlignmentBand.OffPlan =>
              candidate.copy(
                planAlignment = s"${candidate.planAlignment} (risky)",
                structureGuidance = guidance,
                alignmentBand = Some(pa.band.toString),
                whyNot = mergeWhyNot(whyNotWithRisk, Some("plan coherence is fragile if move order slips"))
              )
            case AlignmentBand.Unknown =>
              candidate.copy(
                planAlignment = s"${candidate.planAlignment} (needs verification)",
                structureGuidance = guidance,
                alignmentBand = Some(pa.band.toString),
                whyNot = mergeWhyNot(whyNotWithRisk, Some("structural read is uncertain, verify concrete tactics"))
              )
        case None =>
          candidate

  private def mergeWhyNot(base: Option[String], extra: Option[String]): Option[String] =
    (base.map(_.trim).filter(_.nonEmpty), extra.map(_.trim).filter(_.nonEmpty)) match
      case (Some(a), Some(b)) if a.equalsIgnoreCase(b) => Some(a)
      case (Some(a), Some(b)) => Some(s"$a; $b")
      case (Some(a), None) => Some(a)
      case (None, Some(b)) => Some(b)
      case _ => None

  private def summaryHintFromAlignment(pa: lila.commentary.model.structure.PlanAlignment): String =
    pa.band match
      case AlignmentBand.OnBook => " [plan coherence high]"
      case AlignmentBand.Playable => " [playable structure plan]"
      case AlignmentBand.OffPlan => " [structural risk]"
      case AlignmentBand.Unknown => " [structure needs verification]"

  private def flowHintFromAlignment(pa: lila.commentary.model.structure.PlanAlignment): Option[String] =
    pa.band match
      case AlignmentBand.OnBook => Some("The continuation remains structurally coherent.")
      case AlignmentBand.Playable => Some("The continuation is playable but needs accurate move order.")
      case AlignmentBand.OffPlan => Some("The current route drifts from the cleaner structural plan.")
      case AlignmentBand.Unknown => Some("Structural interpretation is uncertain, so concrete checks matter.")

  private def riskHintFromAlignment(pa: lila.commentary.model.structure.PlanAlignment): Option[String] =
    pa.band match
      case AlignmentBand.OnBook =>
        None
      case AlignmentBand.Playable =>
        pa.reasonWeights.get("PRECOND_MISS").filter(_ > 0.0).map(_ => "some strategic preconditions are still missing")
      case AlignmentBand.OffPlan =>
        val anti = pa.reasonWeights.getOrElse("ANTI_PLAN", 0.0)
        val pre = pa.reasonWeights.getOrElse("PRECOND_MISS", 0.0)
        if anti >= 0.15 then Some("main line conflicts with structure-first planning")
        else if pre > 0.0 then Some("key preconditions for the structure plan are missing")
        else Some("continuation is strategically brittle")
      case AlignmentBand.Unknown =>
        Some("structure confidence is low")

  private def defaultGuidance(band: AlignmentBand): String =
    band match
      case AlignmentBand.OnBook => "keep the position coherent and improve pieces behind the pawn skeleton"
      case AlignmentBand.Playable => "maintain flexibility and avoid irreversible pawn commitments too early"
      case AlignmentBand.OffPlan => "reassess long-term pawn commitments before forcing activity"
      case AlignmentBand.Unknown => "verify tactical details first, then commit to a structural route"

  private case class HypothesisDraft(
    axis: HypothesisAxis,
    claim: String,
    supportSignals: List[String],
    conflictSignals: List[String],
    baseConfidence: Double,
    horizon: HypothesisHorizon
  )

  private case class HypothesisDraftContext(
    data: ExtendedAnalysisData,
    ctx: IntegratedContext,
    candidate: CandidateInfo,
    index: Int,
    rank: Option[Int],
    cpGap: Option[Int],
    probeSignals: ProbeDetector.HypothesisVerificationSignals,
    topCandidate: Option[CandidateInfo],
    openingEvent: Option[OpeningEvent]
  ) {
    val move: String = candidate.move.trim
    val alignment: String = candidate.planAlignment.trim
    val alignmentLow: String = alignment.toLowerCase
    val planName: String = data.plans.headOption.map(_.plan.name).getOrElse("current plan")
    val practicalLow: String = candidate.practicalDifficulty.trim.toLowerCase
    val whyNot: String = candidate.whyNot.getOrElse("").toLowerCase
    val tacticalAlert: String = candidate.tacticalAlert.getOrElse("").toLowerCase
    val isKnightRouteShift: Boolean = topCandidate.exists(tc => tc != candidate && knightRouteShift(tc, candidate))
    val breakFile: Option[String] = ctx.pawnAnalysis.flatMap(_.breakFile)
    val breakReady: Boolean = ctx.pawnAnalysis.exists(_.pawnBreakReady)
    val breakImpact: Int = ctx.pawnAnalysis.map(_.breakImpact).getOrElse(0)
    val conversionWindow: Boolean =
      if data.isWhiteToMove then data.evalCp >= 80
      else data.evalCp <= -80
    val hasKingSignal: Boolean =
      tacticalAlert.contains("king") ||
        tacticalAlert.contains("check") ||
        tacticalAlert.contains("mate") ||
        ctx.threatsToUs.exists(_.threats.exists(t => t.kind == ThreatKind.Mate || t.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP))
    val hasStructSignal: Boolean =
      candidate.facts.exists {
        case _: Fact.WeakSquare | _: Fact.Outpost => true
        case _                                     => false
      } || whyNot.contains("structure") || whyNot.contains("square") || whyNot.contains("pawn")
    val openingSignal: Option[String] = openingEvent.map {
      case OpeningEvent.BranchPoint(_, reason, _) =>
        s"opening branch point: ${reason.take(42)}"
      case OpeningEvent.Novelty(_, _, evidence, _) =>
        s"opening novelty evidence: ${evidence.take(42)}"
      case OpeningEvent.OutOfBook(_, _, _) =>
        "position moved out of book"
      case OpeningEvent.TheoryEnds(_, sampleCount) =>
        s"theory sample thinned to $sampleCount games"
      case OpeningEvent.Intro(_, _, theme, _) =>
        s"opening theme: ${theme.take(42)}"
    }
    val cpSignal: Option[String] = cpGap.map(g => s"engine gap ${f"${g.toDouble / 100}%.1f"} pawns")
    val localSeed: Int = Math.abs(candidate.move.hashCode) ^ (index * 0x9e3779b9) ^ cpGap.getOrElse(0)
    val strategicFrame: Option[ProbeDetector.StrategicFrame] =
      if data.phase.equalsIgnoreCase("middlegame") then probeSignals.strategicFrame
      else None
    val planScoreSignal: Option[String] =
      data.plans.headOption.map(p => variedPlanScoreSignal(p.score, localSeed ^ 0x24d8f59c))
    val rankSignal: Option[String] = rank.map(r => variedEngineRankSignal(r, localSeed ^ 0x3b5296f1))
    val motifSignal: Option[String] = candidate.tacticEvidence.headOption.map(_.take(56))
  }

  private def signals(values: Option[String]*): List[String] =
    values.toList.flatten

  private case class RankedHypothesis(
    card: HypothesisCard,
    score: Double,
    family: String
  )

  private def attachHypothesisCards(
    data: ExtendedAnalysisData,
    ctx: IntegratedContext,
    candidates: List[CandidateInfo],
    probeResults: List[ProbeResult],
    probeRequests: List[ProbeRequest],
    openingEvent: Option[OpeningEvent]
  ): List[CandidateInfo] = {
    if (candidates.isEmpty) return candidates
    val top = candidates.headOption
    candidates.zipWithIndex.map { case (cand, idx) =>
      cand.copy(
        hypotheses = selectedHypothesisCards(
          data = data,
          ctx = ctx,
          candidate = cand,
          index = idx,
          probeResults = probeResults,
          probeRequests = probeRequests,
          topCandidate = top,
          openingEvent = openingEvent
        )
      )
    }
  }

  private def selectedHypothesisCards(
    data: ExtendedAnalysisData,
    ctx: IntegratedContext,
    candidate: CandidateInfo,
    index: Int,
    probeResults: List[ProbeResult],
    probeRequests: List[ProbeRequest],
    topCandidate: Option[CandidateInfo],
    openingEvent: Option[OpeningEvent]
  ): List[HypothesisCard] =
    val (rankOpt, cpGapOpt) = candidateRankAndGap(data, candidate)
    val probeSignals =
      ProbeDetector.hypothesisVerificationSignals(
        candidate = candidate,
        probeResults = probeResults,
        probeRequests = probeRequests,
        isWhiteToMove = data.isWhiteToMove
      )
    val ranked =
      rankedHypothesisDrafts(
        data = data,
        ctx = ctx,
        candidate = candidate,
        index = index,
        rank = rankOpt,
        cpGap = cpGapOpt,
        probeSignals = probeSignals,
        topCandidate = topCandidate,
        openingEvent = openingEvent
      )
    val selected = selectRankedHypothesisFamilies(ranked)
    diversifySelectedHypotheses(selected, probeSignals).map(_.card)

  private def rankedHypothesisDrafts(
    data: ExtendedAnalysisData,
    ctx: IntegratedContext,
    candidate: CandidateInfo,
    index: Int,
    rank: Option[Int],
    cpGap: Option[Int],
    probeSignals: ProbeDetector.HypothesisVerificationSignals,
    topCandidate: Option[CandidateInfo],
    openingEvent: Option[OpeningEvent]
  ): List[RankedHypothesis] =
    buildHypothesisDrafts(
      data = data,
      ctx = ctx,
      candidate = candidate,
      index = index,
      rank = rank,
      cpGap = cpGap,
      probeSignals = probeSignals,
      topCandidate = topCandidate,
      openingEvent = openingEvent
    )
      .flatMap(d => rankHypothesisDraft(d, candidate, probeSignals, rank, cpGap))
      .sortBy(r => -r.score)

  private def selectRankedHypothesisFamilies(ranked: List[RankedHypothesis]): List[RankedHypothesis] =
    ranked
      .foldLeft((Set.empty[String], List.empty[RankedHypothesis])) { case ((seen, acc), rh) =>
        if (seen.contains(rh.family)) (seen, acc)
        else (seen + rh.family, acc :+ rh)
      }
      ._2

  private def diversifySelectedHypotheses(
    selected: List[RankedHypothesis],
    probeSignals: ProbeDetector.HypothesisVerificationSignals
  ): List[RankedHypothesis] =
    selected.headOption match
      case Some(primary) =>
        val promotedPrimary =
          if primary.card.axis == HypothesisAxis.Plan then
            selected
              .drop(1)
              .find(rh => rh.card.axis != HypothesisAxis.Plan && rh.score >= primary.score - 0.08)
              .getOrElse(primary)
          else primary
        val secondary =
          selected
            .filterNot(_ == promotedPrimary)
            .find(_.card.axis != promotedPrimary.card.axis)
            .orElse(selected.filterNot(_ == promotedPrimary).headOption)
        applyLongHorizonProtection(
          selected = selected,
          diversified = (promotedPrimary :: secondary.toList).take(2),
          probeSignals = probeSignals
        )
      case None => Nil

  private def candidateRankAndGap(
    data: ExtendedAnalysisData,
    candidate: CandidateInfo
  ): (Option[Int], Option[Int]) = {
    val byVariation = data.alternatives.zipWithIndex.collectFirst {
      case (v, idx) if candidate.uci.exists(cu => v.moves.headOption.exists(vu => NarrativeUtils.uciEquivalent(cu, vu))) =>
        (idx + 1, v.effectiveScore)
    }
    val byCandidate = data.candidates.zipWithIndex.collectFirst {
      case (c, idx) if candidate.uci.exists(cu => NarrativeUtils.uciEquivalent(cu, c.move)) =>
        (idx + 1, c.score)
    }
    val matched = byVariation.orElse(byCandidate)
    val bestScore =
      data.alternatives.headOption.map(_.effectiveScore).orElse(data.candidates.headOption.map(_.score))
    val cpGap = for
      best <- bestScore
      (_, score) <- matched
    yield cpLossForSideToMove(best, score, data.isWhiteToMove)
    (matched.map(_._1), cpGap)
  }

  private def cpLossForSideToMove(best: Int, score: Int, isWhiteToMove: Boolean): Int =
    if isWhiteToMove then (best - score).max(0) else (score - best).max(0)

  private def buildHypothesisDrafts(
    data: ExtendedAnalysisData,
    ctx: IntegratedContext,
    candidate: CandidateInfo,
    index: Int,
    rank: Option[Int],
    cpGap: Option[Int],
    probeSignals: ProbeDetector.HypothesisVerificationSignals,
    topCandidate: Option[CandidateInfo],
    openingEvent: Option[OpeningEvent]
  ): List[HypothesisDraft] = {
    val h = HypothesisDraftContext(data, ctx, candidate, index, rank, cpGap, probeSignals, topCandidate, openingEvent)
    List(
      planHypothesisDraft(h),
      structureHypothesisDraft(h),
      initiativeHypothesisDraft(h),
      conversionHypothesisDraft(h),
      kingSafetyHypothesisDraft(h),
      pieceCoordinationHypothesisDraft(h),
      pawnBreakHypothesisDraft(h),
      endgameHypothesisDraft(h)
    ).filter(d => d.claim.trim.nonEmpty)
  }

  private def planHypothesisDraft(h: HypothesisDraftContext): HypothesisDraft = {
    val claim =
      h.strategicFrame
        .map(frame => strategicMiddlegameClaim(HypothesisAxis.Plan, h.move, frame, h.localSeed ^ 0x11f17f1d))
        .getOrElse {
          if h.index == 0 then
            NarrativeLexicon.pick(h.localSeed ^ 0x11f17f1d, List(
              s"${h.move} keeps ${humanPlan(h.planName)} as the central roadmap, limiting early strategic drift.",
              s"${h.move} anchors play around ${humanPlan(h.planName)}, so follow-up choices stay structurally coherent.",
              s"${h.move} preserves the ${humanPlan(h.planName)} framework and avoids premature route changes.",
              s"${h.move} keeps the position tied to ${humanPlan(h.planName)}, delaying unnecessary plan detours."
            ))
          else
            NarrativeLexicon.pick(h.localSeed ^ 0x517cc1b7, List(
              s"${h.move} redirects play toward ${humanPlan(h.alignment)}, creating a new strategic branch from the main continuation.",
              s"${h.move} shifts the game into a ${humanPlan(h.alignment)} route, with a different plan cadence from the principal line.",
              s"${h.move} chooses a ${humanPlan(h.alignment)} channel instead of the principal structure-first continuation.",
              s"${h.move} reroutes priorities toward ${humanPlan(h.alignment)}, so the long plan map differs from the engine leader."
            ))
        }
    HypothesisDraft(
      axis = HypothesisAxis.Plan,
      claim = claim,
      supportSignals = signals(h.planScoreSignal, h.rankSignal, h.openingSignal),
      conflictSignals = signals(
        Option.when(h.cpGap.exists(_ >= 100))("engine gap is significant for this route"),
        Option.when(h.candidate.tags.contains(CandidateTag.TacticalGamble))("line is flagged as tactical gamble")
      ),
      baseConfidence = if h.index == 0 then 0.58 else 0.48,
      horizon = HypothesisHorizon.Medium
    )
  }

  private def structureHypothesisDraft(h: HypothesisDraftContext): HypothesisDraft = {
    val claim =
      if h.hasStructSignal then
        NarrativeLexicon.pick(h.localSeed ^ 0x5f356495, List(
          s"${h.move} changes the structural balance, trading immediate activity for longer-term square commitments.",
          s"${h.move} reshapes pawn and square commitments, accepting delayed strategic consequences for present activity.",
          s"${h.move} keeps structural tensions central, where current activity is exchanged for a longer-term square map.",
          s"${h.move} commits to a structural route first, so long-term square control outweighs short tactical comfort."
        ))
      else
        NarrativeLexicon.pick(h.localSeed ^ 0x6c6c6c6c, List(
          s"${h.move} tries to preserve structure first, postponing irreversible pawn commitments.",
          s"${h.move} keeps the pawn skeleton flexible and delays structural decisions until better timing appears.",
          s"${h.move} maintains structural optionality, avoiding early pawn commitments that narrow later plans.",
          s"${h.move} prioritizes structural elasticity now, so irreversible pawn choices are deferred."
        ))
    HypothesisDraft(
      axis = HypothesisAxis.Structure,
      claim = claim,
      supportSignals = signals(
        Option.when(h.hasStructSignal)("fact-level structural weakness signal"),
        Option.when(h.breakReady)("pawn tension context is active")
      ),
      conflictSignals = signals(
        Option.when(h.whyNot.contains("weak"))("candidate rationale already flags a weakness"),
        Option.when(h.cpGap.exists(_ >= 120))("engine penalizes resulting structure")
      ),
      baseConfidence = if h.hasStructSignal then 0.52 else 0.43,
      horizon = HypothesisHorizon.Long
    )
  }

  private def initiativeHypothesisDraft(h: HypothesisDraftContext): HypothesisDraft = {
    val claim =
      h.strategicFrame
        .map(frame => strategicMiddlegameClaim(HypothesisAxis.Initiative, h.move, frame, h.localSeed ^ 0x4f6cdd1d))
        .getOrElse {
          if h.candidate.tags.exists(t => t == CandidateTag.Sharp || t == CandidateTag.TacticalGamble) || h.practicalLow == "complex" then
            NarrativeLexicon.pick(h.localSeed ^ 0x4f6cdd1d, List(
              s"${h.move} pushes for initiative immediately, but tempo accuracy is mandatory from move one.",
              s"${h.move} seeks dynamic momentum now, so even one slow follow-up can let the opponent consolidate.",
              s"${h.move} is an initiative bid: concrete timing is required before the opponent consolidates.",
              s"${h.move} keeps the initiative race open, with little margin for imprecise sequencing."
            ))
          else
            NarrativeLexicon.pick(h.localSeed ^ 0x63d5a6f1, List(
              s"${h.move} concedes some initiative for stability, so the practical test is whether counterplay can be contained.",
              s"${h.move} trades immediate initiative for structure, and the key question is if counterplay arrives in time.",
              s"${h.move} prioritizes stability over momentum, making initiative handoff the central practical risk.",
              s"${h.move} slows the initiative race deliberately, so the follow-up must show that counterplay stays contained."
            ))
        }
    HypothesisDraft(
      axis = HypothesisAxis.Initiative,
      claim = claim,
      supportSignals = signals(
        h.motifSignal,
        h.cpSignal,
        Option.when(h.practicalLow == "complex")("practical complexity is high")
      ),
      conflictSignals = signals(
        Option.when(h.cpGap.exists(_ >= 90))("initiative handoff is too costly"),
        Option.when(h.candidate.whyNot.nonEmpty)("existing refutation note points to initiative drift")
      ),
      baseConfidence = 0.5,
      horizon = HypothesisHorizon.Short
    )
  }

  private def conversionHypothesisDraft(h: HypothesisDraftContext): HypothesisDraft =
    HypothesisDraft(
      axis = HypothesisAxis.Conversion,
      claim =
        if h.conversionWindow then
          s"${h.move} frames simplification as a timing problem: exchanging too early or too late can change the sampled line."
        else
          s"${h.move} keeps simplification deferred, prioritizing coordination before exchanges.",
      supportSignals = signals(
        Option.when(h.conversionWindow)("evaluation marks a possible simplification checkpoint"),
        Option.when(h.candidate.tags.contains(CandidateTag.Converting))("candidate tagged as converting")
      ),
      conflictSignals = signals(
        Option.when(h.practicalLow == "complex")("line remains tactically demanding after simplification"),
        Option.when(h.cpGap.exists(_ >= 110))("simplification route loses too much objective value")
      ),
      baseConfidence = if h.conversionWindow then 0.53 else 0.42,
      horizon = HypothesisHorizon.Medium
    )

  private def kingSafetyHypothesisDraft(h: HypothesisDraftContext): HypothesisDraft =
    HypothesisDraft(
      axis = HypothesisAxis.KingSafety,
      claim =
        if h.hasKingSignal then
          s"${h.move} alters king-safety tempo, so defensive coordination must stay synchronized with the next forcing move."
        else
          s"${h.move} keeps king safety mostly stable, but only if move order avoids loose tempos.",
      supportSignals = signals(
        Option.when(h.hasKingSignal)("threat or tactical alert points to king safety"),
        Option.when(h.tacticalAlert.contains("check"))("candidate alert includes check geometry")
      ),
      conflictSignals = signals(
        Option.when(h.whyNot.contains("king"))("candidate rationale already flags king safety issues"),
        Option.when(h.cpGap.exists(_ >= 140))("engine punishes resulting king exposure")
      ),
      baseConfidence = if h.hasKingSignal then 0.55 else 0.4,
      horizon = HypothesisHorizon.Short
    )

  private def pieceCoordinationHypothesisDraft(h: HypothesisDraftContext): HypothesisDraft =
    HypothesisDraft(
      axis = HypothesisAxis.PieceCoordination,
      claim =
        knightRouteShiftClaim(h.topCandidate, h.candidate).getOrElse(
          s"${h.move} changes piece coordination lanes, with activity gains balanced against route efficiency."
        ),
      supportSignals = signals(
        Option.when(h.isKnightRouteShift)("knight development route diverges from main line"),
        Option.when(isPieceMove(h.candidate.move))("piece move directly changes coordination map"),
        Option.when(h.alignmentLow.contains("development") || h.alignmentLow.contains("activation"))("intent is coordination-led")
      ),
      conflictSignals = signals(
        Option.when(h.cpGap.exists(_ >= 100))("coordination route is slower than principal line")
      ),
      baseConfidence = if h.isKnightRouteShift then 0.6 else 0.47,
      horizon = HypothesisHorizon.Medium
    )

  private def pawnBreakHypothesisDraft(h: HypothesisDraftContext): HypothesisDraft = {
    val claim =
      h.strategicFrame
        .map(frame => strategicMiddlegameClaim(HypothesisAxis.PawnBreakTiming, h.move, frame, h.localSeed ^ 0x1f123bb5))
        .getOrElse {
          if h.breakReady && isLikelyPawnMove(h.move) then
            NarrativeLexicon.pick(h.localSeed ^ 0x1f123bb5, List(
              s"${h.move} clarifies pawn tension immediately, preferring direct break resolution over extra preparation.",
              s"${h.move} commits to immediate break clarification, accepting concrete consequences now instead of waiting.",
              s"${h.move} resolves the pawn-break question at once, choosing concrete timing over additional setup.",
              s"${h.move} forces the break decision now, so follow-up accuracy matters more than setup completeness.",
              s"${h.move} brings pawn tension to a concrete verdict immediately rather than extending preparation."
            ))
          else if h.breakReady then
            NarrativeLexicon.pick(h.localSeed ^ 0x4e67c6a7, List(
              s"${h.move} keeps the break in reserve and improves support before committing.",
              s"${h.move} postpones the break by one phase, aiming for stronger piece support first.",
              s"${h.move} holds pawn tension for now, preparing better support before release.",
              s"${h.move} delays direct break action so supporting pieces can coordinate first.",
              s"${h.move} keeps break timing deferred, prioritizing support links before commitment."
            ))
          else
            NarrativeLexicon.pick(h.localSeed ^ 0x3c79ac49, List(
              s"${h.move} keeps break timing flexible, so central tension can be revisited under better conditions.",
              s"${h.move} preserves pawn-break optionality, leaving central tension unresolved for a later moment.",
              s"${h.move} avoids forcing a break now, keeping the central lever available for a better window.",
              s"${h.move} keeps the break decision open, waiting for clearer support and fewer tactical liabilities.",
              s"${h.move} maintains tension without immediate release, aiming to choose the break after more development."
            ))
        }
    HypothesisDraft(
      axis = HypothesisAxis.PawnBreakTiming,
      claim = claim,
      supportSignals = signals(
        h.breakFile.map(f => s"$f-file break is available"),
        Option.when(h.breakImpact >= 150)("break impact is materially relevant"),
        Option.when(h.ctx.pawnAnalysis.exists(_.tensionPolicy == TensionPolicy.Maintain))("current policy prefers tension maintenance")
      ),
      conflictSignals = signals(
        Option.when(h.cpGap.exists(_ >= 90) && h.breakReady)("timing choice concedes evaluation too early"),
        Option.when(h.candidate.whyNot.exists(_.toLowerCase.contains("tempo")))("candidate rationale flags timing cost")
      ),
      baseConfidence = if h.breakReady then 0.56 else 0.41,
      horizon = if h.breakReady then HypothesisHorizon.Short else HypothesisHorizon.Medium
    )
  }

  private def endgameHypothesisDraft(h: HypothesisDraftContext): HypothesisDraft = {
    val claim =
      if h.data.phase == "endgame" || h.candidate.tags.contains(CandidateTag.Converting) then
        NarrativeLexicon.pick(h.localSeed ^ 0x2f6e2b1, List(
          s"${h.move} influences the late-game direction by prioritizing activity over static structure.",
          s"${h.move} points the game toward a simplified position where active piece routes still need checking.",
          s"${h.move} tilts the future ending toward activity, with fixed structure still unresolved.",
          s"${h.move} frames the late phase as a coordination problem, emphasizing activity over static anchors."
        ))
      else
        NarrativeLexicon.pick(h.localSeed ^ 0x19f8b4ad, List(
          s"${h.move} keeps the endgame trajectory open, with the long-term direction depending on later simplification choices.",
          s"${h.move} defers the final trajectory choice, so the endgame direction depends on which simplification arrives first.",
          s"${h.move} preserves multiple late-game paths, and the practical difference depends on future simplification timing.",
          s"${h.move} leaves the ending map unresolved for now, with long-term value still tied to subsequent exchanges."
        ))
    HypothesisDraft(
      axis = HypothesisAxis.EndgameTrajectory,
      claim = claim,
      supportSignals = signals(
        Option.when(h.data.phase == "endgame")("position is already in endgame phase"),
        Option.when(h.data.endgameFeatures.isDefined)("endgame feature signal is available"),
        Option.when(h.candidate.tags.contains(CandidateTag.Converting))("candidate carries conversion tag")
      ),
      conflictSignals = signals(
        Option.when(h.cpGap.exists(_ >= 120))("long-term trajectory is objectively inferior"),
        Option.when(h.practicalLow == "complex" && h.data.phase == "endgame")("late-game handling remains line-dependent")
      ),
      baseConfidence = if h.data.endgameFeatures.isDefined then 0.52 else 0.4,
      horizon = HypothesisHorizon.Long
    )
  }

  private def strategicMiddlegameClaim(
    axis: HypothesisAxis,
    move: String,
    frame: ProbeDetector.StrategicFrame,
    seed: Int
  ): String = {
    val focus = strategicAxisFocus(axis)
    List(
      strategicFrameCauseSentence(move, frame.cause, focus, seed),
      strategicFrameConsequenceSentence(frame.consequence, focus, seed),
      strategicFrameTurningPointSentence(frame.turningPoint, seed)
    ).mkString(" ")
  }

  private def strategicFrameCauseSentence(
    move: String,
    cause: String,
    focus: String,
    seed: Int
  ): String =
    cause match
      case "near-baseline" =>
        NarrativeLexicon.pick(seed ^ 0x11f17f1d, List(
          s"Probe evidence keeps **$move** near baseline, so $focus remains a viable route.",
          s"With **$move**, probe feedback stays close to baseline and keeps $focus practical.",
          s"Baseline proximity in probe lines suggests **$move** can sustain $focus."
        ))
      case "manageable-concession" =>
        NarrativeLexicon.pick(seed ^ 0x517cc1b7, List(
          s"Probe data shows **$move** as a manageable concession, so $focus timing must stay accurate.",
          s"A manageable probe concession appears after **$move**, which makes $focus sequencing critical.",
          s"Probe checks rate **$move** as manageable, but $focus cannot afford drift."
        ))
      case _ =>
        NarrativeLexicon.pick(seed ^ 0x4f6cdd1d, List(
          s"Probe data marks **$move** as a forcing swing, so $focus errors are punished immediately.",
          s"A forcing swing appears after **$move** in probe lines, narrowing the $focus margin for error.",
          s"Probe checks flag **$move** as a forcing swing, so $focus precision is mandatory."
        ))

  private def strategicFrameConsequenceSentence(
    consequence: String,
    focus: String,
    seed: Int
  ): String =
    consequence match
      case "structural-collapse" =>
        NarrativeLexicon.pick(seed ^ 0x63d5a6f1, List(
          s"The consequence is rapid structural concession, and $focus options become harder to recover.",
          s"Under pressure, structure can collapse and $focus plans lose flexibility.",
          s"Once structure loosens, $focus choices become reactive rather than planned."
        ))
      case "king-safety-exposure" =>
        NarrativeLexicon.pick(seed ^ 0x5f356495, List(
          s"The consequence is king-safety fragility, and $focus decisions must prioritize defensive coordination.",
          s"King exposure increases practical risk and forces $focus choices into defensive mode.",
          s"With king safety exposed, $focus plans are constrained by immediate defensive duties."
        ))
      case "conversion-cost" =>
        NarrativeLexicon.pick(seed ^ 0x6c6c6c6c, List(
          s"The consequence is higher follow-up cost, so $focus gains are harder to realize cleanly.",
          s"This route raises the coordination burden, and $focus edges are harder to stabilize.",
          s"Follow-up handling gets heavier, making $focus execution less efficient."
        ))
      case _ =>
        NarrativeLexicon.pick(seed ^ 0x2f6e2b1, List(
          s"The consequence is a quick initiative flip, so $focus handling becomes reactive.",
          s"Initiative handoff risk rises, and $focus planning must absorb counterplay.",
          s"Momentum can transfer in one sequence, forcing $focus into damage control."
        ))

  private def strategicFrameTurningPointSentence(turningPoint: String, seed: Int): String =
    turningPoint match
      case "immediate-sequence" =>
        NarrativeLexicon.pick(seed ^ 0x19f8b4ad, List(
          "The critical test comes in the next forcing sequence.",
          "Immediate tactical sequencing will decide whether the plan survives.",
          "The route is judged in the very next concrete sequence."
        ))
      case "simplification-transition" =>
        NarrativeLexicon.pick(seed ^ 0x7f4a7c15, List(
          "The turning point arrives at the next simplification transition.",
          "Once exchanges begin, move-order timing determines whether the plan remains sound.",
          "Simplification choices are the key checkpoint for this route."
        ))
      case _ =>
        NarrativeLexicon.pick(seed ^ 0x2a2a2a2a, List(
          "The first serious middlegame regrouping decides the practical direction.",
          "This plan is tested at the next middlegame reorganization.",
          "The route is decided when middlegame regrouping commits both sides."
        ))

  private def strategicAxisFocus(axis: HypothesisAxis): String =
    axis match
      case HypothesisAxis.Plan            => "plan direction"
      case HypothesisAxis.Initiative      => "initiative control"
      case HypothesisAxis.PawnBreakTiming => "pawn-break timing"
      case _                              => "strategic coordination"

  private def rankHypothesisDraft(
    draft: HypothesisDraft,
    candidate: CandidateInfo,
    probeSignals: ProbeDetector.HypothesisVerificationSignals,
    rank: Option[Int],
    cpGap: Option[Int]
  ): Option[RankedHypothesis] = {
    val isLong = draft.horizon == HypothesisHorizon.Long
    val supportLimit = if isLong then 4 else 3
    val conflictLimit = if isLong then 3 else 2
    val support = rankedHypothesisSupport(draft, probeSignals, isLong, supportLimit)
    val conflict = rankedHypothesisConflict(draft, probeSignals, isLong, conflictLimit)
    val score = hypothesisDraftScore(draft, candidate, probeSignals, rank, cpGap, support, conflict)
    val card = rankedHypothesisCard(draft, support, conflict, supportLimit, conflictLimit, score)
    Option.when(card.claim.nonEmpty) {
      RankedHypothesis(card = card, score = score, family = hypothesisFamily(card))
    }
  }

  private def rankedHypothesisSupport(
    draft: HypothesisDraft,
    probeSignals: ProbeDetector.HypothesisVerificationSignals,
    isLong: Boolean,
    supportLimit: Int
  ): List[String] =
    if isLong then
      mergeSignalsPreferred(
        preferred = probeSignals.longSupportSignals,
        fallback = draft.supportSignals ++ probeSignals.supportSignals,
        maxCount = supportLimit
      )
    else
      normalizeSignals(draft.supportSignals ++ probeSignals.supportSignals, supportLimit)

  private def rankedHypothesisConflict(
    draft: HypothesisDraft,
    probeSignals: ProbeDetector.HypothesisVerificationSignals,
    isLong: Boolean,
    conflictLimit: Int
  ): List[String] =
    if isLong then
      mergeSignalsPreferred(
        preferred = probeSignals.longConflictSignals,
        fallback = draft.conflictSignals ++ probeSignals.conflictSignals,
        maxCount = conflictLimit
      )
    else
      normalizeSignals(draft.conflictSignals ++ probeSignals.conflictSignals, conflictLimit)

  private def hypothesisDraftScore(
    draft: HypothesisDraft,
    candidate: CandidateInfo,
    probeSignals: ProbeDetector.HypothesisVerificationSignals,
    rank: Option[Int],
    cpGap: Option[Int],
    support: List[String],
    conflict: List[String]
  ): Double = {
    val supportWeight = support.size * 0.17
    val conflictWeight = conflict.size * 0.16
    val consistencyBonus =
      probeSignals.consistencyBonus +
        (if rank.contains(1) then 0.08 else if cpGap.exists(_ <= 35) then 0.04 else 0.0) +
        (if candidate.whyNot.isEmpty then 0.03 else 0.0)
    val contradictionPenalty =
      probeSignals.contradictionPenalty +
        (if cpGap.exists(_ >= 140) then 0.1 else 0.0) +
        (if candidate.tags.contains(CandidateTag.TacticalGamble) then 0.05 else 0.0)
    val axisBias = hypothesisAxisBias(draft.axis)
    val isLong = draft.horizon == HypothesisHorizon.Long
    val longConfidenceAdjustment = if isLong then probeSignals.longConfidenceDelta else 0.0
    val longGapAdjustment = longHorizonGapAdjustment(isLong, cpGap)
    supportWeight - conflictWeight + consistencyBonus - contradictionPenalty + axisBias +
      longConfidenceAdjustment + longGapAdjustment
  }

  private def hypothesisAxisBias(axis: HypothesisAxis): Double =
    axis match
      case HypothesisAxis.Plan              => -0.06
      case HypothesisAxis.PieceCoordination => 0.06
      case HypothesisAxis.PawnBreakTiming   => 0.06
      case HypothesisAxis.KingSafety        => 0.05
      case HypothesisAxis.Initiative        => 0.04
      case HypothesisAxis.Structure         => 0.04
      case HypothesisAxis.Conversion        => 0.03
      case HypothesisAxis.EndgameTrajectory => 0.05

  private def longHorizonGapAdjustment(isLong: Boolean, cpGap: Option[Int]): Double =
    if isLong then
      if cpGap.exists(_ >= 120) then -0.04
      else if cpGap.exists(_ <= 40) then 0.02
      else 0.0
    else 0.0

  private def rankedHypothesisCard(
    draft: HypothesisDraft,
    support: List[String],
    conflict: List[String],
    supportLimit: Int,
    conflictLimit: Int,
    score: Double
  ): HypothesisCard = {
    val confidence = clampConfidence(draft.baseConfidence + score)
    HypothesisCard(
      axis = draft.axis,
      claim = draft.claim.trim,
      supportSignals = support.take(supportLimit),
      conflictSignals = conflict.take(conflictLimit),
      confidence = confidence,
      horizon = draft.horizon
    )
  }

  private def clampConfidence(v: Double): Double =
    Math.max(0.18, Math.min(0.93, v))

  private def normalizeSignals(signals: List[String], maxCount: Int): List[String] =
    signals.map(_.trim).filter(_.nonEmpty).distinct.take(maxCount)

  private def mergeSignalsPreferred(
    preferred: List[String],
    fallback: List[String],
    maxCount: Int
  ): List[String] =
    normalizeSignals(preferred ++ fallback, maxCount)

  private def applyLongHorizonProtection(
    selected: List[RankedHypothesis],
    diversified: List[RankedHypothesis],
    probeSignals: ProbeDetector.HypothesisVerificationSignals
  ): List[RankedHypothesis] =
    if probeSignals.longConfidenceDelta <= 0.0 then diversified
    else if diversified.isEmpty || diversified.exists(_.card.horizon == HypothesisHorizon.Long) then diversified
    else
      val primary = diversified.head
      val secondaryScore = diversified.drop(1).headOption.map(_.score).getOrElse(primary.score)
      val threshold = secondaryScore - 0.05
      val longPool =
        selected.filter { rh =>
          rh.card.horizon == HypothesisHorizon.Long &&
          !diversified.contains(rh) &&
          rh.score >= threshold
        }
      val promoted =
        longPool.find(_.card.axis != primary.card.axis).orElse(longPool.headOption)
      promoted match
        case Some(longCandidate) => List(primary, longCandidate).take(2)
        case None                => diversified

  private def hypothesisFamily(card: HypothesisCard): String =
    val stem = normalizeHypothesisStem(card.claim)
    s"${card.axis.toString.toLowerCase}:$stem"

  private def normalizeHypothesisStem(text: String): String =
    Option(text).getOrElse("")
      .toLowerCase
      .replaceAll("""\*\*[^*]+\*\*""", " ")
      .replaceAll("""\([^)]*\)""", " ")
      .replaceAll("""\b\d+(?:\.\d+)?\b""", " ")
      .replaceAll("""[^a-z\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .split(" ")
      .filter(_.nonEmpty)
      .take(5)
      .mkString(" ")

  private def humanPlan(raw: String): String =
    Option(raw).getOrElse("")
      .replaceAll("""[_\-]+""", " ")
      .trim
      .toLowerCase match
      case "" => "the current setup"
      case x  => x

  private def variedPlanScoreSignal(score: Double, seed: Int): String =
    val s = f"$score%.2f"
    NarrativeLexicon.pick(seed, List(
      s"plan table confidence $s",
      s"primary plan score sits at $s",
      s"plan match score registers $s",
      s"plan-priority signal is $s"
    ))

  private def variedEngineRankSignal(rank: Int, seed: Int): String =
    NarrativeLexicon.pick(seed, List(
      s"engine ordering keeps this at rank $rank",
      s"sampled line rank is $rank",
      s"engine list position is $rank",
      s"principal-variation rank reads $rank"
    ))

  private def isLikelyPawnMove(move: String): Boolean =
    Option(move).getOrElse("").trim.matches("""^[a-h](?:x[a-h])?[1-8](?:=[QRBN])?[+#]?$""")

  private def isPieceMove(move: String): Boolean =
    Option(move).getOrElse("").trim.headOption.exists(ch => "KQRBN".contains(ch))

  private def knightRouteShift(main: CandidateInfo, alt: CandidateInfo): Boolean =
    isKnightMove(main.move) &&
      isKnightMove(alt.move) &&
      main.uci.exists(mu => alt.uci.exists(au => !NarrativeUtils.uciEquivalent(mu, au)))

  private def isKnightMove(move: String): Boolean =
    Option(move).getOrElse("").trim.startsWith("N")

  private def knightRouteShiftClaim(
    main: Option[CandidateInfo],
    candidate: CandidateInfo
  ): Option[String] = {
    val candUci = candidate.uci.getOrElse("")
    main.flatMap(_.uci).flatMap { mainUci =>
      Option.when(
        isKnightMove(candidate.move) &&
          main.exists(m => isKnightMove(m.move)) &&
          !NarrativeUtils.uciEquivalent(mainUci, candUci)
      ) {
        val candDestFile = Option.when(candUci.length >= 4)(candUci.charAt(2))
        val mainDestFile = Option.when(mainUci.length >= 4)(mainUci.charAt(2))
        val effects = scala.collection.mutable.ListBuffer[String]()
        if candDestFile.contains('e') || mainDestFile.contains('e') then effects += "c-pawn flexibility"
        if List(candDestFile, mainDestFile).flatten.exists(f => f == 'c' || f == 'd' || f == 'e' || f == 'f') then
          effects += "central tension timing"
        if List(candDestFile, mainDestFile).flatten.exists(f => f == 'f' || f == 'g' || f == 'h') then
          effects += "kingside safety tempo"
        val effectText =
          if effects.nonEmpty then effects.distinct.mkString(", ")
          else "piece-coordination timing"
        s"${candidate.move} selects a different knight route, shifting $effectText."
      }
    }
  }

  private def buildErrorClassification(data: ExtendedAnalysisData): Option[ErrorClassification] = {
    data.counterfactual.flatMap { cf =>
      if (cf.cpLoss < 50) None
      else {
        // Tactical motifs (excludes generic Capture to prevent over-detection)
        val hasTacticalMissedMotif = cf.missedMotifs.exists {
          case _: Motif.Fork => true
          case _: Motif.Pin => true
          case _: Motif.Skewer => true
          case _: Motif.DiscoveredAttack => true
          case _: Motif.Check => true
          case _: Motif.DoubleCheck => true
          case c: Motif.Capture => c.captureType == Motif.CaptureType.Winning
          case _ => false
        }
        
        val isTactical = cf.cpLoss >= 200 && (cf.severity == "blunder" || hasTacticalMissedMotif)
        val missedMotifNames = cf.missedMotifs.map(_.getClass.getSimpleName.replace("$", ""))
        
        val errorSummary = if (isTactical) {
          s"전술(${cf.cpLoss}cp${if (missedMotifNames.nonEmpty) s", ${missedMotifNames.head}" else ""})"
        } else {
          s"포지셔널(${cf.cpLoss}cp)"
        }
        
        Some(ErrorClassification(
          isTactical = isTactical,
          missedMotifs = missedMotifNames,
          errorSummary = errorSummary
        ))
      }
    }
  }

  private def buildDivergence(data: ExtendedAnalysisData): Option[DivergenceInfo] = {
    data.counterfactual.map { cf =>
      DivergenceInfo(
        divergePly = data.ply,
        punisherMove = cf.userLine.moves.lift(1),  // Opponent's best reply to user's move
        branchPointFen = Some(data.fen)
      )
    }
  }

  private def buildChoiceType(ctx: IntegratedContext): ChoiceType = {
    ctx.classification.map { c =>
      c.choiceTopology.topologyType match {
        case ChoiceTopologyType.OnlyMove => ChoiceType.OnlyMove
        case ChoiceTopologyType.StyleChoice => ChoiceType.StyleChoice
        case ChoiceTopologyType.NarrowChoice => ChoiceType.NarrowChoice
      }
    }.getOrElse(ChoiceType.NarrowChoice)
  }

  private def buildTargets(data: ExtendedAnalysisData, ctx: IntegratedContext, probeResults: List[ProbeResult]): Targets = {
    Targets(
      tactical = finalizeTargets(tacticalTargetEntries(ctx, probeResults), 5),
      strategic = finalizeTargets(strategicTargetEntries(data, ctx), 5)
    )
  }

  private def tacticalTargetEntries(ctx: IntegratedContext, probeResults: List[ProbeResult]): List[TargetEntry] =
    threatTargetEntries(ctx.threatsToThem, "Attack") ++
      threatTargetEntries(ctx.threatsToUs, "Defend") ++
      refutedProbeTargetEntries(probeResults)

  private def threatTargetEntries(analysis: Option[ThreatAnalysis], prefix: String): List[TargetEntry] =
    analysis.toList.flatMap { ta =>
      ta.threats.sortBy(-_.lossIfIgnoredCp).take(3).flatMap { threat =>
        threat.attackSquares.map { square =>
          TargetEntry(TargetSquare(square), s"$prefix: ${threat.kind}", ConfidenceLevel.Engine, 1)
        }
      }
    }

  private def refutedProbeTargetEntries(probeResults: List[ProbeResult]): List[TargetEntry] =
    probeResults
      .filter(_.deltaVsBaseline.abs >= 100)
      .flatMap { pr =>
        pr.probedMove
          .flatMap(chess.format.Uci.apply)
          .collect { case uci: chess.format.Uci.Move =>
            TargetEntry(TargetSquare(uci.dest.key), s"Refuted: ${pr.deltaVsBaseline}cp", ConfidenceLevel.Probe, 2)
          }
          .toList
      }

  private def strategicTargetEntries(data: ExtendedAnalysisData, ctx: IntegratedContext): List[TargetEntry] =
    positionalTargetEntries(data) ++
      structuralWeaknessTargetEntries(data) ++
      pawnAnalysisTargetEntries(ctx) ++
      planTargetEntries(data)

  private def positionalTargetEntries(data: ExtendedAnalysisData): List[TargetEntry] =
    data.positionalFeatures.flatMap {
      case PositionalTag.Outpost(sq, _) =>
        Some(TargetEntry(TargetSquare(sq.key), "Outpost", ConfidenceLevel.Heuristic, 3))
      case PositionalTag.OpenFile(f, _) =>
        Some(TargetEntry(TargetFile(f.char.toString), "Open file control", ConfidenceLevel.Heuristic, 3))
      case PositionalTag.WeakSquare(sq, _) =>
        Some(TargetEntry(TargetSquare(sq.key), "Weak square", ConfidenceLevel.Heuristic, 3))
      case _ => None
    }

  private def structuralWeaknessTargetEntries(data: ExtendedAnalysisData): List[TargetEntry] =
    data.structuralWeaknesses.flatMap { weakness =>
      weakness.squares.map { square =>
        TargetEntry(TargetSquare(square.key), s"Weak complex (${weakness.color})", ConfidenceLevel.Heuristic, 3)
      }.toList
    }

  private def pawnAnalysisTargetEntries(ctx: IntegratedContext): List[TargetEntry] =
    ctx.pawnAnalysis.toList.flatMap { pa =>
      val blockade =
        pa.blockadeSquare.map { square =>
          TargetEntry(
            TargetSquare(square.key),
            s"Blockade${pa.blockadeRole.map(r => s" by ${r.name}").getOrElse("")}",
            ConfidenceLevel.Heuristic,
            3
          )
        }
      val breakLeverage =
        Option.when(pa.pawnBreakReady && pa.breakImpact >= 200)(pa.breakFile)
          .flatten
          .map(file => TargetEntry(TargetFile(file), "Break leverage", ConfidenceLevel.Heuristic, 3))
      blockade.toList ++ breakLeverage.toList
    }

  private def planTargetEntries(data: ExtendedAnalysisData): List[TargetEntry] =
    data.plans.flatMap { planMatch =>
      planMatch.evidence.flatMap { evidence =>
        extractRefFromMotif(evidence.motif).map { ref =>
          TargetEntry(ref, s"Plan: ${planMatch.plan.name}", ConfidenceLevel.Heuristic, 3)
        }
      }
    }

  private def finalizeTargets(entries: Seq[TargetEntry], limit: Int): List[TargetEntry] =
    entries
      .groupBy(_.ref.label)
      .values
      .map(_.sortBy(_.priority).head)
      .toList
      .sortBy(_.priority)
      .take(limit)

  private def extractRefFromMotif(motif: Motif): Option[TargetRef] = motif match {
    case m: Motif.Pin => m.pinnedSq.map(sq => TargetPiece(m.pinnedPiece.name, sq.key)).orElse(m.pinningSq.map(sq => TargetSquare(sq.key)))
    case m: Motif.Skewer => m.frontSq.map(sq => TargetPiece(m.frontPiece.name, sq.key)).orElse(m.attackingSq.map(sq => TargetSquare(sq.key)))
    case m: Motif.DiscoveredAttack => m.targetSq.map(sq => TargetSquare(sq.key))
    case m: Motif.Battery => m.frontSq.map(sq => TargetSquare(sq.key))
    case m: Motif.Fork => Some(TargetSquare(m.square.key))
    case m: Motif.IsolatedPawn => Some(TargetFile(m.file.char.toString))
    case m: Motif.DoubledPieces => Some(TargetFile(m.file.char.toString))
    case m: Motif.OpenFileControl => Some(TargetFile(m.file.char.toString))
    case _ => None
  }

  private def buildPlanConcurrency(data: ExtendedAnalysisData): PlanConcurrency = {
    val primary = data.plans.headOption.map(_.plan.name).getOrElse("None")
    val secondary = data.plans.lift(1).map(_.plan.name)
    
    val relationship = (data.plans.headOption, data.plans.lift(1)) match {
      case (Some(p1), Some(p2)) if p1.plan.category == p2.plan.category => "↔ synergy"
      case (Some(_), Some(_)) => "independent"
      case _ => "independent"
    }

    PlanConcurrency(primary, secondary, relationship)
  }

  private def buildSemanticSection(
      data: ExtendedAnalysisData,
      afterAnalysis: Option[ExtendedAnalysisData]
  ): Option[SemanticSection] = {
    val filteredPositional = semanticPositionalFeatures(data)
    val currentCompensation = currentSemanticCompensation(data)
    val afterCompensation = afterSemanticCompensation(data, afterAnalysis)

    if !hasSemanticSectionContent(data, filteredPositional, currentCompensation, afterCompensation) then None
    else Some(SemanticSection(
      structuralWeaknesses = data.structuralWeaknesses.map(convertWeakComplex),
      pieceActivity = data.pieceActivity.map(convertPieceActivity),
      positionalFeatures = filteredPositional.map(convertPositionalTag),
      compensation = currentCompensation,
      endgameFeatures = data.endgameFeatures.map(convertEndgame(_, data)),
      practicalAssessment = data.practicalAssessment.map(convertPractical),
      preventedPlans = data.preventedPlans.map(convertPreventedPlan(_, data.fen, data.ply)),
      conceptSummary = data.conceptSummary,
      structureProfile = data.structureProfile.map(convertStructureProfile),
      planAlignment = data.planAlignment.map(convertPlanAlignment),
      afterCompensation = afterCompensation
    ))
  }

  private def semanticPositionalFeatures(data: ExtendedAnalysisData): List[PositionalTag] =
    if isPawnEndgame(data) then
      data.positionalFeatures.filterNot {
        case _: lila.commentary.model.strategic.PositionalTag.OpenFile => true
        case _ => false
      }
    else data.positionalFeatures

  private def isPawnEndgame(data: ExtendedAnalysisData): Boolean =
    data.phase == "endgame" &&
      data.toContext.features.exists { features =>
        (features.materialPhase.whiteMaterial + features.materialPhase.blackMaterial) <= 6
      }

  private def currentSemanticCompensation(data: ExtendedAnalysisData): Option[CompensationInfo] =
    CompensationInterpretation
      .currentRawDecision(data)
      .filter(_.decision.accepted)
      .map(result => convertCompensation(result.compensation))

  private def afterSemanticCompensation(
    data: ExtendedAnalysisData,
    afterAnalysis: Option[ExtendedAnalysisData]
  ): Option[CompensationInfo] =
    afterAnalysis.flatMap { next =>
      CompensationInterpretation
        .afterRawDecision(data, next)
        .filter(_.decision.accepted)
        .map(result => convertCompensation(result.compensation))
    }

  private def hasSemanticSectionContent(
    data: ExtendedAnalysisData,
    filteredPositional: List[PositionalTag],
    currentCompensation: Option[CompensationInfo],
    afterCompensation: Option[CompensationInfo]
  ): Boolean =
    data.structuralWeaknesses.nonEmpty ||
      data.pieceActivity.nonEmpty ||
      filteredPositional.nonEmpty ||
      currentCompensation.isDefined ||
      afterCompensation.isDefined ||
      data.endgameFeatures.isDefined ||
      data.practicalAssessment.isDefined ||
      data.preventedPlans.nonEmpty ||
      data.conceptSummary.nonEmpty ||
      data.structureProfile.isDefined ||
      data.planAlignment.isDefined

  private def convertStructureProfile(sp: lila.commentary.model.structure.StructureProfile): StructureProfileInfo =
    StructureProfileInfo(
      primary = sp.primary.toString,
      confidence = sp.confidence,
      alternatives = sp.alternatives.map(_.toString),
      centerState = sp.centerState.toString,
      evidenceCodes = sp.evidenceCodes
    )

  private def convertPlanAlignment(pa: lila.commentary.model.structure.PlanAlignment): PlanAlignmentInfo =
    PlanAlignmentInfo(
      score = pa.score,
      band = pa.band.toString,
      matchedPlanIds = pa.matchedPlanIds,
      missingPlanIds = pa.missingPlanIds,
      reasonCodes = pa.reasonCodes,
      narrativeIntent = pa.narrativeIntent,
      narrativeRisk = pa.narrativeRisk,
      reasonWeights = pa.reasonWeights
    )

  private def convertWeakComplex(wc: WeakComplex): WeakComplexInfo = {
    val owner = wc.color.name.capitalize
    val distinctColors = wc.squares.map(_.isLight).distinct
    val squareColor =
      distinctColors match
        case Nil          => "mixed"
        case true :: Nil  => "light"
        case false :: Nil => "dark"
        case _            => "mixed"
    WeakComplexInfo(
      owner = owner,
      squareColor = squareColor,
      squares = wc.squares.map(_.key),
      isOutpost = wc.isOutpost,
      cause = wc.cause
    )
  }

  private def convertPieceActivity(pa: PieceActivity): PieceActivityInfo = {
    PieceActivityInfo(
      piece = pa.piece.name,
      square = pa.square.key,
      mobilityScore = pa.mobilityScore,
      isTrapped = pa.isTrapped,
      isBadBishop = pa.isBadBishop,
      keyRoutes = pa.keyRoutes.map(_.key),
      coordinationLinks = pa.coordinationLinks.map(_.key),
      directionalTargets = pa.directionalTargets.map(_.key)
    )
  }

  private def shouldBuildDecisionRationale(
      choiceType: String,
      candidates: List[CandidateInfo],
      probeResults: List[ProbeResult]
  ): Boolean =
    choiceType != "StyleChoice" || bestMoveProbe(candidates, probeResults).exists(hasMoveLinkedProbeEvidence)

  private def bestMoveProbe(
      candidates: List[CandidateInfo],
      probeResults: List[ProbeResult]
  ): Option[ProbeResult] =
    candidates.headOption.flatMap(candidate => probeResults.find(_.probedMove == candidate.uci))

  private def hasMoveLinkedProbeEvidence(probe: ProbeResult): Boolean =
    probe.futureSnapshot.exists(hasMoveLinkedFutureEvidence) ||
      probe.l1Delta.exists(hasMoveLinkedL1DeltaEvidence) ||
      probe.keyMotifs.nonEmpty

  private def hasMoveLinkedFutureEvidence(snapshot: FutureSnapshot): Boolean =
    snapshot.resolvedThreatKinds.nonEmpty ||
      snapshot.newThreatKinds.nonEmpty ||
      snapshot.targetsDelta.tacticalAdded.nonEmpty ||
      snapshot.targetsDelta.strategicAdded.nonEmpty ||
      snapshot.planBlockersRemoved.nonEmpty ||
      snapshot.planPrereqsMet.nonEmpty

  private def hasMoveLinkedL1DeltaEvidence(delta: L1DeltaSnapshot): Boolean =
    delta.materialDelta != 0 ||
      delta.kingSafetyDelta != 0 ||
      delta.centerControlDelta != 0 ||
      delta.openFilesDelta != 0 ||
      delta.mobilityDelta != 0 ||
      delta.collapseReason.exists(_.trim.nonEmpty)

  private def calculateDecisionRationale(
      data: ExtendedAnalysisData,
      ctx: IntegratedContext,
      candidates: List[CandidateInfo],
      semantic: Option[SemanticSection],
      targets: Targets,
      probeResults: List[ProbeResult]
  ): DecisionRationale = {
    val bestProbe = bestMoveProbe(candidates, probeResults)
    
    val delta = buildPVDelta(ctx, bestProbe)
    val focalPoint = identifyFocalPoint(targets, semantic, data.plans)
    
    val (logicSummary, confidence) = if (bestProbe.isDefined) {
      val solve = if (delta.resolvedThreats.nonEmpty) s"Resolves ${delta.resolvedThreats.head}" else "Maintains position"
      val gain = if (delta.newOpportunities.nonEmpty) s"creates pressure on ${delta.newOpportunities.head}" else "improves piece coordination"
      (s"$solve -> $gain", ConfidenceLevel.Probe)
    } else {
      val heuristic = focalPoint.map(fp => s"Focus on ${fp.label}").getOrElse("General improvement")
      (s"$heuristic (probe needed for validation)", ConfidenceLevel.Heuristic)
    }

    DecisionRationale(
      focalPoint = focalPoint,
      logicSummary = logicSummary,
      delta = delta,
      confidence = confidence
    )
  }

  private def buildPVDelta(ctx: IntegratedContext, bestProbe: Option[ProbeResult]): PVDelta = {
    bestProbe match {
      case None =>
        PVDelta(Nil, Nil, Nil, Nil)
        
      case Some(probe) if probe.futureSnapshot.isDefined =>
        val fs = probe.futureSnapshot.get
        val targetsDelta = fs.targetsDelta
        
        val newOps = (targetsDelta.tacticalAdded ++ targetsDelta.strategicAdded).take(2)
        
        val planAdv = (fs.planBlockersRemoved.map(b => s"Removed: $b") ++ 
                       fs.planPrereqsMet.map(p => s"Met: $p")).take(2)
        
        PVDelta(
          resolvedThreats = fs.resolvedThreatKinds.take(2),
          newOpportunities = if (newOps.nonEmpty) newOps else List("Improved position"),
          planAdvancements = planAdv,
          concessions = fs.newThreatKinds.take(1)
        )
        
      case Some(probe) =>
        val currentToUs = ctx.threatsToUs.map(_.threats.map(_.kind.toString)).getOrElse(Nil)
        val futureMotifs = probe.keyMotifs
        
        val resolved = currentToUs.filterNot(futureMotifs.contains)
        val concessions = futureMotifs.filter(m => m == "Mate" || m == "Material")
        
        val newOps = probe.l1Delta.map { d =>
          if (d.materialDelta > 0 || d.mobilityDelta > 10) List("Positional advantage") else Nil
        }.getOrElse(Nil)

        PVDelta(
          resolvedThreats = resolved.take(2),
          newOpportunities = newOps,
          planAdvancements = Nil,
          concessions = concessions.take(1)
        )
    }
  }

  private def identifyFocalPoint(
      targets: Targets,
      semantic: Option[SemanticSection],
      plans: List[PlanMatch]
  ): Option[TargetRef] = {
    val allTargets = targets.tactical ++ targets.strategic
    if (allTargets.isEmpty) return None

    val scores = allTargets.map { t =>
      var score = t.priority match { case 1 => 5; case 2 => 3; case _ => 1 }
      
      semantic.foreach { s =>
        if (s.positionalFeatures.exists(_.square.contains(t.ref.label))) score += 2
        if (s.pieceActivity.exists(_.square == t.ref.label)) score += 2
      }
      
      plans.foreach { pm =>
        if (pm.supports.exists(_.contains(t.ref.label))) score += 1
      }
      
      (t.ref, score)
    }

    scores.sortBy(-_._2).headOption.map(_._1)
  }

  private def convertPositionalTag(pt: PositionalTag): PositionalTagInfo = {
    pt match {
      case PositionalTag.Outpost(sq, c) =>
        PositionalTagInfo("Outpost", Some(sq.key), None, c.name)
      case PositionalTag.OpenFile(f, c) =>
        PositionalTagInfo("OpenFile", None, Some(f.char.toString), c.name)
      case PositionalTag.WeakSquare(sq, c) =>
        PositionalTagInfo("WeakSquare", Some(sq.key), None, c.name)
      case PositionalTag.LoosePiece(sq, _, c) =>
        PositionalTagInfo("LoosePiece", Some(sq.key), None, c.name)
      case PositionalTag.WeakBackRank(c) =>
        PositionalTagInfo("WeakBackRank", None, None, c.name)
      case PositionalTag.BishopPairAdvantage(c) =>
        PositionalTagInfo("BishopPairAdvantage", None, None, c.name)
      case PositionalTag.BadBishop(c) =>
        PositionalTagInfo("BadBishop", None, None, c.name)
      case PositionalTag.GoodBishop(c) =>
        PositionalTagInfo("GoodBishop", None, None, c.name)
      case PositionalTag.RookOnSeventh(c) =>
        PositionalTagInfo("RookOnSeventh", None, None, c.name)
      case PositionalTag.StrongKnight(sq, c) =>
        PositionalTagInfo("StrongKnight", Some(sq.key), None, c.name)
      case PositionalTag.SpaceAdvantage(c) =>
        PositionalTagInfo("SpaceAdvantage", None, None, c.name)
      case PositionalTag.OppositeColorBishops =>
        PositionalTagInfo("OppositeColorBishops", None, None, "Both")
      case PositionalTag.KingStuckCenter(c) =>
        PositionalTagInfo("KingStuckCenter", None, None, c.name)
      case PositionalTag.ConnectedRooks(c) =>
        PositionalTagInfo("ConnectedRooks", None, None, c.name)
      case PositionalTag.DoubledRooks(f, c) =>
        PositionalTagInfo("DoubledRooks", None, Some(f.char.toString), c.name)
      case PositionalTag.ColorComplexWeakness(c, sqColor, sqs) =>
        PositionalTagInfo("ColorComplexWeakness", None, None, c.name, Some(s"$sqColor squares: ${sqs.map(_.key).mkString(",")}"))
      case PositionalTag.PawnMajority(c, flank, count) =>
        PositionalTagInfo("PawnMajority", None, None, c.name, Some(s"$flank $count pawns"))
      case PositionalTag.MinorityAttack(c, flank) =>
        PositionalTagInfo("MinorityAttack", None, None, c.name, Some(s"$flank attack"))
      case PositionalTag.MateNet(c) =>
        PositionalTagInfo("MateNet", None, None, c.name)
      case PositionalTag.RemovingTheDefender(target, c) =>
        PositionalTagInfo("RemovingTheDefender", None, None, c.name, Some(target.name))
      case PositionalTag.Initiative(c) =>
        PositionalTagInfo("Initiative", None, None, c.name)
    }
  }

  private def convertCompensation(comp: Compensation): CompensationInfo = {
    CompensationInfo(
      investedMaterial = comp.investedMaterial,
      returnVector = comp.returnVector,
      expiryPly = comp.expiryPly,
      conversionPlan = comp.conversionPlan
    )
  }

  private def convertEndgame(ef: EndgameFeature, data: ExtendedAnalysisData): EndgameInfo = {
    EndgameInfo(
      hasOpposition = ef.hasOpposition,
      isZugzwang = ef.isZugzwang,
      keySquaresControlled = ef.keySquaresControlled.map(_.key),
      oppositionType = ef.oppositionType.toString,
      zugzwangLikelihood = ef.zugzwangLikelihood,
      ruleOfSquare = ef.ruleOfSquare.toString,
      triangulationAvailable = ef.triangulationAvailable,
      kingActivityDelta = ef.kingActivityDelta,
      rookEndgamePattern = ef.rookEndgamePattern.toString,
      theoreticalOutcomeHint = "Unclear",
      confidence = ef.confidence,
      primaryPattern = ef.primaryPattern,
      patternAge = data.endgamePatternAge,
      transition = data.endgameTransition.map(_.replaceAll("""\((?i:win|draw)\)""", "(Unclear)"))
    )
  }

  private def convertPractical(pa: PracticalAssessment): PracticalInfo = {
    PracticalInfo(
      engineScore = pa.engineScore,
      practicalScore = pa.practicalScore,
      verdict = pa.verdict,
      biasFactors =
        pa.biasFactors.map { bf =>
          PracticalBiasInfo(
            factor = bf.factor,
            description = bf.description,
            weight = bf.weight
          )
        }
    )
  }

  private def convertPreventedPlan(pp: PreventedPlan, fen: String, ply: Int): PreventedPlanInfo = {
    val citationLine =
      pp.sourceLine.flatMap { line =>
        LineScopedCitation.strategicCitation(fen, ply + 1, line)
      }
    PreventedPlanInfo(
      planId = pp.planId,
      deniedSquares = pp.deniedSquares.map(_.key),
      breakNeutralized = pp.breakNeutralized,
      mobilityDelta = pp.mobilityDelta,
      counterplayScoreDrop = pp.counterplayScoreDrop,
      preventedThreatType = pp.preventedThreatType,
      deniedResourceClass = pp.deniedResourceClass,
      deniedEntryScope = pp.deniedEntryScope,
      sourceScope = pp.sourceScope,
      citationLine = citationLine
    )
  }

  private def buildOpponentPlan(data: ExtendedAnalysisData, ctx: IntegratedContext): Option[PlanRow] = {
    val opponentColor = if (ctx.isWhiteToMove) chess.Color.Black else chess.Color.White
    val ctxOpp = buildOpponentContext(ctx)

    val planScoring = PlanMatcher.matchPlans(data.motifs, ctxOpp, opponentColor)
    planScoring.topPlans.headOption.map { p =>
      val isBlockadeEstablished = p.plan match {
        case _: lila.commentary.model.Plan.Blockade =>
          // Blockade is "established" if passerBlockade is true for our side (opponent's passer is blocked)
          ctx.pawnAnalysis.exists(_.passerBlockade)
        case _ => false
      }
      
      PlanRow(
        rank = 1,
        name = p.plan.name,
        score = p.score,
        evidence = p.evidence.map(_.description),
        isEstablished = isBlockadeEstablished
      )
    }
  }

  private def buildOpponentContext(ctx: IntegratedContext): IntegratedContext = {
    ctx.copy(
      threatsToUs = ctx.threatsToThem,
      threatsToThem = ctx.threatsToUs,
      pawnAnalysis = ctx.opponentPawnAnalysis,
      opponentPawnAnalysis = ctx.pawnAnalysis,
      isWhiteToMove = !ctx.isWhiteToMove
    )
  }
