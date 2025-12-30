package lila.security

import play.api.ConfigLoader
import lila.core.net.IpAddress
import lila.common.autoconfig.{ *, given }

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
  def verify(response: String, ip: Option[IpAddress]): Fu[Boolean] = fuTrue
