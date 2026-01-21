package lila.llm.analysis

import munit.FunSuite
import lila.llm.model._
import lila.llm.model.strategic._
import chess.Color

class StrategicNarrativeTest extends FunSuite {

  // Helper to create a minimal context with specific semantic features
  private def renderWithFeatures(
    positional: List[PositionalTag] = Nil,
    weaknesses: List[WeakComplex] = Nil,
    concepts: List[String] = Nil
  ): String = {
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    // 1. Construct Semantic Section
    val semantic = SemanticSection(
      positionalFeatures = positional.map(p => convertPositionalTag(p)),
      structuralWeaknesses = weaknesses.map(w => convertWeakComplex(w)),
      pieceActivity = Nil,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      preventedPlans = Nil,
      conceptSummary = concepts
    )

    // 2. Construct Minimal Context
    val ctx = NarrativeContext(
      fen = fen,
      header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Medium", "ExplainPlan"),
      ply = 1,
      summary = NarrativeSummary("Plan A", None, "StyleChoice", "Maintain", "0.0"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Reason", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      snapshots = Nil,
      delta = None,
      phase = PhaseContext("Middlegame", "Reason", None),
      candidates = List(
        CandidateInfo(
          move = "e4",
          uci = None,
          annotation = "",
          planAlignment = "Attack",
          tacticalAlert = None,
          practicalDifficulty = "clean",
          whyNot = None
        )
      ),
      facts = Nil,
      probeRequests = Nil,
      meta = None,
      strategicFlow = None,
      semantic = Some(semantic),
      opponentPlan = None,
      decision = None,
      openingEvent = None,
      openingData = None,
      updatedBudget = OpeningEventBudget(),
      engineEvidence = None
    )

    BookStyleRenderer.render(ctx)
  }

  // Duplicate convert helpers from NarrativeContextBuilder (since they are private there)
  // In a real refactor, these should be public or in the companion object.
  private def convertPositionalTag(pt: PositionalTag): PositionalTagInfo = {
    // Simplified conversion for testing
    val (name, detail) = pt match {
      case PositionalTag.PawnMajority(_, flank, count) => 
        ("PawnMajority", s"$flank $count pawns")
      case PositionalTag.MinorityAttack(_, flank) => 
        // Note: flank is second arg in case class case MinorityAttack(color: Color, flank: String)
        ("MinorityAttack", s"$flank attack")
      case PositionalTag.Outpost(_, _) => ("Outpost", "")
      case _ => (pt.toString.takeWhile(_ != '('), "")
    }
    PositionalTagInfo(name, None, None, "White", Some(detail))
  }

  private def convertWeakComplex(wc: WeakComplex): WeakComplexInfo = 
    WeakComplexInfo(owner = wc.color.name.capitalize, squareColor = "light", squares = wc.squares.map(_.key), isOutpost = wc.isOutpost, cause = wc.cause)


  // =================================================================================
  // VERIFICATION TESTS
  // =================================================================================

  test("Narrative: Pawn Majority / Storm") {
    // Setup: White has a kingside pawn majority
    val features = List(
      PositionalTag.PawnMajority(Color.White, "kingside", 3)
    )
    val text = renderWithFeatures(positional = features)
    println(s"\n[Test Output: Pawn Majority] $text\n")
    
    // Check if the renderer mentions it
    // EXPECTED TO FAIL currently
    assert(text.toLowerCase.contains("majority") || text.toLowerCase.contains("storm"), 
           s"Text should mention pawn majority/storm. Got: $text")
  }

  test("Narrative: Hanging Pawns") {
    // Setup: Hanging Pawns structure
    val weakness = WeakComplex(Color.White, List(chess.Square.C4, chess.Square.D4), false, "Hanging Pawns")
    val text = renderWithFeatures(weaknesses = List(weakness))
    println(s"\n[Test Output: Hanging Pawns] $text\n")

    // Check if the renderer mentions it
    assert(text.toLowerCase.contains("hanging pawn"), 
           s"Text should mention hanging pawns. Got: $text")
  }
  
  test("Narrative: Minority Attack") {
    // Updated: Now we have a real PositionalTag for it!
    val features = List(
      PositionalTag.MinorityAttack(Color.White, "queenside")
    )
    val text = renderWithFeatures(positional = features)
    println(s"\n[Test Output: Minority Attack] $text\n")
    
    assert(text.toLowerCase.contains("minority attack"), 
           s"Text should mention Minority Attack from concepts. Got: $text")
  }
}
