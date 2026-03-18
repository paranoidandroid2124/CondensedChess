package lila.accountintel

import play.api.Configuration

import scala.concurrent.duration.*

import lila.accountintel.AccountIntel.*

final class AccountIntelApi(
    appConfig: Configuration,
    repo: AccountIntelJobStore,
    dispatcher: AccountIntelJobDispatcher
)(using Executor):

  private val successTtl =
    appConfig.getOptional[FiniteDuration]("accountIntel.job.successTtl").getOrElse(24.hours)
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
    (for
      safeProvider <- normalizeProvider(provider).toRight("Unsupported provider.")
      safeUsername <- normalizeUsername(username).toRight("Invalid username.")
    yield (safeProvider, safeUsername)).fold(
      err => fuccess(Left(err)),
      { case (safeProvider, safeUsername) =>
        val now = nowInstant
        val dedupe = AccountIntelJob.dedupeKey(ownerId.value, safeProvider, safeUsername, kind)
        if force then enqueue(ownerId, safeProvider, safeUsername, kind)
        else
          repo
            .latestByDedupeKey(dedupe)
            .flatMap:
              case Some(job) if AccountIntelJob.canReuse(job, now, successTtl, failedCooldown) =>
                fuccess(Right(job))
              case Some(job) if AccountIntelJob.retryableFailed(job, now, failedCooldown, retryLimit) =>
                enqueue(ownerId, safeProvider, safeUsername, kind)
              case Some(job) if job.status == JobStatus.Failed =>
                fuccess(Right(job))
              case _ =>
                enqueue(ownerId, safeProvider, safeUsername, kind)
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
    withNormalizedKey(ownerId, provider, username, kind): dedupe =>
      repo.byId(jobId).map(_.filter(job => job.ownerId == ownerId.value && job.dedupeKey == dedupe))

  def latestSuccessful(
      ownerId: UserId,
      provider: String,
      username: String,
      kind: ProductKind
  ): Fu[Either[String, Option[AccountIntelJob]]] =
    withNormalizedKey(ownerId, provider, username, kind): dedupe =>
      repo.latestSucceededByDedupeKey(dedupe)

  def latestActive(
      ownerId: UserId,
      provider: String,
      username: String,
      kind: ProductKind
  ): Fu[Either[String, Option[AccountIntelJob]]] =
    withNormalizedKey(ownerId, provider, username, kind): dedupe =>
      repo.latestActiveByDedupeKey(dedupe)

  def history(
      ownerId: UserId,
      provider: String,
      username: String,
      kind: ProductKind,
      limit: Int = 10
  ): Fu[Either[String, List[AccountIntelJob]]] =
    withNormalizedKey(ownerId, provider, username, kind): dedupe =>
      repo.recentByDedupeKey(dedupe, limit)

  def recentSuccessful(ownerId: UserId, limit: Int = 6): Fu[List[AccountIntelJob]] =
    repo.recentSucceededByOwner(ownerId.value, limit)

  private def enqueue(
      ownerId: UserId,
      provider: String,
      username: String,
      kind: ProductKind
  ): Fu[Either[String, AccountIntelJob]] =
    val job = AccountIntelJob.newQueued(ownerId.value, provider, username, kind)
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
          repo.latestActiveByDedupeKey(job.dedupeKey).map:
            _.toRight("Account notebook run is already in progress.")
        case err =>
          fufail(err)

  private def withNormalizedKey[A](
      ownerId: UserId,
      provider: String,
      username: String,
      kind: ProductKind
  )(f: String => Fu[A]): Fu[Either[String, A]] =
    (for
      safeProvider <- normalizeProvider(provider).toRight("Unsupported provider.")
      safeUsername <- normalizeUsername(username).toRight("Invalid username.")
    yield AccountIntelJob.dedupeKey(ownerId.value, safeProvider, safeUsername, kind)).fold(
      err => fuccess(Left(err)),
      dedupe => f(dedupe).map(Right.apply)
    )

  private def isActiveDedupeConflict(err: Throwable): Boolean =
    Option(err.getMessage).exists(_.contains("E11000"))
