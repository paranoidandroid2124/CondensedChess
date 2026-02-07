package lila.llm

import scala.concurrent.{ ExecutionContext, Future }
import lila.llm.analysis.{ BookStyleRenderer, CommentaryEngine, NarrativeContextBuilder, NarrativeLexicon, NarrativeUtils, OpeningExplorerClient }
import lila.llm.analysis.NarrativeLexicon.Style
import lila.llm.model.strategic.VariationLine

/** High-level API for LLM commentary features */
final class LlmApi(client: LlmClient, openingExplorer: OpeningExplorerClient)(using ec: ExecutionContext):

  /** Generate commentary for a single position (real-time analysis) */
  def commentPosition(
      fen: String,
      lastMove: Option[String],
      eval: Option[EvalData],
      opening: Option[String],
      phase: String,
      ply: Int
  ): Future[Option[CommentResponse]] =
    client.commentPosition(CommentRequest(
      fen = fen,
      lastMove = lastMove,
      eval = eval,
      context = PositionContext(opening, phase, ply)
    ))

  /** Generate an instant rule-based briefing for a position */
  def briefCommentPosition(
      fen: String,
      lastMove: Option[String],
      eval: Option[EvalData],
      opening: Option[String],
      phase: String,
      ply: Int
  ): Option[CommentResponse] =
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

  /** Generate a deep, rule-based bookmaker commentary (no Gemini). */
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
        phase.toLowerCase.contains("opening") && effectivePly >= 13

      val mastersFut =
        if shouldFetchMasters then openingExplorer.fetchMasters(fen)
        else Future.successful(None)

      mastersFut.map { openingRef =>
        val dataOpt = CommentaryEngine.assessExtended(
          fen = fen,
          variations = vars,
          playedMove = lastMove,
          opening = opening,
          phase = Some(phase),
          ply = effectivePly,
          prevMove = lastMove
        )

        dataOpt.map { data =>
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

          CommentResponse(
            commentary = prose,
            concepts = ctx.semantic.map(_.conceptSummary).getOrElse(Nil),
            variations = data.alternatives,
            probeRequests = if (probeResults.exists(_.nonEmpty)) Nil else ctx.probeRequests
          )
        }
      }

  /** Generate full game analysis with annotations */
  def analyzeFullGame(
      pgn: String,
      evals: List[MoveEval],
      style: String = "book",
      focusOn: List[String] = List("mistakes", "turning_points")
  ): Future[Option[FullAnalysisResponse]] =
    client.analyzeGame(FullAnalysisRequest(
      pgn = pgn,
      evals = evals,
      options = AnalysisOptions(style, focusOn)
    ))

  /** Generate full game narrative locally (no external LLM). */
  def analyzeFullGameLocal(
      pgn: String,
      evals: List[MoveEval],
      style: String = "book",
      focusOn: List[String] = List("mistakes", "turning_points")
  ): Future[Option[GameNarrativeResponse]] =
    Future {
      val _ = (style, focusOn)
      // `style` and `focusOn` are currently unused by the local engine, but kept for API symmetry.
      val evalMap = evals.map(e => e.ply -> e.getVariations).toMap
      val narrative = CommentaryEngine.generateFullGameNarrative(
        pgn = pgn,
        evals = evalMap
      )
      Some(GameNarrativeResponse.fromNarrative(narrative))
    }

  /** Quick comment for a critical moment */
  def criticalMomentComment(
      fen: String,
      move: String,
      eval: EvalData,
      prevEval: Option[EvalData],
      phase: String
  ): Future[Option[String]] =
    val evalDiff = prevEval.map(p => eval.cp - p.cp).getOrElse(0)
    val momentType = 
      if evalDiff.abs > 200 then "blunder"
      else if evalDiff.abs > 100 then "mistake"
      else if evalDiff.abs > 50 then "inaccuracy"
      else "normal"

    if momentType == "normal" then Future.successful(None)
    else
      commentPosition(
        fen = fen,
        lastMove = Some(move),
        eval = Some(eval),
        opening = None,
        phase = phase,
        ply = 0
      ).map(_.map(_.commentary))
