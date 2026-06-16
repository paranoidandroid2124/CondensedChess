package lila.commentary.analysis

import chess.*
import chess.format.Fen
import chess.variant.Standard
import lila.commentary.{ MoveReviewRefs, MoveReviewVariationRef }
import lila.commentary.analysis.structure.WeaknessTargetProfile
import lila.commentary.model.{ Fact, NarrativeContext }
import lila.commentary.model.strategic.VariationLine

enum LineConsequenceKind:
  case PreviewOnly, ExchangeSequence, ForcingCheckSequence, CentralBreakTiming, CentralPawnAdvance, MaterialTransition,
    ImmediateOpponentPawnCapture, ImmediateOpponentTargetPressure, PlayedMoveTargetPressure, DelayedPawnCapture,
    PassedPawnCreation, PromotionRace, OriginSquareClearance, MinorPieceReroute

enum LineConsequenceRelease:
  case SurfaceCandidate, ReplayBackedInternal, DiagnosticOnly

final case class LineStructureDetail(
    kind: String,
    square: Option[String] = None,
    file: Option[String] = None,
    side: Option[String] = None
):
  def evidenceRefs: List[String] =
    List[Option[String]](
      Some(s"line_consequence_structure_kind:$kind"),
      square.map(value => s"line_consequence_structure_square:$value"),
      file.map(value => s"line_consequence_structure_file:$value"),
      side.map(value => s"line_consequence_structure_side:$value")
    ).flatten

final case class LineTargetDetail(
    kind: String,
    square: String,
    role: String,
    attacker: Option[String] = None,
    side: Option[String] = None
):
  def evidenceRefs: List[String] =
    List[Option[String]](
      Some(s"line_consequence_target_kind:$kind"),
      Some(s"line_consequence_target_square:$square"),
      Some(s"line_consequence_target_role:$role"),
      attacker.map(value => s"line_consequence_target_attacker:$value"),
      side.map(value => s"line_consequence_target_side:$value")
    ).flatten

final case class LineConsequenceEvidence(
    lineId: Option[String],
    sanMoves: List[String],
    uciMoves: List[String],
    scoreCp: Option[Int],
    mate: Option[Int],
    depth: Option[Int],
    windowPly: Int,
    kind: LineConsequenceKind,
    triggerSan: Option[String],
    consequence: String,
    whyItMatters: Option[String],
    release: LineConsequenceRelease,
    rejectReasons: List[String],
    structureDetails: List[LineStructureDetail] = Nil,
    targetDetails: List[LineTargetDetail] = Nil
):
  def playerSentence: String =
    List(Some(consequence), whyItMatters)
      .flatten
      .map(_.trim)
      .filter(_.nonEmpty)
      .distinct
      .mkString(" ")

  def surfaceBlockReasons: List[String] =
    rejectReasons.filter(LineConsequenceRejectReason.blocksSurface)

  def surfaceReady: Boolean =
    release == LineConsequenceRelease.SurfaceCandidate && surfaceBlockReasons.isEmpty

  def narrativeReady: Boolean =
    surfaceReady || release == LineConsequenceRelease.ReplayBackedInternal

private[analysis] object LineConsequenceRejectReason:

  val RefReplayFailed = "line_consequence:ref_replay_failed"
  val EngineOnly = "line_consequence:engine_only"
  val EngineReplayFailed = "line_consequence:engine_replay_failed"

  private val SurfaceBlocking = Set(RefReplayFailed, EngineOnly, EngineReplayFailed)

  def blocksSurface(reason: String): Boolean =
    SurfaceBlocking.contains(Option(reason).getOrElse("").trim)

