package lila.llm

import play.api.libs.json.*
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.DefaultBodyReadables.*
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.*

/** LLM Client for Gemini API integration.
  * Provides chess commentary generation.
  */
final class LlmClient(ws: StandaloneWSClient, config: LlmConfig)(using ec: ExecutionContext):

  private val endpoint = s"https://generativelanguage.googleapis.com/v1beta/models/${config.model}:generateContent"

  def commentPositionRefined(request: CommentRequest, _probeResults: Any): Future[Option[CommentResponse]] = 
    commentPosition(request) // Fallback for now

  /** Generate commentary for a single position */
  def commentPosition(request: CommentRequest): Future[Option[CommentResponse]] =
    if !config.enabled then Future.successful(None)
    else
      val prompt = buildPrompt(request)
      callGemini(prompt).map(parseCommentResponse)

  /** Generate full game analysis commentary */
  def analyzeGame(request: FullAnalysisRequest): Future[Option[FullAnalysisResponse]] =
    if !config.enabled then Future.successful(None)
    else
      val prompt = buildFullAnalysisPrompt(request)
      callGemini(prompt).map(parseFullAnalysisResponse)

  private def buildPrompt(req: CommentRequest): String =
    val evalStr = req.eval.map(e => s"cp=${e.cp}, mate=${e.mate.getOrElse("none")}").getOrElse("unknown")
    val pv = req.eval.flatMap(_.pv).getOrElse(Nil)
    val openingStr = req.context.opening.getOrElse("Unknown")
    val lastMoveStr = req.lastMove.getOrElse("(game start)")
    
    val assessment = lila.llm.analysis.CommentaryEngine.assess(
      fen = req.fen,
      pv = pv,
      opening = req.context.opening,
      phase = Some(req.context.phase)
    )

    val enrichedContext = assessment.map(lila.llm.analysis.CommentaryEngine.formatSparse).getOrElse("")

    s"""You are a specialized chess coach and author providing deep insights.

## POSITION INFO
- FEN: ${req.fen}
- Last Move: $lastMoveStr
- Evaluation: $evalStr
- Opening: $openingStr
- Phase: ${req.context.phase} (Ply: ${req.context.ply})

## EXPERT ANALYSIS
$enrichedContext

## RULES
- Write 1-2 sentences of insightful commentary in a "chess book" style.
- Focus on the strategic plans and motifs identified in the expert analysis.
- Connect the last move to these strategic goals.
- Do NOT hallucinate moves or plans not mentioned in the analysis.
- Maintain a professional, encouraging, and authoritative tone.

Output JSON: {"commentary": "...", "concepts": ["..."]}"""

  private def buildFullAnalysisPrompt(req: FullAnalysisRequest): String =
    val movesText = req.evals.map { e =>
      s"Ply ${e.ply}: cp=${e.cp}, pv=${e.pv.take(3).mkString(" ")}"
    }.mkString("\n")
    
    val focusStr = req.options.focusOn.mkString(", ")

    s"""You are a chess author writing a game summary.

PGN: ${req.pgn}

Evaluations:
$movesText

Focus on: $focusStr
Style: ${req.options.style}

RULES:
- Identify key moments (mistakes, turning points).
- Write a 3-5 sentence summary.
- For each critical move, provide annotation.
- Do NOT invent moves.

Output JSON:
{
  "summary": "...",
  "annotations": [{"ply": 12, "type": "mistake", "comment": "..."}],
  "concepts": ["..."]
}"""

  private def callGemini(prompt: String): Future[Option[String]] =
    val body = Json.obj(
      "contents" -> Json.arr(
        Json.obj("parts" -> Json.arr(Json.obj("text" -> prompt)))
      ),
      "generationConfig" -> Json.obj(
        "responseMimeType" -> "application/json",
        "temperature" -> 0.7
      )
    )

    ws.url(s"$endpoint?key=${config.apiKey}")
      .withRequestTimeout(60.seconds)
      .post(body)
      .map { response =>
        if response.status == 200 then
          extractText(response.body[String])
        else
          val bodyPreview = response.body[String].take(200)
          lila.log("llm").warn(s"Gemini API error: ${response.status} - $bodyPreview")
          None
      }
      .recover { case e: Throwable =>
        lila.log("llm").error(s"Gemini API call failed: ${e.getMessage}")
        None
      }

  private def extractText(body: String): Option[String] =
    try
      val json = Json.parse(body)
      (json \ "candidates" \ 0 \ "content" \ "parts" \ 0 \ "text").asOpt[String]
    catch
      case e: Throwable =>
        lila.log("llm").warn(s"Failed to parse Gemini response: ${e.getMessage}")
        None

  private def parseCommentResponse(textOpt: Option[String]): Option[CommentResponse] =
    textOpt.flatMap { text =>
      try
        val json = Json.parse(text)
        Some(CommentResponse(
          commentary = (json \ "commentary").as[String],
          concepts = (json \ "concepts").asOpt[List[String]].getOrElse(Nil)
        ))
      catch
        case _: Throwable => None
    }

  private def parseFullAnalysisResponse(textOpt: Option[String]): Option[FullAnalysisResponse] =
    textOpt.flatMap { text =>
      try
        val json = Json.parse(text)
        Some(FullAnalysisResponse(
          summary = (json \ "summary").as[String],
          annotations = (json \ "annotations").asOpt[List[Annotation]].getOrElse(Nil),
          concepts = (json \ "concepts").asOpt[List[String]].getOrElse(Nil)
        ))
      catch
        case _: Throwable => None
    }

// Request/Response models
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
    context: PositionContext
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

case class Annotation(ply: Int, `type`: String, comment: String)
object Annotation:
  given Reads[Annotation] = Json.reads[Annotation]
  given Writes[Annotation] = Json.writes[Annotation]

case class FullAnalysisResponse(
    summary: String,
    annotations: List[Annotation],
    concepts: List[String]
)
object FullAnalysisResponse:
  given Writes[FullAnalysisResponse] = Json.writes[FullAnalysisResponse]
