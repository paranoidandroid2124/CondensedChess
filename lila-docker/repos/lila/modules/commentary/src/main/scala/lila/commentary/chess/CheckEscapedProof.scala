package lila.commentary.chess

private[commentary] final case class CheckEscapedProof(
    escapingSide: Side,
    rivalSide: Side,
    movingPieceBefore: Option[Piece],
    movingPieceAfter: Option[Piece],
    originSquare: Option[Square],
    destinationSquare: Option[Square],
    beforeKingSquare: Option[Square],
    afterKingSquare: Option[Square],
    escapeMove: Option[Line],
    sameBoardProof: Boolean,
    legalMove: Boolean,
    exactBeforeBoardState: Boolean,
    exactAfterBoardReplay: Boolean,
    beforeBoardSideKingInCheck: Boolean,
    afterBoardSideKingNotInCheck: Boolean,
    checkEscapedByLegalMove: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object CheckEscapedProof:
  def fromBoardFacts(facts: BoardFacts, move: Line): CheckEscapedProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val escapingSide = facts.sideToMove
    val rivalSide = BoardFacts.opposite(escapingSide)
    val legalMoveRow = facts.seen.legalMoves.find(row => row.side == escapingSide && row.line == move)
    val beforeKing = facts.seen.kingSquares.find(_.side == escapingSide).map(_.king)
    val afterPieces = BoardFacts.piecesAfterLegalMove(facts, move)
    val movingPieceBefore = legalMoveRow.map(_.piece)
    val movingPieceAfter =
      afterPieces.flatMap(_.find(piece => piece.side == escapingSide && piece.square == move.to))
    val originSquare = movingPieceBefore.map(_.square)
    val destinationSquare = movingPieceAfter.map(_.square)
    val afterKing =
      afterPieces.flatMap(_.find(piece => piece.side == escapingSide && piece.man == Man.King))
    val afterSideInCheck = BoardFacts.sideInCheckAfterLegalMove(facts, move, escapingSide)
    val exactBeforeBoardState =
      sameBoardProof &&
        legalMoveRow.nonEmpty &&
        movingPieceBefore.nonEmpty &&
        originSquare.contains(move.from) &&
        beforeKing.nonEmpty
    val beforeBoardSideKingInCheck =
      exactBeforeBoardState && BoardFacts.sideInCheck(facts, escapingSide)
    val exactAfterBoardReplay =
      sameBoardProof &&
        legalMoveRow.nonEmpty &&
        afterPieces.nonEmpty &&
        movingPieceBefore.nonEmpty &&
        movingPieceAfter.nonEmpty &&
        beforeKing.nonEmpty &&
        afterKing.nonEmpty &&
        afterSideInCheck.nonEmpty
    val afterBoardSideKingNotInCheck =
      exactAfterBoardReplay && afterSideInCheck.contains(false)
    val checkEscapedByLegalMove =
      legalMoveRow.nonEmpty &&
        exactAfterBoardReplay &&
        beforeBoardSideKingInCheck &&
        afterBoardSideKingNotInCheck

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(legalMoveRow.isEmpty)("legal move identity"),
      Option.when(movingPieceBefore.isEmpty)("moving piece identity"),
      Option.when(originSquare.isEmpty)("origin square"),
      Option.when(destinationSquare.isEmpty)("destination square"),
      Option.when(!exactBeforeBoardState)("exact before-board state"),
      Option.when(beforeKing.map(_.square).isEmpty)("before king square"),
      Option.when(afterKing.map(_.square).isEmpty)("after king square"),
      Option.when(!beforeBoardSideKingInCheck)("before-board side king in check"),
      Option.when(!exactAfterBoardReplay)("exact after-board replay"),
      Option.when(!afterBoardSideKingNotInCheck)("after-board side king not in check"),
      Option.when(!checkEscapedByLegalMove)("check escaped by legal move")
    ).flatten.distinct

    CheckEscapedProof(
      escapingSide = escapingSide,
      rivalSide = rivalSide,
      movingPieceBefore = movingPieceBefore,
      movingPieceAfter = movingPieceAfter,
      originSquare = originSquare,
      destinationSquare = destinationSquare,
      beforeKingSquare = beforeKing.map(_.square),
      afterKingSquare = afterKing.map(_.square),
      escapeMove = Some(move),
      sameBoardProof = sameBoardProof,
      legalMove = legalMoveRow.nonEmpty,
      exactBeforeBoardState = exactBeforeBoardState,
      exactAfterBoardReplay = exactAfterBoardReplay,
      beforeBoardSideKingInCheck = beforeBoardSideKingInCheck,
      afterBoardSideKingNotInCheck = afterBoardSideKingNotInCheck,
      checkEscapedByLegalMove = checkEscapedByLegalMove,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("CheckEscapedProof", missing))
    )
