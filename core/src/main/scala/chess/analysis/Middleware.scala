package chess
package analysis

import cats.data.{Kleisli, OptionT}
import cats.effect.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.slf4j.Logger

object Middleware:

  // --- Authentication ---
  
  def authMiddleware(apiKey: String): HttpRoutes[IO] => HttpRoutes[IO] = 
    routes => Kleisli { req =>
      val authHeader = req.headers.get(org.http4s.headers.Authorization.name)
        .map(_.head.value)
      
      authHeader match
        case Some(token) if token == s"Bearer $apiKey" =>
          routes(req)
        case _ =>
          OptionT.liftF(Forbidden("Unauthorized: Invalid API Key"))
    }

  // --- Rate Limiting ---
  
  // Simple Token Bucket state: (Tokens, LastRefillTime)
  case class Bucket(tokens: Double, lastRefill: Long)

  def rateLimitMiddleware(
      capacity: Int,
      refillRatePerSecond: Double
  ): IO[HttpRoutes[IO] => HttpRoutes[IO]] =
    Ref.of[IO, Map[String, Bucket]](Map.empty).map { stateRef =>
      routes => Kleisli { req =>
        val ip = req.remoteAddr.map(_.toString).getOrElse("unknown")
        
        OptionT.liftF(checkLimit(stateRef, ip, capacity, refillRatePerSecond)).flatMap { allowed =>
          if (allowed) routes(req)
          else OptionT.liftF(TooManyRequests("Rate limit exceeded"))
        }
      }
    }

  private def checkLimit(
      state: Ref[IO, Map[String, Bucket]],
      key: String,
      capacity: Int,
      rate: Double
  ): IO[Boolean] =
    val now = System.currentTimeMillis()
    state.modify { m =>
      val bucket = m.getOrElse(key, Bucket(capacity.toDouble, now))
      val elapsedSeconds = (now - bucket.lastRefill) / 1000.0
      val newTokens = math.min(capacity.toDouble, bucket.tokens + elapsedSeconds * rate)
      
      if (newTokens >= 1.0)
        val nextBucket = Bucket(newTokens - 1.0, now)
        (m.updated(key, nextBucket), true)
      else
        val nextBucket = bucket.copy(tokens = newTokens, lastRefill = now)
        (m.updated(key, nextBucket), false)
    }

  // --- Request Logging ---
  
  /**
   * Request logging middleware for structured JSON logs.
   * Logs method, path, status, and duration for each request.
   */
  def requestLoggingMiddleware(logger: Logger): HttpRoutes[IO] => HttpRoutes[IO] =
    routes => Kleisli { req =>
      val start = System.currentTimeMillis()
      routes(req).semiflatMap { response =>
        IO {
          val duration = System.currentTimeMillis() - start
          val logJson = ujson.Obj(
            "type" -> "request",
            "method" -> req.method.name,
            "path" -> req.uri.path.renderString,
            "status" -> response.status.code,
            "duration_ms" -> duration
          ).render()
          logger.info(logJson)
          response
        }
      }
    }
