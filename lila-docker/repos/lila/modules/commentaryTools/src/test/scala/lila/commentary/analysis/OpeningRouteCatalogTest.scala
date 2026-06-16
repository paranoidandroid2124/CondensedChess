package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.{
  DirectionalTargetReadiness,
  StrategicIdeaGroup,
  StrategicIdeaKind,
  StrategicIdeaReadiness,
  StrategyDirectionalTarget,
  StrategyIdeaSignal,
  StrategyPack
}
import lila.commentary.model.*
import lila.commentary.model.strategic.{ EngineEvidence, PvMove, VariationLine }

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
  private val WhiteBishopF1Fen =
    "4k3/8/8/8/8/6P1/8/4KB2 w - - 0 1"
  private val WhiteBishopBlockedFen =
    "4k3/8/8/8/8/8/6P1/4KB2 w - - 0 1"
  private val BlackBishopF8Fen =
    "4kb2/8/6p1/8/8/8/8/4K3 b - - 0 1"
  private val WhiteRookH1Fen =
    "4k3/8/8/8/8/8/8/4K2R w - - 0 1"

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

  test("route evidence from context uses raw engine PV before stale parsed metadata") {
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
    val ctx =
      baseContext(
        fen = BenoniFen,
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("f3d2", "e8e7", "d2c4"),
                scoreCp = 40,
                parsedMoves = List(
                  PvMove("f3h4", "Nh4", "f3", "h4", "N", isCapture = false, capturedPiece = None, givesCheck = false),
                  PvMove("e8e7", "Ke7", "e8", "e7", "k", isCapture = false, capturedPiece = None, givesCheck = false),
                  PvMove("h4f5", "Nf5", "h4", "f5", "N", isCapture = false, capturedPiece = None, givesCheck = false)
                )
              )
            )
          )
        )
      )

    val evidence = KnightRouteEvidence.fromContext(ctx, route)

    assertEquals(evidence.map(_.playedMove), Some("f3d2"), clue(evidence))
    assertEquals(evidence.map(_.terminalPosition.board.pieceAt(_root_.chess.Square.C4).map(_.role)), Some(Some(_root_.chess.Knight)))
  }

  test("route target-mode board check uses the replayed terminal board") {
    val route =
      OpeningRouteCatalog.default.route("benoni_d6_knight_route").getOrElse(fail("missing Benoni route"))
    val evidence =
      KnightRouteEvidence
        .evaluate(BenoniFen, Some("f3d2"), List("f3d2", "e8e7", "d2c4"), route)
        .getOrElse(fail("route should replay"))
    val rootCheck =
      _root_.chess.format.Fen
        .read(_root_.chess.variant.Standard, _root_.chess.format.Fen.Full(BenoniFen))
        .exists(position => OpeningRouteTargetEvidence.checkRouteBoard(position.board, position.color, route))

    assertEquals(rootCheck, false)
    assertEquals(OpeningRouteTargetEvidence.checkRouteEvidence(evidence), true)
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
    val benoniRoute = OpeningRouteCatalog.default.route("benoni_d6_knight_route").getOrElse(fail("missing Benoni route"))
    val kingsIndianRoute =
      OpeningRouteCatalog.default.route("kings_indian_c5_knight_route").getOrElse(fail("missing King's Indian route"))

    assertEquals(
      benoniRoute.targetMode,
      "attack_weak_pawn"
    )
    assertEquals(
      OpeningRouteCatalog.default.route("reversed_benoni_d3_knight_route_f6").map(_.targetMode),
      Some("attack_weak_pawn")
    )
    assertEquals(
      kingsIndianRoute.targetMode,
      "occupy_target"
    )
    assert(OpeningRouteTargetEvidence.ownerSeedTerms(benoniRoute).contains("fixed_target:d6"))
    assert(!OpeningRouteTargetEvidence.ownerSeedTerms(kingsIndianRoute).exists(_.startsWith("fixed_target:")))
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

  test("direct Queen's Indian and Bogo-Indian Nf6-e4 routes replay from post-Nf6 positions") {
    val cases =
      List(
        "queens_indian_f6_e4" -> "queens_indian",
        "bogo_indian_f6_e4" -> "bogo_indian"
      )

    cases.foreach { case (routeId, family) =>
      val route = OpeningRouteCatalog.default.route(routeId).getOrElse(fail(s"missing $routeId"))
      val evidence = KnightRouteEvidence.evaluate(BlackKnightF6Fen, Some("f6e4"), List("f6e4"), route)

      assertEquals(
        route.family -> route.targetSquare -> route.path -> route.targetMode,
        family -> "e4" -> List("f6", "e4") -> "occupy_target"
      )
      assertEquals(evidence.map(_.route.routeId), Some(routeId), clue(route))
      assertEquals(evidence.exists(OpeningRouteTargetEvidence.checkRouteEvidence), true, clue(evidence))
    }
  }

  test("opening route witness family admission follows current opening label") {
    val queenIndianCtx =
      baseContext(BlackKnightF6Fen, None).copy(
        openingData = Some(
          OpeningReference(
            eco = Some("E12"),
            name = Some("Queen's Indian Defense"),
            totalGames = 100,
            topMoves = Nil,
            sampleGames = Nil
          )
        )
      )
    val bogoEventCtx =
      baseContext(BlackKnightF6Fen, None).copy(
        openingEvent = Some(OpeningEvent.Intro("E11", "Bogo-Indian Defense", "e4 outpost", Nil))
      )
    val unlabeledCtx = baseContext(BlackKnightF6Fen, None)
    val queenIndianRoute =
      OpeningRouteCatalog.default.route("queens_indian_f6_e4").getOrElse(fail("missing Queen's Indian route"))
    val bogoRoute = OpeningRouteCatalog.default.route("bogo_indian_f6_e4").getOrElse(fail("missing Bogo-Indian route"))
    val dutchRoute = OpeningRouteCatalog.default.route("dutch_g8_f6_e4").getOrElse(fail("missing Dutch route"))

    assertEquals(PlayerFacingTruthModePolicy.openingRouteFamilyAdmissible(queenIndianCtx, queenIndianRoute), true)
    assertEquals(PlayerFacingTruthModePolicy.openingRouteFamilyAdmissible(queenIndianCtx, bogoRoute), false)
    assertEquals(PlayerFacingTruthModePolicy.openingRouteFamilyAdmissible(queenIndianCtx, dutchRoute), false)
    assertEquals(PlayerFacingTruthModePolicy.openingRouteFamilyAdmissible(bogoEventCtx, bogoRoute), true)
    assertEquals(PlayerFacingTruthModePolicy.openingRouteFamilyAdmissible(bogoEventCtx, queenIndianRoute), false)
    assertEquals(PlayerFacingTruthModePolicy.openingRouteFamilyAdmissible(unlabeledCtx, dutchRoute), true)
  }

  test("occupy-target opening route does not become exact target fixation proof") {
    val ctx =
      baseContext(
        fen = BlackKnightF6Fen,
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("f6e4", "e1e2"),
                scoreCp = 32
              )
            )
          )
        )
      ).copy(
        playedMove = Some("f6e4"),
        playedSan = Some("Ne4"),
        openingData = Some(
          OpeningReference(
            eco = Some("E12"),
            name = Some("Queen's Indian Defense"),
            totalGames = 100,
            topMoves = Nil,
            sampleGames = Nil
          )
        )
      )
    val pack =
      StrategyPack(
        sideToMove = "black",
        directionalTargets =
          List(
            StrategyDirectionalTarget(
              targetId = "qid_e4",
              ownerSide = "black",
              piece = "knight",
              from = "f6",
              targetSquare = "e4",
              readiness = DirectionalTargetReadiness.Contested,
              strategicReasons = List("target fixing")
            )
          ),
        strategicIdeas =
          List(
            StrategyIdeaSignal(
              ideaId = "target_fixing_e4",
              ownerSide = "black",
              kind = StrategicIdeaKind.TargetFixing,
              group = StrategicIdeaGroup.StructuralChange,
              readiness = StrategicIdeaReadiness.Ready,
              focusSquares = List("e4"),
              confidence = 0.92,
              evidenceRefs = List("target_fixing:e4", "enemy_weak_square:e4")
            )
          )
      )

    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(ctx, StrategyPackSurface.from(Some(pack)), truthContract = None)

    assert(!delta.exists(_.packet.proofSource == PlayerFacingTruthModePolicy.ExactTargetFixationProofSource), clue(delta))
  }

  test("bishop fianchetto occupy-target route stays route evidence only") {
    val ctx =
      baseContext(
        fen = WhiteBishopF1Fen,
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("f1g2", "e8e7"),
                scoreCp = 24
              )
            )
          )
        )
      ).copy(
        playedMove = Some("f1g2"),
        playedSan = Some("Bg2"),
        openingData = Some(
          OpeningReference(
            eco = Some("E01"),
            name = Some("Catalan Opening"),
            totalGames = 100,
            topMoves = Nil,
            sampleGames = Nil
          )
        )
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        directionalTargets =
          List(
            StrategyDirectionalTarget(
              targetId = "catalan_g2",
              ownerSide = "white",
              piece = "bishop",
              from = "f1",
              targetSquare = "g2",
              readiness = DirectionalTargetReadiness.Contested,
              strategicReasons = List("target fixing")
            )
          ),
        strategicIdeas =
          List(
            StrategyIdeaSignal(
              ideaId = "target_fixing_g2",
              ownerSide = "white",
              kind = StrategicIdeaKind.TargetFixing,
              group = StrategicIdeaGroup.StructuralChange,
              readiness = StrategicIdeaReadiness.Ready,
              focusSquares = List("g2"),
              confidence = 0.92,
              evidenceRefs = List("target_fixing:g2", "weak_complex:g2")
            )
          )
      )

    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(ctx, StrategyPackSurface.from(Some(pack)), truthContract = None)

    assert(!delta.exists(_.packet.proofSource == PlayerFacingTruthModePolicy.ExactTargetFixationProofSource), clue(delta))
  }

  test("piece route evidence supports bishop fianchetto and rook-lift descriptors") {
    val catalog =
      OpeningRouteCatalog.fromTsvLines(
        List(
          "route_id\tfamily\ttarget_square\trole\tfrom\tvia\tto\tmax_replay_plies\ttarget_mode",
          "catalan_f1_g2_bishop\tcatalan\tg2\tbishop\tf1\t\tg2\t4\toccupy_target",
          "rook_lift_h1_h3\topen_games\th3\trook\th1\t\th3\t4\toccupy_target"
        )
      )

    assertEquals(
      PieceRouteEvidence.evaluate(
        WhiteBishopF1Fen,
        Some("f1g2"),
        List("f1g2", "e8e7"),
        catalog.route("catalan_f1_g2_bishop").get
      ).map(_.route.routeId),
      Some("catalan_f1_g2_bishop")
    )
    assertEquals(
      PieceRouteEvidence.evaluate(
        WhiteRookH1Fen,
        Some("h1h3"),
        List("h1h3", "e8e7"),
        catalog.route("rook_lift_h1_h3").get
      ).map(_.route.routeId),
      Some("rook_lift_h1_h3")
    )
  }

  test("sliding piece routes fail closed when a blocker prevents legal replay") {
    val route =
      OpeningRouteCatalog
        .fromTsvLines(
          List(
            "route_id\tfamily\ttarget_square\trole\tfrom\tvia\tto\tmax_replay_plies\ttarget_mode",
            "catalan_f1_g2_bishop\tcatalan\tg2\tbishop\tf1\t\tg2\t4\toccupy_target"
          )
        )
        .route("catalan_f1_g2_bishop")
        .getOrElse(fail("expected route"))

    assertEquals(
      PieceRouteEvidence.evaluate(WhiteBishopBlockedFen, Some("f1g2"), List("f1g2"), route),
      None
    )
  }

  test("default TSV exposes bishop fianchetto descriptors with target allowlists") {
    val expected =
      Map(
        "catalan_f1_g2_bishop" -> ("catalan", "g2", WhiteBishopF1Fen, "f1g2"),
        "english_f1_g2_bishop" -> ("english", "g2", WhiteBishopF1Fen, "f1g2"),
        "kings_indian_f8_g7_bishop" -> ("kings_indian", "g7", BlackBishopF8Fen, "f8g7"),
        "queens_indian_c8_b7_bishop" -> ("queens_indian", "b7", "2b1k3/8/1p6/8/8/8/8/4K3 b - - 0 1", "c8b7")
      )

    expected.foreach { case (routeId, (family, target, fen, played)) =>
      val route = OpeningRouteCatalog.default.route(routeId).getOrElse(fail(s"missing $routeId"))
      assertEquals(route.family -> route.targetSquare -> route.role, family -> target -> "bishop", clue(route))
      assert(OpeningFamilyCatalog.default.targetAllowed(family, target), clue(route))
      assertEquals(PieceRouteEvidence.evaluate(fen, Some(played), List(played), route).map(_.route.routeId), Some(routeId), clue(route))
    }
  }

  private def baseContext(
      fen: String,
      engineEvidence: Option[EngineEvidence]
  ): NarrativeContext =
    NarrativeContext(
      fen = fen,
      header = ContextHeader("Middlegame", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      ply = 1,
      summary = NarrativeSummary("Route", None, "NarrowChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "Balanced middlegame"),
      candidates = Nil,
      engineEvidence = engineEvidence,
      renderMode = NarrativeRenderMode.MoveReview
    )
