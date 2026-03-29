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
    val journalTitle = latestJournalPost.fold("Product notes from Chesstory")(_.title)
    val journalSummary = latestJournalPost.fold(
      "Short notes about what changed, why it changed, and what Chesstory is learning while building board-linked chess analysis."
    )(_.summary)
    val journalReadUrl = latestJournalPost.fold(journalUrl)(post => routes.Main.journalPost(post.slug).url)
    val journalMeta = latestJournalPost.fold("Updates and product notes")(post => s"${post.publishedLabel} / ${post.readTime}")

    Page("Chesstory - Board-linked chess analysis")
      .css("landing")
      .wrap: _ =>
        frag(
          div(cls := "landing-orbs", aria.hidden := "true")(
            div(cls := "orb orb-1"),
            div(cls := "orb orb-2"),
            div(cls := "orb orb-3")
          ),
          div(cls := "landing-pro")(
            main(cls := "landing-main")(
              st.section(id := "top", cls := "hero-section landing-section")(
                div(cls := "landing-container hero-grid")(
                  div(cls := "hero-copy")(
                    p(cls := "eyebrow")("Chesstory"),
                    h1(
                      "One position can become a pattern,",
                      br,
                      span("an explanation, and a full review.")
                    ),
                    p(cls := "hero-summary")(
                      "Chesstory turns games into recurring patterns, board-linked explanations, strategic puzzles, and full analysis when you want the whole line."
                    ),
                    div(cls := "hero-support-copy")(
                      p(
                        "See recurring patterns from public games, read why a move mattered, ask for an explanation on demand, and continue on the board."
                      )
                    ),
                    div(cls := "hero-cta")(
                      a(href := sampleAnalysisUrl, cls := "btn-primary")("Open a sample review"),
                      a(href := accountIntelUrl, cls := "btn-secondary")("See account patterns"),
                      a(href := pgnImportUrl, cls := "btn-secondary")("Start from PGN")
                    ),
                    p(cls := "hero-disclosure")(
                      "Everything below points to pages that already exist in Chesstory today."
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
                          span("Sample position"),
                          strong("Ruy Lopez middlegame structure")
                        )
                      ),
                      div(cls := "hero-card-stack")(
                        st.article(cls := "hero-surface hero-artifact-card hero-artifact-card--pattern")(
                          p(cls := "hero-surface-kicker")("My Patterns"),
                          h3(cls := "hero-surface-title")("Recurring queenside break"),
                          p(cls := "hero-surface-copy")(
                            "A typical position pulled from imported public games."
                          ),
                          p(cls := "hero-artifact-proof")(
                            "Output: pattern, opening context, and one board to keep in view."
                          )
                        ),
                        st.article(cls := "hero-surface hero-artifact-card hero-artifact-card--review")(
                          p(cls := "hero-surface-kicker")("Guided Review"),
                          h3(cls := "hero-surface-title")("Why this move mattered"),
                          p(cls := "hero-surface-copy")(
                            "The same position becomes a selected moment with a forward-looking explanation."
                          ),
                          p(cls := "hero-artifact-proof")(
                            "Output: moment, repair clue, and what to remember."
                          )
                        ),
                        st.article(cls := "hero-surface hero-artifact-card hero-artifact-card--move")(
                          p(cls := "hero-surface-kicker")("Explain This Move"),
                          h3(cls := "hero-surface-title")("Ask only when you want it"),
                          p(cls := "hero-surface-copy")(
                            "Move commentary stays on demand instead of taking over every board change."
                          ),
                          p(cls := "hero-artifact-proof")(
                            "Output: one move, one reason, one board-linked explanation."
                          )
                        ),
                        st.article(cls := "hero-surface hero-artifact-card hero-artifact-card--analysis")(
                          p(cls := "hero-surface-kicker")("Full Analysis"),
                          h3(cls := "hero-surface-title")("Continue on the board"),
                          p(cls := "hero-surface-copy")(
                            "Open the tree, engine, explorer, and references when you want the whole line."
                          ),
                          p(cls := "hero-artifact-proof")(
                            "Output: the same position in a direct analysis shell."
                          )
                        )
                      )
                    ),
                    div(cls := "hero-visual-footer")(
                      span("Real pages, not concept screens"),
                      a(href := sampleAnalysisUrl, cls := "demo-link")("Open the sample")
                    )
                  )
                )
              ),
              st.section(id := "produces", cls := "landing-section produces-section")(
                div(cls := "landing-container")(
                  div(cls := "section-heading")(
                    p(cls := "section-kicker")("What Chesstory produces"),
                    h2("Four artifacts that already exist in the product"),
                    p(
                      "Chesstory does not promise a coaching persona. It gives you recurring positions, move explanations, strategic puzzles, and a full analysis shell."
                    )
                  ),
                  div(cls := "product-grid")(
                    st.article(cls := "feature-card artifact-card")(
                      p(cls := "feature-kicker")("Artifact 01"),
                      h3("My Patterns"),
                      p("See recurring decisions, typical positions, and evidence games from imported public accounts."),
                      p(cls := "artifact-card__proof")("Use it when you want one repeated problem to stay in view.")
                    ),
                    st.article(cls := "feature-card artifact-card")(
                      p(cls := "feature-kicker")("Artifact 02"),
                      h3("Guided Review"),
                      p("Review one game through selected moments, repair clues, and a concise takeaway."),
                      p(cls := "artifact-card__proof")("Use it when you want the game explained before opening the full tree.")
                    ),
                    st.article(cls := "feature-card artifact-card")(
                      p(cls := "feature-kicker")("Artifact 03"),
                      h3("Explain This Move"),
                      p("Ask for commentary on the current move only when the current position needs more context."),
                      p(cls := "artifact-card__proof")("Use it when one decision matters more than a whole-game report.")
                    ),
                    st.article(cls := "feature-card artifact-card")(
                      p(cls := "feature-kicker")("Artifact 04"),
                      h3("Strategic Puzzle"),
                      p("Open a live position-shaped puzzle that keeps the idea on the board and reveals the explanation."),
                      p(cls := "artifact-card__proof")("Use it when the same idea should be seen as a puzzle, not just a note.")
                    )
                  )
                )
              ),
              st.section(id := "story", cls := "landing-section story-section")(
                div(cls := "landing-container")(
                  div(cls := "section-heading")(
                    p(cls := "section-kicker")("One position story"),
                    h2("The same board can move through four Chesstory surfaces"),
                    p(
                      "The point is not a larger feature list. The point is that one chess problem can stay recognisable while the surface changes."
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
                          a(href := sampleAnalysisUrl, cls := "btn-secondary")("Open the sample review"),
                          a(href := strategicPuzzleUrl, cls := "btn-text")("Open Strategic Puzzle")
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
                        p(cls := "story-card__proof")("Output: recurring pattern, openings you actually reach, and evidence games."),
                        a(href := accountIntelUrl, cls := "story-card__link")("See account patterns")
                      ),
                      st.article(cls := "timeline-card story-card")(
                        p(cls := "story-card__label")("Explanation"),
                        h3("Guided Review turns that board into a moment that can be read"),
                        p(cls := "story-card__copy")(
                          "The same position becomes a selected moment with why it mattered, the best chance to fix it, and a short takeaway."
                        ),
                        p(cls := "story-card__proof")("Output: one moment, one reason, and a board-first review shell."),
                        a(href := sampleAnalysisUrl, cls := "story-card__link")("Open a sample review")
                      ),
                      st.article(cls := "timeline-card story-card")(
                        p(cls := "story-card__label")("Strategic Puzzle"),
                        h3("Strategic Puzzle lets the idea stay on the board"),
                        p(cls := "story-card__copy")(
                          "The same theme can be seen as a live position with hints, a revealed line, and a compact explanation."
                        ),
                        p(cls := "story-card__proof")("Output: one board task, one revealed plan, and the next puzzle."),
                        a(href := strategicPuzzleUrl, cls := "story-card__link")("Open Strategic Puzzle")
                      ),
                      st.article(cls := "timeline-card story-card")(
                        p(cls := "story-card__label")("Full Analysis"),
                        h3("Full Analysis keeps the board and tree available when you need them"),
                        p(cls := "story-card__copy")(
                          "When the guided explanation is not enough, continue on the same board with the move list, engine, explorer, and current-node explanation."
                        ),
                        p(cls := "story-card__proof")("Output: board, tree, current node, and direct move explanation."),
                        a(href := analysisUrl, cls := "story-card__link")("Open full analysis")
                      )
                    )
                  )
                )
              ),
              st.section(id := "entry", cls := "landing-section entry-section")(
                div(cls := "landing-container")(
                  div(cls := "section-heading")(
                    p(cls := "section-kicker")("Where you enter"),
                    h2("Three clean ways into Chesstory"),
                    p(
                      "You can start from a public account, a PGN, or the board and tree directly. Each entry keeps the output tied to a real chess artifact."
                    )
                  ),
                  div(cls := "entry-grid")(
                    st.article(cls := "feature-card entry-card")(
                      p(cls := "feature-kicker")("Entry 01"),
                      h3("Public account"),
                      p("Import a Lichess or Chess.com username and start from recurring positions instead of one isolated game."),
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
                      p(cls := "feature-kicker")("Entry 02"),
                      h3("PGN"),
                      p("Paste a game and go straight to Guided Review, then keep Full Analysis one click away."),
                      p(cls := "entry-card__proof")("Best when you already know which game you want to understand."),
                      div(cls := "entry-card__chips sample-chip-row")(
                        span("Guided Review"),
                        span("Sample review"),
                        span("PGN import")
                      ),
                      div(cls := "entry-card__actions")(
                        a(href := sampleAnalysisUrl, cls := "btn-secondary")("Open a sample review"),
                        a(href := pgnImportUrl, cls := "btn-text")("Start from PGN")
                      )
                    ),
                    st.article(cls := "feature-card entry-card")(
                      p(cls := "feature-kicker")("Entry 03"),
                      h3("Current board"),
                      p("Open Full Analysis when you want the board, tree, engine, explorer, and current-node explanation directly."),
                      p(cls := "entry-card__proof")("Best when you already have the position and want to stay inside direct analysis."),
                      div(cls := "entry-card__chips sample-chip-row")(
                        span("Full Analysis"),
                        span("Explain This Move"),
                        span("Board-first")
                      ),
                      div(cls := "entry-card__actions")(
                        a(href := analysisUrl, cls := "btn-secondary")("Open full analysis")
                      )
                    )
                  )
                )
              ),
              st.section(id := "today", cls := "landing-section truth-section")(
                div(cls := "landing-container truth-grid")(
                  div(cls := "section-heading")(
                    p(cls := "section-kicker")("What Chesstory does today"),
                    h2("An honest front door for the system that already exists"),
                    p(
                      "The strongest way to introduce Chesstory is to state the real surfaces clearly, then show where each one begins and where it stops."
                    )
                  ),
                  div(cls := "quality-card truth-card")(
                    p(cls := "quality-label")("Current system"),
                    div(cls := "truth-table")(
                      div(cls := "truth-row")(
                        span("My Patterns"),
                        strong("Recurring positions, openings, evidence games")
                      ),
                      div(cls := "truth-row")(
                        span("Guided Review"),
                        strong("Selected moments, repair clue, what to remember")
                      ),
                      div(cls := "truth-row")(
                        span("Explain This Move"),
                        strong("On-demand commentary for the current move")
                      ),
                      div(cls := "truth-row")(
                        span("Strategic Puzzle"),
                        strong("Live position, revealed line, next puzzle")
                      ),
                      div(cls := "truth-row")(
                        span("Full Analysis"),
                        strong("Board, tree, engine, explorer, import")
                      ),
                      div(cls := "truth-row")(
                        span("Study notebook"),
                        strong("Saved history, reopen flow, study reuse")
                      )
                    ),
                    div(cls := "truth-boundaries")(
                      p("Explain This Move is on demand."),
                      p("Guided Review starts from the game you provide."),
                      p("Full Analysis stays available when you want the board and tree directly.")
                    ),
                    div(cls := "quality-pills")(
                      span("Board-linked"),
                      span("On demand"),
                      span("Study-ready")
                    ),
                    div(cls := "quality-actions truth-actions")(
                      a(href := sampleAnalysisUrl, cls := "btn-primary final-cta")("Open a sample review"),
                      a(href := analysisUrl, cls := "btn-secondary final-cta")("Open full analysis")
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
