package controllers

import play.api.mvc._
import play.api.libs.json._
import lila.app.{ *, given }
import lila.llm.{ LlmApi, FullAnalysisRequest, CommentRequest }
import lila.analyse.ui.BookmakerRenderer

import scala.concurrent.duration.*

final class LlmController(
    api: LlmApi,
    env: Env
) extends LilaController(env):

  // Chesstory: Use local analysis engine (CommentaryEngine) directly
  private val llmQuota =
    env.memo.mongoRateLimitApi[lila.core.userId.UserId](
      name = "llm.request.user",
      credits = 40,
      duration = 1.day
    )

  private def withQuota(cost: Int, msg: => String)(op: => Fu[Result])(using me: Me): Fu[Result] =
    llmQuota
      .either(me.userId, cost = cost, msg = msg, limitedMsg = "LLM quota exceeded")(op)
      .map:
        case Right(res) => res
        case Left(limited) => TooManyRequests(Json.toJson(limited)).as(JSON)

  def analyzeGameLocal = AuthBody(parse.json) { ctx ?=> me ?=>
    withQuota(cost = 20, msg = "game-analysis-local"):
      ctx.body.body.validate[FullAnalysisRequest].fold(
        errors => BadRequest(JsError.toJson(errors)).toFuccess,
        analysisReq =>
          api
            .analyzeFullGame(
              pgn = analysisReq.pgn,
              evals = analysisReq.evals,
              style = analysisReq.options.style,
              focusOn = analysisReq.options.focusOn
            )
            .map {
              case Some(response) => Ok(Json.toJson(response))
              case None           => ServiceUnavailable("LLM Analysis unavailable")
            }
      )
  }

  def analyzeGame = AuthBody(parse.json) { ctx ?=> me ?=>
    withQuota(cost = 20, msg = "game-analysis"):
      ctx.body.body.validate[FullAnalysisRequest].fold(
        errors => BadRequest(JsError.toJson(errors)).toFuccess,
        analysisReq =>
          api
            .analyzeFullGame(
              pgn = analysisReq.pgn,
              evals = analysisReq.evals,
              style = analysisReq.options.style,
              focusOn = analysisReq.options.focusOn
            )
            .map {
              case Some(response) => Ok(Json.toJson(response))
              case None           => ServiceUnavailable("LLM Analysis unavailable or disabled")
            }
      )
  }

  def analyzePosition = AuthBody(parse.json) { ctx ?=> me ?=>
    withQuota(cost = 1, msg = "position-analysis"):
      ctx.body.body.validate[CommentRequest].fold(
        errors => BadRequest(JsError.toJson(errors)).toFuccess,
        commentReq =>
          api
            .commentPosition(
              fen = commentReq.fen,
              lastMove = commentReq.lastMove,
              eval = commentReq.eval,
              opening = commentReq.context.opening,
              phase = commentReq.context.phase,
              ply = commentReq.context.ply
            )
            .map {
              case Some(response) => Ok(Json.toJson(response))
              case None           => ServiceUnavailable("LLM Commentary unavailable")
            }
      )
  }

  def bookmakerPosition = AuthBody(parse.json) { ctx ?=> me ?=>
    withQuota(cost = 1, msg = "bookmaker-position"):
      ctx.body.body.validate[CommentRequest].fold(
        errors => BadRequest(JsError.toJson(errors)).toFuccess,
        commentReq =>
          api
            .commentPosition(
              fen = commentReq.fen,
              lastMove = commentReq.lastMove,
              eval = commentReq.eval,
              opening = commentReq.context.opening,
              phase = commentReq.context.phase,
              ply = commentReq.context.ply
            )
            .map {
              case Some(response) =>
                val html = BookmakerRenderer
                  .render(
                    commentary = response.commentary,
                    variations = response.variations,
                    fenBefore = commentReq.fen
                  )
                  .toString
                Ok(Json.obj("html" -> html, "concepts" -> response.concepts))
              case None => ServiceUnavailable("LLM Commentary unavailable")
            }
      )
  }
