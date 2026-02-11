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
  private val MaxOpeningPrecedents = 3
  private val PrecedentConfidenceThreshold = 0.62
  private val PrefixFamilyLimits = Map(
    "sequence" -> 1,
    "strategic_shift" -> 1,
    "engine" -> 1
  )

  private case class BoardAnchor(text: String, consumedThreat: Boolean = false, consumedFact: Boolean = false)
  private case class AlternativeEngineSignal(
    rank: Option[Int],
    cpLoss: Option[Int],
    bestSan: Option[String]
  )
  private enum PrecedentMechanism:
    case TacticalPressure
    case ExchangeCascade
    case PromotionRace
    case StructuralTransformation
    case InitiativeSwing

  private case class PrecedentSignal(
    triggerMove: String,
    replyMove: Option[String],
    pivotMove: Option[String],
    mechanism: PrecedentMechanism,
    confidence: Double
  )

  private case class OpeningPrecedentLine(
    text: String,
    score: Int,
    year: Int,
    game: ExplorerGame,
    overlap: Int,
    metadataScore: Int
  )
  private case class CrossBeatRepetitionState(
    usedStems: scala.collection.mutable.Set[String],
    prefixCounts: scala.collection.mutable.Map[String, Int],
    usedHypothesisFamilies: scala.collection.mutable.Set[String],
    usedHypothesisStems: scala.collection.mutable.Set[String]
  )
  private case class SelectedHypothesis(
    card: HypothesisCard,
    sourceMove: String
  )
  private enum PrecedentRole:
    case Sequence
    case StrategicTransition
    case DecisionDriver

  def build(ctx: NarrativeContext, rec: TraceRecorder): (NarrativeOutline, OutlineDiagnostics) =
    val bead = Math.abs(ctx.hashCode)
    val beats = scala.collection.mutable.ListBuffer.empty[OutlineBeat]
    val crossBeatState = CrossBeatRepetitionState(
      scala.collection.mutable.HashSet.empty[String],
      scala.collection.mutable.HashMap.empty[String, Int].withDefaultValue(0),
      scala.collection.mutable.HashSet.empty[String],
      scala.collection.mutable.HashSet.empty[String]
    )
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
    val moveLevelPrecedent = buildContextPrecedentSentence(ctx, bead)
    val mainMoveBeat = buildMainMoveBeat(ctx, rec, isAnnotation, bead, moveLevelPrecedent, crossBeatState)
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
    buildOpeningTheoryBeat(ctx, rec, suppressPrecedents = moveLevelPrecedent.nonEmpty).foreach(beats += _)

    // 9. ALTERNATIVES
    val altBeat = buildAlternativesBeat(ctx, rec, bead, crossBeatState)
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

    // Phase 6 Expansion: two-stage lead construction (motif + phase lead) with repeat suppression.
    val phase = ctx.phase.current
    val conceptSummaryMotifs = ctx.semantic.toList.flatMap(_.conceptSummary).map(_.trim).filter(_.nonEmpty).distinct
    val derivedContextMotifs = collectDerivedContextMotifs(ctx)
    val conceptMotifs = (conceptSummaryMotifs ++ derivedContextMotifs).distinct
    val deltaMotifs = ctx.delta.map(_.newMotifs).getOrElse(Nil)
    val counterfactualMotifs = ctx.counterfactual.map(_.missedMotifs.map(_.getClass.getSimpleName)).getOrElse(Nil)
    val motifs = (deltaMotifs ++ counterfactualMotifs ++ conceptMotifs).distinct
    val motifSignals = motifs.map(normalizeMotifKey).filter(_.nonEmpty)
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
    val evalOpt = rankedEngineVariations(ctx).headOption.map(_.scoreCp).orElse(ctx.engineEvidence.flatMap(_.best).map(_.scoreCp))
    val evalText = evalOpt.map(cp => NarrativeLexicon.evalOutcomeClauseFromCp(bead ^ 0x1b873593, cp, ply = ctx.ply)).getOrElse("unclear")
    val openingSeed = bead ^ Math.abs(phase.hashCode) ^ evalOpt.getOrElse(0) ^ motifHash ^ 0x1b873593
    val openingPart = NarrativeLexicon.getOpening(openingSeed, phase, evalText, tactical = highTension, ply = ctx.ply)
    val keyFact = pickKeyFact(ctx)
    val salientConceptMotifs =
      conceptMotifs.filter(NarrativeLexicon.isMotifPrefixSignal)
    val conceptLeadMotifs =
      if salientConceptMotifs.nonEmpty then salientConceptMotifs.take(2)
      else if ctx.ply % 3 == 0 then conceptMotifs.take(2)
      else Nil
    val deltaMotifSignals = deltaMotifs.map(normalizeMotifKey).filter(_.nonEmpty)
    val counterfactualMotifSignals = counterfactualMotifs.map(normalizeMotifKey).filter(_.nonEmpty)
    val conceptSummarySignals = conceptSummaryMotifs.map(normalizeMotifKey).filter(_.nonEmpty)
    val derivedContextSignals = derivedContextMotifs.map(normalizeMotifKey).filter(_.nonEmpty)
    val trustedConceptThemeSignals =
      conceptSummaryMotifs
        .map(_.trim)
        .filter(_.nonEmpty)
        .filter { raw =>
          val motif = normalizeMotifKey(raw)
          val stableStrategicConcept =
            List(
              "minority_attack",
              "bad_bishop",
              "bishop_pair",
              "opposite_bishops",
              "isolated_pawn",
              "hanging_pawns",
              "outpost",
              "open_file",
              "semi_open_file",
              "color_complex"
            ).exists(motif.contains)
          motif.nonEmpty &&
          motifPhaseCompatible(motif, phase) && (
            derivedContextSignals.exists(sig => motifSignalMatches(sig, motif)) ||
            deltaMotifSignals.exists(sig => motifSignalMatches(sig, motif)) ||
            counterfactualMotifSignals.exists(sig => motifSignalMatches(sig, motif)) ||
            keyFact.exists(f => motifCorroboratedByFact(motif, f)) ||
            motifCorroboratedByThreat(motif, ctx.threats.toUs) ||
            motifCorroboratedByPawnPlay(motif, ctx.pawnPlay) ||
            stableStrategicConcept
          )
        }
        .distinct
    val motifPrefixCandidates =
      (conceptLeadMotifs ++ deltaMotifs ++ counterfactualMotifs)
        .map(_.trim)
        .filter(_.nonEmpty)
        .filter(m => motifPhaseCompatible(m, phase))
        .distinct
        .filter { raw =>
          isTrustedMotifPrefixCandidate(
            rawMotif = raw,
            deltaSignals = deltaMotifSignals,
            counterfactualSignals = counterfactualMotifSignals,
            conceptSummarySignals = conceptSummarySignals,
            derivedSignals = derivedContextSignals,
            keyFact = keyFact,
            threatsToUs = ctx.threats.toUs,
            pawnPlay = ctx.pawnPlay,
            phase = phase
          )
        }
        .take(4)
    val motifPrefix = NarrativeLexicon.getMotifPrefix(bead ^ motifHash, motifPrefixCandidates, ply = ctx.ply)

    val boardAnchor = buildBoardAnchor(ctx, keyFact, bead)
    boardAnchor.foreach(a => parts += a.text)

    val leadText = List(motifPrefix.map(_.trim).getOrElse(""), openingPart.trim).filter(_.nonEmpty).mkString(" ").trim
    parts += leadText
    val existingThemeText = List(boardAnchor.map(_.text).getOrElse(""), leadText).mkString(" ")
    val themeSignalPool =
      (derivedContextMotifs ++ deltaMotifs ++ counterfactualMotifs ++ trustedConceptThemeSignals)
        .distinct
    buildThemeKeywordSentence(
      motifs = themeSignalPool,
      existingText = existingThemeText,
      bead = bead ^ 0x6d2b79f5,
      ply = ctx.ply,
      phase = phase
    )
      .foreach(parts += _)

    // Main threat if exists (adds drama)
    ctx.threats.toUs.headOption.filter(_.lossIfIgnoredCp >= 30).foreach { t =>
      rec.use("threats.toUs[0]", t.kind, "Context threat")
      if !boardAnchor.exists(_.consumedThreat) then
        parts += NarrativeLexicon.getThreatStatement(bead, t.kind, t.lossIfIgnoredCp)
      concepts += s"threat_${t.kind}"
    }

    // Top plan (strategic direction). Filter speculative tactical labels unless board evidence supports them.
    ctx.plans.top5.find { p =>
      val planKey = normalizeMotifKey(p.name)
      val needsTacticalProof =
        List("sacrifice", "mate", "smothered", "trap", "combination").exists(planKey.contains)
      val sacrificeSpecific =
        planKey.contains("sacrifice")
      val hasSacrificeEvidence =
        ctx.candidates.exists { c =>
          val evidenceLow = c.tacticEvidence.mkString(" ").toLowerCase
          val whyLow = c.whyNot.getOrElse("").toLowerCase
          evidenceLow.contains("sacrifice") ||
            evidenceLow.contains("exchange_sacrifice") ||
            whyLow.contains("sacrifice")
        } ||
          ctx.delta.map(_.newMotifs.mkString(" ").toLowerCase.contains("sacrifice")).getOrElse(false)
      if !needsTacticalProof then true
      else if sacrificeSpecific then hasSacrificeEvidence
      else
        val hasTacticalProof =
          ctx.candidates.exists { c =>
            c.tags.exists(tag => tag == CandidateTag.Sharp || tag == CandidateTag.TacticalGamble) ||
              c.facts.exists {
                case _: Fact.Fork | _: Fact.Pin | _: Fact.Skewer | _: Fact.HangingPiece => true
                case _                                                                   => false
              }
          } ||
            ctx.threats.toUs.exists(t =>
              t.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP || t.kind.toLowerCase.contains("mate")
            )
        hasTacticalProof
    }.foreach { p =>
      rec.use("plans.top5[0]", p.name, "Context plan")
      parts += NarrativeLexicon.getPlanStatement(bead ^ Math.abs(p.name.hashCode) ^ 0x2b2b2b, p.name, ply = ctx.ply)
      concepts += s"plan_${p.name}"
    }

    // One concrete, verified observation to avoid generic boilerplate.
    if !boardAnchor.exists(_.consumedFact) then
      keyFact.foreach { fact =>
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
      val variations = sortVariationsForSideToMove(ctx.fen, ev.variations).take(3)
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

  private def buildMainMoveBeat(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    isAnnotation: Boolean,
    bead: Int,
    precedentTextOpt: Option[String],
    crossBeatState: CrossBeatRepetitionState
  ): OutlineBeat =
    if isAnnotation then
      val playedSan = ctx.playedSan.getOrElse("")
      val playedUci = ctx.playedMove.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)
      val engineBest = rankedEngineVariations(ctx).headOption
      val engineBestUci = engineBest
        .flatMap(_.moves.headOption)
        .map(NarrativeUtils.normalizeUciMove)
        .filter(_.nonEmpty)
      val engineBestSan = engineBest
        .flatMap(_.ourMove.map(_.san))
        .filter(_.trim.nonEmpty)
        .orElse(engineBestUci.map(uci => NarrativeUtils.uciToSanOrFormat(ctx.fen, uci)).filter(_.trim.nonEmpty))
      val best = engineBestUci
        .flatMap(bestU => ctx.candidates.find(_.uci.exists(cu => NarrativeUtils.uciEquivalent(cu, bestU))))
        .orElse(ctx.candidates.headOption)
      val bestSan = engineBestSan.orElse(best.map(_.move).filter(_.trim.nonEmpty)).getOrElse("")
      val bestUci = engineBestUci.orElse(best.flatMap(_.uci).map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty))
      val playedRank = playedMoveRank(ctx, playedUci, playedSan)
      val sameAsBestByUci =
        playedUci.exists(pu => bestUci.exists(bu => NarrativeUtils.uciEquivalent(pu, bu)))
      val sameAsBestBySan =
        playedSan.nonEmpty &&
          bestSan.nonEmpty &&
          normalizeMoveToken(playedSan) == normalizeMoveToken(bestSan)
      val cpLoss = resolveCpLoss(ctx, playedUci, playedSan, playedRank)
      val hasEngineReference = playedRank.nonEmpty || bestUci.nonEmpty
      val isBest =
        if hasEngineReference then playedRank.contains(1) || sameAsBestByUci
        else sameAsBestByUci || sameAsBestBySan

      val playedCand = playedUci.flatMap(uci => ctx.candidates.find(_.uci.exists(cu => NarrativeUtils.uciEquivalent(cu, uci))))
      val bestCand = best

      val baseText =
        if isBest then NarrativeLexicon.getAnnotationPositive(bead, playedSan)
        else
          NarrativeLexicon.getAnnotationNegative(bead, playedSan, bestSan, cpLoss)

      val detailText =
        if isBest then
          playedCand.flatMap { c =>
            val b = bead ^ Math.abs(c.move.hashCode)
            val intent = NarrativeLexicon.getIntent(b, c.planAlignment, None, ply = ctx.ply)
            val isTerminal = isTerminalAnnotationMove(ctx, playedSan, bestCand)
            val tagHint = annotationTagHint(b, c.tags, c.practicalDifficulty, c.move, ctx.phase.current, isTerminal)
            val alert = c.tacticalAlert.map(_.trim).filter(_.nonEmpty).map(a => s"Note: $a.").getOrElse("")
            val intentSentence = if intent.nonEmpty then s"It $intent." else ""
            val combined = List(intentSentence, tagHint.getOrElse(""), alert).filter(_.trim.nonEmpty).mkString(" ")
            Option.when(combined.nonEmpty)(combined)
          }
        else
          val b = bead ^ Math.abs(playedSan.hashCode)
          val missedMotif = ctx.counterfactual
            .flatMap(_.missedMotifs.headOption)
            .map(m => NarrativeUtils.humanize(motifName(m)))
          val whyNot = playedCand.flatMap(_.whyNot.map(_.trim).filter(_.nonEmpty))
          val alert = playedCand.flatMap(_.tacticalAlert.map(_.trim).filter(_.nonEmpty))
          val bestReply = playedCand
            .flatMap(_.probeLines.headOption.flatMap(normalizedSanHead))
            .orElse(engineBest.flatMap(_.theirReply).map(_.san))
          val bestIntent =
            bestCand.map { c =>
              val intent = NarrativeLexicon.getIntent(b ^ Math.abs(c.move.hashCode), c.planAlignment, None, ply = ctx.ply + 1)
              if intent.nonEmpty then s"Better is **$bestSan**; it $intent."
              else s"Better is **$bestSan** to keep tighter control of the position."
            }.getOrElse {
              if bestSan.nonEmpty then s"Better is **$bestSan** to keep tighter control of the position."
              else ""
            }
          val rankContext = NarrativeLexicon.getEngineRankContext(
            bead = b ^ 0x27d4eb2f,
            rank = playedRank,
            bestSan = bestSan,
            cpLoss = cpLoss
          )
          val reason = buildConcreteAnnotationIssue(
            bead = b ^ 0x6d2b79f5,
            playedSan = playedSan,
            playedUci = playedUci,
            bestSan = bestSan,
            bestUci = bestUci,
            cpLoss = cpLoss,
            playedRank = playedRank,
            missedMotif = missedMotif,
            whyNot = whyNot,
            alert = alert,
            playedCand = playedCand,
            bestReply = bestReply,
            threatsToUs = ctx.threats.toUs,
            contextHint = Math.abs(playedSan.hashCode)
          )
          val combined = composeCausalAnnotation(
            rankContext = rankContext,
            reason = reason,
            bestIntent = bestIntent,
            bead = b ^ 0x3f84d5b5,
            usedStems = crossBeatState.usedStems.toSet,
            prefixCounts = crossBeatState.prefixCounts.toMap
            )
          Option.when(combined.nonEmpty)(combined)

      val hypothesisText =
        buildMainHypothesisNarrative(
          ctx = ctx,
          focusCandidate = playedCand.orElse(bestCand),
          supportCandidate = bestCand.filter(b => !playedCand.contains(b)).orElse(ctx.candidates.lift(1)),
          bead = bead ^ Math.abs(playedSan.hashCode) ^ 0x7f4a7c15,
          crossBeatState = crossBeatState
        ).getOrElse("")

      val deltaText = buildDeltaAfterMoveText(ctx, bead).getOrElse("")
      val precedentText = precedentTextOpt.getOrElse("")
      if precedentText.nonEmpty then
        rec.use("openingData.sampleGames", "1", "Move-level precedent")
      val shouldUsePrecedentFallback =
        precedentText.isEmpty &&
          ctx.openingData.exists(_.sampleGames.isEmpty) &&
          ctx.openingEvent.exists(isCoreOpeningEvent)
      val precedentBridge =
        if !shouldUsePrecedentFallback then ""
        else buildPrecedentFallbackSentence(ctx, bead ^ 0x56f839d3, scope = "main").getOrElse("")
      val rawText = List(baseText, detailText.getOrElse(""), hypothesisText, deltaText, precedentBridge, precedentText)
        .filter(_.trim.nonEmpty)
        .mkString(" ")
      val tonedText = harmonizeAnnotationTone(rawText, cpLoss, isBest, bead ^ Math.abs(playedSan.hashCode))
      val text = enforceAnnotationPolarity(tonedText, cpLoss, isBest, bead ^ Math.abs(bestSan.hashCode))
      if text.trim.nonEmpty then trackTemplateUsage(text, crossBeatState.usedStems, crossBeatState.prefixCounts)

      OutlineBeat(kind = OutlineBeatKind.MainMove, text = text, anchors = List(playedSan, bestSan).filter(_.nonEmpty).distinct)
    else
      ctx.candidates.headOption.map { main =>
        rec.use("candidates[0]", main.move, "Main move")
        val intent = NarrativeLexicon.getIntent(bead, main.planAlignment, None, ply = ctx.ply)
        val engineBest = rankedEngineVariations(ctx).headOption.orElse(ctx.engineEvidence.flatMap(_.best))
        val evalScore = engineBest.map(_.scoreCp).orElse(ctx.engineEvidence.flatMap(_.best).map(_.scoreCp)).getOrElse(0)
        val evalTerm = NarrativeLexicon.evalOutcomeClauseFromCp(bead ^ 0x85ebca6b, evalScore, ply = ctx.ply)
        val replySan = engineBest.flatMap(_.theirReply).map(_.san)
        val sampleRest = engineBest.flatMap(_.sampleLineFrom(2, 6))

        val text = NarrativeLexicon.getMainFlow(bead, main.move, main.annotation, intent, replySan, sampleRest, evalTerm)
        val hypothesisText =
          buildMainHypothesisNarrative(
            ctx = ctx,
            focusCandidate = Some(main),
            supportCandidate = ctx.candidates.lift(1),
            bead = bead ^ Math.abs(main.move.hashCode) ^ 0x4f6cdd1d,
            crossBeatState = crossBeatState
          ).getOrElse("")
        
        // Phase 6.8: Prophylaxis (Stranded Asset 2)
        val prophylaxisText = ctx.semantic.flatMap(_.preventedPlans.headOption).map { pp =>
          NarrativeLexicon.getPreventedPlanStatement(bead, pp.planId)
        }
        val precedentText = precedentTextOpt
        precedentText.foreach(_ => rec.use("openingData.sampleGames", "1", "Move-level precedent"))
        val shouldUsePrecedentFallback =
          precedentText.isEmpty &&
            ctx.openingData.exists(_.sampleGames.isEmpty) &&
            ctx.openingEvent.exists(isCoreOpeningEvent)
        val precedentBridge =
          if !shouldUsePrecedentFallback then ""
          else buildPrecedentFallbackSentence(ctx, bead ^ 0x4f6cdd1d, scope = "main").getOrElse("")
        val mergedText =
          List(text, hypothesisText, prophylaxisText.getOrElse(""), precedentBridge, precedentText.getOrElse(""))
            .filter(_.trim.nonEmpty)
            .mkString(" ")
        if mergedText.trim.nonEmpty then trackTemplateUsage(mergedText, crossBeatState.usedStems, crossBeatState.prefixCounts)

        OutlineBeat(
          kind = OutlineBeatKind.MainMove, 
          text = mergedText, 
          anchors = List(main.move)
        )
      }.getOrElse(OutlineBeat(OutlineBeatKind.MainMove, ""))

  private def buildOpeningTheoryBeat(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    suppressPrecedents: Boolean
  ): Option[OutlineBeat] =
    val openingRef = ctx.openingData
    val openingText = openingRef.filter(_.totalGames >= 5).flatMap { ref =>
      ref.name.map { name =>
        rec.use("openingData", name, "Opening theory")
        val bead = Math.abs(ctx.hashCode)
        NarrativeLexicon.getOpeningReference(bead, name, ref.totalGames, 0.5)
      }
    }
    val precedentSnippets =
      if suppressPrecedents then Nil
      else buildOpeningPrecedentSnippets(ctx, openingRef, Math.abs(ctx.hashCode) ^ 0x4b1d0f6a)
    if precedentSnippets.nonEmpty then
      rec.use("openingData.sampleGames", precedentSnippets.length.toString, "Opening precedents")

    val precedentBridge =
      if precedentSnippets.nonEmpty || openingText.isEmpty || !openingRef.exists(_.sampleGames.isEmpty) then ""
      else buildPrecedentFallbackSentence(ctx, Math.abs(ctx.hashCode) ^ 0x19f8b4ad, scope = "opening").getOrElse("")
    val text = List(openingText.getOrElse(""), precedentBridge, precedentSnippets.mkString(" "))
      .filter(_.trim.nonEmpty)
      .mkString(" ")
      .trim
    if text.isEmpty then None
    else
      val anchors = openingRef.flatMap(_.name).map(_.split(" ").take(2).toList).getOrElse(Nil)
      val concepts =
        if precedentSnippets.nonEmpty then List("opening_theory", "opening_precedent")
        else List("opening_theory")
      Some(OutlineBeat(
        kind = OutlineBeatKind.OpeningTheory,
        text = text,
        conceptIds = concepts,
        anchors = anchors
      ))

  private def buildPrecedentFallbackSentence(
    ctx: NarrativeContext,
    bead: Int,
    scope: String
  ): Option[String] =
    val planHint =
      ctx.plans.top5.headOption
        .map(_.name.replaceAll("""[_\-]+""", " ").trim.toLowerCase)
        .filter(_.nonEmpty)
    val evalCp = rankedEngineVariations(ctx).headOption.map(_.scoreCp).orElse(ctx.engineEvidence.flatMap(_.best).map(_.scoreCp))
    val evalHint = evalCp.map(cp => NarrativeLexicon.evalOutcomeClauseFromCp(bead ^ 0x7f4a7c15, cp, ply = ctx.ply))
    val templates =
      scope.trim.toLowerCase match
        case "opening" =>
          List(
            "At this branch, practical handling matters more than memorized reference games.",
            "This node is best treated as a live practical decision rather than a model-game recall test.",
            "From here, over-the-board plan execution matters more than historical comparison."
          )
        case _ =>
          val planAware = planHint.map(p => s"around $p").getOrElse("from this structure")
          List(
            s"In practical terms, the key is to keep plans coherent $planAware.",
            s"The position is decided more by accurate follow-up than by historical templates $planAware.",
            s"From this point, practical move-order discipline is the main guide $planAware."
          )
    val rendered = NarrativeLexicon.pick(bead ^ 0x2f6e2b1, templates)
    val withEval =
      evalHint match
        case Some(eval) if scope.trim.equalsIgnoreCase("opening") => s"$rendered $eval."
        case _                                                    => rendered
    Option.when(withEval.trim.nonEmpty)(withEval.trim)

  private def buildOpeningPrecedentSnippets(
    ctx: NarrativeContext,
    openingRef: Option[OpeningReference],
    bead: Int
  ): List[String] =
    if !ctx.openingEvent.exists(isCoreOpeningEvent) then Nil
    else
      val lines = rankedOpeningPrecedentLines(ctx, openingRef, requireFocus = false).take(MaxOpeningPrecedents)
      if lines.isEmpty then Nil
      else if shouldUsePrecedentComparison(ctx, lines, requireFocus = false) then
        List(renderPrecedentComparison(lines, bead))
      else
        lines
          .map(line => renderPrecedentBlock(line, bead))
          .filter(_.trim.nonEmpty)

  private def buildContextPrecedentSentence(ctx: NarrativeContext, bead: Int): Option[String] =
    val introOnly = ctx.openingEvent.exists {
      case OpeningEvent.Intro(_, _, _, _) => true
      case _                              => false
    }
    if introOnly then None
    else
      val lines = rankedOpeningPrecedentLines(ctx, ctx.openingData, requireFocus = true).take(MaxOpeningPrecedents)
      if lines.isEmpty then None
      else if shouldUsePrecedentComparison(ctx, lines, requireFocus = true) then
        Some(renderPrecedentComparison(lines, bead))
      else
        lines.headOption.map(line => renderPrecedentBlock(line, bead))

  private def shouldUsePrecedentComparison(
    ctx: NarrativeContext,
    lines: List[OpeningPrecedentLine],
    requireFocus: Boolean
  ): Boolean =
    if lines.size < 2 then false
    else
      val branchLikeEvent = ctx.openingEvent.exists {
        case OpeningEvent.BranchPoint(_, _, _) => true
        case OpeningEvent.OutOfBook(_, _, _)   => true
        case OpeningEvent.TheoryEnds(_, _)     => true
        case OpeningEvent.Novelty(_, _, _, _)  => true
        case _                                 => false
      }
      val highConfidenceSignals =
        lines
          .flatMap(buildPrecedentSignal)
          .count(_.confidence >= PrecedentConfidenceThreshold)
      branchLikeEvent || (requireFocus && highConfidenceSignals >= 2)

  private def renderPrecedentComparison(
    lines: List[OpeningPrecedentLine],
    bead: Int
  ): String =
    val ranked = lines.take(MaxOpeningPrecedents)
    val rankedWithSignals =
      ranked.map { line =>
        line -> buildPrecedentSignal(line).filter(_.confidence >= PrecedentConfidenceThreshold)
      }
    val header = NarrativeLexicon.pick(bead ^ 0x57f1a235, List(
      "Comparable master branches from this split:",
      "At this branch, master games diverged in three practical directions:",
      "Reference branches from elite games at this point:"
    ))
    val usedStems = scala.collection.mutable.HashSet.empty[String]
    val prefixCounts = scala.collection.mutable.HashMap.empty[String, Int].withDefaultValue(0)

    val items = rankedWithSignals.zipWithIndex.map { case ((line, signal), idx) =>
      val label = ('A' + idx).toChar
      val itemSeed = bead ^ Math.abs(line.text.hashCode) ^ ((idx + 1) * 0x9e3779b9)
      val role =
        idx match
          case 0 => PrecedentRole.Sequence
          case 1 => PrecedentRole.StrategicTransition
          case _ => PrecedentRole.DecisionDriver
      val roleTemplates = signal.toList.flatMap { s =>
        role match
          case PrecedentRole.Sequence =>
            (0 until 4).toList.map { step =>
              NarrativeLexicon.getPrecedentRouteLine(
                bead = itemSeed ^ (step * 0x27d4eb2f),
                triggerMove = s.triggerMove,
                replyMove = s.replyMove,
                pivotMove = s.pivotMove
              )
            }
          case PrecedentRole.StrategicTransition =>
            val mechanism = precedentMechanismLabel(s.mechanism)
            (0 until 4).toList.map { step =>
              NarrativeLexicon.getPrecedentStrategicTransitionLine(
                bead = itemSeed ^ (step * 0x7f4a7c15),
                mechanism = mechanism
              )
            }
          case PrecedentRole.DecisionDriver =>
            val mechanism = precedentMechanismLabel(s.mechanism)
            (0 until 4).toList.map { step =>
              NarrativeLexicon.getPrecedentDecisionDriverLine(
                bead = itemSeed ^ (step * 0x6d2b79f5),
                mechanism = mechanism
              )
            }
      }.map(_.trim).filter(_.nonEmpty).distinct
      val roleLine =
        if roleTemplates.isEmpty then ""
        else
          val selected = selectNonRepeatingTemplate(
            templates = roleTemplates,
            seed = itemSeed ^ 0x4f1bbcdc,
            usedStems = usedStems.toSet,
            prefixCounts = prefixCounts.toMap,
            prefixLimits = PrefixFamilyLimits
          )
          trackTemplateUsage(selected, usedStems, prefixCounts)
          selected

      val parts = List(line.text.trim, roleLine).filter(_.nonEmpty)
      s"$label) ${parts.mkString(" ")}"
    }

    val summaryTemplates =
      buildPrecedentComparisonSummaryTemplates(
        rankedWithSignals.flatMap(_._2.map(_.mechanism))
      )
    val summary =
      if summaryTemplates.isEmpty then ""
      else
        val selected = selectNonRepeatingTemplate(
          templates = summaryTemplates,
          seed = bead ^ 0x3c6ef372,
          usedStems = usedStems.toSet,
          prefixCounts = prefixCounts.toMap,
          prefixLimits = PrefixFamilyLimits
        )
        trackTemplateUsage(selected, usedStems, prefixCounts)
        selected

    List(header, items.mkString(" "), summary).filter(_.nonEmpty).mkString(" ")

  private def precedentMechanismLabel(mechanism: PrecedentMechanism): String =
    mechanism match
      case PrecedentMechanism.TacticalPressure =>
        "forcing tactical pressure around king safety and move order"
      case PrecedentMechanism.ExchangeCascade =>
        "exchange timing that simplified into a cleaner structure"
      case PrecedentMechanism.PromotionRace =>
        "promotion threats forcing both sides into tempo-driven play"
      case PrecedentMechanism.StructuralTransformation =>
        "pawn-structure transformation that redirected long-term plans"
      case PrecedentMechanism.InitiativeSwing =>
        "initiative swings created by faster piece activity"

  private def buildPrecedentComparisonSummaryTemplates(
    mechanisms: List[PrecedentMechanism]
  ): List[String] =
    if mechanisms.isEmpty then Nil
    else
      val grouped = mechanisms.groupBy(identity).view.mapValues(_.size).toMap
      val dominant = grouped.maxBy(_._2)._1
      val dominantLabel = precedentMechanismLabel(dominant)
      val diversity = grouped.size
      if diversity >= 2 then
        List(
          s"Across these branches, results changed by which side better handled $dominantLabel.",
          s"Common pattern: the side that managed $dominantLabel more accurately got the practical edge.",
          s"Shared lesson: this split is decided less by result labels and more by control of $dominantLabel."
        )
      else
        List(
          s"All cited branches revolve around $dominantLabel.",
          s"The recurring practical theme across these games is $dominantLabel.",
          s"These precedent lines point to one key driver: $dominantLabel."
        )

  private def renderPrecedentBlock(
    line: OpeningPrecedentLine,
    bead: Int
  ): String =
    val anchorMove = line.game.pgn.flatMap(raw => openingPrecedentSanMoves(raw).headOption)
    val lead = NarrativeLexicon.getPrecedentLead(
      bead = bead ^ Math.abs(line.text.hashCode),
      factualLine = line.text,
      anchorMove = anchorMove
    )
    val mechanismLine =
      buildPrecedentSignal(line)
        .filter(_.confidence >= PrecedentConfidenceThreshold)
        .map { signal =>
          NarrativeLexicon.getPrecedentMechanismLine(
            bead = bead ^ Math.abs(signal.triggerMove.hashCode) ^ Math.abs(signal.mechanism.toString.hashCode),
            triggerMove = signal.triggerMove,
            replyMove = signal.replyMove,
            pivotMove = signal.pivotMove,
            mechanism = signal.mechanism.toString
          )
        }
        .filter(_.trim.nonEmpty)
    List(lead.trim, mechanismLine.getOrElse("").trim).filter(_.nonEmpty).mkString(" ")

  private def rankedOpeningPrecedentLines(
    ctx: NarrativeContext,
    openingRef: Option[OpeningReference],
    requireFocus: Boolean
  ): List[OpeningPrecedentLine] =
    val focusMoves = openingPrecedentFocusMoves(ctx)
    openingRef.toList
      .flatMap(_.sampleGames)
      .flatMap { game =>
        formatOpeningPrecedentSnippet(game).map { text =>
          val overlap = openingPrecedentOverlap(game, focusMoves)
          val metadataScore = openingPrecedentMetadataScore(game)
          val score = openingPrecedentScore(overlap, metadataScore, requireFocus)
          OpeningPrecedentLine(
            text = text,
            score = score,
            year = game.year,
            game = game,
            overlap = overlap,
            metadataScore = metadataScore
          )
        }
      }
      .filter(_.score > 0)
      .sortBy(line => (-line.score, -line.year, line.text))

  private def openingPrecedentFocusMoves(ctx: NarrativeContext): Set[String] =
    val played = ctx.playedSan.toList
    val best = rankedEngineVariations(ctx).headOption.flatMap(_.ourMove.map(_.san)).toList
    val candidateMoves = ctx.candidates.take(3).map(_.move)
    val openingTopMoves = ctx.openingData.toList.flatMap(_.topMoves.take(2).map(_.san))
    (played ++ best ++ candidateMoves ++ openingTopMoves)
      .map(normalizeMoveToken)
      .filter(_.nonEmpty)
      .toSet

  private def openingPrecedentOverlap(game: ExplorerGame, focusMoves: Set[String]): Int =
    if focusMoves.isEmpty then 0
    else openingPrecedentMoveTokens(game.pgn).count(focusMoves.contains)

  private def openingPrecedentMetadataScore(game: ExplorerGame): Int =
    (if game.year > 0 then 3 else 0) +
      (if game.winner.isDefined then 2 else 0) +
      (if normalizeExplorerPlayer(game.white.name).isDefined then 1 else 0) +
      (if normalizeExplorerPlayer(game.black.name).isDefined then 1 else 0) +
      (if game.event.exists(_.trim.nonEmpty) then 2 else 0) +
      (if game.pgn.exists(_.trim.nonEmpty) then 3 else 0)

  private def openingPrecedentScore(overlap: Int, metadataScore: Int, requireFocus: Boolean): Int =
    val overlapBonus = overlap match
      case n if n >= 2 => 10
      case 1           => 6
      case _ if requireFocus => -8
      case _           => 0
    metadataScore + overlapBonus

  private def buildPrecedentSignal(line: OpeningPrecedentLine): Option[PrecedentSignal] =
    line.game.pgn.flatMap { raw =>
      val sanMoves = openingPrecedentSanMoves(raw)
      sanMoves.headOption.map { trigger =>
        val reply = sanMoves.lift(1)
        val pivot = sanMoves.lift(2)
        val captures = sanMoves.count(_.contains("x"))
        val checks = sanMoves.count(m => m.contains("+") || m.contains("#"))
        val promotions = sanMoves.count(_.contains("="))
        val pawnPushes = sanMoves.count(isLikelyPawnMove)
        val pieceMoves = sanMoves.count(isPieceMove)
        val forcingDensity =
          if sanMoves.nonEmpty then (captures + checks + promotions).toDouble / sanMoves.size.toDouble
          else 0.0
        val mechanismScores = Map(
          PrecedentMechanism.TacticalPressure ->
            (checks * 2 + captures + Option.when(forcingDensity >= 0.45)(1).getOrElse(0)),
          PrecedentMechanism.ExchangeCascade ->
            (captures * 2 + Option.when(captures >= 2)(2).getOrElse(0) + Option.when(pieceMoves >= 2)(1).getOrElse(0)),
          PrecedentMechanism.PromotionRace ->
            (promotions * 3 + Option.when(captures >= 1)(1).getOrElse(0) + Option.when(checks >= 1)(1).getOrElse(0)),
          PrecedentMechanism.StructuralTransformation ->
            (pawnPushes * 2 + Option.when(captures <= 1)(1).getOrElse(0) + Option.when(pieceMoves >= 1)(1).getOrElse(0)),
          PrecedentMechanism.InitiativeSwing ->
            (pieceMoves + Option.when(captures == 1)(1).getOrElse(0) + Option.when(checks == 0)(1).getOrElse(0))
        )
        val mechanism = mechanismScores.maxBy(_._2)._1
        val sortedScores = mechanismScores.values.toList.sorted(using Ordering[Int].reverse)
        val dominance = sortedScores match
          case top :: second :: _ => ((top - second).max(0).min(3)).toDouble / 3.0
          case top :: Nil         => (top.min(3)).toDouble / 3.0
          case _                  => 0.0
        val overlapConfidence = line.overlap match
          case n if n >= 3 => 1.0
          case 2           => 0.85
          case 1           => 0.65
          case _           => 0.35
        val metadataConfidence = (line.metadataScore.toDouble / 12.0).min(1.0)
        val sequenceConfidence =
          if sanMoves.size >= 6 then 1.0
          else if sanMoves.size >= 4 then 0.75
          else 0.55
        val confidence =
          (0.40 * overlapConfidence) +
            (0.25 * metadataConfidence) +
            (0.20 * sequenceConfidence) +
            (0.15 * dominance)
        PrecedentSignal(
          triggerMove = trigger,
          replyMove = reply,
          pivotMove = pivot,
          mechanism = mechanism,
          confidence = confidence.max(0.0).min(1.0)
        )
      }
    }

  private def openingPrecedentMoveTokens(pgn: Option[String]): Set[String] =
    val results = Set("1-0", "0-1", "1/2-1/2", "*")
    pgn.toList
      .flatMap(_.split("\\s+").toList)
      .map(normalizeMoveToken)
      .filter(token => token.nonEmpty && !results.contains(token))
      .toSet

  private def isCoreOpeningEvent(event: OpeningEvent): Boolean = event match
    case OpeningEvent.BranchPoint(_, _, _) => true
    case OpeningEvent.OutOfBook(_, _, _)   => true
    case OpeningEvent.TheoryEnds(_, _)     => true
    case OpeningEvent.Novelty(_, _, _, _)  => true
    case OpeningEvent.Intro(_, _, _, _)    => false

  private def formatOpeningPrecedentSnippet(game: ExplorerGame): Option[String] =
    val whiteName = normalizeExplorerPlayer(game.white.name)
    val blackName = normalizeExplorerPlayer(game.black.name)
    val year = Option.when(game.year > 0)(game.year)
    val sanSnippet = game.pgn.map(_.trim).filter(_.nonEmpty).map(shortOpeningPrecedentSan)
    val winnerInfo = game.winner.flatMap { color =>
      val winner = if color == chess.White then whiteName else blackName
      winner.map { winnerName =>
        val result = if color == chess.White then "1-0" else "0-1"
        (winnerName, result)
      }
    }

    for
      white <- whiteName
      black <- blackName
      y <- year
      line <- sanSnippet
      if line.nonEmpty
      (winnerName, result) <- winnerInfo
    yield
      val eventSuffix = game.event.map(_.trim).filter(_.nonEmpty).map(ev => s", $ev").getOrElse("")
      s"In $white-$black ($y$eventSuffix), after $line, $winnerName won ($result)."

  private def normalizeExplorerPlayer(name: String): Option[String] =
    Option(name)
      .map(_.trim)
      .filter(n => n.nonEmpty && n != "?")
      .map { n =>
        val parts = n.split(",").map(_.trim).filter(_.nonEmpty).toList
        parts match
          case last :: first :: Nil => s"$first $last"
          case _                    => n
      }

  private def shortOpeningPrecedentSan(line: String): String =
    val tokens = Option(line).getOrElse("").trim.split("\\s+").toList.filter(_.nonEmpty)
    val clipped = tokens.take(8).mkString(" ")
    if tokens.size > 8 then s"$clipped..." else clipped

  private def openingPrecedentSanMoves(line: String): List[String] =
    val resultTokens = Set("1-0", "0-1", "1/2-1/2", "*")
    Option(line).getOrElse("").trim.split("\\s+").toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.replaceAll("""^\d+\.(?:\.\.)?""", ""))
      .map(_.replaceAll("""^\.\.\.""", ""))
      .filter(token => token.nonEmpty && !resultTokens.contains(token))

  private def isLikelyPawnMove(move: String): Boolean =
    Option(move).getOrElse("").trim.matches("""^[a-h](?:x[a-h])?[1-8](?:=[QRBN])?[+#]?$""")

  private def isPieceMove(move: String): Boolean =
    Option(move).getOrElse("").headOption.exists(ch => "KQRBN".contains(ch))

  private def buildAlternativesBeat(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    bead: Int,
    crossBeatState: CrossBeatRepetitionState
  ): OutlineBeat =
    val played = ctx.playedSan.map(_.trim.toLowerCase)
    val deduped = ctx.candidates
      .drop(1)
      .foldLeft(List.empty[CandidateInfo]) { (acc, c) =>
        if acc.exists(_.move.trim.equalsIgnoreCase(c.move.trim)) then acc else acc :+ c
      }
    val alts = deduped.filterNot(c => played.contains(c.move.trim.toLowerCase)).take(2)
    if alts.isEmpty then return OutlineBeat(OutlineBeatKind.Alternatives, "")

    val ranked = rankedEngineVariations(ctx)
    val bestScore = ranked.headOption.map(_.effectiveScore)
    val bestSan = ranked.headOption
      .flatMap(_.ourMove.map(_.san))
      .orElse(
        ranked.headOption
          .flatMap(_.moves.headOption)
          .map(uci => NarrativeUtils.uciToSanOrFormat(ctx.fen, uci))
          .map(_.trim)
          .filter(_.nonEmpty)
      )
    val mainCandidate = ctx.candidates.headOption
    val signals = alts.map(c => alternativeEngineSignal(ctx, c, ranked, bestScore, bestSan))
    alts.foreach(c => rec.use(s"candidates[${c.move}]", c.move, "Alternative"))

    val attempted = (0 until 3).toList.map { pass =>
      val usedFamilies = scala.collection.mutable.HashSet.empty[String]
      val usedStems = scala.collection.mutable.HashSet.empty[String] ++ crossBeatState.usedStems
      val prefixCounts = scala.collection.mutable.HashMap.empty[String, Int].withDefaultValue(0)
      prefixCounts ++= crossBeatState.prefixCounts
      val usedHypothesisFamilies = scala.collection.mutable.HashSet.empty[String] ++ crossBeatState.usedHypothesisFamilies
      val usedHypothesisStems = scala.collection.mutable.HashSet.empty[String] ++ crossBeatState.usedHypothesisStems
      val passSeed = bead ^ (pass * 0x9e3779b9)
      val lines = alts.zipWithIndex.map { case (c, i) =>
        val localSeed = passSeed ^ ((i + 1) * 0x45d9f3b)
        val role = if i == 0 then "engine_primary" else "practical_secondary"
        val (line, family) = renderAlternativeDiversified(
          c = c,
          idx = i,
          bead = localSeed,
          usedFamilies = usedFamilies.toSet,
          signal = signals(i),
          usedStems = usedStems.toSet,
          prefixCounts = prefixCounts.toMap,
          role = role
        )
        usedFamilies += family
        val withDifference =
          appendAlternativeHypothesisDifference(
            baseLine = line,
            alternative = c,
            mainCandidate = mainCandidate,
            signal = signals(i),
            bead = localSeed ^ 0x6d2b79f5,
            usedStems = usedStems.toSet,
            prefixCounts = prefixCounts.toMap,
            usedHypothesisFamilies = usedHypothesisFamilies,
            usedHypothesisStems = usedHypothesisStems
          )
        trackTemplateUsage(withDifference, usedStems, prefixCounts)
        withDifference
      }
      (
        lines,
        alternativesRepetitionPenalty(lines),
        usedHypothesisFamilies.toSet,
        usedHypothesisStems.toSet
      )
    }
    val bestAttempt = attempted.minBy(_._2)
    val lines = bestAttempt._1
    crossBeatState.usedHypothesisFamilies ++= bestAttempt._3
    crossBeatState.usedHypothesisStems ++= bestAttempt._4
    lines.foreach(line => trackTemplateUsage(line, crossBeatState.usedStems, crossBeatState.prefixCounts))
    OutlineBeat(kind = OutlineBeatKind.Alternatives, text = lines.mkString("\n"), anchors = alts.map(_.move))

  private def buildWrapUpBeat(ctx: NarrativeContext, bead: Int): Option[OutlineBeat] =
    val parts = scala.collection.mutable.ListBuffer[String]()
    val cpWhite = rankedEngineVariations(ctx).headOption.map(_.scoreCp).orElse(ctx.engineEvidence.flatMap(_.best).map(_.scoreCp)).getOrElse(0)

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

    buildWrapUpHypothesisDifference(ctx, bead ^ 0x5f356495).foreach(parts += _)

    if parts.isEmpty then None
    else Some(OutlineBeat(kind = OutlineBeatKind.WrapUp, text = parts.mkString(" "), conceptIds = List("practical_assessment")))

  private def buildMainHypothesisNarrative(
    ctx: NarrativeContext,
    focusCandidate: Option[CandidateInfo],
    supportCandidate: Option[CandidateInfo],
    bead: Int,
    crossBeatState: CrossBeatRepetitionState
  ): Option[String] =
    val primary = selectHypothesis(focusCandidate, crossBeatState, bead ^ 0x24d8f59c)
      .orElse(selectHypothesis(supportCandidate, crossBeatState, bead ^ 0x3b5296f1))
    primary.map { first =>
      val secondary =
        selectSecondaryHypothesis(
          primary = first.card,
          from = List(focusCandidate, supportCandidate).flatten.distinct,
          state = crossBeatState,
          bead = bead ^ 0x6d2b79f5
        )

      val observation = buildHypothesisObservation(ctx, focusCandidate.orElse(supportCandidate), bead ^ 0x11f17f1d)
      val hypothesis = NarrativeLexicon.getHypothesisClause(
        bead = bead ^ Math.abs(first.sourceMove.hashCode),
        claim = first.card.claim,
        confidence = first.card.confidence,
        horizon = first.card.horizon,
        axis = first.card.axis
      )
      val validation = NarrativeLexicon.getHypothesisValidationClause(
        bead = bead ^ 0x517cc1b7,
        supportSignals = first.card.supportSignals,
        conflictSignals = first.card.conflictSignals,
        confidence = first.card.confidence
      )
      val longBridge =
        if first.card.horizon == HypothesisHorizon.Long && hasLongHorizonSupportSignal(first.card.supportSignals) then
          val bridgeCandidates =
            List(0, 1, 2, 3, 4, 5).map { idx =>
              NarrativeLexicon.getLongHorizonBridgeClause(
                bead = bead ^ (0x3124bcf5 + idx * 0x9e3779b9),
                move = first.sourceMove,
                axis = first.card.axis
              )
            }.distinct
          val usedHypothesisNarrativeStems = Set(
            normalizeStem(observation),
            normalizeStem(hypothesis),
            normalizeStem(validation)
          ).filter(_.nonEmpty)
          Some(
            selectNonRepeatingTemplate(
              templates = bridgeCandidates,
              seed = bead ^ 0x19f8b4ad,
              usedStems = crossBeatState.usedStems.toSet ++ usedHypothesisNarrativeStems,
              prefixCounts = crossBeatState.prefixCounts.toMap,
              prefixLimits = PrefixFamilyLimits
            )
          )
        else None
      val practical = NarrativeLexicon.getHypothesisPracticalClause(
        bead = bead ^ 0x4f6cdd1d,
        horizon = first.card.horizon,
        axis = first.card.axis,
        move = first.sourceMove
      )
      val supportBridge = secondary.map { extra =>
        NarrativeLexicon.getSupportingHypothesisClause(
          bead = bead ^ Math.abs(extra.sourceMove.hashCode) ^ 0x2f6e2b1,
          claim = extra.card.claim,
          confidence = extra.card.confidence,
          axis = extra.card.axis
        )
      }.getOrElse("")
      val text = List(observation, hypothesis, validation, longBridge.getOrElse(""), supportBridge, practical)
        .filter(_.trim.nonEmpty)
        .mkString(" ")
      trackHypothesisStemUsage(text, crossBeatState)
      text.trim
    }

  private def appendAlternativeHypothesisDifference(
    baseLine: String,
    alternative: CandidateInfo,
    mainCandidate: Option[CandidateInfo],
    signal: AlternativeEngineSignal,
    bead: Int,
    usedStems: Set[String],
    prefixCounts: Map[String, Int],
    usedHypothesisFamilies: scala.collection.mutable.Set[String],
    usedHypothesisStems: scala.collection.mutable.Set[String]
  ): String =
    val mainHyp = pickHypothesisForDifference(mainCandidate, usedHypothesisFamilies, bead ^ 0x7f4a7c15)
    val altHyp = pickHypothesisForDifference(Some(alternative), usedHypothesisFamilies, bead ^ 0x2a2a2a2a)
    val altClaim =
      altHyp
        .map(_.claim)
        .map(_.trim)
        .filter(_.nonEmpty)
        .filterNot { claim =>
          val stem = normalizeHypothesisStem(claim)
          stem.nonEmpty && usedHypothesisStems.contains(stem)
        }
    val difference = NarrativeLexicon.getAlternativeHypothesisDifference(
      bead = bead,
      alternativeMove = alternative.move,
      mainMove = mainCandidate.map(_.move).getOrElse(signal.bestSan.getOrElse("the principal move")),
      mainAxis = mainHyp.map(_.axis),
      alternativeAxis = altHyp.map(_.axis),
      alternativeClaim = altClaim,
      confidence = altHyp.map(_.confidence).getOrElse(0.42),
      horizon = altHyp.map(_.horizon).orElse(mainHyp.map(_.horizon)).getOrElse(HypothesisHorizon.Medium)
    )
    val rendered = List(baseLine.trim, difference.trim).filter(_.nonEmpty).mkString(" ").trim
    val normalized = normalizeAlternativeTemplateLine(rendered, bead ^ 0x63d5a6f1)
    val stem5 = normalizeHypothesisStem(normalized)
    if stem5.nonEmpty then usedHypothesisStems += stem5
    (mainHyp.toList ++ altHyp.toList).foreach { h => usedHypothesisFamilies += hypothesisFamily(h) }
    selectNonRepeatingTemplate(
      templates = List(normalized),
      seed = bead ^ 0x19f8b4ad,
      usedStems = usedStems ++ usedHypothesisStems.toSet,
      prefixCounts = prefixCounts,
      prefixLimits = PrefixFamilyLimits
    )

  private def buildWrapUpHypothesisDifference(ctx: NarrativeContext, bead: Int): Option[String] =
    val main = ctx.candidates.headOption
    val alt = ctx.candidates.drop(1).headOption
    val mainHyp = main.flatMap(_.hypotheses.sortBy(h => -h.confidence).headOption)
    val altHyp = alt.flatMap(_.hypotheses.sortBy(h => -h.confidence).headOption)
    for
      m <- main
      a <- alt
      mh <- mainHyp
      ah <- altHyp
    yield
      NarrativeLexicon.getWrapUpDecisiveDifference(
        bead = bead,
        mainMove = m.move,
        altMove = a.move,
        mainAxis = mh.axis,
        altAxis = ah.axis,
        mainHorizon = mh.horizon,
        altHorizon = ah.horizon
      )

  private def selectHypothesis(
    candidate: Option[CandidateInfo],
    state: CrossBeatRepetitionState,
    bead: Int
  ): Option[SelectedHypothesis] =
    candidate.flatMap { c =>
      val sorted = c.hypotheses.sortBy(h => -h.confidence)
      val picked = sorted.find { h =>
        val family = hypothesisFamily(h)
        !state.usedHypothesisFamilies.contains(family) && !state.usedHypothesisStems.contains(normalizeHypothesisStem(h.claim))
      }.orElse(sorted.headOption)
      picked.map { h =>
        state.usedHypothesisFamilies += hypothesisFamily(h)
        val stem = normalizeHypothesisStem(h.claim)
        if stem.nonEmpty then state.usedHypothesisStems += stem
        SelectedHypothesis(card = h, sourceMove = c.move)
      }
    }

  private def selectSecondaryHypothesis(
    primary: HypothesisCard,
    from: List[CandidateInfo],
    state: CrossBeatRepetitionState,
    bead: Int
  ): Option[SelectedHypothesis] =
    val pool =
      from.flatMap { c =>
        c.hypotheses.map(h => SelectedHypothesis(card = h, sourceMove = c.move))
      }
    val filtered =
      pool.filter { sh =>
        sh.card.axis != primary.axis &&
        sh.card.claim != primary.claim &&
        !state.usedHypothesisFamilies.contains(hypothesisFamily(sh.card)) &&
        !state.usedHypothesisStems.contains(normalizeHypothesisStem(sh.card.claim))
      }
    val picked = filtered.sortBy(sh => -sh.card.confidence).headOption
      .orElse(pool.find(sh => sh.card.axis != primary.axis && sh.card.claim != primary.claim))
    picked.foreach { sh =>
      state.usedHypothesisFamilies += hypothesisFamily(sh.card)
      val stem = normalizeHypothesisStem(sh.card.claim)
      if stem.nonEmpty then state.usedHypothesisStems += stem
    }
    picked

  private def pickHypothesisForDifference(
    candidate: Option[CandidateInfo],
    usedFamilies: scala.collection.mutable.Set[String],
    bead: Int
  ): Option[HypothesisCard] =
    candidate.flatMap { c =>
      val sorted = c.hypotheses.sortBy(h => -h.confidence)
      val picked = sorted.find(h => !usedFamilies.contains(hypothesisFamily(h))).orElse(sorted.headOption)
      picked.foreach(h => usedFamilies += hypothesisFamily(h))
      picked
    }

  private def buildHypothesisObservation(
    ctx: NarrativeContext,
    candidate: Option[CandidateInfo],
    bead: Int
  ): String =
    val rawObservation =
      candidate.flatMap(_.tacticalAlert.map(_.trim).filter(_.nonEmpty))
        .orElse(
          ctx.threats.toUs.headOption.map { t =>
            val sq = t.square.map(s => s" on $s").getOrElse("")
            s"${t.kind.toLowerCase} pressure$sq remains unresolved"
          }
        )
        .orElse(candidate.flatMap(_.whyNot.map(_.trim).filter(_.nonEmpty)))
        .orElse(
          candidate.map { c =>
            val move = c.move
            NarrativeLexicon.pick(bead ^ Math.abs(move.hashCode), List(
              s"$move reshapes the practical balance",
              s"$move redirects the strategic route",
              s"$move changes which plan family is easier to execute",
              s"$move alters the strategic map for both sides"
            ))
          }
        )
        .getOrElse("the position still has competing strategic routes")
    NarrativeLexicon.getHypothesisObservationClause(bead, rawObservation)

  private def trackHypothesisStemUsage(text: String, state: CrossBeatRepetitionState): Unit =
    val stem = normalizeHypothesisStem(text)
    if stem.nonEmpty then state.usedHypothesisStems += stem

  private def hasLongHorizonSupportSignal(signals: List[String]): Boolean =
    signals.exists(_.toLowerCase.contains("long-horizon"))

  private def normalizeHypothesisStem(text: String): String =
    Option(text).getOrElse("")
      .toLowerCase
      .replaceAll("""\*\*[^*]+\*\*""", " ")
      .replaceAll("""\([^)]*\)""", " ")
      .replaceAll("""\b\d+(?:\.\d+)?\b""", " ")
      .replaceAll("""[^a-z\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .split(" ")
      .filter(_.nonEmpty)
      .take(5)
      .mkString(" ")

  private def hypothesisFamily(card: HypothesisCard): String =
    s"${card.axis.toString.toLowerCase}:${normalizeHypothesisStem(card.claim)}"

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
          "This line is a tactical gamble; one loose move can backfire."
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
              s"After **$moveHint**, the game trends toward a controlled strategic struggle.",
              s"With **$moveHint**, planning depth tends to matter more than short tactics.",
              s"With **$moveHint**, the structure stays stable and plan choices become clearer."
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
      rankedEngineVariations(ctx).headOption.flatMap(_.mate).orElse(ctx.engineEvidence.flatMap(_.best.flatMap(_.mate))).exists(m => Math.abs(m) <= 1)

  private def formatCp(cp: Int): String =
    val sign = if cp >= 0 then "+" else ""
    val pawns = cp.toDouble / 100
    f"$sign$pawns%.1f"

  private def composeCausalAnnotation(
    rankContext: Option[String],
    reason: String,
    bestIntent: String,
    bead: Int,
    usedStems: Set[String],
    prefixCounts: Map[String, Int]
  ): String =
    val rank = rankContext.map(_.trim).filter(_.nonEmpty)
    val issue = Option(reason).map(_.trim).filter(_.nonEmpty)
    val better = Option(bestIntent).map(_.trim).filter(_.nonEmpty)

    val reasonBridge =
      Option.when(rank.nonEmpty && issue.nonEmpty) {
        selectNonRepeatingTemplate(
          templates = List(
            "From a practical perspective,",
            "In strategic terms,",
            "That makes the practical picture clear:",
            "So the practical verdict is straightforward:"
          ),
          seed = bead ^ 0x24d8f59c,
          usedStems = usedStems ++ rank.toSet.map(normalizeStem),
          prefixCounts = prefixCounts,
          prefixLimits = PrefixFamilyLimits
        )
      }.filter(_.nonEmpty)

    val betterBridge =
      Option.when(issue.nonEmpty && better.nonEmpty) {
        selectNonRepeatingTemplate(
          templates = List(
            "Therefore,",
            "As a result,",
            "So",
            "For that reason,"
          ),
          seed = bead ^ 0x3b5296f1,
          usedStems = usedStems ++ issue.toSet.map(normalizeStem),
          prefixCounts = prefixCounts,
          prefixLimits = PrefixFamilyLimits
        )
      }.filter(_.nonEmpty)

    val reasonClause =
      issue.map { r =>
        reasonBridge.map(b => s"$b $r").getOrElse(r)
      }
    val betterClause =
      better.map { b =>
        betterBridge.map(conn => s"$conn $b").getOrElse(b)
      }

    List(rank.getOrElse(""), reasonClause.getOrElse(""), betterClause.getOrElse(""))
      .filter(_.trim.nonEmpty)
      .mkString(" ")

  private def buildConcreteAnnotationIssue(
    bead: Int,
    playedSan: String,
    playedUci: Option[String],
    bestSan: String,
    bestUci: Option[String],
    cpLoss: Int,
    playedRank: Option[Int],
    missedMotif: Option[String],
    whyNot: Option[String],
    alert: Option[String],
    playedCand: Option[CandidateInfo],
    bestReply: Option[String],
    threatsToUs: List[ThreatRow],
    contextHint: Int
  ): String =
    val threatIssue = unresolvedThreatIssue(threatsToUs, playedSan, playedUci, bestSan, bestUci)
    val factIssue = playedCand.flatMap(c => extractFactConsequence(c.facts)).map(s => s"Issue: $s")
    val alertIssue = alert.map(a => s"Issue: ${a.stripSuffix(".")}.")
    val whyNotIssue = whyNot.flatMap(humanizeWhyNot).map(r => s"Issue: $r.")
    val motifIssue =
      Option.when(cpLoss >= Thresholds.INACCURACY_CP) {
        missedMotif.map(m => s"Issue: this bypasses the tactical idea of $m.")
      }.flatten
    val hasConcreteEvidence = List(threatIssue, factIssue, alertIssue, whyNotIssue, motifIssue).flatten.nonEmpty
    val replyIssue =
      Option.when(cpLoss >= Thresholds.INACCURACY_CP && hasConcreteEvidence) {
        bestReply.filter(isForcingReplySan).map(r => s"Issue: after this, ...$r gives the opponent a forcing reply.")
      }.flatten
    val rankIssue =
      playedRank match
        case Some(r) if r >= 3 =>
          Some(s"Issue: this is only the ${ordinal(r)} engine option. ${buildSeverityTail(bead ^ 0x3d12ab77, cpLoss, contextHint ^ r)}")
        case Some(2) if cpLoss >= Thresholds.INACCURACY_CP =>
          Some("Issue: this is second-tier compared with the engine's main continuation.")
        case None if cpLoss >= Thresholds.INACCURACY_CP =>
          Some("Issue: this move falls outside the sampled principal lines.")
        case _ => None
    val fallbackIssue = Option.when(cpLoss >= Thresholds.INACCURACY_CP)(s"Issue: ${defaultIssueBySeverity(bead, cpLoss)}.")
    val cause =
      List(threatIssue, factIssue, alertIssue, whyNotIssue, motifIssue, replyIssue, rankIssue, fallbackIssue)
        .flatten
        .find(_.trim.nonEmpty)
        .getOrElse("")
    val consequence = buildIssueConsequence(bead, cpLoss, bestReply, threatsToUs, playedCand)
    val linkedConsequence =
      consequence.map { c =>
        val bridge = selectNonRepeatingTemplate(
          templates = List(
            "Therefore,",
            "As a result,",
            "So",
            "For that reason,"
          ),
          seed = bead ^ contextHint ^ 0x5f356495,
          usedStems = Set(normalizeStem(cause)),
          prefixCounts = Map.empty,
          prefixLimits = PrefixFamilyLimits
        )
        if cause.trim.nonEmpty && bridge.nonEmpty then s"$bridge $c" else c
      }

    List(cause, linkedConsequence.getOrElse("")).filter(_.trim.nonEmpty).mkString(" ")

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

  private def normalizeMotifKey(raw: String): String =
    Option(raw).getOrElse("").trim
      .replaceAll("([a-z])([A-Z])", "$1_$2")
      .toLowerCase
      .replaceAll("[^a-z0-9]+", "_")
      .replaceAll("_+", "_")
      .stripPrefix("_")
      .stripSuffix("_")

  private def buildThemeKeywordSentence(
    motifs: List[String],
    existingText: String,
    bead: Int,
    ply: Int,
    phase: String
  ): Option[String] =
    val existingLow = Option(existingText).getOrElse("").toLowerCase
    val candidates =
      motifs
        .map(normalizeMotifKey)
        .filter(_.nonEmpty)
        .filter(m => motifPhaseCompatible(m, phase))
        .distinct
        .flatMap(themeReinforcementSentence)
        .sortBy { case (keyword, _) => -themeKeywordPriority(keyword) }
    candidates
      .flatMap { case (keyword, canonicalSentence) =>
        val variants = themeSentenceVariants(keyword, canonicalSentence)
        if existingLow.contains(keyword.toLowerCase) then None
        else
          pickThemeVariant(
            bead = bead ^ Math.abs(keyword.hashCode),
            ply = ply,
            variants = variants,
            existingLow = existingLow
          )
      }
      .headOption

  private def themeKeywordPriority(keyword: String): Int =
    keyword.toLowerCase match
      case "bad bishop"               => 100
      case "bishop pair"              => 100
      case "zwischenzug"              => 100
      case "repeat"                   => 100
      case "opposite-colored bishops" => 95
      case "minority attack"          => 95
      case "pawn storm"               => 95
      case "underpromotion"           => 95
      case "smothered mate"           => 95
      case _                          => 70

  private def themeReinforcementSentence(motif: String): Option[(String, String)] =
    val normalized = normalizeMotifKey(motif)
    if normalized.contains("iqp") || normalized.contains("isolated_pawn") then
      Some("isolated" -> "The isolated queen pawn structure is a key strategic reference point.")
    else if normalized.contains("minority_attack") then
      Some("minority attack" -> "A minority attack is becoming a practical queenside plan.")
    else if normalized.contains("bad_bishop") then
      Some("bad bishop" -> "The bad bishop remains a lasting positional burden.")
    else if normalized.contains("prophylaxis") then
      Some("prophylactic" -> "A prophylactic idea is central to limiting counterplay.")
    else if normalized.contains("interference") then
      Some("interference" -> "Interference is a live tactical resource in this position.")
    else if normalized.contains("deflection") then
      Some("deflection" -> "Deflection is a concrete tactical theme to calculate carefully.")
    else if normalized.contains("king_hunt") then
      Some("king hunt" -> "A king hunt can emerge quickly if king safety loosens.")
    else if normalized.contains("battery") then
      Some("battery" -> "A battery setup is building pressure on key lines.")
    else if normalized.contains("bishop_pair") then
      Some("bishop pair" -> "The bishop pair is a meaningful long-term asset here.")
    else if normalized.contains("opposite_bishops") || normalized.contains("opposite_color_bishops") then
      Some("opposite-colored bishops" -> "Opposite-colored bishops increase attacking chances for both sides.")
    else if normalized.contains("simplification") || normalized.contains("liquidate") then
      Some("simplification" -> "Simplification is a practical route to consolidate the position.")
    else if normalized.contains("smothered_mate") then
      Some("smothered mate" -> "Smothered mate patterns are part of the tactical background.")
    else if normalized.contains("novelty") then
      Some("novelty" -> "This position carries novelty value compared with the usual mainline treatment.")
    else if normalized.contains("rook_lift") then
      Some("rook lift" -> "A rook lift can become a key attacking mechanism.")
    else if normalized.contains("zwischenzug") then
      Some("zwischenzug" -> "A zwischenzug resource may interrupt the expected sequence.")
    else if normalized.contains("pawn_storm") then
      Some("pawn storm" -> "A pawn storm is the direct attacking plan around the king.")
    else if normalized.contains("trapped_piece") || normalized.contains("trapped") then
      Some("trapped" -> "A trapped piece motif is becoming relevant.")
    else if normalized.contains("stalemate") then
      Some("stalemate" -> "Stalemate resources are part of the endgame calculation.")
    else if normalized.contains("underpromotion") then
      Some("underpromotion" -> "An underpromotion resource is a concrete tactical possibility.")
    else if normalized.contains("repetition") || normalized.contains("repeat") then
      Some("repeat" -> "A repeat line is a practical decision point if risk rises.")
    else None

  private def themeSentenceVariants(keyword: String, canonical: String): List[String] =
    keyword.toLowerCase match
      case "minority attack" =>
        List(
          "A minority attack is becoming a practical queenside plan.",
          "Minority attack play on the queenside is becoming the most practical plan.",
          "The queenside minority attack is turning into the key practical route."
        )
      case "bad bishop" =>
        List(
          "The bad bishop remains a lasting positional burden.",
          "The bad bishop continues to limit long-term piece quality.",
          "A bad bishop handicap is still shaping the strategic plans."
        )
      case "repeat" =>
        List(
          "A repeat line is a practical decision point if risk rises.",
          "Repeat options are part of the practical decision tree when risk increases.",
          "A repeat remains a practical fallback if neither side can improve safely."
        )
      case "zwischenzug" =>
        List(
          "A zwischenzug resource may interrupt the expected sequence.",
          "A zwischenzug can reset the move order and change the tactical evaluation.",
          "The key tactical resource is a zwischenzug that disrupts automatic recapture."
        )
      case "bishop pair" =>
        List(
          "The bishop pair is a meaningful long-term asset here.",
          "The bishop pair provides a durable strategic edge over long diagonals.",
          "Long-term play favors the bishop pair as an enduring strategic asset."
        )
      case "opposite-colored bishops" =>
        List(
          "Opposite-colored bishops increase attacking chances for both sides.",
          "Opposite-colored bishops often amplify direct attacking chances.",
          "With opposite-colored bishops, both kings can come under sharper pressure."
        )
      case "smothered mate" =>
        List(
          "Smothered mate patterns are part of the tactical background.",
          "Smothered-mate geometry remains a live tactical motif.",
          "Knight-and-queen coordination keeps smothered-mate motifs relevant."
        )
      case "underpromotion" =>
        List(
          "An underpromotion resource is a concrete tactical possibility.",
          "Underpromotion remains a concrete tactical resource in this position.",
          "A non-queen promotion idea is part of the tactical calculation tree."
        )
      case "pawn storm" =>
        List(
          "A pawn storm is the direct attacking plan around the king.",
          "The direct attacking roadmap features a king-side pawn storm.",
          "Flank pawn-storm timing is central to the attack plan."
        )
      case _ =>
        List(canonical)

  private def pickThemeVariant(
    bead: Int,
    ply: Int,
    variants: List[String],
    existingLow: String
  ): Option[String] =
    val clean = variants.map(_.trim).filter(_.nonEmpty).distinct
    if clean.isEmpty then None
    else
      val seed = bead ^ (ply * 0x27d4eb2f)
      val baseIdx = {
        val mixed = scala.util.hashing.MurmurHash3.mixLast(0x3c6ef372, seed)
        val finalized = scala.util.hashing.MurmurHash3.finalizeHash(mixed, 1)
        Math.floorMod(finalized + ply, clean.size)
      }
      (0 until clean.size)
        .map(i => clean(Math.floorMod(baseIdx + i, clean.size)))
        .find(s => !existingLow.contains(s.toLowerCase))

  private def collectDerivedContextMotifs(ctx: NarrativeContext): List[String] =
    val semantic = ctx.semantic.toList
    val positional = semantic.flatMap(_.positionalFeatures.flatMap(positionalTagMotifs))
    val weaknesses = semantic.flatMap(_.structuralWeaknesses.flatMap(weakComplexMotifs))
    val endgame = semantic.flatMap(_.endgameFeatures.toList.flatMap(endgameMotifs))
    val evidence = ctx.candidates.flatMap(_.tacticEvidence.flatMap(tacticEvidenceMotifs))
    (positional ++ weaknesses ++ endgame ++ evidence)
      .map(_.trim)
      .filter(_.nonEmpty)
      .distinct

  private def endgameMotifs(info: EndgameInfo): List[String] =
    val motifs = scala.collection.mutable.ListBuffer[String]()
    if info.isZugzwang then motifs += "zugzwang"
    if info.hasOpposition then motifs += "opposition"
    motifs.toList

  private def positionalTagMotifs(tag: PositionalTagInfo): List[String] =
    val key = normalizeMotifKey(tag.tagType)
    val detail = normalizeMotifKey(tag.detail.getOrElse(""))
    val fileHint = tag.file.map(_.trim).filter(_.nonEmpty).getOrElse("")
    if key.contains("minority_attack") || key.contains("minorityattack") || detail.contains("minority_attack") then
      List("minority_attack")
    else if key.contains("pawn_majority") || key.contains("pawnmajority") then
      List("pawn_storm")
    else if key.contains("hanging_pawns") || key.contains("hangingpawns") then
      List("hanging_pawns")
    else if key.contains("bad_bishop") || key.contains("badbishop") then
      List("bad_bishop")
    else if key.contains("good_bishop") || key.contains("goodbishop") then
      List("good_bishop")
    else if key.contains("bishop_pair") || key.contains("bishoppair") then
      List("bishop_pair")
    else if key.contains("opposite_color_bishops") || key.contains("oppositecolorbishops") then
      List("opposite_bishops")
    else if key.contains("color_complex") || key.contains("colorcomplex") then
      List("color_complex")
    else if key.contains("semi_open_file_control") || key.contains("semiopenfilecontrol") then
      List(s"semi_open_file_control${if fileHint.nonEmpty then s"_$fileHint" else ""}")
    else if key.contains("rook_on_seventh") || key.contains("rookonseventh") || key.contains("seventh_rank_invasion") then
      List("rook_on_seventh")
    else if key.contains("rook_behind_passed_pawn") || key.contains("rookbehindpassedpawn") then
      List("rook_behind_passed_pawn")
    else if key.contains("king_cut_off") || key.contains("kingcutoff") then
      List("king_cut_off")
    else if key.contains("doubled_rooks") || key.contains("doubledrooks") then
      List("doubled_rooks")
    else if key.contains("connected_rooks") || key.contains("connectedrooks") then
      List("connected_rooks")
    else if key.contains("open_file") || key == "openfile" then
      List(s"open_file${if fileHint.nonEmpty then s"_$fileHint" else ""}")
    else if key.contains("outpost") then
      List("outpost")
    else Nil

  private def weakComplexMotifs(w: WeakComplexInfo): List[String] =
    val cause = normalizeMotifKey(w.cause)
    if cause.contains("hanging_pawns") then List("hanging_pawns")
    else Nil

  private def tacticEvidenceMotifs(raw: String): List[String] =
    val normalized = normalizeMotifKey(raw)
    val low = Option(raw).getOrElse("").toLowerCase
    if normalized.startsWith("maneuver") then List("maneuver")
    else if normalized.startsWith("domination") then List("domination")
    else if normalized.startsWith("trapped_piece") || normalized.startsWith("trappedpiece") then
      val pieceHint =
        if low.contains("queen") then List("trapped_piece_queen")
        else if low.contains("rook") then List("trapped_piece_rook")
        else Nil
      "trapped_piece" :: pieceHint
    else if normalized.startsWith("knight_vs_bishop") || normalized.startsWith("knightvsbishop") then List("knight_vs_bishop")
    else if normalized.startsWith("blockade") then List("blockade")
    else if normalized.startsWith("smothered_mate") || normalized.startsWith("smotheredmate") then List("smothered_mate")
    else if normalized.startsWith("pin") then
      if low.contains("queen") then List("pin_queen", "pin")
      else List("pin")
    else if normalized.startsWith("skewer") then
      if low.contains("queen") then List("skewer_queen", "skewer")
      else List("skewer")
    else if normalized.startsWith("xray") || normalized.startsWith("x_ray") then
      if low.contains("queen") then List("xray_queen", "xray")
      else List("xray")
    else if normalized.startsWith("battery") then List("battery")
    else if normalized.contains("exchange_sacrifice") || normalized.contains("exchangesacrifice") || normalized.contains("sacrifice_roi") then
      List("exchange_sacrifice")
    else Nil

  private def isTrustedMotifPrefixCandidate(
    rawMotif: String,
    deltaSignals: List[String],
    counterfactualSignals: List[String],
    conceptSummarySignals: List[String],
    derivedSignals: List[String],
    keyFact: Option[Fact],
    threatsToUs: List[ThreatRow],
    pawnPlay: PawnPlayTable,
    phase: String
  ): Boolean =
    val motif = normalizeMotifKey(rawMotif)
    if motif.isEmpty || !NarrativeLexicon.isMotifPrefixSignal(motif) || !motifPhaseCompatible(motif, phase) then false
    else
      val fromDelta = deltaSignals.exists(sig => motifSignalMatches(sig, motif))
      val fromCounterfactual = counterfactualSignals.exists(sig => motifSignalMatches(sig, motif))
      val fromConceptSummary = conceptSummarySignals.exists(sig => motifSignalMatches(sig, motif))
      val fromDerived = derivedSignals.exists(sig => motifSignalMatches(sig, motif))
      val corroboratedByBoard =
        keyFact.exists(f => motifCorroboratedByFact(motif, f)) ||
          motifCorroboratedByThreat(motif, threatsToUs) ||
          motifCorroboratedByPawnPlay(motif, pawnPlay)

      fromDelta || fromCounterfactual || fromDerived || (fromConceptSummary && corroboratedByBoard)

  private def motifPhaseCompatible(rawMotif: String, phase: String): Boolean =
    val motif = normalizeMotifKey(rawMotif)
    val p = Option(phase).getOrElse("").trim.toLowerCase
    if motif.isEmpty then false
    else if p.contains("endgame") then
      !List(
        "minority_attack",
        "pawn_storm",
        "greek_gift",
        "smothered_mate",
        "rook_lift",
        "novelty"
      ).exists(motif.contains)
    else if p.contains("opening") then
      !List(
        "zugzwang",
        "opposition",
        "king_cut_off",
        "rook_behind_passed_pawn"
      ).exists(motif.contains)
    else true

  private def motifSignalMatches(rawSignal: String, rawMotif: String): Boolean =
    val signal = normalizeMotifKey(rawSignal)
    val motif = normalizeMotifKey(rawMotif)
    if signal.isEmpty || motif.isEmpty then false
    else
      signal == motif ||
        signal.contains(motif) ||
        motif.contains(signal) ||
        signal.replace("_", "").contains(motif.replace("_", "")) ||
        motif.replace("_", "").contains(signal.replace("_", ""))

  private def motifCorroboratedByFact(motif: String, fact: Fact): Boolean =
    fact match
      case _: Fact.Pin =>
        motif.contains("pin") || motif.contains("xray")
      case _: Fact.Skewer =>
        motif.contains("skewer") || motif.contains("xray")
      case _: Fact.Fork =>
        motif.contains("fork") || motif.contains("deflection")
      case _: Fact.HangingPiece =>
        motif.contains("trapped_piece") || motif.contains("battery")
      case _: Fact.WeakSquare =>
        List("minority_attack", "color_complex", "bad_bishop", "good_bishop", "outpost").exists(motif.contains)
      case _: Fact.Outpost =>
        List("outpost", "knight_domination", "maneuver", "knight_vs_bishop").exists(motif.contains)
      case _: Fact.Opposition =>
        motif.contains("zugzwang") || motif.contains("king_cut_off")
      case _: Fact.KingActivity =>
        motif.contains("king_cut_off") || motif.contains("passed_pawn") || motif.contains("zugzwang")
      case _ => false

  private def motifCorroboratedByThreat(motif: String, threatsToUs: List[ThreatRow]): Boolean =
    val tacticalMotif =
      List(
        "king_hunt",
        "smothered",
        "greek_gift",
        "deflection",
        "interference",
        "pin",
        "skewer",
        "xray",
        "battery",
        "zwischenzug",
        "trapped_piece",
        "exchange_sacrifice"
      ).exists(motif.contains)
    val strategicMotif =
      List(
        "open_file",
        "rook_on_seventh",
        "rook_behind_passed_pawn",
        "king_cut_off",
        "passed_pawn",
        "pawn_storm",
        "minority_attack",
        "hanging_pawns",
        "isolated_pawn",
        "iqp",
        "color_complex",
        "simplification",
        "liquidate",
        "blockade"
      ).exists(motif.contains)

    threatsToUs.exists { t =>
      val kind = t.kind.toLowerCase
      val urgent = t.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP || kind.contains("mate")
      (tacticalMotif && (kind.contains("mate") || kind.contains("material") || urgent)) ||
      (strategicMotif && (kind.contains("positional") || urgent))
    }

  private def motifCorroboratedByPawnPlay(motif: String, pawnPlay: PawnPlayTable): Boolean =
    val hasPawnSignal =
      pawnPlay.breakReady ||
        pawnPlay.breakFile.nonEmpty ||
        pawnPlay.primaryDriver.toLowerCase.contains("break")
    hasPawnSignal &&
      List("pawn_break", "liquidate", "liquidation", "minority_attack", "passed_pawn", "hanging_pawns", "open_file")
        .exists(motif.contains)

  private def resolveCpLoss(
    ctx: NarrativeContext,
    playedUci: Option[String],
    playedSan: String,
    playedRank: Option[Int]
  ): Int =
    val fromCounterfactual = ctx.counterfactual.map(_.cpLoss).getOrElse(0)
    val fromEngine = estimateCpLossFromEngine(ctx, playedUci, playedSan)
    fromEngine
      .orElse {
        Option.when(fromCounterfactual > 0)(fromCounterfactual)
      }
      .orElse {
        playedRank match
          case Some(r) if r >= 3 => Some(30)
          case Some(2)           => Some(20)
          case _                 => None
      }
      .getOrElse(0)

  private def estimateCpLossFromEngine(
    ctx: NarrativeContext,
    playedUci: Option[String],
    playedSan: String
  ): Option[Int] =
    val ranked = rankedEngineVariations(ctx)
    for
      best <- ranked.headOption
      playedScore <- {
        val playedNormUci = playedUci.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)
        val playedNormSan = normalizeMoveToken(playedSan)
        ranked.collectFirst {
          case v if playedNormUci.exists(u => v.moves.headOption.exists(m => NarrativeUtils.uciEquivalent(m, u))) =>
            v.effectiveScore
          case v if playedNormSan.nonEmpty && v.ourMove.exists(m => normalizeMoveToken(m.san) == playedNormSan) =>
            v.effectiveScore
        }
      }
    yield cpLossForSideToMove(ctx.fen, best.effectiveScore, playedScore)

  private def playedMoveRank(
    ctx: NarrativeContext,
    playedUci: Option[String],
    playedSan: String
  ): Option[Int] =
    val playedNormUci = playedUci.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)
    val playedNormSan = normalizeMoveToken(playedSan)
    rankedEngineVariations(ctx).zipWithIndex.collectFirst {
      case (v, i) if playedNormUci.exists(u => v.moves.headOption.exists(m => NarrativeUtils.uciEquivalent(m, u))) =>
        i + 1
      case (v, i) if playedNormSan.nonEmpty && v.ourMove.exists(m => normalizeMoveToken(m.san) == playedNormSan) =>
        i + 1
    }

  private def rankedEngineVariations(ctx: NarrativeContext) =
    sortVariationsForSideToMove(ctx.fen, ctx.engineEvidence.toList.flatMap(_.variations))

  private def sortVariationsForSideToMove(fen: String, vars: List[VariationLine]): List[VariationLine] =
    if fenSideToMoveIsWhite(fen) then vars.sortBy(v => -v.effectiveScore)
    else vars.sortBy(_.effectiveScore)

  private def fenSideToMoveIsWhite(fen: String): Boolean =
    Option(fen).getOrElse("").trim.split("\\s+").drop(1).headOption.contains("w")

  private def cpLossForSideToMove(fen: String, bestScore: Int, playedScore: Int): Int =
    if fenSideToMoveIsWhite(fen) then (bestScore - playedScore).max(0)
    else (playedScore - bestScore).max(0)

  private def ordinal(n: Int): String =
    val suffix =
      if n % 100 >= 11 && n % 100 <= 13 then "th"
      else
        n % 10 match
          case 1 => "st"
          case 2 => "nd"
          case 3 => "rd"
          case _ => "th"
    s"$n$suffix"

  private def humanizeWhyNot(raw: String): Option[String] =
    val cleaned = Option(raw).getOrElse("").trim
      .replaceAll("""\(?[-+]?\d+(?:\.\d+)?\s*cp\)?""", "")
      .replaceAll("""\(\s*[-+]?\d+(?:\.\d+)?\s*\)""", "")
      .replaceAll("""\s{2,}""", " ")
      .replaceAll("""\s+\.""", ".")
      .stripSuffix(".")
      .replaceAll("(?i)\\binferior\\b", "less precise")
      .replaceAll("(?i)\\binaccuracy\\b", "practical concession")
      .replaceAll("(?i)\\bmistake\\b", "practical error")
      .trim
    Option.when(cleaned.nonEmpty)(cleaned)

  private def buildBoardAnchor(ctx: NarrativeContext, keyFact: Option[Fact], bead: Int): Option[BoardAnchor] =
    val urgentThreat =
      ctx.threats.toUs
        .sortBy(t => -t.lossIfIgnoredCp)
        .headOption
        .filter(t => t.kind.equalsIgnoreCase("Mate") || t.lossIfIgnoredCp >= 80)

    urgentThreat.map { t =>
      val kind = t.kind.trim.toLowerCase
      val square = t.square.map(_.trim).filter(_.nonEmpty).map(s => s" on $s").getOrElse("")
      val text = NarrativeLexicon.pick(bead ^ 0x4e67c6a7, List(
        s"The immediate concrete issue is the $kind threat$square.",
        s"On the board right now, handling the $kind threat$square is the priority.",
        s"The position currently hinges on the $kind threat$square."
      ))
      BoardAnchor(text = text, consumedThreat = true)
    }.orElse {
      keyFact.flatMap { fact =>
        val text = NarrativeLexicon.getFactStatement(bead ^ 0x3c79ac49, fact).trim
        Option.when(text.nonEmpty)(BoardAnchor(text = text, consumedFact = true))
      }
    }.orElse {
      ctx.pawnPlay.breakFile.map { br =>
        val file = br.trim
        val fileLabel = if file.toLowerCase.contains("file") then file else s"$file-file"
        val text = NarrativeLexicon.pick(bead ^ 0x1f123bb5, List(
          s"$fileLabel pressure is the concrete lever in the current position.",
          s"The structure around the $fileLabel break is now the practical focal point.",
          s"Plans on both sides revolve around the $fileLabel pawn break."
        ))
        BoardAnchor(text = text)
      }
    }

  private def buildIssueConsequence(
    bead: Int,
    cpLoss: Int,
    bestReply: Option[String],
    threatsToUs: List[ThreatRow],
    playedCand: Option[CandidateInfo]
  ): Option[String] =
    val threatConsequence =
      threatsToUs.find(_.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP).map { t =>
        val kind = t.kind.toLowerCase
        if kind.contains("mate") then "Consequence: king safety deteriorates immediately and the attack becomes forcing."
        else if kind.contains("material") then "Consequence: material pressure becomes harder to contain in practical play."
        else "Consequence: the opponent dictates the play while your pieces are tied to defense."
      }

    val factConsequence =
      playedCand.flatMap(_.facts.collectFirst {
        case Fact.WeakSquare(square, _, _, _) =>
          s"Consequence: ${square.key} can become a long-term target."
        case Fact.HangingPiece(square, role, _, _, _) =>
          s"Consequence: the ${roleLabel(role)} on ${square.key} can become a direct tactical target."
      })

    val replyConsequence =
      Option.when(cpLoss >= Thresholds.INACCURACY_CP) {
        bestReply.filter(isForcingReplySan).map(r => s"Consequence: the opponent can answer with ...$r and seize the initiative.")
      }.flatten

    val severityConsequence =
      Option.when(cpLoss >= Thresholds.INACCURACY_CP) {
        Thresholds.classifySeverity(cpLoss) match
          case "blunder" =>
            NarrativeLexicon.pick(bead ^ 0x7f4a7c15, List(
              "Consequence: tactical control flips immediately and conversion becomes straightforward.",
              "Consequence: king safety and coordination collapse at once.",
              "Consequence: the opponent gets a forcing route with little counterplay."
            ))
          case "mistake" =>
            NarrativeLexicon.pick(bead ^ 0x7f4a7c15, List(
              "Consequence: the opponent improves with forcing moves while your position stays passive.",
              "Consequence: structure or king safety is compromised without compensation.",
              "Consequence: practical control shifts and defense becomes uncomfortable."
            ))
          case "inaccuracy" =>
            NarrativeLexicon.pick(bead ^ 0x7f4a7c15, List(
              "Consequence: the opponent gets the easier plan and more comfortable piece play.",
              "Consequence: piece coordination loosens and counterplay appears.",
              "Consequence: you lose structural clarity and give up practical initiative."
            ))
          case _ => ""
      }.filter(_.nonEmpty)

    factConsequence
      .orElse(threatConsequence)
      .orElse(severityConsequence)
      .orElse(replyConsequence)

  private def isForcingReplySan(reply: String): Boolean =
    val r = Option(reply).getOrElse("").trim
    r.nonEmpty && (r.contains("+") || r.contains("#") || r.contains("x"))

  private def harmonizeAnnotationTone(text: String, cpLoss: Int, isBest: Boolean, contextHint: Int): String =
    if text.trim.isEmpty then text
    else if isBest || cpLoss <= 35 then softenNearBestTone(text)
    else if cpLoss >= Thresholds.INACCURACY_CP && !containsNegativeTone(text) then
      s"${text.trim} ${buildSeverityTail(Math.abs(text.hashCode) ^ 0x239b961b, cpLoss, contextHint)}"
    else text

  private def softenNearBestTone(text: String): String =
    List(
      ("(?i)\\bblunder\\b", "detour"),
      ("(?i)\\bmistake\\b", "detour"),
      ("(?i)\\binaccuracy\\b", "detour"),
      ("(?i)\\bimprecise\\b", "less direct"),
      ("(?i)\\binferior\\b", "less direct"),
      ("(?i)\\berror\\b", "detour"),
      ("(?i)\\bdrops\\b", "concedes"),
      ("(?i)\\bloses\\b", "concedes"),
      ("(?i)\\bslip\\b", "tempo loss"),
      ("(?i)\\bmisses\\b", "bypasses")
    ).foldLeft(text) { case (acc, (pattern, replacement)) =>
      acc.replaceAll(pattern, replacement)
    }

  private def containsNegativeTone(text: String): Boolean =
    val low = text.toLowerCase
    List("blunder", "mistake", "inaccuracy", "imprecise", "misses", "slip", "inferior", "drops", "loses", "error")
      .exists(low.contains)

  private def enforceAnnotationPolarity(
    text: String,
    cpLoss: Int,
    isBest: Boolean,
    contextHint: Int
  ): String =
    if text.trim.isEmpty then text
    else
      val nearBest = isBest || cpLoss <= 25
      val severeError = cpLoss >= 140
      val containsBenchmarkStrongPositive = containsBenchmarkStrongPositiveLexicon(text)

      val neutralized =
        if nearBest then neutralizeBenchmarkNegativeLexicon(text)
        else text

      val softenedPositive =
        if severeError && containsBenchmarkStrongPositive then
          neutralizeBenchmarkStrongPositiveLexicon(neutralized)
        else neutralized

      if severeError && !containsBenchmarkNegativeLexicon(softenedPositive) then
        s"${softenedPositive.trim} ${buildSeverityTail(Math.abs(softenedPositive.hashCode) ^ 0x6f4b1321, cpLoss, contextHint)}"
      else softenedPositive

  private def buildSeverityTail(bead: Int, cpLoss: Int, contextHint: Int): String =
    val seed = bead ^ contextHint ^ Math.abs(cpLoss) ^ 0x5bd1e995
    val templates =
      Thresholds.classifySeverity(cpLoss) match
        case "blunder" =>
          List(
            "This is a blunder, so forcing control shifts to the opponent.",
            "Because this blunder loosens coordination, the opponent gets a direct conversion route.",
            "This loses tactical control; as a result, recovery becomes difficult.",
            "This blunder concedes initiative, therefore the defensive workload spikes immediately.",
            "This gives the opponent a forcing path, while your counterplay resources shrink."
          )
        case "mistake" =>
          List(
            "This is a clear mistake, so practical control swings away quickly.",
            "This mistake yields an easier conversion plan, because your coordination is slower.",
            "This concedes initiative, and as a result your defensive options narrow.",
            "This gives the opponent the cleaner continuation, while your plan becomes reactive.",
            "This mistake leaves you defending without counterplay, therefore every tempo matters."
          )
        case _ =>
          List(
            "This is an inaccuracy, so the opponent's play becomes easier to handle.",
            "This gives up practical initiative, because the move-order becomes less precise.",
            "This drifts from the best plan; as a result, defensive workload increases.",
            "This leaves the opponent with a smoother sequence, while your structure is harder to coordinate.",
            "This practical detour hands over simpler choices, therefore practical pressure rises."
          )
    selectNonRepeatingTemplate(
      templates = templates,
      seed = seed,
      usedStems = Set(normalizeStem("This is a mistake that gives the opponent easier play.")),
      prefixCounts = Map.empty,
      prefixLimits = PrefixFamilyLimits
    )

  private def containsBenchmarkNegativeLexicon(text: String): Boolean =
    val low = text.toLowerCase
    List("blunder", "mistake", "inaccuracy", "misses", "slip", "inferior", "drops", "loses")
      .exists(term => s" $low ".contains(s" $term "))

  private def containsBenchmarkStrongPositiveLexicon(text: String): Boolean =
    val low = text.toLowerCase
    List("best move", "excellent choice", "strong move", "very accurate", "precise move", "fully sound")
      .exists(low.contains)

  private def neutralizeBenchmarkNegativeLexicon(text: String): String =
    List(
      ("(?i)\\bblunder\\b", "detour"),
      ("(?i)\\bmistake\\b", "detour"),
      ("(?i)\\binaccuracy\\b", "detour"),
      ("(?i)\\bmisses\\b", "bypasses"),
      ("(?i)\\bslip\\b", "tempo loss"),
      ("(?i)\\binferior\\b", "less direct"),
      ("(?i)\\bdrops\\b", "concedes"),
      ("(?i)\\bloses\\b", "concedes")
    ).foldLeft(text) { case (acc, (pattern, replacement)) =>
      acc.replaceAll(pattern, replacement)
    }

  private def neutralizeBenchmarkStrongPositiveLexicon(text: String): String =
    List(
      ("(?i)\\bbest move\\b", "reference move"),
      ("(?i)\\bexcellent choice\\b", "practical option"),
      ("(?i)\\bstrong move\\b", "practical move"),
      ("(?i)\\bvery accurate\\b", "playable"),
      ("(?i)\\bprecise move\\b", "reference move"),
      ("(?i)\\bfully sound\\b", "playable")
    ).foldLeft(text) { case (acc, (pattern, replacement)) =>
      acc.replaceAll(pattern, replacement)
    }

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
          "this allows a forcing tactical sequence against your king or material",
          "this collapses coordination and gives the opponent a direct conversion route",
          "this fails to meet the immediate tactical threat and the position unravels"
        ))
      case "mistake" =>
        NarrativeLexicon.pick(bead, List(
          "this hands over the initiative and creates long-term defensive burdens",
          "this concedes either structure or king safety without enough return",
          "this lets the opponent improve with simple, forcing moves"
        ))
      case "inaccuracy" =>
        NarrativeLexicon.pick(bead, List(
          "this gives the opponent the easier plan to execute",
          "this loosens piece coordination and invites counterplay",
          "this drifts from the cleanest structure-preserving continuation"
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

  private def normalizeStem(text: String): String =
    Option(text).getOrElse("")
      .toLowerCase
      .replaceAll("""\*\*[^*]+\*\*""", " ")
      .replaceAll("""\([^)]*\)""", " ")
      .replaceAll("""\b\d+(?:\.\d+)?\b""", " ")
      .replaceAll("""[^a-z\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .split(" ")
      .filter(_.nonEmpty)
      .take(7)
      .mkString(" ")

  private def prefixFamilyOf(text: String): Option[String] =
    val low = Option(text).getOrElse("").trim.toLowerCase
    if low.isEmpty then None
    else if
      low.startsWith("line route") ||
      low.startsWith("the branch follows") ||
      low.startsWith("the move path") ||
      low.contains("route is")
    then Some("sequence")
    else if
      low.startsWith("strategic shift") ||
      low.startsWith("strategically") ||
      low.startsWith("the practical turning factor") ||
      low.startsWith("the recurring practical theme") ||
      low.startsWith("shared lesson")
    then Some("strategic_shift")
    else if
      low.startsWith("engine") ||
      low.contains(" engine ") ||
      low.contains("multi pv") ||
      low.contains("principal engine")
    then Some("engine")
    else None

  private def selectNonRepeatingTemplate(
    templates: List[String],
    seed: Int,
    usedStems: Set[String],
    prefixCounts: Map[String, Int],
    prefixLimits: Map[String, Int]
  ): String =
    val clean = templates.map(_.trim).filter(_.nonEmpty).distinct
    if clean.isEmpty then ""
    else
      val start = Math.floorMod(seed, clean.size)
      val rotated = (0 until clean.size).toList.map(i => clean(Math.floorMod(start + i, clean.size)))
      def withinPrefixLimit(t: String): Boolean =
        prefixFamilyOf(t).forall { family =>
          prefixCounts.getOrElse(family, 0) < prefixLimits.getOrElse(family, Int.MaxValue)
        }
      rotated
        .find(t => !usedStems.contains(normalizeStem(t)) && withinPrefixLimit(t))
        .orElse(rotated.find(withinPrefixLimit))
        .getOrElse(rotated.head)

  private def trackTemplateUsage(
    template: String,
    usedStems: scala.collection.mutable.Set[String],
    prefixCounts: scala.collection.mutable.Map[String, Int]
  ): Unit =
    val stem = normalizeStem(template)
    if stem.nonEmpty then usedStems += stem
    prefixFamilyOf(template).foreach { family =>
      prefixCounts.update(family, prefixCounts.getOrElse(family, 0) + 1)
    }

  private def alternativeEngineSignal(
    ctx: NarrativeContext,
    candidate: CandidateInfo,
    ranked: List[VariationLine],
    bestScore: Option[Int],
    bestSan: Option[String]
  ): AlternativeEngineSignal =
    val matched = ranked.zipWithIndex.collectFirst {
      case (v, idx) if variationMatchesCandidate(candidate, v) => (idx + 1, v.effectiveScore)
    }
    val rank = matched.map(_._1)
    val cpLoss = for
      best <- bestScore
      (_, score) <- matched
    yield cpLossForSideToMove(ctx.fen, best, score)
    AlternativeEngineSignal(rank = rank, cpLoss = cpLoss, bestSan = bestSan)

  private def variationMatchesCandidate(candidate: CandidateInfo, variation: VariationLine): Boolean =
    val candUci = candidate.uci.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)
    val candSan = normalizeMoveToken(candidate.move)
    candUci.exists(u => variation.moves.headOption.exists(m => NarrativeUtils.uciEquivalent(m, u))) ||
      (candSan.nonEmpty && variation.ourMove.exists(m => normalizeMoveToken(m.san) == candSan))

  private def formatCpGap(cpLoss: Int): String =
    f"${cpLoss.toDouble / 100}%.1f pawns"

  private def appendAlternativeEngineContrast(
    line: String,
    signal: AlternativeEngineSignal,
    bead: Int,
    usedStems: Set[String],
    prefixCounts: Map[String, Int],
    role: String
  ): String =
    val contrastSeed =
      bead ^
        (signal.rank.getOrElse(0) * 0x45d9f3b) ^
        (signal.cpLoss.getOrElse(0) * 131) ^
        (Math.abs(signal.bestSan.getOrElse("").hashCode) * 17)

    val shouldAttachContrast =
      signal.rank.exists {
        case r if r >= 3 => true
        case 2           => signal.cpLoss.exists(_ >= 25) || Math.floorMod(contrastSeed, 3) != 0
        case _           => false
      }
    val preferPracticalWording = role.equalsIgnoreCase("practical_secondary")

    val contrastTemplates: List[String] =
      if !shouldAttachContrast then Nil
      else signal.rank match
        case Some(2) =>
          val bestRef = signal.bestSan.map(s => s"**$s**").getOrElse("the top engine move")
          signal.cpLoss match
            case Some(loss) if loss <= 20 =>
              if preferPracticalWording then
                List(
                  s"$bestRef still holds the cleaner route, but the practical gap is narrow.",
                  s"This remains playable over the board, with $bestRef keeping only a small edge.",
                  s"Compared with $bestRef, the difference is modest in practical play."
                )
              else
                List(
                  s"Engine order keeps $bestRef first, though this remains close in practical terms.",
                  s"$bestRef still tops the engine list, but the gap here is narrow.",
                  s"The engine shows a slight preference for $bestRef, while this stays near-equivalent over the board.",
                  s"$bestRef remains the clean reference continuation, with only a small edge."
                )
            case Some(loss) =>
              if preferPracticalWording then
                List(
                  s"Compared with $bestRef, this concedes about ${formatCpGap(loss)} in practical terms.",
                  s"The practical cost versus $bestRef is roughly ${formatCpGap(loss)}.",
                  s"Against $bestRef, the score gap is about ${formatCpGap(loss)}."
                )
              else
                List(
                  s"The engine still points to $bestRef as cleaner, by about ${formatCpGap(loss)}.",
                  s"In engine terms, $bestRef holds roughly a ${formatCpGap(loss)} edge.",
                  s"The practical gap to $bestRef is around ${formatCpGap(loss)}.",
                  s"Engine preference remains with $bestRef, with roughly a ${formatCpGap(loss)} edge in practical terms.",
                  s"Compared with $bestRef, engine evaluation drops by roughly ${formatCpGap(loss)}."
                )
            case None =>
              if preferPracticalWording then
                List(
                  s"$bestRef remains the cleaner benchmark continuation in this structure.",
                  s"Practical handling is usually easier from $bestRef.",
                  s"$bestRef is still the stable benchmark line."
                )
              else
                List(
                  s"Engine order still favors $bestRef as the cleaner continuation.",
                  s"$bestRef remains the engine reference in this structure.",
                  s"The principal engine route still starts with $bestRef.",
                  s"In the sampled lines, $bestRef remains the benchmark move."
                )
        case Some(r) if r >= 3 =>
          val bestRef = signal.bestSan.map(s => s"**$s**").getOrElse("the principal continuation")
          signal.cpLoss match
            case Some(loss) if loss <= 70 =>
              if preferPracticalWording then
                List(
                  s"As a ${ordinal(r)} practical-tier choice, this trails $bestRef by about ${formatCpGap(loss)}.",
                  s"As a ${ordinal(r)} option, this line trails $bestRef by around ${formatCpGap(loss)}.",
                  s"The ${ordinal(r)} choice is workable, but $bestRef still leads by roughly ${formatCpGap(loss)}."
                )
              else
                List(
                  s"Engine ranking places this around ${ordinal(r)}, with $bestRef ahead by about ${formatCpGap(loss)}.",
                  s"This continuation stays in the ${ordinal(r)} engine group, while $bestRef keeps roughly a ${formatCpGap(loss)} edge.",
                  s"In engine order, $bestRef remains first and this ${ordinal(r)} choice trails by around ${formatCpGap(loss)}."
                )
            case Some(loss) =>
              if preferPracticalWording then
                List(
                  s"As a lower-tier option (around ${ordinal(r)}), this trails $bestRef by roughly ${formatCpGap(loss)}.",
                  s"The score gap to $bestRef is substantial here (about ${formatCpGap(loss)}).",
                  s"This ${ordinal(r)} line leaves a large practical deficit versus $bestRef (about ${formatCpGap(loss)})."
                )
              else
                List(
                  s"This sits in a lower engine tier (about ${ordinal(r)}), and $bestRef leads by roughly ${formatCpGap(loss)}.",
                  s"Engine ranking is clear here: around ${ordinal(r)} for this line, while $bestRef is ahead by ${formatCpGap(loss)}.",
                  s"In engine terms this continuation drops to about ${ordinal(r)}, with $bestRef up by ${formatCpGap(loss)}.",
                  s"Around $bestRef, the principal engine route stays cleaner; this ${ordinal(r)} option is behind by about ${formatCpGap(loss)}."
                )
            case None =>
              if preferPracticalWording then
                List(
                  s"This sits in a lower practical tier (around ${ordinal(r)}), while $bestRef remains the benchmark.",
                  s"$bestRef stays the stable reference line, with this branch in a lower tier.",
                  s"As a ${ordinal(r)} option, this is less reliable than $bestRef."
                )
              else
                List(
                  s"This is a lower-ranked engine option (around ${ordinal(r)}), while $bestRef remains the stable benchmark.",
                  s"Engine ordering puts this below the principal choices; $bestRef is the cleaner reference line.",
                  s"The sampled engine set keeps this in a lower tier, with $bestRef as the anchor line."
                )
        case _ => Nil

    val base = line.trim
    val contrast =
      if contrastTemplates.isEmpty then None
      else
        Some(
          selectNonRepeatingTemplate(
            templates = contrastTemplates,
            seed = contrastSeed ^ 0x11f17f1d,
            usedStems = usedStems,
            prefixCounts = prefixCounts,
            prefixLimits = PrefixFamilyLimits
          )
        )
    contrast match
      case Some(extra) if base.nonEmpty => normalizeAlternativeTemplateLine(s"$base $extra", bead)
      case _                            => normalizeAlternativeTemplateLine(base, bead)

  private def appendStrategicImplication(
    line: String,
    move: String,
    role: String,
    plan: String,
    diffLabel: String,
    signal: AlternativeEngineSignal,
    bead: Int,
    usedStems: Set[String],
    prefixCounts: Map[String, Int]
  ): String =
    val cleanedPlan = plan.replaceAll("""[_\-]+""", " ").trim
    val genericPlans = Set("unknown", "development", "positional maneuvering", "quiet move", "general play")
    val planHint =
      if cleanedPlan.nonEmpty && !genericPlans.contains(cleanedPlan) then s" around $cleanedPlan"
      else ""
    val informativeDiff =
      diffLabel.nonEmpty &&
      diffLabel != "unknown" &&
      List("complex", "sharp", "precise", "narrow", "forcing", "tactical", "risky", "volatile", "critical")
        .exists(diffLabel.contains)
    val practicalHint =
      if informativeDiff then s" The practical burden is $diffLabel." else ""
    val templates =
      if role.equalsIgnoreCase("engine_primary") then
        List(
          s"Strategically, **$move** works best when follow-up tempi keep coordination and king safety aligned$planHint.",
          s"The practical upside of **$move** is smoother piece flow$planHint, but only if sequencing after **$move** stays accurate.",
          s"With **$move**, conversion quality depends on whether activity stays coordinated as exchanges begin$planHint.",
          s"If **$move** is handled accurately, structure and initiative reinforce each other$planHint instead of drifting apart.",
          s"**$move** remains robust when defensive coverage and active plans stay synchronized through the next phase$planHint."
        )
      else
        List(
          s"In practical terms, **$move** is judged by conversion ease$planHint, because defensive coordination can diverge quickly.$practicalHint",
          s"After **$move**, king safety and tempo stay linked, so one inaccurate sequence can hand over initiative$planHint.$practicalHint",
          s"With **$move**, a move-order slip can expose coordination gaps$planHint, and recovery windows are short.$practicalHint",
          s"After **$move**, sequence accuracy matters because structure and activity can separate quickly$planHint.$practicalHint",
          s"Strategically, **$move** needs connected follow-up through the next phase$planHint, or practical control leaks away.$practicalHint"
        )
    val implication = selectNonRepeatingTemplate(
      templates = templates.map(_.trim).filter(_.nonEmpty).distinct,
      seed = bead ^ (signal.rank.getOrElse(0) * 0x7f4a7c15) ^ Math.abs(move.hashCode),
      usedStems = usedStems,
      prefixCounts = prefixCounts,
      prefixLimits = PrefixFamilyLimits
    )
    if line.trim.isEmpty then implication.trim
    else s"${line.trim} ${implication.trim}".trim

  private def renderAlternativeDiversified(
    c: CandidateInfo,
    idx: Int,
    bead: Int,
    usedFamilies: Set[String],
    signal: AlternativeEngineSignal,
    usedStems: Set[String],
    prefixCounts: Map[String, Int],
    role: String
  ): (String, String) =
    val rawReason = c.whyNot.flatMap(humanizeWhyNot).map(_.trim).filter(_.nonEmpty).map(_.stripSuffix("."))
    val move = c.move.trim
    val plan = c.planAlignment.trim.toLowerCase
    val diff = c.practicalDifficulty.trim.toLowerCase
    val diffLabel = diff.replaceAll("""[_\-]+""", " ").trim
    val informativePracticalHint =
      diffLabel.nonEmpty &&
      diffLabel != "unknown" &&
      List("complex", "sharp", "precise", "narrow", "forcing", "tactical", "risky", "volatile", "critical")
        .exists(diffLabel.contains)
    val practicalHint =
      if informativePracticalHint then
        s" Practical burden: $diffLabel."
      else ""

    val preferredFamilies: List[String] =
      if rawReason.nonEmpty then List("tradeoff", "practical", "strategic", "generic")
      else if c.tags.contains(CandidateTag.Sharp) || c.tags.contains(CandidateTag.TacticalGamble) || diff.contains("complex") then
        List("dynamic", "strategic", "practical", "generic")
      else if c.tags.contains(CandidateTag.Solid) || c.tags.contains(CandidateTag.Converting) || diff.contains("clean") then
        List("technical", "strategic", "practical", "generic")
      else
        List("strategic", "practical", "dynamic", "technical", "generic")

    val family = preferredFamilies.find(f => !usedFamilies.contains(f)).getOrElse(preferredFamilies.headOption.getOrElse("generic"))
    val localBead = bead ^ Math.abs(move.hashCode) ^ (idx + 1) * 0x9e3779b9
    val reason = rawReason.map(r => diversifyAlternativeReason(r, localBead))

    val baseTemplates =
      family match
        case "tradeoff" =>
          val r = reason.getOrElse("it concedes dynamic chances")
          List(
            s"**$move** is playable, but $r.",
            s"**$move** can work, although $r.",
            s"From a practical angle, **$move** is viable, yet $r.",
            s"With **$move**, the tradeoff is concrete: $r.",
            s"**$move** stays in range, though $r.",
            s"**$move** is serviceable over the board, but $r."
          )
        case "dynamic" =>
          List(
            s"**$move** keeps the game dynamic and can lead to sharper play.",
            s"**$move** invites complications and active piece play.",
            s"With **$move**, the position stays tense and tactical.",
            s"**$move** keeps tactical pressure alive and asks for concrete calculation."
          )
        case "technical" =>
          List(
            s"**$move** is the cleaner technical route, aiming for a stable structure.",
            s"**$move** heads for a controlled position with fewer tactical swings.",
            s"**$move** favors structural clarity and methodical handling over complications.",
            s"**$move** is a clean technical route that lightens defensive duties.",
            s"**$move** aims for a technically manageable position with clear conversion paths.",
            s"**$move** keeps the game in a technical channel where precise handling is rewarded."
          )
        case "strategic" =>
          val planHint =
            if plan.nonEmpty && plan != "unknown" then
              val cleaned = plan.replaceAll("""[_\-]+""", " ").trim
              s" around $cleaned"
            else ""
          List(
            s"**$move** is a strategic alternative$planHint.",
            s"**$move** points to a different strategic plan$planHint.",
            s"**$move** takes the game into another strategic channel$planHint.",
            s"**$move** keeps a coherent strategic direction$planHint.",
            s"Strategically, **$move** is a viable reroute$planHint."
          )
        case "practical" =>
          List(
            s"In practical play, **$move** is viable, and the next two moves must stay accurate.",
            s"**$move** is viable over the board, though move-order precision matters.",
            s"**$move** is playable in practice, but concrete calculation is required.",
            s"Over the board, **$move** is acceptable if tactical details are controlled.",
            s"**$move** remains practical, but one inaccurate follow-up can change the assessment.$practicalHint",
            s"**$move** is playable in a real game, yet it demands precise sequencing.$practicalHint"
          )
        case _ =>
          List(NarrativeLexicon.getAlternative(localBead, move, c.whyNot.flatMap(humanizeWhyNot)))

    val line =
      selectNonRepeatingTemplate(
        templates = baseTemplates.map(_.trim).filter(_.nonEmpty).distinct,
        seed = localBead ^ 0x3124bcf5,
        usedStems = usedStems,
        prefixCounts = prefixCounts,
        prefixLimits = PrefixFamilyLimits
      )

    val contrasted =
      appendAlternativeEngineContrast(
        line = line,
        signal = signal,
        bead = localBead ^ 0x63d5a6f1,
        usedStems = usedStems + normalizeStem(line),
        prefixCounts = prefixCounts,
        role = role
      )
    val withImplication =
      appendStrategicImplication(
        line = contrasted,
        move = move,
        role = role,
        plan = plan,
        diffLabel = diffLabel,
        signal = signal,
        bead = localBead ^ 0x6d2b79f5,
        usedStems = usedStems + normalizeStem(contrasted),
        prefixCounts = prefixCounts
      )

    (withImplication, family)

  private def alternativesRepetitionPenalty(lines: List[String]): Int =
    val stems = lines.map(normalizeAlternativeStem).filter(_.nonEmpty)
    val stemPenalty =
      stems.groupBy(identity).valuesIterator.map(c => (c.size - 1).max(0) * 15).sum

    val ngramPenalty = lines.map(normalizeAlternativeTokens).map { tokens =>
      val tri = ngramRepeatPenalty(tokens, 3, threshold = 2, weight = 6)
      val four = ngramRepeatPenalty(tokens, 4, threshold = 2, weight = 10)
      tri + four
    }.sum

    stemPenalty + ngramPenalty

  private def normalizeAlternativeStem(line: String): String =
    normalizeStem(line)

  private def normalizeAlternativeTokens(line: String): List[String] =
    line.toLowerCase
      .replaceAll("""\*\*[^*]+\*\*""", " ")
      .replaceAll("""\([^)]*\)""", " ")
      .replaceAll("""\b\d+(?:\.\d+)?\b""", " ")
      .replaceAll("""[^a-z\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .split(" ")
      .toList
      .filter(_.nonEmpty)

  private def ngramRepeatPenalty(tokens: List[String], n: Int, threshold: Int, weight: Int): Int =
    if tokens.lengthCompare(n) < 0 then 0
    else
      tokens
        .sliding(n)
        .map(_.mkString(" "))
        .toList
        .groupBy(identity)
        .valuesIterator
        .map(_.size)
        .map(c => (c - threshold + 1).max(0) * weight)
        .sum

  private def normalizeAlternativeTemplateLine(line: String, bead: Int): String =
    val compact = line.replaceAll("""\s+""", " ").trim
    if compact.isEmpty then compact
    else
      val fixedPractical = compact
        .replace(
          "is a workable practical choice, but it leaves little room for imprecision.",
          NarrativeLexicon.pick(bead ^ 0x2a2a2a2a, List(
            "remains practical, but move-order precision is critical.",
            "is playable, though concrete follow-up accuracy is mandatory."
          ))
        )
        .replace(
          "keeps the game on its most coherent technical track.",
          NarrativeLexicon.pick(bead ^ 0x4b4b4b4b, List(
            "keeps the continuation strategically clean and practical.",
            "preserves structural clarity while keeping options flexible.",
            "stays on a stable plan that is easier to execute.",
            "maintains practical control without forcing complications."
          ))
        )
      fixedPractical

  private def diversifyAlternativeReason(reason: String, bead: Int): String =
    val cleaned = reason.trim.stripSuffix(".")
    val slightConcession = """(?i)^slight practical concession after\s+(.+)$""".r
    val decisiveLoss = """(?i)^decisive loss after\s+(.+)$""".r
    val significantDisadvantage = """(?i)^significant disadvantage after\s+(.+)$""".r

    cleaned match
      case slightConcession(rest) =>
        val localSeed = bead ^ 0x1a2b3c4d ^ Math.abs(rest.hashCode)
        NarrativeLexicon.pick(localSeed, List(
          s"it yields a modest practical concession once $rest appears",
          s"after $rest, practical handling is less demanding for the other side",
          s"once $rest appears, defending choices narrow and plan execution becomes harder",
          s"it grants a cleaner practical route to the opponent after $rest"
        ))
      case decisiveLoss(rest) =>
        val localSeed = bead ^ 0x2b3c4d5e ^ Math.abs(rest.hashCode)
        NarrativeLexicon.pick(localSeed, List(
          s"it runs into a decisive sequence after $rest",
          s"the line becomes losing after $rest",
          s"it allows a forcing collapse after $rest"
        ))
      case significantDisadvantage(rest) =>
        val localSeed = bead ^ 0x3c4d5e6f ^ Math.abs(rest.hashCode)
        NarrativeLexicon.pick(localSeed, List(
          s"it yields a notable disadvantage after $rest",
          s"the position worsens materially after $rest",
          s"it concedes a significant practical deficit after $rest"
        ))
      case _ => cleaned
