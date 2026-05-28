package lila.commentary.analysis

import munit.FunSuite

class OpeningRouteCatalogTest extends FunSuite:

  private val BenoniFen =
    "4k3/8/3p4/2pP4/8/5N2/8/4K3 w - - 0 1"
  private val BlackKnightF6Fen =
    "4k3/8/5n2/8/8/3P4/8/4K3 b - - 0 1"
  private val BlackKnightB8Fen =
    "1n2k3/8/8/8/8/3P4/8/4K3 b - - 0 1"
  private val BlackKnightG8Fen =
    "4k1n1/8/8/8/8/8/8/4K3 b - - 0 1"
  private val WhiteKnightB1Fen =
    "4k3/8/8/8/8/8/8/1N2K3 w - - 0 1"
  private val WhiteKnightF1Fen =
    "4k3/8/8/8/8/8/8/4KN2 w - - 0 1"
  private val WhiteKnightG1Fen =
    "4k3/8/8/8/8/8/8/4K1N1 w - - 0 1"

  test("loads knight routes from TSV and verifies the played route plus PV continuation") {
    val route =
      OpeningRouteCatalog
        .fromTsvLines(
          List(
            "route_id\tfamily\ttarget_square\trole\tfrom\tvia\tto\tmax_replay_plies",
            "benoni_d6_knight_route\tbenoni\td6\tknight\tf3\td2\tc4\t8"
          )
        )
        .route("benoni_d6_knight_route")
        .getOrElse(fail("expected route row"))

    val evidence =
      KnightRouteEvidence.evaluate(
        fen = BenoniFen,
        playedUci = Some("f3d2"),
        pvMoves = List("f3d2", "e8e7", "d2c4"),
        route = route
      )

    assertEquals(evidence.map(_.playedMove), Some("f3d2"), clue(evidence))
    assertEquals(evidence.toList.flatMap(_.pvMoves).take(3), List("f3d2", "e8e7", "d2c4"), clue(evidence))
  }

  test("fails closed when a route descriptor asks for the wrong role or via square") {
    val catalog =
      OpeningRouteCatalog.fromTsvLines(
        List(
          "route_id\tfamily\ttarget_square\trole\tfrom\tvia\tto\tmax_replay_plies",
          "wrong_role\tbenoni\td6\tbishop\tf3\td2\tc4\t8",
          "wrong_via\tbenoni\td6\tknight\tf3\te1\tc4\t8"
        )
      )

    assertEquals(
      KnightRouteEvidence.evaluate(BenoniFen, Some("f3d2"), List("f3d2", "e8e7", "d2c4"), catalog.route("wrong_role").get),
      None,
      clue(catalog)
    )
    assertEquals(
      KnightRouteEvidence.evaluate(BenoniFen, Some("f3d2"), List("f3d2", "e8e7", "d2c4"), catalog.route("wrong_via").get),
      None,
      clue(catalog)
    )
  }

  test("reuses the same evaluator for black knight routes through d7 to c5") {
    val catalog =
      OpeningRouteCatalog.fromTsvLines(
        List(
          "route_id\tfamily\ttarget_square\trole\tfrom\tvia\tto\tmax_replay_plies",
          "reversed_benoni_d3_knight_route_f6\tbenoni\td3\tknight\tf6\td7\tc5\t8",
          "reversed_benoni_d3_knight_route_b8\tbenoni\td3\tknight\tb8\td7\tc5\t8",
          "kings_indian_c5_knight_route\tkings_indian\tc5\tknight\tf6\td7\tc5\t8"
        )
      )

    assertEquals(
      KnightRouteEvidence.evaluate(
        BlackKnightF6Fen,
        Some("f6d7"),
        List("f6d7", "e1e2", "d7c5"),
        catalog.route("reversed_benoni_d3_knight_route_f6").get
      ).map(_.route.routeId),
      Some("reversed_benoni_d3_knight_route_f6")
    )
    assertEquals(
      KnightRouteEvidence.evaluate(
        BlackKnightB8Fen,
        Some("b8d7"),
        List("b8d7", "e1e2", "d7c5"),
        catalog.route("reversed_benoni_d3_knight_route_b8").get
      ).map(_.route.routeId),
      Some("reversed_benoni_d3_knight_route_b8")
    )
    assertEquals(
      KnightRouteEvidence.evaluate(
        BlackKnightF6Fen,
        Some("f6d7"),
        List("f6d7", "e1e2", "d7c5"),
        catalog.route("kings_indian_c5_knight_route").get
      ).map(_.route.routeId),
      Some("kings_indian_c5_knight_route")
    )
  }

  test("default TSV exposes reversed Benoni and King's Indian route descriptors without code changes") {
    assertEquals(
      OpeningRouteCatalog.default.route("reversed_benoni_d3_knight_route_f6").map(_.path),
      Some(List("f6", "d7", "c5"))
    )
    assertEquals(
      OpeningRouteCatalog.default.route("reversed_benoni_d3_knight_route_b8").map(_.path),
      Some(List("b8", "d7", "c5"))
    )
    assertEquals(
      OpeningRouteCatalog.default.route("kings_indian_c5_knight_route").map(route => route.family -> route.path),
      Some("kings_indian" -> List("f6", "d7", "c5"))
    )
  }

  test("route descriptors carry target modes for board-level interpretation") {
    assertEquals(
      OpeningRouteCatalog.default.route("benoni_d6_knight_route").map(_.targetMode),
      Some("attack_weak_pawn")
    )
    assertEquals(
      OpeningRouteCatalog.default.route("reversed_benoni_d3_knight_route_f6").map(_.targetMode),
      Some("attack_weak_pawn")
    )
    assertEquals(
      OpeningRouteCatalog.default.route("kings_indian_c5_knight_route").map(_.targetMode),
      Some("occupy_target")
    )
  }

  test("default TSV exposes a starter pack of major opening knight routes") {
    val expected =
      Map(
        "caro_kann_b8_d7_f6_d5" -> ("caro_kann", List("b8", "d7", "f6", "d5")),
        "caro_kann_g8_e7_f5" -> ("caro_kann", List("g8", "e7", "f5")),
        "french_g8_e7_g6_f4" -> ("french", List("g8", "e7", "g6", "f4")),
        "french_b1_c3_e2_g3_f5" -> ("french", List("b1", "c3", "e2", "g3", "f5")),
        "open_games_b1_d2_f1_g3_f5" -> ("open_games", List("b1", "d2", "f1", "g3", "f5")),
        "open_games_f1_e3_d5" -> ("open_games", List("f1", "e3", "d5")),
        "gruenfeld_g8_f6_d5_c3" -> ("gruenfeld", List("g8", "f6", "d5", "c3")),
        "reti_g1_f3_d4_b5" -> ("reti", List("g1", "f3", "d4", "b5")),
        "alekhine_g8_f6_d5_b6" -> ("alekhine", List("g8", "f6", "d5", "b6")),
        "nimzowitsch_b8_c6_e5_g6" -> ("nimzowitsch", List("b8", "c6", "e5", "g6"))
      )

    expected.foreach { case (routeId, (family, path)) =>
      assertEquals(
        OpeningRouteCatalog.default.route(routeId).map(route => route.family -> route.path),
        Some(family -> path),
        clue(routeId)
      )
    }
  }

  test("starter pack routes replay across black and white coordinates") {
    val cases =
      List(
        (BlackKnightB8Fen, "b8d7", List("b8d7", "e1e2", "d7f6", "e2e1", "f6d5"), "caro_kann_b8_d7_f6_d5"),
        (BlackKnightG8Fen, "g8e7", List("g8e7", "e1e2", "e7f5"), "caro_kann_g8_e7_f5"),
        (BlackKnightG8Fen, "g8e7", List("g8e7", "e1e2", "e7g6", "e2e1", "g6f4"), "french_g8_e7_g6_f4"),
        (WhiteKnightB1Fen, "b1c3", List("b1c3", "e8e7", "c3e2", "e7e8", "e2g3", "e8e7", "g3f5"), "french_b1_c3_e2_g3_f5"),
        (WhiteKnightB1Fen, "b1d2", List("b1d2", "e8e7", "d2f1", "e7e8", "f1g3", "e8e7", "g3f5"), "open_games_b1_d2_f1_g3_f5"),
        (WhiteKnightF1Fen, "f1e3", List("f1e3", "e8e7", "e3d5"), "open_games_f1_e3_d5"),
        (BlackKnightG8Fen, "g8f6", List("g8f6", "e1e2", "f6d5", "e2e1", "d5c3"), "gruenfeld_g8_f6_d5_c3"),
        (WhiteKnightG1Fen, "g1f3", List("g1f3", "e8e7", "f3d4", "e7e8", "d4b5"), "reti_g1_f3_d4_b5"),
        (BlackKnightG8Fen, "g8f6", List("g8f6", "e1e2", "f6d5", "e2e1", "d5b6"), "alekhine_g8_f6_d5_b6"),
        (BlackKnightB8Fen, "b8c6", List("b8c6", "e1e2", "c6e5", "e2e1", "e5g6"), "nimzowitsch_b8_c6_e5_g6")
      )

    cases.foreach { case (fen, played, pv, routeId) =>
      assertEquals(
        OpeningRouteCatalog.default.route(routeId).flatMap(route => KnightRouteEvidence.evaluate(fen, Some(played), pv, route)).map(_.route.routeId),
        Some(routeId),
        clue(routeId)
      )
    }
  }
