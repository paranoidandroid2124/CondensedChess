package lila.commentary.root

private[root] object RootCoverageMatrix:

  import RootAtomRegistry.*

  private val resourcePath = "/commentary-corpus/root-coverage-matrix.md"

  private val notesBySchema: Map[String, String] = Map(
    SchemaId.PieceOn -> "seed exact only",
    SchemaId.ControlledBy -> "seed exact only",
    SchemaId.PawnControlledBy -> "seed exact + near_miss",
    SchemaId.Contested -> "seed exact + blocker near_miss",
    SchemaId.OpenFile -> "seed exact only",
    SchemaId.HalfOpenFile -> "seed exact + near_miss",
    SchemaId.KingRingSquare -> "seed exact only",
    SchemaId.WeakSquare -> "seed exact + near_miss",
    SchemaId.OutpostSquare -> "edge-file exact; no-support and blocked/challenge negatives",
    SchemaId.IsolatedPawn -> "seed exact + near_miss",
    SchemaId.BackwardPawn -> "seed exact + blockade exact + near_miss",
    SchemaId.DoubledFile -> "seed exact + near_miss",
    SchemaId.PassedPawn -> "seed exact + near_miss + h7 promotion-edge exact",
    SchemaId.CandidatePasser -> "d4 seed + same-rank support exact + a-file balance exact; passed-vs-candidate nasty",
    SchemaId.FixedPawn -> "seed exact only",
    SchemaId.LoosePiece -> "seed exact + defended-queen exact + near_miss",
    SchemaId.PinnedPiece -> "absolute exact + relative exact + near_miss",
    SchemaId.OverloadedPiece -> "seed exact + near_miss",
    SchemaId.TrappedPiece -> "a2 exact + zero-legal-move exact; one-exit near_miss; king/pawn negatives",
    SchemaId.XrayTarget -> "seed exact + near_miss",
    SchemaId.LeverAvailable -> "seed exact only",
    SchemaId.KingShelterHole -> "polarity exact; no-access near_miss; out-of-regime nasty",
    SchemaId.SideToMove -> "seed white-to-move exact",
    SchemaId.CastlingRights -> "KQkq seed + K-only asymmetry exact",
    SchemaId.EnPassantState -> "d-file capture exact; h-file capture exact; legal none with raw ep square"
  )

  def render(rows: Vector[RootExpectationCorpus.Row] = RootExpectationCorpus.loadAll()): String =
    val caseTypesBySchema = rows.groupMap(_.schema)(_.caseType).view.mapValues(_.toSet).toMap

    val body = all.map: schema =>
      val caseTypes = caseTypesBySchema.getOrElse(schema.id, Set.empty)
      val hasRows = caseTypes.nonEmpty
      s"| `${schema.id}` | ${yesNo(caseTypes.contains("exact"))} | ${yesNo(caseTypes.contains("near_miss"))} | ${yesNo(caseTypes.contains("nasty_negative"))} | ${yesNo(hasRows)} | yes | ${notesBySchema.getOrElse(schema.id, "seed corpus")} |"

    (
      List(
        "# Root Coverage Matrix",
        "",
        "`symmetry-covered?` means the schema has at least one corpus row, so the symmetry suite hits it through the full-vector mirror check.",
        "`input-fail-covered?` reflects the strict `RootExtractor.fromFenFailClosed` boundary rather than permissive synthetic corpus extraction.",
        "",
        "| schema | exact present? | near_miss present? | nasty_negative present? | symmetry-covered? | input-fail-covered? | notes |",
        "| --- | --- | --- | --- | --- | --- | --- |"
      ) ++ body
    ).mkString("\n")

  def loadArtifact(): String =
    val source =
      scala.io.Source.fromInputStream(
        Option(getClass.getResourceAsStream(resourcePath))
          .getOrElse(throw IllegalStateException(s"Missing test resource $resourcePath"))
      )
    try source.mkString
    finally source.close()

  private def yesNo(value: Boolean): String =
    if value then "yes" else "no"
