package lila.commentary.chess

private[commentary] object TacticDecoy:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(decoyStory(story) && check.storyBound && check.evidenceReady && checkBindsStoryRoute(story, check)):
      story.copy(engineCheck = Some(check))

  def write(
      facts: BoardFacts,
      sideMove: Option[Line],
      rivalReply: Option[Line],
      namedPieceSquare: Option[Square],
      decoySquare: Option[Square],
      trapFollowUpProof: Option[TrapProof]
  ): Option[Story] =
    val decoyProof =
      DecoyProof.fromBoardFacts(facts, sideMove, rivalReply, namedPieceSquare, decoySquare, trapFollowUpProof)
    for
      route <- sideMove
      routeSan <- BoardFacts.sanFor(facts, route)
      target <- decoyProof.decoySquare
      anchor <- sideMovedPieceAfter(decoyProof, route)
      if WriterOpen
      if decoyProof.complete
      story = Story(
        scene = Scene.Tactic,
        tactic = Some(Tactic.Decoy),
        side = decoyProof.side,
        rival = decoyProof.rivalSide,
        target = Some(target),
        anchor = Some(anchor.square),
        route = Some(route),
        routeSan = Some(routeSan),
        proof = decoyProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, route),
        writer = Some(StoryWriter.TacticDecoy),
        decoyProof = Some(decoyProof)
      )
      if decoyStory(story)
      if story.proofFailures.isEmpty
    yield story

  private def decoyStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.TacticDecoy) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Decoy) &&
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
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.trapProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.proofFailures.isEmpty &&
      story.decoyProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: DecoyProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalSideMove &&
      proof.legalRivalReply &&
      proof.replyByNamedPiece &&
      proof.rivalPieceRemainsAfterReply &&
      proof.replyLandsOnDecoySquare &&
      proof.decoySquareEqualsLandingSquare &&
      proof.completeTrapFollowUpProof &&
      proof.trapFollowUpBindsSamePieceAndSquare &&
      proof.completeStoryProof &&
      proof.noEngineEvidenceUsed &&
      proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.sideMove.exists(move => story.route.contains(move)) &&
      proof.decoySquare == story.target &&
      proof.landingSquare == story.target &&
      proof.namedPieceAfterReply.exists(piece => story.rival == piece.side && story.target.contains(piece.square)) &&
      proof.sideMove.flatMap(move => sideMovedPieceAfter(proof, move)).exists(piece => story.anchor.contains(piece.square))

  private def sideMovedPieceAfter(proof: DecoyProof, move: Line): Option[Piece] =
    proof.afterSideMoveBoard.find(piece => piece.side == proof.side && piece.square == move.to)

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def decoyProofScore: Proof =
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
      pieceSupport = 100,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 100
    )
