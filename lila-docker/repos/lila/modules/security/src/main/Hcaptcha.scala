package lila.security

import play.api.ConfigLoader
import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.StandaloneWSClient
import lila.core.net.IpAddress
import lila.common.autoconfig.AutoConfig
import lila.core.lilaism.Core.*
import scala.concurrent.duration.*

trait Hcaptcha extends lila.core.security.Hcaptcha

object Hcaptcha:
  case class Config(
      enabled: Boolean,
      endpoint: String,
      secret: String,
      public: PublicConfig
  )
  case class PublicConfig(sitekey: String)
  given ConfigLoader[PublicConfig] = AutoConfig.loader
  given ConfigLoader[Config] = AutoConfig.loader

final class HcaptchaSkip extends Hcaptcha:
  def verify(response: String, ip: Option[IpAddress]): Fu[Boolean] = Future.successful(true)

final class HcaptchaHttp(
    ws: StandaloneWSClient,
    config: Hcaptcha.Config
)(using Executor)
    extends Hcaptcha:

  private val logger = lila.log("security.hcaptcha")

  def verify(response: String, ip: Option[IpAddress]): Fu[Boolean] =
    if !config.enabled then Future.successful(true)
    else
      val token = response.trim
      if token.isEmpty then Future.successful(false)
      else
        val fields = Map(
          "secret" -> Seq(config.secret),
          "response" -> Seq(token)
        ) ++ ip.map(addr => "remoteip" -> Seq(addr.value))
        ws.url(config.endpoint)
          .withRequestTimeout(2.seconds)
          .post(fields)
          .map: res =>
            val body = res.body[String]
            if res.status == 200 then
              scala.util.Try((Json.parse(body) \ "success").asOpt[Boolean].contains(true)).getOrElse(false)
            else
              logger.warn(s"hCaptcha verify failed: ${res.status} ${body.take(120)}")
              false
          .recover { case e =>
            logger.warn(s"hCaptcha verify error: ${e.getMessage}")
            false
          }
