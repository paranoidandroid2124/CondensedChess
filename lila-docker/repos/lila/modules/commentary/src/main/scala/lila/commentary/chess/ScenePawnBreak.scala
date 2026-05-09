package lila.commentary.chess

private[commentary] object ScenePawnBreak:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(
      pawnBreakStory(story) &&
        check.storyBound &&
        check.evidenceReady &&
        checkBindsStoryRoute(story, check)
    ):
      story.copy(engineCheck = Some(check))

  def write(facts: BoardFacts, breakMove: Line): Option[Story] =
    val pawnBreakProof = PawnBreakProof.fromBoardFacts(facts, breakMove)
    for
      routeSan <- BoardFacts.sanFor(facts, breakMove)
      pawnBefore <- pawnBreakProof.pawnBefore
      targetPawn <- pawnBreakProof.targetPawn
      if WriterOpen
      if pawnBreakProof.complete
      story = Story(
        scene = Scene.PawnBreak,
        tactic = None,
        plan = None,
        side = pawnBreakProof.side,
        rival = pawnBreakProof.rivalSide,
        target = Some(targetPawn.square),
        anchor = Some(pawnBefore.square),
        route = Some(breakMove),
        routeSan = Some(routeSan),
        proof = pawnBreakProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, breakMove),
        writer = Some(StoryWriter.ScenePawnBreak),
        pawnBreakProof = Some(pawnBreakProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def pawnBreakStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.ScenePawnBreak) &&
      story.scene == Scene.PawnBreak &&
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
      story.pawnCaptureProof.isEmpty &&
      story.passedPawnCreatedProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      story.proofFailures.isEmpty &&
      story.pawnBreakProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: PawnBreakProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalPawnMove &&
      proof.nonPromotionMove &&
      proof.nonCapturingMove &&
      proof.exactAfterBoardReplay &&
      proof.directPawnLeverAfterMove &&
      proof.leverCreatedByMove &&
      proof.singleRivalPawnTarget &&
      proof.contactKinds == Vector(PawnBreakContactKind.PawnChallengesPawn, PawnBreakContactKind.PawnLeverCreated) &&
      proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.breakMove.exists(move => story.route.contains(move)) &&
      proof.originSquare == story.anchor &&
      proof.targetPawn.exists(piece => story.target.contains(piece.square))

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def pawnBreakProofScore: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
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
