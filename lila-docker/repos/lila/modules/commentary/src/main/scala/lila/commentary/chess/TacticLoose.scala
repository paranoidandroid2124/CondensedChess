package lila.commentary.chess

private[commentary] object TacticLoose:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(looseStory(story) && check.storyBound && check.evidenceReady && checkBindsStoryRoute(story, check)):
      story.copy(engineCheck = Some(check))

  def write(facts: BoardFacts, looseMove: Option[Line]): Option[Story] =
    val looseProof = looseMove.map(move => LoosePieceProof.fromBoardFacts(facts, move))
    for
      route <- looseMove
      routeSan <- BoardFacts.sanFor(facts, route)
      proof <- looseProof
      targetSquare <- proof.targetPieceSquareAfter
      attackingSquare <- proof.attackingPieceSquareAfter
      if WriterOpen
      if proof.complete
      story = Story(
        scene = Scene.Tactic,
        tactic = Some(Tactic.Loose),
        side = proof.attackingSide,
        rival = proof.rivalSide,
        target = Some(targetSquare),
        anchor = Some(attackingSquare),
        route = Some(route),
        routeSan = Some(routeSan),
        proof = looseProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, route),
        writer = Some(StoryWriter.TacticLoose),
        loosePieceProof = Some(proof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def looseStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.TacticLoose) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Loose) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      story.proofFailures.isEmpty &&
      story.loosePieceProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: LoosePieceProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.exactAfterBoardReplay &&
      proof.targetRivalOwned &&
      proof.targetNonKing &&
      proof.afterBoardTargetAttackedByMovingSide &&
      proof.rivalLegalDefendersAfter.isEmpty &&
      proof.rivalSideHasNoLegalDefenderOfTarget &&
      proof.looseAttackProducedOrRevealedByLegalMove &&
      proof.attackingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.attackMove.exists(move => story.route.contains(move)) &&
      proof.targetPieceSquareAfter == story.target &&
      proof.attackingPieceSquareAfter == story.anchor

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def looseProofScore: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 20,
      immediacy = 45,
      forcing = 35,
      conversionPrize = 0,
      counterplayRisk = 45,
      kingHeat = 0,
      pieceSupport = 65,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 65
    )
