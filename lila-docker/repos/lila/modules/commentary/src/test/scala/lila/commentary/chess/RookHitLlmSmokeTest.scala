package lila.commentary.chess

class RookHitLlmSmokeTest extends munit.FunSuite:

  private val rookHitFen = "4k3/8/8/7r/8/8/3R4/4K3 w - - 0 1"
  private val rookHitMove = Line(Square('d', 2), Square('h', 2))
  private val rookHitTarget = Square('h', 5)

  test("LLM smoke accepts only bounded RookHit RenderedLine input"):
    val rookHitPlan = plan
    val rookHitRendered = rendered
    val prompt = LlmNarrationSmoke.codexCliPrompt(rookHitPlan, rookHitRendered).get

    Vector(
      "renderedText: Rh2 attacks the rook on h5.",
      "claimKey: attacks_rook",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"RookHit prompt must include allowed input: $required")

    Vector(
      "Story(",
      "raw Story",
      "RookHitProof",
      "BoardFacts",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "raw PV",
      "principal variation",
      "route:",
      "evidenceLine",
      "proofFailures",
      "source row",
      "rival rook square",
      "attacking piece square",
      "exact after-board replay",
      "same-board proof"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"RookHit prompt must not expose $forbiddenInput")

    assertEquals(LlmNarrationSmoke.mockNarrate(rookHitPlan, rookHitRendered), Some("Rh2 attacks the rook on h5."))
    assertEquals(
      LlmNarrationSmoke.check(rookHitPlan, rookHitRendered, "Rh2 attacks the rook on h5."),
      NarrationSmokeCheck(true, Vector.empty)
    )
    assertEquals(
      LlmNarrationSmoke.check(rookHitPlan, rookHitRendered, "Rh2 hits the rook on h5.").accepted,
      true
    )

  test("LLM smoke rejects RookHit proof engine and source additions"):
    val rookHitPlan = plan
    val rookHitRendered = rendered

    Vector(
      "raw Story" -> "Raw Story says Rh2 attacks the rook on h5.",
      "RookHitProof" -> "RookHitProof proves Rh2 attacks the rook.",
      "BoardFacts" -> "BoardFacts show Rh2 attacks the rook.",
      "EngineCheck" -> "EngineCheck supports Rh2.",
      "EngineLine" -> "Engine line starts with Rh2.",
      "EngineEval" -> "EngineEval is +1.2 after Rh2.",
      "raw PV" -> "Raw PV: Rh2 Kf8.",
      "proofFailures" -> "proofFailures are empty, so Rh2 attacks the rook.",
      "source rows" -> "The source rows say this is a rook hit."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(rookHitPlan, rookHitRendered, output)
      assertEquals(result.accepted, false, label)
      assert(
        result.violations.contains("raw_input") ||
          result.violations.contains("engine_mention") ||
          result.violations.contains("new_move_or_line"),
        s"$label: $result"
      )

  test("LLM smoke rejects RookHit overclaims sibling tactics new moves and new variations"):
    val rookHitPlan = plan
    val rookHitRendered = rendered

    Vector(
      "wins rook" -> "Rh2 attacks the rook and wins the rook.",
      "wins material" -> "Rh2 attacks the rook and wins material.",
      "wins exchange" -> "Rh2 wins the exchange by attacking the rook.",
      "hanging rook" -> "Rh2 attacks the hanging rook on h5.",
      "loose rook" -> "Rh2 attacks the loose rook on h5.",
      "high value" -> "Rh2 attacks a high-value piece on h5.",
      "major piece" -> "Rh2 attacks a major piece on h5.",
      "queen" -> "Rh2 attacks the queen on h5.",
      "fork" -> "Rh2 forks the rook and king.",
      "skewer" -> "Rh2 skewers the rook.",
      "pin" -> "Rh2 pins the rook.",
      "discovered attack" -> "Rh2 creates a discovered attack on the rook.",
      "best move" -> "Rh2 is the best move.",
      "only move" -> "Rh2 is the only move.",
      "forced" -> "Rh2 is forced.",
      "new move" -> "Nf5 also attacks the rook.",
      "new variation" -> "In a new variation, Rh2 Kf8 keeps attacking the rook."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(rookHitPlan, rookHitRendered, output)
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

  test("LLM smoke rejects Support Context Blocked mismatched and raw RookHit input surfaces"):
    val rookHitPlan = plan
    val rookHitRendered = rendered
    val supportPlan = rookHitPlan.copy(role = Role.Support, allowedClaim = None)
    val contextPlan = rookHitPlan.copy(role = Role.Context, allowedClaim = None)
    val blockedPlan = rookHitPlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)
    val mismatchedRendered = rookHitRendered.copy(claimKey = "wins_rook")

    Vector(
      "support" -> supportPlan,
      "context" -> contextPlan,
      "blocked" -> blockedPlan
    ).foreach: (label, malformed) =>
      assertEquals(LlmNarrationSmoke.mockNarrate(malformed, rookHitRendered), None, label)
      assertEquals(LlmNarrationSmoke.codexCliPrompt(malformed, rookHitRendered), None, label)

    assertEquals(LlmNarrationSmoke.mockNarrate(rookHitPlan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(rookHitPlan, mismatchedRendered), None)

    val methodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector(
      "fromStory",
      "fromRookHitProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine",
      "callApi",
      "productionApi"
    ).foreach: method =>
      assert(!methodNames.contains(method), s"RookHit LLM smoke must not expose $method")
    Vector("Story", "RookHitProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach: parameter =>
      assert(!parameterNames.contains(parameter), s"RookHit LLM smoke must not accept $parameter")

  private def facts: BoardFacts =
    BoardFacts.fromFen(rookHitFen).fold(error => fail(s"invalid RookHit LLM FEN: $rookHitFen -> $error"), identity)

  private def story: Story =
    TacticRookHit.write(facts, Some(rookHitMove), Some(rookHitTarget)).get

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
