package lila.commentary.source.opening

import play.api.libs.json.{ Json, JsObject }

class OpeningSourceToolingTest extends munit.FunSuite:

  private val manifests = OpeningSourceCorpus.loadManifests()
  private val lines = OpeningSourceCorpus.loadLines()
  private val positions = OpeningSourceCorpus.loadPositions()
  private val moveStats = OpeningSourceCorpus.loadMoveStats()
  private val aliases = OpeningSourceCorpus.loadAliases()
  private val rejects = OpeningSourceCorpus.loadRejectFixtures()

  test("opening storage boundary is frozen by current worktree docs and ignore rules"):
    val storageDoc = OpeningSourceCorpus.readWorkspaceFile("modules/commentary/docs/legacy-pre-semantic-reset/OpeningSourceStorage.md")
    val gitignore = OpeningSourceCorpus.readWorkspaceFile(".gitignore")

    assert(
      storageDoc.contains("Raw source data and ECO-wide generated aggregates are local-only material."),
      "storage doc must freeze local-only bulk output"
    )
    assert(
      storageDoc.contains("Opening move candidates are statistics or references only."),
      "storage doc must freeze candidate authority"
    )
    assert(
      storageDoc.contains("display/context names live in separate alias rows"),
      "storage doc must separate taxonomy names from aliases"
    )
    assert(
      storageDoc.contains("The 2013-01 Lichess game aggregate is pipeline smoke data only."),
      "storage doc must classify the 2013 aggregate as smoke data"
    )
    assert(
      storageDoc.contains("Alias Catalog Work Order"),
      "storage doc must define the alias catalog work order"
    )
    assert(
      storageDoc.contains("Do not automatically"),
      "storage doc must block automatic alias expansion"
    )
    assert(
      storageDoc.contains("accepted hand-reviewed alias fixtures"),
      "storage doc must keep repo alias fixtures separate from local candidate lists"
    )
    assert(
      storageDoc.contains("this document constrains only rows whose `sourceFamily`"),
      "storage doc must distinguish opening rows from the shared source manifest fixture"
    )
    assert(storageDoc.contains("OpeningIndexBuilder"), "storage doc must name the offline builder boundary")
    assert(gitignore.contains("/tmp/commentary-opening/"), "gitignore must block opening tmp output")
    assert(gitignore.contains("/modules/commentary/.local/opening/"), "gitignore must block opening local cache")
    assert(
      gitignore.contains("/modules/commentary/src/test/resources/commentary-corpus/generated-opening/"),
      "gitignore must block fixture-adjacent generated opening output"
    )

  test("opening source manifest validates license provenance and local-only storage policy"):
    val openingSources = manifests.filter(_.sourceFamily == "opening")
    assertEquals(
      openingSources.map(_.sourceId).toSet,
      Set(
        "lichess-openings",
        "lichess-games",
        "lichess-games-standard-rated-2026-03-all",
        "lichess-broadcast-master-reference"
      )
    )

    openingSources.foreach: manifest =>
      assertEquals(OpeningContextValidator.validateManifest(manifest), Right(manifest))
      assert(manifest.parserVersion.exists(_.startsWith("opening-index-builder")), "manifest must track parserVersion")
      assert(manifest.rawStoragePolicy.exists(Set("externalOnly", "localOnly").contains), "raw source storage must not be commit-allowed")
      assertEquals(manifest.generatedStoragePolicy, Some("localOnly"))

    val smoke = openingSources.find(_.sourceId == "lichess-games").getOrElse(fail("missing Lichess game source"))
    assertEquals(smoke.sourceUse, Some("pipeline_smoke"))
    assertEquals(smoke.aggregateUse, Some("pipeline_smoke"))
    assertEquals(OpeningContextValidator.validateTrendSource(smoke), Right(None))

    val recent = openingSources
      .find(_.sourceId == "lichess-games-standard-rated-2026-03-all")
      .getOrElse(fail("missing recent Lichess trend source"))
    assertEquals(recent.sourceType, "trend_stat")
    assertEquals(recent.sourceUse, Some("online_trend"))
    assertEquals(recent.aggregateUse, Some("online_trend_stat"))
    assertEquals(recent.sourceVersion, Some("2026-03"))
    assertEquals(recent.yearMonth, Some("2026-03"))
    assertEquals(recent.sourceYear, Some(2026))
    assertEquals(recent.sourceMonth, Some(3))
    assertEquals(recent.ratingBucket, Some("all"))
    assertEquals(recent.timeControlBucket, Some("all"))
    assertEquals(recent.variant, Some("standard"))
    assertEquals(recent.ratedFilter, Some("rated"))
    assertEquals(recent.ratedOnly, Some(true))
    assertEquals(recent.sampleSizeThreshold, Some(1000))
    assertEquals(recent.rawStoragePolicy, Some("localOnly"))
    assertEquals(recent.sourceChecksum, Some("d3adc2bcc58e85f4398ece2f7f8ea422c5ec5269a7d2921257bc31c5b914180f"))
    assertEquals(OpeningContextValidator.validateTrendSource(recent), Right(Some("online_trend_stat")))

    val master = openingSources
      .find(_.sourceId == "lichess-broadcast-master-reference")
      .getOrElse(fail("missing Lichess broadcast master reference source"))
    assertEquals(master.sourceType, "masterGameDb")
    assertEquals(master.sourceUse, Some("master_reference"))
    assertEquals(master.aggregateUse, Some("master_reference_stat"))
    assertEquals(master.licenseName, Some("CC-BY-SA-4.0"))
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
    assertEquals(master.generatedStoragePolicy, Some("localOnly"))
    assertEquals(master.sourceChecksum, Some("fixture-only"))
    assertEquals(OpeningContextValidator.validateMasterReferenceSource(master), Right(Some("master_reference_stat")))
    Vector("full_month", "partial_month", "streamed_sample").foreach: scope =>
      val scoped = master.copy(sourceId = s"master-reference-$scope", sourceScope = Some(scope))
      assertEquals(OpeningContextValidator.validateMasterReferenceSource(scoped), Right(Some("master_reference_stat")))

    val trendWithoutChecksum = smoke.copy(
      sourceId = "trend-without-checksum",
      sourceType = "trend_stat",
      sourceVersion = Some("2025-12"),
      sourceUse = Some("online_trend"),
      aggregateUse = Some("online_trend_stat"),
      sourceYear = Some(2025),
      sourceMonth = Some(12),
      yearMonth = Some("2025-12"),
      sampleSizeThreshold = Some(25),
      ratedFilter = Some("rated"),
      ratedOnly = Some(true),
      sourceChecksum = None
    )
    val trendError = OpeningContextValidator.validateTrendSource(trendWithoutChecksum).left.getOrElse("")
    assert(trendError.contains("sourceChecksum"), s"unexpected error: $trendError")

    val trendSmoke = recent.copy(sourceId = "trend-source-smoke-leak", aggregateUse = Some("pipeline_smoke"))
    val trendSmokeError = OpeningContextValidator.validateManifest(trendSmoke).left.getOrElse("")
    assert(trendSmokeError.contains("trend_stat sourceType must use aggregateUse online_trend_stat"), s"unexpected error: $trendSmokeError")

    val smokeAsTrend = smoke.copy(sourceId = "smoke-as-trend", aggregateUse = Some("online_trend_stat"))
    val smokeAsTrendError = OpeningContextValidator.validateManifest(smokeAsTrend).left.getOrElse("")
    assert(smokeAsTrendError.contains("aggregateGameDb sourceType must use aggregateUse pipeline_smoke"), s"unexpected error: $smokeAsTrendError")

    val onlineAsMaster = recent.copy(sourceId = "online-trend-as-master", sourceUse = Some("master_reference"))
    val onlineAsMasterError = OpeningContextValidator.validateManifest(onlineAsMaster).left.getOrElse("")
    assert(onlineAsMasterError.contains("master_reference sourceUse must use sourceType masterGameDb"), s"unexpected error: $onlineAsMasterError")

    val publicPgnAsMaster = recent.copy(sourceId = "public-pgn-as-master", sourceType = "publicPgn", sourceUse = Some("master_reference"))
    val publicPgnAsMasterError = OpeningContextValidator.validateManifest(publicPgnAsMaster).left.getOrElse("")
    assert(publicPgnAsMasterError.contains("master_reference sourceUse must use sourceType masterGameDb"), s"unexpected error: $publicPgnAsMasterError")

    val masterWithoutNotice = master.copy(sourceId = "master-without-license-notice", licenseNotice = None)
    val missingNoticeError = OpeningContextValidator.validateMasterReferenceSource(masterWithoutNotice).left.getOrElse("")
    assert(missingNoticeError.contains("licenseNotice"), s"unexpected error: $missingNoticeError")

    val masterWithoutAttribution = master.copy(sourceId = "master-without-attribution", attributionRequired = false)
    val missingAttributionError = OpeningContextValidator.validateMasterReferenceSource(masterWithoutAttribution).left.getOrElse("")
    assert(missingAttributionError.contains("must require attribution"), s"unexpected error: $missingAttributionError")

    val masterWithoutShareAlike = master.copy(sourceId = "master-without-share-alike", shareAlikeRequired = None)
    val missingShareAlikeError = OpeningContextValidator.validateMasterReferenceSource(masterWithoutShareAlike).left.getOrElse("")
    assert(missingShareAlikeError.contains("shareAlike"), s"unexpected error: $missingShareAlikeError")

    val masterWithoutAttributionText = master.copy(sourceId = "master-without-attribution-text", attributionText = None)
    val missingAttributionTextError = OpeningContextValidator.validateMasterReferenceSource(masterWithoutAttributionText).left.getOrElse("")
    assert(missingAttributionTextError.contains("attributionText"), s"unexpected error: $missingAttributionTextError")

    val masterWithoutEnvironment = master.copy(sourceId = "master-without-play-environment", playEnvironment = None)
    val missingEnvironmentError = OpeningContextValidator.validateMasterReferenceSource(masterWithoutEnvironment).left.getOrElse("")
    assert(missingEnvironmentError.contains("playEnvironment"), s"unexpected error: $missingEnvironmentError")

    val mixedWithoutScope = master.copy(sourceId = "master-mixed-without-scope", playEnvironment = Some("mixed"), sourceScope = Some("recent_broadcast"))
    val mixedError = OpeningContextValidator.validateMasterReferenceSource(mixedWithoutScope).left.getOrElse("")
    assert(mixedError.contains("mixed scope"), s"unexpected error: $mixedError")

    val masterWithoutPlayerPolicy = master.copy(
      sourceId = "master-without-player-policy",
      playerLevel = None,
      minElo = None,
      titlePolicy = None
    )
    val missingPlayerPolicyError = OpeningContextValidator.validateMasterReferenceSource(masterWithoutPlayerPolicy).left.getOrElse("")
    assert(missingPlayerPolicyError.contains("playerLevel"), s"unexpected error: $missingPlayerPolicyError")

    val masterWithoutTimeControl = master.copy(sourceId = "master-without-time-control", timeControlScope = None)
    val missingTimeControlError = OpeningContextValidator.validateMasterReferenceSource(masterWithoutTimeControl).left.getOrElse("")
    assert(missingTimeControlError.contains("timeControlScope"), s"unexpected error: $missingTimeControlError")

    val fastMaster = master.copy(sourceId = "master-fast-only", timeControlScope = Some("fast"))
    val fastMasterError = OpeningContextValidator.validateMasterReferenceSource(fastMaster).left.getOrElse("")
    assert(fastMasterError.contains("classical_rapid"), s"unexpected error: $fastMasterError")

    val masterWithoutDedupe = master.copy(sourceId = "master-without-dedupe", dedupePolicy = None)
    val missingDedupeError = OpeningContextValidator.validateMasterReferenceSource(masterWithoutDedupe).left.getOrElse("")
    assert(missingDedupeError.contains("dedupePolicy"), s"unexpected error: $missingDedupeError")

    val masterWithoutAnnotation = master.copy(sourceId = "master-without-annotation", annotationPolicy = None)
    val missingAnnotationError = OpeningContextValidator.validateMasterReferenceSource(masterWithoutAnnotation).left.getOrElse("")
    assert(missingAnnotationError.contains("annotationPolicy"), s"unexpected error: $missingAnnotationError")

    val masterWithoutThreshold = master.copy(sourceId = "master-without-threshold", perPositionSampleSizeThreshold = None)
    val missingThresholdError = OpeningContextValidator.validateMasterReferenceSource(masterWithoutThreshold).left.getOrElse("")
    assert(missingThresholdError.contains("perPositionSampleSizeThreshold"), s"unexpected error: $missingThresholdError")

    val masterWithoutChecksum = master.copy(sourceId = "master-without-checksum", sourceChecksum = None)
    val missingChecksumError = OpeningContextValidator.validateMasterReferenceSource(masterWithoutChecksum).left.getOrElse("")
    assert(missingChecksumError.contains("sourceChecksum"), s"unexpected error: $missingChecksumError")

    val masterWithoutParser = master.copy(sourceId = "master-without-parser", parserVersion = None)
    val missingParserError = OpeningContextValidator.validateMasterReferenceSource(masterWithoutParser).left.getOrElse("")
    assert(missingParserError.contains("parserVersion"), s"unexpected error: $missingParserError")

    val masterWithoutSourceUrl = master.copy(sourceId = "master-without-source-url", sourceUrl = None)
    val missingSourceUrlError = OpeningContextValidator.validateManifest(masterWithoutSourceUrl).left.getOrElse("")
    assert(missingSourceUrlError.contains("sourceUrl"), s"unexpected error: $missingSourceUrlError")

    val masterWithoutLicenseUrl = master.copy(sourceId = "master-without-license-url", licenseUrl = None)
    val missingLicenseUrlError = OpeningContextValidator.validateManifest(masterWithoutLicenseUrl).left.getOrElse("")
    assert(missingLicenseUrlError.contains("licenseUrl"), s"unexpected error: $missingLicenseUrlError")

    val masterWithoutLicenseCheckDate = master.copy(sourceId = "master-without-license-check-date", licenseVerifiedAt = None)
    val missingLicenseCheckError = OpeningContextValidator.validateManifest(masterWithoutLicenseCheckDate).left.getOrElse("")
    assert(missingLicenseCheckError.contains("licenseVerifiedAt"), s"unexpected error: $missingLicenseCheckError")

    val masterStat = moveStats.head.copy(sourceId = master.sourceId)
    assertEquals(OpeningContextValidator.validateMoveStat(masterStat, positions, openingSources), Right(masterStat))
    val masterCandidate = OpeningCandidate.fromMoveStat(masterStat).fold(message => fail(message), identity)
    assertEquals(masterCandidate.candidateRole, "statistical_reference")
    assertEquals(masterCandidate.currentPositionTruth, false)

    val missingLicense = reject("missingLicenseProvenance").payload
    val error = OpeningContextValidator.validateManifest(OpeningSourceManifest.fromJson(missingLicense)).left.getOrElse("")
    assert(error.contains("missing licenseName"), s"unexpected error: $error")

  test("opening line parser replays legal UCI move order and rejects illegal lines"):
    val catalan = lines.find(_.lineId == "eco-e04-catalan-open").getOrElse(fail("missing Catalan fixture"))
    val replay = OpeningLineReplay.replay(catalan.moveOrder).fold(message => fail(message), identity)

    assertEquals(replay.finalPly, 8)
    assertEquals(replay.finalPositionKey.value, "std:rnbqkb1r/ppp2ppp/4pn2/8/2pP4/6P1/PP2PPBP/RNBQK1NR w KQkq -")
    assertEquals(OpeningContextValidator.validateLine(catalan, manifests), Right(catalan))

    val illegalLine = OpeningLine.fromJson(reject("illegalMoveOrder").payload)
    val error = OpeningContextValidator.validateLine(illegalLine, manifests).left.getOrElse("")
    assert(error.contains("illegal move e2e5"), s"unexpected error: $error")

  test("opening position key normalization matches exact FEN and move-order replay"):
    val catalanPosition = positions.find(_.positionId == "open-pos-catalan-open-tabia").getOrElse(fail("missing position fixture"))
    val normalized = OpeningPositionKey.fromFen(catalanPosition.fen).fold(message => fail(message), identity)

    assertEquals(normalized, catalanPosition.positionKey)
    assertEquals(OpeningContextValidator.validatePosition(catalanPosition, lines, manifests), Right(catalanPosition))

    val nameOnly = OpeningPosition.fromJson(reject("missingPositionKey").payload)
    val error = OpeningContextValidator.validatePosition(nameOnly, lines, manifests).left.getOrElse("")
    assert(error.contains("must declare exact positionKey"), s"unexpected error: $error")

  test("opening index is transposition-safe and downgrades ambiguous transpositions"):
    val index = OpeningIndexBuilder.build(lines, positions, moveStats, manifests).fold(message => fail(message), identity)
    val key = OpeningPositionKey("std:rnbqkb1r/ppp2ppp/4pn2/8/2pP4/6P1/PP2PPBP/RNBQK1NR w KQkq -")

    assertEquals(
      index.positionsByKey(key).map(_.positionId),
      Vector("open-pos-catalan-open-tabia"),
      "only canonical indexed position should be indexed for the key"
    )
    assert(
      index.downgradedPositions.exists(_.positionId == "open-pos-transposed-downgraded"),
      "ambiguous transposition fixture should be downgraded"
    )
    assertEquals(index.moveStatsByKey(key).map(_.move), Vector("d1a4", "g1f3", "d1a4", "g1f3"))

  test("opening aliases are display context rows that preserve taxonomy identity"):
    aliases.foreach: alias =>
      assertEquals(OpeningContextValidator.validateAlias(alias, lines, manifests), Right(alias))
      assert(
        !alias.displayName.contains("Catalan Opening: Open Defense") || alias.displayName == alias.canonicalName,
        s"alias ${alias.aliasId} must not accidentally mirror taxonomy as display copy"
      )

    assert(
      aliases.exists(alias => alias.displayName == "Open Catalan" && alias.contextName == "Catalan with ...dxc4"),
      "missing Catalan alias fixture"
    )
    assert(
      aliases.exists(alias => alias.displayName == "QGD Exchange" && alias.contextName == "Carlsbad context"),
      "missing QGD alias fixture"
    )

    val truthAlias = OpeningAlias.fromJson(reject("aliasTruthLeak").payload)
    val truthError = OpeningContextValidator.validateAlias(truthAlias, lines, manifests).left.getOrElse("")
    assert(truthError.contains("contains authority wording"), s"unexpected error: $truthError")

    val unknownAlias = OpeningAlias.fromJson(reject("aliasUnknownTaxonomy").payload)
    val unknownError = OpeningContextValidator.validateAlias(unknownAlias, lines, manifests).left.getOrElse("")
    assert(unknownError.contains("references unknown taxonomy row"), s"unexpected error: $unknownError")

    val extraFieldError = intercept[IllegalArgumentException]:
      OpeningAlias.fromJson(reject("aliasExtraField").payload)
    assert(extraFieldError.getMessage.contains("uses unsupported fields: positionKey"))

  test("open games aliases are curated by exact taxonomy row"):
    val byFamily = aliases.groupBy(_.openingFamily).view.mapValues(_.size).toMap

    assertEquals(byFamily.getOrElse("Ruy Lopez", 0), 6)
    assertEquals(byFamily.getOrElse("Italian Game", 0), 6)
    assertEquals(byFamily.getOrElse("Petrov's Defense", 0), 1)

    assert(
      aliases.exists(alias =>
        alias.sourceRefs == Vector("eco-c84-ruy-lopez-closed") &&
          alias.displayName == "Closed Spanish" &&
          alias.contextName == "Closed Ruy Lopez"
      ),
      "Closed Spanish must be attached only to a taxonomy row whose variation supports Closed"
    )
    assert(
      aliases.exists(alias => alias.sourceRefs == Vector("eco-c65-ruy-lopez-berlin-defense") && alias.displayName == "Berlin Defense"),
      "missing Berlin Ruy Lopez alias"
    )
    assert(
      aliases.exists(alias => alias.sourceRefs == Vector("eco-c80-ruy-lopez-open") && alias.displayName == "Open Spanish"),
      "missing Open Spanish alias"
    )
    assert(
      aliases.exists(alias => alias.sourceRefs == Vector("eco-c50-italian-game-giuoco-piano") && alias.displayName == "Giuoco Piano"),
      "missing Giuoco Piano alias"
    )
    assert(
      aliases.exists(alias => alias.sourceRefs == Vector("eco-c51-italian-game-evans-gambit") && alias.displayName == "Evans Gambit"),
      "missing Evans Gambit alias"
    )
    assert(
      aliases.exists(alias => alias.sourceRefs == Vector("eco-c55-italian-game-two-knights-defense") && alias.displayName == "Two Knights Italian"),
      "missing Two Knights Italian alias"
    )

    assert(!byFamily.contains("Scotch Game"), "Scotch Game family name is sufficient; do not add a decorative alias")
    assert(!byFamily.contains("Four Knights Game"), "Four Knights family name is sufficient; do not add a decorative alias")
    assert(!byFamily.contains("Philidor Defense"), "Philidor family name is sufficient; do not add a decorative alias")

    val unsupportedClosed = OpeningAlias.fromJson(reject("aliasClosedSpanishUnsupported").payload)
    val closedError = OpeningContextValidator.validateAlias(unsupportedClosed, lines, manifests).left.getOrElse("")
    assert(closedError.contains("requires a Closed Spanish taxonomy variation"), s"unexpected error: $closedError")

    val ambiguousOpenGame = reject("aliasAmbiguousOpenGame")
    assertEquals(ambiguousOpenGame.rowType, "aliasCandidate")
    assert((ambiguousOpenGame.payload \ "reason").as[String].contains("ambiguous_group_label"))

  test("sicilian aliases are curated by exact subfamily taxonomy row"):
    val sicilianAliases = aliases.filter(_.openingFamily == "Sicilian Defense")
    assertEquals(sicilianAliases.size, 16)

    def has(ref: String, display: String, context: String, kind: String): Unit =
      assert(
        sicilianAliases.exists(alias =>
          alias.sourceRefs == Vector(ref) &&
            alias.displayName == display &&
            alias.contextName == context &&
            alias.aliasKind == kind
        ),
        s"missing Sicilian alias $display for $ref"
      )

    has("eco-b20-sicilian-defense", "Sicilian Defense", "1.e4 c5 Sicilian", "context_alias")
    has("eco-b32-sicilian-defense-open", "Open Sicilian", "Open Sicilian", "context_alias")
    has("eco-b90-sicilian-najdorf", "Najdorf Sicilian", "Najdorf Sicilian", "display_alias")
    has("eco-b70-sicilian-defense-dragon-variation", "Dragon Sicilian", "Sicilian Dragon", "display_alias")
    has("eco-b32-sicilian-defense-accelerated-dragon", "Accelerated Dragon", "Accelerated Dragon Sicilian", "display_alias")
    has("eco-b80-sicilian-defense-scheveningen-variation", "Scheveningen Sicilian", "Scheveningen Sicilian", "display_alias")
    has("eco-b56-sicilian-defense-classical-variation", "Classical Sicilian", "Classical Sicilian", "display_alias")
    has(
      "eco-b33-sicilian-defense-lasker-pelikan-variation-sveshnikov-variation",
      "Sveshnikov Sicilian",
      "Lasker-Pelikan Sveshnikov",
      "display_alias"
    )
    has("eco-b44-sicilian-defense-taimanov-variation", "Taimanov Sicilian", "Taimanov Sicilian", "display_alias")
    has("eco-b41-sicilian-defense-kan-variation", "Kan Sicilian", "Kan Sicilian", "display_alias")
    has("eco-b30-sicilian-defense-nyezhmetdinov-rossolimo-attack", "Rossolimo Sicilian", "Rossolimo anti-Sicilian", "display_alias")
    has("eco-b22-sicilian-defense-alapin-variation", "Alapin Sicilian", "Alapin anti-Sicilian", "display_alias")
    has("eco-b23-sicilian-defense-closed", "Closed Sicilian", "Closed Sicilian", "context_alias")
    has("eco-b23-sicilian-defense-grand-prix-attack", "Grand Prix Sicilian", "Grand Prix setup", "display_alias")
    has("eco-b21-sicilian-defense-smith-morra-gambit", "Smith-Morra Gambit", "Smith-Morra Sicilian", "display_alias")
    has("eco-b51-sicilian-defense-moscow-variation", "Moscow Sicilian", "Moscow anti-Sicilian", "display_alias")

    sicilianAliases.foreach: alias =>
      val text = s"${alias.displayName} ${alias.contextName}".toLowerCase
      assert(!text.contains("attack"), s"Sicilian alias ${alias.aliasId} must avoid attack wording")
      assert(!text.contains("counterplay"), s"Sicilian alias ${alias.aliasId} must avoid counterplay wording")

    val openOnNajdorf = OpeningAlias(
      aliasId = "open-alias-open-sicilian-unsupported",
      sourceId = "lichess-openings",
      sourceName = "Sicilian Defense: Najdorf Variation",
      canonicalName = "Sicilian Defense: Najdorf Variation",
      openingFamily = "Sicilian Defense",
      openingVariation = "Najdorf Variation",
      displayName = "Open Sicilian",
      contextName = "Open Sicilian",
      aliasKind = "context_alias",
      sourceRefs = Vector("eco-b90-sicilian-najdorf"),
      negativeBoundaries = Vector("alias_requires_matching_variation")
    )
    val unsupportedError = OpeningContextValidator.validateAlias(openOnNajdorf, lines, manifests).left.getOrElse("")
    assert(unsupportedError.contains("requires Sicilian variation containing Open"), s"unexpected error: $unsupportedError")

    val ambiguousClassical = reject("aliasAmbiguousClassicalSicilian")
    assertEquals(ambiguousClassical.rowType, "aliasCandidate")
    assert((ambiguousClassical.payload \ "reason").as[String].contains("ambiguous_internal_name"))

  test("semi-open defense aliases are curated by exact variation taxonomy row"):
    val byFamily = aliases.groupBy(_.openingFamily).view.mapValues(_.size).toMap

    assertEquals(byFamily.getOrElse("French Defense", 0), 8)
    assertEquals(byFamily.getOrElse("Caro-Kann Defense", 0), 5)
    assertEquals(byFamily.getOrElse("Scandinavian Defense", 0), 6)
    assertEquals(byFamily.getOrElse("Alekhine Defense", 0), 4)

    def has(family: String, ref: String, display: String, context: String, kind: String): Unit =
      assert(
        aliases.exists(alias =>
          alias.openingFamily == family &&
            alias.sourceRefs == Vector(ref) &&
            alias.displayName == display &&
            alias.contextName == context &&
            alias.aliasKind == kind
        ),
        s"missing $family alias $display for $ref"
      )

    has("French Defense", "eco-c02-french-defense-advance-variation", "French Advance", "French Advance Variation", "display_alias")
    has("French Defense", "eco-c03-french-defense-tarrasch-variation", "French Tarrasch", "French Tarrasch Variation", "display_alias")
    has("French Defense", "eco-c15-french-defense-winawer-variation", "French Winawer", "French Winawer Variation", "display_alias")
    has("French Defense", "eco-c11-french-defense-classical-variation", "French Classical", "French Classical Variation", "display_alias")
    has("French Defense", "eco-c01-french-defense-exchange-variation", "French Exchange", "French Exchange Variation", "display_alias")

    has("Caro-Kann Defense", "eco-b12-caro-kann-defense-advance-variation", "Caro-Kann Advance", "Caro-Kann Advance Variation", "display_alias")
    has("Caro-Kann Defense", "eco-b13-caro-kann-defense-exchange-variation", "Caro-Kann Exchange", "Caro-Kann Exchange Variation", "display_alias")
    has("Caro-Kann Defense", "eco-b13-caro-kann-defense-panov-attack", "Panov Caro-Kann", "Panov Caro-Kann", "display_alias")
    has("Caro-Kann Defense", "eco-b18-caro-kann-defense-classical-variation", "Classical Caro-Kann", "Caro-Kann Classical Variation", "display_alias")

    has("Scandinavian Defense", "eco-b01-scandinavian-defense-modern-variation", "Modern Scandinavian", "Scandinavian Modern Variation", "display_alias")
    has("Scandinavian Defense", "eco-b01-scandinavian-defense-portuguese-gambit", "Portuguese Scandinavian", "Portuguese Gambit", "display_alias")
    has("Scandinavian Defense", "eco-b01-scandinavian-defense-mieses-kotroc-variation", "Mieses-Kotroc Scandinavian", "Mieses-Kotroc Variation", "display_alias")
    has("Scandinavian Defense", "eco-b01-scandinavian-defense-icelandic-palme-gambit", "Icelandic-Palme Gambit", "Scandinavian Icelandic-Palme", "display_alias")

    has("Alekhine Defense", "eco-b03-alekhine-defense-exchange-variation", "Alekhine Exchange", "Alekhine Exchange Variation", "display_alias")
    has("Alekhine Defense", "eco-b03-alekhine-defense-four-pawns-attack", "Alekhine Four Pawns", "Alekhine Four Pawns", "display_alias")
    has("Alekhine Defense", "eco-b04-alekhine-defense-modern-variation", "Alekhine Modern", "Alekhine Modern Variation", "display_alias")

    val targetAliases =
      aliases.filter(alias =>
        Set("French Defense", "Caro-Kann Defense", "Scandinavian Defense", "Alekhine Defense").contains(alias.openingFamily)
      )
    targetAliases.foreach: alias =>
      val text = s"${alias.displayName} ${alias.contextName}".toLowerCase
      assert(!text.contains("solid"), s"alias ${alias.aliasId} must avoid evaluation wording")
      assert(!text.contains("bad bishop"), s"alias ${alias.aliasId} must avoid explanatory evaluation wording")
      assert(!text.contains("refutation"), s"alias ${alias.aliasId} must avoid refutation wording")
    assert(!targetAliases.exists(_.displayName == "Exchange"), "Exchange aliases must carry family prefix")
    assert(!targetAliases.exists(_.displayName == "Classical"), "Classical aliases must carry family prefix")
    assert(!targetAliases.exists(_.displayName == "Modern"), "Modern aliases must carry family prefix")

    val unsupportedExchange = OpeningAlias(
      aliasId = "open-alias-french-exchange-unsupported",
      sourceId = "lichess-openings",
      sourceName = "French Defense: Advance Variation",
      canonicalName = "French Defense: Advance Variation",
      openingFamily = "French Defense",
      openingVariation = "Advance Variation",
      displayName = "French Exchange",
      contextName = "French Exchange Variation",
      aliasKind = "display_alias",
      sourceRefs = Vector("eco-c02-french-defense-advance-variation"),
      negativeBoundaries = Vector("alias_requires_matching_variation")
    )
    val unsupportedError = OpeningContextValidator.validateAlias(unsupportedExchange, lines, manifests).left.getOrElse("")
    assert(unsupportedError.contains("requires French variation containing Exchange"), s"unexpected error: $unsupportedError")

    val fantasy = reject("aliasMissingCaroKannFantasy")
    assertEquals(fantasy.rowType, "aliasCandidate")
    assert((fantasy.payload \ "reason").as[String].contains("source_taxonomy_row_missing"))

    val chase = reject("aliasMissingAlekhineChase")
    assertEquals(chase.rowType, "aliasCandidate")
    assert((chase.payload \ "reason").as[String].contains("source_taxonomy_row_missing"))

  test("queen-pawn defense aliases distinguish declined accepted slav semi-slav and catalan contexts"):
    val byFamily = aliases.groupBy(_.openingFamily).view.mapValues(_.size).toMap

    assertEquals(byFamily.getOrElse("Queen's Gambit Declined", 0), 8)
    assertEquals(byFamily.getOrElse("Queen's Gambit Accepted", 0), 4)
    assertEquals(byFamily.getOrElse("Slav Defense", 0), 6)
    assertEquals(byFamily.getOrElse("Semi-Slav Defense", 0), 6)
    assertEquals(byFamily.getOrElse("Catalan Opening", 0), 3)
    assertEquals(byFamily.getOrElse("Tarrasch Defense", 0), 3)

    def has(family: String, ref: String, display: String, context: String, kind: String): Unit =
      assert(
        aliases.exists(alias =>
          alias.openingFamily == family &&
            alias.sourceRefs == Vector(ref) &&
            alias.displayName == display &&
            alias.contextName == context &&
            alias.aliasKind == kind
        ),
        s"missing $family alias $display for $ref"
      )

    has("Queen's Gambit Declined", "eco-d35-qgd-exchange", "QGD Exchange", "Carlsbad context", "structure_alias")
    has("Queen's Gambit Declined", "eco-d60-queen-s-gambit-declined-orthodox-defense", "QGD Orthodox", "Orthodox QGD", "display_alias")
    has("Queen's Gambit Declined", "eco-d58-queen-s-gambit-declined-tartakower-defense", "QGD Tartakower", "Tartakower QGD", "display_alias")
    has("Queen's Gambit Declined", "eco-d08-queen-s-gambit-declined-albin-countergambit", "Albin Countergambit", "QGD Albin Countergambit", "display_alias")

    has("Queen's Gambit Accepted", "eco-d20-queen-s-gambit-accepted", "Queen's Gambit Accepted", "QGA setup", "context_alias")
    has("Queen's Gambit Accepted", "eco-d20-queen-s-gambit-accepted-central-variation-modern-defense", "QGA Central Variation", "QGA Central Variation", "display_alias")
    has("Queen's Gambit Accepted", "eco-d27-queen-s-gambit-accepted-classical-defense-main-line", "QGA Classical", "QGA Classical Defense", "display_alias")

    has("Slav Defense", "eco-d15-slav-defense-chebanenko-variation", "Chebanenko Slav", "Chebanenko Slav", "display_alias")
    has("Slav Defense", "eco-d12-slav-defense-quiet-variation-schallopp-defense", "Schallopp Slav", "Slav Schallopp Defense", "display_alias")
    has("Slav Defense", "eco-d19-slav-defense-czech-variation-dutch-variation", "Dutch Slav", "Slav Dutch Variation", "display_alias")
    has("Slav Defense", "eco-d17-slav-defense-czech-variation-carlsbad-variation", "Slav Carlsbad", "Slav Carlsbad context", "structure_alias")

    has("Semi-Slav Defense", "eco-d47-semi-slav-defense-meran-variation", "Meran Semi-Slav", "Semi-Slav Meran", "display_alias")
    has("Semi-Slav Defense", "eco-d47-semi-slav-defense-semi-meran-variation", "Semi-Meran", "Semi-Slav Semi-Meran", "display_alias")
    has("Semi-Slav Defense", "eco-d44-semi-slav-defense-botvinnik-variation", "Botvinnik Semi-Slav", "Semi-Slav Botvinnik", "display_alias")
    has("Semi-Slav Defense", "eco-d43-semi-slav-defense-moscow-variation", "Moscow Semi-Slav", "Semi-Slav Moscow", "display_alias")
    has("Semi-Slav Defense", "eco-d43-semi-slav-defense-anti-moscow-gambit", "Anti-Moscow Semi-Slav", "Semi-Slav Anti-Moscow", "display_alias")

    has("Catalan Opening", "eco-e01-catalan-opening-closed", "Closed Catalan", "Closed Catalan", "context_alias")
    has("Catalan Opening", "eco-e04-catalan-open", "Open Catalan", "Catalan with ...dxc4", "context_alias")

    has("Tarrasch Defense", "eco-d32-tarrasch-defense", "Tarrasch Defense", "Tarrasch Defense", "context_alias")
    has("Tarrasch Defense", "eco-d34-tarrasch-defense-classical-variation-main-line", "Classical Tarrasch", "Tarrasch Classical Variation", "display_alias")
    has("Tarrasch Defense", "eco-d33-tarrasch-defense-dubov-tarrasch", "Dubov Tarrasch", "Dubov Tarrasch", "display_alias")

    val targetFamilies = Set(
      "Queen's Gambit Declined",
      "Queen's Gambit Accepted",
      "Slav Defense",
      "Semi-Slav Defense",
      "Catalan Opening",
      "Tarrasch Defense"
    )
    val targetAliases = aliases.filter(alias => targetFamilies.contains(alias.openingFamily))
    val structureAliases = targetAliases.filter(_.aliasKind == "structure_alias")
    assertEquals(structureAliases.map(_.displayName).toSet, Set("QGD Exchange", "Slav Carlsbad"))
    structureAliases.foreach: alias =>
      assert(alias.negativeBoundaries.contains("alias_is_not_position_truth"), s"${alias.aliasId} must remain opening context only")

    val qgaOnQgd = OpeningAlias(
      aliasId = "open-alias-qga-qgd-confusion",
      sourceId = "lichess-openings",
      sourceName = "Queen's Gambit Declined: Exchange Variation",
      canonicalName = "Queen's Gambit Declined: Exchange Variation",
      openingFamily = "Queen's Gambit Declined",
      openingVariation = "Exchange Variation",
      displayName = "QGA Exchange",
      contextName = "Queen's Gambit Accepted",
      aliasKind = "display_alias",
      sourceRefs = Vector("eco-d35-qgd-exchange"),
      negativeBoundaries = Vector("alias_requires_matching_family")
    )
    val qgaError = OpeningContextValidator.validateAlias(qgaOnQgd, lines, manifests).left.getOrElse("")
    assert(qgaError.contains("requires Queen's Gambit Accepted family"), s"unexpected error: $qgaError")

    val closedOnOpenCatalan = OpeningAlias(
      aliasId = "open-alias-closed-catalan-unsupported",
      sourceId = "lichess-openings",
      sourceName = "Catalan Opening: Open Defense",
      canonicalName = "Catalan Opening: Open Defense",
      openingFamily = "Catalan Opening",
      openingVariation = "Open Defense",
      displayName = "Closed Catalan",
      contextName = "Closed Catalan",
      aliasKind = "context_alias",
      sourceRefs = Vector("eco-e04-catalan-open"),
      negativeBoundaries = Vector("alias_requires_matching_variation")
    )
    val catalanError = OpeningContextValidator.validateAlias(closedOnOpenCatalan, lines, manifests).left.getOrElse("")
    assert(catalanError.contains("requires Catalan variation containing Closed"), s"unexpected error: $catalanError")

    val antiMeran = reject("aliasMissingSemiSlavAntiMeran")
    assertEquals(antiMeran.rowType, "aliasCandidate")
    assert((antiMeran.payload \ "reason").as[String].contains("source_taxonomy_row_missing"))
    assertEquals(
      lines.filter(line => line.openingFamily == "Semi-Slav Defense" && line.openingVariation.toLowerCase.contains("anti-meran")).map(_.lineId),
      Vector.empty
    )
    assert(!aliases.exists(_.displayName == "Anti-Meran Semi-Slav"), "Anti-Meran must stay skipped until taxonomy explicitly supports it")

    val antiMeranOnMeran = OpeningAlias(
      aliasId = "open-alias-anti-meran-on-meran-rejected",
      sourceId = "lichess-openings",
      sourceName = "Semi-Slav Defense: Meran Variation",
      canonicalName = "Semi-Slav Defense: Meran Variation",
      openingFamily = "Semi-Slav Defense",
      openingVariation = "Meran Variation",
      displayName = "Anti-Meran Semi-Slav",
      contextName = "Semi-Slav Anti-Meran",
      aliasKind = "display_alias",
      sourceRefs = Vector("eco-d47-semi-slav-defense-meran-variation"),
      negativeBoundaries = Vector("alias_requires_matching_variation")
    )
    val antiMeranError = OpeningContextValidator.validateAlias(antiMeranOnMeran, lines, manifests).left.getOrElse("")
    assert(antiMeranError.contains("requires Semi-Slav variation containing Anti-Meran"), s"unexpected error: $antiMeranError")

  test("indian defense aliases preserve family identity and avoid evaluative labels"):
    val byFamily = aliases.groupBy(_.openingFamily).view.mapValues(_.size).toMap

    assertEquals(byFamily.getOrElse("King's Indian Defense", 0), 5)
    assertEquals(byFamily.getOrElse("Nimzo-Indian Defense", 0), 5)
    assertEquals(byFamily.getOrElse("Queen's Indian Defense", 0), 6)
    assertEquals(byFamily.getOrElse("Bogo-Indian Defense", 0), 4)
    assertEquals(byFamily.getOrElse("Grünfeld Defense", 0), 5)
    assertEquals(byFamily.getOrElse("Neo-Grünfeld Defense", 0), 3)
    assertEquals(byFamily.getOrElse("Old Indian Defense", 0), 4)
    assertEquals(byFamily.getOrElse("Benoni Defense", 0), 5)
    assertEquals(byFamily.getOrElse("Benko Gambit", 0), 3)

    def has(family: String, ref: String, display: String, context: String, kind: String): Unit =
      assert(
        aliases.exists(alias =>
          alias.openingFamily == family &&
            alias.sourceRefs == Vector(ref) &&
            alias.displayName == display &&
            alias.contextName == context &&
            alias.aliasKind == kind
        ),
        s"missing $family alias $display for $ref"
      )

    has("King's Indian Defense", "eco-e61-king-s-indian-defense", "King's Indian Defense", "KID setup", "context_alias")
    has("King's Indian Defense", "eco-e80-king-s-indian-defense-s-misch-variation", "KID Sämisch", "King's Indian Sämisch", "display_alias")
    has("King's Indian Defense", "eco-e98-king-s-indian-defense-orthodox-variation-classical-system", "KID Classical", "King's Indian Classical System", "display_alias")

    has("Nimzo-Indian Defense", "eco-e20-nimzo-indian-defense", "Nimzo-Indian Defense", "Nimzo-Indian setup", "context_alias")
    has("Nimzo-Indian Defense", "eco-e40-nimzo-indian-defense-rubinstein-system", "Nimzo Rubinstein", "Nimzo-Indian Rubinstein System", "display_alias")
    has("Nimzo-Indian Defense", "eco-e30-nimzo-indian-defense-leningrad-variation", "Nimzo Leningrad", "Nimzo-Indian Leningrad Variation", "display_alias")

    has("Queen's Indian Defense", "eco-e12-queen-s-indian-defense", "Queen's Indian Defense", "QID setup", "context_alias")
    has("Queen's Indian Defense", "eco-e12-queen-s-indian-defense-kasparov-petrosian-variation-hedgehog-variation", "QID Hedgehog", "Queen's Indian Hedgehog context", "structure_alias")

    has("Bogo-Indian Defense", "eco-e11-bogo-indian-defense", "Bogo-Indian Defense", "Bogo-Indian setup", "context_alias")
    has("Bogo-Indian Defense", "eco-e11-bogo-indian-defense-wade-smyslov-variation", "Bogo Wade-Smyslov", "Bogo-Indian Wade-Smyslov Variation", "display_alias")

    has("Grünfeld Defense", "eco-d80-gr-nfeld-defense", "Grünfeld Defense", "Grünfeld setup", "context_alias")
    has("Grünfeld Defense", "eco-d85-gr-nfeld-defense-exchange-variation", "Grünfeld Exchange", "Grünfeld Exchange Variation", "display_alias")
    has("Grünfeld Defense", "eco-d82-gr-nfeld-defense-brinckmann-attack", "Brinckmann Grünfeld", "Grünfeld Brinckmann line", "display_alias")

    has("Neo-Grünfeld Defense", "eco-d70-neo-gr-nfeld-defense-with-nf3", "Neo-Grünfeld with Nf3", "Neo-Grünfeld Nf3 context", "context_alias")
    has("Old Indian Defense", "eco-a53-old-indian-defense-czech-variation-with-nc3", "Old Indian Czech", "Old Indian Czech Variation", "display_alias")
    has("Benoni Defense", "eco-a43-benoni-defense-benoni-indian-defense", "Benoni-Indian Defense", "Benoni-Indian context", "context_alias")
    has("Benko Gambit", "eco-a58-benko-gambit-fianchetto-variation", "Benko Fianchetto", "Benko Fianchetto Variation", "display_alias")

    val indianFamilies = Set(
      "King's Indian Defense",
      "Nimzo-Indian Defense",
      "Queen's Indian Defense",
      "Bogo-Indian Defense",
      "Grünfeld Defense",
      "Neo-Grünfeld Defense",
      "Old Indian Defense",
      "Benoni Defense",
      "Benko Gambit"
    )
    val indianAliases = aliases.filter(alias => indianFamilies.contains(alias.openingFamily))
    assertEquals(indianAliases.filter(_.aliasKind == "structure_alias").map(_.displayName).toSet, Set("QID Hedgehog"))
    indianAliases.foreach: alias =>
      val text = s"${alias.displayName} ${alias.contextName}".toLowerCase
      assert(!text.contains("attack"), s"${alias.aliasId} must avoid attack wording")
      assert(!text.contains("pressure"), s"${alias.aliasId} must avoid pressure wording")
      assert(!text.contains("counterplay"), s"${alias.aliasId} must avoid counterplay wording")

    val kidAttack = OpeningAlias(
      aliasId = "open-alias-kid-attack-rejected",
      sourceId = "lichess-openings",
      sourceName = "King's Indian Defense",
      canonicalName = "King's Indian Defense",
      openingFamily = "King's Indian Defense",
      openingVariation = "Main Line",
      displayName = "KID Attack",
      contextName = "King's Indian pressure",
      aliasKind = "display_alias",
      sourceRefs = Vector("eco-e61-king-s-indian-defense"),
      negativeBoundaries = Vector("alias_evaluative_wording_rejected")
    )
    val attackError = OpeningContextValidator.validateAlias(kidAttack, lines, manifests).left.getOrElse("")
    assert(attackError.contains("contains authority wording"), s"unexpected error: $attackError")

    val grunfeldExchangeOnMain = OpeningAlias(
      aliasId = "open-alias-grunfeld-exchange-unsupported",
      sourceId = "lichess-openings",
      sourceName = "Grünfeld Defense",
      canonicalName = "Grünfeld Defense",
      openingFamily = "Grünfeld Defense",
      openingVariation = "Main Line",
      displayName = "Grünfeld Exchange",
      contextName = "Grünfeld Exchange Variation",
      aliasKind = "display_alias",
      sourceRefs = Vector("eco-d80-gr-nfeld-defense"),
      negativeBoundaries = Vector("alias_requires_matching_variation")
    )
    val grunfeldError = OpeningContextValidator.validateAlias(grunfeldExchangeOnMain, lines, manifests).left.getOrElse("")
    assert(grunfeldError.contains("requires Grünfeld variation containing Exchange"), s"unexpected error: $grunfeldError")

    val broadIndian = reject("aliasAmbiguousIndianDefense")
    assertEquals(broadIndian.rowType, "aliasCandidate")
    assert((broadIndian.payload \ "reason").as[String].contains("ambiguous_group_label_not_exact_taxonomy_alias"))

    val indianAttack = reject("aliasEvaluativeIndianAttack")
    assertEquals(indianAttack.rowType, "aliasCandidate")
    assert((indianAttack.payload \ "reason").as[String].contains("evaluative_or_wrong_family_label"))

  test("flank and hypermodern aliases stay taxonomy-backed context labels"):
    val byFamily = aliases.groupBy(_.openingFamily).view.mapValues(_.size).toMap

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

    def has(family: String, ref: String, display: String, context: String, kind: String): Unit =
      assert(
        aliases.exists(alias =>
          alias.openingFamily == family &&
            alias.sourceRefs == Vector(ref) &&
            alias.displayName == display &&
            alias.contextName == context &&
            alias.aliasKind == kind
        ),
        s"missing $family alias $display for $ref"
      )

    has("English Opening", "eco-a21-english-opening-king-s-english-variation-reversed-sicilian", "English Reversed Sicilian", "King's English Reversed Sicilian", "display_alias")
    has("English Opening", "eco-a30-english-opening-symmetrical-variation", "Symmetrical English", "English Symmetrical Variation", "display_alias")
    has("English Opening", "eco-a26-english-opening-king-s-english-variation-botvinnik-system", "Botvinnik English", "English Botvinnik System", "display_alias")
    has("English Opening", "eco-a28-english-opening-king-s-english-variation-four-knights-variation", "Four Knights English", "English Four Knights", "display_alias")

    has("Réti Opening", "eco-a09-r-ti-opening", "Réti Opening", "Réti setup", "context_alias")
    has("Réti Opening", "eco-a09-r-ti-opening-r-ti-accepted", "Réti Accepted", "Réti Accepted context", "display_alias")
    has("King's Indian Attack", "eco-a08-king-s-indian-attack-french-variation", "KIA French", "King's Indian Attack French Variation", "display_alias")
    has("Nimzo-Larsen Attack", "eco-a01-nimzo-larsen-attack-classical-variation", "Nimzo-Larsen Classical", "Nimzo-Larsen Classical Variation", "display_alias")

    has("Dutch Defense", "eco-a92-dutch-defense-stonewall-variation", "Stonewall Dutch", "Dutch Stonewall context", "structure_alias")
    has("Dutch Defense", "eco-a86-dutch-defense-leningrad-variation", "Leningrad Dutch", "Dutch Leningrad Variation", "display_alias")
    has("Modern Defense", "eco-a42-modern-defense-averbakh-system", "Modern Averbakh", "Modern Defense Averbakh System", "display_alias")
    has("Pirc Defense", "eco-b09-pirc-defense-austrian-attack", "Pirc Austrian", "Pirc Austrian Attack", "display_alias")

    has("Queen's Pawn Game", "eco-d02-queen-s-pawn-game-london-system", "QPG London", "Queen's Pawn London context", "context_alias")
    has("Queen's Pawn Game", "eco-d03-queen-s-pawn-game-torre-attack", "QPG Torre", "Queen's Pawn Torre context", "context_alias")
    has("Queen's Pawn Game", "eco-d04-queen-s-pawn-game-colle-system", "QPG Colle", "Queen's Pawn Colle context", "context_alias")
    has("Zukertort Opening", "eco-a04-zukertort-opening-kingside-fianchetto", "Zukertort Fianchetto", "Zukertort Kingside Fianchetto", "display_alias")

    val flankFamilies = Set(
      "English Opening",
      "Réti Opening",
      "King's Indian Attack",
      "Nimzo-Larsen Attack",
      "Bird Opening",
      "Dutch Defense",
      "Modern Defense",
      "Pirc Defense",
      "Polish Opening",
      "Trompowsky Attack",
      "London System",
      "Queen's Pawn Game",
      "Zukertort Opening"
    )
    val flankAliases = aliases.filter(alias => flankFamilies.contains(alias.openingFamily))
    assertEquals(flankAliases.filter(_.aliasKind == "structure_alias").map(_.displayName).toSet, Set("Stonewall Dutch"))

    val pircOnModern = OpeningAlias(
      aliasId = "open-alias-pirc-modern-confusion",
      sourceId = "lichess-openings",
      sourceName = "Modern Defense",
      canonicalName = "Modern Defense",
      openingFamily = "Modern Defense",
      openingVariation = "Main Line",
      displayName = "Pirc Defense",
      contextName = "Pirc setup",
      aliasKind = "display_alias",
      sourceRefs = Vector("eco-b06-modern-defense"),
      negativeBoundaries = Vector("alias_requires_matching_family")
    )
    val pircError = OpeningContextValidator.validateAlias(pircOnModern, lines, manifests).left.getOrElse("")
    assert(pircError.contains("requires Pirc Defense family"), s"unexpected error: $pircError")

    val reversedSicilianOnMainEnglish = OpeningAlias(
      aliasId = "open-alias-english-reversed-sicilian-unsupported",
      sourceId = "lichess-openings",
      sourceName = "English Opening",
      canonicalName = "English Opening",
      openingFamily = "English Opening",
      openingVariation = "Main Line",
      displayName = "English Reversed Sicilian",
      contextName = "King's English Reversed Sicilian",
      aliasKind = "display_alias",
      sourceRefs = Vector("eco-a10-english-opening"),
      negativeBoundaries = Vector("alias_requires_matching_variation")
    )
    val englishError = OpeningContextValidator.validateAlias(reversedSicilianOnMainEnglish, lines, manifests).left.getOrElse("")
    assert(englishError.contains("requires English variation containing Reversed Sicilian"), s"unexpected error: $englishError")

    val greatSnake = reject("aliasSkipEnglishGreatSnake")
    assertEquals(greatSnake.rowType, "aliasCandidate")
    assert((greatSnake.payload \ "reason").as[String].contains("rare_taxonomy_name_kept"))

    val walrus = reject("aliasSkipZukertortWalrus")
    assertEquals(walrus.rowType, "aliasCandidate")
    assert((walrus.payload \ "reason").as[String].contains("rare_taxonomy_name_kept"))

  test("gambit and rare system aliases stay source-backed without evaluation wording"):
    val byFamily = aliases.groupBy(_.openingFamily).view.mapValues(_.size).toMap

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
    assertEquals(byFamily.getOrElse("Benko Gambit", 0), 3)
    assertEquals(byFamily.getOrElse("Polish Opening", 0), 3)

    def has(family: String, ref: String, display: String, context: String, kind: String): Unit =
      assert(
        aliases.exists(alias =>
          alias.openingFamily == family &&
            alias.sourceRefs == Vector(ref) &&
            alias.displayName == display &&
            alias.contextName == context &&
            alias.aliasKind == kind
        ),
        s"missing $family alias $display for $ref"
      )

    has("King's Gambit", "eco-c30-king-s-gambit", "King's Gambit", "King's Gambit context", "context_alias")
    has("King's Gambit Accepted", "eco-c33-king-s-gambit-accepted", "King's Gambit Accepted", "KGA context", "context_alias")
    has("King's Gambit Declined", "eco-c30-king-s-gambit-declined-classical-variation", "King's Gambit Declined", "KGD Classical Variation", "context_alias")
    has("Vienna Game", "eco-c29-vienna-game-vienna-gambit", "Vienna Gambit", "Vienna Game Gambit", "display_alias")
    has("Danish Gambit", "eco-c21-danish-gambit", "Danish Gambit", "Danish Gambit context", "context_alias")
    has("Danish Gambit Accepted", "eco-c21-danish-gambit-accepted", "Danish Gambit Accepted", "Danish Gambit Accepted context", "context_alias")
    has("Latvian Gambit", "eco-c40-latvian-gambit", "Latvian Gambit", "Latvian Gambit context", "context_alias")
    has("Latvian Gambit Accepted", "eco-c40-latvian-gambit-accepted", "Latvian Gambit Accepted", "Latvian Gambit Accepted context", "context_alias")
    has("Englund Gambit", "eco-a40-englund-gambit", "Englund Gambit", "Englund Gambit context", "context_alias")
    has("Englund Gambit Declined", "eco-a40-englund-gambit-declined", "Englund Gambit Declined", "Englund Gambit Declined context", "context_alias")
    has("Blackmar-Diemer Gambit", "eco-d00-blackmar-diemer-gambit", "Blackmar-Diemer Gambit", "Blackmar-Diemer context", "context_alias")
    has("Blackmar-Diemer Gambit Accepted", "eco-d00-blackmar-diemer-gambit-accepted", "Blackmar-Diemer Gambit Accepted", "Blackmar-Diemer Accepted context", "context_alias")
    has("Indian Defense", "eco-a51-indian-defense-budapest-defense", "Budapest Defense", "Indian Defense Budapest context", "context_alias")
    has("Grob Opening", "eco-a00-grob-opening", "Grob Opening", "Grob context", "context_alias")
    has("Van Geet Opening", "eco-a00-van-geet-opening", "Van Geet Opening", "Van Geet context", "context_alias")
    has("Amar Opening", "eco-a00-amar-opening", "Amar Opening", "Amar context", "context_alias")
    has("Italian Game", "eco-c51-italian-game-evans-gambit", "Evans Gambit", "Evans Italian", "display_alias")
    has("Queen's Gambit Declined", "eco-d08-queen-s-gambit-declined-albin-countergambit", "Albin Countergambit", "QGD Albin Countergambit", "display_alias")

    val targetFamilies = Set(
      "King's Gambit",
      "King's Gambit Accepted",
      "King's Gambit Declined",
      "Vienna Game",
      "Danish Gambit",
      "Danish Gambit Accepted",
      "Latvian Gambit",
      "Latvian Gambit Accepted",
      "Englund Gambit",
      "Englund Gambit Declined",
      "Blackmar-Diemer Gambit",
      "Blackmar-Diemer Gambit Accepted",
      "Indian Defense",
      "Grob Opening",
      "Van Geet Opening",
      "Amar Opening"
    )
    val targetAliases = aliases.filter(alias => targetFamilies.contains(alias.openingFamily))
    targetAliases.foreach: alias =>
      val text = s"${alias.displayName} ${alias.contextName}".toLowerCase
      assert(!text.contains("dubious"), s"${alias.aliasId} must avoid evaluative wording")
      assert(!text.contains("refuted"), s"${alias.aliasId} must avoid evaluative wording")
      assert(!text.contains("trap"), s"${alias.aliasId} must avoid trap wording")
      assert(!text.contains("best"), s"${alias.aliasId} must avoid best wording")
      assert(!text.contains("winning"), s"${alias.aliasId} must avoid result wording")

    val budapestGambit = OpeningAlias(
      aliasId = "open-alias-budapest-gambit-unsupported",
      sourceId = "lichess-openings",
      sourceName = "Indian Defense: Budapest Defense",
      canonicalName = "Indian Defense: Budapest Defense",
      openingFamily = "Indian Defense",
      openingVariation = "Budapest Defense",
      displayName = "Budapest Gambit",
      contextName = "Budapest Gambit",
      aliasKind = "display_alias",
      sourceRefs = Vector("eco-a51-indian-defense-budapest-defense"),
      negativeBoundaries = Vector("gambit_alias_requires_taxonomy_gambit")
    )
    val budapestError = OpeningContextValidator.validateAlias(budapestGambit, lines, manifests).left.getOrElse("")
    assert(budapestError.contains("requires Indian Defense variation containing Gambit"), s"unexpected error: $budapestError")

    val blackmarAcceptedOnMain = OpeningAlias(
      aliasId = "open-alias-blackmar-accepted-unsupported",
      sourceId = "lichess-openings",
      sourceName = "Blackmar-Diemer Gambit",
      canonicalName = "Blackmar-Diemer Gambit",
      openingFamily = "Blackmar-Diemer Gambit",
      openingVariation = "Main Line",
      displayName = "Blackmar-Diemer Gambit Accepted",
      contextName = "Blackmar-Diemer Accepted context",
      aliasKind = "context_alias",
      sourceRefs = Vector("eco-d00-blackmar-diemer-gambit"),
      negativeBoundaries = Vector("accepted_alias_requires_accepted_taxonomy")
    )
    val blackmarError = OpeningContextValidator.validateAlias(blackmarAcceptedOnMain, lines, manifests).left.getOrElse("")
    assert(blackmarError.contains("requires Blackmar-Diemer Gambit Accepted family"), s"unexpected error: $blackmarError")

    val trapAlias = OpeningAlias(
      aliasId = "open-alias-englund-trap-rejected",
      sourceId = "lichess-openings",
      sourceName = "Englund Gambit",
      canonicalName = "Englund Gambit",
      openingFamily = "Englund Gambit",
      openingVariation = "Main Line",
      displayName = "Englund Trap",
      contextName = "Dubious Englund",
      aliasKind = "display_alias",
      sourceRefs = Vector("eco-a40-englund-gambit"),
      negativeBoundaries = Vector("alias_evaluative_wording_rejected")
    )
    val trapError = OpeningContextValidator.validateAlias(trapAlias, lines, manifests).left.getOrElse("")
    assert(trapError.contains("contains authority wording"), s"unexpected error: $trapError")

    for kind <- Vector("aliasSkipGrobCocaCola", "aliasSkipVanGeetGambitCluster", "aliasSkipAmarGent", "aliasSkipBudapestGambitName") do
      val skipped = reject(kind)
      assertEquals(skipped.rowType, "aliasCandidate")
      assert((skipped.payload \ "reason").as[String].nonEmpty)

  test("opening move stats admit candidates only as statistics or references"):
    moveStats.foreach: stat =>
      assertEquals(OpeningContextValidator.validateMoveStat(stat, positions, manifests), Right(stat))
      val candidate = OpeningCandidate.fromMoveStat(stat).fold(message => fail(message), identity)
      assertEquals(candidate.positionKey, stat.positionKey)
      assertEquals(candidate.move, stat.move)
      assertEquals(candidate.candidateRole, stat.candidateKind)
      assertEquals(candidate.currentPositionTruth, false)
      assert(
        Set("statistical_reference", "context_reference").contains(stat.candidateKind),
        s"candidate role must stay reference-only for ${stat.statId}"
      )

    val bestCandidate = OpeningMoveStat.fromJson(reject("candidateTruthLeak").payload)
    val error = OpeningContextValidator.validateMoveStat(bestCandidate, positions, manifests).left.getOrElse("")
    assert(error.contains("must not claim best/theory/truth authority"), s"unexpected error: $error")
    assert(OpeningCandidate.fromMoveStat(bestCandidate).isLeft, "truth-leaking move stat must not become a candidate")

    val extraTruthField = reject("candidateTruthLeak").payload.as[JsObject] ++ Json.obj(
      "id" -> "open-stat-extra-truth",
      "candidateKind" -> "statistical_reference",
      "bestMove" -> "g1f3"
    )
    val extraFieldError = intercept[IllegalArgumentException]:
      OpeningMoveStat.fromJson(extraTruthField)
    assert(extraFieldError.getMessage.contains("uses unsupported fields: bestMove"))

  test("unknown source is rejected before line or stat admission"):
    val unknownLine = OpeningLine.fromJson(reject("unknownSource").payload)
    val error = OpeningContextValidator.validateLine(unknownLine, manifests).left.getOrElse("")
    assert(error.contains("unknown sourceId missing-opening-source"), s"unexpected error: $error")

  test("opening builder refuses committed artifact output paths"):
    assert(
      OpeningIndexBuilder.validateOutputPath("tmp/commentary-opening/generated/lichess-openings/opening-positions.jsonl").isRight,
      "builder should allow ignored tmp generated output"
    )
    assert(
      OpeningIndexBuilder.validateOutputPath("modules/commentary/.local/opening/cache/index.jsonl").isRight,
      "builder should allow ignored local opening cache"
    )
    assert(
      OpeningIndexBuilder
        .validateOutputPath("modules/commentary/src/test/resources/commentary-corpus/generated-opening/sample.jsonl")
        .isRight,
      "builder should allow ignored fixture-adjacent generated output"
    )

    val committed = (reject("committedOutputPath").payload \ "path").as[String]
    val error = OpeningIndexBuilder.validateOutputPath(committed).left.getOrElse("")
    assert(error.contains("outside ignored opening local output roots"), s"unexpected error: $error")

    Vector(
      "tmp/commentary-opening/..",
      "modules/commentary/.local/opening/..",
      "modules/commentary/src/test/resources/commentary-corpus/generated-opening/.."
    ).foreach: escaped =>
      val escapedBuilderError = OpeningIndexBuilder.validateOutputPath(escaped).left.getOrElse("")
      assert(escapedBuilderError.contains("outside ignored opening local output roots"), s"unexpected error for $escaped: $escapedBuilderError")

    val recent = manifests.find(_.sourceId == "lichess-games-standard-rated-2026-03-all").getOrElse(fail("missing recent source"))
    val escapedArtifact = recent.copy(
      sourceId = "recent-escaped-artifact",
      generatedArtifacts = Vector(
        OpeningSourceArtifact(
          path = "tmp/commentary-opening/../repo-visible.jsonl",
          storagePolicy = "localOnly",
          checksum = Some("fixture"),
          rowCount = Some(1)
        )
      )
    )
    val escapedError = OpeningContextValidator.validateManifest(escapedArtifact).left.getOrElse("")
    assert(escapedError.contains("outside ignored opening local output roots"), s"unexpected error: $escapedError")

  private def reject(kind: String): OpeningRejectFixture =
    rejects.find(_.rejectKind == kind).getOrElse(fail(s"missing reject fixture $kind"))
