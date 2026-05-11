package lila.commentary.chess

class CheckGivenStage7Test extends munit.FunSuite:

  private val checkingFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkingMove = Line(Square('d', 2), Square('e', 2))
  private val replyMove = Line(Square('e', 8), Square('f', 8))

  private def facts: BoardFacts =
    BoardFacts.fromFen(checkingFen).toOption.get

  private def story: Story =
    SceneCheckGiven.write(facts, checkingMove).get

  private def plan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

  private def engineCheck(row: Story, status: EngineCheckStatus, before: Int = 100, after: Int = 100): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(checkingMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  test("Stage-7 DeterministicRenderer phrases only bounded CheckGiven ExplanationPlan"):
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val forbiddenPhrases = Vector(
      "threatens mate",
      "checkmate",
      "mates",
      "the king is unsafe",
      "starts an attack",
      "creates pressure",
      "takes the initiative",
      "forces",
      "best move",
      "only move",
      "winning",
      "decisive",
      "no counterplay",
      "engine says"
    )

    assert(Vector("Re2+ gives check.", "Re2+ checks the king.").contains(rendered.text))
    assertEquals(rendered.claimKey, "gives_check")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    assertEquals(plan.scene, Scene.CheckGiven)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.GivesCheck))
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), s"CheckGiven renderer used forbidden wording: $phrase")

  test("Stage-7 DeterministicRenderer rejects non Lead capped refuted malformed and sibling CheckGiven plans"):
    Vector(
      "support" -> plan.copy(role = Role.Support, allowedClaim = None),
      "context" -> plan.copy(role = Role.Context, allowedClaim = None),
      "blocked" -> plan.copy(role = Role.Blocked, debugOnly = true),
      "missing claim" -> plan.copy(allowedClaim = None),
      "sibling claim" -> plan.copy(allowedClaim = Some(ExplanationClaim.BlocksPawn)),
      "wrong scene" -> plan.copy(scene = Scene.Material),
      "tactic contaminated" -> plan.copy(tactic = Some(Tactic.Fork)),
      "secondary target contaminated" -> plan.copy(secondaryTarget = Some(Square('a', 1))),
      "route missing" -> plan.copy(route = None),
      "route SAN missing" -> plan.copy(routeSan = None),
      "evidence mismatch" -> plan.copy(evidenceLine = Some(Line(Square('d', 2), Square('d', 8)))),
      "unbounded strength by missing forbidden wording" -> plan.copy(forbiddenWording = Vector.empty)
    ).foreach: (label, malformed) =>
      assertEquals(DeterministicRenderer.fromPlan(malformed), None, label)

    val capped = SceneCheckGiven.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Caps)).get
    val refuted =
      SceneCheckGiven.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Supports, before = 250, after = 25)).get

    Vector("capped" -> capped, "refuted" -> refuted).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(row)).head).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("Stage-7 renderer surface remains ExplanationPlan only"):
    val fromPlanMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    val fromPlanParameterShapes =
      fromPlanMethods.map(method => method.getParameterTypes.toVector.map(_.getSimpleName).toVector).toVector

    assertEquals(fromPlanParameterShapes, Vector(Vector("ExplanationPlan")))
