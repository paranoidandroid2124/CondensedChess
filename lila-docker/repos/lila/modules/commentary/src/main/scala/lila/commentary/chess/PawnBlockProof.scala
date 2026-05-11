package lila.commentary.chess

private[commentary] final case class PawnBlockProof(
    blockingSide: Side,
    rivalSide: Side,
    movingPieceBefore: Option[Piece],
    movingPieceAfter: Option[Piece],
    originSquare: Option[Square],
    destinationSquare: Option[Square],
    blockedPawn: Option[Piece],
    blockedPawnSquare: Option[Square],
    blockedPawnNextAdvanceSquare: Option[Square],
    blockMove: Option[Line],
    sameBoardProof: Boolean,
    legalMove: Boolean,
    nextAdvanceSquareOccupiedAfter: Boolean,
    occupyingPieceBelongsToBlockingSide: Boolean,
    exactAfterBoardReplay: Boolean,
    blockCreatedByMove: Boolean,
    ordinaryDirectOneSquarePawnBlock: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object PawnBlockProof:
  def fromBoardFacts(facts: BoardFacts, move: Line): PawnBlockProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMoveRow = facts.seen.legalMoves.find(_.line == move)
    val blockingSide = legalMoveRow.map(_.side).getOrElse(facts.sideToMove)
    val rivalSide = BoardFacts.opposite(blockingSide)
    val bySquare = facts.pieces.map(piece => piece.square -> piece).toMap
    val movingPieceBefore =
      bySquare
        .get(move.from)
        .filter(piece => piece.side == blockingSide)
        .filter(piece => legalMoveRow.exists(_.piece == piece))
    val afterPieces = BoardFacts.piecesAfterLegalMove(facts, move)
    val movingPieceAfter =
      afterPieces.flatMap(_.find(piece => piece.side == blockingSide && piece.square == move.to))
    val rivalPassedPawns =
      facts.seen.passedPawnObservations.filter(_.side == rivalSide).map(_.pawn).toSet
    val passedPawnStopCandidate =
      facts.seen.passedPawnObservations.exists(row =>
        row.side == rivalSide && nextAdvance(row.pawn).contains(move.to)
      )
    val blockedPawnCandidates =
      facts.pieces
        .filter(piece => piece.side == rivalSide && piece.man == Man.Pawn)
        .filterNot(rivalPassedPawns.contains)
        .filter(pawn => nextAdvance(pawn).contains(move.to))
        .sortBy(_.square.index)
    val singleBlockedPawn = blockedPawnCandidates.size == 1
    val blockedPawn = Option.when(singleBlockedPawn)(blockedPawnCandidates.head)
    val nextSquare = blockedPawn.flatMap(nextAdvance)
    val nextAdvanceSquareOccupiedAfter =
      nextSquare.exists(square => afterPieces.exists(_.exists(_.square == square)))
    val occupyingPieceBelongsToBlockingSide =
      nextSquare.exists: square =>
        afterPieces.exists(_.exists(piece => piece.square == square && piece.side == blockingSide))
    val exactAfterBoardReplay =
      sameBoardProof &&
        legalMoveRow.nonEmpty &&
        afterPieces.nonEmpty &&
        movingPieceAfter.nonEmpty &&
        movingPieceBefore.forall(beforePiece =>
          !afterPieces.exists(_.exists(piece => piece.square == move.from && piece == beforePiece))
        )
    val pawnCapture =
      movingPieceBefore.exists(_.man == Man.Pawn) &&
        bySquare.get(move.to).exists(_.side == rivalSide) &&
        move.from.file != move.to.file
    val blockCreatedByMove =
      exactAfterBoardReplay &&
        movingPieceAfter.exists(piece => nextSquare.contains(piece.square)) &&
        occupyingPieceBelongsToBlockingSide
    val ordinaryDirectOneSquarePawnBlock =
      legalMoveRow.nonEmpty &&
        movingPieceBefore.nonEmpty &&
        movingPieceAfter.nonEmpty &&
        singleBlockedPawn &&
        blockCreatedByMove &&
        !pawnCapture &&
        !passedPawnStopCandidate

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(legalMoveRow.isEmpty)("legal move identity"),
      Option.when(movingPieceBefore.isEmpty)("moving piece identity"),
      Option.when(movingPieceBefore.map(_.square).isEmpty)("origin square"),
      Option.when(movingPieceAfter.map(_.square).isEmpty)("destination square"),
      Option.when(!singleBlockedPawn)("one blocked rival pawn"),
      Option.when(blockedPawn.map(_.square).isEmpty)("blocked pawn square"),
      Option.when(nextSquare.isEmpty)("blocked pawn next advance square"),
      Option.when(!nextAdvanceSquareOccupiedAfter)("next advance square occupied after move"),
      Option.when(!occupyingPieceBelongsToBlockingSide)("occupying piece belongs to blocking side"),
      Option.when(!exactAfterBoardReplay)("exact after-board replay"),
      Option.when(!blockCreatedByMove)("block created by this move"),
      Option.when(pawnCapture)("pawn capture"),
      Option.when(passedPawnStopCandidate)("passed pawn stop"),
      Option.when(!ordinaryDirectOneSquarePawnBlock)("ordinary direct one-square pawn block")
    ).flatten.distinct

    PawnBlockProof(
      blockingSide = blockingSide,
      rivalSide = rivalSide,
      movingPieceBefore = movingPieceBefore,
      movingPieceAfter = movingPieceAfter,
      originSquare = movingPieceBefore.map(_.square),
      destinationSquare = movingPieceAfter.map(_.square),
      blockedPawn = blockedPawn,
      blockedPawnSquare = blockedPawn.map(_.square),
      blockedPawnNextAdvanceSquare = nextSquare,
      blockMove = Some(move),
      sameBoardProof = sameBoardProof,
      legalMove = legalMoveRow.nonEmpty,
      nextAdvanceSquareOccupiedAfter = nextAdvanceSquareOccupiedAfter,
      occupyingPieceBelongsToBlockingSide = occupyingPieceBelongsToBlockingSide,
      exactAfterBoardReplay = exactAfterBoardReplay,
      blockCreatedByMove = blockCreatedByMove,
      ordinaryDirectOneSquarePawnBlock = ordinaryDirectOneSquarePawnBlock,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("PawnBlockProof", missing))
    )

  private def nextAdvance(pawn: Piece): Option[Square] =
    val nextRank =
      if pawn.side == Side.White then pawn.square.rank + 1
      else pawn.square.rank - 1
    Option.when(nextRank >= 0 && nextRank <= 7)(Square(('a' + pawn.square.file).toChar, nextRank + 1))
