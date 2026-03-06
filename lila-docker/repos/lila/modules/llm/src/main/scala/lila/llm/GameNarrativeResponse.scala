package lila.llm

import play.api.libs.json.*
import lila.llm.model.{ FullGameNarrative, MomentNarrative }
import lila.llm.model.strategic.VariationLine

case class GameNarrativeResponse(
    schema: String,
    intro: String,
    moments: List[GameNarrativeMoment],
    conclusion: String,
    themes: List[String],
    review: Option[GameNarrativeReview] = None,
    sourceMode: String = "rule",
    model: Option[String] = None,
    planTier: String = PlanTier.Basic,
    llmLevel: String = LlmLevel.Polish
)

object GameNarrativeResponse:

  val schemaV2 = "chesstory.gameNarrative.v2"

  def fromNarrative(
      narrative: FullGameNarrative,
      review: Option[GameNarrativeReview] = None,
      sourceMode: String = "rule",
      model: Option[String] = None,
      planTier: String = PlanTier.Basic,
      llmLevel: String = LlmLevel.Polish
  ): GameNarrativeResponse =
    GameNarrativeResponse(
      schema = schemaV2,
      intro = narrative.gameIntro,
      moments = narrative.keyMomentNarratives.map(GameNarrativeMoment.fromMoment),
      conclusion = narrative.conclusion,
      themes = narrative.overallThemes,
      review = review,
      sourceMode = sourceMode,
      model = model,
      planTier = PlanTier.normalize(planTier),
      llmLevel = LlmLevel.normalize(llmLevel)
    )

  given Writes[GameNarrativeResponse] = Json.writes[GameNarrativeResponse]

case class GameNarrativeMoment(
    momentId: String,
    ply: Int,
    moveNumber: Int,
    side: String,
    moveClassification: Option[String],
    momentType: String,
    fen: String,
    narrative: String,
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
    authorQuestions: List[AuthorQuestionSummary] = Nil,
    authorEvidence: List[AuthorEvidenceSummary] = Nil,
    mainStrategicPlans: List[lila.llm.model.authoring.PlanHypothesis] = Nil,
    latentPlans: List[lila.llm.model.authoring.LatentPlanNarrative] = Nil,
    whyAbsentFromTopMultiPV: List[String] = Nil,
    strategicBranch: Boolean = false,
    activeStrategicNote: Option[String] = None,
    activeStrategicSourceMode: Option[String] = None,
    activeStrategicRoutes: List[ActiveStrategicRouteRef] = Nil,
    activeStrategicMoves: List[ActiveStrategicMoveRef] = Nil
)

object GameNarrativeMoment:

  def fromMoment(moment: MomentNarrative): GameNarrativeMoment =
    val moveNum = (moment.ply + 1) / 2
    val side = if (moment.ply % 2 == 1) "white" else "black"
    GameNarrativeMoment(
      momentId = s"ply_${moment.ply}_${moment.momentType.toLowerCase}",
      ply = moment.ply,
      moveNumber = moveNum,
      side = side,
      moveClassification = moment.moveClassification,
      momentType = moment.momentType,
      fen = moment.analysisData.fen,
      narrative = moment.narrative,
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
      authorQuestions = moment.authorQuestions,
      authorEvidence = moment.authorEvidence,
      mainStrategicPlans = moment.mainStrategicPlans,
      latentPlans = moment.latentPlans,
      whyAbsentFromTopMultiPV = moment.whyAbsentFromTopMultiPV,
      strategicBranch = moment.strategicBranch,
      activeStrategicNote = moment.activeStrategicNote,
      activeStrategicSourceMode = moment.activeStrategicSourceMode,
      activeStrategicRoutes = moment.activeStrategicRoutes,
      activeStrategicMoves = moment.activeStrategicMoves
    )

  given Writes[GameNarrativeMoment] = Json.writes[GameNarrativeMoment]

case class GameNarrativeReview(
    schemaVersion: Int,
    reviewPerspective: String,
    totalPlies: Int,
    evalCoveredPlies: Int,
    evalCoveragePct: Int,
    selectedMoments: Int,
    selectedMomentPlies: List[Int],
    blundersCount: Int,
    missedWinsCount: Int,
    brilliantMovesCount: Int,
    accuracyWhite: Option[Double],
    accuracyBlack: Option[Double],
    momentTypeCounts: Map[String, Int]
)

object GameNarrativeReview:
  given Writes[GameNarrativeReview] = Json.writes[GameNarrativeReview]

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
