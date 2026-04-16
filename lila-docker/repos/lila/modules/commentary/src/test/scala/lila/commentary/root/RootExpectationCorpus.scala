package lila.commentary.root

import chess.format.Fen
import chess.{ Color, File, Role, Square }

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

private[root] object RootExpectationCorpus:

  import RootAtomRegistry.*

  private val resourcePath = "/commentary-corpus/root-expectations.jsonl"

  final case class Row(
      id: String,
      caseType: String,
      fen: String,
      schema: String,
      polarityColor: Option[String],
      piece: Option[String],
      trueSquares: List[String],
      trueFiles: List[String],
      trueState: Option[String],
      expectedGlobalIndices: List[Int],
      expectedMask64: Option[String],
      expectedFileMask8: Option[String]
  ):
    val rootSchema = requireSchema(schema)

    def normalizedFen: Fen.Full = Fen.Full.clean(fen)

    def requiredColor: Color =
      polarityColor
        .flatMap(Color.fromName)
        .getOrElse(throw IllegalArgumentException(s"Row $id requires a valid polarityColor"))

    def requiredRole: Role =
      piece
        .flatMap(name => Role.allByName.get(name.toLowerCase))
        .getOrElse(throw IllegalArgumentException(s"Row $id requires a valid piece"))

    def derivedSquareMask64: Option[Long] =
      rootSchema.family match
        case SchemaFamily.ColorPieceSquare | SchemaFamily.ColorSquare | SchemaFamily.ColorPawnSquare | SchemaFamily.NeutralSquare =>
          Some(
            trueSquares.foldLeft(0L): (mask, squareKey) =>
              Square.fromKey(squareKey).fold(mask)(square => mask | square.bl)
          )
        case _ => None

    def derivedFileMask8: Option[Int] =
      rootSchema.family match
        case SchemaFamily.ColorFile | SchemaFamily.NeutralFile =>
          Some(
            trueFiles.foldLeft(0): (mask, fileKey) =>
              fileKey.headOption.flatMap(File.fromChar).fold(mask)(file => mask | (1 << file.value))
          )
        case _ => None

  private given Reads[Row] =
    (
      (__ \ "id").read[String] and
        (__ \ "caseType").read[String] and
        (__ \ "fen").read[String] and
        (__ \ "schema").read[String] and
        (__ \ "polarityColor").readNullable[String] and
        (__ \ "piece").readNullable[String] and
        (__ \ "trueSquares").readWithDefault[List[String]](Nil) and
        (__ \ "trueFiles").readWithDefault[List[String]](Nil) and
        (__ \ "trueState").readNullable[String] and
        (__ \ "expectedGlobalIndices").read[List[Int]] and
        (__ \ "expectedMask64").readNullable[String] and
        (__ \ "expectedFileMask8").readNullable[String]
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
          Json.parse(line).validate[Row] match
            case JsSuccess(row, _) => row
            case JsError(errors) =>
              throw IllegalArgumentException(
                s"Failed to parse root expectation row ${index + 1}: ${JsError.toJson(errors)}"
              )
        .toVector
    finally source.close()
