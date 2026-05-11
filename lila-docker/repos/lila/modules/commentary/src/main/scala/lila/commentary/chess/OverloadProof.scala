package lila.commentary.chess

private[commentary] final case class OverloadProof(
    beforeBoard: Vector[Piece],
    route: Option[Line],
    afterBoard: Vector[Piece],
    side: Side,
    rivalSide: Side,
    defender: Option[Piece],
    dutyTargets: Vector[Piece],
    target: Option[Piece],
    secondaryTarget: Option[Piece],
    anchor: Option[Piece],
    overloadMove: Option[Line],
    testedDutyTarget: Option[Piece],
    defenderDuty: Option[OverloadProof.DefenderDutyRelation],
    dualDefenderDuty: Option[OverloadProof.DualDefenderDutyRelation],
    overloadTest: Option[OverloadProof.OverloadTestRelation],
    cannotSatisfyBoth: Option[OverloadProof.CannotSatisfyBothRelation],
    replyMap: Vector[OverloadProof.ReplyPreservation],
    sameBoardProof: Boolean,
    legalMove: Boolean,
    sameBoardLegalReplay: Boolean,
    dualDutyDefender: Boolean,
    dutyTargetsNonKingMaterial: Boolean,
    testMoveAttacksDutyTargetAfter: Boolean,
    noReplyPreservesBothDutyTargets: Boolean,
    proofComplete: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean =
    proofComplete &&
      missingEvidence.isEmpty &&
      defenderDuty.exists(_.complete) &&
      dualDefenderDuty.exists(_.complete) &&
      overloadTest.exists(_.complete) &&
      cannotSatisfyBoth.exists(_.complete)
  def otherDutyTarget: Option[Piece] =
    testedDutyTarget.flatMap(tested => dutyTargets.find(_.square != tested.square))

private[commentary] object OverloadProof:
  final case class ReplyPreservation(reply: Line, preservesBothDutyTargets: Boolean)

  final case class DefenderDutyRelation(
      beforeBoard: Vector[Piece],
      side: Side,
      rivalSide: Side,
      defender: Option[Piece],
      target: Option[Piece],
      sameBoardProof: Boolean,
      defenderRivalOwned: Boolean,
      targetRivalOwned: Boolean,
      targetNonKingMaterial: Boolean,
      defenderGuardsTarget: Boolean,
      missingEvidence: Vector[BoardFacts.MissingEvidence]
  ):
    val publicClaimAllowed: Boolean = false
    def complete: Boolean = missingEvidence.isEmpty

  final case class DualDefenderDutyRelation(
      beforeBoard: Vector[Piece],
      side: Side,
      rivalSide: Side,
      defender: Option[Piece],
      dutyTargets: Vector[Piece],
      firstDuty: DefenderDutyRelation,
      secondDuty: DefenderDutyRelation,
      distinctDutyTargets: Boolean,
      dutyTargetsNonKingMaterial: Boolean,
      defenderGuardsBothTargets: Boolean,
      missingEvidence: Vector[BoardFacts.MissingEvidence]
  ):
    val publicClaimAllowed: Boolean = false
    def complete: Boolean = missingEvidence.isEmpty

  final case class OverloadTestRelation(
      beforeBoard: Vector[Piece],
      route: Option[Line],
      afterBoard: Vector[Piece],
      side: Side,
      rivalSide: Side,
      defender: Option[Piece],
      target: Option[Piece],
      secondaryTarget: Option[Piece],
      anchor: Option[Piece],
      dualDefenderDuty: DualDefenderDutyRelation,
      legalMove: Boolean,
      sameBoardLegalReplay: Boolean,
      dutyTargetsRemainAfterMove: Boolean,
      testedTargetIsDutyTarget: Boolean,
      testMoveAttacksDutyTargetAfter: Boolean,
      missingEvidence: Vector[BoardFacts.MissingEvidence]
  ):
    val publicClaimAllowed: Boolean = false
    def complete: Boolean = missingEvidence.isEmpty

  final case class CannotSatisfyBothRelation(
      beforeBoard: Vector[Piece],
      route: Option[Line],
      afterBoard: Vector[Piece],
      side: Side,
      rivalSide: Side,
      defender: Option[Piece],
      target: Option[Piece],
      secondaryTarget: Option[Piece],
      anchor: Option[Piece],
      overloadTest: OverloadTestRelation,
      replyMap: Vector[ReplyPreservation],
      noReplyPreservesBothDutyTargets: Boolean,
      missingEvidence: Vector[BoardFacts.MissingEvidence]
  ):
    val publicClaimAllowed: Boolean = false
    def complete: Boolean = missingEvidence.isEmpty

  final case class RelationBundle(
      defenderDuty: DefenderDutyRelation,
      dualDefenderDuty: DualDefenderDutyRelation,
      overloadTest: OverloadTestRelation,
      cannotSatisfyBoth: CannotSatisfyBothRelation
  )

  def observeRelations(
      facts: BoardFacts,
      overloadMove: Option[Line],
      defenderSquare: Option[Square],
      firstDutyTarget: Option[Square],
      secondDutyTarget: Option[Square],
      testedDutyTarget: Option[Square]
  ): RelationBundle =
    val beforeBoard = facts.pieces
    val legalMoveRow = overloadMove.flatMap(line => facts.seen.legalMoves.find(move => move.line == line))
    val side = legalMoveRow.map(_.side).getOrElse(facts.sideToMove)
    val beforeBySquare = beforeBoard.map(piece => piece.square -> piece).toMap
    val defender = defenderSquare.flatMap(beforeBySquare.get)
    val targetSquares = Vector(firstDutyTarget, secondDutyTarget).flatten.distinct
    val dutyTargets = targetSquares.flatMap(beforeBySquare.get)
    val rivalSide = defender.orElse(dutyTargets.headOption).map(_.side).getOrElse(BoardFacts.opposite(side))
    val testedTarget = testedDutyTarget.flatMap(beforeBySquare.get)
    val fallbackTarget = testedTarget.orElse(dutyTargets.headOption)
    val firstDuty = defenderDutyRelation(facts, side, rivalSide, defenderSquare, firstDutyTarget)
    val secondDuty = defenderDutyRelation(facts, side, rivalSide, defenderSquare, secondDutyTarget)
    val defenderDuty =
      if fallbackTarget.exists(target => firstDuty.target.exists(_.square == target.square)) then firstDuty
      else if fallbackTarget.exists(target => secondDuty.target.exists(_.square == target.square)) then secondDuty
      else firstDuty
    val dualDefenderDuty = dualDefenderDutyRelation(facts, side, rivalSide, firstDuty, secondDuty)
    val overloadTest =
      overloadTestRelation(facts, side, rivalSide, overloadMove, testedTarget, dualDefenderDuty)
    val cannotSatisfyBoth = cannotSatisfyBothRelation(facts, overloadMove, overloadTest)

    RelationBundle(
      defenderDuty = defenderDuty,
      dualDefenderDuty = dualDefenderDuty,
      overloadTest = overloadTest,
      cannotSatisfyBoth = cannotSatisfyBoth
    )

  def fromRelations(
      facts: BoardFacts,
      overloadMove: Option[Line],
      relations: RelationBundle
  ): OverloadProof =
    val beforeBoard = facts.pieces
    val relationBeforeBoards =
      Vector(
        relations.defenderDuty.beforeBoard,
        relations.dualDefenderDuty.beforeBoard,
        relations.overloadTest.beforeBoard,
        relations.cannotSatisfyBoth.beforeBoard
      )
    val relationAfterBoards =
      Vector(relations.overloadTest.afterBoard, relations.cannotSatisfyBoth.afterBoard)
    val boardMatchesRelations =
      relationBeforeBoards.forall(_ == beforeBoard) &&
        relationAfterBoards.distinct.size == 1
    val route = overloadMove.orElse(relations.overloadTest.route).orElse(relations.cannotSatisfyBoth.route)
    val legalMoveRow = route.flatMap(line => facts.seen.legalMoves.find(move => move.line == line))
    val routeAfterBoard = route.flatMap(line => BoardFacts.piecesAfterLegalMove(facts, line))
    val afterBoard = relations.overloadTest.afterBoard
    val routeMatchesRelations =
      relations.overloadTest.route == route &&
        relations.cannotSatisfyBoth.route == route
    val afterBoardMatchesLegalReplay =
      routeAfterBoard.exists(_ == afterBoard) &&
        relationAfterBoards.forall(_ == afterBoard)
    val legalMove = legalMoveRow.exists(_.side == facts.sideToMove)
    val sameBoardProof = BoardFacts.sameBoardReady(facts) && boardMatchesRelations
    val sameBoardLegalReplay =
      sameBoardProof && legalMove && routeMatchesRelations && afterBoardMatchesLegalReplay
    val side = legalMoveRow.map(_.side).getOrElse(relations.overloadTest.side)
    val rivalSide = relations.overloadTest.rivalSide
    val defender = relations.overloadTest.defender
    val target = relations.overloadTest.target
    val secondaryTarget = relations.overloadTest.secondaryTarget
    val dutyTargets =
      distinctBySquare(Vector(target, secondaryTarget).flatten ++ relations.dualDefenderDuty.dutyTargets)
    val beforeBySquare = beforeBoard.map(piece => piece.square -> piece).toMap
    val proofPiecesOnBoard =
      defender.exists(piece => beforeBySquare.get(piece.square).contains(piece)) &&
        dutyTargets.size == 2 &&
        dutyTargets.forall(piece => beforeBySquare.get(piece.square).contains(piece))
    val distinctNonKingMaterialTargets =
      dutyTargets.size == 2 &&
        dutyTargets.map(_.square).distinct.size == 2 &&
        dutyTargets.forall(piece => piece.side == rivalSide && piece.side != side && pieceValue(piece).nonEmpty)
    val testedTargetIsDutyTarget =
      target.exists(tested => dutyTargets.exists(_.square == tested.square))
    val noReplyPreservesBothDutyTargets =
      relations.cannotSatisfyBoth.noReplyPreservesBothDutyTargets &&
        relations.cannotSatisfyBoth.replyMap.nonEmpty &&
        !relations.cannotSatisfyBoth.replyMap.exists(_.preservesBothDutyTargets)

    val missing = Vector(
      Option.when(!relations.defenderDuty.complete)("complete DefenderDuty relation"),
      Option.when(!relations.dualDefenderDuty.complete)("complete DualDefenderDuty relation"),
      Option.when(!relations.overloadTest.complete)("complete OverloadTest relation"),
      Option.when(!relations.cannotSatisfyBoth.complete)("complete CannotSatisfyBoth relation"),
      Option.when(!boardMatchesRelations)("board mismatch"),
      Option.when(!sameBoardLegalReplay)("same-board legal replay"),
      Option.when(!proofPiecesOnBoard)("defender and duty targets on exact proof board"),
      Option.when(!distinctNonKingMaterialTargets)("distinct non-king material duty targets"),
      Option.when(!testedTargetIsDutyTarget)("tested duty target is one of the two duty targets"),
      Option.when(!noReplyPreservesBothDutyTargets)("no legal rival reply preserves both duty targets")
    ).flatten.distinct

    val allMissing =
      (
        relations.defenderDuty.missingEvidence ++
          relations.dualDefenderDuty.missingEvidence ++
          relations.overloadTest.missingEvidence ++
          relations.cannotSatisfyBoth.missingEvidence ++
          Option.when(missing.nonEmpty)(BoardFacts.MissingEvidence("OverloadProof", missing)).toVector
      ).filter(_.missing.nonEmpty)
    val proofComplete = allMissing.isEmpty

    OverloadProof(
      beforeBoard = beforeBoard,
      route = route,
      afterBoard = afterBoard,
      side = side,
      rivalSide = rivalSide,
      defender = defender,
      dutyTargets = dutyTargets,
      target = target,
      secondaryTarget = secondaryTarget,
      anchor = relations.overloadTest.anchor,
      overloadMove = route,
      testedDutyTarget = target,
      defenderDuty = Some(relations.defenderDuty),
      dualDefenderDuty = Some(relations.dualDefenderDuty),
      overloadTest = Some(relations.overloadTest),
      cannotSatisfyBoth = Some(relations.cannotSatisfyBoth),
      replyMap = relations.cannotSatisfyBoth.replyMap,
      sameBoardProof = sameBoardProof,
      legalMove = legalMove,
      sameBoardLegalReplay = sameBoardLegalReplay,
      dualDutyDefender = relations.dualDefenderDuty.defenderGuardsBothTargets,
      dutyTargetsNonKingMaterial = distinctNonKingMaterialTargets,
      testMoveAttacksDutyTargetAfter = relations.overloadTest.testMoveAttacksDutyTargetAfter,
      noReplyPreservesBothDutyTargets = noReplyPreservesBothDutyTargets,
      proofComplete = proofComplete,
      missingEvidence = allMissing
    )

  private def defenderDutyRelation(
      facts: BoardFacts,
      side: Side,
      rivalSide: Side,
      defenderSquare: Option[Square],
      targetSquare: Option[Square]
  ): DefenderDutyRelation =
    val beforeBoard = facts.pieces
    val beforeBySquare = beforeBoard.map(piece => piece.square -> piece).toMap
    val defender = defenderSquare.flatMap(beforeBySquare.get)
    val target = targetSquare.flatMap(beforeBySquare.get)
    val defenderRivalOwned = defender.exists(piece => piece.side == rivalSide && piece.side != side)
    val targetRivalOwned = target.exists(piece => piece.side == rivalSide && piece.side != side)
    val targetNonKingMaterial = target.exists(piece => targetRivalOwned && pieceValue(piece).nonEmpty)
    val defenderGuardsTarget =
      defender.exists(guard => target.exists(t => facts.seen.guards.exists(row => row.guard == guard && row.target == t)))

    val missing = Vector(
      Option.when(!BoardFacts.sameBoardReady(facts))("same-board proof"),
      Option.when(defender.isEmpty)("defender identity"),
      Option.when(target.isEmpty)("duty target identity"),
      Option.when(!defenderRivalOwned)("defender rival-owned"),
      Option.when(!targetRivalOwned)("target rival-owned"),
      Option.when(!targetNonKingMaterial)("target non-king material"),
      Option.when(!defenderGuardsTarget)("defender guards duty target")
    ).flatten.distinct

    DefenderDutyRelation(
      beforeBoard = beforeBoard,
      side = side,
      rivalSide = rivalSide,
      defender = defender,
      target = target,
      sameBoardProof = BoardFacts.sameBoardReady(facts),
      defenderRivalOwned = defenderRivalOwned,
      targetRivalOwned = targetRivalOwned,
      targetNonKingMaterial = targetNonKingMaterial,
      defenderGuardsTarget = defenderGuardsTarget,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("DefenderDuty", missing))
    )

  private def dualDefenderDutyRelation(
      facts: BoardFacts,
      side: Side,
      rivalSide: Side,
      firstDuty: DefenderDutyRelation,
      secondDuty: DefenderDutyRelation
  ): DualDefenderDutyRelation =
    val dutyTargets = distinctBySquare(Vector(firstDuty.target, secondDuty.target).flatten)
    val defender = firstDuty.defender.filter(piece => secondDuty.defender.exists(_.square == piece.square))
    val distinctDutyTargets = dutyTargets.size == 2
    val dutyTargetsNonKingMaterial =
      distinctDutyTargets && dutyTargets.forall(piece => piece.side == rivalSide && piece.side != side && pieceValue(piece).nonEmpty)
    val defenderGuardsBothTargets =
      defender.nonEmpty && firstDuty.defenderGuardsTarget && secondDuty.defenderGuardsTarget

    val missing = Vector(
      Option.when(!firstDuty.complete)("complete first DefenderDuty relation"),
      Option.when(!secondDuty.complete)("complete second DefenderDuty relation"),
      Option.when(defender.isEmpty)("same defender"),
      Option.when(!distinctDutyTargets)("two distinct duty targets"),
      Option.when(!dutyTargetsNonKingMaterial)("duty targets non-king material"),
      Option.when(!defenderGuardsBothTargets)("defender guards both duty targets")
    ).flatten.distinct

    DualDefenderDutyRelation(
      beforeBoard = facts.pieces,
      side = side,
      rivalSide = rivalSide,
      defender = defender,
      dutyTargets = dutyTargets,
      firstDuty = firstDuty,
      secondDuty = secondDuty,
      distinctDutyTargets = distinctDutyTargets,
      dutyTargetsNonKingMaterial = dutyTargetsNonKingMaterial,
      defenderGuardsBothTargets = defenderGuardsBothTargets,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("DualDefenderDuty", missing))
    )

  private def overloadTestRelation(
      facts: BoardFacts,
      side: Side,
      rivalSide: Side,
      overloadMove: Option[Line],
      testedTarget: Option[Piece],
      dualDuty: DualDefenderDutyRelation
  ): OverloadTestRelation =
    val legalMoveRow = overloadMove.flatMap(line => facts.seen.legalMoves.find(move => move.line == line))
    val legalMove = legalMoveRow.exists(_.side == facts.sideToMove)
    val afterMovePieces = overloadMove.flatMap(line => BoardFacts.piecesAfterLegalMove(facts, line))
    val afterBoard = afterMovePieces.getOrElse(Vector.empty)
    val afterMoveBySquare = afterMovePieces.map(_.map(piece => piece.square -> piece).toMap)
    val target = testedTarget
    val secondaryTarget = target.flatMap(tested => dualDuty.dutyTargets.find(_.square != tested.square))
    val testedTargetIsDutyTarget = target.exists(tested => dualDuty.dutyTargets.exists(_.square == tested.square))
    val dutyTargetsRemainAfterMove =
      afterMoveBySquare.exists: bySquare =>
        dualDuty.dutyTargets.size == 2 &&
          dualDuty.dutyTargets.forall(target => bySquare.get(target.square).exists(samePiece(_, target)))
    val movedAttackerAfter =
      for
        move <- overloadMove
        bySquare <- afterMoveBySquare
        piece <- bySquare.get(move.to)
        if piece.side == side
      yield piece
    val testMoveAttacksDutyTargetAfter =
      for
        attacker <- movedAttackerAfter
        t <- target
        bySquare <- afterMoveBySquare
      yield BoardFacts.attacksSquare(attacker, t.square, occupiedMask(bySquare.values.toVector))
    val sameBoardLegalReplay =
      BoardFacts.sameBoardReady(facts) && legalMove && afterMovePieces.nonEmpty

    val missing = Vector(
      Option.when(!dualDuty.complete)("complete DualDefenderDuty relation"),
      Option.when(!legalMove)("legal move route"),
      Option.when(afterMovePieces.isEmpty)("afterBoard"),
      Option.when(!sameBoardLegalReplay)("same-board legal replay"),
      Option.when(target.isEmpty || !testedTargetIsDutyTarget)("tested duty target"),
      Option.when(secondaryTarget.isEmpty)("secondary duty target"),
      Option.when(!dutyTargetsRemainAfterMove)("duty targets remain after test move"),
      Option.when(!testMoveAttacksDutyTargetAfter.getOrElse(false))("test move attacks duty target after move")
    ).flatten.distinct

    OverloadTestRelation(
      beforeBoard = facts.pieces,
      route = overloadMove,
      afterBoard = afterBoard,
      side = side,
      rivalSide = rivalSide,
      defender = dualDuty.defender,
      target = target,
      secondaryTarget = secondaryTarget,
      anchor = dualDuty.defender,
      dualDefenderDuty = dualDuty,
      legalMove = legalMove,
      sameBoardLegalReplay = sameBoardLegalReplay,
      dutyTargetsRemainAfterMove = dutyTargetsRemainAfterMove,
      testedTargetIsDutyTarget = testedTargetIsDutyTarget,
      testMoveAttacksDutyTargetAfter = testMoveAttacksDutyTargetAfter.getOrElse(false),
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("OverloadTest", missing))
    )

  private def cannotSatisfyBothRelation(
      facts: BoardFacts,
      overloadMove: Option[Line],
      overloadTest: OverloadTestRelation
  ): CannotSatisfyBothRelation =
    val replyMap =
      overloadMove.toVector.flatMap: move =>
        BoardFacts.rivalLegalRepliesAfter(facts, move).map: reply =>
          ReplyPreservation(
            reply = reply.line,
            preservesBothDutyTargets =
              replyPreservesBothDutyTargets(reply, overloadTest.defender, overloadTest.dualDefenderDuty.dutyTargets)
          )
    val noReplyPreservesBothDutyTargets =
      replyMap.nonEmpty && !replyMap.exists(_.preservesBothDutyTargets)

    val missing = Vector(
      Option.when(!overloadTest.complete)("complete OverloadTest relation"),
      Option.when(replyMap.isEmpty)("rival legal reply map"),
      Option.when(!noReplyPreservesBothDutyTargets)("no legal rival reply preserves both duty targets")
    ).flatten.distinct

    CannotSatisfyBothRelation(
      beforeBoard = facts.pieces,
      route = overloadMove,
      afterBoard = overloadTest.afterBoard,
      side = overloadTest.side,
      rivalSide = overloadTest.rivalSide,
      defender = overloadTest.defender,
      target = overloadTest.target,
      secondaryTarget = overloadTest.secondaryTarget,
      anchor = overloadTest.anchor,
      overloadTest = overloadTest,
      replyMap = replyMap,
      noReplyPreservesBothDutyTargets = noReplyPreservesBothDutyTargets,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("CannotSatisfyBoth", missing))
    )

  private def replyPreservesBothDutyTargets(
      reply: BoardFacts.LegalReply,
      defender: Option[Piece],
      dutyTargets: Vector[Piece]
  ): Boolean =
    defender.exists: originalDefender =>
      val bySquare = reply.pieces.map(piece => piece.square -> piece).toMap
      val defenderAfter =
        if reply.line.from == originalDefender.square then bySquare.get(reply.line.to)
        else bySquare.get(originalDefender.square)
      val targetsAfter =
        dutyTargets.flatMap(target => bySquare.get(target.square).filter(samePiece(_, target)))
      defenderAfter.exists: guard =>
        targetsAfter.size == 2 &&
          guard.side == originalDefender.side &&
          guard.man == originalDefender.man &&
          targetsAfter.forall(target => BoardFacts.attacksSquare(guard, target.square, occupiedMask(reply.pieces)))

  private def samePiece(candidate: Piece, original: Piece): Boolean =
    candidate.side == original.side && candidate.man == original.man && candidate.square == original.square

  private def distinctBySquare(pieces: Vector[Piece]): Vector[Piece] =
    pieces.foldLeft(Vector.empty[Piece]): (acc, piece) =>
      if acc.exists(_.square == piece.square) then acc else acc :+ piece

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
