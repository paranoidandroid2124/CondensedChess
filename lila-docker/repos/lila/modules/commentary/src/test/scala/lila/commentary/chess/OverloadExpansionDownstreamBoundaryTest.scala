package lila.commentary.chess

import java.nio.file.{ Files, Paths }

class OverloadExpansionDownstreamBoundaryTest extends munit.FunSuite:

  private val overloadFen = "7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1"
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val defender = Square('e', 6)
  private val firstDutyTarget = Square('e', 7)
  private val secondDutyTarget = Square('a', 6)

  test("Stage-5 selected uncapped Lead Overload is the only downstream speaking path"):
    val verdict = StoryTable.choose(Vector(admittedOverloadStory)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.selected, true)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes), false)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.OverloadsDefender))
    assertEquals(plan.allowedClaim.map(_.key), Some("overloads_defender"))
    assertEquals(rendered.claimKey, "overloads_defender")
    assertEquals(rendered.text, "This move overloads the defender.")
    assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text).accepted, true)
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

    Vector("renderedText:", "claimKey:", "strength:", "forbiddenWording:").foreach: field =>
      assert(prompt.contains(field), field)
    assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."))
    assertNoRawInput(prompt, "prompt")

  test("Stage-5 support context blocked capped and refuted Overload rows produce no downstream text"):
    val facts = board(overloadFen)
    val overload = admittedOverloadStory
    val support = StoryTable.choose(Vector(overload, overload)).find(_.role == Role.Support).get
    val context = StoryTable.choose(Vector(overload.copy(proof = lowProof))).head
    val blocked = StoryTable.choose(Vector(overload.copy(overloadProof = None))).head
    val capped =
      StoryTable.choose(Vector(TacticOverload.withEngineCheck(overload, engineCheck(facts, overload, EngineCheckStatus.Caps)).get)).head
    val refuted =
      StoryTable.choose(Vector(TacticOverload.withEngineCheck(overload, engineCheck(facts, overload, EngineCheckStatus.Refutes)).get)).head

    assertEquals(support.role, Role.Support)
    assertEquals(context.role, Role.Context)
    assertEquals(blocked.role, Role.Blocked)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(refuted.role, Role.Blocked)

    Vector(support, context, blocked, capped, refuted).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, verdict.toString)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, verdict.toString)

  test("Stage-5 downstream layers reject duplicate-owned neighbor wording"):
    val verdict = StoryTable.choose(Vector(admittedOverloadStory)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    duplicateOwnedOutputs.foreach: output =>
      val check = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(check.accepted, false, output)

  test("Stage-5 renderer and LLM source contracts keep downstream inputs narrow"):
    val renderer = Files.readString(Paths.get("modules/commentary/src/main/scala/lila/commentary/chess/DeterministicRenderer.scala"))
    val smoke = Files.readString(Paths.get("modules/commentary/src/main/scala/lila/commentary/chess/LlmNarrationSmoke.scala"))

    assert(renderer.contains("def fromPlan(plan: ExplanationPlan): Option[RenderedLine]"))
    assert(!renderer.contains("def fromVerdict"))
    assert(!renderer.contains("def fromStory"))
    assert(smoke.contains("def codexCliPrompt(plan: ExplanationPlan, rendered: RenderedLine): Option[String]"))
    assert(smoke.contains("renderedText: ${rendered.text}"))
    assert(smoke.contains("claimKey: ${rendered.claimKey}"))
    assert(smoke.contains("strength: ${rendered.strength}"))
    assert(smoke.contains("forbiddenWording: ${promptForbiddenLabels(plan).mkString"))
    assert(smoke.contains("instruction: Rephrase only. Do not add chess facts."))

  private val duplicateOwnedOutputs =
    Vector(
      "This move overloads the defender and creates a material gain.",
      "This move overloads the defender and wins.",
      "This move overloads the defender and makes the target loose.",
      "This move overloads the defender and leaves a hanging target.",
      "This move overloads the defender because the queen is attacked.",
      "This move overloads the defender and removes defender coverage.",
      "This move overloads the defender because the defender cannot satisfy both duties.",
      "The engine says this move overloads the defender.",
      "This is the best overload.",
      "This is a forced overload.",
      "This is a decisive overload.",
      "This move overloads the defender with no counterplay."
    )

  private def assertNoRawInput(text: String, label: String): Unit =
    val normalized = text.toLowerCase(java.util.Locale.ROOT)
    Vector(
      overloadFen.toLowerCase(java.util.Locale.ROOT),
      "boardfacts",
      "overloadproof",
      "enginecheck",
      "raw pv",
      "evalbefore",
      "evalafter",
      "defenderduty",
      "dualdefenderduty",
      "overloadtest",
      "cannotsatisfyboth"
    ).foreach: forbidden =>
      assert(!normalized.contains(forbidden), s"$label leaked $forbidden")

  private def admittedOverloadStory: Story =
    TacticOverload
      .write(
        board(overloadFen),
        Some(overloadMove),
        Some(defender),
        Some(firstDutyTarget),
        Some(secondDutyTarget),
        Some(firstDutyTarget)
      )
      .get
      .copy(proof = orderingProof)

  private def engineCheck(
      facts: BoardFacts,
      story: Story,
      status: EngineCheckStatus
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(overloadMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('g', 8))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(if status == EngineCheckStatus.Refutes then 0 else 20)),
      depth = Some(20),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-5 FEN: $fen -> $error"), identity)

  private def orderingProof: Proof =
    Proof(
      boardProof = 99,
      lineProof = 99,
      ownerProof = 99,
      anchorProof = 99,
      routeProof = 99,
      persistence = 99,
      immediacy = 99,
      forcing = 99,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = 99,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 99
    )

  private def lowProof: Proof =
    orderingProof.copy(boardProof = 60, lineProof = 60, ownerProof = 60, anchorProof = 60, routeProof = 60)
