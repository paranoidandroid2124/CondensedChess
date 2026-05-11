package lila.commentary.chess

class TacticOverloadStage7Test extends munit.FunSuite:

  private val overloadFen = "7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1"
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val defender = Square('e', 6)
  private val firstDutyTarget = Square('e', 7)
  private val secondDutyTarget = Square('a', 6)

  test("Stage-7 overloads_defender renders one bounded sentence"):
    val rendered = overloadRenderedLine

    assertEquals(rendered.text, "This move overloads the defender.")
    assertEquals(rendered.claimKey, "overloads_defender")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)

  test("Stage-7 overload renderer forbidden wording scan passes"):
    val rendered = overloadRenderedLine
    val forbidden =
      Vector(
        "wins material",
        "wins a piece",
        "wins the queen",
        "forced",
        "best move",
        "only move",
        "decisive",
        "winning",
        "no counterplay",
        "deflects",
        "decoys",
        "removes the defender",
        "engine",
        "eval",
        "pv",
        "principal variation"
      )
    val normalized = rendered.text.toLowerCase

    forbidden.foreach: phrase =>
      assert(!normalized.contains(phrase), phrase)
    assertEquals(rendered.forbiddenCheckPassed, true)

  test("Stage-7 capped refuted support and context rows render no standalone Overload text"):
    val facts = board(overloadFen)
    val overload = overloadStory(facts).copy(proof = orderingProof)
    val support = StoryTable.choose(Vector(overload, overload)).find(_.role == Role.Support).get
    val context = StoryTable.choose(Vector(overload.copy(proof = lowProof))).head
    val capped = StoryTable
      .choose(Vector(TacticOverload.withEngineCheck(overload, check(facts, overload, EngineCheckStatus.Caps)).get))
      .head
    val refuted = StoryTable
      .choose(Vector(TacticOverload.withEngineCheck(overload, check(facts, overload, EngineCheckStatus.Refutes)).get))
      .head

    assertEquals(support.role, Role.Support)
    assertEquals(context.role, Role.Context)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    Vector(support, context, capped, refuted).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, verdict.toString)

  private def overloadRenderedLine: RenderedLine =
    val story = overloadStory(board(overloadFen)).copy(proof = orderingProof)
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    DeterministicRenderer.fromPlan(plan).get

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

  private def check(facts: BoardFacts, story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(overloadMove))),
      replyLine = Some(EngineLine(Vector(overloadMove))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(if status == EngineCheckStatus.Supports then 300 else -50)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-7 FEN: $fen -> $error"), identity)

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

  private def lowProof: Proof =
    orderingProof.copy(boardProof = 60, lineProof = 60, ownerProof = 60, anchorProof = 60, routeProof = 60)
