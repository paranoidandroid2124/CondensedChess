package lila.web
package ui

import scalalib.model.Days
import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class TopNav(helpers: Helpers):
  import helpers.{ *, given }

  private def linkTitle(url: String, name: Frag)(using ctx: Context) =
    if ctx.blind then h3(name) else a(href := url)(name)

  def apply(hasClas: Boolean, hasDgt: Boolean)(using ctx: Context) =
    val canSeeClasMenu = hasClas || ctx.me.exists(u => u.hasTitle || u.roles.contains("ROLE_COACH"))
    st.nav(id := "topnav", cls := "hover")(
      st.section(
        linkTitle(
          "/",
          frag(
            span(cls := "play")(trans.site.play()),
            span(cls := "home")("lichess.org")
          )
        ),
        div(role := "group")(
          if ctx.noBot then a(href := s"${langHref("/")}?any#hook")(trans.site.createLobbyGame())
          else a(href := "/?any#friend")(trans.site.challengeAFriend()),
          Option.when(ctx.noBot):
            frag(
              (ctx.kid.no && !ctx.me.exists(_.isPatron)).option:
                a(cls := "community-patron mobile-only", href := routes.Plan.index())(trans.patron.donate())
            )
        )
      ),
      st.section(
        linkTitle(langHref(routes.Study.allDefault().url), trans.site.learnMenu()),
        div(role := "group")(
          
          a(href := langHref(routes.Study.allDefault()))(trans.site.studyMenu()),
        )
      ),
      st.section:
        val broadcastUrl = langHref(routes.Tv.index.url)
        frag(
          linkTitle(broadcastUrl, trans.site.watch()),
          div(role := "group")(
            a(href := langHref(routes.Tv.index))("Lichess TV"),
            a(href := routes.Tv.games)(trans.site.currentGames()),
          )
        )
      ,
      st.section(
        linkTitle(routes.User.list.url, trans.site.community()),
        div(role := "group")(
          a(href := routes.User.list)(trans.site.players()),
          ctx.me.map(me => a(href := routes.Relation.following(me.username))(trans.site.friends())),
          (ctx.kid.no && ctx.me.exists(_.isPatron))
            .option(a(cls := "community-patron", href := routes.Plan.index())(trans.patron.donate()))
        )
      ),
      st.section(
        linkTitle(routes.UserAnalysis.index.url, trans.site.tools()),
        div(role := "group")(
          a(href := routes.UserAnalysis.index)(trans.site.analysis()),
          a(href := routes.Opening.index())(trans.site.openings()),
          a(href := routes.Editor.index)(trans.site.boardEditor()),
          a(href := routes.Importer.importGame)(trans.site.importGame()),
          a(href := routes.Search.index())(trans.search.advancedSearch())
        )
      )
    )
