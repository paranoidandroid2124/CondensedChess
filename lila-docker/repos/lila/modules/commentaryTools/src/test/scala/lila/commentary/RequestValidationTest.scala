package lila.commentary

import munit.FunSuite

class RequestValidationTest extends FunSuite:

  test("move review rejects positions before ply five") {
    val req =
      CommentRequest(
        fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
        lastMove = Some("e2e4"),
        eval = None,
        context = PositionContext(None, "opening", 4)
      )

    val result = CommentRequest.validateMoveReview(req)

    assertEquals(result.left.toOption.map(_.code), Some("move_review_too_early"))
    val error = result.left.toOption.getOrElse(fail("expected move_review_too_early"))
    assert(error.message.contains("move 3"), clues(error.message))
    assert(error.message.contains("Move Review"), clues(error.message))
    assert(!error.message.toLowerCase.contains("ply"), clues(error.message))
  }

  test("move review accepts positions from ply five onward") {
    val req =
      CommentRequest(
        fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3",
        lastMove = Some("b8c6"),
        eval = None,
        context = PositionContext(None, "opening", 5)
      )

    val result = CommentRequest.validateMoveReview(req)

    assert(result.isRight, clues(result))
  }
