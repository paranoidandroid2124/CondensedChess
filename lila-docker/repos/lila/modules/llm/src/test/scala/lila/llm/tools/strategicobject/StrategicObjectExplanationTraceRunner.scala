package lila.llm.tools.strategicobject

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

object StrategicObjectExplanationTraceRunner:

  final case class Config(
      outputPath: Path,
      limit: Option[Int],
      caseTypes: Set[String]
  )

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[strategic-object-explanation-trace] $err")
        sys.exit(2)
      case Right(config) =>
        run(config)

  private def run(config: Config): Unit =
    val rows = StrategicObjectExplanationTraceSupport.traceRows
    val filteredByCaseType =
      if config.caseTypes.isEmpty then rows
      else rows.filter(row => config.caseTypes.contains(row.caseType))
    val selected = config.limit.fold(filteredByCaseType)(filteredByCaseType.take)
    writeText(config.outputPath, StrategicObjectExplanationTraceSupport.renderJsonl(selected))
    println(s"[strategic-object-explanation-trace] wrote ${selected.size} rows to ${config.outputPath}")

  private def parseArgs(args: List[String]): Either[String, Config] =
    val workspaceRoot = detectWorkspaceRoot()
    val defaults =
      Config(
        outputPath =
          workspaceRoot.resolve(
            Path.of("tools", "strategic_object", "reports", "StrategicObjectExplanationTrace.latest.jsonl")
          ),
        limit = None,
        caseTypes = Set.empty
      )

    @annotation.tailrec
    def loop(rest: List[String], cfg: Config): Either[String, Config] =
      rest match
        case Nil => Right(cfg)
        case head :: tail if head.startsWith("--output=") =>
          loop(tail, cfg.copy(outputPath = Path.of(head.stripPrefix("--output=")).toAbsolutePath.normalize))
        case "--output" :: value :: tail =>
          loop(tail, cfg.copy(outputPath = Path.of(value).toAbsolutePath.normalize))
        case head :: tail if head.startsWith("--limit=") =>
          head.stripPrefix("--limit=").toIntOption match
            case Some(limit) if limit > 0 => loop(tail, cfg.copy(limit = Some(limit)))
            case _                        => Left(s"invalid --limit: $head")
        case "--limit" :: value :: tail =>
          value.toIntOption match
            case Some(limit) if limit > 0 => loop(tail, cfg.copy(limit = Some(limit)))
            case _                        => Left(s"invalid --limit: $value")
        case head :: tail if head.startsWith("--case-types=") =>
          loop(tail, cfg.copy(caseTypes = parseCaseTypes(head.stripPrefix("--case-types="))))
        case "--case-types" :: value :: tail =>
          loop(tail, cfg.copy(caseTypes = parseCaseTypes(value)))
        case unknown :: _ =>
          Left(s"unknown argument: $unknown")

    loop(args, defaults)

  private def parseCaseTypes(
      raw: String
  ): Set[String] =
    raw
      .split(",")
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .toSet

  private def detectWorkspaceRoot(): Path =
    val cwd = Path.of(".").toAbsolutePath.normalize
    if Files.isDirectory(cwd.resolve("tools")) && Files.isDirectory(cwd.resolve("lila-docker")) then cwd
    else
      Option(cwd.getParent)
        .flatMap(parent => Option(parent.getParent))
        .flatMap(grandParent => Option(grandParent.getParent))
        .filter(root => Files.isDirectory(root.resolve("tools")) && Files.isDirectory(root.resolve("lila-docker")))
        .getOrElse(cwd)

  private def writeText(
      path: Path,
      value: String
  ): Unit =
    Option(path.getParent).foreach(parent => Files.createDirectories(parent))
    Files.writeString(path, value, StandardCharsets.UTF_8)
