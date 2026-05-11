package lila.commentary.chess

class CheckmateStage2Test extends munit.FunSuite:

  private val mateFen = "k7/2Q5/2K5/8/8/8/8/8 w - - 0 1"
  private val mateMove = Line(Square('c', 7), Square('b', 7))
  private val checkButEscapeFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkButEscapeMove = Line(Square('d', 2), Square('e', 2))

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def eventProof(value: Int): Proof =
    Proof(
      boardProof = value,
      lineProof = value,
      ownerProof = value,
      anchorProof = value,
      routeProof = value,
      persistence = 0,
      immediacy = value,
      forcing = value,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = value,
      pieceSupport = 0,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = value
    )

  private def refutingEngine(move: Line): EngineCheck =
    EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(move),
      engineLine = Some(EngineLine(Vector(move))),
      replyLine = Some(EngineLine(Vector(move))),
      evalBefore = Some(EngineEval(250)),
      evalAfter = Some(EngineEval(25)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Refutes
    )

  test("Stage-2 SceneCheckmate writes only exact complete CheckmateProof rows"):
    val mateFacts = facts(mateFen)
    val story = SceneCheckmate.write(mateFacts, mateMove).get

    assertEquals(story.scene, Scene.Checkmate)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('a', 8)))
    assertEquals(story.anchor, Some(Square('c', 7)))
    assertEquals(story.route, Some(mateMove))
    assertEquals(story.routeSan, Some("Qb7#"))
    assertEquals(story.writer, Some(StoryWriter.SceneCheckmate))
    assertEquals(story.checkmateProof.exists(_.complete), true)
    assertEquals(story.checkGivenProof, None)
    assertEquals(story.checkEscapedProof, None)
    assertEquals(story.proofFailures, Vector.empty)

    val verdict = StoryTable.choose(Vector(story)).head
    assertEquals(verdict.story, story)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.selected, true)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("checkmates"))

  test("Stage-2 SceneCheckmate stays silent for non-mate writerless contaminated and refuted rows"):
    val mateFacts = facts(mateFen)
    val story = SceneCheckmate.write(mateFacts, mateMove).get
    val checkButEscapeFacts = facts(checkButEscapeFen)

    assertEquals(SceneCheckmate.write(checkButEscapeFacts, checkButEscapeMove), None)
    assertEquals(SceneCheckmate.write(mateFacts, Line(Square('c', 7), Square('c', 8))), None)

    val writerless = story.copy(writer = None)
    val contaminatedTactic = story.copy(tactic = Some(Tactic.SafeCheck))
    val contaminatedCheckGiven = story.copy(checkGivenProof = Some(CheckGivenProof.fromBoardFacts(mateFacts, mateMove)))
    val incompleteProof = story.copy(
      checkmateProof = story.checkmateProof.map(
        _.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("CheckmateProof", Vector("rival-side no legal escape"))))
      )
    )
    val refuted = story.copy(engineCheck = Some(refutingEngine(mateMove)))

    Vector(
      "writerless" -> writerless,
      "contaminated tactic" -> contaminatedTactic,
      "contaminated CheckGiven" -> contaminatedCheckGiven,
      "incomplete proof" -> incompleteProof,
      "refuted" -> refuted
    ).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("Stage-2 Checkmate and CheckGiven keep separate proof and Story homes"):
    val mateFacts = facts(mateFen)
    val checkmate = SceneCheckmate.write(mateFacts, mateMove).get.copy(proof = eventProof(99))
    val checkGiven = SceneCheckGiven.write(mateFacts, mateMove).get.copy(proof = eventProof(98))

    assertEquals(checkmate.scene, Scene.Checkmate)
    assertEquals(checkmate.writer, Some(StoryWriter.SceneCheckmate))
    assertEquals(checkmate.checkmateProof.exists(_.complete), true)
    assertEquals(checkmate.checkGivenProof, None)

    assertEquals(checkGiven.scene, Scene.CheckGiven)
    assertEquals(checkGiven.writer, Some(StoryWriter.SceneCheckGiven))
    assertEquals(checkGiven.checkGivenProof.exists(_.complete), true)
    assertEquals(checkGiven.checkmateProof, None)

    val verdicts = StoryTable.choose(Vector(checkGiven, checkmate))
    assertEquals(verdicts.map(_.story.scene).toSet, Set(Scene.Checkmate, Scene.CheckGiven))
    verdicts.foreach: verdict =>
      if verdict.story.scene == Scene.Checkmate then
        assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key), Some("checkmates"))
        if verdict.role == Role.Lead then
          assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("checkmates"))
        else assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None)
      if verdict.story.scene == Scene.CheckGiven && verdict.role == Role.Lead then
        assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key), Some("gives_check"))
