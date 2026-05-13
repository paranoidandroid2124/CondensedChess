package lila.commentary.chess

private[commentary] object SceneMateThreat:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(
        mateThreatStory(story) &&
        check.storyBound &&
        check.evidenceReady &&
        checkBindsStoryRoute(story, check)
    ):
      story.copy(engineCheck = Some(check))

  def write(facts: BoardFacts, threatMove: Line): Option[Story] =
    val mateThreatProof = MateThreatProof.fromBoardFacts(facts, threatMove)
    for
      routeSan <- BoardFacts.sanFor(facts, threatMove)
      destination <- mateThreatProof.destinationSquare
      rivalKing <- mateThreatProof.rivalKingSquareAfterThreatMove
      if WriterOpen
      if mateThreatProof.complete
      story = Story(
        scene = Scene.MateThreat,
        tactic = None,
        plan = None,
        side = mateThreatProof.threateningSide,
        rival = mateThreatProof.rivalSide,
        target = Some(rivalKing),
        anchor = Some(destination),
        route = Some(threatMove),
        routeSan = Some(routeSan),
        proof = mateThreatProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, threatMove),
        writer = Some(StoryWriter.SceneMateThreat),
        mateThreatProof = Some(mateThreatProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def mateThreatStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.SceneMateThreat) &&
      story.scene == Scene.MateThreat &&
      story.tactic.isEmpty &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.secondaryTarget.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.overloadProof.isEmpty &&
      story.deflectProof.isEmpty &&
      story.decoyProof.isEmpty &&
      story.interferenceProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.rookHitProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.trapProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnBlockProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      story.pawnCaptureProof.isEmpty &&
      story.passedPawnCreatedProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.openedFile.isEmpty &&
      story.checkGivenProof.isEmpty &&
      story.checkEscapedProof.isEmpty &&
      story.checkmateProof.isEmpty &&
      story.stalemateProof.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.proofFailures.isEmpty &&
      story.mateThreatProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: MateThreatProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.exactAfterBoardReplay &&
      proof.threatMoveIsNotImmediateCheckmate &&
      proof.nextSidePlyLegalCheckmateAvailable &&
      proof.threateningSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.threatMove.exists(move => story.route.contains(move)) &&
      proof.destinationSquare.exists(square => story.anchor.contains(square)) &&
      proof.rivalKingSquareAfterThreatMove.exists(square => story.target.contains(square)) &&
      proof.nextMateMove.nonEmpty &&
      proof.nextMateSan.nonEmpty

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def mateThreatProofScore: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 100,
      immediacy = 100,
      forcing = 0,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 95,
      pieceSupport = 100,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 100
    )
