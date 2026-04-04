package lila.accountintel

import reactivemongo.api.bson.*

import java.time.Instant
import scala.util.Try

import lila.accountintel.AccountIntel.{ AccountIntelJob, AccountIntelSurfaceSnapshot, JobStatus, ProductKind }
import lila.db.dsl.{ *, given }

object BSONHandlers:

  private given BSONHandler[ProductKind] = quickHandler(
    {
      case BSONString(value) => ProductKind.fromKey(value).getOrElse(ProductKind.MyAccountIntelligenceLite)
      case _                 => ProductKind.MyAccountIntelligenceLite
    },
    kind => BSONString(kind.key)
  )

  private given BSONHandler[JobStatus] = quickHandler(
    {
      case BSONString(value) => JobStatus.fromKey(value).getOrElse(JobStatus.Queued)
      case _                 => JobStatus.Queued
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
      dedupeKey = doc
        .getAsOpt[String]("dedupeKey")
        .getOrElse(AccountIntel.AccountIntelJob.dedupeKey(provider, username, kind))
      ownerScopeKey = doc
        .getAsOpt[String]("ownerScopeKey")
        .getOrElse(AccountIntel.AccountIntelJob.ownerScopeKey(ownerId, provider, username, kind))
      buildOwner = doc.getAsOpt[Boolean]("buildOwner").getOrElse(true)
      buildSourceId = doc.getAsOpt[String]("buildSourceId")
      surfaceId = doc.getAsOpt[String]("surfaceId")
      sourceFingerprint = doc.getAsOpt[String]("sourceFingerprint")
      progressStage <- doc.getAsTry[String]("progressStage")
      queueState = doc.getAsOpt[String]("queueState").getOrElse(status match
        case JobStatus.Queued    => "queued"
        case JobStatus.Running   => "running"
        case JobStatus.Succeeded => "ready"
        case JobStatus.Failed    => "failed"
      )
      snapshotState = doc.getAsOpt[String]("snapshotState").getOrElse(progressStage)
      processedGames = doc.getAsOpt[Int]("processedGames").getOrElse(0)
      totalGames = doc.getAsOpt[Int]("totalGames")
      etaSec = doc.getAsOpt[Int]("etaSec")
      cacheHit = doc.getAsOpt[Boolean]("cacheHit").getOrElse(false)
      refreshLockedUntil = doc.getAsOpt[Instant]("refreshLockedUntil")
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
      ownerScopeKey = ownerScopeKey,
      buildOwner = buildOwner,
      buildSourceId = buildSourceId,
      surfaceId = surfaceId,
      sourceFingerprint = sourceFingerprint,
      progressStage = progressStage,
      queueState = queueState,
      snapshotState = snapshotState,
      processedGames = processedGames,
      totalGames = totalGames,
      etaSec = etaSec,
      cacheHit = cacheHit,
      refreshLockedUntil = refreshLockedUntil,
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
        "ownerScopeKey" -> job.ownerScopeKey,
        "buildOwner" -> job.buildOwner,
        "buildSourceId" -> job.buildSourceId,
        "surfaceId" -> job.surfaceId,
        "sourceFingerprint" -> job.sourceFingerprint,
        "progressStage" -> job.progressStage,
        "queueState" -> job.queueState,
        "snapshotState" -> job.snapshotState,
        "processedGames" -> job.processedGames,
        "totalGames" -> job.totalGames,
        "etaSec" -> job.etaSec,
        "cacheHit" -> job.cacheHit,
        "refreshLockedUntil" -> job.refreshLockedUntil,
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

  given BSONDocumentReader[AccountIntelSurfaceSnapshot] =
    BSONDocumentReader.from[AccountIntelSurfaceSnapshot]: doc =>
      for
        id <- doc.getAsTry[String]("_id")
        dedupeKey <- doc.getAsTry[String]("dedupeKey")
        provider <- doc.getAsTry[String]("provider")
        username <- doc.getAsTry[String]("username")
        kind <- doc.getAsTry[ProductKind]("kind")
        sourceFingerprint <- doc.getAsTry[String]("sourceFingerprint")
        surfaceJson <- doc.getAsTry[String]("surfaceJson")
        dossierJson <- doc.getAsTry[String]("dossierJson")
        representativePgn <- doc.getAsTry[String]("representativePgn")
        sampledGameCount = doc.getAsOpt[Int]("sampledGameCount").getOrElse(0)
        eligibleGameCount = doc.getAsOpt[Int]("eligibleGameCount").getOrElse(0)
        warnings = doc.getAsOpt[List[String]]("warnings").getOrElse(Nil)
        createdAt <- doc.getAsTry[Instant]("createdAt")
        updatedAt <- doc.getAsTry[Instant]("updatedAt")
      yield AccountIntelSurfaceSnapshot(
        id = id,
        dedupeKey = dedupeKey,
        provider = provider,
        username = username,
        kind = kind,
        sourceFingerprint = sourceFingerprint,
        surfaceJson = surfaceJson,
        dossierJson = dossierJson,
        representativePgn = representativePgn,
        sampledGameCount = sampledGameCount,
        eligibleGameCount = eligibleGameCount,
        warnings = warnings,
        createdAt = createdAt,
        updatedAt = updatedAt
      )

  given BSONDocumentWriter[AccountIntelSurfaceSnapshot] =
    BSONDocumentWriter.from[AccountIntelSurfaceSnapshot]: snapshot =>
      Try:
        BSONDocument(
          "_id" -> snapshot.id,
          "dedupeKey" -> snapshot.dedupeKey,
          "provider" -> snapshot.provider,
          "username" -> snapshot.username,
          "kind" -> snapshot.kind,
          "sourceFingerprint" -> snapshot.sourceFingerprint,
          "surfaceJson" -> snapshot.surfaceJson,
          "dossierJson" -> snapshot.dossierJson,
          "representativePgn" -> snapshot.representativePgn,
          "sampledGameCount" -> snapshot.sampledGameCount,
          "eligibleGameCount" -> snapshot.eligibleGameCount,
          "warnings" -> snapshot.warnings,
          "createdAt" -> snapshot.createdAt,
          "updatedAt" -> snapshot.updatedAt
        )
