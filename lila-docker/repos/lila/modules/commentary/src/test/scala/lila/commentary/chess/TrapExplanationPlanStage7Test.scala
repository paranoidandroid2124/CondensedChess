package lila.commentary.chess

class TrapExplanationPlanStage7Test extends munit.FunSuite:

  test("Stage-7 selected uncapped Lead Trap lowers only to traps_piece"):
    val verdict = StoryTable.choose(Vector(trapStory)).head
    val plan = ExplanationPlan.fromSelected(verdict).get

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.Trap))
    assertEquals(plan.allowedClaim.map(_.key), Some("traps_piece"))
    assertEquals(plan.evidenceLine, Some(trapMove))
    assertEquals(plan.route, Some(trapMove))
    assertEquals(plan.routeSan, Some("Ra7"))
    assertEquals(plan.target, Some(Square('a', 8)))
    assertEquals(plan.anchor, Some(Square('a', 7)))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.supportContextLinks, Vector.empty)

  test("Stage-7 non-selected non-Lead capped and refuted Trap rows lower to no public claim"):
    val story = trapStory
    val support =
      StoryTable.choose(Vector(strongerQueenHit, story)).find(_.story.tactic.contains(Tactic.Trap)).get
    val context = support.copy(role = Role.Context, leadAllowed = false)
    val blocked = story.copy(target = Some(Square('b', 6)))
    val capped = TacticTrap.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Caps)).get
    val refuted = TacticTrap.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Refutes)).get
    val unknown = TacticTrap.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Unknown)).get
    val unselected = StoryTable.choose(Vector(story)).head.copy(selected = false)

    Vector(
      "support" -> support,
      "context" -> context,
      "blocked" -> StoryTable.choose(Vector(blocked)).head,
      "capped" -> StoryTable.choose(Vector(capped)).head,
      "refuted" -> StoryTable.choose(Vector(refuted)).head,
      "unknown" -> StoryTable.choose(Vector(unknown)).head,
      "unselected" -> unselected
    ).foreach: (label, verdict) =>
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("Stage-7 Trap plan carries forbidden wording but no raw proof engine or escape-map fields"):
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(trapStory)).head).get
    val forbidden = plan.forbiddenWording.map(_.key).toSet
    val exposedNames = plan.productElementNames.toSet

    Vector(
      "wins_piece",
      "wins_material",
      "forced",
      "only_move",
      "best_move",
      "no_escape",
      "cannot_be_saved",
      "no_counterplay",
      "queen_trap",
      "free_piece"
    ).foreach: key =>
      assert(forbidden.contains(key), key)

    Vector(
      "trapProof",
      "safeEscapeSquares",
      "unsafeTargetMoves",
      "engineCheck",
      "engineDiagnostics",
      "evalBefore",
      "evalAfter",
      "rawPv"
    ).foreach: closedField =>
      assertEquals(exposedNames.contains(closedField), false, closedField)

  private def engineCheck(story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = trapFacts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(trapMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(if status == EngineCheckStatus.Refutes then 200 else 0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Trap Stage-7 FEN: $fen -> $error"), identity)

  private def trapStory: Story =
    TacticTrap.write(trapFacts, Some(trapMove)).get

  private def strongerQueenHit: Story =
    TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get.copy(proof = proofScore(96))

  private def proofScore(score: Int): Proof =
    Proof(
      boardProof = score,
      lineProof = score,
      ownerProof = score,
      anchorProof = score,
      routeProof = score,
      persistence = score,
      immediacy = score,
      forcing = 0,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = score,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = score
    )

  private val trapFacts = board("n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1")
  private val trapMove = Line(Square('a', 1), Square('a', 7))
  private val queenHitFacts = board("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1")
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))
