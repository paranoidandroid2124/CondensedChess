package lila.llm.analysis

/**
 * Centralized threshold constants for analysis logic.
 * 
 * Single source of truth for:
 * - Severity classification (cpLoss → blunder/mistake/inaccuracy)
 * - Threat significance (lossIfIgnoredCp thresholds)
 * - Move annotation (NAG symbols)
 */
object Thresholds:

  // ============================================================
  // SEVERITY CLASSIFICATION (cpLoss thresholds)
  // ============================================================
  
  /** cpLoss >= BLUNDER → "blunder" severity */
  val BLUNDER_CP = 300
  
  /** cpLoss >= MISTAKE → "mistake" severity */
  val MISTAKE_CP = 100
  
  /** cpLoss >= INACCURACY → "inaccuracy" severity */
  val INACCURACY_CP = 50
  
  // ============================================================
  // NAG ANNOTATION (BookStyleRenderer move marks)
  // ============================================================
  
  /** cpLoss >= DOUBLE_QUESTION → "??" (blunder mark) */
  val DOUBLE_QUESTION_CP = 200
  
  /** cpLoss >= SINGLE_QUESTION → "?" (mistake mark) */
  val SINGLE_QUESTION_CP = 100
  
  /** cpLoss >= DUBIOUS → "?!" (dubious mark) */
  val DUBIOUS_CP = 50

  // ============================================================
  // THREAT SIGNIFICANCE
  // ============================================================
  
  /** lossIfIgnoredCp >= SIGNIFICANT_THREAT → threat is narratively significant */
  val SIGNIFICANT_THREAT_CP = 100
  
  /** lossIfIgnoredCp >= URGENT_THREAT → threat requires immediate attention */
  val URGENT_THREAT_CP = 200

  /** lossIfIgnoredCp >= CRITICAL_THREAT → threat is virtually winning for opponent */
  val CRITICAL_THREAT_CP = 800
  
  /** lossIfIgnoredCp >= MINOR_THREAT → threshold for opponent opportunities */
  val MINOR_THREAT_CP = 50

  /** Difference in CP to consider a defense "adequate" */
  val DEFENSE_TOLERANCE_CP = 50

  /** Minimum depth for analysis to be considered reliable */
  val MIN_RELIABLE_DEPTH = 16

  // ============================================================
  // HELPER METHODS
  // ============================================================
  
  /** Classify severity from cpLoss */
  def classifySeverity(cpLoss: Int): String =
    if cpLoss >= BLUNDER_CP then "blunder"
    else if cpLoss >= MISTAKE_CP then "mistake"
    else if cpLoss >= INACCURACY_CP then "inaccuracy"
    else "ok"
  
  /** Get NAG annotation mark from cpLoss */
  def annotationMark(cpLoss: Int): String =
    if cpLoss >= DOUBLE_QUESTION_CP then "??"
    else if cpLoss >= SINGLE_QUESTION_CP then "?"
    else if cpLoss >= DUBIOUS_CP then "?!"
    else ""
