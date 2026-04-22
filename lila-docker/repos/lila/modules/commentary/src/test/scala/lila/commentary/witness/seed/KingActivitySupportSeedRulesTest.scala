package lila.commentary.witness.seed

import chess.Color

import lila.commentary.witness.seed.StrategySupportSeedTestSupport.*

class KingActivitySupportSeedRulesTest extends munit.FunSuite:

  test("king entry and access route seeds use an exact king path to an enemy-pawn entry square"):
    val fen = "6k1/8/8/3p4/5K2/8/8/8 w - - 0 1"

    val entry =
      findSquare(fen, "king_entry_square_seed", "e5", Some(Color.White))
        .getOrElse(fail("missing king entry square"))
    val route =
      findPieceSquare(fen, "king_access_route_seed", "f4", Some(Color.White), Some("to_e5"))
        .getOrElse(fail("missing king access route"))

    assertEquals(squareValue(entry.payload, "entry_square").map(_.key), Some("e5"))
    assertEquals(squareValue(route.payload, "king_square").map(_.key), Some("f4"))
    assertEquals(squareValue(route.payload, "entry_square").map(_.key), Some("e5"))
    assertEquals(squareList(route.payload, "route_squares").map(_.key), Vector("f4", "e5"))

  test("king opposition contact seed is exact opposition, not a winning projection claim"):
    val fen = "8/8/4k3/8/4K3/8/8/8 b - - 0 1"

    val opposition =
      findSquare(fen, "king_opposition_contact_seed", "e5", Some(Color.White))
        .getOrElse(fail("missing king opposition contact"))

    assertEquals(squareValue(opposition.payload, "beneficiary_king_square").map(_.key), Some("e4"))
    assertEquals(squareValue(opposition.payload, "rival_king_square").map(_.key), Some("e6"))
    assertEquals(token(opposition.payload, "relation"), Some("direct_opposition"))
    assertEquals(token(opposition.payload, "projection_status"), None)

  test("king entry and access reject generic centralization without a named entry function"):
    val fen = "6k1/8/8/8/4K3/8/8/8 w - - 0 1"

    assertEquals(descriptor(fen, "king_entry_square_seed"), Vector.empty)
    assertEquals(descriptor(fen, "king_access_route_seed"), Vector.empty)

  test("king access route rejects a route-shaped path when no entry seed is admitted"):
    val fen = "6k1/8/8/8/5K2/8/8/8 w - - 0 1"

    assertEquals(descriptor(fen, "king_entry_square_seed"), Vector.empty)
    assertEquals(descriptor(fen, "king_access_route_seed"), Vector.empty)

  test("king opposition contact rejects mere adjacency and broad endgame proximity"):
    val adjacentFen = "8/8/8/4k3/4K3/8/8/8 w - - 0 1"
    val remoteFen = "8/8/8/8/4K3/8/8/6k1 w - - 0 1"

    assertEquals(descriptor(adjacentFen, "king_opposition_contact_seed"), Vector.empty)
    assertEquals(descriptor(remoteFen, "king_opposition_contact_seed"), Vector.empty)
