package lila.llm.analysis

import _root_.chess.*
import lila.llm.model.*

case class PositionNature(
    natureType: NatureType,
    tension: Double,   // 0.0 to 1.0 (0.0 = dead draw/dry, 1.0 = chaos)
    stability: Double, // 0.0 to 1.0 (0.0 = wild swings, 1.0 = solid)
    description: String
)

enum NatureType:
  case Static, Dynamic, Transition, Chaos

object PositionCharacterizer:

  def characterize(pos: Position): PositionNature =
    val tension = calculateTension(pos)
    val fluidity = calculateFluidity(pos.board)
    
    val natureType = 
      if (tension > 0.7) NatureType.Chaos
      else if (tension > 0.4 || fluidity > 0.6) NatureType.Dynamic
      else if (fluidity < 0.3) NatureType.Static
      else NatureType.Transition

    PositionNature(
      natureType = natureType,
      tension = tension,
      stability = 1.0 - (tension * 0.5), // Purely heuristic for now
      description = deriveDescription(natureType, tension)
    )

  /**
   * Tension is based on the number of active attacks between pieces of opposite colors.
   */
  private def calculateTension(pos: Position): Double =
    val board = pos.board
    val occupied = board.occupied
    
    var attackCount = 0
    board.foreach { (color, role, sq) =>
      val targets: Bitboard = role match
        case Pawn   => sq.pawnAttacks(color)
        case Knight => sq.knightAttacks
        case Bishop => sq.bishopAttacks(occupied)
        case Rook   => sq.rookAttacks(occupied)
        case Queen  => sq.queenAttacks(occupied)
        case King   => sq.kingAttacks
      
      attackCount += (targets & board.byColor(!color)).count
    }

    // Normalized by number of pieces on board (max tension roughly 1.5 attacks per piece)
    val pieceCount = board.nbPieces
    if (pieceCount == 0) 0.0
    else Math.min(1.0, attackCount.toDouble / (pieceCount.toDouble * 1.5))

  /**
   * Fluidity is based on open pawn structures.
   */
  private def calculateFluidity(board: Board): Double =
    val pawns = board.pawns
    val totalPawns = pawns.count
    if (totalPawns == 0) return 1.0
    
    val centerFiles = List(File.C, File.D, File.E, File.F)
    val openFilesCount = centerFiles.count(f => (pawns & Bitboard.file(f)).isEmpty)
    
    openFilesCount.toDouble / centerFiles.size

  private def deriveDescription(nt: NatureType, tension: Double): String = nt match
    case NatureType.Static => "A solid, maneuvering position with a fixed structure."
    case NatureType.Dynamic => "A dynamic position with active piece play and open lines."
    case NatureType.Chaos => "A high-tension tactical battlefield."
    case NatureType.Transition => "A fluid position transitioning between structures."
