package chess
package analysis

object PhaseCalculator:

  enum GamePhase:
    case Opening, Middlegame, Endgame

  private val W_Q = 4.0
  private val W_R = 2.0
  private val W_B = 1.0
  private val W_N = 1.0
  
  // Total initial weight = 2*Q + 4*R + 4*B + 4*N = 8 + 8 + 4 + 4 = 24
  private val MAX_PHASE_WEIGHT = 24.0

  def getPhase(fen: String, ply: Int, semanticTags: List[String]): GamePhase =
    // 1. Semantic Overrides
    val hasOpeningTag = semanticTags.exists(t => t.contains("opening") || t.contains("theory") || t.contains("book"))
    val hasEndgameTag = semanticTags.exists(t => t.contains("endgame") || t.contains("conversion") || t.contains("fortress"))

    if hasOpeningTag then GamePhase.Opening
    else if hasEndgameTag && ply >= 20 then GamePhase.Endgame
    else
      // 2. Material Phase Score
      val phaseScore = computePhaseScore(fen)
      
      // 3. Ply Gates + Phase Score
      if phaseScore >= 0.7 && ply <= 30 then GamePhase.Opening
      else if phaseScore <= 0.3 && ply >= 20 then GamePhase.Endgame
      else GamePhase.Middlegame

  def computePhaseScore(fen: String): Double =
    val piecePart = fen.takeWhile(_ != ' ')
    var weight = 0.0
    piecePart.foreach {
      case 'q' | 'Q' => weight += W_Q
      case 'r' | 'R' => weight += W_R
      case 'b' | 'B' => weight += W_B
      case 'n' | 'N' => weight += W_N
      case _ => // Pawns and Kings excluded from phase calculation typically
    }
    // Normalize 0.0 ~ 1.0 (1.0 = Start, 0.0 = Empty)
    math.max(0.0, math.min(1.0, weight / MAX_PHASE_WEIGHT))
