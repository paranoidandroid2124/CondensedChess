package lila.commentary.tools.quality

import java.nio.file.{ Path, Paths }
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

import play.api.libs.json.Json
import lila.commentary.tools.review.CommentaryPlayerQcSupport

object CommentaryQualityContrastRunner:

  import CommentaryPlayerQcSupport.*
  import CommentaryQualityContrastSupport.*

  final case class Config(
      beforeMoveReviewPath: Path =
        Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\reports\\commentary_quality_real16_20260330\\move_review_outputs_real16.jsonl"),
      afterMoveReviewPath: Path =
        Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\reports\\commentary_quality_contrast_real16_20260330\\move_review_outputs_real16_contrast.jsonl"),
      outDir: Path =
        Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\reports\\commentary_quality_contrast_real16_20260330"),
      gitDiffRoot: Path = Paths.get("").toAbsolutePath.normalize(),
      gitDiffPaths: List[String] = Nil,
      newHelperCount: Option[Int] = None,
      reusedHelperNames: List[String] = Nil
  )

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val beforeEntries =
      readJsonLines[MoveReviewOutputEntry](config.beforeMoveReviewPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[quality-contrast] failed to read before moveReview outputs `${config.beforeMoveReviewPath}`: $err")
          sys.exit(1)
    val afterEntries =
      readJsonLines[MoveReviewOutputEntry](config.afterMoveReviewPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[quality-contrast] failed to read after moveReview outputs `${config.afterMoveReviewPath}`: $err")
          sys.exit(1)

    ensureDir(config.outDir)

    val codeCost =
      readGitDiffCodeCost(config.gitDiffRoot, config.gitDiffPaths, config.newHelperCount, config.reusedHelperNames)

    buildContrastReport(beforeEntries, afterEntries, codeCost = codeCost) match
      case Left(err) =>
        System.err.println(s"[quality-contrast] failed to build contrast report: $err")
        sys.exit(1)
      case Right(report) =>
        writeJsonLines(config.outDir.resolve("commentary_quality_contrast_selector_rows.jsonl"), report.selectorRows)
        writeJsonLines(config.outDir.resolve("commentary_quality_contrast_eval_rows.jsonl"), report.evalRows)
        writeJson(config.outDir.resolve("commentary_quality_contrast_summary.json"), Json.toJson(report.summary))
        writeText(config.outDir.resolve("commentary_quality_contrast_summary.md"), renderContrastMarkdown(report))
        writeJsonLines(config.outDir.resolve("move_review_quality_gate_rows.jsonl"), report.moveReviewGateRows)
        report.moveReviewGate.foreach { gate =>
          writeJson(config.outDir.resolve("move_review_quality_gate_summary.json"), Json.toJson(gate))
          writeText(config.outDir.resolve("move_review_quality_gate_summary.md"), renderMoveReviewGate(Some(gate)))
        }
        println(
          s"[quality-contrast] wrote selector `${config.outDir.resolve("commentary_quality_contrast_selector_rows.jsonl")}`, contrast summary `${config.outDir.resolve("commentary_quality_contrast_summary.json")}`, and MoveReview gate `${config.outDir.resolve("move_review_quality_gate_summary.json")}`"
        )

  private def parseConfig(args: List[String]): Config =
    val positional = positionalArgs(args)
    Config(
      beforeMoveReviewPath = positional.headOption.map(Paths.get(_)).getOrElse(Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\reports\\commentary_quality_real16_20260330\\move_review_outputs_real16.jsonl")),
      afterMoveReviewPath = positional.lift(1).map(Paths.get(_)).getOrElse(Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\reports\\commentary_quality_contrast_real16_20260330\\move_review_outputs_real16_contrast.jsonl")),
      outDir = positional.lift(2).map(Paths.get(_)).getOrElse(Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\reports\\commentary_quality_contrast_real16_20260330")),
      gitDiffRoot = optionStrings(args, "--git-diff-root").headOption.map(Paths.get(_)).getOrElse(Paths.get("").toAbsolutePath.normalize()),
      gitDiffPaths = optionStrings(args, "--git-diff-path"),
      newHelperCount = optionStrings(args, "--new-helper-count").headOption.flatMap(value => value.toIntOption),
      reusedHelperNames = optionStrings(args, "--reused-helper")
    )

  private def readGitDiffCodeCost(
      root: Path,
      paths: List[String],
      newHelperCount: Option[Int],
      reusedHelperNames: List[String]
  ): Option[CodeCostSummary] =
    def runProcess(command: List[String]): Either[String, String] =
      try
        val process =
          new ProcessBuilder(command.asJava)
            .directory(root.toFile)
            .redirectErrorStream(true)
            .start()
        val source = Source.fromInputStream(process.getInputStream)
        val output =
          try source.mkString
          finally source.close()
        val exit = process.waitFor()
        if exit == 0 then Right(output)
        else Left(output.trim)
      catch
        case NonFatal(err) => Left(err.getMessage)

    val pathArgs = if paths.isEmpty then Nil else "--" :: paths
    val numstat =
      runProcess("git" :: "-C" :: root.toString :: "diff" :: "--numstat" :: pathArgs).toOption
    val nameStatus =
      runProcess("git" :: "-C" :: root.toString :: "diff" :: "--name-status" :: pathArgs).toOption
    numstat.map { output =>
      val (added, deleted) =
        output.linesIterator.foldLeft(0 -> 0) { case ((addAcc, delAcc), line) =>
          val parts = line.split("\\t").toList
          val add = parts.headOption.flatMap(_.toIntOption).getOrElse(0)
          val del = parts.lift(1).flatMap(_.toIntOption).getOrElse(0)
          (addAcc + add) -> (delAcc + del)
        }
      val newFiles =
        nameStatus.map(_.linesIterator.count(line => line.split("\\t").headOption.contains("A")))
      CodeCostSummary(
        netLineAdded = added,
        netLineDeleted = deleted,
        newFileCount = newFiles,
        newHelperCount = newHelperCount,
        reusedExistingHelperNames = reusedHelperNames,
        notes =
          List(
            Some(s"git_diff_root=$root"),
            Option.when(paths.nonEmpty)(s"git_diff_paths=${paths.mkString(",")}")
          ).flatten
      )
    }

  private def positionalArgs(args: List[String]): List[String] =
    args match
      case Nil => Nil
      case flag :: _ :: tail if flag.startsWith("--") => positionalArgs(tail)
      case flag :: tail if flag.startsWith("--")      => positionalArgs(tail)
      case value :: tail                              => value :: positionalArgs(tail)

  private def optionStrings(args: List[String], name: String): List[String] =
    args.sliding(2).collect { case List(flag, value) if flag == name => value }.toList
