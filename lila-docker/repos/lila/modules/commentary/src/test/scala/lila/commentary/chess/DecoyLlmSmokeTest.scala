package lila.commentary.chess

class DecoyLlmSmokeTest extends munit.FunSuite:

  test("LLM smoke accepts exact rendered Decoy text only through bounded input"):
    val decoyPlan = plan
    val decoyRendered = rendered
    val prompt = LlmNarrationSmoke.codexCliPrompt(decoyPlan, decoyRendered).get

    Vector(
      "renderedText: Bf2 decoys the piece to a8.",
      "claimKey: decoys_piece",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"Decoy prompt must include allowed input: $required")

    Vector(
      "Story(",
      "raw Story",
      "DecoyProof",
      "TrapProof",
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
      "b6a8",
      "same-board proof"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"Decoy prompt must not expose $forbiddenInput")

    assertEquals(LlmNarrationSmoke.mockNarrate(decoyPlan, decoyRendered), Some("Bf2 decoys the piece to a8."))
    assertEquals(
      LlmNarrationSmoke.check(decoyPlan, decoyRendered, "Bf2 decoys the piece to a8."),
      NarrationSmokeCheck(true, Vector.empty)
    )

  test("LLM smoke rejects Decoy material force sibling tactic engine and raw reply additions"):
    val decoyPlan = plan
    val decoyRendered = rendered

    Vector(
      "wins material" -> "Bf2 decoys the piece to a8 and wins material.",
      "wins piece" -> "Bf2 decoys the piece to a8 and wins a piece.",
      "forced" -> "Bf2 decoys the piece to a8 with a forced reply.",
      "only" -> "Bf2 is the only move.",
      "best" -> "Bf2 is the best move.",
      "cannot refuse" -> "Black cannot refuse the decoy.",
      "no escape" -> "Bf2 decoys the piece to a8 with no escape.",
      "no counterplay" -> "Bf2 decoys the piece to a8 with no counterplay.",
      "traps piece" -> "Bf2 traps the piece on a8.",
      "removes defender" -> "Bf2 removes the defender.",
      "overloads defender" -> "Bf2 overloads the defender.",
      "deflects defender" -> "Bf2 deflects the defender.",
      "engine line" -> "The engine line shows Bf2 decoys the piece to a8.",
      "raw reply coordinate" -> "After b6a8, Bf2 decoys the piece to a8.",
      "raw reply explanation" -> "Bf2 works because the rival reply moves the knight to a8."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(decoyPlan, decoyRendered, output)
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
        s"$label must be rejected as an added Decoy smoke claim: $result"
      )

  test("LLM smoke refuses non-Lead mismatched and raw Decoy input surfaces"):
    val decoyPlan = plan
    val decoyRendered = rendered
    val supportPlan = decoyPlan.copy(role = Role.Support, allowedClaim = None)
    val contextPlan = decoyPlan.copy(role = Role.Context, allowedClaim = None)
    val blockedPlan = decoyPlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)
    val mismatchedRendered = decoyRendered.copy(claimKey = "traps_piece")

    Vector(
      "support" -> supportPlan,
      "context" -> contextPlan,
      "blocked" -> blockedPlan
    ).foreach: (label, malformed) =>
      assertEquals(LlmNarrationSmoke.mockNarrate(malformed, decoyRendered), None, label)
      assertEquals(LlmNarrationSmoke.codexCliPrompt(malformed, decoyRendered), None, label)

    assertEquals(LlmNarrationSmoke.mockNarrate(decoyPlan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(decoyPlan, mismatchedRendered), None)

    val methodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector(
      "fromStory",
      "fromDecoyProof",
      "fromTrapProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine",
      "fromReplyMap",
      "callApi",
      "productionApi"
    ).foreach: method =>
      assert(!methodNames.contains(method), s"Decoy LLM smoke must not expose $method")
    Vector("Story", "DecoyProof", "TrapProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach: parameter =>
      assert(!parameterNames.contains(parameter), s"Decoy LLM smoke must not accept $parameter")

  private def plan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

  private def rendered: RenderedLine =
    DeterministicRenderer.fromPlan(plan).get

  private def story: Story =
    TacticDecoy
      .write(
        facts,
        Some(route),
        Some(reply),
        Some(namedPieceBeforeReply),
        Some(target),
        Some(completeTrapFollowUp)
      )
      .get

  private def completeTrapFollowUp: TrapProof =
    TrapProof.fromBoardFacts(afterReplyTrapFacts, trapMove)

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Decoy LLM smoke FEN: $fen -> $error"), identity)

  private val facts = board("4k3/8/1nb5/8/8/8/8/R3B1K1 w - - 0 1")
  private val afterReplyTrapFacts = board("n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1")
  private val route = Line(Square('e', 1), Square('f', 2))
  private val reply = Line(Square('b', 6), Square('a', 8))
  private val namedPieceBeforeReply = Square('b', 6)
  private val target = Square('a', 8)
  private val trapMove = Line(Square('a', 1), Square('a', 7))
