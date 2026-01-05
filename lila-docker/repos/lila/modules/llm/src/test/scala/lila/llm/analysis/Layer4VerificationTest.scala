package lila.llm.analysis

import munit.FunSuite
import chess.*
import lila.llm.model.*
import lila.llm.analysis.L3.*
import lila.llm.model.Motif.*
import chess.format.Uci

class Layer4VerificationTest extends FunSuite {

  // --- CP STANDARDIZATION & POV ---

  test("Plan Recognition: Evaluation Boundary (199 vs 201 CP)") {
    val fen = "6k1/5ppp/8/3q4/3Q4/8/5P1P/6K1 w - - 0 1"
    val result199 = analyzeWithMock(fen = fen, pv = List("d4d5"), evalCp = 199)
    assert(!result199.topPlans.exists(_.plan.isInstanceOf[Plan.Simplification]), "199 CP should not trigger Simplification")

    val result201 = analyzeWithMock(fen = fen, pv = List("d4d5"), evalCp = 201)
    assert(result201.topPlans.exists(_.plan.isInstanceOf[Plan.Simplification]), "201 CP should trigger Simplification")
  }

  // --- THREAT POV & CHESS-CORRECT GATING ---
  // threatsToUs = threats opponent makes TO US (defensive pressure)
  // threatsToThem = threats WE make TO THEM (attacking opportunity)

  test("Threat POV: Defensive pressure should ALLOW simplification (pressure relief)") {
    val fen = "6k1/5ppp/8/3q4/3Q4/8/5P1P/6K1 w - - 0 1"
    
    // Position: White to move. White is under threat (threatsToUs = Urgent).
    // Chess logic: Simplification can relieve defensive pressure.
    val result = analyzeWithMock(
      fen = fen,
      pv = List("d4d5"),
      evalCp = 500,
      ownThreatSeverity = ThreatSeverity.Urgent,  // We are under threat (threatsToUs)
      opponentThreatSeverity = ThreatSeverity.Low  // We have no attacking threats (threatsToThem)
    )
    
    assert(result.topPlans.exists(_.plan.isInstanceOf[Plan.Simplification]), 
      "Defensive pressure should ALLOW simplification (pressure relief)")
  }

  test("Threat POV: Attacking opportunity should BLOCK simplification (opportunity loss)") {
    val fen = "6k1/5ppp/8/3q4/3Q4/8/5P1P/6K1 w - - 0 1"
    
    // Position: White to move. White has an attacking threat (threatsToThem = Urgent).
    // Chess logic: Don't simplify when holding attacking threats.
    val result = analyzeWithMock(
      fen = fen,
      pv = List("d4d5"),
      evalCp = 500,
      ownThreatSeverity = ThreatSeverity.Low,      // We are NOT under threat
      opponentThreatSeverity = ThreatSeverity.Urgent  // We HAVE attacking threats (threatsToThem)
    )
    
    assert(!result.topPlans.exists(_.plan.isInstanceOf[Plan.Simplification]), 
      "Attacking opportunity should BLOCK simplification (opportunity loss)")
  }

  test("Threat POV: No significant threats = normal simplification based on eval") {
    val fen = "6k1/5ppp/8/3q4/3Q4/8/5P1P/6K1 w - - 0 1"
    
    val result = analyzeWithMock(
      fen = fen,
      pv = List("d4d5"),
      evalCp = 500,
      ownThreatSeverity = ThreatSeverity.Low,
      opponentThreatSeverity = ThreatSeverity.Low
    )
    
    assert(result.topPlans.exists(_.plan.isInstanceOf[Plan.Simplification]), 
      "No threats = normal eval-based simplification should work")
  }

  // --- PHASE GATING ---

