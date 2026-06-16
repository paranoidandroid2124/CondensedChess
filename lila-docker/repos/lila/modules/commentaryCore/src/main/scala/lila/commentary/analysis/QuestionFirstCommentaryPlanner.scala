package lila.commentary.analysis


import lila.commentary.analysis.claim.*
import lila.commentary.analysis.semantic.RelationSurfaceRowKind
import lila.commentary.analysis.semantic.StrategicObservationIds.ProofFamilyId
import lila.commentary.{ MoveReviewRefs, StrategyPack }
import lila.commentary.model.*
import lila.commentary.model.authoring.*
import lila.commentary.model.strategic.CounterfactualMatch
import scala.annotation.unused

private[commentary] enum QuestionPlanFallbackMode:
  case PlannerOwned
  case DemotedToWhyThis
  case DemotedToWhatMustBeStopped
  case FactualFallback
  case Suppressed

private[commentary] enum QuestionPlanStrengthTier:
  case Strong
  case Moderate
  case Exact

private[commentary] enum QuestionPlanConsequenceBeat:
  case WrapUp
  case TeachingPoint

private[commentary] final case class QuestionPlanEvidence(
    text: String,
    purposes: List[String],
    sourceKinds: List[String],
    sourceIds: List[String] = Nil,
    branchScoped: Boolean = false
)

private[commentary] final case class QuestionPlanConsequence(
    text: String,
    beat: QuestionPlanConsequenceBeat,
    certified: Boolean = true
)

private[commentary] enum SceneType:
  case ForcingDefense
  case ConcreteTactical
  case LineConsequence
  case AlternativeComparison
  case QuietImprovement
  case TransitionConversion
  case PlanClash
  case OpeningRelation
  case EndgameTransition

  def wireName: String =
    this match
      case ForcingDefense      => "forcing_defense"
      case ConcreteTactical    => "concrete_tactical"
      case LineConsequence     => "line_consequence"
      case AlternativeComparison => "alternative_comparison"
      case QuietImprovement    => "quiet_improvement"
      case TransitionConversion => "transition_conversion"
      case PlanClash           => "plan_clash"
      case OpeningRelation     => "opening_relation"
      case EndgameTransition   => "endgame_transition"

private[commentary] enum PlannerOwnerKind:
  case ConcreteTactical
  case LineConsequence
  case AlternativeComparison
  case ForcingDefense
  case MoveDelta
  case PositionProbe
  case DecisionTiming
  case PlanRace
  case OpeningRelation
  case EndgameTransition

  def wireName: String =
    this match
      case ConcreteTactical  => "ConcreteTactical"
      case LineConsequence   => "LineConsequence"
      case AlternativeComparison => "AlternativeComparison"
      case ForcingDefense    => "ForcingDefense"
      case MoveDelta         => "MoveDelta"
      case PositionProbe     => "PositionProbe"
      case DecisionTiming    => "DecisionTiming"
      case PlanRace          => "PlanRace"
      case OpeningRelation   => "OpeningRelation"
      case EndgameTransition => "EndgameTransition"

private[commentary] enum OwnerCandidateMateriality:
  case OwnerCandidate
  case SupportMaterial

  def wireName: String =
    this match
      case OwnerCandidate  => "owner_candidate"
      case SupportMaterial => "support_material"

private[commentary] enum TimingSource:
  case DecisionComparison
  case CloseCandidate
  case PreventedResource
  case OnlyMove
  case RelationWitness

  def wireName: String =
    this match
      case DecisionComparison => "decision_comparison"
      case CloseCandidate     => "close_candidate"
      case PreventedResource  => "prevented_resource"
      case OnlyMove           => "only_move"
      case RelationWitness    => "relation_witness"

private[commentary] enum DecisionComparisonTimingDetail:
  case ConcreteReplyOrReason
  case BareEngineGap

  def wireName: String =
    this match
      case ConcreteReplyOrReason => "concrete_reply_or_reason"
      case BareEngineGap         => "bare_engine_gap"

private[commentary] enum AdmissionDecision:
  case PrimaryAllowed
  case SupportOnly
  case Demote
  case Forbidden

  def wireName: String =
    this match
      case PrimaryAllowed => "PrimaryAllowed"
      case SupportOnly    => "SupportOnly"
      case Demote         => "Demote"
      case Forbidden      => "Forbidden"

private[commentary] final case class OwnerCandidateTrace(
    plannerOwnerKind: PlannerOwnerKind,
    source: String,
    sourceKinds: List[String],
    questionKinds: List[AuthorQuestionKind],
    moveLinked: Boolean,
    materiality: OwnerCandidateMateriality,
    timingSource: Option[TimingSource],
    decisionComparisonTimingDetail: Option[DecisionComparisonTimingDetail],
    proposedOwnerMapping: String,
    reasons: List[String],
    admissionDecision: Option[AdmissionDecision] = None,
    admissionReason: Option[String] = None,
    demotedTo: Option[AuthorQuestionKind] = None
):
  def supportMaterial: Boolean =
    materiality == OwnerCandidateMateriality.SupportMaterial

  def key: (PlannerOwnerKind, String, OwnerCandidateMateriality, Option[TimingSource]) =
    (plannerOwnerKind, source, materiality, timingSource)

  def render: String =
    (
      List(
        plannerOwnerKind.wireName,
        s"source_kind=$source",
        s"materiality=${materiality.wireName}",
        s"move_linked=$moveLinked",
        s"support_material=$supportMaterial",
        s"mapping=$proposedOwnerMapping",
        s"source_kinds=${sourceKinds.distinct.sorted.mkString("+")}",
        s"questions=${questionKinds.map(_.toString).distinct.sorted.mkString("+")}"
      ) ++
        timingSource.toList.map(source => s"timing_source=${source.wireName}") ++
        decisionComparisonTimingDetail.toList.map(detail =>
          s"decision_comparison_detail=${detail.wireName}"
        ) ++
        admissionDecision.toList.map(decision => s"admission_decision=${decision.wireName}") ++
        admissionReason.toList.map(reason => s"admission_reason=$reason") ++
        demotedTo.toList.map(kind => s"demoted_to=${kind.toString}") ++
        List(reasons.distinct.sorted.mkString("+"))
    ).filter(_.nonEmpty).mkString(":")

private[commentary] final case class DroppedPlannerOwnerTrace(
    plannerOwnerKind: PlannerOwnerKind,
    source: String,
    reasons: List[String],
    questionKinds: List[AuthorQuestionKind]
):
  def render: String =
    List(
      plannerOwnerKind.wireName,
      source,
      questionKinds.map(_.toString).distinct.sorted.mkString("+"),
      reasons.distinct.sorted.mkString("+")
    ).filter(_.nonEmpty).mkString(":")

private[commentary] final case class PlannerOwnerTrace(
    sceneType: SceneType = SceneType.QuietImprovement,
    sceneReasons: List[String] = Nil,
    ownerCandidates: List[OwnerCandidateTrace] = Nil,
    admittedPlannerOwners: List[OwnerCandidateTrace] = Nil,
    droppedPlannerOwners: List[DroppedPlannerOwnerTrace] = Nil,
    demotionReasons: List[String] = Nil,
    selectedQuestion: Option[AuthorQuestionKind] = None,
    selectedPlannerOwnerKind: Option[PlannerOwnerKind] = None,
    selectedPlannerSource: Option[String] = None
):
  def ownerCandidateLabels: List[String] =
    ownerCandidates.map(_.render)

  def admittedPlannerOwnerLabels: List[String] =
    admittedPlannerOwners.map(_.render)

  def droppedPlannerOwnerLabels: List[String] =
    droppedPlannerOwners.map(_.render)

  def supportMaterialSeparationLabels: List[String] =
    ownerCandidates.map(candidate =>
      s"${candidate.source}:${candidate.materiality.wireName}" +
        candidate.timingSource.fold("")(source => s":timing_source=${source.wireName}") +
        candidate.decisionComparisonTimingDetail.fold("")(detail =>
          s":decision_comparison_detail=${detail.wireName}"
        )
    )

  def proposedOwnerMappingLabels: List[String] =
    ownerCandidates.map(candidate =>
      s"${candidate.source}->${candidate.plannerOwnerKind.wireName}:${candidate.proposedOwnerMapping}:${candidate.materiality.wireName}" +
        candidate.timingSource.fold("")(source => s":timing_source=${source.wireName}") +
        candidate.decisionComparisonTimingDetail.fold("")(detail =>
          s":decision_comparison_detail=${detail.wireName}"
        )
    )

private[commentary] final case class QuestionPlanTimingWitness(
    proofFamily: String,
    source: String,
    namedBreak: Option[String] = None,
    continuationMove: Option[String] = None,
    branchKey: Option[String] = None,
    witnessTokens: List[String] = Nil
)

private[commentary] final case class QuestionPlan(
    questionId: String,
    questionKind: AuthorQuestionKind,
    priority: Int,
    claim: String,
    evidence: Option[QuestionPlanEvidence],
    contrast: Option[String],
    consequence: Option[QuestionPlanConsequence],
    fallbackMode: QuestionPlanFallbackMode,
    strengthTier: QuestionPlanStrengthTier,
    sourceKinds: List[String],
    admissibilityReasons: List[String],
    plannerOwnerKind: PlannerOwnerKind,
    plannerSource: String,
    prefixKind: PlayerFacingClaimPrefixKind = PlayerFacingClaimPrefixKind.None,
    demotionReasons: List[String] = Nil,
    timingWitness: Option[QuestionPlanTimingWitness] = None
)

private[commentary] final case class RejectedQuestionPlan(
    questionId: String,
    questionKind: AuthorQuestionKind,
    fallbackMode: QuestionPlanFallbackMode,
    reasons: List[String],
    demotedTo: Option[AuthorQuestionKind] = None,
    demotionReasons: List[String] = Nil
)

private[commentary] final case class RankedQuestionPlans(
    primary: Option[QuestionPlan],
    secondary: Option[QuestionPlan],
    rejected: List[RejectedQuestionPlan],
    ownerTrace: PlannerOwnerTrace = PlannerOwnerTrace()
)

private[commentary] final case class SceneClassificationTrace(
    sceneType: SceneType,
    reasons: List[String]
)

private[commentary] final case class SceneClassificationSignals(
    families: Set[PlannerOwnerKind],
    hasTransitionAnchor: Boolean,
    hasTranslatorOverlap: Boolean,
    truthSignalsConversion: Boolean
):
  def contains(ownerKind: PlannerOwnerKind): Boolean =
    families.contains(ownerKind)

  def hasTransitionConversion: Boolean =
    hasTranslatorOverlap || truthSignalsConversion || hasTransitionAnchor

  def transitionReasons: List[String] =
    List(
      Option.when(hasTranslatorOverlap)("domain_transition_overlap"),
      Option.when(truthSignalsConversion)("truth_reason=Conversion"),
      Option.when(hasTransitionAnchor)("pv_delta_transition_anchor")
    ).flatten

private[commentary] final case class AdmissionOutcome(
    decision: AdmissionDecision,
    reason: String,
    demotedTo: Option[AuthorQuestionKind] = None
)

private[commentary] final case class PvCoupledPlanSupport(
    planName: String,
    playedSan: String,
    evidenceLine: String,
    planAnchorLine: Option[String] = None,
    anchorTokens: List[String] = Nil,
    matchedAnchorTokens: List[String] = Nil
):
  def anchorMatched: Boolean =
    matchedAnchorTokens.nonEmpty

  def claim: String =
    if anchorMatched then
      val anchor = matchedAnchorTokens.take(2).mkString(" and ")
      s"The checked line from $playedSan keeps $planName connected to $anchor as a practical plan."
    else
      s"The checked line from $playedSan keeps a practical continuation available, but it does not yet prove $planName."

private[commentary] final case class QuestionPlannerInputs(
    mainBundle: Option[MainPathClaimBundle],
    quietIntent: Option[QuietMoveIntentClaim],
    decisionFrame: CertifiedDecisionFrame,
    decisionComparison: Option[DecisionComparison],
    alternativeNarrative: Option[AlternativeNarrative],
    truthMode: PlayerFacingTruthMode,
    preventedPlansNow: List[PreventedPlanInfo],
    pvDelta: Option[PVDelta],
    counterfactual: Option[CounterfactualMatch],
    practicalAssessment: Option[PracticalInfo],
    opponentThreats: List[ThreatRow],
    forcingThreats: List[ThreatRow],
    evidenceByQuestionId: Map[String, List[QuestionEvidence]],
    candidateEvidenceLines: List[String],
    evidenceBackedPlans: List[PlanHypothesis],
    opponentPlan: Option[PlanRow],
    factualFallback: Option[String],
    heavyPieceLocalBindBlocked: Boolean = false,
    openingRelationClaim: Option[String] = None,
    endgameTransitionClaim: Option[String] = None,
    restrictedDefenseConversionSurface: Option[RestrictedDefenseConversionProof.Contract] = None,
    dualAxisBindSurface: Option[TwoAxisBindProof.Contract] = None,
    namedRouteNetworkSurface: Option[RouteNetworkBindProof.SurfaceNetwork] = None,
    pvCoupledPlanSupport: Option[PvCoupledPlanSupport] = None,
    lineConsequence: Option[LineConsequenceEvidence] = None,
    localFactResult: Option[MoveReviewExplanationBuilder.Result] = None,
    stageTimingsMs: Map[String, Long] = Map.empty
)

private[commentary] enum WhyNowTimingOwner:
  case Threat(threat: ThreatRow)
  case Prevented(plan: PreventedPlanInfo)
  case OnlyMove(reason: String, witness: Option[QuestionPlanTimingWitness])
  case RelationLocalFact(result: MoveReviewExplanationBuilder.Result)
  case DecisionComparisonOwner(comparison: DecisionComparison)

private[commentary] final case class WhyThisPlanMaterial(
    claim: String,
    evidenceSourceKinds: List[String],
    contrast: Option[String],
    consequence: Option[QuestionPlanConsequence],
    strengthTier: QuestionPlanStrengthTier,
    sourceKinds: List[String],
    plannerOwnerKind: PlannerOwnerKind,
    plannerSource: String,
    admissibilityReasons: List[String] = List("move_owner", "move_local_claim")
)

private[commentary] final case class PositionProbePlanMaterial(
    claim: MainPathScopedClaim,
    packet: PlayerFacingClaimPacket,
    decision: ClaimAuthorityDecision
):
  def supportedLocal: Boolean =
    decision.tier == ClaimAuthorityTier.SupportedLocal

  def plannerSource: String =
    packet.proofSource

  def strengthTier: QuestionPlanStrengthTier =
    if supportedLocal then QuestionPlanStrengthTier.Moderate else QuestionPlanStrengthTier.Strong

  def fallbackMode: QuestionPlanFallbackMode =
    if supportedLocal then QuestionPlanFallbackMode.FactualFallback
    else QuestionPlanFallbackMode.PlannerOwned

  def prefixKind: PlayerFacingClaimPrefixKind =
    if supportedLocal then PlayerFacingClaimPrefixKind.SupportedLocal else claim.prefixKind

  def sourceKinds: List[String] =
    List(claim.sourceKind, plannerSource).distinct

  def admissibilityReasons: List[String] =
    List("position_probe_owner", "current_position_truth") ++
      Option.when(decision.tier == ClaimAuthorityTier.CertifiedOwner)("certified_position_probe").toList ++
      Option.when(supportedLocal)("strategic_claim_supported_local").toList

private[commentary] final case class WhyNowPlanMaterial(
    claim: String,
    contrast: Option[String],
    consequence: Option[QuestionPlanConsequence],
    sourceKind: String,
    plannerOwnerKind: PlannerOwnerKind,
    supportSourceKinds: List[String] = Nil,
    supportReasons: List[String] = Nil
):
  def sourceKinds: List[String] = ("timing_reason" :: sourceKind :: supportSourceKinds).distinct

  def evidenceSourceKinds: List[String] = ("timing_proof" :: sourceKind :: supportSourceKinds).distinct

private[commentary] final case class WhatMustBeStoppedMaterial(
    urgentThreat: Option[ThreatRow],
    preventedPlan: Option[PreventedPlanInfo],
    claim: String,
    strongClaim: Boolean
):
  def plannerSource: String =
    if urgentThreat.nonEmpty then "threat" else "prevented_plan"

  def sourceKinds: List[String] =
    urgentThreat.toList.map(_ => "threat") ++ preventedPlan.toList.map(_ => "prevented_plan")

private[commentary] final case class WhosePlanRaceState(
    owner: String,
    intent: Option[CertifiedDecisionSupport],
    battlefront: Option[CertifiedDecisionSupport],
    opponentPlan: Option[PlanRow],
    opponentRace: Option[String],
    raceAnchor: Option[String],
    opponentPressureAvailable: Boolean,
    ownPlanAvailable: Boolean,
    intentReason: String
):
  def material: Option[WhosePlanRaceMaterial] =
    for
      readyIntent <- intent
      readyBattlefront <- battlefront
      readyOpponentRace <- opponentRace
      readyRaceAnchor <- raceAnchor
    yield
      WhosePlanRaceMaterial(
        owner = owner,
        intent = readyIntent,
        battlefront = readyBattlefront,
        opponentRace = readyOpponentRace,
        raceAnchor = readyRaceAnchor,
        opponentSourceKind = if opponentPlan.nonEmpty then "opponent_plan" else "opponent_threat",
        intentReason = intentReason
      )

  def onlyOpponentPressure: Boolean =
    opponentRace.nonEmpty && opponentPressureAvailable

private[commentary] final case class WhosePlanRaceMaterial(
    owner: String,
    intent: CertifiedDecisionSupport,
    battlefront: CertifiedDecisionSupport,
    opponentRace: String,
    raceAnchor: String,
    opponentSourceKind: String,
    intentReason: String
):
  def claim: String =
    s"$owner keeps the initiative ahead of $opponentRace because $raceAnchor."

  def contrast: String =
    s"${owner}'s plan is racing $opponentRace, and the battlefront is ${battlefront.sentence}"

  def consequenceText: String =
    s"That preserves ${owner.toLowerCase}'s plan window before $opponentRace catches up."

  def sourceKinds: List[String] =
    List(intent.sourceKind, battlefront.sourceKind, opponentSourceKind)

private[commentary] final case class WhatChangedPlannerMaterial(
    moveOwner: Option[MainPathScopedClaim],
    supportedLocalMoveOwner: Option[MainPathScopedClaim],
    allowedPreventedPlans: List[PreventedPlanInfo],
    exactTargetFixationChange: Option[String],
    canPromoteDecisionComparisonChange: Boolean,
    localFileEntryPair: Option[LocalFileEntryProof.SurfacePair],
    localFileEntryChange: Option[String],
    hasLocalFileEntryChange: Boolean,
    preventedPlanChange: Option[String],
    hasPreventedPlanChangeMaterial: Boolean,
    pvCoupledPlanSupport: Option[PvCoupledPlanSupport],
    lineConsequence: Option[LineConsequenceEvidence],
    lineConsequenceClaim: Option[String],
    lineConsequenceRiskGate: Boolean,
    localFactResult: Option[MoveReviewExplanationBuilder.Result],
    localFactChange: Option[String],
    decisionComparisonChange: Option[String],
    moveLinkedChange: Option[String]
):
  def supportedLocal: Boolean =
    supportedLocalMoveOwner.nonEmpty

  def hasMoveAttributedChange: Boolean =
    moveOwner.nonEmpty || moveLinkedChange.nonEmpty

