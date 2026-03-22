package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import play.api.libs.json.Json

object StrategicPuzzleRevealAuditRunner:

  final case class Config(
      inputPath: Path,
      markdownPath: Path,
      jsonPath: Path,
      limit: Option[Int]
  )

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[strategic-puzzle-reveal-audit] $err")
        sys.exit(2)
      case Right(config) =>
        run(config)

  private def run(config: Config): Unit =
    if !Files.exists(config.inputPath) then
      throw new IllegalArgumentException(s"input file does not exist: ${config.inputPath}")
    val docs = StrategicPuzzleRevealAuditSupport.readDocs(config.inputPath)
    val selected = config.limit.fold(docs)(docs.take)
    val report = StrategicPuzzleRevealAuditSupport.auditDocs(selected, config.inputPath)
    writeText(config.markdownPath, StrategicPuzzleRevealAuditSupport.renderMarkdown(report))
    writeText(config.jsonPath, Json.prettyPrint(Json.toJson(report)) + "\n")
    println(s"[strategic-puzzle-reveal-audit] wrote ${config.markdownPath} and ${config.jsonPath}")

  private def parseArgs(args: List[String]): Either[String, Config] =
    val workspaceRoot = detectWorkspaceRoot()
    val defaults =
      Config(
        inputPath = workspaceRoot.resolve(Path.of("tools", "strategic_puzzles", "runtime_prompt_sample10_20260319.jsonl")),
        markdownPath = workspaceRoot.resolve(Path.of("tools", "strategic_puzzles", "reports", "StrategicPuzzleRevealAudit.latest.md")),
        jsonPath = workspaceRoot.resolve(Path.of("tools", "strategic_puzzles", "reports", "StrategicPuzzleRevealAudit.latest.json")),
        limit = None
      )

    @annotation.tailrec
    def loop(rest: List[String], cfg: Config): Either[String, Config] =
      rest match
        case Nil => Right(cfg)
        case head :: tail if head.startsWith("--input=") =>
          loop(tail, cfg.copy(inputPath = Path.of(head.stripPrefix("--input=")).toAbsolutePath.normalize))
        case "--input" :: value :: tail =>
          loop(tail, cfg.copy(inputPath = Path.of(value).toAbsolutePath.normalize))
        case head :: tail if head.startsWith("--md=") =>
          loop(tail, cfg.copy(markdownPath = Path.of(head.stripPrefix("--md=")).toAbsolutePath.normalize))
        case "--md" :: value :: tail =>
          loop(tail, cfg.copy(markdownPath = Path.of(value).toAbsolutePath.normalize))
        case head :: tail if head.startsWith("--json=") =>
          loop(tail, cfg.copy(jsonPath = Path.of(head.stripPrefix("--json=")).toAbsolutePath.normalize))
        case "--json" :: value :: tail =>
          loop(tail, cfg.copy(jsonPath = Path.of(value).toAbsolutePath.normalize))
        case head :: tail if head.startsWith("--limit=") =>
          head.stripPrefix("--limit=").toIntOption match
            case Some(limit) if limit > 0 => loop(tail, cfg.copy(limit = Some(limit)))
            case _                        => Left(s"invalid --limit: $head")
        case "--limit" :: value :: tail =>
          value.toIntOption match
            case Some(limit) if limit > 0 => loop(tail, cfg.copy(limit = Some(limit)))
            case _                        => Left(s"invalid --limit: $value")
        case unknown :: _ => Left(s"unknown argument: $unknown")

    loop(args, defaults)

  private def detectWorkspaceRoot(): Path =
    val cwd = Path.of(".").toAbsolutePath.normalize
    if Files.isDirectory(cwd.resolve("tools")) && Files.isDirectory(cwd.resolve("lila-docker")) then cwd
    else
      Option(cwd.getParent)
        .flatMap(parent => Option(parent.getParent))
        .flatMap(grandParent => Option(grandParent.getParent))
        .filter(root => Files.isDirectory(root.resolve("tools")) && Files.isDirectory(root.resolve("lila-docker")))
        .getOrElse(cwd)

  private def writeText(path: Path, value: String): Unit =
    Option(path.getParent).foreach(parent => Files.createDirectories(parent))
    Files.writeString(path, value, StandardCharsets.UTF_8)
