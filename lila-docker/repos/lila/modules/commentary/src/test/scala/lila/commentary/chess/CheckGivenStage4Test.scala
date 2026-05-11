package lila.commentary.chess

class CheckGivenStage4Test extends munit.FunSuite:

  private val checkingFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkingMove = Line(Square('d', 2), Square('e', 2))
  private val quietMove = Line(Square('d', 2), Square('a', 2))
  private val replyMove = Line(Square('e', 8), Square('f', 8))

  private def facts: BoardFacts =
    BoardFacts.fromFen(checkingFen).toOption.get

  private def story: Story =
    SceneCheckGiven.write(facts, checkingMove).get

  private def check(
      row: Story,
      status: EngineCheckStatus,
      engineLine: EngineLine = EngineLine(Vector(checkingMove)),
      before: Int = 100,
      after: Int = 100,
      freshnessPly: Option[Int] = Some(0)
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(engineLine),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(12),
      freshnessPly = freshnessPly,
      requestedStatus = status
    )

  test("Stage-4 EngineCheck attaches only to complete same-board SceneCheckGiven Stories"):
    val row = story
    val bound = check(row, EngineCheckStatus.Supports)
    assertEquals(bound.storyBound, true)
    assertEquals(bound.evidenceReady, true)
    assertEquals(SceneCheckGiven.withEngineCheck(row, bound).nonEmpty, true)

    val rawEvidence = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(checkingMove),
      engineLine = Some(EngineLine(Vector(checkingMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(100)),
      evalAfter = Some(EngineEval(100)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    assertEquals(rawEvidence.evidenceReady, true)
    assertEquals(rawEvidence.storyBound, false)
    assertEquals(SceneCheckGiven.withEngineCheck(row, rawEvidence), None)

    val wrongRoute = check(row, EngineCheckStatus.Supports, EngineLine(Vector(quietMove)))
    assertEquals(wrongRoute.evidenceReady, false)
    assertEquals(SceneCheckGiven.withEngineCheck(row, wrongRoute), None)

    val stale = check(row, EngineCheckStatus.Supports, freshnessPly = Some(2))
    assertEquals(stale.evidenceReady, false)
    assertEquals(SceneCheckGiven.withEngineCheck(row, stale), None)

    assertEquals(SceneCheckGiven.withEngineCheck(row.copy(checkGivenProof = None), bound), None)
    assertEquals(SceneCheckGiven.withEngineCheck(row.copy(writer = None), bound), None)

  test("Stage-4 Supports Caps Refutes and Unknown create no CheckGiven engine wording"):
    val row = story
    val supports = SceneCheckGiven.withEngineCheck(row, check(row, EngineCheckStatus.Supports)).get
    val caps = SceneCheckGiven.withEngineCheck(row, check(row, EngineCheckStatus.Caps)).get
    val refutes =
      SceneCheckGiven.withEngineCheck(row, check(row, EngineCheckStatus.Supports, before = 250, after = 25)).get
    val unknown = SceneCheckGiven.withEngineCheck(row, check(row, EngineCheckStatus.Unknown)).get

    val supportedVerdict = StoryTable.choose(Vector(supports)).head
    assertEquals(supportedVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportedVerdict.role, Role.Lead)
    assertEquals(supportedVerdict.leadAllowed, true)
    assertEquals(ExplanationPlan.fromSelected(supportedVerdict).flatMap(_.allowedClaim).map(_.key), Some("gives_check"))
    assertEquals(ExplanationPlan.fromSelected(supportedVerdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("gives_check"))

    val cappedVerdict = StoryTable.choose(Vector(caps)).head
    assertEquals(cappedVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(cappedVerdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(cappedVerdict), None)

    val refutedVerdict = StoryTable.choose(Vector(refutes)).head
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(unknownVerdict), None)

    Vector(
      "engine_says_check",
      "eval_number",
      "best_move",
      "only_move",
      "forced_check",
      "winning_check",
      "decisive_check",
      "mate_threat",
      "checkmate",
      "raw_pv",
      "no_counterplay"
    ).foreach: forbidden =>
      assert(!ExplanationClaim.CheckGivenAllowed.map(_.key).contains(forbidden))
      assert(
        ExplanationClaim.CheckGivenForbiddenKeys.contains(forbidden) ||
          Vector("engine_says_check", "eval_number", "forced_check", "winning_check", "decisive_check", "raw_pv")
            .contains(forbidden),
        s"forbidden engine wording must stay outside gives_check: $forbidden"
      )
