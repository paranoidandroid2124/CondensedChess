package lila.commentary.witness.u

import chess.Color

import lila.commentary.witness.WitnessDirection
import lila.commentary.witness.u.UWitnessTestSupport.*

class TacticalGeometryRulesTest extends munit.FunSuite:

  test("pin emits absolute and relative ray witnesses only when the blocker is actually pinned"):
    val absoluteFen = "4r2k/8/8/8/8/8/4B3/4K3 w - - 0 1"
    val relativeFen = "4k3/8/8/8/1b6/2N5/3Q4/4K3 w - - 0 1"
    val negativeFen = "4r2k/8/8/8/8/8/3B4/4K3 w - - 0 1"

    val absoluteWitness =
      findRay(absoluteFen, "pin", "e8", WitnessDirection.South, Some(Color.Black)).getOrElse(fail("missing absolute pin"))
    val relativeWitness =
      findRay(relativeFen, "pin", "b4", WitnessDirection.SouthEast, Some(Color.Black)).getOrElse(fail("missing relative pin"))

    assertEquals(token(absoluteWitness.payload, "pin_mode"), Some("absolute_king_pin"))
    assertEquals(token(relativeWitness.payload, "pin_mode"), Some("relative_anchor_pin"))
    assert(findRay(negativeFen, "pin", "e8", WitnessDirection.South, Some(Color.Black)).isEmpty)

  test("fork requires two current enemy targets"):
    val positiveFen = "4k3/8/3r1r2/8/4N3/8/8/4K3 w - - 0 1"
    val negativeFen = "4k3/8/3r4/8/4N3/8/8/4K3 w - - 0 1"

    val witness =
      findPieceSquare(positiveFen, "fork", "e4", Some(Color.White)).getOrElse(fail("missing fork"))

    assertEquals(squareList(witness.payload, "target_squares").map(_.key), Vector("d6", "f6"))
    assert(findPieceSquare(negativeFen, "fork", "e4", Some(Color.White)).isEmpty)

  test("skewer requires higher-value front target in front of a rear target"):
    val positiveFen = "r6k/q7/8/8/8/8/8/R6K w - - 0 1"
    val negativeFen = "q6k/r7/8/8/8/8/8/R6K w - - 0 1"

    assert(findRay(positiveFen, "skewer", "a1", WitnessDirection.North, Some(Color.White)).nonEmpty)
    assert(findRay(negativeFen, "skewer", "a1", WitnessDirection.North, Some(Color.White)).isEmpty)

  test("overload requires at least two qualifying duty squares"):
    val positiveFen = "k2r3r/8/8/8/3Q3B/8/3N4/K7 w - - 0 1"
    val negativeFen = "k2r4/8/8/8/3Q4/8/3N4/K7 w - - 0 1"

    assert(findPieceSquare(positiveFen, "overload", "d4", Some(Color.Black)).nonEmpty)
    assert(findPieceSquare(negativeFen, "overload", "d4", Some(Color.Black)).isEmpty)
