package lila.commentary.chess

private[commentary] object SceneCheckGiven:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(
      checkGivenStory(story) &&
        check.storyBound &&
        check.evidenceReady &&
        checkBindsStoryRoute(story, check)
    ):
      story.copy(engineCheck = Some(check))

  def write(facts: BoardFacts, checkMove: Line): Option[Story] =
    val checkGivenProof = CheckGivenProof.fromBoardFacts(facts, checkMove)
    for
      routeSan <- BoardFacts.sanFor(facts, checkMove)
      origin <- checkGivenProof.originSquare
      rivalKing <- checkGivenProof.rivalKingSquareAfter
      if WriterOpen
      if checkGivenProof.complete
      story = Story(
        scene = Scene.CheckGiven,
        tactic = None,
        plan = None,
        side = checkGivenProof.checkingSide,
        rival = checkGivenProof.rivalSide,
        target = Some(rivalKing),
        anchor = Some(origin),
        route = Some(checkMove),
        routeSan = Some(routeSan),
        proof = checkGivenProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, checkMove),
        writer = Some(StoryWriter.SceneCheckGiven),
        checkGivenProof = Some(checkGivenProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def checkGivenStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.SceneCheckGiven) &&
      story.scene == Scene.CheckGiven &&
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
      story.skewerProof.isEmpty &&
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
      story.proofFailures.isEmpty &&
      story.checkGivenProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: CheckGivenProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.exactAfterBoardReplay &&
      proof.afterBoardRivalKingInCheck &&
      proof.checkProducedByLegalMove &&
      proof.checkingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.checkMove.exists(move => story.route.contains(move)) &&
      proof.originSquare == story.anchor &&
      proof.rivalKingSquareAfter == story.target

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def checkGivenProofScore: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 0,
      immediacy = 100,
      forcing = 80,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 100,
      pieceSupport = 0,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 100
    )
