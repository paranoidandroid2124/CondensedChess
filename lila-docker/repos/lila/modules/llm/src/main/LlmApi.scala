package lila.llm

import scala.concurrent.Future
import scala.util.control.NonFatal
import lila.llm.analysis.{ BookStyleRenderer, CommentaryEngine, NarrativeContextBuilder, NarrativeLexicon, NarrativeUtils, OpeningExplorerClient }
import lila.llm.analysis.NarrativeLexicon.Style
import lila.llm.model.{ FullGameNarrative, OpeningReference }
import lila.llm.model.strategic.VariationLine

/** Pipeline: CommentaryEngine → BookStyleRenderer → (optional) GeminiClient.polish */
final class LlmApi(
    openingExplorer: OpeningExplorerClient,
    geminiClient: GeminiClient,
    commentaryCache: CommentaryCache
)(using Executor):

  private val logger = lila.log("llm.api")
  private val OpeningRefMinPly = 3
  private val OpeningRefMaxPly = 24


  def isGeminiEnabled: Boolean = geminiClient.isEnabled


  def fetchOpeningMasters(fen: String): Future[Option[OpeningReference]] =
    openingExplorer.fetchMasters(fen)


  def fetchOpeningMasterPgn(gameId: String): Future[Option[String]] =
    openingExplorer.fetchMasterPgn(gameId)

  /** Generate an instant rule-based briefing for a position. */
  def briefCommentPosition(
      fen: String,
      lastMove: Option[String],
      eval: Option[EvalData],
      ply: Int
  ): Future[Option[CommentResponse]] = Future {
    val _ = ply
    val pv = eval.flatMap(_.pv).getOrElse(Nil)
    CommentaryEngine.assess(fen, pv).map { assessment =>
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

  /** Generate deep bookmaker commentary with optional Gemini polish. */
  def bookmakerCommentPosition(
      fen: String,
      lastMove: Option[String],
      eval: Option[EvalData],
      variations: Option[List[VariationLine]],
      probeResults: Option[List[lila.llm.model.ProbeResult]] = None,
      openingData: Option[OpeningReference] = None,
      afterFen: Option[String] = None,
      afterEval: Option[EvalData] = None,
      afterVariations: Option[List[VariationLine]] = None,
      opening: Option[String],
      phase: String,
      ply: Int
  ): Future[Option[CommentResponse]] =
    commentaryCache.get(fen, lastMove) match
      case Some(cached) =>
        logger.debug(s"Cache hit: ${fen.take(20)}...")
        Future.successful(Some(cached))
      case None =>
        computeBookmakerResponse(
          fen, lastMove, eval, variations, probeResults, openingData,
          afterFen, afterEval, afterVariations, opening, phase, ply
        )


  private def computeBookmakerResponse(
      fen: String,
      lastMove: Option[String],
      eval: Option[EvalData],
      variations: Option[List[VariationLine]],
      probeResults: Option[List[lila.llm.model.ProbeResult]],
      openingData: Option[OpeningReference],
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
        if openingData.isDefined then Future.successful(Some(openingExplorer.enrichWithLocalPgn(fen, openingData.get)))
        else if shouldFetchMasters then openingExplorer.fetchMasters(fen)
        else Future.successful(None)

      mastersFut.flatMap { openingRef =>
        val dataOpt = CommentaryEngine.assessExtended(
          fen = fen,
          variations = vars,
          playedMove = lastMove,
          opening = opening,
          phase = Some(phase),
          ply = effectivePly,
          prevMove = lastMove,
          probeResults = probeResults.getOrElse(Nil)
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

            val evalDelta = eval.map(_.cp)
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
                commentaryCache.put(fen, lastMove, response)
                Some(response)
              }
      }


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
      Some(
        GameNarrativeResponse.fromNarrative(
          narrative = narrative,
          review = Some(buildGameReview(narrative, pgn, evals))
        )
      )
    }.recover { case NonFatal(e) =>
      logger.warn(s"Opening reference fetch failed for full game analysis: ${e.getMessage}")
      val narrative = CommentaryEngine.generateFullGameNarrative(
        pgn = pgn,
        evals = evalMap
      )
      Some(
        GameNarrativeResponse.fromNarrative(
          narrative = narrative,
          review = Some(buildGameReview(narrative, pgn, evals))
        )
      )
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

  private def buildGameReview(
      narrative: FullGameNarrative,
      pgn: String,
      evals: List[MoveEval]
  ): GameNarrativeReview =
    val totalPliesFromPgn = PgnAnalysisHelper.extractPlyData(pgn).toOption.map(_.size).getOrElse(0)
    val evalPlies = evals.map(_.ply).filter(_ > 0).distinct
    val inferredTotalPlies =
      if totalPliesFromPgn > 0 then totalPliesFromPgn
      else evalPlies.maxOption.getOrElse(0)
    val evalCoveredPlies =
      if inferredTotalPlies > 0 then evalPlies.count(_ <= inferredTotalPlies)
      else evalPlies.size
    val evalCoveragePct =
      if inferredTotalPlies <= 0 then 0
      else Math.round((evalCoveredPlies.toDouble * 100.0) / inferredTotalPlies.toDouble).toInt
    val selectedMomentPlies = narrative.keyMomentNarratives.map(_.ply).filter(_ > 0).distinct.sorted

    GameNarrativeReview(
      totalPlies = inferredTotalPlies,
      evalCoveredPlies = evalCoveredPlies,
      evalCoveragePct = evalCoveragePct.max(0).min(100),
      selectedMoments = selectedMomentPlies.size,
      selectedMomentPlies = selectedMomentPlies
    )
