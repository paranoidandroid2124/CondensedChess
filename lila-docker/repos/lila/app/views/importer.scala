package views

import controllers.Importer.GameCard
import lila.analyse.ImportHistory
import lila.app.UiEnv.*
import lila.ui.Page

object importer:

  def index(
      error: Option[String] = None,
      provider: String = "lichess",
      username: String = "",
      recentAccounts: List[ImportHistory.Account] = Nil,
      recentAnalyses: List[ImportHistory.Analysis] = Nil
  )(using ctx: Context): Page =
    val pageError = error.orElse(ctx.flash("error"))
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
                p(cls := "auth-subtitle")(
                  "Load recent public games from Lichess or Chess.com, then open them in the analysis shell for on-demand move insight or full-game review."
                ),
                pageError.map(msg => div(cls := "auth-error")(msg)),
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
                renderRecentSections(recentAccounts, recentAnalyses),
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
      notice: Option[String] = None,
      recentAccounts: List[ImportHistory.Account] = Nil,
      recentAnalyses: List[ImportHistory.Analysis] = Nil
  )(using ctx: Context): Page =
    val pageError = ctx.flash("error")
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
                p(cls := "auth-subtitle")(
                  "Choose a game to open it in analysis. From there you can explain one move on demand or run Game Chronicle on the full PGN."
                ),
                pageError.map(msg => div(cls := "auth-error")(msg)),
                notice.map(msg => div(cls := "auth-error")(msg)),
                games.nonEmpty.option(
                  div(cls := "auth-history__section")(
                    div(cls := "auth-history__section-head")(
                      strong("Turn This Account Into A Notebook"),
                      span("Create a saved notebook from the recent public game sample. The same account can be read as your own repair surface or as opponent prep.")
                    ),
                    form(cls := "auth-form", method := "post", action := routes.Study.createAccountNotebook.url)(
                      input(tpe := "hidden", name := "provider", value := provider),
                      input(tpe := "hidden", name := "username", value := username),
                      input(tpe := "hidden", name := "kind", value := "my_account_intelligence_lite"),
                      button(cls := "auth-submit", tpe := "submit")(
                        "Build My Account Notebook",
                        span(cls := "arrow")(" ->")
                      )
                    ),
                    form(cls := "auth-form", method := "post", action := routes.Study.createAccountNotebook.url)(
                      input(tpe := "hidden", name := "provider", value := provider),
                      input(tpe := "hidden", name := "username", value := username),
                      input(tpe := "hidden", name := "kind", value := "opponent_prep"),
                      button(cls := "auth-submit", tpe := "submit")(
                        "Build Opponent Prep",
                        span(cls := "arrow")(" ->")
                      )
                    )
                  )
                ),
                games.nonEmpty.option(
                  div(
                    games.map(renderGameCard(_, username))*
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
                renderRecentSections(recentAccounts, recentAnalyses),
                div(cls := "auth-links")(
                  a(href := routes.Importer.importGame.url)("New import"),
                  a(href := routes.UserAnalysis.index.url)("Back to Analysis")
                )
              )
            )
          )
        )

  private def renderGameCard(game: GameCard, username: String): Frag =
    div(
      cls := "auth-import-card"
    )(
      div(cls := "auth-import-card__head")(
        div(cls := "auth-history__badges")(
          providerBadge(game.provider),
          span(cls := "auth-badge auth-badge--result")(game.result)
        ),
        strong(cls := "auth-import-card__title")(s"${game.white} vs ${game.black}")
      ),
      div(cls := "auth-import-card__meta")(
        s"${game.playedAt} UTC",
        span(cls := "auth-import-card__dot")("•"),
        game.speed,
        span(cls := "auth-import-card__dot")("•"),
        game.gameId
      ),
      div(cls := "auth-import-card__actions")(
        form(method := "post", action := routes.Importer.sendGame.url, style := "margin: 0;")(
          input(tpe := "hidden", name := "pgn64", value := game.pgn64),
          input(tpe := "hidden", name := "sourceType", value := ImportHistory.sourceAccount),
          input(tpe := "hidden", name := "sourceProvider", value := game.provider),
          input(tpe := "hidden", name := "sourceUsername", value := username),
          input(tpe := "hidden", name := "sourceGameId", value := game.gameId),
          input(tpe := "hidden", name := "sourceWhite", value := game.white),
          input(tpe := "hidden", name := "sourceBlack", value := game.black),
          input(tpe := "hidden", name := "sourceResult", value := game.result),
          input(tpe := "hidden", name := "sourceSpeed", value := game.speed),
          input(tpe := "hidden", name := "sourcePlayedAt", value := game.playedAt),
          game.sourceUrl.map(url => input(tpe := "hidden", name := "sourceUrl", value := url)),
          button(tpe := "submit", cls := "auth-import-card__action")("Open in Analysis")
        ),
        game.sourceUrl.map(url =>
          a(href := url, target := "_blank", rel := "noopener noreferrer", cls := "auth-import-card__source")("Source")
        )
      )
    )

  private def renderRecentSections(
      recentAccounts: List[ImportHistory.Account],
      recentAnalyses: List[ImportHistory.Analysis]
  )(using ctx: Context): Frag =
    if recentAccounts.isEmpty && recentAnalyses.isEmpty then
      div(cls := "auth-history auth-history--empty")(
        div(cls := "auth-empty-state")(
          span(cls := "auth-empty-state__eyebrow")(if ctx.isAuth then "Saved imports" else "Cross-device history"),
          strong(if ctx.isAuth then "No saved analyses yet" else "Sign in to keep import history"),
          p(
            if ctx.isAuth then
              "Open a PGN or imported game once, and it will appear here for fast reopen in the same analysis shell."
            else "Guest sessions only keep local drafts. Sign in to save recent accounts and imported games across devices."
          )
        )
      )
    else
      div(cls := "auth-history")(
        recentAnalyses.nonEmpty.option:
          div(cls := "auth-history__section")(
            div(cls := "auth-history__section-head")(
              strong("Recent analyses"),
              span("Resume imported games from the exact PGN snapshot you saved, then continue with move insight or full-game review.")
            ),
            div(cls := "auth-history__list")(
              recentAnalyses.zipWithIndex.map { case (entry, index) =>
                renderRecentAnalysis(entry, priority = index == 0)
              }*
            )
          ),
        recentAccounts.nonEmpty.option:
          div(cls := "auth-history__section")(
            div(cls := "auth-history__section-head")(
              strong("Recent accounts"),
              span("Jump back into saved Lichess or Chess.com account lookups without retyping usernames.")
            ),
            div(cls := "auth-history__list")(
              recentAccounts.zipWithIndex.map { case (account, index) =>
                renderRecentAccount(account, priority = index == 0)
              }*
            )
          )
      )

  private def renderRecentAccount(account: ImportHistory.Account, priority: Boolean): Frag =
    val targetUrl =
      account.provider match
        case ImportHistory.providerChessCom => routes.Importer.importFromChessCom(account.username).url
        case _                              => routes.Importer.importFromLichess(account.username).url
    val footer =
      if priority then "Fastest way back to your last imported account list."
      else "Open this account list again without typing the username."
    a(
      href := targetUrl,
      cls := List(
        "auth-history__card",
        "auth-history__card--account",
        if priority then "auth-history__card--priority" else ""
      ).filter(_.nonEmpty).mkString(" ")
    )(
      div(cls := "auth-history__badges")(
        providerBadge(account.provider),
        priority.option(span(cls := "auth-badge auth-badge--priority")("Latest")),
        account.lastAnalysedAt.isDefined.option(span(cls := "auth-badge auth-badge--activity")("Analysed"))
      ),
      div(cls := "auth-history__title-row")(
        strong(cls := "auth-history__title")(s"@${account.username}"),
        span(cls := "auth-history__cta")(if priority then "Open latest" else "Open")
      ),
      div(cls := "auth-history__summary")(s"${account.analysisCount} saved analyses"),
      div(cls := "auth-history__meta")(footer)
    )

  private def renderRecentAnalysis(entry: ImportHistory.Analysis, priority: Boolean): Frag =
    val supportLine =
      List(
        entry.opening,
        entry.variant.filterNot(v => entry.opening.contains(v))
      ).flatten.mkString(" • ")
    a(
      href := routes.UserAnalysis.imported(entry._id).url,
      cls := List(
        "auth-history__card",
        "auth-history__card--analysis",
        if priority then "auth-history__card--priority" else ""
      ).filter(_.nonEmpty).mkString(" ")
    )(
      div(cls := "auth-history__badges")(
        entry.provider.map(providerBadge),
        span(cls := s"auth-badge auth-badge--${sourceBadgeTone(entry.sourceType)}")(sourceTypeLabel(entry.sourceType)),
        priority.option(span(cls := "auth-badge auth-badge--priority")("Latest"))
      ),
      div(cls := "auth-history__title-row")(
        strong(cls := "auth-history__title")(entry.title),
        span(cls := "auth-history__cta")(if priority then "Resume latest" else "Resume")
      ),
      div(cls := "auth-history__summary")(recentAnalysisMeta(entry)),
      supportLine.nonEmpty.option(div(cls := "auth-history__meta")(supportLine))
    )

  private def recentAnalysisMeta(entry: ImportHistory.Analysis): String =
    val line = List(
      entry.username.map("@" + _),
      entry.playedAtLabel.filterNot(_ == "-"),
      entry.result,
      entry.speed.filterNot(_ == "-")
    ).flatten.mkString(" • ")
    if line.nonEmpty then line else "Saved PGN snapshot ready to resume"

  private def providerLabel(provider: String): String =
    provider.trim.toLowerCase match
      case "chesscom" => "Chess.com Recent Games"
      case _          => "Lichess Recent Games"

  private def providerShortLabel(provider: String): String =
    provider.trim.toLowerCase match
      case "chesscom" => "Chess.com"
      case _          => "Lichess"

  private def providerBadge(provider: String): Frag =
    span(cls := s"auth-badge auth-badge--provider auth-badge--${providerTone(provider)}")(providerShortLabel(provider))

  private def providerTone(provider: String): String =
    provider.trim.toLowerCase match
      case "chesscom" => "chesscom"
      case _          => "lichess"

  private def sourceTypeLabel(sourceType: String): String =
    sourceType match
      case ImportHistory.sourceManual => "Manual PGN"
      case _                          => "Imported game"

  private def sourceBadgeTone(sourceType: String): String =
    sourceType match
      case ImportHistory.sourceManual => "manual"
      case _                          => "imported"
