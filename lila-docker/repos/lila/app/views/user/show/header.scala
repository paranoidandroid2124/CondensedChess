package views.user
package show

import lila.app.UiEnv.{ *, given }
import lila.app.mashup.UserInfo
import lila.web.ui.bits.splitNumber

object header:

  private def userDom(u: User)(using ctx: Context) =
    span(
      cls := userClass(u.id, none, withOnline = true, withPowerTip = false),
      dataHref := userUrl(u.username)
    )(
      lineIcon,
      u.username,
      if ctx.blind
      then s" : ${if isOnline.exec(u.id) then trans.site.online.txt() else trans.site.offline.txt()}"
      else emptyFrag
    )

  def apply(u: User, info: UserInfo, angle: UserInfo.Angle, social: UserInfo.Social)(using ctx: Context) =
    val hideTroll = u.marks.troll && ctx.isnt(u)
    frag(
      div(cls := "box__top user-show__header")(
        h1(userDom(u)),
        u.enabled.no.option(span(cls := "closed")("CLOSED"))
      ),
      div(cls := "user-show__social")(
        div(cls := "number-menu")(
          a(href := "#", cls := "nm-item")(
            splitNumber(trans.site.`nbStudies`.pluralSame(info.nbStudies))
          )
        )
      ),
      // standardFlash was here
      standardFlash,
      div(id := "us_profile")(
        div(cls := "profile-side")(
             div(cls := "user-infos")(
               p(cls := "thin")(trans.site.memberSince(), " ", showDate(u.createdAt)),
               u.seenAt.map: seen =>
                 p(cls := "thin")(trans.site.lastSeenActive(momentFromNow(seen)))
             )
        )
      ),
      div(cls := "angles number-menu number-menu--tabs menu-box-pop")(
        a(
          dataTab := "games",
          cls := List(
            "nm-item to-games" -> true,
            "active" -> (angle.key == "games")
          ),
          href := routes.User.gamesAll(u.username)
        )(
          // Using simplified count (removed count module, but User typically has games count?)
          // Wait, u.count was removed from User. But info.nbs.crosstable is option.
          // Check info.nbs.playing or similar, or just "Games" text if count unavailable.
          "Games"
        )
      )
    )
