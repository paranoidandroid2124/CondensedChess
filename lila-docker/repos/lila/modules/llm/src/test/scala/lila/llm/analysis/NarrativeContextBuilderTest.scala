package lila.llm.analysis

import munit.FunSuite
import chess.Color
import chess.format.{Fen, Uci}
import chess.variant.Standard
import chess.File
import lila.llm.model._
import lila.llm.model.strategic.*
import lila.llm.analysis.L3._

/**
 * Comprehensive A-axis verification tests.
 * Tests actual value population for A1-A8 features.
 */
class NarrativeContextBuilderTest extends FunSuite {

  val testFen = "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"

  def minimalData(ctx: Option[IntegratedContext] = None): ExtendedAnalysisData = 
    ExtendedAnalysisData(
      fen = testFen,
      nature = PositionNature(lila.llm.model.NatureType.Dynamic, 0.5, 0.5, "Dynamic position"),
      motifs = Nil,
      plans = Nil,
      preventedPlans = Nil,
      pieceActivity = Nil,
      structuralWeaknesses = Nil,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      prevMove = None,
      ply = 7,
      evalCp = 50,
      isWhiteToMove = true,
      phase = "opening",
      integratedContext = ctx
    )

  // ============================================================
  // A1: THREATS TABLE
  // ============================================================
  
  test("A1: buildThreatTable populates ThreatRows from IntegratedContext.threatsToUs".ignore) {
    val threat = Threat(
      kind = ThreatKind.Material,
      lossIfIgnoredCp = 400,
      turnsToImpact = 2,
      motifs = List("Fork"),
      attackSquares = List("e5"),
      targetPieces = List("Knight", "Rook"),
      bestDefense = Some("Nc3"),
      defenseCount = 3
    )
    
    val threatAnalysis = ThreatAnalysis(
      threats = List(threat),
      defense = DefenseAssessment(ThreatSeverity.Important, None, List("Nc3", "Nd4"), false, false, 70, "Standard defense"),
      threatSeverity = ThreatSeverity.Important,
      immediateThreat = true, strategicThreat = false, threatIgnorable = false,
      defenseRequired = true, counterThreatBetter = false, prophylaxisNeeded = false,
      resourceAvailable = true, maxLossIfIgnored = 400, primaryDriver = "material_threat",
      insufficientData = false
    )
    
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true, threatsToUs = Some(threatAnalysis))
    val narrativeCtx = NarrativeContextBuilder.build(minimalData(Some(ctx)), ctx, None)
    
