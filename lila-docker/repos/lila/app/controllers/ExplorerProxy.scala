package controllers

import play.api.libs.json.Json
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.DefaultBodyReadables.*

import scala.util.control.NonFatal

import lila.app.*

final class ExplorerProxy(
    env: Env,
    ws: StandaloneWSClient
) extends LilaController(env):

  private val logger = lila.log("explorer.proxy")
  private val allowedDbs = Set("masters", "lichess", "player")
  private def configured(path: String): Option[String] =
    env.config.getOptional[String](path).map(_.trim).filter(_.nonEmpty)
  private val explorerBase =
    configured("explorer.internal_endpoint")
      .orElse(configured("explorer.endpoint"))
      .orElse(sys.env.get("EXPLORER_API_BASE").map(_.trim).filter(_.nonEmpty))
      .getOrElse("https://explorer.lichess.org")
      .stripSuffix("/")
  private val explorerToken =
    configured("explorer.api_token")
      .orElse(sys.env.get("LICHESS_EXPLORER_TOKEN"))
      .orElse(sys.env.get("LICHESS_API_TOKEN"))
      .map(_.trim)
      .filter(_.nonEmpty)
  private val requestTimeout = 15.seconds

  def opening(db: String) = Open:
    val normalizedDb = Option(db).map(_.trim.toLowerCase).getOrElse("")
    if !allowedDbs(normalizedDb) then NotFound(Json.obj("error" -> "invalid_db")).toFuccess
    else
      val acceptHeader =
        req.headers.get("Accept").getOrElse(if normalizedDb == "player" then "application/x-ndjson" else "application/json")

      val withQueryParams = req.queryString.foldLeft(ws.url(s"$explorerBase/$normalizedDb").withRequestTimeout(requestTimeout)) {
        case (request, (_, Nil)) => request
        case (request, (key, values)) =>
          request.addQueryStringParameters(values.map(v => key -> v)*)
      }

      val authHeaders = explorerToken.fold(List.empty[(String, String)])(token =>
        List("Authorization" -> s"Bearer $token")
      )

      withQueryParams
        .withHttpHeaders((("Accept" -> acceptHeader) :: authHeaders)*)
        .get()
        .map: upstream =>
          if upstream.status == 401 && explorerToken.isEmpty then
            Unauthorized(
              Json.obj(
                "error" -> "explorer_token_missing",
                "message" -> "Set LICHESS_EXPLORER_TOKEN to use opening explorer."
              )
            ).as(JSON)
          else
            val contentType = Option(upstream.contentType).filter(_.nonEmpty).getOrElse:
              if normalizedDb == "player" then "application/x-ndjson; charset=utf-8"
              else "application/json; charset=utf-8"
            Status(upstream.status)(upstream.body[Array[Byte]]).as(contentType)
        .recover { case NonFatal(e) =>
          logger.warn(s"Explorer proxy failed db=$normalizedDb err=${e.getMessage}")
          BadGateway(Json.obj("error" -> "explorer_upstream_error", "message" -> e.getMessage)).as(JSON)
        }
