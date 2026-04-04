package views

import controllers.AccountIntel.ProductState
import lila.analyse.ImportHistory
import lila.app.UiEnv.*
import play.api.libs.json.{ JsObject, Json }

import scala.util.Try
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object accountIntel:

  private def withJobId(url: String, jobId: Option[String]): String =
    jobId.fold(url): id =>
      s"$url${if url.contains('?') then "&" else "?"}jobId=${URLEncoder.encode(id, StandardCharsets.UTF_8)}"

  private def productUrl(
      provider: String,
      username: String,
      kindKey: String,
      side: String = "",
      jobId: Option[String] = None
  ): String =
    withJobId(routes.AccountIntel.product(provider, username, kindKey, side).url, jobId)

  private def productUrl(state: ProductState): String =
    productUrl(state.provider, state.username, state.kind.key, "", state.selectedJobId)

  private def productUrl(state: ProductState, side: String): String =
    productUrl(state.provider, state.username, state.kind.key, side, state.selectedJobId)

  private def surfaceApiUrl(state: ProductState): String =
    surfaceApiUrl(state, state.kind.key, state.selectedJobId)

  private def surfaceApiUrl(
      state: ProductState,
      kindKey: String,
      jobId: Option[String]
  ): String =
    withJobId(routes.AccountIntel.surfaceApi(state.provider, state.username, kindKey).url, jobId)

  def landing(
      provider: String,
      username: String,
      selectedKindKey: String,
      recentAccounts: List[ImportHistory.Account],
      recentRuns: List[lila.accountintel.AccountIntel.AccountIntelJob]
  )(using ctx: Context): Page =
    val pageError = ctx.flash("error")
    Page("Account Intel - Chesstory")
      .css("auth")
      .wrap: _ =>
        main(cls := "auth-page auth-page--importer")(
          div(cls := "landing-container")(
            div(cls := "landing-header")(
              a(href := homeUrl, cls := "logo")("Chesstory")
            ),
            div(cls := "auth-container auth-container--wide")(
              div(cls := "auth-card auth-card--importer auth-card--account-product")(
                div(cls := "account-product-shell")(
                  div(cls := "importer-hero importer-hero--account-product")(
                    div(cls := "importer-hero__eyebrow")("Pattern report"),
                    h1(cls := "auth-title importer-hero__title")("Choose the account and the question you want answered."),
                    p(cls := "auth-subtitle importer-hero__subtitle")(
                      "Pick My Patterns when you want your own recurring issues, or Prep for Opponent when you are preparing for someone else. The latest finished report reopens first."
                    ),
                    div(cls := "importer-summary-strip")(
                      summaryChip("My Patterns", "Recurring mistakes and typical positions"),
                      summaryChip("Prep for Opponent", "Game plan and pressure points"),
                      summaryChip("History", "Reopen the latest finished report")
                    )
                  ),
                  pageError.map(msg => div(cls := "auth-error")(msg)),
                  div(cls := "importer-grid importer-grid--product")(
                    div(cls := "importer-panel importer-panel--intake")(
                      div(cls := "importer-panel__head")(
                        strong(cls := "importer-panel__title")("Find an account"),
                        p(cls := "importer-panel__copy")(
                          "Enter a public username, choose the mode first, and reopen the latest finished result if it already exists."
                        )
                      ),
                      form(cls := "auth-form importer-form", method := "get", action := routes.AccountIntel.landing("", "").url)(
                        div(cls := "form-group")(
                          label(`for` := "account-provider")("Provider"),
                          st.select(id := "account-provider", name := "provider")(
                            option(value := "chesscom", if provider == "chesscom" then selected := true else emptyFrag)("Chess.com"),
                            option(value := "lichess", if provider == "lichess" then selected := true else emptyFrag)("Lichess")
                          )
                        ),
                        div(cls := "form-group")(
                          label(`for` := "account-username")("Username"),
                          input(
                            id := "account-username",
                            tpe := "text",
                            name := "username",
                            value := username,
                            placeholder := "e.g. ych24 or hikaru",
                            required
                          )
                        ),
                        div(cls := "form-group account-product-mode-field")(
                          span(cls := "account-product-mode-field__label")("Mode"),
                          div(cls := "account-product-mode-choice-grid")(
                            landingModeChoice(
                              selectedKindKey = selectedKindKey,
                              kindKey = "my_account_intelligence_lite",
                              title = "My Patterns",
                              body = "Review your recurring mistakes, typical positions, and what to look for next."
                            ),
                            landingModeChoice(
                              selectedKindKey = selectedKindKey,
                              kindKey = "opponent_prep",
                              title = "Prep for Opponent",
                              body = "Build a short game plan, typical position, and prep checklist for the matchup."
                            )
                          )
                        ),
                        div(cls := "importer-panel__hint")(
                          span("Public games only"),
                          span("Async analysis build"),
                          span("Study notebook stays secondary")
                        ),
                        button(cls := "auth-submit importer-submit", tpe := "submit")(
                          "Open pattern report",
                          span(cls := "arrow")(" ->")
                        )
                      )
                    ),
                    div(cls := "importer-side")(
                      div(cls := "importer-panel importer-panel--guide")(
                        div(cls := "importer-panel__head")(
                          strong(cls := "importer-panel__title")("What opens first"),
                          p(cls := "importer-panel__copy")(
                            "The result page should answer the primary question without forcing a study notebook decision first."
                          )
                        ),
                        div(cls := "account-product-note-grid")(
                          noteCard("My Patterns", "Start from the main recurring issue, then move to the typical position."),
                          noteCard("Prep for Opponent", "Start from the game plan, then move into openings and the typical position."),
                          noteCard("Games that show the pattern", "Use supporting games as evidence, not as the first thing you read."),
                          noteCard("History", "Compare the newest report against older ones without rebuilding your flow.")
                        )
                      ),
                      recentRuns.nonEmpty.option(
                        div(cls := "importer-panel importer-panel--guide")(
                          div(cls := "importer-panel__head")(
                            strong(cls := "importer-panel__title")("Recent results"),
                            p(cls := "importer-panel__copy")(
                              "Jump straight back into the latest account reports you already built."
                            )
                          ),
                          div(cls := "account-product-recent-grid")(
                            recentRuns.take(4).map(renderRecentRunCard)*
                          )
                        )
                      )
                    )
                  ),
                  recentAccounts.nonEmpty.option(
                    div(cls := "importer-panel importer-panel--recent")(
                      div(cls := "importer-panel__head")(
                        strong(cls := "importer-panel__title")("Recent account lookups"),
                        p(cls := "importer-panel__copy")("Use your recent searches as fast re-entry points.")
                      ),
                      div(cls := "account-product-recent-grid")(
                        recentAccounts.take(6).map(renderRecentAccountCard)*
                      )
                    )
                  ),
                  div(cls := "auth-links")(
                    a(href := routes.UserAnalysis.index.url)("Open one game in analysis"),
                    a(href := routes.Importer.importGame.url)("Legacy importer")
                  )
                )
              )
            )
          )
        )

  def product(state: ProductState, side: String, initialState: JsObject)(using ctx: Context): Page =
    val safeSide = normalizeSide(side, state.displayedJob.flatMap(surfaceOf))
    val surface = state.displayedJob.flatMap(surfaceOf)
    val active = state.activeJob
    Page(s"@${state.username} • Account Intel")
      .css("auth")
      .wrap: _ =>
        main(cls := "auth-page auth-page--importer")(
          div(cls := "landing-container")(
            div(cls := "landing-header")(
              a(href := homeUrl, cls := "logo")("Chesstory")
            ),
            div(cls := "auth-container auth-container--wide")(
              div(cls := "auth-card auth-card--importer auth-card--account-product")(
                surface.fold(renderEmptyProduct(state, active)) { js =>
                  renderProductSurface(state, js, safeSide, active, initialState)
                }
              )
            ),
            script(id := "account-intel-state", tpe := "application/json")(raw(Json.stringify(initialState)))
          )
        )

  def status(job: lila.accountintel.AccountIntel.AccountIntelJob)(using ctx: Context): Page =
    val resultHref = productUrl(job.provider, job.username, job.kind.key, "", Some(job.id))
    Page("Building Account Intel - Chesstory")
      .css("auth")
      .wrap: _ =>
        main(cls := "auth-page auth-page--importer")(
          div(cls := "landing-container")(
            div(cls := "landing-header")(
              a(href := homeUrl, cls := "logo")("Chesstory")
            ),
            div(cls := "auth-container auth-container--wide")(
              div(
                cls := "auth-card auth-card--importer auth-card--status js-account-intel-status",
                attr("data-job-id") := job.id,
                attr("data-result-url") := resultHref
              )(
                div(cls := "status-shell")(
                  div(cls := "importer-hero importer-hero--status")(
                    div(cls := "importer-hero__eyebrow")("Pattern report"),
                    h1(cls := "auth-title importer-hero__title")(statusTitle(job)),
                    p(cls := "auth-subtitle importer-hero__subtitle")(
                      "This run is building in the background. The pattern report opens automatically after the analysis is ready."
                    ),
                    div(cls := "importer-summary-strip")(
                      summaryChip(s"@${job.username}", providerLabel(job.provider)),
                      summaryChip(kindLabel(job.kind.key), "Mode"),
                      summaryChip(statusChipLabel(job), stageLabel(job.progressStage))
                    )
                  ),
                  div(cls := "status-grid")(
                    div(cls := "importer-panel importer-panel--status")(
                      div(cls := "importer-panel__head")(
                        strong(cls := "importer-panel__title")("Analysis progress"),
                        p(cls := "importer-panel__copy")(
                          "You should be able to tell at a glance whether the worker is moving, blocked, or already finished."
                        )
                      ),
                      div(cls := "status-progress")(
                        progressSteps.map(step =>
                          div(
                            cls := s"status-progress__step status-progress__step--${stepState(job, step.key)}"
                          )(
                            span(cls := "status-progress__index")(step.index.toString),
                            div(cls := "status-progress__copy")(
                              strong(step.label),
                              span(step.description)
                            )
                          )
                        )*
                      ),
                      div(cls := "status-meta-grid")(
                        metaCard("Requested", job.requestedAt.toString),
                        metaCard("Run ID", job.id),
                        metaCardRich("Current step", span(cls := "js-ai-status-meta-stage")(stageLabel(job.progressStage)))
                      )
                    ),
                    div(cls := "importer-panel importer-panel--status-side")(
                      div(cls := "importer-panel__head")(
                        strong(cls := "importer-panel__title")("Next action"),
                        p(cls := "importer-panel__copy")(
                          statusActionCopy(job)
                        )
                      ),
                      div(cls := "status-callout status-callout--primary")(
                        strong(statusTitle(job)),
                        span(cls := "js-ai-status-callout-copy")(
                          if job.status == lila.accountintel.AccountIntel.JobStatus.Failed then
                            "The build stopped before the pattern report was attached."
                          else if job.status == lila.accountintel.AccountIntel.JobStatus.Succeeded then
                            "The pattern report is ready to open."
                          else "The worker is still processing the account."
                        )
                      ),
                      job.warnings.nonEmpty.option(
                        div(cls := "status-warning-list")(
                          job.warnings.map(w =>
                            div(cls := "status-callout status-callout--warning")(
                              strong("Confidence note"),
                              span(w)
                            )
                          )*
                        )
                      ),
                      job.errorMessage.map(msg =>
                        div(cls := "status-callout status-callout--error")(
                          strong("Build error"),
                          span(msg)
                        )
                      ),
                      div(cls := "auth-links status-links")(
                        (job.status match
                          case lila.accountintel.AccountIntel.JobStatus.Succeeded =>
                            a(href := resultHref, cls := "status-links__primary js-ai-status-result")("Open pattern report")
                          case lila.accountintel.AccountIntel.JobStatus.Failed =>
                            statusPrimaryAction(job, resultHref)
                          case _ =>
                            a(href := resultHref, cls := "status-links__primary js-ai-status-result", hidden := "hidden")(
                              "Open pattern report"
                            )),
                        job.notebookUrl
                          .map(url => a(href := url, cls := "js-ai-status-notebook")("Open study notebook"))
                          .getOrElse(
                            a(href := "#", cls := "js-ai-status-notebook", hidden := "hidden")("Open study notebook")
                          ),
                        a(href := routes.AccountIntel.landing("", "").url)("Back to pattern report")
                      )
                    )
                  )
                )
              )
            )
          )
        )

  private def renderProductSurface(
      state: ProductState,
      surface: JsObject,
      side: String,
      activeJob: Option[lila.accountintel.AccountIntel.AccountIntelJob],
      initialState: JsObject
  ): Frag =
    val headline = (surface \ "headline").asOpt[String].getOrElse(s"@${state.username}")
    val confidence = (surface \ "confidence" \ "label").asOpt[String].getOrElse("weak")
    val sampled = (surface \ "source" \ "sampledGameCount").asOpt[Int].getOrElse(0)
    val generatedAt = (surface \ "generatedAt").asOpt[String].getOrElse("")
    val patterns = (surface \ "patterns").asOpt[List[JsObject]].getOrElse(Nil)
    val overviewCards = (surface \ "overview" \ "cards").asOpt[List[JsObject]].getOrElse(Nil)
    val openingCards = (surface \ "openingCards").asOpt[List[JsObject]].getOrElse(Nil)
    val exemplarGames = (surface \ "exemplarGames").asOpt[List[JsObject]].getOrElse(Nil)
    val actionCards = (surface \ "actions").asOpt[List[JsObject]].getOrElse(Nil)
    val checklist = (surface \ "checklist").asOpt[JsObject]
    val notebookUrl = state.displayedJob.flatMap(_.notebookUrl)
    val visiblePatterns = patterns.filter(pattern => side == "all" || (pattern \ "side").asOpt[String].contains(side))
    val leadPattern = visiblePatterns.headOption
    val hasAdditionalPatterns = visiblePatterns.drop(1).nonEmpty
    div(
      cls := "account-product-shell js-account-intel-product",
      attr("data-state-url") := surfaceApiUrl(state),
      attr("data-side") := side,
      attr("data-page-base-url") := s"/account-intel/${state.provider}/${state.username}",
      attr("data-landing-url") := routes.AccountIntel.landing("", "").url,
      attr("data-strategic-puzzle-url") := routes.StrategicPuzzle.home.url
    )(
      div(cls := "account-product-header")(
        div(cls := "importer-hero importer-hero--account-product")(
          div(cls := "importer-hero__eyebrow")(providerLabel(state.provider)),
          h1(cls := "auth-title importer-hero__title")(s"@${state.username}"),
          p(cls := "auth-subtitle importer-hero__subtitle js-ai-headline")(headline),
          div(cls := "importer-summary-strip js-ai-summary-strip")(
            summaryChip(sampled.toString, "Sampled games"),
            summaryChip(confidence.capitalize, "Confidence"),
            summaryChip(kindLabel(state.kind.key), s"Generated $generatedAt")
          )
        ),
        div(cls := "js-ai-active-job")(activeJob.map(renderActiveJobCallout)),
        div(cls := "account-product-hero-actions")(
          div(cls := "account-product-mode-switch js-ai-mode-switch")(
            modeLink(state, lila.accountintel.AccountIntel.ProductKind.MyAccountIntelligenceLite, "My Patterns"),
            modeLink(state, lila.accountintel.AccountIntel.ProductKind.OpponentPrep, "Prep for Opponent")
          ),
          div(cls := "account-product-action-row")(
            form(method := "post", action := routes.AccountIntel.submit.url, cls := "js-ai-rerun-form")(
              input(tpe := "hidden", name := "provider", value := state.provider),
              input(tpe := "hidden", name := "username", value := state.username),
              input(tpe := "hidden", name := "kind", value := state.kind.key, cls := "js-ai-rerun-kind"),
              input(tpe := "hidden", name := "force", value := "true"),
              button(cls := "auth-submit importer-submit account-product-cta", tpe := "submit")(
                "Refresh analysis",
                span(cls := "arrow")(" ->")
              )
            ),
            a(href := routes.AccountIntel.landing("", "").url, cls := "account-product-secondary-link")("Change account")
          )
        )
      ),
      div(cls := "account-product-workspace js-ai-workspace")(
        renderWorkspace(
          state = state,
          side = side,
          patterns = patterns,
          leadPattern = leadPattern,
          hasAdditionalPatterns = hasAdditionalPatterns,
          actionCards = actionCards,
          checklist = checklist,
          openingCards = openingCards,
          exemplarGames = exemplarGames,
          overviewCards = overviewCards,
          notebookUrl = notebookUrl
        )
      ),
      div(cls := "js-ai-state-source", attr("data-initial-state") := Json.stringify(initialState))
    )

  private def renderWorkspace(
      state: ProductState,
      side: String,
      patterns: List[JsObject],
      leadPattern: Option[JsObject],
      hasAdditionalPatterns: Boolean,
      actionCards: List[JsObject],
      checklist: Option[JsObject],
      openingCards: List[JsObject],
      exemplarGames: List[JsObject],
      overviewCards: List[JsObject],
      notebookUrl: Option[String]
  ): Frag =
    val secondarySections =
      if state.kind == lila.accountintel.AccountIntel.ProductKind.OpponentPrep then
        frag(
          div(cls := "js-ai-openings")(renderOpeningIdentity(openingCards)),
          hasAdditionalPatterns.option(
            div(cls := "js-ai-patterns")(renderPatterns(state, side, patterns, dropLead = true))
          ),
          div(cls := "js-ai-exemplars")(renderExemplarGames(exemplarGames)),
          div(cls := "js-ai-support")(renderSupportPanels(state, notebookUrl, overviewCards))
        )
      else
        frag(
          hasAdditionalPatterns.option(
            div(cls := "js-ai-patterns")(renderPatterns(state, side, patterns, dropLead = true))
          ),
          div(cls := "js-ai-openings")(renderOpeningIdentity(openingCards)),
          div(cls := "js-ai-exemplars")(renderExemplarGames(exemplarGames)),
          div(cls := "js-ai-support")(renderSupportPanels(state, notebookUrl, overviewCards))
        )
    div(cls := "account-product-body account-product-body--coach")(
      div(cls := "account-product-rail")(
        div(cls := "account-product-rail-shell js-ai-rail-shell", tabindex := -1)(
          div(cls := "account-product-rail-slot account-product-rail-slot--lead js-ai-lead-pattern")(
            renderLeadPattern(state.kind, leadPattern)
          ),
          div(cls := "account-product-rail-slot account-product-rail-slot--action js-ai-actions")(
            renderActionPanel(state.kind, actionCards, checklist, notebookUrl)
          )
        )
      ),
      div(cls := "account-product-secondary")(
        div(cls := "account-product-secondary-scroll js-ai-secondary-scroll", tabindex := -1)(
          secondarySections
        )
      )
    )

  private def renderEmptyProduct(
      state: ProductState,
      activeJob: Option[lila.accountintel.AccountIntel.AccountIntelJob]
  ): Frag =
    div(cls := "account-product-shell")(
      div(cls := "importer-hero importer-hero--account-product")(
        div(cls := "importer-hero__eyebrow")(providerLabel(state.provider)),
        h1(cls := "auth-title importer-hero__title")(s"@${state.username}"),
        p(cls := "auth-subtitle importer-hero__subtitle")(
          "No finished pattern report is attached yet. Choose the mode first, start a fresh run, then come back here for the standalone result page."
        )
      ),
      activeJob.map(renderActiveJobCallout),
      div(cls := "account-product-empty")(
        div(cls := "status-callout status-callout--primary")(
          strong("Analyze this account"),
          span("Choose My Patterns or Prep for Opponent. The finished review will live here, while the study notebook stays secondary.")
        ),
        div(cls := "account-product-mode-switch")(
          modeLink(state, lila.accountintel.AccountIntel.ProductKind.MyAccountIntelligenceLite, "My Patterns"),
          modeLink(state, lila.accountintel.AccountIntel.ProductKind.OpponentPrep, "Prep for Opponent")
        ),
        form(method := "post", action := routes.AccountIntel.submit.url)(
          input(tpe := "hidden", name := "provider", value := state.provider),
          input(tpe := "hidden", name := "username", value := state.username),
          input(tpe := "hidden", name := "kind", value := state.kind.key),
          button(cls := "auth-submit importer-submit account-product-cta", tpe := "submit")(
            "Analyze this account",
            span(cls := "arrow")(" ->")
          )
        ),
        div(cls := "auth-links")(
          a(href := routes.UserAnalysis.index.url)("Open one game in analysis"),
                  a(href := routes.AccountIntel.landing("", "").url)("Back to pattern report")
        )
      )
    )

  private def renderOverview(cards: List[JsObject], summary: String): Frag =
    div(cls := "importer-panel importer-panel--guide")(
      div(cls := "importer-panel__head")(
        strong(cls := "importer-panel__title")("Notes behind the review"),
        p(cls := "importer-panel__copy")(summary)
      ),
      div(cls := "account-product-overview-grid")(
        cards.map { card =>
          div(cls := "account-product-overview-card")(
            span(cls := "account-product-overview-kicker")((card \ "title").asOpt[String].getOrElse("Overview")),
            strong((card \ "headline").asOpt[String].getOrElse("")),
            p((card \ "summary").asOpt[String].getOrElse("")),
            span(cls := "account-product-evidence-line")(playerFacingEvidenceLine((card \ "evidence").asOpt[JsObject].getOrElse(Json.obj())))
          )
        }*
      )
    )

  private def renderOpeningIdentity(cards: List[JsObject]): Frag =
    div(cls := "importer-panel importer-panel--guide")(
      div(cls := "importer-panel__head")(
        strong(cls := "importer-panel__title")("Openings you actually reach"),
        p(cls := "importer-panel__copy")(
          "Start from the short map first, then open the details only if you need them."
        )
      ),
      cards.headOption.map { first =>
        frag(
          div(cls := "account-product-opening-summary")(
            strong(openingSummary(cards)),
            span(cls := "account-product-evidence-line")(playerFacingEvidenceLine((first \ "evidence").asOpt[JsObject].getOrElse(Json.obj())))
          ),
          details(cls := "account-product-opening-details")(
            summary("See opening details"),
            div(cls := "account-product-opening-grid")(
              cards.map { card =>
                div(cls := "account-product-opening-card")(
                  strong((card \ "title").asOpt[String].getOrElse("Opening map")),
                  span(cls := "account-product-opening-family")((card \ "openingFamily").asOpt[String].getOrElse("Recent practical structure")),
                  p((card \ "story").asOpt[String].getOrElse("")),
                  span(cls := "account-product-evidence-line")(playerFacingEvidenceLine((card \ "evidence").asOpt[JsObject].getOrElse(Json.obj())))
                )
              }*
            )
          )
        )
      }.getOrElse(
        div(cls := "status-callout")(
          strong("No opening map yet"),
          span("The current sample is still too thin to summarize the openings honestly.")
        )
      )
    )

  private def renderSideToggle(
      state: ProductState,
      side: String,
      patterns: List[JsObject]
  ): Frag =
    val whiteCount = patterns.count(p => (p \ "side").asOpt[String].contains("white"))
    val blackCount = patterns.count(p => (p \ "side").asOpt[String].contains("black"))
    div(cls := "account-product-toggle-row")(
      sideLink(state, "all", s"All (${patterns.size})", side == "all"),
      sideLink(state, "white", s"White ($whiteCount)", side == "white"),
      sideLink(state, "black", s"Black ($blackCount)", side == "black")
    )

  private def renderLeadPattern(
      kind: lila.accountintel.AccountIntel.ProductKind,
      pattern: Option[JsObject]
  ): Frag =
    div(cls := "importer-panel importer-panel--results")(
      div(cls := "importer-panel__head")(
        strong(cls := "importer-panel__title")(
          if kind == lila.accountintel.AccountIntel.ProductKind.MyAccountIntelligenceLite then "Main pattern to fix"
          else "Typical position"
        ),
        p(cls := "importer-panel__copy")(
          if kind == lila.accountintel.AccountIntel.ProductKind.MyAccountIntelligenceLite then
            "Start here. This is the clearest recurring issue in the current sample."
          else
            "Use this position as the board-first reference point for the game plan."
        )
      ),
      pattern.map(renderLeadPatternCard(kind, _)).getOrElse(
        div(cls := "status-callout")(
          strong("No lead pattern yet"),
          span("This sample still needs more evidence before it can anchor the page on one typical position.")
        )
      )
    )

  private def renderPatterns(
      state: ProductState,
      side: String,
      patterns: List[JsObject],
      dropLead: Boolean = false
  ): Frag =
    val visiblePatterns = patterns.filter(pattern => side == "all" || (pattern \ "side").asOpt[String].contains(side))
    val listed = if dropLead then visiblePatterns.drop(1) else visiblePatterns
    val featured = listed.take(2)
    val remaining = listed.drop(2)
    div(cls := "importer-panel importer-panel--results")(
      div(cls := "importer-panel__head")(
        strong(cls := "importer-panel__title")(
          if state.kind == lila.accountintel.AccountIntel.ProductKind.MyAccountIntelligenceLite then "Additional patterns"
          else "More patterns to watch"
        ),
        p(cls := "importer-panel__copy")(
          if state.kind == lila.accountintel.AccountIntel.ProductKind.MyAccountIntelligenceLite then
            "Keep the lead pattern first. Open the other recurring issues only after the first fix is clear."
          else
            "Keep the game plan first. Open these extra patterns only when you want more context."
        )
      ),
      div(cls := "js-ai-side-toggle")(renderSideToggle(state, side, patterns)),
      featured.nonEmpty.option(
        div(cls := "account-product-patterns account-product-patterns--featured")(
          featured.map(renderPatternCard(state.kind, _, featured = true))*
        )
      ).getOrElse(
        div(cls := "status-callout")(
          strong("No side-specific pattern yet"),
          span("This side of the sample is still too thin, so the page is leaning on the opposite color or broader overview cards.")
        )
      ),
      remaining.nonEmpty.option(
        details(cls := "account-product-more-patterns js-ai-more-patterns")(
          summary(
            span("More patterns"),
            span(cls := "account-product-more-patterns__count")(s"${remaining.size} more")
          ),
          div(cls := "account-product-patterns account-product-patterns--extra")(
            remaining.map(renderPatternCard(state.kind, _))*
          )
        )
      )
    )

  private def renderLeadPatternCard(
      kind: lila.accountintel.AccountIntel.ProductKind,
      pattern: JsObject
  ): Frag =
    val anchor = (pattern \ "anchor").asOpt[JsObject]
    div(cls := "account-product-lead")(
      div(cls := "account-product-pattern-head account-product-pattern-head--lead")(
        div(cls := "account-product-pattern-headline")(
          span(cls := "account-product-pattern-side")(displaySideLabel((pattern \ "side").asOpt[String].getOrElse("mixed"))),
          strong((pattern \ "title").asOpt[String].getOrElse("Pattern")),
          span(cls := "account-product-pattern-structure")(
            s"Repeated structure: ${playerFacingStructureLabel(pattern)}"
          )
        ),
        div(cls := "account-product-pattern-meta")(
          span(cls := "account-product-pattern-confidence")(surfaceConfidenceLabel(pattern)),
          span(cls := "account-product-evidence-line")(playerFacingEvidenceLine((pattern \ "evidence").asOpt[JsObject].getOrElse(Json.obj())))
        )
      ),
      anchor.map(renderAnchorCard(kind, (pattern \ "side").asOpt[String], pattern, _, lead = true)).getOrElse(
        div(cls := "account-product-anchor-copy")(
          div(cls := "account-product-anchor-plan")(
            strong("Why this matters"),
            p((pattern \ "summary").asOpt[String].getOrElse(""))
          )
        )
      )
    )

  private def renderPatternCard(
      kind: lila.accountintel.AccountIntel.ProductKind,
      pattern: JsObject,
      featured: Boolean = false
  ): Frag =
    val evidenceGames = (pattern \ "evidenceGames").asOpt[List[JsObject]].getOrElse(Nil)
    val anchor = (pattern \ "anchor").asOpt[JsObject]
    val actions = (pattern \ "actions").asOpt[List[JsObject]].getOrElse(Nil)
    div(cls := s"account-product-pattern${if featured then " is-featured" else ""}")(
      div(cls := "account-product-pattern-head")(
        div(cls := "account-product-pattern-headline")(
          span(cls := "account-product-pattern-side")(displaySideLabel((pattern \ "side").asOpt[String].getOrElse("mixed"))),
          strong((pattern \ "title").asOpt[String].getOrElse("Pattern")),
          span(cls := "account-product-pattern-structure")(
            s"Repeated structure: ${playerFacingStructureLabel(pattern)}"
          )
        ),
        div(cls := "account-product-pattern-meta")(
          span(cls := "account-product-pattern-confidence")(surfaceConfidenceLabel(pattern)),
          span(cls := "account-product-evidence-line")(playerFacingEvidenceLine((pattern \ "evidence").asOpt[JsObject].getOrElse(Json.obj())))
        )
      ),
      div(cls := "account-product-pattern-body")(
        p(cls := "account-product-pattern-summary")((pattern \ "summary").asOpt[String].getOrElse("")),
        anchor.map(renderAnchorCard(kind, (pattern \ "side").asOpt[String], pattern, _)),
        actions.nonEmpty.option(
          div(cls := "account-product-action-list")(
            actions.map(renderActionCard)*
          )
        ),
        evidenceGames.nonEmpty.option(
          details(cls := "account-product-evidence-block")(
            summary("Evidence games"),
            div(cls := "account-product-evidence-grid")(
              evidenceGames.map(renderEvidenceGame)*
            )
          )
        )
      )
    )

  private def renderAnchorCard(
      kind: lila.accountintel.AccountIntel.ProductKind,
      side: Option[String],
      pattern: JsObject,
      anchor: JsObject,
      lead: Boolean = false
  ): Frag =
    val fen = (anchor \ "fen").asOpt[String].getOrElse("")
    val orientation = boardOrientationFor(kind, side)
    div(cls := s"account-product-anchor account-product-anchor--coach${if lead then " is-lead" else ""}")(
      fen.nonEmpty.option(
        div(
          cls := "mini-board mini-board--init account-product-anchor-board",
          attr("data-state") := s"$fen,$orientation,"
        )
      ),
      div(cls := "account-product-anchor-copy")(
        span(cls := "account-product-anchor-kicker")("Typical position"),
        strong((anchor \ "title").asOpt[String].getOrElse("Typical position")),
        div(cls := "account-product-anchor-meta")(
          span(s"Repeated structure: ${playerFacingStructureLabel(pattern)}"),
          span(playerFacingDecisionPoint((anchor \ "moveContext" \ "ply").asOpt[Int].getOrElse(0)))
        ),
        div(cls := "account-product-anchor-plan")(
          strong("Why this matters"),
          p((anchor \ "explanation").asOpt[String].getOrElse((anchor \ "claim").asOpt[String].getOrElse("")))
        ),
        div(cls := "account-product-anchor-plan")(
          strong("What to look for next"),
          p((anchor \ "recommendedPlan" \ "summary").asOpt[String].getOrElse(""))
        )
      ),
    )

  private def renderActionCard(card: JsObject): Frag =
    div(cls := "account-product-action-card")(
      strong((card \ "title").asOpt[String].getOrElse("Action")),
      p((card \ "instruction").asOpt[String].getOrElse("")),
      span((card \ "successMarker").asOpt[String].getOrElse(""))
    )

  private def renderEvidenceGame(game: JsObject): Frag =
    val label = s"${(game \ "white").asOpt[String].getOrElse("?")} vs ${(game \ "black").asOpt[String].getOrElse("?")}"
    val url = (game \ "sourceUrl").asOpt[String]
    val inner =
      div(cls := "account-product-evidence-card")(
        strong(label),
        span(s"${(game \ "result").asOpt[String].getOrElse("-")} • ${(game \ "opening").asOpt[String].getOrElse("Imported game")}"),
        span((game \ "role").asOpt[String].getOrElse("support"))
      )
    url.fold(inner)(gameUrl => a(lila.app.UiEnv.href := gameUrl, target := "_blank", rel := "noopener")(inner))

  private def renderActionPanel(
      kind: lila.accountintel.AccountIntel.ProductKind,
      actionCards: List[JsObject],
      checklist: Option[JsObject],
      notebookUrl: Option[String]
  ): Frag =
    val primaryAction = actionCards.headOption
    val extraActions = actionCards.drop(1)
    val primaryHref =
      if kind == lila.accountintel.AccountIntel.ProductKind.OpponentPrep then notebookUrl
      else Some(routes.StrategicPuzzle.home.url)
    val primaryLabel =
      if kind == lila.accountintel.AccountIntel.ProductKind.OpponentPrep then "Open study notebook"
      else "Try the idea on the board"
    div(cls := "importer-panel importer-panel--guide")(
      div(cls := "importer-panel__head")(
        strong(cls := "importer-panel__title")(if kind == lila.accountintel.AccountIntel.ProductKind.MyAccountIntelligenceLite then "What to look for next" else "Game plan"),
        p(cls := "importer-panel__copy")(
          if kind == lila.accountintel.AccountIntel.ProductKind.MyAccountIntelligenceLite then
            "Use the actions below as the shortest route from the diagnosis to the board."
          else
            "Keep the prep short enough to carry into the next game."
        )
      ),
      primaryAction.map(card => div(cls := "account-product-action-stack account-product-action-stack--primary")(renderActionCard(card))),
      Option.when(extraActions.nonEmpty || checklist.nonEmpty)(
        details(cls := "account-product-action-details")(
          summary(
            if kind == lila.accountintel.AccountIntel.ProductKind.MyAccountIntelligenceLite then "See the full plan"
            else "See the full prep plan"
          ),
          div(cls := "account-product-action-stack")(
            extraActions.map(renderActionCard),
            checklist.toList.map(renderChecklist)
          )
        )
      ),
      div(cls := "account-product-action-cta-row")(
        primaryHref.map(url => a(href := url, cls := "account-product-primary-link")(primaryLabel)),
        notebookUrl
          .filter(_ => kind == lila.accountintel.AccountIntel.ProductKind.MyAccountIntelligenceLite)
          .map(url => a(href := url, cls := "account-product-secondary-link")("Open study notebook")),
        Option.when(kind == lila.accountintel.AccountIntel.ProductKind.OpponentPrep)(
          a(href := routes.StrategicPuzzle.home.url, cls := "account-product-secondary-link")("Try the idea on the board")
        )
      )
    )

  private def renderChecklist(card: JsObject): Frag =
    div(cls := "account-product-checklist")(
      strong((card \ "title").asOpt[String].getOrElse("Checklist")),
      div(cls := "account-product-checklist-items")(
        (card \ "items").asOpt[List[JsObject]].getOrElse(Nil).map { item =>
          div(cls := "account-product-checklist-item")(
            span(cls := "account-product-checklist-priority")((item \ "priority").asOpt[String].getOrElse("medium")),
            div(
              strong((item \ "label").asOpt[String].getOrElse("")),
              (item \ "reason").asOpt[String].map(text => span(text))
            )
          )
        }*
      )
    )

  private def renderExemplarGames(games: List[JsObject]): Frag =
    div(cls := "importer-panel importer-panel--guide")(
      div(cls := "importer-panel__head")(
        strong(cls := "importer-panel__title")("Games that show the pattern"),
        p(cls := "importer-panel__copy")(
          "Use one game first as proof that the pattern is real, then open the rest only if you need more evidence."
        )
      ),
      games.headOption.map(renderExemplarGame),
      Option.when(games.size > 1)(
        details(cls := "account-product-opening-details")(
          summary(s"See ${games.size - 1} more games"),
          div(cls := "account-product-exemplar-list")(
            games.drop(1).map(renderExemplarGame)*
          )
        )
      )
    )

  private def renderExemplarGame(game: JsObject): Frag =
    val meta = (game \ "game").asOpt[JsObject].getOrElse(Json.obj())
    div(cls := "account-product-exemplar-card")(
      strong((game \ "title").asOpt[String].getOrElse("Example game")),
      span(s"${(meta \ "white").asOpt[String].getOrElse("?")} vs ${(meta \ "black").asOpt[String].getOrElse("?")}"),
      p((game \ "whyItMatters").asOpt[String].getOrElse("")),
      p((game \ "takeaway").asOpt[String].getOrElse("")),
      (meta \ "sourceUrl").asOpt[String].map(url => a(href := url, target := "_blank", rel := "noopener")("Open source game"))
    )

  private def renderHistory(state: ProductState): Frag =
    div(cls := "importer-panel importer-panel--guide")(
      div(cls := "importer-panel__head")(
        strong(cls := "importer-panel__title")("History"),
        p(cls := "importer-panel__copy")(
          "Keep the latest report in front, but make older reports easy to reopen and compare."
        )
      ),
          div(cls := "account-product-history")(
        state.history.take(10).zipWithIndex.map { case (job, idx) =>
          val surface = surfaceOf(job)
          div(cls := s"account-product-history-item${if state.selectedJobId.contains(job.id) then " is-selected" else ""}")(
            div(cls := "account-product-history-copy")(
              strong(if idx == 0 then s"${job.status.key} • latest" else job.status.key),
              span(job.requestedAt.toString),
              span(surface.flatMap(s => (s \ "confidence" \ "label").asOpt[String]).getOrElse("pending")),
              span(surface.flatMap(s => (s \ "source" \ "sampledGameCount").asOpt[Int]).map(n => s"$n games").getOrElse("no sample"))
            ),
            div(cls := "account-product-history-actions")(
              a(href := productUrl(job.provider, job.username, job.kind.key, "", Some(job.id)))(
                if state.selectedJobId.contains(job.id) then "Viewing report" else "Open report"
              ),
              surface.isDefined.option(
                button(
                  tpe := "button",
                  cls := "account-product-history-compare js-ai-history-compare",
                  attr("data-job-id") := job.id
                )("Compare")
              )
            )
          )
        }*
      )
    )

  private def renderActiveJobCallout(job: lila.accountintel.AccountIntel.AccountIntelJob): Frag =
    div(cls := "status-callout status-callout--primary account-product-callout")(
      strong(s"${statusTitle(job)} • ${kindLabel(job.kind.key)}"),
      span(stageLabel(job.progressStage)),
      div(cls := "auth-links status-links")(
        a(href := routes.AccountIntel.jobStatusPage(job.id).url, cls := "status-links__primary")("Open build status")
      )
    )

  private def modeLink(
      state: ProductState,
      kind: lila.accountintel.AccountIntel.ProductKind,
      label: String
  ): Frag =
    a(
      href := productUrl(state.provider, state.username, kind.key),
      cls := s"account-product-mode-link js-ai-mode-link${if state.kind == kind then " is-active" else ""}",
      attr("data-kind") := kind.key,
      attr("data-state-url") := surfaceApiUrl(state, kind.key, None)
    )(label)

  private def sideLink(
      state: ProductState,
      side: String,
      label: String,
      active: Boolean
  ): Frag =
    a(
      href := productUrl(state, if side == "all" then "" else side),
      cls := s"account-product-side-link js-ai-side-link${if active then " is-active" else ""}",
      attr("data-side") := side
    )(label)

  private def renderNotebookUtility(
      notebookUrl: Option[String],
      state: ProductState
  ): Frag =
    val publishableJobId = state.selectedJobId.orElse(state.displayedJob.map(_.id))
    div(cls := "importer-panel importer-panel--guide account-product-utility")(
      div(cls := "importer-panel__head")(
        strong(cls := "importer-panel__title")("Study notebook"),
        p(cls := "importer-panel__copy")(
          "Stay on this page for the answer. Create the study notebook only when you want the move tree, chapter flow, and a shareable study artifact."
        )
      ),
      div(cls := "account-product-utility-links")(
        notebookUrl.map(url => a(href := url, cls := "account-product-secondary-link")("Open study notebook")),
        publishableJobId.filter(_ => notebookUrl.isEmpty).map(_ =>
          button(tpe := "button", cls := "account-product-secondary-link js-ai-publish-study")("Create study notebook")
        ),
        div(cls := "copy-me account-product-copy")(
          input(
            tpe := "text",
            readonly := true,
            value := productUrl(state),
            cls := "account-product-copy__value"
          ),
          button(cls := "copy-me__button button-metal")("Copy result link")
        )
      )
    )

  private def renderSupportPanels(
      state: ProductState,
      notebookUrl: Option[String],
      overviewCards: List[JsObject]
  ): Frag =
    div(cls := "account-product-support-tabs js-ai-support-region", attr("data-active-tab") := "study")(
      div(cls := "importer-panel__head")(
        strong(cls := "importer-panel__title")("Support tools"),
        p(cls := "importer-panel__copy")(
          "Keep the lead position and action plan in the rail. Use these tabs only when you need notebook, comparison, history, or supporting notes."
        )
      ),
      div(cls := "account-product-support-tablist", attr("role") := "tablist", attr("aria-label") := "Support tools")(
        supportTabButton("study", "Study notebook", active = true),
        supportTabButton("compare", "Compare reports"),
        supportTabButton("history", "History"),
        overviewCards.nonEmpty.option(supportTabButton("notes", "Notes"))
      ),
      div(cls := "account-product-support-panels")(
        supportPanel("study", active = true)(
          div(cls := "js-ai-utility")(renderNotebookUtility(notebookUrl, state))
        ),
        supportPanel("compare")(
          div(cls := "js-ai-compare")(renderHistoryComparePlaceholder())
        ),
        supportPanel("history")(
          div(cls := "js-ai-history")(renderHistory(state))
        ),
        overviewCards.nonEmpty.option(
          supportPanel("notes")(
            div(cls := "js-ai-overview")(renderOverview(overviewCards, "Notes that support the main diagnosis."))
          )
        )
      )
    )

  private def supportTabButton(tab: String, label: String, active: Boolean = false): Frag =
    button(
      tpe := "button",
      cls := s"account-product-support-tab js-ai-support-tab${if active then " is-active" else ""}",
      attr("data-tab") := tab,
      attr("role") := "tab",
      attr("aria-selected") := active.toString
    )(label)

  private def supportPanel(tab: String, active: Boolean = false)(content: Frag): Frag =
    div(
      cls := s"account-product-support-panel${if active then " is-active" else ""}",
      attr("data-tab") := tab,
      attr("role") := "tabpanel",
      (!active).option(attr("hidden") := "hidden")
    )(content)

  private def renderHistoryComparePlaceholder(): Frag =
    div(cls := "importer-panel importer-panel--guide account-product-compare")(
      div(cls := "importer-panel__head")(
        strong(cls := "importer-panel__title")("Compare reports"),
        p(cls := "importer-panel__copy")(
          "Pick an older report from history to see how the headline, confidence, and main patterns moved."
        )
      ),
      div(cls := "status-callout")(
        strong("No comparison selected"),
        span("Use Compare on a past report to open a compact then-vs-now panel without leaving the page.")
      )
    )

  private def renderRecentRunCard(job: lila.accountintel.AccountIntel.AccountIntelJob): Frag =
    val headline = surfaceOf(job).flatMap(js => (js \ "headline").asOpt[String]).getOrElse(s"@${job.username}")
    a(
      href := productUrl(job.provider, job.username, job.kind.key, "", Some(job.id)),
      cls := "account-product-recent-card"
    )(
      strong(s"@${job.username}"),
      span(s"${providerLabel(job.provider)} • ${kindLabel(job.kind.key)}"),
      p(headline)
    )

  private def renderRecentAccountCard(account: ImportHistory.Account): Frag =
    a(
      href := routes.AccountIntel.product(account.provider, account.username, lila.accountintel.AccountIntel.ProductKind.MyAccountIntelligenceLite.key, "").url,
      cls := "account-product-recent-card"
    )(
      strong(s"@${account.username}"),
      span(providerLabel(account.provider)),
      p(s"Searched ${account.searchCount} time(s) • analyzed ${account.analysisCount} time(s)")
    )

  private def surfaceOf(job: lila.accountintel.AccountIntel.AccountIntelJob): Option[JsObject] =
    job.surfaceJson.flatMap(raw => Try(Json.parse(raw).as[JsObject]).toOption)

  private def normalizeSide(side: String, surface: Option[JsObject]): String =
    val requested = Option(side).map(_.trim.toLowerCase).filter(Set("white", "black", "all"))
    requested.getOrElse:
      surface
        .flatMap(js =>
          (js \ "patterns")
            .asOpt[List[JsObject]]
            .flatMap(_.headOption.flatMap(pattern => (pattern \ "side").asOpt[String]))
        )
        .getOrElse("all")

  private def playerFacingEvidenceLine(evidence: JsObject): String =
    val support = (evidence \ "supportingGames").asOpt[Int].getOrElse(0)
    val total = (evidence \ "totalSampledGames").asOpt[Int].getOrElse(0)
    if total > 0 then s"Seen in $support of $total games" else "Sample still building"

  private def surfaceConfidenceLabel(pattern: JsObject): String =
    val snapshot = (pattern \ "snapshotConfidenceMean").asOpt[Double].getOrElse(0d)
    if snapshot >= 0.75 then "High confidence"
    else if snapshot >= 0.5 then "Medium confidence"
    else "Low confidence"

  private def boardOrientationFor(
      kind: lila.accountintel.AccountIntel.ProductKind,
      side: Option[String]
  ): String =
    side.map(_.trim.toLowerCase).filter(Set("white", "black")).getOrElse:
      if kind == lila.accountintel.AccountIntel.ProductKind.OpponentPrep then "black" else "white"

  private def displaySideLabel(raw: String): String =
    raw.trim.toLowerCase match
      case "white" => "White games"
      case "black" => "Black games"
      case _        => "Mixed sample"

  private def playerFacingStructureLabel(pattern: JsObject): String =
    (pattern \ "structureFamily").asOpt[String].filter(_.trim.nonEmpty).getOrElse("recurring middlegame shape")

  private def playerFacingDecisionPoint(ply: Int): String =
    if ply > 0 then s"Critical choice near move ${Math.max(1, (ply + 1) / 2)}" else "Critical choice repeats early"

  private def openingSummary(cards: List[JsObject]): String =
    cards
      .take(2)
      .map(card => (card \ "title").asOpt[String].filter(_.trim.nonEmpty).getOrElse("Unnamed line"))
      .mkString("Most common: ", ", ", "")

  private def summaryChip(value: String, label: String): Frag =
    div(cls := "importer-summary-chip")(
      strong(value),
      span(label)
    )

  private def landingModeChoice(
      selectedKindKey: String,
      kindKey: String,
      title: String,
      body: String
  ): Frag =
    label(cls := "account-product-mode-choice")(
      input(
        cls := "account-product-mode-choice__input",
        tpe := "radio",
        name := "kind",
        value := kindKey,
        if selectedKindKey == kindKey then checked := true else emptyFrag
      ),
      div(cls := "account-product-mode-choice__copy")(
        strong(title),
        span(body)
      )
    )

  private def noteCard(title: String, copy: String): Frag =
    div(cls := "importer-note-card")(
      strong(title),
      span(copy)
    )

  private def metaCard(label: String, value: String): Frag =
    div(cls := "status-meta-card")(
      strong(label),
      span(value)
    )

  private def metaCardRich(label: String, value: Frag): Frag =
    div(cls := "status-meta-card")(
      strong(label),
      value
    )

  private def statusTitle(job: lila.accountintel.AccountIntel.AccountIntelJob): String =
    job.status match
      case lila.accountintel.AccountIntel.JobStatus.Queued => "Queued for analysis"
      case lila.accountintel.AccountIntel.JobStatus.Running => "Building pattern report"
      case lila.accountintel.AccountIntel.JobStatus.Succeeded => "Pattern report ready"
      case lila.accountintel.AccountIntel.JobStatus.Failed => "Pattern report failed"

  private def statusChipLabel(job: lila.accountintel.AccountIntel.AccountIntelJob): String =
    job.status match
      case lila.accountintel.AccountIntel.JobStatus.Queued => "Waiting"
      case lila.accountintel.AccountIntel.JobStatus.Running => "Running"
      case lila.accountintel.AccountIntel.JobStatus.Succeeded => "Ready"
      case lila.accountintel.AccountIntel.JobStatus.Failed => "Failed"

  private def statusActionCopy(job: lila.accountintel.AccountIntel.AccountIntelJob): String =
    job.status match
      case lila.accountintel.AccountIntel.JobStatus.Succeeded =>
        "The pattern report is ready. Open it now and treat the study notebook as a secondary export."
      case lila.accountintel.AccountIntel.JobStatus.Failed =>
        "The run stopped before the result page was attached. Run it again or switch accounts."
      case _ =>
        "Stay on this page until the review is ready. The account result page unlocks only after the run succeeds."

  private def statusPrimaryAction(job: lila.accountintel.AccountIntel.AccountIntelJob, resultHref: String): Frag =
    job.status match
      case lila.accountintel.AccountIntel.JobStatus.Succeeded =>
        a(href := resultHref, cls := "status-links__primary")("Open pattern report")
      case lila.accountintel.AccountIntel.JobStatus.Failed =>
        postForm(cls := "status-links__form", action := routes.AccountIntel.submit.url)(
          input(tpe := "hidden", name := "provider", value := job.provider),
          input(tpe := "hidden", name := "username", value := job.username),
          input(tpe := "hidden", name := "kind", value := job.kind.key),
          input(tpe := "hidden", name := "force", value := "true"),
          submitButton(cls := "status-links__primary")("Run again")
        )
      case _ =>
        span(cls := "status-links__primary is-disabled", aria("disabled") := "true")("Analysis still running")

  private def stageLabel(stage: String): String =
    stage match
      case "queued" => "Waiting for the worker."
      case "fetching_games" => "Fetching recent public games."
      case "extracting_primitives" => "Extracting recurring structure signals."
      case "publishing_surface" => "Finalizing the shared pattern report."
      case "completed" => "Pattern report created successfully."
      case "failed" => "The job ended with an error."
      case other => other.replace('_', ' ')

  private def providerLabel(provider: String): String =
    provider.trim.toLowerCase match
      case "chesscom" => "Chess.com"
      case _          => "Lichess"

  private def kindLabel(kind: String): String =
    kind match
      case "my_account_intelligence_lite" => "My Patterns"
      case "opponent_prep"                => "Prep for Opponent"
      case other                          => other.replace('_', ' ')

  private case class ProgressStep(index: Int, key: String, label: String, description: String)

  private val progressSteps =
    List(
      ProgressStep(1, "queued", "Waiting to start", "The request is waiting for a worker slot."),
      ProgressStep(2, "fetching_games", "Fetch games", "Recent public games are being collected from the provider."),
      ProgressStep(3, "extracting_primitives", "Extract signals", "Recurring structures, transitions, and anchor candidates are being assembled."),
      ProgressStep(
        4,
        "publishing_surface",
        "Publish surface",
        "The shared pattern report is being finalized and attached to the current run."
      )
    )

  private def stepState(job: lila.accountintel.AccountIntel.AccountIntelJob, key: String): String =
    if job.status == lila.accountintel.AccountIntel.JobStatus.Failed then
      if job.progressStage == key then "failed"
      else if progressSteps.exists(step => step.key == key && progressSteps.indexWhere(_.key == key) < progressSteps.indexWhere(_.key == job.progressStage))
      then "done"
      else "idle"
    else
      val stepIndex = progressSteps.indexWhere(_.key == key)
      val currentIndex = progressSteps.indexWhere(_.key == job.progressStage)
      if job.status == lila.accountintel.AccountIntel.JobStatus.Succeeded then "done"
      else if stepIndex >= 0 && currentIndex >= 0 && stepIndex < currentIndex then "done"
      else if key == job.progressStage then "active"
      else "idle"
