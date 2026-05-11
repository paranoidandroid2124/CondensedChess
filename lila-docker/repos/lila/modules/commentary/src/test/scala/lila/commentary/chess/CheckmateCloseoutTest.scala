package lila.commentary.chess

class CheckmateCloseoutTest extends munit.FunSuite:

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

  private def checkmateStory: Story =
    SceneCheckmate.write(mateFacts, mateMove).get

  private def selected(row: Story): Verdict =
    StoryTable.choose(Vector(row)).head

  private def checkmatePlan(row: Story = checkmateStory): ExplanationPlan =
    ExplanationPlan.fromSelected(selected(row)).get

  private def checkmateRendered(row: Story): RenderedLine =
    DeterministicRenderer.fromPlan(checkmatePlan(row)).get

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

  private def assertNoStandaloneText(label: String, row: Story): Unit =
    assertEquals(ExplanationPlan.fromSelected(selected(row)).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("Checkmate closeout keeps proof Story writer and speech key authority separated"):
    val story = checkmateStory
    val proof = story.checkmateProof.get
    val plan = checkmatePlan(story)
    val rendered = checkmateRendered(story)

    assertEquals(story.scene, Scene.Checkmate)
    assertEquals(story.writer, Some(StoryWriter.SceneCheckmate))
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('a', 8)))
    assertEquals(story.anchor, Some(Square('c', 7)))
    assertEquals(story.route, Some(mateMove))
    assertEquals(story.routeSan, Some("Qb7#"))
    assertEquals(story.checkGivenProof, None)
    assertEquals(story.checkEscapedProof, None)

    assertEquals(proof.complete, true)
    assertEquals(proof.matingSide, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.mateMove, Some(mateMove))
    assertEquals(proof.rivalKingSquareAfter, Some(Square('a', 8)))
    assertEquals(proof.afterBoardRivalKingInCheck, true)
    assertEquals(proof.afterBoardRivalSideHasNoLegalEscape, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.sameBoardProof, true)

    assertEquals(plan.scene, Scene.Checkmate)
    assertEquals(plan.allowedClaim.map(_.key), Some("checkmates"))
    assertEquals(plan.route, Some(mateMove))
    assertEquals(plan.evidenceLine, Some(mateMove))
    assertEquals(rendered.claimKey, "checkmates")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.text, "Qb7# is checkmate.")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some("Qb7# is checkmate."))

  test("Checkmate closeout keeps check given check escaped and checkmate as one chain each"):
    val checkGivenStory = SceneCheckGiven.write(facts(checkGivenFen), checkGivenMove).get
    val checkEscapedStory = SceneCheckEscaped.write(facts(checkEscapedFen), checkEscapedMove).get
    val checkGivenPlan = ExplanationPlan.fromSelected(selected(checkGivenStory)).get
    val checkEscapedPlan = ExplanationPlan.fromSelected(selected(checkEscapedStory)).get
    val matePlan = checkmatePlan()

    assertEquals(checkGivenStory.scene, Scene.CheckGiven)
    assertEquals(checkGivenStory.writer, Some(StoryWriter.SceneCheckGiven))
    assertEquals(checkGivenStory.checkGivenProof.exists(_.complete), true)
    assertEquals(checkGivenStory.checkmateProof, None)
    assertEquals(checkGivenStory.checkEscapedProof, None)
    assertEquals(checkGivenPlan.allowedClaim.map(_.key), Some("gives_check"))

    assertEquals(checkEscapedStory.scene, Scene.CheckEscaped)
    assertEquals(checkEscapedStory.writer, Some(StoryWriter.SceneCheckEscaped))
    assertEquals(checkEscapedStory.checkEscapedProof.exists(_.complete), true)
    assertEquals(checkEscapedStory.checkmateProof, None)
    assertEquals(checkEscapedStory.checkGivenProof, None)
    assertEquals(checkEscapedPlan.allowedClaim.map(_.key), Some("escapes_check"))

    assertEquals(checkmateStory.checkmateProof.exists(_.complete), true)
    assertEquals(matePlan.allowedClaim.map(_.key), Some("checkmates"))
    assertEquals(ExplanationClaim.CheckmateAllowed.map(_.key), Vector("checkmates"))
    assertEquals(
      Vector(
        checkGivenPlan.allowedClaim.map(_.key),
        checkEscapedPlan.allowedClaim.map(_.key),
        matePlan.allowedClaim.map(_.key)
      ).flatten.distinct.sorted,
      Vector("checkmates", "escapes_check", "gives_check").sorted
    )

  test("Checkmate closeout keeps BoardFacts SAN and engine evidence below Story creation"):
    val story = checkmateStory
    val checkProof = CheckmateProof.fromBoardFacts(mateFacts, mateMove)
    val givesCheckProof = CheckGivenProof.fromBoardFacts(mateFacts, mateMove)
    val engineOnly =
      EngineCheck.fromEvidence(
        sameBoardProof = true,
        checkedMove = Some(mateMove),
        engineLine = Some(EngineLine(Vector(mateMove))),
        replyLine = Some(EngineLine(Vector(replyMove))),
        evalBefore = Some(EngineEval(0)),
        evalAfter = Some(EngineEval(10000)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    assertEquals(BoardFacts.sideInCheck(mateFacts, Side.White), false)
    assertEquals(BoardFacts.sideInCheck(mateFacts, Side.Black), false)
    assert(mateFacts.sideLegal.sanFor(mateMove).exists(_.contains("#")))
    assertEquals(checkProof.complete, true)
    assertEquals(givesCheckProof.complete, true)
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty)
    assertEquals(engineOnly.storyBound, false)
    assertEquals(engineOnly.publicClaimAllowed, false)

    val supports = story.copy(engineCheck = Some(engineCheck(story, EngineCheckStatus.Supports)))
    val capped = story.copy(engineCheck = Some(engineCheck(story, EngineCheckStatus.Caps)))
    val refuted = story.copy(engineCheck = Some(engineCheck(story, EngineCheckStatus.Supports, before = 250, after = 0)))
    val unknown = story.copy(engineCheck = Some(engineCheck(story, EngineCheckStatus.Unknown)))

    assertEquals(selected(supports).engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(checkmateRendered(supports).claimKey, "checkmates")
    assertEquals(selected(capped).engineStrengthLimited, true)
    assertNoStandaloneText("capped Checkmate", capped)
    assertEquals(selected(refuted).role, Role.Blocked)
    assertNoStandaloneText("refuted Checkmate", refuted)
    assertEquals(selected(unknown).engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(selected(unknown).engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(selected(unknown)).flatMap(_.allowedClaim).map(_.key), None)

  test("Checkmate closeout keeps sibling and forbidden meanings silent"):
    val story = checkmateStory
    val rendered = checkmateRendered(story)
    val plan = checkmatePlan(story)
    val checkGivenProof = CheckGivenProof.fromBoardFacts(mateFacts, mateMove)

    Vector(
      "support" -> selected(story).copy(role = Role.Support, leadAllowed = false),
      "context" -> selected(story).copy(role = Role.Context, leadAllowed = false),
      "blocked" -> selected(story).copy(role = Role.Blocked, leadAllowed = false)
    ).foreach: (label, verdict) =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

    Vector(
      "writerless" -> story.copy(writer = None),
      "check-given contamination" -> story.copy(checkGivenProof = Some(checkGivenProof)),
      "tactic contamination" -> story.copy(tactic = Some(Tactic.Fork)),
      "material contamination" -> story.copy(scene = Scene.Material),
      "defense contamination" -> story.copy(scene = Scene.Defense),
      "pawn contamination" -> story.copy(scene = Scene.PawnAdvance)
    ).foreach: (label, forged) =>
      assertNoStandaloneText(label, forged)

    Vector(
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
    ).foreach: key =>
      assert(ExplanationClaim.CheckmateForbiddenKeys.contains(key), key)
      assert(plan.forbiddenWording.map(_.key).contains(key), key)

    Vector(
      "Qb7# threatens mate.",
      "Qb7# starts mate in two.",
      "Qb7# is forced mate.",
      "Qb7# is the best move.",
      "Qb7# is the only move.",
      "Qb7# is winning.",
      "Qb7# is decisive.",
      "Qb7# leaves no counterplay.",
      "Qb7# shows the king is unsafe.",
      "Qb7# starts an attack.",
      "Qb7# creates pressure.",
      "Qb7# takes the initiative.",
      "The engine says Qb7# is mate.",
      "The raw PV is Qb7#.",
      "CheckmateProof proves Qb7#.",
      "The SAN # diagnostics say this is mate."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("Checkmate closeout exposes no raw downstream or production API surface"):
    val rendererMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    val rendererParameterShapes =
      rendererMethods.map(method => method.getParameterTypes.toVector.map(_.getSimpleName).toVector)
    val llmMethodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val llmParameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    assertEquals(rendererParameterShapes, Vector(Vector("ExplanationPlan")))
    Vector(
      "fromStory",
      "fromCheckmateProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine",
      "callApi",
      "productionApi"
    ).foreach: method =>
      assert(!llmMethodNames.contains(method), method)
    Vector("Story", "CheckmateProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach: parameter =>
      assert(!llmParameterNames.contains(parameter), parameter)
