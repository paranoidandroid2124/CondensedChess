package lila.commentary.chess

class ForkPawnAttackerStage6Test extends munit.FunSuite:

  private val forkFen = "7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1"
  private val forkFacts = facts(forkFen)
  private val forkRoute = Line(Square('e', 4), Square('e', 5))
  private val quietRoute = Line(Square('h', 1), Square('h', 2))
  private val replyRoute = Line(Square('h', 8), Square('h', 7))
  private val targetA = Square('d', 6)
  private val targetB = Square('f', 6)

  test("Stage-6 ExplanationPlan lowers selected uncapped pawn-attacker Fork Lead only"):
    val verdict = StoryTable.choose(Vector(forkStory)).head
    val plan = ExplanationPlan.fromSelected(verdict).get

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes), false)
    assertEquals(plan.tactic, Some(Tactic.Fork))
    assertEquals(plan.allowedClaim.map(_.key), Some("forks_two_targets"))
    assertEquals(plan.evidenceLine, Some(forkRoute))
    assertEquals(plan.route, Some(forkRoute))
    assertEquals(plan.secondaryTarget, Some(targetB))
    assertEquals(plan.supportContextLinks, Vector.empty)

  test("Stage-6 ExplanationPlan does not add PawnFork or sibling claim keys"):
    val verdict = StoryTable.choose(Vector(forkStory)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val openClaim = plan.allowedClaim.map(_.key).toVector
    val forbiddenClaims = Vector(
      "pawn_forks_two_targets",
      "pawn_fork",
      "promotes_pawn",
      "creates_promotion_threat",
      "material_balance_changes",
      "wins_material",
      "tempo"
    )

    assertEquals(openClaim, Vector("forks_two_targets"))
    forbiddenClaims.foreach: closed =>
      assertEquals(openClaim.exists(_.contains(closed)), false, closed)

  test("Stage-6 ExplanationPlan does not prove or repair missing pawn Fork proof"):
    val story = forkStory
    val proofless = story.copy(multiTargetProof = None)
    val incompleteStoryProof = story.copy(storyProof = StoryProof.empty)
    val wrongRouteProof =
      story.copy(multiTargetProof = story.multiTargetProof.map(_.copy(forkMove = Some(quietRoute))))
    val wrongAnchorProof =
      story.copy(multiTargetProof = story.multiTargetProof.map(_.copy(attackerAfterMove = Some(Piece(Side.White, Man.Pawn, Square('e', 6))))))
    val wrongWriter = story.copy(writer = None)

    Vector(proofless, incompleteStoryProof, wrongRouteProof, wrongAnchorProof, wrongWriter).foreach: broken =>
      assertEquals(ExplanationPlan.fromSelected(selectedLead(broken)), None, broken.toString)

  test("Stage-6 Support Context Blocked capped and refuted pawn-attacker Fork rows stay silent"):
    val strongFork = nonPawnFork
    val support = StoryTable
      .choose(Vector(strongFork, forkStory))
      .find(_.story.route.contains(forkRoute))
      .get
    val context = StoryTable.choose(Vector(forkStory.copy(proof = lowProof))).head
    val blocked = StoryTable.choose(Vector(forkStory.copy(writer = None))).head
    val capped =
      StoryTable.choose(Vector(TacticFork.withEngineCheck(forkStory, check(forkStory, EngineCheckStatus.Caps)).get)).head
    val refuted =
      StoryTable
        .choose(Vector(TacticFork.withEngineCheck(forkStory, check(forkStory, EngineCheckStatus.Supports, before = 220, after = 20)).get))
        .head

    assertEquals(support.role, Role.Support)
    assertEquals(context.role, Role.Context)
    assertEquals(blocked.role, Role.Blocked)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    Vector(support, context, blocked, capped, refuted).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, verdict.toString)

  private def selectedLead(story: Story): Verdict =
    Verdict(
      story = story,
      rank = 0,
      leadAllowed = true,
      strength = 1.0,
      role = Role.Lead,
      selected = true
    )

  private def check(
      story: Story,
      status: EngineCheckStatus,
      before: Int = 20,
      after: Int = 20
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = forkFacts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(forkRoute))),
      replyLine = Some(EngineLine(Vector(replyRoute))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def forkStory: Story =
    TacticFork.write(forkFacts, Some(forkRoute), Some(targetA), Some(targetB)).get

  private def nonPawnFork: Story =
    val forkFacts = facts("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1")
    val forkMove = Line(Square('f', 3), Square('d', 4))
    TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-6 FEN: $fen -> $error"), identity)

  private val lowProof: Proof =
    forkStory.proof.copy(boardProof = 60, lineProof = 60, ownerProof = 60, anchorProof = 60, routeProof = 60)
