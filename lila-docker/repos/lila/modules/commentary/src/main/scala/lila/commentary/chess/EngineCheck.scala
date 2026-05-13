package lila.commentary.chess

private[commentary] final case class EngineEval(centipawns: Int)

private[commentary] final case class EngineLine(moves: Vector[Line]):
  require(moves.nonEmpty, "EngineLine requires at least one move")

private[commentary] enum EngineCheckStatus:
  case Unknown
  case Supports
  case Caps
  case Refutes

private[commentary] final case class EngineCheck(
    sameBoardProof: Boolean,
    checkedMove: Option[Line],
    engineLine: Option[EngineLine],
    replyLine: Option[EngineLine],
    evalBefore: Option[EngineEval],
    evalAfter: Option[EngineEval],
    depth: Option[Int],
    freshnessPly: Option[Int],
    status: EngineCheckStatus,
    missingEvidence: Vector[BoardFacts.MissingEvidence],
    storyBound: Boolean
):
  val publicClaimAllowed: Boolean = false
  def evidenceReady: Boolean = missingEvidence.isEmpty

private[commentary] object EngineCheck:
  private val RefuteEvalDropCentipawns = 150

  def fromStory(
      facts: BoardFacts,
      story: Option[Story],
      engineLine: Option[EngineLine],
      replyLine: Option[EngineLine],
      evalBefore: Option[EngineEval],
      evalAfter: Option[EngineEval],
      depth: Option[Int],
      freshnessPly: Option[Int],
      requestedStatus: EngineCheckStatus = EngineCheckStatus.Supports
  ): EngineCheck =
    val checkedMove = story.flatMap(_.route)
    val storyBound = story.exists(storyIdentityOnFacts(facts, _))
    val sameBoardProof = BoardFacts.sameBoardReady(facts) && storyBound
    val engineStartsWithStoryRoute =
      checkedMove
        .zip(engineLine)
        .forall((route, line) => line.moves.headOption.contains(route))
    val sameLegalLine =
      checkedMove.forall(line =>
        facts.sideLegal.lines.contains(line) || facts.rivalLegal.lines.contains(line)
      )
    val missing = Vector(
      Option.when(story.isEmpty)("Story"),
      Option.when(story.exists(_.route.isEmpty))("Story route"),
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(!sameLegalLine)("same legal line"),
      Option.when(!engineStartsWithStoryRoute)("same Story route"),
      Option.when(engineLine.isEmpty)("engine line"),
      Option.when(!engineLineBindsCheckedMove(engineLine, checkedMove))("checked move in engine line"),
      Option.when(replyLine.isEmpty)("reply line"),
      Option.when(evalBefore.isEmpty)("eval before"),
      Option.when(evalAfter.isEmpty)("eval after"),
      Option.when(!depthOrFreshnessPresent(depth, freshnessPly))("depth or freshness"),
      Option.when(freshnessPly.exists(_ > 0))("fresh engine evidence")
    ).flatten

    EngineCheck(
      sameBoardProof = sameBoardProof,
      checkedMove = checkedMove,
      engineLine = engineLine,
      replyLine = replyLine,
      evalBefore = evalBefore,
      evalAfter = evalAfter,
      depth = depth,
      freshnessPly = freshnessPly,
      status =
        if missing.isEmpty then checkedStatus(requestedStatus, evalBefore, evalAfter)
        else EngineCheckStatus.Unknown,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("EngineCheck", missing)),
      storyBound = storyBound
    )

  def fromEvidence(
      sameBoardProof: Boolean,
      checkedMove: Option[Line],
      engineLine: Option[EngineLine],
      replyLine: Option[EngineLine],
      evalBefore: Option[EngineEval],
      evalAfter: Option[EngineEval],
      depth: Option[Int],
      freshnessPly: Option[Int],
      requestedStatus: EngineCheckStatus = EngineCheckStatus.Unknown
  ): EngineCheck =
    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(checkedMove.isEmpty)("checked move"),
      Option.when(engineLine.isEmpty)("engine line"),
      Option.when(!engineLineBindsCheckedMove(engineLine, checkedMove))("checked move in engine line"),
      Option.when(replyLine.isEmpty)("reply line"),
      Option.when(evalBefore.isEmpty)("eval before"),
      Option.when(evalAfter.isEmpty)("eval after"),
      Option.when(!depthOrFreshnessPresent(depth, freshnessPly))("depth or freshness"),
      Option.when(freshnessPly.exists(_ > 0))("fresh engine evidence")
    ).flatten

    EngineCheck(
      sameBoardProof = sameBoardProof,
      checkedMove = checkedMove,
      engineLine = engineLine,
      replyLine = replyLine,
      evalBefore = evalBefore,
      evalAfter = evalAfter,
      depth = depth,
      freshnessPly = freshnessPly,
      status = if missing.isEmpty then requestedStatus else EngineCheckStatus.Unknown,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("EngineCheck", missing)),
      storyBound = false
    )

  private def engineLineBindsCheckedMove(engineLine: Option[EngineLine], checkedMove: Option[Line]): Boolean =
    engineLine.zip(checkedMove).forall((line, move) => line.moves.headOption.contains(move))

  private def depthOrFreshnessPresent(depth: Option[Int], freshnessPly: Option[Int]): Boolean =
    depth.exists(_ > 0) || freshnessPly.contains(0)

  private def storyIdentityOnFacts(facts: BoardFacts, story: Story): Boolean =
    val pieces = facts.pieces.toSet
    story.writer match
      case Some(StoryWriter.TacticHanging) =>
        story.tactic.contains(Tactic.Hanging) &&
        story.proofFailures.isEmpty &&
        story.captureResult.exists: result =>
          result.sameBoardProof &&
            result.missingEvidence.isEmpty &&
            story.route.contains(result.captureLine) &&
            result.capturingPiece.exists(pieces.contains) &&
            result.targetPiece.exists(pieces.contains)
      case Some(StoryWriter.TacticFork) =>
        story.tactic.contains(Tactic.Fork) &&
        story.proofFailures.isEmpty &&
        story.multiTargetProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.forkMove.exists(move => story.route.contains(move)) &&
            proof.attacker.exists(pieces.contains) &&
            proof.targets.forall(pieces.contains)
      case Some(StoryWriter.SceneMaterial) =>
        story.scene == Scene.Material &&
        story.tactic.isEmpty &&
        story.proofFailures.isEmpty &&
        story.captureResult.exists: result =>
          result.sameBoardProof &&
            result.missingEvidence.isEmpty &&
            story.route.contains(result.captureLine) &&
            result.capturingPiece.exists(pieces.contains) &&
            result.targetPiece.exists(pieces.contains)
      case Some(StoryWriter.SceneDefense) =>
        story.scene == Scene.Defense &&
        story.tactic.isEmpty &&
        story.proofFailures.isEmpty &&
        story.threatProof.exists: threat =>
          threat.complete &&
            threat.sameBoardProof &&
            threat.legalThreatLine.exists(line => facts.rivalLegal.lines.contains(line)) &&
            threat.attackingPiece.exists(pieces.contains) &&
            threat.threatenedTarget.exists(pieces.contains) &&
            story.defenseProof.exists: defense =>
              defense.complete &&
                defense.sameBoardProof &&
                defense.defenseMove.exists(move => story.route.contains(move)) &&
                defense.defenseMove.exists(move => facts.sideLegal.lines.contains(move)) &&
                defense.defendedTarget.exists(pieces.contains)
      case Some(StoryWriter.TacticDiscoveredAttack) =>
        story.scene == Scene.Tactic &&
        story.tactic.contains(Tactic.DiscoveredAttack) &&
        story.proofFailures.isEmpty &&
        story.lineProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.revealingMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.movedPiece.exists(pieces.contains) &&
            proof.slider.exists(pieces.contains) &&
            proof.revealedTarget.exists(pieces.contains)
      case Some(StoryWriter.TacticPin) =>
        story.scene == Scene.Tactic &&
        story.tactic.contains(Tactic.Pin) &&
        story.proofFailures.isEmpty &&
        story.pinProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.pinningMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.pinnedTarget.exists(pieces.contains) &&
            proof.kingBehindTarget.exists(pieces.contains) &&
            proof.pinningSlider.exists(slider =>
              pieces.contains(slider) ||
                proof.pinningMove.exists(move =>
                  story.route.contains(move) &&
                    move.to == slider.square &&
                    pieces.exists(piece =>
                      piece.side == slider.side && piece.man == slider.man && piece.square == move.from
                    )
                )
            )
      case Some(StoryWriter.TacticRemoveGuard) =>
        story.scene == Scene.Tactic &&
        story.tactic.contains(Tactic.RemoveGuard) &&
        story.proofFailures.isEmpty &&
        story.removeGuardProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.exactBoardAfterMoveRelation &&
            proof.removeGuardMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.guardedTarget.exists(pieces.contains) &&
            proof.removedDefender.exists(pieces.contains)
      case Some(StoryWriter.TacticOverload) =>
        story.scene == Scene.Tactic &&
        story.tactic.contains(Tactic.Overload) &&
        story.proofFailures.isEmpty &&
        story.overloadProof.exists: proof =>
          proof.complete &&
            proof.proofComplete &&
            proof.sameBoardProof &&
            proof.legalMove &&
            proof.sameBoardLegalReplay &&
            proof.dualDutyDefender &&
            proof.dutyTargetsNonKingMaterial &&
            proof.testMoveAttacksDutyTargetAfter &&
            proof.noReplyPreservesBothDutyTargets &&
            proof.route.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.defender.exists(pieces.contains) &&
            proof.dutyTargets.size == 2 &&
            proof.dutyTargets.forall(pieces.contains) &&
            proof.target.exists(piece => story.target.contains(piece.square)) &&
            proof.secondaryTarget.exists(piece => story.secondaryTarget.contains(piece.square)) &&
            proof.anchor.exists(piece => story.anchor.contains(piece.square))
      case Some(StoryWriter.TacticDeflect) =>
        story.scene == Scene.Tactic &&
        story.tactic.contains(Tactic.Deflect) &&
        story.proofFailures.isEmpty &&
        story.deflectProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalSideMove &&
            proof.legalRivalReply &&
            proof.replyByNamedDefender &&
            proof.defenderGuardedTargetBeforeReply &&
            proof.defenderNoLongerGuardsTargetAfterReply &&
            proof.targetRemainsAfterReply &&
            proof.defenderRemainsAfterReply &&
            proof.sideMoveDoesNotCaptureDefender &&
            proof.completeStoryProof &&
            proof.noEngineEvidenceUsed &&
            proof.side == story.side &&
            proof.rivalSide == story.rival &&
            proof.sideMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.rivalReply.nonEmpty &&
            proof.defenderBeforeReply.exists(pieces.contains) &&
            proof.targetBeforeReply.exists(piece =>
              pieces.contains(piece) &&
                story.target.contains(piece.square) &&
                piece.side == story.rival &&
                piece.man != Man.King
            ) &&
            proof.targetAfterReply.exists(piece =>
              story.target.contains(piece.square) &&
                piece.side == story.rival &&
                piece.man != Man.King
            ) &&
            proof.defenderAfterReply.exists(piece =>
              story.anchor.contains(piece.square) &&
                piece.side == story.rival
            )
      case Some(StoryWriter.TacticSkewer) =>
        story.scene == Scene.Tactic &&
        story.tactic.contains(Tactic.Skewer) &&
        story.proofFailures.isEmpty &&
        story.skewerProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.skewerMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.frontTarget.exists(pieces.contains) &&
            proof.rearTarget.exists(pieces.contains) &&
            proof.skewerSlider.exists(slider =>
              pieces.contains(slider) ||
                proof.skewerMove.exists(move =>
                  story.route.contains(move) &&
                    move.to == slider.square &&
                    pieces.exists(piece =>
                      piece.side == slider.side && piece.man == slider.man && piece.square == move.from
                    )
                )
            )
      case Some(StoryWriter.TacticQueenHit) =>
        story.scene == Scene.Tactic &&
        story.tactic.contains(Tactic.QueenHit) &&
        story.proofFailures.isEmpty &&
        story.queenHitProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalMove &&
            proof.exactAfterBoardReplay &&
            proof.rivalQueenExistsAfter &&
            proof.afterBoardQueenAttackedByMovingSide &&
            proof.queenHitProducedOrRevealedByLegalMove &&
            proof.attackMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.movingPieceBefore.exists(pieces.contains) &&
            proof.rivalQueenSquareAfter == story.target &&
            proof.attackingPieceSquareAfter == story.anchor
      case Some(StoryWriter.TacticRookHit) =>
        story.scene == Scene.Tactic &&
        story.tactic.contains(Tactic.RookHit) &&
        story.proofFailures.isEmpty &&
        story.rookHitProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalMove &&
            proof.completeStoryProof &&
            proof.exactAfterBoardReplay &&
            proof.targetPieceIsRivalRook &&
            proof.afterBoardRookAttackedByMovingSide &&
            proof.routeAndTargetBindSameBoard &&
            proof.attackMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.movingPieceBefore.exists(pieces.contains) &&
            proof.targetSquare == story.target &&
            proof.attackingPieceSquareAfter == story.anchor
      case Some(StoryWriter.TacticLoose) =>
        story.scene == Scene.Tactic &&
        story.tactic.contains(Tactic.Loose) &&
        story.proofFailures.isEmpty &&
        story.loosePieceProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalMove &&
            proof.exactAfterBoardReplay &&
            proof.targetRivalOwned &&
            proof.targetNonKing &&
            proof.afterBoardTargetAttackedByMovingSide &&
            proof.rivalLegalDefendersAfter.isEmpty &&
            proof.rivalSideHasNoLegalDefenderOfTarget &&
            proof.looseAttackProducedOrRevealedByLegalMove &&
            proof.attackMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.movingPieceBefore.exists(pieces.contains) &&
            proof.targetPieceSquareAfter == story.target &&
            proof.attackingPieceSquareAfter == story.anchor
      case Some(StoryWriter.TacticTrap) =>
        story.scene == Scene.Tactic &&
        story.tactic.contains(Tactic.Trap) &&
        story.proofFailures.isEmpty &&
        story.trapProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalMove &&
            proof.nonCapturingMove &&
            proof.exactAfterBoardReplay &&
            proof.completeStoryProof &&
            proof.targetRivalMinor &&
            proof.targetDefendedByRivalSide &&
            proof.afterBoardTargetAttackedByMovingSide &&
            proof.targetHasLegalMoves &&
            proof.everyTargetMoveUnsafe &&
            proof.routeDoesNotGiveCheck &&
            proof.routeDoesNotGiveMate &&
            proof.routeDoesNotPromote &&
            proof.trapMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.movingPieceBefore.exists(pieces.contains) &&
            proof.targetPieceSquareAfter == story.target &&
            proof.anchorSquareAfter == story.anchor
      case Some(StoryWriter.TacticDecoy) =>
        story.scene == Scene.Tactic &&
        story.tactic.contains(Tactic.Decoy) &&
        story.proofFailures.isEmpty &&
        story.decoyProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalSideMove &&
            proof.legalRivalReply &&
            proof.replyByNamedPiece &&
            proof.rivalPieceRemainsAfterReply &&
            proof.replyLandsOnDecoySquare &&
            proof.decoySquareEqualsLandingSquare &&
            proof.completeTrapFollowUpProof &&
            proof.trapFollowUpBindsSamePieceAndSquare &&
            proof.completeStoryProof &&
            proof.noEngineEvidenceUsed &&
            proof.side == story.side &&
            proof.rivalSide == story.rival &&
            proof.sideMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.rivalReply.nonEmpty &&
            proof.decoySquare == story.target &&
            proof.landingSquare == story.target &&
            proof.namedPieceAfterReply.exists(piece =>
              story.rival == piece.side &&
                story.target.contains(piece.square)
            ) &&
            proof.afterSideMoveBoard.exists(piece =>
              piece.side == story.side &&
                story.anchor.contains(piece.square) &&
                proof.sideMove.exists(_.to == piece.square)
            )
      case Some(StoryWriter.TacticInterference) =>
        story.scene == Scene.Tactic &&
          story.tactic.contains(Tactic.Interference) &&
          story.proofFailures.isEmpty &&
          story.interferenceProof.exists: proof =>
            proof.complete &&
              proof.sameBoardProof &&
              proof.legalSideMove &&
              proof.completeStoryProof &&
              proof.moveIsNonCapture &&
              proof.movedPieceLandsOnBlockingSquare &&
              proof.lineDefenderIsSlider &&
              proof.targetBound &&
              proof.defenderBlockingTargetCollinearOnSliderRay &&
              proof.blockingSquareStrictlyBetweenDefenderAndTarget &&
              proof.defenderLineContactBeforeMove &&
              proof.afterMoveBlockingSquareOccupiedByMovedPiece &&
              proof.defenderLineContactRemovedByBlocker &&
              proof.noEngineEvidenceUsed &&
              proof.side == story.side &&
              proof.rivalSide == story.rival &&
              proof.sideMove.exists(move =>
                story.route.contains(move) &&
                  (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
              ) &&
              proof.targetSquare == story.target &&
              proof.blockingSquare == story.anchor &&
              proof.lineDefenderBefore.exists(piece =>
                pieces.contains(piece) &&
                  piece.side == story.rival
              ) &&
              proof.movedPieceBefore.exists(piece =>
                pieces.contains(piece) &&
                  piece.side == story.side &&
                  proof.sideMove.exists(_.from == piece.square)
              ) &&
              proof.movedPieceAfter.exists(piece =>
                story.anchor.contains(piece.square) &&
                  piece.side == story.side
              ) &&
              proof.targetPieceBefore.forall(piece =>
                pieces.contains(piece) &&
                  story.target.contains(piece.square) &&
                  piece.side == story.rival &&
                  piece.man != Man.King
              ) &&
              proof.lineDefenderAfter.exists(piece => piece.side == story.rival)
      case Some(StoryWriter.ScenePawnAdvance) =>
        story.scene == Scene.PawnAdvance &&
        story.tactic.isEmpty &&
        story.plan.isEmpty &&
        story.proofFailures.isEmpty &&
        story.pawnAdvanceProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalOneStepNonCaptureNonPromotion &&
            proof.alreadyPassedBefore &&
            proof.afterBoardPassedPawn &&
            proof.exactAfterBoardReplay &&
            proof.advanceMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.pawnBefore.exists(pieces.contains) &&
            proof.fromSquare == story.anchor &&
            proof.toSquare == story.target
      case Some(StoryWriter.ScenePawnStop) =>
        story.scene == Scene.PawnStop &&
        story.tactic.isEmpty &&
        story.plan.isEmpty &&
        story.proofFailures.isEmpty &&
        story.pawnStopProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalStopMove &&
            proof.targetPawn.nonEmpty &&
            proof.targetPawnAlreadyPassed &&
            proof.nextAdvanceSquareNonPromotion &&
            proof.nextAdvanceSquareEmptyBefore &&
            proof.stopKind.exists(PawnStopKind.values.contains) &&
            (
              proof.nextAdvanceSquareOccupiedAfter ||
                proof.nextAdvanceSquareAttackedAfter ||
                proof.nextAdvanceSquareControlledByPawnAfter
            ) &&
            proof.exactAfterBoardReplay &&
            proof.targetPawnStillPresentAfter &&
            proof.nextAdvanceSquareStoppedAfter &&
            proof.stopMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.targetPawn.exists(pieces.contains) &&
            proof.nextAdvanceSquare == story.target &&
            proof.stopMove.map(_.from) == story.anchor
      case Some(StoryWriter.ScenePawnBreak) =>
        story.scene == Scene.PawnBreak &&
        story.tactic.isEmpty &&
        story.plan.isEmpty &&
        story.proofFailures.isEmpty &&
        story.captureResult.isEmpty &&
        story.threatProof.isEmpty &&
        story.defenseProof.isEmpty &&
        story.multiTargetProof.isEmpty &&
        story.lineProof.isEmpty &&
        story.pinProof.isEmpty &&
        story.removeGuardProof.isEmpty &&
        story.skewerProof.isEmpty &&
        story.pawnAdvanceProof.isEmpty &&
        story.pawnStopProof.isEmpty &&
        story.pawnCaptureProof.isEmpty &&
        story.passedPawnCreatedProof.isEmpty &&
        story.fileOpenedProof.isEmpty &&
        story.promotionThreatProof.isEmpty &&
        story.promotionProof.isEmpty &&
        story.pawnBreakProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalPawnMove &&
            proof.nonPromotionMove &&
            proof.nonCapturingMove &&
            proof.exactAfterBoardReplay &&
            proof.directPawnLeverAfterMove &&
            proof.leverCreatedByMove &&
            proof.singleRivalPawnTarget &&
            proof.contactKinds == Vector(
              PawnBreakContactKind.PawnChallengesPawn,
              PawnBreakContactKind.PawnLeverCreated
            ) &&
            proof.breakMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.pawnBefore.exists(pieces.contains) &&
            proof.pawnBefore.map(_.square) == story.anchor &&
            proof.targetPawn.exists(piece => story.target.contains(piece.square))
      case Some(StoryWriter.ScenePawnBlock) =>
        story.scene == Scene.PawnBlock &&
        story.tactic.isEmpty &&
        story.plan.isEmpty &&
        story.proofFailures.isEmpty &&
        story.captureResult.isEmpty &&
        story.threatProof.isEmpty &&
        story.defenseProof.isEmpty &&
        story.multiTargetProof.isEmpty &&
        story.lineProof.isEmpty &&
        story.pinProof.isEmpty &&
        story.removeGuardProof.isEmpty &&
        story.skewerProof.isEmpty &&
        story.pawnAdvanceProof.isEmpty &&
        story.pawnStopProof.isEmpty &&
        story.pawnBreakProof.isEmpty &&
        story.pawnCaptureProof.isEmpty &&
        story.passedPawnCreatedProof.isEmpty &&
        story.fileOpenedProof.isEmpty &&
        story.promotionThreatProof.isEmpty &&
        story.promotionProof.isEmpty &&
        story.pawnBlockProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalMove &&
            proof.exactAfterBoardReplay &&
            proof.blockCreatedByMove &&
            proof.nextAdvanceSquareOccupiedAfter &&
            proof.occupyingPieceBelongsToBlockingSide &&
            proof.ordinaryDirectOneSquarePawnBlock &&
            proof.blockMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.movingPieceBefore.exists(pieces.contains) &&
            proof.blockedPawn.exists(pieces.contains) &&
            proof.originSquare == story.anchor &&
            proof.blockedPawnNextAdvanceSquare == story.target
      case Some(StoryWriter.SceneCheckGiven) =>
        story.scene == Scene.CheckGiven &&
        story.tactic.isEmpty &&
        story.plan.isEmpty &&
        story.proofFailures.isEmpty &&
        story.captureResult.isEmpty &&
        story.threatProof.isEmpty &&
        story.defenseProof.isEmpty &&
        story.multiTargetProof.isEmpty &&
        story.lineProof.isEmpty &&
        story.pinProof.isEmpty &&
        story.removeGuardProof.isEmpty &&
        story.skewerProof.isEmpty &&
        story.pawnAdvanceProof.isEmpty &&
        story.pawnStopProof.isEmpty &&
        story.pawnBreakProof.isEmpty &&
        story.pawnBlockProof.isEmpty &&
        story.pawnCaptureProof.isEmpty &&
        story.passedPawnCreatedProof.isEmpty &&
        story.fileOpenedProof.isEmpty &&
        story.promotionThreatProof.isEmpty &&
        story.promotionProof.isEmpty &&
        story.checkEscapedProof.isEmpty &&
        story.checkGivenProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalMove &&
            proof.exactAfterBoardReplay &&
            proof.afterBoardRivalKingInCheck &&
            proof.checkProducedByLegalMove &&
            proof.checkMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.movingPieceBefore.exists(pieces.contains) &&
            proof.originSquare == story.anchor &&
            proof.rivalKingSquareAfter == story.target
      case Some(StoryWriter.SceneCheckEscaped) =>
        story.scene == Scene.CheckEscaped &&
        story.tactic.isEmpty &&
        story.plan.isEmpty &&
        story.proofFailures.isEmpty &&
        story.captureResult.isEmpty &&
        story.threatProof.isEmpty &&
        story.defenseProof.isEmpty &&
        story.multiTargetProof.isEmpty &&
        story.lineProof.isEmpty &&
        story.pinProof.isEmpty &&
        story.removeGuardProof.isEmpty &&
        story.skewerProof.isEmpty &&
        story.pawnAdvanceProof.isEmpty &&
        story.pawnStopProof.isEmpty &&
        story.pawnBreakProof.isEmpty &&
        story.pawnBlockProof.isEmpty &&
        story.pawnCaptureProof.isEmpty &&
        story.passedPawnCreatedProof.isEmpty &&
        story.fileOpenedProof.isEmpty &&
        story.promotionThreatProof.isEmpty &&
        story.promotionProof.isEmpty &&
        story.checkGivenProof.isEmpty &&
        story.checkEscapedProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalMove &&
            proof.exactBeforeBoardState &&
            proof.exactAfterBoardReplay &&
            proof.beforeBoardSideKingInCheck &&
            proof.afterBoardSideKingNotInCheck &&
            proof.checkEscapedByLegalMove &&
            proof.escapeMove.exists(move =>
              story.route.contains(move) &&
                facts.sideLegal.lines.contains(move)
            ) &&
            proof.movingPieceBefore.exists(pieces.contains) &&
            proof.beforeKingSquare.nonEmpty &&
            proof.originSquare == story.anchor &&
            proof.afterKingSquare == story.target
      case Some(StoryWriter.SceneCheckmate) =>
        story.scene == Scene.Checkmate &&
        story.tactic.isEmpty &&
        story.plan.isEmpty &&
        story.proofFailures.isEmpty &&
        story.captureResult.isEmpty &&
        story.threatProof.isEmpty &&
        story.defenseProof.isEmpty &&
        story.multiTargetProof.isEmpty &&
        story.lineProof.isEmpty &&
        story.pinProof.isEmpty &&
        story.removeGuardProof.isEmpty &&
        story.skewerProof.isEmpty &&
        story.pawnAdvanceProof.isEmpty &&
        story.pawnStopProof.isEmpty &&
        story.pawnBreakProof.isEmpty &&
        story.pawnBlockProof.isEmpty &&
        story.pawnCaptureProof.isEmpty &&
        story.passedPawnCreatedProof.isEmpty &&
        story.fileOpenedProof.isEmpty &&
        story.promotionThreatProof.isEmpty &&
        story.promotionProof.isEmpty &&
        story.checkGivenProof.isEmpty &&
        story.checkEscapedProof.isEmpty &&
        story.checkmateProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalMove &&
            proof.exactAfterBoardReplay &&
            proof.afterBoardRivalKingInCheck &&
            proof.afterBoardRivalSideHasNoLegalEscape &&
            proof.checkmateProducedByLegalMove &&
            proof.matingSide == story.side &&
            proof.rivalSide == story.rival &&
            proof.mateMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.movingPieceBefore.exists(pieces.contains) &&
            proof.originSquare == story.anchor &&
            proof.rivalKingSquareAfter == story.target
      case Some(StoryWriter.SceneStalemate) =>
        story.scene == Scene.Stalemate &&
        story.tactic.isEmpty &&
        story.plan.isEmpty &&
        story.proofFailures.isEmpty &&
        story.captureResult.isEmpty &&
        story.threatProof.isEmpty &&
        story.defenseProof.isEmpty &&
        story.multiTargetProof.isEmpty &&
        story.lineProof.isEmpty &&
        story.pinProof.isEmpty &&
        story.removeGuardProof.isEmpty &&
        story.skewerProof.isEmpty &&
        story.pawnAdvanceProof.isEmpty &&
        story.pawnStopProof.isEmpty &&
        story.pawnBreakProof.isEmpty &&
        story.pawnBlockProof.isEmpty &&
        story.pawnCaptureProof.isEmpty &&
        story.passedPawnCreatedProof.isEmpty &&
        story.fileOpenedProof.isEmpty &&
        story.promotionThreatProof.isEmpty &&
        story.promotionProof.isEmpty &&
        story.checkGivenProof.isEmpty &&
        story.checkEscapedProof.isEmpty &&
        story.checkmateProof.isEmpty &&
        story.stalemateProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalMove &&
            proof.exactAfterBoardReplay &&
            proof.afterBoardRivalSideNotInCheck &&
            proof.afterBoardRivalSideHasNoLegalMoves &&
            proof.stalemateProducedByLegalMove &&
            proof.stalematingSide == story.side &&
            proof.rivalSide == story.rival &&
            proof.stalemateMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.movingPieceBefore.exists(pieces.contains) &&
            proof.originSquare == story.anchor &&
            proof.rivalKingSquareAfter == story.target
      case Some(StoryWriter.SceneMateThreat) =>
        story.scene == Scene.MateThreat &&
        story.tactic.isEmpty &&
        story.plan.isEmpty &&
        story.proofFailures.isEmpty &&
        story.captureResult.isEmpty &&
        story.threatProof.isEmpty &&
        story.defenseProof.isEmpty &&
        story.multiTargetProof.isEmpty &&
        story.lineProof.isEmpty &&
        story.pinProof.isEmpty &&
        story.removeGuardProof.isEmpty &&
        story.overloadProof.isEmpty &&
        story.deflectProof.isEmpty &&
        story.decoyProof.isEmpty &&
        story.interferenceProof.isEmpty &&
        story.skewerProof.isEmpty &&
        story.queenHitProof.isEmpty &&
        story.rookHitProof.isEmpty &&
        story.loosePieceProof.isEmpty &&
        story.trapProof.isEmpty &&
        story.pawnAdvanceProof.isEmpty &&
        story.pawnStopProof.isEmpty &&
        story.pawnBreakProof.isEmpty &&
        story.pawnBlockProof.isEmpty &&
        story.pawnCaptureProof.isEmpty &&
        story.passedPawnCreatedProof.isEmpty &&
        story.fileOpenedProof.isEmpty &&
        story.promotionThreatProof.isEmpty &&
        story.promotionProof.isEmpty &&
        story.checkGivenProof.isEmpty &&
        story.checkEscapedProof.isEmpty &&
        story.checkmateProof.isEmpty &&
        story.stalemateProof.isEmpty &&
        story.mateThreatProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalMove &&
            proof.exactAfterBoardReplay &&
            proof.threatMoveIsNotImmediateCheckmate &&
            proof.nextSidePlyLegalCheckmateAvailable &&
            proof.threateningSide == story.side &&
            proof.rivalSide == story.rival &&
            proof.threatMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.movingPieceBefore.exists(pieces.contains) &&
            proof.destinationSquare == story.anchor &&
            proof.rivalKingSquareAfterThreatMove == story.target &&
            proof.nextMateMove.nonEmpty &&
            proof.nextMateSan.nonEmpty
      case Some(StoryWriter.ScenePromotionThreat) =>
        story.scene == Scene.PromotionThreat &&
        story.tactic.isEmpty &&
        story.plan.isEmpty &&
        story.proofFailures.isEmpty &&
        story.captureResult.isEmpty &&
        story.threatProof.isEmpty &&
        story.defenseProof.isEmpty &&
        story.multiTargetProof.isEmpty &&
        story.lineProof.isEmpty &&
        story.pinProof.isEmpty &&
        story.removeGuardProof.isEmpty &&
        story.skewerProof.isEmpty &&
        story.pawnAdvanceProof.isEmpty &&
        story.pawnStopProof.isEmpty &&
        story.pawnBreakProof.isEmpty &&
        story.pawnCaptureProof.isEmpty &&
        story.passedPawnCreatedProof.isEmpty &&
        story.fileOpenedProof.isEmpty &&
        story.promotionProof.isEmpty &&
        story.promotionThreatProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalPawnMove &&
            proof.nonPromotionCreatingMove &&
            proof.exactAfterBoardReplay &&
            proof.nextMovePromotionLegal &&
            proof.creatingMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.pawnBefore.exists(pieces.contains) &&
            proof.pawnBefore.map(_.square) == story.anchor &&
            proof.promotionSquare == story.target &&
            proof.nextPromotionMove == proof.promotionRoute
      case Some(StoryWriter.ScenePromotion) =>
        story.scene == Scene.Promotion &&
        story.tactic.isEmpty &&
        story.plan.isEmpty &&
        story.proofFailures.isEmpty &&
        story.captureResult.isEmpty &&
        story.threatProof.isEmpty &&
        story.defenseProof.isEmpty &&
        story.multiTargetProof.isEmpty &&
        story.lineProof.isEmpty &&
        story.pinProof.isEmpty &&
        story.removeGuardProof.isEmpty &&
        story.skewerProof.isEmpty &&
        story.pawnAdvanceProof.isEmpty &&
        story.pawnStopProof.isEmpty &&
        story.pawnBreakProof.isEmpty &&
        story.pawnCaptureProof.isEmpty &&
        story.passedPawnCreatedProof.isEmpty &&
        story.fileOpenedProof.isEmpty &&
        story.promotionThreatProof.isEmpty &&
        story.proof.conversionPrize == 0 &&
        story.promotionProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalPromotionMove &&
            proof.nonCapturing &&
            proof.exactBoardReplay &&
            proof.pawnReachesFinalRank &&
            proof.promotionMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.pawn.exists(pieces.contains) &&
            proof.originSquare == story.anchor &&
            proof.promotionSquare == story.target &&
            proof.promotedPiece.exists(piece => story.target.contains(piece.square))
      case Some(StoryWriter.ScenePawnCapture) =>
        story.scene == Scene.PawnCapture &&
        story.tactic.isEmpty &&
        story.plan.isEmpty &&
        story.proofFailures.isEmpty &&
        story.captureResult.isEmpty &&
        story.threatProof.isEmpty &&
        story.defenseProof.isEmpty &&
        story.multiTargetProof.isEmpty &&
        story.lineProof.isEmpty &&
        story.pinProof.isEmpty &&
        story.removeGuardProof.isEmpty &&
        story.skewerProof.isEmpty &&
        story.pawnAdvanceProof.isEmpty &&
        story.pawnStopProof.isEmpty &&
        story.pawnBreakProof.isEmpty &&
        story.passedPawnCreatedProof.isEmpty &&
        story.fileOpenedProof.isEmpty &&
        story.promotionThreatProof.isEmpty &&
        story.promotionProof.isEmpty &&
        story.pawnCaptureProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalPawnCapture &&
            proof.ordinaryDiagonalPawnCapture &&
            proof.exactAfterBoardReplay &&
            proof.singleRivalPawnCaptured &&
            proof.captureMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.pawnBefore.exists(pieces.contains) &&
            proof.capturedPawn.exists(pieces.contains) &&
            proof.originSquare == story.anchor &&
            proof.captureSquare == story.target
      case Some(StoryWriter.ScenePassedPawnCreated) =>
        story.scene == Scene.PassedPawnCreated &&
        story.tactic.isEmpty &&
        story.plan.isEmpty &&
        story.proofFailures.isEmpty &&
        story.captureResult.isEmpty &&
        story.threatProof.isEmpty &&
        story.defenseProof.isEmpty &&
        story.multiTargetProof.isEmpty &&
        story.lineProof.isEmpty &&
        story.pinProof.isEmpty &&
        story.removeGuardProof.isEmpty &&
        story.skewerProof.isEmpty &&
        story.pawnAdvanceProof.isEmpty &&
        story.pawnStopProof.isEmpty &&
        story.pawnBreakProof.isEmpty &&
        story.pawnCaptureProof.isEmpty &&
        story.fileOpenedProof.isEmpty &&
        story.promotionThreatProof.isEmpty &&
        story.promotionProof.isEmpty &&
        story.passedPawnCreatedProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.exactBeforeBoard &&
            proof.legalPawnMove &&
            proof.ordinaryPawnMoveOrCapture &&
            proof.nonPromotionMove &&
            !proof.passedBefore &&
            proof.exactAfterBoardReplay &&
            proof.passedAfter &&
            proof.exactlyOneNewPassedPawn &&
            proof.creatingMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.pawnBefore.exists(pieces.contains) &&
            proof.originSquare == story.anchor &&
            proof.createdPassedPawn.exists(piece => story.target.contains(piece.square))
      case Some(StoryWriter.SceneFileOpened) =>
        story.scene == Scene.FileOpened &&
        story.tactic.isEmpty &&
        story.plan.isEmpty &&
        story.proofFailures.isEmpty &&
        story.captureResult.isEmpty &&
        story.threatProof.isEmpty &&
        story.defenseProof.isEmpty &&
        story.multiTargetProof.isEmpty &&
        story.lineProof.isEmpty &&
        story.pinProof.isEmpty &&
        story.removeGuardProof.isEmpty &&
        story.skewerProof.isEmpty &&
        story.pawnAdvanceProof.isEmpty &&
        story.pawnStopProof.isEmpty &&
        story.pawnBreakProof.isEmpty &&
        story.pawnCaptureProof.isEmpty &&
        story.passedPawnCreatedProof.isEmpty &&
        story.promotionThreatProof.isEmpty &&
        story.promotionProof.isEmpty &&
        story.fileOpenedProof.exists: proof =>
          proof.complete &&
            proof.sameBoardProof &&
            proof.legalPawnMove &&
            proof.nonPromotionMove &&
            proof.ordinaryPawnMoveOrCapture &&
            !proof.enPassantMove &&
            proof.leavesOriginFile &&
            proof.originFileOccupiedBeforeByMovingPawn &&
            !proof.originFileOpenBefore &&
            proof.exactAfterBoardReplay &&
            proof.afterBoardHasNoWhitePawnOnOriginFile &&
            proof.afterBoardHasNoBlackPawnOnOriginFile &&
            proof.originFileOpenAfter &&
            proof.openedFileIsOriginFile &&
            proof.openingMove.exists(move =>
              story.route.contains(move) &&
                (facts.sideLegal.lines.contains(move) || facts.rivalLegal.lines.contains(move))
            ) &&
            proof.pawnBefore.exists(pieces.contains) &&
            proof.originSquare == story.anchor &&
            proof.destinationSquare == story.target &&
            proof.openedFile == story.openedFile
      case _ => false

  private def checkedStatus(
      requestedStatus: EngineCheckStatus,
      evalBefore: Option[EngineEval],
      evalAfter: Option[EngineEval]
  ): EngineCheckStatus =
    if evalDrop(evalBefore, evalAfter) >= RefuteEvalDropCentipawns then EngineCheckStatus.Refutes
    else requestedStatus

  private def evalDrop(evalBefore: Option[EngineEval], evalAfter: Option[EngineEval]): Int =
    evalBefore
      .zip(evalAfter)
      .fold(0): (before, after) =>
        before.centipawns - after.centipawns
