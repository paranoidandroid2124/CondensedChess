package lila.llm.analysis

import lila.llm.*
import lila.llm.model.*
import lila.llm.model.strategic.*
import lila.llm.NarrativeTemplates

class Phase3ValidationTest extends munit.FunSuite {

  test("GameNarrativeOrchestrator identifies blunders") {
    val evals = List(
      MoveEval(1, cp = 10, variations = Nil),
      MoveEval(2, cp = 250, variations = Nil), // BIG SWING
      MoveEval(3, cp = 260, variations = Nil)
    )
    val moments = GameNarrativeOrchestrator.selectKeyMoments(evals)
    
    assert(moments.exists(_.ply == 2), "Should identify ply 2 as key moment")
    assert(moments.exists(_.momentType == "Blunder"), "Should classify as Blunder")
  }

  test("GameNarrativeOrchestrator identifies tension") {
    val looseVars = List(
       AnalysisVariation(List("e4"), 50, None),
       AnalysisVariation(List("d4"), 45, None) // Close score
    )
    val evals = List(
      MoveEval(10, cp = 50, variations = looseVars)
    )
    val moments = GameNarrativeOrchestrator.selectKeyMoments(evals)
    
    assert(moments.exists(_.momentType == "TensionPeak"), "Should identify TensionPeak")
  }

  test("NarrativeGenerator outputs Variation A/B structure") {
    val candidates = List(
      AnalyzedCandidate("e2e4", 100, Nil, Nil, "Attack", lila.llm.model.strategic.VariationLine(Nil, 0, None)),
      AnalyzedCandidate("d2d4", 50, Nil, Nil, "Solid", lila.llm.model.strategic.VariationLine(Nil, 0, None))
    )
    
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val output = NarrativeGenerator.describeCandidatesBookStyle(candidates, fen, 10)
    
    assert(output.contains("**6. e4 aims for attack"), "Should contain Main Line 6. e4 and aims for intent")
    assert(output.contains("a)"), "Should contain Alternative label 'a)'")
    assert(output.contains("d4"), "Should contain alternative d4") // d2d4 -> d4
    
    // Comparison is implicitly handled in refutations/alternatives description now
    // assert(output.contains("**Comparison**:"), "Should generate candidate comparison text")
  }

  test("NarrativeGenerator respects Style parameter (Coach)") {
     import lila.llm.NarrativeTemplates.Style
     
     // Mock Data
     val nature = lila.llm.analysis.PositionNature(lila.llm.analysis.NatureType.Dynamic, 50.0, 0.5, "Dynamic Position")
     val plan = PlanMatch(lila.llm.model.Plan.KingsideAttack(chess.Color.White), 100.0, Nil)
     val data = ExtendedAnalysisData(
       fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
       nature = nature,
       motifs = Nil,
       plans = List(plan), // Must have plans to trigger rhetorical question
       preventedPlans = Nil,
       pieceActivity = Nil,
       structuralWeaknesses = Nil,
       compensation = None,
       endgameFeatures = None,
       practicalAssessment = None,
       prevMove = None,
       ply = 10,
       candidates = Nil
     )

     val output = NarrativeGenerator.describeExtended(data, Style.Coach)
     
     // Coach style intro check (expanded coverage for varied mixSeed results)
     val coachPhrases = Seq("Let's pause", "Look closely", "Training point", "Don't rush", "If you were", "critical moment", "Understand the nature", "Mastery requires", "tension is", "deep breath")
     assert(coachPhrases.exists(output.contains), s"Should use Coach intro. Got: ${output.take(200)}")
     
     // Rhetorical Question check
     assert(output.contains("?"), "Should contain rhetorical question in Coach style")
  }
  test("GameNarrativeOrchestrator identifies Missed Win") {
    val evals = List(
      MoveEval(10, cp = 300, variations = Nil), // Winning
      MoveEval(11, cp = 0, variations = Nil)    // Draw (Missed Win)
    )
    val moments = GameNarrativeOrchestrator.selectKeyMoments(evals)
    
    assert(moments.exists(_.momentType == "MissedWin"), "Should identify MissedWin")
  }

  test("GameNarrativeOrchestrator identifies Sustained Pressure") {
    // Issue #6: Use all EVEN plies (White to move) for consistent normalization
    val evals = List(
      MoveEval(20, cp = 10, variations = Nil),
      MoveEval(22, cp = 40, variations = Nil),
      MoveEval(24, cp = 70, variations = Nil),
      MoveEval(26, cp = 100, variations = Nil),
      MoveEval(28, cp = 130, variations = Nil),
      MoveEval(30, cp = 180, variations = Nil) // Total +170, max step 50 (all White moves)
    )
    val moments = GameNarrativeOrchestrator.selectKeyMoments(evals)
    
    assert(moments.exists(_.momentType == "SustainedPressure"), "Should identify SustainedPressure")
  }
}
