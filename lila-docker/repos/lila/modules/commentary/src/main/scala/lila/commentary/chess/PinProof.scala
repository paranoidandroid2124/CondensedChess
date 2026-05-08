package lila.commentary.chess

private[commentary] final case class PinProof(
    side: Side,
    pinnedTarget: Option[Piece],
    pinningSlider: Option[Piece],
    kingBehindTarget: Option[Piece],
    pinningMove: Option[Line],
    lineKind: Option[BoardFacts.LineKind],
    sameBoardProof: Boolean,
    beforePinRelation: Boolean,
    afterPinRelation: Boolean,
    targetNonKing: Boolean,
    targetAndKingSameSide: Boolean,
    sliderAttacksThroughTargetTowardKingAfterMove: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object PinProof:
  def fromBoardFacts(
      facts: BoardFacts,
      pinningMove: Option[Line],
      sliderSquare: Option[Square],
      targetSquare: Option[Square],
      kingSquare: Option[Square]
  ): PinProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMove = pinningMove.flatMap(line => facts.seen.legalMoves.find(_.line == line))
    val side = legalMove.map(_.side).getOrElse(facts.sideToMove)
    val beforePieces = facts.pieces
    val beforeBySquare = beforePieces.map(piece => piece.square -> piece).toMap
    val afterPieces = afterMovePieces(beforePieces, legalMove)
    val afterBySquare = afterPieces.map(piece => piece.square -> piece).toMap
    val pinnedTarget = targetSquare.flatMap(afterBySquare.get)
    val kingBehindTarget = kingSquare.flatMap(afterBySquare.get).filter(_.man == Man.King)
    val pinningSlider =
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
    val beforeTarget = targetSquare.flatMap(beforeBySquare.get)
    val beforeKing = kingSquare.flatMap(beforeBySquare.get).filter(_.man == Man.King)
    val beforeRelation =
      pinRelation(beforePieces, beforeSlider, beforeTarget, beforeKing).relation
    val afterPin = pinRelation(afterPieces, pinningSlider, pinnedTarget, kingBehindTarget)
    val afterRelation = afterPin.relation
    val targetNonKing = pinnedTarget.exists(_.man != Man.King)
    val targetAndKingSameSide =
      pinnedTarget.zip(kingBehindTarget).exists((target, king) => target.side == king.side)
    val lineKind = afterPin.lineKind
    val sliderRay = afterPin.sliderRayThroughTargetToKing

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(legalMove.isEmpty)("legal pinning or revealing move"),
      Option.when(pinningSlider.isEmpty)("pinning slider"),
      Option.when(pinnedTarget.isEmpty)("pinned target"),
      Option.when(kingBehindTarget.isEmpty)("king behind target"),
      Option.when(lineKind.isEmpty)("line kind"),
      Option.when(beforeRelation || !afterRelation)("before/after pin relation"),
      Option.when(!targetNonKing)("target non-king"),
      Option.when(!targetAndKingSameSide)("target and king same side"),
      Option.when(!sliderRay)("slider attacks through target toward king after move")
    ).flatten.distinct

    PinProof(
      side = side,
      pinnedTarget = pinnedTarget,
      pinningSlider = pinningSlider,
      kingBehindTarget = kingBehindTarget,
      pinningMove = pinningMove,
      lineKind = lineKind,
      sameBoardProof = sameBoardProof,
      beforePinRelation = beforeRelation,
      afterPinRelation = afterRelation,
      targetNonKing = targetNonKing,
      targetAndKingSameSide = targetAndKingSameSide,
      sliderAttacksThroughTargetTowardKingAfterMove = sliderRay,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("PinProof", missing))
    )

  private final case class PinRelation(
      relation: Boolean,
      lineKind: Option[BoardFacts.LineKind],
      sliderRayThroughTargetToKing: Boolean
  )

  private def afterMovePieces(pieces: Vector[Piece], legalMove: Option[BoardFacts.LegalMove]): Vector[Piece] =
    legalMove
      .map: move =>
        pieces.filterNot(piece => piece.square == move.line.from || piece.square == move.line.to) :+
          move.piece.copy(square = move.line.to)
      .getOrElse(pieces)

  private def pinRelation(
      pieces: Vector[Piece],
      slider: Option[Piece],
      target: Option[Piece],
      king: Option[Piece]
  ): PinRelation =
    val bySquare = pieces.map(piece => piece.square -> piece).toMap
    val step =
      for
        sliderPiece <- slider
        kingPiece <- king
        lineStep <- BoardFacts.lineStep(sliderPiece.square, kingPiece.square)
        if sliderUses(sliderPiece.man, lineStep)
      yield lineStep
    val blockers =
      step
        .zip(slider.zip(king))
        .map: (lineStep, sliderAndKing) =>
          val (sliderPiece, kingPiece) = sliderAndKing
          BoardFacts.squaresBetween(sliderPiece.square, kingPiece.square, lineStep).flatMap(bySquare.get)
        .getOrElse(Vector.empty)
    val targetAndKingSameSide = target.zip(king).exists((targetPiece, kingPiece) => targetPiece.side == kingPiece.side)
    val sliderOpposesKing = slider.zip(king).exists((sliderPiece, kingPiece) => sliderPiece.side != kingPiece.side)
    val rayThroughTarget =
      step.nonEmpty &&
        targetAndKingSameSide &&
        sliderOpposesKing &&
        target.exists(targetPiece => blockers == Vector(targetPiece))
    PinRelation(
      relation = rayThroughTarget,
      lineKind = step.map(kindOf),
      sliderRayThroughTargetToKing = rayThroughTarget
    )

  private def sliderPiece(man: Man): Boolean =
    man == Man.Bishop || man == Man.Rook || man == Man.Queen

  private def sliderUses(man: Man, step: Int): Boolean =
    man match
      case Man.Rook => math.abs(step) == 1 || math.abs(step) == 8
      case Man.Bishop => math.abs(step) == 7 || math.abs(step) == 9
      case Man.Queen =>
        math.abs(step) == 1 || math.abs(step) == 7 || math.abs(step) == 8 || math.abs(step) == 9
      case _ => false

  private def kindOf(step: Int): BoardFacts.LineKind =
    if math.abs(step) == 8 then BoardFacts.LineKind.File
    else if math.abs(step) == 1 then BoardFacts.LineKind.Rank
    else BoardFacts.LineKind.Diagonal
