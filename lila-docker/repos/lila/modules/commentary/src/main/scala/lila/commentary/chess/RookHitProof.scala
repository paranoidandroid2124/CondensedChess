package lila.commentary.chess

private[commentary] final case class RookHitProof(
    attackingSide: Side,
    rivalSide: Side,
    movingPieceBefore: Option[Piece],
    movingPieceAfter: Option[Piece],
    targetPieceAfter: Option[Piece],
    attackingPiecesAfter: Vector[Piece],
    attackingPieceSquareAfter: Option[Square],
    originSquare: Option[Square],
    destinationSquare: Option[Square],
    targetSquare: Option[Square],
    attackMove: Option[Line],
    sameBoardProof: Boolean,
    legalMove: Boolean,
    completeStoryProof: Boolean,
    exactAfterBoardReplay: Boolean,
    targetSquareBound: Boolean,
    targetPieceExistsAfter: Boolean,
    targetPieceIsRivalRook: Boolean,
    targetAttackedBeforeMove: Boolean,
    afterBoardRookAttackedByMovingSide: Boolean,
    routeAndTargetBindSameBoard: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object RookHitProof:
  def fromBoardFacts(facts: BoardFacts, move: Line, targetSquare: Option[Square]): RookHitProof =
    fromBoardFacts(
      facts = facts,
      move = move,
      targetSquare = targetSquare,
      storyProof = StoryProof.fromBoardFacts(facts, move)
    )

  def fromBoardFacts(
      facts: BoardFacts,
      move: Line,
      targetSquare: Option[Square],
      storyProof: StoryProof
  ): RookHitProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMoveRow = facts.seen.legalMoves.find(_.line == move)
    val attackingSide = legalMoveRow.map(_.side).getOrElse(facts.sideToMove)
    val rivalSide = BoardFacts.opposite(attackingSide)
    val afterPieces = BoardFacts.piecesAfterLegalMove(facts, move)
    val movingPieceBefore = legalMoveRow.map(_.piece)
    val movingPieceAfter =
      afterPieces.flatMap(_.find(piece => piece.side == attackingSide && piece.square == move.to))
    val targetPieceAfter =
      for
        pieces <- afterPieces
        square <- targetSquare
        piece <- pieces.find(_.square == square)
      yield piece

    val completeStoryProof =
      storyProof.completeFor(
        side = attackingSide,
        rival = rivalSide,
        target = targetSquare,
        anchor = movingPieceBefore.map(_.square),
        route = Some(move)
      )
    val exactAfterBoardReplay =
      sameBoardProof &&
        legalMoveRow.nonEmpty &&
        afterPieces.nonEmpty &&
        movingPieceBefore.nonEmpty &&
        movingPieceAfter.nonEmpty
    val targetSquareBound = targetSquare.nonEmpty
    val targetPieceExistsAfter = exactAfterBoardReplay && targetPieceAfter.nonEmpty
    val targetPieceIsRivalRook =
      targetPieceAfter.exists(piece => piece.side == rivalSide && piece.man == Man.Rook)
    val occupiedAfterMask =
      afterPieces.fold(0L)(pieces => pieces.foldLeft(0L)((mask, piece) => mask | piece.square.bit))
    val legalAfterRookAttackLines =
      if exactAfterBoardReplay && targetSquareBound && targetPieceIsRivalRook then
        targetSquare.toVector.flatMap: square =>
          BoardFacts
            .sameSideLegalContinuationsAfter(facts, move)
            .map(_.line)
            .filter(_.to == square)
      else Vector.empty
    val attackingPiecesAfter =
      if exactAfterBoardReplay && targetSquareBound && targetPieceIsRivalRook then
        targetSquare.fold(Vector.empty): square =>
          afterPieces.toVector.flatten
            .filter(piece => piece.side == attackingSide)
            .filter(piece => BoardFacts.attacksSquare(piece, square, occupiedAfterMask))
            .filter(piece => legalAfterRookAttackLines.exists(_.from == piece.square))
            .sortBy(piece => (piece.man.ordinal, piece.square.index))
      else Vector.empty
    val targetAttackedBeforeMove =
      targetSquare.exists: square =>
        facts.seen.legalMoves.exists(moveRow => moveRow.side == attackingSide && moveRow.line.to == square)
    val afterBoardRookAttackedByMovingSide = targetPieceIsRivalRook && attackingPiecesAfter.nonEmpty
    val routeAndTargetBindSameBoard =
      sameBoardProof &&
        legalMoveRow.nonEmpty &&
        completeStoryProof &&
        exactAfterBoardReplay &&
        targetSquareBound &&
        targetPieceAfter.exists(piece => targetSquare.contains(piece.square)) &&
        attackMoveBindsRoute(move, Some(move))

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(legalMoveRow.isEmpty)("legal move identity"),
      Option.when(!completeStoryProof)("complete StoryProof"),
      Option.when(movingPieceBefore.isEmpty)("moving piece identity"),
      Option.when(movingPieceBefore.map(_.square).isEmpty)("origin square"),
      Option.when(movingPieceAfter.map(_.square).isEmpty)("destination square"),
      Option.when(!exactAfterBoardReplay)("exact after-board replay"),
      Option.when(!targetSquareBound)("target square bound"),
      Option.when(!targetPieceExistsAfter)("target piece on after-board"),
      Option.when(!targetPieceIsRivalRook)("target piece is rival rook"),
      Option.when(!afterBoardRookAttackedByMovingSide)("after-board rook attack"),
      Option.when(!afterBoardRookAttackedByMovingSide)("legal after-board rook attack"),
      Option.when(!routeAndTargetBindSameBoard)("route and target bind same move and same board")
    ).flatten.distinct

    RookHitProof(
      attackingSide = attackingSide,
      rivalSide = rivalSide,
      movingPieceBefore = movingPieceBefore,
      movingPieceAfter = movingPieceAfter,
      targetPieceAfter = targetPieceAfter,
      attackingPiecesAfter = attackingPiecesAfter,
      attackingPieceSquareAfter = attackingPiecesAfter.headOption.map(_.square),
      originSquare = movingPieceBefore.map(_.square),
      destinationSquare = movingPieceAfter.map(_.square),
      targetSquare = targetSquare,
      attackMove = Some(move),
      sameBoardProof = sameBoardProof,
      legalMove = legalMoveRow.nonEmpty,
      completeStoryProof = completeStoryProof,
      exactAfterBoardReplay = exactAfterBoardReplay,
      targetSquareBound = targetSquareBound,
      targetPieceExistsAfter = targetPieceExistsAfter,
      targetPieceIsRivalRook = targetPieceIsRivalRook,
      targetAttackedBeforeMove = targetAttackedBeforeMove,
      afterBoardRookAttackedByMovingSide = afterBoardRookAttackedByMovingSide,
      routeAndTargetBindSameBoard = routeAndTargetBindSameBoard,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("RookHitProof", missing))
    )

  private def attackMoveBindsRoute(move: Line, route: Option[Line]): Boolean =
    route.contains(move)
