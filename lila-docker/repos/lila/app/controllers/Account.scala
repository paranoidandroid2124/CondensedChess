package controllers

import play.api.libs.json.*
import play.api.mvc.*
import lila.app.{ *, given }

final class Account(
    env: Env
) extends LilaController(env):

  private val forms = new lila.user.UserForm

  def profile = Auth { ctx ?=> me ?=>
    val form = forms.username(me)
    Ok.page(views.account.profile(me, form))
  }

  def profileApply = AuthBody { ctx ?=> me ?=>
    bindForm(forms.username(me))(
      err => BadRequest.page(views.account.profile(me, err)),
      name =>
        if name.id == me.username.id then
          env.user.api.updateUsername(me.id, name) inject
            Redirect(routes.Account.profile).flashing("success" -> "Username updated")
        else
          fuccess(BadRequest("Username mismatch"))
    )
  }

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

  def close = Auth { ctx ?=> me ?=>
    Ok.page(views.account.close(me))
  }

  def closeConfirm = Auth { ctx ?=> me ?=>
    val logoutF = env.security.api.reqSessionId(ctx.req).fold(funit)(env.security.api.logout)
    (env.user.api.disable(me.id) >> logoutF).inject(
      Redirect(routes.Main.landing).flashing("success" -> "Account closed")
    )
  }

  def delete = Auth { ctx ?=> me ?=>
    Ok.page(views.account.delete(me))
  }

  def deleteConfirm = Auth { ctx ?=> me ?=>
    val logoutF = env.security.api.reqSessionId(ctx.req).fold(funit)(env.security.api.logout)
    (env.user.api.delete(me.id) >> logoutF).inject(
      Redirect(routes.Main.landing).flashing("success" -> "Account and data permanently deleted")
    )
  }

  def username = profile
  def usernameApply = profileApply

  def passwd = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.Account.profile)) }
  def passwdApply = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.Account.profile)) }
  def twoFactor = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.Account.profile)) }
  def setupTwoFactor = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.Account.profile)) }
  def disableTwoFactor = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.Account.profile)) }
  def kid = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.Account.profile)) }
  def apiKid = Scoped() { _ ?=> _ ?=> JsonOk(Json.obj("kid" -> false)) }
  def kidPost = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.Account.profile)) }
  def apiKidPost = Scoped() { _ ?=> _ ?=> JsonOk(Json.obj("ok" -> true)) }
  def security = Auth { _ ?=> _ ?=> fuccess(Redirect(routes.Account.profile)) }
  def data = Auth { _ ?=> me ?=> fuccess(Redirect(routes.Account.profile)) }
