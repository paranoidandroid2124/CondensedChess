package lila.commentary.analysis

import lila.commentary.model._
import lila.commentary.model.authoring._

/**
 * BookStyleRenderer: Pure prose assembler.
 *
 * This module ONLY handles "how to phrase" - all decisions about
 * "what to say" are made by NarrativeOutlineBuilder and validated
 * by NarrativeOutlineValidator.
 */
object BookStyleRenderer:

  private final case class RenderedSentenceCandidate(
      candidate: NarrativeDedupCore.NarrativeSentenceCandidate,
      beatKind: OutlineBeatKind,
      beatIndex: Int,
      sentenceIndex: Int
  )

  private val structureLeakTokens = List(
    "PA_MATCH",
    "PRECOND_MISS",
    "ANTI_PLAN",
    "LOW_CONF",
    "LOW_MARGIN",
    "REQ_MISS",
    "BLK_CONFLICT"
  )

  private val QuestionStartPattern = """(?i)^(what|how|why|can|should|is|are|do|does|did|will|would|could)\b.*""".r
  private val ReqPattern = """\bREQ_[A-Z0-9_]+\b""".r
  private val SupPattern = """\bSUP_[A-Z0-9_]+\b""".r
  private val BlkPattern = """\bBLK_[A-Z0-9_]+\b""".r
  /**
   * Render NarrativeContext into book-style prose.
   */
  def render(ctx: NarrativeContext): String =
    renderValidatedOutline(validatedOutline(ctx), ctx)

  private[commentary] def renderDraft(ctx: NarrativeContext): String =
    renderOutlineRaw(validatedOutline(ctx), ctx)

  def validatedOutline(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract] = None,
      strategyPack: Option[lila.commentary.StrategyPack] = None
  ): NarrativeOutline =
    val rec = new TraceRecorder()
    val (outline, diag) = NarrativeOutlineBuilder.build(ctx, rec, truthContract, strategyPack)
    EarlyOpeningNarrationPolicy.compactOutline(
      ctx,
      NarrativeOutlineValidator.validate(outline, diag, rec, Some(ctx)),
      truthContract
    )

  def renderValidatedOutline(
      outline: NarrativeOutline,
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract] = None
  ): String =
    val prose = renderOutlineRaw(outline, ctx)
    EarlyOpeningNarrationPolicy.clampNarrative(
      ctx,
      StandardCommentaryClaimPolicy.finalizeProse(
        ctx,
        PostCritic.revise(ctx, redactStructureTokens(prose)),
        truthContract
      ),
      truthContract
    )

  private[analysis] def renderBeatForSelection(beat: OutlineBeat, ctx: NarrativeContext): String =
    redactStructureTokens(renderBeat(beat, Math.abs(ctx.hashCode))).trim

  private def renderOutlineRaw(outline: NarrativeOutline, ctx: NarrativeContext): String =
    val bead = Math.abs(ctx.hashCode)
    val renderedCandidates =
      outline.beats.zipWithIndex.flatMap { case (beat, beatIndex) =>
        renderBeatSentences(beat, bead, beatIndex)
      }
    val kept = NarrativeDedupCore.dedupe(renderedCandidates.map(_.candidate)).map(_.order).toSet
    val selected = renderedCandidates.filter(candidate => kept.contains(candidate.candidate.order))

    val sb = new StringBuilder()
    var lastKind: Option[OutlineBeatKind] = None
    var lastBeatIndex: Option[Int] = None

    selected.foreach { rendered =>
      val text = rendered.candidate.text
      if text.nonEmpty then
        val separator =
          if sb.isEmpty then ""
          else if lastBeatIndex.contains(rendered.beatIndex) then " "
          else if lastKind.contains(OutlineBeatKind.MoveHeader) then ": "
          else if needsNewParagraph(lastKind, rendered.beatKind) then "\n\n"
          else " "
        sb.append(separator)
        sb.append(text)
        lastKind = Some(rendered.beatKind)
        lastBeatIndex = Some(rendered.beatIndex)
    }

    sb.toString()

  private def renderBeatSentences(
      beat: OutlineBeat,
      bead: Int,
      beatIndex: Int
  ): List[RenderedSentenceCandidate] =
    val rendered = renderBeat(beat, bead).trim
    if rendered.isEmpty then Nil
    else
      val parts =
        beat.kind match
          case OutlineBeatKind.Evidence | OutlineBeatKind.Alternatives =>
            rendered.linesIterator.map(_.trim).filter(_.nonEmpty).toList
          case _ =>
            NarrativeDedupCore.splitSentences(rendered)
      parts.zipWithIndex.map { case (sentence, sentenceIndex) =>
        RenderedSentenceCandidate(
          candidate =
            NarrativeDedupCore.buildCandidate(
              surface = "renderer",
              role = roleForBeat(beat.kind),
              text = sentence,
              priority = beat.focusPriority,
              order = beatIndex * 100 + sentenceIndex,
              familyOverride = familyForBeat(beat.kind)
            ),
          beatKind = beat.kind,
          beatIndex = beatIndex,
          sentenceIndex = sentenceIndex
        )
      }

  private def needsNewParagraph(
      previous: Option[OutlineBeatKind],
      current: OutlineBeatKind
  ): Boolean =
    previous match
      case Some(OutlineBeatKind.MoveHeader) => false
      case Some(OutlineBeatKind.MainMove) if current == OutlineBeatKind.PsychologicalVerdict => false
      case Some(OutlineBeatKind.Context) if current == OutlineBeatKind.DecisionPoint          => true
      case Some(_)                                                                           => true
      case None                                                                              => false

  private def roleForBeat(kind: OutlineBeatKind): String =
    kind match
      case OutlineBeatKind.Context              => "context"
      case OutlineBeatKind.DecisionPoint        => "support"
      case OutlineBeatKind.Evidence             => "support"
      case OutlineBeatKind.TeachingPoint        => "support"
      case OutlineBeatKind.MainMove             => "main_move"
      case OutlineBeatKind.OpeningTheory        => "support"
      case OutlineBeatKind.Alternatives         => "route"
      case OutlineBeatKind.WrapUp               => "wrap_up"
      case OutlineBeatKind.MoveHeader           => "context"
      case OutlineBeatKind.PsychologicalVerdict => "support"

  private def familyForBeat(
      kind: OutlineBeatKind
  ): Option[NarrativeDedupCore.NarrativeClaimFamily] =
    kind match
      case OutlineBeatKind.MainMove        => Some(NarrativeDedupCore.NarrativeClaimFamily.PlanLead)
      case OutlineBeatKind.WrapUp          => Some(NarrativeDedupCore.NarrativeClaimFamily.PracticalVerdict)
      case _                               => None

  private def renderBeat(beat: OutlineBeat, bead: Int): String =
    if beat.text.nonEmpty then
      if beat.confidenceLevel < 0.6 then softenText(beat.text, bead)
      else beat.text
    else
      ""

  private def softenText(text: String, bead: Int): String =
    val trimmed = text.trim
    if trimmed.isEmpty then text
    else
      val isQuestion =
        trimmed.endsWith("?") || QuestionStartPattern.matches(trimmed)

      val prefix =
        if isQuestion then NarrativeLexicon.pick(bead, List("A key question: ", "Worth asking: ", "One practical question: "))
        else NarrativeLexicon.pick(bead, List("It seems that ", "Possibly, ", "Perhaps ", "In some lines, "))

      if isQuestion then s"$prefix$trimmed"
      else if trimmed.length == 1 then s"$prefix${trimmed.toLowerCase}"
      else s"$prefix${trimmed.head.toLower}${trimmed.tail}"

  private def redactStructureTokens(text: String): String =
    val basic = structureLeakTokens.foldLeft(text) { (acc, token) =>
      acc.replace(token, "structure")
    }
    val s1 = ReqPattern.replaceAllIn(basic, "structure")
    val s2 = SupPattern.replaceAllIn(s1, "structure")
    BlkPattern.replaceAllIn(s2, "structure")
