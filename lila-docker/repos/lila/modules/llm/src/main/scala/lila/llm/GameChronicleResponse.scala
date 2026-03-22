package lila.llm

import play.api.libs.json.*
import lila.llm.model.{ GameArc, GameArcMoment }
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

  def fromGameArc(
      arc: GameArc,
      review: Option[GameChronicleReview] = None,
      sourceMode: String = "rule",
      model: Option[String] = None,
      planTier: String = PlanTier.Basic,
      llmLevel: String = LlmLevel.Polish
  ): GameChronicleResponse =
    GameChronicleResponse(
      schema = schemaV6,
      intro = arc.gameIntro,
      moments = arc.keyMomentNarratives.map(GameChronicleMoment.fromArcMoment),
      conclusion = arc.conclusion,
      themes = arc.overallThemes,
      review = review,
      sourceMode = sourceMode,
      model = model,
      planTier = PlanTier.normalize(planTier),
      llmLevel = LlmLevel.normalize(llmLevel),
      strategicThreads = arc.strategicThreads
    )

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
    mainStrategicPlans: List[lila.llm.model.authoring.PlanHypothesis] = Nil,
    strategicPlanExperiments: List[lila.llm.model.StrategicPlanExperiment] = Nil,
    latentPlans: List[lila.llm.model.authoring.LatentPlanNarrative] = Nil,
    whyAbsentFromTopMultiPV: List[String] = Nil,
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

  def fromArcMoment(moment: GameArcMoment): GameChronicleMoment =
    val moveNum = (moment.ply + 1) / 2
    val side = if (moment.ply % 2 == 1) "white" else "black"
    GameChronicleMoment(
      momentId = s"ply_${moment.ply}_${moment.momentType.toLowerCase}",
      ply = moment.ply,
      moveNumber = moveNum,
      side = side,
      moveClassification = moment.moveClassification,
      momentType = moment.momentType,
      fen = moment.analysisData.fen,
      narrative = moment.narrative,
      selectionKind = moment.selectionKind,
      selectionLabel = moment.selectionLabel,
      selectionReason = moment.selectionReason,
      concepts = moment.analysisData.conceptSummary,
      variations = moment.analysisData.alternatives,
      cpBefore = moment.cpBefore.getOrElse(0),
      cpAfter = moment.cpAfter.getOrElse(moment.analysisData.evalCp),
      mateBefore = moment.mateBefore,
      mateAfter = moment.mateAfter,
      wpaSwing = moment.wpaSwing,
      strategicSalience = Some(moment.analysisData.strategicSalience.toString),
      transitionType = moment.transitionType,
      transitionConfidence = moment.transitionConfidence,
      activePlan = moment.activePlan,
      topEngineMove = moment.topEngineMove,
      collapse = moment.collapse,
      strategyPack = moment.strategyPack,
      signalDigest = moment.signalDigest,
      probeRequests = moment.probeRequests,
      probeRefinementRequests = moment.probeRefinementRequests,
      authorQuestions = moment.authorQuestions,
      authorEvidence = moment.authorEvidence,
      mainStrategicPlans = moment.mainStrategicPlans,
      strategicPlanExperiments = moment.strategicPlanExperiments,
      latentPlans = moment.latentPlans,
      whyAbsentFromTopMultiPV = moment.whyAbsentFromTopMultiPV,
      strategicBranch = moment.strategicBranch,
      activeStrategicNote = moment.activeStrategicNote,
      activeStrategicSourceMode = moment.activeStrategicSourceMode,
      activeStrategicIdeas = moment.activeStrategicIdeas,
      activeStrategicRoutes = moment.activeStrategicRoutes,
      activeStrategicMoves = moment.activeStrategicMoves,
      activeDirectionalTargets = moment.activeDirectionalTargets,
      activeBranchDossier = moment.activeBranchDossier,
      strategicThread = moment.strategicThread
    )

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
