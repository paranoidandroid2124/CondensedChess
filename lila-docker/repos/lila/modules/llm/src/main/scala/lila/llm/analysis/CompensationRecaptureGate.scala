package lila.llm.analysis

import chess.{ Bishop, Board, Color, Knight, Pawn, Queen, Rook, Role }
import chess.format.{ Fen, Uci }

private[llm] object CompensationRecaptureGate:

  private val MaterialToleranceCp = 75

  /**
   * Immediate parity-restoring captures should not be promoted into
   * durable after-move compensation.
   *
   * We do not have the previous ply's SAN/UCI here, so the gate uses the
   * current board plus the played move:
   * - the move must be a capture
   * - the mover must currently be down material by roughly the value of the captured piece
   * - the capture must restore near-parity for the mover
   */
  def suppressAfterCompensation(
      fenBefore: String,
      playedMoveUci: String,
      investedMaterial: Int
  ): Boolean =
    if investedMaterial <= 0 then false
    else
      Fen.read(chess.variant.Standard, Fen.Full(fenBefore))
        .flatMap { pos =>
          Uci(playedMoveUci)
            .collect { case m: Uci.Move => m }
            .flatMap(pos.move(_).toOption)
            .map { move =>
              if !move.captures then false
              else
                val capturedRole =
                  move.capture
                    .flatMap(pos.board.roleAt)
                    .orElse(pos.board.roleAt(move.dest))

                capturedRole.exists { role =>
                  val capturedValue = pieceValueCp(role)
                  val mover = pos.color
                  val beforeDeficit = sideMaterialDeficitCp(pos.board, mover)
                  val afterDeficit = sideMaterialDeficitCp(move.after.board, mover)
                  val recovered = beforeDeficit - afterDeficit

                  beforeDeficit >= capturedValue - MaterialToleranceCp &&
                  afterDeficit <= MaterialToleranceCp &&
                  recovered >= capturedValue - MaterialToleranceCp
                }
            }
        }
        .getOrElse(false)

  private def sideMaterialDeficitCp(board: Board, side: Color): Int =
    val diff = materialDiffCp(board)
    if side.white then math.max(0, -diff) else math.max(0, diff)

  private def materialDiffCp(board: Board): Int =
    materialCp(board, Color.White) - materialCp(board, Color.Black)

  private def materialCp(board: Board, side: Color): Int =
    board.byPiece(side, Pawn).count * 100 +
      board.byPiece(side, Knight).count * 300 +
      board.byPiece(side, Bishop).count * 300 +
      board.byPiece(side, Rook).count * 500 +
      board.byPiece(side, Queen).count * 900

  private def pieceValueCp(role: Role): Int = role match
    case Pawn   => 100
    case Knight => 300
    case Bishop => 300
    case Rook   => 500
    case Queen  => 900
    case _      => 0
