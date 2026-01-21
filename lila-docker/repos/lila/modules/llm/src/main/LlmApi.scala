package lila.llm

import scala.concurrent.{ ExecutionContext, Future }
import lila.llm.analysis.{ BookStyleRenderer, CommentaryEngine, NarrativeContextBuilder, NarrativeLexicon, OpeningExplorerClient }
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
      opening: Option[String],
      phase: String,
      ply: Int
  ): Future[Option[CommentResponse]] =
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
    if vars.isEmpty then Future.successful(None)
    else
      val shouldFetchMasters =
        phase.toLowerCase.contains("opening") && ply >= 13

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
          ply = ply,
          prevMove = lastMove
        )

        dataOpt.map { data =>
          val ctx = NarrativeContextBuilder.build(
            data = data,
            ctx = data.toContext,
            probeResults = probeResults.getOrElse(Nil),
            openingRef = openingRef
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
