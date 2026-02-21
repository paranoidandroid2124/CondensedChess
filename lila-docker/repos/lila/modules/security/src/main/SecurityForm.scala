package lila.security

import play.api.data.*
import play.api.data.Forms.*
import play.api.mvc.RequestHeader
import scala.annotation.unused

import lila.common.LameName
import lila.common.Form.into
import lila.core.security.ClearPassword
import lila.core.email.EmailAddress
import lila.core.userId.UserName
import lila.common.{ so => _, Form => _, * }
import lila.pref.Pref

final class SecurityForm(
    userRepo: lila.user.UserRepo,
    @unused authenticator: Authenticator,
    lameNameCheck: LameNameCheck
)(using @unused ec: Executor, @unused mode: play.api.Mode):
  
  def prefOf(pref: Pref) = Form:
    mapping(
      "theme" -> optional(text),
      "theme3d" -> optional(text),
      "pieceSet" -> optional(text),
      "pieceSet3d" -> optional(text),
      "bg" -> optional(text)
    )(
      (theme, theme3d, pieceSet, pieceSet3d, bg) =>
        List(
          theme.map("theme" -> _),
          theme3d.map("theme3d" -> _),
          pieceSet.map("pieceSet" -> _),
          pieceSet3d.map("pieceSet3d" -> _),
          bg.map("bg" -> _)
        ).flatten.foldLeft(pref) { case (p, (name, value)) =>
          p.set(name, value)
        }
    )(p =>
      Some(
        (
          p.theme.some,
          p.theme3d.some,
          p.pieceSet.some,
          p.pieceSet3d.some,
          p.currentBg.some
        )
      )
    )

  import SecurityForm.*

  private val newPasswordField =
    nonEmptyText(minLength = 4, maxLength = 999)

  object signup extends lila.core.security.SignupForm:
    
    val emailField: Mapping[EmailAddress] =
      nonEmptyText(maxLength = EmailAddress.maxLength)
        .transform(_.trim, identity)
        .verifying("invalidEmail", EmailAddress.isValid)
        .transform[EmailAddress](EmailAddress.apply, _.value)

    val username: Mapping[UserName] = lila.common.Form
      .cleanNonEmptyText(minLength = 2, maxLength = 20)
      .into[UserName]
      .verifying("usernameUnacceptable", u => !lameNameCheck.value || !LameName.username(u))
      .verifying(
        "usernameAlreadyUsed",
        u => u.id.noGhost && !userRepo.exists(u.id).await(3.seconds, "signupUsername")
      )

    def website(using @unused req: RequestHeader) = Form:
      mapping(
        "email" -> emailField,
        "username" -> username,
        "password" -> newPasswordField,
        "h-captcha-response" -> optional(nonEmptyText),
        "fp" -> optional(nonEmptyText)
      )(SignupData.apply)(_ => None)

    val mobile = Form:
      mapping(
        "email" -> emailField,
        "username" -> username,
        "password" -> newPasswordField
      )(MobileSignupData.apply)(_ => None)

object SecurityForm:

  trait AnySignupData:
    def email: EmailAddress
    def username: UserName
    def fp: Option[String]

  case class SignupData(
      email: EmailAddress,
      username: UserName,
      password: String,
      hCaptchaResponse: Option[String],
      fp: Option[String]
  ) extends AnySignupData:
    def normalizedEmail = EmailAddress(email.normalize.value)
    def captchaResponse = hCaptchaResponse.map(_.trim).filter(_.nonEmpty)
    def fingerPrint = FingerPrint.from(fp.filter(_.nonEmpty))
    def clearPassword = ClearPassword(password)

  case class MobileSignupData(
      email: EmailAddress,
      username: UserName,
      password: String
  ) extends AnySignupData:
    def normalizedEmail = EmailAddress(email.normalize.value)
    def fp = Option.empty
    def clearPassword = ClearPassword(password)
