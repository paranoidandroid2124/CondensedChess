package controllers

import play.api.mvc._
import play.api.libs.json._
import lila.app.{ *, given }
import lila.llm.{ AsyncGameAnalysisDurability, CommentRequest, FullAnalysisRequest, GameAnalysisValidationError, LlmApi }
import lila.analyse.ui.BookmakerRenderer
import lila.llm.model.OpeningReference.given

import scala.concurrent.duration.*

final class LlmController(
    api: LlmApi,
    env: Env
) extends LilaController(env):

  private val GameArcRefineHeader = "X-Chesstory-GameArc-Refine"
  private val GameArcRefineTokenHeader = "X-Chesstory-GameArc-Refine-Token"
  private val AsyncStatusTokenHeader = "X-Chesstory-Async-Status-Token"

  private def normalizeProbeResultsByPly(
      entries: Option[List[lila.llm.ProbeResultsByPlyEntry]]
  ): Map[Int, List[lila.llm.model.ProbeResult]] =
    entries
      .getOrElse(Nil)
      .collect {
        case entry if entry.ply > 0 && entry.results.nonEmpty =>
          entry.ply -> entry.results.take(1)
      }
      .groupMapReduce(_._1)(_._2)(_ ++ _)

  // Tier quota policy:
  // - anonymous: Game Chronicle 1/day per IP
  // - free user: Game Chronicle 1/day + move analysis 100/day
  // - premium user: no daily hard cap, but rolling 30-day fair use and burst guard
  private val anonFullGameDailyLimiter =
    env.memo.mongoRateLimitApi[String](
      name = "llm.full.anon.ip.daily",
      credits = 1,
      duration = 1.day
    )

  private val freeFullGameDailyLimiter =
    env.memo.mongoRateLimitApi[String](
      name = "llm.full.free.user.daily",
      credits = 1,
      duration = 1.day
    )

  private val freePerMoveDailyLimiter =
    env.memo.mongoRateLimitApi[String](
      name = "llm.move.free.user.daily",
      credits = 100,
      duration = 1.day
    )

  private val premiumFullGameRollingLimiter =
    env.memo.mongoRateLimitApi[String](
      name = "llm.full.premium.user.30d",
      credits = 900,
      duration = 30.day
    )

  private val premiumPerMoveRollingLimiter =
    env.memo.mongoRateLimitApi[String](
      name = "llm.move.premium.user.30d",
      credits = 15000,
      duration = 30.day
    )

  private val premiumBurstLimiter =
    env.memo.mongoRateLimitApi[String](
      name = "llm.request.premium.user.burst",
      credits = 60,
      duration = 1.minute
    )

  private val premiumBookmakerBurstLimiter =
    env.memo.mongoRateLimitApi[String](
      name = "llm.request.premium.user.burst.bookmaker",
      credits = 60,
      duration = 1.minute,
      queueTimeout = 40.seconds
    )

  private val betaPremiumForAllLoggedIn =
    sys.env
      .get("CHESSTORY_BETA_PREMIUM_ALL")
      .map(v => Set("1", "true", "yes", "on").contains(v.trim.toLowerCase))
      .getOrElse(true)

  /** Game Chronicle analysis. */
  def analyzeGameLocal = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    ctx.body.body.validate[FullAnalysisRequest].fold(
      errors => BadRequest(JsError.toJson(errors)).toFuccess,
      analysisReq =>
        validatedGameChronicleRequest(analysisReq).fold(
          err => validationErrorResult(err).toFuccess,
          validReq =>
            val suppliedProbeResults =
              validReq.probeResultsByPly.exists(_.exists(_.results.nonEmpty))
            val wantsProbeRefinement =
              suppliedProbeResults && ctx.req.headers.get(GameArcRefineHeader).contains("1")
            val refinementToken =
              ctx.req.headers.get(GameArcRefineTokenHeader).map(_.trim).filter(_.nonEmpty)

            def runGameChronicle(allowLlmPolish: Boolean) =
              api
                .analyzeGameChronicleLocal(
                  pgn = validReq.pgn,
                  evals = validReq.evals,
                  style = validReq.options.style,
                  focusOn = validReq.options.focusOn,
                  allowLlmPolish = allowLlmPolish,
                  asyncTier = false,
                  lang = requestLang,
                  planTier = resolvedPlanTier,
                  llmLevel = resolvedGameAnalysisLlmLevel,
                  variant = validReq.variant,
                  probeResultsByPly = normalizeProbeResultsByPly(validReq.probeResultsByPly)
                )
                .map:
                  case Some(response) =>
                    val uidOpt = ctx.me.map(_.userId.value)
                    uidOpt.foreach(uid => api.stashCcaResults(uid, response)) // fire-and-forget
                    Ok(gameChronicleEnvelope(response, validReq.pgn))
                  case None           => ServiceUnavailable("Game Chronicle unavailable")

            if suppliedProbeResults && !wantsProbeRefinement then
              BadRequest(
                Json.obj(
                  "error" -> "invalid_probe_refinement",
                  "msg" -> "Probe-backed Game Chronicle requests require a valid refinement token."
                )
              ).toFuccess
            else if wantsProbeRefinement then
              refinementToken match
                case Some(token)
                    if api.consumeGameChronicleRefinementToken(token, analysisRequesterKey, validReq.pgn) =>
                  runGameChronicle(allowLlmPolish = true)
                case _ =>
                  Forbidden(
                    Json.obj(
                      "error" -> "invalid_refine_token",
                      "msg" -> "Game Chronicle refinement token is missing, expired, or no longer valid."
                    )
                  ).toFuccess
            else
              withFullGameQuota:
                allowLlmPolish =>
                  runGameChronicle(allowLlmPolish)
        )
    )

  /** Game Chronicle analysis (async queue). */
  def analyzeGameAsync = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    ctx.body.body.validate[FullAnalysisRequest].fold(
      errors => BadRequest(JsError.toJson(errors)).toFuccess,
      analysisReq =>
        validatedGameChronicleRequest(analysisReq).fold(
          err => validationErrorResult(err).toFuccess,
          validReq =>
            if validReq.probeResultsByPly.exists(_.exists(_.results.nonEmpty)) then
              BadRequest(
                Json.obj(
                  "error" -> "invalid_probe_refinement",
                  "msg" -> "Async Game Chronicle submit does not accept probe results. Start the analysis first, then use the issued refinement token."
                )
              ).toFuccess
            else
              withFullGameQuota:
                allowLlmPolish =>
                  val submit = api.submitGameChronicleAsync(
                    req = validReq,
                    allowLlmPolish = allowLlmPolish,
                    lang = requestLang,
                    refineToken = api.issueGameChronicleRefinementToken(analysisRequesterKey, validReq.pgn),
                    planTier = resolvedPlanTier,
                    llmLevel = resolvedGameAnalysisLlmLevel
                  )
                  Created(Json.toJson(submit)).toFuccess
        )
    )

  /** Poll status for async Game Chronicle analysis. */
  def analyzeGameAsyncStatus(jobId: String) = Open:
    val id = jobId.trim
    val statusToken = ctx.req.headers.get(AsyncStatusTokenHeader).map(_.trim).filter(_.nonEmpty)
    if id.isEmpty then BadRequest(Json.obj("error" -> "missing_job_id")).toFuccess
    else if statusToken.isEmpty then
      BadRequest(Json.obj("error" -> "missing_status_token")).toFuccess
    else
      (
        api.getGameChronicleAsyncStatus(id, statusToken.get) match
          case Some(status) =>
            val base = Json.toJson(status).as[JsObject]
            val isCcaEnabled = ctx.me.map(_.userId.value)
              .filter(_ => status.result.isDefined)
              .exists(uid => new lila.llm.DefeatDnaApi().isCcaEnabledForUser(uid))
            // Stash CCA data from the completed async result (fire-and-forget)
            for {
              uid <- ctx.me.map(_.userId.value)
              result <- status.result
            } api.stashCcaResults(uid, result)
            Ok(base ++ Json.obj("ccaEnabled" -> isCcaEnabled))
          case None         =>
            Gone(
              Json.obj(
                "error" -> "job_unavailable",
                "durability" -> AsyncGameAnalysisDurability.EphemeralMemory,
                "msg" -> "Async Game Chronicle jobs are stored in temporary memory only. Keep this tab open while the job runs and resubmit if it becomes unavailable."
              )
            )
      ).toFuccess

  /** Per-ply bookmaker commentary. */
  def bookmakerPosition = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    withPerMoveQuota:
      allowLlmPolish =>
        ctx.body.body.validate[CommentRequest].fold(
          errors => BadRequest(JsError.toJson(errors)).toFuccess,
          commentReq =>
            validatedBookmakerRequest(commentReq).fold(
              err => validationErrorResult(err).toFuccess,
              validReq =>
                api
                  .bookmakerCommentPosition(
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
                    allowLlmPolish = allowLlmPolish,
                    lang = requestLang,
                    planTier = resolvedPlanTier,
                    llmLevel = resolvedBookmakerLlmLevel
                  )
                  .map {
                    case Some(result) =>
                      val response = result.response
                      val html = BookmakerRenderer
                        .render(
                          commentary = response.commentary,
                          variations = response.variations,
                          fenBefore = validReq.fen,
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
                        "authorQuestions" -> response.authorQuestions,
                        "authorEvidence" -> response.authorEvidence,
                        "mainStrategicPlans" -> response.mainStrategicPlans,
                        "planStateToken" -> response.planStateToken,
                        "endgameStateToken" -> response.endgameStateToken,
                        "sourceMode" -> response.sourceMode,
                        "model" -> response.model,
                        "planTier" -> response.planTier,
                        "llmLevel" -> response.llmLevel,
                        "strategyPack" -> response.strategyPack,
                        "signalDigest" -> response.signalDigest,
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

  /** Defeat DNA Aggregation for User */
  def defeatDna = Open:
    (ctx: Context) ?=>
      ctx.me match
        case Some(user) =>
          val uid = user.userId.value
          val dnaApi = new lila.llm.DefeatDnaApi()
          if dnaApi.isCcaEnabledForUser(uid) then
            api.getCcaHistory(uid).map: history =>
              val report = dnaApi.aggregateDna(uid, history)
              Ok(Json.toJson(report)(lila.llm.DefeatDnaReport.writes))
          else
            NotFound(Json.obj("error" -> "cca_not_enabled")).toFuccess
        case None =>
          Unauthorized(Json.obj("error" -> "login_required")).toFuccess


  // ── Helpers ──────────────────────────────────────────────────────────

  private def userRequesterKey(using ctx: Context): String =
    s"user:${ctx.me.map(_.userId.value).getOrElse("anonymous")}"

  private def ipRequesterKey(using ctx: Context): String =
    s"ip:${ctx.ip.value}"

  private def analysisRequesterKey(using ctx: Context): String =
    ctx.me.fold(ipRequesterKey)(user => s"user:${user.userId.value}")

  private def isPremiumPlan(using ctx: Context): Boolean =
    ctx.me.exists(_.tier.isPremium)

  private def isLoggedIn(using ctx: Context): Boolean =
    ctx.me.isDefined

  private def hasPremiumExperience(using ctx: Context): Boolean =
    isPremiumPlan || (betaPremiumForAllLoggedIn && isLoggedIn)

  private def resolvedPlanTier(using ctx: Context): String =
    if hasPremiumExperience then lila.llm.PlanTier.Pro
    else lila.llm.PlanTier.Basic

  private def resolvedGameAnalysisLlmLevel(using ctx: Context): String =
    if resolvedPlanTier == lila.llm.PlanTier.Pro then lila.llm.LlmLevel.Active
    else lila.llm.LlmLevel.Polish

  private def resolvedBookmakerLlmLevel: String =
    lila.llm.LlmLevel.Polish

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

  private def validatedGameChronicleRequest(
      request: FullAnalysisRequest
  ): Either[GameAnalysisValidationError, FullAnalysisRequest] =
    FullAnalysisRequest.validateGameChronicle(request)

  private def validatedBookmakerRequest(
      request: CommentRequest
  ): Either[GameAnalysisValidationError, CommentRequest] =
    CommentRequest.validateBookmaker(request)

  private def validationErrorResult(error: GameAnalysisValidationError): Result =
    BadRequest(Json.obj("error" -> error.code, "msg" -> error.message))

  private def gameChronicleEnvelope(
      response: lila.llm.GameChronicleResponse,
      pgn: String
  )(using ctx: Context): JsObject =
    val uidOpt = ctx.me.map(_.userId.value)
    val isCcaEnabled = uidOpt.exists(uid => new lila.llm.DefeatDnaApi().isCcaEnabledForUser(uid))
    val sanitizedResponse = lila.llm.UserFacingPayloadSanitizer.sanitize(response)
    Json.toJson(sanitizedResponse).as[JsObject] ++ Json.obj(
      "ccaEnabled" -> isCcaEnabled,
      "refineToken" -> api.issueGameChronicleRefinementToken(analysisRequesterKey, pgn)
    )

  private def withHardQuota(
      limiter: lila.memo.MongoRateLimit[String],
      key: String,
      msg: String,
      limitedMsg: String
  )(op: => Fu[Result]): Fu[Result] =
    limiter.either(key, cost = 1, msg = msg, limitedMsg = limitedMsg)(op).map:
      case Right(res) => res
      case Left(limited) => JsonLimited(limited)

  private def withPremiumBurstQuota(key: String, msg: String)(op: => Fu[Result]): Fu[Result] =
    withHardQuota(
      limiter = premiumBurstLimiter,
      key = key,
      msg = msg,
      limitedMsg = "Too many requests. Please slow down."
    )(op)

  private def withPremiumBookmakerBurstQuota(key: String, msg: String)(op: => Fu[Result]): Fu[Result] =
    withHardQuota(
      limiter = premiumBookmakerBurstLimiter,
      key = key,
      msg = msg,
      limitedMsg = "Too many commentary requests. Please wait a moment and try again."
    )(op)

  private def withPremiumSoftLlmQuota(
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

  private def withFullGameQuota(op: Boolean => Fu[Result])(using ctx: Context): Fu[Result] =
    if hasPremiumExperience then
      withPremiumBurstQuota(userRequesterKey, "llm.full.premium.burst"):
        withPremiumSoftLlmQuota(
          limiter = premiumFullGameRollingLimiter,
          key = userRequesterKey,
          msg = "llm.full.premium.30d"
        )(op)
    else if isLoggedIn then
      withHardQuota(
        limiter = freeFullGameDailyLimiter,
        key = userRequesterKey,
        msg = "llm.full.free.daily",
        limitedMsg = "Daily free full-game quota reached (1/day)."
      ):
        op(true)
    else
      withHardQuota(
        limiter = anonFullGameDailyLimiter,
        key = ipRequesterKey,
        msg = "llm.full.anon.daily",
        limitedMsg = "Daily anonymous full-game quota reached (1/day per IP). Sign up for more."
      ):
        op(true)

  private def withPerMoveQuota(op: Boolean => Fu[Result])(using ctx: Context): Fu[Result] =
    if hasPremiumExperience then
      withPremiumBookmakerBurstQuota(userRequesterKey, "llm.move.premium.burst"):
        withPremiumSoftLlmQuota(
          limiter = premiumPerMoveRollingLimiter,
          key = userRequesterKey,
          msg = "llm.move.premium.30d"
        )(op)
    else if isLoggedIn then
      withHardQuota(
        limiter = freePerMoveDailyLimiter,
        key = userRequesterKey,
        msg = "llm.move.free.daily",
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
