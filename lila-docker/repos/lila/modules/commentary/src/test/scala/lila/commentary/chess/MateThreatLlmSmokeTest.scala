package lila.commentary.chess

class MateThreatLlmSmokeTest extends munit.FunSuite:

  private val mateThreatFen = "7k/8/6K1/8/8/8/5Q2/8 w - - 0 1"
  private val threatMove = Line(Square('g', 6), Square('h', 6))

  private def facts: BoardFacts =
    BoardFacts.fromFen(mateThreatFen).toOption.get

  private def plan: ExplanationPlan =
    val story = SceneMateThreat.write(facts, threatMove).get
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

  private def rendered: RenderedLine =
    DeterministicRenderer.fromPlan(plan).get

  test("MateThreat LLM smoke accepts only bounded rendered-text input"):
    val mateThreatPlan = plan
    val mateThreatRendered = rendered
    val prompt = LlmNarrationSmoke.codexCliPrompt(mateThreatPlan, mateThreatRendered).get

    Vector(
      "instruction: Rephrase only. Do not add chess facts.",
      "renderedText: Kh6 threatens mate next move.",
      "claimKey: threatens_mate_next",
      "strength: bounded",
      "forbiddenWording:"
    ).foreach: required =>
      assert(prompt.contains(required), s"MateThreat prompt must include allowed input: $required")

    Vector(
      "You are",
      "Return JSON",
      "Story(",
      "raw Story",
      "MateThreatProof",
      "CheckmateProof",
      "CheckGivenProof",
      "BoardFacts",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "raw PV",
      "principal variation",
      "proofFailures",
      "source row",
      "Qf8",
      "#",
      "mate score",
      "route diagnostics",
      "exact after-board replay",
      "same-board proof"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"MateThreat prompt must not expose $forbiddenInput")

    assertEquals(LlmNarrationSmoke.mockNarrate(mateThreatPlan, mateThreatRendered), Some("Kh6 threatens mate next move."))
    assertEquals(
      LlmNarrationSmoke.check(mateThreatPlan, mateThreatRendered, "Kh6 threatens mate next move."),
      NarrationSmokeCheck(true, Vector.empty)
    )
    assertEquals(
      LlmNarrationSmoke.check(mateThreatPlan, mateThreatRendered, "Kh6 threatens mate on the next move.").accepted,
      true
    )

  test("MateThreat LLM smoke rejects raw proof engine mate-move and source additions"):
    val mateThreatPlan = plan
    val mateThreatRendered = rendered

    Vector(
      "raw Story" -> "Raw Story says Kh6 threatens mate next move.",
      "MateThreatProof" -> "MateThreatProof proves Kh6 threatens mate next move.",
      "CheckmateProof" -> "CheckmateProof finds Qf8# after Kh6.",
      "BoardFacts" -> "BoardFacts show Kh6 threatens mate next move.",
      "EngineCheck" -> "EngineCheck supports Kh6.",
      "EngineEval" -> "EngineEval says mate after Kh6.",
      "EngineLine" -> "EngineLine starts Kh6.",
      "raw PV" -> "Raw PV: Kh6 Kg8 Qf8#.",
      "mate score" -> "Kh6 has a mate score.",
      "proofFailures" -> "proofFailures are empty, so Kh6 threatens mate next move.",
      "source rows" -> "The source rows show Kh6 threatens mate next move.",
      "route diagnostics" -> "Route diagnostics show g6h6 threatens mate."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(mateThreatPlan, mateThreatRendered, output)
      assertEquals(result.accepted, false, label)
      assert(
        result.violations.exists(v =>
          v == "raw_input" ||
            v == "engine_mention" ||
            v == "new_move_or_line" ||
            v == "new_cause_or_evaluation" ||
            v == "stronger_claim"
        ),
        s"$label: $result"
      )

  test("MateThreat LLM smoke rejects stronger mate and line claims"):
    val mateThreatPlan = plan
    val mateThreatRendered = rendered

    Vector(
      "forced mate" -> "Kh6 threatens forced mate.",
      "mate in two" -> "Kh6 starts mate in two.",
      "mate in N" -> "Kh6 starts mate in N.",
      "unavoidable mate" -> "Kh6 creates unavoidable mate.",
      "only defense" -> "Kh6 leaves only defense.",
      "no defense" -> "Kh6 leaves no defense.",
      "cannot stop mate" -> "Black cannot stop mate after Kh6.",
      "winning attack" -> "Kh6 is a winning attack.",
      "best move" -> "Kh6 is the best move.",
      "engine says mate" -> "The engine says mate after Kh6.",
      "raw mate move" -> "Kh6 threatens Qf8#.",
      "new move" -> "Kh6 threatens mate after Kg8.",
      "new variation" -> "After Kh6 Kg8 Qf8#, White mates.",
      "checkmate now" -> "Kh6 is checkmate.",
      "why" -> "Kh6 threatens mate because the king has no escape."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(mateThreatPlan, mateThreatRendered, output)
      assertEquals(result.accepted, false, label)
      assert(
        result.violations.exists(v =>
          v == "forbidden_wording" ||
            v == "new_move_or_line" ||
            v == "new_tactic_or_plan" ||
            v == "new_cause_or_evaluation" ||
            v == "engine_mention" ||
            v == "stronger_claim"
        ),
        s"$label must be rejected as a new chess fact or stronger claim: $result"
      )

  test("MateThreat LLM smoke rejects Support Context Blocked mismatched and raw input surfaces"):
    val mateThreatPlan = plan
    val mateThreatRendered = rendered
    val supportPlan = mateThreatPlan.copy(role = Role.Support, allowedClaim = None)
    val contextPlan = mateThreatPlan.copy(role = Role.Context, allowedClaim = None)
    val blockedPlan = mateThreatPlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)
    val mismatchedRendered = mateThreatRendered.copy(claimKey = "checkmates")

    Vector(
      "support" -> supportPlan,
      "context" -> contextPlan,
      "blocked" -> blockedPlan
    ).foreach: (label, malformed) =>
      assertEquals(LlmNarrationSmoke.mockNarrate(malformed, mateThreatRendered), None, label)
      assertEquals(LlmNarrationSmoke.codexCliPrompt(malformed, mateThreatRendered), None, label)

    assertEquals(LlmNarrationSmoke.mockNarrate(mateThreatPlan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(mateThreatPlan, mismatchedRendered), None)

    val methodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector(
      "fromStory",
      "fromMateThreatProof",
      "fromCheckmateProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine",
      "callApi",
      "productionApi"
    ).foreach: method =>
      assert(!methodNames.contains(method), s"MateThreat LLM smoke must not expose $method")
    Vector("Story", "MateThreatProof", "CheckmateProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach: parameter =>
      assert(!parameterNames.contains(parameter), s"MateThreat LLM smoke must not accept $parameter")
