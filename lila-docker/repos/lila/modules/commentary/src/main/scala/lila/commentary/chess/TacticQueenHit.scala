package lila.commentary.chess

private[commentary] object TacticQueenHit:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(queenHitStory(story) && check.storyBound && check.evidenceReady && checkBindsStoryRoute(story, check)):
      story.copy(engineCheck = Some(check))

  def write(facts: BoardFacts, queenHitMove: Option[Line]): Option[Story] =
    val queenHitProof = queenHitMove.map(move => QueenHitProof.fromBoardFacts(facts, move))
    for
      route <- queenHitMove
      routeSan <- BoardFacts.sanFor(facts, route)
      proof <- queenHitProof
      queenSquare <- proof.rivalQueenSquareAfter
      attackingSquare <- proof.attackingPieceSquareAfter
      if WriterOpen
      if proof.complete
      story = Story(
        scene = Scene.Tactic,
        tactic = Some(Tactic.QueenHit),
        side = proof.attackingSide,
        rival = proof.rivalSide,
        target = Some(queenSquare),
        anchor = Some(attackingSquare),
        route = Some(route),
        routeSan = Some(routeSan),
        proof = queenHitProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, route),
        writer = Some(StoryWriter.TacticQueenHit),
        queenHitProof = Some(proof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def queenHitStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.TacticQueenHit) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.QueenHit) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      story.proofFailures.isEmpty &&
      story.queenHitProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: QueenHitProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.exactAfterBoardReplay &&
      proof.rivalQueenExistsAfter &&
      proof.afterBoardQueenAttackedByMovingSide &&
      proof.queenHitProducedOrRevealedByLegalMove &&
      proof.attackingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.attackMove.exists(move => story.route.contains(move)) &&
      proof.rivalQueenSquareAfter == story.target &&
      proof.attackingPieceSquareAfter == story.anchor

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def queenHitProofScore: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 20,
      immediacy = 50,
      forcing = 45,
      conversionPrize = 0,
      counterplayRisk = 45,
      kingHeat = 0,
      pieceSupport = 65,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 65
    )
