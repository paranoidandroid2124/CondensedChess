package lila.commentary.chess

class LoosePieceStage8Test extends munit.FunSuite:

  private val looseFen = "4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"
  private val looseMove = Line(Square('d', 2), Square('h', 2))

  test("Stage-8 LLM smoke accepts only bounded Loose RenderedLine input"):
    val loosePlan = plan
    val looseRendered = rendered
    val prompt = LlmNarrationSmoke.codexCliPrompt(loosePlan, looseRendered).get

    Vector(
      "renderedText: Rh2 attacks the undefended piece on h5.",
      "claimKey: attacks_loose_piece",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"Stage-8 prompt must include allowed input: $required")

    Vector(
      "Story(",
      "raw Story",
      "LoosePieceProof",
      "BoardFacts",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "raw PV",
      "principal variation",
      "proofFailures",
      "source row",
      "target piece identity",
      "target piece square",
      "attacking piece square",
      "legal defender",
      "exact after-board replay",
      "same-board proof"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"Stage-8 prompt must not expose $forbiddenInput")

    assertEquals(LlmNarrationSmoke.mockNarrate(loosePlan, looseRendered), Some("Rh2 attacks the undefended piece on h5."))
    assertEquals(
      LlmNarrationSmoke.check(loosePlan, looseRendered, "Rh2 attacks the undefended piece on h5."),
      NarrationSmokeCheck(true, Vector.empty)
    )
    assertEquals(
      LlmNarrationSmoke.check(loosePlan, looseRendered, "Rh2 hits the undefended piece on h5.").accepted,
      true
    )

  test("Stage-8 LLM smoke rejects Loose proof engine and source additions"):
    val loosePlan = plan
    val looseRendered = rendered

    Vector(
      "raw Story" -> "Raw Story says Rh2 attacks the undefended piece on h5.",
      "LoosePieceProof" -> "LoosePieceProof proves Rh2 attacks the undefended piece.",
      "BoardFacts" -> "BoardFacts show Rh2 attacks the undefended piece.",
      "EngineCheck" -> "EngineCheck supports Rh2.",
      "EngineLine" -> "Engine line starts with Rh2.",
      "EngineEval" -> "EngineEval is +1.2 after Rh2.",
      "raw PV" -> "Raw PV: Rh2 Kf8.",
      "proofFailures" -> "proofFailures are empty, so Rh2 attacks the undefended piece.",
      "source rows" -> "The source rows say this is a loose piece."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(loosePlan, looseRendered, output)
      assertEquals(result.accepted, false, label)
      assert(
        result.violations.contains("raw_input") ||
          result.violations.contains("engine_mention") ||
          result.violations.contains("new_move_or_line"),
        s"$label: $result"
      )

  test("Stage-8 LLM smoke rejects Loose overclaims new moves and new variations"):
    val loosePlan = plan
    val looseRendered = rendered

    Vector(
      "wins piece" -> "Rh2 attacks the undefended piece and wins the piece.",
      "wins material" -> "Rh2 attacks the undefended piece and wins material.",
      "hanging" -> "Rh2 attacks the hanging piece on h5.",
      "free piece" -> "Rh2 attacks a free piece on h5.",
      "en prise" -> "Rh2 attacks the piece en prise.",
      "underdefended" -> "Rh2 attacks the underdefended piece on h5.",
      "overloaded" -> "Rh2 attacks the overloaded defender on h5.",
      "pressure" -> "Rh2 creates pressure on the piece.",
      "initiative" -> "Rh2 takes the initiative.",
      "tempo" -> "Rh2 attacks the piece with tempo.",
      "engine line" -> "The engine line is Rh2 Kf8.",
      "best move" -> "Rh2 is the best move.",
      "only move" -> "Rh2 is the only move.",
      "forced move" -> "Rh2 is a forced move.",
      "decisive" -> "Rh2 is decisive.",
      "winning" -> "Rh2 is winning.",
      "new move" -> "Nf5 also attacks the undefended piece.",
      "new variation" -> "In a new variation, Rh2 Kf8 keeps the piece undefended."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(loosePlan, looseRendered, output)
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

  test("Stage-8 LLM smoke rejects Support Context Blocked mismatched and raw Loose input surfaces"):
    val loosePlan = plan
    val looseRendered = rendered
    val supportPlan = loosePlan.copy(role = Role.Support, allowedClaim = None)
    val contextPlan = loosePlan.copy(role = Role.Context, allowedClaim = None)
    val blockedPlan = loosePlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)
    val mismatchedRendered = looseRendered.copy(claimKey = "wins_piece")

    Vector(
      "support" -> supportPlan,
      "context" -> contextPlan,
      "blocked" -> blockedPlan
    ).foreach: (label, malformed) =>
      assertEquals(LlmNarrationSmoke.mockNarrate(malformed, looseRendered), None, label)
      assertEquals(LlmNarrationSmoke.codexCliPrompt(malformed, looseRendered), None, label)

    assertEquals(LlmNarrationSmoke.mockNarrate(loosePlan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(loosePlan, mismatchedRendered), None)

    val methodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector(
      "fromStory",
      "fromLoosePieceProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine",
      "callApi",
      "productionApi"
    ).foreach: method =>
      assert(!methodNames.contains(method), s"Stage-8 LLM smoke must not expose $method")
    Vector("Story", "LoosePieceProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach: parameter =>
      assert(!parameterNames.contains(parameter), s"Stage-8 LLM smoke must not accept $parameter")

  private def facts: BoardFacts =
    BoardFacts.fromFen(looseFen).fold(error => fail(s"invalid Stage-8 FEN: $looseFen -> $error"), identity)

  private def story: Story =
    TacticLoose.write(facts, Some(looseMove)).get

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