  test("Phase Gating: Zugzwang should only trigger in Endgame") {
    val middlegameResult = analyzeWithMock(
      phaseType = GamePhaseType.Middlegame,
      forceZugzwangMotif = true
    )
    assert(!middlegameResult.topPlans.exists(_.plan.isInstanceOf[Plan.Zugzwang]), "Zugzwang should be ignored in Middlegame")

    val endgameResult = analyzeWithMock(
      phaseType = GamePhaseType.Endgame,
      forceZugzwangMotif = true
    )
    assert(endgameResult.topPlans.exists(_.plan.isInstanceOf[Plan.Zugzwang]), "Zugzwang should trigger in Endgame")
  }

  // --- BLOCKADE EVIDENCE & PENALTY ---

  test("Blockade: High fidelity evidence using Phase 3 data") {
    val withData = analyzeWithMock(
      side = Color.Black,
      opponentPasserBlockade = true,
      blockadeSquare = Some(Square.D5),
      blockadeRole = Some(Knight)
    )
    
    val plan = withData.topPlans.find(_.plan.isInstanceOf[Plan.Blockade])
    assert(plan.isDefined, "Blockade plan should be detected")
    val evidence = plan.get.evidence.map(_.description)
    assert(evidence.exists(_.contains("successfully blockaded by knight on d5")), s"Evidence should contain detailed info: $evidence")
  }

  test("Blockade: Generic evidence when Phase 3 data is missing (score remains high)") {
    val minimalResult = analyzeWithMock(
      opponentPasserBlockade = true,
      blockadeSquare = None,
      blockadeRole = None,
      forceBlockadeMotif = false
    )
    
    val plan = minimalResult.topPlans.find(_.plan.isInstanceOf[Plan.Blockade])
    assert(plan.isDefined, "Blockade plan should still be detected")
    // Score is evidence-based: 0.9 with detailed evidence, 0.7 without
    assert(plan.get.score >= 0.7, s"Blockade score should be >= 0.7, got ${plan.get.score}")
    // Evidence should be generic when Phase 3 data is missing
    val evidence = plan.get.evidence.map(_.description)
    assert(evidence.exists(_.contains("halted")), s"Evidence should be generic (halted), found: $evidence")
  }

  // --- PROPHYLAXIS ---

  test("Prophylaxis: High fidelity evidence using Threat details") {
    val result = analyzeWithMock(
      pv = List("g1h1"),
      ownThreatSeverity = ThreatSeverity.Important,
      prophylaxisNeeded = true,
      threatKind = Some(ThreatKind.Mate)
    )
    
    val plan = result.topPlans.find(_.plan.isInstanceOf[Plan.Prophylaxis])
    assert(plan.isDefined, "Prophylaxis plan should be detected")
    val evidence = plan.get.evidence.map(_.description)
    assert(evidence.exists(_.contains("neutralize mate threat")), s"Evidence should mention specific threat kind: $evidence")
  }

  // --- SACRIFICE ---

  test("Sacrifice: Risk-aware scoring") {
    val lowRiskResult = analyzeWithMock(
      evalCp = 100,
      forceSacrificeMotif = true,
      riskLevel = RiskLevel.Low
    )
    val highRiskResult = analyzeWithMock(
      evalCp = 100,
      forceSacrificeMotif = true,
      riskLevel = RiskLevel.High,
      kingExposure = 10
    )
    
    val scoreLow = lowRiskResult.topPlans.find(_.plan.isInstanceOf[Plan.Sacrifice]).map(_.score).getOrElse(0.0)
    val scoreHigh = highRiskResult.topPlans.find(_.plan.isInstanceOf[Plan.Sacrifice]).map(_.score).getOrElse(0.0)
    
    // High risk (+0.3) + King exposure (+0.2) = 0.5 bonus expected
    assert(scoreHigh > scoreLow, s"High risk sacrifice should score higher ($scoreHigh) than low risk ($scoreLow)")
    assert(scoreHigh - scoreLow >= 0.4, s"Score difference should be >= 0.4 from risk bonus, got ${scoreHigh - scoreLow}")
  }

