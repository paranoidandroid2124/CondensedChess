package lila.commentary.chess

private[commentary] final case class CheckGivenProof(
    checkingSide: Side,
    rivalSide: Side,
    movingPieceBefore: Option[Piece],
    movingPieceAfter: Option[Piece],
    originSquare: Option[Square],
    destinationSquare: Option[Square],
    rivalKingSquareAfter: Option[Square],
    checkMove: Option[Line],
    sameBoardProof: Boolean,
    legalMove: Boolean,
    exactAfterBoardReplay: Boolean,
    afterBoardRivalKingInCheck: Boolean,
    checkProducedByLegalMove: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object CheckGivenProof:
  def fromBoardFacts(facts: BoardFacts, move: Line): CheckGivenProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMoveRow = facts.seen.legalMoves.find(_.line == move)
    val checkMoveRow = facts.seen.legalCheckMoves.find(_.line == move)
    val checkingSide = legalMoveRow.map(_.side).orElse(checkMoveRow.map(_.side)).getOrElse(facts.sideToMove)
    val rivalSide = BoardFacts.opposite(checkingSide)
    val afterPieces = BoardFacts.piecesAfterLegalMove(facts, move)
    val movingPieceBefore = legalMoveRow.map(_.piece)
    val movingPieceAfter =
      afterPieces.flatMap(_.find(piece => piece.side == checkingSide && piece.square == move.to))
    val rivalKingAfter =
      afterPieces.flatMap(_.find(piece => piece.side == rivalSide && piece.man == Man.King))
    val exactAfterBoardReplay =
      sameBoardProof &&
        legalMoveRow.nonEmpty &&
        afterPieces.nonEmpty &&
        movingPieceBefore.nonEmpty &&
        movingPieceAfter.nonEmpty &&
        rivalKingAfter.nonEmpty
    val afterBoardRivalKingInCheck =
      exactAfterBoardReplay &&
        checkMoveRow.exists(row =>
          row.side == checkingSide &&
            row.rivalSide == rivalSide &&
            rivalKingAfter.exists(king => king.square == row.rivalKingSquareAfter)
        )
    val checkProducedByLegalMove =
      legalMoveRow.nonEmpty &&
        exactAfterBoardReplay &&
        afterBoardRivalKingInCheck

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(legalMoveRow.isEmpty)("legal move identity"),
      Option.when(movingPieceBefore.isEmpty)("moving piece identity"),
      Option.when(movingPieceBefore.map(_.square).isEmpty)("origin square"),
      Option.when(movingPieceAfter.map(_.square).isEmpty)("destination square"),
      Option.when(rivalKingAfter.map(_.square).isEmpty)("rival king square after move"),
      Option.when(!exactAfterBoardReplay)("exact after-board replay"),
      Option.when(!afterBoardRivalKingInCheck)("after-board rival king in check"),
      Option.when(!checkProducedByLegalMove)("check produced by legal move")
    ).flatten.distinct

    CheckGivenProof(
      checkingSide = checkingSide,
      rivalSide = rivalSide,
      movingPieceBefore = movingPieceBefore,
      movingPieceAfter = movingPieceAfter,
      originSquare = movingPieceBefore.map(_.square),
      destinationSquare = movingPieceAfter.map(_.square),
      rivalKingSquareAfter = rivalKingAfter.map(_.square),
      checkMove = Some(move),
      sameBoardProof = sameBoardProof,
      legalMove = legalMoveRow.nonEmpty,
      exactAfterBoardReplay = exactAfterBoardReplay,
      afterBoardRivalKingInCheck = afterBoardRivalKingInCheck,
      checkProducedByLegalMove = checkProducedByLegalMove,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("CheckGivenProof", missing))
    )
