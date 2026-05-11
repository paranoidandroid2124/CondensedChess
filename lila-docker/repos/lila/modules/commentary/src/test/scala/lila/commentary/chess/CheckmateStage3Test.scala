package lila.commentary.chess

class CheckmateStage3Test extends munit.FunSuite:

  private val mateFen = "k7/2Q5/2K5/8/8/8/8/8 w - - 0 1"
  private val mateMove = Line(Square('c', 7), Square('b', 7))
  private val checkButEscapeFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkButEscapeMove = Line(Square('d', 2), Square('e', 2))
  private val quietMove = Line(Square('d', 2), Square('a', 2))
  private val illegalMove = Line(Square('c', 7), Square('c', 1))
  private val stalemateFen = "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1"

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def mateFacts: BoardFacts =
    facts(mateFen)

  private def checkEscapeFacts: BoardFacts =
    facts(checkButEscapeFen)

  private def mateStory: Story =
    SceneCheckmate.write(mateFacts, mateMove).get

  private def assertBlocked(label: String, story: Story): Unit =
    val verdict = StoryTable.choose(Vector(story)).head
    assertEquals(verdict.role, Role.Blocked, label)
    assertEquals(verdict.leadAllowed, false, label)
    assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  private def withIncompleteCheckmateProof(story: Story, missing: String): Story =
    story.copy(
      checkmateProof = story.checkmateProof.map(
        _.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("CheckmateProof", Vector(missing))))
      )
    )

  test("Stage-3 CheckmateProof negative corpus requires exact legal same-board mate replay"):
    val checkButEscape = CheckmateProof.fromBoardFacts(checkEscapeFacts, checkButEscapeMove)
    val noCheck = CheckmateProof.fromBoardFacts(checkEscapeFacts, quietMove)
    val illegal = CheckmateProof.fromBoardFacts(mateFacts, illegalMove)
    val stalemate = facts(stalemateFen)
    val stalemateProof = CheckmateProof.fromBoardFacts(stalemate, Line(Square('h', 8), Square('h', 7)))
    val sanOnlyFacts =
      BoardFacts.untrusted(
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
    val sanOnly = CheckmateProof.fromBoardFacts(sanOnlyFacts, mateMove)

    assert(mateFacts.sideLegal.sanFor(mateMove).exists(_.contains("#")))
    assertEquals(SceneCheckmate.write(sanOnlyFacts, mateMove), None)

    assertEquals(checkButEscape.legalMove, true)
    assertEquals(checkButEscape.afterBoardRivalKingInCheck, true)
    assertEquals(checkButEscape.afterBoardRivalSideHasNoLegalEscape, false)
    assertEquals(checkButEscape.complete, false)

    assertEquals(noCheck.legalMove, true)
    assertEquals(noCheck.afterBoardRivalKingInCheck, false)
    assertEquals(noCheck.complete, false)

    assertEquals(illegal.legalMove, false)
    assertEquals(illegal.exactAfterBoardReplay, false)
    assertEquals(illegal.complete, false)

    assertEquals(stalemate.sideLegal.lines.isEmpty, true)
    assertEquals(BoardFacts.sideInCheck(stalemate, Side.Black), false)
    assertEquals(stalemateProof.afterBoardRivalKingInCheck, false)
    assertEquals(stalemateProof.afterBoardRivalSideHasNoLegalEscape, false)
    assertEquals(stalemateProof.complete, false)

    assertEquals(sanOnly.sameBoardProof, false)
    assertEquals(sanOnly.exactAfterBoardReplay, false)
    assertEquals(sanOnly.complete, false)

  test("Stage-3 SceneCheckmate blocks forged writerless contaminated and mismatched rows"):
    val story = mateStory
    val checkGivenProof = CheckGivenProof.fromBoardFacts(mateFacts, mateMove)
    val lineProof = LineProof.fromBoardFacts(mateFacts, Some(mateMove), None, None)

    assertBlocked("legal move missing", story.copy(route = None))
    assertBlocked("same-board proof missing", story.copy(storyProof = StoryProof.empty))
    assertBlocked("exact replay missing", story.copy(checkmateProof = story.checkmateProof.map(_.copy(exactAfterBoardReplay = false))))
    assertBlocked("rival king square missing", story.copy(target = None))
    assertBlocked(
      "proof rival king square missing",
      story.copy(checkmateProof = story.checkmateProof.map(_.copy(rivalKingSquareAfter = None)))
    )
    assertBlocked("route mismatch", story.copy(route = Some(Line(Square('c', 7), Square('b', 8)))))
    assertBlocked("side-to-move confusion", story.copy(side = Side.Black, rival = Side.White))
    assertBlocked("stale board proof", story.copy(storyProof = StoryProof.untrustedLegalLine(mateMove)))
    assertBlocked("incomplete StoryProof", story.copy(anchor = None))
    assertBlocked("incomplete CheckmateProof", withIncompleteCheckmateProof(story, "after-board rival king in check"))
    assertBlocked("writerless row", story.copy(writer = None))
    assertBlocked("contaminated tactic row", story.copy(tactic = Some(Tactic.SafeCheck)))
    assertBlocked("contaminated sidecar row", story.copy(lineProof = Some(lineProof)))
    assertBlocked(
      "CheckGiven-only proof",
      story.copy(checkmateProof = None, checkGivenProof = Some(checkGivenProof))
    )
    assertBlocked("BoardFacts-only check row", Story(Scene.Checkmate, side = Side.White, rival = Side.Black, proof = story.proof))

    assertEquals(SceneCheckmate.write(checkEscapeFacts, checkButEscapeMove), None)
    assertEquals(SceneCheckmate.write(checkEscapeFacts, quietMove), None)

  test("Stage-3 engine-only mate-looking evidence and downstream wording stay closed"):
    val engineOnly =
      EngineCheck.fromEvidence(
        sameBoardProof = true,
        checkedMove = Some(mateMove),
        engineLine = Some(EngineLine(Vector(mateMove))),
        replyLine = Some(EngineLine(Vector(mateMove))),
        evalBefore = Some(EngineEval(0)),
        evalAfter = Some(EngineEval(10000)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
    val rawPvMate =
      EngineCheck.fromEvidence(
        sameBoardProof = true,
        checkedMove = Some(mateMove),
        engineLine = Some(EngineLine(Vector(mateMove, Line(Square('a', 8), Square('a', 7))))),
        replyLine = Some(EngineLine(Vector(mateMove))),
        evalBefore = Some(EngineEval(0)),
        evalAfter = Some(EngineEval(10000)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    assertEquals(engineOnly.status, EngineCheckStatus.Supports)
    assertEquals(engineOnly.storyBound, false)
    assertEquals(engineOnly.publicClaimAllowed, false)
    assertEquals(rawPvMate.status, EngineCheckStatus.Supports)
    assertEquals(rawPvMate.storyBound, false)
    assertEquals(rawPvMate.publicClaimAllowed, false)
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty)

    val verdict = StoryTable.choose(Vector(mateStory)).head
    assertEquals(verdict.story.scene, Scene.Checkmate)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key), Some("checkmates"))
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("checkmates"))
