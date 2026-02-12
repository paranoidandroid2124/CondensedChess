package lila.security

import play.api.mvc.{ Cookie, RequestHeader }
import lila.common.Bus

object EmailConfirm:

  def send(user: lila.user.User, subject: String, html: scalatags.Text.Frag): Unit = ()

  object cookie:
    def has(req: RequestHeader): Boolean = false
    def get(req: RequestHeader): Option[lila.user.User] = None
    def set(username: String, email: String): Cookie =
      Cookie("lila-email-confirm", s"$username:$email")
