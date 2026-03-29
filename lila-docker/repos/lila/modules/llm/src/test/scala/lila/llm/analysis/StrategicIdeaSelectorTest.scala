package lila.llm.analysis

import chess.{ Bishop, Board, Color, Queen, Square }
import chess.format.Fen
import chess.variant.Standard
import lila.llm.*
import lila.llm.model.{ Motif, Plan, PlanMatch, StrategicPlanExperiment }
import lila.llm.model.strategic.{ PositionalTag, PreventedPlan }
import munit.FunSuite

class StrategicIdeaSelectorTest extends FunSuite:

  private def selectFromFen(
      fen: String,
      phase: String = "middlegame"
  ): List[StrategyIdeaSignal] =
    val board =
      Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"invalid FEN: $fen"))
    val data =
      CommentaryEngine
        .assessExtended(
          fen = fen,
          variations = List(lila.llm.model.strategic.VariationLine(Nil, 0, depth = 0)),
          phase = Some(phase),
          ply = 24
        )
        .getOrElse(fail(s"analysis missing for $fen"))
    val ctx = NarrativeContextBuilder.build(data, data.toContext, None)
    val semantic = StrategicIdeaSemanticContext.from(data, ctx, Some(board))
    val pack = StrategyPackBuilder.build(data, ctx).getOrElse(fail(s"strategy pack missing for $fen"))
    StrategicIdeaSelector.select(pack, semantic)

  private def noisyPack(): StrategyPack =
    StrategyPack(
      sideToMove = "white",
      plans = List(
        StrategySidePlan(
          side = "white",
          horizon = "long",
          planName = "Build kingside clamp and space gain"
        )
      ),
      longTermFocus = List("misleading long-term focus about exchange play"),
      signalDigest = Some(
        NarrativeSignalDigest(
          structuralCue = Some("misleading space gain text"),
          latentPlan = Some("misleading favorable exchange text"),
          decision = Some("misleading prophylaxis text"),
          prophylaxisPlan = Some("misleading prevent text"),
          opponentPlan = Some("misleading counterplay text")
        )
      )
    )

  private def boardFromFen(fen: String): Board =
    Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"invalid FEN: $fen"))

  private def planMatch(plan: Plan, score: Double): PlanMatch =
    PlanMatch(plan = plan, score = score, evidence = Nil)

  private def compensationFeatures(
      fen: String,
      materialDiff: Int,
      whiteDevelopmentLag: Int,
      blackDevelopmentLag: Int,
      whiteAttackersCount: Int = 0,
      blackAttackersCount: Int = 0,
      blackKingRingAttacked: Int = 0,
      blackKingExposedFiles: Int = 0,
      blackCastledSide: String = "none",
      whiteSemiOpenFiles: Int,
      openFilesCount: Int,
      spaceDiff: Int,
      phase: String = "opening"
  ): PositionFeatures =
    PositionFeatures.empty.copy(
      fen = fen,
      sideToMove = "white",
      activity = PositionFeatures.empty.activity.copy(
        whiteDevelopmentLag = whiteDevelopmentLag,
        blackDevelopmentLag = blackDevelopmentLag
      ),
      kingSafety = PositionFeatures.empty.kingSafety.copy(
        whiteCastledSide = "short",
        blackCastledSide = blackCastledSide,
        whiteAttackersCount = whiteAttackersCount,
        blackAttackersCount = blackAttackersCount,
        blackKingRingAttacked = blackKingRingAttacked,
        blackKingExposedFiles = blackKingExposedFiles
      ),
      materialPhase = PositionFeatures.empty.materialPhase.copy(
        whiteMaterial = 37,
        blackMaterial = 37 - materialDiff,
        materialDiff = materialDiff,
        phase = phase
      ),
      lineControl = PositionFeatures.empty.lineControl.copy(
        openFilesCount = openFilesCount,
        whiteSemiOpenFiles = whiteSemiOpenFiles
      ),
      centralSpace = PositionFeatures.empty.centralSpace.copy(spaceDiff = spaceDiff)
    )

  test("typed selector ignores text-only ambiguity without typed evidence") {
    val pack = noisyPack()

    val typed = StrategicIdeaSelector.select(pack, StrategicIdeaSemanticContext.empty("white"))

    assertEquals(typed, Nil)
  }

  test("refuted experiment blocks matching king-attack idea") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        positionalFeatures = List(PositionalTag.MateNet(Color.White)),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "flank_attack",
            themeL1 = ThemeTaxonomy.ThemeL1.FlankInfrastructure.id,
            subplanId = Some(ThemeTaxonomy.SubplanId.HookCreation.id),
            evidenceTier = "refuted",
            refuteProbeCount = 1,
            moveOrderSensitive = true,
            experimentConfidence = 0.08
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)

    assertEquals(ideas, Nil)
  }

  test("pv-coupled move-order-sensitive experiment demotes but does not erase slow space idea") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        positionalFeatures = List(PositionalTag.SpaceAdvantage(Color.White)),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "space_clamp",
            themeL1 = ThemeTaxonomy.ThemeL1.SpaceClamp.id,
            subplanId = Some(ThemeTaxonomy.SubplanId.CentralSpaceBind.id),
            evidenceTier = "pv_coupled",
            moveOrderSensitive = true,
            experimentConfidence = 0.52
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.SpaceGainOrRestriction))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("experiment:space_clamp")))
  }

  test("evidence-backed experiment adds experiment refs to surviving outpost idea") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        positionalFeatures = List(PositionalTag.Outpost(Square.E5, Color.White)),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "outpost_plan",
            themeL1 = ThemeTaxonomy.ThemeL1.PieceRedeployment.id,
            subplanId = Some(ThemeTaxonomy.SubplanId.OutpostEntrenchment.id),
            evidenceTier = "evidence_backed",
            supportProbeCount = 1,
            bestReplyStable = true,
            futureSnapshotAligned = true,
            experimentConfidence = 0.91
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.OutpostCreationOrOccupation))
    assert(ideas.headOption.exists(_.evidenceRefs.exists(_ == "experiment:piece_redeployment")))
    assert(ideas.headOption.exists(_.evidenceRefs.exists(_ == "experiment_subplan:outpost_entrenchment")))
  }

  test("family-first staging keeps concrete pawn break ahead of slow structural noise") {
    val ideas =
      selectFromFen("r2qk2r/pp1bbppp/2n1p3/3pPn2/NP1P4/P2B1N2/1B3PPP/R2QK2R b KQkq - 8 12")

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.PawnBreak))
  }

  test("family-first staging keeps IQP trade-down windows in the conversion family") {
    val ideas =
      selectFromFen("r1bqr1k1/pp2bpp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12")

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.FavorableTradeOrTransformation))
  }

  test("compensation development lead promotes king-attack build-up over broad space") {
    val fen = "r4rk1/ppp2ppp/2n5/3p4/3P4/2NB1Q2/PPP2PPP/R1B2RK1 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen(fen)),
        positionalFeatures = List(PositionalTag.SpaceAdvantage(Color.White)),
        plans = List(
          planMatch(Plan.OpeningDevelopment(Color.White), 0.82),
          planMatch(Plan.KingsideAttack(Color.White), 0.79),
          planMatch(Plan.SpaceAdvantage(Color.White), 0.77)
        ),
        motifs = List(
          Motif.Battery(
            front = Bishop,
            back = Queen,
            axis = Motif.BatteryAxis.Diagonal,
            color = Color.White,
            plyIndex = 0,
            move = None,
            frontSq = Some(Square.D3),
            backSq = Some(Square.F3)
          )
        ),
        positionFeatures = Some(
          compensationFeatures(
            fen = fen,
            materialDiff = -2,
            whiteDevelopmentLag = 0,
            blackDevelopmentLag = 3,
            whiteAttackersCount = 2,
            blackKingRingAttacked = 2,
            blackKingExposedFiles = 1,
            whiteSemiOpenFiles = 1,
            openFilesCount = 1,
            spaceDiff = 2
          )
        ),
        phase = "opening"
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        pieceRoutes = List(
          StrategyPieceRoute(
            ownerSide = "white",
            piece = "B",
            from = "d3",
            route = List("h7"),
            purpose = "keep the king under pressure",
            strategicFit = 0.86,
            tacticalSafety = 0.76,
            surfaceConfidence = 0.86,
            surfaceMode = RouteSurfaceMode.Exact
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(pack, semantic)

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.KingAttackBuildUp))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("source:compensation_development_lead")))
  }

  test("compensation open lines keep line occupation ahead of generic space") {
    val fen = "r4rk1/2p2ppp/p1n5/1p1p4/3P4/5N2/PPQ2PPP/2R2RK1 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen(fen)),
        positionalFeatures = List(PositionalTag.SpaceAdvantage(Color.White)),
        plans = List(
          planMatch(Plan.OpeningDevelopment(Color.White), 0.80),
          planMatch(Plan.FileControl(Color.White, "c-file"), 0.78),
          planMatch(Plan.PieceActivation(Color.White), 0.75)
        ),
        positionFeatures = Some(
          compensationFeatures(
            fen = fen,
            materialDiff = -1,
            whiteDevelopmentLag = 0,
            blackDevelopmentLag = 2,
            blackCastledSide = "short",
            whiteSemiOpenFiles = 1,
            openFilesCount = 1,
            spaceDiff = 2
          )
        ),
        phase = "opening"
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        pieceRoutes = List(
          StrategyPieceRoute(
            ownerSide = "white",
            piece = "R",
            from = "c1",
            route = List("c7"),
            purpose = "occupy the c-file before recovering material",
            strategicFit = 0.84,
            tacticalSafety = 0.80,
            surfaceConfidence = 0.84,
            surfaceMode = RouteSurfaceMode.Exact
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(pack, semantic)

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.LineOccupation))
    assert(ideas.headOption.exists(_.evidenceRefs.exists(ref =>
      ref == "source:compensation_open_lines" || ref == "source:delayed_recovery_window"
    )))
  }

  test("compensation target fixation keeps Open Catalan pressure target-led") {
    val ideas =
      selectFromFen(
        "1rbqk2r/1pp1bppp/p3pn2/n7/P1pP4/4P1P1/1P1N1PBP/RNBQ1RK1 w k - 3 10",
        phase = "opening"
      )

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.TargetFixing))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("source:compensation_target_fixation")))
  }

  test("quiet compensation line pressure stays ahead of attack family when king window is weak") {
    val fen = "r1b2rk1/1pp2ppp/p1n5/3p4/1P1P4/2P2N2/P1Q2PPP/2R2RK1 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen(fen)),
        plans = List(
          planMatch(Plan.KingsideAttack(Color.White), 0.79),
          planMatch(Plan.FileControl(Color.White, "c-file"), 0.81),
          planMatch(Plan.PieceActivation(Color.White), 0.74)
        ),
        positionFeatures = Some(
          compensationFeatures(
            fen = fen,
            materialDiff = -1,
            whiteDevelopmentLag = 0,
            blackDevelopmentLag = 1,
            blackCastledSide = "short",
            whiteSemiOpenFiles = 1,
            openFilesCount = 1,
            spaceDiff = 1,
            phase = "middlegame"
          )
        ),
        phase = "middlegame"
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        pieceRoutes = List(
          StrategyPieceRoute(
            ownerSide = "white",
            piece = "R",
            from = "c1",
            route = List("c1", "c5"),
            purpose = "occupy the c-file before recovering material",
            strategicFit = 0.82,
            tacticalSafety = 0.79,
            surfaceConfidence = 0.83,
            surfaceMode = RouteSurfaceMode.Exact
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(pack, semantic)

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.LineOccupation))
    assert(ideas.headOption.forall(_.kind != StrategicIdeaKind.KingAttackBuildUp))
  }

  test("compensation counterplay denial produces suppression carrier instead of drifting to attack") {
    val fen = "r2q1rk1/1p2bppp/p1n1pn2/2pp4/3P4/1PN1PN2/PBQ2PPP/2RR2K1 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen(fen)),
        preventedPlans = List(
          PreventedPlan(
            planId = "DenyB5",
            deniedSquares = List(Square.B5, Square.C4),
            breakNeutralized = Some("b5"),
            mobilityDelta = -2,
            counterplayScoreDrop = 140,
            deniedResourceClass = Some("break")
          )
        ),
        plans = List(
          planMatch(Plan.Prophylaxis(Color.White, "b5 break"), 0.80),
          planMatch(Plan.FileControl(Color.White, "b-file"), 0.78)
        ),
        positionFeatures = Some(
          compensationFeatures(
            fen = fen,
            materialDiff = -1,
            whiteDevelopmentLag = 0,
            blackDevelopmentLag = 1,
            blackCastledSide = "short",
            whiteSemiOpenFiles = 1,
            openFilesCount = 1,
            spaceDiff = 1,
            phase = "middlegame"
          )
        ),
        phase = "middlegame"
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        pieceRoutes = List(
          StrategyPieceRoute(
            ownerSide = "white",
            piece = "R",
            from = "d1",
            route = List("d1", "b1", "b5"),
            purpose = "queenside pressure",
            strategicFit = 0.80,
            tacticalSafety = 0.76,
            surfaceConfidence = 0.80,
            surfaceMode = RouteSurfaceMode.Toward
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(pack, semantic)

    assert(ideas.exists(signal => signal.kind == StrategicIdeaKind.CounterplaySuppression))
    assert(ideas.exists(_.evidenceRefs.contains("source:compensation_counterplay_denial")))
    assert(ideas.headOption.forall(_.kind != StrategicIdeaKind.KingAttackBuildUp))
  }
