package views.pages

import lila.app.UiEnv.{ *, given }
import lila.ui.Page

object strategicPuzzleDemo:

  private case class PieceVm(color: String, role: String, label: String)
  private case class SquareVm(
      key: String,
      file: Char,
      rank: Int,
      light: Boolean,
      piece: Option[PieceVm],
      marker: Option[String]
  )

  private val sampleFen = "r1b1kb1r/1p1n1pp1/p2p1q1p/P1pPp3/N1B1P3/5N1P/1PPQ1PP1/R3K2R w KQkq - 4 14"
  private val samplePgn =
    "1. e4 e5 2. Bc4 Nf6 3. d3 c6 4. Nf3 d5 5. exd5 cxd5 6. Bb3 Nc6 7. O-O Be7 8. Re1 Qd6 9. Na4 h6 10. h3 g5 11. c4"

  private val markers = Map(
    "d3" -> "start",
    "g1" -> "start",
    "b4" -> "start",
    "c6" -> "target",
    "e7" -> "reply",
    "d4" -> "pressure"
  )

  private def pieceVm(char: Char): PieceVm =
    val color = if char.isUpper then "white" else "black"
    val role = char.toLower match
      case 'k' => "king"
      case 'q' => "queen"
      case 'r' => "rook"
      case 'b' => "bishop"
      case 'n' => "knight"
      case _   => "pawn"
    PieceVm(color, role, if role == "knight" then "N" else role.head.toUpper.toString)

  private def boardSquares(fen: String): List[SquareVm] =
    val board = fen.takeWhile(_ != ' ').split('/').toList
    board.zipWithIndex.flatMap: (rankChunk, rankIndex) =>
      val rank = 8 - rankIndex
      val cells = scala.collection.mutable.ListBuffer.empty[SquareVm]
      var fileIndex = 0
      rankChunk.foreach:
        case digit if digit.isDigit =>
          (0 until digit.asDigit).foreach: _ =>
            val file = ('a' + fileIndex).toChar
            cells += SquareVm(
              key = s"$file$rank",
              file = file,
              rank = rank,
              light = (fileIndex + rank) % 2 == 0,
              piece = None,
              marker = markers.get(s"$file$rank")
            )
            fileIndex += 1
        case piece =>
          val file = ('a' + fileIndex).toChar
          cells += SquareVm(
            key = s"$file$rank",
            file = file,
            rank = rank,
            light = (fileIndex + rank) % 2 == 0,
            piece = Some(pieceVm(piece)),
            marker = markers.get(s"$file$rank")
          )
          fileIndex += 1
      cells.toList

  private def boardView =
    val squares = boardSquares(sampleFen)
    div(cls := "sp-demo-board-card")(
      div(cls := "sp-demo-board-head")(
        div(
          span(cls := "sp-demo-eyebrow")("Strategic Puzzle"),
          h2("Find the plan, then prove it.")
        ),
        div(cls := "sp-demo-board-badges")(
          span(cls := "sp-chip sp-chip--turn")("White to move"),
          span(cls := "sp-chip sp-chip--theme")("Space gain / restriction"),
          span(cls := "sp-chip sp-chip--echo")("Fixed reply demo")
        )
      ),
      div(cls := "sp-demo-board-frame")(
        div(cls := "sp-demo-board-shell")(
          div(cls := "sp-demo-board-grid", aria.label := "Strategic puzzle board")(
            squares.map: square =>
              div(
                cls := List(
                  "sp-square" -> true,
                  "is-light" -> square.light,
                  "is-dark" -> !square.light,
                  "is-start" -> square.marker.contains("start"),
                  "is-target" -> square.marker.contains("target"),
                  "is-reply" -> square.marker.contains("reply"),
                  "is-pressure" -> square.marker.contains("pressure")
                ),
                attr("data-key") := square.key,
                attr("data-file") := square.file.toString,
                attr("data-rank") := square.rank.toString
              )(
                square.piece.map: piece =>
                  span(
                    cls := s"sp-piece ${piece.color} ${piece.role}",
                    aria.label := s"${piece.color} ${piece.role}"
                  )(piece.label),
                (square.rank == 1).option(span(cls := "sp-file-label")(square.file.toString)),
                (square.file == 'a').option(span(cls := "sp-rank-label")(square.rank.toString))
              )
          ),
          div(cls := "sp-demo-board-callouts")(
            div(cls := "sp-callout")(
              strong("Hidden target"),
              span("c6 is the square all accepted roots keep under pressure.")
            ),
            div(cls := "sp-callout")(
              strong("Fixed reply"),
              span("After a correct start, Black answers with ...Be7 in this route.")
            )
          )
        ),
        div(cls := "sp-demo-board-legend")(
          div(cls := "sp-legend-row")(
            span(cls := "sp-dot is-start"),
            strong("Accepted starts"),
            span("Bd3, O-O, b4")
          ),
          div(cls := "sp-legend-row")(
            span(cls := "sp-dot is-target"),
            strong("Anchor"),
            span("c6 restraint")
          ),
          div(cls := "sp-legend-row")(
            span(cls := "sp-dot is-reply"),
            strong("Opponent"),
            span("best reply is fixed and auto-played")
          ),
          div(cls := "sp-legend-row")(
            span(cls := "sp-dot is-pressure"),
            strong("Pressure square"),
            span("d4 is where the position starts to open")
          )
        )
      )
    )

  def apply: Page =
    val analysisUrl = routes.UserAnalysis.pgn(samplePgn).url

    Page("Strategic Puzzle Demo - Chesstory")
      .css("strategicPuzzle")
      .wrap: _ =>
        frag(
          div(cls := "sp-demo-aura", aria.hidden := "true")(
            div(cls := "sp-aura sp-aura--1"),
            div(cls := "sp-aura sp-aura--2"),
            div(cls := "sp-aura sp-aura--3")
          ),
          main(cls := "sp-demo-page")(
            st.section(cls := "sp-demo-hero")(
              div(cls := "sp-demo-hero__copy")(
                p(cls := "sp-demo-kicker")("Plan-first puzzle"),
                h1(
                  "Build a strategy puzzle page that feels",
                  br,
                  span("closer to a rehearsal than a quiz.")
                ),
                p(cls := "sp-demo-intro")(
                  "The move is still concrete, but the page is organized around plan recognition, fixed opponent replies, and end-of-line review instead of a one-shot best-move reveal."
                ),
                div(cls := "sp-demo-metric-row")(
                  div(cls := "sp-metric-card")(
                    strong("10"),
                    span("sample puzzles probed")
                  ),
                  div(cls := "sp-metric-card")(
                    strong("4.1"),
                    span("terminal branches per puzzle")
                  ),
                  div(cls := "sp-metric-card")(
                    strong("2.1"),
                    span("accepted root moves on average")
                  ),
                  div(cls := "sp-metric-card")(
                    strong("end-only"),
                    span("Review opens at the end, not every move")
                  )
                )
              ),
              div(cls := "sp-demo-hero__rail")(
                div(cls := "sp-rail-card")(
                  p(cls := "sp-rail-label")("Different from tactics"),
                  ul(
                    li("The page asks for a plan to be sustained, not a single forcing shot."),
                    li("Opponent replies are fixed inside the route to keep training load focused."),
                    li("Analysis is still visible through line rails, plan labels, and underboard review cards.")
                  )
                ),
                div(cls := "sp-rail-card sp-rail-card--soft")(
                  p(cls := "sp-rail-label")("Page split"),
                  ul(
                    li("Solve surface above the fold."),
                    li("Line rail and reveal cards under the board."),
                    li("Dashboard, themes, and review stay on separate pages.")
                  )
                )
              )
            ),
            st.section(cls := "sp-demo-shell")(
              boardView,
              div(cls := "sp-demo-side")(
                st.section(cls := "sp-demo-panel sp-demo-panel--solve")(
                  p(cls := "sp-demo-panel__label")("Solve state"),
                  div(cls := "sp-stepper")(
                    span(cls := "is-live")("1. Read the imbalance"),
                    span(cls := "is-live")("2. Choose the start"),
                    span("3. Handle the fixed reply")
                  ),
                  h3("White to move. Advance the long plan without releasing the center too early."),
                  p(cls := "sp-demo-panel__copy")(
                    "The page prompt stays short. Opening name, eval, and best line stay hidden until reveal so the user solves the position as a planning task."
                  ),
                  div(cls := "sp-choice-grid sp-choice-grid--hidden")(
                    div(cls := "sp-choice-grid__notice")(
                      strong("Move list stays hidden"),
                      span("The live puzzle keeps starts off the page. You drag on the board first, then the review underneath explains which starts converge.")
                    )
                  ),
                  div(cls := "sp-runtime-actions")(
                    a(href := routes.StrategicPuzzle.home.url, cls := "sp-demo-link")("Try a live puzzle"),
                    a(href := analysisUrl, cls := "sp-demo-link")("Review the sample in analysis")
                  ),
                  div(cls := "sp-feedback-strip")(
                    div(
                      strong("Expected behavior"),
                      span("Incorrect first moves get a short retry message. Correct roots unlock the fixed opponent reply automatically.")
                    )
                  ),
                  div(cls := "sp-hidden-list")(
                    p("Hidden until reveal"),
                    ul(
                      li("Pattern label"),
                      li("Other accepted starts"),
                      li("Engine comparison against the fastest line"),
                      li("Review commentary")
                    )
                  )
                ),
                st.section(cls := "sp-demo-panel sp-demo-panel--reveal")(
                  p(cls := "sp-demo-panel__label")("Review"),
                  h3("After 7. O-O ...Be7 8. b4"),
                  p(cls := "sp-demo-panel__copy")(
                    "Once the player reaches the end of the line, the page stops talking in move-sized fragments and explains the sequence as one coherent strategic action."
                  ),
                  div(cls := "sp-summary-card")(
                    p(cls := "sp-summary-card__eyebrow")("Why this line works"),
                    h4("Castle first, then expand while the reply is tied down."),
                    p(
                      "O-O keeps White's structure intact long enough for b4 to gain queenside space without giving Black an immediate central break. The point is not speed alone but preserving the option to hit c6 under improved coordination."
                    )
                  ),
                  div(cls := "sp-mini-facts")(
                    div(
                      strong("Pattern"),
                      span("space_gain_or_restriction | c6")
                    ),
                    div(
                      strong("Fixed reply"),
                      span("...Be7")
                    ),
                    div(
                      strong("Other starts"),
                      span("Bd3 and b4 converge toward the same plan")
                    )
                  )
                )
              )
            ),
            st.section(cls := "sp-demo-lines")(
              div(cls := "sp-demo-section-head")(
                p(cls := "sp-demo-kicker")("Underboard review"),
                h2("Keep the tree visible without turning the page into an engine dump."),
                p("Each lane is one playable route: accepted root, fixed opponent reply, then the review card that explains the result.")
              ),
              div(cls := "sp-line-grid")(
                st.article(cls := "sp-line-card")(
                  p(cls := "sp-line-card__label")("Lane A"),
                  h3("Bd3 -> ...Be7 -> b4"),
                  p("The cleanest way to preserve both kingside safety and queenside space gain before committing the pawn break.")
                ),
                st.article(cls := "sp-line-card")(
                  p(cls := "sp-line-card__label")("Lane B"),
                  h3("O-O -> ...Be7 -> b4"),
                  p("A more patient start that keeps the same pattern while reducing tactical noise.")
                ),
                st.article(cls := "sp-line-card")(
                  p(cls := "sp-line-card__label")("Lane C"),
                  h3("b4 -> ...Ng6 -> Bd3"),
                  p("The space-first branch. Still valid, but the continuation has to repair coordination afterward.")
                )
              )
            ),
            st.section(cls := "sp-demo-analysis")(
              div(cls := "sp-demo-section-head")(
                p(cls := "sp-demo-kicker")("Analysis stays on the page"),
                h2("The board remains analytical, just not noisy."),
                p("Strategic puzzles should still show structure, comparison, and opponent resources. They just reveal them in a tighter order.")
              ),
              div(cls := "sp-analysis-grid")(
                st.article(cls := "sp-analysis-card")(
                  p(cls := "sp-analysis-card__label")("Root lens"),
                  h3("What changed after the first move"),
                  ul(
                    li("Board keeps the anchor square visible."),
                    li("Opponent best reply is shown as a forced checkpoint."),
                    li("The engine view becomes comparison, not guidance.")
                  )
                ),
                st.article(cls := "sp-analysis-card")(
                  p(cls := "sp-analysis-card__label")("Terminal lens"),
                  h3("Where LLM commentary belongs"),
                  ul(
                    li("One terminal card per reached line."),
                    li("Optional overall summary only when multiple accepted routes matter."),
                    li("No prose attached to obvious first-move failures.")
                  )
                ),
                st.article(cls := "sp-analysis-card")(
                  p(cls := "sp-analysis-card__label")("Review lens"),
                  h3("What moves to other pages"),
                  ul(
                    li("Theme drilling"),
                    li("Mistake history"),
                    li("From-my-games generation"),
                    li("Daily and streak modes")
                  )
                )
              )
            ),
            footer(cls := "sp-demo-footer")(
              a(href := routes.Main.landing.url, cls := "sp-demo-footer__link")("Back to home"),
              a(href := routes.StrategicPuzzle.home.url, cls := "sp-demo-footer__link")("Open a live puzzle"),
              a(href := analysisUrl, cls := "sp-demo-footer__link is-strong")("Open the sample in analysis")
            )
          )
        )
