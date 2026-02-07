package controllers

import play.api.mvc._
import play.api.libs.json._
import lila.app.{ *, given }
import lila.llm.{ LlmApi, FullAnalysisRequest, CommentRequest }
import lila.analyse.ui.BookmakerRenderer

import lila.common.HTTPRequest
import scala.concurrent.duration.*

final class LlmController(
    api: LlmApi,
    env: Env
) extends LilaController(env):

  // Chesstory: Use local analysis engine (CommentaryEngine) directly
  private val llmQuota =
    env.memo.mongoRateLimitApi[String](
      name = "llm.request.user",
      credits = 40,
      duration = 1.day
    )

  private def withQuota(cost: Int, msg: => String, key: String)(op: => Fu[Result]): Fu[Result] =
    llmQuota
      .either(key, cost = cost, msg = msg, limitedMsg = "LLM quota exceeded")(op)
      .map:
        case Right(res) => res
        case Left(limited) => TooManyRequests(Json.toJson(limited)).as(JSON)

  def analyzeGameLocal = AuthBody(parse.json) { ctx ?=> me ?=>
    withQuota(cost = 20, msg = "game-analysis-local", key = me.userId.value):
      ctx.body.body.validate[FullAnalysisRequest].fold(
        errors => BadRequest(JsError.toJson(errors)).toFuccess,
        analysisReq =>
          api
            .analyzeFullGameLocal(
              pgn = analysisReq.pgn,
              evals = analysisReq.evals,
              style = analysisReq.options.style,
              focusOn = analysisReq.options.focusOn
            )
            .map:
              case Some(response) => Ok(Json.toJson(response))
              case None           => ServiceUnavailable("Narrative Analysis unavailable")
      )
  }

  def analyzeGame = AuthBody(parse.json) { ctx ?=> me ?=>
    withQuota(cost = 20, msg = "game-analysis", key = me.userId.value):
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

  def analyzePosition = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    val rateLimitKey = ctx.me.map(_.userId.value).getOrElse(HTTPRequest.ipAddressStr(ctx.body))
    withQuota(cost = 1, msg = "position-analysis", key = rateLimitKey):
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

  def bookmakerPosition = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    // GEMINI DISABLED FOR LEXICON AUDIT
    ctx.body.body.validate[CommentRequest].fold(
      errors => BadRequest(JsError.toJson(errors)).toFuccess,
      commentReq =>
        api
          .bookmakerCommentPosition(
            fen = commentReq.fen,
            lastMove = commentReq.lastMove,
            eval = commentReq.eval,
            variations = commentReq.variations,
            probeResults = commentReq.probeResults,
            afterFen = commentReq.afterFen,
            afterEval = commentReq.afterEval,
            afterVariations = commentReq.afterVariations,
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
              Ok(
                Json.obj(
                  "html" -> html,
                  "commentary" -> response.commentary,
                  "variations" -> response.variations,
                  "concepts" -> response.concepts,
                  "probeRequests" -> response.probeRequests
                )
              )
            case None => ServiceUnavailable("Lexicon Commentary unavailable")
          }
    )

  def bookmakerBriefing = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    // Briefing is cheap (no Gemini call), so we can skip or use lighter rate limiting
    ctx.body.body.validate[CommentRequest].fold(
      errors => BadRequest(JsError.toJson(errors)).toFuccess,
      commentReq =>
        api
          .briefCommentPosition(
            fen = commentReq.fen,
            lastMove = commentReq.lastMove,
            eval = commentReq.eval,
            opening = commentReq.context.opening,
            phase = commentReq.context.phase,
            ply = commentReq.context.ply
          ) match
          case Some(response) =>
            val html = BookmakerRenderer
              .render(
                commentary = response.commentary,
                variations = response.variations,
                fenBefore = commentReq.fen
              )
              .toString
            Ok(Json.obj("html" -> html, "concepts" -> response.concepts)).toFuccess
          case None => NotFound("Briefing unavailable").toFuccess
    )
