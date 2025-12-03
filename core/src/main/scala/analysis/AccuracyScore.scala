package chess
package analysis

/** Accuracy Score calculator.
  * Formula based on Chess.com's CAPS (Computer Aggregated Precision Score).
  * Converts average centipawn loss to 0-100 scale.
  */
object AccuracyScore:

  /** Calculate accuracy score (0-100) for a player based on their moves.
    * Formula: accuracy = 103.1668 * e^(-0.04354 * ACPL) - 3.1669
    * Reference: https://support.chess.com/article/2965-how-is-accuracy-calculated
    *
    * @param moves moves timeline filtered for one player
    * @return accuracy score (0-100)
    */
  def calculate(moves: Vector[AnalyzePgn.PlyOutput]): Double =
    if moves.isEmpty then return 0.0
    
    // Calculate average centipawn loss
    val totalCpLoss = moves.map { move =>
      // Use epLoss (expected pawn loss) as proxy for centipawn loss
      // epLoss is in win% delta, convert to centipawn equivalent
      // Rough conversion: 1% win% ≈ 25cp in the middlegame
      val cpLoss = move.epLoss * 25.0
      math.max(0.0, cpLoss) // Only count losses, not gains
    }.sum
    
    val acpl = totalCpLoss / moves.length.toDouble
    
    // Adjusted accuracy formula for more realistic distribution
    // High scores (95+) only for near-perfect play (ACPL < 5)
    // Average amateur games (ACPL 20-40) score 70-85
    val C = 30.0   // Floor value
    val k = 0.015  // Steepness (tunable)
    val rawAccuracy = C + (100.0 - C) * math.exp(-k * acpl)
    
    // Clamp to [0, 100]
    math.max(0.0, math.min(100.0, rawAccuracy))

  /** Calculate accuracy for both White and Black.
    * @param timeline full game timeline
    * @return (whiteAccuracy, blackAccuracy)
    */
  def calculateBothSides(timeline: Vector[AnalyzePgn.PlyOutput]): (Double, Double) =
    val whiteMoves = timeline.filter(_.turn == Color.White)
    val blackMoves = timeline.filter(_.turn == Color.Black)
    (calculate(whiteMoves), calculate(blackMoves))
