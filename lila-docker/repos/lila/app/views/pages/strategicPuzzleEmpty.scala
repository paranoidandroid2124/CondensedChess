package views.pages

import lila.app.UiEnv.*
import lila.ui.Page

object strategicPuzzleEmpty:

  def apply(): Page =
    Page("Strategic Puzzles")
      .css("strategicPuzzle")
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
            div(cls := "sp-empty-state")(
              p(cls := "sp-demo-kicker")("Current batch complete"),
              h1("No uncleared strategic puzzle is queued right now."),
              p(
                "You have already cleared the currently available live shells. Use the demo to review the format, or jump back into analysis and journal while the next batch is prepared."
              ),
              div(cls := "sp-empty-state__actions")(
                a(href := routes.Main.strategicPuzzleDemo.url, cls := "sp-demo-footer__link is-strong")("Open the demo"),
                a(href := routes.UserAnalysis.index.url, cls := "sp-demo-footer__link")("Back to analysis"),
                a(href := routes.Main.journal.url, cls := "sp-demo-footer__link")("Browse the journal")
              )
            )
          )
        )
