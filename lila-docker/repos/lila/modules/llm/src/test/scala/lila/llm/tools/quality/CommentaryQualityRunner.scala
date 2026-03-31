package lila.llm.tools.quality

import java.nio.file.{ Path, Paths }

import play.api.libs.json.Json
import lila.llm.tools.review.{ ChronicleActivePlannerSliceRunner, CommentaryPlayerQcSupport }

object CommentaryQualityRunner:

  import CommentaryPlayerQcSupport.*
  import CommentaryQualitySupport.*
  import CommentaryQualityQuietSupport.*

  final case class Config(
      bookmakerPath: Path = DefaultBookmakerRunDir.resolve("bookmaker_outputs.jsonl"),
      surfaceEntriesPath: Path = DefaultReportDir.resolve("planner_surface_entries.jsonl"),
      parityJsonPath: Path = DefaultReportDir.resolve("commentary_quality_same_ply_parity_report.json"),
      parityMarkdownPath: Path = DefaultReportDir.resolve("commentary_quality_same_ply_parity_report.md"),
      metricsRowsPath: Path = DefaultReportDir.resolve("commentary_quality_eval_real_seed_rows.jsonl"),
      metricsSummaryPath: Path = DefaultReportDir.resolve("commentary_quality_eval_real_seed_summaries.jsonl"),
      metricsMarkdownPath: Path = DefaultReportDir.resolve("commentary_quality_eval_real_seed.md"),
      judgePromptPath: Path = DefaultReportDir.resolve("commentary_quality_judge_prompt.txt"),
      surfaceThresholdRowsPath: Path = DefaultReportDir.resolve("commentary_quality_surface_threshold_rows.jsonl"),
      surfaceThresholdSummaryPath: Path = DefaultReportDir.resolve("commentary_quality_surface_threshold_summary.json"),
      surfaceThresholdMarkdownPath: Path = DefaultReportDir.resolve("commentary_quality_surface_threshold_summary.md"),
      quietSupportRowsPath: Path = DefaultReportDir.resolve("commentary_quality_quiet_support_rows.jsonl"),
      quietSupportReportPath: Path = DefaultReportDir.resolve("commentary_quality_quiet_support_report.json"),
      quietSupportMarkdownPath: Path = DefaultReportDir.resolve("commentary_quality_quiet_support_report.md")
  )

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val bookmakerEntries =
      readJsonLines[BookmakerOutputEntry](config.bookmakerPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[quality] failed to read bookmaker outputs `${config.bookmakerPath}`: $err")
          sys.exit(1)

    val surfaceEntries =
      readJsonLines[ChronicleActivePlannerSliceRunner.SliceSurfaceEntry](config.surfaceEntriesPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[quality] failed to read planner surface entries `${config.surfaceEntriesPath}`: $err")
          sys.exit(1)

    val paritySnapshots =
      bookmakerEntries.map(bookmakerParitySnapshot) ++
        surfaceEntries.flatMap(entry => List(chronicleParitySnapshot(entry), activeParitySnapshot(entry)))
    val parityReport = buildSamePlyParityReport(paritySnapshots)
    writeJson(config.parityJsonPath, Json.toJson(parityReport))
    writeText(config.parityMarkdownPath, renderSamePlyParityMarkdown(parityReport))

    val (records, summaries) =
      buildRealEvaluationSeedSlice(bookmakerEntries, parityReport) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[quality] failed to build real evaluation seed shard: $err")
          sys.exit(1)
    writeJsonLines(config.metricsRowsPath, records)
    writeJsonLines(config.metricsSummaryPath, summaries)
    writeText(config.metricsMarkdownPath, renderEvaluationSummaryMarkdown(records, summaries))

    val baselineByKey = records.groupBy(_.comparisonKey).view.mapValues(_.find(_.candidateLabel == "before")).toMap
    val judgePrompt =
      records
        .find(_.candidateLabel == "after")
        .map(after => renderJudgePrompt(after, baselineByKey.getOrElse(after.comparisonKey, None)))
        .getOrElse(
          "No `after` sample was available for the commentary-quality judge prompt scaffold."
        )
    writeText(config.judgePromptPath, judgePrompt)

    val surfaceThresholdReport =
      buildSurfaceThresholdReport(bookmakerEntries, surfaceEntries, parityReport)
    writeJsonLines(config.surfaceThresholdRowsPath, surfaceThresholdReport.rows)
    writeJson(config.surfaceThresholdSummaryPath, Json.toJson(surfaceThresholdReport.summary))
    writeText(config.surfaceThresholdMarkdownPath, renderSurfaceThresholdMarkdown(surfaceThresholdReport))

    val quietSupportReport =
      buildQuietRichReport(
        bookmakerEntries = bookmakerEntries,
        realShardSource = config.bookmakerPath.toString
      )
    writeJsonLines(config.quietSupportRowsPath, quietSupportReport.rows)
    writeJson(config.quietSupportReportPath, Json.toJson(quietSupportReport))
    writeText(config.quietSupportMarkdownPath, renderQuietRichMarkdown(quietSupportReport))

    println(
      s"[quality] wrote parity `${config.parityJsonPath}`, metrics `${config.metricsRowsPath}`, threshold `${config.surfaceThresholdSummaryPath}`, quiet-support `${config.quietSupportReportPath}`, and judge prompt `${config.judgePromptPath}`"
    )

  private def parseConfig(args: List[String]): Config =
    val positional = positionalArgs(args)
    Config(
      bookmakerPath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultBookmakerRunDir.resolve("bookmaker_outputs.jsonl")),
      surfaceEntriesPath = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("planner_surface_entries.jsonl")),
      parityJsonPath = positional.lift(2).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_same_ply_parity_report.json")),
      parityMarkdownPath = positional.lift(3).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_same_ply_parity_report.md")),
      metricsRowsPath = positional.lift(4).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_metrics_sample_rows.jsonl")),
      metricsSummaryPath = positional.lift(5).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_metrics_sample_summaries.jsonl")),
      metricsMarkdownPath = positional.lift(6).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_metrics_sample.md")),
      judgePromptPath = positional.lift(7).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_judge_prompt.txt")),
      surfaceThresholdRowsPath = positional.lift(8).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_surface_threshold_rows.jsonl")),
      surfaceThresholdSummaryPath = positional.lift(9).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_surface_threshold_summary.json")),
      surfaceThresholdMarkdownPath = positional.lift(10).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_surface_threshold_summary.md")),
      quietSupportRowsPath = positional.lift(11).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_quiet_support_rows.jsonl")),
      quietSupportReportPath = positional.lift(12).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_quiet_support_report.json")),
      quietSupportMarkdownPath = positional.lift(13).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_quiet_support_report.md"))
    )

  private def positionalArgs(args: List[String]): List[String] =
    args.filterNot(_.startsWith("--"))
