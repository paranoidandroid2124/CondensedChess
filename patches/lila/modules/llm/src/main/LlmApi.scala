package lila.llm

import play.api.libs.json.*
import scala.concurrent.{ ExecutionContext, Future }

/** High-level API for LLM commentary features */
final class LlmApi(client: LlmClient)(using ec: ExecutionContext):

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
