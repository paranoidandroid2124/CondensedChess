package lila.llm

import play.api.libs.json.*
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.DefaultBodyReadables.*
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.*

import lila.llm.model.{ OpeningEvent, OpeningEventBudget, OpeningReference, ProbeRequest, ProbeResult }

/** LLM Client for Gemini API integration.
  * Provides chess commentary generation.
  */
final class LlmClient(ws: StandaloneWSClient, config: LlmConfig)(using ec: ExecutionContext):

  private val endpoint = s"https://generativelanguage.googleapis.com/v1beta/models/${config.model}:generateContent"
  private val explorer = new lila.llm.analysis.OpeningExplorerClient(ws)

  /** Generate commentary for a single position */
  def commentPosition(request: CommentRequest): Future[Option[CommentResponse]] =
    if !config.enabled then Future.successful(None)
    else if (request.probeResults.nonEmpty) then
      commentPositionRefined(request, request.probeResults)
    else
      explorer.fetchMasters(request.fen).flatMap { openingRef =>
        val (prompt, probeReqs) = buildPrompt(request, openingRef)
        val vars = buildVariations(request)
        
        callGemini(prompt).map: textOpt =>
          parseCommentResponse(textOpt).map(_.copy(
            variations = vars,
            probeRequests = probeReqs
          ))
      }

  /** Generate commentary enriched with probe results */
  def commentPositionRefined(request: CommentRequest, probeResults: List[lila.llm.model.ProbeResult]): Future[Option[CommentResponse]] =
    if !config.enabled then Future.successful(None)
    else
      // 1. Recalculate expected probes for stateless validation
      val variations = buildVariations(request)
      val extendedData = lila.llm.analysis.CommentaryEngine.assessExtended(
        fen = request.fen,
        variations = variations,
        playedMove = request.lastMove,
        opening = request.context.opening,
        phase = Some(request.context.phase),
        ply = request.context.ply,
        prevMove = request.lastMove
      )
      
      val expectedRequests = extendedData.map(d => lila.llm.analysis.NarrativeContextBuilder.build(d, d.toContext, None).probeRequests).getOrElse(Nil)
      val expectedMoves = expectedRequests.flatMap(_.moves).toSet
      
      // 2. Filter valid results (security: only allow results for moves we actually requested)
      val validResults = probeResults.filter(pr => pr.probedMove.exists(expectedMoves.contains))
      
      if (validResults.isEmpty) then 
        // fallback to normal if no valid probes
        commentPosition(request.copy(probeResults = Nil))
      else
        explorer.fetchMasters(request.fen).flatMap { openingRef =>
          val prompt = buildRefinedPrompt(request, validResults, extendedData, openingRef)
          callGemini(prompt).map: textOpt =>
            parseCommentResponse(textOpt).map(_.copy(variations = variations))
        }

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

      fetchOpeningEventsForGame(request.pgn).flatMap { openingEvents =>
        val semanticData = lila.llm.analysis.CommentaryEngine.analyzeGame(
          pgn = request.pgn,
          evals = evalsMap
        )

        val prompt = buildFullAnalysisPrompt(request, semanticData, openingEvents)
        callGemini(prompt).map(parseFullAnalysisResponse)
      }

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

    def toResponse(narrative: lila.llm.model.FullGameNarrative): FullAnalysisResponse =
      FullAnalysisResponse(
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

    // A9: Prefetch Masters DB references for the early opening window (used for event scanning).
    lila.llm.PgnAnalysisHelper.extractPlyData(request.pgn) match
      case Left(_) =>
        val narrative = lila.llm.analysis.CommentaryEngine.generateFullGameNarrative(
          pgn = request.pgn,
          evals = evalsMap
        )
        Future.successful(Some(toResponse(narrative)))
      case Right(plyDataList) =>
        // A9: Fetch only the early opening window needed for event scanning (avoid per-move spam).
        val maxOpeningScanPly = 20
        val maxMastersFetchFens = 20

        val openingScanFens = plyDataList
          .takeWhile(_.ply <= maxOpeningScanPly)
          .map(_.fen)
          .distinct
          .take(maxMastersFetchFens)

        Future
          .traverse(openingScanFens)(fen => explorer.fetchMasters(fen).map(fen -> _))
          .map { pairs =>
            val openingRefsByFen = pairs.collect { case (fen, Some(ref)) => fen -> ref }.toMap

            val narrative = lila.llm.analysis.CommentaryEngine.generateFullGameNarrative(
              pgn = request.pgn,
              evals = evalsMap,
              openingRefsByFen = openingRefsByFen
            )

            Some(toResponse(narrative))
          }

  private def buildPrompt(req: CommentRequest, openingRef: Option[lila.llm.model.OpeningReference]): (String, List[ProbeRequest]) =
    val evalStr = req.eval.map(e => s"cp=${e.cp}, mate=${e.mate.getOrElse("none")}").getOrElse("unknown")
    val openingStr = req.context.opening.getOrElse("Unknown")
    val lastMoveStr = req.lastMove.getOrElse("(game start)")
    
    val variations: List[lila.llm.model.strategic.VariationLine] = buildVariations(req)
    
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

    val narrativeCtx = extendedData.map(d => lila.llm.analysis.NarrativeContextBuilder.build(d, d.toContext, None, Nil, openingRef))
    val enrichedContext = narrativeCtx.map(ctx => lila.llm.analysis.BookStyleRenderer.renderFull(ctx)).getOrElse("Analysis unavailable.")
    val probeRequests = narrativeCtx.map(_.probeRequests).getOrElse(Nil)

    val prompt = s"""You are a specialized chess coach and author providing deep insights.
|
|## POSITION INFO
|- FEN: ${req.fen}
|- Last Move: $lastMoveStr
|- Evaluation: $evalStr
|- Opening: $openingStr
|- Phase: ${req.context.phase} (Ply: ${req.context.ply})
|
|## STRUCTURED ANALYSIS DATA (APPENDIX)
|$enrichedContext
|
|## RULES
|- Write a detailed, engaging paragraph (3-5 sentences) in **Chess Book style**.
|- **Data Navigation**:
|    - Check `## Threats` for urgent dangers (especially `lossIfIgnoredCp` > 100).
|    - Check `## Plans` for the strategic direction (Top 5).
|    - Check `## Semantic Layer` for positional details (Weaknesses, Activity).
|- Use **bold** for key moves and concepts.
|- Focus strictly on the STRATEGIC FACTORS identified in the Appendix.
|- If the move was a mistake, explicitly compare it to the better **Alternatives** listed.
|- Connect the last move to the practical assessment (e.g., "White ignores the threat...").
|- Do NOT hallucinate.
|
|Output JSON: {"commentary": "...", "concepts": ["..."]}""".stripMargin
    (prompt, probeRequests)

  private def buildRefinedPrompt(req: CommentRequest, probeResults: List[ProbeResult], extendedData: Option[lila.llm.model.ExtendedAnalysisData], openingRef: Option[lila.llm.model.OpeningReference]): String =
    val narrativeCtx = extendedData.map(d => lila.llm.analysis.NarrativeContextBuilder.build(d, d.toContext, None, probeResults, openingRef))
    val enrichedContext = narrativeCtx.map(ctx => lila.llm.analysis.BookStyleRenderer.renderFull(ctx)).getOrElse("Analysis unavailable.")

    s"""You are a specialized chess coach and author providing deep insights.
       |Enriched with verified engine evidence for alternative lines.
       |
       |## POSITION INFO
       |- FEN: ${req.fen}
       |- Last Move: ${req.lastMove.getOrElse("(start)")}
       |- Evaluation: ${req.eval.map(e => s"cp=${e.cp}").getOrElse("?")}
       |- Opening: ${req.context.opening.getOrElse("Unknown")}
       |- Phase: ${req.context.phase}
       |
       |## STRUCTURED ANALYSIS DATA (APPENDIX + PROBES)
       |$enrichedContext
       |
       |## RULES
       |- Write a final, definitive book-style commentary.
       |- **Use the Appendix Data**:
       |    - `## Threats`: Mention specific threats if "URGENT" or "IMPORTANT".
       |    - `## Candidates`: Use "WhyNot" evidence to explain why specific alternatives fail.
       |    - `## Semantic Layer`: Cite specific weaknesses (e.g., "weak f7 square") or activity.
       |- Be authoritative and instructive.
       |- Output JSON: {"commentary": "...", "concepts": ["..."]}
       |""".stripMargin

  private def buildVariations(req: CommentRequest): List[lila.llm.model.strategic.VariationLine] =
    req.eval.fold(Nil): e =>
      if e.variations.nonEmpty then
        e.variations.map: v =>
          lila.llm.model.strategic.VariationLine(
            moves = v.moves,
            scoreCp = v.scoreCp,
            mate = v.mate,
            depth = v.depth,
            tags = Nil
          )
      else
        e.pv.getOrElse(Nil) match
          case Nil => Nil
          case moves =>
            List(
              lila.llm.model.strategic.VariationLine(
                moves = moves,
                scoreCp = e.cp,
                mate = e.mate,
                depth = e.depth,
                resultingFen = Some(lila.llm.analysis.NarrativeUtils.uciListToFen(req.fen, moves)),
                tags = Nil
              )
            )

  private def buildFullAnalysisPrompt(
      req: FullAnalysisRequest,
      semantic: List[lila.llm.model.ExtendedAnalysisData],
      openingEvents: List[OpeningEvent]
  ): String =
    val movesText = req.evals.map { e =>
      s"Ply ${e.ply}: cp=${e.cp}, pv=${e.pv.take(3).mkString(" ")}"
    }.mkString("\n")
    
    val semanticText = semantic.map { data =>
      val desc = lila.llm.NarrativeGenerator.describeExtended(data)
      s"--- Ply ${data.ply} ---\n$desc"
    }.mkString("\n")
    
    val focusStr = req.options.focusOn.mkString(", ")

    val openingText =
      if (openingEvents.nonEmpty) then
        val lines = openingEvents.map {
          case OpeningEvent.Intro(eco, name, theme, topMoves) =>
            s"- [INTRO] $eco: $name | Theme: $theme | Main: ${topMoves.mkString(", ")}"
          case OpeningEvent.BranchPoint(moves, reason, sampleGame) =>
            s"- [BRANCH] $reason | Options: ${moves.mkString(", ")}${sampleGame.fold("")(g => s" | Example: $g")}"
          case OpeningEvent.OutOfBook(playedMove, topMoves, ply) =>
            s"- [OUT OF BOOK] ply $ply: $playedMove (theory: ${topMoves.mkString(", ")})"
          case OpeningEvent.TheoryEnds(lastPly, sampleCount) =>
            s"- [THEORY ENDS] ply $lastPly (samples: $sampleCount)"
          case OpeningEvent.Novelty(move, cpLoss, evidence, ply) =>
            s"- [NOVELTY] ply $ply: $move (loss $cpLoss cp) | $evidence"
        }.mkString("\n")

        s"""OPENING REFERENCES (MASTERS):
           |$lines
           |""".stripMargin
      else ""

    s"""You are a chess author writing a game summary.

PGN: ${req.pgn}

Evaluations:
$movesText

KEY MOMENTS & EXPERT INSIGHTS:
$semanticText

$openingText

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

  private def fetchOpeningEventsForGame(pgn: String): Future[List[OpeningEvent]] =
    lila.llm.PgnAnalysisHelper.extractPlyData(pgn) match
      case Left(_) => Future.successful(Nil)
      case Right(plyDataList) =>
        val maxScanPly = 20
        val maxFetchFens = 20

        val scanPlies = plyDataList.takeWhile(_.ply <= maxScanPly)
        val fensToFetch = scanPlies.map(_.fen).distinct.take(maxFetchFens)

        Future
          .traverse(fensToFetch)(fen => explorer.fetchMasters(fen).map(fen -> _))
          .map { pairs =>
            val refsByFen = pairs.collect { case (fen, Some(ref)) => fen -> ref }.toMap

            val events = scala.collection.mutable.ListBuffer.empty[OpeningEvent]
            var budget = OpeningEventBudget()
            var prevRef: Option[OpeningReference] = None

            val it = scanPlies.iterator
            while (it.hasNext && !budget.theoryEnded) {
              val plyData = it.next()
              val refOpt = refsByFen.get(plyData.fen)

              val eventOpt = lila.llm.analysis.OpeningEventDetector.detect(
                ply = plyData.ply,
                playedMove = Some(plyData.playedUci),
                fen = plyData.fen,
                ref = refOpt,
                budget = budget,
                cpLoss = None,
                hasConstructiveEvidence = false,
                prevRef = prevRef
              )

              eventOpt.foreach { ev =>
                events += ev
                budget = ev match
                  case OpeningEvent.Intro(_, _, _, _) => budget.afterIntro
                  case OpeningEvent.TheoryEnds(_, _) => budget.afterTheoryEnds
                  case _ => budget.afterEvent
              }

              if (eventOpt.isEmpty) {
                budget = budget.updatePly(plyData.ply)
              }

              prevRef = refOpt.orElse(prevRef)
            }

            events.toList
          }

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
        val variationsJson = (json \ "variations").asOpt[List[lila.llm.model.strategic.VariationLine]].getOrElse(Nil)
        Some(CommentResponse(
          commentary = (json \ "commentary").as[String],
          concepts = (json \ "concepts").asOpt[List[String]].getOrElse(Nil),
          variations = variationsJson
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
case class EvalData(cp: Int, mate: Option[Int], pv: Option[List[String]], depth: Int = 0, variations: List[AnalysisVariation] = Nil)
object EvalData:
  // Custom Reads for backward compatibility (depth may be missing from older clients)
  given Reads[EvalData] = new Reads[EvalData]:
    def reads(json: JsValue): JsResult[EvalData] =
      for
        cp <- (json \ "cp").validate[Int]
        mate <- (json \ "mate").validateOpt[Int]
        pv <- (json \ "pv").validateOpt[List[String]]
        depth <- JsSuccess((json \ "depth").asOpt[Int].getOrElse(0))
        variations <- (json \ "variations").validateOpt[List[AnalysisVariation]].map(_.getOrElse(Nil))
      yield EvalData(cp, mate, pv, depth, variations)
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
    probeResults: List[ProbeResult] = Nil // NEW: for refined commentary
)
object CommentRequest:
  given Reads[CommentRequest] = Json.reads[CommentRequest]

case class CommentResponse(
    commentary: String, 
    concepts: List[String],
    variations: List[lila.llm.model.strategic.VariationLine] = Nil,
    probeRequests: List[ProbeRequest] = Nil // NEW: for client-side probing
)
object CommentResponse:
  given Writes[CommentResponse] = Json.writes[CommentResponse]

case class AnalysisVariation(moves: List[String], scoreCp: Int, mate: Option[Int], depth: Int = 0)
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
    depth: Int = 0,
    best: Option[String] = None,  // UCI of best move
    variation: Option[String] = None, // Space-separated UCI moves
    pv: List[String] = Nil,
    variations: List[AnalysisVariation] = Nil
):
  // Helper to get variations from either variations list or variation string
  def getVariations: List[AnalysisVariation] =
    if variations.nonEmpty then variations
    else variation.map { v =>
      List(AnalysisVariation(v.split(" ").toList, cp, mate, depth))
    }.getOrElse(Nil)

object MoveEval:
  // Custom Reads to handle BOTH flat and nested eval formats
  given Reads[MoveEval] = new Reads[MoveEval]:
    def reads(json: JsValue): JsResult[MoveEval] =
      val ply = (json \ "ply").as[Int]
      val fen = (json \ "fen").asOpt[String]
      
      // Check if eval is nested: { eval: { cp, mate, best, variation } }
      val evalObj = (json \ "eval").toOption
      
      val (cp, mate, depth, best, variation) = evalObj match
        case Some(e) =>
          (
            (e \ "cp").asOpt[Int].getOrElse(0),
            (e \ "mate").asOpt[Int],
            (e \ "depth").asOpt[Int].getOrElse(0),
            (e \ "best").asOpt[String],
            (e \ "variation").asOpt[String]
          )
        case None =>
          // Flat format
          (
            (json \ "cp").asOpt[Int].getOrElse(0),
            (json \ "mate").asOpt[Int],
            (json \ "depth").asOpt[Int].getOrElse(0),
            (json \ "best").asOpt[String],
            (json \ "variation").asOpt[String]
          )
      
      val pv = (json \ "pv").asOpt[List[String]].getOrElse(Nil)
      val variations = (json \ "variations").asOpt[List[AnalysisVariation]].getOrElse(Nil)
      
      JsSuccess(MoveEval(ply, fen, cp, mate, depth, best, variation, pv, variations))

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
