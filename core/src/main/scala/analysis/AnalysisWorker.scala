package chess
package analysis

import cats.effect.*
import chess.db.{Database, BlobStorage, JobRepo}
import doobie.implicits.*
import scala.concurrent.ExecutionContext.Implicits.global

object AnalysisWorker extends IOApp:

  private val logger = org.slf4j.LoggerFactory.getLogger("worker")


  override def run(args: List[String]): IO[ExitCode] =
    val redisUrl = EnvLoader.getOrElse("REDIS_URL", "redis://localhost:6379")
    
    Database.make().use { xa =>
      QueueClient.makeRedis(redisUrl).use { queue =>
        ProgressClient.makeRedis(redisUrl).use { progress =>
          cats.effect.std.Dispatcher.parallel[IO].use { dispatcher =>
            val blob = new BlobStorage.FileBlobStorage("data")
            val analysisService = new AnalysisService(xa, blob)
            val engineService = new EngineService()

            val workerLoop = queue.dequeue.flatMap { job =>
              IO(logger.info(s"Processing job ${job.jobId}")) >>
              (for
                // 1. Update Status -> Processing
                _ <- JobRepo.updateStatus(java.util.UUID.fromString(job.jobId), "PROCESSING", 0).transact(xa)

                // 2. Configure Engine
                config = AnalyzePgn.EngineConfig.fromEnv()
                
                // Callback for progress updates
                progCallback = (stage: AnalysisStage.AnalysisStage, p: Double) => 
                   dispatcher.unsafeRunAndForget(
                     progress.update(job.jobId, stage, p).handleErrorWith(e => IO(logger.warn(s"Progress update failed: ${e.getMessage}")))
                   )

                // 3. Run Analysis
                output <- IO.blocking(
                  AnalyzePgn.analyze(
                    pgn = job.pgn, 
                    engineService = engineService, 
                    config = config, 
                    llmRequestedPlys = job.options.forceCriticalPlys, 
                    onProgress = progCallback
                  )
                ).flatMap {
                    case Right(out) => IO.pure(out)
                    case Left(err) => IO.raiseError(new Exception(err))
                  }
                
                // 4. Annotate
                _ <- IO(progCallback(AnalysisStage.LLM_GENERATION, 0.0))
                annotated = LlmAnnotator.annotate(output)
                _ <- IO(progCallback(AnalysisStage.FINALIZATION, 1.0))
                
                // 5. Save
                userId = job.userId.flatMap(u => scala.util.Try(java.util.UUID.fromString(u)).toOption)
                _ <- analysisService.save(job.jobId, annotated, userId)
                
                // 6. Complete
                _ <- JobRepo.updateStatus(java.util.UUID.fromString(job.jobId), "COMPLETED", 100).transact(xa)
                 >> IO(logger.info(s"Job ${job.jobId} completed"))

              yield ()).handleErrorWith { e =>
                 logger.error(s"Job ${job.jobId} failed", e)
                 JobRepo.updateStatus(java.util.UUID.fromString(job.jobId), "FAILED", 0).transact(xa).void
              }
            }.foreverM

            IO(logger.info("Starting Analysis Worker...")) >>
            workerLoop.as(ExitCode.Success)
          }
        }
      }
    }
