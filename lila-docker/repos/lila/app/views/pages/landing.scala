package views.pages

import lila.app.UiEnv.{ *, given }
import lila.ui.Page

object landing:

  def apply()(using ctx: Context): Page =
    Page("Chesstory")
      .css("landing")
      .wrap: _ =>
        main(cls := "landing-pro")(
          div(cls := "landing-container")(
            div(cls := "landing-header")(
              div(cls := "logo")("Chesstory"),
              div(cls := "auth-nav")(
                ctx.me match
                  case Some(me) =>
                    frag(
                      a(href := routes.UserAnalysis.index.url, cls := "btn-text")("Dashboard"),
                      a(href := routes.User.show(me.username).url, cls := "btn-text")(me.username.value),
                      postForm(action := routes.Auth.logout, cls := "logout-form")(
                        button(cls := "btn-text")("Log out")
                      )
                    )
                  case None =>
                    a(href := routes.Auth.login.url, cls := "btn-text")("Log in")
              )
            ),
            div(cls := "hero-section")(
              div(cls := "content")(
                h1("Deep Understanding, Simplified.")(span(cls := "highlight")(".")),
                p("Experience chess analysis reimagined as an interactive narrative. No distractions, just pure insight."),
                a(href := routes.UserAnalysis.index.url, cls := "btn-pro-primary")(
                  "Start Analysis",
                  span(cls := "arrow")("→")
                )
              ),
              div(cls := "visual-abstract")(
                div(cls := "board-abstract")
              )
            ),
            div(cls := "landing-footer")(
              div(cls := "footer-links")(
                span("© 2026 Chesstory"),
                a(href := routes.Main.privacy.url, cls := "btn-text")("Privacy"),
                a(href := routes.Main.terms.url, cls := "btn-text")("Terms")
              )
            )
          )
        )
