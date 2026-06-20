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

  def winPercentFromWhiteEval(whiteCp: Int, mate: Option[Int]): Double =
    mate match
      case Some(m) if m > 0 => 100.0
      case Some(m) if m < 0 => 0.0
      case _                => winPercentFromWhiteCp(whiteCp)

  def winPercentFromRelativeCp(relativeCp: Int): Double =
    winPercentFromWhiteCp(relativeCp)

  def winPercentFromRelativeEval(relativeCp: Int, mate: Option[Int]): Double =
    mate match
      case Some(m) if m > 0 => 100.0
      case Some(m) if m < 0 => 0.0
      case _                => winPercentFromRelativeCp(relativeCp)

  def winPercentForMover(mover: Color, whiteCp: Int): Double =
    val whiteWinPercent = winPercentFromWhiteCp(whiteCp)
    if mover.white then whiteWinPercent else 100.0 - whiteWinPercent

  def winPercentForMover(mover: Color, whiteCp: Int, mate: Option[Int]): Double =
    val whiteWinPercent = winPercentFromWhiteEval(whiteCp, mate)
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

  def winPercentLossForMover(
      mover: Color,
      bestWhiteCp: Int,
      bestMate: Option[Int],
      playedWhiteCp: Int,
      playedMate: Option[Int]
  ): Double =
    (winPercentForMover(mover, bestWhiteCp, bestMate) - winPercentForMover(mover, playedWhiteCp, playedMate)).max(0.0)

  def winPercentLossFromRelativeCp(bestRelativeCp: Int, candidateRelativeCp: Int): Double =
    (winPercentFromRelativeCp(bestRelativeCp) - winPercentFromRelativeCp(candidateRelativeCp)).max(0.0)

  def winPercentLossFromRelativeEval(
      bestRelativeCp: Int,
      bestMate: Option[Int],
      candidateRelativeCp: Int,
      candidateMate: Option[Int]
  ): Double =
    (winPercentFromRelativeEval(bestRelativeCp, bestMate) -
      winPercentFromRelativeEval(candidateRelativeCp, candidateMate)).max(0.0)

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

  def winPercentImprovementForMover(
      mover: Color,
      defendedWhiteCp: Int,
      defendedMate: Option[Int],
      threatWhiteCp: Int,
      threatMate: Option[Int]
  ): Double =
    winPercentForMover(mover, defendedWhiteCp, defendedMate) - winPercentForMover(mover, threatWhiteCp, threatMate)
