package lila.commentary.strategic

import chess.Color

import StrategicObjectTestSupport.*
import lila.commentary.witness.WitnessAnchor

class FortressHoldingShellRuleTest extends munit.FunSuite:

  test("fortress holding shell matches the frozen exact-board corpus row"):
    val fen = "r6k/6pp/8/8/4K3/8/8/1R6 w - - 0 1"

    val fortress = findSquare(fen, "FortressHoldingShell", "h8", Some(Color.Black))

    assert(fortress.nonEmpty)
    assertEquals(fortress.get.anchor, WitnessAnchor.SquareAnchor(chess.Square.fromKey("h8").get))
    assertEquals(fortress.get.color, Some(Color.Black))
    assertEquals(squareList(fortress.get.payload, "occupied_shell_squares").map(_.key), Vector("g7", "h7"))

  test("fortress holding shell rejects insufficient shell occupancy"):
    val fen = "r6k/7p/4b3/8/4K3/8/8/1R6 w - - 0 1"

    assertEquals(family(fen, "FortressHoldingShell"), Vector.empty)

  test("fortress holding shell rejects direct file entry"):
    val fen = "r6k/6pp/8/8/4K3/8/8/7R w - - 0 1"

    assertEquals(family(fen, "FortressHoldingShell"), Vector.empty)

  test("fortress holding shell rejects diagonal entry into the shell mask"):
    val fen = "7k/6pp/8/8/4B3/4K3/8/8 w - - 0 1"

    assertEquals(family(fen, "FortressHoldingShell"), Vector.empty)

  test("fortress holding shell keeps a blocked neighboring-file major outside shell entry"):
    val fen = "7k/6pp/8/6K1/6B1/8/8/6R1 w - - 0 1"

    val fortress = findSquare(fen, "FortressHoldingShell", "h8", Some(Color.Black))

    assert(fortress.nonEmpty)

  test("fortress holding shell rejects same-file passed pawn pressure"):
    val fen = "6k1/5b1r/8/6P1/8/8/8/R5K1 w - - 0 1"

    assertEquals(family(fen, "FortressHoldingShell"), Vector.empty)
