package lila.security

import play.api.data.*
import play.api.data.Forms.*
import scala.concurrent.duration.*
import play.api.data.validation.Constraints
import play.api.mvc.RequestHeader

import lila.common.Form.*
import lila.common.LameName
import lila.core.security.ClearPassword
import lila.core.user.{ TotpSecret, TotpToken }
import lila.core.email.EmailAddress
import lila.core.userId.UserName
import lila.core.lilaism.Core.{ *, given }
import scalalib.future.extensions.*
import lila.common.{ so => _, Form => _, * }
import lila.pref.Pref

final class SecurityForm(
    userRepo: lila.user.UserRepo,
    authenticator: Authenticator,
    lameNameCheck: LameNameCheck
)(using ec: Executor, mode: play.api.Mode):
  
  def prefOf(pref: Pref) = Form:
    mapping(
      "theme" -> optional(text),
      "theme3d" -> optional(text),
      "pieceSet" -> optional(text),
      "pieceSet3d" -> optional(text),
      "bg" -> optional(text)
    )((_, _, _, _, _) => pref)(_ => None)

  import SecurityForm.*

  private val newPasswordField =
    nonEmptyText(minLength = 4, maxLength = 999)

  object signup extends lila.core.security.SignupForm:
    
    val emailField: Mapping[EmailAddress] = ignored(EmailAddress("ignored@example.com"))

    val username: Mapping[UserName] = lila.common.Form
      .cleanNonEmptyText(minLength = 2, maxLength = 20)
      .into[UserName]
      .verifying("usernameUnacceptable", u => !lameNameCheck.value || !LameName.username(u))
      .verifying(
        "usernameAlreadyUsed",
        u => u.id.noGhost && !userRepo.exists(u.id).await(3.seconds, "signupUsername")
      )

    def website(using RequestHeader) = Form:
      mapping(
        "username" -> username,
        "password" -> newPasswordField,
        "fp" -> optional(nonEmptyText)
      )(SignupData.apply)(_ => None)

    val mobile = Form:
      mapping(
        "username" -> username,
        "password" -> newPasswordField
      )(MobileSignupData.apply)(_ => None)

object SecurityForm:

  trait AnySignupData:
    def username: UserName
    def fp: Option[String]

  case class SignupData(
      username: UserName,
      password: String,
      fp: Option[String]
  ) extends AnySignupData:
    def fingerPrint = FingerPrint.from(fp.filter(_.nonEmpty))
    def clearPassword = ClearPassword(password)

  case class MobileSignupData(
      username: UserName,
      password: String
  ) extends AnySignupData:
    def fp = Option.empty
