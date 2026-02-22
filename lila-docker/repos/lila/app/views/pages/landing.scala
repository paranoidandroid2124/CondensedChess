package views.pages

import lila.app.UiEnv.{ *, given }
import lila.ui.Page

object landing:

  def apply()(using ctx: Context): Page =
    Page("Chesstory — AI Chess Commentary")
      .css("landing")
      .css("transitions")
      .wrap: _ =>
        frag(
          // Animated gradient orbs background
          div(cls := "landing-orbs")(
            div(cls := "orb orb-1"),
            div(cls := "orb orb-2"),
            div(cls := "orb orb-3")
          ),
          main(cls := "landing-pro")(
            div(cls := "landing-container")(
              // ── Header ──
              div(cls := "landing-header")(
                div(cls := "logo")("Chesstory"),
                div(cls := "auth-nav")(
                  ctx.me match
                    case Some(me) =>
                      frag(
                        a(href := "/support", cls := "btn-text")("Support"),
                        a(href := routes.UserAnalysis.index.url, cls := "btn-text")("Analysis"),
                        a(href := routes.User.show(me.username).url, cls := "btn-text")(me.username.value),
                        postForm(action := routes.Auth.logout, cls := "logout-form")(
                          button(cls := "btn-text")("Log out")
                        )
                      )
                    case None =>
                      frag(
                        a(href := "/support", cls := "btn-text")("Support"),
                        a(href := routes.Auth.login.url, cls := "btn-text")("Log in")
                      )
                )
              ),
              // ── Hero Section ──
              div(cls := "hero-section")(
                div(cls := "content")(
                  div(cls := "tag")(
                    div(cls := "pulse"),
                    "AI-Powered Analysis"
                  ),
                  h1("Deep Understanding,")(br, span(cls := "highlight")("Simplified.")),
                  p("Experience chess analysis reimagined as book-quality narrative commentary. Powered by AI, crafted for insight."),
                  a(href := routes.UserAnalysis.index.url, cls := "btn-pro-primary")(
                    "Start Analysis",
                    span(cls := "arrow")("→")
                  )
                ),
                div(cls := "visual-abstract")(
                  div(cls := "board-abstract"),
                  div(cls := "board-glass-overlay")(
                    div(cls := "overlay-line")(
                      div(cls := "overlay-dot green"),
                      "Nf3 — precise and consistent with the plan"
                    ),
                    div(cls := "overlay-line")(
                      div(cls := "overlay-dot amber"),
                      "d5 — introduces strategic tension"
                    ),
                    div(cls := "overlay-line")(
                      div(cls := "overlay-dot blue"),
                      "Bb5+ — forcing the pace of play"
                    )
                  )
                )
              ),
              // ── Feature Cards ──
              div(cls := "features-section")(
                div(cls := "section-label")("What makes Chesstory different"),
                div(cls := "feature-grid")(
                  div(cls := "feature-card")(
                    div(cls := "card-icon")("✦"),
                    h3("AI Commentary"),
                    p("Book-quality prose that explains every move, not just evaluation numbers. Understand the why behind each decision.")
                  ),
                  div(cls := "feature-card")(
                    div(cls := "card-icon")("◎"),
                    h3("Study Library"),
                    p("Build and curate your own analysis collections. Annotate, share, and revisit your deepest tactical discoveries.")
                  ),
                  div(cls := "feature-card")(
                    div(cls := "card-icon")("⟡"),
                    h3("Narrative Insight"),
                    p("Every game tells a story. Our engine weaves openings, tactics, and strategy into coherent, readable narratives.")
                  )
                )
              ),
              // ── Footer ──
              div(cls := "landing-footer")(
                div(cls := "footer-links")(
                  span("© 2026 Chesstory"),
                  a(href := routes.Main.privacy.url)("Privacy"),
                  a(href := routes.Main.terms.url)("Terms")
                )
              )
            )
          )
        )
