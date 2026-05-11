package lila.commentary.chess

class StalemateStage2Test extends munit.FunSuite:

  private val stalemateFen = "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1"
  private val stalemateMove = Line(Square('g', 5), Square('g', 6))
  private val checkmateFen = "k7/2Q5/2K5/8/8/8/8/8 w - - 0 1"
  private val checkmateMove = Line(Square('c', 7), Square('b', 7))
  private val legalWithRepliesFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val legalWithRepliesMove = Line(Square('d', 2), Square('a', 2))

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

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

  test("Stalemate-2 SceneStalemate writes only exact complete StalemateProof rows"):
    val story = SceneStalemate.write(facts(stalemateFen), stalemateMove).get

    assertEquals(story.scene, Scene.Stalemate)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('h', 8)))
    assertEquals(story.anchor, Some(Square('g', 5)))
    assertEquals(story.route, Some(stalemateMove))
    assertEquals(story.routeSan, Some("Qg6"))
    assertEquals(story.writer, Some(StoryWriter.SceneStalemate))
    assertEquals(story.stalemateProof.exists(_.complete), true)
    assertEquals(story.checkmateProof, None)
    assertEquals(story.checkGivenProof, None)
    assertEquals(story.checkEscapedProof, None)
    assertEquals(story.proofFailures, Vector.empty)

    val verdict = StoryTable.choose(Vector(story)).head
    assertEquals(verdict.story, story)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.selected, true)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("stalemates"))

  test("Stalemate-2 SceneStalemate stays silent for checkmate legal replies illegal writerless contaminated and refuted rows"):
    val stalemateFacts = facts(stalemateFen)
    val story = SceneStalemate.write(stalemateFacts, stalemateMove).get

    assertEquals(SceneStalemate.write(facts(checkmateFen), checkmateMove), None)
    assertEquals(SceneStalemate.write(facts(legalWithRepliesFen), legalWithRepliesMove), None)
    assertEquals(SceneStalemate.write(stalemateFacts, Line(Square('g', 5), Square('h', 8))), None)

    val writerless = story.copy(writer = None)
    val contaminatedTactic = story.copy(tactic = Some(Tactic.SafeCheck))
    val contaminatedCheckmate = story.copy(
      checkmateProof = Some(CheckmateProof.fromBoardFacts(facts(checkmateFen), checkmateMove))
    )
    val contaminatedCheckGiven = story.copy(
      checkGivenProof = Some(CheckGivenProof.fromBoardFacts(facts(checkmateFen), checkmateMove))
    )
    val contaminatedCheckEscaped = story.copy(
      checkEscapedProof = Some(
        CheckEscapedProof.fromBoardFacts(
          facts("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1"),
          Line(Square('e', 1), Square('f', 1))
        )
      )
    )
    val incompleteProof = story.copy(
      stalemateProof = story.stalemateProof.map(
        _.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("StalemateProof", Vector("no legal moves"))))
      )
    )
    val refuted = story.copy(engineCheck = Some(refutingEngine(stalemateMove)))

    Vector(
      "writerless" -> writerless,
      "contaminated tactic" -> contaminatedTactic,
      "contaminated Checkmate" -> contaminatedCheckmate,
      "contaminated CheckGiven" -> contaminatedCheckGiven,
      "contaminated CheckEscaped" -> contaminatedCheckEscaped,
      "incomplete proof" -> incompleteProof,
      "refuted" -> refuted
    ).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("Stalemate-2 keeps terminal and check meanings in separate homes"):
    val stalemate = SceneStalemate.write(facts(stalemateFen), stalemateMove).get
    val checkmate = SceneCheckmate.write(facts(checkmateFen), checkmateMove).get
    val checkGiven = SceneCheckGiven.write(facts(checkmateFen), checkmateMove).get

    assertEquals(stalemate.scene, Scene.Stalemate)
    assertEquals(stalemate.writer, Some(StoryWriter.SceneStalemate))
    assertEquals(stalemate.stalemateProof.exists(_.complete), true)
    assertEquals(stalemate.checkmateProof, None)
    assertEquals(stalemate.checkGivenProof, None)
    assertEquals(stalemate.checkEscapedProof, None)

    assertEquals(checkmate.scene, Scene.Checkmate)
    assertEquals(checkmate.writer, Some(StoryWriter.SceneCheckmate))
    assertEquals(checkmate.checkmateProof.exists(_.complete), true)
    assertEquals(checkmate.stalemateProof, None)

    assertEquals(checkGiven.scene, Scene.CheckGiven)
    assertEquals(checkGiven.writer, Some(StoryWriter.SceneCheckGiven))
    assertEquals(checkGiven.checkGivenProof.exists(_.complete), true)
    assertEquals(checkGiven.stalemateProof, None)

    val verdicts = StoryTable.choose(Vector(stalemate, checkmate, checkGiven))
    assert(verdicts.exists(_.story.scene == Scene.Stalemate))
    assert(verdicts.exists(_.story.scene == Scene.Checkmate))
    assert(verdicts.exists(_.story.scene == Scene.CheckGiven))
    verdicts.foreach: verdict =>
      if verdict.story.scene == Scene.Stalemate then
        assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("stalemates"))
