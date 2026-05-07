package lila.commentary.chess

private[commentary] enum DefenseTargetStatus:
  case TargetMovedAway
  case TargetGuarded
  case AttackerLineBlocked
  case AttackerCaptured

private[commentary] final case class DefenseProof(
    defendingSide: Side,
    defenseMove: Option[Line],
    defendedTarget: Option[Piece],
    originalThreat: ThreatProof,
    afterDefenseTargetStatus: Option[DefenseTargetStatus],
    materialLossPrevented: Option[Int],
    sameBoardProof: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean = missingEvidence.isEmpty

private[commentary] object DefenseProof:
  def fromBoardFacts(facts: BoardFacts, threat: ThreatProof, defenseLine: Line): DefenseProof =
    val defendingSide = BoardFacts.opposite(threat.rivalSide)
    val sameBoardProof = BoardFacts.sameBoardReady(facts) && threat.sameBoardProof
    val legalDefenseMove = facts.sideLegal.lines.contains(defenseLine)
    val piecesBySquare = facts.pieces.map(piece => piece.square -> piece).toMap
    val movingPiece =
      piecesBySquare
        .get(defenseLine.from)
        .filter(piece => legalDefenseMove && piece.side == defendingSide)
    val target = threat.threatenedTarget
    val attacker = threat.attackingPiece
    val afterPieces = movePieces(facts.pieces, movingPiece, defenseLine)
    val afterStatus =
      Vector(
        Option.when(targetMovesAway(target, attacker, movingPiece, defenseLine, afterPieces))(
          DefenseTargetStatus.TargetMovedAway
        ),
        Option.when(attackerCaptured(attacker, movingPiece, defenseLine))(DefenseTargetStatus.AttackerCaptured),
        Option.when(attackerLineBlocked(attacker, target, movingPiece, defenseLine))(DefenseTargetStatus.AttackerLineBlocked),
        Option.when(targetGuarded(target, afterPieces))(DefenseTargetStatus.TargetGuarded)
      ).flatten.headOption
    val materialAfterDefense = materialLossAfterDefense(threat, afterStatus, movingPiece, defenseLine, afterPieces)
    val materialLossPrevented =
      for
        originalLoss <- threat.materialLossIfUnanswered
        afterLoss <- materialAfterDefense
        if originalLoss > 0 && afterLoss <= 0
      yield originalLoss

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(!threat.complete)("complete ThreatProof"),
      Option.when(!legalDefenseMove)("legal defense move"),
      Option.when(movingPiece.isEmpty)("defending piece"),
      Option.when(target.isEmpty)("defended target"),
      Option.when(afterStatus.isEmpty)("after-defense target status"),
      Option.when(materialLossPrevented.isEmpty)("material loss prevented")
    ).flatten.distinct

    DefenseProof(
      defendingSide = defendingSide,
      defenseMove = Option.when(legalDefenseMove)(defenseLine),
      defendedTarget = target,
      originalThreat = threat,
      afterDefenseTargetStatus = Option.when(missing.isEmpty)(afterStatus).flatten,
      materialLossPrevented = materialLossPrevented,
      sameBoardProof = sameBoardProof,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("DefenseProof", missing))
    )

  private def targetMovesAway(
      target: Option[Piece],
      attacker: Option[Piece],
      movingPiece: Option[Piece],
      defenseLine: Line,
      afterPieces: Vector[Piece]
  ): Boolean =
    target.zip(attacker).zip(movingPiece).exists:
      case ((threatened, threateningPiece), moved) =>
        threatened == moved &&
          defenseLine.to != threatened.square &&
          !BoardFacts.attacksSquare(threateningPiece, defenseLine.to, occupiedMask(afterPieces))

  private def attackerCaptured(attacker: Option[Piece], movingPiece: Option[Piece], defenseLine: Line): Boolean =
    attacker.zip(movingPiece).exists: (threateningPiece, moved) =>
      moved.side != threateningPiece.side && defenseLine.to == threateningPiece.square

  private def attackerLineBlocked(
      attacker: Option[Piece],
      target: Option[Piece],
      movingPiece: Option[Piece],
      defenseLine: Line
  ): Boolean =
    attacker.zip(target).zip(movingPiece).exists:
      case ((threateningPiece, threatened), moved) =>
        moved != threatened &&
          moved.side == threatened.side &&
          slider(threateningPiece) &&
          BoardFacts.lineStep(threateningPiece.square, threatened.square).exists: step =>
            BoardFacts.squaresBetween(threateningPiece.square, threatened.square, step).contains(defenseLine.to)

  private def targetGuarded(target: Option[Piece], afterPieces: Vector[Piece]): Boolean =
    target.exists: threatened =>
      afterPieces.exists(piece => piece == threatened) &&
        afterPieces.exists: guard =>
          guard != threatened &&
            guard.side == threatened.side &&
            BoardFacts.attacksSquare(guard, threatened.square, occupiedMask(afterPieces))

  private def materialLossAfterDefense(
      threat: ThreatProof,
      status: Option[DefenseTargetStatus],
      movingPiece: Option[Piece],
      defenseLine: Line,
      afterPieces: Vector[Piece]
  ): Option[Int] =
    status match
      case Some(DefenseTargetStatus.TargetMovedAway) | Some(DefenseTargetStatus.AttackerLineBlocked) =>
        Some(0)
      case Some(DefenseTargetStatus.AttackerCaptured) =>
        Option.when(!equivalentRecaptureOnDefender(threat, movingPiece, defenseLine, afterPieces))(0)
      case Some(DefenseTargetStatus.TargetGuarded) =>
        for
          targetValue <- threat.targetValue
          attackerValue <- threat.attackingPiece.flatMap(pieceValue)
          target <- threat.threatenedTarget
          if targetGuarded(Some(target), afterPieces)
        yield targetValue - attackerValue
      case None => None

  private def equivalentRecaptureOnDefender(
      threat: ThreatProof,
      movingPiece: Option[Piece],
      defenseLine: Line,
      afterPieces: Vector[Piece]
  ): Boolean =
    movingPiece.exists: defender =>
      val occupied = occupiedMask(afterPieces)
      val defenderAfterMove = defender.copy(square = defenseLine.to)
      val capturedAttackerValue = threat.attackingPiece.flatMap(pieceValue).getOrElse(0)
      val defenderValue = pieceValue(defender).getOrElse(Int.MaxValue)
      defenderValue >= capturedAttackerValue &&
        afterPieces.exists: rival =>
          rival.side == threat.rivalSide &&
            BoardFacts.attacksSquare(rival, defenderAfterMove.square, occupied)

  private def movePieces(pieces: Vector[Piece], movingPiece: Option[Piece], defenseLine: Line): Vector[Piece] =
    movingPiece.fold(pieces): piece =>
      pieces
        .filterNot(existing => existing.square == defenseLine.from || existing.square == defenseLine.to) :+
        piece.copy(square = defenseLine.to)

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
