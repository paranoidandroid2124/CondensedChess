package lila.commentary.chess

class QueenHitStage6Test extends munit.FunSuite:

  private val queenHitFen = "4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1"
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))
  private val replyMove = Line(Square('e', 8), Square('e', 7))

  test("Stage-6 ExplanationPlan lowers only selected uncapped Lead QueenHit to attacks_queen"):
    val selected = verdict(leadStory)
    assertEquals(selected.role, Role.Lead)
    assertEquals(selected.leadAllowed, true)
    assertEquals(selected.selected, true)
    assertEquals(selected.engineStrengthLimited, false)
    val plan = ExplanationPlan.fromSelected(selected).get
    val forbiddenClaimKeys = Vector(
      "wins_queen",
      "traps_queen",
      "gains_tempo",
      "wins_material",
      "best_move",
      "only_move",
      "forced_move",
      "decisive",
      "winning"
    )

    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.QueenHit))
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('h', 5)))
    assertEquals(plan.anchor, Some(Square('h', 2)))
    assertEquals(plan.route, Some(queenHitMove))
    assertEquals(plan.routeSan, Some("Rh2"))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim.map(_.key), Some("attacks_queen"))
    assertEquals(plan.evidenceLine, Some(queenHitMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    forbiddenClaimKeys.foreach: key =>
      assertEquals(plan.allowedClaim.exists(_.key == key), false, s"QueenHit allowed forbidden claim key $key")
      assert(plan.forbiddenWording.map(_.key).contains(key), s"QueenHit plan must forbid $key")

  test("Stage-6 QueenHit plan requires clean same-board proof-backed Story identity"):
    val base = leadStory
    val selected = verdict(base)
    val proof = base.queenHitProof.get

    Vector(
      "writerless" -> base.copy(writer = None),
      "missing proof" -> base.copy(queenHitProof = None),
      "incomplete StoryProof" -> base.copy(storyProof = StoryProof.empty),
      "contaminated scene" -> base.copy(scene = Scene.Material, tactic = None),
      "contaminated plan" -> base.copy(plan = Some(Plan.OpenFile)),
      "target mismatch" -> base.copy(target = Some(Square('h', 4))),
      "anchor mismatch" -> base.copy(anchor = Some(Square('d', 2))),
      "route mismatch" -> base.copy(route = Some(Line(Square('d', 2), Square('d', 8)))),
      "incomplete QueenHitProof" -> base.copy(
        queenHitProof = Some(
          proof.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("QueenHitProof", Vector("exact after-board replay"))))
        )
      )
    ).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(selected.copy(story = row)), None, label)

  test("Stage-6 Support Context Blocked capped and refuted QueenHit rows stay silent"):
    val selected = verdict(leadStory)
    Vector(
      "unselected" -> selected.copy(selected = false),
      "support" -> selected.copy(role = Role.Support, leadAllowed = false),
      "context" -> selected.copy(role = Role.Context, leadAllowed = false),
      "blocked" -> selected.copy(role = Role.Blocked, leadAllowed = false)
    ).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(row), None, label)

    val lowProof = StoryTable.choose(Vector(story.copy(proof = story.proof.copy(boardProof = 60, lineProof = 60)))).head
    val capped = verdict(TacticQueenHit.withEngineCheck(leadStory, engineCheck(EngineCheckStatus.Caps, leadStory)).get)
    val refuted =
      verdict(TacticQueenHit.withEngineCheck(leadStory, engineCheck(EngineCheckStatus.Supports, leadStory, before = 220, after = 20)).get)

    assertEquals(lowProof.role, Role.Context)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(lowProof), None, "context")
    assertEquals(ExplanationPlan.fromSelected(capped), None, "capped")
    assertEquals(ExplanationPlan.fromSelected(refuted), None, "refuted")

  private def facts: BoardFacts =
    BoardFacts.fromFen(queenHitFen).fold(error => fail(s"invalid Stage-6 FEN: $queenHitFen -> $error"), identity)

  private def story: Story =
    TacticQueenHit.write(facts, Some(queenHitMove)).get

  private def leadStory: Story =
    story.copy(proof = orderingProof)

  private def verdict(row: Story): Verdict =
    StoryTable.choose(Vector(row)).head

  private def engineCheck(status: EngineCheckStatus, row: Story, before: Int = 20, after: Int = 20): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(queenHitMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

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
