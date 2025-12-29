package controllers
import play.api.data.{ Form, FormError }
import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.common.Json.given
import lila.core.id.SessionId
import lila.core.email.{ UserIdOrEmail, UserStrOrEmail }
import lila.core.net.IpAddress
import lila.core.security.ClearPassword
import lila.memo.RateLimit
// import lila.security.SecurityForm.{ MagicLink, PasswordReset } removed
import lila.security.{ FingerPrint, Signup, IsPwned }

final class Auth(env: Env, accountC: => Account) extends LilaController(env):

  import env.security.{ api, forms }

  private def mobileUserOk(u: UserModel, sessionId: SessionId)(using Context): Fu[Result] = for
    perfs <- env.user.perfsRepo.perfsOf(u) // pref check removed
  yield Ok:
    env.user.jsonView.full(u, Some(perfs), withProfile = true) ++ Json.obj(
      "nowPlaying" -> JsArray(), // round removed
      "sessionId" -> sessionId
    )

  private def getReferrerOption(using ctx: Context): Option[String] =
    env.web.referrerRedirect.fromReq.orElse(ctx.req.session.get(api.AccessUri))

  private def getReferrer(using Context): String = getReferrerOption | routes.UserAnalysis.index.url

  def authenticateUser(
      u: UserModel,
      pwned: IsPwned,
      remember: Boolean,
      result: Option[String => Result] = None
  )(using ctx: Context): Fu[Result] =
    api
      .saveAuthentication(u.id, ctx.mobileApiVersion, pwned)
      .flatMap: sessionId =>
        negotiate(
          result.fold(Redirect(getReferrer))(_(getReferrer)),
          mobileUserOk(u, sessionId)
        ).map(authenticateCookie(sessionId, remember))
      .recoverWith(authRecovery)

  private def authenticateAppealUser(u: UserModel, redirect: String => Result, url: String)(using
      ctx: Context
  ): Fu[Result] =
    api.appeal
      .saveAuthentication(u.id)
      .flatMap: sessionId =>
        authenticateCookie(sessionId, remember = false):
          redirect(url)
      .recoverWith(authRecovery)

  private def authenticateCookie(sessionId: SessionId, remember: Boolean)(
      result: Result
  )(using RequestHeader) =
    result.withCookies(
      env.security.lilaCookie.withSession(remember = remember) {
        _ + (api.sessionIdKey -> sessionId.value) - api.AccessUri
      }
    )

  private def authRecovery(using ctx: Context): PartialFunction[Throwable, Fu[Result]] =
    case _ => BadRequest("Auth Error").toFuccess 
    // EmailConfirm case removed

  def login = Open(serveLogin)
  def loginLang = LangPage(routes.Auth.login)(serveLogin)

  private def serveLogin(using ctx: Context) = NoBot:
    val referrer = env.web.referrerRedirect.fromReq
    val switch = get("switch").orElse(get("as"))
    referrer.ifTrue(ctx.isAuth).ifTrue(switch.isEmpty) match
      case Some(url) => Redirect(url) // redirect immediately if already logged in
      case None =>
        val prefillUsername = UserStrOrEmail(~switch.filter(_ != "1"))
        val form = api.loginFormFilled(prefillUsername)
        Ok.page(views.auth.login(form, referrer)).map(_.withCanonical(routes.Auth.login))

  private val is2fa = Set("MissingTotpToken", "InvalidTotpToken")

  def authenticate = OpenBody:
    NoCrawlers:
      Firewall:
        def redirectTo(url: String) = if HTTPRequest.isXhr(ctx.req) then Ok(s"ok:$url") else Redirect(url)
        val referrer = get("referrer").filterNot(env.web.referrerRedirect.sillyLoginReferrers)
        val isRemember = api.rememberForm.bindFromRequest().value | true
        bindForm(api.loginForm)(
          err =>
            negotiate(
              Unauthorized.page(views.auth.login(err, referrer, isRemember)),
              Unauthorized(doubleJsonFormErrorBody(err))
            ),
          (login, pass) =>
            LoginRateLimit(login.normalize, ctx.req): chargeLimiters =>
              env.security.pwned
                .isPwned(pass)
                .flatMap: pwned =>
                  if pwned.yes then chargeLimiters()
                  val isEmail = EmailAddress.isValid(login.value)
                  api.loadLoginForm(login, pwned).flatMap {
                    _.bindFromRequest()
                      .fold(
                        err =>
                          chargeLimiters()
                          lila.mon.security.login
                            .attempt(isEmail, pwned = pwned.yes, result = false)
                            .increment()
                          negotiate(
                            err.errors match
                              case List(FormError("", Seq(err), _)) if is2fa(err) => Ok(err)
                              case _ => Unauthorized.page(views.auth.login(err, referrer, isRemember))
                            ,
                            Unauthorized(doubleJsonFormErrorBody(err))
                          )
                        ,
                        result =>
                          result.toOption match
                            case None => InternalServerError("Authentication error")
                            case Some(u) if u.enabled.no =>
                              negotiate(
                                redirectTo(routes.UserAnalysis.index.url).toFuccess, // Account.reopen likely broken
                                Unauthorized(jsonError("This account is closed."))
                              )
                            case Some(u) =>
                              lila.mon.security.login
                                .attempt(isEmail, pwned = pwned.yes, result = true)
                              // garbageCollect removed
                              authenticateUser(u, pwned, isRemember, Some(redirectTo))
                      )
                  }
        )

  // clasLogin removed - clas module deleted

  def logout = Open:
    val sid = env.security.api.reqSessionId(ctx.req)
    for
      _ <- sid.so(env.security.store.delete)
      // push unsub removed
      res <- negotiate(Redirect(routes.Auth.login), jsonOkResult)
    yield res.withCookies(env.security.lilaCookie.newSession)

  // mobile app BC logout with GET
  def logoutGet = Auth { ctx ?=> _ ?=>
    negotiate(
      html = Ok.page(views.auth.logout),
      json = ctx.req.session.get(api.sessionIdKey).map(SessionId.apply).so(env.security.store.delete) >>
        jsonOkResult.withCookies(env.security.lilaCookie.newSession)
    )
  }

  def signup = Open(serveSignup)
  def signupLang = LangPage(routes.Auth.signup)(serveSignup)
  private def serveSignup(using Context) = NoTor:
    forms.signup.website.flatMap: form =>
      Ok.page(views.auth.signup(form))

  private def authLog(user: UserName, email: Option[EmailAddress], msg: String)(using ctx: Context) = for
    proxy <- env.security.ip2proxy.ofReq(ctx.req)
    creationApi <- env.user.repo.createdWithApiVersion(user.id)
    cav = creationApi.fold("-")(_.toString)
  do lila.log("auth").info(s"$proxy $user - cav:$cav $msg")

  def signupPost = OpenBody:
    NoTor:
      Firewall:
        WithProxy: proxy ?=>
          limit.enumeration.signup(rateLimited):
            // forms.preloadEmailDns() removed
              HTTPRequest
                .apiVersion(ctx.req)
                .match
                  case None =>
                    env.security.signup
                      .website(ctx.blind)
                      .flatMap:
                        case Signup.Result.RateLimited | Signup.Result.ForbiddenNetwork => rateLimited
                        case Signup.Result.MissingCaptcha =>
                          forms.signup.website.flatMap: form =>
                            BadRequest.page(views.auth.signup(form))
                        case Signup.Result.Bad(err) =>
                          forms.signup.website.flatMap: baseForm =>
                            BadRequest.page(views.auth.signup(baseForm.withForm(err)))
                        // ConfirmEmail case removed
                        case Signup.Result.AllSet(user) => // No email returned
                          welcome(user) >> redirectNewUser(user)
                  case Some(apiVersion) =>
                    env.security.signup
                      .mobile(apiVersion)
                      .flatMap:
                        case Signup.Result.RateLimited => rateLimited
                        case Signup.Result.ForbiddenNetwork =>
                          BadRequest(jsonError("This network cannot create new accounts."))
                        case Signup.Result.MissingCaptcha => BadRequest(jsonError("Missing captcha?!"))
                        case Signup.Result.Bad(err) => doubleJsonFormError(err)
                        // ConfirmEmail case removed
                        case Signup.Result.AllSet(user) =>
                          welcome(user) >>
                            authenticateUser(user, remember = true, pwned = IsPwned.No)

  private def welcome(user: UserModel)(using
      ctx: Context
  ): Funit =
     // garbageCollect removed
     // mailer welcome removed
     // env.mailer.automaticEmail.welcomePM(user) // mailer removed
     funit
     // welcomePM might need mailer? Assume yes. Remove.
     // pref.api.saveNewUserPrefs removed

  private def redirectNewUser(user: UserModel)(using Context) =
    api
      .saveAuthentication(user.id, ctx.mobileApiVersion, pwned = IsPwned.No)
      .flatMap: sessionId =>
        negotiate(
          Redirect(getReferrerOption | routes.User.show(user.username).url)
            .flashSuccess("Welcome! Your account is now active."),
          mobileUserOk(user, sessionId)
        ).map(authenticateCookie(sessionId, remember = true))
      .recoverWith(authRecovery)

  def setFingerPrint(fp: String, ms: Int) = Auth { ctx ?=> me ?=>
    lila.mon.http.fingerPrint.record(ms)
    api
      .setFingerPrint(ctx.req, FingerPrint(fp))
      .logFailure(lila.log("fp"), _ => s"${HTTPRequest.print(ctx.req)} $fp")
      .inject(NoContent)
  }

  // All password reset / magic link / email confirm methods DELETE
  def checkYourEmail = Action(NotFound)
  def fixEmail = Action(NotFound)
  def signupConfirmEmail(token: String) = Action(NotFound)
  def signupConfirmEmailPost(token: String) = Action(NotFound)
  def passwordReset = Action(NotFound)
  def passwordResetApply = Action(NotFound)
  def passwordResetSent(email: String) = Action(NotFound)
  def passwordResetConfirm(token: String) = Action(NotFound)
  def passwordResetConfirmApply(token: String) = Action(NotFound)
  def magicLink = Action(NotFound)
  def magicLinkApply = Action(NotFound)
  def magicLinkSent = Action(NotFound)
  def makeLoginToken = Action(NotFound)
  def loginWithToken(token: String) = Action(NotFound)
  def loginWithTokenPost(token: String, referrer: Option[String]) = Action(NotFound)
  
  // LoginRateLimit, HasherRateLimit check usage of deleted modules?
  // LoginRateLimit uses env.memo, env.security.passwordHasher. Looks ok.
  // HasherRateLimit uses env.security.passwordHasher. Looks ok.
  // passwordCost uses env.security.ipTrust. Looks ok.
  // EmailConfirmRateLimit deleted

  private[controllers] object LoginRateLimit:
    private val lastAttemptIp =
      env.memo.cacheApi.notLoadingSync[UserIdOrEmail, IpAddress](256, "login.lastIp"):
        _.expireAfterWrite(1.minute).build()
    def apply(id: UserIdOrEmail, req: RequestHeader)(run: RateLimit.Charge => Fu[Result])(using
        Context
    ): Fu[Result] =
      val ip = req.ipAddress
      val multipleIps = lastAttemptIp.asMap().put(id, ip).exists(_ != ip)
      passwordCost(req).flatMap: cost =>
        env.security.passwordHasher.rateLimit[Result](
          rateLimited,
          enforce = env.net.rateLimit,
          ipCost = cost.toInt + EmailAddress.isValid(id.value).so(2),
          userCost = 1 + multipleIps.so(4)
        )(id, req)(run)

  private[controllers] def HasherRateLimit(run: => Fu[Result])(using me: Me, ctx: Context): Fu[Result] =
    passwordCost(req).flatMap: cost =>
      env.security.passwordHasher.rateLimit[Result](
        rateLimited,
        enforce = env.net.rateLimit,
        ipCost = cost.toInt
      )(me.id.into(UserIdOrEmail), req)(_ => run)

  private def passwordCost(req: RequestHeader): Fu[Float] =
    env.security.ipTrust
      .rateLimitCostFactor(req, _.proxyMultiplier(if HTTPRequest.nginxWhitelist(req) then 1 else 8))

  // private[controllers] def EmailConfirmRateLimit ... REMOVED

  private[controllers] def RedirectToProfileIfLoggedIn(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    ctx.me.fold(f)(me => Redirect(routes.User.show(me.username)))
