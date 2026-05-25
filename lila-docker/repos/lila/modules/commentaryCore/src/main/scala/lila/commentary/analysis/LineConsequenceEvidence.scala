package lila.commentary.analysis

import chess.*
import chess.format.{ Fen, Uci }
import chess.variant.Standard
import lila.commentary.{ MoveReviewRefs, MoveReviewVariationRef }
import lila.commentary.model.NarrativeContext
import lila.commentary.model.strategic.VariationLine

enum LineConsequenceKind:
  case PreviewOnly, ExchangeSequence, ForcingCheckSequence, CentralBreakTiming, CentralPawnAdvance, MaterialTransition

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
      from: String,
      to: String,
      role: Role,
      captures: Boolean,
      capturedRole: Option[Role],
      givesCheck: Boolean
  )

  def surfaceCandidate(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      maxPly: Int = SurfaceMaxPly
  ): Option[LineConsequenceEvidence] =
    fromRefs(ctx, refs, maxPly).find(_.surfaceReady)

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

  def fromEngine(ctx: NarrativeContext, maxPly: Int = SurfaceMaxPly): List[LineConsequenceEvidence] =
    ctx.engineEvidence.toList.flatMap(_.variations).map { line =>
      val uciMoves = normalizedLineMoves(line).take(maxPly)
      val steps = replaySteps(ctx.fen, uciMoves, Nil)
      val replayed = steps.nonEmpty
      buildEvidence(
        ctx = ctx,
        lineId = None,
        sanMoves = steps.map(_.san).ifEmpty(engineSanFallback(ctx.fen, line, maxPly)),
        uciMoves = uciMoves,
        scoreCp = Some(line.scoreCp),
        mate = line.mate,
        depth = Option.when(line.depth > 0)(line.depth),
        steps = steps,
        release = if replayed then LineConsequenceRelease.ReplayBackedInternal else LineConsequenceRelease.DiagnosticOnly,
        rejectReasons = (List(EngineOnly) ++ Option.when(!replayed)(EngineReplayFailed)).distinct,
        maxPly = maxPly
      )
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
    val centralWitness = CentralBreakTimingWitness.exact(ctx)
    val centralStep =
      centralWitness.flatMap(witness =>
        steps.find(step => MoveReviewPvLine.normalizeUci(step.uci) == MoveReviewPvLine.normalizeUci(witness.breakMove))
      )
    val kind =
      if release == LineConsequenceRelease.SurfaceCandidate && centralWitness.nonEmpty && centralStep.nonEmpty then
        LineConsequenceKind.CentralBreakTiming
      else if centralPawnAdvance.nonEmpty then LineConsequenceKind.CentralPawnAdvance
      else if captureSteps.size >= 2 then LineConsequenceKind.ExchangeSequence
      else if checkSteps.size >= 2 || mate.exists(_ != 0) then LineConsequenceKind.ForcingCheckSequence
      else if captureSteps.exists(step => step.capturedRole.exists(_ != Pawn)) then LineConsequenceKind.MaterialTransition
      else LineConsequenceKind.PreviewOnly
    val trigger =
      kind match
        case LineConsequenceKind.CentralBreakTiming => centralStep.map(_.san)
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
    val normalized = rawMoves.map(MoveReviewPvLine.normalizeUci).filter(isUci)
    var current = Fen.read(Standard, Fen.Full(fen))
    val accepted = scala.collection.mutable.ListBuffer.empty[LineStep]
    val it = normalized.zipWithIndex.iterator
    var ok = true
    while it.hasNext && ok do
      val (uci, idx) = it.next()
      current.flatMap(position => legalMove(position, uci).map(position -> _)) match
        case Some((position, move)) =>
          val san = preferredSan.lift(idx).map(_.trim).filter(_.nonEmpty).getOrElse(move.toSanStr.toString)
          val capturedRole = position.board.pieceAt(move.dest).map(_.role)
          accepted += LineStep(
            san = san,
            uci = uci,
            from = uci.slice(0, 2),
            to = uci.slice(2, 4),
            role = move.piece.role,
            captures = move.captures,
            capturedRole = capturedRole,
            givesCheck = move.after.check.yes
          )
          current = Some(move.after)
        case None =>
          ok = false
    if ok then accepted.toList else Nil

  private def legalMove(position: Position, uci: String): Option[Move] =
    Uci(uci).collect { case move: Uci.Move => move }.flatMap(position.move(_).toOption)

  private def centralPawnAdvanceStep(steps: List[LineStep]): Option[LineStep] =
    steps.find { step =>
      step.role == Pawn &&
        !step.captures &&
        step.from.take(1) == step.to.take(1) &&
        Set("d", "e").contains(step.from.take(1)) &&
        Set("d4", "e4", "d5", "e5").contains(step.to)
    }

  private def normalizedLineMoves(line: VariationLine): List[String] =
    val raw =
      if line.moves.nonEmpty then line.moves
      else line.parsedMoves.map(_.uci)
    raw.map(MoveReviewPvLine.normalizeUci).filter(isUci)

  private def engineSanFallback(fen: String, line: VariationLine, maxPly: Int): List[String] =
    if line.parsedMoves.nonEmpty then line.parsedMoves.take(maxPly).map(_.san.trim).filter(_.nonEmpty)
    else NarrativeUtils.uciListToSan(fen, line.moves.take(maxPly)).map(_.trim).filter(_.nonEmpty)

  private def isUci(raw: String): Boolean =
    raw.matches("""[a-h][1-8][a-h][1-8][qrbn]?""")

extension [A](values: List[A])
  private def ifEmpty(fallback: => List[A]): List[A] =
    if values.nonEmpty then values else fallback
