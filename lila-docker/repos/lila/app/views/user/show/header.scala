package views.user
package show

import lila.app.UiEnv.{ *, given }
import lila.app.mashup.UserInfo
import lila.user.PlayTime.*
import lila.user.Profile.*
import lila.web.ui.bits.splitNumber

object header:

  private val actionMenu = lila.user.ui.UserActionMenu(helpers)

  private def possibleSeoBot(u: User) =
    !u.isVerified && !u.hasTitle && u.count.game < 5 && (
      u.profile.exists(_.links.isDefined) ||
        u.profile.flatMap(_.nonEmptyBio).exists(_.contains("https://"))
    )

  private def userActionsMenu(u: User, social: UserInfo.Social)(using ctx: Context) =
    actionMenu(
      u,
      Nil,
      false
    )

  private def userDom(u: User)(using ctx: Context) =
    span(
      cls := userClass(u.id, none, withOnline = true, withPowerTip = false),
      dataHref := userUrl(u.username)
    )(
      lineIcon,
      titleTag(u.title),
      u.username,
      if ctx.blind
      then s" : ${if isOnline.exec(u.id) then trans.site.online.txt() else trans.site.offline.txt()}"
      else emptyFrag
    )

  def apply(u: User, info: UserInfo, angle: UserInfo.Angle, social: UserInfo.Social)(using ctx: Context) =
    val showLinks = !possibleSeoBot(u) || isGranted(_.Shadowban)
    frag(
      div(cls := "box__top user-show__header")(
        h1(userDom(u)),
        div(cls := "trophies")(
          otherTrophies(info)
        ),
        u.enabled.no.option(span(cls := "closed")("CLOSED"))
      ),
      div(cls := "user-show__social")(
        div(cls := "number-menu")(
          a(href := "#", cls := "nm-item")(
            splitNumber(trans.site.`nbStudies`.pluralSame(info.nbStudies))
          ),
          (ctx.isAuth && ctx.isnt(u))
            .option(a(cls := "nm-item note-zone-toggle")(splitNumber(s"${social.notes.size} Notes")))
        ),
        div(
          cls := "user-actions dropdown-overflow",
          attr("data-menu") := userActionsMenu(u, social).serialize
        )
      ),
      ctx.isnt(u).option(noteUi.zone(u, social.notes)),
      standardFlash,
      angle match
        case UserInfo.Angle.Games(Some(searchForm)) => frag("Search not available")
        case _ =>
          val profile = u.profileOrDefault
          val hideTroll = u.marks.troll && ctx.isnt(u)
          div(id := "us_profile")(
            (ctx.is(u) && u.count.game < 10).option(ui.newPlayer(u)),
            div(cls := "profile-side")(
              div(cls := "user-infos")(
                (u.lame && ctx.isnt(u)).option:
                  div(cls := "warning tos_warning")(
                    span(dataIcon := Icon.CautionCircle, cls := "is4"),
                    trans.site.thisAccountViolatedTos()
                  )
                ,
                (ctx.kid.no && u.kid.no && !hideTroll)
                  .so(profile.nonEmptyRealName)
                  .map(strong(cls := "name")(_)),
                (showLinks && ctx.kid.no && u.kid.no && !hideTroll)
                  .so(profile.nonEmptyBio)
                  .map: bio =>
                    p(cls := List("bio" -> true, "muted" -> hideTroll))(richText(bio, nl2br = true)),
                div(cls := "stats")(
                  profile.officialRating.map: r =>
                    div(r.name.toUpperCase, " rating: ", strong(r.rating)),
                  profile.nonEmptyLocation.ifTrue(ctx.kid.no && !hideTroll).map { l =>
                    span(cls := "location")(l)
                  },
                  profile.flagInfo.map: c =>
                    span(cls := "flag")(
                      img(src := assetUrl(s"flags/${c.code}.png")),
                      " ",
                      c.name
                    ),
                  p(cls := "thin")(trans.site.memberSince(), " ", showDate(u.createdAt)),
                  u.seenAt.map: seen =>
                    p(cls := "thin")(trans.site.lastSeenActive(momentFromNow(seen))),
                  ctx
                    .is(u)
                    .option(
                      a(href := "#", title := trans.site.editProfile.txt())(
                        trans.site.profileCompletion(s"${profile.completionPercent}%")
                      )
                    ),
                  u.playTime.map: playTime =>
                    frag(
                      p(
                        trans.site.tpTimeSpentPlaying(
                          lila.core.i18n.translateDuration(playTime.totalDuration)
                        )
                      )
                    ),
                  (!hideTroll && u.kid.no).option(
                    div(cls := "social_links col2")(
                      showLinks
                        .option(profile.actualLinks)
                        .filter(_.nonEmpty)
                        .map: links =>
                          frag(
                            strong(trans.site.socialMediaLinks()),
                            links.map: link =>
                              a(href := link.url, targetBlank, noFollow, relMe)(link.site.name)
                          )
                    )
                  )
                )
              )
            )
          )
      ,
      div(cls := "angles number-menu number-menu--tabs menu-box-pop")(
        a(
          dataTab := "activity",
          cls := List(
            "nm-item to-activity" -> true,
            "active" -> (angle == UserInfo.Angle.Activity)
          ),
          href := routes.User.show(u.username)
        )(trans.activity.activity()),
        a(
          dataTab := "games",
          cls := List(
            "nm-item to-games" -> true,
            "active" -> (angle.key == "games")
          ),
          href := routes.User.gamesAll(u.username)
        )(
          trans.site.nbGames.plural(info.user.count.game, info.user.count.game.localize)
        )
      )
    )
