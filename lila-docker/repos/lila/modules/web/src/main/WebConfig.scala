package lila.web

import play.api.{ Configuration, ConfigLoader }

import lila.common.config.given
import lila.core.config.*
import lila.core.security.LilaCookie

final class WebConfig(
    val apiToken: Secret,
    val influxEventEndpoint: String,
    val influxEventEnv: String,
    val prometheusKey: String
)

object WebConfig:

  object blindCookie:
    val name = "mBzamRgfXgRBSnXB"
    val maxAge = 365.days
    def make(lilaCookie: LilaCookie)(enable: Boolean) = lilaCookie.cookie(
      name,
      if enable then "1" else "",
      maxAge = maxAge.toSeconds.toInt.some,
      httpOnly = true.some
    )

  def loadFrom(c: Configuration) =
    WebConfig(
      c.get[Secret]("api.token"),
      c.getOptional[String]("api.influx_event.endpoint").getOrElse(""),
      c.getOptional[String]("api.influx_event.env").getOrElse(""),
      c.getOptional[String]("kamon.prometheus.lilaKey").getOrElse("")
    )

  def analyseEndpoints(c: Configuration) =
    lila.ui.AnalyseEndpoints(
      explorer = c.getOptional[String]("explorer.endpoint").getOrElse(""),
      tablebase = c.getOptional[String]("explorer.tablebase_endpoint").getOrElse(""),
      externalEngine = c.getOptional[String]("externalEngine.endpoint").getOrElse("")
    )

  def netConfig(c: Configuration) = NetConfig(
    domain = c.get[NetDomain]("net.domain"),
    prodDomain = c.get[NetDomain]("net.prodDomain"),
    baseUrl = c.get[BaseUrl]("net.base_url"),
    assetDomain = c.get[AssetDomain]("net.asset.domain"),
    assetBaseUrl = c.get[AssetBaseUrl]("net.asset.base_url"),
    stageBanner = c.get[Boolean]("net.stage.banner"),
    siteName = c.get[String]("net.site.name"),
    socketDomains = c.getOptional[List[String]]("net.socket.domains").getOrElse(Nil),
    socketAlts = c.getOptional[List[String]]("net.socket.alts").getOrElse(Nil),
    crawlable = c.get[Boolean]("net.crawlable"),
    rateLimit = c.get[RateLimit]("net.ratelimit"),
    email = c.get[EmailAddress]("net.email"),
    logRequests = c.get[Boolean]("net.http.log")
  )

  final class LilaVersion(val date: String, val commit: String, val message: String)

  def lilaVersion(c: Configuration): Option[LilaVersion] = (
    c.getOptional[String]("app.version.date"),
    c.getOptional[String]("app.version.commit"),
    c.getOptional[String]("app.version.message")
  ).mapN(LilaVersion.apply)
