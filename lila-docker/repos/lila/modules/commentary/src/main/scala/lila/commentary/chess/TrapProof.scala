package lila.commentary.chess

private[commentary] final case class TrapProof(
    attackingSide: Side,
    rivalSide: Side,
    movingPieceBefore: Option[Piece],
    movingPieceAfter: Option[Piece],
    anchorSquareAfter: Option[Square],
    targetPieceAfter: Option[Piece],
    targetPieceSquareAfter: Option[Square],
    attackingPiecesAfter: Vector[Piece],
    rivalDefendersAfter: Vector[Piece],
    targetLegalMovesAfter: Vector[Line],
    unsafeTargetMovesAfter: Vector[Line],
    safeEscapeMovesAfter: Vector[Line],
    originSquare: Option[Square],
    destinationSquare: Option[Square],
    trapMove: Option[Line],
    sameBoardProof: Boolean,
    legalMove: Boolean,
    nonCapturingMove: Boolean,
    exactAfterBoardReplay: Boolean,
    completeStoryProof: Boolean,
    targetRivalMinor: Boolean,
    targetDefendedByRivalSide: Boolean,
    afterBoardTargetAttackedByMovingSide: Boolean,
    targetHasLegalMoves: Boolean,
    everyTargetMoveUnsafe: Boolean,
    routeDoesNotGiveCheck: Boolean,
    routeDoesNotGiveMate: Boolean,
    routeDoesNotPromote: Boolean,
    noEngineEvidenceUsed: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object TrapProof:
  def fromBoardFacts(facts: BoardFacts, move: Line): TrapProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMoveRow = facts.seen.legalMoves.find(_.line == move)
    val attackingSide = legalMoveRow.map(_.side).getOrElse(facts.sideToMove)
    val rivalSide = BoardFacts.opposite(attackingSide)
    val movingPieceBefore = legalMoveRow.map(_.piece)
    val afterPieces = BoardFacts.piecesAfterLegalMove(facts, move)
    val movingPieceAfter =
      afterPieces.flatMap(_.find(piece => piece.side == attackingSide && piece.square == move.to))
    val exactAfterBoardReplay =
      sameBoardProof &&
        legalMoveRow.nonEmpty &&
        afterPieces.nonEmpty &&
        movingPieceBefore.nonEmpty &&
        movingPieceAfter.nonEmpty
    val occupiedAfterMask = occupiedMask(afterPieces.toVector.flatten)
    val nonCapturingMove =
      legalMoveRow.nonEmpty &&
        afterPieces.exists(_.size == facts.pieces.size) &&
        !facts.pieces.exists(piece => piece.side == rivalSide && piece.square == move.to)
    val routeDoesNotPromote =
      legalMoveRow.forall(row => row.piece.man != Man.Pawn || (move.to.rank != 0 && move.to.rank != 7)) &&
        BoardFacts.sansFor(facts, move).forall(!_.contains("="))
    val routeGivesCheck =
      BoardFacts.sideInCheckAfterLegalMove(facts, move, rivalSide).contains(true)
    val routeDoesNotGiveCheck =
      BoardFacts.sideInCheckAfterLegalMove(facts, move, rivalSide).contains(false)
    val routeDoesNotGiveMate =
      !routeGivesCheck || BoardFacts.sideLegalMoveCountAfterLegalMove(facts, move, rivalSide).exists(_ > 0)

    val attackedDefendedMinorTargets =
      if exactAfterBoardReplay then
        afterPieces.toVector.flatten
          .filter(piece => piece.side == rivalSide && minor(piece.man))
          .map: target =>
            val attackers = attackersOf(afterPieces.toVector.flatten, attackingSide, target.square, occupiedAfterMask)
            val defenders = defendersOf(afterPieces.toVector.flatten, rivalSide, target, occupiedAfterMask)
            (target, attackers, defenders)
          .filter((_, attackers, defenders) => attackers.nonEmpty && defenders.nonEmpty)
          .sortBy((target, _, _) => (target.man.ordinal, target.square.index))
      else Vector.empty
    val selected =
      Option.when(attackedDefendedMinorTargets.size == 1)(attackedDefendedMinorTargets.head)
    val targetPieceAfter = selected.map(_._1)
    val attackingPiecesAfter = selected.map(_._2).getOrElse(Vector.empty)
    val rivalDefendersAfter = selected.map(_._3).getOrElse(Vector.empty)
    val targetLegalReplies =
      targetPieceAfter.fold(Vector.empty): target =>
        BoardFacts.rivalLegalRepliesAfter(facts, move)
          .filter(_.line.from == target.square)
          .sortBy(reply => (reply.line.from.index, reply.line.to.index))
    val targetSafety =
      targetPieceAfter.fold(Vector.empty[(Line, Boolean)]): target =>
        targetLegalReplies.map: reply =>
          val replyOccupied = occupiedMask(reply.pieces)
          val targetAfterReply =
            reply.pieces.find(piece => piece.side == rivalSide && piece.man == target.man && piece.square == reply.line.to)
          val stillAttacked =
            targetAfterReply.exists: movedTarget =>
              reply.pieces.exists(piece =>
                piece.side == attackingSide &&
                  BoardFacts.attacksSquare(piece, movedTarget.square, replyOccupied)
              )
          reply.line -> stillAttacked
    val unsafeTargetMovesAfter = targetSafety.collect { case (line, true) => line }
    val safeEscapeMovesAfter = targetSafety.collect { case (line, false) => line }
    val targetHasLegalMoves = targetLegalReplies.nonEmpty
    val everyTargetMoveUnsafe =
      targetHasLegalMoves && targetLegalReplies.size == unsafeTargetMovesAfter.size && safeEscapeMovesAfter.isEmpty
    val completeStoryProof =
      sameBoardProof &&
        legalMoveRow.nonEmpty &&
        movingPieceAfter.nonEmpty &&
        targetPieceAfter.nonEmpty &&
        targetPieceAfter.exists(_.side == rivalSide) &&
        attackingSide != Side.None &&
        rivalSide != Side.None &&
        attackingSide != rivalSide

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(legalMoveRow.isEmpty)("legal move identity"),
      Option.when(movingPieceBefore.isEmpty)("moving piece identity"),
      Option.when(movingPieceBefore.map(_.square).isEmpty)("origin square"),
      Option.when(movingPieceAfter.map(_.square).isEmpty)("anchor moved piece after route"),
      Option.when(!exactAfterBoardReplay)("exact after-board replay"),
      Option.when(!completeStoryProof)("complete StoryProof"),
      Option.when(!nonCapturingMove)("non-capturing route"),
      Option.when(!routeDoesNotGiveCheck)("route does not give check"),
      Option.when(!routeDoesNotGiveMate)("route does not give mate"),
      Option.when(!routeDoesNotPromote)("route does not promote"),
      Option.when(targetPieceAfter.isEmpty)("target piece identity"),
      Option.when(targetPieceAfter.map(_.square).isEmpty)("target piece square after move"),
      Option.when(!targetPieceAfter.exists(piece => piece.side == rivalSide && minor(piece.man)))(
        "target is rival knight or bishop"
      ),
      Option.when(attackedDefendedMinorTargets.size != 1)("exactly one defended rival minor target attacked"),
      Option.when(attackingPiecesAfter.isEmpty)("after-board target attacked by moving side"),
      Option.when(rivalDefendersAfter.isEmpty)("target defended by rival side"),
      Option.when(!targetHasLegalMoves)("target has legal target-piece moves"),
      Option.when(!everyTargetMoveUnsafe)("no safe target-piece escape")
    ).flatten.distinct

    TrapProof(
      attackingSide = attackingSide,
      rivalSide = rivalSide,
      movingPieceBefore = movingPieceBefore,
      movingPieceAfter = movingPieceAfter,
      anchorSquareAfter = movingPieceAfter.map(_.square),
      targetPieceAfter = targetPieceAfter,
      targetPieceSquareAfter = targetPieceAfter.map(_.square),
      attackingPiecesAfter = attackingPiecesAfter,
      rivalDefendersAfter = rivalDefendersAfter,
      targetLegalMovesAfter = targetLegalReplies.map(_.line),
      unsafeTargetMovesAfter = unsafeTargetMovesAfter,
      safeEscapeMovesAfter = safeEscapeMovesAfter,
      originSquare = movingPieceBefore.map(_.square),
      destinationSquare = movingPieceAfter.map(_.square),
      trapMove = Some(move),
      sameBoardProof = sameBoardProof,
      legalMove = legalMoveRow.nonEmpty,
      nonCapturingMove = nonCapturingMove,
      exactAfterBoardReplay = exactAfterBoardReplay,
      completeStoryProof = completeStoryProof,
      targetRivalMinor = targetPieceAfter.exists(piece => piece.side == rivalSide && minor(piece.man)),
      targetDefendedByRivalSide = rivalDefendersAfter.nonEmpty,
      afterBoardTargetAttackedByMovingSide = attackingPiecesAfter.nonEmpty,
      targetHasLegalMoves = targetHasLegalMoves,
      everyTargetMoveUnsafe = everyTargetMoveUnsafe,
      routeDoesNotGiveCheck = routeDoesNotGiveCheck,
      routeDoesNotGiveMate = routeDoesNotGiveMate,
      routeDoesNotPromote = routeDoesNotPromote,
      noEngineEvidenceUsed = true,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("TrapProof", missing))
    )

  private def minor(man: Man): Boolean =
    man == Man.Knight || man == Man.Bishop

  private def occupiedMask(pieces: Vector[Piece]): Long =
    pieces.foldLeft(0L)((mask, piece) => mask | piece.square.bit)

  private def attackersOf(pieces: Vector[Piece], side: Side, target: Square, occupied: Long): Vector[Piece] =
    pieces
      .filter(_.side == side)
      .filter(piece => BoardFacts.attacksSquare(piece, target, occupied))
      .sortBy(piece => (piece.man.ordinal, piece.square.index))

  private def defendersOf(pieces: Vector[Piece], side: Side, target: Piece, occupied: Long): Vector[Piece] =
    pieces
      .filter(piece => piece.side == side && piece != target)
      .filter(piece => BoardFacts.attacksSquare(piece, target.square, occupied))
      .sortBy(piece => (piece.man.ordinal, piece.square.index))
