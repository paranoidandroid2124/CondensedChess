package lila.llm.analysis

import _root_.chess.*
import lila.llm.model.*

/**
 * Phase 2 Analysis: Characterizes the nature of the position using L1 features.
 * Now acts as a proper L2 layer, consuming insights from PositionFeatures and Eval.
 */
object PositionCharacterizer:

  /**
   * Characterizes the position using aggregated context.
   * 
   * @param pos     The raw board state.
   * @param features  L1 Strategic Features (e.g. King Safety, Pawn Structure).
   * @param evalCp    Engine Evaluation (centipawns) from white's perspective.
   * @param material  Material Imbalance info.
   */
  def characterize(
    pos: Position,
    features: List[lila.llm.model.strategic.PositionalTag],
    evalCp: Option[Int],
    material: String
  ): PositionNature =
    val tension = calculateTension(pos)
    val fluidity = calculateFluidity(pos.board)
    val kingSafetyPenalty = calculateKingSafetyPenalty(features)
    val isImbalanced = material.exists(c => c.isUpper != c.isLower) // Rough check if pieces are different
    
    // Adjusted Tension: Raw attacks + King Danger
    val adjTension = Math.min(1.0, tension + kingSafetyPenalty)
    
    // Evaluation Logic:
    // If one side is winning easily (>300cp), it's rarely "Chaos". It's "Technical" or "Consolidation".
    // We map this to existing types for now (Static/Dynamic).
    val absCp = evalCp.map(_.abs).getOrElse(0)
    val isDecided = absCp > 400
    
    val natureType = 
      if (isDecided) 
        if (fluidity > 0.5) NatureType.Dynamic else NatureType.Static
      else if (adjTension > 0.6 || (isImbalanced && adjTension > 0.4)) NatureType.Chaos
      else if (adjTension > 0.3 || fluidity > 0.5) NatureType.Dynamic
      else if (fluidity < 0.2 && adjTension < 0.2) NatureType.Static
      else NatureType.Transition

    PositionNature(
      natureType = natureType,
      tension = adjTension,
      stability = calculateStability(adjTension, fluidity, isDecided),
      description = deriveDescription(natureType, adjTension, isDecided)
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
    
  private def calculateKingSafetyPenalty(features: List[lila.llm.model.strategic.PositionalTag]): Double =
    // Boost tension if King is exposed or under attack
    features.find {
      case lila.llm.model.strategic.PositionalTag.MateNet(_) => true
      case lila.llm.model.strategic.PositionalTag.KingStuckCenter(_) => true
      case lila.llm.model.strategic.PositionalTag.WeakBackRank(_) => true
      case _ => false 
    } match
      case Some(_) => 0.3
      case None => 0.0
      
  private def calculateStability(tension: Double, fluidity: Double, isDecided: Boolean): Double =
    if (isDecided) 0.9 // Game is settled, high stability
    else 1.0 - (tension * 0.6 + fluidity * 0.4)

  private def deriveDescription(nt: NatureType, tension: Double, isDecided: Boolean): String = 
    if (isDecided) "A decisive position requiring concrete conversion."
    else nt match
      case NatureType.Static => "A solid, maneuvering position with a fixed structure."
      case NatureType.Dynamic => "A dynamic position with active piece play and open lines."
      case NatureType.Chaos => "A high-tension tactical battlefield."
      case NatureType.Transition => "A fluid position transitioning between structures."
