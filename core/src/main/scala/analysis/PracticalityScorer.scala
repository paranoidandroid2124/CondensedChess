package chess
package analysis

object PracticalityScorer:

  final case class Score(
      overall: Double,
      robustness: Double,
      horizon: Double,
      naturalness: Double,
      category: String
  )

  def score(
      eval: AnalyzePgn.EngineEval,
      tacticalDepth: Double,
      sacrificeQuality: Double,
      tags: List[String]
  ): Score =
    val robustness = computeRobustness(eval)
    val horizon = computeHorizon(tacticalDepth)
    val naturalness = computeNaturalness(sacrificeQuality, tags)

    val overall = 0.4 * robustness + 0.4 * horizon + 0.2 * naturalness
    val category = categorize(overall)

    Score(overall, robustness, horizon, naturalness, category)

  private def computeRobustness(eval: AnalyzePgn.EngineEval): Double =
    val topLines = eval.lines.take(3)
    if topLines.isEmpty then 0.0
    else
      val best = topLines.head.winPct
      val worst = topLines.last.winPct
      val spread = (best - worst).abs
      // If spread is 20% or more, robustness is 0.
      // If spread is 0%, robustness is 1.
      clamp(1.0 - (spread / 20.0))

  private def computeHorizon(tacticalDepth: Double): Double =
    // tacticalDepth is typically 0.0 to 1.0 (shallow vs deep eval gap)
    // High tacticalDepth means the advantage is hidden deep -> Low Horizon
    clamp(1.0 - tacticalDepth)

  private def computeNaturalness(sacrificeQuality: Double, tags: List[String]): Double =
    // Penalty for sacrifice
    val sacrificePenalty = if sacrificeQuality >= 0.5 then 0.2 else 0.0
    // Penalty for plan change
    val planShiftPenalty = if tags.exists(_.contains("plan_change")) then 0.1 else 0.0
    
    clamp(1.0 - (sacrificePenalty + planShiftPenalty))

  private def categorize(score: Double): String =
    if score >= 0.8 then "Human-Friendly"
    else if score >= 0.5 then "Challenging"
    else if score >= 0.2 then "Engine-Like"
    else "Computer-Only"

  private def clamp(d: Double): Double = math.max(0.0, math.min(1.0, d))
