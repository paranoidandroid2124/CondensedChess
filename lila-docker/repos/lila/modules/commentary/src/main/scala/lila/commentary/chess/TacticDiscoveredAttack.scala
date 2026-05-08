package lila.commentary.chess

private[commentary] object TacticDiscoveredAttack:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(discoveredAttackStory(story) && check.storyBound && check.evidenceReady && checkBindsStoryRoute(story, check)):
      story.copy(engineCheck = Some(check))

  def write(
      facts: BoardFacts,
      revealingMove: Option[Line],
      sliderSquare: Option[Square],
      targetSquare: Option[Square]
  ): Option[Story] =
    val lineProof = LineProof.fromBoardFacts(facts, revealingMove, sliderSquare, targetSquare)
    for
      route <- revealingMove
      routeSan <- BoardFacts.sanFor(facts, route)
      target <- lineProof.revealedTarget
      if WriterOpen
      if lineProof.complete
      anchor <- lineProof.movedPiece.orElse(lineProof.slider)
      story = Story(
        scene = Scene.Tactic,
        tactic = Some(Tactic.DiscoveredAttack),
        side = lineProof.side,
        rival = rivalOf(lineProof.side),
        target = Some(target.square),
        anchor = Some(anchor.square),
        route = Some(route),
        routeSan = Some(routeSan),
        proof = discoveredAttackProof,
        storyProof = StoryProof.fromBoardFacts(facts, route),
        writer = Some(StoryWriter.TacticDiscoveredAttack),
        lineProof = Some(lineProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def discoveredAttackStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.TacticDiscoveredAttack) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.DiscoveredAttack) &&
      story.plan.isEmpty

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def discoveredAttackProof: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 25,
      immediacy = 85,
      forcing = 80,
      conversionPrize = 80,
      counterplayRisk = 40,
      kingHeat = 0,
      pieceSupport = 70,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 75
    )

  private def rivalOf(side: Side): Side =
    side match
      case Side.White => Side.Black
      case Side.Black => Side.White
      case _          => Side.None