private[commentary] object QuestionPlannerInputsBuilder:

  def build(
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract],
      candidateEvidenceLines: List[String] = Nil,
      refs: Option[MoveReviewRefs] = None
  ): QuestionPlannerInputs =
    val stageTimings = scala.collection.mutable.LinkedHashMap.empty[String, Long]
    def timeStage[A](stage: String)(body: => A): A =
      val started = System.nanoTime()
      try body
      finally
        val elapsed = ((System.nanoTime() - started) / 1000000L).max(0L)
        stageTimings.update(stage, stageTimings.getOrElse(stage, 0L) + elapsed)
    val decisionComparison =
      timeStage("sanitized_decision_comparison") {
        sanitizedDecisionComparison(ctx, truthContract)
      }
    val cleanedEvidenceLines =
      timeStage("planner_evidence_lines") {
        plannerEvidenceLines(ctx, candidateEvidenceLines, decisionComparison)
      }
    val truthMode =
      timeStage("truth_mode") {
        PlayerFacingTruthModePolicy.classify(ctx, strategyPack, truthContract)
      }
    val mainBundle =
      timeStage("main_path_move_delta_claim") {
        MainPathMoveDeltaClaimBuilder.build(
          ctx,
          strategyPack,
          truthContract,
          cleanedEvidenceLines,
          classifiedModeOverride = Some(truthMode)
        )
      }
    val comparativeDecisionComparison =
      timeStage("comparative_decision_comparison") {
        comparativeDecisionComparisonFrom(
          ctx = ctx,
          refs = refs,
          strategyPack = strategyPack,
          truthContract = truthContract,
          cleanedEvidenceLines = cleanedEvidenceLines,
          mainBundle = mainBundle,
          decisionComparison = decisionComparison
        )
      }
    val quietIntent =
      timeStage("quiet_intent") {
        quietIntentWhenNoMainBundle(ctx, cleanedEvidenceLines, mainBundle)
      }
    val decisionFrame =
      timeStage("decision_frame") {
        CertifiedDecisionFrameBuilder.build(
          ctx = ctx,
          strategyPack = strategyPack,
          truthContract = truthContract,
          mainBundle = mainBundle,
          quietIntent = quietIntent
        )
      }
    val preventedPlansNow =
      timeStage("prevented_plans_now") {
        currentPreventedPlans(ctx)
      }
    val evidenceBackedPlans =
      timeStage("evidence_backed_plans") {
        StrategicNarrativePlanSupport.evidenceBackedMainPlans(ctx)
      }
    val heavyPieceLocalBindBlocked =
      timeStage("heavy_piece_local_bind") {
        HeavyPieceLocalBindValidation.blocksPlayerFacingShell(ctx)
      }
    val namedRouteNetworkSurface =
      timeStage("named_route_network") {
        namedRouteNetworkSurfaceWhenAllowed(heavyPieceLocalBindBlocked, preventedPlansNow, evidenceBackedPlans)
      }
    val pvCoupledPlanSupport =
      timeStage("pv_coupled_plan_support") {
        playedMovePvCoupledPlanSupport(ctx, cleanedEvidenceLines)
      }
    val lineConsequence =
      timeStage("line_consequence_candidate") {
        lineConsequenceCandidate(ctx, refs, truthContract)
          .filter(_.kind != LineConsequenceKind.PreviewOnly)
      }
    val localFactResult =
      timeStage("local_fact_result") {
        MoveReviewExplanationBuilder
          .buildWithLocalFact(
            ctx,
            refs,
            truthContract,
            strategyPack,
            precomputedLineConsequence = lineConsequence
          )
          .filter(plannerLocalFactResultEligible)
      }
    val alternativeNarrative =
      timeStage("alternative_narrative") {
        AlternativeNarrativeSupport.build(ctx)
      }
    val factualFallback =
      timeStage("factual_fallback") {
        QuietMoveIntentBuilder.exactFactualSentence(ctx)
      }
    val openingRelationClaim =
      timeStage("opening_relation_claim") {
        QuestionFirstCommentaryPlanner.openingRelationReplayClaim(ctx)
      }
    val endgameTransitionClaim =
      timeStage("endgame_transition_claim") {
        QuestionFirstCommentaryPlanner.endgameTransitionReplayClaim(ctx)
      }

    QuestionPlannerInputs(
      mainBundle = mainBundle,
      quietIntent = quietIntent,
      decisionFrame = decisionFrame,
      decisionComparison = comparativeDecisionComparison,
      alternativeNarrative = alternativeNarrative,
      truthMode = truthMode,
      preventedPlansNow = preventedPlansNow,
      pvDelta = ctx.decision.map(_.delta),
      counterfactual = ctx.counterfactual,
      practicalAssessment = ctx.semantic.flatMap(_.practicalAssessment),
      opponentThreats = ctx.threats.toUs,
      forcingThreats = ctx.threats.toThem,
      evidenceByQuestionId = ctx.authorEvidence.groupBy(_.questionId),
      candidateEvidenceLines = cleanedEvidenceLines,
      evidenceBackedPlans = evidenceBackedPlans,
      opponentPlan = ctx.opponentPlan,
      factualFallback = factualFallback,
      heavyPieceLocalBindBlocked = heavyPieceLocalBindBlocked,
      openingRelationClaim = openingRelationClaim,
      endgameTransitionClaim = endgameTransitionClaim,
      restrictedDefenseConversionSurface = ctx.restrictedDefenseConversion.filter(_.certified),
      dualAxisBindSurface = ctx.dualAxisBind.filter(_.certified),
      namedRouteNetworkSurface = namedRouteNetworkSurface,
      pvCoupledPlanSupport = pvCoupledPlanSupport,
      lineConsequence = lineConsequence,
      localFactResult = localFactResult,
      stageTimingsMs = stageTimings.toMap
    )

  private def lineConsequenceCandidate(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      truthContract: Option[DecisiveTruthContract]
  ): Option[LineConsequenceEvidence] =
    LineConsequenceEvaluator.moveReviewCandidate(ctx, refs, truthContract)

  private def plannerLocalFactResultEligible(result: MoveReviewExplanationBuilder.Result): Boolean =
    val fact = result.localFact
    Set(
      MoveReviewLocalFact.Family.Attack,
      MoveReviewLocalFact.Family.Defense,
      MoveReviewLocalFact.Family.Threat,
      MoveReviewLocalFact.Family.Pressure,
      MoveReviewLocalFact.Family.PlanSupport,
      MoveReviewLocalFact.Family.Timing
    ).contains(fact.family) ||
      (
        fact.family == MoveReviewLocalFact.Family.LineConsequence &&
          fact.producer == MoveReviewLocalFact.Producer.LineConsequence &&
          result.explanation.source == "practical_central_challenge"
      )

  private def sanitizedDecisionComparison(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract]
  ): Option[DecisionComparison] =
    val rawComparison = DecisionComparisonBuilder.build(ctx)
    truthContract
      .flatMap(contract => DecisiveTruth.sanitizeDecisionComparison(rawComparison, contract))
      .orElse(rawComparison)

  private def plannerEvidenceLines(
      ctx: NarrativeContext,
      candidateEvidenceLines: List[String],
      decisionComparison: Option[DecisionComparison]
  ): List[String] =
    val authorEvidenceLines =
      ctx.authorEvidence.flatMap(_.branches).flatMap(branchDisplayLine)
    val comparisonEvidence = decisionComparison.flatMap(_.evidence).toList
    (candidateEvidenceLines ++ authorEvidenceLines ++ comparisonEvidence)
      .flatMap(cleanLine)
      .distinct

  private def comparativeDecisionComparisonFrom(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract],
      cleanedEvidenceLines: List[String],
      mainBundle: Option[MainPathClaimBundle],
      decisionComparison: Option[DecisionComparison]
  ): Option[DecisionComparison] =
    DecisionComparisonComparativeSupport.enrich(
      comparison = decisionComparison,
      ctx = ctx,
      refs = refs,
      strategyPack = strategyPack,
      truthContract = truthContract,
      candidateEvidenceLines = cleanedEvidenceLines,
      mainBundleOverride = mainBundle
    )

  private def quietIntentWhenNoMainBundle(
      ctx: NarrativeContext,
      cleanedEvidenceLines: List[String],
      mainBundle: Option[MainPathClaimBundle]
  ): Option[QuietMoveIntentClaim] =
    Option.when(mainBundle.isEmpty) {
      QuietMoveIntentBuilder.build(ctx, cleanedEvidenceLines)
    }.flatten

  private def currentPreventedPlans(ctx: NarrativeContext): List[PreventedPlanInfo] =
    ctx.semantic.toList.flatMap(_.preventedPlans).filter(_.sourceScope == FactScope.Now)

  private def namedRouteNetworkSurfaceWhenAllowed(
      heavyPieceLocalBindBlocked: Boolean,
      preventedPlansNow: List[PreventedPlanInfo],
      evidenceBackedPlans: List[PlanHypothesis]
  ): Option[RouteNetworkBindProof.SurfaceNetwork] =
    Option.unless(heavyPieceLocalBindBlocked) {
      RouteNetworkBindProof.certifiedSurfaceNetwork(
        preventedPlans = preventedPlansNow,
        evidenceBackedPlans = evidenceBackedPlans
      )
    }.flatten

  private def playedMovePvCoupledPlanSupport(
      ctx: NarrativeContext,
      cleanedEvidenceLines: List[String]
  ): Option[PvCoupledPlanSupport] =
    for
      playedSan <- ctx.playedSan.flatMap(cleanLine)
      line <- cleanedEvidenceLines.find(line => lineStartsWithPlayedMove(line, playedSan))
      plan <- ctx.strategicPlanEvidence.pvCoupledPlans
        .flatMap(plan => pvCoupledPlanSupportCandidate(ctx, plan, playedSan, line, cleanedEvidenceLines))
        .sortBy { case (support, rank) => (if support.anchorMatched then 0 else 1, rank._1, rank._2) }
        .map(_._1)
        .headOption
    yield plan

  private def pvCoupledPlanSupportCandidate(
      ctx: NarrativeContext,
      plan: PlanEvidenceEvaluator.EvaluatedPlan,
      playedSan: String,
      evidenceLine: String,
      cleanedEvidenceLines: List[String]
  ): Option[(PvCoupledPlanSupport, (Int, Int))] =
    cleanLine(plan.hypothesis.planName).map { planName =>
      val anchorLine = planAnchorLine(planName, cleanedEvidenceLines)
      val anchorTokens = anchorLine.toList.flatMap(LineScopedCitation.concreteSanTokens).distinct
      val lineTokens = LineScopedCitation.concreteSanTokens(evidenceLine)
      val matched =
        anchorTokens
          .filterNot(token => sameSanToken(token, playedSan))
          .filter(token => lineTokens.exists(lineToken => sameSanToken(lineToken, token)))
          .distinct
      PvCoupledPlanSupport(
        planName = planName,
        playedSan = playedSan,
        evidenceLine = evidenceLine,
        planAnchorLine = anchorLine,
        anchorTokens = anchorTokens,
        matchedAnchorTokens = matched
      ) -> pvCoupledPlanSupportRank(ctx, plan)
    }

  private def planAnchorLine(planName: String, lines: List[String]): Option[String] =
    val planTerms = significantPlanTerms(planName)
    lines.find { line =>
      val normalized = normalizePlanText(line)
      normalized.startsWith("further probe work still targets ") &&
        planTerms.exists(term => normalized.contains(term))
    }

  private def significantPlanTerms(planName: String): List[String] =
    normalizePlanText(planName)
      .split("\\s+")
      .toList
      .filter(token => token.length >= 4 && !Set("with", "this", "that", "plan").contains(token))
      .distinct

  private def normalizePlanText(raw: String): String =
    Option(raw)
      .getOrElse("")
      .toLowerCase
      .replaceAll("""[^a-z0-9+#=\s]+""", " ")
      .replaceAll("\\s+", " ")
      .trim

  private def pvCoupledPlanSupportRank(
      ctx: NarrativeContext,
      plan: PlanEvidenceEvaluator.EvaluatedPlan
  ): (Int, Int) =
    val text =
      normalizeSanToken(
        List(
          plan.hypothesis.planName,
          plan.hypothesis.themeL1,
          plan.hypothesis.subplanId.getOrElse("")
        ).mkString(" ")
      )
    val playedIsCapture = ctx.playedSan.exists(_.contains("x"))
    val tacticalPlan =
      List("attack", "counterplay", "tactical", "forcing", "initiative", "gain", "exchange").exists(text.contains)
    val restraintPlan =
      List("prophylaxis", "restriction", "restraint").exists(text.contains)
    val fit =
      if playedIsCapture && restraintPlan then 3
      else if playedIsCapture && tacticalPlan then 0
      else 1
    fit -> plan.hypothesis.rank

  private def lineStartsWithPlayedMove(line: String, playedSan: String): Boolean =
    LineScopedCitation.hasConcreteSanLine(line) &&
      LineScopedCitation.firstConcreteSanToken(line).exists(token => sameSanToken(token, playedSan))

  private def sameSanToken(left: String, right: String): Boolean =
    normalizeSanToken(left) == normalizeSanToken(right)

  private def normalizeSanToken(raw: String): String =
    Option(raw)
      .getOrElse("")
      .trim
      .replaceAll("""[+#?!]+$""", "")
      .replaceAll("""^\d+\.(?:\.\.)?""", "")
      .replaceAll("""[.,;:]+$""", "")
      .trim
      .toLowerCase

  private def branchDisplayLine(branch: EvidenceBranch): Option[String] =
    LineScopedCitation
      .evidenceBranchDisplayLine(branch)
      .orElse(cleanLine(branch.line))

  private def cleanLine(raw: String): Option[String] =
    Option(raw)
      .map(_.trim.replaceAll("\\s+", " "))
      .filter(_.nonEmpty)

