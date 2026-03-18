package lila.accountintel.service

import chess.format.Fen
import play.api.Configuration
import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import scala.concurrent.duration.*
import scala.util.control.NonFatal

import lila.accountintel.*

private[accountintel] case class SelectiveEvalProbe(
    cp: Option[Int],
    mate: Option[Int],
    bestMove: Option[String],
    source: String
)

private[accountintel] trait SelectiveEvalLookup:
  def lookup(fen: String): Fu[Option[SelectiveEvalProbe]]

final class AccountIntelEvalRequester(
    appConfig: Configuration,
    ws: StandaloneWSClient
)(using Executor)
    extends SelectiveEvalLookup:

  private val endpoint =
    appConfig
      .getOptional[String]("accountIntel.selectiveEval.endpoint")
      .orElse(appConfig.getOptional[String]("externalEngine.endpoint"))
      .map(_.trim)
      .filter(_.nonEmpty)

  private val timeout =
    appConfig.getOptional[FiniteDuration]("accountIntel.selectiveEval.timeout").getOrElse(4.seconds)
  private val retryCount =
    appConfig.getOptional[Int]("accountIntel.selectiveEval.retryCount").getOrElse(2).max(0)
  private val bearerToken =
    appConfig.getOptional[String]("accountIntel.selectiveEval.bearerToken").map(_.trim).filter(_.nonEmpty)
  private val authHeaderName =
    appConfig.getOptional[String]("accountIntel.selectiveEval.authHeaderName").map(_.trim).filter(_.nonEmpty)
  private val authHeaderValue =
    appConfig.getOptional[String]("accountIntel.selectiveEval.authHeaderValue").map(_.trim).filter(_.nonEmpty)
  private val extraHeaders =
    bearerToken.map(token => List("Authorization" -> s"Bearer $token")).getOrElse(Nil) :::
      authHeaderName.zip(authHeaderValue).toList

  def lookup(fen: String): Fu[Option[SelectiveEvalProbe]] =
    endpoint.fold(fuccess(none[SelectiveEvalProbe])): base =>
      lookupWithRetry(base, fen, retryCount)

  private def lookupWithRetry(
      base: String,
      fen: String,
      retriesLeft: Int
  ): Fu[Option[SelectiveEvalProbe]] =
    ws.url(base)
      .addQueryStringParameters(
        "fen" -> fen,
        "multiPv" -> "1",
        "variant" -> "standard"
      )
      .withHttpHeaders(extraHeaders*)
      .withRequestTimeout(timeout)
      .get()
      .flatMap: res =>
        if res.status / 100 == 2 then
          fuccess(SelectiveEvalProbe.parseEvalJson(Json.parse(res.body[String]), source = "requester"))
        else if retryableStatus(res.status) && retriesLeft > 0 then
          lookupWithRetry(base, fen, retriesLeft - 1)
        else
          logger.warn(s"accountintel selective eval failed status=${res.status} fen=${fen.take(48)}")
          fuccess(none[SelectiveEvalProbe])
      .recoverWith { case NonFatal(err) =>
        if retriesLeft > 0 then lookupWithRetry(base, fen, retriesLeft - 1)
        else
          logger.warn(s"accountintel selective eval exception fen=${fen.take(48)} err=${err.getMessage}")
          fuccess(none[SelectiveEvalProbe])
      }

  private def retryableStatus(status: Int): Boolean =
    status == 408 || status == 425 || status == 429 || status >= 500

private[accountintel] object SelectiveEvalProbe:

  def parseEvalJson(js: JsValue, source: String): Option[SelectiveEvalProbe] =
    val maybePv =
      (js \ "pvs").asOpt[List[JsObject]].flatMap(_.headOption)
        .orElse(js.asOpt[JsObject].filter(o => (o \ "cp").asOpt[Int].isDefined || (o \ "mate").asOpt[Int].isDefined))

    maybePv.map: pv =>
      val bestMove =
        (pv \ "moves")
          .asOpt[String]
          .flatMap(_.trim.split("\\s+").toList.headOption)
          .filter(_.nonEmpty)
          .orElse((pv \ "best").asOpt[String].filter(_.nonEmpty))
      SelectiveEvalProbe(
        cp = (pv \ "cp").asOpt[Int],
        mate = (pv \ "mate").asOpt[Int],
        bestMove = bestMove,
        source = source
      )

final class EvalCacheLookup(
    evalCacheApi: lila.evalCache.EvalCacheApi
)(using Executor)
    extends SelectiveEvalLookup:

  import lila.core.chess.MultiPv

  def lookup(fen: String): Fu[Option[SelectiveEvalProbe]] =
    evalCacheApi
      .getEvalJson(chess.variant.Standard, Fen.Full(fen), MultiPv(1))
      .map(_.flatMap(SelectiveEvalProbe.parseEvalJson(_, source = "cache")))
