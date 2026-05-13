package lila.commentary.chess

class RookHitEngineCheckStoryTableTest extends munit.FunSuite:

  test("EngineCheck attaches only to existing proof-backed RookHit Story"):
    val story = rookHitStory
    val supporting = checkFromStory(story, EngineCheckStatus.Supports)
    val capped = checkFromStory(story, EngineCheckStatus.Caps)
    val refuted = checkFromStory(story, EngineCheckStatus.Refutes)

    assertEquals(supporting.storyBound, true)
    assertEquals(supporting.evidenceReady, true)
    assertEquals(supporting.status, EngineCheckStatus.Supports)
    assertEquals(capped.status, EngineCheckStatus.Caps)
    assertEquals(refuted.status, EngineCheckStatus.Refutes)
    assertEquals(TacticRookHit.withEngineCheck(story, supporting).map(_.engineCheck.map(_.status)), Some(Some(EngineCheckStatus.Supports)))
    assertEquals(TacticRookHit.withEngineCheck(story, capped).map(_.engineCheck.map(_.status)), Some(Some(EngineCheckStatus.Caps)))
    assertEquals(TacticRookHit.withEngineCheck(story, refuted), None)
    assertEquals(TacticRookHit.withEngineCheck(story, engineOnlyCheck), None)

  test("EngineCheck cannot create or repair RookHitProof"):
    val story = rookHitStory
    val supporting = checkFromStory(story, EngineCheckStatus.Supports)
    val missingProof = story.copy(rookHitProof = None)
    val incompleteProof =
      story.copy(
        rookHitProof = story.rookHitProof.map(
          _.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("RookHitProof", Vector("after-board rook attack"))))
        )
      )
    val forgedEngineOnly =
      Story(
        scene = Scene.Tactic,
        tactic = Some(Tactic.RookHit),
        side = story.side,
        rival = story.rival,
        target = story.target,
        anchor = story.anchor,
        route = story.route,
        routeSan = story.routeSan,
        proof = story.proof,
        storyProof = story.storyProof,
        writer = Some(StoryWriter.TacticRookHit),
        engineCheck = Some(supporting)
      )

    assertEquals(TacticRookHit.withEngineCheck(missingProof, supporting), None)
    assertEquals(TacticRookHit.withEngineCheck(incompleteProof, supporting), None)
    Vector(missingProof, incompleteProof, forgedEngineOnly).foreach: row =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, row.toString)
      assertNoRookHitText(verdict, row.toString)

  test("EngineCheck rejects unknown unbound stale and wrong-route RookHit evidence"):
    val story = rookHitStory
    val unknown = evidenceCheck(story, EngineCheckStatus.Unknown).copy(storyBound = true)
    val unbound = evidenceCheck(story, EngineCheckStatus.Supports)
    val stale = checkFromStory(story, EngineCheckStatus.Supports).copy(freshnessPly = Some(2), missingEvidence = Vector(BoardFacts.MissingEvidence("EngineCheck", Vector("fresh engine evidence"))))
    val wrongRoute =
      evidenceCheck(story, EngineCheckStatus.Supports)
        .copy(
          storyBound = true,
          checkedMove = Some(Line(Square('d', 2), Square('d', 5))),
          engineLine = Some(EngineLine(Vector(Line(Square('d', 2), Square('d', 5)))))
        )

    assertEquals(TacticRookHit.withEngineCheck(story, unknown), None)
    assertEquals(TacticRookHit.withEngineCheck(story, unbound), None)
    assertEquals(TacticRookHit.withEngineCheck(story, stale), None)
    assertEquals(TacticRookHit.withEngineCheck(story, wrongRoute), None)

  test("StoryTable orders RookHit Lead Support Context Blocked capped and refuted rows"):
    val lead = StoryTable.choose(Vector(rookHitStory)).head
    val support =
      StoryTable
        .choose(Vector(rookHitStory, rookHitStory.copy(proof = supportProof)))
        .find(_.role == Role.Support)
        .getOrElse(fail("expected a non-lead RookHit support row"))
    val context = StoryTable.choose(Vector(rookHitStory.copy(proof = lowProof))).head
    val blocked = StoryTable.choose(Vector(rookHitStory.copy(target = Some(Square('h', 4))))).head
    val capped = StoryTable.choose(Vector(TacticRookHit.withEngineCheck(rookHitStory, checkFromStory(rookHitStory, EngineCheckStatus.Caps)).get)).head
    val refuted = StoryTable.choose(Vector(rookHitStory.copy(engineCheck = Some(checkFromStory(rookHitStory, EngineCheckStatus.Refutes))))).head

    assertEquals(lead.role, Role.Lead)
    assertEquals(support.role, Role.Support)
    assertEquals(context.role, Role.Context)
    assertEquals(blocked.role, Role.Blocked)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)

    assertRookHitText(lead, "Lead")
    Vector(support, context, blocked, capped, refuted).foreach: verdict =>
      assertNoRookHitText(verdict, verdict.toString)

  test("raw eval PV and depth cannot create or strengthen RookHit"):
    val lowEngine =
      StoryTable
        .choose(Vector(TacticRookHit.withEngineCheck(rookHitStory, checkFromStory(rookHitStory, EngineCheckStatus.Supports, before = 0, after = 0, depth = 31336)).get))
        .head
    val highEngine =
      StoryTable
        .choose(Vector(TacticRookHit.withEngineCheck(rookHitStory, checkFromStory(rookHitStory, EngineCheckStatus.Supports, before = -900, after = 900, depth = 31337)).get))
        .head
    val rawEngineOnly =
      StoryTable
        .choose(Vector(rookHitStory.copy(writer = None, rookHitProof = None, engineCheck = Some(engineOnlyCheck))))
        .head

    assertEquals(lowEngine.role, Role.Lead)
    assertEquals(highEngine.role, Role.Lead)
    assertEquals(highEngine.strength, lowEngine.strength)
    assertEquals(rawEngineOnly.role, Role.Blocked)
    Vector(lowEngine, highEngine, rawEngineOnly).foreach: verdict =>
      assertEquals(verdict.values.contains(900.0), false, verdict.toString)
      assertEquals(verdict.values.contains(31337.0), false, verdict.toString)
    assertRookHitText(lowEngine, "low raw eval")
    assertRookHitText(highEngine, "high raw eval")
    assertNoRookHitText(rawEngineOnly, "raw engine only")

  private def rookHitStory: Story =
    TacticRookHit.write(facts, Some(rookHitMove), Some(rookHitTarget)).get

  private def checkFromStory(
      story: Story,
      status: EngineCheckStatus,
      before: Int = 0,
      after: Int = 0,
      depth: Int = 18
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(rookHitMove, Line(Square('e', 8), Square('e', 7))))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(if status == EngineCheckStatus.Refutes then EngineEval(-200) else EngineEval(after)),
      depth = Some(depth),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def evidenceCheck(story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = story.route,
      engineLine = story.route.map(route => EngineLine(Vector(route))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(if status == EngineCheckStatus.Refutes then EngineEval(-200) else EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def engineOnlyCheck: EngineCheck =
    EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(rookHitMove),
      engineLine = Some(EngineLine(Vector(rookHitMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

  private def assertNoRookHitText(verdict: Verdict, label: String): Unit =
    val plan = ExplanationPlan.fromSelected(verdict)
    val rendered = plan.flatMap(DeterministicRenderer.fromPlan)
    assertEquals(plan, None, label)
    assertEquals(rendered, None, label)

  private def assertRookHitText(verdict: Verdict, label: String): Unit =
    val plan = ExplanationPlan.fromSelected(verdict)
    val rendered = plan.flatMap(DeterministicRenderer.fromPlan)
    assertEquals(plan.flatMap(_.allowedClaim).map(_.key), Some("attacks_rook"), label)
    assertEquals(rendered.map(_.claimKey), Some("attacks_rook"), label)
    assertEquals(rendered.map(_.text), Some("Rh2 attacks the rook on h5."), label)

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid RookHit EngineCheck FEN: $fen -> $error"), identity)

  private val facts = board("4k3/8/8/7r/8/8/3R4/4K3 w - - 0 1")
  private val rookHitMove = Line(Square('d', 2), Square('h', 2))
  private val rookHitTarget = Square('h', 5)

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
