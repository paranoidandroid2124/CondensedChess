package lila.commentary.chess

private[commentary] enum RemoveGuardRemovalKind:
  case DefenderCaptured
  case GuardLineBlocked

private[commentary] final case class RemoveGuardProof(
    side: Side,
    rivalSide: Side,
    guardedTarget: Option[Piece],
    removedDefender: Option[Piece],
    removeGuardMove: Option[Line],
    removalKind: Option[RemoveGuardRemovalKind],
    targetNonKingMaterial: Boolean,
    defenderGuardedTargetBeforeMove: Boolean,
    afterMoveDefenderNoLongerGuardsTarget: Boolean,
    sameBoardProof: Boolean,
    exactBoardAfterMoveRelation: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object RemoveGuardProof:
  def fromBoardFacts(
      facts: BoardFacts,
      removeGuardMove: Option[Line],
      targetSquare: Option[Square],
      defenderSquare: Option[Square]
  ): RemoveGuardProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMove = removeGuardMove.flatMap(line => facts.seen.legalMoves.find(_.line == line))
    val side = legalMove.map(_.side).getOrElse(facts.sideToMove)
    val beforePieces = facts.pieces
    val beforeBySquare = beforePieces.map(piece => piece.square -> piece).toMap
    val guardedTarget = targetSquare.flatMap(beforeBySquare.get)
    val removedDefender = defenderSquare.flatMap(beforeBySquare.get)
    val rivalSide = guardedTarget.orElse(removedDefender).map(_.side).getOrElse(BoardFacts.opposite(side))
    val targetNonKingMaterial =
      guardedTarget.exists(piece => piece.side == rivalSide && pieceValue(piece).nonEmpty)
    val defenderGuardedTargetBeforeMove =
      removedDefender.zip(guardedTarget).exists: (defender, target) =>
        facts.seen.guards.exists(row => row.guard == defender && row.target == target)
    val afterPieces = afterMovePieces(beforePieces, legalMove)
    val targetAfterMove = guardedTarget.filter(target => afterPieces.exists(_ == target))
    val defenderAfterMove = removedDefender.filter(defender => afterPieces.exists(_ == defender))
    val afterMoveDefenderStillGuards =
      defenderAfterMove.zip(targetAfterMove).exists: (defender, target) =>
        BoardFacts.attacksSquare(defender, target.square, occupiedMask(afterPieces))
    val afterMoveDefenderNoLongerGuardsTarget =
      defenderGuardedTargetBeforeMove && targetAfterMove.nonEmpty && !afterMoveDefenderStillGuards
    val removalKind =
      Vector(
        Option.when(defenderCaptured(legalMove, removedDefender))(RemoveGuardRemovalKind.DefenderCaptured),
        Option.when(guardLineBlocked(legalMove, removedDefender, guardedTarget, afterMoveDefenderNoLongerGuardsTarget))(
          RemoveGuardRemovalKind.GuardLineBlocked
        )
      ).flatten.headOption
    val exactBoardAfterMoveRelation =
      sameBoardProof &&
        legalMove.nonEmpty &&
        targetAfterMove.nonEmpty &&
        afterMoveDefenderNoLongerGuardsTarget &&
        removalKind.nonEmpty

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(side == Side.None || side == rivalSide)("rival side"),
      Option.when(guardedTarget.isEmpty)("guarded target"),
      Option.when(removedDefender.isEmpty)("removed defender"),
      Option.when(legalMove.isEmpty)("legal remove-guard move"),
      Option.when(!targetNonKingMaterial)("target non-king material piece"),
      Option.when(!defenderGuardedTargetBeforeMove)("defender guarded target before move"),
      Option.when(!afterMoveDefenderNoLongerGuardsTarget)("after move defender no longer guards target"),
      Option.when(removalKind.isEmpty)("allowed removal kind"),
      Option.when(!exactBoardAfterMoveRelation)("exact-board after-move relation")
    ).flatten.distinct

    RemoveGuardProof(
      side = side,
      rivalSide = rivalSide,
      guardedTarget = guardedTarget,
      removedDefender = removedDefender,
      removeGuardMove = removeGuardMove,
      removalKind = removalKind,
      targetNonKingMaterial = targetNonKingMaterial,
      defenderGuardedTargetBeforeMove = defenderGuardedTargetBeforeMove,
      afterMoveDefenderNoLongerGuardsTarget = afterMoveDefenderNoLongerGuardsTarget,
      sameBoardProof = sameBoardProof,
      exactBoardAfterMoveRelation = exactBoardAfterMoveRelation,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("RemoveGuardProof", missing))
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

  private def defenderCaptured(
      legalMove: Option[BoardFacts.LegalMove],
      defender: Option[Piece]
  ): Boolean =
    legalMove.zip(defender).exists: (move, removedDefender) =>
      move.side != removedDefender.side && move.line.to == removedDefender.square

  private def guardLineBlocked(
      legalMove: Option[BoardFacts.LegalMove],
      defender: Option[Piece],
      target: Option[Piece],
      afterGuardRemoved: Boolean
  ): Boolean =
    legalMove.zip(defender).zip(target).exists:
      case ((move, removedDefender), guardedTarget) =>
        move.side != removedDefender.side &&
          move.line.to != removedDefender.square &&
          slider(removedDefender) &&
          BoardFacts.lineStep(removedDefender.square, guardedTarget.square).exists: step =>
            BoardFacts.sliderUses(removedDefender.man, step) &&
              BoardFacts.squaresBetween(removedDefender.square, guardedTarget.square, step).contains(move.line.to) &&
              afterGuardRemoved

  private def slider(piece: Piece): Boolean =
    piece.man == Man.Bishop || piece.man == Man.Rook || piece.man == Man.Queen

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
