package lila.llm.analysis

import munit.FunSuite
import chess.*
import lila.llm.model.*
import lila.llm.analysis.L3.*
import lila.llm.analysis.PlanMatcher.ActivePlans

class TransitionLogicTest extends FunSuite {

  // --- CONTINUATION ---

  test("Continuation: Same plan persists, momentum increases") {
    val prevPlan = Plan.KingsideAttack(Color.White)
    val currActive = makeActivePlans(Plan.KingsideAttack(Color.White), 0.8)
    val ctx = makeContext()
    
    val result = TransitionAnalyzer.analyze(
      currentPlans = currActive,
      previousPlan = Some(prevPlan),
      previousMomentum = 0.7,
      planHistory = List(PlanId.KingsideAttack),
      ctx = ctx
    )
    
    assertEquals(result.transitionType, TransitionType.Continuation)
    // Momentum should increase: 0.7 + 0.1 boost = 0.8
    assert(Math.abs(result.momentum - 0.8) < 0.0001, s"Expected ~0.8, got ${result.momentum}")
    assert(result.momentum > 0.7)
  }

  // --- NATURAL SHIFT ---

  test("NaturalShift: Phase change triggers shift") {
    val prevPlan = Plan.KingsideAttack(Color.White)
    val currActive = makeActivePlans(Plan.Simplification(Color.White), 0.6)
    val ctx = makeContext(evalCp = 300) // Winning, natural to simplify
    
    val result = TransitionAnalyzer.analyze(
      currentPlans = currActive,
      previousPlan = Some(prevPlan),
      previousMomentum = 0.8,
      planHistory = List(PlanId.KingsideAttack),
      ctx = ctx
    )
    
    assertEquals(result.transitionType, TransitionType.NaturalShift)
    assertEquals(result.momentum, 0.5) // Reset to neutral
  }

  // --- FORCED PIVOT ---

  test("ForcedPivot: Tactical threat forces plan abandonment") {
    val prevPlan = Plan.KingsideAttack(Color.White)
    val currActive = makeActivePlans(Plan.DefensiveConsolidation(Color.White), 0.7)
    val ctx = makeContext(tacticalThreatToUs = true)
    
    val result = TransitionAnalyzer.analyze(
      currentPlans = currActive,
      previousPlan = Some(prevPlan),
      previousMomentum = 0.9,
      planHistory = List(PlanId.KingsideAttack),
      ctx = ctx
    )
    
    assertEquals(result.transitionType, TransitionType.ForcedPivot)
    assertEquals(result.momentum, 0.3) // Low, forced change
  }

  // --- OPPORTUNISTIC ---

  test("Opportunistic: Unexpected attacking opportunity") {
    val prevPlan = Plan.DefensiveConsolidation(Color.White)
    val currActive = makeActivePlans(Plan.KingsideAttack(Color.White), 0.9)
    val ctx = makeContext(tacticalThreatToThem = true)
    
    val result = TransitionAnalyzer.analyze(
      currentPlans = currActive,
      previousPlan = Some(prevPlan),
      previousMomentum = 0.5,
      planHistory = List(PlanId.DefensiveConsolidation),
      ctx = ctx
    )
    
    assertEquals(result.transitionType, TransitionType.Opportunistic)
    assertEquals(result.momentum, 0.6) // Moderate, new attack
  }

  // --- MOMENTUM DECAY ---

  test("Momentum: Continuation caps at 1.0") {
    val prevPlan = Plan.KingsideAttack(Color.White)
    val currActive = makeActivePlans(Plan.KingsideAttack(Color.White), 0.9)
    val ctx = makeContext()
    
    val result = TransitionAnalyzer.analyze(
      currentPlans = currActive,
      previousPlan = Some(prevPlan),
      previousMomentum = 0.95, // Very high
      planHistory = List(PlanId.KingsideAttack),
      ctx = ctx
    )
    
    assertEquals(result.momentum, 1.0) // Capped at max
  }

  // --- EXPLAIN TRANSITION ---

