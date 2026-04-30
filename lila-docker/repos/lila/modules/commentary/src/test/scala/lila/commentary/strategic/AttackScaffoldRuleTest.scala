package lila.commentary.strategic

import chess.Color

import StrategicObjectTestSupport.*
import lila.commentary.witness.WitnessAnchor
import lila.commentary.witness.WitnessValue

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

  test("attack scaffold rejects early opening retained-home-piece geometry"):
    val fen = "rnbqk2r/ppp1bppp/4pn2/3p4/8/5NP1/PPPPPPBP/RNBQ1RK1 w kq - 2 5"

    assertEquals(family(fen, "AttackScaffold"), Vector.empty)

  test("attack scaffold does not use unbound loose or pinned smell as support"):
    val fen = "6k1/5q2/4B3/8/8/8/7R/6K1 w - - 0 1"

    val supportFragments =
      family(fen, "AttackScaffold")
        .flatMap(_.payload.get("support_fragment_ids"))
        .collect { case WitnessValue.TokenListValue(values) => values }
        .flatten
        .toSet

    assert(!supportFragments.contains("loose_piece"), clues(supportFragments))
    assert(!supportFragments.contains("pinned_piece"), clues(supportFragments))

  test("attack scaffold rejects audited wrong-theater pinned diagonal projection shape"):
    val fen = "3r1rk1/pp2ppbp/1qn3p1/3R1b2/Q7/2P3P1/PP2PPBP/R1B1N1K1 b - - 2 13"

    assertEquals(family(fen, "AttackScaffold"), Vector.empty)

  test("attack scaffold rejects audited opening diagonal pinned projection shape"):
    val fen = "r1br2k1/1pq2ppp/p1n1pn2/2b5/P1B1P3/2N2N1P/1P2QPP1/R1B1R1K1 b - - 2 13"

    assertEquals(family(fen, "AttackScaffold"), Vector.empty)

  test("attack scaffold rejects audited queenless second-rank loose pin projection shape"):
    val fen = "6k1/5pp1/b6p/4p3/1N4n1/1P2P1P1/3r1PBP/2R3K1 b - - 0 29"

    assertEquals(family(fen, "AttackScaffold"), Vector.empty)
