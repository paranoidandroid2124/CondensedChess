package lila.security

import lila.common.*
import lila.core.lilaism.Core.*
import lila.core.id.SessionId
import lila.oauth.EndpointScopes
import lila.oauth.OAuthServer.*

final class SecurityApi(
    userRepo: lila.user.UserRepo,
    sessionStore: SessionStore
)(using Executor):

  val sessionIdKey = "sid"
  val AccessUri = "access"

  def reqSessionId(req: play.api.mvc.RequestHeader): Option[SessionId] =
    req.cookies.get(sessionIdKey).map(_.value).map(SessionId.apply)

  def saveAuthentication(
      userId: lila.core.userId.UserId,
      apiVersion: Option[lila.core.net.ApiVersion],
      pwned: IsPwned
  )(using req: play.api.mvc.RequestHeader): Fu[SessionId] =
    val sid = SessionId(java.util.UUID.randomUUID.toString)
    sessionStore.save(
      sid,
      userId,
      req,
      apiVersion,
      up = true,
      fp = Option.empty,
      proxy = lila.core.security.IsProxy.empty,
      pwned = pwned.yes
    ) >> Future.successful(sid)

  def setFingerPrint(req: play.api.mvc.RequestHeader, fp: FingerPrint): Funit =
    reqSessionId(req).fold(Future.unit) { sid =>
      sessionStore.setFingerPrint(sid, fp).void
    }

  object appeal:
    def saveAuthentication(userId: lila.core.userId.UserId)(using req: play.api.mvc.RequestHeader): Fu[SessionId] =
      val sid = SessionId(java.util.UUID.randomUUID.toString)
      sessionStore.save(
        sid,
        userId,
        req,
        apiVersion = Option.empty,
        up = true,
        fp = Option.empty,
        proxy = lila.core.security.IsProxy.empty,
        pwned = false
      ) >> Future.successful(sid)

  import play.api.data.*
  import play.api.data.Forms.*

  def loginForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )((u, p) => (u, p))(_ => None)
  )

  def loginFormFilled(u: lila.core.email.UserStrOrEmail) = loginForm.fill(u.value -> "")
  def loadLoginForm(u: lila.core.email.UserStrOrEmail, pwned: IsPwned) = Future.successful(loginFormFilled(u))
  def rememberForm = Form(single("remember" -> boolean))

  def authenticate(login: String, pass: String): Fu[Option[lila.core.user.User]] =
    userRepo.byId(lila.core.userId.UserId(login.toLowerCase)).flatMap:
      case Some(u) => fuccess(Some(u)) // Auto-authenticate for now, or check pass if needed
      case _ => fuccess(None)

  def logout(sid: SessionId): Funit =
    sessionStore.delete(sid)

  def restoreUser(req: play.api.mvc.RequestHeader): Fu[Option[lila.core.user.User]] =
    // Simplified analysis-only implementation
    Future.successful(None)
    
  def oauthScoped(req: play.api.mvc.RequestHeader, accepted: EndpointScopes): lila.oauth.OAuthServer.AuthFu =
    lila.oauth.OAuthServer.NoSuchToken.raise
