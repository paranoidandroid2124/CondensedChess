package views.pages

import lila.app.JournalContent
import lila.app.UiEnv.{ *, given }
import lila.ui.Page

object landing:

  def apply(latestJournalPost: Option[JournalContent.Post])(using ctx: Context): Page =
    val samplePgn =
      "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O Be7 6. Re1 b5 7. Bb3 d6 8. c3 O-O 9. h3 Nb8 10. d4 Nbd7 11. c4"
    val sampleAnalysisUrl = routes.UserAnalysis.pgn(samplePgn).url
    val analysisUrl = routes.UserAnalysis.index.url
    val pgnImportUrl = routes.Importer.importGame.url
    val accountIntelUrl = routes.AccountIntel.landing("", "").url
    val strategicPuzzleUrl = routes.StrategicPuzzle.home.url
    val journalUrl = routes.Main.journal.url
    val journalTitle = latestJournalPost.fold("Notes from the study room")(_.title)
    val journalSummary = latestJournalPost.fold(
      "Short notes about the positions, reviews, and study habits Chesstory is learning from."
    )(_.summary)
    val journalReadUrl = latestJournalPost.fold(journalUrl)(post => routes.Main.journalPost(post.slug).url)
    val journalMeta = latestJournalPost.fold("Study notes")(post => s"${post.publishedLabel} / ${post.readTime}")

    Page("Chesstory - Review your games with the board in view")
      .css("landing")
      .wrap: _ =>
        frag(
          div(cls := "landing-pro")(
            main(cls := "landing-main")(
              st.section(id := "top", cls := "hero-section landing-section")(
                div(cls := "landing-container hero-grid")(
                  div(cls := "hero-copy")(
                    p(cls := "eyebrow")("Chesstory"),
                    h1(
                      "One game can become a review,",
                      br,
                      span("a lesson, and a board you remember.")
                    ),
                    p(cls := "hero-summary")(
                      "Chesstory turns your games into board-led Move Reviews, recurring position studies, and quiet board work you can return to later."
                    ),
                    div(cls := "hero-support-copy")(
                      p(
                        "Bring in a game, keep the position in front of you, and move through the verdict, why, plan, line, and memory cue as one review player."
                      )
                    ),
                    div(cls := "hero-cta")(
                      a(href := sampleAnalysisUrl, cls := "btn-primary")("Open Move Review"),
                      a(href := accountIntelUrl, cls := "btn-secondary")("Study recurring positions"),
                      a(href := pgnImportUrl, cls := "btn-secondary")("Start from game text")
                    ),
                    p(cls := "hero-disclosure")(
                      "Everything below starts from real games, positions, and boards."
                    )
                  ),
                  div(
                    cls := "hero-visual",
                    attr("role") := "complementary",
                    aria.label := "One position story preview"
                  )(
                    div(cls := "hero-stage")(
                      div(cls := "timeline-card hero-board-shell")(
                        div(cls := "hero-board")(
                          span(cls := "hero-board-callout hero-board-callout--top")("Typical position"),
                          span(cls := "hero-board-callout hero-board-callout--bottom")("Move 24")
                        ),
                        div(cls := "hero-board-caption")(
                          span("Review position"),
                          strong("Ruy Lopez middlegame structure")
                        )
                      ),
                      div(cls := "hero-card-stack")(
                        st.article(cls := "hero-surface hero-artifact-card hero-artifact-card--review")(
                          p(cls := "hero-surface-kicker")("Move Review"),
                          h3(cls := "hero-surface-title")("Why this move mattered"),
                          p(cls := "hero-surface-copy")(
                            "The same position becomes one coaching scene with a clear next plan."
                          ),
                          p(cls := "hero-artifact-proof")(
                            "The board, line, reason, and memory cue move together."
                          )
                        ),
                        st.article(cls := "hero-surface hero-artifact-card hero-artifact-card--pattern")(
                          p(cls := "hero-surface-kicker")("My Patterns"),
                          h3(cls := "hero-surface-title")("Recurring queenside break"),
                          p(cls := "hero-surface-copy")(
                            "A familiar position from games you or another player already reached."
                          ),
                          p(cls := "hero-artifact-proof")(
                            "Keep the board, the plan, and the repeat pattern together."
                          )
                        ),
                        st.article(cls := "hero-surface hero-artifact-card hero-artifact-card--move")(
                          p(cls := "hero-surface-kicker")("Ask About This Move"),
                          h3(cls := "hero-surface-title")("Ask about the move in front of you"),
                          p(cls := "hero-surface-copy")(
                            "When one decision feels unclear, study that move without leaving the board."
                          ),
                          p(cls := "hero-artifact-proof")(
                            "One move, one reason, one board position."
                          )
                        ),
                        st.article(cls := "hero-surface hero-artifact-card hero-artifact-card--analysis")(
                          p(cls := "hero-surface-kicker")("Board View"),
                          h3(cls := "hero-surface-title")("Continue on the board"),
                          p(cls := "hero-surface-copy")(
                            "Open the moves and side lines when the lesson needs more board work."
                          ),
                          p(cls := "hero-artifact-proof")(
                            "Stay with the same position and keep exploring."
                          )
                        )
                      )
                    ),
                    div(cls := "hero-visual-footer")(
                      span("Start from the board, not a menu"),
                      a(href := sampleAnalysisUrl, cls := "demo-link")("Open the review")
                    )
                  )
                )
              ),
              st.section(id := "produces", cls := "landing-section produces-section")(
                div(cls := "landing-container")(
                  div(cls := "section-heading")(
                    p(cls := "section-kicker")("What you study"),
                    h2("Four ways to keep the board in view"),
                    p(
                      "Chesstory is built around the board: games you want reviewed, patterns you keep reaching, moves that need a reason, and positions worth playing through."
                    )
                  ),
                  div(cls := "product-grid")(
                    st.article(cls := "feature-card artifact-card")(
                      p(cls := "feature-kicker")("Study 01"),
                      h3("Move Review"),
                      p("Review one game through coaching scenes where the board, line, reason, and takeaway stay together."),
                      p(cls := "artifact-card__proof")("Use it when one game should feel like a short lesson with the position still visible.")
                    ),
                    st.article(cls := "feature-card artifact-card")(
                      p(cls := "feature-kicker")("Study 02"),
                      h3("My Patterns"),
                      p("See recurring decisions, typical positions, and evidence games from imported public accounts."),
                      p(cls := "artifact-card__proof")("Use it when the same position keeps appearing in your games.")
                    ),
                    st.article(cls := "feature-card artifact-card")(
                      p(cls := "feature-kicker")("Study 03"),
                      h3("Ask About This Move"),
                      p("Ask for a coach note on the current move only when the current position needs more context."),
                      p(cls := "artifact-card__proof")("Use it when one decision matters more than a whole-game review.")
                    ),
                    st.article(cls := "feature-card artifact-card")(
                      p(cls := "feature-kicker")("Study 04"),
                      h3("Position Exercise"),
                      p("Open a live position exercise that keeps the idea on the board and reveals the explanation."),
                      p(cls := "artifact-card__proof")("Use it when a theme should be played through, not only read.")
                    )
                  )
                )
              ),
              st.section(id := "story", cls := "landing-section story-section")(
                div(cls := "landing-container")(
                  div(cls := "section-heading")(
                    p(cls := "section-kicker")("One position story"),
                    h2("The same board can become a pattern, a review, or a position exercise"),
                    p(
                      "The point is not more menus. The point is that one chess problem stays recognisable while you study it from different angles."
                    )
                  ),
                  div(cls := "story-grid")(
                    div(cls := "story-stage")(
                      div(cls := "hero-surface story-stage-shell")(
                        div(cls := "timeline-card story-board-shell")(
                          div(cls := "hero-board story-board")(
                            span(cls := "hero-board-callout hero-board-callout--top")("Pattern"),
                            span(cls := "hero-board-callout hero-board-callout--bottom")("Counterplay")
                          ),
                          div(cls := "story-caption-grid")(
                            div(cls := "story-caption-item")(
                              strong("Typical position"),
                              span("Queenside break is the recurring theme.")
                            ),
                            div(cls := "story-caption-item")(
                              strong("Guided moment"),
                              span("Move 24 decides who gets the easier plan.")
                            )
                          )
                        ),
                        div(cls := "story-stage-footer")(
                          a(href := sampleAnalysisUrl, cls := "btn-secondary")("Open Move Review"),
                          a(href := strategicPuzzleUrl, cls := "btn-text")("Open position exercise")
                        )
                      )
                    ),
                    div(cls := "story-step-list")(
                      st.article(cls := "timeline-card story-card")(
                        p(cls := "story-card__label")("Pattern"),
                        h3("My Patterns keeps the recurring position in front of you"),
                        p(cls := "story-card__copy")(
                          "Start with a public account and keep one typical board, one explanation, and one next place to look."
                        ),
                        p(cls := "story-card__proof")("You keep the recurring position, the openings you actually reach, and the games behind it."),
                        a(href := accountIntelUrl, cls := "story-card__link")("Study recurring positions")
                      ),
                      st.article(cls := "timeline-card story-card")(
                        p(cls := "story-card__label")("Explanation"),
                        h3("Move Review turns that board into a coaching scene"),
                        p(cls := "story-card__copy")(
                          "The same position becomes a selected scene with why it mattered, the next plan, a playable line, and a short takeaway."
                        ),
                        p(cls := "story-card__proof")("You get one coaching scene, one reason, and a board that stays visible."),
                        a(href := sampleAnalysisUrl, cls := "story-card__link")("Open Move Review")
                      ),
                      st.article(cls := "timeline-card story-card")(
                        p(cls := "story-card__label")("Position Exercise"),
                        h3("Position Exercise lets the idea stay on the board"),
                        p(cls := "story-card__copy")(
                          "The same theme can be seen as a live position with hints, a revealed line, and a compact explanation."
                        ),
                        p(cls := "story-card__proof")("You get one board task, one revealed plan, and the next position."),
                        a(href := strategicPuzzleUrl, cls := "story-card__link")("Open position exercise")
                      ),
                      st.article(cls := "timeline-card story-card")(
                        p(cls := "story-card__label")("Board View"),
                        h3("The board stays available when you need more lines"),
                        p(cls := "story-card__copy")(
                          "When the guided explanation is not enough, continue on the same board with the moves, side lines, and current-move explanation."
                        ),
                        p(cls := "story-card__proof")("You keep the board, the line, and the direct move explanation together."),
                        a(href := analysisUrl, cls := "story-card__link")("Open board")
                      )
                    )
                  )
                )
              ),
              st.section(id := "entry", cls := "landing-section entry-section")(
                div(cls := "landing-container")(
                  div(cls := "section-heading")(
                    p(cls := "section-kicker")("Where you start"),
                    h2("Three clean ways into Chesstory"),
                    p(
                      "You can start from a public account, a pasted game, or a board. Each path stays tied to a real chess position."
                    )
                  ),
                  div(cls := "entry-grid")(
                    st.article(cls := "feature-card entry-card")(
                      p(cls := "feature-kicker")("Start 01"),
                      h3("Public account"),
                      p("Choose a Lichess or Chess.com username and start from recurring positions instead of one isolated game."),
                      p(cls := "entry-card__proof")("Best when you want My Patterns, openings you actually reach, and opponent prep."),
                      div(cls := "entry-card__chips sample-chip-row")(
                        span("Lichess"),
                        span("Chess.com"),
                        span("My Patterns")
                      ),
                      div(cls := "entry-card__actions")(
                        a(href := accountIntelUrl, cls := "btn-secondary")("Open account patterns")
                      )
                    ),
                    st.article(cls := "feature-card entry-card")(
                      p(cls := "feature-kicker")("Start 02"),
                      h3("Pasted game"),
                      p("Paste a game and go straight to Move Review, then keep the board one click away."),
                      p(cls := "entry-card__proof")("Best when you already know which game you want to understand."),
                      div(cls := "entry-card__chips sample-chip-row")(
                        span("Move Review"),
                        span("Coach scenes"),
                        span("Game text")
                      ),
                      div(cls := "entry-card__actions")(
                        a(href := sampleAnalysisUrl, cls := "btn-secondary")("Open Move Review"),
                        a(href := pgnImportUrl, cls := "btn-text")("Start from game text")
                      )
                    ),
                    st.article(cls := "feature-card entry-card")(
                      p(cls := "feature-kicker")("Start 03"),
                      h3("Current board"),
                      p("Open the board when you already have the position and want to explore lines directly."),
                      p(cls := "entry-card__proof")("Best when the board itself is the starting point."),
                      div(cls := "entry-card__chips sample-chip-row")(
                        span("Board View"),
                        span("Ask About This Move"),
                        span("Board-first")
                      ),
                      div(cls := "entry-card__actions")(
                        a(href := analysisUrl, cls := "btn-secondary")("Open board")
                      )
                    )
                  )
                )
              ),
              st.section(id := "today", cls := "landing-section truth-section")(
                div(cls := "landing-container truth-grid")(
                  div(cls := "section-heading")(
                    p(cls := "section-kicker")("What you can do today"),
                    h2("A quiet study room for the games you already play"),
                    p(
                      "Bring a game, a public account, or a position. Chesstory keeps the study focused on the board."
                    )
                  ),
                  div(cls := "quality-card truth-card")(
                    p(cls := "quality-label")("Current study paths"),
                    div(cls := "truth-table")(
                      div(cls := "truth-row")(
                        span("My Patterns"),
                        strong("Recurring positions, openings, evidence games")
                      ),
                      div(cls := "truth-row")(
                        span("Move Review"),
                        strong("Verdict, why, plan, playable line, what to remember")
                      ),
                      div(cls := "truth-row")(
                        span("Ask About This Move"),
                        strong("On-demand coach note for the current move")
                      ),
                      div(cls := "truth-row")(
                        span("Position Exercise"),
                        strong("Live position, revealed line, next exercise")
                      ),
                      div(cls := "truth-row")(
                        span("Board View"),
                        strong("Board, moves, side lines, game text")
                      ),
                      div(cls := "truth-row")(
                        span("Saved study"),
                        strong("Saved history, reopen flow, study reuse")
                      )
                    ),
                    div(cls := "truth-boundaries")(
                      p("Ask About This Move appears only when you ask."),
                      p("Move Review starts from the game you provide."),
                      p("The board stays available when you want to explore directly.")
                    ),
                    div(cls := "quality-pills")(
                      span("Board stays visible"),
                      span("Ask when needed"),
                      span("Ready to revisit")
                    ),
                    div(cls := "quality-actions truth-actions")(
                      a(href := sampleAnalysisUrl, cls := "btn-primary final-cta")("Open Move Review"),
                      a(href := analysisUrl, cls := "btn-secondary final-cta")("Open board")
                    )
                  )
                )
              ),
              st.section(cls := "landing-section journal-strip-section")(
                div(cls := "landing-container")(
                  st.article(cls := "quality-card journal-strip-card")(
                    div(cls := "journal-strip-copy")(
                      p(cls := "section-kicker")("From the journal"),
                      h2(journalTitle),
                      p(journalSummary)
                    ),
                    div(cls := "journal-strip-meta")(
                      p(cls := "journal-strip-note")(journalMeta),
                      a(href := journalReadUrl, cls := "btn-text journal-strip-link")("Read the journal")
                    )
                  )
                )
              )
            ),
            footer(cls := "landing-footer")(
              div(cls := "landing-container footer-links")(
                span("© 2026 Chesstory"),
                a(href := routes.Main.journal.url)("Journal"),
                a(href := routes.Main.support.url)("Support"),
                a(href := routes.Main.source.url)("Open Source"),
                a(href := routes.Main.privacy.url)("Privacy"),
                a(href := routes.Main.terms.url)("Terms")
              )
            )
          )
        )
