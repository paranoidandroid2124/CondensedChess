package lila.llm.model.structure

import lila.llm.model.PlanId
import play.api.libs.json.*

case class StructureGoldRow(
    id: String,
    fen: String,
    primary: StructureId,
    alternatives: List[StructureId] = Nil,
    expectedTopPlanIds: List[PlanId] = Nil,
    seedPv: List[String] = Nil,
    sourceGameId: Option[String] = None,
    sourcePly: Option[Int] = None,
    annotators: List[String] = Nil,
    adjudicatedBy: Option[String] = None,
    notes: Option[String] = None
)

object StructureGoldRow:
  given Writes[PlanId] = Writes(v => JsString(v.toString))
  given Reads[PlanId] = Reads {
    case JsString(raw) =>
      PlanId.values.find(_.toString == raw).map(JsSuccess(_)).getOrElse(JsError(s"Unknown PlanId: $raw"))
    case _ => JsError("Expected PlanId string")
  }
  given OFormat[StructureGoldRow] = Json.format[StructureGoldRow]

case class PerClassMetrics(
    precision: Double,
    recall: Double,
    f1: Double,
    support: Int
)

object PerClassMetrics:
  given OFormat[PerClassMetrics] = Json.format[PerClassMetrics]

case class StructureEvalMetrics(
    macroF1: Double,
    unknownFalsePositiveRate: Double,
    perClass: Map[String, PerClassMetrics],
    confusionMatrix: Map[String, Map[String, Int]],
    evaluatedRows: Int
)

object StructureEvalMetrics:
  given OFormat[StructureEvalMetrics] = Json.format[StructureEvalMetrics]

case class AlignmentEvalMetrics(
    top1Accuracy: Double,
    evaluatedRows: Int,
    hitRows: Int
)

object AlignmentEvalMetrics:
  given OFormat[AlignmentEvalMetrics] = Json.format[AlignmentEvalMetrics]

case class GateThresholds(
    macroF1: Double = 0.85,
    alignmentTop1: Double = 0.75,
    unknownFalsePositiveRate: Double = 0.10
)

object GateThresholds:
  given OFormat[GateThresholds] = Json.format[GateThresholds]

case class GateVerdict(
    macroF1Pass: Boolean,
    alignmentTop1Pass: Boolean,
    unknownFalsePositivePass: Boolean,
    overallPass: Boolean,
    thresholds: GateThresholds
)

object GateVerdict:
  given OFormat[GateVerdict] = Json.format[GateVerdict]

case class StructureQualityReport(
    structure: StructureEvalMetrics,
    alignment: AlignmentEvalMetrics,
    gate: GateVerdict
)

object StructureQualityReport:
  given OFormat[StructureQualityReport] = Json.format[StructureQualityReport]
