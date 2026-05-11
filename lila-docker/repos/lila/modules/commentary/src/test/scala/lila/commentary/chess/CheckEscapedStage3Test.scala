package lila.commentary.chess

class CheckEscapedStage3Test extends munit.FunSuite:

  private val escapeFen = "k3r3/8/8/8/8/8/8/4K3 w - - 0 1"
  private val quietFen = "k7/8/8/8/8/8/8/4K3 w - - 0 1"
  private val escapeMove = Line(Square('e', 1), Square('f', 1))
  private val illegalMove = Line(Square('e', 1), Square('e', 2))
  private val replyMove = Line(Square('a', 8), Square('b', 8))

  private def facts: BoardFacts =
    BoardFacts.fromFen(escapeFen).toOption.get

  private def story: Story =
    SceneCheckEscaped.write(facts, escapeMove).get

  private def assertSilent(row: Story): Unit =
    val verdict = StoryTable.choose(Vector(row)).head
    assertEquals(verdict.role, Role.Blocked)
    assertEquals(verdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None)

  test("Stage-3 CheckEscaped stays silent for missing legal replay non-check and stale board states"):
    val escapeFacts = facts
    val quietFacts = BoardFacts.fromFen(quietFen).toOption.get

    val quietProof = CheckEscapedProof.fromBoardFacts(quietFacts, escapeMove)
    assertEquals(quietProof.complete, false)
    assertEquals(quietProof.legalMove, true)
    assertEquals(quietProof.beforeBoardSideKingInCheck, false)
    assertEquals(quietProof.checkEscapedByLegalMove, false)
    assertEquals(SceneCheckEscaped.write(quietFacts, escapeMove), None)

    val illegalProof = CheckEscapedProof.fromBoardFacts(escapeFacts, illegalMove)
    assertEquals(illegalProof.complete, false)
    assertEquals(illegalProof.legalMove, false)
    assertEquals(illegalProof.exactAfterBoardReplay, false)
    assertEquals(illegalProof.afterBoardSideKingNotInCheck, false)
    assertEquals(SceneCheckEscaped.write(escapeFacts, illegalMove), None)

    val staleFacts = BoardFacts.untrusted(
      root = escapeFacts.root,
      sideToMove = escapeFacts.sideToMove,
      header = escapeFacts.header,
      sideLegal = escapeFacts.sideLegal,
      rivalLegal = escapeFacts.rivalLegal,
      control = escapeFacts.control,
      material = escapeFacts.material,
      pawns = escapeFacts.pawns,
      pieces = escapeFacts.pieces
    )
    val staleProof = CheckEscapedProof.fromBoardFacts(staleFacts, escapeMove)
    assertEquals(BoardFacts.sideInCheck(staleFacts, Side.White), true)
    assert(staleFacts.sideLegal.lines.nonEmpty)
    assertEquals(staleProof.complete, false)
    assertEquals(staleProof.sameBoardProof, false)
    assertEquals(staleProof.exactAfterBoardReplay, false)
    assertEquals(staleProof.beforeBoardSideKingInCheck, false)
    assertEquals(SceneCheckEscaped.write(staleFacts, escapeMove), None)

  test("Stage-3 StoryTable blocks route mismatch stale proof incomplete proof and contaminated rows"):
    val row = story
    val incompleteCheckProof =
      row.copy(checkEscapedProof = row.checkEscapedProof.map(_.copy(afterBoardSideKingNotInCheck = false)))
    val missingBeforeKing =
      row.copy(checkEscapedProof = row.checkEscapedProof.map(_.copy(beforeKingSquare = None)))
    val missingAfterKing =
      row.copy(checkEscapedProof = row.checkEscapedProof.map(_.copy(afterKingSquare = None)))
    val missingReplay =
      row.copy(checkEscapedProof = row.checkEscapedProof.map(_.copy(exactAfterBoardReplay = false)))
    val missingMoveIdentity =
      row.copy(checkEscapedProof = row.checkEscapedProof.map(_.copy(escapeMove = None)))

    Vector(
      row.copy(writer = None),
      row.copy(route = Some(illegalMove)),
      row.copy(storyProof = StoryProof.empty),
      row.copy(storyProof = StoryProof.untrustedLegalLine(escapeMove)),
      row.copy(checkEscapedProof = None),
      incompleteCheckProof,
      missingBeforeKing,
      missingAfterKing,
      missingReplay,
      missingMoveIdentity,
      row.copy(tactic = Some(Tactic.SafeCheck)),
      row.copy(plan = Some(Plan.KingConvert)),
      row.copy(checkGivenProof = Some(CheckGivenProof.fromBoardFacts(facts, illegalMove)))
    ).foreach(assertSilent)

  test("Stage-3 BoardFacts-only and engine-only escape evidence cannot create CheckEscaped"):
    val boardFacts = facts
    assert(BoardFacts.sideInCheck(boardFacts, Side.White))
    assert(boardFacts.sideLegal.lines.contains(escapeMove))

    val boardFactsOnly = Story(
      scene = Scene.CheckEscaped,
      proof = story.proof,
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('f', 1)),
      anchor = Some(Square('e', 1)),
      route = Some(escapeMove),
      routeSan = Some("Kf1"),
      storyProof = StoryProof.fromBoardFacts(boardFacts, escapeMove)
    )
    assertSilent(boardFactsOnly)

    val engineOnlyEscape = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(escapeMove),
      engineLine = Some(EngineLine(Vector(escapeMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    assertEquals(engineOnlyEscape.evidenceReady, true)
    assertEquals(engineOnlyEscape.storyBound, false)
    assertEquals(engineOnlyEscape.publicClaimAllowed, false)
    assertEquals(SceneCheckEscaped.withEngineCheck(story, engineOnlyEscape), None)

    val rawPvWithoutStory = EngineCheck.fromStory(
      facts = boardFacts,
      story = None,
      engineLine = Some(EngineLine(Vector(escapeMove))),
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

  test("Stage-3 CheckEscaped has only bounded escapes-check key and forbids adjacent wording"):
    val selected = StoryTable.choose(Vector(story)).head
    assertEquals(selected.role, Role.Lead)
    assertEquals(selected.leadAllowed, true)
    assertEquals(ExplanationPlan.fromSelected(selected).flatMap(_.allowedClaim).map(_.key), Some("escapes_check"))
    assertEquals(ExplanationPlan.fromSelected(selected).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("escapes_check"))
    assertEquals(ExplanationClaim.CheckEscapedAllowed.map(_.key), Vector("escapes_check"))

    Vector(
      "king_escapes_check",
      "blocks_check",
      "captures_checker",
      "mate_threat",
      "checkmate",
      "avoids_mate",
      "king_safety",
      "safe_king",
      "unsafe_king",
      "defense_success",
      "refutes_attack",
      "pressure",
      "initiative",
      "forced",
      "best_move",
      "only_move",
      "winning",
      "decisive",
      "no_counterplay"
    ).foreach: forbidden =>
      assert(ExplanationClaim.CheckEscapedForbiddenKeys.contains(forbidden), s"missing forbidden key: $forbidden")
