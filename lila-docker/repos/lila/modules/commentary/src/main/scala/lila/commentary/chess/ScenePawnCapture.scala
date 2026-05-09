package lila.commentary.chess

private[commentary] object ScenePawnCapture:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(
      pawnCaptureStory(story) &&
        check.storyBound &&
        check.evidenceReady &&
        checkBindsStoryRoute(story, check)
    ):
      story.copy(engineCheck = Some(check))

  def write(facts: BoardFacts, captureMove: Line): Option[Story] =
    val pawnCaptureProof = PawnCaptureProof.fromBoardFacts(facts, captureMove)
    for
      routeSan <- BoardFacts.sanFor(facts, captureMove)
      pawnBefore <- pawnCaptureProof.pawnBefore
      capturedPawn <- pawnCaptureProof.capturedPawn
      if WriterOpen
      if pawnCaptureProof.complete
      story = Story(
        scene = Scene.PawnCapture,
        tactic = None,
        plan = None,
        side = pawnCaptureProof.side,
        rival = pawnCaptureProof.rivalSide,
        target = Some(capturedPawn.square),
        anchor = Some(pawnBefore.square),
        route = Some(captureMove),
        routeSan = Some(routeSan),
        proof = pawnCaptureProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, captureMove),
        writer = Some(StoryWriter.ScenePawnCapture),
        pawnCaptureProof = Some(pawnCaptureProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def pawnCaptureStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.ScenePawnCapture) &&
      story.scene == Scene.PawnCapture &&
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
      story.passedPawnCreatedProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      story.proofFailures.isEmpty &&
      story.pawnCaptureProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: PawnCaptureProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalPawnMove &&
      proof.legalPawnCapture &&
      proof.nonPromotionMove &&
      proof.ordinaryDiagonalPawnCapture &&
      proof.pawnCapturesPawn &&
      proof.exactAfterBoardReplay &&
      proof.singleRivalPawnCaptured &&
      proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.captureMove.exists(move => story.route.contains(move)) &&
      proof.originSquare == story.anchor &&
      proof.capturedPawn.exists(piece => story.target.contains(piece.square))

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def pawnCaptureProofScore: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 100,
      immediacy = 90,
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
