package lila.commentary.chess

private[commentary] object SceneFileOpened:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(
      fileOpenedStory(story) &&
        check.storyBound &&
        check.evidenceReady &&
        checkBindsStoryRoute(story, check)
    ):
      story.copy(engineCheck = Some(check))

  def write(facts: BoardFacts, move: Line): Option[Story] =
    val fileOpenedProof = FileOpenedProof.fromBoardFacts(facts, move)
    for
      routeSan <- BoardFacts.sanFor(facts, move)
      origin <- fileOpenedProof.originSquare
      destination <- fileOpenedProof.destinationSquare
      openedFile <- fileOpenedProof.openedFile
      if WriterOpen
      if fileOpenedProof.complete
      story = Story(
        scene = Scene.FileOpened,
        tactic = None,
        plan = None,
        side = fileOpenedProof.side,
        rival = fileOpenedProof.rivalSide,
        target = Some(destination),
        anchor = Some(origin),
        route = Some(move),
        routeSan = Some(routeSan),
        proof = fileOpenedProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, move),
        writer = Some(StoryWriter.SceneFileOpened),
        openedFile = Some(openedFile),
        fileOpenedProof = Some(fileOpenedProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def fileOpenedStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.SceneFileOpened) &&
      story.scene == Scene.FileOpened &&
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
      story.passedPawnCreatedProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      story.proofFailures.isEmpty &&
      story.fileOpenedProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: FileOpenedProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalPawnMove &&
      proof.nonPromotionMove &&
      proof.ordinaryPawnMoveOrCapture &&
      !proof.enPassantMove &&
      proof.leavesOriginFile &&
      proof.originFileOccupiedBeforeByMovingPawn &&
      !proof.originFileOpenBefore &&
      proof.exactAfterBoardReplay &&
      proof.afterBoardHasNoWhitePawnOnOriginFile &&
      proof.afterBoardHasNoBlackPawnOnOriginFile &&
      proof.originFileOpenAfter &&
      proof.openedFileIsOriginFile &&
      proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.openingMove.exists(move => story.route.contains(move)) &&
      proof.originSquare == story.anchor &&
      proof.destinationSquare == story.target &&
      proof.openedFile == story.openedFile &&
      proof.openedFile == proof.originFile

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def fileOpenedProofScore: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 100,
      persistence = 100,
      immediacy = 70,
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
