package lila.commentary.chess

private[commentary] object TacticSkewer:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(
      skewerStory(story) &&
        check.storyBound &&
        check.evidenceReady &&
        checkBindsStoryRoute(story, check)
    ):
      story.copy(engineCheck = Some(check))

  def write(
      facts: BoardFacts,
      skewerMove: Option[Line],
      sliderSquare: Option[Square],
      frontTargetSquare: Option[Square],
      rearTargetSquare: Option[Square]
  ): Option[Story] =
    val skewerProof = SkewerProof.fromBoardFacts(facts, skewerMove, sliderSquare, frontTargetSquare, rearTargetSquare)
    for
      route <- skewerMove
      routeSan <- BoardFacts.sanFor(facts, route)
      slider <- skewerProof.skewerSlider
      frontTarget <- skewerProof.frontTarget
      rearTarget <- skewerProof.rearTarget
      if WriterOpen
      if skewerProof.complete
      story = Story(
        scene = Scene.Tactic,
        tactic = Some(Tactic.Skewer),
        side = skewerProof.side,
        rival = skewerProof.rivalSide,
        target = Some(frontTarget.square),
        secondaryTarget = Some(rearTarget.square),
        anchor = Some(slider.square),
        route = Some(route),
        routeSan = Some(routeSan),
        proof = skewerProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, route),
        writer = Some(StoryWriter.TacticSkewer),
        skewerProof = Some(skewerProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def skewerStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.TacticSkewer) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Skewer) &&
      story.plan.isEmpty

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def skewerProofScore: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 20,
      immediacy = 80,
      forcing = 80,
      conversionPrize = 75,
      counterplayRisk = 45,
      kingHeat = 0,
      pieceSupport = 75,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 75
    )
