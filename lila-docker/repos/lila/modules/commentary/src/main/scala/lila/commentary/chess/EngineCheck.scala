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
      checkedMove.forall(line => facts.sideLegal.lines.contains(line) || facts.rivalLegal.lines.contains(line))
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
      case _ => false

  private def checkedStatus(
      requestedStatus: EngineCheckStatus,
      evalBefore: Option[EngineEval],
      evalAfter: Option[EngineEval]
  ): EngineCheckStatus =
    if evalDrop(evalBefore, evalAfter) >= RefuteEvalDropCentipawns then EngineCheckStatus.Refutes
    else requestedStatus

  private def evalDrop(evalBefore: Option[EngineEval], evalAfter: Option[EngineEval]): Int =
    evalBefore.zip(evalAfter).fold(0): (before, after) =>
      before.centipawns - after.centipawns
