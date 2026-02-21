package views.llm

import lila.app.UiEnv.*
import lila.ui.Page
import scala.annotation.unused

object checkout:

  def apply(me: Me, checkoutEnabled: Boolean)(using @unused ctx: Context): Page =
    Page(if checkoutEnabled then "Secure Checkout — Analyst Pro" else "Checkout Unavailable — Analyst Pro")
      .css("llm.plan")
      .wrap: _ =>
        main(cls := "llm-checkout-page")(
          div(cls := "checkout-container glass fade-in")(
            div(cls := "checkout-header")(
              h1("Analyst ", span(cls := "pro")("Pro")),
              p(if checkoutEnabled then "Finalize your upgrade" else "Payment integration is not enabled yet")
            ),
            if checkoutEnabled then
              frag(
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
                  p(cls := "mock-note")("Development sandbox only. No real payment provider is connected.")
                ),
                postForm(action := "/plan/checkout/process")(
                  button(cls := "btn-pro-primary full-width")("Complete Sandbox Upgrade")
                )
              )
            else
              div(cls := "mock-payment")(
                p(cls := "mock-note")(
                  "Checkout is disabled in this deployment. ",
                  "No charges will be made and no automatic plan upgrade is available yet."
                ),
                a(href := routes.Main.contact.url, cls := "btn secondary")("Contact for Pro Access")
              ),
            a(href := "/plan", cls := "btn-text cancel")(if checkoutEnabled then "Cancel" else "Back to Plans")
          )
        )
