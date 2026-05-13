package lila.commentary.chess

class DecoyProofTest extends munit.FunSuite:

  test("DecoyProof proves a named rival piece is drawn onto a Trap follow-up square"):
    val proof = DecoyProof.fromBoardFacts(
      facts = board(positiveFen),
      sideMove = Some(sideMove),
      rivalReply = Some(decoyReply),
      namedPieceSquare = Some(namedPieceBeforeReply),
      decoySquare = Some(decoySquare),
      trapFollowUpProof = Some(completeTrapFollowUp)
    )

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.sideMove, Some(sideMove))
    assertEquals(proof.rivalReply, Some(decoyReply))
    assertEquals(proof.namedPieceBeforeReply.map(_.square), Some(namedPieceBeforeReply))
    assertEquals(proof.namedPieceAfterReply.map(_.square), Some(decoySquare))
    assertEquals(proof.decoySquare, Some(decoySquare))
    assertEquals(proof.landingSquare, Some(decoySquare))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalSideMove, true)
    assertEquals(proof.legalRivalReply, true)
    assertEquals(proof.replyByNamedPiece, true)
    assertEquals(proof.rivalPieceRemainsAfterReply, true)
    assertEquals(proof.replyLandsOnDecoySquare, true)
    assertEquals(proof.decoySquareEqualsLandingSquare, true)
    assertEquals(proof.completeTrapFollowUpProof, true)
    assertEquals(proof.trapFollowUpBindsSamePieceAndSquare, true)
    assertEquals(proof.completeStoryProof, true)
    assertEquals(proof.noEngineEvidenceUsed, true)
    assertEquals(proof.trapFollowUpProof.map(_.complete), Some(true))
    assertEquals(proof.missingEvidence, Vector.empty)

  test("DecoyProof rejects legal replay identity and Trap follow-up gaps"):
    val cases: Vector[(String, (BoardFacts, Option[Line], Option[Line], Option[Square], Option[Square], Option[TrapProof], String))] = Vector(
      "illegal side move" ->
        (board(positiveFen), Some(illegalSideMove), Some(decoyReply), Some(namedPieceBeforeReply), Some(decoySquare), Some(completeTrapFollowUp), "legal side move"),
      "illegal reply" ->
        (board(positiveFen), Some(sideMove), Some(illegalReply), Some(namedPieceBeforeReply), Some(decoySquare), Some(completeTrapFollowUp), "legal rival reply"),
      "wrong replying piece" ->
        (board(positiveFen), Some(sideMove), Some(wrongPieceReply), Some(namedPieceBeforeReply), Some(decoySquare), Some(completeTrapFollowUp), "reply by named rival piece"),
      "piece does not land on decoy square" ->
        (board(positiveFen), Some(sideMove), Some(wrongLandingReply), Some(namedPieceBeforeReply), Some(decoySquare), Some(completeTrapFollowUp), "reply lands on decoy square"),
      "no Trap follow-up proof" ->
        (board(positiveFen), Some(sideMove), Some(decoyReply), Some(namedPieceBeforeReply), Some(decoySquare), None, "complete Trap follow-up proof"),
      "Trap follow-up for different piece" ->
        (board(positiveFen), Some(sideMove), Some(decoyReply), Some(namedPieceBeforeReply), Some(decoySquare), Some(differentPieceTrapFollowUp), "Trap follow-up binds same piece and square"),
      "Trap follow-up for different square" ->
        (board(positiveFen), Some(sideMove), Some(decoyReply), Some(namedPieceBeforeReply), Some(decoySquare), Some(differentSquareTrapFollowUp), "Trap follow-up binds same piece and square"),
      "engine-only reply" ->
        (board(positiveFen), Some(sideMove), Some(engineOnlyReply), Some(namedPieceBeforeReply), Some(decoySquare), Some(completeTrapFollowUp), "legal rival reply"),
      "incomplete StoryProof" ->
        (untrustedBoard(positiveFen), Some(sideMove), Some(decoyReply), Some(namedPieceBeforeReply), Some(decoySquare), Some(completeTrapFollowUp), "complete StoryProof")
    )
    cases.foreach:
      case (label, (inputFacts, side, reply, namedPiece, targetSquare, followUp, expectedMissing)) =>
        assertMissing(inputFacts, side, reply, namedPiece, targetSquare, followUp, expectedMissing, label)

  private def assertMissing(
      facts: BoardFacts,
      sideMove: Option[Line],
      rivalReply: Option[Line],
      namedPieceSquare: Option[Square],
      decoySquare: Option[Square],
      trapFollowUpProof: Option[TrapProof],
      expected: String,
      clue: String
  ): Unit =
    val proof =
      DecoyProof.fromBoardFacts(facts, sideMove, rivalReply, namedPieceSquare, decoySquare, trapFollowUpProof)
    val missing = proof.missingEvidence.flatMap(_.missing)
    assertEquals(proof.complete, false, clue)
    assert(missing.contains(expected), s"$clue expected '$expected' in ${missing.mkString(", ")}")

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid DecoyProof FEN: $fen -> $error"), identity)

  private def untrustedBoard(fen: String): BoardFacts =
    val ready = board(fen)
    BoardFacts.untrusted(
      root = ready.root,
      sideToMove = ready.sideToMove,
      header = ready.header,
      sideLegal = ready.sideLegal,
      rivalLegal = ready.rivalLegal,
      control = ready.control,
      material = ready.material,
      pawns = ready.pawns,
      pieces = ready.pieces
    )

  private val positiveFen = "4k3/8/1nb5/8/8/8/8/R3B1K1 w - - 0 1"
  private val afterReplyTrapFen = "n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1"

  private val sideMove = Line(Square('e', 1), Square('f', 2))
  private val decoyReply = Line(Square('b', 6), Square('a', 8))
  private val trapMove = Line(Square('a', 1), Square('a', 7))
  private val illegalSideMove = Line(Square('e', 1), Square('e', 4))
  private val illegalReply = Line(Square('b', 6), Square('b', 5))
  private val wrongPieceReply = Line(Square('c', 6), Square('a', 8))
  private val wrongLandingReply = Line(Square('b', 6), Square('c', 4))
  private val engineOnlyReply = Line(Square('b', 6), Square('b', 8))
  private val namedPieceBeforeReply = Square('b', 6)
  private val decoySquare = Square('a', 8)

  private def completeTrapFollowUp: TrapProof =
    TrapProof.fromBoardFacts(board(afterReplyTrapFen), trapMove)

  private def differentPieceTrapFollowUp: TrapProof =
    completeTrapFollowUp.copy(targetPieceAfter = Some(Piece(Side.Black, Man.Bishop, decoySquare)))

  private def differentSquareTrapFollowUp: TrapProof =
    completeTrapFollowUp.copy(
      targetPieceAfter = Some(Piece(Side.Black, Man.Knight, Square('b', 8))),
      targetPieceSquareAfter = Some(Square('b', 8))
    )
