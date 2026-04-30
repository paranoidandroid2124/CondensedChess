package lila.commentary.diagnostic

import java.nio.file.Path

import play.api.libs.json.*

object LowerDiagnosticLargeCorpus:

  final case class Row(
      id: String,
      sourceFile: String,
      sourceKind: String,
      sourceSchema: Option[String],
      caseType: Option[String],
      expectation: Option[String],
      input: LowerLayerDiagnostic.Input,
      metadata: Map[String, String]
  )

  private val trackedResources: Vector[(String, String)] =
    Vector(
      "root-expectations.jsonl" -> "root",
      "witness-expectations.jsonl" -> "witness",
      "object-expectations.jsonl" -> "object",
      "delta-expectations.jsonl" -> "delta",
      "certification-expectations.jsonl" -> "certification",
      "projection-expectations.jsonl" -> "projection",
      "engine-probe-expectations.jsonl" -> "engine_probe"
    )

  def loadTrackedRows(): Vector[Row] =
    trackedResources.flatMap: (file, kind) =>
      loadResource(file).flatMap(rowFromJson(file, kind, _))

  def loadExternalRows(path: Path, maxRows: Option[Int] = None): Vector[Row] =
    val source = scala.io.Source.fromFile(path.toFile, "UTF-8")
    try
      val lines = source.getLines().filter(_.trim.nonEmpty)
      maxRows.fold(lines)(lines.take).zipWithIndex.map: (line, index) =>
        Json.parse(line) match
          case obj: JsObject =>
            rowFromJson(path.getFileName.toString, "external_candidate", obj).getOrElse(
              throw IllegalArgumentException(s"${path.getFileName} row ${index + 1} has no current FEN field")
            )
          case other =>
            throw IllegalArgumentException(s"${path.getFileName} row ${index + 1} is not a JSON object: $other")
      .toVector
    finally source.close()

  private def loadResource(file: String): Vector[JsObject] =
    val path = s"/commentary-corpus/$file"
    val stream =
      Option(getClass.getResourceAsStream(path))
        .getOrElse(throw IllegalStateException(s"Missing test resource $path"))
    val source = scala.io.Source.fromInputStream(stream)
    try
      source
        .getLines()
        .filter(_.trim.nonEmpty)
        .zipWithIndex
        .map: (line, index) =>
          Json.parse(line) match
            case obj: JsObject => obj
            case other =>
              throw IllegalArgumentException(s"$file row ${index + 1} is not a JSON object: $other")
        .toVector
    finally source.close()

  private def rowFromJson(file: String, kind: String, obj: JsObject): Option[Row] =
    val id =
      stringAt(obj, "id")
        .orElse(stringAt(obj, "sampleId"))
        .getOrElse(throw IllegalArgumentException(s"$file row missing id/sampleId: $obj"))
    val currentFen =
      stringAt(obj, "currentFen")
        .orElse(stringAt(obj, "fenAfter"))
        .orElse(stringAt(obj, "fen"))
    currentFen.map: fen =>
      val beforeFen = stringAt(obj, "beforeFen").orElse(stringAt(obj, "fenBefore"))
      val playedMove =
        stringAt(obj, "playedMove")
          .orElse(Option.when(beforeFen.nonEmpty)(stringAt(obj, "playedUci")).flatten)
      val nodeId = stringAt(obj, "nodeId").getOrElse(s"diag-$kind-$id")
      val ply = intAt(obj, "ply").getOrElse(if beforeFen.nonEmpty then 1 else 0)
      val sourceSchema = schemaFor(kind, obj)
      Row(
        id = s"$kind:$id",
        sourceFile = file,
        sourceKind = kind,
        sourceSchema = sourceSchema,
        caseType = stringAt(obj, "caseType"),
        expectation = stringAt(obj, "expectation"),
        input = LowerLayerDiagnostic.Input(
          id = s"$kind:$id",
          currentFen = fen,
          beforeFen = beforeFen,
          playedMove = playedMove,
          nodeId = nodeId,
          ply = ply
        ),
        metadata = Map(
          "sourceId" -> id,
          "sourceKind" -> kind
        ) ++ sourceSchema.map("sourceSchema" -> _) ++
          stringAt(obj, "caseType").map("caseType" -> _) ++
          stringAt(obj, "expectation").map("expectation" -> _) ++
          stringAt(obj, "playedUci").map("playedUci" -> _) ++
          stringAt(obj, "source").map("source" -> _) ++
          stringAt(obj, "sourceKind").map("externalSourceKind" -> _) ++
          stringAt(obj, "pgnPath").map("pgnPath" -> _) ++
          stringAt(obj, "opening").map("opening" -> _) ++
          stringAt(obj, "bestStage").map("bestStage" -> _) ++
          stringAt(obj, "bestAdmission").map("bestAdmission" -> _) ++
          stringAt(obj, "family").map("family" -> _) ++
          stringAt(obj, "mixBucket").map("mixBucket" -> _) ++
          stringAt(obj, "gameKey").map("gameKey" -> _) ++
          jsonAt(obj, "tags").map("tags" -> _) ++
          jsonAt(obj, "axes").map("axes" -> _) ++
          jsonAt(obj, "moveLocalBoundary").map("moveLocalBoundary" -> _)
      )

  private def schemaFor(kind: String, obj: JsObject): Option[String] =
    kind match
      case "root"          => stringAt(obj, "schema")
      case "witness"       => stringAt(obj, "descriptorId")
      case "object"        => stringAt(obj, "family")
      case "delta"         => stringAt(obj, "family")
      case "certification" => stringAt(obj, "family")
      case "projection"    => stringAt(obj, "band").orElse(stringAt(obj, "admissionPath"))
      case "engine_probe"  => stringAt(obj, "schema").orElse(stringAt(obj, "family")).orElse(stringAt(obj, "probeKind"))
      case "external_candidate" =>
        stringAt(obj, "family").orElse(stringAt(obj, "schema")).orElse(stringAt(obj, "descriptorId"))
      case _               => None

  private def stringAt(obj: JsObject, field: String): Option[String] =
    (obj \ field).asOpt[String].filter(_.trim.nonEmpty)

  private def jsonAt(obj: JsObject, field: String): Option[String] =
    (obj \ field).toOption.filterNot(_ == JsNull).map(Json.stringify)

  private def intAt(obj: JsObject, field: String): Option[Int] =
    (obj \ field).asOpt[Int]
