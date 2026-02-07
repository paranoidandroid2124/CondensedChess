package controllers
import play.api.libs.json.*
import play.api.mvc.*
import play.api.data.*
import play.api.data.Forms.*
import play.api.Mode

import lila.app.*
import lila.core.id.SessionId
import lila.core.email.EmailAddress

final class Auth(env: Env) extends LilaController(env):

  import env.security.{ api }

  private def mobileUserOk(u: UserModel, sessionId: SessionId): Fu[Result] =
    fuccess:
      Ok:
        env.user.jsonView.full(u) ++ Json.obj(
          "nowPlaying" -> JsArray(),
          "sessionId" -> sessionId.value
        )

  private def getReferrer(using Context): String = 
    env.web.referrerRedirect.fromReq | routes.UserAnalysis.index.url

  private val cookieMaxAgeRemember: Int = 60 * 60 * 24 * 30
  private def sidCookie(value: String, remember: Boolean)(using ctx: Context): Cookie =
    Cookie(
      name = api.sessionIdKey,
      value = value,
      maxAge = remember.option(cookieMaxAgeRemember),
      path = "/",
      secure = ctx.req.secure,
      httpOnly = true,
      sameSite = Cookie.SameSite.Lax.some
    )

  def authenticateUser(
      u: UserModel,
      remember: Boolean
  )(using ctx: Context): Fu[Result] =
    api
      .saveAuthentication(u.id, ctx.mobileApiVersion, lila.security.IsPwned(false))
      .flatMap: sessionId =>
        negotiate(
          Redirect(getReferrer),
          mobileUserOk(u, sessionId)
        ).map: res =>
          res.withCookies(sidCookie(sessionId.value, remember))
      .recover:
        case _ => BadRequest("Auth Error")

  def login = Open:
    fuccess(Redirect(routes.Auth.magicLink))

  def authenticate = OpenBody:
    negotiate(
      Redirect(routes.Auth.magicLink).toFuccess,
      BadRequest(jsonError("Use /auth/magic-link")).toFuccess
    )

  def logout = Open:
    api.reqSessionId(ctx.req).fold(fuccess(Redirect(getReferrer))): sid =>
      api.logout(sid).map: _ =>
        Redirect(getReferrer)
          .withNewSession
          .discardingCookies(DiscardingCookie(api.sessionIdKey))

  def logoutGet = logout

  def signup = Open:
    fuccess(Redirect(routes.Auth.login))

  def signupPost = signup
  def passwordReset = signup
  def passwordResetApply = signup
  private val magicLinkForm = Form(
    single(
      "email" -> nonEmptyText.verifying("invalid email", EmailAddress.isValid)
    )
  )

  def magicLink = Open:
    Ok.page(views.auth.magicLink())

  def magicLinkApply = OpenBody:
    bindForm(magicLinkForm)(
      _ => Ok.page(views.auth.magicLink(Some("Invalid email address"))),
      emailStr =>
        val email = EmailAddress(emailStr)
        limit.magicLink(EmailAddress(email.normalize.value).value, Ok.page(views.auth.magicLink(Some("Too many login emails, try again later")))):
          val token = env.security.loginToken.generate(email, 15.minutes)
          val url = s"${env.baseUrl}${routes.Auth.loginWithToken(token).url}"
          env.mailer.automaticEmail.magicLinkLogin(email, url)

          val mockEmail = env.config.getOptional[Boolean]("mailer.primary.mock").getOrElse(false)
          val exposeMockLink = mockEmail && env.mode != Mode.Prod

          if exposeMockLink then
            Ok.page(views.auth.magicLinkDev(url))
          else Redirect(routes.Auth.magicLinkSent).toFuccess
    )

  def magicLinkSent = Open:
    Ok.page(views.auth.magicLinkSent())
  def checkYourEmail = signup
  def fixEmail = signup

  def loginWithToken(token: String) = Open:
    env.security.loginToken.read(token).fold(Ok.page(views.auth.loginError("Invalid or expired login link. Please request a new one."))): email =>
      env.user.repo.upsertEmailUser(email).flatMap: user =>
        given Context = ctx
        authenticateUser(user, remember = true)

  def loginWithTokenPost(token: String) = loginWithToken(token)
