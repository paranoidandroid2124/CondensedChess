package lila.commentary.tools.quality

import java.nio.file.{ Path, Paths }

import play.api.libs.json.Json
import lila.commentary.tools.review.CommentaryPlayerQcSupport

object CommentaryQualityQuietSupportRunner:

  import CommentaryPlayerQcSupport.*
  import CommentaryQualityQuietSupport.*

  final case class Config(
      beforeMoveReviewPath: Path = DefaultMoveReviewRunDir.resolve("move_review_outputs_before.jsonl"),
      afterMoveReviewPath: Path = DefaultMoveReviewRunDir.resolve("move_review_outputs.jsonl"),
      selectorRowsPath: Path = DefaultReportDir.resolve("commentary_quality_quiet_support_selector_rows.jsonl"),
      evalRowsPath: Path = DefaultReportDir.resolve("commentary_quality_quiet_support_eval_rows.jsonl"),
      summaryJsonPath: Path = DefaultReportDir.resolve("commentary_quality_quiet_support_summary.json"),
      summaryMarkdownPath: Path = DefaultReportDir.resolve("commentary_quality_quiet_support_summary.md")
  )

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val beforeEntries =
      readJsonLines[MoveReviewOutputEntry](config.beforeMoveReviewPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[quality-quiet-support] failed to read before moveReview outputs `${config.beforeMoveReviewPath}`: $err")
          sys.exit(1)
    val afterEntries =
      readJsonLines[MoveReviewOutputEntry](config.afterMoveReviewPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[quality-quiet-support] failed to read after moveReview outputs `${config.afterMoveReviewPath}`: $err")
          sys.exit(1)

    val (selectorRows, evalRows, summary) =
      buildQuietSupportEvaluation(
        beforeEntries = beforeEntries,
        afterEntries = afterEntries,
        beforeSource = config.beforeMoveReviewPath.toString,
        afterSource = config.afterMoveReviewPath.toString
      )

    writeJsonLines(config.selectorRowsPath, selectorRows)
    writeJsonLines(config.evalRowsPath, evalRows)
    writeJson(config.summaryJsonPath, Json.toJson(summary))
    writeText(
      config.summaryMarkdownPath,
      renderQuietSupportSummaryMarkdown(summary, selectorRows, evalRows)
    )

    println(
      s"[quality-quiet-support] wrote selector `${config.selectorRowsPath}`, eval `${config.evalRowsPath}`, and summary `${config.summaryJsonPath}`"
    )

  private def parseConfig(args: List[String]): Config =
    Config(
      beforeMoveReviewPath = args.headOption.map(Paths.get(_)).getOrElse(DefaultMoveReviewRunDir.resolve("move_review_outputs_before.jsonl")),
      afterMoveReviewPath = args.lift(1).map(Paths.get(_)).getOrElse(DefaultMoveReviewRunDir.resolve("move_review_outputs.jsonl")),
      selectorRowsPath = args.lift(2).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_quiet_support_selector_rows.jsonl")),
      evalRowsPath = args.lift(3).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_quiet_support_eval_rows.jsonl")),
      summaryJsonPath = args.lift(4).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_quiet_support_summary.json")),
      summaryMarkdownPath = args.lift(5).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_quiet_support_summary.md"))
    )
