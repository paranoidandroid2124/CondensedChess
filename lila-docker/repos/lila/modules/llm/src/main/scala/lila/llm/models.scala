package lila.llm

import play.api.libs.json.*

object PlanTier:
  val Basic = "basic"
  val Pro = "pro"

  def normalize(raw: String): String =
    Option(raw).map(_.trim.toLowerCase) match
      case Some(Pro) => Pro
      case _         => Basic

object LlmLevel:
  val Polish = "polish"
  val Active = "active"

  def normalize(raw: String): String =
    Option(raw).map(_.trim.toLowerCase) match
      case Some(Active) => Active
      case _            => Polish

case class EvalData(cp: Int, mate: Option[Int], pv: Option[List[String]])
object EvalData:
  given Reads[EvalData] = Json.reads[EvalData]
  given Writes[EvalData] = Json.writes[EvalData]

case class PositionContext(opening: Option[String], phase: String, ply: Int)
object PositionContext:
  given Reads[PositionContext] = Json.reads[PositionContext]
  given Writes[PositionContext] = Json.writes[PositionContext]

case class CommentRequest(
    fen: String,
    lastMove: Option[String],
    eval: Option[EvalData],
    context: PositionContext,
    // Bookmaker Stage 2: optional MultiPV payload (sent by client-side Stockfish)
    variations: Option[List[lila.llm.model.strategic.VariationLine]] = None,
    // Bookmaker Stage 2 (optional): probe evidence from client to enable a1/a2 sub-branches
    probeResults: Option[List[lila.llm.model.ProbeResult]] = None,
    // Bookmaker Stage 2 (optional): explorer data from client to bypass server-side I/O
    openingData: Option[lila.llm.model.OpeningReference] = None,
    // Bookmaker delta: optional post-move position to compute before/after differences.
    afterFen: Option[String] = None,
    afterEval: Option[EvalData] = None,
    afterVariations: Option[List[lila.llm.model.strategic.VariationLine]] = None,
    // State Passing: persist plan state between individual move queries
    planStateToken: Option[lila.llm.analysis.PlanStateTracker] = None,
    endgameStateToken: Option[lila.llm.model.strategic.EndgamePatternState] = None
)
object CommentRequest:
  given Reads[CommentRequest] = Json.reads[CommentRequest]

case class MoveRefV1(
    refId: String,
    san: String,
    uci: String,
    fenAfter: String,
    ply: Int,
    moveNo: Int,
    marker: Option[String]
)
object MoveRefV1:
  given Writes[MoveRefV1] = Json.writes[MoveRefV1]

case class VariationRefV1(
    lineId: String,
    scoreCp: Int,
    mate: Option[Int],
    depth: Int,
    moves: List[MoveRefV1]
)
object VariationRefV1:
  given Writes[VariationRefV1] = Json.writes[VariationRefV1]

case class BookmakerRefsV1(
    schema: String = "chesstory.refs.v1",
    startFen: String,
    startPly: Int,
    variations: List[VariationRefV1]
)
object BookmakerRefsV1:
  given Writes[BookmakerRefsV1] = Json.writes[BookmakerRefsV1]

case class PolishMetaV1(
    provider: String,
    model: Option[String],
    sourceMode: String,
    validationPhase: String,
    validationReasons: List[String],
    cacheHit: Boolean,
    promptTokens: Option[Int],
    cachedTokens: Option[Int],
    completionTokens: Option[Int],
    estimatedCostUsd: Option[Double],
    strategyCoverage: Option[StrategyCoverageMetaV1] = None
)
object PolishMetaV1:
  given Writes[PolishMetaV1] = Json.writes[PolishMetaV1]

case class StrategyCoverageMetaV1(
    mode: String,
    enforced: Boolean,
    threshold: Double,
    availableCategories: Int,
    coveredCategories: Int,
    requiredCategories: Int,
    coverageScore: Double,
    passesThreshold: Boolean,
    planSignals: Int,
    planHits: Int,
    routeSignals: Int,
    routeHits: Int,
    focusSignals: Int,
    focusHits: Int
)
object StrategyCoverageMetaV1:
  given Writes[StrategyCoverageMetaV1] = Json.writes[StrategyCoverageMetaV1]

case class StrategySidePlan(
    side: String,
    horizon: String,
    planName: String,
    priorities: List[String] = Nil,
    riskTriggers: List[String] = Nil
)
object StrategySidePlan:
  given Writes[StrategySidePlan] = Json.writes[StrategySidePlan]

case class StrategyPieceRoute(
    side: String,
    piece: String,
    from: String,
    route: List[String],
    purpose: String,
    confidence: Double,
    evidence: List[String] = Nil
)
object StrategyPieceRoute:
  given Writes[StrategyPieceRoute] = Json.writes[StrategyPieceRoute]

case class NarrativeSignalDigest(
    opening: Option[String] = None,
    strategicStack: List[String] = Nil,
    latentPlan: Option[String] = None,
    latentReason: Option[String] = None,
    authoringEvidence: Option[String] = None,
    practicalVerdict: Option[String] = None,
    practicalFactors: List[String] = Nil,
    compensation: Option[String] = None,
    compensationVectors: List[String] = Nil,
    investedMaterial: Option[Int] = None,
    structuralCue: Option[String] = None,
    structureProfile: Option[String] = None,
    centerState: Option[String] = None,
    alignmentBand: Option[String] = None,
    alignmentReasons: List[String] = Nil,
    prophylaxisPlan: Option[String] = None,
    prophylaxisThreat: Option[String] = None,
    counterplayScoreDrop: Option[Int] = None,
    decision: Option[String] = None,
    strategicFlow: Option[String] = None,
    opponentPlan: Option[String] = None,
    preservedSignals: List[String] = Nil
)
object NarrativeSignalDigest:
  given Writes[NarrativeSignalDigest] = Json.writes[NarrativeSignalDigest]

