package lila.commentary.source

import play.api.libs.json.Json

class SourceContextCorpusTest extends munit.FunSuite:

  private val manifests = SourceContextCorpus.loadManifests()
  private val openingLines = SourceContextCorpus.loadOpeningLines()
  private val openingPositions = SourceContextCorpus.loadOpeningPositions()
  private val openingMoveStats = SourceContextCorpus.loadOpeningMoveStats()
  private val openingThemes = SourceContextCorpus.loadOpeningThemes()
  private val motifExamples = SourceContextCorpus.loadMotifExamples()
  private val motifRejects = SourceContextCorpus.loadMotifRejectFixtures()
  private val endgameStudies = SourceContextCorpus.loadEndgameStudies()
  private val endgameFixtures = SourceContextCorpus.loadEndgameStudyFixtures()
  private val endgameRejects = SourceContextCorpus.loadEndgameStudyRejectFixtures()
  private val retrievalExamples = SourceContextCorpus.loadRetrievalExamples()

  test("source context corpus keeps the expected fixture files present"):
    assertEquals(
      SourceContextCorpus.resourceFileNames,
      Vector(
        "opening-sources.jsonl",
        "opening-lines.jsonl",
        "opening-positions.jsonl",
        "opening-move-stats.jsonl",
        "opening-themes.jsonl",
        "motif-examples.jsonl",
        "motif-reject-fixtures.jsonl",
        "endgame-studies.jsonl",
        "endgame-study-fixtures.jsonl",
        "endgame-study-reject-fixtures.jsonl",
        "retrieval-examples.jsonl"
      )
    )
    SourceContextCorpus.resourceFileNames.foreach: fileName =>
      assert(SourceContextCorpus.resourceExists(fileName), s"Missing source context fixture $fileName")

  test("source manifest allows only active reference families"):
    assertEquals(
      manifests.map(_.validatedSourceFamily).toSet,
      Set("opening", "motif", "endgameStudy", "retrieval")
    )
    manifests.foreach: row =>
      row.validatedSourceId
      row.validatedLicense
      row.validatedRedistribution
      row.validatedDerivedData
      row.validatedAttribution
      row.validatedSourceType
      row.validatedSourceUrl

  test("source manifest rejects out-of-scope game-context and endgame result-service families"):
    assertEquals(SourceContextCorpus.sourceFamilyStatus("game" + "Context"), SourceFamilyStatus.Rejected)
    assertEquals(SourceContextCorpus.sourceFamilyStatus("endgame" + "Or" + "acle"), SourceFamilyStatus.Rejected)
    assertEquals(SourceContextCorpus.sourceFamilyStatus("table" + "base"), SourceFamilyStatus.Rejected)
    assertEquals(SourceContextCorpus.sourceFamilyStatus("opening"), SourceFamilyStatus.Active)
    assertEquals(SourceContextCorpus.sourceFamilyStatus("unknownFamily"), SourceFamilyStatus.Rejected)

  test("source manifest rejects unknown licenses"):
    val json = Json.obj(
      "sourceId" -> "bad-license",
      "sourceFamily" -> "opening",
      "sourceType" -> "public_data_aggregate",
      "license" -> "unknown",
      "redistribution" -> "unknown",
      "derivedData" -> "allowed",
      "attributionRequired" -> false,
      "sourceUrl" -> "https://example.invalid/opening"
    )

    interceptMessage[IllegalArgumentException]("Source bad-license uses unsupported license unknown"):
      SourceContextCorpus.parseManifest(json).validatedLicense

  test("source manifest rejects missing provenance URL"):
    val json = Json.obj(
      "sourceId" -> "bad-url",
      "sourceFamily" -> "opening",
      "sourceType" -> "public_data_aggregate",
      "license" -> "CC0-1.0",
      "redistribution" -> "allowed",
      "derivedData" -> "allowed",
      "attributionRequired" -> false,
      "sourceUrl" -> ""
    )

    interceptMessage[IllegalArgumentException]("Source bad-url must declare a provenance URL"):
      SourceContextCorpus.parseManifest(json).validatedSourceUrl

  test("source manifest rejects missing id and unsupported redistribution or derived data"):
    val missingId = Json.obj(
      "sourceFamily" -> "opening",
      "sourceType" -> "public_data_aggregate",
      "license" -> "CC0-1.0",
      "redistribution" -> "allowed",
      "derivedData" -> "allowed",
      "attributionRequired" -> false,
      "sourceUrl" -> "https://example.invalid/opening"
    )
    intercept[IllegalArgumentException]:
      SourceContextCorpus.parseManifest(missingId)

    val badRedistribution = Json.obj(
      "sourceId" -> "bad-redistribution",
      "sourceFamily" -> "opening",
      "sourceType" -> "public_data_aggregate",
      "license" -> "CC0-1.0",
      "redistribution" -> "unknown",
      "derivedData" -> "allowed",
      "attributionRequired" -> false,
      "sourceUrl" -> "https://example.invalid/opening"
    )
    interceptMessage[IllegalArgumentException]("Source bad-redistribution uses unsupported redistribution unknown"):
      SourceContextCorpus.parseManifest(badRedistribution).validatedRedistribution

    val badDerivedData = Json.obj(
      "sourceId" -> "bad-derived-data",
      "sourceFamily" -> "opening",
      "sourceType" -> "public_data_aggregate",
      "license" -> "CC0-1.0",
      "redistribution" -> "allowed",
      "derivedData" -> "unknown",
      "attributionRequired" -> false,
      "sourceUrl" -> "https://example.invalid/opening"
    )
    interceptMessage[IllegalArgumentException]("Source bad-derived-data uses unsupported derivedData unknown"):
      SourceContextCorpus.parseManifest(badDerivedData).validatedDerivedData

  test("opening line schema is ECO-wide and reference-only"):
    openingLines.foreach: row =>
      row.validatedSourceId(manifests)
      row.validatedEco
      row.validatedName
      row.validatedMoveOrder
      row.validatedAuthority
      row.validatedNegativeBoundaries

    assert(openingLines.exists(_.eco == "E04"), "opening fixtures must include an E04 line")
    assert(openingLines.exists(_.eco == "C60"), "opening fixtures must include a C60 line")

  test("opening position index requires exact position key and transposition status"):
    openingPositions.foreach: row =>
      row.validatedSourceId(manifests)
      row.validatedLineId(openingLines)
      row.validatedPositionKey
      row.validatedFen
      row.validatedMoveOrder
      row.validatedTransposition
      row.validatedAuthority

    val downgraded = openingPositions.find(_.id == "open-pos-transposed-downgraded").get
    assertEquals(downgraded.validatedTransposition, "downgraded")

  test("opening name without exact position key is rejected"):
    val json = Json.obj(
      "id" -> "open-pos-name-only",
      "sourceId" -> "lichess-openings",
      "lineId" -> "eco-e04-catalan-open",
      "eco" -> "E04",
      "name" -> "Catalan Opening: Open Defense",
      "positionKey" -> "",
      "fen" -> "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      "ply" -> 0,
      "moveOrder" -> Json.arr("d2d4", "g8f6"),
      "transpositionState" -> "canonical",
      "status" -> "indexed",
      "authority" -> "opening_context",
      "contextRefs" -> Json.arr("eco-e04-catalan-open")
    )

    interceptMessage[IllegalArgumentException]("Opening position open-pos-name-only must declare normalized positionKey"):
      SourceContextCorpus.parseOpeningPosition(json).validatedPositionKey

  test("opening position key must match normalized FEN fields"):
    val json = Json.obj(
      "id" -> "open-pos-wrong-key",
      "sourceId" -> "lichess-openings",
      "lineId" -> "eco-e04-catalan-open",
      "eco" -> "E04",
      "name" -> "Catalan Opening: Open Defense",
      "positionKey" -> "std:not-the-board",
      "fen" -> "rnbqkb1r/ppp2ppp/4pn2/8/2pP4/6P1/PP2PPBP/RNBQK1NR w KQkq - 0 5",
      "ply" -> 8,
      "moveOrder" -> Json.arr("d2d4", "g8f6", "c2c4", "e7e6", "g2g3", "d7d5", "f1g2", "d5c4"),
      "transpositionState" -> "canonical",
      "status" -> "indexed",
      "authority" -> "opening_context",
      "contextRefs" -> Json.arr("eco-e04-catalan-open")
    )

    interceptMessage[IllegalArgumentException]("Opening position open-pos-wrong-key positionKey must match normalized FEN key"):
      SourceContextCorpus.parseOpeningPosition(json).validatedPositionKey

  test("opening position metadata must match referenced line"):
    val json = Json.obj(
      "id" -> "open-pos-line-mismatch",
      "sourceId" -> "lichess-openings",
      "lineId" -> "eco-e04-catalan-open",
      "eco" -> "C60",
      "name" -> "Ruy Lopez",
      "positionKey" -> openingPositions.head.positionKey,
      "fen" -> openingPositions.head.fen,
      "ply" -> 8,
      "moveOrder" -> Json.arr("d2d4", "g8f6", "c2c4", "e7e6", "g2g3", "d7d5", "f1g2", "d5c4"),
      "transpositionState" -> "canonical",
      "status" -> "indexed",
      "authority" -> "opening_context",
      "contextRefs" -> Json.arr("eco-e04-catalan-open")
    )

    interceptMessage[IllegalArgumentException]("Opening position open-pos-line-mismatch metadata must match lineId eco-e04-catalan-open"):
      SourceContextCorpus.parseOpeningPosition(json).validatedLineId(openingLines)

  test("opening candidate move remains statistic or reference"):
    openingMoveStats.foreach: row =>
      row.validatedSourceId(manifests)
      row.validatedPositionKey(openingPositions)
      row.validatedMove
      row.validatedCounts
      row.validatedAuthority
      row.validatedCandidateKind

    val json = Json.obj(
      "id" -> "open-stat-forbidden-leak",
      "sourceId" -> "lichess-games",
      "positionKey" -> openingPositions.head.positionKey,
      "move" -> "g1f3",
      "sampleSize" -> 10,
      "whiteWins" -> 5,
      "draws" -> 2,
      "blackWins" -> 3,
      "frequency" -> 0.5,
      "candidateKind" -> ("be" + "st_move"),
      "authority" -> "opening_statistic",
      "negativeBoundaries" -> Json.arr("candidate_is_not_optimal_move")
    )

    interceptMessage[IllegalArgumentException]("Opening move stat open-stat-forbidden-leak uses forbidden candidateKind"):
      SourceContextCorpus.parseOpeningMoveStat(json).validatedCandidateKind

  test("opening themes remain context references"):
    openingThemes.foreach: row =>
      row.validatedSourceId(manifests)
      row.validatedPositionKey(openingPositions)
      row.validatedTheme
      row.validatedAuthority

  test("opening stats and themes require indexed positions, not downgraded-only keys"):
    val downgradedOnly = Vector(openingPositions.find(_.status == "downgraded").get)
    val stat = openingMoveStats.head.copy(positionKey = downgradedOnly.head.positionKey)
    val theme = openingThemes.head.copy(positionKey = downgradedOnly.head.positionKey)

    interceptMessage[IllegalArgumentException]("Opening move stat open-stat-catalan-qa4 references no indexed positionKey"):
      stat.validatedPositionKey(downgradedOnly)
    interceptMessage[IllegalArgumentException]("Opening theme open-theme-catalan-long-diagonal references no indexed positionKey"):
      theme.validatedPositionKey(downgradedOnly)

  test("opening ambiguous transposition must be downgraded"):
    val json = Json.obj(
      "id" -> "open-pos-ambiguous-indexed",
      "sourceId" -> "lichess-openings",
      "lineId" -> "eco-e04-catalan-open",
      "eco" -> "E04",
      "name" -> "Catalan Opening: Open Defense",
      "positionKey" -> "std:ambiguous",
      "fen" -> "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      "ply" -> 0,
      "moveOrder" -> Json.arr("d2d4", "g8f6"),
      "transpositionState" -> "ambiguous",
      "status" -> "indexed",
      "authority" -> "opening_context",
      "contextRefs" -> Json.arr("eco-e04-catalan-open")
    )

    interceptMessage[IllegalArgumentException]("Opening position open-pos-ambiguous-indexed ambiguous transpositions must be downgraded"):
      SourceContextCorpus.parseOpeningPosition(json).validatedTransposition

  test("motif examples require exact-board detector carrier"):
    motifExamples.foreach: row =>
      row.validatedSourceId(manifests)
      row.validatedMotifId
      row.validatedFen
      row.validatedDetectorCarrier
      row.validatedSourceTags
      row.validatedAuthority
      row.validatedCurrentPositionBoundary

    motifRejects.foreach: row =>
      row.validatedRejectReason
      assertEquals(row.expectation, "rejected")
      interceptMessage[IllegalArgumentException](s"Motif example ${row.id} must bind an exact-board detector carrier"):
        row.asExampleCandidate.validatedDetectorCarrier

    val json = Json.obj(
      "id" -> "motif-tag-only",
      "sourceId" -> "lichess-puzzles",
      "motifId" -> "fork",
      "fen" -> "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
      "sourceTags" -> Json.arr("fork"),
      "authority" -> "motif_example",
      "currentPositionClaim" -> false,
      "negativeBoundaries" -> Json.arr("source_tag_without_detector")
    )

    interceptMessage[IllegalArgumentException]("Motif example motif-tag-only must bind an exact-board detector carrier"):
      SourceContextCorpus.parseMotifExample(json).validatedDetectorCarrier

  test("motif label and source tags cannot leak current-position or Sxx truth"):
    val json = Json.obj(
      "id" -> "motif-label-leak",
      "sourceId" -> "lichess-puzzles",
      "motifId" -> "fork",
      "fen" -> "4k3/8/8/8/8/3N4/8/R3K2r w - - 0 1",
      "detectorCarrier" -> Json.obj("descriptorId" -> "fork", "anchor" -> "piece:d3", "carrierKind" -> "u_witness"),
      "involvedSquares" -> Json.arr("d3"),
      "sourceTags" -> Json.arr("S24", "current_position_truth"),
      "authority" -> "motif_detector",
      "currentPositionClaim" -> false,
      "negativeBoundaries" -> Json.arr("source_tag_is_example_only")
    )

    interceptMessage[IllegalArgumentException]("Motif example motif-label-leak sourceTags contain truth-bearing labels"):
      SourceContextCorpus.parseMotifExample(json).validatedSourceTags

  test("motif detector carrier must match motif identity"):
    val json = Json.obj(
      "id" -> "motif-wrong-carrier",
      "sourceId" -> "lichess-puzzles",
      "motifId" -> "fork",
      "fen" -> "4k3/8/8/8/8/3N4/8/R3K2r w - - 0 1",
      "detectorCarrier" -> Json.obj("descriptorId" -> "pin", "anchor" -> "piece:d3", "carrierKind" -> "u_witness"),
      "involvedSquares" -> Json.arr("d3"),
      "sourceTags" -> Json.arr("fork"),
      "authority" -> "motif_detector",
      "currentPositionClaim" -> false,
      "negativeBoundaries" -> Json.arr("detector_required_for_current_board")
    )

    interceptMessage[IllegalArgumentException]("Motif example motif-wrong-carrier detector carrier must match motifId fork"):
      SourceContextCorpus.parseMotifExample(json).validatedDetectorCarrier

  test("endgame study rows are reference contexts, not result verdicts"):
    endgameStudies.foreach: row =>
      row.validatedSourceId(manifests)
      row.validatedStudyId
      row.validatedNames
      row.validatedMaterialClass
      row.validatedSideToMove
      row.validatedPlacementRules
      row.validatedCandidatePlans
      row.validatedContextBoundary
      row.validatedNegativeBoundaries

    assert(endgameStudies.exists(_.studyId == "lucena_rook_pawn"), "endgame study fixtures must include Lucena")
    assert(endgameStudies.exists(_.studyId == "philidor_rook_pawn"), "endgame study fixtures must include Philidor")

  test("Lucena label without rook-pawn-king placement constraints is rejected"):
    val json = Json.obj(
      "studyId" -> "lucena_missing_rules",
      "names" -> Json.arr("Lucena Position"),
      "materialClass" -> "rook_pawn_vs_rook",
      "sideToMove" -> Json.arr("stronger", "defender"),
      "placementRules" -> Json.arr("pawn_on_seventh"),
      "relationRules" -> Json.arr("rook_check_shelter"),
      "allowedTransforms" -> Json.arr("mirror_files"),
      "candidatePlans" -> Json.arr("bridge_shelter"),
      "sourceRefs" -> Json.arr("endgame-study-public"),
      "authority" -> "endgame_study_context",
      "outcomeClaim" -> "none",
      "negativeBoundaries" -> Json.arr("label_without_exact_applicability")
    )

    interceptMessage[IllegalArgumentException]("Endgame study lucena_missing_rules Lucena context lacks required applicability rules"):
      SourceContextCorpus.parseEndgameStudy(json).validatedPlacementRules

  test("Philidor label without defender rook and king rank relation is rejected"):
    val json = Json.obj(
      "studyId" -> "philidor_missing_rules",
      "names" -> Json.arr("Philidor Position"),
      "materialClass" -> "rook_pawn_vs_rook",
      "sideToMove" -> Json.arr("stronger", "defender"),
      "placementRules" -> Json.arr("defender_king_in_front"),
      "relationRules" -> Json.arr("attacker_pawn_not_on_sixth"),
      "allowedTransforms" -> Json.arr("mirror_files"),
      "candidatePlans" -> Json.arr("third_rank_defense"),
      "sourceRefs" -> Json.arr("endgame-study-public"),
      "authority" -> "endgame_study_context",
      "outcomeClaim" -> "none",
      "negativeBoundaries" -> Json.arr("label_without_exact_applicability")
    )

    interceptMessage[IllegalArgumentException]("Endgame study philidor_missing_rules Philidor context lacks required defender relation rules"):
      SourceContextCorpus.parseEndgameStudy(json).validatedPlacementRules

  test("study name without exact material class is rejected"):
    val json = Json.obj(
      "studyId" -> "lucena-no-material",
      "names" -> Json.arr("Lucena Position"),
      "sideToMove" -> Json.arr("stronger"),
      "placementRules" -> Json.arr("strong_king_near_pawn", "pawn_on_seventh", "defender_king_cut_off"),
      "relationRules" -> Json.arr("rook_check_shelter"),
      "allowedTransforms" -> Json.arr("mirror_files"),
      "candidatePlans" -> Json.arr("bridge_shelter"),
      "sourceRefs" -> Json.arr("endgame-study-public"),
      "authority" -> "endgame_study_context",
      "outcomeClaim" -> "none",
      "negativeBoundaries" -> Json.arr("label_without_exact_material")
    )

    intercept[IllegalArgumentException]:
      SourceContextCorpus.parseEndgameStudy(json)

  test("study context cannot claim win draw or loss without external evidence"):
    val json = Json.obj(
      "studyId" -> "lucena-outcome-leak",
      "names" -> Json.arr("Lucena Position"),
      "materialClass" -> "rook_pawn_vs_rook",
      "sideToMove" -> Json.arr("stronger"),
      "placementRules" -> Json.arr("strong_king_near_pawn", "pawn_on_seventh", "defender_king_cut_off"),
      "relationRules" -> Json.arr("rook_check_shelter"),
      "allowedTransforms" -> Json.arr("mirror_files"),
      "candidatePlans" -> Json.arr("bridge_shelter"),
      "sourceRefs" -> Json.arr("endgame-study-public"),
      "authority" -> "endgame_study_context",
      "outcomeClaim" -> "win",
      "negativeBoundaries" -> Json.arr("pattern_context_is_not_result_truth")
    )

    interceptMessage[IllegalArgumentException]("Endgame study lucena-outcome-leak must not declare outcomeClaim win"):
      SourceContextCorpus.parseEndgameStudy(json).validatedContextBoundary

  test("study candidate plans cannot claim forced conversion"):
    val json = Json.obj(
      "studyId" -> "lucena-plan-leak",
      "names" -> Json.arr("Lucena Position"),
      "materialClass" -> "rook_pawn_vs_rook",
      "sideToMove" -> Json.arr("stronger"),
      "placementRules" -> Json.arr("strong_king_near_pawn", "pawn_on_seventh", "defender_king_cut_off"),
      "relationRules" -> Json.arr("rook_check_shelter"),
      "allowedTransforms" -> Json.arr("mirror_files"),
      "candidatePlans" -> Json.arr("forced_conversion_path"),
      "sourceRefs" -> Json.arr("endgame-study-public"),
      "authority" -> "endgame_study_context",
      "outcomeClaim" -> "none",
      "negativeBoundaries" -> Json.arr("pattern_context_is_not_result_truth")
    )

    interceptMessage[IllegalArgumentException]("Endgame study lucena-plan-leak candidatePlans contain result claims: forced_conversion_path"):
      SourceContextCorpus.parseEndgameStudy(json).validatedCandidatePlans

  test("endgame study fixtures bind exact boards to declared applicability evidence"):
    endgameFixtures.foreach: row =>
      row.validatedStudyId(endgameStudies)
      row.validatedFen
      row.validatedMaterialClass(endgameStudies)
      row.validatedApplicability(endgameStudies)
      row.validatedContextBoundary

    endgameRejects.foreach: row =>
      row.validatedStudyId(endgameStudies)
      row.validatedRejectReason
      row.validatedApplicabilityRejected(endgameStudies)
      assertEquals(row.expectation, "rejected")

  test("retrieval examples remain non-authoritative"):
    retrievalExamples.foreach: row =>
      row.validatedSourceId(manifests)
      row.validatedSourceRef
      row.validatedFen
      row.validatedSimilarityKey
      row.validatedSimilarityScore
      row.validatedAuthority
      row.validatedTags
      row.validatedCurrentPositionBoundary

    val json = Json.obj(
      "id" -> "retrieval-truth-leak",
      "sourceId" -> "lichess-puzzles",
      "sourceRef" -> "puzzle:sample",
      "fen" -> "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
      "similarityKey" -> Json.obj("materialClass" -> "minor_piece", "motifTags" -> Json.arr("fork")),
      "similarityScore" -> 0.84,
      "snippetRole" -> "current_position_truth",
      "currentPositionClaim" -> true,
      "authority" -> "retrieval_example",
      "tags" -> Json.arr("fork")
    )

    interceptMessage[IllegalArgumentException]("Retrieval example retrieval-truth-leak must remain non-authoritative"):
      SourceContextCorpus.parseRetrievalExample(json).validatedCurrentPositionBoundary

  test("retrieval provenance and tags are validated"):
    val emptyRef = Json.obj(
      "id" -> "retrieval-empty-ref",
      "sourceId" -> "example-index",
      "sourceRef" -> "",
      "fen" -> retrievalExamples.head.fen,
      "similarityKey" -> Json.obj("materialClass" -> "minor_piece"),
      "similarityScore" -> 0.5,
      "snippetRole" -> "non_authoritative_context",
      "currentPositionClaim" -> false,
      "authority" -> "retrieval_example",
      "tags" -> Json.arr("example_only")
    )
    interceptMessage[IllegalArgumentException]("Retrieval example retrieval-empty-ref must declare sourceRef"):
      SourceContextCorpus.parseRetrievalExample(emptyRef).validatedSourceRef

    val truthTag = Json.obj(
      "id" -> "retrieval-tag-leak",
      "sourceId" -> "example-index",
      "sourceRef" -> "example:tag-leak",
      "fen" -> retrievalExamples.head.fen,
      "similarityKey" -> Json.obj("materialClass" -> "minor_piece"),
      "similarityScore" -> 0.5,
      "snippetRole" -> "non_authoritative_context",
      "currentPositionClaim" -> false,
      "authority" -> "retrieval_example",
      "tags" -> Json.arr("example_only", "current_position_truth")
    )
    interceptMessage[IllegalArgumentException]("Retrieval example retrieval-tag-leak tags contain truth-bearing labels"):
      SourceContextCorpus.parseRetrievalExample(truthTag).validatedTags

    val extraField = Json.obj(
      "id" -> "retrieval-extra-field",
      "sourceId" -> "example-index",
      "sourceRef" -> "example:extra",
      "fen" -> retrievalExamples.head.fen,
      "similarityKey" -> Json.obj("materialClass" -> "minor_piece"),
      "similarityScore" -> 0.5,
      "snippetRole" -> "non_authoritative_context",
      "currentPositionClaim" -> false,
      "authority" -> "retrieval_example",
      "tags" -> Json.arr("example_only"),
      "claimText" -> "White is winning"
    )
    interceptMessage[IllegalArgumentException]("Retrieval example retrieval-extra-field uses unsupported fields: claimText"):
      SourceContextCorpus.parseRetrievalExample(extraField)
