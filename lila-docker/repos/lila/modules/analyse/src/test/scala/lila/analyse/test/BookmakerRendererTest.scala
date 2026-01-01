package lila.analyse.test

import lila.llm.model.strategic.VariationLine
import lila.analyse.ui.BookmakerRenderer

class BookmakerRendererTest extends munit.FunSuite:

  test("should wrap variations in pv-line spans with data-fen") {
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
    
    // Check for interactive markers
    assert(html.contains("pv-line"))
    assert(html.contains("mini-board"))
    assert(html.contains("data-fen=\"rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1\""))
    assert(html.contains("data-color=\"white\"")) // Always white perspective
    assert(html.contains("data-lastmove=\"e2e4\""))
    
    // Check for SAN conversion (e2e4 -> e4)
    assert(html.contains("e4"))
    
    // Check for eval display
    assert(html.contains("+0.5"))
  }
  
  test("should identify and chip-ify moves in commentary text using variation map") {
    val commentary = "The move e4 is strong, while d4 is also playable."
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    // Provide variations so the SAN→UCI map is populated
    val vars = List(
      VariationLine(moves = List("e2e4"), scoreCp = 30, mate = None),
      VariationLine(moves = List("d2d4"), scoreCp = 25, mate = None)
    )
    val html = BookmakerRenderer.render(commentary, vars, fen).toString
    
    // e4 and d4 are in the variations, so they get data-uci
    assert(html.contains("<span class=\"move-chip\" data-san=\"e4\" data-uci=\"e2e4\">e4</span>"))
    assert(html.contains("<span class=\"move-chip\" data-san=\"d4\" data-uci=\"d2d4\">d4</span>"))
  }
  
  test("moves not in variations only get data-san") {
    val commentary = "Castling O-O is possible later."
    val html = BookmakerRenderer.render(commentary, Nil, "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1").toString
    // O-O is not in any variation, so no data-uci
    assert(html.contains("<span class=\"move-chip\" data-san=\"O-O\">O-O</span>"))
  }

  test("should handle empty variations gracefully") {
    val html = BookmakerRenderer.render("Just text.", Nil, "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1").toString
    assert(!html.contains("Alternative Options"))
    assert(html.contains("Just text."))
  }
