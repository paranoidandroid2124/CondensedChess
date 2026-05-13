package lila.commentary.chess

class DeflectLlmSmokeStage6Test extends munit.FunSuite:

  test("Stage-6 LLM smoke accepts exact rendered Deflect text only through bounded input"):
    val deflectPlan = plan
    val deflectRendered = rendered
    val prompt = LlmNarrationSmoke.codexCliPrompt(deflectPlan, deflectRendered).get

    Vector(
      "renderedText: h3 deflects the defender from d5.",
      "claimKey: deflects_defender",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"Stage-6 prompt must include allowed input: $required")

    Vector(
      "Story(",
      "raw Story",
      "DeflectProof",
      "BoardFacts",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "raw PV",
      "principal variation",
      "proofFailures",
      "source row",
      "reply map",
      "rival reply",
      "e6g4",
      "same-board proof"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"Stage-6 prompt must not expose $forbiddenInput")

    assertEquals(LlmNarrationSmoke.mockNarrate(deflectPlan, deflectRendered), Some("h3 deflects the defender from d5."))
    assertEquals(
      LlmNarrationSmoke.check(deflectPlan, deflectRendered, "h3 deflects the defender from d5."),
      NarrationSmokeCheck(true, Vector.empty)
    )

  test("Stage-6 LLM smoke rejects Deflect material force sibling tactic engine and raw reply additions"):
    val deflectPlan = plan
    val deflectRendered = rendered

    Vector(
      "wins material" -> "h3 deflects the defender from d5 and wins material.",
      "wins piece" -> "h3 deflects the defender from d5 and wins a piece.",
      "forced" -> "h3 deflects the defender from d5 with a forced reply.",
      "only" -> "h3 is the only move.",
      "best" -> "h3 is the best move.",
      "no defense" -> "h3 deflects the defender from d5 and leaves no defense.",
      "no counterplay" -> "h3 deflects the defender from d5 with no counterplay.",
      "removes defender" -> "h3 removes the defender from d5.",
      "overloads defender" -> "h3 overloads the defender.",
      "traps defender" -> "h3 traps the defender.",
      "decoys" -> "h3 decoys the defender.",
      "engine line" -> "The engine line shows h3 deflects the defender from d5.",
      "raw reply coordinate" -> "After e6g4, h3 deflects the defender from d5.",
      "raw reply explanation" -> "h3 works because the rival reply moves the bishop away from d5."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(deflectPlan, deflectRendered, output)
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
        s"$label must be rejected as an added Deflect smoke claim: $result"
      )

  test("Stage-6 LLM smoke refuses non-Lead mismatched and raw Deflect input surfaces"):
    val deflectPlan = plan
    val deflectRendered = rendered
    val supportPlan = deflectPlan.copy(role = Role.Support, allowedClaim = None)
    val contextPlan = deflectPlan.copy(role = Role.Context, allowedClaim = None)
    val blockedPlan = deflectPlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)
    val mismatchedRendered = deflectRendered.copy(claimKey = "removes_defender")

    Vector(
      "support" -> supportPlan,
      "context" -> contextPlan,
      "blocked" -> blockedPlan
    ).foreach: (label, malformed) =>
      assertEquals(LlmNarrationSmoke.mockNarrate(malformed, deflectRendered), None, label)
      assertEquals(LlmNarrationSmoke.codexCliPrompt(malformed, deflectRendered), None, label)

    assertEquals(LlmNarrationSmoke.mockNarrate(deflectPlan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(deflectPlan, mismatchedRendered), None)

    val methodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector(
      "fromStory",
      "fromDeflectProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine",
      "fromReplyMap",
      "callApi",
      "productionApi"
    ).foreach: method =>
      assert(!methodNames.contains(method), s"Stage-6 LLM smoke must not expose $method")
    Vector("Story", "DeflectProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach: parameter =>
      assert(!parameterNames.contains(parameter), s"Stage-6 LLM smoke must not accept $parameter")

  private def plan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

  private def rendered: RenderedLine =
    DeterministicRenderer.fromPlan(plan).get

  private def story: Story =
    TacticDeflect.write(facts, Some(route), Some(reply), Some(defenderBeforeReply), Some(target)).get

  private def facts: BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Deflect Stage-6 FEN: $fen -> $error"), identity)

  private val fen = "7k/8/4b3/3n4/8/8/7P/7K w - - 0 1"
  private val route = Line(Square('h', 2), Square('h', 3))
  private val reply = Line(Square('e', 6), Square('g', 4))
  private val defenderBeforeReply = Square('e', 6)
  private val target = Square('d', 5)
