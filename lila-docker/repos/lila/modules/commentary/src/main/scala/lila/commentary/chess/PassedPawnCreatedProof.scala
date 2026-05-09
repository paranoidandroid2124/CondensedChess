package lila.commentary.chess

private[commentary] final case class PassedPawnCreatedProof(
    side: Side,
    rivalSide: Side,
    createdPassedPawn: Option[Piece],
    pawnBefore: Option[Piece],
    pawnAfter: Option[Piece],
    originSquare: Option[Square],
    afterSquare: Option[Square],
    fromSquare: Option[Square],
    toSquare: Option[Square],
    creatingMove: Option[Line],
    exactBeforeBoard: Boolean,
    sameBoardProof: Boolean,
    legalPawnMove: Boolean,
    ordinaryPawnMoveOrCapture: Boolean,
    nonPromotionMove: Boolean,
    passedBefore: Boolean,
    exactAfterBoardReplay: Boolean,
    passedAfter: Boolean,
    exactlyOneNewPassedPawn: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object PassedPawnCreatedProof:
  def fromBoardFacts(facts: BoardFacts, move: Line): PassedPawnCreatedProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMove = facts.seen.legalMoves.find(_.line == move)
    val side = legalMove.map(_.side).getOrElse(facts.sideToMove)
    val rivalSide = BoardFacts.opposite(side)
    val bySquare = facts.pieces.map(piece => piece.square -> piece).toMap
    val pawnBefore =
      bySquare
        .get(move.from)
        .filter(piece => piece.man == Man.Pawn && piece.side == side)
        .filter(piece => legalMove.exists(_.piece == piece))
    val pawnAfter = pawnBefore.map(_.copy(square = move.to))
    val legalPawnMove =
      legalMove.nonEmpty &&
        pawnBefore.nonEmpty
    val exactBeforeBoard =
      sameBoardProof &&
        legalPawnMove &&
        pawnBefore.exists(pawn => facts.pieces.contains(pawn))
    val nonPromotionMove =
      legalPawnMove &&
        pawnAfter.exists(pawn => !onPromotionRank(pawn.side, pawn.square))
    val destinationPiece = bySquare.get(move.to)
    val ordinaryPawnMove =
      legalPawnMove &&
        move.from.file == move.to.file &&
        destinationPiece.isEmpty
    val ordinaryPawnCapture =
      legalPawnMove &&
        math.abs(move.from.file - move.to.file) == 1 &&
        destinationPiece.exists(_.side == rivalSide)
    val ordinaryPawnMoveOrCapture = ordinaryPawnMove || ordinaryPawnCapture
    val afterPieces =
      if legalPawnMove && nonPromotionMove && ordinaryPawnMoveOrCapture then
        facts.pieces.filterNot(piece =>
          piece.square == move.from || piece.square == move.to
        ) ++ pawnAfter.toVector
      else facts.pieces
    val passedBefore =
      pawnBefore.exists(pawn => isPassedPawn(pawn, facts.pieces))
    val exactAfterBoardReplay =
      sameBoardProof &&
        legalPawnMove &&
        nonPromotionMove &&
        pawnAfter.exists(afterPawn => afterPieces.exists(_ == afterPawn)) &&
        !afterPieces.exists(_.square == move.from)
    val passedAfter =
      exactAfterBoardReplay &&
        pawnAfter.exists(pawn => isPassedPawn(pawn, afterPieces))
    val newlyPassedPawns =
      if exactAfterBoardReplay then
        afterPieces
          .filter(piece => piece.man == Man.Pawn && isPassedPawn(piece, afterPieces))
          .filter: afterPawn =>
            if pawnAfter.contains(afterPawn) then !passedBefore
            else
              !facts.pieces.exists(beforePawn =>
                beforePawn == afterPawn && beforePawn.man == Man.Pawn && isPassedPawn(
                  beforePawn,
                  facts.pieces
                )
              )
      else Vector.empty
    val exactlyOneNewPassedPawn =
      newlyPassedPawns == pawnAfter.toVector && !passedBefore && passedAfter
    val createdPassedPawn =
      Option.when(exactlyOneNewPassedPawn)(pawnAfter).flatten

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(!exactBeforeBoard)("exact before-board"),
      Option.when(pawnBefore.isEmpty)("pawn identity"),
      Option.when(pawnBefore.map(_.square).isEmpty)("from square"),
      Option.when(pawnAfter.map(_.square).isEmpty)("to square"),
      Option.when(!legalPawnMove)("legal pawn move"),
      Option.when(!ordinaryPawnMoveOrCapture)("ordinary pawn move or capture"),
      Option.when(!nonPromotionMove)("move is non-promotion"),
      Option.when(passedBefore)("not passed before move"),
      Option.when(!exactAfterBoardReplay)("exact after-board replay"),
      Option.when(!passedAfter)("passed pawn on exact after-board"),
      Option.when(!exactlyOneNewPassedPawn)("exactly one newly passed pawn")
    ).flatten.distinct

    PassedPawnCreatedProof(
      side = side,
      rivalSide = rivalSide,
      createdPassedPawn = createdPassedPawn,
      pawnBefore = pawnBefore,
      pawnAfter = pawnAfter,
      originSquare = pawnBefore.map(_.square),
      afterSquare = pawnAfter.map(_.square),
      fromSquare = pawnBefore.map(_.square),
      toSquare = pawnAfter.map(_.square),
      creatingMove = Some(move),
      exactBeforeBoard = exactBeforeBoard,
      sameBoardProof = sameBoardProof,
      legalPawnMove = legalPawnMove,
      ordinaryPawnMoveOrCapture = ordinaryPawnMoveOrCapture,
      nonPromotionMove = nonPromotionMove,
      passedBefore = passedBefore,
      exactAfterBoardReplay = exactAfterBoardReplay,
      passedAfter = passedAfter,
      exactlyOneNewPassedPawn = exactlyOneNewPassedPawn,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("PassedPawnCreatedProof", missing))
    )

  private def onPromotionRank(side: Side, square: Square): Boolean =
    (side == Side.White && square.rank == 7) ||
      (side == Side.Black && square.rank == 0)

  private def isPassedPawn(pawn: Piece, pieces: Vector[Piece]): Boolean =
    pieces
      .filter(piece => piece.side == BoardFacts.opposite(pawn.side) && piece.man == Man.Pawn)
      .forall: enemy =>
        math.abs(enemy.square.file - pawn.square.file) > 1 || !isAheadOf(pawn.side, pawn.square, enemy.square)

  private def isAheadOf(side: Side, from: Square, to: Square): Boolean =
    if side == Side.White then to.rank > from.rank else to.rank < from.rank
