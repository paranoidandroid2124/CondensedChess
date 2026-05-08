package lila.commentary.chess

private[commentary] final case class SkewerProof(
    side: Side,
    rivalSide: Side,
    skewerSlider: Option[Piece],
    frontTarget: Option[Piece],
    rearTarget: Option[Piece],
    skewerMove: Option[Line],
    lineKind: Option[BoardFacts.LineKind],
    sameBoardProof: Boolean,
    frontTargetNonKingMaterial: Boolean,
    rearTargetNonKingMaterial: Boolean,
    frontAndRearSameRivalSide: Boolean,
    afterMoveSliderAttacksFrontTarget: Boolean,
    rearTargetBehindFrontTargetOnSameRay: Boolean,
    noExtraBlockerBreaksFrontToRearRelation: Boolean,
    beforeSkewerRelationAbsentOrBlocked: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object SkewerProof:
  def fromBoardFacts(
      facts: BoardFacts,
      skewerMove: Option[Line],
      sliderSquare: Option[Square],
      frontTargetSquare: Option[Square],
      rearTargetSquare: Option[Square]
  ): SkewerProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMove = skewerMove.flatMap(line => facts.seen.legalMoves.find(_.line == line))
    val side = legalMove.map(_.side).getOrElse(facts.sideToMove)
    val beforePieces = facts.pieces
    val beforeBySquare = beforePieces.map(piece => piece.square -> piece).toMap
    val afterPieces = afterMovePieces(beforePieces, legalMove)
    val afterBySquare = afterPieces.map(piece => piece.square -> piece).toMap
    val skewerSlider =
      sliderSquare
        .flatMap(afterBySquare.get)
        .filter(piece => piece.side == side && sliderPiece(piece.man))
    val beforeSlider =
      sliderSquare
        .flatMap(beforeBySquare.get)
        .filter(piece => piece.side == side && sliderPiece(piece.man))
        .orElse:
          legalMove
            .filter(move => sliderSquare.contains(move.line.to))
            .map(_.piece)
            .filter(piece => piece.side == side && sliderPiece(piece.man))
    val frontTarget = frontTargetSquare.flatMap(afterBySquare.get)
    val rearTarget = rearTargetSquare.flatMap(afterBySquare.get)
    val beforeFrontTarget = frontTargetSquare.flatMap(beforeBySquare.get)
    val beforeRearTarget = rearTargetSquare.flatMap(beforeBySquare.get)
    val rivalSide = frontTarget.orElse(rearTarget).map(_.side).getOrElse(BoardFacts.opposite(side))
    val frontTargetNonKingMaterial =
      frontTarget.exists(piece => piece.side == rivalSide && pieceValue(piece).nonEmpty)
    val rearTargetNonKingMaterial =
      rearTarget.exists(piece => piece.side == rivalSide && pieceValue(piece).nonEmpty)
    val frontAndRearSameRivalSide =
      frontTarget.zip(rearTarget).exists: (front, rear) =>
        front.side == rear.side && front.side == rivalSide && front.side != side
    val beforeRelation = skewerRelation(beforePieces, beforeSlider, beforeFrontTarget, beforeRearTarget)
    val afterRelation = skewerRelation(afterPieces, skewerSlider, frontTarget, rearTarget)
    val beforeSkewerRelationAbsentOrBlocked = !beforeRelation.relation
    val legalSkewerMove =
      legalMove.nonEmpty &&
        afterRelation.relation &&
        beforeSkewerRelationAbsentOrBlocked

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(side == Side.None || side == rivalSide)("rival side"),
      Option.when(skewerSlider.isEmpty)("skewer slider"),
      Option.when(frontTarget.isEmpty)("front target"),
      Option.when(rearTarget.isEmpty)("rear target"),
      Option.when(!legalSkewerMove)("legal skewer or revealing move"),
      Option.when(afterRelation.lineKind.isEmpty)("line kind"),
      Option.when(!frontTargetNonKingMaterial)("front target non-king material piece"),
      Option.when(!rearTargetNonKingMaterial)("rear target non-king material piece"),
      Option.when(!frontAndRearSameRivalSide)("front and rear target same rival side"),
      Option.when(!afterRelation.sliderAttacksFront)("after move slider attacks front target"),
      Option.when(!afterRelation.rearBehindFront)("rear target behind front target on same ray"),
      Option.when(!afterRelation.noExtraBlocker)("no extra blocker breaks front-to-rear relation"),
      Option.when(!beforeSkewerRelationAbsentOrBlocked)("before-move skewer relation absent or blocked")
    ).flatten.distinct

    SkewerProof(
      side = side,
      rivalSide = rivalSide,
      skewerSlider = skewerSlider,
      frontTarget = frontTarget,
      rearTarget = rearTarget,
      skewerMove = skewerMove,
      lineKind = afterRelation.lineKind,
      sameBoardProof = sameBoardProof,
      frontTargetNonKingMaterial = frontTargetNonKingMaterial,
      rearTargetNonKingMaterial = rearTargetNonKingMaterial,
      frontAndRearSameRivalSide = frontAndRearSameRivalSide,
      afterMoveSliderAttacksFrontTarget = afterRelation.sliderAttacksFront,
      rearTargetBehindFrontTargetOnSameRay = afterRelation.rearBehindFront,
      noExtraBlockerBreaksFrontToRearRelation = afterRelation.noExtraBlocker,
      beforeSkewerRelationAbsentOrBlocked = beforeSkewerRelationAbsentOrBlocked,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("SkewerProof", missing))
    )

  private final case class SkewerRelation(
      relation: Boolean,
      lineKind: Option[BoardFacts.LineKind],
      sliderAttacksFront: Boolean,
      rearBehindFront: Boolean,
      noExtraBlocker: Boolean
  )

  private def afterMovePieces(
      pieces: Vector[Piece],
      legalMove: Option[BoardFacts.LegalMove]
  ): Vector[Piece] =
    legalMove
      .map: move =>
        pieces.filterNot(piece => piece.square == move.line.from || piece.square == move.line.to) :+
          move.piece.copy(square = move.line.to)
      .getOrElse(pieces)

  private def skewerRelation(
      pieces: Vector[Piece],
      slider: Option[Piece],
      frontTarget: Option[Piece],
      rearTarget: Option[Piece]
  ): SkewerRelation =
    val occupied = occupiedMask(pieces)
    val stepToRear =
      for
        sliderPiece <- slider
        rear <- rearTarget
        step <- BoardFacts.lineStep(sliderPiece.square, rear.square)
        if BoardFacts.sliderUses(sliderPiece.man, step)
      yield step
    val sliderAttacksFront =
      slider.exists: sliderPiece =>
        frontTarget.exists(front => BoardFacts.attacksSquare(sliderPiece, front.square, occupied))
    val rearBehindFront =
      stepToRear.zip(slider.zip(frontTarget).zip(rearTarget)).exists:
        case (step, ((sliderPiece, front), rear)) =>
          BoardFacts.squaresBetween(sliderPiece.square, rear.square, step).contains(front.square)
    val noExtraBlocker =
      stepToRear.zip(frontTarget.zip(rearTarget)).exists:
        case (step, (front, rear)) =>
          BoardFacts.squaresBetween(front.square, rear.square, step).forall: square =>
            pieces.forall(_.square != square)
    SkewerRelation(
      relation = sliderAttacksFront && rearBehindFront && noExtraBlocker,
      lineKind = stepToRear.map(kindOf),
      sliderAttacksFront = sliderAttacksFront,
      rearBehindFront = rearBehindFront,
      noExtraBlocker = noExtraBlocker
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

  private def occupiedMask(pieces: Vector[Piece]): Long =
    pieces.foldLeft(0L): (mask, piece) =>
      mask | piece.square.bit