  test("Sacrifice: Defensive context should lower base score") {
    val attackingResult = analyzeWithMock(
      evalCp = 200, // Attacking context
      forceSacrificeMotif = true
    )
    val defensiveResult = analyzeWithMock(
      evalCp = -200, // Defensive context (losing)
      forceSacrificeMotif = true
    )
    
    val scoreAttack = attackingResult.topPlans.find(_.plan.isInstanceOf[Plan.Sacrifice]).map(_.score).getOrElse(0.0)
    val scoreDefend = defensiveResult.topPlans.find(_.plan.isInstanceOf[Plan.Sacrifice]).map(_.score).getOrElse(0.0)
    
    // Attacking: 1.0 + evalBonus(0.3) = 1.3, Defensive: 0.6
    assert(scoreAttack > scoreDefend, s"Attacking: $scoreAttack should be > Defensive: $scoreDefend")
    assert(scoreAttack >= 1.2 && scoreDefend < 0.8, "Base score thresholds should be respected")
  }

  // --- SIDE-AWARE MOTIFS ---

  test("Side-Aware: Black motifs should be correctly handled") {
    val fen = "6k1/5ppp/8/8/8/8/5PPP/6K1 b - - 0 1"
    
    val result = analyzeWithMock(
      fen = fen,
      side = Color.Black,
      evalCp = -500, // Black winning
      forceSacrificeMotif = true // Should be flipped to Black in helper
    )
    
    val plan = result.topPlans.find(_.plan.isInstanceOf[Plan.Sacrifice])
    assert(plan.isDefined, "Black should detect Sacrifice plan")
    assert(plan.get.plan.color == Color.Black, s"Plan color should be Black, found ${plan.get.plan.color}")
  }

  // --- Mocking Helper ---

