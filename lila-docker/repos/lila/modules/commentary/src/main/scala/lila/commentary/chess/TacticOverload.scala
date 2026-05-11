package lila.commentary.chess

private[commentary] object TacticOverload:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(overloadStory(story) && check.storyBound && check.evidenceReady && checkBindsStoryRoute(story, check)):
      story.copy(engineCheck = Some(check))

  def write(
      facts: BoardFacts,
      overloadMove: Option[Line],
      defenderSquare: Option[Square],
      firstDutyTarget: Option[Square],
      secondDutyTarget: Option[Square],
      testedDutyTarget: Option[Square]
  ): Option[Story] =
    val overloadRelations = OverloadProof.observeRelations(
      facts,
      overloadMove,
      defenderSquare,
      firstDutyTarget,
      secondDutyTarget,
      testedDutyTarget
    )
    val overloadProof = OverloadProof.fromRelations(facts, overloadMove, overloadRelations)
    for
      route <- overloadMove
      routeSan <- BoardFacts.sanFor(facts, route)
      defender <- overloadProof.defender
      testedTarget <- overloadProof.testedDutyTarget
      otherTarget <- overloadProof.otherDutyTarget
      if WriterOpen
      if overloadProof.complete
      story = Story(
        scene = Scene.Tactic,
        tactic = Some(Tactic.Overload),
        side = overloadProof.side,
        rival = overloadProof.rivalSide,
        target = Some(testedTarget.square),
        secondaryTarget = Some(otherTarget.square),
        anchor = Some(defender.square),
        route = Some(route),
        routeSan = Some(routeSan),
        proof = overloadProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, route),
        writer = Some(StoryWriter.TacticOverload),
        overloadProof = Some(overloadProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def overloadStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.TacticOverload) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Overload) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.proofFailures.isEmpty &&
      story.overloadProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: OverloadProof): Boolean =
    proof.complete &&
      proof.proofComplete &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.sameBoardLegalReplay &&
      proof.dualDutyDefender &&
      proof.dutyTargetsNonKingMaterial &&
      proof.testMoveAttacksDutyTargetAfter &&
      proof.noReplyPreservesBothDutyTargets &&
      proof.defenderDuty.exists(_.complete) &&
      proof.dualDefenderDuty.exists(_.complete) &&
      proof.overloadTest.exists(_.complete) &&
      proof.cannotSatisfyBoth.exists(_.complete) &&
      proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.route.exists(move => story.route.contains(move)) &&
      proof.target.exists(piece => story.target.contains(piece.square) && piece.side == story.rival) &&
      proof.secondaryTarget.exists(piece => story.secondaryTarget.contains(piece.square) && piece.side == story.rival) &&
      proof.anchor.exists(piece => story.anchor.contains(piece.square) && piece.side == story.rival)

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def overloadProofScore: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 20,
      immediacy = 85,
      forcing = 85,
      conversionPrize = 70,
      counterplayRisk = 45,
      kingHeat = 0,
      pieceSupport = 75,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 75
    )
