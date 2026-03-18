package lila.common

import play.api.mvc.{ Cookie, DiscardingCookie, RequestHeader }

object CookieConsent:

  val cookieName = "chesstory_cookie_consent"
  val cookieVersion = "v1"
  private val cookieMaxAge = 60 * 60 * 24 * 180

  enum Choice:
    case EssentialOnly
    case Preferences

  object Choice:
    def fromString(value: String): Option[Choice] =
      value match
        case "essential"   => Some(Choice.EssentialOnly)
        case "preferences" => Some(Choice.Preferences)
        case _             => None

  case class State(
      decided: Boolean,
      preferences: Boolean
  ):
    def preferencesAllowed = decided && preferences

  val undecided = State(decided = false, preferences = false)

  def fromRequest(req: RequestHeader): State =
    req.cookies
      .get(cookieName)
      .flatMap(cookie => parse(cookie.value))
      .getOrElse(undecided)

  def cookie(choice: Choice, secure: Boolean): Cookie =
    Cookie(
      name = cookieName,
      value = encode(choice),
      maxAge = Some(cookieMaxAge),
      path = "/",
      secure = secure,
      httpOnly = false,
      sameSite = Some(Cookie.SameSite.Lax)
    )

  def discard: DiscardingCookie =
    DiscardingCookie(cookieName, path = "/")

  private def encode(choice: Choice): String =
    s"$cookieVersion:${choice match
        case Choice.EssentialOnly => "e"
        case Choice.Preferences   => "p"}"

  private def parse(value: String): Option[State] =
    value match
      case raw if raw == s"$cookieVersion:e" => Some(State(decided = true, preferences = false))
      case raw if raw == s"$cookieVersion:p" => Some(State(decided = true, preferences = true))
      case _                                 => None
