package lila.llm.analysis.prior

import chess.Color
import lila.llm.analysis.IntegratedContext

object StrategicPriorFeatures:

  def fromContext(
      side: Color,
      ctx: IntegratedContext,
      executionEase: Double
  ): Map[String, Double] =
    val centerStable = ctx.features.map(f => if f.centralSpace.lockedCenter then 1.0 else if f.centralSpace.openCenter then 0.2 else 0.6).getOrElse(0.5)
    val flankSpace = ctx.features.map { f =>
      val diff = if side.white then f.centralSpace.spaceDiff else -f.centralSpace.spaceDiff
      if diff > 0 then 0.6 else 0.25
    }.getOrElse(0.3)
    val kingExposureThem = ctx.features.map { f =>
      if side.white then (f.kingSafety.blackKingExposedFiles.toDouble / 4.0).min(1.0)
      else (f.kingSafety.whiteKingExposedFiles.toDouble / 4.0).min(1.0)
    }.getOrElse(0.3)
    val counterplayRisk = if ctx.tacticalThreatToUs then 1.0 else if ctx.strategicThreatToUs then 0.7 else 0.2
    Map(
      "center_stable" -> centerStable,
      "space_flank_kingside" -> flankSpace,
      "space_flank_queenside" -> flankSpace,
      "king_exposure_them" -> kingExposureThem,
      "tactical_threat_them" -> (if ctx.tacticalThreatToThem then 1.0 else 0.0),
      "tactical_threat_us" -> (if ctx.tacticalThreatToUs then 1.0 else 0.0),
      "counterplay_risk" -> counterplayRisk,
      "execution_ease" -> executionEase.max(0.0).min(1.0)
    )
