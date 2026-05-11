package lila.commentary.chess

class QueenHitStage4Test extends munit.FunSuite:

  private val queenHitFen = "4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1"
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))
  private val quietMove = Line(Square('d', 2), Square('a', 2))
  private val replyMove = Line(Square('e', 8), Square('e', 7))

  test("Stage-4 EngineCheck attaches only to already proof-backed QueenHit Story rows"):
    val board = facts(queenHitFen)
    val story = TacticQueenHit.write(board, Some(queenHitMove)).get
    val supports = check(board, story, EngineCheckStatus.Supports)
    val proofless = story.copy(queenHitProof = None)
    val incompleteStoryProof = story.copy(storyProof = StoryProof.empty)
    val contaminated = story.copy(scene = Scene.Material, tactic = None)
    val writerless = story.copy(writer = None)
    val engineOnly = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(queenHitMove),
      engineLine = Some(EngineLine(Vector(queenHitMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    assertEquals(TacticQueenHit.withEngineCheck(story, supports).map(_.engineCheck.map(_.status)), Some(Some(EngineCheckStatus.Supports)))
    assertEquals(TacticQueenHit.withEngineCheck(proofless, supports), None)
    assertEquals(TacticQueenHit.withEngineCheck(incompleteStoryProof, supports), None)
    assertEquals(TacticQueenHit.withEngineCheck(contaminated, supports), None)
    assertEquals(TacticQueenHit.withEngineCheck(writerless, supports), None)
    assertEquals(TacticQueenHit.withEngineCheck(story, engineOnly), None)
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty)

  test("Stage-4 EngineCheck support caps refutes and unknown create no QueenHit speech"):
    val board = facts(queenHitFen)
    val story = TacticQueenHit.write(board, Some(queenHitMove)).get
    val supports = TacticQueenHit.withEngineCheck(story, check(board, story, EngineCheckStatus.Supports)).get
    val caps = TacticQueenHit.withEngineCheck(story, check(board, story, EngineCheckStatus.Caps)).get
    val unknown = TacticQueenHit.withEngineCheck(story, check(board, story, EngineCheckStatus.Unknown)).get
    val refutes =
      TacticQueenHit.withEngineCheck(story, check(board, story, EngineCheckStatus.Supports, before = 220, after = 20)).get

    assertEngineBoundNoSpeech(supports, EngineCheckStatus.Supports, expectedRole = Role.Context, capped = false)
    assertEngineBoundNoSpeech(caps, EngineCheckStatus.Caps, expectedRole = Role.Context, capped = true)
    assertEngineBoundNoSpeech(unknown, EngineCheckStatus.Unknown, expectedRole = Role.Context, capped = false)
    assertEngineBoundNoSpeech(refutes, EngineCheckStatus.Refutes, expectedRole = Role.Blocked, capped = false)

  test("Stage-4 EngineCheck evidence must bind the same QueenHit route and legal line"):
    val board = facts(queenHitFen)
    val story = TacticQueenHit.write(board, Some(queenHitMove)).get
    val wrongEngineLine = check(
      board,
      story,
      EngineCheckStatus.Supports,
      engineRoute = quietMove
    )
    val wrongCheckedMove = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(quietMove),
      engineLine = Some(EngineLine(Vector(quietMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    assertEquals(wrongEngineLine.evidenceReady, false)
    assert(wrongEngineLine.missingEvidence.exists(_.missing.contains("same Story route")))
    assertEquals(TacticQueenHit.withEngineCheck(story, wrongEngineLine), None)
    assertEquals(TacticQueenHit.withEngineCheck(story, wrongCheckedMove), None)

  private def assertEngineBoundNoSpeech(
      story: Story,
      status: EngineCheckStatus,
      expectedRole: Role,
      capped: Boolean
  ): Unit =
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(verdict.engineCheckStatus, Some(status))
    assertEquals(verdict.engineStrengthLimited, capped)
    assertEquals(verdict.role, expectedRole)
    assertEquals(verdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)

  private def check(
      facts: BoardFacts,
      story: Story,
      status: EngineCheckStatus,
      before: Int = 20,
      after: Int = 20,
      engineRoute: Line = queenHitMove
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(engineRoute))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-4 FEN: $fen -> $error"), identity)
