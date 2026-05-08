package lila.commentary.chess

private[commentary] final case class LineProof(
    side: Side,
    slider: Option[Piece],
    blocker: Option[Piece],
    movedPiece: Option[Piece],
    revealedTarget: Option[Piece],
    revealingMove: Option[Line],
    lineKind: Option[BoardFacts.LineKind],
    sameBoardProof: Boolean,
    beforeLineBlockedOrInactive: Boolean,
    afterSliderAttacksTarget: Boolean,
    targetNonKingMaterial: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object LineProof:
  def fromBoardFacts(
      facts: BoardFacts,
      revealingMove: Option[Line],
      sliderSquare: Option[Square],
      targetSquare: Option[Square]
  ): LineProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMove = revealingMove.flatMap(line => facts.seen.legalMoves.find(_.line == line))
    val side = legalMove.map(_.side).getOrElse(facts.sideToMove)
    val piecesBySquare = facts.pieces.map(piece => piece.square -> piece).toMap
    val movedPiece =
      for
        move <- revealingMove
        legal <- legalMove
        piece <- piecesBySquare.get(move.from)
        if legal.piece == piece && legal.side == piece.side
      yield piece
    val slider =
      sliderSquare
        .flatMap(piecesBySquare.get)
        .filter(piece => piece.side == side && sliderPiece(piece.man))
    val revealedTarget = targetSquare.flatMap(piecesBySquare.get)
    val lineStep =
      for
        sliderPiece <- slider
        target <- revealedTarget
        step <- BoardFacts.lineStep(sliderPiece.square, target.square)
        if BoardFacts.sliderUses(sliderPiece.man, step)
      yield step
    val candidateLineFact =
      for
        sliderPiece <- slider
        moved <- movedPiece
        target <- revealedTarget
      yield facts.seen.lineFacts.find: row =>
        row.from.contains(sliderPiece) &&
          row.screen.contains(moved) &&
          row.target.contains(target) &&
          row.blockers == Vector(moved, target)
    val boundLineFact = candidateLineFact.flatten
    val blocker = boundLineFact.flatMap(_.screen)
    val beforeOccupied = facts.pieces.foldLeft(0L): (mask, piece) =>
      mask | piece.square.bit
    val beforeSliderAttacksTarget =
      slider.exists: sliderPiece =>
        revealedTarget.exists: target =>
          BoardFacts.attacksSquare(sliderPiece, target.square, beforeOccupied)
    val movedPieceClearsLine =
      (for
        move <- revealingMove
        sliderPiece <- slider
        target <- revealedTarget
        step <- lineStep
      yield !BoardFacts.squaresBetween(sliderPiece.square, target.square, step).contains(move.to) &&
        move.to != target.square).getOrElse(false)
    val beforeLineBlockedOrInactive =
      boundLineFact.nonEmpty && !beforeSliderAttacksTarget && movedPieceClearsLine
    val afterPieces =
      (for
        move <- revealingMove
        moved <- movedPiece
      yield facts.pieces.filterNot(piece => piece.square == move.from || piece.square == move.to) :+
        moved.copy(square = move.to)).getOrElse(facts.pieces)
    val afterOccupied = afterPieces.foldLeft(0L): (mask, piece) =>
      mask | piece.square.bit
    val targetRemainsAfterMove =
      revealingMove.zip(revealedTarget).forall((move, target) => move.to != target.square)
    val afterSliderAttacksTarget =
      slider.exists: sliderPiece =>
        revealedTarget.exists: target =>
          targetRemainsAfterMove && BoardFacts.attacksSquare(sliderPiece, target.square, afterOccupied)
    val targetNonKingMaterial =
      (for
        sliderPiece <- slider
        target <- revealedTarget
      yield target.side != sliderPiece.side && pieceValue(target).nonEmpty).getOrElse(false)
    val lineKind = boundLineFact.map(_.kind).orElse(lineStep.map(kindOf))

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(legalMove.isEmpty)("legal revealing move"),
      Option.when(slider.isEmpty)("slider piece"),
      Option.when(movedPiece.isEmpty || blocker.isEmpty || blocker != movedPiece)("blocker or moved piece"),
      Option.when(revealedTarget.isEmpty)("revealed target"),
      Option.when(lineKind.isEmpty)("line kind"),
      Option.when(!beforeLineBlockedOrInactive)("before-move blocked or inactive line"),
      Option.when(!afterSliderAttacksTarget)("after-move slider attack"),
      Option.when(!targetNonKingMaterial)("target non-king material piece")
    ).flatten.distinct

    LineProof(
      side = side,
      slider = slider,
      blocker = blocker,
      movedPiece = movedPiece,
      revealedTarget = revealedTarget,
      revealingMove = revealingMove,
      lineKind = lineKind,
      sameBoardProof = sameBoardProof,
      beforeLineBlockedOrInactive = beforeLineBlockedOrInactive,
      afterSliderAttacksTarget = afterSliderAttacksTarget,
      targetNonKingMaterial = targetNonKingMaterial,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("LineProof", missing))
    )

  private def sliderPiece(man: Man): Boolean =
    man == Man.Bishop || man == Man.Rook || man == Man.Queen

  private def kindOf(step: Int): BoardFacts.LineKind =
    if math.abs(step) == 8 then BoardFacts.LineKind.File
    else if math.abs(step) == 1 then BoardFacts.LineKind.Rank
    else BoardFacts.LineKind.Diagonal

  private def pieceValue(piece: Piece): Option[Int] =
    piece.man match
      case Man.Pawn   => Some(100)
      case Man.Knight => Some(320)
      case Man.Bishop => Some(330)
      case Man.Rook   => Some(500)
      case Man.Queen  => Some(900)
      case Man.King   => None
