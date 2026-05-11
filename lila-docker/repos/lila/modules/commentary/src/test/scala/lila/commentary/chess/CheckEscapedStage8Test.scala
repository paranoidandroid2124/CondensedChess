package lila.commentary.chess

class CheckEscapedStage8Test extends munit.FunSuite:

  private val escapeFen = "k3r3/8/8/8/8/8/8/4K3 w - - 0 1"
  private val escapeMove = Line(Square('e', 1), Square('f', 1))

  private def facts: BoardFacts =
    BoardFacts.fromFen(escapeFen).toOption.get

  private def plan: ExplanationPlan =
    val story = SceneCheckEscaped.write(facts, escapeMove).get
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

  private def rendered: RenderedLine =
    DeterministicRenderer.fromPlan(plan).get

  test("Stage-8 LLM smoke accepts only bounded CheckEscaped RenderedLine input"):
    val checkEscapedPlan = plan
    val checkEscapedRendered = rendered
    val prompt = LlmNarrationSmoke.codexCliPrompt(checkEscapedPlan, checkEscapedRendered).get

    Vector(
      "renderedText: Kf1 gets out of check.",
      "claimKey: escapes_check",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"Stage-8 prompt must include allowed input: $required")

    Vector(
      "Story(",
      "raw Story",
      "CheckEscapedProof",
      "CheckGivenProof",
      "BoardFacts",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "raw PV",
      "principal variation",
      "proofFailures",
      "source row",
      "escape method",
      "king move",
      "interposition",
      "checking piece capture",
      "exact after-board replay",
      "same-board proof"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"Stage-8 prompt must not expose $forbiddenInput")

    assertEquals(LlmNarrationSmoke.mockNarrate(checkEscapedPlan, checkEscapedRendered), Some("Kf1 gets out of check."))
    assertEquals(
      LlmNarrationSmoke.check(checkEscapedPlan, checkEscapedRendered, "Kf1 gets out of check."),
      NarrationSmokeCheck(true, Vector.empty)
    )
    assertEquals(
      LlmNarrationSmoke.check(checkEscapedPlan, checkEscapedRendered, "Kf1 escapes the check.").accepted,
      true
    )

  test("Stage-8 LLM smoke rejects raw CheckEscaped proof engine source and escape-method diagnostics"):
    val checkEscapedPlan = plan
    val checkEscapedRendered = rendered

    Vector(
      "raw Story" -> "Raw Story says Kf1 gets out of check.",
      "CheckEscapedProof" -> "CheckEscapedProof proves Kf1 gets out of check.",
      "CheckGivenProof" -> "CheckGivenProof is separate from Kf1.",
      "BoardFacts" -> "BoardFacts show Kf1 gets out of check.",
      "EngineCheck" -> "EngineCheck supports Kf1.",
      "EngineLine" -> "EngineLine starts with Kf1.",
      "EngineEval" -> "EngineEval is +1.2 after Kf1.",
      "raw PV" -> "Raw PV: Kf1 Rf8.",
      "proofFailures" -> "proofFailures are empty, so Kf1 gets out of check.",
      "source rows" -> "The source rows show Kf1.",
      "escape diagnostics" -> "The escape method diagnostics say this was a king move."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(checkEscapedPlan, checkEscapedRendered, output)
      assertEquals(result.accepted, false, label)
      assert(
        result.violations.contains("raw_input") || result.violations.contains("engine_mention"),
        s"$label: $result"
      )

  test("Stage-8 LLM smoke rejects new moves lines and forbidden CheckEscaped meanings"):
    val checkEscapedPlan = plan
    val checkEscapedRendered = rendered

    Vector(
      "new move" -> "Nf3 also gets out of check.",
      "new line" -> "After Kf1 Rf8, White has escaped.",
      "king moved out" -> "Kf1 moves the king out of check.",
      "blocked check" -> "Kf1 blocked the check.",
      "captured checker" -> "Kf1 captured the checker.",
      "gives check" -> "Kf1 also gives check.",
      "mate threat" -> "Kf1 creates a mate threat.",
      "checkmate" -> "Kf1 prevents checkmate.",
      "avoids mate" -> "Kf1 avoids mate.",
      "king safety" -> "Kf1 solves king safety.",
      "safe king" -> "Kf1 makes the king safe.",
      "unsafe king" -> "Kf1 means the king is no longer unsafe.",
      "refutes attack" -> "Kf1 refutes the attack.",
      "defense success" -> "Kf1 is a successful defense.",
      "initiative" -> "Kf1 takes the initiative.",
      "pressure" -> "Kf1 creates pressure.",
      "forced reply" -> "Kf1 forces a reply.",
      "best move" -> "Kf1 is the best move.",
      "only move" -> "Kf1 is the only move.",
      "winning" -> "Kf1 is winning.",
      "decisive" -> "Kf1 is decisive.",
      "no counterplay" -> "Kf1 leaves no counterplay.",
      "engine mention" -> "The engine says Kf1 gets out of check."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(checkEscapedPlan, checkEscapedRendered, output)
      assertEquals(result.accepted, false, label)
      assert(
        result.violations.exists(v =>
          v == "new_move_or_line" ||
            v == "forbidden_wording" ||
            v == "new_tactic_or_plan" ||
            v == "engine_mention" ||
            v == "stronger_claim"
        ),
        s"$label must be rejected as a new chess fact or stronger claim: $result"
      )

  test("Stage-8 LLM smoke rejects Support Context Blocked mismatched and raw input surfaces"):
    val checkEscapedPlan = plan
    val checkEscapedRendered = rendered
    val supportPlan = checkEscapedPlan.copy(role = Role.Support, allowedClaim = None)
    val contextPlan = checkEscapedPlan.copy(role = Role.Context, allowedClaim = None)
    val blockedPlan = checkEscapedPlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)
    val mismatchedRendered = checkEscapedRendered.copy(claimKey = "gives_check")

    Vector(
      "support" -> supportPlan,
      "context" -> contextPlan,
      "blocked" -> blockedPlan
    ).foreach: (label, malformed) =>
      assertEquals(LlmNarrationSmoke.mockNarrate(malformed, checkEscapedRendered), None, label)
      assertEquals(LlmNarrationSmoke.codexCliPrompt(malformed, checkEscapedRendered), None, label)

    assertEquals(LlmNarrationSmoke.mockNarrate(checkEscapedPlan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(checkEscapedPlan, mismatchedRendered), None)

    val methodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector(
      "fromStory",
      "fromCheckEscapedProof",
      "fromCheckGivenProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine",
      "callApi",
      "productionApi"
    ).foreach: method =>
      assert(!methodNames.contains(method), s"Stage-8 LLM smoke must not expose $method")
    Vector("Story", "CheckEscapedProof", "CheckGivenProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach: parameter =>
      assert(!parameterNames.contains(parameter), s"Stage-8 LLM smoke must not accept $parameter")
