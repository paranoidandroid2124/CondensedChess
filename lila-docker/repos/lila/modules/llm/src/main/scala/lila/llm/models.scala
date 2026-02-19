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
    afterVariations: Option[List[lila.llm.model.strategic.VariationLine]] = None
)
object CommentRequest:
  given Reads[CommentRequest] = Json.reads[CommentRequest]

case class CommentResponse(
  commentary: String,
  concepts: List[String],
  variations: List[lila.llm.model.strategic.VariationLine] = Nil,
  probeRequests: List[lila.llm.model.ProbeRequest] = Nil
)
object CommentResponse:
  given Writes[CommentResponse] = Json.writes[CommentResponse]

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
