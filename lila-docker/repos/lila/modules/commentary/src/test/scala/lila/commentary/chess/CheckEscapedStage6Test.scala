package lila.commentary.chess

class CheckEscapedStage6Test extends munit.FunSuite:

  private val escapeFen = "k3r3/8/8/8/8/8/8/4K3 w - - 0 1"
  private val escapeMove = Line(Square('e', 1), Square('f', 1))
  private val checkingFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkingMove = Line(Square('d', 2), Square('e', 2))
  private val replyMove = Line(Square('a', 8), Square('b', 8))

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def escapeFacts: BoardFacts =
    facts(escapeFen)

  private def story: Story =
    SceneCheckEscaped.write(escapeFacts, escapeMove).get

  private def verdict(row: Story): Verdict =
    StoryTable.choose(Vector(row)).head

  private def engineCheck(row: Story, status: EngineCheckStatus, before: Int = 100, after: Int = 100): EngineCheck =
    EngineCheck.fromStory(
      facts = escapeFacts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(escapeMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  test("Stage-6 ExplanationPlan lowers only selected uncapped Lead CheckEscaped to escapes_check"):
    val selected = verdict(story)
    val plan = ExplanationPlan.fromSelected(selected).get
    val forbiddenClaimKeys = Vector(
      "king_escapes_check",
      "blocks_check",
      "captures_checker",
      "gives_check",
      "avoids_mate",
      "mate_threat",
      "checkmate",
      "king_safe",
      "king_unsafe",
      "refutes_attack",
      "defends_position",
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
    val forbiddenWordingKeys = Vector(
      "blocks_check",
      "captures_checker",
      "avoids_mate",
      "mate_threat",
      "checkmate",
      "king_safe",
      "king_unsafe",
      "refutes_attack",
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
    assertEquals(plan.scene, Scene.CheckEscaped)
    assertEquals(plan.tactic, None)
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('f', 1)))
    assertEquals(plan.anchor, Some(Square('e', 1)))
    assertEquals(plan.route, Some(escapeMove))
    assertEquals(plan.routeSan, Some("Kf1"))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim.map(_.key), Some("escapes_check"))
    assertEquals(plan.evidenceLine, Some(escapeMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)

    forbiddenClaimKeys.foreach: key =>
      assertEquals(plan.allowedClaim.exists(_.key == key), false, s"CheckEscaped allowed forbidden claim key $key")
      assert(ExplanationClaim.CheckEscapedForbiddenKeys.contains(key), s"CheckEscaped forbidden keys must include $key")
    forbiddenWordingKeys.foreach: key =>
      assert(plan.forbiddenWording.map(_.key).contains(key), s"CheckEscaped plan must forbid $key")

  test("Stage-6 CheckEscaped plan requires clean same-board proof-backed Story identity"):
    val selected = verdict(story)
    val proof = story.checkEscapedProof.get

    Vector(
      "writerless" -> story.copy(writer = None),
      "missing proof" -> story.copy(checkEscapedProof = None),
      "contaminated tactic" -> story.copy(tactic = Some(Tactic.Fork)),
      "contaminated plan" -> story.copy(plan = Some(Plan.OpenFile)),
      "target mismatch" -> story.copy(target = Some(Square('e', 1))),
      "anchor mismatch" -> story.copy(anchor = Some(Square('f', 1))),
      "route mismatch" -> story.copy(route = Some(Line(Square('e', 1), Square('e', 2)))),
      "incomplete proof" -> story.copy(
        checkEscapedProof = Some(
          proof.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("CheckEscapedProof", Vector("after-board side king not in check"))))
        )
      )
    ).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(selected.copy(story = row)), None, label)

  test("Stage-6 escape method details do not lower to standalone claim keys"):
    val methodRows = Vector(
      "king move" -> story,
      "interposition" -> SceneCheckEscaped.write(
        facts("k3r3/8/8/8/8/8/3B4/4K3 w - - 0 1"),
        Line(Square('d', 2), Square('e', 3))
      ).get,
      "checking piece capture" -> SceneCheckEscaped.write(
        facts("k7/8/8/8/8/8/4r3/4K3 w - - 0 1"),
        Line(Square('e', 1), Square('e', 2))
      ).get
    )

    methodRows.foreach: (label, row) =>
      val plan = ExplanationPlan.fromSelected(verdict(row)).get
      assertEquals(plan.allowedClaim.map(_.key), Some("escapes_check"), label)
      assertEquals(plan.allowedClaim.exists(claim => Set("king_escapes_check", "blocks_check", "captures_checker").contains(claim.key)), false, label)

  test("Stage-6 no-standalone boundary blocks Support Context Blocked capped and refuted CheckEscaped rows"):
    val selected = verdict(story)
    Vector(
      "unselected" -> selected.copy(selected = false),
      "support" -> selected.copy(role = Role.Support, leadAllowed = false),
      "context" -> selected.copy(role = Role.Context, leadAllowed = false),
      "blocked" -> selected.copy(role = Role.Blocked, leadAllowed = false)
    ).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(row), None, label)

    val supports = SceneCheckEscaped.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Supports)).get
    val supportsVerdict = verdict(supports)
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(supportsVerdict).flatMap(_.allowedClaim).map(_.key), Some("escapes_check"))

    val capped = verdict(SceneCheckEscaped.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Caps)).get)
    val refuted =
      verdict(SceneCheckEscaped.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Supports, before = 250, after = 25)).get)

    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(capped), None, "capped")
    assertEquals(ExplanationPlan.fromSelected(refuted), None, "refuted")

  test("Stage-6 CheckGiven rows retain gives_check and never lower to escapes_check"):
    val checkGivenFacts = facts(checkingFen)
    val checkGivenStory = SceneCheckGiven.write(checkGivenFacts, checkingMove).get
    val checkGivenPlan = ExplanationPlan.fromSelected(verdict(checkGivenStory)).get

    assertEquals(checkGivenPlan.scene, Scene.CheckGiven)
    assertEquals(checkGivenPlan.allowedClaim.map(_.key), Some("gives_check"))
    assertEquals(checkGivenPlan.allowedClaim.exists(_.key == "escapes_check"), false)
