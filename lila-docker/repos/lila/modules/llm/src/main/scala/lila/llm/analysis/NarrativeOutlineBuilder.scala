package lila.llm.analysis

import lila.llm.model._
import lila.llm.model.authoring._
import lila.llm.model.strategic.VariationLine

/**
 * NarrativeOutlineBuilder: SSOT for "what to say"
 *
 * Phase 5: Decision engine for narrative structure.
 * All "what to say" decisions happen here; Renderer only handles phrasing.
 */
object NarrativeOutlineBuilder:

  def build(ctx: NarrativeContext, rec: TraceRecorder): (NarrativeOutline, OutlineDiagnostics) =
    val bead = Math.abs(ctx.hashCode)
    val beats = scala.collection.mutable.ListBuffer.empty[OutlineBeat]
    var diag = OutlineDiagnostics()

    val isAnnotation = isMoveAnnotation(ctx)
    val questions = ctx.authorQuestions.sortBy(-_.priority).take(3)
    diag = diag.copy(selectedQuestions = questions)

    val availablePurposes = ctx.authorEvidence.map(_.purpose).toSet
    diag = diag.copy(usedEvidencePurposes = availablePurposes)

    val missingPurposes = EvidencePlanner.getMissingPurposesFromEvidence(questions, ctx.authorEvidence)
    diag = diag.copy(missingEvidencePurposes = missingPurposes)

    // 1. MOVE HEADER
    if isAnnotation then
      buildMoveHeader(ctx, rec).foreach(beats += _)

    // 2. CONTEXT
    beats += buildContextBeat(ctx, rec, bead)

    // 3. DECISION POINT
    buildDecisionBeat(questions, rec).foreach(beats += _)

    // 4. EVIDENCE (from authorEvidence OR engineEvidence fallback)
    buildEvidenceBeat(ctx, questions, rec).foreach(beats += _)

    // 5. CONDITIONAL PLAN
    questions.find(_.kind == AuthorQuestionKind.LatentPlan)
      .flatMap(buildConditionalPlanBeat(ctx, _, rec)).foreach(beats += _)

    // 6. TEACHING POINT (lower threshold for visibility)
    buildTeachingBeat(ctx, rec, bead).foreach(beats += _)

    // 7. MAIN MOVE
    val mainMoveBeat = buildMainMoveBeat(ctx, rec, isAnnotation, bead)
    if mainMoveBeat.text.nonEmpty then beats += mainMoveBeat

    // Phase 6.8: Psychological Reconstruction
    LogicReconstructor.analyze(ctx).foreach { recon =>
      beats += OutlineBeat(
        kind = OutlineBeatKind.PsychologicalVerdict,
        text = recon.description,
        conceptIds = List(s"psych_${recon.kind.toString.toLowerCase}")
      )
    }

    // 8. OPENING THEORY
    buildOpeningTheoryBeat(ctx, rec).foreach(beats += _)

    // 9. ALTERNATIVES
    val altBeat = buildAlternativesBeat(ctx, rec, bead)
    if altBeat.text.nonEmpty then beats += altBeat

    // 10. WRAP-UP
    buildWrapUpBeat(ctx, bead).foreach(beats += _)

    (NarrativeOutline(beats.toList, Some(diag)), diag)

  def isMoveAnnotation(ctx: NarrativeContext): Boolean =
    ctx.playedMove.isDefined && ctx.playedSan.isDefined

  // ===========================================================================
  // Beat Builders
  // ===========================================================================

  private def buildMoveHeader(ctx: NarrativeContext, rec: TraceRecorder): Option[OutlineBeat] =
    for
      san <- ctx.playedSan
      moveNum = (ctx.ply + 1) / 2
      prefix = if ctx.ply % 2 == 1 then s"$moveNum." else s"$moveNum..."
    yield
      rec.use("playedSan", san, "Move header")
      OutlineBeat(kind = OutlineBeatKind.MoveHeader, text = s"$prefix $san", anchors = List(san))

  private def buildContextBeat(ctx: NarrativeContext, rec: TraceRecorder, bead: Int): OutlineBeat =
    val parts = scala.collection.mutable.ListBuffer[String]()
    val concepts = scala.collection.mutable.ListBuffer[String]()

    // Position statement
    val phase = ctx.phase.current
    val evalOpt = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp)
    val evalText = evalOpt.map(NarrativeLexicon.evalOutcomeClauseFromCp).getOrElse("unclear")
    val openingPart = NarrativeLexicon.getOpening(bead, phase, evalText)
    
    // Phase 6 Expansion: Prefix with specific motif if available
    val motifs = (ctx.semantic.map(_.conceptSummary).getOrElse(Nil) ++ 
                  ctx.delta.map(_.newMotifs).getOrElse(Nil) ++ 
                  ctx.counterfactual.map(_.missedMotifs.map(_.getClass.getSimpleName)).getOrElse(Nil)).distinct
    val motifPrefix = NarrativeLexicon.getMotifPrefix(bead, motifs)
    
    parts += motifPrefix.map(_ + openingPart).getOrElse(openingPart)

    // Main threat if exists (adds drama)
    ctx.threats.toUs.headOption.filter(_.lossIfIgnoredCp >= 30).foreach { t =>
      rec.use("threats.toUs[0]", t.kind, "Context threat")
      parts += NarrativeLexicon.getThreatStatement(bead, t.kind, t.lossIfIgnoredCp)
      concepts += s"threat_${t.kind}"
    }

    // Top plan (strategic direction)
    ctx.plans.top5.headOption.foreach { p =>
      rec.use("plans.top5[0]", p.name, "Context plan")
      parts += NarrativeLexicon.getPlanStatement(bead, p.name)
      concepts += s"plan_${p.name}"
    }

    // Phase 6.8: Pawn Play (Stranded Asset 1)
    ctx.pawnPlay.breakFile.foreach { br =>
      rec.use("pawnPlay.breakFile", br, "Context break")
      parts += NarrativeLexicon.getPawnPlayStatement(bead, br, ctx.pawnPlay.tensionPolicy)
      concepts += "pawn_break_ready"
    }

    OutlineBeat(
      kind = OutlineBeatKind.Context,
      text = parts.filter(_.nonEmpty).mkString(" ").trim,
      conceptIds = concepts.toList
    )

  private def buildDecisionBeat(
    questions: List[AuthorQuestion],
    rec: TraceRecorder
  ): Option[OutlineBeat] =
    val nonLatent = questions.filterNot(_.kind == AuthorQuestionKind.LatentPlan)
    nonLatent.headOption.map { q =>
      rec.use(s"authorQuestions[${q.id}]", q.question, "Decision point")
      OutlineBeat(
        kind = OutlineBeatKind.DecisionPoint,
        text = q.question,
        questionIds = List(q.id),
        questionKinds = List(q.kind),
        anchors = q.anchors,
        requiresEvidence = true
      )
    }

  /**
   * Build evidence beat from authorEvidence OR fallback to engineEvidence.
   * This ensures we always show a)/b) alternatives when engine data is available.
   */
  private def buildEvidenceBeat(
    ctx: NarrativeContext,
    questions: List[AuthorQuestion],
    rec: TraceRecorder
  ): Option[OutlineBeat] =
    // Primary: use authorEvidence if available
    val relevantEvidence = ctx.authorEvidence.filter { ev =>
      questions.exists(_.id == ev.questionId)
    }

    if relevantEvidence.nonEmpty then
      val branches = relevantEvidence.flatMap(_.branches).take(4)
      if branches.size >= 2 then
        val labels = List("a)", "b)", "c)", "d)")
        val formatted = branches.zip(labels).map { case (b, label) =>
          val evalPart = b.evalCp.map(cp => s" (${formatCp(cp)})").getOrElse("")
          s"$label ${b.keyMove} ${b.line}$evalPart"
        }
        val purposes = relevantEvidence.map(_.purpose).distinct
        val qKinds = questions.filter(q => relevantEvidence.exists(_.questionId == q.id)).map(_.kind).distinct

        rec.use("authorEvidence", purposes.mkString(","), "Evidence from authorEvidence")
        return Some(OutlineBeat(
          kind = OutlineBeatKind.Evidence,
          text = formatted.mkString("\n"),
          questionIds = relevantEvidence.map(_.questionId).distinct,
          questionKinds = qKinds,
          evidencePurposes = purposes
        ))

    // Fallback: use engineEvidence variations
    ctx.engineEvidence.flatMap { ev =>
      val variations = ev.variations.take(3)
      if variations.size >= 2 then
        val labels = List("a)", "b)", "c)")
        val formatted = variations.zip(labels).map { case (v, label) =>
          val moveSan = v.ourMove.map(_.san).getOrElse(v.moves.headOption.getOrElse(""))
          val lineSample = v.sampleLine(4)
          s"$label $moveSan $lineSample (${formatCp(v.scoreCp)})"
        }
        rec.use("engineEvidence.variations", variations.size.toString, "Evidence fallback from engine PV")
        Some(OutlineBeat(
          kind = OutlineBeatKind.Evidence,
          text = formatted.mkString("\n"),
          evidencePurposes = List("engine_alternatives")
        ))
      else None
    }

  private def buildConditionalPlanBeat(
    ctx: NarrativeContext,
    question: AuthorQuestion,
    rec: TraceRecorder
  ): Option[OutlineBeat] =
    question.latentPlan.map { lp =>
      rec.use(s"latentPlan[${lp.seedId}]", lp.narrative.template, "Conditional plan")

      val hasViability = ctx.authorEvidence.exists { ev =>
        ev.questionId == question.id &&
          (ev.purpose == "free_tempo_branches" || ev.purpose == "latent_plan_immediate")
      }
      val hasRefutation = ctx.authorEvidence.exists { ev =>
        ev.questionId == question.id && ev.purpose == "latent_plan_refutation"
      }

      val purposes =
        (if hasViability then List("free_tempo_branches") else Nil) ++
          (if hasRefutation then List("latent_plan_refutation") else Nil)

      OutlineBeat(
        kind = OutlineBeatKind.ConditionalPlan,
        text = lp.narrative.template,
        conceptIds = List(lp.seedId),
        anchors = lp.candidateMoves.take(2).map(_.toString),
        questionIds = List(question.id),
        questionKinds = List(AuthorQuestionKind.LatentPlan),
        evidencePurposes = purposes,
        requiresEvidence = true
      )
    }

  /**
   * Build teaching beat with lower threshold (cpLoss > 20 instead of > 50).
   * Also adds fallback for counterfactual without motifs.
   */
  private def buildTeachingBeat(ctx: NarrativeContext, rec: TraceRecorder, bead: Int): Option[OutlineBeat] =
    ctx.counterfactual.filter(_.cpLoss > 20).map { cf =>
      rec.use("counterfactual", cf.userMove, "Teaching point")
      val theme = cf.missedMotifs.headOption.map(motifName)
        .orElse(Some(cf.severity))
        .getOrElse("positional consideration")
      val text = NarrativeLexicon.getTeachingPoint(bead, theme, cf.cpLoss)
      OutlineBeat(
        kind = OutlineBeatKind.TeachingPoint,
        text = text,
        conceptIds = List("teaching_counterfactual"),
        anchors = List(cf.bestMove)
      )
    }

  private def buildMainMoveBeat(ctx: NarrativeContext, rec: TraceRecorder, isAnnotation: Boolean, bead: Int): OutlineBeat =
    if isAnnotation then
      val playedSan = ctx.playedSan.getOrElse("")
      val best = ctx.candidates.headOption
      val bestSan = best.map(_.move).getOrElse("")
      val isBest = ctx.playedMove == best.flatMap(_.uci)

      val text = if isBest then
        NarrativeLexicon.getAnnotationPositive(bead, playedSan)
      else
        val cpLoss = ctx.counterfactual.map(_.cpLoss).getOrElse(0)
        NarrativeLexicon.getAnnotationNegative(bead, playedSan, bestSan, cpLoss)

      OutlineBeat(kind = OutlineBeatKind.MainMove, text = text, anchors = List(playedSan, bestSan).filter(_.nonEmpty).distinct)
    else
      ctx.candidates.headOption.map { main =>
        rec.use("candidates[0]", main.move, "Main move")
        val intent = NarrativeLexicon.getIntent(bead, main.planAlignment, None)
        val evalScore = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp).getOrElse(0)
        val evalTerm = NarrativeLexicon.evalOutcomeClauseFromCp(evalScore)
        val replySan = ctx.engineEvidence.flatMap(_.best.flatMap(_.theirReply)).map(_.san)
        val sampleRest = ctx.engineEvidence.flatMap(_.best.flatMap(_.sampleLineFrom(2, 6)))

        val text = NarrativeLexicon.getMainFlow(bead, main.move, main.annotation, intent, replySan, sampleRest, evalTerm)
        
        // Phase 6.8: Prophylaxis (Stranded Asset 2)
        val prophylaxisText = ctx.semantic.flatMap(_.preventedPlans.headOption).map { pp =>
          NarrativeLexicon.getPreventedPlanStatement(bead, pp.planId)
        }

        OutlineBeat(
          kind = OutlineBeatKind.MainMove, 
          text = prophylaxisText.map(t => s"$text $t").getOrElse(text), 
          anchors = List(main.move)
        )
      }.getOrElse(OutlineBeat(OutlineBeatKind.MainMove, ""))

  private def buildOpeningTheoryBeat(ctx: NarrativeContext, rec: TraceRecorder): Option[OutlineBeat] =
    ctx.openingData.filter(_.totalGames >= 5).flatMap { ref =>
      ref.name.map { name =>
        rec.use("openingData", name, "Opening theory")
        val bead = Math.abs(ctx.hashCode)
        val text = NarrativeLexicon.getOpeningReference(bead, name, ref.totalGames, 0.5)
        OutlineBeat(
          kind = OutlineBeatKind.OpeningTheory,
          text = text,
          conceptIds = List("opening_theory"),
          anchors = name.split(" ").take(2).toList
        )
      }
    }

  private def buildAlternativesBeat(ctx: NarrativeContext, rec: TraceRecorder, bead: Int): OutlineBeat =
    val alts = ctx.candidates.drop(1).take(2)
    if alts.isEmpty then return OutlineBeat(OutlineBeatKind.Alternatives, "")

    val lines = alts.map { c =>
      rec.use(s"candidates[${c.move}]", c.move, "Alternative")
      NarrativeLexicon.getAlternative(bead, c.move, c.whyNot)
    }
    OutlineBeat(kind = OutlineBeatKind.Alternatives, text = lines.mkString("\n"), anchors = alts.map(_.move))

  private def buildWrapUpBeat(ctx: NarrativeContext, bead: Int): Option[OutlineBeat] =
    val parts = scala.collection.mutable.ListBuffer[String]()

    ctx.threats.toUs.headOption.filter(_.lossIfIgnoredCp >= 50).foreach { t =>
      parts += NarrativeLexicon.getThreatWarning(bead, t.kind, t.square)
    }

    ctx.semantic.flatMap(_.practicalAssessment).foreach { pa =>
      parts += NarrativeLexicon.getPracticalVerdict(bead, pa.verdict)
    }

    // Phase 6.8: Compensation (Stranded Asset 3)
    ctx.semantic.flatMap(_.compensation).foreach { comp =>
      parts += NarrativeLexicon.getCompensationStatement(bead, comp.conversionPlan, "Sufficient")
    }

    if parts.isEmpty then None
    else Some(OutlineBeat(kind = OutlineBeatKind.WrapUp, text = parts.mkString(" "), conceptIds = List("practical_assessment")))

  // ===========================================================================
  // Helpers
  // ===========================================================================

  private def formatCp(cp: Int): String =
    val sign = if cp >= 0 then "+" else ""
    val pawns = cp.toDouble / 100
    f"$sign$pawns%.1f"

  private def motifName(m: lila.llm.model.Motif): String =
    m.getClass.getSimpleName.replaceAll("\\$", "")
