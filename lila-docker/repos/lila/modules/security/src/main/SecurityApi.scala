package lila.security

import lila.common.*
import lila.core.lilaism.Core.*
import lila.core.id.SessionId
import lila.core.email.EmailAddress
import lila.core.userId.UserStr
import lila.core.security.ClearPassword

final class SecurityApi(
    userRepo: lila.user.UserRepo,
    sessionStore: SessionStore,
    authenticator: Authenticator,
    pwnedApi: PwnedApi
)(using Executor):

  val sessionIdKey = "sid"

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
  def loadLoginForm(u: lila.core.email.UserStrOrEmail, pwned: IsPwned) =
    Future.successful:
      val form = loginFormFilled(u)
      if pwned.yes then form.withGlobalError("This password appears in known breaches. Change it after login.")
      else form
  def rememberForm = Form(single("remember" -> boolean))

  case class AuthSuccess(user: lila.core.user.User, pwned: IsPwned)

  def authenticate(
      login: String,
      pass: String
  ): Fu[Option[AuthSuccess]] =
    val trimmed = login.trim
    val password = ClearPassword(pass)
    val withPassword = PasswordAndToken(password, None)

    val authUser: Fu[Option[lila.core.user.User]] =
      EmailAddress.from(trimmed) match
        case Some(address) =>
          authenticator.authenticateByEmail(address.normalize, withPassword)
        case None =>
          UserStr
            .read(trimmed)
            .fold[Fu[Option[lila.core.user.User]]](fuccess(None))(u => authenticator.authenticateById(u.id, withPassword))

    authUser.flatMap:
      case Some(user) if user.enabled.yes =>
        pwnedApi.isPwned(password).map(pwned => AuthSuccess(user, pwned).some)
      case _ =>
        fuccess(None)

  def logout(sid: SessionId): Funit =
    sessionStore.delete(sid)

  def restoreUser(req: play.api.mvc.RequestHeader): Fu[Option[lila.core.user.User]] =
    reqSessionId(req).fold(fuccess(None)): sid =>
      sessionStore
        .authInfo(sid)
        .flatMap:
          case Some(info) => userRepo.byId(info.userId)
          case None       => fuccess(None)
