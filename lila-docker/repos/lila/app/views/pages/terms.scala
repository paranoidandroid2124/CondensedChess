package views.pages

import lila.app.UiEnv.{ *, given }
import lila.ui.Page

object terms:
  def apply()(using ctx: Context): Page =
    Page("Terms of Service - Chesstory")
      .css("legal")
      .wrap: _ =>
        main(cls := "legal-page")(
          div(cls := "legal-container")(
            st.article(cls := "legal-content")(
              header(cls := "legal-header")(
                h1("Terms of Service"),
                p(cls := "legal-meta")("Effective Date: February 1, 2026 • Last Updated: February 5, 2026")
              ),

              st.section(cls := "legal-section")(
                h2("1. Agreement to Terms"),
                p(
                  "By accessing or using Chesstory (the \"Service\"), you agree to be bound by these Terms of Service. ",
                  "If you do not agree to these terms, please do not use the Service."
                ),
                p(
                  "These terms constitute a legally binding agreement between you and Chesstory regarding your use of the Service."
                )
              ),

              st.section(cls := "legal-section")(
                h2("2. Description of Service"),
                p(
                  "Chesstory is an AI-powered chess analysis platform that provides:"
                ),
                ul(
                  li("Deep position and game analysis using artificial intelligence"),
                  li("Narrative-style explanations of chess concepts and moves"),
                  li("Study creation and management tools"),
                  li("PGN import and export capabilities")
                )
              ),

              st.section(cls := "legal-section")(
                h2("3. User Accounts"),
                h3("3.1 Registration"),
                p(
                  "To access certain features, you must create an account using a valid email address. ",
                  "You are responsible for maintaining the confidentiality of your account."
                ),
                h3("3.2 Account Security"),
                p(
                  "You agree to notify us immediately of any unauthorized use of your account. ",
                  "We use Magic Link authentication for enhanced security."
                )
              ),

              st.section(cls := "legal-section")(
                h2("4. Acceptable Use"),
                p("You agree ", strong("not"), " to:"),
                ul(
                  li("Use the Service for any unlawful purpose"),
                  li("Attempt to overload, disrupt, or damage our servers or infrastructure"),
                  li("Scrape, crawl, or use automated tools to access the Service without permission"),
                  li("Reverse engineer or attempt to extract source code from the Service"),
                  li("Use the Service to develop competing products"),
                  li("Impersonate others or provide false information"),
                  li("Share your account credentials with others")
                )
              ),

              st.section(cls := "legal-section")(
                h2("5. Intellectual Property"),
                h3("5.1 Our Content"),
                p(
                  "The Service, including its design, code, and AI models, is owned by Chesstory and protected by ",
                  "intellectual property laws. You may not copy, modify, or distribute our content without permission."
                ),
                h3("5.2 Your Content"),
                p(
                  "You retain ownership of the chess games and analysis you create. By using the Service, you grant us ",
                  "a license to store, process, and display your content to provide the Service."
                )
              ),

              st.section(cls := "legal-section")(
                h2("6. AI-Generated Content"),
                p(
                  "Chesstory uses artificial intelligence to generate analysis and commentary. While we strive for accuracy:"
                ),
                ul(
                  li("AI-generated content is for informational and educational purposes only"),
                  li("We do not guarantee the accuracy or completeness of AI analysis"),
                  li("AI insights should not be the sole basis for competitive or financial decisions")
                )
              ),

              st.section(cls := "legal-section")(
                h2("7. Limitation of Liability"),
                p(
                  "TO THE MAXIMUM EXTENT PERMITTED BY LAW, CHESSTORY SHALL NOT BE LIABLE FOR ANY INDIRECT, ",
                  "INCIDENTAL, SPECIAL, CONSEQUENTIAL, OR PUNITIVE DAMAGES ARISING FROM YOUR USE OF THE SERVICE."
                ),
                p(
                  "Our total liability for any claims relating to the Service shall not exceed the amount you paid ",
                  "to us (if any) in the twelve months preceding the claim."
                )
              ),

              st.section(cls := "legal-section")(
                h2("8. Disclaimer of Warranties"),
                p(
                  "THE SERVICE IS PROVIDED \"AS IS\" AND \"AS AVAILABLE\" WITHOUT WARRANTIES OF ANY KIND, ",
                  "EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES OF MERCHANTABILITY, ",
                  "FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT."
                ),
                p(
                  "We do not warrant that the Service will be uninterrupted, error-free, or secure."
                )
              ),

              st.section(cls := "legal-section")(
                h2("9. Indemnification"),
                p(
                  "You agree to indemnify and hold harmless Chesstory, its officers, directors, employees, and agents ",
                  "from any claims, damages, or expenses arising from your use of the Service or violation of these Terms."
                )
              ),

              st.section(cls := "legal-section")(
                h2("10. Termination"),
                p(
                  "We may suspend or terminate your access to the Service at any time, with or without cause. ",
                  "You may also terminate your account at any time by contacting us."
                ),
                p(
                  "Upon termination, your right to use the Service will immediately cease. Provisions that by their ",
                  "nature should survive termination (e.g., limitations of liability) will continue to apply."
                )
              ),

              st.section(cls := "legal-section")(
                h2("11. Changes to Terms"),
                p(
                  "We reserve the right to modify these Terms at any time. We will notify you of significant changes ",
                  "by posting the updated terms on this page. Continued use of the Service after changes constitutes ",
                  "acceptance of the new Terms."
                )
              ),

              st.section(cls := "legal-section")(
                h2("12. Governing Law"),
                p(
                  "These Terms shall be governed by and construed in accordance with applicable laws, without regard ",
                  "to conflict of law principles. Any disputes arising from these Terms shall be resolved in the ",
                  "appropriate courts."
                )
              ),

              st.section(cls := "legal-section")(
                h2("13. Severability"),
                p(
                  "If any provision of these Terms is found to be unenforceable, the remaining provisions will continue ",
                  "in full force and effect."
                )
              ),

              st.section(cls := "legal-section")(
                h2("14. Contact"),
                p("For questions about these Terms of Service, please contact us:"),
                ul(
                  li("Email: ", a(href := "mailto:legal@chesstory.com")("legal@chesstory.com")),
                  li("Support: ", a(href := routes.Main.contact.url)("Contact Page"))
                )
              ),

              footer(cls := "legal-footer")(
                a(href := routes.Main.privacy.url, cls := "legal-link")("Privacy Policy"),
                span(" • "),
                a(href := routes.Main.landing.url, cls := "legal-link")("Back to Home")
              )
            )
          )
        )
