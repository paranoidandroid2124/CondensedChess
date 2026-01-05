package lila.llm.analysis

import chess.Color
import lila.llm.model._
import lila.llm.model.strategic._
import lila.llm.analysis.L3._
import lila.llm.analysis.PlanMatcher.ActivePlans

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
    probeResults: List[ProbeResult] = Nil // NEW: for B7 Why-not mapping
  ): NarrativeContext = {
    val header = buildHeader(ctx)
    val summary = buildSummary(data, ctx)
    val threats = buildThreatTable(ctx)
    val pawnPlay = buildPawnPlayTable(ctx)
    val plans = buildPlanTable(data, ctx)
    val l1 = buildL1Snapshot(ctx)
    val phase = buildPhaseContext(ctx, prevAnalysis)
    val delta = prevAnalysis.map(prev => buildDelta(prev, data, phase.transitionTrigger))
    
    // B7: Enriched candidates with probe results
    val candidates = buildCandidatesEnriched(data, probeResults)
    
    // Phase 6.5: Evidence Augmentation (Probe Loop)
    val planScoring = PlanScoringResult(
      topPlans = data.plans,
      confidence = 0.0, // fallback
      phase = ctx.phase
    )
    val multiPv = data.alternatives.map(v => PvLine(v.moves, v.scoreCp, v.mate, 0))
    val probeRequests = ProbeDetector.detect(ctx, planScoring, multiPv, data.fen)
    
    // B-axis: Meta signals (Step 1-3)
    // Only populate meta if we have meaningful source data
    val meta = buildMetaSignals(data, ctx, probeResults)

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

    NarrativeContext(
      header = header,
      summary = summary,
      threats = threats,
      pawnPlay = pawnPlay,
      plans = plans,
      l1Snapshot = l1,
      delta = delta,
      phase = phase,
      candidates = candidates,
      probeRequests = probeRequests,
      meta = meta,
      strategicFlow = strategicFlow
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
    
    val keyThreat = ctx.threatsToUs.flatMap { ta =>
      ta.threats.headOption.map { t =>
        val urgency = if (t.lossIfIgnoredCp >= 300) "must defend" else "consider"
        s"${t.kind} on ${t.attackSquares.headOption.getOrElse("?")} ($urgency)"
      }
    }
    
    val choiceType = ctx.classification
      .map(c => c.choiceTopology.topologyType.toString)
      .getOrElse("Unknown")
    
    val tensionPolicy = ctx.pawnAnalysis
      .map(p => s"${p.tensionPolicy} (${p.primaryDriver})")
      .getOrElse("N/A")
    
    val evalDelta = data.practicalAssessment
      .map(pa => f"Eval: ${pa.engineScore / 100.0}%.1f")
      .getOrElse("No eval")
    
    NarrativeSummary(
      primaryPlan = primaryPlan,
      keyThreat = keyThreat,
      choiceType = choiceType,
      tensionPolicy = tensionPolicy,
      evalDelta = evalDelta
    )
  }

  // ============================================================
  // THREAT TABLE
  // ============================================================
  
  private def buildThreatTable(ctx: IntegratedContext): ThreatTable = {
    val toUs = ctx.threatsToUs.map(ta => buildThreatRows(ta)).getOrElse(Nil)
    val toThem = ctx.threatsToThem.map(ta => buildThreatRows(ta)).getOrElse(Nil)
    ThreatTable(toUs = toUs, toThem = toThem)
  }
  
  private def buildThreatRows(ta: ThreatAnalysis): List[ThreatRow] = {
    ta.threats.take(3).map { t =>
      ThreatRow(
        kind = t.kind.toString,  // "Mate" | "Material" | "Positional"
        square = t.attackSquares.headOption,
        lossIfIgnoredCp = t.lossIfIgnoredCp,
        turnsToImpact = t.turnsToImpact,
        bestDefense = t.bestDefense,
        defenseCount = t.defenseCount,
        insufficientData = ta.insufficientData  // FIX: Use actual ThreatAnalysis flag
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
    val top5 = data.plans.take(5).zipWithIndex.map { case (pm, idx) =>
      PlanRow(
        rank = idx + 1,
        name = pm.plan.name,
        score = pm.score,
        evidence = pm.evidence.take(2).map(_.description)
      )
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
        
        L1Snapshot(
          material = materialStr,
          imbalance = imbalanceStr,
          kingSafetyUs = kingSafetyUs,
          kingSafetyThem = kingSafetyThem,
          mobility = mobilityStr,
          centerControl = centerStr,
          openFiles = openFilesList
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
      val annotation = if (idx == 0) "!" 
                       else if (cand.score < bestScore - 300) "??"
                       else if (cand.score < bestScore - 100) "?"
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
      
      // A7: Enriched whyNot
      val whyNot = if (idx > 0 && cand.score < bestScore - 50) {
        val diff = (bestScore - cand.score) / 100.0
        val refutation = sanMoves.lift(1).map(m => s" after $m").getOrElse("")
        val reason = if (diff > 2.0) "decisive loss" else if (diff > 0.8) "significant disadvantage" else "slight inaccuracy"
        Some(f"$reason ($diff%.1f)$refutation")
      } else None

      CandidateInfo(
        move = moveSan,
        uci = Some(cand.move),
        annotation = annotation,
        planAlignment = cand.futureContext,
        tacticalAlert = alert,
        practicalDifficulty = if (cand.line.moves.length > 6) "complex" else "clean",
        whyNot = whyNot
      )
    }
  }

  // ============================================================
  // DEFAULTS
  // ============================================================
  
  private val defaultClassification = PositionClassification(
    nature = NatureResult(NatureType.Static, 0, 0, 0, false),
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
      openFileCreated = None,
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
  private def buildMetaSignals(data: ExtendedAnalysisData, ctx: IntegratedContext, probeResults: List[ProbeResult]): Option[MetaSignals] = {
    // Require at least classification OR threats OR plans to populate meta
    val hasClassification = ctx.classification.isDefined
    val hasThreats = ctx.threatsToUs.isDefined || ctx.threatsToThem.isDefined
    val hasPlans = data.plans.nonEmpty
    val hasProbeResults = probeResults.nonEmpty
    
    if (!hasClassification && !hasThreats && !hasPlans && !hasProbeResults) None
    else {
      val choiceType = buildChoiceType(ctx)
      val targets = buildTargets(ctx)
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
   */
  private def buildWhyNotSummary(probeResults: List[ProbeResult], isWhiteToMove: Boolean): Option[String] = {
    if (probeResults.isEmpty) None
    else {
      val summaries = probeResults.flatMap { pr =>
        val moverLoss = if (isWhiteToMove) -pr.deltaVsBaseline else pr.deltaVsBaseline
        if (moverLoss >= 200) {
          val moveText = pr.probedMove.getOrElse("this move")
          Some(s"'$moveText' is refuted losing $moverLoss cp")
        } else None
      }
      if (summaries.isEmpty) None else Some(summaries.mkString(". "))
    }
  }

  private def buildCandidatesEnriched(data: ExtendedAnalysisData, probeResults: List[ProbeResult]): List[CandidateInfo] = {
    val isWhiteToMove = data.isWhiteToMove
    val existing = buildCandidates(data)
    val existingUcis = existing.flatMap(_.uci).toSet
    
    val enriched = existing.map { c =>
      probeResults.find(pr => pr.probedMove == c.uci).flatMap { pr =>
        val moverLoss = if (isWhiteToMove) -pr.deltaVsBaseline else pr.deltaVsBaseline
        if (moverLoss >= 50) {
           val bestReply = pr.bestReplyPv.headOption.getOrElse("?")
           // Try to get SAN for the reply
           val replySan = c.uci.map(u => NarrativeUtils.uciListToSan(data.fen, List(u, bestReply)).lift(1).getOrElse(bestReply)).getOrElse(bestReply)
           Some(s"inferior by $moverLoss cp after $replySan")
        } else None
      }.map(w => c.copy(whyNot = Some(w))).getOrElse(c)
    }
    
    val ghosts = probeResults.filter(pr => pr.probedMove.isDefined && !pr.probedMove.exists(existingUcis.contains)).map { pr =>
      val uci = pr.probedMove.get
      val san = NarrativeUtils.uciListToSan(data.fen, List(uci)).headOption.getOrElse(uci)
      val moverLoss = if (isWhiteToMove) -pr.deltaVsBaseline else pr.deltaVsBaseline
      val bestReply = pr.bestReplyPv.headOption.getOrElse("?")
      val replySan = NarrativeUtils.uciListToSan(data.fen, List(uci, bestReply)).lift(1).getOrElse(bestReply)

      CandidateInfo(
        move = san,
        uci = Some(uci),
        annotation = if (moverLoss >= 200) "??" else "?",
        planAlignment = "Alt Plan",
        tacticalAlert = None,
        practicalDifficulty = "complex",
        whyNot = Some(s"refuted by $replySan (-$moverLoss cp)")
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
   * B5: Targets from threatAnalysis
   * Extracts attack/defend squares with strict validation (a-h, 1-8).
   * Sorts by lossIfIgnoredCp descending to preserve highest-threat reasons.
   */
  private def buildTargets(ctx: IntegratedContext): Targets = {
    def extractTargets(ta: Option[ThreatAnalysis]): List[(String, String)] = {
      ta.toList.flatMap(_.threats)
        .sortBy(-_.lossIfIgnoredCp) // Higher threat first
        .flatMap { t =>
          val reason = s"${t.kind}/${t.lossIfIgnoredCp}cp/${t.turnsToImpact}t"
          t.attackSquares
            .filter(sq => sq.length == 2 && ValidFiles.contains(sq(0)) && ValidRanks.contains(sq(1)))
            .map(sq => (sq, reason))
        }
        .distinctBy(_._1) // Now preserves highest-threat reason
        .take(3)
    }

    Targets(
      attackTargets = extractTargets(ctx.threatsToThem),
      defendTargets = extractTargets(ctx.threatsToUs)
    )
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
