package controllers

import akka.stream.scaladsl.*
import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.common.Json.given
import lila.core.chess.MultiPv
import lila.core.net.IpAddress
import lila.core.{ LightUser, id }
import lila.security.{ Mobile, UserAgentParser }

/* Chesstory: Analysis-only API controller
 * Removed: round, perfStat, tournament, swiss dependencies
 */
final class Api(env: Env, gameC: => Game) extends LilaController(env):

  import Api.*
  import env.api.gameApi

  private lazy val apiStatusJson = Json.obj:
    "api" -> Json.obj(
      "current" -> Mobile.Api.currentVersion.value,
      "olds" -> Json.arr()
    )

  val status = Anon:
    val appVersion = get("v")
    val mustUpgrade = appVersion.exists(Mobile.AppVersion.mustUpgrade)
    JsonOk(apiStatusJson.add("mustUpgrade", mustUpgrade))

  private val userShowApiRateLimit =
    env.security.ipTrust.rateLimit(8_000 * 2, 1.day, "user.show.api.ip", _.proxyMultiplier(4))

  def user(name: UserStr) = OpenOrScoped(): ctx ?=>
    val cost =
      if ctx.me.exists(_.isVerified) then 1
      else (if env.socket.isOnline.exec(name.id) then 2 else 4) + ctx.isAnon.so(3)
    userShowApiRateLimit(rateLimited, cost = cost):
      env.user.api.withPerfs(name).map { userOpt =>
        toApiResult(userOpt.map { u =>
          env.user.jsonView.full(u.user, u.perfs.some, withProfile = getBoolOpt("profile") | true)
        })
      }.map(toHttp)

  private[controllers] def userWithFollows(using req: RequestHeader) =
    HTTPRequest.apiVersion(req).exists(_.value < 6) && !getBool("noFollows")

  def usersByIds = AnonOrScopedBody(parse.tolerantText)(): ctx ?=>
    val usernames = ctx.body.body.replace("\n", "").split(',').take(300).flatMap(UserStr.read).toList
    val cost = usernames.size / (if ctx.me.exists(_.isVerified) then 20 else 4)
    val withRanks = getBool("rank")
    limit.apiUsers(req.ipAddress, rateLimited, cost = cost.atLeast(1)):
      lila.mon.api.users.increment(cost.toLong)
      env.user.api
        .listWithPerfs(usernames)
        .map:
          _.map: u =>
            env.user.jsonView.full(
              u.user,
              u.perfs.some,
              withProfile = getBoolOpt("profile") | true,
              rankMap = withRanks.option(env.user.rankingsOf(u.user.id))
            )
        .map(toApiResult)
        .map(toHttp)

  def usersStatus = ApiRequest:
    val ids = get("ids").so(_.split(',').take(100).toList.flatMap(UserStr.read).map(_.id))
    if ids.isEmpty then fuccess(ApiResult.ClientError("No user ids provided"))
    else
      env.user.lightUserApi.asyncMany(ids).dmap(_.flatten).map { users =>
        def toJson(u: LightUser) =
          lila.common.Json.lightUser
            .write(u)
            .add("online", env.socket.isOnline.exec(u.id))
        toApiResult(users.map(toJson))
      }

  private def gameFlagsFromRequest(using RequestHeader) =
    lila.api.GameApi.WithFlags(
      analysis = getBool("with_analysis"),
      moves = getBool("with_moves"),
      fens = getBool("with_fens"),
      opening = getBool("with_opening"),
      moveTimes = getBool("with_movetimes"),
      token = get("token")
    )

  def game(id: GameId) = ApiRequest:
    gameApi.one(id, gameFlagsFromRequest).map(toApiResult)

  def crosstable(name1: UserStr, name2: UserStr) = ApiRequest:
    limit.crosstable(req.ipAddress, fuccess(ApiResult.Limited), cost = 1):
      val (u1, u2) = (name1.id, name2.id)
      import lila.game.JsonView.given
      for
        ct <- env.game.crosstableApi(u1, u2)
        matchup <- (ct.results.nonEmpty && getBool("matchup"))
          .so(env.game.crosstableApi.getMatchup(u1, u2))
        both = lila.game.Crosstable.WithMatchup(ct, matchup)
      yield toApiResult(Json.toJsObject(both).some)

  val cloudEval =
    val rateLimit = env.security.ipTrust.rateLimit(3_000, 1.day, "cloud-eval.api.ip", _.proxyMultiplier(3))
    Anon:
      WithProxy: proxy ?=>
        limit.enumeration.cloudEval(rateLimited):
          val suspUA = UserAgentParser.trust.isSuspicious(req.userAgent)
          val cost = if ctx.isAuth then 1 else if suspUA then 5 else 2
          rateLimit(rateLimited, cost = cost):
            get("fen").fold[Fu[Result]](notFoundJson("Missing FEN")): fen =>
              import chess.variant.Variant
              env.evalCache.api
                .getEvalJson(
                  Variant.orDefault(getAs[Variant.LilaKey]("variant")),
                  chess.format.Fen.Full.clean(fen),
                  getIntAs[MultiPv]("multiPv") | MultiPv(1)
                )
                .map:
                  _.fold[Result](notFoundJson("No cloud evaluation available for that position"))(JsonOk)

  def gamesByUsersStream = AnonOrScopedBody(parse.tolerantText)(): ctx ?=>
    val max = ctx.me.fold(300): u =>
      if u.is(UserId.lichess4545) then 900 else 500
    withIdsFromReqBody[UserId](ctx.body, max, id => UserStr.read(id).map(_.id)): ids =>
      GlobalConcurrencyLimitPerIP.events(ctx.ip)(
        ndJson.addKeepAlive:
          env.game.gamesByUsersStream(userIds = ids, withCurrentGames = getBool("withCurrentGames"))
      )(jsOptToNdJson)

  def activity(username: UserStr) = ApiRequest:
    fuccess(ApiResult.NoData) // Activity module removed

  private def withIdsFromReqBody[Id](
      req: Request[String],
      max: Int,
      transform: String => Option[Id]
  )(f: Set[Id] => Result): Result =
    val ids = req.body.split(',').view.filter(_.nonEmpty).flatMap(s => transform(s.trim)).toSet
    if ids.size > max then JsonBadRequest(jsonError(s"Too many ids: ${ids.size}, expected up to $max"))
    else f(ids)

  def ApiRequest(js: Context ?=> Fu[ApiResult]) = Anon:
    js.map(toHttp)

  def toApiResult(json: Option[JsValue]): ApiResult =
    json.fold[ApiResult](ApiResult.NoData)(ApiResult.Data.apply)
  def toApiResult(json: Seq[JsValue]): ApiResult = ApiResult.Data(JsArray(json))

  val toHttp: ApiResult => Result =
    case ApiResult.Limited => rateLimitedJson
    case ApiResult.ClientError(msg) => BadRequest(jsonError(msg))
    case ApiResult.NoData => notFoundJson()
    case ApiResult.Custom(result) => result
    case ApiResult.Done => jsonOkResult
    case ApiResult.Data(json) => JsonOk(json)

  def jsonDownload(makeSource: => Source[JsValue, ?])(using req: RequestHeader): Result =
    GlobalConcurrencyLimitPerIP.download(req.ipAddress)(makeSource)(jsToNdJson)

  def csvDownload(makeSource: => Source[String, ?])(using req: RequestHeader): Result =
    GlobalConcurrencyLimitPerIP.download(req.ipAddress)(makeSource)(sourceToCsv)

  private def sourceToCsv(source: Source[String, ?]): Result =
    Ok.chunked(source.map(_ + "\n")).as(csvContentType).noProxyBuffer

  private[controllers] object GlobalConcurrencyLimitPerIP:
    val events = lila.web.ConcurrencyLimit[IpAddress](
      name = "API events concurrency per IP",
      key = "api.ip.events",
      ttl = 1.hour,
      maxConcurrency = 4
    )
    val eventsForVerifiedUser = lila.web.ConcurrencyLimit[IpAddress](
      name = "API verified events concurrency per IP",
      key = "api.ip.events.verified",
      ttl = 1.hour,
      maxConcurrency = 12
    )
    val download = lila.web.ConcurrencyLimit[IpAddress](
      name = "API download concurrency per IP",
      key = "api.ip.download",
      ttl = 1.hour,
      maxConcurrency = 2
    )
    val generous = lila.web.ConcurrencyLimit[IpAddress](
      name = "API generous concurrency per IP",
      key = "api.ip.generous",
      ttl = 1.hour,
      maxConcurrency = 20
    )

  private[controllers] val GlobalConcurrencyLimitUser = lila.web.ConcurrencyLimit[UserId](
    name = "API concurrency per user",
    key = "api.user",
    ttl = 1.hour,
    maxConcurrency = 2
  )
  private[controllers] val GlobalConcurrencyLimitUserMobile = lila.web.ConcurrencyLimit[UserId](
    name = "API concurrency per mobile user",
    key = "api.user.mobile",
    ttl = 1.hour,
    maxConcurrency = 3
  )
  private[controllers] def GlobalConcurrencyLimitPerUserOption[T](using
      ctx: Context
  ): Option[SourceIdentity[T]] =
    ctx.me.fold(some[SourceIdentity[T]](identity)): me =>
      val limiter = if ctx.isMobileOauth then GlobalConcurrencyLimitUserMobile else GlobalConcurrencyLimitUser
      limiter.compose[T](me.userId)

  private[controllers] def GlobalConcurrencyLimitPerIpAndUserOption[T, U: UserIdOf](
      about: Option[U]
  )(makeSource: => Source[T, ?])(makeResult: Source[T, ?] => Result)(using ctx: Context): Result =
    val ipLimiter =
      if ctx.me.exists(u => about.exists(u.is(_)))
      then GlobalConcurrencyLimitPerIP.generous
      else GlobalConcurrencyLimitPerIP.download
    ipLimiter
      .compose[T](req.ipAddress)
      .flatMap: limitIp =>
        GlobalConcurrencyLimitPerUserOption[T].map: limitUser =>
          makeResult(limitIp(limitUser(makeSource)))
      .getOrElse:
        lila.web.ConcurrencyLimit.limitedDefault(1)

  private type SourceIdentity[T] = Source[T, ?] => Source[T, ?]

private[controllers] object Api:

  enum ApiResult:
    case Data(json: JsValue)
    case ClientError(msg: String)
    case NoData
    case Done
    case Limited
    case Custom(result: Result)
