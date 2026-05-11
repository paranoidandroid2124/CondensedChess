package lila.commentary.chess

private[commentary] object SceneCheckmate:
  val WriterOpen = true

  def write(facts: BoardFacts, mateMove: Line): Option[Story] =
    val checkmateProof = CheckmateProof.fromBoardFacts(facts, mateMove)
    for
      routeSan <- BoardFacts.sanFor(facts, mateMove)
      origin <- checkmateProof.originSquare
      rivalKing <- checkmateProof.rivalKingSquareAfter
      if WriterOpen
      if checkmateProof.complete
      story = Story(
        scene = Scene.Checkmate,
        tactic = None,
        plan = None,
        side = checkmateProof.matingSide,
        rival = checkmateProof.rivalSide,
        target = Some(rivalKing),
        anchor = Some(origin),
        route = Some(mateMove),
        routeSan = Some(routeSan),
        proof = checkmateProofScore,
        storyProof = StoryProof.fromBoardFacts(facts, mateMove),
        writer = Some(StoryWriter.SceneCheckmate),
        checkmateProof = Some(checkmateProof)
      )
      if story.proofFailures.isEmpty
    yield story

  private def checkmateProofScore: Proof =
    Proof(
      boardProof = 95,
      lineProof = 95,
      ownerProof = 95,
      anchorProof = 95,
      routeProof = 95,
      persistence = 0,
      immediacy = 100,
      forcing = 100,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 100,
      pieceSupport = 0,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 100
    )
