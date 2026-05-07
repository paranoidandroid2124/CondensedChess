package lila.commentary.chess

private[commentary] final case class ThreatProof(
    rivalSide: Side,
    threatenedTarget: Option[Piece],
    attackingPiece: Option[Piece],
    legalThreatLine: Option[Line],
    targetValue: Option[Int],
    materialLossIfUnanswered: Option[Int],
    sameBoardProof: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object ThreatProof:
  def fromBoardFacts(facts: BoardFacts, threatLine: Line): ThreatProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val expectedRival = BoardFacts.opposite(facts.sideToMove)
    val piecesBySquare = facts.pieces.map(piece => piece.square -> piece).toMap
    val legalThreatLine = facts.rivalLegal.lines.contains(threatLine)
    val attackingPiece =
      for
        piece <- piecesBySquare.get(threatLine.from)
        if legalThreatLine && piece.side == expectedRival
      yield piece
    val threatenedTarget =
      piecesBySquare
        .get(threatLine.to)
        .filter(target => attackingPiece.exists(_.side != target.side))
    val targetValue = threatenedTarget.flatMap(pieceValue)
    val targetAttacked =
      attackingPiece.zip(threatenedTarget).exists: (attacker, target) =>
        BoardFacts.attacksSquare(attacker, target.square, occupiedMask(facts.pieces))
    val captureResult = CaptureResult.fromBoardFacts(facts, threatLine)
    val materialLoss =
      Option.when(
        legalThreatLine &&
          attackingPiece.nonEmpty &&
          threatenedTarget.exists(_.man != Man.King) &&
          targetAttacked
      )(captureResult.materialResult.getOrElse(0))

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(!legalThreatLine || threatenedTarget.isEmpty)("legal threat line"),
      Option.when(attackingPiece.isEmpty)("attacking piece"),
      Option.when(threatenedTarget.isEmpty)("threatened target"),
      Option.when(threatenedTarget.exists(_.man == Man.King))("threatened non-king target"),
      Option.when(!targetAttacked)("target attacked"),
      Option.when(targetValue.isEmpty)("target value"),
      Option.when(!materialLoss.exists(_ > 0))("material loss if unanswered")
    ).flatten.distinct

    ThreatProof(
      rivalSide = expectedRival,
      threatenedTarget = threatenedTarget,
      attackingPiece = attackingPiece,
      legalThreatLine = Option.when(missing.isEmpty)(threatLine),
      targetValue = targetValue,
      materialLossIfUnanswered = materialLoss,
      sameBoardProof = sameBoardProof,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("ThreatProof", missing))
    )

  private def pieceValue(piece: Piece): Option[Int] =
    piece.man match
      case Man.Pawn   => Some(100)
      case Man.Knight => Some(320)
      case Man.Bishop => Some(330)
      case Man.Rook   => Some(500)
      case Man.Queen  => Some(900)
      case Man.King   => None

  private def occupiedMask(pieces: Vector[Piece]): Long =
    pieces.foldLeft(0L): (mask, piece) =>
      mask | piece.square.bit
