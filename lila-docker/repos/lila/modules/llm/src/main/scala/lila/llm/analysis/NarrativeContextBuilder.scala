package lila.llm.analysis

import chess.{ Color, Square }
import lila.llm.model._
import lila.llm.model.strategic._
import lila.llm.analysis.L3._

/**
 * Phase 6: NarrativeContext Builder
 * 
 * Converts existing analysis results into hierarchical NarrativeContext.
 */
object NarrativeContextBuilder:

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
    // Phase 2: We can now probe from non-root FENs (e.g., after playedMove for recapture branching).
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
    val plans = buildPlanTable(data, ctx)
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
        .map(after => buildDelta(data, after, afterPhaseTrigger))

    val prevDelta = prevAnalysis.map(prev => buildDelta(prev, data, phase.transitionTrigger))
    val delta = afterDelta.orElse(prevDelta)
    
    // B7: Enriched candidates with probe results
    val candidates = buildCandidatesEnriched(data, ctx, rootProbeResults)

    val playedSan = data.prevMove.flatMap { uci =>
      NarrativeUtils.uciListToSan(data.fen, List(uci)).headOption
    }

    // Phase 1: AuthorQuestions (used for Phase 2 evidence planning as well)
    val authorQuestions =
      AuthorQuestionGenerator.generate(
        data = data,
        ctx = ctx,
        candidates = candidates,
        playedSan = playedSan
      )
    
    // B5: Build targets early for use in meta and decision
    val targets = buildTargets(data, ctx, rootProbeResults)
    
    // Phase 6.5: Evidence Augmentation (Probe Loop)
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
          candidates = candidates,
          authorQuestions = authorQuestions
        )
        .distinctBy(_.id)
    
    // B-axis: Meta signals (Step 1-3)
    // Only populate meta if we have meaningful source data
    val meta = buildMetaSignals(data, ctx, targets, rootProbeResults)

    val strategicFlow = data.planSequence.flatMap { seq =>
      seq.previousPlan.flatMap { prev =>
        val curr = seq.currentPlans.primary.plan
        if (prev.id != curr.id) {
           Some(TransitionAnalyzer.explainTransition(
             prev, 
             curr, 
             data.toContext,
             isTacticalThreatToUs = Some(data.tacticalThreatToUs),
             isTacticalThreatToThem = Some(data.tacticalThreatToThem),
             phase = Some(data.phase)
           ))
        } else None
      }
    }

    // Phase A: Semantic section from ExtendedAnalysisData
    val semantic = buildSemanticSection(data)

    // Phase B: Opponent plan (side=!toMove)
    val opponentPlan = buildOpponentPlan(data, ctx)


    // Phase F: Decision Rationale (Synthesis)
    val decision = if (header.choiceType != "StyleChoice") {
      Some(calculateDecisionRationale(data, ctx, candidates, semantic, targets, rootProbeResults))
    } else None

    // Phase A9: Opening Event Detection (event-driven, not per-move)
    val openingEvent = detectOpeningEvent(data, openingRef, decision, openingBudget, prevOpeningRef)
    
    // Compute updated budget based on fired event
    val updatedBudget = openingEvent match {
      case Some(OpeningEvent.Intro(_, _, _, _)) => openingBudget.afterIntro
      case Some(OpeningEvent.TheoryEnds(_, _)) => openingBudget.afterTheoryEnds
      case Some(_) => openingBudget.afterEvent  // BranchPoint, OutOfBook, Novelty
      case None => openingBudget.updatePly(data.ply)
    }

    val authorEvidence =
      AuthorEvidenceBuilder.build(
        fen = data.fen,
        ply = data.ply,
        playedMove = data.prevMove,
        bestMove = candidates.headOption.flatMap(_.uci),
        authorQuestions = authorQuestions,
        probeResults = probeResults
      )

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
      snapshots = List(l1),
      delta = delta,
      phase = phase,
      candidates = candidates,
      authorQuestions = authorQuestions,
      authorEvidence = authorEvidence,
      facts = FactExtractor.fromMotifs(board, data.motifs, FactScope.Now) ++ 
              FactExtractor.extractStaticFacts(board, color) ++
              FactExtractor.extractEndgameFacts(board, color),
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
    if (data.phase != "opening") return None
    
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

  // ============================================================
  // HEADER
  // ============================================================
  
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

  // ============================================================
  // SUMMARY (5 lines)
  // ============================================================
  
  private def buildSummary(
    data: ExtendedAnalysisData,
    ctx: IntegratedContext
  ): NarrativeSummary = {
    val primaryPlan = data.plans.headOption
      .map(p => s"${p.plan.name} (${f"${p.score}%.2f"})")
      .getOrElse("No clear plan")
    
    // Phase 23: Strict Fact-Gating for Threats
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
    // P10 Issue 1: Also check alternatives for positive mate (mate FOR us)
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
    
    // P10 Issue 1: Mate Override - forced mate takes precedence over heuristic plans
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

  // ============================================================
  // THREAT TABLE
  // ============================================================
  
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
      // Item 3 Fix: Check against both SAN and UCI for robust linking
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

  // ============================================================
  // PAWN PLAY TABLE
  // ============================================================
  
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

  // ============================================================
  // PLAN TABLE
  // ============================================================
  
  private def buildPlanTable(
    data: ExtendedAnalysisData,
    ctx: IntegratedContext
  ): PlanTable = {
    // P10 Issue 1: Check for forced mate
    val hasMateCandidate = data.candidates.exists { cand =>
      cand.line.mate.isDefined || NarrativeUtils.uciListToSan(data.fen, cand.line.moves.take(1)).exists(_.contains("#"))
    }
    val hasForcedMateForUs = data.alternatives.exists(_.mate.exists(_ > 0)) || hasMateCandidate
    
    val basePlans = data.plans.take(5).zipWithIndex.map { case (pm, idx) =>
      PlanRow(
        rank = idx + 1,
        name = pm.plan.name,
        score = pm.score,
        evidence = pm.evidence.take(2).map(_.description),
        supports = pm.supports,
        blockers = pm.blockers,
        missingPrereqs = pm.missingPrereqs
      )
    }
    
    // P10 Issue 1: Inject Forced Mate plan at rank 1 when applicable
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
    
    // A4: Track suppressed/removed plans from PlanMatcher trace
    val suppressed = data.planSequence.map(_.currentPlans.compatibilityEvents).getOrElse(Nil)
      .collect {
        case e if e.eventType == "removed" || e.eventType == "downweight" =>
          SuppressedPlan(
            name = e.planName,
            originalScore = e.originalScore,
            reason = e.reason,
            isRemoved = e.eventType == "removed"
          )
      }.distinctBy(_.name)
    
    PlanTable(top5 = top5, suppressed = suppressed)
  }

  // ============================================================
  // L1 SNAPSHOT
  // ============================================================
  
  private def buildL1Snapshot(ctx: IntegratedContext): L1Snapshot = {
    ctx.features match {
      case Some(f) =>
        val isWhite = ctx.isWhiteToMove
        val mp = f.materialPhase
        val ks = f.kingSafety
        val act = f.activity
        val lc = f.lineControl
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
        
        // P10 Issue 3: Phase-aware filtering for L1 Snapshot
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


  // ============================================================
  // CANDIDATES
  // ============================================================
  
  private def buildCandidates(data: ExtendedAnalysisData): List[CandidateInfo] = {
    data.candidates.zipWithIndex.map { case (cand, idx) =>
      val bestScore = data.candidates.headOption.map(_.score).getOrElse(0)
      val secondScore = data.candidates.lift(1).map(_.score).getOrElse(bestScore)
      val bestGap = bestScore - secondScore
      val cpDiff = bestScore - cand.score
      val annotation =
        if idx == 0 then
          // Avoid overusing "!"; only mark when the best move is clearly separated.
          if bestGap >= Thresholds.MISTAKE_CP then "!" else ""
        else if cpDiff >= Thresholds.DOUBLE_QUESTION_CP then "??"
        else if cpDiff >= Thresholds.SINGLE_QUESTION_CP then "?"
        else if cpDiff >= Thresholds.DUBIOUS_CP then "?!"
        else ""
      
      // A7: SAN conversion
      val sanMoves = NarrativeUtils.uciListToSan(data.fen, cand.line.moves.take(2))
      val moveSan = sanMoves.headOption.getOrElse(cand.move)
      
      // A7: Tactical alerts (2rd move in line = opponent response)
      val responseMotifs = cand.motifs.filter(_.plyIndex == 1)
      val alert = responseMotifs.collectFirst {
        case m: Motif.Check => s"allows ${sanMoves.lift(1).getOrElse("")} check"
        case m: Motif.Fork => s"allows ${sanMoves.lift(1).getOrElse("")} fork"
        case m: Motif.Capture if m.captureType == Motif.CaptureType.Winning => 
          s"allows ${sanMoves.lift(1).getOrElse("")} winning capture"
        case _: Motif.DiscoveredAttack => "reveals discovered attack"
      }

      // Item 2: Self-evidence for tactical claims (plyIndex == 0)
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
          // Phase 21.1: Positional Reasoning
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
          case sm: Motif.SmotheredMate =>
            Some(s"SmotheredMate(Knight)")
          case xr: Motif.XRay =>
            Some(s"XRay(${xr.piece} through to ${xr.target} on ${xr.square})")
          case _ => None
        }
      }

      // Phase 22.5: Use dual intent (immediate + downstream)
      val alignment = cand.moveIntent.immediate
      val downstream = cand.moveIntent.downstream
      
      // A7: Enriched whyNot
      val whyNot = if (idx > 0 && cand.score < bestScore - 50) {
        val diff = (bestScore - cand.score) / 100.0
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
   * Phase F: Maps VariationTag enum to CandidateTag.
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

  // ============================================================
  // DEFAULTS
  // ============================================================
  
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

  // ============================================================
  // A5: OPEN FILES COMPUTATION
  // ============================================================
  
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

  // ============================================================
  // PHASE CONTEXT (A8)
  // ============================================================
  
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

  // ============================================================
  // DELTA (A6)
  // ============================================================

  private def buildDelta(
    prev: ExtendedAnalysisData,
    current: ExtendedAnalysisData,
    phaseTrigger: Option[String] = None
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
      phaseChange = phaseTrigger
    )
  }

  // ============================================================
  // META SIGNALS (B-axis)
  // ============================================================

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
   * Phase C: Enhanced with L1 delta collapse reasons.
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

  private def buildCandidatesEnriched(data: ExtendedAnalysisData, ctx: IntegratedContext, probeResults: List[ProbeResult]): List[CandidateInfo] = {
    val isWhiteToMove = data.isWhiteToMove
    val existing = buildCandidates(data)
    val existingUcis = existing.flatMap(_.uci).toSet
    
    val enriched = existing.map { c =>
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

      c.copy(
        whyNot = whyNot.orElse(c.whyNot),
        tags = (c.tags ++ probeTags).distinct,
        probeLines = probeLines
      )
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
  private val ValidFiles = Set('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h')
  private val ValidRanks = Set('1', '2', '3', '4', '5', '6', '7', '8')

  /**
   * B5: Clean Target Architecture
   * Separates Tactical (immediate) and Strategic (positional) targets.
   * Priority: 1 (Urgent/Threat) > 2 (High/Probe) > 3 (Normal/Heuristic).
   */
  private def buildTargets(data: ExtendedAnalysisData, ctx: IntegratedContext, probeResults: List[ProbeResult]): Targets = {
    import lila.llm.model.{TargetEntry, TargetSquare, TargetFile, TargetPiece}

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
    case m: Motif.IsolatedPawn => Some(TargetFile(m.file.char.toString))  // FIX: Use .char for letter
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
    
    val events = data.planSequence.map(_.currentPlans.compatibilityEvents).getOrElse(Nil)
    
    // Check if secondary plan was affected by compatibility
    val relationship = secondary.flatMap { secName =>
      events.find(_.planName == secName).map { e =>
        if e.eventType == "boosted" then "↔ synergy"
        else if e.eventType == "downweight" || e.eventType == "removed" then "⟂ conflict"
        else "independent"
      }
    }.getOrElse("independent")

    PlanConcurrency(primary, secondary, relationship)
  }

  // ============================================================
  // SEMANTIC SECTION (Phase A)
  // ============================================================

  /**
   * Builds SemanticSection from ExtendedAnalysisData semantic fields.
   * Returns None if no meaningful semantic data exists.
   */
  private def buildSemanticSection(data: ExtendedAnalysisData): Option[SemanticSection] = {
    // P10 Issue 3: Suspend irrelevant features in pawn endgames
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

    if (!hasWeaknesses && !hasActivity && !hasPositional && !hasCompensation && 
        !hasEndgame && !hasPractical && !hasPrevented && !hasConcepts) None
    else Some(SemanticSection(
      structuralWeaknesses = data.structuralWeaknesses.map(convertWeakComplex),
      pieceActivity = data.pieceActivity.map(convertPieceActivity),
      positionalFeatures = filteredPositional.map(convertPositionalTag),
      compensation = data.compensation.map(convertCompensation),
      endgameFeatures = data.endgameFeatures.map(convertEndgame),
      practicalAssessment = data.practicalAssessment.map(convertPractical),
      preventedPlans = data.preventedPlans.map(convertPreventedPlan),
      conceptSummary = data.conceptSummary
    ))
  }

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

  // ============================================================
  // PHASE F: DECISION RATIONALE LOGIC
  // ============================================================

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
      // Phase 29: Queen and Tactical motifs
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
      keySquaresControlled = ef.keySquaresControlled.map(_.key)
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

  // ============================================================
  // OPPONENT PLAN (Phase B)
  // ============================================================

  /**
   * Builds opponent's top plan by calling PlanMatcher for the opposite side.
   * Shows "what the opponent wants to do" for organic narrative flow.
   */
  private def buildOpponentPlan(data: ExtendedAnalysisData, ctx: IntegratedContext): Option[PlanRow] = {
    val opponentColor = if (ctx.isWhiteToMove) chess.Color.Black else chess.Color.White
    val ctxOpp = buildOpponentContext(ctx)

    val planScoring = PlanMatcher.matchPlans(data.motifs, ctxOpp, opponentColor)
    planScoring.topPlans.headOption.map { p =>
      // P10 Issue 2: Check if this plan represents an already-established fact
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
