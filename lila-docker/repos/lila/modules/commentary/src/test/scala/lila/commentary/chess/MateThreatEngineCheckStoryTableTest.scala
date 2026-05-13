package lila.commentary.chess

class MateThreatEngineCheckStoryTableTest extends munit.FunSuite:

  private val mateThreatFen = "7k/8/6K1/8/8/8/5Q2/8 w - - 0 1"
  private val threatMove = Line(Square('g', 6), Square('h', 6))
  private val replyMove = Line(Square('h', 8), Square('g', 8))

  private def facts: BoardFacts =
    BoardFacts.fromFen(mateThreatFen).toOption.get

  private def story: Story =
    SceneMateThreat.write(facts, threatMove).get

  private def engineCheck(row: Story, status: EngineCheckStatus, before: Int = 100, after: Int = 100): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(threatMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(16),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def offRouteEngineCheck(row: Story): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(Line(Square('g', 6), Square('f', 6))))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(100)),
      evalAfter = Some(EngineEval(100)),
      depth = Some(16),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

  private def assertSilent(label: String, verdict: Verdict): Unit =
    val plan = ExplanationPlan.fromSelected(verdict)
    assertEquals(plan.flatMap(_.allowedClaim).map(_.key), None, label)
    assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)

  private def selected(row: Story): Verdict =
    StoryTable.choose(Vector(row)).head

  test("MateThreat EngineCheck attaches only to existing proof-backed SceneMateThreat rows"):
    val clean = story
    val supports = engineCheck(clean, EngineCheckStatus.Supports)
    val caps = engineCheck(clean, EngineCheckStatus.Caps)
    val refutes = engineCheck(clean, EngineCheckStatus.Supports, before = 220, after = 20)

    assertEquals(supports.status, EngineCheckStatus.Supports)
    assertEquals(caps.status, EngineCheckStatus.Caps)
    assertEquals(refutes.status, EngineCheckStatus.Refutes)
    assertEquals(SceneMateThreat.withEngineCheck(clean, supports).flatMap(_.engineCheck).map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(SceneMateThreat.withEngineCheck(clean, caps).flatMap(_.engineCheck).map(_.status), Some(EngineCheckStatus.Caps))
    assertEquals(SceneMateThreat.withEngineCheck(clean, refutes).flatMap(_.engineCheck).map(_.status), Some(EngineCheckStatus.Refutes))

    Vector(
      "missing proof" -> clean.copy(mateThreatProof = None),
      "incomplete proof" -> clean.copy(mateThreatProof = clean.mateThreatProof.map(_.copy(legalMove = false))),
      "wrong scene" -> clean.copy(scene = Scene.Checkmate),
      "writerless" -> clean.copy(writer = None),
      "wrong route" -> clean.copy(route = Some(Line(Square('g', 6), Square('g', 7)))),
      "with checkmate proof" -> clean.copy(checkmateProof = Some(CheckmateProof.fromBoardFacts(facts, Line(Square('f', 2), Square('f', 8)))))
    ).foreach: (label, forged) =>
      assertEquals(SceneMateThreat.withEngineCheck(forged, supports), None, label)

    val unbound = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(threatMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(500)),
      evalAfter = Some(EngineEval(500)),
      depth = Some(20),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    assertEquals(unbound.storyBound, false)
    assertEquals(unbound.status, EngineCheckStatus.Unknown)
    assertEquals(SceneMateThreat.withEngineCheck(clean, unbound), None)
    assertEquals(SceneMateThreat.withEngineCheck(clean, offRouteEngineCheck(clean)), None)

  test("MateThreat StoryTable orders Lead Support Context Blocked capped and refuted rows without text"):
    val clean = story
    val lead = SceneMateThreat.withEngineCheck(clean, engineCheck(clean, EngineCheckStatus.Supports)).get
    val support = clean.copy(routeSan = Some("Kh6"))
    val quietContext = clean.copy(
      writer = None,
      mateThreatProof = None,
      scene = Scene.Quiet,
      proof = clean.proof.copy(boardProof = 10, lineProof = 10, ownerProof = 10, anchorProof = 10, routeProof = 10)
    )
    val blocked = clean.copy(mateThreatProof = None)
    val capped = SceneMateThreat.withEngineCheck(clean, engineCheck(clean, EngineCheckStatus.Caps)).get
    val refuted = SceneMateThreat.withEngineCheck(clean, engineCheck(clean, EngineCheckStatus.Supports, before = 220, after = 20)).get

    val leadSupportVerdicts = StoryTable.choose(Vector(lead, support))
    val leadVerdict = leadSupportVerdicts.find(_.role == Role.Lead).get
    val supportVerdict = leadSupportVerdicts.find(_.role == Role.Support).get

    assertEquals(leadVerdict.story.scene, Scene.MateThreat)
    assertEquals(supportVerdict.story.scene, Scene.MateThreat)
    assertSilent("Support MateThreat", supportVerdict)

    val contextVerdict = selected(quietContext)
    assertEquals(contextVerdict.role, Role.Context)
    assertSilent("Context row", contextVerdict)

    val blockedVerdict = selected(blocked)
    assertEquals(blockedVerdict.role, Role.Blocked)
    assertSilent("Blocked MateThreat", blockedVerdict)

    val cappedVerdict = selected(capped)
    assertEquals(cappedVerdict.role, Role.Lead)
    assertEquals(cappedVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertSilent("capped MateThreat", cappedVerdict)

    val refutedVerdict = selected(refuted)
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertSilent("refuted MateThreat", refutedVerdict)

  test("MateThreat EngineCheck cannot create or repair proof and raw engine fields stay downstream silent"):
    val clean = story
    val engineOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(threatMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(999)),
      evalAfter = Some(EngineEval(999)),
      depth = Some(30),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val proofless = clean.copy(mateThreatProof = None, engineCheck = Some(engineOnly))
    val proofBefore = clean.mateThreatProof
    val capped = SceneMateThreat.withEngineCheck(clean, engineCheck(clean, EngineCheckStatus.Caps)).get
    val refuted = SceneMateThreat.withEngineCheck(clean, engineCheck(clean, EngineCheckStatus.Supports, before = 220, after = 20)).get

    assertEquals(StoryTable.choose(Vector.empty), Vector.empty)
    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)
    assertEquals(SceneMateThreat.withEngineCheck(proofless, engineOnly), None)
    assertEquals(proofless.mateThreatProof, None)
    assertEquals(capped.mateThreatProof, proofBefore)
    assertEquals(refuted.mateThreatProof, proofBefore)
    Vector("mate score", "raw pv", "depth", "eval").foreach: forbidden =>
      assert(!proofBefore.toString.toLowerCase.contains(forbidden), forbidden)
    assertSilent("engine-only proofless MateThreat", selected(proofless))
    assertSilent("capped raw engine fields", selected(capped))
    assertSilent("refuted raw engine fields", selected(refuted))
