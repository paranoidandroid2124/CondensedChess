package lila.commentary.chess

class TacticOverloadStage2Test extends munit.FunSuite:

  private val overloadFen = "7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1"
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val illegalMove = Line(Square('g', 3), Square('g', 8))
  private val defender = Square('e', 6)
  private val firstDutyTarget = Square('e', 7)
  private val secondDutyTarget = Square('a', 6)

  test("Stage-2 TacticOverload creates one public Overload row from complete proof and non-refuting EngineCheck"):
    val facts = BoardFacts.fromFen(overloadFen).toOption.get
    val story = overloadStory(facts).get
    val supported = TacticOverload.withEngineCheck(story, engineCheck(facts, story, EngineCheckStatus.Supports)).get
    val verdict = StoryTable.choose(Vector(supported)).head
    val plan = ExplanationPlan.fromSelected(verdict).get

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Overload))
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(firstDutyTarget))
    assertEquals(story.secondaryTarget, Some(secondDutyTarget))
    assertEquals(story.anchor, Some(defender))
    assertEquals(story.route, Some(overloadMove))
    assertEquals(story.writer, Some(StoryWriter.TacticOverload))
    assertEquals(story.overloadProof.exists(_.complete), true)
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(supported.engineCheck.map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.OverloadsDefender))
    assertEquals(plan.allowedClaim.map(_.key), Some("overloads_defender"))
    assertEquals(plan.evidenceLine, Some(overloadMove))
    assertEquals(plan.debugOnly, false)

  test("Stage-2 EngineCheck Refutes blocks Overload public rendering under existing cap/refute model"):
    val facts = BoardFacts.fromFen(overloadFen).toOption.get
    val story = overloadStory(facts).get
    val refuted = TacticOverload.withEngineCheck(story, engineCheck(facts, story, EngineCheckStatus.Refutes)).get
    val verdict = StoryTable.choose(Vector(refuted)).head

    assertEquals(refuted.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    assertEquals(verdict.role, Role.Blocked)
    assertEquals(verdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("Stage-2 incomplete OverloadProof creates no Story"):
    val facts = BoardFacts.fromFen(overloadFen).toOption.get
    assertEquals(
      TacticOverload.write(
        facts,
        Some(illegalMove),
        Some(defender),
        Some(firstDutyTarget),
        Some(secondDutyTarget),
        Some(firstDutyTarget)
      ),
      None
    )

  test("Stage-2 TacticOverload emits no sibling Story labels or public raw proof text"):
    val facts = BoardFacts.fromFen(overloadFen).toOption.get
    val story = overloadStory(facts).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic.exists(tactic => Set(Tactic.Deflect, Tactic.Decoy, Tactic.RemoveGuard, Tactic.Hanging).contains(tactic)), false)
    assertEquals(story.tactic, Some(Tactic.Overload))
    assertEquals(story.captureResult, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.multiTargetProof, None)
    assert(!plan.forbiddenWording.map(_.key).contains("overloads_defender"), "allowed overload key must not be forbidden")
    Vector("wins_material", "forced_move", "best_move", "only_move").foreach: forbidden =>
      assert(plan.forbiddenWording.map(_.key).contains(forbidden), forbidden)
    Vector("proofFailures", "OverloadProof", "CannotSatisfyBoth").foreach: raw =>
      assert(!plan.toString.contains(raw), raw)

  private def overloadStory(facts: BoardFacts): Option[Story] =
    TacticOverload.write(
      facts,
      Some(overloadMove),
      Some(defender),
      Some(firstDutyTarget),
      Some(secondDutyTarget),
      Some(firstDutyTarget)
    )

  private def engineCheck(facts: BoardFacts, story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(overloadMove))),
      replyLine = Some(EngineLine(Vector(overloadMove))),
      evalBefore = Some(EngineEval(if status == EngineCheckStatus.Refutes then 200 else 0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = status
    )
