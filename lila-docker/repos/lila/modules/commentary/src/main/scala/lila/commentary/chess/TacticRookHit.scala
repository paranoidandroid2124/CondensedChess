package lila.commentary.chess

private[commentary] object TacticRookHit:
  val WriterOpen = true

  def write(
      facts: BoardFacts,
      rookHitMove: Option[Line],
      targetSquare: Option[Square],
      engineCheck: Option[EngineCheck] = None
  ): Option[Story] =
    for
      route <- rookHitMove
      proof = RookHitProof.fromBoardFacts(facts, route, targetSquare)
      story <- fromProof(facts, route, proof)
      result <- engineCheck.fold(Some(story))(check => withEngineCheck(story, check))
    yield result

  def fromProof(facts: BoardFacts, route: Line, proof: RookHitProof): Option[Story] =
    for
      routeSan <- BoardFacts.sanFor(facts, route)
      target <- proof.targetSquare
      anchor <- proof.attackingPieceSquareAfter
      if WriterOpen
      if proof.complete
      if proof.sameBoardProof
      if proof.legalMove
      if proof.completeStoryProof
      if proof.exactAfterBoardReplay
      if proof.targetPieceIsRivalRook
      if proof.afterBoardRookAttackedByMovingSide
      if proof.routeAndTargetBindSameBoard
      if proof.attackMove.contains(route)
      if proof == RookHitProof.fromBoardFacts(facts, route, proof.targetSquare)
      story = Story(
        scene = Scene.Tactic,
        tactic = Some(Tactic.RookHit),
        side = proof.attackingSide,
        rival = proof.rivalSide,
        target = Some(target),
        anchor = Some(anchor),
        route = Some(route),
        routeSan = Some(routeSan),
        proof = rookHitProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, route),
        writer = Some(StoryWriter.TacticRookHit),
        rookHitProof = Some(proof)
      )
      if validStory(story)
    yield story

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(validStory(story) && engineCheckBindsStory(story, check)):
      story.copy(engineCheck = Some(check))

  private[commentary] def validStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.TacticRookHit) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.RookHit) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.overloadProof.isEmpty &&
      story.deflectProof.isEmpty &&
      story.decoyProof.isEmpty &&
      story.interferenceProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.trapProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      story.proofFailures.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.rookHitProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: RookHitProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.completeStoryProof &&
      proof.exactAfterBoardReplay &&
      proof.targetPieceIsRivalRook &&
      proof.afterBoardRookAttackedByMovingSide &&
      proof.routeAndTargetBindSameBoard &&
      proof.attackingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.attackMove.exists(move => story.route.contains(move)) &&
      proof.targetSquare == story.target &&
      proof.attackingPieceSquareAfter == story.anchor &&
      proof.targetPieceAfter.exists(piece => piece.side == story.rival && piece.man == Man.Rook)

  private def engineCheckBindsStory(story: Story, check: EngineCheck): Boolean =
      check.storyBound &&
      check.evidenceReady &&
      (check.status == EngineCheckStatus.Supports || check.status == EngineCheckStatus.Caps) &&
      check.checkedMove.exists(move => story.route.contains(move))

  private def rookHitProofScore: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 100,
      immediacy = 100,
      forcing = 100,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = 100,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 100
    )