private[commentary] object LineConsequenceEvaluator:

  val RefReplayFailed = LineConsequenceRejectReason.RefReplayFailed
  val EngineOnly = LineConsequenceRejectReason.EngineOnly
  val EngineReplayFailed = LineConsequenceRejectReason.EngineReplayFailed
  private val SurfaceMaxPly = 6
  private val InternalMaxPly = 8

  private[analysis] final case class LineStep(
      san: String,
      uci: String,
      orig: Square,
      dest: Square,
      role: Role,
      color: Color,
      captures: Boolean,
      capturedRole: Option[Role],
      givesCheck: Boolean,
      createsPassedPawn: Boolean,
      advancesPassedPawn: Boolean,
      promotes: Boolean,
      structureDetails: List[LineStructureDetail]
  )

  def surfaceCandidate(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      maxPly: Int = SurfaceMaxPly
  ): Option[LineConsequenceEvidence] =
    val candidates = fromRefs(ctx, refs, maxPly).filter(evidence => evidence.surfaceReady && genericSurfaceCandidateAllowed(evidence))
    preferredSurfaceCandidate(ctx, candidates).orElse(candidates.headOption)

  def reviewedMoveSurfaceCandidate(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      maxPly: Int = InternalMaxPly
  ): Option[LineConsequenceEvidence] =
    fromRefs(ctx, refs, maxPly)
      .filter(evidence =>
        evidence.surfaceReady &&
          evidence.kind != LineConsequenceKind.PreviewOnly &&
          lineStartsWithPlayed(ctx, evidence)
      )
      .headOption

  def moveReviewCandidate(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      truthContract: Option[DecisiveTruthContract],
      reviewedMoveOwnerCertified: Boolean = false
  ): Option[LineConsequenceEvidence] =
    val surface = surfaceCandidate(ctx, refs)
    val reviewed =
      reviewedMoveSurfaceCandidate(ctx, refs)
        .filter(reviewedMoveCandidateAllowed(truthContract, _, reviewedMoveOwnerCertified))
    val surfaceWithoutReviewedOnlyClaims =
      surface.filter(evidence =>
        evidence.kind != LineConsequenceKind.ImmediateOpponentPawnCapture &&
          evidence.kind != LineConsequenceKind.ImmediateOpponentTargetPressure &&
          evidence.kind != LineConsequenceKind.PlayedMoveTargetPressure &&
          evidence.kind != LineConsequenceKind.DelayedPawnCapture
      )
    val ownedSurface =
      surfaceWithoutReviewedOnlyClaims.filter(lineStartsWithPlayed(ctx, _))
    if reviewedMoveOwnerCertified || truthContract.exists(_.blocksStrategicSupport) then
      reviewed.orElse(ownedSurface).orElse(surfaceWithoutReviewedOnlyClaims)
    else
      ownedSurface
        .filter(_.kind != LineConsequenceKind.PreviewOnly)
        .orElse(reviewed)
        .orElse(surfaceWithoutReviewedOnlyClaims.filter(_.kind != LineConsequenceKind.PreviewOnly))
        .orElse(surfaceWithoutReviewedOnlyClaims.filter(_.kind == LineConsequenceKind.PreviewOnly))

  def narrativeCandidate(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      maxPly: Int = InternalMaxPly
  ): Option[LineConsequenceEvidence] =
    val refEvidence = fromRefsOnly(ctx, refs, maxPly).filter(genericSurfaceCandidateAllowed)
    refEvidence.find(_.surfaceReady)
      .orElse(refEvidence.find(_.narrativeReady))
      .orElse(fromEngine(ctx, maxPly).filter(genericSurfaceCandidateAllowed).find(_.narrativeReady))

  def fromRefs(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      maxPly: Int = SurfaceMaxPly
  ): List[LineConsequenceEvidence] =
    val refEvidence = fromRefsOnly(ctx, refs, maxPly)
    if refEvidence.nonEmpty then refEvidence
    else fromEngine(ctx, maxPly)

  def fromRefsForRoleComparison(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      maxPly: Int = InternalMaxPly
  ): List[LineConsequenceEvidence] =
    refs match
      case Some(refsValue) if refsValue.variations.nonEmpty =>
        refsValue.variations.map(refEvidenceFromStart(ctx, _, maxPly, includeMinorPieceReroute = true)).filter(genericSurfaceCandidateAllowed)
      case _ =>
        Nil

  private[analysis] def genericSurfaceCandidateAllowed(evidence: LineConsequenceEvidence): Boolean =
    evidence.kind != LineConsequenceKind.ImmediateOpponentTargetPressure &&
      evidence.kind != LineConsequenceKind.PlayedMoveTargetPressure

  private def fromRefsOnly(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      maxPly: Int
  ): List[LineConsequenceEvidence] =
    refs match
      case Some(refsValue) if refsValue.variations.nonEmpty =>
        refsValue.variations.map(refEvidence(ctx, _, maxPly))
      case _ =>
        Nil

  private def preferredSurfaceCandidate(
      ctx: NarrativeContext,
      candidates: List[LineConsequenceEvidence]
  ): Option[LineConsequenceEvidence] =
    val concreteOwnedByPlayed =
      candidates.filter(evidence =>
        evidence.kind != LineConsequenceKind.PreviewOnly &&
          lineTriggerMatchesPlayed(ctx, evidence)
      )
    concreteOwnedByPlayed.headOption

  private def lineTriggerMatchesPlayed(ctx: NarrativeContext, evidence: LineConsequenceEvidence): Boolean =
    (
      for
        trigger <- evidence.triggerSan
        played <- ctx.playedSan
      yield sameSan(trigger, played)
    ).getOrElse(false)

  private def lineStartsWithPlayed(ctx: NarrativeContext, evidence: LineConsequenceEvidence): Boolean =
    val playedUci = ctx.playedMove.map(MoveReviewPvLine.normalizeUci).filter(_.nonEmpty)
    val playedSan = ctx.playedSan.map(_.trim).filter(_.nonEmpty)
    playedUci.exists(uci => evidence.uciMoves.headOption.exists(MoveReviewPvLine.normalizeUci(_) == uci)) ||
      playedSan.exists(san => evidence.sanMoves.headOption.exists(sameSan(_, san)))

  private def reviewedMoveCandidateAllowed(
      truthContract: Option[DecisiveTruthContract],
      evidence: LineConsequenceEvidence,
      reviewedMoveOwnerCertified: Boolean
  ): Boolean =
    val quietBestBranchConsequence =
      truthContract.exists(contract =>
        contract.truthClass == DecisiveTruthClass.Best &&
          contract.chosenMatchesBest &&
          contract.reasonFamily == DecisiveReasonKind.QuietTechnicalMove &&
          contract.failureMode == FailureInterpretationMode.NoClearPlan &&
          Set(
            LineConsequenceKind.ExchangeSequence,
            LineConsequenceKind.MaterialTransition
          ).contains(evidence.kind)
      )
    val immediateOpponentPawnCaptureAllowed =
      evidence.kind != LineConsequenceKind.ImmediateOpponentPawnCapture ||
        truthContract.exists(contract => contract.isBad && !contract.chosenMatchesBest)
    val immediateOpponentTargetPressureAllowed =
      evidence.kind != LineConsequenceKind.ImmediateOpponentTargetPressure ||
        truthContract.exists(contract => contract.isBad && !contract.chosenMatchesBest)
    val playedMoveTargetPressureAllowed =
      evidence.kind != LineConsequenceKind.PlayedMoveTargetPressure ||
        (playedMoveTargetPressureEvidenceReady(evidence) &&
          truthContract.exists(contract => !contract.isBad && !contract.blocksStrategicSupport))
    evidence.surfaceReady &&
      evidence.kind != LineConsequenceKind.PreviewOnly &&
      immediateOpponentPawnCaptureAllowed &&
      immediateOpponentTargetPressureAllowed &&
      playedMoveTargetPressureAllowed &&
      (reviewedMoveOwnerCertified || truthContract.exists(contract =>
        contract.blocksStrategicSupport ||
          contract.reasonFamily == DecisiveReasonKind.TacticalRefutation ||
          contract.failureMode == FailureInterpretationMode.TacticalRefutation ||
          evidence.kind == LineConsequenceKind.PlayedMoveTargetPressure
      ) || quietBestBranchConsequence)

  private[analysis] def playedMoveTargetPressureEvidenceReady(evidence: LineConsequenceEvidence): Boolean =
    evidence.kind != LineConsequenceKind.PlayedMoveTargetPressure ||
      (
        playedMoveTargetPressureDetailsPresent(evidence) &&
          samePieceUciContinuationPresent(evidence)
      )

  private def playedMoveTargetPressureDetailsPresent(evidence: LineConsequenceEvidence): Boolean =
    evidence.targetDetails.exists(detail =>
      detail.kind == AdvancedPawnTargetPressureDetail &&
        detail.role == roleKey(Pawn) &&
        detail.attacker.exists(_.trim.nonEmpty) &&
        detail.side.exists(_.trim.nonEmpty) &&
        detail.square.trim.nonEmpty
    )

  private def samePieceUciContinuationPresent(evidence: LineConsequenceEvidence): Boolean =
    evidence.uciMoves.headOption.exists { first =>
      val normalized = MoveReviewPvLine.normalizeUci(first)
      val dest = normalized.slice(2, 4)
      dest.nonEmpty &&
        evidence.uciMoves.drop(1).map(MoveReviewPvLine.normalizeUci).exists(uci => uci.startsWith(dest))
    }

  private def sameSan(left: String, right: String): Boolean =
    def clean(value: String): String =
      Option(value).getOrElse("").trim.replaceAll("""[+#?!]+$""", "")
    clean(left) == clean(right)

  def fromEngine(ctx: NarrativeContext, maxPly: Int = SurfaceMaxPly): List[LineConsequenceEvidence] =
    ctx.engineEvidence.toList.flatMap(_.variations).map { line =>
      val uciMoves = normalizedLineMoves(line).take(maxPly)
      val steps = replayStepsPrefix(ctx.fen, uciMoves, Nil)
      val fullReplay = uciMoves.nonEmpty && steps.size == uciMoves.size
      val replayed = fullReplay || enginePrefixHasConcreteConsequence(ctx, line, steps)
      val trustedMate = if fullReplay then line.mate else None
      val evidence =
        buildEvidence(
          ctx = ctx,
          lineId = None,
          sanMoves = steps.map(_.san).ifEmpty(engineSanFallback(ctx.fen, line, maxPly)),
          uciMoves = if steps.nonEmpty then steps.map(_.uci) else uciMoves,
          scoreCp = Some(line.scoreCp),
          mate = trustedMate,
          depth = Option.when(line.depth > 0)(line.depth),
          steps = steps,
          release = if replayed then LineConsequenceRelease.ReplayBackedInternal else LineConsequenceRelease.DiagnosticOnly,
          rejectReasons = (List(EngineOnly) ++ Option.when(!replayed)(EngineReplayFailed)).distinct,
          maxPly = maxPly
        )
      if !fullReplay && evidence.kind == LineConsequenceKind.PreviewOnly then
        evidence.copy(release = LineConsequenceRelease.DiagnosticOnly)
      else evidence
    }

  private def refEvidence(
      ctx: NarrativeContext,
      variation: MoveReviewVariationRef,
      maxPly: Int
  ): LineConsequenceEvidence =
    val played = ctx.playedMove.map(MoveReviewPvLine.normalizeUci).filter(_.nonEmpty)
    val validated =
      played.flatMap(uci => MoveReviewPvLine.validatedLine(ctx.fen, variation, uci))
    val refMoves = validated.map(_.moves).getOrElse(variation.moves).take(maxPly)
    val uciMoves = refMoves.map(move => MoveReviewPvLine.normalizeUci(move.uci)).filter(_.nonEmpty)
    val sanMoves = refMoves.map(_.san.trim).filter(_.nonEmpty)
    val steps =
      validated
        .map(valid => replaySteps(ctx.fen, valid.moves.take(maxPly).map(_.uci), sanMoves))
        .getOrElse(Nil)
    val replayed = validated.nonEmpty && steps.nonEmpty

    buildEvidence(
      ctx = ctx,
      lineId = Some(variation.lineId),
      sanMoves = if replayed then steps.map(_.san) else sanMoves,
      uciMoves = uciMoves,
      scoreCp = Some(variation.scoreCp),
      mate = variation.mate,
      depth = Option.when(variation.depth > 0)(variation.depth),
      steps = steps,
      release = if replayed then LineConsequenceRelease.SurfaceCandidate else LineConsequenceRelease.DiagnosticOnly,
      rejectReasons = if replayed then Nil else List(RefReplayFailed),
      maxPly = maxPly
    )

  private def refEvidenceFromStart(
      ctx: NarrativeContext,
      variation: MoveReviewVariationRef,
      maxPly: Int,
      includeMinorPieceReroute: Boolean
  ): LineConsequenceEvidence =
    val validated = MoveReviewPvLine.validatedLineFromStart(ctx.fen, variation)
    val refMoves = validated.map(_.moves).getOrElse(variation.moves).take(maxPly)
    val uciMoves = refMoves.map(move => MoveReviewPvLine.normalizeUci(move.uci)).filter(_.nonEmpty)
    val sanMoves = refMoves.map(_.san.trim).filter(_.nonEmpty)
    val steps =
      validated
        .map(valid => replaySteps(ctx.fen, valid.moves.take(maxPly).map(_.uci), sanMoves))
        .getOrElse(Nil)
    val replayed = validated.nonEmpty && steps.nonEmpty

    buildEvidence(
      ctx = ctx,
      lineId = Some(variation.lineId),
      sanMoves = if replayed then steps.map(_.san) else sanMoves,
      uciMoves = uciMoves,
      scoreCp = Some(variation.scoreCp),
      mate = variation.mate,
      depth = Option.when(variation.depth > 0)(variation.depth),
      steps = steps,
      release = if replayed then LineConsequenceRelease.ReplayBackedInternal else LineConsequenceRelease.DiagnosticOnly,
      rejectReasons = if replayed then Nil else List(RefReplayFailed),
      maxPly = maxPly,
      includeMinorPieceReroute = includeMinorPieceReroute
    )

  private def buildEvidence(
      ctx: NarrativeContext,
      lineId: Option[String],
      sanMoves: List[String],
      uciMoves: List[String],
      scoreCp: Option[Int],
      mate: Option[Int],
      depth: Option[Int],
      steps: List[LineStep],
      release: LineConsequenceRelease,
      rejectReasons: List[String],
      maxPly: Int,
      includeMinorPieceReroute: Boolean = false
  ): LineConsequenceEvidence =
    val captureSteps = steps.filter(_.captures)
    val checkSteps = steps.filter(step => step.givesCheck || step.san.contains("+") || step.san.contains("#"))
    val centralPawnAdvance = centralPawnAdvanceStep(steps)
    val promotionRace = steps.find(step => step.promotes || advancedPassedPawn(step))
    val passedPawnCreation = steps.find(_.createsPassedPawn)
    val immediateOpponentPawnCapture = immediateOpponentPawnCaptureStep(ctx, steps)
    val immediateOpponentTargetPressure = immediateOpponentTargetPressureStep(ctx, steps)
    val playedMoveTargetPressure = playedMoveTargetPressureStep(ctx, steps)
    val delayedPawnCapture = delayedPawnCaptureStep(ctx, steps)
    val originSquareClearance = originSquareClearanceStep(ctx, steps)
    val minorPieceReroute = Option.when(includeMinorPieceReroute)(minorPieceRerouteStep(steps)).flatten
    val structureDetails = steps.flatMap(_.structureDetails).distinct
    val centralWitness = CentralBreakTimingWitness.exact(ctx)
    val centralStep =
      centralWitness.flatMap(witness =>
        steps.find(step => MoveReviewPvLine.normalizeUci(step.uci) == MoveReviewPvLine.normalizeUci(witness.breakMove))
      )
    val captureStartedExchange = steps.headOption.exists(_.captures) && captureSteps.size >= 2
    val kind =
      if release == LineConsequenceRelease.SurfaceCandidate && centralWitness.nonEmpty && centralStep.nonEmpty then
        LineConsequenceKind.CentralBreakTiming
      else if promotionRace.nonEmpty then LineConsequenceKind.PromotionRace
      else if passedPawnCreation.nonEmpty then LineConsequenceKind.PassedPawnCreation
      else if captureStartedExchange then LineConsequenceKind.ExchangeSequence
      else if centralPawnAdvance.nonEmpty then LineConsequenceKind.CentralPawnAdvance
      else if captureSteps.size >= 2 then LineConsequenceKind.ExchangeSequence
      else if checkSteps.size >= 2 || mate.exists(_ != 0) then LineConsequenceKind.ForcingCheckSequence
      else if captureSteps.exists(step => step.capturedRole.exists(_ != Pawn)) then LineConsequenceKind.MaterialTransition
      else if immediateOpponentPawnCapture.nonEmpty then LineConsequenceKind.ImmediateOpponentPawnCapture
      else if delayedPawnCapture.nonEmpty then LineConsequenceKind.DelayedPawnCapture
      else if immediateOpponentTargetPressure.nonEmpty then LineConsequenceKind.ImmediateOpponentTargetPressure
      else if playedMoveTargetPressure.nonEmpty then LineConsequenceKind.PlayedMoveTargetPressure
      else if originSquareClearance.nonEmpty then LineConsequenceKind.OriginSquareClearance
      else if minorPieceReroute.nonEmpty then LineConsequenceKind.MinorPieceReroute
      else LineConsequenceKind.PreviewOnly
    val trigger =
      kind match
        case LineConsequenceKind.CentralBreakTiming => centralStep.map(_.san)
        case LineConsequenceKind.PromotionRace      => promotionRace.map(_.san)
        case LineConsequenceKind.PassedPawnCreation => passedPawnCreation.map(_.san)
        case LineConsequenceKind.CentralPawnAdvance => centralPawnAdvance.map(_.san)
        case LineConsequenceKind.ExchangeSequence   => captureSteps.headOption.map(_.san)
        case LineConsequenceKind.ForcingCheckSequence => checkSteps.headOption.map(_.san)
        case LineConsequenceKind.MaterialTransition => captureSteps.headOption.map(_.san)
        case LineConsequenceKind.ImmediateOpponentPawnCapture => immediateOpponentPawnCapture.map(_.san)
        case LineConsequenceKind.ImmediateOpponentTargetPressure => immediateOpponentTargetPressure.map(_._1.san)
        case LineConsequenceKind.PlayedMoveTargetPressure => playedMoveTargetPressure.map(_._1.san)
        case LineConsequenceKind.DelayedPawnCapture => delayedPawnCapture.map(_.san)
        case LineConsequenceKind.OriginSquareClearance => originSquareClearance.map(_.san)
        case LineConsequenceKind.MinorPieceReroute => minorPieceReroute.map(_.san)
        case LineConsequenceKind.PreviewOnly        => sanMoves.headOption
    val targetDetails =
      if kind == LineConsequenceKind.ImmediateOpponentTargetPressure then
        immediateOpponentTargetPressure.toList.flatMap(_._2).distinct
      else if kind == LineConsequenceKind.PlayedMoveTargetPressure then
        playedMoveTargetPressure.toList.flatMap(_._2).distinct
      else Nil
    val (consequence, why) = wording(kind, trigger, sanMoves, structureDetails, targetDetails)
    LineConsequenceEvidence(
      lineId = lineId,
      sanMoves = sanMoves.take(maxPly),
      uciMoves = uciMoves.take(maxPly),
      scoreCp = scoreCp,
      mate = mate,
      depth = depth,
      windowPly = sanMoves.take(maxPly).size.max(uciMoves.take(maxPly).size),
      kind = kind,
      triggerSan = trigger,
      consequence = consequence,
      whyItMatters = why,
      release = release,
      rejectReasons = rejectReasons.distinct,
      structureDetails = structureDetails,
      targetDetails = targetDetails
    )

  private def wording(
      kind: LineConsequenceKind,
      trigger: Option[String],
      sanMoves: List[String],
      structureDetails: List[LineStructureDetail],
      targetDetails: List[LineTargetDetail]
  ): (String, Option[String]) =
    val triggerText = trigger.map(san => s" after $san").getOrElse("")
    kind match
      case LineConsequenceKind.CentralBreakTiming =>
        s"The checked line times the central break$triggerText." ->
          Some("That matters because the central break changes the pawn structure before counterplay settles.")
      case LineConsequenceKind.CentralPawnAdvance =>
        s"The checked line includes a central pawn advance$triggerText." ->
          Some("The local result is a changed central pawn structure.")
      case LineConsequenceKind.PassedPawnCreation =>
        s"The checked line creates a passed pawn$triggerText." ->
          Some("That matters because the resulting pawn can become a lasting conversion target.")
      case LineConsequenceKind.PromotionRace =>
        s"The checked line pushes a passed pawn toward promotion$triggerText." ->
          Some("The local result is a promotion race rather than only a move-order detail.")
      case LineConsequenceKind.ExchangeSequence =>
        s"The checked line reaches an exchange sequence$triggerText." ->
          primaryStructureDetailSurface(structureDetails)
            .map(detail => s"The decision is about the resulting structure: ${detail.stripPrefix("leaving ")}.")
            .orElse(Some("The decision is about which pieces and pawns remain."))
      case LineConsequenceKind.ForcingCheckSequence =>
        s"The checked line becomes forcing$triggerText." ->
          Some("Checks narrow the replies before the position can settle.")
      case LineConsequenceKind.MaterialTransition =>
        s"The checked line changes material$triggerText." ->
          Some("The point is the resulting material balance, not just the first move.")
      case LineConsequenceKind.ImmediateOpponentPawnCapture =>
        s"The checked line meets the move with an immediate pawn capture$triggerText." ->
          Some("The local result is a pawn being taken right after the reviewed move.")
      case LineConsequenceKind.ImmediateOpponentTargetPressure =>
        s"The checked line meets the move with immediate target pressure$triggerText." ->
          targetPressureDetailSurface(targetDetails)
            .map(targets => s"The reply puts pressure on $targets near the king.")
            .orElse(Some("The local result is immediate pressure around the king after the reply."))
      case LineConsequenceKind.PlayedMoveTargetPressure =>
        s"The checked line keeps target pressure tied to the move$triggerText." ->
          targetPressureDetailSurface(targetDetails)
            .map(targets => s"The local result is pressure on $targets, backed by the checked continuation.")
            .orElse(Some("The local result is target pressure backed by the checked continuation."))
      case LineConsequenceKind.DelayedPawnCapture =>
        s"The checked line reaches a delayed pawn capture$triggerText." ->
          Some("The point is the later pawn capture, not just the first move.")
      case LineConsequenceKind.OriginSquareClearance =>
        val leadText = sanMoves.headOption.map(san => s" by $san").getOrElse(" by the first move")
        val reuseText = trigger.map(san => s" when $san arrives").getOrElse("")
        s"The checked line later uses the square cleared$leadText$reuseText." ->
          Some("The first move frees its original square for another piece on the checked branch.")
      case LineConsequenceKind.MinorPieceReroute =>
        s"The checked line shows a minor-piece reroute$triggerText." ->
          Some("The same minor piece keeps moving on the checked branch.")
      case LineConsequenceKind.PreviewOnly =>
        val preview = sanMoves.take(4).mkString(" ")
        s"The checked line continues ${preview.trim}.".trim ->
          None

  private[analysis] def replaySteps(
      fen: String,
      rawMoves: List[String],
      preferredSan: List[String]
  ): List[LineStep] =
    replayStepsWithMode(fen, rawMoves, preferredSan, allowLegalPrefix = false)

  private[analysis] def replayStepsPrefix(
      fen: String,
      rawMoves: List[String],
      preferredSan: List[String]
  ): List[LineStep] =
    replayStepsWithMode(fen, rawMoves, preferredSan, allowLegalPrefix = true)

  private def replayStepsWithMode(
      fen: String,
      rawMoves: List[String],
      preferredSan: List[String],
      allowLegalPrefix: Boolean
  ): List[LineStep] =
    val normalized = rawMoves.map(MoveReviewPvLine.normalizeUci).filter(isUci)
    val start = Fen.read(Standard, Fen.Full(fen))
    var current = start
    val accepted = scala.collection.mutable.ListBuffer.empty[LineStep]
    val it = normalized.zipWithIndex.iterator
    var ok = true
    while it.hasNext && ok do
      val (uci, idx) = it.next()
      current.flatMap(position =>
        MoveReviewExchangeAnalyzer.legalMove(position, uci).map(position -> _)
      ) match
        case Some((position, move)) =>
          val san = preferredSan.lift(idx).map(_.trim).filter(_.nonEmpty).getOrElse(move.toSanStr.toString)
          val capturedRole = position.board.pieceAt(move.dest).map(_.role)
          val mover = move.piece.color
          val beforePassed = passedPawns(position.board, mover)
          val afterPassed = passedPawns(move.after.board, mover)
          val createsPassedPawn =
            move.piece.role == Pawn &&
              afterPassed.contains(move.dest) &&
              !beforePassed.contains(move.orig)
          val advancesPassedPawn =
            move.piece.role == Pawn &&
              afterPassed.contains(move.dest) &&
              beforePassed.contains(move.orig)
          accepted += LineStep(
            san = san,
            uci = uci,
            orig = move.orig,
            dest = move.dest,
            role = move.piece.role,
            color = mover,
            captures = move.captures,
            capturedRole = capturedRole,
            givesCheck = move.after.check.yes,
            createsPassedPawn = createsPassedPawn,
            advancesPassedPawn = advancesPassedPawn,
            promotes = move.promotion.nonEmpty,
            structureDetails = Nil
          )
          current = Some(move.after)
        case None =>
          ok = false
    if ok || allowLegalPrefix then
      val replayedSteps = accepted.toList
      val details =
        start.zip(current).map { case (startPosition, finalPosition) =>
          val files = replayedSteps.flatMap(structuralFiles).toSet
          lineStructureDetails(startPosition.board, finalPosition.board, files)
        }.getOrElse(Nil)
      replayedSteps.map(_.copy(structureDetails = details))
    else Nil

  private def enginePrefixHasConcreteConsequence(
      ctx: NarrativeContext,
      line: VariationLine,
      steps: List[LineStep]
  ): Boolean =
    steps.nonEmpty &&
      buildEvidence(
        ctx = ctx,
        lineId = None,
        sanMoves = steps.map(_.san),
        uciMoves = steps.map(_.uci),
        scoreCp = Some(line.scoreCp),
        mate = None,
        depth = Option.when(line.depth > 0)(line.depth),
        steps = steps,
        release = LineConsequenceRelease.ReplayBackedInternal,
        rejectReasons = List(EngineOnly, EngineReplayFailed),
        maxPly = steps.size
      ).kind != LineConsequenceKind.PreviewOnly

  private def centralPawnAdvanceStep(steps: List[LineStep]): Option[LineStep] =
    steps.find { step =>
      step.role == Pawn &&
        !step.captures &&
        step.orig.file == step.dest.file &&
        Set(File.D, File.E).contains(step.orig.file) &&
        Set(Square.D4, Square.E4, Square.D5, Square.E5).contains(step.dest)
    }

  private def minorPieceRerouteStep(steps: List[LineStep]): Option[LineStep] =
    steps.headOption
      .filter(step => Set(Knight, Bishop).contains(step.role))
      .filterNot(_.captures)
      .flatMap { lead =>
        val current = lead.dest
        steps.drop(1).zipWithIndex.collectFirst {
          case (step, idx)
              if idx <= 3 &&
                step.color == lead.color &&
                step.role == lead.role &&
                step.orig == current &&
                !step.captures =>
            step
        }
      }

  private[analysis] def originSquareClearanceStep(
      ctx: NarrativeContext,
      steps: List[LineStep]
  ): Option[LineStep] =
    val playedUci = ctx.playedMove.map(MoveReviewPvLine.normalizeUci).filter(_.nonEmpty)
    steps.headOption
      .filter(step => !step.captures && playedUci.exists(_ == MoveReviewPvLine.normalizeUci(step.uci)))
      .filter(step => Set(Knight, Bishop, Rook, Queen).contains(step.role))
      .flatMap { lead =>
        steps.drop(1).zipWithIndex.collectFirst {
          case (step, idx)
              if idx <= 5 &&
                step.color == lead.color &&
                step.dest == lead.orig &&
                !step.captures &&
                step.role != Pawn &&
                step.role != King &&
                step.role != lead.role &&
                steps.drop(1).take(idx).forall(previous => previous.dest != lead.orig) =>
            step
        }
      }

  private def delayedPawnCaptureStep(ctx: NarrativeContext, steps: List[LineStep]): Option[LineStep] =
    val playedUci = ctx.playedMove.map(MoveReviewPvLine.normalizeUci).filter(_.nonEmpty)
    val lead = steps.headOption
    Option
      .when(
        lead.exists(step =>
          !step.captures &&
            step.role == Bishop &&
            playedUci.exists(_ == MoveReviewPvLine.normalizeUci(step.uci))
        )
      ) {
        steps.zipWithIndex.collectFirst {
          case (step, idx)
              if idx >= 2 &&
                step.captures &&
                step.capturedRole.contains(Pawn) &&
                lead.exists(_.color != step.color) =>
            step
        }
      }
      .flatten

  private def immediateOpponentPawnCaptureStep(ctx: NarrativeContext, steps: List[LineStep]): Option[LineStep] =
    val playedUci = ctx.playedMove.map(MoveReviewPvLine.normalizeUci).filter(_.nonEmpty)
    for
      lead <- steps.headOption
      if !lead.captures
      if playedUci.exists(_ == MoveReviewPvLine.normalizeUci(lead.uci))
      reply <- steps.lift(1)
      if reply.color != lead.color
      if reply.captures && reply.capturedRole.contains(Pawn)
    yield reply

  private def immediateOpponentTargetPressureStep(
      ctx: NarrativeContext,
      steps: List[LineStep]
  ): Option[(LineStep, List[LineTargetDetail])] =
    val playedUci = ctx.playedMove.map(MoveReviewPvLine.normalizeUci).filter(_.nonEmpty)
    for
      lead <- steps.headOption
      if !lead.captures
      if playedUci.exists(_ == MoveReviewPvLine.normalizeUci(lead.uci))
      reply <- steps.lift(1)
      if reply.color != lead.color
      if !reply.captures
      afterLead <- boardAfter(ctx.fen, steps.take(1).map(_.uci))
      afterReply <- boardAfter(ctx.fen, steps.take(2).map(_.uci))
      details = newKingZoneTargetPressureDetails(afterLead, afterReply, lead.color, reply.dest)
      if details.nonEmpty
    yield reply -> details

  private def playedMoveTargetPressureStep(
      ctx: NarrativeContext,
      steps: List[LineStep]
  ): Option[(LineStep, List[LineTargetDetail])] =
    val playedUci = ctx.playedMove.map(MoveReviewPvLine.normalizeUci).filter(_.nonEmpty)
    for
      lead <- steps.headOption
      if !lead.captures
      if lead.role == Queen || lead.role == Rook
      if playedUci.exists(_ == MoveReviewPvLine.normalizeUci(lead.uci))
      afterLead <- boardAfter(ctx.fen, steps.take(1).map(_.uci))
      details = playedMoveTargetPressureDetails(afterLead, lead.color, lead.dest)
      if details.nonEmpty
      if firstContinuationByPlayedPiece(steps).nonEmpty
    yield lead -> details

  private[analysis] def firstContinuationByPlayedPiece(steps: List[LineStep]): Option[LineStep] =
    steps.headOption.flatMap { lead =>
      def loop(current: Square, rest: List[LineStep]): Option[LineStep] =
        rest match
          case Nil => None
          case step :: _ if step.color != lead.color && step.captures && step.dest == current =>
            None
          case step :: _ if step.color == lead.color && step.role == lead.role && step.orig == current =>
            Some(step)
          case _ :: tail =>
            loop(current, tail)
      loop(lead.dest, steps.drop(1))
    }

  private def boardAfter(fen: String, rawMoves: List[String]): Option[Board] =
    var current = Fen.read(Standard, Fen.Full(fen))
    var ok = true
    val it = rawMoves.map(MoveReviewPvLine.normalizeUci).filter(isUci).iterator
    while it.hasNext && ok do
      val uci = it.next()
      current.flatMap(position => MoveReviewExchangeAnalyzer.legalMove(position, uci).map(_.after)) match
        case Some(next) => current = Some(next)
        case None       => ok = false
    Option.when(ok)(current.map(_.board)).flatten

  private def newKingZoneTargetPressureDetails(
      beforeReply: Board,
      afterReply: Board,
      pressuredSide: Color,
      attacker: Square
  ): List[LineTargetDetail] =
    val beforeTargets = targetSquaresAttackedFrom(beforeReply, pressuredSide, attacker).toSet
    FactExtractor
      .extractStaticFacts(afterReply, pressuredSide)
      .collect {
        case Fact.HangingPiece(square, role, attackers, _, _) if attackers.contains(attacker) =>
          targetDetail(afterReply, pressuredSide, attacker, square, role, KingZoneTargetPressureDetail)
        case Fact.TargetPiece(square, role, attackers, _, _) if attackers.contains(attacker) =>
          targetDetail(afterReply, pressuredSide, attacker, square, role, KingZoneTargetPressureDetail)
      }
      .flatten
      .filter(detail => !beforeTargets.contains(detail.square))
      .sortBy(detail => (if detail.role == "pawn" then 1 else 0, detail.square))
      .take(2)

  private def playedMoveTargetPressureDetails(
      board: Board,
      pressureSide: Color,
      attacker: Square
  ): List[LineTargetDetail] =
    val pressuredSide = !pressureSide
    FactExtractor
      .extractStaticFacts(board, pressuredSide)
      .collect {
        case Fact.HangingPiece(square, Pawn, attackers, _, _) if attackers.contains(attacker) =>
          advancedPawnTargetDetail(pressuredSide, attacker, square)
        case Fact.TargetPiece(square, Pawn, attackers, _, _) if attackers.contains(attacker) =>
          advancedPawnTargetDetail(pressuredSide, attacker, square)
      }
      .flatten
      .distinct
      .sortBy(_.square)
      .take(2)

  private def advancedPawnTargetDetail(
      weakSide: Color,
      attacker: Square,
      target: Square
  ): Option[LineTargetDetail] =
    Option.when(advancedPawnTarget(target, weakSide)) {
      LineTargetDetail(
        kind = AdvancedPawnTargetPressureDetail,
        square = target.key,
        role = roleKey(Pawn),
        attacker = Some(attacker.key),
        side = Some(sideKey(weakSide))
      )
    }

  private def advancedPawnTarget(target: Square, weakSide: Color): Boolean =
    target.key.lift(1).exists { rankChar =>
      val rank = rankChar.asDigit
      if weakSide.white then rank >= 5 else rank <= 4
    }

  private def targetSquaresAttackedFrom(board: Board, pressuredSide: Color, attacker: Square): List[String] =
    FactExtractor
      .extractStaticFacts(board, pressuredSide)
      .collect {
        case Fact.HangingPiece(square, _, attackers, _, _) if attackers.contains(attacker) => square.key
        case Fact.TargetPiece(square, _, attackers, _, _) if attackers.contains(attacker)  => square.key
      }

  private def targetDetail(
      board: Board,
      pressuredSide: Color,
      attacker: Square,
      target: Square,
      role: Role,
      kind: String
  ): Option[LineTargetDetail] =
    Option.when(role != King && kingZoneTarget(board, pressuredSide, target)) {
      LineTargetDetail(
        kind = kind,
        square = target.key,
        role = roleKey(role),
        attacker = Some(attacker.key),
        side = Some(sideKey(pressuredSide))
      )
    }

  private def kingZoneTarget(board: Board, side: Color, target: Square): Boolean =
    board.kingPosOf(side).exists { king =>
      (king.file.value - target.file.value).abs <= 2 &&
        (king.rank.value - target.rank.value).abs <= 2
    }

  private def advancedPassedPawn(step: LineStep): Boolean =
    step.advancesPassedPawn &&
      step.role == Pawn &&
      (if step.color.white then step.dest.rank.value >= 5 else step.dest.rank.value <= 2)

  private def passedPawns(board: Board, color: Color): List[Square] =
    PositionAnalyzer.passedPawns(color, board.pawns & board.byColor(color), board.pawns & board.byColor(!color))

  private[analysis] def primaryStructureDetailSurface(details: List[LineStructureDetail]): Option[String] =
    details.sortBy(structureDetailPriority).flatMap(structureDetailSurface).headOption

  private[analysis] def structureDetailSurface(detail: LineStructureDetail): Option[String] =
    detail.kind match
      case WeaknessTargetProfile.IQP =>
        detail.square.map(square => withSide(detail.side, s"an isolated queen pawn on $square"))
      case WeaknessTargetProfile.IsolatedPawn =>
        detail.square.map(square => withSide(detail.side, s"an isolated pawn on $square"))
      case WeaknessTargetProfile.DoubledPawn =>
        detail.file
          .map(file => withSide(detail.side, s"doubled pawns on the $file-file"))
          .orElse(detail.square.map(square => withSide(detail.side, s"doubled-pawn target on $square")))
      case WeaknessTargetProfile.BackwardPawn =>
        detail.square.map(square => withSide(detail.side, s"a backward pawn target on $square"))
      case OpenFileDetail =>
        detail.file.map(file => s"opening the $file-file")
      case SemiOpenFileDetail =>
        detail.file.map(file => s"leaving a semi-open $file-file${detail.side.map(side => s" for ${side.capitalize}").getOrElse("")}")
      case _ =>
        None

  private def withSide(side: Option[String], text: String): String =
    s"leaving ${side.map(value => s"${value.capitalize} with ").getOrElse("")}$text"

  private[analysis] def targetPressureDetailSurface(details: List[LineTargetDetail]): Option[String] =
    val targetTexts =
      details
        .map(detail => s"the ${detail.role} on ${detail.square}")
        .distinct
        .take(2)
    targetTexts match
      case Nil          => None
      case single :: Nil => Some(single)
      case first :: second :: Nil => Some(s"$first and $second")
      case first :: rest => Some((first :: rest).mkString(", "))

  private def structureDetailPriority(detail: LineStructureDetail): Int =
    detail.kind match
      case WeaknessTargetProfile.IQP          => 0
      case WeaknessTargetProfile.DoubledPawn  => 1
      case WeaknessTargetProfile.IsolatedPawn => 2
      case WeaknessTargetProfile.BackwardPawn => 3
      case OpenFileDetail                     => 4
      case SemiOpenFileDetail                 => 5
      case _                                  => 99

  private def lineStructureDetails(
      beforeBoard: Board,
      afterBoard: Board,
      localFiles: Set[Char]
  ): List[LineStructureDetail] =
    (
      List(Color.White, Color.Black).flatMap(side => newWeaknessDetails(beforeBoard, afterBoard, side, localFiles)) ++
        newOpenFileDetails(beforeBoard, afterBoard, localFiles) ++
        List(Color.White, Color.Black).flatMap(side => newSemiOpenFileDetails(beforeBoard, afterBoard, side, localFiles))
    ).distinct

  private def newWeaknessDetails(
      beforeBoard: Board,
      afterBoard: Board,
      weakSide: Color,
      localFiles: Set[Char]
  ): List[LineStructureDetail] =
    val beforeTargets = WeaknessTargetProfile.forWeakSide(beforeBoard, weakSide).map(_.targetSquare).toSet
    WeaknessTargetProfile
      .forWeakSide(afterBoard, weakSide)
      .filter(profile =>
        Set(
          WeaknessTargetProfile.IQP,
          WeaknessTargetProfile.IsolatedPawn,
          WeaknessTargetProfile.DoubledPawn,
          WeaknessTargetProfile.BackwardPawn
        ).contains(profile.kind) &&
          profile.targetSquare.headOption.exists(localFiles.contains) &&
          !beforeTargets.contains(profile.targetSquare)
      )
      .map(profile =>
        LineStructureDetail(
          kind = profile.kind,
          square = Some(profile.targetSquare),
          file = profile.targetSquare.headOption.map(_.toString),
          side = Some(sideKey(profile.weakSide))
        )
      )

  private def newOpenFileDetails(
      beforeBoard: Board,
      afterBoard: Board,
      localFiles: Set[Char]
  ): List[LineStructureDetail] =
    val before = openFiles(beforeBoard)
    openFiles(afterBoard)
      .diff(before)
      .filter(file => file.headOption.exists(localFiles.contains))
      .toList
      .sorted
      .map(file => LineStructureDetail(kind = OpenFileDetail, file = Some(file)))

  private def newSemiOpenFileDetails(
      beforeBoard: Board,
      afterBoard: Board,
      side: Color,
      localFiles: Set[Char]
  ): List[LineStructureDetail] =
    val before = semiOpenFiles(beforeBoard, side)
    semiOpenFiles(afterBoard, side)
      .diff(before)
      .filter(file => file.headOption.exists(localFiles.contains))
      .toList
      .sorted
      .map(file => LineStructureDetail(kind = SemiOpenFileDetail, file = Some(file), side = Some(sideKey(side))))

  private def openFiles(board: Board): Set[String] =
    AllFiles.filter(file => pawnsOnFile(board, file).isEmpty).map(_.toString).toSet

  private def semiOpenFiles(board: Board, side: Color): Set[String] =
    AllFiles
      .filter(file =>
        val pawns = pawnsOnFile(board, file)
        pawns.nonEmpty && pawns.forall(square => board.pieceAt(square).exists(_.color != side))
      )
      .map(_.toString)
      .toSet

  private def pawnsOnFile(board: Board, file: Char): List[Square] =
    Square.all.toList.filter(square =>
      square.key.headOption.contains(file) &&
        board.pieceAt(square).exists(_.role == Pawn)
    )

  private def sideKey(side: Color): String =
    if side.white then "white" else "black"

  private def roleKey(role: Role): String =
    role.toString.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase

  private def localFiles(orig: Square, dest: Square): Set[Char] =
    List(orig, dest).flatMap(_.key.headOption).toSet

  private def structuralFiles(step: LineStep): Set[Char] =
    if step.role == Pawn || step.capturedRole.contains(Pawn) then localFiles(step.orig, step.dest)
    else Set.empty

  private def normalizedLineMoves(line: VariationLine): List[String] =
    val raw =
      if line.moves.nonEmpty then line.moves
      else line.parsedMoves.map(_.uci)
    raw.map(MoveReviewPvLine.normalizeUci).filter(isUci)

  private def engineSanFallback(fen: String, line: VariationLine, maxPly: Int): List[String] =
    LineScopedCitation.sanMoves(fen, line).take(maxPly).map(_.trim).filter(_.nonEmpty)

  private def isUci(raw: String): Boolean =
    raw.matches("""[a-h][1-8][a-h][1-8][qrbn]?""")

  private val AllFiles = ('a' to 'h').toList
  private val OpenFileDetail = "open_file"
  private val SemiOpenFileDetail = "semi_open_file"
  private val KingZoneTargetPressureDetail = "king_zone_target_pressure"
  private val AdvancedPawnTargetPressureDetail = "advanced_pawn_target_pressure"

extension [A](values: List[A])
  private def ifEmpty(fallback: => List[A]): List[A] =
    if values.nonEmpty then values else fallback
