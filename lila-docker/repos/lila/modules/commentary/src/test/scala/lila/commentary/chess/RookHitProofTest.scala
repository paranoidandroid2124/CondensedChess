package lila.commentary.chess

class RookHitProofTest extends munit.FunSuite:

  private val rookHitFen = "4k3/8/8/7r/8/8/3R4/4K3 w - - 0 1"
  private val queenTargetFen = "4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1"
  private val bishopTargetFen = "4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"
  private val knightTargetFen = "4k3/8/8/7n/8/8/3R4/4K3 w - - 0 1"
  private val pawnTargetFen = "4k3/8/8/7p/8/8/3R4/4K3 w - - 0 1"
  private val kingTargetFen = "8/8/8/7k/8/8/3R4/4K3 w - - 0 1"
  private val ownRookTargetFen = "4k3/8/8/7R/8/8/3R4/4K3 w - - 0 1"
  private val noRookFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val attackedOnlyBeforeFen = "4k3/8/8/7r/8/8/7R/4K3 w - - 0 1"
  private val rookHitMove = Line(Square('d', 2), Square('h', 2))
  private val attackedOnlyBeforeMove = Line(Square('h', 2), Square('a', 2))
  private val illegalMove = Line(Square('d', 2), Square('h', 8))
  private val targetSquare = Square('h', 5)

  test("RookHitProof proves only exact legal same-board after-board rival rook contact"):
    val facts = board(rookHitFen)
    val proof = RookHitProof.fromBoardFacts(facts, rookHitMove, Some(targetSquare))

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.attackingSide, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.attackMove, Some(rookHitMove))
    assertEquals(proof.originSquare, Some(Square('d', 2)))
    assertEquals(proof.destinationSquare, Some(Square('h', 2)))
    assertEquals(proof.targetSquare, Some(targetSquare))
    assertEquals(proof.targetPieceAfter.map(_.man), Some(Man.Rook))
    assertEquals(proof.targetPieceAfter.map(_.side), Some(Side.Black))
    assertEquals(proof.attackingPieceSquareAfter, Some(Square('h', 2)))
    assertEquals(proof.legalMove, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.completeStoryProof, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.targetSquareBound, true)
    assertEquals(proof.targetPieceExistsAfter, true)
    assertEquals(proof.targetPieceIsRivalRook, true)
    assertEquals(proof.afterBoardRookAttackedByMovingSide, true)
    assertEquals(proof.routeAndTargetBindSameBoard, true)
    assertEquals(proof.missingEvidence, Vector.empty)

  test("RookHitProof rejects illegal empty own-rook and non-rook target cases"):
    val facts = board(rookHitFen)

    val illegalProof = RookHitProof.fromBoardFacts(facts, illegalMove, Some(targetSquare))
    assertEquals(illegalProof.complete, false)
    assertEquals(illegalProof.legalMove, false)
    assert(illegalProof.missingEvidence.exists(_.missing.contains("legal move identity")))

    val emptyProof = RookHitProof.fromBoardFacts(board(noRookFen), rookHitMove, Some(targetSquare))
    assertEquals(emptyProof.complete, false)
    assertEquals(emptyProof.targetPieceExistsAfter, false)
    assert(emptyProof.missingEvidence.exists(_.missing.contains("target piece on after-board")))

    val ownRookProof = RookHitProof.fromBoardFacts(board(ownRookTargetFen), rookHitMove, Some(targetSquare))
    assertEquals(ownRookProof.complete, false)
    assertEquals(ownRookProof.targetPieceIsRivalRook, false)

    Vector(
      "queen" -> queenTargetFen,
      "bishop" -> bishopTargetFen,
      "knight" -> knightTargetFen,
      "pawn" -> pawnTargetFen,
      "king" -> kingTargetFen
    ).foreach: (label, fen) =>
      val proof = RookHitProof.fromBoardFacts(board(fen), rookHitMove, Some(targetSquare))
      assertEquals(proof.complete, false, label)
      assertEquals(proof.targetPieceIsRivalRook, false, label)

  test("RookHitProof rejects missing target stale board SAN-only EngineCheck-only and incomplete StoryProof"):
    val facts = board(rookHitFen)
    val missingTarget = RookHitProof.fromBoardFacts(facts, rookHitMove, None)
    assertEquals(missingTarget.complete, false)
    assertEquals(missingTarget.targetSquareBound, false)
    assert(missingTarget.missingEvidence.exists(_.missing.contains("target square bound")))

    val untrustedFacts = BoardFacts.untrusted(
      root = facts.root,
      sideToMove = facts.sideToMove,
      header = facts.header,
      sideLegal = facts.sideLegal,
      rivalLegal = facts.rivalLegal,
      control = facts.control,
      material = facts.material,
      pawns = facts.pawns,
      pieces = facts.pieces
    )
    val staleProof = RookHitProof.fromBoardFacts(untrustedFacts, rookHitMove, Some(targetSquare))
    assertEquals(staleProof.complete, false)
    assertEquals(staleProof.sameBoardProof, false)
    assert(staleProof.missingEvidence.exists(_.missing.contains("same-board proof")))

    val incompleteStoryProof =
      RookHitProof.fromBoardFacts(facts, rookHitMove, Some(targetSquare), storyProof = StoryProof.empty)
    assertEquals(incompleteStoryProof.complete, false)
    assertEquals(incompleteStoryProof.completeStoryProof, false)
    assert(incompleteStoryProof.missingEvidence.exists(_.missing.contains("complete StoryProof")))

    assertEquals(BoardFacts.sanFor(facts, rookHitMove), Some("Rh2"))
    assertEquals(RookHitProof.getClass.getDeclaredMethods.exists(_.getParameterTypes.exists(_ == classOf[String])), false)
    assertEquals(RookHitProof.getClass.getDeclaredMethods.exists(_.getParameterTypes.exists(_ == classOf[EngineCheck])), false)

  test("RookHitProof rejects attacks that exist only before the move"):
    val facts = board(attackedOnlyBeforeFen)
    val proof = RookHitProof.fromBoardFacts(facts, attackedOnlyBeforeMove, Some(targetSquare))

    assertEquals(proof.complete, false)
    assertEquals(proof.targetAttackedBeforeMove, true)
    assertEquals(proof.afterBoardRookAttackedByMovingSide, false)
    assert(proof.missingEvidence.exists(_.missing.contains("after-board rook attack")))

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid RookHit FEN: $fen -> $error"), identity)
