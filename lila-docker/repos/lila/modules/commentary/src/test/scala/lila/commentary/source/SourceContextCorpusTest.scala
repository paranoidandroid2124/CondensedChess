package lila.commentary.source

import lila.commentary.certification.{ CertificationEvidenceBundle, CertificationExtractor, CertificationFamilyId, CertificationScopeContract, CertificationVerdict }
import lila.commentary.root.{ RootAtomRegistry, RootExtractor }
import lila.commentary.strategic.StrategicObjectExtractor
import lila.commentary.witness.WitnessDescriptorId
import lila.commentary.witness.u.{ UScopeContract, UWitnessExtractor }
import play.api.libs.json.{ JsObject, Json }

class SourceContextCorpusTest extends munit.FunSuite:

  private val manifests = SourceContextCorpus.loadManifests()
  private val openingLines = SourceContextCorpus.loadOpeningLines()
  private val openingPositions = SourceContextCorpus.loadOpeningPositions()
  private val openingMoveStats = SourceContextCorpus.loadOpeningMoveStats()
  private val openingThemes = SourceContextCorpus.loadOpeningThemes()
  private val openingAliases = SourceContextCorpus.loadOpeningAliases()
  private val motifExamples = SourceContextCorpus.loadMotifExamples()
  private val motifRejects = SourceContextCorpus.loadMotifRejectFixtures()
  private val endgameStudies = SourceContextCorpus.loadEndgameStudies()
  private val endgameFixtures = SourceContextCorpus.loadEndgameStudyFixtures()
  private val endgameRejects = SourceContextCorpus.loadEndgameStudyRejectFixtures()
  private val retrievalExamples = SourceContextCorpus.loadRetrievalExamples()
  private val retrievalRejects = SourceContextCorpus.loadRetrievalRejectFixtures()
  private val motifCarrierRefs = motifExamples.map(_.validatedDetectorCarrier.ref).toSet
  private val endgameApplicabilityRefs = endgameFixtures.flatMap(_.sourceRefs.filter(_.startsWith("endgame-study-applicability:"))).toSet
  private val motifCarrierBindings = motifExamples.map(row => row.validatedDetectorCarrier.ref -> (row.motifId, row.fen)).toMap
  private val endgameApplicabilityBindings = endgameFixtures
    .flatMap(row => row.sourceRefs.filter(_.startsWith("endgame-study-applicability:")).map(ref => ref -> (row.studyId, row.fen)))
    .toMap

  test("source context corpus keeps the expected fixture files present"):
    assertEquals(
      SourceContextCorpus.resourceFileNames,
      Vector(
        "opening-sources.jsonl",
        "opening-lines.jsonl",
        "opening-positions.jsonl",
        "opening-move-stats.jsonl",
        "opening-themes.jsonl",
        "opening-aliases.jsonl",
        "opening-context-candidates.jsonl",
        "motif-examples.jsonl",
        "motif-reject-fixtures.jsonl",
        "endgame-studies.jsonl",
        "endgame-study-fixtures.jsonl",
        "endgame-study-reject-fixtures.jsonl",
        "retrieval-examples.jsonl",
        "retrieval-reject-fixtures.jsonl"
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
      row.validatedRetrievalBoundary
      row.validatedSourceUrl

  test("retrieval source manifest rejects master reference and aggregate roles"):
    val masterLikeRetrieval = Json.obj(
      "sourceId" -> "retrieval-master-like",
      "sourceFamily" -> "retrieval",
      "sourceType" -> "derived_example_index",
      "sourceUse" -> "master_reference",
      "aggregateUse" -> "master_reference_stat",
      "license" -> "CC0-1.0",
      "redistribution" -> "derived_only",
      "derivedData" -> "allowed",
      "attributionRequired" -> false,
      "sourceUrl" -> "local-curation:retrieval-master-like"
    )

    interceptMessage[IllegalArgumentException]("Source retrieval-master-like retrieval source must not declare sourceUse"):
      SourceContextCorpus.parseManifest(masterLikeRetrieval).validatedRetrievalBoundary

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

    val nonTaxonomyLine = openingLines.head.copy(id = "open-line-non-taxonomy-source", sourceId = "lichess-games-standard-rated-2026-03-all")
    interceptMessage[IllegalArgumentException]("requirement failed: Opening line open-line-non-taxonomy-source source must use taxonomy_reference"):
      nonTaxonomyLine.validatedSourceId(manifests)

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

    interceptMessage[IllegalArgumentException]("Opening move stat open-stat-extra-truth uses unsupported fields: bestMove"):
      SourceContextCorpus.parseOpeningMoveStat(json ++ Json.obj("id" -> "open-stat-extra-truth", "candidateKind" -> "statistical_reference", "bestMove" -> "g1f3"))

  test("opening game aggregate source separates smoke data from trend statistics"):
    val smoke = manifests.find(_.sourceId == "lichess-games").getOrElse(fail("missing Lichess game fixture source"))
    assertEquals(smoke.sourceUse, Some("pipeline_smoke"))
    assertEquals(smoke.validatedOpeningAggregateUse, Some("pipeline_smoke"))
    assertEquals(smoke.validatedTrendMetadata, None)

    val recent = manifests
      .find(_.sourceId == "lichess-games-standard-rated-2026-03-all")
      .getOrElse(fail("missing recent Lichess trend source"))
    assertEquals(recent.sourceType, "trend_stat")
    assertEquals(recent.sourceUse, Some("online_trend"))
    assertEquals(recent.validatedOpeningAggregateUse, Some("online_trend_stat"))
    assertEquals(recent.validatedTrendMetadata, Some("online_trend_stat"))
    assertEquals(recent.sourceYear, Some(2026))
    assertEquals(recent.sourceMonth, Some(3))
    assertEquals(recent.ratedFilter, Some("rated"))
    assertEquals(recent.sourceChecksum, Some("d3adc2bcc58e85f4398ece2f7f8ea422c5ec5269a7d2921257bc31c5b914180f"))

    val master = manifests
      .find(_.sourceId == "lichess-broadcast-master-reference")
      .getOrElse(fail("missing Lichess broadcast master reference source"))
    assertEquals(master.sourceType, "masterGameDb")
    assertEquals(master.sourceUse, Some("master_reference"))
    assertEquals(master.validatedOpeningAggregateUse, Some("master_reference_stat"))
    assertEquals(master.validatedMasterReferenceMetadata, Some("master_reference_stat"))
    assertEquals(master.license, "CC-BY-SA-4.0")
    assertEquals(master.attributionRequired, true)
    assertEquals(master.shareAlikeRequired, Some(true))
    assert(master.attributionText.exists(_.contains("Lichess Broadcast")), "master source must carry attribution text")
    assert(master.licenseNotice.exists(_.contains("CC BY-SA 4.0")), "master source must carry license notice")
    assertEquals(master.sourceScope, Some("recent_broadcast"))
    assertEquals(master.playEnvironment, Some("otb"))
    assertEquals(master.playerLevel, Some("master"))
    assertEquals(master.ratingSystem, Some("fide"))
    assertEquals(master.minElo, Some(2200))
    assertEquals(master.titlePolicy, Some("title_or_min_elo"))
    assertEquals(master.timeScope, Some("date_range"))
    assertEquals(master.timeControlScope, Some("classical_rapid"))
    assertEquals(master.perPositionSampleSizeThreshold, Some(5))
    assertEquals(master.dedupePolicy, Some("stable_game_id_or_normalized_pgn_hash"))
    assertEquals(master.annotationPolicy, Some("strip_comments_report_engine_eval"))
    assertEquals(master.rawStoragePolicy, Some("localOnly"))

    val trend = Json.obj(
      "sourceId" -> "recent-trend-missing-metadata",
      "sourceFamily" -> "opening",
      "sourceType" -> "trend_stat",
      "sourceUse" -> "online_trend",
      "license" -> "CC0-1.0",
      "redistribution" -> "allowed",
      "derivedData" -> "allowed",
      "attributionRequired" -> false,
      "sourceUrl" -> "https://database.lichess.org/",
      "aggregateUse" -> "online_trend_stat",
      "generatedStoragePolicy" -> "localOnly"
    )
    interceptMessage[IllegalArgumentException]("Source recent-trend-missing-metadata trend_stat must declare sourceYear"):
      SourceContextCorpus.parseManifest(trend).validatedTrendMetadata

    val committedTrend = Json.obj(
      "sourceId" -> "recent-trend-committed",
      "sourceFamily" -> "opening",
      "sourceType" -> "trend_stat",
      "sourceUse" -> "online_trend",
      "license" -> "CC0-1.0",
      "redistribution" -> "allowed",
      "derivedData" -> "allowed",
      "attributionRequired" -> false,
      "sourceUrl" -> "https://database.lichess.org/",
      "aggregateUse" -> "online_trend_stat",
      "sourceYear" -> 2025,
      "sourceMonth" -> 12,
      "yearMonth" -> "2025-12",
      "ratingBucket" -> "all",
      "timeControlBucket" -> "all",
      "variant" -> "standard",
      "ratedFilter" -> "rated",
      "ratedOnly" -> true,
      "sampleSizeThreshold" -> 25,
      "parserVersion" -> "opening-index-builder-2026-04-24",
      "generatedStoragePolicy" -> "committed"
    )
    interceptMessage[IllegalArgumentException]("Source recent-trend-committed trend output must stay local-only"):
      SourceContextCorpus.parseManifest(committedTrend).validatedTrendMetadata

    val noChecksum = committedTrend ++ Json.obj("generatedStoragePolicy" -> "localOnly")
    interceptMessage[IllegalArgumentException]("Source recent-trend-committed trend_stat must declare sourceChecksum"):
      SourceContextCorpus.parseManifest(noChecksum).validatedTrendMetadata

    val missingRatedOnly = noChecksum ++ Json.obj("sourceChecksum" -> "checksum-fixture")
    val missingRatedOnlyError = intercept[IllegalArgumentException]:
      SourceContextCorpus.parseManifest(missingRatedOnly - "ratedOnly").validatedTrendMetadata
    assert(missingRatedOnlyError.getMessage.contains("Source recent-trend-committed trend_stat must declare ratedOnly true"))

    val pipelineSmokeTrend = noChecksum ++ Json.obj(
      "sourceId" -> "trend-source-smoke-leak",
      "aggregateUse" -> "pipeline_smoke",
      "sourceChecksum" -> "checksum-fixture"
    )
    interceptMessage[IllegalArgumentException]("Source trend-source-smoke-leak trend_stat sourceType must use aggregateUse online_trend_stat"):
      SourceContextCorpus.parseManifest(pipelineSmokeTrend).validatedOpeningAggregateUse

    val onlineTrendAsMaster = noChecksum ++ Json.obj(
      "sourceId" -> "online-trend-as-master",
      "sourceUse" -> "master_reference",
      "sourceChecksum" -> "checksum-fixture"
    )
    interceptMessage[IllegalArgumentException]("Source online-trend-as-master master_reference sourceUse must use sourceType masterGameDb"):
      SourceContextCorpus.parseManifest(onlineTrendAsMaster).validatedOpeningAggregateUse

    val smokeAsTrend = Json.obj(
      "sourceId" -> "smoke-as-trend",
      "sourceFamily" -> "opening",
      "sourceType" -> "aggregateGameDb",
      "sourceUse" -> "pipeline_smoke",
      "license" -> "CC0-1.0",
      "redistribution" -> "allowed",
      "derivedData" -> "allowed",
      "attributionRequired" -> false,
      "sourceUrl" -> "https://database.lichess.org/",
      "aggregateUse" -> "online_trend_stat"
    )
    interceptMessage[IllegalArgumentException]("Source smoke-as-trend aggregateGameDb sourceType must use aggregateUse pipeline_smoke"):
      SourceContextCorpus.parseManifest(smokeAsTrend).validatedOpeningAggregateUse

    val pgnAsMaster = smokeAsTrend ++ Json.obj(
      "sourceId" -> "pgn-as-master",
      "sourceType" -> "publicPgn",
      "sourceUse" -> "master_reference",
      "aggregateUse" -> "master_reference_stat"
    )
    interceptMessage[IllegalArgumentException]("Source pgn-as-master master_reference sourceUse must use sourceType masterGameDb"):
      SourceContextCorpus.parseManifest(pgnAsMaster).validatedSourceUse

  test("master reference source metadata fails closed"):
    val base = Json.obj(
      "sourceId" -> "master-reference-fixture",
      "sourceFamily" -> "opening",
      "sourceType" -> "masterGameDb",
      "sourceUse" -> "master_reference",
      "license" -> "CC-BY-SA-4.0",
      "redistribution" -> "allowed",
      "derivedData" -> "allowed",
      "attributionRequired" -> true,
      "shareAlikeRequired" -> true,
      "attributionText" -> "Source: Lichess Broadcast PGN dumps, licensed under CC BY-SA 4.0.",
      "licenseNotice" -> "Derived opening statistics must preserve CC BY-SA 4.0 attribution and share-alike notice.",
      "sourceUrl" -> "https://database.lichess.org/#broadcasts",
      "aggregateUse" -> "master_reference_stat",
      "sourceScope" -> "recent_broadcast",
      "playEnvironment" -> "otb",
      "playerLevel" -> "master",
      "ratingSystem" -> "fide",
      "minElo" -> 2200,
      "titlePolicy" -> "title_or_min_elo",
      "timeScope" -> "date_range",
      "periodStart" -> "2024-01",
      "periodEnd" -> "2026-03",
      "timeControlScope" -> "classical_rapid",
      "perPositionSampleSizeThreshold" -> 5,
      "dedupePolicy" -> "stable_game_id_or_normalized_pgn_hash",
      "annotationPolicy" -> "strip_comments_report_engine_eval",
      "parserVersion" -> "opening-index-builder-master-reference-2026-04-24",
      "rawStoragePolicy" -> "localOnly",
      "generatedStoragePolicy" -> "localOnly",
      "sourceChecksum" -> "fixture-only"
    )

    assertEquals(SourceContextCorpus.parseManifest(base).validatedMasterReferenceMetadata, Some("master_reference_stat"))
    Vector("full_month", "partial_month", "streamed_sample").foreach: scope =>
      val scoped = base ++ Json.obj("sourceId" -> s"master-reference-$scope", "sourceScope" -> scope)
      assertEquals(SourceContextCorpus.parseManifest(scoped).validatedMasterReferenceMetadata, Some("master_reference_stat"))

    interceptMessage[IllegalArgumentException]("Source master-reference-fixture master_reference must declare licenseNotice"):
      SourceContextCorpus.parseManifest(base - "licenseNotice").validatedMasterReferenceMetadata

    interceptMessage[IllegalArgumentException]("Source master-reference-fixture master_reference must require attribution"):
      SourceContextCorpus.parseManifest(base - "attributionRequired").validatedMasterReferenceMetadata

    interceptMessage[IllegalArgumentException]("Source master-reference-fixture master_reference must require shareAlike"):
      SourceContextCorpus.parseManifest(base - "shareAlikeRequired").validatedMasterReferenceMetadata

    interceptMessage[IllegalArgumentException]("Source master-reference-fixture master_reference must declare attributionText"):
      SourceContextCorpus.parseManifest(base - "attributionText").validatedMasterReferenceMetadata

    interceptMessage[IllegalArgumentException]("Source master-reference-fixture master_reference must declare playEnvironment"):
      SourceContextCorpus.parseManifest(base - "playEnvironment").validatedMasterReferenceMetadata

    val mixedWithoutScope = base ++ Json.obj("playEnvironment" -> "mixed", "sourceScope" -> "recent_broadcast")
    interceptMessage[IllegalArgumentException]("Source master-reference-fixture mixed playEnvironment requires explicit mixed scope"):
      SourceContextCorpus.parseManifest(mixedWithoutScope).validatedMasterReferenceMetadata

    interceptMessage[IllegalArgumentException]("Source master-reference-fixture master_reference must declare playerLevel"):
      SourceContextCorpus.parseManifest(base - "playerLevel").validatedMasterReferenceMetadata

    interceptMessage[IllegalArgumentException]("Source master-reference-fixture master_reference must declare titlePolicy or minElo"):
      SourceContextCorpus.parseManifest((base - "titlePolicy") - "minElo").validatedMasterReferenceMetadata

    interceptMessage[IllegalArgumentException]("Source master-reference-fixture master_reference must declare timeControlScope"):
      SourceContextCorpus.parseManifest(base - "timeControlScope").validatedMasterReferenceMetadata

    val fastOnly = base ++ Json.obj("timeControlScope" -> "fast")
    interceptMessage[IllegalArgumentException]("Source master-reference-fixture master_reference timeControlScope must be classical_rapid"):
      SourceContextCorpus.parseManifest(fastOnly).validatedMasterReferenceMetadata

    interceptMessage[IllegalArgumentException]("Source master-reference-fixture master_reference must declare dedupePolicy"):
      SourceContextCorpus.parseManifest(base - "dedupePolicy").validatedMasterReferenceMetadata

    interceptMessage[IllegalArgumentException]("Source master-reference-fixture master_reference must declare annotationPolicy"):
      SourceContextCorpus.parseManifest(base - "annotationPolicy").validatedMasterReferenceMetadata

    interceptMessage[IllegalArgumentException]("Source master-reference-fixture master_reference must declare positive perPositionSampleSizeThreshold"):
      SourceContextCorpus.parseManifest(base - "perPositionSampleSizeThreshold").validatedMasterReferenceMetadata

    interceptMessage[IllegalArgumentException]("Source master-reference-fixture master_reference must declare sourceChecksum"):
      SourceContextCorpus.parseManifest(base - "sourceChecksum").validatedMasterReferenceMetadata

    interceptMessage[IllegalArgumentException]("Source master-reference-fixture master_reference must declare parserVersion"):
      SourceContextCorpus.parseManifest(base - "parserVersion").validatedMasterReferenceMetadata

    interceptMessage[IllegalArgumentException]("Source master-reference-fixture master_reference raw artifacts must stay local-only"):
      SourceContextCorpus.parseManifest(base - "rawStoragePolicy").validatedMasterReferenceMetadata

    interceptMessage[IllegalArgumentException]("Source master-reference-fixture master_reference generated artifacts must stay local-only"):
      SourceContextCorpus.parseManifest(base - "generatedStoragePolicy").validatedMasterReferenceMetadata

  test("opening themes remain context references"):
    openingThemes.foreach: row =>
      row.validatedSourceId(manifests)
      row.validatedPositionKey(openingPositions)
      row.validatedTheme
      row.validatedAuthority

  test("opening aliases preserve source taxonomy and remain display context only"):
    openingAliases.foreach: row =>
      row.validatedSourceId(manifests)
      row.validatedSourceTaxonomy(openingLines)
      row.validatedAliasKind
      row.validatedDisplayBoundary
      row.validatedSourceRefs
      row.validatedNegativeBoundaries

    assert(openingAliases.exists(_.displayName == "Open Catalan"), "missing Catalan display alias")
    assert(openingAliases.exists(_.displayName == "Spanish Opening"), "missing Ruy Lopez display alias")
    assert(openingAliases.exists(_.displayName == "Closed Spanish"), "missing supported Closed Spanish alias")
    assert(openingAliases.exists(_.displayName == "Berlin Defense"), "missing Berlin Ruy Lopez alias")
    assert(openingAliases.exists(_.displayName == "Giuoco Piano"), "missing Giuoco Piano alias")
    assert(openingAliases.exists(_.displayName == "Evans Gambit"), "missing Evans Italian alias")
    assert(openingAliases.exists(_.displayName == "Two Knights Italian"), "missing Two Knights Italian alias")
    assert(openingAliases.exists(_.displayName == "Open Sicilian"), "missing Open Sicilian alias")
    assert(openingAliases.exists(_.displayName == "Dragon Sicilian"), "missing Dragon Sicilian alias")
    assert(openingAliases.exists(_.displayName == "Accelerated Dragon"), "missing Accelerated Dragon alias")
    assert(openingAliases.exists(_.displayName == "Sveshnikov Sicilian"), "missing Sveshnikov Sicilian alias")
    assert(openingAliases.exists(_.displayName == "Moscow Sicilian"), "missing Moscow Sicilian alias")
    assert(openingAliases.exists(_.displayName == "French Advance"), "missing French Advance alias")
    assert(openingAliases.exists(_.displayName == "French Exchange"), "missing French Exchange alias")
    assert(openingAliases.exists(_.displayName == "Caro-Kann Advance"), "missing Caro-Kann Advance alias")
    assert(openingAliases.exists(_.displayName == "Panov Caro-Kann"), "missing Panov Caro-Kann alias")
    assert(openingAliases.exists(_.displayName == "Portuguese Scandinavian"), "missing Portuguese Scandinavian alias")
    assert(openingAliases.exists(_.displayName == "Icelandic-Palme Gambit"), "missing Icelandic-Palme alias")
    assert(openingAliases.exists(_.displayName == "Alekhine Four Pawns"), "missing Alekhine Four Pawns alias")
    assert(openingAliases.exists(_.displayName == "Alekhine Modern"), "missing Alekhine Modern alias")
    assert(openingAliases.exists(alias => alias.displayName == "QGD Exchange" && alias.aliasKind == "structure_alias"), "missing QGD structure alias")
    assert(openingAliases.exists(_.displayName == "Albin Countergambit"), "missing Albin Countergambit alias")
    assert(openingAliases.exists(_.displayName == "Queen's Gambit Accepted"), "missing QGA alias")
    assert(openingAliases.exists(_.displayName == "Chebanenko Slav"), "missing Chebanenko Slav alias")
    assert(openingAliases.exists(alias => alias.displayName == "Slav Carlsbad" && alias.aliasKind == "structure_alias"), "missing Slav Carlsbad structure alias")
    assert(openingAliases.exists(_.displayName == "Meran Semi-Slav"), "missing Meran Semi-Slav alias")
    assert(openingAliases.exists(_.displayName == "Anti-Moscow Semi-Slav"), "missing Anti-Moscow Semi-Slav alias")
    assert(openingAliases.exists(_.displayName == "Closed Catalan"), "missing Closed Catalan alias")
    assert(openingAliases.exists(_.displayName == "Classical Tarrasch"), "missing Classical Tarrasch alias")
    assert(openingAliases.exists(_.contextName == "Carlsbad context"), "missing QGD structure alias")

    val byFamily = openingAliases.groupBy(_.openingFamily).view.mapValues(_.size).toMap
    assertEquals(byFamily.getOrElse("Ruy Lopez", 0), 6)
    assertEquals(byFamily.getOrElse("Italian Game", 0), 6)
    assertEquals(byFamily.getOrElse("Sicilian Defense", 0), 16)
    assertEquals(byFamily.getOrElse("French Defense", 0), 8)
    assertEquals(byFamily.getOrElse("Caro-Kann Defense", 0), 5)
    assertEquals(byFamily.getOrElse("Scandinavian Defense", 0), 6)
    assertEquals(byFamily.getOrElse("Alekhine Defense", 0), 4)
    assertEquals(byFamily.getOrElse("Queen's Gambit Declined", 0), 8)
    assertEquals(byFamily.getOrElse("Queen's Gambit Accepted", 0), 4)
    assertEquals(byFamily.getOrElse("Slav Defense", 0), 6)
    assertEquals(byFamily.getOrElse("Semi-Slav Defense", 0), 6)
    assertEquals(byFamily.getOrElse("Catalan Opening", 0), 3)
    assertEquals(byFamily.getOrElse("Tarrasch Defense", 0), 3)
    assertEquals(byFamily.getOrElse("King's Indian Defense", 0), 5)
    assertEquals(byFamily.getOrElse("Nimzo-Indian Defense", 0), 5)
    assertEquals(byFamily.getOrElse("Queen's Indian Defense", 0), 6)
    assertEquals(byFamily.getOrElse("Bogo-Indian Defense", 0), 4)
    assertEquals(byFamily.getOrElse("Grünfeld Defense", 0), 5)
    assertEquals(byFamily.getOrElse("Neo-Grünfeld Defense", 0), 3)
    assertEquals(byFamily.getOrElse("Old Indian Defense", 0), 4)
    assertEquals(byFamily.getOrElse("Benoni Defense", 0), 5)
    assertEquals(byFamily.getOrElse("Benko Gambit", 0), 3)
    assert(openingAliases.exists(_.displayName == "KID Sämisch"), "missing KID Sämisch alias")
    assert(openingAliases.exists(_.displayName == "Nimzo Rubinstein"), "missing Nimzo Rubinstein alias")
    assert(openingAliases.exists(alias => alias.displayName == "QID Hedgehog" && alias.aliasKind == "structure_alias"), "missing QID Hedgehog structure alias")
    assert(openingAliases.exists(_.displayName == "Brinckmann Grünfeld"), "missing Brinckmann Grünfeld alias")
    assert(openingAliases.exists(_.displayName == "Benoni-Indian Defense"), "missing Benoni-Indian alias")
    assert(openingAliases.exists(_.displayName == "Benko Fianchetto"), "missing Benko Fianchetto alias")
    assertEquals(byFamily.getOrElse("English Opening", 0), 6)
    assertEquals(byFamily.getOrElse("Réti Opening", 0), 3)
    assertEquals(byFamily.getOrElse("King's Indian Attack", 0), 4)
    assertEquals(byFamily.getOrElse("Nimzo-Larsen Attack", 0), 4)
    assertEquals(byFamily.getOrElse("Bird Opening", 0), 3)
    assertEquals(byFamily.getOrElse("Dutch Defense", 0), 4)
    assertEquals(byFamily.getOrElse("Modern Defense", 0), 3)
    assertEquals(byFamily.getOrElse("Pirc Defense", 0), 3)
    assertEquals(byFamily.getOrElse("Polish Opening", 0), 3)
    assertEquals(byFamily.getOrElse("Trompowsky Attack", 0), 3)
    assertEquals(byFamily.getOrElse("London System", 0), 2)
    assertEquals(byFamily.getOrElse("Queen's Pawn Game", 0), 4)
    assertEquals(byFamily.getOrElse("Zukertort Opening", 0), 4)
    assert(openingAliases.exists(_.displayName == "English Reversed Sicilian"), "missing English Reversed Sicilian alias")
    assert(openingAliases.exists(_.displayName == "Réti Accepted"), "missing Réti Accepted alias")
    assert(openingAliases.exists(_.displayName == "KIA French"), "missing KIA French alias")
    assert(openingAliases.exists(alias => alias.displayName == "Stonewall Dutch" && alias.aliasKind == "structure_alias"), "missing Dutch Stonewall structure alias")
    assert(openingAliases.exists(_.displayName == "Modern Averbakh"), "missing Modern Averbakh alias")
    assert(openingAliases.exists(_.displayName == "Pirc Austrian"), "missing Pirc Austrian alias")
    assert(openingAliases.exists(_.displayName == "QPG London"), "missing Queen's Pawn London alias")
    assert(openingAliases.exists(_.displayName == "Zukertort Fianchetto"), "missing Zukertort Fianchetto alias")
    assertEquals(byFamily.getOrElse("King's Gambit", 0), 1)
    assertEquals(byFamily.getOrElse("King's Gambit Accepted", 0), 1)
    assertEquals(byFamily.getOrElse("King's Gambit Declined", 0), 1)
    assertEquals(byFamily.getOrElse("Vienna Game", 0), 1)
    assertEquals(byFamily.getOrElse("Danish Gambit", 0), 1)
    assertEquals(byFamily.getOrElse("Danish Gambit Accepted", 0), 1)
    assertEquals(byFamily.getOrElse("Latvian Gambit", 0), 1)
    assertEquals(byFamily.getOrElse("Latvian Gambit Accepted", 0), 1)
    assertEquals(byFamily.getOrElse("Englund Gambit", 0), 1)
    assertEquals(byFamily.getOrElse("Englund Gambit Declined", 0), 1)
    assertEquals(byFamily.getOrElse("Blackmar-Diemer Gambit", 0), 1)
    assertEquals(byFamily.getOrElse("Blackmar-Diemer Gambit Accepted", 0), 1)
    assertEquals(byFamily.getOrElse("Indian Defense", 0), 1)
    assertEquals(byFamily.getOrElse("Grob Opening", 0), 1)
    assertEquals(byFamily.getOrElse("Van Geet Opening", 0), 1)
    assertEquals(byFamily.getOrElse("Amar Opening", 0), 1)
    assert(openingAliases.exists(_.displayName == "King's Gambit Accepted"), "missing KGA alias")
    assert(openingAliases.exists(_.displayName == "Vienna Gambit"), "missing Vienna Gambit alias")
    assert(openingAliases.exists(_.displayName == "Blackmar-Diemer Gambit"), "missing Blackmar-Diemer alias")
    assert(openingAliases.exists(_.displayName == "Budapest Defense"), "missing Budapest Defense alias")
    assert(openingAliases.exists(_.displayName == "Grob Opening"), "missing Grob alias")
    assert(openingAliases.exists(_.displayName == "Van Geet Opening"), "missing Van Geet alias")
    assert(openingAliases.exists(_.displayName == "Amar Opening"), "missing Amar alias")
    assert(!byFamily.contains("Scotch Game"), "source taxonomy name is already sufficient for Scotch")

  test("opening aliases reject unknown taxonomy rows and taxonomy rewrites"):
    val unknown = Json.obj(
      "id" -> "open-alias-unknown-line",
      "sourceId" -> "lichess-openings",
      "sourceName" -> "Unknown Opening",
      "canonicalName" -> "Unknown Opening",
      "openingFamily" -> "Unknown Opening",
      "openingVariation" -> "Main Line",
      "displayName" -> "Unknown",
      "contextName" -> "Unknown context",
      "aliasKind" -> "display_alias",
      "sourceRefs" -> Json.arr("eco-a00-missing"),
      "negativeBoundaries" -> Json.arr("alias_without_taxonomy_rejected")
    )
    interceptMessage[IllegalArgumentException]("Opening alias open-alias-unknown-line references unknown taxonomy row eco-a00-missing"):
      SourceContextCorpus.parseOpeningAlias(unknown).validatedSourceTaxonomy(openingLines)

    val rewrite = Json.obj(
      "id" -> "open-alias-taxonomy-rewrite",
      "sourceId" -> "lichess-openings",
      "sourceName" -> "Catalan Opening: Open Defense",
      "canonicalName" -> "Open Catalan",
      "openingFamily" -> "Catalan Opening",
      "openingVariation" -> "Open Defense",
      "displayName" -> "Open Catalan",
      "contextName" -> "Catalan with dxc4",
      "aliasKind" -> "display_alias",
      "sourceRefs" -> Json.arr("eco-e04-catalan-open"),
      "negativeBoundaries" -> Json.arr("alias_does_not_rewrite_source_taxonomy")
    )
    interceptMessage[IllegalArgumentException]("Opening alias open-alias-taxonomy-rewrite canonicalName must match source taxonomy"):
      SourceContextCorpus.parseOpeningAlias(rewrite).validatedSourceTaxonomy(openingLines)

  test("opening aliases reject truth and unsupported taxonomy-sensitive wording"):
    val truth = Json.obj(
      "id" -> "open-alias-truth-leak",
      "sourceId" -> "lichess-openings",
      "sourceName" -> "Catalan Opening: Open Defense",
      "canonicalName" -> "Catalan Opening: Open Defense",
      "openingFamily" -> "Catalan Opening",
      "openingVariation" -> "Open Defense",
      "displayName" -> "Theory Open Catalan",
      "contextName" -> "result truth Catalan",
      "aliasKind" -> "display_alias",
      "sourceRefs" -> Json.arr("eco-e04-catalan-open"),
      "negativeBoundaries" -> Json.arr("alias_authority_wording_rejected")
    )
    interceptMessage[IllegalArgumentException]("Opening alias open-alias-truth-leak contains authority wording"):
      SourceContextCorpus.parseOpeningAlias(truth).validatedDisplayBoundary

    val closedSpanish = Json.obj(
      "id" -> "open-alias-closed-spanish-unsupported",
      "sourceId" -> "lichess-openings",
      "sourceName" -> "Ruy Lopez",
      "canonicalName" -> "Ruy Lopez",
      "openingFamily" -> "Ruy Lopez",
      "openingVariation" -> "Morphy Defense context",
      "displayName" -> "Closed Spanish context",
      "contextName" -> "Closed Spanish",
      "aliasKind" -> "context_alias",
      "sourceRefs" -> Json.arr("eco-c60-ruy-lopez"),
      "negativeBoundaries" -> Json.arr("alias_requires_matching_variation")
    )
    interceptMessage[IllegalArgumentException]("Opening alias open-alias-closed-spanish-unsupported requires a Closed Spanish taxonomy variation"):
      SourceContextCorpus.parseOpeningAlias(closedSpanish).validatedSourceTaxonomy(openingLines)

    val openSicilianUnsupported = Json.obj(
      "id" -> "open-alias-open-sicilian-unsupported",
      "sourceId" -> "lichess-openings",
      "sourceName" -> "Sicilian Defense: Najdorf Variation",
      "canonicalName" -> "Sicilian Defense: Najdorf Variation",
      "openingFamily" -> "Sicilian Defense",
      "openingVariation" -> "Najdorf Variation",
      "displayName" -> "Open Sicilian",
      "contextName" -> "Open Sicilian",
      "aliasKind" -> "context_alias",
      "sourceRefs" -> Json.arr("eco-b90-sicilian-najdorf"),
      "negativeBoundaries" -> Json.arr("alias_requires_matching_variation")
    )
    interceptMessage[IllegalArgumentException]("Opening alias open-alias-open-sicilian-unsupported requires Sicilian variation containing Open"):
      SourceContextCorpus.parseOpeningAlias(openSicilianUnsupported).validatedSourceTaxonomy(openingLines)

    val frenchExchangeUnsupported = Json.obj(
      "id" -> "open-alias-french-exchange-unsupported",
      "sourceId" -> "lichess-openings",
      "sourceName" -> "French Defense: Advance Variation",
      "canonicalName" -> "French Defense: Advance Variation",
      "openingFamily" -> "French Defense",
      "openingVariation" -> "Advance Variation",
      "displayName" -> "French Exchange",
      "contextName" -> "French Exchange Variation",
      "aliasKind" -> "display_alias",
      "sourceRefs" -> Json.arr("eco-c02-french-defense-advance-variation"),
      "negativeBoundaries" -> Json.arr("alias_requires_matching_variation")
    )
    interceptMessage[IllegalArgumentException]("Opening alias open-alias-french-exchange-unsupported requires French variation containing Exchange"):
      SourceContextCorpus.parseOpeningAlias(frenchExchangeUnsupported).validatedSourceTaxonomy(openingLines)

    val qgaQgdConfusion = Json.obj(
      "id" -> "open-alias-qga-qgd-confusion",
      "sourceId" -> "lichess-openings",
      "sourceName" -> "Queen's Gambit Declined: Exchange Variation",
      "canonicalName" -> "Queen's Gambit Declined: Exchange Variation",
      "openingFamily" -> "Queen's Gambit Declined",
      "openingVariation" -> "Exchange Variation",
      "displayName" -> "QGA Exchange",
      "contextName" -> "Queen's Gambit Accepted",
      "aliasKind" -> "display_alias",
      "sourceRefs" -> Json.arr("eco-d35-qgd-exchange"),
      "negativeBoundaries" -> Json.arr("alias_requires_matching_family")
    )
    interceptMessage[IllegalArgumentException]("Opening alias open-alias-qga-qgd-confusion requires Queen's Gambit Accepted family"):
      SourceContextCorpus.parseOpeningAlias(qgaQgdConfusion).validatedSourceTaxonomy(openingLines)

    val closedCatalanUnsupported = Json.obj(
      "id" -> "open-alias-closed-catalan-unsupported",
      "sourceId" -> "lichess-openings",
      "sourceName" -> "Catalan Opening: Open Defense",
      "canonicalName" -> "Catalan Opening: Open Defense",
      "openingFamily" -> "Catalan Opening",
      "openingVariation" -> "Open Defense",
      "displayName" -> "Closed Catalan",
      "contextName" -> "Closed Catalan",
      "aliasKind" -> "context_alias",
      "sourceRefs" -> Json.arr("eco-e04-catalan-open"),
      "negativeBoundaries" -> Json.arr("alias_requires_matching_variation")
    )
    interceptMessage[IllegalArgumentException]("Opening alias open-alias-closed-catalan-unsupported requires Catalan variation containing Closed"):
      SourceContextCorpus.parseOpeningAlias(closedCatalanUnsupported).validatedSourceTaxonomy(openingLines)

    val antiMeranUnsupported = Json.obj(
      "id" -> "open-alias-anti-meran-on-meran-rejected",
      "sourceId" -> "lichess-openings",
      "sourceName" -> "Semi-Slav Defense: Meran Variation",
      "canonicalName" -> "Semi-Slav Defense: Meran Variation",
      "openingFamily" -> "Semi-Slav Defense",
      "openingVariation" -> "Meran Variation",
      "displayName" -> "Anti-Meran Semi-Slav",
      "contextName" -> "Semi-Slav Anti-Meran",
      "aliasKind" -> "display_alias",
      "sourceRefs" -> Json.arr("eco-d47-semi-slav-defense-meran-variation"),
      "negativeBoundaries" -> Json.arr("alias_requires_matching_variation")
    )
    interceptMessage[IllegalArgumentException]("Opening alias open-alias-anti-meran-on-meran-rejected requires Semi-Slav variation containing Anti-Meran"):
      SourceContextCorpus.parseOpeningAlias(antiMeranUnsupported).validatedSourceTaxonomy(openingLines)

    val kidAttack = Json.obj(
      "id" -> "open-alias-kid-attack-rejected",
      "sourceId" -> "lichess-openings",
      "sourceName" -> "King's Indian Defense",
      "canonicalName" -> "King's Indian Defense",
      "openingFamily" -> "King's Indian Defense",
      "openingVariation" -> "Main Line",
      "displayName" -> "KID Attack",
      "contextName" -> "King's Indian pressure",
      "aliasKind" -> "display_alias",
      "sourceRefs" -> Json.arr("eco-e61-king-s-indian-defense"),
      "negativeBoundaries" -> Json.arr("alias_evaluative_wording_rejected")
    )
    interceptMessage[IllegalArgumentException]("Opening alias open-alias-kid-attack-rejected contains authority wording"):
      SourceContextCorpus.parseOpeningAlias(kidAttack).validatedDisplayBoundary

    val grunfeldExchangeUnsupported = Json.obj(
      "id" -> "open-alias-grunfeld-exchange-unsupported",
      "sourceId" -> "lichess-openings",
      "sourceName" -> "Grünfeld Defense",
      "canonicalName" -> "Grünfeld Defense",
      "openingFamily" -> "Grünfeld Defense",
      "openingVariation" -> "Main Line",
      "displayName" -> "Grünfeld Exchange",
      "contextName" -> "Grünfeld Exchange Variation",
      "aliasKind" -> "display_alias",
      "sourceRefs" -> Json.arr("eco-d80-gr-nfeld-defense"),
      "negativeBoundaries" -> Json.arr("alias_requires_matching_variation")
    )
    interceptMessage[IllegalArgumentException]("Opening alias open-alias-grunfeld-exchange-unsupported requires Grünfeld variation containing Exchange"):
      SourceContextCorpus.parseOpeningAlias(grunfeldExchangeUnsupported).validatedSourceTaxonomy(openingLines)

    val pircModernConfusion = Json.obj(
      "id" -> "open-alias-pirc-modern-confusion",
      "sourceId" -> "lichess-openings",
      "sourceName" -> "Modern Defense",
      "canonicalName" -> "Modern Defense",
      "openingFamily" -> "Modern Defense",
      "openingVariation" -> "Main Line",
      "displayName" -> "Pirc Defense",
      "contextName" -> "Pirc setup",
      "aliasKind" -> "display_alias",
      "sourceRefs" -> Json.arr("eco-b06-modern-defense"),
      "negativeBoundaries" -> Json.arr("alias_requires_matching_family")
    )
    interceptMessage[IllegalArgumentException]("Opening alias open-alias-pirc-modern-confusion requires Pirc Defense family"):
      SourceContextCorpus.parseOpeningAlias(pircModernConfusion).validatedSourceTaxonomy(openingLines)

    val reversedSicilianUnsupported = Json.obj(
      "id" -> "open-alias-english-reversed-sicilian-unsupported",
      "sourceId" -> "lichess-openings",
      "sourceName" -> "English Opening",
      "canonicalName" -> "English Opening",
      "openingFamily" -> "English Opening",
      "openingVariation" -> "Main Line",
      "displayName" -> "English Reversed Sicilian",
      "contextName" -> "King's English Reversed Sicilian",
      "aliasKind" -> "display_alias",
      "sourceRefs" -> Json.arr("eco-a10-english-opening"),
      "negativeBoundaries" -> Json.arr("alias_requires_matching_variation")
    )
    interceptMessage[IllegalArgumentException]("Opening alias open-alias-english-reversed-sicilian-unsupported requires English variation containing Reversed Sicilian"):
      SourceContextCorpus.parseOpeningAlias(reversedSicilianUnsupported).validatedSourceTaxonomy(openingLines)

    val budapestGambitUnsupported = Json.obj(
      "id" -> "open-alias-budapest-gambit-unsupported",
      "sourceId" -> "lichess-openings",
      "sourceName" -> "Indian Defense: Budapest Defense",
      "canonicalName" -> "Indian Defense: Budapest Defense",
      "openingFamily" -> "Indian Defense",
      "openingVariation" -> "Budapest Defense",
      "displayName" -> "Budapest Gambit",
      "contextName" -> "Budapest Gambit",
      "aliasKind" -> "display_alias",
      "sourceRefs" -> Json.arr("eco-a51-indian-defense-budapest-defense"),
      "negativeBoundaries" -> Json.arr("gambit_alias_requires_taxonomy_gambit")
    )
    interceptMessage[IllegalArgumentException]("Opening alias open-alias-budapest-gambit-unsupported requires Indian Defense variation containing Gambit"):
      SourceContextCorpus.parseOpeningAlias(budapestGambitUnsupported).validatedSourceTaxonomy(openingLines)

    val blackmarAcceptedUnsupported = Json.obj(
      "id" -> "open-alias-blackmar-accepted-unsupported",
      "sourceId" -> "lichess-openings",
      "sourceName" -> "Blackmar-Diemer Gambit",
      "canonicalName" -> "Blackmar-Diemer Gambit",
      "openingFamily" -> "Blackmar-Diemer Gambit",
      "openingVariation" -> "Main Line",
      "displayName" -> "Blackmar-Diemer Gambit Accepted",
      "contextName" -> "Blackmar-Diemer Accepted context",
      "aliasKind" -> "context_alias",
      "sourceRefs" -> Json.arr("eco-d00-blackmar-diemer-gambit"),
      "negativeBoundaries" -> Json.arr("accepted_alias_requires_accepted_taxonomy")
    )
    interceptMessage[IllegalArgumentException]("Opening alias open-alias-blackmar-accepted-unsupported requires Blackmar-Diemer Gambit Accepted family"):
      SourceContextCorpus.parseOpeningAlias(blackmarAcceptedUnsupported).validatedSourceTaxonomy(openingLines)

    val trapAlias = Json.obj(
      "id" -> "open-alias-englund-trap-rejected",
      "sourceId" -> "lichess-openings",
      "sourceName" -> "Englund Gambit",
      "canonicalName" -> "Englund Gambit",
      "openingFamily" -> "Englund Gambit",
      "openingVariation" -> "Main Line",
      "displayName" -> "Englund Trap",
      "contextName" -> "Dubious Englund",
      "aliasKind" -> "display_alias",
      "sourceRefs" -> Json.arr("eco-a40-englund-gambit"),
      "negativeBoundaries" -> Json.arr("alias_evaluative_wording_rejected")
    )
    interceptMessage[IllegalArgumentException]("Opening alias open-alias-englund-trap-rejected contains authority wording"):
      SourceContextCorpus.parseOpeningAlias(trapAlias).validatedDisplayBoundary

  test("opening alias schema rejects current-position or stat fields"):
    val json = Json.obj(
      "id" -> "open-alias-position-rewrite",
      "sourceId" -> "lichess-openings",
      "sourceName" -> "Catalan Opening: Open Defense",
      "canonicalName" -> "Catalan Opening: Open Defense",
      "openingFamily" -> "Catalan Opening",
      "openingVariation" -> "Open Defense",
      "displayName" -> "Open Catalan",
      "contextName" -> "Catalan with dxc4",
      "aliasKind" -> "display_alias",
      "sourceRefs" -> Json.arr("eco-e04-catalan-open"),
      "negativeBoundaries" -> Json.arr("alias_is_not_position_truth"),
      "positionKey" -> openingPositions.head.positionKey
    )
    interceptMessage[IllegalArgumentException]("Opening alias open-alias-position-rewrite uses unsupported fields: positionKey"):
      SourceContextCorpus.parseOpeningAlias(json)

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
    assertEquals(
      motifExamples.map(_.motifId).toSet,
      Set("loose_piece", "pin", "fork", "skewer", "overload", "trapped_piece", "mate_net", "perpetual_check")
    )
    motifExamples.foreach: row =>
      row.validatedSourceId(manifests)
      row.validatedMotifId
      row.validatedDisplayName
      row.validatedFen
      row.validatedDetectorCarrier
      row.validatedInvolvedSquares
      row.validatedSourceTags
      row.validatedAuthority
      row.validatedCurrentPositionBoundary
      row.validatedSourceRefs
      row.validatedNegativeBoundaries

    motifRejects.foreach: row =>
      row.validatedRejectReason
      assertEquals(row.expectation, "rejected")
      intercept[IllegalArgumentException]:
        row.asExampleCandidate.validatedContract

    val rejectMessages = Map(
      "motif-wrong-board-reject" -> "Motif example motif-wrong-board-reject detector carrier ref must match row id",
      "motif-mismatched-carrier-reject" -> "Motif example motif-mismatched-carrier-reject detector carrier must match motifId fork",
      "motif-mate-net-without-cert-reject" -> "Motif example motif-mate-net-without-cert-reject detector carrier must match motifId mate_net",
      "motif-perpetual-without-cert-reject" -> "Motif example motif-perpetual-without-cert-reject detector carrier must match motifId perpetual_check",
      "motif-truth-wording-reject" -> "Motif example motif-truth-wording-reject displayName contains forbidden truth wording"
    )
    rejectMessages.foreach: (id, expected) =>
      val row = motifRejects.find(_.id == id).getOrElse(fail(s"missing motif reject fixture $id"))
      interceptMessage[IllegalArgumentException](expected):
        row.asExampleCandidate.validatedContract

    val json = Json.obj(
      "id" -> "motif-tag-only",
      "sourceId" -> "lichess-puzzles",
      "motifId" -> "fork",
      "displayName" -> "Fork",
      "fen" -> "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
      "sourceTags" -> Json.arr("fork"),
      "authority" -> "motif_example",
      "currentPositionClaim" -> false,
      "sourceRefs" -> Json.arr("motif-example:motif-tag-only"),
      "negativeBoundaries" -> Json.arr("source_tag_without_detector")
    )

    interceptMessage[IllegalArgumentException]("Motif example motif-tag-only must bind an exact-board detector carrier"):
      SourceContextCorpus.parseMotifExample(json).validatedDetectorCarrier

  test("motif examples resolve declared carriers through current backend extractors"):
    motifExamples.foreach(assertCurrentBackendCarrier)

  test("motif label and source tags cannot leak current-position or Sxx truth"):
    val json = Json.obj(
      "id" -> "motif-label-leak",
      "sourceId" -> "lichess-puzzles",
      "motifId" -> "fork",
      "displayName" -> "Best forced fork",
      "fen" -> "4k3/8/8/8/8/3N4/8/R3K2r w - - 0 1",
      "detectorCarrier" -> Json.obj("ref" -> "motif-detector-carrier:motif-label-leak", "descriptorId" -> "fork", "anchor" -> "piece:d3", "carrierKind" -> "u_witness"),
      "involvedSquares" -> Json.arr("d3"),
      "sourceTags" -> Json.arr("S24", "current_position_truth"),
      "authority" -> "motif_detector",
      "currentPositionClaim" -> false,
      "sourceRefs" -> Json.arr("motif-example:motif-label-leak"),
      "negativeBoundaries" -> Json.arr("source_tag_is_example_only")
    )

    interceptMessage[IllegalArgumentException]("Motif example motif-label-leak sourceTags contain truth-bearing labels"):
      SourceContextCorpus.parseMotifExample(json).validatedSourceTags
    interceptMessage[IllegalArgumentException]("Motif example motif-label-leak displayName contains forbidden truth wording"):
      SourceContextCorpus.parseMotifExample(json).validatedDisplayName

  test("motif detector carrier must match motif identity"):
    val json = Json.obj(
      "id" -> "motif-wrong-carrier",
      "sourceId" -> "lichess-puzzles",
      "motifId" -> "fork",
      "displayName" -> "Fork",
      "fen" -> "4k3/8/8/8/8/3N4/8/R3K2r w - - 0 1",
      "detectorCarrier" -> Json.obj("ref" -> "motif-detector-carrier:motif-wrong-carrier", "descriptorId" -> "pin", "anchor" -> "piece:d3", "carrierKind" -> "u_witness"),
      "involvedSquares" -> Json.arr("d3"),
      "sourceTags" -> Json.arr("fork"),
      "authority" -> "motif_detector",
      "currentPositionClaim" -> false,
      "sourceRefs" -> Json.arr("motif-example:motif-wrong-carrier"),
      "negativeBoundaries" -> Json.arr("detector_required_for_current_board")
    )

    interceptMessage[IllegalArgumentException]("Motif example motif-wrong-carrier detector carrier must match motifId fork"):
      SourceContextCorpus.parseMotifExample(json).validatedDetectorCarrier

  test("motif schema rejects unknown fields and missing required context fields"):
    val valid = Json.obj(
      "id" -> "motif-schema-pin",
      "sourceId" -> "lichess-puzzles",
      "motifId" -> "pin",
      "displayName" -> "Pin",
      "fen" -> "4r2k/8/8/8/8/8/4B3/4K3 w - - 0 1",
      "detectorCarrier" -> Json.obj("ref" -> "motif-detector-carrier:motif-schema-pin", "descriptorId" -> "pin", "anchor" -> "ray:e8:south", "carrierKind" -> "u_witness"),
      "involvedSquares" -> Json.arr("e8", "e2", "e1"),
      "sourceTags" -> Json.arr("pin"),
      "authority" -> "motif_detector",
      "currentPositionClaim" -> false,
      "sourceRefs" -> Json.arr("motif-example:motif-schema-pin"),
      "negativeBoundaries" -> Json.arr("source_tag_is_example_only")
    )

    interceptMessage[IllegalArgumentException]("Motif example motif-schema-pin uses unsupported fields: currentPositionTruth"):
      SourceContextCorpus.parseMotifExample(valid.as[JsObject] ++ Json.obj("currentPositionTruth" -> true))
    interceptMessage[IllegalArgumentException]("Motif detector carrier unknown uses unsupported fields: currentPositionTruth"):
      SourceContextCorpus.parseMotifExample(
        valid.as[JsObject] ++ Json.obj(
          "detectorCarrier" -> Json.obj(
            "ref" -> "motif-detector-carrier:motif-schema-pin",
            "descriptorId" -> "pin",
            "anchor" -> "ray:e8:south",
            "carrierKind" -> "u_witness",
            "currentPositionTruth" -> true
          )
        )
      )
    intercept[IllegalArgumentException]:
      SourceContextCorpus.parseMotifExample(valid.as[JsObject] - "involvedSquares").validatedInvolvedSquares
    intercept[IllegalArgumentException]:
      SourceContextCorpus.parseMotifExample(valid.as[JsObject] - "negativeBoundaries").validatedNegativeBoundaries
    val extraMotifRef =
      valid.as[JsObject] ++ Json.obj(
        "id" -> "motif-extra-source-ref",
        "detectorCarrier" -> Json.obj("ref" -> "motif-detector-carrier:motif-extra-source-ref", "descriptorId" -> "pin", "anchor" -> "ray:e8:south", "carrierKind" -> "u_witness"),
        "sourceRefs" -> Json.arr("motif-example:motif-extra-source-ref", "motif-example:other-row")
      )
    interceptMessage[IllegalArgumentException]("Motif example motif-extra-source-ref sourceRefs must bind exactly motif-example:motif-extra-source-ref"):
      SourceContextCorpus.parseMotifExample(extraMotifRef).validatedSourceRefs

  test("certification-only motifs require certification carrier kind"):
    val mateWithoutCertification = Json.obj(
      "id" -> "motif-mate-net-no-cert",
      "sourceId" -> "lichess-puzzles",
      "motifId" -> "mate_net",
      "displayName" -> "Mate net",
      "fen" -> "6k1/6pp/5Q2/6R1/8/8/8/6K1 w - - 0 1",
      "detectorCarrier" -> Json.obj("ref" -> "motif-detector-carrier:motif-mate-net-no-cert", "descriptorId" -> "mate_net", "anchor" -> "board", "carrierKind" -> "u_witness"),
      "involvedSquares" -> Json.arr("g8", "f6", "g5"),
      "sourceTags" -> Json.arr("mate_net"),
      "authority" -> "motif_detector",
      "currentPositionClaim" -> false,
      "sourceRefs" -> Json.arr("motif-example:motif-mate-net-no-cert"),
      "negativeBoundaries" -> Json.arr("certification_required_for_current_board")
    )
    interceptMessage[IllegalArgumentException]("Motif example motif-mate-net-no-cert detector carrier must match motifId mate_net"):
      SourceContextCorpus.parseMotifExample(mateWithoutCertification).validatedDetectorCarrier

    val perpetualWithoutCertification =
      mateWithoutCertification.as[JsObject] ++ Json.obj(
        "id" -> "motif-perpetual-no-cert",
        "motifId" -> "perpetual_check",
        "displayName" -> "Perpetual check",
        "detectorCarrier" -> Json.obj("ref" -> "motif-detector-carrier:motif-perpetual-no-cert", "descriptorId" -> "perpetual_check", "anchor" -> "board", "carrierKind" -> "u_witness"),
        "sourceRefs" -> Json.arr("motif-example:motif-perpetual-no-cert")
      )
    interceptMessage[IllegalArgumentException]("Motif example motif-perpetual-no-cert detector carrier must match motifId perpetual_check"):
      SourceContextCorpus.parseMotifExample(perpetualWithoutCertification).validatedDetectorCarrier

  test("endgame study rows are reference contexts, not result verdicts"):
    endgameStudies.foreach: row =>
      row.validatedSourceId(manifests)
      row.validatedStudyId
      row.validatedDisplayName
      row.validatedNames
      row.validatedMaterialClass
      row.validatedSideToMove
      row.validatedPlacementRules
      row.validatedAllowedTransforms
      row.validatedCandidatePlans
      row.validatedContextBoundary
      row.validatedNegativeBoundaries

    val admitted = endgameStudies.map(_.studyId).toSet
    assert(
      Set(
        "lucena_rook_pawn",
        "philidor_rook_pawn",
        "vancura_rook_pawn",
        "basic_opposition_kpk",
        "distant_opposition_kpk",
        "wrong_rook_pawn_bishop",
        "rook_behind_passed_pawn"
      ).subsetOf(admitted),
      "endgame study fixtures must include the exact-board admitted study contexts"
    )
    assert(
      !admitted.exists(
        Set(
          "outside_passer",
          "fortress_pattern",
          "rook_on_seventh",
          "triangulation",
          "corresponding_squares",
          "shouldering",
          "breakthrough",
          "reserve_tempo"
        )
      ),
      "calculation/certification-heavy endgame labels must remain deferred"
    )

  test("Lucena label without rook-pawn-king placement constraints is rejected"):
    val json = Json.obj(
      "studyId" -> "lucena_missing_rules",
      "displayName" -> "Lucena Position",
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
      "displayName" -> "Philidor Position",
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
      "displayName" -> "Lucena Position",
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
      "displayName" -> "Lucena Position",
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
      "displayName" -> "Lucena Position",
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

  test("endgame study schema rejects source-truth and unsupported transform fields"):
    val valid = Json.obj(
      "studyId" -> "basic_opposition_kpk",
      "displayName" -> "Basic opposition",
      "names" -> Json.arr("Basic opposition"),
      "materialClass" -> "king_pawn_vs_king",
      "sideToMove" -> Json.arr("stronger", "defender"),
      "placementRules" -> Json.arr("single_pawn_endgame"),
      "relationRules" -> Json.arr("kings_in_basic_opposition"),
      "allowedTransforms" -> Json.arr("mirror_files"),
      "candidatePlans" -> Json.arr("opposition_context"),
      "sourceRefs" -> Json.arr("endgame-study-public"),
      "authority" -> "endgame_study_context",
      "outcomeClaim" -> "none",
      "negativeBoundaries" -> Json.arr("pattern_context_is_not_result_truth")
    )

    interceptMessage[IllegalArgumentException]("Endgame study basic_opposition_kpk uses unsupported fields: currentPositionTruth"):
      SourceContextCorpus.parseEndgameStudy(valid.as[JsObject] ++ Json.obj("currentPositionTruth" -> true))
    interceptMessage[IllegalArgumentException]("Endgame study basic_opposition_kpk uses unsupported allowedTransforms mirror_ranks"):
      SourceContextCorpus.parseEndgameStudy(valid.as[JsObject] ++ Json.obj("allowedTransforms" -> Json.arr("mirror_ranks"))).validatedAllowedTransforms
    interceptMessage[IllegalArgumentException]("Endgame study basic_opposition_kpk displayName contains forbidden truth wording"):
      SourceContextCorpus.parseEndgameStudy(valid.as[JsObject] ++ Json.obj("displayName" -> "Winning basic opposition")).validatedDisplayName
    interceptMessage[IllegalArgumentException]("Endgame study basic_opposition_kpk candidatePlans contain result claims: tablebase_oracle_path"):
      SourceContextCorpus.parseEndgameStudy(valid.as[JsObject] ++ Json.obj("candidatePlans" -> Json.arr("tablebase_oracle_path"))).validatedCandidatePlans
    interceptMessage[IllegalArgumentException]("Endgame study basic_opposition_kpk names contain forbidden truth wording: S23 tablebase truth"):
      SourceContextCorpus.parseEndgameStudy(valid.as[JsObject] ++ Json.obj("names" -> Json.arr("Basic opposition", "S23 tablebase truth"))).validatedNames
    interceptMessage[IllegalArgumentException]("Endgame study basic_opposition_kpk candidatePlans contain result claims: best_truth_s23_context"):
      SourceContextCorpus.parseEndgameStudy(valid.as[JsObject] ++ Json.obj("candidatePlans" -> Json.arr("best_truth_s23_context"))).validatedCandidatePlans
    interceptMessage[IllegalArgumentException]("Endgame study outside_passer requires future exact-board certification"):
      SourceContextCorpus.parseEndgameStudy(valid.as[JsObject] ++ Json.obj("studyId" -> "outside_passer", "displayName" -> "Outside passer", "names" -> Json.arr("Outside passer"))).validatedStudyId

  test("endgame study fixtures bind exact boards to declared applicability evidence"):
    endgameFixtures.foreach: row =>
      row.validatedStudyId(endgameStudies)
      row.validatedFen
      row.validatedMaterialClass(endgameStudies)
      row.validatedApplicability(endgameStudies)
      row.validatedContextBoundary
      row.validatedSourceRefs

    endgameRejects.foreach: row =>
      row.validatedStudyId(endgameStudies)
      row.validatedFen
      row.validatedRejectReason
      row.validatedApplicabilityRejected(endgameStudies)
      assertEquals(row.expectation, "rejected")

    assertEquals(endgameFixtures.size, 7)
    assertEquals(endgameRejects.size, 15)

    val wrongSide = endgameFixtures.head.copy(id = "study-lucena-wrong-side", sideToMove = "defender")
    interceptMessage[IllegalArgumentException]("Endgame fixture study-lucena-wrong-side sideToMove must match exact board"):
      wrongSide.validatedApplicability(endgameStudies)

  test("retrieval examples remain non-authoritative"):
    retrievalExamples.foreach: row =>
      row.validatedContract(manifests, retrievalExamples, motifCarrierRefs, endgameApplicabilityRefs, motifCarrierBindings, endgameApplicabilityBindings)
      row.validatedSourceId(manifests)
      row.validatedRetrievalId
      row.validatedSourceRef
      row.validatedFen
      row.validatedPositionKey
      row.validatedSideToMove
      row.validatedSimilarityKey(motifCarrierRefs, endgameApplicabilityRefs, motifCarrierBindings, endgameApplicabilityBindings)
      row.validatedSimilarityScore
      row.validatedSimilarityKind
      row.validatedMatchedFeatures
      row.validatedSourceQuality
      row.validatedCitationBoundary
      row.validatedAuthority
      row.validatedTags
      row.validatedNegativeBoundaries
      row.validatedCurrentPositionBoundary

    val json = Json.obj(
      "retrievalId" -> "retrieval-truth-leak",
      "sourceId" -> "lichess-puzzles",
      "sourceRef" -> "retrieval-example:truth-leak",
      "exampleKind" -> "puzzle_reference",
      "fen" -> "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
      "positionKey" -> "std:4k3/8/8/8/8/8/4N3/4K3 w - -",
      "sideToMove" -> "white",
      "similarityKey" -> Json.obj("materialClass" -> "minor_piece", "motifTags" -> Json.arr("fork")),
      "similarityScore" -> 0.84,
      "similarityKind" -> "motif_context",
      "matchedFeatures" -> Json.arr("motif:fork"),
      "sourceQuality" -> "manifest_backed",
      "snippetRole" -> "current_position_truth",
      "currentPositionClaim" -> true,
      "authority" -> "retrieval_example",
      "tags" -> Json.arr("fork"),
      "negativeBoundaries" -> Json.arr("retrieval_is_not_current_position_truth")
    )

    interceptMessage[IllegalArgumentException]("Retrieval example retrieval-truth-leak must remain non-authoritative"):
      SourceContextCorpus.parseRetrievalExample(json).validatedCurrentPositionBoundary

    assertEquals(retrievalExamples.size, 5)

  test("retrieval provenance, similarity, and tags are validated"):
    val emptyRef = Json.obj(
      "retrievalId" -> "retrieval-empty-ref",
      "sourceId" -> "example-index",
      "sourceRef" -> "",
      "exampleKind" -> "curated_reference",
      "fen" -> retrievalExamples.head.fen,
      "positionKey" -> retrievalExamples.head.positionKey,
      "sideToMove" -> retrievalExamples.head.sideToMove,
      "similarityKey" -> Json.obj("materialClass" -> "minor_piece"),
      "similarityScore" -> 0.5,
      "similarityKind" -> "mixed_feature_context",
      "matchedFeatures" -> Json.arr("material:minor_piece"),
      "sourceQuality" -> "local_curated",
      "snippetRole" -> "non_authoritative_context",
      "currentPositionClaim" -> false,
      "authority" -> "retrieval_example",
      "tags" -> Json.arr("example_only"),
      "negativeBoundaries" -> Json.arr("retrieval_is_not_current_position_truth")
    )
    interceptMessage[IllegalArgumentException]("Retrieval example retrieval-empty-ref must declare sourceRef"):
      SourceContextCorpus.parseRetrievalExample(emptyRef).validatedSourceRef

    val truthTag = Json.obj(
      "retrievalId" -> "retrieval-tag-leak",
      "sourceId" -> "example-index",
      "sourceRef" -> "retrieval-example:tag-leak",
      "exampleKind" -> "curated_reference",
      "fen" -> retrievalExamples.head.fen,
      "positionKey" -> retrievalExamples.head.positionKey,
      "sideToMove" -> retrievalExamples.head.sideToMove,
      "similarityKey" -> Json.obj("materialClass" -> "minor_piece"),
      "similarityScore" -> 0.72,
      "similarityKind" -> "mixed_feature_context",
      "matchedFeatures" -> Json.arr("material:minor_piece"),
      "sourceQuality" -> "local_curated",
      "snippetRole" -> "non_authoritative_context",
      "currentPositionClaim" -> false,
      "authority" -> "retrieval_example",
      "tags" -> Json.arr("example_only", "current_position_truth"),
      "negativeBoundaries" -> Json.arr("retrieval_is_not_current_position_truth")
    )
    interceptMessage[IllegalArgumentException]("Retrieval example retrieval-tag-leak tags contain truth-bearing labels"):
      SourceContextCorpus.parseRetrievalExample(truthTag).validatedTags

    val extraField = Json.obj(
      "retrievalId" -> "retrieval-extra-field",
      "sourceId" -> "example-index",
      "sourceRef" -> "retrieval-example:extra",
      "exampleKind" -> "curated_reference",
      "fen" -> retrievalExamples.head.fen,
      "positionKey" -> retrievalExamples.head.positionKey,
      "sideToMove" -> retrievalExamples.head.sideToMove,
      "similarityKey" -> Json.obj("materialClass" -> "minor_piece"),
      "similarityScore" -> 0.72,
      "similarityKind" -> "mixed_feature_context",
      "matchedFeatures" -> Json.arr("material:minor_piece"),
      "sourceQuality" -> "local_curated",
      "snippetRole" -> "non_authoritative_context",
      "currentPositionClaim" -> false,
      "authority" -> "retrieval_example",
      "tags" -> Json.arr("example_only"),
      "negativeBoundaries" -> Json.arr("retrieval_is_not_current_position_truth"),
      "claimText" -> "White is winning"
    )
    interceptMessage[IllegalArgumentException]("Retrieval example retrieval-extra-field uses unsupported fields: claimText"):
      SourceContextCorpus.parseRetrievalExample(extraField)

    val citationExtraField = Json.obj(
      "retrievalId" -> "retrieval-citation-extra-field",
      "sourceId" -> "example-index",
      "sourceRef" -> "retrieval-example:citation-extra",
      "exampleKind" -> "broadcast_game_reference",
      "fen" -> retrievalExamples.head.fen,
      "positionKey" -> retrievalExamples.head.positionKey,
      "sideToMove" -> retrievalExamples.head.sideToMove,
      "similarityKey" -> Json.obj("materialClass" -> "minor_piece", "phaseContext" -> Json.arr("endgame")),
      "similarityScore" -> 0.72,
      "similarityKind" -> "mixed_feature_context",
      "matchedFeatures" -> Json.arr("material:minor_piece", "phase:endgame"),
      "sourceQuality" -> "attribution_required",
      "gameMetadata" -> Json.obj("players" -> Json.arr("White Player", "Black Player"), "displaySafe" -> true),
      "licenseNotice" -> "Citation metadata only.",
      "attributionText" -> "Local curated retrieval fixture.",
      "snippetRole" -> "non_authoritative_context",
      "currentPositionClaim" -> false,
      "authority" -> "retrieval_example",
      "tags" -> Json.arr("citation_metadata", "example_only"),
      "negativeBoundaries" -> Json.arr("retrieval_non_authoritative")
    )
    interceptMessage[IllegalArgumentException]("Retrieval citation unknown uses unsupported fields: displaySafe"):
      SourceContextCorpus.parseRetrievalExample(citationExtraField)

    val sourceIdentityTruth = Json.obj(
      "retrievalId" -> "retrieval-best-forced-result-s21-current-position-proof",
      "sourceId" -> "example-index",
      "sourceRef" -> "retrieval-example:quiet-source",
      "exampleKind" -> "curated_reference",
      "fen" -> retrievalExamples.head.fen,
      "positionKey" -> retrievalExamples.head.positionKey,
      "sideToMove" -> retrievalExamples.head.sideToMove,
      "similarityKey" -> Json.obj("materialClass" -> "opening_full_material", "phaseContext" -> Json.arr("opening")),
      "similarityScore" -> 0.72,
      "similarityKind" -> "mixed_feature_context",
      "matchedFeatures" -> Json.arr("material:opening_full_material", "phase:opening"),
      "sourceQuality" -> "local_curated",
      "snippetRole" -> "non_authoritative_context",
      "currentPositionClaim" -> false,
      "authority" -> "retrieval_example",
      "tags" -> Json.arr("example_only"),
      "negativeBoundaries" -> Json.arr("retrieval_non_authoritative")
    )
    interceptMessage[IllegalArgumentException]("Retrieval example retrieval-best-forced-result-s21-current-position-proof source identity contains forbidden truth wording"):
      SourceContextCorpus.parseRetrievalExample(sourceIdentityTruth).validatedSourceRef

    val sourceRefTruth = sourceIdentityTruth ++ Json.obj(
      "retrievalId" -> "retrieval-quiet-source-identity",
      "sourceRef" -> "retrieval-example:best-forced-result-s21-current-position-proof"
    )
    interceptMessage[IllegalArgumentException]("Retrieval example retrieval-quiet-source-identity source identity contains forbidden truth wording"):
      SourceContextCorpus.parseRetrievalExample(sourceRefTruth).validatedSourceRef

    val similarityKeyExtraField = Json.obj(
      "retrievalId" -> "retrieval-similarity-key-extra-field",
      "sourceId" -> "example-index",
      "sourceRef" -> "retrieval-example:similarity-key-extra",
      "exampleKind" -> "curated_reference",
      "fen" -> retrievalExamples.head.fen,
      "positionKey" -> retrievalExamples.head.positionKey,
      "sideToMove" -> retrievalExamples.head.sideToMove,
      "similarityKey" -> Json.obj(
        "materialClass" -> "opening_full_material",
        "phaseContext" -> Json.arr("opening"),
        "contextNote" -> "quiet_reference"
      ),
      "similarityScore" -> 0.72,
      "similarityKind" -> "mixed_feature_context",
      "matchedFeatures" -> Json.arr("material:opening_full_material", "phase:opening"),
      "sourceQuality" -> "local_curated",
      "snippetRole" -> "non_authoritative_context",
      "currentPositionClaim" -> false,
      "authority" -> "retrieval_example",
      "tags" -> Json.arr("example_only"),
      "negativeBoundaries" -> Json.arr("retrieval_non_authoritative")
    )
    interceptMessage[IllegalArgumentException]("Retrieval example retrieval-similarity-key-extra-field similarityKey uses unsupported fields: contextNote"):
      SourceContextCorpus.parseRetrievalExample(similarityKeyExtraField).validatedSimilarityKey()

    val wrongMaterialClass = Json.obj(
      "retrievalId" -> "retrieval-wrong-material-class",
      "sourceId" -> "example-index",
      "sourceRef" -> "retrieval-example:wrong-material-class",
      "exampleKind" -> "curated_reference",
      "fen" -> "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
      "positionKey" -> "std:4k3/8/8/8/8/8/4N3/4K3 w - -",
      "sideToMove" -> "white",
      "similarityKey" -> Json.obj("materialClass" -> "rook_pawn_vs_rook", "phaseContext" -> Json.arr("endgame")),
      "similarityScore" -> 0.72,
      "similarityKind" -> "mixed_feature_context",
      "matchedFeatures" -> Json.arr("material:rook_pawn_vs_rook", "phase:endgame"),
      "sourceQuality" -> "local_curated",
      "snippetRole" -> "non_authoritative_context",
      "currentPositionClaim" -> false,
      "authority" -> "retrieval_example",
      "tags" -> Json.arr("example_only"),
      "negativeBoundaries" -> Json.arr("retrieval_non_authoritative")
    )
    interceptMessage[IllegalArgumentException]("Retrieval example retrieval-wrong-material-class materialClass must match exact example FEN"):
      SourceContextCorpus.parseRetrievalExample(wrongMaterialClass).validatedSimilarityKey()

    val wrongOpeningFamily = Json.obj(
      "retrievalId" -> "retrieval-wrong-opening-family",
      "sourceId" -> "example-index",
      "sourceRef" -> "retrieval-example:wrong-opening-family",
      "exampleKind" -> "curated_reference",
      "fen" -> retrievalExamples.head.fen,
      "positionKey" -> retrievalExamples.head.positionKey,
      "sideToMove" -> retrievalExamples.head.sideToMove,
      "similarityKey" -> Json.obj("openingFamily" -> "Sicilian Defense", "pawnStructure" -> Json.arr("queenside_tension")),
      "similarityScore" -> 0.72,
      "similarityKind" -> "opening_context",
      "matchedFeatures" -> Json.arr("opening:Sicilian_Defense", "structure:queenside_tension"),
      "sourceQuality" -> "local_curated",
      "snippetRole" -> "non_authoritative_context",
      "currentPositionClaim" -> false,
      "authority" -> "retrieval_example",
      "tags" -> Json.arr("example_only"),
      "negativeBoundaries" -> Json.arr("retrieval_non_authoritative")
    )
    interceptMessage[IllegalArgumentException]("Retrieval example retrieval-wrong-opening-family opening similarity family must match exact example FEN"):
      SourceContextCorpus.parseRetrievalExample(wrongOpeningFamily).validatedSimilarityKey()

    val codedResultFeature = Json.obj(
      "retrievalId" -> "retrieval-coded-result-feature",
      "sourceId" -> "example-index",
      "sourceRef" -> "retrieval-example:coded-result-feature",
      "exampleKind" -> "curated_reference",
      "fen" -> "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
      "positionKey" -> "std:4k3/8/8/8/8/8/4N3/4K3 w - -",
      "sideToMove" -> "white",
      "similarityKey" -> Json.obj("materialClass" -> "minor_piece", "phaseContext" -> Json.arr("endgame")),
      "similarityScore" -> 0.72,
      "similarityKind" -> "mixed_feature_context",
      "matchedFeatures" -> Json.arr("score:1-0"),
      "sourceQuality" -> "local_curated",
      "snippetRole" -> "non_authoritative_context",
      "currentPositionClaim" -> false,
      "authority" -> "retrieval_example",
      "tags" -> Json.arr("example_only"),
      "negativeBoundaries" -> Json.arr("retrieval_non_authoritative")
    )
    interceptMessage[IllegalArgumentException]("Retrieval example retrieval-coded-result-feature matchedFeatures contain forbidden truth wording"):
      SourceContextCorpus.parseRetrievalExample(codedResultFeature).validatedMatchedFeatures

    val currentPositionFeature = Json.obj(
      "retrievalId" -> "retrieval-current-position-feature",
      "sourceId" -> "example-index",
      "sourceRef" -> "retrieval-example:current-position-feature",
      "exampleKind" -> "curated_reference",
      "fen" -> "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1",
      "positionKey" -> "std:4k3/8/8/8/8/8/4N3/4K3 w - -",
      "sideToMove" -> "white",
      "similarityKey" -> Json.obj("materialClass" -> "minor_piece", "phaseContext" -> Json.arr("endgame")),
      "similarityScore" -> 0.72,
      "similarityKind" -> "mixed_feature_context",
      "matchedFeatures" -> Json.arr("current-position:reference"),
      "sourceQuality" -> "local_curated",
      "snippetRole" -> "non_authoritative_context",
      "currentPositionClaim" -> false,
      "authority" -> "retrieval_example",
      "tags" -> Json.arr("example_only"),
      "negativeBoundaries" -> Json.arr("retrieval_non_authoritative")
    )
    interceptMessage[IllegalArgumentException]("Retrieval example retrieval-current-position-feature matchedFeatures contain forbidden truth wording"):
      SourceContextCorpus.parseRetrievalExample(currentPositionFeature).validatedMatchedFeatures

  test("retrieval reject fixtures freeze fail-closed boundaries"):
    retrievalRejects.foreach: row =>
      row.validatedRejectReason
      try
        row.asExampleCandidate.validatedContract(manifests, retrievalExamples, motifCarrierRefs, endgameApplicabilityRefs, motifCarrierBindings, endgameApplicabilityBindings)
        fail(s"${row.retrievalId} should have failed validation")
      catch
        case _: IllegalArgumentException => ()

    assertEquals(retrievalRejects.size, 32)

  private def assertCurrentBackendCarrier(row: SourceContextCorpus.MotifExample): Unit =
    val carrier = row.validatedDetectorCarrier
    val fen = row.validatedFen
    carrier.carrierKind match
      case "u_witness" =>
        val descriptorId = WitnessDescriptorId(carrier.descriptorId)
        assert(
          UScopeContract.isActivePrimaryDescriptorId(descriptorId),
          s"${row.id}: ${carrier.descriptorId} must remain an active U-primary descriptor"
        )
        val extraction = UWitnessExtractor.fromFen(fen).fold(message => fail(s"${row.id}: $message"), identity)
        assert(
          extraction.witnesses.forDescriptorId(descriptorId).nonEmpty,
          s"${row.id}: current board must emit U witness ${carrier.descriptorId}"
        )
      case "root_atom" =>
        assert(
          RootAtomRegistry.schema(carrier.descriptorId).nonEmpty,
          s"${row.id}: ${carrier.descriptorId} must remain an active root schema"
        )
        val rootState = RootExtractor.fromFen(fen).fold(message => fail(s"${row.id}: $message"), identity)
        assert(
          rootState.activeIndicesForSchema(carrier.descriptorId).nonEmpty,
          s"${row.id}: current board must emit root atom ${carrier.descriptorId}"
        )
      case "certified_line" =>
        val familyId = CertificationFamilyId(carrier.descriptorId)
        assert(
          CertificationScopeContract.isActiveFamilyId(familyId),
          s"${row.id}: ${carrier.descriptorId} must remain an active certification family"
        )
        val current = StrategicObjectExtractor.fromFen(fen).fold(message => fail(s"${row.id}: $message"), identity)
        val extraction = CertificationExtractor
          .fromObjectExtractionFailClosed(current, CertificationEvidenceBundle.empty)
          .fold(message => fail(s"${row.id}: $message"), identity)
        assert(
          extraction.claims.forFamilyId(familyId).exists(_.verdict != CertificationVerdict.Rejected),
          s"${row.id}: current board must emit a non-rejected certification candidate for ${carrier.descriptorId}"
        )
      case other =>
        fail(s"${row.id}: unsupported carrier kind $other")
