package lila.commentary.chess

import java.nio.file.{ Files, Paths }

class StalemateStage8Test extends munit.FunSuite:

  private val stalemateFen = "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1"
  private val stalemateMove = Line(Square('g', 5), Square('g', 6))

  private def facts: BoardFacts =
    BoardFacts.fromFen(stalemateFen).toOption.get

  private def plan: ExplanationPlan =
    val story = SceneStalemate.write(facts, stalemateMove).get
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

  private def rendered: RenderedLine =
    DeterministicRenderer.fromPlan(plan).get

  test("Stage-8 LLM smoke accepts only bounded Stalemate RenderedLine input"):
    val stalematePlan = plan
    val stalemateRendered = rendered
    val prompt = LlmNarrationSmoke.codexCliPrompt(stalematePlan, stalemateRendered).get

    Vector(
      "renderedText: Qg6 is stalemate.",
      "claimKey: stalemates",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"Stage-8 prompt must include allowed input: $required")

    Vector(
      "Story(",
      "raw Story",
      "StalemateProof",
      "BoardFacts",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "raw PV",
      "principal variation",
      "proofFailures",
      "source row",
      "result notation",
      "tablebase diagnostics",
      "rival king square",
      "exact after-board replay",
      "same-board proof"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"Stage-8 prompt must not expose $forbiddenInput")

    assertEquals(LlmNarrationSmoke.mockNarrate(stalematePlan, stalemateRendered), Some("Qg6 is stalemate."))
    assertEquals(
      LlmNarrationSmoke.check(stalematePlan, stalemateRendered, "Qg6 is stalemate."),
      NarrationSmokeCheck(true, Vector.empty)
    )
    assertEquals(
      LlmNarrationSmoke.check(stalematePlan, stalemateRendered, "Qg6 stalemates the king.").accepted,
      true
    )

  test("Stage-8 LLM smoke rejects raw Stalemate proof engine source result and tablebase additions"):
    val stalematePlan = plan
    val stalemateRendered = rendered

    Vector(
      "raw Story" -> "Raw Story says Qg6 is stalemate.",
      "StalemateProof" -> "StalemateProof proves Qg6 is stalemate.",
      "BoardFacts" -> "BoardFacts show Qg6 is stalemate.",
      "EngineCheck" -> "EngineCheck supports Qg6.",
      "EngineLine" -> "EngineLine starts with Qg6.",
      "EngineEval" -> "EngineEval is 0.00 after Qg6.",
      "raw PV" -> "Raw PV: Qg6.",
      "proofFailures" -> "proofFailures are empty, so Qg6 is stalemate.",
      "source rows" -> "The source rows show Qg6.",
      "result notation" -> "The result notation is 1/2-1/2 after Qg6.",
      "tablebase diagnostics" -> "Tablebase diagnostics say Qg6 draws."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(stalematePlan, stalemateRendered, output)
      assertEquals(result.accepted, false, label)
      assert(
        result.violations.contains("raw_input") ||
          result.violations.contains("engine_mention") ||
          result.violations.contains("new_cause_or_evaluation") ||
          result.violations.contains("forbidden_wording"),
        s"$label: $result"
      )

  test("Stage-8 LLM smoke rejects new moves lines draw evaluation and sibling King Check additions"):
    val stalematePlan = plan
    val stalemateRendered = rendered

    Vector(
      "new move" -> "Qh5 also stalemates.",
      "new line" -> "After Qg6 Kh7, it is still stalemate.",
      "draw result explanation" -> "Qg6 draws the game by stalemate.",
      "saves game" -> "Qg6 saves the game.",
      "throws win" -> "Qg6 throws away the win.",
      "blunder" -> "Qg6 is a blunder.",
      "tablebase" -> "The tablebase says Qg6 draws.",
      "engine mention" -> "The engine says Qg6 is stalemate.",
      "best move" -> "Qg6 is the best move.",
      "only move" -> "Qg6 is the only move.",
      "forced" -> "Qg6 is forced.",
      "winning" -> "Qg6 was winning.",
      "losing" -> "Qg6 is losing.",
      "decisive" -> "Qg6 is decisive.",
      "no counterplay" -> "Qg6 leaves no counterplay.",
      "checkmate" -> "Qg6 is checkmate.",
      "check" -> "Qg6 gives check.",
      "escape check" -> "Qg6 escapes check."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(stalematePlan, stalemateRendered, output)
      assertEquals(result.accepted, false, label)
      assert(
        result.violations.exists(v =>
          v == "new_move_or_line" ||
            v == "forbidden_wording" ||
            v == "new_tactic_or_plan" ||
            v == "new_cause_or_evaluation" ||
            v == "engine_mention" ||
            v == "stronger_claim"
        ),
        s"$label must be rejected as a new chess fact or stronger claim: $result"
      )

  test("Stage-8 LLM smoke rejects Support Context Blocked mismatched and raw input surfaces"):
    val stalematePlan = plan
    val stalemateRendered = rendered
    val supportPlan = stalematePlan.copy(role = Role.Support, allowedClaim = None)
    val contextPlan = stalematePlan.copy(role = Role.Context, allowedClaim = None)
    val blockedPlan = stalematePlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)
    val mismatchedRendered = stalemateRendered.copy(claimKey = "checkmates")

    Vector(
      "support" -> supportPlan,
      "context" -> contextPlan,
      "blocked" -> blockedPlan
    ).foreach: (label, malformed) =>
      assertEquals(LlmNarrationSmoke.mockNarrate(malformed, stalemateRendered), None, label)
      assertEquals(LlmNarrationSmoke.codexCliPrompt(malformed, stalemateRendered), None, label)

    assertEquals(LlmNarrationSmoke.mockNarrate(stalematePlan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(stalematePlan, mismatchedRendered), None)

    val methodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector(
      "fromStory",
      "fromStalemateProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine",
      "fromResultNotation",
      "fromTablebase",
      "callApi",
      "productionApi"
    ).foreach: method =>
      assert(!methodNames.contains(method), s"Stage-8 LLM smoke must not expose $method")
    Vector("Story", "StalemateProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach: parameter =>
      assert(!parameterNames.contains(parameter), s"Stage-8 LLM smoke must not accept $parameter")

  test("Stage-8 Stalemate LLM smoke authority lives in StoryInteractionLaw"):
    val law = Files.readString(Paths.get("modules/commentary/docs/StoryInteractionLaw.md"))
    assert(law.contains("### Stalemate-8 LLM Smoke"))
    assert(law.contains("Reuse only existing LLM smoke boundary for bounded Stalemate `RenderedLine`."))
    assert(law.contains("`Rephrase only. Do not add chess facts.`"))
    assert(law.contains("Completion standard: Stalemate-8 closes when LLM smoke may only rephrase bounded"))
