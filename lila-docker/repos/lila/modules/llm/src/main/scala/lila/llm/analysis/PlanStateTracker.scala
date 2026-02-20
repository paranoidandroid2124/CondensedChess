package lila.llm.analysis

import lila.llm.model.PlanMatch
import lila.llm.model.strategic.PlanContinuity
import _root_.chess.Color

/**
 * Tracks the longitudinal execution of strategic plans across a game.
 * Used to avoid generic "White plans X" in favor of "White continues their X setup".
 */
case class PlanStateTracker(
  history: Map[Color, Option[PlanContinuity]] = Map(Color.White -> None, Color.Black -> None)
) {

  def update(ply: Int, whitePlans: List[PlanMatch], blackPlans: List[PlanMatch]): PlanStateTracker = {
    PlanStateTracker(Map(
      Color.White -> updateColor(Color.White, ply, whitePlans.headOption.map(_.plan.name)),
      Color.Black -> updateColor(Color.Black, ply, blackPlans.headOption.map(_.plan.name))
    ))
  }

  private def updateColor(color: Color, ply: Int, dominantPlan: Option[String]): Option[PlanContinuity] = {
    val previous = history.getOrElse(color, None)

    (previous, dominantPlan) match {
      case (Some(prev), Some(current)) if prev.planName == current =>
        // Same plan continuing
        Some(prev.copy(consecutivePlies = prev.consecutivePlies + 1))
      case (_, Some(current)) =>
        // New plan started
        Some(PlanContinuity(current, 1, ply))
      case (Some(prev), None) =>
        // Plan interrupted or finished, but we might want a decay counter instead of instant wipe
        None
      case (None, None) =>
        None
    }
  }

  def getContinuity(color: Color): Option[PlanContinuity] = history.getOrElse(color, None)
}

object PlanStateTracker {
  val empty: PlanStateTracker = PlanStateTracker()
}
