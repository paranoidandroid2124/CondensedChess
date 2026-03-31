package lila.llm.tools.quality

import java.nio.file.{ Path, Paths }

import play.api.libs.json.Json
import lila.llm.tools.review.{ ChronicleActivePlannerSliceRunner, CommentaryPlayerQcSupport }

object CommentaryQualityQuietSupportRunner:

  import CommentaryPlayerQcSupport.*
  import CommentaryQualityQuietSupport.*

  final case class Config(
      beforeBookmakerPath: Path = DefaultBookmakerRunDir.resolve("bookmaker_outputs_before.jsonl"),
      afterBookmakerPath: Path = DefaultBookmakerRunDir.resolve("bookmaker_outputs.jsonl"),
      selectorRowsPath: Path = DefaultReportDir.resolve("commentary_quality_quiet_support_selector_rows.jsonl"),
      evalRowsPath: Path = DefaultReportDir.resolve("commentary_quality_quiet_support_eval_rows.jsonl"),
      summaryJsonPath: Path = DefaultReportDir.resolve("commentary_quality_quiet_support_summary.json"),
      summaryMarkdownPath: Path = DefaultReportDir.resolve("commentary_quality_quiet_support_summary.md"),
      beforeChroniclePath: Option[Path] = None,
      afterChroniclePath: Option[Path] = None
  )

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val beforeEntries =
      readJsonLines[BookmakerOutputEntry](config.beforeBookmakerPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[quality-quiet-support] failed to read before bookmaker outputs `${config.beforeBookmakerPath}`: $err")
          sys.exit(1)
    val afterEntries =
      readJsonLines[BookmakerOutputEntry](config.afterBookmakerPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[quality-quiet-support] failed to read after bookmaker outputs `${config.afterBookmakerPath}`: $err")
          sys.exit(1)
    val beforeChronicleEntries =
      config.beforeChroniclePath match
        case Some(path) =>
          readJsonLines[ChronicleActivePlannerSliceRunner.SliceSurfaceEntry](path) match
            case Right(value) => value
            case Left(err) =>
              System.err.println(s"[quality-quiet-support] failed to read before chronicle entries `${path}`: $err")
              sys.exit(1)
        case None => Nil
    val afterChronicleEntries =
      config.afterChroniclePath match
        case Some(path) =>
          readJsonLines[ChronicleActivePlannerSliceRunner.SliceSurfaceEntry](path) match
            case Right(value) => value
            case Left(err) =>
              System.err.println(s"[quality-quiet-support] failed to read after chronicle entries `${path}`: $err")
              sys.exit(1)
        case None => Nil

    val (selectorRows, evalRows, summary) =
      buildQuietSupportEvaluation(
        beforeEntries = beforeEntries,
        afterEntries = afterEntries,
        beforeSource = config.beforeBookmakerPath.toString,
        afterSource = config.afterBookmakerPath.toString,
        beforeChronicleEntries = beforeChronicleEntries,
        afterChronicleEntries = afterChronicleEntries,
        beforeChronicleSource = config.beforeChroniclePath.map(_.toString),
        afterChronicleSource = config.afterChroniclePath.map(_.toString)
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
    val flagNames = Set("--before-chronicle", "--after-chronicle")
    val (positional, flags) =
      args.foldLeft((List.empty[String], Map.empty[String, String], Option.empty[String])) {
        case ((posAcc, flagAcc, pendingFlag), arg) =>
          pendingFlag match
            case Some(flag) =>
              (posAcc, flagAcc.updated(flag, arg), None)
            case None if flagNames.contains(arg) =>
              (posAcc, flagAcc, Some(arg))
            case None =>
              (posAcc :+ arg, flagAcc, None)
      } match
        case (positionalArgs, flagMap, Some(flag)) =>
          throw new IllegalArgumentException(s"missing value for $flag")
        case (positionalArgs, flagMap, None) =>
          (positionalArgs, flagMap)
    Config(
      beforeBookmakerPath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultBookmakerRunDir.resolve("bookmaker_outputs_before.jsonl")),
      afterBookmakerPath = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultBookmakerRunDir.resolve("bookmaker_outputs.jsonl")),
      selectorRowsPath = positional.lift(2).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_quiet_support_selector_rows.jsonl")),
      evalRowsPath = positional.lift(3).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_quiet_support_eval_rows.jsonl")),
      summaryJsonPath = positional.lift(4).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_quiet_support_summary.json")),
      summaryMarkdownPath = positional.lift(5).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("commentary_quality_quiet_support_summary.md")),
      beforeChroniclePath = flags.get("--before-chronicle").map(Paths.get(_)),
      afterChroniclePath = flags.get("--after-chronicle").map(Paths.get(_))
    )
