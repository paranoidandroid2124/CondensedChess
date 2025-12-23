package chess
package analysis

import cats.effect.*
import java.util.UUID
import java.time.Instant
import chess.db.{BlobRepo, DbGame, DbAnalysisJob, GameRepo, JobRepo}
import doobie.*
import doobie.implicits.*

trait JobManager:
  def submit(pgn: String, config: AnalyzePgn.EngineConfig, llmPlys: Set[Int], userId: Option[String]): IO[String]

object JobManager:
  private val logger = org.slf4j.LoggerFactory.getLogger("chess.job")

  def make(xa: Transactor[IO]): IO[JobManager] = 

    // Janitor: Cleanup Zombie jobs & Init Blob Table
    val startup = for {
      _ <- BlobRepo.init.transact(xa)
      // Aggressive Cleanup: Reset ANY job left in PROCESSING state from previous runs
      _ <- sql"""
        UPDATE analysis_jobs 
        SET status = 'FAILED', error_message = 'Job terminated unexpectedly (server restart)'
        WHERE status LIKE 'PROCESSING%'
      """.update.run.transact(xa).flatMap { count =>
         if (count > 0) IO(logger.info(s"[Start] Cleaned up $count zombie jobs from previous run."))
         else IO.unit
      }
    } yield ()

    startup.as {
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
    
          // 2. Create Job Entity (Status=QUEUED, with PGN and options)
          // Serialize options to JSON
          val optionsJson = ujson.Obj("forceCriticalPlys" -> ujson.Arr(llmPlys.toSeq.map(ujson.Num(_))*)).render()
          
          val job = DbAnalysisJob(
            id = id, 
            gameId = id,
            status = "QUEUED",  // DB-only queue: jobs start as QUEUED
            progress = 0,
            errorMessage = None,
            pgnText = pgn,
            optionsJson = optionsJson,
            createdAt = now,
            updatedAt = now
          )
    
          // 3. Persist to DB (no memory queue or Redis)
          val dbOps = for {
            _ <- GameRepo.insert(game)
            _ <- JobRepo.create(job)
          } yield ()
    
          for {
            _ <- IO(logger.info(s"Job $id: Saving to DB (Status=QUEUED)..."))
            _ <- dbOps.transact(xa)
            _ <- IO(logger.info(s"Job $id: Queued successfully."))
          } yield id.toString
    }
