package lila.commentary.chess

class LoosePieceStage6Test extends munit.FunSuite:

  private val looseFen = "4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"
  private val looseMove = Line(Square('d', 2), Square('h', 2))
  private val replyMove = Line(Square('e', 8), Square('e', 7))

  test("Stage-6 ExplanationPlan lowers only selected uncapped Lead Loose to attacks_loose_piece"):
    val selected = verdict(leadStory)
    assertEquals(selected.role, Role.Lead)
    assertEquals(selected.leadAllowed, true)
    assertEquals(selected.selected, true)
    assertEquals(selected.engineStrengthLimited, false)
    val plan = ExplanationPlan.fromSelected(selected).get
    val forbiddenClaimKeys = Vector(
      "hanging_piece",
      "wins_piece",
      "wins_material",
      "attacks_queen",
      "removes_defender",
      "gains_tempo",
      "creates_pressure",
      "best_move",
      "only_move",
      "forced_move",
      "decisive",
      "winning"
    )

    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.Loose))
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('h', 5)))
    assertEquals(plan.anchor, Some(Square('h', 2)))
    assertEquals(plan.route, Some(looseMove))
    assertEquals(plan.routeSan, Some("Rh2"))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim.map(_.key), Some("attacks_loose_piece"))
    assertEquals(plan.evidenceLine, Some(looseMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    forbiddenClaimKeys.foreach: key =>
      assertEquals(plan.allowedClaim.exists(_.key == key), false, s"Loose allowed forbidden claim key $key")
      assert(plan.forbiddenWording.map(_.key).contains(key), s"Loose plan must forbid $key")

  test("Stage-6 Loose plan requires clean same-board proof-backed Story identity"):
    val base = leadStory
    val selected = verdict(base)
    val proof = base.loosePieceProof.get

    Vector(
      "writerless" -> base.copy(writer = None),
      "missing proof" -> base.copy(loosePieceProof = None),
      "incomplete StoryProof" -> base.copy(storyProof = StoryProof.empty),
      "contaminated scene" -> base.copy(scene = Scene.Material, tactic = None),
      "contaminated plan" -> base.copy(plan = Some(Plan.OpenFile)),
      "target mismatch" -> base.copy(target = Some(Square('h', 4))),
      "anchor mismatch" -> base.copy(anchor = Some(Square('d', 2))),
      "route mismatch" -> base.copy(route = Some(Line(Square('d', 2), Square('d', 8)))),
      "defender evidence mismatch" -> base.copy(
        loosePieceProof = Some(proof.copy(rivalLegalDefendersAfter = Vector(Piece(Side.Black, Man.Knight, Square('f', 4)))))
      ),
      "incomplete LoosePieceProof" -> base.copy(
        loosePieceProof = Some(
          proof.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("LoosePieceProof", Vector("exact after-board replay"))))
        )
      )
    ).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(selected.copy(story = row)), None, label)

  test("Stage-6 Support Context Blocked capped and refuted Loose rows stay silent"):
    val selected = verdict(leadStory)
    Vector(
      "unselected" -> selected.copy(selected = false),
      "support" -> selected.copy(role = Role.Support, leadAllowed = false),
      "context" -> selected.copy(role = Role.Context, leadAllowed = false),
      "blocked" -> selected.copy(role = Role.Blocked, leadAllowed = false)
    ).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(row), None, label)

    val lowProof = StoryTable.choose(Vector(story.copy(proof = story.proof.copy(boardProof = 60, lineProof = 60)))).head
    val capped = verdict(TacticLoose.withEngineCheck(leadStory, engineCheck(EngineCheckStatus.Caps, leadStory)).get)
    val refuted =
      verdict(TacticLoose.withEngineCheck(leadStory, engineCheck(EngineCheckStatus.Supports, leadStory, before = 220, after = 20)).get)

    assertEquals(lowProof.role, Role.Context)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(lowProof), None, "context")
    assertEquals(ExplanationPlan.fromSelected(capped), None, "capped")
    assertEquals(ExplanationPlan.fromSelected(refuted), None, "refuted")

  private def facts: BoardFacts =
    BoardFacts.fromFen(looseFen).fold(error => fail(s"invalid Stage-6 FEN: $looseFen -> $error"), identity)

  private def story: Story =
    TacticLoose.write(facts, Some(looseMove)).get

  private def leadStory: Story =
    story.copy(proof = orderingProof)

  private def verdict(row: Story): Verdict =
    StoryTable.choose(Vector(row)).head

  private def engineCheck(status: EngineCheckStatus, row: Story, before: Int = 20, after: Int = 20): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(looseMove))),
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
