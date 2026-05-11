package lila.commentary.chess

class LoosePieceStage4Test extends munit.FunSuite:

  private val looseFen = "4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"
  private val looseMove = Line(Square('d', 2), Square('h', 2))
  private val wrongMove = Line(Square('d', 2), Square('d', 3))
  private val replyMove = Line(Square('e', 8), Square('e', 7))

  test("Stage-4 EngineCheck attaches only to existing proof-backed Tactic.Loose"):
    val facts = board
    val story = looseStory(facts)
    val engineOnly = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(looseMove),
      engineLine = Some(EngineLine(Vector(looseMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val incompleteProofStory = story.copy(
      loosePieceProof = story.loosePieceProof.map(
        _.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("LoosePieceProof", Vector("exact after-board replay"))))
      )
    )
    val inconsistentDefenderStory = story.copy(
      loosePieceProof = story.loosePieceProof.map(
        _.copy(rivalLegalDefendersAfter = Vector(Piece(Side.Black, Man.Knight, Square('f', 4))))
      )
    )
    val support = check(facts, story, EngineCheckStatus.Supports)
    val inconsistentSupport = check(facts, inconsistentDefenderStory, EngineCheckStatus.Supports)

    assertEquals(engineOnly.storyBound, false)
    assertEquals(inconsistentSupport.storyBound, false)
    assertEquals(TacticLoose.withEngineCheck(story, engineOnly), None)
    assertEquals(TacticLoose.withEngineCheck(incompleteProofStory, support), None)
    assertEquals(TacticLoose.withEngineCheck(inconsistentDefenderStory, support), None)
    assertEquals(TacticLoose.withEngineCheck(story.copy(writer = None), support), None)
    assertEquals(TacticLoose.withEngineCheck(story.copy(tactic = Some(Tactic.Hanging)), support), None)

  test("Stage-4 Supports Caps Unknown create no engine claim and Refutes blocks"):
    val facts = board
    val story = looseStory(facts).copy(proof = strongProof)

    val supports = TacticLoose.withEngineCheck(story, check(facts, story, EngineCheckStatus.Supports)).get
    val caps = TacticLoose.withEngineCheck(story, check(facts, story, EngineCheckStatus.Caps)).get
    val unknown = TacticLoose.withEngineCheck(story, check(facts, story, EngineCheckStatus.Unknown)).get
    val refutes =
      TacticLoose.withEngineCheck(story, check(facts, story, EngineCheckStatus.Supports, before = 220, after = 20)).get

    assertNoEngineSpeech(
      "Supports",
      supports,
      EngineCheckStatus.Supports,
      expectedBlocked = false,
      expectedClaim = Some("attacks_loose_piece"),
      expectedRender = Some("Rh2 attacks the undefended piece on h5.")
    )
    assertNoEngineSpeech(
      "Caps",
      caps,
      EngineCheckStatus.Caps,
      expectedBlocked = false,
      expectedClaim = None,
      expectedRender = None
    )
    assertNoEngineSpeech(
      "Unknown",
      unknown,
      EngineCheckStatus.Unknown,
      expectedBlocked = false,
      expectedClaim = Some("attacks_loose_piece"),
      expectedRender = Some("Rh2 attacks the undefended piece on h5.")
    )
    assertNoEngineSpeech(
      "Refutes",
      refutes,
      EngineCheckStatus.Refutes,
      expectedBlocked = true,
      expectedClaim = None,
      expectedRender = None
    )

  test("Stage-4 Engine evidence must bind same Story route and legal line"):
    val facts = board
    val story = looseStory(facts)
    val wrongRouteLine = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(wrongMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val wrongCheckedMove = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(wrongMove),
      engineLine = Some(EngineLine(Vector(wrongMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    assert(wrongRouteLine.missingEvidence.exists(_.missing.contains("same Story route")))
    assertEquals(TacticLoose.withEngineCheck(story, wrongRouteLine), None)
    assertEquals(TacticLoose.withEngineCheck(story, wrongCheckedMove), None)

  private def assertNoEngineSpeech(
      label: String,
      row: Story,
      status: EngineCheckStatus,
      expectedBlocked: Boolean,
      expectedClaim: Option[String],
      expectedRender: Option[String]
  ): Unit =
    val verdict = StoryTable.choose(Vector(row)).head
    assertEquals(verdict.engineCheckStatus, Some(status), s"$label status must be preserved")
    assertEquals(verdict.role == Role.Blocked, expectedBlocked, s"$label block state")
    assertEquals(verdict.leadAllowed, !expectedBlocked && verdict.role == Role.Lead, s"$label lead flag must match role")
    val plan = ExplanationPlan.fromSelected(verdict)
    assertEquals(plan.flatMap(_.allowedClaim).map(_.key), expectedClaim, s"$label must not create an engine claim")
    assertEquals(plan.flatMap(DeterministicRenderer.fromPlan).map(_.text), expectedRender, s"$label render boundary")

  private def check(
      facts: BoardFacts,
      story: Story,
      status: EngineCheckStatus,
      before: Int = 20,
      after: Int = 20
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(looseMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def looseStory(facts: BoardFacts): Story =
    TacticLoose.write(facts, Some(looseMove)).get

  private def board: BoardFacts =
    BoardFacts.fromFen(looseFen).fold(error => fail(s"invalid Stage-4 FEN: $error"), identity)

  private val strongProof: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 80,
      immediacy = 80,
      forcing = 80,
      conversionPrize = 80,
      counterplayRisk = 10,
      kingHeat = 0,
      pieceSupport = 80,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 80
    )
