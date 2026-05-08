package lila.commentary.chess

private[commentary] object TacticPin:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(pinStory(story) && check.storyBound && check.evidenceReady && checkBindsStoryRoute(story, check)):
      story.copy(engineCheck = Some(check))

  def write(
      facts: BoardFacts,
      pinningMove: Option[Line],
      sliderSquare: Option[Square],
      targetSquare: Option[Square],
      kingSquare: Option[Square]
  ): Option[Story] =
    val pinProof = PinProof.fromBoardFacts(facts, pinningMove, sliderSquare, targetSquare, kingSquare)
    for
      route <- pinningMove
      routeSan <- BoardFacts.sanFor(facts, route)
      target <- pinProof.pinnedTarget
      slider <- pinProof.pinningSlider
      if WriterOpen
      if pinProof.complete
      story = Story(
        scene = Scene.Tactic,
        tactic = Some(Tactic.Pin),
        side = pinProof.side,
        rival = target.side,
        target = Some(target.square),
        anchor = Some(slider.square),
        route = Some(route),
        routeSan = Some(routeSan),
        proof = pinProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, route),
        writer = Some(StoryWriter.TacticPin),
        pinProof = Some(pinProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def pinStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.TacticPin) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Pin) &&
      story.plan.isEmpty

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def pinProofScore: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 25,
      immediacy = 75,
      forcing = 85,
      conversionPrize = 75,
      counterplayRisk = 40,
      kingHeat = 0,
      pieceSupport = 80,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 75
    )
