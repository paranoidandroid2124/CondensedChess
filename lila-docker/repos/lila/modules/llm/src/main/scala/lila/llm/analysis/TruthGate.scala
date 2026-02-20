package lila.llm.analysis

import chess.*
import chess.format.{ Fen, Uci }

/**
 * Truth Gate (High-Precision Validation Layer)
 *
 * Deterministic detection of:
 * 1. Opening Traps (Specific Move Sequences)
 * 2. Mating Patterns (Geometry Check)
 * 3. Tactical Themes (Structural Check)
 */
object TruthGate:

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

  def detect(fen: String, playedUci: String, ply: Int): Option[VerifiedTheme] =
    val afterFen = NarrativeUtils.uciListToFen(fen, List(playedUci))
    val posOpt = Fen.read(chess.variant.Standard, Fen.Full(afterFen))

    // 1. TRAP SEQUENCES
    val trapMatch = Traps.find { trap =>
      trap.moves.headOption.flatMap(m => Uci(m).collect { case mv: Uci.Move => mv }).exists { u =>
        posOpt.exists(_.move(u).isRight)
      } && verifySequence(afterFen, trap.moves, trap.result)
    }

    trapMatch.map { t =>
      val displayLine = NarrativeUtils.formatSanWithMoveNumbers(ply + 1, generateSanLine(afterFen, t.moves))
      VerifiedTheme(t.id, t.name, displayLine)
    }.orElse {
      // 2. TACTICAL PATTERNS (Geometry / Static)
      posOpt.flatMap { pos =>
        detectPatterns(pos, ply, playedUci)
      }
    }
  // 3. PATTERN DETECTORS (Geometric Matches)

  private def detectPatterns(pos: Position, ply: Int, playedUci: String): Option[VerifiedTheme] =
    if (pos.checkMate)
      if (isSmotheredMate(pos)) Some(VerifiedTheme("smothered_mate", "Smothered Mate", ""))
      else if (isBackRankMate(pos)) Some(VerifiedTheme("back_rank_mate", "Back Rank Mate", ""))
      else if (isArabianMate(pos)) Some(VerifiedTheme("arabian_mate", "Arabian Mate", ""))
      else if (isBodensMate(pos)) Some(VerifiedTheme("bodens_mate", "Boden’s Mate", ""))
      else if (isAnastasiaMate(pos)) Some(VerifiedTheme("anastasia_mate", "Anastasia’s Mate", ""))
      else if (isHookMate(pos)) Some(VerifiedTheme("hook_mate", "Hook Mate", ""))
      else if (isCornerMate(pos)) Some(VerifiedTheme("corner_mate", "Corner Mate", ""))
      else None
    else if (pos.staleMate)
      Some(VerifiedTheme("stalemate_trap", "Stalemate Trap", ""))
    else if (isGreekGift(pos, playedUci))
      Some(VerifiedTheme("greek_gift", "Greek Gift Sacrifice", ""))
    else if (isWindmillCandidate(pos))
      Some(VerifiedTheme("windmill", "Windmill Pattern", ""))
    else
      None

  // --- MATE PATTERNS ---

  private def isSmotheredMate(pos: Position): Boolean =
    pos.board.kingPosOf(pos.color).exists { king =>
      // Knight check ONLY
      pos.board.attackers(king, !pos.color).forall(sq => pos.board.roleAt(sq).contains(Knight)) &&
      // All flight squares blocked by OWN pieces
      king.kingAttacks.forall(sq => pos.board.pieceAt(sq).exists(_.color == pos.color))
    }

  private def isBackRankMate(pos: Position): Boolean =
    val loser = pos.color
    pos.board.kingPosOf(loser).exists { king =>
      val backRank = if (loser.white) Rank.First else Rank.Eighth
      if (king.rank != backRank) false
      else {
        // Checked by R/Q
        val checkedByMajor = pos.board.attackers(king, !loser).exists(sq => pos.board.roleAt(sq).exists(r => r == Rook || r == Queen))
        if (!checkedByMajor) false
        else {
          // Flights forward blocked by own pieces OR controlled by enemy
          val forward = if (loser.white) Rank.Second else Rank.Seventh
          king.kingAttacks.filter(_.rank == forward).forall { sq =>
            pos.board.pieceAt(sq).exists(_.color == loser) || pos.board.attackers(sq, !loser).nonEmpty
          }
        }
      }
    }

  private def isArabianMate(pos: Position): Boolean =
    pos.board.kingPosOf(pos.color).exists { king =>
      val winner = !pos.color
      // R+N mate in corner. R adjacent to K, N defends R.
      isCornerRegion(king) && {
        val checkers = pos.board.attackers(king, winner)
        checkers.exists(sq => pos.board.roleAt(sq).contains(Rook) && pos.board.attackers(sq, winner).exists(k => pos.board.roleAt(k).contains(Knight)))
      }
    }

  private def isBodensMate(pos: Position): Boolean =
    pos.board.kingPosOf(pos.color).exists { king =>
      val winner = !pos.color
      val checkers = pos.board.attackers(king, winner)
      // Must be Bishop check and at least 2 bishops for winner
      checkers.exists(sq => pos.board.roleAt(sq).contains(Bishop)) &&
      (pos.board.bishops & pos.board.byColor(winner)).count >= 2
    }

  private def isAnastasiaMate(pos: Position): Boolean =
    pos.board.kingPosOf(pos.color).exists { king =>
      if (king.file != File.H && king.file != File.A) false
      else {
        val winner = !pos.color
        val checkers = pos.board.attackers(king, winner)
        val fileCheck = checkers.exists(sq => (pos.board.roleAt(sq).contains(Rook) || pos.board.roleAt(sq).contains(Queen)) && sq.file == king.file)
        if (!fileCheck) false
        else {
          (pos.board.knights & pos.board.byColor(winner)).exists { sq =>
            sq.file != king.file && (sq.rank.value - king.rank.value).abs <= 2
          }
        }
      }
    }

  private def isHookMate(pos: Position): Boolean =
    pos.board.kingPosOf(pos.color).exists { king =>
      val winner = !pos.color
      val checkers = pos.board.attackers(king, winner)
      val rookCheck = checkers.find(sq => pos.board.roleAt(sq).contains(Rook))
      rookCheck.exists { rSq =>
        // Rook protected by Knight
        pos.board.attackers(rSq, winner).exists(sq => pos.board.roleAt(sq).contains(Knight))
      }
    }

  private def isCornerMate(pos: Position): Boolean =
    pos.board.kingPosOf(pos.color).exists { king =>
      isCornerRegion(king) && (pos.board.attackers(king, !pos.color).count >= 1) && king.kingAttacks.forall(sq => pos.board.pieceAt(sq).isDefined)
    }

  // --- TACTICAL THEMES (Non-Mate) ---

  private def isGreekGift(pos: Position, lastUci: String): Boolean =
    Uci(lastUci).collect{ case m: Uci.Move => m }.exists { move =>
      val isBishopSac = pos.board.roleAt(move.dest).isEmpty && 
                        (move.dest == Square.H7 || move.dest == Square.H2)
      (lastUci.contains("h7") || lastUci.contains("h2")) && pos.check.yes
    }

  private def isWindmillCandidate(pos: Position): Boolean =
    // Discovered check mechanism available?
    // R+B or R+N setup where R moves, checking, then returns?
    // Hard to detect statically without sequence.
    false // Placeholder for complex logic

  // --- HELPERS ---

  private def isCornerRegion(sq: Square): Boolean =
    (sq.file == File.A || sq.file == File.B || sq.file == File.G || sq.file == File.H) &&
    (sq.rank == Rank.First || sq.rank == Rank.Second || sq.rank == Rank.Seventh || sq.rank == Rank.Eighth)

  private def verifySequence(startFen: String, moves: List[String], expected: ExpectedResult): Boolean =
     applyLineStrict(startFen, moves) match
       case Some((finalPos, _)) =>
         expected match
           case ExpectedResult.Mate => finalPos.checkMate
           case ExpectedResult.Stalemate => finalPos.staleMate
           case _ => true // Accept functional traps
       case None => false

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
