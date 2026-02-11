package views.llm

import lila.app.UiEnv.{ *, given }
import lila.ui.Page

object checkoutSuccess:

  def apply(me: Me)(using ctx: Context): Page =
    Page("Welcome to Analyst Pro!")
      .css("llm.plan")
      .wrap: _ =>
        main(cls := "llm-success-page")(
          div(cls := "success-container glass scale-up")(
            div(cls := "confetti-icon")("🎉"),
            h1("Welcome to the ", span(cls := "pro")("Pro"), " Tier!"),
            p(cls := "message")("Your account has been upgraded. 2,000 credits have been added to your balance."),
            div(cls := "unlocked-features")(
              h3("Unlocked Features:"),
              ul(
                li("✓ Gemini AI Strategic Polish"),
                li("✓ 2,000 Monthly Analysis Credits"),
                li("✓ Priority Analysis Pipeline")
              )
            ),
            a(href := routes.UserAnalysis.index.url, cls := "btn-pro-primary")("Start Analyzing Now")
          )
        )
