package views.pages

import lila.app.UiEnv.*
import lila.ui.Page

object strategicPuzzleEmpty:

  def apply(): Page =
    Page("Position Exercises")
      .css("strategicPuzzle")
      :
        frag(
          main(cls := "sp-demo-page sp-live-page")(
            div(cls := "sp-empty-state")(
              p(cls := "sp-demo-kicker")("No new position exercise yet"),
              h1("No live position exercise is ready right now."),
              p(
                "You have already finished the exercises that are currently available. Use the demo to review the format, or jump back to the board and your notebooks while the next batch is prepared."
              ),
              div(cls := "sp-empty-state__actions")(
                a(href := routes.Main.strategicPuzzleDemo.url, cls := "sp-demo-footer__link is-strong")("Open the demo"),
                a(href := routes.UserAnalysis.index.url, cls := "sp-demo-footer__link")("Back to board"),
                a(href := routes.Study.allDefault().url, cls := "sp-demo-footer__link")("Open notebooks")
              )
            )
          )
        )
