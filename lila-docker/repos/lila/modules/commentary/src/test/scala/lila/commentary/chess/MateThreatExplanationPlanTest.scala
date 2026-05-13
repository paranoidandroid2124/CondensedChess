package lila.commentary.chess

class MateThreatExplanationPlanTest extends munit.FunSuite:

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

  private def verdict(row: Story): Verdict =
    StoryTable.choose(Vector(row)).head

  test("MateThreat ExplanationPlan admits only selected uncapped Lead"):
    val clean = story
    val supported = SceneMateThreat.withEngineCheck(clean, engineCheck(clean, EngineCheckStatus.Supports)).get
    val capped = SceneMateThreat.withEngineCheck(clean, engineCheck(clean, EngineCheckStatus.Caps)).get
    val refuted =
      SceneMateThreat.withEngineCheck(clean, engineCheck(clean, EngineCheckStatus.Supports, before = 220, after = 20)).get
    val supportVerdict = StoryTable.choose(Vector(clean, supported)).find(_.role == Role.Support).get
    val contextVerdict =
      Verdict(story = clean, rank = 2, leadAllowed = false, strength = 0, role = Role.Context, selected = true)
    val blocked = clean.copy(mateThreatProof = None)

    val plan = ExplanationPlan.fromSelected(verdict(clean)).get
    assertEquals(plan.scene, Scene.MateThreat)
    assertEquals(plan.tactic, None)
    assertEquals(plan.side, Side.White)
    assertEquals(plan.rival, Side.Black)
    assertEquals(plan.target, Some(Square('h', 8)))
    assertEquals(plan.anchor, Some(Square('h', 6)))
    assertEquals(plan.route, Some(threatMove))
    assertEquals(plan.routeSan, Some("Kh6"))
    assertEquals(plan.allowedClaim.map(_.key), Some("threatens_mate_next"))
    assertEquals(plan.evidenceLine, Some(threatMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.debugOnly, false)

    assertEquals(ExplanationPlan.fromSelected(verdict(supported)).flatMap(_.allowedClaim).map(_.key), Some("threatens_mate_next"))
    assertEquals(ExplanationPlan.fromSelected(supportVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(contextVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(verdict(blocked)), None)
    assertEquals(ExplanationPlan.fromSelected(verdict(capped)), None)
    assertEquals(ExplanationPlan.fromSelected(verdict(refuted)), None)

  test("MateThreat ExplanationPlan keeps mate move and engine evidence proof-only"):
    val plan = ExplanationPlan.fromSelected(verdict(story)).get
    val planText = plan.toString.toLowerCase

    assert(!planText.contains("qf8"), "next mate SAN must not enter ExplanationPlan")
    assert(!planText.contains("mate score"), "engine mate score wording must not enter ExplanationPlan")
    assert(!planText.contains("raw pv"), "raw PV wording must not enter ExplanationPlan")
    assert(!planText.contains("engine says"), "engine wording must not enter ExplanationPlan")

    Vector(
      ForbiddenWording.MateInN,
      ForbiddenWording.ForcedMate,
      ForbiddenWording.Forced,
      ForbiddenWording.Unavoidable,
      ForbiddenWording.OnlyDefense,
      ForbiddenWording.NoDefense,
      ForbiddenWording.Winning,
      ForbiddenWording.Attack,
      ForbiddenWording.BestMove,
      ForbiddenWording.EngineSays,
      ForbiddenWording.EngineSaysMate,
      ForbiddenWording.RawPv
    ).foreach: forbidden =>
      assert(plan.forbiddenWording.contains(forbidden), s"MateThreat plan must forbid ${forbidden.key}")

    assert(!plan.forbiddenWording.contains(ForbiddenWording.MateThreat), "allowed MateThreat wording must remain phraseable")
