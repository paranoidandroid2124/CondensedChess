package chess
package analysis

/** Canonical tag labels used across analysis outputs to avoid alias duplication. */
object TagName:
  val OpeningTheoryBranch = "opening_theory_branch"
  val PlanChange = "plan_change"
  val EndgameTransition = "endgame_transition"
  val ShiftTacticalToPositional = "shift_tactical_to_positional"
  val FortressBuilding = "fortress_building"
  val KingExposed = "king_exposed"
  val ConversionDifficulty = "conversion_difficulty"
  val PositionalSacrifice = "positional_sacrifice"
  val WeakBackRank = "weak_back_rank"
