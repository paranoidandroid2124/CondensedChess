package views.pages

import play.api.libs.json.Json

import lila.app.UiEnv.*
import lila.strategicPuzzle.StrategicPuzzle.BootstrapPayload
import lila.ui.{ Page, PageModule }

object strategicPuzzlePage:

  def apply(payload: BootstrapPayload): Page =
    Page("Strategic Puzzles")
      .css("strategicPuzzle")
      .js(PageModule("strategicPuzzle", Json.toJson(payload)))
      :
        frag(
          div(cls := "sp-demo-aura", aria.hidden := "true")(
            div(cls := "sp-aura sp-aura--1"),
            div(cls := "sp-aura sp-aura--2"),
            div(cls := "sp-aura sp-aura--3")
          ),
          main(cls := "sp-demo-page sp-live-page")(
            div(id := "strategic-puzzle-app")(
              div(cls := "sp-live-noscript")(
                h2("Loading strategic puzzle"),
                p("If this stays empty, enable JavaScript to solve the live puzzle and open the review.")
              )
            )
          )
        )
