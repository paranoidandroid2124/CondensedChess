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
    isStandard(ctx.variantKey) &&
      openingLike(ctx) &&
      ctx.ply <= CollapsePlyCutoff &&
      !hasEscapeHatch(ctx)

  def compactOutline(ctx: NarrativeContext, outline: NarrativeOutline): NarrativeOutline =
    if !collapsedEarlyOpening(ctx) then outline
    else
      val compactBeats = outline.beats.filter(beat => CompactBeatKinds.contains(beat.kind))
      if compactBeats.nonEmpty then NarrativeOutline(compactBeats, outline.diagnostics)
      else outline

  def clampNarrative(ctx: NarrativeContext, text: String): String =
    if !collapsedEarlyOpening(ctx) then text
    else limitSentences(text, MaxCollapsedSentences)

  private def isStandard(variantKey: String): Boolean =
    normalizeVariantKey(Option(variantKey)) == StandardVariant

  private def openingLike(ctx: NarrativeContext): Boolean =
    Option(ctx.phase.current).exists(_.trim.equalsIgnoreCase("opening")) ||
      Option(ctx.header.phase).exists(_.trim.equalsIgnoreCase("opening")) ||
      ctx.openingEvent.isDefined

  private def hasEscapeHatch(ctx: NarrativeContext): Boolean =
    hasMeaningfulOpeningEvent(ctx) ||
      StandardCommentaryClaimPolicy.hasDurableStructuralCommitment(ctx) ||
      hasForcedOrCriticalState(ctx) ||
      hasStrongTacticalPressure(ctx) ||
      hasSevereCounterfactual(ctx)

  private def hasMeaningfulOpeningEvent(ctx: NarrativeContext): Boolean =
    ctx.openingEvent.exists {
      case OpeningEvent.BranchPoint(_, _, _) => true
      case OpeningEvent.OutOfBook(_, _, _)   => true
      case OpeningEvent.TheoryEnds(_, _)     => true
      case OpeningEvent.Novelty(_, _, _, _)  => true
      case OpeningEvent.Intro(_, _, _, _)    => false
    }

  private def hasForcedOrCriticalState(ctx: NarrativeContext): Boolean =
    val criticality = Option(ctx.header.criticality).map(_.trim.toLowerCase).getOrElse("")
    criticality.contains("forced") ||
      criticality.contains("critical") ||
      Option(ctx.header.choiceType).exists(_.trim.equalsIgnoreCase("OnlyMove"))

  private def hasStrongTacticalPressure(ctx: NarrativeContext): Boolean =
    ctx.threats.toUs.exists(t => t.lossIfIgnoredCp >= 80 || t.kind.toLowerCase.contains("mate")) ||
      ctx.candidates.exists(c =>
        c.tags.contains(CandidateTag.Sharp) || c.tags.contains(CandidateTag.TacticalGamble)
      )

  private def hasSevereCounterfactual(ctx: NarrativeContext): Boolean =
    ctx.counterfactual.exists { cf =>
      cf.cpLoss >= 150 ||
      List("blunder", "missedwin", "mistake").contains(cf.severity.trim.toLowerCase.replace(" ", ""))
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
