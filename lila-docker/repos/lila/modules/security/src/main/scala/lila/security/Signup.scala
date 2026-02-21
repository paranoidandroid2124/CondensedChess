package lila.security

import lila.common.Executor
import lila.common.HTTPRequest
import lila.core.lilaism.Core.*
import lila.db.dsl.*
import lila.core.userId.*
import lila.core.security.ClearPassword
import lila.core.user.{ User, UserEnabled }
import lila.user.{ UserRepo, BSONFields as F }
import play.api.mvc.RequestHeader
import scala.annotation.unused

object Signup:
  enum Result:
    case AllSet(user: User)
    case RateLimited
    case ForbiddenNetwork
    case MissingCaptcha
    case Bad(err: play.api.data.Form[?])

final class Signup(
    userRepo: UserRepo,
    authenticator: Authenticator,
    forms: SecurityForm,
    hcaptcha: Hcaptcha,
    pwnedApi: PwnedApi
)(using Executor):

  private def isDuplicateKey(e: Throwable): Boolean =
    Option(e.getMessage).exists(_.contains("E11000"))

  def apply(
      username: UserName,
      email: lila.core.email.EmailAddress,
      password: ClearPassword,
      enabled: UserEnabled
  ): Fu[User] =
    val id = username.id
    val user = User(
      id = id,
      username = username,
      enabled = enabled,
      roles = Nil,
      createdAt = java.time.Instant.now(),
      email = email.some
    )
    val hash = authenticator.passEnc(password)
    val doc = lila.user.BSONHandlers.userBSONWriter.writeTry(user).get ++ $doc(
      F.bpass -> hash.bytes
    )
    userRepo.coll.insert.one(doc) >> Future.successful(user)

  def website(data: SecurityForm.SignupData, @unused blind: Boolean)(using req: RequestHeader): Fu[Signup.Result] =
    val form = forms.signup.website.fill(data)
    val ip = HTTPRequest.ipAddress(req)
    hcaptcha.verify(data.captchaResponse | "", ip.some).flatMap:
      case false =>
        Future.successful(Signup.Result.MissingCaptcha)
      case true =>
        pwnedApi.isPwned(data.clearPassword).flatMap:
          case pwned if pwned.yes =>
            Future.successful(Signup.Result.Bad(form.withError("password", "This password is known in data breaches")))
          case _ =>
            userRepo.byEmail(data.normalizedEmail).flatMap:
              case Some(_) =>
                Future.successful(Signup.Result.Bad(form.withError("email", "Email already in use")))
              case None =>
                apply(
                  username = data.username,
                  email = data.normalizedEmail,
                  password = data.clearPassword,
                  enabled = UserEnabled(false)
                ).map(Signup.Result.AllSet.apply).recover {
                  case e if isDuplicateKey(e) =>
                    Signup.Result.Bad(form.withError("username", "Username already in use"))
                }

  def mobile(data: SecurityForm.MobileSignupData, @unused apiVersion: lila.core.net.ApiVersion): Fu[Signup.Result] =
    val form = forms.signup.mobile.fill(data)
    pwnedApi.isPwned(data.clearPassword).flatMap:
      case pwned if pwned.yes =>
        Future.successful(Signup.Result.Bad(form.withError("password", "This password is known in data breaches")))
      case _ =>
        userRepo.byEmail(data.normalizedEmail).flatMap:
          case Some(_) =>
            Future.successful(Signup.Result.Bad(form.withError("email", "Email already in use")))
          case None =>
            apply(
              username = data.username,
              email = data.normalizedEmail,
              password = data.clearPassword,
              enabled = UserEnabled(false)
            ).map(Signup.Result.AllSet.apply).recover {
              case e if isDuplicateKey(e) =>
                Signup.Result.Bad(form.withError("username", "Username already in use"))
            }
