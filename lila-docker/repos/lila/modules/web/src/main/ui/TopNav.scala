package lila.web
package ui

import lila.ui.ScalatagsTemplate.*
import lila.ui.Helpers

case class TopNav(helpers: Helpers):
  def apply(using lila.ui.PageContext): Frag = frag(
    st.nav(id := "topnav")(
      ul(cls := "topnav__main")(
        li(a(href := "/site")("Site")),
        li(a(href := "/study")("Study")),
        li(a(href := "/analysis")("Analysis"))
      )
    )
  )
