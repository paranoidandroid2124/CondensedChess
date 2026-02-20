package lila.analyse.test

import lila.llm.model.strategic.VariationLine
import lila.analyse.ui.BookmakerRenderer
import lila.llm.analysis.NarrativeUtils

class BookmakerRendererTest extends munit.FunSuite:

  test("should render interactive variation spans and preview container") {
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val vars = List(
      VariationLine(
        moves = List("e2e4"),
        scoreCp = 50,
        mate = None,
        resultingFen = Some("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1")
      )
    )
    val commentary = "White should play e4."
    val html = BookmakerRenderer.render(commentary, vars, fen).toString

    assert(html.contains("pv-line"))
    assert(html.contains("bookmaker-pv-preview"))
    assert(html.contains("""data-board="rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1|e2e4""""))
    assert(html.contains("""<span class="move-chip" data-san="e4" data-uci="e2e4""""))
    assert(html.contains("e4"))
    assert(html.contains("+0.5"))
  }

  test("should identify and chip-ify moves in commentary text using variation map") {
    val commentary = "The move e4 is strong, while d4 is also playable."
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val vars = List(
      VariationLine(moves = List("e2e4"), scoreCp = 30, mate = None),
      VariationLine(moves = List("d2d4"), scoreCp = 25, mate = None)
    )
    val html = BookmakerRenderer.render(commentary, vars, fen).toString

    assert(
      html.contains(
        """<span class="move-chip" data-san="e4" data-uci="e2e4" data-board="rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1|e2e4">e4</span>"""
      )
    )
    assert(
      html.contains(
        """<span class="move-chip" data-san="d4" data-uci="d2d4" data-board="rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq - 0 1|d2d4">d4</span>"""
      )
    )
  }

  test("moves not in variations only get data-san") {
    val commentary = "Castling O-O is possible later."
    val html = BookmakerRenderer.render(commentary, Nil, "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1").toString
    assert(html.contains("<span class=\"move-chip\" data-san=\"O-O\">O-O</span>"))
  }

  test("same SAN in multiple branches keeps occurrence order without twisting board context") {
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val commentary = "Nf3 is natural. In another branch, Nf3 appears later."
    val vars = List(
      VariationLine(moves = List("g1f3", "d7d5"), scoreCp = 20, mate = None),
      VariationLine(moves = List("d2d4", "g8f6", "g1f3"), scoreCp = 18, mate = None)
    )
    val html = BookmakerRenderer.render(commentary, vars, fen).toString
    val pattern =
      """<span class="move-chip" data-san="Nf3" data-uci="g1f3" data-board="([^"]+)">Nf3</span>""".r
    val boards = pattern.findAllMatchIn(html).map(_.group(1)).toList

    val firstFen = NarrativeUtils.uciListToFen(fen, List("g1f3"))
    val secondFen = NarrativeUtils.uciListToFen(fen, List("d2d4", "g8f6", "g1f3"))
    def normalizeFen(f: String): String = f.split(" ").take(4).mkString(" ")
    val obtained = boards.map { b =>
      val parts = b.split("\\|", 2)
      (normalizeFen(parts(0)), parts(1))
    }
    val expected = List(
      (normalizeFen(firstFen), "g1f3"),
      (normalizeFen(secondFen), "g1f3")
    )
    assertEquals(obtained, expected)
  }

  test("should handle empty variations gracefully") {
    val html = BookmakerRenderer.render("Just text.", Nil, "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1").toString
    assert(!html.contains("Alternative Options"))
    assert(html.contains("Just text."))
  }
