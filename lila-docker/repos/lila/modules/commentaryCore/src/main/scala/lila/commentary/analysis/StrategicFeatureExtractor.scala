package lila.commentary.analysis

import lila.commentary.model.{ ExtendedAnalysisData, PlanMatch, Motif, PositionNature }
import lila.commentary.model.strategic.{ PlanContinuity, EndgamePatternState }
import chess.Color

trait StrategicFeatureExtractor {
  def extract(
      fen: String,
      metadata: AnalysisMetadata,
      baseData: BaseAnalysisData,
      vars: List[lila.commentary.model.strategic.VariationLine],
      playedMove: Option[String],
      probeResults: List[lila.commentary.model.ProbeResult] = Nil,
      prevEndgameState: Option[EndgamePatternState] = None
  ): ExtendedAnalysisData
}

case class BaseAnalysisData(
    nature: PositionNature,
    motifs: List[Motif],
    plans: List[PlanMatch],
    planContinuity: Option[PlanContinuity] = None,
    planSequence: Option[lila.commentary.model.PlanSequenceSummary] = None
)

case class AnalysisMetadata(
    color: Color,
    ply: Int,
    prevMove: Option[String]
)
