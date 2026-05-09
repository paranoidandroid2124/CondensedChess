package lila.commentary.chess

private[commentary] object ScenePawnStop:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(
      pawnStopStory(story) &&
        check.storyBound &&
        check.evidenceReady &&
        checkBindsStoryRoute(story, check)
    ):
      story.copy(engineCheck = Some(check))

  def write(facts: BoardFacts, stopMove: Line): Option[Story] =
    val pawnStopProof = PawnStopProof.fromBoardFacts(facts, stopMove)
    for
      routeSan <- BoardFacts.sanFor(facts, stopMove)
      stoppedSquare <- pawnStopProof.nextAdvanceSquare
      if WriterOpen
      if pawnStopProof.complete
      story = Story(
        scene = Scene.PawnStop,
        tactic = None,
        plan = None,
        side = pawnStopProof.side,
        rival = pawnStopProof.rivalSide,
        target = Some(stoppedSquare),
        anchor = Some(stopMove.from),
        route = Some(stopMove),
        routeSan = Some(routeSan),
        proof = pawnStopProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, stopMove),
        writer = Some(StoryWriter.ScenePawnStop),
        pawnStopProof = Some(pawnStopProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def pawnStopStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.ScenePawnStop) &&
      story.scene == Scene.PawnStop &&
      story.tactic.isEmpty &&
      story.plan.isEmpty

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def pawnStopProofScore: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 100,
      immediacy = 60,
      forcing = 0,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = 0,
      pawnSupport = 100,
      sourceFit = 0,
      novelty = 0,
      clarity = 100
    )
