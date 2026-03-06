package lila.llm.model

import lila.llm.model.ExtendedAnalysisData
import lila.llm.model.CollapseAnalysis

import play.api.libs.json._

case class GameMetadata(
  white: String,
  black: String,
  event: String, 
  date: String,
  result: String
)
object GameMetadata {
  implicit val writes: OWrites[GameMetadata] = Json.writes[GameMetadata]
}

case class MomentNarrative(
  ply: Int,
  momentType: String,          // "Blunder", "MissedWin", "TensionPeak", etc.
  narrative: String,           // The generated Book-Style narrative text
  analysisData: ExtendedAnalysisData,
  
  // UX Specific Metadata fields passed to Frontend
  moveClassification: Option[String] = None,
  cpBefore: Option[Int] = None,
  cpAfter: Option[Int] = None,
  mateBefore: Option[Int] = None,
  mateAfter: Option[Int] = None,
  wpaSwing: Option[Double] = None,
  transitionType: Option[String] = None,
  transitionConfidence: Option[Double] = None,
  activePlan: Option[lila.llm.ActivePlanRef] = None,
  topEngineMove: Option[lila.llm.EngineAlternative] = None,
  collapse: Option[CollapseAnalysis] = None,
  strategyPack: Option[lila.llm.StrategyPack] = None,
  signalDigest: Option[lila.llm.NarrativeSignalDigest] = None,
  probeRequests: List[lila.llm.model.ProbeRequest] = Nil,
  authorQuestions: List[lila.llm.AuthorQuestionSummary] = Nil,
  authorEvidence: List[lila.llm.AuthorEvidenceSummary] = Nil,
  mainStrategicPlans: List[lila.llm.model.authoring.PlanHypothesis] = Nil,
  latentPlans: List[lila.llm.model.authoring.LatentPlanNarrative] = Nil,
  whyAbsentFromTopMultiPV: List[String] = Nil,
  strategicBranch: Boolean = false,
  activeStrategicNote: Option[String] = None,
  activeStrategicSourceMode: Option[String] = None,
  activeStrategicRoutes: List[lila.llm.ActiveStrategicRouteRef] = Nil,
  activeStrategicMoves: List[lila.llm.ActiveStrategicMoveRef] = Nil
)
object MomentNarrative {
  implicit val writes: OWrites[MomentNarrative] = Json.writes[MomentNarrative]
}

case class FullGameNarrative(
  gameIntro: String,           // e.g. "In this Ruy Lopez encounter..."
  keyMomentNarratives: List[MomentNarrative],
  conclusion: String,          // e.g. "White capitalized on the blunder..."
  overallThemes: List[String]  // e.g. ["King hunt", "Exchange sacrifice"]
)
object FullGameNarrative {
  implicit val writes: OWrites[FullGameNarrative] = Json.writes[FullGameNarrative]
}
