package controllers

import play.api.mvc.*
import lila.app.*

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

  def set(name: String) = Open { ctx ?=>
    get("v").fold(fuccess(BadRequest)): v =>
      name match
        // Board zoom is a purely local UI preference (used by layout.pageZoom via the `zoom` cookie),
        // so keep it available to anonymous users without touching the DB.
        case "zoom" =>
          v.toIntOption
            .filter(z => z >= 0 && z <= 100)
            .fold(fuccess(BadRequest)): zoom =>
              fuccess(
                Ok.withCookies(
                  Cookie(
                    name = "zoom",
                    value = zoom.toString,
                    maxAge = (60 * 60 * 24 * 365 * 5).some,
                    path = "/",
                    httpOnly = false
                  )
                )
              )

        case _ =>
          ctx.me.fold(fuccess(Unauthorized)): me =>
            env.pref.api
              .set(me, name, v)
              .inject(Ok)
              .recover { case _: Exception => BadRequest }
  }

  def options = Auth { ctx ?=> me ?=>
    env.pref.api.get(me).map: pref =>
      Ok(lila.pref.ui.DasherJson(pref, none)(using ctx)).as(JSON)
  }
