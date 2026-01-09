package lila.security

import play.api.mvc.{ Cookie, RequestHeader }

object EmailConfirm:

  object cookie:
    def has(req: RequestHeader): Boolean = false
    def get(req: RequestHeader): Option[lila.user.User] = None
    def set(username: String, email: String): Cookie =
      Cookie("lila-email-confirm", s"$username:$email")
