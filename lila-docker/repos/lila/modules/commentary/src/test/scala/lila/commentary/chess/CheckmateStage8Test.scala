package lila.commentary.chess

class CheckmateStage8Test extends munit.FunSuite:

  private val mateFen = "k7/2Q5/2K5/8/8/8/8/8 w - - 0 1"
  private val mateMove = Line(Square('c', 7), Square('b', 7))

  private def facts: BoardFacts =
    BoardFacts.fromFen(mateFen).toOption.get

  private def plan: ExplanationPlan =
    val story = SceneCheckmate.write(facts, mateMove).get
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

  private def rendered: RenderedLine =
    DeterministicRenderer.fromPlan(plan).get

  test("Stage-8 LLM smoke accepts only bounded Checkmate RenderedLine input"):
    val checkmatePlan = plan
    val checkmateRendered = rendered
    val prompt = LlmNarrationSmoke.codexCliPrompt(checkmatePlan, checkmateRendered).get

    Vector(
      "renderedText: Qb7# is checkmate.",
      "claimKey: checkmates",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"Stage-8 prompt must include allowed input: $required")

    Vector(
      "Story(",
      "raw Story",
      "CheckmateProof",
      "CheckGivenProof",
      "CheckEscapedProof",
      "BoardFacts",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "raw PV",
      "principal variation",
      "proofFailures",
      "source row",
      "SAN # diagnostics",
      "rival king square",
      "exact after-board replay",
      "same-board proof"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"Stage-8 prompt must not expose $forbiddenInput")

    assertEquals(LlmNarrationSmoke.mockNarrate(checkmatePlan, checkmateRendered), Some("Qb7# is checkmate."))
    assertEquals(
      LlmNarrationSmoke.check(checkmatePlan, checkmateRendered, "Qb7# is checkmate."),
      NarrationSmokeCheck(true, Vector.empty)
    )
    assertEquals(
      LlmNarrationSmoke.check(checkmatePlan, checkmateRendered, "Qb7# checkmates the king.").accepted,
      true
    )

  test("Stage-8 LLM smoke rejects raw Checkmate proof engine and source additions"):
    val checkmatePlan = plan
    val checkmateRendered = rendered

    Vector(
      "raw Story" -> "Raw Story says Qb7# is checkmate.",
      "CheckmateProof" -> "CheckmateProof proves Qb7# is checkmate.",
      "CheckGivenProof" -> "CheckGivenProof is separate from Qb7#.",
      "CheckEscapedProof" -> "CheckEscapedProof is separate from Qb7#.",
      "BoardFacts" -> "BoardFacts show Qb7# is checkmate.",
      "EngineCheck" -> "EngineCheck supports Qb7#.",
      "EngineLine" -> "EngineLine starts with Qb7#.",
      "EngineEval" -> "EngineEval is mate after Qb7#.",
      "raw PV" -> "Raw PV: Qb7#.",
      "proofFailures" -> "proofFailures are empty, so Qb7# is checkmate.",
      "source rows" -> "The source rows show Qb7#.",
      "SAN diagnostics" -> "The SAN # diagnostics say Qb7# is mate."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(checkmatePlan, checkmateRendered, output)
      assertEquals(result.accepted, false, label)
      assert(
        result.violations.contains("raw_input") ||
          result.violations.contains("engine_mention") ||
          result.violations.contains("new_cause_or_evaluation"),
        s"$label: $result"
      )

  test("Stage-8 LLM smoke rejects new moves lines and forbidden Checkmate additions"):
    val checkmatePlan = plan
    val checkmateRendered = rendered

    Vector(
      "new move" -> "Qa7# is also checkmate.",
      "new line" -> "After Qb7# Kxb7, White still mates.",
      "mate threat" -> "Qb7# threatens mate.",
      "mate in one" -> "Qb7# was mate in one.",
      "mate in n" -> "Qb7# starts mate in two.",
      "forced mate" -> "Qb7# is forced mate.",
      "best move" -> "Qb7# is the best move.",
      "only move" -> "Qb7# is the only move.",
      "winning" -> "Qb7# is winning.",
      "decisive" -> "Qb7# is decisive.",
      "no counterplay" -> "Qb7# leaves no counterplay.",
      "king safety" -> "Qb7# solves king safety.",
      "unsafe king" -> "Qb7# shows the king is unsafe.",
      "attack" -> "Qb7# starts an attack.",
      "initiative" -> "Qb7# takes the initiative.",
      "pressure" -> "Qb7# creates pressure.",
      "engine mention" -> "The engine says Qb7# is checkmate.",
      "mate score" -> "Qb7# has a mate score.",
      "why mate" -> "Qb7# is checkmate because the king has no legal moves."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(checkmatePlan, checkmateRendered, output)
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
    val checkmatePlan = plan
    val checkmateRendered = rendered
    val supportPlan = checkmatePlan.copy(role = Role.Support, allowedClaim = None)
    val contextPlan = checkmatePlan.copy(role = Role.Context, allowedClaim = None)
    val blockedPlan = checkmatePlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)
    val mismatchedRendered = checkmateRendered.copy(claimKey = "gives_check")

    Vector(
      "support" -> supportPlan,
      "context" -> contextPlan,
      "blocked" -> blockedPlan
    ).foreach: (label, malformed) =>
      assertEquals(LlmNarrationSmoke.mockNarrate(malformed, checkmateRendered), None, label)
      assertEquals(LlmNarrationSmoke.codexCliPrompt(malformed, checkmateRendered), None, label)

    assertEquals(LlmNarrationSmoke.mockNarrate(checkmatePlan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(checkmatePlan, mismatchedRendered), None)

    val methodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector(
      "fromStory",
      "fromCheckmateProof",
      "fromCheckGivenProof",
      "fromCheckEscapedProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine",
      "callApi",
      "productionApi"
    ).foreach: method =>
      assert(!methodNames.contains(method), s"Stage-8 LLM smoke must not expose $method")
    Vector("Story", "CheckmateProof", "CheckGivenProof", "CheckEscapedProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach: parameter =>
      assert(!parameterNames.contains(parameter), s"Stage-8 LLM smoke must not accept $parameter")
