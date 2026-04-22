package lila.commentary.witness.seed

import chess.{ Bishop, Color, Square }

import lila.commentary.witness.seed.StrategySupportSeedTestSupport.*

class SamePieceSupportSeedRulesTest extends munit.FunSuite:

  test("same-piece liability anchor fixes one concrete loose minor piece"):
    val fen = "4k3/8/8/3b4/5N2/8/8/4K3 b - - 0 1"

    val anchor =
      findPieceSquare(fen, "same_piece_liability_anchor_seed", "d5", Some(Color.Black))
        .getOrElse(fail("missing same-piece liability anchor"))

    assertEquals(roleValue(anchor.payload, "piece_role"), Some(Bishop))
    assertEquals(squareValue(anchor.payload, "liability_anchor_square").map(_.key), Some("d5"))
    assertEquals(tokenList(anchor.payload, "liability_tags"), Vector("loose_piece"))

  test("same-piece repair route is hosted on the same liability carrier"):
    val fen = "4k3/8/8/3b4/5N2/8/8/4K3 b - - 0 1"

    val repair =
      findPieceSquare(fen, "same_piece_repair_route_seed", "d5", Some(Color.Black))
        .getOrElse(fail("missing same-piece repair route"))

    assertEquals(squareValue(repair.payload, "liability_anchor_square").map(_.key), Some("d5"))
    assert(squareList(repair.payload, "relief_squares").map(_.key).contains("c6"))
    assertEquals(descriptor(fen, "same_piece_exchange_relief_seed"), Vector.empty)

  test("same-piece exchange relief requires exchange contact by the anchored minor"):
    val fen = "4k3/8/8/3b4/5N2/8/6B1/7K b - - 0 1"

    val exchange =
      findPieceSquare(fen, "same_piece_exchange_relief_seed", "d5", Some(Color.Black))
        .getOrElse(fail("missing same-piece exchange relief"))

    assertEquals(squareValue(exchange.payload, "liability_anchor_square").map(_.key), Some("d5"))
    assertEquals(squareList(exchange.payload, "exchange_target_squares").map(_.key), Vector("g2"))

  test("same-piece exchange relief rejects an unprotected minor capture by the liability carrier"):
    val fen = "4k3/8/8/3b4/8/8/6B1/4K3 b - - 0 1"

    assert(findPieceSquare(fen, "same_piece_liability_anchor_seed", "d5", Some(Color.Black)).nonEmpty)
    assert(findPieceSquare(fen, "same_piece_exchange_relief_seed", "d5", Some(Color.Black)).isEmpty)

  test("same-piece exchange relief rejects raw attackers that cannot legally recapture"):
    val fen = "k3r3/8/8/3b4/8/8/4R1B1/4K3 b - - 0 1"

    assert(findPieceSquare(fen, "same_piece_liability_anchor_seed", "d5", Some(Color.Black)).nonEmpty)
    assert(findPieceSquare(fen, "same_piece_exchange_relief_seed", "d5", Some(Color.Black)).isEmpty)

  test("same-piece liability alone does not imply repair or exchange relief"):
    val fen = "6kb/5Npp/8/8/8/8/8/4K3 b - - 0 1"

    val anchor =
      findPieceSquare(fen, "same_piece_liability_anchor_seed", "h8", Some(Color.Black))
        .getOrElse(fail("missing trapped-piece liability anchor"))

    assert(tokenList(anchor.payload, "liability_tags").contains("trapped_piece"))
    assert(findPieceSquare(fen, "same_piece_repair_route_seed", "h8", Some(Color.Black)).isEmpty)
    assert(findPieceSquare(fen, "same_piece_exchange_relief_seed", "h8", Some(Color.Black)).isEmpty)

  test("same-piece exchange relief rejects an unrelated board exchange shortcut"):
    val fen = "r3k3/8/8/3b4/5N2/8/8/R3K3 b - - 0 1"

    assert(findPieceSquare(fen, "same_piece_liability_anchor_seed", "d5", Some(Color.Black)).nonEmpty)
    assertEquals(descriptor(fen, "same_piece_exchange_relief_seed"), Vector.empty)

  test("S17 seeds reject generic bad-piece, bishop-pair, mobility, and trade-invariant-only shortcuts"):
    val genericBadPieceFen = "4k3/8/8/3b4/8/8/8/4K3 b - - 0 1"
    val bishopPairFen = "4k3/8/8/8/8/8/6B1/2B1K3 w - - 0 1"
    val mobilityOnlyFen = "4k3/8/8/8/3B4/8/8/4K3 w - - 0 1"
    val rookTradeOnlyFen = "4k3/8/8/3r4/3R4/8/8/4K3 w - - 0 1"

    Vector(genericBadPieceFen, bishopPairFen, mobilityOnlyFen, rookTradeOnlyFen).foreach: fen =>
      assertEquals(descriptor(fen, "same_piece_liability_anchor_seed"), Vector.empty, fen)
      assertEquals(descriptor(fen, "same_piece_repair_route_seed"), Vector.empty, fen)
      assertEquals(descriptor(fen, "same_piece_exchange_relief_seed"), Vector.empty, fen)
