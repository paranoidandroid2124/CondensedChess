package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }
import lila.commentary.analysis.L3.*
import lila.commentary.analysis.L4.PositionalPlanEngine

class PositionalPlanEngineTest extends FunSuite:

  private val initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val knightOnA1Fen = "8/8/8/8/8/1P6/2P5/N1K5 w - - 0 1" // White knight on a1, blocked by pawns

  private def dummyHypothesis: PlanHypothesis =
    PlanHypothesis(
      planId = "test_plan",
      planName = "Test Plan",
      rank = 1,
      score = 0.8,
      preconditions = Nil,
      executionSteps = Nil,
      failureModes = Nil,
      viability = PlanViability(0.8, "high", "none")
    )

  private def makeClassification(taskMode: TaskModeType, nature: NatureType): PositionClassification =
    PositionClassification(
      nature = NatureResult(nature, 0, 0, 0, false),
      criticality = CriticalityResult(CriticalityType.Normal, 0, None, 0),
      choiceTopology = ChoiceTopologyResult(ChoiceTopologyType.StyleChoice, 0, 0, None, 0, 0, None),
      gamePhase = GamePhaseResult(GamePhaseType.Middlegame, 32, true, 8),
      simplifyBias = SimplifyBiasResult(false, 0, false, false),
      drawBias = DrawBiasResult(false, false, false, false, false),
      riskProfile = RiskProfileResult(RiskLevel.Medium, 0, 0, 0),
      taskMode = TaskModeResult(taskMode, "default")
    )

  test("PositionalPlanEngine maps ExplainPlan + Static to StaticClamp template") {
    val classification = makeClassification(TaskModeType.ExplainPlan, NatureType.Static)
    val hypothesis = dummyHypothesis

    val enriched = PositionalPlanEngine.enrich(initialFen, classification, None, hypothesis)

    assert(enriched.executionSteps.size == 3)
    assert(enriched.executionSteps.head.contains("In this static position"))
    assert(enriched.executionSteps(1).contains("Maintain a solid pawn skeleton"))
    assert(enriched.executionSteps(2).contains("weakness on d5"))
  }

  test("PositionalPlanEngine maps ExplainPlan + Dynamic/Chaos to DynamicBreak template") {
    val classification = makeClassification(TaskModeType.ExplainPlan, NatureType.Dynamic)
    val hypothesis = dummyHypothesis

    val enriched = PositionalPlanEngine.enrich(initialFen, classification, None, hypothesis)

    assert(enriched.executionSteps.size == 3)
    assert(enriched.executionSteps.head.contains("open-board activity"))
    assert(enriched.executionSteps(1).contains("Force pawn tension resolution"))
    assert(enriched.executionSteps(2).contains("vulnerable target on d5"))
  }

  test("PositionalPlanEngine maps ExplainConvert to EndgameConversion template") {
    val classification = makeClassification(TaskModeType.ExplainConvert, NatureType.Static)
    val hypothesis = dummyHypothesis

    val enriched = PositionalPlanEngine.enrich(initialFen, classification, None, hypothesis)

    assert(enriched.executionSteps.size == 3)
    assert(enriched.executionSteps.head.contains("Activate your king"))
    assert(enriched.executionSteps(1).contains("Advance your primary passed pawn"))
    assert(enriched.executionSteps(2).contains("weak pawn on d5"))
  }

  test("PositionalPlanEngine maps ExplainTactics/ExplainDefense to TacticalRestraint template") {
    val classification = makeClassification(TaskModeType.ExplainTactics, NatureType.Static)
    val hypothesis = dummyHypothesis

    val enriched = PositionalPlanEngine.enrich(initialFen, classification, None, hypothesis)

    assert(enriched.executionSteps.size == 3)
    assert(enriched.executionSteps.head.contains("Secure piece safety"))
    assert(enriched.executionSteps(1).contains("Maintain a closed pawn center"))
    assert(enriched.executionSteps(2).contains("Guard against tactical threats"))
  }

  test("PositionalPlanEngine extracts actual low-mobility pieces from the board state") {
    val classification = makeClassification(TaskModeType.ExplainPlan, NatureType.Static)
    val hypothesis = dummyHypothesis

    val enriched = PositionalPlanEngine.enrich(knightOnA1Fen, classification, None, hypothesis)

    // The knight on a1 should have the lowest mobility score
    assert(enriched.executionSteps.head.contains("knight"))
    assert(enriched.executionSteps.head.contains("a1"))
  }

  test("PositionalPlanEngine handles pawn analysis break files correctly") {
    val classification = makeClassification(TaskModeType.ExplainPlan, NatureType.Static)
    val hypothesis = dummyHypothesis
    val pawnAnalysis = PawnPlayAnalysis(
      pawnBreakReady = true,
      breakFile = Some("e"),
      breakImpact = 150,
      advanceOrCapture = false,
      passedPawnUrgency = PassedPawnUrgency.Background,
      passerBlockade = false,
      blockadeSquare = None,
      blockadeRole = None,
      pusherSupport = false,
      minorityAttack = false,
      counterBreak = false,
      tensionPolicy = TensionPolicy.Maintain,
      tensionSquares = Nil,
      primaryDriver = "break_ready",
      notes = ""
    )

    val enriched = PositionalPlanEngine.enrich(initialFen, classification, Some(pawnAnalysis), hypothesis)

    assert(enriched.executionSteps(1).contains("e-file"))
  }
