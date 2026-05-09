package lila.commentary.chess

private[commentary] enum PawnBreakContactKind:
  case PawnChallengesPawn, PawnLeverCreated

private[commentary] final case class PawnBreakProof(
    side: Side,
    rivalSide: Side,
    pawnBefore: Option[Piece],
    pawnAfter: Option[Piece],
    targetPawn: Option[Piece],
    originSquare: Option[Square],
    destinationSquare: Option[Square],
    breakMove: Option[Line],
    sameBoardProof: Boolean,
    legalPawnMove: Boolean,
    nonPromotionMove: Boolean,
    nonCapturingMove: Boolean,
    exactAfterBoardReplay: Boolean,
    directPawnLeverAfterMove: Boolean,
    leverCreatedByMove: Boolean,
    singleRivalPawnTarget: Boolean,
    contactKinds: Vector[PawnBreakContactKind],
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def fromSquare: Option[Square] = originSquare
  def toSquare: Option[Square] = destinationSquare
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object PawnBreakProof:
  def fromBoardFacts(facts: BoardFacts, breakMove: Line): PawnBreakProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMove = facts.seen.legalMoves.find(_.line == breakMove)
    val side = legalMove.map(_.side).getOrElse(facts.sideToMove)
    val rivalSide = BoardFacts.opposite(side)
    val bySquare = facts.pieces.map(piece => piece.square -> piece).toMap
    val pawnBefore =
      bySquare
        .get(breakMove.from)
        .filter(piece => piece.man == Man.Pawn && piece.side == side)
        .filter(piece => legalMove.exists(_.piece == piece))
    val pawnAfter = pawnBefore.map(_.copy(square = breakMove.to))
    val destinationEmptyBefore = !bySquare.contains(breakMove.to)
    val legalPawnMove =
      legalMove.nonEmpty &&
        pawnBefore.nonEmpty
    val nonPromotionMove =
      legalPawnMove &&
        pawnAfter.exists(pawn => !onPromotionRank(pawn.side, pawn.square))
    val nonCapturingMove =
      legalPawnMove &&
        nonPromotionMove &&
        destinationEmptyBefore &&
        BoardFacts.sansFor(facts, breakMove).forall(!_.contains("x"))
    val afterPieces =
      if legalPawnMove && nonCapturingMove then
        facts.pieces.filterNot(_.square == breakMove.from) ++ pawnAfter.toVector
      else facts.pieces
    val exactAfterBoardReplay =
      sameBoardProof &&
        legalPawnMove &&
        nonCapturingMove &&
        pawnAfter.exists(afterPawn => afterPieces.exists(_ == afterPawn)) &&
        !afterPieces.exists(_.square == breakMove.from)
    val targetsBefore =
      pawnBefore
        .toVector
        .flatMap(pawnTargets(_, facts.pieces))
        .sortBy(piece => piece.square.index)
    val targetsAfter =
      if exactAfterBoardReplay then
        pawnAfter
          .toVector
          .flatMap(pawnTargets(_, afterPieces))
          .sortBy(piece => piece.square.index)
      else Vector.empty
    val directPawnLeverAfterMove = targetsAfter.nonEmpty
    val leverCreatedByMove =
      targetsAfter.exists(target => !targetsBefore.exists(_.square == target.square))
    val singleRivalPawnTarget = targetsAfter.size == 1
    val targetPawn = Option.when(singleRivalPawnTarget)(targetsAfter.head)
    val contactKinds =
      Vector(
        Option.when(directPawnLeverAfterMove)(PawnBreakContactKind.PawnChallengesPawn),
        Option.when(leverCreatedByMove)(PawnBreakContactKind.PawnLeverCreated)
      ).flatten

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(pawnBefore.isEmpty)("pawn identity"),
      Option.when(pawnBefore.map(_.square).isEmpty)("from square"),
      Option.when(pawnAfter.map(_.square).isEmpty)("to square"),
      Option.when(!legalPawnMove)("legal pawn move"),
      Option.when(!nonPromotionMove)("move is non-promotion"),
      Option.when(!nonCapturingMove)("move is non-capturing"),
      Option.when(!exactAfterBoardReplay)("exact after-board replay"),
      Option.when(!directPawnLeverAfterMove)("direct rival pawn lever after move"),
      Option.when(!leverCreatedByMove)("new direct pawn lever"),
      Option.when(!singleRivalPawnTarget)("exactly one rival pawn target")
    ).flatten.distinct

    PawnBreakProof(
      side = side,
      rivalSide = rivalSide,
      pawnBefore = pawnBefore,
      pawnAfter = pawnAfter,
      targetPawn = targetPawn,
      originSquare = pawnBefore.map(_.square),
      destinationSquare = pawnAfter.map(_.square),
      breakMove = Some(breakMove),
      sameBoardProof = sameBoardProof,
      legalPawnMove = legalPawnMove,
      nonPromotionMove = nonPromotionMove,
      nonCapturingMove = nonCapturingMove,
      exactAfterBoardReplay = exactAfterBoardReplay,
      directPawnLeverAfterMove = directPawnLeverAfterMove,
      leverCreatedByMove = leverCreatedByMove,
      singleRivalPawnTarget = singleRivalPawnTarget,
      contactKinds = contactKinds,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("PawnBreakProof", missing))
    )

  private def onPromotionRank(side: Side, square: Square): Boolean =
    (side == Side.White && square.rank == 7) ||
      (side == Side.Black && square.rank == 0)

  private def pawnTargets(pawn: Piece, pieces: Vector[Piece]): Vector[Piece] =
    BoardFacts
      .pawnAttacks(pawn)
      .flatMap(square =>
        pieces.find(piece => piece.side == BoardFacts.opposite(pawn.side) && piece.man == Man.Pawn && piece.square == square)
      )
