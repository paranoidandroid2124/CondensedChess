package chess
package analysis

import munit.FunSuite
import chess.analysis.BookModel.*
import chess.analysis.AnalysisModel.*
import chess.analysis.AnalysisTypes.*
import chess.analysis.ConceptLabeler.* // For ConceptLabels

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
      SectionType.OpeningReview,
      "The Opening",
      plys,
      List(diag)
    )
    
    assert(prompt.contains("OpeningReview"))
    assert(prompt.contains("Move Range: 1 - 2"))
    assert(prompt.contains("Dominant Roles: DevelopmentAdvantage, Violence")) 
    assert(prompt.contains("GOAL: Describe the opening phase"), "Missing GOAL description")
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
       SectionType.TurningPoints, "Crisis", Vector(p), List(diagram)
    )
    
    assert(prompt.contains("15. w"), s"Expected human notation '15. w', got: $prompt")
    assert(prompt.contains("[Greed]"), s"Expected tag '[Greed]', got: $prompt")
    assert(prompt.contains("Dropped 4.5%"), s"Expected eval change 'Dropped 4.5%', got: $prompt")
    assert(prompt.contains("concise sentences"), s"Expected relaxed constraint 'concise sentences', got: $prompt")
  }

  test("buildSectionPrompt renders rich evidence (Safety, PV San)") {
    val safetyEv = KingSafetyEvidence("g8", List("h-file"), 2, Nil, 1)
    
    // Use valid alternating moves: White e4, Black e5
    val pvEv = PvEvidence(List("e2e4", "e7e5"), EngineEval(20, List(EngineLine("e2e4", 0.99, Some(35), None, Nil)) ) ) 
    
    val labels = ConceptLabels(
       structureTags = Nil,
       planTags = Nil,
       tacticTags = Nil,
       mistakeTags = Nil,
       endgameTags = Nil,
       positionalTags = Nil,
       transitionTags = Nil,
       missedPatternTypes = Nil,
       richTags = List(
         RichTag("king_danger", 0.9, TagCategory.Dynamic, List(EvidenceRef("safety", "safety_1"))),
         RichTag("winning_line", 0.8, TagCategory.Tactic, List(EvidenceRef("pv", "pv_1")))
       ),
       evidence = EvidencePack(
         kingSafety = Map("safety_1" -> safetyEv),
         pv = Map("pv_1" -> pvEv)
       )
    )

    val p = mockPly(30, "MatingAttack").copy(
       conceptLabels = Some(labels)
    )
    val diagram = BookDiagram("Mate", "fen...", List("MatingAttack"), 30, TagBundle(Nil,Nil,Nil,Nil,Nil,Nil))

    val prompt = NarrativeTemplates.buildSectionPrompt(
       SectionType.TacticalStorm, "Attack", Vector(p), List(diagram)
    )

    assert(prompt.contains("King Danger: 2 attackers on g8"), s"Missing Safety evidence: $prompt")
    // Assert SAN conversion happens: e2e4 -> e4, e7e5 -> e5
    assert(prompt.contains("Line: e4 e5..."), s"Missing PV evidence or SAN conversion failed: $prompt")
  }

  test("buildSectionPrompt includes chess book style requirements") {
    val p = mockPly(29, "Mistake").copy(deltaWinPct = -6.0)
    val prompt = NarrativeTemplates.buildSectionPrompt(
      SectionType.TurningPoints, "Crisis", Vector(p), Nil
    )
    assert(prompt.contains("CHESS BOOK STYLE"), s"Missing CHESS BOOK STYLE section: $prompt")
    assert(prompt.contains("leads to X because of Y"), s"Missing causation pattern: $prompt")
    assert(prompt.contains("Question:"), s"Should require Q&A format: $prompt")
    assert(prompt.contains("personality"), s"Missing personification guidance: $prompt")
  }

