package lila.commentary.chess

class CheckmateStage6Test extends munit.FunSuite:

  private val mateFen = "k7/2Q5/2K5/8/8/8/8/8 w - - 0 1"
  private val mateMove = Line(Square('c', 7), Square('b', 7))
  private val replyMove = Line(Square('a', 8), Square('a', 7))
  private val checkGivenFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkGivenMove = Line(Square('d', 2), Square('e', 2))
  private val checkEscapedFen = "k3r3/8/8/8/8/8/8/4K3 w - - 0 1"
  private val checkEscapedMove = Line(Square('e', 1), Square('f', 1))

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def mateFacts: BoardFacts =
    facts(mateFen)

  private def story: Story =
    SceneCheckmate.write(mateFacts, mateMove).get

  private def verdict(row: Story): Verdict =
    StoryTable.choose(Vector(row)).head

  private def engineCheck(row: Story, status: EngineCheckStatus, before: Int = 0, after: Int = 0): EngineCheck =
    EngineCheck.fromStory(
      facts = mateFacts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(mateMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  test("Stage-6 ExplanationPlan lowers only selected uncapped Lead Checkmate to checkmates"):
    val selected = verdict(story)
    val plan = ExplanationPlan.fromSelected(selected).get
    val proof = story.checkmateProof.get
    val forbiddenClaimKeys = Vector(
      "gives_check",
      "escapes_check",
      "mate_threat",
      "mate_in_one",
      "mate_in_n",
      "forced_mate",
      "best_move",
      "only_move",
      "winning",
      "decisive",
      "no_counterplay",
      "king_unsafe",
      "attacks_king",
      "creates_attack",
      "creates_pressure",
      "takes_initiative",
      "engine_says_mate"
    )

    assertEquals(selected.role, Role.Lead)
    assertEquals(selected.leadAllowed, true)
    assertEquals(selected.selected, true)
    assertEquals(selected.engineStrengthLimited, false)
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Checkmate)
    assertEquals(plan.tactic, None)
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('a', 8)))
    assertEquals(plan.anchor, Some(Square('c', 7)))
    assertEquals(plan.route, Some(mateMove))
    assertEquals(plan.routeSan, Some("Qb7#"))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim.map(_.key), Some("checkmates"))
    assertEquals(plan.evidenceLine, Some(mateMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    assertEquals(DeterministicRenderer.fromPlan(plan).map(_.claimKey), Some("checkmates"))
    assertEquals(proof.afterBoardRivalKingInCheck, true)
    assertEquals(proof.afterBoardRivalSideHasNoLegalEscape, true)
    assertEquals(proof.mateMove, Some(mateMove))

    forbiddenClaimKeys.foreach: key =>
      assertEquals(plan.allowedClaim.exists(_.key == key), false, s"Checkmate allowed forbidden claim key $key")
      assert(ExplanationClaim.CheckmateForbiddenKeys.contains(key), s"Checkmate forbidden keys must include $key")
      assert(plan.forbiddenWording.map(_.key).contains(key), s"Checkmate plan must forbid $key")

  test("Stage-6 Checkmate plan requires clean same-board proof-backed Story identity"):
    val selected = verdict(story)
    val proof = story.checkmateProof.get

    Vector(
      "writerless" -> story.copy(writer = None),
      "missing proof" -> story.copy(checkmateProof = None),
      "contaminated tactic" -> story.copy(tactic = Some(Tactic.Fork)),
      "contaminated plan" -> story.copy(plan = Some(Plan.OpenFile)),
      "target mismatch" -> story.copy(target = Some(Square('b', 8))),
      "anchor mismatch" -> story.copy(anchor = Some(Square('b', 7))),
      "route mismatch" -> story.copy(route = Some(Line(Square('c', 7), Square('c', 8)))),
      "check sidecar contamination" -> story.copy(checkGivenProof = Some(CheckGivenProof.fromBoardFacts(mateFacts, mateMove))),
      "incomplete proof" -> story.copy(
        checkmateProof = Some(
          proof.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("CheckmateProof", Vector("no legal escape"))))
        )
      ),
      "after-board check missing" -> story.copy(checkmateProof = Some(proof.copy(afterBoardRivalKingInCheck = false))),
      "no-legal-escape missing" -> story.copy(checkmateProof = Some(proof.copy(afterBoardRivalSideHasNoLegalEscape = false)))
    ).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(selected.copy(story = row)), None, label)

  test("Stage-6 no-standalone boundary blocks Support Context Blocked capped and refuted Checkmate rows"):
    val selected = verdict(story)
    Vector(
      "unselected" -> selected.copy(selected = false),
      "support" -> selected.copy(role = Role.Support, leadAllowed = false),
      "context" -> selected.copy(role = Role.Context, leadAllowed = false),
      "blocked" -> selected.copy(role = Role.Blocked, leadAllowed = false)
    ).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(row), None, label)

    val supports = story.copy(engineCheck = Some(engineCheck(story, EngineCheckStatus.Supports)))
    val supportsVerdict = verdict(supports)
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(supportsVerdict).flatMap(_.allowedClaim).map(_.key), Some("checkmates"))
    assertEquals(ExplanationPlan.fromSelected(supportsVerdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("checkmates"))

    val capped = verdict(story.copy(engineCheck = Some(engineCheck(story, EngineCheckStatus.Caps))))
    val refuted =
      verdict(story.copy(engineCheck = Some(engineCheck(story, EngineCheckStatus.Supports, before = 250, after = 0))))

    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(capped), None, "capped")
    assertEquals(ExplanationPlan.fromSelected(refuted), None, "refuted")

  test("Stage-6 CheckGiven and CheckEscaped rows keep their own claim keys"):
    val checkGivenStory = SceneCheckGiven.write(facts(checkGivenFen), checkGivenMove).get
    val checkEscapedStory = SceneCheckEscaped.write(facts(checkEscapedFen), checkEscapedMove).get
    val checkGivenPlan = ExplanationPlan.fromSelected(verdict(checkGivenStory)).get
    val checkEscapedPlan = ExplanationPlan.fromSelected(verdict(checkEscapedStory)).get

    assertEquals(checkGivenPlan.scene, Scene.CheckGiven)
    assertEquals(checkGivenPlan.allowedClaim.map(_.key), Some("gives_check"))
    assertEquals(checkGivenPlan.allowedClaim.exists(_.key == "checkmates"), false)
    assertEquals(checkEscapedPlan.scene, Scene.CheckEscaped)
    assertEquals(checkEscapedPlan.allowedClaim.map(_.key), Some("escapes_check"))
    assertEquals(checkEscapedPlan.allowedClaim.exists(_.key == "checkmates"), false)
