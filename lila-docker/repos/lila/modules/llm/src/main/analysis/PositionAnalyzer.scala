package lila.llm

import _root_.chess.*
import _root_.chess.format.Fen
// Note: OpeningDb and Math.abs removed - unused after refactoring

/**
 * Unified Position Analyzer
 * 
 * Combines quantitative feature extraction (from core/FeatureExtractor)
 * with qualitative position characterization (from PositionCharacterizer).
 * 
 * This is the single source of truth for static position analysis.
 */
enum NatureType:
  case Static, Dynamic, Transition, Chaos

case class PositionNature(
    natureType: NatureType,
    tension: Double,   // 0.0 to 1.0 (0.0 = dead draw/dry, 1.0 = chaos)
    stability: Double, // 0.0 to 1.0 (0.0 = wild swings, 1.0 = solid)
    description: String
)

final case class PawnStructureFeatures(
    whitePawnCount: Int,
    blackPawnCount: Int,
    whiteIsolatedPawns: Int,
    blackIsolatedPawns: Int,
    whiteDoubledPawns: Int,
    blackDoubledPawns: Int,
    whitePassedPawns: Int,
    blackPassedPawns: Int,
    whiteIQP: Boolean,
    blackIQP: Boolean,
    whiteHangingPawns: Boolean,
    blackHangingPawns: Boolean,
    whiteCentralPawns: Int,
    blackCentralPawns: Int
)

final case class ActivityFeatures(
    whiteLegalMoves: Int,
    blackLegalMoves: Int,
    whiteMinorPieceMobility: Int,
    blackMinorPieceMobility: Int,
    whiteCenterControl: Int,
    blackCenterControl: Int
)

final case class KingSafetyFeatures(
    whiteCastlingState: String,
    blackCastlingState: String,
    whiteKingShieldPawns: Int,
    blackKingShieldPawns: Int,
    whiteKingExposedFiles: Int,
    blackKingExposedFiles: Int,
    whiteBackRankWeak: Boolean,
    blackBackRankWeak: Boolean
)

final case class MaterialPhaseFeatures(
    whiteMaterial: Int,
    blackMaterial: Int,
    materialDiff: Int,
    phase: String // "opening" | "middlegame" | "endgame"
)

final case class PositionFeatures(
    fen: String,
    sideToMove: String,
    plyCount: Int,
    pawns: PawnStructureFeatures,
    activity: ActivityFeatures,
    kingSafety: KingSafetyFeatures,
    materialPhase: MaterialPhaseFeatures,
    nature: PositionNature
)

/**
 * Unified Position Analyzer
 * 
 * Combines quantitative feature extraction (from core/FeatureExtractor)
 * with qualitative position characterization (from PositionCharacterizer).
 * 
 * This is the single source of truth for static position analysis.
 */
