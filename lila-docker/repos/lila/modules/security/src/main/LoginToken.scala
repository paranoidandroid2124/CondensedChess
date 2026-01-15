package lila.security

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.concurrent.duration.*

import lila.core.config.Secret
import lila.core.email.EmailAddress

final class LoginToken(secret: Secret):

  private val Algo = "HmacSHA256"
  private val encoder = Base64.getUrlEncoder().withoutPadding()
  private val decoder = Base64.getUrlDecoder()
  private val key = new SecretKeySpec(secret.value.getBytes(StandardCharsets.UTF_8), Algo)

  private def b64(bytes: Array[Byte]): String = encoder.encodeToString(bytes)
  private def unb64(str: String): Option[Array[Byte]] =
    try Some(decoder.decode(str))
    catch case _: IllegalArgumentException => None

  private def hmac(bytes: Array[Byte]): Array[Byte] =
    val mac = Mac.getInstance(Algo)
    mac.init(key)
    mac.doFinal(bytes)

  def generate(email: EmailAddress, ttl: FiniteDuration): String =
    val expiresAt = Instant.now().plusSeconds(ttl.toSeconds)
    val payload = s"${EmailAddress(email.normalize.value).value}|${expiresAt.getEpochSecond}|${scalalib.SecureRandom.nextString(12)}"
    val bytes = payload.getBytes(StandardCharsets.UTF_8)
    s"${b64(bytes)}.${b64(hmac(bytes))}"

  def read(token: String): Option[EmailAddress] =
    token.split('.') match
      case Array(payloadB64, sigB64) =>
        for
          payloadBytes <- unb64(payloadB64)
          sigBytes <- unb64(sigB64)
          if MessageDigest.isEqual(hmac(payloadBytes), sigBytes)
          payload <- Some(String(payloadBytes, StandardCharsets.UTF_8))
          parts = payload.split('|')
          if parts.length >= 2
          email <- EmailAddress.from(parts(0))
          exp <- parts(1).toLongOption
          if Instant.now().isBefore(Instant.ofEpochSecond(exp))
        yield EmailAddress(email.normalize.value)
      case _ => None
