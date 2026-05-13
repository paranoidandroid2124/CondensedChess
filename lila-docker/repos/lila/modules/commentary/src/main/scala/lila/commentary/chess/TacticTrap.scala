package lila.commentary.chess

private[commentary] object TacticTrap:
  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(
      trapStory(story) &&
        check.storyBound &&
        check.evidenceReady &&
        checkBindsStoryRoute(story, check)
    ):
      story.copy(engineCheck = Some(check))

  def write(facts: BoardFacts, trapMove: Option[Line]): Option[Story] =
    val trapProof = trapMove.map(move => TrapProof.fromBoardFacts(facts, move))
    for
      route <- trapMove
      routeSan <- BoardFacts.sanFor(facts, route)
      proof <- trapProof
      targetSquare <- proof.targetPieceSquareAfter
      anchorSquare <- proof.anchorSquareAfter
      if proof.complete
      story = Story(
        scene = Scene.Tactic,
        tactic = Some(Tactic.Trap),
        side = proof.attackingSide,
        rival = proof.rivalSide,
        target = Some(targetSquare),
        anchor = Some(anchorSquare),
        route = Some(route),
        routeSan = Some(routeSan),
        proof = trapProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, route),
        writer = Some(StoryWriter.TacticTrap),
        trapProof = Some(proof)
      )
      if trapStory(story)
      if story.proofFailures.isEmpty
    yield story

  private def trapStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.TacticTrap) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Trap) &&
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
      story.fileOpenedProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.proofFailures.isEmpty &&
      story.trapProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: TrapProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.nonCapturingMove &&
      proof.exactAfterBoardReplay &&
      proof.completeStoryProof &&
      proof.targetRivalMinor &&
      proof.targetDefendedByRivalSide &&
      proof.afterBoardTargetAttackedByMovingSide &&
      proof.targetHasLegalMoves &&
      proof.everyTargetMoveUnsafe &&
      proof.routeDoesNotGiveCheck &&
      proof.routeDoesNotGiveMate &&
      proof.routeDoesNotPromote &&
      proof.noEngineEvidenceUsed &&
      proof.attackingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.trapMove.exists(move => story.route.contains(move)) &&
      proof.targetPieceSquareAfter == story.target &&
      proof.anchorSquareAfter == story.anchor &&
      proof.targetPieceAfter.exists(piece =>
        story.rival == piece.side &&
          (piece.man == Man.Knight || piece.man == Man.Bishop) &&
          proof.targetPieceSquareAfter.contains(piece.square)
      ) &&
      proof.movingPieceAfter.exists(piece =>
        story.side == piece.side &&
          proof.anchorSquareAfter.contains(piece.square) &&
          story.anchor.contains(piece.square)
      )

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def trapProofScore: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 100,
      immediacy = 75,
      forcing = 0,
      conversionPrize = 0,
      counterplayRisk = 20,
      kingHeat = 0,
      pieceSupport = 100,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 100
    )
