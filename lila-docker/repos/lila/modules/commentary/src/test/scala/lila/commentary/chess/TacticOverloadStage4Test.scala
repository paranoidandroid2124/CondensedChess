package lila.commentary.chess

class TacticOverloadStage4Test extends munit.FunSuite:

  private val overloadFen = "7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1"
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val defender = Square('e', 6)
  private val firstDutyTarget = Square('e', 7)
  private val secondDutyTarget = Square('a', 6)

  test("Stage-4 proof complete plus EngineCheck Support remains bounded to existing Overload claim"):
    val facts = BoardFacts.fromFen(overloadFen).toOption.get
    val story = overloadStory(facts)
    val supported = TacticOverload.withEngineCheck(story, engineCheck(facts, story, EngineCheckStatus.Supports)).get
    val verdict = StoryTable.choose(Vector(supported)).head
    val plan = ExplanationPlan.fromSelected(verdict).get

    assertEquals(supported.engineCheck.map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.OverloadsDefender))
    assertEquals(plan.allowedClaim.map(_.key), Some("overloads_defender"))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.evidenceLine, Some(overloadMove))
    assertEquals(plan.debugOnly, false)

  test("Stage-4 proof complete plus EngineCheck Cap does not render standalone Overload text"):
    val facts = BoardFacts.fromFen(overloadFen).toOption.get
    val story = overloadStory(facts)
    val capped = TacticOverload.withEngineCheck(story, engineCheck(facts, story, EngineCheckStatus.Caps)).get
    val verdict = StoryTable.choose(Vector(capped)).head

    assertEquals(capped.engineCheck.map(_.status), Some(EngineCheckStatus.Caps))
    assertEquals(verdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None)

  test("Stage-4 proof complete plus EngineCheck Refute does not render standalone Overload text"):
    val facts = BoardFacts.fromFen(overloadFen).toOption.get
    val story = overloadStory(facts)
    val refuted = TacticOverload.withEngineCheck(story, engineCheck(facts, story, EngineCheckStatus.Refutes)).get
    val verdict = StoryTable.choose(Vector(refuted)).head

    assertEquals(refuted.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    assertEquals(verdict.role, Role.Blocked)
    assertEquals(verdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None)

  test("Stage-4 EngineCheck Support cannot create Overload without proof"):
    val facts = BoardFacts.fromFen(overloadFen).toOption.get
    val story = overloadStory(facts)
    val support = engineCheck(facts, story, EngineCheckStatus.Supports)
    val proofAbsent = story.copy(overloadProof = None)
    val forged = proofAbsent.copy(engineCheck = Some(support))
    val verdict = StoryTable.choose(Vector(forged)).head

    assertEquals(TacticOverload.withEngineCheck(proofAbsent, support), None)
    assertEquals(verdict.role, Role.Blocked)
    assertEquals(verdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None)

  test("Stage-4 raw PV and eval appear nowhere in renderer or LLM smoke input"):
    val facts = BoardFacts.fromFen(overloadFen).toOption.get
    val story = overloadStory(facts)
    val supported = TacticOverload.withEngineCheck(story, engineCheck(facts, story, EngineCheckStatus.Supports)).get
    val verdict = StoryTable.choose(Vector(supported)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan)
    val prompt = rendered.flatMap(LlmNarrationSmoke.codexCliPrompt(plan, _))
    val publicSurfaces = Vector(plan.toString) ++ rendered.toVector.map(_.toString) ++ prompt.toVector

    assertEquals(plan.allowedClaim.map(_.key), Some("overloads_defender"))
    publicSurfaces.foreach: surface =>
      Vector("EngineEval", "EngineLine", "eval", "PV", "principal variation", "+3.00", "300 cp").foreach: raw =>
        assert(!surface.contains(raw), raw)

  private def overloadStory(facts: BoardFacts): Story =
    TacticOverload
      .write(
        facts,
        Some(overloadMove),
        Some(defender),
        Some(firstDutyTarget),
        Some(secondDutyTarget),
        Some(firstDutyTarget)
      )
      .get

  private def engineCheck(facts: BoardFacts, story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(overloadMove))),
      replyLine = Some(EngineLine(Vector(overloadMove))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(if status == EngineCheckStatus.Supports then 300 else -50)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )
