package lila.commentary.chess

class ForkPawnAttackerStage8Test extends munit.FunSuite:

  private val forkFen = "7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1"
  private val forkFacts = facts(forkFen)
  private val forkRoute = Line(Square('e', 4), Square('e', 5))
  private val targetA = Square('d', 6)
  private val targetB = Square('f', 6)

  test("Stage-8 Fork LLM smoke accepts only existing Fork rendered contract"):
    val prompt = LlmNarrationSmoke.codexCliPrompt(forkPlan, forkRendered).get
    val allowedPromptFields = Vector(
      "renderedText: e5 forks the pieces on d6 and f6.",
      "claimKey: forks_two_targets",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    )

    allowedPromptFields.foreach: field =>
      assert(prompt.contains(field), s"Fork smoke prompt missing allowed field: $field")
    Vector(
      "raw Story",
      "MultiTargetProof",
      "BoardFacts",
      "EngineCheck",
      "raw PV",
      "proofFailures",
      "source rows"
    ).foreach: raw =>
      assert(!prompt.contains(raw), s"Fork smoke prompt leaked raw input: $raw")
    assertEquals(forkRendered.claimKey, "forks_two_targets")
    assertEquals(LlmNarrationSmoke.mockNarrate(forkPlan, forkRendered), Some("e5 forks the pieces on d6 and f6."))

  test("Stage-8 Fork LLM smoke rejects pawn fork material promotion tempo and engine overclaims"):
    val forbiddenOutputs = Vector(
      "e5 is a pawn fork on d6 and f6.",
      "e5 forks the pieces on d6 and f6 and wins material.",
      "e5 forks the pieces on d6 and f6 and wins a piece.",
      "e5 forks the pieces on d6 and f6 and creates a promotion threat.",
      "e5 advances the pawn and forks d6 and f6.",
      "e5 gains tempo with the fork.",
      "e5 is the best move.",
      "e5 is the only move.",
      "e5 is a forced move.",
      "e5 is decisive.",
      "e5 is winning.",
      "The engine line says e5 forks d6 and f6.",
      "e5 forks d6 and f6, then h7 is the new variation.",
      "e5 forks d6 and f6; next Nf7 wins."
    )

    forbiddenOutputs.foreach: output =>
      assertEquals(LlmNarrationSmoke.check(forkPlan, forkRendered, output).accepted, false, output)

  test("Stage-8 Fork LLM smoke rejects non Fork claim keys and mismatched rendered lines"):
    val wrongClaim =
      forkPlan.copy(allowedClaim = Some(ExplanationClaim.AttacksTwoTargets))
    val pawnForkTactic =
      forkPlan.copy(tactic = Some(Tactic.PawnFork))
    val wrongRendered =
      forkRendered.copy(claimKey = "pawn_forks_two_targets")
    val noClaimRendered =
      forkRendered.copy(claimKey = "forks_two_targets", forbiddenCheckPassed = false)

    Vector(
      "wrong claim" -> wrongClaim,
      "pawn fork tactic" -> pawnForkTactic,
      "support" -> forkPlan.copy(role = Role.Support, allowedClaim = None),
      "context" -> forkPlan.copy(role = Role.Context, allowedClaim = None),
      "blocked" -> forkPlan.copy(role = Role.Blocked, debugOnly = true),
      "route missing" -> forkPlan.copy(route = None),
      "evidence mismatch" -> forkPlan.copy(evidenceLine = Some(Line(Square('e', 4), Square('e', 6))))
    ).foreach: (label, malformed) =>
      assertEquals(LlmNarrationSmoke.mockNarrate(malformed, forkRendered), None, label)
      assertEquals(LlmNarrationSmoke.codexCliPrompt(malformed, forkRendered), None, label)

    assertEquals(LlmNarrationSmoke.mockNarrate(forkPlan, wrongRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(forkPlan, wrongRendered), None)
    assertEquals(LlmNarrationSmoke.mockNarrate(forkPlan, noClaimRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(forkPlan, noClaimRendered), None)

  test("Stage-8 Fork LLM smoke verifier rejects raw inputs and remains smoke-only"):
    val methodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val methodParameterShapes =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .filter(method => Set("mockNarrate", "codexCliPrompt", "check").contains(method.getName))
        .map(method => method.getParameterTypes.toVector.map(_.getSimpleName).toVector)

    Vector("fromStory", "fromMultiTargetProof", "fromBoardFacts", "fromEngineCheck", "fromSourceRow", "publicNarrate")
      .foreach: forbiddenMethod =>
        assert(!methodNames.contains(forbiddenMethod), s"LLM smoke must not expose $forbiddenMethod")
    Vector("Story", "MultiTargetProof", "BoardFacts", "EngineCheck", "EngineLine")
      .foreach: forbiddenInput =>
        assert(!methodParameterShapes.flatten.contains(forbiddenInput), s"LLM smoke must not accept raw $forbiddenInput")
    Vector(
      "raw Story",
      "raw MultiTargetProof",
      "BoardFacts",
      "EngineCheck",
      "raw PV",
      "proofFailures",
      "source rows"
    ).foreach: raw =>
      assertEquals(LlmNarrationSmoke.check(forkPlan, forkRendered, s"e5 forks the pieces on d6 and f6 using $raw.").accepted, false, raw)

  test("Stage-8 Fork LLM smoke authority lives in StoryInteractionLaw"):
    val law = scala.io.Source
      .fromFile("modules/commentary/docs/StoryInteractionLaw.md")
      .getLines()
      .mkString("\n")

    assert(law.contains("## Stage-8 LLM Smoke"))
    assert(law.contains("existing Fork LLM smoke may rephrase rendered Fork text"))
    assert(law.contains("Allowed input:"))
    assert(law.contains("- renderedText"))
    assert(law.contains("- claimKey"))
    assert(law.contains("- strength"))
    assert(law.contains("- forbidden wording"))
    assert(law.contains("- `Rephrase only. Do not add chess facts.`"))
    assert(law.contains("Allowed claimKey:"))
    assert(law.contains("- existing `forks_two_targets` only"))
    assert(law.contains("LLM must not add:"))
    assert(law.contains("- pawn fork as separate claim"))
    assert(law.contains("- wins material"))
    assert(law.contains("- new variation"))
    assert(law.contains("LLM only polishes."))
    assert(law.contains("Verifier rejects overclaim."))
    assert(law.contains("public/user-facing LLM narration remains closed."))

  private def forkPlan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(forkStory)).head).get

  private def forkRendered: RenderedLine =
    DeterministicRenderer.fromPlan(forkPlan).get

  private def forkStory: Story =
    TacticFork.write(forkFacts, Some(forkRoute), Some(targetA), Some(targetB)).get

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-8 FEN: $fen -> $error"), identity)
