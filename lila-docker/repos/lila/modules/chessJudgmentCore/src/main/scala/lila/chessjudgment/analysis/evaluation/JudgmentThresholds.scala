package lila.chessjudgment.analysis.evaluation

/**
 * Centralized threshold constants for analysis logic.
 */
object JudgmentThresholds:

  val PLAYABLE_LOSS_WP = 2.5

  val ONLY_MOVE_GAP_WP = 10.0

  val THREAT_RELIABLE_DEPTH = 16

  /** WinPercent difference thresholds */
  val BLUNDER_WP = 20.0
  val INACCURACY_WP = 5.0
  val SIGNIFICANT_THREAT_WP = 2.5
  val MATERIAL_THREAT_WP = 5.0
  val URGENT_THREAT_WP = 15.0
  val IGNORABLE_THREAT_WP = 2.0
  val ONLY_DEFENSE_TOLERANCE_WP = 1.25
