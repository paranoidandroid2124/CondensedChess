package controllers
import play.api.mvc.*
import play.api.libs.json.Json
import scala.util.control.NonFatal

import lila.app.*
import lila.core.study.StudyOrder
import lila.core.config.CollName
import lila.db.dsl.*
import lila.web.{ StaticContent, WebForms }

/* Chesstory: Analysis-only main controller
 * Removed: round, puzzle, challenge dependencies
 */
final class Main(
    env: Env,
    assetsC: ExternalAssets
) extends LilaController(env):

  private val journalContent = JournalContent(env.getFile, env.memo.markdown)

  private def supportLink(key: String): Option[String] =
    env.config
      .getOptional[String](key)
      .map(_.trim)
      .filter(_.nonEmpty)

  private def publicContactEmail: Option[String] =
    Option(env.net.email.value).map(_.trim).filter(_.nonEmpty)

  def landing = Open:
    if ctx.isAuth then Redirect(routes.Main.home).toFuccess
    else
      Ok.page(views.pages.landing(journalContent.latestPost)
        .flag(_.noHeader)
        .flag(_.fullScreen))

  def home = Auth { ctx ?=> me ?=>
    for
      summary <- env.analyse.importHistory.recentSummary(me.userId)
      recentPatternReports <- env.accountintel.api.recentSuccessful(me.userId, limit = 3)
      notebookPager <- env.study.pager.mine(StudyOrder.updated, page = 1)(using me)
      recentNotebooks = notebookPager.currentPageResults.take(3).toList
      continueCard =
        summary.analyses.headOption
          .map(Main.HomeContinueCard.Analysis.apply)
          .orElse(recentPatternReports.headOption.map(Main.HomeContinueCard.PatternReport.apply))
          .orElse(recentNotebooks.headOption.map(Main.HomeContinueCard.Notebook.apply))
          .getOrElse(Main.HomeContinueCard.Starter)
      data = Main.HomePageData(
        continueCard = continueCard,
        quickActions = List(
          Main.HomeQuickAction(
            label = "Start",
            title = "Start from PGN",
            copy = "Paste a game and open Guided Review without rebuilding your entry flow.",
            href = routes.Importer.importGame.url
          ),
          Main.HomeQuickAction(
            label = "Board",
            title = "Open full analysis",
            copy = "Go straight to the board, tree, engine, and explorer.",
            href = s"${routes.UserAnalysis.index.url}?mode=raw"
          ),
          Main.HomeQuickAction(
            label = "Patterns",
            title = "See account patterns",
            copy = "Open My Patterns or Prep for Opponent from a public account.",
            href = routes.AccountIntel.landing("", "").url
          ),
          Main.HomeQuickAction(
            label = "Puzzle",
            title = "Open Strategic Puzzle",
            copy = "Open the current live strategic puzzle without promising saved progress.",
            href = routes.StrategicPuzzle.home.url
          )
        ),
        recentAnalyses = summary.analyses.take(4),
        recentPatternReports = recentPatternReports,
        recentAccounts = summary.accounts.take(4),
        recentNotebooks = recentNotebooks
      )
      result <- Ok.page(views.pages.home(data))
    yield result
  }

  def support = Open:
    Ok.page(
      views.pages.support(
        patreon = supportLink("support.links.patreon"),
        githubSponsors = supportLink("support.links.githubSponsors"),
        buyMeACoffee = supportLink("support.links.buyMeACoffee")
      )
    )

  def plan = Open:
    Redirect(routes.Main.support, MOVED_PERMANENTLY).toFuccess

  def privacy = Open:
    Ok.page(views.pages.privacy(publicContactEmail))

  def terms = Open:
    Ok.page(views.pages.terms(publicContactEmail))

  def source = Open:
    Ok.page(views.pages.openSource())

  def journal = Open:
    Ok.page(views.pages.journal(journalContent.all, None))

  def journalPost(slug: String) = Open:
    journalContent.bySlug(slug) match
      case Some(post) => Ok.page(views.pages.journal(journalContent.all, Some(post)))
      case None       => NotFound.page(views.site.message.notFound(Some("Journal post not found.")))

  def strategicPuzzleDemo = Open:
    Ok.page(views.pages.strategicPuzzleDemo.apply)

  def toggleBlindMode = OpenBody:
    bindForm(WebForms.blind)(
      _ => BadRequest,
      (enable, redirect) =>
        Redirect(redirect).withCookies:
          lila.web.WebConfig.blindCookie.make(env.security.lilaCookie)(enable != "0")
    )

  def handlerNotFound(msg: Option[String]) =
    fuccess(NotFound(msg.getOrElse("Not Found")))

  def robots = Anon:
    Ok:
      if env.net.crawlable && req.domain == env.net.domain.value && env.mode.isProd
      then StaticContent.robotsTxt
      else "User-agent: *\nDisallow: /"

  def manifest = Anon:
    JsonOk:
      StaticContent.manifest(env.net)

  def contact = Open:
    Ok.page(views.pages.contact(publicContactEmail))

  def livez = Anon:
    Ok("ok").toFuccess

  def healthz = Anon:
    healthChecks.map: checks =>
      val ready = Main.isReady(checks)
      (if ready then Ok else ServiceUnavailable)(
        Json.obj(
          "status" -> (if ready then "ready" else "unready"),
          "checks" -> checks.map(_.json)
        )
      )

  def prometheusMetrics(key: String) = Anon:
    if key == env.web.config.prometheusKey
    then
      lila.web.PrometheusReporter
        .latestScrapeData()
        .fold(NotFound("No metrics found")): data =>
          Ok(data)
    else NotFound("Invalid prometheus key")

  def commentaryOps(key: String, limit: Int) = Anon:
    if key == env.web.config.prometheusKey
    then Ok(Json.toJson(env.llm.api.commentaryOpsSnapshot(limit.max(1).min(50))))
    else NotFound("Invalid commentary ops key")


  def devAsset(@scala.annotation.unused v: String, path: String, file: String) = assetsC.at(path, file)

  private def healthChecks =
    val requiredInProd = env.mode.isProd
    for
      mongoReady <- mongoHealth
      bindingStatuses <- env.openBetaBindings.snapshot
    yield
      List(
        Main.HealthCheck(
          name = "mongo",
          ok = mongoReady,
          required = true,
          detail = if mongoReady then "query_ok" else "query_failed"
        ),
        Main.mailerCheck(env.config, env.mailer.mailer.canSend, required = requiredInProd),
        Main.llmCheck(
          openAiEnabled = env.llm.openAiClient.isEnabled,
          geminiEnabled = env.llm.geminiClient.isEnabled,
          required = requiredInProd
        )
      ) ++ bindingStatuses.flatMap(Main.bindingHealthCheck(_, requiredInProd))

  private def mongoHealth =
    env.mongo
      .mainDb(CollName("announce"))
      .find($empty)
      .one[Bdoc]
      .map(_ => true)
      .recover { case NonFatal(_) => false }

