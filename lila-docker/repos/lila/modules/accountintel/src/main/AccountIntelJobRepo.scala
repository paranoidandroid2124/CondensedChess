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
    val status = "status"
    val dedupeKey = "dedupeKey"
    val updatedAt = "updatedAt"
    val requestedAt = "requestedAt"
    val startedAt = "startedAt"
    val finishedAt = "finishedAt"
    val attemptCount = "attemptCount"
    val progressStage = "progressStage"
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

  def latestByDedupeKey(dedupeKey: String): Fu[Option[AccountIntelJob]] =
    coll
      .find($doc(F.dedupeKey -> dedupeKey))
      .sort($sort.desc(F.updatedAt))
      .one[AccountIntelJob]

  def latestSucceededByDedupeKey(dedupeKey: String): Fu[Option[AccountIntelJob]] =
    coll
      .find($doc(F.dedupeKey -> dedupeKey, F.status -> JobStatus.Succeeded.key))
      .sort($sort.desc(F.updatedAt))
      .one[AccountIntelJob]

  def latestActiveByDedupeKey(dedupeKey: String): Fu[Option[AccountIntelJob]] =
    coll
      .find(
        $doc(
          F.dedupeKey -> dedupeKey,
          F.status.$in(List(JobStatus.Queued.key, JobStatus.Running.key))
        )
      )
      .sort($sort.desc(F.updatedAt))
      .one[AccountIntelJob]

  def recentByDedupeKey(dedupeKey: String, limit: Int): Fu[List[AccountIntelJob]] =
    coll
      .find($doc(F.dedupeKey -> dedupeKey), none[Bdoc])
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
      selector = $doc(F.status -> JobStatus.Queued.key),
      update = $set(
        F.status -> JobStatus.Running.key,
        F.progressStage -> "fetching_games",
        F.startedAt -> now,
        F.updatedAt -> now
      ) ++ $inc(F.attemptCount -> 1),
      fetchNewObject = true,
      sort = $sort.asc(F.requestedAt).some
    )

  def claimQueuedById(id: String, now: Instant): Fu[Option[AccountIntelJob]] =
    coll.findAndUpdateSimplified[AccountIntelJob](
      selector = $id(id) ++ $doc(F.status -> JobStatus.Queued.key),
      update = $set(
        F.status -> JobStatus.Running.key,
        F.progressStage -> "fetching_games",
        F.startedAt -> now,
        F.updatedAt -> now
      ) ++ $inc(F.attemptCount -> 1),
      fetchNewObject = true
    )

  def setProgress(id: String, progressStage: String): Funit =
    coll.update
      .one(
        $id(id),
        $set(F.progressStage -> progressStage, F.updatedAt -> nowInstant)
      )
      .void

  def markSucceeded(
      id: String,
      studyId: String,
      chapterId: String,
      notebookUrl: String,
      warnings: List[String],
      surfaceJson: String
  ): Funit =
    coll.update
      .one(
        $id(id),
        $set(
          F.status -> JobStatus.Succeeded.key,
          F.progressStage -> "completed",
          F.studyId -> studyId,
          F.chapterId -> chapterId,
          F.notebookUrl -> notebookUrl,
          F.surfaceJson -> surfaceJson,
          F.warnings -> warnings,
          F.finishedAt -> nowInstant,
          F.updatedAt -> nowInstant
        ) ++ $unset(F.errorCode, F.errorMessage)
      )
      .void

  def markFailed(id: String, code: String, message: String): Funit =
    coll.update
      .one(
        $id(id),
        $set(
          F.status -> JobStatus.Failed.key,
          F.progressStage -> "failed",
          F.errorCode -> code,
          F.errorMessage -> message.take(500),
          F.finishedAt -> nowInstant,
          F.updatedAt -> nowInstant
        )
      )
      .void

  def requeueTimedOutRunning(timeout: FiniteDuration, retryLimit: Int): Funit =
    val now = nowInstant
    coll
      .find(
        $doc(
          F.status -> JobStatus.Running.key,
          F.updatedAt.$lt(now.minusMillis(timeout.toMillis))
        )
      )
      .cursor[AccountIntelJob]()
      .listAll()
      .flatMap: jobs =>
        jobs.sequentiallyVoid: job =>
          if AccountIntelJob.timedOutRunning(job, now, timeout) && job.attemptCount < retryLimit then
            coll.update
              .one(
                $id(job.id),
                $set(
                  F.status -> JobStatus.Queued.key,
                  F.progressStage -> "queued",
                  F.updatedAt -> nowInstant
                ) ++ $unset(F.errorCode, F.errorMessage, F.finishedAt)
              )
              .void
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

  private def ensureIndexes(): Unit =
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
        name = Some("active_dedupe_unique"),
        unique = true,
        options = $doc(
          "partialFilterExpression" -> $doc(
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
