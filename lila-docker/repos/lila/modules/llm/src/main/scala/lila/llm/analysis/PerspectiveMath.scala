package lila.llm.analysis

import chess.Color

/**
 * White POV score helpers converted to mover-relative deltas.
 *
 * Contract:
 * - Input scores are always White POV centipawns.
 * - Returned values are mover-relative positive deltas when the mover improves.
 */
object PerspectiveMath:

  /**
   * Loss for the moving side if it chooses `playedWhiteCp` over `bestWhiteCp`.
   * Returns 0 when the played line is equal or better for the mover.
   */
  def cpLossForMover(isWhiteMover: Boolean, bestWhiteCp: Int, playedWhiteCp: Int): Int =
    if isWhiteMover then (bestWhiteCp - playedWhiteCp).max(0)
    else (playedWhiteCp - bestWhiteCp).max(0)

  def cpLossForMover(mover: Color, bestWhiteCp: Int, playedWhiteCp: Int): Int =
    cpLossForMover(mover.white, bestWhiteCp, playedWhiteCp)

  /**
   * Improvement for the mover when comparing defended/main line vs threat line.
   * Positive means defended line is better for the mover.
   */
  def improvementForMover(isWhiteMover: Boolean, defendedWhiteCp: Int, threatWhiteCp: Int): Int =
    if isWhiteMover then defendedWhiteCp - threatWhiteCp
    else threatWhiteCp - defendedWhiteCp

  def improvementForMover(mover: Color, defendedWhiteCp: Int, threatWhiteCp: Int): Int =
    improvementForMover(mover.white, defendedWhiteCp, threatWhiteCp)
