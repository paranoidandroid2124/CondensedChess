package lila.llm.analysis

import lila.llm._
import lila.llm.model._
import lila.llm.model.strategic._
import munit.FunSuite

class A12BookStyleOutputTest extends FunSuite {

  test("Book Style Renderer: Output is prose paragraphs, not structured sections") {
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

    val ctx = NarrativeContextBuilder.build(data, data.toContext)

    // Generate Book Style output
    val bookOutput = BookStyleRenderer.render(ctx)
    println("=== BOOK STYLE OUTPUT ===")
    println(bookOutput)
    println("=== END ===")
    
    // Verify NO structured sections
    assert(!bookOutput.contains("==="), "Should not contain section headers (===)")
    assert(!bookOutput.contains("•"), "Should not contain bullet points")
    assert(!bookOutput.contains("[Competitive]"), "Should not expose raw tags")
    assert(!bookOutput.contains("[TacticalGamble]"), "Should not expose raw tags")
    
    // Verify prose structure
    assert(bookOutput.split("\n\n").length >= 2, "Should have multiple paragraphs")
    
    // Verify key phrases expected in book style
    assert(bookOutput.toLowerCase.contains("position") || bookOutput.toLowerCase.contains("the"), 
      "Should contain natural prose")
    
    // Write output for manual review
    java.nio.file.Files.writeString(
      java.nio.file.Path.of("a12_book_style_output.txt"),
      bookOutput
    )
  }

  test("renderTrace: Shows field usage transparency") {
    val fen = "r1bq1rk1/pp2bppp/2n1pn2/2pp4/2PP4/2N1PN2/PP2BPPP/R1BQ1RK1 w - - 4 8"
    val multiPv = List(
      VariationLine(moves = List("b2b3"), scoreCp = 20, mate = None, depth = 20),
      VariationLine(moves = List("c1d2"), scoreCp = 15, mate = None, depth = 20)
    )

    val data = CommentaryEngine.assessExtended(
      fen = fen,
      variations = multiPv,
      playedMove = Some("b2b3"),
      ply = 10,
      prevMove = None
    ).getOrElse(fail("Analysis failed"))

    val ctx = NarrativeContextBuilder.build(data, data.toContext)
    
    val trace = BookStyleRenderer.renderTrace(ctx)
    println("=== TRACE OUTPUT ===")
    println(trace)
    
    // Verify it's a markdown table
    assert(trace.contains("| Field |"), "Should have Field column")
    assert(trace.contains("| Status |"), "Should have Status column")
    assert(trace.contains("USED"), "Should show USED fields")
    
    // Write for inspection
    java.nio.file.Files.writeString(
      java.nio.file.Path.of("a12_trace_output.txt"),
      trace
    )
  }

  test("renderFull: Provides lossless structured appendix") {
    val fen = "r1bq1rk1/pp2bppp/2n1pn2/2pp4/2PP4/2N1PN2/PP2BPPP/R1BQ1RK1 w - - 4 8"
    val multiPv = List(
      VariationLine(moves = List("b2b3"), scoreCp = 20, mate = None, depth = 20),
      VariationLine(moves = List("c1d2"), scoreCp = 15, mate = None, depth = 20)
    )

    val data = CommentaryEngine.assessExtended(
      fen = fen,
      variations = multiPv,
      playedMove = Some("b2b3"),
      ply = 10,
      prevMove = None
    ).getOrElse(fail("Analysis failed"))

    val ctx = NarrativeContextBuilder.build(data, data.toContext)
    
    val full = BookStyleRenderer.renderFull(ctx)
    println("=== FULL APPENDIX OUTPUT ===")
    println(full.take(500) + "...")
    
    // Verify key sections exist
    assert(full.contains("## Context"), "Should have Context section")
    assert(full.contains("## Summary"), "Should have Summary section")
    assert(full.contains("## Threats"), "Should have Threats section")
    assert(full.contains("## Semantic Layer"), "Should have Semantic section")
    assert(full.contains("## Candidates"), "Should have Candidates section")
    
    // Write for inspection
    java.nio.file.Files.writeString(
      java.nio.file.Path.of("a12_appendix_output.txt"),
      full
    )
  }
  
}
