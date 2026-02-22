package lila.llm.analysis

import munit.FunSuite
import _root_.chess.Color
import lila.llm.model.strategic.VariationLine

class ThreatExtractorFallbackPerspectiveTest extends FunSuite {

  test("fallback eval-drop uses mover perspective for black") {
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1"

    val bestLine = VariationLine(
      moves = List("d7d5", "d2d4"),
      scoreCp = 0
    )
    val userLine = VariationLine(
      // Intentionally invalid UCIs to force motif-free fallback path.
      moves = List("zzzz", "yyyy"),
      scoreCp = 300
    )

    val threat = ThreatExtractor.extractCounterfactualCausality(
      fen = fen,
      playerColor = Color.Black,
      userLine = userLine,
      bestLine = bestLine
    )

    assert(threat.isDefined, "Black mover should trigger fallback on large mover-relative loss")
    assertEquals(threat.get.concept, "Positional Collapse")
    assertEquals(threat.get.motifs, Nil)
  }
}
