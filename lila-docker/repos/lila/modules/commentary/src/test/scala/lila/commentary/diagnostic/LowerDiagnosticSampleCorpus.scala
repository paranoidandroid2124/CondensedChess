package lila.commentary.diagnostic

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

object LowerDiagnosticSampleCorpus:

  private val resourcePath = "/commentary-corpus/lower-diagnostic-sample.jsonl"

  final case class Row(
      id: String,
      currentFen: String,
      beforeFen: Option[String],
      playedMove: Option[String],
      nodeId: String,
      ply: Int,
      expectedTacticalVerdict: String,
      expectedPreRendererVerdict: String,
      expectedBreaks: Vector[String]
  ):
    def input: LowerLayerDiagnostic.Input =
      LowerLayerDiagnostic.Input(
        id = id,
        currentFen = currentFen,
        beforeFen = beforeFen,
        playedMove = playedMove,
        nodeId = nodeId,
        ply = ply
      )

  private given Reads[Row] =
    (
      (__ \ "id").read[String] and
        (__ \ "currentFen").read[String] and
        (__ \ "beforeFen").readNullable[String] and
        (__ \ "playedMove").readNullable[String] and
        (__ \ "nodeId").read[String] and
        (__ \ "ply").read[Int] and
        (__ \ "expectedTacticalVerdict").read[String] and
        (__ \ "expectedPreRendererVerdict").read[String] and
        (__ \ "expectedBreaks").readWithDefault[Vector[String]](Vector.empty)
    )(Row.apply)

  def loadAll(): Vector[Row] =
    val stream =
      Option(getClass.getResourceAsStream(resourcePath))
        .getOrElse(throw IllegalStateException(s"Missing test resource $resourcePath"))
    val source = scala.io.Source.fromInputStream(stream)
    try
      source
        .getLines()
        .filter(_.trim.nonEmpty)
        .zipWithIndex
        .map: (line, index) =>
          Json.parse(line).validate[Row] match
            case JsSuccess(row, _) => row
            case JsError(errors) =>
              throw IllegalArgumentException(
                s"Failed to parse lower diagnostic sample row ${index + 1}: ${JsError.toJson(errors)}"
              )
        .toVector
    finally source.close()

final case class LowerDiagnosticSummary(
    total: Int,
    tacticalVerdicts: Map[String, Int],
    preRendererVerdicts: Map[String, Int],
    breakCounts: Map[String, Int]
)

object LowerDiagnosticSummary:
  def from(traces: Vector[LowerLayerDiagnostic.Trace]): LowerDiagnosticSummary =
    LowerDiagnosticSummary(
      total = traces.size,
      tacticalVerdicts = countBy(traces.map(_.tacticalVerdict)),
      preRendererVerdicts = countBy(traces.map(_.preRendererVerdict)),
      breakCounts = countBy(traces.flatMap(_.breaks))
    )

  private def countBy(values: Vector[String]): Map[String, Int] =
    values.groupMapReduce(identity)(_ => 1)(_ + _).withDefaultValue(0)
