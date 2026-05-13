package lila.commentary.chess

private[commentary] final case class InterferenceProof(
    beforeBoard: Vector[Piece],
    afterMoveBoard: Vector[Piece],
    side: Side,
    rivalSide: Side,
    sideMove: Option[Line],
    lineDefenderBefore: Option[Piece],
    lineDefenderAfter: Option[Piece],
    targetSquare: Option[Square],
    targetPieceBefore: Option[Piece],
    targetPieceAfter: Option[Piece],
    blockingSquare: Option[Square],
    movedPieceBefore: Option[Piece],
    movedPieceAfter: Option[Piece],
    lineKind: Option[BoardFacts.LineKind],
    sameBoardProof: Boolean,
    legalSideMove: Boolean,
    completeStoryProof: Boolean,
    moveIsNonCapture: Boolean,
    movedPieceLandsOnBlockingSquare: Boolean,
    lineDefenderIsSlider: Boolean,
    targetBound: Boolean,
    defenderBlockingTargetCollinearOnSliderRay: Boolean,
    blockingSquareStrictlyBetweenDefenderAndTarget: Boolean,
    defenderLineContactBeforeMove: Boolean,
    afterMoveBlockingSquareOccupiedByMovedPiece: Boolean,
    defenderLineContactRemovedByBlocker: Boolean,
    noEngineEvidenceUsed: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object InterferenceProof:
  def fromBoardFacts(
      facts: BoardFacts,
      sideMove: Option[Line],
      defenderSquare: Option[Square],
      blockingSquare: Option[Square],
      targetSquare: Option[Square]
  ): InterferenceProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val beforeBoard = facts.pieces
    val beforeBySquare = beforeBoard.map(piece => piece.square -> piece).toMap
    val legalSideMoveRow =
      sideMove.flatMap(line => facts.seen.legalMoves.find(move => move.line == line && move.side == facts.sideToMove))
    val legalSideMove = legalSideMoveRow.nonEmpty
    val side = legalSideMoveRow.map(_.side).getOrElse(facts.sideToMove)
    val afterMoveBoard = sideMove.flatMap(line => BoardFacts.piecesAfterLegalMove(facts, line)).getOrElse(Vector.empty)
    val afterBySquare = afterMoveBoard.map(piece => piece.square -> piece).toMap
    val movedPieceBefore = legalSideMoveRow.map(_.piece)
    val movedPieceAfter =
      legalSideMoveRow.flatMap: move =>
        afterBySquare.get(move.line.to).filter(piece => sameManAndSide(piece, move.piece))
    val lineDefenderBefore =
      defenderSquare.flatMap(beforeBySquare.get)
    val lineDefenderAfter =
      lineDefenderBefore.flatMap: defender =>
        afterBySquare.get(defender.square).filter(piece => sameManAndSide(piece, defender))
    val targetPieceBefore = targetSquare.flatMap(beforeBySquare.get)
    val targetPieceAfter =
      targetPieceBefore.flatMap: target =>
        afterBySquare.get(target.square).filter(piece => samePiece(piece, target))
    val rivalSide = lineDefenderBefore.orElse(targetPieceBefore).map(_.side).getOrElse(BoardFacts.opposite(side))
    val lineDefenderIsSlider =
      lineDefenderBefore.exists(defender => defender.side == rivalSide && defender.side != side && sliderPiece(defender.man))
    val targetBound =
      targetSquare.exists: _ =>
        targetPieceBefore match
          case Some(piece) => piece.side == rivalSide && piece.side != side && pieceValue(piece).nonEmpty
          case None        => lineDefenderBefore.nonEmpty
    val lineStep =
      for
        defender <- lineDefenderBefore
        target <- targetSquare
        step <- BoardFacts.lineStep(defender.square, target)
        if BoardFacts.sliderUses(defender.man, step)
      yield step
    val defenderBlockingTargetCollinearOnSliderRay =
      lineDefenderIsSlider && lineStep.nonEmpty
    val blockingSquareStrictlyBetweenDefenderAndTarget =
      (for
        defender <- lineDefenderBefore
        block <- blockingSquare
        target <- targetSquare
        step <- lineStep
      yield BoardFacts.squaresBetween(defender.square, target, step).contains(block)).getOrElse(false)
    val movedPieceLandsOnBlockingSquare =
      legalSideMoveRow.zip(blockingSquare).exists((move, block) => move.line.to == block)
    val moveIsNonCapture =
      legalSideMoveRow.exists: move =>
        beforeBySquare.get(move.line.to).isEmpty && afterMoveBoard.size == beforeBoard.size
    val beforeOccupied = occupiedMask(beforeBoard)
    val defenderLineContactBeforeMove =
      lineDefenderBefore.zip(targetSquare).exists: (defender, target) =>
        targetBound &&
          defenderBlockingTargetCollinearOnSliderRay &&
          BoardFacts.attacksSquare(defender, target, beforeOccupied)
    val afterMoveBlockingSquareOccupiedByMovedPiece =
      movedPieceAfter.zip(blockingSquare).exists: (piece, block) =>
        piece.square == block && movedPieceLandsOnBlockingSquare
    val afterOccupied = occupiedMask(afterMoveBoard)
    val defenderLineContactAfterMove =
      lineDefenderAfter.zip(targetSquare).exists: (defender, target) =>
        BoardFacts.attacksSquare(defender, target, afterOccupied)
    val defenderLineContactRemovedByBlocker =
      defenderLineContactBeforeMove &&
        afterMoveBlockingSquareOccupiedByMovedPiece &&
        blockingSquareStrictlyBetweenDefenderAndTarget &&
        !defenderLineContactAfterMove
    val completeStoryProof =
      sameBoardProof &&
        legalSideMove &&
        side != Side.None &&
        rivalSide != Side.None &&
        side != rivalSide
    val lineKind = lineStep.map(kindOf)

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(!legalSideMove)("legal side move"),
      Option.when(!completeStoryProof)("complete StoryProof"),
      Option.when(!movedPieceLandsOnBlockingSquare)("moved piece lands on blocking square"),
      Option.when(!moveIsNonCapture)("non-capture move"),
      Option.when(!lineDefenderIsSlider)("line defender bishop rook or queen"),
      Option.when(!targetBound)("named target"),
      Option.when(!defenderBlockingTargetCollinearOnSliderRay)(
        "defender blocking square and target collinear on legal slider ray"
      ),
      Option.when(!blockingSquareStrictlyBetweenDefenderAndTarget)(
        "blocking square strictly between defender and target"
      ),
      Option.when(!defenderLineContactBeforeMove)("defender line contact to target before move"),
      Option.when(!afterMoveBlockingSquareOccupiedByMovedPiece)("blocking square occupied by moved piece after move"),
      Option.when(!defenderLineContactRemovedByBlocker)("defender line contact removed by blocking square")
    ).flatten.distinct

    InterferenceProof(
      beforeBoard = beforeBoard,
      afterMoveBoard = afterMoveBoard,
      side = side,
      rivalSide = rivalSide,
      sideMove = sideMove,
      lineDefenderBefore = lineDefenderBefore,
      lineDefenderAfter = lineDefenderAfter,
      targetSquare = targetSquare,
      targetPieceBefore = targetPieceBefore,
      targetPieceAfter = targetPieceAfter,
      blockingSquare = blockingSquare,
      movedPieceBefore = movedPieceBefore,
      movedPieceAfter = movedPieceAfter,
      lineKind = lineKind,
      sameBoardProof = sameBoardProof,
      legalSideMove = legalSideMove,
      completeStoryProof = completeStoryProof,
      moveIsNonCapture = moveIsNonCapture,
      movedPieceLandsOnBlockingSquare = movedPieceLandsOnBlockingSquare,
      lineDefenderIsSlider = lineDefenderIsSlider,
      targetBound = targetBound,
      defenderBlockingTargetCollinearOnSliderRay = defenderBlockingTargetCollinearOnSliderRay,
      blockingSquareStrictlyBetweenDefenderAndTarget = blockingSquareStrictlyBetweenDefenderAndTarget,
      defenderLineContactBeforeMove = defenderLineContactBeforeMove,
      afterMoveBlockingSquareOccupiedByMovedPiece = afterMoveBlockingSquareOccupiedByMovedPiece,
      defenderLineContactRemovedByBlocker = defenderLineContactRemovedByBlocker,
      noEngineEvidenceUsed = true,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("InterferenceProof", missing))
    )

  private def samePiece(candidate: Piece, original: Piece): Boolean =
    candidate.side == original.side && candidate.man == original.man && candidate.square == original.square

  private def sameManAndSide(candidate: Piece, original: Piece): Boolean =
    candidate.side == original.side && candidate.man == original.man

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
