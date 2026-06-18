package lila.chessjudgment.analysis.evaluation

/**
 * Centralized threshold constants for analysis logic.
 */
object JudgmentThresholds:

  val PLAYABLE_LOSS_CP = 25
  
  val BLUNDER_CP = 300
  
  val MISTAKE_CP = 100

  val ONLY_MOVE_GAP_CP = 100
  
  val INACCURACY_CP = 50
  
  val DOUBLE_QUESTION_CP = 200
  
  val SINGLE_QUESTION_CP = 100
  
  val DUBIOUS_CP = 50
  
  /** lossIfIgnoredCp >= SIGNIFICANT_THREAT */
  val SIGNIFICANT_THREAT_CP = 100
  
  /** lossIfIgnoredCp >= URGENT_THREAT → threat requires immediate attention */
  val URGENT_THREAT_CP = 200
  
  /** lossIfIgnoredCp >= MINOR_THREAT → threshold for opponent opportunities */
  val MINOR_THREAT_CP = 50

  /** WinPercent difference thresholds */
  val BLUNDER_WP = 20.0
  val MISTAKE_WP = 10.0
  val INACCURACY_WP = 5.0
  val CATASTROPHIC_WP = 25.0
