package lila.commentary.chess

private[commentary] object SceneStalemate:
  val WriterOpen = true

  def write(facts: BoardFacts, stalemateMove: Line): Option[Story] =
    val stalemateProof = StalemateProof.fromBoardFacts(facts, stalemateMove)
    for
      routeSan <- BoardFacts.sanFor(facts, stalemateMove)
      origin <- stalemateProof.originSquare
      rivalKing <- stalemateProof.rivalKingSquareAfter
      if WriterOpen
      if stalemateProof.complete
      story = Story(
        scene = Scene.Stalemate,
        tactic = None,
        plan = None,
        side = stalemateProof.stalematingSide,
        rival = stalemateProof.rivalSide,
        target = Some(rivalKing),
        anchor = Some(origin),
        route = Some(stalemateMove),
        routeSan = Some(routeSan),
        proof = stalemateProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, stalemateMove),
        writer = Some(StoryWriter.SceneStalemate),
        stalemateProof = Some(stalemateProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def stalemateProofScore: Proof =
    Proof(
      boardProof = 95,
      lineProof = 95,
      ownerProof = 95,
      anchorProof = 95,
      routeProof = 95,
      persistence = 100,
      immediacy = 100,
      forcing = 0,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = 100,
      pawnSupport = 100,
      sourceFit = 0,
      novelty = 0,
      clarity = 100
    )
