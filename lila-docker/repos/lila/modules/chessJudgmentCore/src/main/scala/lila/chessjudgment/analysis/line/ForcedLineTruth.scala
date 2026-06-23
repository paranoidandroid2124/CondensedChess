package lila.chessjudgment.analysis.line

import chess.*
import chess.format.{ Fen, Uci }
import lila.chessjudgment.analysis.tactical.TacticalPatternDetectors
import lila.chessjudgment.model.strategic.VariationLine

/**
 * Truth Boundary (High-Precision Validation Layer)
 *
 * Deterministic detection of:
 * 1. Opening Traps (Specific Move Sequences)
 * 2. Mating Patterns (Geometry Check)
 * 3. Tactical Themes (Structural Check)
 */
object ForcedLineTruth:

  val ImmediateReplyCheckId = "immediate_reply_check"

  case class VerifiedTheme(
    id: String,
    lineMoves: List[String] = Nil
  )

  def isProofSignalThemeId(id: String): Boolean =
    id != ImmediateReplyCheckId

  enum ExpectedResult:
    case Mate
    case WinMaterial
    case Stalemate
    case Perpetual

  case class TrapDef(
    id: String,
    moves: List[String],
    result: ExpectedResult
  )

  private case class TrapMatch(trap: TrapDef, remainingMoves: List[String])
  // 1. TRAP LIBRARY (Opening Sequences)
  
  val Traps = List(
    // --- King's Pawn ---
    TrapDef("legals_mate", List("f3e5", "g4d1", "c4f7", "e8e7", "c3d5"), ExpectedResult.Mate),
    TrapDef("scholars_mate", List("d1h5", "b8c6", "f1c4", "g8f6", "h5f7"), ExpectedResult.Mate),
    TrapDef("fools_mate", List("f3e4", "g2g4", "d8h4"), ExpectedResult.Mate),
    TrapDef("philidor_mate", List("f3g5", "f7g8", "g5f7", "g8g7", "f7h6", "g8h8", "d1g8", "f8g8", "h6f7"), ExpectedResult.Mate),
    TrapDef("fishing_pole", List("f6g4", "h3h4", "h5g4", "h4g5", "d8h4"), ExpectedResult.Mate),

    // --- Queen's Pawn / Gambits ---
    TrapDef("elephant_trap", List("c3d5", "f6d5", "g5d8", "f8b4", "d1d2", "b4d2", "e1d2", "e8d8"), ExpectedResult.WinMaterial),
    TrapDef("lasker_trap", List("d4e5", "d2b4", "e5f2", "e1e2", "f2g1"), ExpectedResult.WinMaterial),
    TrapDef("englund_gambit_mate", List("c3b4", "d2b4", "b2c1"), ExpectedResult.Mate),
    TrapDef("rubinstein_trap_qga", List("f3d4", "c6d4", "e2e3", "b5b4", "a3b4", "c5b4", "d1a4"), ExpectedResult.WinMaterial),

    // --- Sicilian & Others ---
    TrapDef("siberian_trap", List("f3d4", "d1h2"), ExpectedResult.Mate),
    TrapDef("magnus_smith_trap", List("e4e5", "d6e5", "c4f7", "e8f7", "d1d8"), ExpectedResult.WinMaterial),
    TrapDef("noahs_ark", List("f8b4", "c1d2", "b4d2", "d1d2", "d7d5", "e4d5", "c6d5", "c4b3", "c5c4"), ExpectedResult.WinMaterial),
    TrapDef("cambridge_springs", List("d4d5", "c4c5", "f3d2", "d8a5", "g5f6", "d7f6", "c3d5", "f6d5"), ExpectedResult.WinMaterial)
  )
  // 2. ENTRY POINT

  def detect(
      fen: String,
      playedUci: String,
      variations: List[VariationLine] = Nil
  ): Option[VerifiedTheme] =
    PrincipalVariationEvidence.legalFenAfter(fen, playedUci).flatMap { afterFen =>
      val posOpt = Fen.read(chess.variant.Standard, Fen.Full(afterFen))

      // 1. TRAP SEQUENCES
      val trapMatch =
        Traps
          .flatMap(trap => confirmedTrapLine(afterFen, playedUci, trap, variations))
          .headOption

      trapMatch.map { matched =>
        VerifiedTheme(matched.trap.id, matched.remainingMoves.map(normalizeUci))
      }.orElse {
        // 2. TACTICAL PATTERNS (Geometry / Static)
        val beforePosOpt = Fen.read(chess.variant.Standard, Fen.Full(fen))
        posOpt.flatMap { pos =>
          detectPatterns(beforePosOpt, pos, playedUci, variations)
        }
      }.orElse(detectImmediateReplyCheck(fen, playedUci, variations))
    }
  // 3. PATTERN DETECTORS (Geometric Matches)

  private def detectPatterns(
      beforePos: Option[Position],
      pos: Position,
      playedUci: String,
      variations: List[VariationLine]
  ): Option[VerifiedTheme] =
    val continuationLines = variations.map(_.moves.map(normalizeUci))
    TacticalPatternDetectors.ordered
      .find(detector =>
        (!detector.requiresMate || pos.checkMate) &&
          detector.matchesWithContinuations(beforePos, pos, playedUci, continuationLines)
      )
      .map(detector => VerifiedTheme(detector.id))

  private[analysis] def validateForcedSequence(startFen: String, moves: List[String], expected: ExpectedResult): Boolean =
    Fen.read(chess.variant.Standard, Fen.Full(startFen)).exists { startPos =>
      applyLineStrict(startFen, moves) match
        case Some(finalPos) =>
          expected match
            case ExpectedResult.Mate      => finalPos.checkMate
            case ExpectedResult.Stalemate => finalPos.staleMate
            case ExpectedResult.WinMaterial =>
              val finalMover = !finalPos.color
              materialBalance(finalPos.board, finalMover) - materialBalance(startPos.board, finalMover) >= 250
            case ExpectedResult.Perpetual => false
        case None => false
    }

  private[analysis] def verifySequence(startFen: String, moves: List[String], expected: ExpectedResult): Boolean =
    validateForcedSequence(startFen, moves, expected)

  private def confirmedTrapLine(
      afterFen: String,
      playedUci: String,
      trap: TrapDef,
      variations: List[VariationLine]
  ): Option[TrapMatch] =
    trap.moves match
      case entry :: remaining
          if normalizeUci(entry) == normalizeUci(playedUci) &&
            pvConfirmsTrapLine(entry, remaining, variations) &&
            validateForcedSequence(afterFen, remaining, trap.result) =>
        Some(TrapMatch(trap, remaining))
      case _ => None

  private def pvConfirmsTrapLine(
      entry: String,
      remaining: List[String],
      variations: List[VariationLine]
  ): Boolean =
    val fullLine = (entry :: remaining).map(normalizeUci)
    val tail = remaining.map(normalizeUci)
    tail.nonEmpty &&
      variations.exists { variation =>
        val moves = variation.moves.map(normalizeUci)
        moves.take(fullLine.size) == fullLine ||
          moves.take(tail.size) == tail
      }

  private def detectImmediateReplyCheck(
      fen: String,
      playedUci: String,
      variations: List[VariationLine]
  ): Option[VerifiedTheme] =
    val played = normalizeUci(playedUci)
    variations.view
      .flatMap { variation =>
        val moves = variation.moves.map(normalizeUci).filter(_.nonEmpty)
        Option.when(moves.headOption.contains(played) && moves.size >= 2)(moves.take(2))
      }
      .find(replyChecksMover(fen, _))
      .map(line => VerifiedTheme(ImmediateReplyCheckId, line))

  private def replyChecksMover(fen: String, line: List[String]): Boolean =
    applyLineStrict(fen, line).exists(_.check.yes)

  private def normalizeUci(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def materialBalance(board: Board, color: Color): Int =
    materialScore(board, color) - materialScore(board, !color)

  private def materialScore(board: Board, color: Color): Int =
    board.byPiece(color, Pawn).count * 100 +
      board.byPiece(color, Knight).count * 320 +
      board.byPiece(color, Bishop).count * 330 +
      board.byPiece(color, Rook).count * 500 +
      board.byPiece(color, Queen).count * 900

  private def applyLineStrict(startFen: String, uciLine: List[String]): Option[Position] =
    Fen.read(chess.variant.Standard, Fen.Full(startFen)).flatMap { start =>
      var pos = start
      var ok = true
      val it = uciLine.iterator
      while (it.hasNext && ok) {
        val u = it.next()
        applyUci(pos, u) match
          case None => ok = false
          case Some(next) =>
            pos = next
      }
      Option.when(ok)(pos)
    }

  private def applyUci(pos: Position, uciStr: String): Option[Position] =
    Uci(uciStr).collect { case m: Uci.Move => m }.flatMap { u =>
      pos.move(u).toOption.map(_.after)
    }
