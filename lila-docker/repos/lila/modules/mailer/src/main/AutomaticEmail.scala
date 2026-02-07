package lila.mailer

import play.api.i18n.Lang
import scalatags.Text.all.*

import lila.core.config.BaseUrl
import lila.core.lilaism.LilaException

final class AutomaticEmail(
    userApi: lila.core.user.UserApi,
    mailer: Mailer,
    baseUrl: BaseUrl,
    lightUser: lila.core.user.LightUserApi
)(using Executor):

  import Mailer.html.*

  val regards = """Regards,

The Chesstory team"""

  def welcomeEmail(user: User, email: EmailAddress)(using Lang): Funit =
    mailer.canSend.so:
      lila.mon.email.send.welcome.increment()
      val profileUrl = s"$baseUrl/@/${user.username}"
      val editUrl = s"$baseUrl/account/profile"
      mailer.sendOrSkip:
        Mailer.Message(
          to = email,
          subject = s"Welcome to Chesstory, ${user.username}!",
          text = Mailer.txt.addServiceNote(s"Your profile: $profileUrl\nEdit your profile: $editUrl"),
          htmlBody = standardEmail(
            s"Your profile: $profileUrl\nEdit your profile: $editUrl"
          ).some
        )

  def magicLinkLogin(email: EmailAddress, url: String): Funit =
    given Lang = Lang("en")
    mailer.canSend.so:
      val body =
        s"""Hello,
           |
           |Use this link to log in:
           |$url
           |
           |If you did not request this, you can ignore this email.
           |
           |$regards
           |""".stripMargin
      mailer.sendOrSkip:
        Mailer.Message(
          to = email,
          subject = "Your login link",
          text = Mailer.txt.addServiceNote(body),
          htmlBody = standardEmail(body).some
        )

  // Social/Game/Patron emails removed for Analysis-Only version

  def delete(user: User): Funit =
    val body =
      s"""Hello,

Following your request, the Chesstory account "${user.username}" will be deleted in 7 days from now.

$regards
"""
    userApi.email(user.id).flatMapz { email =>
      given Lang = Lang("en")
      mailer.sendOrSkip:
        Mailer.Message(
          to = email,
          subject = "Chesstory account deletion",
          text = Mailer.txt.addServiceNote(body),
          htmlBody = standardEmail(body).some
        )
    }

  private def userLang(user: User): Lang = Lang("en")
