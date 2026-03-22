package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.time.Instant

import scala.jdk.CollectionConverters.*

import play.api.libs.json.Json

object RealPgnNarrativeEvalReportMerge:

  import RealPgnNarrativeEvalRunner.RunReport

  final case class Config(
      inputDir: Path,
      markdownPath: Path,
      jsonPath: Path
  )

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[real-pgn-eval-merge] $err")
        sys.exit(2)
      case Right(config) =>
        run(config)

  private def run(config: Config): Unit =
    if !Files.isDirectory(config.inputDir) then
      throw new IllegalArgumentException(s"input directory does not exist: ${config.inputDir}")
    val reportFiles =
      Files
        .walk(config.inputDir)
        .iterator()
        .asScala
        .filter(path => Files.isRegularFile(path) && path.getFileName.toString == "report.json")
        .toList
        .sortBy(_.toAbsolutePath.normalize.toString)
    if reportFiles.isEmpty then
      throw new IllegalArgumentException(s"no shard report.json files found under ${config.inputDir}")
    val reports = reportFiles.map(readReport)
    val first = reports.head
    val mergedGames = reports.flatMap(_.games).distinctBy(_.id).sortBy(_.id)
    val negativeGuards = reports.flatMap(_.signoff.negativeGuards).distinctBy(_.id).sortBy(_.id)
    val merged =
      RealPgnNarrativeEvalRunner.RunReport(
        generatedAt = Instant.now().toString,
        corpusTitle = first.corpusTitle.replaceAll("""\s+\[[^\]]+\]$""", "") + " [merged shards]",
        corpusAsOfDate = first.corpusAsOfDate,
        depth = first.depth,
        multiPv = first.multiPv,
        enginePath = first.enginePath,
        summary = RealPgnNarrativeEvalRunner.buildSummary(mergedGames),
        signoff = RealPgnNarrativeEvalRunner.buildSignoff(mergedGames, negativeGuards),
        games = mergedGames
      )
    writeText(config.markdownPath, RealPgnNarrativeEvalRunner.renderMarkdown(merged))
    writeText(config.jsonPath, Json.prettyPrint(Json.toJson(merged)) + "\n")
    println(
      s"[real-pgn-eval-merge] merged ${reports.size} shard reports -> `${config.jsonPath}` (games=${merged.games.size})"
    )

  private def readReport(path: Path): RunReport =
    Json.parse(Files.readString(path, StandardCharsets.UTF_8)).as[RunReport]

  private def parseArgs(args: List[String]): Either[String, Config] =
    val positional = args.filterNot(_.startsWith("--"))
    positional match
      case inputDir :: markdownPath :: jsonPath :: Nil =>
        Right(
          Config(
            inputDir = Paths.get(inputDir).toAbsolutePath.normalize,
            markdownPath = Paths.get(markdownPath).toAbsolutePath.normalize,
            jsonPath = Paths.get(jsonPath).toAbsolutePath.normalize
          )
        )
      case _ =>
        Left("usage: <input-dir> <markdown-path> <json-path>")

  private def writeText(path: Path, value: String): Unit =
    Option(path.getParent).foreach(parent => Files.createDirectories(parent))
    Files.writeString(path, value, StandardCharsets.UTF_8)
