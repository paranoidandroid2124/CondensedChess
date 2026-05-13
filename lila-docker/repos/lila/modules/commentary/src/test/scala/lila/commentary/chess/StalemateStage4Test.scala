package lila.commentary.chess

class StalemateStage4Test extends munit.FunSuite:

  private val stalemateFen = "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1"
  private val stalemateMove = Line(Square('g', 5), Square('g', 6))
  private val otherMove = Line(Square('g', 5), Square('g', 7))

  private def facts: BoardFacts =
    BoardFacts.fromFen(stalemateFen).toOption.get

  private def story: Story =
    SceneStalemate.write(facts, stalemateMove).get

  private def engineFromStory(
      row: Option[Story],
      requestedStatus: EngineCheckStatus,
      engineLine: Option[EngineLine] = Some(EngineLine(Vector(stalemateMove))),
      evalBefore: EngineEval = EngineEval(0),
      evalAfter: EngineEval = EngineEval(0)
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = row,
      engineLine = engineLine,
      replyLine = Some(EngineLine(Vector(stalemateMove))),
      evalBefore = Some(evalBefore),
      evalAfter = Some(evalAfter),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = requestedStatus
    )

  test("Stalemate-4 EngineCheck attaches only to an existing complete same-board SceneStalemate Story"):
    val row = story
    val supported = engineFromStory(Some(row), EngineCheckStatus.Supports)
    val noStory = engineFromStory(None, EngineCheckStatus.Supports)
    val routeMismatch = engineFromStory(Some(row), EngineCheckStatus.Supports, Some(EngineLine(Vector(otherMove))))
    val incompleteStalemate = row.copy(
      stalemateProof = row.stalemateProof.map(
        _.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("StalemateProof", Vector("no legal moves"))))
      )
    )
    val incomplete = engineFromStory(Some(incompleteStalemate), EngineCheckStatus.Supports)
    val writerless = engineFromStory(Some(row.copy(writer = None)), EngineCheckStatus.Supports)

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
    assertEquals(writerless.storyBound, false)
    assertEquals(writerless.status, EngineCheckStatus.Unknown)

  test("Stalemate-4 Supports Caps Refutes and Unknown create no engine or draw expression"):
    val base = story
    val supports = base.copy(engineCheck = Some(engineFromStory(Some(base), EngineCheckStatus.Supports)))
    val caps = base.copy(engineCheck = Some(engineFromStory(Some(base), EngineCheckStatus.Caps)))
    val refutes = base.copy(engineCheck = Some(engineFromStory(Some(base), EngineCheckStatus.Refutes)))
    val unknown = base.copy(engineCheck = Some(engineFromStory(Some(base), EngineCheckStatus.Unknown)))

    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head
    val unknownVerdict = StoryTable.choose(Vector(unknown)).head

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(supportsVerdict).flatMap(_.allowedClaim).map(_.key), Some("stalemates"))
    assertEquals(ExplanationPlan.fromSelected(supportsVerdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("stalemates"))

    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), None)

    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(refutesVerdict.selected, false)
    assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None)

    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(ExplanationPlan.fromSelected(unknownVerdict), None)

  test("Stalemate-4 engine-only draw-looking evidence cannot create SceneStalemate or forbidden wording"):
    val engineOnly =
      EngineCheck.fromEvidence(
        sameBoardProof = true,
        checkedMove = Some(stalemateMove),
        engineLine = Some(EngineLine(Vector(stalemateMove))),
        replyLine = Some(EngineLine(Vector(stalemateMove))),
        evalBefore = Some(EngineEval(0)),
        evalAfter = Some(EngineEval(0)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    assertEquals(engineOnly.storyBound, false)
    assertEquals(engineOnly.publicClaimAllowed, false)
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty)

    val claimKeys = ExplanationClaim.values.map(_.key).toVector
    Vector(
      "draw",
      "draws_endgame",
      "tablebase_draw",
      "eval_number",
      "best_move",
      "only_move",
      "forced_draw",
      "winning_thrown_away",
      "blunder",
      "decisive_mistake"
    ).foreach: closedKey =>
      assertEquals(claimKeys.contains(closedKey), false, closedKey)
