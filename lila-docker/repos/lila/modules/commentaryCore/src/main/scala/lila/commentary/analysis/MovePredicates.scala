package lila.commentary.analysis

import chess.*
import chess.format.Fen
import lila.commentary.model.authoring.MovePattern

object MovePredicates:

  def isCentralFile(f: File): Boolean =
    f == File.C || f == File.D || f == File.E || f == File.F

  def isCentralFile(c: Char): Boolean =
    c.toLower match
      case 'c' | 'd' | 'e' | 'f' => true
      case _                     => false

  def isTensionReleaseCandidate(pos: Position, uci: String): Boolean =
    chess.format.Uci(uci).collect { case m: chess.format.Uci.Move => m }.flatMap(pos.move(_).toOption) match
      case Some(mv) =>
        val isCapture = mv.captures
        val isPawnCapture = mv.piece.role == Pawn && isCapture
        val isCentralCapture = isCapture && (isCentralFile(mv.orig.file) || isCentralFile(mv.dest.file))
        isPawnCapture || isCentralCapture
      case None => false

  def planClashOptions(afterFen: String): (Option[String], Option[String]) =
    Fen.read(chess.variant.Standard, Fen.Full(afterFen)) match
      case None => (None, None)
      case Some(pos) =>
        val legalMoves = pos.legalMoves.toList

        val pawnCaptures =
          legalMoves.filter(mv => mv.piece.role == Pawn && mv.captures && isCentralFile(mv.dest.file))
        val pawnPushes =
          legalMoves.filter(mv => mv.piece.role == Pawn && !mv.captures && isCentralFile(mv.dest.file))

        val cap =
          pawnCaptures
            .find(_.orig.file == File.D)
            .orElse(pawnCaptures.headOption)
            .map(_.toUci.uci)

        val push =
          pawnPushes
            .find(_.orig.file == File.C)
            .orElse(pawnPushes.headOption)
            .map(_.toUci.uci)

        (cap, push)

  def matchesMovePattern(fen: String, uci: String, pattern: MovePattern): Boolean =
    Fen.read(chess.variant.Standard, Fen.Full(fen)).exists(pos => matchesMovePattern(pos, uci, pattern))

  def matchesMovePattern(pos: Position, uci: String, pattern: MovePattern): Boolean =
    chess.format.Uci(uci).collect { case m: chess.format.Uci.Move => m }.flatMap(pos.move(_).toOption) match
      case None => false
      case Some(mv) =>
        pattern match
          case MovePattern.PawnAdvance(file) =>
            mv.piece.role == Pawn && !mv.captures && mv.dest.file == file
          case MovePattern.PawnLever(from, to) =>
            mv.piece.role == Pawn && mv.captures && mv.orig.file == from && mv.dest.file == to
          case MovePattern.PieceTo(role, square) =>
            mv.piece.role == role && mv.dest == square
          case MovePattern.Castle =>
            mv.castle.isDefined
          case MovePattern.PieceManeuver(role, target, via) =>
            // Check if this move is a step in the maneuver
            // Simplified: verify role and either reaching target or one of the 'via' squares
            mv.piece.role == role && (mv.dest == target || via.contains(mv.dest))
          case MovePattern.Exchange(role, on) =>
            // Must be a capture on the target square by a piece (usually) or pawn
            mv.captures && mv.dest == on
          case MovePattern.BatteryFormation(front, back, line) =>
            val movedRoleOk = mv.piece.role == front || mv.piece.role == back
            movedRoleOk &&
              destinationMatchesLine(mv.dest, line) &&
              batteryPartnerOnLine(mv.after.board, mv.piece.color, mv.dest, movedPartnerRole(mv.piece.role, front, back), line)

  private def movedPartnerRole(moved: Role, front: Role, back: Role): Role =
    if moved == front then back else front

  private def destinationMatchesLine(
      destination: Square,
      line: lila.commentary.model.authoring.LineType
  ): Boolean =
    line match
      case lila.commentary.model.authoring.LineType.FileLine(file) => destination.file == file
      case lila.commentary.model.authoring.LineType.RankLine(rank) => destination.rank == rank
      case lila.commentary.model.authoring.LineType.DiagonalLine   => true

  private def batteryPartnerOnLine(
      board: Board,
      color: Color,
      destination: Square,
      partnerRole: Role,
      line: lila.commentary.model.authoring.LineType
  ): Boolean =
    board.byPiece(color, partnerRole).exists { partner =>
      partner != destination &&
        (line match
          case lila.commentary.model.authoring.LineType.FileLine(file) => partner.file == file
          case lila.commentary.model.authoring.LineType.RankLine(rank) => partner.rank == rank
          case lila.commentary.model.authoring.LineType.DiagonalLine   => sameDiagonal(destination, partner)
        ) &&
        clearRayBetween(board, destination, partner)
    }

  private def sameDiagonal(left: Square, right: Square): Boolean =
    (left.file.value - right.file.value).abs == (left.rank.value - right.rank.value).abs

  private def clearRayBetween(board: Board, from: Square, to: Square): Boolean =
    val fileDiff = to.file.value - from.file.value
    val rankDiff = to.rank.value - from.rank.value
    val aligned =
      fileDiff == 0 || rankDiff == 0 || fileDiff.abs == rankDiff.abs
    if !aligned then false
    else
      val fileStep = Integer.signum(fileDiff)
      val rankStep = Integer.signum(rankDiff)
      def loop(file: Int, rank: Int): Boolean =
        Square.at(file, rank) match
          case Some(square) if square == to => true
          case Some(square) =>
            board.pieceAt(square).isEmpty && loop(file + fileStep, rank + rankStep)
          case None => false
      loop(from.file.value + fileStep, from.rank.value + rankStep)
