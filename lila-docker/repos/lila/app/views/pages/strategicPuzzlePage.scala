package views.pages

import play.api.libs.json.Json

import lila.app.UiEnv.*
import lila.strategicPuzzle.StrategicPuzzle.BootstrapPayload
import lila.ui.{ Page, PageModule }

object strategicPuzzlePage:

  def apply(payload: BootstrapPayload): Page =
    Page("Position Exercises")
      .css("strategicPuzzle")
      .js(PageModule("strategicPuzzle", Json.toJson(payload)))
      :
        frag(
          main(cls := "sp-demo-page sp-live-page")(
            div(id := "strategic-puzzle-app")(
              div(cls := "sp-live-noscript")(
                h2("Loading position exercise"),
                p("If this stays empty, enable JavaScript to play through the position and open the review.")
              )
            )
          )
        )
