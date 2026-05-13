package lila.commentary.chess

class DeflectProofStage1Test extends munit.FunSuite:

  test("Stage-1 DeflectProof proves a named defender leaves one non-king duty after a legal reply"):
    val proof = DeflectProof.fromBoardFacts(
      board(positiveFen),
      Some(sideMove),
      Some(deflectingReply),
      Some(defender),
      Some(target)
    )

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.sideMove, Some(sideMove))
    assertEquals(proof.rivalReply, Some(deflectingReply))
    assertEquals(proof.defenderBeforeReply.map(_.square), Some(defender))
    assertEquals(proof.defenderAfterReply.map(_.square), Some(defenderAfterReply))
    assertEquals(proof.targetBeforeReply.map(_.square), Some(target))
    assertEquals(proof.targetAfterReply.map(_.square), Some(target))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalSideMove, true)
    assertEquals(proof.legalRivalReply, true)
    assertEquals(proof.replyByNamedDefender, true)
    assertEquals(proof.defenderGuardedTargetBeforeReply, true)
    assertEquals(proof.defenderNoLongerGuardsTargetAfterReply, true)
    assertEquals(proof.targetRemainsAfterReply, true)
    assertEquals(proof.defenderRemainsAfterReply, true)
    assertEquals(proof.sideMoveDoesNotCaptureDefender, true)
    assertEquals(proof.completeStoryProof, true)
    assertEquals(proof.noEngineEvidenceUsed, true)
    assertEquals(proof.missingEvidence, Vector.empty)

  test("Stage-1 DeflectProof rejects illegal replay identity gaps and non-deflections"):
    val facts = board(positiveFen)

    Vector(
      "illegal side move" ->
        (facts, Some(illegalSideMove), Some(deflectingReply), Some(defender), Some(target), "legal side move"),
      "illegal rival reply" ->
        (facts, Some(sideMove), Some(illegalReply), Some(defender), Some(target), "legal rival reply"),
      "wrong replying piece" ->
        (facts, Some(sideMove), Some(wrongPieceReply), Some(defender), Some(target), "reply by named defender"),
      "defender still defends target" ->
        (board(stillDefendsFen), Some(sideMove), Some(stillDefendsReply), Some(stillDefendsDefender), Some(stillDefendsTarget), "defender no longer guards target after reply"),
      "target gone" ->
        (facts, Some(sideMove), Some(wrongPieceReply), Some(defender), Some(target), "target remains after reply"),
      "defender gone" ->
        (board(defenderGoneFen), Some(defenderGoneSideMove), Some(defenderGoneReply), Some(defenderGoneDefender), Some(defenderGoneTarget), "defender remains after reply"),
      "defender captured by side move" ->
        (board(defenderCapturedFen), Some(defenderCapturedSideMove), Some(defenderCapturedReply), Some(defenderCapturedDefender), Some(defenderCapturedTarget), "side move does not capture defender"),
      "king target" ->
        (board(kingTargetFen), Some(sideMove), Some(deflectingReply), Some(defender), Some(kingTarget), "target non-king material"),
      "engine-only reply" ->
        (facts, Some(sideMove), Some(engineOnlyReply), Some(defender), Some(target), "legal rival reply"),
      "incomplete StoryProof" ->
        (untrustedBoard(positiveFen), Some(sideMove), Some(deflectingReply), Some(defender), Some(target), "complete StoryProof")
    ).foreach:
      case (label, (inputFacts, side, reply, defenderSquare, targetSquare, expectedMissing)) =>
        assertMissing(inputFacts, side, reply, defenderSquare, targetSquare, expectedMissing, label)

  private def assertMissing(
      facts: BoardFacts,
      sideMove: Option[Line],
      rivalReply: Option[Line],
      defenderSquare: Option[Square],
      targetSquare: Option[Square],
      expected: String,
      clue: String
  ): Unit =
    val proof = DeflectProof.fromBoardFacts(facts, sideMove, rivalReply, defenderSquare, targetSquare)
    val missing = proof.missingEvidence.flatMap(_.missing)
    assertEquals(proof.complete, false, clue)
    assert(missing.contains(expected), s"$clue expected '$expected' in ${missing.mkString(", ")}")

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid DeflectProof FEN: $fen -> $error"), identity)

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

  private val positiveFen = "7k/8/4b3/3n4/8/8/7P/7K w - - 0 1"
  private val sideMove = Line(Square('h', 2), Square('h', 3))
  private val deflectingReply = Line(Square('e', 6), Square('g', 4))
  private val wrongPieceReply = Line(Square('d', 5), Square('f', 4))
  private val illegalSideMove = Line(Square('h', 2), Square('h', 5))
  private val illegalReply = Line(Square('e', 6), Square('e', 5))
  private val engineOnlyReply = Line(Square('e', 6), Square('b', 3))
  private val defender = Square('e', 6)
  private val defenderAfterReply = Square('g', 4)
  private val target = Square('d', 5)

  private val stillDefendsFen = "7k/4n3/4r3/8/8/8/7P/7K w - - 0 1"
  private val stillDefendsReply = Line(Square('e', 6), Square('e', 5))
  private val stillDefendsDefender = Square('e', 6)
  private val stillDefendsTarget = Square('e', 7)

  private val defenderGoneFen = "7k/8/8/8/8/8/1p5P/2b4K w - - 0 1"
  private val defenderGoneSideMove = Line(Square('h', 2), Square('h', 3))
  private val defenderGoneReply = Line(Square('b', 2), Square('b', 1))
  private val defenderGoneDefender = Square('b', 2)
  private val defenderGoneTarget = Square('c', 1)

  private val defenderCapturedFen = "7k/8/4b3/3n4/8/8/8/4R2K w - - 0 1"
  private val defenderCapturedSideMove = Line(Square('e', 1), Square('e', 6))
  private val defenderCapturedReply = Line(Square('d', 5), Square('f', 4))
  private val defenderCapturedDefender = Square('e', 6)
  private val defenderCapturedTarget = Square('d', 5)

  private val kingTargetFen = "8/8/4b3/3k4/8/8/7P/7K w - - 0 1"
  private val kingTarget = Square('d', 5)
