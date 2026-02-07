package lila.llm.analysis

import lila.llm.model._
import lila.llm.model.authoring._

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
    buildDecisionBeat(ctx, questions, rec).foreach(beats += _)

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

    // Phase 6 Expansion: Prefix with specific motif if available
    val phase = ctx.phase.current
    val motifs = (ctx.semantic.map(_.conceptSummary).getOrElse(Nil) ++ 
                  ctx.delta.map(_.newMotifs).getOrElse(Nil) ++ 
                  ctx.counterfactual.map(_.missedMotifs.map(_.getClass.getSimpleName)).getOrElse(Nil)).distinct
    val motifSignals = motifs.map(_.toLowerCase)
    val highTensionByMotif =
      motifSignals.exists { m =>
        List(
          "mate",
          "sacrifice",
          "king_hunt",
          "smothered",
          "greek_gift",
          "fork",
          "skewer",
          "deflection",
          "interference",
          "zwischenzug"
        ).exists(m.contains)
      }
    val highTensionByThreat =
      ctx.threats.toUs.headOption.exists(t => t.lossIfIgnoredCp >= 250 || t.kind.equalsIgnoreCase("Mate"))
    val highTension = highTensionByMotif || highTensionByThreat
    val motifHash = motifSignals.foldLeft(0)((acc, m) => acc ^ Math.abs(m.hashCode))

    // Position statement
    val evalOpt = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp)
    val evalText = evalOpt.map(cp => NarrativeLexicon.evalOutcomeClauseFromCp(bead ^ 0x1b873593, cp)).getOrElse("unclear")
    val openingSeed = bead ^ Math.abs(phase.hashCode) ^ evalOpt.getOrElse(0) ^ motifHash ^ 0x1b873593
    val openingPart = NarrativeLexicon.getOpening(openingSeed, phase, evalText, tactical = highTension, ply = ctx.ply)
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
      parts += NarrativeLexicon.getPlanStatement(bead ^ Math.abs(p.name.hashCode) ^ 0x2b2b2b, p.name, ply = ctx.ply)
      concepts += s"plan_${p.name}"
    }

    // One concrete, verified observation to avoid generic boilerplate.
    pickKeyFact(ctx).foreach { fact =>
      val factText = NarrativeLexicon.getFactStatement(bead ^ Math.abs(fact.hashCode), fact)
      if factText.nonEmpty then parts += factText
    }

    // Phase 6.8: Pawn Play (Stranded Asset 1)
    ctx.pawnPlay.breakFile.foreach { br =>
      rec.use("pawnPlay.breakFile", br, "Context break")
      parts += NarrativeLexicon.getPawnPlayStatement(bead, br, ctx.pawnPlay.breakImpact, ctx.pawnPlay.tensionPolicy)
      concepts += "pawn_break_ready"
    }

    OutlineBeat(
      kind = OutlineBeatKind.Context,
      text = parts.filter(_.nonEmpty).mkString(" ").trim,
      conceptIds = concepts.toList
    )

  private def buildDecisionBeat(
    ctx: NarrativeContext,
    questions: List[AuthorQuestion],
    rec: TraceRecorder
  ): Option[OutlineBeat] =
    val nonLatent = questions.filterNot(_.kind == AuthorQuestionKind.LatentPlan)
    nonLatent.headOption.map { q =>
      val alignedQuestion = alignDecisionQuestionWithEvidence(q.question, ctx.authorEvidence.filter(_.questionId == q.id))
      rec.use(s"authorQuestions[${q.id}]", alignedQuestion, "Decision point")
      OutlineBeat(
        kind = OutlineBeatKind.DecisionPoint,
        text = alignedQuestion,
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
      val branches = dedupeEvidenceBranches(relevantEvidence.flatMap(_.branches)).take(3)
      if branches.size >= 2 then
        val labels = List("a)", "b)", "c)")
        val formatted = branches.zip(labels).map { case (b, label) =>
          val evalPart = b.evalCp.map(cp => s" (${formatCp(cp)})").getOrElse("")
          val line0 = Option(b.line).map(_.trim).getOrElse("")
          val key = Option(b.keyMove).map(_.trim).getOrElse("")
          val keySan = normalizedSanHead(key)
          val lineSan = normalizedSanHead(line0)
          val line =
            if line0.isEmpty then key
            else if key.nonEmpty && (line0.startsWith(key) || (keySan.nonEmpty && lineSan == keySan)) then line0
            else if key.nonEmpty then s"$key $line0"
            else line0
          s"$label $line$evalPart"
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
          val line0 = lineSample.trim
          val line =
            if line0.isEmpty then moveSan
            else if moveSan.nonEmpty && line0.startsWith(moveSan) then line0
            else s"$moveSan $line0".trim
          s"$label $line (${formatCp(v.scoreCp)})"
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
    ctx.counterfactual.flatMap { cf =>
      val motifOpt =
        cf.missedMotifs
          .filter(_.category == MotifCategory.Tactical)
          .sortBy(_.plyIndex)
          .headOption
          .orElse {
            cf.missedMotifs
              .filterNot(_.category == MotifCategory.King)
              .sortBy(_.plyIndex)
              .headOption
          }

      val hasTacticalTheme = motifOpt.exists(_.category == MotifCategory.Tactical)
      val shouldShow = cf.cpLoss >= 50 && (hasTacticalTheme || cf.cpLoss >= 150)
      if !shouldShow then None
      else
        rec.use("counterfactual", cf.userMove, "Teaching point")
        val theme =
          motifOpt
            .map(m => NarrativeUtils.humanize(motifName(m)))
            .getOrElse(cf.severity.toLowerCase)
        val text = NarrativeLexicon.getTeachingPoint(bead, theme, cf.cpLoss)
        Some(OutlineBeat(
          kind = OutlineBeatKind.TeachingPoint,
          text = text,
          conceptIds = List("teaching_counterfactual"),
          anchors = List(cf.bestMove)
        ))
    }

  private def buildMainMoveBeat(ctx: NarrativeContext, rec: TraceRecorder, isAnnotation: Boolean, bead: Int): OutlineBeat =
    if isAnnotation then
      val playedSan = ctx.playedSan.getOrElse("")
      val best = ctx.candidates.headOption
      val bestSan = best.map(_.move).getOrElse("")
      val playedUci = ctx.playedMove
      val bestUci = best.flatMap(_.uci)
      val isBest = playedUci.isDefined && playedUci == bestUci

      val playedCand = playedUci.flatMap(uci => ctx.candidates.find(_.uci.contains(uci)))
      val bestCand = best

      val baseText =
        if isBest then NarrativeLexicon.getAnnotationPositive(bead, playedSan)
        else
          val cpLoss = ctx.counterfactual.map(_.cpLoss).getOrElse(0)
          NarrativeLexicon.getAnnotationNegative(bead, playedSan, bestSan, cpLoss)

      val detailText =
        if isBest then
          playedCand.flatMap { c =>
            val b = bead ^ Math.abs(c.move.hashCode)
            val intent = NarrativeLexicon.getIntent(b, c.planAlignment, None)
            val isTerminal = isTerminalAnnotationMove(ctx, playedSan, best)
            val tagHint = annotationTagHint(b, c.tags, c.practicalDifficulty, c.move, ctx.phase.current, isTerminal)
            val alert = c.tacticalAlert.map(_.trim).filter(_.nonEmpty).map(a => s"Note: $a.").getOrElse("")
            val intentSentence = if intent.nonEmpty then s"It $intent." else ""
            val combined = List(intentSentence, tagHint.getOrElse(""), alert).filter(_.trim.nonEmpty).mkString(" ")
            Option.when(combined.nonEmpty)(combined)
          }
        else
          val b = bead ^ Math.abs(playedSan.hashCode)
          val cpLoss = ctx.counterfactual.map(_.cpLoss).getOrElse(0)
          val missedMotif = ctx.counterfactual
            .flatMap(_.missedMotifs.headOption)
            .map(m => NarrativeUtils.humanize(motifName(m)))
          val whyNot = playedCand.flatMap(_.whyNot.map(_.trim).filter(_.nonEmpty))
          val alert = playedCand.flatMap(_.tacticalAlert.map(_.trim).filter(_.nonEmpty))
          val bestReply = playedCand
            .flatMap(_.probeLines.headOption.flatMap(normalizedSanHead))
            .orElse(ctx.engineEvidence.flatMap(_.best.flatMap(_.theirReply).map(_.san)))
          val bestIntent =
            bestCand.map { c =>
              val intent = NarrativeLexicon.getIntent(b ^ Math.abs(c.move.hashCode), c.planAlignment, None)
              if intent.nonEmpty then s"Better is **$bestSan**; it $intent." else ""
            }.getOrElse("")
          val reason = buildConcreteAnnotationIssue(
            bead = b ^ 0x6d2b79f5,
            playedSan = playedSan,
            playedUci = playedUci,
            bestSan = bestSan,
            bestUci = bestUci,
            cpLoss = cpLoss,
            missedMotif = missedMotif,
            whyNot = whyNot,
            alert = alert,
            playedCand = playedCand,
            bestReply = bestReply,
            threatsToUs = ctx.threats.toUs
          )
          val combined = List(reason, bestIntent).filter(_.trim.nonEmpty).mkString(" ")
          Option.when(combined.nonEmpty)(combined)

      val deltaText = buildDeltaAfterMoveText(ctx, bead).getOrElse("")
      val text = List(baseText, detailText.getOrElse(""), deltaText).filter(_.trim.nonEmpty).mkString(" ")

      OutlineBeat(kind = OutlineBeatKind.MainMove, text = text, anchors = List(playedSan, bestSan).filter(_.nonEmpty).distinct)
    else
      ctx.candidates.headOption.map { main =>
        rec.use("candidates[0]", main.move, "Main move")
        val intent = NarrativeLexicon.getIntent(bead, main.planAlignment, None)
        val evalScore = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp).getOrElse(0)
        val evalTerm = NarrativeLexicon.evalOutcomeClauseFromCp(bead ^ 0x85ebca6b, evalScore)
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
    val played = ctx.playedSan.map(_.trim.toLowerCase)
    val deduped = ctx.candidates
      .drop(1)
      .foldLeft(List.empty[CandidateInfo]) { (acc, c) =>
        if acc.exists(_.move.trim.equalsIgnoreCase(c.move.trim)) then acc else acc :+ c
      }
    val alts = deduped.filterNot(c => played.contains(c.move.trim.toLowerCase)).take(2)
    if alts.isEmpty then return OutlineBeat(OutlineBeatKind.Alternatives, "")

    val usedFamilies = scala.collection.mutable.HashSet.empty[String]
    val lines = alts.zipWithIndex.map { case (c, i) =>
      rec.use(s"candidates[${c.move}]", c.move, "Alternative")
      val (line, family) = renderAlternativeDiversified(c, i, bead, usedFamilies.toSet)
      usedFamilies += family
      line
    }
    OutlineBeat(kind = OutlineBeatKind.Alternatives, text = lines.mkString("\n"), anchors = alts.map(_.move))

  private def buildWrapUpBeat(ctx: NarrativeContext, bead: Int): Option[OutlineBeat] =
    val parts = scala.collection.mutable.ListBuffer[String]()
    val cpWhite = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp).getOrElse(0)

    ctx.threats.toUs.headOption.filter(_.lossIfIgnoredCp >= 50).foreach { t =>
      parts += NarrativeLexicon.getThreatWarning(bead, t.kind, t.square)
    }

    ctx.semantic.flatMap(_.practicalAssessment).foreach { pa =>
      val seed = bead ^ Math.abs(pa.verdict.hashCode) ^ (cpWhite << 1)
      parts += NarrativeLexicon.getPracticalVerdict(seed, pa.verdict, cpWhite, ply = ctx.ply)
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

  private def pickKeyFact(ctx: NarrativeContext): Option[Fact] =
    ctx.facts
      .filter(_.scope == FactScope.Now)
      .filterNot {
        case _: Fact.TargetPiece    => true
        case _: Fact.DoubleCheck    => true
        case _: Fact.ActivatesPiece => true
        case _                      => false
      }
      .sortBy {
        case _: Fact.HangingPiece  => 0
        case _: Fact.Pin           => 1
        case _: Fact.Fork          => 2
        case _: Fact.Skewer        => 3
        case _: Fact.PawnPromotion => 4
        case _: Fact.WeakSquare    => 5
        case _: Fact.Outpost       => 6
        case _: Fact.Opposition    => 7
        case _: Fact.KingActivity  => 8
        case _                     => 99
      }
      .headOption

  private def buildDeltaAfterMoveText(ctx: NarrativeContext, bead: Int): Option[String] =
    if !ctx.deltaAfterMove then None
    else
      ctx.delta.flatMap { d =>
        val moverIsWhite = ctx.ply % 2 == 1
        val mover = if moverIsWhite then "White" else "Black"
        val moverCp = if moverIsWhite then d.evalChange else -d.evalChange

        val b = bead ^ Math.abs(mover.hashCode) ^ Math.abs(d.hashCode)
        val evalPart = NarrativeLexicon.getEvalSwingAfterMoveStatement(b, mover, moverCp)

        val phasePart =
          d.phaseChange.flatMap { s =>
            val raw = s.stripPrefix("Transition from ").trim
            raw.split(" to ", 2).toList match
              case from :: to :: Nil if from.nonEmpty && to.nonEmpty =>
                Some(NarrativeLexicon.getPhaseTransitionStatement(b ^ 0x1f1f1f, from, to))
              case _ => None
          }

        val highlightPart: Option[String] =
          phasePart.orElse {
            d.openFileCreated.map(f => NarrativeLexicon.getOpenFileCreatedStatement(b ^ 0x2f2f2f, f))
          }.orElse {
            d.structureChange.map(sc => NarrativeLexicon.getStructureChangeStatement(b ^ 0x3f3f3f, sc))
          }.orElse {
            d.newMotifs.headOption.map(m => NarrativeLexicon.getMotifAppearsStatement(b ^ 0x4f4f4f, NarrativeUtils.humanize(m)))
          }.orElse {
            d.lostMotifs.headOption.map(m => NarrativeLexicon.getMotifFadesStatement(b ^ 0x5f5f5f, NarrativeUtils.humanize(m)))
          }

        val combined = List(evalPart, highlightPart).flatten.filter(_.trim.nonEmpty).mkString(" ")
        Option.when(combined.nonEmpty)(combined)
      }

  private def annotationTagHint(
    bead: Int,
    tags: List[CandidateTag],
    practicalDifficulty: String,
    moveHint: String,
    phase: String,
    isTerminalMove: Boolean
  ): Option[String] =
    val diff = practicalDifficulty.trim.toLowerCase
    val phaseLower = phase.trim.toLowerCase

    if isTerminalMove then
      return Some(NarrativeLexicon.pick(bead, List(
        s"**$moveHint** forces an immediate tactical resolution.",
        s"**$moveHint** ends the game sequence on the spot.",
        s"After **$moveHint**, there is no long maneuvering phase left."
      )))

    val tagHint =
      if tags.contains(CandidateTag.TacticalGamble) then
        Some(NarrativeLexicon.pick(bead, List(
          "It's a tactical try—be ready for a precise response.",
          "This line is a tactical gamble; one mistake can backfire."
        )))
      else if tags.contains(CandidateTag.Sharp) then
        Some(NarrativeLexicon.pick(bead, List(
          "The position stays sharp; calculation matters.",
          "Expect complications—accuracy matters here."
        )))
      else if tags.contains(CandidateTag.Prophylactic) then
        Some(NarrativeLexicon.pick(bead, List(
          "It also limits counterplay.",
          "A useful prophylactic touch, restricting the opponent's options."
        )))
      else if tags.contains(CandidateTag.Converting) then
        Some(NarrativeLexicon.pick(bead, List(
          "It nudges the game toward a cleaner conversion.",
          "A practical converting approach, aiming for a simpler win."
        )))
      else if tags.contains(CandidateTag.Solid) then
        Some(NarrativeLexicon.pick(bead, List(
          "A solid, low-risk choice.",
          "A steady improving move with few drawbacks."
        )))
      else if tags.contains(CandidateTag.Competitive) then
        Some(NarrativeLexicon.pick(bead, List(
          "Several moves are close in strength.",
          "This is a competitive option among several near-equals."
        )))
      else None

    val diffHint =
      if diff.contains("complex") then
        Some(NarrativeLexicon.pick(bead, List(
          s"After **$moveHint**, the line is complex; keep calculating.",
          s"**$moveHint** leads to complex play where precision is rewarded.",
          s"There are tactical resources after **$moveHint**; stay alert.",
          s"**$moveHint** can produce a messy middlegame; calculation matters.",
          s"This is not a line to play on autopilot after **$moveHint**."
        )))
      else if diff.contains("clean") then
        val cleanTemplates =
          if phaseLower == "endgame" then
            List(
              s"The line after **$moveHint** is clean and technical, where subtle king routes and tempi matter.",
              s"**$moveHint** guides play into a precise conversion phase.",
              s"After **$moveHint**, technical details matter more than tactical tricks.",
              s"**$moveHint** keeps the structure stable and highlights endgame technique.",
              s"With **$moveHint**, progress is mostly about methodical coordination."
            )
          else
            List(
              s"The line after **$moveHint** is relatively clean and technical, with less tactical turbulence.",
              s"**$moveHint** lowers immediate tactical volatility and rewards precise coordination.",
              s"After **$moveHint**, the game trends toward a controlled strategic struggle.",
              s"**$moveHint** aims for a tidy continuation with fewer forcing turns.",
              s"With **$moveHint**, planning depth tends to matter more than short tactics.",
              s"**$moveHint** often leads to a stable structure with clear plans."
            )
        Some(NarrativeLexicon.pick(bead, cleanTemplates))
      else None

    tagHint.orElse(diffHint)

  private def isTerminalAnnotationMove(
    ctx: NarrativeContext,
    playedSan: String,
    best: Option[CandidateInfo]
  ): Boolean =
    playedSan.contains("#") ||
      best.exists(_.move.contains("#")) ||
      ctx.engineEvidence.flatMap(_.best.flatMap(_.mate)).exists(m => Math.abs(m) <= 1)

  private def formatCp(cp: Int): String =
    val sign = if cp >= 0 then "+" else ""
    val pawns = cp.toDouble / 100
    f"$sign$pawns%.1f"

  private def buildConcreteAnnotationIssue(
    bead: Int,
    playedSan: String,
    playedUci: Option[String],
    bestSan: String,
    bestUci: Option[String],
    cpLoss: Int,
    missedMotif: Option[String],
    whyNot: Option[String],
    alert: Option[String],
    playedCand: Option[CandidateInfo],
    bestReply: Option[String],
    threatsToUs: List[ThreatRow]
  ): String =
    val threatIssue = unresolvedThreatIssue(threatsToUs, playedSan, playedUci, bestSan, bestUci)
    val factIssue = playedCand.flatMap(c => extractFactConsequence(c.facts)).map(s => s"Issue: $s")
    val alertIssue = alert.map(a => s"Issue: ${a.stripSuffix(".")}.")
    val whyNotIssue = whyNot.flatMap(humanizeWhyNot).map(r => s"Issue: $r.")
    val motifIssue = missedMotif.map(m => s"Issue: this misses the tactical idea of $m.")
    val replyIssue = bestReply.map(r => s"Issue: after this, ...$r gives the opponent a forcing reply.")
    val fallbackIssue = Option.when(cpLoss >= Thresholds.INACCURACY_CP)(s"Issue: ${defaultIssueBySeverity(bead, cpLoss)}.")

    List(threatIssue, factIssue, alertIssue, whyNotIssue, motifIssue, replyIssue, fallbackIssue)
      .flatten
      .find(_.trim.nonEmpty)
      .getOrElse("")

  private def unresolvedThreatIssue(
    threatsToUs: List[ThreatRow],
    playedSan: String,
    playedUci: Option[String],
    bestSan: String,
    bestUci: Option[String]
  ): Option[String] =
    threatsToUs
      .filter(_.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP)
      .find { t =>
        val playedHandles = defenseMatches(t.bestDefense, playedSan, playedUci)
        val bestHandles = defenseMatches(t.bestDefense, bestSan, bestUci)
        !playedHandles && (bestHandles || t.bestDefense.nonEmpty)
      }
      .map { t =>
        val kind = t.kind.toLowerCase
        val square = t.square.map(s => s" on $s").getOrElse("")
        s"Issue: this does not neutralize the $kind threat$square."
      }

  private def defenseMatches(bestDefense: Option[String], san: String, uci: Option[String]): Boolean =
    bestDefense.exists { raw =>
      val defense = normalizeMoveToken(raw)
      val sanNorm = normalizeMoveToken(san)
      val bySan = sanNorm.nonEmpty && (defense == sanNorm || defense.startsWith(sanNorm))
      val byUci = uci.exists(u => defense == normalizeMoveToken(u))
      bySan || byUci
    }

  private def normalizeMoveToken(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
      .replaceAll("""^\d+\.(?:\.\.)?\s*""", "")
      .replaceAll("""^\.{2,}\s*""", "")
      .replaceAll("""[+#?!]+$""", "")
      .replaceAll("\\s+", "")

  private def humanizeWhyNot(raw: String): Option[String] =
    val cleaned = Option(raw).getOrElse("").trim
      .replaceAll("""\(?[-+]?\d+(?:\.\d+)?\s*cp\)?""", "")
      .replaceAll("""\(\s*[-+]?\d+(?:\.\d+)?\s*\)""", "")
      .replaceAll("""\s{2,}""", " ")
      .replaceAll("""\s+\.""", ".")
      .stripSuffix(".")
      .trim
    Option.when(cleaned.nonEmpty)(cleaned)

  private def extractFactConsequence(facts: List[Fact]): Option[String] =
    val prioritized = facts.sortBy {
      case _: Fact.HangingPiece => 0
      case _: Fact.Pin          => 1
      case _: Fact.Fork         => 2
      case _: Fact.Skewer       => 3
      case _: Fact.WeakSquare   => 4
      case _                    => 99
    }

    prioritized.collectFirst {
      case Fact.HangingPiece(square, role, _, defenders, _) if defenders.isEmpty =>
        s"it leaves the ${roleLabel(role)} on ${square.key} hanging."
      case Fact.Pin(_, _, pinned, pinnedRole, behind, behindRole, _, _) =>
        s"it allows a pin on ${pinned.key}, tying the ${roleLabel(pinnedRole)} to the ${roleLabel(behindRole)} on ${behind.key}."
      case Fact.Fork(attacker, attackerRole, targets, _) if targets.nonEmpty =>
        val targetText = targets.take(2).map { case (sq, r) => s"${roleLabel(r)} on ${sq.key}" } match
          case a :: b :: Nil => s"$a and $b"
          case a :: Nil      => a
          case _             => "multiple targets"
        s"it allows a fork by the ${roleLabel(attackerRole)} on ${attacker.key} against $targetText."
      case Fact.Skewer(attacker, attackerRole, front, frontRole, back, backRole, _) =>
        s"it allows a skewer: ${roleLabel(attackerRole)} on ${attacker.key} can hit ${roleLabel(frontRole)} on ${front.key} and then ${roleLabel(backRole)} on ${back.key}."
      case Fact.WeakSquare(square, _, _, _) =>
        s"it creates a durable weakness on ${square.key}."
    }

  private def roleLabel(role: chess.Role): String = role.toString.toLowerCase

  private def defaultIssueBySeverity(bead: Int, cpLoss: Int): String =
    Thresholds.classifySeverity(cpLoss) match
      case "blunder" =>
        NarrativeLexicon.pick(bead, List(
          "this loses material or allows a forcing attack",
          "this fails tactically and the position starts collapsing",
          "this gives the opponent a direct winning sequence"
        ))
      case "mistake" =>
        NarrativeLexicon.pick(bead, List(
          "this hands over the initiative and creates defensive problems",
          "this concedes a serious positional or tactical concession",
          "this gives the opponent an easier route to improve"
        ))
      case "inaccuracy" =>
        NarrativeLexicon.pick(bead, List(
          "this concedes the easier game to the opponent",
          "this gives up useful coordination without compensation",
          "this drifts from the most reliable plan"
        ))
      case _ => ""

  private def motifName(m: lila.llm.model.Motif): String =
    m.getClass.getSimpleName.replaceAll("\\$", "")

  private def alignDecisionQuestionWithEvidence(
    question: String,
    evidence: List[QuestionEvidence]
  ): String =
    if evidence.isEmpty then question
    else
      val lower = question.toLowerCase
      val recaptureMode = lower.contains("recapture")
      val candidates0 = dedupeEvidenceBranches(evidence.flatMap(_.branches))
        .flatMap(b => normalizedSanHead(b.keyMove).orElse(normalizedSanHead(b.line)))
      val candidates =
        if recaptureMode then candidates0.filter(_.contains("x")).distinct
        else candidates0.distinct

      val picked = candidates.take(3)
      if picked.size < 2 then question
      else
        val blackRecapture = recaptureMode && lower.contains("how should black")
        val rendered =
          picked.map { san =>
            val core = san.replaceFirst("""^\.\.\.""", "")
            if blackRecapture then s"...$core" else core
          }
        val stem = question.takeWhile(_ != '—').trim.stripSuffix("?")
        if stem.nonEmpty && question.contains("—") then s"$stem —${joinWithOr(rendered)}?"
        else question

  private def joinWithOr(items: List[String]): String =
    items match
      case Nil => ""
      case one :: Nil => one
      case a :: b :: Nil => s"$a or $b"
      case xs => xs.dropRight(1).mkString(", ") + s", or ${xs.last}"

  private def dedupeEvidenceBranches(branches: List[EvidenceBranch]): List[EvidenceBranch] =
    val seen = scala.collection.mutable.HashSet.empty[String]
    branches.filter { b =>
      val id = normalizedSanHead(b.keyMove).orElse(normalizedSanHead(b.line)).getOrElse("").trim
      if id.isEmpty then true
      else if seen.contains(id) then false
      else
        seen += id
        true
    }

  private def normalizedSanHead(text: String): Option[String] =
    val cleaned = Option(text).getOrElse("").trim
      .replaceAll("""^\d+\.(?:\.\.)?\s*""", "")
      .replaceAll("""^\.{2,}\s*""", "")
    cleaned.split("\\s+").headOption
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.replaceAll("""^[\.\u2026]+""", ""))

  private def renderAlternativeDiversified(
    c: CandidateInfo,
    idx: Int,
    bead: Int,
    usedFamilies: Set[String]
  ): (String, String) =
    val reason = c.whyNot.map(_.trim).filter(_.nonEmpty).map(_.stripSuffix("."))
    val move = c.move.trim
    val plan = c.planAlignment.trim.toLowerCase
    val diff = c.practicalDifficulty.trim.toLowerCase

    val preferredFamilies: List[String] =
      if reason.nonEmpty then List("tradeoff", "practical", "strategic", "generic")
      else if c.tags.contains(CandidateTag.Sharp) || c.tags.contains(CandidateTag.TacticalGamble) || diff.contains("complex") then
        List("dynamic", "strategic", "practical", "generic")
      else if c.tags.contains(CandidateTag.Solid) || c.tags.contains(CandidateTag.Converting) || diff.contains("clean") then
        List("technical", "strategic", "practical", "generic")
      else
        List("strategic", "practical", "dynamic", "technical", "generic")

    val family = preferredFamilies.find(f => !usedFamilies.contains(f)).getOrElse(preferredFamilies.headOption.getOrElse("generic"))
    val localBead = bead ^ Math.abs(move.hashCode) ^ (idx + 1) * 0x9e3779b9

    val line =
      family match
        case "tradeoff" =>
          val r = reason.getOrElse("it concedes dynamic chances")
          NarrativeLexicon.pick(localBead, List(
            s"**$move** is playable, but $r.",
            s"**$move** can work; the tradeoff is that $r.",
            s"Practically speaking, **$move** is viable, but $r."
          ))
        case "dynamic" =>
          NarrativeLexicon.pick(localBead, List(
            s"**$move** keeps the game dynamic and can lead to sharper play.",
            s"**$move** invites complications and active piece play.",
            s"With **$move**, the position stays tense and tactical."
          ))
        case "technical" =>
          NarrativeLexicon.pick(localBead, List(
            s"**$move** is the cleaner technical route, aiming for a stable structure.",
            s"**$move** heads for a controlled position with fewer tactical swings.",
            s"**$move** favors structural clarity and methodical handling over complications."
          ))
        case "strategic" =>
          val planHint =
            if plan.nonEmpty && plan != "unknown" then
              val cleaned = plan.replaceAll("""[_\-]+""", " ").trim
              s" around $cleaned"
            else ""
          NarrativeLexicon.pick(localBead, List(
            s"**$move** is a strategic alternative$planHint.",
            s"**$move** points to a different strategic plan$planHint.",
            s"**$move** takes the game into another strategic channel$planHint."
          ))
        case "practical" =>
          NarrativeLexicon.pick(localBead, List(
            s"In practical play, **$move** is reasonable but needs accurate follow-up.",
            s"**$move** is viable over the board, though move-order precision matters.",
            s"**$move** is playable in practice, but it asks for careful handling."
          ))
        case _ =>
          NarrativeLexicon.getAlternative(localBead, move, c.whyNot)

    (line, family)
