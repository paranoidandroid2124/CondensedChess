package lila.commentary.chess

class TacticOverloadStage8Test extends munit.FunSuite:

  private val overloadFen = "7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1"
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val defender = Square('e', 6)
  private val firstDutyTarget = Square('e', 7)
  private val secondDutyTarget = Square('a', 6)

  test("Stage-8 LLM smoke preserves overloads_defender meaning from rendered line only"):
    val (plan, rendered) = overloadPlanAndRendered
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(rendered.claimKey, "overloads_defender")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, "This move overloads the defender.").accepted, true)
    assertEquals(LlmNarrationSmoke.check(plan, rendered, "The move overloads the defender.").accepted, true)
    assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."))
    assert(prompt.contains("renderedText: This move overloads the defender."))
    assert(prompt.contains("claimKey: overloads_defender"))
    assert(prompt.contains("strength: bounded"))
    assert(prompt.contains("forbiddenWording:"))

    Vector(
      overloadFen,
      "BoardFacts",
      "FEN",
      "OverloadProof",
      "proof",
      "source row",
      "source rows",
      "EngineCheck",
      "engine line",
      "raw PV",
      "eval",
      "beforeBoard",
      "afterBoard",
      "DefenderDuty",
      "CannotSatisfyBoth"
    ).foreach: forbidden =>
      assert(!prompt.contains(forbidden), forbidden)

  test("Stage-8 LLM smoke rejects wins material forced best only and decisive wording"):
    val (plan, rendered) = overloadPlanAndRendered

    Vector(
      "This move overloads the defender and wins material.",
      "This forced move overloads the defender.",
      "This is the best move because it overloads the defender.",
      "This is the only move that overloads the defender.",
      "This move decisively overloads the defender.",
      "This winning move overloads the defender."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("Stage-8 LLM smoke rejects deflect decoy and remove-guard wording"):
    val (plan, rendered) = overloadPlanAndRendered

    Vector(
      "This move deflects the defender.",
      "This move is a deflection.",
      "This move decoys the defender.",
      "This move is a decoy.",
      "This move removes the defender.",
      "This move is a remove-guard tactic."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("Stage-8 LLM smoke cannot mention raw engine data"):
    val (plan, rendered) = overloadPlanAndRendered

    Vector(
      "The engine says this move overloads the defender.",
      "This move overloads the defender according to the eval.",
      "The PV shows this move overloads the defender.",
      "At +1.3, this move overloads the defender.",
      "This move overloads the defender by 120 centipawns."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  private def overloadPlanAndRendered: (ExplanationPlan, RenderedLine) =
    val story = overloadStory(board(overloadFen)).copy(proof = orderingProof)
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    (plan, rendered)

  private def overloadStory(facts: BoardFacts): Story =
    TacticOverload
      .write(
        facts,
        Some(overloadMove),
        Some(defender),
        Some(firstDutyTarget),
        Some(secondDutyTarget),
        Some(firstDutyTarget)
      )
      .get

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-8 FEN: $fen -> $error"), identity)

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
