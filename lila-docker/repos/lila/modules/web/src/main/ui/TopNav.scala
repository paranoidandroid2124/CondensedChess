package lila.web
package ui

import lila.ui.Helpers
import lila.ui.ScalatagsTemplate.{ *, given }

case class TopNav(helpers: Helpers):
  def apply(using ctx: lila.ui.PageContext): Frag =
    def isOn(prefix: String) = ctx.req.path == prefix || ctx.req.path.startsWith(prefix + "/")

    def item(hrefUrl: String, label: String, active: Boolean) =
      li(
        a(
          href := hrefUrl,
          cls := List(
            "topnav__link" -> true,
            "is-active" -> active
          ),
          aria("current") := Option.when(active)("page")
        )(label)
      )

    st.nav(id := "topnav")(
      ul(cls := "topnav__main")(
        item("/", "Home", ctx.req.path == "/"),
        item("/plan", "Analyst Pro", isOn("/plan")),
        item("/study", "Study", isOn("/study")),
        item("/analysis", "Analysis", isOn("/analysis"))
      )
    )
