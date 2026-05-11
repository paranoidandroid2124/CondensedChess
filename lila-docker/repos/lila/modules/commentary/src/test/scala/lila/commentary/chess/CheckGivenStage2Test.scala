package lila.commentary.chess

class CheckGivenStage2Test extends munit.FunSuite:

  private val checkingFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkingMove = Line(Square('d', 2), Square('e', 2))
  private val replyMove = Line(Square('e', 8), Square('f', 8))

  private def facts: BoardFacts =
    BoardFacts.fromFen(checkingFen).toOption.get

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
      engineLine = Some(EngineLine(Vector(checkingMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  test("Stage-2 SceneCheckGiven writes only exact complete CheckGivenProof identity"):
    val boardFacts = facts
    val story = SceneCheckGiven.write(boardFacts, checkingMove).get

    assertEquals(story.scene, Scene.CheckGiven)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('e', 8)))
    assertEquals(story.anchor, Some(Square('d', 2)))
    assertEquals(story.route, Some(checkingMove))
    assertEquals(story.writer, Some(StoryWriter.SceneCheckGiven))
    assertEquals(story.checkGivenProof.exists(_.complete), true)
    assertEquals(story.checkGivenProof.exists(_.checkProducedByLegalMove), true)
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).flatMap(_.allowedClaim).map(_.key),
      Some("gives_check")
    )
    assertEquals(
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey),
      Some("gives_check")
    )

  test("Stage-2 SceneCheckGiven accepts bounded EngineCheck but Refutes blocks lead"):
    val boardFacts = facts
    val story = SceneCheckGiven.write(boardFacts, checkingMove).get

    val supports = SceneCheckGiven.withEngineCheck(story, check(boardFacts, story, EngineCheckStatus.Supports)).get
    val caps = SceneCheckGiven.withEngineCheck(story, check(boardFacts, story, EngineCheckStatus.Caps)).get
    val unknown = SceneCheckGiven.withEngineCheck(story, check(boardFacts, story, EngineCheckStatus.Unknown)).get
    val refutes =
      SceneCheckGiven.withEngineCheck(story, check(boardFacts, story, EngineCheckStatus.Supports, 220, 20)).get

    val supportedVerdict = StoryTable.choose(Vector(supports)).head
    assertEquals(supportedVerdict.role, Role.Lead)
    assertEquals(supportedVerdict.leadAllowed, true)
    assertEquals(ExplanationPlan.fromSelected(supportedVerdict).flatMap(_.allowedClaim).map(_.key), Some("gives_check"))
    assertEquals(ExplanationPlan.fromSelected(supportedVerdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("gives_check"))

    Vector(caps, unknown).foreach: row =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Lead)
      assertEquals(verdict.leadAllowed, true)
      assertEquals(ExplanationPlan.fromSelected(verdict), None)

    val refutedVerdict = StoryTable.choose(Vector(refutes)).head
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

  test("Stage-2 SceneCheckGiven rejects unbound or contaminated engine attachment"):
    val boardFacts = facts
    val story = SceneCheckGiven.write(boardFacts, checkingMove).get
    val validCheck = check(boardFacts, story, EngineCheckStatus.Supports)
    val wrongRouteCheck = validCheck.copy(checkedMove = Some(Line(Square('a', 1), Square('a', 2))))

    assertEquals(SceneCheckGiven.withEngineCheck(story.copy(tactic = Some(Tactic.SafeCheck)), validCheck), None)
    assertEquals(SceneCheckGiven.withEngineCheck(story.copy(checkGivenProof = None), validCheck), None)
    assertEquals(SceneCheckGiven.withEngineCheck(story, wrongRouteCheck), None)