  private def analyzeWithMock(
      fen: String = "6k1/5ppp/8/8/8/8/8/6K1 w - - 0 1",
      side: Color = Color.White,
      pv: List[String] = Nil,
      evalCp: Int = 0,
      phaseType: GamePhaseType = GamePhaseType.Middlegame,
      ownThreatSeverity: ThreatSeverity = ThreatSeverity.Low,
      opponentThreatSeverity: ThreatSeverity = ThreatSeverity.Low,
      threatKind: Option[ThreatKind] = None,
      prophylaxisNeeded: Boolean = false,
      blockadeSquare: Option[Square] = None,
      blockadeRole: Option[Role] = None,
      opponentPasserBlockade: Boolean = false,
      riskLevel: RiskLevel = RiskLevel.Medium,
      kingExposure: Int = 0,
      forceSacrificeMotif: Boolean = false,
      forceSpaceMotif: Boolean = false,
      forceZugzwangMotif: Boolean = false,
      forceBlockadeMotif: Boolean = false
  ): PlanScoringResult = {
    
    val baseMotifs = if (pv.nonEmpty) MoveAnalyzer.tokenizePv(fen, pv) else Nil
    
    // Ensure forced motifs match the side
    val extraMotifs = 
      (if (forceSacrificeMotif) {
         val dummyMove = Uci(pv.headOption.getOrElse("e2e4")).collect { case m: Uci.Move => m }.get
         val mSide = side // Use the test side
         List(Motif.Capture(Pawn, Queen, dummyMove.dest, CaptureType.Sacrifice, mSide, 0, None))
      } else Nil) ++
      (if (forceSpaceMotif) List(Motif.SpaceAdvantage(side, 3, 0, None)) else Nil) ++
      (if (forceZugzwangMotif) List(Motif.Zugzwang(side, 0, None)) else Nil) ++
      (if (forceBlockadeMotif) List(Motif.Centralization(Knight, Square.D4, side, 0, None)) else Nil)

    // Flip baseMotifs color if side is Black (MoveAnalyzer usually handles this, but here we mock it)
    val finalMotifs = (baseMotifs ++ extraMotifs).map {
      case m: Motif.Capture if side == Color.Black && m.color == Color.White => m.copy(color = Color.Black)
      case m: Motif.PawnAdvance if side == Color.Black && m.color == Color.White => m.copy(color = Color.Black)
      case m => m
    }

    // --- Robust Mocks (No nulls) ---
    val mockNature = NatureResult(NatureType.Static, 0, 0, 0, false)
    val mockCrit = CriticalityResult(CriticalityType.Normal, 0, None, 0)
    val mockTopo = ChoiceTopologyResult(ChoiceTopologyType.StyleChoice, 0, 0, None, 0, 0, None)
    val mockPhase = GamePhaseResult(phaseType, 30, true, 4)
    val mockSimp = SimplifyBiasResult(false, 0, false, false)
    val mockDraw = DrawBiasResult(false, false, false, false, false)
    val mockRisk = RiskProfileResult(riskLevel, 0, 0, kingExposure)
    val mockTask = TaskModeResult(TaskModeType.ExplainPlan, "test")

    val mockClass = PositionClassification(mockNature, mockCrit, mockTopo, mockPhase, mockSimp, mockDraw, mockRisk, mockTask)
    
    val mockPawn = PawnPlayAnalysis(false, None, 0, false, PassedPawnUrgency.Background, false, None, None, false, false, false, TensionPolicy.Maintain, Nil, "none", "")
    val mockOppPawn = PawnPlayAnalysis(false, None, 0, false, PassedPawnUrgency.Background, opponentPasserBlockade, blockadeSquare, blockadeRole, false, false, false, TensionPolicy.Maintain, Nil, "none", "")
    
    // Create threat lists with proper numeric values for tactical/strategic detection
    // Tactical: turnsToImpact <= 2, lossIfIgnoredCp >= 200
    // Strategic: turnsToImpact <= 5, lossIfIgnoredCp >= 100
    val ownThreatsList = if (ownThreatSeverity == ThreatSeverity.Urgent)
      List(Threat(threatKind.getOrElse(ThreatKind.Material), 300, 1, Nil, Nil, Nil, None, 1))  // Tactical
    else if (ownThreatSeverity == ThreatSeverity.Important)
      List(Threat(threatKind.getOrElse(ThreatKind.Material), 150, 3, Nil, Nil, Nil, None, 1))  // Strategic
    else Nil
    
    val oppThreatsList = if (opponentThreatSeverity == ThreatSeverity.Urgent)
      List(Threat(ThreatKind.Material, 300, 1, Nil, Nil, Nil, None, 1))  // Tactical
    else if (opponentThreatSeverity == ThreatSeverity.Important)
      List(Threat(ThreatKind.Material, 150, 3, Nil, Nil, Nil, None, 1))  // Strategic
    else Nil
    
    val mockOwnThreats = ThreatAnalysis(ownThreatsList, DefenseAssessment(ownThreatSeverity, None, Nil, false, prophylaxisNeeded, 0, ""), ownThreatSeverity, ownThreatSeverity == ThreatSeverity.Urgent, false, ownThreatSeverity == ThreatSeverity.Low, ownThreatSeverity != ThreatSeverity.Low, false, prophylaxisNeeded, true, 0, "none", false)
    val mockOppThreats = ThreatAnalysis(oppThreatsList, DefenseAssessment(opponentThreatSeverity, None, Nil, false, false, 0, ""), opponentThreatSeverity, opponentThreatSeverity == ThreatSeverity.Urgent, false, opponentThreatSeverity == ThreatSeverity.Low, opponentThreatSeverity != ThreatSeverity.Low, false, false, true, 0, "none", false)

    val ctx = IntegratedContext(
      evalCp = evalCp,
      classification = Some(mockClass),
      pawnAnalysis = Some(mockPawn),
      opponentPawnAnalysis = Some(mockOppPawn),
      threatsToUs = Some(mockOwnThreats),    // Threats OPPONENT makes TO US
      threatsToThem = Some(mockOppThreats),  // Threats WE make TO THEM
      isWhiteToMove = (side == Color.White && fen.contains(" w ")) || (side == Color.Black && fen.contains(" b "))
    )
    
    PlanMatcher.matchPlans(finalMotifs, ctx, side)
  }
}
