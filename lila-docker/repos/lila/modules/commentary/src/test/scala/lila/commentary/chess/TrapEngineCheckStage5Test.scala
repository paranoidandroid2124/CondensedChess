package lila.commentary.chess

class TrapEngineCheckStage5Test extends munit.FunSuite:

  test("Stage-5 EngineCheck supports caps refutes and unknown attach only to existing Trap Story"):
    val story = trapStory

    Vector(
      EngineCheckStatus.Supports -> Role.Lead,
      EngineCheckStatus.Caps -> Role.Lead,
      EngineCheckStatus.Refutes -> Role.Blocked,
      EngineCheckStatus.Unknown -> Role.Lead
    ).foreach: (status, expectedRole) =>
      val checked = TacticTrap.withEngineCheck(story, engineCheck(story, status)).get
      val verdict = StoryTable.choose(Vector(checked)).head

      assertEquals(checked.engineCheck.map(_.status), Some(status), status.toString)
      assertEquals(verdict.role, expectedRole, status.toString)
      assertEquals(
        ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key),
        if status == EngineCheckStatus.Supports then Some("traps_piece") else None,
        status.toString
      )
      assertEquals(
        ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey),
        if status == EngineCheckStatus.Supports then Some("traps_piece") else None,
        status.toString
      )

  test("Stage-5 EngineCheck cannot create or repair Trap proof Story or wording"):
    val story = trapStory
    val check = engineCheck(story, EngineCheckStatus.Supports)
    val incomplete = story.copy(
      trapProof = story.trapProof.map(_.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("TrapProof", Vector("gap")))))
    )
    val noStoryCheck = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(trapMove),
      engineLine = Some(EngineLine(Vector(trapMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    assertEquals(TacticTrap.withEngineCheck(incomplete, check), None)
    assertEquals(TacticTrap.withEngineCheck(story.copy(trapProof = None), check), None)
    assertEquals(TacticTrap.withEngineCheck(story, noStoryCheck), None)
    assertEquals(TacticTrap.write(trapFacts, None), None)
    assertEquals(EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(trapMove),
      engineLine = Some(EngineLine(Vector(trapMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    ).storyBound, false)

  private def engineCheck(story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = trapFacts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(trapMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(if status == EngineCheckStatus.Refutes then 200 else 0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Trap Stage-5 FEN: $fen -> $error"), identity)

  private def trapStory: Story =
    TacticTrap.write(trapFacts, Some(trapMove)).get

  private val trapFacts = board("n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1")
  private val trapMove = Line(Square('a', 1), Square('a', 7))
