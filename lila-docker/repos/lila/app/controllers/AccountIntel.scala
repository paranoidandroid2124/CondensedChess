package controllers

import play.api.libs.json.*
import play.api.mvc.*
import scala.util.Try
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import lila.app.*
import lila.analyse.ImportHistory

object AccountIntel:
  case class ProductState(
      provider: String,
      username: String,
      kind: lila.accountintel.AccountIntel.ProductKind,
      latestSuccessful: Option[lila.accountintel.AccountIntel.AccountIntelJob],
      activeJob: Option[lila.accountintel.AccountIntel.AccountIntelJob],
      history: List[lila.accountintel.AccountIntel.AccountIntelJob],
      displayedJob: Option[lila.accountintel.AccountIntel.AccountIntelJob],
      selectedJobId: Option[String]
  )

final class AccountIntel(
    env: Env
) extends LilaController(env):
  import controllers.AccountIntel.ProductState

  private case class SubmitPayload(provider: String, username: String, kind: String, force: Boolean = false)

  private object SubmitPayload:
    given Reads[SubmitPayload] =
      Reads: js =>
        for
          provider <- (js \ "provider").validate[String]
          username <- (js \ "username").validate[String]
          kind <- (js \ "kind").validate[String]
        yield SubmitPayload(
          provider = provider,
          username = username,
          kind = kind,
          force = (js \ "force").asOpt[Boolean].getOrElse(false)
        )

  private val defaultKind = lila.accountintel.AccountIntel.ProductKind.MyAccountIntelligenceLite
  private val defaultProvider = ImportHistory.providerChessCom

  private def wantsJson(using req: RequestHeader): Boolean =
    req.path.startsWith("/api/") ||
      req.accepts("application/json") ||
      req.contentType.exists(_.contains("application/json"))

  private def internalWorkerAuthorized(using req: RequestHeader): Boolean =
    env.accountintel.internalAuthHeaderValue.exists: expected =>
      req.headers.get(env.accountintel.internalAuthHeaderName).contains(expected)

  private def parseSubmit(body: AnyContent): Either[String, SubmitPayload] =
    body.asJson
      .flatMap(_.validate[SubmitPayload].asOpt)
      .orElse:
        body.asFormUrlEncoded.map: form =>
          def first(name: String) = form.get(name).flatMap(_.headOption).getOrElse("")
          def bool(name: String) =
            form
              .get(name)
              .flatMap(_.headOption)
              .exists(v => Set("true", "1", "yes", "on").contains(v.trim.toLowerCase))
          SubmitPayload(
            provider = first("provider"),
            username = first("username"),
            kind = first("kind"),
            force = bool("force")
          )
      .toRight("Missing account notebook payload.")

  private def parseSurface(job: lila.accountintel.AccountIntel.AccountIntelJob): Option[JsObject] =
    job.surfaceJson.flatMap(raw => Try(Json.parse(raw).as[JsObject]).toOption)

  private def accountResultUrl(
      provider: String,
      username: String,
      kind: lila.accountintel.AccountIntel.ProductKind,
      side: String = "",
      jobId: Option[String] = None
  ): String =
    val base = routes.AccountIntel.product(provider, username, kind.key, side).url
    jobId.fold(base): id =>
      s"$base${if base.contains('?') then "&" else "?"}jobId=${URLEncoder.encode(id, StandardCharsets.UTF_8)}"

  private def historyEntryJson(job: lila.accountintel.AccountIntel.AccountIntelJob): JsObject =
    val surface = parseSurface(job)
    val surfacePreview = surface.map: s =>
      Json.obj(
        "headline" -> (s \ "headline").asOpt[String],
        "summary" -> (s \ "summary").asOpt[String],
        "generatedAt" -> (s \ "generatedAt").asOpt[String],
        "confidence" -> (s \ "confidence" \ "label").asOpt[String],
        "sampledGameCount" -> (s \ "source" \ "sampledGameCount").asOpt[Int],
        "warnings" -> (s \ "warnings").asOpt[List[String]].getOrElse(Nil),
        "patterns" -> JsArray(
          (s \ "patterns").asOpt[List[JsObject]].getOrElse(Nil).take(3).map: pattern =>
            Json.obj(
              "title" -> (pattern \ "title").asOpt[String],
              "side" -> (pattern \ "side").asOpt[String],
              "summary" -> (pattern \ "summary").asOpt[String]
            )
        )
      )
    Json.obj(
      "jobId" -> job.id,
      "status" -> job.status.key,
      "kind" -> job.kind.key,
      "requestedAt" -> job.requestedAt.toString,
      "finishedAt" -> job.finishedAt.map(_.toString),
      "progressStage" -> job.progressStage,
      "warnings" -> job.warnings,
      "url" -> accountResultUrl(job.provider, job.username, job.kind, jobId = Some(job.id)),
      "notebookUrl" -> job.notebookUrl,
      "sampledGameCount" -> surface.flatMap(s => (s \ "source" \ "sampledGameCount").asOpt[Int]),
      "confidence" -> surface.flatMap(s => (s \ "confidence" \ "label").asOpt[String]),
      "headline" -> surface.flatMap(s => (s \ "headline").asOpt[String]),
      "surfacePreview" -> surfacePreview
    )

  private def productStateJson(state: ProductState): JsObject =
    Json.obj(
      "provider" -> state.provider,
      "username" -> state.username,
      "kind" -> state.kind.key,
      "resultUrl" -> accountResultUrl(state.provider, state.username, state.kind, jobId = state.selectedJobId),
      "selectedJobId" -> state.selectedJobId,
      "surfaceJobId" -> state.displayedJob.map(_.id),
      "latestSuccessfulJob" -> state.latestSuccessful.map(_.toStatusResponse()),
      "activeJob" -> state.activeJob.map(_.toStatusResponse()),
      "surface" -> state.displayedJob.flatMap(parseSurface),
      "history" -> JsArray(state.history.map(historyEntryJson))
    )

  private def resolveKind(key: String): lila.accountintel.AccountIntel.ProductKind =
    lila.accountintel.AccountIntel.ProductKind.fromKey(key).getOrElse(defaultKind)

  private def safeLandingProvider(provider: String): String =
    lila.accountintel.AccountIntel.normalizeProvider(provider).getOrElse(defaultProvider)

  private def loadProductState(
      ownerId: UserId,
      provider: String,
      username: String,
      kind: lila.accountintel.AccountIntel.ProductKind,
      selectedJobId: Option[String]
  ): Fu[Either[String, ProductState]] =
    for
      latestSuccess <- env.accountintel.api.latestSuccessful(ownerId, provider, username, kind)
      active <- env.accountintel.api.latestActive(ownerId, provider, username, kind)
      history <- env.accountintel.api.history(ownerId, provider, username, kind)
      selected <- selectedJobId.fold(fuccess(Right(none[lila.accountintel.AccountIntel.AccountIntelJob]))):
        env.accountintel.api.selectedJob(ownerId, provider, username, kind, _)
    yield
      for
        success <- latestSuccess
        running <- active
        recent <- history
        selectedJob <- selected
        alignedSuccess = recent.find(job =>
          job.status == lila.accountintel.AccountIntel.JobStatus.Succeeded && parseSurface(job).isDefined
        ).orElse(success.filter(job => parseSurface(job).isDefined))
        alignedActive = recent.find(job =>
          job.status == lila.accountintel.AccountIntel.JobStatus.Queued ||
            job.status == lila.accountintel.AccountIntel.JobStatus.Running
        ).orElse(running)
        displayed = selectedJob.filter(job => parseSurface(job).isDefined).orElse(alignedSuccess)
      yield ProductState(
        provider = lila.accountintel.AccountIntel.normalizeProvider(provider).getOrElse(provider.trim.toLowerCase),
        username = lila.accountintel.AccountIntel.normalizeUsername(username).getOrElse(username.trim),
        kind = kind,
        latestSuccessful = alignedSuccess,
        activeJob = alignedActive,
        history = recent,
        displayedJob = displayed,
        selectedJobId = selectedJobId.filter(id => selectedJob.exists(_.id == id))
      )

  private def submitError(message: String, provider: String)(using
      req: RequestHeader
  ): Fu[Result] =
    if wantsJson then BadRequest(jsonError(message)).toFuccess
    else Redirect(routes.AccountIntel.landing(safeLandingProvider(provider), "")).flashing("error" -> message).toFuccess

  def landing(provider: String, username: String) = Auth { ctx ?=> me ?=>
    val safeProvider = safeLandingProvider(provider)
    val safeUsername = username.trim
    val requestedKindKey = get("kind").filter(_.nonEmpty).getOrElse(defaultKind.key)
    val requestedKind = resolveKind(requestedKindKey)
    if safeUsername.nonEmpty then
      (for
        validProvider <- lila.accountintel.AccountIntel.normalizeProvider(provider).toRight("Unsupported provider.")
        validUsername <- lila.accountintel.AccountIntel.normalizeUsername(safeUsername).toRight("Invalid username.")
      yield validProvider -> validUsername).fold(
        err => Redirect(routes.AccountIntel.landing(safeProvider, "")).flashing("error" -> err).toFuccess,
        { case (validProvider, validUsername) =>
          Redirect(accountResultUrl(validProvider, validUsername, requestedKind)).toFuccess
        }
      )
    else
      env.analyse.importHistory
        .recentSummary(me.userId)
        .zip(env.accountintel.api.recentSuccessful(me.userId))
        .flatMap { case (summary, recentRuns) =>
          Ok.page(
            views.accountIntel.landing(
              provider = safeProvider,
              username = "",
              selectedKindKey = requestedKind.key,
              recentAccounts = summary.accounts,
              recentRuns = recentRuns
            )
          )
        }
  }

  def submit = AuthBody(parse.anyContent) { ctx ?=> me ?=>
    parseSubmit(ctx.body.body).fold(
      msg => submitError(msg, ""),
      payload =>
        lila.accountintel.AccountIntel.ProductKind
          .fromKey(payload.kind)
          .fold(submitError("Unsupported notebook kind.", payload.provider)) { kind =>
            env.analyse.importHistory
              .recordAccountSearch(me.userId, payload.provider, payload.username)
              .flatMap: _ =>
                env.accountintel.api
                  .submit(me.userId, payload.provider, payload.username, kind, force = payload.force)
                  .flatMap:
                    case Left(err) =>
                      submitError(err, payload.provider)
                    case Right(job) =>
                      val statusUrl = routes.AccountIntel.jobStatusPage(job.id).url
                      if wantsJson then
                        Created(
                          Json.toJson(
                            lila.accountintel.AccountIntel.SubmitResponse(
                              jobId = job.id,
                              status = job.status.key,
                              url = statusUrl
                            )
                          )
                        ).toFuccess
                      else Redirect(statusUrl).toFuccess
          }
    )
  }

  def jobStatusApi(jobId: String) = Auth { _ ?=> me ?=>
    env.accountintel.api
      .status(me.userId, jobId)
      .map:
        _.fold[Result](NotFound(jsonError("Notebook build job not found."))): job =>
          Ok(Json.toJson(job.toStatusResponse(Some(accountResultUrl(job.provider, job.username, job.kind, jobId = Some(job.id))))))
  }

  def jobStatusPage(jobId: String) = Auth { ctx ?=> me ?=>
    env.accountintel.api
      .status(me.userId, jobId)
      .flatMap:
        _.fold(notFound): job =>
          Ok.page(views.accountIntel.status(job))
  }

  def product(provider: String, username: String, kind: String, side: String) = Auth { ctx ?=> me ?=>
    val resolvedKind = resolveKind(kind)
    val selectedJobId = get("jobId").filter(_.nonEmpty)
    loadProductState(me.userId, provider, username, resolvedKind, selectedJobId).flatMap:
      case Left(err) =>
        Redirect(routes.AccountIntel.landing(safeLandingProvider(provider), "")).flashing("error" -> err).toFuccess
      case Right(state) =>
        Ok.page(views.accountIntel.product(state, side, productStateJson(state)))
  }

  def surfaceApi(provider: String, username: String, kind: String) = Auth { _ ?=> me ?=>
    val selectedJobId = get("jobId").filter(_.nonEmpty)
    loadProductState(me.userId, provider, username, resolveKind(kind), selectedJobId).map:
      _.fold(
        _ => BadRequest(jsonError("Invalid account intel request.")),
        state => Ok(productStateJson(state))
      )
  }

  def historyApi(provider: String, username: String, kind: String) = Auth { _ ?=> me ?=>
    env.accountintel.api
      .history(me.userId, provider, username, resolveKind(kind))
      .map:
        _.fold(
          _ => BadRequest(jsonError("Invalid account intel request.")),
          history => Ok(Json.obj("history" -> JsArray(history.map(historyEntryJson))))
        )
  }

  def runJobInternal(jobId: String) = Anon:
    if !env.accountintel.workerEnabled || env.accountintel.internalAuthHeaderValue.isEmpty then
      NotFound(jsonError("Account intel worker endpoint is disabled.")).toFuccess
    else if !internalWorkerAuthorized then
      Unauthorized(jsonError("Invalid account intel worker token.")).toFuccess
    else
      env.accountintel.worker.runJob(jobId).flatMap:
        case lila.accountintel.AccountIntel.RunJobOutcome.Started =>
          Accepted(Json.obj("ok" -> true, "jobId" -> jobId)).toFuccess
        case lila.accountintel.AccountIntel.RunJobOutcome.Missing =>
          NotFound(jsonError("Account notebook build job not found.")).toFuccess
        case lila.accountintel.AccountIntel.RunJobOutcome.NotQueued =>
          Conflict(jsonError("Account notebook build job is not queued.")).toFuccess
