package lila.llm.analysis

import munit.FunSuite
import chess.*
import lila.llm.model.*
import lila.llm.model.strategic.PlanContinuity
import lila.llm.analysis.L3.*
import lila.llm.analysis.PlanMatcher.ActivePlans

class TransitionLogicTest extends FunSuite {

  // --- CONTINUATION ---

  test("Continuation: Same plan persists") {
    val currActive = makeActivePlans(Plan.KingsideAttack(Color.White), 0.8)
    // Previous continuity was "Kingside Attack" — same as current plan
    val continuity = Some(PlanContinuity("Kingside Attack", Some("KingsideAttack"), 3, 10))
    val ctx = makeContext()
    
    val summary = TransitionAnalyzer.analyze(
      currentPlans = currActive,
      continuityOpt = continuity,
      ctx = ctx
    )
    
    assertEquals(summary.transitionType, TransitionType.Continuation)
    assert(summary.momentum > 0.5, s"Expected momentum > 0.5, got ${summary.momentum}")
  }

  // --- NATURAL SHIFT ---

  test("NaturalShift: Different plan without threat triggers shift") {
    val currActive = makeActivePlans(Plan.Simplification(Color.White), 0.6)
    // Previous continuity was "Kingside Attack" — different from current
    val continuity = Some(PlanContinuity("Kingside Attack", Some("KingsideAttack"), 5, 8))
    val ctx = makeContext(evalCp = 300)
    
    val summary = TransitionAnalyzer.analyze(
      currentPlans = currActive,
      continuityOpt = continuity,
      ctx = ctx
    )
    
    assertEquals(summary.transitionType, TransitionType.NaturalShift)
    assertEquals(summary.momentum, 0.5) // Reset to neutral
  }

  // --- FORCED PIVOT ---

  test("ForcedPivot: Tactical threat forces plan abandonment") {
    val currActive = makeActivePlans(Plan.DefensiveConsolidation(Color.White), 0.7)
    val continuity = Some(PlanContinuity("Kingside Attack", Some("KingsideAttack"), 4, 6))
    val ctx = makeContext(tacticalThreatToUs = true)
    
    val summary = TransitionAnalyzer.analyze(
      currentPlans = currActive,
      continuityOpt = continuity,
      ctx = ctx
    )
    
    assertEquals(summary.transitionType, TransitionType.ForcedPivot)
    assertEquals(summary.momentum, 0.3)
  }

  // --- OPPORTUNISTIC ---

  test("Opportunistic: Unexpected attacking opportunity") {
    val currActive = makeActivePlans(Plan.KingsideAttack(Color.White), 0.9)
    val continuity = Some(PlanContinuity("Defensive Consolidation", Some("DefensiveConsolidation"), 2, 12))
    val ctx = makeContext(tacticalThreatToThem = true)
    
    val summary = TransitionAnalyzer.analyze(
      currentPlans = currActive,
      continuityOpt = continuity,
      ctx = ctx
    )
    
    assertEquals(summary.transitionType, TransitionType.Opportunistic)
    assertEquals(summary.momentum, 0.6)
  }

  // --- OPENING (no continuity) ---

  test("Opening: No previous continuity") {
    val currActive = makeActivePlans(Plan.KingsideAttack(Color.White), 0.9)
    val ctx = makeContext()
    
    val summary = TransitionAnalyzer.analyze(
      currentPlans = currActive,
      continuityOpt = None,
      ctx = ctx
    )
    
    assertEquals(summary.transitionType, TransitionType.Opening)
    assertEquals(summary.momentum, 0.5)
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
    val mockNature = NatureResult(lila.llm.analysis.L3.NatureType.Static, 0, 0, 0, false)
    val mockCrit = CriticalityResult(CriticalityType.Normal, 0, None, 0)
    val mockTopo = ChoiceTopologyResult(ChoiceTopologyType.StyleChoice, 0, 0, None, 0, 0, None)
    val mockPhase = GamePhaseResult(GamePhaseType.Middlegame, 30, true, 4)
    val mockSimp = SimplifyBiasResult(false, 0, false, false)
    val mockDraw = DrawBiasResult(false, false, false, false, false)
    val mockRisk = RiskProfileResult(RiskLevel.Medium, 0, 0, 0)
    val mockTask = TaskModeResult(TaskModeType.ExplainPlan, "test")
    
    val mockClass = PositionClassification(mockNature, mockCrit, mockTopo, mockPhase, mockSimp, mockDraw, mockRisk, mockTask)
    
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
