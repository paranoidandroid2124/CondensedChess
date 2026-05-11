package lila.commentary.chess

class CheckGivenStage1Test extends munit.FunSuite:

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def proof(fen: String, line: Line): CheckGivenProof =
    CheckGivenProof.fromBoardFacts(facts(fen), line)

  test("Stage-1 CheckGivenProof proves ordinary discovered double and promotion checks as one event"):
    val ordinary = proof(
      "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1",
      Line(Square('d', 2), Square('e', 2))
    )
    val discovered = proof(
      "4k3/8/8/8/8/8/4B3/K3R3 w - - 0 1",
      Line(Square('e', 2), Square('g', 4))
    )
    val doubleCheck = proof(
      "4k3/8/8/8/8/8/4B3/K3R3 w - - 0 1",
      Line(Square('e', 2), Square('b', 5))
    )
    val promotion = proof(
      "7k/1P6/8/8/8/8/8/7K w - - 0 1",
      Line(Square('b', 7), Square('b', 8))
    )

    Vector(ordinary, discovered, doubleCheck, promotion).foreach: row =>
      assertEquals(row.complete, true)
      assertEquals(row.checkingSide, Side.White)
      assertEquals(row.rivalSide, Side.Black)
      assertEquals(row.legalMove, true)
      assertEquals(row.sameBoardProof, true)
      assertEquals(row.exactAfterBoardReplay, true)
      assertEquals(row.afterBoardRivalKingInCheck, true)
      assertEquals(row.checkProducedByLegalMove, true)
      assertEquals(row.originSquare, row.checkMove.map(_.from))
      assertEquals(row.destinationSquare, row.checkMove.map(_.to))
      assert(row.movingPieceBefore.nonEmpty)
      assert(row.rivalKingSquareAfter.nonEmpty)
      assertEquals(row.publicClaimAllowed, false)

  test("Stage-1 CheckGivenProof stays incomplete when the legal move does not produce check"):
    val quiet = proof(
      "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1",
      Line(Square('d', 2), Square('a', 2))
    )

    assertEquals(quiet.complete, false)
    assertEquals(quiet.legalMove, true)
    assertEquals(quiet.sameBoardProof, true)
    assertEquals(quiet.exactAfterBoardReplay, true)
    assertEquals(quiet.afterBoardRivalKingInCheck, false)
    assertEquals(quiet.checkProducedByLegalMove, false)
    assertEquals(quiet.publicClaimAllowed, false)
