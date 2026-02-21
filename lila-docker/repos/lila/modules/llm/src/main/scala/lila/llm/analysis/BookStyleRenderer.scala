package lila.llm.analysis

import lila.llm.model._
import lila.llm.model.authoring._

/**
 * BookStyleRenderer: Pure prose assembler (SSOT Phase 5)
 *
 * This module ONLY handles "how to phrase" - all decisions about
 * "what to say" are made by NarrativeOutlineBuilder and validated
 * by NarrativeOutlineValidator.
 */
object BookStyleRenderer:

  /**
   * Render NarrativeContext into book-style prose.
   */
  def render(ctx: NarrativeContext): String =
    val rec = new TraceRecorder()
    val (outline, diag) = NarrativeOutlineBuilder.build(ctx, rec)
    val validated = NarrativeOutlineValidator.validate(outline, diag, rec, Some(ctx))
    val prose = renderOutline(validated, ctx)
    PostCritic.revise(ctx, prose)

  /**
   * Render with trace output for debugging.
   */
  def renderTrace(ctx: NarrativeContext): String =
    val rec = new TraceRecorder()
    val (outline, diag) = NarrativeOutlineBuilder.build(ctx, rec)
    val validated = NarrativeOutlineValidator.validate(outline, diag, rec, Some(ctx))

    val sb = new StringBuilder()
    sb.append("# Outline Diagnostics\n")
    sb.append(s"${diag.summary}\n\n")

    if diag.droppedBeats.nonEmpty then
      sb.append("## Dropped Beats\n")
      diag.droppedBeats.foreach { case (kind, reason) => sb.append(s"- $kind: $reason\n") }
      sb.append("\n")

    if diag.downgradedBeats.nonEmpty then
      sb.append("## Downgraded Beats\n")
      diag.downgradedBeats.foreach { case (kind, reason) => sb.append(s"- $kind: $reason\n") }
      sb.append("\n")

    sb.append("## Outline Structure\n")
    validated.beats.foreach { b =>
      val conf = if b.confidenceLevel < 1.0 then s" (conf=${b.confidenceLevel})" else ""
      sb.append(s"- ${b.kind}$conf\n")
    }
    sb.append("\n")

    sb.append("## Trace Table\n")
    sb.append(rec.renderTable)
    sb.toString()

  /**
   * Full lossless appendix (for debugging/QA).
   */
  def renderFull(ctx: NarrativeContext): String =
    AppendixRenderer.render(ctx)

  private def renderOutline(outline: NarrativeOutline, ctx: NarrativeContext): String =
    val bead = Math.abs(ctx.hashCode)
    val sb = new StringBuilder()
    var lastKind: Option[OutlineBeatKind] = None

    outline.beats.foreach { b =>
      val text = renderBeat(b, ctx, bead)
      if (text.nonEmpty) {
        val needsNewParagraph = lastKind match {
          case Some(OutlineBeatKind.MoveHeader) => false
          case Some(OutlineBeatKind.MainMove) if b.kind == OutlineBeatKind.PsychologicalVerdict => false
          case Some(OutlineBeatKind.Context) if b.kind == OutlineBeatKind.DecisionPoint => true
          case Some(_) => true
          case None => false
        }

        if (needsNewParagraph && sb.nonEmpty) sb.append("\n\n")
        else if (sb.nonEmpty) {
          if (lastKind.contains(OutlineBeatKind.MoveHeader)) sb.append(": ")
          else sb.append(" ")
        }

        sb.append(text)
        lastKind = Some(b.kind)
      }
    }
    sb.toString()

  private def renderBeat(beat: OutlineBeat, ctx: NarrativeContext, bead: Int): String =
    if beat.text.nonEmpty then
      if beat.confidenceLevel < 0.6 then softenText(beat.text, bead)
      else beat.text
    else
      beat.kind match
        case OutlineBeatKind.MoveHeader => beat.anchors.headOption.getOrElse("")
        case OutlineBeatKind.Context => generateContext(ctx, bead)
        case OutlineBeatKind.DecisionPoint => generateDecision(beat, ctx)
        case OutlineBeatKind.Evidence => generateEvidence(beat, ctx)
        case OutlineBeatKind.ConditionalPlan => generateConditionalPlan(beat, ctx)
        case OutlineBeatKind.TeachingPoint => generateTeaching(ctx, bead)
        case OutlineBeatKind.MainMove => generateMainMove(ctx, bead)
        case OutlineBeatKind.OpeningTheory => generateOpeningTheory(ctx, bead)
        case OutlineBeatKind.Alternatives => generateAlternatives(ctx, bead)
        case OutlineBeatKind.WrapUp => generateWrapUp(ctx, bead)
        case OutlineBeatKind.PsychologicalVerdict => ""

  private def generateContext(ctx: NarrativeContext, bead: Int): String =
    val phase = ctx.phase.current
    val evalOpt = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp)
    val evalText = evalOpt.map(cp => NarrativeLexicon.evalOutcomeClauseFromCp(bead ^ 0x52dce729, cp, ply = ctx.ply)).getOrElse("The position is unclear")
    NarrativeLexicon.getOpening(bead, phase, evalText, ply = ctx.ply)

  private def generateDecision(beat: OutlineBeat, ctx: NarrativeContext): String =
    ctx.authorQuestions
      .find(q => beat.questionIds.contains(q.id))
      .map(_.question)
      .getOrElse("")

  private def generateEvidence(beat: OutlineBeat, ctx: NarrativeContext): String =
    val evidence = ctx.authorEvidence.filter(e => beat.questionIds.contains(e.questionId))
    if evidence.isEmpty then ""
    else
      val branches = evidence.flatMap(_.branches).take(4)
      val labels = List("a)", "b)", "c)", "d)")
      branches.zip(labels).map { case (b, label) =>
        val evalPart = b.evalCp.map(cp => s" (${formatCp(cp)})").getOrElse("")
        s"$label ${b.keyMove} ${b.line}$evalPart"
      }.mkString("\n")

  private def generateConditionalPlan(beat: OutlineBeat, ctx: NarrativeContext): String =
    val hasRefutation = beat.evidencePurposes.contains("latent_plan_refutation")
    val template = beat.conceptIds.headOption.flatMap { seedId =>
      ctx.authorQuestions
        .find(_.latentPlan.exists(_.seedId == seedId))
        .flatMap(_.latentPlan.map(_.narrative.template))
    }.getOrElse("A strategic idea worth considering if given time.")

    if hasRefutation then s"$template But the opponent can neutralize this with accurate play."
    else template

  private def generateTeaching(ctx: NarrativeContext, bead: Int): String =
    ctx.counterfactual.map { cf =>
      cf.causalThreat match {
        case Some(ct) => 
          NarrativeLexicon.getCausalTeachingPoint(bead, ct.concept, ct.narrative, cf.cpLoss)
        case None =>
          val theme = cf.missedMotifs.headOption.map(motifName)
            .orElse(Some(cf.severity))
            .getOrElse("tactic")
          NarrativeLexicon.getTeachingPoint(bead, theme, cf.cpLoss)
      }
    }.getOrElse("")

  private def generateMainMove(ctx: NarrativeContext, bead: Int): String =
    ctx.candidates.headOption.map { main =>
      val intent = NarrativeLexicon.getIntent(bead, main.planAlignment, None, ply = ctx.ply)
      val evalScore = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp).getOrElse(0)
      val evalTerm = NarrativeLexicon.evalOutcomeClauseFromCp(bead ^ 0x4b1d0f6a, evalScore, ply = ctx.ply)

      NarrativeLexicon.getMainFlow(bead, main.move, main.annotation, intent, None, None, evalTerm)
    }.getOrElse("")

  private def generateOpeningTheory(ctx: NarrativeContext, bead: Int): String =
    ctx.openingData.flatMap { ref =>
      ref.name.map { name =>
        NarrativeLexicon.getOpeningReference(bead, name, ref.totalGames, 0.5)
      }
    }.getOrElse("")

  private def generateAlternatives(ctx: NarrativeContext, bead: Int): String =
    ctx.candidates.drop(1).take(2).map { c =>
      NarrativeLexicon.getAlternative(bead, c.move, c.whyNot)
    }.mkString("\n")

  private def generateWrapUp(ctx: NarrativeContext, bead: Int): String =
    ctx.semantic.flatMap(_.practicalAssessment).map { pa =>
      NarrativeLexicon.getPracticalVerdict(bead, pa.verdict, cpWhite = 0, ply = ctx.ply)
    }.getOrElse("")

  private def softenText(text: String, bead: Int): String =
    val trimmed = text.trim
    if trimmed.isEmpty then text
    else
      val isQuestion =
        trimmed.endsWith("?") ||
          trimmed.matches("(?i)^(what|how|why|can|should|is|are|do|does|did|will|would|could)\\b.*")

      val prefix =
        if isQuestion then NarrativeLexicon.pick(bead, List("A key question: ", "Worth asking: ", "One practical question: "))
        else NarrativeLexicon.pick(bead, List("It seems that ", "Possibly, ", "Perhaps ", "In some lines, "))

      if isQuestion then s"$prefix$trimmed"
      else if trimmed.length == 1 then s"$prefix${trimmed.toLowerCase}"
      else s"$prefix${trimmed.head.toLower}${trimmed.tail}"

  private def formatCp(cp: Int): String =
    val sign = if cp >= 0 then "+" else ""
    val pawns = cp.toDouble / 100
    f"$sign$pawns%.1f"

  private def motifName(m: lila.llm.model.Motif): String =
    m.getClass.getSimpleName.replaceAll("\\$", "")

  def humanizePlan(plan: String): String =
    val low = plan.toLowerCase
    if low.contains("attack") then low.replaceAll("aligned", "").trim
    else if low.contains("control") then s"control of the ${low.replaceAll("control", "").trim}"
    else if low.contains("development") then "development of the pieces"
    else if low.contains("consolidation") then "consolidation and coordination"
    else if low.contains("prophylaxis") then "positional prophylaxis"
    else low.replaceAll("good", "").trim

  /**
   * Automated Lossless Appendix Renderer.
   */
  private[analysis] object AppendixRenderer:
    def render(ctx: NarrativeContext): String =
      val sb = new StringBuilder()
      sb.append("# Narrative Appendix (Lossless)\n")
      sb.append("> Units: 100cp = 1.0 pawn. Scores are from White's perspective.\n\n")

      sb.append("## KEY FACTS\n")
      ctx.plans.top5.headOption.foreach(p => sb.append(s"- Top Plan: ${p.name}\n"))
      ctx.threats.toUs.headOption.foreach { t =>
        sb.append(s"- Primary Threat: ${t.kind}${t.square.map(s => s" on $s").getOrElse("")} (${t.lossIfIgnoredCp}cp)\n")
      }
      ctx.candidates.headOption.foreach(c => sb.append(s"- Recommended: ${c.move}${c.annotation}\n"))
      ctx.engineEvidence.flatMap(_.best).map(_.scoreCp).foreach(cp => sb.append(s"- Engine Score (White POV): $cp\n"))
      sb.append("\n")
      sb.toString()
