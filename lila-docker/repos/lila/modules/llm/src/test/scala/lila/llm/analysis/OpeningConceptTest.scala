package lila.llm.analysis

import munit.FunSuite
import lila.llm.model._
import lila.llm.model.strategic._

class OpeningConceptTest extends FunSuite {

  def mockCtx(
    fen: String, 
    uci: String, 
    cp: Int = 0, 
    ply: Int = 10,
    kingSafe: Boolean = true, 
    center: Boolean = true
  ): NarrativeContext = {
    val vLine = VariationLine(moves = List(uci), scoreCp = cp)
    val engineEv = EngineEvidence(depth = 18, variations = List(vLine))

    val snapshot = L1Snapshot(
      material = "=",
      imbalance = None,
      kingSafetyUs = Some(if (kingSafe) "Safe" else "Exposed"),
      kingSafetyThem = None,
      mobility = None,
      centerControl = Some(if (center) "Dominant" else "Weak"),
      openFiles = Nil
    )

    NarrativeContext(
      fen = fen,
      header = ContextHeader("Opening", "Normal", "OnlyMove", "Low", "ExplainPlan"),
      ply = ply,
      playedMove = Some(uci),
      playedSan = Some(uci),
      summary = NarrativeSummary("Plan", None, "Choice", "Policy", "Eval"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Reason", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Opening", "Material"),
      candidates = Nil,
      engineEvidence = Some(engineEv),
      semantic = None,
      snapshots = List(snapshot)
    )
  }

  val emptyFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

  test("1. Sicilian Liberator - Achieved") {
    val ctx = mockCtx(
      fen = "r1bqkbnr/pp2pppp/2n5/2pP4/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 1", // With c5 pawn
      uci = "d7d5",
      cp = 10,
      kingSafe = true
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.status, OpeningGoals.Status.Achieved)
    assertEquals(result.get.goalName, "Sicilian Liberator")
  }

  test("1.1. Sicilian Liberator (Open) - Achieved") {
    val ctx = mockCtx(
      fen = "r1bqk1r1/pp2ppbp/2np1np1/8/3pP3/2N2N2/PPP1BPPP/R1BQK2R w KQ - 0 1", // Najdorf/Dragon like footprint (e4, d6, no c7/c5)
      uci = "d7d5",
      cp = 10,
      kingSafe = true
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.status, OpeningGoals.Status.Achieved)
    assertEquals(result.get.goalName, "Sicilian Liberator")
  }

  test("2. French Base Chipper - Premature (Bad CP)") {
    val ctx = mockCtx(
      fen = "rnbqkbnr/ppp2ppp/4p3/3p4/3PP3/8/PPP2PPP/RNBQKBNR b KQkq - 0 1", // French structure (e6 + d5)
      uci = "c7c5",
      cp = 250, // Black is losing by 2.5 pawns (normalized sideScore = -250)
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.status, OpeningGoals.Status.Premature)
    assert(result.get.missingEvidence.contains("Soundness"))
  }

  test("3. French Chain Breaker - Achieved") {
    val ctx = mockCtx(
      fen = "r1bqkbnr/pp4pp/2n1pp2/3pP3/3P4/5N2/PPP2PPP/RNBQKB1R b KQkq - 0 1", // French (e6, d5) + White e5
      uci = "f7f6",
      cp = -20
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.goalName, "French Chain Breaker")
    assertEquals(result.get.status, OpeningGoals.Status.Achieved)
  }

  test("4. KID Kingside Storm - Achieved") {
    val ctx = mockCtx(
      fen = "rnbq1rk1/ppp1ppbp/3p1np1/8/2PPP3/2N2N2/PP2BPPP/R1BQK2R b KQ - 0 1", // KID setup (g6, d6, Nf6)
      uci = "f7f5",
      cp = -40
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.status, OpeningGoals.Status.Achieved)
    assertEquals(result.get.goalName, "Kingside Storm")
  }

  test("5. Benoni Expansion - Achieved") {
    val ctx = mockCtx(
      fen = "rnbq1rk1/pp1p1pbp/3p1np1/2pP4/8/2N2N2/PP2BPPP/R1BQK2R b KQ - 0 1", // Benoni (c5, d6, White d5)
      uci = "b7b5",
      cp = -30
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.goalName, "Benoni Expansion")
  }

  test("6. Catalan Expansion - Achieved") {
    val ctx = mockCtx(
      fen = "rnbq1rk1/ppp1ppbp/3p1np1/8/2PP4/2N2NP1/PP2BP1P/R1BQK2R w KQ - 0 1", // Catalan (d4, g3/Bg2)
      uci = "g2g3",
      cp = 50
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.goalName, "Catalan Expansion")
  }

  test("7. QG Challenge - Achieved") {
    val ctx = mockCtx(
      fen = "rnbqkb1r/ppp1pppp/5n2/3p4/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 1", // QG (d5, c4)
      uci = "c7c5",
      cp = 10
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.status, OpeningGoals.Status.Achieved)
  }

  test("8. London Pyramid Peak - Achieved") {
    val ctx = mockCtx(
      // London core: d4, e3, Bf4, Nf3
      fen = "rnbqkb1r/pp1ppppp/5n2/2p5/3P1B2/4PN2/PPP2PPP/RN1QKB1R b KQkq - 0 1",
      uci = "e2e4",
      cp = 30
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.goalName, "London Pyramid Peak")
    assertEquals(result.get.status, OpeningGoals.Status.Achieved)
  }

