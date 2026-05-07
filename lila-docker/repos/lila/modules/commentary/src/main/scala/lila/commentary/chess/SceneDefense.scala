package lila.commentary.chess

private[commentary] object SceneDefense:
  val WriterOpen = true

  def withEngineCheck(story: Story, check: EngineCheck): Option[Story] =
    Option.when(defenseStory(story) && check.storyBound && check.evidenceReady && checkBindsStoryRoute(story, check)):
      story.copy(engineCheck = Some(check))

  def write(facts: BoardFacts, threatLine: Line, defenseLine: Line): Option[Story] =
    val threatProof = ThreatProof.fromBoardFacts(facts, threatLine)
    val defenseProof = DefenseProof.fromBoardFacts(facts, threatProof, defenseLine)
    for
      defendedTarget <- defenseProof.defendedTarget
      defenseMove <- defenseProof.defenseMove
      routeSan <- BoardFacts.sanFor(facts, defenseMove)
      if WriterOpen
      if threatProof.complete
      if defenseProof.complete
      if defenseProof.sameBoardProof
      if defenseProof.materialLossPrevented.exists(_ > 0)
      story = Story(
        scene = Scene.Defense,
        tactic = None,
        plan = None,
        side = defenseProof.defendingSide,
        rival = threatProof.rivalSide,
        target = Some(defendedTarget.square),
        anchor = Some(defenseMove.from),
        route = Some(defenseMove),
        routeSan = Some(routeSan),
        proof = defenseStoryProof(defenseProof),
        storyProof = StoryProof.fromBoardFacts(facts, defenseMove),
        writer = Some(StoryWriter.SceneDefense),
        threatProof = Some(threatProof),
        defenseProof = Some(defenseProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def defenseStory(story: Story): Boolean =
    story.writer.contains(StoryWriter.SceneDefense) &&
      story.scene == Scene.Defense &&
      story.tactic.isEmpty &&
      story.plan.isEmpty

  private def checkBindsStoryRoute(story: Story, check: EngineCheck): Boolean =
    check.checkedMove.exists(move => story.route.contains(move))

  private def defenseStoryProof(defenseProof: DefenseProof): Proof =
    val material = defenseProof.materialLossPrevented.getOrElse(0)
    val prize = math.min(90, math.max(80, material / 4))
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 35,
      immediacy = 90,
      forcing = 85,
      conversionPrize = prize,
      counterplayRisk = 30,
      kingHeat = 0,
      pieceSupport = 80,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 80
    )
