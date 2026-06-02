package lila.commentary.tools.quality

import java.nio.file.{ Path, Paths }

import play.api.libs.json.Json
import lila.commentary.tools.review.CommentaryPlayerQcSupport

object CommentaryQualityRunner:

  import CommentaryPlayerQcSupport.*
  import CommentaryQualitySupport.*
  import CommentaryQualityQuietSupport.*

  final case class Config(
      moveReviewPath: Path = DefaultMoveReviewRunDir.resolve("move_review_outputs.jsonl"),
      metricsRowsPath: Path = DefaultReportDir.resolve("commentary_quality_eval_real_seed_rows.jsonl"),
      metricsSummaryPath: Path = DefaultReportDir.resolve("commentary_quality_eval_real_seed_summaries.jsonl"),
      metricsMarkdownPath: Path = DefaultReportDir.resolve("commentary_quality_eval_real_seed.md"),
      judgePromptPath: Path = DefaultReportDir.resolve("commentary_quality_judge_prompt.txt"),
      quietSupportRowsPath: Path = DefaultReportDir.resolve("commentary_quality_quiet_support_rows.jsonl"),
      quietSupportReportPath: Path = DefaultReportDir.resolve("commentary_quality_quiet_support_report.json"),
      quietSupportMarkdownPath: Path = DefaultReportDir.resolve("commentary_quality_quiet_support_report.md")
  )

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val moveReviewEntries =
      readJsonLines[MoveReviewOutputEntry](config.moveReviewPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[quality] failed to read moveReview outputs `${config.moveReviewPath}`: $err")
          sys.exit(1)

    val (records, summaries) =
      buildRealEvaluationSeedSlice(moveReviewEntries) match
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

    val quietSupportReport =
      buildQuietRichReport(
        moveReviewEntries = moveReviewEntries,
        realShardSource = config.moveReviewPath.toString
      )
    writeJsonLines(config.quietSupportRowsPath, quietSupportReport.rows)
    writeJson(config.quietSupportReportPath, Json.toJson(quietSupportReport))
    writeText(config.quietSupportMarkdownPath, renderQuietRichMarkdown(quietSupportReport))

    println(
      s"[quality] wrote metrics `${config.metricsRowsPath}`, quiet-support `${config.quietSupportReportPath}`, and judge prompt `${config.judgePromptPath}`"
    )

  private def parseConfig(args: List[String]): Config =
    val positional = positionalArgs(args)
    Config(
      moveReviewPath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultMoveReviewRunDir.resolve("move_review_outputs.jsonl")),
      metricsRowsPath = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_metrics_sample_rows.jsonl")),
      metricsSummaryPath = positional.lift(2).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_metrics_sample_summaries.jsonl")),
      metricsMarkdownPath = positional.lift(3).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_metrics_sample.md")),
      judgePromptPath = positional.lift(4).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_judge_prompt.txt")),
      quietSupportRowsPath = positional.lift(5).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_quiet_support_rows.jsonl")),
      quietSupportReportPath = positional.lift(6).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_quiet_support_report.json")),
      quietSupportMarkdownPath = positional.lift(7).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_quiet_support_report.md"))
    )

  private def positionalArgs(args: List[String]): List[String] =
    args.filterNot(_.startsWith("--"))
