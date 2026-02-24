package lila.llm.analysis

import chess.{ Color, Square }
import lila.llm.LlmConfig
import lila.llm.model._
import lila.llm.model.strategic._
import lila.llm.analysis.L3._
import lila.llm.model.structure.AlignmentBand

/**
 * NarrativeContext Builder
 * 
 * Converts existing analysis results into hierarchical NarrativeContext.
 */
object NarrativeContextBuilder:
  private val llmConfig = LlmConfig.fromEnv

  /**
   * Build NarrativeContext from ExtendedAnalysisData and IntegratedContext.
   */
  def build(
    data: ExtendedAnalysisData,
    ctx: IntegratedContext,
    prevAnalysis: Option[ExtendedAnalysisData] = None,
    probeResults: List[ProbeResult] = Nil,
    openingRef: Option[OpeningReference] = None,
    prevOpeningRef: Option[OpeningReference] = None,  // For BranchPoint/TheoryEnds detection
    openingBudget: OpeningEventBudget = OpeningEventBudget(),  // Passed from game-level tracker
    afterAnalysis: Option[ExtendedAnalysisData] = None
  ): NarrativeContext = {
    // Keep "root" probes (same base fen, or missing fen for backward compatibility) separate
    // so they don't pollute candidate lists and meta signals.
    val rootProbeResults = probeResults.filter(pr => pr.fen.forall(_ == data.fen))

    // A7/B7: Candidates built early to extract top move SAN/UCI for threat linkage
    val baseCandidates = buildCandidates(data)
    val color = if (data.isWhiteToMove) Color.White else Color.Black
    val board = chess.format.Fen.read(chess.variant.Standard, chess.format.Fen.Full(data.fen)).map(_.board).getOrElse(chess.Board.empty)

    val topSan = baseCandidates.headOption.map(_.move)
    val topUci = baseCandidates.headOption.flatMap(_.uci)

    val header = buildHeader(ctx)
    val summary = buildSummary(data, ctx)
    val threats = buildThreatTable(ctx, topSan, topUci, data.fen)
    val pawnPlay = buildPawnPlayTable(ctx)
    val plans = buildPlanTable(data)
    val l1 = buildL1Snapshot(ctx)
    val phase0 = buildPhaseContext(ctx, prevAnalysis)
    val afterPhaseTrigger =
      afterAnalysis
        .filter(_ => data.prevMove.isDefined)
        .flatMap(after =>
          Option.when(after.phase != data.phase)(
            s"Transition from ${data.phase.capitalize} to ${after.phase.capitalize}"
          )
        )

    val phase =
      afterPhaseTrigger match
        case Some(t) => phase0.copy(transitionTrigger = Some(t))
        case None    => phase0

    val afterDelta =
      afterAnalysis
        .filter(_ => data.prevMove.isDefined)
        .map(after => buildDelta(data, after))

    val prevDelta = prevAnalysis.map(prev => buildDelta(prev, data))
    val delta = afterDelta.orElse(prevDelta)
    val alignmentForTone = if llmConfig.structKbEnabled then ctx.planAlignment else None
    val enrichedCandidates = buildCandidatesEnriched(data, rootProbeResults, alignmentForTone)

    val playedSan = data.prevMove.flatMap { uci =>
      NarrativeUtils
        .uciListToSan(data.fen, List(uci))
        .headOption
        .orElse(Some(NarrativeUtils.formatUciAsSan(uci)))
    }
    val authorQuestions =
      AuthorQuestionGenerator.generate(
        data = data,
        ctx = ctx,
        candidates = enrichedCandidates,
        playedSan = playedSan
      )
    val targets = buildTargets(data, ctx, rootProbeResults)
    val planScoring = PlanScoringResult(
      topPlans = data.plans,
      confidence = 0.0, // fallback
      phase = ctx.phase
    )
    val multiPv = data.alternatives.map(v => PvLine(v.moves, v.scoreCp, v.mate, v.depth))
    val probeRequests =
      ProbeDetector
        .detect(
          ctx = ctx,
          planScoring = planScoring,
          multiPv = multiPv,
          fen = data.fen,
          playedMove = data.prevMove,
          candidates = enrichedCandidates,
          authorQuestions = authorQuestions
        )
        .distinctBy(_.id)
    
    // B-axis: Meta signals (Step 1-3)
    // Only populate meta if we have meaningful source data
    val meta = buildMetaSignals(data, ctx, targets, rootProbeResults)

    val strategicFlow = buildStrategicFlow(data)

    // Phase A: Semantic section from ExtendedAnalysisData
    val semantic = buildSemanticSection(data)

    // Phase B: Opponent plan (side=!toMove)
    val opponentPlan = buildOpponentPlan(data, ctx)


    // Phase F: Decision Rationale (Synthesis)
    val decision = if (header.choiceType != "StyleChoice") {
      Some(calculateDecisionRationale(data, ctx, enrichedCandidates, semantic, targets, rootProbeResults))
    } else None

    // Phase A9: Opening Event Detection (event-driven, not per-move)
    val openingEvent = detectOpeningEvent(data, openingRef, decision, openingBudget, prevOpeningRef)
    val candidates =
      attachHypothesisCards(
        data = data,
        ctx = ctx,
        candidates = enrichedCandidates,
        probeResults = rootProbeResults,
        probeRequests = probeRequests,
        openingEvent = openingEvent
      )
    
    // Compute updated budget based on fired event
    val updatedBudget = openingEvent match {
      case Some(OpeningEvent.Intro(_, _, _, _)) => openingBudget.afterIntro
      case Some(OpeningEvent.TheoryEnds(_, _)) => openingBudget.afterTheoryEnds
      case Some(_) => openingBudget.afterEvent  // BranchPoint, OutOfBook, Novelty
      case None => openingBudget.updatePly(data.ply)
    }

    val bestEngineMove = data.alternatives.headOption.flatMap(_.moves.headOption)

    val authorEvidence =
      AuthorEvidenceBuilder.build(
        fen = data.fen,
        ply = data.ply,
        playedMove = data.prevMove,
        bestMove = bestEngineMove.orElse(candidates.headOption.flatMap(_.uci)),
        authorQuestions = authorQuestions,
        probeResults = probeResults
      )

    val motifFactsRaw = FactExtractor.fromMotifs(board, data.motifs, FactScope.Now)
    val motifFacts =
      if data.endgameFeatures.isDefined then
        motifFactsRaw.filter {
          case _: Fact.Opposition => false
          case _: Fact.Zugzwang   => false
          case _                  => true
        }
      else motifFactsRaw
    val staticFacts = FactExtractor.extractStaticFacts(board, color)
    val endgameFacts = FactExtractor.extractEndgameFacts(board, color, data.endgameFeatures)

    NarrativeContext(
      fen = data.fen,
      header = header,
      ply = data.ply,
      playedMove = data.prevMove,
      playedSan = playedSan,
      counterfactual = data.counterfactual,
      summary = summary,
      threats = threats,
      pawnPlay = pawnPlay,
      plans = plans,
      planContinuity = data.planContinuity,
      snapshots = List(l1),
      delta = delta,
      phase = phase,
      candidates = candidates,
      authorQuestions = authorQuestions,
      authorEvidence = authorEvidence,
      facts = motifFacts ++ staticFacts ++ endgameFacts,
      probeRequests = probeRequests,
      meta = meta,
      strategicFlow = strategicFlow,
      semantic = semantic,
      opponentPlan = opponentPlan,
      decision = decision,
      openingEvent = openingEvent,
      openingData = openingRef,
      updatedBudget = updatedBudget,
      engineEvidence = Some(lila.llm.model.strategic.EngineEvidence(
        depth = data.alternatives.map(_.depth).maxOption.getOrElse(0),
        variations = data.alternatives
      )),
      deltaAfterMove = afterDelta.isDefined
    )
  }

  /**
   * A9 Event Detection: Detect opening-related events for narrative.
   * Events are budget-controlled and only fire on specific triggers.
   * 
   * Budget must be passed from game-level tracker to prevent repeated firing.
   */
  private def detectOpeningEvent(
      data: ExtendedAnalysisData,
      openingRef: Option[OpeningReference],
      decision: Option[DecisionRationale],
      budget: OpeningEventBudget,
      prevRef: Option[OpeningReference]
  ): Option[OpeningEvent] = {
    // Guard: Only detect events in opening phase
    if (!data.phase.equalsIgnoreCase("opening")) return None
    
    // Extract cpLoss for novelty detection
    // counterfactual=None means this IS the best move (cpLoss=0)
    // counterfactual=Some(cf) => use cf.cpLoss
    val cpLoss = data.counterfactual match {
      case Some(cf) => Some(cf.cpLoss)
      case None => Some(0)  // No counterfactual = this is best move = cpLoss=0
    }
    
    // Check for constructive evidence
    val hasConstructiveEvidence = 
      decision.exists(d => d.delta.newOpportunities.nonEmpty || d.delta.planAdvancements.nonEmpty) ||
      data.plans.exists(_.score > 0.6) ||
      data.motifs.exists(m => m.isInstanceOf[Motif.Outpost] || m.isInstanceOf[Motif.Centralization])
    
    OpeningEventDetector.detect(
      ply = data.ply,
      playedMove = data.prevMove,
      fen = data.fen,  // For UCI→SAN conversion
      ref = openingRef,
      budget = budget,
      cpLoss = cpLoss,
      hasConstructiveEvidence = hasConstructiveEvidence,
      prevRef = prevRef
    )
  }
  
  private def buildHeader(ctx: IntegratedContext): ContextHeader = {
    val classification = ctx.classification.getOrElse(defaultClassification)
    
    ContextHeader(
      phase = classification.gamePhase.phaseType.toString,
      criticality = classification.criticality.criticalityType match {
        case CriticalityType.Normal         => "Normal"
        case CriticalityType.CriticalMoment => "Critical"
        case CriticalityType.ForcedSequence => "Forced"
      },
      choiceType = classification.choiceTopology.topologyType.toString,
      riskLevel = classification.riskProfile.riskLevel.toString,
      taskMode = classification.taskMode.taskMode.toString
    )
  }
  
  private def buildSummary(
    data: ExtendedAnalysisData,
    ctx: IntegratedContext
  ): NarrativeSummary = {
    val alignmentHint = data.planAlignment.map(summaryHintFromAlignment).getOrElse("")
    val primaryPlan = data.plans.headOption
      .map(p => s"${p.plan.name} (${f"${p.score}%.2f"})$alignmentHint")
      .getOrElse(s"No clear plan$alignmentHint")
    val keyThreat = ctx.threatsToUs.flatMap { ta =>
      ta.threats
        .find(t => t.attackSquares.headOption.flatMap(str => chess.Square.fromKey(str)).exists(sq => NarrativeUtils.isVerifiedThreat(data.fen, sq, if (!data.isWhiteToMove) chess.Color.White else chess.Color.Black)))
        .map { t =>
          val urgency = 
            if (t.lossIfIgnoredCp >= 800 || t.kind == ThreatKind.Mate) "URGENT"
            else if (t.lossIfIgnoredCp >= 300) "IMPORTANT"
            else "LOW"
          val attacker = t.motifs.headOption.map(_.takeWhile(_ != '(')).getOrElse("attacker")
          s"$urgency: $attacker on ${t.attackSquares.head}"
        }
    }
    
    val choiceType = ctx.classification
      .map(c => c.choiceTopology.topologyType.toString)
      .getOrElse("Unknown")
    
    val hasMateThreatToUs = ctx.threatsToUs.exists(_.threats.exists(_.kind == ThreatKind.Mate))
    val hasMateThreatToThem = ctx.threatsToThem.exists(_.threats.exists(_.kind == ThreatKind.Mate))
    val hasMateCandidate = data.candidates.exists { cand =>
      cand.line.mate.isDefined || NarrativeUtils.uciListToSan(data.fen, cand.line.moves.take(1)).exists(_.contains("#"))
    }
    val hasForcedMateForUs = data.alternatives.exists(_.mate.exists(_ > 0)) || hasMateCandidate
    val isMate = data.alternatives.exists(_.mate.isDefined) || hasMateThreatToUs || hasMateThreatToThem || hasMateCandidate
    
    val tensionPolicy = if (isMate) "Maximum (shattered)" else ctx.pawnAnalysis
      .map(p => s"${p.tensionPolicy} (${p.primaryDriver})")
      .getOrElse("N/A")
    
    val evalDelta = data.practicalAssessment
      .map { pa => 
        val score = pa.engineScore / 100.0
        val sign = if (score > 0) "+" else ""
        f"Eval: $sign$score%.1f"
      }
      .getOrElse("No eval")
    val (finalPrimaryPlan, finalKeyThreat) = 
      if (hasForcedMateForUs) {
        // We have a forced mate - override everything
        val mateDepth = data.alternatives.flatMap(_.mate).filter(_ > 0).headOption
          .orElse(data.candidates.flatMap(_.line.mate).filter(_ > 0).headOption)
        val mateText = mateDepth.map(m => s"Forced Mate in $m").getOrElse("Forced Mate")
        (mateText, Some(s"URGENT: $mateText available"))
      } else if (hasMateThreatToUs) {
        // Opponent threatens mate - upgrade keyThreat severity
        (primaryPlan, Some("URGENT: Mate threat against us"))
      } else {
        (primaryPlan, keyThreat)
      }
    
    NarrativeSummary(
      primaryPlan = finalPrimaryPlan,
      keyThreat = finalKeyThreat,
      choiceType = choiceType,
      tensionPolicy = tensionPolicy,
      evalDelta = evalDelta
    )
  }
  
  private def buildThreatTable(ctx: IntegratedContext, topSan: Option[String], topUci: Option[String], fen: String): ThreatTable = {
    // TO US threats: bestDefense is valid (how we can defend against opponent's threat)
    val toUs = ctx.threatsToUs.map(ta => buildThreatRowsToUs(ta, topSan, topUci, ctx.isWhiteToMove, fen)).getOrElse(Nil)
    // TO THEM threats: bestDefense should be None (we don't know their defensive moves)
    val toThem = ctx.threatsToThem.map(ta => buildThreatRowsToThem(ta)).getOrElse(Nil)
    ThreatTable(toUs = toUs, toThem = toThem)
  }
  
  // For threats TO US: bestDefense represents how we can defend
  private def buildThreatRowsToUs(ta: ThreatAnalysis, topSan: Option[String], topUci: Option[String], isWhiteToMove: Boolean, fen: String): List[ThreatRow] = {
    val victimColor: Color = if (isWhiteToMove) Color.White else Color.Black
    val uniqueThreats = ta.threats
      .filter(_.lossIfIgnoredCp >= 80) // Noise gate
      .filter { t =>
        val attackSq = t.attackSquares.headOption.flatMap(k => Square.fromKey(k))
        attackSq.exists(sq => NarrativeUtils.isVerifiedThreat(fen, sq, victimColor))
      }
      .distinctBy(t => (t.kind, t.attackSquares.headOption, t.bestDefense))
      .take(3)
    
    uniqueThreats.map { t =>
      val isTopDefense = (topUci.isDefined && t.bestDefense == topUci) || 
                         (topSan.exists(s => t.bestDefense.exists(_.startsWith(s))))

      ThreatRow(
        kind = t.kind.toString,
        side = "US",
        square = t.attackSquares.headOption,
        lossIfIgnoredCp = t.lossIfIgnoredCp,
        turnsToImpact = t.turnsToImpact,
        bestDefense = t.bestDefense, // Valid: how we defend against this threat
        defenseCount = t.defenseCount,
        insufficientData = ta.insufficientData,
        isTopCandidateDefense = isTopDefense
      )
    }
  }
  
  // For threats TO THEM: we don't know their defensive moves, so bestDefense = None
  private def buildThreatRowsToThem(ta: ThreatAnalysis): List[ThreatRow] = {
    val uniqueThreats = ta.threats
      .filter(_.lossIfIgnoredCp >= 80) // Noise gate: skip insignificant threats
      .distinctBy(t => (t.kind, t.attackSquares.headOption))
      .take(3)
    
    uniqueThreats.map { t =>
      ThreatRow(
        kind = t.kind.toString,
        side = "THEM",
        square = t.attackSquares.headOption,
        lossIfIgnoredCp = t.lossIfIgnoredCp,
        turnsToImpact = t.turnsToImpact,
        bestDefense = None, // Semantic fix: we don't know their defensive options
        defenseCount = 0,   // Unknown
        insufficientData = ta.insufficientData
      )
    }
  }
  
  private def buildPawnPlayTable(ctx: IntegratedContext): PawnPlayTable = {
    ctx.pawnAnalysis match {
      case Some(pa) =>
        PawnPlayTable(
          breakReady = pa.pawnBreakReady,
          breakFile = pa.breakFile,
          breakImpact = if (pa.breakImpact >= 200) "High" else if (pa.breakImpact >= 100) "Medium" else "Low",
          tensionPolicy = pa.tensionPolicy.toString,
          tensionReason = deriveTensionReason(pa, ctx),
          passedPawnUrgency = pa.passedPawnUrgency.toString,
          passerBlockade = pa.blockadeSquare.map(sq => 
            s"${sq.key}${pa.blockadeRole.map(r => s" by ${r.name}").getOrElse("")}"
          ),
          counterBreak = pa.counterBreak,
          primaryDriver = pa.primaryDriver
        )
      case None =>
        PawnPlayTable(
          breakReady = false,
          breakFile = None,
          breakImpact = "Low",
          tensionPolicy = "Ignore",
          tensionReason = "No pawn analysis available",
          passedPawnUrgency = "Background",
          passerBlockade = None,
          counterBreak = false,
          primaryDriver = "quiet"
        )
    }
  }
  
  private def deriveTensionReason(pa: PawnPlayAnalysis, ctx: IntegratedContext): String = {
    pa.tensionPolicy match {
      case TensionPolicy.Maintain =>
        if (pa.pawnBreakReady) "preparing break"
        else if (ctx.tacticalThreatToThem) "attack preparation"
        else "flexibility needed"
      case TensionPolicy.Release =>
        if (pa.advanceOrCapture) "must resolve"
        else if (pa.passedPawnUrgency == PassedPawnUrgency.Critical) "passer advance"
        else "favorable exchange"
      case TensionPolicy.Ignore =>
        "no significant tension"
    }
  }
  
  private def buildPlanTable(
    data: ExtendedAnalysisData
  ): PlanTable = {
    val hasMateCandidate = data.candidates.exists { cand =>
      cand.line.mate.isDefined || NarrativeUtils.uciListToSan(data.fen, cand.line.moves.take(1)).exists(_.contains("#"))
    }
    val hasForcedMateForUs = data.alternatives.exists(_.mate.exists(_ > 0)) || hasMateCandidate
    
    val establishedKey =
      data.planContinuity
        .filter(_.consecutivePlies >= 2)
        .map(c => c.planId.map(_.toLowerCase).getOrElse(c.planName.toLowerCase))

    val basePlans = data.plans.take(5).zipWithIndex.map { case (pm, idx) =>
      val isEstablished =
        establishedKey.exists(k => k == pm.plan.id.toString.toLowerCase || k == pm.plan.name.toLowerCase)
      PlanRow(
        rank = idx + 1,
        name = pm.plan.name,
        score = pm.score,
        evidence = pm.evidence.take(2).map(_.description),
        supports = pm.supports,
        blockers = pm.blockers,
        missingPrereqs = pm.missingPrereqs,
        isEstablished = isEstablished
      )
    }
    val top5 = if (hasForcedMateForUs) {
      val mateDepth = data.alternatives.flatMap(_.mate).filter(_ > 0).headOption
        .orElse(data.candidates.flatMap(_.line.mate).filter(_ > 0).headOption)
      val matePlan = PlanRow(
        rank = 1,
        name = mateDepth.map(m => s"Forced Mate in $m").getOrElse("Forced Mate"),
        score = 1.0,
        evidence = List("Checkmate is available"),
        confidence = ConfidenceLevel.Engine
      )
      // Re-rank existing plans
      val shifted = basePlans.take(4).map(p => p.copy(rank = p.rank + 1))
      matePlan :: shifted
    } else {
      basePlans
    }
    val suppressed = List.empty[SuppressedPlan] // Compatibility events removed with PlanSequence migration
    
    PlanTable(top5 = top5, suppressed = suppressed)
  }

  private def buildStrategicFlow(data: ExtendedAnalysisData): Option[String] = {
    val side = if (data.isWhiteToMove) "White" else "Black"
    val anchorPlan =
      data.planSequence
        .flatMap(_.primaryPlanName)
        .orElse(data.plans.headOption.map(_.plan.name))
        .orElse(data.planContinuity.map(_.planName))
    val continuitySnippet =
      data.planContinuity
        .filter(_.consecutivePlies >= 2)
        .map(c => s" This idea has held for ${c.consecutivePlies} plies.")
        .getOrElse("")

    data.planSequence.map { seq =>
      val planLabel = anchorPlan.getOrElse("the current plan")
      val alignmentClause = data.planAlignment.flatMap(flowHintFromAlignment).map(h => s" $h").getOrElse("")
      val base = seq.transitionType match {
        case TransitionType.Continuation =>
          s"$side is clearly continuing with $planLabel."
        case TransitionType.NaturalShift =>
          s"$side makes a natural strategic shift toward $planLabel."
        case TransitionType.ForcedPivot =>
          s"$side is forced to pivot into $planLabel under tactical pressure."
        case TransitionType.Opportunistic =>
          s"$side switches opportunistically to $planLabel after a concrete chance appeared."
        case TransitionType.Opening =>
          s"$side starts a fresh strategic thread with $planLabel."
      }
      s"$base$alignmentClause$continuitySnippet".trim
    }.orElse {
      data.planContinuity.flatMap { continuity =>
        Option.when(continuity.consecutivePlies >= 2)(s"$side continues ${continuity.planName}.")
      }
    }
  }
  
  private def buildL1Snapshot(ctx: IntegratedContext): L1Snapshot = {
    ctx.features match {
      case Some(f) =>
        val isWhite = ctx.isWhiteToMove
        val mp = f.materialPhase
        val ks = f.kingSafety
        val act = f.activity
        val imb = f.imbalance
        
        // Material: "+2" | "=" | "-1" from White POV, then adjust for side
        val matDiff = mp.materialDiff // White POV
        val sideMaterial = if (isWhite) matDiff else -matDiff
        val materialStr = if (sideMaterial > 0) s"+$sideMaterial" 
                          else if (sideMaterial < 0) s"$sideMaterial"
                          else "="
        
        // Imbalance detection
        val imbalanceStr = if (imb.whiteBishopPair && !imb.blackBishopPair) Some("Bishop pair (us)")
                           else if (imb.blackBishopPair && !imb.whiteBishopPair) Some("Bishop pair (them)")
                           else if (imb.whiteRooks != imb.blackRooks) Some(s"Rook imbalance (${imb.whiteRooks}v${imb.blackRooks})")
                           else None
        
        // King safety (us/them based on isWhiteToMove)
        val (ourAttackers, ourEscapes) = if (isWhite) 
          (ks.whiteAttackersCount, ks.whiteEscapeSquares) 
        else 
          (ks.blackAttackersCount, ks.blackEscapeSquares)
        val (theirAttackers, theirEscapes) = if (isWhite)
          (ks.blackAttackersCount, ks.blackEscapeSquares)
        else
          (ks.whiteAttackersCount, ks.whiteEscapeSquares)
        
        val kingSafetyUs = if (ourAttackers >= 2 || ourEscapes <= 1)
          Some(s"Exposed ($ourAttackers attackers, $ourEscapes escapes)")
        else None
        
        val kingSafetyThem = if (theirAttackers >= 2 || theirEscapes <= 1)
          Some(s"Exposed ($theirAttackers attackers, $theirEscapes escapes)")
        else None
        
        // Mobility advantage
        val mobDiff = if (isWhite) act.whitePseudoMobility - act.blackPseudoMobility
                      else act.blackPseudoMobility - act.whitePseudoMobility
        val mobilityStr = if (mobDiff > 10) Some(s"Advantage (+$mobDiff)")
                          else if (mobDiff < -10) Some(s"Disadvantage ($mobDiff)")
                          else None
        
        // Center control
        val cs = f.centralSpace
        val ccDiff = if (isWhite) cs.whiteCenterControl - cs.blackCenterControl
                     else cs.blackCenterControl - cs.whiteCenterControl
        val centerStr = if (ccDiff >= 2) Some("Dominant")
                        else if (ccDiff <= -2) Some("Weak")
                        else None
        
        // Open files - compute actual file letters from FEN
        val openFilesList = computeOpenFiles(f.fen)
        val isEndgame = ctx.classification.exists(_.gamePhase.isEndgame)
        val isPawnEndgame = (mp.whiteMaterial + mp.blackMaterial) <= 6 && isEndgame
        
        val filteredCenter = if (isPawnEndgame) None else centerStr
        val filteredOpenFiles = if (isPawnEndgame) Nil else openFilesList
        
        L1Snapshot(
          material = materialStr,
          imbalance = imbalanceStr,
          kingSafetyUs = kingSafetyUs,
          kingSafetyThem = kingSafetyThem,
          mobility = mobilityStr,
          centerControl = filteredCenter,
          openFiles = filteredOpenFiles
        )
        
      case None =>
        // Fallback stub when features not available
        L1Snapshot(
          material = "=",
          imbalance = None,
          kingSafetyUs = None,
          kingSafetyThem = None,
          mobility = None,
          centerControl = None,
          openFiles = Nil
        )
    }
  }
  
  private def buildCandidates(data: ExtendedAnalysisData): List[CandidateInfo] = {
    data.candidates.zipWithIndex.map { case (cand, idx) =>
      val bestScore = data.candidates.headOption.map(_.score).getOrElse(0)
      val secondScore = data.candidates.lift(1).map(_.score).getOrElse(bestScore)
      def lossFromBest(score: Int): Int =
        if data.isWhiteToMove then (bestScore - score).max(0)
        else (score - bestScore).max(0)

      val bestGap = lossFromBest(secondScore)
      val cpDiff = lossFromBest(cand.score)
      val annotation =
        if idx == 0 then
          // Avoid overusing "!"; only mark when the best move is clearly separated.
          if bestGap >= Thresholds.MISTAKE_CP then "!" else ""
        else if cpDiff >= Thresholds.DOUBLE_QUESTION_CP then "??"
        else if cpDiff >= Thresholds.SINGLE_QUESTION_CP then "?"
        else if cpDiff >= Thresholds.DUBIOUS_CP then "?!"
        else ""
      val sanMoves = NarrativeUtils.uciListToSan(data.fen, cand.line.moves.take(2))
      val moveSan = sanMoves.headOption.getOrElse(cand.move)
      val responseMotifs = cand.motifs.filter(_.plyIndex == 1)
      val alert = responseMotifs.collectFirst {
        case _: Motif.Check => s"allows ${sanMoves.lift(1).getOrElse("")} check"
        case _: Motif.Fork => s"allows ${sanMoves.lift(1).getOrElse("")} fork"
        case m: Motif.Capture if m.captureType == Motif.CaptureType.Winning => 
          s"allows ${sanMoves.lift(1).getOrElse("")} winning capture"
        case _: Motif.DiscoveredAttack => "reveals discovered attack"
      }
      val selfMotifs = cand.motifs.filter(_.plyIndex == 0)
      val tacticEvidence = selfMotifs.flatMap { m =>
        m match {
          case f: Motif.Fork => 
            Some(s"Fork(${f.attackingPiece} on ${f.square} vs ${f.targets.mkString(",")})")
          case p: Motif.Pin => 
            val pSq = p.pinnedSq.map(_.key).getOrElse("?")
            val bSq = p.behindSq.map(_.key).getOrElse("?")
            Some(s"Pin(${p.pinnedPiece} on $pSq to ${p.targetBehind} on $bSq)")
          case c: Motif.Capture if c.captureType == Motif.CaptureType.Winning => 
            Some(s"WinningCapture(${c.piece} takes ${c.captured} on ${c.square})")
          case d: Motif.DiscoveredAttack => 
            val aSq = d.attackingSq.map(_.key).getOrElse("?")
            val tSq = d.targetSq.map(_.key).getOrElse("?")
            Some(s"DiscoveredAttack(${d.attackingPiece} from $aSq on ${d.target} on $tSq)")
          case ch: Motif.Check => 
            Some(s"Check(${ch.piece} on ${ch.targetSquare})")
          case sk: Motif.Skewer =>
            val fSq = sk.frontSq.map(_.key).getOrElse("?")
            val bSq = sk.backSq.map(_.key).getOrElse("?")
            Some(s"Skewer(${sk.attackingPiece} through ${sk.frontPiece} on $fSq to ${sk.backPiece} on $bSq)")
          case o: Motif.Outpost =>
            Some(s"Outpost(${o.piece} on ${o.square})")
          case of: Motif.OpenFileControl =>
            Some(s"OpenFileControl(Rook on ${of.file}-file)")
          case c: Motif.Centralization =>
            Some(s"Centralization(${c.piece} on ${c.square})")
          case rl: Motif.RookLift =>
            Some(s"RookLift(Rook to rank ${rl.toRank})")
          case dom: Motif.Domination =>
            Some(s"Domination(${dom.dominatingPiece} dominates ${dom.dominatedPiece})")
          case man: Motif.Maneuver =>
            Some(s"Maneuver(${man.piece.name}, ${man.purpose})")
          case tp: Motif.TrappedPiece =>
            Some(s"TrappedPiece(${tp.trappedRole})")
          case kvb: Motif.KnightVsBishop =>
            Some(s"KnightVsBishop(${kvb.color}, ${kvb.isKnightBetter})")
          case b: Motif.Blockade =>
            Some(s"Blockade(${b.piece}, ${b.pawnSquare})")
          case _: Motif.SmotheredMate =>
            Some(s"SmotheredMate(Knight)")
          case xr: Motif.XRay =>
            Some(s"XRay(${xr.piece} through to ${xr.target} on ${xr.square})")
          case _ => None
        }
      }
      val alignment = cand.moveIntent.immediate
      val downstream = cand.moveIntent.downstream
      val whyNot = if (idx > 0 && cpDiff >= 50) {
        val diff = cpDiff / 100.0
        val refutation = sanMoves.lift(1).map(m => s" after $m").getOrElse("")
        val reason = if (diff > 2.0) "decisive loss" else if (diff > 0.8) "significant disadvantage" else "slight inaccuracy"
        Some(f"$reason ($diff%.1f)$refutation")
      } else None
      
      // Phase F: VariationTag -> CandidateTag mapping
      val tags = mapVariationTags(cand.line.tags)

      CandidateInfo(
        move = moveSan,
        uci = Some(cand.move),
        annotation = annotation,
        planAlignment = alignment,
        structureGuidance = None,
        alignmentBand = None,
        downstreamTactic = downstream,
        tacticalAlert = alert,
        practicalDifficulty = if (cand.line.moves.length > 6) "complex" else "clean",
        whyNot = whyNot,
        tags = tags,
        tacticEvidence = tacticEvidence,
        facts = cand.facts
      )
    }.filterNot(_.move.isEmpty)
  }
  
  /**
   * Maps VariationTag enum to CandidateTag.
   */
  private def mapVariationTags(varTags: List[lila.llm.model.strategic.VariationTag]): List[CandidateTag] = {
    import lila.llm.model.strategic.VariationTag
    varTags.flatMap {
      case VariationTag.Sharp        => Some(CandidateTag.Sharp)
      case VariationTag.Solid        => Some(CandidateTag.Solid)
      case VariationTag.Prophylaxis  => Some(CandidateTag.Prophylactic)
      case VariationTag.Simplification => Some(CandidateTag.Converting)
      case _ => None // Mistake, Good, Excellent, Inaccuracy, Blunder, Forced are not strategic tags
    }
  }
  
  private val defaultClassification = PositionClassification(
    nature = NatureResult(lila.llm.analysis.L3.NatureType.Static, 0, 0, 0, false),
    criticality = CriticalityResult(CriticalityType.Normal, 0, None, 0),
    choiceTopology = ChoiceTopologyResult(ChoiceTopologyType.StyleChoice, 0, 0, None, 0, 0, None),
    gamePhase = GamePhaseResult(GamePhaseType.Middlegame, 0, false, 0),
    simplifyBias = SimplifyBiasResult(false, 0, false, false),
    drawBias = DrawBiasResult(false, false, false, false, false),
    riskProfile = RiskProfileResult(RiskLevel.Medium, 0, 0, 0),
    taskMode = TaskModeResult(TaskModeType.ExplainPlan, "default")
  )
  
  /**
   * Compute actual open file letters from FEN.
   * An open file has no pawns (white or black) on it.
   */
  private def computeOpenFiles(fen: String): List[String] = {
    val boardPart = fen.split(" ").headOption.getOrElse("")
    val ranks = boardPart.split("/")
    
    // Expand FEN rank notation (e.g., "rnbqkbnr" or "8" or "3p4")
    def expandRank(rank: String): String = {
      rank.flatMap { c =>
        if (c.isDigit) " " * c.asDigit
        else c.toString
      }
    }
    
    val expandedRanks = ranks.map(expandRank)
    val files = "abcdefgh"
    
    // Check each file (column) for pawns
    files.zipWithIndex.collect {
      case (fileChar, fileIdx) if expandedRanks.forall { rank =>
        rank.lift(fileIdx).forall(c => c != 'p' && c != 'P')
      } => fileChar.toString
    }.toList
  }
  
  private def buildPhaseContext(
    ctx: IntegratedContext,
    prevAnalysis: Option[ExtendedAnalysisData]
  ): PhaseContext = {
    val phaseResult = ctx.classification.map(_.gamePhase)
    val currentPhase = ctx.phase.capitalize
    
    val reason = phaseResult.map { pr =>
      val queens = if (pr.queensOnBoard) "Queens present" else "Queens traded"
      s"Material: ${pr.totalMaterial}, Minors: ${pr.minorPiecesCount}, $queens"
    }.getOrElse("Insufficient data")
    
    val transitionTrigger = prevAnalysis.flatMap { prev =>
      if (prev.phase != ctx.phase) {
        val from = prev.phase.capitalize
        val to = currentPhase
        Some(s"Transition from $from to $to")
      } else None
    }
    
    PhaseContext(
      current = currentPhase,
      reason = reason,
      transitionTrigger = transitionTrigger
    )
  }

  private def buildDelta(
    prev: ExtendedAnalysisData,
    current: ExtendedAnalysisData
  ): MoveDelta = {
    val evalChange = current.evalCp - prev.evalCp

    val prevOpenFiles = prev.positionalFeatures.collect { case PositionalTag.OpenFile(f, _) => f }.toSet
    val currOpenFiles = current.positionalFeatures.collect { case PositionalTag.OpenFile(f, _) => f }.toSet
    val openFileCreated = (currOpenFiles -- prevOpenFiles).toList.sortBy(_.char).headOption.map(_.char.toString)
    
    val prevMotifs = prev.motifs.map(_.getClass.getSimpleName).toSet
    val currMotifs = current.motifs.map(_.getClass.getSimpleName).toSet
    
    val newMotifs = (currMotifs -- prevMotifs).toList.sorted
    val lostMotifs = (prevMotifs -- currMotifs).toList.sorted
    
    // Simple structure change detection
    val structureChange = if (newMotifs.contains("PawnChain") && !prevMotifs.contains("PawnChain"))
      Some("Pawn chain established")
    else if (lostMotifs.contains("PawnChain"))
      Some("Pawn chain broken")
    else if (current.structuralWeaknesses.size > prev.structuralWeaknesses.size)
      Some("New structural weakness appeared")
    else None

    MoveDelta(
      evalChange = evalChange,
      newMotifs = newMotifs,
      lostMotifs = lostMotifs,
      structureChange = structureChange,
      openFileCreated = openFileCreated,
      phaseChange = None
    )
  }
  // META SIGNALS (B-axis)

  /**
   * Builds MetaSignals only if meaningful source data exists.
   * Returns None if no classification, threats, or planSequence.
   */
  private def buildMetaSignals(data: ExtendedAnalysisData, ctx: IntegratedContext, targets: Targets, probeResults: List[ProbeResult]): Option[MetaSignals] = {
    // Require at least classification OR threats OR plans to populate meta
    val hasClassification = ctx.classification.isDefined
    val hasThreats = ctx.threatsToUs.isDefined || ctx.threatsToThem.isDefined
    val hasPlans = data.plans.nonEmpty
    val hasProbeResults = probeResults.nonEmpty
    
    if (!hasClassification && !hasThreats && !hasPlans && !hasProbeResults) None
    else {
      val choiceType = buildChoiceType(ctx)
      val planConcurrency = buildPlanConcurrency(data)
      val divergence = buildDivergence(data)
      val errorClass = buildErrorClassification(data)
      val whyNot = buildWhyNotSummary(probeResults, ctx.isWhiteToMove)

      Some(MetaSignals(
        choiceType = choiceType,
        targets = targets,
        planConcurrency = planConcurrency,
        divergence = divergence,
        errorClass = errorClass,
        whyNot = whyNot
      ))
    }
  }

  /**
   * B7: WhyNot summary for the Meta section.
   * Enhanced with L1 delta collapse reasons.
   */
  private def buildWhyNotSummary(probeResults: List[ProbeResult], isWhiteToMove: Boolean): Option[String] = {
    if (probeResults.isEmpty) None
    else {
      val summaries = probeResults.flatMap { pr =>
        val moverLoss = if (isWhiteToMove) -pr.deltaVsBaseline else pr.deltaVsBaseline
        if (moverLoss >= 200) {
          val moveText = pr.probedMove.getOrElse("this move")
          // Phase C: Include L1 collapse reason if available
          val collapseNote = pr.l1Delta.flatMap(_.collapseReason).map(r => s" ($r)").getOrElse("")
          Some(s"'$moveText' is refuted losing $moverLoss cp$collapseNote [Verified]")
        } else None
      }
      if (summaries.isEmpty) None else Some(summaries.mkString(". "))
    }
  }

  private def buildCandidatesEnriched(
    data: ExtendedAnalysisData,
    probeResults: List[ProbeResult],
    planAlignment: Option[lila.llm.model.structure.PlanAlignment]
  ): List[CandidateInfo] = {
    val isWhiteToMove = data.isWhiteToMove
    val existing = buildCandidates(data)
    val existingUcis = existing.flatMap(_.uci).toSet
    
    val enriched = existing.zipWithIndex.map { case (c, idx) =>
      val probe = probeResults.find(pr => pr.probedMove == c.uci)

      // Probe PV samples (a1/a2): convert probe reply PVs into SAN lines, starting from opponent reply.
      val probeLines = probe.flatMap { pr =>
        val moveUciOpt = c.uci
        val replyPvs = pr.replyPvs.getOrElse {
          if (pr.bestReplyPv.nonEmpty) List(pr.bestReplyPv) else Nil
        }

        moveUciOpt.map { moveUci =>
          replyPvs.take(2).flatMap { pv =>
            val san = NarrativeUtils.uciListToSan(data.fen, moveUci :: pv.take(6)).drop(1).mkString(" ")
            Option(san).map(_.trim).filter(_.nonEmpty)
          }
        }
      }.getOrElse(Nil)
      
      // 1. Logic for tags
      val probeTags = probe.map { pr =>
        if (pr.id.startsWith("competitive")) List(CandidateTag.Competitive)
        else if (pr.id.startsWith("aggressive") && (if (isWhiteToMove) -pr.deltaVsBaseline else pr.deltaVsBaseline) >= 50) 
          List(CandidateTag.TacticalGamble)
        else Nil
      }.getOrElse(Nil)

      // 2. Logic for whyNot
      val whyNot = probe.flatMap { pr =>
        val moverLoss = if (isWhiteToMove) -pr.deltaVsBaseline else pr.deltaVsBaseline
        if (moverLoss >= 50) {
           val bestReply = pr.bestReplyPv.headOption.getOrElse("?")
           val replySan = c.uci.map(u => NarrativeUtils.uciListToSan(data.fen, List(u, bestReply)).lift(1).getOrElse(bestReply)).getOrElse(bestReply)
           val collapseNote = pr.l1Delta.flatMap(_.collapseReason).map(r => s" ($r)").getOrElse("")
           
           if (pr.id.startsWith("aggressive"))
             Some(s"refuted: $replySan is a clear answer$collapseNote")
           else
             Some(s"inferior by $moverLoss cp after $replySan$collapseNote")
        } else if (pr.id.startsWith("competitive") && moverLoss.abs < 30) {
           Some("verified as a strong alternative")
        } else None
      }

      val baseCandidate = c.copy(
        whyNot = whyNot.orElse(c.whyNot),
        tags = (c.tags ++ probeTags).distinct,
        probeLines = probeLines
      )
      toneCandidateByAlignment(baseCandidate, idx, planAlignment)
    }
    
    val ghosts = probeResults.filter(pr => pr.probedMove.isDefined && !pr.probedMove.exists(existingUcis.contains)).map { pr =>
      val uci = pr.probedMove.get
      val san = NarrativeUtils.uciListToSan(data.fen, List(uci)).headOption.getOrElse(uci)
      val moverLoss = if (isWhiteToMove) -pr.deltaVsBaseline else pr.deltaVsBaseline
      val bestReply = pr.bestReplyPv.headOption.getOrElse("?")
      val replySan = NarrativeUtils.uciListToSan(data.fen, List(uci, bestReply)).lift(1).getOrElse(bestReply)
      val collapseNote = pr.l1Delta.flatMap(_.collapseReason).map(r => s" ($r)").getOrElse("")

      val ghostTags = if (pr.id.startsWith("aggressive")) List(CandidateTag.TacticalGamble, CandidateTag.Sharp)
                      else if (pr.id.startsWith("competitive")) List(CandidateTag.Competitive)
                      else Nil

      CandidateInfo(
        move = san,
        uci = Some(uci),
        annotation = if (moverLoss >= 200) "??" else "?",
        planAlignment = if (pr.id.startsWith("aggressive")) "Tactical Gamble" else "Alternative Path",
        structureGuidance = None,
        alignmentBand = None,
        tacticalAlert = None,
        practicalDifficulty = "complex",
        whyNot = Some(s"refuted by $replySan (-$moverLoss cp)$collapseNote"),
        tags = ghostTags,
        probeLines =
          pr.replyPvs.getOrElse(if (pr.bestReplyPv.nonEmpty) List(pr.bestReplyPv) else Nil).take(2).flatMap { pv =>
            val sanLine = NarrativeUtils.uciListToSan(data.fen, uci :: pv.take(6)).drop(1).mkString(" ")
            Option(sanLine).map(_.trim).filter(_.nonEmpty)
          }
      )
    }
    
    enriched ++ ghosts
  }

  private def toneCandidateByAlignment(
    candidate: CandidateInfo,
    index: Int,
    planAlignment: Option[lila.llm.model.structure.PlanAlignment]
  ): CandidateInfo =
    if index > 1 then candidate
    else
      planAlignment match
        case Some(pa) =>
          val guidance = pa.narrativeIntent.orElse(Some(defaultGuidance(pa.band)))
          val riskNote = riskHintFromAlignment(pa)
          val whyNotWithRisk = mergeWhyNot(candidate.whyNot, riskNote)
          pa.band match
            case AlignmentBand.OnBook =>
              candidate.copy(
                structureGuidance = guidance,
                alignmentBand = Some(pa.band.toString),
                whyNot = if index == 0 then candidate.whyNot else whyNotWithRisk
              )
            case AlignmentBand.Playable =>
              candidate.copy(
                planAlignment = s"${candidate.planAlignment} (practical)",
                structureGuidance = guidance,
                alignmentBand = Some(pa.band.toString),
                whyNot = whyNotWithRisk
              )
            case AlignmentBand.OffPlan =>
              candidate.copy(
                planAlignment = s"${candidate.planAlignment} (risky)",
                structureGuidance = guidance,
                alignmentBand = Some(pa.band.toString),
                whyNot = mergeWhyNot(whyNotWithRisk, Some("plan coherence is fragile if move order slips"))
              )
            case AlignmentBand.Unknown =>
              candidate.copy(
                planAlignment = s"${candidate.planAlignment} (needs verification)",
                structureGuidance = guidance,
                alignmentBand = Some(pa.band.toString),
                whyNot = mergeWhyNot(whyNotWithRisk, Some("structural read is uncertain, verify concrete tactics"))
              )
        case None =>
          candidate

  private def mergeWhyNot(base: Option[String], extra: Option[String]): Option[String] =
    (base.map(_.trim).filter(_.nonEmpty), extra.map(_.trim).filter(_.nonEmpty)) match
      case (Some(a), Some(b)) if a.equalsIgnoreCase(b) => Some(a)
      case (Some(a), Some(b)) => Some(s"$a; $b")
      case (Some(a), None) => Some(a)
      case (None, Some(b)) => Some(b)
      case _ => None

  private def summaryHintFromAlignment(pa: lila.llm.model.structure.PlanAlignment): String =
    pa.band match
      case AlignmentBand.OnBook => " [plan coherence high]"
      case AlignmentBand.Playable => " [playable structure plan]"
      case AlignmentBand.OffPlan => " [structural risk]"
      case AlignmentBand.Unknown => " [structure needs verification]"

  private def flowHintFromAlignment(pa: lila.llm.model.structure.PlanAlignment): Option[String] =
    pa.band match
      case AlignmentBand.OnBook => Some("The continuation remains structurally coherent.")
      case AlignmentBand.Playable => Some("The continuation is playable but needs accurate move order.")
      case AlignmentBand.OffPlan => Some("The current route drifts from the cleaner structural plan.")
      case AlignmentBand.Unknown => Some("Structural interpretation is uncertain, so concrete checks matter.")

  private def riskHintFromAlignment(pa: lila.llm.model.structure.PlanAlignment): Option[String] =
    pa.band match
      case AlignmentBand.OnBook =>
        None
      case AlignmentBand.Playable =>
        pa.reasonWeights.get("PRECOND_MISS").filter(_ > 0.0).map(_ => "some strategic preconditions are still missing")
      case AlignmentBand.OffPlan =>
        val anti = pa.reasonWeights.getOrElse("ANTI_PLAN", 0.0)
        val pre = pa.reasonWeights.getOrElse("PRECOND_MISS", 0.0)
        if anti >= 0.15 then Some("main line conflicts with structure-first planning")
        else if pre > 0.0 then Some("key preconditions for the structure plan are missing")
        else Some("continuation is strategically brittle")
      case AlignmentBand.Unknown =>
        Some("structure confidence is low")

  private def defaultGuidance(band: AlignmentBand): String =
    band match
      case AlignmentBand.OnBook => "keep the position coherent and improve pieces behind the pawn skeleton"
      case AlignmentBand.Playable => "maintain flexibility and avoid irreversible pawn commitments too early"
      case AlignmentBand.OffPlan => "reassess long-term pawn commitments before forcing activity"
      case AlignmentBand.Unknown => "verify tactical details first, then commit to a structural route"

  private case class HypothesisDraft(
    axis: HypothesisAxis,
    claim: String,
    supportSignals: List[String],
    conflictSignals: List[String],
    baseConfidence: Double,
    horizon: HypothesisHorizon
  )

  private case class RankedHypothesis(
    card: HypothesisCard,
    score: Double,
    family: String
  )

  private def attachHypothesisCards(
    data: ExtendedAnalysisData,
    ctx: IntegratedContext,
    candidates: List[CandidateInfo],
    probeResults: List[ProbeResult],
    probeRequests: List[ProbeRequest],
    openingEvent: Option[OpeningEvent]
  ): List[CandidateInfo] = {
    if (candidates.isEmpty) return candidates
    val top = candidates.headOption
    candidates.zipWithIndex.map { case (cand, idx) =>
      val (rankOpt, cpGapOpt) = candidateRankAndGap(data, cand)
      val probeSignals =
        ProbeDetector.hypothesisVerificationSignals(
          candidate = cand,
          probeResults = probeResults,
          probeRequests = probeRequests,
          isWhiteToMove = data.isWhiteToMove
        )
      val drafts =
        buildHypothesisDrafts(
          data = data,
          ctx = ctx,
          candidate = cand,
          index = idx,
          rank = rankOpt,
          cpGap = cpGapOpt,
          probeSignals = probeSignals,
          topCandidate = top,
          openingEvent = openingEvent
        )
      val ranked = drafts
        .flatMap(d => rankHypothesisDraft(d, cand, probeSignals, rankOpt, cpGapOpt))
        .sortBy(r => -r.score)

      val selected = ranked
        .foldLeft((Set.empty[String], List.empty[RankedHypothesis])) { case ((seen, acc), rh) =>
          if (seen.contains(rh.family)) (seen, acc)
          else (seen + rh.family, acc :+ rh)
        }
        ._2
      val diversified =
        selected.headOption match
          case Some(primary) =>
            val promotedPrimary =
              if primary.card.axis == HypothesisAxis.Plan then
                selected
                  .drop(1)
                  .find(rh => rh.card.axis != HypothesisAxis.Plan && rh.score >= primary.score - 0.08)
                  .getOrElse(primary)
              else primary
            val secondary =
              selected
                .filterNot(_ == promotedPrimary)
                .find(_.card.axis != promotedPrimary.card.axis)
                .orElse(selected.filterNot(_ == promotedPrimary).headOption)
            applyLongHorizonProtection(
              selected = selected,
              diversified = (promotedPrimary :: secondary.toList).take(2),
              probeSignals = probeSignals
            )
          case None => Nil
      val cards = diversified.map(_.card)

      cand.copy(hypotheses = cards)
    }
  }

  private def candidateRankAndGap(
    data: ExtendedAnalysisData,
    candidate: CandidateInfo
  ): (Option[Int], Option[Int]) = {
    val byVariation = data.alternatives.zipWithIndex.collectFirst {
      case (v, idx) if candidate.uci.exists(cu => v.moves.headOption.exists(vu => NarrativeUtils.uciEquivalent(cu, vu))) =>
        (idx + 1, v.effectiveScore)
    }
    val byCandidate = data.candidates.zipWithIndex.collectFirst {
      case (c, idx) if candidate.uci.exists(cu => NarrativeUtils.uciEquivalent(cu, c.move)) =>
        (idx + 1, c.score)
    }
    val matched = byVariation.orElse(byCandidate)
    val bestScore =
      data.alternatives.headOption.map(_.effectiveScore).orElse(data.candidates.headOption.map(_.score))
    val cpGap = for
      best <- bestScore
      (_, score) <- matched
    yield cpLossForSideToMove(best, score, data.isWhiteToMove)
    (matched.map(_._1), cpGap)
  }

  private def cpLossForSideToMove(best: Int, score: Int, isWhiteToMove: Boolean): Int =
    if isWhiteToMove then (best - score).max(0) else (score - best).max(0)

  private def buildHypothesisDrafts(
    data: ExtendedAnalysisData,
    ctx: IntegratedContext,
    candidate: CandidateInfo,
    index: Int,
    rank: Option[Int],
    cpGap: Option[Int],
    probeSignals: ProbeDetector.HypothesisVerificationSignals,
    topCandidate: Option[CandidateInfo],
    openingEvent: Option[OpeningEvent]
  ): List[HypothesisDraft] = {
    val move = candidate.move.trim
    val alignment = candidate.planAlignment.trim
    val alignmentLow = alignment.toLowerCase
    val planName = data.plans.headOption.map(_.plan.name).getOrElse("current plan")
    val practicalLow = candidate.practicalDifficulty.trim.toLowerCase
    val whyNot = candidate.whyNot.getOrElse("").toLowerCase
    val tacticalAlert = candidate.tacticalAlert.getOrElse("").toLowerCase
    val isKnightRouteShift = topCandidate.exists(tc => tc != candidate && knightRouteShift(tc, candidate))
    val breakFile = ctx.pawnAnalysis.flatMap(_.breakFile)
    val breakReady = ctx.pawnAnalysis.exists(_.pawnBreakReady)
    val breakImpact = ctx.pawnAnalysis.map(_.breakImpact).getOrElse(0)
    val conversionWindow =
      if data.isWhiteToMove then data.evalCp >= 80
      else data.evalCp <= -80
    val hasKingSignal =
      tacticalAlert.contains("king") ||
        tacticalAlert.contains("check") ||
        tacticalAlert.contains("mate") ||
        ctx.threatsToUs.exists(_.threats.exists(t => t.kind == ThreatKind.Mate || t.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP))
    val hasStructSignal =
      candidate.facts.exists {
        case _: Fact.WeakSquare | _: Fact.Outpost => true
        case _                                     => false
      } || whyNot.contains("structure") || whyNot.contains("square") || whyNot.contains("pawn")
    val openingSignal = openingEvent.map {
      case OpeningEvent.BranchPoint(_, reason, _) =>
        s"opening branch point: ${reason.take(42)}"
      case OpeningEvent.Novelty(_, _, evidence, _) =>
        s"opening novelty evidence: ${evidence.take(42)}"
      case OpeningEvent.OutOfBook(_, _, _) =>
        "position moved out of book"
      case OpeningEvent.TheoryEnds(_, sampleCount) =>
        s"theory sample thinned to $sampleCount games"
      case OpeningEvent.Intro(_, _, theme, _) =>
        s"opening theme: ${theme.take(42)}"
    }
    val cpSignal = cpGap.map(g => s"engine gap ${f"${g.toDouble / 100}%.1f"} pawns")
    val localSeed = Math.abs(candidate.move.hashCode) ^ (index * 0x9e3779b9) ^ cpGap.getOrElse(0)
    val strategicFrame = Option.when(data.phase.equalsIgnoreCase("middlegame"))(probeSignals.strategicFrame).flatten
    val planScoreSignal = data.plans.headOption.map(p => variedPlanScoreSignal(p.score, localSeed ^ 0x24d8f59c))
    val rankSignal = rank.map(r => variedEngineRankSignal(r, localSeed ^ 0x3b5296f1))
    val motifSignal = candidate.tacticEvidence.headOption.map(_.take(56))
    val planClaim =
      strategicFrame
        .map(frame => strategicMiddlegameClaim(HypothesisAxis.Plan, move, frame, localSeed ^ 0x11f17f1d))
        .getOrElse {
          if index == 0 then
            NarrativeLexicon.pick(localSeed ^ 0x11f17f1d, List(
              s"$move keeps ${humanPlan(planName)} as the central roadmap, limiting early strategic drift.",
              s"$move anchors play around ${humanPlan(planName)}, so follow-up choices stay structurally coherent.",
              s"$move preserves the ${humanPlan(planName)} framework and avoids premature route changes.",
              s"$move keeps the position tied to ${humanPlan(planName)}, delaying unnecessary plan detours."
            ))
          else
            NarrativeLexicon.pick(localSeed ^ 0x517cc1b7, List(
              s"$move redirects play toward ${humanPlan(alignment)}, creating a new strategic branch from the main continuation.",
              s"$move shifts the game into a ${humanPlan(alignment)} route, with a different plan cadence from the principal line.",
              s"$move chooses a ${humanPlan(alignment)} channel instead of the principal structure-first continuation.",
              s"$move reroutes priorities toward ${humanPlan(alignment)}, so the long plan map differs from the engine leader."
            ))
        }

    val planDraft = HypothesisDraft(
      axis = HypothesisAxis.Plan,
      claim = planClaim,
      supportSignals =
        List(
          planScoreSignal,
          rankSignal,
          openingSignal
        ).flatten,
      conflictSignals =
        List(
          Option.when(cpGap.exists(_ >= 100))("engine gap is significant for this route"),
          Option.when(candidate.tags.contains(CandidateTag.TacticalGamble))("line is flagged as tactical gamble")
        ).flatten,
      baseConfidence = if index == 0 then 0.58 else 0.48,
      horizon = HypothesisHorizon.Medium
    )

    val structureClaim =
      if hasStructSignal then
        NarrativeLexicon.pick(localSeed ^ 0x5f356495, List(
          s"$move changes the structural balance, trading immediate activity for longer-term square commitments.",
          s"$move reshapes pawn and square commitments, accepting delayed strategic consequences for present activity.",
          s"$move keeps structural tensions central, where current activity is exchanged for a longer-term square map.",
          s"$move commits to a structural route first, so long-term square control outweighs short tactical comfort."
        ))
      else
        NarrativeLexicon.pick(localSeed ^ 0x6c6c6c6c, List(
          s"$move tries to preserve structure first, postponing irreversible pawn commitments.",
          s"$move keeps the pawn skeleton flexible and delays structural decisions until better timing appears.",
          s"$move maintains structural optionality, avoiding early pawn commitments that narrow later plans.",
          s"$move prioritizes structural elasticity now, so irreversible pawn choices are deferred."
        ))
    val structureDraft = HypothesisDraft(
      axis = HypothesisAxis.Structure,
      claim = structureClaim,
      supportSignals =
        List(
          Option.when(hasStructSignal)("fact-level structural weakness signal"),
          Option.when(breakReady)("pawn tension context is active")
        ).flatten,
      conflictSignals =
        List(
          Option.when(whyNot.contains("weak"))("candidate rationale already flags a weakness"),
          Option.when(cpGap.exists(_ >= 120))("engine penalizes resulting structure")
        ).flatten,
      baseConfidence = if hasStructSignal then 0.52 else 0.43,
      horizon = HypothesisHorizon.Long
    )

    val initiativeClaim =
      strategicFrame
        .map(frame => strategicMiddlegameClaim(HypothesisAxis.Initiative, move, frame, localSeed ^ 0x4f6cdd1d))
        .getOrElse {
          if candidate.tags.exists(t => t == CandidateTag.Sharp || t == CandidateTag.TacticalGamble) || practicalLow == "complex" then
            NarrativeLexicon.pick(localSeed ^ 0x4f6cdd1d, List(
              s"$move pushes for initiative immediately, but tempo accuracy is mandatory from move one.",
              s"$move seeks dynamic momentum now, so even one slow follow-up can reverse the practical balance.",
              s"$move is an initiative bid: concrete timing is required before the opponent consolidates.",
              s"$move keeps the initiative race open, with little margin for imprecise sequencing."
            ))
          else
            NarrativeLexicon.pick(localSeed ^ 0x63d5a6f1, List(
              s"$move concedes some initiative for stability, so the practical test is whether counterplay can be contained.",
              s"$move trades immediate initiative for structure, and the key question is if counterplay arrives in time.",
              s"$move prioritizes stability over momentum, making initiative handoff the central practical risk.",
              s"$move slows the initiative race deliberately, betting that the resulting position is easier to control."
            ))
        }

    val initiativeDraft = HypothesisDraft(
      axis = HypothesisAxis.Initiative,
      claim = initiativeClaim,
      supportSignals =
        List(
          motifSignal,
          cpSignal,
          Option.when(practicalLow == "complex")("practical complexity is high")
        ).flatten,
      conflictSignals =
        List(
          Option.when(cpGap.exists(_ >= 90))("initiative handoff is too costly"),
          Option.when(candidate.whyNot.nonEmpty)("existing refutation note points to initiative drift")
        ).flatten,
      baseConfidence = 0.5,
      horizon = HypothesisHorizon.Short
    )

    val conversionDraft = HypothesisDraft(
      axis = HypothesisAxis.Conversion,
      claim =
        if conversionWindow then
          s"$move frames conversion as a timing problem: simplifying too early or too late can change the practical result."
        else
          s"$move keeps conversion deferred, prioritizing coordination before simplification.",
      supportSignals =
        List(
          Option.when(conversionWindow)("evaluation indicates a conversion window"),
          Option.when(candidate.tags.contains(CandidateTag.Converting))("candidate tagged as converting")
        ).flatten,
      conflictSignals =
        List(
          Option.when(practicalLow == "complex")("line remains tactically demanding to convert"),
          Option.when(cpGap.exists(_ >= 110))("conversion route loses too much objective value")
        ).flatten,
      baseConfidence = if conversionWindow then 0.53 else 0.42,
      horizon = HypothesisHorizon.Medium
    )

    val kingSafetyDraft = HypothesisDraft(
      axis = HypothesisAxis.KingSafety,
      claim =
        if hasKingSignal then
          s"$move alters king-safety tempo, so defensive coordination must stay synchronized with the next forcing move."
        else
          s"$move keeps king safety mostly stable, but only if move order avoids loose tempos.",
      supportSignals =
        List(
          Option.when(hasKingSignal)("threat or tactical alert points to king safety"),
          Option.when(tacticalAlert.contains("check"))("candidate alert includes check geometry")
        ).flatten,
      conflictSignals =
        List(
          Option.when(whyNot.contains("king"))("candidate rationale already flags king safety issues"),
          Option.when(cpGap.exists(_ >= 140))("engine punishes resulting king exposure")
        ).flatten,
      baseConfidence = if hasKingSignal then 0.55 else 0.4,
      horizon = HypothesisHorizon.Short
    )

    val pieceCoordDraft = HypothesisDraft(
      axis = HypothesisAxis.PieceCoordination,
      claim =
        knightRouteShiftClaim(topCandidate, candidate).getOrElse(
          s"$move changes piece coordination lanes, with activity gains balanced against route efficiency."
        ),
      supportSignals =
        List(
          Option.when(isKnightRouteShift)("knight development route diverges from main line"),
          Option.when(isPieceMove(candidate.move))("piece move directly changes coordination map"),
          Option.when(alignmentLow.contains("development") || alignmentLow.contains("activation"))("intent is coordination-led")
        ).flatten,
      conflictSignals =
        List(
          Option.when(cpGap.exists(_ >= 100))("coordination route is slower than principal line")
        ).flatten,
      baseConfidence = if isKnightRouteShift then 0.6 else 0.47,
      horizon = HypothesisHorizon.Medium
    )

    val pawnBreakClaim =
      strategicFrame
        .map(frame => strategicMiddlegameClaim(HypothesisAxis.PawnBreakTiming, move, frame, localSeed ^ 0x1f123bb5))
        .getOrElse {
          if breakReady && isLikelyPawnMove(move) then
            NarrativeLexicon.pick(localSeed ^ 0x1f123bb5, List(
              s"$move clarifies pawn tension immediately, preferring direct break resolution over extra preparation.",
              s"$move commits to immediate break clarification, accepting concrete consequences now instead of waiting.",
              s"$move resolves the pawn-break question at once, choosing concrete timing over additional setup.",
              s"$move forces the break decision now, so follow-up accuracy matters more than setup completeness.",
              s"$move brings pawn tension to a concrete verdict immediately rather than extending preparation."
            ))
          else if breakReady then
            NarrativeLexicon.pick(localSeed ^ 0x4e67c6a7, List(
              s"$move keeps the break in reserve and improves support before committing.",
              s"$move postpones the break by one phase, aiming for stronger piece support first.",
              s"$move holds pawn tension for now, preparing better support before release.",
              s"$move delays direct break action so supporting pieces can coordinate first.",
              s"$move keeps break timing deferred, prioritizing support links before commitment."
            ))
          else
            NarrativeLexicon.pick(localSeed ^ 0x3c79ac49, List(
              s"$move keeps break timing flexible, so central tension can be revisited under better conditions.",
              s"$move preserves pawn-break optionality, leaving central tension unresolved for a later moment.",
              s"$move avoids forcing a break now, keeping the central lever available for a better window.",
              s"$move keeps the break decision open, waiting for clearer support and fewer tactical liabilities.",
              s"$move maintains tension without immediate release, aiming to choose the break after more development."
            ))
        }

    val pawnBreakDraft = HypothesisDraft(
      axis = HypothesisAxis.PawnBreakTiming,
      claim = pawnBreakClaim,
      supportSignals =
        List(
          breakFile.map(f => s"$f-file break is available"),
          Option.when(breakImpact >= 150)("break impact is materially relevant"),
          Option.when(ctx.pawnAnalysis.exists(_.tensionPolicy == TensionPolicy.Maintain))("current policy prefers tension maintenance")
        ).flatten,
      conflictSignals =
        List(
          Option.when(cpGap.exists(_ >= 90) && breakReady)("timing choice concedes evaluation too early"),
          Option.when(candidate.whyNot.exists(_.toLowerCase.contains("tempo")))("candidate rationale flags timing cost")
        ).flatten,
      baseConfidence = if breakReady then 0.56 else 0.41,
      horizon = if breakReady then HypothesisHorizon.Short else HypothesisHorizon.Medium
    )

    val endgameClaim =
      if data.phase == "endgame" || candidate.tags.contains(CandidateTag.Converting) then
        NarrativeLexicon.pick(localSeed ^ 0x2f6e2b1, List(
          s"$move influences the endgame trajectory by prioritizing activity over static structure.",
          s"$move points the game toward a technical ending where active piece routes matter more than static shape.",
          s"$move tilts the future ending toward dynamic conversion, with activity carrying more weight than fixed structure.",
          s"$move frames the late phase as a technique problem, emphasizing active coordination over static anchors."
        ))
      else
        NarrativeLexicon.pick(localSeed ^ 0x19f8b4ad, List(
          s"$move keeps the endgame trajectory open, with the long-term outcome hinging on later simplification choices.",
          s"$move defers the final trajectory choice, so the endgame direction depends on which simplification arrives first.",
          s"$move preserves multiple late-game paths, and the practical result depends on future simplification timing.",
          s"$move leaves the ending map unresolved for now, with long-term value decided by subsequent exchanges."
        ))
    val endgameDraft = HypothesisDraft(
      axis = HypothesisAxis.EndgameTrajectory,
      claim = endgameClaim,
      supportSignals =
        List(
          Option.when(data.phase == "endgame")("position is already in endgame phase"),
          Option.when(data.endgameFeatures.isDefined)("endgame feature signal is available"),
          Option.when(candidate.tags.contains(CandidateTag.Converting))("candidate carries conversion tag")
        ).flatten,
      conflictSignals =
        List(
          Option.when(cpGap.exists(_ >= 120))("long-term trajectory is objectively inferior"),
          Option.when(practicalLow == "complex" && data.phase == "endgame")("technical conversion remains unstable")
        ).flatten,
      baseConfidence = if data.endgameFeatures.isDefined then 0.52 else 0.4,
      horizon = HypothesisHorizon.Long
    )

    List(
      planDraft,
      structureDraft,
      initiativeDraft,
      conversionDraft,
      kingSafetyDraft,
      pieceCoordDraft,
      pawnBreakDraft,
      endgameDraft
    ).filter(d => d.claim.trim.nonEmpty)
  }

  private def strategicMiddlegameClaim(
    axis: HypothesisAxis,
    move: String,
    frame: ProbeDetector.StrategicFrame,
    seed: Int
  ): String = {
    val focus = strategicAxisFocus(axis)
    val causeSentence =
      frame.cause match
        case "near-baseline" =>
          NarrativeLexicon.pick(seed ^ 0x11f17f1d, List(
            s"Probe evidence keeps **$move** near baseline, so $focus remains a viable route.",
            s"With **$move**, probe feedback stays close to baseline and keeps $focus practical.",
            s"Baseline proximity in probe lines suggests **$move** can sustain $focus."
          ))
        case "manageable-concession" =>
          NarrativeLexicon.pick(seed ^ 0x517cc1b7, List(
            s"Probe data shows **$move** as a manageable concession, so $focus timing must stay accurate.",
            s"A manageable probe concession appears after **$move**, which makes $focus sequencing critical.",
            s"Probe checks rate **$move** as manageable, but $focus cannot afford drift."
          ))
        case _ =>
          NarrativeLexicon.pick(seed ^ 0x4f6cdd1d, List(
            s"Probe data marks **$move** as a forcing swing, so $focus errors are punished immediately.",
            s"A forcing swing appears after **$move** in probe lines, narrowing the $focus margin for error.",
            s"Probe checks flag **$move** as a forcing swing, so $focus precision is mandatory."
          ))

    val consequenceSentence =
      frame.consequence match
        case "structural-collapse" =>
          NarrativeLexicon.pick(seed ^ 0x63d5a6f1, List(
            s"The consequence is rapid structural concession, and $focus options become harder to recover.",
            s"Under pressure, structure can collapse and $focus plans lose flexibility.",
            s"Once structure loosens, $focus choices become reactive rather than planned."
          ))
        case "king-safety-exposure" =>
          NarrativeLexicon.pick(seed ^ 0x5f356495, List(
            s"The consequence is king-safety fragility, and $focus decisions must prioritize defensive coordination.",
            s"King exposure increases practical risk and forces $focus choices into defensive mode.",
            s"With king safety exposed, $focus plans are constrained by immediate defensive duties."
          ))
        case "conversion-cost" =>
          NarrativeLexicon.pick(seed ^ 0x6c6c6c6c, List(
            s"The consequence is higher conversion cost, so $focus gains are harder to convert cleanly.",
            s"This route raises conversion cost, and $focus edges are difficult to cash in.",
            s"Technical conversion gets heavier, making $focus execution less efficient."
          ))
        case _ =>
          NarrativeLexicon.pick(seed ^ 0x2f6e2b1, List(
            s"The consequence is a quick initiative flip, so $focus handling becomes reactive.",
            s"Initiative handoff risk rises, and $focus planning must absorb counterplay.",
            s"Momentum can transfer in one sequence, forcing $focus into damage control."
          ))

    val turningPointSentence =
      frame.turningPoint match
        case "immediate-sequence" =>
          NarrativeLexicon.pick(seed ^ 0x19f8b4ad, List(
            "The critical test comes in the next forcing sequence.",
            "Immediate tactical sequencing will decide whether the plan survives.",
            "The route is judged in the very next concrete sequence."
          ))
        case "simplification-transition" =>
          NarrativeLexicon.pick(seed ^ 0x7f4a7c15, List(
            "The turning point arrives at the next simplification transition.",
            "Once exchanges begin, conversion timing determines whether the plan remains sound.",
            "Simplification choices are the key checkpoint for this route."
          ))
        case _ =>
          NarrativeLexicon.pick(seed ^ 0x2a2a2a2a, List(
            "The first serious middlegame regrouping decides the practical direction.",
            "This plan is tested at the next middlegame reorganization.",
            "The route is decided when middlegame regrouping commits both sides."
          ))

    s"$causeSentence $consequenceSentence $turningPointSentence"
  }

  private def strategicAxisFocus(axis: HypothesisAxis): String =
    axis match
      case HypothesisAxis.Plan            => "plan direction"
      case HypothesisAxis.Initiative      => "initiative control"
      case HypothesisAxis.PawnBreakTiming => "pawn-break timing"
      case _                              => "strategic coordination"

  private def rankHypothesisDraft(
    draft: HypothesisDraft,
    candidate: CandidateInfo,
    probeSignals: ProbeDetector.HypothesisVerificationSignals,
    rank: Option[Int],
    cpGap: Option[Int]
  ): Option[RankedHypothesis] = {
    val isLong = draft.horizon == HypothesisHorizon.Long
    val supportLimit = if isLong then 4 else 3
    val conflictLimit = if isLong then 3 else 2
    val support =
      if isLong then
        mergeSignalsPreferred(
          preferred = probeSignals.longSupportSignals,
          fallback = draft.supportSignals ++ probeSignals.supportSignals,
          maxCount = supportLimit
        )
      else
        normalizeSignals(draft.supportSignals ++ probeSignals.supportSignals, supportLimit)
    val conflict =
      if isLong then
        mergeSignalsPreferred(
          preferred = probeSignals.longConflictSignals,
          fallback = draft.conflictSignals ++ probeSignals.conflictSignals,
          maxCount = conflictLimit
        )
      else
        normalizeSignals(draft.conflictSignals ++ probeSignals.conflictSignals, conflictLimit)
    val supportWeight = support.size * 0.17
    val conflictWeight = conflict.size * 0.16
    val consistencyBonus =
      probeSignals.consistencyBonus +
        (if rank.contains(1) then 0.08 else if cpGap.exists(_ <= 35) then 0.04 else 0.0) +
        (if candidate.whyNot.isEmpty then 0.03 else 0.0)
    val contradictionPenalty =
      probeSignals.contradictionPenalty +
        (if cpGap.exists(_ >= 140) then 0.1 else 0.0) +
        (if candidate.tags.contains(CandidateTag.TacticalGamble) then 0.05 else 0.0)
    val axisBias =
      draft.axis match
        case HypothesisAxis.Plan             => -0.06
        case HypothesisAxis.PieceCoordination => 0.06
        case HypothesisAxis.PawnBreakTiming  => 0.06
        case HypothesisAxis.KingSafety       => 0.05
        case HypothesisAxis.Initiative       => 0.04
        case HypothesisAxis.Structure        => 0.04
        case HypothesisAxis.Conversion       => 0.03
        case HypothesisAxis.EndgameTrajectory => 0.05
    val longConfidenceAdjustment = if isLong then probeSignals.longConfidenceDelta else 0.0
    val longGapAdjustment =
      if isLong then
        if cpGap.exists(_ >= 120) then -0.04
        else if cpGap.exists(_ <= 40) then 0.02
        else 0.0
      else 0.0
    val score =
      supportWeight - conflictWeight + consistencyBonus - contradictionPenalty + axisBias +
        longConfidenceAdjustment + longGapAdjustment
    val confidence = clampConfidence(draft.baseConfidence + score)
    val card =
      HypothesisCard(
        axis = draft.axis,
        claim = draft.claim.trim,
        supportSignals = support.take(supportLimit),
        conflictSignals = conflict.take(conflictLimit),
        confidence = confidence,
        horizon = draft.horizon
      )
    Option.when(card.claim.nonEmpty) {
      RankedHypothesis(card = card, score = score, family = hypothesisFamily(card))
    }
  }

  private def clampConfidence(v: Double): Double =
    Math.max(0.18, Math.min(0.93, v))

  private def normalizeSignals(signals: List[String], maxCount: Int): List[String] =
    signals.map(_.trim).filter(_.nonEmpty).distinct.take(maxCount)

  private def mergeSignalsPreferred(
    preferred: List[String],
    fallback: List[String],
    maxCount: Int
  ): List[String] =
    normalizeSignals(preferred ++ fallback, maxCount)

  private def applyLongHorizonProtection(
    selected: List[RankedHypothesis],
    diversified: List[RankedHypothesis],
    probeSignals: ProbeDetector.HypothesisVerificationSignals
  ): List[RankedHypothesis] =
    if probeSignals.longConfidenceDelta <= 0.0 then diversified
    else if diversified.isEmpty || diversified.exists(_.card.horizon == HypothesisHorizon.Long) then diversified
    else
      val primary = diversified.head
      val secondaryScore = diversified.drop(1).headOption.map(_.score).getOrElse(primary.score)
      val threshold = secondaryScore - 0.05
      val longPool =
        selected.filter { rh =>
          rh.card.horizon == HypothesisHorizon.Long &&
          !diversified.contains(rh) &&
          rh.score >= threshold
        }
      val promoted =
        longPool.find(_.card.axis != primary.card.axis).orElse(longPool.headOption)
      promoted match
        case Some(longCandidate) => List(primary, longCandidate).take(2)
        case None                => diversified

  private def hypothesisFamily(card: HypothesisCard): String =
    val stem = normalizeHypothesisStem(card.claim)
    s"${card.axis.toString.toLowerCase}:$stem"

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

  private def humanPlan(raw: String): String =
    Option(raw).getOrElse("")
      .replaceAll("""[_\-]+""", " ")
      .trim
      .toLowerCase match
      case "" => "the current setup"
      case x  => x

  private def variedPlanScoreSignal(score: Double, seed: Int): String =
    val s = f"$score%.2f"
    NarrativeLexicon.pick(seed, List(
      s"plan table confidence $s",
      s"primary plan score sits at $s",
      s"plan match score registers $s",
      s"plan-priority signal is $s"
    ))

  private def variedEngineRankSignal(rank: Int, seed: Int): String =
    NarrativeLexicon.pick(seed, List(
      s"engine ordering keeps this at rank $rank",
      s"sampled line rank is $rank",
      s"engine list position is $rank",
      s"principal-variation rank reads $rank"
    ))

  private def isLikelyPawnMove(move: String): Boolean =
    Option(move).getOrElse("").trim.matches("""^[a-h](?:x[a-h])?[1-8](?:=[QRBN])?[+#]?$""")

  private def isPieceMove(move: String): Boolean =
    Option(move).getOrElse("").trim.headOption.exists(ch => "KQRBN".contains(ch))

  private def knightRouteShift(main: CandidateInfo, alt: CandidateInfo): Boolean =
    isKnightMove(main.move) &&
      isKnightMove(alt.move) &&
      main.uci.exists(mu => alt.uci.exists(au => !NarrativeUtils.uciEquivalent(mu, au)))

  private def isKnightMove(move: String): Boolean =
    Option(move).getOrElse("").trim.startsWith("N")

  private def knightRouteShiftClaim(
    main: Option[CandidateInfo],
    candidate: CandidateInfo
  ): Option[String] = {
    val candUci = candidate.uci.getOrElse("")
    main.flatMap(_.uci).flatMap { mainUci =>
      Option.when(
        isKnightMove(candidate.move) &&
          main.exists(m => isKnightMove(m.move)) &&
          !NarrativeUtils.uciEquivalent(mainUci, candUci)
      ) {
        val candDestFile = Option.when(candUci.length >= 4)(candUci.charAt(2))
        val mainDestFile = Option.when(mainUci.length >= 4)(mainUci.charAt(2))
        val effects = scala.collection.mutable.ListBuffer[String]()
        if candDestFile.contains('e') || mainDestFile.contains('e') then effects += "c-pawn flexibility"
        if List(candDestFile, mainDestFile).flatten.exists(f => f == 'c' || f == 'd' || f == 'e' || f == 'f') then
          effects += "central tension timing"
        if List(candDestFile, mainDestFile).flatten.exists(f => f == 'f' || f == 'g' || f == 'h') then
          effects += "kingside safety tempo"
        val effectText =
          if effects.nonEmpty then effects.distinct.mkString(", ")
          else "piece-coordination timing"
        s"${candidate.move} selects a different knight route, shifting $effectText."
      }
    }
  }

  /**
   * B2/B6: ErrorClassification from counterfactual.
   * Classifies errors as tactical or positional based on:
   * - None: cpLoss < 50 (style difference, not worth classifying)
   * - Tactical: cpLoss >= 200 AND (severity is "blunder" or missedMotifs contain tactical patterns)
   * - Positional: cpLoss >= 50 but doesn't meet tactical threshold
   */
  private def buildErrorClassification(data: ExtendedAnalysisData): Option[ErrorClassification] = {
    data.counterfactual.flatMap { cf =>
      // Skip classification for minor style differences
      if (cf.cpLoss < 50) None
      else {
        // Tactical motifs (excludes generic Capture to prevent over-detection)
        val hasTacticalMissedMotif = cf.missedMotifs.exists {
          case _: Motif.Fork => true
          case _: Motif.Pin => true
          case _: Motif.Skewer => true
          case _: Motif.DiscoveredAttack => true
          case _: Motif.Check => true
          case _: Motif.DoubleCheck => true
          case c: Motif.Capture => c.captureType == Motif.CaptureType.Winning
          case _ => false
        }
        
        val isTactical = cf.cpLoss >= 200 && (cf.severity == "blunder" || hasTacticalMissedMotif)
        val missedMotifNames = cf.missedMotifs.map(_.getClass.getSimpleName.replace("$", ""))
        
        val errorSummary = if (isTactical) {
          s"전술(${cf.cpLoss}cp${if (missedMotifNames.nonEmpty) s", ${missedMotifNames.head}" else ""})"
        } else {
          s"포지셔널(${cf.cpLoss}cp)"
        }
        
        Some(ErrorClassification(
          isTactical = isTactical,
          missedMotifs = missedMotifNames,
          errorSummary = errorSummary
        ))
      }
    }
  }

  /**
   * B3: DivergePly from counterfactual analysis.
   * Populated only when user made a suboptimal move (counterfactual exists).
   */
  private def buildDivergence(data: ExtendedAnalysisData): Option[DivergenceInfo] = {
    data.counterfactual.map { cf =>
      DivergenceInfo(
        divergePly = data.ply,
        punisherMove = cf.userLine.moves.lift(1),  // Opponent's best reply to user's move
        branchPointFen = Some(data.fen)
      )
    }
  }

  /**
   * B1: ChoiceType from classification.choiceTopology
   * Reuses ContextHeader logic but returns enum for structured access.
   */
   private def buildChoiceType(ctx: IntegratedContext): ChoiceType = {
    ctx.classification.map { c =>
      c.choiceTopology.topologyType match {
        case ChoiceTopologyType.OnlyMove => ChoiceType.OnlyMove
        case ChoiceTopologyType.StyleChoice => ChoiceType.StyleChoice
        case ChoiceTopologyType.NarrowChoice => ChoiceType.NarrowChoice
      }
    }.getOrElse(ChoiceType.NarrowChoice)
  }

  // Valid chess square coordinates

  /**
   * B5: Clean Target Architecture
   * Separates Tactical (immediate) and Strategic (positional) targets.
   * Priority: 1 (Urgent/Threat) > 2 (High/Probe) > 3 (Normal/Heuristic).
   */
  private def buildTargets(data: ExtendedAnalysisData, ctx: IntegratedContext, probeResults: List[ProbeResult]): Targets = {

    // 1. Tactical Targets (Attack/Defend)
    val tacticalBuffer = scala.collection.mutable.ListBuffer[TargetEntry]()

    // ThreatsToThem -> Attack (Priority 1)
    ctx.threatsToThem.foreach { ta =>
      ta.threats.sortBy(-_.lossIfIgnoredCp).take(3).foreach { t =>
        t.attackSquares.foreach { sq =>
          tacticalBuffer += TargetEntry(TargetSquare(sq), s"Attack: ${t.kind}", ConfidenceLevel.Engine, 1)
        }
      }
    }
    // ThreatsToUs -> Defend (Priority 1)
    ctx.threatsToUs.foreach { ta =>
      ta.threats.sortBy(-_.lossIfIgnoredCp).take(3).foreach { t =>
        t.attackSquares.foreach { sq =>
          tacticalBuffer += TargetEntry(TargetSquare(sq), s"Defend: ${t.kind}", ConfidenceLevel.Engine, 1)
        }
      }
    }
    // Probe Refutations -> Tactical (Priority 2)
    probeResults.filter(pr => pr.deltaVsBaseline.abs >= 100).foreach { pr =>
      pr.probedMove.flatMap(chess.format.Uci.apply).foreach {
        case uci: chess.format.Uci.Move => 
          val sq = uci.dest.key
          tacticalBuffer += TargetEntry(TargetSquare(sq), s"Refuted: ${pr.deltaVsBaseline}cp", ConfidenceLevel.Probe, 2)
        case _ => // Ignore drops in standard chess
      }
    }

    // 2. Strategic Targets (Expansion/Control/Structure)
    val strategicBuffer = scala.collection.mutable.ListBuffer[TargetEntry]()

    // Outposts/Weakness from Semantic Data (Priority 3)
    data.positionalFeatures.foreach {
      case PositionalTag.Outpost(sq, _) => 
        strategicBuffer += TargetEntry(TargetSquare(sq.key), "Outpost", ConfidenceLevel.Heuristic, 3)
      case PositionalTag.OpenFile(f, _) =>
        strategicBuffer += TargetEntry(TargetFile(f.char.toString), "Open file control", ConfidenceLevel.Heuristic, 3)
      case PositionalTag.WeakSquare(sq, _) =>
        strategicBuffer += TargetEntry(TargetSquare(sq.key), "Weak square", ConfidenceLevel.Heuristic, 3)
      case _ => // ignore others for targets
    }
    
    data.structuralWeaknesses.foreach { wc =>
      wc.squares.foreach { sq =>
        strategicBuffer += TargetEntry(TargetSquare(sq.key), s"Weak complex (${wc.color})", ConfidenceLevel.Heuristic, 3)
      }
    }

    // PawnPlay -> Blockade/Tension
    ctx.pawnAnalysis.foreach { pa =>
      pa.blockadeSquare.foreach { sq =>
        strategicBuffer += TargetEntry(TargetSquare(sq.key), s"Blockade${pa.blockadeRole.map(r => s" by ${r.name}").getOrElse("")}", ConfidenceLevel.Heuristic, 3)
      }
      if (pa.pawnBreakReady && pa.breakImpact >= 200) {
        pa.breakFile.foreach { f =>
             strategicBuffer += TargetEntry(TargetFile(f), "Break leverage", ConfidenceLevel.Heuristic, 3)
        }
      }
    }

    // Plan Evidence Extraction (Priority 3)
    data.plans.foreach { pm =>
      pm.evidence.foreach { ev =>
        extractRefFromMotif(ev.motif).foreach { ref =>
          strategicBuffer += TargetEntry(ref, s"Plan: ${pm.plan.name}", ConfidenceLevel.Heuristic, 3)
        }
      }
    }

    // 3. Deduplicate and Finalize
    def finalizeTargets(buffer: Seq[TargetEntry], limit: Int): List[TargetEntry] = {
      buffer.groupBy(_.ref.label).values.map { entries =>
        entries.sortBy(_.priority).head // Keep highest priority for each ref
      }.toList.sortBy(_.priority).take(limit)
    }

    Targets(
      tactical = finalizeTargets(tacticalBuffer.toSeq, 5),
      strategic = finalizeTargets(strategicBuffer.toSeq, 5)
    )
  }

  /**
   * Helper to extract TargetRef from a Motif.
   */
  private def extractRefFromMotif(motif: Motif): Option[TargetRef] = motif match {
    case m: Motif.Pin => m.pinnedSq.map(sq => TargetPiece(m.pinnedPiece.name, sq.key)).orElse(m.pinningSq.map(sq => TargetSquare(sq.key)))
    case m: Motif.Skewer => m.frontSq.map(sq => TargetPiece(m.frontPiece.name, sq.key)).orElse(m.attackingSq.map(sq => TargetSquare(sq.key)))
    case m: Motif.DiscoveredAttack => m.targetSq.map(sq => TargetSquare(sq.key))
    case m: Motif.Battery => m.frontSq.map(sq => TargetSquare(sq.key))
    case m: Motif.Fork => Some(TargetSquare(m.square.key))
    case m: Motif.IsolatedPawn => Some(TargetFile(m.file.char.toString))
    case m: Motif.DoubledPieces => Some(TargetFile(m.file.char.toString))
    case m: Motif.OpenFileControl => Some(TargetFile(m.file.char.toString))
    case _ => None
  }

  /**
   * B8: PlanConcurrency from compatibilityEvents
   * Uses simple planName-based heuristic: if secondary was downweight/removed → conflict
   */
  private def buildPlanConcurrency(data: ExtendedAnalysisData): PlanConcurrency = {
    val primary = data.plans.headOption.map(_.plan.name).getOrElse("None")
    val secondary = data.plans.lift(1).map(_.plan.name)
    
    // Simple heuristic: if both plans share the same category, they synergize
    val relationship = (data.plans.headOption, data.plans.lift(1)) match {
      case (Some(p1), Some(p2)) if p1.plan.category == p2.plan.category => "↔ synergy"
      case (Some(_), Some(_)) => "independent"
      case _ => "independent"
    }

    PlanConcurrency(primary, secondary, relationship)
  }

  /**
   * Builds SemanticSection from ExtendedAnalysisData semantic fields.
   * Returns None if no meaningful semantic data exists.
   */
  private def buildSemanticSection(data: ExtendedAnalysisData): Option[SemanticSection] = {
    val isPawnEndgame = data.phase == "endgame" && 
      data.toContext.features.exists(f => (f.materialPhase.whiteMaterial + f.materialPhase.blackMaterial) <= 6)
    
    val filteredPositional = if (isPawnEndgame) {
      data.positionalFeatures.filterNot {
        case _: lila.llm.model.strategic.PositionalTag.OpenFile => true
        case _ => false
      }
    } else data.positionalFeatures

    val hasWeaknesses = data.structuralWeaknesses.nonEmpty
    val hasActivity = data.pieceActivity.nonEmpty
    val hasPositional = filteredPositional.nonEmpty
    val hasCompensation = data.compensation.isDefined
    val hasEndgame = data.endgameFeatures.isDefined
    val hasPractical = data.practicalAssessment.isDefined
    val hasPrevented = data.preventedPlans.nonEmpty
    val hasConcepts = data.conceptSummary.nonEmpty
    val hasStructureProfile = data.structureProfile.isDefined
    val hasPlanAlignment = data.planAlignment.isDefined

    if (!hasWeaknesses && !hasActivity && !hasPositional && !hasCompensation && 
        !hasEndgame && !hasPractical && !hasPrevented && !hasConcepts && !hasStructureProfile && !hasPlanAlignment) None
    else Some(SemanticSection(
      structuralWeaknesses = data.structuralWeaknesses.map(convertWeakComplex),
      pieceActivity = data.pieceActivity.map(convertPieceActivity),
      positionalFeatures = filteredPositional.map(convertPositionalTag),
      compensation = data.compensation.map(convertCompensation),
      endgameFeatures = data.endgameFeatures.map(convertEndgame),
      practicalAssessment = data.practicalAssessment.map(convertPractical),
      preventedPlans = data.preventedPlans.map(convertPreventedPlan),
      conceptSummary = data.conceptSummary,
      structureProfile = data.structureProfile.map(convertStructureProfile),
      planAlignment = data.planAlignment.map(convertPlanAlignment)
    ))
  }

  private def convertStructureProfile(sp: lila.llm.model.structure.StructureProfile): StructureProfileInfo =
    StructureProfileInfo(
      primary = sp.primary.toString,
      confidence = sp.confidence,
      alternatives = sp.alternatives.map(_.toString),
      centerState = sp.centerState.toString,
      evidenceCodes = sp.evidenceCodes
    )

  private def convertPlanAlignment(pa: lila.llm.model.structure.PlanAlignment): PlanAlignmentInfo =
    PlanAlignmentInfo(
      score = pa.score,
      band = pa.band.toString,
      matchedPlanIds = pa.matchedPlanIds,
      missingPlanIds = pa.missingPlanIds,
      reasonCodes = pa.reasonCodes,
      narrativeIntent = pa.narrativeIntent,
      narrativeRisk = pa.narrativeRisk,
      reasonWeights = pa.reasonWeights
    )

  private def convertWeakComplex(wc: WeakComplex): WeakComplexInfo = {
    val owner = wc.color.name.capitalize
    val distinctColors = wc.squares.map(_.isLight).distinct
    val squareColor =
      distinctColors match
        case Nil          => "mixed"
        case true :: Nil  => "light"
        case false :: Nil => "dark"
        case _            => "mixed"
    WeakComplexInfo(
      owner = owner,
      squareColor = squareColor,
      squares = wc.squares.map(_.key),
      isOutpost = wc.isOutpost,
      cause = wc.cause
    )
  }

  private def convertPieceActivity(pa: PieceActivity): PieceActivityInfo = {
    PieceActivityInfo(
      piece = pa.piece.name,
      square = pa.square.key,
      mobilityScore = pa.mobilityScore,
      isTrapped = pa.isTrapped,
      isBadBishop = pa.isBadBishop,
      keyRoutes = pa.keyRoutes.map(_.key),
      coordinationLinks = pa.coordinationLinks.map(_.key)
    )
  }

  private def calculateDecisionRationale(
      data: ExtendedAnalysisData,
      ctx: IntegratedContext,
      candidates: List[CandidateInfo],
      semantic: Option[SemanticSection],
      targets: Targets,
      probeResults: List[ProbeResult]
  ): DecisionRationale = {
    val bestMove = candidates.headOption
    val bestProbe = bestMove.flatMap(c => probeResults.find(_.probedMove == c.uci))
    
    val delta = buildPVDelta(ctx, bestProbe)
    val focalPoint = identifyFocalPoint(targets, semantic, data.plans)
    
    // Phase F Refinement: Conservative logicSummary when probe data is unavailable
    val (logicSummary, confidence) = if (bestProbe.isDefined) {
      val solve = if (delta.resolvedThreats.nonEmpty) s"Resolves ${delta.resolvedThreats.head}" else "Maintains position"
      val gain = if (delta.newOpportunities.nonEmpty) s"creates pressure on ${delta.newOpportunities.head}" else "improves piece coordination"
      (s"$solve -> $gain", ConfidenceLevel.Probe)
    } else {
      // No probe data: don't overstate claims
      val heuristic = focalPoint.map(fp => s"Focus on ${fp.label}").getOrElse("General improvement")
      (s"$heuristic (probe needed for validation)", ConfidenceLevel.Heuristic)
    }

    DecisionRationale(
      focalPoint = focalPoint,
      logicSummary = logicSummary,
      delta = delta,
      confidence = confidence
    )
  }

  /**
   * F1/P1: Compare current threats with PV1 future threats.
   * Uses FutureSnapshot when available for accurate comparison.
   * Falls back to L1DeltaSnapshot heuristics otherwise.
   * Returns empty PVDelta when bestProbe is None.
   */
  private def buildPVDelta(ctx: IntegratedContext, bestProbe: Option[ProbeResult]): PVDelta = {
    bestProbe match {
      case None =>
        // No probe: return empty delta to prevent overconfident assertions
        PVDelta(Nil, Nil, Nil, Nil)
        
      case Some(probe) if probe.futureSnapshot.isDefined =>
        // P1: Use structured FutureSnapshot for accurate comparison
        val fs = probe.futureSnapshot.get
        val targetsDelta = fs.targetsDelta
        
        // Opportunities: new targets created
        val newOps = (targetsDelta.tacticalAdded ++ targetsDelta.strategicAdded).take(2)
        
        // Plan advancements from blockers/prereqs
        val planAdv = (fs.planBlockersRemoved.map(b => s"Removed: $b") ++ 
                       fs.planPrereqsMet.map(p => s"Met: $p")).take(2)
        
        PVDelta(
          resolvedThreats = fs.resolvedThreatKinds.take(2),
          newOpportunities = if (newOps.nonEmpty) newOps else List("Improved position"),
          planAdvancements = planAdv,
          concessions = fs.newThreatKinds.take(1)
        )
        
      case Some(probe) =>
        // Fallback: use L1DeltaSnapshot heuristics (legacy behavior)
        val currentToUs = ctx.threatsToUs.map(_.threats.map(_.kind.toString)).getOrElse(Nil)
        val futureMotifs = probe.keyMotifs
        
        val resolved = currentToUs.filterNot(futureMotifs.contains)
        val concessions = futureMotifs.filter(m => m == "Mate" || m == "Material")
        
        val newOps = probe.l1Delta.map { d =>
          if (d.materialDelta > 0 || d.mobilityDelta > 10) List("Positional advantage") else Nil
        }.getOrElse(Nil)

        PVDelta(
          resolvedThreats = resolved.take(2),
          newOpportunities = newOps,
          planAdvancements = Nil,
          concessions = concessions.take(1)
        )
    }
  }

  /**
   * F2: Score and select FocalPoint from intersection of Targets ∩ Semantic ∩ Plans.
   */
  private def identifyFocalPoint(
      targets: Targets,
      semantic: Option[SemanticSection],
      plans: List[PlanMatch]
  ): Option[TargetRef] = {
    val allTargets = targets.tactical ++ targets.strategic
    if (allTargets.isEmpty) return None

    // Scoring: appearance in semantic features + appearance in plan supports
    val scores = allTargets.map { t =>
      var score = t.priority match { case 1 => 5; case 2 => 3; case _ => 1 }
      
      // Match square in semantic
      semantic.foreach { s =>
        if (s.positionalFeatures.exists(_.square.contains(t.ref.label))) score += 2
        if (s.pieceActivity.exists(_.square == t.ref.label)) score += 2
      }
      
      // Match in plans
      plans.foreach { pm =>
        if (pm.supports.exists(_.contains(t.ref.label))) score += 1
      }
      
      (t.ref, score)
    }

    scores.sortBy(-_._2).headOption.map(_._1)
  }

  private def convertPositionalTag(pt: PositionalTag): PositionalTagInfo = {
    pt match {
      case PositionalTag.Outpost(sq, c) =>
        PositionalTagInfo("Outpost", Some(sq.key), None, c.name)
      case PositionalTag.OpenFile(f, c) =>
        PositionalTagInfo("OpenFile", None, Some(f.char.toString), c.name)
      case PositionalTag.WeakSquare(sq, c) =>
        PositionalTagInfo("WeakSquare", Some(sq.key), None, c.name)
      case PositionalTag.LoosePiece(sq, _, c) =>
        PositionalTagInfo("LoosePiece", Some(sq.key), None, c.name)
      case PositionalTag.WeakBackRank(c) =>
        PositionalTagInfo("WeakBackRank", None, None, c.name)
      case PositionalTag.BishopPairAdvantage(c) =>
        PositionalTagInfo("BishopPairAdvantage", None, None, c.name)
      case PositionalTag.BadBishop(c) =>
        PositionalTagInfo("BadBishop", None, None, c.name)
      case PositionalTag.GoodBishop(c) =>
        PositionalTagInfo("GoodBishop", None, None, c.name)
      case PositionalTag.RookOnSeventh(c) =>
        PositionalTagInfo("RookOnSeventh", None, None, c.name)
      case PositionalTag.StrongKnight(sq, c) =>
        PositionalTagInfo("StrongKnight", Some(sq.key), None, c.name)
      case PositionalTag.SpaceAdvantage(c) =>
        PositionalTagInfo("SpaceAdvantage", None, None, c.name)
      case PositionalTag.OppositeColorBishops =>
        PositionalTagInfo("OppositeColorBishops", None, None, "Both")
      case PositionalTag.KingStuckCenter(c) =>
        PositionalTagInfo("KingStuckCenter", None, None, c.name)
      case PositionalTag.ConnectedRooks(c) =>
        PositionalTagInfo("ConnectedRooks", None, None, c.name)
      case PositionalTag.DoubledRooks(f, c) =>
        PositionalTagInfo("DoubledRooks", None, Some(f.char.toString), c.name)
      case PositionalTag.ColorComplexWeakness(c, sqColor, sqs) =>
        PositionalTagInfo("ColorComplexWeakness", None, None, c.name, Some(s"$sqColor squares: ${sqs.map(_.key).mkString(",")}"))
      case PositionalTag.PawnMajority(c, flank, count) =>
        PositionalTagInfo("PawnMajority", None, None, c.name, Some(s"$flank $count pawns"))
      case PositionalTag.MinorityAttack(c, flank) =>
        PositionalTagInfo("MinorityAttack", None, None, c.name, Some(s"$flank attack"))
      // case PositionalTag.QueenActivity(c) =>
      //   PositionalTagInfo("QueenActivity", None, None, c.name)
      // case PositionalTag.QueenManeuver(c) =>
      //   PositionalTagInfo("QueenManeuver", None, None, c.name)
      case PositionalTag.MateNet(c) =>
        PositionalTagInfo("MateNet", None, None, c.name)
      // case PositionalTag.PerpetualCheck(c) =>
      //   PositionalTagInfo("PerpetualCheck", None, None, c.name)
      case PositionalTag.RemovingTheDefender(target, c) =>
        PositionalTagInfo("RemovingTheDefender", None, None, c.name, Some(target.name))
      case PositionalTag.Initiative(c) =>
        PositionalTagInfo("Initiative", None, None, c.name)
    }
  }

  private def convertCompensation(comp: Compensation): CompensationInfo = {
    CompensationInfo(
      investedMaterial = comp.investedMaterial,
      returnVector = comp.returnVector,
      expiryPly = comp.expiryPly,
      conversionPlan = comp.conversionPlan
    )
  }

  private def convertEndgame(ef: EndgameFeature): EndgameInfo = {
    EndgameInfo(
      hasOpposition = ef.hasOpposition,
      isZugzwang = ef.isZugzwang,
      keySquaresControlled = ef.keySquaresControlled.map(_.key),
      oppositionType = ef.oppositionType.toString,
      zugzwangLikelihood = ef.zugzwangLikelihood,
      ruleOfSquare = ef.ruleOfSquare.toString,
      triangulationAvailable = ef.triangulationAvailable,
      kingActivityDelta = ef.kingActivityDelta,
      rookEndgamePattern = ef.rookEndgamePattern.toString,
      theoreticalOutcomeHint = ef.theoreticalOutcomeHint.toString,
      confidence = ef.confidence,
      primaryPattern = ef.primaryPattern
    )
  }

  private def convertPractical(pa: PracticalAssessment): PracticalInfo = {
    PracticalInfo(
      engineScore = pa.engineScore,
      practicalScore = pa.practicalScore,
      verdict = pa.verdict
    )
  }

  private def convertPreventedPlan(pp: PreventedPlan): PreventedPlanInfo = {
    PreventedPlanInfo(
      planId = pp.planId,
      deniedSquares = pp.deniedSquares.map(_.key),
      breakNeutralized = pp.breakNeutralized,
      mobilityDelta = pp.mobilityDelta,
      preventedThreatType = pp.preventedThreatType
    )
  }

  /**
   * Builds opponent's top plan by calling PlanMatcher for the opposite side.
   * Shows "what the opponent wants to do" for organic narrative flow.
   */
  private def buildOpponentPlan(data: ExtendedAnalysisData, ctx: IntegratedContext): Option[PlanRow] = {
    val opponentColor = if (ctx.isWhiteToMove) chess.Color.Black else chess.Color.White
    val ctxOpp = buildOpponentContext(ctx)

    val planScoring = PlanMatcher.matchPlans(data.motifs, ctxOpp, opponentColor)
    planScoring.topPlans.headOption.map { p =>
      val isBlockadeEstablished = p.plan match {
        case _: lila.llm.model.Plan.Blockade =>
          // Blockade is "established" if passerBlockade is true for our side (opponent's passer is blocked)
          ctx.pawnAnalysis.exists(_.passerBlockade)
        case _ => false
      }
      
      PlanRow(
        rank = 1,
        name = p.plan.name,
        score = p.score,
        evidence = p.evidence.map(_.description),
        isEstablished = isBlockadeEstablished
      )
    }
  }


  /**
   * Swaps IntegratedContext POV for opponent plan matching.
   * Ensures threatsToUs/Them and pawnAnalysis/opponentPawnAnalysis are correctly attributed.
   */
  private def buildOpponentContext(ctx: IntegratedContext): IntegratedContext = {
    ctx.copy(
      threatsToUs = ctx.threatsToThem,
      threatsToThem = ctx.threatsToUs,
      pawnAnalysis = ctx.opponentPawnAnalysis,
      opponentPawnAnalysis = ctx.pawnAnalysis,
      isWhiteToMove = !ctx.isWhiteToMove
    )
  }
