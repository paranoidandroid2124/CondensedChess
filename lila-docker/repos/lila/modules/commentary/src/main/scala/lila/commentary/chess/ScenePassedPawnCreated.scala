package lila.commentary.chess

private[commentary] object ScenePassedPawnCreated:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(
      passedPawnCreatedStory(story) &&
        check.storyBound &&
        check.evidenceReady &&
        checkBindsStoryRoute(story, check)
    ):
      story.copy(engineCheck = Some(check))

  def write(facts: BoardFacts, creatingMove: Line): Option[Story] =
    val passedPawnCreatedProof = PassedPawnCreatedProof.fromBoardFacts(facts, creatingMove)
    for
      routeSan <- BoardFacts.sanFor(facts, creatingMove)
      pawnBefore <- passedPawnCreatedProof.pawnBefore
      createdPawn <- passedPawnCreatedProof.createdPassedPawn
      if WriterOpen
      if passedPawnCreatedProof.complete
      story = Story(
        scene = Scene.PassedPawnCreated,
        tactic = None,
        plan = None,
        side = passedPawnCreatedProof.side,
        rival = passedPawnCreatedProof.rivalSide,
        target = Some(createdPawn.square),
        anchor = Some(pawnBefore.square),
        route = Some(creatingMove),
        routeSan = Some(routeSan),
        proof = passedPawnCreatedProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, creatingMove),
        writer = Some(StoryWriter.ScenePassedPawnCreated),
        passedPawnCreatedProof = Some(passedPawnCreatedProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def passedPawnCreatedStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.ScenePassedPawnCreated) &&
      story.scene == Scene.PassedPawnCreated &&
      story.tactic.isEmpty &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnCaptureProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      story.proofFailures.isEmpty &&
      story.passedPawnCreatedProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: PassedPawnCreatedProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.exactBeforeBoard &&
      proof.legalPawnMove &&
      proof.ordinaryPawnMoveOrCapture &&
      proof.nonPromotionMove &&
      !proof.passedBefore &&
      proof.exactAfterBoardReplay &&
      proof.passedAfter &&
      proof.exactlyOneNewPassedPawn &&
      proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.creatingMove.exists(move => story.route.contains(move)) &&
      proof.originSquare == story.anchor &&
      proof.createdPassedPawn.exists(piece => story.target.contains(piece.square))

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def passedPawnCreatedProofScore: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 100,
      immediacy = 80,
      forcing = 0,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = 0,
      pawnSupport = 100,
      sourceFit = 0,
      novelty = 0,
      clarity = 100
    )
