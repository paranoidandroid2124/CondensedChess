package lila.accountintel

import java.time.Instant
import scala.concurrent.duration.*
import reactivemongo.api.indexes.{ Index, IndexType }

import lila.accountintel.AccountIntel.{ AccountIntelJob, JobStatus }
import lila.db.dsl.{ *, given }

final class AccountIntelJobRepo(coll: Coll)(using Executor) extends AccountIntel.AccountIntelJobStore:

  import BSONHandlers.given

  ensureIndexes()

  private val logger = lila.log("accountIntelJob")

  private object F:
    val ownerId = "ownerId"
    val ownerScopeKey = "ownerScopeKey"
    val buildOwner = "buildOwner"
    val buildSourceId = "buildSourceId"
    val surfaceId = "surfaceId"
    val sourceFingerprint = "sourceFingerprint"
    val status = "status"
    val dedupeKey = "dedupeKey"
    val updatedAt = "updatedAt"
    val requestedAt = "requestedAt"
    val startedAt = "startedAt"
    val finishedAt = "finishedAt"
    val attemptCount = "attemptCount"
    val progressStage = "progressStage"
    val queueState = "queueState"
    val snapshotState = "snapshotState"
    val processedGames = "processedGames"
    val totalGames = "totalGames"
    val etaSec = "etaSec"
    val cacheHit = "cacheHit"
    val refreshLockedUntil = "refreshLockedUntil"
    val warnings = "warnings"
    val studyId = "studyId"
    val chapterId = "chapterId"
    val notebookUrl = "notebookUrl"
    val surfaceJson = "surfaceJson"
    val errorCode = "errorCode"
    val errorMessage = "errorMessage"

  def byId(id: String): Fu[Option[AccountIntelJob]] =
    coll.one[AccountIntelJob]($id(id))

  def insert(job: AccountIntelJob): Funit =
    coll.insert.one(job).void

  def latestByOwnerScopeKey(ownerScopeKey: String): Fu[Option[AccountIntelJob]] =
    coll
      .find($doc(F.ownerScopeKey -> ownerScopeKey))
      .sort($sort.desc(F.updatedAt))
      .one[AccountIntelJob]

  def latestSucceededByOwnerScopeKey(ownerScopeKey: String): Fu[Option[AccountIntelJob]] =
    coll
      .find($doc(F.ownerScopeKey -> ownerScopeKey, F.status -> JobStatus.Succeeded.key))
      .sort($sort.desc(F.updatedAt))
      .one[AccountIntelJob]

  def latestActiveByOwnerScopeKey(ownerScopeKey: String): Fu[Option[AccountIntelJob]] =
    coll
      .find(
        $doc(
          F.ownerScopeKey -> ownerScopeKey,
          F.status.$in(List(JobStatus.Queued.key, JobStatus.Running.key))
        )
      )
      .sort($sort.desc(F.updatedAt))
      .one[AccountIntelJob]

  def latestActiveBuildByDedupeKey(dedupeKey: String): Fu[Option[AccountIntelJob]] =
    coll
      .find(
        $doc(
          F.dedupeKey -> dedupeKey,
          F.buildOwner -> true,
          F.status.$in(List(JobStatus.Queued.key, JobStatus.Running.key))
        )
      )
      .sort($sort.desc(F.updatedAt))
      .one[AccountIntelJob]

  def recentByOwnerScopeKey(ownerScopeKey: String, limit: Int): Fu[List[AccountIntelJob]] =
    coll
      .find($doc(F.ownerScopeKey -> ownerScopeKey), none[Bdoc])
      .sort($sort.desc(F.updatedAt))
      .cursor[AccountIntelJob]()
      .list(limit)

  def recentSucceededByOwner(ownerId: String, limit: Int): Fu[List[AccountIntelJob]] =
    coll
      .find($doc(F.ownerId -> ownerId, F.status -> JobStatus.Succeeded.key), none[Bdoc])
      .sort($sort.desc(F.updatedAt))
      .cursor[AccountIntelJob]()
      .list(limit)

  def claimNextQueued(now: Instant): Fu[Option[AccountIntelJob]] =
    coll.findAndUpdateSimplified[AccountIntelJob](
      selector = $doc(F.status -> JobStatus.Queued.key, F.buildOwner -> true),
      update = $set(
        F.status -> JobStatus.Running.key,
        F.progressStage -> "fetching_games",
        F.queueState -> "running",
        F.snapshotState -> "fetching_games",
        F.etaSec -> 150,
        F.startedAt -> now,
        F.updatedAt -> now
      ) ++ $inc(F.attemptCount -> 1),
      fetchNewObject = true,
      sort = $sort.asc(F.requestedAt).some
    )

  def claimQueuedById(id: String, now: Instant): Fu[Option[AccountIntelJob]] =
    coll.findAndUpdateSimplified[AccountIntelJob](
      selector = $id(id) ++ $doc(F.status -> JobStatus.Queued.key, F.buildOwner -> true),
      update = $set(
        F.status -> JobStatus.Running.key,
        F.progressStage -> "fetching_games",
        F.queueState -> "running",
        F.snapshotState -> "fetching_games",
        F.etaSec -> 150,
        F.startedAt -> now,
        F.updatedAt -> now
      ) ++ $inc(F.attemptCount -> 1),
      fetchNewObject = true
    )

  def setProgress(
      id: String,
      progressStage: String,
      snapshotState: String,
      processedGames: Option[Int],
      totalGames: Option[Int],
      etaSec: Option[Int]
  ): Funit =
    val setDoc = $set(
      F.progressStage -> progressStage,
      F.snapshotState -> snapshotState,
      F.updatedAt -> nowInstant
    ) ++ processedGames.fold($doc())(value => $set(F.processedGames -> value)) ++
      totalGames.fold($doc())(value => $set(F.totalGames -> value)) ++
      etaSec.fold($unset(F.etaSec))(value => $set(F.etaSec -> value))
    coll.update.one($id(id), setDoc).void >>
      mirrorFollowers(
        id,
        $set(
          F.status -> JobStatus.Running.key,
          F.progressStage -> progressStage,
          F.queueState -> "running",
          F.snapshotState -> snapshotState,
          F.updatedAt -> nowInstant
        ) ++ processedGames.fold($doc())(value => $set(F.processedGames -> value)) ++
          totalGames.fold($doc())(value => $set(F.totalGames -> value)) ++
          etaSec.fold($unset(F.etaSec))(value => $set(F.etaSec -> value))
      )

  def markSucceeded(
      id: String,
      surfaceId: String,
      sourceFingerprint: String,
      warnings: List[String],
      surfaceJson: String,
      sampledGameCount: Int,
      totalGames: Int,
      cacheHit: Boolean,
      refreshLockedUntil: Option[Instant]
  ): Funit =
    val now = nowInstant
    val update = $set(
      F.status -> JobStatus.Succeeded.key,
      F.progressStage -> "completed",
      F.queueState -> "ready",
      F.snapshotState -> "ready",
      F.surfaceId -> surfaceId,
      F.sourceFingerprint -> sourceFingerprint,
      F.surfaceJson -> surfaceJson,
      F.warnings -> warnings,
      F.processedGames -> sampledGameCount,
      F.totalGames -> totalGames,
      F.etaSec -> 0,
      F.cacheHit -> cacheHit,
      F.refreshLockedUntil -> refreshLockedUntil,
      F.finishedAt -> now,
      F.updatedAt -> now
    ) ++ $unset(F.errorCode, F.errorMessage)
    coll.update.one($id(id), update).void >>
      mirrorFollowers(
        id,
        $set(
          F.status -> JobStatus.Succeeded.key,
          F.progressStage -> "completed",
          F.queueState -> "ready",
          F.snapshotState -> "ready",
          F.surfaceId -> surfaceId,
          F.sourceFingerprint -> sourceFingerprint,
          F.surfaceJson -> surfaceJson,
          F.warnings -> warnings,
          F.processedGames -> sampledGameCount,
          F.totalGames -> totalGames,
          F.etaSec -> 0,
          F.cacheHit -> cacheHit,
          F.refreshLockedUntil -> refreshLockedUntil,
          F.finishedAt -> now,
          F.updatedAt -> now
        ) ++ $unset(F.errorCode, F.errorMessage)
      )

  def setPublication(id: String, studyId: String, chapterId: String, notebookUrl: String): Funit =
    coll.update
      .one(
        $id(id),
        $set(
          F.studyId -> studyId,
          F.chapterId -> chapterId,
          F.notebookUrl -> notebookUrl,
          F.updatedAt -> nowInstant
        )
      )
      .void

  def markFailed(id: String, code: String, message: String): Funit =
    val now = nowInstant
    val update = $set(
      F.status -> JobStatus.Failed.key,
      F.progressStage -> "failed",
      F.queueState -> "failed",
      F.snapshotState -> "failed",
      F.errorCode -> code,
      F.errorMessage -> message.take(500),
      F.finishedAt -> now,
      F.updatedAt -> now
    ) ++ $unset(F.etaSec)
    coll.update.one($id(id), update).void >>
      mirrorFollowers(
        id,
        $set(
          F.status -> JobStatus.Failed.key,
          F.progressStage -> "failed",
          F.queueState -> "failed",
          F.snapshotState -> "failed",
          F.errorCode -> code,
          F.errorMessage -> message.take(500),
          F.finishedAt -> now,
          F.updatedAt -> now
        ) ++ $unset(F.etaSec)
      )

  def requeueTimedOutRunning(timeout: FiniteDuration, retryLimit: Int): Funit =
    val now = nowInstant
    coll
      .find(
        $doc(
          F.status -> JobStatus.Running.key,
          F.buildOwner -> true,
          F.updatedAt.$lt(now.minusMillis(timeout.toMillis))
        )
      )
      .cursor[AccountIntelJob]()
      .listAll()
      .flatMap: jobs =>
        jobs.sequentiallyVoid: job =>
          if AccountIntelJob.timedOutRunning(job, now, timeout) && job.attemptCount < retryLimit then
            val update = $set(
              F.status -> JobStatus.Queued.key,
              F.progressStage -> "queued",
              F.queueState -> "queued",
              F.snapshotState -> "queued",
              F.etaSec -> 180,
              F.updatedAt -> now
            ) ++ $unset(F.errorCode, F.errorMessage, F.finishedAt)
            coll.update.one($id(job.id), update).void >>
              mirrorFollowers(
                job.id,
                $set(
                  F.status -> JobStatus.Queued.key,
                  F.progressStage -> "queued",
                  F.queueState -> "queued",
                  F.snapshotState -> "queued",
                  F.etaSec -> 180,
                  F.updatedAt -> now
                ) ++ $unset(F.errorCode, F.errorMessage, F.finishedAt)
              )
          else markFailed(job.id, "timeout", "Account notebook build timed out.")

  def cleanupExpired(successTtl: FiniteDuration, failedTtl: FiniteDuration): Funit =
    val now = nowInstant
    val successCutoff = now.minusMillis(successTtl.toMillis)
    val failedCutoff = now.minusMillis(failedTtl.toMillis)
    coll.delete
      .one(
        $or(
          $doc(F.status -> JobStatus.Succeeded.key, F.updatedAt.$lt(successCutoff)),
          $doc(F.status -> JobStatus.Failed.key, F.updatedAt.$lt(failedCutoff))
        )
      )
      .void
      .recover:
        case err =>
          logger.warn(s"accountintel cleanup failed: ${err.getMessage}")

  private def mirrorFollowers(buildId: String, update: Bdoc): Funit =
    coll.update
      .one(
        $doc(F.buildSourceId -> buildId, F.buildOwner -> false),
        update,
        multi = true
      )
      .void

  private def ensureIndexes(): Unit =
    coll.indexesManager.ensure(
      Index(
        key = Seq(F.ownerScopeKey -> IndexType.Ascending, F.updatedAt -> IndexType.Descending),
        name = Some("owner_scope_updated")
      )
    )
    coll.indexesManager.ensure(
      Index(
        key = Seq(F.dedupeKey -> IndexType.Ascending, F.updatedAt -> IndexType.Descending),
        name = Some("dedupe_updated")
      )
    )
    coll.indexesManager.ensure(
      Index(
        key = Seq(F.status -> IndexType.Ascending, F.requestedAt -> IndexType.Ascending),
        name = Some("status_requested")
      )
    )
    coll.indexesManager.ensure(
      Index(
        key = Seq(F.dedupeKey -> IndexType.Ascending),
        name = Some("active_build_unique"),
        unique = true,
        options = $doc(
          "partialFilterExpression" -> $doc(
            F.buildOwner -> true,
            F.status -> $doc("$in" -> List(JobStatus.Queued.key, JobStatus.Running.key))
          )
        )
      )
    )
    coll.indexesManager.ensure(
      Index(
        key = Seq(F.ownerId -> IndexType.Ascending, F.status -> IndexType.Ascending, F.updatedAt -> IndexType.Descending),
        name = Some("owner_status_updated")
      )
    )
    coll.indexesManager.ensure(
      Index(
        key = Seq(F.buildSourceId -> IndexType.Ascending, F.updatedAt -> IndexType.Descending),
        name = Some("build_source_updated")
      )
    )
