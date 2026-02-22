package lila.llm

import scala.concurrent.Future
import scala.util.control.NonFatal
import java.util.concurrent.atomic.AtomicLong
import lila.llm.analysis.{ BookStyleRenderer, CommentaryEngine, NarrativeContextBuilder, NarrativeLexicon, NarrativeUtils, OpeningExplorerClient }
import lila.llm.analysis.NarrativeLexicon.Style
import lila.llm.model.{ FullGameNarrative, OpeningReference }
import lila.llm.model.strategic.VariationLine

/** Pipeline: CommentaryEngine → BookStyleRenderer (rule-based only). */
final class LlmApi(
    openingExplorer: OpeningExplorerClient,
    geminiClient: GeminiClient,
    commentaryCache: CommentaryCache
)(using Executor):

  private val logger = lila.log("llm.api")
  private val OpeningRefMinPly = 3
  private val OpeningRefMaxPly = 24
  private val bookmakerRequests = new AtomicLong(0L)
  private val tokenPresentCount = new AtomicLong(0L)
  private val tokenEmitCount = new AtomicLong(0L)
  private val continuityAppliedCount = new AtomicLong(0L)
  private val stateAwareCacheHitCount = new AtomicLong(0L)
  private val stateAwareCacheMissCount = new AtomicLong(0L)
  private val samePlyIdempotentHitCount = new AtomicLong(0L)
  private val transitionCountByType = scala.collection.concurrent.TrieMap.empty[String, AtomicLong]

  private def incTransition(kind: String): Unit =
    transitionCountByType.getOrElseUpdate(kind, new AtomicLong(0L)).incrementAndGet()

  private def maybeLogBookmakerMetrics(): Unit =
    val total = bookmakerRequests.get()
    if total > 0 && total % 100 == 0 then
      val tokenPresentRate = tokenPresentCount.get().toDouble / total.toDouble
      val tokenEmitRate = tokenEmitCount.get().toDouble / total.toDouble
      val continuityAppliedRate = continuityAppliedCount.get().toDouble / total.toDouble
      val transitionDist = transitionCountByType.toList.map { case (k, v) => k -> v.get() }.toMap
      logger.info(
        s"bookmaker.metrics total=$total " +
          f"token_present_rate=$tokenPresentRate%.3f " +
          f"token_emit_rate=$tokenEmitRate%.3f " +
          f"continuity_applied_rate=$continuityAppliedRate%.3f " +
          s"state_cache_hit=${stateAwareCacheHitCount.get()} " +
          s"state_cache_miss=${stateAwareCacheMissCount.get()} " +
          s"same_ply_idempotent_hits=${samePlyIdempotentHitCount.get()} " +
          s"transition_dist=$transitionDist"
      )


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

  /** Generate deep bookmaker commentary (rule-based). */
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
      ply: Int,
      prevStateToken: Option[lila.llm.analysis.PlanStateTracker] = None
  ): Future[Option[CommentResponse]] =
    val incomingProbes = probeResults.getOrElse(Nil)
    bookmakerRequests.incrementAndGet()
    if prevStateToken.isDefined then tokenPresentCount.incrementAndGet()
    commentaryCache.get(fen, lastMove, incomingProbes, prevStateToken) match
      case Some(cached) =>
        stateAwareCacheHitCount.incrementAndGet()
        logger.debug(s"Cache hit: ${fen.take(20)}...")
        maybeLogBookmakerMetrics()
        Future.successful(Some(cached))
      case None =>
        stateAwareCacheMissCount.incrementAndGet()
        maybeLogBookmakerMetrics()
        computeBookmakerResponse(
          fen, lastMove, eval, variations, probeResults, openingData,
          afterFen, afterEval, afterVariations, opening, phase, ply, prevStateToken
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
      ply: Int,
      prevStateToken: Option[lila.llm.analysis.PlanStateTracker] = None
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
        val isWhiteTurn = fen.contains(" w ")
        val movingColor = if (isWhiteTurn) _root_.chess.Color.White else _root_.chess.Color.Black
        val tracker = prevStateToken.getOrElse(lila.llm.analysis.PlanStateTracker.empty)

        val dataOpt = CommentaryEngine.assessExtended(
          fen = fen,
          variations = vars,
          playedMove = lastMove,
          opening = opening,
          phase = Some(phase),
          ply = effectivePly,
          prevMove = lastMove,
          prevPlanContinuity = tracker.getContinuity(movingColor),
          probeResults = probeResults.getOrElse(Nil)
        )

        dataOpt match
          case None => Future.successful(None)
          case Some(data) =>
            if tracker.getColorState(movingColor).lastPly.contains(effectivePly) then
              samePlyIdempotentHitCount.incrementAndGet()

            val nextTracker = tracker.update(
              movingColor = movingColor,
              ply = effectivePly,
              primaryPlan = data.plans.headOption,
              secondaryPlan = data.plans.lift(1),
              sequence = data.planSequence
            )
            val dataWithContinuity = data.copy(planContinuity = nextTracker.getContinuity(movingColor))
            if dataWithContinuity.planContinuity.exists(_.consecutivePlies >= 2) then
              continuityAppliedCount.incrementAndGet()
            dataWithContinuity.planSequence.foreach(ps => incTransition(ps.transitionType.toString))

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
              data = dataWithContinuity,
              ctx = dataWithContinuity.toContext,
              probeResults = probeResults.getOrElse(Nil),
              openingRef = openingRef,
              afterAnalysis = afterDataOpt
            )
            val prose = BookStyleRenderer.render(ctx)

            val response = CommentResponse(
              commentary = prose,
              concepts = ctx.semantic.map(_.conceptSummary).getOrElse(Nil),
              variations = dataWithContinuity.alternatives,
              probeRequests = if probeResults.exists(_.nonEmpty) then Nil else ctx.probeRequests,
              planStateToken = Some(nextTracker)
            )
            if response.planStateToken.isDefined then tokenEmitCount.incrementAndGet()
            commentaryCache.put(fen, lastMove, response, probeResults.getOrElse(Nil), prevStateToken)
            maybeLogBookmakerMetrics()
            Future.successful(Some(response))
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
