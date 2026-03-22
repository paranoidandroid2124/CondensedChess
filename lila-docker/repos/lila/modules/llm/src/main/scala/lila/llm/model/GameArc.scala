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

case class GameArcMoment(
  ply: Int,
  momentType: String,          // "Blunder", "MissedWin", "TensionPeak", etc.
  narrative: String,           // The generated Book-Style narrative text
  analysisData: ExtendedAnalysisData,
  selectionKind: String = "key",
  selectionLabel: Option[String] = Some("Key Moment"),
  selectionReason: Option[String] = None,
  
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
  probeRefinementRequests: List[lila.llm.model.ProbeRequest] = Nil,
  authorQuestions: List[lila.llm.AuthorQuestionSummary] = Nil,
  authorEvidence: List[lila.llm.AuthorEvidenceSummary] = Nil,
  mainStrategicPlans: List[lila.llm.model.authoring.PlanHypothesis] = Nil,
  strategicPlanExperiments: List[lila.llm.model.StrategicPlanExperiment] = Nil,
  latentPlans: List[lila.llm.model.authoring.LatentPlanNarrative] = Nil,
  whyAbsentFromTopMultiPV: List[String] = Nil,
  strategicBranch: Boolean = false,
  activeStrategicNote: Option[String] = None,
  activeStrategicSourceMode: Option[String] = None,
  activeStrategicIdeas: List[lila.llm.ActiveStrategicIdeaRef] = Nil,
  activeStrategicRoutes: List[lila.llm.ActiveStrategicRouteRef] = Nil,
  activeStrategicMoves: List[lila.llm.ActiveStrategicMoveRef] = Nil,
  activeDirectionalTargets: List[lila.llm.StrategyDirectionalTarget] = Nil,
  activeBranchDossier: Option[lila.llm.ActiveBranchDossier] = None,
  strategicThread: Option[lila.llm.ActiveStrategicThreadRef] = None
)
object GameArcMoment {
  implicit val writes: OWrites[GameArcMoment] = Json.writes[GameArcMoment]
}

case class GameArc(
  gameIntro: String,           // e.g. "In this Ruy Lopez encounter..."
  keyMomentNarratives: List[GameArcMoment],
  conclusion: String,          // e.g. "White capitalized on the blunder..."
  overallThemes: List[String], // e.g. ["King hunt", "Exchange sacrifice"]
  internalMomentCount: Int = 0,
  strategicThreads: List[lila.llm.ActiveStrategicThread] = Nil
)
object GameArc {
  implicit val writes: OWrites[GameArc] = Json.writes[GameArc]
}
