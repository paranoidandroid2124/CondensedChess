package lila.llm.analysis

import lila.llm.StrategyPack
import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.strategic.CounterfactualMatch
import scala.annotation.unused

private[llm] enum QuestionPlanFallbackMode:
  case PlannerOwned
  case DemotedToWhyThis
  case DemotedToWhatMustBeStopped
  case FactualFallback
  case Suppressed

private[llm] enum QuestionPlanStrengthTier:
  case Strong
  case Moderate
  case Exact

private[llm] enum QuestionPlanConsequenceBeat:
  case WrapUp
  case TeachingPoint

private[llm] final case class QuestionPlanEvidence(
    text: String,
    purposes: List[String],
    sourceKinds: List[String],
    sourceIds: List[String] = Nil,
    branchScoped: Boolean = false
)

private[llm] final case class QuestionPlanConsequence(
    text: String,
    beat: QuestionPlanConsequenceBeat,
    certified: Boolean = true
)

private[llm] enum SceneType:
  case ForcingDefense
  case TacticalFailure
  case QuietImprovement
  case TransitionConversion
  case PlanClash
  case OpeningRelation
  case EndgameTransition

  def wireName: String =
    this match
      case ForcingDefense      => "forcing_defense"
      case TacticalFailure     => "tactical_failure"
      case QuietImprovement    => "quiet_improvement"
      case TransitionConversion => "transition_conversion"
      case PlanClash           => "plan_clash"
      case OpeningRelation     => "opening_relation"
      case EndgameTransition   => "endgame_transition"

private[llm] enum OwnerFamily:
  case TacticalFailure
  case ForcingDefense
  case MoveDelta
  case PositionProbe
  case DecisionTiming
  case PlanRace
  case OpeningRelation
  case EndgameTransition

  def wireName: String =
    this match
      case TacticalFailure   => "TacticalFailure"
      case ForcingDefense    => "ForcingDefense"
      case MoveDelta         => "MoveDelta"
      case PositionProbe     => "PositionProbe"
      case DecisionTiming    => "DecisionTiming"
      case PlanRace          => "PlanRace"
      case OpeningRelation   => "OpeningRelation"
      case EndgameTransition => "EndgameTransition"

private[llm] enum OwnerCandidateMateriality:
  case OwnerCandidate
  case SupportMaterial

  def wireName: String =
    this match
      case OwnerCandidate  => "owner_candidate"
      case SupportMaterial => "support_material"

private[llm] enum TimingSource:
  case DecisionComparison
  case CloseCandidate
  case PreventedResource
  case OnlyMove

  def wireName: String =
    this match
      case DecisionComparison => "decision_comparison"
      case CloseCandidate     => "close_candidate"
      case PreventedResource  => "prevented_resource"
      case OnlyMove           => "only_move"

private[llm] enum DecisionComparisonTimingDetail:
  case ConcreteReplyOrReason
  case BareEngineGap

  def wireName: String =
    this match
      case ConcreteReplyOrReason => "concrete_reply_or_reason"
      case BareEngineGap         => "bare_engine_gap"

private[llm] enum AdmissionDecision:
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

