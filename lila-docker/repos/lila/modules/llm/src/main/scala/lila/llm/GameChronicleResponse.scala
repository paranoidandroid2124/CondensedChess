package lila.llm

import play.api.libs.json.*
import lila.llm.model.strategic.VariationLine

case class GameChronicleResponse(
    schema: String,
    intro: String,
    moments: List[GameChronicleMoment],
    conclusion: String,
    themes: List[String],
    review: Option[GameChronicleReview] = None,
    sourceMode: String = "rule",
    model: Option[String] = None,
    planTier: String = PlanTier.Basic,
    llmLevel: String = LlmLevel.Polish,
    strategicThreads: List[ActiveStrategicThread] = Nil
)

object GameChronicleResponse:

  val schemaV6 = "chesstory.gameNarrative.v6"

  given Writes[GameChronicleResponse] = Json.writes[GameChronicleResponse]

case class GameChronicleMoment(
    momentId: String,
    ply: Int,
    moveNumber: Int,
    side: String,
    moveClassification: Option[String],
    momentType: String,
    fen: String,
    narrative: String,
    selectionKind: String = "key",
    selectionLabel: Option[String] = Some("Key Moment"),
    selectionReason: Option[String] = None,
    concepts: List[String],
    variations: List[VariationLine],
    cpBefore: Int,
    cpAfter: Int,
    mateBefore: Option[Int],
    mateAfter: Option[Int],
    wpaSwing: Option[Double],
    strategicSalience: Option[String],
    transitionType: Option[String],
    transitionConfidence: Option[Double],
    activePlan: Option[ActivePlanRef],
    topEngineMove: Option[EngineAlternative],
    collapse: Option[lila.llm.model.CollapseAnalysis],
    strategyPack: Option[lila.llm.StrategyPack] = None,
    signalDigest: Option[lila.llm.NarrativeSignalDigest] = None,
    probeRequests: List[lila.llm.model.ProbeRequest] = Nil,
    probeRefinementRequests: List[lila.llm.model.ProbeRequest] = Nil,
    authorQuestions: List[AuthorQuestionSummary] = Nil,
    authorEvidence: List[AuthorEvidenceSummary] = Nil,
    mainStrategicPlans: List[JsObject] = Nil,
    strategicPlanExperiments: List[JsObject] = Nil,
    strategicBranch: Boolean = false,
    activeStrategicNote: Option[String] = None,
    activeStrategicSourceMode: Option[String] = None,
    activeStrategicIdeas: List[ActiveStrategicIdeaRef] = Nil,
    activeStrategicRoutes: List[ActiveStrategicRouteRef] = Nil,
    activeStrategicMoves: List[ActiveStrategicMoveRef] = Nil,
    activeDirectionalTargets: List[StrategyDirectionalTarget] = Nil,
    activeBranchDossier: Option[ActiveBranchDossier] = None,
    strategicThread: Option[ActiveStrategicThreadRef] = None
)

object GameChronicleMoment:

  given Writes[GameChronicleMoment] = Json.writes[GameChronicleMoment]

case class GameChronicleReview(
    schemaVersion: Int,
    reviewPerspective: String,
    totalPlies: Int,
    evalCoveredPlies: Int,
    evalCoveragePct: Int,
    selectedMoments: Int,
    selectedMomentPlies: List[Int],
    internalMomentCount: Int,
    visibleMomentCount: Int,
    polishedMomentCount: Int,
    visibleStrategicMomentCount: Int,
    visibleBridgeMomentCount: Int,
    blundersCount: Int,
    missedWinsCount: Int,
    brilliantMovesCount: Int,
    accuracyWhite: Option[Double],
    accuracyBlack: Option[Double],
    momentTypeCounts: Map[String, Int]
)

object GameChronicleReview:
  given Writes[GameChronicleReview] = Json.writes[GameChronicleReview]

case class ActivePlanRef(
    themeL1: String,
    subplanId: Option[String],
    phase: Option[String],
    commitmentScore: Option[Double]
)
object ActivePlanRef:
  given Writes[ActivePlanRef] = Json.writes[ActivePlanRef]

case class EngineAlternative(
    uci: String,
    san: Option[String],
    cpAfterAlt: Option[Int],
    cpLossVsPlayed: Option[Int],
    pv: List[String]
)
object EngineAlternative:
  given Writes[EngineAlternative] = Json.writes[EngineAlternative]
