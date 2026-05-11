package lila.commentary.chess

private[commentary] final case class StalemateProof(
    stalematingSide: Side,
    rivalSide: Side,
    movingPieceBefore: Option[Piece],
    movingPieceAfter: Option[Piece],
    originSquare: Option[Square],
    destinationSquare: Option[Square],
    rivalKingSquareAfter: Option[Square],
    stalemateMove: Option[Line],
    sameBoardProof: Boolean,
    legalMove: Boolean,
    exactAfterBoardReplay: Boolean,
    afterBoardRivalSideNotInCheck: Boolean,
    afterBoardRivalSideHasNoLegalMoves: Boolean,
    stalemateProducedByLegalMove: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object StalemateProof:
  def fromBoardFacts(facts: BoardFacts, move: Line): StalemateProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMoveRow = facts.seen.legalMoves.find(_.line == move)
    val stalematingSide = legalMoveRow.map(_.side).getOrElse(facts.sideToMove)
    val rivalSide = BoardFacts.opposite(stalematingSide)
    val afterPieces = BoardFacts.piecesAfterLegalMove(facts, move)
    val movingPieceBefore = legalMoveRow.map(_.piece)
    val movingPieceAfter =
      afterPieces.flatMap(_.find(piece => piece.side == stalematingSide && piece.square == move.to))
    val rivalKingAfter =
      afterPieces.flatMap(_.find(piece => piece.side == rivalSide && piece.man == Man.King))
    val rivalInCheckAfter = BoardFacts.sideInCheckAfterLegalMove(facts, move, rivalSide)
    val rivalLegalMoveCountAfter = BoardFacts.sideLegalMoveCountAfterLegalMove(facts, move, rivalSide)
    val exactAfterBoardReplay =
      sameBoardProof &&
        legalMoveRow.nonEmpty &&
        afterPieces.nonEmpty &&
        movingPieceBefore.nonEmpty &&
        movingPieceAfter.nonEmpty &&
        rivalKingAfter.nonEmpty &&
        rivalInCheckAfter.nonEmpty &&
        rivalLegalMoveCountAfter.nonEmpty
    val afterBoardRivalSideNotInCheck =
      exactAfterBoardReplay &&
        rivalInCheckAfter.contains(false)
    val afterBoardRivalSideHasNoLegalMoves =
      exactAfterBoardReplay &&
        rivalLegalMoveCountAfter.contains(0)
    val stalemateProducedByLegalMove =
      legalMoveRow.nonEmpty &&
        exactAfterBoardReplay &&
        afterBoardRivalSideNotInCheck &&
        afterBoardRivalSideHasNoLegalMoves

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(legalMoveRow.isEmpty)("legal move identity"),
      Option.when(movingPieceBefore.isEmpty)("moving piece identity"),
      Option.when(movingPieceBefore.map(_.square).isEmpty)("origin square"),
      Option.when(movingPieceAfter.map(_.square).isEmpty)("destination square"),
      Option.when(rivalKingAfter.map(_.square).isEmpty)("rival king square after move"),
      Option.when(!exactAfterBoardReplay)("exact after-board replay"),
      Option.when(!afterBoardRivalSideNotInCheck)("after-board rival side not in check"),
      Option.when(!afterBoardRivalSideHasNoLegalMoves)("after-board rival side has no legal moves"),
      Option.when(!stalemateProducedByLegalMove)("stalemate produced by legal move")
    ).flatten.distinct

    StalemateProof(
      stalematingSide = stalematingSide,
      rivalSide = rivalSide,
      movingPieceBefore = movingPieceBefore,
      movingPieceAfter = movingPieceAfter,
      originSquare = movingPieceBefore.map(_.square),
      destinationSquare = movingPieceAfter.map(_.square),
      rivalKingSquareAfter = rivalKingAfter.map(_.square),
      stalemateMove = Some(move),
      sameBoardProof = sameBoardProof,
      legalMove = legalMoveRow.nonEmpty,
      exactAfterBoardReplay = exactAfterBoardReplay,
      afterBoardRivalSideNotInCheck = afterBoardRivalSideNotInCheck,
      afterBoardRivalSideHasNoLegalMoves = afterBoardRivalSideHasNoLegalMoves,
      stalemateProducedByLegalMove = stalemateProducedByLegalMove,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("StalemateProof", missing))
    )
