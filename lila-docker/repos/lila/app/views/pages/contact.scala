package views.pages

import lila.app.UiEnv.*
import lila.ui.Page
import scala.annotation.unused

object contact:
  private def emailLink(address: String) = a(href := s"mailto:$address")(address)

  def apply(contactEmail: Option[String])(using @unused ctx: Context): Page =
    Page("Contact - Chesstory")
      .css("legal")
      .wrap: _ =>
        main(cls := "legal-page")(
          div(cls := "legal-container")(
            st.article(cls := "legal-content")(
              header(cls := "legal-header")(
                h1("Contact and Privacy Requests"),
                p(cls := "legal-meta")("Chesstory is currently run as a personal beta project by an individual developer based in the Republic of Korea.")
              ),

              st.section(cls := "legal-section")(
                h2("1. Contact"),
                contactEmail.fold[Frag](
                  p(
                    "A public contact email has not been configured for this environment yet."
                  )
                )(email =>
                  p(
                    "General support, product questions, privacy requests, and legal notices can be sent to ",
                    emailLink(email),
                    "."
                  )
                )
              ),

              st.section(cls := "legal-section")(
                h2("2. What to Include"),
                ul(
                  li("Your Chesstory username, if you have one"),
                  li("The email address connected to your account"),
                  li("A short description of your question or request"),
                  li("If this is a deletion or privacy request, enough detail for us to verify and process it safely")
                )
              ),

              st.section(cls := "legal-section")(
                h2("3. What This Channel Covers"),
                ul(
                  li("Account recovery or account security issues"),
                  li("Privacy requests such as access, correction, or deletion"),
                  li("Bug reports, support questions, and feedback"),
                  li("Legal notices or rights-related questions")
                )
              ),

              st.section(cls := "legal-section")(
                h2("4. Related Policies"),
                p(
                  "For more detail about how Chesstory handles personal information, read the ",
                  a(href := routes.Main.privacy.url)("Privacy Policy"),
                  ". For the rules that govern use of the Service, read the ",
                  a(href := routes.Main.terms.url)("Terms of Service"),
                  "."
                )
              ),

              footer(cls := "legal-footer")(
                a(href := routes.Main.privacy.url, cls := "legal-link")("Privacy Policy"),
                span(" • "),
                a(href := routes.Main.terms.url, cls := "legal-link")("Terms of Service"),
                span(" • "),
                a(href := homeUrl, cls := "legal-link")("Back to Home")
              )
            )
          )
        )
