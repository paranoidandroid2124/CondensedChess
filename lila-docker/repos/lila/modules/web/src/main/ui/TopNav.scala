package lila.web
package ui

import lila.ui.Helpers
import lila.ui.ScalatagsTemplate.{ *, given }
import ShellPrimitives.*

case class TopNav(helpers: Helpers):
  def apply(using ctx: lila.ui.PageContext): Frag =
    def isOn(prefix: String) = ctx.req.path == prefix || ctx.req.path.startsWith(prefix + "/")

    def item(hrefUrl: String, label: String, active: Boolean) =
      li(
        a(
          href := hrefUrl,
          cls := List(
            navLinkClass -> true,
            "is-active" -> active
          ),
          aria("current") := Option.when(active)("page")
        )(label)
      )

    st.nav(id := navId)(
      ul(cls := navMainClass)(
        item("/", "Home", ctx.req.path == "/"),
        item("/journal", "Journal", isOn("/journal")),
        item("/analysis", "Analysis", isOn("/analysis")),
        item("/strategic-puzzle", "Strategic Puzzles", isOn("/strategic-puzzle")),
        item("/notebook", "Notebook", isOn("/notebook")),
        item("/import", "Import", isOn("/import")),
        item("/account-intel", "Account Intel", isOn("/account-intel")),
        item("/support", "Support", isOn("/support") || isOn("/plan"))
      )
    )
