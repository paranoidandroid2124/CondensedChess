package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.time.Instant
import scala.jdk.CollectionConverters.*

import play.api.libs.json.Json

/**
 * Gate runner for shadow latency logs.
 *
 * Expected line formats:
 * 1) key=value logs (from llm.api bookmaker.metrics), e.g.
 *    bookmaker.metrics struct_mode=off shadow_window=1200 total_latency_p95_ms=45.3 epoch=1730000000
 * 2) JSON lines, e.g.
 *    {"struct_mode":"shadow","shadow_window":1200,"total_latency_p95_ms":57.8,"epoch":1730000000}
 */
object ShadowMetricsGateRunner:

  private final case class Config(
      logPaths: List[Path],
      strict: Boolean,
      days: Int,
      minSamples: Int,
      maxP95DeltaMs: Double
  )

  private final case class MetricPoint(
      epochSec: Long,
      mode: String,
      samples: Long,
      totalP95Ms: Double
  )

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[shadow-gate] $err")
        sys.exit(2)
      case Right(cfg) =>
        val points = cfg.logPaths.flatMap(readPoints)
        val cutoff = Instant.now().getEpochSecond - cfg.days.toLong * 24L * 3600L
        val recent = points.filter(_.epochSec >= cutoff)

        val normalized = recent.map(p => p.copy(mode = normalizeMode(p.mode)))
        val off = normalized.filter(_.mode == "off")
        val on = normalized.filter(_.mode == "on")

        val offSamples = off.map(_.samples).sum
        val onSamples = on.map(_.samples).sum
        val totalSamples = offSamples + onSamples
        val offP95 = weightedAverage(off)
        val onP95 = weightedAverage(on)
        val delta = onP95 - offP95

        val enoughSamples = totalSamples >= cfg.minSamples
        val hasBothSides = off.nonEmpty && on.nonEmpty
        val deltaPass = hasBothSides && delta <= cfg.maxP95DeltaMs
        val pass = enoughSamples && hasBothSides && deltaPass

        val summary =
          f"samples=$totalSamples offSamples=$offSamples onSamples=$onSamples offP95=$offP95%.3f onP95=$onP95%.3f delta=$delta%.3f " +
            s"windowDays=${cfg.days} minSamples=${cfg.minSamples} maxDelta=${cfg.maxP95DeltaMs}"

        if pass then
          println(s"[shadow-gate] ✅ $summary")
        else
          val missingSideMsg =
            if hasBothSides then ""
            else " missing on/off modes in recent window."
          val sampleMsg =
            if enoughSamples then ""
            else s" insufficient samples: $totalSamples < ${cfg.minSamples}."
          val deltaMsg =
            if !hasBothSides || deltaPass then ""
            else f" latency delta too high: $delta%.3f > ${cfg.maxP95DeltaMs}%.3f."
          System.err.println(s"[shadow-gate] ❌ $summary.$sampleMsg$missingSideMsg$deltaMsg")
          if cfg.strict then sys.exit(1)

  private def parseArgs(args: List[String]): Either[String, Config] =
    val defaults = Config(
      logPaths = List(
        Paths.get("modules/llm/docs/shadow/ShadowMetricsOps.txt"),
        Paths.get("modules/llm/docs/shadow/ShadowMetricsStaging.txt")
      ),
      strict = false,
      days = 3,
      minSamples = 2000,
      maxP95DeltaMs = 15.0
    )

    args.foldLeft[Either[String, Config]](Right(defaults)) {
      case (Left(err), _) => Left(err)
      case (Right(cfg), "--strict") =>
        Right(cfg.copy(strict = true))
      case (Right(cfg), arg) if arg.startsWith("--days=") =>
        arg.stripPrefix("--days=").toIntOption.filter(_ > 0).toRight(s"invalid --days: $arg").map(v => cfg.copy(days = v))
      case (Right(cfg), arg) if arg.startsWith("--min-samples=") =>
        arg.stripPrefix("--min-samples=").toIntOption.filter(_ > 0).toRight(s"invalid --min-samples: $arg").map(v => cfg.copy(minSamples = v))
      case (Right(cfg), arg) if arg.startsWith("--max-p95-delta-ms=") =>
        arg.stripPrefix("--max-p95-delta-ms=").toDoubleOption.filter(_ >= 0.0).toRight(s"invalid --max-p95-delta-ms: $arg").map(v =>
          cfg.copy(maxP95DeltaMs = v)
        )
      case (Right(cfg), arg) if !arg.startsWith("--") =>
        Right(cfg.copy(logPaths = cfg.logPaths :+ Paths.get(arg)))
      case (Right(_), arg) =>
        Left(s"unknown argument: $arg")
    }

  private def readPoints(path: Path): List[MetricPoint] =
    if !Files.exists(path) then Nil
    else
      Files
        .readAllLines(path, StandardCharsets.UTF_8)
        .asScala
        .toList
        .flatMap(parseLine)

  private def parseLine(raw: String): Option[MetricPoint] =
    val line = raw.trim
    if line.isEmpty then None
    else if line.startsWith("{") then parseJsonLine(line)
    else parseKeyValueLine(line)

  private def parseJsonLine(line: String): Option[MetricPoint] =
    val js = Json.parse(line)
    val mode = (js \ "struct_mode").asOpt[String].orElse((js \ "mode").asOpt[String]).getOrElse("unknown")
    val samples = (js \ "shadow_window").asOpt[Long].orElse((js \ "samples").asOpt[Long]).getOrElse(0L)
    val p95 = (js \ "total_latency_p95_ms").asOpt[Double]
      .orElse((js \ "latency_total_p95_ms").asOpt[Double])
    val epoch = (js \ "epoch").asOpt[Long].getOrElse(Instant.now().getEpochSecond)
    p95.map(v => MetricPoint(epochSec = epoch, mode = mode, samples = samples, totalP95Ms = v))

  private def parseKeyValueLine(line: String): Option[MetricPoint] =
    val parts = line.split("\\s+").toList
    val kv = parts.flatMap { token =>
      token.split("=", 2).toList match
        case k :: v :: Nil => Some(k -> v)
        case _ => None
    }.toMap
    val p95 = kv.get("total_latency_p95_ms").flatMap(_.toDoubleOption).orElse(kv.get("latency_total_p95_ms").flatMap(_.toDoubleOption))
    val mode = kv.getOrElse("struct_mode", kv.getOrElse("mode", "unknown"))
    val samples = kv.get("shadow_window").flatMap(_.toLongOption).orElse(kv.get("samples").flatMap(_.toLongOption)).getOrElse(0L)
    val epoch = kv.get("epoch").flatMap(_.toLongOption).getOrElse(Instant.now().getEpochSecond)
    p95.map(v => MetricPoint(epochSec = epoch, mode = mode, samples = samples, totalP95Ms = v))

  private def normalizeMode(raw: String): String =
    raw.trim.toLowerCase match
      case "off" | "disabled" => "off"
      case "on" | "enabled" | "shadow" => "on"
      case _ => "unknown"

  private def weightedAverage(points: List[MetricPoint]): Double =
    val weighted = points.map(p => p.totalP95Ms * p.samples.toDouble).sum
    val weights = points.map(_.samples).sum.toDouble
    if weights <= 0.0 then 0.0 else weighted / weights
