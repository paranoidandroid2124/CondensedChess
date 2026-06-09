package lila.commentary.analysis

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

  final case class Admission(
      family: Family,
      authority: Authority,
      strictFallbackEligible: Boolean,
      guardrails: List[String] = Nil
  ):
    def tags: List[String] =
      (List(
        s"local_fact_family:${family.key}",
        s"local_fact_authority:${authority.key}",
        s"local_fact_strict:${strictFallbackEligible.toString}"
      ) ++ guardrails.map(guardrail => s"local_fact_guardrail:$guardrail")).distinct
