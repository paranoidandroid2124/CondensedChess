package lila.commentary.chess

class InterferenceLlmSmokeTest extends munit.FunSuite:

  test("Interference LLM smoke accepts already-rendered bounded input only"):
    val interferencePlan = plan
    val interferenceRendered = rendered
    val prompt = LlmNarrationSmoke.codexCliPrompt(interferencePlan, interferenceRendered).get

    Vector(
      "renderedText: Ba4 blocks the defender's line to a1.",
      "claimKey: blocks_defender_line",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"Interference prompt must include allowed input: $required")

    Vector(
      "Story(",
      "raw Story",
      "Use only the supplied fields",
      "Do not add a move",
      "Return JSON only",
      "InterferenceProof",
      "BoardFacts",
      "StoryTable",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "raw PV",
      "principal variation",
      "proofFailures",
      "source row",
      "a8a1",
      "a8-a1",
      "b3a4",
      "b3-a4",
      "same-board proof",
      "line defender",
      "blocking square"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"Interference prompt must not expose $forbiddenInput")

    assertEquals(LlmNarrationSmoke.mockNarrate(interferencePlan, interferenceRendered), Some(interferenceRendered.text))
    assertEquals(
      LlmNarrationSmoke.check(interferencePlan, interferenceRendered, interferenceRendered.text),
      NarrationSmokeCheck(true, Vector.empty)
    )

  test("Interference LLM smoke rejects added chess facts"):
    val interferencePlan = plan
    val interferenceRendered = rendered

    Vector(
      "wins material" -> "Ba4 blocks the defender's line to a1 and wins material.",
      "wins piece" -> "Ba4 blocks the defender's line to a1 and wins a piece.",
      "material result" -> "Ba4 blocks the defender's line to a1, so White gets a material gain.",
      "why it matters" -> "Ba4 blocks the defender's line to a1. Why it matters: the defender is cut off.",
      "forced" -> "Ba4 blocks the defender's line to a1 by force.",
      "only" -> "Ba4 is the only move that blocks the defender's line to a1.",
      "best" -> "Ba4 is the best move because it blocks the defender's line to a1.",
      "no defense" -> "Ba4 blocks the defender's line to a1 and leaves no defense.",
      "no counterplay" -> "Ba4 blocks the defender's line to a1 with no counterplay.",
      "pin" -> "Ba4 pins the piece on a1.",
      "skewer" -> "Ba4 skewers the piece on a1.",
      "fork" -> "Ba4 forks the pieces on a1 and a8.",
      "discovered attack" -> "Ba4 creates a discovered attack on a1.",
      "decoy" -> "Ba4 decoys the defender.",
      "deflect" -> "Ba4 deflects the defender from a1.",
      "remove guard" -> "Ba4 removes the defender of a1.",
      "overload" -> "Ba4 overloads the defender.",
      "trap" -> "Ba4 traps the piece on a1.",
      "engine" -> "The engine says Ba4 blocks the defender's line to a1.",
      "engine pv" -> "Ba4 blocks the defender's line to a1 according to the PV.",
      "engine eval" -> "Ba4 blocks the defender's line to a1 and the eval is +1.2.",
      "engine depth" -> "Ba4 blocks the defender's line to a1 at depth 18.",
      "raw line" -> "Ba4 blocks the a8-a1 line.",
      "raw route" -> "After b3-a4, the defender line is blocked.",
      "new variation" -> "Ba4 blocks the defender's line to a1; then Rc8 follows."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(interferencePlan, interferenceRendered, output)
      assertEquals(result.accepted, false, label)
      assert(
        result.violations.exists(v =>
          v == "forbidden_wording" ||
            v == "engine_mention" ||
            v == "new_move_or_line" ||
            v == "new_tactic_or_plan" ||
            v == "new_cause_or_evaluation" ||
            v == "stronger_claim"
        ),
        s"$label must be rejected as an added Interference smoke claim: $result"
      )

  test("Interference LLM smoke refuses non-Lead mismatched and raw input surfaces"):
    val interferencePlan = plan
    val interferenceRendered = rendered
    val supportPlan = interferencePlan.copy(role = Role.Support, allowedClaim = None)
    val contextPlan = interferencePlan.copy(role = Role.Context, allowedClaim = None)
    val blockedPlan = interferencePlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)
    val mismatchedRendered = interferenceRendered.copy(claimKey = "deflects_defender")

    Vector(
      "support" -> supportPlan,
      "context" -> contextPlan,
      "blocked" -> blockedPlan
    ).foreach: (label, malformed) =>
      assertEquals(LlmNarrationSmoke.mockNarrate(malformed, interferenceRendered), None, label)
      assertEquals(LlmNarrationSmoke.codexCliPrompt(malformed, interferenceRendered), None, label)

    assertEquals(LlmNarrationSmoke.mockNarrate(interferencePlan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(interferencePlan, mismatchedRendered), None)

    val methodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector(
      "fromStory",
      "fromInterferenceProof",
      "fromBoardFacts",
      "fromStoryTable",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine",
      "fromLineGeometry",
      "callApi",
      "productionApi"
    ).foreach: method =>
      assert(!methodNames.contains(method), s"Interference LLM smoke must not expose $method")
    Vector("Story", "InterferenceProof", "BoardFacts", "StoryTable", "EngineCheck", "EngineLine", "EngineEval").foreach: parameter =>
      assert(!parameterNames.contains(parameter), s"Interference LLM smoke must not accept $parameter")

  private def plan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

  private def rendered: RenderedLine =
    DeterministicRenderer.fromPlan(plan).get

  private def story: Story =
    TacticInterference.write(facts, Some(route), Some(defender), Some(blockingSquare), Some(target)).get

  private def facts: BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Interference LLM smoke FEN: $fen -> $error"), identity)

  private val fen = "r6k/8/8/8/8/1B6/8/n6K w - - 0 1"
  private val route = Line(Square('b', 3), Square('a', 4))
  private val defender = Square('a', 8)
  private val blockingSquare = Square('a', 4)
  private val target = Square('a', 1)
