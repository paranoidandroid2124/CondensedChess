package lila.llm.analysis

import lila.llm.model.*
import lila.llm.model.authoring.{ OutlineBeat, OutlineBeatKind }

private[llm] object GameChronicleCompressionPolicy:

  private enum ClaimFamily:
    case MainMove
    case Decision
    case Context
    case Opening
    case Teaching
    case Practical
    case Fallback

  private final case class BeatSurface(
      beat: OutlineBeat,
      rendered: String,
      sentences: List[String],
      citedLine: Option[String]
  )

  private final case class ClaimChoice(
      beat: OutlineBeat,
      family: ClaimFamily,
      text: String
  )

  private val claimOrder = List(
    ClaimFamily.MainMove,
    ClaimFamily.Decision,
    ClaimFamily.Context,
    ClaimFamily.Opening,
    ClaimFamily.Teaching,
    ClaimFamily.Practical,
    ClaimFamily.Fallback
  )

  def render(
      ctx: NarrativeContext,
      parts: CommentaryEngine.HybridNarrativeParts
  ): Option[String] =
    val compactOpening = EarlyOpeningNarrationPolicy.collapsedEarlyOpening(ctx)
    val surfaces =
      parts.focusedOutline.beats
        .filterNot(isStrategicDistributionBeat)
        .filterNot(_.kind == OutlineBeatKind.MoveHeader)
        .flatMap(renderSurface(ctx, _))
    buildSlots(ctx, surfaces, parts, compactOpening)
      .orElse(fallbackSlots(parts))
      .map(LiveNarrativeCompressionCore.deterministicProse)
      .map(text => FullGameDraftNormalizer.normalize(text).trim)

  private def renderSurface(ctx: NarrativeContext, beat: OutlineBeat): Option[BeatSurface] =
    val rendered = BookStyleRenderer.renderBeatForSelection(beat, ctx)
    val sentences =
      splitSentences(rendered)
        .flatMap(raw => BookmakerLiveCompressionPolicy.cleanNarrativeSentence(raw, ctx))
        .filterNot(isQuestionLike)
        .filterNot(isLowValueSentence)
        .filterNot(s => beat.branchScoped && !LineScopedCitation.hasInlineCitation(s))

    val citedLine =
      citationFromBeat(beat, rendered)
        .flatMap(raw => BookmakerLiveCompressionPolicy.cleanNarrativeSentence(raw, ctx))
        .filterNot(isLowValueSentence)

    Option.when(sentences.nonEmpty || citedLine.nonEmpty)(
      BeatSurface(
        beat = beat,
        rendered = rendered,
        sentences = sentences,
        citedLine = citedLine
      )
    )

  private def buildSlots(
      ctx: NarrativeContext,
      surfaces: List[BeatSurface],
      parts: CommentaryEngine.HybridNarrativeParts,
      compactOpening: Boolean
  ): Option[BookmakerPolishSlots] =
    selectClaim(ctx, surfaces, parts).map { claim =>
      val support =
        Option.unless(compactOpening) {
          selectSupport(ctx, surfaces, claim)
        }.flatten
      val thirdParagraph =
        Option.unless(compactOpening) {
          selectThirdParagraph(ctx, surfaces, claim, support, parts)
        }.flatten
      val evidenceHook = thirdParagraph.filter(LineScopedCitation.hasConcreteSanLine)
      val tension = thirdParagraph.filterNot(LineScopedCitation.hasConcreteSanLine)
      val paragraphPlan =
        if compactOpening then List("p1=claim")
        else
          List(
            Some("p1=claim"),
            support.map(_ => "p2=support"),
            evidenceHook.map(_ => "p3=cited_line").orElse(tension.map(_ => "p3=practical_nuance"))
          ).flatten
      BookmakerPolishSlots(
        lens = lensFor(claim.family),
        claim = claim.text,
        supportPrimary = support,
        supportSecondary = None,
        tension = tension,
        evidenceHook = evidenceHook,
        coda = None,
        factGuardrails = evidenceHook.toList,
        paragraphPlan = paragraphPlan
      )
    }

  private def fallbackSlots(parts: CommentaryEngine.HybridNarrativeParts): Option[BookmakerPolishSlots] =
    LiveNarrativeCompressionCore.slotsFromCompressedProse(
      List(parts.body, parts.defaultBridge, parts.lead)
        .map(_.trim)
        .find(_.nonEmpty)
        .getOrElse("")
    )

  private def selectClaim(
      ctx: NarrativeContext,
      surfaces: List[BeatSurface],
      parts: CommentaryEngine.HybridNarrativeParts
  ): Option[ClaimChoice] =
    val direct =
      claimOrder.iterator
        .flatMap(family => claimCandidates(ctx, surfaces, family).iterator.map(choice => (family, choice)))
        .collectFirst { case (family, (beat, text)) => ClaimChoice(beat, family, text) }

    direct.orElse {
      fallbackSentences(parts)
        .flatMap(raw => BookmakerLiveCompressionPolicy.cleanNarrativeSentence(raw, ctx))
        .filterNot(isQuestionLike)
        .filterNot(isLowValueSentence)
        .headOption
        .map(text => ClaimChoice(OutlineBeat(kind = OutlineBeatKind.Context), ClaimFamily.Fallback, text))
    }

  private def lensFor(family: ClaimFamily): StrategicLens =
    family match
      case ClaimFamily.Practical => StrategicLens.Practical
      case ClaimFamily.Opening   => StrategicLens.Opening
      case ClaimFamily.Decision  => StrategicLens.Decision
      case ClaimFamily.Context   => StrategicLens.Structure
      case ClaimFamily.Teaching  => StrategicLens.Decision
      case ClaimFamily.MainMove  => StrategicLens.Decision
      case ClaimFamily.Fallback  => StrategicLens.Decision

  private def claimCandidates(
      ctx: NarrativeContext,
      surfaces: List[BeatSurface],
      family: ClaimFamily
  ): List[(OutlineBeat, String)] =
    surfaces.flatMap { surface =>
      val kindMatches =
        family match
          case ClaimFamily.MainMove  => surface.beat.kind == OutlineBeatKind.MainMove
          case ClaimFamily.Decision  => surface.beat.kind == OutlineBeatKind.DecisionPoint
          case ClaimFamily.Context   => surface.beat.kind == OutlineBeatKind.Context
          case ClaimFamily.Opening   => surface.beat.kind == OutlineBeatKind.OpeningTheory
          case ClaimFamily.Teaching  => surface.beat.kind == OutlineBeatKind.TeachingPoint
          case ClaimFamily.Practical => isPracticalWrapUp(surface.beat, ctx)
          case ClaimFamily.Fallback  => false
      if kindMatches then
        surface.sentences.headOption.toList.map(surface.beat -> _)
      else Nil
    }

  private def selectSupport(
      ctx: NarrativeContext,
      surfaces: List[BeatSurface],
      claim: ClaimChoice
  ): Option[String] =
    val sameBeatExtra =
      surfaces
        .find(_.beat == claim.beat)
        .flatMap(_.sentences.filterNot(sameSentence(_, claim.text)).headOption)

    sameBeatExtra.orElse {
      supportKindPriority(claim.family).iterator
        .flatMap { kind =>
          surfaces.iterator
            .filter(_.beat != claim.beat)
            .filter(_.beat.kind == kind)
            .filterNot(surface => kind == OutlineBeatKind.WrapUp && !isPracticalWrapUp(surface.beat, ctx))
            .flatMap(_.sentences.iterator)
            .filterNot(sameSentence(_, claim.text))
        }
        .find(_.nonEmpty)
    }

  private def selectThirdParagraph(
      ctx: NarrativeContext,
      surfaces: List[BeatSurface],
      claim: ClaimChoice,
      support: Option[String],
      parts: CommentaryEngine.HybridNarrativeParts
  ): Option[String] =
    val cited =
      citedLineCandidates(parts, surfaces).find { candidate =>
        candidate.nonEmpty &&
        !sameSentence(candidate, claim.text) &&
        support.forall(s => !sameSentence(candidate, s))
      }

    cited.orElse {
      Option.when(
        ctx.phase.current.equalsIgnoreCase("Endgame") || claim.family == ClaimFamily.Practical
      ) {
        surfaces.iterator
          .filter(surface => surface.beat != claim.beat && isPracticalWrapUp(surface.beat, ctx))
          .flatMap(_.sentences.iterator)
          .filterNot(sameSentence(_, claim.text))
          .find(s => support.forall(other => !sameSentence(s, other)))
      }.flatten
    }

  private def citedLineCandidates(
      parts: CommentaryEngine.HybridNarrativeParts,
      surfaces: List[BeatSurface]
  ): List[String] =
    val beatLines =
      surfaces
        .filter(surface =>
          surface.beat.kind == OutlineBeatKind.Evidence ||
            surface.beat.kind == OutlineBeatKind.Alternatives ||
            surface.beat.branchScoped
        )
        .flatMap(_.citedLine)
    val critical = parts.criticalBranch.toList.flatMap(normalizeCitationSentence)
    (beatLines ++ critical).distinct

  private def citationFromBeat(beat: OutlineBeat, raw: String): Option[String] =
    beat.kind match
      case OutlineBeatKind.Evidence | OutlineBeatKind.Alternatives =>
        raw.linesIterator
          .map(_.trim)
          .filter(_.nonEmpty)
          .flatMap(normalizeCitationSentence)
          .find(_.nonEmpty)
      case _ =>
        splitSentences(raw)
          .flatMap(normalizeCitationSentence)
          .headOption

  private def normalizeCitationSentence(raw: String): Option[String] =
    val trimmed = Option(raw).map(_.trim).getOrElse("")
    val unlabelled = trimmed.replaceFirst("""^[a-z]\)\s*""", "").trim
    if LineScopedCitation.hasInlineCitation(trimmed) then
      Some(trimmed.stripSuffix(".") + ".")
    else if LineScopedCitation.hasConcreteSanLine(unlabelled) then
      Some(s"A concrete line is ${unlabelled.stripSuffix(".")}.")
    else None

  private def fallbackSentences(
      parts: CommentaryEngine.HybridNarrativeParts
  ): List[String] =
    List(parts.body, parts.defaultBridge, parts.lead)
      .flatMap(splitSentences)
      .map(_.trim)
      .filter(_.nonEmpty)

  private def supportKindPriority(family: ClaimFamily): List[OutlineBeatKind] =
    family match
      case ClaimFamily.MainMove =>
        List(OutlineBeatKind.DecisionPoint, OutlineBeatKind.Context, OutlineBeatKind.OpeningTheory, OutlineBeatKind.TeachingPoint, OutlineBeatKind.WrapUp)
      case ClaimFamily.Decision =>
        List(OutlineBeatKind.MainMove, OutlineBeatKind.Context, OutlineBeatKind.OpeningTheory, OutlineBeatKind.TeachingPoint, OutlineBeatKind.WrapUp)
      case ClaimFamily.Context =>
        List(OutlineBeatKind.MainMove, OutlineBeatKind.DecisionPoint, OutlineBeatKind.OpeningTheory, OutlineBeatKind.TeachingPoint)
      case ClaimFamily.Opening =>
        List(OutlineBeatKind.MainMove, OutlineBeatKind.DecisionPoint, OutlineBeatKind.Context)
      case ClaimFamily.Teaching =>
        List(OutlineBeatKind.MainMove, OutlineBeatKind.DecisionPoint, OutlineBeatKind.Context)
      case ClaimFamily.Practical =>
        List(OutlineBeatKind.MainMove, OutlineBeatKind.DecisionPoint, OutlineBeatKind.Context)
      case ClaimFamily.Fallback =>
        List(OutlineBeatKind.MainMove, OutlineBeatKind.DecisionPoint, OutlineBeatKind.Context)

  private def splitSentences(text: String): List[String] =
    Option(text)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.split("(?<=[.!?])\\s+").toList)
      .getOrElse(Nil)
      .map(_.trim)
      .filter(_.nonEmpty)

  private def isQuestionLike(text: String): Boolean =
    val trimmed = Option(text).map(_.trim).getOrElse("")
    val low = trimmed.toLowerCase
    trimmed.endsWith("?") ||
      low.startsWith("why ") ||
      low.startsWith("what ") ||
      low.startsWith("how ") ||
      low.startsWith("which ")

  private def isLowValueSentence(text: String): Boolean =
    val low = normalize(text)
    LiveNarrativeCompressionCore.isLowValueNarrativeSentence(text) ||
      low.startsWith("the strategic stack still favors") ||
      low.startsWith("the leading route is") ||
      low.startsWith("the backup strategic stack is") ||
      low.startsWith("the main signals are") ||
      low.startsWith("evidence must show") ||
      low.startsWith("current support centers on") ||
      low.startsWith("initial board read") ||
      low.startsWith("clearest read is that") ||
      low.startsWith("validation evidence specifically") ||
      low.startsWith("another key pillar is that") ||
      low.startsWith("in practical terms the split should appear") ||
      low.startsWith("dominant thesis:") ||
      low.startsWith("keep ") && low.contains(" in the foreground via ") ||
      low.contains("ranked stack") ||
      low.contains("latent plan") ||
      low.contains("probe evidence says") ||
      low.contains("refutation hold")

  private def isStrategicDistributionBeat(beat: OutlineBeat): Boolean =
    beat.conceptIds.contains("strategic_distribution_first") ||
      beat.conceptIds.contains("plan_evidence_three_stage")

  private def isPracticalWrapUp(beat: OutlineBeat, ctx: NarrativeContext): Boolean =
    beat.kind == OutlineBeatKind.WrapUp &&
      (beat.conceptIds.contains("practical_assessment") || ctx.phase.current.equalsIgnoreCase("Endgame")) &&
      !isStrategicDistributionBeat(beat) &&
      !isLowValueSentence(beat.text)

  private def normalize(text: String): String =
    Option(text)
      .getOrElse("")
      .replace("**", "")
      .replaceAll("""[^\p{L}\p{N}\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .toLowerCase

  private def sameSentence(a: String, b: String): Boolean =
    val left = normalize(a)
    val right = normalize(b)
    left.nonEmpty && left == right
