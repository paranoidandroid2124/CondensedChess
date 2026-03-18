package lila.accountintel

import play.api.Configuration
import play.api.libs.json.Json

import scala.concurrent.duration.*

import lila.accountintel.AccountIntel.*
import lila.accountintel.service.AccountNotebookEngine
import lila.accountintel.service.NotebookSink

final class AccountIntelWorker(
    appConfig: Configuration,
    repo: AccountIntelJobStore,
    source: AccountGameFetcher,
    engine: AccountNotebookEngine,
    notebookSink: NotebookSink
)(using Executor):

  private val runningTimeout =
    appConfig.getOptional[FiniteDuration]("accountIntel.job.runningTimeout").getOrElse(8.minutes)
  private val retryLimit =
    appConfig.getOptional[Int]("accountIntel.job.retryLimit").getOrElse(3)
  private val successTtl =
    appConfig.getOptional[FiniteDuration]("accountIntel.job.successTtl").getOrElse(24.hours)
  private val failedTtl =
    appConfig.getOptional[FiniteDuration]("accountIntel.job.failedTtl").getOrElse(72.hours)

  def tick(): Funit =
    repo.requeueTimedOutRunning(runningTimeout, retryLimit) >>
      repo.cleanupExpired(successTtl, failedTtl) >>
      runNext()

  def runJob(jobId: String): Fu[RunJobOutcome] =
    repo.byId(jobId).flatMap:
      case None => fuccess(RunJobOutcome.Missing)
      case Some(_) =>
        repo
          .claimQueuedById(jobId, nowInstant)
          .flatMap:
            case Some(job) =>
              process(job)
                .recoverWith { case err =>
                  logger.warn(s"accountintel worker failed job=${job.id} err=${err.getMessage}")
                  repo.markFailed(job.id, "worker_exception", err.getMessage)
                }
                .inject(RunJobOutcome.Started)
            case None => fuccess(RunJobOutcome.NotQueued)

  def runNext(): Funit =
    repo
      .claimNextQueued(nowInstant)
      .flatMap:
        case None => funit
        case Some(job) =>
          process(job).recoverWith { case err =>
            logger.warn(s"accountintel worker failed job=${job.id} err=${err.getMessage}")
            repo.markFailed(job.id, "worker_exception", err.getMessage)
          }

  private def process(job: AccountIntelJob): Funit =
    for
      _ <- repo.setProgress(job.id, "fetching_games")
      games <- source.fetchRecentGames(job.provider, job.username)
      _ <- repo.setProgress(job.id, "extracting_primitives")
      buildResult <- engine.buildFromGames(
        job.provider,
        job.username,
        job.kind,
        games,
        generatedAt = nowInstant
      )
      _ <- buildResult.fold(
        err => repo.markFailed(job.id, "build_failed", err),
        artifact =>
          for
            _ <- repo.setProgress(job.id, "creating_notebook")
            _ <- persist(job, artifact)
          yield ()
      )
    yield ()

  private def persist(job: AccountIntelJob, artifact: NotebookBuildArtifact): Funit =
    notebookSink
      .persist(job, artifact)
      .flatMap:
        case Left(failure) =>
          repo.markFailed(
            job.id,
            failure.code,
            failure.message
          )
        case Right(persisted) =>
          repo.markSucceeded(
            id = job.id,
            studyId = persisted.studyId,
            chapterId = persisted.chapterId,
            notebookUrl = persisted.notebookUrl,
            warnings = artifact.warnings,
            surfaceJson = Json.stringify(artifact.surface)
          )
