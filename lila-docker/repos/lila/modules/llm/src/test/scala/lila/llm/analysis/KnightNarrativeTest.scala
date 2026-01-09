package lila.llm.analysis

import munit.FunSuite
import lila.llm.model._
import lila.llm.model.strategic._
import chess.Color

class KnightNarrativeTest extends FunSuite {

  test("BookStyleRenderer.render Maneuver narrative correctly") {
    val ctx = createCtx(
      move = "Nd2",
      evidence = "Maneuver(Knight, rerouting)", 
      plan = "maneuvering"
    )
    val prose = BookStyleRenderer.render(ctx)
    println(s"Maneuver Prose: $prose")
    
    val keywords = List("rerout", "transfer", "switch")
    val found = keywords.exists(k => prose.toLowerCase.contains(k))
    assert(found, s"Result should contain one of $keywords, got: $prose")
  }

  test("BookStyleRenderer.render Domination narrative correctly") {
    val ctx = createCtx(
      move = "Nd5",
      evidence = "Domination(Knight dominates Bishop)",
      plan = "central domination"
    )
    val prose = BookStyleRenderer.render(ctx)
    println(s"Domination Prose: $prose")

    val keywords = List("dominat", "paralyz", "grip")
    val found = keywords.exists(k => prose.toLowerCase.contains(k))
    assert(found, s"Result should contain one of $keywords, got: $prose")
  }

  test("BookStyleRenderer.render TrappedPiece narrative correctly") {
    val ctx = createCtx(
      move = "Bh6",
      evidence = "TrappedPiece(Queen)",
      plan = "trapping material"
    )
    val prose = BookStyleRenderer.render(ctx)
    println(s"TrappedPiece Prose: $prose")

    val keywords = List("trap", "entomb", "exploit")
    val found = keywords.exists(k => prose.toLowerCase.contains(k))
    assert(found, s"Result should contain one of $keywords, got: $prose")
    assert(prose.toLowerCase.contains("queen"), "Should mention Queen")
  }

  test("BookStyleRenderer.render KnightVsBishop narrative correctly") {
    val ctx = createCtx(
      move = "Nd4",
      evidence = "KnightVsBishop(White, true)",
      plan = "exploiting minor piece imbalance"
    )
    val prose = BookStyleRenderer.render(ctx)
    println(s"Imbalance Prose: $prose")

    val keywords = List("knight", "bishop", "closed", "open")
    val found = keywords.exists(k => prose.toLowerCase.contains(k))
    assert(found, s"Result should contain one of $keywords, got: $prose")
  }

  test("BookStyleRenderer.render Blockade narrative correctly") {
    val ctx = createCtx(
      move = "Nd4",
      evidence = "Blockade(Knight, d5)",
      plan = "stopping the passer"
    )
    val prose = BookStyleRenderer.render(ctx)
    println(s"Blockade Prose: $prose")

    val keywords = List("blocking", "stopping", "blockade")
    val found = keywords.exists(k => prose.toLowerCase.contains(k))
    assert(found, s"Result should contain one of $keywords, got: $prose")
  }

  test("BookStyleRenderer.render SmotheredMate narrative correctly") {
    val ctx = createCtx(
      move = "Nf7#",
      evidence = "SmotheredMate(Knight)",
      plan = "checkmate"
    )
    val prose = BookStyleRenderer.render(ctx)
    println(s"SmotheredMate Prose: $prose")

    val keywords = List("smothered", "suffocating", "spectacular")
    val found = keywords.exists(k => prose.toLowerCase.contains(k))
    assert(found, s"Result should contain one of $keywords, got: $prose")
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
}
