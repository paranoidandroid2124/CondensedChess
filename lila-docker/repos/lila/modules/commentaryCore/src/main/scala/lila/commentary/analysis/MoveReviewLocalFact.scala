package lila.commentary.analysis

import lila.commentary.model.authoring.AuthorQuestionKind

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
    case CanonicalFact, CertifiedStrategy, OpeningGoalEvidence, PvCoupledLine, TruthContract

    def key: String =
      this match
        case CanonicalFact       => "canonical_fact"
        case CertifiedStrategy   => "certified_strategy"
        case OpeningGoalEvidence => "opening_goal_evidence"
        case PvCoupledLine       => "pv_coupled_line"
        case TruthContract       => "truth_contract"

  enum Source:
    case CanonicalFact, CertifiedStrategy, OpeningGoalEvidence, PvCoupledLine, TruthContract

    def key: String =
      this match
        case CanonicalFact       => "canonical_fact"
        case CertifiedStrategy   => "certified_strategy"
        case OpeningGoalEvidence => "opening_goal_evidence"
        case PvCoupledLine       => "pv_coupled_line"
        case TruthContract       => "truth_contract"

    def authority: Authority =
      this match
        case CanonicalFact       => Authority.CanonicalFact
        case CertifiedStrategy   => Authority.CertifiedStrategy
        case OpeningGoalEvidence => Authority.OpeningGoalEvidence
        case PvCoupledLine       => Authority.PvCoupledLine
        case TruthContract       => Authority.TruthContract

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
      subject: Subject,
      strictFallbackCandidate: Boolean,
      anchors: List[Anchor] = Nil,
      lineBinding: LineBinding = LineBinding.None,
      evidenceRefs: List[String] = Nil,
      guardrails: List[String] = Nil
  )

  final case class Decision(
      admission: Option[Admission],
      rejectReasons: List[String] = Nil
  )

  final case class Admission(
      family: Family,
      authority: Authority,
      strictFallbackEligible: Boolean,
      guardrails: List[String] = Nil,
      anchors: List[Anchor] = Nil,
      lineBinding: LineBinding = LineBinding.None,
      evidenceRefs: List[String] = Nil
  ):
    def tags: List[String] =
      (List(
        s"local_fact_family:${family.key}",
        s"local_fact_authority:${authority.key}",
        s"local_fact_strict:${strictFallbackEligible.toString}"
      ) ++ guardrails.map(guardrail => s"local_fact_guardrail:$guardrail")).distinct

  def admit(candidate: Candidate): Decision =
    Decision(
      admission =
        Some(
          Admission(
            family = candidate.family,
            authority = candidate.source.authority,
            strictFallbackEligible = candidate.strictFallbackCandidate,
            guardrails = candidate.guardrails,
            anchors = candidate.anchors,
            lineBinding = candidate.lineBinding,
            evidenceRefs = candidate.evidenceRefs
          )
        )
    )

  def admitted(candidate: Candidate): Admission =
    admit(candidate).admission.getOrElse(
      throw new IllegalArgumentException(s"MoveReview local fact rejected: ${candidate.family.key}")
    )

  def admitPlanner(
      plan: QuestionPlan,
      evidenceKinds: List[String],
      relationKinds: List[String],
      lineConsequenceBacked: Boolean,
      lineBinding: LineBinding = LineBinding.None
  ): Decision =
    plannerCandidate(plan, evidenceKinds, relationKinds, lineConsequenceBacked, lineBinding)
      .fold(Decision(None, List("local_fact_family_missing")))(admit)

  def strategicMoveDeltaCandidate(
      delta: PlayerFacingMoveDeltaEvidence,
      anchors: List[Anchor],
      guardrails: List[String]
  ): Candidate =
    Candidate(
      family = familyForMoveDeltaClass(delta.deltaClass),
      source = Source.CertifiedStrategy,
      subject = Subject.PlanResource,
      strictFallbackCandidate = false,
      anchors = anchors,
      lineBinding = LineBinding.PvCoupled,
      evidenceRefs =
        List(
          Option(delta.packet.proofFamily).map(_.trim).filter(_.nonEmpty).map(value => s"proof_family:$value"),
          Option(delta.packet.proofSource).map(_.trim).filter(_.nonEmpty).map(value => s"proof_source:$value")
        ).flatten,
      guardrails = guardrails
    )

  def familyForMoveDeltaClass(deltaClass: PlayerFacingMoveDeltaClass): Family =
    deltaClass match
      case PlayerFacingMoveDeltaClass.CounterplayReduction => Family.Defense
      case PlayerFacingMoveDeltaClass.ExchangeForcing      => Family.LineConsequence
      case PlayerFacingMoveDeltaClass.PlanAdvance          => Family.PlanSupport
      case PlayerFacingMoveDeltaClass.ResourceRemoval      => Family.Defense
      case PlayerFacingMoveDeltaClass.PressureIncrease     => Family.Pressure
      case PlayerFacingMoveDeltaClass.NewAccess            => Family.LineConsequence

  private def plannerCandidate(
      plan: QuestionPlan,
      evidenceKinds: List[String],
      relationKinds: List[String],
      lineConsequenceBacked: Boolean,
      lineBinding: LineBinding
  ): Option[Candidate] =
    val normalizedEvidence = evidenceKinds.map(normalize)
    val normalizedRelations = relationKinds.map(normalize)
    plannerFamily(plan, normalizedRelations, lineConsequenceBacked).map { family =>
      Candidate(
        family = family,
        source = plannerSource(plan, normalizedEvidence, lineConsequenceBacked),
        subject = plannerSubject(plan),
        strictFallbackCandidate = strictFallbackEligible(plan, family, normalizedRelations),
        anchors = plannerAnchors(plan),
        lineBinding = plannerLineBinding(normalizedEvidence, lineConsequenceBacked, lineBinding),
        evidenceRefs = normalizedEvidence.map(kind => s"evidence_kind:$kind"),
        guardrails =
          List(
            s"planner_owner:${plan.plannerOwnerKind.wireName}",
            s"planner_source:${plan.plannerSource}"
          ) ++
            plan.sourceKinds.map(source => s"source_kind:$source") ++
            normalizedRelations.map(relation => s"causal_relation:$relation")
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
    else
      plan.plannerOwnerKind match
        case PlannerOwnerKind.ForcingDefense    => Some(Family.Defense)
        case PlannerOwnerKind.DecisionTiming    => Some(Family.Timing)
        case PlannerOwnerKind.PlanRace          => Some(Family.Timing)
        case PlannerOwnerKind.PositionProbe     => Some(Family.Pressure)
        case PlannerOwnerKind.OpeningRelation   => Some(Family.OpeningGoal)
        case PlannerOwnerKind.EndgameTransition => Some(Family.Endgame)
        case PlannerOwnerKind.TacticalFailure =>
          if sourceLooksTactical(plan) then Some(Family.Threat)
          else None
        case PlannerOwnerKind.MoveDelta =>
          moveDeltaFamilyFromSourceKinds(plan.sourceKinds)

  private def moveDeltaFamilyFromSourceKinds(sourceKinds: List[String]): Option[Family] =
    val sources = sourceKinds.map(normalize)
    if sources.exists(_.contains("timing")) then Some(Family.Timing)
    else if sources.exists(source => source.contains("threat") || source.contains("tactical")) then Some(Family.Threat)
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
    else if sources.exists(_.contains("pv_delta")) then Some(Family.LineConsequence)
    else None

  private def plannerSource(
      plan: QuestionPlan,
      evidenceKinds: List[String],
      lineConsequenceBacked: Boolean
  ): Source =
    val sources = plan.sourceKinds.map(normalize)
    if sources.exists(_.contains("truth_contract")) || plan.plannerOwnerKind == PlannerOwnerKind.ForcingDefense then
      Source.TruthContract
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
        sourceLooksTactical(plan) &&
          plan.sourceKinds.exists(source => normalize(source).contains("canonical"))
      case _ => false

  private def plannerSubject(plan: QuestionPlan): Subject =
    if plan.questionKind == AuthorQuestionKind.WhatMustBeStopped ||
        plan.sourceKinds.exists(kind => normalize(kind).contains("reply") || normalize(kind).contains("threat"))
    then Subject.LineOrReply
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

  private def sourceLooksTactical(plan: QuestionPlan): Boolean =
    plan.sourceKinds.exists { source =>
      val low = normalize(source)
      low.contains("tactical") || low.contains("threat")
    }

  private def normalize(value: String): String =
    Option(value).getOrElse("").trim.toLowerCase
