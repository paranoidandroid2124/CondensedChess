package controllers

import play.api.mvc._
import play.api.libs.json._
import lila.app.{ *, given }
import lila.llm.{ LlmApi, FullAnalysisRequest, CommentRequest, CreditApi, CreditConfig }
import lila.analyse.ui.BookmakerRenderer
import lila.llm.model.OpeningReference.given

import lila.common.HTTPRequest
import scala.concurrent.duration.*

final class LlmController(
    api: LlmApi,
    creditApi: CreditApi,
    env: Env
) extends LilaController(env):

  // Rate limit for DDoS protection (separate from credit system)
  private val rateLimiter =
    env.memo.mongoRateLimitApi[String](
      name = "llm.request.user",
      credits = 120,  // generous per-day rate limit (DDoS only, credits handle billing)
      duration = 1.day
    )

  /** Full game narrative analysis (local, no Gemini). */
  def analyzeGameLocal = AuthBody(parse.json) { ctx ?=> me ?=>
    withCreditCheck(me.userId.value, CreditConfig.FullGameNarrative):
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

  /** Per-ply bookmaker commentary (rule-based + optional Gemini polish).
    * Requires authentication for credit tracking.
    */
  def bookmakerPosition = AuthBody(parse.json) { ctx ?=> me ?=>
    withCreditCheck(me.userId.value, CreditConfig.PerPlyAnalysis):
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
              openingData = commentReq.openingData,
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
  }

  /** Instant briefing (free, no credits, no auth required). */
  def bookmakerBriefing = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    ctx.body.body.validate[CommentRequest].fold(
      errors => BadRequest(JsError.toJson(errors)).toFuccess,
      commentReq =>
        api
          .briefCommentPosition(
            fen = commentReq.fen,
            lastMove = commentReq.lastMove,
            eval = commentReq.eval,
            opening = commentReq.context.opening,
            openingData = commentReq.openingData,
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
            case None => NotFound("Briefing unavailable")
          }
    )

  /** Get current user's credit status. */
  def creditStatus = Auth { _ ?=> me ?=>
    creditApi.remaining(me.userId.value).map { status =>
      Ok(Json.toJson(status))
    }
  }

  /** Proxy endpoint for opening explorer masters data. */
  def openingMasters(fen: String) = Open:
    val normalizedFen = fen.trim
    if normalizedFen.isEmpty then BadRequest(Json.obj("error" -> "missing_fen")).toFuccess
    else
      api.fetchOpeningMasters(normalizedFen).map:
        case Some(ref) => Ok(Json.toJson(ref))
        case None      => NotFound(Json.obj("error" -> "not_found"))

  /** Proxy endpoint for opening explorer master PGN. */
  def openingMasterPgn(id: String) = Open:
    val gameId = id.trim
    if gameId.isEmpty then BadRequest(Json.obj("error" -> "missing_id")).toFuccess
    else
      api.fetchOpeningMasterPgn(gameId).map:
        case Some(pgn) => Ok(pgn).as("text/plain; charset=utf-8")
        case None      => NotFound(Json.obj("error" -> "not_found"))

  // ── Helpers ──────────────────────────────────────────────────────────

  /** Combined rate limit + credit check gate. */
  private def withCreditCheck(userId: String, cost: Int)(op: => Fu[Result]): Fu[Result] =
    rateLimiter.either(userId, cost = 1, msg = "llm-request", limitedMsg = "Too many requests")(
      creditApi.deduct(userId, cost).flatMap {
        case Left(err) =>
          Forbidden(Json.obj(
            "error" -> "insufficient_credits",
            "remaining" -> err.remaining,
            "required" -> err.required,
            "resetAt" -> err.resetAt.toString
          )).toFuccess
        case Right(remaining) =>
          op.map(_.withHeaders("X-Credits-Remaining" -> remaining.toString))
      }
    ).map:
      case Right(res) => res
      case Left(limited) => TooManyRequests(Json.toJson(limited)).as(JSON)
