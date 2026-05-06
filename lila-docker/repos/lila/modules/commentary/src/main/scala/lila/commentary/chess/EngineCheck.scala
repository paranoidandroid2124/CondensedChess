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
    missingEvidence: Vector[BoardFacts.MissingEvidence]
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
    val sameBoardProof = BoardFacts.sameBoardReady(facts) && story.exists(storyIdentityOnFacts(facts, _))
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
        else Vector(BoardFacts.MissingEvidence("EngineCheck", missing))
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
        else Vector(BoardFacts.MissingEvidence("EngineCheck", missing))
    )

  private def engineLineBindsCheckedMove(engineLine: Option[EngineLine], checkedMove: Option[Line]): Boolean =
    engineLine.zip(checkedMove).forall((line, move) => line.moves.headOption.contains(move))

  private def depthOrFreshnessPresent(depth: Option[Int], freshnessPly: Option[Int]): Boolean =
    depth.exists(_ > 0) || freshnessPly.contains(0)

  private def storyIdentityOnFacts(facts: BoardFacts, story: Story): Boolean =
    val pieces = facts.pieces.toSet
    story.writer.contains(StoryWriter.TacticHanging) &&
      story.tactic.contains(Tactic.Hanging) &&
      story.proofFailures.isEmpty &&
      story.captureResult.exists: result =>
        result.sameBoardProof &&
          result.missingEvidence.isEmpty &&
          story.route.contains(result.captureLine) &&
          result.capturingPiece.exists(pieces.contains) &&
          result.targetPiece.exists(pieces.contains)

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
