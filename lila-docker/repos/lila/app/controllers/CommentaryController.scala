package controllers

import play.api.mvc._
import play.api.libs.json._
import lila.app.{ *, given }
import lila.commentary.{ CommentRequest, GameAnalysisValidationError, CommentaryApi, MoveReviewResponsePayload }
import lila.analyse.ui.MoveReviewRenderer
import lila.commentary.model.OpeningReference.given

import scala.concurrent.duration.*

final class CommentaryController(
    api: CommentaryApi,
    env: Env
) extends LilaController(env):

  // Tier quota policy:
  // - free user: move analysis 100/day
  // - premium user: no daily hard cap, but rolling 30-day fair use and burst guard
  private val freePerMoveDailyLimiter =
    env.memo.mongoRateLimitApi[String](
      name = "commentary.move.free.user.daily",
      credits = 100,
      duration = 1.day
    )

  private val premiumPerMoveRollingLimiter =
    env.memo.mongoRateLimitApi[String](
      name = "commentary.move.premium.user.30d",
      credits = 15000,
      duration = 30.day
    )

  private val premiumMoveReviewBurstLimiter =
    env.memo.mongoRateLimitApi[String](
      name = "commentary.request.premium.user.burst.moveReview",
      credits = 60,
      duration = 1.minute,
      queueTimeout = 40.seconds
    )

  private val betaPremiumForAllLoggedIn =
    sys.env
      .get("CHESSTORY_BETA_PREMIUM_ALL")
      .map(v => Set("1", "true", "yes", "on").contains(v.trim.toLowerCase))
      .getOrElse(true)

  /** Per-ply moveReview commentary. */
  def moveReviewPosition = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    withPerMoveQuota:
      allowAiPolish =>
        ctx.body.body.validate[CommentRequest].fold(
          errors => BadRequest(JsError.toJson(errors)).toFuccess,
          commentReq =>
            validatedMoveReviewRequest(commentReq).fold(
              err => validationErrorResult(err).toFuccess,
              validReq =>
                api
                  .moveReviewPosition(
                    fen = validReq.fen,
                    lastMove = validReq.lastMove,
                    eval = validReq.eval,
                    variations = validReq.variations,
                    probeResults = validReq.probeResults,
                    openingData = validReq.openingData,
                    afterFen = validReq.afterFen,
                    afterEval = validReq.afterEval,
                    afterVariations = validReq.afterVariations,
                    opening = validReq.context.opening,
                    phase = validReq.context.phase,
                    ply = validReq.context.ply,
                    variant = validReq.context.variant,
                    prevStateToken = validReq.planStateToken,
                    prevEndgameStateToken = validReq.endgameStateToken,
                    allowAiPolish = allowAiPolish,
                    lang = requestLang,
                    planTier = resolvedPlanTier
                  )
                  .map {
                    case Some(result) =>
                      val response = result.response
                      val html = MoveReviewRenderer
                        .render(
                          commentary = response.commentary,
                          variations = response.variations,
                          fenBefore = validReq.fen,
                          refs = response.refs
                        )
                        .toString
                      val payload =
                        MoveReviewResponsePayload.json(
                          response = response,
                          html = html,
                          cacheHit = result.cacheHit
                        )
                      Ok(
                        payload
                      )
                    case None => ServiceUnavailable("Lexicon Commentary unavailable")
                  }
            )
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

  private def userRequesterKey(using ctx: Context): String =
    s"user:${ctx.me.map(_.userId.value).getOrElse("anonymous")}"

  private def isPremiumPlan(using ctx: Context): Boolean =
    ctx.me.exists(_.tier.isPremium)

  private def isLoggedIn(using ctx: Context): Boolean =
    ctx.me.isDefined

  private def hasPremiumExperience(using ctx: Context): Boolean =
    isPremiumPlan || (betaPremiumForAllLoggedIn && isLoggedIn)

  private def resolvedPlanTier(using ctx: Context): String =
    if hasPremiumExperience then lila.commentary.PlanTier.Pro
    else lila.commentary.PlanTier.Basic

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

  private def validatedMoveReviewRequest(
      request: CommentRequest
  ): Either[GameAnalysisValidationError, CommentRequest] =
    CommentRequest.validateMoveReview(request)

  private def validationErrorResult(error: GameAnalysisValidationError): Result =
    BadRequest(Json.obj("error" -> error.code, "msg" -> error.message))

  private def withHardQuota(
      limiter: lila.memo.MongoRateLimit[String],
      key: String,
      msg: String,
      limitedMsg: String
  )(op: => Fu[Result]): Fu[Result] =
    limiter.either(key, cost = 1, msg = msg, limitedMsg = limitedMsg)(op).map:
      case Right(res) => res
      case Left(limited) => JsonLimited(limited)

  private def withPremiumMoveReviewBurstQuota(key: String, msg: String)(op: => Fu[Result]): Fu[Result] =
    withHardQuota(
      limiter = premiumMoveReviewBurstLimiter,
      key = key,
      msg = msg,
      limitedMsg = "Too many commentary requests. Please wait a moment and try again."
    )(op)

  private def withPremiumSoftAiQuota(
      limiter: lila.memo.MongoRateLimit[String],
      key: String,
      msg: String
  )(op: Boolean => Fu[Result]): Fu[Result] =
    limiter
      .either(
        k = key,
        cost = 1,
        msg = msg,
        limitedMsg = "Premium fair-use threshold reached; continuing with rule-based commentary."
      )(fuccess(()))
      .flatMap:
        case Right(_) => op(true)
        case Left(_)  => op(false)

  private def withPerMoveQuota(op: Boolean => Fu[Result])(using ctx: Context): Fu[Result] =
    if hasPremiumExperience then
      withPremiumMoveReviewBurstQuota(userRequesterKey, "commentary.move.premium.burst"):
        withPremiumSoftAiQuota(
          limiter = premiumPerMoveRollingLimiter,
          key = userRequesterKey,
          msg = "commentary.move.premium.30d"
        )(op)
    else if isLoggedIn then
      withHardQuota(
        limiter = freePerMoveDailyLimiter,
        key = userRequesterKey,
        msg = "commentary.move.free.daily",
        limitedMsg = "Daily free move-analysis quota reached (100/day)."
      ):
        op(true)
    else
      Unauthorized(
        Json.obj(
          "error" -> "signup_required",
          "msg" -> "Sign up to use move-by-move analysis."
        )
      ).as(JSON).toFuccess
