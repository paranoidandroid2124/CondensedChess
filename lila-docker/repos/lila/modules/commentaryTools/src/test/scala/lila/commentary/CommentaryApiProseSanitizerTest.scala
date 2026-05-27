package lila.commentary

class CommentaryApiProseSanitizerTest extends munit.FunSuite:

  test("does not rewrite opening-family prose after rendering") {
    val input =
      "This looks like an attempt at d5 Equalizer, but this stabilizing move is typical in Open Games (1.e4 e5), but the current pawn structure is different at this moment."
    val out = CommentaryApi.sanitizeMoveReviewProse(input)

    assert(out.contains("Open Games"))
    assert(!out.contains("opening-family claim"))
  }

  test("applies user-facing placeholder rewrites to moveReview fallback prose") {
    val input =
      """The direct alternative stays secondary because Piece Activation is deferred as PlayableByPV under strict evidence mode (supported by engine-coupled continuation (probe evidence pending)).
        |
        |Further probe work still targets Piece Activation [subplan:worst piece improvement] and still leans on {seed}.""".stripMargin
    val out = CommentaryApi.sanitizeMoveReviewProse(input)

    assert(!out.contains("PlayableByPV"), clues(out))
    assert(!out.contains("under strict evidence mode"), clues(out))
    assert(!out.contains("probe evidence pending"), clues(out))
    assert(!out.contains("[subplan:"), clues(out))
    assert(!out.contains("{seed}"), clues(out))
    assert(out.contains("current evidence threshold"), clues(out))
    assert(out.contains("current engine line"), clues(out))
    assert(out.contains("confirmation is still pending"), clues(out))
    assert(out.contains("intended pawn lever"), clues(out))
  }

  test("rewrites compensation jargon into club-player prose") {
    val input =
      "The return vector only holds if the initiative keeps generating line pressure, delayed recovery, and a clean cash out."
    val out = CommentaryApi.sanitizeMoveReviewProse(input)

    assert(!out.toLowerCase.contains("return vector"), clues(out))
    assert(!out.toLowerCase.contains("cash out"), clues(out))
    assert(!out.toLowerCase.contains("delayed recovery"), clues(out))
    assert(!out.toLowerCase.contains("line pressure"), clues(out))
    assert(out.toLowerCase.contains("pays off"), clues(out))
    assert(out.toLowerCase.contains("continuing pressure"), clues(out))
  }
