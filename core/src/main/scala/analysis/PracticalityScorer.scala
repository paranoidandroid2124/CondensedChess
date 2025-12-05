package chess
package analysis

object PracticalityScorer:

  final case class Score(
      overall: Double,
      robustness: Double,
      horizon: Double,
      naturalness: Double,
      categoryGlobal: String,
      categoryPersonal: Option[String] = None
  )

  def score(
      eval: AnalyzePgn.EngineEval,
      tacticalDepth: Double,
      sacrificeQuality: Double,
      tags: List[String],
      context: Option[AnalysisModel.PlayerContext] = None,
      turn: chess.Color
  ): Score =
    val robustness = computeRobustness(eval)
    val horizon = computeHorizon(tacticalDepth, context)
    val naturalness = computeNaturalness(sacrificeQuality, tags)

    val overall = 0.3 * robustness + 0.5 * horizon + 0.2 * naturalness
    val categoryGlobal = categorize(overall)
    
    val categoryPersonal = context.flatMap { ctx =>
      val playerElo = if turn == chess.White then ctx.whiteElo else ctx.blackElo
      categorizePersonal(overall, playerElo, ctx.timeControl)
    }

    Score(overall, robustness, horizon, naturalness, categoryGlobal, categoryPersonal)

  def computeOpponentRobustness(eval: AnalyzePgn.EngineEval): Double =
    computeRobustness(eval)

  private def computeRobustness(eval: AnalyzePgn.EngineEval): Double =
    val topLines = eval.lines.take(3)
    if topLines.isEmpty then 0.0
    else
      val best = topLines.head.winPct
      val worst = topLines.last.winPct
      val spread = (best - worst).abs
      // v2: Exponential decay instead of linear
      // tau=20.0 calibrated so spread=3.2% → robustness≈0.85
      val tau = 20.0
      math.max(0.0, math.min(1.0, math.exp(-spread / tau)))

  private def computeHorizon(tacticalDepth: Double, context: Option[AnalysisModel.PlayerContext]): Double =
    // tacticalDepth is typically 0.0 to 1.0 (shallow vs deep eval gap)
    // High tacticalDepth means the advantage is hidden deep -> Low Horizon
    
    // v2: Exponential decay with saturating behavior
    // Time control adjustment: higher k = steeper penalty
    val k = context.flatMap(_.timeControl).map { tc =>
      if tc.isBlitz then 1.8      // Steeper penalty in blitz
      else if tc.isRapid then 1.4 // Moderate penalty in rapid
      else 1.0
    }.getOrElse(1.0)

    // exp(-k*tacticalDepth): 0→1.0, 0.5→~0.4-0.6, 1.0→~0.14-0.17
    clamp(math.exp(-k * tacticalDepth))

  private def computeNaturalness(sacrificeQuality: Double, tags: List[String]): Double =
    // Penalty for sacrifice
    val sacrificePenalty = if sacrificeQuality >= 0.5 then 0.2 else 0.0
    // Penalty for plan change
    val planShiftPenalty = if tags.exists(_.contains("plan_change")) then 0.1 else 0.0
    
    clamp(1.0 - (sacrificePenalty + planShiftPenalty))

  private def categorize(score: Double): String =
    if score >= 0.85 then "Human-Friendly"
    else if score >= 0.6 then "Challenging"
    else if score >= 0.3 then "Engine-Like"
    else "Computer-Only"

  private def categorizePersonal(
      rawScore: Double, 
      playerElo: Option[Int], 
      timeControl: Option[AnalysisModel.TimeControl]
  ): Option[String] =
    playerElo.map { elo =>
      // Rating-based scaling
      // Lower rated players get a boost (easier to be considered Human-Friendly)
      // Higher rated players get a penalty (stricter standards)
      val scaleFactor = 
        if elo < 1200 then 1.2
        else if elo < 1600 then 1.1
        else if elo < 2000 then 1.0
        else if elo < 2400 then 0.9
        else 0.8 // GM level

      val adjustedScore = rawScore * scaleFactor
      categorize(adjustedScore)
    }

  private def clamp(d: Double): Double = math.max(0.0, math.min(1.0, d))
