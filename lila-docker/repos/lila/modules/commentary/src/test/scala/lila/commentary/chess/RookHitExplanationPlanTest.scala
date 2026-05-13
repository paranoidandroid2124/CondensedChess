package lila.commentary.chess

class RookHitExplanationPlanTest extends munit.FunSuite:

  private val rookHitFen = "4k3/8/8/7r/8/8/3R4/4K3 w - - 0 1"
  private val rookHitMove = Line(Square('d', 2), Square('h', 2))
  private val rookHitTarget = Square('h', 5)
  private val replyMove = Line(Square('e', 8), Square('e', 7))

  test("ExplanationPlan lowers only selected uncapped Lead RookHit to attacks_rook"):
    val selected = verdict(leadStory)
    val plan = ExplanationPlan.fromSelected(selected).get
    val forbiddenClaimKeys = Vector(
      "wins_rook",
      "wins_material",
      "wins_exchange",
      "hanging_rook",
      "loose_rook",
      "trapped_rook",
      "forced",
      "only_move",
      "best_move",
      "high_value_piece",
      "major_piece",
      "attacks_queen"
    )

    assertEquals(selected.role, Role.Lead)
    assertEquals(selected.leadAllowed, true)
    assertEquals(selected.selected, true)
    assertEquals(selected.engineStrengthLimited, false)
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.RookHit))
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(rookHitTarget))
    assertEquals(plan.anchor, Some(Square('h', 2)))
    assertEquals(plan.route, Some(rookHitMove))
    assertEquals(plan.routeSan, Some("Rh2"))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.AttacksRook))
    assertEquals(plan.allowedClaim.map(_.key), Some("attacks_rook"))
    assertEquals(plan.evidenceLine, Some(rookHitMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    forbiddenClaimKeys.foreach: key =>
      assertEquals(plan.allowedClaim.exists(_.key == key), false, s"RookHit allowed forbidden claim key $key")
      assert(plan.forbiddenWording.map(_.key).contains(key), s"RookHit plan must forbid $key")

  test("RookHit plan requires clean same-board proof-backed Story identity"):
    val base = leadStory
    val selected = verdict(base)
    val proof = base.rookHitProof.get

    Vector(
      "writerless" -> base.copy(writer = None),
      "missing proof" -> base.copy(rookHitProof = None),
      "incomplete StoryProof" -> base.copy(storyProof = StoryProof.empty),
      "contaminated scene" -> base.copy(scene = Scene.Material, tactic = None),
      "contaminated plan" -> base.copy(plan = Some(Plan.OpenFile)),
      "target mismatch" -> base.copy(target = Some(Square('h', 4))),
      "anchor mismatch" -> base.copy(anchor = Some(Square('d', 2))),
      "route mismatch" -> base.copy(route = Some(Line(Square('d', 2), Square('d', 8)))),
      "secondary target contaminated" -> base.copy(secondaryTarget = Some(Square('a', 1))),
      "borrowed QueenHit proof" ->
        base.copy(queenHitProof = TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get.queenHitProof),
      "incomplete RookHitProof" -> base.copy(
        rookHitProof = Some(
          proof.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("RookHitProof", Vector("after-board rook attack"))))
        )
      )
    ).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(selected.copy(story = row)), None, label)

  test("Support Context Blocked capped and refuted RookHit rows stay silent"):
    val selected = verdict(leadStory)
    Vector(
      "unselected" -> selected.copy(selected = false),
      "support" -> selected.copy(role = Role.Support, leadAllowed = false),
      "context" -> selected.copy(role = Role.Context, leadAllowed = false),
      "blocked" -> selected.copy(role = Role.Blocked, leadAllowed = false)
    ).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(row), None, label)

    val lowProof = StoryTable.choose(Vector(story.copy(proof = story.proof.copy(boardProof = 60, lineProof = 60)))).head
    val capped = verdict(TacticRookHit.withEngineCheck(leadStory, engineCheck(EngineCheckStatus.Caps, leadStory)).get)
    val refuted =
      StoryTable.choose(Vector(leadStory.copy(engineCheck = Some(engineCheck(EngineCheckStatus.Refutes, leadStory))))).head

    assertEquals(lowProof.role, Role.Context)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(lowProof), None, "context")
    assertEquals(ExplanationPlan.fromSelected(capped), None, "capped")
    assertEquals(ExplanationPlan.fromSelected(refuted), None, "refuted")

  private def facts: BoardFacts =
    BoardFacts.fromFen(rookHitFen).fold(error => fail(s"invalid RookHit ExplanationPlan FEN: $rookHitFen -> $error"), identity)

  private def story: Story =
    TacticRookHit.write(facts, Some(rookHitMove), Some(rookHitTarget)).get

  private def leadStory: Story =
    story.copy(proof = orderingProof)

  private def verdict(row: Story): Verdict =
    StoryTable.choose(Vector(row)).head

  private def engineCheck(status: EngineCheckStatus, row: Story): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(rookHitMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(if status == EngineCheckStatus.Refutes then 200 else 0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def queenHitFacts: BoardFacts =
    BoardFacts.fromFen("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1").fold(error => fail(s"invalid QueenHit FEN: $error"), identity)

  private val queenHitMove = Line(Square('d', 2), Square('h', 2))

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
