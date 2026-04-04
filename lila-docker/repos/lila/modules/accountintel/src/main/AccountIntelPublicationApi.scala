package lila.accountintel

import lila.accountintel.service.{ NotebookPersistFailure, NotebookPersisted, StudyNotebookSink }

final class AccountIntelPublicationApi(
    repo: AccountIntel.AccountIntelJobStore,
    surfaceStore: AccountIntel.AccountIntelSurfaceStore,
    studyNotebookSink: StudyNotebookSink
)(using Executor):

  def publish(ownerId: UserId, jobId: String): Fu[Either[String, NotebookPersisted]] =
    repo.byId(jobId).flatMap:
      case Some(job) if job.ownerId == ownerId.value =>
        job.notebookUrl match
          case Some(url) if job.studyId.isDefined && job.chapterId.isDefined =>
            fuccess(
              Right(
                NotebookPersisted(
                  studyId = job.studyId.getOrElse(""),
                  chapterId = job.chapterId.getOrElse(""),
                  notebookUrl = url
                )
              )
            )
          case _ =>
            job.surfaceId match
              case None =>
                fuccess(Left("This report does not have a stored surface to publish yet."))
              case Some(surfaceId) if job.status != AccountIntel.JobStatus.Succeeded =>
                fuccess(Left("Only completed reports can be published into a study notebook."))
              case Some(surfaceId) =>
                surfaceStore.byId(surfaceId).flatMap:
                  case None => fuccess(Left("The stored report snapshot is no longer available."))
                  case Some(snapshot) =>
                    studyNotebookSink.persistStored(job, snapshot).flatMap:
                      case Left(NotebookPersistFailure(_, message)) => fuccess(Left(message))
                      case Right(persisted) =>
                        repo
                          .setPublication(job.id, persisted.studyId, persisted.chapterId, persisted.notebookUrl)
                          .inject(Right(persisted))
      case _ => fuccess(Left("Account report not found."))
