package lila.analyse.test

import lila.llm.model.strategic.VariationLine
import lila.analyse.ui.BookmakerRenderer
import lila.llm.analysis.NarrativeUtils
import lila.llm.{ BookmakerRefsV1, MoveRefV1, VariationRefV1 }

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
    assert(
      """<span class="move-chip[^"]*" data-san="e4" data-uci="e2e4"""".r.findFirstIn(html).nonEmpty
    )
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
      """<span class="move-chip[^"]*" data-san="e4" data-uci="e2e4" data-board="rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1\|e2e4"[^>]*>e4</span>""".r
        .findFirstIn(html)
        .nonEmpty
    )
    assert(
      """<span class="move-chip[^"]*" data-san="d4" data-uci="d2d4" data-board="rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq - 0 1\|d2d4"[^>]*>d4</span>""".r
        .findFirstIn(html)
        .nonEmpty
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
      """<span class="move-chip[^"]*" data-san="Nf3" data-uci="g1f3" data-board="([^"]+)"[^>]*>Nf3</span>""".r
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

  test("variation rendering should include absolute move numbers (including black ellipsis)") {
    val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 17"
    val vars = List(
      VariationLine(
        moves = List("e7e5", "g1f3"),
        scoreCp = 15,
        mate = None
      )
    )
    val html = BookmakerRenderer.render("Main line.", vars, fen).toString
    assert(html.contains("17..."))
    assert(html.contains("18."))
  }

  test("numbered prose sequence should map each SAN in order for mini-board hover") {
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val uciLine = List("e2e4", "e7e5", "g1f3", "b8c6")
    val vars = List(VariationLine(moves = uciLine, scoreCp = 30, mate = None))
    val commentary = "Main line: 1 e4 e5 2 Nf3 Nc6."
    val html = BookmakerRenderer.render(commentary, vars, fen).toString

    val fenAfterE4 = NarrativeUtils.uciListToFen(fen, List("e2e4"))
    val fenAfterE5 = NarrativeUtils.uciListToFen(fenAfterE4, List("e7e5"))
    val fenAfterNf3 = NarrativeUtils.uciListToFen(fenAfterE5, List("g1f3"))
    val fenAfterNc6 = NarrativeUtils.uciListToFen(fenAfterNf3, List("b8c6"))

    assert(html.contains(s"""data-san="e4" data-uci="e2e4" data-board="$fenAfterE4|e2e4""""))
    assert(html.contains(s"""data-san="e5" data-uci="e7e5" data-board="$fenAfterE5|e7e5""""))
    assert(html.contains(s"""data-san="Nf3" data-uci="g1f3" data-board="$fenAfterNf3|g1f3""""))
    assert(html.contains(s"""data-san="Nc6" data-uci="b8c6" data-board="$fenAfterNc6|b8c6""""))
  }

  test("black-to-move prose sequence should preserve 17... marker and map chips in order") {
    val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 17"
    val uciLine = List("e7e5", "g1f3")
    val vars = List(VariationLine(moves = uciLine, scoreCp = 12, mate = None))
    val commentary = "Critical line: 17... e5! 18 Nf3."
    val html = BookmakerRenderer.render(commentary, vars, fen).toString

    val fenAfterE5 = NarrativeUtils.uciListToFen(fen, List("e7e5"))
    val fenAfterNf3 = NarrativeUtils.uciListToFen(fenAfterE5, List("g1f3"))

    assert(html.contains("17..."))
    assert(html.contains(s"""data-san="e5" data-uci="e7e5" data-board="$fenAfterE5|e7e5""""))
    assert(html.contains(s"""data-san="Nf3" data-uci="g1f3" data-board="$fenAfterNf3|g1f3""""))
  }

  test("annotated SAN in prose should still map to move chips by canonical SAN") {
    val fen = "6k1/8/8/8/8/8/4Q3/4K3 w - - 0 1"
    val vars = List(
      VariationLine(
        moves = List("e2e8"),
        scoreCp = 900,
        mate = Some(1)
      )
    )
    val html = BookmakerRenderer.render("Critical sequence: Qe8!.", vars, fen).toString
    assert(
      """<span class="move-chip[^"]*" data-san="Qe8" data-uci="e2e8"""".r.findFirstIn(html).nonEmpty
    )
    assert(html.contains(">Qe8!</span>"))
  }

  test("refs contract should emit data-ref-id for prose chips and variation chips") {
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val vars = List(
      VariationLine(moves = List("e2e4", "e7e5"), scoreCp = 28, mate = None)
    )
    val fenAfterE4 = NarrativeUtils.uciListToFen(fen, List("e2e4"))
    val fenAfterE5 = NarrativeUtils.uciListToFen(fenAfterE4, List("e7e5"))
    val refs = BookmakerRefsV1(
      startFen = fen,
      startPly = 1,
      variations = List(
        VariationRefV1(
          lineId = "line_01",
          scoreCp = 28,
          mate = None,
          depth = 0,
          moves = List(
            MoveRefV1(
              refId = "l01_m01",
              san = "e4",
              uci = "e2e4",
              fenAfter = fenAfterE4,
              ply = 1,
              moveNo = 1,
              marker = Some("1.")
            ),
            MoveRefV1(
              refId = "l01_m02",
              san = "e5",
              uci = "e7e5",
              fenAfter = fenAfterE5,
              ply = 2,
              moveNo = 1,
              marker = Some("1...")
            )
          )
        )
      )
    )

    val html = BookmakerRenderer.render("Critical line: 1 e4 e5.", vars, fen, refs = Some(refs)).toString
    assert(html.contains("""data-ref-id="l01_m01""""))
    assert(html.contains("""data-ref-id="l01_m02""""))
    assert(html.contains(s"""data-board="$fenAfterE4|e2e4""""))
    assert(html.contains(s"""data-board="$fenAfterE5|e7e5""""))
  }
