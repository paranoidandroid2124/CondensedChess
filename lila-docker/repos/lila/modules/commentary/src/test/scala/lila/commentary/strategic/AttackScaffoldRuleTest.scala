package lila.commentary.strategic

import chess.Color

import StrategicObjectTestSupport.*
import lila.commentary.witness.WitnessAnchor

class AttackScaffoldRuleTest extends munit.FunSuite:

  test("attack scaffold matches the frozen exact-board corpus row"):
    val fen = "6k1/6pp/8/8/3B4/8/6R1/6K1 w - - 0 1"

    val attackScaffold = findSquare(fen, "AttackScaffold", "g8", Some(Color.White))

    assert(attackScaffold.nonEmpty)
    assertEquals(attackScaffold.get.anchor, WitnessAnchor.SquareAnchor(chess.Square.fromKey("g8").get))
    assertEquals(attackScaffold.get.color, Some(Color.White))

  test("attack scaffold rejects off-theater pressure"):
    val fen = "1k6/6pp/8/8/3B4/8/6R1/6K1 w - - 0 1"

    assertEquals(family(fen, "AttackScaffold"), Vector.empty)

  test("attack scaffold rejects carrier-only pressure"):
    val fen = "8/6k1/8/8/8/8/6R1/6K1 b - - 0 1"

    assertEquals(family(fen, "AttackScaffold"), Vector.empty)

  test("attack scaffold rejects a lone open-file rook plus shelter holes"):
    val fen = "6k1/8/8/8/8/8/6R1/K7 b - - 0 1"

    assertEquals(family(fen, "AttackScaffold"), Vector.empty)

  test("attack scaffold rejects same-file stacked heavy pieces plus shelter holes"):
    val fen = "6k1/8/8/8/8/8/6R1/6QK b - - 0 1"

    assertEquals(family(fen, "AttackScaffold"), Vector.empty)
