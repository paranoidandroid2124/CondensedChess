package lila.commentary.chess

class StalemateCloseoutTest extends munit.FunSuite:

  private val stalemateFen = "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1"
  private val stalemateMove = Line(Square('g', 5), Square('g', 6))
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

  private def stalemateStory: Story =
    SceneStalemate.write(stalemateFacts, stalemateMove).get

  private def selected(row: Story): Verdict =
    StoryTable.choose(Vector(row)).head

  private def stalematePlan(row: Story = stalemateStory): ExplanationPlan =
    ExplanationPlan.fromSelected(selected(row)).get

  private def stalemateRendered(row: Story): RenderedLine =
    DeterministicRenderer.fromPlan(stalematePlan(row)).get

  private def engineCheck(row: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = stalemateFacts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(stalemateMove))),
      replyLine = Some(EngineLine(Vector(stalemateMove))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def assertNoStandaloneText(label: String, row: Story): Unit =
    assertEquals(ExplanationPlan.fromSelected(selected(row)).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("Stalemate closeout keeps proof Story writer and speech key authority separated"):
    val row = stalemateStory
    val proof = row.stalemateProof.get
    val plan = stalematePlan(row)
    val rendered = stalemateRendered(row)

    assertEquals(row.scene, Scene.Stalemate)
    assertEquals(row.writer, Some(StoryWriter.SceneStalemate))
    assertEquals(row.tactic, None)
    assertEquals(row.plan, None)
    assertEquals(row.side, Side.White)
    assertEquals(row.rival, Side.Black)
    assertEquals(row.target, Some(Square('h', 8)))
    assertEquals(row.anchor, Some(Square('g', 5)))
    assertEquals(row.route, Some(stalemateMove))
    assertEquals(row.routeSan, Some("Qg6"))

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.stalematingSide, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.stalemateMove, Some(stalemateMove))
    assertEquals(proof.originSquare, Some(Square('g', 5)))
    assertEquals(proof.destinationSquare, Some(Square('g', 6)))
    assertEquals(proof.rivalKingSquareAfter, Some(Square('h', 8)))
    assertEquals(proof.afterBoardRivalSideNotInCheck, true)
    assertEquals(proof.afterBoardRivalSideHasNoLegalMoves, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.stalemateProducedByLegalMove, true)

    assertEquals(plan.scene, Scene.Stalemate)
    assertEquals(plan.tactic, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.Stalemates))
    assertEquals(plan.allowedClaim.map(_.key), Some("stalemates"))
    assertEquals(plan.route, Some(stalemateMove))
    assertEquals(plan.evidenceLine, Some(stalemateMove))
    assertEquals(rendered.claimKey, "stalemates")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.text, "Qg6 is stalemate.")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some("Qg6 is stalemate."))

  test("Stalemate closeout keeps terminal and King Check chains from owning each other"):
    val stale = stalemateStory
    val mate = SceneCheckmate.write(facts(mateFen), mateMove).get
    val checkGiven = SceneCheckGiven.write(facts(checkGivenFen), checkGivenMove).get
    val checkEscaped = SceneCheckEscaped.write(facts(checkEscapedFen), checkEscapedMove).get
    val stalePlan = stalematePlan(stale)
    val matePlan = ExplanationPlan.fromSelected(selected(mate)).get
    val checkGivenPlan = ExplanationPlan.fromSelected(selected(checkGiven)).get
    val checkEscapedPlan = ExplanationPlan.fromSelected(selected(checkEscaped)).get

    assertEquals(stale.stalemateProof.exists(_.complete), true)
    assertEquals(stale.stalemateProof.exists(_.afterBoardRivalSideNotInCheck), true)
    assertEquals(stale.stalemateProof.exists(_.afterBoardRivalSideHasNoLegalMoves), true)
    assertEquals(stale.checkmateProof, None)
    assertEquals(stale.checkGivenProof, None)
    assertEquals(stale.checkEscapedProof, None)

    assertEquals(mate.checkmateProof.exists(_.complete), true)
    assertEquals(mate.checkmateProof.exists(_.afterBoardRivalKingInCheck), true)
    assertEquals(mate.checkmateProof.exists(_.afterBoardRivalSideHasNoLegalEscape), true)
    assertEquals(mate.stalemateProof, None)

    assertEquals(checkGiven.checkGivenProof.exists(_.complete), true)
    assertEquals(checkGiven.stalemateProof, None)
    assertEquals(checkGiven.checkmateProof, None)
    assertEquals(checkEscaped.checkEscapedProof.exists(_.complete), true)
    assertEquals(checkEscaped.stalemateProof, None)
    assertEquals(checkEscaped.checkmateProof, None)

    assertEquals(stalePlan.allowedClaim.map(_.key), Some("stalemates"))
    assertEquals(matePlan.allowedClaim.map(_.key), Some("checkmates"))
    assertEquals(checkGivenPlan.allowedClaim.map(_.key), Some("gives_check"))
    assertEquals(checkEscapedPlan.allowedClaim.map(_.key), Some("escapes_check"))
    assertEquals(ExplanationClaim.StalemateAllowed.map(_.key), Vector("stalemates"))
    assertEquals(ExplanationClaim.CheckmateAllowed.map(_.key), Vector("checkmates"))
    assert(!ExplanationClaim.CheckmateAllowed.map(_.key).contains("stalemates"))
    assert(!ExplanationClaim.StalemateAllowed.map(_.key).contains("checkmates"))

  test("Stalemate closeout keeps sibling sidecars engine rows and non Lead rows silent"):
    val row = stalemateStory
    val checkmateProof = CheckmateProof.fromBoardFacts(facts(mateFen), mateMove)
    val checkGivenProof = CheckGivenProof.fromBoardFacts(facts(checkGivenFen), checkGivenMove)
    val checkEscapedProof = CheckEscapedProof.fromBoardFacts(facts(checkEscapedFen), checkEscapedMove)

    Vector(
      "support" -> selected(row).copy(role = Role.Support, leadAllowed = false),
      "context" -> selected(row).copy(role = Role.Context, leadAllowed = false),
      "blocked" -> selected(row).copy(role = Role.Blocked, leadAllowed = false)
    ).foreach: (label, verdict) =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

    Vector(
      "writerless" -> row.copy(writer = None),
      "checkmate contamination" -> row.copy(checkmateProof = Some(checkmateProof)),
      "check-given contamination" -> row.copy(checkGivenProof = Some(checkGivenProof)),
      "check-escaped contamination" -> row.copy(checkEscapedProof = Some(checkEscapedProof)),
      "tactic contamination" -> row.copy(tactic = Some(Tactic.Fork)),
      "plan contamination" -> row.copy(plan = Some(Plan.Race)),
      "material contamination" -> row.copy(scene = Scene.Material)
    ).foreach: (label, forged) =>
      assertNoStandaloneText(label, forged)

    val supports = row.copy(engineCheck = Some(engineCheck(row, EngineCheckStatus.Supports)))
    val capped = row.copy(engineCheck = Some(engineCheck(row, EngineCheckStatus.Caps)))
    val refuted = row.copy(engineCheck = Some(engineCheck(row, EngineCheckStatus.Refutes)))
    val unknown = row.copy(engineCheck = Some(engineCheck(row, EngineCheckStatus.Unknown)))

    assertEquals(ExplanationPlan.fromSelected(selected(supports)).flatMap(_.allowedClaim).map(_.key), Some("stalemates"))
    assertEquals(ExplanationPlan.fromSelected(selected(supports)).flatMap(DeterministicRenderer.fromPlan).map(_.text), Some("Qg6 is stalemate."))
    assertEquals(selected(capped).engineStrengthLimited, true)
    assertNoStandaloneText("capped Stalemate", capped)
    assertEquals(selected(refuted).role, Role.Blocked)
    assertNoStandaloneText("refuted Stalemate", refuted)
    assertEquals(selected(unknown).engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(ExplanationPlan.fromSelected(selected(unknown)), None)

  test("Stalemate closeout keeps draw evaluation and engine tablebase wording closed"):
    val plan = stalematePlan()
    val rendered = stalemateRendered(stalemateStory)

    Vector(
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
      "no_counterplay",
      "checkmates",
      "gives_check",
      "escapes_check"
    ).foreach: key =>
      assert(ExplanationClaim.StalemateForbiddenKeys.contains(key), key)

    Vector(
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
      "no_counterplay",
      "checkmate",
      "gives_check",
      "escapes_check"
    ).foreach: key =>
      assert(plan.forbiddenWording.map(_.key).contains(key), key)

    Vector(
      "Qg6 draws the game.",
      "Qg6 saves the game.",
      "Qg6 throws away the win.",
      "Qg6 is a blunder.",
      "The tablebase says Qg6 draws.",
      "The engine says Qg6 is a draw.",
      "Qg6 is the best move.",
      "Qg6 is the only move.",
      "Qg6 is forced.",
      "Qg6 was winning.",
      "Qg6 is losing.",
      "Qg6 is decisive.",
      "Qg6 leaves no counterplay.",
      "Qg6 is checkmate.",
      "Qg6 gives check.",
      "Qg6 escapes check.",
      "StalemateProof proves Qg6.",
      "The result notation is 1/2-1/2 after Qg6."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("Stalemate closeout exposes no raw downstream LLM surface"):
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
      "fromStalemateProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine",
      "fromResultNotation",
      "fromTablebase",
      "callApi",
      "productionApi"
    ).foreach: method =>
      assert(!llmMethodNames.contains(method), s"Stalemate closeout LLM smoke must not expose $method")
    Vector("Story", "StalemateProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach: parameter =>
      assert(!llmParameterNames.contains(parameter), s"Stalemate closeout LLM smoke must not accept $parameter")
