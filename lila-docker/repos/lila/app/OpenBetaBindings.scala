package lila.app

import java.net.{ InetSocketAddress, Socket, URI }
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import scala.concurrent.Future
import scala.concurrent.blocking
import scala.concurrent.duration.*
import scala.util.control.NonFatal

import com.typesafe.config.ConfigException
import play.api.Configuration
import play.api.libs.json.*
import play.api.libs.functional.syntax.*
import play.api.libs.ws.StandaloneWSClient

import lila.common.config.GetRelativeFile

final case class OpenBetaBindingSpec(
    env: String,
    configPath: String,
    kind: String,
    requiredMode: String,
    readinessClass: String,
    probe: String,
    notes: String
):
  def displayConfigPath = if configPath.nonEmpty then configPath else "env-only"

final case class OpenBetaBindingsManifest(
    bindings: List[OpenBetaBindingSpec],
    removedBindings: List[String]
)

object OpenBetaBindingsManifest:
  given Reads[OpenBetaBindingSpec] = Json.reads[OpenBetaBindingSpec]
  given Reads[OpenBetaBindingsManifest] =
    (
      (__ \ "bindings").read[List[OpenBetaBindingSpec]] and
        (__ \ "removedBindings").readWithDefault[List[String]](Nil)
    )(OpenBetaBindingsManifest.apply)

final case class OpenBetaBindingStatus(
    spec: OpenBetaBindingSpec,
    required: Boolean,
    configured: Boolean,
    reachable: Option[Boolean],
    detail: String
):
  def readinessRequired = required && spec.readinessClass == "core"
  def readyOk =
    if !readinessRequired then true
    else
      spec.probe match
        case "config_only" => configured
        case _             => configured && reachable.contains(true)

final class OpenBetaBindings(
    config: Configuration,
    getFile: GetRelativeFile,
    ws: StandaloneWSClient
)(using Executor):

  private val requestTimeout = 2.seconds

  lazy val manifest: OpenBetaBindingsManifest = loadManifest()

  def snapshot: Fu[List[OpenBetaBindingStatus]] =
    Future.traverse(manifest.bindings)(statusOf)

  private def loadManifest(): OpenBetaBindingsManifest =
    val path = getFile.exec("conf/openbeta-bindings.json").toPath
    val text = Files.readString(path, StandardCharsets.UTF_8)
    Json.parse(text).as[OpenBetaBindingsManifest]

  private def dispatchEnabled =
    configuredString("accountIntel.dispatch.baseUrl").isDefined

  private def workerEnabled =
    config.getOptional[Boolean]("accountIntel.worker.enabled").getOrElse(true)

  private def selectiveEvalEnabled =
    configuredString("accountIntel.selectiveEval.endpoint").isDefined

  private def statusOf(spec: OpenBetaBindingSpec): Fu[OpenBetaBindingStatus] =
    val required = isRequired(spec)
    bindingValue(spec) match
      case None =>
        fuccess(
          OpenBetaBindingStatus(
            spec = spec,
            required = required,
            configured = false,
            reachable = None,
            detail = missingDetail(spec)
          )
        )
      case Some(value) =>
        spec.probe match
          case "http" =>
            probeHttpReachable(value).map: reachable =>
              OpenBetaBindingStatus(
                spec = spec,
                required = required,
                configured = true,
                reachable = Some(reachable),
                detail = if reachable then "reachable" else s"unreachable:$value"
              )
          case "redis" =>
            probeRedisReachable(value).map: reachable =>
              OpenBetaBindingStatus(
                spec = spec,
                required = required,
                configured = true,
                reachable = Some(reachable),
                detail =
                  if reachable then "reachable"
                  else redisDetail(value).fold("unreachable")(addr => s"unreachable:$addr")
              )
          case _ =>
            fuccess(
              OpenBetaBindingStatus(
                spec = spec,
                required = required,
                configured = true,
                reachable = None,
                detail = "configured"
              )
            )

  private def isRequired(spec: OpenBetaBindingSpec) =
    spec.requiredMode match
      case "always"              => true
      case "dispatch_only"       => dispatchEnabled
      case "selective_eval_only" => selectiveEvalEnabled
      case _                     => false

  private def missingDetail(spec: OpenBetaBindingSpec) =
    spec.requiredMode match
      case "dispatch_only" if !dispatchEnabled       => if workerEnabled then "local_worker" else "dispatch_disabled"
      case "selective_eval_only" if !selectiveEvalEnabled => "selective_eval_disabled"
      case "soft_optional"                           => "optional_missing"
      case _                                         => "missing"

  private def bindingValue(spec: OpenBetaBindingSpec): Option[String] =
    configuredString(spec.configPath)
      .orElse(sys.env.get(spec.env).map(_.trim).filter(_.nonEmpty))

  private def configuredString(path: String): Option[String] =
    Option(path).map(_.trim).filter(_.nonEmpty).flatMap: key =>
      try
        Option(config.underlying.getAnyRef(key)).flatMap:
          case s: String => Option(s.trim).filter(_.nonEmpty)
          case v: java.lang.Boolean => Some(v.toString)
          case v: java.lang.Number  => Some(v.toString)
          case xs: java.util.Collection[?] if !xs.isEmpty => Some(xs.toString)
          case other => Option(other.toString).filter(_.nonEmpty)
      catch
        case _: ConfigException.Missing => None

  private def probeHttpReachable(url: String): Fu[Boolean] =
    ws.url(url)
      .withRequestTimeout(requestTimeout)
      .get()
      .map(res => res.status < 500)
      .recover { case NonFatal(_) => false }

  private def probeRedisReachable(uriValue: String): Fu[Boolean] =
    Future:
      blocking:
        redisAddress(uriValue).exists: (host, port) =>
          val socket = new Socket()
          try
            socket.connect(new InetSocketAddress(host, port), requestTimeout.toMillis.toInt)
            true
          catch
            case NonFatal(_) => false
          finally socket.close()

  private def redisAddress(uriValue: String): Option[(String, Int)] =
    try
      val uri = URI(uriValue)
      val scheme = Option(uri.getScheme).map(_.toLowerCase)
      if !scheme.exists(s => s == "redis" || s == "rediss") then None
      else
        Option(uri.getHost).map(_.trim).filter(_.nonEmpty).map: host =>
          val port = if uri.getPort > 0 then uri.getPort else 6379
          host -> port
    catch
      case NonFatal(_) => None

  private def redisDetail(uriValue: String): Option[String] =
    redisAddress(uriValue).map: (host, port) =>
      s"$host:$port"
