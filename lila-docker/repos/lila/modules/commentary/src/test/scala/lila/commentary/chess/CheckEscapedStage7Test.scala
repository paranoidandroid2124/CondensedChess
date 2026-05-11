package lila.commentary.chess

class CheckEscapedStage7Test extends munit.FunSuite:

  private val escapeFen = "k3r3/8/8/8/8/8/8/4K3 w - - 0 1"
  private val escapeMove = Line(Square('e', 1), Square('f', 1))
  private val replyMove = Line(Square('a', 8), Square('b', 8))

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def escapeFacts: BoardFacts =
    facts(escapeFen)

  private def story: Story =
    SceneCheckEscaped.write(escapeFacts, escapeMove).get

  private def plan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

  private def engineCheck(row: Story, status: EngineCheckStatus, before: Int = 100, after: Int = 100): EngineCheck =
    EngineCheck.fromStory(
      facts = escapeFacts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(escapeMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  test("Stage-7 DeterministicRenderer phrases only bounded CheckEscaped ExplanationPlan"):
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val forbiddenPhrases = Vector(
      "moves the king out of check",
      "blocks the check",
      "captures the checking piece",
      "gives check",
      "avoids mate",
      "prevents checkmate",
      "the king is safe",
      "refutes the attack",
      "defends everything",
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

    assert(Vector("Kf1 gets out of check.", "Kf1 escapes the check.").contains(rendered.text))
    assertEquals(rendered.claimKey, "escapes_check")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    assertEquals(plan.scene, Scene.CheckEscaped)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.EscapesCheck))
    assert(plan.forbiddenWording.map(_.key).contains("gives_check"))
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), s"CheckEscaped renderer used forbidden wording: $phrase")

  test("Stage-7 DeterministicRenderer rejects non Lead capped refuted malformed and sibling CheckEscaped plans"):
    Vector(
      "support" -> plan.copy(role = Role.Support, allowedClaim = None),
      "context" -> plan.copy(role = Role.Context, allowedClaim = None),
      "blocked" -> plan.copy(role = Role.Blocked, debugOnly = true),
      "missing claim" -> plan.copy(allowedClaim = None),
      "sibling claim" -> plan.copy(allowedClaim = Some(ExplanationClaim.GivesCheck)),
      "wrong scene" -> plan.copy(scene = Scene.CheckGiven),
      "tactic contaminated" -> plan.copy(tactic = Some(Tactic.Fork)),
      "secondary target contaminated" -> plan.copy(secondaryTarget = Some(Square('a', 1))),
      "route missing" -> plan.copy(route = None),
      "route SAN missing" -> plan.copy(routeSan = None),
      "evidence mismatch" -> plan.copy(evidenceLine = Some(Line(Square('e', 1), Square('e', 2)))),
      "unbounded strength by missing forbidden wording" -> plan.copy(forbiddenWording = Vector.empty)
    ).foreach: (label, malformed) =>
      assertEquals(DeterministicRenderer.fromPlan(malformed), None, label)

    val capped = SceneCheckEscaped.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Caps)).get
    val refuted =
      SceneCheckEscaped.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Supports, before = 250, after = 25)).get

    Vector("capped" -> capped, "refuted" -> refuted).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(row)).head).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("Stage-7 CheckEscaped renderer rejects forbidden wording even in otherwise valid plans"):
    Vector(
      "captures the checking piece",
      "gives check",
      "prevents checkmate",
      "defends everything"
    ).foreach: phrase =>
      val contaminated = plan.copy(routeSan = Some(s"Kf1 $phrase"))
      assertEquals(DeterministicRenderer.fromPlan(contaminated), None, phrase)

  test("Stage-7 CheckEscaped renderer does not phrase escape method details"):
    val methodRows = Vector(
      SceneCheckEscaped.write(
        facts("k3r3/8/8/8/8/8/3B4/4K3 w - - 0 1"),
        Line(Square('d', 2), Square('e', 3))
      ).get,
      SceneCheckEscaped.write(
        facts("k7/8/8/8/8/8/4r3/4K3 w - - 0 1"),
        Line(Square('e', 1), Square('e', 2))
      ).get
    )

    methodRows.foreach: row =>
      val text = ExplanationPlan.fromSelected(StoryTable.choose(Vector(row)).head).flatMap(DeterministicRenderer.fromPlan).map(_.text).get
      val normalized = text.toLowerCase
      assert(normalized.endsWith("gets out of check."))
      assert(!normalized.contains("blocks the check"))
      assert(!normalized.contains("captures the checking piece"))
      assert(!normalized.contains("moves the king out of check"))

  test("Stage-7 renderer surface remains ExplanationPlan only"):
    val fromPlanMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    val fromPlanParameterShapes =
      fromPlanMethods.map(method => method.getParameterTypes.toVector.map(_.getSimpleName).toVector).toVector

    assertEquals(fromPlanParameterShapes, Vector(Vector("ExplanationPlan")))
