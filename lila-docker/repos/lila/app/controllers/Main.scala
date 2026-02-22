package controllers
import play.api.mvc.*

import lila.app.*
import lila.web.{ StaticContent, WebForms }

/* Chesstory: Analysis-only main controller
 * Removed: round, puzzle, challenge dependencies
 */
final class Main(
    env: Env,
    assetsC: ExternalAssets
) extends LilaController(env):

  private def supportLink(key: String): Option[String] =
    env.config
      .getOptional[String](key)
      .map(_.trim)
      .filter(_.nonEmpty)

  def landing = Open:
    Ok.page(views.pages.landing()
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

  def privacy = Open:
    Ok.page(views.pages.privacy())

  def terms = Open:
    Ok.page(views.pages.terms())

  def source = Open:
    Ok.page(views.pages.openSource())

  def toggleBlindMode = OpenBody:
    bindForm(WebForms.blind)(
      _ => BadRequest,
      (enable, redirect) =>
        Redirect(redirect).withCookies:
          lila.web.WebConfig.blindCookie.make(env.security.lilaCookie)(enable != "0")
    )

  def handlerNotFound(msg: Option[String]) =
    fuccess(NotFound(msg.getOrElse("Not Found")))

  // Captcha removed - not used in Chesstory
  def captchaCheck(@scala.annotation.unused id: GameId) = Anon:
    Ok(1) // Always valid (captcha disabled)

  def webmasters = Open:
    Ok("Webmasters")

  def robots = Anon:
    Ok:
      if env.net.crawlable && req.domain == env.net.domain.value && env.mode.isProd
      then StaticContent.robotsTxt
      else "User-agent: *\nDisallow: /"

  def manifest = Anon:
    JsonOk:
      StaticContent.manifest(env.net)

  def contact = Open:
    Ok("Contact")

  def faq = Open:
    Ok("FAQ")

  def instantChess = Open:
    Redirect(routes.UserAnalysis.index)

  def prometheusMetrics(key: String) = Anon:
    if key == env.web.config.prometheusKey
    then
      lila.web.PrometheusReporter
        .latestScrapeData()
        .fold(NotFound("No metrics found")): data =>
          Ok(data)
    else NotFound("Invalid prometheus key")


  def devAsset(@scala.annotation.unused v: String, path: String, file: String) = assetsC.at(path, file)
