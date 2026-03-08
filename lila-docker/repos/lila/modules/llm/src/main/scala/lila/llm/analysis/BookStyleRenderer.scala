package lila.llm.analysis

import lila.llm.model._
import lila.llm.model.authoring._
import lila.llm.analysis.ThemeTaxonomy.ThemeResolver

/**
 * BookStyleRenderer: Pure prose assembler (SSOT Phase 5)
 *
 * This module ONLY handles "how to phrase" - all decisions about
 * "what to say" are made by NarrativeOutlineBuilder and validated
 * by NarrativeOutlineValidator.
 */
object BookStyleRenderer:

  private val structureLeakTokens = List(
    "PA_MATCH",
    "PRECOND_MISS",
    "ANTI_PLAN",
    "LOW_CONF",
    "LOW_MARGIN",
    "REQ_MISS",
    "BLK_CONFLICT"
  )
  private val LegacyStrategicFallbackText = boolEnv("LLM_LEGACY_STRATEGIC_TEXT_FALLBACK", default = false)

  /**
   * Render NarrativeContext into book-style prose.
   */
  def render(ctx: NarrativeContext): String =
    renderValidatedOutline(validatedOutline(ctx), ctx)

  private[llm] def renderDraft(ctx: NarrativeContext): String =
    renderOutlineRaw(validatedOutline(ctx), ctx)

  def validatedOutline(ctx: NarrativeContext): NarrativeOutline =
    val rec = new TraceRecorder()
    val (outline, diag) = NarrativeOutlineBuilder.build(ctx, rec)
    NarrativeOutlineValidator.validate(outline, diag, rec, Some(ctx))

  def renderValidatedOutline(outline: NarrativeOutline, ctx: NarrativeContext): String =
    val prose = renderOutlineRaw(outline, ctx)
    PostCritic.revise(ctx, redactStructureTokens(prose))

  private def renderOutlineRaw(outline: NarrativeOutline, ctx: NarrativeContext): String =
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
      val continuityOpt = ctx.planContinuity
      val intentAnchor = topStrategicPlanName(ctx).getOrElse(main.planAlignment)
      val intent = NarrativeLexicon.getIntent(bead, intentAnchor, None, ply = ctx.ply, continuity = continuityOpt)
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
    val strategicSummary =
      if ctx.mainStrategicPlans.nonEmpty then
        val topPlans = ctx.mainStrategicPlans.take(3)
        val lead = topPlans.head
        val slots = ThemeNarrativeSlots.forTheme(themeIdOfHypothesis(lead))
        val ranked = topPlans.map(p => s"${p.rank}. ${p.planName} (${f"${p.score}%.2f"})").mkString("; ")
        val preconditions =
          lead.preconditions.map(_.trim).filter(_.nonEmpty).take(2)
        val sources =
          topPlans
            .flatMap(_.evidenceSources)
            .map(_.trim)
            .filter(_.nonEmpty)
            .distinct
            .take(3)
            .mkString(", ")
        val failures =
          (lead.failureModes ++ lead.refutation.toList ++ ctx.whyAbsentFromTopMultiPV ++ ctx.latentPlans.map(_.whyAbsentFromTopMultiPv))
            .map(_.trim)
            .filter(_.nonEmpty)
            .distinct
        val preconditionText =
          if preconditions.nonEmpty then s" Preconditions: ${preconditions.mkString("; ")}." else ""
        val evidenceText =
          if sources.nonEmpty then s"${slots.evidence} Signals: $sources."
          else s"${slots.evidence} Probe + structural evidence is still limited."
        val holdText =
          if failures.nonEmpty then s"${slots.hold} ${failures.take(2).mkString("; ")}."
          else s"${slots.hold} No explicit refutation was detected."
        s"Idea: ${slots.idea} Primary route is ${lead.planName}. Ranked stack: $ranked.$preconditionText Evidence: $evidenceText Refutation/Hold: $holdText"
      else ""

    val practicalSummary =
      ctx.semantic.flatMap(_.practicalAssessment).map { pa =>
        NarrativeLexicon.getPracticalVerdict(bead, pa.verdict, cpWhite = 0, ply = ctx.ply)
      }.getOrElse("")

    List(strategicSummary, practicalSummary).filter(_.nonEmpty).mkString(" ").trim

  private def themeIdOfHypothesis(plan: PlanHypothesis): String =
    ThemeResolver.fromHypothesis(plan).id

  private def topStrategicPlanName(ctx: NarrativeContext): Option[String] =
    ctx.mainStrategicPlans.headOption.map(_.planName).orElse {
      if LegacyStrategicFallbackText then ctx.plans.top5.headOption.map(_.name) else None
    }

  private def boolEnv(name: String, default: Boolean): Boolean =
    sys.env
      .get(name)
      .map(_.trim.toLowerCase)
      .flatMap {
        case "1" | "true" | "yes" | "on"  => Some(true)
        case "0" | "false" | "no" | "off" => Some(false)
        case _                              => None
      }
      .getOrElse(default)

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

  private def redactStructureTokens(text: String): String =
    val basic = structureLeakTokens.foldLeft(text) { (acc, token) =>
      acc.replace(token, "structure")
    }
    basic
      .replaceAll("\\bREQ_[A-Z0-9_]+\\b", "structure")
      .replaceAll("\\bSUP_[A-Z0-9_]+\\b", "structure")
      .replaceAll("\\bBLK_[A-Z0-9_]+\\b", "structure")

  def humanizePlan(plan: String): String =
    val low = plan.toLowerCase
    if low.contains("attack") then low.replaceAll("aligned", "").trim
    else if low.contains("control") then s"control of the ${low.replaceAll("control", "").trim}"
    else if low.contains("development") then "development of the pieces"
    else if low.contains("consolidation") then "consolidation and coordination"
    else if low.contains("prophylaxis") then "positional prophylaxis"
    else low.replaceAll("good", "").trim
