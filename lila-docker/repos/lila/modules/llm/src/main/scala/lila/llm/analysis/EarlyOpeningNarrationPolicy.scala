package lila.llm.analysis

import lila.llm.model.*
import lila.llm.model.authoring.{ NarrativeOutline, OutlineBeatKind }

private[llm] object EarlyOpeningNarrationPolicy:

  val StandardVariant = "standard"
  val Chess960Variant = "chess960"
  val CollapsePlyCutoff = 10
  val MaxCollapsedSentences = 2

  private val CompactBeatKinds = Set(
    OutlineBeatKind.MoveHeader,
    OutlineBeatKind.Context,
    OutlineBeatKind.MainMove,
    OutlineBeatKind.OpeningTheory
  )

  def normalizeVariantKey(raw: Option[String]): String =
    Option(raw).flatten
      .map(_.trim.toLowerCase)
      .filter(_.nonEmpty)
      .collect {
        case Chess960Variant | "freestyle" | "960" => Chess960Variant
      }
      .getOrElse(StandardVariant)

  def collapsedEarlyOpening(ctx: NarrativeContext): Boolean =
    collapsedEarlyOpening(ctx, None)

  def collapsedEarlyOpening(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    isStandard(ctx.variantKey) &&
      openingLike(ctx) &&
      ctx.ply <= CollapsePlyCutoff &&
      !hasEscapeHatch(ctx, truthContract)

  def compactOutline(ctx: NarrativeContext, outline: NarrativeOutline): NarrativeOutline =
    compactOutline(ctx, outline, None)

  def compactOutline(
      ctx: NarrativeContext,
      outline: NarrativeOutline,
      truthContract: Option[DecisiveTruthContract]
  ): NarrativeOutline =
    if !collapsedEarlyOpening(ctx, truthContract) then outline
    else
      val compactBeats = outline.beats.filter(beat => CompactBeatKinds.contains(beat.kind))
      if compactBeats.nonEmpty then NarrativeOutline(compactBeats, outline.diagnostics)
      else outline

  def clampNarrative(ctx: NarrativeContext, text: String): String =
    clampNarrative(ctx, text, None)

  def clampNarrative(
      ctx: NarrativeContext,
      text: String,
      truthContract: Option[DecisiveTruthContract]
  ): String =
    if !collapsedEarlyOpening(ctx, truthContract) then text
    else limitSentences(text, MaxCollapsedSentences)

  private def isStandard(variantKey: String): Boolean =
    normalizeVariantKey(Option(variantKey)) == StandardVariant

  private def openingLike(ctx: NarrativeContext): Boolean =
    Option(ctx.phase.current).exists(_.trim.equalsIgnoreCase("opening")) ||
      Option(ctx.header.phase).exists(_.trim.equalsIgnoreCase("opening")) ||
      ctx.openingEvent.isDefined

  private def hasEscapeHatch(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    hasMeaningfulOpeningEvent(ctx) ||
      StandardCommentaryClaimPolicy.hasDurableStructuralCommitment(ctx) ||
      TacticalTensionPolicy.hasForcedOrCriticalState(ctx, truthContract) ||
      TacticalTensionPolicy.hasStrongTacticalPressure(ctx, truthContract) ||
      TacticalTensionPolicy.hasSevereCounterfactual(ctx, truthContract)

  private def hasMeaningfulOpeningEvent(ctx: NarrativeContext): Boolean =
    ctx.openingEvent.exists {
      case OpeningEvent.BranchPoint(_, _, _) => true
      case OpeningEvent.OutOfBook(_, _, _)   => true
      case OpeningEvent.TheoryEnds(_, _)     => true
      case OpeningEvent.Novelty(_, _, _, _)  => true
      case OpeningEvent.Intro(_, _, _, _)    => false
    }

  private def limitSentences(text: String, maxSentences: Int): String =
    val sentences =
      Option(text)
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(_.split("(?<=[.!?])\\s+").toList.map(_.trim).filter(_.nonEmpty))
        .getOrElse(Nil)
    if sentences.size <= maxSentences then Option(text).getOrElse("").trim
    else sentences.take(maxSentences).mkString(" ").trim
