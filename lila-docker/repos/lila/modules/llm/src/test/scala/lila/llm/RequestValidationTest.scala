package lila.llm

import munit.FunSuite

class RequestValidationTest extends FunSuite:

  private val ShortGamePgn =
    """[Event "Mini"]
      |[Site "?"]
      |[Date "2026.03.19"]
      |[Round "?"]
      |[White "White"]
      |[Black "Black"]
      |[Result "*"]
      |
      |1.e4 e5 2.Nf3 Nc6 *
      |""".stripMargin

  private val LongEnoughGamePgn =
    """[Event "Mini"]
      |[Site "?"]
      |[Date "2026.03.19"]
      |[Round "?"]
      |[White "White"]
      |[Black "Black"]
      |[Result "*"]
      |
      |1.e4 e5 2.Nf3 Nc6 3.Bb5 a6 4.Ba4 Nf6 5.O-O *
      |""".stripMargin

  test("bookmaker rejects positions before ply five") {
    val req =
      CommentRequest(
        fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
        lastMove = Some("e2e4"),
        eval = None,
        context = PositionContext(None, "opening", 4)
      )

    val result = CommentRequest.validateBookmaker(req)

    assertEquals(result.left.toOption.map(_.code), Some("bookmaker_too_early"))
    val error = result.left.toOption.getOrElse(fail("expected bookmaker_too_early"))
    assert(error.message.contains("move 3"), clues(error.message))
    assert(!error.message.toLowerCase.contains("ply"), clues(error.message))
  }

  test("bookmaker accepts positions from ply five onward") {
    val req =
      CommentRequest(
        fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3",
        lastMove = Some("b8c6"),
        eval = None,
        context = PositionContext(None, "opening", 5)
      )

    val result = CommentRequest.validateBookmaker(req)

    assert(result.isRight, clues(result))
  }

  test("game chronicle rejects games shorter than nine plies") {
    val req =
      FullAnalysisRequest(
        pgn = ShortGamePgn,
        evals = Nil,
        options = AnalysisOptions(style = "book", focusOn = List("mistakes", "turning_points"))
      )

    val result = FullAnalysisRequest.validateGameChronicle(req)

    assertEquals(result.left.toOption.map(_.code), Some("game_chronicle_too_short"))
    val error = result.left.toOption.getOrElse(fail("expected game_chronicle_too_short"))
    assert(error.message.contains("move 5"), clues(error.message))
    assert(!error.message.toLowerCase.contains("ply"), clues(error.message))
  }

  test("game chronicle accepts games from ply nine onward") {
    val req =
      FullAnalysisRequest(
        pgn = LongEnoughGamePgn,
        evals = Nil,
        options = AnalysisOptions(style = "book", focusOn = List("mistakes", "turning_points"))
      )

    val result = FullAnalysisRequest.validateGameChronicle(req)

    assert(result.isRight, clues(result))
  }
