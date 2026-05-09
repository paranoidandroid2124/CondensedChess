package lila.commentary.chess

private[commentary] final case class PromotionProof(
    side: Side,
    rivalSide: Side,
    pawn: Option[Piece],
    originSquare: Option[Square],
    promotionSquare: Option[Square],
    promotionMove: Option[Line],
    promotedPiece: Option[Piece],
    sameBoardProof: Boolean,
    legalPromotionMove: Boolean,
    nonCapturing: Boolean,
    exactBoardReplay: Boolean,
    pawnReachesFinalRank: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object PromotionProof:
  def fromBoardFacts(facts: BoardFacts, move: Line): PromotionProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMove = facts.seen.legalMoves.find(_.line == move)
    val side = legalMove.map(_.side).getOrElse(facts.sideToMove)
    val rivalSide = BoardFacts.opposite(side)
    val bySquare = facts.pieces.map(piece => piece.square -> piece).toMap
    val pawn =
      bySquare
        .get(move.from)
        .filter(piece => piece.man == Man.Pawn && piece.side == side)
        .filter(piece => legalMove.exists(_.piece == piece))
    val originSquare = pawn.map(_.square)
    val promotionSquare = Option.when(pawn.nonEmpty)(move.to)
    val pawnReachesFinalRank =
      pawn.nonEmpty &&
        onFinalRank(side, move.to)
    val nonCapturing =
      pawn.nonEmpty &&
        move.from.file == move.to.file &&
        !bySquare.contains(move.to)
    val promotedMan =
      BoardFacts
        .sansFor(facts, move)
        .flatMap(promotedManFromSan)
        .sortBy(promotionPreference)
        .headOption
    val promotedPiece =
      for
        man <- promotedMan
        square <- promotionSquare
        if pawnReachesFinalRank
      yield Piece(side, man, square)
    val legalPromotionMove =
      legalMove.nonEmpty &&
        pawn.nonEmpty &&
        pawnReachesFinalRank &&
        promotedPiece.nonEmpty
    val exactBoardReplay =
      sameBoardProof &&
        legalPromotionMove &&
        nonCapturing &&
        promotedPiece.exists(piece => piece.square == move.to && piece.side == side)

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(pawn.isEmpty)("pawn identity"),
      Option.when(originSquare.isEmpty)("origin square"),
      Option.when(promotionSquare.isEmpty)("promotion square"),
      Option.when(!legalPromotionMove)("legal promotion move"),
      Option.when(!nonCapturing)("move is non-capturing"),
      Option.when(promotedPiece.isEmpty)("promoted piece identity"),
      Option.when(!exactBoardReplay)("exact board replay"),
      Option.when(!pawnReachesFinalRank)("pawn reaches final rank")
    ).flatten.distinct

    PromotionProof(
      side = side,
      rivalSide = rivalSide,
      pawn = pawn,
      originSquare = originSquare,
      promotionSquare = promotionSquare,
      promotionMove = Some(move),
      promotedPiece = promotedPiece,
      sameBoardProof = sameBoardProof,
      legalPromotionMove = legalPromotionMove,
      nonCapturing = nonCapturing,
      exactBoardReplay = exactBoardReplay,
      pawnReachesFinalRank = pawnReachesFinalRank,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("PromotionProof", missing))
    )

  private def onFinalRank(side: Side, square: Square): Boolean =
    (side == Side.White && square.rank == 7) ||
      (side == Side.Black && square.rank == 0)

  private def promotedManFromSan(san: String): Option[Man] =
    san.lastIndexOf('=') match
      case -1 => None
      case index if index + 1 >= san.length => None
      case index =>
        san.charAt(index + 1).toUpper match
          case 'Q' => Some(Man.Queen)
          case 'R' => Some(Man.Rook)
          case 'B' => Some(Man.Bishop)
          case 'N' => Some(Man.Knight)
          case _   => None

  private def promotionPreference(man: Man): Int =
    man match
      case Man.Queen  => 0
      case Man.Rook   => 1
      case Man.Bishop => 2
      case Man.Knight => 3
      case _          => 4
