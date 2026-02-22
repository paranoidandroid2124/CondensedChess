package lila.llm.analysis.structure

import java.nio.file.Paths

import munit.FunSuite
import lila.llm.model.structure.GateThresholds

class StructureQualityGateTest extends FunSuite:

  private val v1GoldsetPath =
    Paths.get("modules/llm/src/test/resources/structure_goldset_v1.jsonl")
  private val curatedGoldsetPath =
    Paths.get("modules/llm/src/test/resources/structure_goldset_v1_llm_curated.jsonl")

  test("v1 regression gate should pass with relaxed thresholds") {
    val rows = PawnStructureQualityEvaluator.loadRows(v1GoldsetPath) match
      case Left(err) => fail(err)
      case Right(v) => v

    val report = PawnStructureQualityEvaluator.evaluate(
      rows = rows,
      minConfidence = 0.72,
      minMargin = 0.10,
      thresholds = GateThresholds(
        macroF1 = 0.80,
        alignmentTop1 = 0.70,
        unknownFalsePositiveRate = 0.15
      )
    )

    assert(report.gate.macroF1Pass, clues(report.structure.macroF1, report.structure.perClass))
    assert(report.gate.alignmentTop1Pass, clues(report.alignment.top1Accuracy, report.alignment))
    assert(report.gate.unknownFalsePositivePass, clues(report.structure.unknownFalsePositiveRate))
  }

  test("llm curated quality gate should pass with strict thresholds") {
    val rows = PawnStructureQualityEvaluator.loadRows(curatedGoldsetPath) match
      case Left(err) => fail(err)
      case Right(v) => v

    val report = PawnStructureQualityEvaluator.evaluate(
      rows = rows,
      minConfidence = 0.72,
      minMargin = 0.10,
      thresholds = GateThresholds(
        macroF1 = 0.85,
        alignmentTop1 = 0.75,
        unknownFalsePositiveRate = 0.10
      )
    )

    assert(report.gate.macroF1Pass, clues(report.structure.macroF1, report.structure.perClass))
    assert(report.gate.alignmentTop1Pass, clues(report.alignment.top1Accuracy, report.alignment))
    assert(report.gate.unknownFalsePositivePass, clues(report.structure.unknownFalsePositiveRate))
  }
