package chess
package analysis

import AnalysisModel.*
// PhaseClassifier removed - using inline stub until full TransitionTagger migration

object CriticalDetector:
  def detectCritical(

      timeline: Vector[PlyOutput], 
      experimentRunner: ExperimentRunner, 
      config: EngineConfig, 
      llmRequestedPlys: Set[Int], 
      onProgress: (AnalysisStage.AnalysisStage, Double) => Unit = (_, _) => (),
      criticalConfig: CriticalConfig = CriticalConfig.default
  )(using scala.concurrent.ExecutionContext): scala.concurrent.Future[Vector[CriticalNode]] =
    
    val judgementBoost: Map[String, Double] = Map(
      "blunder" -> criticalConfig.blunderBoost,
      "mistake" -> criticalConfig.mistakeBoost,
      "inaccuracy" -> criticalConfig.inaccuracyBoost,
      "good" -> 0.0,
      "best" -> 0.0,
      "book" -> 0.0
    )

    // Helper to calc criticality score (Synchronous part)
    def criticalityScore(p: PlyOutput): (Double, Double, Option[String]) =
      val deltaScore = math.abs(p.deltaWinPct)
      val conceptJump = p.concepts.dynamic + p.concepts.tacticalDepth + p.concepts.blunderRisk
      val bestVsSecondGap = p.bestVsSecondGap.getOrElse {
        val top = p.evalBeforeDeep.lines.headOption
        val second = p.evalBeforeDeep.lines.drop(1).headOption
        (for t <- top; s <- second yield (t.winPct - s.winPct).abs).getOrElse(100.0)
      }
      val branchTension = math.max(0.0, 12.0 - bestVsSecondGap)
      
      // Use integrated TransitionTags from ConceptLabeler (Phase 4 integration)
      val transitionTags = p.conceptLabels.map(_.transitionTags).getOrElse(Nil)
      
      // Calculate scores based on tags
      val transitionScore = transitionTags.map {
         case ConceptLabeler.TransitionTag.EndgameTransition => criticalConfig.dynamicCollapseScore
         case ConceptLabeler.TransitionTag.TacticalToPositional => criticalConfig.tacticalDryScore
         case ConceptLabeler.TransitionTag.FortressStructure => criticalConfig.fortressJumpScore
         case ConceptLabeler.TransitionTag.ComfortToUnpleasant => criticalConfig.comfortLossScore
         case ConceptLabeler.TransitionTag.PositionalSacrifice => criticalConfig.alphaZeroSpikeScore
         case ConceptLabeler.TransitionTag.KingExposed => criticalConfig.kingSafetyCollapseScore
         case ConceptLabeler.TransitionTag.ConversionDifficulty => criticalConfig.conversionIssueScore
      }.maxOption.getOrElse(0.0)
      
      val phaseLabel = transitionTags.headOption.map(_.toSnakeCase)

      val score =
        deltaScore * criticalConfig.deltaWeight +
          branchTension * criticalConfig.branchTensionWeight +
          judgementBoost.getOrElse(p.judgement, 0.0) +
          conceptJump * criticalConfig.conceptJumpWeight +
          transitionScore * criticalConfig.phaseShiftWeight
      (score, bestVsSecondGap, phaseLabel)

    val scored = timeline.map { p =>
      val (s, gap, phaseLabel) = criticalityScore(p)
      val llmBoost = if llmRequestedPlys.contains(p.ply.value) then criticalConfig.llmBoost else 0.0
      (p, (s + llmBoost, gap, phaseLabel))
    }
    
    val cap = math.min(criticalConfig.maxCriticalNodes, math.max(criticalConfig.minCriticalNodes, criticalConfig.minCriticalNodes + timeline.size / criticalConfig.nodesPerMoves))
    val topNodes = scored.sortBy { case (_, (s, _, _)) => -s }.take(cap)
    val totalCritical = topNodes.size

    // Asynchronous enrichment for top nodes
    scala.concurrent.Future.sequence(
      topNodes.zipWithIndex.map { case ((ply, (_, gap, phaseLabel)), idx) =>
        val prog = idx.toDouble / totalCritical
        onProgress(AnalysisStage.CRITICAL_DETECTION, prog)
        
        val isBlunderOrMistake = ply.judgement == "blunder" || ply.judgement == "mistake"
        
        // Use ExperimentRunner for extra analysis (Cached MultiPV)
        // If blunder/mistake, we specifically want TurningPointAnalysis (MultiPV=3 for before)
        val enrichmentF = 
           (if isBlunderOrMistake then
             experimentRunner.runTurningPointAnalysis(ply.fenBefore, ply.fen, ply.uci, depth = config.deepDepth + config.extraDepthDelta)
               .map(tp => (tp.evalBefore.lines, Some(tp)))
           else
             // Just get MultiPV for the position to show alternatives
             experimentRunner.run(AnalysisTypes.ExperimentType.StructureAnalysis, ply.fenBefore, None, depth = config.deepDepth, multiPv = 3)
               .map(res => (res.eval.lines, None))
           ).recover { case e: Throwable =>
             System.err.println(s"[CriticalDetector] Enrichment failed for ply ${ply.ply.value}: ${e.getMessage}")
             (Nil, None) // Fallback: empty lines, no turning point
           }

        enrichmentF.map { case (extraLines, turningPoint) =>
           val reason =
             if ply.judgement == "blunder" then "ΔWin% blunder spike"
             else if ply.judgement == "mistake" then "ΔWin% mistake"
             else if phaseLabel.nonEmpty then s"phase shift: ${phaseLabel.get}"
             else if gap <= 5.0 then "critical branching (lines close)"
             else if ply.deltaWinPct.abs >= 3 then "notable eval swing"
             else "concept shift"

           val branches = extraLines.take(3).zipWithIndex.map { case (l, idx) =>
              val label = idx match
                case 0 => "Best Move"
                case 1 => "Alternative"
                case _ => "Line"
              Branch(move = l.move, winPct = l.winPct, pv = l.pv, label = label, cp = l.cp, mate = l.mate)
           }

           val forcedMove = ply.legalMoves <= 1 || gap >= 20.0
           val tags = (phaseLabel.toList ++ ply.semanticTags).distinct.take(6)

           // Opponent Robustness check (Async)
           // If we have turning point data, we might not need separate call?
           // Robustness is about the position AFTER the move.
           // Let's do a quick check if needed.
           // Optimisation: skip robustness if low priority or trust static features?
           // Let's keep it simple for now and omit extra Robustness call to save time, 
           // or add a light check if critical.
           val opponentRobustness = None // Defer for performance in Zone 6

           val isPressurePoint = false

           CriticalNode(
             ply = ply.ply,
             reason = reason,
             deltaWinPct = ply.deltaWinPct,
             branches = branches,
             bestVsSecondGap = Some(gap),
             bestVsPlayedGap = ply.bestVsPlayedGap,
             forced = forcedMove,
             legalMoves = Some(ply.legalMoves),
             mistakeCategory = ply.mistakeCategory,
             tags = tags,
             practicality = ply.practicality,
             opponentRobustness = opponentRobustness,
             isPressurePoint = isPressurePoint
           )
        }
      }
    )
