package lila.commentary.analysis

import lila.commentary.model.authoring.AuthorQuestionKind
import lila.commentary.analysis.semantic.RelationSurfaceRowKind

private[commentary] object MoveReviewLocalFact:

  enum Family:
    case Attack, Defense, Threat, Pressure, PlanSupport, LineConsequence, Timing
    case OpeningGoal, KingSafety, Capture, Endgame

    def key: String =
      this match
        case Attack          => "attack"
        case Defense         => "defense"
        case Threat          => "threat"
        case Pressure        => "pressure"
        case PlanSupport     => "plan_support"
        case LineConsequence => "line_consequence"
        case Timing          => "timing"
        case OpeningGoal     => "opening_goal"
        case KingSafety      => "king_safety"
        case Capture         => "capture"
        case Endgame         => "endgame"

  enum Authority:
    case CanonicalFact, CertifiedStrategy, OpeningGoalEvidence, PvCoupledLine, OnlyMoveDefense, ForcedReply, AlternativeComparison

    def key: String =
      this match
        case CanonicalFact       => "canonical_fact"
        case CertifiedStrategy   => "certified_strategy"
        case OpeningGoalEvidence => "opening_goal_evidence"
        case PvCoupledLine       => "pv_coupled_line"
        case OnlyMoveDefense     => "only_move_defense"
        case ForcedReply         => "forced_reply"
        case AlternativeComparison => "alternative_comparison"

  enum Source:
    case CanonicalFact, CertifiedStrategy, OpeningGoalEvidence, PvCoupledLine, OnlyMoveDefense, ForcedReply, AlternativeComparison

    def key: String =
      this match
        case CanonicalFact       => "canonical_fact"
        case CertifiedStrategy   => "certified_strategy"
        case OpeningGoalEvidence => "opening_goal_evidence"
        case PvCoupledLine       => "pv_coupled_line"
        case OnlyMoveDefense     => "only_move_defense"
        case ForcedReply         => "forced_reply"
        case AlternativeComparison => "alternative_comparison"

    def authority: Authority =
      this match
        case CanonicalFact       => Authority.CanonicalFact
        case CertifiedStrategy   => Authority.CertifiedStrategy
        case OpeningGoalEvidence => Authority.OpeningGoalEvidence
        case PvCoupledLine       => Authority.PvCoupledLine
        case OnlyMoveDefense     => Authority.OnlyMoveDefense
        case ForcedReply         => Authority.ForcedReply
        case AlternativeComparison => Authority.AlternativeComparison

  object Source:
    def fromAuthority(authority: Authority): Option[Source] =
      Source.values.find(_.authority == authority)

  enum Producer:
    case OpeningGoal
    case CastlingSafety
    case CertifiedStrategyDelta
    case TacticalMotif
    case ForcedLineTruth
    case TargetPressure
    case TargetDefense
    case ForkEntryDefense
    case RelationWitness
    case DefensiveTruth
    case CaptureSequence
    case EndgameFact
    case LineConsequence
    case ClaimAuthorityPromotion
    case PlannerCausalClaim
    case ForcedReply
    case AlternativeComparison

    def key: String =
      this match
        case OpeningGoal            => "opening_goal"
        case CastlingSafety         => "castling_safety"
        case CertifiedStrategyDelta => "certified_strategy_delta"
        case TacticalMotif          => "tactical_motif"
        case ForcedLineTruth        => "forced_line_truth"
        case TargetPressure         => "target_pressure"
        case TargetDefense          => "target_defense"
        case ForkEntryDefense       => "fork_entry_defense"
        case RelationWitness        => "relation_witness"
        case DefensiveTruth         => "defensive_truth"
        case CaptureSequence        => "capture_sequence"
        case EndgameFact            => "endgame_fact"
        case LineConsequence        => "line_consequence"
        case ClaimAuthorityPromotion => "claim_authority_promotion"
        case PlannerCausalClaim     => "planner_causal_claim"
        case ForcedReply            => "forced_reply"
        case AlternativeComparison  => "alternative_comparison"

    def supports(family: Family, source: Source): Boolean =
      allowedFamilies.contains(family) && allowedSources.contains(source)

    private def allowedFamilies: Set[Family] =
      this match
        case OpeningGoal            => Set(Family.OpeningGoal)
        case CastlingSafety         => Set(Family.KingSafety)
        case CertifiedStrategyDelta =>
          Set(Family.Defense, Family.LineConsequence, Family.PlanSupport, Family.Pressure, Family.Timing)
        case TacticalMotif          => Set(Family.Threat)
        case ForcedLineTruth        => Set(Family.Attack, Family.Threat, Family.Defense)
        case TargetPressure         => Set(Family.Pressure)
        case TargetDefense          => Set(Family.Defense)
        case ForkEntryDefense       => Set(Family.Defense)
        case RelationWitness        => Set(Family.Attack, Family.Threat, Family.Pressure, Family.Defense, Family.Timing)
        case DefensiveTruth         => Set(Family.Defense)
        case CaptureSequence        => Set(Family.Capture)
        case EndgameFact            => Set(Family.Endgame)
        case LineConsequence        => Set(Family.LineConsequence)
        case ClaimAuthorityPromotion =>
          Set(
            Family.Attack,
            Family.Defense,
            Family.Threat,
            Family.Pressure,
            Family.PlanSupport,
            Family.LineConsequence,
            Family.Timing
          )
        case PlannerCausalClaim =>
          Set(
            Family.Attack,
            Family.Defense,
            Family.Threat,
            Family.Pressure,
            Family.PlanSupport,
            Family.LineConsequence,
            Family.Timing,
            Family.OpeningGoal,
            Family.Endgame
          )
        case ForcedReply => Set(Family.Defense, Family.Timing)
        case AlternativeComparison => Set(Family.LineConsequence)

    private def allowedSources: Set[Source] =
      this match
        case OpeningGoal            => Set(Source.OpeningGoalEvidence)
        case CastlingSafety         => Set(Source.PvCoupledLine)
        case CertifiedStrategyDelta => Set(Source.CertifiedStrategy, Source.PvCoupledLine)
        case TacticalMotif          => Set(Source.CanonicalFact)
        case ForcedLineTruth        => Set(Source.PvCoupledLine)
        case TargetPressure         => Set(Source.CanonicalFact)
        case TargetDefense          => Set(Source.CanonicalFact)
        case ForkEntryDefense       => Set(Source.CanonicalFact)
        case RelationWitness        => Set(Source.PvCoupledLine)
        case DefensiveTruth         => Set(Source.OnlyMoveDefense)
        case CaptureSequence        => Set(Source.PvCoupledLine)
        case EndgameFact            => Set(Source.CanonicalFact)
        case LineConsequence        => Set(Source.PvCoupledLine)
        case ClaimAuthorityPromotion => Set(Source.CertifiedStrategy, Source.PvCoupledLine)
        case PlannerCausalClaim =>
          Set(Source.CertifiedStrategy, Source.OpeningGoalEvidence, Source.PvCoupledLine, Source.OnlyMoveDefense)
        case ForcedReply            => Set(Source.ForcedReply)
        case AlternativeComparison  => Set(Source.AlternativeComparison)

  object Producer:
    val registry: List[Producer] =
      Producer.values.toList

  enum Subject:
    case PlayedMove, LineOrReply, Target, PlanResource, OpeningGoal, KingSafety, Capture, Endgame

    def key: String =
      this match
        case PlayedMove   => "played_move"
        case LineOrReply  => "line_or_reply"
        case Target       => "target"
        case PlanResource => "plan_resource"
        case OpeningGoal  => "opening_goal"
        case KingSafety   => "king_safety"
        case Capture      => "capture"
        case Endgame      => "endgame"

  enum LineBinding:
    case None, PvCoupled, Replayed, BranchScoped

    def key: String =
      this match
        case None         => "none"
        case PvCoupled    => "pv_coupled"
        case Replayed     => "replayed"
        case BranchScoped => "branch_scoped"

  final case class Anchor(key: String, value: String)

  final case class Candidate(
      family: Family,
      source: Source,
      producer: Producer,
      subject: Subject,
      strictFallbackCandidate: Boolean,
      anchors: List[Anchor] = Nil,
      lineBinding: LineBinding = LineBinding.None,
      evidenceRefs: List[String] = Nil,
      guardrails: List[String] = Nil,
      relationSurface: Option[RelationSurfaceRowKind] = None
  )

  final case class Decision(
      admission: Option[Admission],
      rejectReasons: List[String] = Nil
  )

  final case class Admission(
      family: Family,
      authority: Authority,
      producer: Producer,
      strictFallbackEligible: Boolean,
      guardrails: List[String] = Nil,
      anchors: List[Anchor] = Nil,
      lineBinding: LineBinding = LineBinding.None,
      evidenceRefs: List[String] = Nil,
      relationSurface: Option[RelationSurfaceRowKind] = None
  ):
    def tags: List[String] =
      (List(
        s"local_fact_family:${family.key}",
        s"local_fact_authority:${authority.key}",
        s"local_fact_producer:${producer.key}",
        s"local_fact_strict:${strictFallbackEligible.toString}"
      ) ++ guardrails.map(guardrail => s"local_fact_guardrail:$guardrail")).distinct

  def admit(candidate: Candidate): Decision =
    if !Producer.registry.contains(candidate.producer) then
      Decision(
        admission = None,
        rejectReasons = List(s"producer_unregistered:${candidate.producer.key}")
      )
    else if !candidate.producer.supports(candidate.family, candidate.source) then
      Decision(
        admission = None,
        rejectReasons = List(s"producer_family_source_mismatch:${candidate.producer.key}:${candidate.family.key}:${candidate.source.key}")
      )
    else
      Decision(
        admission =
          Some(
            Admission(
              family = candidate.family,
              authority = candidate.source.authority,
              producer = candidate.producer,
              strictFallbackEligible = candidate.strictFallbackCandidate,
              guardrails = candidate.guardrails,
              anchors = candidate.anchors,
              lineBinding = candidate.lineBinding,
              evidenceRefs = candidate.evidenceRefs,
              relationSurface = candidate.relationSurface
            )
        )
      )

  def admitted(candidate: Candidate): Admission =
    admit(candidate).admission.getOrElse(
      throw new IllegalArgumentException(s"MoveReview local fact rejected: ${candidate.family.key}:${candidate.producer.key}")
    )

  def candidateFromAdmission(admission: Admission): Option[Candidate] =
    Source.fromAuthority(admission.authority).map { source =>
      Candidate(
        family = admission.family,
        source = source,
        producer = admission.producer,
        subject = subjectForFamily(admission.family),
        strictFallbackCandidate = admission.strictFallbackEligible,
        anchors = admission.anchors,
        lineBinding = admission.lineBinding,
        evidenceRefs = admission.evidenceRefs,
        guardrails = admission.guardrails,
        relationSurface = admission.relationSurface
      )
    }

  def admitPlanner(
      plan: QuestionPlan,
      evidenceKinds: List[String],
      relationKinds: List[String],
      lineConsequenceBacked: Boolean,
      lineBinding: LineBinding = LineBinding.None,
      evidenceRefs: List[String] = Nil,
      guardrails: List[String] = Nil,
      anchors: List[Anchor] = Nil
  ): Decision =
    plannerCandidate(plan, evidenceKinds, relationKinds, lineConsequenceBacked, lineBinding, evidenceRefs, guardrails, anchors)
      .fold(Decision(None, List("local_fact_family_missing")))(admit)

  def strategicMoveDeltaCandidate(
      delta: PlayerFacingMoveDeltaEvidence,
      anchors: List[Anchor],
      guardrails: List[String],
      evidenceRefs: List[String] = Nil,
      strictFallbackCandidate: Boolean = false
  ): Candidate =
    Candidate(
      family = familyForMoveDeltaEvidence(delta),
      source = Source.CertifiedStrategy,
      producer = Producer.CertifiedStrategyDelta,
      subject = Subject.PlanResource,
      strictFallbackCandidate = strictFallbackCandidate,
      anchors = anchors,
      lineBinding = LineBinding.PvCoupled,
      evidenceRefs =
        List(
          Option(delta.packet.proofFamily).map(_.trim).filter(_.nonEmpty).map(value => s"proof_family:$value"),
          Option(delta.packet.proofSource).map(_.trim).filter(_.nonEmpty).map(value => s"proof_source:$value")
        ).flatten ++ evidenceRefs.map(_.trim).filter(_.nonEmpty),
      guardrails = guardrails
    )

  def lineConsequenceCandidate(
      evidence: LineConsequenceEvidence,
      strictFallbackCandidate: Boolean
  ): Candidate =
    Candidate(
      family = Family.LineConsequence,
      source = Source.PvCoupledLine,
      producer = Producer.LineConsequence,
      subject = Subject.PlayedMove,
      strictFallbackCandidate = strictFallbackCandidate,
      anchors = lineConsequenceAnchors(evidence),
      lineBinding = LineBinding.PvCoupled,
      evidenceRefs = lineConsequenceEvidenceRefs(evidence),
      guardrails = lineConsequenceGuardrails(evidence)
    )

  def lineConsequenceEvidenceRefs(evidence: LineConsequenceEvidence): List[String] =
    (
      List(
        s"line_consequence_kind:${wireKey(evidence.kind)}",
        s"line_consequence_release:${wireKey(evidence.release)}",
        s"line_consequence_window_ply:${evidence.windowPly}"
      ) ++
        evidence.lineId.map(lineId => s"line_consequence_line_id:$lineId") ++
        evidence.triggerSan.map(san => s"line_consequence_trigger_san:$san") ++
        evidence.scoreCp.map(cp => s"line_consequence_score_cp:$cp") ++
        evidence.mate.map(mate => s"line_consequence_mate:$mate") ++
        evidence.depth.map(depth => s"line_consequence_depth:$depth") ++
        evidence.uciMoves.take(6).map(uci => s"line_consequence_uci:${MoveReviewPvLine.normalizeUci(uci)}") ++
        evidence.structureDetails.take(3).flatMap(_.evidenceRefs) ++
        evidence.targetDetails.take(3).flatMap(_.evidenceRefs)
    ).map(_.trim).filter(_.nonEmpty).distinct

  def lineConsequenceAnchors(evidence: LineConsequenceEvidence): List[Anchor] =
    (
      List(
        Some(Anchor("line_consequence_kind", wireKey(evidence.kind))),
        evidence.lineId.map(lineId => Anchor("line_id", lineId)),
        evidence.triggerSan.map(san => Anchor("trigger_san", san))
      ).flatten ++
        evidence.uciMoves.headOption.map(uci => Anchor("first_uci", MoveReviewPvLine.normalizeUci(uci)))
    ).distinct

  def lineConsequenceGuardrails(evidence: LineConsequenceEvidence): List[String] =
    (
      List(
        s"line_consequence_kind:${wireKey(evidence.kind)}",
        s"line_consequence_release:${wireKey(evidence.release)}"
      ) ++ evidence.rejectReasons.map(reason => s"line_consequence_reject:$reason")
    ).distinct

  def familyForMoveDeltaClass(deltaClass: PlayerFacingMoveDeltaClass): Family =
    deltaClass match
      case PlayerFacingMoveDeltaClass.CounterplayReduction => Family.Defense
      case PlayerFacingMoveDeltaClass.ExchangeForcing      => Family.LineConsequence
      case PlayerFacingMoveDeltaClass.PlanAdvance          => Family.PlanSupport
      case PlayerFacingMoveDeltaClass.ResourceRemoval      => Family.Defense
      case PlayerFacingMoveDeltaClass.PressureIncrease     => Family.Pressure
      case PlayerFacingMoveDeltaClass.NewAccess            => Family.LineConsequence

  def familyForMoveDeltaEvidence(delta: PlayerFacingMoveDeltaEvidence): Family =
    delta.packet.proofPathWitness.exactSliceProof
      .map(familyForExactProof)
      .getOrElse(familyForMoveDeltaClass(delta.deltaClass))

  def familyForExactProof(proof: PlayerFacingExactSliceProof): Family =
    proof match
      case _: PlayerFacingExactSliceProof.CounterplayAxisSuppression =>
        Family.Defense
      case _: PlayerFacingExactSliceProof.ProphylacticRestraint =>
        Family.Defense
      case _: PlayerFacingExactSliceProof.QueenTradeShield =>
        Family.LineConsequence
      case _: PlayerFacingExactSliceProof.DefenderTrade =>
        Family.LineConsequence
      case _: PlayerFacingExactSliceProof.BadPieceLiquidation =>
        Family.LineConsequence
      case _: PlayerFacingExactSliceProof.SimplificationWindow =>
        Family.LineConsequence
      case _: PlayerFacingExactSliceProof.CentralBreakTiming =>
        Family.Timing
      case _: PlayerFacingExactSliceProof.ExactTargetFixation =>
        Family.Pressure
      case _: PlayerFacingExactSliceProof.CarlsbadFixedTarget =>
        Family.Pressure
      case _: PlayerFacingExactSliceProof.TargetFocusedCoordination =>
        Family.Pressure
      case _: PlayerFacingExactSliceProof.ColorComplexSqueeze =>
        Family.Pressure
      case _: PlayerFacingExactSliceProof.LocalFileEntryBind =>
        Family.Pressure
      case _: PlayerFacingExactSliceProof.OutpostOccupation =>
        Family.Pressure
      case _: PlayerFacingExactSliceProof.IqpInducement =>
        Family.Pressure

  private def subjectForFamily(family: Family): Subject =
    family match
      case Family.Attack | Family.Defense | Family.Threat => Subject.LineOrReply
      case Family.PlanSupport                             => Subject.PlanResource
      case Family.OpeningGoal                             => Subject.OpeningGoal
      case Family.KingSafety                              => Subject.KingSafety
      case Family.Capture                                 => Subject.Capture
      case Family.Endgame                                 => Subject.Endgame
      case _                                              => Subject.PlayedMove

  private def plannerCandidate(
      plan: QuestionPlan,
      evidenceKinds: List[String],
      relationKinds: List[String],
      lineConsequenceBacked: Boolean,
      lineBinding: LineBinding,
      evidenceRefs: List[String],
      guardrails: List[String],
      anchors: List[Anchor]
  ): Option[Candidate] =
    val normalizedEvidence = evidenceKinds.map(normalize)
    val normalizedRelations = relationKinds.map(normalize)
    plannerFamily(plan, normalizedRelations, lineConsequenceBacked).map { family =>
      val source = plannerSource(plan, normalizedEvidence, lineConsequenceBacked)
      val producer =
        if source == Source.AlternativeComparison then Producer.AlternativeComparison
        else Producer.PlannerCausalClaim
      Candidate(
        family = family,
        source = source,
        producer = producer,
        subject = plannerSubject(plan),
        strictFallbackCandidate = strictFallbackEligible(plan, family, normalizedRelations),
        anchors = (plannerAnchors(plan) ++ anchors).distinct,
        lineBinding = plannerLineBinding(normalizedEvidence, lineConsequenceBacked, lineBinding),
        evidenceRefs =
          (normalizedEvidence.map(kind => s"evidence_kind:$kind") ++
            evidenceRefs.map(_.trim).filter(_.nonEmpty)).distinct,
        guardrails =
          (
            List(
              s"planner_owner:${plan.plannerOwnerKind.wireName}",
              s"planner_source:${plan.plannerSource}"
            ) ++
              plan.sourceKinds.map(source => s"source_kind:$source") ++
              normalizedRelations.map(relation => s"causal_relation:$relation") ++
              guardrails.map(_.trim).filter(_.nonEmpty)
          ).distinct
      )
    }

  private def plannerFamily(
      plan: QuestionPlan,
      relationKinds: List[String],
      lineConsequenceBacked: Boolean
  ): Option[Family] =
    if lineConsequenceBacked then Some(Family.LineConsequence)
    else if relationKinds.contains("timing_constraint") then Some(Family.Timing)
    else if relationKinds.contains("defensive_resource") then Some(Family.Defense)
    else if relationKinds.contains("plan_race") then Some(Family.Timing)
    else if decisionComparisonChangeConsequence(plan, relationKinds) then Some(Family.LineConsequence)
    else
      plan.plannerOwnerKind match
        case PlannerOwnerKind.ForcingDefense    => Some(Family.Defense)
        case PlannerOwnerKind.DecisionTiming    => Some(Family.Timing)
        case PlannerOwnerKind.PlanRace          => Some(Family.Timing)
        case PlannerOwnerKind.PositionProbe     => Some(Family.Pressure)
        case PlannerOwnerKind.OpeningRelation   => Some(Family.OpeningGoal)
        case PlannerOwnerKind.EndgameTransition => Some(Family.Endgame)
        case PlannerOwnerKind.ConcreteTactical =>
          if sourceLooksOwnedTactical(plan) then Some(Family.Threat)
          else None
        case PlannerOwnerKind.LineConsequence =>
          Some(Family.LineConsequence)
        case PlannerOwnerKind.AlternativeComparison =>
          if roleAwareLineConsequenceSource(plan) then Some(Family.LineConsequence)
          else None
        case PlannerOwnerKind.MoveDelta =>
          moveDeltaFamilyFromSourceKinds(plan.sourceKinds)

  private def decisionComparisonChangeConsequence(
      plan: QuestionPlan,
      relationKinds: List[String]
  ): Boolean =
    plan.questionKind == AuthorQuestionKind.WhatChanged &&
      relationKinds.contains("change_consequence") &&
      (normalize(plan.plannerSource) == "decision_comparison" ||
        plan.sourceKinds.exists(source => normalize(source) == "decision_comparison"))

  private def roleAwareLineConsequenceSource(plan: QuestionPlan): Boolean =
    plan.sourceKinds.exists(source =>
      normalize(source) == normalize(DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource)
    )

  private def moveDeltaFamilyFromSourceKinds(sourceKinds: List[String]): Option[Family] =
    val sources = sourceKinds.map(normalize)
    if sources.exists(_.contains("timing")) then Some(Family.Timing)
    else if sources.exists(sourceLooksOwnedTactical) then Some(Family.Threat)
    else if sources.exists(source =>
      source.contains("prevented_plan") ||
        source.contains("defense") ||
        source.contains("neutralize_key_break") ||
        source.contains("counterplay") ||
        source.contains("local_file_entry") ||
        source.contains("prophylactic")
    ) then Some(Family.Defense)
    else if sources.exists(source =>
      source.contains("position_probe") ||
        source.contains("target") ||
        source.contains("iqp_inducement") ||
        source.contains("carlsbad") ||
        source.contains("color_complex") ||
        source.contains("outpost") ||
        source.contains("half_open_file_pressure")
    ) then Some(Family.Pressure)
    else if sources.exists(source =>
      source.contains("defender_trade") ||
        source.contains("bad_piece_liquidation") ||
        source.contains("queen_trade_shield") ||
        source.contains("simplification") ||
        source.contains("exchange")
    ) then Some(Family.LineConsequence)
    else if sources.exists(source =>
      source.contains("plan_support") ||
        source.contains("plan_advance")
    ) then Some(Family.PlanSupport)
    else if sources.exists(_.contains("pv_delta")) then Some(Family.LineConsequence)
    else None

  private def plannerSource(
      plan: QuestionPlan,
      evidenceKinds: List[String],
      lineConsequenceBacked: Boolean
  ): Source =
    val sources =
      (
        plan.sourceKinds ++
          List(plan.plannerSource) ++
          plan.timingWitness.toList.map(_.source)
      ).map(normalize)
    if plan.plannerOwnerKind == PlannerOwnerKind.AlternativeComparison && roleAwareLineConsequenceSource(plan) then
      Source.AlternativeComparison
    else if sources.exists(_.contains("only_move_defense")) then
      Source.OnlyMoveDefense
    else if lineConsequenceBacked || evidenceKinds.contains("branch_line") then
      Source.PvCoupledLine
    else if plan.plannerOwnerKind == PlannerOwnerKind.OpeningRelation then
      Source.OpeningGoalEvidence
    else Source.CertifiedStrategy

  private def strictFallbackEligible(
      plan: QuestionPlan,
      family: Family,
      relationKinds: List[String]
  ): Boolean =
    family match
      case Family.Defense =>
        plan.plannerOwnerKind == PlannerOwnerKind.ForcingDefense &&
          relationKinds.contains("defensive_resource")
      case Family.Timing =>
        relationKinds.contains("timing_constraint") &&
          plan.timingWitness.nonEmpty
      case Family.Threat =>
        sourceLooksOwnedTactical(plan) &&
          plan.sourceKinds.exists(source => normalize(source).contains("canonical"))
      case _ => false

  private def plannerSubject(plan: QuestionPlan): Subject =
    if plan.questionKind == AuthorQuestionKind.WhatMustBeStopped ||
        plan.sourceKinds.exists(kind => normalize(kind).contains("reply") || normalize(kind).contains("threat"))
    then Subject.LineOrReply
    else if plan.sourceKinds.exists(kind => normalize(kind).contains("plan_support")) then Subject.PlanResource
    else Subject.PlayedMove

  private def plannerAnchors(plan: QuestionPlan): List[Anchor] =
    List(
      Anchor("planner_owner", plan.plannerOwnerKind.wireName),
      Anchor("planner_source", plan.plannerSource)
    ) ++ plan.timingWitness.toList.flatMap { witness =>
      List(
        witness.namedBreak.map(value => Anchor("timing_break", value)),
        witness.continuationMove.map(value => Anchor("timing_continuation", value)),
        witness.branchKey.map(value => Anchor("timing_branch", value))
      ).flatten ++ witness.witnessTokens.map(token => Anchor("timing_token", token))
    }

  private def plannerLineBinding(
      evidenceKinds: List[String],
      lineConsequenceBacked: Boolean,
      lineBinding: LineBinding
  ): LineBinding =
    if lineBinding != LineBinding.None then lineBinding
    else if lineConsequenceBacked || evidenceKinds.contains("branch_line") then LineBinding.PvCoupled
    else LineBinding.None

  private def sourceLooksOwnedTactical(plan: QuestionPlan): Boolean =
    plan.sourceKinds.exists(sourceLooksOwnedTactical)

  private def sourceLooksOwnedTactical(source: String): Boolean =
    val low = normalize(source)
    !low.contains("tactical_sacrifice") &&
      (low.contains("tactical") || low.contains("threat"))

  private def normalize(value: String): String =
    Option(value).getOrElse("").trim.toLowerCase

  private def wireKey(value: Any): String =
    Option(value).map(_.toString).getOrElse("").replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase
