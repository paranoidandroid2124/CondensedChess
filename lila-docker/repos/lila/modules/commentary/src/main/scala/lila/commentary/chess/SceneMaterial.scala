package lila.commentary.chess

private[commentary] object SceneMaterial:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(materialStory(story) && check.storyBound && check.evidenceReady && checkBindsStoryRoute(story, check)):
      story.copy(engineCheck = Some(check))

  def write(facts: BoardFacts, captureLine: Line): Option[Story] =
    val captureResult = CaptureResult.fromBoardFacts(facts, captureLine)
    for
      capturingPiece <- captureResult.capturingPiece
      targetPiece <- captureResult.targetPiece
      routeSan <- BoardFacts.sanFor(facts, captureLine)
      if WriterOpen
      if captureResult.positiveMaterial
      if captureResult.sameBoardProof
      if captureResult.missingEvidence.isEmpty
      if captureResult.materialResult.nonEmpty
      if captureResult.boundedExchangeSequence.nonEmpty
      story = Story(
        scene = Scene.Material,
        tactic = None,
        side = captureResult.side,
        rival = rivalOf(captureResult.side),
        target = Some(targetPiece.square),
        anchor = Some(capturingPiece.square),
        route = Some(captureLine),
        routeSan = Some(routeSan),
        proof = materialProof(captureResult),
        storyProof = StoryProof.fromBoardFacts(facts, captureLine),
        writer = Some(StoryWriter.SceneMaterial),
        captureResult = Some(captureResult)
      )
      if story.proofFailures.isEmpty
    yield story

  private def materialStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.SceneMaterial) &&
      story.scene == Scene.Material &&
      story.tactic.isEmpty &&
      story.plan.isEmpty

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def materialProof(captureResult: CaptureResult): Proof =
    val material = captureResult.materialResult.getOrElse(0)
    val prize = math.min(90, math.max(80, material / 4))
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 75,
      immediacy = 60,
      forcing = 20,
      conversionPrize = prize,
      counterplayRisk = 20,
      kingHeat = 0,
      pieceSupport = 75,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 85
    )

  private def rivalOf(side: Side): Side =
    side match
      case Side.White => Side.Black
      case Side.Black => Side.White
      case _          => Side.None
