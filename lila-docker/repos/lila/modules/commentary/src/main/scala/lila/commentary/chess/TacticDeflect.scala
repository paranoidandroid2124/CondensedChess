package lila.commentary.chess

private[commentary] object TacticDeflect:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(deflectStory(story) && check.storyBound && check.evidenceReady && checkBindsStoryRoute(story, check)):
      story.copy(engineCheck = Some(check))

  def write(
      facts: BoardFacts,
      sideMove: Option[Line],
      rivalReply: Option[Line],
      defenderSquare: Option[Square],
      targetSquare: Option[Square]
  ): Option[Story] =
    val deflectProof = DeflectProof.fromBoardFacts(facts, sideMove, rivalReply, defenderSquare, targetSquare)
    for
      route <- sideMove
      routeSan <- BoardFacts.sanFor(facts, route)
      target <- deflectProof.targetBeforeReply
      defenderAfterReply <- deflectProof.defenderAfterReply
      if WriterOpen
      if deflectProof.complete
      story = Story(
        scene = Scene.Tactic,
        tactic = Some(Tactic.Deflect),
        side = deflectProof.side,
        rival = deflectProof.rivalSide,
        target = Some(target.square),
        anchor = Some(defenderAfterReply.square),
        route = Some(route),
        routeSan = Some(routeSan),
        proof = deflectProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, route),
        writer = Some(StoryWriter.TacticDeflect),
        deflectProof = Some(deflectProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def deflectStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.TacticDeflect) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Deflect) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.overloadProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.trapProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      !story.engineCheck.exists(_.status == EngineCheckStatus.Refutes) &&
      story.proofFailures.isEmpty &&
      story.deflectProof.exists(proofBindsStory(story, _))

  private def proofBindsStory(story: Story, proof: DeflectProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalSideMove &&
      proof.legalRivalReply &&
      proof.replyByNamedDefender &&
      proof.defenderGuardedTargetBeforeReply &&
      proof.defenderNoLongerGuardsTargetAfterReply &&
      proof.targetRemainsAfterReply &&
      proof.defenderRemainsAfterReply &&
      proof.sideMoveDoesNotCaptureDefender &&
      proof.completeStoryProof &&
      proof.noEngineEvidenceUsed &&
      proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.sideMove.exists(move => story.route.contains(move)) &&
      proof.targetBeforeReply.exists(piece =>
        story.rival == piece.side &&
          story.target.contains(piece.square) &&
          piece.man != Man.King
      ) &&
      proof.targetAfterReply.exists(piece =>
        story.rival == piece.side &&
          story.target.contains(piece.square) &&
          piece.man != Man.King
      ) &&
      proof.defenderAfterReply.exists(piece =>
        story.rival == piece.side &&
          story.anchor.contains(piece.square)
      )

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def deflectProofScore: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 100,
      immediacy = 85,
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