private[llm] final case class OwnerCandidateTrace(
    family: OwnerFamily,
    source: String,
    sourceKinds: List[String],
    questionKinds: List[AuthorQuestionKind],
    moveLinked: Boolean,
    materiality: OwnerCandidateMateriality,
    timingSource: Option[TimingSource],
    decisionComparisonTimingDetail: Option[DecisionComparisonTimingDetail],
    proposedFamilyMapping: String,
    reasons: List[String],
    admissionDecision: Option[AdmissionDecision] = None,
    admissionReason: Option[String] = None,
    demotedTo: Option[AuthorQuestionKind] = None
):
  def supportMaterial: Boolean =
    materiality == OwnerCandidateMateriality.SupportMaterial

  def key: (OwnerFamily, String, OwnerCandidateMateriality, Option[TimingSource]) =
    (family, source, materiality, timingSource)

  def render: String =
    (
      List(
        family.wireName,
        s"source_kind=$source",
        s"materiality=${materiality.wireName}",
        s"move_linked=$moveLinked",
        s"support_material=$supportMaterial",
        s"mapping=$proposedFamilyMapping",
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

private[llm] final case class DroppedOwnerFamilyTrace(
    family: OwnerFamily,
    source: String,
    reasons: List[String],
    questionKinds: List[AuthorQuestionKind]
):
  def render: String =
    List(
      family.wireName,
      source,
      questionKinds.map(_.toString).distinct.sorted.mkString("+"),
      reasons.distinct.sorted.mkString("+")
    ).filter(_.nonEmpty).mkString(":")

private[llm] final case class PlannerOwnerTrace(
    sceneType: SceneType = SceneType.QuietImprovement,
    sceneReasons: List[String] = Nil,
    ownerCandidates: List[OwnerCandidateTrace] = Nil,
    admittedFamilies: List[OwnerCandidateTrace] = Nil,
    droppedFamilies: List[DroppedOwnerFamilyTrace] = Nil,
    demotionReasons: List[String] = Nil,
    selectedQuestion: Option[AuthorQuestionKind] = None,
    selectedOwnerFamily: Option[OwnerFamily] = None,
    selectedOwnerSource: Option[String] = None
):
  def ownerCandidateLabels: List[String] =
    ownerCandidates.map(_.render)

  def admittedFamilyLabels: List[String] =
    admittedFamilies.map(_.render)

  def droppedFamilyLabels: List[String] =
    droppedFamilies.map(_.render)

  def supportMaterialSeparationLabels: List[String] =
    ownerCandidates.map(candidate =>
      s"${candidate.source}:${candidate.materiality.wireName}" +
        candidate.timingSource.fold("")(source => s":timing_source=${source.wireName}") +
        candidate.decisionComparisonTimingDetail.fold("")(detail =>
          s":decision_comparison_detail=${detail.wireName}"
        )
    )

  def proposedFamilyMappingLabels: List[String] =
    ownerCandidates.map(candidate =>
      s"${candidate.source}->${candidate.family.wireName}:${candidate.proposedFamilyMapping}:${candidate.materiality.wireName}" +
        candidate.timingSource.fold("")(source => s":timing_source=${source.wireName}") +
        candidate.decisionComparisonTimingDetail.fold("")(detail =>
          s":decision_comparison_detail=${detail.wireName}"
        )
    )

private[llm] final case class QuestionPlan(
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
    ownerFamily: OwnerFamily,
    ownerSource: String,
    demotionReasons: List[String] = Nil
)

private[llm] final case class RejectedQuestionPlan(
    questionId: String,
    questionKind: AuthorQuestionKind,
    fallbackMode: QuestionPlanFallbackMode,
    reasons: List[String],
    demotedTo: Option[AuthorQuestionKind] = None,
    demotionReasons: List[String] = Nil
)

private[llm] final case class RankedQuestionPlans(
    primary: Option[QuestionPlan],
    secondary: Option[QuestionPlan],
    rejected: List[RejectedQuestionPlan],
    ownerTrace: PlannerOwnerTrace = PlannerOwnerTrace()
)

private[llm] final case class SceneClassificationTrace(
    sceneType: SceneType,
    reasons: List[String]
)

private[llm] final case class AdmissionOutcome(
    decision: AdmissionDecision,
    reason: String,
    demotedTo: Option[AuthorQuestionKind] = None
)

private[llm] final case class QuestionPlannerInputs(
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
    namedRouteNetworkSurface: Option[NamedRouteNetworkBindCertification.SurfaceNetwork] = None
)

private[llm] enum WhyNowTimingOwner:
  case Threat(threat: ThreatRow)
  case Prevented(plan: PreventedPlanInfo)
  case OnlyMove(reason: String)
  case DecisionComparisonOwner(comparison: DecisionComparison)

private[llm] object QuestionPlannerInputsBuilder:

  def build(
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract],
      candidateEvidenceLines: List[String] = Nil
  ): QuestionPlannerInputs =
    val decisionComparisonRaw = DecisionComparisonBuilder.build(ctx)
    val decisionComparison =
      truthContract
        .flatMap(contract => DecisiveTruth.sanitizeDecisionComparison(decisionComparisonRaw, contract))
        .orElse(decisionComparisonRaw)
    val authorEvidenceLines =
      ctx.authorEvidence.flatMap(_.branches).flatMap(branchDisplayLine)
    val comparisonEvidence = decisionComparison.flatMap(_.evidence).toList
    val cleanedEvidenceLines =
      (candidateEvidenceLines ++ authorEvidenceLines ++ comparisonEvidence)
        .flatMap(cleanLine)
        .distinct
    val mainBundle =
      MainPathMoveDeltaClaimBuilder.build(ctx, strategyPack, truthContract, cleanedEvidenceLines)
    val comparativeDecisionComparison =
      DecisionComparisonComparativeSupport.enrich(
        comparison = decisionComparison,
        ctx = ctx,
        strategyPack = strategyPack,
        truthContract = truthContract,
        candidateEvidenceLines = cleanedEvidenceLines,
        mainBundleOverride = mainBundle
      )
    val quietIntent =
      Option.when(mainBundle.isEmpty) {
        QuietMoveIntentBuilder.build(ctx, cleanedEvidenceLines)
      }.flatten
    val decisionFrame =
      CertifiedDecisionFrameBuilder.build(
        ctx = ctx,
        strategyPack = strategyPack,
        truthContract = truthContract,
        mainBundle = mainBundle,
        quietIntent = quietIntent
      )
    val preventedPlansNow =
      ctx.semantic.toList.flatMap(_.preventedPlans).filter(_.sourceScope == FactScope.Now)
    val evidenceBackedPlans =
      StrategicNarrativePlanSupport.evidenceBackedMainPlans(ctx)
    val heavyPieceLocalBindBlocked =
      HeavyPieceLocalBindValidation.blocksPlayerFacingShell(ctx)
    val namedRouteNetworkSurface =
      Option.unless(heavyPieceLocalBindBlocked) {
        NamedRouteNetworkBindCertification.certifiedSurfaceNetwork(
          preventedPlans = preventedPlansNow,
          evidenceBackedPlans = evidenceBackedPlans
        )
      }.flatten

    QuestionPlannerInputs(
      mainBundle = mainBundle,
      quietIntent = quietIntent,
      decisionFrame = decisionFrame,
      decisionComparison = comparativeDecisionComparison,
      alternativeNarrative = AlternativeNarrativeSupport.build(ctx),
      truthMode = PlayerFacingTruthModePolicy.classify(ctx, strategyPack, truthContract),
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
      factualFallback = QuietMoveIntentBuilder.exactFactualSentence(ctx),
      heavyPieceLocalBindBlocked = heavyPieceLocalBindBlocked,
      openingRelationClaim = QuestionFirstCommentaryPlanner.openingRelationReplayClaim(ctx),
      endgameTransitionClaim = QuestionFirstCommentaryPlanner.endgameTransitionReplayClaim(ctx),
      namedRouteNetworkSurface = namedRouteNetworkSurface
    )

  private def branchDisplayLine(branch: EvidenceBranch): Option[String] =
    LineScopedCitation
      .evidenceBranchDisplayLine(branch)
      .orElse(cleanLine(branch.line))

  private def cleanLine(raw: String): Option[String] =
    Option(raw)
      .map(_.trim.replaceAll("\\s+", " "))
      .filter(_.nonEmpty)

private[llm] object QuestionFirstCommentaryPlanner:

  private val PlannerLinePurpose = "planner_line_proof"

  private[llm] def openingRelationReplayClaim(ctx: NarrativeContext): Option[String] =
    OpeningPrecedentBranching
      .relationSentence(ctx, ctx.openingData, requireFocus = false)
      .flatMap(cleanLine)

  private[llm] def endgameTransitionReplayClaim(ctx: NarrativeContext): Option[String] =
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
    val evaluated =
      authorQuestions.map(question =>
        evaluateQuestion(question, ply, hydratedInputs, truthContract, sceneType)
      )
    val evaluatedAdmitted = evaluated.collect { case Left(value) => value }
    val evaluationRejected = evaluated.collect { case Right(value) => value }
    val ownerCandidateIndex =
      ownerCandidates
        .filter(_.materiality == OwnerCandidateMateriality.OwnerCandidate)
        .groupBy(candidate => candidate.family -> candidate.source)
        .view
        .mapValues(_.sortBy(candidate =>
          (
            admissionRank(candidate.admissionDecision.getOrElse(AdmissionDecision.Forbidden)),
            candidate.timingSource.map(_.wireName).getOrElse(""),
            candidate.reasons.mkString("+")
          )
        ).head)
        .toMap
    val (admitted, admissionRejected) =
      evaluatedAdmitted.foldLeft((List.empty[QuestionPlan], List.empty[RejectedQuestionPlan])) {
          case ((kept, dropped), plan) =>
            admittedPlanDecision(sceneType, ownerCandidateIndex.get(plan.ownerFamily -> plan.ownerSource), plan) match
              case Left(admittedPlan)    => (kept :+ admittedPlan, dropped)
              case Right(rejectedPlan) => (kept, dropped :+ rejectedPlan)
        }
    val rejected = evaluationRejected ++ admissionRejected
    val ranked = admitted.sortBy(plan => planScore(sceneType, plan)).reverse
    val primary = ranked.headOption
    val secondary = ranked.drop(1).find(plan => secondaryAllowed(primary, plan))
    val ownerTrace = buildOwnerTrace(sceneTrace, ownerCandidates, admitted, rejected, primary)
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
    concreteWhyNowTimingOwner(inputs, truthContract).nonEmpty

  private def evaluateQuestion(
      question: AuthorQuestion,
      ply: Int,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      sceneType: SceneType
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    question.kind match
      case AuthorQuestionKind.WhatMattersHere =>
        buildWhatMattersHerePlan(question, inputs, truthContract, sceneType)
      case AuthorQuestionKind.WhyThis =>
        buildWhyThisPlan(question, inputs, truthContract, sceneType)
      case AuthorQuestionKind.WhyNow =>
        buildWhyNowPlan(question, ply, inputs, truthContract, sceneType)
      case AuthorQuestionKind.WhatChanged =>
        buildWhatChangedPlan(question, inputs, truthContract, sceneType)
      case AuthorQuestionKind.WhatMustBeStopped =>
        buildWhatMustBeStoppedPlan(question, ply, inputs, truthContract, sceneType)
      case AuthorQuestionKind.WhosePlanIsFaster =>
        buildWhosePlanIsFasterPlan(question, ply, inputs, truthContract, sceneType)

  private def planScore(sceneType: SceneType, plan: QuestionPlan): (Int, Int, Int, Int, Int, Int, Int, Int) =
    (
      exactStateDeltaPriority(plan),
      questionSuitability(sceneType, plan),
      strengthScore(plan.strengthTier),
      plan.contrast.fold(0)(_ => 1),
      plan.consequence.fold(0)(_ => 1),
      plan.evidence.fold(0)(evidence => evidenceQuality(evidence)),
      plan.priority,
      tacticalSeverity(plan)
    )

  private def exactStateDeltaPriority(plan: QuestionPlan): Int =
    Option.when(
      plan.questionKind == AuthorQuestionKind.WhatChanged &&
        plan.admissibilityReasons.contains("exact_target_state_delta") &&
        (plan.contrast.nonEmpty || plan.consequence.nonEmpty)
    )(1).getOrElse(0)

  private def questionSuitability(
      sceneType: SceneType,
      plan: QuestionPlan
  ): Int =
    sceneType match
      case SceneType.TacticalFailure =>
        plan.questionKind match
          case AuthorQuestionKind.WhatMattersHere => 1
          case AuthorQuestionKind.WhyThis           => 5
          case AuthorQuestionKind.WhatChanged       => 4
          case AuthorQuestionKind.WhatMustBeStopped => 3
          case AuthorQuestionKind.WhyNow            => 2
          case AuthorQuestionKind.WhosePlanIsFaster => 1
      case SceneType.ForcingDefense =>
        plan.questionKind match
          case AuthorQuestionKind.WhatMattersHere => 1
          case AuthorQuestionKind.WhatMustBeStopped => 5
          case AuthorQuestionKind.WhyNow            => 4
          case AuthorQuestionKind.WhatChanged       => 3
          case AuthorQuestionKind.WhyThis           => 2
          case AuthorQuestionKind.WhosePlanIsFaster => 1
      case SceneType.PlanClash =>
        plan.questionKind match
          case AuthorQuestionKind.WhatMattersHere => 1
          case AuthorQuestionKind.WhosePlanIsFaster => 5
          case AuthorQuestionKind.WhyThis           => 4
          case AuthorQuestionKind.WhyNow            => 3
          case AuthorQuestionKind.WhatChanged       => 2
          case AuthorQuestionKind.WhatMustBeStopped => 1
      case SceneType.TransitionConversion =>
        plan.questionKind match
          case AuthorQuestionKind.WhatMattersHere => 2
          case AuthorQuestionKind.WhatChanged       => 5
          case AuthorQuestionKind.WhyThis           => 4
          case AuthorQuestionKind.WhyNow            => 3
          case AuthorQuestionKind.WhatMustBeStopped => 2
          case AuthorQuestionKind.WhosePlanIsFaster => 1
      case SceneType.OpeningRelation =>
        plan.questionKind match
          case AuthorQuestionKind.WhatMattersHere => 1
          case AuthorQuestionKind.WhyThis           => 5
          case AuthorQuestionKind.WhatChanged       => 4
          case AuthorQuestionKind.WhyNow            => 3
          case AuthorQuestionKind.WhatMustBeStopped => 2
          case AuthorQuestionKind.WhosePlanIsFaster => 1
      case SceneType.EndgameTransition =>
        plan.questionKind match
          case AuthorQuestionKind.WhatMattersHere => 1
          case AuthorQuestionKind.WhatChanged       => 5
          case AuthorQuestionKind.WhyThis           => 4
          case AuthorQuestionKind.WhyNow            => 3
          case AuthorQuestionKind.WhatMustBeStopped => 2
          case AuthorQuestionKind.WhosePlanIsFaster => 1
      case SceneType.QuietImprovement =>
        plan.questionKind match
          case AuthorQuestionKind.WhatMattersHere => 6
          case AuthorQuestionKind.WhyThis           => 5
          case AuthorQuestionKind.WhatChanged       => 4
          case AuthorQuestionKind.WhyNow            => 3
          case AuthorQuestionKind.WhatMustBeStopped => 2
          case AuthorQuestionKind.WhosePlanIsFaster => 1

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
    """(?m)^[a-z]\)\s+""".r.findAllMatchIn(Option(text).getOrElse("")).size

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
      primary.ownerFamily == OwnerFamily.MoveDelta &&
      candidate.ownerFamily == OwnerFamily.MoveDelta &&
      primary.ownerSource == candidate.ownerSource

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
    cleanLine(text).map { line =>
      QuestionPlanEvidence(
        text = s"a) $line",
        purposes = List(PlannerLinePurpose),
        sourceKinds = sourceKinds,
        branchScoped = true
      )
    }

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
      ownerFamily: OwnerFamily,
      ownerSource: String,
      demotionReasons: List[String] = Nil
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
            ownerFamily = ownerFamily,
            ownerSource = ownerSource,
            demotionReasons = demotionReasons.distinct
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

  private def cleanLine(raw: String): Option[String] =
    Option(raw).map(_.trim.replaceAll("\\s+", " ")).filter(_.nonEmpty)

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
      (contract.reasonFamily == DecisiveReasonFamily.InvestmentSacrifice ||
        contract.reasonFamily == DecisiveReasonFamily.Conversion)

  private def prefersQuietMoveDeltaIngress(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    inputs.pvDelta.nonEmpty &&
      truthContract.forall { contract =>
        val quietMoveDeltaFamily =
          contract.reasonFamily match
          case DecisiveReasonFamily.InvestmentSacrifice |
              DecisiveReasonFamily.QuietTechnicalMove |
              DecisiveReasonFamily.Conversion =>
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
      normalizedId(plan.themeL1) == ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id &&
      plan.subplanId.exists { subplanId =>
        val normalizedSubplan = normalizedId(subplanId)
        normalizedSubplan == ThemeTaxonomy.SubplanId.ProphylaxisRestraint.id ||
        normalizedSubplan == ThemeTaxonomy.SubplanId.BreakPrevention.id ||
        normalizedSubplan == ThemeTaxonomy.SubplanId.KeySquareDenial.id
      }
    }

  private def plannerBestHoldNeedsForcingOwner(
      inputs: QuestionPlannerInputs,
      contract: DecisiveTruthContract
  ): Boolean =
    contract.truthClass == DecisiveTruthClass.Best &&
      contract.reasonFamily == DecisiveReasonFamily.TacticalRefutation &&
      contract.chosenMatchesBest &&
      contract.failureMode == FailureInterpretationMode.NoClearPlan &&
      !hasConcreteMoveDeltaChange(inputs)

  private def onlyMovePressure(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[String] =
    truthContract.flatMap { contract =>
      Option.when(
        contract.isCriticalBestMove ||
          contract.reasonFamily == DecisiveReasonFamily.OnlyMoveDefense ||
          plannerBestHoldNeedsForcingOwner(inputs, contract)
      ) {
        contract.benchmarkMove match
          case Some(best) => s"Other moves allow the position to slip away; the benchmark move is $best"
          case None       => "Other moves allow the position to slip away"
      }
    }

  private def planAdvanceConsequence(delta: PVDelta): Option[String] =
    delta.planAdvancements.headOption.map(step => s"That changes the next phase of the position by making $step available.")

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
      inputs: QuestionPlannerInputs
  ): Option[String] =
    Option.unless(inputs.heavyPieceLocalBindBlocked) {
      LocalFileEntryBindCertification
        .certifiedSurfacePair(inputs.preventedPlansNow, inputs.evidenceBackedPlans)
        .map(pair =>
          s"This changes the position by taking the ${pair.file} away as a counterplay route and closing ${pair.entrySquare}."
        )
    }.flatten

  private def localFileEntryChangeContrast(
      inputs: QuestionPlannerInputs
  ): Option[String] =
    Option.unless(inputs.heavyPieceLocalBindBlocked) {
      LocalFileEntryBindCertification
        .certifiedSurfacePair(inputs.preventedPlansNow, inputs.evidenceBackedPlans)
        .map(pair =>
          s"Before the move, the ${pair.file} and the ${pair.entrySquare} entry were still available."
        )
    }.flatten

  private def localFileEntryChangeConsequence(
      inputs: QuestionPlannerInputs
  ): Option[String] =
    Option.unless(inputs.heavyPieceLocalBindBlocked) {
      LocalFileEntryBindCertification
        .certifiedSurfacePair(inputs.preventedPlansNow, inputs.evidenceBackedPlans)
        .filter(_.counterplayScoreDrop > 0)
        .map(pair =>
          s"That removes roughly ${pair.counterplayScoreDrop}cp of counterplay from the local route."
        )
    }.flatten

  private def hasCertifiedLocalFileEntryChange(
      inputs: QuestionPlannerInputs
  ): Boolean =
    !inputs.heavyPieceLocalBindBlocked &&
      LocalFileEntryBindCertification
        .certifiedSurfacePair(inputs.preventedPlansNow, inputs.evidenceBackedPlans)
        .nonEmpty

  private def namedRouteNetworkWhyThisClaim(
      inputs: QuestionPlannerInputs
  ): Option[String] =
    Option.unless(inputs.heavyPieceLocalBindBlocked) {
      inputs.namedRouteNetworkSurface
        .flatMap { network =>
          network.intermediateSquare match
            case Some(_) =>
              // Broader route-chain intermediates stay backend-only until a real
              // exact-FEN root-best control is restored for planner-owned truth.
              None
            case None =>
              Some(
                s"This keeps ${network.entrySquare} closed, takes the ${network.file} away, and cuts off the ${network.rerouteSquare} reroute."
              )
        }
    }.flatten

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

  private def certifiedPositionProbeClaim(
      inputs: QuestionPlannerInputs
  ): Option[MainPathScopedClaim] =
    inputs.mainBundle.flatMap(_.mainClaim).filter(claim =>
      claim.scope == PlayerFacingClaimScope.PositionLocal &&
        claim.packet.exists(PlayerFacingTruthModePolicy.certifiedPositionProbePacket)
    )

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
      PlayerFacingTruthModePolicy.exactTargetFixationWhatChangedConsequence(packet).map(text =>
        QuestionPlanConsequence(text, QuestionPlanConsequenceBeat.WrapUp)
      )
    )

  private def decisionComparisonChangeClaim(
      comparison: Option[DecisionComparison]
  ): Option[String] =
    comparison.flatMap { value =>
      value.cpLossVsChosen
        .map(math.abs)
        .filter(_ >= 60)
        .map { loss =>
          value.deferredMove
            .filter(_.trim.nonEmpty)
            .map(move => s"This changes the practical balance by keeping about ${loss}cp that would drift toward $move.")
            .orElse(
              value.deferredReason
                .filter(_.trim.nonEmpty)
                .map(reason => s"This changes the practical balance by preserving about ${loss}cp: $reason.")
            )
            .getOrElse(s"This changes the practical balance by preserving about ${loss}cp.")
        }
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
      value.cpLossVsChosen
        .map(math.abs)
        .filter(_ >= 60)
        .map(loss => s"That keeps roughly ${loss}cp of practical value from slipping away.")
    }

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
        Some("Before the move, the opening was still following the more familiar setup.")
      case _ =>
        inputs.alternativeNarrative.map(_.sentence)
          .orElse(
            inputs.decisionComparison.flatMap(
              _.deferredMove.map(move => s"The practical alternative $move keeps the more familiar opening route.")
            )
          )
          .orElse(onlyMovePressure(inputs, truthContract))
          .orElse(Some("The move matters because it changes which opening script the position follows."))

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
          .flatMap(delta => planAdvanceConsequence(delta).orElse(newOpportunityConsequence(delta)))
          .map(text => QuestionPlanConsequence(text, QuestionPlanConsequenceBeat.WrapUp)),
      fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
      strengthTier = QuestionPlanStrengthTier.Moderate,
      sourceKinds = List("opening_relation_translator"),
      admissibilityReasons = List("opening_relation_owner", "scene_first_domain_owner"),
      ownerFamily = OwnerFamily.OpeningRelation,
      ownerSource = "opening_relation_translator"
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
      ownerFamily = OwnerFamily.EndgameTransition,
      ownerSource = "endgame_transition_translator"
    )

  private def buildWhatMattersHerePlan(
      question: AuthorQuestion,
      inputs: QuestionPlannerInputs,
      @unused truthContract: Option[DecisiveTruthContract],
      @unused sceneType: SceneType
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    val rawPositionProbe =
      inputs.mainBundle.flatMap(_.mainClaim).filter(_.scope == PlayerFacingClaimScope.PositionLocal)
    certifiedPositionProbeClaim(inputs) match
      case None =>
        rawPositionProbe match
          case Some(_) =>
            reject(question, QuestionPlanFallbackMode.FactualFallback, "position_probe_not_certified")
          case None =>
            reject(question, QuestionPlanFallbackMode.FactualFallback, "position_probe_missing")
      case Some(claim) =>
        val packet = claim.packet.get
        val ownerSource = packet.ownerSource
        val evidence =
          evidenceForQuestion(
            question = question,
            fallbackLine = inputs.mainBundle.flatMap(_.lineScopedClaim).map(_.claimText),
            sourceKinds = List(claim.sourceKind)
          )
        val consequence =
          positionProbeConsequence(packet).map(text =>
            QuestionPlanConsequence(text, QuestionPlanConsequenceBeat.WrapUp)
          )
        val strength =
          QuestionPlanStrengthTier.Strong
        mkPlan(
          question = question,
          kind = AuthorQuestionKind.WhatMattersHere,
          claim = claim.claimText,
          evidence = evidence,
          contrast = None,
          consequence = consequence,
          fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
          strengthTier = strength,
          sourceKinds = List(claim.sourceKind, ownerSource).distinct,
          admissibilityReasons = List("position_probe_owner", "current_position_truth", "certified_position_probe"),
          ownerFamily = OwnerFamily.PositionProbe,
          ownerSource = ownerSource
        )

  private def positionProbeConsequence(packet: PlayerFacingClaimPacket): Option[String] =
    PlayerFacingTruthModePolicy.positionProbeTaskConsequence(packet)

  private def buildWhyThisPlan(
      question: AuthorQuestion,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      sceneType: SceneType
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    val moveOwnerClaim =
      inputs.mainBundle.flatMap { bundle =>
        bundle.mainClaim.filter(_.scope == PlayerFacingClaimScope.MoveLocal).orElse(
          bundle.lineScopedClaim.filter(claim =>
            claim.scope == PlayerFacingClaimScope.LineScoped &&
              claim.packet.exists(_.fallbackMode == PlayerFacingClaimFallbackMode.WeakMain)
          )
        )
      }
    val domainFirst =
      sceneType match
        case SceneType.OpeningRelation if inputs.openingRelationClaim.nonEmpty =>
          Some(buildOpeningRelationPlan(question, inputs, truthContract, AuthorQuestionKind.WhyThis))
        case SceneType.EndgameTransition if inputs.endgameTransitionClaim.nonEmpty =>
          Some(buildEndgameTransitionPlan(question, inputs, AuthorQuestionKind.WhyThis))
        case _ => None

    domainFirst.getOrElse {
      val namedRouteClaim =
        namedRouteNetworkWhyThisClaim(inputs)
      val ownerClaim =
        namedRouteClaim
          .orElse(moveOwnerClaim.map(_.claimText))
          .orElse(inputs.quietIntent.map(_.claimText))
      ownerClaim match
        case None =>
          reject(question, QuestionPlanFallbackMode.FactualFallback, "missing_move_owner")
        case Some(claim) =>
          val namedRouteOwner =
            namedRouteClaim.contains(claim)
          val evidence =
            evidenceForQuestion(
              question = question,
              fallbackLine =
                inputs.mainBundle.flatMap(_.lineScopedClaim).map(_.claimText)
                  .orElse(inputs.quietIntent.flatMap(_.evidenceLine)),
              sourceKinds =
                (
                  List("main_bundle", "quiet_intent") ++
                    Option.when(namedRouteOwner)(NamedRouteNetworkBindCertification.OwnerSource).toList
                ).distinct
            )
          val contrast =
            inputs.decisionComparison.flatMap(_.deferredMove.map(move => s"The practical alternative $move remains secondary here."))
              .orElse(inputs.alternativeNarrative.map(_.sentence))
              .orElse(onlyMovePressure(inputs, truthContract))
          val consequence =
            inputs.pvDelta
              .flatMap(delta => planAdvanceConsequence(delta).orElse(newOpportunityConsequence(delta)))
              .map(text => QuestionPlanConsequence(text, QuestionPlanConsequenceBeat.WrapUp))
              .orElse {
                inputs.counterfactual
                  .filter(_.cpLoss > 0)
                  .map(cf =>
                    QuestionPlanConsequence(
                      s"If the move is missed, ${cf.bestMove} becomes the cleaner continuation instead.",
                      QuestionPlanConsequenceBeat.WrapUp
                    )
                  )
              }
          val strength =
            if moveOwnerClaim.exists(_.mode == PlayerFacingTruthMode.Tactical) then
              QuestionPlanStrengthTier.Strong
            else QuestionPlanStrengthTier.Moderate
          mkPlan(
            question = question,
            kind = AuthorQuestionKind.WhyThis,
            claim = claim,
            evidence = evidence,
            contrast = contrast,
            consequence = consequence,
            fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
            strengthTier = strength,
            sourceKinds =
              (
                List(moveOwnerClaim.map(_.sourceKind), inputs.quietIntent.map(_.sourceKind)).flatten ++
                  Option.when(namedRouteOwner)(NamedRouteNetworkBindCertification.OwnerSource).toList
              ).distinct,
            admissibilityReasons = List("move_owner", "move_local_claim"),
            ownerFamily =
              if namedRouteOwner then OwnerFamily.MoveDelta
              else if moveOwnerClaim.exists(_.mode == PlayerFacingTruthMode.Tactical) then OwnerFamily.TacticalFailure
              else OwnerFamily.MoveDelta,
            ownerSource =
              moveOwnerClaim.map(_.sourceKind)
                .orElse(inputs.quietIntent.map(_.sourceKind))
                .getOrElse("move_owner")
          )
    }

  private def buildWhyNowPlan(
      question: AuthorQuestion,
      @unused ply: Int,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      sceneType: SceneType
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    val timingOwner = concreteWhyNowTimingOwner(inputs, truthContract)
    timingOwner match
      case None =>
        resolveDemotion(
          question = question,
          QuestionPlanFallbackMode.DemotedToWhyThis,
          demotedTo = AuthorQuestionKind.WhyThis,
          reasons = List("generic_urgency_only"),
          fallbackBuild = buildWhyThisPlan(question, inputs, truthContract, sceneType)
        )
      case Some(owner) =>
        val claim =
          owner match
            case WhyNowTimingOwner.Threat(threat) =>
              threat.bestDefense
                .map(defense => s"The move has to happen now because otherwise $defense is demanded immediately.")
                .getOrElse(s"The move has to happen now because the opponent's ${threat.kind.toLowerCase} threat is already live.")
            case WhyNowTimingOwner.Prevented(plan) =>
              preventedPlanTimingClaim(plan).getOrElse("")
            case WhyNowTimingOwner.OnlyMove(reason) =>
              s"The timing matters now because $reason."
            case WhyNowTimingOwner.DecisionComparisonOwner(comparison) =>
              decisionComparisonTimingClaim(Some(comparison)).getOrElse("")
        val evidence =
          evidenceForQuestion(
            question = question,
            fallbackLine =
              inputs.mainBundle.flatMap(_.lineScopedClaim).map(_.claimText)
                .orElse(inputs.decisionComparison.flatMap(_.evidence))
                .orElse(inputs.quietIntent.flatMap(_.evidenceLine)),
            sourceKinds = List("timing_proof", "decision_comparison")
          )
        val contrast =
          owner match
            case WhyNowTimingOwner.Threat(threat) =>
              threat.bestDefense.map(defense => s"If White drifts, $defense is the reply.")
            case WhyNowTimingOwner.Prevented(plan) =>
              preventedPlanTimingContrast(plan)
            case WhyNowTimingOwner.OnlyMove(_) =>
              decisionComparisonTimingContrast(inputs.decisionComparison)
            case WhyNowTimingOwner.DecisionComparisonOwner(comparison) =>
              decisionComparisonTimingContrast(Some(comparison))
        val consequence =
          owner match
            case WhyNowTimingOwner.Threat(threat) =>
              Some(
                QuestionPlanConsequence(
                  s"That keeps the immediate ${threat.kind.toLowerCase} pressure from taking over.",
                  QuestionPlanConsequenceBeat.WrapUp
                )
              )
            case WhyNowTimingOwner.Prevented(plan) =>
              Option.when(plan.counterplayScoreDrop > 0)(
                QuestionPlanConsequence(
                  s"That shuts down roughly ${plan.counterplayScoreDrop}cp of counterplay before it starts.",
                  QuestionPlanConsequenceBeat.WrapUp
                )
              ).orElse(
                inputs.decisionComparison.flatMap { comparison =>
                  comparison.cpLossVsChosen.map(math.abs).filter(_ >= 60).map { loss =>
                    QuestionPlanConsequence(
                      s"That preserves roughly ${loss}cp of practical value that drifting would give back.",
                      QuestionPlanConsequenceBeat.WrapUp
                    )
                  }
                }
              )
            case WhyNowTimingOwner.OnlyMove(_) | WhyNowTimingOwner.DecisionComparisonOwner(_) =>
              inputs.decisionComparison.flatMap { comparison =>
                comparison.cpLossVsChosen.map(math.abs).filter(_ >= 60).map { loss =>
                  QuestionPlanConsequence(
                    s"That preserves roughly ${loss}cp of practical value that drifting would give back.",
                    QuestionPlanConsequenceBeat.WrapUp
                  )
                }
              }
        val sourceKinds =
          List("timing_reason") ++
            (
              owner match
                case WhyNowTimingOwner.Threat(_)                  => List("threat")
                case WhyNowTimingOwner.Prevented(_)               => List("prevented_plan")
                case WhyNowTimingOwner.OnlyMove(_)                => List("truth_contract")
                case WhyNowTimingOwner.DecisionComparisonOwner(_) => List("decision_comparison")
            )
        val strength =
          if sourceKinds.exists(kind =>
              kind == "threat" || kind == "prevented_plan" || kind == "truth_contract" || kind == "decision_comparison"
            )
          then QuestionPlanStrengthTier.Strong
          else QuestionPlanStrengthTier.Moderate
        mkPlan(
          question = question,
          kind = AuthorQuestionKind.WhyNow,
          claim = claim,
          evidence = evidence,
          contrast = contrast,
          consequence = consequence,
          fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
          strengthTier = strength,
          sourceKinds = sourceKinds,
          admissibilityReasons =
            List("timing_owner", "delay_sensitive_proof") ++
              Option.when(sourceKinds.contains("decision_comparison"))("timing_loss").toList,
          ownerFamily =
            owner match
              case WhyNowTimingOwner.DecisionComparisonOwner(_) => OwnerFamily.DecisionTiming
              case _                                            => OwnerFamily.ForcingDefense,
          ownerSource =
            owner match
              case WhyNowTimingOwner.Threat(_)                  => "threat"
              case WhyNowTimingOwner.Prevented(_)               => "prevented_plan"
              case WhyNowTimingOwner.OnlyMove(_)                => "truth_contract"
              case WhyNowTimingOwner.DecisionComparisonOwner(_) => "decision_comparison"
        )

  private def concreteWhyNowTimingOwner(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[WhyNowTimingOwner] =
    val urgentThreat = bestImmediateThreat(inputs.opponentThreats)
    val preventedNamedOrBreak =
      Option.unless(inputs.heavyPieceLocalBindBlocked) {
        inputs.preventedPlansNow.find(plan =>
          plan.breakNeutralized.exists(_.trim.nonEmpty) ||
            plan.preventedThreatType.exists(_.trim.nonEmpty)
        )
      }.flatten
    val preventedCounterplayWindow =
      Option.unless(inputs.heavyPieceLocalBindBlocked) {
        inputs.preventedPlansNow.find(plan =>
          !plan.breakNeutralized.exists(_.trim.nonEmpty) &&
            !plan.preventedThreatType.exists(_.trim.nonEmpty) &&
            plan.counterplayScoreDrop >= 80
        )
      }.flatten
    urgentThreat.map(WhyNowTimingOwner.Threat.apply)
      .orElse(
        preventedNamedOrBreak
          .filter(plan => preventedPlanTimingClaim(plan).nonEmpty)
          .map(WhyNowTimingOwner.Prevented.apply)
      )
      .orElse(onlyMovePressure(inputs, truthContract).map(WhyNowTimingOwner.OnlyMove.apply))
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

  private def buildWhatChangedPlan(
      question: AuthorQuestion,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      sceneType: SceneType
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    val domainFirst =
      sceneType match
        case SceneType.OpeningRelation if inputs.openingRelationClaim.nonEmpty =>
          Some(buildOpeningRelationPlan(question, inputs, truthContract, AuthorQuestionKind.WhatChanged))
        case SceneType.EndgameTransition if inputs.endgameTransitionClaim.nonEmpty =>
          Some(buildEndgameTransitionPlan(question, inputs, AuthorQuestionKind.WhatChanged))
        case _ => None
    val moveOwner =
      inputs.mainBundle.flatMap(_.mainClaim).filter(_.scope == PlayerFacingClaimScope.MoveLocal)
    val exactTargetFixationChange = exactTargetFixationChangeClaim(inputs)
    val canPromoteDecisionComparisonChange =
      moveOwner.nonEmpty || hasConcreteMoveDeltaChange(inputs)
    val allowedPreventedPlans =
      Option.unless(inputs.heavyPieceLocalBindBlocked)(inputs.preventedPlansNow).getOrElse(Nil)
    val moveLinkedChange =
      exactTargetFixationChange.orElse {
        inputs.pvDelta.flatMap { delta =>
          resolvedThreatConsequence(delta)
            .orElse(newOpportunityConsequence(delta))
            .orElse(planAdvanceConsequence(delta))
        }
      }.orElse {
        localFileEntryChangeClaim(inputs)
      }.orElse {
        allowedPreventedPlans.collectFirst(Function.unlift(preventedPlanChangeClaim))
      }.orElse {
        Option.when(canPromoteDecisionComparisonChange)(decisionComparisonChangeClaim(inputs.decisionComparison)).flatten
      }
    domainFirst.getOrElse {
      (moveOwner, moveLinkedChange) match
        case (None, None) =>
          reject(question, QuestionPlanFallbackMode.FactualFallback, "state_truth_only")
        case _ =>
          val claim =
            moveLinkedChange
              .orElse(moveOwner.map(_.claimText))
              .getOrElse("")
          val contrast =
            exactTargetFixationChangeContrast(inputs).orElse {
              inputs.pvDelta.flatMap { delta =>
                delta.resolvedThreats.headOption.map(threat => s"Before the move, $threat was still on the board.")
                  .orElse(delta.concessions.headOption.map(concession => s"The tradeoff is that $concession."))
              }
            }.orElse {
              localFileEntryChangeContrast(inputs)
            }.orElse {
              allowedPreventedPlans.collectFirst(Function.unlift(preventedPlanChangeContrast))
            }.orElse {
              Option.when(canPromoteDecisionComparisonChange)(decisionComparisonChangeContrast(inputs.decisionComparison)).flatten
            }
          val consequence =
            exactTargetFixationChangeConsequence(inputs).orElse {
              inputs.pvDelta.flatMap { delta =>
                planAdvanceConsequence(delta)
                  .orElse(newOpportunityConsequence(delta))
                  .map(text => QuestionPlanConsequence(text, QuestionPlanConsequenceBeat.WrapUp))
              }
            }.orElse {
              localFileEntryChangeConsequence(inputs)
                .map(text => QuestionPlanConsequence(text, QuestionPlanConsequenceBeat.WrapUp))
            }.orElse {
              allowedPreventedPlans.collectFirst(Function.unlift(preventedPlanChangeConsequence))
                .map(text => QuestionPlanConsequence(text, QuestionPlanConsequenceBeat.WrapUp))
            }.orElse {
              Option.when(canPromoteDecisionComparisonChange)(decisionComparisonChangeConsequence(inputs.decisionComparison)).flatten
                .map(text => QuestionPlanConsequence(text, QuestionPlanConsequenceBeat.WrapUp))
            }
          val sourceKinds =
            moveOwner.toList.map(_.sourceKind) ++
              Option.when(exactTargetFixationChange.nonEmpty)("exact_target_fixation_delta").toList ++
              inputs.pvDelta.toList.map(_ => "pv_delta") ++
              Option.when(localFileEntryChangeClaim(inputs).nonEmpty)("prevented_plan").toList ++
              allowedPreventedPlans
                .find(plan =>
                  preventedPlanChangeClaim(plan).nonEmpty ||
                    preventedPlanChangeContrast(plan).nonEmpty ||
                    preventedPlanChangeConsequence(plan).nonEmpty
                )
                .toList
                .map(_ => "prevented_plan") ++
              inputs.decisionComparison.toList
                .filter(comparison => canPromoteDecisionComparisonChange && decisionComparisonChangeClaim(Some(comparison)).nonEmpty)
                .map(_ => "decision_comparison")
          mkPlan(
            question = question,
            kind = AuthorQuestionKind.WhatChanged,
            claim = claim,
            evidence =
              evidenceForQuestion(
                question = question,
                fallbackLine =
                  inputs.mainBundle.flatMap(_.lineScopedClaim).map(_.claimText)
                    .orElse(allowedPreventedPlans.flatMap(_.citationLine).find(_.trim.nonEmpty)),
                sourceKinds = List("move_delta", "prevented_plan")
              ),
            contrast = contrast,
            consequence = consequence,
            fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
            strengthTier = QuestionPlanStrengthTier.Strong,
            sourceKinds = sourceKinds,
            admissibilityReasons =
              List("move_attributed_change") ++
                Option.when(exactTargetFixationChange.nonEmpty)("exact_target_state_delta").toList,
            ownerFamily =
              if moveOwner.exists(_.mode == PlayerFacingTruthMode.Tactical) then OwnerFamily.TacticalFailure
              else if inputs.decisionComparison.exists(comparison => decisionComparisonChangeClaim(Some(comparison)).nonEmpty) &&
                inputs.pvDelta.isEmpty &&
                allowedPreventedPlans.forall(plan => preventedPlanChangeClaim(plan).isEmpty)
              then OwnerFamily.DecisionTiming
              else if hasCertifiedLocalFileEntryChange(inputs) &&
                inputs.pvDelta.isEmpty &&
                moveOwner.isEmpty
              then OwnerFamily.MoveDelta
              else if allowedPreventedPlans.exists(plan => preventedPlanChangeClaim(plan).nonEmpty) &&
                inputs.pvDelta.isEmpty &&
                moveOwner.isEmpty
              then OwnerFamily.ForcingDefense
              else OwnerFamily.MoveDelta,
            ownerSource =
              if moveOwner.exists(_.mode == PlayerFacingTruthMode.Tactical) then
                moveOwner.map(_.sourceKind).getOrElse("move_delta")
              else if moveOwner.nonEmpty then
                moveOwner.map(_.sourceKind).getOrElse("move_delta")
              else if inputs.pvDelta.nonEmpty then "pv_delta"
              else if hasCertifiedLocalFileEntryChange(inputs) then "prevented_plan"
              else if allowedPreventedPlans.exists(plan => preventedPlanChangeClaim(plan).nonEmpty) then "prevented_plan"
              else if inputs.decisionComparison.exists(comparison => decisionComparisonChangeClaim(Some(comparison)).nonEmpty) then
                "decision_comparison"
              else moveOwner.map(_.sourceKind).getOrElse("move_delta")
          )
    }

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

  private def buildWhatMustBeStoppedPlan(
      question: AuthorQuestion,
      @unused ply: Int,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      sceneType: SceneType
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    val urgentThreat = bestImmediateThreat(inputs.opponentThreats)
    val preventedNow =
      Option.unless(inputs.heavyPieceLocalBindBlocked) {
        inputs.preventedPlansNow.find(plan =>
          plan.counterplayScoreDrop > 0 ||
            plan.breakNeutralized.exists(_.trim.nonEmpty) ||
            plan.preventedThreatType.exists(_.trim.nonEmpty)
        )
      }.flatten
    val claim =
      urgentThreat.map { threat =>
        s"This has to stop the opponent's ${threat.kind.toLowerCase} threat before it lands."
      }.orElse {
        preventedNow.flatMap { plan =>
          plan.breakNeutralized.map(file => s"This has to stop the opponent's $file-break before it starts.")
            .orElse(plan.preventedThreatType.map(kind => s"This has to stop the opponent's $kind before it becomes concrete."))
            .orElse(Option.when(plan.counterplayScoreDrop > 0)("This has to stop the opponent's easiest counterplay before it grows."))
        }
      }
    claim match
      case None =>
        resolveDemotion(
          question = question,
          QuestionPlanFallbackMode.DemotedToWhyThis,
          demotedTo = AuthorQuestionKind.WhyThis,
          reasons = List("generic_opponent_plan_only"),
          fallbackBuild = buildWhyThisPlan(question, inputs, truthContract, sceneType)
        )
      case Some(text) =>
        val contrast =
          urgentThreat.flatMap(_.bestDefense.map(defense => s"If the move is missed, $defense is forced."))
            .orElse(preventedNow.flatMap(_.citationLine.map(line => s"If the move is missed, the line $line comes back.")))
        val consequence =
          preventedNow.flatMap { plan =>
            Option.when(plan.counterplayScoreDrop > 0)(
              QuestionPlanConsequence(
                s"That keeps roughly ${plan.counterplayScoreDrop}cp of counterplay from appearing.",
                QuestionPlanConsequenceBeat.WrapUp
              )
            )
          }
        mkPlan(
          question = question,
          kind = AuthorQuestionKind.WhatMustBeStopped,
          claim = text,
          evidence =
            evidenceForQuestion(
              question = question,
              fallbackLine =
                preventedNow.flatMap(_.citationLine)
                  .orElse(inputs.mainBundle.flatMap(_.lineScopedClaim).map(_.claimText)),
              sourceKinds = List("threat", "prevented_plan")
            ),
          contrast = contrast,
          consequence = consequence,
          fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
          strengthTier = QuestionPlanStrengthTier.Strong,
          sourceKinds =
            urgentThreat.toList.map(_ => "threat") ++ preventedNow.toList.map(_ => "prevented_plan"),
          admissibilityReasons = List("defensive_owner", "loss_if_ignored"),
          ownerFamily = OwnerFamily.ForcingDefense,
          ownerSource =
            if urgentThreat.nonEmpty then "threat" else "prevented_plan"
        )

  private def buildWhosePlanIsFasterPlan(
      question: AuthorQuestion,
      ply: Int,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      sceneType: SceneType
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    val owner = ownerSideLabel(inputs, ply)
    val intent =
      inputs.decisionFrame.intent.orElse(evidenceBackedRaceIntent(owner, inputs))
    val battlefront = inputs.decisionFrame.battlefront
    val opponentPlan = inputs.opponentPlan.filter(plan => cleanLine(plan.name).nonEmpty)
    val opponentRace =
      opponentPlan.map(_.name)
        .orElse(
          bestImmediateThreat(inputs.opponentThreats).map(threat => s"the ${threat.kind.toLowerCase} threat")
        )
        .orElse(
          Option.unless(inputs.heavyPieceLocalBindBlocked)(inputs.preventedPlansNow).getOrElse(Nil).collectFirst {
            case plan if plan.breakNeutralized.exists(_.trim.nonEmpty) =>
              s"the ${plan.breakNeutralized.get}-break"
          }
        )
    val urgencyRaceAnchor =
      opponentRace.flatMap { race =>
        concreteRaceUrgency(inputs).map(anchor => s"$anchor before $race gets fully rolling")
      }
    val raceAnchor =
      bestImmediateThreat(inputs.opponentThreats)
        .map(threat => s"the reply window is short against the ${threat.kind.toLowerCase} threat")
        .orElse(
          Option.unless(inputs.heavyPieceLocalBindBlocked)(inputs.preventedPlansNow).getOrElse(Nil).collectFirst {
            case plan if plan.breakNeutralized.exists(_.trim.nonEmpty) =>
              s"the ${plan.breakNeutralized.get}-break is the timing window"
          }
        )
        .orElse(urgencyRaceAnchor)
    if intent.isEmpty || battlefront.isEmpty || opponentRace.isEmpty || raceAnchor.isEmpty then
      val onlyOpponentPressure =
        opponentRace.nonEmpty &&
          (
            bestImmediateThreat(inputs.opponentThreats).nonEmpty ||
              Option.unless(inputs.heavyPieceLocalBindBlocked)(inputs.preventedPlansNow).exists(_.nonEmpty)
          )
      val onlyOurPlan = intent.nonEmpty || battlefront.nonEmpty || inputs.evidenceBackedPlans.nonEmpty
      if onlyOpponentPressure then
        resolveDemotion(
          question = question,
          QuestionPlanFallbackMode.DemotedToWhatMustBeStopped,
          demotedTo = AuthorQuestionKind.WhatMustBeStopped,
          reasons = List("missing_certified_race_pair"),
          fallbackBuild = buildWhatMustBeStoppedPlan(question, ply, inputs, truthContract, sceneType)
        )
      else if onlyOurPlan then
        resolveDemotion(
          question = question,
          QuestionPlanFallbackMode.DemotedToWhyThis,
          demotedTo = AuthorQuestionKind.WhyThis,
          reasons = List("single_sided_plan_only"),
          fallbackBuild = buildWhyThisPlan(question, inputs, truthContract, sceneType)
        )
      else reject(question, QuestionPlanFallbackMode.FactualFallback, "missing_certified_race_pair")
    else
      val intentReason =
        if inputs.decisionFrame.intent.nonEmpty then "certified_intent"
        else "probe_backed_plan_intent"
      mkPlan(
        question = question,
        kind = AuthorQuestionKind.WhosePlanIsFaster,
        claim = s"$owner keeps the initiative ahead of ${opponentRace.get} because ${raceAnchor.get}.",
        evidence =
          evidenceForQuestion(
            question = question,
            fallbackLine = battlefront.map(_.sentence).orElse(intent.map(_.sentence)),
            sourceKinds = List(intent.get.sourceKind, battlefront.get.sourceKind)
          ),
        contrast = Some(s"${owner}'s plan is racing ${opponentRace.get}, and the battlefront is ${battlefront.get.sentence}"),
        consequence = Some(
          QuestionPlanConsequence(
            s"That preserves ${owner.toLowerCase}'s plan window before ${opponentRace.get} catches up.",
            QuestionPlanConsequenceBeat.WrapUp
          )
        ),
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Moderate,
        sourceKinds = List(intent.get.sourceKind, battlefront.get.sourceKind, if opponentPlan.nonEmpty then "opponent_plan" else "opponent_threat"),
        admissibilityReasons = List(intentReason, "certified_battlefront", "timing_anchor"),
        ownerFamily = OwnerFamily.PlanRace,
        ownerSource = intent.get.sourceKind
      )

  private val EndgameTransitionPattern = raw"(.+)\((.+)\)\s*→\s*(.+)\((.+)\)".r

  private def ownerCandidate(
      family: OwnerFamily,
      source: String,
      sourceKinds: List[String],
      questionKinds: List[AuthorQuestionKind],
      moveLinked: Boolean,
      materiality: OwnerCandidateMateriality = OwnerCandidateMateriality.OwnerCandidate,
      timingSource: Option[TimingSource] = None,
      decisionComparisonTimingDetail: Option[DecisionComparisonTimingDetail] = None,
      proposedFamilyMapping: String,
      reasons: List[String]
  ): OwnerCandidateTrace =
    OwnerCandidateTrace(
      family = family,
      source = source,
      sourceKinds = sourceKinds,
      questionKinds = questionKinds,
      moveLinked = moveLinked,
      materiality = materiality,
      timingSource = timingSource,
      decisionComparisonTimingDetail = decisionComparisonTimingDetail,
      proposedFamilyMapping = proposedFamilyMapping,
      reasons = reasons
    )

  private def buildOwnerTrace(
      sceneTrace: SceneClassificationTrace,
      ownerCandidates: List[OwnerCandidateTrace],
      admitted: List[QuestionPlan],
      rejected: List[RejectedQuestionPlan],
      primary: Option[QuestionPlan]
  ): PlannerOwnerTrace =
    val admittedFamilies =
      ownerCandidates
        .filter(_.admissionDecision.contains(AdmissionDecision.PrimaryAllowed))
        .map { candidate =>
          val relatedPlans =
            admitted.filter(plan => plan.ownerFamily == candidate.family && plan.ownerSource == candidate.source)
          candidate.copy(
            sourceKinds = (candidate.sourceKinds ++ relatedPlans.flatMap(_.sourceKinds)).distinct.sorted,
            questionKinds = (candidate.questionKinds ++ relatedPlans.map(_.questionKind)).distinct.sortBy(_.toString),
            reasons = (candidate.reasons ++ relatedPlans.flatMap(_.admissibilityReasons) ++ relatedPlans.flatMap(_.demotionReasons)).distinct.sorted
          )
        }
    val droppedFamilies =
      ownerCandidates
        .filterNot(_.admissionDecision.contains(AdmissionDecision.PrimaryAllowed))
        .map { candidate =>
          val relatedRejected =
            rejected.filter(rejectedPlan =>
              candidate.questionKinds.contains(rejectedPlan.questionKind) ||
                rejectedPlan.reasons.contains(candidate.admissionReason.getOrElse(""))
            )
          val reasons =
            (relatedRejected.flatMap(_.reasons) ++
              relatedRejected.flatMap(_.demotionReasons) ++
              candidate.reasons ++
              candidate.admissionReason.toList ++
              candidate.admissionDecision.toList.map(_.wireName)).distinct.sorted
          DroppedOwnerFamilyTrace(
            family = candidate.family,
            source = candidate.source,
            reasons = if reasons.nonEmpty then reasons else List("not_admitted"),
            questionKinds = relatedRejected.map(_.questionKind).distinct.sortBy(_.toString)
          )
        }
    PlannerOwnerTrace(
      sceneType = sceneTrace.sceneType,
      sceneReasons = sceneTrace.reasons,
      ownerCandidates = ownerCandidates,
      admittedFamilies = admittedFamilies,
      droppedFamilies = droppedFamilies,
      demotionReasons =
        (
          admitted.flatMap(_.demotionReasons) ++
            rejected.flatMap(_.demotionReasons) ++
            ownerCandidates
              .filterNot(_.admissionDecision.contains(AdmissionDecision.PrimaryAllowed))
              .flatMap(_.admissionReason)
        ).distinct.sorted,
      selectedQuestion = primary.map(_.questionKind),
      selectedOwnerFamily = primary.map(_.ownerFamily),
      selectedOwnerSource = primary.map(_.ownerSource)
    )

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
          s"owner_family=${plan.ownerFamily.wireName}",
          s"owner_source=${plan.ownerSource}"
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
    def primary(reason: String) = AdmissionOutcome(AdmissionDecision.PrimaryAllowed, reason)
    def support(reason: String) = AdmissionOutcome(AdmissionDecision.SupportOnly, reason)
    def demote(reason: String, to: AuthorQuestionKind) =
      AdmissionOutcome(AdmissionDecision.Demote, reason, demotedTo = Some(to))
    def forbid(reason: String) = AdmissionOutcome(AdmissionDecision.Forbidden, reason)

    if candidate.materiality == OwnerCandidateMateriality.SupportMaterial then
      support("support_material_not_owner_legal")
    else
      candidate.family match
        case OwnerFamily.TacticalFailure =>
          if sceneType == SceneType.TacticalFailure then primary("tactical_failure_primary_in_tactical_scene")
          else support("tactical_failure_subordinate_outside_tactical_scene")
        case OwnerFamily.ForcingDefense =>
          sceneType match
            case SceneType.ForcingDefense => primary("forcing_defense_primary_in_forcing_scene")
            case SceneType.TacticalFailure | SceneType.PlanClash | SceneType.TransitionConversion |
                SceneType.EndgameTransition => support("forcing_defense_support_only_outside_forcing_scene")
            case _ => forbid("forcing_defense_not_legal_in_current_scene")
        case OwnerFamily.MoveDelta =>
          sceneType match
            case SceneType.QuietImprovement | SceneType.TransitionConversion =>
              primary("move_delta_primary_in_quiet_or_conversion_scene")
            case _ => support("move_delta_support_only_outside_quiet_or_conversion_scene")
        case OwnerFamily.PositionProbe =>
          sceneType match
            case SceneType.QuietImprovement =>
              primary("position_probe_primary_in_quiet_scene")
            case _ =>
              support("position_probe_support_only_outside_quiet_scene")
        case OwnerFamily.DecisionTiming =>
          candidate.timingSource match
            case Some(TimingSource.CloseCandidate) =>
              support("decision_timing_close_candidate_support_only")
            case Some(TimingSource.DecisionComparison) =>
              sceneType match
                case SceneType.TacticalFailure =>
                  demote("decision_timing_demoted_under_tactical_failure", AuthorQuestionKind.WhyThis)
                case _ =>
                  support("decision_timing_support_only_in_v1")
            case Some(TimingSource.PreventedResource | TimingSource.OnlyMove) =>
              sceneType match
                case SceneType.TacticalFailure =>
                  demote("decision_timing_demoted_under_tactical_failure", AuthorQuestionKind.WhyThis)
                case _ =>
                  support("decision_timing_support_only_in_v1")
            case _ =>
              forbid("decision_timing_missing_supported_source")
        case OwnerFamily.PlanRace =>
          sceneType match
            case SceneType.PlanClash => primary("plan_race_primary_in_plan_clash")
            case SceneType.ForcingDefense =>
              demote("plan_race_demoted_under_forcing_defense", AuthorQuestionKind.WhatMustBeStopped)
            case _ =>
              demote("plan_race_demoted_outside_plan_clash", AuthorQuestionKind.WhyThis)
        case OwnerFamily.OpeningRelation =>
          if !candidate.moveLinked || candidate.source == "opening_precedent_summary" then
            support("opening_relation_raw_summary_support_only")
          else
            sceneType match
              case SceneType.OpeningRelation => primary("opening_relation_primary_in_opening_scene")
              case SceneType.TransitionConversion => support("opening_relation_support_only_under_conversion")
              case _ => forbid("opening_relation_not_legal_in_current_scene")
        case OwnerFamily.EndgameTransition =>
          if !candidate.moveLinked || candidate.source == "endgame_theoretical_hint" then
            support("endgame_transition_raw_hint_support_only")
          else
            sceneType match
              case SceneType.EndgameTransition => primary("endgame_transition_primary_in_endgame_scene")
              case SceneType.TransitionConversion => support("endgame_transition_support_only_under_conversion")
              case _ => forbid("endgame_transition_not_legal_in_current_scene")

  private def rawOwnerCandidates(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): List[OwnerCandidateTrace] =
    val tacticalFailure =
      Option.when(
        truthContract.exists(contract =>
          contract.truthClass == DecisiveTruthClass.Blunder ||
            contract.truthClass == DecisiveTruthClass.MissedWin ||
            (contract.reasonFamily == DecisiveReasonFamily.TacticalRefutation && contract.isBad) ||
            contract.reasonFamily == DecisiveReasonFamily.MissedWin
        ) ||
          inputs.mainBundle.flatMap(_.mainClaim).exists(_.mode == PlayerFacingTruthMode.Tactical)
      ) {
        ownerCandidate(
          family = OwnerFamily.TacticalFailure,
          source = truthContract.map(_ => "truth_contract").getOrElse("main_bundle"),
          sourceKinds = List(truthContract.map(_ => "truth_contract").getOrElse("main_bundle")),
          questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
          moveLinked = true,
          proposedFamilyMapping = "TacticalFailure/move_linked",
          reasons = List("tactical_failure_signal")
        )
      }.toList

    val forcingDefense =
      List(
        Option.unless(prefersQuietMoveDeltaIngress(inputs, truthContract)) {
          bestImmediateThreat(inputs.opponentThreats).map { _ =>
            ownerCandidate(
              family = OwnerFamily.ForcingDefense,
              source = "threat",
              sourceKinds = List("threat"),
              questionKinds = List(AuthorQuestionKind.WhyNow, AuthorQuestionKind.WhatMustBeStopped),
              moveLinked = true,
              proposedFamilyMapping = "ForcingDefense/move_linked",
              reasons = List("urgent_threat")
            )
          }
        }.flatten,
        Option.unless(prefersRestrictedSuppressionMoveDeltaIngress(inputs)) {
          Option.unless(inputs.heavyPieceLocalBindBlocked)(inputs.preventedPlansNow).getOrElse(Nil)
            .find(plan =>
              preventedPlanTimingClaim(plan).nonEmpty ||
                preventedPlanChangeClaim(plan).nonEmpty
            )
            .map { _ =>
              ownerCandidate(
                family = OwnerFamily.ForcingDefense,
                source = "prevented_plan",
                sourceKinds = List("prevented_plan"),
                questionKinds = List(
                  AuthorQuestionKind.WhyNow,
                  AuthorQuestionKind.WhatChanged,
                  AuthorQuestionKind.WhatMustBeStopped
                ),
                moveLinked = true,
                proposedFamilyMapping = "ForcingDefense/move_linked",
                reasons = List("prevented_resource")
              )
            }
        }.flatten,
        onlyMovePressure(inputs, truthContract).map { _ =>
          ownerCandidate(
            family = OwnerFamily.ForcingDefense,
            source = "truth_contract",
            sourceKinds = List("truth_contract"),
            questionKinds = List(AuthorQuestionKind.WhyNow),
            moveLinked = true,
            proposedFamilyMapping = "ForcingDefense/move_linked",
            reasons = List("only_move_defense")
          )
        }
      ).flatten

    val positionProbe =
      certifiedPositionProbeClaim(inputs).map { claim =>
        val packet = claim.packet.get
        ownerCandidate(
          family = OwnerFamily.PositionProbe,
          source = packet.ownerSource,
          sourceKinds = List(claim.sourceKind, packet.ownerSource).distinct,
          questionKinds = List(AuthorQuestionKind.WhatMattersHere),
          moveLinked = false,
          proposedFamilyMapping = "PositionProbe/position_local",
          reasons = List("current_position_probe", "certified_position_probe")
        )
      }.toList

    val moveDelta =
      List(
        inputs.mainBundle.flatMap { bundle =>
          bundle.mainClaim.filter(_.scope == PlayerFacingClaimScope.MoveLocal).orElse(
            bundle.lineScopedClaim.filter(claim =>
              claim.packet.exists(_.fallbackMode == PlayerFacingClaimFallbackMode.WeakMain)
            )
          )
        }.map { claim =>
          ownerCandidate(
            family =
              if claim.mode == PlayerFacingTruthMode.Tactical then OwnerFamily.TacticalFailure
              else OwnerFamily.MoveDelta,
            source = claim.sourceKind,
            sourceKinds = List(claim.sourceKind),
            questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
            moveLinked = true,
            proposedFamilyMapping =
              if claim.mode == PlayerFacingTruthMode.Tactical then "TacticalFailure/move_linked"
              else "MoveDelta/move_linked",
            reasons = List("main_move_claim")
          )
        },
        inputs.quietIntent.map { intent =>
          ownerCandidate(
            family = OwnerFamily.MoveDelta,
            source = intent.sourceKind,
            sourceKinds = List(intent.sourceKind),
            questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
            moveLinked = true,
            proposedFamilyMapping = "MoveDelta/move_linked",
            reasons = List("quiet_move_claim")
          )
        },
        inputs.pvDelta.map { _ =>
          ownerCandidate(
            family = OwnerFamily.MoveDelta,
            source = "pv_delta",
            sourceKinds = List("pv_delta"),
            questionKinds = List(AuthorQuestionKind.WhatChanged),
            moveLinked = true,
            proposedFamilyMapping = "MoveDelta/move_linked",
            reasons = List("move_local_delta")
          )
        }
      ).flatten

    val decisionTiming =
      Option.when(
        decisionComparisonTimingClaim(inputs.decisionComparison).nonEmpty ||
          decisionComparisonChangeClaim(inputs.decisionComparison).nonEmpty
      ) {
        val detail =
          inputs.decisionComparison
            .map(decisionComparisonTimingDetail)
            .getOrElse(DecisionComparisonTimingDetail.BareEngineGap)
        ownerCandidate(
          family = OwnerFamily.DecisionTiming,
          source = "decision_comparison",
          sourceKinds = List("decision_comparison"),
          questionKinds = List(AuthorQuestionKind.WhyNow, AuthorQuestionKind.WhatChanged),
          moveLinked = true,
          timingSource = Some(TimingSource.DecisionComparison),
          decisionComparisonTimingDetail = Some(detail),
          proposedFamilyMapping = s"DecisionTiming/${detail.wireName}",
          reasons = List("timing_loss", detail.wireName)
        )
      }.toList

    val timingRefinements =
      List(
        Option.unless(inputs.heavyPieceLocalBindBlocked)(inputs.preventedPlansNow).getOrElse(Nil)
          .find(plan => preventedPlanTimingClaim(plan).nonEmpty)
          .map { _ =>
            ownerCandidate(
              family = OwnerFamily.DecisionTiming,
              source = "prevented_plan",
              sourceKinds = List("prevented_plan"),
              questionKinds = List(AuthorQuestionKind.WhyNow, AuthorQuestionKind.WhatChanged),
              moveLinked = true,
              timingSource = Some(TimingSource.PreventedResource),
              proposedFamilyMapping = "DecisionTiming/move_linked",
              reasons = List("prevented_resource_timing")
            )
          },
        onlyMovePressure(inputs, truthContract).map { _ =>
          ownerCandidate(
            family = OwnerFamily.DecisionTiming,
            source = "truth_contract",
            sourceKinds = List("truth_contract"),
            questionKinds = List(AuthorQuestionKind.WhyNow),
            moveLinked = true,
            timingSource = Some(TimingSource.OnlyMove),
            proposedFamilyMapping = "DecisionTiming/move_linked",
            reasons = List("only_move_timing")
          )
        }
      ).flatten

    val planRace =
      Option.when(hasPlanRaceCandidate(inputs)) {
        ownerCandidate(
          family = OwnerFamily.PlanRace,
          source =
            inputs.decisionFrame.intent.map(_.sourceKind)
              .orElse(Option.when(inputs.evidenceBackedPlans.nonEmpty)("evidence_backed_plan"))
              .orElse(inputs.decisionFrame.battlefront.map(_.sourceKind))
              .getOrElse("plan_race"),
          sourceKinds =
            List(
              inputs.decisionFrame.intent.map(_.sourceKind),
              inputs.decisionFrame.battlefront.map(_.sourceKind),
              Option.when(inputs.opponentPlan.nonEmpty)("opponent_plan")
            ).flatten.distinct,
          questionKinds = List(AuthorQuestionKind.WhosePlanIsFaster),
          moveLinked = true,
          proposedFamilyMapping = "PlanRace/move_linked",
          reasons = List("certified_plan_race")
        )
      }.toList

    val domainShadowSignals = shadowDomainSignals(ctx, inputs)

    (tacticalFailure ++ forcingDefense ++ positionProbe ++ moveDelta ++ decisionTiming ++ timingRefinements ++ planRace ++ domainShadowSignals)
      .groupBy(_.key)
      .toList
      .sortBy { case ((family, source, materiality, timingSource), _) =>
        (family.wireName, source, materiality.wireName, timingSource.map(_.wireName).getOrElse(""))
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

  private def shadowDomainSignals(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs
  ): List[OwnerCandidateTrace] =
    val decisionComparisonSupport =
      inputs.alternativeNarrative
        .filter(_.source == "close_candidate")
        .map { _ =>
          ownerCandidate(
            family = OwnerFamily.DecisionTiming,
            source = "close_candidate",
            sourceKinds = List("alternative_narrative", "close_candidate"),
            questionKinds = List(
              AuthorQuestionKind.WhyThis,
              AuthorQuestionKind.WhyNow,
              AuthorQuestionKind.WhatChanged
            ),
            moveLinked = false,
            materiality = OwnerCandidateMateriality.SupportMaterial,
            timingSource = Some(TimingSource.CloseCandidate),
            proposedFamilyMapping = "DecisionTiming/support_only",
            reasons = List("raw_close_alternative")
          )
        }
        .toList

    val openingSignals =
      List(
        ctx.flatMap(openingRelationReplayClaim).orElse(inputs.openingRelationClaim).map { _ =>
          ownerCandidate(
            family = OwnerFamily.OpeningRelation,
            source = "opening_relation_translator",
            sourceKinds = List("opening_relation_translator"),
            questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
            moveLinked = true,
            proposedFamilyMapping = "OpeningRelation/move_linked",
            reasons = List("opening_relation_translated")
          )
        },
        ctx.flatMap { narrativeCtx =>
          OpeningPrecedentBranching.summarySentence(narrativeCtx, narrativeCtx.openingData, requireFocus = false)
        }.map { _ =>
          ownerCandidate(
            family = OwnerFamily.OpeningRelation,
            source = "opening_precedent_summary",
            sourceKinds = List("opening_precedent_summary"),
            questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
            moveLinked = false,
            materiality = OwnerCandidateMateriality.SupportMaterial,
            proposedFamilyMapping = "OpeningRelation/support_only",
            reasons = List("raw_opening_precedent_summary")
          )
        }
      ).flatten

    val endgameSignals =
      List(
        ctx.flatMap(endgameTransitionReplayClaim).orElse(inputs.endgameTransitionClaim).map { _ =>
          ownerCandidate(
            family = OwnerFamily.EndgameTransition,
            source = "endgame_transition_translator",
            sourceKinds = List("endgame_transition_translator"),
            questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
            moveLinked = true,
            proposedFamilyMapping = "EndgameTransition/move_linked",
            reasons = List("endgame_transition_translated")
          )
        },
        ctx.flatMap(_.semantic.flatMap(_.endgameFeatures)).flatMap(rawEndgameHint).map { _ =>
          ownerCandidate(
            family = OwnerFamily.EndgameTransition,
            source = "endgame_theoretical_hint",
            sourceKinds = List("endgame_theoretical_hint", "endgame_oracle"),
            questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
            moveLinked = false,
            materiality = OwnerCandidateMateriality.SupportMaterial,
            proposedFamilyMapping = "EndgameTransition/support_only",
            reasons = List("raw_endgame_hint")
          )
        }
      ).flatten

    decisionComparisonSupport ++ openingSignals ++ endgameSignals

  private def rawEndgameHint(info: EndgameInfo): Option[String] =
    cleanLine(info.theoreticalOutcomeHint)
      .filterNot(_.equalsIgnoreCase("unclear"))
      .orElse(info.primaryPattern.flatMap(cleanLine))
      .orElse(info.transition.flatMap(cleanLine))

  private[llm] def endgameTransitionSentence(info: EndgameInfo): Option[String] =
    info.transition.flatMap {
      case EndgameTransitionPattern(fromRaw, fromHintRaw, toRaw, toHintRaw) =>
        val fromLabel = humanizeEndgamePattern(fromRaw)
        val toLabel = humanizeEndgamePattern(toRaw)
        val fromTask = endgameTaskPhrase(fromHintRaw)
        val toTask = endgameTaskPhrase(toHintRaw)
        Some(
          if fromRaw.equalsIgnoreCase("none") then
            s"A new $toLabel pattern has emerged, giving the position a clearer $toTask."
          else if toRaw.equalsIgnoreCase("none") then
            s"The $fromLabel pattern has dissolved, so the earlier $fromTask no longer holds automatically."
          else
            s"The endgame geometry has shifted from $fromLabel to $toLabel, turning the position from a $fromTask into a $toTask."
        )
      case _ => None
    }.orElse {
      info.primaryPattern.flatMap { pattern =>
        Option.when(info.patternAge >= 2) {
          val label = humanizeEndgamePattern(pattern)
          val task = endgameTaskPhrase(info.theoreticalOutcomeHint)
          s"The $label structure has held, so the same $task remains in force."
        }
      }
    }

  private def humanizeEndgamePattern(raw: String): String =
    val normalized = Option(raw).getOrElse("").trim
    if normalized.isEmpty then "endgame pattern"
    else if normalized.equalsIgnoreCase("none") then "no stable endgame pattern"
    else
      normalized
        .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
        .replace('_', ' ')

  private def endgameTaskPhrase(rawHint: String): String =
    Option(rawHint).getOrElse("").trim.toLowerCase match
      case "win"     => "winning method"
      case "draw"    => "drawing setup"
      case "unclear" => "technical plan"
      case other if other.nonEmpty => s"${other} technical task"
      case _ => "technical plan"

  private def hasPlanRaceCandidate(inputs: QuestionPlannerInputs): Boolean =
    val opponentRaceAvailable =
      inputs.opponentPlan.exists(plan => cleanLine(plan.name).nonEmpty) ||
        bestImmediateThreat(inputs.opponentThreats).nonEmpty ||
        (
          !inputs.heavyPieceLocalBindBlocked &&
            inputs.preventedPlansNow.exists(_.breakNeutralized.exists(_.trim.nonEmpty))
        )
    val ownRaceAvailable =
      inputs.decisionFrame.intent.nonEmpty ||
        (inputs.decisionFrame.battlefront.nonEmpty && inputs.evidenceBackedPlans.nonEmpty)
    val timingAnchorAvailable = concreteRaceUrgency(inputs).nonEmpty || bestImmediateThreat(inputs.opponentThreats).nonEmpty
    ownRaceAvailable && opponentRaceAvailable && timingAnchorAvailable

  private def hasDomainTransitionOverlap(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      families: Set[OwnerFamily]
  ): Boolean =
    val pairedTranslators =
      families.contains(OwnerFamily.OpeningRelation) &&
        families.contains(OwnerFamily.EndgameTransition)
    val moveTransitionAnchor =
      families.contains(OwnerFamily.MoveDelta) ||
        truthContract.exists(_.reasonFamily == DecisiveReasonFamily.Conversion) ||
        inputs.pvDelta.exists(delta =>
          delta.resolvedThreats.nonEmpty || delta.newOpportunities.nonEmpty || delta.planAdvancements.nonEmpty
        )
    pairedTranslators && moveTransitionAnchor

  private def classifySceneTrace(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      candidates: List[OwnerCandidateTrace]
  ): SceneClassificationTrace =
    val families = candidates.filterNot(_.supportMaterial).map(_.family).toSet
    val hasTransitionAnchor =
      inputs.pvDelta.exists(delta =>
        delta.resolvedThreats.nonEmpty || delta.newOpportunities.nonEmpty || delta.planAdvancements.nonEmpty
      )
    val hasTranslatorOverlap =
      hasDomainTransitionOverlap(inputs, truthContract, families)
    val truthSignalsConversion =
      truthContract.exists(_.reasonFamily == DecisiveReasonFamily.Conversion)
    if families.contains(OwnerFamily.TacticalFailure) then
      SceneClassificationTrace(
        sceneType = SceneType.TacticalFailure,
        reasons = List("owner_family=TacticalFailure")
      )
    else if families.contains(OwnerFamily.PlanRace) then
      SceneClassificationTrace(
        sceneType = SceneType.PlanClash,
        reasons = List("owner_family=PlanRace")
      )
    else if families.contains(OwnerFamily.ForcingDefense) then
      SceneClassificationTrace(
        sceneType = SceneType.ForcingDefense,
        reasons =
          List("owner_family=ForcingDefense") ++
            truthContract
              .filter(_.reasonFamily == DecisiveReasonFamily.OnlyMoveDefense)
              .map(_ => "truth_reason=OnlyMoveDefense")
      )
    else if hasTranslatorOverlap || truthSignalsConversion || hasTransitionAnchor then
      SceneClassificationTrace(
        sceneType = SceneType.TransitionConversion,
        reasons =
          List(
            Option.when(hasTranslatorOverlap)("domain_transition_overlap"),
            Option.when(truthSignalsConversion)("truth_reason=Conversion"),
            Option.when(hasTransitionAnchor)("pv_delta_transition_anchor")
          ).flatten
      )
    else if families.contains(OwnerFamily.OpeningRelation) then
      SceneClassificationTrace(
        sceneType = SceneType.OpeningRelation,
        reasons = List("owner_family=OpeningRelation")
      )
    else if families.contains(OwnerFamily.EndgameTransition) then
      SceneClassificationTrace(
        sceneType = SceneType.EndgameTransition,
        reasons = List("owner_family=EndgameTransition")
      )
    else
      SceneClassificationTrace(
        sceneType = SceneType.QuietImprovement,
        reasons = List("default_quiet_improvement")
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
