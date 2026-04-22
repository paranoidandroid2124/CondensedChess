package lila.commentary.witness.seed

import chess.{ Color, Queen, Rook }

import lila.commentary.witness.seed.StrategySupportSeedTestSupport.*

class HorizontalRankAccessSupportSeedRulesTest extends munit.FunSuite:

  test("rank corridor seed admits current-board cross-wing rank switching, not rook-lift history"):
    val fen = "6k1/8/8/8/R7/8/8/6K1 w - - 0 1"

    val corridor =
      findPieceSquare(fen, "rank_corridor_state_seed", "a4", Some(Color.White), Some("east_to_f4"))
        .getOrElse(fail("missing rank corridor seed"))

    assertEquals(squareValue(corridor.payload, "source_square").map(_.key), Some("a4"))
    assertEquals(roleValue(corridor.payload, "source_role"), Some(Rook))
    assertEquals(token(corridor.payload, "source_sector"), Some("queenside"))
    assertEquals(squareValue(corridor.payload, "entry_square").map(_.key), Some("f4"))
    assertEquals(token(corridor.payload, "entry_sector"), Some("kingside"))
    assertEquals(token(corridor.payload, "corridor_kind"), Some("cross_wing_rank_switch"))
    assertEquals(
      squareList(corridor.payload, "corridor_squares").map(_.key),
      Vector("b4", "c4", "d4", "e4", "f4")
    )
    assertEquals(squareList(corridor.payload, "crossed_center_squares").map(_.key), Vector("d4", "e4"))

  test("rank corridor seed admits queen cross-wing rank switching with the same exact-board law"):
    val fen = "6k1/8/8/8/Q7/8/8/6K1 w - - 0 1"

    val corridor =
      findPieceSquare(fen, "rank_corridor_state_seed", "a4", Some(Color.White), Some("east_to_f4"))
        .getOrElse(fail("missing queen rank corridor seed"))

    assertEquals(roleValue(corridor.payload, "source_role"), Some(Queen))
    assertEquals(squareValue(corridor.payload, "entry_square").map(_.key), Some("f4"))
    assertEquals(token(corridor.payload, "corridor_kind"), Some("cross_wing_rank_switch"))

  test("rank corridor seed rejects blocked same-rank paths"):
    val fen = "6k1/8/8/8/R2n4/8/8/6K1 w - - 0 1"

    assertEquals(descriptor(fen, "rank_corridor_state_seed"), Vector.empty)

  test("rank corridor seed rejects geometrically clear but illegal pinned switches"):
    val fen = "r5k1/8/8/8/R7/8/8/K7 w - - 0 1"

    assertEquals(descriptor(fen, "rank_corridor_state_seed"), Vector.empty)

  test("rank corridor seed rejects home-rank lateral mobility and back-rank defense"):
    val fen = "6k1/8/8/8/8/8/8/R5K1 w - - 0 1"

    assertEquals(descriptor(fen, "rank_corridor_state_seed"), Vector.empty)
