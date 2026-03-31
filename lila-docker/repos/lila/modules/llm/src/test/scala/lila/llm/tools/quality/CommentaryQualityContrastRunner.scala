package lila.llm.tools.quality

import java.nio.file.{ Path, Paths }

import play.api.libs.json.Json
import lila.llm.tools.review.{ ChronicleActivePlannerSliceRunner, CommentaryPlayerQcSupport }

object CommentaryQualityContrastRunner:

  import CommentaryPlayerQcSupport.*
  import CommentaryQualitySupport.*
  import CommentaryQualityContrastSupport.*

  final case class Config(
      beforeBookmakerPath: Path =
        Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\reports\\commentary_quality_real16_20260330\\bookmaker_outputs_real16.jsonl"),
      afterBookmakerPath: Path =
        Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\reports\\commentary_quality_contrast_real16_20260330\\bookmaker_outputs_real16_contrast.jsonl"),
      surfaceEntriesPath: Path =
        Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\reports\\commentary_quality_real16_20260330\\planner_surface_entries_real16.jsonl"),
      outDir: Path =
        Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\reports\\commentary_quality_contrast_real16_20260330")
  )

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val beforeEntries =
      readJsonLines[BookmakerOutputEntry](config.beforeBookmakerPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[quality-contrast] failed to read before bookmaker outputs `${config.beforeBookmakerPath}`: $err")
          sys.exit(1)
    val afterEntries =
      readJsonLines[BookmakerOutputEntry](config.afterBookmakerPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[quality-contrast] failed to read after bookmaker outputs `${config.afterBookmakerPath}`: $err")
          sys.exit(1)
    val surfaceEntries =
      readJsonLines[ChronicleActivePlannerSliceRunner.SliceSurfaceEntry](config.surfaceEntriesPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[quality-contrast] failed to read planner surface entries `${config.surfaceEntriesPath}`: $err")
          sys.exit(1)

    ensureDir(config.outDir)

    val paritySnapshots =
      afterEntries.map(bookmakerParitySnapshot) ++
        surfaceEntries.flatMap(entry => List(chronicleParitySnapshot(entry), activeParitySnapshot(entry)))
    val parityReport = buildSamePlyParityReport(paritySnapshots)
    writeJson(config.outDir.resolve("commentary_quality_contrast_same_ply_parity_after.json"), Json.toJson(parityReport))
    writeText(config.outDir.resolve("commentary_quality_contrast_same_ply_parity_after.md"), renderSamePlyParityMarkdown(parityReport))

    buildContrastReport(beforeEntries, afterEntries, surfaceEntries, parityReport) match
      case Left(err) =>
        System.err.println(s"[quality-contrast] failed to build contrast report: $err")
        sys.exit(1)
      case Right(report) =>
        writeJsonLines(config.outDir.resolve("commentary_quality_contrast_selector_rows.jsonl"), report.selectorRows)
        writeJsonLines(config.outDir.resolve("commentary_quality_contrast_eval_rows.jsonl"), report.evalRows)
        writeJson(config.outDir.resolve("commentary_quality_contrast_summary.json"), Json.toJson(report.summary))
        writeText(config.outDir.resolve("commentary_quality_contrast_summary.md"), renderContrastMarkdown(report))
        println(
          s"[quality-contrast] wrote selector `${config.outDir.resolve("commentary_quality_contrast_selector_rows.jsonl")}` and summary `${config.outDir.resolve("commentary_quality_contrast_summary.json")}`"
        )

  private def parseConfig(args: List[String]): Config =
    val positional = args.filterNot(_.startsWith("--"))
    Config(
      beforeBookmakerPath = positional.headOption.map(Paths.get(_)).getOrElse(Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\reports\\commentary_quality_real16_20260330\\bookmaker_outputs_real16.jsonl")),
      afterBookmakerPath = positional.lift(1).map(Paths.get(_)).getOrElse(Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\reports\\commentary_quality_contrast_real16_20260330\\bookmaker_outputs_real16_contrast.jsonl")),
      surfaceEntriesPath = positional.lift(2).map(Paths.get(_)).getOrElse(Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\reports\\commentary_quality_real16_20260330\\planner_surface_entries_real16.jsonl")),
      outDir = positional.lift(3).map(Paths.get(_)).getOrElse(Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\reports\\commentary_quality_contrast_real16_20260330"))
    )
