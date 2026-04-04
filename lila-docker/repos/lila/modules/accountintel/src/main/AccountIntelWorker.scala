package lila.accountintel

import play.api.Configuration

import scala.concurrent.duration.*

import lila.accountintel.AccountIntel.*
import lila.accountintel.service.AccountNotebookEngine

final class AccountIntelWorker(
    appConfig: Configuration,
    repo: AccountIntelJobStore,
    surfaceStore: AccountIntelSurfaceStore,
    source: AccountGameFetcher,
    engine: AccountNotebookEngine
)(using Executor):

  private val runningTimeout =
    appConfig.getOptional[FiniteDuration]("accountIntel.job.runningTimeout").getOrElse(8.minutes)
  private val retryLimit =
    appConfig.getOptional[Int]("accountIntel.job.retryLimit").getOrElse(3)
  private val successTtl =
    appConfig.getOptional[FiniteDuration]("accountIntel.job.successTtl").getOrElse(24.hours)
  private val failedTtl =
    appConfig.getOptional[FiniteDuration]("accountIntel.job.failedTtl").getOrElse(72.hours)
  private val refreshCooldown =
    appConfig.getOptional[FiniteDuration]("accountIntel.surface.refreshCooldown").getOrElse(24.hours)

  def tick(): Funit =
    runMaintenance() >> runNext()

  def runMaintenance(): Funit =
    repo.requeueTimedOutRunning(runningTimeout, retryLimit) >>
      repo.cleanupExpired(successTtl, failedTtl)

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
      _ <- repo.setProgress(job.id, "fetching_games", "fetching_games", etaSec = Some(150))
      games <- source.fetchRecentGames(job.provider, job.username)
      fingerprint = AccountIntelSurfaceSnapshot.fingerprint(games)
      existing <- surfaceStore.byDedupeKeyAndFingerprint(job.dedupeKey, fingerprint)
      _ <- existing.fold(buildAndPersist(job, games, fingerprint)):
        snapshot =>
          repo.markSucceeded(
            id = job.id,
            surfaceId = snapshot.id,
            sourceFingerprint = snapshot.sourceFingerprint,
            warnings = snapshot.warnings,
            surfaceJson = snapshot.surfaceJson,
            sampledGameCount = snapshot.sampledGameCount,
            totalGames = snapshot.sampledGameCount,
            cacheHit = true,
            refreshLockedUntil = Some(snapshot.updatedAt.plusMillis(refreshCooldown.toMillis))
          )
    yield ()

  private def buildAndPersist(job: AccountIntelJob, games: List[ExternalGame], fingerprint: String): Funit =
    for
      _ <- repo.setProgress(
        job.id,
        "extracting_primitives",
        "building_surface",
        processedGames = Some(0),
        totalGames = Some(games.size),
        etaSec = Some(90)
      )
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
            _ <- repo.setProgress(
              job.id,
              "publishing_surface",
              "building_surface",
              processedGames = Some(games.size),
              totalGames = Some(games.size),
              etaSec = Some(15)
            )
            snapshot = AccountIntelSurfaceSnapshot.fromArtifact(
              provider = job.provider,
              username = job.username,
              kind = job.kind,
              sourceFingerprint = fingerprint,
              artifact = artifact
            )
            _ <- surfaceStore.insert(snapshot)
            _ <- repo.markSucceeded(
              id = job.id,
              surfaceId = snapshot.id,
              sourceFingerprint = snapshot.sourceFingerprint,
              warnings = artifact.warnings,
              surfaceJson = snapshot.surfaceJson,
              sampledGameCount = artifact.sampledGameCount,
              totalGames = games.size,
              cacheHit = false,
              refreshLockedUntil = Some(snapshot.updatedAt.plusMillis(refreshCooldown.toMillis))
            )
          yield ()
      )
    yield ()
