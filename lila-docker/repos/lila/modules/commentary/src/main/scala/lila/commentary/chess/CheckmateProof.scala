package lila.commentary.chess

private[commentary] final case class CheckmateProof(
    matingSide: Side,
    rivalSide: Side,
    movingPieceBefore: Option[Piece],
    movingPieceAfter: Option[Piece],
    originSquare: Option[Square],
    destinationSquare: Option[Square],
    rivalKingSquareAfter: Option[Square],
    mateMove: Option[Line],
    sameBoardProof: Boolean,
    legalMove: Boolean,
    exactAfterBoardReplay: Boolean,
    afterBoardRivalKingInCheck: Boolean,
    afterBoardRivalSideHasNoLegalEscape: Boolean,
    checkmateProducedByLegalMove: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object CheckmateProof:
  def fromBoardFacts(facts: BoardFacts, move: Line): CheckmateProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMoveRow = facts.seen.legalMoves.find(_.line == move)
    val checkMoveRow = facts.seen.legalCheckMoves.find(_.line == move)
    val matingSide = legalMoveRow.map(_.side).orElse(checkMoveRow.map(_.side)).getOrElse(facts.sideToMove)
    val rivalSide = BoardFacts.opposite(matingSide)
    val afterPieces = BoardFacts.piecesAfterLegalMove(facts, move)
    val movingPieceBefore = legalMoveRow.map(_.piece)
    val movingPieceAfter =
      afterPieces.flatMap(_.find(piece => piece.side == matingSide && piece.square == move.to))
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
    val afterBoardRivalKingInCheck =
      exactAfterBoardReplay &&
        rivalInCheckAfter.contains(true) &&
        checkMoveRow.exists(row =>
          row.side == matingSide &&
            row.rivalSide == rivalSide &&
            rivalKingAfter.exists(king => king.square == row.rivalKingSquareAfter)
        )
    val afterBoardRivalSideHasNoLegalEscape =
      exactAfterBoardReplay &&
        afterBoardRivalKingInCheck &&
        rivalLegalMoveCountAfter.contains(0)
    val checkmateProducedByLegalMove =
      legalMoveRow.nonEmpty &&
        exactAfterBoardReplay &&
        afterBoardRivalKingInCheck &&
        afterBoardRivalSideHasNoLegalEscape

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(legalMoveRow.isEmpty)("legal move identity"),
      Option.when(movingPieceBefore.isEmpty)("moving piece identity"),
      Option.when(movingPieceBefore.map(_.square).isEmpty)("origin square"),
      Option.when(movingPieceAfter.map(_.square).isEmpty)("destination square"),
      Option.when(rivalKingAfter.map(_.square).isEmpty)("rival king square after move"),
      Option.when(!exactAfterBoardReplay)("exact after-board replay"),
      Option.when(!afterBoardRivalKingInCheck)("after-board rival king in check"),
      Option.when(!afterBoardRivalSideHasNoLegalEscape)("after-board rival side has no legal escape"),
      Option.when(!checkmateProducedByLegalMove)("checkmate produced by legal move")
    ).flatten.distinct

    CheckmateProof(
      matingSide = matingSide,
      rivalSide = rivalSide,
      movingPieceBefore = movingPieceBefore,
      movingPieceAfter = movingPieceAfter,
      originSquare = movingPieceBefore.map(_.square),
      destinationSquare = movingPieceAfter.map(_.square),
      rivalKingSquareAfter = rivalKingAfter.map(_.square),
      mateMove = Some(move),
      sameBoardProof = sameBoardProof,
      legalMove = legalMoveRow.nonEmpty,
      exactAfterBoardReplay = exactAfterBoardReplay,
      afterBoardRivalKingInCheck = afterBoardRivalKingInCheck,
      afterBoardRivalSideHasNoLegalEscape = afterBoardRivalSideHasNoLegalEscape,
      checkmateProducedByLegalMove = checkmateProducedByLegalMove,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("CheckmateProof", missing))
    )
