package controllers

import play.api.libs.json.*
import play.api.mvc.*
import lila.app.*

final class Account(
    env: Env
) extends LilaController(env):

  def profile = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.UserAnalysis.index)) }
  def username = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.UserAnalysis.index)) }
  def profileApply = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.UserAnalysis.index)) }
  def usernameApply = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.UserAnalysis.index)) }

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

  def passwd = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.UserAnalysis.index)) }
  def passwdApply = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.UserAnalysis.index)) }
  def twoFactor = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.UserAnalysis.index)) }
  def setupTwoFactor = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.UserAnalysis.index)) }
  def disableTwoFactor = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.UserAnalysis.index)) }
  def close = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.UserAnalysis.index)) }
  def closeConfirm = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.UserAnalysis.index)) }
  def delete = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.UserAnalysis.index)) }
  def deleteConfirm = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.UserAnalysis.index)) }
  def deleteDone = Open { _ ?=> Ok("Scheduled for deletion") }
  def kid = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.UserAnalysis.index)) }
  def apiKid = Scoped() { _ ?=> _ ?=> JsonOk(Json.obj("kid" -> false)) }
  def kidPost = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.UserAnalysis.index)) }
  def apiKidPost = Scoped() { _ ?=> _ ?=> JsonOk(Json.obj("ok" -> true)) }
  def security = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.UserAnalysis.index)) }
  def data = Auth { _ ?=> me ?=> fuccess(Redirect(routes.UserAnalysis.index)) }
