package lila.llm.tools.strategicobject

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }
import play.api.libs.json.Json

object StrategicObjectBatchCoverageRunner:

  final case class Config(
      outputPath: Path,
      rowsOutputPath: Option[Path],
      auditOutputPath: Option[Path],
      manualAuditOutputPath: Option[Path],
      fenJsonl: Option[Path],
      gamesJson: Option[Path],
      catalogJsonl: Option[Path],
      maxGames: Option[Int],
      plyStep: Int,
      maxPliesPerGame: Option[Int],
      families: Set[String]
  )

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[strategic-object-batch-coverage] $err")
        sys.exit(2)
      case Right(config) =>
        run(config)

  private def run(config: Config): Unit =
    val loadConfig =
      StrategicObjectBatchCoverageSupport.LoadConfig(
        maxGames = config.maxGames,
        plyStep = config.plyStep,
        maxPliesPerGame = config.maxPliesPerGame
      )

    val loaded =
      List(
        config.fenJsonl.map(path => StrategicObjectBatchCoverageSupport.loadFenJsonl(path)),
        config.gamesJson.map(path => StrategicObjectBatchCoverageSupport.loadEmbeddedGamesJson(path, loadConfig)),
        config.catalogJsonl.map(path => StrategicObjectBatchCoverageSupport.loadCatalogJsonl(path, loadConfig))
      ).flatten

    if loaded.isEmpty then
      System.err.println("[strategic-object-batch-coverage] no input provided; use --fen-jsonl, --games-json, or --catalog-jsonl")
      sys.exit(2)

    val mergedSummary =
      StrategicObjectBatchCoverageSupport.InputLoadSummary(
        sourceKinds = loaded.flatMap(_._1.sourceKinds).distinct.sorted,
        sourcePaths = loaded.flatMap(_._1.sourcePaths).distinct.sorted,
        rawGameCount = loaded.map(_._1.rawGameCount).sum,
        evaluatedSampleCount = loaded.map(_._1.evaluatedSampleCount).sum,
        samplesWithPlayedMove = loaded.map(_._1.samplesWithPlayedMove).sum,
        skippedGameCount = loaded.map(_._1.skippedGameCount).sum,
        skippedGames = loaded.flatMap(_._1.skippedGames).take(50)
      )
    val rows = loaded.flatMap(_._2)
    val (report, evidenceRows, auditRows) =
      StrategicObjectBatchCoverageSupport.report(mergedSummary, rows, config.families)

    writeText(config.outputPath, Json.prettyPrint(Json.toJson(report)))
    config.rowsOutputPath.foreach(path =>
      writeText(path, StrategicObjectBatchCoverageSupport.renderJsonl(evidenceRows))
    )
    config.auditOutputPath.foreach(path =>
      writeText(path, StrategicObjectBatchCoverageSupport.renderAuditJsonl(auditRows))
    )
    config.manualAuditOutputPath.foreach(path =>
      writeText(path, StrategicObjectBatchCoverageSupport.renderAuditJsonl(StrategicObjectBatchCoverageSupport.topManualAuditRows(auditRows)))
    )

    println(
      s"[strategic-object-batch-coverage] wrote ${report.families.size} families / ${rows.size} samples to ${config.outputPath}"
    )
    config.rowsOutputPath.foreach(path =>
      println(s"[strategic-object-batch-coverage] wrote activation rows to $path")
    )
    config.auditOutputPath.foreach(path =>
      println(s"[strategic-object-batch-coverage] wrote audit rows to $path")
    )
    config.manualAuditOutputPath.foreach(path =>
      println(s"[strategic-object-batch-coverage] wrote top manual audit rows to $path")
    )

  private def parseArgs(args: List[String]): Either[String, Config] =
    val workspaceRoot = detectWorkspaceRoot()
    val defaults =
      Config(
        outputPath =
          workspaceRoot.resolve(
            Path.of("tmp", "strategic_object", "reports", "StrategicObjectBatchCoverage.latest.json")
          ),
        rowsOutputPath = None,
        auditOutputPath =
          Some(
            workspaceRoot.resolve(
              Path.of("tmp", "strategic_object", "reports", "StrategicObjectBatchCoverage.latest.audit.jsonl")
            )
          ),
        manualAuditOutputPath = None,
        fenJsonl = None,
        gamesJson = None,
        catalogJsonl = None,
        maxGames = Some(50),
        plyStep = 8,
        maxPliesPerGame = Some(8),
        families = Set.empty
      )

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
        case head :: tail if head.startsWith("--audit-output=") =>
          loop(tail, cfg.copy(auditOutputPath = Some(Path.of(head.stripPrefix("--audit-output=")).toAbsolutePath.normalize)))
        case "--audit-output" :: value :: tail =>
          loop(tail, cfg.copy(auditOutputPath = Some(Path.of(value).toAbsolutePath.normalize)))
        case head :: tail if head.startsWith("--manual-audit-output=") =>
          loop(tail, cfg.copy(manualAuditOutputPath = Some(Path.of(head.stripPrefix("--manual-audit-output=")).toAbsolutePath.normalize)))
        case "--manual-audit-output" :: value :: tail =>
          loop(tail, cfg.copy(manualAuditOutputPath = Some(Path.of(value).toAbsolutePath.normalize)))
        case head :: tail if head.startsWith("--fen-jsonl=") =>
          loop(tail, cfg.copy(fenJsonl = Some(Path.of(head.stripPrefix("--fen-jsonl=")).toAbsolutePath.normalize)))
        case "--fen-jsonl" :: value :: tail =>
          loop(tail, cfg.copy(fenJsonl = Some(Path.of(value).toAbsolutePath.normalize)))
        case head :: tail if head.startsWith("--games-json=") =>
          loop(tail, cfg.copy(gamesJson = Some(Path.of(head.stripPrefix("--games-json=")).toAbsolutePath.normalize)))
        case "--games-json" :: value :: tail =>
          loop(tail, cfg.copy(gamesJson = Some(Path.of(value).toAbsolutePath.normalize)))
        case head :: tail if head.startsWith("--catalog-jsonl=") =>
          loop(tail, cfg.copy(catalogJsonl = Some(Path.of(head.stripPrefix("--catalog-jsonl=")).toAbsolutePath.normalize)))
        case "--catalog-jsonl" :: value :: tail =>
          loop(tail, cfg.copy(catalogJsonl = Some(Path.of(value).toAbsolutePath.normalize)))
        case head :: tail if head.startsWith("--max-games=") =>
          parseOptionalPositiveInt(head.stripPrefix("--max-games=")).fold[Either[String, Config]](Left(s"invalid --max-games: $head"))(value =>
            loop(tail, cfg.copy(maxGames = value))
          )
        case "--max-games" :: value :: tail =>
          parseOptionalPositiveInt(value).fold[Either[String, Config]](Left(s"invalid --max-games: $value"))(parsed =>
            loop(tail, cfg.copy(maxGames = parsed))
          )
        case head :: tail if head.startsWith("--ply-step=") =>
          parsePositiveInt(head.stripPrefix("--ply-step=")).fold[Either[String, Config]](Left(s"invalid --ply-step: $head"))(value =>
            loop(tail, cfg.copy(plyStep = value))
          )
        case "--ply-step" :: value :: tail =>
          parsePositiveInt(value).fold[Either[String, Config]](Left(s"invalid --ply-step: $value"))(parsed =>
            loop(tail, cfg.copy(plyStep = parsed))
          )
        case head :: tail if head.startsWith("--max-plies-per-game=") =>
          parseOptionalPositiveInt(head.stripPrefix("--max-plies-per-game=")).fold[Either[String, Config]](Left(s"invalid --max-plies-per-game: $head"))(value =>
            loop(tail, cfg.copy(maxPliesPerGame = value))
          )
        case "--max-plies-per-game" :: value :: tail =>
          parseOptionalPositiveInt(value).fold[Either[String, Config]](Left(s"invalid --max-plies-per-game: $value"))(parsed =>
            loop(tail, cfg.copy(maxPliesPerGame = parsed))
          )
        case head :: tail if head.startsWith("--families=") =>
          loop(tail, cfg.copy(families = parseFamilies(head.stripPrefix("--families="))))
        case "--families" :: value :: tail =>
          loop(tail, cfg.copy(families = parseFamilies(value)))
        case unknown :: _ =>
          Left(s"unknown argument: $unknown")

    loop(args, defaults)

  private def parsePositiveInt(
      raw: String
  ): Option[Int] =
    raw.toIntOption.filter(_ > 0)

  private def parseOptionalPositiveInt(
      raw: String
  ): Option[Option[Int]] =
    raw.trim.toLowerCase match
      case "all" | "none" => Some(None)
      case other           => parsePositiveInt(other).map(Some(_))

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
