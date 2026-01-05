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
      nature = PositionNature(lila.llm.analysis.NatureType.Dynamic, 0.5, 0.5, "Dynamic position"),
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
  
  test("A1: buildThreatTable populates ThreatRows from IntegratedContext.threatsToUs") {
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
      nature = NatureResult(NatureType.Dynamic, 3, 2, 1, false),
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
    val openFiles = narrativeCtx.l1Snapshot.openFiles
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
      line = candidateLine
    )
    
    val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val data = minimalData().copy(fen = startFen, candidates = List(candidate))
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.candidates.nonEmpty, "Should have candidates")
    assertEquals(narrativeCtx.candidates.head.move, "e4", "UCI e2e4 should convert to SAN e4")
    assertEquals(narrativeCtx.candidates.head.annotation, "!")
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
      nature = NatureResult(NatureType.Dynamic, 3, 2, 1, false),
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

  test("describeHierarchical outputs PHASE section (A8)") {
    val ctx = lila.llm.model.NarrativeContext(
      header = lila.llm.model.ContextHeader("Middlegame", "Normal", "StyleChoice", "Medium", "ExplainPlan"),
      summary = lila.llm.model.NarrativeSummary("Kingside Attack", None, "StyleChoice", "Maintain", "+50"),
      threats = lila.llm.model.ThreatTable(Nil, Nil),
      pawnPlay = lila.llm.model.PawnPlayTable(false, None, "Low", "Maintain", "No breaks", "Background", None, false, "quiet"),
      plans = lila.llm.model.PlanTable(Nil, Nil),
      l1Snapshot = lila.llm.model.L1Snapshot("=", None, None, None, None, None, Nil),
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

    assert(narrativeCtx.probeRequests.nonEmpty, "Should emit at least one probe request")
    val req = narrativeCtx.probeRequests.head
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

    assertEquals(narrativeCtx.probeRequests, Nil, "Should not request probes for already-represented plans")
  }

  test("Probe: score threshold prevents noisy probes") {
    val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val plan = PlanMatch(plan = Plan.KingsideAttack(Color.White), score = 0.69, evidence = Nil)
    val pv1 = lila.llm.model.strategic.VariationLine(moves = List("e2e4"), scoreCp = 0, mate = None, tags = Nil)

    val data = minimalData().copy(fen = startFen, plans = List(plan), alternatives = List(pv1))
    val ctx = IntegratedContext(evalCp = 0, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)

    assertEquals(narrativeCtx.probeRequests, Nil, "Plan score below threshold should not trigger probes")
  }

  // ============================================================
  // B-AXIS: META SIGNALS VERIFICATION
  // ============================================================

  test("B1: ChoiceType derived from classification.choiceTopology (OnlyMove)") {
    val classification = PositionClassification(
      nature = NatureResult(NatureType.Dynamic, 2, 1, 0, false),
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

  test("B5: Targets extracted from threatsToUs/Them with deduplication") {
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
    
    // Attack targets (from threatsToThem) - should be deduplicated
    assertEquals(targets.attackTargets.map(_._1).toSet, Set("e5", "d4"), "Should deduplicate attack squares")
    assert(targets.attackTargets.head._2.contains("Material"), "Reason should include kind")
    
    // Defend targets (from threatsToUs)
    assertEquals(targets.defendTargets.map(_._1), List("c3"))
  }

  test("B8: PlanConcurrency shows conflict when secondary was downweighted") {
    val plan1 = PlanMatch(plan = Plan.KingsideAttack(Color.White), score = 0.90, evidence = Nil)
    val plan2 = PlanMatch(plan = Plan.Simplification(Color.White), score = 0.60, evidence = Nil)
    
    val compatEvent = CompatibilityEvent(
      planName = "Simplification into Endgame", // Must match Plan.Simplification.name
      originalScore = 0.75,
      finalScore = 0.60,
      delta = -0.15,
      reason = "conflict: attack ⟂ simplification",
      eventType = "downweight"
    )
    
    val activePlans = PlanMatcher.ActivePlans(
      primary = plan1,
      secondary = Some(plan2),
      suppressed = Nil,
      allPlans = List(plan1, plan2),
      compatibilityEvents = List(compatEvent)
    )
    
    val planSeq = PlanSequence(
      currentPlans = activePlans,
      previousPlan = None,
      transitionType = TransitionType.Continuation,
      momentum = 0.8,
      planHistory = Nil
    )
    val data = minimalData().copy(plans = List(plan1, plan2), planSequence = Some(planSeq))
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.meta.isDefined)
    val concurrency = narrativeCtx.meta.get.planConcurrency
    
    assertEquals(concurrency.primary, "Kingside Attack")
    assertEquals(concurrency.secondary, Some("Simplification into Endgame"))
    assertEquals(concurrency.relationship, "⟂ conflict")
  }

  test("B8: PlanConcurrency shows independent when no compatibility events") {
    val plan1 = PlanMatch(plan = Plan.CentralControl(Color.White), score = 0.80, evidence = Nil)
    val plan2 = PlanMatch(plan = Plan.PieceActivation(Color.White), score = 0.70, evidence = Nil)
    
    val activePlans = PlanMatcher.ActivePlans(
      primary = plan1,
      secondary = Some(plan2),
      suppressed = Nil,
      allPlans = List(plan1, plan2),
      compatibilityEvents = Nil // No events
    )
    
    val planSeq = PlanSequence(
      currentPlans = activePlans,
      previousPlan = None,
      transitionType = TransitionType.Continuation,
      momentum = 0.7,
      planHistory = Nil
    )
    val data = minimalData().copy(plans = List(plan1, plan2), planSequence = Some(planSeq))
    val ctx = IntegratedContext(evalCp = 50, isWhiteToMove = true)
    val narrativeCtx = NarrativeContextBuilder.build(data, ctx, None)
    
    assert(narrativeCtx.meta.isDefined)
    assertEquals(narrativeCtx.meta.get.planConcurrency.relationship, "independent")
  }

  test("B8: PlanConcurrency shows synergy when secondary was boosted") {
    val plan1 = PlanMatch(plan = Plan.KingsideAttack(Color.White), score = 0.85, evidence = Nil)
    val plan2 = PlanMatch(plan = Plan.RookActivation(Color.White), score = 0.75, evidence = Nil)
    
    val compatEvent = CompatibilityEvent(
      planName = "Rook Activation",  // Must match Plan.RookActivation.name
      originalScore = 0.60,
      finalScore = 0.75,
      delta = 0.15,
      reason = "synergy: attack + rook activation",
      eventType = "boosted"
    )
    
    val activePlans = PlanMatcher.ActivePlans(
      primary = plan1,
      secondary = Some(plan2),
      suppressed = Nil,
      allPlans = List(plan1, plan2),
      compatibilityEvents = List(compatEvent)
    )
    
    val planSeq = PlanSequence(
      currentPlans = activePlans,
      previousPlan = None,
      transitionType = TransitionType.Continuation,
      momentum = 0.9,
      planHistory = Nil
    )
    val data = minimalData().copy(plans = List(plan1, plan2), planSequence = Some(planSeq))
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
      line = VariationLine(List("e2e4", "e7e5"), 30, None, None, Nil)
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
    assertEquals(cGhost.get.planAlignment, "Alt Plan")
    assert(cGhost.get.whyNot.get.contains("-330 cp"))
    
    // Verify meta summary
    assert(narrativeCtx.meta.isDefined)
    assert(narrativeCtx.meta.get.whyNot.isDefined)
    assert(narrativeCtx.meta.get.whyNot.get.contains("'g2g4' is refuted"))
  }
}
