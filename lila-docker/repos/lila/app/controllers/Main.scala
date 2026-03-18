package controllers
import play.api.mvc.*
import play.api.libs.json.Json

import lila.app.*
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
    Ok.page(views.pages.landing(journalContent.latestPost)
      .flag(_.noHeader)
      .flag(_.fullScreen))

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

  def healthz = Anon:
    Ok("ok").toFuccess

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
