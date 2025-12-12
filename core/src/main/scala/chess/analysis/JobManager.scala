package chess
package analysis

import cats.effect.*
import java.util.UUID

trait JobManager:
  def submit(pgn: String, config: AnalyzePgn.EngineConfig, llmPlys: Set[Int], userId: Option[String]): IO[String]
  def getStatus(id: String): IO[Option[String]]

object JobManager:
  def make(queue: QueueClient[IO]): JobManager = new JobManager:
    def submit(pgn: String, config: AnalyzePgn.EngineConfig, llmPlys: Set[Int], userId: Option[String]): IO[String] =
      val id = UUID.randomUUID().toString
      val payload = JobPayload(id, userId, pgn, ApiTypes.ReviewOptions(forceCriticalPlys = llmPlys))
      queue.enqueue(payload).as(id)

    def getStatus(id: String): IO[Option[String]] = 
      // In stateless architecture, status checks should query the DB (Analysis_Jobs table)
      // Since JobManager is now just a producer, it doesn't know the status.
      // The caller (ApiServer) should use JobRepo directly for status checks.
      IO.pure(Some("queued")) // Placeholder
