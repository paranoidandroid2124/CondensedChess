package views.pages

import lila.app.UiEnv.{ *, given }
import lila.ui.Page

object openSource:
  def apply()(using ctx: Context): Page =
    Page("Open Source - Chesstory")
      .css("legal")
      .wrap: _ =>
        main(cls := "legal-page")(
          div(cls := "legal-container")(
            st.article(cls := "legal-content")(
              header(cls := "legal-header")(
                h1("Open Source Notice"),
                p(cls := "legal-meta")("Chesstory includes software licensed under GNU AGPL v3.")
              ),
              st.section(cls := "legal-section")(
                h2("Source Code Availability"),
                p(
                  "This service is based on AGPL-licensed components. ",
                  "The corresponding source code and license details are available at:"
                ),
                ul(
                  li(a(href := "https://lichess.org/source", target := "_blank", rel := "noopener")("Lichess Source & Licensing")),
                  li(
                    a(
                      href := "https://www.gnu.org/licenses/agpl-3.0.html#section13",
                      target := "_blank",
                      rel := "noopener"
                    )("GNU AGPL v3 Section 13")
                  ),
                  li(
                    a(
                      href := "https://raw.githubusercontent.com/lichess-org/lila/master/COPYING.md",
                      target := "_blank",
                      rel := "noopener"
                    )("Lila COPYING (including non-free exceptions)")
                  )
                )
              ),
              st.section(cls := "legal-section")(
                h2("Branding And Assets"),
                p(
                  "Some third-party or upstream assets may have additional restrictions. ",
                  "See the COPYING file for full details."
                )
              ),
              footer(cls := "legal-footer")(
                a(href := routes.Main.privacy.url, cls := "legal-link")("Privacy Policy"),
                " • ",
                a(href := routes.Main.terms.url, cls := "legal-link")("Terms of Service"),
                " • ",
                a(href := routes.Main.landing.url, cls := "legal-link")("Back to Home")
              )
            )
          )
        )
