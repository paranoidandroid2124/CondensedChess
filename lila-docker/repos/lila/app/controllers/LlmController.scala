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

  private val GameArcRefineHeader = "X-Chesstory-GameArc-Refine"

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
        val suppliedProbeResults =
          analysisReq.probeResultsByPly.exists(_.exists(_.results.nonEmpty))
        val isProbeRefinement =
          suppliedProbeResults && ctx.req.headers.get(GameArcRefineHeader).contains("1")

        def runGameChronicle(allowLlmPolish: Boolean) =
          api
            .analyzeGameChronicleLocal(
              pgn = analysisReq.pgn,
              evals = analysisReq.evals,
              style = analysisReq.options.style,
              focusOn = analysisReq.options.focusOn,
              allowLlmPolish = allowLlmPolish,
              asyncTier = false,
              lang = requestLang,
              planTier = resolvedPlanTier,
              llmLevel = resolvedGameAnalysisLlmLevel,
              probeResultsByPly = normalizeProbeResultsByPly(analysisReq.probeResultsByPly)
            )
            .map:
              case Some(response) =>
                val uidOpt = ctx.me.map(_.userId.value)
                val isCcaEnabled = uidOpt.exists(uid => new lila.llm.DefeatDnaApi().isCcaEnabledForUser(uid))
                uidOpt.foreach(uid => api.stashCcaResults(uid, response)) // fire-and-forget
                val wrapper = Json.toJson(response).as[JsObject] ++ Json.obj("ccaEnabled" -> isCcaEnabled)
                Ok(wrapper)
              case None           => ServiceUnavailable("Game Chronicle unavailable")

        if isProbeRefinement then runGameChronicle(allowLlmPolish = true)
        else
          withFullGameQuota:
            allowLlmPolish =>
              runGameChronicle(allowLlmPolish)
    )

  /** Game Chronicle analysis (async queue). */
  def analyzeGameAsync = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    ctx.body.body.validate[FullAnalysisRequest].fold(
      errors => BadRequest(JsError.toJson(errors)).toFuccess,
      analysisReq =>
        withFullGameQuota:
          allowLlmPolish =>
            val submit = api.submitGameChronicleAsync(
              req = analysisReq,
              allowLlmPolish = allowLlmPolish,
              lang = requestLang,
              planTier = resolvedPlanTier,
              llmLevel = resolvedGameAnalysisLlmLevel
            )
            Created(Json.toJson(submit)).toFuccess
    )

  /** Poll status for async Game Chronicle analysis. */
  def analyzeGameAsyncStatus(jobId: String) = Open:
    val id = jobId.trim
    if id.isEmpty then BadRequest(Json.obj("error" -> "missing_job_id")).toFuccess
    else
      (
        api.getGameChronicleAsyncStatus(id) match
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
          case None         => NotFound(Json.obj("error" -> "not_found"))
      ).toFuccess

  /** Per-ply bookmaker commentary. */
  def bookmakerPosition = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    withPerMoveQuota:
      allowLlmPolish =>
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
                prevEndgameStateToken = commentReq.endgameStateToken,
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
                    "authorQuestions" -> response.authorQuestions,
                    "authorEvidence" -> response.authorEvidence,
                    "mainStrategicPlans" -> response.mainStrategicPlans,
                    "latentPlans" -> response.latentPlans,
                    "whyAbsentFromTopMultiPV" -> response.whyAbsentFromTopMultiPV,
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
