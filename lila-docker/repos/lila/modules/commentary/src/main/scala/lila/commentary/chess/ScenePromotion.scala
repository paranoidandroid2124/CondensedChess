package lila.commentary.chess

private[commentary] object ScenePromotion:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(promotionStory(story) && check.storyBound && check.evidenceReady && checkBindsStoryRoute(story, check)):
      story.copy(engineCheck = Some(check))

  def write(facts: BoardFacts, promotionMove: Line): Option[Story] =
    val promotionProof = PromotionProof.fromBoardFacts(facts, promotionMove)
    for
      routeSan <- promotionSanFor(facts, promotionMove)
      origin <- promotionProof.originSquare
      promotionSquare <- promotionProof.promotionSquare
      if WriterOpen
      if promotionProof.complete
      story = Story(
        scene = Scene.Promotion,
        tactic = None,
        plan = None,
        side = promotionProof.side,
        rival = promotionProof.rivalSide,
        target = Some(promotionSquare),
        anchor = Some(origin),
        route = Some(promotionMove),
        routeSan = Some(routeSan),
        proof = promotionProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, promotionMove),
        writer = Some(StoryWriter.ScenePromotion),
        promotionProof = Some(promotionProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def promotionStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.ScenePromotion) &&
      story.scene == Scene.Promotion &&
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
      story.proofFailures.isEmpty &&
      story.proof.conversionPrize == 0 &&
      story.promotionProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: PromotionProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalPromotionMove &&
      proof.nonCapturing &&
      proof.exactBoardReplay &&
      proof.pawnReachesFinalRank &&
      proof.promotedPiece.nonEmpty &&
      proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.promotionMove.exists(move => story.route.contains(move)) &&
      proof.originSquare.exists(square => story.anchor.contains(square)) &&
      proof.promotionSquare.exists(square => story.target.contains(square)) &&
      proof.promotedPiece.exists(piece => story.target.contains(piece.square))

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def promotionSanFor(facts: BoardFacts, move: Line): Option[String] =
    BoardFacts
      .sansFor(facts, move)
      .filter(_.contains("="))
      .sortBy(promotionSanPreference)
      .headOption

  private def promotionSanPreference(san: String): Int =
    san.lastIndexOf('=') match
      case -1 => 4
      case index if index + 1 >= san.length => 4
      case index =>
        san.charAt(index + 1).toUpper match
          case 'Q' => 0
          case 'R' => 1
          case 'B' => 2
          case 'N' => 3
          case _   => 4

  private def promotionProofScore: Proof =
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
