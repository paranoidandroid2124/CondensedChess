package chess
package analysis

import cats.effect.*
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.Stdout.*

trait ProgressClient[F[_]]:
  def update(jobId: String, stage: AnalysisStage.AnalysisStage, progress: Double): F[Unit]
  def get(jobId: String): F[Option[AnalysisProgressTracker.AnalysisProgress]]

object ProgressClient:

  // Reusing the data model from the old Tracker for compatibility if possible, 
  // or we define our own cleaner one. Let's reuse for now to minimize ripple.
  import AnalysisProgressTracker.AnalysisProgress
  
  def makeRedis(url: String): Resource[IO, ProgressClient[IO]] =
    Redis[IO].utf8(url).map(cmd => new ProgressClient[IO]:
      def update(jobId: String, stage: AnalysisStage.AnalysisStage, stageProgress: Double): IO[Unit] =
        val totalProg = AnalysisStage.totalProgress(stage, stageProgress)
        // Store as Hash
        // key: "prog:{id}"
        val key = s"prog:$jobId"
        val data = Map(
          "stage" -> stage.toString,
          "stageProgress" -> stageProgress.toString,
          "totalProgress" -> totalProg.toString,
          "updatedAt" -> System.currentTimeMillis().toString
        )
        cmd.hSet(key, data).void

      def get(jobId: String): IO[Option[AnalysisProgress]] =
        val key = s"prog:$jobId"
        cmd.hGetAll(key).map { data =>
          if data.isEmpty then None
          else
            Some(AnalysisProgress(
              stage = AnalysisStage.withName(data.getOrElse("stage", "Setup")),
              stageProgress = data.getOrElse("stageProgress", "0.0").toDouble,
              totalProgress = data.getOrElse("totalProgress", "0.0").toDouble,
              startedAt = data.getOrElse("updatedAt", "0").toLong // Using updatedAt as proxy for now
            ))
        }
    )

  def makeMemory: Resource[IO, ProgressClient[IO]] =
    Resource.eval(IO.ref(Map.empty[String, AnalysisProgress])).map { ref =>
      new ProgressClient[IO]:
        def update(jobId: String, stage: AnalysisStage.AnalysisStage, stageProgress: Double): IO[Unit] =
           val totalProg = AnalysisStage.totalProgress(stage, stageProgress)
           val prog = AnalysisProgress(stage, stageProgress, totalProg, System.currentTimeMillis())
           ref.update(m => m.updated(jobId, prog))

        def get(jobId: String): IO[Option[AnalysisProgress]] =
           ref.get.map(_.get(jobId))
    }
