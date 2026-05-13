package lila.commentary.chess

private[commentary] final case class DecoyProof(
    beforeBoard: Vector[Piece],
    afterSideMoveBoard: Vector[Piece],
    afterReplyBoard: Vector[Piece],
    side: Side,
    rivalSide: Side,
    sideMove: Option[Line],
    rivalReply: Option[Line],
    namedPieceBeforeReply: Option[Piece],
    namedPieceAfterReply: Option[Piece],
    decoySquare: Option[Square],
    landingSquare: Option[Square],
    trapFollowUpProof: Option[TrapProof],
    sameBoardProof: Boolean,
    legalSideMove: Boolean,
    legalRivalReply: Boolean,
    replyByNamedPiece: Boolean,
    rivalPieceRemainsAfterReply: Boolean,
    replyLandsOnDecoySquare: Boolean,
    decoySquareEqualsLandingSquare: Boolean,
    completeTrapFollowUpProof: Boolean,
    trapFollowUpBindsSamePieceAndSquare: Boolean,
    completeStoryProof: Boolean,
    noEngineEvidenceUsed: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object DecoyProof:
  def fromBoardFacts(
      facts: BoardFacts,
      sideMove: Option[Line],
      rivalReply: Option[Line],
      namedPieceSquare: Option[Square],
      decoySquare: Option[Square],
      trapFollowUpProof: Option[TrapProof]
  ): DecoyProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val beforeBoard = facts.pieces
    val legalSideMoveRow =
      sideMove.flatMap(line => facts.seen.legalMoves.find(move => move.line == line && move.side == facts.sideToMove))
    val legalSideMove = legalSideMoveRow.nonEmpty
    val side = legalSideMoveRow.map(_.side).getOrElse(facts.sideToMove)
    val afterSideMoveBoard =
      sideMove.flatMap(line => BoardFacts.piecesAfterLegalMove(facts, line)).getOrElse(Vector.empty)
    val afterSideMoveBySquare = afterSideMoveBoard.map(piece => piece.square -> piece).toMap
    val namedPieceBeforeReply = namedPieceSquare.flatMap(afterSideMoveBySquare.get)
    val rivalSide = namedPieceBeforeReply.map(_.side).getOrElse(BoardFacts.opposite(side))
    val legalReplyRows =
      sideMove.toVector.flatMap(line => BoardFacts.rivalLegalRepliesAfter(facts, line))
    val legalReplyRow = rivalReply.flatMap(line => legalReplyRows.find(_.line == line))
    val legalRivalReply = legalReplyRow.nonEmpty
    val afterReplyBoard = legalReplyRow.map(_.pieces).getOrElse(Vector.empty)
    val afterReplyBySquare = afterReplyBoard.map(piece => piece.square -> piece).toMap
    val replyByNamedPiece =
      rivalReply.zip(namedPieceBeforeReply).exists: (reply, piece) =>
        reply.from == piece.square && piece.side == rivalSide && piece.side != side
    val namedPieceAfterReply =
      namedPieceBeforeReply.flatMap: piece =>
        val expectedSquare =
          if replyByNamedPiece then rivalReply.map(_.to) else Some(piece.square)
        expectedSquare.flatMap(afterReplyBySquare.get).filter(candidate => sameManAndSide(candidate, piece))
    val landingSquare = namedPieceAfterReply.map(_.square)
    val rivalPieceRemainsAfterReply = namedPieceAfterReply.nonEmpty
    val replyLandsOnDecoySquare =
      rivalReply.zip(decoySquare).exists((reply, square) => reply.to == square)
    val decoySquareEqualsLandingSquare =
      decoySquare.zip(landingSquare).exists((target, landing) => target == landing)
    val completeTrapFollowUpProof = trapFollowUpProof.exists(_.complete)
    val trapFollowUpBindsSamePieceAndSquare =
      trapFollowUpProof.zip(namedPieceAfterReply).exists: (trap, piece) =>
        trap.targetPieceAfter.exists(target => sameManAndSide(target, piece)) &&
          trap.targetPieceSquareAfter.contains(piece.square) &&
          decoySquare.contains(piece.square)
    val completeStoryProof =
      sameBoardProof &&
        legalSideMove &&
        legalRivalReply &&
        afterSideMoveBoard.nonEmpty &&
        afterReplyBoard.nonEmpty &&
        side != Side.None &&
        rivalSide != Side.None &&
        side != rivalSide

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(!legalSideMove)("legal side move"),
      Option.when(!legalRivalReply)("legal rival reply"),
      Option.when(namedPieceBeforeReply.isEmpty)("named rival piece before reply"),
      Option.when(!replyByNamedPiece)("reply by named rival piece"),
      Option.when(!rivalPieceRemainsAfterReply)("rival piece remains after reply"),
      Option.when(!replyLandsOnDecoySquare)("reply lands on decoy square"),
      Option.when(!decoySquareEqualsLandingSquare)("decoy square equals landing square"),
      Option.when(!completeTrapFollowUpProof)("complete Trap follow-up proof"),
      Option.when(!trapFollowUpBindsSamePieceAndSquare)("Trap follow-up binds same piece and square"),
      Option.when(!completeStoryProof)("complete StoryProof")
    ).flatten.distinct

    DecoyProof(
      beforeBoard = beforeBoard,
      afterSideMoveBoard = afterSideMoveBoard,
      afterReplyBoard = afterReplyBoard,
      side = side,
      rivalSide = rivalSide,
      sideMove = sideMove,
      rivalReply = rivalReply,
      namedPieceBeforeReply = namedPieceBeforeReply,
      namedPieceAfterReply = namedPieceAfterReply,
      decoySquare = decoySquare,
      landingSquare = landingSquare,
      trapFollowUpProof = trapFollowUpProof,
      sameBoardProof = sameBoardProof,
      legalSideMove = legalSideMove,
      legalRivalReply = legalRivalReply,
      replyByNamedPiece = replyByNamedPiece,
      rivalPieceRemainsAfterReply = rivalPieceRemainsAfterReply,
      replyLandsOnDecoySquare = replyLandsOnDecoySquare,
      decoySquareEqualsLandingSquare = decoySquareEqualsLandingSquare,
      completeTrapFollowUpProof = completeTrapFollowUpProof,
      trapFollowUpBindsSamePieceAndSquare = trapFollowUpBindsSamePieceAndSquare,
      completeStoryProof = completeStoryProof,
      noEngineEvidenceUsed = true,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("DecoyProof", missing))
    )

  private def sameManAndSide(candidate: Piece, original: Piece): Boolean =
    candidate.side == original.side && candidate.man == original.man
