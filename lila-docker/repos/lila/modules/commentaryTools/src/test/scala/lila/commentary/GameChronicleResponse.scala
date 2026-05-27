package lila.commentary

import play.api.libs.json.*
import lila.commentary.analysis.{ DecisionFrameCarrierInput, DecisionFrameDossierInput, PlayerFacingTruthMode }
import lila.commentary.model.{ GameArc, GameArcMoment }
import lila.commentary.model.strategic.VariationLine

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
    commentaryMode: String = CommentaryMode.Polish,
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
      commentaryMode: String = CommentaryMode.Polish
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
      commentaryMode = CommentaryMode.normalize(commentaryMode),
      strategicThreads = Nil
    )

  given Writes[GameChronicleResponse] = Json.writes[GameChronicleResponse]

case class ActivePlanRef(
    themeL1: String,
    subplanId: Option[String],
    phase: Option[String],
    commitmentScore: Option[Double]
)
object ActivePlanRef:
  given Writes[ActivePlanRef] = Json.writes[ActivePlanRef]

case class ActiveBranchDossier(
    dominantLens: String,
    chosenBranchLabel: String,
    engineBranchLabel: Option[String] = None,
    deferredBranchLabel: Option[String] = None,
    whyChosen: Option[String] = None,
    whyDeferred: Option[String] = None,
    opponentResource: Option[String] = None,
    routeCue: Option[ActiveBranchRouteCue] = None,
    moveCue: Option[ActiveBranchMoveCue] = None,
    evidenceCue: Option[String] = None,
    continuationFocus: Option[String] = None,
    practicalRisk: Option[String] = None,
    comparisonGapCp: Option[Int] = None,
    threadLabel: Option[String] = None,
    threadStage: Option[String] = None,
    threadSummary: Option[String] = None,
    threadOpponentCounterplan: Option[String] = None
):
  def decisionFrameInput: DecisionFrameDossierInput =
    DecisionFrameDossierInput(
      routeCue = routeCue,
      moveCue = moveCue,
      evidenceCue = evidenceCue,
      whyChosen = whyChosen,
      opponentResource = opponentResource,
      continuationFocus = continuationFocus,
      practicalRisk = practicalRisk
    )
object ActiveBranchDossier:
  given Writes[ActiveBranchDossier] = Json.writes[ActiveBranchDossier]

case class ActiveStrategicThread(
    threadId: String,
    side: String,
    themeKey: String,
    themeLabel: String,
    summary: String,
    seedPly: Int,
    lastPly: Int,
    representativePlies: List[Int] = Nil,
    opponentCounterplan: Option[String] = None,
    continuityScore: Double
)
object ActiveStrategicThread:
  given Writes[ActiveStrategicThread] = Json.writes[ActiveStrategicThread]

case class ActiveStrategicThreadRef(
    threadId: String,
    themeKey: String,
    themeLabel: String,
    stageKey: String,
    stageLabel: String
)
object ActiveStrategicThreadRef:
  given Writes[ActiveStrategicThreadRef] = Json.writes[ActiveStrategicThreadRef]

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
    collapse: Option[lila.commentary.model.CollapseAnalysis],
    strategyPack: Option[lila.commentary.StrategyPack] = None,
    signalDigest: Option[lila.commentary.NarrativeSignalDigest] = None,
    probeRequests: List[lila.commentary.model.ProbeRequest] = Nil,
    probeRefinementRequests: List[lila.commentary.model.ProbeRequest] = Nil,
    authorQuestions: List[AuthorQuestionSummary] = Nil,
    authorEvidence: List[AuthorEvidenceSummary] = Nil,
    mainStrategicPlans: List[lila.commentary.model.authoring.PlanHypothesis] = Nil,
    strategicPlanExperiments: List[lila.commentary.model.StrategicPlanExperiment] = Nil,
    strategicBranch: Boolean = false,
    activeStrategicNote: Option[String] = None,
    activeStrategicSourceMode: Option[String] = None,
    activeStrategicIdeas: List[ActiveStrategicIdeaRef] = Nil,
    activeStrategicRoutes: List[ActiveStrategicRouteRef] = Nil,
    activeStrategicMoves: List[ActiveStrategicMoveRef] = Nil,
    activeDirectionalTargets: List[StrategyDirectionalTarget] = Nil,
    activeBranchDossier: Option[ActiveBranchDossier] = None,
    strategicThread: Option[ActiveStrategicThreadRef] = None
):
  def decisionFrameInput(truthMode: PlayerFacingTruthMode = PlayerFacingTruthMode.Minimal): DecisionFrameCarrierInput =
    DecisionFrameCarrierInput(
      side = side,
      strategyPack = strategyPack,
      signalDigest = signalDigest,
      mainStrategicPlans = mainStrategicPlans,
      strategicPlanExperiments = strategicPlanExperiments,
      truthMode = truthMode,
      tensionScore =
        Some(
          signalDigest.flatMap(_.counterplayScoreDrop).getOrElse(
            signalDigest.flatMap(_.decisionComparison).flatMap(_.cpLossVsChosen)
              .orElse(topEngineMove.flatMap(_.cpLossVsPlayed))
              .getOrElse(0)
          )
        )
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
      activePlan = None,
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
      strategicBranch = moment.strategicBranch
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
