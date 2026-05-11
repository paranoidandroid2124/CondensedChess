package lila.commentary.chess

class CheckEscapedStage1Test extends munit.FunSuite:

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def proof(fen: String, line: Line): CheckEscapedProof =
    CheckEscapedProof.fromBoardFacts(facts(fen), line)

  test("Stage-1 CheckEscapedProof proves king move interposition capture and promotion escapes as one event"):
    val kingMove = proof(
      "k3r3/8/8/8/8/8/8/4K3 w - - 0 1",
      Line(Square('e', 1), Square('f', 1))
    )
    val interposition = proof(
      "k3r3/8/8/8/8/8/3B4/4K3 w - - 0 1",
      Line(Square('d', 2), Square('e', 3))
    )
    val checkingPieceCapture = proof(
      "k7/8/8/8/8/8/4r3/4K3 w - - 0 1",
      Line(Square('e', 1), Square('e', 2))
    )
    val promotionCounterCheck = proof(
      "k3r3/3P4/8/8/8/8/8/4K3 w - - 0 1",
      Line(Square('d', 7), Square('e', 8))
    )

    Vector(kingMove, interposition, checkingPieceCapture, promotionCounterCheck).foreach: row =>
      assertEquals(row.complete, true)
      assertEquals(row.escapingSide, Side.White)
      assertEquals(row.rivalSide, Side.Black)
      assertEquals(row.legalMove, true)
      assertEquals(row.sameBoardProof, true)
      assertEquals(row.exactBeforeBoardState, true)
      assertEquals(row.exactAfterBoardReplay, true)
      assertEquals(row.beforeBoardSideKingInCheck, true)
      assertEquals(row.afterBoardSideKingNotInCheck, true)
      assertEquals(row.checkEscapedByLegalMove, true)
      assertEquals(row.originSquare, row.escapeMove.map(_.from))
      assertEquals(row.destinationSquare, row.escapeMove.map(_.to))
      assert(row.movingPieceBefore.nonEmpty)
      assert(row.beforeKingSquare.nonEmpty)
      assert(row.afterKingSquare.nonEmpty)
      assertEquals(row.publicClaimAllowed, false)

  test("Stage-1 CheckEscapedProof stays incomplete outside exact legal check escape"):
    val quietBefore = proof(
      "k7/8/8/8/8/8/8/4K3 w - - 0 1",
      Line(Square('e', 1), Square('f', 1))
    )
    val illegalMove = proof(
      "k3r3/8/8/8/8/8/8/4K3 w - - 0 1",
      Line(Square('e', 1), Square('e', 2))
    )

    assertEquals(quietBefore.complete, false)
    assertEquals(quietBefore.legalMove, true)
    assertEquals(quietBefore.exactBeforeBoardState, true)
    assertEquals(quietBefore.beforeBoardSideKingInCheck, false)
    assertEquals(quietBefore.checkEscapedByLegalMove, false)
    assertEquals(quietBefore.publicClaimAllowed, false)

    assertEquals(illegalMove.complete, false)
    assertEquals(illegalMove.legalMove, false)
    assertEquals(illegalMove.exactBeforeBoardState, false)
    assertEquals(illegalMove.exactAfterBoardReplay, false)
    assertEquals(illegalMove.afterBoardSideKingNotInCheck, false)
    assertEquals(illegalMove.checkEscapedByLegalMove, false)
    assertEquals(illegalMove.publicClaimAllowed, false)
