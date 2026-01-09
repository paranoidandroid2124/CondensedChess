package controllers

import play.api.libs.json.*
import play.api.mvc.*
import lila.app.{ *, given }
import lila.web.AnnounceApi
import lila.api.Api

final class Account(
    env: Env,
    auth: Auth,
    apiC: => Api
) extends LilaController(env):

  def profile = Auth { _ ?=> _ ?=> fuccess(NotFound) }
  def username = Auth { _ ?=> _ ?=> fuccess(NotFound) }
  def profileApply = Auth { _ ?=> _ ?=> fuccess(NotFound) }
  def usernameApply = Auth { _ ?=> _ ?=> fuccess(NotFound) }

  def info = Auth { ctx ?=> me ?=>
    negotiateJson:
      Ok(
        env.user.jsonView.full(me) ++ Json.obj(
            "prefs" -> Json.obj(),
            "nowPlaying" -> JsArray(),
            "online" -> true
          )
          .add("kid" -> false)
          .add("troll" -> me.marks.troll)
          .add("announce" -> env.announceApi.current.map(_.json))
      ).headerCacheSeconds(15)
  }

  def nowPlaying = Auth { _ ?=> _ ?=>
    negotiateJson(JsonOk(Json.obj("nowPlaying" -> JsArray())))
  }

  val apiMe = Scoped() { ctx ?=> me ?=>
    JsonOk(env.user.jsonView.full(me))
  }

  def apiNowPlaying = Scoped()(JsonOk(Json.obj("nowPlaying" -> JsArray())))

  def dasher = Auth { _ ?=> me ?=>
    negotiateJson:
      Ok:
        lila.common.Json.lightUser.write(me.light) ++ Json.obj(
            "prefs" -> Json.obj()
        )
  }

  def passwd = Auth { _ ?=> _ ?=> fuccess(NotFound) }
  def passwdApply = Auth { _ ?=> _ ?=> fuccess(NotFound) }
  def twoFactor = Auth { _ ?=> _ ?=> fuccess(NotFound) }
  def setupTwoFactor = Auth { _ ?=> _ ?=> fuccess(NotFound) }
  def disableTwoFactor = Auth { _ ?=> _ ?=> fuccess(NotFound) }
  def close = Auth { _ ?=> _ ?=> fuccess(NotFound) }
  def closeConfirm = Auth { _ ?=> _ ?=> fuccess(NotFound) }
  def delete = Auth { _ ?=> _ ?=> fuccess(NotFound) }
  def deleteConfirm = Auth { _ ?=> _ ?=> fuccess(NotFound) }
  def deleteDone = Open { _ ?=> Ok("Scheduled for deletion") }
  def kid = Auth { _ ?=> _ ?=> fuccess(NotFound) }
  def apiKid = Scoped() { _ ?=> _ ?=> JsonOk(Json.obj("kid" -> false)) }
  def kidPost = Auth { _ ?=> _ ?=> fuccess(NotFound) }
  def apiKidPost = Scoped() { _ ?=> _ ?=> JsonOk(Json.obj("ok" -> true)) }
  def security = Auth { _ ?=> _ ?=> fuccess(NotFound) }
  def signout(sessionId: String) = Auth { _ ?=> _ ?=> fuccess(NotFound) }
  def reopen = Open { _ ?=> fuccess(NotFound) }
  def reopenApply = Open { _ ?=> fuccess(NotFound) }
  def reopenSent = Open { _ ?=> fuccess(NotFound) }
  def reopenLogin(token: String) = Open { _ ?=> fuccess(NotFound) }
  def data = Auth { _ ?=> me ?=> fuccess(NotFound) }
