package lila.accountintel

import play.api.Configuration
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.StandaloneWSClient

import scala.concurrent.duration.*
import scala.util.control.NonFatal

import lila.accountintel.AccountIntel.{ AccountIntelJob, AccountIntelJobDispatcher, DispatchOutcome }

final class NoopAccountIntelJobDispatcher extends AccountIntelJobDispatcher:
  def dispatch(job: AccountIntelJob): Fu[DispatchOutcome] = fuccess(DispatchOutcome.Accepted)

final class HttpAccountIntelJobDispatcher(
    appConfig: Configuration,
    ws: StandaloneWSClient
)(using Executor)
    extends AccountIntelJobDispatcher:

  private val baseUrl =
    appConfig
      .getOptional[String]("accountIntel.dispatch.baseUrl")
      .map(_.trim.stripSuffix("/"))
      .filter(_.nonEmpty)

  private val timeout =
    appConfig.getOptional[FiniteDuration]("accountIntel.dispatch.timeout").getOrElse(4.seconds)
  private val bearerToken =
    appConfig.getOptional[String]("accountIntel.dispatch.bearerToken").map(_.trim).filter(_.nonEmpty)
  private val authHeaderName =
    appConfig.getOptional[String]("accountIntel.dispatch.authHeaderName").map(_.trim).filter(_.nonEmpty)
  private val authHeaderValue =
    appConfig.getOptional[String]("accountIntel.dispatch.authHeaderValue").map(_.trim).filter(_.nonEmpty)
  private val headers =
    bearerToken.map(token => List("Authorization" -> s"Bearer $token")).getOrElse(Nil) :::
      authHeaderName.zip(authHeaderValue).toList

  def dispatch(job: AccountIntelJob): Fu[DispatchOutcome] =
    baseUrl.fold(fuccess(DispatchOutcome.Accepted)): root =>
      val url = s"$root/internal/account-intel/jobs/${job.id}/run"
      ws.url(url)
        .withHttpHeaders(headers*)
        .withRequestTimeout(timeout)
        .post("")
        .map: res =>
          if res.status / 100 == 2 then DispatchOutcome.Accepted
          else
            val msg = s"Worker dispatch returned HTTP ${res.status}."
            logger.warn(s"accountintel dispatch failed status=${res.status} job=${job.id}")
            DispatchOutcome.Failed(msg)
        .recover:
          case NonFatal(err) =>
            logger.warn(s"accountintel dispatch exception job=${job.id} err=${err.getMessage}")
            DispatchOutcome.Failed(err.getMessage)
