package lila.commentary.witness.u

import chess.format.Fen
import chess.{ Bishop, Color, File, King, Knight, Pawn, Queen, Role, Rook, Square }

import lila.commentary.witness.*
import play.api.libs.json.*

private[commentary] object UWitnessExpectationCorpus:

  private val resourcePath = "/commentary-corpus/witness-expectations.jsonl"

  final case class Row(
      id: String,
      caseType: String,
      fen: String,
      descriptorId: String,
      expectation: String,
      coverageAxis: String,
      coverageBucket: String,
      polarity: String,
      color: Option[String],
      anchorKind: String,
      anchor: String,
      variant: Option[String],
      payloadTokens: Map[String, String],
      payloadTokenLists: Map[String, List[String]],
      payloadRays: Map[String, String],
      payloadColors: Map[String, String],
      payloadFiles: Map[String, String],
      payloadSectors: Map[String, String],
      payloadSquares: Map[String, String],
      payloadSquareLists: Map[String, List[String]],
      payloadRoles: Map[String, String],
      payloadRoleLists: Map[String, List[String]],
      payloadDirectionLists: Map[String, List[String]],
      payloadObjectLists: Map[String, List[JsObject]],
      payloadNumbers: Map[String, Int]
  ):
    def normalizedFen: Fen.Full = Fen.Full.clean(fen)

    def expectedDescriptorId: WitnessDescriptorId =
      WitnessDescriptorId(descriptorId)

    def expectedPolarity: WitnessPolarity =
      polarity match
        case "neutral"     => WitnessPolarity.Neutral
        case "owner"       => WitnessPolarity.Owner
        case "beneficiary" => WitnessPolarity.Beneficiary
        case "host"        => WitnessPolarity.Host
        case other         => throw IllegalArgumentException(s"Row $id has invalid polarity $other")

    def expectedColor: Option[Color] =
      color.map(colorName)

    def expectedVariant: Option[WitnessVariantId] =
      variant.map(WitnessVariantId.apply)

    def expectedAnchor: WitnessAnchor =
      anchorKind match
        case "board" =>
          require(anchor == "board", s"Row $id board anchor must use key board")
          WitnessAnchor.BoardAnchor
        case "file" =>
          WitnessAnchor.FileAnchor(file(anchor))
        case "sector" =>
          WitnessAnchor.SectorAnchor(
            WitnessSector.values.find(_.key == anchor).getOrElse:
              throw IllegalArgumentException(s"Row $id has invalid sector anchor $anchor")
          )
        case "square" =>
          WitnessAnchor.SquareAnchor(square(anchor))
        case "piece_square" =>
          WitnessAnchor.PieceSquareAnchor(square(anchor))
        case "ray" =>
          val parts = anchor.split(":")
          require(parts.length == 2, s"Row $id ray anchor must be square:direction")
          val direction =
            WitnessDirection.values.find(_.key == parts(1)).getOrElse:
              throw IllegalArgumentException(s"Row $id has invalid ray direction ${parts(1)}")
          WitnessAnchor.RayAnchor(WitnessRay(square(parts(0)), direction))
        case other =>
          throw IllegalArgumentException(s"Row $id has invalid anchorKind $other")

    def identityMatches(witness: Witness): Boolean =
      witness.descriptorId == expectedDescriptorId &&
        witness.anchor == expectedAnchor &&
        witness.polarity == expectedPolarity &&
        witness.color == expectedColor &&
        witness.variant == expectedVariant

    def payloadMismatches(witness: Witness): Vector[String] =
      val expectedFields =
        (
          payloadTokens.keySet ++
            payloadTokenLists.keySet ++
            payloadRays.keySet ++
            payloadColors.keySet ++
            payloadFiles.keySet ++
            payloadSectors.keySet ++
            payloadSquares.keySet ++
            payloadSquareLists.keySet ++
            payloadRoles.keySet ++
            payloadRoleLists.keySet ++
            payloadDirectionLists.keySet ++
            payloadObjectLists.keySet ++
            payloadNumbers.keySet
        ).toSet
      val actualFields = witness.payload.entries.map(_._1).toSet
      val shapeMismatches =
        Option
          .when(actualFields != expectedFields)(
            s"payload fields expected ${expectedFields.toVector.sorted.mkString(",")} but found ${actualFields.toVector.sorted.mkString(",")}"
          )
          .toVector
      val tokenMismatches = payloadTokens.toVector.flatMap: (field, expected) =>
        mismatch(field, expected, witness.payload.get(field).collect { case WitnessValue.Token(value) => value })
      val tokenListMismatches = payloadTokenLists.toVector.flatMap: (field, expected) =>
        mismatch(
          field,
          expected,
          witness.payload.get(field).collect { case WitnessValue.TokenListValue(values) => values.toList }
        )
      val rayMismatches = payloadRays.toVector.flatMap: (field, expected) =>
        mismatch(field, expected, witness.payload.get(field).collect { case WitnessValue.RayValue(value) => value.key })
      val colorMismatches = payloadColors.toVector.flatMap: (field, expected) =>
        mismatch(field, expected, witness.payload.get(field).collect { case WitnessValue.ColorValue(value) => value.name })
      val fileMismatches = payloadFiles.toVector.flatMap: (field, expected) =>
        mismatch(field, expected, witness.payload.get(field).collect { case WitnessValue.FileValue(value) => value.char.toString })
      val sectorMismatches = payloadSectors.toVector.flatMap: (field, expected) =>
        mismatch(field, expected, witness.payload.get(field).collect { case WitnessValue.SectorValue(value) => value.key })
      val squareMismatches = payloadSquares.toVector.flatMap: (field, expected) =>
        mismatch(field, expected, witness.payload.get(field).collect { case WitnessValue.SquareValue(value) => value.key })
      val squareListMismatches = payloadSquareLists.toVector.flatMap: (field, expected) =>
        mismatch(
          field,
          expected,
          witness.payload.get(field).collect { case WitnessValue.SquareListValue(values) => values.map(_.key).toList }
        )
      val roleMismatches = payloadRoles.toVector.flatMap: (field, expected) =>
        mismatch(field, expected, witness.payload.get(field).collect { case WitnessValue.RoleValue(value) => roleName(value) })
      val roleListMismatches = payloadRoleLists.toVector.flatMap: (field, expected) =>
        mismatch(
          field,
          expected,
          witness.payload.get(field).collect { case WitnessValue.RoleListValue(values) => values.map(roleName).toList }
        )
      val directionListMismatches = payloadDirectionLists.toVector.flatMap: (field, expected) =>
        mismatch(
          field,
          expected,
          witness.payload.get(field).collect { case WitnessValue.DirectionListValue(values) => values.map(_.key).toList }
        )
      val objectListMismatches = payloadObjectLists.toVector.flatMap: (field, expected) =>
        mismatch(
          field,
          expected,
          witness.payload.get(field).collect {
            case WitnessValue.ListValue(values) if values.forall(_.isInstanceOf[WitnessValue.ObjectValue]) =>
              values.collect { case WitnessValue.ObjectValue(payload) => payloadToJson(payload) }.toList
          }
        )
      val numberMismatches = payloadNumbers.toVector.flatMap: (field, expected) =>
        mismatch(field, expected, witness.payload.get(field).collect { case WitnessValue.Number(value) => value })
      shapeMismatches ++ tokenMismatches ++ tokenListMismatches ++ rayMismatches ++ colorMismatches ++ fileMismatches ++ sectorMismatches ++ squareMismatches ++ squareListMismatches ++ roleMismatches ++ roleListMismatches ++ directionListMismatches ++ objectListMismatches ++ numberMismatches

    private def mismatch[A](field: String, expected: A, actual: Option[A]): Vector[String] =
      Option.when(actual != Some(expected))(s"$field expected $expected but found ${actual.getOrElse("<missing>")}").toVector

    private def payloadToJson(payload: WitnessPayload): JsObject =
      JsObject(payload.entries.map((field, value) => field -> valueToJson(value)))

    private def valueToJson(value: WitnessValue): JsValue =
      value match
        case WitnessValue.Token(value) => JsString(value)
        case WitnessValue.Number(value) => JsNumber(value)
        case WitnessValue.BooleanValue(value) => JsBoolean(value)
        case WitnessValue.ColorValue(value) => JsString(value.name)
        case WitnessValue.FileValue(value) => JsString(value.char.toString)
        case WitnessValue.SquareValue(value) => JsString(value.key)
        case WitnessValue.RoleValue(value) => JsString(roleName(value))
        case WitnessValue.SectorValue(value) => JsString(value.key)
        case WitnessValue.RayValue(value) => JsString(value.key)
        case WitnessValue.DirectionValue(value) => JsString(value.key)
        case WitnessValue.TokenListValue(values) => JsArray(values.map(JsString.apply))
        case WitnessValue.SquareListValue(values) => JsArray(values.map(square => JsString(square.key)))
        case WitnessValue.RoleListValue(values) => JsArray(values.map(role => JsString(roleName(role))))
        case WitnessValue.DirectionListValue(values) => JsArray(values.map(direction => JsString(direction.key)))
        case WitnessValue.IntListValue(values) => JsArray(values.map(value => JsNumber(value)))
        case WitnessValue.LongMaskValue(value) => JsNumber(value)
        case WitnessValue.FileMaskValue(value) => JsNumber(value)
        case WitnessValue.ListValue(values) => JsArray(values.map(valueToJson))
        case WitnessValue.ObjectValue(payload) => payloadToJson(payload)

  private given Reads[Row] = Reads: json =>
    for
      id <- (json \ "id").validate[String]
      caseType <- (json \ "caseType").validate[String]
      fen <- (json \ "fen").validate[String]
      descriptorId <- (json \ "descriptorId").validate[String]
      expectation <- (json \ "expectation").validate[String]
      coverageAxis <- (json \ "coverageAxis").validate[String]
      coverageBucket <- (json \ "coverageBucket").validate[String]
      polarity <- (json \ "polarity").validate[String]
      color <- (json \ "color").validateOpt[String]
      anchorKind <- (json \ "anchorKind").validate[String]
      anchor <- (json \ "anchor").validate[String]
      variant <- (json \ "variant").validateOpt[String]
      payloadTokens <- (json \ "payloadTokens").validateOpt[Map[String, String]].map(_.getOrElse(Map.empty))
      payloadTokenLists <- (json \ "payloadTokenLists").validateOpt[Map[String, List[String]]].map(_.getOrElse(Map.empty))
      payloadRays <- (json \ "payloadRays").validateOpt[Map[String, String]].map(_.getOrElse(Map.empty))
      payloadColors <- (json \ "payloadColors").validateOpt[Map[String, String]].map(_.getOrElse(Map.empty))
      payloadFiles <- (json \ "payloadFiles").validateOpt[Map[String, String]].map(_.getOrElse(Map.empty))
      payloadSectors <- (json \ "payloadSectors").validateOpt[Map[String, String]].map(_.getOrElse(Map.empty))
      payloadSquares <- (json \ "payloadSquares").validateOpt[Map[String, String]].map(_.getOrElse(Map.empty))
      payloadSquareLists <- (json \ "payloadSquareLists").validateOpt[Map[String, List[String]]].map(_.getOrElse(Map.empty))
      payloadRoles <- (json \ "payloadRoles").validateOpt[Map[String, String]].map(_.getOrElse(Map.empty))
      payloadRoleLists <- (json \ "payloadRoleLists").validateOpt[Map[String, List[String]]].map(_.getOrElse(Map.empty))
      payloadDirectionLists <- (json \ "payloadDirectionLists").validateOpt[Map[String, List[String]]].map(_.getOrElse(Map.empty))
      payloadObjectLists <- (json \ "payloadObjectLists").validateOpt[Map[String, List[JsObject]]].map(_.getOrElse(Map.empty))
      payloadNumbers <- (json \ "payloadNumbers").validateOpt[Map[String, Int]].map(_.getOrElse(Map.empty))
    yield Row(
      id,
      caseType,
      fen,
      descriptorId,
      expectation,
      coverageAxis,
      coverageBucket,
      polarity,
      color,
      anchorKind,
      anchor,
      variant,
      payloadTokens,
      payloadTokenLists,
      payloadRays,
      payloadColors,
      payloadFiles,
      payloadSectors,
      payloadSquares,
      payloadSquareLists,
      payloadRoles,
      payloadRoleLists,
      payloadDirectionLists,
      payloadObjectLists,
      payloadNumbers
    )

  def loadAll(): Vector[Row] =
    val source =
      scala.io.Source.fromInputStream(
        Option(getClass.getResourceAsStream(resourcePath))
          .getOrElse(throw IllegalStateException(s"Missing test resource $resourcePath")),
        "UTF-8"
      )
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
                s"Failed to parse U witness expectation row ${index + 1}: ${JsError.toJson(errors)}"
              )
        .toVector
    finally source.close()

  private def colorName(value: String): Color =
    Color.fromName(value).getOrElse:
      throw IllegalArgumentException(s"Invalid color $value")

  private def square(value: String): Square =
    Square.fromKey(value).getOrElse:
      throw IllegalArgumentException(s"Invalid square $value")

  private def file(value: String): File =
    value.headOption.flatMap(File.fromChar).getOrElse:
      throw IllegalArgumentException(s"Invalid file $value")

  private def roleName(value: Role): String =
    value.name

  val validRoleNames: Set[String] =
    Set(Pawn, Knight, Bishop, Rook, Queen, King).map(roleName)

