package lila.commentary.chess

class CheckEscapedStage4Test extends munit.FunSuite:

  private val escapeFen = "k3r3/8/8/8/8/8/8/4K3 w - - 0 1"
  private val escapeMove = Line(Square('e', 1), Square('f', 1))
  private val wrongMove = Line(Square('e', 1), Square('e', 2))
  private val replyMove = Line(Square('a', 8), Square('b', 8))

  private def facts: BoardFacts =
    BoardFacts.fromFen(escapeFen).toOption.get

  private def story: Story =
    SceneCheckEscaped.write(facts, escapeMove).get

  private def check(
      row: Story,
      status: EngineCheckStatus,
      engineLine: EngineLine = EngineLine(Vector(escapeMove)),
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

  test("Stage-4 EngineCheck attaches only to complete same-board SceneCheckEscaped Stories"):
    val row = story
    val bound = check(row, EngineCheckStatus.Supports)
    assertEquals(bound.storyBound, true)
    assertEquals(bound.evidenceReady, true)
    assertEquals(SceneCheckEscaped.withEngineCheck(row, bound).nonEmpty, true)

    val rawEvidence = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(escapeMove),
      engineLine = Some(EngineLine(Vector(escapeMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(100)),
      evalAfter = Some(EngineEval(100)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    assertEquals(rawEvidence.evidenceReady, true)
    assertEquals(rawEvidence.storyBound, false)
    assertEquals(SceneCheckEscaped.withEngineCheck(row, rawEvidence), None)

    val wrongRoute = check(row, EngineCheckStatus.Supports, EngineLine(Vector(wrongMove)))
    assertEquals(wrongRoute.evidenceReady, false)
    assertEquals(SceneCheckEscaped.withEngineCheck(row, wrongRoute), None)

    val stale = check(row, EngineCheckStatus.Supports, freshnessPly = Some(2))
    assertEquals(stale.evidenceReady, false)
    assertEquals(SceneCheckEscaped.withEngineCheck(row, stale), None)

    assertEquals(SceneCheckEscaped.withEngineCheck(row.copy(checkEscapedProof = None), bound), None)
    assertEquals(SceneCheckEscaped.withEngineCheck(row.copy(writer = None), bound), None)

  test("Stage-4 Supports Caps Refutes and Unknown create no CheckEscaped engine wording"):
    val row = story
    val supports = SceneCheckEscaped.withEngineCheck(row, check(row, EngineCheckStatus.Supports)).get
    val caps = SceneCheckEscaped.withEngineCheck(row, check(row, EngineCheckStatus.Caps)).get
    val refutes =
      SceneCheckEscaped.withEngineCheck(row, check(row, EngineCheckStatus.Supports, before = 250, after = 25)).get
    val unknown = SceneCheckEscaped.withEngineCheck(row, check(row, EngineCheckStatus.Unknown)).get

    val supportedVerdict = StoryTable.choose(Vector(supports)).head
    assertEquals(supportedVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportedVerdict.role, Role.Lead)
    assertEquals(supportedVerdict.leadAllowed, true)
    assertEquals(ExplanationPlan.fromSelected(supportedVerdict).flatMap(_.allowedClaim).map(_.key), Some("escapes_check"))
    assertEquals(ExplanationPlan.fromSelected(supportedVerdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("escapes_check"))

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
      "engine_says_this_escapes_check",
      "eval_number",
      "best_move",
      "only_move",
      "forced_move",
      "forced_escape",
      "winning_escape",
      "decisive_escape",
      "avoids_mate",
      "checkmate_defense",
      "raw_pv",
      "no_counterplay"
    ).foreach: forbidden =>
      assert(!ExplanationClaim.CheckEscapedAllowed.map(_.key).contains(forbidden))
      assert(ExplanationClaim.CheckEscapedForbiddenKeys.contains(forbidden), s"missing forbidden engine key: $forbidden")