object PositionAnalyzer:

  def characterize(pos: Position): PositionNature =
    val tension = calculateTension(pos)
    val fluidity = calculateFluidity(pos.board)
    
    val natureType = 
      if tension > 0.7 then NatureType.Chaos
      else if tension > 0.4 || fluidity > 0.6 then NatureType.Dynamic
      else if fluidity < 0.3 then NatureType.Static
      else NatureType.Transition

    PositionNature(
      natureType = natureType,
      tension = tension,
      stability = 1.0 - (tension * 0.5),
      description = deriveDescription(natureType, tension)
    )

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
    if pieceCount == 0 then 0.0
    else Math.min(1.0, attackCount.toDouble / (pieceCount.toDouble * 1.5))

  private def calculateFluidity(board: Board): Double =
    val pawns = board.pawns
    val totalPawns = pawns.count
    if totalPawns == 0 then return 1.0
    
    val centerFiles = List(File.C, File.D, File.E, File.F)
    val openFilesCount = centerFiles.count(f => (pawns & Bitboard.file(f)).isEmpty)
    
    openFilesCount.toDouble / centerFiles.size

  private def deriveDescription(nt: NatureType, @annotation.nowarn("msg=unused") tension: Double): String = nt match
    case NatureType.Static => "A solid, maneuvering position with a fixed structure."
    case NatureType.Dynamic => "A dynamic position with active piece play and open lines."
    case NatureType.Chaos => "A high-tension tactical battlefield."
    case NatureType.Transition => "A fluid position transitioning between structures."

  // --- Main Entry Point ---

  def extractFeatures(fen: String, plyCount: Int): Option[PositionFeatures] =
    Fen.read(chess.variant.Standard, Fen.Full(fen)).map { position =>
      val board = position.board
      val nature = characterize(position)

      PositionFeatures(
        fen = fen,
        sideToMove = position.color.name,
        plyCount = plyCount,
        pawns = computePawnStructure(board),
        activity = computeActivity(position),
        kingSafety = computeKingSafety(board, position),
        materialPhase = computeMaterialPhase(board),
        nature = nature
      )
    }

  // --- Internal Calculation Logic ---

  private def computePawnStructure(board: Board): PawnStructureFeatures =
    val wPawns = board.pawns & board.white
    val bPawns = board.pawns & board.black
    val wByFile = countsByFile(wPawns)
    val bByFile = countsByFile(bPawns)

    val wIso = isolatedPawns(wByFile)
    val bIso = isolatedPawns(bByFile)
    val wDbl = doubledPawns(wByFile)
    val bDbl = doubledPawns(bByFile)
    val wPassed = passedPawns(Color.White, wPawns, bPawns)
    val bPassed = passedPawns(Color.Black, bPawns, wPawns)

    // IQP (Isolated Queen Pawn on d-file)
    val wIQP = wByFile.getOrElse(3, 0) > 0 && wByFile.getOrElse(2, 0) == 0 && wByFile.getOrElse(4, 0) == 0
    val bIQP = bByFile.getOrElse(3, 0) > 0 && bByFile.getOrElse(2, 0) == 0 && bByFile.getOrElse(4, 0) == 0

    // Hanging Pawns (c & d pawns, no b & e pawns)
    def isHanging(byFile: Map[Int, Int]) = 
      byFile.getOrElse(2, 0) > 0 && byFile.getOrElse(3, 0) > 0 && 
      byFile.getOrElse(1, 0) == 0 && byFile.getOrElse(4, 0) == 0
    
    val wHanging = isHanging(wByFile)
    val bHanging = isHanging(bByFile)

    // Central Pawns (d, e files)
    val wCentral = wByFile.getOrElse(3, 0) + wByFile.getOrElse(4, 0)
    val bCentral = bByFile.getOrElse(3, 0) + bByFile.getOrElse(4, 0)

    PawnStructureFeatures(
      whitePawnCount = wPawns.count,
      blackPawnCount = bPawns.count,
      whiteIsolatedPawns = wIso,
      blackIsolatedPawns = bIso,
      whiteDoubledPawns = wDbl,
      blackDoubledPawns = bDbl,
      whitePassedPawns = wPassed,
      blackPassedPawns = bPassed,
      whiteIQP = wIQP,
      blackIQP = bIQP,
      whiteHangingPawns = wHanging,
      blackHangingPawns = bHanging,
      whiteCentralPawns = wCentral,
      blackCentralPawns = bCentral
    )

  private def computeActivity(position: Position): ActivityFeatures =
    val board = position.board
    
    val wPos = if position.color == Color.White then position else position.withColor(Color.White)
    val bPos = if position.color == Color.Black then position else position.withColor(Color.Black)
    
    val wMoves = wPos.legalMoves
    val bMoves = bPos.legalMoves
    
    val wMoveMap = wMoves.groupBy(_.orig)
    val bMoveMap = bMoves.groupBy(_.orig)
    
    def countMobility(moves: Map[Square, List[Move]], roles: Set[Role]): Int =
      moves.values.flatten.count(m => roles.contains(m.piece.role))

    val wMinors = countMobility(wMoveMap, Set(Knight, Bishop))
    val bMinors = countMobility(bMoveMap, Set(Knight, Bishop))

    // Center Control (d4, e4, d5, e5)
    val dFile = File.all(3)
    val eFile = File.all(4)
    val center = (dFile.bb & Rank.Fourth.bb) | (eFile.bb & Rank.Fourth.bb) | 
                 (dFile.bb & Rank.Fifth.bb) | (eFile.bb & Rank.Fifth.bb)
    
    def countControl(color: Color) = 
      center.squares.count(sq => board.attackers(sq, color).nonEmpty)

    val wCenter = countControl(White)
    val bCenter = countControl(Black)

    ActivityFeatures(
      whiteLegalMoves = wMoves.size,
      blackLegalMoves = bMoves.size,
      whiteMinorPieceMobility = wMinors,
      blackMinorPieceMobility = bMinors,
      whiteCenterControl = wCenter,
      blackCenterControl = bCenter
    )

  private def computeKingSafety(board: Board, position: Position): KingSafetyFeatures =
    val castles = position.history.castles
    def castleState(color: Color): String =
      val k = castles.can(color, Side.KingSide)
      val q = castles.can(color, Side.QueenSide)
      if k && q then "can_castle_both"
      else if k then "can_castle_short"
      else if q then "can_castle_long"
      else "none"

    def kingShield(color: Color) =
       board.kingPosOf(color).map { kSq =>
         val shield = kSq.kingAttacks & board.pawns & board.byColor(color)
         shield.count
       }.getOrElse(0)

    def kingExposed(color: Color) =
       board.kingPosOf(color).map { kSq =>
         val files = List(Some(kSq.file), File.all.lift(kSq.file.value - 1), File.all.lift(kSq.file.value + 1)).flatten
         files.count { f => 
           val pawns = board.pawns & f.bb
           pawns.isEmpty || (pawns & board.byColor(color)).isEmpty
         }
       }.getOrElse(0)

    def backRankWeak(color: Color) =
       val kSq = board.kingPosOf(color)
       val rank = if color == Color.White then Rank.First else Rank.Eighth
       kSq.exists(_.rank == rank) && kingShield(color) == 0

    KingSafetyFeatures(
      whiteCastlingState = castleState(Color.White),
      blackCastlingState = castleState(Color.Black),
      whiteKingShieldPawns = kingShield(Color.White),
      blackKingShieldPawns = kingShield(Color.Black),
      whiteKingExposedFiles = kingExposed(Color.White),
      blackKingExposedFiles = kingExposed(Color.Black),
      whiteBackRankWeak = backRankWeak(Color.White),
      blackBackRankWeak = backRankWeak(Color.Black)
    )

  private def computeMaterialPhase(board: Board): MaterialPhaseFeatures =
    def mat(color: Color) =
      board.piecesOf(color).map { case (_, cm) =>
         cm.role match
           case Queen => 9
           case Rook => 5
           case Bishop => 3
           case Knight => 3
           case Pawn => 1
           case King => 0
      }.sum

    val wMat = mat(Color.White)
    val bMat = mat(Color.Black)
    
    val wMaj = board.count(Color.White, Queen) + board.count(Color.White, Rook)
    val bMaj = board.count(Color.Black, Queen) + board.count(Color.Black, Rook)
    val wMin = board.count(Color.White, Bishop) + board.count(Color.White, Knight)
    val bMin = board.count(Color.Black, Bishop) + board.count(Color.Black, Knight)

    val totalMat = wMat + bMat
    val isEndgameByPieces = wMaj + bMaj == 0 && wMin + bMin <= 2
    val phase = 
      if isEndgameByPieces || totalMat <= 40 then "endgame"
      else if totalMat >= 70 then "opening"
      else "middlegame"

    MaterialPhaseFeatures(
      whiteMaterial = wMat,
      blackMaterial = bMat,
      materialDiff = wMat - bMat,
      phase = phase
    )

  // --- Helpers ---

  private def countsByFile(pawns: Bitboard): Map[Int, Int] =
    pawns.squares.groupBy(_.file.value).view.mapValues(_.size).toMap

  private def isolatedPawns(countsByFile: Map[Int, Int]): Int =
    countsByFile.foldLeft(0) { case (acc, (file, count)) =>
      val left = countsByFile.getOrElse(file - 1, 0)
      val right = countsByFile.getOrElse(file + 1, 0)
      if count > 0 && left == 0 && right == 0 then acc + count else acc
    }

  private def doubledPawns(countsByFile: Map[Int, Int]): Int =
    countsByFile.values.foldLeft(0) { (acc, c) =>
      if c > 1 then acc + (c - 1) else acc
    }

  private def passedPawns(color: Color, pawns: Bitboard, oppPawns: Bitboard): Int =
    val oppSquares = oppPawns.squares
    pawns.squares.count { pawn =>
      val pawnFile = pawn.file.value
      val pawnRank = pawn.rank.value
      val blocking = oppSquares.exists { opp =>
        val fileOk = (opp.file.value - pawnFile).abs <= 1
        val ahead =
          if color == Color.White then opp.rank.value > pawnRank
          else opp.rank.value < pawnRank
        fileOk && ahead
      }
      !blocking
    }
