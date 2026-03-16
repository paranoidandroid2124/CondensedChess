package lila.llm

import munit.FunSuite

class PgnAnalysisHelperTest extends FunSuite:

  test("extractPlyDataStrict accepts complete legal PGN replay") {
    val pgn =
      """[Event "Mini"]
        |[Site "?"]
        |[Date "2026.03.16"]
        |[Round "?"]
        |[White "White"]
        |[Black "Black"]
        |[Result "1-0"]
        |
        |1.e4 e5 2.Nf3 Nc6 3.Bb5 a6 4.Ba4 Nf6 1-0
        |""".stripMargin

    val result = PgnAnalysisHelper.extractPlyDataStrict(pgn)

    assert(result.isRight, clues(result))
    assertEquals(result.toOption.map(_.size), Some(8))
  }

  test("extractPlyDataStrict rejects replay truncation instead of silently swallowing it") {
    val invalidBenkoFragment =
      """[Event "X Acropolis Tournament (Cat.13)"]
        |[Site "Athens GRE"]
        |[Date "1992.08.30"]
        |[Round "9"]
        |[Result "1-0"]
        |[White "Viswanathan Anand"]
        |[Black "Mickey Adams"]
        |[ECO "A57"]
        |
        |1.d4 Nf6 2.c4 c5 3.d5 b5 4.cxb5 a6 5.bxa6 g6 6.Nc3 Bxa6 7.e4 Bxf1 8.Kxf1 d6 9.Nf3 Bg7 10.Kg2 Qa5 1-0
        |""".stripMargin

    val result = PgnAnalysisHelper.extractPlyDataStrict(invalidBenkoFragment)

    assert(result.isLeft, clues(result))
    assert(result.left.toOption.exists(_.contains("PGN replay truncated after 18/20 plies")), clues(result))
  }
