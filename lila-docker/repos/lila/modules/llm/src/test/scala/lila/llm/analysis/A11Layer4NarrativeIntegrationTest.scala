package lila.llm.analysis

import lila.llm._
import lila.llm.model._
import lila.llm.model.strategic._
import lila.llm.model.CandidateTag
import lila.llm.analysis.L3.PvLine
import chess.format.Fen
import munit.FunSuite

class A11Layer4NarrativeIntegrationTest extends FunSuite {

  test("Narrative Integration: Verify probe-enhanced candidate descriptions") {
    val fen = "r1bq1rk1/pp2bppp/2n1pn2/2pp4/2PP4/2N1PN2/PP2BPPP/R1BQ1RK1 w - - 4 8"
    val multiPv = List(
      VariationLine(moves = List("b2b3"), scoreCp = 20, mate = None, depth = 20),
      VariationLine(moves = List("c1d2"), scoreCp = 15, mate = None, depth = 20),
      VariationLine(moves = List("c4d5"), scoreCp = -60, mate = None, depth = 20)
    )

    val data = CommentaryEngine.assessExtended(
      fen = fen,
      variations = multiPv,
      playedMove = Some("b2b3"),
      ply = 10,
      prevMove = None
    ).getOrElse(fail("Analysis failed"))

    // Mock ProbeResults
    val probeResults = List(
      ProbeResult(
        id = "competitive_c1d2_mock",
        evalCp = 18,
        deltaVsBaseline = -2, // vs b2b3(20)
        bestReplyPv = List("c7c6"),
        keyMotifs = Nil,
        probedMove = Some("c1d2")
      ),
      ProbeResult(
        id = "aggressive_why_not_c4d5_mock",
        evalCp = -65,
        deltaVsBaseline = -85, // vs b2b3(20)
        bestReplyPv = List("e6d5"),
        keyMotifs = List("IsolatedPawn"),
        probedMove = Some("c4d5"),
        l1Delta = Some(L1DeltaSnapshot(
           materialDelta = -100,
           kingSafetyDelta = 0,
           centerControlDelta = -2,
           openFilesDelta = 0,
           mobilityDelta = -5,
           collapseReason = Some("Lost center control")
        ))
      )
    )

    val ctx = NarrativeContextBuilder.build(data, data.toContext, probeResults = probeResults)

    // Check Candidate Enrichment
    val candidates = ctx.candidates
    
    // 1. Check Competitive Alternative (c1d2)
    val c1d2 = candidates.find(c => c.move.contains("Bd2") || c.uci.contains("c1d2")).getOrElse(fail("Bd2 not found"))
    assert(c1d2.tags.contains(CandidateTag.Competitive), "Bd2 should be tagged Competitive")
    assert(c1d2.whyNot.contains("verified as a strong alternative"), s"Unexpected whyNot for Bd2: ${c1d2.whyNot}")

    // 2. Check (c4d5) - This is a "Ghost" in this scenario
    // Candidate C (cxd5) should have TacticalGamble tag due to refutation
    val cCand = candidates.find(_.move.startsWith("cxd5")).getOrElse(fail("cxd5 ghost not found"))
    
    // cCand is CandidateInfo (unwrapped)
    val hasGambleTag = cCand.tags.exists(_.toString == "TacticalGamble")
    assert(hasGambleTag, "Candidate 'cxd5' should be tagged as TacticalGamble")
    
    val whyNot = cCand.whyNot.getOrElse("")
    assert(whyNot.contains("refuted: exd5 is a clear answer"), s"Unexpected whyNot for c4d5: $whyNot")
    assert(whyNot.contains("Lost center control"), "Should include L1 collapse reason")

    // Check Narrative Generation
    val narrative = NarrativeGenerator.describeHierarchical(ctx)
    println("=== INTEGRATED NARRATIVE OUTPUT ===")
    println(narrative)
    
    assert(narrative.contains("[Competitive]") || narrative.contains("competitive alternative"), "Narrative should surface Competitive tag")
    assert(narrative.contains("tactical attempt"), "Narrative should surface TacticalGamble as 'tactical attempt'")
    assert(narrative.contains("refuted: exd5 is a clear answer"), "Narrative should surface refutation string")
    
    java.nio.file.Files.writeString(
      java.nio.file.Path.of("a11_narrative_integration_output.txt"),
      narrative
    )
  }
}
