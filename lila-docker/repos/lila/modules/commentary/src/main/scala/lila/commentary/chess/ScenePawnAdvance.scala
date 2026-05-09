package lila.commentary.chess

private[commentary] object ScenePawnAdvance:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(pawnAdvanceStory(story) && check.storyBound && check.evidenceReady && checkBindsStoryRoute(story, check)):
      story.copy(engineCheck = Some(check))

  def write(facts: BoardFacts, advanceMove: Line): Option[Story] =
    val pawnAdvanceProof = PawnAdvanceProof.fromBoardFacts(facts, advanceMove)
    for
      routeSan <- BoardFacts.sanFor(facts, advanceMove)
      pawnBefore <- pawnAdvanceProof.pawnBefore
      pawnAfter <- pawnAdvanceProof.pawnAfter
      if WriterOpen
      if pawnAdvanceProof.complete
      story = Story(
        scene = Scene.PawnAdvance,
        tactic = None,
        plan = None,
        side = pawnAdvanceProof.side,
        rival = pawnAdvanceProof.rivalSide,
        target = Some(pawnAfter.square),
        anchor = Some(pawnBefore.square),
        route = Some(advanceMove),
        routeSan = Some(routeSan),
        proof = pawnAdvanceProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, advanceMove),
        writer = Some(StoryWriter.ScenePawnAdvance),
        pawnAdvanceProof = Some(pawnAdvanceProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def pawnAdvanceStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.ScenePawnAdvance) &&
      story.scene == Scene.PawnAdvance &&
      story.tactic.isEmpty &&
      story.plan.isEmpty

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def pawnAdvanceProofScore: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 100,
      immediacy = 45,
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
