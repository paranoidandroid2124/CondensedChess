package lila.accountintel.service

import play.api.libs.json.Json

import lila.accountintel.*
import lila.accountintel.AccountIntel.*
import lila.core.study.Visibility

case class NotebookPersisted(studyId: String, chapterId: String, notebookUrl: String)
case class NotebookPersistFailure(code: String, message: String)

trait NotebookSink:
  def persist(job: AccountIntelJob, artifact: NotebookBuildArtifact): Fu[Either[NotebookPersistFailure, NotebookPersisted]]

final class StudyNotebookSink(
    studyApi: lila.study.StudyApi,
    userApi: lila.core.user.UserApi
)(using Executor)
    extends NotebookSink:

  def persist(
      job: AccountIntelJob,
      artifact: NotebookBuildArtifact
  ): Fu[Either[NotebookPersistFailure, NotebookPersisted]] =
    userApi
      .enabledById(UserId(job.ownerId))
      .flatMap:
        case None =>
          fuccess(Left(NotebookPersistFailure("owner_missing", "The account that requested this notebook is no longer available.")))
        case Some(user) =>
          val importData = lila.study.StudyMaker.ImportGame(
            form = lila.study.StudyForm.importGame.Data(
              pgnStr = artifact.representativePgn.some
            ),
            name = notebookName(job.username, job.kind).some
          )
          studyApi
            .create(
              importData,
              user,
              withRatings = true,
              transform = _.copy(
                name = notebookName(job.username, job.kind),
                visibility = Visibility.`private`
              )
            )
            .flatMap:
              case None =>
                fuccess(Left(NotebookPersistFailure("study_create_failed", "Notebook study creation failed.")))
              case Some(sc) =>
                val serialized = Json.stringify(artifact.dossier)
                studyApi
                  .setNotebookDossier(sc.study.id, serialized.some)(lila.study.Who(user.id))
                  .map: _ =>
                    Right(
                      NotebookPersisted(
                        studyId = sc.study.id.value,
                        chapterId = sc.chapter.id.value,
                        notebookUrl = s"/notebook/${sc.study.id.value}/${sc.chapter.id.value}"
                      )
                    )
