package controllers

import java.net.URLEncoder
import play.api.mvc.*
import lila.app.*
import lila.common.CookieConsent

object Pref:
  private[controllers] val canonicalCateg = "display"

  private[controllers] def canonicalCategory(categSlug: String): String =
    Option(categSlug)
      .map(_.trim.toLowerCase)
      .filter(_ == canonicalCateg)
      .getOrElse(canonicalCateg)

final class Pref(
    env: Env
) extends LilaController(env):

  private val bgCookieName = "bg"
  private val bgCookieMaxAge = 60 * 60 * 24 * 365 * 5

  private def bgCookie(value: String)(using ctx: Context): Cookie =
    Cookie(
      name = bgCookieName,
      value = value,
      maxAge = bgCookieMaxAge.some,
      path = "/",
      secure = ctx.req.secure,
      httpOnly = false,
      sameSite = Cookie.SameSite.Lax.some
    )

  def form(categSlug: String) = Open:
    val canonicalCateg = Pref.canonicalCategory(categSlug)
    if categSlug != canonicalCateg then Redirect(routes.Pref.form(canonicalCateg)).toFuccess
    else
      ctx.me.fold(
        {
          val referrer = routes.Pref.form(canonicalCateg).url
          val encoded = URLEncoder.encode(referrer, "UTF-8")
          fuccess(Redirect(s"${routes.Auth.login.url}?referrer=$encoded"))
        }
      ) { me =>
        env.pref.api.get(me).flatMap: pref =>
          Ok.page(views.account.pref(me, env.security.forms.prefOf(pref)))
      }

  def formApply = AuthBody { ctx ?=> me ?=>
    implicit val _ = ctx
    bindForm(env.security.forms.prefOf(ctx.pref))(
      err =>
        negotiate(
          BadRequest.page(views.account.pref(me, err)),
          BadRequest(errorsAsJson(err))
        ),
      pref =>
        val bgValue = lila.pref.Pref.Bg.asString.getOrElse(pref.bg, "dark")
        val consent = CookieConsent.fromRequest(ctx.req)
        env.pref.api.setPref(me, pref) >>
          negotiate(
            fuccess(
              Redirect(routes.Pref.form(Pref.canonicalCateg)).flashing("success" -> "Preferences updated")
            ),
            fuccess(NoContent)
          ).map:
            if consent.preferencesAllowed then _.withCookies(bgCookie(bgValue))
            else _.discardingCookies(DiscardingCookie(bgCookieName, path = "/"))
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
              val consent = CookieConsent.fromRequest(ctx.req)
              fuccess:
                if consent.preferencesAllowed then
                  Ok.withCookies(
                    Cookie(
                      name = "zoom",
                      value = zoom.toString,
                      maxAge = (60 * 60 * 24 * 365 * 5).some,
                      path = "/",
                      httpOnly = false
                    )
                  )
                else Ok.discardingCookies(DiscardingCookie("zoom", path = "/"))

        case "bg" =>
          lila.pref.Pref.Bg.fromString
            .get(v)
            .fold(fuccess(BadRequest)): _ =>
              val consent = CookieConsent.fromRequest(ctx.req)
              val response =
                if consent.preferencesAllowed then Ok.withCookies(bgCookie(v))
                else Ok.discardingCookies(DiscardingCookie(bgCookieName, path = "/"))
              ctx.me.fold(fuccess(response)) { me =>
                env.pref.api
                  .set(me, name, v)
                  .inject(response)
                  .recover { case _: Exception => BadRequest }
              }

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
