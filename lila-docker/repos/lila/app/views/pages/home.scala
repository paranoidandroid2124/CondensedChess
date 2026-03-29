package views.pages

import controllers.Main
import lila.app.UiEnv.{ *, given }
import lila.ui.Page
import play.api.libs.json.{ JsObject, Json }

import scala.util.Try
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
                p(cls := "home-eyebrow")("Chesstory Home"),
                h1("Continue where you left off."),
                p(cls := "home-hero__summary")(
                  "Open the last game, pattern report, or notebook first. Start something new only when you need a different entry point."
                ),
                renderContinueCard(data.continueCard)
              ),
              div(cls := "home-quickstart")(
                div(cls := "home-section-head")(
                  strong("Quick start"),
                  span("Four real entry points that already exist")
                ),
                div(cls := "home-quickstart__grid")(
                  data.quickActions.map(renderQuickAction)*
                )
              )
            ),
            st.section(cls := "home-section")(
              div(cls := "home-section-head")(
                strong("Recent analyses"),
                span("Resume recent analysis snapshots in Guided Review by default, or open the same game in Full Analysis when you need the whole board.")
              ),
              if data.recentAnalyses.nonEmpty then
                div(cls := "home-card-grid home-card-grid--analysis")(
                  data.recentAnalyses.map(renderRecentAnalysis)*
                )
              else renderEmptyStrip("No recent analyses yet", "Start from PGN or import a public game once and it will show up here.")
            ),
            div(cls := "home-secondary-grid")(
              st.section(cls := "home-section")(
                div(cls := "home-section-head")(
                  strong("Recent pattern reports"),
                  span("Jump back into My Patterns or Prep for Opponent without rebuilding the report.")
                ),
                if data.recentPatternReports.nonEmpty then
                  div(cls := "home-card-grid home-card-grid--reports")(
                    data.recentPatternReports.map(renderPatternReport)*
                  )
                else renderEmptyStrip("No recent pattern reports yet", "Open account patterns when you want recurring positions or a prep view from public games.")
              ),
              st.section(cls := "home-section")(
                div(cls := "home-section-head")(
                  strong("Recent notebooks"),
                  span("Reopen saved studies without turning Home into a full notebook directory.")
                ),
                if data.recentNotebooks.nonEmpty then
                  div(cls := "home-card-grid home-card-grid--notebooks")(
                    data.recentNotebooks.map(renderNotebook)*
                  )
                else renderEmptyStrip("No recent notebooks yet", "Create or save one from analysis or account patterns when the position is worth keeping.")
              )
            ),
            st.section(cls := "home-section home-section--accounts")(
              div(cls := "home-section-head")(
                strong("Recent public accounts"),
                span("Accounts tied to your recent imports or account searches. Open My Patterns from here when you want the account view.")
              ),
              if data.recentAccounts.nonEmpty then
                div(cls := "home-strip-grid")(
                  data.recentAccounts.map(renderAccountLookup)*
                )
              else
                renderEmptyStrip(
                  "No recent public accounts yet",
                  "Search a public Chess.com or Lichess username, or import a public account game once and it will show up here."
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
          p(cls := "home-continue-card__eyebrow")("Continue Guided Review"),
          h2(cls := "home-continue-card__title")(entry.title),
          p(cls := "home-continue-card__summary")(recentAnalysisMeta(entry)),
          supportLine.nonEmpty.option(p(cls := "home-continue-card__support")(supportLine)),
          div(cls := "home-continue-card__actions")(
            a(href := importedAnalysisUrl(entry._id, "review"), cls := "button button-fat")("Continue Guided Review"),
            a(href := importedAnalysisUrl(entry._id, "raw"), cls := "button button-metal")("Open full analysis")
          )
        )
      case Main.HomeContinueCard.PatternReport(job) =>
        val headline = reportHeadline(job).getOrElse(s"@${job.username}")
        st.article(cls := "home-continue-card")(
          p(cls := "home-continue-card__eyebrow")(kindLabel(job.kind.key)),
          h2(cls := "home-continue-card__title")(s"@${job.username}"),
          p(cls := "home-continue-card__summary")(headline),
          p(cls := "home-continue-card__support")(s"${providerLabel(job.provider)} • ${kindLabel(job.kind.key)}"),
          div(cls := "home-continue-card__actions")(
            a(href := reportUrl(job), cls := "button button-fat")(s"Open ${kindLabel(job.kind.key)}"),
            a(href := routes.AccountIntel.landing("", "").url, cls := "button button-metal")("See account patterns")
          )
        )
      case Main.HomeContinueCard.Notebook(entry) =>
        val sectionCount = entry.chapters.size
        st.article(cls := "home-continue-card")(
          p(cls := "home-continue-card__eyebrow")("Study notebook"),
          h2(cls := "home-continue-card__title")(entry.study.name.value),
          p(cls := "home-continue-card__summary")(
            s"${if entry.study.isPublic then "Public notebook" else "Private notebook"} • $sectionCount ${if sectionCount == 1 then "section" else "sections"}"
          ),
          p(cls := "home-continue-card__support")(
            "Reopen the saved study when the board, notes, and chapter flow already exist."
          ),
          div(cls := "home-continue-card__actions")(
            a(href := routes.Study.show(entry.study.id).url, cls := "button button-fat")("Open notebook"),
            a(href := analysisIndexUrl("raw"), cls := "button button-metal")("Open full analysis")
          )
        )
      case Main.HomeContinueCard.Starter =>
        st.article(cls := "home-continue-card")(
          p(cls := "home-continue-card__eyebrow")("Start fresh"),
          h2(cls := "home-continue-card__title")("No recent work yet"),
          p(cls := "home-continue-card__summary")(
            "Start from a PGN, open full analysis directly, look up account patterns, or open the current live strategic puzzle."
          ),
          div(cls := "home-continue-card__actions")(
            a(href := routes.Importer.importGame.url, cls := "button button-fat")("Start from PGN"),
            a(href := analysisIndexUrl("raw"), cls := "button button-metal")("Open full analysis")
          )
        )

  private def renderQuickAction(action: Main.HomeQuickAction): Frag =
    a(href := action.href, cls := "home-action-card")(
      span(cls := "home-action-card__label")(action.label),
      strong(cls := "home-action-card__title")(action.title),
      p(cls := "home-action-card__copy")(action.copy),
      span(cls := "home-action-card__cta")("Open")
    )

  private def renderRecentAnalysis(entry: lila.analyse.ImportHistory.Analysis): Frag =
    val supportLine =
      List(entry.opening, entry.variant.filterNot(v => entry.opening.contains(v))).flatten.mkString(" • ")
    a(href := importedAnalysisUrl(entry._id, "review"), cls := "home-card home-card--analysis")(
      div(cls := "home-card__badges")(
        entry.provider.map(providerBadge),
        span(cls := s"home-pill home-pill--${sourceBadgeTone(entry.sourceType)}")(sourceTypeLabel(entry.sourceType))
      ),
      strong(cls := "home-card__title")(entry.title),
      p(cls := "home-card__summary")(recentAnalysisMeta(entry)),
      supportLine.nonEmpty.option(p(cls := "home-card__meta")(supportLine)),
      span(cls := "home-card__cta")("Resume")
    )

  private def renderPatternReport(job: lila.accountintel.AccountIntel.AccountIntelJob): Frag =
    a(href := reportUrl(job), cls := "home-card home-card--report")(
      div(cls := "home-card__badges")(
        providerBadge(job.provider),
        span(cls := "home-pill home-pill--pattern")(kindLabel(job.kind.key))
      ),
      strong(cls := "home-card__title")(s"@${job.username}"),
      p(cls := "home-card__summary")(reportHeadline(job).getOrElse("Latest finished report ready to reopen.")),
      p(cls := "home-card__meta")(s"${providerLabel(job.provider)} • ${kindLabel(job.kind.key)}"),
      span(cls := "home-card__cta")("Open")
    )

  private def renderNotebook(entry: lila.study.Study.WithChaptersAndLiked): Frag =
    val sectionCount = entry.chapters.size
    val sectionPreview =
      entry.chapters.take(2).map(_.value).mkString(" • ")
    a(href := routes.Study.show(entry.study.id).url, cls := "home-card home-card--notebook")(
      div(cls := "home-card__badges")(
        span(cls := "home-pill home-pill--notebook")(if entry.study.isPublic then "Public notebook" else "Private notebook"),
        entry.liked.option(span(cls := "home-pill home-pill--liked")("Liked"))
      ),
      strong(cls := "home-card__title")(entry.study.name.value),
      p(cls := "home-card__summary")(s"$sectionCount ${if sectionCount == 1 then "section" else "sections"}"),
      sectionPreview.nonEmpty.option(p(cls := "home-card__meta")(sectionPreview)),
      span(cls := "home-card__cta")("Open")
    )

  private def renderAccountLookup(account: lila.analyse.ImportHistory.Account): Frag =
    a(
      href := routes.AccountIntel.product(
        account.provider,
        account.username,
        lila.accountintel.AccountIntel.ProductKind.MyAccountIntelligenceLite.key,
        ""
      ).url,
      cls := "home-strip-card"
    )(
      div(cls := "home-strip-card__top")(
        strong(s"@${account.username}"),
        providerBadge(account.provider)
      ),
      p(s"${account.analysisCount} saved analyses • ${account.searchCount} account searches"),
      span(cls := "home-card__cta")("Open My Patterns")
    )

  private def renderEmptyStrip(title: String, copy: String): Frag =
    div(cls := "home-empty")(
      strong(title),
      p(copy)
    )

  private def reportUrl(job: lila.accountintel.AccountIntel.AccountIntelJob): String =
    val base = routes.AccountIntel.product(job.provider, job.username, job.kind.key, "").url
    s"$base?jobId=${URLEncoder.encode(job.id, StandardCharsets.UTF_8)}"

  private def analysisIndexUrl(mode: String): String =
    s"${routes.UserAnalysis.index.url}?mode=${URLEncoder.encode(mode, StandardCharsets.UTF_8)}"

  private def importedAnalysisUrl(id: String, mode: String): String =
    s"${routes.UserAnalysis.imported(id).url}?mode=${URLEncoder.encode(mode, StandardCharsets.UTF_8)}"

  private def reportHeadline(job: lila.accountintel.AccountIntel.AccountIntelJob): Option[String] =
    job.surfaceJson
      .flatMap(raw => Try(Json.parse(raw).as[JsObject]).toOption)
      .flatMap(js => (js \ "headline").asOpt[String])

  private def recentAnalysisMeta(entry: lila.analyse.ImportHistory.Analysis): String =
    val line = List(
      entry.username.map("@" + _),
      entry.playedAtLabel.filterNot(_ == "-"),
      entry.result,
      entry.speed.filterNot(_ == "-")
    ).flatten.mkString(" • ")
    if line.nonEmpty then line else "Saved PGN snapshot ready to resume"

  private def providerBadge(provider: String): Frag =
    span(cls := s"home-pill home-pill--provider home-pill--${providerTone(provider)}")(providerLabel(provider))

  private def providerTone(provider: String): String =
    provider.trim.toLowerCase match
      case "chesscom" => "chesscom"
      case _          => "lichess"

  private def providerLabel(provider: String): String =
    provider.trim.toLowerCase match
      case "chesscom" => "Chess.com"
      case _          => "Lichess"

  private def kindLabel(kind: String): String =
    kind match
      case "my_account_intelligence_lite" => "My Patterns"
      case "opponent_prep"                => "Prep for Opponent"
      case other                          => other.replace('_', ' ')

  private def sourceTypeLabel(sourceType: String): String =
    sourceType match
      case lila.analyse.ImportHistory.sourceManual => "Manual PGN"
      case _                                       => "Imported game"

  private def sourceBadgeTone(sourceType: String): String =
    sourceType match
      case lila.analyse.ImportHistory.sourceManual => "manual"
      case _                                       => "imported"
