package lila.llm

import scala.concurrent.Future
import scala.util.control.NonFatal
import lila.llm.analysis.{ BookStyleRenderer, CommentaryEngine, NarrativeContextBuilder, NarrativeLexicon, NarrativeUtils, OpeningExplorerClient }
import lila.llm.analysis.NarrativeLexicon.Style
import lila.llm.model.OpeningReference
import lila.llm.model.strategic.VariationLine

/** High-level API for LLM commentary features.
  *
  * Pipeline: CommentaryEngine → BookStyleRenderer → (optional) GeminiClient.polish
  * The Gemini polish step is feature-flagged: when GeminiConfig.enabled=false,
  * the rule-based prose is returned directly.
  */
final class LlmApi(
    openingExplorer: OpeningExplorerClient,
    geminiClient: GeminiClient,
    commentaryCache: CommentaryCache
)(using Executor):

  private val logger = lila.log("llm.api")
  private val OpeningRefMinPly = 3
  private val OpeningRefMaxPly = 24

  /** Whether Gemini polish is currently active. */
  def isGeminiEnabled: Boolean = geminiClient.isEnabled

  /** Generate an instant rule-based briefing for a position.
    * Always local, never Gemini-polished (designed for speed).
    */
  def briefCommentPosition(
      fen: String,
      lastMove: Option[String],
      eval: Option[EvalData],
      opening: Option[String],
      phase: String,
      ply: Int
  ): Future[Option[CommentResponse]] = Future {
    val _ = ply
    val pv = eval.flatMap(_.pv).getOrElse(Nil)
    CommentaryEngine.assess(fen, pv, opening, Some(phase)).map { assessment =>
      val bead = Math.abs(fen.hashCode)
      val intro = NarrativeLexicon.intro(bead, assessment.nature.natureType.toString, assessment.nature.tension, Style.Book)

      val bestPlan = assessment.plans.topPlans.headOption.map(_.plan.name).getOrElse("strategic improvement")
      val intent = lastMove.map(m => NarrativeLexicon.intent(bead, m, bestPlan, Style.Book)).getOrElse("")

      val briefing = s"$intro\n\n$intent"

      CommentResponse(
        commentary = briefing,
        concepts = assessment.plans.topPlans.map(_.plan.name),
        variations = Nil
      )
    }
  }

  /** Generate deep bookmaker commentary with optional Gemini polish.
    *
    * Flow:
    * 1. Check server cache → if hit, return immediately
    * 2. Run CommentaryEngine.assessExtended → BookStyleRenderer.render
    * 3. If Gemini enabled: polish(prose) → use polished or fallback to rule-based
    * 4. Store result in server cache
    */
  def bookmakerCommentPosition(
      fen: String,
      lastMove: Option[String],
      eval: Option[EvalData],
      variations: Option[List[VariationLine]],
      probeResults: Option[List[lila.llm.model.ProbeResult]] = None,
      afterFen: Option[String] = None,
      afterEval: Option[EvalData] = None,
      afterVariations: Option[List[VariationLine]] = None,
      opening: Option[String],
      phase: String,
      ply: Int
  ): Future[Option[CommentResponse]] =
    // ── Server cache check ───────────────────────────────────────────────
    commentaryCache.get(fen, lastMove) match
      case Some(cached) =>
        logger.debug(s"Cache hit: ${fen.take(20)}...")
        Future.successful(Some(cached))
      case None =>
        computeBookmakerResponse(
          fen, lastMove, eval, variations, probeResults,
          afterFen, afterEval, afterVariations, opening, phase, ply
        )

  /** Internal: compute commentary (rule-based + optional Gemini polish). */
  private def computeBookmakerResponse(
      fen: String,
      lastMove: Option[String],
      eval: Option[EvalData],
      variations: Option[List[VariationLine]],
      probeResults: Option[List[lila.llm.model.ProbeResult]],
      afterFen: Option[String],
      afterEval: Option[EvalData],
      afterVariations: Option[List[VariationLine]],
      opening: Option[String],
      phase: String,
      ply: Int
  ): Future[Option[CommentResponse]] =
    val effectivePly = NarrativeUtils.resolveAnnotationPly(fen, lastMove, ply)

    val varsFromEval =
      eval.flatMap(_.pv).filter(_.nonEmpty).map { pv =>
        List(
          VariationLine(
            moves = pv,
            scoreCp = eval.map(_.cp).getOrElse(0),
            mate = eval.flatMap(_.mate),
            depth = 0
          )
        )
      }

    val vars = variations.filter(_.nonEmpty).orElse(varsFromEval).getOrElse(Nil)

    val afterVarsFromEval =
      afterEval.flatMap(_.pv).filter(_.nonEmpty).map { pv =>
        List(
          VariationLine(
            moves = pv,
            scoreCp = afterEval.map(_.cp).getOrElse(0),
            mate = afterEval.flatMap(_.mate),
            depth = 0
          )
        )
      }

    val afterVars = afterVariations.filter(_.nonEmpty).orElse(afterVarsFromEval).getOrElse(Nil)
    if vars.isEmpty then Future.successful(None)
    else
      val shouldFetchMasters =
        phase.trim.equalsIgnoreCase("opening") &&
          effectivePly >= OpeningRefMinPly &&
          effectivePly <= OpeningRefMaxPly

      val mastersFut =
        if shouldFetchMasters then openingExplorer.fetchMasters(fen)
        else Future.successful(None)

      mastersFut.flatMap { openingRef =>
        val dataOpt = CommentaryEngine.assessExtended(
          fen = fen,
          variations = vars,
          playedMove = lastMove,
          opening = opening,
          phase = Some(phase),
          ply = effectivePly,
          prevMove = lastMove
        )

        dataOpt match
          case None => Future.successful(None)
          case Some(data) =>
            val afterDataOpt =
              afterFen
                .filter(_.nonEmpty)
                .filter(_ => afterVars.nonEmpty)
                .flatMap { f =>
                  CommentaryEngine.assessExtended(
                    fen = f,
                    variations = afterVars,
                    playedMove = None,
                    opening = opening,
                    phase = Some(phase),
                    ply = effectivePly,
                    prevMove = None
                  )
                }

            val ctx = NarrativeContextBuilder.build(
              data = data,
              ctx = data.toContext,
              probeResults = probeResults.getOrElse(Nil),
              openingRef = openingRef,
              afterAnalysis = afterDataOpt
            )
            val prose = BookStyleRenderer.render(ctx)

            // ── Optional Gemini polish ─────────────────────────────────
            val evalDelta = eval.map(_.cp) // approximate delta
            val nature = Some(data.nature.description)
            val tension = Some(data.nature.tension)

            geminiClient
              .polish(
                prose = prose,
                phase = phase,
                evalDelta = evalDelta,
                concepts = ctx.semantic.map(_.conceptSummary).getOrElse(Nil),
                fen = fen,
                nature = nature,
                tension = tension
              )
              .map { polishedOpt =>
                val finalProse = polishedOpt.getOrElse(prose)
                val response = CommentResponse(
                  commentary = finalProse,
                  concepts = ctx.semantic.map(_.conceptSummary).getOrElse(Nil),
                  variations = data.alternatives,
                  probeRequests = if probeResults.exists(_.nonEmpty) then Nil else ctx.probeRequests
                )
                // Store in server cache
                commentaryCache.put(fen, lastMove, response)
                Some(response)
              }
      }

  /** Generate full game narrative locally (no external LLM). */
  def analyzeFullGameLocal(
      pgn: String,
      evals: List[MoveEval],
      style: String = "book",
      focusOn: List[String] = List("mistakes", "turning_points")
  ): Future[Option[GameNarrativeResponse]] =
    val _ = (style, focusOn)
    val evalMap = evals.map(e => e.ply -> e.getVariations).toMap
    fetchOpeningRefsForPgn(pgn).map { openingRefsByFen =>
      val narrative = CommentaryEngine.generateFullGameNarrative(
        pgn = pgn,
        evals = evalMap,
        openingRefsByFen = openingRefsByFen
      )
      Some(GameNarrativeResponse.fromNarrative(narrative))
    }.recover { case NonFatal(e) =>
      logger.warn(s"Opening reference fetch failed for full game analysis: ${e.getMessage}")
      val narrative = CommentaryEngine.generateFullGameNarrative(
        pgn = pgn,
        evals = evalMap
      )
      Some(GameNarrativeResponse.fromNarrative(narrative))
    }

  private def fetchOpeningRefsForPgn(pgn: String): Future[Map[String, OpeningReference]] =
    val openingFens = PgnAnalysisHelper.extractPlyData(pgn) match
      case Left(err) =>
        logger.warn(s"Failed to parse PGN for opening references: $err")
        Nil
      case Right(plyData) =>
        plyData
          .collect {
            case pd if pd.ply >= OpeningRefMinPly && pd.ply <= OpeningRefMaxPly => pd.fen
          }
          .distinct

    if openingFens.isEmpty then Future.successful(Map.empty)
    else
      Future
        .traverse(openingFens) { fen =>
          openingExplorer
            .fetchMasters(fen)
            .map(refOpt => fen -> refOpt)
            .recover { case NonFatal(e) =>
              logger.warn(s"Opening explorer fetch failed for FEN `${fen.take(32)}...`: ${e.getMessage}")
              fen -> None
            }
        }
        .map(_.collect { case (fen, Some(ref)) => fen -> ref }.toMap)
