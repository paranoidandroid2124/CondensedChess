package lila.commentary.chess

class TrapProofTest extends munit.FunSuite:

  test("TrapProof proves one defended minor has no safe target-piece escape"):
    val proof = TrapProof.fromBoardFacts(board(positiveFen), trapMove)

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.attackingSide, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.targetPieceAfter.map(_.man), Some(Man.Knight))
    assertEquals(proof.targetPieceSquareAfter, Some(Square('a', 8)))
    assertEquals(proof.anchorSquareAfter, Some(Square('a', 7)))
    assertEquals(proof.targetLegalMovesAfter.map(_.to).toSet, Set(Square('b', 6), Square('c', 7)))
    assertEquals(proof.safeEscapeMovesAfter, Vector.empty)
    assertEquals(proof.noEngineEvidenceUsed, true)

  test("TrapProof rejects safe escape square"):
    assertMissing(board(safeEscapeFen), trapMove, "no safe target-piece escape")

  test("TrapProof rejects undefended target so Loose keeps its boundary"):
    assertMissing(board(undefendedFen), trapMove, "target defended by rival side")

  test("TrapProof rejects queen rook pawn and king targets"):
    Vector(
      "queen" -> (queenTargetFen, trapMove),
      "rook" -> (rookTargetFen, trapMove),
      "pawn" -> (pawnTargetFen, pawnTargetMove),
      "king" -> (kingTargetFen, kingTargetMove)
    ).foreach: (label, input) =>
      val (fen, move) = input
      assertMissing(board(fen), move, "target is rival knight or bishop", label)

  test("TrapProof rejects capture checking incomplete StoryProof and illegal replay"):
    assertMissing(board(positiveFen), captureRoute, "non-capturing route", "capture route")
    assertMissing(board(checkingRouteFen), trapMove, "route does not give check", "checking route")
    assertMissing(untrustedBoard(positiveFen), trapMove, "complete StoryProof", "incomplete StoryProof")
    assertMissing(board(positiveFen), illegalMove, "legal move identity", "illegal replay")

  private def assertMissing(facts: BoardFacts, move: Line, expected: String, clue: String = ""): Unit =
    val proof = TrapProof.fromBoardFacts(facts, move)
    val missing = proof.missingEvidence.flatMap(_.missing)
    assertEquals(proof.complete, false, clue)
    assert(missing.contains(expected), s"$clue expected '$expected' in ${missing.mkString(", ")}")

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid TrapProof FEN: $fen -> $error"), identity)

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

  private val trapMove = Line(Square('a', 1), Square('a', 7))
  private val captureRoute = Line(Square('a', 1), Square('a', 8))
  private val pawnTargetMove = Line(Square('a', 1), Square('a', 6))
  private val kingTargetMove = Line(Square('a', 1), Square('e', 1))
  private val illegalMove = Line(Square('b', 1), Square('b', 2))

  private val positiveFen = "n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1"
  private val safeEscapeFen = "n3k3/8/2b5/8/8/8/8/R5K1 w - - 0 1"
  private val undefendedFen = "n3k3/8/8/8/8/8/5B2/R5K1 w - - 0 1"
  private val queenTargetFen = "q3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1"
  private val rookTargetFen = "r3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1"
  private val pawnTargetFen = "4k3/p7/8/2b5/8/8/5B2/R5K1 w - - 0 1"
  private val kingTargetFen = "4k3/8/8/8/8/8/5B2/R5K1 w - - 0 1"
  private val checkingRouteFen = "n7/4k3/2b5/8/8/8/5B2/R5K1 w - - 0 1"
