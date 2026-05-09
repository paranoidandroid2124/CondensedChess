package lila.commentary.chess

private[commentary] object ScenePromotionThreat:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(promotionThreatStory(story) && check.storyBound && check.evidenceReady && checkBindsStoryRoute(story, check)):
      story.copy(engineCheck = Some(check))

  def write(facts: BoardFacts, creatingMove: Line): Option[Story] =
    val promotionThreatProof = PromotionThreatProof.fromBoardFacts(facts, creatingMove)
    for
      routeSan <- BoardFacts.sanFor(facts, creatingMove)
      pawnBefore <- promotionThreatProof.pawnBefore
      promotionSquare <- promotionThreatProof.promotionSquare
      if WriterOpen
      if promotionThreatProof.complete
      story = Story(
        scene = Scene.PromotionThreat,
        tactic = None,
        plan = None,
        side = promotionThreatProof.side,
        rival = promotionThreatProof.rivalSide,
        target = Some(promotionSquare),
        anchor = Some(pawnBefore.square),
        route = Some(creatingMove),
        routeSan = Some(routeSan),
        proof = promotionThreatProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, creatingMove),
        writer = Some(StoryWriter.ScenePromotionThreat),
        promotionThreatProof = Some(promotionThreatProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def promotionThreatStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.ScenePromotionThreat) &&
      story.scene == Scene.PromotionThreat &&
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
      story.promotionThreatProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: PromotionThreatProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalPawnMove &&
      proof.nonPromotionCreatingMove &&
      proof.exactAfterBoardReplay &&
      proof.pawnOnPenultimateRankAfter &&
      proof.nextMovePromotionLegal &&
      proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.creatingMove.exists(move => story.route.contains(move)) &&
      proof.pawnBefore.exists(piece => story.anchor.contains(piece.square)) &&
      proof.promotionSquare.exists(square => story.target.contains(square)) &&
      proof.nextPromotionMove == proof.promotionRoute

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def promotionThreatProofScore: Proof =
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
      kingHeat = 0,
      pieceSupport = 0,
      pawnSupport = 100,
      sourceFit = 0,
      novelty = 0,
      clarity = 100
    )
