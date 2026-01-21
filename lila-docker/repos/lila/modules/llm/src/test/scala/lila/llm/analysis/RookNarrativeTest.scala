package lila.llm.analysis

import munit.FunSuite
import lila.llm.model._
import lila.llm.model.strategic._
import chess.Color

class RookNarrativeTest extends FunSuite {

  test("BookStyleRenderer.render Semi-open File narrative correctly") {
    val ctx = createPositionalCtx("SemiOpenFileControl", Some("d"))
    val prose = BookStyleRenderer.render(ctx)
    println(s"Semi-open File Prose: $prose")
    
    val keywords = List("semi-open", "file")
    assertFound(prose, keywords)
  }

  test("BookStyleRenderer.render Seventh Rank Invasion narrative correctly") {
    val ctx = createPositionalCtx("RookOnSeventh")
    val prose = BookStyleRenderer.render(ctx)
    println(s"Seventh Rank Prose: $prose")
    
    val keywords = List("seventh", "invasion", "poking")
    assertFound(prose, keywords)
  }

  test("BookStyleRenderer.render Rook Behind Passed Pawn narrative correctly") {
    val ctx = createPositionalCtx("RookBehindPassedPawn", Some("e"))
    val prose = BookStyleRenderer.render(ctx)
    println(s"Rook Behind Passed Pawn Prose: $prose")
    
    val keywords = List("behind", "passed", "tarrasch")
    assertFound(prose, keywords)
  }

  test("BookStyleRenderer.render King Cut-Off narrative correctly") {
    val ctx = createPositionalCtx("KingCutOff", Some("rank"))
    val prose = BookStyleRenderer.render(ctx)
    println(s"King Cut-Off Prose: $prose")
    
    val keywords = List("cutting", "restricting", "trapped")
    assertFound(prose, keywords)
  }

  test("BookStyleRenderer.render Doubled/Connected Rooks narrative correctly") {
    val doubledCtx = createPositionalCtx("DoubledRooks")
    val doubledProse = BookStyleRenderer.render(doubledCtx)
    assertFound(doubledProse, List("doubled", "firepower", "stacking"))

    val connectedCtx = createPositionalCtx("ConnectedRooks")
    val connectedProse = BookStyleRenderer.render(connectedCtx)
    assertFound(connectedProse, List("connecting", "coordination", "linking"))
  }

  test("BookStyleRenderer.render Exchange Sacrifice ROI correctly") {
    val ctx = createSacrificeCtx("open_file", 250)
    val prose = BookStyleRenderer.render(ctx)
    println(s"Exchange Sac Prose: $prose")
    
    val keywords = List("sacrificing", "exchange", "file", "dominant")
    assertFound(prose, keywords)
  }

  private def assertFound(prose: String, keywords: List[String]): Unit = {
    val proseLower = prose.toLowerCase.replace("-", " ")
    val found = keywords.find(k => proseLower.contains(k.toLowerCase.replace("-", " ")))
    assert(found.isDefined, s"Result should contain one of $keywords, but none were found in: $prose")
  }

  def createPositionalCtx(tagType: String, detail: Option[String] = None): NarrativeContext = {
    val feature = PositionalTagInfo(tagType, None, None, "White", detail)
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
       fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
       header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Low", "ExplainPlan"),
       ply = 1,
       summary = NarrativeSummary("Improving", None, "StyleChoice", "Maintain", "+0.0"),
       phase = PhaseContext("Middlegame", "Material equality", None),
       snapshots = List(L1Snapshot("=", None, None, None, None, Some("Dominant"), Nil)),
       pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Test", "Background", None, false, "quiet"),
       threats = ThreatTable(Nil, Nil), 
       plans = PlanTable(Nil, Nil),
       candidates = List(
         CandidateInfo(
           move = "Rd1",
           uci = None, 
           annotation = "!", 
           planAlignment = "activating rook", 
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

  def createSacrificeCtx(reason: String, value: Int): NarrativeContext = {
    val roi = Motif.SacrificeROI(reason, value)
    NarrativeContext(
       fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
       header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Low", "ExplainPlan"),
       ply = 1,
       summary = NarrativeSummary("Sacrificing", None, "StyleChoice", "Maintain", "+1.2"),
       phase = PhaseContext("Middlegame", "Exchange down", None),
       snapshots = List(L1Snapshot("-2", None, None, None, None, None, Nil)),
       pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Test", "Background", None, false, "quiet"),
       threats = ThreatTable(Nil, Nil), 
       plans = PlanTable(Nil, Nil),
       candidates = List(
         CandidateInfo(
           move = "Rxd4",
           uci = None, 
           annotation = "!!", 
           planAlignment = "tactical attack", 
           downstreamTactic = None, 
           tacticalAlert = None, 
           practicalDifficulty = "sharp", 
           whyNot = None, 
           tags = Nil, 
           tacticEvidence = List(s"ExchangeSacrifice($reason)"), 
           facts = Nil
         )
       ),
       delta = None,
       decision = None,
       opponentPlan = None,
       facts = Nil,
       engineEvidence = None,
       semantic = Some(SemanticSection(
         structuralWeaknesses = Nil,
         pieceActivity = Nil,
         positionalFeatures = Nil,
         compensation = None, // Placeholder for CompensationInfo if needed, or just let it be None
         endgameFeatures = None,
         practicalAssessment = Some(PracticalInfo(10, 10, "Under Pressure")),
         preventedPlans = Nil,
         conceptSummary = Nil
       ))
    )
  }
}
