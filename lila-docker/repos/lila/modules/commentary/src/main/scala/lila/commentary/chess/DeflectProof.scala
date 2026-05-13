package lila.commentary.chess

private[commentary] final case class DeflectProof(
    beforeBoard: Vector[Piece],
    afterSideMoveBoard: Vector[Piece],
    afterReplyBoard: Vector[Piece],
    side: Side,
    rivalSide: Side,
    sideMove: Option[Line],
    rivalReply: Option[Line],
    defenderBeforeReply: Option[Piece],
    defenderAfterReply: Option[Piece],
    targetBeforeReply: Option[Piece],
    targetAfterReply: Option[Piece],
    sameBoardProof: Boolean,
    legalSideMove: Boolean,
    legalRivalReply: Boolean,
    replyByNamedDefender: Boolean,
    defenderGuardedTargetBeforeReply: Boolean,
    defenderNoLongerGuardsTargetAfterReply: Boolean,
    targetRemainsAfterReply: Boolean,
    defenderRemainsAfterReply: Boolean,
    sideMoveDoesNotCaptureDefender: Boolean,
    completeStoryProof: Boolean,
    noEngineEvidenceUsed: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object DeflectProof:
  def fromBoardFacts(
      facts: BoardFacts,
      sideMove: Option[Line],
      rivalReply: Option[Line],
      defenderSquare: Option[Square],
      targetSquare: Option[Square]
  ): DeflectProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val beforeBoard = facts.pieces
    val beforeBySquare = beforeBoard.map(piece => piece.square -> piece).toMap
    val initialNamedDefender = defenderSquare.flatMap(beforeBySquare.get)
    val legalSideMoveRow =
      sideMove.flatMap(line => facts.seen.legalMoves.find(move => move.line == line && move.side == facts.sideToMove))
    val legalSideMove = legalSideMoveRow.nonEmpty
    val side = legalSideMoveRow.map(_.side).getOrElse(facts.sideToMove)
    val afterSideMoveBoard = sideMove.flatMap(line => BoardFacts.piecesAfterLegalMove(facts, line)).getOrElse(Vector.empty)
    val afterSideMoveBySquare = afterSideMoveBoard.map(piece => piece.square -> piece).toMap
    val defenderBeforeReply = defenderSquare.flatMap(afterSideMoveBySquare.get)
    val targetBeforeReply = targetSquare.flatMap(afterSideMoveBySquare.get)
    val rivalSide = defenderBeforeReply.orElse(targetBeforeReply).map(_.side).getOrElse(BoardFacts.opposite(side))
    val legalReplyRows =
      sideMove.toVector.flatMap(line => BoardFacts.rivalLegalRepliesAfter(facts, line))
    val legalReplyRow = rivalReply.flatMap(line => legalReplyRows.find(_.line == line))
    val legalRivalReply = legalReplyRow.nonEmpty
    val afterReplyBoard = legalReplyRow.map(_.pieces).getOrElse(Vector.empty)
    val afterReplyBySquare = afterReplyBoard.map(piece => piece.square -> piece).toMap
    val replyByNamedDefender =
      rivalReply.zip(defenderBeforeReply).exists: (reply, defender) =>
        reply.from == defender.square && defender.side == rivalSide && defender.side != side
    val defenderAfterReply =
      defenderBeforeReply.flatMap: defender =>
        val candidateSquare =
          if replyByNamedDefender then rivalReply.map(_.to) else Some(defender.square)
        candidateSquare.flatMap(afterReplyBySquare.get).filter(piece => sameManAndSide(piece, defender))
    val targetAfterReply =
      targetBeforeReply.flatMap: target =>
        afterReplyBySquare.get(target.square).filter(piece => samePiece(piece, target))
    val targetNonKingMaterial =
      targetBeforeReply.exists(piece => piece.side == rivalSide && piece.side != side && pieceValue(piece).nonEmpty)
    val defenderRivalOwned =
      defenderBeforeReply.exists(piece => piece.side == rivalSide && piece.side != side)
    val defenderGuardedTargetBeforeReply =
      defenderBeforeReply.zip(targetBeforeReply).exists: (defender, target) =>
        targetNonKingMaterial &&
          defenderRivalOwned &&
          defender != target &&
          BoardFacts.attacksSquare(defender, target.square, occupiedMask(afterSideMoveBoard))
    val targetRemainsAfterReply = targetAfterReply.nonEmpty
    val defenderRemainsAfterReply = defenderAfterReply.nonEmpty
    val defenderStillGuardsTargetAfterReply =
      defenderAfterReply.zip(targetAfterReply).exists: (defender, target) =>
        BoardFacts.attacksSquare(defender, target.square, occupiedMask(afterReplyBoard))
    val defenderNoLongerGuardsTargetAfterReply =
      defenderGuardedTargetBeforeReply &&
        targetRemainsAfterReply &&
        defenderRemainsAfterReply &&
        !defenderStillGuardsTargetAfterReply
    val sideMoveCapturesNamedDefender =
      legalSideMoveRow.zip(initialNamedDefender).exists: (move, defender) =>
        move.line.to == defender.square && move.side != defender.side
    val sideMoveDoesNotCaptureDefender = !sideMoveCapturesNamedDefender
    val completeStoryProof =
      sameBoardProof &&
        legalSideMove &&
        afterSideMoveBoard.nonEmpty &&
        side != Side.None &&
        rivalSide != Side.None &&
        side != rivalSide

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(!legalSideMove)("legal side move"),
      Option.when(!legalRivalReply)("legal rival reply"),
      Option.when(!replyByNamedDefender)("reply by named defender"),
      Option.when(defenderBeforeReply.isEmpty || !defenderRivalOwned)("named rival defender before reply"),
      Option.when(targetBeforeReply.isEmpty)("named rival target before reply"),
      Option.when(!targetNonKingMaterial)("target non-king material"),
      Option.when(!defenderGuardedTargetBeforeReply)("defender guards target before reply"),
      Option.when(!defenderNoLongerGuardsTargetAfterReply)("defender no longer guards target after reply"),
      Option.when(!targetRemainsAfterReply)("target remains after reply"),
      Option.when(!defenderRemainsAfterReply)("defender remains after reply"),
      Option.when(!sideMoveDoesNotCaptureDefender)("side move does not capture defender"),
      Option.when(!completeStoryProof)("complete StoryProof")
    ).flatten.distinct

    DeflectProof(
      beforeBoard = beforeBoard,
      afterSideMoveBoard = afterSideMoveBoard,
      afterReplyBoard = afterReplyBoard,
      side = side,
      rivalSide = rivalSide,
      sideMove = sideMove,
      rivalReply = rivalReply,
      defenderBeforeReply = defenderBeforeReply,
      defenderAfterReply = defenderAfterReply,
      targetBeforeReply = targetBeforeReply,
      targetAfterReply = targetAfterReply,
      sameBoardProof = sameBoardProof,
      legalSideMove = legalSideMove,
      legalRivalReply = legalRivalReply,
      replyByNamedDefender = replyByNamedDefender,
      defenderGuardedTargetBeforeReply = defenderGuardedTargetBeforeReply,
      defenderNoLongerGuardsTargetAfterReply = defenderNoLongerGuardsTargetAfterReply,
      targetRemainsAfterReply = targetRemainsAfterReply,
      defenderRemainsAfterReply = defenderRemainsAfterReply,
      sideMoveDoesNotCaptureDefender = sideMoveDoesNotCaptureDefender,
      completeStoryProof = completeStoryProof,
      noEngineEvidenceUsed = true,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("DeflectProof", missing))
    )

  private def samePiece(candidate: Piece, original: Piece): Boolean =
    candidate.side == original.side && candidate.man == original.man && candidate.square == original.square

  private def sameManAndSide(candidate: Piece, original: Piece): Boolean =
    candidate.side == original.side && candidate.man == original.man

  private def occupiedMask(pieces: Vector[Piece]): Long =
    pieces.foldLeft(0L): (mask, piece) =>
      mask | piece.square.bit

  private def pieceValue(piece: Piece): Option[Int] =
    piece.man match
      case Man.Pawn   => Some(100)
      case Man.Knight => Some(320)
      case Man.Bishop => Some(330)
      case Man.Rook   => Some(500)
      case Man.Queen  => Some(900)
      case Man.King   => None
