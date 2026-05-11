package lila.commentary.chess

import java.nio.file.{ Files, Paths }

class StalemateStage7Test extends munit.FunSuite:

  private val stalemateFen = "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1"
  private val stalemateMove = Line(Square('g', 5), Square('g', 6))
  private val replyMove = Line(Square('h', 8), Square('h', 7))

  private def facts: BoardFacts =
    BoardFacts.fromFen(stalemateFen).toOption.get

  private def story: Story =
    SceneStalemate.write(facts, stalemateMove).get

  private def plan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

  private def engineCheck(row: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(stalemateMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  test("Stage-7 DeterministicRenderer phrases only bounded Stalemate ExplanationPlan"):
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val allowedTemplates = Vector("Qg6 is stalemate.", "Qg6 stalemates the king.")
    val forbiddenPhrases = Vector(
      "draws the game",
      "saves the game",
      "throws away the win",
      "blunder",
      "tablebase draw",
      "engine says",
      "best move",
      "only move",
      "forced",
      "winning",
      "losing",
      "decisive",
      "no counterplay"
    )

    assert(allowedTemplates.contains(rendered.text))
    assertEquals(rendered.claimKey, "stalemates")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    assertEquals(plan.scene, Scene.Stalemate)
    assertEquals(plan.tactic, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.Stalemates))
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), s"Stalemate renderer used forbidden wording: $phrase")

  test("Stage-7 DeterministicRenderer rejects non Lead capped refuted malformed and sibling Stalemate plans"):
    Vector(
      "support" -> plan.copy(role = Role.Support, allowedClaim = None),
      "context" -> plan.copy(role = Role.Context, allowedClaim = None),
      "blocked" -> plan.copy(role = Role.Blocked, debugOnly = true),
      "missing claim" -> plan.copy(allowedClaim = None),
      "sibling checkmate claim" -> plan.copy(allowedClaim = Some(ExplanationClaim.Checkmates)),
      "sibling gives-check claim" -> plan.copy(allowedClaim = Some(ExplanationClaim.GivesCheck)),
      "sibling escapes-check claim" -> plan.copy(allowedClaim = Some(ExplanationClaim.EscapesCheck)),
      "wrong scene" -> plan.copy(scene = Scene.Checkmate),
      "tactic contaminated" -> plan.copy(tactic = Some(Tactic.Fork)),
      "secondary target contaminated" -> plan.copy(secondaryTarget = Some(Square('a', 1))),
      "target missing" -> plan.copy(target = None),
      "anchor missing" -> plan.copy(anchor = None),
      "route missing" -> plan.copy(route = None),
      "route SAN missing" -> plan.copy(routeSan = None),
      "evidence mismatch" -> plan.copy(evidenceLine = Some(Line(Square('g', 5), Square('h', 5)))),
      "unbounded strength by missing forbidden wording" -> plan.copy(forbiddenWording = Vector.empty)
    ).foreach: (label, malformed) =>
      assertEquals(DeterministicRenderer.fromPlan(malformed), None, label)

    val capped = story.copy(engineCheck = Some(engineCheck(story, EngineCheckStatus.Caps)))
    val refuted = story.copy(engineCheck = Some(engineCheck(story, EngineCheckStatus.Refutes)))

    Vector("capped" -> capped, "refuted" -> refuted).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(row)).head).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("Stage-7 Stalemate renderer rejects forbidden wording even in otherwise valid plans"):
    Vector(
      "draws the game",
      "saves the game",
      "throws away the win",
      "blunder",
      "tablebase draw",
      "engine says draw",
      "best move",
      "only move",
      "forced",
      "winning",
      "losing",
      "decisive",
      "no counterplay"
    ).foreach: phrase =>
      val contaminated = plan.copy(routeSan = Some(s"Qg6 $phrase"))
      assertEquals(DeterministicRenderer.fromPlan(contaminated), None, phrase)

  test("Stage-7 renderer surface remains ExplanationPlan only"):
    val fromPlanMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    val fromPlanParameterShapes =
      fromPlanMethods.map(method => method.getParameterTypes.toVector.map(_.getSimpleName).toVector).toVector
    val rendererMethodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet

    assertEquals(fromPlanParameterShapes, Vector(Vector("ExplanationPlan")))
    Vector(
      "fromStory",
      "fromVerdict",
      "fromStalemateProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromPv",
      "fromProofFailures",
      "fromSourceRows",
      "fromResultNotation",
      "fromTablebase"
    ).foreach: forbiddenMethod =>
      assert(!rendererMethodNames.contains(forbiddenMethod), s"DeterministicRenderer must not expose $forbiddenMethod")

  test("Stage-7 Stalemate renderer authority lives in StoryInteractionLaw"):
    val law = Files.readString(Paths.get("modules/commentary/docs/StoryInteractionLaw.md"))
    assert(law.contains("### Stalemate-7 DeterministicRenderer"))
    assert(law.contains("`DeterministicRenderer` input is `ExplanationPlan` only."))
    assert(law.contains("`{route} is stalemate.`"))
    assert(law.contains("`{route} stalemates the king.`"))
