package lila.mailer

import play.api.i18n.Lang
import scalatags.Text.all.*

import lila.core.config.BaseUrl
import lila.core.i18n.I18nKey.emails as trans
import lila.core.i18n.Translator
import lila.core.lilaism.LilaException

final class AutomaticEmail(
    userApi: lila.core.user.UserApi,
    mailer: Mailer,
    baseUrl: BaseUrl,
    lightUser: lila.core.user.LightUserApi
)(using Executor, Translator):

  import Mailer.html.*

  val regards = """Regards,

The Lichess team"""

  def welcomeEmail(user: User, email: EmailAddress)(using Lang): Funit =
    mailer.canSend.so:
      lila.mon.email.send.welcome.increment()
      val profileUrl = s"$baseUrl/@/${user.username}"
      val editUrl = s"$baseUrl/account/profile"
      mailer.sendOrSkip:
        Mailer.Message(
          to = email,
          subject = trans.welcome_subject.txt(user.username),
          text = Mailer.txt.addServiceNote(trans.welcome_text.txt(profileUrl, editUrl)),
          htmlBody = standardEmail(
            trans.welcome_text.txt(profileUrl, editUrl)
          ).some
        )

  // Social/Game/Patron emails removed for Analysis-Only version

  def delete(user: User): Funit =
    val body =
      s"""Hello,

Following your request, the Lichess account "${user.username}" will be deleted in 7 days from now.

$regards
"""
    userApi.email(user.id).flatMapz { email =>
      given Lang = user.lang.flatMap(Lang.get).getOrElse(lila.core.i18n.defaultLang)
      mailer.sendOrSkip:
        Mailer.Message(
          to = email,
          subject = "lichess.org account deletion",
          text = Mailer.txt.addServiceNote(body),
          htmlBody = standardEmail(body).some
        )
    }

  private def userLang(user: User): Lang = user.lang.flatMap(Lang.get).getOrElse(lila.core.i18n.defaultLang)
