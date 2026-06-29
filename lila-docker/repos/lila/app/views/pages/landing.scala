package views.pages

import lila.app.UiEnv.*
import lila.ui.Page
import scala.annotation.unused

object landing:

  private val heroPieces = List(
    "wK" -> "g1",
    "wQ" -> "d1",
    "wR" -> "a1",
    "wB" -> "c4",
    "wN" -> "f3",
    "wP" -> "e4",
    "wP" -> "d3",
    "wP" -> "h3",
    "bK" -> "g8",
    "bQ" -> "d8",
    "bR" -> "a8",
    "bB" -> "c5",
    "bN" -> "f6",
    "bP" -> "e5",
    "bP" -> "d6"
  )

  def apply()(using @unused ctx: Context): Page =
    Page("Chesstory - Chess analysis that explains the plan")
      .css("landing")
      .flag(_.noHeader)
      .wrap: _ =>
        main(cls := "landing-pro")(
          landingHeader,
          hero,
          story,
          proof,
          studyPayoff,
          landingFooter
        )

  private def landingHeader(using Context) =
    header(cls := "landing-header")(
      div(cls := "landing-container landing-header__inner")(
        a(href := routes.Main.landing.url, cls := "logo")("Chesstory"),
        st.nav(cls := "section-nav", aria.label := "Landing sections")(
          a(href := "#story", cls := "btn-text")("Story"),
          a(href := "#proof", cls := "btn-text")("Proof"),
          a(href := "#study", cls := "btn-text")("Study")
        ),
        div(cls := "auth-nav")(
          a(href := routes.Auth.login.url, cls := "btn-text")("Log in"),
          a(href := routes.UserAnalysis.index.url, cls := "btn-secondary landing-header-cta")("Analyze a game")
        )
      )
    )

  private def hero(using Context) =
    st.section(cls := "landing-hero")(
      div(cls := "landing-container landing-hero__grid")(
        div(cls := "landing-hero__copy")(
          p(cls := "eyebrow")("For players who want the plan, not only the number"),
          h1("Chess analysis that explains what the position is asking"),
          p(cls := "hero-summary")(
            "Paste a PGN or import a public game. Chesstory keeps Stockfish as evidence, " +
              "then turns the key position into human strategic explanation and a shareable Review Study."
          ),
          div(cls := "hero-cta")(
            a(href := routes.UserAnalysis.index.url, cls := "btn-primary")("Analyze a game"),
            a(href := routes.Importer.importGame.url, cls := "btn-secondary")("Import public game")
          ),
          p(cls := "hero-disclosure")("No playing lobby. No guesswork. One game, explained deeply.")
        ),
        div(cls := "landing-hero__visual")(
          div(cls := "motion-demo")(
            div(cls := "motion-demo__pgn")(
              span(cls := "motion-demo__label")("PGN"),
              code("1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. O-O Nf6 5. d3 O-O 6. h3")
            ),
            board("motion-demo__board"),
            div(cls := "motion-demo__engine")(
              span(cls := "motion-demo__label")("Stockfish"),
              strong("+0.31 depth 22"),
              engineLine("c3", "+0.31", true),
              engineLine("Re1", "+0.24", false),
              engineLine("Nc3", "+0.16", false)
            ),
            div(cls := "motion-demo__explain")(
              span(cls := "motion-demo__label")("Chesstory"),
              strong("The question is whether White can prepare d4."),
              p("h3 prevents a pin, but c3 connects development to the central break White is building toward.")
            )
          )
        )
      )
    )

  private def story(using Context) =
    st.section(id := "story", cls := "landing-story")(
      div(cls := "landing-container")(
        div(cls := "section-heading section-heading--center")(
          p(cls := "section-kicker")("From moves to meaning"),
          h2("Watch a game turn into an explanation you can study"),
          p(
            "Raw game text becomes a board you can inspect. Stockfish shows the evidence. " +
              "Chesstory turns the key position into a plan, then keeps it as a Review Study."
          )
        )
      ),
      div(cls := "landing-cinema")(
        div(cls := "landing-container landing-cinema__grid")(
          div(cls := "cinema-stage", attr("aria-hidden") := "true")(
            div(cls := "cinema-stage__shell")(
              div(cls := "cinema-layer cinema-layer--pgn")(
                span(cls := "cinema-kicker")("Raw game"),
                code("[White \"ych24\"] [Black \"RojoCapo\"] 1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. O-O Nf6 5. d3 O-O 6. h3")
              ),
              board("cinema-layer cinema-layer--board"),
              div(cls := "cinema-layer cinema-layer--engine")(
                span(cls := "cinema-kicker")("Engine evidence"),
                strong("+0.31"),
                engineLine("c3", "+0.31", true),
                engineLine("Re1", "+0.24", false),
                engineLine("Nc3", "+0.16", false)
              ),
              div(cls := "cinema-layer cinema-layer--brief")(
                span(cls := "cinema-kicker")("Human explanation"),
                explanationRow("Position question", "Can White prepare d4 without giving Black easy counterplay?"),
                explanationRow("Current move effect", "h3 prevents Bg4, but it does not help the c-pawn support the center."),
                explanationRow("Human idea", "When the opening plan is d4, prefer moves that make that break stronger.")
              ),
              div(cls := "cinema-layer cinema-layer--study")(
                span(cls := "cinema-kicker")("Review Study"),
                strong("ych24 vs RojoCapo review"),
                p("Opening to middlegame transition"),
                span(cls := "study-link-pill")("/study/7kP2aQb/2rNf9xL")
              )
            )
          ),
          ol(cls := "cinema-steps")(
            storyStep(
              "01",
              "Paste or import a game",
              "Start from a PGN, a Lichess game, or a Chess.com public game. The story begins with the real moves.",
              "Raw input enters the analysis board."
            ),
            storyStep(
              "02",
              "Let the position appear",
              "The board, move list, and current position become the shared reference point for every explanation.",
              "The game is now something you can inspect."
            ),
            storyStep(
              "03",
              "Keep the engine as evidence",
              "Stockfish still matters. MultiPV and eval show what the position allows, but they do not explain the plan alone.",
              "Numbers stay visible, but they stop being the whole answer."
            ),
            storyStep(
              "04",
              "Recover the strategic logic",
              "Chesstory separates the move effect, best-move difference, human idea, and proof line.",
              "The position starts to speak in chess language."
            ),
            storyStep(
              "05",
              "Save the explanation",
              "Turn a serious review into a Study with sections, playable lines, notes, and a shareable link.",
              "A game becomes a review artifact."
            )
          )
        )
      )
    )

  private def proof =
    st.section(id := "proof", cls := "landing-section landing-proof")(
      div(cls := "landing-container")(
        div(cls := "section-heading")(
          p(cls := "section-kicker")("Engine says. Chesstory explains."),
          h2("The value is not hiding Stockfish. It is explaining what the number means."),
          p(
            "A centipawn score is useful evidence. Chesstory earns trust by tying that evidence back to the board, " +
              "candidate moves, and the strategic idea a player can remember next time."
          )
        ),
        div(cls := "proof-grid")(
          proofCard(
            "Engine verdict",
            "+0.31, depth 22, best line: c3 d6 d4",
            "Objective evidence, fast and precise."
          ),
          proofCard(
            "Chesstory explanation",
            "The point is not that h3 is bad. It is that c3 prepares the central break White's setup is asking for.",
            "A reason the player can apply again."
          ),
          proofCard(
            "Review note",
            "Before choosing a quiet developing move, ask whether it also improves the pawn break or the target square.",
            "A reusable lesson attached to the position."
          )
        )
      )
    )

  private def studyPayoff(using Context) =
    st.section(id := "study", cls := "landing-section landing-study-payoff")(
      div(cls := "landing-container landing-study-payoff__grid")(
        div(cls := "study-payoff-copy")(
          p(cls := "section-kicker")("The final artifact"),
          h2("A Review Study is the page you can return to and share"),
          p(
            "Keep the board, candidate lines, and explanation together under a clear title, " +
              "then reopen it later or share it with someone who wants to see the reasoning."
          ),
          div(cls := "quality-actions")(
            a(href := routes.UserAnalysis.index.url, cls := "btn-primary")("Analyze a game"),
            a(href := routes.Importer.importGame.url, cls := "btn-secondary")("Import public game")
          )
        ),
        div(cls := "study-artifact")(
          div(cls := "study-artifact__cover")(
            span("Review Study"),
            strong("Opening to middlegame transition"),
            p("ych24 vs RojoCapo")
          ),
          div(cls := "study-artifact__body")(
            explanationRow("Position question", "Can White prepare d4 without losing time?"),
            explanationRow("Best vs played", "c3 supports the break; h3 answers a pin that was not urgent yet."),
            explanationRow("Lesson", "Before making a useful move, ask whether it advances the plan the structure wants.")
          )
        )
      )
    )

  private def landingFooter(using Context) =
    footer(cls := "landing-footer")(
      div(cls := "landing-container footer-links")(
        span("2026 Chesstory"),
        span(
          a(href := routes.Main.privacy.url)("Privacy"),
          " / ",
          a(href := routes.Main.terms.url)("Terms"),
          " / ",
          a(href := routes.Main.contact.url)("Contact")
        )
      )
    )

  private def board(extraCls: String): Frag =
    div(cls := s"landing-board $extraCls")(
      div(cls := "landing-board__grid"),
      heroPieces.map { case (piece, square) =>
        span(cls := s"landing-piece landing-piece--$piece landing-piece--$square")()
      }*
    )

  private def engineLine(move: String, eval: String, active: Boolean): Frag =
    div(cls := s"engine-line${if active then " is-active" else ""}")(
      span(move),
      strong(eval)
    )

  private def explanationRow(label: String, body: String): Frag =
    div(cls := "explain-row")(
      span(label),
      p(body)
    )

  private def storyStep(index: String, title: String, copy: String, output: String): Frag =
    li(cls := "cinema-step")(
      span(cls := "step-index")(index),
      h3(title),
      p(copy),
      div(cls := "step-output")(output)
    )

  private def proofCard(title: String, body: String, note: String): Frag =
    div(cls := "proof-card")(
      span(cls := "proof-card__label")(title),
      p(cls := "proof-card__body")(body),
      strong(cls := "proof-card__note")(note)
    )
