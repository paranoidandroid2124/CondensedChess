package lila.llm.analysis

import chess.Color
import lila.llm.model.*
import lila.llm.analysis.PlanMatcher.ActivePlans

/**
 * Analyzes plan transitions across moves.
 * 
 * Core responsibilities:
 * - Classify transition type (Continuation, NaturalShift, etc.)
 * - Calculate momentum for plan persistence
 * - Generate "why now" justifications for narrative
 */
object TransitionAnalyzer:

  private val MOMENTUM_DECAY = 0.15      // Linear decay per move
  private val MOMENTUM_BOOST = 0.25       // Boost (net +0.1 after decay)
  private val HISTORY_SIZE = 3           // Track last 3 plans
  private val MOMENTUM_MAX = 1.0
  private val MOMENTUM_MIN = 0.0

  /**
   * Main entry: analyze transition from previous to current plan.
   */
  def analyze(
    currentPlans: ActivePlans,
    previousPlan: Option[Plan],
    previousMomentum: Double,
    planHistory: List[PlanId],
    ctx: IntegratedContext
  ): PlanSequence = {
    val currPlan = currentPlans.primary.plan
    val transType = classifyTransition(previousPlan, currPlan, ctx)
    val momentum = calcMomentum(currPlan, previousPlan, previousMomentum, transType)
    val newHistory = updateHistory(currPlan.id, planHistory)
    
    PlanSequence(
      currentPlans = currentPlans,
      previousPlan = previousPlan,
      transitionType = transType,
      momentum = momentum,
      planHistory = newHistory
    )
  }

  /**
   * Classify how the plan changed.
   */
  def classifyTransition(
    prevPlan: Option[Plan],
    currPlan: Plan,
    ctx: IntegratedContext
  ): TransitionType = prevPlan match {
    case None => 
      TransitionType.Opening
      
    case Some(prev) if prev.id == currPlan.id => 
      TransitionType.Continuation
      
    case Some(prev) =>
      // ForcedPivot: tactical threat TO US forces abandonment
      if (ctx.tacticalThreatToUs)
        TransitionType.ForcedPivot
      // Opportunistic: we suddenly have tactical threat TO THEM
      else if (ctx.tacticalThreatToThem && !prev.category.isAttack)
        TransitionType.Opportunistic
      // NaturalShift: phase change or natural plan evolution
      else
        TransitionType.NaturalShift
  }

  /**
   * Calculate momentum based on transition type.
   * 
   * - Continuation: boost by 0.1, capped at 1.0
   * - ForcedPivot: reset to 0.3 (low, forced change)
   * - Opportunistic: reset to 0.6 (moderate, new attack)
   * - NaturalShift: reset to 0.5 (neutral, planned change)
   * - Opening: start at 0.5
   */
  def calcMomentum(
    _currPlan: Plan,
    _prevPlan: Option[Plan],
    prevMomentum: Double,
    transitionType: TransitionType
  ): Double = transitionType match {
    case TransitionType.Continuation =>
      (prevMomentum - MOMENTUM_DECAY + MOMENTUM_BOOST).max(MOMENTUM_MIN).min(MOMENTUM_MAX)
      
    case TransitionType.Opening =>
      0.5
      
    case TransitionType.ForcedPivot =>
      0.3
      
    case TransitionType.Opportunistic =>
      0.6
      
    case TransitionType.NaturalShift =>
      0.5
  }

  /**
   * Generate narrative explanation for the transition.
   */
  def explainTransition(
    prev: Plan,
    curr: Plan,
    ctx: IntegratedContext,
    isTacticalThreatToUs: Option[Boolean] = None,
    isTacticalThreatToThem: Option[Boolean] = None,
    phase: Option[String] = None
  ): String = {
    val side = if (ctx.isWhiteToMove) "White" else "Black"
    val eval = ctx.evalFor(if (ctx.isWhiteToMove) Color.White else Color.Black)
    
    val threatToUs = isTacticalThreatToUs.getOrElse(ctx.tacticalThreatToUs)
    val threatToThem = isTacticalThreatToThem.getOrElse(ctx.tacticalThreatToThem)
    val phaseStr = phase.getOrElse(ctx.phase)
    
    (prev.category, curr.category) match {
      // Attack → Defense: forced retreat
      case (PlanCategory.Attack, PlanCategory.Defensive) if threatToUs =>
        s"Facing a direct threat, $side is forced to pivot from the attack to defensive consolidation."
        
      // Attack → Transition: converting advantage
      case (PlanCategory.Attack, PlanCategory.Transition) if eval > 200 =>
        s"With a decisive advantage in the $phaseStr, $side shifts from attacking to simplifying the position."
        
      // Defense → Attack: counterattack
      case (PlanCategory.Defensive, PlanCategory.Attack) if threatToThem =>
        s"$side seizes the opportunity in the $phaseStr to launch a sharp counterattack."
        
      // Phase-specific transitions
      case (p, PlanCategory.Endgame) if p != PlanCategory.Endgame =>
        s"As the game transitions into the endgame, $side realigns their strategy toward ${curr.name}."

      case (PlanCategory.Structural, PlanCategory.Attack) =>
        s"Having solidified the pawn structure, $side now channels that stability into a kingside offensive."

      // Same category: evolution
      case _ if prev.category == curr.category =>
        s"$side continues to refine their ${prev.category.toString.toLowerCase} approach, specifically targeting ${curr.name}."
        
      // General phase shift
      case _ =>
        s"$side shifts focus from ${prev.name} to ${curr.name} in this stage of the $phaseStr."
    }
  }

  /**
   * Update plan history, keeping last N entries.
   */
  private def updateHistory(
    currPlanId: PlanId,
    history: List[PlanId]
  ): List[PlanId] =
    (currPlanId :: history).take(HISTORY_SIZE)

  // Extension to check if category is attack
  extension (cat: PlanCategory)
    def isAttack: Boolean = cat == PlanCategory.Attack
