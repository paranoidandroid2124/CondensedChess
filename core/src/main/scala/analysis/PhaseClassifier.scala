package chess
package analysis

import AnalysisModel.Concepts

object PhaseClassifier:
  def detectPhaseLabel(delta: Concepts, winPctBefore: Double): Option[String] =
    if delta.dynamic <= -0.3 && delta.drawish >= 0.3 then Some(TagName.EndgameTransition)
    else if delta.tacticalDepth <= -0.25 && delta.dry >= 0.25 then Some(TagName.ShiftTacticalToPositional)
    else if delta.fortress >= 0.4 then Some(TagName.FortressBuilding)
    else if delta.comfortable <= -0.3 && delta.unpleasant >= 0.3 then Some("comfort_to_unpleasant")
    else if delta.alphaZeroStyle >= 0.35 then Some(TagName.PositionalSacrifice)
    else if delta.kingSafety >= 0.4 then Some(TagName.KingExposed)
    else if delta.conversionDifficulty >= 0.35 && winPctBefore >= 60 then Some(TagName.ConversionDifficulty)
    else None

  def phaseTransition(delta: Concepts, winPctBefore: Double): (Double, Option[String]) =
    val dynamicCollapse = if delta.dynamic <= -0.3 && delta.drawish >= 0.3 then 15.0 else 0.0
    val tacticalDry = if delta.tacticalDepth <= -0.25 && delta.dry >= 0.25 then 12.0 else 0.0
    val fortressJump = if delta.fortress >= 0.4 then 10.0 else 0.0
    val comfortLoss = if delta.comfortable <= -0.3 && delta.unpleasant >= 0.3 then 10.0 else 0.0
    val alphaZeroSpike = if delta.alphaZeroStyle >= 0.35 then 8.0 else 0.0
    val kingSafetyCollapse = if delta.kingSafety >= 0.4 then 12.0 else 0.0
    val conversionIssue = if delta.conversionDifficulty >= 0.35 && winPctBefore >= 60 then 8.0 else 0.0
    val phaseScore = List(dynamicCollapse, tacticalDry, fortressJump, comfortLoss, alphaZeroSpike, kingSafetyCollapse, conversionIssue).max
    (phaseScore, detectPhaseLabel(delta, winPctBefore))