  test("9. Caro-Kann Liquidator - Achieved") {
    val ctx = mockCtx(
      fen = "rnbqkbnr/pp2pppp/2p5/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1", // Caro-Kann (c6, d5)
      uci = "c6c5",
      cp = 15 // Black POV
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.goalName, "Caro-Kann Liquidator")
  }

  test("10. Nimzo-Indian Challenge - Achieved") {
    val ctx = mockCtx(
      fen = "rnbqk2r/pppp1ppp/4pn2/8/1bPP4/2N5/PP2PPPP/R1BQKBNR w KQkq - 0 1", // Nimzo (Bb4 pin on Nc3)
      uci = "c7c5",
      cp = 10
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.goalName, "Nimzo-Indian Challenge")
  }

  test("11. Scandinavian Expansion - Achieved (Immediate response)") {
    val ctx = mockCtx(
      fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1", // 1.e4 played
      uci = "d7d5",
      cp = 30,
      ply = 2
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.goalName, "Scandinavian Expansion")
    assertEquals(result.get.status, OpeningGoals.Status.Achieved)
  }

  test("12. KID Kingside Storm - Partial (Tight sound)") {
    val ctx = mockCtx(
      fen = "rnbq1rk1/ppp1ppbp/3p1np1/8/2PPP3/2N2N2/PP2BPPP/R1BQK2R b KQ - 0 1",
      uci = "f7f5",
      cp = 80 // Black is losing by 0.8 pawns (normalized sideScore = -80)
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.status, OpeningGoals.Status.Partial)
  }

  test("13. Overlapping Triggers - Score-driven selection (Nimzo > French)") {
    // A rare setup with d5, e6 AND Bb4 pin
    val ctx = mockCtx(
      fen = "rnbqk2r/ppp1bppp/4pn2/3p4/1bPP4/2N5/PP2PPPP/R1BQKBNR w KQkq - 0 1",
      uci = "c7c5",
      cp = -10
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    // NimzoChallenge has 0.95, FrenchBaseChipper/QGChallenge have 0.9
    assertEquals(result.get.goalName, "Nimzo-Indian Challenge")
    assertEquals(result.get.status, OpeningGoals.Status.Achieved)
  }

  test("14. Open Center d4 Break - Achieved") {
    val ctx = mockCtx(
      fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 1", // Ruy/Italian/Scotch context
      uci = "d2d4",
      cp = 40
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.goalName, "Open Center d4 Break")
    assertEquals(result.get.status, OpeningGoals.Status.Achieved)
  }

  test("15. d5 Equalizer - Achieved") {
    val ctx = mockCtx(
      fen = "r1bqk1r1/pppp1ppp/2n2n2/4p3/4P3/2N2N2/PPPP1PPP/R1BQKB1R b KQ - 0 1", // Four Knights context
      uci = "d7d5",
      cp = 10,
      kingSafe = true
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.goalName, "d5 Equalizer")
    assertEquals(result.get.status, OpeningGoals.Status.Achieved)
  }

  test("16. English Squeeze - Achieved") {
    val ctx = mockCtx(
      fen = "rnbqkbnr/pppppppp/8/8/2P5/2N3P1/PP1PPP1P/R1BQKBNR w KQkq - 0 1", // White c4, g3, Nc3 (suppression)
      uci = "g2g3",
      cp = 30
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.goalName, "English Squeeze")
    assertEquals(result.get.status, OpeningGoals.Status.Achieved)
    assert(result.get.supportedEvidence.head.contains("d5 restraint established"))
  }

  test("16.1. English Squeeze - Premature (No suppression)") {
    val ctx = mockCtx(
      fen = "rnbqkbnr/pp1ppppp/8/2p5/2P5/6P1/PP1PPP1P/RNBQKBNR w KQkq - 0 1", // White c4, g3 - but no Nc3/Nf3/e4
      uci = "g2g3",
      cp = 10
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.status, OpeningGoals.Status.Premature)
    assert(result.get.missingEvidence.head.contains("d5 restraint"))
  }

  test("17. Austrian Attack - Achieved") {
    val ctx = mockCtx(
      fen = "rnbqkb1r/ppp1pp1p/3p1np1/8/3PPP2/2N5/PPP3PP/R1BQKBNR w KQkq - 0 1", // White e4, d4, f4 vs Black d6, g6 AND Nc3
      uci = "f2f4",
      cp = 60
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.goalName, "Austrian Attack")
    assertEquals(result.get.status, OpeningGoals.Status.Achieved)
    assert(result.get.supportedEvidence.head.contains("development/coordination"))
  }

  test("17.1. Austrian Attack - Premature (No minor piece)") {
    val ctx = mockCtx(
      fen = "rnbqkb1r/ppp1pp1p/3p1np1/8/3PPP2/8/PPP3PP/RNBQKBNR w KQkq - 0 1", // White center but no minor piece development
      uci = "f2f4",
      cp = 10
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.status, OpeningGoals.Status.Premature)
    assert(result.get.missingEvidence.head.contains("center a target"))
  }

  test("18. Instructional Feedback - Mismatch with reason") {
    val ctx = mockCtx(
      fen = "rnbqkbnr/pppp1ppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1", // Only e4, no e5 (not an Open Game)
      uci = "d7d5",
      cp = 10
    )
    val result = OpeningGoals.analyze(ctx)
    assert(result.isDefined)
    assertEquals(result.get.status, OpeningGoals.Status.Mismatch)
    assert(result.get.mismatchReason.isDefined)
    assert(result.get.mismatchReason.get.contains("typical in Open Games"))
  }
}
