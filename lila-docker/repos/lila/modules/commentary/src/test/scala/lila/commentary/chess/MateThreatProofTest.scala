package lila.commentary.chess

class MateThreatProofTest extends ChessTestSupport:

  private val mateThreatFen = "7k/8/6K1/8/8/8/5Q2/8 w - - 0 1"
  private val threatMove = Line(Square('g', 6), Square('h', 6))
  private val nextMateMove = Line(Square('f', 2), Square('f', 8))
  private val quietMove = Line(Square('g', 6), Square('f', 6))
  private val illegalMove = Line(Square('g', 6), Square('g', 8))
  private val immediateMateMove = Line(Square('f', 2), Square('f', 8))
  private val checkButNotMateMove = Line(Square('f', 2), Square('f', 6))

  private def facts: BoardFacts =
    BoardFacts.fromFen(mateThreatFen).toOption.get

  private def missing(proof: MateThreatProof): String =
    proof.missingEvidence.flatMap(_.missing).mkString("\n")

  test("MateThreatProof binds the side move, exact after-board, and next legal CheckmateProof"):
    val proof = MateThreatProof.fromBoardFacts(facts, threatMove)

    assertEquals(proof.complete, true)
    assertEquals(proof.threateningSide, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.threatMove, Some(threatMove))
    assertEquals(proof.nextMateMove, Some(nextMateMove))
    assertEquals(proof.nextMateSan, Some("Qf8#"))
    assertEquals(proof.mateTargetKingSquare, Some(Square('h', 8)))
    assertEquals(proof.rivalKingSquareAfterThreatMove, Some(Square('h', 8)))
    assertEquals(proof.completeStoryProof, true)
    assertEquals(proof.storyProof.exists(_.complete), true)
    assertEquals(proof.afterBoard.exists(BoardFacts.sameBoardReady), true)
    assertEquals(proof.nextMateProof.exists(_.complete), true)
    assertEquals(proof.nextMateProof.flatMap(_.mateMove), Some(nextMateMove))
    assertEquals(proof.nextMateProof.map(_.matingSide), Some(Side.White))
    assertEquals(proof.nextMateProof.map(_.rivalSide), Some(Side.Black))
    assertEquals(proof.missingEvidence, Vector.empty)

  test("MateThreatProof rejects illegal, missing StoryProof, immediate mate, and no-mate continuations"):
    val illegalProof = MateThreatProof.fromBoardFacts(facts, illegalMove)
    assertEquals(illegalProof.complete, false)
    assertEquals(illegalProof.legalMove, false)

    val missingStoryProof = MateThreatProof.fromBoardFacts(facts, threatMove, storyProof = None)
    assertEquals(missingStoryProof.complete, false)
    assertEquals(missingStoryProof.completeStoryProof, false)
    assert(missing(missingStoryProof).contains("complete StoryProof"))

    val untrustedStoryProof =
      MateThreatProof.fromBoardFacts(facts, threatMove, storyProof = Some(StoryProof.untrustedLegalLine(threatMove)))
    assertEquals(untrustedStoryProof.complete, false)
    assertEquals(untrustedStoryProof.completeStoryProof, false)

    val immediateMateProof = MateThreatProof.fromBoardFacts(facts, immediateMateMove)
    assertEquals(immediateMateProof.complete, false)
    assertEquals(immediateMateProof.threatMoveIsNotImmediateCheckmate, false)
    assertEquals(CheckmateProof.fromBoardFacts(facts, immediateMateMove).complete, true)

    val noMateProof = MateThreatProof.fromBoardFacts(facts, quietMove)
    assertEquals(noMateProof.complete, false)
    assertEquals(noMateProof.nextSidePlyLegalCheckmateAvailable, false)

  test("MateThreatProof rejects illegal stale wrong-side and non-mating mate candidates"):
    val proof = MateThreatProof.fromBoardFacts(facts, threatMove)
    assertEquals(proof.complete, true)

    val illegalMateMove = proof.copy(nextMateMove = Some(illegalMove))
    assertEquals(illegalMateMove.complete, false)

    val checkButNotMate = proof.copy(
      nextMateMove = Some(checkButNotMateMove),
      nextMateProof = Some(CheckmateProof.fromBoardFacts(proof.afterBoard.get, checkButNotMateMove))
    )
    assertEquals(CheckmateProof.fromBoardFacts(proof.afterBoard.get, checkButNotMateMove).legalMove, true)
    assertEquals(CheckmateProof.fromBoardFacts(proof.afterBoard.get, checkButNotMateMove).afterBoardRivalKingInCheck, true)
    assertEquals(checkButNotMate.complete, false)

    val wrongSideMate = proof.copy(nextMateProof = proof.nextMateProof.map(_.copy(matingSide = Side.Black)))
    assertEquals(wrongSideMate.complete, false)

    val staleMateProof = proof.copy(
      nextMateProofBoard = Some(facts),
      nextMateProof = Some(CheckmateProof.fromBoardFacts(facts, immediateMateMove))
    )
    assertEquals(staleMateProof.nextMateProof.exists(_.complete), true)
    assertEquals(staleMateProof.complete, false)

    val wrongAfterBoard = proof.copy(afterBoard = Some(facts))
    assertEquals(wrongAfterBoard.complete, false)

  test("MateThreatProof ignores engine-shaped and SAN-only mate evidence"):
    val engineOnly = MateThreatProof.fromBoardFacts(facts, quietMove)
    assertEquals(engineOnly.complete, false)

    val sanOnlyFacts = minimalBoardFacts(
      sideLegal = Moves(known = true, lines = Vector(threatMove), san = Vector("Kh6"), moveCount = 1),
      rivalLegal = Moves(known = true, lines = Vector(nextMateMove), san = Vector("Qf8#"), moveCount = 1),
      pieces = Vector(
        Piece(Side.White, Man.King, Square('g', 6)),
        Piece(Side.White, Man.Queen, Square('f', 2)),
        Piece(Side.Black, Man.King, Square('h', 8))
      )
    )
    val sanOnly = MateThreatProof.fromBoardFacts(sanOnlyFacts, threatMove)
    assertEquals(sanOnly.complete, false)
    assertEquals(sanOnly.sameBoardProof, false)
    assertEquals(sanOnly.nextSidePlyLegalCheckmateAvailable, false)
