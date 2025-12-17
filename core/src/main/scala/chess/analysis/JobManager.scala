package chess
package analysis

import cats.effect.*
import java.util.UUID
import java.time.Instant
import scala.concurrent.duration.*
import chess.db.{DbGame, DbAnalysisJob, GameRepo, JobRepo}
import doobie.*
import doobie.implicits.*

trait JobManager:
  def submit(pgn: String, config: AnalyzePgn.EngineConfig, llmPlys: Set[Int], userId: Option[String]): IO[String]
  def startPoller: IO[Unit]

object JobManager:
  private val logger = org.slf4j.LoggerFactory.getLogger("chess.job")

  def make(queue: QueueClient[IO], xa: Transactor[IO]): IO[JobManager] = 
    cats.effect.std.Queue.unbounded[IO, JobPayload].map { internalQueue =>
      new JobManager:
        private val TagRegex = """\[(\w+) "([^"]+)"\]""".r
    
        private def extractTag(pgn: String, name: String): Option[String] =
          TagRegex.findAllMatchIn(pgn)
            .collectFirst { case m if m.group(1) == name => m.group(2) }
    
        def submit(pgn: String, config: AnalyzePgn.EngineConfig, llmPlys: Set[Int], userIdString: Option[String]): IO[String] =
          val id = UUID.randomUUID()
          val now = Instant.now()
          val userId = userIdString.flatMap(u => scala.util.Try(UUID.fromString(u)).toOption)
    
          // 1. Create Game Entity
          val game = DbGame(
            id = id,
            userId = userId,
            whitePlayer = extractTag(pgn, "White"),
            blackPlayer = extractTag(pgn, "Black"),
            result = extractTag(pgn, "Result"),
            datePlayed = None, 
            eco = extractTag(pgn, "ECO"),
            pgnHeaders = "{}", 
            analysisS3Key = None, 
            createdAt = now
          )
    
          // 2. Create Job Entity (Status=CREATED)
          val job = DbAnalysisJob(
            id = id, 
            gameId = id,
            status = "CREATED",
            progress = 0,
            errorMessage = None,
            createdAt = now,
            updatedAt = now
          )
    
          val payload = JobPayload(id.toString, userIdString, pgn, ApiTypes.ReviewOptions(forceCriticalPlys = llmPlys))
    
          // 3. Persist to DB ONLY
          val dbOps = for {
            _ <- GameRepo.insert(game)
            _ <- JobRepo.create(job)
          } yield ()
    
          for {
            _ <- IO(logger.info(s"Job $id: Saving to DB (Status=CREATED)..."))
            _ <- dbOps.transact(xa)
            _ <- IO(logger.info(s"Job $id: DB Saved. Buffering to Memory Queue..."))
            _ <- internalQueue.offer(payload)
            _ <- IO(logger.info(s"Job $id: Buffered successfully."))
          } yield id.toString
    
        def getStatus(id: String): IO[Option[String]] = 
          JobRepo.findById(UUID.fromString(id)).transact(xa).map(_.map(_.status))

        // Background Poller: Memory -> Redis
        def startPoller: IO[Unit] = 
          internalQueue.take.flatMap { payload =>
             (for 
               _ <- IO(logger.info(s"Poller: Popped Job ${payload.jobId}. Pushing to Redis..."))
               _ <- queue.enqueue(payload) // Standard enqueue (Redis)
               _ <- JobRepo.updateStatus(UUID.fromString(payload.jobId), "QUEUED", 0).transact(xa)
               _ <- IO(logger.info(s"Poller: Job ${payload.jobId} pushed to Redis & Status updated."))
             yield ()).handleErrorWith { e =>
               // If Redis fails, we should ideally retry or put back in queue?
               // For MVP, just log error. The job remains "CREATED" regarding DB status (stuck state), 
               // but it is lost from memory queue if we don't re-enqueue.
               // Let's Retry forever for Redis errors.
               logger.error(s"Poller: Failed to push Job ${payload.jobId} to Redis. Retrying in 3s...", e)
               IO.sleep(3.seconds) >> internalQueue.offer(payload) // Re-buffer
             }
          }.foreverM
    }