  test("ExplainTransition: Attack to Simplification when winning") {
    val prev = Plan.KingsideAttack(Color.White)
    val curr = Plan.Simplification(Color.White)
    val ctx = makeContext(evalCp = 300, isWhiteToMove = true)
    
    val explanation = TransitionAnalyzer.explainTransition(prev, curr, ctx)
    
    assert(explanation.toLowerCase.contains("simplifying"), s"Expected 'simplifying', got: $explanation")
    assert(explanation.contains("advantage"), s"Expected 'advantage', got: $explanation")
  }

  // --- PLAN HISTORY ---

  test("PlanHistory: Maintains last 3 plans") {
    val currActive = makeActivePlans(Plan.Promotion(Color.White), 0.9)
    val ctx = makeContext()
    
    val result = TransitionAnalyzer.analyze(
      currentPlans = currActive,
      previousPlan = Some(Plan.PassedPawnPush(Color.White)),
      previousMomentum = 0.6,
      planHistory = List(PlanId.PassedPawnPush, PlanId.KingActivation, PlanId.Simplification),
      ctx = ctx
    )
    
    assertEquals(result.planHistory.length, 3)
    assertEquals(result.planHistory.head, PlanId.Promotion) // Current added first
  }

  // --- HELPER METHODS ---

  private def makeActivePlans(plan: Plan, score: Double): ActivePlans = {
    val pm = PlanMatch(plan, score, Nil)
    ActivePlans(
      primary = pm,
      secondary = None,
      suppressed = Nil,
      allPlans = List(pm)
    )
  }

  private def makeContext(
    evalCp: Int = 0,
    tacticalThreatToUs: Boolean = false,
    tacticalThreatToThem: Boolean = false,
    isWhiteToMove: Boolean = true
  ): IntegratedContext = {
    val mockNature = NatureResult(NatureType.Static, 0, 0, 0, false)
    val mockCrit = CriticalityResult(CriticalityType.Normal, 0, None, 0)
    val mockTopo = ChoiceTopologyResult(ChoiceTopologyType.StyleChoice, 0, 0, None, 0, 0, None)
    val mockPhase = GamePhaseResult(GamePhaseType.Middlegame, 30, true, 4)
    val mockSimp = SimplifyBiasResult(false, 0, false, false)
    val mockDraw = DrawBiasResult(false, false, false, false, false)
    val mockRisk = RiskProfileResult(RiskLevel.Medium, 0, 0, 0)
    val mockTask = TaskModeResult(TaskModeType.ExplainPlan, "test")
    
    val mockClass = PositionClassification(mockNature, mockCrit, mockTopo, mockPhase, mockSimp, mockDraw, mockRisk, mockTask)
    
    // Create threat lists with proper numeric values
    val ownThreatsList = if (tacticalThreatToUs)
      List(Threat(ThreatKind.Material, 300, 1, Nil, Nil, Nil, None, 1))
    else Nil
    
    val oppThreatsList = if (tacticalThreatToThem)
      List(Threat(ThreatKind.Material, 300, 1, Nil, Nil, Nil, None, 1))
    else Nil
    
    val severity = if (tacticalThreatToUs) ThreatSeverity.Urgent else ThreatSeverity.Low
    val oppSeverity = if (tacticalThreatToThem) ThreatSeverity.Urgent else ThreatSeverity.Low
    
    val mockOwnThreats = ThreatAnalysis(ownThreatsList, DefenseAssessment(severity, None, Nil, false, false, 0, ""), severity, tacticalThreatToUs, false, !tacticalThreatToUs, tacticalThreatToUs, false, false, true, 0, "none", false)
    val mockOppThreats = ThreatAnalysis(oppThreatsList, DefenseAssessment(oppSeverity, None, Nil, false, false, 0, ""), oppSeverity, tacticalThreatToThem, false, !tacticalThreatToThem, tacticalThreatToThem, false, false, true, 0, "none", false)

    IntegratedContext(
      evalCp = evalCp,
      classification = Some(mockClass),
      pawnAnalysis = None,
      opponentPawnAnalysis = None,
      threatsToUs = Some(mockOwnThreats),
      threatsToThem = Some(mockOppThreats),
      isWhiteToMove = isWhiteToMove
    )
  }
}
