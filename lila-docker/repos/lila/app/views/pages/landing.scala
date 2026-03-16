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

    Page("Chesstory - AI Chess Commentary")
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
                    p(cls := "eyebrow")("Two review modes, one board"),
                    h1(
                      "Explain the move,",
                      br,
                      span("or review the whole game.")
                    ),
                    p(cls := "hero-summary")(
                      "Chesstory already has two real analysis paths: Bookmaker for on-demand move commentary, and Game Chronicle for full-game review with moments, repair windows, and pattern tracking."
                    ),
                    ul(cls := "hero-points")(
                      li("Ask for commentary on the current move only when you want it."),
                      li("Run a deeper review when you need turning points, repair windows, and patterns."),
                      li("Keep the result in your study flow with imports, saved history, and study sync.")
                    ),
                    div(cls := "hero-modes", aria.label := "Product surfaces")(
                      span("Bookmaker"),
                      span("Game Chronicle"),
                      span("Repair"),
                      span("Study Memory")
                    ),
                    div(cls := "hero-cta")(
                      a(href := "#sample-commentary", cls := "btn-primary")("See Product Sample"),
                      a(href := routes.UserAnalysis.index.url, cls := "btn-secondary")("Start Analysis")
                    ),
                    div(cls := "hero-imports")(
                      p(cls := "import-label")("Quick import from"),
                      div(cls := "import-buttons")(
                        a(href := lichessImportUrl, cls := "btn-secondary import-provider")("Lichess"),
                        a(href := chessComImportUrl, cls := "btn-secondary import-provider")("Chess.com")
                      ),
                      p(cls := "import-note")("Independent workflow. Not affiliated with either platform.")
                    ),
                    p(cls := "hero-disclosure")(
                      "Bookmaker stays on demand. Game Chronicle runs deeper and can take longer on large PGNs."
                    )
                  ),
                  div(
                    cls := "hero-visual",
                    attr("role") := "complementary",
                    aria.label := "Product surface preview"
                  )(
                    st.article(cls := "hero-surface hero-surface--move")(
                      div(cls := "hero-surface-head")(
                        span(cls := "hero-surface-kicker")("Bookmaker"),
                        span(cls := "hero-surface-meta")("On-demand move insight")
                      ),
                      h3(cls := "hero-surface-title")("19. Nf3"),
                      p(cls := "hero-surface-copy")(
                        "The move matters less as a forcing shot than as a way to keep the center flexible while White finishes the kingside setup."
                      ),
                      p(cls := "hero-surface-copy")(
                        "That matters because the knight improves coordination without releasing Black's counterplay too early."
                      ),
                      div(cls := "sample-chip-row")(
                        span("Explain this move"),
                        span("Interactive refs"),
                        span("Saved in study")
                      )
                    ),
                    st.article(cls := "hero-surface hero-surface--review")(
                      div(cls := "hero-surface-head")(
                        span(cls := "hero-surface-kicker")("Game Chronicle"),
                        span(cls := "hero-surface-meta")("Full-game review")
                      ),
                      div(cls := "hero-review-stack")(
                        div(cls := "hero-review-item")(
                          strong("Overview"),
                          span("3 selected moments, 1 repair window")
                        ),
                        div(cls := "hero-review-item")(
                          strong("Repair"),
                          span("22-27 Tactical Oversight")
                        ),
                        div(cls := "hero-review-item")(
                          strong("Patterns"),
                          span("Builds a long-term profile after more deep reviews")
                        )
                      )
                    ),
                    div(cls := "hero-visual-footer")(
                      span("Live sample: imported PGN to review shell"),
                      a(href := sampleAnalysisUrl, cls := "demo-link")("Open full sample")
                    )
                  )
                )
              ),
              st.section(id := "proof", cls := "proof-section landing-section")(
                div(cls := "landing-container proof-grid")(
                  st.article(cls := "proof-card")(
                    strong(cls := "proof-value")("On demand"),
                    p(cls := "proof-label")("Bookmaker"),
                    p(cls := "proof-copy")(
                      "Move commentary starts only when requested, instead of firing on every board change."
                    )
                  ),
                  st.article(cls := "proof-card")(
                    strong(cls := "proof-value")("Full review"),
                    p(cls := "proof-label")("Game Chronicle"),
                    p(cls := "proof-copy")(
                      "The deeper review shell already exposes overview, moments, repair, patterns, moves, and reference."
                    )
                  ),
                  st.article(cls := "proof-card")(
                    strong(cls := "proof-value")("Repair"),
                    p(cls := "proof-label")("Collapse and patch replay"),
                    p(cls := "proof-copy")(
                      "Critical games can surface the collapse interval, earliest preventable ply, and replayable patch line."
                    )
                  ),
                  st.article(cls := "proof-card")(
                    strong(cls := "proof-value")("Study-ready"),
                    p(cls := "proof-label")("Memory and sync"),
                    p(cls := "proof-copy")(
                      "Saved commentary can restore from session state, study snapshots, and recent imported analysis history."
                    )
                  )
                )
              ),
              st.section(id := "sample-commentary", cls := "sample-section landing-section")(
                div(cls := "landing-container sample-grid")(
                  div(cls := "section-heading")(
                    p(cls := "section-kicker")("Product sample"),
                    h2("See prompt-shaped output before you commit"),
                    p(
                      "These samples are not copied outputs, but they now follow the same prompt contracts that shape the shipped product."
                    ),
                    div(cls := "sample-actions")(
                      a(href := sampleAnalysisUrl, cls := "btn-secondary sample-open")("Open Interactive Sample")
                    )
                  ),
                  div(cls := "sample-panel-grid")(
                    st.article(cls := "sample-mode-card sample-mode-card--move")(
                      p(cls := "sample-kicker")("Quick Move Insight"),
                      h3(cls := "sample-mode-title")("Bookmaker: explain the current move"),
                      p(cls := "sample-mode-copy")(
                        "The move matters less as an immediate threat than as a way to hold the center together while White finishes the kingside setup."
                      ),
                      p(cls := "sample-mode-copy")(
                        "That matters because the knight improves coordination without handing Black an early queenside release. The sharper alternative stays secondary because forcing the break now would give counterplay before the attack is ready."
                      ),
                      div(cls := "sample-chip-row")(
                        span("One move"),
                        span("On demand"),
                        span("Interactive")
                      )
                    ),
                    st.article(cls := "sample-mode-card sample-mode-card--review")(
                      p(cls := "sample-kicker")("Full Game Review"),
                      h3(cls := "sample-mode-title")("Game Chronicle: review the whole game"),
                      p(cls := "sample-mode-copy")(
                        "Because Black's queenside counterplay is about to become the only active resource, this is the moment to finish White's regrouping rather than simplify too early."
                      ),
                      p(cls := "sample-mode-copy")(
                        "The review note stays forward-looking: carry the knight toward the kingside, keep the center closed while the attack is still maturing, and be ready to switch plans if ...c5 lands under better conditions."
                      ),
                      div(cls := "sample-mini-list")(
                        div(cls := "sample-mini-item")(
                          strong("Moments"),
                          span("Move 24 is surfaced as the turning point because it releases Black's clean queenside break and changes who owns the easier plan.")
                        ),
                        div(cls := "sample-mini-item")(
                          strong("Repair"),
                          span("The review marks the 22-27 collapse window, points to the earliest preventable choice, and then shows a patch replay line.")
                        ),
                        div(cls := "sample-mini-item")(
                          strong("Patterns"),
                          span("Across enough deep reviews, similar collapses can be grouped into recurring causes such as tactical oversight or drift against counterplay.")
                        )
                      )
                    )
                  ),
                  div(cls := "sample-summary-card")(
                    p(cls := "sample-label")("What this sample proves"),
                    ul(
                      li("Bookmaker prose is claim-first and cause-driven, while the rest of the detail stays in structured UI blocks."),
                      li("Game Chronicle notes are why-now-first and forward-looking, instead of paraphrasing the current board."),
                      li("The review shell still carries moments, repair, patterns, imports, saved analyses, and study reuse around that prose.")
                    ),
                    p(cls := "sample-footnote")(
                      "The interactive sample opens the same analysis shell used for imported games and PGN review."
                    )
                  )
                )
              ),
              st.section(id := "features", cls := "features-section landing-section")(
                div(cls := "landing-container")(
                  div(cls := "section-heading")(
                    p(cls := "section-kicker")("What is already real"),
                    h2("Four product surfaces the landing should speak about directly")
                  ),
                  div(cls := "feature-grid")(
                    st.article(cls := "feature-card")(
                      p(cls := "feature-kicker")("Surface 01"),
                      h3("Quick Move Insight"),
                      p("Bookmaker explains a single decision without forcing a full-game report."),
                      ul(
                        li("Runs only when requested for the current move"),
                        li("Can restore saved snapshots and study commentary")
                      )
                    ),
                    st.article(cls := "feature-card")(
                      p(cls := "feature-kicker")("Surface 02"),
                      h3("Full Game Review"),
                      p("Game Chronicle builds a narrative-first review shell around the whole PGN."),
                      ul(
                        li("Overview, moments, repair, patterns, moves, and reference"),
                        li("Async review path with local full-analysis fallback")
                      )
                    ),
                    st.article(cls := "feature-card")(
                      p(cls := "feature-kicker")("Surface 03"),
                      h3("Repair Windows"),
                      p("Critical games can surface where the game first broke and how to replay the patch line."),
                      ul(
                        li("Collapse interval plus earliest preventable ply"),
                        li("Patch replay compares improved and original continuations")
                      )
                    ),
                    st.article(cls := "feature-card")(
                      p(cls := "feature-kicker")("Surface 04"),
                      h3("Study Memory"),
                      p("The analysis shell is already wired for reuse instead of one-off output."),
                      ul(
                        li("PGN, FEN, Lichess, and Chess.com entry points"),
                        li("Saved imports, study sync, and reopenable analysis history")
                      )
                    )
                  )
                )
              ),
              st.section(id := "how-it-works", cls := "workflow-section landing-section")(
                div(cls := "landing-container")(
                  div(cls := "section-heading")(
                    p(cls := "section-kicker")("How it works"),
                    h2("The actual product flow is import, choose a path, then save the result")
                  ),
                  ol(cls := "workflow-grid")(
                    li(cls := "workflow-step")(
                      span(cls := "step-index")("01"),
                      h3("Import or jump"),
                      p("Paste PGN, open an imported game, or jump into a FEN from the current board state."),
                      p(cls := "step-output")("Output: clean move timeline with board context")
                    ),
                    li(cls := "workflow-step workflow-step--branch")(
                      span(cls := "step-index")("02A"),
                      h3("Explain this move"),
                      p("Use Bookmaker when one decision needs context, not a full report."),
                      p(cls := "step-output")("Output: move commentary, refs, and saved snapshot")
                    ),
                    li(cls := "workflow-step workflow-step--branch")(
                      span(cls := "step-index")("02B"),
                      h3("Run Game Chronicle"),
                      p("Use the deeper review when you need moments, repair windows, and long-term patterns."),
                      p(cls := "step-output")("Output: overview, moments, repair, patterns")
                    ),
                    li(cls := "workflow-step")(
                      span(cls := "step-index")("03"),
                      h3("Save and Revisit"),
                      p("Resume imported games, reopen saved analyses, or push commentary into study chapters."),
                      p(cls := "step-output")("Output: reusable study trail")
                    )
                  )
                )
              ),
              st.section(id := "trust", cls := "quality-section landing-section")(
                div(cls := "landing-container quality-grid")(
                  div(cls := "section-heading")(
                    p(cls := "section-kicker")("Reality check"),
                    h2("What the product really does today"),
                    p(
                      "The strongest story is already in the shipped analysis shell, so this landing now reflects real behavior instead of placeholder promise."
                    )
                  ),
                  div(cls := "quality-card")(
                    p(cls := "quality-label")("Current product surfaces"),
                    div(cls := "quality-metrics")(
                      div(cls := "metric-row")(
                        span("Move insight"),
                        strong(cls := "metric-value")("Bookmaker on demand")
                      ),
                      div(cls := "metric-row")(
                        span("Full review"),
                        strong(cls := "metric-value")("Async Game Chronicle")
                      ),
                      div(cls := "metric-row")(
                        span("Study flow"),
                        strong(cls := "metric-value")("Imports, reopen, study sync")
                      )
                    ),
                    ul(
                      li("Move commentary is interactive, with hover previews and move references."),
                      li("Full-game review keeps moments, repair, patterns, raw moves, and reference tools in one shell."),
                      li("Deep review and move commentary follow login and plan-based usage rules."),
                      li("Large PGNs can take longer because Game Chronicle runs a deeper scan before generating review text.")
                    ),
                    div(cls := "quality-pills")(
                      span("Moments"),
                      span("Repair"),
                      span("Patterns"),
                      span("Reference")
                    ),
                    p(cls := "quality-note")(
                      "Use the sample to see the real analysis shell first, then start with your own PGN or imported game."
                    ),
                    div(cls := "quality-actions")(
                      a(href := sampleAnalysisUrl, cls := "btn-secondary final-cta")("Open Sample"),
                      a(href := routes.UserAnalysis.index.url, cls := "btn-primary final-cta")("Start Analysis")
                    )
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
