package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }
import lila.commentary.analysis.L3.*
import lila.commentary.analysis.L4.PositionalPlanEngine

class PositionalPlanEngineTest extends FunSuite:

  private val initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val knightOnA1Fen = "8/8/8/8/8/1P6/2P5/N1K5 w - - 0 1" // White knight on a1, blocked by pawns
  private val blackHoleFen = "4k3/8/8/pppp4/8/8/PPPP4/4K3 w - - 0 1"

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
    assert(enriched.executionSteps(1).contains("pawn structure stable"))
    assert(enriched.executionSteps(2).contains("no specific weak square is certified yet"))
  }

  test("PositionalPlanEngine maps ExplainPlan + Dynamic/Chaos to DynamicBreak template") {
    val classification = makeClassification(TaskModeType.ExplainPlan, NatureType.Dynamic)
    val hypothesis = dummyHypothesis

    val enriched = PositionalPlanEngine.enrich(initialFen, classification, None, hypothesis)

    assert(enriched.executionSteps.size == 3)
    assert(enriched.executionSteps.head.contains("open-board activity"))
    assert(enriched.executionSteps(1).contains("concrete central tension break"))
    assert(enriched.executionSteps(2).contains("concrete target"))
  }

  test("PositionalPlanEngine maps ExplainConvert without inventing passed-pawn or winning-ending claims") {
    val classification = makeClassification(TaskModeType.ExplainConvert, NatureType.Static)
    val hypothesis = dummyHypothesis

    val enriched = PositionalPlanEngine.enrich(initialFen, classification, None, hypothesis)

    assert(enriched.executionSteps.size == 3)
    assert(enriched.executionSteps.head.contains("Activate your king"))
    assert(enriched.executionSteps(1).contains("Improve king activity"))
    assert(enriched.executionSteps(2).contains("verify the next pawn or exchange target"))
    assert(!enriched.executionSteps.exists(_.toLowerCase.contains("winning ending")), clue(enriched.executionSteps))
    assert(!enriched.executionSteps.exists(_.toLowerCase.contains("primary passed pawn")), clue(enriched.executionSteps))
  }

  test("PositionalPlanEngine uses passed-pawn wording only from pawn analysis") {
    val classification = makeClassification(TaskModeType.ExplainConvert, NatureType.Static)
    val hypothesis = dummyHypothesis
    val pawnAnalysis = PawnPlayAnalysis(
      pawnBreakReady = false,
      breakFile = None,
      breakImpact = 0,
      advanceOrCapture = false,
      passedPawnUrgency = PassedPawnUrgency.Critical,
      passerBlockade = false,
      blockadeSquare = None,
      blockadeRole = None,
      pusherSupport = true,
      minorityAttack = false,
      counterBreak = false,
      tensionPolicy = TensionPolicy.Maintain,
      tensionSquares = Nil,
      primaryDriver = "passed_pawn",
      notes = ""
    )

    val enriched = PositionalPlanEngine.enrich(initialFen, classification, Some(pawnAnalysis), hypothesis)

    assert(enriched.executionSteps.exists(_.contains("passed-pawn task")), clue(enriched.executionSteps))
    assert(enriched.executionSteps.exists(_.contains("current support intact")), clue(enriched.executionSteps))
  }

  test("PositionalPlanEngine does not relabel weak-complex squares as weak pawns") {
    val classification = makeClassification(TaskModeType.ExplainConvert, NatureType.Static)
    val hypothesis = dummyHypothesis

    val enriched = PositionalPlanEngine.enrich(blackHoleFen, classification, None, hypothesis)

    assert(enriched.executionSteps.exists(_.contains("structural target")), clue(enriched.executionSteps))
    assert(!enriched.executionSteps.exists(_.toLowerCase.contains("weak pawn")), clue(enriched.executionSteps))
  }

  test("PositionalPlanEngine maps ExplainTactics/ExplainDefense to TacticalRestraint template") {
    val classification = makeClassification(TaskModeType.ExplainTactics, NatureType.Static)
    val hypothesis = dummyHypothesis

    val enriched = PositionalPlanEngine.enrich(initialFen, classification, None, hypothesis)

    assert(enriched.executionSteps.size == 3)
    assert(enriched.executionSteps.head.contains("Secure piece safety"))
    assert(enriched.executionSteps(1).contains("pawn structure stable"))
    assert(enriched.executionSteps(2).contains("Guard against tactical threats"))
    assert(!enriched.executionSteps.exists(_.contains("None")), clue(enriched.executionSteps))
    assert(!enriched.executionSteps.exists(_.toLowerCase.contains("outpost")), clue(enriched.executionSteps))
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