case class StrategyPack(
    schema: String = "chesstory.strategyPack.v1",
    sideToMove: String,
    plans: List[StrategySidePlan] = Nil,
    pieceRoutes: List[StrategyPieceRoute] = Nil,
    longTermFocus: List[String] = Nil,
    evidence: List[String] = Nil,
    signalDigest: Option[NarrativeSignalDigest] = None
)
object StrategyPack:
  given Writes[StrategyPack] = Json.writes[StrategyPack]

case class ActiveStrategicRouteRef(
    routeId: String,
    piece: String,
    route: List[String],
    purpose: String,
    confidence: Double
)
object ActiveStrategicRouteRef:
  given Writes[ActiveStrategicRouteRef] = Json.writes[ActiveStrategicRouteRef]

case class ActiveStrategicMoveRef(
    label: String,
    source: String,
    uci: String,
    san: Option[String] = None,
    fenAfter: Option[String] = None
)
object ActiveStrategicMoveRef:
  given Writes[ActiveStrategicMoveRef] = Json.writes[ActiveStrategicMoveRef]

case class AuthorQuestionSummary(
    id: String,
    kind: String,
    priority: Int,
    question: String,
    why: Option[String] = None,
    anchors: List[String] = Nil,
    confidence: String,
    latentPlanName: Option[String] = None,
    latentSeedId: Option[String] = None
)
object AuthorQuestionSummary:
  given Writes[AuthorQuestionSummary] = Json.writes[AuthorQuestionSummary]

case class EvidenceBranchSummary(
    keyMove: String,
    line: String,
    evalCp: Option[Int] = None,
    mate: Option[Int] = None,
    depth: Option[Int] = None,
    sourceId: Option[String] = None
)
object EvidenceBranchSummary:
  given Writes[EvidenceBranchSummary] = Json.writes[EvidenceBranchSummary]

case class AuthorEvidenceSummary(
    questionId: String,
    questionKind: String,
    question: String,
    why: Option[String] = None,
    status: String,
    purposes: List[String] = Nil,
    branchCount: Int = 0,
    branches: List[EvidenceBranchSummary] = Nil,
    pendingProbeIds: List[String] = Nil,
    pendingProbeCount: Int = 0,
    probeObjectives: List[String] = Nil,
    linkedPlans: List[String] = Nil
)
object AuthorEvidenceSummary:
  given Writes[AuthorEvidenceSummary] = Json.writes[AuthorEvidenceSummary]

case class CommentResponse(
  commentary: String,
  concepts: List[String],
  variations: List[lila.llm.model.strategic.VariationLine] = Nil,
  probeRequests: List[lila.llm.model.ProbeRequest] = Nil,
  authorQuestions: List[AuthorQuestionSummary] = Nil,
  authorEvidence: List[AuthorEvidenceSummary] = Nil,
  mainStrategicPlans: List[lila.llm.model.authoring.PlanHypothesis] = Nil,
  latentPlans: List[lila.llm.model.authoring.LatentPlanNarrative] = Nil,
  whyAbsentFromTopMultiPV: List[String] = Nil,
  planStateToken: Option[lila.llm.analysis.PlanStateTracker] = None,
  endgameStateToken: Option[lila.llm.model.strategic.EndgamePatternState] = None,
  sourceMode: String = "rule",
  model: Option[String] = None,
  refs: Option[BookmakerRefsV1] = None,
  polishMeta: Option[PolishMetaV1] = None,
  planTier: String = PlanTier.Basic,
  llmLevel: String = LlmLevel.Polish,
  strategyPack: Option[StrategyPack] = None,
  signalDigest: Option[NarrativeSignalDigest] = None
)
object CommentResponse:
  given Writes[CommentResponse] = Json.writes[CommentResponse]

case class BookmakerResult(
    response: CommentResponse,
    cacheHit: Boolean
)

case class AsyncGameAnalysisSubmitResponse(
    jobId: String,
    status: String
)
object AsyncGameAnalysisSubmitResponse:
  given Writes[AsyncGameAnalysisSubmitResponse] = Json.writes[AsyncGameAnalysisSubmitResponse]

case class AsyncGameAnalysisStatusResponse(
    jobId: String,
    status: String,
    createdAtMs: Long,
    updatedAtMs: Long,
    result: Option[GameNarrativeResponse] = None,
    error: Option[String] = None
)
object AsyncGameAnalysisStatusResponse:
  given Writes[AsyncGameAnalysisStatusResponse] = Json.writes[AsyncGameAnalysisStatusResponse]

case class AnalysisOptions(style: String, focusOn: List[String])
object AnalysisOptions:
  given Reads[AnalysisOptions] = Json.reads[AnalysisOptions]

case class FullAnalysisRequest(
    pgn: String,
    evals: List[MoveEval],
    options: AnalysisOptions
)
object FullAnalysisRequest:
  given Reads[FullAnalysisRequest] = Json.reads[FullAnalysisRequest]
