package lila.commentary.chess

class CheckmateStage7Test extends munit.FunSuite:

  private val mateFen = "k7/2Q5/2K5/8/8/8/8/8 w - - 0 1"
  private val mateMove = Line(Square('c', 7), Square('b', 7))
  private val replyMove = Line(Square('a', 8), Square('a', 7))

  private def facts: BoardFacts =
    BoardFacts.fromFen(mateFen).toOption.get

  private def story: Story =
    SceneCheckmate.write(facts, mateMove).get

  private def plan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

  private def engineCheck(row: Story, status: EngineCheckStatus, before: Int = 0, after: Int = 0): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(mateMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  test("Stage-7 DeterministicRenderer phrases only bounded Checkmate ExplanationPlan"):
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val forbiddenPhrases = Vector(
      "threatens mate",
      "mate in one",
      "mate in n",
      "forced mate",
      "best move",
      "only move",
      "winning",
      "decisive",
      "no counterplay",
      "the king is unsafe",
      "starts an attack",
      "creates pressure",
      "takes the initiative",
      "engine says",
      "mate score"
    )

    assert(Vector("Qb7# is checkmate.", "Qb7# checkmates the king.").contains(rendered.text))
    assertEquals(rendered.claimKey, "checkmates")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    assertEquals(plan.scene, Scene.Checkmate)
    assertEquals(plan.tactic, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.Checkmates))
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), s"Checkmate renderer used forbidden wording: $phrase")

  test("Stage-7 DeterministicRenderer rejects non Lead capped refuted malformed and sibling Checkmate plans"):
    Vector(
      "support" -> plan.copy(role = Role.Support, allowedClaim = None),
      "context" -> plan.copy(role = Role.Context, allowedClaim = None),
      "blocked" -> plan.copy(role = Role.Blocked, debugOnly = true),
      "missing claim" -> plan.copy(allowedClaim = None),
      "sibling gives-check claim" -> plan.copy(allowedClaim = Some(ExplanationClaim.GivesCheck)),
      "sibling escapes-check claim" -> plan.copy(allowedClaim = Some(ExplanationClaim.EscapesCheck)),
      "wrong scene" -> plan.copy(scene = Scene.CheckGiven),
      "tactic contaminated" -> plan.copy(tactic = Some(Tactic.Fork)),
      "secondary target contaminated" -> plan.copy(secondaryTarget = Some(Square('a', 1))),
      "target missing" -> plan.copy(target = None),
      "anchor missing" -> plan.copy(anchor = None),
      "route missing" -> plan.copy(route = None),
      "route SAN missing" -> plan.copy(routeSan = None),
      "evidence mismatch" -> plan.copy(evidenceLine = Some(Line(Square('c', 7), Square('c', 8)))),
      "unbounded strength by missing forbidden wording" -> plan.copy(forbiddenWording = Vector.empty)
    ).foreach: (label, malformed) =>
      assertEquals(DeterministicRenderer.fromPlan(malformed), None, label)

    val capped = story.copy(engineCheck = Some(engineCheck(story, EngineCheckStatus.Caps)))
    val refuted = story.copy(engineCheck = Some(engineCheck(story, EngineCheckStatus.Supports, before = 250, after = 0)))

    Vector("capped" -> capped, "refuted" -> refuted).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(row)).head).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("Stage-7 Checkmate renderer rejects forbidden wording even in otherwise valid plans"):
    Vector(
      "threatens mate",
      "mate in one",
      "forced mate",
      "best move",
      "only move",
      "engine says mate",
      "mate score"
    ).foreach: phrase =>
      val contaminated = plan.copy(routeSan = Some(s"Qb7# $phrase"))
      assertEquals(DeterministicRenderer.fromPlan(contaminated), None, phrase)

  test("Stage-7 renderer surface remains ExplanationPlan only"):
    val fromPlanMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    val fromPlanParameterShapes =
      fromPlanMethods.map(method => method.getParameterTypes.toVector.map(_.getSimpleName).toVector).toVector

    assertEquals(fromPlanParameterShapes, Vector(Vector("ExplanationPlan")))
