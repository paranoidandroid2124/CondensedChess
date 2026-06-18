package views.pages

import controllers.Main
import lila.analyse.ImportHistory
import lila.app.UiEnv.{ *, given }
import lila.ui.Page

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object home:

  def apply(data: Main.HomePageData)(using ctx: Context): Page =
    Page("Home - Chesstory")
      .css("home")
      .wrap: _ =>
        main(cls := "home-page")(
          div(cls := "home-shell")(
            st.section(cls := "home-hero")(
              div(cls := "home-hero__copy")(
                p(cls := "home-eyebrow")("Analysis room"),
                h1("Review the game with the board first."),
                p(cls := "home-hero__summary")(
                  "Open saved games or studies without losing the board that made the move matter."
                ),
                renderContinueCard(data.continueCard)
              ),
              div(cls := "home-quickstart")(
                div(cls := "home-section-head")(
                  strong("Choose the next board"),
                  span("Start from the kind of chess work you want to do now.")
                ),
                div(cls := "home-quickstart__grid")(
                  data.quickActions.map(renderQuickAction)*
                )
              )
            ),
            st.section(cls := "home-section home-section--reviews")(
              div(cls := "home-section-head")(
                strong("Recent game analyses"),
                span("Reopen a saved game on the board, or return to the position when you want to explore the line.")
              ),
              if data.recentAnalyses.nonEmpty then
                div(cls := "home-card-grid home-card-grid--analysis")(
                  data.recentAnalyses.map(renderRecentAnalysis)*
                )
                else renderEmptyStrip("No recent games yet", "Start from a pasted game or bring in a public game once and it will show up here.")
            ),
            st.section(cls := "home-section")(
              div(cls := "home-section-head")(
                strong("Saved studies"),
                span("Reopen saved boards, notes, and sections without searching through every study.")
              ),
              if data.recentNotebooks.nonEmpty then
                div(cls := "home-card-grid home-card-grid--notebooks")(
                  data.recentNotebooks.map(renderNotebook)*
                )
              else renderEmptyStrip("No saved studies yet", "Save one from a board when the position is worth keeping.")
            ),
            st.section(cls := "home-section home-section--accounts")(
              div(cls := "home-section-head")(
                strong("Players you study"),
                span("Open a player again when you want to study the positions that appear in their games.")
              ),
              if data.recentAccounts.nonEmpty then
                div(cls := "home-strip-grid")(
                  data.recentAccounts.map(renderAccountLookup)*
                )
              else
                renderEmptyStrip(
                  "No recent public accounts yet",
                  "Search a public Chess.com or Lichess username, or bring in a public account game once and it will show up here."
                )
            )
          )
        )

  private def renderContinueCard(card: Main.HomeContinueCard)(using ctx: Context): Frag =
    card match
      case Main.HomeContinueCard.Analysis(entry) =>
        val supportLine =
          List(entry.opening, entry.variant.filterNot(v => entry.opening.contains(v))).flatten.mkString(" • ")
        st.article(cls := "home-continue-card")(
          p(cls := "home-continue-card__eyebrow")("Saved game"),
          h2(cls := "home-continue-card__title")(entry.title),
          p(cls := "home-continue-card__summary")(recentAnalysisMeta(entry)),
          supportLine.nonEmpty.option(p(cls := "home-continue-card__support")(supportLine)),
          div(cls := "home-continue-card__actions")(
            a(href := importedAnalysisUrl(entry._id, "raw"), cls := "button button-fat")("Continue analysis"),
            a(href := importedAnalysisUrl(entry._id, "raw"), cls := "button button-metal")("Open board")
          )
        )
      case Main.HomeContinueCard.Notebook(entry) =>
        val sectionCount = entry.chapters.size
        st.article(cls := "home-continue-card")(
          p(cls := "home-continue-card__eyebrow")("Saved study"),
          h2(cls := "home-continue-card__title")(entry.study.name.value),
          p(cls := "home-continue-card__summary")(
            s"${if entry.study.isPublic then "Public study" else "Private study"} • $sectionCount ${if sectionCount == 1 then "section" else "sections"}"
          ),
          p(cls := "home-continue-card__support")(
            "Reopen the saved study when the board, notes, and section flow already exist."
          ),
          div(cls := "home-continue-card__actions")(
            a(href := routes.Study.show(entry.study.id).url, cls := "button button-fat")("Open study"),
            a(href := analysisIndexUrl("raw"), cls := "button button-metal")("Open board")
          )
        )
      case Main.HomeContinueCard.Starter =>
        st.article(cls := "home-continue-card")(
          p(cls := "home-continue-card__eyebrow")("Start analysis"),
          h2(cls := "home-continue-card__title")("No recent game yet"),
          p(cls := "home-continue-card__summary")(
            "Start from a pasted game, open a board, or return to a saved study."
          ),
          div(cls := "home-continue-card__actions")(
            a(href := routes.Importer.importGame.url, cls := "button button-fat")("Start from game text"),
            a(href := analysisIndexUrl("raw"), cls := "button button-metal")("Open board")
          )
        )

  private def renderQuickAction(action: Main.HomeQuickAction): Frag =
    a(href := action.href, cls := "home-action-card")(
      span(cls := "home-action-card__label")(action.label),
      strong(cls := "home-action-card__title")(action.title),
      p(cls := "home-action-card__copy")(action.copy),
      span(cls := "home-action-card__cta")("Start")
    )

  private def renderRecentAnalysis(entry: ImportHistory.Analysis): Frag =
    val supportLine =
      List(entry.opening, entry.variant.filterNot(v => entry.opening.contains(v))).flatten.mkString(" • ")
    a(href := importedAnalysisUrl(entry._id, "raw"), cls := "home-card home-card--analysis")(
      div(cls := "home-card__badges")(
        entry.provider.map(providerBadge),
        span(cls := s"home-pill home-pill--${sourceBadgeTone(entry.sourceType)}")(sourceTypeLabel(entry.sourceType))
      ),
      strong(cls := "home-card__title")(entry.title),
      p(cls := "home-card__summary")(recentAnalysisMeta(entry)),
      supportLine.nonEmpty.option(p(cls := "home-card__meta")(supportLine)),
      span(cls := "home-card__cta")("Open analysis")
    )

  private def renderNotebook(entry: lila.study.Study.WithChaptersAndLiked): Frag =
    val sectionCount = entry.chapters.size
    val sectionPreview =
      entry.chapters.take(2).map(_.value).mkString(" • ")
    a(href := routes.Study.show(entry.study.id).url, cls := "home-card home-card--notebook")(
      div(cls := "home-card__badges")(
        span(cls := "home-pill home-pill--notebook")(if entry.study.isPublic then "Public study" else "Private study"),
        entry.liked.option(span(cls := "home-pill home-pill--liked")("Liked"))
      ),
      strong(cls := "home-card__title")(entry.study.name.value),
      p(cls := "home-card__summary")(s"$sectionCount ${if sectionCount == 1 then "section" else "sections"}"),
      sectionPreview.nonEmpty.option(p(cls := "home-card__meta")(sectionPreview)),
      span(cls := "home-card__cta")("Open section")
    )

  private def renderAccountLookup(account: ImportHistory.Account): Frag =
    a(
      href := routes.Importer.importGame.url,
      cls := "home-strip-card"
    )(
      div(cls := "home-strip-card__top")(
        strong(s"@${account.username}"),
        providerBadge(account.provider)
      ),
      p(s"${account.analysisCount} saved games"),
      span(cls := "home-card__cta")("Load games")
    )

  private def renderEmptyStrip(title: String, copy: String): Frag =
    div(cls := "home-empty")(
      strong(title),
      p(copy)
    )

  private def analysisIndexUrl(mode: String): String =
    s"${routes.UserAnalysis.index.url}?mode=${URLEncoder.encode(mode, StandardCharsets.UTF_8)}"

  private def importedAnalysisUrl(id: String, mode: String): String =
    s"${routes.UserAnalysis.imported(id).url}?mode=${URLEncoder.encode(mode, StandardCharsets.UTF_8)}"

  private def recentAnalysisMeta(entry: ImportHistory.Analysis): String =
    val line = List(
      entry.username.map("@" + _),
      entry.playedAtLabel.filterNot(_ == "-"),
      entry.result,
      entry.speed.filterNot(_ == "-")
    ).flatten.mkString(" • ")
    if line.nonEmpty then line else "Saved game ready for review"

  private def providerBadge(provider: String): Frag =
    span(cls := s"home-pill home-pill--provider home-pill--${providerTone(provider)}")(providerLabel(provider))

  private def providerTone(provider: String): String =
    provider.trim.toLowerCase match
      case ImportHistory.providerChessCom => ImportHistory.providerChessCom
      case _                              => ImportHistory.providerLichess

  private def providerLabel(provider: String): String =
    provider.trim.toLowerCase match
      case ImportHistory.providerChessCom => "Chess.com"
      case _                              => "Lichess"

  private def sourceTypeLabel(sourceType: String): String =
    sourceType match
      case ImportHistory.sourceManual => "Pasted game"
      case _                          => "Player game"

  private def sourceBadgeTone(sourceType: String): String =
    sourceType match
      case ImportHistory.sourceManual => "manual"
      case _                          => "imported"
