package lila.commentary.chess

private[commentary] final case class MateThreatProof(
    threateningSide: Side,
    rivalSide: Side,
    beforeBoard: Option[BoardFacts],
    afterBoard: Option[BoardFacts],
    storyProof: Option[StoryProof],
    movingPieceBefore: Option[Piece],
    movingPieceAfter: Option[Piece],
    originSquare: Option[Square],
    destinationSquare: Option[Square],
    rivalKingSquareAfterThreatMove: Option[Square],
    threatMove: Option[Line],
    nextMateMove: Option[Line],
    nextMateSan: Option[String],
    nextMateProofBoard: Option[BoardFacts],
    nextMateProof: Option[CheckmateProof],
    mateTargetKingSquare: Option[Square],
    sameBoardProof: Boolean,
    legalMove: Boolean,
    completeStoryProof: Boolean,
    exactAfterBoardReplay: Boolean,
    threatMoveIsNotImmediateCheckmate: Boolean,
    nextMateMoveVerifiedByCheckmateProof: Boolean,
    nextSidePlyLegalCheckmateAvailable: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = MateThreatProof.complete(this)

private[commentary] object MateThreatProof:
  def fromBoardFacts(facts: BoardFacts, threatMove: Line): MateThreatProof =
    fromBoardFacts(facts, threatMove, storyProof = Some(StoryProof.fromBoardFacts(facts, threatMove)))

  def fromBoardFacts(facts: BoardFacts, threatMove: Line, storyProof: Option[StoryProof]): MateThreatProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMoveRow = facts.seen.legalMoves.find(_.line == threatMove)
    val threateningSide = legalMoveRow.map(_.side).getOrElse(facts.sideToMove)
    val rivalSide = BoardFacts.opposite(threateningSide)
    val afterPieces = BoardFacts.piecesAfterLegalMove(facts, threatMove)
    val afterThreatFacts = BoardFacts.sameSideFactsAfterLegalMove(facts, threatMove)
    val completeStoryProof = storyProof.exists(_.completeForLine(threatMove))
    val movingPieceBefore = legalMoveRow.map(_.piece)
    val movingPieceAfter =
      afterPieces.flatMap(_.find(piece => piece.side == threateningSide && piece.square == threatMove.to))
    val rivalKingAfter =
      afterPieces.flatMap(_.find(piece => piece.side == rivalSide && piece.man == Man.King))
    val immediateCheckmate = CheckmateProof.fromBoardFacts(facts, threatMove)
    val exactAfterBoardReplay =
      sameBoardProof &&
        legalMoveRow.nonEmpty &&
        afterPieces.nonEmpty &&
        afterThreatFacts.nonEmpty &&
        movingPieceBefore.nonEmpty &&
        movingPieceAfter.nonEmpty &&
        rivalKingAfter.nonEmpty
    val legalNextMates =
      afterThreatFacts.toVector.flatMap: nextFacts =>
        nextFacts.sideLegal.lines.flatMap: line =>
          val proof = CheckmateProof.fromBoardFacts(nextFacts, line)
          Option.when(proof.complete)(line -> proof)
    val nextMate = legalNextMates.headOption
    val nextMateLine = nextMate.map(_._1)
    val nextMateProof = nextMate.map(_._2)
    val mateTargetKingSquare = nextMateProof.flatMap(_.rivalKingSquareAfter)
    val nextMateMoveVerifiedByCheckmateProof =
      afterThreatFacts.nonEmpty &&
        nextMateLine.nonEmpty &&
        nextMateProof.exists(proof =>
          proof.complete &&
            proof.matingSide == threateningSide &&
            proof.rivalSide == rivalSide &&
            nextMateLine.exists(line => proof.mateMove.contains(line))
        )
    val nextSidePlyLegalCheckmateAvailable =
      exactAfterBoardReplay &&
        completeStoryProof &&
        nextMate.nonEmpty &&
        nextMateMoveVerifiedByCheckmateProof
    val threatMoveIsNotImmediateCheckmate =
      legalMoveRow.nonEmpty &&
        !immediateCheckmate.complete
    val nextMateSan =
      afterThreatFacts.flatMap(nextFacts => nextMateLine.flatMap(BoardFacts.sanFor(nextFacts, _)))

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(legalMoveRow.isEmpty)("legal move identity"),
      Option.when(!completeStoryProof)("complete StoryProof"),
      Option.when(movingPieceBefore.isEmpty)("moving piece identity"),
      Option.when(movingPieceBefore.map(_.square).isEmpty)("origin square"),
      Option.when(movingPieceAfter.map(_.square).isEmpty)("destination square"),
      Option.when(rivalKingAfter.map(_.square).isEmpty)("rival king square after threat move"),
      Option.when(!exactAfterBoardReplay)("exact after-board replay"),
      Option.when(!threatMoveIsNotImmediateCheckmate)("threat move is not immediate checkmate"),
      Option.when(nextMateLine.isEmpty)("next mate move"),
      Option.when(nextMateSan.isEmpty)("next mate SAN"),
      Option.when(nextMateProof.isEmpty)("next CheckmateProof"),
      Option.when(mateTargetKingSquare.isEmpty)("mate target king square"),
      Option.when(!nextMateMoveVerifiedByCheckmateProof)("next mate move verified by CheckmateProof"),
      Option.when(!nextSidePlyLegalCheckmateAvailable)("next-side-ply legal checkmate")
    ).flatten.distinct

    MateThreatProof(
      threateningSide = threateningSide,
      rivalSide = rivalSide,
      beforeBoard = Some(facts),
      afterBoard = afterThreatFacts,
      storyProof = storyProof,
      movingPieceBefore = movingPieceBefore,
      movingPieceAfter = movingPieceAfter,
      originSquare = movingPieceBefore.map(_.square),
      destinationSquare = movingPieceAfter.map(_.square),
      rivalKingSquareAfterThreatMove = rivalKingAfter.map(_.square),
      threatMove = Some(threatMove),
      nextMateMove = nextMateLine,
      nextMateSan = nextMateSan,
      nextMateProofBoard = afterThreatFacts,
      nextMateProof = nextMateProof,
      mateTargetKingSquare = mateTargetKingSquare,
      sameBoardProof = sameBoardProof,
      legalMove = legalMoveRow.nonEmpty,
      completeStoryProof = completeStoryProof,
      exactAfterBoardReplay = exactAfterBoardReplay,
      threatMoveIsNotImmediateCheckmate = threatMoveIsNotImmediateCheckmate,
      nextMateMoveVerifiedByCheckmateProof = nextMateMoveVerifiedByCheckmateProof,
      nextSidePlyLegalCheckmateAvailable = nextSidePlyLegalCheckmateAvailable,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("MateThreatProof", missing))
    )

  private def complete(proof: MateThreatProof): Boolean =
    proof.missingEvidence.isEmpty &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.completeStoryProof &&
      proof.exactAfterBoardReplay &&
      proof.threatMoveIsNotImmediateCheckmate &&
      proof.nextMateMoveVerifiedByCheckmateProof &&
      proof.nextSidePlyLegalCheckmateAvailable &&
      proof.storyProofBindsThreatMove &&
      proof.afterBoardBindsThreatMove &&
      proof.checkmateProofBindsAfterBoard

  extension (proof: MateThreatProof)
    private def storyProofBindsThreatMove: Boolean =
      proof.threatMove.exists(line => proof.storyProof.exists(_.completeForLine(line)))

    private def afterBoardBindsThreatMove: Boolean =
      (for
        before <- proof.beforeBoard
        after <- proof.afterBoard
        route <- proof.threatMove
        replay <- BoardFacts.sameSideFactsAfterLegalMove(before, route)
      yield sameBoardSnapshot(replay, after)).contains(true)

    private def checkmateProofBindsAfterBoard: Boolean =
      (for
        after <- proof.afterBoard
        proofBoard <- proof.nextMateProofBoard
        move <- proof.nextMateMove
        mateProof <- proof.nextMateProof
        expected = CheckmateProof.fromBoardFacts(after, move)
      yield
        sameBoardSnapshot(after, proofBoard) &&
          expected == mateProof &&
          mateProof.complete &&
          mateProof.matingSide == proof.threateningSide &&
          mateProof.rivalSide == proof.rivalSide &&
          mateProof.mateMove.contains(move) &&
          mateProof.rivalKingSquareAfter == proof.mateTargetKingSquare
      ).contains(true)

  private def sameBoardSnapshot(left: BoardFacts, right: BoardFacts): Boolean =
    BoardFacts.sameBoardReady(left) &&
      BoardFacts.sameBoardReady(right) &&
      left.sideToMove == right.sideToMove &&
      left.header.plyFromStart == right.header.plyFromStart &&
      left.header.fullmoveNumber == right.header.fullmoveNumber &&
      left.header.halfmoveClock == right.header.halfmoveClock &&
      left.header.castlingMask == right.header.castlingMask &&
      left.header.epSquare == right.header.epSquare &&
      left.header.inCheckMask == right.header.inCheckMask &&
      left.pieces.sortBy(pieceKey) == right.pieces.sortBy(pieceKey) &&
      left.sideLegal.lines == right.sideLegal.lines &&
      left.rivalLegal.lines == right.rivalLegal.lines

  private def pieceKey(piece: Piece): (Int, Int, Int) =
    (piece.side.ordinal, piece.man.ordinal, piece.square.index)
