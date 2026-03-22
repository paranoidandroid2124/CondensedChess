package views.pages

import lila.app.UiEnv.*
import lila.ui.Page

object strategicPuzzleEmpty:

  def apply(): Page =
    Page("Strategic Puzzles")
      .css("strategicPuzzle")
      :
        frag(
          div(cls := "sp-demo-aura", aria.hidden := "true")(
            div(cls := "sp-aura sp-aura--1"),
            div(cls := "sp-aura sp-aura--2"),
            div(cls := "sp-aura sp-aura--3")
          ),
          main(cls := "sp-demo-page sp-live-page")(
            div(cls := "sp-empty-state")(
              p(cls := "sp-demo-kicker")("No new live puzzle yet"),
              h1("No live strategic puzzle is ready right now."),
              p(
                "You have already finished the puzzles that are currently available. Use the demo to review the format, or jump back into analysis and your notebook while the next batch is prepared."
              ),
              div(cls := "sp-empty-state__actions")(
                a(href := routes.Main.strategicPuzzleDemo.url, cls := "sp-demo-footer__link is-strong")("Open the demo"),
                a(href := routes.UserAnalysis.index.url, cls := "sp-demo-footer__link")("Back to analysis"),
                a(href := routes.Study.allDefault().url, cls := "sp-demo-footer__link")("Open notebooks")
              )
            )
          )
        )
