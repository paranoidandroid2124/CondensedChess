package lila.commentary.chess

class StalemateStage6Test extends munit.FunSuite:

  private val stalemateFen = "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1"
  private val stalemateMove = Line(Square('g', 5), Square('g', 6))
  private val replyMove = Line(Square('h', 8), Square('h', 7))
  private val mateFen = "k7/2Q5/2K5/8/8/8/8/8 w - - 0 1"
  private val mateMove = Line(Square('c', 7), Square('b', 7))
  private val checkGivenFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkGivenMove = Line(Square('d', 2), Square('e', 2))
  private val checkEscapedFen = "k3r3/8/8/8/8/8/8/4K3 w - - 0 1"
  private val checkEscapedMove = Line(Square('e', 1), Square('f', 1))

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def stalemateFacts: BoardFacts =
    facts(stalemateFen)

  private def story: Story =
    SceneStalemate.write(stalemateFacts, stalemateMove).get

  private def verdict(row: Story): Verdict =
    StoryTable.choose(Vector(row)).head

  private def engineCheck(row: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = stalemateFacts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(stalemateMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  test("Stage-6 ExplanationPlan lowers only selected uncapped Lead Stalemate to stalemates"):
    val selected = verdict(story)
    val plan = ExplanationPlan.fromSelected(selected).get
    val proof = story.stalemateProof.get
    val forbiddenClaimKeys = Vector(
      "checkmates",
      "gives_check",
      "escapes_check",
      "draws_game",
      "saves_game",
      "throws_win",
      "blunder",
      "tablebase_draw",
      "engine_says_draw",
      "best_move",
      "only_move",
      "forced",
      "winning",
      "losing",
      "decisive",
      "no_counterplay"
    )

    assertEquals(selected.role, Role.Lead)
    assertEquals(selected.leadAllowed, true)
    assertEquals(selected.selected, true)
    assertEquals(selected.engineStrengthLimited, false)
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Stalemate)
    assertEquals(plan.tactic, None)
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('h', 8)))
    assertEquals(plan.anchor, Some(Square('g', 5)))
    assertEquals(plan.route, Some(stalemateMove))
    assertEquals(plan.routeSan, Some("Qg6"))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim.map(_.key), Some("stalemates"))
    assertEquals(plan.evidenceLine, Some(stalemateMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    assertEquals(DeterministicRenderer.fromPlan(plan).map(_.claimKey), Some("stalemates"))
    assertEquals(proof.afterBoardRivalSideNotInCheck, true)
    assertEquals(proof.afterBoardRivalSideHasNoLegalMoves, true)
    assertEquals(proof.stalemateMove, Some(stalemateMove))

    forbiddenClaimKeys.foreach: key =>
      assertEquals(plan.allowedClaim.exists(_.key == key), false, s"Stalemate allowed forbidden claim key $key")
      assert(ExplanationClaim.StalemateForbiddenKeys.contains(key), s"Stalemate forbidden keys must include $key")

  test("Stage-6 Stalemate plan requires clean same-board proof-backed Story identity"):
    val selected = verdict(story)
    val proof = story.stalemateProof.get

    Vector(
      "writerless" -> story.copy(writer = None),
      "missing proof" -> story.copy(stalemateProof = None),
      "contaminated tactic" -> story.copy(tactic = Some(Tactic.Fork)),
      "contaminated plan" -> story.copy(plan = Some(Plan.OpenFile)),
      "target mismatch" -> story.copy(target = Some(Square('g', 8))),
      "anchor mismatch" -> story.copy(anchor = Some(Square('g', 6))),
      "route mismatch" -> story.copy(route = Some(Line(Square('g', 5), Square('h', 5)))),
      "checkmate sidecar contamination" -> story.copy(checkmateProof = Some(CheckmateProof.fromBoardFacts(facts(mateFen), mateMove))),
      "check given sidecar contamination" -> story.copy(checkGivenProof = Some(CheckGivenProof.fromBoardFacts(facts(checkGivenFen), checkGivenMove))),
      "check escaped sidecar contamination" -> story.copy(checkEscapedProof = Some(CheckEscapedProof.fromBoardFacts(facts(checkEscapedFen), checkEscapedMove))),
      "incomplete proof" -> story.copy(
        stalemateProof = Some(
          proof.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("StalemateProof", Vector("no legal moves"))))
        )
      ),
      "after-board not-in-check missing" -> story.copy(stalemateProof = Some(proof.copy(afterBoardRivalSideNotInCheck = false))),
      "after-board no-legal-move missing" -> story.copy(stalemateProof = Some(proof.copy(afterBoardRivalSideHasNoLegalMoves = false)))
    ).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(selected.copy(story = row)), None, label)

  test("Stage-6 no-standalone boundary blocks Support Context Blocked capped and refuted Stalemate rows"):
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
    assertEquals(ExplanationPlan.fromSelected(supportsVerdict).flatMap(_.allowedClaim).map(_.key), Some("stalemates"))
    assertEquals(ExplanationPlan.fromSelected(supportsVerdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("stalemates"))

    val capped = verdict(story.copy(engineCheck = Some(engineCheck(story, EngineCheckStatus.Caps))))
    val refuted = verdict(story.copy(engineCheck = Some(engineCheck(story, EngineCheckStatus.Refutes))))

    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(capped), None, "capped")
    assertEquals(ExplanationPlan.fromSelected(refuted), None, "refuted")

  test("Stage-6 sibling terminal rows keep their own claim keys"):
    val checkmatePlan = ExplanationPlan.fromSelected(verdict(SceneCheckmate.write(facts(mateFen), mateMove).get)).get
    val checkGivenPlan = ExplanationPlan.fromSelected(verdict(SceneCheckGiven.write(facts(checkGivenFen), checkGivenMove).get)).get
    val checkEscapedPlan =
      ExplanationPlan.fromSelected(verdict(SceneCheckEscaped.write(facts(checkEscapedFen), checkEscapedMove).get)).get

    assertEquals(checkmatePlan.scene, Scene.Checkmate)
    assertEquals(checkmatePlan.allowedClaim.map(_.key), Some("checkmates"))
    assertEquals(checkmatePlan.allowedClaim.exists(_.key == "stalemates"), false)
    assertEquals(checkGivenPlan.scene, Scene.CheckGiven)
    assertEquals(checkGivenPlan.allowedClaim.map(_.key), Some("gives_check"))
    assertEquals(checkGivenPlan.allowedClaim.exists(_.key == "stalemates"), false)
    assertEquals(checkEscapedPlan.scene, Scene.CheckEscaped)
    assertEquals(checkEscapedPlan.allowedClaim.map(_.key), Some("escapes_check"))
    assertEquals(checkEscapedPlan.allowedClaim.exists(_.key == "stalemates"), false)
