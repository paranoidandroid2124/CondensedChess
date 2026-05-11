package lila.commentary.chess

class StalemateStage1Test extends munit.FunSuite:

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def proof(fen: String, line: Line): StalemateProof =
    StalemateProof.fromBoardFacts(facts(fen), line)

  test("Stalemate-1 StalemateProof proves only exact legal after-board stalemate events"):
    val row = proof(
      "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1",
      Line(Square('g', 5), Square('g', 6))
    )

    if !row.complete then fail(row.missingEvidence.toString)
    assertEquals(row.complete, true)
    assertEquals(row.stalematingSide, Side.White)
    assertEquals(row.rivalSide, Side.Black)
    assertEquals(row.legalMove, true)
    assertEquals(row.sameBoardProof, true)
    assertEquals(row.exactAfterBoardReplay, true)
    assertEquals(row.afterBoardRivalSideNotInCheck, true)
    assertEquals(row.afterBoardRivalSideHasNoLegalMoves, true)
    assertEquals(row.stalemateProducedByLegalMove, true)
    assertEquals(row.originSquare, row.stalemateMove.map(_.from))
    assertEquals(row.destinationSquare, row.stalemateMove.map(_.to))
    assert(row.movingPieceBefore.nonEmpty)
    assert(row.movingPieceAfter.nonEmpty)
    assert(row.rivalKingSquareAfter.nonEmpty)
    assertEquals(row.publicClaimAllowed, false)

  test("Stalemate-1 StalemateProof rejects checkmate legal-move-with-replies illegal and untrusted rows"):
    val checkmate = proof(
      "k7/2Q5/2K5/8/8/8/8/8 w - - 0 1",
      Line(Square('c', 7), Square('b', 7))
    )
    val withReplies = proof(
      "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1",
      Line(Square('d', 2), Square('a', 2))
    )
    val illegal = proof(
      "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1",
      Line(Square('g', 5), Square('h', 8))
    )

    assertEquals(checkmate.complete, false)
    assertEquals(checkmate.legalMove, true)
    assertEquals(checkmate.afterBoardRivalSideNotInCheck, false)
    assertEquals(checkmate.afterBoardRivalSideHasNoLegalMoves, true)
    assertEquals(checkmate.stalemateProducedByLegalMove, false)
    assertEquals(checkmate.publicClaimAllowed, false)

    assertEquals(withReplies.complete, false)
    assertEquals(withReplies.legalMove, true)
    assertEquals(withReplies.afterBoardRivalSideNotInCheck, true)
    assertEquals(withReplies.afterBoardRivalSideHasNoLegalMoves, false)
    assertEquals(withReplies.stalemateProducedByLegalMove, false)
    assertEquals(withReplies.publicClaimAllowed, false)

    assertEquals(illegal.complete, false)
    assertEquals(illegal.legalMove, false)
    assertEquals(illegal.exactAfterBoardReplay, false)
    assertEquals(illegal.afterBoardRivalSideNotInCheck, false)
    assertEquals(illegal.afterBoardRivalSideHasNoLegalMoves, false)
    assertEquals(illegal.stalemateProducedByLegalMove, false)
    assertEquals(illegal.publicClaimAllowed, false)

    val stalemateFacts = facts("7k/5K2/8/6Q1/8/8/8/8 w - - 0 1")
    val stalemateMove = Line(Square('g', 5), Square('g', 6))
    val untrustedFacts = BoardFacts.untrusted(
      root = stalemateFacts.root,
      sideToMove = stalemateFacts.sideToMove,
      header = stalemateFacts.header,
      sideLegal = stalemateFacts.sideLegal,
      rivalLegal = stalemateFacts.rivalLegal,
      control = stalemateFacts.control,
      material = stalemateFacts.material,
      pawns = stalemateFacts.pawns,
      pieces = stalemateFacts.pieces
    )
    val untrustedProof = StalemateProof.fromBoardFacts(untrustedFacts, stalemateMove)
    assertEquals(untrustedProof.complete, false)
    assertEquals(untrustedProof.sameBoardProof, false)
    assertEquals(untrustedProof.stalemateProducedByLegalMove, false)

  test("Stalemate-1 StalemateProof alone opens no downstream speech path"):
    assertEquals(
      proof("7k/5K2/8/6Q1/8/8/8/8 w - - 0 1", Line(Square('g', 5), Square('g', 6))).publicClaimAllowed,
      false
    )
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty)
