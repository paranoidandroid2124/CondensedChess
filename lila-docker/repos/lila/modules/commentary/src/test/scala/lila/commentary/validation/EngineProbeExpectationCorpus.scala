package lila.commentary.validation

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

private[validation] object EngineProbeExpectationCorpus:

  val resourcePath = "/commentary-corpus/engine-probe-expectations.jsonl"
  val supportedLayers: Set[String] = Set("object", "certification", "root")

  final case class Row(
      id: String,
      layer: Option[String],
      maxMatePly: Int,
      maxAbsCp: Option[Int],
      notes: Option[String]
  ):
    def resolvedLayer: String =
      val inferred =
        layer
          .map(_.trim)
          .filter(_.nonEmpty)
          .getOrElse:
            if id.startsWith("cert-") then "certification"
            else if id.startsWith("r-") then "root"
            else "object"
      require(
        supportedLayers.contains(inferred),
        s"Unsupported engine-probe layer $inferred for $id"
      )
      inferred

  private given Reads[Row] =
    (
      (__ \ "id").read[String] and
        (__ \ "layer").readNullable[String] and
        (__ \ "maxMatePly").read[Int] and
        (__ \ "maxAbsCp").readNullable[Int] and
        (__ \ "notes").readNullable[String]
    )(Row.apply)

  def loadAll(): Vector[Row] =
    val source =
      scala.io.Source.fromInputStream(
        Option(getClass.getResourceAsStream(resourcePath))
          .getOrElse(throw IllegalStateException(s"Missing test resource $resourcePath"))
      )
    try
      source
        .getLines()
        .filter(_.nonEmpty)
        .zipWithIndex
        .map: (line, index) =>
          Json.parse(line).validate[Row].fold(
            errors => throw IllegalArgumentException(s"Invalid engine probe row ${index + 1}: $errors"),
            identity
          )
        .toVector
    finally source.close()
