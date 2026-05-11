package lila.commentary.chess

class CheckmateStage4Test extends munit.FunSuite:

  private val mateFen = "k7/2Q5/2K5/8/8/8/8/8 w - - 0 1"
  private val mateMove = Line(Square('c', 7), Square('b', 7))
  private val otherMove = Line(Square('c', 7), Square('c', 8))

  private def facts: BoardFacts =
    BoardFacts.fromFen(mateFen).toOption.get

  private def story: Story =
    SceneCheckmate.write(facts, mateMove).get

  private def engineFromStory(
      row: Option[Story],
      requestedStatus: EngineCheckStatus,
      engineLine: Option[EngineLine] = Some(EngineLine(Vector(mateMove))),
      evalBefore: EngineEval = EngineEval(0),
      evalAfter: EngineEval = EngineEval(0)
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = row,
      engineLine = engineLine,
      replyLine = Some(EngineLine(Vector(mateMove))),
      evalBefore = Some(evalBefore),
      evalAfter = Some(evalAfter),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = requestedStatus
    )

  test("Stage-4 EngineCheck attaches only to an existing complete same-board SceneCheckmate Story"):
    val row = story
    val supported = engineFromStory(Some(row), EngineCheckStatus.Supports)
    val noStory = engineFromStory(None, EngineCheckStatus.Supports)
    val routeMismatch = engineFromStory(Some(row), EngineCheckStatus.Supports, Some(EngineLine(Vector(otherMove))))
    val incompleteCheckmate = row.copy(
      checkmateProof = row.checkmateProof.map(
        _.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("CheckmateProof", Vector("no legal escape"))))
      )
    )
    val incomplete = engineFromStory(Some(incompleteCheckmate), EngineCheckStatus.Supports)

    assertEquals(supported.storyBound, true)
    assertEquals(supported.sameBoardProof, true)
    assertEquals(supported.status, EngineCheckStatus.Supports)
    assertEquals(supported.publicClaimAllowed, false)

    assertEquals(noStory.storyBound, false)
    assertEquals(noStory.status, EngineCheckStatus.Unknown)
    assertEquals(routeMismatch.storyBound, true)
    assertEquals(routeMismatch.status, EngineCheckStatus.Unknown)
    assert(routeMismatch.missingEvidence.exists(_.missing.contains("same Story route")))
    assertEquals(incomplete.storyBound, false)
    assertEquals(incomplete.status, EngineCheckStatus.Unknown)

  test("Stage-4 Supports Caps Refutes and Unknown reuse existing StoryTable boundaries"):
    val base = story
    val supports = base.copy(engineCheck = Some(engineFromStory(Some(base), EngineCheckStatus.Supports)))
    val caps = base.copy(engineCheck = Some(engineFromStory(Some(base), EngineCheckStatus.Caps)))
    val refutes = base.copy(
      engineCheck = Some(
        engineFromStory(
          Some(base),
          EngineCheckStatus.Supports,
          evalBefore = EngineEval(250),
          evalAfter = EngineEval(0)
        )
      )
    )
    val unknown = base.copy(engineCheck = Some(engineFromStory(Some(base), EngineCheckStatus.Unknown)))

    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head
    val unknownVerdict = StoryTable.choose(Vector(unknown)).head

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(supportsVerdict).flatMap(_.allowedClaim).map(_.key), Some("checkmates"))
    assertEquals(ExplanationPlan.fromSelected(supportsVerdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("checkmates"))

    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), None)

    assertEquals(refutes.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None)

    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(ExplanationPlan.fromSelected(unknownVerdict), None)

  test("Stage-4 engine-only mate-looking evidence cannot create Checkmate wording or public expression"):
    val engineOnly =
      EngineCheck.fromEvidence(
        sameBoardProof = true,
        checkedMove = Some(mateMove),
        engineLine = Some(EngineLine(Vector(mateMove))),
        replyLine = Some(EngineLine(Vector(mateMove))),
        evalBefore = Some(EngineEval(0)),
        evalAfter = Some(EngineEval(10000)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
    val rawPvMate =
      EngineCheck.fromEvidence(
        sameBoardProof = true,
        checkedMove = Some(mateMove),
        engineLine = Some(EngineLine(Vector(mateMove, Line(Square('a', 8), Square('a', 7))))),
        replyLine = Some(EngineLine(Vector(mateMove))),
        evalBefore = Some(EngineEval(0)),
        evalAfter = Some(EngineEval(10000)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    assertEquals(engineOnly.storyBound, false)
    assertEquals(engineOnly.publicClaimAllowed, false)
    assertEquals(rawPvMate.storyBound, false)
    assertEquals(rawPvMate.publicClaimAllowed, false)
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty)
    assert(ExplanationClaim.values.exists(_.key == "checkmates"))
