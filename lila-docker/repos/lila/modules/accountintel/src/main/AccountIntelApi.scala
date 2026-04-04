package lila.accountintel

import play.api.Configuration

import java.time.Instant
import scala.concurrent.duration.*

import lila.accountintel.AccountIntel.*

final class AccountIntelApi(
    appConfig: Configuration,
    repo: AccountIntelJobStore,
    surfaceStore: AccountIntelSurfaceStore,
    dispatcher: AccountIntelJobDispatcher
)(using Executor):

  private val surfaceReuseTtl =
    appConfig.getOptional[FiniteDuration]("accountIntel.surface.reuseTtl").getOrElse(72.hours)
  private val refreshCooldown =
    appConfig.getOptional[FiniteDuration]("accountIntel.surface.refreshCooldown").getOrElse(24.hours)
  private val failedCooldown =
    appConfig.getOptional[FiniteDuration]("accountIntel.job.failedCooldown").getOrElse(15.minutes)
  private val retryLimit =
    appConfig.getOptional[Int]("accountIntel.job.retryLimit").getOrElse(3)
  private val workerEnabled =
    appConfig.getOptional[Boolean]("accountIntel.worker.enabled").getOrElse(true)

  def submit(
      ownerId: UserId,
      provider: String,
      username: String,
      kind: ProductKind,
      force: Boolean = false
  ): Fu[Either[String, AccountIntelJob]] =
    normalizedScope(provider, username, kind).fold(
      err => fuccess(Left(err)),
      { case (safeProvider, safeUsername, sharedKey) =>
        val ownerKey = AccountIntelJob.ownerScopeKey(ownerId.value, safeProvider, safeUsername, kind)
        val now = nowInstant
        for
          latestOwner <- repo.latestByOwnerScopeKey(ownerKey)
          latestShared <- surfaceStore.latestByDedupeKey(sharedKey)
          freshShared <- surfaceStore.latestFreshByDedupeKey(sharedKey, now.minusMillis(surfaceReuseTtl.toMillis))
          activeBuild <- repo.latestActiveBuildByDedupeKey(sharedKey)
          result <- decideSubmit(
            ownerId = ownerId,
            provider = safeProvider,
            username = safeUsername,
            kind = kind,
            ownerKey = ownerKey,
            latestOwner = latestOwner,
            latestShared = latestShared,
            freshShared = freshShared,
            activeBuild = activeBuild,
            force = force,
            now = now
          )
        yield result
      }
    )

  def status(ownerId: UserId, jobId: String): Fu[Option[AccountIntelJob]] =
    repo.byId(jobId).map(_.filter(_.ownerId == ownerId.value))

  def selectedJob(
      ownerId: UserId,
      provider: String,
      username: String,
      kind: ProductKind,
      jobId: String
  ): Fu[Either[String, Option[AccountIntelJob]]] =
    withNormalizedOwnerKey(ownerId, provider, username, kind): ownerScopeKey =>
      repo.byId(jobId).map(_.filter(job => job.ownerId == ownerId.value && job.ownerScopeKey == ownerScopeKey))

  def latestSuccessful(
      ownerId: UserId,
      provider: String,
      username: String,
      kind: ProductKind
  ): Fu[Either[String, Option[AccountIntelJob]]] =
    withNormalizedOwnerKey(ownerId, provider, username, kind): ownerScopeKey =>
      repo.latestSucceededByOwnerScopeKey(ownerScopeKey)

  def latestActive(
      ownerId: UserId,
      provider: String,
      username: String,
      kind: ProductKind
  ): Fu[Either[String, Option[AccountIntelJob]]] =
    withNormalizedOwnerKey(ownerId, provider, username, kind): ownerScopeKey =>
      repo.latestActiveByOwnerScopeKey(ownerScopeKey)

  def history(
      ownerId: UserId,
      provider: String,
      username: String,
      kind: ProductKind,
      limit: Int = 10
  ): Fu[Either[String, List[AccountIntelJob]]] =
    withNormalizedOwnerKey(ownerId, provider, username, kind): ownerScopeKey =>
      repo.recentByOwnerScopeKey(ownerScopeKey, limit)

  def recentSuccessful(ownerId: UserId, limit: Int = 6): Fu[List[AccountIntelJob]] =
    repo.recentSucceededByOwner(ownerId.value, limit)

  private def decideSubmit(
      ownerId: UserId,
      provider: String,
      username: String,
      kind: ProductKind,
      ownerKey: String,
      latestOwner: Option[AccountIntelJob],
      latestShared: Option[AccountIntelSurfaceSnapshot],
      freshShared: Option[AccountIntelSurfaceSnapshot],
      activeBuild: Option[AccountIntelJob],
      force: Boolean,
      now: Instant
  ): Fu[Either[String, AccountIntelJob]] =
    val refreshLockedUntil = latestShared.map(_.updatedAt.plusMillis(refreshCooldown.toMillis))

    if force && refreshLockedUntil.exists(_.isAfter(now)) then
      fuccess(Left(s"Refresh is locked until ${refreshLockedUntil.get}."))
    else
      latestOwner match
        case Some(job) if !force && job.status == JobStatus.Queued =>
          fuccess(Right(job))
        case Some(job) if !force && job.status == JobStatus.Running =>
          fuccess(Right(job))
        case Some(job)
            if !force &&
              job.status == JobStatus.Succeeded &&
              freshShared.exists(snapshot => job.surfaceId.contains(snapshot.id)) =>
          fuccess(Right(job))
        case _ =>
          freshShared match
            case Some(snapshot) if !force =>
              attachCached(ownerId, provider, username, kind, snapshot, refreshLockedUntil)
            case _ =>
              latestOwner match
                case Some(job)
                    if !force &&
                      job.status == JobStatus.Failed &&
                      job.updatedAt.isAfter(now.minusMillis(failedCooldown.toMillis)) =>
                  fuccess(Right(job))
                case _ =>
                  activeBuild match
                    case Some(build) =>
                      attachWatchingRun(ownerId, provider, username, kind, build, refreshLockedUntil)
                    case None =>
                      enqueue(ownerId, provider, username, kind, refreshLockedUntil)

  private def enqueue(
      ownerId: UserId,
      provider: String,
      username: String,
      kind: ProductKind,
      refreshLockedUntil: Option[Instant]
  ): Fu[Either[String, AccountIntelJob]] =
    val job = AccountIntelJob
      .newQueued(ownerId.value, provider, username, kind)
      .copy(refreshLockedUntil = refreshLockedUntil)
    repo
      .insert(job)
      .flatMap: _ =>
        dispatcher.dispatch(job).flatMap:
          case DispatchOutcome.Accepted =>
            fuccess(Right(job))
          case DispatchOutcome.Failed(message) =>
            if workerEnabled then
              logger.warn(s"accountintel dispatch degraded job=${job.id} err=$message")
              fuccess(Right(job))
            else
              repo
                .markFailed(job.id, "dispatch_failed", message)
                .inject(Left("Account notebook worker is unavailable. Please try again later."))
      .recoverWith:
        case err if isActiveDedupeConflict(err) =>
          repo.latestActiveBuildByDedupeKey(job.dedupeKey).flatMap:
            case Some(build) => attachWatchingRun(ownerId, provider, username, kind, build, refreshLockedUntil)
            case None        => fuccess(Left("Account notebook run is already in progress."))
        case err =>
          fufail(err)

  private def attachWatchingRun(
      ownerId: UserId,
      provider: String,
      username: String,
      kind: ProductKind,
      sourceJob: AccountIntelJob,
      refreshLockedUntil: Option[Instant]
  ): Fu[Either[String, AccountIntelJob]] =
    val attached =
      AccountIntelJob.newAttached(ownerId.value, provider, username, kind, sourceJob, refreshLockedUntil)
    repo.insert(attached).inject(Right(attached))

  private def attachCached(
      ownerId: UserId,
      provider: String,
      username: String,
      kind: ProductKind,
      snapshot: AccountIntelSurfaceSnapshot,
      refreshLockedUntil: Option[Instant]
  ): Fu[Either[String, AccountIntelJob]] =
    val cached =
      AccountIntelJob.newCached(ownerId.value, provider, username, kind, snapshot, refreshLockedUntil)
    repo.insert(cached).inject(Right(cached))

  private def withNormalizedOwnerKey[A](
      ownerId: UserId,
      provider: String,
      username: String,
      kind: ProductKind
  )(f: String => Fu[A]): Fu[Either[String, A]] =
    normalizedScope(provider, username, kind).fold(
      err => fuccess(Left(err)),
      { case (safeProvider, safeUsername, _) =>
        val ownerKey = AccountIntelJob.ownerScopeKey(ownerId.value, safeProvider, safeUsername, kind)
        f(ownerKey).map(Right.apply)
      }
    )

  private def normalizedScope(
      provider: String,
      username: String,
      kind: ProductKind
  ): Either[String, (String, String, String)] =
    (for
      safeProvider <- normalizeProvider(provider).toRight("Unsupported provider.")
      safeUsername <- normalizeUsername(username).toRight("Invalid username.")
    yield (
      safeProvider,
      safeUsername,
      AccountIntelJob.dedupeKey(safeProvider, safeUsername, kind)
    ))

  private def isActiveDedupeConflict(err: Throwable): Boolean =
    Option(err.getMessage).exists(_.contains("E11000"))
