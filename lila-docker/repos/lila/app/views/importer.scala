package views

import controllers.Importer.GameCard
import lila.app.UiEnv.*
import lila.ui.Page
import scala.annotation.unused

object importer:

  def index(
      error: Option[String] = None,
      provider: String = "lichess",
      username: String = ""
  )(using @unused ctx: Context): Page =
    Page("Import Games - Chesstory")
      .css("auth")
      .wrap: _ =>
        main(cls := "auth-page")(
          div(cls := "landing-container")(
            div(cls := "landing-header")(
              a(href := routes.Main.landing.url, cls := "logo")("Chesstory")
            ),
            div(cls := "auth-container")(
              div(cls := "auth-card")(
                h1(cls := "auth-title")("Import Recent Games"),
                p(cls := "auth-subtitle")("Load recent public games from Lichess or Chess.com."),
                error.map(msg => div(cls := "auth-error")(msg)),
                form(cls := "auth-form", method := "post", action := routes.Importer.sendGame.url)(
                  div(cls := "form-group")(
                    label(`for` := "import-provider")("Provider"),
                    st.select(id := "import-provider", name := "provider")(
                      option(value := "lichess", if provider == "lichess" then selected := true else emptyFrag)("Lichess"),
                      option(value := "chesscom", if provider == "chesscom" then selected := true else emptyFrag)("Chess.com")
                    )
                  ),
                  div(cls := "form-group")(
                    label(`for` := "import-username")("Username"),
                    input(
                      id := "import-username",
                      tpe := "text",
                      name := "username",
                      value := username,
                      placeholder := "e.g. DrNykterstein or hikaru",
                      required
                    )
                  ),
                  button(cls := "auth-submit", tpe := "submit")(
                    "Fetch Recent Games",
                    span(cls := "arrow")(" ->")
                  )
                ),
                div(cls := "auth-links")(
                  a(href := routes.UserAnalysis.index.url)("Back to Analysis")
                )
              )
            )
          )
        )

  def gameList(
      provider: String,
      username: String,
      games: List[GameCard],
      notice: Option[String] = None
  )(using @unused ctx: Context): Page =
    Page("Imported Games - Chesstory")
      .css("auth")
      .wrap: _ =>
        main(cls := "auth-page")(
          div(cls := "landing-container")(
            div(cls := "landing-header")(
              a(href := routes.Main.landing.url, cls := "logo")("Chesstory")
            ),
            div(cls := "auth-container")(
              div(cls := "auth-card")(
                h1(cls := "auth-title")(s"${providerLabel(provider)}: @$username"),
                p(cls := "auth-subtitle")("Choose a game and click Analyze."),
                notice.map(msg => div(cls := "auth-error")(msg)),
                games.nonEmpty.option(
                  div(
                    games.map(renderGameCard)*
                  )
                ),
                form(cls := "auth-form", method := "post", action := routes.Importer.sendGame.url)(
                  input(tpe := "hidden", name := "provider", value := provider),
                  div(cls := "form-group")(
                    label(`for` := "import-username")("Load another username"),
                    input(
                      id := "import-username",
                      tpe := "text",
                      name := "username",
                      value := username,
                      required
                    )
                  ),
                  button(cls := "auth-submit", tpe := "submit")(
                    "Refresh List",
                    span(cls := "arrow")(" ->")
                  )
                ),
                div(cls := "auth-links")(
                  a(href := routes.Importer.importGame.url)("New import"),
                  a(href := routes.UserAnalysis.index.url)("Back to Analysis")
                )
              )
            )
          )
        )

  private def renderGameCard(game: GameCard): Frag =
    div(
      cls := "auth-message",
      style := "margin: 1rem 0; text-align: left;"
    )(
      div(style := "font-weight: 700; margin-bottom: 0.35rem;")(
        s"${game.white} vs ${game.black} (${game.result})"
      ),
      div(style := "font-size: 0.9rem; opacity: 0.85; margin-bottom: 0.5rem;")(
        s"${game.playedAt} UTC | ${game.speed} | ${game.gameId}"
      ),
      div(style := "display: flex; gap: 0.6rem; align-items: center; flex-wrap: wrap;")(
        form(method := "post", action := routes.Importer.sendGame.url, style := "margin: 0;")(
          input(tpe := "hidden", name := "pgn64", value := game.pgn64),
          button(tpe := "submit", cls := "btn-primary", style := "padding: 0.45rem 0.85rem; border-radius: 8px;")("Analyze")
        ),
        game.sourceUrl.map(url => a(href := url, target := "_blank", rel := "noopener noreferrer", cls := "btn-text")("Source"))
      )
    )

  private def providerLabel(provider: String): String =
    provider.trim.toLowerCase match
      case "chesscom" => "Chess.com Recent Games"
      case _          => "Lichess Recent Games"
