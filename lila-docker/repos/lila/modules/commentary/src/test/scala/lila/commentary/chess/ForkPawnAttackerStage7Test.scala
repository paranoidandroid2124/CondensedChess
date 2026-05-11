package lila.commentary.chess

class ForkPawnAttackerStage7Test extends munit.FunSuite:

  private val forkFen = "7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1"
  private val forkFacts = facts(forkFen)
  private val forkRoute = Line(Square('e', 4), Square('e', 5))
  private val replyRoute = Line(Square('h', 8), Square('h', 7))
  private val targetA = Square('d', 6)
  private val targetB = Square('f', 6)

  test("Stage-7 DeterministicRenderer renders pawn-attacker Fork through existing Fork plan only"):
    val plan = forkPlan
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.Fork))
    assertEquals(plan.allowedClaim.map(_.key), Some("forks_two_targets"))
    assertEquals(plan.evidenceLine, Some(forkRoute))
    assertEquals(rendered.text, "e5 forks the pieces on d6 and f6.")
    assertEquals(rendered.claimKey, "forks_two_targets")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)

  test("Stage-7 Fork renderer emits no PawnFork material promotion or tempo wording"):
    val rendered = DeterministicRenderer.fromPlan(forkPlan).get
    val publicText = rendered.text.toLowerCase
    val forbiddenPhrases = Vector(
      "pawn fork",
      "wins material",
      "wins piece",
      "wins a piece",
      "promotes",
      "promotion",
      "tempo",
      "best move",
      "only move",
      "forced",
      "decisive",
      "winning",
      "engine says",
      "raw pv"
    )

    forbiddenPhrases.foreach: phrase =>
      assert(!publicText.contains(phrase), s"Fork renderer used forbidden wording: $phrase")
    assertEquals(rendered.claimKey, "forks_two_targets")

  test("Stage-7 Fork renderer rejects non Lead capped refuted malformed and PawnFork-shaped plans"):
    val plan = forkPlan
    Vector(
      "support" -> plan.copy(role = Role.Support, allowedClaim = None),
      "context" -> plan.copy(role = Role.Context, allowedClaim = None),
      "blocked" -> plan.copy(role = Role.Blocked, debugOnly = true),
      "missing claim" -> plan.copy(allowedClaim = None),
      "sibling claim" -> plan.copy(allowedClaim = Some(ExplanationClaim.AttacksTwoTargets)),
      "pawn fork tactic" -> plan.copy(tactic = Some(Tactic.PawnFork)),
      "material scene" -> plan.copy(scene = Scene.Material, tactic = None),
      "promotion scene" -> plan.copy(scene = Scene.Promotion, tactic = None),
      "missing secondary target" -> plan.copy(secondaryTarget = None),
      "missing target" -> plan.copy(target = None),
      "missing route" -> plan.copy(route = None),
      "missing SAN" -> plan.copy(routeSan = None),
      "evidence mismatch" -> plan.copy(evidenceLine = Some(Line(Square('e', 4), Square('e', 6)))),
      "unbounded by missing forbidden wording" -> plan.copy(forbiddenWording = Vector.empty)
    ).foreach: (label, malformed) =>
      assertEquals(DeterministicRenderer.fromPlan(malformed), None, label)

    val strongFork = nonPawnFork
    val support = StoryTable
      .choose(Vector(strongFork, forkStory))
      .find(_.story.route.contains(forkRoute))
      .get
    val context = StoryTable.choose(Vector(forkStory.copy(proof = lowProof))).head
    val blocked = StoryTable.choose(Vector(forkStory.copy(writer = None))).head
    val capped =
      StoryTable.choose(Vector(TacticFork.withEngineCheck(forkStory, check(forkStory, EngineCheckStatus.Caps)).get)).head
    val refuted =
      StoryTable
        .choose(Vector(TacticFork.withEngineCheck(forkStory, check(forkStory, EngineCheckStatus.Supports, before = 220, after = 20)).get))
        .head

    Vector("support" -> support, "context" -> context, "blocked" -> blocked, "capped" -> capped, "refuted" -> refuted)
      .foreach: (label, verdict) =>
        assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("Stage-7 Fork renderer surface remains ExplanationPlan only"):
    val fromPlanMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    val fromPlanParameterShapes =
      fromPlanMethods.map(method => method.getParameterTypes.toVector.map(_.getSimpleName).toVector).toVector
    val rendererMethodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet

    assertEquals(fromPlanParameterShapes, Vector(Vector("ExplanationPlan")))
    Vector("Story", "MultiTargetProof", "BoardFacts", "EngineCheck", "EngineLine", "Proof").foreach: forbiddenInput =>
      assert(!fromPlanParameterShapes.flatten.contains(forbiddenInput), s"renderer must not accept raw $forbiddenInput")
    Vector("fromStory", "fromMultiTargetProof", "fromBoardFacts", "fromEngineCheck", "fromProofFailures", "fromSourceRow")
      .foreach: forbiddenMethod =>
        assert(!rendererMethodNames.contains(forbiddenMethod), s"renderer must not expose $forbiddenMethod")

  test("Stage-7 Fork renderer authority lives in StoryInteractionLaw"):
    val law = scala.io.Source
      .fromFile("modules/commentary/docs/StoryInteractionLaw.md")
      .getLines()
      .mkString("\n")

    assert(law.contains("## Stage-7 DeterministicRenderer"))
    assert(law.contains("existing Fork renderer path may render pawn-attacker Fork"))
    assert(law.contains("Renderer input:"))
    assert(law.contains("- `ExplanationPlan` only"))
    assert(law.contains("Do not add:"))
    assert(law.contains("- PawnFork renderer template"))
    assert(law.contains("Forbidden renderer input:"))
    assert(law.contains("- raw `Story`"))
    assert(law.contains("- raw `MultiTargetProof`"))
    assert(law.contains("- `proofFailures`"))
    assert(law.contains("Renderer emits no text for Support, Context, Blocked, capped, or refuted rows."))
    assert(law.contains("Renderer emits no PawnFork text."))

  private def forkPlan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(forkStory)).head).get

  private def forkStory: Story =
    TacticFork.write(forkFacts, Some(forkRoute), Some(targetA), Some(targetB)).get

  private def nonPawnFork: Story =
    val forkFacts = facts("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1")
    val forkMove = Line(Square('f', 3), Square('d', 4))
    TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get

  private def check(
      story: Story,
      status: EngineCheckStatus,
      before: Int = 20,
      after: Int = 20
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = forkFacts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(forkRoute))),
      replyLine = Some(EngineLine(Vector(replyRoute))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-7 FEN: $fen -> $error"), identity)

  private val lowProof: Proof =
    forkStory.proof.copy(boardProof = 60, lineProof = 60, ownerProof = 60, anchorProof = 60, routeProof = 60)
