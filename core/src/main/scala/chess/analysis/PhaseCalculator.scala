package chess
package analysis

/**
 * Phase calculator - now delegates to PositionFeatures.materialPhase.phase
 * for consistency. Legacy FEN-based method kept for backward compatibility.
 */
object PhaseCalculator:

  enum GamePhase:
    case Opening, Middlegame, Endgame

  /** Primary method: convert phase string from PositionFeatures */
  def fromPhaseString(phase: String): GamePhase =
    phase match
      case "opening" => GamePhase.Opening
      case "endgame" => GamePhase.Endgame
      case _ => GamePhase.Middlegame

  /** Legacy method for backward compatibility - uses FEN directly */
  def getPhase(fen: String, ply: Int, semanticTags: List[String]): GamePhase =
    // Semantic tag overrides (still useful for edge cases)
    val hasOpeningTag = semanticTags.exists(t => t.contains("opening") || t.contains("theory") || t.contains("book"))
    val hasEndgameTag = semanticTags.exists(t => t.contains("endgame") || t.contains("conversion") || t.contains("fortress"))

    if hasOpeningTag then GamePhase.Opening
    else if hasEndgameTag && ply >= 20 then GamePhase.Endgame
    else
      // Delegate to material-based calculation (consistent with PositionFeatures)
      val phaseScore = computePhaseScore(fen)
      if phaseScore >= 0.90 && ply <= 20 then GamePhase.Opening  // Tightened: 90% material, ply 20
      else if phaseScore <= 0.50 then GamePhase.Endgame           // Aligned with 40/78 = ~0.51
      else GamePhase.Middlegame

  private val W_Q = 4.0
  private val W_R = 2.0
  private val W_B = 1.0
  private val W_N = 1.0
  private val MAX_PHASE_WEIGHT = 24.0

  private def computePhaseScore(fen: String): Double =
    val piecePart = fen.takeWhile(_ != ' ')
    var weight = 0.0
    piecePart.foreach {
      case 'q' | 'Q' => weight += W_Q
      case 'r' | 'R' => weight += W_R
      case 'b' | 'B' => weight += W_B
      case 'n' | 'N' => weight += W_N
      case _ => 
    }
    math.max(0.0, math.min(1.0, weight / MAX_PHASE_WEIGHT))

