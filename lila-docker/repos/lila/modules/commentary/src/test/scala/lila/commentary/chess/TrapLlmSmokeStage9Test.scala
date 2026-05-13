package lila.commentary.chess

class TrapLlmSmokeStage9Test extends munit.FunSuite:

  test("Stage-9 LLM smoke accepts exact rendered Trap text only through bounded input"):
    val trapPlan = plan
    val trapRendered = rendered
    val prompt = LlmNarrationSmoke.codexCliPrompt(trapPlan, trapRendered).get

    Vector(
      "renderedText: Ra7 traps the piece on a8.",
      "claimKey: traps_piece",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"Stage-9 prompt must include allowed input: $required")

    Vector(
      "Story(",
      "raw Story",
      "TrapProof",
      "BoardFacts",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "raw PV",
      "principal variation",
      "proofFailures",
      "source row",
      "escape-square",
      "escape square",
      "target move map",
      "exact after-board replay",
      "same-board proof"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"Stage-9 prompt must not expose $forbiddenInput")

    assertEquals(LlmNarrationSmoke.mockNarrate(trapPlan, trapRendered), Some("Ra7 traps the piece on a8."))
    assertEquals(
      LlmNarrationSmoke.check(trapPlan, trapRendered, "Ra7 traps the piece on a8."),
      NarrationSmokeCheck(true, Vector.empty)
    )

  test("Stage-9 LLM smoke rejects Trap material forced no-escape best-only queen engine and no-counterplay claims"):
    val trapPlan = plan
    val trapRendered = rendered

    Vector(
      "wins piece" -> "Ra7 traps the piece on a8 and wins the piece.",
      "wins material" -> "Ra7 traps the piece on a8 and wins material.",
      "forced" -> "Ra7 traps the piece on a8 with a forced line.",
      "no escape" -> "Ra7 traps the piece on a8 with no escape.",
      "cannot be saved" -> "Ra7 traps the piece on a8 and cannot be saved.",
      "best move" -> "Ra7 is the best move.",
      "only move" -> "Ra7 is the only move.",
      "queen trap" -> "Ra7 creates a queen trap.",
      "queen is trapped" -> "The queen is trapped on a8.",
      "engine" -> "The engine says Ra7 traps the piece on a8.",
      "engine eval" -> "Stockfish gives +2.1 after Ra7.",
      "no counterplay" -> "Ra7 traps the piece on a8 with no counterplay."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(trapPlan, trapRendered, output)
      assertEquals(result.accepted, false, label)
      assert(
        result.violations.exists(v =>
          v == "forbidden_wording" ||
            v == "engine_mention" ||
            v == "new_tactic_or_plan" ||
            v == "stronger_claim"
        ),
        s"$label must be rejected as an added Trap smoke claim: $result"
      )

  test("Stage-9 LLM smoke refuses non-Lead mismatched and raw Trap input surfaces"):
    val trapPlan = plan
    val trapRendered = rendered
    val supportPlan = trapPlan.copy(role = Role.Support, allowedClaim = None)
    val contextPlan = trapPlan.copy(role = Role.Context, allowedClaim = None)
    val blockedPlan = trapPlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)
    val mismatchedRendered = trapRendered.copy(claimKey = "attacks_queen")

    Vector(
      "support" -> supportPlan,
      "context" -> contextPlan,
      "blocked" -> blockedPlan
    ).foreach: (label, malformed) =>
      assertEquals(LlmNarrationSmoke.mockNarrate(malformed, trapRendered), None, label)
      assertEquals(LlmNarrationSmoke.codexCliPrompt(malformed, trapRendered), None, label)

    assertEquals(LlmNarrationSmoke.mockNarrate(trapPlan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(trapPlan, mismatchedRendered), None)

    val methodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector(
      "fromStory",
      "fromTrapProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine",
      "callApi",
      "productionApi"
    ).foreach: method =>
      assert(!methodNames.contains(method), s"Stage-9 LLM smoke must not expose $method")
    Vector("Story", "TrapProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach: parameter =>
      assert(!parameterNames.contains(parameter), s"Stage-9 LLM smoke must not accept $parameter")

  private def plan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

  private def rendered: RenderedLine =
    DeterministicRenderer.fromPlan(plan).get

  private def story: Story =
    TacticTrap.write(facts, Some(route)).get

  private def facts: BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Trap Stage-9 FEN: $fen -> $error"), identity)

  private val fen = "n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1"
  private val route = Line(Square('a', 1), Square('a', 7))
