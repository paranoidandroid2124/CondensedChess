package lila.security

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.concurrent.duration.*

import play.api.mvc.{ Cookie, DiscardingCookie, RequestHeader }

import lila.core.config.Secret
import lila.core.email.EmailAddress
import lila.core.userId.UserId

object EmailConfirm:

  private val Algo = "HmacSHA256"
  private val encoder = Base64.getUrlEncoder.withoutPadding()
  private val decoder = Base64.getUrlDecoder()
  private val ttl = 24.hours

  @volatile private var secretValue: String = "dev-email-confirm-secret"
  @volatile private var cookieName: String = "lila-email-confirm"
  @volatile private var cookieSecure: Boolean = false

  case class Pending(userId: UserId, email: EmailAddress)

  def configure(secret: Secret, cookie: String, secure: Boolean): Unit =
    secretValue = secret.value
    cookieName = cookie.trim match
      case "" => "lila-email-confirm"
      case c  => c
    cookieSecure = secure

  private def hmac(bytes: Array[Byte]): Array[Byte] =
    val key = new SecretKeySpec(secretValue.getBytes(StandardCharsets.UTF_8), Algo)
    val mac = Mac.getInstance(Algo)
    mac.init(key)
    mac.doFinal(bytes)

  private def b64(bytes: Array[Byte]): String =
    encoder.encodeToString(bytes)

  private def unb64(str: String): Option[Array[Byte]] =
    try Some(decoder.decode(str))
    catch case _: IllegalArgumentException => None

  private def encode(pending: Pending): String =
    val expiresAt = Instant.now().plusSeconds(ttl.toSeconds)
    val payload = s"${pending.userId.value}|${EmailAddress(pending.email.normalize.value).value}|${expiresAt.getEpochSecond}"
    val bytes = payload.getBytes(StandardCharsets.UTF_8)
    s"${b64(bytes)}.${b64(hmac(bytes))}"

  private def decode(token: String): Option[Pending] =
    token.split('.') match
      case Array(payloadB64, sigB64) =>
        for
          payloadBytes <- unb64(payloadB64)
          sigBytes <- unb64(sigB64)
          if MessageDigest.isEqual(hmac(payloadBytes), sigBytes)
          payload <- Some(String(payloadBytes, StandardCharsets.UTF_8))
          parts = payload.split('|')
          if parts.length == 3
          userId <- Option.when(parts(0).nonEmpty)(UserId(parts(0)))
          email <- EmailAddress.from(parts(1))
          expiresAt <- parts(2).toLongOption
          if Instant.now().isBefore(Instant.ofEpochSecond(expiresAt))
        yield Pending(userId, EmailAddress(email.normalize.value))
      case _ => None

  object cookie:
    def has(req: RequestHeader): Boolean = get(req).isDefined

    def get(req: RequestHeader): Option[Pending] =
      req.cookies.get(cookieName).flatMap(c => decode(c.value))

    def set(userId: UserId, email: EmailAddress): Cookie =
      Cookie(
        name = cookieName,
        value = encode(Pending(userId, EmailAddress(email.normalize.value))),
        maxAge = Some(ttl.toSeconds.toInt),
        path = "/",
        secure = cookieSecure,
        httpOnly = true,
        sameSite = Some(Cookie.SameSite.Lax)
      )

    def clear: DiscardingCookie =
      DiscardingCookie(cookieName, path = "/")
