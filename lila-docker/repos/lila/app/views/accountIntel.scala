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
              a(href := routes.Main.landing.url, cls := "logo")("Chesstory")
            ),
            div(cls := "auth-container auth-container--wide")(
              div(cls := "auth-card auth-card--importer auth-card--account-product")(
                div(cls := "account-product-shell")(
                  div(cls := "importer-hero importer-hero--account-product")(
                    div(cls := "importer-hero__eyebrow")("Account Intel"),
                    h1(cls := "auth-title importer-hero__title")("Analyze one account like a paid product, not a utility form."),
                    p(cls := "auth-subtitle importer-hero__subtitle")(
                      "Load a public Chess.com or Lichess account, get a full self-analysis or opponent prep surface, and keep the notebook as a secondary deep-dive."
                    ),
                    div(cls := "importer-summary-strip")(
                      summaryChip("Self", "Repair priorities and recurring structures"),
                      summaryChip("Prep", "Steering targets and pre-game checklist"),
                      summaryChip("History", "Refresh runs instead of starting over")
                    )
                  ),
                  pageError.map(msg => div(cls := "auth-error")(msg)),
                  div(cls := "importer-grid importer-grid--product")(
                    div(cls := "importer-panel importer-panel--intake")(
                      div(cls := "importer-panel__head")(
                        strong(cls := "importer-panel__title")("Find an account"),
                        p(cls := "importer-panel__copy")(
                          "Enter a public username and open its account-intel page. The first screen will show the latest analysis if it already exists."
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
                        div(cls := "importer-panel__hint")(
                          span("Public games only"),
                          span("Async analysis build"),
                          span("Notebook stays secondary")
                        ),
                        button(cls := "auth-submit importer-submit", tpe := "submit")(
                          "Open Account Intel",
                          span(cls := "arrow")(" ->")
                        )
                      )
                    ),
                    div(cls := "importer-side")(
                      div(cls := "importer-panel importer-panel--guide")(
                        div(cls := "importer-panel__head")(
                          strong(cls := "importer-panel__title")("What you get"),
                          p(cls := "importer-panel__copy")(
                            "This product should feel like a dossier. The latest run, top patterns, White/Black split, and rerun controls live in one place."
                          )
                        ),
                        div(cls := "account-product-note-grid")(
                          noteCard("Top 3 patterns", "See the decisions that keep repeating instead of sifting through all 40 games."),
                          noteCard("White / Black split", "Treat repertoire and recurring mistakes as separate surfaces."),
                          noteCard("Evidence drill-in", "Open the supporting games that justify each pattern."),
                          noteCard("Rerun history", "Compare the newest run against older ones without rebuilding your workflow.")
                        )
                      ),
                      recentRuns.nonEmpty.option(
                        div(cls := "importer-panel importer-panel--guide")(
                          div(cls := "importer-panel__head")(
                            strong(cls := "importer-panel__title")("Recent results"),
                            p(cls := "importer-panel__copy")(
                              "Jump straight back into the latest account surfaces you already built."
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

  def product(state: ProductState, side: String, initialState: JsObject): Page =
    val safeSide = normalizeSide(side, state.displayedJob.flatMap(surfaceOf))
    val surface = state.displayedJob.flatMap(surfaceOf)
    val active = state.activeJob
    Page(s"@${state.username} • Account Intel")
      .css("auth")
      .wrap: _ =>
        main(cls := "auth-page auth-page--importer")(
          div(cls := "landing-container")(
            div(cls := "landing-header")(
              a(href := routes.Main.landing.url, cls := "logo")("Chesstory")
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

  def status(job: lila.accountintel.AccountIntel.AccountIntelJob): Page =
    val refreshMs =
      if job.status == lila.accountintel.AccountIntel.JobStatus.Queued || job.status == lila.accountintel.AccountIntel.JobStatus.Running
      then 4000
      else 0
    val resultHref = productUrl(job.provider, job.username, job.kind.key, "", Some(job.id))
    Page("Building Account Intel - Chesstory")
      .css("auth")
      .wrap: _ =>
        main(cls := "auth-page auth-page--importer")(
          div(cls := "landing-container")(
            div(cls := "landing-header")(
              a(href := routes.Main.landing.url, cls := "logo")("Chesstory")
            ),
            div(cls := "auth-container auth-container--wide")(
              div(cls := "auth-card auth-card--importer auth-card--status")(
                div(cls := "status-shell")(
                  div(cls := "importer-hero importer-hero--status")(
                    div(cls := "importer-hero__eyebrow")("Account Intel"),
                    h1(cls := "auth-title importer-hero__title")(statusTitle(job)),
                    p(cls := "auth-subtitle importer-hero__subtitle")(
                      "This run is building in the background. Keep this page open while the account surface is being attached."
                    ),
                    div(cls := "importer-summary-strip")(
                      summaryChip(s"@${job.username}", providerLabel(job.provider)),
                      summaryChip(kindLabel(job.kind.key), "Mode"),
                      summaryChip(job.status.key, stageLabel(job.progressStage))
                    )
                  ),
                  div(cls := "status-grid")(
                    div(cls := "importer-panel importer-panel--status")(
                      div(cls := "importer-panel__head")(
                        strong(cls := "importer-panel__title")("Build progress"),
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
                        metaCard("Job ID", job.id),
                        metaCard("Current stage", stageLabel(job.progressStage))
                      )
                    ),
                    div(cls := "importer-panel importer-panel--status-side")(
                      div(cls := "importer-panel__head")(
                        strong(cls := "importer-panel__title")("Next action"),
                        p(cls := "importer-panel__copy")(
                          "The primary destination is now the account result page. The notebook stays a secondary deep-dive."
                        )
                      ),
                      div(cls := "status-callout status-callout--primary")(
                        strong(statusTitle(job)),
                        span(
                          if job.status == lila.accountintel.AccountIntel.JobStatus.Failed then
                            "The build stopped before the account surface was attached."
                          else if job.status == lila.accountintel.AccountIntel.JobStatus.Succeeded then
                            "The account surface is ready to open."
                          else "The worker is still processing the account. This page auto-refreshes while the run is active."
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
                        a(href := resultHref, cls := "status-links__primary")("Open account surface"),
                        job.notebookUrl.map(url => a(href := url)("Open notebook")),
                        a(href := routes.AccountIntel.landing("", "").url)("Back to Account Intel")
                      )
                    )
                  ),
                  (refreshMs > 0).option(
                    script(raw(s"window.setTimeout(function () { window.location.reload(); }, $refreshMs);"))
                  ),
                  (refreshMs == 0 && job.status == lila.accountintel.AccountIntel.JobStatus.Succeeded).option(
                    script(raw(s"window.setTimeout(function () { window.location.assign('$resultHref'); }, 1200);"))
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
    val summary = (surface \ "summary").asOpt[String].getOrElse("")
    val confidence = (surface \ "confidence" \ "label").asOpt[String].getOrElse("weak")
    val sampled = (surface \ "source" \ "sampledGameCount").asOpt[Int].getOrElse(0)
    val generatedAt = (surface \ "generatedAt").asOpt[String].getOrElse("")
    val patterns = (surface \ "patterns").asOpt[List[JsObject]].getOrElse(Nil)
    val visiblePatterns = patterns.filter(pattern => side == "all" || (pattern \ "side").asOpt[String].contains(side))
    val overviewCards = (surface \ "overview" \ "cards").asOpt[List[JsObject]].getOrElse(Nil)
    val openingCards = (surface \ "openingCards").asOpt[List[JsObject]].getOrElse(Nil)
    val exemplarGames = (surface \ "exemplarGames").asOpt[List[JsObject]].getOrElse(Nil)
    val actionCards = (surface \ "actions").asOpt[List[JsObject]].getOrElse(Nil)
    val checklist = (surface \ "checklist").asOpt[JsObject]
    val notebookUrl = state.displayedJob.flatMap(_.notebookUrl).orElse(state.latestSuccessful.flatMap(_.notebookUrl))
    div(
      cls := "account-product-shell js-account-intel-product",
      attr("data-state-url") := surfaceApiUrl(state),
      attr("data-side") := side,
      attr("data-page-base-url") := s"/account-intel/${state.provider}/${state.username}",
      attr("data-landing-url") := routes.AccountIntel.landing("", "").url
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
            modeLink(state, lila.accountintel.AccountIntel.ProductKind.MyAccountIntelligenceLite, "My Account"),
            modeLink(state, lila.accountintel.AccountIntel.ProductKind.OpponentPrep, "Opponent Prep")
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
      div(cls := "account-product-body")(
        div(cls := "account-product-main")(
          div(cls := "js-ai-overview")(renderOverview(overviewCards, summary)),
          div(cls := "js-ai-openings")(renderOpeningIdentity(openingCards)),
          div(cls := "js-ai-side-toggle")(renderSideToggle(state, side, patterns)),
          div(cls := "js-ai-patterns")(renderPatterns(visiblePatterns)),
          div(cls := "js-ai-actions")(renderActionPanel(state.kind, actionCards, checklist))
        ),
        div(cls := "account-product-side")(
          div(cls := "js-ai-utility")(renderNotebookUtility(notebookUrl, state)),
          div(cls := "js-ai-compare")(renderHistoryComparePlaceholder()),
          div(cls := "js-ai-exemplars")(renderExemplarGames(exemplarGames)),
          div(cls := "js-ai-history")(renderHistory(state))
        )
      ),
      div(cls := "js-ai-state-source", attr("data-initial-state") := Json.stringify(initialState))
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
          "No finished account surface is attached yet. Start with a fresh run, then come back here for the standalone result page."
        )
      ),
      activeJob.map(renderActiveJobCallout),
      div(cls := "account-product-empty")(
        div(cls := "status-callout status-callout--primary")(
          strong("Analyze this account"),
          span("Start with My Account or Opponent Prep. The finished result will live here, while the notebook stays secondary.")
        ),
        div(cls := "account-product-mode-switch")(
          modeLink(state, lila.accountintel.AccountIntel.ProductKind.MyAccountIntelligenceLite, "My Account"),
          modeLink(state, lila.accountintel.AccountIntel.ProductKind.OpponentPrep, "Opponent Prep")
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
          a(href := routes.AccountIntel.landing("", "").url)("Back to Account Intel")
        )
      )
    )

  private def renderOverview(cards: List[JsObject], summary: String): Frag =
    div(cls := "importer-panel importer-panel--guide")(
      div(cls := "importer-panel__head")(
        strong(cls := "importer-panel__title")("Overview"),
        p(cls := "importer-panel__copy")(summary)
      ),
      div(cls := "account-product-overview-grid")(
        cards.map { card =>
          div(cls := "account-product-overview-card")(
            span(cls := "account-product-overview-kicker")((card \ "title").asOpt[String].getOrElse("Overview")),
            strong((card \ "headline").asOpt[String].getOrElse("")),
            p((card \ "summary").asOpt[String].getOrElse("")),
            renderEvidenceLine((card \ "evidence").asOpt[JsObject].getOrElse(Json.obj()))
          )
        }*
      )
    )

  private def renderOpeningIdentity(cards: List[JsObject]): Frag =
    div(cls := "importer-panel importer-panel--guide")(
      div(cls := "importer-panel__head")(
        strong(cls := "importer-panel__title")("Opening identity"),
        p(cls := "importer-panel__copy")(
          "Keep opening labels honest and split by side. This page is about what the account repeatedly enters, not one vanity line."
        )
      ),
      div(cls := "account-product-opening-grid")(
        cards.map { card =>
          div(cls := "account-product-opening-card")(
            strong((card \ "title").asOpt[String].getOrElse("Opening map")),
            span(cls := "account-product-opening-family")((card \ "openingFamily").asOpt[String].getOrElse("Recent practical structure")),
            p((card \ "story").asOpt[String].getOrElse("")),
            renderEvidenceLine((card \ "evidence").asOpt[JsObject].getOrElse(Json.obj()))
          )
        }*
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

  private def renderPatterns(patterns: List[JsObject]): Frag =
    div(cls := "importer-panel importer-panel--results")(
      div(cls := "importer-panel__head")(
        strong(cls := "importer-panel__title")("Top patterns"),
        p(cls := "importer-panel__copy")(
          "These are the account-level patterns that justify the product, not just a notebook chapter list."
        )
      ),
      patterns.nonEmpty.option(
        div(cls := "account-product-patterns")(
          patterns.map(renderPatternCard)*
        )
      ).getOrElse(
        div(cls := "status-callout")(
          strong("No side-specific pattern yet"),
          span("This side of the sample is still too thin, so the page is leaning on the opposite color or broader overview cards.")
        )
      )
    )

  private def renderPatternCard(pattern: JsObject): Frag =
    val evidenceGames = (pattern \ "evidenceGames").asOpt[List[JsObject]].getOrElse(Nil)
    val anchor = (pattern \ "anchor").asOpt[JsObject]
    val actions = (pattern \ "actions").asOpt[List[JsObject]].getOrElse(Nil)
    details(cls := "account-product-pattern")(
      summary(
        div(cls := "account-product-pattern-head")(
          div(
            span(cls := "account-product-pattern-side")((pattern \ "side").asOpt[String].getOrElse("mixed")),
            strong((pattern \ "title").asOpt[String].getOrElse("Pattern")),
            span(cls := "account-product-pattern-structure")((pattern \ "structureFamily").asOpt[String].getOrElse(""))
          ),
          div(cls := "account-product-pattern-meta")(
            renderEvidenceLine((pattern \ "evidence").asOpt[JsObject].getOrElse(Json.obj())),
            span(s"confidence ${(pattern \ "snapshotConfidenceMean").asOpt[Double].getOrElse(0d)}")
          )
        )
      ),
      div(cls := "account-product-pattern-body")(
        p(cls := "account-product-pattern-summary")((pattern \ "summary").asOpt[String].getOrElse("")),
        anchor.map(renderAnchorCard),
        actions.nonEmpty.option(
          div(cls := "account-product-action-list")(
            actions.map(renderActionCard)*
          )
        ),
        evidenceGames.nonEmpty.option(
          div(cls := "account-product-evidence-block")(
            strong("Evidence games"),
            div(cls := "account-product-evidence-grid")(
              evidenceGames.map(renderEvidenceGame)*
            )
          )
        )
      )
    )

  private def renderAnchorCard(anchor: JsObject): Frag =
    div(cls := "account-product-anchor")(
      strong((anchor \ "title").asOpt[String].getOrElse("Anchor position")),
      p((anchor \ "explanation").asOpt[String].getOrElse((anchor \ "claim").asOpt[String].getOrElse(""))),
      div(cls := "account-product-anchor-meta")(
        span((anchor \ "claim").asOpt[String].getOrElse("")),
        span(s"ply ${(anchor \ "moveContext" \ "ply").asOpt[Int].getOrElse(0)}")
      ),
      div(cls := "account-product-anchor-plan")(
        strong("What to do next"),
        p((anchor \ "recommendedPlan" \ "summary").asOpt[String].getOrElse(""))
      )
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
      checklist: Option[JsObject]
  ): Frag =
    div(cls := "importer-panel importer-panel--guide")(
      div(cls := "importer-panel__head")(
        strong(cls := "importer-panel__title")(if kind == lila.accountintel.AccountIntel.ProductKind.MyAccountIntelligenceLite then "What to do next" else "How to steer"),
        p(cls := "importer-panel__copy")(
          if kind == lila.accountintel.AccountIntel.ProductKind.MyAccountIntelligenceLite then
            "Use the product surface as a repair sheet. The notebook is still there, but the next action should already be obvious here."
          else
            "Prep should end in steering decisions you can actually carry into the next game."
        )
      ),
      div(cls := "account-product-action-stack")(
        actionCards.map(renderActionCard),
        checklist.toList.map(renderChecklist)
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
        strong(cls := "importer-panel__title")("Representative games"),
        p(cls := "importer-panel__copy")(
          "One or two games should explain why the selected patterns deserve attention."
        )
      ),
      div(cls := "account-product-exemplar-list")(
        games.map { game =>
          val meta = (game \ "game").asOpt[JsObject].getOrElse(Json.obj())
          div(cls := "account-product-exemplar-card")(
            strong((game \ "title").asOpt[String].getOrElse("Exemplar")),
            span(s"${(meta \ "white").asOpt[String].getOrElse("?")} vs ${(meta \ "black").asOpt[String].getOrElse("?")}"),
            p((game \ "whyItMatters").asOpt[String].getOrElse("")),
            p((game \ "takeaway").asOpt[String].getOrElse("")),
            (meta \ "sourceUrl").asOpt[String].map(url => a(href := url, target := "_blank", rel := "noopener")("Open source game"))
          )
        }*
      )
    )

  private def renderHistory(state: ProductState): Frag =
    div(cls := "importer-panel importer-panel--guide")(
      div(cls := "importer-panel__head")(
        strong(cls := "importer-panel__title")("Run history"),
        p(cls := "importer-panel__copy")(
          "Keep the latest result in front, but make reruns and older runs easy to compare."
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
                if state.selectedJobId.contains(job.id) then "Viewing run" else "Open run"
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
    div(cls := "importer-panel importer-panel--guide account-product-utility")(
      div(cls := "importer-panel__head")(
        strong(cls := "importer-panel__title")("Deep dive"),
        p(cls := "importer-panel__copy")(
          "Stay on this page for the answer. Open the notebook only when you want the move tree, chapter flow, and a shareable study artifact."
        )
      ),
      div(cls := "account-product-utility-links")(
        notebookUrl.map(url => a(href := url, cls := "account-product-secondary-link")("Open notebook")),
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

  private def renderHistoryComparePlaceholder(): Frag =
    div(cls := "importer-panel importer-panel--guide account-product-compare")(
      div(cls := "importer-panel__head")(
        strong(cls := "importer-panel__title")("Compare runs"),
        p(cls := "importer-panel__copy")(
          "Pick an older run from history to see how the headline, confidence, and top patterns moved."
        )
      ),
      div(cls := "status-callout")(
        strong("No comparison selected"),
        span("Use Compare on a past run to open a compact then-vs-now panel without leaving the page.")
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

  private def renderEvidenceLine(evidence: JsObject): Frag =
    val support = (evidence \ "supportingGames").asOpt[Int].getOrElse(0)
    val total = (evidence \ "totalSampledGames").asOpt[Int].getOrElse(0)
    val strength = (evidence \ "strength").asOpt[String].getOrElse("weak")
    span(cls := "account-product-evidence-line")(s"$support/$total games • $strength")

  private def summaryChip(value: String, label: String): Frag =
    div(cls := "importer-summary-chip")(
      strong(value),
      span(label)
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

  private def statusTitle(job: lila.accountintel.AccountIntel.AccountIntelJob): String =
    job.status match
      case lila.accountintel.AccountIntel.JobStatus.Queued => "Queued"
      case lila.accountintel.AccountIntel.JobStatus.Running => "Building account surface"
      case lila.accountintel.AccountIntel.JobStatus.Succeeded => "Account surface ready"
      case lila.accountintel.AccountIntel.JobStatus.Failed => "Account build failed"

  private def stageLabel(stage: String): String =
    stage match
      case "queued" => "Waiting for the worker."
      case "fetching_games" => "Fetching recent public games."
      case "extracting_primitives" => "Extracting recurring structure signals."
      case "creating_notebook" => "Attaching the notebook and account surface."
      case "completed" => "Account surface created successfully."
      case "failed" => "The job ended with an error."
      case other => other.replace('_', ' ')

  private def providerLabel(provider: String): String =
    provider.trim.toLowerCase match
      case "chesscom" => "Chess.com"
      case _          => "Lichess"

  private def kindLabel(kind: String): String =
    kind match
      case "my_account_intelligence_lite" => "My Account"
      case "opponent_prep"                => "Opponent Prep"
      case other                          => other.replace('_', ' ')

  private case class ProgressStep(index: Int, key: String, label: String, description: String)

  private val progressSteps =
    List(
      ProgressStep(1, "queued", "Queued", "The request is waiting for a worker slot."),
      ProgressStep(2, "fetching_games", "Fetch games", "Recent public games are being collected from the provider."),
      ProgressStep(3, "extracting_primitives", "Extract signals", "Recurring structures, transitions, and anchor candidates are being assembled."),
      ProgressStep(4, "creating_notebook", "Attach result", "The account surface and notebook are being created and linked.")
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
