package controllers

import play.api.mvc._
import play.api.libs.json._
import lila.app.{ *, given }
import lila.llm.{ LlmApi, FullAnalysisRequest, CommentRequest }
import lila.analyse.ui.BookmakerRenderer
import lila.llm.model.OpeningReference.given

import scala.concurrent.duration.*

final class LlmController(
    api: LlmApi,
    env: Env
) extends LilaController(env):

  // Daily budget (per user/IP) sized to comfortably cover at least one full PGN walkthrough.
  private val dailyBudgetCredits = 360

  // Rate limit for service protection.
  private val rateLimiter =
    env.memo.mongoRateLimitApi[String](
      name = "llm.request.actor",
      credits = dailyBudgetCredits,
      duration = 1.day
    )

  /** Full game narrative analysis (local, rule-based). */
  def analyzeGameLocal = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    withRateLimit(requesterKey, cost = 3):
      ctx.body.body.validate[FullAnalysisRequest].fold(
        errors => BadRequest(JsError.toJson(errors)).toFuccess,
        analysisReq =>
          val allowLlmPolish = isPremiumPlan
          api
            .analyzeFullGameLocal(
              pgn = analysisReq.pgn,
              evals = analysisReq.evals,
              style = analysisReq.options.style,
              focusOn = analysisReq.options.focusOn,
              allowLlmPolish = allowLlmPolish,
              asyncTier = false,
              lang = requestLang
            )
            .map:
              case Some(response) => Ok(Json.toJson(response))
              case None           => ServiceUnavailable("Narrative Analysis unavailable")
      )

  /** Full game narrative analysis (async queue; low-cost lane). */
  def analyzeGameAsync = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    withRateLimit(requesterKey, cost = 3):
      ctx.body.body.validate[FullAnalysisRequest].fold(
        errors => BadRequest(JsError.toJson(errors)).toFuccess,
        analysisReq =>
          val submit = api.submitGameAnalysisAsync(
            req = analysisReq,
            allowLlmPolish = isPremiumPlan,
            lang = requestLang
          )
          Created(Json.toJson(submit)).toFuccess
      )

  /** Poll status for async full-game analysis. */
  def analyzeGameAsyncStatus(jobId: String) = Open:
    val id = jobId.trim
    if id.isEmpty then BadRequest(Json.obj("error" -> "missing_job_id")).toFuccess
    else
      (
        api.getGameAnalysisAsyncStatus(id) match
          case Some(status) => Ok(Json.toJson(status))
          case None         => NotFound(Json.obj("error" -> "not_found"))
      ).toFuccess

  /** Per-ply bookmaker commentary (rule-based). */
  def bookmakerPosition = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    withRateLimit(requesterKey, cost = 1):
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
              ply = commentReq.context.ply,
              prevStateToken = commentReq.planStateToken,
              allowLlmPolish = isPremiumPlan,
              lang = requestLang
            )
            .map {
              case Some(result) =>
                val response = result.response
                val html = BookmakerRenderer
                  .render(
                    commentary = response.commentary,
                    variations = response.variations,
                    fenBefore = commentReq.fen,
                    refs = response.refs
                  )
                  .toString
                val baseJson = Json.obj(
                  "schema" -> "chesstory.bookmaker.v2",
                  "html" -> html,
                  "commentary" -> response.commentary,
                  "variations" -> response.variations,
                  "concepts" -> response.concepts,
                  "probeRequests" -> response.probeRequests,
                  "planStateToken" -> response.planStateToken,
                  "sourceMode" -> response.sourceMode,
                  "model" -> response.model,
                  "cacheHit" -> result.cacheHit
                )
                val withRefs = response.refs.fold(baseJson)(r => baseJson ++ Json.obj("refs" -> r))
                val payload = response.polishMeta.fold(withRefs)(m => withRefs ++ Json.obj("polishMeta" -> m))
                Ok(
                  payload
                )
              case None => ServiceUnavailable("Lexicon Commentary unavailable")
            }
      )
  /** Instant briefing (free, no auth required). */
  def bookmakerBriefing = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    ctx.body.body.validate[CommentRequest].fold(
      errors => BadRequest(JsError.toJson(errors)).toFuccess,
      commentReq =>
        api
          .briefCommentPosition(
            fen = commentReq.fen,
            lastMove = commentReq.lastMove,
            eval = commentReq.eval,
            ply = commentReq.context.ply
          )
          .map {
            case Some(response) =>
              val html = BookmakerRenderer
                .render(
                  commentary = response.commentary,
                  variations = response.variations,
                  fenBefore = commentReq.fen,
                  refs = response.refs
                )
                .toString
              Ok(Json.obj("html" -> html, "concepts" -> response.concepts))
            case None => NotFound("Briefing unavailable")
          }
    )

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

  private def requesterKey(using ctx: Context): String =
    ctx.me
      .map(me => s"user:${me.userId.value}")
      .getOrElse(s"ip:${ctx.ip.value}")

  private def isPremiumPlan(using ctx: Context): Boolean =
    ctx.me.exists(_.tier.isPremium)

  private def requestLang(using RequestHeader): String =
    val raw = req.headers.get("Accept-Language").getOrElse("")
    raw
      .split(",")
      .toList
      .map(_.trim.toLowerCase)
      .find(_.nonEmpty)
      .map(_.takeWhile(ch => ch.isLetter || ch == '-'))
      .filter(_.nonEmpty)
      .getOrElse("en")

  private def withRateLimit(key: String, cost: Int)(op: => Fu[Result]): Fu[Result] =
    rateLimiter.either(key, cost = cost, msg = "llm-request", limitedMsg = "Too many requests")(op).map:
      case Right(res) => res
      case Left(limited) => TooManyRequests(Json.toJson(limited)).as(JSON)
