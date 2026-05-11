package lila.commentary.chess

class CheckGivenStage6Test extends munit.FunSuite:

  private val checkingFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkingMove = Line(Square('d', 2), Square('e', 2))
  private val replyMove = Line(Square('e', 8), Square('f', 8))

  private def facts: BoardFacts =
    BoardFacts.fromFen(checkingFen).toOption.get

  private def story: Story =
    SceneCheckGiven.write(facts, checkingMove).get

  private def verdict(row: Story): Verdict =
    StoryTable.choose(Vector(row)).head

  private def engineCheck(row: Story, status: EngineCheckStatus, before: Int = 100, after: Int = 100): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(checkingMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  test("Stage-6 ExplanationPlan lowers only selected uncapped Lead CheckGiven to gives_check"):
    val selected = verdict(story)
    val plan = ExplanationPlan.fromSelected(selected).get
    val forbiddenClaimKeys = Vector(
      "mate_threat",
      "checkmate",
      "king_unsafe",
      "attacks_king",
      "creates_attack",
      "creates_pressure",
      "takes_initiative",
      "forces_reply",
      "best_move",
      "only_move",
      "winning",
      "decisive",
      "no_counterplay"
    )

    assertEquals(selected.role, Role.Lead)
    assertEquals(selected.leadAllowed, true)
    assertEquals(selected.selected, true)
    assertEquals(selected.engineStrengthLimited, false)
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.CheckGiven)
    assertEquals(plan.tactic, None)
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('e', 8)))
    assertEquals(plan.anchor, Some(Square('d', 2)))
    assertEquals(plan.route, Some(checkingMove))
    assertEquals(plan.routeSan, Some("Re2+"))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim.map(_.key), Some("gives_check"))
    assertEquals(plan.evidenceLine, Some(checkingMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    assertEquals(DeterministicRenderer.fromPlan(plan).map(_.claimKey), Some("gives_check"))
    forbiddenClaimKeys.foreach: key =>
      assertEquals(plan.allowedClaim.exists(_.key == key), false, s"CheckGiven allowed forbidden claim key $key")
      assert(plan.forbiddenWording.map(_.key).contains(key), s"CheckGiven plan must forbid $key")

  test("Stage-6 CheckGiven plan requires clean same-board proof-backed Story identity"):
    val selected = verdict(story)
    val proof = story.checkGivenProof.get

    Vector(
      "writerless" -> story.copy(writer = None),
      "missing proof" -> story.copy(checkGivenProof = None),
      "contaminated tactic" -> story.copy(tactic = Some(Tactic.Fork)),
      "contaminated plan" -> story.copy(plan = Some(Plan.OpenFile)),
      "target mismatch" -> story.copy(target = Some(Square('f', 8))),
      "anchor mismatch" -> story.copy(anchor = Some(Square('e', 2))),
      "route mismatch" -> story.copy(route = Some(Line(Square('d', 2), Square('d', 8)))),
      "incomplete proof" -> story.copy(
        checkGivenProof = Some(
          proof.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("CheckGivenProof", Vector("exact after-board replay"))))
        )
      )
    ).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(selected.copy(story = row)), None, label)

  test("Stage-6 no-standalone boundary blocks Support Context Blocked capped and refuted CheckGiven rows"):
    val selected = verdict(story)
    Vector(
      "unselected" -> selected.copy(selected = false),
      "support" -> selected.copy(role = Role.Support, leadAllowed = false),
      "context" -> selected.copy(role = Role.Context, leadAllowed = false),
      "blocked" -> selected.copy(role = Role.Blocked, leadAllowed = false)
    ).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(row), None, label)

    val supports = SceneCheckGiven.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Supports)).get
    val supportsVerdict = verdict(supports)
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(supportsVerdict).flatMap(_.allowedClaim).map(_.key), Some("gives_check"))

    val capped = verdict(SceneCheckGiven.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Caps)).get)
    val refuted =
      verdict(SceneCheckGiven.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Supports, before = 250, after = 25)).get)

    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(capped), None, "capped")
    assertEquals(ExplanationPlan.fromSelected(refuted), None, "refuted")
