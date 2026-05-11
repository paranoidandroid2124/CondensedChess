package lila.commentary.chess

class CheckGivenStage8Test extends munit.FunSuite:

  private val checkingFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkingMove = Line(Square('d', 2), Square('e', 2))

  private def facts: BoardFacts =
    BoardFacts.fromFen(checkingFen).toOption.get

  private def plan: ExplanationPlan =
    val story = SceneCheckGiven.write(facts, checkingMove).get
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

  private def rendered: RenderedLine =
    DeterministicRenderer.fromPlan(plan).get

  test("Stage-8 LLM smoke accepts only bounded CheckGiven RenderedLine input"):
    val checkGivenPlan = plan
    val checkGivenRendered = rendered
    val prompt = LlmNarrationSmoke.codexCliPrompt(checkGivenPlan, checkGivenRendered).get

    Vector(
      "renderedText: Re2+ gives check.",
      "claimKey: gives_check",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"Stage-8 prompt must include allowed input: $required")

    Vector(
      "Story(",
      "raw Story",
      "CheckGivenProof",
      "BoardFacts",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "raw PV",
      "principal variation",
      "proofFailures",
      "source row",
      "rival king square",
      "exact after-board replay",
      "same-board proof"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"Stage-8 prompt must not expose $forbiddenInput")

    assertEquals(LlmNarrationSmoke.mockNarrate(checkGivenPlan, checkGivenRendered), Some("Re2+ gives check."))
    assertEquals(
      LlmNarrationSmoke.check(checkGivenPlan, checkGivenRendered, "Re2+ gives check."),
      NarrationSmokeCheck(true, Vector.empty)
    )
    assertEquals(
      LlmNarrationSmoke.check(checkGivenPlan, checkGivenRendered, "Re2+ checks the king.").accepted,
      true
    )

  test("Stage-8 LLM smoke rejects raw CheckGiven proof engine and source additions"):
    val checkGivenPlan = plan
    val checkGivenRendered = rendered

    Vector(
      "raw Story" -> "Raw Story says Re2+ gives check.",
      "CheckGivenProof" -> "CheckGivenProof proves Re2+ gives check.",
      "BoardFacts" -> "BoardFacts show Re2+ gives check.",
      "EngineCheck" -> "EngineCheck supports Re2+.",
      "EngineEval" -> "EngineEval is +1.2 after Re2+.",
      "raw PV" -> "Raw PV: Re2+ Kf8.",
      "proofFailures" -> "proofFailures are empty, so Re2+ gives check.",
      "source rows" -> "The source rows show Re2+."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(checkGivenPlan, checkGivenRendered, output)
      assertEquals(result.accepted, false, label)
      assert(result.violations.contains("raw_input") || result.violations.contains("engine_mention"), s"$label: $result")

  test("Stage-8 LLM smoke rejects new moves lines and forbidden CheckGiven meanings"):
    val checkGivenPlan = plan
    val checkGivenRendered = rendered

    Vector(
      "new move" -> "Nf5+ also gives check.",
      "new line" -> "After Re2+ Kf8, White keeps checking.",
      "mate threat" -> "Re2+ threatens mate.",
      "checkmate" -> "Re2+ is checkmate.",
      "mates" -> "Re2+ mates.",
      "king safety" -> "Re2+ shows the king is unsafe.",
      "attack" -> "Re2+ starts an attack.",
      "initiative" -> "Re2+ takes the initiative.",
      "pressure" -> "Re2+ creates pressure.",
      "forced reply" -> "Re2+ forces a reply.",
      "best move" -> "Re2+ is the best move.",
      "only move" -> "Re2+ is the only move.",
      "winning" -> "Re2+ is winning.",
      "decisive" -> "Re2+ is decisive.",
      "no counterplay" -> "Re2+ leaves no counterplay.",
      "engine mention" -> "The engine says Re2+ gives check."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(checkGivenPlan, checkGivenRendered, output)
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
    val checkGivenPlan = plan
    val checkGivenRendered = rendered
    val supportPlan = checkGivenPlan.copy(role = Role.Support, allowedClaim = None)
    val contextPlan = checkGivenPlan.copy(role = Role.Context, allowedClaim = None)
    val blockedPlan = checkGivenPlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)
    val mismatchedRendered = checkGivenRendered.copy(claimKey = "checkmate")

    Vector(
      "support" -> supportPlan,
      "context" -> contextPlan,
      "blocked" -> blockedPlan
    ).foreach: (label, malformed) =>
      assertEquals(LlmNarrationSmoke.mockNarrate(malformed, checkGivenRendered), None, label)
      assertEquals(LlmNarrationSmoke.codexCliPrompt(malformed, checkGivenRendered), None, label)

    assertEquals(LlmNarrationSmoke.mockNarrate(checkGivenPlan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(checkGivenPlan, mismatchedRendered), None)

    val methodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector(
      "fromStory",
      "fromCheckGivenProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine",
      "callApi",
      "productionApi"
    ).foreach: method =>
      assert(!methodNames.contains(method), s"Stage-8 LLM smoke must not expose $method")
    Vector("Story", "CheckGivenProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach: parameter =>
      assert(!parameterNames.contains(parameter), s"Stage-8 LLM smoke must not accept $parameter")