object Main:

  sealed trait HomeContinueCard
  object HomeContinueCard:
    final case class Analysis(entry: lila.analyse.ImportHistory.Analysis) extends HomeContinueCard
    final case class PatternReport(job: lila.accountintel.AccountIntel.AccountIntelJob) extends HomeContinueCard
    final case class Notebook(entry: lila.study.Study.WithChaptersAndLiked) extends HomeContinueCard
    case object Starter extends HomeContinueCard

  final case class HomeQuickAction(
      label: String,
      title: String,
      copy: String,
      href: String
  )

  final case class HomePageData(
      continueCard: HomeContinueCard,
      quickActions: List[HomeQuickAction],
      recentAnalyses: List[lila.analyse.ImportHistory.Analysis],
      recentPatternReports: List[lila.accountintel.AccountIntel.AccountIntelJob],
      recentAccounts: List[lila.analyse.ImportHistory.Account],
      recentNotebooks: List[lila.study.Study.WithChaptersAndLiked]
  )

  final case class HealthCheck(
      name: String,
      ok: Boolean,
      required: Boolean,
      detail: String
  ):
    def json =
      Json.obj(
        "name" -> name,
        "ok" -> ok,
        "required" -> required,
        "detail" -> detail
      )

  def isReady(checks: Iterable[HealthCheck]): Boolean =
    checks.forall(check => !check.required || check.ok)

  def mailerCheck(config: play.api.Configuration, canSend: Boolean, required: Boolean): HealthCheck =
    val mock = config.getOptional[Boolean]("mailer.primary.mock").getOrElse(true)
    val missing =
      List(
        "host" -> config.getOptional[String]("mailer.primary.host"),
        "user" -> config.getOptional[String]("mailer.primary.user"),
        "password" -> config.getOptional[String]("mailer.primary.password"),
        "sender" -> config.getOptional[String]("mailer.primary.sender")
      ).collect:
        case (label, value) if value.forall(_.trim.isEmpty) => label
    val ok = !mock && canSend && missing.isEmpty
    val detail =
      if mock then "mock_enabled"
      else if !canSend then "disabled_by_live_setting"
      else if missing.nonEmpty then s"missing:${missing.mkString(",")}"
      else "configured"
    HealthCheck("mailer", ok = ok, required = required, detail = detail)

  def llmCheck(openAiEnabled: Boolean, geminiEnabled: Boolean, required: Boolean): HealthCheck =
    val ok = openAiEnabled || geminiEnabled
    val detail =
      if openAiEnabled && geminiEnabled then "openai+gemini"
      else if openAiEnabled then "openai"
      else if geminiEnabled then "gemini"
      else "disabled"
    HealthCheck("llm", ok = ok, required = required, detail = detail)

  def bindingHealthCheck(
      status: OpenBetaBindingStatus,
      requiredInProd: Boolean
  ): Option[HealthCheck] =
    Option.unless(status.spec.readinessClass == "none"):
      HealthCheck(
        name = healthCheckName(status.spec.env),
        ok = status.readyOk,
        required = requiredInProd && status.readinessRequired,
        detail = status.detail
      )

  private def healthCheckName(env: String) =
    env match
      case "REDIS_URI" => "redis"
      case "PROMETHEUS_KEY" => "metrics"
      case "EXPLORER_API_BASE" => "explorer"
      case "TABLEBASE_API_BASE" => "tablebase"
      case "LICHESS_IMPORT_API_BASE" => "lichess_import_api"
      case "LICHESS_WEB_BASE" => "lichess_web"
      case "CHESSCOM_API_BASE" => "chesscom_api"
      case "EXTERNAL_ENGINE_ENDPOINT" => "external_engine"
      case "ACCOUNT_INTEL_DISPATCH_BASE_URL" => "accountintel_dispatch"
      case "ACCOUNT_INTEL_SELECTIVE_EVAL_ENDPOINT" => "accountintel_selective_eval"
      case "GIF_EXPORT_URL" => "gif_export"
      case other => other.toLowerCase
