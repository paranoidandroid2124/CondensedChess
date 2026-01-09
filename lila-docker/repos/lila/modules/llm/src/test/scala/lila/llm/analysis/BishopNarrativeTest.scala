package lila.llm.analysis

import munit.FunSuite
import lila.llm.model._
import lila.llm.model.strategic._
import chess.Color

class BishopNarrativeTest extends FunSuite {

  test("BookStyleRenderer.render Pin narrative correctly") {
    val ctx = createCtx(
      move = "Bb5",
      evidence = "Pin(Bishop on b5 to king on e8)",
      plan = "pinning the king"
    )
    val prose = BookStyleRenderer.render(ctx)
    println(s"Pin Prose: $prose")
    
    val keywords = List("pin", "paralyz", "troublesome")
    val found = keywords.exists(k => prose.toLowerCase.contains(k))
    assert(found, s"Result should contain one of $keywords, got: $prose")
    assert(prose.toLowerCase.contains("king"), "Should mention King")
  }

  test("BookStyleRenderer.render Skewer narrative correctly") {
    val ctx = createCtx(
      move = "Ba3",
      evidence = "Skewer(Bishop through queen on d6 to rook on f8)",
      plan = "skewering pieces"
    )
    val prose = BookStyleRenderer.render(ctx)
    println(s"Skewer Prose: $prose")

    val keywords = List("skewer", "firing through", "power")
    val found = keywords.exists(k => prose.toLowerCase.contains(k))
    assert(found, s"Result should contain one of $keywords, got: $prose")
    assert(prose.toLowerCase.contains("queen"), "Should mention Queen")
  }

  test("BookStyleRenderer.render XRay narrative correctly") {
    val ctx = createCtx(
      move = "Bg2",
      evidence = "XRay(Bishop through to rook on h1)",
      plan = "x-ray pressure"
    )
    val prose = BookStyleRenderer.render(ctx)
    println(s"XRay Prose: $prose")

    val keywords = List("x-ray", "peering", "pressure")
    val found = keywords.exists(k => prose.toLowerCase.contains(k))
    assert(found, s"Result should contain one of $keywords, got: $prose")
  }

  test("BookStyleRenderer.render Battery narrative correctly") {
    val ctx = createCtx(
      move = "Bc2",
      evidence = "Battery(Bishop and Queen)",
      plan = "battery formation"
    )
    val prose = BookStyleRenderer.render(ctx)
    println(s"Battery Prose: $prose")

    val keywords = List("battery", "aligning", "backing up")
    val found = keywords.exists(k => prose.toLowerCase.contains(k))
    assert(found, s"Result should contain one of $keywords, got: $prose")
  }

  test("BookStyleRenderer.render Bishop positional themes via ConceptLinker") {
    val themes = List(
      ("GoodBishop", List("good bishop", "activating", "strong", "unobstructed")),
      ("BadBishop", List("bad bishop", "struggling", "restricted", "blocked")),
      ("BishopPairAdvantage", List("bishop pair", "leveraging", "long-range", "coordination")),
      ("OppositeColorBishops", List("opposite-colored", "complexities", "draw")),
      ("ColorComplexWeakness", List("color complex", "dominating", "exploiting", "light or dark"))
    )

    themes.foreach { case (tagType, keywords) =>
      val ctx = createPositionalCtx(tagType)
      val prose = BookStyleRenderer.render(ctx)
      println(s"Positional ($tagType) Prose: $prose")

      val found = keywords.map(_.toLowerCase).exists(k => prose.toLowerCase.contains(k))
      assert(found, s"Result for $tagType should contain one of $keywords, got: $prose")
    }
  }

  def createCtx(move: String, evidence: String, plan: String): NarrativeContext = {
    NarrativeContext(
       header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Low", "ExplainPlan"),
       summary = NarrativeSummary("Improving", None, "StyleChoice", "Maintain", "+0.0"),
       phase = PhaseContext("Middlegame", "Material equality", None),
       snapshots = List(L1Snapshot("=", None, None, None, None, None, Nil)),
       pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Test", "Background", None, false, "quiet"),
       threats = ThreatTable(Nil, Nil), 
       plans = PlanTable(List(PlanRow(1, plan, 1.0, List("evidence"))), Nil),
       candidates = List(
         CandidateInfo(
           move = move,
           uci = None, 
           annotation = "!", 
           planAlignment = plan, 
           downstreamTactic = None, 
           tacticalAlert = None, 
           practicalDifficulty = "clean", 
           whyNot = None, 
           tags = Nil, 
           tacticEvidence = List(evidence), 
           facts = Nil
         )
       ),
       delta = None,
       decision = None,
       opponentPlan = None,
       facts = Nil,
       engineEvidence = None,
       semantic = None
    )
  }

  def createPositionalCtx(tagType: String): NarrativeContext = {
    val feature = PositionalTagInfo(tagType, None, None, "White", Some("detailed info"))
    val semantic = SemanticSection(
      structuralWeaknesses = Nil,
      pieceActivity = Nil,
      positionalFeatures = List(feature),
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = Some(PracticalInfo(10, 10, "Comfortable")),
      preventedPlans = Nil,
      conceptSummary = Nil
    )
    
    NarrativeContext(
       header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Low", "ExplainPlan"),
       summary = NarrativeSummary("Improving", None, "StyleChoice", "Maintain", "+0.0"),
       phase = PhaseContext("Middlegame", "Material equality", None),
       snapshots = List(L1Snapshot("=", None, None, None, None, Some("Dominant"), Nil)),
       pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Test", "Background", None, false, "quiet"),
       threats = ThreatTable(Nil, Nil), 
       plans = PlanTable(Nil, Nil), // Important: No plans to allow Link #4 (Strategic Feature -> Candidate) to fire or Link #2
       candidates = List(
         CandidateInfo(
           move = "Bc4",
           uci = None, 
           annotation = "!", 
           planAlignment = "improving bishop", 
           downstreamTactic = None, 
           tacticalAlert = None, 
           practicalDifficulty = "clean", 
           whyNot = None, 
           tags = Nil, 
           tacticEvidence = Nil, 
           facts = Nil
         )
       ),
       delta = None,
       decision = None,
       opponentPlan = None,
       facts = Nil,
       engineEvidence = None,
       semantic = Some(semantic)
    )
  }
}
