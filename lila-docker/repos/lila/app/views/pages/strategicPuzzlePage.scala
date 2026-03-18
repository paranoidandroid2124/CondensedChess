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
      .flag(_.noHeader)
      .flag(_.fullScreen):
        frag(
          div(cls := "sp-demo-aura", aria.hidden := "true")(
            div(cls := "sp-aura sp-aura--1"),
            div(cls := "sp-aura sp-aura--2"),
            div(cls := "sp-aura sp-aura--3")
          ),
          main(cls := "sp-demo-page sp-live-page")(
            header(cls := "sp-demo-header")(
              div(cls := "sp-demo-header__brand")(
                a(href := routes.Main.landing.url, cls := "sp-demo-logo")("Chesstory"),
                span(cls := "sp-demo-header__divider")("/"),
                span("Strategic Puzzles")
              ),
              div(cls := "sp-demo-header__actions")(
                a(href := routes.UserAnalysis.index.url, cls := "sp-demo-link")("Analysis"),
                a(href := routes.Main.strategicPuzzleDemo.url, cls := "sp-demo-link")("Demo"),
                a(href := routes.Main.journal.url, cls := "sp-demo-link")("Journal")
              )
            ),
            div(id := "strategic-puzzle-app")(
              div(cls := "sp-live-noscript")(
                h2("Loading strategic puzzle"),
                p("If this stays empty, enable JavaScript to run the precomputed puzzle shell.")
              )
            )
          )
        )
