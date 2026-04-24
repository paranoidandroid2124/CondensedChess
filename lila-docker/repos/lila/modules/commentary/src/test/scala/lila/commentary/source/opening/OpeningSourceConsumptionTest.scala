package lila.commentary.source.opening

import play.api.libs.json.Json

class OpeningSourceConsumptionTest extends munit.FunSuite:

  private val manifests = OpeningSourceCorpus.loadManifests()
  private val lines = OpeningSourceCorpus.loadLines()
  private val positions = OpeningSourceCorpus.loadPositions()
  private val aliases = OpeningSourceCorpus.loadAliases()
  private val moveStats = OpeningSourceCorpus.loadMoveStats()
  private val consumptionCandidates = OpeningSourceCorpus.loadConsumptionCandidates()

  private val catalanLine = lines.find(_.lineId == "eco-e04-catalan-open").getOrElse(fail("missing Catalan line"))
  private val catalanAlias = aliases.find(_.aliasId == "open-alias-catalan-open").getOrElse(fail("missing Catalan alias"))
  private val catalanPosition = positions.find(_.positionId == "open-pos-catalan-open-tabia").getOrElse(fail("missing Catalan position"))
  private val catalanKey = catalanPosition.positionKey
  private val smokeSource = manifests.find(_.sourceUse.contains("pipeline_smoke")).getOrElse(fail("missing smoke source"))
  private val onlineSource = manifests.find(_.sourceUse.contains("online_trend")).getOrElse(fail("missing online trend source"))
  private val masterSource = manifests.find(_.sourceUse.contains("master_reference")).getOrElse(fail("missing master source"))

  test("opening consumption fixture validates structured candidate shape"):
    val candidate = consumptionCandidates.find(_.candidateId == "open-context-catalan-master-online").getOrElse(fail("missing consumption fixture"))
    val validated = OpeningConsumptionContract.validateCandidate(candidate, manifests, lines, aliases, moveStats).fold(message => fail(message.reason), identity)

    assertEquals(validated.openingIdentity.canonicalName, "Catalan Opening: Open Defense")
    assertEquals(validated.displayAlias.map(_.displayName), Some("Open Catalan"))
    assertEquals(validated.sourceSelection.primarySourceUse, Some("master_reference"))
    assertEquals(validated.sourceSelection.secondarySourceUses, Vector("online_trend"))
    assertEquals(validated.sourceSelection.suppressedSourceUses, Vector("pipeline_smoke"))
    assertEquals(validated.sourceSelection.mergedRankings, false)
    assertEquals(validated.confidence, OpeningConfidence.Usable)
    assert(validated.boundaries.exists(_.value == "move_stats_are_reference_only"))
    assert(validated.boundaries.exists(_.value == "no_specific_game_citation"))

  test("source selection uses master_reference primary and online_trend secondary without merging rankings"):
    val candidate = build(
      Vector(
        stat("master-qa4", masterSource.sourceId, "d1a4", 8, 0.42),
        stat("online-nf3", onlineSource.sourceId, "g1f3", 1000, 0.31),
        stat("smoke-qa4", smokeSource.sourceId, "d1a4", 1200, 0.34)
      )
    ).fold(message => fail(message.reason), identity)

    assertEquals(candidate.sourceSelection.primarySourceUse, Some("master_reference"))
    assertEquals(candidate.sourceSelection.secondarySourceUses, Vector("online_trend"))
    assertEquals(candidate.sourceSelection.suppressedSourceUses, Vector("pipeline_smoke"))
    assertEquals(candidate.primaryReferenceStats.map(_.move), Vector("d1a4"))
    assertEquals(candidate.secondaryTrendStats.map(_.move), Vector("g1f3"))
    assertEquals(candidate.sourceSelection.mergedRankings, false)
    assert(candidate.boundaries.exists(_.value == "source_disagreement_context_only"))
    assert(!candidate.primaryReferenceStats.exists(_.sourceUse == "online_trend"))
    assert(!candidate.secondaryTrendStats.exists(_.sourceUse == "master_reference"))

  test("low master sample is suppressed and online-only input remains secondary only"):
    val lowMaster = build(Vector(stat("master-low", masterSource.sourceId, "d1a4", 3, 0.5)))
      .fold(message => fail(message.reason), identity)
    assertEquals(lowMaster.confidence, OpeningConfidence.Suppressed)
    assert(lowMaster.boundaries.exists(_.value == "master_reference_below_threshold"))

    val onlineOnly = build(Vector(stat("online-only", onlineSource.sourceId, "g1f3", 1000, 0.31)))
      .fold(message => fail(message.reason), identity)
    assertEquals(onlineOnly.confidence, OpeningConfidence.SecondaryOnly)
    assertEquals(onlineOnly.sourceSelection.primarySourceUse, None)
    assertEquals(onlineOnly.secondaryTrendStats.map(_.sourceUse), Vector("online_trend"))
    assert(onlineOnly.boundaries.exists(_.value == "online_trend_secondary_only"))

  test("pipeline_smoke alone cannot create a product consumption candidate"):
    val rejected = build(Vector(stat("smoke-only", smokeSource.sourceId, "d1a4", 1200, 0.34))).left.getOrElse(fail("expected rejection"))
    assert(rejected.reason.contains("no product candidate"), rejected.reason)

  test("opening context requires exact position key and taxonomy row"):
    val noKey = OpeningConsumptionContract.buildCandidate(
      "no-key",
      OpeningPositionKey(""),
      Some(catalanLine),
      Some(catalanAlias),
      Vector(stat("master-qa4", masterSource.sourceId, "d1a4", 8, 0.42)),
      manifests,
      lines
    )
    assert(noKey.left.exists(_.reason.contains("no exact positionKey")))

    val noLine = OpeningConsumptionContract.buildCandidate(
      "no-line",
      catalanKey,
      None,
      None,
      Vector(stat("master-qa4", masterSource.sourceId, "d1a4", 8, 0.42)),
      manifests,
      lines
    )
    assert(noLine.left.exists(_.reason.contains("no taxonomy row")))

  test("alias and taxonomy identity are preserved and alias cannot rewrite source names"):
    val accepted = build(Vector(stat("master-qa4", masterSource.sourceId, "d1a4", 8, 0.42)))
      .fold(message => fail(message.reason), identity)
    assertEquals(accepted.openingIdentity.sourceName, "Catalan Opening: Open Defense")
    assertEquals(accepted.openingIdentity.canonicalName, "Catalan Opening: Open Defense")
    assertEquals(accepted.displayAlias.map(_.contextName), Some("Catalan with ...dxc4"))

    val rewritten = accepted.copy(openingIdentity = accepted.openingIdentity.copy(canonicalName = "Open Catalan"))
    val rejected = OpeningConsumptionContract.validateCandidate(rewritten, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(rejected.reason.contains("preserve source taxonomy"), rejected.reason)

    val aliasRewrite = accepted.copy(
      displayAlias = accepted.displayAlias.map(_.copy(displayName = "Best Open Catalan"))
    )
    val aliasRejected = OpeningConsumptionContract.validateCandidate(aliasRewrite, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(aliasRejected.reason.contains("alias row") || aliasRejected.reason.contains("truth wording"), aliasRejected.reason)

    val wrongAlias = aliases.find(_.aliasId == "open-alias-ruy-spanish").getOrElse(fail("missing Ruy Lopez alias"))
    val mismatchedAlias = accepted.copy(displayAlias = Some(OpeningContextAlias.fromAlias(wrongAlias)))
    val mismatchRejected = OpeningConsumptionContract.validateCandidate(mismatchedAlias, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(mismatchRejected.reason.contains("taxonomy sourceRef"), mismatchRejected.reason)

  test("move stat wording cannot become strongest-move or theory truth"):
    val truthStat = stat("truth-stat", masterSource.sourceId, "d1a4", 8, 0.42).copy(candidateKind = "theory_truth")
    val rejected = build(Vector(truthStat)).left.getOrElse(fail("expected rejection"))
    assert(rejected.reason.contains("truth-bearing") || rejected.reason.contains("candidate"), rejected.reason)

    val merged = consumptionCandidates.head.copy(
      sourceSelection = consumptionCandidates.head.sourceSelection.copy(mergedRankings = true)
    )
    val mergedRejected = OpeningConsumptionContract.validateCandidate(merged, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(mergedRejected.reason.contains("rankings must not be merged"), mergedRejected.reason)

    val engineAuthority = stat("engine-authority", masterSource.sourceId, "d1a4", 8, 0.42).copy(authority = "engine_verdict")
    val engineRejected = build(Vector(engineAuthority)).left.getOrElse(fail("expected rejection"))
    assert(engineRejected.reason.contains("truth-bearing") || engineRejected.reason.contains("opening_statistic"), engineRejected.reason)

    Vector("best_reference", "forced_reference", "result_reference", "oracle_reference").foreach: authority =>
      val leaking = stat(s"$authority-stat", masterSource.sourceId, "d1a4", 8, 0.42).copy(authority = authority)
      val leakingRejected = build(Vector(leaking)).left.getOrElse(fail(s"expected rejection for $authority"))
      assert(leakingRejected.reason.contains("truth-bearing") || leakingRejected.reason.contains("opening_statistic"), leakingRejected.reason)

  test("specific game citation fields are deferred to retrieval"):
    val citation = Json.obj(
      "statId" -> "master-game-citation",
      "sourceId" -> masterSource.sourceId,
      "sourceUse" -> "master_reference",
      "aggregateUse" -> "master_reference_stat",
      "move" -> "d1a4",
      "sampleSize" -> 8,
      "frequency" -> 0.42,
      "candidateKind" -> "statistical_reference",
      "authority" -> "opening_statistic",
      "confidence" -> "usable",
      "gameUrl" -> "https://example.invalid/game"
    )
    val rejected = OpeningConsumptionContract.rejectTruthFields(citation).left.getOrElse(fail("expected rejection"))
    assert(rejected.reason.contains("deferred/truth fields"), rejected.reason)
    interceptMessage[IllegalArgumentException]("Opening reference stats row uses unsupported fields: gameUrl"):
      OpeningReferenceStats.fromJson(citation)

    Vector("playerUrl", "eventUrl").foreach: field =>
      val fieldRejected = OpeningConsumptionContract.rejectTruthFields(citation - "gameUrl" ++ Json.obj(field -> "https://example.invalid/ref"))
        .left
        .getOrElse(fail(s"expected rejection for $field"))
      assert(fieldRejected.reason.contains("deferred/truth fields"), fieldRejected.reason)

  test("legacy sourceUse metadata fails closed for product consumption"):
    val legacyMaster = masterSource.copy(sourceId = "legacy-master-source", sourceUse = None)
    val legacyStat = stat("legacy-master-stat", legacyMaster.sourceId, "d1a4", 8, 0.42)
    val rejected =
      OpeningConsumptionContract
        .buildCandidate("legacy-source", catalanKey, Some(catalanLine), Some(catalanAlias), Vector(legacyStat), manifests :+ legacyMaster, lines)
        .left
        .getOrElse(fail("expected rejection"))
    assert(rejected.reason.contains("sourceUse"), rejected.reason)

  test("candidate confidence must match master threshold and source role"):
    val candidate = consumptionCandidates.head
    val inflatedStat = candidate.primaryReferenceStats.head.copy(confidence = OpeningConfidence.High)
    val inflated = candidate.copy(primaryReferenceStats = Vector(inflatedStat), confidence = OpeningConfidence.High)
    val rejected = OpeningConsumptionContract.validateCandidate(inflated, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(rejected.reason.contains("confidence"), rejected.reason)

    val wrongOnline = candidate.copy(
      secondaryTrendStats = candidate.secondaryTrendStats.map(_.copy(confidence = OpeningConfidence.High))
    )
    val onlineRejected = OpeningConsumptionContract.validateCandidate(wrongOnline, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(onlineRejected.reason.contains("secondary_only"), onlineRejected.reason)

  test("persisted candidate stats must match committed move-stat fixture rows"):
    val candidate = consumptionCandidates.head
    val wrongPosition = candidate.copy(positionKey = OpeningPositionKey("std:8/8/8/8/8/8/8/8 w - -"))
    val positionRejected = OpeningConsumptionContract.validateCandidate(wrongPosition, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(positionRejected.reason.contains("positionKey"), positionRejected.reason)

    val rewrittenStat = candidate.copy(
      primaryReferenceStats = candidate.primaryReferenceStats.map(_.copy(move = "g1f3"))
    )
    val statRejected = OpeningConsumptionContract.validateCandidate(rewrittenStat, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(statRejected.reason.contains("move must match fixture"), statRejected.reason)

    val unknownStat = candidate.copy(
      primaryReferenceStats = candidate.primaryReferenceStats.map(_.copy(statId = "missing-master-stat"))
    )
    val missingRejected = OpeningConsumptionContract.validateCandidate(unknownStat, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(missingRejected.reason.contains("committed move-stat fixture"), missingRejected.reason)

  test("sourceRefs cannot carry specific game citation"):
    val candidate = consumptionCandidates.head.copy(sourceRefs = consumptionCandidates.head.sourceRefs :+ "game:https://example.invalid/game")
    val rejected = OpeningConsumptionContract.validateCandidate(candidate, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(rejected.reason.contains("retrieval"), rejected.reason)

    val gameUrl = consumptionCandidates.head.copy(sourceRefs = consumptionCandidates.head.sourceRefs :+ "gameUrl:https://example.invalid/game")
    val gameUrlRejected = OpeningConsumptionContract.validateCandidate(gameUrl, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(gameUrlRejected.reason.contains("retrieval"), gameUrlRejected.reason)

    Vector(
      "playerUrl:https://example.invalid/player",
      "eventUrl:https://example.invalid/event",
      "source:https://example.invalid/game"
    ).foreach: ref =>
      val leaked = consumptionCandidates.head.copy(sourceRefs = consumptionCandidates.head.sourceRefs :+ ref)
      val leakedRejected = OpeningConsumptionContract.validateCandidate(leaked, manifests, lines, aliases, moveStats).left.getOrElse(fail(s"expected rejection for $ref"))
      assert(leakedRejected.reason.contains("retrieval"), leakedRejected.reason)

  test("persisted candidate boundary text cannot carry source truth wording"):
    val theoryBoundary = consumptionCandidates.head.copy(boundaries = consumptionCandidates.head.boundaries :+ OpeningContextBoundary("theory_reference"))
    val theoryRejected = OpeningConsumptionContract.validateCandidate(theoryBoundary, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(theoryRejected.reason.contains("truth wording"), theoryRejected.reason)

    val engineBoundary = consumptionCandidates.head.copy(boundaries = consumptionCandidates.head.boundaries :+ OpeningContextBoundary("engine_context"))
    val engineRejected = OpeningConsumptionContract.validateCandidate(engineBoundary, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(engineRejected.reason.contains("truth wording"), engineRejected.reason)

  test("primary and secondary stat vectors reject sourceUse swaps"):
    val candidate = consumptionCandidates.head
    val swapped = candidate.copy(
      primaryReferenceStats = candidate.secondaryTrendStats,
      secondaryTrendStats = candidate.primaryReferenceStats
    )
    val rejected = OpeningConsumptionContract.validateCandidate(swapped, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(rejected.reason.contains("primaryReferenceStats"), rejected.reason)

    val mislabeledSelection = candidate.copy(
      sourceSelection = candidate.sourceSelection.copy(primarySourceUse = Some("online_trend"))
    )
    val selectionRejected = OpeningConsumptionContract.validateCandidate(mislabeledSelection, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(selectionRejected.reason.contains("source selection primary"), selectionRejected.reason)

    val mislabeledStat = candidate.copy(
      primaryReferenceStats = candidate.primaryReferenceStats.map(_.copy(sourceUse = "online_trend"))
    )
    val statRoleRejected = OpeningConsumptionContract.validateCandidate(mislabeledStat, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(statRoleRejected.reason.contains("sourceUse must be master_reference"), statRoleRejected.reason)

  test("persisted source selection rejects empty product stats and missing smoke suppression"):
    val candidate = consumptionCandidates.head
    val emptyStats = candidate.copy(
      primaryReferenceStats = Vector.empty,
      secondaryTrendStats = Vector.empty,
      sourceSelection = OpeningSourceSelection.empty,
      confidence = OpeningConfidence.Suppressed
    )
    val emptyRejected = OpeningConsumptionContract.validateCandidate(emptyStats, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(emptyRejected.reason.contains("no product candidate"), emptyRejected.reason)

    val missingSuppressed = candidate.copy(
      sourceSelection = candidate.sourceSelection.copy(suppressedSourceUses = Vector.empty)
    )
    val suppressedRejected = OpeningConsumptionContract.validateCandidate(missingSuppressed, manifests, lines, aliases, moveStats).left.getOrElse(fail("expected rejection"))
    assert(suppressedRejected.reason.contains("suppressed sources"), suppressedRejected.reason)

  test("persisted opening identity must come from taxonomy_reference source"):
    val candidate = consumptionCandidates.head
    val nonTaxonomyLine = catalanLine.copy(sourceId = onlineSource.sourceId)
    val rejected =
      OpeningConsumptionContract
        .validateCandidate(candidate, manifests, lines.filterNot(_.lineId == catalanLine.lineId) :+ nonTaxonomyLine, aliases, moveStats)
        .left
        .getOrElse(fail("expected rejection"))
    assert(rejected.reason.contains("taxonomy_reference"), rejected.reason)

  private def build(stats: Vector[OpeningMoveStat]): Either[OpeningConsumptionReject, OpeningContextCandidate] =
    OpeningConsumptionContract.buildCandidate(
      candidateId = "built-catalan-context",
      positionKey = catalanKey,
      line = Some(catalanLine),
      alias = Some(catalanAlias),
      stats = stats,
      manifests = manifests,
      lines = lines
    )

  private def stat(id: String, sourceId: String, move: String, sampleSize: Int, frequency: Double): OpeningMoveStat =
    val white = sampleSize / 2
    val draws = sampleSize - white
    moveStats.head.copy(
      statId = id,
      sourceId = sourceId,
      positionKey = catalanKey,
      move = move,
      sampleSize = sampleSize,
      whiteWins = white,
      draws = draws,
      blackWins = 0,
      frequency = frequency,
      candidateKind = "statistical_reference",
      authority = "opening_statistic",
      negativeBoundaries = Vector("candidate_is_not_optimal_move")
    )
