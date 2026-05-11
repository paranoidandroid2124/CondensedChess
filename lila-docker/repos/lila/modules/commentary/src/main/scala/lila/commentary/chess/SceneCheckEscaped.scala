package lila.commentary.chess

private[commentary] object SceneCheckEscaped:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(
      checkEscapedStory(story) &&
        check.storyBound &&
        check.evidenceReady &&
        checkBindsStoryRoute(story, check)
    ):
      story.copy(engineCheck = Some(check))

  def write(facts: BoardFacts, escapeMove: Line): Option[Story] =
    val checkEscapedProof = CheckEscapedProof.fromBoardFacts(facts, escapeMove)
    for
      routeSan <- BoardFacts.sanFor(facts, escapeMove)
      _ <- checkEscapedProof.beforeKingSquare
      origin <- checkEscapedProof.originSquare
      afterKing <- checkEscapedProof.afterKingSquare
      if WriterOpen
      if checkEscapedProof.complete
      story = Story(
        scene = Scene.CheckEscaped,
        tactic = None,
        plan = None,
        side = checkEscapedProof.escapingSide,
        rival = checkEscapedProof.rivalSide,
        target = Some(afterKing),
        anchor = Some(origin),
        route = Some(escapeMove),
        routeSan = Some(routeSan),
        proof = checkEscapedProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, escapeMove),
        writer = Some(StoryWriter.SceneCheckEscaped),
        checkEscapedProof = Some(checkEscapedProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def checkEscapedStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.SceneCheckEscaped) &&
      story.scene == Scene.CheckEscaped &&
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
      story.checkGivenProof.isEmpty &&
      story.proofFailures.isEmpty &&
      story.checkEscapedProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: CheckEscapedProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.exactBeforeBoardState &&
      proof.exactAfterBoardReplay &&
      proof.beforeBoardSideKingInCheck &&
      proof.afterBoardSideKingNotInCheck &&
      proof.checkEscapedByLegalMove &&
      proof.escapingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.escapeMove.exists(move => story.route.contains(move)) &&
      proof.beforeKingSquare.nonEmpty &&
      proof.originSquare == story.anchor &&
      proof.afterKingSquare == story.target

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def checkEscapedProofScore: Proof =
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
