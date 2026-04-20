package lila.commentary.strategic

import chess.Color

import StrategicObjectTestSupport.*
import lila.commentary.witness.WitnessAnchor

class KingSafetyShellRuleTest extends munit.FunSuite:

  test("king safety shell matches the frozen exact-board corpus row"):
    val fen = "6k1/8/6p1/4B3/8/8/7R/6K1 w - - 0 1"

    val kingSafety = findSquare(fen, "KingSafetyShell", "g7", Some(Color.Black))

    assert(kingSafety.nonEmpty)
    assertEquals(kingSafety.get.anchor, WitnessAnchor.SquareAnchor(chess.Square.fromKey("g7").get))
    assertEquals(kingSafety.get.color, Some(Color.Black))
    assertEquals(squareList(kingSafety.get.payload, "paired_holes").map(_.key), Vector("g7", "h7"))

  test("king safety shell rejects a single home-shelter hole"):
    val fen = "6k1/5pp1/8/8/8/8/7R/6K1 w - - 0 1"

    assertEquals(family(fen, "KingSafetyShell"), Vector.empty)

  test("king safety shell rejects non-adjacent shelter defects"):
    val fen = "6k1/6p1/6p1/4N3/8/8/7R/6K1 w - - 0 1"

    assertEquals(family(fen, "KingSafetyShell"), Vector.empty)

  test("king safety shell rejects a central home-rank king"):
    val fen = "4k3/8/8/8/8/8/8/3RR1K1 b - - 0 1"

    assertEquals(family(fen, "KingSafetyShell"), Vector.empty)
