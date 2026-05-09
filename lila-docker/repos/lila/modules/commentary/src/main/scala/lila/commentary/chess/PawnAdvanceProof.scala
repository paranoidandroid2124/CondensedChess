package lila.commentary.chess

private[commentary] final case class PawnAdvanceProof(
    side: Side,
    rivalSide: Side,
    pawnBefore: Option[Piece],
    pawnAfter: Option[Piece],
    fromSquare: Option[Square],
    toSquare: Option[Square],
    advanceMove: Option[Line],
    sameBoardProof: Boolean,
    legalPawnAdvance: Boolean,
    nonCapture: Boolean,
    nonPromotion: Boolean,
    legalOneStepNonCaptureNonPromotion: Boolean,
    alreadyPassedBefore: Boolean,
    exactAfterBoardReplay: Boolean,
    afterBoardPassedPawn: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object PawnAdvanceProof:
  def fromBoardFacts(facts: BoardFacts, advanceMove: Line): PawnAdvanceProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMove = facts.seen.legalMoves.find(_.line == advanceMove)
    val side = legalMove.map(_.side).getOrElse(facts.sideToMove)
    val rivalSide = BoardFacts.opposite(side)
    val bySquare = facts.pieces.map(piece => piece.square -> piece).toMap
    val pawnBefore =
      bySquare
        .get(advanceMove.from)
        .filter(piece => piece.man == Man.Pawn && piece.side == side)
        .filter(piece => legalMove.exists(_.piece == piece))
    val pawnAfter = pawnBefore.map(_.copy(square = advanceMove.to))
    val destinationEmpty = !bySquare.contains(advanceMove.to)
    val oneStepForward =
      pawnBefore.exists: pawn =>
        advanceMove.from.file == advanceMove.to.file &&
          rankDelta(pawn.side, advanceMove.from, advanceMove.to) == 1
    val legalPawnAdvance =
      legalMove.nonEmpty &&
        pawnBefore.nonEmpty &&
        oneStepForward
    val nonCapture =
      legalMove.nonEmpty &&
        pawnBefore.nonEmpty &&
        advanceMove.from.file == advanceMove.to.file &&
        destinationEmpty
    val nonPromotion =
      legalMove.nonEmpty &&
      pawnAfter.exists: pawn =>
        (pawn.side == Side.White && pawn.square.rank < 7) ||
          (pawn.side == Side.Black && pawn.square.rank > 0)
    val legalOneStepNonCaptureNonPromotion =
      legalPawnAdvance &&
        nonCapture &&
        nonPromotion
    val alreadyPassedBefore =
      pawnBefore.exists: pawn =>
        facts.seen.passedPawnObservations.exists(row => row.side == pawn.side && row.pawn == pawn)
    val afterPieces =
      if legalOneStepNonCaptureNonPromotion then
        facts.pieces.filterNot(_.square == advanceMove.from) ++ pawnAfter.toVector
      else facts.pieces
    val exactAfterBoardReplay =
      sameBoardProof &&
        legalOneStepNonCaptureNonPromotion &&
        pawnAfter.exists(afterPawn => afterPieces.exists(_ == afterPawn)) &&
        !afterPieces.exists(_.square == advanceMove.from)
    val afterBoardPassedPawn =
      exactAfterBoardReplay &&
        pawnAfter.exists(pawn => isPassedPawn(pawn, afterPieces))

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(pawnBefore.isEmpty)("pawn identity"),
      Option.when(pawnBefore.map(_.square).isEmpty)("from square"),
      Option.when(pawnAfter.map(_.square).isEmpty)("to square"),
      Option.when(!legalPawnAdvance)("legal pawn advance"),
      Option.when(!nonCapture)("move is non-capture"),
      Option.when(!nonPromotion)("move is non-promotion"),
      Option.when(!alreadyPassedBefore)("already passed pawn before move"),
      Option.when(!exactAfterBoardReplay)("exact after-board replay"),
      Option.when(!afterBoardPassedPawn)("passed pawn on exact after-board")
    ).flatten.distinct

    PawnAdvanceProof(
      side = side,
      rivalSide = rivalSide,
      pawnBefore = pawnBefore,
      pawnAfter = pawnAfter,
      fromSquare = pawnBefore.map(_.square),
      toSquare = pawnAfter.map(_.square),
      advanceMove = Some(advanceMove),
      sameBoardProof = sameBoardProof,
      legalPawnAdvance = legalPawnAdvance,
      nonCapture = nonCapture,
      nonPromotion = nonPromotion,
      legalOneStepNonCaptureNonPromotion = legalOneStepNonCaptureNonPromotion,
      alreadyPassedBefore = alreadyPassedBefore,
      exactAfterBoardReplay = exactAfterBoardReplay,
      afterBoardPassedPawn = afterBoardPassedPawn,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("PawnAdvanceProof", missing))
    )

  private def rankDelta(side: Side, from: Square, to: Square): Int =
    if side == Side.White then to.rank - from.rank
    else if side == Side.Black then from.rank - to.rank
    else 0

  private def isPassedPawn(pawn: Piece, pieces: Vector[Piece]): Boolean =
    pieces
      .filter(piece => piece.side == BoardFacts.opposite(pawn.side) && piece.man == Man.Pawn)
      .forall: enemy =>
        math.abs(enemy.square.file - pawn.square.file) > 1 || !isAheadOf(pawn.side, pawn.square, enemy.square)

  private def isAheadOf(side: Side, from: Square, to: Square): Boolean =
    if side == Side.White then to.rank > from.rank else to.rank < from.rank
