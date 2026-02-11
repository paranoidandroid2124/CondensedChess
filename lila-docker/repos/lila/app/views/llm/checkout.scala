package views.llm

import lila.app.UiEnv.{ *, given }
import lila.ui.Page

object checkout:

  def apply(me: Me)(using ctx: Context): Page =
    Page("Secure Checkout — Analyst Pro")
      .css("llm.plan")
      .wrap: _ =>
        main(cls := "llm-checkout-page")(
          div(cls := "checkout-container glass fade-in")(
            div(cls := "checkout-header")(
              h1("Analyst ", span(cls := "pro")("Pro")),
              p("Finalize your upgrade")
            ),
            div(cls := "summary-box")(
              div(cls := "summary-line")(
                span("Plan:"),
                span(cls := "val")("Analyst Pro (Monthly)")
              ),
              div(cls := "summary-line")(
                span("Total:"),
                span(cls := "price")("$5.00")
              )
            ),
            div(cls := "mock-payment")(
              div(cls := "card-placeholder")(
                div(cls := "chip"),
                div(cls := "number")("**** **** **** 2026"),
                div(cls := "name")(me.username.value.toUpperCase)
              ),
              p(cls := "mock-note")("This is a mock checkout for Chesstory 2026. No real funds will be charged.")
            ),
            postForm(action := "/plan/checkout/process")(
              button(cls := "btn-pro-primary full-width")("Complete Purchase")
            ),
            a(href := "/plan", cls := "btn-text cancel")("Cancel")
          )
        )
