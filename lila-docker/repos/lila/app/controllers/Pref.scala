package controllers

import play.api.mvc.*
import lila.app.{ *, given }
import lila.common.HTTPRequest

final class Pref(
    env: Env
) extends LilaController(env):

  private val redirects = Map(
    "game-display" -> "display",
    "game-behavior" -> "behavior",
    "privacy" -> "privacy"
  )

  def form(categSlug: String) = Open:
    redirects.get(categSlug) match
      case Some(redir) => Redirect(routes.Pref.form(redir)).toFuccess
      case None =>
        ctx.me.fold(fuccess(Redirect(routes.Main.contact))): me =>
          env.pref.api.get(me).flatMap: pref =>
            Ok.page(views.account.pref(me, env.security.forms.prefOf(pref), categSlug))

  def formApply = AuthBody { ctx ?=> me ?=>
    implicit val _ = ctx
    bindForm(env.security.forms.prefOf(ctx.pref))(
      err =>
        negotiate(
          BadRequest.page(views.account.pref(me, err, "gen")),
          BadRequest(errorsAsJson(err))
        ),
      pref => env.pref.api.setPref(me, pref).inject(NoContent)
    )
  }

  def set(name: String) = Auth { ctx ?=> me ?=>
    get("v").fold(fuccess(BadRequest)): v =>
      env.pref.api
        .set(me, name, v)
        .inject(Ok)
        .recover { case _: Exception => BadRequest }
  }
