package lila.commentary.chess

class CheckGivenStage3Test extends munit.FunSuite:

  private val checkingFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkingMove = Line(Square('d', 2), Square('e', 2))
  private val quietMove = Line(Square('d', 2), Square('a', 2))
  private val illegalMove = Line(Square('d', 2), Square('e', 8))
  private val replyMove = Line(Square('e', 8), Square('f', 8))

  private def facts: BoardFacts =
    BoardFacts.fromFen(checkingFen).toOption.get

  private def story: Story =
    SceneCheckGiven.write(facts, checkingMove).get

  private def assertSilent(row: Story): Unit =
    val verdict = StoryTable.choose(Vector(row)).head
    assertEquals(verdict.role, Role.Blocked)
    assertEquals(verdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("Stage-3 CheckGiven stays silent for missing legal replay and after-board no-check states"):
    val boardFacts = facts

    val quietProof = CheckGivenProof.fromBoardFacts(boardFacts, quietMove)
    assertEquals(quietProof.complete, false)
    assertEquals(quietProof.legalMove, true)
    assertEquals(quietProof.afterBoardRivalKingInCheck, false)
    assertEquals(quietProof.checkProducedByLegalMove, false)
    assertEquals(SceneCheckGiven.write(boardFacts, quietMove), None)

    val illegalProof = CheckGivenProof.fromBoardFacts(boardFacts, illegalMove)
    assertEquals(illegalProof.complete, false)
    assertEquals(illegalProof.legalMove, false)
    assertEquals(illegalProof.exactAfterBoardReplay, false)
    assertEquals(SceneCheckGiven.write(boardFacts, illegalMove), None)

    val staleFacts = BoardFacts.untrusted(
      root = boardFacts.root,
      sideToMove = boardFacts.sideToMove,
      header = boardFacts.header,
      sideLegal = boardFacts.sideLegal,
      rivalLegal = boardFacts.rivalLegal,
      control = boardFacts.control,
      material = boardFacts.material,
      pawns = boardFacts.pawns,
      pieces = boardFacts.pieces
    )
    val staleProof = CheckGivenProof.fromBoardFacts(staleFacts, checkingMove)
    assertEquals(staleProof.complete, false)
    assertEquals(staleProof.sameBoardProof, false)
    assertEquals(staleProof.exactAfterBoardReplay, false)
    assertEquals(SceneCheckGiven.write(staleFacts, checkingMove), None)

  test("Stage-3 StoryTable blocks route mismatch stale proof incomplete proof and writerless rows"):
    val row = story
    val incompleteCheckProof =
      row.copy(checkGivenProof = row.checkGivenProof.map(_.copy(afterBoardRivalKingInCheck = false)))
    val missingKingSquare =
      row.copy(checkGivenProof = row.checkGivenProof.map(_.copy(rivalKingSquareAfter = None)))
    val missingReplay =
      row.copy(checkGivenProof = row.checkGivenProof.map(_.copy(exactAfterBoardReplay = false)))

    Vector(
      row.copy(writer = None),
      row.copy(route = Some(quietMove)),
      row.copy(storyProof = StoryProof.empty),
      row.copy(storyProof = StoryProof.untrustedLegalLine(checkingMove)),
      row.copy(checkGivenProof = None),
      incompleteCheckProof,
      missingKingSquare,
      missingReplay,
      row.copy(tactic = Some(Tactic.SafeCheck)),
      row.copy(plan = Some(Plan.KingConvert))
    ).foreach(assertSilent)

  test("Stage-3 BoardFacts-only and engine-only checking evidence cannot create CheckGiven"):
    val boardFacts = facts
    assert(boardFacts.seen.legalCheckMoves.exists(_.line == checkingMove))

    val boardFactsOnly = Story(
      scene = Scene.CheckGiven,
      proof = story.proof,
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('e', 8)),
      anchor = Some(Square('d', 2)),
      route = Some(checkingMove),
      routeSan = Some("Re2+"),
      storyProof = StoryProof.fromBoardFacts(boardFacts, checkingMove)
    )
    assertSilent(boardFactsOnly)

    val engineOnlyCheck = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(checkingMove),
      engineLine = Some(EngineLine(Vector(checkingMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    assertEquals(engineOnlyCheck.evidenceReady, true)
    assertEquals(engineOnlyCheck.storyBound, false)
    assertEquals(engineOnlyCheck.publicClaimAllowed, false)
    assertEquals(SceneCheckGiven.withEngineCheck(story, engineOnlyCheck), None)

    val rawPvWithoutStory = EngineCheck.fromStory(
      facts = boardFacts,
      story = None,
      engineLine = Some(EngineLine(Vector(checkingMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    assertEquals(rawPvWithoutStory.evidenceReady, false)
    assertEquals(rawPvWithoutStory.storyBound, false)
    assertEquals(rawPvWithoutStory.status, EngineCheckStatus.Unknown)

  test("Stage-3 legal check has only bounded gives-check speech key and renderer text"):
    val selected = StoryTable.choose(Vector(story)).head
    assertEquals(selected.role, Role.Lead)
    assertEquals(selected.leadAllowed, true)
    assertEquals(ExplanationPlan.fromSelected(selected).flatMap(_.allowedClaim).map(_.key), Some("gives_check"))
    assertEquals(ExplanationPlan.fromSelected(selected).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("gives_check"))
    assertEquals(ExplanationClaim.CheckGivenAllowed.map(_.key), Vector("gives_check"))

    Vector(
      "checkmate",
      "mate_threat",
      "king_safety",
      "king_unsafe",
      "attack",
      "creates_pressure",
      "takes_initiative",
      "forced",
      "best_move",
      "only_move",
      "winning",
      "decisive",
      "no_counterplay"
    ).foreach: forbidden =>
      assert(ExplanationClaim.CheckGivenForbiddenKeys.contains(forbidden), s"missing forbidden key: $forbidden")
