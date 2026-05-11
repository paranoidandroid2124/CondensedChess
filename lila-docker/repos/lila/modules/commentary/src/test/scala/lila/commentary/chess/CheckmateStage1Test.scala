package lila.commentary.chess

class CheckmateStage1Test extends munit.FunSuite:

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def proof(fen: String, line: Line): CheckmateProof =
    CheckmateProof.fromBoardFacts(facts(fen), line)

  test("Stage-1 CheckmateProof proves only exact legal after-board checkmate events"):
    val ordinary = proof(
      "k7/2Q5/2K5/8/8/8/8/8 w - - 0 1",
      Line(Square('c', 7), Square('b', 7))
    )
    val promotionMate = proof(
      "7k/5P2/5KP1/8/8/8/8/8 w - - 0 1",
      Line(Square('f', 7), Square('f', 8))
    )

    Vector("ordinary" -> ordinary, "promotion" -> promotionMate).foreach: (label, row) =>
      if !row.complete then fail(s"$label ${row.missingEvidence}")
      assertEquals(row.complete, true)
      assertEquals(row.matingSide, Side.White)
      assertEquals(row.rivalSide, Side.Black)
      assertEquals(row.legalMove, true)
      assertEquals(row.sameBoardProof, true)
      assertEquals(row.exactAfterBoardReplay, true)
      assertEquals(row.afterBoardRivalKingInCheck, true)
      assertEquals(row.afterBoardRivalSideHasNoLegalEscape, true)
      assertEquals(row.checkmateProducedByLegalMove, true)
      assertEquals(row.originSquare, row.mateMove.map(_.from))
      assertEquals(row.destinationSquare, row.mateMove.map(_.to))
      assert(row.movingPieceBefore.nonEmpty)
      assert(row.rivalKingSquareAfter.nonEmpty)
      assertEquals(row.publicClaimAllowed, false)

  test("Stage-1 CheckmateProof stays incomplete for non-mate check legal escape illegal SAN-only and untrusted proof"):
    val checkButEscape = proof(
      "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1",
      Line(Square('d', 2), Square('e', 2))
    )
    val quiet = proof(
      "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1",
      Line(Square('d', 2), Square('a', 2))
    )
    val illegal = proof(
      "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1",
      Line(Square('d', 2), Square('e', 8))
    )

    assertEquals(checkButEscape.complete, false)
    assertEquals(checkButEscape.legalMove, true)
    assertEquals(checkButEscape.afterBoardRivalKingInCheck, true)
    assertEquals(checkButEscape.afterBoardRivalSideHasNoLegalEscape, false)
    assertEquals(checkButEscape.checkmateProducedByLegalMove, false)
    assertEquals(checkButEscape.publicClaimAllowed, false)

    assertEquals(quiet.complete, false)
    assertEquals(quiet.legalMove, true)
    assertEquals(quiet.afterBoardRivalKingInCheck, false)
    assertEquals(quiet.afterBoardRivalSideHasNoLegalEscape, false)
    assertEquals(quiet.checkmateProducedByLegalMove, false)
    assertEquals(quiet.publicClaimAllowed, false)

    assertEquals(illegal.complete, false)
    assertEquals(illegal.legalMove, false)
    assertEquals(illegal.exactAfterBoardReplay, false)
    assertEquals(illegal.afterBoardRivalKingInCheck, false)
    assertEquals(illegal.afterBoardRivalSideHasNoLegalEscape, false)
    assertEquals(illegal.checkmateProducedByLegalMove, false)
    assertEquals(illegal.publicClaimAllowed, false)

    val mateFacts = facts("k7/2Q5/2K5/8/8/8/8/8 w - - 0 1")
    val mateMove = Line(Square('c', 7), Square('b', 7))
    assert(mateFacts.sideLegal.sanFor(mateMove).exists(_.contains("#")))

    val untrustedFacts = BoardFacts.untrusted(
      root = mateFacts.root,
      sideToMove = mateFacts.sideToMove,
      header = mateFacts.header,
      sideLegal = mateFacts.sideLegal,
      rivalLegal = mateFacts.rivalLegal,
      control = mateFacts.control,
      material = mateFacts.material,
      pawns = mateFacts.pawns,
      pieces = mateFacts.pieces
    )
    val untrustedProof = CheckmateProof.fromBoardFacts(untrustedFacts, mateMove)
    assertEquals(untrustedProof.complete, false)
    assertEquals(untrustedProof.sameBoardProof, false)
    assertEquals(untrustedProof.checkmateProducedByLegalMove, false)
