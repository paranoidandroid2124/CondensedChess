package lila.commentary.chess

class InterferenceProofTest extends munit.FunSuite:

  test("InterferenceProof proves a non-capturing move blocks a slider defender line"):
    val row = proof(ready(positiveFen), sideMove, defender, blockingSquare, target)

    assertEquals(row.complete, true)
    assertEquals(row.publicClaimAllowed, false)
    assertEquals(row.side, Side.White)
    assertEquals(row.rivalSide, Side.Black)
    assertEquals(row.sideMove, Some(sideMove))
    assertEquals(row.lineDefenderBefore.map(_.square), Some(defender))
    assertEquals(row.lineDefenderAfter.map(_.square), Some(defender))
    assertEquals(row.targetSquare, Some(target))
    assertEquals(row.targetPieceBefore.map(_.square), Some(target))
    assertEquals(row.blockingSquare, Some(blockingSquare))
    assertEquals(row.movedPieceBefore.map(_.square), Some(Square('b', 3)))
    assertEquals(row.movedPieceAfter.map(_.square), Some(blockingSquare))
    assertEquals(row.sameBoardProof, true)
    assertEquals(row.legalSideMove, true)
    assertEquals(row.completeStoryProof, true)
    assertEquals(row.moveIsNonCapture, true)
    assertEquals(row.movedPieceLandsOnBlockingSquare, true)
    assertEquals(row.lineDefenderIsSlider, true)
    assertEquals(row.targetBound, true)
    assertEquals(row.defenderBlockingTargetCollinearOnSliderRay, true)
    assertEquals(row.blockingSquareStrictlyBetweenDefenderAndTarget, true)
    assertEquals(row.defenderLineContactBeforeMove, true)
    assertEquals(row.afterMoveBlockingSquareOccupiedByMovedPiece, true)
    assertEquals(row.defenderLineContactRemovedByBlocker, true)
    assertEquals(row.noEngineEvidenceUsed, true)
    assertEquals(row.missingEvidence, Vector.empty)

  test("InterferenceProof also accepts an explicitly defended empty target square"):
    val row = proof(ready(positiveFen), sideMove, defender, blockingSquare, emptyTarget)

    assertEquals(row.complete, true)
    assertEquals(row.targetSquare, Some(emptyTarget))
    assertEquals(row.targetPieceBefore, None)
    assertEquals(row.targetBound, true)
    assertEquals(row.defenderLineContactBeforeMove, true)
    assertEquals(row.defenderLineContactRemovedByBlocker, true)

  test("InterferenceProof rejects non-line-blocking evidence"):
    val facts = ready(positiveFen)

    Vector(
      "illegal move" ->
        (facts, illegalMove, defender, blockingSquare, target, "legal side move"),
      "capture move" ->
        (ready(captureFen), captureMove, defender, blockingSquare, target, "non-capture move"),
      "non-slider defender" ->
        (ready(nonSliderFen), sideMove, defender, blockingSquare, target, "line defender bishop rook or queen"),
      "blocker not between defender and target" ->
        (facts, notBetweenMove, defender, Square('c', 2), target, "blocking square strictly between defender and target"),
      "defender and target not collinear" ->
        (facts, sideMove, defender, blockingSquare, wrongTarget, "defender blocking square and target collinear on legal slider ray"),
      "line already blocked before the move" ->
        (ready(alreadyBlockedFen), sideMove, defender, blockingSquare, target, "defender line contact to target before move"),
      "target not defended before the move" ->
        (ready(targetNotDefendedFen), sideMove, defender, blockingSquare, target, "defender line contact to target before move"),
      "defender still defends target after the move" ->
        (facts, notBetweenMove, defender, Square('c', 2), target, "defender line contact removed by blocking square"),
      "wrong defender" ->
        (facts, sideMove, Square('h', 8), blockingSquare, target, "line defender bishop rook or queen"),
      "wrong target" ->
        (facts, sideMove, defender, blockingSquare, wrongTarget, "defender blocking square and target collinear on legal slider ray"),
      "wrong blocking square" ->
        (facts, sideMove, defender, Square('a', 5), target, "moved piece lands on blocking square"),
      "incomplete StoryProof" ->
        (untrusted(positiveFen), sideMove, defender, blockingSquare, target, "complete StoryProof"),
      "engine-only evidence" ->
        (facts, engineOnlyMove, defender, blockingSquare, target, "legal side move")
    ).foreach:
      case (label, (inputFacts, move, defenderSquare, blockSquare, targetSquare, expectedMissing)) =>
        assertMissing(inputFacts, move, defenderSquare, blockSquare, targetSquare, expectedMissing, label)

  private def proof(
      facts: BoardFacts,
      move: Line,
      defenderSquare: Square,
      blockSquare: Square,
      targetSquare: Square
  ): InterferenceProof =
    InterferenceProof.fromBoardFacts(facts, Some(move), Some(defenderSquare), Some(blockSquare), Some(targetSquare))

  private def assertMissing(
      facts: BoardFacts,
      move: Line,
      defenderSquare: Square,
      blockSquare: Square,
      targetSquare: Square,
      expected: String,
      clue: String
  ): Unit =
    val row = proof(facts, move, defenderSquare, blockSquare, targetSquare)
    val missing = row.missingEvidence.flatMap(_.missing)
    assertEquals(row.complete, false, clue)
    assert(missing.contains(expected), s"$clue expected '$expected' in ${missing.mkString(", ")}")
    assertEquals(row.publicClaimAllowed, false, clue)

  private def ready(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid InterferenceProof FEN: $fen -> $error"), identity)

  private def untrusted(fen: String): BoardFacts =
    val board = ready(fen)
    BoardFacts.untrusted(
      root = board.root,
      sideToMove = board.sideToMove,
      header = board.header,
      sideLegal = board.sideLegal,
      rivalLegal = board.rivalLegal,
      control = board.control,
      material = board.material,
      pawns = board.pawns,
      pieces = board.pieces
    )

  private val positiveFen = "r6k/8/8/8/8/1B6/8/n6K w - - 0 1"
  private val captureFen = "r6k/8/8/8/p7/1B6/8/n6K w - - 0 1"
  private val nonSliderFen = "n6k/8/8/8/8/1B6/8/n6K w - - 0 1"
  private val alreadyBlockedFen = "r6k/8/8/p7/8/1B6/8/n6K w - - 0 1"
  private val targetNotDefendedFen = "r6k/8/8/8/8/1B6/8/N6K w - - 0 1"

  private val sideMove = Line(Square('b', 3), Square('a', 4))
  private val captureMove = Line(Square('b', 3), Square('a', 4))
  private val illegalMove = Line(Square('b', 3), Square('b', 4))
  private val notBetweenMove = Line(Square('b', 3), Square('c', 2))
  private val engineOnlyMove = Line(Square('a', 4), Square('a', 5))
  private val defender = Square('a', 8)
  private val blockingSquare = Square('a', 4)
  private val target = Square('a', 1)
  private val emptyTarget = Square('a', 2)
  private val wrongTarget = Square('b', 1)