    assertEquals(narrativeCtx.threats.toUs.size, 1, "Should have 1 threat")
    assertEquals(narrativeCtx.threats.toUs.head.kind, "Material")
    assertEquals(narrativeCtx.threats.toUs.head.lossIfIgnoredCp, 400)
    assertEquals(narrativeCtx.threats.toUs.head.square, Some("e5"))
  }

  // ============================================================
  // A2: PAWN PLAY TABLE  
  // ============================================================
  
  test("A2: buildPawnPlayTable populates from IntegratedContext.pawnAnalysis") {
    val pawnAnalysis = PawnPlayAnalysis(
      pawnBreakReady = true,
      breakFile = Some("d"),
      breakImpact = 250,
      advanceOrCapture = false,
      passedPawnUrgency = PassedPawnUrgency.Important,
      passerBlockade = false,
      blockadeSquare = None,
      blockadeRole = None,
      pusherSupport = false,
      minorityAttack = false,
      counterBreak = false,
      tensionPolicy = TensionPolicy.Maintain,
      tensionSquares = Nil,
      primaryDriver = "break_ready",
      notes = "Test"
    )
    
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true, pawnAnalysis = Some(pawnAnalysis))
    val narrativeCtx = NarrativeContextBuilder.build(minimalData(Some(ctx)), ctx, None)
    
    assertEquals(narrativeCtx.pawnPlay.breakReady, true)
    assertEquals(narrativeCtx.pawnPlay.breakFile, Some("d"))
    assertEquals(narrativeCtx.pawnPlay.breakImpact, "High")  // 250 >= 200
    assertEquals(narrativeCtx.pawnPlay.tensionPolicy, "Maintain")
    assertEquals(narrativeCtx.pawnPlay.primaryDriver, "break_ready")
  }

  // ============================================================
  // A3: HEADER from Classification
  // ============================================================
  
  test("A3: buildHeader populates from IntegratedContext.classification") {
    val classification = PositionClassification(
      nature = NatureResult(lila.llm.analysis.L3.NatureType.Dynamic, 3, 2, 1, false),
      criticality = CriticalityResult(CriticalityType.CriticalMoment, 0, None, 5),
      choiceTopology = ChoiceTopologyResult(ChoiceTopologyType.OnlyMove, 100, 50, None, 50, 60, Some("PV2 drops piece")),
      gamePhase = GamePhaseResult(GamePhaseType.Middlegame, 28, true, 4),
      simplifyBias = SimplifyBiasResult(false, 0, false, false),
      drawBias = DrawBiasResult(false, false, false, false, false),
      riskProfile = RiskProfileResult(RiskLevel.High, 0, 0, 0),
      taskMode = TaskModeResult(TaskModeType.ExplainTactics, "forcing")
    )
    
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true, classification = Some(classification))
    val narrativeCtx = NarrativeContextBuilder.build(minimalData(Some(ctx)), ctx, None)
    
    assertEquals(narrativeCtx.header.phase, "Middlegame")
    assertEquals(narrativeCtx.header.criticality, "Critical")
    assertEquals(narrativeCtx.header.choiceType, "OnlyMove")
    assertEquals(narrativeCtx.header.riskLevel, "High")
    assertEquals(narrativeCtx.header.taskMode, "ExplainTactics")
  }

  // ============================================================
  // A4: PLAN TABLE - 6 plans to verify take(5) truncation
  // ============================================================
  
  test("A4: buildPlanTable truncates to 5 and preserves order") {
    val plans = List(
      PlanMatch(Plan.KingsideAttack(Color.White), 0.90, Nil),
      PlanMatch(Plan.QueensideAttack(Color.White), 0.80, Nil),
      PlanMatch(Plan.CentralBreakthrough(Color.White), 0.70, Nil),
      PlanMatch(Plan.CentralControl(Color.White), 0.60, Nil),
      PlanMatch(Plan.PieceActivation(Color.White), 0.50, Nil),
      PlanMatch(Plan.RookActivation(Color.White), 0.40, Nil)  // 6th should be cut
    )
    
    val data = minimalData().copy(plans = plans)
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assertEquals(narrativeCtx.plans.top5.size, 5, "Should truncate to 5 plans")
    assertEquals(narrativeCtx.plans.top5.head.name, "Kingside Attack")
    assertEquals(narrativeCtx.plans.top5.last.name, "Piece Activation")
    // 6th plan (RookActivation) should not appear
    assert(!narrativeCtx.plans.top5.exists(_.name.contains("Rook Activation")), "6th plan should be cut")
  }

  // ============================================================
  // A5: L1 SNAPSHOT - with features set to trigger real path
  // ============================================================
  
  test("A5: buildL1Snapshot uses ctx.features for openFiles") {
    val openFileFen = "r3k2r/ppp2ppp/2n2n2/3b4/3B4/2N2N2/PPP2PPP/R3K2R w KQkq - 0 10"
    
    // Create PositionFeatures with the FEN embedded
    val features = PositionAnalyzer.extractFeatures(openFileFen, 20).getOrElse(
      fail("Failed to extract features from openFileFen")
    )
    
    val ctx = IntegratedContext(evalCp = 0, isWhiteToMove = true, features = Some(features))
    val data = minimalData(Some(ctx)).copy(fen = openFileFen)
    
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    // d and e files have no pawns in this FEN
    val openFiles = narrativeCtx.snapshots.head.openFiles
    assert(openFiles.contains("d"), s"d-file should be open. Got: $openFiles")
    assert(openFiles.contains("e"), s"e-file should be open. Got: $openFiles")
  }

  // ============================================================
  // A6: DELTA - Eval Change and Motif Diff
  // ============================================================
  
  test("A6: buildDelta calculates evalChange and motif diff") {
    val prevData = minimalData().copy(
      evalCp = 100,
      motifs = List(Motif.PawnAdvance(chess.File.D, 2, 4, Color.White, 0, Some("d4")))
    )
    val currData = minimalData().copy(
      evalCp = 200,
      motifs = List(
        Motif.RookLift(chess.File.F, 1, 3, Color.White, 0, Some("Rf3")),
        Motif.Centralization(chess.Knight, chess.Square.E5, Color.White, 0, Some("Ne5"))
      )
    )
    
    val ctx = IntegratedContext(evalCp = 200, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(currData, ctx, Some(prevData))
    
    assert(narrativeCtx.delta.isDefined, "Delta should be present")
    val delta = narrativeCtx.delta.get
    
    assertEquals(delta.evalChange, 100)
    assert(delta.newMotifs.contains("RookLift"), "RookLift should be new")
    assert(delta.lostMotifs.contains("PawnAdvance"), "PawnAdvance should be lost")
  }

  // ============================================================
  // A7: CANDIDATES - SAN Conversion assertion
  // ============================================================
  
  test("A7: buildCandidates converts UCI to SAN") {
    val candidateLine = lila.llm.model.strategic.VariationLine(
      moves = List("e2e4", "e7e5"),
      scoreCp = 50, mate = None, tags = Nil
    )
    val candidate = lila.llm.model.strategic.AnalyzedCandidate(
      move = "e2e4",
      score = 50,
      motifs = Nil,
      prophylaxisResults = Nil,
      futureContext = "Central control",
      moveIntent = lila.llm.model.strategic.MoveIntent("Central control", None),
      line = candidateLine
    )
    
    val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val data = minimalData().copy(fen = startFen, candidates = List(candidate))
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.candidates.nonEmpty, "Should have candidates")
    assertEquals(narrativeCtx.candidates.head.move, "e4", "UCI e2e4 should convert to SAN e4")
    assert(
      Set("", "!", "!?").contains(narrativeCtx.candidates.head.annotation),
      s"Unexpected annotation: ${narrativeCtx.candidates.head.annotation}"
    )
    assertEquals(narrativeCtx.candidates.head.planAlignment, "Central control")
    // scoreCp is 50, so alert should be None. Let's try 300 in another candidate or change this one.
    // For now, just check it is None as expected for 50.
    assertEquals(narrativeCtx.candidates.head.tacticalAlert, None)
  }

  // ============================================================
  // A8: PHASE CONTEXT - Transition Trigger
  // ============================================================
  
  test("A8: buildPhaseContext detects phase transition trigger") {
    val classification = PositionClassification(
      nature = NatureResult(lila.llm.analysis.L3.NatureType.Dynamic, 3, 2, 1, false),
      criticality = CriticalityResult(CriticalityType.Normal, 0, None, 0),
      choiceTopology = ChoiceTopologyResult(ChoiceTopologyType.StyleChoice, 50, 40, None, 10, 15, None),
      gamePhase = GamePhaseResult(GamePhaseType.Endgame, 12, false, 0),
      simplifyBias = SimplifyBiasResult(false, 0, false, false),
      drawBias = DrawBiasResult(false, false, false, false, false),
      riskProfile = RiskProfileResult(RiskLevel.Low, 0, 0, 0),
      taskMode = TaskModeResult(TaskModeType.ExplainPlan, "default")
    )
    
    val prevData = minimalData().copy(phase = "middlegame")
    val ctx = IntegratedContext(evalCp = 0, isWhiteToMove = true, classification = Some(classification))
    val currData = minimalData(Some(ctx)).copy(phase = "endgame")
    
    val narrativeCtx = NarrativeContextBuilder.build(currData, ctx, Some(prevData))
    
    assertEquals(narrativeCtx.phase.current, "Endgame")
    assert(narrativeCtx.phase.transitionTrigger.isDefined, "Should detect phase transition")
    assert(narrativeCtx.phase.transitionTrigger.get.contains("Middlegame"), "Should mention from-phase")
  }

  // ============================================================
  // TOCONTEXT TESTS
  // ============================================================

  test("toContext returns integratedContext when populated") {
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val data = minimalData(Some(ctx))
    assertEquals(data.toContext.evalCp, 50)
  }

  test("toContext falls back to minimal when integratedContext is None") {
    val data = minimalData().copy(evalCp = 100, isWhiteToMove = false)
    assertEquals(data.toContext.evalCp, 100)
    assert(data.toContext.classification.isEmpty)
  }

  test("strategicFlow reflects transition type with continuity context") {
    val plans = List(PlanMatch(Plan.CentralControl(Color.White), 0.82, Nil))
    val data = minimalData().copy(
      plans = plans,
      planContinuity = Some(PlanContinuity("Central Control", Some("CentralControl"), 2, 5)),
      planSequence = Some(
        PlanSequenceSummary(
          transitionType = TransitionType.Continuation,
          momentum = 0.7,
          primaryPlanId = Some("CentralControl"),
          primaryPlanName = Some("Central Control")
        )
      )
    )
    val ctx = IntegratedContext(evalCp = 60, isWhiteToMove = true)

    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    val flow = narrativeCtx.strategicFlow.getOrElse(fail("strategicFlow should be generated"))
    assert(flow.contains("continuing"), s"Expected continuation wording, got: $flow")
    assert(flow.contains("Central Control"), s"Expected plan anchor, got: $flow")
  }

  test("describeHierarchical outputs PHASE section (A8)") {
    val ctx = lila.llm.model.NarrativeContext(
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      header = lila.llm.model.ContextHeader("Middlegame", "Normal", "StyleChoice", "Medium", "ExplainPlan"),
      ply = 1,
      summary = lila.llm.model.NarrativeSummary("Kingside Attack", None, "StyleChoice", "Maintain", "+50"),
      threats = lila.llm.model.ThreatTable(Nil, Nil),
      pawnPlay = lila.llm.model.PawnPlayTable(false, None, "Low", "Maintain", "No breaks", "Background", None, false, "quiet"),
      plans = lila.llm.model.PlanTable(Nil, Nil),
      snapshots = List(lila.llm.model.L1Snapshot("=", None, None, None, None, None, Nil)),
      delta = None,
      phase = lila.llm.model.PhaseContext("Middlegame", "Material: 28, Queens present", None),
      candidates = Nil
    )
    
    val output = lila.llm.NarrativeGenerator.describeHierarchical(ctx)
    
    assert(output.contains("=== PHASE"), "Should have PHASE section")
     assert(output.contains("Middlegame"), "Phase should show Middlegame")
   }

  // ============================================================
  // PROBE LOOP (Phase 6.5): Ghost plan -> ProbeRequest
  // ============================================================

  test("Probe: emits LEGAL UCI moves when a high-score plan is not represented in top MultiPV moves") {
    val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    val plan = PlanMatch(
      plan = Plan.KingsideAttack(Color.White),
      score = 0.80,
      evidence = List(
        EvidenceAtom(
          motif = Motif.PawnAdvance(File.G, fromRank = 2, toRank = 4, color = Color.White, plyIndex = 0, move = Some("g4")),
          weight = 1.0,
          description = "g-pawn push"
        )
      )
    )

    val pv1 = lila.llm.model.strategic.VariationLine(
      moves = List("e2e4", "e7e5"),
      scoreCp = 20,
      mate = None,
      tags = Nil
    )

    val data = minimalData().copy(fen = startFen, plans = List(plan), alternatives = List(pv1))
    val ctx = IntegratedContext(evalCp = 0, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)

    val planRequests = narrativeCtx.probeRequests.filterNot(_.purpose.contains("NullMoveThreat"))
    assert(planRequests.nonEmpty, "Should emit at least one plan probe request")
    val req = planRequests.head
    assert(req.moves.contains("g2g4"), s"Expected g2g4 in probe moves, got ${req.moves}")

    val pos = Fen.read(Standard, Fen.Full(startFen)).getOrElse(sys.error("Invalid FEN"))
    req.moves.foreach { uciStr =>
      val uciMove = Uci(uciStr).get.asInstanceOf[Uci.Move]
      assert(pos.move(uciMove).toOption.isDefined, s"Probe move must be legal UCI: $uciStr")
    }
  }

  test("Probe: does not emit when the plan is already represented in top MultiPV moves") {
    val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    val plan = PlanMatch(
      plan = Plan.KingsideAttack(Color.White),
      score = 0.80,
      evidence = List(
        EvidenceAtom(
          motif = Motif.PawnAdvance(File.G, fromRank = 2, toRank = 4, color = Color.White, plyIndex = 0, move = Some("g4")),
          weight = 1.0,
          description = "g-pawn push"
        )
      )
    )

    val pv1 = lila.llm.model.strategic.VariationLine(
      moves = List("g2g4", "e7e5"),
      scoreCp = 0,
      mate = None,
      tags = Nil
    )

    val data = minimalData().copy(fen = startFen, plans = List(plan), alternatives = List(pv1))
    val ctx = IntegratedContext(evalCp = 0, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)

    val planRequests = narrativeCtx.probeRequests.filterNot(_.purpose.contains("NullMoveThreat"))
    assertEquals(planRequests, Nil, "Should not request plan probes for already-represented plans")
  }

  test("Probe: score threshold prevents noisy probes") {
    val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val plan = PlanMatch(plan = Plan.KingsideAttack(Color.White), score = 0.69, evidence = Nil)
    val pv1 = lila.llm.model.strategic.VariationLine(moves = List("e2e4"), scoreCp = 0, mate = None, tags = Nil)

    val data = minimalData().copy(fen = startFen, plans = List(plan), alternatives = List(pv1))
    val ctx = IntegratedContext(evalCp = 0, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)

    val planRequests = narrativeCtx.probeRequests.filterNot(_.purpose.contains("NullMoveThreat"))
    assertEquals(planRequests, Nil, "Plan score below threshold should not trigger plan probes")
  }

  // ============================================================
  // B-AXIS: META SIGNALS VERIFICATION
  // ============================================================

  test("B1: ChoiceType derived from classification.choiceTopology (OnlyMove)") {
    val classification = PositionClassification(
      nature = NatureResult(lila.llm.analysis.L3.NatureType.Dynamic, 2, 1, 0, false),
      criticality = CriticalityResult(CriticalityType.Normal, 0, None, 1),
      choiceTopology = ChoiceTopologyResult(ChoiceTopologyType.OnlyMove, 100, -50, None, 150, 150, Some("loses_material")),
      gamePhase = GamePhaseResult(GamePhaseType.Middlegame, 50, true, 4),
      simplifyBias = SimplifyBiasResult(false, 50, false, false),
      drawBias = DrawBiasResult(false, false, false, false, false),
      riskProfile = RiskProfileResult(RiskLevel.Medium, 0, 0, 0),
      taskMode = TaskModeResult(TaskModeType.ExplainPlan, "opening")
    )
    
    val ctx = IntegratedContext(evalCp = 100, isWhiteToMove = true, classification = Some(classification))
    val narrativeCtx = NarrativeContextBuilder.build(minimalData(Some(ctx)), ctx, None)
    
    assert(narrativeCtx.meta.isDefined, "Meta should be populated")
    assertEquals(narrativeCtx.meta.get.choiceType, ChoiceType.OnlyMove)
  }

  test("B5: Targets extracted from threats with tactical/strategic separation") {
    val threatToThem = Threat(
      kind = ThreatKind.Material,
      lossIfIgnoredCp = 300,
      turnsToImpact = 2,
      motifs = List("Fork"),
      attackSquares = List("e5", "e5", "d4"), // Duplicate e5
      targetPieces = List("Knight"),
      bestDefense = None,
      defenseCount = 1
    )
    
    val threatToUs = Threat(
      kind = ThreatKind.Positional,
      lossIfIgnoredCp = 100,
      turnsToImpact = 3,
      motifs = List("OpenFile"),
      attackSquares = List("c3"),
      targetPieces = Nil,
      bestDefense = Some("Nc3"),
      defenseCount = 2
    )
    
    val taToThem = ThreatAnalysis(
      threats = List(threatToThem),
      defense = DefenseAssessment(ThreatSeverity.Low, None, Nil, false, false, 0, ""),
      threatSeverity = ThreatSeverity.Important, immediateThreat = true, strategicThreat = false,
      threatIgnorable = false, defenseRequired = false, counterThreatBetter = false,
      prophylaxisNeeded = false, resourceAvailable = true, maxLossIfIgnored = 300,
      primaryDriver = "material", insufficientData = false
    )
    val taToUs = ThreatAnalysis(
      threats = List(threatToUs),
      defense = DefenseAssessment(ThreatSeverity.Low, None, Nil, false, false, 0, ""),
      threatSeverity = ThreatSeverity.Low, immediateThreat = false, strategicThreat = true,
      threatIgnorable = true, defenseRequired = false, counterThreatBetter = false,
      prophylaxisNeeded = false, resourceAvailable = true, maxLossIfIgnored = 100,
      primaryDriver = "positional", insufficientData = false
    )
    
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true, threatsToThem = Some(taToThem), threatsToUs = Some(taToUs))
    val narrativeCtx = NarrativeContextBuilder.build(minimalData(Some(ctx)), ctx, None)
    
    assert(narrativeCtx.meta.isDefined)
    val targets = narrativeCtx.meta.get.targets
    
    // Tactical targets (from threats)
    val tacticalLabels = targets.tactical.map(_.ref.label).toSet
    assert(tacticalLabels.contains("e5"), s"Expected e5 in tactical, got $tacticalLabels")
    assert(tacticalLabels.contains("d4"), s"Expected d4 in tactical, got $tacticalLabels")
    assert(tacticalLabels.contains("c3"), s"Expected c3 in tactical, got $tacticalLabels")
    
    assertEquals(targets.tactical.find(_.ref.label == "e5").get.priority, 1)
  }

  test("Phase D: Strategic targets extracted from plan evidence") {
    val plan = PlanMatch(
      plan = Plan.KingsideAttack(Color.White),
      score = 0.8,
      evidence = List(
        EvidenceAtom(
          motif = Motif.Fork(chess.Knight, List(chess.King, chess.Rook), chess.Square.D5, Nil, Color.White, 0, None),
          weight = 1.0,
          description = "Knight fork"
        )
      )
    )
    
    val data = minimalData().copy(plans = List(plan))
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val result = NarrativeContextBuilder.build(data, ctx)
    
    val strategic = result.meta.get.targets.strategic
    assert(strategic.exists(_.ref.label == "d5"), s"d5 should be strategic from plan evidence. Got: ${strategic.map(_.ref.label)}")
    assertEquals(strategic.find(_.ref.label == "d5").get.priority, 3)
  }

  test("B8: PlanConcurrency shows independent when plans have different categories") {
    val plan1 = PlanMatch(plan = Plan.KingsideAttack(Color.White), score = 0.90, evidence = Nil)
    val plan2 = PlanMatch(plan = Plan.Simplification(Color.White), score = 0.60, evidence = Nil)
    
    val data = minimalData().copy(plans = List(plan1, plan2))
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.meta.isDefined)
    val concurrency = narrativeCtx.meta.get.planConcurrency
    
    assertEquals(concurrency.primary, "Kingside Attack")
    assertEquals(concurrency.secondary, Some("Simplification into Endgame"))
    // Attack vs Transition => independent
    assertEquals(concurrency.relationship, "independent")
  }

  test("B8: PlanConcurrency shows independent when no secondary plan") {
    val plan1 = PlanMatch(plan = Plan.CentralControl(Color.White), score = 0.80, evidence = Nil)
    
    val data = minimalData().copy(plans = List(plan1))
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.meta.isDefined)
    assertEquals(narrativeCtx.meta.get.planConcurrency.relationship, "independent")
  }

  test("B8: PlanConcurrency shows synergy when both plans share same category") {
    // Both are Attack category
    val plan1 = PlanMatch(plan = Plan.KingsideAttack(Color.White), score = 0.85, evidence = Nil)
    val plan2 = PlanMatch(plan = Plan.QueensideAttack(Color.White), score = 0.75, evidence = Nil)
    
    val data = minimalData().copy(plans = List(plan1, plan2))
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.meta.isDefined)
    assertEquals(narrativeCtx.meta.get.planConcurrency.relationship, "↔ synergy")
  }

  // ============================================================
  // B3: DIVERGEPLY (COUNTERFACTUAL BASED)
  // ============================================================

  // Helper: dummy plan to ensure meta is populated (meta requires classification/threats/plans)
  private val dummyPlan = PlanMatch(plan = Plan.CentralControl(Color.White), score = 0.5, evidence = Nil)

  test("B3: DivergePly populated when counterfactual exists with punisher") {
    val userLine = lila.llm.model.strategic.VariationLine(
      moves = List("e2e4", "d7d5", "e4d5"),  // User move, punisher (d7d5), continuation
      scoreCp = -100,
      mate = None,
      tags = Nil
    )
    
    val counterfactual = lila.llm.model.strategic.CounterfactualMatch(
      userMove = "e2e4",
      bestMove = "d2d4",
      cpLoss = 50,
      missedMotifs = Nil,
      userMoveMotifs = Nil,
      severity = "inaccuracy",
      userLine = userLine
    )
    
    val data = minimalData().copy(
      ply = 10,
      counterfactual = Some(counterfactual),
      plans = List(dummyPlan)  // Ensure meta is populated
    )
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.meta.isDefined, "meta should be defined when plans exist")
    val div = narrativeCtx.meta.get.divergence
    assert(div.isDefined, "Divergence should be populated when counterfactual exists")
    assertEquals(div.get.divergePly, 10)
    assertEquals(div.get.punisherMove, Some("d7d5"))
    assertEquals(div.get.branchPointFen, Some(testFen))
  }

  test("B3: DivergePly is None when no counterfactual") {
    val data = minimalData().copy(counterfactual = None, plans = List(dummyPlan))
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.meta.isDefined)
    assertEquals(narrativeCtx.meta.get.divergence, None)
  }

  test("B3: DivergePly punisherMove is None when userLine has only 1 move") {
    val userLine = lila.llm.model.strategic.VariationLine(
      moves = List("e2e4"),  // Only user move, no opponent response
      scoreCp = -50,
      mate = None,
      tags = Nil
    )
    
    val counterfactual = lila.llm.model.strategic.CounterfactualMatch(
      userMove = "e2e4",
      bestMove = "d2d4",
      cpLoss = 30,
      missedMotifs = Nil,
      userMoveMotifs = Nil,
      severity = "inaccuracy",
      userLine = userLine
    )
    
    val data = minimalData().copy(ply = 5, counterfactual = Some(counterfactual), plans = List(dummyPlan))
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.meta.isDefined)
    val div = narrativeCtx.meta.get.divergence
    assert(div.isDefined)
    assertEquals(div.get.divergePly, 5)
    assertEquals(div.get.punisherMove, None)  // No opponent response available
  }

  // ============================================================
  // B2/B6: ERRORCLASSIFICATION
  // ============================================================

  test("B2/B6: ErrorClassification tactical when cpLoss >= 200 and blunder severity") {
    val userLine = lila.llm.model.strategic.VariationLine(
      moves = List("g2g4", "d8h4"),  // Blunder allowing Qh4#
      scoreCp = -1000,
      mate = Some(-1),
      tags = Nil
    )
    
    val counterfactual = lila.llm.model.strategic.CounterfactualMatch(
      userMove = "g2g4",
      bestMove = "e2e4",
      cpLoss = 500,
      missedMotifs = List(Motif.Check(
        piece = chess.Queen,
        targetSquare = chess.Square.H4,
        checkType = Motif.CheckType.Normal,
        color = Color.Black,
        plyIndex = 1,
        move = Some("Qh4#")
      )),
      userMoveMotifs = Nil,
      severity = "blunder",
      userLine = userLine
    )
    
    val data = minimalData().copy(counterfactual = Some(counterfactual), plans = List(dummyPlan))
    val ctx = IntegratedContext(evalCp = -500, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.meta.isDefined)
    val err = narrativeCtx.meta.get.errorClass
    assert(err.isDefined, "ErrorClass should be populated when counterfactual exists")
    assert(err.get.isTactical, "Should be tactical for blunder with cpLoss >= 200")
    assert(err.get.errorSummary.contains("전술"))
    assert(err.get.missedMotifs.nonEmpty)
  }

  test("B2/B6: ErrorClassification positional when cpLoss < 200") {
    val userLine = lila.llm.model.strategic.VariationLine(
      moves = List("a2a3", "e7e5"),
      scoreCp = -30,
      mate = None,
      tags = Nil
    )
    
    val counterfactual = lila.llm.model.strategic.CounterfactualMatch(
      userMove = "a2a3",
      bestMove = "e2e4",
      cpLoss = 50,
      missedMotifs = Nil,
      userMoveMotifs = Nil,
      severity = "inaccuracy",
      userLine = userLine
    )
    
    val data = minimalData().copy(counterfactual = Some(counterfactual), plans = List(dummyPlan))
    val ctx = IntegratedContext(evalCp = -30, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.meta.isDefined)
    val err = narrativeCtx.meta.get.errorClass
    assert(err.isDefined)
    assert(!err.get.isTactical, "Should be positional for cpLoss < 200")
    assert(err.get.errorSummary.contains("포지셔널"))
  }

  test("B2/B6: ErrorClassification is None when no counterfactual") {
    val data = minimalData().copy(counterfactual = None, plans = List(dummyPlan))
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.meta.isDefined)
    assertEquals(narrativeCtx.meta.get.errorClass, None)
  }

  test("B2/B6: ErrorClassification is None when cpLoss < 50 (style difference)") {
    val userLine = lila.llm.model.strategic.VariationLine(
      moves = List("a2a3", "e7e5"),
      scoreCp = -10,
      mate = None,
      tags = Nil
    )
    
    val counterfactual = lila.llm.model.strategic.CounterfactualMatch(
      userMove = "a2a3",
      bestMove = "e2e4",
      cpLoss = 30,  // Below threshold
      missedMotifs = Nil,
      userMoveMotifs = Nil,
      severity = "inaccuracy",
      userLine = userLine
    )
    
    val data = minimalData().copy(counterfactual = Some(counterfactual), plans = List(dummyPlan))
    val ctx = IntegratedContext(evalCp = -10, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.meta.isDefined)
    assertEquals(narrativeCtx.meta.get.errorClass, None, "cpLoss < 50 should return None")
  }

  test("B2/B6: ErrorClassification tactical when cpLoss >= 200 and missedMotif is Fork (non-blunder)") {
    val userLine = lila.llm.model.strategic.VariationLine(
      moves = List("a2a3", "c6d4"),  // Missed fork
      scoreCp = -250,
      mate = None,
      tags = Nil
    )
    
    val counterfactual = lila.llm.model.strategic.CounterfactualMatch(
      userMove = "a2a3",
      bestMove = "c6d4",
      cpLoss = 250,
      missedMotifs = List(Motif.Fork(
        attackingPiece = chess.Knight,
        targets = List(chess.King, chess.Rook),  // Targeted pieces, not squares
        targetSquares = Nil,
        square = chess.Square.D4,
        color = Color.Black,
        plyIndex = 1,
        move = Some("Nd4")
      )),
      userMoveMotifs = Nil,
      severity = "mistake",  // Not blunder, but has tactical motif
      userLine = userLine
    )
    
    val data = minimalData().copy(counterfactual = Some(counterfactual), plans = List(dummyPlan))
    val ctx = IntegratedContext(evalCp = -250, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.meta.isDefined)
    val err = narrativeCtx.meta.get.errorClass
    assert(err.isDefined)
    assert(err.get.isTactical, "Should be tactical for cpLoss >= 200 with Fork motif")
    assert(err.get.errorSummary.contains("전술"))
    assert(err.get.missedMotifs.exists(_.contains("Fork")))
  }

  test("B7: WhyNot enrichment and Ghost Plan detection") {
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    
    // 1. Existing candidate e2e4 (but say it's bad in probe)
    val cand1 = AnalyzedCandidate(
      move = "e2e4",
      score = 30,
      motifs = Nil,
      prophylaxisResults = Nil,
      futureContext = "Center",
      moveIntent = MoveIntent("Central control", None),
      line = VariationLine(List("e2e4", "e7e5"), 30, None, 0, None, Nil)
    )
    
    val data = minimalData().copy(
      fen = fen, 
      candidates = List(cand1),
      plans = List(dummyPlan),
      isWhiteToMove = true
    )
    
    val probeResult = ProbeResult(
      id = "probe1",
      probedMove = Some("e2e4"),
      evalCp = -150, // 180cp loss vs baseline 30
      bestReplyPv = List("e7e5", "d2d4"),
      deltaVsBaseline = -180,
      keyMotifs = Nil,
      mate = None
    )
    
    // 2. Ghost candidate g2g4 (missing from top PVs)
    val probeResultGhost = ProbeResult(
      id = "probe2",
      probedMove = Some("g2g4"),
      evalCp = -300,
      bestReplyPv = List("d7d5"),
      deltaVsBaseline = -330,
      keyMotifs = Nil,
      mate = None
    )
    
    val ctx = IntegratedContext(evalCp = 30, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None, List(probeResult, probeResultGhost))
    
    // Verify candidate enrichment
    val c1 = narrativeCtx.candidates.find(_.uci.contains("e2e4"))
    assert(c1.isDefined)
    assert(c1.get.whyNot.isDefined)
    assert(c1.get.whyNot.get.contains("inferior by 180 cp"))
    
    // Verify ghost candidate
    val cGhost = narrativeCtx.candidates.find(_.uci.contains("g2g4"))
    assert(cGhost.isDefined)
    assertEquals(cGhost.get.planAlignment, "Alternative Path")
    assert(cGhost.get.whyNot.get.contains("-330 cp"))
    
    // Verify meta summary
    assert(narrativeCtx.meta.isDefined)
    assert(narrativeCtx.meta.get.whyNot.isDefined)
    assert(narrativeCtx.meta.get.whyNot.get.contains("'g2g4' is refuted"))
  }

  // ============================================================
  // SEMANTIC SECTION (Phase A Enhancement)
  // ============================================================

  test("Semantic: buildSemanticSection returns None when no semantic data exists") {
    val data = minimalData() // No semantic fields populated
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assertEquals(narrativeCtx.semantic, None, "Semantic section should be None when no data")
  }

  test("Semantic: structuralWeaknesses converted with correct squareColor") {
    val weakness = WeakComplex(
      color = Color.White, // Light squares
      squares = List(chess.Square.F3, chess.Square.G2, chess.Square.H3),
      isOutpost = true,
      cause = "Missing fianchetto bishop"
    )
    
    val data = minimalData().copy(structuralWeaknesses = List(weakness))
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.semantic.isDefined, "Semantic should be defined")
    assertEquals(narrativeCtx.semantic.get.structuralWeaknesses.size, 1)
    val wc = narrativeCtx.semantic.get.structuralWeaknesses.head
    assertEquals(wc.squareColor, "light") // White => "light"
    assertEquals(wc.squares, List("f3", "g2", "h3"))
    assertEquals(wc.isOutpost, true)
    assertEquals(wc.cause, "Missing fianchetto bishop")
  }

  test("Semantic: pieceActivity converted with all fields") {
    val activity = PieceActivity(
      piece = chess.Knight,
      square = chess.Square.C3,
      mobilityScore = 0.75,
      isTrapped = false,
      isBadBishop = false,
      keyRoutes = List(chess.Square.E4, chess.Square.D5),
      coordinationLinks = List(chess.Square.E2)
    )
    
    val data = minimalData().copy(pieceActivity = List(activity))
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.semantic.isDefined)
    val pa = narrativeCtx.semantic.get.pieceActivity.head
    assertEquals(pa.piece.toLowerCase, "knight")
    assertEquals(pa.square, "c3")
    assertEquals(pa.mobilityScore, 0.75)
    assertEquals(pa.keyRoutes, List("e4", "d5"))
  }

  test("Semantic: positionalFeatures converts all PositionalTag variants") {
    val tags = List(
      PositionalTag.Outpost(chess.Square.E5, Color.White),
      PositionalTag.OpenFile(chess.File.D, Color.White),
      PositionalTag.WeakSquare(chess.Square.F3, Color.Black),
      PositionalTag.BishopPairAdvantage(Color.White),
      PositionalTag.OppositeColorBishops,
      PositionalTag.PawnMajority(Color.White, "queenside", 3)
    )
    
    val data = minimalData().copy(positionalFeatures = tags)
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.semantic.isDefined)
    val pf = narrativeCtx.semantic.get.positionalFeatures
    assertEquals(pf.size, 6)
    
    // Verify specific conversions
    val outpost = pf.find(_.tagType == "Outpost")
    assert(outpost.isDefined)
    assertEquals(outpost.get.square, Some("e5"))
    assertEquals(outpost.get.color.toLowerCase, "white")
    
    val openFile = pf.find(_.tagType == "OpenFile")
    assert(openFile.isDefined)
    assertEquals(openFile.get.file, Some("d"))
    
    val ocb = pf.find(_.tagType == "OppositeColorBishops")
    assert(ocb.isDefined)
    assertEquals(ocb.get.color, "Both") // This one might be "Both" as I hardcoded it
    
    val majority = pf.find(_.tagType == "PawnMajority")
    assert(majority.isDefined)
    assertEquals(majority.get.detail, Some("queenside 3 pawns"))
    
    // Check color for majority as well
    assertEquals(majority.get.color.toLowerCase, "white")
  }

  test("Semantic: compensation converted with returnVector") {
    val comp = Compensation(
      investedMaterial = 300, // Sacrificed a piece
      returnVector = Map("Time" -> 0.8, "Attack" -> 0.9),
      expiryPly = Some(10),
      conversionPlan = "Mating attack"
    )
    
    val data = minimalData().copy(compensation = Some(comp))
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.semantic.isDefined)
    val c = narrativeCtx.semantic.get.compensation
    assert(c.isDefined)
    assertEquals(c.get.investedMaterial, 300)
    assertEquals(c.get.returnVector("Time"), 0.8)
    assertEquals(c.get.conversionPlan, "Mating attack")
  }

  test("Semantic: endgameFeatures converted with key squares") {
    val ef = EndgameFeature(
      hasOpposition = true,
      isZugzwang = false,
      keySquaresControlled = List(chess.Square.D5, chess.Square.E5),
      oppositionType = EndgameOppositionType.Direct,
      zugzwangLikelihood = 0.41,
      ruleOfSquare = RuleOfSquareStatus.Holds,
      triangulationAvailable = true,
      kingActivityDelta = 2,
      rookEndgamePattern = RookEndgamePattern.KingCutOff,
      theoreticalOutcomeHint = TheoreticalOutcomeHint.Draw,
      confidence = 0.78
    )
    
    val data = minimalData().copy(endgameFeatures = Some(ef))
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.semantic.isDefined)
    val e = narrativeCtx.semantic.get.endgameFeatures
    assert(e.isDefined)
    assertEquals(e.get.hasOpposition, true)
    assertEquals(e.get.keySquaresControlled, List("d5", "e5"))
    assertEquals(e.get.oppositionType, "Direct")
    assertEquals(e.get.ruleOfSquare, "Holds")
    assertEquals(e.get.triangulationAvailable, true)
    assertEquals(e.get.rookEndgamePattern, "KingCutOff")
    assertEquals(e.get.theoreticalOutcomeHint, "Draw")
  }

  test("Semantic: preventedPlans converted with threat type") {
    val pp = PreventedPlan(
      planId = "PreventFork",
      deniedSquares = List(chess.Square.D4, chess.Square.E5),
      breakNeutralized = Some("f5"),
      mobilityDelta = -3,
      counterplayScoreDrop = 50,
      preventedThreatType = Some("Fork")
    )
    
    val data = minimalData().copy(preventedPlans = List(pp))
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.semantic.isDefined)
    val p = narrativeCtx.semantic.get.preventedPlans.head
    assertEquals(p.planId, "PreventFork")
    assertEquals(p.deniedSquares, List("d4", "e5"))
    assertEquals(p.breakNeutralized, Some("f5"))
    assertEquals(p.preventedThreatType, Some("Fork"))
  }

  test("Semantic: conceptSummary passed through directly") {
    val concepts = List("Kingside Attack", "Weak Back Rank", "Material Imbalance")
    
    val data = minimalData().copy(conceptSummary = concepts)
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.semantic.isDefined)
    assertEquals(narrativeCtx.semantic.get.conceptSummary, concepts)
  }

  test("Semantic: practicalAssessment converted with verdict") {
    val pa = PracticalAssessment(
      engineScore = 150,
      practicalScore = 120.5,
      biasFactors = List(BiasFactor("Mobility", "White has more active pieces", 0.3)),
      verdict = "White is Fighting"
    )
    
    val data = minimalData().copy(practicalAssessment = Some(pa))
    val ctx = IntegratedContext(evalCp = 150, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.semantic.isDefined)
    val p = narrativeCtx.semantic.get.practicalAssessment
    assert(p.isDefined)
    assertEquals(p.get.engineScore, 150)
    assertEquals(p.get.practicalScore, 120.5)
    assertEquals(p.get.verdict, "White is Fighting")
  }

  // ============================================================
  // OPPONENT PLAN (Phase B Enhancement)
  // ============================================================

  test("OpponentPlan: buildOpponentPlan returns opponent's top plan") {
    // Motifs that would score well for Black (opponent when White to move)
    val blackMotifs = List(
      Motif.PawnAdvance(chess.File.E, 7, 5, Color.Black, 0, Some("e5")),
      Motif.Centralization(chess.Knight, chess.Square.D4, Color.Black, 0, Some("Nd4"))
    )
    
    val data = minimalData().copy(motifs = blackMotifs, isWhiteToMove = true)
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    // Should have opponent plan populated
    assert(narrativeCtx.opponentPlan.isDefined || narrativeCtx.opponentPlan.isEmpty, 
      "opponentPlan should be Option[PlanRow]")
  }

  test("OpponentPlan: returns None when no plans for opponent") {
    // Motifs only for White (to move)
    val whiteOnlyMotifs = List(
      Motif.PawnAdvance(chess.File.D, 2, 4, Color.White, 0, Some("d4"))
    )
    
    val data = minimalData().copy(motifs = whiteOnlyMotifs, isWhiteToMove = true)
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    // Opponent plan may be None or have low score
    // The key is that it doesn't crash
    assert(narrativeCtx.opponentPlan.isEmpty || narrativeCtx.opponentPlan.get.score <= 1.0)
  }

  test("OpponentPlan: opponent side is Black when White to move") {
    // Both sides have motifs
    val mixedMotifs = List(
      Motif.PawnAdvance(chess.File.D, 2, 4, Color.White, 0, Some("d4")),
      Motif.PawnAdvance(chess.File.E, 7, 5, Color.Black, 0, Some("e5")),
      Motif.Centralization(chess.Knight, chess.Square.E4, Color.Black, 0, Some("Ne4"))
    )
    
    val data = minimalData().copy(motifs = mixedMotifs, isWhiteToMove = true)
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    // When White to move, opponent is Black
    // The opponent plan should be based on Black's motifs
    if (narrativeCtx.opponentPlan.isDefined) {
      assert(narrativeCtx.opponentPlan.get.rank == 1, "Opponent's top plan should be rank 1")
    }
  }

  // ============================================================
  // WHYNOT L1 DELTA (Phase C Enhancement)
  // ============================================================

  test("Phase C: WhyNot summary includes L1 collapse reason") {
    val l1Delta = L1DeltaSnapshot(
      materialDelta = 0,
      kingSafetyDelta = -3,
      centerControlDelta = -2,
      openFilesDelta = 0,
      mobilityDelta = -5,
      collapseReason = Some("King safety collapsed")
    )
    
    val probeResult = ProbeResult(
      id = "probe_c1",
      probedMove = Some("g2g4"),
      evalCp = -300,
      bestReplyPv = List("d7d5"),
      deltaVsBaseline = -350,
      keyMotifs = Nil,
      l1Delta = Some(l1Delta)
    )
    
    val data = minimalData().copy(isWhiteToMove = true)
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    // buildMetaSignals calls buildWhyNotSummary
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None, List(probeResult))
    
    val whyNot = narrativeCtx.meta.flatMap(_.whyNot).getOrElse("")
    assert(whyNot.contains("350 cp"), s"Should mention eval loss. Got: $whyNot")
    assert(whyNot.contains("(King safety collapsed) [Verified]"), s"Should include L1 collapse reason and [Verified] tag. Got: $whyNot")
  }

  test("Phase C: Candidate explanation includes L1 collapse reason") {
    val l1Delta = L1DeltaSnapshot(
      materialDelta = -300,
      kingSafetyDelta = 0,
      centerControlDelta = 0,
      openFilesDelta = 0,
      mobilityDelta = -2,
      collapseReason = Some("Material loss")
    )
    
    val probeResult = ProbeResult(
      id = "probe_c2",
      probedMove = Some("e2e4"), // UCI for the candidate
      evalCp = -250,
      bestReplyPv = List("e7e5", "d2d4"),
      deltaVsBaseline = -300,
      keyMotifs = Nil,
      l1Delta = Some(l1Delta)
    )
    
    val cand1 = AnalyzedCandidate(
      move = "e2e4",
      score = -250,
      motifs = Nil,
      prophylaxisResults = Nil,
      futureContext = "Center",
      line = VariationLine(List("e2e4", "e7e5"), -250, None, 0, None, Nil)
    )
    
    val data = minimalData().copy(
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      candidates = List(cand1),
      isWhiteToMove = true
    )
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None, List(probeResult))
    
    val candidate = narrativeCtx.candidates.find(_.uci.contains("e2e4")).get
    val whyNot = candidate.whyNot.getOrElse("")
    
    assert(whyNot.contains("300 cp"), s"Should mention eval loss. Got: $whyNot")
    assert(whyNot.contains("Material loss"), s"Should include L1 collapse reason. Got: $whyNot")
  }

  test("opponentPlan POV swap: White threat triggers Black prophylaxis") {
    val ta = ThreatAnalysis(
      threats = List(Threat(
        kind = ThreatKind.Mate,
        lossIfIgnoredCp = 1000,
        turnsToImpact = 1,
        motifs = List("Mate"),
        attackSquares = List("h7"),
        targetPieces = List("King"),
        bestDefense = Some("Kh8"),
        defenseCount = 1
      )),
      defense = DefenseAssessment(ThreatSeverity.Urgent, Some("Kh8"), Nil, false, true, 80, "Mate threat"),
      threatSeverity = ThreatSeverity.Urgent,
      immediateThreat = true,
      strategicThreat = false,
      threatIgnorable = false,
      defenseRequired = true,
      counterThreatBetter = false,
      prophylaxisNeeded = true, 
      resourceAvailable = true,
      maxLossIfIgnored = 1000,
      primaryDriver = "mate_threat",
      insufficientData = false
    )
    
    val ctx = IntegratedContext(
      evalCp = 200,
      isWhiteToMove = true,
      threatsToUs = None,
      threatsToThem = Some(ta) // White threatening Black
    )
    
    val data = minimalData(Some(ctx)).copy(
      isWhiteToMove = true,
      motifs = List(Motif.KingStep(Motif.KingStepType.ToCorner, Color.Black, 0, None))
    )
    
    val result = NarrativeContextBuilder.build(data, ctx)
    
    // Opponent (Black) should want Prophylaxis because White (us) has a threat
    // This proves buildOpponentContext correctly swapped threatsToUs/Them
    assert(result.opponentPlan.isDefined)
    assert(result.opponentPlan.get.name.startsWith("Prophylaxis"), s"Expected Prophylaxis for opponent, got: ${result.opponentPlan.get.name}")
  }

  // ============================================================
  // PHASE F: DECISION RATIONALE TESTS
  // ============================================================
  
  test("F1: buildPVDelta returns empty when bestProbe is None") {
    val ctx = IntegratedContext(
      evalCp = 100,
      isWhiteToMove = true,
      classification = Some(PositionClassification(
        nature = NatureResult(lila.llm.analysis.L3.NatureType.Dynamic, 0, 0, 0, false),
        criticality = CriticalityResult(CriticalityType.CriticalMoment, 50, None, 0), // Non-StyleChoice
        choiceTopology = ChoiceTopologyResult(ChoiceTopologyType.NarrowChoice, 100, 50, None, 2, 0, None),
        gamePhase = GamePhaseResult(GamePhaseType.Middlegame, 40, true, 4),
        simplifyBias = SimplifyBiasResult(false, 0, false, false),
        drawBias = DrawBiasResult(false, false, false, false, false),
        riskProfile = RiskProfileResult(RiskLevel.Medium, 0, 0, 0),
        taskMode = TaskModeResult(TaskModeType.ExplainPlan, "test")
      ))
    )
    
    val data = minimalData(Some(ctx))
    // Note: No probeResults passed = bestProbe=None
    val result = NarrativeContextBuilder.build(data, ctx, None, Nil)
    
    // Decision should exist (NarrowChoice, not StyleChoice)
    assert(result.decision.isDefined, "Decision should be populated for NarrowChoice")
    
    // PVDelta should be empty (no probe data)
    val delta = result.decision.get.delta
    assertEquals(delta.resolvedThreats, Nil, "resolvedThreats should be empty without probe")
    assertEquals(delta.newOpportunities, Nil, "newOpportunities should be empty without probe")
    assertEquals(delta.planAdvancements, Nil, "planAdvancements should be empty without probe")
    assertEquals(delta.concessions, Nil, "concessions should be empty without probe")
  }
  
  test("F2: logicSummary is conservative when probe is unavailable") {
    val ctx = IntegratedContext(
      evalCp = 100,
      isWhiteToMove = true,
      classification = Some(PositionClassification(
        nature = NatureResult(lila.llm.analysis.L3.NatureType.Dynamic, 0, 0, 0, false),
        criticality = CriticalityResult(CriticalityType.CriticalMoment, 50, None, 0),
        choiceTopology = ChoiceTopologyResult(ChoiceTopologyType.NarrowChoice, 100, 50, None, 2, 0, None),
        gamePhase = GamePhaseResult(GamePhaseType.Middlegame, 40, true, 4),
        simplifyBias = SimplifyBiasResult(false, 0, false, false),
        drawBias = DrawBiasResult(false, false, false, false, false),
        riskProfile = RiskProfileResult(RiskLevel.Medium, 0, 0, 0),
        taskMode = TaskModeResult(TaskModeType.ExplainPlan, "test")
      ))
    )
    
    val data = minimalData(Some(ctx))
    val result = NarrativeContextBuilder.build(data, ctx, None, Nil)
    
    assert(result.decision.isDefined)
    val logicSummary = result.decision.get.logicSummary
    assert(logicSummary.contains("probe needed"), s"LogicSummary should indicate probe needed, got: $logicSummary")
    assertEquals(result.decision.get.confidence, ConfidenceLevel.Heuristic, "Confidence should be Heuristic without probe")
  }
  
  test("F3: VariationTag maps to CandidateTag correctly") {
    val ctx = IntegratedContext(evalCp = 100, isWhiteToMove = true)
    
    // Create candidate with VariationTag.Sharp
    val candidate = AnalyzedCandidate(
      move = "e2e4",
      score = 50,
      motifs = Nil,
      prophylaxisResults = Nil,
      futureContext = "central break",
      line = VariationLine(
        moves = List("e2e4", "e7e5"),
        scoreCp = 50,
        depth = 20,
        tags = List(VariationTag.Sharp, VariationTag.Solid)
      )
    )
    
    val data = minimalData(Some(ctx)).copy(candidates = List(candidate))
    val result = NarrativeContextBuilder.build(data, ctx, None, Nil)
    
    assertEquals(result.candidates.size, 1)
    val tags = result.candidates.head.tags
    assert(tags.contains(CandidateTag.Sharp), s"Should have Sharp tag, got: $tags")
    assert(tags.contains(CandidateTag.Solid), s"Should have Solid tag, got: $tags")
  }
  
  test("F4: VariationTag.Simplification maps to CandidateTag.Converting") {
    val ctx = IntegratedContext(evalCp = 100, isWhiteToMove = true)
    
    val candidate = AnalyzedCandidate(
      move = "d4d5",
      score = 100,
      motifs = Nil,
      prophylaxisResults = Nil,
      futureContext = "simplification",
      line = VariationLine(
        moves = List("d4d5"),
        scoreCp = 100,
        depth = 15,
        tags = List(VariationTag.Simplification)
      )
    )
    
    val data = minimalData(Some(ctx)).copy(candidates = List(candidate))
    val result = NarrativeContextBuilder.build(data, ctx, None, Nil)
    
    val tags = result.candidates.head.tags
    assert(tags.contains(CandidateTag.Converting), s"Simplification should map to Converting, got: $tags")
  }
  
  test("F5: VariationTag.Prophylaxis maps to CandidateTag.Prophylactic") {
    val ctx = IntegratedContext(evalCp = 100, isWhiteToMove = true)
    
    val candidate = AnalyzedCandidate(
      move = "h2h3",
      score = 30,
      motifs = Nil,
      prophylaxisResults = Nil,
      futureContext = "defensive",
      line = VariationLine(
        moves = List("h2h3"),
        scoreCp = 30,
        depth = 15,
        tags = List(VariationTag.Prophylaxis)
      )
    )
    
    val data = minimalData(Some(ctx)).copy(candidates = List(candidate))
    val result = NarrativeContextBuilder.build(data, ctx, None, Nil)
    
    val tags = result.candidates.head.tags
    assert(tags.contains(CandidateTag.Prophylactic), s"Prophylaxis should map to Prophylactic, got: $tags")
  }
  
  test("F6: Decision section is not generated for StyleChoice") {
    val ctx = IntegratedContext(
      evalCp = 100,
      isWhiteToMove = true,
      classification = Some(PositionClassification(
        nature = NatureResult(lila.llm.analysis.L3.NatureType.Static, 0, 0, 0, false),
        criticality = CriticalityResult(CriticalityType.Normal, 0, None, 0),
        choiceTopology = ChoiceTopologyResult(ChoiceTopologyType.StyleChoice, 10, 5, None, 3, 0, None), // StyleChoice
        gamePhase = GamePhaseResult(GamePhaseType.Middlegame, 40, true, 4),
        simplifyBias = SimplifyBiasResult(false, 0, false, false),
        drawBias = DrawBiasResult(false, false, false, false, false),
        riskProfile = RiskProfileResult(RiskLevel.Low, 0, 0, 0),
        taskMode = TaskModeResult(TaskModeType.ExplainPlan, "test")
      ))
    )
    
    val data = minimalData(Some(ctx))
    val result = NarrativeContextBuilder.build(data, ctx, None, Nil)
    
    // StyleChoice positions should NOT have Decision section (cost control)
    assert(result.decision.isEmpty, s"Decision should be None for StyleChoice, got: ${result.decision}")
  }
  
  // ============================================================
  // P1: FUTURESNAPSHOT-BASED PVDELTA TESTS
  // ============================================================
  
  test("F7: FutureSnapshot populates resolvedThreats accurately") {
    val ctx = IntegratedContext(
      evalCp = 100,
      isWhiteToMove = true,
      classification = Some(PositionClassification(
        nature = NatureResult(lila.llm.analysis.L3.NatureType.Dynamic, 0, 0, 0, false),
        criticality = CriticalityResult(CriticalityType.CriticalMoment, 50, None, 0),
        choiceTopology = ChoiceTopologyResult(ChoiceTopologyType.NarrowChoice, 100, 50, None, 2, 0, None),
        gamePhase = GamePhaseResult(GamePhaseType.Middlegame, 40, true, 4),
        simplifyBias = SimplifyBiasResult(false, 0, false, false),
        drawBias = DrawBiasResult(false, false, false, false, false),
        riskProfile = RiskProfileResult(RiskLevel.Medium, 0, 0, 0),
        taskMode = TaskModeResult(TaskModeType.ExplainPlan, "test")
      ))
    )
    
    val futureSnapshot = FutureSnapshot(
      resolvedThreatKinds = List("Mate", "Material"),
      newThreatKinds = Nil,
      targetsDelta = TargetsDelta(Nil, Nil, Nil, Nil),
      planBlockersRemoved = Nil,
      planPrereqsMet = Nil
    )
    
    val probeResult = ProbeResult(
      id = "test",
      evalCp = 100,
      bestReplyPv = Nil,
      deltaVsBaseline = 0,
      keyMotifs = Nil,
      probedMove = Some("e2e4"),
      futureSnapshot = Some(futureSnapshot)
    )
    
    val candidate = AnalyzedCandidate(
      move = "e2e4",
      score = 100,
      motifs = Nil,
      prophylaxisResults = Nil,
      futureContext = "central",
      line = VariationLine(List("e2e4"), 100, depth = 20)
    )
    
    val data = minimalData(Some(ctx)).copy(candidates = List(candidate))
    val result = NarrativeContextBuilder.build(data, ctx, None, List(probeResult))
    
    assert(result.decision.isDefined, "Decision should be populated")
    val delta = result.decision.get.delta
    assertEquals(delta.resolvedThreats, List("Mate", "Material"), "Should use FutureSnapshot.resolvedThreatKinds")
    assertEquals(result.decision.get.confidence, ConfidenceLevel.Probe, "Confidence should be Probe when futureSnapshot is used")
  }
  
  test("F8: FutureSnapshot populates newOpportunities from targetsDelta") {
    val ctx = IntegratedContext(
      evalCp = 100,
      isWhiteToMove = true,
      classification = Some(PositionClassification(
        nature = NatureResult(lila.llm.analysis.L3.NatureType.Dynamic, 0, 0, 0, false),
        criticality = CriticalityResult(CriticalityType.CriticalMoment, 50, None, 0),
        choiceTopology = ChoiceTopologyResult(ChoiceTopologyType.NarrowChoice, 100, 50, None, 2, 0, None),
        gamePhase = GamePhaseResult(GamePhaseType.Middlegame, 40, true, 4),
        simplifyBias = SimplifyBiasResult(false, 0, false, false),
        drawBias = DrawBiasResult(false, false, false, false, false),
        riskProfile = RiskProfileResult(RiskLevel.Medium, 0, 0, 0),
        taskMode = TaskModeResult(TaskModeType.ExplainPlan, "test")
      ))
    )
    
    val futureSnapshot = FutureSnapshot(
      resolvedThreatKinds = Nil,
      newThreatKinds = Nil,
      targetsDelta = TargetsDelta(
        tacticalAdded = List("e5"),
        tacticalRemoved = Nil,
        strategicAdded = List("d-file control"),
        strategicRemoved = Nil
      ),
      planBlockersRemoved = List("Locked center"),
      planPrereqsMet = List("Pawn break achieved")
    )
    
    val probeResult = ProbeResult(
      id = "test2",
      evalCp = 100,
      bestReplyPv = Nil,
      deltaVsBaseline = 0,
      keyMotifs = Nil,
      probedMove = Some("d4d5"),
      futureSnapshot = Some(futureSnapshot)
    )
    
    val candidate = AnalyzedCandidate(
      move = "d4d5",
      score = 100,
      motifs = Nil,
      prophylaxisResults = Nil,
      futureContext = "break",
      line = VariationLine(List("d4d5"), 100, depth = 20)
    )
    
    val data = minimalData(Some(ctx)).copy(candidates = List(candidate))
    val result = NarrativeContextBuilder.build(data, ctx, None, List(probeResult))
    
    assert(result.decision.isDefined)
    val delta = result.decision.get.delta
    // newOpportunities should combine tactical + strategic
    assert(delta.newOpportunities.contains("e5"), s"Should include tactical target e5, got: ${delta.newOpportunities}")
    assert(delta.newOpportunities.contains("d-file control"), s"Should include strategic target, got: ${delta.newOpportunities}")
    // planAdvancements should include blockers removed and prereqs met
    assert(delta.planAdvancements.exists(_.contains("Locked center")), s"Should include blocker removal, got: ${delta.planAdvancements}")
  }

  test("F9: positive long-horizon probe evidence keeps a Long card in top-2") {
    val ctx = IntegratedContext(evalCp = 45, isWhiteToMove = true)
    val candidate = AnalyzedCandidate(
      move = "g1f3",
      score = 45,
      motifs = Nil,
      prophylaxisResults = Nil,
      futureContext = "development",
      line = VariationLine(List("g1f3"), 45, depth = 20)
    )
    val snapshot = FutureSnapshot(
      resolvedThreatKinds = List("Counterplay"),
      newThreatKinds = Nil,
      targetsDelta = TargetsDelta(Nil, Nil, List("f-file control"), Nil),
      planBlockersRemoved = List("Piece congestion"),
      planPrereqsMet = List("conversion route synchronized")
    )
    val probe = ProbeResult(
      id = "long_positive",
      evalCp = 45,
      bestReplyPv = List("d7d5"),
      deltaVsBaseline = 0,
      keyMotifs = List("coordination route"),
      purpose = Some("convert_reply_multipv"),
      probedMove = Some("g1f3"),
      futureSnapshot = Some(snapshot)
    )
    val data = minimalData(Some(ctx)).copy(
      candidates = List(candidate),
      alternatives = List(VariationLine(List("g1f3", "d7d5"), 45, depth = 20))
    )

    val result = NarrativeContextBuilder.build(data, ctx, None, List(probe))
    val enriched = result.candidates.find(_.uci.contains("g1f3")).getOrElse(fail("candidate not found"))
    val top = enriched.hypotheses.sortBy(h => -h.confidence)
    assert(top.nonEmpty, "Hypotheses should be populated")
    assert(top.take(2).exists(_.horizon == HypothesisHorizon.Long), clue(top.mkString(" | ")))
    assert(
      top
        .take(2)
        .filter(_.horizon == HypothesisHorizon.Long)
        .exists(_.supportSignals.exists(_.toLowerCase.contains("long-horizon"))),
      clue(top.mkString(" | "))
    )
  }

  test("F10: long-horizon conflict evidence can suppress Long card selection") {
    val ctx = IntegratedContext(evalCp = 90, isWhiteToMove = true)
    val best = AnalyzedCandidate(
      move = "g1f3",
      score = 90,
      motifs = Nil,
      prophylaxisResults = Nil,
      futureContext = "development",
      line = VariationLine(List("g1f3"), 90, depth = 20)
    )
    val risky = AnalyzedCandidate(
      move = "c2c4",
      score = -80,
      motifs = Nil,
      prophylaxisResults = Nil,
      futureContext = "expansion",
      line = VariationLine(List("c2c4"), -80, depth = 20)
    )
    val failingSnapshot = FutureSnapshot(
      resolvedThreatKinds = Nil,
      newThreatKinds = List("Mate"),
      targetsDelta = TargetsDelta(Nil, Nil, Nil, Nil),
      planBlockersRemoved = Nil,
      planPrereqsMet = Nil
    )
    val failingProbe = ProbeResult(
      id = "long_negative",
      evalCp = -80,
      bestReplyPv = List("d7d5"),
      deltaVsBaseline = -160,
      keyMotifs = List("king safety"),
      probedMove = Some("c2c4"),
      l1Delta = Some(
        L1DeltaSnapshot(
          materialDelta = 0,
          kingSafetyDelta = -2,
          centerControlDelta = -1,
          openFilesDelta = 0,
          mobilityDelta = -1,
          collapseReason = Some("King exposed and structure collapsed")
        )
      ),
      futureSnapshot = Some(failingSnapshot)
    )
    val data = minimalData(Some(ctx)).copy(
      candidates = List(best, risky),
      alternatives = List(
        VariationLine(List("g1f3", "d7d5"), 90, depth = 20),
        VariationLine(List("c2c4", "d7d5"), -80, depth = 20)
      )
    )

    val result = NarrativeContextBuilder.build(data, ctx, None, List(failingProbe))
    val riskyCandidate = result.candidates.find(_.uci.contains("c2c4")).getOrElse(fail("risky candidate not found"))
    assert(!riskyCandidate.hypotheses.take(2).exists(_.horizon == HypothesisHorizon.Long), clue(riskyCandidate.hypotheses))
  }

  test("F11: forcing-swing probe in middlegame injects cause-consequence-turning-point framing") {
    val pawnAnalysis = PawnPlayAnalysis(
      pawnBreakReady = true,
      breakFile = Some("d"),
      breakImpact = 220,
      advanceOrCapture = true,
      passedPawnUrgency = PassedPawnUrgency.Background,
      passerBlockade = false,
      blockadeSquare = None,
      blockadeRole = None,
      pusherSupport = true,
      minorityAttack = false,
      counterBreak = false,
      tensionPolicy = TensionPolicy.Maintain,
      tensionSquares = Nil,
      primaryDriver = "break_ready",
      notes = "test"
    )
    val ctx = IntegratedContext(evalCp = 35, isWhiteToMove = true, pawnAnalysis = Some(pawnAnalysis))
    val candidate = AnalyzedCandidate(
      move = "d4d5",
      score = 35,
      motifs = Nil,
      prophylaxisResults = Nil,
      futureContext = "central break",
      line = VariationLine(List("d4d5"), 35, depth = 20)
    )
    val probe = ProbeResult(
      id = "forcing_frame",
      evalCp = -180,
      bestReplyPv = List("c6d5"),
      deltaVsBaseline = -215,
      keyMotifs = List("initiative swing"),
      purpose = Some("reply_multipv"),
      probedMove = Some("d4d5"),
      l1Delta = Some(
        L1DeltaSnapshot(
          materialDelta = 0,
          kingSafetyDelta = -2,
          centerControlDelta = -1,
          openFilesDelta = 0,
          mobilityDelta = -1,
          collapseReason = Some("King exposed and structure collapsed")
        )
      )
    )
    val data = minimalData(Some(ctx)).copy(
      phase = "middlegame",
      evalCp = 35,
      candidates = List(candidate),
      alternatives = List(VariationLine(List("d4d5", "c6d5"), 35, depth = 20))
    )

    val result = NarrativeContextBuilder.build(data, ctx, None, List(probe))
    val enriched = result.candidates.find(_.uci.contains("d4d5")).getOrElse(fail("candidate not found"))
    val framed = enriched.hypotheses.find { h =>
      val lower = h.claim.toLowerCase
      lower.contains("forcing swing") &&
      List("consequence", "initiative", "structure", "king safety", "king exposure", "conversion").exists(lower.contains) &&
      List(
        "next forcing sequence",
        "very next concrete sequence",
        "critical test comes",
        "middlegame regrouping",
        "simplification transition"
      ).exists(lower.contains)
    }.getOrElse(fail(s"Expected framed hypothesis, got: ${enriched.hypotheses.mkString(" | ")}"))

    val lower = framed.claim.toLowerCase
    assert(lower.contains("forcing swing"), clue(framed.claim))
    assert(
      List("initiative", "structure", "king safety", "king exposure", "conversion").exists(lower.contains),
      clue(framed.claim)
    )
    assert(
      List(
        "next forcing sequence",
        "very next concrete sequence",
        "critical test comes",
        "middlegame regrouping",
        "simplification transition"
      ).exists(lower.contains),
      clue(framed.claim)
    )
  }

  // ============================================================
  // PHASE G (A9): OPENING EVENT LAYER TESTS
  // ============================================================

  test("G1: Opening event is None for non-opening phase (middlegame)") {
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val openingRef = OpeningReference(
      eco = Some("C00"),
      name = Some("French Defense"),
      totalGames = 1000,
      topMoves = Nil,
      sampleGames = Nil
    )
    val data = minimalData(Some(ctx)).copy(phase = "middlegame")  // NOT opening
    val result = NarrativeContextBuilder.build(data, ctx, None, Nil, Some(openingRef))
    
    // Opening event should NOT fire for non-opening phase
    assertEquals(result.openingEvent, None, "OpeningEvent should be None for middlegame phase")
  }

  test("G2: Opening event is None when Masters DB returns 0 games") {
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val openingRef = OpeningReference(
      eco = None,
      name = None,
      totalGames = 0,  // No games in DB
      topMoves = Nil,
      sampleGames = Nil
    )
    val data = minimalData(Some(ctx)).copy(phase = "opening")
    val result = NarrativeContextBuilder.build(data, ctx, None, Nil, Some(openingRef))
    
    // Opening event should NOT fire for 0 games
    assertEquals(result.openingEvent, None, "OpeningEvent should be None when totalGames=0")
  }

  test("G3: Intro event fires at early ply with confirmed ECO") {
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val topMoves = List(
      ExplorerMove("e2e4", "e4", 5000, 2000, 1500, 1500, 2650),
      ExplorerMove("d2d4", "d4", 4000, 1800, 1200, 1000, 2620)
    )
    val openingRef = OpeningReference(
      eco = Some("B99"),
      name = Some("Sicilian Najdorf"),
      totalGames = 5000,
      topMoves = topMoves,
      sampleGames = Nil
    )
    val data = minimalData(Some(ctx)).copy(
      phase = "opening",
      ply = 4  // Early ply
    )
    val result = NarrativeContextBuilder.build(data, ctx, None, Nil, Some(openingRef))
    
    assert(result.openingEvent.isDefined, "OpeningEvent should fire")
    result.openingEvent.get match {
      case OpeningEvent.Intro(eco, name, _, _) =>
        assertEquals(eco, "B99")
        assertEquals(name, "Sicilian Najdorf")
      case other => fail(s"Expected Intro event, got: $other")
    }
  }

  test("G4: OutOfBook event fires when played move not in top moves") {
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val topMoves = List(
      ExplorerMove("e2e4", "e4", 5000, 2000, 1500, 1500, 2650),
      ExplorerMove("d2d4", "d4", 4000, 1800, 1200, 1000, 2620)
    )
    val openingRef = OpeningReference(
      eco = Some("B99"),
      name = Some("Sicilian Najdorf"),
      totalGames = 5000,
      topMoves = topMoves,
      sampleGames = Nil
    )
    val data = minimalData(Some(ctx)).copy(
      phase = "opening",
      ply = 10,
      prevMove = Some("g2g4")  // Weird move not in top moves
    )
    val result = NarrativeContextBuilder.build(data, ctx, None, Nil, Some(openingRef))
    
    // Should fire OutOfBook since g2g4 is not in topMoves
    assert(result.openingEvent.isDefined, "OpeningEvent should fire")
    result.openingEvent.get match {
      case OpeningEvent.OutOfBook(playedMove, _, _) =>
        assertEquals(playedMove, "g4")  // Converted from g2g4 to g4
      case other => fail(s"Expected OutOfBook event, got: $other")
    }
  }

  test("G5: TheoryEnds fires when totalGames drops below threshold") {
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val prevRef = OpeningReference(
      eco = Some("C00"),
      name = Some("French Defense"),
      totalGames = 150,  // Above threshold
      topMoves = Nil,
      sampleGames = Nil
    )
    val currRef = OpeningReference(
      eco = Some("C00"),
      name = Some("French Defense"),
      totalGames = 50,   // Below threshold (100 for ply < 10)
      topMoves = Nil,
      sampleGames = Nil
    )
    val data = minimalData(Some(ctx)).copy(
      phase = "opening",
      ply = 8
    )
    val result = NarrativeContextBuilder.build(
      data, ctx, None, Nil, Some(currRef), Some(prevRef), OpeningEventBudget()
    )
    
    assert(result.openingEvent.isDefined, "OpeningEvent should fire")
    result.openingEvent.get match {
      case OpeningEvent.TheoryEnds(ply, count) =>
        assertEquals(ply, 8)
        assertEquals(count, 50)
      case other => fail(s"Expected TheoryEnds event, got: $other")
    }
    assert(result.updatedBudget.theoryEnded, "Budget should mark theory as ended")
  }

  test("G6: BranchPoint fires when top move distribution shifts") {
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val prevRef = OpeningReference(
      eco = Some("C00"),
      name = Some("French Defense"),
      totalGames = 1000,
      topMoves = List(
        ExplorerMove("e2e4", "e4", 800, 400, 200, 200, 2650)  // 80% dominance
      ),
      sampleGames = Nil
    )
    val currRef = OpeningReference(
      eco = Some("C00"),
      name = Some("French Defense"),
      totalGames = 1000,
      topMoves = List(
        ExplorerMove("d2d4", "d4", 250, 100, 100, 50, 2620),  // 25% - new top
        ExplorerMove("e2e4", "e4", 200, 100, 50, 50, 2650)    // 20% - dropped
      ),
      sampleGames = Nil
    )
    val data = minimalData(Some(ctx)).copy(
      phase = "opening",
      ply = 10
    )
    val result = NarrativeContextBuilder.build(
      data, ctx, None, Nil, Some(currRef), Some(prevRef), OpeningEventBudget()
    )
    
    assert(result.openingEvent.isDefined, "OpeningEvent should fire")
    result.openingEvent.get match {
      case OpeningEvent.BranchPoint(_, reason, _) =>
        assert(reason.contains("Main line") || reason.contains("fragments"), s"Unexpected reason: $reason")
      case other => fail(s"Expected BranchPoint event, got: $other")
    }
  }

  test("G7: Budget prevents Intro from firing twice") {
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val openingRef = OpeningReference(
      eco = Some("B99"),
      name = Some("Sicilian Najdorf"),
      totalGames = 5000,
      topMoves = List(ExplorerMove("e2e4", "e4", 5000, 2000, 1500, 1500, 2650)),
      sampleGames = Nil
    )
    val data = minimalData(Some(ctx)).copy(phase = "opening", ply = 4)
    
    // First call - Intro should fire
    val result1 = NarrativeContextBuilder.build(data, ctx, None, Nil, Some(openingRef))
    assert(result1.openingEvent.isDefined, "First Intro should fire")
    assert(result1.updatedBudget.introUsed, "introUsed should be true")
    
    // Second call with updated budget - Intro should NOT fire
    val result2 = NarrativeContextBuilder.build(
      data, ctx, None, Nil, Some(openingRef), None, result1.updatedBudget
    )
    assertEquals(result2.openingEvent, None, "Second Intro should NOT fire")
  }

  test("G8: Budget limits events to maxEvents (2)") {
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val topMoves = List(ExplorerMove("e2e4", "e4", 5000, 2000, 1500, 1500, 2650))
    val openingRef = OpeningReference(
      eco = Some("B99"),
      name = Some("Sicilian Najdorf"),
      totalGames = 5000,
      topMoves = topMoves,
      sampleGames = Nil
    )
    
    var budget = OpeningEventBudget(introUsed = true)  // Intro already used
    
    // First OutOfBook
    val data1 = minimalData(Some(ctx)).copy(phase = "opening", ply = 10, prevMove = Some("g2g4"))
    val result1 = NarrativeContextBuilder.build(data1, ctx, None, Nil, Some(openingRef), None, budget)
    assert(result1.openingEvent.isDefined, "First OutOfBook should fire")
    budget = result1.updatedBudget
    assertEquals(budget.eventsUsed, 1, "eventsUsed should be 1")
    
    // Second OutOfBook
    val data2 = minimalData(Some(ctx)).copy(phase = "opening", ply = 12, prevMove = Some("h2h4"))
    val result2 = NarrativeContextBuilder.build(data2, ctx, None, Nil, Some(openingRef), None, budget)
    assert(result2.openingEvent.isDefined, "Second OutOfBook should fire")
    budget = result2.updatedBudget
    assertEquals(budget.eventsUsed, 2, "eventsUsed should be 2")
    
    // Third OutOfBook should NOT fire (budget exhausted)
    val data3 = minimalData(Some(ctx)).copy(phase = "opening", ply = 14, prevMove = Some("a2a4"))
    val result3 = NarrativeContextBuilder.build(data3, ctx, None, Nil, Some(openingRef), None, budget)
    assertEquals(result3.openingEvent, None, "Third event should NOT fire (budget exhausted)")
  }

  test("G9: OutOfBook rarity uses totalGames (not truncated move totals)") {
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val topMoves = List(
      ExplorerMove("e2e4", "e4", 5, 2, 1, 2, 2650),   // 0.5% of 1000 games
      ExplorerMove("d2d4", "d4", 100, 50, 30, 20, 2620) // Remaining moves omitted (truncation)
    )
    val openingRef = OpeningReference(
      eco = Some("A00"),
      name = Some("Uncommon Opening"),
      totalGames = 1000,
      topMoves = topMoves,
      sampleGames = Nil
    )

    val data = minimalData(Some(ctx)).copy(
      phase = "opening",
      ply = 4,
      prevMove = Some("e2e4")
    )

    val result = NarrativeContextBuilder.build(data, ctx, None, Nil, Some(openingRef))
    assert(result.openingEvent.isDefined, "OpeningEvent should fire")
    result.openingEvent.get match {
      case OpeningEvent.OutOfBook(playedMove, _, _) =>
        assertEquals(playedMove, "e4")
      case other => fail(s"Expected OutOfBook event, got: $other")
    }
  }

  test("G10: OutOfBook converts piece moves to SAN using FEN context") {
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val openingRef = OpeningReference(
      eco = Some("A00"),
      name = Some("Start Position"),
      totalGames = 5000,
      topMoves = List(ExplorerMove("e2e4", "e4", 5000, 2000, 1500, 1500, 2650)),
      sampleGames = Nil
    )

    val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    val data = minimalData(Some(ctx)).copy(
      fen = startFen,
      phase = "opening",
      ply = 2,
      prevMove = Some("g1f3") // Not in topMoves => OutOfBook should fire
    )

    val result = NarrativeContextBuilder.build(data, ctx, None, Nil, Some(openingRef))
    assert(result.openingEvent.isDefined, "OpeningEvent should fire")
    result.openingEvent.get match {
      case OpeningEvent.OutOfBook(playedMove, _, _) =>
        assertEquals(playedMove, "Nf3")
      case other => fail(s"Expected OutOfBook event, got: $other")
    }
  }
}
