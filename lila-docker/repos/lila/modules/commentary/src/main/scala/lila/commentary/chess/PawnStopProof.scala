package lila.commentary.chess

private[commentary] enum PawnStopKind:
  case NextSquareOccupied
  case NextSquareAttacked
  case NextSquareControlledByPawn

private[commentary] final case class PawnStopProof(
    side: Side,
    rivalSide: Side,
    targetPawn: Option[Piece],
    nextAdvanceSquare: Option[Square],
    stoppingPieceBefore: Option[Piece],
    stoppingPieceAfter: Option[Piece],
    stopMove: Option[Line],
    stopKind: Option[PawnStopKind],
    sameBoardProof: Boolean,
    legalStopMove: Boolean,
    targetPawnAlreadyPassed: Boolean,
    nextAdvanceSquareNonPromotion: Boolean,
    nextAdvanceSquareEmptyBefore: Boolean,
    nextAdvanceSquareOccupiedAfter: Boolean,
    nextAdvanceSquareAttackedAfter: Boolean,
    nextAdvanceSquareControlledByPawnAfter: Boolean,
    exactAfterBoardReplay: Boolean,
    targetPawnStillPresentAfter: Boolean,
    nextAdvanceSquareStoppedAfter: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object PawnStopProof:
  def fromBoardFacts(facts: BoardFacts, stopMove: Line): PawnStopProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMove = facts.seen.legalMoves.find(_.line == stopMove)
    val side = legalMove.map(_.side).getOrElse(facts.sideToMove)
    val rivalSide = BoardFacts.opposite(side)
    val bySquare = facts.pieces.map(piece => piece.square -> piece).toMap
    val stoppingPieceBefore =
      bySquare
        .get(stopMove.from)
        .filter(piece => piece.side == side)
        .filter(piece => legalMove.exists(_.piece == piece))
    val stoppingPieceAfter = stoppingPieceBefore.map(_.copy(square = stopMove.to))
    val afterPieces =
      if legalMove.nonEmpty && stoppingPieceBefore.nonEmpty then
        facts.pieces.filterNot(piece => piece.square == stopMove.from || piece.square == stopMove.to) ++
          stoppingPieceAfter.toVector
      else facts.pieces
    val targetCandidates: Vector[(Piece, Square, Option[PawnStopKind])] =
      facts.seen.passedPawnObservations
        .filter(_.side == rivalSide)
        .flatMap: row =>
          nextAdvance(row.pawn).flatMap: square =>
            Option.when(isNextAdvanceSquareEmptyBefore(square, bySquare)):
              (row.pawn, square, stopKind(stoppingPieceAfter, square, afterPieces))
    val targetAndKind =
      targetCandidates.collectFirst:
        case (pawn, square, Some(kind)) => (pawn, square, Some(kind))
      .orElse(targetCandidates.headOption)
    val candidateTargetPawn = targetAndKind.map(_._1)
    val nextSquare = targetAndKind.map(_._2)
    val kind = targetAndKind.flatMap(_._3)
    val legalStopMove =
      legalMove.nonEmpty &&
        stoppingPieceBefore.nonEmpty &&
        kind.nonEmpty
    val targetPawnAlreadyPassed =
      candidateTargetPawn.exists: pawn =>
        facts.seen.passedPawnObservations.exists(row => row.side == pawn.side && row.pawn == pawn)
    val nextAdvanceSquareNonPromotion =
      candidateTargetPawn.zip(nextSquare).exists: (pawn, square) =>
          (pawn.side == Side.White && square.rank < 7) ||
          (pawn.side == Side.Black && square.rank > 0)
    val nextAdvanceSquareEmptyBefore =
      nextSquare.exists(square => isNextAdvanceSquareEmptyBefore(square, bySquare))
    val nextAdvanceSquareOccupiedAfter =
      kind.contains(PawnStopKind.NextSquareOccupied)
    val nextAdvanceSquareAttackedAfter =
      kind.contains(PawnStopKind.NextSquareAttacked)
    val nextAdvanceSquareControlledByPawnAfter =
      kind.contains(PawnStopKind.NextSquareControlledByPawn)
    val exactAfterBoardReplay =
      sameBoardProof &&
        legalStopMove &&
        nextAdvanceSquareEmptyBefore &&
        stoppingPieceAfter.exists(afterPiece => afterPieces.exists(_ == afterPiece)) &&
        !afterPieces.exists(_.square == stopMove.from)
    val targetPawnStillPresentAfter =
      exactAfterBoardReplay &&
        candidateTargetPawn.exists(pawn => afterPieces.exists(_ == pawn))
    val nextAdvanceSquareStoppedAfter =
      exactAfterBoardReplay &&
        kind.nonEmpty

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(stoppingPieceBefore.isEmpty)("stopping piece identity"),
      Option.when(candidateTargetPawn.isEmpty)("already-passed target pawn"),
      Option.when(nextSquare.isEmpty)("target pawn next advance square"),
      Option.when(!legalStopMove)("legal stop move"),
      Option.when(!targetPawnAlreadyPassed)("target pawn already passed"),
      Option.when(!nextAdvanceSquareNonPromotion)("next advance square is non-promotion"),
      Option.when(!nextAdvanceSquareEmptyBefore)("next advance square empty before move"),
      Option.when(kind.isEmpty)("next advance square stop kind"),
      Option.when(!exactAfterBoardReplay)("exact after-board replay"),
      Option.when(!targetPawnStillPresentAfter)("target pawn still present after move"),
      Option.when(!nextAdvanceSquareStoppedAfter)("next advance square stopped after move")
    ).flatten.distinct

    PawnStopProof(
      side = side,
      rivalSide = rivalSide,
      targetPawn = candidateTargetPawn,
      nextAdvanceSquare = nextSquare,
      stoppingPieceBefore = stoppingPieceBefore,
      stoppingPieceAfter = stoppingPieceAfter,
      stopMove = Some(stopMove),
      stopKind = kind,
      sameBoardProof = sameBoardProof,
      legalStopMove = legalStopMove,
      targetPawnAlreadyPassed = targetPawnAlreadyPassed,
      nextAdvanceSquareNonPromotion = nextAdvanceSquareNonPromotion,
      nextAdvanceSquareEmptyBefore = nextAdvanceSquareEmptyBefore,
      nextAdvanceSquareOccupiedAfter = nextAdvanceSquareOccupiedAfter,
      nextAdvanceSquareAttackedAfter = nextAdvanceSquareAttackedAfter,
      nextAdvanceSquareControlledByPawnAfter = nextAdvanceSquareControlledByPawnAfter,
      exactAfterBoardReplay = exactAfterBoardReplay,
      targetPawnStillPresentAfter = targetPawnStillPresentAfter,
      nextAdvanceSquareStoppedAfter = nextAdvanceSquareStoppedAfter,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("PawnStopProof", missing))
    )

  private def nextAdvance(pawn: Piece): Option[Square] =
    val nextRank =
      if pawn.side == Side.White then pawn.square.rank + 1
      else if pawn.side == Side.Black then pawn.square.rank - 1
      else pawn.square.rank
    Option.when(nextRank >= 0 && nextRank <= 7)(Square(('a' + pawn.square.file).toChar, nextRank + 1))

  private def isNextAdvanceSquareEmptyBefore(square: Square, bySquare: Map[Square, Piece]): Boolean =
    !bySquare.contains(square)

  private def stopKind(stoppingPieceAfter: Option[Piece], square: Square, pieces: Vector[Piece]): Option[PawnStopKind] =
    stoppingPieceAfter.flatMap: piece =>
      if piece.square == square then Some(PawnStopKind.NextSquareOccupied)
      else if piece.man == Man.Pawn && attacks(piece, square, pieces) then
        Some(PawnStopKind.NextSquareControlledByPawn)
      else if attacks(piece, square, pieces) then Some(PawnStopKind.NextSquareAttacked)
      else None

  private def attacks(piece: Piece, target: Square, pieces: Vector[Piece]): Boolean =
    val df = target.file - piece.square.file
    val dr = target.rank - piece.square.rank
    piece.man match
      case Man.Pawn =>
        math.abs(df) == 1 &&
          ((piece.side == Side.White && dr == 1) || (piece.side == Side.Black && dr == -1))
      case Man.Knight =>
        (math.abs(df) == 1 && math.abs(dr) == 2) || (math.abs(df) == 2 && math.abs(dr) == 1)
      case Man.Bishop =>
        math.abs(df) == math.abs(dr) && clearRay(piece.square, target, pieces)
      case Man.Rook =>
        (df == 0 || dr == 0) && clearRay(piece.square, target, pieces)
      case Man.Queen =>
        (df == 0 || dr == 0 || math.abs(df) == math.abs(dr)) && clearRay(piece.square, target, pieces)
      case Man.King =>
        math.max(math.abs(df), math.abs(dr)) == 1

  private def clearRay(from: Square, to: Square, pieces: Vector[Piece]): Boolean =
    val df = Integer.signum(to.file - from.file)
    val dr = Integer.signum(to.rank - from.rank)
    if df == 0 && dr == 0 then false
    else
      Iterator
        .iterate((from.file + df, from.rank + dr))((file, rank) => (file + df, rank + dr))
        .takeWhile((file, rank) => file != to.file || rank != to.rank)
        .forall((file, rank) => !pieces.exists(piece => piece.square.file == file && piece.square.rank == rank))
