package chess
package analysis

import chess.opening.OpeningDb
import chess.format.Fen
import chess.{ Board, Color, File, Rank, Square, Role }
// import chess.bitboard.Bitboard // Removed: Value not member
// Situation removed from import (Value not member)
// If Bitboard import fails, try import chess.Bitboard or rely on package scope
// Revert Bitboard import if it caused "value not member" - trying local scope first
// But wait, Replay.scala didn't import Bitboard explicitly. 
// I'll comment out explicit Bitboard import and trust package chess. 
// If it fails with "Not found: value Bitboard", then I'll find the right path.
// import chess.bitboard.Bitboard
import java.lang.Math.abs

/** 경량 피처 추출기: 엔진 없이 보드 상태만으로 계산 가능한 구조/기물 기반 지표들. */
object FeatureExtractor:

  // --- Legacy Support (Phase 2 Compatible) ---
  final case class SideFeatures(
      pawnIslands: Int,
      isolatedPawns: Int,
      doubledPawns: Int,
      passedPawns: Int,
      rookOpenFiles: Int,
      rookSemiOpenFiles: Int,
      bishopPair: Boolean,
      kingRingPressure: Int,
      spaceControl: Int
  )

  final case class Snapshot(
      ply: Ply,
      sideToMove: Color,
      white: SideFeatures,
      black: SideFeatures,
      oppositeColorBishops: Boolean,
      openingAtPly: Option[chess.opening.Opening.AtPly]
  )

  def snapshot(position: Position, ply: Ply, sans: Iterable[format.pgn.SanStr]): Snapshot =
    val whiteFeatures = sideFeatures(position, Color.White)
    val blackFeatures = sideFeatures(position, Color.Black)
    Snapshot(
      ply = ply,
      sideToMove = position.color,
      white = whiteFeatures,
      black = blackFeatures,
      oppositeColorBishops = hasOppositeColorBishops(position.board),
      openingAtPly = OpeningDb.search(sans)
    )

  /** @deprecated Use toSideFeatures(PositionFeatures, Color, Position) instead */
  def sideFeatures(position: Position, color: Color): SideFeatures =
    val board = position.board
    val pawns = board.pawns & board.byColor(color)
    val oppPawns = board.pawns & board.byColor(!color)
    val pawnCountsByFile = countsByFile(pawns)
    val islands = pawnIslands(pawnCountsByFile)
    val isolated = isolatedPawns(pawnCountsByFile)
    val doubled = doubledPawns(pawnCountsByFile)
    val passed = passedPawns(color, pawns, oppPawns)
    val (openFiles, semiOpenFiles) = openAndSemiOpenFiles(pawns, oppPawns)
    val bishopPair = board.count(color, Bishop) >= 2
    val kingRingPressure = kingRingThreats(position, color)
    val spaceControl = spaceScore(position.board, color)
    SideFeatures(
      pawnIslands = islands,
      isolatedPawns = isolated,
      doubledPawns = doubled,
      passedPawns = passed,
      rookOpenFiles = openFiles,
      rookSemiOpenFiles = semiOpenFiles,
      bishopPair = bishopPair,
      kingRingPressure = kingRingPressure,
      spaceControl = spaceControl
    )

  /** Derives SideFeatures from PositionFeatures - use this to avoid duplicate computation */
  def toSideFeatures(pf: PositionFeatures, side: Color, position: Position): SideFeatures =
    val isWhite = side == Color.White
    val pawnCounts = if isWhite then pf.pawns.whitePawnCountByFile else pf.pawns.blackPawnCountByFile
    val islands = countIslandsFromList(pawnCounts)
    SideFeatures(
      pawnIslands = islands,
      isolatedPawns = if isWhite then pf.pawns.whiteIsolatedPawns else pf.pawns.blackIsolatedPawns,
      doubledPawns = if isWhite then pf.pawns.whiteDoubledPawns else pf.pawns.blackDoubledPawns,
      passedPawns = if isWhite then pf.pawns.whitePassedPawns else pf.pawns.blackPassedPawns,
      rookOpenFiles = if isWhite then pf.activity.whiteRooksOnOpenFiles else pf.activity.blackRooksOnOpenFiles,
      rookSemiOpenFiles = if isWhite then pf.activity.whiteRooksOnSemiOpenFiles else pf.activity.blackRooksOnSemiOpenFiles,
      bishopPair = position.board.count(side, Bishop) >= 2,
      kingRingPressure = if isWhite then pf.kingSafety.whiteKingRingEnemyPieces else pf.kingSafety.blackKingRingEnemyPieces,
      spaceControl = if isWhite then pf.space.whiteCentralSpace else pf.space.blackCentralSpace
    )

  private def countIslandsFromList(pawnCounts: List[Int]): Int =
    if pawnCounts.isEmpty then 0
    else
      var islands = 0
      var inIsland = false
      for count <- pawnCounts do
        if count > 0 then
          if !inIsland then
            islands += 1
            inIsland = true
        else
          inIsland = false
      islands

  // --- Phase 4.1: The "Eye" (New High-Dimensional Features) ---

  final case class PawnStructureFeatures(
    whitePawnCount: Int,
    blackPawnCount: Int,
    whitePawnCountByFile: List[Int],
    blackPawnCountByFile: List[Int],
    whiteIsolatedPawns: Int,
    blackIsolatedPawns: Int,
    whiteDoubledPawns: Int,
    blackDoubledPawns: Int,
    whiteBackwardPawns: Int,
    blackBackwardPawns: Int,
    whitePassedPawns: Int,
    blackPassedPawns: Int,
    whiteAdvancedPassedPawns: Int,
    blackAdvancedPassedPawns: Int,
    whiteIQP: Boolean,
    blackIQP: Boolean,
    whiteHangingPawns: Boolean,
    blackHangingPawns: Boolean,
    whiteMinorityAttackReady: Boolean,
    blackMinorityAttackReady: Boolean,
    whiteCentralPawns: Int,
    blackCentralPawns: Int,
    whiteCentralFixed: Boolean,
    blackCentralFixed: Boolean
  )

  final case class ActivityFeatures(
    whiteLegalMoves: Int,
    blackLegalMoves: Int,
    whiteMinorPieceMobility: Int,
    blackMinorPieceMobility: Int,
    whiteRookMobility: Int,
    blackRookMobility: Int,
    whiteQueenMobility: Int,
    blackQueenMobility: Int,
    whiteCenterControl: Int,
    blackCenterControl: Int,
    whiteRooksOnOpenFiles: Int,
    blackRooksOnOpenFiles: Int,
    whiteRooksOnSemiOpenFiles: Int,
    blackRooksOnSemiOpenFiles: Int,
    whiteKnightOutposts: Int,
    blackKnightOutposts: Int
    // badBishopCount and bishopPair removed - duplicates SideFeatures/ConceptScorer
  )

  final case class KingSafetyFeatures(
    whiteCastlingState: String, // "none" | "kingside" | "queenside" | "can_castle_both" | "can_castle_short" | "can_castle_long"
    blackCastlingState: String,
    whiteKingShieldPawns: Int,
    blackKingShieldPawns: Int,
    whiteKingExposedFiles: Int,
    blackKingExposedFiles: Int,
    whiteBackRankWeak: Boolean,
    blackBackRankWeak: Boolean,
    whiteKingCenterDistance: Double,
    blackKingCenterDistance: Double,
    whiteKingRingEnemyPieces: Int,
    blackKingRingEnemyPieces: Int
  )

  final case class MaterialPhaseFeatures(
    whiteMaterial: Int,
    blackMaterial: Int,
    materialDiff: Int,
    whiteMajorPieces: Int,
    blackMajorPieces: Int,
    whiteMinorPieces: Int,
    blackMinorPieces: Int,
    phase: String // "opening" | "middlegame" | "endgame"
  )

  final case class DevelopmentFeatures(
    whiteUndevelopedMinors: Int,
    blackUndevelopedMinors: Int,
    whiteUndevelopedRooks: Int,
    blackUndevelopedRooks: Int,
    whiteRooksConnected: Boolean,
    blackRooksConnected: Boolean
  )

  final case class SpaceFeatures(
    whiteCentralSpace: Int,
    blackCentralSpace: Int,
    whiteEnemyCampPresence: Int,
    blackEnemyCampPresence: Int,
    whiteRestrictivePawns: Int,
    blackRestrictivePawns: Int
  )

  final case class CoordinationFeatures(
    whiteBatteryAligned: Boolean,
    blackBatteryAligned: Boolean,
    whiteRookOn7th: Boolean,
    blackRookOn7th: Boolean,
    whiteRooksBehindPassedPawns: Int,
    blackRooksBehindPassedPawns: Int
  )

  // GeometryFeatures removed - was unused (Replaced with GeometricFacts for Narrative)

  final case class GeometricFacts(
    whiteKingSquare: String,
    blackKingSquare: String,
    whiteOpenFilesNearKing: List[String],
    blackOpenFilesNearKing: List[String],
    whiteKingAttackers: List[String], // Squares of attackers
    blackKingAttackers: List[String],
    whiteAvailableChecks: List[String], // SAN or UCI
    blackAvailableChecks: List[String] 
  )

  final case class TacticalFeatures(
    whiteHangingPieces: Int,
    blackHangingPieces: Int,
    whiteEnPrise: Int,
    blackEnPrise: Int,
    whiteLoosePieces: Int,
    blackLoosePieces: Int
  )

  final case class PositionFeatures(
    fen: String,
    sideToMove: String, // "white" | "black"
    plyCount: Int,
    pawns: PawnStructureFeatures,
    activity: ActivityFeatures,
    kingSafety: KingSafetyFeatures,
    materialPhase: MaterialPhaseFeatures,
    development: DevelopmentFeatures,
    space: SpaceFeatures,
    coordination: CoordinationFeatures,
    tactics: TacticalFeatures,
    geometry: GeometricFacts
  )

  def extractPositionFeatures(fen: String, plyCount: Int): PositionFeatures =
    val positionOrNone = Fen.read(chess.variant.Standard, fen.asInstanceOf[chess.format.Fen.Full])
    val position = positionOrNone.getOrElse(sys.error("Invalid FEN: " + fen))
    val board = position.board

    PositionFeatures(
      fen = fen,
      sideToMove = position.color.name,
      plyCount = plyCount,
      pawns = computePawnStructure(board),
      activity = computeActivity(position),
      kingSafety = computeKingSafety(board, position),
      materialPhase = computeMaterialPhase(board),
      development = computeDevelopment(board),
      space = computeSpace(board),
      coordination = computeCoordination(board),
      tactics = computeTactics(board),
      geometry = computeGeometricFacts(position)
    )

  private def computeGeometricFacts(position: Position): GeometricFacts =
    val board = position.board
    
    // King Squares
    val whiteKing = board.kingPosOf(Color.White).map(_.key).getOrElse("?")
    val blackKing = board.kingPosOf(Color.Black).map(_.key).getOrElse("?")

    // Open Files Near King
    def openFilesNear(color: Color): List[String] =
      board.kingPosOf(color).map { kSq =>
         List(kSq.file.value - 1, kSq.file.value, kSq.file.value + 1)
           .filter(i => i >= 0 && i <= 7)
           .flatMap(File.all.lift)
           .filter { f => 
             val pawns = board.pawns & f.bb
             pawns.isEmpty || (pawns & board.byColor(color)).isEmpty
           }
           .map(_.toString)
      }.getOrElse(Nil)

    // King Attackers (squares)
    def attackers(color: Color): List[String] =
       board.kingPosOf(color).fold(List.empty[String]) { kSq =>
         val ring = kSq.kingAttacks | kSq.bb
         ring.squares.flatMap { sq =>
           board.attackers(sq, !color).squares.map(_.key)
         }.distinct
       }

    // Available Checks
    val activeColor = position.color
    // Ensure we handle check generation safely
    val checks = position.legalMoves.filter(m => m.after.check.yes).map(_.toUci.uci).distinct
    
    val (wChecks, bChecks) = 
      if activeColor == Color.White then (checks, List.empty[String])
      else (List.empty[String], checks)

    GeometricFacts(
      whiteKingSquare = whiteKing,
      blackKingSquare = blackKing,
      whiteOpenFilesNearKing = openFilesNear(Color.White),
      blackOpenFilesNearKing = openFilesNear(Color.Black),
      whiteKingAttackers = attackers(Color.White),
      blackKingAttackers = attackers(Color.Black),
      whiteAvailableChecks = wChecks,
      blackAvailableChecks = bChecks
    )

  // --- Internal Calculation Logic ---

  private def computePawnStructure(board: Board): PawnStructureFeatures =
    val wPawns = board.pawns & board.white
    val bPawns = board.pawns & board.black
    val wByFile = countsByFile(wPawns)
    val bByFile = countsByFile(bPawns)

    val wCountsList = (0 to 7).map(f => wByFile.getOrElse(f, 0)).toList
    val bCountsList = (0 to 7).map(f => bByFile.getOrElse(f, 0)).toList

    // Reuse existing logic
    val wIso = isolatedPawns(wByFile)
    val bIso = isolatedPawns(bByFile)
    val wDbl = doubledPawns(wByFile)
    val bDbl = doubledPawns(bByFile)
    val wPassed = passedPawns(Color.White, wPawns, bPawns)
    val bPassed = passedPawns(Color.Black, bPawns, wPawns)
    
    // Advanced passed pawns (Rank 5-7 for White, 2-4 for Black)
    val wAdvancedPassed = advancedPassedPawns(Color.White, wPawns, bPawns)
    val bAdvancedPassed = advancedPassedPawns(Color.Black, bPawns, wPawns)

    // New logic
    // Backward (simplified: pawn on rank 2/3 (white) blocked or controlled, no support from friendly pawns)
    // For now, heuristic: generic backward detection to be refined
    val wBackward = backwardPawns(Color.White, wPawns, bPawns)
    val bBackward = backwardPawns(Color.Black, bPawns, wPawns)

    // IQP
    val wIQP = wByFile.getOrElse(3, 0) > 0 && wByFile.getOrElse(2, 0) == 0 && wByFile.getOrElse(4, 0) == 0
    val bIQP = bByFile.getOrElse(3, 0) > 0 && bByFile.getOrElse(2, 0) == 0 && bByFile.getOrElse(4, 0) == 0

    // Hanging Pawns (c & d pawns, no b & e pawns)
    def isHanging(byFile: Map[Int, Int]) = 
      byFile.getOrElse(2, 0) > 0 && byFile.getOrElse(3, 0) > 0 && 
      byFile.getOrElse(1, 0) == 0 && byFile.getOrElse(4, 0) == 0
    
    val wHanging = isHanging(wByFile)
    val bHanging = isHanging(bByFile)

    // Minority Attack Ready (White a,b vs Black a,b,c or similar)
    // Simplified trigger: less pawns on queen side than opponent
    val wQueenSide = (0 to 2).map(wByFile.getOrElse(_, 0)).sum
    val bQueenSide = (0 to 2).map(bByFile.getOrElse(_, 0)).sum
    val wMinority = wQueenSide > 0 && wQueenSide < bQueenSide
    val bMinority = bQueenSide > 0 && bQueenSide < wQueenSide

    // Central Pawns (d, e files)
    val wCentral = wByFile.getOrElse(3, 0) + wByFile.getOrElse(4, 0)
    val bCentral = bByFile.getOrElse(3, 0) + bByFile.getOrElse(4, 0)
    
    // Central Fixed (e.g. d4 blocked by d5)
    def checkFixed(file: File): Boolean = 
      val wP = (wPawns & file.bb).squares.headOption
      val bP = (bPawns & file.bb).squares.headOption
      (wP, bP) match
        case (Some(w), Some(b)) => (w.rank.value + 1 == b.rank.value)
        case _ => false
    
    val wFixed = checkFixed(File.all(3)) || checkFixed(File.all(4)) // Check only d/e files for now
    val bFixed = wFixed

    PawnStructureFeatures(
      whitePawnCount = wPawns.count,
      blackPawnCount = bPawns.count,
      whitePawnCountByFile = wCountsList,
      blackPawnCountByFile = bCountsList,
      whiteIsolatedPawns = wIso,
      blackIsolatedPawns = bIso,
      whiteDoubledPawns = wDbl,
      blackDoubledPawns = bDbl,
      whiteBackwardPawns = wBackward,
      blackBackwardPawns = bBackward,
      whitePassedPawns = wPassed,
      blackPassedPawns = bPassed,
      whiteAdvancedPassedPawns = wAdvancedPassed,
      blackAdvancedPassedPawns = bAdvancedPassed,
      whiteIQP = wIQP,
      blackIQP = bIQP,
      whiteHangingPawns = wHanging,
      blackHangingPawns = bHanging,
      whiteMinorityAttackReady = wMinority,
      blackMinorityAttackReady = bMinority,
      whiteCentralPawns = wCentral,
      blackCentralPawns = bCentral,
      whiteCentralFixed = wFixed,
      blackCentralFixed = bFixed
    )

  private def computeActivity(position: Position): ActivityFeatures =
    val board = position.board
    // Use Position.color to determine active side? Or just calculate features.
    // We want features specifically for White and Black regardless of turn.
    // To get legal moves for a specific side, we might need to assume it's their turn?
    // Use board.color(Color) approach? No, Position has color.
    // For "mobility", we usually mean valid moves.
    // If we want white moves, we need a Position where it is White's turn.
    
    val wPos = if position.color == Color.White then position else position.withColor(Color.White)
    val bPos = if position.color == Color.Black then position else position.withColor(Color.Black)
    
    val wMoves = wPos.legalMoves
    val bMoves = bPos.legalMoves
    
    // legalMoves is List[Move].
    // We need to group by from-square to count mobility per piece.
    val wMoveMap = wMoves.groupBy(_.orig)
    val bMoveMap = bMoves.groupBy(_.orig)
    
    def countMobility(moves: Map[Square, List[Move]], roles: Set[Role]): Int =
      moves.values.flatten.count(m => roles.contains(m.piece.role))

    val wMinors = countMobility(wMoveMap, Set(Knight, Bishop))
    val bMinors = countMobility(bMoveMap, Set(Knight, Bishop))
    val wRooks = countMobility(wMoveMap, Set(Rook))
    val bRooks = countMobility(bMoveMap, Set(Rook))
    val wQueens = countMobility(wMoveMap, Set(Queen))
    val bQueens = countMobility(bMoveMap, Set(Queen))

    // Center Control (d4, e4, d5, e5)
    val dFile = File.all(3)
    val eFile = File.all(4)
    val center = (dFile.bb & Rank.Fourth.bb) | (eFile.bb & Rank.Fourth.bb) | 
                 (dFile.bb & Rank.Fifth.bb) | (eFile.bb & Rank.Fifth.bb)
    
    def countControl(color: Color) = 
      center.squares.count(sq => board.attackers(sq, color).nonEmpty)

    val wCenter = countControl(White)
    val bCenter = countControl(Black)

    // Rooks on Open/Semi lines
    def rookFileStats(color: Color, myPawns: Bitboard, oppPawns: Bitboard): (Int, Int) =
      val rooks = board.rooks & board.byColor(color)
      rooks.squares.foldLeft((0,0)) { case ((open, semi), sq) =>
        val f = sq.file
        val myP = (myPawns & f.bb).nonEmpty
        val oppP = (oppPawns & f.bb).nonEmpty
        if !myP && !oppP then (open + 1, semi)
        else if !myP && oppP then (open, semi + 1)
        else (open, semi)
      }

    val wPawns = board.pawns & board.white
    val bPawns = board.pawns & board.black
    val (wROpen, wRSemi) = rookFileStats(Color.White, wPawns, bPawns)
    val (bROpen, bRSemi) = rookFileStats(Color.Black, bPawns, wPawns)

    // Outposts (Knight on protected square, no enemy pawn can attack it)
    def countOutposts(color: Color) =
      val knights = board.knights & board.byColor(color)
      val myPawns = board.pawns & board.byColor(color)
      val oppPawns = board.pawns & board.byColor(!color)
      knights.squares.count { sq =>
        val protectedByPawn = board.attackers(sq, color).intersects(myPawns)
        val validRank = if color == Color.White then sq.rank >= Rank.Fourth else sq.rank <= Rank.Fifth
        val attackedByPawn = board.attackers(sq, !color).intersects(oppPawns)
        protectedByPawn && validRank && !attackedByPawn
      }

    val wOutposts = countOutposts(Color.White)
    val bOutposts = countOutposts(Color.Black)

    ActivityFeatures(
      whiteLegalMoves = wMoves.size,
      blackLegalMoves = bMoves.size,
      whiteMinorPieceMobility = wMinors,
      blackMinorPieceMobility = bMinors,
      whiteRookMobility = wRooks,
      blackRookMobility = bRooks,
      whiteQueenMobility = wQueens,
      blackQueenMobility = bQueens,
      whiteCenterControl = wCenter,
      blackCenterControl = bCenter,
      whiteRooksOnOpenFiles = wROpen,
      blackRooksOnOpenFiles = bROpen,
      whiteRooksOnSemiOpenFiles = wRSemi,
      blackRooksOnSemiOpenFiles = bRSemi,
      whiteKnightOutposts = wOutposts,
      blackKnightOutposts = bOutposts
    )

  private def computeKingSafety(board: Board, position: Position): KingSafetyFeatures =
    // Castling State
    // Position.history.castles has rights
    // We want the current rights from the position (FEN)
    val castles = position.history.castles
    def castleState(color: Color): String =
      val k = castles.can(color, Side.KingSide)
      val q = castles.can(color, Side.QueenSide)
      if k && q then "can_castle_both"
      else if k then "can_castle_short"
      else if q then "can_castle_long"
      else "none"

    // King Shield
    def kingShield(color: Color) =
       board.kingPosOf(color).map { kSq =>
         val shield = kSq.kingAttacks & board.pawns & board.byColor(color)
         shield.count
       }.getOrElse(0)

    def kingExposed(color: Color) =
       board.kingPosOf(color).map { kSq =>
         // file of king, file-1, file+1
         val files = List(Some(kSq.file), File.all.lift(kSq.file.value - 1), File.all.lift(kSq.file.value + 1)).flatten
         files.count { f => 
           // Check if open or semi-open
           val pawns = board.pawns & f.bb
           pawns.isEmpty || (pawns & board.byColor(color)).isEmpty
         }
       }.getOrElse(0)

    // Back Rank Weakness
    def backRankWeak(color: Color) =
       val kSq = board.kingPosOf(color)
       val rank = if color == Color.White then Rank.First else Rank.Eighth
       kSq.exists(_.rank == rank) && kingShield(color) == 0 // Very rough

    // Center Distance
    def distCenter(color: Color) =
      board.kingPosOf(color).map { kSq =>
        abs(kSq.file.value - 3.5) + abs(kSq.rank.value - 3.5)
      }.getOrElse(0.0)

    // Enemy Pieces in King Ring
    def ringAttackers(color: Color) =
      kingRingThreats(position.withColor(color), color) // re-use but ensure color matches? kingRingThreats takes Position

    KingSafetyFeatures(
      whiteCastlingState = castleState(Color.White),
      blackCastlingState = castleState(Color.Black),
      whiteKingShieldPawns = kingShield(Color.White),
      blackKingShieldPawns = kingShield(Color.Black),
      whiteKingExposedFiles = kingExposed(Color.White),
      blackKingExposedFiles = kingExposed(Color.Black),
      whiteBackRankWeak = backRankWeak(Color.White),
      blackBackRankWeak = backRankWeak(Color.Black),
      whiteKingCenterDistance = distCenter(Color.White),
      blackKingCenterDistance = distCenter(Color.Black),
      whiteKingRingEnemyPieces = ringAttackers(Color.White),
      blackKingRingEnemyPieces = ringAttackers(Color.Black)
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
    // Starting material = 78 (2*Q=18, 4*R=20, 4*B=12, 4*N=12, 16*P=16)
    // Phase thresholds based on material remaining:
    // - Opening: >= 70 (early game, most pieces on board)
    // - Endgame: <= 40 (~50% gone) OR no major pieces with few minors
    // - Middlegame: everything else
    val isEndgameByPieces = wMaj + bMaj == 0 && wMin + bMin <= 2
    val phase = 
      if isEndgameByPieces || totalMat <= 40 then "endgame"
      else if totalMat >= 70 then "opening"
      else "middlegame"

    MaterialPhaseFeatures(
      whiteMaterial = wMat,
      blackMaterial = bMat,
      materialDiff = wMat - bMat,
      whiteMajorPieces = wMaj,
      blackMajorPieces = bMaj,
      whiteMinorPieces = wMin,
      blackMinorPieces = bMin,
      phase = phase
    )

  private def backwardPawns(color: Color, pawns: Bitboard, oppPawns: Bitboard): Int =
    val dir = if color == Color.White then 1 else -1
    pawns.squares.count { sq =>
      val f = sq.file
      val r = sq.rank
      val neighbors = (pawns & (File.all.lift(f.value - 1).map(_.bb).getOrElse(Bitboard.empty) | 
                                File.all.lift(f.value + 1).map(_.bb).getOrElse(Bitboard.empty)))
      
      // 1. No friendly pawn on adjacent files is "behind" or same rank (supporting candidate)
      // Supporting means rank <= sq.rank (for White)
      val supported = neighbors.squares.exists { nSig =>
        if color == Color.White then nSig.rank.value <= r.value 
        else nSig.rank.value >= r.value
      }

      // 2. Stop square (sq + dir) is controlled by enemy pawn
      // Stop square attacks: checks if any enemy pawn attacks the square in front of me
      val stopRank = Rank.all.lift(r.value + dir).getOrElse(r)
      // val stopSq = Square.at(f.value, stopRank.value)
      // If error says expected Int, maybe I should check import or Square object.
      // Assuming Square.at(File, Rank) returns Option[Square] or Square.
      // The previous error was "Found: File, Required: Int". 
      // This STRICTLY suggests `Square(x, y)` style apply, NOT `Square.at`. 
      // But let's try `Square(f, stopRank)` if `apply` is the way.
      // Wait, `Square.at` is standard in newer scalachess for Option.
      // But maybe I should use `Square(f, stopRank)` which might be apply? 
      // Or `Square(f.value + (stopRank.value * 8))`? No.
      // I'll try `Square(f, stopRank)` assuming apply exists.
      // Actually, safest is simple bit manipulation if I knew internal index `f.value + (r.value+dir)*8`
      // But let's try `Square.at(f.value, stopRank.value)`
      val stopSqOpt = Square.at(f.value, stopRank.value) // Explicit Ints if strictly required
      
      val stopControlled = stopSqOpt.exists { s => 
        // Attackers from opposite color pawns
        // oppPawns attacks s?
        // Pawns attack "forward-diagonal". 
        // If I am White, stop square is r+1. Enemy (Black) at r+2 attacks r+1.
        // Bitboard.pawnAttacks(!color, s) gives squares that ATTACK s? 
        // No, `pawnAttacks(color, sq)` gives squares that `color` at `sq` attacks.
        // We need: is `s` attacked by `oppPawns`?
        // `board.attackers(s, !color)` includes pawns.
        // Doing raw bitboard check to be safe/fast:
        // Attackers to 's' from Black are at s.rank+1, file+/-1
        // Attackers to 's' from White are at s.rank-1, file+/-1
        // We have `oppPawns`.
        // We want to check if any OPPONENT pawn attacks 's'.
        // Opponent pawns that attack 's' are located at squares that 's' would attack if 's' was a pawn of COLOR.
        // e.g. White StopSquare d4. Black pawns at c5/e5 attack d4.
        // d4.pawnAttacks(White) -> {c5, e5}.
        // So we check intersection with oppPawns.
        val attackers = s.pawnAttacks(color) 
        (attackers & oppPawns).nonEmpty
      }

      !supported && stopControlled
    }

  // --- Legacy helpers ---
  // (Preserved from original file)
  private def countsByFile(pawns: Bitboard): Map[Int, Int] =
    pawns.squares.groupBy(_.file.value).view.mapValues(_.size).toMap

  private def pawnIslands(countsByFile: Map[Int, Int]): Int =
    val files = countsByFile.keys.toList.sorted
    files.foldLeft((0, -2)) { case ((islands, prev), file) =>
      val newIslands = if file != prev + 1 then islands + 1 else islands
      newIslands -> file
    }._1

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

  private def advancedPassedPawns(color: Color, pawns: Bitboard, oppPawns: Bitboard): Int =
    val oppSquares = oppPawns.squares
    pawns.squares.count { pawn =>
      val pawnFile = pawn.file.value
      val pawnRank = pawn.rank.value
      
      // Is advanced?
      val isAdvanced = 
        if color == Color.White then pawnRank >= Rank.Fifth.value
        else pawnRank <= Rank.Fourth.value
      
      if !isAdvanced then false
      else {
        val blocking = oppSquares.exists { opp =>
          val fileOk = (opp.file.value - pawnFile).abs <= 1
          val ahead =
            if color == Color.White then opp.rank.value > pawnRank
            else opp.rank.value < pawnRank
          fileOk && ahead
        }
        !blocking
      }
    }

  private def openAndSemiOpenFiles(pawns: Bitboard, oppPawns: Bitboard): (Int, Int) =
    val friendly = pawns
    val enemy = oppPawns
    File.all.foldLeft((0, 0)) { case ((open, semiOpen), file) =>
      val mask = file.bb
      val friendlyOnFile = friendly.intersects(mask)
      val enemyOnFile = enemy.intersects(mask)
      if !friendlyOnFile && !enemyOnFile then (open + 1, semiOpen)
      else if !friendlyOnFile && enemyOnFile then (open, semiOpen + 1)
      else (open, semiOpen)
    }

  private def kingRingThreats(position: Position, color: Color): Int =
    position.kingPosOf(color).fold(0) { kingSq =>
      val ring = kingSq.kingAttacks | kingSq.bb
      val attackers =
        ring.fold(Bitboard.empty)((acc, sq) => acc | position.attackers(sq, !color))
      attackers.count
    }

  private def spaceScore(board: Board, color: Color): Int =
    val attacked = attackedSquares(board, color)
    attacked.squares.count { sq =>
      if color == Color.White then sq.rank.value >= Rank.Fourth.value
      else sq.rank.value <= Rank.Fifth.value
    }

  private def attackedSquares(board: Board, color: Color): Bitboard =
    Square.all.foldLeft(Bitboard.empty) { (acc, sq) =>
      if board.attackers(sq, color).nonEmpty then acc | sq.bb else acc
    }

  private def computeDevelopment(board: Board): DevelopmentFeatures =
    def undevelopedMinors(color: Color): Int =
      val backRank = if color == Color.White then Rank.First else Rank.Eighth
      val knights = board.knights & board.byColor(color) & backRank.bb
      val bishops = board.bishops & board.byColor(color) & backRank.bb
      // Standard starting squares? For now, simply back rank is a strong heuristic
      knights.count + bishops.count

    def undevelopedRooks(color: Color): Int =
      val backRank = if color == Color.White then Rank.First else Rank.Eighth
      val rooks = board.rooks & board.byColor(color) & backRank.bb
      // Specifically corners? No, let's just count rooks on back rank
      rooks.count

    def rooksConnected(color: Color): Boolean =
      val rooks = board.rooks & board.byColor(color)
      if rooks.count < 2 then false
      else
        // Check if they are on same rank or file and no pieces between
        // Simplified: same rank/file and no obstruction
        // It's expensive to do raycasting here.
        // Heuristic: if on same rank/file and friendly piece count between is 0
        // Actually, "Rooks Connected" usually means they protect each other.
        // board.attackers(sq, color) contains other rook?
        rooks.squares.exists { r1 =>
          val attackers = board.attackers(r1, color)
          attackers.intersects(rooks)
        }

    DevelopmentFeatures(
      whiteUndevelopedMinors = undevelopedMinors(Color.White),
      blackUndevelopedMinors = undevelopedMinors(Color.Black),
      whiteUndevelopedRooks = undevelopedRooks(Color.White),
      blackUndevelopedRooks = undevelopedRooks(Color.Black),
      whiteRooksConnected = rooksConnected(Color.White),
      blackRooksConnected = rooksConnected(Color.Black)
    )

  private def computeSpace(board: Board): SpaceFeatures =
    // Central Space: pawns controlling c3-f6 box
    // c3, d3, e3, f3, c4, d4, e4, f4 ...
    
    // Enemy Camp Presence
    def enemyCamp(color: Color): Int =
      val myPieces = board.piecesOf(color).keys
      val enemyRanks = if color == Color.White then 5 to 8 else 1 to 4
      myPieces.count(sq => enemyRanks.contains(sq.rank.value + 1)) // rank value is 0-indexed

    // Restrictive Pawns (rank 5/6)
    def restrictive(color: Color): Int =
      val pawns = board.pawns & board.byColor(color)
      pawns.squares.count { p =>
        if color == Color.White then p.rank >= Rank.Fifth
        else p.rank <= Rank.Fourth
      }

    // Central control by pawns
    def centralControl(color: Color): Int =
      // Squares c3 to f6
      // Let's sample key squares
      val keySquares = List(
        "c3", "d3", "e3", "f3",
        "c4", "d4", "e4", "f4",
        "c5", "d5", "e5", "f5",
        "c6", "d6", "e6", "f6"
      ).flatMap(Square.fromKey)
      
      keySquares.count { sq =>
         // Controlled by pawns of color?
         // Check if `sq` is attacked by pawns of `color`.
         // `board.attackers(sq, color)` includes pawns.
         // We need ONLY pawns.
         val attackers = board.attackers(sq, color)
         attackers.intersects(board.pawns)
      }

    SpaceFeatures(
      whiteCentralSpace = centralControl(Color.White),
      blackCentralSpace = centralControl(Color.Black),
      whiteEnemyCampPresence = enemyCamp(Color.White),
      blackEnemyCampPresence = enemyCamp(Color.Black),
      whiteRestrictivePawns = restrictive(Color.White),
      blackRestrictivePawns = restrictive(Color.Black)
    )

  private def computeCoordination(board: Board): CoordinationFeatures =
    def battery(color: Color): Boolean =
       // Queen + Bishop on diagonal, Queen + Rook on file/rank
       // Simplified: Is there any structure where Q and B/R share a line?
       // Hard without raycasting.
       // Heuristic: Q and R on same file/rank, Q and B on same diagonal.
       val queens = board.queens & board.byColor(color)
       if queens.isEmpty then false
       else
         val q = queens.squares.head 
         // Check Rook on same file/rank
         val rooks = board.rooks & board.byColor(color)
         val bishops = board.bishops & board.byColor(color)
         
         val rAlign = rooks.squares.exists(r => r.file == q.file || r.rank == q.rank)
         val bAlign = bishops.squares.exists(b => b.onSameDiagonal(q))
         rAlign || bAlign

    def rookOn7th(color: Color): Boolean =
       val rank = if color == Color.White then Rank.Seventh else Rank.Second
       val rooks = board.rooks & board.byColor(color)
       rooks.squares.exists(_.rank == rank)

    CoordinationFeatures(
      whiteBatteryAligned = battery(Color.White),
      blackBatteryAligned = battery(Color.Black),
      whiteRookOn7th = rookOn7th(Color.White),
      blackRookOn7th = rookOn7th(Color.Black),
      whiteRooksBehindPassedPawns = computeRooksBehind(board, Color.White),
      blackRooksBehindPassedPawns = computeRooksBehind(board, Color.Black)
    )

  // Requires specialized passed pawn detection logic if existing helper takes too many args.
  // Re-implementing simplified "is passed" check here for efficiency or reusing helper?
  // We have `passedPawns` helper but it returns count. 
  // Let's implement `countRooksBehind` using `passedPawns` approach.
  private def computeRooksBehind(board: Board, color: Color): Int =
     val pawns = board.pawns & board.byColor(color)
     val oppPawns = board.pawns & board.byColor(!color)
     val rooks = board.rooks & board.byColor(color)
     
     // 1. Identify Passed Pawns
     // Copied logic from passedPawns helper but returning squares
     val passedSquares = pawns.squares.filter { pawn =>
        val pawnFile = pawn.file.value
        val pawnRank = pawn.rank.value
        // Blocking?
        val blocking = oppPawns.squares.exists { opp =>
          val fileOk = (opp.file.value - pawnFile).abs <= 1
          val ahead = if color == Color.White then opp.rank.value > pawnRank else opp.rank.value < pawnRank
          fileOk && ahead
        }
        !blocking
     }

     // 2. Check if Rook is behind
     // Behind = Same file, usually "behind" means rank < pawnRank (for White pawn moving up)
     // Tarrasch Rule: "Rook belongs behind passed pawns". 
     // For White passed pawn at e5, White Rook at e1 is good.
     // For Black passed pawn at e4, Black Rook at e8 is good.
     passedSquares.count { p =>
        val f = p.file
        // Friendly rooks on same file
        val fileRooks = rooks & f.bb
        fileRooks.squares.exists { r =>
           if color == Color.White then r.rank.value < p.rank.value
           else r.rank.value > p.rank.value
        }
     }

  private def computeTactics(board: Board): TacticalFeatures =
    def analyze(color: Color): (Int, Int, Int) =
      // Loose: 0 defenders
      // Hanging: Attacked > Defended? No, typically "Attacked but not defended". 
      //          Or "Attacked value > Defended value" (SEE).
      // En Prise: Attacked by lower value.
      
      var loose = 0
      var hanging = 0
      var enPrise = 0
      
      val myPieces = board.piecesOf(color)
      
      myPieces.foreach { (sq, piece) =>
        // sq is Square, piece is Piece
        // attackers returns Bitboard
        val attackers = board.attackers(sq, !color)
        val defenders = board.attackers(sq, color)
        
        if defenders.isEmpty then
          loose += 1
          if attackers.nonEmpty then hanging += 1
        else
          // Defended, but could be en prise (attacked by lower value)
          val lowestAttackerValue = attackers.squares.map(a => 
             board.pieceAt(a).map(p => pieceValue(p.role)).getOrElse(99)
          ).minOption.getOrElse(99)
          
          if lowestAttackerValue < pieceValue(piece.role) then
             enPrise += 1
      }
      
      (hanging, enPrise, loose)
      
      (hanging, enPrise, loose)

    val (wH, wEp, wL) = analyze(Color.White)
    val (bH, bEp, bL) = analyze(Color.Black)
    
    TacticalFeatures(
      whiteHangingPieces = wH,
      blackHangingPieces = bH,
      whiteEnPrise = wEp,
      blackEnPrise = bEp,
      whiteLoosePieces = wL,
      blackLoosePieces = bL
    )
    
  private def pieceValue(role: Role): Int = role match
    case Pawn => 1
    case Knight => 3
    case Bishop => 3
    case Rook => 5
    case Queen => 9
    case King => 100

  def hasOppositeColorBishops(board: Board): Boolean =
    val whiteBishops = (board.bishops & board.white).squares
    val blackBishops = (board.bishops & board.black).squares
    if whiteBishops.size == 1 && blackBishops.size == 1 then
      whiteBishops.head.isLight != blackBishops.head.isLight
    else false


