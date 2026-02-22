package lila.llm.analysis.structure

import java.nio.file.Paths

import munit.FunSuite
import lila.llm.model.structure.GateThresholds

class StructureDualDatasetGateTest extends FunSuite:

  private val datasets = List(
    Paths.get("modules/llm/src/test/resources/structure_goldset_v1.jsonl") ->
      GateThresholds(macroF1 = 0.80, alignmentTop1 = 0.70, unknownFalsePositiveRate = 0.15),
    Paths.get("modules/llm/src/test/resources/structure_goldset_v1_llm_curated.jsonl") ->
      GateThresholds(macroF1 = 0.85, alignmentTop1 = 0.75, unknownFalsePositiveRate = 0.10)
  )

  test("dual-dataset quality gate should pass for v1 and llm-curated") {
    val reports = datasets.map { case (path, thresholds) =>
      val rows = PawnStructureQualityEvaluator.loadRows(path) match
        case Left(err) => fail(err)
        case Right(v) => v
      path -> PawnStructureQualityEvaluator.evaluate(
        rows = rows,
        minConfidence = 0.72,
        minMargin = 0.10,
        thresholds = thresholds
      )
    }

    val failures = reports.collect {
      case (path, report) if !report.gate.overallPass =>
        s"${path.getFileName}: macroF1=${report.structure.macroF1} alignTop1=${report.alignment.top1Accuracy} unknownFp=${report.structure.unknownFalsePositiveRate}"
    }

    assert(failures.isEmpty, clues(failures.mkString(" | ")))
  }
