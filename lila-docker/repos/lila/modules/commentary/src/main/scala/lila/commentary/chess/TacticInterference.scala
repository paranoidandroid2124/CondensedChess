package lila.commentary.chess

private[commentary] object TacticInterference:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(
      interferenceStory(story) &&
        check.storyBound &&
        check.evidenceReady &&
        engineCheckCanOnlyBoundExistingStory(check) &&
        checkBindsStoryRoute(story, check)
    ):
      story.copy(engineCheck = Some(check))

  def write(
      facts: BoardFacts,
      sideMove: Option[Line],
      defenderSquare: Option[Square],
      blockingSquare: Option[Square],
      targetSquare: Option[Square]
  ): Option[Story] =
    val interferenceProof =
      InterferenceProof.fromBoardFacts(facts, sideMove, defenderSquare, blockingSquare, targetSquare)
    for
      route <- sideMove
      routeSan <- BoardFacts.sanFor(facts, route)
      target <- interferenceProof.targetSquare
      anchor <- interferenceProof.blockingSquare
      if WriterOpen
      if interferenceProof.complete
      story = Story(
        scene = Scene.Tactic,
        tactic = Some(Tactic.Interference),
        side = interferenceProof.side,
        rival = interferenceProof.rivalSide,
        target = Some(target),
        anchor = Some(anchor),
        route = Some(route),
        routeSan = Some(routeSan),
        proof = interferenceProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, route),
        writer = Some(StoryWriter.TacticInterference),
        interferenceProof = Some(interferenceProof)
      )
      if interferenceStory(story)
      if story.proofFailures.isEmpty
    yield story

  private def interferenceStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.TacticInterference) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Interference) &&
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
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.trapProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.proofFailures.isEmpty &&
      story.interferenceProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: InterferenceProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalSideMove &&
      proof.completeStoryProof &&
      proof.moveIsNonCapture &&
      proof.movedPieceLandsOnBlockingSquare &&
      proof.lineDefenderIsSlider &&
      proof.targetBound &&
      proof.defenderBlockingTargetCollinearOnSliderRay &&
      proof.blockingSquareStrictlyBetweenDefenderAndTarget &&
      proof.defenderLineContactBeforeMove &&
      proof.afterMoveBlockingSquareOccupiedByMovedPiece &&
      proof.defenderLineContactRemovedByBlocker &&
      proof.noEngineEvidenceUsed &&
      proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.sideMove.exists(move => story.route.contains(move)) &&
      proof.targetSquare.exists(square => story.target.contains(square)) &&
      proof.blockingSquare.exists(square => story.anchor.contains(square)) &&
      proof.movedPieceAfter.exists(piece => story.side == piece.side && story.anchor.contains(piece.square)) &&
      proof.lineDefenderBefore.exists(piece => story.rival == piece.side) &&
      proof.lineDefenderAfter.exists(piece => story.rival == piece.side)

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def engineCheckCanOnlyBoundExistingStory(check: EngineCheck): Boolean =
    check.status match
      case EngineCheckStatus.Supports | EngineCheckStatus.Caps | EngineCheckStatus.Refutes => true
      case _ => false

  private def interferenceProofScore: Proof =
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
