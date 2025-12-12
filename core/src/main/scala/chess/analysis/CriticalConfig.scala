package chess
package analysis

/**
 * Configuration for Critical Moment Detection thresholds.
 * All thresholds have sensible defaults matching original hardcoded values.
 */
final case class CriticalConfig(
    // Phase transition thresholds
    dynamicCollapseThreshold: Double = 0.3,
    drawishThreshold: Double = 0.3,
    tacticalDryThreshold: Double = 0.25,
    dryThreshold: Double = 0.25,
    fortressJumpThreshold: Double = 0.4,
    comfortLossThreshold: Double = 0.3,
    unpleasantThreshold: Double = 0.3,
    alphaZeroSpikeThreshold: Double = 0.35,
    kingSafetyCollapseThreshold: Double = 0.4,
    conversionIssueThreshold: Double = 0.35,
    conversionWinPctThreshold: Double = 60.0,
    
    // Phase transition scores
    dynamicCollapseScore: Double = 15.0,
    tacticalDryScore: Double = 12.0,
    fortressJumpScore: Double = 10.0,
    comfortLossScore: Double = 10.0,
    alphaZeroSpikeScore: Double = 8.0,
    kingSafetyCollapseScore: Double = 12.0,
    conversionIssueScore: Double = 8.0,
    
    // Judgement boost scores
    blunderBoost: Double = 15.0,
    mistakeBoost: Double = 8.0,
    inaccuracyBoost: Double = 4.0,
    
    // Criticality score weights
    deltaWeight: Double = 0.6,
    branchTensionWeight: Double = 0.8,
    conceptJumpWeight: Double = 20.0,
    phaseShiftWeight: Double = 1.5,
    llmBoost: Double = 50.0,
    
    // Critical node selection
    minCriticalNodes: Int = 3,
    maxCriticalNodes: Int = 8,
    nodesPerMoves: Int = 20  // 3 + timeline.size / nodesPerMoves
)

object CriticalConfig:
  val default: CriticalConfig = CriticalConfig()
