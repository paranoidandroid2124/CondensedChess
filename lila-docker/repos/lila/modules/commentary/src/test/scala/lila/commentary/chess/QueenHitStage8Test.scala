package lila.commentary.chess

class QueenHitStage8Test extends munit.FunSuite:

  private val queenHitFen = "4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1"
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))

  test("Stage-8 LLM smoke accepts only bounded QueenHit RenderedLine input"):
    val queenHitPlan = plan
    val queenHitRendered = rendered
    val prompt = LlmNarrationSmoke.codexCliPrompt(queenHitPlan, queenHitRendered).get

    Vector(
      "renderedText: Rh2 attacks the queen on h5.",
      "claimKey: attacks_queen",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"Stage-8 prompt must include allowed input: $required")

    Vector(
      "Story(",
      "raw Story",
      "QueenHitProof",
      "BoardFacts",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "raw PV",
      "principal variation",
      "proofFailures",
      "source row",
      "rival queen square",
      "attacking piece square",
      "exact after-board replay",
      "same-board proof"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"Stage-8 prompt must not expose $forbiddenInput")

    assertEquals(LlmNarrationSmoke.mockNarrate(queenHitPlan, queenHitRendered), Some("Rh2 attacks the queen on h5."))
    assertEquals(
      LlmNarrationSmoke.check(queenHitPlan, queenHitRendered, "Rh2 attacks the queen on h5."),
      NarrationSmokeCheck(true, Vector.empty)
    )
    assertEquals(
      LlmNarrationSmoke.check(queenHitPlan, queenHitRendered, "Rh2 hits the queen on h5.").accepted,
      true
    )

  test("Stage-8 LLM smoke rejects QueenHit proof engine and source additions"):
    val queenHitPlan = plan
    val queenHitRendered = rendered

    Vector(
      "raw Story" -> "Raw Story says Rh2 attacks the queen on h5.",
      "QueenHitProof" -> "QueenHitProof proves Rh2 attacks the queen.",
      "BoardFacts" -> "BoardFacts show Rh2 attacks the queen.",
      "EngineCheck" -> "EngineCheck supports Rh2.",
      "EngineLine" -> "Engine line starts with Rh2.",
      "EngineEval" -> "EngineEval is +1.2 after Rh2.",
      "raw PV" -> "Raw PV: Rh2 Kf8.",
      "proofFailures" -> "proofFailures are empty, so Rh2 attacks the queen.",
      "source rows" -> "The source rows say this is a queen hit."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(queenHitPlan, queenHitRendered, output)
      assertEquals(result.accepted, false, label)
      assert(
        result.violations.contains("raw_input") ||
          result.violations.contains("engine_mention") ||
          result.violations.contains("new_move_or_line"),
        s"$label: $result"
      )

  test("Stage-8 LLM smoke rejects QueenHit overclaims new moves and new variations"):
    val queenHitPlan = plan
    val queenHitRendered = rendered

    Vector(
      "wins queen" -> "Rh2 attacks the queen and wins the queen.",
      "queen trap" -> "Rh2 traps the queen on h5.",
      "queen is lost" -> "After Rh2, the queen is lost.",
      "tempo" -> "Rh2 attacks the queen with tempo.",
      "material gain" -> "Rh2 attacks the queen for material gain.",
      "engine line" -> "The engine line is Rh2 Kf8.",
      "best move" -> "Rh2 is the best move.",
      "only move" -> "Rh2 is the only move.",
      "forced move" -> "Rh2 is a forced move.",
      "decisive" -> "Rh2 is decisive.",
      "winning" -> "Rh2 is winning.",
      "new move" -> "Nf5 also attacks the queen.",
      "new variation" -> "In a new variation, Rh2 Kf8 keeps attacking the queen."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(queenHitPlan, queenHitRendered, output)
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

  test("Stage-8 LLM smoke rejects Support Context Blocked mismatched and raw QueenHit input surfaces"):
    val queenHitPlan = plan
    val queenHitRendered = rendered
    val supportPlan = queenHitPlan.copy(role = Role.Support, allowedClaim = None)
    val contextPlan = queenHitPlan.copy(role = Role.Context, allowedClaim = None)
    val blockedPlan = queenHitPlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)
    val mismatchedRendered = queenHitRendered.copy(claimKey = "wins_queen")

    Vector(
      "support" -> supportPlan,
      "context" -> contextPlan,
      "blocked" -> blockedPlan
    ).foreach: (label, malformed) =>
      assertEquals(LlmNarrationSmoke.mockNarrate(malformed, queenHitRendered), None, label)
      assertEquals(LlmNarrationSmoke.codexCliPrompt(malformed, queenHitRendered), None, label)

    assertEquals(LlmNarrationSmoke.mockNarrate(queenHitPlan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(queenHitPlan, mismatchedRendered), None)

    val methodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector(
      "fromStory",
      "fromQueenHitProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine",
      "callApi",
      "productionApi"
    ).foreach: method =>
      assert(!methodNames.contains(method), s"Stage-8 LLM smoke must not expose $method")
    Vector("Story", "QueenHitProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach: parameter =>
      assert(!parameterNames.contains(parameter), s"Stage-8 LLM smoke must not accept $parameter")

  private def facts: BoardFacts =
    BoardFacts.fromFen(queenHitFen).fold(error => fail(s"invalid Stage-8 FEN: $queenHitFen -> $error"), identity)

  private def story: Story =
    TacticQueenHit.write(facts, Some(queenHitMove)).get

  private def leadStory: Story =
    story.copy(proof = orderingProof)

  private def plan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(leadStory)).head).get

  private def rendered: RenderedLine =
    DeterministicRenderer.fromPlan(plan).get

  private def orderingProof: Proof =
    Proof(
      boardProof = 99,
      lineProof = 99,
      ownerProof = 99,
      anchorProof = 99,
      routeProof = 99,
      persistence = 99,
      immediacy = 99,
      forcing = 99,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = 99,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 99
    )
