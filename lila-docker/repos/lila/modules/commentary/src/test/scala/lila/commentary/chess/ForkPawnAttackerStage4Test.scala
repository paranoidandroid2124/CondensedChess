package lila.commentary.chess

class ForkPawnAttackerStage4Test extends munit.FunSuite:

  private val forkFen = "7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1"
  private val forkFacts = facts(forkFen)
  private val forkRoute = Line(Square('e', 4), Square('e', 5))
  private val quietRoute = Line(Square('h', 1), Square('h', 2))
  private val replyRoute = Line(Square('h', 8), Square('h', 7))
  private val targetA = Square('d', 6)
  private val targetB = Square('f', 6)

  test("Stage-4 EngineCheck attaches only to complete pawn-attacker Fork rows"):
    val story = forkStory
    val supports = check(story, EngineCheckStatus.Supports)
    val proofless = story.copy(multiTargetProof = None)
    val incompleteStoryProof = story.copy(storyProof = StoryProof.empty)
    val contaminated = story.copy(tactic = Some(Tactic.PawnFork))
    val writerless = story.copy(writer = None)
    val engineOnly = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(forkRoute),
      engineLine = Some(EngineLine(Vector(forkRoute))),
      replyLine = Some(EngineLine(Vector(replyRoute))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    assertEquals(TacticFork.withEngineCheck(story, supports).map(_.engineCheck.map(_.status)), Some(Some(EngineCheckStatus.Supports)))
    assertEquals(TacticFork.withEngineCheck(proofless, supports), None)
    assertEquals(TacticFork.withEngineCheck(incompleteStoryProof, supports), None)
    assertEquals(TacticFork.withEngineCheck(contaminated, supports), None)
    assertEquals(TacticFork.withEngineCheck(writerless, supports), None)
    assertEquals(TacticFork.withEngineCheck(story, engineOnly), None)

  test("Stage-4 EngineCheck statuses reuse Fork without creating engine speech"):
    val story = forkStory
    val supports = TacticFork.withEngineCheck(story, check(story, EngineCheckStatus.Supports)).get
    val caps = TacticFork.withEngineCheck(story, check(story, EngineCheckStatus.Caps)).get
    val unknown = TacticFork.withEngineCheck(story, check(story, EngineCheckStatus.Unknown)).get
    val refutes =
      TacticFork.withEngineCheck(story, check(story, EngineCheckStatus.Supports, before = 220, after = 20)).get

    assertForkPlan(supports, EngineCheckStatus.Supports, expectedRole = Role.Lead, allowedClaim = Some("forks_two_targets"))
    assertForkPlan(caps, EngineCheckStatus.Caps, expectedRole = Role.Lead, allowedClaim = None)
    assertForkPlan(unknown, EngineCheckStatus.Unknown, expectedRole = Role.Lead, allowedClaim = Some("forks_two_targets"))
    assertForkPlan(refutes, EngineCheckStatus.Refutes, expectedRole = Role.Blocked, allowedClaim = None)

  test("Stage-4 EngineCheck evidence must bind the same legal Fork route"):
    val story = forkStory
    val wrongEngineLine = check(story, EngineCheckStatus.Supports, engineRoute = quietRoute)
    val wrongCheckedMove = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(quietRoute),
      engineLine = Some(EngineLine(Vector(quietRoute))),
      replyLine = Some(EngineLine(Vector(replyRoute))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    assertEquals(wrongEngineLine.evidenceReady, false)
    assert(wrongEngineLine.missingEvidence.exists(_.missing.contains("same Story route")))
    assertEquals(TacticFork.withEngineCheck(story, wrongEngineLine), None)
    assertEquals(TacticFork.withEngineCheck(story, wrongCheckedMove), None)

  test("Stage-4 Fork forbidden wording blocks engine and stronger pawn-fork claims"):
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(forkStory)).head).get
    val forbiddenKeys = plan.forbiddenWording.map(_.key).toSet

    Vector(
      "engine_says",
      "eval_number",
      "raw_pv",
      "best_move",
      "only_move",
      "forced",
      "wins_material",
      "wins_pawn",
      "pawn_fork_wins",
      "decisive",
      "winning"
    ).foreach: closed =>
      assert(forbiddenKeys.exists(_.contains(closed)), closed)

  private def assertForkPlan(
      story: Story,
      status: EngineCheckStatus,
      expectedRole: Role,
      allowedClaim: Option[String]
  ): Unit =
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict)

    assertEquals(verdict.engineCheckStatus, Some(status))
    assertEquals(verdict.role, expectedRole)
    assertEquals(verdict.leadAllowed, expectedRole == Role.Lead)
    assertEquals(plan.flatMap(_.allowedClaim.map(_.key)), allowedClaim)
    if allowedClaim.nonEmpty then
      assertEquals(plan.exists(_.forbiddenWording.exists(_.key.contains("engine_says"))), true)
      assertEquals(plan.exists(_.forbiddenWording.exists(_.key.contains("raw_pv"))), true)
    else assertEquals(plan, None)

  private def check(
      story: Story,
      status: EngineCheckStatus,
      before: Int = 20,
      after: Int = 20,
      engineRoute: Line = forkRoute
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = forkFacts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(engineRoute))),
      replyLine = Some(EngineLine(Vector(replyRoute))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def forkStory: Story =
    TacticFork.write(forkFacts, Some(forkRoute), Some(targetA), Some(targetB)).get

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-4 FEN: $fen -> $error"), identity)
