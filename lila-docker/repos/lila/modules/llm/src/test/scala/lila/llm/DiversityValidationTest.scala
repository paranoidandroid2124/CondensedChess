package lila.llm

import munit.FunSuite
import lila.llm.model._
import lila.llm.analysis.BookStyleRenderer

class DiversityValidationTest extends FunSuite {

  test("Scenario 1: Tactical Crisis (Threat -> Defense Link)") {
    val ctx = DiversityTestHelper.tacticalCrisis
    val result = BookStyleRenderer.render(ctx)
    
    println(s"\n=== SCENARIO 1: TACTICAL ===\n$result\n============================")

    // Intent 1: Cohesion (Max 2 blocks)
    assert(result.split("\n\n").length <= 4, "Should have condensed to a few paragraphs")
    
    // Intent 2: Organic Link (Threat mentions Plan)
    val combinedText = result.replace("\n", " ")
    assert(combinedText.contains("threat"), "Should mention threat")
    assert(combinedText.contains("defense") || combinedText.contains("Defense"), "Should mention defense")
  }

  test("Scenario 2: Strategic Quiet (Positional Maneuver)") {
    val ctx = DiversityTestHelper.strategicQuiet
    val result = BookStyleRenderer.render(ctx)
    
    println(s"\n=== SCENARIO 2: STRATEGIC ===\n$result\n=============================")

    assert(result.split("\n\n").length <= 4, "Should have condensed to a few paragraphs")
    assert(result.contains("tension") || result.contains("Tension"), "Should mention tension")
  }

  test("Scenario 3: Endgame Precision (Promotion Race)") {
    val ctx = DiversityTestHelper.endgame
    val result = BookStyleRenderer.render(ctx)
    
    println(s"\n=== SCENARIO 3: ENDGAME ===\n$result\n===========================")

    assert(result.split("\n\n").length <= 4, "Should have condensed to a few paragraphs")
    assert(result.toLowerCase.contains("endgame") || result.toLowerCase.contains("winning"), "Should mention endgame context")
  }

}

object DiversityTestHelper {
  
  // Helpers to build simplified contexts
  private val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  def emptySnap = L1Snapshot("Material safe", None, None, None, None, None, Nil)
  def emptyHeader = ContextHeader("Opening", "Normal", "Standard", "Low", "ExplainPlan")
  def emptyPhase = PhaseContext("opening", "Development")

  // Mock helpers
  def mockSummary(s: String) = NarrativeSummary(s, None, "NarrowChoice", "Maintain", "+0.0")
  def mockRow(kind: String) = ThreatRow(kind, "US", Some("h2"), 500, 1, Some("g3"), 1, false, ConfidenceLevel.Engine)
  def mockPlan(name: String) = PlanRow(1, name, 10.0, List("evidence"), Nil, Nil, Nil, ConfidenceLevel.Engine)
  def mockPawn(policy: String) = PawnPlayTable(false, None, "Low", policy, "reason", "Background", None, false, "quiet")
  
  // Candidate helper (Phase 22.5: added downstreamTactic)
  def mockCand(move: String, ann: String, align: String, whyNot: Option[String]) =
    CandidateInfo(
      move = move,
      uci = None,
      annotation = ann,
      planAlignment = align,
      tacticalAlert = None,
      practicalDifficulty = "clean",
      whyNot = whyNot
    )


  val tacticalCrisis = NarrativeContext(
    fen = startFen,
    header = emptyHeader.copy(phase = "Middlegame"),
    ply = 1,
    phase = PhaseContext("middlegame", "Tactical"),
    summary = mockSummary("Defend against mate"),
    snapshots = List(emptySnap),
    threats = ThreatTable(
      toUs = List(mockRow("Tactical")),
      toThem = Nil
    ),
    pawnPlay = mockPawn("Maintain"),
    plans = PlanTable(
      top5 = List(mockPlan("Defense")),
      suppressed = Nil
    ),
    candidates = List(
       mockCand("g3", "!", "defense", Some("verified")),
       mockCand("h3", "?", "defense", Some("weakening king safety"))
    ),
    semantic = None,
    delta = None,
    decision = None,
    opponentPlan = Some(mockPlan("Kingside Attack")),
    probeRequests = Nil
  )
  
  val strategicQuiet = NarrativeContext(
    fen = startFen,
    header = emptyHeader.copy(phase = "Middlegame"),
    ply = 1,
    phase = PhaseContext("middlegame", "Strategic"),
    summary = mockSummary("Improve position"),
    snapshots = List(emptySnap),
    threats = ThreatTable(Nil, Nil),
    pawnPlay = mockPawn("Maintain"),
    plans = PlanTable(
      top5 = List(mockPlan("Minor Piece improvement")),
      suppressed = Nil
    ),
    candidates = List(
       mockCand("Nd2", "", "development", Some("competitive")),
       mockCand("Be3", "", "development", Some("competitive"))
    ),
    semantic = Some(SemanticSection(Nil, Nil, Nil, None, None, 
      Some(PracticalInfo(30, 0.3, "Slightly better")), Nil, Nil)),
    delta = None,
    decision = None,
    opponentPlan = Some(mockPlan("Consolidation")),
    probeRequests = Nil
  )

  val endgame = NarrativeContext(
    fen = startFen,
    header = emptyHeader.copy(phase = "Endgame"),
    ply = 1,
    phase = PhaseContext("endgame", "Technical"),
    summary = mockSummary("Push pawns"),
    snapshots = List(emptySnap.copy(material = "R vs R")),
    threats = ThreatTable(Nil, Nil),
    pawnPlay = mockPawn("Maintain").copy(primaryDriver = "passed_pawn"),
    plans = PlanTable(
      top5 = List(mockPlan("Promotion")),
      suppressed = Nil
    ),
    candidates = List(
       mockCand("a5", "!", "improves the position", Some("verified"))
    ),
    semantic = Some(SemanticSection(Nil, Nil, Nil, None, None, 
      Some(PracticalInfo(250, 2.5, "Winning")), Nil, Nil)),
    delta = None,
    decision = None,
    opponentPlan = None,
    probeRequests = Nil
  )
}
