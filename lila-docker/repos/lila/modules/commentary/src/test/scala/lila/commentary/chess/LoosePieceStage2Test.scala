package lila.commentary.chess

class LoosePieceStage2Test extends munit.FunSuite:

  private val looseFen = "4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"
  private val guardedFen = "4k3/8/8/7b/5n2/8/3R4/4K3 w - - 0 1"
  private val looseMove = Line(Square('d', 2), Square('h', 2))
  private val quietMove = Line(Square('d', 2), Square('a', 2))
  private val illegalMove = Line(Square('d', 2), Square('h', 8))

  test("Stage-2 TacticLoose writer admits only complete LoosePieceProof Story identity"):
    val facts = BoardFacts.fromFen(looseFen).toOption.get
    val story = TacticLoose.write(facts, Some(looseMove)).get
    val proof = story.loosePieceProof.get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Loose))
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.TacticLoose))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('h', 5)))
    assertEquals(story.anchor, Some(Square('h', 2)))
    assertEquals(story.route, Some(looseMove))
    assertEquals(story.routeSan, BoardFacts.sanFor(facts, looseMove))
    assertEquals(story.proofFailures, Vector.empty)

    assertEquals(proof.complete, true)
    assertEquals(proof.attackMove, Some(looseMove))
    assertEquals(proof.targetPieceSquareAfter, story.target)
    assertEquals(proof.attackingPieceSquareAfter, story.anchor)
    assertEquals(proof.targetRivalOwned, true)
    assertEquals(proof.targetNonKing, true)
    assertEquals(proof.afterBoardTargetAttackedByMovingSide, true)
    assertEquals(proof.rivalSideHasNoLegalDefenderOfTarget, true)
    assertEquals(proof.looseAttackProducedOrRevealedByLegalMove, true)

    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.queenHitProof, None)
    assertEquals(verdict.role, Role.Context)
    assertEquals(verdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("Stage-2 TacticLoose writer requires complete proof and blocks refuted or sibling rows"):
    val facts = BoardFacts.fromFen(looseFen).toOption.get
    val guardedFacts = BoardFacts.fromFen(guardedFen).toOption.get
    val story = TacticLoose.write(facts, Some(looseMove)).get

    assertEquals(TacticLoose.write(facts, Some(quietMove)), None)
    assertEquals(TacticLoose.write(facts, Some(illegalMove)), None)
    assertEquals(TacticLoose.write(guardedFacts, Some(looseMove)), None)
    assertEquals(TacticLoose.write(facts, None), None)

    val refutes = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(looseMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(220)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    assertEquals(refutes.status, EngineCheckStatus.Refutes)
    val refutedStory = TacticLoose.withEngineCheck(story, refutes).get
    val refutedVerdict = StoryTable.choose(Vector(refutedStory)).head
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

    Vector(
      story.copy(tactic = Some(Tactic.Hanging)),
      story.copy(scene = Scene.Material, tactic = None),
      story.copy(tactic = Some(Tactic.QueenHit)),
      story.copy(tactic = Some(Tactic.Fork), secondaryTarget = Some(Square('e', 8))),
      story.copy(tactic = Some(Tactic.Skewer), secondaryTarget = Some(Square('e', 8))),
      story.copy(tactic = Some(Tactic.RemoveGuard))
    ).foreach: sibling =>
      val verdict = StoryTable.choose(Vector(sibling)).head
      assertEquals(verdict.role, Role.Blocked)
      assertEquals(verdict.leadAllowed, false)
