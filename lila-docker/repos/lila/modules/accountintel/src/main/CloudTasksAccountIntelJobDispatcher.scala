package lila.accountintel

import play.api.Configuration
import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.StandaloneWSClient

import scala.concurrent.duration.*
import scala.util.control.NonFatal

import lila.accountintel.AccountIntel.{ AccountIntelJob, AccountIntelJobDispatcher, DispatchOutcome }

final class CloudTasksAccountIntelJobDispatcher(
    appConfig: Configuration,
    ws: StandaloneWSClient
)(using Executor)
    extends AccountIntelJobDispatcher:

  private val projectId =
    appConfig.getOptional[String]("accountIntel.dispatch.cloudTasks.projectId").map(_.trim).filter(_.nonEmpty)
  private val location =
    appConfig.getOptional[String]("accountIntel.dispatch.cloudTasks.location").map(_.trim).filter(_.nonEmpty)
  private val queue =
    appConfig.getOptional[String]("accountIntel.dispatch.cloudTasks.queue").map(_.trim).filter(_.nonEmpty)
  private val workerUrl =
    appConfig
      .getOptional[String]("accountIntel.dispatch.cloudTasks.workerUrl")
      .map(_.trim.stripSuffix("/"))
      .filter(_.nonEmpty)
  private val serviceAccountEmail =
    appConfig
      .getOptional[String]("accountIntel.dispatch.cloudTasks.serviceAccountEmail")
      .map(_.trim)
      .filter(_.nonEmpty)
  private val audience =
    appConfig
      .getOptional[String]("accountIntel.dispatch.cloudTasks.audience")
      .map(_.trim)
      .filter(_.nonEmpty)
  private val timeout =
    appConfig.getOptional[FiniteDuration]("accountIntel.dispatch.timeout").getOrElse(4.seconds)
  private val metadataTokenUrl =
    appConfig
      .getOptional[String]("accountIntel.dispatch.cloudTasks.metadataTokenUrl")
      .getOrElse("http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token")

  def dispatch(job: AccountIntelJob): Fu[DispatchOutcome] =
    (for
      projectId <- projectId
      location <- location
      queue <- queue
      workerUrl <- workerUrl
      serviceAccountEmail <- serviceAccountEmail
    yield (projectId, location, queue, workerUrl, serviceAccountEmail)) match
      case None =>
        fuccess(DispatchOutcome.Failed("Cloud Tasks dispatch is not configured."))
      case Some((projectId, location, queue, workerUrl, serviceAccountEmail)) =>
        for
          accessToken <- fetchAccessToken()
          outcome <- createTask(job, projectId, location, queue, workerUrl, serviceAccountEmail, accessToken)
        yield outcome

  private def fetchAccessToken(): Fu[String] =
    ws.url(metadataTokenUrl)
      .withHttpHeaders("Metadata-Flavor" -> "Google")
      .withRequestTimeout(timeout)
      .get()
      .flatMap: response =>
        val token = (Json.parse(response.body) \ "access_token").asOpt[String].filter(_.nonEmpty)
        token match
          case Some(token) => fuccess(token)
          case None =>
            fufail(new RuntimeException(s"Cloud Tasks metadata token fetch failed with HTTP ${response.status}."))

  private def createTask(
      job: AccountIntelJob,
      projectId: String,
      location: String,
      queue: String,
      workerUrl: String,
      serviceAccountEmail: String,
      accessToken: String
  ): Fu[DispatchOutcome] =
    val url = s"https://cloudtasks.googleapis.com/v2/projects/$projectId/locations/$location/queues/$queue/tasks"
    val targetUrl = s"$workerUrl/internal/account-intel/jobs/${job.id}/run"
    val oidcToken =
      audience.fold(Json.obj("serviceAccountEmail" -> serviceAccountEmail)): aud =>
        Json.obj("serviceAccountEmail" -> serviceAccountEmail, "audience" -> aud)
    val body = Json.obj(
      "task" -> Json.obj(
        "httpRequest" -> Json.obj(
          "httpMethod" -> "POST",
          "url" -> targetUrl,
          "headers" -> Json.obj("Content-Type" -> "application/json"),
          "body" -> java.util.Base64.getEncoder.encodeToString("{}".getBytes("UTF-8")),
          "oidcToken" -> oidcToken
        )
      )
    )
    ws.url(url)
      .withHttpHeaders(
        "Authorization" -> s"Bearer $accessToken",
        "Content-Type" -> "application/json"
      )
      .withRequestTimeout(timeout)
      .post(body)
      .map: response =>
        if response.status / 100 == 2 then DispatchOutcome.Accepted
        else
          val msg = s"Cloud Tasks dispatch returned HTTP ${response.status}."
          logger.warn(s"accountintel cloud tasks dispatch failed status=${response.status} job=${job.id}")
          DispatchOutcome.Failed(msg)
      .recover:
        case NonFatal(err) =>
          logger.warn(s"accountintel cloud tasks dispatch exception job=${job.id} err=${err.getMessage}")
          DispatchOutcome.Failed(err.getMessage)
