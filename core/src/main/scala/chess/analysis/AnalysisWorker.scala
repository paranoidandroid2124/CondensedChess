package chess
package analysis

import cats.effect.*
import chess.db.{Database, BlobStorage, JobRepo, GameRepo}
import doobie.implicits.*
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global

object AnalysisWorker extends IOApp:

  private val logger = org.slf4j.LoggerFactory.getLogger("worker")


  override def run(args: List[String]): IO[ExitCode] =
    
    Database.make().use { xa =>
      // DB-only queue: no QueueClient needed
      cats.effect.std.Dispatcher.parallel[IO].use { dispatcher =>
        val blob = new BlobStorage.DbBlobStorage(xa)
        val analysisService = new AnalysisService(xa, blob)
        val engineService = new EngineService()

        // --- Worker Loop: Poll DB for QUEUED jobs ---
        def pollAndProcess: IO[Unit] = 
          JobRepo.claimNextJob.transact(xa).flatMap {
            case Some(job) =>
              IO(logger.info(s"Processing job ${job.id}")) >> {
                // Parse options from JSON - outside for-comprehension
                val optionsJson = scala.util.Try(ujson.read(job.optionsJson)).getOrElse(ujson.Obj())
                val forceCriticalPlys = optionsJson.obj.get("forceCriticalPlys")
                  .map(_.arr.map(_.num.toInt).toSet).getOrElse(Set.empty[Int])
                
                // Configure Engine
                val config = AnalyzePgn.EngineConfig.fromEnv()
                
                // Callback for progress updates - uses DB for distributed consistency
                val progCallback: (AnalysisStage.AnalysisStage, Double) => Unit = 
                  (stage, p) => dispatcher.unsafeRunAndForget(
                    JobRepo.updateStatus(
                      job.id, 
                      s"PROCESSING:${AnalysisStage.labelFor(stage)}:${(p * 100).toInt}%", 
                      (p * 100).toInt
                    ).transact(xa).handleErrorWith(e => IO(logger.warn(s"Progress update failed: ${e.getMessage}")))
                  )

                (for
                  // Run Analysis
                  output <- IO.blocking(
                    AnalyzePgn.analyze(
                      pgn = job.pgnText, 
                      engineService = engineService, 
                      config = config, 
                      llmRequestedPlys = forceCriticalPlys, 
                      onProgress = progCallback
                    )
                  ).flatMap {
                      case Right(out) => IO.pure(out)
                      case Left(err) => IO.raiseError(new Exception(err))
                    }
                  
                  // Annotate
                  _ <- IO(progCallback(AnalysisStage.LLM_GENERATION, 0.0))
                  annotated = LlmAnnotator.annotate(output)
                  _ <- IO(progCallback(AnalysisStage.FINALIZATION, 1.0))
                  
                  // Save (get userId from games table via gameId)
                  gameOpt <- GameRepo.findById(job.gameId).transact(xa)
                  userIdOpt = gameOpt.flatMap(_.userId)
                  _ <- analysisService.save(job.id.toString, annotated, userIdOpt)
                  
                  // Verify blob exists to ensure data consistency
                  _ <- blob.load(s"games/${job.id}/analysis.json").flatMap {
                     case Some(_) => IO.unit
                     case None => IO.raiseError(new Exception(s"Blob verification failed for job ${job.id}"))
                  }

                  // Complete
                  _ <- JobRepo.updateStatus(job.id, "COMPLETED", 100).transact(xa)
                   >> IO(logger.info(s"Job ${job.id} completed"))

                yield ()).handleErrorWith { e =>
                   logger.error(s"Job ${job.id} failed", e)
                   JobRepo.updateStatus(job.id, "FAILED", 0).transact(xa).void
                }
              }
            case None =>
              // No jobs available, wait before polling again
              IO.sleep(1.second)
          }
        
        val workerLoop = pollAndProcess.foreverM

        IO(logger.info("Starting Analysis Worker (DB-only queue)...")) >>
        workerLoop.as(ExitCode.Success)
      }
    }
