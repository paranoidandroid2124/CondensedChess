package lila.commentary.chess

private[commentary] final case class LoosePieceProof(
    attackingSide: Side,
    rivalSide: Side,
    movingPieceBefore: Option[Piece],
    movingPieceAfter: Option[Piece],
    targetPieceAfter: Option[Piece],
    targetPieceSquareAfter: Option[Square],
    targetRivalOwned: Boolean,
    targetNonKing: Boolean,
    attackingPiecesAfter: Vector[Piece],
    attackingPieceSquareAfter: Option[Square],
    rivalLegalDefendersAfter: Vector[Piece],
    originSquare: Option[Square],
    destinationSquare: Option[Square],
    attackMove: Option[Line],
    sameBoardProof: Boolean,
    legalMove: Boolean,
    exactAfterBoardReplay: Boolean,
    afterBoardTargetAttackedByMovingSide: Boolean,
    rivalSideHasNoLegalDefenderOfTarget: Boolean,
    looseAttackProducedOrRevealedByLegalMove: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object LoosePieceProof:
  def fromBoardFacts(facts: BoardFacts, move: Line): LoosePieceProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMoveRow = facts.seen.legalMoves.find(_.line == move)
    val attackingSide = legalMoveRow.map(_.side).getOrElse(facts.sideToMove)
    val rivalSide = BoardFacts.opposite(attackingSide)
    val afterPieces = BoardFacts.piecesAfterLegalMove(facts, move)
    val movingPieceBefore = legalMoveRow.map(_.piece)
    val movingPieceAfter =
      afterPieces.flatMap(_.find(piece => piece.side == attackingSide && piece.square == move.to))
    val exactAfterBoardReplay =
      sameBoardProof &&
        legalMoveRow.nonEmpty &&
        afterPieces.nonEmpty &&
        movingPieceBefore.nonEmpty &&
        movingPieceAfter.nonEmpty
    val occupiedAfterMask =
      afterPieces.fold(0L)(pieces => pieces.foldLeft(0L)((mask, piece) => mask | piece.square.bit))
    val attackedTargetsAfter =
      if exactAfterBoardReplay then
        afterPieces.toVector.flatten
          .filter(_.side == rivalSide)
          .filter: target =>
            afterPieces.toVector.flatten.exists: attacker =>
              attacker.side == attackingSide &&
                BoardFacts.attacksSquare(attacker, target.square, occupiedAfterMask)
          .sortBy(piece => (if piece.man == Man.King then 1 else 0, piece.man.ordinal, piece.square.index))
      else Vector.empty
    val targetPieceAfter = attackedTargetsAfter.headOption
    val targetRivalOwned = targetPieceAfter.exists(_.side == rivalSide)
    val targetNonKing = targetPieceAfter.exists(_.man != Man.King)
    val legalAfterAttackLines =
      if exactAfterBoardReplay then
        targetPieceAfter.toVector.flatMap: target =>
          BoardFacts
            .sameSideLegalContinuationsAfter(facts, move)
            .map(_.line)
            .filter(_.to == target.square)
      else Vector.empty
    val attackingPiecesAfter =
      targetPieceAfter.fold(Vector.empty): target =>
        afterPieces.toVector.flatten
          .filter(_.side == attackingSide)
          .filter(piece => BoardFacts.attacksSquare(piece, target.square, occupiedAfterMask))
          .filter(piece => legalAfterAttackLines.exists(_.from == piece.square))
          .sortBy(piece => (piece.man.ordinal, piece.square.index))
    val rivalLegalDefendersAfter =
      targetPieceAfter.fold(Vector.empty): target =>
        if exactAfterBoardReplay && targetRivalOwned then
          afterPieces.toVector.flatten
            .filter(piece => piece.side == rivalSide && piece != target)
            .filter(piece => BoardFacts.attacksSquare(piece, target.square, occupiedAfterMask))
            .sortBy(piece => (piece.man.ordinal, piece.square.index))
        else Vector.empty
    val afterBoardTargetAttackedByMovingSide =
      targetPieceAfter.nonEmpty && attackingPiecesAfter.nonEmpty
    val rivalSideHasNoLegalDefenderOfTarget =
      targetPieceAfter.nonEmpty &&
        targetRivalOwned &&
        targetNonKing &&
        rivalLegalDefendersAfter.isEmpty
    val beforeBoardLooseAttackByMovingSide =
      targetPieceAfter.exists: target =>
        facts.pieces.exists(piece => piece.side == rivalSide && piece.man == target.man && piece.square == target.square) &&
          target.man != Man.King &&
          facts.seen.legalMoves.exists(move => move.side == attackingSide && move.line.to == target.square) &&
          !facts.seen.guards.exists(guard =>
            guard.target.side == rivalSide &&
              guard.target.man == target.man &&
              guard.target.square == target.square
          )
    val looseAttackProducedOrRevealedByLegalMove =
      legalMoveRow.nonEmpty &&
        exactAfterBoardReplay &&
        afterBoardTargetAttackedByMovingSide &&
        rivalSideHasNoLegalDefenderOfTarget &&
        !beforeBoardLooseAttackByMovingSide

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(legalMoveRow.isEmpty)("legal move identity"),
      Option.when(movingPieceBefore.isEmpty)("moving piece identity"),
      Option.when(movingPieceBefore.map(_.square).isEmpty)("origin square"),
      Option.when(movingPieceAfter.map(_.square).isEmpty)("destination square"),
      Option.when(!exactAfterBoardReplay)("exact after-board replay"),
      Option.when(targetPieceAfter.isEmpty)("target piece identity"),
      Option.when(targetPieceAfter.map(_.square).isEmpty)("target piece square after move"),
      Option.when(!targetRivalOwned)("target piece is rival-owned"),
      Option.when(!targetNonKing)("target piece is not king"),
      Option.when(attackingPiecesAfter.headOption.map(_.square).isEmpty)("attacking piece square after move"),
      Option.when(!afterBoardTargetAttackedByMovingSide)("after-board loose target attack"),
      Option.when(!rivalSideHasNoLegalDefenderOfTarget)("zero legal defenders of target square"),
      Option.when(!looseAttackProducedOrRevealedByLegalMove)(
        "loose attack produced or revealed by legal move"
      )
    ).flatten.distinct

    LoosePieceProof(
      attackingSide = attackingSide,
      rivalSide = rivalSide,
      movingPieceBefore = movingPieceBefore,
      movingPieceAfter = movingPieceAfter,
      targetPieceAfter = targetPieceAfter,
      targetPieceSquareAfter = targetPieceAfter.map(_.square),
      targetRivalOwned = targetRivalOwned,
      targetNonKing = targetNonKing,
      attackingPiecesAfter = attackingPiecesAfter,
      attackingPieceSquareAfter = attackingPiecesAfter.headOption.map(_.square),
      rivalLegalDefendersAfter = rivalLegalDefendersAfter,
      originSquare = movingPieceBefore.map(_.square),
      destinationSquare = movingPieceAfter.map(_.square),
      attackMove = Some(move),
      sameBoardProof = sameBoardProof,
      legalMove = legalMoveRow.nonEmpty,
      exactAfterBoardReplay = exactAfterBoardReplay,
      afterBoardTargetAttackedByMovingSide = afterBoardTargetAttackedByMovingSide,
      rivalSideHasNoLegalDefenderOfTarget = rivalSideHasNoLegalDefenderOfTarget,
      looseAttackProducedOrRevealedByLegalMove = looseAttackProducedOrRevealedByLegalMove,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("LoosePieceProof", missing))
    )