private[commentary] object QuestionFirstCommentaryPlanner:

  private val BranchPattern = """(?m)^[a-z]\)\s+""".r

  private val PlannerLinePurpose = "planner_line_proof"
  private val NeutralizeKeyBreakProofFamily = ProofFamilyId.NeutralizeKeyBreak.wireKey
  private val OnlyMoveDefenseProofFamily = "only_move_defense"
  private val OpeningRelationDataOnlyPlyCutoff = 20

  private[commentary] def openingRelationReplayClaim(ctx: NarrativeContext): Option[String] =
    Option
      .when(openingRelationReplayEligible(ctx)) {
        OpeningPrecedentBranching
          .relationSentence(ctx, ctx.openingData, requireFocus = false)
          .flatMap(cleanLine)
      }
      .flatten

  private def openingRelationReplayEligible(ctx: NarrativeContext): Boolean =
    openingRelationPrecedentEnough(ctx) &&
      (
        ctx.openingEvent.nonEmpty ||
          ctx.ply <= OpeningRelationDataOnlyPlyCutoff
      )

  private def openingRelationPrecedentEnough(ctx: NarrativeContext): Boolean =
    ctx.openingData.exists(ref => ref.totalGames >= 2 || ref.sampleGames.size >= 2)

  private[commentary] def endgameTransitionReplayClaim(ctx: NarrativeContext): Option[String] =
    ctx.semantic.flatMap(_.endgameFeatures).flatMap(endgameTransitionSentence)

  def plan(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): RankedQuestionPlans =
    planInternal(
      ctx = Some(ctx),
      ply = ctx.ply,
      authorQuestions = ctx.authorQuestions,
      inputs = inputs,
      truthContract = truthContract
    )

  def plan(
      ply: Int,
      authorQuestions: List[AuthorQuestion],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): RankedQuestionPlans =
    planInternal(
      ctx = None,
      ply = ply,
      authorQuestions = authorQuestions,
      inputs = inputs,
      truthContract = truthContract
    )

  private def planInternal(
      ctx: Option[NarrativeContext],
      ply: Int,
      authorQuestions: List[AuthorQuestion],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): RankedQuestionPlans =
    val hydratedInputs = hydrateDomainClaims(ctx, inputs)
    val rawCandidates = rawOwnerCandidates(ctx, hydratedInputs, truthContract)
    val sceneTrace = classifySceneTrace(hydratedInputs, truthContract, rawCandidates)
    val sceneType = sceneTrace.sceneType
    val ownerCandidates = rawCandidates.map(candidate => applyAdmission(sceneType, candidate))
    val (evaluatedAdmitted, evaluationRejected) =
      evaluateQuestions(ctx, authorQuestions, ply, hydratedInputs, truthContract, sceneType)
    val (admitted, admissionRejected) =
      admitEvaluatedPlans(sceneType, ownerCandidateIndex(ownerCandidates), evaluatedAdmitted)
    val (releaseAdmitted, releaseRejected) =
      applyStrategicReleasePolicy(ctx, hydratedInputs, truthContract, admitted)
    val rejected = evaluationRejected ++ admissionRejected ++ releaseRejected
    val (primary, secondary) = selectRankedPlans(sceneType, releaseAdmitted)
    val ownerTrace = buildOwnerTrace(sceneTrace, ownerCandidates, releaseAdmitted, rejected, primary)
    RankedQuestionPlans(primary = primary, secondary = secondary, rejected = rejected, ownerTrace = ownerTrace)

  private def hydrateDomainClaims(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs
  ): QuestionPlannerInputs =
    inputs.copy(
      openingRelationClaim = inputs.openingRelationClaim.orElse(ctx.flatMap(openingRelationReplayClaim)),
      endgameTransitionClaim = inputs.endgameTransitionClaim.orElse(ctx.flatMap(endgameTransitionReplayClaim))
    )

  def hasConcreteWhyNowOwner(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    concreteWhyNowTimingOwner(None, inputs, truthContract).nonEmpty

  private def evaluateQuestion(
      ctx: Option[NarrativeContext],
      question: AuthorQuestion,
      ply: Int,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      sceneType: SceneType
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    question.kind match
      case AuthorQuestionKind.WhatMattersHere =>
        buildWhatMattersHerePlan(ctx, question, inputs, truthContract, sceneType)
      case AuthorQuestionKind.WhyThis =>
        buildWhyThisPlan(ctx, question, inputs, truthContract, sceneType)
      case AuthorQuestionKind.WhyNow =>
        buildWhyNowPlan(ctx, question, ply, inputs, truthContract, sceneType)
      case AuthorQuestionKind.WhatChanged =>
        buildWhatChangedPlan(ctx, question, inputs, truthContract, sceneType)
      case AuthorQuestionKind.WhatMustBeStopped =>
        buildWhatMustBeStoppedPlan(ctx, question, ply, inputs, truthContract, sceneType)
      case AuthorQuestionKind.WhosePlanIsFaster =>
        buildWhosePlanIsFasterPlan(ctx, question, ply, inputs, truthContract, sceneType)

  private def evaluateQuestions(
      ctx: Option[NarrativeContext],
      authorQuestions: List[AuthorQuestion],
      ply: Int,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      sceneType: SceneType
  ): (List[QuestionPlan], List[RejectedQuestionPlan]) =
    val evaluated =
      authorQuestions.map(question =>
        evaluateQuestion(ctx, question, ply, inputs, truthContract, sceneType)
      )
    (
      evaluated.collect { case Left(value) => value },
      evaluated.collect { case Right(value) => value }
    )

  private def ownerCandidateIndex(
      ownerCandidates: List[OwnerCandidateTrace]
  ): Map[(PlannerOwnerKind, String), OwnerCandidateTrace] =
    ownerCandidates
      .filter(_.materiality == OwnerCandidateMateriality.OwnerCandidate)
      .groupBy(candidate => candidate.plannerOwnerKind -> candidate.source)
      .view
      .mapValues(_.sortBy(candidate =>
        (
          admissionRank(candidate.admissionDecision.getOrElse(AdmissionDecision.Forbidden)),
          candidate.timingSource.map(_.wireName).getOrElse(""),
          candidate.reasons.mkString("+")
        )
      ).head)
      .toMap

  private def admitEvaluatedPlans(
      sceneType: SceneType,
      candidateIndex: Map[(PlannerOwnerKind, String), OwnerCandidateTrace],
      plans: List[QuestionPlan]
  ): (List[QuestionPlan], List[RejectedQuestionPlan]) =
    plans.foldLeft((List.empty[QuestionPlan], List.empty[RejectedQuestionPlan])) {
      case ((kept, dropped), plan) =>
        admittedPlanDecision(sceneType, candidateIndex.get(plan.plannerOwnerKind -> plan.plannerSource), plan) match
          case Left(admittedPlan)  => (kept :+ admittedPlan, dropped)
          case Right(rejectedPlan) => (kept, dropped :+ rejectedPlan)
    }

  private def selectRankedPlans(
      sceneType: SceneType,
      plans: List[QuestionPlan]
  ): (Option[QuestionPlan], Option[QuestionPlan]) =
    val ranked = plans.sortBy(plan => planScore(sceneType, plan)).reverse
    val primary = ranked.headOption
    val secondary = ranked.drop(1).find(plan => secondaryAllowed(primary, plan))
    primary -> secondary

  private def planScore(sceneType: SceneType, plan: QuestionPlan): (Int, Int, Int, Int, Int, Int, Int, Int) =
    (
      exactStateDeltaPriority(sceneType, plan),
      questionSuitability(sceneType, plan),
      strengthScore(plan.strengthTier),
      plan.contrast.fold(0)(_ => 1),
      plan.consequence.fold(0)(_ => 1),
      plan.evidence.fold(0)(evidence => evidenceQuality(evidence)),
      plan.priority,
      tacticalSeverity(plan)
    )

  private def exactStateDeltaPriority(sceneType: SceneType, plan: QuestionPlan): Int =
    Option.when(
      (
        plan.questionKind == AuthorQuestionKind.WhatChanged &&
          plan.admissibilityReasons.contains("exact_target_state_delta") &&
          (plan.contrast.nonEmpty || plan.consequence.nonEmpty)
      ) || (
        sceneType == SceneType.ForcingDefense &&
          plan.questionKind == AuthorQuestionKind.WhatMattersHere &&
          plan.plannerSource == PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource &&
          plan.admissibilityReasons.contains("certified_position_probe")
      ) || (
        sceneType == SceneType.ForcingDefense &&
          plan.plannerSource == PlayerFacingTruthModePolicy.DefenderTradeProofSource &&
          plan.admissibilityReasons.contains("strategic_claim_supported_local")
      )
    )(1).getOrElse(0)

  private def questionSuitability(
      sceneType: SceneType,
      plan: QuestionPlan
  ): Int =
    import AuthorQuestionKind.*
    val kind = plan.questionKind
    sceneType match
      case SceneType.ConcreteTactical =>
        questionKindScore(kind, WhyThis -> 5, WhatChanged -> 4, WhatMustBeStopped -> 3, WhyNow -> 2, WhatMattersHere -> 1, WhosePlanIsFaster -> 1)
      case SceneType.LineConsequence =>
        questionKindScore(kind, WhatChanged -> 5, WhyThis -> 4, WhyNow -> 2, WhatMustBeStopped -> 1, WhatMattersHere -> 1, WhosePlanIsFaster -> 1)
      case SceneType.AlternativeComparison =>
        questionKindScore(kind, WhyThis -> 5, WhatChanged -> 4, WhyNow -> 3, WhatMattersHere -> 1, WhatMustBeStopped -> 1, WhosePlanIsFaster -> 1)
      case SceneType.ForcingDefense =>
        questionKindScore(kind, WhatMustBeStopped -> 5, WhyNow -> 4, WhatChanged -> 3, WhyThis -> 2, WhatMattersHere -> 1, WhosePlanIsFaster -> 1)
      case SceneType.PlanClash =>
        questionKindScore(kind, WhosePlanIsFaster -> 5, WhyThis -> 4, WhyNow -> 3, WhatChanged -> 2, WhatMattersHere -> 1, WhatMustBeStopped -> 1)
      case SceneType.TransitionConversion =>
        questionKindScore(kind, WhatChanged -> 5, WhyThis -> 4, WhyNow -> 3, WhatMattersHere -> 2, WhatMustBeStopped -> 2, WhosePlanIsFaster -> 1)
      case SceneType.OpeningRelation =>
        questionKindScore(kind, WhyThis -> 5, WhatChanged -> 4, WhyNow -> 3, WhatMustBeStopped -> 2, WhatMattersHere -> 1, WhosePlanIsFaster -> 1)
      case SceneType.EndgameTransition =>
        questionKindScore(kind, WhatChanged -> 5, WhyThis -> 4, WhyNow -> 3, WhatMustBeStopped -> 2, WhatMattersHere -> 1, WhosePlanIsFaster -> 1)
      case SceneType.QuietImprovement =>
        questionKindScore(kind, WhatMattersHere -> 6, WhyThis -> 5, WhatChanged -> 4, WhyNow -> 3, WhatMustBeStopped -> 2, WhosePlanIsFaster -> 1)

  private def questionKindScore(
      kind: AuthorQuestionKind,
      scores: (AuthorQuestionKind, Int)*
  ): Int =
    scores.find(_._1 == kind).map(_._2).getOrElse(0)

  private def strengthScore(tier: QuestionPlanStrengthTier): Int =
    tier match
      case QuestionPlanStrengthTier.Strong   => 3
      case QuestionPlanStrengthTier.Moderate => 2
      case QuestionPlanStrengthTier.Exact    => 1

  private def evidenceQuality(evidence: QuestionPlanEvidence): Int =
    val branchScore = countBranches(evidence.text)
    if branchScore >= 2 then 3
    else if evidence.branchScoped then 2
    else 1

  private def countBranches(text: String): Int =
    BranchPattern.findAllMatchIn(Option(text).getOrElse("")).size

  private def tacticalSeverity(plan: QuestionPlan): Int =
    if plan.questionKind == AuthorQuestionKind.WhatMustBeStopped then 3
    else if plan.sourceKinds.exists(kind => kind.contains("tactical") || kind.contains("threat")) then 2
    else if plan.sourceKinds.exists(_.contains("prevented_plan")) then 1
    else 0

  private def secondaryAllowed(primary: Option[QuestionPlan], candidate: QuestionPlan): Boolean =
    primary.forall { head =>
      head.questionId != candidate.questionId &&
      head.questionKind != candidate.questionKind &&
      !sameText(head.claim, candidate.claim) &&
      !suppressesExactStateDeltaRestatement(head, candidate) &&
      (
        candidate.contrast.exists(text => !head.contrast.exists(sameText(_, text))) ||
          candidate.evidence.exists(e => !head.evidence.exists(existing => sameText(existing.text, e.text)))
      )
    }

  private def suppressesExactStateDeltaRestatement(
      primary: QuestionPlan,
      candidate: QuestionPlan
  ): Boolean =
    primary.questionKind == AuthorQuestionKind.WhatChanged &&
      primary.admissibilityReasons.contains("exact_target_state_delta") &&
      candidate.questionKind == AuthorQuestionKind.WhyThis &&
      primary.plannerOwnerKind == PlannerOwnerKind.MoveDelta &&
      candidate.plannerOwnerKind == PlannerOwnerKind.MoveDelta &&
      primary.plannerSource == candidate.plannerSource

  private def buildAuthorEvidence(
      question: AuthorQuestion,
      inputs: QuestionPlannerInputs
  ): Option[QuestionPlanEvidence] =
    val evidence = inputs.evidenceByQuestionId.getOrElse(question.id, Nil)
    val branches = evidence.flatMap(_.branches).distinctBy(_.line).take(3)
    val rendered =
      branches.flatMap(branch => LineScopedCitation.evidenceBranchDisplayLine(branch).map(line => branch -> line))
    Option.when(rendered.nonEmpty) {
      val labels = List("a)", "b)", "c)")
      QuestionPlanEvidence(
        text =
          rendered.zip(labels).map { case ((branch, line), label) =>
            val evalPart = branch.evalCp.map(cp => s" (${formatCp(cp)})").getOrElse("")
            s"$label $line$evalPart"
          }.mkString("\n"),
        purposes = evidence.map(_.purpose).distinct,
        sourceKinds = List("author_evidence"),
        sourceIds = branches.flatMap(_.sourceId).distinct,
        branchScoped = true
      )
    }

  private def buildSingleLineEvidence(
      text: String,
      sourceKinds: List[String]
  ): Option[QuestionPlanEvidence] =
    cleanLine(stripLineLabel(text)).filterNot(probeRequestReminder).map { line =>
      QuestionPlanEvidence(
        text = s"a) $line",
        purposes = List(PlannerLinePurpose),
        sourceKinds = sourceKinds,
        branchScoped = true
      )
    }

  private def stripLineLabel(raw: String): String =
    Option(raw)
      .getOrElse("")
      .trim
      .replaceFirst("""(?i)^line:\s*""", "")
      .replaceFirst("""(?i)^short line:\s*""", "")
      .replaceFirst("""^[a-z]\)\s*""", "")

  private def probeRequestReminder(line: String): Boolean =
    line.toLowerCase.startsWith("further probe work still targets ")

  private def wrapUpConsequence(text: String): QuestionPlanConsequence =
    QuestionPlanConsequence(text, QuestionPlanConsequenceBeat.WrapUp)

  private def evidenceForQuestion(
      question: AuthorQuestion,
      fallbackLine: Option[String],
      sourceKinds: List[String]
  )(
      using inputs: QuestionPlannerInputs
  ): Option[QuestionPlanEvidence] =
    buildAuthorEvidence(question, inputs)
      .orElse {
        val needsMultiBranch =
          question.evidencePurposes.exists(p => p == "keep_tension_branches" || p == "recapture_branches")
        if needsMultiBranch then None else fallbackLine.flatMap(buildSingleLineEvidence(_, sourceKinds))
      }

  private def mkPlan(
      question: AuthorQuestion,
      kind: AuthorQuestionKind,
      claim: String,
      evidence: Option[QuestionPlanEvidence],
      contrast: Option[String],
      consequence: Option[QuestionPlanConsequence],
      fallbackMode: QuestionPlanFallbackMode,
      strengthTier: QuestionPlanStrengthTier,
      sourceKinds: List[String],
      admissibilityReasons: List[String],
      plannerOwnerKind: PlannerOwnerKind,
      plannerSource: String,
      prefixKind: PlayerFacingClaimPrefixKind = PlayerFacingClaimPrefixKind.None,
      demotionReasons: List[String] = Nil,
      timingWitness: Option[QuestionPlanTimingWitness] = None
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    cleanLine(claim) match
      case Some(cleanClaim) =>
        Left(
          QuestionPlan(
            questionId = question.id,
            questionKind = kind,
            priority = question.priority,
            claim = ensureSentence(cleanClaim),
            evidence = evidence,
            contrast = contrast.flatMap(cleanLine).map(ensureSentence),
            consequence = consequence.map(value => value.copy(text = ensureSentence(value.text))),
            fallbackMode = fallbackMode,
            strengthTier = strengthTier,
            sourceKinds = sourceKinds.distinct,
            admissibilityReasons = admissibilityReasons.distinct,
            plannerOwnerKind = plannerOwnerKind,
            plannerSource = plannerSource,
            prefixKind = prefixKind,
            demotionReasons = demotionReasons.distinct,
            timingWitness = timingWitness.map(witness =>
              witness.copy(witnessTokens = witness.witnessTokens.flatMap(timingWitnessTokenVariants).distinct)
            )
          )
        )
      case None =>
        reject(question, QuestionPlanFallbackMode.FactualFallback, "missing_claim")

  private def reject(
      question: AuthorQuestion,
      fallbackMode: QuestionPlanFallbackMode,
      reasons: String*
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    Right(
      RejectedQuestionPlan(
        questionId = question.id,
        questionKind = question.kind,
        fallbackMode = fallbackMode,
        reasons = reasons.toList.distinct
      )
    )

  private def resolveDemotion(
      question: AuthorQuestion,
      fallbackMode: QuestionPlanFallbackMode,
      demotedTo: AuthorQuestionKind,
      reasons: List[String],
      fallbackBuild: => Either[QuestionPlan, RejectedQuestionPlan]
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    fallbackBuild match
      case Left(plan) =>
        Left(
          plan.copy(
            fallbackMode = fallbackMode,
            demotionReasons = (plan.demotionReasons ++ reasons).distinct
          )
        )
      case Right(_)   =>
        Right(
          RejectedQuestionPlan(
            questionId = question.id,
            questionKind = question.kind,
            fallbackMode = QuestionPlanFallbackMode.Suppressed,
            reasons =
              (
                reasons ++
                  List(
                    "demotion_intentional_drop",
                    "demote_target_unavailable",
                    s"demoted_to=${demotedTo.toString}"
                  )
              ).distinct,
            demotedTo = Some(demotedTo),
            demotionReasons = (reasons :+ "demotion_intentional_drop").distinct
          )
        )

  private def sameText(a: String, b: String): Boolean =
    normalizeText(a) == normalizeText(b)

  private def normalizeText(raw: String): String =
    Option(raw).getOrElse("").toLowerCase.replaceAll("""[^a-z0-9\s]""", " ").replaceAll("\\s+", " ").trim

  private def timingWitnessTokenVariants(raw: String): List[String] =
    cleanLine(raw).toList.flatMap { text =>
      (text :: text.split("""[^A-Za-z0-9]+""").toList).map(normalizeText).filter(_.nonEmpty)
    }

  private def cleanLine(raw: String): Option[String] =
    Option(raw).map(_.trim.replaceAll("\\s+", " ")).filter(_.nonEmpty)

  private def sanitizeThreatDefense(
      ctx: Option[NarrativeContext],
      threat: ThreatRow
  ): ThreatRow =
    threat.copy(bestDefense = threat.bestDefense.map(defenseMoveLabel(ctx, _)))

  private def defenseMoveLabel(
      ctx: Option[NarrativeContext],
      raw: String
  ): String =
    val cleaned = cleanLine(raw).getOrElse("")
    moveLabel(ctx, raw).getOrElse {
      if isUciMove(cleaned) || coordinateOnly(cleaned) then "the defensive reply" else cleaned
    }

  private def moveLabel(
      ctx: Option[NarrativeContext],
      raw: String
  ): Option[String] =
    val cleaned = cleanLine(raw).getOrElse("")
    ctx.flatMap { current =>
      val normalized = NarrativeUtils.normalizeUciMove(cleaned)
      current.playedMove
        .map(NarrativeUtils.normalizeUciMove)
        .filter(_ == normalized)
        .flatMap(_ => current.playedSan.flatMap(cleanLine))
        .orElse {
          Option.when(isUciMove(normalized)) {
            NarrativeUtils.uciListToSan(current.fen, List(normalized)).headOption.flatMap(cleanLine)
          }.flatten
        }
    }.orElse(Option.when(cleaned.nonEmpty && !isUciMove(cleaned))(cleaned))

  private def isUciMove(raw: String): Boolean =
    raw.matches("""[a-h][1-8][a-h][1-8][qrbn]?""")

  private def coordinateOnly(raw: String): Boolean =
    raw.matches("""[a-h][1-8]""")

  private def concreteDefenseLabel(raw: String): Boolean =
    cleanLine(raw).exists(label => label != "the defensive reply" && !coordinateOnly(label))

  private def ensureSentence(raw: String): String =
    val text = Option(raw).map(_.trim).getOrElse("")
    if text.isEmpty then ""
    else if ".!?".contains(text.last) then text
    else s"$text."

  private def formatCp(cp: Int): String =
    f"${cp.toDouble / 100}%.2f"

  private def ownerSideLabel(inputs: QuestionPlannerInputs, ply: Int): String =
    inputs.decisionFrame.ownerSide
      .orElse(Option.when(ply % 2 == 0)("white").orElse(Some("black")))
      .map(_.capitalize)
      .getOrElse("The side to move")

  private def bestImmediateThreat(threats: List[ThreatRow]): Option[ThreatRow] =
    threats
      .filter(threat => threat.lossIfIgnoredCp > 0 || threat.kind.equalsIgnoreCase("mate"))
      .sortBy(threat => (-threat.lossIfIgnoredCp, threat.turnsToImpact))
      .headOption

  private def threatOwnerAllowedForMoveReview(
      ctx: Option[NarrativeContext],
      threat: ThreatRow
  ): Boolean =
    !ctx.exists(_.renderMode == NarrativeRenderMode.MoveReview) ||
      threatBestDefenseMatchesPlayedMove(ctx, threat)

  private def threatBestDefenseMatchesPlayedMove(
      ctx: Option[NarrativeContext],
      threat: ThreatRow
  ): Boolean =
    ctx.flatMap(_.playedMove).exists { playedMove =>
      val playedUci = NarrativeUtils.normalizeUciMove(playedMove)
      val playedSan = ctx.flatMap(_.playedSan).flatMap(cleanLine)
      threat.bestDefense.exists { raw =>
        val cleaned = cleanLine(raw).getOrElse("")
        NarrativeUtils.normalizeUciMove(cleaned) == playedUci ||
          playedSan.exists(san => sameText(cleaned, san))
      }
    }

  private def hasConcreteMoveDeltaChange(inputs: QuestionPlannerInputs): Boolean =
    inputs.pvDelta.exists(delta =>
      delta.resolvedThreats.nonEmpty || delta.newOpportunities.nonEmpty || delta.planAdvancements.nonEmpty
    )

  private def plannerThreatNeedsForcingOwner(
      inputs: QuestionPlannerInputs,
      contract: DecisiveTruthContract
  ): Boolean =
    inputs.pvDelta.nonEmpty &&
      !hasConcreteMoveDeltaChange(inputs) &&
      bestImmediateThreat(inputs.opponentThreats).nonEmpty &&
      contract.failureMode == FailureInterpretationMode.NoClearPlan &&
      (contract.reasonFamily == DecisiveReasonKind.InvestmentSacrifice ||
        contract.reasonFamily == DecisiveReasonKind.Conversion)

  private def prefersQuietMoveDeltaIngress(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    inputs.pvDelta.nonEmpty &&
      truthContract.forall { contract =>
        val quietMoveDeltaFamily =
          contract.reasonFamily match
          case DecisiveReasonKind.InvestmentSacrifice |
              DecisiveReasonKind.QuietTechnicalMove |
              DecisiveReasonKind.Conversion =>
            true
          case _ => false
        quietMoveDeltaFamily && !plannerThreatNeedsForcingOwner(inputs, contract)
      }

  private def prefersRestrictedSuppressionMoveDeltaIngress(
      inputs: QuestionPlannerInputs
  ): Boolean =
    def normalizedId(raw: String): String =
      Option(raw).map(_.trim.toLowerCase).getOrElse("")
    inputs.evidenceBackedPlans.exists { plan =>
      normalizedId(plan.themeL1) == PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id &&
      plan.subplanId.exists { subplanId =>
        val normalizedSubplan = normalizedId(subplanId)
        normalizedSubplan == PlanTaxonomy.PlanKind.ProphylaxisRestraint.id ||
        normalizedSubplan == PlanTaxonomy.PlanKind.BreakPrevention.id ||
        normalizedSubplan == PlanTaxonomy.PlanKind.KeySquareDenial.id
      }
    }

  private def onlyMovePressure(
      truthContract: Option[DecisiveTruthContract]
  ): Option[String] =
    truthContract.flatMap { contract =>
      Option.when(
        onlyMoveOwnsReviewedMove(contract)
      ) {
        contract.benchmarkMove match
          case Some(best) => s"Other moves allow the position to slip away; the benchmark move is $best"
          case None       => "Other moves allow the position to slip away"
      }
    }

  private def onlyMoveOwnsReviewedMove(contract: DecisiveTruthContract): Boolean =
    contract.chosenMatchesBest &&
      (
        contract.isCriticalBestMove ||
          contract.reasonFamily == DecisiveReasonKind.OnlyMoveDefense
      )

  private def becauseClause(raw: String): String =
    val text = Option(raw).getOrElse("").trim
    if text.isEmpty then text
    else text.take(1).toLowerCase + text.drop(1)

  private def planAdvanceConsequence(delta: PVDelta): Option[String] =
    delta.planAdvancements.headOption.map(step => s"That changes the next phase of the position by making $step available.")

  private def planAdvanceOrOpportunity(delta: PVDelta): Option[String] =
    planAdvanceConsequence(delta).orElse(newOpportunityConsequence(delta))

  private def resolvedThreatConsequence(delta: PVDelta): Option[String] =
    delta.resolvedThreats.headOption.map(threat => s"That removes the immediate problem of $threat.")

  private def newOpportunityConsequence(delta: PVDelta): Option[String] =
    delta.newOpportunities.headOption.map(opportunity => s"That creates a concrete follow-up around $opportunity.")

  private def preventedPlanTimingClaim(plan: PreventedPlanInfo): Option[String] =
    plan.breakNeutralized.map(file => s"The timing matters now because otherwise the $file-break becomes available.")
      .orElse(plan.preventedThreatType.map(kind => s"The timing matters now because otherwise the opponent's $kind comes to life."))
      .orElse(
        Option.when(plan.counterplayScoreDrop >= 80)(
          s"The timing matters now because drifting gives the opponent roughly ${plan.counterplayScoreDrop}cp of counterplay."
        )
      )

  private def preventedPlanTimingContrast(plan: PreventedPlanInfo): Option[String] =
    plan.citationLine.map(line => s"If delayed, the line $line becomes available again.")
      .orElse(plan.breakNeutralized.map(file => s"If delayed, the $file-break comes back into play."))
      .orElse(
        Option.when(plan.counterplayScoreDrop >= 80)(
          s"If delayed, that counterplay window reopens immediately."
        )
      )

  private def preventedPlanChangeClaim(plan: PreventedPlanInfo): Option[String] =
    plan.breakNeutralized.map(file => s"This changes the position by taking the $file-break away.")
      .orElse(plan.preventedThreatType.map(kind => s"This changes the position by taking the opponent's $kind off the table."))
      .orElse(
        Option.when(plan.counterplayScoreDrop >= 80)(
          s"This changes the position by shrinking the opponent's counterplay window by about ${plan.counterplayScoreDrop}cp."
        )
      )

  private def localFileEntryChangeClaim(
      pair: LocalFileEntryProof.SurfacePair
  ): Option[String] =
    Some(
      s"This changes the position by taking the ${pair.file} away as a counterplay route and closing ${pair.entrySquare}."
    )

  private def localFileEntryChangeContrast(
      pair: LocalFileEntryProof.SurfacePair
  ): Option[String] =
    Some(
      s"Before the move, the ${pair.file} and the ${pair.entrySquare} entry were still available."
    )

  private def localFileEntrySurfacePair(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs
  ): Option[LocalFileEntryProof.SurfacePair] =
    Option.unless(inputs.heavyPieceLocalBindBlocked) {
      ctx.flatMap(context =>
        LocalFileEntryProof.certifiedSurfacePair(
          ctx = context,
          preventedPlans = inputs.preventedPlansNow,
          evidenceBackedPlans = inputs.evidenceBackedPlans
        )
      )
    }.flatten

  private def preventedPlansWhenLocalBindAllowed(inputs: QuestionPlannerInputs): List[PreventedPlanInfo] =
    if inputs.heavyPieceLocalBindBlocked then Nil else inputs.preventedPlansNow

  private def preventedPlansAllowedForPlannerSurface(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): List[PreventedPlanInfo] =
    preventedPlansWhenLocalBindAllowed(inputs)
      .filter(plan => certifiedPreventedPlanSurface(ctx, inputs, truthContract, plan))

  private def certifiedPreventedPlanSurface(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      plan: PreventedPlanInfo
  ): Boolean =
    plan.breakNeutralized.exists(_.trim.nonEmpty) &&
      !centralBreakOwnsPreventedBreak(ctx, plan) &&
      neutralizeKeyBreakPreventedPlanTimingWitness(plan).exists { witness =>
        val admissionPlan =
          QuestionPlan(
            questionId = "prevented_plan_surface_gate",
            questionKind = AuthorQuestionKind.WhyNow,
            priority = 0,
            claim = preventedPlanTimingClaim(plan).getOrElse(""),
            evidence = None,
            contrast = None,
            consequence = None,
            fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
            strengthTier = QuestionPlanStrengthTier.Moderate,
            sourceKinds = List("prevented_plan"),
            admissibilityReasons = List("prevented_plan_surface_gate"),
            plannerOwnerKind = PlannerOwnerKind.ForcingDefense,
            plannerSource = "prevented_plan",
            timingWitness = Some(witness)
          )
        ClaimAuthorityResolver
          .supportedLocalNeutralizeKeyBreakTimingAdmission(ctx, inputs, truthContract, admissionPlan)
          .exists(_.decision.tier == ClaimAuthorityTier.SupportedLocal)
      }

  private def centralBreakOwnsPreventedBreak(
      ctx: Option[NarrativeContext],
      plan: PreventedPlanInfo
  ): Boolean =
    ctx.exists { context =>
      CentralBreakTimingWitness.exact(context).exists { witness =>
        witness.sourceTags.exists(_ == "board:played_break") &&
          plan.breakNeutralized.flatMap(preventedBreakDestination).contains(normalizePlannerToken(witness.breakSquare)) &&
          !explicitBreakPreventionPlan(context)
      }
    }

  private def explicitBreakPreventionPlan(ctx: NarrativeContext): Boolean =
    val breakPrevention = PlanTaxonomy.PlanKind.BreakPrevention.id
    ctx.mainStrategicPlans.exists(_.subplanId.exists(id => normalizePlannerToken(id) == breakPrevention)) ||
      ctx.strategicPlanExperiments.exists(experiment =>
        experiment.subplanId.exists(id => normalizePlannerToken(id) == breakPrevention) &&
          experiment.counterBreakNeutralized
      )

  private def preventedBreakDestination(token: String): Option[String] =
    normalizePlannerToken(token).stripPrefix("...").split("-").lastOption.filter(_.nonEmpty)

  private def normalizePlannerToken(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def localFileEntryChangeConsequence(
      pair: LocalFileEntryProof.SurfacePair
  ): Option[String] =
    Option.when(pair.counterplayScoreDrop > 0)(
      s"That removes roughly ${pair.counterplayScoreDrop}cp of counterplay from the local route."
    )

  private def namedRouteNetworkWhyThisClaim(
      inputs: QuestionPlannerInputs
  ): Option[String] =
    if inputs.heavyPieceLocalBindBlocked then None
    else
      inputs.namedRouteNetworkSurface
        .flatMap { network =>
          network.intermediateSquare match
            case Some(_) =>
              // Broader route-chain intermediates stay backend-only until a real
              // exact-FEN root-best control is restored for planner-owned truth.
              None
            case None =>
              Some(network.routeDenialText("This"))
        }

  private def preventedPlanChangeContrast(plan: PreventedPlanInfo): Option[String] =
    plan.breakNeutralized.map(file => s"Before the move, the $file-break was still available.")
      .orElse(plan.preventedThreatType.map(kind => s"Before the move, the opponent's $kind was still live."))
      .orElse(
        Option.when(plan.counterplayScoreDrop >= 80)(
          "Before the move, that counterplay window was still there."
        )
      )

  private def preventedPlanChangeConsequence(plan: PreventedPlanInfo): Option[String] =
    Option.when(plan.counterplayScoreDrop > 0) {
      s"That removes roughly ${plan.counterplayScoreDrop}cp of counterplay from the next phase."
    }

  private def exactTargetFixationPacket(
      inputs: QuestionPlannerInputs
  ): Option[PlayerFacingClaimPacket] =
    inputs.mainBundle.flatMap(_.mainClaim).flatMap(_.packet)
      .filter(PlayerFacingTruthModePolicy.certifiedExactTargetFixationPacket)

  private def exactTargetFixationChangeClaim(
      inputs: QuestionPlannerInputs
  ): Option[String] =
    exactTargetFixationPacket(inputs).flatMap(PlayerFacingTruthModePolicy.exactTargetFixationWhatChangedClaim)

  private def exactTargetFixationChangeContrast(
      inputs: QuestionPlannerInputs
  ): Option[String] =
    exactTargetFixationPacket(inputs).flatMap(PlayerFacingTruthModePolicy.exactTargetFixationWhatChangedContrast)

  private def exactTargetFixationChangeConsequence(
      inputs: QuestionPlannerInputs
  ): Option[QuestionPlanConsequence] =
    exactTargetFixationPacket(inputs).flatMap(packet =>
      PlayerFacingTruthModePolicy.exactTargetFixationWhatChangedConsequence(packet).map(wrapUpConsequence)
    )

  private def decisionComparisonChangeClaim(
      comparison: Option[DecisionComparison]
  ): Option[String] =
    comparison.flatMap { value =>
      certifiedAlternativeComparisonConsequence(value)
        .map(consequence => s"The concrete difference is that $consequence")
        .orElse(
          value.cpLossVsChosen
        .map(math.abs)
        .filter(_ >= 60)
        .map { loss =>
          value.deferredMove
            .filter(_.trim.nonEmpty)
            .map(move => s"This changes the engine margin by keeping about ${loss}cp that would drift toward $move.")
            .orElse(
              value.deferredReason
                .filter(_.trim.nonEmpty)
                .map(reason => s"This changes the engine margin by preserving about ${loss}cp: $reason.")
            )
            .getOrElse(s"This changes the engine margin by preserving about ${loss}cp.")
        }
        )
    }

  private def decisionComparisonChangeContrast(
      comparison: Option[DecisionComparison]
  ): Option[String] =
    comparison.flatMap { value =>
      value.deferredMove
        .filter(_.trim.nonEmpty)
        .map(move => s"Without the move, the cleaner version of the position runs through $move.")
        .orElse(
          value.deferredReason
            .filter(_.trim.nonEmpty)
            .map(reason => s"Without the move, $reason.")
        )
    }

  private def decisionComparisonChangeConsequence(
      comparison: Option[DecisionComparison]
  ): Option[String] =
    comparison.flatMap { value =>
      certifiedAlternativeComparisonConsequence(value)
        .orElse(
          value.cpLossVsChosen
        .map(math.abs)
        .filter(_ >= 60)
        .map(loss => s"That keeps roughly ${loss}cp of engine margin from slipping away.")
        )
    }

  private def roleAwareDecisionComparisonConsequence(
      comparison: DecisionComparison
  ): Option[String] =
    comparison.comparativeSource
      .filter(source => source.trim == DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource)
      .flatMap(_ => comparison.comparativeConsequence.map(_.trim).filter(DecisionComparisonComparativeSupport.roleAwareLineConsequenceText))

  private def certifiedAlternativeComparisonConsequence(
      comparison: DecisionComparison
  ): Option[String] =
    roleAwareDecisionComparisonConsequence(comparison)

  private def certifiedAlternativeComparisonAccepted(
      comparison: DecisionComparison,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    DecisionComparisonComparativeSupport.roleAwareLineConsequenceAccepted(comparison, truthContract)

  private def certifiedAlternativeComparisonSource(comparison: DecisionComparison): String =
    comparison.comparativeSource
      .filter(source =>
        source == DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource
      )
      .getOrElse(DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource)

  private def comparisonAlternativeMove(
      comparison: DecisionComparison
  ): Option[String] =
    comparison.deferredMove
      .filter(_.trim.nonEmpty)
      .orElse(
        comparison.engineBestMove
          .filter(_.trim.nonEmpty)
          .filter(move => comparison.chosenMove.forall(chosen => !sameText(chosen, move)))
      )

  private def decisionComparisonTimingDetail(
      comparison: DecisionComparison
  ): DecisionComparisonTimingDetail =
    val concreteSource =
      comparison.deferredSource.exists(source => !isEngineGapLikeDecisionSource(source))
    val concreteReason =
      comparison.deferredReason
        .filter(_.trim.nonEmpty)
        .exists(reason =>
          !comparison.practicalAlternative &&
            !looksLikeEngineGapReason(reason) &&
            comparison.deferredSource.forall(source => !isEngineGapLikeDecisionSource(source))
        )
    val concreteReply = !comparison.practicalAlternative &&
      comparison.deferredMove.exists(_.trim.nonEmpty) &&
      concreteSource
    if concreteReason || concreteReply then DecisionComparisonTimingDetail.ConcreteReplyOrReason
    else DecisionComparisonTimingDetail.BareEngineGap

  private def isEngineGapLikeDecisionSource(source: String): Boolean =
    sameText(source, "engine_gap") ||
      sameText(source, "top_engine_move") ||
      sameText(source, "close_candidate")

  private def looksLikeEngineGapReason(reason: String): Boolean =
    val normalized = normalizeText(reason)
    normalized.startsWith("it trails the engine line by about") ||
      normalized.startsWith("it trails the engine line") ||
      normalized.startsWith("delaying costs about") ||
      normalized.startsWith("it costs about")

  private def domainEvidenceFallbackLine(
      inputs: QuestionPlannerInputs
  ): Option[String] =
    inputs.mainBundle.flatMap(_.lineScopedClaim).map(_.claimText)
      .orElse(inputs.candidateEvidenceLines.find(line => cleanLine(line).nonEmpty))

  private def openingRelationContrast(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      questionKind: AuthorQuestionKind
  ): Option[String] =
    questionKind match
      case AuthorQuestionKind.WhatChanged =>
        Some("Before the move, the opening reference still matched the more familiar setup.")
      case _ =>
        inputs.alternativeNarrative.map(_.sentence)
          .orElse(
            inputs.decisionComparison.flatMap(
              _.deferredMove.map(move => s"The practical alternative $move stays closer to the familiar opening reference.")
            )
          )
          .orElse(onlyMovePressure(truthContract))
          .orElse(Some("The move changes how much opening-reference context still applies."))

  private def endgameTransitionContrast(
      questionKind: AuthorQuestionKind
  ): Option[String] =
    questionKind match
      case AuthorQuestionKind.WhatChanged =>
        Some("Before the move, the earlier endgame task still defined the position.")
      case _ =>
        Some("The move matters because it changes the technical task of the ending, not just the local geometry.")

  private def buildOpeningRelationPlan(
      question: AuthorQuestion,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      questionKind: AuthorQuestionKind
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    mkPlan(
      question = question,
      kind = questionKind,
      claim = inputs.openingRelationClaim.getOrElse(""),
      evidence =
        evidenceForQuestion(
          question = question,
          fallbackLine = domainEvidenceFallbackLine(inputs),
          sourceKinds = List("opening_relation_translator")
        ),
      contrast = openingRelationContrast(inputs, truthContract, questionKind),
      consequence =
        inputs.pvDelta
          .flatMap(planAdvanceOrOpportunity)
          .map(wrapUpConsequence),
      fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
      strengthTier = QuestionPlanStrengthTier.Moderate,
      sourceKinds = List("opening_relation_translator"),
      admissibilityReasons = List("opening_relation_owner", "scene_first_domain_owner"),
      plannerOwnerKind = PlannerOwnerKind.OpeningRelation,
      plannerSource = "opening_relation_translator"
    )

  private def buildEndgameTransitionPlan(
      question: AuthorQuestion,
      inputs: QuestionPlannerInputs,
      questionKind: AuthorQuestionKind
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    mkPlan(
      question = question,
      kind = questionKind,
      claim = inputs.endgameTransitionClaim.getOrElse(""),
      evidence =
        evidenceForQuestion(
          question = question,
          fallbackLine = domainEvidenceFallbackLine(inputs),
          sourceKinds = List("endgame_transition_translator")
        ),
      contrast = endgameTransitionContrast(questionKind),
      consequence = None,
      fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
      strengthTier =
        if questionKind == AuthorQuestionKind.WhatChanged then QuestionPlanStrengthTier.Strong
        else QuestionPlanStrengthTier.Moderate,
      sourceKinds = List("endgame_transition_translator"),
      admissibilityReasons = List("endgame_transition_owner", "scene_first_domain_owner"),
      plannerOwnerKind = PlannerOwnerKind.EndgameTransition,
      plannerSource = "endgame_transition_translator"
    )

  private def buildWhatMattersHerePlan(
      ctx: Option[NarrativeContext],
      question: AuthorQuestion,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      @unused sceneType: SceneType
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    positionProbePlanMaterial(ctx, inputs, truthContract) match
      case Left(reasons) =>
        reject(question, QuestionPlanFallbackMode.FactualFallback, reasons*)
      case Right(material) =>
        buildPositionProbePlan(question, material)

  private def positionProbePlanMaterial(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Either[List[String], PositionProbePlanMaterial] =
    val rawPositionProbe =
      inputs.mainBundle.flatMap(_.mainClaim).filter(_.scope == PlayerFacingClaimScope.PositionLocal)
    rawPositionProbe match
      case None =>
        Left(List("position_probe_missing"))
      case Some(claim) =>
        claim.packet match
          case None =>
            Left(List("position_probe_not_certified"))
          case Some(packet) =>
            val decision = ClaimAuthorityResolver.decidePositionProbe(ctx, inputs, truthContract, packet)
            if decision.admitted then
              Right(PositionProbePlanMaterial(claim = claim, packet = packet, decision = decision))
            else Left(positionProbeRejectionReasons(decision))

  private def positionProbeRejectionReasons(decision: ClaimAuthorityDecision): List[String] =
    val rejectionReasons =
      Option.when(decision.vetoReasons.nonEmpty)("strategic_claim_tactical_veto").toList ++
        decision.vetoReasons ++
        decision.failureCodes.filterNot(decision.vetoReasons.contains)
    if rejectionReasons.nonEmpty then rejectionReasons else List("position_probe_not_certified")

  private def buildPositionProbePlan(
      question: AuthorQuestion,
      material: PositionProbePlanMaterial
  )(
      using inputs: QuestionPlannerInputs
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    mkPlan(
      question = question,
      kind = AuthorQuestionKind.WhatMattersHere,
      claim = material.claim.claimText,
      evidence =
        evidenceForQuestion(
          question = question,
          fallbackLine = inputs.mainBundle.flatMap(_.lineScopedClaim).map(_.claimText),
          sourceKinds = List(material.claim.sourceKind)
        ),
      contrast = None,
      consequence = positionProbePlanConsequence(material),
      fallbackMode = material.fallbackMode,
      strengthTier = material.strengthTier,
      sourceKinds = material.sourceKinds,
      admissibilityReasons = material.admissibilityReasons,
      plannerOwnerKind = PlannerOwnerKind.PositionProbe,
      plannerSource = material.plannerSource,
      prefixKind = material.prefixKind
    )

  private def positionProbePlanConsequence(material: PositionProbePlanMaterial): Option[QuestionPlanConsequence] =
    Option.unless(material.supportedLocal) {
      positionProbeConsequence(material.packet).map(wrapUpConsequence)
    }.flatten

  private def positionProbeConsequence(packet: PlayerFacingClaimPacket): Option[String] =
    PlayerFacingTruthModePolicy.positionProbeTaskConsequence(packet)

  private def buildWhyThisPlan(
      ctx: Option[NarrativeContext],
      question: AuthorQuestion,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
    sceneType: SceneType
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    whyThisDomainFirstPlan(question, inputs, sceneType).getOrElse {
      val moveOwnerClaim = whyThisMoveOwnerClaim(inputs)
      whyThisPlanMaterial(ctx, inputs, truthContract, moveOwnerClaim) match
        case None =>
          reject(question, QuestionPlanFallbackMode.FactualFallback, "missing_move_owner")
        case Some(material) =>
          val evidence =
            evidenceForQuestion(
              question = question,
              fallbackLine =
                inputs.mainBundle.flatMap(_.lineScopedClaim).map(_.claimText)
                  .orElse(inputs.decisionComparison.flatMap(_.evidence))
                  .orElse(inputs.quietIntent.flatMap(_.evidenceLine)),
              sourceKinds = material.evidenceSourceKinds
            )
          mkPlan(
            question = question,
            kind = AuthorQuestionKind.WhyThis,
            claim = material.claim,
            evidence = evidence,
            contrast = material.contrast,
            consequence = material.consequence,
            fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
            strengthTier = material.strengthTier,
            sourceKinds = material.sourceKinds,
            admissibilityReasons = material.admissibilityReasons,
            plannerOwnerKind = material.plannerOwnerKind,
            plannerSource = material.plannerSource
          )
    }

  private def whyThisDomainFirstPlan(
      question: AuthorQuestion,
      inputs: QuestionPlannerInputs,
      sceneType: SceneType
  ): Option[Either[QuestionPlan, RejectedQuestionPlan]] =
    sceneType match
      case SceneType.EndgameTransition if inputs.endgameTransitionClaim.nonEmpty =>
        Some(buildEndgameTransitionPlan(question, inputs, AuthorQuestionKind.WhyThis))
      case _ => None

  private def whyThisMoveOwnerClaim(inputs: QuestionPlannerInputs): Option[MainPathScopedClaim] =
    inputs.mainBundle.flatMap { bundle =>
      bundle.mainClaim.filter(_.scope == PlayerFacingClaimScope.MoveLocal).orElse(
        bundle.lineScopedClaim.filter(claim =>
          claim.scope == PlayerFacingClaimScope.LineScoped &&
            claim.packet.exists(_.fallbackMode == PlayerFacingClaimFallbackMode.WeakMain)
        )
      ).filter(plannerMoveOwnerClaimAllowed)
    }

  private def plannerMoveOwnerClaimAllowed(claim: MainPathScopedClaim): Boolean =
    claim.mode != PlayerFacingTruthMode.Tactical || tacticalClaimOwnsCurrentMove(claim)

  private def tacticalClaimOwnsCurrentMove(claim: MainPathScopedClaim): Boolean =
    claim.tacticalOwnership.exists(ownedTacticalTag) &&
      normalizeText(claim.sourceKind) != "tactical sacrifice"

  private def ownedTacticalTag(raw: String): Boolean =
    val tag = normalizeText(raw)
    tag.nonEmpty && tag != "tactical sacrifice"

  private def whyThisPlanMaterial(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      moveOwnerClaim: Option[MainPathScopedClaim]
  ): Option[WhyThisPlanMaterial] =
    val namedRouteClaim = namedRouteNetworkWhyThisClaim(inputs)
    val moveOwnedMaterial =
      namedRouteClaim
      .orElse(moveOwnerClaim.map(_.claimText))
      .orElse(inputs.quietIntent.map(_.claimText))
      .map { claim =>
        val namedRouteOwner = namedRouteClaim.contains(claim)
        val routeSource = Option.when(namedRouteOwner)(RouteNetworkBindProof.ProofSource)
        val tacticalOwner = moveOwnerClaim.exists(tacticalClaimOwnsCurrentMove)
        WhyThisPlanMaterial(
          claim = claim,
          evidenceSourceKinds = (List("main_bundle", "quiet_intent") ++ routeSource.toList).distinct,
          contrast = whyThisContrast(inputs, truthContract),
          consequence = whyThisConsequence(ctx, inputs),
          strengthTier =
            if tacticalOwner then QuestionPlanStrengthTier.Strong
            else QuestionPlanStrengthTier.Moderate,
          sourceKinds =
            (List(moveOwnerClaim.map(_.sourceKind), inputs.quietIntent.map(_.sourceKind)).flatten ++
              routeSource.toList).distinct,
          plannerOwnerKind =
            if namedRouteOwner then PlannerOwnerKind.MoveDelta
            else if tacticalOwner then PlannerOwnerKind.ConcreteTactical
            else PlannerOwnerKind.MoveDelta,
          plannerSource =
            moveOwnerClaim.map(_.sourceKind)
              .orElse(inputs.quietIntent.map(_.sourceKind))
              .getOrElse("move_owner")
        )
      }
    moveOwnedMaterial
      .orElse(localFactWhyThisPlanMaterial(inputs, truthContract))
      .orElse(roleAwareWhyThisPlanMaterial(inputs, truthContract))

  private def localFactWhyThisPlanMaterial(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[WhyThisPlanMaterial] =
    inputs.localFactResult
      .filter(localFactResultWhyThisEligible)
      .filterNot(result =>
        checkOnlyTacticalLocalFact(result) &&
          certifiedAlternativeComparisonAvailable(inputs, truthContract)
      )
      .map { result =>
        val source = localFactResultSource(result)
        WhyThisPlanMaterial(
          claim = result.explanation.prose,
          evidenceSourceKinds = localFactResultSourceKinds(result),
          contrast = None,
          consequence = None,
          strengthTier = QuestionPlanStrengthTier.Strong,
          sourceKinds = localFactResultSourceKinds(result),
          plannerOwnerKind = localFactResultPlannerOwnerKind(result),
          plannerSource = source,
          admissibilityReasons =
            List(
              "typed_local_fact",
              s"local_fact_family:${result.localFact.family.key}",
              s"local_fact_producer:${result.localFact.producer.key}"
            )
        )
      }

  private def roleAwareWhyThisPlanMaterial(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[WhyThisPlanMaterial] =
    inputs.decisionComparison
      .filter(certifiedAlternativeComparisonAccepted(_, truthContract))
        .flatMap(comparison => certifiedAlternativeComparisonConsequence(comparison).map(comparison -> _))
        .map { case (comparison, consequence) =>
          val comparisonSource = certifiedAlternativeComparisonSource(comparison)
          WhyThisPlanMaterial(
            claim = s"The concrete difference is that $consequence",
            evidenceSourceKinds =
              List(
                "decision_comparison",
                comparisonSource
              ),
            contrast = None,
            consequence = None,
            strengthTier = QuestionPlanStrengthTier.Strong,
            sourceKinds =
              List(
                "decision_comparison",
                comparisonSource
              ),
            plannerOwnerKind = PlannerOwnerKind.AlternativeComparison,
            plannerSource = "decision_comparison",
            admissibilityReasons =
              List(
                comparisonSource,
                "branch_scoped_alternative"
              ) ++ roleAwareAlternativeGateReasons(truthContract)
          )
        }

  private def roleAwareAlternativeGateReasons(
      truthContract: Option[DecisiveTruthContract]
  ): List[String] =
    List(
      Option.when(truthContract.exists(_.blocksStrategicSupport))("risk_gate_blocks_strategic_support"),
      Option.when(truthContract.exists(contract =>
        contract.reasonFamily == DecisiveReasonKind.OnlyMoveDefense && !contract.chosenMatchesBest
      ))("missed_benchmark_alternative")
    ).flatten

  private def whyThisContrast(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[String] =
    inputs.alternativeNarrative.map(_.sentence)
      .orElse(inputs.decisionComparison.flatMap(_.deferredMove.map(move => s"The practical alternative $move remains secondary here.")))
      .orElse(onlyMovePressure(truthContract))

  private def whyThisConsequence(ctx: Option[NarrativeContext], inputs: QuestionPlannerInputs): Option[QuestionPlanConsequence] =
    inputs.pvDelta
      .flatMap(planAdvanceOrOpportunity)
      .map(wrapUpConsequence)
      .orElse {
        inputs.counterfactual
          .filter(_.cpLoss > 0)
          .map(cf => wrapUpConsequence(counterfactualConsequenceText(ctx, cf)))
      }

  private def counterfactualConsequenceText(ctx: Option[NarrativeContext], counterfactual: CounterfactualMatch): String =
    ThreatExtractor.counterfactualCausalThreatConsequence(counterfactual)
      .getOrElse(
        moveLabel(ctx, counterfactual.bestMove)
          .map(move => s"If the move is missed, $move becomes the cleaner continuation instead.")
          .getOrElse("If the move is missed, the opponent gets a cleaner continuation instead.")
      )

  private def buildWhyNowPlan(
      ctx: Option[NarrativeContext],
      question: AuthorQuestion,
      @unused ply: Int,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      sceneType: SceneType
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    val timingOwner = concreteWhyNowTimingOwner(ctx, inputs, truthContract)
    timingOwner match
      case None =>
        resolveDemotion(
          question = question,
          QuestionPlanFallbackMode.DemotedToWhyThis,
          demotedTo = AuthorQuestionKind.WhyThis,
          reasons = List("generic_urgency_only"),
          fallbackBuild = buildWhyThisPlan(ctx, question, inputs, truthContract, sceneType)
        )
      case Some(owner) =>
        val material = whyNowPlanMaterial(ctx, owner, inputs, truthContract)
        val evidence =
          evidenceForQuestion(
            question = question,
            fallbackLine =
              inputs.mainBundle.flatMap(_.lineScopedClaim).map(_.claimText)
                .orElse(inputs.decisionComparison.flatMap(_.evidence))
                .orElse(inputs.quietIntent.flatMap(_.evidenceLine)),
            sourceKinds = material.evidenceSourceKinds
          )
        mkPlan(
          question = question,
          kind = AuthorQuestionKind.WhyNow,
          claim = material.claim,
          evidence = evidence,
          contrast = material.contrast,
          consequence = material.consequence,
          fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
          strengthTier = QuestionPlanStrengthTier.Strong,
          sourceKinds = material.sourceKinds,
          admissibilityReasons =
            List("timing_owner", "delay_sensitive_proof") ++
              Option.when(material.sourceKind == "decision_comparison")("timing_loss").toList ++
              material.supportReasons,
          plannerOwnerKind = material.plannerOwnerKind,
          plannerSource = material.sourceKind,
          timingWitness = neutralizeKeyBreakTimingWitness(owner)
        )

  private def whyNowPlanMaterial(
      ctx: Option[NarrativeContext],
      owner: WhyNowTimingOwner,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): WhyNowPlanMaterial =
    owner match
      case WhyNowTimingOwner.Threat(threat) =>
        val defense = threat.bestDefense.map(defenseMoveLabel(ctx, _)).filter(concreteDefenseLabel)
        WhyNowPlanMaterial(
          claim =
            defense
              .map(defense => s"The move has to happen now because otherwise $defense is demanded immediately.")
              .getOrElse(s"The move has to happen now because the opponent's ${threat.kind.toLowerCase} threat is already live."),
          contrast = defense.map(defense => s"If White drifts, $defense is the reply."),
          consequence = Some(
            wrapUpConsequence(s"That keeps the immediate ${threat.kind.toLowerCase} pressure from taking over.")
          ),
          sourceKind = "threat",
          plannerOwnerKind = PlannerOwnerKind.ForcingDefense
        )
      case WhyNowTimingOwner.Prevented(plan) =>
        WhyNowPlanMaterial(
          claim = preventedPlanTimingClaim(plan).getOrElse(""),
          contrast = preventedPlanTimingContrast(plan),
          consequence =
            Option.when(plan.counterplayScoreDrop > 0)(
              wrapUpConsequence(s"That shuts down roughly ${plan.counterplayScoreDrop}cp of counterplay before it starts.")
            ).orElse(decisionComparisonValueConsequence(inputs.decisionComparison)),
          sourceKind = "prevented_plan",
          plannerOwnerKind = PlannerOwnerKind.ForcingDefense
        )
      case WhyNowTimingOwner.OnlyMove(reason, _) =>
        val supportedLocal = onlyMoveSupportedLocalReason(ctx, inputs, truthContract)
        WhyNowPlanMaterial(
          claim =
            supportedLocal
              .map(localReason => s"The timing matters now because ${localReason.clause}.")
              .getOrElse(s"The timing matters now because ${becauseClause(reason)}."),
          contrast =
            if supportedLocal.nonEmpty then None
            else decisionComparisonTimingContrast(inputs.decisionComparison),
          consequence = decisionComparisonValueConsequence(inputs.decisionComparison),
          sourceKind = "only_move_defense",
          plannerOwnerKind = PlannerOwnerKind.ForcingDefense,
          supportSourceKinds = supportedLocal.toList.flatMap(_.sourceKinds).distinct,
          supportReasons = supportedLocal.toList.flatMap(_.reasons).distinct
        )
      case WhyNowTimingOwner.RelationLocalFact(result) =>
        WhyNowPlanMaterial(
          claim = result.explanation.prose,
          contrast = None,
          consequence = None,
          sourceKind = localFactResultSource(result),
          plannerOwnerKind = PlannerOwnerKind.DecisionTiming
        )
      case WhyNowTimingOwner.DecisionComparisonOwner(comparison) =>
        WhyNowPlanMaterial(
          claim = decisionComparisonTimingClaim(Some(comparison)).getOrElse(""),
          contrast = decisionComparisonTimingContrast(Some(comparison)),
          consequence = decisionComparisonValueConsequence(inputs.decisionComparison),
          sourceKind = "decision_comparison",
          plannerOwnerKind = PlannerOwnerKind.DecisionTiming
        )

  private final case class OnlyMoveSupportedLocalReason(
      clause: String,
      sourceKinds: List[String],
      reasons: List[String]
  )

  private def onlyMoveSupportedLocalReason(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[OnlyMoveSupportedLocalReason] =
    onlyMoveSupportedLocalClaim(ctx, inputs, truthContract)
      .flatMap(claim =>
        onlyMoveSupportedLocalClause(claim).map(clause =>
          OnlyMoveSupportedLocalReason(
            clause = clause,
            sourceKinds = (List("supported_local_packet", claim.sourceKind) ++ claim.packet.toList.map(_.proofSource)).distinct,
            reasons = List("only_move_supported_local_context", s"supported_local_source:${claim.sourceKind}")
          )
        )
      )
      .orElse(onlyMovePositionProbeReason(ctx, inputs, truthContract))
      .orElse(onlyMoveTypedLocalFactReason(inputs.localFactResult))

  private def onlyMoveSupportedLocalClaim(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[MainPathScopedClaim] =
    val moveOwner =
      inputs.mainBundle
        .flatMap(_.mainClaim)
        .filter(_.scope == PlayerFacingClaimScope.MoveLocal)
        .filter(plannerMoveOwnerClaimAllowed)
    supportedLocalWhatChangedOwner(ctx, inputs, truthContract, moveOwner)
      .filter(claim => cleanLine(claim.claimText).nonEmpty)

  private def onlyMoveSupportedLocalClause(claim: MainPathScopedClaim): Option[String] =
    cleanLine(claim.claimText).map { text =>
      onlyMoveSupportBecauseClause(text)
    }

  private def onlyMovePositionProbeReason(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[OnlyMoveSupportedLocalReason] =
    inputs.mainBundle
      .flatMap(_.mainClaim)
      .filter(_.scope == PlayerFacingClaimScope.PositionLocal)
      .flatMap { claim =>
        claim.packet
          .filter(packet => ClaimAuthorityResolver.decidePositionProbe(ctx, inputs, truthContract, packet).admitted)
          .filter(onlyMovePositionProbePacket)
          .filter(packet => onlyMovePositionProbeBoardChecked(ctx, packet))
          .flatMap(packet =>
            onlyMoveSupportedLocalClause(claim).map(clause =>
              OnlyMoveSupportedLocalReason(
                clause = clause,
                sourceKinds = List("position_probe_context", claim.sourceKind, packet.proofFamily, packet.proofSource).distinct,
                reasons =
                  List(
                    "only_move_supported_local_context",
                    "only_move_position_probe_context",
                    s"supported_local_source:${claim.sourceKind}",
                    s"proof_source:${packet.proofSource}"
                  )
              )
            )
          )
      }

  private def onlyMovePositionProbePacket(packet: PlayerFacingClaimPacket): Boolean =
    Set(
      PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource,
      PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofSource,
      PlayerFacingTruthModePolicy.ColorComplexSqueezeProbeProofSource
    ).contains(packet.proofSource)

  private def onlyMovePositionProbeBoardChecked(
      ctx: Option[NarrativeContext],
      packet: PlayerFacingClaimPacket
  ): Boolean =
    packet.proofSource match
      case PlayerFacingTruthModePolicy.ColorComplexSqueezeProbeProofSource =>
        (
          for
            context <- ctx
            played <- context.playedMove.map(MoveReviewPvLine.normalizeUci).filter(_.nonEmpty)
            targetKey <- PlayerFacingTruthModePolicy.exactSliceTargetSquare(packet)
            target <- _root_.chess.Square.all.find(_.key == targetKey)
            afterFen <- MoveReviewPvLine.legalFenAfter(context.fen, played)
            after <- _root_.chess.format.Fen.read(_root_.chess.variant.Standard, _root_.chess.format.Fen.Full(afterFen))
          yield
            val side = after.color
            after.board.attackers(target, !side).exists { square =>
              after.board.pieceAt(square).exists(piece =>
                piece.color == !side &&
                  (piece.role == _root_.chess.Bishop || piece.role == _root_.chess.Knight)
              )
            }
        ).contains(true)
      case _ => true

  private def onlyMoveTypedLocalFactReason(
      result: Option[MoveReviewExplanationBuilder.Result]
  ): Option[OnlyMoveSupportedLocalReason] =
    result
      .filter(onlyMoveTypedLocalFactEligible)
      .flatMap { result =>
        cleanLine(result.explanation.prose).map { text =>
          val source = localFactResultSource(result)
          OnlyMoveSupportedLocalReason(
            clause = onlyMoveSupportBecauseClause(text),
            sourceKinds =
              (
                localFactResultSourceKinds(result) ++
                  result.localFact.evidenceRefs.filter(ref => ref.startsWith("proof_family:") || ref.startsWith("proof_source:"))
              ).distinct,
            reasons =
              List(
                "only_move_supported_local_context",
                "only_move_typed_local_fact_context",
                s"supported_local_source:$source",
                s"local_fact_family:${result.localFact.family.key}",
                s"local_fact_producer:${result.localFact.producer.key}"
              )
          )
        }
      }

  private def onlyMoveTypedLocalFactEligible(result: MoveReviewExplanationBuilder.Result): Boolean =
    localFactResultWhyThisEligible(result) &&
      result.localFact.authority == MoveReviewLocalFact.Authority.CertifiedStrategy &&
      result.localFact.producer == MoveReviewLocalFact.Producer.CertifiedStrategyDelta &&
      result.localFact.lineBinding == MoveReviewLocalFact.LineBinding.PvCoupled &&
      Set("certified_strategy_support", "supported_local_position_probe").contains(localFactResultSource(result)) &&
      result.localFact.evidenceRefs.exists(_.startsWith("proof_family:")) &&
      result.localFact.evidenceRefs.exists(_.startsWith("proof_source:"))

  private def onlyMoveSupportBecauseClause(raw: String): String =
    val text = onlyMoveSupportLeadSentence(raw)
    if leadingSanToken(text) then text else becauseClause(text)

  private def onlyMoveSupportLeadSentence(raw: String): String =
    Option(raw)
      .map(_.trim.replaceAll("\\s+", " "))
      .filter(_.nonEmpty)
      .flatMap(_.split("""(?<=[.!?])\s+""", 2).headOption)
      .getOrElse("")
      .stripSuffix(".")

  private def leadingSanToken(text: String): Boolean =
    text.matches("""^(?:O-O(?:-O)?|[KQRBN][a-h1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?).*""")

  private def concreteWhyNowTimingOwner(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[WhyNowTimingOwner] =
    val urgentThreat = bestImmediateThreat(inputs.opponentThreats)
    val allowedPreventedPlans = preventedPlansAllowedForPlannerSurface(ctx, inputs, truthContract)
    val preventedNamedOrBreak =
      allowedPreventedPlans.find(plan =>
        plan.breakNeutralized.exists(_.trim.nonEmpty) ||
          plan.preventedThreatType.exists(_.trim.nonEmpty)
      )
    val preventedCounterplayWindow =
      allowedPreventedPlans.find(plan =>
        !plan.breakNeutralized.exists(_.trim.nonEmpty) &&
          !plan.preventedThreatType.exists(_.trim.nonEmpty) &&
          plan.counterplayScoreDrop >= 80
      )
    urgentThreat.map(WhyNowTimingOwner.Threat.apply)
      .orElse(
        preventedNamedOrBreak
          .filter(plan => preventedPlanTimingClaim(plan).nonEmpty)
          .map(WhyNowTimingOwner.Prevented.apply)
      )
      .orElse(onlyMoveTimingOwner(ctx, inputs, truthContract))
      .orElse(
        inputs.localFactResult
          .filter(localFactResultTimingEligible)
          .map(WhyNowTimingOwner.RelationLocalFact.apply)
      )
      .orElse(
        inputs.decisionComparison
          .filter(comparison => decisionComparisonTimingClaim(Some(comparison)).nonEmpty)
          .map(WhyNowTimingOwner.DecisionComparisonOwner.apply)
      )
      .orElse(
        preventedCounterplayWindow
          .filter(plan => preventedPlanTimingClaim(plan).nonEmpty)
          .map(WhyNowTimingOwner.Prevented.apply)
      )

  private def neutralizeKeyBreakTimingWitness(owner: WhyNowTimingOwner): Option[QuestionPlanTimingWitness] =
    owner match
      case WhyNowTimingOwner.Threat(threat) =>
        neutralizeKeyBreakThreatTimingWitness(threat)
      case WhyNowTimingOwner.Prevented(plan) =>
        neutralizeKeyBreakPreventedPlanTimingWitness(plan)
      case WhyNowTimingOwner.OnlyMove(_, witness) =>
        witness
      case WhyNowTimingOwner.RelationLocalFact(_) =>
        None
      case WhyNowTimingOwner.DecisionComparisonOwner(_) =>
        None

  private def onlyMoveTimingOwner(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[WhyNowTimingOwner] =
    onlyMovePressure(truthContract)
      .map(reason => WhyNowTimingOwner.OnlyMove(reason, onlyMoveTimingWitness(ctx, inputs, truthContract)))

  private def onlyMoveTimingWitness(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[QuestionPlanTimingWitness] =
    truthContract
      .filter(onlyMoveOwnsReviewedMove)
      .flatMap { contract =>
        val rawMoves =
          List(
            contract.benchmarkMove,
            contract.verifiedBestMove,
            contract.playedMove,
            inputs.decisionComparison.flatMap(_.engineBestMove),
            inputs.decisionComparison.flatMap(_.chosenMove)
          ).flatten.flatMap(cleanLine).distinct
        val anchor =
          rawMoves.iterator
            .flatMap(move => moveLabel(ctx, move).orElse(Some(move)))
            .toList
            .headOption
        anchor.map { move =>
          QuestionPlanTimingWitness(
            proofFamily = OnlyMoveDefenseProofFamily,
            source = "only_move_defense",
            continuationMove = Some(move),
            witnessTokens = (move :: rawMoves).distinct
          )
        }
      }

  private def neutralizeKeyBreakThreatTimingWitness(threat: ThreatRow): Option[QuestionPlanTimingWitness] =
    val continuationMove = threat.bestDefense.flatMap(cleanLine)
    val tokens =
      continuationMove.toList ++
        threat.square.flatMap(cleanLine).toList ++
        threat.targetPieces.flatMap(cleanLine) ++
        threat.motifs.flatMap(cleanLine)
    Option.when(tokens.nonEmpty)(
      QuestionPlanTimingWitness(
        proofFamily = NeutralizeKeyBreakProofFamily,
        source = "threat",
        continuationMove = continuationMove,
        witnessTokens = tokens.distinct
      )
    )

  private def neutralizeKeyBreakPreventedPlanTimingWitness(plan: PreventedPlanInfo): Option[QuestionPlanTimingWitness] =
    val namedBreak = plan.breakNeutralized.flatMap(cleanLine)
    val squareTokens = plan.deniedSquares.flatMap(cleanLine)
    val tokens = namedBreak.toList ++ squareTokens
    Option.when(tokens.nonEmpty)(
      QuestionPlanTimingWitness(
        proofFamily = NeutralizeKeyBreakProofFamily,
        source = "prevented_plan",
        namedBreak = namedBreak,
        witnessTokens = tokens
      )
    )

  private def buildWhatChangedPlan(
      ctx: Option[NarrativeContext],
      question: AuthorQuestion,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      sceneType: SceneType
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    val material = whatChangedPlannerMaterial(ctx, inputs, truthContract)
    val domainFirst = whatChangedDomainFirstPlan(question, inputs, truthContract, sceneType, material)
    domainFirst.getOrElse(buildMoveAttributedWhatChangedPlan(question, inputs, material))

  private def whatChangedDomainFirstPlan(
      question: AuthorQuestion,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      sceneType: SceneType,
      material: WhatChangedPlannerMaterial
  ): Option[Either[QuestionPlan, RejectedQuestionPlan]] =
    sceneType match
      case SceneType.OpeningRelation
          if inputs.openingRelationClaim.nonEmpty && material.pvCoupledPlanSupport.isEmpty && material.lineConsequenceClaim.isEmpty &&
            material.localFactResult.isEmpty =>
        Some(buildOpeningRelationPlan(question, inputs, truthContract, AuthorQuestionKind.WhatChanged))
      case SceneType.EndgameTransition if inputs.endgameTransitionClaim.nonEmpty =>
        Some(buildEndgameTransitionPlan(question, inputs, AuthorQuestionKind.WhatChanged))
      case _ => None

  private def whatChangedPlannerMaterial(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): WhatChangedPlannerMaterial =
    val moveOwner =
      inputs.mainBundle
        .flatMap(_.mainClaim)
        .filter(_.scope == PlayerFacingClaimScope.MoveLocal)
        .filter(plannerMoveOwnerClaimAllowed)
    val supportedLocalMoveOwner = supportedLocalWhatChangedOwner(ctx, inputs, truthContract, moveOwner)
    val exactTargetFixationChange = exactTargetFixationChangeClaim(inputs)
    val canPromoteDecisionComparisonChange =
      moveOwner.nonEmpty ||
        hasConcreteMoveDeltaChange(inputs) ||
        inputs.decisionComparison.exists(certifiedAlternativeComparisonConsequence(_).nonEmpty)
    val allowedPreventedPlans = preventedPlansAllowedForPlannerSurface(ctx, inputs, truthContract)
    val localFileEntryPair = localFileEntrySurfacePair(ctx, inputs)
    val localFileEntryChange = localFileEntryPair.flatMap(localFileEntryChangeClaim)
    val hasLocalFileEntryChange = localFileEntryPair.nonEmpty
    val preventedPlanChange = allowedPreventedPlans.collectFirst(Function.unlift(preventedPlanChangeClaim))
    val hasPreventedPlanChangeMaterial = allowedPreventedPlans.exists(preventedPlanHasChangeMaterial)
    val pvCoupledPlanSupport =
      Option
        .unless(truthContract.exists(_.blocksStrategicSupport))(inputs.pvCoupledPlanSupport)
        .flatten
        .filter(_.anchorMatched)
    val lineConsequenceRiskGate =
      truthContract.exists(_.blocksStrategicSupport)
    val lineConsequence =
      inputs.lineConsequence.filter(evidence => evidence.surfaceReady && evidence.kind != LineConsequenceKind.PreviewOnly)
    val lineConsequenceClaim =
      lineConsequence.flatMap(evidence =>
        ctx.flatMap(VariationNarrativeBuilder.build(_, evidence)).orElse(cleanLine(evidence.playerSentence))
      )
    val localFactResult =
      inputs.localFactResult.filter(localFactResultWhyThisEligible).filterNot(forkEntryDefenseLocalFact)
    val localFactChange =
      localFactResult.map(_.explanation.prose)
    val decisionComparisonChange =
      if canPromoteDecisionComparisonChange then decisionComparisonChangeClaim(inputs.decisionComparison) else None
    val moveLinkedChange =
      whatChangedMoveLinkedChange(
        inputs,
        exactTargetFixationChange,
        localFileEntryChange,
        preventedPlanChange,
        pvCoupledPlanSupport,
        lineConsequenceClaim,
        localFactChange,
        decisionComparisonChange
      )
    WhatChangedPlannerMaterial(
      moveOwner = moveOwner,
      supportedLocalMoveOwner = supportedLocalMoveOwner,
      allowedPreventedPlans = allowedPreventedPlans,
      exactTargetFixationChange = exactTargetFixationChange,
      canPromoteDecisionComparisonChange = canPromoteDecisionComparisonChange,
      localFileEntryPair = localFileEntryPair,
      localFileEntryChange = localFileEntryChange,
      hasLocalFileEntryChange = hasLocalFileEntryChange,
      preventedPlanChange = preventedPlanChange,
      hasPreventedPlanChangeMaterial = hasPreventedPlanChangeMaterial,
      pvCoupledPlanSupport = pvCoupledPlanSupport,
      lineConsequence = lineConsequence,
      lineConsequenceClaim = lineConsequenceClaim,
      lineConsequenceRiskGate = lineConsequenceRiskGate,
      localFactResult = localFactResult,
      localFactChange = localFactChange,
      decisionComparisonChange = decisionComparisonChange,
      moveLinkedChange = moveLinkedChange
    )

  private def supportedLocalWhatChangedOwner(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      moveOwner: Option[MainPathScopedClaim]
  ): Option[MainPathScopedClaim] =
    moveOwner.filter(claim =>
      claim.packet.exists(packet =>
        ClaimAuthorityResolver
          .supportedLocalMoveDeltaPacketDecision(ctx, inputs, truthContract, packet)
          .supportedLocalWithoutTacticalVeto
      )
    )

  private def preventedPlanHasChangeMaterial(plan: PreventedPlanInfo): Boolean =
    preventedPlanChangeClaim(plan).nonEmpty ||
      preventedPlanChangeContrast(plan).nonEmpty ||
      preventedPlanChangeConsequence(plan).nonEmpty

  private def whatChangedMoveLinkedChange(
      inputs: QuestionPlannerInputs,
      exactTargetFixationChange: Option[String],
      localFileEntryChange: Option[String],
      preventedPlanChange: Option[String],
      pvCoupledPlanSupport: Option[PvCoupledPlanSupport],
      lineConsequenceClaim: Option[String],
      localFactChange: Option[String],
      decisionComparisonChange: Option[String]
  ): Option[String] =
    exactTargetFixationChange.orElse {
      inputs.pvDelta.flatMap { delta =>
        resolvedThreatConsequence(delta)
          .orElse(newOpportunityConsequence(delta))
          .orElse(planAdvanceConsequence(delta))
      }
    }.orElse {
      localFileEntryChange
    }.orElse {
      preventedPlanChange
    }.orElse {
      lineConsequenceClaim
    }.orElse {
      localFactChange
    }.orElse {
      pvCoupledPlanSupport.map(_.claim)
    }.orElse {
      decisionComparisonChange
    }

  private def buildMoveAttributedWhatChangedPlan(
      question: AuthorQuestion,
      inputs: QuestionPlannerInputs,
      material: WhatChangedPlannerMaterial
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    if !material.hasMoveAttributedChange then
      reject(question, QuestionPlanFallbackMode.FactualFallback, "state_truth_only")
    else
      mkPlan(
        question = question,
        kind = AuthorQuestionKind.WhatChanged,
        claim = whatChangedClaim(material),
        evidence =
          evidenceForQuestion(
            question = question,
            fallbackLine = whatChangedEvidenceFallbackLine(inputs, material),
            sourceKinds = whatChangedSourceKinds(inputs, material)
          ),
        contrast = whatChangedContrast(inputs, material),
        consequence = whatChangedConsequence(inputs, material),
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Strong,
        sourceKinds = whatChangedSourceKinds(inputs, material),
        admissibilityReasons =
          List("move_attributed_change") ++
            Option.when(material.exactTargetFixationChange.nonEmpty)("exact_target_state_delta").toList,
        plannerOwnerKind = whatChangedPlannerOwnerKind(inputs, material),
        plannerSource = whatChangedPlannerSource(inputs, material)
      )

  private def whatChangedClaim(material: WhatChangedPlannerMaterial): String =
    material.supportedLocalMoveOwner.map(_.claimText)
      .orElse(material.moveLinkedChange)
      .orElse(material.moveOwner.map(_.claimText))
      .getOrElse("")

  private def whatChangedEvidenceFallbackLine(
      inputs: QuestionPlannerInputs,
      material: WhatChangedPlannerMaterial
  ): Option[String] =
    inputs.mainBundle.flatMap(_.lineScopedClaim).map(_.claimText)
      .orElse(
        material.lineConsequence.flatMap(evidence =>
          cleanLine(evidence.sanMoves.take(5).mkString(" ")).map(line => s"Short line: $line.")
        )
      )
      .orElse(material.pvCoupledPlanSupport.map(_.evidenceLine))
      .orElse(material.allowedPreventedPlans.flatMap(_.citationLine).find(_.trim.nonEmpty))

  private def whatChangedContrast(
      inputs: QuestionPlannerInputs,
      material: WhatChangedPlannerMaterial
  ): Option[String] =
    Option.unless(material.supportedLocal) {
      exactTargetFixationChangeContrast(inputs).orElse {
        inputs.pvDelta.flatMap { delta =>
          delta.resolvedThreats.headOption.map(threat => s"Before the move, $threat was still on the board.")
            .orElse(delta.concessions.headOption.map(concession => s"The tradeoff is that $concession."))
        }
      }.orElse {
        material.localFileEntryPair.flatMap(localFileEntryChangeContrast)
      }.orElse {
        material.allowedPreventedPlans.collectFirst(Function.unlift(preventedPlanChangeContrast))
      }.orElse {
        if material.canPromoteDecisionComparisonChange then decisionComparisonChangeContrast(inputs.decisionComparison) else None
      }
    }.flatten

  private def whatChangedConsequence(
      inputs: QuestionPlannerInputs,
      material: WhatChangedPlannerMaterial
  ): Option[QuestionPlanConsequence] =
    Option.unless(material.supportedLocal) {
      exactTargetFixationChangeConsequence(inputs).orElse {
        inputs.pvDelta.flatMap(planAdvanceOrOpportunity).map(wrapUpConsequence)
      }.orElse {
        material.localFileEntryPair.flatMap(localFileEntryChangeConsequence).map(wrapUpConsequence)
      }.orElse {
        material.lineConsequenceClaim.map(wrapUpConsequence)
      }.orElse {
        material.allowedPreventedPlans.collectFirst(Function.unlift(preventedPlanChangeConsequence))
          .map(wrapUpConsequence)
      }.orElse {
        (if material.canPromoteDecisionComparisonChange then decisionComparisonChangeConsequence(inputs.decisionComparison) else None)
          .map(wrapUpConsequence)
      }
    }.flatten

  private def whatChangedSourceKinds(
      inputs: QuestionPlannerInputs,
      material: WhatChangedPlannerMaterial
  ): List[String] =
    material.moveOwner.toList.map(_.sourceKind) ++
      Option.when(material.exactTargetFixationChange.nonEmpty)("exact_target_fixation_delta").toList ++
      inputs.pvDelta.toList.map(_ => "pv_delta") ++
      Option.when(material.localFileEntryChange.nonEmpty || material.hasPreventedPlanChangeMaterial)("prevented_plan").toList ++
      material.lineConsequenceClaim.toList.map(_ => "line_consequence") ++
      material.localFactResult.toList.flatMap(localFactResultSourceKinds) ++
      material.pvCoupledPlanSupport.toList.map(_ => "pv_coupled_plan_support") ++
      inputs.decisionComparison.toList
        .filter(_ => material.decisionComparisonChange.nonEmpty)
        .map(_ => "decision_comparison")

  private def whatChangedPlannerOwnerKind(
      inputs: QuestionPlannerInputs,
      material: WhatChangedPlannerMaterial
  ): PlannerOwnerKind =
    if material.moveOwner.exists(tacticalClaimOwnsCurrentMove) then PlannerOwnerKind.ConcreteTactical
    else if material.localFactResult.nonEmpty then
      material.localFactResult.map(localFactResultPlannerOwnerKind).getOrElse(PlannerOwnerKind.MoveDelta)
    else if material.lineConsequenceClaim.nonEmpty then
      PlannerOwnerKind.LineConsequence
    else if material.decisionComparisonChange.nonEmpty &&
      inputs.pvDelta.isEmpty &&
      material.preventedPlanChange.isEmpty
    then PlannerOwnerKind.AlternativeComparison
    else if material.hasLocalFileEntryChange &&
      inputs.pvDelta.isEmpty &&
      material.moveOwner.isEmpty
    then PlannerOwnerKind.MoveDelta
    else if material.preventedPlanChange.nonEmpty &&
      inputs.pvDelta.isEmpty &&
      material.moveOwner.isEmpty
    then PlannerOwnerKind.ForcingDefense
    else PlannerOwnerKind.MoveDelta

  private def whatChangedPlannerSource(
      inputs: QuestionPlannerInputs,
      material: WhatChangedPlannerMaterial
  ): String =
    if material.moveOwner.exists(tacticalClaimOwnsCurrentMove) then
      material.moveOwner.map(_.sourceKind).getOrElse("move_delta")
    else if material.moveOwner.nonEmpty then
      material.moveOwner.map(_.sourceKind).getOrElse("move_delta")
    else if material.localFactResult.nonEmpty then
      material.localFactResult.map(localFactResultSource).getOrElse("typed_local_fact")
    else if hasConcreteMoveDeltaChange(inputs) then "pv_delta"
    else if material.hasLocalFileEntryChange || material.preventedPlanChange.nonEmpty then "prevented_plan"
    else if material.lineConsequenceClaim.nonEmpty then "line_consequence"
    else if material.pvCoupledPlanSupport.nonEmpty then "pv_coupled_plan_support"
    else if material.decisionComparisonChange.nonEmpty then "decision_comparison"
    else material.moveOwner.map(_.sourceKind).getOrElse("move_delta")

  private def decisionComparisonTimingClaim(
      comparison: Option[DecisionComparison]
  ): Option[String] =
    comparison.flatMap { value =>
      value.cpLossVsChosen
        .map(math.abs)
        .filter(_ >= 60)
        .map { loss =>
          comparisonAlternativeMove(value)
            .map(move => s"The timing matters now because drifting lets $move take over and costs about ${loss}cp.")
            .orElse(
              value.deferredReason
                .filter(_.trim.nonEmpty)
                .map(reason => s"The timing matters now because delaying costs about ${loss}cp: $reason.")
            )
            .getOrElse(s"The timing matters now because delaying costs about ${loss}cp.")
        }
    }

  private def decisionComparisonTimingContrast(
      comparison: Option[DecisionComparison]
  ): Option[String] =
    comparison.flatMap { value =>
      value.deferredReason
        .filter(_.trim.nonEmpty)
        .map(reason => s"If delayed, $reason.")
        .orElse(
          comparisonAlternativeMove(value)
            .map(move => s"If delayed, the cleaner version runs through $move.")
        )
    }

  private def decisionComparisonValueConsequence(
      comparison: Option[DecisionComparison]
  ): Option[QuestionPlanConsequence] =
    comparison.flatMap { value =>
      value.cpLossVsChosen.map(math.abs).filter(_ >= 60).map { loss =>
        wrapUpConsequence(s"That preserves roughly ${loss}cp of engine margin that drifting would give back.")
      }
    }

  private def buildWhatMustBeStoppedPlan(
      ctx: Option[NarrativeContext],
      question: AuthorQuestion,
      @unused ply: Int,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      sceneType: SceneType
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    whatMustBeStoppedMaterial(ctx, inputs, truthContract) match
      case None =>
        resolveDemotion(
          question = question,
          QuestionPlanFallbackMode.DemotedToWhyThis,
          demotedTo = AuthorQuestionKind.WhyThis,
          reasons = List("generic_opponent_plan_only"),
          fallbackBuild = buildWhyThisPlan(ctx, question, inputs, truthContract, sceneType)
        )
      case Some(material) =>
        buildWhatMustBeStoppedOwnedPlan(question, material)

  private def whatMustBeStoppedMaterial(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[WhatMustBeStoppedMaterial] =
    val urgentThreat = bestImmediateThreat(inputs.opponentThreats).filter(threatOwnerAllowedForMoveReview(ctx, _))
    val sanitizedThreat = urgentThreat.map(sanitizeThreatDefense(ctx, _))
    val preventedNow = preventedPlanNeedingStop(ctx, inputs, truthContract)
    val strongClaim = strongWhatMustBeStoppedClaimAllowed(sanitizedThreat, preventedNow, truthContract)
    whatMustBeStoppedClaim(sanitizedThreat, preventedNow, strongClaim).map { claim =>
      WhatMustBeStoppedMaterial(
        urgentThreat = sanitizedThreat,
        preventedPlan = preventedNow,
        claim = claim,
        strongClaim = strongClaim
      )
    }

  private def preventedPlanNeedingStop(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[PreventedPlanInfo] =
    preventedPlansAllowedForPlannerSurface(ctx, inputs, truthContract).find(plan =>
      plan.counterplayScoreDrop > 0 ||
        plan.breakNeutralized.exists(_.trim.nonEmpty) ||
        plan.preventedThreatType.exists(_.trim.nonEmpty)
    )

  private def whatMustBeStoppedClaim(
      urgentThreat: Option[ThreatRow],
      preventedPlan: Option[PreventedPlanInfo],
      strongClaim: Boolean
  ): Option[String] =
    val stopClaim =
      urgentThreat.map { threat =>
        s"This has to stop the opponent's ${threatDescription(threat)} before it lands."
      }.orElse(preventedPlan.flatMap(preventedPlanStopClaim))
    if strongClaim then stopClaim
    else urgentThreat.flatMap(weakerThreatClaim).orElse(preventedPlan.flatMap(weakerPreventedPlanClaim))

  private def strongWhatMustBeStoppedClaimAllowed(
      urgentThreat: Option[ThreatRow],
      preventedPlan: Option[PreventedPlanInfo],
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    truthContract.exists { contract =>
      contract.chosenMatchesBest &&
        contract.isCriticalBestMove &&
        (
          urgentThreat.exists(threat =>
            threat.bestDefense.exists(concreteDefenseLabel) &&
              (threat.isTopCandidateDefense || contract.reasonFamily == DecisiveReasonKind.OnlyMoveDefense)
          ) ||
            preventedPlan.exists(plan =>
              plan.breakNeutralized.exists(_.trim.nonEmpty) ||
                plan.preventedThreatType.exists(_.trim.nonEmpty) ||
                plan.counterplayScoreDrop >= 200
            )
        )
    }

  private def preventedPlanStopClaim(plan: PreventedPlanInfo): Option[String] =
    plan.breakNeutralized.map(file => s"This has to stop the opponent's $file-break before it starts.")
      .orElse(plan.preventedThreatType.map(kind => s"This has to stop the opponent's $kind before it becomes concrete."))
      .orElse(Option.when(plan.counterplayScoreDrop > 0)("This has to stop the opponent's easiest counterplay before it grows."))

  private def weakerThreatClaim(threat: ThreatRow): Option[String] =
    Some(s"This addresses the opponent's ${threatDescription(threat)} on the checked line.")

  private def threatDescription(threat: ThreatRow): String =
    val base = threatKindNoun(threat)
    val motif = specificThreatMotif(threat)
    val target = threatTargetPhrase(threat.targetPieces)
    val square = threatSquare(threat)
    motif match
      case Some(label) =>
        List(Some(label), target.map(tacticalThreatTargetClause(label, _)), square.map(s => s"on $s")).flatten.mkString(" ")
      case None =>
        List(Some(base), target.map(t => s"against $t"), square.map(s => s"on $s")).flatten.mkString(" ")

  private def threatKindNoun(threat: ThreatRow): String =
    val threatLabel = cleanLine(threat.kind).map(_.toLowerCase).getOrElse("threat")
    val targetLabels = threat.targetPieces.flatMap(threatPieceLabel).distinct
    if targetLabels.contains("king") && threatLabel == "material" then "forcing threat"
    else if threatLabel.endsWith("threat") then threatLabel
    else s"$threatLabel threat"

  private def specificThreatMotif(threat: ThreatRow): Option[String] =
    val targetBacked = threatTargetPhrase(threat.targetPieces).nonEmpty
    threat.motifs.iterator.map(normalizeText).collectFirst {
      case text if text.contains("backrank") => "back-rank mating threat"
      case text if text.contains("mate") => "mating threat"
      case text if text.contains("passedpawn") || text.contains("promotion") => "passed-pawn threat"
      case text if text.contains("capture") && targetBacked => "capture threat"
    }

  private def tacticalThreatTargetClause(label: String, target: String): String =
    label match
      case "deflection tactic" | "decoy tactic" | "overload tactic" | "interference tactic" | "zwischenzug" =>
        s"involving $target"
      case _ =>
        s"against $target"

  private def threatTargetPhrase(targets: List[String]): Option[String] =
    targets.flatMap(threatPieceLabel).distinct.take(2) match
      case Nil => None
      case one :: Nil => Some(s"the $one")
      case first :: second :: Nil => Some(s"the $first and $second")
      case _ => None

  private def threatPieceLabel(raw: String): Option[String] =
    cleanLine(raw).map(_.toLowerCase.replaceAll("[^a-z]", "")).flatMap {
      case "k" | "king" => Some("king")
      case "q" | "queen" => Some("queen")
      case "r" | "rook" => Some("rook")
      case "b" | "bishop" => Some("bishop")
      case "n" | "knight" => Some("knight")
      case "p" | "pawn" => Some("pawn")
      case _ => None
    }

  private def threatSquare(threat: ThreatRow): Option[String] =
    threat.square.flatMap(cleanLine).map(_.toLowerCase).filter(_.matches("""[a-h][1-8]"""))

  private def weakerPreventedPlanClaim(plan: PreventedPlanInfo): Option[String] =
    plan.breakNeutralized.map(file => s"This keeps the opponent's $file-break under control on the checked line.")
      .orElse(plan.preventedThreatType.map(kind => s"This reduces the opponent's $kind on the checked line."))
      .orElse(Option.when(plan.counterplayScoreDrop > 0)("This reduces the opponent's easiest counterplay on the checked line."))

  private def buildWhatMustBeStoppedOwnedPlan(
      question: AuthorQuestion,
      material: WhatMustBeStoppedMaterial
  )(
      using inputs: QuestionPlannerInputs
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    mkPlan(
      question = question,
      kind = AuthorQuestionKind.WhatMustBeStopped,
      claim = material.claim,
      evidence =
        evidenceForQuestion(
          question = question,
          fallbackLine = whatMustBeStoppedEvidenceFallbackLine(material, inputs),
          sourceKinds = List("threat", "prevented_plan")
        ),
      contrast = Option.when(material.strongClaim)(whatMustBeStoppedContrast(material)).flatten,
      consequence = whatMustBeStoppedConsequence(material),
      fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
      strengthTier = if material.strongClaim then QuestionPlanStrengthTier.Strong else QuestionPlanStrengthTier.Moderate,
      sourceKinds = material.sourceKinds,
      admissibilityReasons =
        if material.strongClaim then List("defensive_owner", "loss_if_ignored")
        else List("defensive_support", "truth_strength_limited"),
      plannerOwnerKind = PlannerOwnerKind.ForcingDefense,
      plannerSource = material.plannerSource,
      timingWitness = whatMustBeStoppedTimingWitness(material)
    )

  private def whatMustBeStoppedEvidenceFallbackLine(
      material: WhatMustBeStoppedMaterial,
      inputs: QuestionPlannerInputs
  ): Option[String] =
    material.preventedPlan.flatMap(_.citationLine)
      .orElse(inputs.mainBundle.flatMap(_.lineScopedClaim).map(_.claimText))

  private def whatMustBeStoppedContrast(material: WhatMustBeStoppedMaterial): Option[String] =
    material.urgentThreat.flatMap(_.bestDefense.filter(concreteDefenseLabel).map(defense => s"If the move is missed, $defense is forced."))
      .orElse(material.preventedPlan.flatMap(_.citationLine.map(line => s"If the move is missed, the line $line comes back.")))

  private def whatMustBeStoppedConsequence(material: WhatMustBeStoppedMaterial): Option[QuestionPlanConsequence] =
    material.preventedPlan.flatMap { plan =>
      Option.when(plan.counterplayScoreDrop > 0)(
        wrapUpConsequence(s"That keeps roughly ${plan.counterplayScoreDrop}cp of counterplay from appearing.")
      )
    }

  private def whatMustBeStoppedTimingWitness(material: WhatMustBeStoppedMaterial): Option[QuestionPlanTimingWitness] =
    if material.plannerSource == "threat" then material.urgentThreat.flatMap(neutralizeKeyBreakThreatTimingWitness)
    else material.preventedPlan.flatMap(neutralizeKeyBreakPreventedPlanTimingWitness)

  private def buildWhosePlanIsFasterPlan(
      ctx: Option[NarrativeContext],
      question: AuthorQuestion,
      ply: Int,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      sceneType: SceneType
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    val allowedPreventedPlans = preventedPlansAllowedForPlannerSurface(ctx, inputs, truthContract)
    val state =
      whosePlanRaceState(
        owner = ownerSideLabel(inputs, ply),
        inputs = inputs,
        allowedPreventedPlans = allowedPreventedPlans
      )
    state.material match
      case None =>
        buildMissingWhosePlanRaceFallback(ctx, question, ply, inputs, truthContract, sceneType, state)
      case Some(material) =>
        buildWhosePlanRacePlan(question, material)

  private def whosePlanRaceState(
      owner: String,
      inputs: QuestionPlannerInputs,
      allowedPreventedPlans: List[PreventedPlanInfo]
  ): WhosePlanRaceState =
    val urgentThreat = bestImmediateThreat(inputs.opponentThreats)
    val preventedBreak =
      allowedPreventedPlans.collectFirst {
        case plan if plan.breakNeutralized.exists(_.trim.nonEmpty) => plan.breakNeutralized.get
      }
    val intent = inputs.decisionFrame.intent.orElse(evidenceBackedRaceIntent(owner, inputs))
    val opponentPlan = inputs.opponentPlan.filter(plan => cleanLine(plan.name).nonEmpty)
    val opponentRace =
      opponentPlan.map(_.name)
        .orElse(urgentThreat.map(threat => s"the ${threat.kind.toLowerCase} threat"))
        .orElse(preventedBreak.map(file => s"the $file-break"))
    val urgencyRaceAnchor =
      opponentRace.flatMap(race => concreteRaceUrgency(inputs).map(anchor => s"$anchor before $race gets fully rolling"))
    WhosePlanRaceState(
      owner = owner,
      intent = intent,
      battlefront = inputs.decisionFrame.battlefront,
      opponentPlan = opponentPlan,
      opponentRace = opponentRace,
      raceAnchor =
        urgentThreat
          .map(threat => s"the reply window is short against the ${threat.kind.toLowerCase} threat")
          .orElse(preventedBreak.map(file => s"the $file-break is the timing window"))
          .orElse(urgencyRaceAnchor),
      opponentPressureAvailable = urgentThreat.nonEmpty || allowedPreventedPlans.nonEmpty,
      ownPlanAvailable = intent.nonEmpty || inputs.decisionFrame.battlefront.nonEmpty || inputs.evidenceBackedPlans.nonEmpty,
      intentReason =
        if inputs.decisionFrame.intent.nonEmpty then "certified_intent"
        else "probe_backed_plan_intent"
    )

  private def buildMissingWhosePlanRaceFallback(
      ctx: Option[NarrativeContext],
      question: AuthorQuestion,
      ply: Int,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      sceneType: SceneType,
      state: WhosePlanRaceState
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    if state.onlyOpponentPressure then
      resolveDemotion(
        question = question,
        QuestionPlanFallbackMode.DemotedToWhatMustBeStopped,
        demotedTo = AuthorQuestionKind.WhatMustBeStopped,
        reasons = List("missing_certified_race_pair"),
        fallbackBuild = buildWhatMustBeStoppedPlan(ctx, question, ply, inputs, truthContract, sceneType)
      )
    else if state.ownPlanAvailable then
      resolveDemotion(
        question = question,
        QuestionPlanFallbackMode.DemotedToWhyThis,
        demotedTo = AuthorQuestionKind.WhyThis,
        reasons = List("single_sided_plan_only"),
        fallbackBuild = buildWhyThisPlan(ctx, question, inputs, truthContract, sceneType)
      )
    else reject(question, QuestionPlanFallbackMode.FactualFallback, "missing_certified_race_pair")

  private def buildWhosePlanRacePlan(
      question: AuthorQuestion,
      material: WhosePlanRaceMaterial
  )(
      using QuestionPlannerInputs
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    mkPlan(
      question = question,
      kind = AuthorQuestionKind.WhosePlanIsFaster,
      claim = material.claim,
      evidence =
        evidenceForQuestion(
          question = question,
          fallbackLine = Some(material.battlefront.sentence),
          sourceKinds = List(material.intent.sourceKind, material.battlefront.sourceKind)
        ),
      contrast = Some(material.contrast),
      consequence = Some(wrapUpConsequence(material.consequenceText)),
      fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
      strengthTier = QuestionPlanStrengthTier.Moderate,
      sourceKinds = material.sourceKinds,
      admissibilityReasons = List(material.intentReason, "certified_battlefront", "timing_anchor"),
      plannerOwnerKind = PlannerOwnerKind.PlanRace,
      plannerSource = material.intent.sourceKind
    )

  private val EndgameTransitionPattern = raw"(.+)\((.+)\)\s*→\s*(.+)\((.+)\)".r

  private def ownerCandidate(
      plannerOwnerKind: PlannerOwnerKind,
      source: String,
      sourceKinds: List[String],
      questionKinds: List[AuthorQuestionKind],
      moveLinked: Boolean,
      materiality: OwnerCandidateMateriality = OwnerCandidateMateriality.OwnerCandidate,
      timingSource: Option[TimingSource] = None,
      decisionComparisonTimingDetail: Option[DecisionComparisonTimingDetail] = None,
      proposedOwnerMapping: String,
      reasons: List[String]
  ): OwnerCandidateTrace =
    OwnerCandidateTrace(
      plannerOwnerKind = plannerOwnerKind,
      source = source,
      sourceKinds = sourceKinds,
      questionKinds = questionKinds,
      moveLinked = moveLinked,
      materiality = materiality,
      timingSource = timingSource,
      decisionComparisonTimingDetail = decisionComparisonTimingDetail,
      proposedOwnerMapping = proposedOwnerMapping,
      reasons = reasons
    )

  private def singleSourceOwnerCandidate(
      plannerOwnerKind: PlannerOwnerKind,
      source: String,
      questionKinds: List[AuthorQuestionKind],
      proposedOwnerMapping: String,
      reasons: List[String],
      moveLinked: Boolean = true,
      materiality: OwnerCandidateMateriality = OwnerCandidateMateriality.OwnerCandidate,
      timingSource: Option[TimingSource] = None,
      decisionComparisonTimingDetail: Option[DecisionComparisonTimingDetail] = None
  ): OwnerCandidateTrace =
    ownerCandidate(
      plannerOwnerKind = plannerOwnerKind,
      source = source,
      sourceKinds = List(source),
      questionKinds = questionKinds,
      moveLinked = moveLinked,
      materiality = materiality,
      timingSource = timingSource,
      decisionComparisonTimingDetail = decisionComparisonTimingDetail,
      proposedOwnerMapping = proposedOwnerMapping,
      reasons = reasons
    )

  private def buildOwnerTrace(
      sceneTrace: SceneClassificationTrace,
      ownerCandidates: List[OwnerCandidateTrace],
      admitted: List[QuestionPlan],
      rejected: List[RejectedQuestionPlan],
      primary: Option[QuestionPlan]
  ): PlannerOwnerTrace =
    PlannerOwnerTrace(
      sceneType = sceneTrace.sceneType,
      sceneReasons = sceneTrace.reasons,
      ownerCandidates = ownerCandidates,
      admittedPlannerOwners = admittedPlannerOwnerTraces(ownerCandidates, admitted),
      droppedPlannerOwners = droppedPlannerOwnerTraces(ownerCandidates, admitted, rejected),
      demotionReasons = ownerTraceDemotionReasons(admitted, rejected, ownerCandidates),
      selectedQuestion = primary.map(_.questionKind),
      selectedPlannerOwnerKind = primary.map(_.plannerOwnerKind),
      selectedPlannerSource = primary.map(_.plannerSource)
    )

  private def admittedPlannerOwnerTraces(
      ownerCandidates: List[OwnerCandidateTrace],
      admitted: List[QuestionPlan]
  ): List[OwnerCandidateTrace] =
    ownerCandidates
      .filter(_.admissionDecision.contains(AdmissionDecision.PrimaryAllowed))
      .flatMap { candidate =>
        val relatedPlans = relatedAdmittedPlans(candidate, admitted)
        Option.when(relatedPlans.nonEmpty)(admittedPlannerOwnerTrace(candidate, relatedPlans))
      }

  private def admittedPlannerOwnerTrace(
      candidate: OwnerCandidateTrace,
      relatedPlans: List[QuestionPlan]
  ): OwnerCandidateTrace =
    candidate.copy(
      sourceKinds = (candidate.sourceKinds ++ relatedPlans.flatMap(_.sourceKinds)).distinct.sorted,
      questionKinds = (candidate.questionKinds ++ relatedPlans.map(_.questionKind)).distinct.sortBy(_.toString),
      reasons = (candidate.reasons ++ relatedPlans.flatMap(_.admissibilityReasons) ++ relatedPlans.flatMap(_.demotionReasons)).distinct.sorted
    )

  private def droppedPlannerOwnerTraces(
      ownerCandidates: List[OwnerCandidateTrace],
      admitted: List[QuestionPlan],
      rejected: List[RejectedQuestionPlan]
  ): List[DroppedPlannerOwnerTrace] =
    ownerCandidates
      .filterNot(candidate =>
        candidate.admissionDecision.contains(AdmissionDecision.PrimaryAllowed) &&
          relatedAdmittedPlans(candidate, admitted).nonEmpty
      )
      .map(candidate => droppedPlannerOwnerTrace(candidate, admitted, rejected))

  private def relatedAdmittedPlans(
      candidate: OwnerCandidateTrace,
      admitted: List[QuestionPlan]
  ): List[QuestionPlan] =
    admitted.filter(plan => plan.plannerOwnerKind == candidate.plannerOwnerKind && plan.plannerSource == candidate.source)

  private def droppedPlannerOwnerTrace(
      candidate: OwnerCandidateTrace,
      admitted: List[QuestionPlan],
      rejected: List[RejectedQuestionPlan]
  ): DroppedPlannerOwnerTrace =
    val relatedRejected =
      rejected.filter(rejectedPlan =>
        candidate.questionKinds.contains(rejectedPlan.questionKind) ||
          rejectedPlan.reasons.contains(candidate.admissionReason.getOrElse(""))
      )
    val primaryWithoutSurvivingPlan =
      candidate.admissionDecision.contains(AdmissionDecision.PrimaryAllowed) &&
        relatedAdmittedPlans(candidate, admitted).isEmpty
    val reasons =
      (relatedRejected.flatMap(_.reasons) ++
        relatedRejected.flatMap(_.demotionReasons) ++
        candidate.reasons ++
        candidate.admissionReason.toList ++
        candidate.admissionDecision.toList.map(_.wireName) ++
        Option.when(primaryWithoutSurvivingPlan)("primary_admission_without_surviving_plan").toList).distinct.sorted
    DroppedPlannerOwnerTrace(
      plannerOwnerKind = candidate.plannerOwnerKind,
      source = candidate.source,
      reasons = if reasons.nonEmpty then reasons else List("not_admitted"),
      questionKinds = relatedRejected.map(_.questionKind).distinct.sortBy(_.toString)
    )

  private def ownerTraceDemotionReasons(
      admitted: List[QuestionPlan],
      rejected: List[RejectedQuestionPlan],
      ownerCandidates: List[OwnerCandidateTrace]
  ): List[String] =
    (
      admitted.flatMap(_.demotionReasons) ++
        rejected.flatMap(_.demotionReasons) ++
        ownerCandidates
          .filterNot(_.admissionDecision.contains(AdmissionDecision.PrimaryAllowed))
          .flatMap(_.admissionReason)
    ).distinct.sorted

  private def admissionRank(decision: AdmissionDecision): Int =
    decision match
      case AdmissionDecision.PrimaryAllowed => 0
      case AdmissionDecision.SupportOnly    => 1
      case AdmissionDecision.Demote         => 2
      case AdmissionDecision.Forbidden      => 3

  private def admittedPlanDecision(
      sceneType: SceneType,
      candidate: Option[OwnerCandidateTrace],
      plan: QuestionPlan
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    candidate match
      case Some(ownerCandidate) if ownerCandidate.admissionDecision.contains(AdmissionDecision.PrimaryAllowed) =>
        Left(plan)
      case Some(ownerCandidate) =>
        Right(rejectByAdmission(plan, sceneType, ownerCandidate))
      case None =>
        Right(
          RejectedQuestionPlan(
            questionId = plan.questionId,
            questionKind = plan.questionKind,
            fallbackMode = QuestionPlanFallbackMode.Suppressed,
            reasons = List("admission_missing_owner_candidate", s"scene=${sceneType.wireName}"),
            demotionReasons = plan.demotionReasons
          )
        )

  private def applyStrategicReleasePolicy(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      plans: List[QuestionPlan]
  ): (List[QuestionPlan], List[RejectedQuestionPlan]) =
    plans.foldLeft((List.empty[QuestionPlan], List.empty[RejectedQuestionPlan])) {
      case ((kept, rejected), plan) =>
        releasePolicyDecision(ctx, inputs, truthContract, plan) match
          case Left(keptPlan)      => (kept :+ keptPlan, rejected)
          case Right(rejectedPlan) => (kept, rejected :+ rejectedPlan)
    }

  private def releasePolicyDecision(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      plan: QuestionPlan
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    ClaimAuthorityResolver.planAuthorityDecision(ctx, inputs, truthContract, plan) match
      case Some(decision) if decision.tier == ClaimAuthorityTier.Suppressed =>
        Right(suppressedStrategicRelease(plan, decision))
      case Some(decision) if decision.tier == ClaimAuthorityTier.SupportedLocal =>
        Left(supportedLocalStrategicRelease(plan))
      case None =>
        Left(plan)
      case Some(_) =>
        Left(plan)

  private def suppressedStrategicRelease(
      plan: QuestionPlan,
      decision: ClaimAuthorityDecision
  ): RejectedQuestionPlan =
    RejectedQuestionPlan(
      questionId = plan.questionId,
      questionKind = plan.questionKind,
      fallbackMode = QuestionPlanFallbackMode.FactualFallback,
      reasons =
        (
          List(
            "strategic_claim_tactical_veto",
            s"planner_owner=${plan.plannerOwnerKind.wireName}",
            s"planner_source=${plan.plannerSource}"
          ) ++ decision.vetoReasons
        ).distinct,
      demotionReasons = plan.demotionReasons
    )

  private def supportedLocalStrategicRelease(plan: QuestionPlan): QuestionPlan =
    plan.copy(
      claim = plan.claim,
      prefixKind =
        if plan.plannerSource == CentralBreakTimingWitness.ProofSource then plan.prefixKind
        else PlayerFacingClaimPrefixKind.SupportedLocal,
      evidence = None,
      contrast = None,
      consequence = None,
      fallbackMode = QuestionPlanFallbackMode.FactualFallback,
      strengthTier = QuestionPlanStrengthTier.Moderate,
      admissibilityReasons = (plan.admissibilityReasons :+ "strategic_claim_supported_local").distinct
    )

  private def rejectByAdmission(
      plan: QuestionPlan,
      sceneType: SceneType,
      candidate: OwnerCandidateTrace
  ): RejectedQuestionPlan =
    val decision = candidate.admissionDecision.getOrElse(AdmissionDecision.Forbidden)
    val reason = candidate.admissionReason.getOrElse("admission_filtered")
    val fallbackMode =
      candidate.demotedTo match
        case Some(AuthorQuestionKind.WhyThis)           => QuestionPlanFallbackMode.DemotedToWhyThis
        case Some(AuthorQuestionKind.WhatMustBeStopped) => QuestionPlanFallbackMode.DemotedToWhatMustBeStopped
        case _                                          => QuestionPlanFallbackMode.Suppressed
    RejectedQuestionPlan(
      questionId = plan.questionId,
      questionKind = plan.questionKind,
      fallbackMode = fallbackMode,
      reasons =
        List(
          s"admission_${decision.wireName}",
          reason,
          s"scene=${sceneType.wireName}",
          s"planner_owner=${plan.plannerOwnerKind.wireName}",
          s"planner_source=${plan.plannerSource}"
        ).distinct,
      demotedTo = candidate.demotedTo,
      demotionReasons = (plan.demotionReasons ++ candidate.demotedTo.toList.map(_ => reason)).distinct
    )

  private def applyAdmission(
      sceneType: SceneType,
      candidate: OwnerCandidateTrace
  ): OwnerCandidateTrace =
    val outcome = admissionOutcome(sceneType, candidate)
    candidate.copy(
      admissionDecision = Some(outcome.decision),
      admissionReason = Some(outcome.reason),
      demotedTo = outcome.demotedTo
    )

  private def admissionOutcome(
      sceneType: SceneType,
      candidate: OwnerCandidateTrace
  ): AdmissionOutcome =
    if candidate.materiality == OwnerCandidateMateriality.SupportMaterial then
      supportAdmission("support_material_not_owner_legal")
    else
      candidate.plannerOwnerKind match
        case PlannerOwnerKind.ConcreteTactical =>
          concreteTacticalAdmission(sceneType)
        case PlannerOwnerKind.LineConsequence =>
          lineConsequenceAdmission(sceneType)
        case PlannerOwnerKind.AlternativeComparison =>
          alternativeComparisonAdmission(sceneType)
        case PlannerOwnerKind.ForcingDefense =>
          forcingDefenseAdmission(sceneType)
        case PlannerOwnerKind.MoveDelta =>
          moveDeltaAdmission(sceneType, candidate)
        case PlannerOwnerKind.PositionProbe =>
          positionProbeAdmission(sceneType, candidate)
        case PlannerOwnerKind.DecisionTiming =>
          decisionTimingAdmission(sceneType, candidate)
        case PlannerOwnerKind.PlanRace =>
          planRaceAdmission(sceneType)
        case PlannerOwnerKind.OpeningRelation =>
          openingRelationAdmission(sceneType, candidate)
        case PlannerOwnerKind.EndgameTransition =>
          endgameTransitionAdmission(sceneType, candidate)

  private def primaryAdmission(reason: String): AdmissionOutcome =
    AdmissionOutcome(AdmissionDecision.PrimaryAllowed, reason)

  private def supportAdmission(reason: String): AdmissionOutcome =
    AdmissionOutcome(AdmissionDecision.SupportOnly, reason)

  private def demotedAdmission(reason: String, to: AuthorQuestionKind): AdmissionOutcome =
    AdmissionOutcome(AdmissionDecision.Demote, reason, demotedTo = Some(to))

  private def forbiddenAdmission(reason: String): AdmissionOutcome =
    AdmissionOutcome(AdmissionDecision.Forbidden, reason)

  private def concreteTacticalAdmission(sceneType: SceneType): AdmissionOutcome =
    if sceneType == SceneType.ConcreteTactical then primaryAdmission("concrete_tactical_primary_in_tactical_scene")
    else supportAdmission("concrete_tactical_support_only_outside_tactical_scene")

  private def lineConsequenceAdmission(sceneType: SceneType): AdmissionOutcome =
    sceneType match
      case SceneType.LineConsequence | SceneType.TransitionConversion =>
        primaryAdmission("line_consequence_primary_in_line_scene")
      case SceneType.ConcreteTactical | SceneType.ForcingDefense =>
        supportAdmission("line_consequence_support_only_under_forcing_or_tactical_scene")
      case _ =>
        supportAdmission("line_consequence_support_only_outside_line_scene")

  private def alternativeComparisonAdmission(sceneType: SceneType): AdmissionOutcome =
    sceneType match
      case SceneType.AlternativeComparison =>
        primaryAdmission("alternative_comparison_primary_in_alternative_scene")
      case SceneType.ConcreteTactical | SceneType.ForcingDefense | SceneType.LineConsequence =>
        supportAdmission("alternative_comparison_support_only_under_stronger_scene")
      case _ =>
        supportAdmission("alternative_comparison_support_only_outside_alternative_scene")

  private def forcingDefenseAdmission(sceneType: SceneType): AdmissionOutcome =
    sceneType match
      case SceneType.ForcingDefense => primaryAdmission("forcing_defense_primary_in_forcing_scene")
      case SceneType.ConcreteTactical | SceneType.LineConsequence | SceneType.AlternativeComparison |
          SceneType.PlanClash | SceneType.TransitionConversion |
          SceneType.EndgameTransition =>
        supportAdmission("forcing_defense_support_only_outside_forcing_scene")
      case _ => forbiddenAdmission("forcing_defense_not_legal_in_current_scene")

  private def moveDeltaAdmission(
      sceneType: SceneType,
      candidate: OwnerCandidateTrace
  ): AdmissionOutcome =
    sceneType match
      case SceneType.QuietImprovement | SceneType.TransitionConversion =>
        primaryAdmission("move_delta_primary_in_quiet_or_conversion_scene")
      case SceneType.OpeningRelation if candidate.source == "pv_coupled_plan_support" =>
        primaryAdmission("pv_coupled_plan_support_primary_in_opening_relation_scene")
      case SceneType.ForcingDefense
          if candidate.source == PlayerFacingTruthModePolicy.DefenderTradeProofSource =>
        primaryAdmission("supported_local_defender_trade_non_tactical_forcing_scene")
      case _ => supportAdmission("move_delta_support_only_outside_quiet_or_conversion_scene")

  private def positionProbeAdmission(
      sceneType: SceneType,
      candidate: OwnerCandidateTrace
  ): AdmissionOutcome =
    sceneType match
      case SceneType.QuietImprovement =>
        primaryAdmission("position_probe_primary_in_quiet_scene")
      case SceneType.ForcingDefense
          if candidate.source == PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource &&
            candidate.reasons.contains("certified_position_probe") =>
        primaryAdmission("certified_position_probe_non_tactical_forcing_scene")
      case _ =>
        supportAdmission("position_probe_support_only_outside_quiet_scene")

  private def decisionTimingAdmission(
      sceneType: SceneType,
      candidate: OwnerCandidateTrace
  ): AdmissionOutcome =
    candidate.timingSource match
      case Some(TimingSource.CloseCandidate) =>
        supportAdmission("decision_timing_close_candidate_support_only")
      case Some(TimingSource.RelationWitness) =>
        sceneType match
          case SceneType.QuietImprovement | SceneType.TransitionConversion =>
            primaryAdmission("relation_witness_timing_primary_in_quiet_or_conversion_scene")
          case SceneType.ConcreteTactical =>
            demotedAdmission("relation_witness_timing_demoted_under_concrete_tactical", AuthorQuestionKind.WhyThis)
          case _ =>
            supportAdmission("relation_witness_timing_support_only_under_stronger_scene")
      case Some(TimingSource.DecisionComparison) =>
        decisionTimingSupportedSourceAdmission(sceneType)
      case Some(TimingSource.PreventedResource | TimingSource.OnlyMove) =>
        decisionTimingSupportedSourceAdmission(sceneType)
      case _ =>
        forbiddenAdmission("decision_timing_missing_supported_source")

  private def decisionTimingSupportedSourceAdmission(sceneType: SceneType): AdmissionOutcome =
    sceneType match
      case SceneType.ConcreteTactical =>
        demotedAdmission("decision_timing_demoted_under_concrete_tactical", AuthorQuestionKind.WhyThis)
      case _ =>
        supportAdmission("decision_timing_support_only")

  private def planRaceAdmission(sceneType: SceneType): AdmissionOutcome =
    sceneType match
      case SceneType.PlanClash => primaryAdmission("plan_race_primary_in_plan_clash")
      case SceneType.ForcingDefense =>
        demotedAdmission("plan_race_demoted_under_forcing_defense", AuthorQuestionKind.WhatMustBeStopped)
      case _ =>
        demotedAdmission("plan_race_demoted_outside_plan_clash", AuthorQuestionKind.WhyThis)

  private def openingRelationAdmission(
      sceneType: SceneType,
      candidate: OwnerCandidateTrace
  ): AdmissionOutcome =
    if !candidate.moveLinked || candidate.source == "opening_precedent_summary" then
      supportAdmission("opening_relation_raw_summary_support_only")
    else if candidate.source == "opening_relation_translator" then
      supportAdmission("opening_relation_translator_support_only_without_causal_contrast")
    else
      sceneType match
        case SceneType.OpeningRelation => primaryAdmission("opening_relation_primary_in_opening_scene")
        case SceneType.TransitionConversion => supportAdmission("opening_relation_support_only_under_conversion")
        case _ => forbiddenAdmission("opening_relation_not_legal_in_current_scene")

  private def endgameTransitionAdmission(
      sceneType: SceneType,
      candidate: OwnerCandidateTrace
  ): AdmissionOutcome =
    if !candidate.moveLinked || candidate.source == "endgame_theoretical_hint" then
      supportAdmission("endgame_transition_raw_hint_support_only")
    else
      sceneType match
        case SceneType.EndgameTransition => primaryAdmission("endgame_transition_primary_in_endgame_scene")
        case SceneType.TransitionConversion => supportAdmission("endgame_transition_support_only_under_conversion")
        case _ => forbiddenAdmission("endgame_transition_not_legal_in_current_scene")

  private def rawOwnerCandidates(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): List[OwnerCandidateTrace] =
    val allowedPreventedPlans = preventedPlansAllowedForPlannerSurface(ctx, inputs, truthContract)
    val rawGroups =
      List(
        causalMechanismOwnerCandidates(inputs, truthContract),
        forcingDefenseOwnerCandidates(ctx, inputs, truthContract, allowedPreventedPlans),
        positionProbeOwnerCandidates(ctx, inputs, truthContract),
        moveDeltaOwnerCandidates(inputs, truthContract),
        decisionTimingOwnerCandidates(inputs, truthContract),
        planRaceOwnerCandidates(inputs),
        shadowDomainSignals(ctx, inputs)
      )
    mergeOwnerCandidates(rawGroups.flatten)

  private def causalMechanismOwnerCandidates(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): List[OwnerCandidateTrace] =
    import AuthorQuestionKind.*
    import PlannerOwnerKind.*

    val riskGateBlocksStrategicSupport =
      truthContract.exists(_.blocksStrategicSupport)
    val hasLineConsequence =
      inputs.lineConsequence.exists(evidence => evidence.surfaceReady && evidence.kind != LineConsequenceKind.PreviewOnly)
    val hasAlternativeComparison = certifiedAlternativeComparisonAvailable(inputs, truthContract)
    val alternativeComparisonSourceKind =
      inputs.decisionComparison.map(certifiedAlternativeComparisonSource)
        .getOrElse(DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource)

    List(
      Option.when(hasLineConsequence) {
        singleSourceOwnerCandidate(
          plannerOwnerKind = LineConsequence,
          source = "line_consequence",
          questionKinds = List(WhatChanged),
          proposedOwnerMapping = "LineConsequence/played_move",
          reasons =
            List("pv_line_consequence") ++
              Option.when(riskGateBlocksStrategicSupport)("risk_gate_blocks_strategic_support")
        )
      },
      Option.when(hasAlternativeComparison) {
        singleSourceOwnerCandidate(
          plannerOwnerKind = AlternativeComparison,
          source = "decision_comparison",
          questionKinds = List(WhyThis, WhatChanged),
          proposedOwnerMapping = s"AlternativeComparison/$alternativeComparisonSourceKind",
          reasons =
            List(
              alternativeComparisonSourceKind,
              "branch_scoped_alternative"
            ) ++ roleAwareAlternativeGateReasons(truthContract)
        )
      }
    ).flatten

  private def forcingDefenseOwnerCandidates(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      allowedPreventedPlans: List[PreventedPlanInfo]
  ): List[OwnerCandidateTrace] =
    import AuthorQuestionKind.*
    import PlannerOwnerKind.*

    val onlyMove = onlyMovePressure(truthContract)
    def forcingCandidate(
        source: String,
        questionKinds: List[AuthorQuestionKind],
        reason: String
    ): OwnerCandidateTrace =
      singleSourceOwnerCandidate(
        plannerOwnerKind = ForcingDefense,
        source = source,
        questionKinds = questionKinds,
        proposedOwnerMapping = "ForcingDefense/move_linked",
        reasons = List(reason)
      )

    List(
      Option.unless(prefersQuietMoveDeltaIngress(inputs, truthContract)) {
        bestImmediateThreat(inputs.opponentThreats).filter(threatOwnerAllowedForMoveReview(ctx, _)).map { _ =>
          forcingCandidate("threat", List(WhyNow, WhatMustBeStopped), "urgent_threat")
        }
      }.flatten,
      Option.unless(prefersRestrictedSuppressionMoveDeltaIngress(inputs)) {
        allowedPreventedPlans
          .find(plan =>
            preventedPlanTimingClaim(plan).nonEmpty ||
              preventedPlanChangeClaim(plan).nonEmpty
          )
          .map { _ =>
            forcingCandidate(
              source = "prevented_plan",
              questionKinds = List(WhyNow, WhatChanged, WhatMustBeStopped),
              reason = "prevented_resource"
            )
          }
      }.flatten,
      onlyMove.map { _ =>
        forcingCandidate("only_move_defense", List(WhyNow), "only_move_defense")
      }
    ).flatten

  private def positionProbeOwnerCandidates(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): List[OwnerCandidateTrace] =
    inputs.mainBundle.flatMap(_.mainClaim)
      .filter(_.scope == PlayerFacingClaimScope.PositionLocal)
      .toList
      .flatMap(claim => positionProbeOwnerCandidate(ctx, inputs, truthContract, claim))

  private def positionProbeOwnerCandidate(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      claim: MainPathScopedClaim
  ): Option[OwnerCandidateTrace] =
    claim.packet
      .map(packet => packet -> ClaimAuthorityResolver.decidePositionProbe(ctx, inputs, truthContract, packet))
      .filter { case (_, decision) => decision.admitted }
      .map { case (packet, decision) =>
        ownerCandidate(
          plannerOwnerKind = PlannerOwnerKind.PositionProbe,
          source = packet.proofSource,
          sourceKinds = List(claim.sourceKind, packet.proofSource).distinct,
          questionKinds = List(AuthorQuestionKind.WhatMattersHere),
          moveLinked = false,
          proposedOwnerMapping = "PositionProbe/position_local",
          reasons = positionProbeOwnerReasons(decision)
        )
      }

  private def positionProbeOwnerReasons(decision: ClaimAuthorityDecision): List[String] =
    List("current_position_probe") ++
      Option.when(decision.tier == ClaimAuthorityTier.CertifiedOwner)("certified_position_probe").toList ++
      Option.when(decision.tier == ClaimAuthorityTier.SupportedLocal)("strategic_claim_supported_local").toList

  private def planRaceOwnerCandidates(inputs: QuestionPlannerInputs): List[OwnerCandidateTrace] =
    Option.when(hasPlanRaceCandidate(inputs)) {
      ownerCandidate(
        plannerOwnerKind = PlannerOwnerKind.PlanRace,
        source = planRaceOwnerSource(inputs),
        sourceKinds = planRaceOwnerSourceKinds(inputs),
        questionKinds = List(AuthorQuestionKind.WhosePlanIsFaster),
        moveLinked = true,
        proposedOwnerMapping = "PlanRace/move_linked",
        reasons = List("certified_plan_race")
      )
    }.toList

  private def planRaceOwnerSource(inputs: QuestionPlannerInputs): String =
    inputs.decisionFrame.intent.map(_.sourceKind)
      .orElse(Option.when(inputs.evidenceBackedPlans.nonEmpty)("evidence_backed_plan"))
      .orElse(inputs.decisionFrame.battlefront.map(_.sourceKind))
      .getOrElse("plan_race")

  private def planRaceOwnerSourceKinds(inputs: QuestionPlannerInputs): List[String] =
    List(
      inputs.decisionFrame.intent.map(_.sourceKind),
      inputs.decisionFrame.battlefront.map(_.sourceKind),
      Option.when(inputs.opponentPlan.nonEmpty)("opponent_plan")
    ).flatten.distinct

  private def mergeOwnerCandidates(candidates: List[OwnerCandidateTrace]): List[OwnerCandidateTrace] =
    candidates
      .groupBy(_.key)
      .toList
      .sortBy { case ((plannerOwnerKind, source, materiality, timingSource), _) =>
        (plannerOwnerKind.wireName, source, materiality.wireName, timingSource.map(_.wireName).getOrElse(""))
      }
      .map { case (_, traces) =>
        traces.reduce { (left, right) =>
          left.copy(
            sourceKinds = (left.sourceKinds ++ right.sourceKinds).distinct.sorted,
            questionKinds = (left.questionKinds ++ right.questionKinds).distinct.sortBy(_.toString),
            reasons = (left.reasons ++ right.reasons).distinct.sorted
          )
        }
      }

  private def moveDeltaOwnerCandidates(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): List[OwnerCandidateTrace] =
    import AuthorQuestionKind.*
    import PlannerOwnerKind.*

    val whyThisOrWhatChanged = List(WhyThis, WhatChanged)
    val hasAlternativeComparison = certifiedAlternativeComparisonAvailable(inputs, truthContract)
    def moveLinkedCandidate(
        plannerOwnerKind: PlannerOwnerKind,
        source: String,
        questionKinds: List[AuthorQuestionKind],
        proposedOwnerMapping: String,
        reason: String,
        materiality: OwnerCandidateMateriality = OwnerCandidateMateriality.OwnerCandidate
    ): OwnerCandidateTrace =
      singleSourceOwnerCandidate(
        plannerOwnerKind = plannerOwnerKind,
        source = source,
        questionKinds = questionKinds,
        proposedOwnerMapping = proposedOwnerMapping,
        reasons = List(reason),
        materiality = materiality
      )

    List(
      inputs.mainBundle.flatMap { bundle =>
        bundle.mainClaim.filter(_.scope == PlayerFacingClaimScope.MoveLocal).orElse(
          bundle.lineScopedClaim.filter(claim =>
            claim.packet.exists(_.fallbackMode == PlayerFacingClaimFallbackMode.WeakMain)
          )
        )
      }.map { claim =>
        val tactical = tacticalClaimOwnsCurrentMove(claim)
        val tacticalWithoutOwnership = claim.mode == PlayerFacingTruthMode.Tactical && !tactical
        moveLinkedCandidate(
          plannerOwnerKind = if tactical then ConcreteTactical else MoveDelta,
          source = claim.sourceKind,
          questionKinds = whyThisOrWhatChanged,
          proposedOwnerMapping =
            if tactical then "ConcreteTactical/move_linked"
            else if tacticalWithoutOwnership then "MoveDelta/support_only"
            else "MoveDelta/move_linked",
          reason =
            if tacticalWithoutOwnership then "tactical_claim_lacks_current_move_ownership"
            else "main_move_claim",
          materiality =
            if tacticalWithoutOwnership then OwnerCandidateMateriality.SupportMaterial
            else OwnerCandidateMateriality.OwnerCandidate
        )
      },
      inputs.localFactResult
        .filter(localFactResultWhyThisEligible)
        .map { result =>
          val ownerKind = localFactResultPlannerOwnerKind(result)
          val checkOnlySupport =
            checkOnlyTacticalLocalFact(result) && hasAlternativeComparison
          moveLinkedCandidate(
            plannerOwnerKind = ownerKind,
            source = localFactResultSource(result),
            questionKinds = if forkEntryDefenseLocalFact(result) then List(WhyThis) else whyThisOrWhatChanged,
            proposedOwnerMapping = s"${ownerKind.wireName}/typed_local_fact",
            reason = if checkOnlySupport then "check_only_support_under_alternative_comparison" else "typed_local_fact",
            materiality =
              if checkOnlySupport then OwnerCandidateMateriality.SupportMaterial
              else OwnerCandidateMateriality.OwnerCandidate
          )
        },
      inputs.quietIntent.map { intent =>
        moveLinkedCandidate(
          plannerOwnerKind = MoveDelta,
          source = intent.sourceKind,
          questionKinds = whyThisOrWhatChanged,
          proposedOwnerMapping = "MoveDelta/move_linked",
          reason = "quiet_move_claim"
        )
      },
      inputs.pvDelta.map { _ =>
        moveLinkedCandidate(
          plannerOwnerKind = MoveDelta,
          source = "pv_delta",
          questionKinds = List(WhatChanged),
          proposedOwnerMapping = "MoveDelta/move_linked",
          reason = "move_local_delta"
        )
      },
      Option
        .unless(truthContract.exists(_.blocksStrategicSupport))(inputs.pvCoupledPlanSupport)
        .flatten
        .filter(_.anchorMatched)
        .map { _ =>
          moveLinkedCandidate(
            plannerOwnerKind = MoveDelta,
            source = "pv_coupled_plan_support",
            questionKinds = List(WhatChanged),
            proposedOwnerMapping = "MoveDelta/pv_coupled_plan_support",
            reason = "pv_coupled_plan_support"
          )
        }
    ).flatten

  private[analysis] def localFactResultWhyThisEligible(result: MoveReviewExplanationBuilder.Result): Boolean =
    (
      Set(
        MoveReviewLocalFact.Family.Attack,
        MoveReviewLocalFact.Family.Threat,
        MoveReviewLocalFact.Family.Pressure,
        MoveReviewLocalFact.Family.PlanSupport
      ).contains(result.localFact.family) ||
        openingGoalLocalFact(result) ||
        strictLineConsequenceLocalFact(result) ||
        practicalCentralChallengeLocalFact(result) ||
        centralBreakTimingLocalFact(result) ||
        boardBackedTargetDefenseLocalFact(result) ||
        forkEntryDefenseLocalFact(result) ||
        (
          result.localFact.family == MoveReviewLocalFact.Family.Defense &&
            (
              result.localFact.producer == MoveReviewLocalFact.Producer.CertifiedStrategyDelta ||
                localFactResultDrawResourceDefense(result)
            )
        )
    ) &&
      result.explanation.prose.trim.nonEmpty

  private def certifiedAlternativeComparisonAvailable(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    inputs.decisionComparison.exists(comparison =>
      certifiedAlternativeComparisonConsequence(comparison).nonEmpty &&
        certifiedAlternativeComparisonAccepted(comparison, truthContract)
    )

  private def checkOnlyTacticalLocalFact(result: MoveReviewExplanationBuilder.Result): Boolean =
    result.localFact.family == MoveReviewLocalFact.Family.Threat &&
      result.localFact.producer == MoveReviewLocalFact.Producer.TacticalMotif &&
      result.localFact.authority == MoveReviewLocalFact.Authority.CanonicalFact &&
      result.localFact.lineBinding == MoveReviewLocalFact.LineBinding.PvCoupled &&
      result.localFact.anchors.exists(anchor => anchor.key == "tactical_kind" && anchor.value == "check")

  private def localFactResultDrawResourceDefense(result: MoveReviewExplanationBuilder.Result): Boolean =
    result.localFact.family == MoveReviewLocalFact.Family.Defense &&
      result.localFact.producer == MoveReviewLocalFact.Producer.RelationWitness &&
      result.localFact.relationSurface.contains(RelationSurfaceRowKind.DrawResource) &&
      result.localFact.lineBinding == MoveReviewLocalFact.LineBinding.PvCoupled

  private def openingGoalLocalFact(result: MoveReviewExplanationBuilder.Result): Boolean =
    result.localFact.family == MoveReviewLocalFact.Family.OpeningGoal &&
      result.localFact.authority == MoveReviewLocalFact.Authority.OpeningGoalEvidence &&
      result.localFact.producer == MoveReviewLocalFact.Producer.OpeningGoal &&
      result.localFact.lineBinding == MoveReviewLocalFact.LineBinding.PvCoupled &&
      result.localFact.strictFallbackEligible

  private def boardBackedTargetDefenseLocalFact(result: MoveReviewExplanationBuilder.Result): Boolean =
    result.localFact.family == MoveReviewLocalFact.Family.Defense &&
      result.localFact.producer == MoveReviewLocalFact.Producer.TargetDefense &&
      result.localFact.authority == MoveReviewLocalFact.Authority.CanonicalFact &&
      result.localFact.lineBinding == MoveReviewLocalFact.LineBinding.PvCoupled &&
      result.localFact.guardrails.contains("target_fact_defended_by_played_move")

  private def forkEntryDefenseLocalFact(result: MoveReviewExplanationBuilder.Result): Boolean =
    result.localFact.family == MoveReviewLocalFact.Family.Defense &&
      result.localFact.producer == MoveReviewLocalFact.Producer.ForkEntryDefense &&
      result.localFact.authority == MoveReviewLocalFact.Authority.CanonicalFact &&
      result.localFact.lineBinding == MoveReviewLocalFact.LineBinding.PvCoupled &&
      result.localFact.guardrails.contains("fork_entry_square_defended_by_played_move")

  private def practicalCentralChallengeLocalFact(result: MoveReviewExplanationBuilder.Result): Boolean =
    result.localFact.family == MoveReviewLocalFact.Family.LineConsequence &&
      result.localFact.producer == MoveReviewLocalFact.Producer.LineConsequence &&
      localFactResultSource(result) == "practical_central_challenge"

  private def strictLineConsequenceLocalFact(result: MoveReviewExplanationBuilder.Result): Boolean =
    result.localFact.family == MoveReviewLocalFact.Family.LineConsequence &&
      result.localFact.strictFallbackEligible &&
      result.localFact.lineBinding == MoveReviewLocalFact.LineBinding.PvCoupled &&
      (
        result.localFact.producer == MoveReviewLocalFact.Producer.LineConsequence ||
          result.localFact.producer == MoveReviewLocalFact.Producer.CertifiedStrategyDelta
      )

  private def centralBreakTimingLocalFact(result: MoveReviewExplanationBuilder.Result): Boolean =
    result.localFact.family == MoveReviewLocalFact.Family.Timing &&
      result.localFact.authority == MoveReviewLocalFact.Authority.CertifiedStrategy &&
      result.localFact.producer == MoveReviewLocalFact.Producer.CertifiedStrategyDelta &&
      result.localFact.lineBinding == MoveReviewLocalFact.LineBinding.PvCoupled &&
      result.localFact.evidenceRefs.exists(_ == "proof_family:central_break_timing") &&
      result.localFact.evidenceRefs.exists(_ == "proof_source:central_break_timing")

  private[analysis] def localFactResultPlannerOwnerKind(result: MoveReviewExplanationBuilder.Result): PlannerOwnerKind =
    result.localFact.family match
      case MoveReviewLocalFact.Family.Attack | MoveReviewLocalFact.Family.Threat =>
        PlannerOwnerKind.ConcreteTactical
      case MoveReviewLocalFact.Family.Pressure
          if result.localFact.producer == MoveReviewLocalFact.Producer.RelationWitness &&
            result.localFact.relationSurface.contains(RelationSurfaceRowKind.TacticalRelation) =>
        PlannerOwnerKind.ConcreteTactical
      case MoveReviewLocalFact.Family.Pressure
          if result.localFact.producer == MoveReviewLocalFact.Producer.RelationWitness =>
        PlannerOwnerKind.MoveDelta
      case MoveReviewLocalFact.Family.LineConsequence =>
        PlannerOwnerKind.LineConsequence
      case MoveReviewLocalFact.Family.Defense
          if result.localFact.producer == MoveReviewLocalFact.Producer.ForkEntryDefense =>
        PlannerOwnerKind.ForcingDefense
      case _ =>
        PlannerOwnerKind.MoveDelta

  private[analysis] def localFactResultSource(result: MoveReviewExplanationBuilder.Result): String =
    Option(result.explanation.source).map(_.trim).filter(_.nonEmpty)
      .getOrElse(result.localFact.producer.key)

  private[analysis] def localFactResultSourceKinds(result: MoveReviewExplanationBuilder.Result): List[String] =
    List(
      "typed_local_fact",
      localFactResultSource(result),
      result.localFact.producer.key,
      s"local_fact_family:${result.localFact.family.key}"
    ).distinct

  private def decisionTimingOwnerCandidates(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): List[OwnerCandidateTrace] =
    import AuthorQuestionKind.*

    val whyNowOrWhatChanged = List(WhyNow, WhatChanged)
    directDecisionTimingOwnerCandidate(inputs, whyNowOrWhatChanged).toList ++
      supportDecisionTimingOwnerCandidates(inputs, truthContract, whyNowOrWhatChanged)

  private def directDecisionTimingOwnerCandidate(
      inputs: QuestionPlannerInputs,
      questionKinds: List[AuthorQuestionKind]
  ): Option[OwnerCandidateTrace] =
    val hasDecisionTimingOrChange =
      decisionComparisonTimingClaim(inputs.decisionComparison).nonEmpty ||
        inputs.decisionComparison.exists(value =>
          certifiedAlternativeComparisonConsequence(value).isEmpty &&
            decisionComparisonChangeClaim(Some(value)).nonEmpty
        )
    Option.when(
      hasDecisionTimingOrChange
    ) {
      val detail =
        inputs.decisionComparison
          .map(decisionComparisonTimingDetail)
          .getOrElse(DecisionComparisonTimingDetail.BareEngineGap)
      singleSourceOwnerCandidate(
        plannerOwnerKind = PlannerOwnerKind.DecisionTiming,
        source = "decision_comparison",
        questionKinds = questionKinds,
        timingSource = Some(TimingSource.DecisionComparison),
        decisionComparisonTimingDetail = Some(detail),
        proposedOwnerMapping = s"DecisionTiming/${detail.wireName}",
        reasons = List("timing_loss", detail.wireName)
      )
    }

  private def supportDecisionTimingOwnerCandidates(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      questionKinds: List[AuthorQuestionKind]
  ): List[OwnerCandidateTrace] =
    List(
      inputs.localFactResult
        .filter(localFactResultTimingEligible)
        .map { result =>
          singleSourceOwnerCandidate(
            plannerOwnerKind = PlannerOwnerKind.DecisionTiming,
            source = localFactResultSource(result),
            questionKinds = List(AuthorQuestionKind.WhyNow),
            timingSource = Some(TimingSource.RelationWitness),
            proposedOwnerMapping = "DecisionTiming/typed_local_fact",
            reasons = List("typed_local_fact_timing", result.localFact.producer.key)
          )
        },
      preventedPlansWhenLocalBindAllowed(inputs)
        .find(plan => preventedPlanTimingClaim(plan).nonEmpty)
        .map { _ =>
          singleSourceOwnerCandidate(
            plannerOwnerKind = PlannerOwnerKind.DecisionTiming,
            source = "prevented_plan",
            questionKinds = questionKinds,
            timingSource = Some(TimingSource.PreventedResource),
            proposedOwnerMapping = "DecisionTiming/move_linked",
            reasons = List("prevented_resource_timing"),
            materiality = OwnerCandidateMateriality.SupportMaterial
          )
        },
      onlyMovePressure(truthContract).map { _ =>
        singleSourceOwnerCandidate(
          plannerOwnerKind = PlannerOwnerKind.DecisionTiming,
          source = "only_move_defense",
          questionKinds = List(AuthorQuestionKind.WhyNow),
          timingSource = Some(TimingSource.OnlyMove),
          proposedOwnerMapping = "DecisionTiming/move_linked",
          reasons = List("only_move_timing")
        )
      }
    ).flatten

  private def localFactResultTimingEligible(result: MoveReviewExplanationBuilder.Result): Boolean =
    result.localFact.family == MoveReviewLocalFact.Family.Timing &&
      result.localFact.producer == MoveReviewLocalFact.Producer.RelationWitness &&
      result.localFact.relationSurface.contains(RelationSurfaceRowKind.MoveOrder) &&
      result.explanation.prose.trim.nonEmpty

  private def shadowDomainSignals(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs
  ): List[OwnerCandidateTrace] =
    decisionComparisonShadowSignals(inputs) ++
      openingShadowSignals(ctx, inputs) ++
      endgameShadowSignals(ctx, inputs)

  private def decisionComparisonShadowSignals(inputs: QuestionPlannerInputs): List[OwnerCandidateTrace] =
    inputs.alternativeNarrative
      .filter(_.source == "close_candidate")
      .map { alternative =>
        val enriched =
          practical.ContrastiveSupportAdmissibility.enrichedCloseCandidateSentence(alternative.sentence)
        ownerCandidate(
          plannerOwnerKind = PlannerOwnerKind.DecisionTiming,
          source = "close_candidate",
          sourceKinds =
            if enriched then List("alternative_narrative", "close_candidate", "enriched_close_candidate")
            else List("alternative_narrative", "close_candidate"),
          questionKinds = List(
            AuthorQuestionKind.WhyThis,
            AuthorQuestionKind.WhyNow,
            AuthorQuestionKind.WhatChanged
          ),
          moveLinked = false,
          materiality = OwnerCandidateMateriality.SupportMaterial,
          timingSource = Some(TimingSource.CloseCandidate),
          proposedOwnerMapping = "DecisionTiming/support_only",
          reasons = List(if enriched then "enriched_close_candidate" else "raw_close_alternative")
        )
      }
      .toList

  private def openingShadowSignals(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs
  ): List[OwnerCandidateTrace] =
    List(
      inputs.openingRelationClaim.map { _ =>
        ownerCandidate(
          plannerOwnerKind = PlannerOwnerKind.OpeningRelation,
          source = "opening_relation_translator",
          sourceKinds = List("opening_relation_translator"),
          questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
          moveLinked = true,
          proposedOwnerMapping = "OpeningRelation/move_linked",
          reasons = List("opening_relation_translated")
        )
      },
      ctx.flatMap { narrativeCtx =>
        OpeningPrecedentBranching.summarySentence(narrativeCtx, narrativeCtx.openingData, requireFocus = false)
      }.map { _ =>
        ownerCandidate(
          plannerOwnerKind = PlannerOwnerKind.OpeningRelation,
          source = "opening_precedent_summary",
          sourceKinds = List("opening_precedent_summary"),
          questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
          moveLinked = false,
          materiality = OwnerCandidateMateriality.SupportMaterial,
          proposedOwnerMapping = "OpeningRelation/support_only",
          reasons = List("raw_opening_precedent_summary")
        )
      }
    ).flatten

  private def endgameShadowSignals(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs
  ): List[OwnerCandidateTrace] =
    List(
      inputs.endgameTransitionClaim.map { _ =>
        ownerCandidate(
          plannerOwnerKind = PlannerOwnerKind.EndgameTransition,
          source = "endgame_transition_translator",
          sourceKinds = List("endgame_transition_translator"),
          questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
          moveLinked = true,
          proposedOwnerMapping = "EndgameTransition/move_linked",
          reasons = List("endgame_transition_translated")
        )
      },
      ctx.flatMap(_.semantic.flatMap(_.endgameFeatures)).flatMap(rawEndgameHint).map { _ =>
        ownerCandidate(
          plannerOwnerKind = PlannerOwnerKind.EndgameTransition,
          source = "endgame_theoretical_hint",
          sourceKinds = List("endgame_theoretical_hint", "endgame_oracle"),
          questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
          moveLinked = false,
          materiality = OwnerCandidateMateriality.SupportMaterial,
          proposedOwnerMapping = "EndgameTransition/support_only",
          reasons = List("raw_endgame_hint")
        )
      }
    ).flatten

  private def rawEndgameHint(info: EndgameInfo): Option[String] =
    info.primaryPattern
      .flatMap(cleanLine)
      .orElse(info.transition.flatMap(cleanLine))

  private[commentary] def endgameTransitionSentence(info: EndgameInfo): Option[String] =
    info.transition.flatMap {
      case EndgameTransitionPattern(fromRaw, _, toRaw, _) =>
        val fromLabel = humanizeEndgamePattern(fromRaw)
        val toLabel = humanizeEndgamePattern(toRaw)
        Some(
          if fromRaw.equalsIgnoreCase("none") then
            s"A new endgame structure is visible around $toLabel, so the context has changed."
          else if toRaw.equalsIgnoreCase("none") then
            s"The earlier endgame structure around $fromLabel has dissolved, so that context is no longer stable by itself."
          else
            s"The endgame structure has shifted from $fromLabel to $toLabel."
        )
      case _ => None
    }.orElse {
      info.primaryPattern.flatMap { pattern =>
        Option.when(info.patternAge >= 2) {
          val label = humanizeEndgamePattern(pattern)
          s"The $label has stayed visible, keeping that endgame context in view."
        }
      }
    }

  private def humanizeEndgamePattern(raw: String): String =
    val normalized = Option(raw).getOrElse("").trim
    if normalized.isEmpty then "endgame shape"
    else if normalized.equalsIgnoreCase("none") then "no stable endgame shape"
    else if normalized.equalsIgnoreCase("Lucena") then "bridge-building rook-pawn shape"
    else if normalized.equalsIgnoreCase("PhilidorDefense") then "barrier-rank rook defense"
    else if normalized.equalsIgnoreCase("VancuraDefense") then "side-checking rook defense"
    else if normalized.equalsIgnoreCase("WrongRookPawnWrongBishopFortress") then "wrong-corner rook-pawn setup"
    else if normalized.equalsIgnoreCase("OutsidePasserDecoy") then "outside-passer decoy shape"
    else if normalized.equalsIgnoreCase("ConnectedPassers") then "connected-passer shape"
    else if normalized.equalsIgnoreCase("KeySquaresOppositionBreakthrough") then "king-and-key-square breakthrough shape"
    else if normalized.equalsIgnoreCase("TriangulationZugzwang") then "triangulation-tempo shape"
    else if normalized.equalsIgnoreCase("BreakthroughSacrifice") then "pawn-breakthrough shape"
    else if normalized.equalsIgnoreCase("Shouldering") then "king-shouldering shape"
    else if normalized.equalsIgnoreCase("RetiManeuver") then "king-race pursuit shape"
    else if normalized.equalsIgnoreCase("ShortSideDefense") then "short-side rook defense"
    else if normalized.equalsIgnoreCase("OppositeColoredBishopsDraw") then "opposite-colored bishops blockade"
    else if normalized.equalsIgnoreCase("GoodBishopRookPawnConversion") then "bishop-and-rook-pawn setup"
    else if normalized.equalsIgnoreCase("KnightBlockadeRookPawnDraw") then "knight rook-pawn blockade"
    else if normalized.equalsIgnoreCase("QueenVsAdvancedPawn") then "queen-versus-advanced-pawn checking setup"
    else if normalized.equalsIgnoreCase("TarraschDefenseActive") then "active rook-behind-pawn setup"
    else if normalized.equalsIgnoreCase("PassiveRookDefense") then "passive rook-behind-pawn setup"
    else if normalized.equalsIgnoreCase("RookAndBishopVsRookDraw") then "rook-and-bishop versus rook defensive geometry"
    else if normalized.equalsIgnoreCase("SameColoredBishopsBlockade") then "same-colored bishops blockade"
    else
      "endgame shape"

  private def hasPlanRaceCandidate(inputs: QuestionPlannerInputs): Boolean =
    val opponentRaceAvailable =
      inputs.opponentPlan.exists(plan => cleanLine(plan.name).nonEmpty) ||
        bestImmediateThreat(inputs.opponentThreats).nonEmpty
    val ownRaceAvailable =
      inputs.decisionFrame.intent.nonEmpty ||
        (inputs.decisionFrame.battlefront.nonEmpty && inputs.evidenceBackedPlans.nonEmpty)
    val timingAnchorAvailable = concreteRaceUrgency(inputs).nonEmpty || bestImmediateThreat(inputs.opponentThreats).nonEmpty
    ownRaceAvailable && opponentRaceAvailable && timingAnchorAvailable

  private def hasDomainTransitionOverlap(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      families: Set[PlannerOwnerKind]
  ): Boolean =
    val pairedTranslators =
      families.contains(PlannerOwnerKind.OpeningRelation) &&
        families.contains(PlannerOwnerKind.EndgameTransition)
    val moveTransitionAnchor =
      families.contains(PlannerOwnerKind.MoveDelta) ||
        truthContract.exists(_.reasonFamily == DecisiveReasonKind.Conversion) ||
        hasTransitionAnchor(inputs)
    pairedTranslators && moveTransitionAnchor

  private def classifySceneTrace(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      candidates: List[OwnerCandidateTrace]
  ): SceneClassificationTrace =
    val signals = sceneClassificationSignals(inputs, truthContract, candidates)
    if signals.contains(PlannerOwnerKind.ConcreteTactical) then
      SceneClassificationTrace(
        sceneType = SceneType.ConcreteTactical,
        reasons = List("mechanism=ConcreteTactical")
      )
    else if signals.contains(PlannerOwnerKind.PlanRace) then
      SceneClassificationTrace(
        sceneType = SceneType.PlanClash,
        reasons = List("proof_family=PlanRace")
      )
    else if signals.contains(PlannerOwnerKind.ForcingDefense) then
      SceneClassificationTrace(
        sceneType = SceneType.ForcingDefense,
        reasons =
          List("proof_family=ForcingDefense") ++
            truthContract
              .filter(_.reasonFamily == DecisiveReasonKind.OnlyMoveDefense)
              .map(_ => "truth_reason=OnlyMoveDefense")
      )
    else if signals.contains(PlannerOwnerKind.AlternativeComparison) then
      SceneClassificationTrace(
        sceneType = SceneType.AlternativeComparison,
        reasons = List("mechanism=AlternativeComparison")
      )
    else if signals.contains(PlannerOwnerKind.LineConsequence) then
      SceneClassificationTrace(
        sceneType = SceneType.LineConsequence,
        reasons = List("mechanism=LineConsequence")
      )
    else if signals.hasTransitionConversion then
      SceneClassificationTrace(
        sceneType = SceneType.TransitionConversion,
        reasons = signals.transitionReasons
      )
    else if signals.contains(PlannerOwnerKind.OpeningRelation) then
      SceneClassificationTrace(
        sceneType = SceneType.OpeningRelation,
        reasons = List("proof_family=OpeningRelation")
      )
    else if signals.contains(PlannerOwnerKind.EndgameTransition) then
      SceneClassificationTrace(
        sceneType = SceneType.EndgameTransition,
        reasons = List("proof_family=EndgameTransition")
      )
    else
      SceneClassificationTrace(
        sceneType = SceneType.QuietImprovement,
        reasons = List("default_quiet_improvement")
      )

  private def sceneClassificationSignals(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      candidates: List[OwnerCandidateTrace]
  ): SceneClassificationSignals =
    val families = candidates.filterNot(_.supportMaterial).map(_.plannerOwnerKind).toSet
    SceneClassificationSignals(
      families = families,
      hasTransitionAnchor = hasTransitionAnchor(inputs),
      hasTranslatorOverlap = hasDomainTransitionOverlap(inputs, truthContract, families),
      truthSignalsConversion = truthContract.exists(_.reasonFamily == DecisiveReasonKind.Conversion)
    )

  private def hasTransitionAnchor(inputs: QuestionPlannerInputs): Boolean =
    inputs.pvDelta.exists(delta =>
      delta.resolvedThreats.nonEmpty || delta.newOpportunities.nonEmpty || delta.planAdvancements.nonEmpty
    )

  private def evidenceBackedRaceIntent(
      owner: String,
      inputs: QuestionPlannerInputs
  ): Option[CertifiedDecisionSupport] =
    inputs.evidenceBackedPlans
      .sortBy(plan => (-plan.score, plan.rank))
      .collectFirst(Function.unlift { plan =>
        cleanLine(plan.planName)
          .filter(isConcreteRacePlanName)
          .map(name =>
            CertifiedDecisionSupport(
              axis = CertifiedDecisionFrameAxis.Intent,
              sentence = s"$owner is playing for ${name.toLowerCase}.",
              priority = 78,
              sourceKind = "evidence_backed_plan"
            )
          )
      })

  private def concreteRaceUrgency(inputs: QuestionPlannerInputs): Option[String] =
    inputs.decisionFrame.urgency
      .filterNot(_.sourceKind == "slow_truth_mode")
      .map { urgency =>
        if urgency.sourceKind == "tactical_truth_mode" then "the tactical window is short"
        else "the timing window is short"
      }
      .orElse {
        inputs.practicalAssessment
          .filter(_.biasFactors.nonEmpty)
          .map(_ => "the practical window is short")
      }

  private def isConcreteRacePlanName(name: String): Boolean =
    val low = name.toLowerCase
    low.length >= 8 &&
    !List(
      "plan",
      "idea",
      "improvement",
      "development",
      "play"
    ).contains(low)
