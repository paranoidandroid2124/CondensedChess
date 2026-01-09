package controllers
import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.id.SessionId
import lila.core.email.UserStrOrEmail

final class Auth(env: Env, accountC: => Account) extends LilaController(env):

  import env.security.{ api }

  private def mobileUserOk(u: UserModel, sessionId: SessionId)(using Context): Fu[Result] = 
    fuccess:
      Ok:
        env.user.jsonView.full(u) ++ Json.obj(
          "nowPlaying" -> JsArray(),
          "sessionId" -> sessionId.value
        )

  private def getReferrer(using Context): String = 
    env.web.referrerRedirect.fromReq | routes.UserAnalysis.index.url

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
           res.withCookies(
             env.security.lilaCookie.withSession(remember = remember) {
               _ + (api.sessionIdKey -> sessionId.value)
             }
           )
      .recover:
        case _ => BadRequest("Auth Error")

  def login = Open:
    fuccess(Ok("Login Placeholder"))

  def authenticate = OpenBody:
    val referrer = get("referrer")
    val isRemember = api.rememberForm.bindFromRequest().value | true
    bindForm(api.loginForm)(
      err => fuccess(Unauthorized("Login Error")),
      (login, pass) =>
        api.authenticate(login, pass).flatMap:
          case Some(u) => authenticateUser(u, isRemember)
          case _ => fuccess(Unauthorized("Invalid credentials"))
    )

  def logout = Open:
    api.reqSessionId(ctx.req).fold(fuccess(Redirect(getReferrer))): sid =>
      api.logout(sid).map: _ =>
        Redirect(getReferrer).withNewSession

  def logoutGet = logout

  def signup = Open:
    fuccess(NotFound)

  def signupPost = signup
  def signupConfirmEmail(token: String) = signup
  def signupConfirmEmailPost(token: String) = signup
  def passwordReset = signup
  def passwordResetApply = signup
  def passwordResetConfirm(token: String) = signup
  def passwordResetConfirmApply(token: String) = signup
  def magicLink = signup
  def magicLinkApply = signup
  def magicLinkSent = signup
  def checkYourEmail = signup
  def fixEmail = signup
  def loginWithToken(token: String) = signup
  def loginWithTokenPost(token: String, referrer: Option[String]) = signup
