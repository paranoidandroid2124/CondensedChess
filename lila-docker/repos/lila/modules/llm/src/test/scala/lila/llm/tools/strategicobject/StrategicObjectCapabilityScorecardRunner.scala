package lila.llm.tools.strategicobject

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }
import play.api.libs.json.Json

object StrategicObjectCapabilityScorecardRunner:

  final case class Config(
      outputPath: Path,
      rowsOutputPath: Option[Path],
      families: Set[String]
  )

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[strategic-object-capability-scorecard] $err")
        sys.exit(2)
      case Right(config) =>
        run(config)

  private def run(config: Config): Unit =
    val allRows = StrategicObjectCapabilityScorecardSupport.evidenceRows
    val filteredRows =
      if config.families.isEmpty then allRows
      else allRows.filter(row => config.families.contains(row.family))
    val report = StrategicObjectCapabilityScorecardSupport.scorecard(filteredRows)

    writeText(config.outputPath, Json.prettyPrint(Json.toJson(report)))
    config.rowsOutputPath.foreach(path =>
      writeText(path, StrategicObjectCapabilityScorecardSupport.renderJsonl(filteredRows))
    )

    println(
      s"[strategic-object-capability-scorecard] wrote ${report.families.size} families / ${filteredRows.size} rows to ${config.outputPath}"
    )
    config.rowsOutputPath.foreach(path =>
      println(s"[strategic-object-capability-scorecard] wrote evidence rows to $path")
    )

  private def parseArgs(args: List[String]): Either[String, Config] =
    val workspaceRoot = detectWorkspaceRoot()
    val defaults =
      Config(
        outputPath =
          workspaceRoot.resolve(
            Path.of("tmp", "strategic_object", "reports", "StrategicObjectCapabilityScorecard.latest.json")
          ),
        rowsOutputPath = None,
        families = Set.empty
      )

    @annotation.tailrec
    def loop(rest: List[String], cfg: Config): Either[String, Config] =
      rest match
        case Nil => Right(cfg)
        case head :: tail if head.startsWith("--output=") =>
          loop(tail, cfg.copy(outputPath = Path.of(head.stripPrefix("--output=")).toAbsolutePath.normalize))
        case "--output" :: value :: tail =>
          loop(tail, cfg.copy(outputPath = Path.of(value).toAbsolutePath.normalize))
        case head :: tail if head.startsWith("--rows-output=") =>
          loop(tail, cfg.copy(rowsOutputPath = Some(Path.of(head.stripPrefix("--rows-output=")).toAbsolutePath.normalize)))
        case "--rows-output" :: value :: tail =>
          loop(tail, cfg.copy(rowsOutputPath = Some(Path.of(value).toAbsolutePath.normalize)))
        case head :: tail if head.startsWith("--families=") =>
          loop(tail, cfg.copy(families = parseFamilies(head.stripPrefix("--families="))))
        case "--families" :: value :: tail =>
          loop(tail, cfg.copy(families = parseFamilies(value)))
        case unknown :: _ =>
          Left(s"unknown argument: $unknown")

    loop(args, defaults)

  private def parseFamilies(
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
