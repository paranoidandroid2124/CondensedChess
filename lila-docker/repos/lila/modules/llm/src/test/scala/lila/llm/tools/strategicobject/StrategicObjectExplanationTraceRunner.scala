package lila.llm.tools.strategicobject

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }
import play.api.libs.json.Json

object StrategicObjectExplanationTraceRunner:

  final case class Config(
      outputPath: Path,
      evaluationPath: Option[Path],
      runTailRiskGate: Boolean,
      macroPassThreshold: Double,
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
    if config.runTailRiskGate then
      val evaluation = StrategicObjectExplanationTraceSupport.tailRiskEvaluation(selected, config.macroPassThreshold)
      val reportPath =
        config.evaluationPath.getOrElse(
          config.outputPath
            .getParent
            .resolve(
              s"${config.outputPath.getFileName.toString.replaceFirst("\\.jsonl$", "")}.tail-risk.json"
            )
        )
      writeText(reportPath, Json.prettyPrint(Json.toJson(evaluation)))
      println(
        s"[strategic-object-explanation-trace] tail-risk: macroPass=${evaluation.macroMetrics.passRate}, " +
          s"plannerLeaks=${evaluation.tailRisk.plannerLeakRows}/${evaluation.tailRisk.hardestRows}, " +
          s"threshold=${evaluation.macroPassThreshold}"
      )
      if !evaluation.passed then
        evaluation.failures.foreach { failure => println(s"[strategic-object-explanation-trace] tail-risk failure: $failure") }
        sys.exit(1)
    writeText(config.outputPath, StrategicObjectExplanationTraceSupport.renderJsonl(selected))
    println(s"[strategic-object-explanation-trace] wrote ${selected.size} rows to ${config.outputPath}")

  private def parseArgs(args: List[String]): Either[String, Config] =
    val workspaceRoot = detectWorkspaceRoot()
    val defaults =
      Config(
        outputPath =
          workspaceRoot.resolve(
            Path.of("tmp", "strategic_object", "reports", "StrategicObjectExplanationTrace.latest.jsonl")
          ),
        evaluationPath = None,
        runTailRiskGate = false,
        macroPassThreshold = StrategicObjectExplanationTraceSupport.tailRiskMacroPassThreshold,
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
        case "--tail-risk" :: tail =>
          loop(tail, cfg.copy(runTailRiskGate = true))
        case head :: tail if head.startsWith("--tail-risk-threshold=") =>
          head.stripPrefix("--tail-risk-threshold=").toDoubleOption match
            case Some(value) if value >= 0.0 && value <= 1.0 => loop(tail, cfg.copy(macroPassThreshold = value))
            case _                                          => Left(s"invalid --tail-risk-threshold: $head")
        case "--tail-risk-threshold" :: value :: tail =>
          value.toDoubleOption match
            case Some(value) if value >= 0.0 && value <= 1.0 => loop(tail, cfg.copy(macroPassThreshold = value))
            case _                                          => Left(s"invalid --tail-risk-threshold: $value")
        case head :: tail if head.startsWith("--tail-risk-output=") =>
          loop(tail, cfg.copy(evaluationPath = Some(Path.of(head.stripPrefix("--tail-risk-output=")).toAbsolutePath.normalize)))
        case "--tail-risk-output" :: value :: tail =>
          loop(tail, cfg.copy(evaluationPath = Some(Path.of(value).toAbsolutePath.normalize)))
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
