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
                p(cls := "legal-meta")("Core analysis stays open. Deeper commentary paths still follow current usage limits.")
              ),
              st.section(cls := "legal-section")(
                h2("Why Support"),
                p(
                  "Chesstory keeps the board and review workflow available without a public paid subscription. ",
                  "If this helps your study workflow, support goes toward hosting, maintenance, and keeping the free tier sustainable while deeper review features stay online."
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
                h2("Open Beta Feedback"),
                p(
                  "If you just used Strategic Puzzle, Game Chronicle, or another core analysis surface, you can tell us whether it already feels worth paying for and optionally join the paid-plan waitlist."
                ),
                div(cls := "legal-actions")(
                  a(
                    href := routes.BetaFeedback.formPage(
                      surface = "general",
                      feature = "support_page",
                      entrypoint = "support_page",
                      returnTo = routes.Main.support.url,
                      notify = false
                    ).url,
                    cls := "button"
                  )("Share beta feedback"),
                  a(
                    href := routes.BetaFeedback.formPage(
                      surface = "general",
                      feature = "paid_plan_waitlist",
                      entrypoint = "support_waitlist",
                      returnTo = routes.Main.support.url,
                      notify = true
                    ).url,
                    cls := "button button-empty"
                  )("Join paid-plan waitlist")
                )
              ),
              st.section(cls := "legal-section")(
                h2("Current Access Policy"),
                p("Support helps sustain the service, but access still follows the product's current login, quota, and fair-use controls."),
                ul(
                  li("Anonymous full-game review is currently limited to 1 Game Chronicle request per day per IP."),
                  li("Signed-in free access is currently limited to 1 full-game review per day and 100 Bookmaker move requests per day."),
                  li("Additional burst or fair-use controls may apply on higher-access beta tiers."),
                  li("Support does not bypass safety, abuse-prevention, or quota controls.")
                )
              ),
              footer(cls := "legal-footer")(
                a(href := routes.Main.landing.url, cls := "legal-link")("Back to Home")
              )
            )
          )
        )
