package lila.web
package ui

import lila.ui.Helpers
import lila.ui.ScalatagsTemplate.{ *, given }
import ShellPrimitives.*

case class TopNav(helpers: Helpers):
  def apply(using ctx: lila.ui.PageContext): Frag =
    def isOn(prefix: String) = ctx.req.path == prefix || ctx.req.path.startsWith(prefix + "/")

    def item(
        hrefUrl: String,
        label: String,
        active: Boolean,
        titleText: Option[String] = None,
        mobileOnly: Boolean = false
    ) =
      li(cls := List("mobile-only" -> mobileOnly))(
        a(
          href := hrefUrl,
          cls := List(
            navLinkClass -> true,
            "is-active" -> active
          ),
          aria("current") := Option.when(active)("page"),
          title := titleText.getOrElse(label),
          aria.label := titleText.getOrElse(label)
        )(label)
      )

    st.nav(id := navId, aria.label := "Primary")(
      ul(cls := navMainClass)(
        item("/", "Home", ctx.req.path == "/", mobileOnly = true),
        item("/analysis", "Analysis", isOn("/analysis")),
        item("/strategic-puzzle", "Strategic Puzzles", isOn("/strategic-puzzle")),
        item("/notebook", "Notebook", isOn("/notebook")),
        item("/import", "Import Games", isOn("/import"), Some("Import recent public games")),
        item("/account-intel", "Account Reports", isOn("/account-intel"), Some("Open account reports"))
      )
    )
