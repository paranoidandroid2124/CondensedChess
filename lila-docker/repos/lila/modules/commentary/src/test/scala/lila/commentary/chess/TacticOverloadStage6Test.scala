package lila.commentary.chess

class TacticOverloadStage6Test extends munit.FunSuite:

  private val overloadFen = "7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1"
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val defender = Square('e', 6)
  private val firstDutyTarget = Square('e', 7)
  private val secondDutyTarget = Square('a', 6)

  test("Stage-6 selected uncapped Lead Tactic.Overload lowers to overloads_defender"):
    val story = overloadStory(board(overloadFen)).copy(proof = orderingProof)
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.selected, true)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes), false)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.OverloadsDefender))
    assertEquals(plan.allowedClaim.map(_.key), Some("overloads_defender"))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.side, Side.White)
    assertEquals(plan.rival, Side.Black)
    assertEquals(plan.target, Some(firstDutyTarget))
    assertEquals(plan.secondaryTarget, Some(secondDutyTarget))
    assertEquals(plan.anchor, Some(defender))
    assertEquals(plan.route, Some(overloadMove))
    assertEquals(plan.evidenceLine, Some(overloadMove))
    assert(plan.forbiddenWording.nonEmpty)

  test("Stage-6 Support Context Blocked capped and refuted Overload rows produce no standalone plan"):
    val facts = board(overloadFen)
    val overload = overloadStory(facts).copy(proof = orderingProof)
    val support = StoryTable.choose(Vector(overload, overload)).find(_.role == Role.Support).get
    val context = StoryTable.choose(Vector(overload.copy(proof = lowProof))).head
    val blocked = StoryTable.choose(Vector(overload.copy(overloadProof = None))).head
    val capped = StoryTable
      .choose(Vector(TacticOverload.withEngineCheck(overload, check(facts, overload, EngineCheckStatus.Caps)).get))
      .head
    val refuted = StoryTable
      .choose(Vector(TacticOverload.withEngineCheck(overload, check(facts, overload, EngineCheckStatus.Refutes)).get))
      .head

    assertEquals(support.role, Role.Support)
    assertEquals(context.role, Role.Context)
    assertEquals(blocked.role, Role.Blocked)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    Vector(support, context, blocked, capped, refuted).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, verdict.toString)

  test("Stage-6 ExplanationPlan does not accept raw proof facts or EngineCheck as Overload admission"):
    val facts = board(overloadFen)
    val story = overloadStory(facts).copy(proof = orderingProof)
    val rawProofOnly = story.copy(writer = None)
    val engineOnly = story.copy(overloadProof = None, engineCheck = Some(check(facts, story, EngineCheckStatus.Supports)))

    assertEquals(StoryTable.choose(Vector(rawProofOnly)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(engineOnly)).head.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(rawProofOnly)).head), None)
    assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(engineOnly)).head), None)

  test("Stage-6 no wins-material result claim enters Overload ExplanationPlan"):
    val story = overloadStory(board(overloadFen)).copy(proof = orderingProof)
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val resultClaims =
      Vector(
        ExplanationClaim.CanWinPiece,
        ExplanationClaim.PieceCanBeTakenWithGain,
        ExplanationClaim.CaptureLeavesMaterialGain,
        ExplanationClaim.MaterialBalanceChanges,
        ExplanationClaim.LineLeavesMaterialGain,
        ExplanationClaim.ExchangeLeavesSideAhead,
        ExplanationClaim.RemovesDefender
      )

    assertEquals(plan.allowedClaim, Some(ExplanationClaim.OverloadsDefender))
    assert(!resultClaims.exists(claim => plan.allowedClaim.contains(claim)))
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.supportContextLinks, Vector.empty)
    assert(plan.forbiddenWording.contains(ForbiddenWording.WinsMaterial))

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
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-6 FEN: $fen -> $error"), identity)

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
