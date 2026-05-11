package lila.commentary.chess

private[commentary] final case class MultiTargetProof(
    side: Side,
    attacker: Option[Piece],
    attackerAfterMove: Option[Piece],
    forkMove: Option[Line],
    targets: Vector[Piece],
    targetValues: Vector[Int],
    attackedTargetSquaresAfterMove: Vector[Square],
    replyMap: Vector[MultiTargetProof.TargetReply],
    materialOrTempoResult: Option[Int],
    sameBoardProof: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty
  def forkSquare: Option[Square] = forkMove.map(_.to)
  def targetA: Option[Piece] = targets.headOption
  def targetB: Option[Piece] = targets.lift(1)
  def targetSquares: Vector[Square] = targets.map(_.square)

private[commentary] object MultiTargetProof:
  final case class TargetReply(target: Piece, savedByOneReply: Boolean)

  def fromBoardFacts(
      facts: BoardFacts,
      forkMove: Option[Line],
      firstTarget: Option[Square],
      secondTarget: Option[Square]
  ): MultiTargetProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMove = forkMove.flatMap(line => facts.seen.legalMoves.find(_.line == line))
    val afterPieces = forkMove.flatMap(line => BoardFacts.piecesAfterLegalMove(facts, line))
    val side = legalMove.map(_.side).getOrElse(facts.sideToMove)
    val piecesBySquare = facts.pieces.map(piece => piece.square -> piece).toMap
    val attacker =
      for
        move <- forkMove
        piece <- piecesBySquare.get(move.from)
        legal <- legalMove
        if legal.piece == piece && legal.side == piece.side
      yield piece
    val attackerAfterMove =
      for
        move <- forkMove
        piece <- attacker
      yield Piece(piece.side, piece.man, move.to)
    val pawnAttacker = attacker.exists(_.man == Man.Pawn)
    val pawnAfterRouteDestination =
      !pawnAttacker || attacker.zip(forkMove).exists: (piece, move) =>
        afterPieces.exists(_.contains(Piece(piece.side, Man.Pawn, move.to)))
    val pawnMoveNonPromotion =
      !pawnAttacker || attacker.zip(forkMove).exists: (piece, move) =>
        !promotionSquare(piece.side, move.to)
    val pawnMoveNotEnPassant =
      !pawnAttacker || attacker.zip(forkMove).exists: (_, move) =>
        move.from.file == move.to.file || piecesBySquare.get(move.to).nonEmpty
    val duplicateTargets =
      firstTarget.zip(secondTarget).exists((first, second) => first == second)
    val namedTargets = Vector(firstTarget, secondTarget)
    val targetPieces =
      namedTargets.flatMap(_.flatMap(piecesBySquare.get))
    val targetValues = targetPieces.flatMap(pieceValue)
    val targetEnemies =
      targetPieces.size == 2 && attacker.exists(attackingPiece => targetPieces.forall(_.side != attackingPiece.side))
    val targetsImportant =
      targetValues.size == 2 && targetValues.forall(_ >= 300)
    val occupiedAfter = forkMove.fold(facts.pieces): move =>
      facts.pieces
        .filterNot(piece => piece.square == move.from || piece.square == move.to) ++
        attackerAfterMove.toVector
    val occupiedAfterMask = occupiedAfter.foldLeft(0L): (mask, piece) =>
      mask | piece.square.bit
    val attackedTargetSquaresAfterMove =
      attackerAfterMove.fold(Vector.empty): moved =>
        targetPieces
          .filter(target => BoardFacts.attacksSquare(moved, target.square, occupiedAfterMask))
          .map(_.square)
    val targetsAttackedAfterMove =
      targetPieces.size == 2 &&
        targetPieces.forall(target => attackedTargetSquaresAfterMove.contains(target.square))
    val forkSquareUnsafe =
      attackerAfterMove.exists: moved =>
        occupiedAfter.exists: piece =>
          piece.side != moved.side &&
            BoardFacts.attacksSquare(piece, moved.square, occupiedAfterMask)
    val replyMap =
      if targetsAttackedAfterMove && !forkSquareUnsafe && targetEnemies && targetsImportant then
        targetPieces.map(target => TargetReply(target, savedByOneReply = false))
      else Vector.empty
    val materialOrTempoResult =
      Option.when(replyMap.nonEmpty)(targetValues.min)

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(forkMove.isEmpty)("fork square"),
      Option.when(legalMove.isEmpty)("legal move"),
      Option.when(attacker.isEmpty)("attacker"),
      Option.when(pawnAttacker && !pawnAfterRouteDestination)("pawn after route destination"),
      Option.when(pawnAttacker && !pawnMoveNonPromotion)("pawn move is non-promotion"),
      Option.when(pawnAttacker && !pawnMoveNotEnPassant)("not en passant"),
      Option.when(firstTarget.isEmpty || secondTarget.isEmpty)("two targets"),
      Option.when(duplicateTargets)("distinct targets"),
      Option.when(targetPieces.size != 2)("target pieces"),
      Option.when(!targetEnemies)("enemy targets"),
      Option.when(!targetsImportant)("target importance"),
      Option.when(!targetsAttackedAfterMove)("target attacks after move"),
      Option.when(forkSquareUnsafe)("bounded reply map")
    ).flatten.distinct

    MultiTargetProof(
      side = side,
      attacker = attacker,
      attackerAfterMove = attackerAfterMove,
      forkMove = forkMove,
      targets = targetPieces,
      targetValues = targetValues,
      attackedTargetSquaresAfterMove = attackedTargetSquaresAfterMove,
      replyMap = replyMap,
      materialOrTempoResult = materialOrTempoResult,
      sameBoardProof = sameBoardProof,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("MultiTargetProof", missing))
    )

  private def pieceValue(piece: Piece): Option[Int] =
    piece.man match
      case Man.Pawn   => Some(100)
      case Man.Knight => Some(320)
      case Man.Bishop => Some(330)
      case Man.Rook   => Some(500)
      case Man.Queen  => Some(900)
      case Man.King   => None

  private def promotionSquare(side: Side, square: Square): Boolean =
    (side == Side.White && square.rank == 7) || (side == Side.Black && square.rank == 0)
