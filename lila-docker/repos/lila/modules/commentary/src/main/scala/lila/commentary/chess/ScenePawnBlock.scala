package lila.commentary.chess

private[commentary] object ScenePawnBlock:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(
      pawnBlockStory(story) &&
        check.storyBound &&
        check.evidenceReady &&
        checkBindsStoryRoute(story, check)
    ):
      story.copy(engineCheck = Some(check))

  def write(facts: BoardFacts, blockMove: Line): Option[Story] =
    val pawnBlockProof = PawnBlockProof.fromBoardFacts(facts, blockMove)
    for
      routeSan <- BoardFacts.sanFor(facts, blockMove)
      origin <- pawnBlockProof.originSquare
      target <- pawnBlockProof.blockedPawnNextAdvanceSquare
      if WriterOpen
      if pawnBlockProof.complete
      story = Story(
        scene = Scene.PawnBlock,
        tactic = None,
        plan = None,
        side = pawnBlockProof.blockingSide,
        rival = pawnBlockProof.rivalSide,
        target = Some(target),
        anchor = Some(origin),
        route = Some(blockMove),
        routeSan = Some(routeSan),
        proof = pawnBlockProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, blockMove),
        writer = Some(StoryWriter.ScenePawnBlock),
        pawnBlockProof = Some(pawnBlockProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def pawnBlockStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.ScenePawnBlock) &&
      story.scene == Scene.PawnBlock &&
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
      story.fileOpenedProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      story.proofFailures.isEmpty &&
      story.pawnBlockProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: PawnBlockProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.exactAfterBoardReplay &&
      proof.blockCreatedByMove &&
      proof.nextAdvanceSquareOccupiedAfter &&
      proof.occupyingPieceBelongsToBlockingSide &&
      proof.ordinaryDirectOneSquarePawnBlock &&
      proof.blockingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.blockMove.exists(move => story.route.contains(move)) &&
      proof.originSquare == story.anchor &&
      proof.blockedPawnNextAdvanceSquare == story.target &&
      proof.blockedPawn.exists(piece =>
        piece.side == proof.rivalSide &&
          piece.man == Man.Pawn &&
          proof.blockedPawnSquare.contains(piece.square)
      )

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def pawnBlockProofScore: Proof =
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
