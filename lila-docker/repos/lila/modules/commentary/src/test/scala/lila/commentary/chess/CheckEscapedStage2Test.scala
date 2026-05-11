package lila.commentary.chess

class CheckEscapedStage2Test extends munit.FunSuite:

  private val interposeFen = "k3r3/8/8/8/8/8/3B4/4K3 w - - 0 1"
  private val interposeMove = Line(Square('d', 2), Square('e', 3))
  private val replyMove = Line(Square('a', 8), Square('b', 8))

  private def facts: BoardFacts =
    BoardFacts.fromFen(interposeFen).toOption.get

  private def check(
      facts: BoardFacts,
      story: Story,
      status: EngineCheckStatus,
      before: Int = 200,
      after: Int = 200
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(interposeMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  test("Stage-2 SceneCheckEscaped writes only exact complete CheckEscapedProof identity"):
    val boardFacts = facts
    val story = SceneCheckEscaped.write(boardFacts, interposeMove).get

    assertEquals(story.scene, Scene.CheckEscaped)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('e', 1)))
    assertEquals(story.anchor, Some(Square('d', 2)))
    assertEquals(story.route, Some(interposeMove))
    assertEquals(story.writer, Some(StoryWriter.SceneCheckEscaped))
    assertEquals(story.checkEscapedProof.exists(_.complete), true)
    assertEquals(story.checkEscapedProof.flatMap(_.originSquare), Some(Square('d', 2)))
    assertEquals(story.checkEscapedProof.flatMap(_.afterKingSquare), Some(Square('e', 1)))
    assertEquals(story.checkEscapedProof.exists(_.checkEscapedByLegalMove), true)
    assertEquals(story.checkEscapedProof.exists(_.exactBeforeBoardState), true)
    assertEquals(story.checkEscapedProof.exists(_.exactAfterBoardReplay), true)
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(story.captureResult, None)
    assertEquals(story.checkGivenProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.pawnStopProof, None)
    assertEquals(story.pawnBreakProof, None)
    assertEquals(story.pawnBlockProof, None)
    assertEquals(story.pawnCaptureProof, None)
    assertEquals(story.passedPawnCreatedProof, None)
    assertEquals(story.promotionThreatProof, None)
    assertEquals(story.promotionProof, None)

  test("Stage-2 SceneCheckEscaped accepts bounded EngineCheck but Refutes blocks lead"):
    val boardFacts = facts
    val story = SceneCheckEscaped.write(boardFacts, interposeMove).get

    val supports = SceneCheckEscaped.withEngineCheck(story, check(boardFacts, story, EngineCheckStatus.Supports)).get
    val caps = SceneCheckEscaped.withEngineCheck(story, check(boardFacts, story, EngineCheckStatus.Caps)).get
    val unknown = SceneCheckEscaped.withEngineCheck(story, check(boardFacts, story, EngineCheckStatus.Unknown)).get
    val refutes =
      SceneCheckEscaped.withEngineCheck(story, check(boardFacts, story, EngineCheckStatus.Supports, 220, 20)).get

    val supportedVerdict = StoryTable.choose(Vector(supports)).head
    assertEquals(supportedVerdict.role, Role.Lead)
    assertEquals(supportedVerdict.leadAllowed, true)

    Vector(caps, unknown).foreach: row =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Lead)
      assertEquals(verdict.leadAllowed, true)

    val refutedVerdict = StoryTable.choose(Vector(refutes)).head
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.leadAllowed, false)

  test("Stage-2 SceneCheckEscaped rejects unbound or contaminated engine attachment"):
    val boardFacts = facts
    val story = SceneCheckEscaped.write(boardFacts, interposeMove).get
    val validCheck = check(boardFacts, story, EngineCheckStatus.Supports)
    val wrongRouteCheck = validCheck.copy(checkedMove = Some(Line(Square('a', 1), Square('a', 2))))

    assertEquals(SceneCheckEscaped.withEngineCheck(story.copy(tactic = Some(Tactic.SafeCheck)), validCheck), None)
    assertEquals(SceneCheckEscaped.withEngineCheck(story.copy(checkEscapedProof = None), validCheck), None)
    assertEquals(SceneCheckEscaped.withEngineCheck(story, wrongRouteCheck), None)
