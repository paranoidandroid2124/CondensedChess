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
      // Convert DTO variations to Domain variations
      val evalsMap = request.evals.map { me =>
        val vars = me.getVariations.map { v =>
          lila.llm.model.strategic.VariationLine(
            moves = v.moves,
            scoreCp = v.scoreCp,
            mate = v.mate,
            tags = Nil
          )
        }
        me.ply -> vars
      }.toMap

      val semanticData = lila.llm.analysis.CommentaryEngine.analyzeGame(
        pgn = request.pgn,
        evals = evalsMap
      )

      val prompt = buildFullAnalysisPrompt(request, semanticData)
      callGemini(prompt).map(parseFullAnalysisResponse)

  /** Generate full game analysis commentary locally (Rule-based / Book-Style Engine) */
  def analyzeGameLocal(request: FullAnalysisRequest): Future[Option[FullAnalysisResponse]] =
    
    // Convert DTO variations to Domain variations
    val evalsMap = request.evals.map { me =>
      val vars = me.getVariations.map { v =>
        lila.llm.model.strategic.VariationLine(
          moves = v.moves,
          scoreCp = v.scoreCp,
          mate = v.mate,
          tags = Nil
        )
      }
      me.ply -> vars
    }.toMap

    // Generate Narrative using Logic Engine (No Gemini)
    val narrative = lila.llm.analysis.CommentaryEngine.generateFullGameNarrative(
      pgn = request.pgn,
      evals = evalsMap
    )
    
    // Map to Response DTO
    val response = FullAnalysisResponse(
      summary = s"${narrative.gameIntro}\n\n${narrative.conclusion}",
      annotations = narrative.keyMomentNarratives.map { m =>
        Annotation(
          ply = m.ply,
          `type` = m.momentType.toLowerCase,
          comment = m.narrative
        )
      },
      concepts = narrative.overallThemes
    )
    
    Future.successful(Some(response))

  private def buildPrompt(req: CommentRequest): String =
    val evalStr = req.eval.map(e => s"cp=${e.cp}, mate=${e.mate.getOrElse("none")}").getOrElse("unknown")
    val openingStr = req.context.opening.getOrElse("Unknown")
    val lastMoveStr = req.lastMove.getOrElse("(game start)")

    val variations: List[lila.llm.model.strategic.VariationLine] = req.eval.map { e =>
       if (e.variations.nonEmpty) {
          e.variations.map { v =>
             lila.llm.model.strategic.VariationLine(
               moves = v.moves,
               scoreCp = v.scoreCp,
               mate = v.mate,
               tags = Nil
             )
          }
       } else {
          val moves = e.pv.getOrElse(Nil)
          if (moves.nonEmpty) List(lila.llm.model.strategic.VariationLine(moves, e.cp, e.mate, Nil)) else Nil
       }
    }.getOrElse(Nil)
    
    // Phase 2: Use Extended Assessment with Semantic Layer
    val extendedData = lila.llm.analysis.CommentaryEngine.assessExtended(
      fen = req.fen,
      variations = variations,
      playedMove = req.lastMove,
      opening = req.context.opening,
      phase = Some(req.context.phase),
      ply = req.context.ply,
      prevMove = req.lastMove
    )

    val enrichedContext = extendedData.map(d => lila.llm.NarrativeGenerator.describeExtended(d)).getOrElse("Analysis unavailable.")

    s"""You are a specialized chess coach and author providing deep insights.

## POSITION INFO
- FEN: ${req.fen}
- Last Move: $lastMoveStr
- Evaluation: $evalStr
- Opening: $openingStr
- Phase: ${req.context.phase} (Ply: ${req.context.ply})

## EXPERT SEMANTIC ANALYSIS
$enrichedContext

## RULES
- Write a detailed, engaging paragraph (3-5 sentences) in **Chess Book style**.
- Use **bold** for key moves and concepts.
- Focus strictly on the STRATEGIC FACTORS identified above.
- If the move was a mistake, explicitly compare it to the better **Alternatives** listed in the analysis.
- Connect the last move to the practical assessment (e.g., "White ignores the threat...").
- Do NOT hallucinate.

Output JSON: {"commentary": "...", "concepts": ["..."]}"""

  private def buildFullAnalysisPrompt(req: FullAnalysisRequest, semantic: List[lila.llm.model.ExtendedAnalysisData]): String =
    val movesText = req.evals.map { e =>
      s"Ply ${e.ply}: cp=${e.cp}, pv=${e.pv.take(3).mkString(" ")}"
    }.mkString("\n")
    
    val semanticText = semantic.map { data =>
      val desc = lila.llm.NarrativeGenerator.describeExtended(data)
      s"--- Ply ${data.ply} ---\n$desc"
    }.mkString("\n")
    
    val focusStr = req.options.focusOn.mkString(", ")

    s"""You are a chess author writing a game summary.

PGN: ${req.pgn}

Evaluations:
$movesText

KEY MOMENTS & EXPERT INSIGHTS:
$semanticText

Focus on: $focusStr
Style: ${req.options.style}

RULES:
- Identify key moments (mistakes, turning points) from the Key Moments list.
- Write a **comprehensive summary** of the game's narrative arc, highlighting the decisive strategic themes.
- For each critical move, provide an annotation explaining WHY it was critical, using the **Alternatives** data to show what should have been played.
- Use professional chess terminology.
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
case class EvalData(cp: Int, mate: Option[Int], pv: Option[List[String]], variations: List[AnalysisVariation] = Nil)
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

case class CommentResponse(commentary: String, concepts: List[String])
object CommentResponse:
  given Writes[CommentResponse] = Json.writes[CommentResponse]

case class AnalysisVariation(moves: List[String], scoreCp: Int, mate: Option[Int])
object AnalysisVariation:
  given Reads[AnalysisVariation] = Json.reads[AnalysisVariation]
  given Writes[AnalysisVariation] = Json.writes[AnalysisVariation]

// FIX 7: Updated MoveEval to accept frontend payload format
// Frontend sends: { ply, fen, eval: { cp, mate, best, variation } }
case class MoveEval(
    ply: Int, 
    fen: Option[String] = None,
    cp: Int = 0, 
    mate: Option[Int] = None,
    best: Option[String] = None,  // UCI of best move
    variation: Option[String] = None, // Space-separated UCI moves
    pv: List[String] = Nil,
    variations: List[AnalysisVariation] = Nil
):
  // Helper to get variations from either variations list or variation string
  def getVariations: List[AnalysisVariation] =
    if variations.nonEmpty then variations
    else variation.map { v =>
      List(AnalysisVariation(v.split(" ").toList, cp, mate))
    }.getOrElse(Nil)

object MoveEval:
  // Custom Reads to handle BOTH flat and nested eval formats
  given Reads[MoveEval] = new Reads[MoveEval]:
    def reads(json: JsValue): JsResult[MoveEval] =
      val ply = (json \ "ply").as[Int]
      val fen = (json \ "fen").asOpt[String]
      
      // Check if eval is nested: { eval: { cp, mate, best, variation } }
      val evalObj = (json \ "eval").toOption
      
      val (cp, mate, best, variation) = evalObj match
        case Some(e) =>
          (
            (e \ "cp").asOpt[Int].getOrElse(0),
            (e \ "mate").asOpt[Int],
            (e \ "best").asOpt[String],
            (e \ "variation").asOpt[String]
          )
        case None =>
          // Flat format
          (
            (json \ "cp").asOpt[Int].getOrElse(0),
            (json \ "mate").asOpt[Int],
            (json \ "best").asOpt[String],
            (json \ "variation").asOpt[String]
          )
      
      val pv = (json \ "pv").asOpt[List[String]].getOrElse(Nil)
      val variations = (json \ "variations").asOpt[List[AnalysisVariation]].getOrElse(Nil)
      
      JsSuccess(MoveEval(ply, fen, cp, mate, best, variation, pv, variations))

case class AnalysisOptions(style: String = "balanced", focusOn: List[String] = Nil)
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
