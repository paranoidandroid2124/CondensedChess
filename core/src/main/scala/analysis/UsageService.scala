package chess.analysis

import cats.effect.*
import dev.profunktor.redis4cats.RedisCommands
import java.util.UUID
import java.time.LocalDate
import java.time.format.DateTimeFormatter

trait UsageService[F[_]]:
  def checkLimit(userId: UUID, tier: String): F[Boolean]
  def increment(userId: UUID): F[Unit]

object UsageService:
  
  private val DateFmt = DateTimeFormatter.BASIC_ISO_DATE // YYYYMMDD
  
  def make(redis: RedisCommands[IO, String, String]): UsageService[IO] = new UsageService[IO]:
    
    private def key(userId: UUID): String =
      val date = LocalDate.now().format(DateFmt)
      s"usage:daily:${userId}:${date}"

    def checkLimit(userId: UUID, tier: String): IO[Boolean] =
      // PRO / PREMIUM / BETA has no limit
      if (tier != "FREE") IO.pure(true)
      else
        redis.get(key(userId)).map {
          case Some(countStr) => 
            val count = countStr.toIntOption.getOrElse(0)
            count < 1 // Limit 1 for FREE
          case None => true
        }

    def increment(userId: UUID): IO[Unit] =
      val k = key(userId)
      for
        count <- redis.incr(k)
        _ <- if (count == 1) redis.expire(k, scala.concurrent.duration.Duration(24, scala.concurrent.duration.HOURS)).void else IO.unit
      yield ()
