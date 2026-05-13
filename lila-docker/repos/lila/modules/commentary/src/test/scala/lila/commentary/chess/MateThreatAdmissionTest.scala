package lila.commentary.chess

class MateThreatAdmissionTest extends munit.FunSuite:

  private val mateThreatFen = "7k/8/6K1/8/8/8/5Q2/8 w - - 0 1"
  private val mateThreatMove = Line(Square('g', 6), Square('h', 6))
  private val nextMateMove = Line(Square('f', 2), Square('f', 8))
  private val quietMove = Line(Square('g', 6), Square('f', 6))
  private val illegalMove = Line(Square('g', 6), Square('g', 8))
  private val immediateMateMove = Line(Square('f', 2), Square('f', 8))
  private val replyMove = Line(Square('h', 8), Square('g', 8))

  private def facts: BoardFacts =
    BoardFacts.fromFen(mateThreatFen).toOption.get

  private def selected(row: Story): Verdict =
    StoryTable.choose(Vector(row)).head

  private def engineCheck(row: Story, status: EngineCheckStatus, before: Int = 100, after: Int = 100): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(mateThreatMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  test("MateThreatProof proves only next-side-ply legal checkmate availability"):
    val boardFacts = facts
    val proof = MateThreatProof.fromBoardFacts(boardFacts, mateThreatMove)

    assertEquals(proof.complete, true)
    assertEquals(proof.threateningSide, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.threatMove, Some(mateThreatMove))
    assertEquals(proof.nextMateMove, Some(nextMateMove))
    assertEquals(proof.nextMateSan, Some("Qf8#"))
    assertEquals(proof.rivalKingSquareAfterThreatMove, Some(Square('h', 8)))
    assertEquals(proof.legalMove, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.nextSidePlyLegalCheckmateAvailable, true)
    assertEquals(proof.threatMoveIsNotImmediateCheckmate, true)
    assertEquals(proof.missingEvidence, Vector.empty)

    val story = SceneMateThreat.write(boardFacts, mateThreatMove).get
    assertEquals(story.scene, Scene.MateThreat)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('h', 8)))
    assertEquals(story.anchor, Some(Square('h', 6)))
    assertEquals(story.route, Some(mateThreatMove))
    assertEquals(story.routeSan, Some("Kh6"))
    assertEquals(story.writer, Some(StoryWriter.SceneMateThreat))
    assertEquals(story.mateThreatProof.exists(_.complete), true)
    assertEquals(story.checkmateProof, None)
    assertEquals(story.proofFailures, Vector.empty)

    val verdict = selected(story)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.selected, true)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key), Some("threatens_mate_next"))

  test("MateThreat stays silent for quiet illegal immediate-mate writerless and contaminated rows"):
    val boardFacts = facts

    val quietProof = MateThreatProof.fromBoardFacts(boardFacts, quietMove)
    assertEquals(quietProof.complete, false)
    assertEquals(quietProof.nextSidePlyLegalCheckmateAvailable, false)
    assertEquals(SceneMateThreat.write(boardFacts, quietMove), None)

    val illegalProof = MateThreatProof.fromBoardFacts(boardFacts, illegalMove)
    assertEquals(illegalProof.complete, false)
    assertEquals(illegalProof.legalMove, false)
    assertEquals(SceneMateThreat.write(boardFacts, illegalMove), None)

    val immediateMateProof = MateThreatProof.fromBoardFacts(boardFacts, immediateMateMove)
    assertEquals(CheckmateProof.fromBoardFacts(boardFacts, immediateMateMove).complete, true)
    assertEquals(immediateMateProof.complete, false)
    assertEquals(immediateMateProof.threatMoveIsNotImmediateCheckmate, false)
    assertEquals(SceneMateThreat.write(boardFacts, immediateMateMove), None)

    val story = SceneMateThreat.write(boardFacts, mateThreatMove).get
    Vector(
      "writerless" -> story.copy(writer = None),
      "checkmate contaminated" -> story.copy(checkmateProof = Some(CheckmateProof.fromBoardFacts(boardFacts, immediateMateMove))),
      "check-given contaminated" -> story.copy(checkGivenProof = Some(CheckGivenProof.fromBoardFacts(boardFacts, immediateMateMove))),
      "wrong scene" -> story.copy(scene = Scene.Checkmate),
      "tactic contaminated" -> story.copy(tactic = Some(Tactic.MateNet))
    ).foreach: (label, row) =>
      val verdict = selected(row)
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("MateThreat EngineCheck supports caps or refutes only an existing proof-backed story"):
    val story = SceneMateThreat.write(facts, mateThreatMove).get
    val supports = SceneMateThreat.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Supports)).get
    val caps = SceneMateThreat.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Caps)).get
    val refutes = engineCheck(story, EngineCheckStatus.Supports, before = 220, after = 20)
    val unbound = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(mateThreatMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(100)),
      evalAfter = Some(EngineEval(100)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    val supportedVerdict = selected(supports)
    assertEquals(supportedVerdict.role, Role.Lead)
    assertEquals(supportedVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(ExplanationPlan.fromSelected(supportedVerdict).flatMap(_.allowedClaim).map(_.key), Some("threatens_mate_next"))

    val cappedVerdict = selected(caps)
    assertEquals(cappedVerdict.role, Role.Lead)
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(cappedVerdict), None)

    val refuted = SceneMateThreat.withEngineCheck(story, refutes).get
    assertEquals(refuted.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    val refutedVerdict = selected(refuted)
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

    assertEquals(unbound.storyBound, false)
    assertEquals(unbound.status, EngineCheckStatus.Unknown)
    assertEquals(SceneMateThreat.withEngineCheck(story.copy(mateThreatProof = None), engineCheck(story, EngineCheckStatus.Supports)), None)
