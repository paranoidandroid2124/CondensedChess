package views.pages

import lila.app.UiEnv.*
import lila.ui.Page
import scala.annotation.unused

object support:
  def apply(
      patreon: Option[String],
      githubSponsors: Option[String],
      buyMeACoffee: Option[String]
  )(using @unused ctx: Context): Page =
    val links = List(
      "Patreon" -> patreon,
      "GitHub Sponsors" -> githubSponsors,
      "Buy Me a Coffee" -> buyMeACoffee
    ).collect { case (label, Some(url)) => label -> url }

    Page("Support Chesstory")
      .css("legal")
      .wrap: _ =>
        main(cls := "legal-page")(
          div(cls := "legal-container")(
            st.article(cls := "legal-content")(
              header(cls := "legal-header")(
                h1("Support Chesstory"),
                p(cls := "legal-meta")("Free analysis for everyone. Optional support keeps the project sustainable.")
              ),
              st.section(cls := "legal-section")(
                h2("Why Support"),
                p(
                  "Chesstory keeps core analysis and rule-based commentary freely available. ",
                  "If this helps your study workflow, you can support hosting, maintenance, and future improvements."
                )
              ),
              st.section(cls := "legal-section")(
                h2("Support Links"),
                if links.nonEmpty then
                  ul(
                    links.map { (label, url) =>
                      li(a(href := url, target := "_blank", rel := "noopener noreferrer")(label))
                    }
                  )
                else
                  p(
                    "Support links are not configured yet. ",
                    "For manual support options, please use ",
                    a(href := routes.Main.contact.url)("Contact"),
                    "."
                  )
              ),
              st.section(cls := "legal-section")(
                h2("Policy"),
                p("All chess analysis features remain available without a paid subscription.")
              ),
              footer(cls := "legal-footer")(
                a(href := routes.Main.landing.url, cls := "legal-link")("Back to Home")
              )
            )
          )
        )
