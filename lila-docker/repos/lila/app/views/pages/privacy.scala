package views.pages

import lila.app.UiEnv.{ *, given }
import lila.ui.Page

object privacy:
  def apply()(using ctx: Context): Page =
    Page("Privacy Policy - Chesstory")
      .css("legal")
      .wrap: _ =>
        main(cls := "legal-page")(
          div(cls := "legal-container")(
            st.article(cls := "legal-content")(
              header(cls := "legal-header")(
                h1("Privacy Policy"),
                p(cls := "legal-meta")("Effective Date: February 1, 2026 • Last Updated: February 5, 2026")
              ),

              st.section(cls := "legal-section")(
                h2("1. Introduction"),
                p(
                  "Chesstory (\"we\", \"us\", or \"our\") is committed to protecting your privacy. ",
                  "This Privacy Policy explains how we collect, use, disclose, and safeguard your information ",
                  "when you use our chess analysis service."
                ),
                p(
                  "Please read this policy carefully. By using Chesstory, you consent to the practices described herein."
                )
              ),

              st.section(cls := "legal-section")(
                h2("2. Information We Collect"),
                h3("2.1 Information You Provide"),
                ul(
                  li(strong("Account Information: "), "Email address used for authentication via Magic Link."),
                  li(strong("Chess Data: "), "Games you import (PGN files), positions you analyze, and studies you create."),
                  li(strong("Preferences: "), "Display settings, theme choices, and board configurations.")
                ),
                h3("2.2 Information Collected Automatically"),
                ul(
                  li(strong("Usage Data: "), "Pages visited, features used, and time spent on the service."),
                  li(strong("Device Information: "), "Browser type, operating system, and device identifiers."),
                  li(strong("Log Data: "), "IP address, access times, and referring URLs.")
                )
              ),

              st.section(cls := "legal-section")(
                h2("3. How We Use Your Information"),
                p("We use the collected information to:"),
                ul(
                  li("Provide and maintain the Chesstory service"),
                  li("Authenticate your identity and secure your account"),
                  li("Generate AI-powered chess analysis and insights"),
                  li("Improve our algorithms and user experience"),
                  li("Respond to your requests and provide customer support"),
                  li("Send service-related communications (e.g., Magic Link emails)")
                )
              ),

              st.section(cls := "legal-section")(
                h2("4. Data Storage and Security"),
                p(
                  "Your data is stored on secure servers. We implement industry-standard security measures ",
                  "including encryption in transit (TLS) and at rest. However, no method of transmission over ",
                  "the Internet is 100% secure."
                ),
                p(
                  strong("Data Retention: "),
                  "We retain your data for as long as your account is active. You may request deletion of your ",
                  "account and associated data at any time."
                )
              ),

              st.section(cls := "legal-section")(
                h2("5. Data Sharing"),
                p("We do ", strong("not"), " sell, trade, or rent your personal information to third parties."),
                p("We may share data only in the following circumstances:"),
                ul(
                  li(strong("Service Providers: "), "Third-party services that help us operate (e.g., email delivery, hosting)."),
                  li(strong("Legal Requirements: "), "When required by law or to protect our rights."),
                  li(strong("Business Transfers: "), "In connection with a merger, acquisition, or sale of assets.")
                )
              ),

              st.section(cls := "legal-section")(
                h2("6. Your Rights"),
                p("Depending on your jurisdiction, you may have the right to:"),
                ul(
                  li("Access the personal data we hold about you"),
                  li("Request correction of inaccurate data"),
                  li("Request deletion of your data"),
                  li("Object to or restrict certain processing"),
                  li("Data portability (receive your data in a structured format)")
                ),
                p(
                  "To exercise these rights, please contact us at ",
                  a(href := "mailto:privacy@chesstory.com")("privacy@chesstory.com"),
                  "."
                )
              ),

              st.section(cls := "legal-section")(
                h2("7. Cookies"),
                p(
                  "We use essential cookies for authentication and session management. ",
                  "We do not use tracking or advertising cookies."
                )
              ),

              st.section(cls := "legal-section")(
                h2("8. Children's Privacy"),
                p(
                  "Chesstory is not intended for children under 13. We do not knowingly collect personal ",
                  "information from children under 13. If you believe we have collected such information, ",
                  "please contact us immediately."
                )
              ),

              st.section(cls := "legal-section")(
                h2("9. International Data Transfers"),
                p(
                  "Your data may be transferred to and processed in countries other than your own. ",
                  "We ensure appropriate safeguards are in place for such transfers."
                )
              ),

              st.section(cls := "legal-section")(
                h2("10. Changes to This Policy"),
                p(
                  "We may update this Privacy Policy from time to time. We will notify you of significant ",
                  "changes by posting the new policy on this page and updating the \"Last Updated\" date."
                )
              ),

              st.section(cls := "legal-section")(
                h2("11. Contact Us"),
                p("If you have questions about this Privacy Policy, please contact us:"),
                ul(
                  li("Email: ", a(href := "mailto:privacy@chesstory.com")("privacy@chesstory.com")),
                  li("Support: ", a(href := routes.Main.contact.url)("Contact Page"))
                )
              ),

              footer(cls := "legal-footer")(
                a(href := routes.Main.terms.url, cls := "legal-link")("Terms of Service"),
                span(" • "),
                a(href := routes.Main.landing.url, cls := "legal-link")("Back to Home")
              )
            )
          )
        )
