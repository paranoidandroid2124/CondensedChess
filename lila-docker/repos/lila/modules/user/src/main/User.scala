package lila.user

import java.time.Duration


case class TotpToken(value: String) extends AnyVal


object PlayTime:
  extension (p: PlayTime)
    def totalDuration = Duration.ofSeconds(p.total)
    def tvDuration = Duration.ofSeconds(p.tv)
    def nonEmptyTvDuration = Option.when(p.tv > 0)(p.tvDuration)

object nameRules:
  // what new usernames should be like -- now split into further parts for clearer error messages
  val newUsernameRegex = "(?i)[a-z][a-z0-9_-]{0,28}[a-z0-9]".r
  val newUsernamePrefix = "(?i)^[a-z].*".r
  val newUsernameSuffix = "(?i).*[a-z0-9]$".r
  val newUsernameChars = "(?i)^[a-z0-9_-]*$".r
  val newUsernameLetters = "(?i)^([a-z0-9][_-]?)+$".r
