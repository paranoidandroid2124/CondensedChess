package lila.accountintel

import reactivemongo.api.bson.*

import java.time.Instant
import scala.util.Try

import lila.accountintel.AccountIntel.{ AccountIntelJob, JobStatus, ProductKind }
import lila.db.dsl.{ *, given }

object BSONHandlers:

  private given BSONHandler[ProductKind] = quickHandler(
    {
      case BSONString(value) => ProductKind.fromKey(value).getOrElse(ProductKind.MyAccountIntelligenceLite)
      case _ => ProductKind.MyAccountIntelligenceLite
    },
    kind => BSONString(kind.key)
  )

  private given BSONHandler[JobStatus] = quickHandler(
    {
      case BSONString(value) => JobStatus.fromKey(value).getOrElse(JobStatus.Queued)
      case _ => JobStatus.Queued
    },
    status => BSONString(status.key)
  )

  given BSONDocumentReader[AccountIntelJob] = BSONDocumentReader.from[AccountIntelJob]: doc =>
    for
      id <- doc.getAsTry[String]("_id")
      ownerId <- doc.getAsTry[String]("ownerId")
      provider <- doc.getAsTry[String]("provider")
      username <- doc.getAsTry[String]("username")
      kind <- doc.getAsTry[ProductKind]("kind")
      status <- doc.getAsTry[JobStatus]("status")
      dedupeKey <- doc.getAsTry[String]("dedupeKey")
      progressStage <- doc.getAsTry[String]("progressStage")
      requestedAt <- doc.getAsTry[Instant]("requestedAt")
      startedAt = doc.getAsOpt[Instant]("startedAt")
      finishedAt = doc.getAsOpt[Instant]("finishedAt")
      attemptCount = doc.getAsOpt[Int]("attemptCount").getOrElse(0)
      warnings = doc.getAsOpt[List[String]]("warnings").getOrElse(Nil)
      studyId = doc.getAsOpt[String]("studyId")
      chapterId = doc.getAsOpt[String]("chapterId")
      notebookUrl = doc.getAsOpt[String]("notebookUrl")
      surfaceJson = doc.getAsOpt[String]("surfaceJson")
      errorCode = doc.getAsOpt[String]("errorCode")
      errorMessage = doc.getAsOpt[String]("errorMessage")
      createdAt <- doc.getAsTry[Instant]("createdAt")
      updatedAt <- doc.getAsTry[Instant]("updatedAt")
    yield AccountIntelJob(
      id = id,
      ownerId = ownerId,
      provider = provider,
      username = username,
      kind = kind,
      status = status,
      dedupeKey = dedupeKey,
      progressStage = progressStage,
      requestedAt = requestedAt,
      startedAt = startedAt,
      finishedAt = finishedAt,
      attemptCount = attemptCount,
      warnings = warnings,
      studyId = studyId,
      chapterId = chapterId,
      notebookUrl = notebookUrl,
      surfaceJson = surfaceJson,
      errorCode = errorCode,
      errorMessage = errorMessage,
      createdAt = createdAt,
      updatedAt = updatedAt
    )

  given BSONDocumentWriter[AccountIntelJob] = BSONDocumentWriter.from[AccountIntelJob]: job =>
    Try:
      BSONDocument(
        "_id" -> job.id,
        "ownerId" -> job.ownerId,
        "provider" -> job.provider,
        "username" -> job.username,
        "kind" -> job.kind,
        "status" -> job.status,
        "dedupeKey" -> job.dedupeKey,
        "progressStage" -> job.progressStage,
        "requestedAt" -> job.requestedAt,
        "startedAt" -> job.startedAt,
        "finishedAt" -> job.finishedAt,
        "attemptCount" -> job.attemptCount,
        "warnings" -> job.warnings,
        "studyId" -> job.studyId,
        "chapterId" -> job.chapterId,
        "notebookUrl" -> job.notebookUrl,
        "surfaceJson" -> job.surfaceJson,
        "errorCode" -> job.errorCode,
        "errorMessage" -> job.errorMessage,
        "createdAt" -> job.createdAt,
        "updatedAt" -> job.updatedAt
      )
