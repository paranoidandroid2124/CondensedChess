package views.pages

import lila.app.UiEnv.{ *, given }
import lila.ui.Page

object landing:

  def apply()(using ctx: Context): Page =
    val samplePgn =
      "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O Be7 6. Re1 b5 7. Bb3 d6 8. c3 O-O 9. h3 Nb8 10. d4 Nbd7 11. c4"
    val sampleAnalysisUrl = routes.UserAnalysis.pgn(samplePgn).url
    val lichessImportUrl = s"${routes.Importer.importGame.url}?provider=lichess"
    val chessComImportUrl = s"${routes.Importer.importGame.url}?provider=chesscom"

    Page("Chesstory — AI Chess Commentary")
      .css("landing")
      .wrap: _ =>
        frag(
          div(cls := "landing-orbs", aria.hidden := "true")(
            div(cls := "orb orb-1"),
            div(cls := "orb orb-2"),
            div(cls := "orb orb-3")
          ),
          div(cls := "landing-pro")(
            header(cls := "landing-header")(
              div(cls := "landing-container")(
                a(href := routes.Main.landing.url, cls := "logo")("Chesstory"),
                st.nav(cls := "section-nav", aria.label := "Landing sections")(
                  a(href := "#features", cls := "btn-text")("Features"),
                  a(href := "#sample-commentary", cls := "btn-text")("Sample"),
                  a(href := "#how-it-works", cls := "btn-text")("How it works"),
                  a(href := routes.Main.support.url, cls := "btn-text")("Support")
                ),
                div(cls := "auth-nav")(
                  ctx.me match
                    case Some(me) =>
                      frag(
                        a(href := routes.UserAnalysis.index.url, cls := "btn-text")("Analysis"),
                        a(href := routes.User.show(me.username).url, cls := "btn-text")(me.username.value),
                        postForm(action := routes.Auth.logout, cls := "logout-form")(
                          button(cls := "btn-text")("Log out")
                        )
                      )
                    case None =>
                      a(href := routes.Auth.login.url, cls := "btn-text")("Log in")
                )
              )
            ),
            main(cls := "landing-main")(
              st.section(id := "top", cls := "hero-section landing-section")(
                div(cls := "landing-container hero-grid")(
                  div(cls := "hero-copy")(
                    p(cls := "eyebrow")("Narrative-first chess intelligence"),
                    h1(
                      "Understand the game,",
                      br,
                      span("not just the eval bar.")
                    ),
                    p(cls := "hero-summary")(
                      "Chesstory turns your PGN into clear, book-quality commentary that explains ideas, plans, and turning points in plain language."
                    ),
                    ul(cls := "hero-points")(
                      li("Plan-by-plan commentary instead of isolated move blurbs."),
                      li("Readable quickly on both mobile and large desktop screens."),
                      li("Structured output for players, coaches, and study groups.")
                    ),
                    div(cls := "hero-audience", aria.label := "Audience fit")(
                      span("Players"),
                      span("Coaches"),
                      span("Creators")
                    ),
                    div(cls := "hero-cta")(
                      a(href := "#sample-commentary", cls := "btn-primary")("See Sample Commentary"),
                      a(href := routes.UserAnalysis.index.url, cls := "btn-secondary")("Start Analysis")
                    ),
                    div(cls := "hero-imports")(
                      p(cls := "import-label")("Quick import from"),
                      div(cls := "import-buttons")(
                        a(href := lichessImportUrl, cls := "btn-secondary import-provider")("Lichess"),
                        a(href := chessComImportUrl, cls := "btn-secondary import-provider")("Chess.com")
                      ),
                      p(cls := "import-note")("Independent workflow. Not affiliated with either platform.")
                    )
                  ),
                  div(
                    cls := "hero-visual",
                    attr("role") := "complementary",
                    aria.label := "Sample commentary preview"
                  )(
                    div(cls := "hero-board", aria.hidden := "true"),
                    div(cls := "sample-note")(
                      p(cls := "move")("19. Nf3"),
                      p(
                        "White consolidates development before forcing tactical play. The move keeps central tension while preparing a kingside initiative."
                      )
                    ),
                    div(cls := "sample-note")(
                      p(cls := "move")("... d5"),
                      p(
                        "Black chooses dynamic counterplay. Structurally risky, but it opens practical chances if White mishandles the transition."
                      )
                    ),
                    div(cls := "sample-note")(
                      p(cls := "move")("20. Bb5+"),
                      p(
                        "A forcing check that accelerates the position. From here, precision matters more than raw aggression."
                      )
                    ),
                    div(cls := "demo-meta")(
                      span("Demo line: Ruy Lopez structure"),
                      a(href := sampleAnalysisUrl, cls := "demo-link")("Open full sample")
                    )
                  )
                )
              ),
              st.section(id := "sample-commentary", cls := "sample-section landing-section")(
                div(cls := "landing-container section-grid")(
                  div(cls := "section-heading")(
                    p(cls := "section-kicker")("Sample commentary"),
                    h2("A concrete example before you commit"),
                    p(
                      "Explore how Chesstory blends tactical facts with strategic narrative. The output is designed to read like a strong coach note, not a machine dump."
                    ),
                    div(cls := "sample-actions")(
                      a(href := sampleAnalysisUrl, cls := "btn-secondary sample-open")("Open Interactive Sample")
                    )
                  ),
                  div(cls := "timeline-card")(
                    st.article(cls := "timeline-item")(
                      p(cls := "timeline-ply")("Move 13"),
                      p(cls := "timeline-tag")("Opening transition"),
                      p(cls := "timeline-text")(
                        "Both sides complete development, but White's bishop pair now has long-term pressure on dark squares."
                      )
                    ),
                    st.article(cls := "timeline-item")(
                      p(cls := "timeline-ply")("Move 19"),
                      p(cls := "timeline-tag")("Plan consolidation"),
                      p(cls := "timeline-text")(
                        "Nf3 keeps the center flexible and improves coordination. The position favors patient buildup over immediate liquidation."
                      )
                    ),
                    st.article(cls := "timeline-item")(
                      p(cls := "timeline-ply")("Move 26"),
                      p(cls := "timeline-tag")("Technical conversion"),
                      p(cls := "timeline-text")(
                        "After simplification, the game enters a technical phase where pawn structure and king activity decide conversion speed."
                      )
                    )
                  ),
                  div(cls := "sample-summary-card")(
                    p(cls := "sample-label")("What this gives you"),
                    ul(
                      li("Move-by-move explanation anchored to plans"),
                      li("Clear transitions: opening, middlegame, endgame"),
                      li("Readable language for review and sharing")
                    ),
                    p(cls := "sample-footnote")(
                      "Best used after your own game review, opening prep sessions, or coach feedback loops."
                    )
                  )
                )
              ),
              st.section(id := "features", cls := "features-section landing-section")(
                div(cls := "landing-container")(
                  div(cls := "section-heading")(
                    p(cls := "section-kicker")("Why Chesstory"),
                    h2("Built for players who want understanding")
                  ),
                  div(cls := "feature-grid")(
                    st.article(cls := "feature-card")(
                      h3("Narrative-first analysis"),
                      p("Get prose that explains intent and consequences, not only move rankings."),
                      ul(
                        li("Connects tactical motifs to strategic plans"),
                        li("Highlights why alternatives were less practical")
                      )
                    ),
                    st.article(cls := "feature-card")(
                      h3("Plan-level reasoning"),
                      p("Track the strategic thread across moves so transitions make sense at a glance."),
                      ul(
                        li("Keeps narrative continuity across game phases"),
                        li("Frames critical moments around practical choices")
                      )
                    ),
                    st.article(cls := "feature-card")(
                      h3("Study-ready output"),
                      p("Store, revisit, and share commentary as a durable learning asset."),
                      ul(
                        li("Easy to revisit during spaced repetition"),
                        li("Readable enough to share with teammates or students")
                      )
                    )
                  )
                )
              ),
              st.section(id := "how-it-works", cls := "workflow-section landing-section")(
                div(cls := "landing-container")(
                  div(cls := "section-heading")(
                    p(cls := "section-kicker")("How it works"),
                    h2("From PGN to practical insight in three steps")
                  ),
                  ol(cls := "workflow-grid")(
                    li(cls := "workflow-step")(
                      span(cls := "step-index")("01"),
                      h3("Import PGN"),
                      p("Paste or upload your game and prepare the position context."),
                      p(cls := "step-output")("Output: clean move timeline with board context")
                    ),
                    li(cls := "workflow-step")(
                      span(cls := "step-index")("02"),
                      h3("Generate Commentary"),
                      p("Chesstory analyzes moves and produces coherent, structured narrative."),
                      p(cls := "step-output")("Output: phase-aware explanation and plan flow")
                    ),
                    li(cls := "workflow-step")(
                      span(cls := "step-index")("03"),
                      h3("Save and Revisit"),
                      p("Keep commentary in your study flow and return when preparing openings or review sessions."),
                      p(cls := "step-output")("Output: reusable notes for prep and coaching")
                    )
                  )
                )
              ),
              st.section(id := "quality", cls := "quality-section landing-section")(
                div(cls := "landing-container quality-grid")(
                  div(cls := "section-heading")(
                    p(cls := "section-kicker")("Quality proof"),
                    h2("Designed for signal over noise"),
                    p("Every commentary run emphasizes clarity, structure, and actionable interpretation.")
                  ),
                  div(cls := "quality-card")(
                    p(cls := "quality-label")("Scorecard format (example)"),
                    div(cls := "quality-metrics")(
                      div(cls := "metric-row")(
                        span("Anchor coverage"),
                        strong(cls := "metric-value")("5/5 referenced moves")
                      ),
                      div(cls := "metric-row")(
                        span("Phase continuity"),
                        strong(cls := "metric-value")("Opening -> Middlegame -> Conversion")
                      ),
                      div(cls := "metric-row")(
                        span("Actionability"),
                        strong(cls := "metric-value")("3 concrete plan takeaways")
                      )
                    ),
                    ul(
                      li("Narrative output keeps tactical facts grounded in board reality."),
                      li("Sectioned flow improves quick review on both desktop and mobile."),
                      li("Language stays concise enough for repeat study cycles.")
                    ),
                    div(cls := "quality-pills")(
                      span("Move anchors"),
                      span("Phase transitions"),
                      span("Plan continuity"),
                      span("Practical tone")
                    ),
                    p(cls := "quality-note")(
                      "Numbers above illustrate how commentary quality is reviewed on a real sample before publishing."
                    ),
                    a(href := routes.UserAnalysis.index.url, cls := "btn-primary final-cta")("Start Analysis")
                  )
                )
              )
            ),
            footer(cls := "landing-footer")(
              div(cls := "landing-container footer-links")(
                span("© 2026 Chesstory"),
                a(href := routes.Main.support.url)("Support"),
                a(href := routes.Main.source.url)("Open Source"),
                a(href := routes.Main.privacy.url)("Privacy"),
                a(href := routes.Main.terms.url)("Terms")
              )
            )
          )
        )
