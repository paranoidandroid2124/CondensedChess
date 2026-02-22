package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import lila.llm.analysis.structure.PawnStructureQualityEvaluator
import lila.llm.model.structure.{ GateThresholds, StructureQualityReport }

object PawnStructureQualityRunner:

  private final case class RunnerConfig(
      inputPaths: List[Path],
      reportPath: Path,
      strict: Boolean,
      minConfidence: Double,
      minMargin: Double,
      thresholds: GateThresholds,
      dualGate: Boolean
  )

  private final case class DatasetResult(
      inputPath: Path,
      report: StructureQualityReport,
      predictions: List[PawnStructureQualityEvaluator.PredictedRow]
  )

  private val V1Thresholds = GateThresholds(macroF1 = 0.80, alignmentTop1 = 0.70, unknownFalsePositiveRate = 0.15)
  private val CuratedThresholds = GateThresholds(macroF1 = 0.85, alignmentTop1 = 0.75, unknownFalsePositiveRate = 0.10)

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[structure-quality] $err")
        sys.exit(2)
      case Right(cfg) =>
        val datasetResultsOrError = cfg.inputPaths.foldLeft[Either[String, List[DatasetResult]]](Right(Nil)) {
          case (Left(err), _) => Left(err)
          case (Right(acc), inputPath) =>
            PawnStructureQualityEvaluator.loadRows(inputPath) match
              case Left(err) => Left(s"[structure-quality] $err")
              case Right(rows) =>
                val predictions = PawnStructureQualityEvaluator.predictRows(
                  rows = rows,
                  minConfidence = cfg.minConfidence,
                  minMargin = cfg.minMargin
                )
                val thresholds = thresholdsFor(inputPath, cfg)
                val report = PawnStructureQualityEvaluator.evaluate(
                  rows = rows,
                  minConfidence = cfg.minConfidence,
                  minMargin = cfg.minMargin,
                  thresholds = thresholds
                )
                Right(acc :+ DatasetResult(inputPath = inputPath, report = report, predictions = predictions))
        }

        datasetResultsOrError match
          case Left(err) =>
            System.err.println(err)
            sys.exit(2)
          case Right(results) =>
            val overallPass = results.nonEmpty && results.forall(_.report.gate.overallPass)
            writeText(cfg.reportPath, renderCombinedReport(results, cfg, overallPass))

            val summary = results.map { r =>
              val s = r.report.structure
              val a = r.report.alignment
              f"${r.inputPath.getFileName.toString}: macroF1=${s.macroF1}%.4f alignTop1=${a.top1Accuracy}%.4f unknownFp=${s.unknownFalsePositiveRate}%.4f pass=${r.report.gate.overallPass}"
            }.mkString(" | ")

            if overallPass then
              println(s"[structure-quality] ✅ overallPass=true dualGate=${cfg.dualGate} report=${cfg.reportPath} :: $summary")
            else
              System.err.println(s"[structure-quality] ❌ overallPass=false dualGate=${cfg.dualGate} report=${cfg.reportPath} :: $summary")
              if cfg.strict then sys.exit(1)

  private def parseArgs(args: List[String]): Either[String, RunnerConfig] =
    val defaults = RunnerConfig(
      inputPaths = Nil,
      reportPath = Paths.get("modules/llm/docs/PawnStructureQualityReport.md"),
      strict = false,
      minConfidence = 0.72,
      minMargin = 0.10,
      thresholds = GateThresholds(),
      dualGate = true
    )

    args.foldLeft[Either[String, RunnerConfig]](Right(defaults)) {
      case (Left(err), _) => Left(err)
      case (Right(cfg), arg) if arg == "--strict" =>
        Right(cfg.copy(strict = true))
      case (Right(cfg), arg) if arg == "--dual-gate" =>
        Right(cfg.copy(dualGate = true))
      case (Right(cfg), arg) if arg == "--no-dual-gate" =>
        Right(cfg.copy(dualGate = false))
      case (Right(cfg), arg) if arg.startsWith("--input=") =>
        Right(cfg.copy(inputPaths = cfg.inputPaths :+ Paths.get(arg.stripPrefix("--input="))))
      case (Right(cfg), arg) if arg.startsWith("--out=") =>
        Right(cfg.copy(reportPath = Paths.get(arg.stripPrefix("--out="))))
      case (Right(cfg), arg) if arg.startsWith("--min-confidence=") =>
        arg.stripPrefix("--min-confidence=").toDoubleOption
          .filter(v => v > 0.0 && v <= 1.0)
          .toRight(s"invalid --min-confidence: $arg")
          .map(v => cfg.copy(minConfidence = v))
      case (Right(cfg), arg) if arg.startsWith("--min-margin=") =>
        arg.stripPrefix("--min-margin=").toDoubleOption
          .filter(v => v >= 0.0 && v <= 1.0)
          .toRight(s"invalid --min-margin: $arg")
          .map(v => cfg.copy(minMargin = v))
      case (Right(cfg), arg) if arg.startsWith("--gate-macro-f1=") =>
        arg.stripPrefix("--gate-macro-f1=").toDoubleOption
          .filter(v => v >= 0.0 && v <= 1.0)
          .toRight(s"invalid --gate-macro-f1: $arg")
          .map(v => cfg.copy(thresholds = cfg.thresholds.copy(macroF1 = v)))
      case (Right(cfg), arg) if arg.startsWith("--gate-align-top1=") =>
        arg.stripPrefix("--gate-align-top1=").toDoubleOption
          .filter(v => v >= 0.0 && v <= 1.0)
          .toRight(s"invalid --gate-align-top1: $arg")
          .map(v => cfg.copy(thresholds = cfg.thresholds.copy(alignmentTop1 = v)))
      case (Right(cfg), arg) if arg.startsWith("--gate-unknown-fp=") =>
        arg.stripPrefix("--gate-unknown-fp=").toDoubleOption
          .filter(v => v >= 0.0 && v <= 1.0)
          .toRight(s"invalid --gate-unknown-fp: $arg")
          .map(v => cfg.copy(thresholds = cfg.thresholds.copy(unknownFalsePositiveRate = v)))
      case (Right(_), arg) =>
        Left(s"unknown argument: $arg")
    }.map { cfg =>
      val defaultInputs = List(Paths.get("modules/llm/src/test/resources/structure_goldset_v1.jsonl"))
      cfg.copy(inputPaths = if cfg.inputPaths.nonEmpty then cfg.inputPaths else defaultInputs)
    }

  private def thresholdsFor(inputPath: Path, cfg: RunnerConfig): GateThresholds =
    if !cfg.dualGate then cfg.thresholds
    else
      val normalized = inputPath.toString.replace("\\", "/").toLowerCase
      if normalized.contains("structure_goldset_v1_llm_curated.jsonl") then CuratedThresholds
      else if normalized.endsWith("/structure_goldset_v1.jsonl") then V1Thresholds
      else cfg.thresholds

  private def writeText(path: Path, text: String): Unit =
    val parent = path.getParent
    if parent != null then Files.createDirectories(parent)
    Files.writeString(path, text, StandardCharsets.UTF_8)

  private def renderCombinedReport(
      results: List[DatasetResult],
      cfg: RunnerConfig,
      overallPass: Boolean
  ): String =
    val sb = new StringBuilder()
    sb.append("# Pawn Structure Quality Report\n\n")
    sb.append(s"- Mode: ${if cfg.dualGate then "DualGate" else "SingleGate"}\n")
    sb.append(s"- Inputs: ${results.map(r => s"`${r.inputPath}`").mkString(", ")}\n")
    sb.append(s"- Overall Gate: ${if overallPass then "PASS" else "FAIL"}\n")
    sb.append(f"- Min Confidence: ${cfg.minConfidence}%.2f | Min Margin: ${cfg.minMargin}%.2f\n")

    results.foreach { r =>
      sb.append("\n---\n\n")
      sb.append(renderDatasetReport(r))
    }

    sb.toString()

  private def renderDatasetReport(dataset: DatasetResult): String =
    val report = dataset.report
    val sb = new StringBuilder()
    sb.append(s"## Dataset: `${dataset.inputPath}`\n\n")
    sb.append(s"- Evaluated Rows: ${report.structure.evaluatedRows}\n")
    sb.append(f"- Macro-F1: ${report.structure.macroF1}%.4f\n")
    sb.append(f"- Unknown False Positive Rate: ${report.structure.unknownFalsePositiveRate}%.4f\n")
    sb.append(f"- Alignment Top1 Accuracy: ${report.alignment.top1Accuracy}%.4f (${report.alignment.hitRows}/${report.alignment.evaluatedRows})\n")
    sb.append(s"- Gate: ${if report.gate.overallPass then "PASS" else "FAIL"}\n")
    sb.append("\n### Thresholds\n")
    sb.append(f"- macro-F1 >= ${report.gate.thresholds.macroF1}%.2f\n")
    sb.append(f"- alignment top1 >= ${report.gate.thresholds.alignmentTop1}%.2f\n")
    sb.append(f"- unknown false positive <= ${report.gate.thresholds.unknownFalsePositiveRate}%.2f\n")
    sb.append("\n### Per-Class Metrics\n")
    sb.append("| Label | Support | Precision | Recall | F1 |\n")
    sb.append("|---|---:|---:|---:|---:|\n")
    report.structure.perClass.toList.sortBy(_._1).foreach { case (label, m) =>
      sb.append(f"| $label | ${m.support} | ${m.precision}%.4f | ${m.recall}%.4f | ${m.f1}%.4f |\n")
    }
    sb.append("\n### Confusion (non-zero rows)\n")
    report.structure.confusionMatrix.toList.sortBy(_._1).foreach { case (gt, preds) =>
      val nonZero = preds.toList.filter(_._2 > 0).sortBy { case (_, c) => -c }
      if nonZero.nonEmpty then
        sb.append(s"- $gt -> ")
        sb.append(nonZero.map { case (pred, c) => s"$pred:$c" }.mkString(", "))
        sb.append("\n")
    }
    val mismatches = dataset.predictions.filter(p => p.row.primary != p.predictedPrimary).take(25)
    if mismatches.nonEmpty then
      sb.append("\n### Sample Mismatches\n")
      sb.append("| id | gt | pred |\n")
      sb.append("|---|---|---|\n")
      mismatches.foreach { m =>
        sb.append(s"| ${m.row.id} | ${m.row.primary.toString} | ${m.predictedPrimary.toString} |\n")
      }
    sb.toString()
