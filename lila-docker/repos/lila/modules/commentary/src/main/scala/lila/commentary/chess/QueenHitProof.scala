package lila.commentary.chess

private[commentary] final case class QueenHitProof(
    attackingSide: Side,
    rivalSide: Side,
    movingPieceBefore: Option[Piece],
    movingPieceAfter: Option[Piece],
    attackingPiecesAfter: Vector[Piece],
    attackingPieceSquareAfter: Option[Square],
    originSquare: Option[Square],
    destinationSquare: Option[Square],
    rivalQueenSquareAfter: Option[Square],
    attackMove: Option[Line],
    sameBoardProof: Boolean,
    legalMove: Boolean,
    exactAfterBoardReplay: Boolean,
    rivalQueenExistsAfter: Boolean,
    afterBoardQueenAttackedByMovingSide: Boolean,
    queenHitProducedOrRevealedByLegalMove: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object QueenHitProof:
  def fromBoardFacts(facts: BoardFacts, move: Line): QueenHitProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMoveRow = facts.seen.legalMoves.find(_.line == move)
    val attackingSide = legalMoveRow.map(_.side).getOrElse(facts.sideToMove)
    val rivalSide = BoardFacts.opposite(attackingSide)
    val afterPieces = BoardFacts.piecesAfterLegalMove(facts, move)
    val movingPieceBefore = legalMoveRow.map(_.piece)
    val movingPieceAfter =
      afterPieces.flatMap(_.find(piece => piece.side == attackingSide && piece.square == move.to))
    val rivalQueenAfter =
      afterPieces.flatMap(_.find(piece => piece.side == rivalSide && piece.man == Man.Queen))
    val exactAfterBoardReplay =
      sameBoardProof &&
        legalMoveRow.nonEmpty &&
        afterPieces.nonEmpty &&
        movingPieceBefore.nonEmpty &&
        movingPieceAfter.nonEmpty
    val occupiedAfterMask =
      afterPieces.fold(0L)(pieces => pieces.foldLeft(0L)((mask, piece) => mask | piece.square.bit))
    val legalAfterQueenAttackLines =
      if exactAfterBoardReplay then
        rivalQueenAfter.toVector.flatMap: queen =>
          BoardFacts
            .sameSideLegalContinuationsAfter(facts, move)
            .map(_.line)
            .filter(_.to == queen.square)
      else Vector.empty
    val attackingPiecesAfter =
      if exactAfterBoardReplay then
        rivalQueenAfter.fold(Vector.empty): queen =>
          afterPieces.toVector.flatten
            .filter(piece => piece.side == attackingSide)
            .filter(piece => BoardFacts.attacksSquare(piece, queen.square, occupiedAfterMask))
            .filter(piece => legalAfterQueenAttackLines.exists(_.from == piece.square))
            .sortBy(piece => (piece.man.ordinal, piece.square.index))
      else Vector.empty
    val beforeBoardQueenAttackedByMovingSide =
      facts.pieces
        .find(piece => piece.side == rivalSide && piece.man == Man.Queen)
        .exists: queen =>
          facts.seen.legalMoves.exists(moveRow => moveRow.side == attackingSide && moveRow.line.to == queen.square)
    val rivalQueenExistsAfter = exactAfterBoardReplay && rivalQueenAfter.nonEmpty
    val afterBoardQueenAttackedByMovingSide =
      rivalQueenExistsAfter && attackingPiecesAfter.nonEmpty
    val queenHitProducedOrRevealedByLegalMove =
      legalMoveRow.nonEmpty &&
        exactAfterBoardReplay &&
        afterBoardQueenAttackedByMovingSide &&
        !beforeBoardQueenAttackedByMovingSide

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(legalMoveRow.isEmpty)("legal move identity"),
      Option.when(movingPieceBefore.isEmpty)("moving piece identity"),
      Option.when(movingPieceBefore.map(_.square).isEmpty)("origin square"),
      Option.when(movingPieceAfter.map(_.square).isEmpty)("destination square"),
      Option.when(!exactAfterBoardReplay)("exact after-board replay"),
      Option.when(!rivalQueenExistsAfter)("rival queen after move"),
      Option.when(!afterBoardQueenAttackedByMovingSide)("after-board queen attack"),
      Option.when(!afterBoardQueenAttackedByMovingSide)("legal after-board queen attack"),
      Option.when(beforeBoardQueenAttackedByMovingSide)("queen hit already existed before move"),
      Option.when(!queenHitProducedOrRevealedByLegalMove)("queen-hit produced or revealed by legal move")
    ).flatten.distinct

    QueenHitProof(
      attackingSide = attackingSide,
      rivalSide = rivalSide,
      movingPieceBefore = movingPieceBefore,
      movingPieceAfter = movingPieceAfter,
      attackingPiecesAfter = attackingPiecesAfter,
      attackingPieceSquareAfter = attackingPiecesAfter.headOption.map(_.square),
      originSquare = movingPieceBefore.map(_.square),
      destinationSquare = movingPieceAfter.map(_.square),
      rivalQueenSquareAfter = rivalQueenAfter.map(_.square),
      attackMove = Some(move),
      sameBoardProof = sameBoardProof,
      legalMove = legalMoveRow.nonEmpty,
      exactAfterBoardReplay = exactAfterBoardReplay,
      rivalQueenExistsAfter = rivalQueenExistsAfter,
      afterBoardQueenAttackedByMovingSide = afterBoardQueenAttackedByMovingSide,
      queenHitProducedOrRevealedByLegalMove = queenHitProducedOrRevealedByLegalMove,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("QueenHitProof", missing))
    )
