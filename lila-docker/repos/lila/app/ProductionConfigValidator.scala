package lila.app

import scala.collection.mutable.ListBuffer

import play.api.{ Configuration, Mode }

object ProductionConfigValidator:

  def validate(config: Configuration, mode: Mode): Unit =
    if mode != Mode.Prod then ()
    else
      val errors = ListBuffer.empty[String]

      val domain = config.getOptional[String]("net.domain").map(_.trim).getOrElse("")
      if domain.isEmpty then errors += "net.domain must be set in production."
      else if domain.contains("localhost") || domain.contains("127.0.0.1") || domain == "lila" then
        errors += "net.domain must point at the production hostname in production."
      requireBaseUrl(config, errors)
      requirePublicContactEmail(config, errors)

      List(
        "security.password_reset.secret" -> Set("???"),
        "security.email_confirm.secret"  -> Set("???"),
        "security.email_change.secret"   -> Set("???"),
        "security.login_token.secret"    -> Set("???")
      ).foreach: (path, invalid) =>
        requireNonPlaceholder(config, path, errors, invalid)

      requireMailer(config, errors)
      requireCaptchaIfEnabled(config, errors)

      val problems = errors.toList
      if problems.nonEmpty then
        throw new IllegalStateException(
          s"""Invalid production configuration:
             |${problems.map(err => s"- $err").mkString("\n")}""".stripMargin
        )

  private def requireBaseUrl(config: Configuration, errors: ListBuffer[String]): Unit =
    val baseUrl = config.getOptional[String]("net.base_url").map(_.trim).getOrElse("")
    if baseUrl.isEmpty then errors += "net.base_url must be set in production."
    else if baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1") then
      errors += "net.base_url must not point to localhost in production."
    else if !baseUrl.startsWith("https://") then
      errors += "net.base_url must use https in production."

  private def requireMailer(config: Configuration, errors: ListBuffer[String]): Unit =
    val mock = config.getOptional[Boolean]("mailer.primary.mock").getOrElse(true)
    if mock then errors += "mailer.primary.mock must be false in production."

    requireNonPlaceholder(
      config,
      "mailer.primary.host",
      errors,
      invalid = Set("localhost", "mailpit")
    )
    requirePositiveInt(config, "mailer.primary.port", errors)
    requireBoolean(config, "mailer.primary.tls", errors)
    requireNonPlaceholder(
      config,
      "mailer.primary.user",
      errors,
      invalid = Set("???")
    )
    requireNonPlaceholder(
      config,
      "mailer.primary.password",
      errors,
      invalid = Set("???")
    )

    val sender = config.getOptional[String]("mailer.primary.sender").map(_.trim).getOrElse("")
    if sender.isEmpty then errors += "mailer.primary.sender must be set in production."
    else if sender.toLowerCase.contains("lichess.org") then
      errors += "mailer.primary.sender must be a Chesstory-owned sender, not a lichess.org address."

  private def requirePublicContactEmail(
      config: Configuration,
      errors: ListBuffer[String]
  ): Unit =
    val email = config.getOptional[String]("net.email").map(_.trim).getOrElse("")
    if email.isEmpty then errors += "net.email must be set in production."
    else if !email.contains("@") then
      errors += "net.email must be a valid public contact email in production."

  private def requireCaptchaIfEnabled(
      config: Configuration,
      errors: ListBuffer[String]
  ): Unit =
    val enabled = config.getOptional[Boolean]("security.hcaptcha.enabled").getOrElse(false)
    if enabled then
      requireNonPlaceholder(
        config,
        "security.hcaptcha.secret",
        errors,
        invalid = Set("dummy_secret", "0x0000000000000000000000000000000000000000")
      )
      requireNonPlaceholder(
        config,
        "security.hcaptcha.public.sitekey",
        errors,
        invalid = Set("10000000-ffff-ffff-ffff-000000000001", "f91a151d-73e5-4a95-9d4e-74bfa19bec9d")
      )

  private def requirePositiveInt(
      config: Configuration,
      path: String,
      errors: ListBuffer[String]
  ): Unit =
    config.getOptional[Int](path) match
      case Some(value) if value > 0 => ()
      case _                        => errors += s"$path must be a positive integer in production."

  private def requireBoolean(
      config: Configuration,
      path: String,
      errors: ListBuffer[String]
  ): Unit =
    if config.getOptional[Boolean](path).isEmpty then
      errors += s"$path must be set in production."

  private def requireNonPlaceholder(
      config: Configuration,
      path: String,
      errors: ListBuffer[String],
      invalid: Set[String]
  ): Unit =
    val value = config.getOptional[String](path).map(_.trim).getOrElse("")
    if value.isEmpty then errors += s"$path must be set in production."
    else if invalid.contains(value) then
      errors += s"$path must not use the placeholder value `$value` in production."
