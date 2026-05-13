package lila.commentary.chess

class InterferenceEngineCheckTest extends munit.FunSuite:

  test("EngineCheck attaches to proof-backed Interference Story"):
    val story = interferenceStory
    val supporting = checkFromStory(story, EngineCheckStatus.Supports)
    val capped = checkFromStory(story, EngineCheckStatus.Caps)
    val refuted = checkFromStory(story, EngineCheckStatus.Refutes)

    assertEquals(supporting.storyBound, true)
    assertEquals(supporting.evidenceReady, true)
    assertEquals(supporting.status, EngineCheckStatus.Supports)
    assertEquals(TacticInterference.withEngineCheck(story, supporting).flatMap(_.engineCheck.map(_.status)), Some(EngineCheckStatus.Supports))
    assertEquals(TacticInterference.withEngineCheck(story, capped).flatMap(_.engineCheck.map(_.status)), Some(EngineCheckStatus.Caps))
    assertEquals(TacticInterference.withEngineCheck(story, refuted).flatMap(_.engineCheck.map(_.status)), Some(EngineCheckStatus.Refutes))

  test("EngineCheck cannot create or repair Interference proof"):
    val story = interferenceStory
    val supporting = checkFromStory(story, EngineCheckStatus.Supports)
    val missingProof = story.copy(interferenceProof = None)
    val incompleteProof =
      story.interferenceProof.get.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("InterferenceProof", Vector("line block"))))
    val incompleteStory = story.copy(interferenceProof = Some(incompleteProof))
    val engineOnly =
      Story(
        scene = Scene.Tactic,
        tactic = Some(Tactic.Interference),
        side = story.side,
        rival = story.rival,
        target = story.target,
        anchor = story.anchor,
        route = story.route,
        routeSan = story.routeSan,
        proof = story.proof,
        storyProof = story.storyProof,
        writer = Some(StoryWriter.TacticInterference),
        engineCheck = Some(supporting)
      )

    assertEquals(TacticInterference.withEngineCheck(missingProof, supporting), None)
    assertEquals(TacticInterference.withEngineCheck(incompleteStory, supporting), None)
    assertEquals(StoryTable.choose(Vector(engineOnly)).head.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(engineOnly)).head), None)

  test("EngineCheck attachment rejects unbound unknown and wrong-route evidence"):
    val story = interferenceStory
    val unknown = evidenceCheck(story, EngineCheckStatus.Unknown).copy(storyBound = true)
    val unbound = evidenceCheck(story, EngineCheckStatus.Supports)
    val wrongRoute =
      evidenceCheck(story, EngineCheckStatus.Supports)
        .copy(
          storyBound = true,
          checkedMove = Some(Line(Square('b', 3), Square('c', 2))),
          engineLine = Some(EngineLine(Vector(Line(Square('b', 3), Square('c', 2)))))
        )

    assertEquals(TacticInterference.withEngineCheck(story, unknown), None)
    assertEquals(TacticInterference.withEngineCheck(story, unbound), None)
    assertEquals(TacticInterference.withEngineCheck(story, wrongRoute), None)

  test("StoryTable orders Interference Lead Support Context Blocked capped and refuted rows"):
    val lead = StoryTable.choose(Vector(interferenceStory)).head
    val support =
      StoryTable
        .choose(Vector(interferenceStory, interferenceStory.copy(proof = supportProof)))
        .find(_.role == Role.Support)
        .getOrElse(fail("expected a non-lead Interference support row"))
    val context = StoryTable.choose(Vector(interferenceStory.copy(proof = lowProof))).head
    val blocked = StoryTable.choose(Vector(interferenceStory.copy(target = Some(Square('a', 2))))).head
    val capped = StoryTable.choose(Vector(TacticInterference.withEngineCheck(interferenceStory, checkFromStory(interferenceStory, EngineCheckStatus.Caps)).get)).head
    val refuted = StoryTable.choose(Vector(TacticInterference.withEngineCheck(interferenceStory, checkFromStory(interferenceStory, EngineCheckStatus.Refutes)).get)).head

    assertEquals(lead.role, Role.Lead)
    assertEquals(support.role, Role.Support)
    assertEquals(context.role, Role.Context)
    assertEquals(blocked.role, Role.Blocked)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)

    assertEquals(ExplanationPlan.fromSelected(lead).flatMap(_.allowedClaim), Some(ExplanationClaim.BlocksDefenderLine))

    Vector(support, context, blocked, capped, refuted).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, verdict.toString)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, verdict.toString)

  private def interferenceStory: Story =
    TacticInterference.write(facts, Some(interferenceMove), Some(defender), Some(blockingSquare), Some(target)).get

  private def checkFromStory(story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = story.route.map(route => EngineLine(Vector(route))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(if status == EngineCheckStatus.Refutes then EngineEval(-200) else EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def evidenceCheck(story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = story.route,
      engineLine = story.route.map(route => EngineLine(Vector(route))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(if status == EngineCheckStatus.Refutes then EngineEval(-200) else EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Interference EngineCheck FEN: $fen -> $error"), identity)

  private val facts = board("r6k/8/8/8/8/1B6/8/n6K w - - 0 1")
  private val interferenceMove = Line(Square('b', 3), Square('a', 4))
  private val defender = Square('a', 8)
  private val blockingSquare = Square('a', 4)
  private val target = Square('a', 1)

  private val lowProof =
    Proof(
      boardProof = 70,
      lineProof = 70,
      ownerProof = 70,
      anchorProof = 70,
      routeProof = 70,
      persistence = 0,
      immediacy = 0,
      forcing = 0,
      conversionPrize = 0,
      counterplayRisk = 70,
      kingHeat = 0,
      pieceSupport = 0,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 0
    )

  private val supportProof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 95,
      immediacy = 85,
      forcing = 0,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = 95,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 95
    )
