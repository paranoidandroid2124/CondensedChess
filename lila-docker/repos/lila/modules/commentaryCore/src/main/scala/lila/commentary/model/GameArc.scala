package lila.commentary.model

import lila.commentary.model.ExtendedAnalysisData
import lila.commentary.model.CollapseAnalysis

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
  activePlan: Option[lila.commentary.ActivePlanRef] = None,
  topEngineMove: Option[lila.commentary.EngineAlternative] = None,
  collapse: Option[CollapseAnalysis] = None,
  strategyPack: Option[lila.commentary.StrategyPack] = None,
  signalDigest: Option[lila.commentary.NarrativeSignalDigest] = None,
  probeRequests: List[lila.commentary.model.ProbeRequest] = Nil,
  probeRefinementRequests: List[lila.commentary.model.ProbeRequest] = Nil,
  authorQuestions: List[lila.commentary.AuthorQuestionSummary] = Nil,
  authorEvidence: List[lila.commentary.AuthorEvidenceSummary] = Nil,
  mainStrategicPlans: List[lila.commentary.model.authoring.PlanHypothesis] = Nil,
  strategicPlanExperiments: List[lila.commentary.model.StrategicPlanExperiment] = Nil,
  strategicBranch: Boolean = false,
  activeStrategicNote: Option[String] = None,
  activeStrategicSourceMode: Option[String] = None,
  activeStrategicIdeas: List[lila.commentary.ActiveStrategicIdeaRef] = Nil,
  activeStrategicRoutes: List[lila.commentary.ActiveStrategicRouteRef] = Nil,
  activeStrategicMoves: List[lila.commentary.ActiveStrategicMoveRef] = Nil,
  activeDirectionalTargets: List[lila.commentary.StrategyDirectionalTarget] = Nil,
  activeBranchDossier: Option[lila.commentary.ActiveBranchDossier] = None,
  strategicThread: Option[lila.commentary.ActiveStrategicThreadRef] = None,
  truthPhase: Option[String] = None,
  surfacedMoveOwnsTruth: Boolean = false,
  verifiedPayoffAnchor: Option[String] = None,
  compensationProseAllowed: Boolean = false,
  benchmarkProseAllowed: Boolean = false,
  investmentTruthChainKey: Option[String] = None
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
  strategicThreads: List[lila.commentary.ActiveStrategicThread] = Nil
)
object GameArc {
  implicit val writes: OWrites[GameArc] = Json.writes[GameArc]
}
