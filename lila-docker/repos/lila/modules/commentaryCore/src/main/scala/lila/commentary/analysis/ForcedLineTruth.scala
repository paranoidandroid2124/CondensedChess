package lila.commentary.analysis

import chess.*
import chess.format.{ Fen, Uci }
import lila.commentary.analysis.tactical.TacticalPatternDetectors
import lila.commentary.model.strategic.VariationLine

/**
 * Truth Boundary (High-Precision Validation Layer)
 *
 * Deterministic detection of:
 * 1. Opening Traps (Specific Move Sequences)
 * 2. Mating Patterns (Geometry Check)
 * 3. Tactical Themes (Structural Check)
 */
object ForcedLineTruth:

  case class VerifiedTheme(
    id: String,
    name: String,
    line: String
  )

  enum ExpectedResult:
    case Mate
    case WinMaterial
    case Stalemate
    case Perpetual

  case class TrapDef(
    id: String,
    name: String,
    moves: List[String],
    result: ExpectedResult,
    description: String = ""
  )

  private case class TrapMatch(trap: TrapDef, remainingMoves: List[String])
  // 1. TRAP LIBRARY (Opening Sequences)
  
  val Traps = List(
    // --- King's Pawn ---
    TrapDef("legals_mate", "Legal’s Mate", List("f3e5", "g4d1", "c4f7", "e8e7", "c3d5"), ExpectedResult.Mate),
    TrapDef("scholars_mate", "Scholar’s Mate", List("d1h5", "b8c6", "f1c4", "g8f6", "h5f7"), ExpectedResult.Mate),
    TrapDef("fools_mate", "Fool’s Mate", List("f3e4", "g2g4", "d8h4"), ExpectedResult.Mate),
    TrapDef("philidor_legacy", "Philidor Legacy (Smothered)", List("f3g5", "f7g8", "g5f7", "g8g7", "f7h6", "g8h8", "d1g8", "f8g8", "h6f7"), ExpectedResult.Mate),
    TrapDef("fishing_pole", "Fishing Pole Trap", List("f6g4", "h3h4", "h5g4", "h4g5", "d8h4"), ExpectedResult.Mate), // Approximated entry

    // --- Queen's Pawn / Gambits ---
    TrapDef("elephant_trap", "Elephant Trap", List("c3d5", "f6d5", "g5d8", "f8b4", "d1d2", "b4d2", "e1d2", "e8d8"), ExpectedResult.WinMaterial),
    TrapDef("lasker_trap", "Lasker Trap", List("d4e5", "d2b4", "e5f2", "e1e2", "f2g1"), ExpectedResult.WinMaterial), // Albin Counter-Gambit subset
    TrapDef("englund_gambit_mate", "Englund Gambit Mate", List("c3b4", "d2b4", "b2c1"), ExpectedResult.Mate), // ...Qxb2 naive defense
    TrapDef("rubinstein_trap_qga", "Rubinstein Trap (QGA)", List("f3d4", "c6d4", "e2e3", "b5b4", "a3b4", "c5b4", "d1a4"), ExpectedResult.WinMaterial), // Winning the Knight on d4/b4 complex

    // --- Sicilian & Others ---
    TrapDef("siberian_trap", "Siberian Trap", List("f3d4", "d1h2"), ExpectedResult.Mate), // Smith-Morra: ...Nd4 -> ...Qh2# or ...Nxf3+
    TrapDef("magnus_smith_trap", "Magnus Smith Trap", List("e4e5", "d6e5", "c4f7", "e8f7", "d1d8"), ExpectedResult.WinMaterial), // Sicilian structure
    TrapDef("noahs_ark", "Noah’s Ark Trap", List("f8b4", "c1d2", "b4d2", "d1d2", "d7d5", "e4d5", "c6d5", "c4b3", "c5c4"), ExpectedResult.WinMaterial), // White Bishop trapped on b3
    TrapDef("cambridge_springs", "Cambridge Springs Trap", List("d4d5", "c4c5", "f3d2", "d8a5", "g5f6", "d7f6", "c3d5", "f6d5"), ExpectedResult.WinMaterial) // Winning piece after mistakes
  )
  // 2. ENTRY POINT

  def detect(
      fen: String,
      playedUci: String,
      ply: Int,
      variations: List[VariationLine] = Nil
  ): Option[VerifiedTheme] =
    MoveReviewPvLine.legalFenAfter(fen, playedUci).flatMap { afterFen =>
      val posOpt = Fen.read(chess.variant.Standard, Fen.Full(afterFen))

      // 1. TRAP SEQUENCES
      val trapMatch =
        Traps
          .flatMap(trap => confirmedTrapLine(afterFen, playedUci, trap, variations))
          .headOption

      trapMatch.map { matched =>
        val displayLine =
          NarrativeUtils.formatSanWithMoveNumbers(ply + 1, generateSanLine(afterFen, matched.remainingMoves))
        VerifiedTheme(matched.trap.id, matched.trap.name, displayLine)
      }.orElse {
        // 2. TACTICAL PATTERNS (Geometry / Static)
        val beforePosOpt = Fen.read(chess.variant.Standard, Fen.Full(fen))
        posOpt.flatMap { pos =>
          detectPatterns(beforePosOpt, pos, playedUci, variations)
        }
      }
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
      .map(detector => VerifiedTheme(detector.id, detector.displayName, ""))

  private[analysis] def validateForcedSequence(startFen: String, moves: List[String], expected: ExpectedResult): Boolean =
    Fen.read(chess.variant.Standard, Fen.Full(startFen)).exists { startPos =>
      applyLineStrict(startFen, moves) match
        case Some((finalPos, _)) =>
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

  private def generateSanLine(startFen: String, moves: List[String]): List[String] =
    applyLineStrict(startFen, moves).map(_._2).getOrElse(Nil)

  private def applyLineStrict(startFen: String, uciLine: List[String]): Option[(Position, List[String])] =
    Fen.read(chess.variant.Standard, Fen.Full(startFen)).flatMap { start =>
      val sans = scala.collection.mutable.ListBuffer.empty[String]
      var pos = start
      var ok = true
      val it = uciLine.iterator
      while (it.hasNext && ok) {
        val u = it.next()
        applyUci(pos, u) match
          case None => ok = false
          case Some((next, san)) =>
            pos = next
            sans += san
      }
      Option.when(ok)(pos -> sans.toList)
    }

  private def applyUci(pos: Position, uciStr: String): Option[(Position, String)] =
    Uci(uciStr).collect { case m: Uci.Move => m }.flatMap { u =>
      pos.move(u).toOption.map { mv => (mv.after, mv.toSanStr.toString) }
    }

  // Compatibility
  def legalMateTrap(fen: String, playedMoveUci: String, ply: Int): Option[VerifiedTheme] =
    detect(fen, playedMoveUci, ply).filter(_.id == "legals_mate")