class UWitnessExpectationCorpusTest extends munit.FunSuite:

  test("formal U witness corpus rows match live U extraction"):
    val rows = UWitnessExpectationCorpus.loadAll()

    assertEquals(rows.size, UBroadCoverageMatrix.expectedFormalCorpusRows)

    rows.foreach: row =>
      val expectedRoleNames = row.payloadRoles.values.toSet ++ row.payloadRoleLists.valuesIterator.flatten.toSet
      assert(
        expectedRoleNames.subsetOf(UWitnessExpectationCorpus.validRoleNames),
        s"${row.id}: invalid role payload names ${expectedRoleNames.diff(UWitnessExpectationCorpus.validRoleNames).toVector.sorted.mkString(", ")}"
      )

      val witnesses =
        if UAttachedScopeContract.activeAttachedDescriptorIds.contains(row.expectedDescriptorId) then
          UAttachedExtractor.fromFen(row.normalizedFen).fold(message => fail(s"${row.id}: $message"), identity).witnesses
        else
          UWitnessExtractor.fromFen(row.normalizedFen).fold(message => fail(s"${row.id}: $message"), identity).witnesses
      val matches = witnesses.forDescriptorId(row.expectedDescriptorId).filter(row.identityMatches)

      row.expectation match
        case "present" =>
          assert(
            matches.nonEmpty,
            s"${row.id}: expected witness ${row.descriptorId}/${row.anchorKind}:${row.anchor} was absent"
          )
          val payloadFailures = matches.flatMap(row.payloadMismatches)
          assert(
            payloadFailures.isEmpty,
            s"${row.id}: payload mismatches: ${payloadFailures.mkString("; ")}"
          )
        case "absent" =>
          assert(
            matches.isEmpty,
            s"${row.id}: expected witness ${row.descriptorId}/${row.anchorKind}:${row.anchor} to be absent, found ${matches.size}"
          )
        case other =>
          fail(s"${row.id}: invalid expectation $other")
