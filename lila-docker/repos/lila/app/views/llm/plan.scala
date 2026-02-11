package views.llm

import lila.app.UiEnv.{ *, given }
import lila.ui.Page

object plan:

  def apply(me: Me, status: lila.llm.CreditApi.CreditStatus)(using ctx: Context): Page =
    Page("Analyst Pro — Plans & Credits")
      .css("llm.plan")
      .wrap: _ =>
        main(cls := "llm-plan-page")(
          div(cls := "hero")(
            h1("Analyst ", span(cls := "pro")("Pro")),
            p(cls := "subtitle")("Powering your chess analysis with deep strategic insights.")
          ),
          div(cls := "credit-usage fade-in")(
            div(cls := "usage-card glass")(
              div(cls := "usage-chart")(
                svgTag(viewBoxAttr := "0 0 36 36", cls := "circular-chart")(
                  tag("path")(
                    cls := "circle-bg",
                    attr("d") := "M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                  ),
                  tag("path")(
                    cls := "circle",
                    attr("stroke-dasharray") := s"${(status.remaining.toDouble / status.maxCredits.toDouble * 100).toInt}, 100",
                    attr("d") := "M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                  ),
                  svgTextTag(attr("x") := "18", attr("y") := "20.35", cls := "percentage")(status.remaining.toString)
                )
              ),
              div(cls := "usage-details")(
                h2("Your Credits"),
                p(cls := "status")(s"${status.remaining} / ${status.maxCredits} remaining"),
                p(cls := "reset")(
                  "Auto-reset on ",
                  strong(status.resetAt.toString.take(10))
                ),
                p(cls := "tier-tag " + status.tier.toLowerCase)(status.tier.toUpperCase + " TIER")
              )
            )
          ),
          div(cls := "tiers-grid")(
            // Free Tier
            div(cls := "tier-card " + (if status.tier == "free" then "active" else ""))(
              div(cls := "header")(
                h3("Free"),
                div(cls := "price")(
                  "$0",
                  span("/mo")
                )
              ),
              ul(cls := "features")(
                li(i(dataIcon := "L"), " 150 Analysis Credits / mo"),
                li(i(dataIcon := "L"), " Rule-based Commentary"),
                li(i(dataIcon := "L"), " Masters Database integration"),
                li(cls := "disabled")(i(dataIcon := "L"), " Gemini AI Polish")
              ),
              if status.tier == "free" then button(cls := "btn disabled")("Current Plan")
              else button(cls := "btn secondary")("Downgrade")
            ),
            // Pro Tier
            div(cls := "tier-card pro " + (if status.tier == "pro" then "active" else ""))(
              div(cls := "premium-badge")("BEST VALUE"),
              div(cls := "header")(
                h3("Pro"),
                div(cls := "price")(
                  "$5",
                  span("/mo")
                )
              ),
              ul(cls := "features")(
                li(i(dataIcon := "L"), " 2,000 Analysis Credits / mo"),
                li(i(dataIcon := "L"), strong("Gemini AI Polish")),
                li(i(dataIcon := "L"), " Deep Strategic Insights"),
                li(i(dataIcon := "L"), " Priority Analysis Pipeline")
              ),
              if status.tier == "pro" then button(cls := "btn disabled")("Current Plan")
              else button(cls := "btn primary")("Upgrade to Pro")
            )
          )
        )
