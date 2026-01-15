package lila.llm.analysis

import _root_.chess.*
import _root_.chess.format.Uci
import lila.llm.model.*
import lila.llm.model.Motif.*

/**
 * Converts a PV (Principal Variation) into a sequence of semantic Motifs.
 * Maintains board state to detect move-by-move and position-based patterns.
 */
object MotifTokenizer:

  /**
   * Translates a PV (list of UCI moves) into a list of Motifs.
   */
  def tokenize(initialFen: String, pv: List[String]): List[Motif] =
    _root_.chess.format.Fen.read(_root_.chess.variant.Standard, _root_.chess.format.Fen.Full(initialFen)).map { initialPos =>
      val (_, motifs) = pv.zipWithIndex.foldLeft((initialPos, List.empty[Motif])) {
        case ((pos, acc), (uciStr, index)) =>
          Uci(uciStr).collect { case m: Uci.Move => m }.flatMap(pos.move(_).toOption) match
            case Some(mv) =>
              val moveMotifs = detectMoveMotifs(mv, pos, index)
              val stateMotifs = detectStateMotifs(mv.after, index)
              (mv.after, acc ++ moveMotifs ++ stateMotifs)
            case None => 
              (pos, acc)
      }
      motifs
    }.getOrElse(Nil)

  def tokenize(moves: List[Uci], pos: Position): List[Motif] =
    val (_, motifs) = moves.zipWithIndex.foldLeft((pos, List.empty[Motif])) {
      case ((p, acc), (uci, index)) =>
        uci match
          case m: Uci.Move =>
            p.move(m).toOption match
              case Some(mv) =>
                val moveMotifs = detectMoveMotifs(mv, p, index)
                val stateMotifs = detectStateMotifs(mv.after, index)
                (mv.after, acc ++ moveMotifs ++ stateMotifs)
              case None => (p, acc)
          case _ => (p, acc)
    }
    motifs

  // ============================================================
  // MOVE-BASED MOTIF DETECTION
  // ============================================================

  private def detectMoveMotifs(mv: Move, pos: Position, plyIndex: Int): List[Motif] =
    val color = pos.color
    val san = mv.toSanStr.toString
    val nextPos = mv.after

    List.concat(
      detectPawnMotifs(mv, pos, color, san, plyIndex),
      detectPieceMotifs(mv, pos, color, san, plyIndex),
      detectKingMotifs(mv, pos, color, san, plyIndex),
      detectTacticalMotifs(mv, pos, nextPos, color, san, plyIndex)
    )

  // ============================================================
  // PAWN MOTIFS
  // ============================================================

  private def detectPawnMotifs(mv: Move, pos: Position, color: Color, san: String, plyIndex: Int): List[Motif] =
    if (mv.piece.role != Pawn) return Nil
    
    var motifs = List.empty[Motif]

    // Pawn Advance (non-capture)
    if (!mv.captures) {
      motifs = motifs :+ PawnAdvance(
        file = mv.dest.file,
        fromRank = mv.orig.rank.value + 1,
        toRank = mv.dest.rank.value + 1,
        color = color,
        plyIndex = plyIndex,
        move = Some(san)
      )
    }

    // Pawn Break (capture that opens/challenges structure)
    if (mv.captures) {
      motifs = motifs :+ PawnBreak(
        file = mv.orig.file,
        targetFile = mv.dest.file,
        color = color,
        plyIndex = plyIndex,
        move = Some(san)
      )
    }

    // Pawn Promotion
    mv.promotion.foreach { promotedTo =>
      motifs = motifs :+ PawnPromotion(
        file = mv.dest.file,
        promotedTo = promotedTo,
        color = color,
        plyIndex = plyIndex,
        move = Some(san)
      )
    }

    motifs

  // ============================================================
  // PIECE MOTIFS
  // ============================================================

  private def detectPieceMotifs(mv: Move, pos: Position, color: Color, san: String, plyIndex: Int): List[Motif] =
    var motifs = List.empty[Motif]
    val role = mv.piece.role

    // Rook Lift (rook moves from back rank forward)
    if (role == Rook) {
      val backRankValue = if (color.white) 0 else 7
      if (mv.orig.rank.value == backRankValue && mv.dest.rank.value != backRankValue) {
        motifs = motifs :+ RookLift(
          file = mv.dest.file,
          fromRank = mv.orig.rank.value + 1,
          toRank = mv.dest.rank.value + 1,
          color = color,
          plyIndex = plyIndex,
          move = Some(san)
        )
      }
    }

    // Fianchetto
    if (role == Bishop) {
      val fianchettoSquares = if (color.white) List(Square.G2, Square.B2) else List(Square.G7, Square.B7)
      if (fianchettoSquares.contains(mv.dest)) {
        val side = if (mv.dest.file == _root_.chess.File.G) FianchettoSide.Kingside else FianchettoSide.Queenside
        motifs = motifs :+ Fianchetto(side, color, plyIndex, Some(san))
      }
    }

    // Centralization
    val centralSquares = List(Square.D4, Square.D5, Square.E4, Square.E5)
    if ((role == Knight || role == Bishop) && centralSquares.contains(mv.dest)) {
      motifs = motifs :+ Centralization(role, mv.dest, color, plyIndex, Some(san))
    }

    // Outpost
    if (role == Knight) {
      val isInEnemyTerritory = if (color.white) mv.dest.rank.value >= 4 else mv.dest.rank.value <= 3
      if (isInEnemyTerritory) {
        motifs = motifs :+ Outpost(role, mv.dest, color, plyIndex, Some(san))
      }
    }

    // Piece Lift
    if (role == Knight || role == Bishop) {
      val relFrom = Motif.relativeRank(mv.orig.rank.value + 1, color)
      val relTo = Motif.relativeRank(mv.dest.rank.value + 1, color)
      if (relTo > relFrom + 1) {
        motifs = motifs :+ PieceLift(role, mv.orig.rank.value + 1, mv.dest.rank.value + 1, color, plyIndex, Some(san))
      }
    }

    motifs

  // ============================================================
  // KING MOTIFS
  // ============================================================

  private def detectKingMotifs(mv: Move, pos: Position, color: Color, san: String, plyIndex: Int): List[Motif] =
    if (mv.piece.role != King) return Nil
    var motifs = List.empty[Motif]
    if (mv.castle.isDefined) {
      val side = if (mv.dest.file == _root_.chess.File.G) CastlingSide.Kingside else CastlingSide.Queenside
      motifs = motifs :+ Castling(side, color, plyIndex, Some(san))
    } else {
      val stepType = determineKingStepType(mv, color)
      motifs = motifs :+ KingStep(stepType, color, plyIndex, Some(san))
    }
    motifs

  private def determineKingStepType(mv: Move, color: Color): KingStepType =
    val relFrom = Motif.relativeRank(mv.orig.rank.value + 1, color)
    val relTo = Motif.relativeRank(mv.dest.rank.value + 1, color)
    if (relFrom == 1 && relTo > 1) KingStepType.OffBackRank
    else if (relTo >= 4) KingStepType.Activation
    else if (mv.dest.file == _root_.chess.File.A || mv.dest.file == _root_.chess.File.H) KingStepType.ToCorner
    else KingStepType.Other

  // ============================================================
  // TACTICAL MOTIFS
  // ============================================================

  private def detectTacticalMotifs(
      mv: Move, 
      pos: Position, 
      nextPos: Position,
      color: Color, 
      san: String, 
      plyIndex: Int
  ): List[Motif] =
    var motifs = List.empty[Motif]
    if (nextPos.check.yes) {
      val checkType = determineCheckType(mv, pos, nextPos)
      motifs = motifs :+ Motif.Check(mv.piece.role, mv.dest, checkType, color, plyIndex, Some(san))
    }
    if (mv.captures) {
      val capturedRole = pos.board.roleAt(mv.dest).getOrElse(Pawn)
      val captureType = determineCaptureType(mv.piece.role, capturedRole)
      motifs = motifs :+ Capture(mv.piece.role, capturedRole, mv.dest, captureType, color, plyIndex, Some(san))
      detectRemoveDefender(mv, pos, nextPos, color, san, plyIndex).foreach { rd =>
        motifs = motifs :+ rd
      }
    }
    val forkTargetsList = detectForkTargets(mv, nextPos, color)
    if (forkTargetsList.size >= 2) {
      motifs = motifs :+ Fork(mv.piece.role, forkTargetsList, mv.dest, Nil, color, plyIndex, Some(san))
    }
    if (nextPos.check.yes) {
      detectDoubleCheck(mv, pos, nextPos, color, san, plyIndex).foreach { dc =>
        motifs = motifs :+ dc
      }
    }
    detectBackRankMate(mv, pos, nextPos, color, san, plyIndex).foreach { brm =>
      motifs = motifs :+ brm
    }
    detectTrappedPiece(mv, nextPos, color, san, plyIndex).foreach { tp =>
      motifs = motifs :+ tp
    }
    motifs

  private def determineCheckType(mv: Move, pos: Position, nextPos: Position): CheckType =
    val isDouble = detectDoubleCheck(mv, pos, nextPos, pos.color, "", 0).isDefined
    if (isDouble) CheckType.Double
    else CheckType.Normal

  private def detectDoubleCheck(
      mv: Move, 
      pos: Position, 
      nextPos: Position,
      color: Color,
      san: String,
      plyIndex: Int
  ): Option[Motif] =
    if (!nextPos.check.yes) return None
    val oppColor = !color
    nextPos.board.kingPosOf(oppColor).flatMap { kingPos =>
      val attackers = nextPos.board.pieceMap.filter { case (s, p) =>
        p.color == color && canAttackSquare(p.role, s, kingPos, nextPos.board, color)
      }
      if (attackers.size >= 2) {
        val revealedRole = attackers.find((s, _) => s != mv.dest).map((_, p) => p.role).getOrElse(mv.piece.role)
        Some(DoubleCheck(mv.piece.role, revealedRole, color, plyIndex, Some(san)))
      } else None
    }

  private def detectBackRankMate(
      mv: Move, 
      pos: Position, 
      nextPos: Position, 
      color: Color, 
      san: String, 
      plyIndex: Int
  ): Option[Motif] =
    if (!nextPos.check.yes || nextPos.legalMoves.nonEmpty) return None
    val oppColor = !color
    val oppBackRank = if (oppColor.white) _root_.chess.Rank.First else _root_.chess.Rank.Eighth
    nextPos.board.kingPosOf(oppColor).flatMap { kingPos =>
      if (kingPos.rank == oppBackRank) {
        val attackingPieceRole = mv.piece.role
        Some(BackRankMate(BackRankMateType.Execution, attackingPieceRole, color, plyIndex, Some(san)))
      } else None
    }

  private def determineCaptureType(attacker: Role, victim: Role): CaptureType =
    val val1 = pieceValue(attacker)
    val val2 = pieceValue(victim)
    if (val2 < val1) CaptureType.Sacrifice
    else if (val2 == val1) CaptureType.Exchange
    else CaptureType.Normal

  private def detectTrappedPiece(mv: Move, nextPos: Position, color: Color, san: String, plyIndex: Int): Option[Motif] =
    val oppColor = !color
    nextPos.board.pieceMap.find { case (sq, piece) =>
      piece.color == oppColor && piece.role != King && piece.role != Pawn && isTrapped(sq, piece.role, oppColor, nextPos)
    }.map { case (sq, piece) =>
      TrappedPiece(piece.role, sq, oppColor, plyIndex, Some(san))
    }

  private def isTrapped(sq: Square, role: Role, color: Color, pos: Position): Boolean =
    val moves = getPossibleMoveSquares(sq, role)
    val safeMoves = moves.filter { target =>
      !pos.board.isOccupied(target) && !isSquareAttacked(target, !color, pos.board)
    }
    safeMoves.isEmpty && isSquareAttacked(sq, !color, pos.board)

  private def detectRemoveDefender(mv: Move, pos: Position, nextPos: Position, color: Color, san: String, plyIndex: Int): Option[Motif] =
    if (!mv.captures) return None
    val victimSq = mv.dest
    val victimPiece = pos.board.pieceAt(victimSq).get
    pos.board.pieceMap.find { case (protectedSq, protectedPiece) =>
      protectedPiece.color != color && 
      protectedSq != victimSq &&
      wasDefendedBy(protectedSq, victimSq, victimPiece.role, pos.board, !color) &&
      isNowHanging(protectedSq, nextPos)
    }.map { case (sq, piece) =>
      RemovingTheDefender(mv.piece.role, victimPiece.role, piece.role, sq, color, plyIndex, Some(san))
    }

  private def isNowHanging(sq: Square, pos: Position): Boolean =
    pos.board.colorAt(sq).flatMap { color =>
      val attackers = pos.board.attackers(sq, !color)
      val defenders = pos.board.attackers(sq, color)
      if (attackers.nonEmpty && defenders.isEmpty) Some(true) else None
    }.getOrElse(false)

  private def detectForkTargets(mv: Move, nextPos: Position, color: Color): List[Role] =
    val sq = mv.dest
    val role = mv.piece.role
    val targets: Bitboard = role match
      case Pawn   => sq.pawnAttacks(color)
      case Knight => sq.knightAttacks
      case Bishop => sq.bishopAttacks(nextPos.board.occupied)
      case Rook   => sq.rookAttacks(nextPos.board.occupied)
      case Queen  => sq.queenAttacks(nextPos.board.occupied)
      case King   => sq.kingAttacks
    (targets & nextPos.board.byColor(!color)).squares.flatMap(nextPos.board.roleAt)

  private def detectPin(mv: Move, nextPos: Position, color: Color, san: String, plyIndex: Int): List[Motif] = Nil
  private def detectSkewer(mv: Move, nextPos: Position, color: Color, san: String, plyIndex: Int): List[Motif] = Nil

  // ============================================================
  // STATE-BASED MOTIF DETECTION
  // ============================================================

  private def detectStateMotifs(pos: Position, plyIndex: Int): List[Motif] =
    val board = pos.board
    List.concat(
      detectPawnStructure(board, plyIndex),
      detectDoubledPieces(board, plyIndex),
      detectBattery(board, plyIndex),
      detectOpposition(board, plyIndex),
      detectStalemateThreat(pos, plyIndex)
    )

  private def detectPawnStructure(board: Board, plyIndex: Int): List[Motif] =
    var motifs = List.empty[Motif]
    for (color <- List(White, Black)) {
      val pawns = board.byPiece(color, Pawn)
      val oppPawns = board.byPiece(!color, Pawn)
      val pawnsByFile = pawns.squares.groupBy(_.file)
      val oppPawnsByFile = oppPawns.squares.groupBy(_.file)
      pawns.foreach { pawnSq =>
        if (isIsolated(pawnSq, pawnsByFile)) motifs = motifs :+ IsolatedPawn(pawnSq.file, pawnSq.rank.value + 1, color, plyIndex)
        if (isPassed(pawnSq, color, oppPawnsByFile)) motifs = motifs :+ PassedPawn(pawnSq.file, pawnSq.rank.value + 1, color, isProtectedPawn(pawnSq, color, board), plyIndex)
        if (isBackward(pawnSq, color, pawnsByFile, oppPawnsByFile)) motifs = motifs :+ BackwardPawn(pawnSq.file, pawnSq.rank.value + 1, color, plyIndex)
      }
    }
    motifs

  private def isProtectedPawn(sq: Square, color: Color, board: Board): Boolean =
    board.attackers(sq, color).exists(board.roleAt(_).contains(Pawn))

  private def isIsolated(pawnSq: Square, pawnsByFile: Map[_root_.chess.File, List[Square]]): Boolean =
    val fileValue = pawnSq.file.value
    val adjacentFiles = List(fileValue - 1, fileValue + 1).filter(f => f >= 0 && f <= 7)
    !adjacentFiles.exists { adjFileIdx => 
      _root_.chess.File.all.lift(adjFileIdx).exists(pawnsByFile.contains)
    }

  private def isPassed(pawnSq: Square, color: Color, oppPawnsByFile: Map[_root_.chess.File, List[Square]]): Boolean =
    val fileValue = pawnSq.file.value
    val filesToCheck = List(fileValue - 1, fileValue, fileValue + 1).filter(f => f >= 0 && f <= 7)
    filesToCheck.forall { fIdx =>
      _root_.chess.File.all.lift(fIdx).forall { f =>
        oppPawnsByFile.get(f).forall { oppPawns =>
          oppPawns.forall { oppPawn =>
            if (color.white) oppPawn.rank.value <= pawnSq.rank.value
            else oppPawn.rank.value >= pawnSq.rank.value
          }
        }
      }
    }

  private def isBackward(
      pawnSq: Square, 
      color: Color, 
      pawnsByFile: Map[_root_.chess.File, List[Square]], 
      oppPawnsByFile: Map[_root_.chess.File, List[Square]]
  ): Boolean =
    val rank = pawnSq.rank.value
    val fileValue = pawnSq.file.value
    val adjacentFiles = List(fileValue - 1, fileValue + 1).filter(f => f >= 0 && f <= 7)
    val canBeSupportedLater = adjacentFiles.exists { adjFileIdx =>
      _root_.chess.File.all.lift(adjFileIdx).exists { adjFile =>
        pawnsByFile.get(adjFile).exists(_.exists { supportSq =>
          if (color.white) supportSq.rank.value <= rank
          else supportSq.rank.value >= rank
        })
      }
    }
    !canBeSupportedLater

  private def detectDoubledPieces(board: Board, plyIndex: Int): List[Motif] = Nil
  private def detectBattery(board: Board, plyIndex: Int): List[Motif] = Nil

  private def isOnSameDiagonal(sq1: Square, sq2: Square): Boolean =
    val fileDiff = (sq1.file.value - sq2.file.value).abs
    val rankDiff = (sq1.rank.value - sq2.rank.value).abs
    fileDiff == rankDiff && fileDiff > 0

  private def isCloserToEnemy(sq1: Square, sq2: Square, color: Color): Boolean =
    if (color.white) sq1.rank.value > sq2.rank.value else sq1.rank.value < sq2.rank.value

  private def detectOpposition(board: Board, plyIndex: Int): List[Motif] =
    (board.kingPosOf(White), board.kingPosOf(Black)) match
      case (Some(wk), Some(bk)) =>
        val fDiff = (wk.file.value - bk.file.value).abs
        val rDiff = (wk.rank.value - bk.rank.value).abs
        if (fDiff == 0 && rDiff == 2) List(Opposition(bk, wk, OppositionType.Direct, White, plyIndex))
        else if (rDiff == 0 && fDiff == 2) List(Opposition(bk, wk, OppositionType.Direct, White, plyIndex))
        else Nil
      case _ => Nil

  private def detectStalemateThreat(pos: Position, plyIndex: Int): List[Motif] =
    if (pos.legalMoves.size <= 2 && !pos.check.yes) List(StalemateThreat(pos.color, plyIndex)) else Nil

  // ============================================================
  // HELPERS
  // ============================================================

  private def offsetSquare(sq: Square, df: Int, dr: Int): Option[Square] =
    Square.at(sq.file.value + df, sq.rank.value + dr)

  private def isSquareAttacked(sq: Square, color: Color, board: Board): Boolean =
    board.attackers(sq, color).nonEmpty

  private def canAttackSquare(role: Role, attackerSq: Square, targetSq: Square, board: Board, color: Color): Boolean =
    val targets: Bitboard = role match
      case Pawn   => attackerSq.pawnAttacks(color)
      case Knight => attackerSq.knightAttacks
      case Bishop => attackerSq.bishopAttacks(board.occupied)
      case Rook   => attackerSq.rookAttacks(board.occupied)
      case Queen  => attackerSq.queenAttacks(board.occupied)
      case King   => attackerSq.kingAttacks
    targets.contains(targetSq)

  private def getPossibleMoveSquares(sq: Square, role: Role): List[Square] =
    val offsets = role match
      case Queen  => List((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1))
      case Rook   => List((1, 0), (-1, 0), (0, 1), (0, -1))
      case Bishop => List((1, 1), (1, -1), (-1, 1), (-1, -1))
      case Knight => List((2, 1), (2, -1), (-2, 1), (-2, -1), (1, 2), (1, -2), (-1, 2), (-1, -2))
      case King   => List((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1))
      case _      => Nil
    offsets.flatMap(d => offsetSquare(sq, d._1, d._2))

  private def pieceValue(role: Role): Int = role match
    case Pawn   => 1
    case Knight => 3
    case Bishop => 3
    case Rook   => 5
    case Queen  => 9
    case King   => 100

  private def wasDefendedBy(targetSq: Square, defenderSq: Square, defenderRole: Role, board: Board, color: Color): Boolean =
    canAttackSquare(defenderRole, defenderSq, targetSq, board, color)
