package lila.commentary.chess

class QueenHitStage2Test extends munit.FunSuite:

  private val queenHitFen = "4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1"
  private val noQueenFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))
  private val quietMove = Line(Square('d', 2), Square('a', 2))
  private val illegalMove = Line(Square('d', 2), Square('h', 8))

  test("Stage-2 TacticQueenHit writer admits only complete QueenHitProof Story identity"):
    val facts = BoardFacts.fromFen(queenHitFen).toOption.get
    val story = TacticQueenHit.write(facts, Some(queenHitMove)).get
    val proof = story.queenHitProof.get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.QueenHit))
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.TacticQueenHit))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('h', 5)))
    assertEquals(story.anchor, Some(Square('h', 2)))
    assertEquals(story.route, Some(queenHitMove))
    assertEquals(story.routeSan, BoardFacts.sanFor(facts, queenHitMove))
    assertEquals(story.proofFailures, Vector.empty)

    assertEquals(proof.complete, true)
    assertEquals(proof.attackMove, Some(queenHitMove))
    assertEquals(proof.rivalQueenSquareAfter, story.target)
    assertEquals(proof.attackingPieceSquareAfter, story.anchor)
    assertEquals(proof.afterBoardQueenAttackedByMovingSide, true)
    assertEquals(proof.queenHitProducedOrRevealedByLegalMove, true)

    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(verdict.role, Role.Context)
    assertEquals(verdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("Stage-2 TacticQueenHit writer requires complete proof and blocks refuted or sibling rows"):
    val facts = BoardFacts.fromFen(queenHitFen).toOption.get
    val noQueenFacts = BoardFacts.fromFen(noQueenFen).toOption.get
    val story = TacticQueenHit.write(facts, Some(queenHitMove)).get

    assertEquals(TacticQueenHit.write(facts, Some(quietMove)), None)
    assertEquals(TacticQueenHit.write(facts, Some(illegalMove)), None)
    assertEquals(TacticQueenHit.write(noQueenFacts, Some(quietMove)), None)
    assertEquals(TacticQueenHit.write(facts, None), None)

    val refutes = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(queenHitMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(220)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    assertEquals(refutes.status, EngineCheckStatus.Refutes)
    val refutedStory = TacticQueenHit.withEngineCheck(story, refutes).get
    val refutedVerdict = StoryTable.choose(Vector(refutedStory)).head
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

    Vector(
      story.copy(tactic = Some(Tactic.Hanging)),
      story.copy(tactic = Some(Tactic.Fork), secondaryTarget = Some(Square('e', 8))),
      story.copy(tactic = Some(Tactic.Skewer), secondaryTarget = Some(Square('e', 8))),
      story.copy(tactic = Some(Tactic.Pin)),
      story.copy(tactic = Some(Tactic.RemoveGuard)),
      story.copy(scene = Scene.Material, tactic = None)
    ).foreach: sibling =>
      val verdict = StoryTable.choose(Vector(sibling)).head
      assertEquals(verdict.role, Role.Blocked)
