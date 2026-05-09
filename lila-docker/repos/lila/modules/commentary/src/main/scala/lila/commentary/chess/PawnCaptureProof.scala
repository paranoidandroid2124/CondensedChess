package lila.commentary.chess

private[commentary] final case class PawnCaptureProof(
    side: Side,
    rivalSide: Side,
    pawnBefore: Option[Piece],
    pawnAfter: Option[Piece],
    capturedPawn: Option[Piece],
    originSquare: Option[Square],
    destinationSquare: Option[Square],
    captureMove: Option[Line],
    sameBoardProof: Boolean,
    legalPawnMove: Boolean,
    legalPawnCapture: Boolean,
    nonPromotionMove: Boolean,
    ordinaryDiagonalPawnCapture: Boolean,
    pawnCapturesPawn: Boolean,
    exactAfterBoardReplay: Boolean,
    singleRivalPawnCaptured: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def captureSquare: Option[Square] = destinationSquare
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object PawnCaptureProof:
  def fromBoardFacts(facts: BoardFacts, move: Line): PawnCaptureProof =
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
    val capturedPawn =
      bySquare
        .get(move.to)
        .filter(piece => piece.man == Man.Pawn && piece.side == rivalSide)
    val pawnAfter = pawnBefore.map(_.copy(square = move.to))
    val legalPawnMove =
      legalMove.nonEmpty &&
        pawnBefore.nonEmpty
    val nonPromotionMove =
      legalPawnMove &&
        pawnAfter.exists(pawn => !onPromotionRank(pawn.side, pawn.square))
    val legalPawnCapture =
      legalPawnMove &&
        nonPromotionMove &&
        capturedPawn.nonEmpty &&
        move.from.file != move.to.file &&
        BoardFacts.sansFor(facts, move).exists(_.contains("x"))
    val ordinaryDiagonalPawnCapture =
      legalPawnCapture &&
        nonPromotionMove &&
        oneFileStep(move) &&
        oneForwardRankStep(side, move)
    val pawnCapturesPawn =
      ordinaryDiagonalPawnCapture
    val afterPieces =
      if ordinaryDiagonalPawnCapture then
        facts.pieces.filterNot(piece => piece.square == move.from || piece.square == move.to) ++ pawnAfter.toVector
      else facts.pieces
    val exactAfterBoardReplay =
      sameBoardProof &&
        ordinaryDiagonalPawnCapture &&
        pawnAfter.exists(afterPawn => afterPieces.exists(_ == afterPawn)) &&
        !afterPieces.exists(_.square == move.from) &&
        !capturedPawn.exists(captured => afterPieces.exists(piece => piece.side == captured.side && piece.square == captured.square))
    val singleRivalPawnCaptured =
      ordinaryDiagonalPawnCapture &&
        capturedPawn.nonEmpty

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(pawnBefore.isEmpty)("pawn identity"),
      Option.when(pawnBefore.map(_.square).isEmpty)("origin square"),
      Option.when(pawnAfter.map(_.square).isEmpty)("capture square"),
      Option.when(!legalPawnMove)("legal pawn move"),
      Option.when(!legalPawnCapture)("legal pawn capture"),
      Option.when(!nonPromotionMove)("move is non-promotion"),
      Option.when(capturedPawn.isEmpty)("captured rival pawn"),
      Option.when(!ordinaryDiagonalPawnCapture)("ordinary diagonal pawn capture"),
      Option.when(!pawnCapturesPawn)("pawn captures pawn"),
      Option.when(!exactAfterBoardReplay)("exact-board replay"),
      Option.when(!singleRivalPawnCaptured)("exactly one rival pawn captured")
    ).flatten.distinct

    PawnCaptureProof(
      side = side,
      rivalSide = rivalSide,
      pawnBefore = pawnBefore,
      pawnAfter = pawnAfter,
      capturedPawn = capturedPawn,
      originSquare = pawnBefore.map(_.square),
      destinationSquare = pawnAfter.map(_.square),
      captureMove = Some(move),
      sameBoardProof = sameBoardProof,
      legalPawnMove = legalPawnMove,
      legalPawnCapture = legalPawnCapture,
      nonPromotionMove = nonPromotionMove,
      ordinaryDiagonalPawnCapture = ordinaryDiagonalPawnCapture,
      pawnCapturesPawn = pawnCapturesPawn,
      exactAfterBoardReplay = exactAfterBoardReplay,
      singleRivalPawnCaptured = singleRivalPawnCaptured,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("PawnCaptureProof", missing))
    )

  private def onPromotionRank(side: Side, square: Square): Boolean =
    (side == Side.White && square.rank == 7) ||
      (side == Side.Black && square.rank == 0)

  private def oneFileStep(move: Line): Boolean =
    math.abs(move.from.file - move.to.file) == 1

  private def oneForwardRankStep(side: Side, move: Line): Boolean =
    val rankDelta = move.to.rank - move.from.rank
    (side == Side.White && rankDelta == 1) ||
      (side == Side.Black && rankDelta == -1)
