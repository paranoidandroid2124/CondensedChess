package chess
package analysis

import cats.effect.*
import java.util.UUID
import java.time.Instant
import chess.db.{DbGame, DbAnalysisJob, GameRepo, JobRepo}
import doobie.*
import doobie.implicits.*

trait JobManager:
  def submit(pgn: String, config: AnalyzePgn.EngineConfig, llmPlys: Set[Int], userId: Option[String]): IO[String]
  def getStatus(id: String): IO[Option[String]]

object JobManager:
  def make(queue: QueueClient[IO], xa: Transactor[IO]): JobManager = new JobManager:
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
        analysisS3Key = None, // Linked later
        createdAt = now
      )

      // 2. Create Job Entity
      val job = DbAnalysisJob(
        id = id, // 1:1 mapping for simplicity in this system
        gameId = id,
        status = "QUEUED",
        progress = 0,
        errorMessage = None,
        createdAt = now,
        updatedAt = now
      )

      val payload = JobPayload(id.toString, userIdString, pgn, ApiTypes.ReviewOptions(forceCriticalPlys = llmPlys))

      // 3. Persist and Enqueue
      val dbOps = for {
        _ <- GameRepo.insert(game)
        _ <- JobRepo.create(job)
      } yield ()

      for {
        _ <- dbOps.transact(xa)
        _ <- queue.enqueue(payload)
      } yield id.toString

    def getStatus(id: String): IO[Option[String]] = 
      JobRepo.findById(UUID.fromString(id)).transact(xa).map(_.map(_.status))
