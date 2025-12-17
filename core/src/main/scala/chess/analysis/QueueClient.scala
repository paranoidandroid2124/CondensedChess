package chess
package analysis

import cats.effect.*
import cats.data.NonEmptyList
import scala.concurrent.duration.*
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.Stdout.*
import ujson.Value

// Simple Job Protocol
case class JobPayload(
  jobId: String,
  userId: Option[String],
  pgn: String,
  options: ApiTypes.ReviewOptions
)

trait QueueClient[F[_]]:
  def enqueue(payload: JobPayload): F[Unit]
  def dequeue: F[JobPayload] // Blocking dequeue

object QueueClient:

  // --- Redis Implementation ---
  
  def makeRedis(url: String): Resource[IO, QueueClient[IO]] =
    Redis[IO].utf8(url).map(cmd => new QueueClient[IO]:
      def enqueue(payload: JobPayload): IO[Unit] =
        val json = ujson.Obj(
          "jobId" -> payload.jobId,
          "userId" -> payload.userId.map(ujson.Str(_)).getOrElse(ujson.Null),
          "pgn" -> payload.pgn,
          // Simplify options serialization for this MVP
          "force" -> ujson.Arr.from(payload.options.forceCriticalPlys)
          // Add other options as needed
        ).render()
        cmd.rPush("analysis_queue", json).timeout(5.seconds).void

      def dequeue: IO[JobPayload] =
        // BLPOP returns Option[(key, value)]
        cmd.blPop(0.seconds, NonEmptyList.of("analysis_queue")).flatMap {
           case Some((_, jsonStr)) =>
             IO {
               val json = ujson.read(jsonStr)
               JobPayload(
                 jobId = json("jobId").str,
                 userId = scala.util.Try(json("userId").str).toOption,
                 pgn = json("pgn").str,
                 options = ApiTypes.ReviewOptions(
                   forceCriticalPlys = scala.util.Try(json("force").arr.map(_.num.toInt).toSet).getOrElse(Set.empty)
                 )
               )
             }.handleErrorWith { e =>
                // Poison Pill detected: Log and skip
                IO(System.err.println(s"[QueueClient] Failed to parse job: ${e.getMessage}. Payload: $jsonStr")) >> dequeue
             }
           case None => dequeue // Retry if timeout/empty (though 0.seconds usually blocks)
        }
    )

  // --- Memory Implementation (Fallback) ---
  def makeMemory: Resource[IO, QueueClient[IO]] =
    Resource.eval(cats.effect.std.Queue.unbounded[IO, JobPayload]).map { q =>
      new QueueClient[IO]:
        def enqueue(payload: JobPayload): IO[Unit] = q.offer(payload)
        def dequeue: IO[JobPayload] = q.take
    }
