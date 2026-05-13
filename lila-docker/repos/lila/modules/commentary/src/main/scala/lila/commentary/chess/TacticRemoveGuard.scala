package lila.commentary.chess

private[commentary] object TacticRemoveGuard:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(removeGuardStory(story) && check.storyBound && check.evidenceReady && checkBindsStoryRoute(story, check)):
      story.copy(engineCheck = Some(check))

  def write(
      facts: BoardFacts,
      removeGuardMove: Option[Line],
      targetSquare: Option[Square],
      defenderSquare: Option[Square]
  ): Option[Story] =
    val removeGuardProof = RemoveGuardProof.fromBoardFacts(facts, removeGuardMove, targetSquare, defenderSquare)
    for
      route <- removeGuardMove
      routeSan <- BoardFacts.sanFor(facts, route)
      target <- removeGuardProof.guardedTarget
      defender <- removeGuardProof.removedDefender
      if WriterOpen
      if removeGuardProof.complete
      story = Story(
        scene = Scene.Tactic,
        tactic = Some(Tactic.RemoveGuard),
        side = removeGuardProof.side,
        rival = removeGuardProof.rivalSide,
        target = Some(target.square),
        anchor = Some(defender.square),
        route = Some(route),
        routeSan = Some(routeSan),
        proof = removeGuardProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, route),
        writer = Some(StoryWriter.TacticRemoveGuard),
        removeGuardProof = Some(removeGuardProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def removeGuardStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.TacticRemoveGuard) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.RemoveGuard) &&
      story.plan.isEmpty &&
      story.overloadProof.isEmpty &&
      story.deflectProof.isEmpty &&
      story.trapProof.isEmpty

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def removeGuardProofScore: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 20,
      immediacy = 85,
      forcing = 85,
      conversionPrize = 80,
      counterplayRisk = 45,
      kingHeat = 0,
      pieceSupport = 75,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 75
    )
