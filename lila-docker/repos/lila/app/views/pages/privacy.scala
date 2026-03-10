package views.pages

import lila.app.UiEnv.*
import lila.ui.Page
import scala.annotation.unused

object privacy:
  private def emailLink(address: String) = a(href := s"mailto:$address")(address)

  def apply(contactEmail: Option[String])(using @unused ctx: Context): Page =
    Page("Privacy Policy - Chesstory")
      .css("legal")
      .wrap: _ =>
        main(cls := "legal-page")(
          div(cls := "legal-container")(
            st.article(cls := "legal-content")(
              header(cls := "legal-header")(
                h1("Privacy Policy"),
                p(cls := "legal-meta")("Effective Date: March 10, 2026 • Last Updated: March 10, 2026")
              ),

              st.section(cls := "legal-section")(
                h2("1. Overview"),
                p(
                  "This Privacy Policy explains how Chesstory collects, uses, and handles personal information."
                ),
                p(
                  "Chesstory is currently operated as a personal beta project by an individual developer based in the Republic of Korea."
                )
              ),

              st.section(cls := "legal-section")(
                h2("2. Information We Collect"),
                ul(
                  li(strong("Account data: "), "email address, username, and password hash or other authentication-related records needed to run your account"),
                  li(strong("Chess content: "), "games, PGNs, studies, positions, and analysis inputs you submit or save"),
                  li(strong("Support communications: "), "messages you send through email or the contact page"),
                  li(strong("Technical and security data: "), "IP address, browser and device information, request logs, and abuse-prevention signals"),
                  li(strong("Public chess data: "), "if you request imports or opponent analysis, Chesstory may fetch public game data from third-party chess platforms")
                )
              ),

              st.section(cls := "legal-section")(
                h2("3. How We Use Information"),
                ul(
                  li("Provide, maintain, and secure the Service"),
                  li("Authenticate accounts and send account-related emails such as verification and password reset messages"),
                  li("Generate analysis, commentary, and product features you request"),
                  li("Prevent abuse, fraud, and unauthorized access"),
                  li("Respond to support, privacy, and account requests"),
                  li("Improve reliability and product quality")
                )
              ),

              st.section(cls := "legal-section")(
                h2("4. Sharing and Service Providers"),
                p("We do not sell or rent your personal information."),
                p("We may share limited information with service providers when reasonably necessary to operate Chesstory, including:"),
                ul(
                  li("Hosting and infrastructure providers"),
                  li("Email delivery providers"),
                  li("AI or model providers when needed to generate analysis features"),
                  li("Anti-abuse or captcha providers when needed to protect the Service"),
                  li("Authorities or counterparties when required by law or reasonably necessary to protect rights, safety, or the Service")
                )
              ),

              st.section(cls := "legal-section")(
                h2("5. Cookies and Similar Technologies"),
                p("Chesstory uses mainly essential cookies and similar local storage needed to run the Service."),
                ul(
                  li(strong("Session and sign-in cookies: "), "keep you signed in and protect your account session"),
                  li(strong("Security cookies or tokens: "), "support email confirmation, password reset, and related account security flows"),
                  li(strong("Preference storage: "), "remember interface settings such as accessibility or display preferences"),
                  li(strong("Anti-abuse tools: "), "if captcha or similar protection is enabled, those providers may collect device or browser signals under their own policies")
                ),
                p(
                  "Chesstory does not currently use advertising cookies or cross-site tracking cookies."
                )
              ),

              st.section(cls := "legal-section")(
                h2("6. Retention, Closure, and Deletion"),
                p(
                  "We keep account and service data for as long as needed to operate the Service, secure accounts, and handle legitimate support and operational needs."
                ),
                p(
                  "Closing your account disables access, but deletion is handled through a separate request process. ",
                  "If you request erasure, the request is reviewed and processed manually. ",
                  "Some backups, logs, or security records may remain for a limited period before they age out or are overwritten."
                ),
                p(
                  "We may retain limited records where reasonably necessary for security, fraud prevention, or legal compliance."
                )
              ),

              st.section(cls := "legal-section")(
                h2("7. International Processing"),
                p(
                  "Chesstory is operated from the Republic of Korea, and some service providers may process data in other countries. ",
                  "By using the Service, you understand that data may be processed outside your home jurisdiction."
                )
              ),

              st.section(cls := "legal-section")(
                h2("8. Children"),
                p(
                  "Chesstory is not intended for users under 14 years old. ",
                  "If you believe a child under 14 has provided personal information to Chesstory, contact us so we can review and respond."
                )
              ),

              st.section(cls := "legal-section")(
                h2("9. Your Choices and Requests"),
                p("You may have rights to request access, correction, or deletion of your information, depending on applicable law."),
                p(
                  "You can also manage some account information directly inside the Service, including email and password settings when available."
                ),
                contactEmail.fold[Frag](
                  p(
                    "For privacy or account requests, please use the ",
                    a(href := routes.Main.contact.url)("Contact page"),
                    "."
                  )
                )(email =>
                  p(
                    "For privacy or account requests, contact ",
                    emailLink(email),
                    ". Please include your Chesstory username, account email address, and a short description of your request."
                  )
                )
              ),

              st.section(cls := "legal-section")(
                h2("10. Changes to This Policy"),
                p(
                  "We may update this Privacy Policy from time to time. We will post the updated version on this page and update the Last Updated date."
                )
              ),

              footer(cls := "legal-footer")(
                a(href := routes.Main.terms.url, cls := "legal-link")("Terms of Service"),
                span(" • "),
                a(href := routes.Main.contact.url, cls := "legal-link")("Contact"),
                span(" • "),
                a(href := routes.Main.landing.url, cls := "legal-link")("Back to Home")
              )
            )
          )
        )
