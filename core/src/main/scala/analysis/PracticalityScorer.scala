package chess
package analysis

import chess.analysis.ConceptLabeler.{ ConceptLabels, TransitionTag, TacticTag }

/**
 * PracticalityScorer V2: Rebuilt on Phase 4 data.
 * Uses ConceptLabels (tags) instead of direct engine eval.
 */
object PracticalityScorer:

  final case class Score(
      overall: Double,
      robustness: Double,
      horizon: Double,
      naturalness: Double,
      categoryGlobal: String,
      categoryPersonal: Option[String] = None
  )

  /**
   * V2 Implementation: Score from Phase 4 ConceptLabels
   */
  def scoreFromConcepts(
      labels: ConceptLabels,
      evalSpread: Double,       // (best - 3rd best) winPct spread
      tacticalDepth: Double,    // 0.0 (shallow) to 1.0 (deep)
      playerElo: Option[Int] = None
  ): Score =
    val robustness = computeRobustnessV2(evalSpread)
    val horizon = computeHorizonV2(tacticalDepth)
    val naturalness = computeNaturalnessV2(labels)

    val overall = 0.5 * robustness + 0.3 * horizon + 0.2 * naturalness
    val categoryGlobal = categorize(overall)
    val categoryPersonal = playerElo.map(elo => categorizePersonal(overall, elo))

    Score(overall, robustness, horizon, naturalness, categoryGlobal, categoryPersonal)

  // --- V2 Helpers ---

  private def computeRobustnessV2(evalSpread: Double): Double =
    // Lower spread = more robust
    val tau = 20.0
    math.max(0.0, math.min(1.0, math.exp(-evalSpread / tau)))

  private def computeHorizonV2(tacticalDepth: Double): Double =
    // Higher depth = harder to see = lower horizon score
    math.max(0.0, math.min(1.0, math.exp(-1.2 * tacticalDepth)))

  private def computeNaturalnessV2(labels: ConceptLabels): Double =
    var penalty = 0.0
    
    // Penalty for positional sacrifice
    if labels.transitionTags.contains(TransitionTag.PositionalSacrifice) then
      penalty += 0.25
    
    // Penalty for engine-only tactics
    if labels.tacticTags.exists(t => t == TacticTag.GreekGiftUnsound) then
      penalty += 0.2
    
    // Penalty for comfort loss
    if labels.transitionTags.contains(TransitionTag.ComfortToUnpleasant) then
      penalty += 0.15
    
    math.max(0.0, math.min(1.0, 1.0 - penalty))

  private def categorize(score: Double): String =
    if score >= 0.85 then "Human-Friendly"
    else if score >= 0.6 then "Challenging"
    else if score >= 0.3 then "Engine-Like"
    else "Computer-Only"

  private def categorizePersonal(rawScore: Double, elo: Int): String =
    val scaleFactor =
      if elo < 1200 then 0.8
      else if elo < 1600 then 0.9
      else if elo < 2000 then 1.0
      else if elo < 2400 then 1.1
      else 1.2
    categorize(rawScore * scaleFactor)

  // --- Legacy Helper (kept for CriticalDetector.computeOpponentRobustness) ---

  def computeOpponentRobustness(eval: AnalyzePgn.EngineEval): Double =
    computeRobustnessLegacy(eval)

  private def computeRobustnessLegacy(eval: AnalyzePgn.EngineEval): Double =
    val topLines = eval.lines.take(3)
    if topLines.isEmpty then 0.0
    else
      val best = topLines.head.winPct
      val worst = topLines.last.winPct
      val spread = (best - worst).abs
      val tau = 20.0
      math.max(0.0, math.min(1.0, math.exp(-spread / tau)))
