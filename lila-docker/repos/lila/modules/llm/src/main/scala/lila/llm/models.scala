package lila.llm

import play.api.libs.json.*

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
    planStateToken: Option[lila.llm.analysis.PlanStateTracker] = None
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
    estimatedCostUsd: Option[Double]
)
object PolishMetaV1:
  given Writes[PolishMetaV1] = Json.writes[PolishMetaV1]

case class CommentResponse(
  commentary: String,
  concepts: List[String],
  variations: List[lila.llm.model.strategic.VariationLine] = Nil,
  probeRequests: List[lila.llm.model.ProbeRequest] = Nil,
  mainStrategicPlans: List[lila.llm.model.authoring.PlanHypothesis] = Nil,
  latentPlans: List[lila.llm.model.authoring.LatentPlanNarrative] = Nil,
  whyAbsentFromTopMultiPV: List[String] = Nil,
  planStateToken: Option[lila.llm.analysis.PlanStateTracker] = None,
  sourceMode: String = "rule",
  model: Option[String] = None,
  refs: Option[BookmakerRefsV1] = None,
  polishMeta: Option[PolishMetaV1] = None
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
