package lila.commentary.chess

private[commentary] final case class PromotionThreatProof(
    side: Side,
    rivalSide: Side,
    pawnBefore: Option[Piece],
    pawnAfter: Option[Piece],
    creatingMove: Option[Line],
    nextPromotionMove: Option[Line],
    promotionSquare: Option[Square],
    promotionRoute: Option[Line],
    promotionSan: Option[String],
    sameBoardProof: Boolean,
    legalPawnMove: Boolean,
    nonPromotionCreatingMove: Boolean,
    exactAfterBoardReplay: Boolean,
    pawnOnPenultimateRankAfter: Boolean,
    nextMovePromotionLegal: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object PromotionThreatProof:
  def fromBoardFacts(facts: BoardFacts, creatingMove: Line): PromotionThreatProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMove = facts.seen.legalMoves.find(_.line == creatingMove)
    val side = legalMove.map(_.side).getOrElse(facts.sideToMove)
    val rivalSide = BoardFacts.opposite(side)
    val bySquare = facts.pieces.map(piece => piece.square -> piece).toMap
    val pawnBefore =
      bySquare
        .get(creatingMove.from)
        .filter(piece => piece.man == Man.Pawn && piece.side == side)
        .filter(piece => legalMove.exists(_.piece == piece))
    val pawnAfter = pawnBefore.map(_.copy(square = creatingMove.to))
    val legalPawnMove =
      legalMove.nonEmpty &&
        pawnBefore.nonEmpty
    val nonPromotionCreatingMove =
      legalPawnMove &&
        pawnAfter.exists(pawn => !onPromotionRank(pawn.side, pawn.square))
    val afterPieces =
      if legalPawnMove && nonPromotionCreatingMove then
        facts.pieces.filterNot(piece => piece.square == creatingMove.from || piece.square == creatingMove.to) ++
          pawnAfter.toVector
      else facts.pieces
    val exactAfterBoardReplay =
      sameBoardProof &&
        legalPawnMove &&
        nonPromotionCreatingMove &&
        pawnAfter.exists(afterPawn => afterPieces.exists(_ == afterPawn)) &&
        !afterPieces.exists(_.square == creatingMove.from)
    val pawnOnPenultimateRankAfter =
      exactAfterBoardReplay &&
        pawnAfter.exists(pawn => onPenultimateRank(pawn.side, pawn.square))
    val promotionSquare =
      pawnAfter.flatMap: pawn =>
        Option.when(pawnOnPenultimateRankAfter)(Square(('a' + pawn.square.file).toChar, finalRank(pawn.side) + 1))
    val promotionRoute =
      pawnAfter.zip(promotionSquare).map((pawn, square) => Line(pawn.square, square))
    val legalPromotionContinuation =
      promotionRoute.flatMap: route =>
        BoardFacts
          .sameSideLegalContinuationsAfter(facts, creatingMove)
          .find(continuation => continuation.promotion && continuation.line == route)
    val nextMovePromotionLegal =
      exactAfterBoardReplay &&
        pawnOnPenultimateRankAfter &&
        legalPromotionContinuation.nonEmpty
    val promotionSan = legalPromotionContinuation.map(_.san)

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(pawnBefore.isEmpty)("pawn identity"),
      Option.when(!legalPawnMove)("legal pawn move"),
      Option.when(!nonPromotionCreatingMove)("creating move is non-promotion"),
      Option.when(!exactAfterBoardReplay)("exact after-board replay"),
      Option.when(!pawnOnPenultimateRankAfter)("pawn on penultimate rank after move"),
      Option.when(promotionSquare.isEmpty)("promotion square"),
      Option.when(promotionRoute.isEmpty)("promotion route"),
      Option.when(!nextMovePromotionLegal)("legal next-move promotion")
    ).flatten.distinct

    PromotionThreatProof(
      side = side,
      rivalSide = rivalSide,
      pawnBefore = pawnBefore,
      pawnAfter = pawnAfter,
      creatingMove = Some(creatingMove),
      nextPromotionMove = promotionRoute,
      promotionSquare = promotionSquare,
      promotionRoute = promotionRoute,
      promotionSan = promotionSan,
      sameBoardProof = sameBoardProof,
      legalPawnMove = legalPawnMove,
      nonPromotionCreatingMove = nonPromotionCreatingMove,
      exactAfterBoardReplay = exactAfterBoardReplay,
      pawnOnPenultimateRankAfter = pawnOnPenultimateRankAfter,
      nextMovePromotionLegal = nextMovePromotionLegal,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("PromotionThreatProof", missing))
    )

  private def onPromotionRank(side: Side, square: Square): Boolean =
    (side == Side.White && square.rank == 7) ||
      (side == Side.Black && square.rank == 0)

  private def onPenultimateRank(side: Side, square: Square): Boolean =
    (side == Side.White && square.rank == 6) ||
      (side == Side.Black && square.rank == 1)

  private def finalRank(side: Side): Int =
    if side == Side.White then 7 else 0
