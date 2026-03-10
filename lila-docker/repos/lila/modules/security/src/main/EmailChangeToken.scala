package lila.security

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.concurrent.duration.FiniteDuration

import lila.core.config.Secret
import lila.core.email.EmailAddress
import lila.core.userId.UserId

final class EmailChangeToken(secret: Secret):

  private val Algo = "HmacSHA256"
  private val encoder = Base64.getUrlEncoder.withoutPadding()
  private val decoder = Base64.getUrlDecoder()
  private val key = secret.value.getBytes(UTF_8)

  private def b64(bytes: Array[Byte]) = encoder.encodeToString(bytes)
  private def unb64(str: String) =
    try Some(decoder.decode(str))
    catch case _: IllegalArgumentException => None

  private def hmac(bytes: Array[Byte]) =
    val mac = Mac.getInstance(Algo)
    mac.init(new SecretKeySpec(key, Algo))
    mac.doFinal(bytes)

  def generate(userId: UserId, email: EmailAddress, ttl: FiniteDuration): String =
    val expiresAt = Instant.now().plusSeconds(ttl.toSeconds)
    val payload =
      s"${userId.value}|${EmailAddress(email.normalize.value).value}|${expiresAt.getEpochSecond}"
    val payloadBytes = payload.getBytes(UTF_8)
    s"${b64(payloadBytes)}.${b64(hmac(payloadBytes))}"

  def read(token: String): Option[(UserId, EmailAddress)] =
    token.split('.') match
      case Array(payloadB64, sigB64) =>
        for
          payloadBytes <- unb64(payloadB64)
          sigBytes <- unb64(sigB64)
          if MessageDigest.isEqual(sigBytes, hmac(payloadBytes))
          payload = String(payloadBytes, UTF_8)
          parts = payload.split('|')
          if parts.length >= 3
          userId = UserId(parts(0))
          email <- EmailAddress.from(parts(1))
          expiresAt <- parts(2).toLongOption.map(Instant.ofEpochSecond)
          if !expiresAt.isBefore(Instant.now())
        yield (userId, EmailAddress(email.normalize.value))
      case _ =>
        None
