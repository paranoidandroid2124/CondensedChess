package lila.commentary.chess

private[commentary] object TacticHanging:
  val WriterOpen = true

  def write(facts: BoardFacts, captureLine: Line): Option[Story] =
    val captureResult = CaptureResult.fromBoardFacts(facts, captureLine)
    for
      capturingPiece <- captureResult.capturingPiece
      targetPiece <- captureResult.targetPiece
      if WriterOpen
      if captureResult.positiveMaterial
      if targetPiece.man != Man.Pawn && targetPiece.man != Man.King
      if targetAttackedByCapturingPiece(facts, capturingPiece, targetPiece)
      story = Story(
        scene = Scene.Tactic,
        tactic = Some(Tactic.Hanging),
        side = captureResult.side,
        rival = rivalOf(captureResult.side),
        target = Some(targetPiece.square),
        anchor = Some(capturingPiece.square),
        route = Some(captureLine),
        proof = hangingProof(captureResult),
        storyProof = StoryProof.fromBoardFacts(facts, captureLine),
        writer = Some(StoryWriter.TacticHanging),
        captureResult = Some(captureResult)
      )
      if story.proofFailures.isEmpty
    yield story

  private def targetAttackedByCapturingPiece(facts: BoardFacts, capturingPiece: Piece, targetPiece: Piece): Boolean =
    facts.seen.attacks.exists(attack => attack.attacker == capturingPiece && attack.target == targetPiece)

  private def hangingProof(captureResult: CaptureResult): Proof =
    val material = captureResult.materialResult.getOrElse(0)
    val prize = math.min(99, math.max(80, material / 4))
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 30,
      immediacy = 90,
      forcing = 80,
      conversionPrize = prize,
      counterplayRisk = 20,
      kingHeat = 0,
      pieceSupport = 70,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 80
    )

  private def rivalOf(side: Side): Side =
    side match
      case Side.White => Side.Black
      case Side.Black => Side.White
      case _ => Side.None
