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

final class SecurityForm(
    userRepo: lila.user.UserRepo,
    authenticator: Authenticator,
    lameNameCheck: LameNameCheck
)(using ec: Executor, mode: play.api.Mode):

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
        u => u.id.noGhost && !userRepo.exists(u).await(3.seconds, "signupUsername")
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
    def fp = none
