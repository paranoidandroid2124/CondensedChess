package lila.commentary.chess

class SceneMateThreatWriterTest extends munit.FunSuite:

  private val mateThreatFen = "7k/8/6K1/8/8/8/5Q2/8 w - - 0 1"
  private val threatMove = Line(Square('g', 6), Square('h', 6))
  private val quietMove = Line(Square('g', 6), Square('f', 6))
  private val illegalMove = Line(Square('g', 6), Square('g', 8))
  private val immediateMateMove = Line(Square('f', 2), Square('f', 8))
  private val replyMove = Line(Square('h', 8), Square('g', 8))

  private def facts: BoardFacts =
    BoardFacts.fromFen(mateThreatFen).toOption.get

  private def selected(row: Story): Verdict =
    StoryTable.choose(Vector(row)).head

  private def engineCheck(row: Story, status: EngineCheckStatus, before: Int = 100, after: Int = 100): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(threatMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  test("SceneMateThreat writes one proof-backed Story identity"):
    val story = SceneMateThreat.write(facts, threatMove).get

    assertEquals(story.scene, Scene.MateThreat)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('h', 8)))
    assertEquals(story.anchor, Some(Square('h', 6)))
    assertEquals(story.route, Some(threatMove))
    assertEquals(story.routeSan, Some("Kh6"))
    assertEquals(story.writer, Some(StoryWriter.SceneMateThreat))
    assertEquals(story.storyProof.completeForLine(threatMove), true)
    assertEquals(story.mateThreatProof.exists(_.complete), true)
    assertEquals(story.checkmateProof, None)
    assertEquals(story.checkGivenProof, None)
    assertEquals(story.proofFailures, Vector.empty)

    val verdict = selected(story)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key), Some("threatens_mate_next"))

  test("SceneMateThreat writes only from complete MateThreatProof and complete StoryProof"):
    assertEquals(SceneMateThreat.write(facts, quietMove), None)
    assertEquals(SceneMateThreat.write(facts, illegalMove), None)
    assertEquals(SceneMateThreat.write(facts, immediateMateMove), None)

    val story = SceneMateThreat.write(facts, threatMove).get
    Vector(
      "missing MateThreatProof" -> story.copy(mateThreatProof = None),
      "incomplete MateThreatProof" -> story.copy(mateThreatProof = story.mateThreatProof.map(_.copy(legalMove = false))),
      "missing StoryProof" -> story.copy(storyProof = StoryProof.empty),
      "untrusted StoryProof" -> story.copy(storyProof = StoryProof.untrustedLegalLine(threatMove))
    ).foreach: (label, forged) =>
      val verdict = selected(forged)
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("SceneMateThreat rejects forged side rival target anchor and route identity"):
    val story = SceneMateThreat.write(facts, threatMove).get
    Vector(
      "side" -> story.copy(side = Side.Black),
      "rival" -> story.copy(rival = Side.White),
      "target" -> story.copy(target = Some(Square('g', 8))),
      "anchor" -> story.copy(anchor = Some(Square('g', 6))),
      "route" -> story.copy(route = Some(Line(Square('g', 6), Square('g', 7)))),
      "scene" -> story.copy(scene = Scene.Checkmate),
      "tactic" -> story.copy(tactic = Some(Tactic.MateNet)),
      "plan" -> story.copy(plan = Some(Plan.Initiative)),
      "writer" -> story.copy(writer = None)
    ).foreach: (label, forged) =>
      val verdict = selected(forged)
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("SceneMateThreat accepts bounded and refuting EngineCheck"):
    val story = SceneMateThreat.write(facts, threatMove).get
    val supports = SceneMateThreat.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Supports)).get
    val caps = SceneMateThreat.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Caps)).get
    val refutes = engineCheck(story, EngineCheckStatus.Supports, before = 220, after = 20)

    assertEquals(selected(supports).role, Role.Lead)
    assertEquals(selected(supports).engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(selected(caps).role, Role.Lead)
    assertEquals(selected(caps).engineStrengthLimited, true)
    val refuted = SceneMateThreat.withEngineCheck(story, refutes).get
    assertEquals(refuted.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    assertEquals(selected(refuted).role, Role.Blocked)
