package chess
package analysis

import AnalysisModel.*
// PhaseClassifier removed - using inline stub until full TransitionTagger migration

object CriticalDetector:
  private final case class EvalCacheKey(fen: String, depth: Int, multiPv: Int, timeMs: Int)

  // Stub replacement for PhaseClassifier.phaseTransition
  private def phaseTransition(delta: Concepts, winPctBefore: Double): (Double, Option[String]) =
    val dynamicCollapse = if delta.dynamic <= -0.3 && delta.drawish >= 0.3 then 15.0 else 0.0
    val tacticalDry = if delta.tacticalDepth <= -0.25 && delta.dry >= 0.25 then 12.0 else 0.0
    val fortressJump = if delta.fortress >= 0.4 then 10.0 else 0.0
    val comfortLoss = if delta.comfortable <= -0.3 && delta.unpleasant >= 0.3 then 10.0 else 0.0
    val alphaZeroSpike = if delta.alphaZeroStyle >= 0.35 then 8.0 else 0.0
    val kingSafetyCollapse = if delta.kingSafety >= 0.4 then 12.0 else 0.0
    val conversionIssue = if delta.conversionDifficulty >= 0.35 && winPctBefore >= 60 then 8.0 else 0.0
    val phaseScore = List(dynamicCollapse, tacticalDry, fortressJump, comfortLoss, alphaZeroSpike, kingSafetyCollapse, conversionIssue).max
    
    val label = 
      if dynamicCollapse > 0 then Some("endgame_transition")
      else if tacticalDry > 0 then Some("tactical_to_positional")
      else if fortressJump > 0 then Some("fortress_building")
      else if comfortLoss > 0 then Some("comfort_to_unpleasant")
      else if alphaZeroSpike > 0 then Some("positional_sacrifice")
      else if kingSafetyCollapse > 0 then Some("king_exposed")
      else if conversionIssue > 0 then Some("conversion_difficulty")
      else None
    (phaseScore, label)

  def detectCritical(timeline: Vector[PlyOutput], client: StockfishClient, config: EngineConfig, llmRequestedPlys: Set[Int], jobId: Option[String] = None): Vector[CriticalNode] =
    val evalCache = scala.collection.mutable.Map.empty[EvalCacheKey, EngineEval]
    val judgementBoost: Map[String, Double] = Map(
      "blunder" -> 15.0,
      "mistake" -> 8.0,
      "inaccuracy" -> 4.0,
      "good" -> 0.0,
      "best" -> 0.0,
      "book" -> 0.0
    )

    def criticalityScore(p: PlyOutput): (Double, Double, Option[String]) =
      val deltaScore = math.abs(p.deltaWinPct)
      val conceptJump = p.concepts.dynamic + p.concepts.tacticalDepth + p.concepts.blunderRisk
      val bestVsSecondGap = p.bestVsSecondGap.getOrElse {
        val top = p.evalBeforeDeep.lines.headOption
        val second = p.evalBeforeDeep.lines.drop(1).headOption
        (for t <- top; s <- second yield (t.winPct - s.winPct).abs).getOrElse(100.0)
      }
      val branchTension = math.max(0.0, 12.0 - bestVsSecondGap)
      val (phaseShift, phaseLabel) = phaseTransition(p.conceptDelta, p.winPctBefore)
      val score =
        deltaScore * 0.6 +
          branchTension * 0.8 +
          judgementBoost.getOrElse(p.judgement, 0.0) +
          conceptJump * 20.0 +
          phaseShift * 1.5
      (score, bestVsSecondGap, phaseLabel)

    val scored = timeline.map { p =>
      val (s, gap, phaseLabel) = criticalityScore(p)
      val llmBoost = if llmRequestedPlys.contains(p.ply.value) then 50.0 else 0.0
      (p, (s + llmBoost, gap, phaseLabel))
    }
    val cap = math.min(8, math.max(3, 3 + timeline.size / 20))

    def extraEval(fen: String, multiPv: Int): EngineEval =
      val depth = config.deepDepth + config.extraDepthDelta
      val time = config.deepTimeMs + config.extraTimeMs
      val key = EvalCacheKey(fen, depth, multiPv, time)
      evalCache.getOrElseUpdate(key, EngineProbe.evalFen(client, fen, depth = depth, multiPv = multiPv, moveTimeMs = Some(time)))

    val topNodes = scored.sortBy { case (_, (s, _, _)) => -s }.take(cap)
    val totalCritical = topNodes.size

    topNodes.zipWithIndex.map { case ((ply, (_, gap, phaseLabel)), idx) =>
      jobId.foreach { id =>
        val prog = idx.toDouble / totalCritical
        AnalysisProgressTracker.update(id, AnalysisStage.CRITICAL_DETECTION, prog)
      }
      val reason =
        if ply.judgement == "blunder" then "ΔWin% blunder spike"
        else if ply.judgement == "mistake" then "ΔWin% mistake"
        else if phaseLabel.nonEmpty then s"phase shift: ${phaseLabel.get}"
        else if gap <= 5.0 then "critical branching (lines close)"
        else if ply.deltaWinPct.abs >= 3 then "notable eval swing"
        else "concept shift"

      val baseLines = (ply.evalBeforeDeep.lines.headOption.toList ++ ply.evalBeforeDeep.lines.drop(1).take(2))
      val enriched =
        try
          val extra = extraEval(ply.fenBefore, math.min(3, config.maxMultiPv)).lines
          if extra.nonEmpty then extra.take(3) else baseLines
        catch
          case _: Throwable => baseLines

      val branches =
        enriched.zipWithIndex.map {
          case (l, idx) =>
            val label = idx match
              case 0 => "Best Move"
              case 1 => "Alternative"
              case 2 => "Alternative"
              case _ => "Line"
            Branch(move = l.move, winPct = l.winPct, pv = l.pv, label = label)
        }

      val forcedMove = ply.legalMoves <= 1 || gap >= 20.0

      val tags = (phaseLabel.toList ++ ply.semanticTags).distinct.take(6)

      // Calculate opponent robustness (difficulty for them to reply)
      // We analyze the position AFTER the move (ply.fen)
      val opponentRobustness =
        try
          val oppEval = extraEval(ply.fen, math.min(3, config.maxMultiPv))
          Some(PracticalityScorer.computeOpponentRobustness(oppEval))
        catch
          case _: Throwable => None

      // Pressure Point: opponent faces difficult defensive problem
      val isPressurePoint = opponentRobustness.exists(_ < 0.3)

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
