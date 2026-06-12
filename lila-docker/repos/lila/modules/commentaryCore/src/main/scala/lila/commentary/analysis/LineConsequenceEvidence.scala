package lila.commentary.analysis

import chess.*
import chess.format.Fen
import chess.variant.Standard
import lila.commentary.{ MoveReviewRefs, MoveReviewVariationRef }
import lila.commentary.model.NarrativeContext
import lila.commentary.model.strategic.VariationLine

enum LineConsequenceKind:
  case PreviewOnly, ExchangeSequence, ForcingCheckSequence, CentralBreakTiming, CentralPawnAdvance, MaterialTransition,
    PassedPawnCreation, PromotionRace

enum LineConsequenceRelease:
  case SurfaceCandidate, ReplayBackedInternal, DiagnosticOnly

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
    rejectReasons: List[String]
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
      promotes: Boolean
  )

  def surfaceCandidate(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      maxPly: Int = SurfaceMaxPly
  ): Option[LineConsequenceEvidence] =
    val candidates = fromRefs(ctx, refs, maxPly).filter(_.surfaceReady)
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

  def narrativeCandidate(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      maxPly: Int = InternalMaxPly
  ): Option[LineConsequenceEvidence] =
    val refEvidence = fromRefsOnly(ctx, refs, maxPly)
    refEvidence.find(_.surfaceReady)
      .orElse(refEvidence.find(_.narrativeReady))
      .orElse(fromEngine(ctx, maxPly).find(_.narrativeReady))

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
        refsValue.variations.map(refEvidenceFromStart(ctx, _, maxPly))
      case _ =>
        Nil

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
      maxPly: Int
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
      maxPly = maxPly
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
      maxPly: Int
  ): LineConsequenceEvidence =
    val captureSteps = steps.filter(_.captures)
    val checkSteps = steps.filter(step => step.givesCheck || step.san.contains("+") || step.san.contains("#"))
    val centralPawnAdvance = centralPawnAdvanceStep(steps)
    val promotionRace = steps.find(step => step.promotes || advancedPassedPawn(step))
    val passedPawnCreation = steps.find(_.createsPassedPawn)
    val centralWitness = CentralBreakTimingWitness.exact(ctx)
    val centralStep =
      centralWitness.flatMap(witness =>
        steps.find(step => MoveReviewPvLine.normalizeUci(step.uci) == MoveReviewPvLine.normalizeUci(witness.breakMove))
      )
    val kind =
      if release == LineConsequenceRelease.SurfaceCandidate && centralWitness.nonEmpty && centralStep.nonEmpty then
        LineConsequenceKind.CentralBreakTiming
      else if promotionRace.nonEmpty then LineConsequenceKind.PromotionRace
      else if passedPawnCreation.nonEmpty then LineConsequenceKind.PassedPawnCreation
      else if centralPawnAdvance.nonEmpty then LineConsequenceKind.CentralPawnAdvance
      else if captureSteps.size >= 2 then LineConsequenceKind.ExchangeSequence
      else if checkSteps.size >= 2 || mate.exists(_ != 0) then LineConsequenceKind.ForcingCheckSequence
      else if captureSteps.exists(step => step.capturedRole.exists(_ != Pawn)) then LineConsequenceKind.MaterialTransition
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
        case LineConsequenceKind.PreviewOnly        => sanMoves.headOption
    val (consequence, why) = wording(kind, trigger, sanMoves)
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
      rejectReasons = rejectReasons.distinct
    )

  private def wording(
      kind: LineConsequenceKind,
      trigger: Option[String],
      sanMoves: List[String]
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
          Some("The decision is about which structure remains.")
      case LineConsequenceKind.ForcingCheckSequence =>
        s"The checked line becomes forcing$triggerText." ->
          Some("Checks narrow the replies before the position can settle.")
      case LineConsequenceKind.MaterialTransition =>
        s"The checked line changes material$triggerText." ->
          Some("The point is the resulting material balance, not just the first move.")
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
    var current = Fen.read(Standard, Fen.Full(fen))
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
            promotes = move.promotion.nonEmpty
          )
          current = Some(move.after)
        case None =>
          ok = false
    if ok || allowLegalPrefix then accepted.toList else Nil

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

  private def advancedPassedPawn(step: LineStep): Boolean =
    step.advancesPassedPawn &&
      step.role == Pawn &&
      (if step.color.white then step.dest.rank.value >= 5 else step.dest.rank.value <= 2)

  private def passedPawns(board: Board, color: Color): List[Square] =
    PositionAnalyzer.passedPawns(color, board.pawns & board.byColor(color), board.pawns & board.byColor(!color))

  private def normalizedLineMoves(line: VariationLine): List[String] =
    val raw =
      if line.moves.nonEmpty then line.moves
      else line.parsedMoves.map(_.uci)
    raw.map(MoveReviewPvLine.normalizeUci).filter(isUci)

  private def engineSanFallback(fen: String, line: VariationLine, maxPly: Int): List[String] =
    LineScopedCitation.sanMoves(fen, line).take(maxPly).map(_.trim).filter(_.nonEmpty)

  private def isUci(raw: String): Boolean =
    raw.matches("""[a-h][1-8][a-h][1-8][qrbn]?""")

extension [A](values: List[A])
  private def ifEmpty(fallback: => List[A]): List[A] =
    if values.nonEmpty then values else fallback
