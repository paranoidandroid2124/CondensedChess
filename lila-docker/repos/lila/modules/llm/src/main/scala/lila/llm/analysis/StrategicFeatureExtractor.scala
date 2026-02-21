package lila.llm.analysis

import lila.llm.model.{ ExtendedAnalysisData, PlanMatch, Motif, PositionNature }
import chess.Color

trait StrategicFeatureExtractor {
  def extract(
      fen: String,
      metadata: AnalysisMetadata,
      baseData: BaseAnalysisData,
      vars: List[lila.llm.model.strategic.VariationLine],
      playedMove: Option[String],
      probeResults: List[lila.llm.model.ProbeResult] = Nil
  ): ExtendedAnalysisData
}

case class BaseAnalysisData(
    nature: PositionNature,
    motifs: List[Motif],
    plans: List[PlanMatch],
    planSequence: Option[(lila.llm.model.TransitionType, Double)] = None
)

case class AnalysisMetadata(
    color: Color,
    ply: Int,
    prevMove: Option[String]
)
