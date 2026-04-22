package lila.commentary.root

class RootExtractorCorpusTest extends munit.FunSuite:

  import RootAtomRegistry.*

  private val rows = RootExpectationCorpus.loadAll()
  private val caseTypesBySchema = rows.groupMap(_.schema)(_.caseType).view.mapValues(_.toSet).toMap

  rows.foreach: row =>
    test(s"root extraction stays deterministic for ${row.id}"):
      if !RootExtractor.implementedSchemas.contains(row.schema) then
        fail(s"Row ${row.id} references unimplemented schema ${row.schema}")

      val extractedOnce = RootExtractor.fromFen(row.normalizedFen).fold(message => fail(message), identity)
      val extractedTwice = RootExtractor.fromFen(row.normalizedFen).fold(message => fail(message), identity)

      assertEquals(extractedOnce, extractedTwice)

      val actualIndices = actualIndicesForRow(row, extractedOnce)
      val expectedIndices = row.expectedGlobalIndices.toVector

      assertEquals(expectedIndices, expectedIndices.sorted.distinct)

      assertEquals(actualIndices, expectedIndices)

      row.expectedMask64.map(parseUnsignedLong).orElse(row.derivedSquareMask64).foreach: expectedMask =>
        val actualMask = actualMask64ForRow(row, extractedOnce).getOrElse(
          fail(s"Row ${row.id} expected a square mask for schema ${row.schema}")
        )
        assertEquals(actualMask, expectedMask)

      row.expectedFileMask8.map(parseUnsignedInt).orElse(row.derivedFileMask8).foreach: expectedMask =>
        val actualMask = actualFileMask8ForRow(row, extractedOnce).getOrElse(
          fail(s"Row ${row.id} expected a file mask for schema ${row.schema}")
        )
        assertEquals(actualMask, expectedMask)

      row.rootSchema.family match
        case SchemaFamily.SideToMoveState | SchemaFamily.CastlingRightsState | SchemaFamily.EnPassantState =>
          assertEquals(actualIndices.size, 1)
        case _ =>

  test("high-risk root atoms carry exact near_miss and nasty_negative coverage"):
    val requiredCaseTypes = Set("exact", "near_miss", "nasty_negative")
    val highRiskSchemas = List(
      SchemaId.OutpostSquare,
      SchemaId.CandidatePasser,
      SchemaId.KingShelterHole,
      SchemaId.TrappedPiece
    )

    highRiskSchemas.foreach: schemaId =>
      val actualCaseTypes = caseTypesBySchema.getOrElse(schemaId, Set.empty)
      assert(
        requiredCaseTypes.subsetOf(actualCaseTypes),
        s"$schemaId case types = ${actualCaseTypes.toVector.sorted.mkString(",")}"
      )

  test("root coverage matrix artifact stays in sync with the corpus"):
    def normalizeSnapshot(value: String): String =
      value.replace("\r\n", "\n").stripSuffix("\n")

    assertEquals(
      normalizeSnapshot(RootCoverageMatrix.loadArtifact()),
      normalizeSnapshot(RootCoverageMatrix.render(rows))
    )

  private def actualIndicesForRow(row: RootExpectationCorpus.Row, vector: RootStateVector): Vector[Int] =
    row.rootSchema.family match
      case SchemaFamily.ColorPieceSquare =>
        val mask = vector.squareMask64(row.schema, color = Some(row.requiredColor), role = Some(row.requiredRole)).getOrElse(0L)
        indicesForSquareMask(row.schema, mask, color = Some(row.requiredColor), role = Some(row.requiredRole))
      case SchemaFamily.ColorSquare | SchemaFamily.ColorPawnSquare =>
        val mask = vector.squareMask64(row.schema, color = Some(row.requiredColor)).getOrElse(0L)
        indicesForSquareMask(row.schema, mask, color = Some(row.requiredColor))
      case SchemaFamily.NeutralSquare =>
        val mask = vector.squareMask64(row.schema).getOrElse(0L)
        indicesForSquareMask(row.schema, mask)
      case SchemaFamily.ColorFile =>
        val mask = vector.fileMask8(row.schema, color = Some(row.requiredColor)).getOrElse(0)
        indicesForFileMask(row.schema, mask, color = Some(row.requiredColor))
      case SchemaFamily.NeutralFile =>
        val mask = vector.fileMask8(row.schema).getOrElse(0)
        indicesForFileMask(row.schema, mask)
      case SchemaFamily.SideToMoveState | SchemaFamily.CastlingRightsState | SchemaFamily.EnPassantState =>
        vector.activeIndicesForSchema(row.schema)

  private def actualMask64ForRow(row: RootExpectationCorpus.Row, vector: RootStateVector): Option[Long] =
    row.rootSchema.family match
      case SchemaFamily.ColorPieceSquare =>
        vector.squareMask64(row.schema, color = Some(row.requiredColor), role = Some(row.requiredRole))
      case SchemaFamily.ColorSquare | SchemaFamily.ColorPawnSquare =>
        vector.squareMask64(row.schema, color = Some(row.requiredColor))
      case SchemaFamily.NeutralSquare =>
        vector.squareMask64(row.schema)
      case _ => None

  private def actualFileMask8ForRow(row: RootExpectationCorpus.Row, vector: RootStateVector): Option[Int] =
    row.rootSchema.family match
      case SchemaFamily.ColorFile => vector.fileMask8(row.schema, color = Some(row.requiredColor))
      case SchemaFamily.NeutralFile => vector.fileMask8(row.schema)
      case _ => None

  private def parseUnsignedLong(hex: String): Long =
    java.lang.Long.parseUnsignedLong(hex.stripPrefix("0x"), 16)

  private def parseUnsignedInt(hex: String): Int =
    java.lang.Integer.parseUnsignedInt(hex.stripPrefix("0x"), 16)
