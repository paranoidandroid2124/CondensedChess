package lila.commentary.analysis.structure

import chess.{ Board, Color }
import chess.format.Fen
import chess.variant.Standard
import lila.commentary.analysis.NarrativeUtils
import munit.FunSuite

class StructuralDeltaAnalyzerTest extends FunSuite:

  private def boardFromFen(fen: String): Board =
    Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"invalid FEN: $fen"))

  private def deltaAfter(
      fen: String,
      moves: List[String],
      side: Color,
      files: List[Char],
      targets: List[String],
      createdTensionFrom: Option[String]
  ): StructuralDelta =
    val afterFen = NarrativeUtils.uciListToFen(fen, moves)
    StructuralDeltaAnalyzer
      .delta(
        beforeFen = fen,
        beforeBoard = boardFromFen(fen),
        afterFen = afterFen,
        afterBoard = boardFromFen(afterFen),
        side = side,
        files = files,
        targets = targets,
        createdTensionFrom = createdTensionFrom
      )
      .getOrElse(fail("missing structural delta"))

  test("before-after minority break records target tension, weakness, and pressure gain") {
    val delta =
      deltaAfter(
        fen = "4k3/pp3ppp/2p5/3p4/1P1P4/4P3/P4PPP/4K3 w - - 0 1",
        moves = List("b4b5"),
        side = Color.White,
        files = List('a', 'b', 'c'),
        targets = List("c6"),
        createdTensionFrom = Some("b5")
      )

    assert(delta.hasConsequence, clues(delta))
    assert(delta.createdTension.contains("b5-c6"), clues(delta))
    assert(delta.newWeakPawns.contains("c6"), clues(delta))
    assert(delta.newWeakSquares.contains("c6"), clues(delta))
    assert(delta.targetPressureDelta > 0, clues(delta))
  }

  test("unchanged before-after position has no structural consequence") {
    val fen = "4k3/pp3ppp/2p5/3p4/1P1P4/4P3/P4PPP/4K3 w - - 0 1"
    val delta =
      StructuralDeltaAnalyzer
        .delta(
          beforeFen = fen,
          beforeBoard = boardFromFen(fen),
          afterFen = fen,
          afterBoard = boardFromFen(fen),
          side = Color.White,
          files = List('a', 'b', 'c'),
          targets = List("c6"),
          createdTensionFrom = None
        )
        .getOrElse(fail("missing structural delta"))

    assert(!delta.hasConsequence, clues(delta))
  }

  test("color-reversed break preserves target-pressure semantics") {
    val delta =
      deltaAfter(
        fen = "4k3/p4ppp/4p3/1p1p4/3P4/2P1P3/PP3PPP/4K3 b - - 0 1",
        moves = List("b5b4"),
        side = Color.Black,
        files = List('a', 'b', 'c'),
        targets = List("c3"),
        createdTensionFrom = Some("b4")
      )

    assert(delta.hasConsequence, clues(delta))
    assert(delta.createdTension.contains("b4-c3"), clues(delta))
    assert(delta.newWeakPawns.contains("c3"), clues(delta))
    assert(delta.targetPressureDelta > 0, clues(delta))
  }

  test("derives dynamic targets when no coordinate list is supplied") {
    val delta =
      deltaAfter(
        fen = "4k3/pp3ppp/2p5/3p4/1P1P4/4P3/P4PPP/4K3 w - - 0 1",
        moves = List("b4b5"),
        side = Color.White,
        files = List('a', 'b', 'c'),
        targets = Nil,
        createdTensionFrom = Some("b5")
      )

    assert(delta.hasConsequence, clues(delta))
    assert(delta.createdTension.contains("b5-c6"), clues(delta))
    assert(delta.newWeakPawns.contains("c6"), clues(delta))
  }
