package chess
package analysis

import munit.FunSuite
import chess.analysis.BookModel.*
import chess.analysis.AnalysisModel.*

class NarrativeTemplatesTest extends FunSuite:

  def mockPly(plyVal: Int, role: String): PlyOutput =
    PlyOutput(
      ply = Ply(plyVal),
      turn = if plyVal % 2 == 0 then Color.White else Color.Black,
      san = "e4",
      uci = "e2e4",
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      fenBefore = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      legalMoves = 20,
      features = FeatureExtractor.SideFeatures(0,0,0,0,0,0,false,0,0),
      evalBeforeShallow = EngineEval(10, Nil),
      evalBeforeDeep = EngineEval(20, List(EngineLine("e2e4", 0.5, Some(10), None, Nil))),
      winPctBefore = 50.0,
      winPctAfterForPlayer = 50.0,
      deltaWinPct = 0.0,
      epBefore = 100,
      epAfter = 100,
      epLoss = 0,
      judgement = "Normal",
      special = None,
      concepts = Concepts(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
      conceptsBefore = Concepts(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
      conceptDelta = Concepts(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
      bestVsSecondGap = None,
      bestVsPlayedGap = None,
      semanticTags = Nil,
      mistakeCategory = None,
      roles = List(role),
      conceptLabels = None,
      playedEvalCp = Some(10)
    )

  test("buildSectionPrompt includes section context") {
    val plys = Vector(
      mockPly(1, "DevelopmentAdvantage"),
      mockPly(2, "Violence")
    )
    val diag = BookDiagram(
      "diag1", 
      "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 
      List("Violence"), 
      1, 
      TagBundle(Nil, Nil, Nil, Nil, Nil, Nil)
    )
    
    val prompt = NarrativeTemplates.buildSectionPrompt(
      SectionType.OpeningPortrait,
      "The Opening",
      plys,
      List(diag)
    )
    
    assert(prompt.contains("OpeningPortrait"))
    assert(prompt.contains("Ply Range: 1 - 2"))
    assert(prompt.contains("Dominant Roles: DevelopmentAdvantage, Violence")) // Order depends on frequency/sort
    assert(prompt.contains("GOAL: Describe the opening phase"))
  }

  test("buildSectionPrompt handles TacticalStorm") {
    val plys = Vector(mockPly(10, "Violence"))
    val prompt = NarrativeTemplates.buildSectionPrompt(
      SectionType.TacticalStorm,
      "Storm",
      plys,
      Nil
    )
    assert(prompt.contains("TacticalStorm"))
    assert(prompt.contains("GOAL: Describe a tactical sequence"))
  }

  test("buildSectionPrompt covers all SectionTypes") {
    val plys = Vector(mockPly(1, "Generic"))
    SectionType.values.foreach { tpe =>
      val prompt = NarrativeTemplates.buildSectionPrompt(
        tpe,
        "Test Section",
        plys,
        Nil
      )
      assert(prompt.nonEmpty, s"Prompt for $tpe should not be empty")
      assert(prompt.contains("GOAL:"), s"Prompt for $tpe should contain GOAL section")
    }
  }

  test("buildSectionPrompt includes Phase 20 tags and Human Notation") {
    // Ply 29 => Move 15. w
    val p = mockPly(29, "Mistake").copy(
       semanticTags = List("Greed"),
       deltaWinPct = -4.5
    )
    val diagram = BookDiagram("Greed Moment", "fen...", List("Mistake"), 29, TagBundle(Nil,Nil,Nil,Nil,Nil,Nil))
    
    val prompt = NarrativeTemplates.buildSectionPrompt(
       SectionType.CriticalCrisis, "Crisis", Vector(p), List(diagram)
    )
    
    assert(prompt.contains("Move 15. w"), s"Expected human notation 'Move 15. w', got: $prompt")
    assert(prompt.contains("[Greed]"), s"Expected tag '[Greed]', got: $prompt")
    assert(prompt.contains("Dropped 4.5%"), s"Expected eval change 'Dropped 4.5%', got: $prompt")
    assert(prompt.contains("3-4 sentences"), s"Expected relaxed constraint '3-4 sentences', got: $prompt")
  }
