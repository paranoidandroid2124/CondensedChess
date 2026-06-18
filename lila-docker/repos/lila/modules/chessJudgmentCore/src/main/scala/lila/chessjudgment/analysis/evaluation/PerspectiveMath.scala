package lila.chessjudgment.analysis.evaluation

import chess.Color

/**
 * White POV score helpers converted to mover-relative deltas.
 *
 * Contract:
 * - Input scores are always White POV centipawns.
 * - Returned values are mover-relative positive deltas when the mover improves.
 */
object PerspectiveMath:

  private val WinPercentSlope = 0.00368208

  def winPercentFromWhiteCp(whiteCp: Int): Double =
    50.0 + 50.0 * (2.0 / (1.0 + math.exp(-WinPercentSlope * whiteCp.toDouble)) - 1.0)

  def winPercentForMover(mover: Color, whiteCp: Int): Double =
    val whiteWinPercent = winPercentFromWhiteCp(whiteCp)
    if mover.white then whiteWinPercent else 100.0 - whiteWinPercent

  /**
   * Loss for the moving side if it chooses `playedWhiteCp` over `bestWhiteCp`.
   * Returns 0 when the played line is equal or better for the mover.
   */
  def cpLossForMover(isWhiteMover: Boolean, bestWhiteCp: Int, playedWhiteCp: Int): Int =
    if isWhiteMover then (bestWhiteCp - playedWhiteCp).max(0)
    else (playedWhiteCp - bestWhiteCp).max(0)

  def cpLossForMover(mover: Color, bestWhiteCp: Int, playedWhiteCp: Int): Int =
    cpLossForMover(mover.white, bestWhiteCp, playedWhiteCp)

  def winPercentLossForMover(mover: Color, bestWhiteCp: Int, playedWhiteCp: Int): Double =
    (winPercentForMover(mover, bestWhiteCp) - winPercentForMover(mover, playedWhiteCp)).max(0.0)

  /**
   * Improvement for the mover when comparing defended/main line vs threat line.
   * Positive means defended line is better for the mover.
   */
  def improvementForMover(isWhiteMover: Boolean, defendedWhiteCp: Int, threatWhiteCp: Int): Int =
    if isWhiteMover then defendedWhiteCp - threatWhiteCp
    else threatWhiteCp - defendedWhiteCp

  def improvementForMover(mover: Color, defendedWhiteCp: Int, threatWhiteCp: Int): Int =
    improvementForMover(mover.white, defendedWhiteCp, threatWhiteCp)

  def winPercentImprovementForMover(mover: Color, defendedWhiteCp: Int, threatWhiteCp: Int): Double =
    winPercentForMover(mover, defendedWhiteCp) - winPercentForMover(mover, threatWhiteCp)
