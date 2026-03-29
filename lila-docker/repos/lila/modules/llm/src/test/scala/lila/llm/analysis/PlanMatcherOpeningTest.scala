package lila.llm.analysis

import chess.{ Color, File, Knight, Queen, Rook, Square }
import lila.llm.analysis.L3.*
import lila.llm.analysis.ThemeTaxonomy.ThemeL1
import lila.llm.model.{
  ContextHeader,
  ExplorerMove,
  NarrativeContext,
  NarrativeRenderMode,
  NarrativeSummary,
  OpeningEvent,
  OpeningEventBudget,
  OpeningReference,
  PawnPlayTable,
  PhaseContext,
  PlanId,
  PlanRow,
  PlanTable,
  ThreatTable,
  Motif
}
import munit.FunSuite

class PlanMatcherOpeningTest extends FunSuite:

  private def openingClassification: PositionClassification =
    PositionClassification(
      nature = NatureResult(NatureType.Static, 1, 0, 0, lockedCenter = false),
      criticality = CriticalityResult(CriticalityType.Normal, 0, None, 0),
      choiceTopology = ChoiceTopologyResult(ChoiceTopologyType.StyleChoice, 20, 10, Some(0), 10, 20, None),
      gamePhase = GamePhaseResult(GamePhaseType.Opening, totalMaterial = 76, queensOnBoard = true, minorPiecesCount = 8),
      simplifyBias = SimplifyBiasResult(isSimplificationWindow = false, evalAdvantage = 0, isEndgameNear = false, exchangeAvailable = true),
      drawBias = DrawBiasResult(isDrawish = false, materialSymmetry = false, oppositeColorBishops = false, fortressLikely = false, insufficientMaterial = false),
      riskProfile = RiskProfileResult(RiskLevel.Medium, evalVolatility = 0, tacticalMotifsCount = 0, kingExposureSum = 0),
      taskMode = TaskModeResult(TaskModeType.ExplainPlan, "opening")
    )

  private def openingFeatures(devLag: Int): PositionFeatures =
    val empty = PositionFeatures.empty
    empty.copy(
      activity = empty.activity.copy(whiteDevelopmentLag = devLag),
      centralSpace = empty.centralSpace.copy(
        whiteCentralPawns = 2,
        blackCentralPawns = 1,
        whiteCenterControl = 4,
        blackCenterControl = 2
      ),
      materialPhase = empty.materialPhase.copy(phase = "opening")
    )

  private def immediateThreatToThem: ThreatAnalysis =
    val threat = Threat(
      kind = ThreatKind.Material,
      lossIfIgnoredCp = 320,
      turnsToImpact = 1,
      motifs = List("fork"),
      attackSquares = List("f7"),
      targetPieces = List("queen"),
      bestDefense = Some("Qe7"),
      defenseCount = 1
    )
    ThreatAnalysis(
      threats = List(threat),
      defense = DefenseAssessment(
        necessity = ThreatSeverity.Important,
        onlyDefense = Some("Qe7"),
        alternatives = Nil,
        counterIsBetter = false,
        prophylaxisNeeded = false,
        resourceCoverageScore = 40,
        notes = "forcing defense required"
      ),
      threatSeverity = ThreatSeverity.Important,
      immediateThreat = true,
      strategicThreat = false,
      threatIgnorable = false,
      defenseRequired = true,
      counterThreatBetter = false,
      prophylaxisNeeded = false,
      resourceAvailable = true,
      maxLossIfIgnored = 320,
      primaryDriver = "material_threat",
      insufficientData = false
    )

  private def openingNarrativeContext(
      fen: String,
      playedMove: String,
      playedSan: String
  ): NarrativeContext =
    NarrativeContext(
      fen = fen,
      header = ContextHeader("Opening", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      ply = 6,
      playedMove = Some(playedMove),
      playedSan = Some(playedSan),
      summary = NarrativeSummary("Development", None, "NarrowChoice", "Maintain", "+0.10"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(
        top5 = List(PlanRow(rank = 1, name = "Development", score = 0.6, evidence = List("opening setup"))),
        suppressed = Nil
      ),
      delta = None,
      phase = PhaseContext("Opening", "Opening structure"),
      candidates = Nil,
      renderMode = NarrativeRenderMode.Bookmaker
    )

  test("opening phase should emit OpeningDevelopment plan with opening theme") {
    val motifs = List(
      Motif.Centralization(Knight, Square.C3, Color.White, 0, Some("Nc3")),
      Motif.PawnAdvance(File.E, 2, 4, Color.White, 0, Some("e4")),
      Motif.Castling(Motif.CastlingSide.Kingside, Color.White, 0, Some("O-O"))
    )

    val ctx = IntegratedContext(
      evalCp = 20,
      classification = Some(openingClassification),
      isWhiteToMove = true,
      features = Some(openingFeatures(devLag = 2))
    )

    val result = PlanMatcher.matchPlans(motifs, ctx, Color.White)
    val openingPlan = result.topPlans.find(_.plan.id == PlanId.OpeningDevelopment)

    assert(openingPlan.nonEmpty, "OpeningDevelopment should be present in opening phase")
    assert(
      openingPlan.exists(_.supports.contains(s"theme:${ThemeL1.OpeningPrinciples.id}")),
      "OpeningDevelopment should use opening_principles theme"
    )
  }

  test("sharp opening with forcing threat should prioritize tactical plan over OpeningDevelopment") {
    val motifs = List(
      Motif.Centralization(Knight, Square.C3, Color.White, 0, Some("Nc3")),
      Motif.PawnAdvance(File.E, 2, 4, Color.White, 0, Some("e4")),
      Motif.Check(Queen, Square.E8, Motif.CheckType.Normal, Color.White, 0, Some("Qh5+")),
      Motif.Fork(Knight, List(Queen, Rook), Square.F7, List(Square.D8, Square.H8), Color.White, 0, Some("Nf7")),
      Motif.Capture(Knight, Queen, Square.D6, Motif.CaptureType.Winning, Color.White, 0, Some("Nxd6"))
    )

    val ctx = IntegratedContext(
      evalCp = 40,
      classification = Some(openingClassification),
      threatsToThem = Some(immediateThreatToThem),
      isWhiteToMove = true,
      features = Some(openingFeatures(devLag = 1))
    )

    val result = PlanMatcher.matchPlans(motifs, ctx, Color.White)
    val top = result.topPlans.headOption.getOrElse(fail("Expected at least one top plan"))

    assertNotEquals(top.plan.id, PlanId.OpeningDevelopment)
    assert(
      top.supports.contains(s"theme:${ThemeL1.ImmediateTacticalGain.id}"),
      "forcing tactical context should be ranked as immediate_tactical_gain"
    )
  }

  test("opening concept distinguishes flank fianchetto support inside existing opening goals") {
    val ctx =
      openingNarrativeContext(
        fen = "rnbqkb1r/pppppppp/5n2/8/3P4/6P1/PPP1PP1P/RNBQKBNR b KQkq - 0 2",
        playedMove = "g2g3",
        playedSan = "g3"
      )

    val evaluation = OpeningGoals.analyze(ctx).getOrElse(fail("expected opening goal"))
    assertEquals(evaluation.goalName, "Flank Fianchetto Support")
    assert(Set(OpeningGoals.Status.Achieved, OpeningGoals.Status.Partial).contains(evaluation.status), clue(evaluation))
  }

  test("intro event theme recognizes Scandinavian as early queen exposure") {
    val ref = OpeningReference(
      eco = Some("B01"),
      name = Some("Scandinavian Defense"),
      totalGames = 600,
      topMoves = List(
        ExplorerMove("d8d5", "Qxd5", 180, 65, 50, 65, 2580),
        ExplorerMove("b8c6", "Nc6", 150, 58, 42, 50, 2570)
      ),
      sampleGames = Nil
    )

    val event = OpeningEventDetector.detectIntro(ply = 4, ref = ref, budget = OpeningEventBudget()).getOrElse(fail("expected intro"))
    event match
      case OpeningEvent.Intro(_, _, theme, _) =>
        assertEquals(theme, "early queen exposure")
      case other => fail(s"expected intro, got: $other")
  }

  test("branch-point event reason upgrades to development logic when theory pivots to minor-piece routes") {
    val prevRef = OpeningReference(
      eco = Some("C50"),
      name = Some("Italian Game"),
      totalGames = 1000,
      topMoves = List(
        ExplorerMove("e2e4", "e4", 700, 320, 200, 180, 2650)
      ),
      sampleGames = Nil
    )
    val currRef = OpeningReference(
      eco = Some("C50"),
      name = Some("Italian Game"),
      totalGames = 1000,
      topMoves = List(
        ExplorerMove("g1f3", "Nf3", 260, 110, 90, 60, 2640),
        ExplorerMove("b1c3", "Nc3", 230, 95, 80, 55, 2630),
        ExplorerMove("f1b5", "Bb5", 180, 75, 60, 45, 2620)
      ),
      sampleGames = Nil
    )

    val event = OpeningEventDetector.detectBranchPoint(currRef, Some(prevRef), OpeningEventBudget()).getOrElse(fail("expected branch point"))
    event match
      case OpeningEvent.BranchPoint(_, reason, _) =>
        assert(reason.toLowerCase.contains("development logic"), clue(reason))
      case other => fail(s"expected branch point, got: $other")
  }
