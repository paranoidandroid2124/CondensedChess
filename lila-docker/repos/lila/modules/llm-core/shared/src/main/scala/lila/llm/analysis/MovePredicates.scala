package lila.llm.analysis

import chess.*
import chess.format.Fen
import lila.llm.model.authoring.MovePattern

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
            // Hard to verify single move is "BatteryFormation" without state history, 
            // but we can check if the move places 'front' or 'back' piece on the line.
            // For now, accept if role matches and dest is on the line (simplified).
            // A more robust check would require the board state to see if they align.
            val onLine = line match
              case lila.llm.model.authoring.LineType.FileLine(f) => mv.dest.file == f
              case lila.llm.model.authoring.LineType.RankLine(r) => mv.dest.rank == r
              case _ => true
            (mv.piece.role == front || mv.piece.role == back) && onLine
