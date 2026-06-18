package lila.chessjudgment.model

/**
 * Tracks plan evolution across moves.
 * 
 * Core components:
 * - currentPlans: Active plan set for this move
 * - transitionType: How we arrived at current plan
 * - momentum: 0.0-1.0, persistence strength of the plan
 * - planHistory: Last N plans for pattern detection
 */
case class PlanSequence(
  currentPlans: ActivePlans,
  previousPlan: Option[Plan],
  transitionType: TransitionType,
  momentum: Double,
  planHistory: List[PlanId]
) {
  /** Check if plan has been stable for N moves */
  def isStable(moves: Int): Boolean = 
    planHistory.take(moves).distinct.size == 1
    
  /** Current primary plan ID */
  def currentPlanId: Option[PlanId] = 
    Option(currentPlans.primary).map(_.plan.id)
}

/**
 * Compact transition summary used by move judgment path.
 */
case class PlanSequenceSummary(
  transitionType: TransitionType,
  momentum: Double,
  primaryPlanId: Option[String] = None,
  secondaryPlanId: Option[String] = None
)

object PlanSequence {
  val empty: PlanSequence = PlanSequence(
    currentPlans = ActivePlans(
      primary = PlanMatch(Plan.CentralControl(chess.Color.White), 0.0, Nil),
      secondary = None,
      suppressed = Nil,
      allPlans = Nil
    ),
    previousPlan = None,
    transitionType = TransitionType.Opening,
    momentum = 0.0,
    planHistory = Nil
  )
}

enum TransitionType:
  case Continuation   // Same plan persists
  case NaturalShift   // Phase change → new plan
  case ForcedPivot    // Threat forces abandonment
  case Opportunistic  // Unexpected opportunity
  case Opening        // First move of sequence
