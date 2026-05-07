package lila.commentary.chess

private[commentary] object TacticFork:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(forkStory(story) && check.evidenceReady && checkBindsStoryRoute(story, check)):
      story.copy(engineCheck = Some(check))

  def write(
      facts: BoardFacts,
      forkMove: Option[Line],
      firstTarget: Option[Square],
      secondTarget: Option[Square]
  ): Option[Story] =
    val multiTargetProof = MultiTargetProof.fromBoardFacts(facts, forkMove, firstTarget, secondTarget)
    for
      route <- forkMove
      attacker <- multiTargetProof.attacker
      primaryTarget <- firstTarget
      secondaryTarget <- secondTarget
      if WriterOpen
      if multiTargetProof.complete
      story = Story(
        scene = Scene.Tactic,
        tactic = Some(Tactic.Fork),
        side = multiTargetProof.side,
        rival = rivalOf(multiTargetProof.side),
        target = Some(primaryTarget),
        secondaryTarget = Some(secondaryTarget),
        anchor = Some(attacker.square),
        route = Some(route),
        proof = forkProof(multiTargetProof),
        storyProof = StoryProof.fromBoardFacts(facts, route),
        writer = Some(StoryWriter.TacticFork),
        multiTargetProof = Some(multiTargetProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def forkStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.TacticFork) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Fork) &&
      story.plan.isEmpty

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def forkProof(proof: MultiTargetProof): Proof =
    val result = proof.materialOrTempoResult.getOrElse(0)
    val prize = math.min(90, math.max(80, result / 8))
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 20,
      immediacy = 85,
      forcing = 80,
      conversionPrize = prize,
      counterplayRisk = 30,
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
