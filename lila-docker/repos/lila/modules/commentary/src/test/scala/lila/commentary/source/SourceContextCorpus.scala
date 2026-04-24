package lila.commentary.source

import chess.format.Fen

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

enum SourceFamilyStatus:
  case Active, Deferred, Rejected

private[source] object SourceContextCorpus:

  private val manifestPath = "/commentary-corpus/opening-sources.jsonl"
  private val openingLinePath = "/commentary-corpus/opening-lines.jsonl"
  private val openingPositionPath = "/commentary-corpus/opening-positions.jsonl"
  private val openingMoveStatPath = "/commentary-corpus/opening-move-stats.jsonl"
  private val openingThemePath = "/commentary-corpus/opening-themes.jsonl"
  private val openingAliasPath = "/commentary-corpus/opening-aliases.jsonl"
  private val motifExamplePath = "/commentary-corpus/motif-examples.jsonl"
  private val motifRejectPath = "/commentary-corpus/motif-reject-fixtures.jsonl"
  private val endgameStudyPath = "/commentary-corpus/endgame-studies.jsonl"
  private val endgameStudyFixturePath = "/commentary-corpus/endgame-study-fixtures.jsonl"
  private val endgameStudyRejectPath = "/commentary-corpus/endgame-study-reject-fixtures.jsonl"
  private val retrievalExamplePath = "/commentary-corpus/retrieval-examples.jsonl"
  private val retrievalRejectPath = "/commentary-corpus/retrieval-reject-fixtures.jsonl"

  val resourceFileNames: Vector[String] = Vector(
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

  private val activeFamilies = Set("opening", "motif", "endgameStudy", "retrieval")
  private val allowedLicenses = Set("CC0-1.0", "CC-BY-SA-4.0", "public-domain-facts")
  private val allowedRedistribution = Set("allowed", "derived_only")
  private val allowedDerivedData = Set("allowed")
  private val allowedSourceTypes = Set(
    "public_data_aggregate",
    "curated_public_facts",
    "derived_example_index",
    "ecoTaxonomy",
    "aggregateGameDb",
    "trend_stat",
    "masterGameDb",
    "publicPgn",
    "polyglotBook",
    "curatedLineSet"
  )
  private val allowedOpeningAuthorities = Set("opening_reference", "opening_context", "opening_statistic")
  private val allowedSourceUses = Set("taxonomy_reference", "pipeline_smoke", "online_trend", "master_reference")
  private val allowedAggregateUses = Set("pipeline_smoke", "online_trend_stat", "master_reference_stat")
  private val allowedAliasKinds = Set("display_alias", "context_alias", "structure_alias")
  private val allowedMotifAuthorities = Set("motif_detector", "motif_example")
  private val admittedMotifIds = Set("loose_piece", "pin", "fork", "skewer", "overload", "trapped_piece", "mate_net", "perpetual_check")
  private val deferredMotifIds = Set("discovered_attack", "deflection", "back_rank", "back_rank_mate", "clearance", "interference")
  private val allowedEndgameAuthorities = Set("endgame_study_context")
  private val deferredEndgameStudyIds = Set(
    "outside_passer",
    "fortress_pattern",
    "rook_on_seventh",
    "triangulation",
    "corresponding_squares",
    "shouldering",
    "breakthrough",
    "reserve_tempo"
  )
  private val allowedRetrievalAuthorities = Set("retrieval_example")
  private val allowedRetrievalExampleKinds = Set("curated_reference", "broadcast_game_reference", "puzzle_reference", "study_reference", "educational_reference")
  private val allowedRetrievalSimilarityKinds = Set("exact_position", "opening_context", "motif_context", "endgame_study_context", "mixed_feature_context")
  private val allowedRetrievalSourceQuality = Set("local_curated", "manifest_backed", "attribution_required", "public_fact")
  private val retrievalSimilarityFloor = 0.65
  private val exactPositionSimilarityFloor = 0.95
  private val allowedCandidateKinds = Set("statistical_reference", "context_reference")
  private val forbiddenCandidateWords = Set("be" + "st", "theory", "truth", "forced", "objective", "oracle")
  private val forbiddenAliasWords = Set(
    "best",
    "theory",
    "truth",
    "forced",
    "result",
    "winning",
    "draw",
    "drawn",
    "loss",
    "lost",
    "engine",
    "oracle",
    "solid",
    "bad bishop",
    "refutation",
    "dubious",
    "refuted",
    "trap"
  )
  private val allowedStudyMaterialClasses = Set(
    "rook_pawn_vs_rook",
    "king_pawn_vs_king",
    "bishop_rook_pawn_vs_king",
    "wrong_rook_pawn",
    "fortress_pattern"
  )
  private val allowedEndgameTransforms = Set("mirror_files")
  private val resultWords = Set("win", "draw", "loss", "won", "lost", "forced", "conversion", "convert", "tablebase", "oracle", "truth", "wdl", "dtz", "dtm")
  private val truthBearingLabels = Set("current_position_truth", "current_position_claim", "position_truth")
  private val openingMoveStatFields = Set(
    "id",
    "sourceId",
    "positionKey",
    "move",
    "sampleSize",
    "whiteWins",
    "draws",
    "blackWins",
    "frequency",
    "candidateKind",
    "authority",
    "negativeBoundaries"
  )
  private val openingAliasFields = Set(
    "id",
    "sourceId",
    "sourceName",
    "canonicalName",
    "openingFamily",
    "openingVariation",
    "displayName",
    "contextName",
    "aliasKind",
    "sourceRefs",
    "negativeBoundaries"
  )
  private val retrievalFields = Set(
    "retrievalId",
    "sourceId",
    "sourceRef",
    "exampleKind",
    "fen",
    "positionKey",
    "sideToMove",
    "similarityKey",
    "similarityScore",
    "similarityKind",
    "matchedFeatures",
    "sourceQuality",
    "gameMetadata",
    "licenseNotice",
    "attributionText",
    "snippetRole",
    "currentPositionClaim",
    "authority",
    "tags",
    "negativeBoundaries"
  )
  private val retrievalRejectFields = retrievalFields ++ Set("expectation", "rejectReason")
  private val retrievalCitationFields = Set("players", "event", "date", "round", "result", "url")
  private val retrievalSimilarityFieldsByKind = Map(
    "exact_position" -> Set("positionKey", "materialClass", "openingFamily", "pawnStructure"),
    "opening_context" -> Set("openingFamily", "openingVariation", "pawnStructure", "sourceRefs", "aliasId"),
    "motif_context" -> Set("materialClass", "motifTags", "motifCarriers"),
    "endgame_study_context" -> Set("materialClass", "endgameStudy", "applicabilityRefs"),
    "mixed_feature_context" -> Set("materialClass", "openingFamily", "pawnStructure", "phaseContext", "motifTags", "motifCarriers", "endgameStudy", "applicabilityRefs", "sourceRefs")
  )
  private val retrievalSimilarityStringFields = Set("positionKey", "materialClass", "openingFamily", "openingVariation", "aliasId")
  private val retrievalSimilarityArrayFields = Set("pawnStructure", "sourceRefs", "phaseContext", "motifTags", "motifCarriers", "endgameStudy", "applicabilityRefs")
  private val motifExampleFields = Set(
    "id",
    "motifId",
    "displayName",
    "sourceId",
    "fen",
    "detectorCarrier",
    "involvedSquares",
    "sourceTags",
    "authority",
    "currentPositionClaim",
    "sourceRefs",
    "negativeBoundaries"
  )
  private val motifCarrierFields = Set("ref", "descriptorId", "anchor", "carrierKind")
  private val motifRejectFields = motifExampleFields ++ Set("expectation", "rejectReason")
  private val endgameStudyFields = Set(
    "studyId",
    "displayName",
    "names",
    "materialClass",
    "sideToMove",
    "placementRules",
    "relationRules",
    "allowedTransforms",
    "candidatePlans",
    "sourceRefs",
    "authority",
    "outcomeClaim",
    "negativeBoundaries"
  )

  def sourceFamilyStatus(sourceFamily: String): SourceFamilyStatus =
    if activeFamilies.contains(sourceFamily) then SourceFamilyStatus.Active
    else SourceFamilyStatus.Rejected

  final case class SourceManifestRow(
      sourceId: String,
      sourceFamily: String,
      sourceType: String,
      sourceUse: Option[String],
      license: String,
      redistribution: String,
      derivedData: String,
      attributionRequired: Boolean,
      shareAlikeRequired: Option[Boolean],
      attributionText: Option[String],
      licenseNotice: Option[String],
      sourceUrl: String,
      notes: Option[String],
      aggregateUse: Option[String],
      sourceScope: Option[String],
      playEnvironment: Option[String],
      playerLevel: Option[String],
      ratingSystem: Option[String],
      minElo: Option[Int],
      titlePolicy: Option[String],
      timeScope: Option[String],
      periodStart: Option[String],
      periodEnd: Option[String],
      timeControlScope: Option[String],
      perPositionSampleSizeThreshold: Option[Int],
      dedupePolicy: Option[String],
      annotationPolicy: Option[String],
      sourceYear: Option[Int],
      sourceMonth: Option[Int],
      yearMonth: Option[String],
      ratingBucket: Option[String],
      timeControlBucket: Option[String],
      variant: Option[String],
      ratedFilter: Option[String],
      ratedOnly: Option[Boolean],
      sampleSizeThreshold: Option[Int],
      parserVersion: Option[String],
      rawStoragePolicy: Option[String],
      generatedStoragePolicy: Option[String],
      sourceChecksum: Option[String]
  ):
    def validatedSourceId: String =
      requireToken("Source", sourceId, "sourceId")

    def validatedSourceFamily: String =
      sourceFamilyStatus(sourceFamily) match
        case SourceFamilyStatus.Active => sourceFamily
        case SourceFamilyStatus.Deferred =>
          throw IllegalArgumentException(s"Source $sourceId sourceFamily $sourceFamily is deferred in this contract")
        case SourceFamilyStatus.Rejected =>
          throw IllegalArgumentException(s"Source $sourceId sourceFamily $sourceFamily is unsupported")

    def validatedSourceType: String =
      require(
        allowedSourceTypes.contains(sourceType),
        s"Source $sourceId uses unsupported sourceType $sourceType"
      )
      sourceType

    def validatedSourceUse: Option[String] =
      if sourceFamily == "opening" then
        val value = sourceUse.getOrElse(throw IllegalArgumentException(s"Source $sourceId must declare sourceUse"))
        require(allowedSourceUses.contains(value), s"Source $sourceId uses unsupported sourceUse $value")
        value match
          case "online_trend" =>
            ensure(sourceType == "trend_stat", s"Source $sourceId online_trend sourceUse must use sourceType trend_stat")
          case "master_reference" =>
            ensure(sourceType == "masterGameDb", s"Source $sourceId master_reference sourceUse must use sourceType masterGameDb")
          case "pipeline_smoke" =>
            ensure(sourceType == "aggregateGameDb", s"Source $sourceId pipeline_smoke sourceUse must use sourceType aggregateGameDb")
          case _ => ()
        Some(value)
      else sourceUse

    def validatedLicense: String =
      ensure(
        allowedLicenses.contains(license),
        s"Source $sourceId uses unsupported license $license"
      )
      license

    def validatedRedistribution: String =
      ensure(
        allowedRedistribution.contains(redistribution),
        s"Source $sourceId uses unsupported redistribution $redistribution"
      )
      redistribution

    def validatedDerivedData: String =
      ensure(
        allowedDerivedData.contains(derivedData),
        s"Source $sourceId uses unsupported derivedData $derivedData"
      )
      derivedData

    def validatedAttribution: Boolean =
      if license == "CC-BY-SA-4.0" then
        require(attributionRequired, s"Source $sourceId must require attribution for $license")
        require(shareAlikeRequired.contains(true), s"Source $sourceId must require shareAlike for $license")
      attributionRequired

    def validatedSourceUrl: String =
      ensure(sourceUrl.trim.nonEmpty, s"Source $sourceId must declare a provenance URL")
      require(
        sourceUrl.startsWith("https://") || sourceUrl.startsWith("local-curation:"),
        s"Source $sourceId uses unsupported provenance URL $sourceUrl"
      )
      sourceUrl

    def validatedOpeningAggregateUse: Option[String] =
      validatedSourceUse
      if sourceFamily == "opening" && Set("aggregateGameDb", "trend_stat", "masterGameDb").contains(sourceType) then
        val value = aggregateUse.getOrElse(throw IllegalArgumentException(s"Source $sourceId must declare aggregateUse"))
        ensure(allowedAggregateUses.contains(value), s"Source $sourceId uses unsupported aggregateUse $value")
        if sourceType == "trend_stat" then
          ensure(sourceUse.contains("online_trend"), s"Source $sourceId online trend sourceType must use sourceUse online_trend")
          ensure(value == "online_trend_stat", s"Source $sourceId trend_stat sourceType must use aggregateUse online_trend_stat")
        if sourceType == "aggregateGameDb" then
          ensure(sourceUse.contains("pipeline_smoke"), s"Source $sourceId aggregateGameDb sourceType must use sourceUse pipeline_smoke")
          ensure(value == "pipeline_smoke", s"Source $sourceId aggregateGameDb sourceType must use aggregateUse pipeline_smoke")
        if sourceType == "masterGameDb" then validatedMasterReferenceMetadata
        Some(value)
      else None

    def validatedRetrievalBoundary: Unit =
      if sourceFamily == "retrieval" then
        ensure(sourceType == "derived_example_index", s"Source $sourceId retrieval source must use sourceType derived_example_index")
        ensure(sourceUse.isEmpty, s"Source $sourceId retrieval source must not declare sourceUse")
        ensure(aggregateUse.isEmpty, s"Source $sourceId retrieval source must not declare aggregateUse")

    def validatedTrendMetadata: Option[String] =
      if sourceType == "trend_stat" || aggregateUse.contains("online_trend_stat") then
        if sourceType == "trend_stat" then validatedOpeningAggregateUse
        val year = sourceYear.getOrElse(throw IllegalArgumentException(s"Source $sourceId trend_stat must declare sourceYear"))
        val month = sourceMonth.getOrElse(throw IllegalArgumentException(s"Source $sourceId trend_stat must declare sourceMonth"))
        ensure(year >= 2024, s"Source $sourceId trend_stat sourceYear must be recent")
        ensure(month >= 1 && month <= 12, s"Source $sourceId trend_stat sourceMonth out of range")
        ensure(yearMonth.contains(f"$year%04d-$month%02d"), s"Source $sourceId trend_stat must declare yearMonth matching sourceYear/sourceMonth")
        requirePresent(ratingBucket, s"Source $sourceId trend_stat must declare ratingBucket")
        requirePresent(timeControlBucket, s"Source $sourceId trend_stat must declare timeControlBucket")
        ensure(variant.contains("standard"), s"Source $sourceId trend_stat variant must be standard")
        requirePresent(ratedFilter, s"Source $sourceId trend_stat must declare ratedFilter")
        ensure(ratedOnly.contains(true), s"Source $sourceId trend_stat must declare ratedOnly true")
        ensure(sampleSizeThreshold.exists(_ > 0), s"Source $sourceId trend_stat must declare positive sampleSizeThreshold")
        requirePresent(parserVersion, s"Source $sourceId trend_stat must declare parserVersion")
        ensure(generatedStoragePolicy.contains("localOnly"), s"Source $sourceId trend output must stay local-only")
        requirePresent(sourceChecksum, s"Source $sourceId trend_stat must declare sourceChecksum")
        Some("online_trend_stat")
      else None

    def validatedMasterReferenceMetadata: Option[String] =
      if sourceUse.contains("master_reference") || aggregateUse.contains("master_reference_stat") then
        ensure(sourceType == "masterGameDb", s"Source $sourceId master_reference must use sourceType masterGameDb")
        ensure(aggregateUse.contains("master_reference_stat"), s"Source $sourceId master_reference must use aggregateUse master_reference_stat")
        ensure(sourceUse.contains("master_reference"), s"Source $sourceId must declare sourceUse master_reference")
        ensure(license == "CC-BY-SA-4.0", s"Source $sourceId master_reference must use CC-BY-SA-4.0")
        ensure(attributionRequired, s"Source $sourceId master_reference must require attribution")
        ensure(shareAlikeRequired.contains(true), s"Source $sourceId master_reference must require shareAlike")
        requirePresent(attributionText, s"Source $sourceId master_reference must declare attributionText")
        requirePresent(licenseNotice, s"Source $sourceId master_reference must declare licenseNotice")
        requirePresent(sourceScope, s"Source $sourceId master_reference must declare sourceScope")
        val environment = requirePresent(playEnvironment, s"Source $sourceId master_reference must declare playEnvironment")
        if environment == "mixed" then ensure(sourceScope.contains("mixed"), s"Source $sourceId mixed playEnvironment requires explicit mixed scope")
        requirePresent(playerLevel, s"Source $sourceId master_reference must declare playerLevel")
        requirePresent(ratingSystem, s"Source $sourceId master_reference must declare ratingSystem")
        ensure(titlePolicy.exists(_.trim.nonEmpty) || minElo.exists(_ > 0), s"Source $sourceId master_reference must declare titlePolicy or minElo")
        requirePresent(timeScope, s"Source $sourceId master_reference must declare timeScope")
        requirePresent(periodStart, s"Source $sourceId master_reference must declare periodStart")
        requirePresent(periodEnd, s"Source $sourceId master_reference must declare periodEnd")
        val tc = requirePresent(timeControlScope, s"Source $sourceId master_reference must declare timeControlScope")
        ensure(tc == "classical_rapid", s"Source $sourceId master_reference timeControlScope must be classical_rapid")
        ensure(perPositionSampleSizeThreshold.exists(_ > 0), s"Source $sourceId master_reference must declare positive perPositionSampleSizeThreshold")
        requirePresent(dedupePolicy, s"Source $sourceId master_reference must declare dedupePolicy")
        requirePresent(annotationPolicy, s"Source $sourceId master_reference must declare annotationPolicy")
        requirePresent(parserVersion, s"Source $sourceId master_reference must declare parserVersion")
        ensure(rawStoragePolicy.contains("localOnly"), s"Source $sourceId master_reference raw artifacts must stay local-only")
        ensure(generatedStoragePolicy.contains("localOnly"), s"Source $sourceId master_reference generated artifacts must stay local-only")
        requirePresent(sourceChecksum, s"Source $sourceId master_reference must declare sourceChecksum")
        Some("master_reference_stat")
      else None

  final case class OpeningLine(
      id: String,
      sourceId: String,
      eco: String,
      family: String,
      variation: String,
      names: List[String],
      moveOrder: List[String],
      lineKey: String,
      authority: String,
      negativeBoundaries: List[String]
  ):
    def validatedSourceId(manifests: Iterable[SourceManifestRow]): String =
      val source = requireSource(sourceId, "opening", manifests)
      val manifest = manifests.find(_.sourceId == sourceId).get
      require(manifest.sourceUse.contains("taxonomy_reference"), s"Opening line $id source must use taxonomy_reference")
      source

    def validatedEco: String =
      require(eco.matches("^[A-E][0-9]{2}$"), s"Opening line $id uses invalid ECO $eco")
      eco

    def validatedName: String =
      require(names.nonEmpty, s"Opening line $id must declare at least one name")
      require(names.forall(_.trim.nonEmpty), s"Opening line $id has an empty opening name")
      require(family.trim.nonEmpty, s"Opening line $id must declare family")
      require(variation.trim.nonEmpty, s"Opening line $id must declare variation")
      names.head

    def validatedMoveOrder: Vector[String] =
      validateMoveOrder("Opening line", id, moveOrder)

    def validatedAuthority: String =
      require(
        allowedOpeningAuthorities.contains(authority) && authority == "opening_reference",
        s"Opening line $id uses unsupported authority $authority"
      )
      authority

    def validatedNegativeBoundaries: Vector[String] =
      validateTokenList("Opening line", id, "negativeBoundaries", negativeBoundaries)

  final case class OpeningPosition(
      id: String,
      sourceId: String,
      lineId: String,
      eco: String,
      name: String,
      positionKey: String,
      fen: String,
      ply: Int,
      moveOrder: List[String],
      transpositionState: String,
      status: String,
      authority: String,
      contextRefs: List[String]
  ):
    def validatedSourceId(manifests: Iterable[SourceManifestRow]): String =
      requireSource(sourceId, "opening", manifests)

    def validatedLineId(lines: Iterable[OpeningLine]): String =
      val line = lines.find(_.id == lineId).getOrElse(
        throw IllegalArgumentException(s"Opening position $id references unknown lineId $lineId")
      )
      ensure(
        line.eco == eco && line.names.contains(name),
        s"Opening position $id metadata must match lineId $lineId"
      )
      lineId

    def validatedPositionKey: String =
      ensure(positionKey.trim.nonEmpty, s"Opening position $id must declare normalized positionKey")
      require(positionKey.startsWith("std:"), s"Opening position $id uses unsupported positionKey $positionKey")
      ensure(positionKey == positionKeyFromFen(fen), s"Opening position $id positionKey must match normalized FEN key")
      positionKey

    def validatedFen: Fen.Full =
      Fen.Full.clean(fen)

    def validatedMoveOrder: Vector[String] =
      validateMoveOrder("Opening position", id, moveOrder)

    def validatedTransposition: String =
      require(
        Set("canonical", "transposed", "ambiguous").contains(transpositionState),
        s"Opening position $id uses unsupported transpositionState $transpositionState"
      )
      require(Set("indexed", "downgraded").contains(status), s"Opening position $id uses unsupported status $status")
      if transpositionState == "ambiguous" then
        ensure(status == "downgraded", s"Opening position $id ambiguous transpositions must be downgraded")
      status

    def validatedAuthority: String =
      require(
        allowedOpeningAuthorities.contains(authority) && authority == "opening_context",
        s"Opening position $id uses unsupported authority $authority"
      )
      authority

  final case class OpeningMoveStat(
      id: String,
      sourceId: String,
      positionKey: String,
      move: String,
      sampleSize: Int,
      whiteWins: Int,
      draws: Int,
      blackWins: Int,
      frequency: Double,
      candidateKind: String,
      authority: String,
      negativeBoundaries: List[String]
  ):
    def validatedSourceId(manifests: Iterable[SourceManifestRow]): String =
      requireSource(sourceId, "opening", manifests)

    def validatedPositionKey(positions: Iterable[OpeningPosition]): String =
      ensure(
        positions.exists(position => position.positionKey == positionKey && position.status == "indexed"),
        s"Opening move stat $id references no indexed positionKey"
      )
      positionKey

    def validatedMove: String =
      requireUciMove("Opening move stat", id, move)

    def validatedCounts: Int =
      require(sampleSize > 0, s"Opening move stat $id must have positive sampleSize")
      require(
        whiteWins >= 0 && draws >= 0 && blackWins >= 0,
        s"Opening move stat $id cannot use negative result counts"
      )
      require(
        whiteWins + draws + blackWins == sampleSize,
        s"Opening move stat $id result counts must sum to sampleSize"
      )
      require(frequency >= 0.0 && frequency <= 1.0, s"Opening move stat $id frequency out of range")
      sampleSize

    def validatedAuthority: String =
      require(
        allowedOpeningAuthorities.contains(authority) && authority == "opening_statistic",
        s"Opening move stat $id uses unsupported authority $authority"
      )
      authority

    def validatedCandidateKind: String =
      val normalized = candidateKind.toLowerCase
      if forbiddenCandidateWords.exists(normalized.contains) then
        throw IllegalArgumentException(s"Opening move stat $id uses forbidden candidateKind")
      require(
        allowedCandidateKinds.contains(candidateKind),
        s"Opening move stat $id uses unsupported candidateKind $candidateKind"
      )
      candidateKind

  final case class OpeningTheme(
      id: String,
      sourceId: String,
      positionKey: String,
      themeId: String,
      family: String,
      contextTags: List[String],
      planRefs: List[String],
      breakRefs: List[String],
      authority: String,
      negativeBoundaries: List[String]
  ):
    def validatedSourceId(manifests: Iterable[SourceManifestRow]): String =
      requireSource(sourceId, "opening", manifests)

    def validatedPositionKey(positions: Iterable[OpeningPosition]): String =
      ensure(
        positions.exists(position => position.positionKey == positionKey && position.status == "indexed"),
        s"Opening theme $id references no indexed positionKey"
      )
      positionKey

    def validatedTheme: String =
      requireToken("Opening theme", themeId, "themeId")
      validateTokenList("Opening theme", id, "contextTags", contextTags)
      validateTokenList("Opening theme", id, "planRefs", planRefs)
      validateTokenList("Opening theme", id, "breakRefs", breakRefs)
      themeId

    def validatedAuthority: String =
      require(
        allowedOpeningAuthorities.contains(authority) && authority == "opening_context",
        s"Opening theme $id uses unsupported authority $authority"
      )
      authority

  final case class OpeningAlias(
      id: String,
      sourceId: String,
      sourceName: String,
      canonicalName: String,
      openingFamily: String,
      openingVariation: String,
      displayName: String,
      contextName: String,
      aliasKind: String,
      sourceRefs: List[String],
      negativeBoundaries: List[String]
  ):
    def validatedSourceId(manifests: Iterable[SourceManifestRow]): String =
      val source = requireSource(sourceId, "opening", manifests)
      val manifest = manifests.find(_.sourceId == sourceId).get
      ensure(manifest.sourceUse.contains("taxonomy_reference"), s"Opening alias $id source must use taxonomy_reference")
      source

    def validatedAliasKind: String =
      ensure(allowedAliasKinds.contains(aliasKind), s"Opening alias $id uses unsupported aliasKind $aliasKind")
      aliasKind

    def validatedSourceTaxonomy(lines: Iterable[OpeningLine]): String =
      val refs = validatedSourceRefs
      val lineId = refs.head
      val line = lines.find(_.id == lineId).getOrElse(throw IllegalArgumentException(s"Opening alias $id references unknown taxonomy row $lineId"))
      ensure(line.names.contains(sourceName), s"Opening alias $id sourceName must match taxonomy row")
      ensure(canonicalName == sourceName, s"Opening alias $id canonicalName must match source taxonomy")
      ensure(openingFamily == line.family, s"Opening alias $id openingFamily must match taxonomy row")
      ensure(line.variation.contains(openingVariation) || openingVariation.contains(line.variation) || openingVariation == line.variation, s"Opening alias $id openingVariation must match taxonomy row")
      validateOpeningAliasSupport(id, openingFamily, displayName, contextName, openingVariation)
      lineId

    def validatedDisplayBoundary: String =
      ensure(displayName.trim.nonEmpty && contextName.trim.nonEmpty, s"Opening alias $id must declare display/context text")
      ensure(
        !containsForbiddenAliasWording(displayName) && !containsForbiddenAliasWording(contextName),
        s"Opening alias $id contains authority wording"
      )
      displayName

    def validatedSourceRefs: Vector[String] =
      val refs = validateTokenList("Opening alias", id, "sourceRefs", sourceRefs)
      ensure(refs.size == 1, s"Opening alias $id must bind exactly one taxonomy sourceRef")
      refs

    def validatedNegativeBoundaries: Vector[String] =
      validateTokenList("Opening alias", id, "negativeBoundaries", negativeBoundaries)

  final case class MotifCarrierRef(ref: String, descriptorId: String, anchor: String, carrierKind: String):
    def validated(rowId: String, motifId: String): MotifCarrierRef =
      ensure(ref == s"motif-detector-carrier:$rowId", s"Motif example $rowId detector carrier ref must match row id")
      requireToken("Motif detector", descriptorId, "descriptorId")
      require(anchor.trim.nonEmpty, s"Motif example $rowId detector carrier must declare anchor")
      require(
        Set("u_witness", "root_atom", "certified_line").contains(carrierKind),
        s"Motif example $rowId uses unsupported detector carrierKind $carrierKind"
      )
      val allowedCarriers =
        motifId match
          case "loose_piece"      => Set("loose_piece_target_state")
          case "mate_net"         => Set("MateNetCertification")
          case "perpetual_check"  => Set("PerpetualCheckHolding")
          case other              => Set(other)
      ensure(
        allowedCarriers.contains(descriptorId),
        s"Motif example $rowId detector carrier must match motifId $motifId"
      )
      if Set("mate_net", "perpetual_check").contains(motifId) then
        ensure(carrierKind == "certified_line", s"Motif example $rowId detector carrier must match motifId $motifId")
      this

  final case class MotifExample(
      id: String,
      motifId: String,
      displayName: String,
      sourceId: String,
      fen: String,
      detectorCarrier: Option[MotifCarrierRef],
      involvedSquares: List[String],
      sourceTags: List[String],
      authority: String,
      currentPositionClaim: Boolean,
      sourceRefs: List[String],
      negativeBoundaries: List[String]
  ):
    def validatedContract: MotifExample =
      validatedMotifId
      validatedDisplayName
      validatedFen
      validatedDetectorCarrier
      validatedInvolvedSquares
      validatedSourceTags
      validatedAuthority
      validatedCurrentPositionBoundary
      validatedSourceRefs
      validatedNegativeBoundaries
      this

    def validatedSourceId(manifests: Iterable[SourceManifestRow]): String =
      requireSource(sourceId, "motif", manifests)

    def validatedMotifId: String =
      requireToken("Motif example", motifId, "motifId")
      ensure(!isTruthBearingLabel(motifId), s"Motif example $id motifId contains truth-bearing label")
      ensure(!deferredMotifIds.contains(motifId), s"Motif example $id motifId $motifId requires future exact-board detector")
      ensure(admittedMotifIds.contains(motifId), s"Motif example $id uses unsupported motifId $motifId")
      motifId

    def validatedDisplayName: String =
      requireDisplayText("Motif example", id, "displayName", displayName, containsForbiddenMotifWording)

    def validatedFen: Fen.Full =
      Fen.Full.clean(fen)

    def validatedDetectorCarrier: MotifCarrierRef =
      validatedMotifId
      detectorCarrier
        .getOrElse(throw IllegalArgumentException(s"Motif example $id must bind an exact-board detector carrier"))
        .validated(id, motifId)

    def validatedInvolvedSquares: Vector[String] =
      validateTokenList("Motif example", id, "involvedSquares", involvedSquares)

    def validatedSourceTags: Vector[String] =
      val tags = validateTokenList("Motif example", id, "sourceTags", sourceTags)
      ensure(
        !tags.exists(isTruthBearingLabel),
        s"Motif example $id sourceTags contain truth-bearing labels"
      )
      tags

    def validatedSourceRefs: Vector[String] =
      val refs = validateTokenList("Motif example", id, "sourceRefs", sourceRefs)
      ensure(refs == Vector(s"motif-example:$id"), s"Motif example $id sourceRefs must bind exactly motif-example:$id")
      refs

    def validatedAuthority: String =
      require(allowedMotifAuthorities.contains(authority), s"Motif example $id uses unsupported authority $authority")
      authority

    def validatedCurrentPositionBoundary: Boolean =
      require(!currentPositionClaim, s"Motif example $id must not become a current-position claim")
      currentPositionClaim

    def validatedNegativeBoundaries: Vector[String] =
      validateTokenList("Motif example", id, "negativeBoundaries", negativeBoundaries)

  final case class MotifRejectFixture(
      id: String,
      sourceId: String,
      motifId: String,
      fen: String,
      displayName: Option[String],
      detectorCarrier: Option[MotifCarrierRef],
      involvedSquares: List[String],
      sourceTags: List[String],
      authority: Option[String],
      currentPositionClaim: Option[Boolean],
      sourceRefs: List[String],
      expectation: String,
      rejectReason: String,
      negativeBoundaries: List[String]
  ):
    def validatedRejectReason: String =
      require(expectation == "rejected", s"Motif reject fixture $id must be rejected")
      requireToken("Motif reject fixture", rejectReason, "rejectReason")

    def asExampleCandidate: MotifExample =
      MotifExample(
        id = id,
        motifId = motifId,
        displayName = displayName.getOrElse(motifId),
        sourceId = sourceId,
        fen = fen,
        detectorCarrier = detectorCarrier,
        involvedSquares = involvedSquares,
        sourceTags = sourceTags,
        authority = authority.getOrElse("motif_example"),
        currentPositionClaim = currentPositionClaim.getOrElse(false),
        sourceRefs = if sourceRefs.nonEmpty then sourceRefs else List(s"motif-example:$id"),
        negativeBoundaries = negativeBoundaries
      )

  final case class EndgameStudy(
      studyId: String,
      displayName: String,
      names: List[String],
      materialClass: String,
      sideToMove: List[String],
      placementRules: List[String],
      relationRules: List[String],
      allowedTransforms: List[String],
      candidatePlans: List[String],
      sourceRefs: List[String],
      authority: String,
      outcomeClaim: String,
      negativeBoundaries: List[String]
  ):
    def validatedSourceId(manifests: Iterable[SourceManifestRow]): Vector[String] =
      sourceRefs.map(ref => requireSource(ref, "endgameStudy", manifests)).toVector

    def validatedStudyId: String =
      requireToken("Endgame study", studyId, "studyId")
      ensure(!deferredEndgameStudyIds.contains(studyId), s"Endgame study $studyId requires future exact-board certification")
      studyId

    def validatedDisplayName: String =
      requireDisplayText("Endgame study", studyId, "displayName", displayName, containsForbiddenEndgameWording)

    def validatedNames: Vector[String] =
      val values = names.map(_.trim).filter(_.nonEmpty).toVector
      require(values.nonEmpty, s"Endgame study $studyId must declare at least one name")
      val leaks = values.filter(containsForbiddenEndgameWording)
      ensure(leaks.isEmpty, s"Endgame study $studyId names contain forbidden truth wording: ${leaks.mkString(", ")}")
      values

    def validatedMaterialClass: String =
      require(
        allowedStudyMaterialClasses.contains(materialClass),
        s"Endgame study $studyId uses unsupported materialClass $materialClass"
      )
      materialClass

    def validatedSideToMove: Vector[String] =
      val values = validateTokenList("Endgame study", studyId, "sideToMove", sideToMove)
      require(
        values.forall(Set("stronger", "defender", "white", "black").contains),
        s"Endgame study $studyId uses unsupported sideToMove ${values.mkString(", ")}"
      )
      values

    def validatedPlacementRules: Vector[String] =
      val placement = validateTokenList("Endgame study", studyId, "placementRules", placementRules)
      val relations = validateTokenList("Endgame study", studyId, "relationRules", relationRules)
      if studyId.toLowerCase.contains("lucena") then
        val requiredPlacement = Set("strong_king_near_pawn", "pawn_on_seventh", "defender_king_cut_off")
        val requiredRelations = Set("rook_check_shelter")
        ensure(
          requiredPlacement.subsetOf(placement.toSet) && requiredRelations.subsetOf(relations.toSet),
          s"Endgame study $studyId Lucena context lacks required applicability rules"
        )
      if studyId.toLowerCase.contains("philidor") then
        val requiredPlacement = Set("defender_king_in_front", "attacker_pawn_not_on_sixth")
        val requiredRelations = Set("defender_rook_on_third_rank")
        ensure(
          requiredPlacement.subsetOf(placement.toSet) && requiredRelations.subsetOf(relations.toSet),
          s"Endgame study $studyId Philidor context lacks required defender relation rules"
        )
      if studyId.toLowerCase.contains("vancura") then
        val requiredPlacement = Set("rook_pawn_on_sixth_or_seventh", "defender_king_near_corner")
        val requiredRelations = Set("defender_rook_lateral_checking_file")
        ensure(
          requiredPlacement.subsetOf(placement.toSet) && requiredRelations.subsetOf(relations.toSet),
          s"Endgame study $studyId Vancura context lacks required applicability rules"
        )
      placement

    def validatedAllowedTransforms: Vector[String] =
      val transforms = validateTokenList("Endgame study", studyId, "allowedTransforms", allowedTransforms)
      transforms.foreach(transform => ensure(allowedEndgameTransforms.contains(transform), s"Endgame study $studyId uses unsupported allowedTransforms $transform"))
      transforms

    def validatedCandidatePlans: Vector[String] =
      val values = validateTokenList("Endgame study", studyId, "candidatePlans", candidatePlans)
      val leaks = values.filter(value => resultWords.exists(word => value.toLowerCase.contains(word)))
      ensure(leaks.isEmpty, s"Endgame study $studyId candidatePlans contain result claims: ${leaks.mkString(", ")}")
      values

    def validatedContextBoundary: String =
      require(
        allowedEndgameAuthorities.contains(authority) && authority == "endgame_study_context",
        s"Endgame study $studyId uses unsupported authority $authority"
      )
      ensure(outcomeClaim == "none", s"Endgame study $studyId must not declare outcomeClaim $outcomeClaim")
      outcomeClaim

    def validatedNegativeBoundaries: Vector[String] =
      validateTokenList("Endgame study", studyId, "negativeBoundaries", negativeBoundaries)

  final case class EndgameStudyFixture(
      id: String,
      studyId: String,
      fen: String,
      materialClass: String,
      sideToMove: String,
      placementEvidence: List[String],
      relationEvidence: List[String],
      contextStatus: String,
      outcomeClaim: String,
      sourceRefs: List[String]
  ):
    def validatedStudyId(studies: Iterable[EndgameStudy]): String =
      require(studies.exists(_.studyId == studyId), s"Endgame fixture $id references unknown studyId $studyId")
      studyId

    def validatedFen: Fen.Full =
      Fen.Full.clean(fen)

    def validatedMaterialClass(studies: Iterable[EndgameStudy]): String =
      val study = findStudy(studies)
      require(materialClass == study.materialClass, s"Endgame fixture $id materialClass mismatch")
      require(materialClassFromFen(fen) == materialClass, s"Endgame fixture $id FEN does not match $materialClass")
      materialClass

    def validatedApplicability(studies: Iterable[EndgameStudy]): String =
      val study = findStudy(studies)
      require(contextStatus == "matches", s"Endgame fixture $id must be a matching fixture")
      ensure(study.validatedSideToMove.contains(sideToMove), s"Endgame fixture $id sideToMove is not allowed by $studyId")
      ensure(sideToMove == endgameSideToMoveRoleFromFen(fen), s"Endgame fixture $id sideToMove must match exact board")
      require(
        study.placementRules.toSet.subsetOf(placementEvidence.toSet),
        s"Endgame fixture $id missing placement evidence for $studyId"
      )
      require(
        study.relationRules.toSet.subsetOf(relationEvidence.toSet),
        s"Endgame fixture $id missing relation evidence for $studyId"
      )
      val boardEvidence = EndgameBoardEvidence.fromFen(fen)
      ensure(
        study.placementRules.toSet.subsetOf(boardEvidence.placementEvidence),
        s"Endgame fixture $id placement evidence does not match exact board"
      )
      ensure(
        study.relationRules.toSet.subsetOf(boardEvidence.relationEvidence),
        s"Endgame fixture $id relation evidence does not match exact board"
      )
      contextStatus

    def validatedContextBoundary: String =
      require(outcomeClaim == "none", s"Endgame fixture $id must not declare outcomeClaim $outcomeClaim")
      outcomeClaim

    def validatedSourceRefs: Vector[String] =
      val refs = validateTokenList("Endgame fixture", id, "sourceRefs", sourceRefs)
      ensure(refs.contains(s"endgame-study:$studyId:applicable"), s"Endgame fixture $id must bind study applicability sourceRef")
      ensure(refs.contains(s"endgame-study-applicability:$id"), s"Endgame fixture $id must bind exact applicability evidence ref")
      refs

    private def findStudy(studies: Iterable[EndgameStudy]): EndgameStudy =
      studies.find(_.studyId == studyId).getOrElse(throw IllegalArgumentException(s"Unknown studyId $studyId"))

  final case class EndgameStudyRejectFixture(
      id: String,
      studyId: String,
      fen: String,
      materialClass: String,
      expectation: String,
      rejectReason: String,
      negativeBoundaries: List[String]
  ):
    def validatedStudyId(studies: Iterable[EndgameStudy]): String =
      require(studies.exists(_.studyId == studyId), s"Endgame reject fixture $id references unknown studyId $studyId")
      studyId

    def validatedFen: Fen.Full =
      Fen.Full.clean(fen)

    def validatedRejectReason: String =
      require(expectation == "rejected", s"Endgame reject fixture $id must be rejected")
      requireToken("Endgame reject fixture", rejectReason, "rejectReason")

    def validatedApplicabilityRejected(studies: Iterable[EndgameStudy]): String =
      val study = studies.find(_.studyId == studyId).getOrElse(
        throw IllegalArgumentException(s"Endgame reject fixture $id references unknown studyId $studyId")
      )
      val boardEvidence = EndgameBoardEvidence.fromFen(fen)
      ensure(
        !study.placementRules.toSet.subsetOf(boardEvidence.placementEvidence) ||
          !study.relationRules.toSet.subsetOf(boardEvidence.relationEvidence),
        s"Endgame reject fixture $id unexpectedly satisfies study applicability"
      )
      rejectReason

  final case class RetrievalSimilarityKey(fields: JsObject):
    def validated(
        rowId: String,
        similarityKind: String,
        rowPositionKey: String,
        rowFen: String,
        motifCarrierRefs: Set[String] = Set.empty,
        endgameApplicabilityRefs: Set[String] = Set.empty,
        motifCarrierBindings: Map[String, (String, String)] = Map.empty,
        endgameApplicabilityBindings: Map[String, (String, String)] = Map.empty
    ): JsObject =
      require(fields.keys.nonEmpty, s"Retrieval example $rowId must declare similarityKey")
      val allowedFields = retrievalSimilarityFieldsByKind.getOrElse(
        similarityKind,
        throw IllegalArgumentException(s"Retrieval example $rowId uses unsupported similarityKind $similarityKind")
      )
      val unknownFields = fields.keys.filterNot(allowedFields.contains).toVector.sorted
      ensure(unknownFields.isEmpty, s"Retrieval example $rowId similarityKey uses unsupported fields: ${unknownFields.mkString(", ")}")
      fields.fields.foreach: (key, value) =>
        if retrievalSimilarityStringFields.contains(key) then
          require(value.asOpt[String].exists(_.trim.nonEmpty), s"Retrieval example $rowId similarityKey field $key must be a string")
        if retrievalSimilarityArrayFields.contains(key) then
          val values = value.asOpt[Vector[String]].getOrElse(throw IllegalArgumentException(s"Retrieval example $rowId similarityKey field $key must be a string array"))
          ensure(values.nonEmpty && values.forall(_.trim.nonEmpty), s"Retrieval example $rowId similarityKey field $key must be a non-empty string array")
      ensure(
        !fields.fields.exists((key, value) => containsForbiddenRetrievalWording(key) || jsonContainsForbiddenRetrievalWording(value)),
        s"Retrieval example $rowId similarityKey contains forbidden truth wording"
      )
      fields.value.get("materialClass").foreach: value =>
        val materialClass = value.as[String]
        ensure(materialClass == materialClassFromFen(rowFen), s"Retrieval example $rowId materialClass must match exact example FEN")
      similarityKind match
        case "exact_position" =>
          val key = requireJsonString(fields, "positionKey", s"Retrieval example $rowId exact_position similarity requires positionKey")
          ensure(key == rowPositionKey, s"Retrieval example $rowId exact_position similarityKey must match row positionKey")
          validateOpeningFamilyIfPresent(rowId, rowFen)
        case "opening_context" =>
          val family = requireJsonString(fields, "openingFamily", s"Retrieval example $rowId opening_context similarity requires openingFamily")
          ensure(
            openingFamilyEvidenceFromFen(rowFen).contains(family),
            s"Retrieval example $rowId opening similarity family must match exact example FEN"
          )
          val structures = requireJsonStringArray(fields, "pawnStructure", s"Retrieval example $rowId opening_context similarity requires pawnStructure")
          val evidence = pawnStructureEvidenceFromFen(rowFen)
          ensure(
            structures.forall(evidence.contains),
            s"Retrieval example $rowId opening_context pawnStructure must match exact example FEN"
          )
        case "motif_context" =>
          val tags = fields.value.get("motifTags").flatMap(_.asOpt[Vector[String]]).getOrElse(Vector.empty)
          val carriers = requireJsonStringArray(fields, "motifCarriers", s"Retrieval example $rowId motif_context similarity requires motifCarriers")
          ensure(carriers.forall(_.startsWith("motif-detector-carrier:")), s"Retrieval example $rowId motif_context requires exact motif detector carriers")
          if motifCarrierRefs.nonEmpty then
            ensure(carriers.forall(motifCarrierRefs.contains), s"Retrieval example $rowId motif_context references unknown motif detector carrier")
          if motifCarrierBindings.nonEmpty then
            ensure(
              carriers.forall: ref =>
                motifCarrierBindings.get(ref).exists: (motifId, carrierFen) =>
                  tags.contains(motifId) && positionKeyFromFen(carrierFen) == rowPositionKey,
              s"Retrieval example $rowId motif_context carrier must match motif tag and exact example FEN"
            )
        case "endgame_study_context" =>
          val studies = requireJsonStringArray(fields, "endgameStudy", s"Retrieval example $rowId endgame_study_context similarity requires endgameStudy")
          val refs = requireJsonStringArray(fields, "applicabilityRefs", s"Retrieval example $rowId endgame_study_context similarity requires applicabilityRefs")
          ensure(refs.exists(_.startsWith("endgame-study-applicability:")), s"Retrieval example $rowId endgame_study_context requires applicability evidence ref")
          if endgameApplicabilityRefs.nonEmpty then
            ensure(refs.forall(endgameApplicabilityRefs.contains), s"Retrieval example $rowId endgame_study_context references unknown applicability evidence ref")
          if endgameApplicabilityBindings.nonEmpty then
            ensure(
              refs.forall: ref =>
                endgameApplicabilityBindings.get(ref).exists: (studyId, fixtureFen) =>
                  studies.contains(studyId) && positionKeyFromFen(fixtureFen) == rowPositionKey,
              s"Retrieval example $rowId endgame_study_context applicability must match study tag and exact example FEN"
            )
        case "mixed_feature_context" =>
          ensure(mixedFeatureFamilies.size >= 2, s"Retrieval example $rowId mixed_feature_context requires at least two feature families")
          validateOpeningFamilyIfPresent(rowId, rowFen)
          validateMotifFamilyIfPresent(rowId, rowPositionKey, motifCarrierRefs, motifCarrierBindings)
          validateEndgameFamilyIfPresent(rowId, rowPositionKey, endgameApplicabilityRefs, endgameApplicabilityBindings)
        case other =>
          throw IllegalArgumentException(s"Retrieval example $rowId uses unsupported similarityKind $other")
      fields

    private def mixedFeatureFamilies: Set[String] =
      Set(
        Option.when(fields.value.contains("materialClass"))("material"),
        Option.when(fields.value.contains("phaseContext"))("phase"),
        Option.when(Vector("openingFamily", "openingVariation", "aliasId", "pawnStructure", "sourceRefs").exists(fields.value.contains))("opening"),
        Option.when(Vector("motifTags", "motifCarriers").exists(fields.value.contains))("motif"),
        Option.when(Vector("endgameStudy", "applicabilityRefs").exists(fields.value.contains))("endgameStudy")
      ).flatten

    private def validateOpeningFamilyIfPresent(rowId: String, rowFen: String): Unit =
      if Vector("openingFamily", "openingVariation", "aliasId", "pawnStructure", "sourceRefs").exists(fields.value.contains) then
        fields.value.get("openingFamily").foreach: value =>
          val family = value.as[String]
          ensure(
            openingFamilyEvidenceFromFen(rowFen).contains(family),
            s"Retrieval example $rowId opening similarity family must match exact example FEN"
          )
        val structures = requireJsonStringArray(fields, "pawnStructure", s"Retrieval example $rowId opening similarity requires pawnStructure")
        val evidence = pawnStructureEvidenceFromFen(rowFen)
        ensure(
          structures.forall(evidence.contains),
          s"Retrieval example $rowId opening similarity pawnStructure must match exact example FEN"
        )

    private def validateMotifFamilyIfPresent(
        rowId: String,
        rowPositionKey: String,
        motifCarrierRefs: Set[String],
        motifCarrierBindings: Map[String, (String, String)]
    ): Unit =
      if Vector("motifTags", "motifCarriers").exists(fields.value.contains) then
        val tags = requireJsonStringArray(fields, "motifTags", s"Retrieval example $rowId motif similarity requires motifTags")
        val carriers = requireJsonStringArray(fields, "motifCarriers", s"Retrieval example $rowId motif similarity requires motifCarriers")
        ensure(carriers.forall(_.startsWith("motif-detector-carrier:")), s"Retrieval example $rowId motif similarity requires exact motif detector carriers")
        if motifCarrierRefs.nonEmpty then
          ensure(carriers.forall(motifCarrierRefs.contains), s"Retrieval example $rowId motif similarity references unknown motif detector carrier")
        if motifCarrierBindings.nonEmpty then
          ensure(
            carriers.forall: ref =>
              motifCarrierBindings.get(ref).exists: (motifId, carrierFen) =>
                tags.contains(motifId) && positionKeyFromFen(carrierFen) == rowPositionKey,
            s"Retrieval example $rowId motif similarity carrier must match motif tag and exact example FEN"
          )

    private def validateEndgameFamilyIfPresent(
        rowId: String,
        rowPositionKey: String,
        endgameApplicabilityRefs: Set[String],
        endgameApplicabilityBindings: Map[String, (String, String)]
    ): Unit =
      if Vector("endgameStudy", "applicabilityRefs").exists(fields.value.contains) then
        val studies = requireJsonStringArray(fields, "endgameStudy", s"Retrieval example $rowId endgame similarity requires endgameStudy")
        val refs = requireJsonStringArray(fields, "applicabilityRefs", s"Retrieval example $rowId endgame similarity requires applicabilityRefs")
        ensure(refs.exists(_.startsWith("endgame-study-applicability:")), s"Retrieval example $rowId endgame similarity requires applicability evidence ref")
        if endgameApplicabilityRefs.nonEmpty then
          ensure(refs.forall(endgameApplicabilityRefs.contains), s"Retrieval example $rowId endgame similarity references unknown applicability evidence ref")
        if endgameApplicabilityBindings.nonEmpty then
          ensure(
            refs.forall: ref =>
              endgameApplicabilityBindings.get(ref).exists: (studyId, fixtureFen) =>
                studies.contains(studyId) && positionKeyFromFen(fixtureFen) == rowPositionKey,
            s"Retrieval example $rowId endgame similarity applicability must match study tag and exact example FEN"
          )

  final case class RetrievalCitation(
      players: List[String],
      event: Option[String],
      date: Option[String],
      round: Option[String],
      result: Option[String],
      url: Option[String]
  ):
    def hasMetadata: Boolean =
      players.exists(_.trim.nonEmpty) || event.exists(_.trim.nonEmpty) || date.exists(_.trim.nonEmpty) ||
        round.exists(_.trim.nonEmpty) || result.exists(_.trim.nonEmpty) || url.exists(_.trim.nonEmpty)

    def validated(rowId: String): RetrievalCitation =
      players.foreach(player => requireDisplayText("Retrieval citation", rowId, "players", player, containsForbiddenRetrievalWording))
      event.foreach(value => requireDisplayText("Retrieval citation", rowId, "event", value, containsForbiddenRetrievalWording))
      date.foreach: value =>
        require(value.matches("^[0-9]{4}(-[0-9]{2}(-[0-9]{2})?)?$"), s"Retrieval citation $rowId uses invalid date $value")
      round.foreach(value => requireDisplayText("Retrieval citation", rowId, "round", value, containsForbiddenRetrievalWording))
      result.foreach: value =>
        require(Set("1-0", "0-1", "1/2-1/2", "*").contains(value), s"Retrieval citation $rowId uses invalid result metadata $value")
      url.foreach: value =>
        require(value.startsWith("https://") || value.startsWith("local-curation:"), s"Retrieval citation $rowId uses unsupported URL $value")
      this

  final case class RetrievalExample(
      retrievalId: String,
      sourceId: String,
      sourceRef: String,
      exampleKind: String,
      fen: String,
      positionKey: String,
      sideToMove: String,
      similarityKey: RetrievalSimilarityKey,
      similarityScore: Double,
      similarityKind: String,
      matchedFeatures: List[String],
      sourceQuality: String,
      gameMetadata: Option[RetrievalCitation],
      licenseNotice: Option[String],
      attributionText: Option[String],
      snippetRole: String,
      currentPositionClaim: Boolean,
      authority: String,
      tags: List[String],
      negativeBoundaries: List[String]
  ):
    def validatedContract(
        manifests: Iterable[SourceManifestRow],
        existingRows: Iterable[RetrievalExample] = Nil,
        motifCarrierRefs: Set[String] = Set.empty,
        endgameApplicabilityRefs: Set[String] = Set.empty,
        motifCarrierBindings: Map[String, (String, String)] = Map.empty,
        endgameApplicabilityBindings: Map[String, (String, String)] = Map.empty
    ): RetrievalExample =
      validatedSourceId(manifests)
      validatedRetrievalId
      validatedSourceRef
      validatedExampleKind
      validatedFen
      validatedPositionKey
      validatedSideToMove
      validatedSimilarityKind
      validatedSimilarityKey(motifCarrierRefs, endgameApplicabilityRefs, motifCarrierBindings, endgameApplicabilityBindings)
      validatedSimilarityScore
      validatedMatchedFeatures
      validatedSourceQuality
      validatedCitationBoundary
      validatedAuthority
      validatedTags
      validatedNegativeBoundaries
      validatedCurrentPositionBoundary
      validateDuplicateSourceRef(existingRows)
      this

    def validatedSourceId(manifests: Iterable[SourceManifestRow]): String =
      requireSource(sourceId, "retrieval", manifests)

    def validatedRetrievalId: String =
      requireToken("Retrieval example", retrievalId, "retrievalId")

    def validatedFen: Fen.Full =
      Fen.Full.clean(fen)

    def validatedPositionKey: String =
      ensure(positionKey.trim.nonEmpty, s"Retrieval example $retrievalId must declare positionKey")
      ensure(positionKey == positionKeyFromFen(fen), s"Retrieval example $retrievalId positionKey must match normalized FEN key")
      positionKey

    def validatedSideToMove: String =
      ensure(Set("white", "black").contains(sideToMove), s"Retrieval example $retrievalId uses unsupported sideToMove $sideToMove")
      ensure(sideToMove == sideToMoveFromFen(fen), s"Retrieval example $retrievalId sideToMove must match FEN")
      sideToMove

    def validatedSimilarityKey(
        motifCarrierRefs: Set[String] = Set.empty,
        endgameApplicabilityRefs: Set[String] = Set.empty,
        motifCarrierBindings: Map[String, (String, String)] = Map.empty,
        endgameApplicabilityBindings: Map[String, (String, String)] = Map.empty
    ): JsObject =
      similarityKey.validated(retrievalId, similarityKind, positionKey, fen, motifCarrierRefs, endgameApplicabilityRefs, motifCarrierBindings, endgameApplicabilityBindings)

    def validatedSourceRef: String =
      ensure(sourceRef.trim.nonEmpty, s"Retrieval example $retrievalId must declare sourceRef")
      require(
        sourceRef.startsWith("retrieval-example:") && sourceRef.matches("^[A-Za-z][A-Za-z0-9_:-]*$"),
        s"Retrieval example $retrievalId uses invalid sourceRef $sourceRef"
      )
      ensure(
        !containsForbiddenRetrievalWording(retrievalId) && !containsForbiddenRetrievalWording(sourceRef),
        s"Retrieval example $retrievalId source identity contains forbidden truth wording"
      )
      sourceRef

    def validatedExampleKind: String =
      require(allowedRetrievalExampleKinds.contains(exampleKind), s"Retrieval example $retrievalId uses unsupported exampleKind $exampleKind")
      exampleKind

    def validatedSimilarityScore: Double =
      require(
        similarityScore >= retrievalSimilarityFloor && similarityScore <= 1.0,
        s"Retrieval example $retrievalId similarityScore below retrieval floor"
      )
      if similarityKind == "exact_position" then
        require(similarityScore >= exactPositionSimilarityFloor, s"Retrieval example $retrievalId exact_position similarityScore below exact floor")
      similarityScore

    def validatedSimilarityKind: String =
      require(allowedRetrievalSimilarityKinds.contains(similarityKind), s"Retrieval example $retrievalId uses unsupported similarityKind $similarityKind")
      similarityKind

    def validatedMatchedFeatures: Vector[String] =
      val features = validateTokenList("Retrieval example", retrievalId, "matchedFeatures", matchedFeatures)
      ensure(!features.exists(isTruthBearingLabel), s"Retrieval example $retrievalId matchedFeatures contain truth-bearing labels")
      ensure(!features.exists(containsForbiddenRetrievalWording), s"Retrieval example $retrievalId matchedFeatures contain forbidden truth wording")
      if similarityKind == "opening_context" then
        val pawnStructures = (similarityKey.fields \ "pawnStructure").asOpt[Vector[String]].getOrElse(Vector.empty)
        ensure(pawnStructures.exists(structure => features.contains(s"structure:$structure")), s"Retrieval example $retrievalId opening_context requires matched pawn structure feature")
      features

    def validatedSourceQuality: String =
      require(allowedRetrievalSourceQuality.contains(sourceQuality), s"Retrieval example $retrievalId uses unsupported sourceQuality $sourceQuality")
      sourceQuality

    def validatedCitationBoundary: Boolean =
      gameMetadata.foreach(_.validated(retrievalId))
      val hasCitation = gameMetadata.exists(_.hasMetadata)
      if hasCitation then
        requirePresent(licenseNotice, s"Retrieval example $retrievalId citation metadata requires licenseNotice")
        requirePresent(attributionText, s"Retrieval example $retrievalId citation metadata requires attributionText")
      licenseNotice.foreach(value => requireDisplayText("Retrieval example", retrievalId, "licenseNotice", value, containsForbiddenRetrievalWording))
      attributionText.foreach(value => requireDisplayText("Retrieval example", retrievalId, "attributionText", value, containsForbiddenRetrievalWording))
      true

    def validatedAuthority: String =
      require(
        allowedRetrievalAuthorities.contains(authority) && authority == "retrieval_example",
        s"Retrieval example $retrievalId uses unsupported authority $authority"
      )
      authority

    def validatedCurrentPositionBoundary: Boolean =
      ensure(
        !currentPositionClaim && snippetRole == "non_authoritative_context",
        s"Retrieval example $retrievalId must remain non-authoritative"
      )
      currentPositionClaim

    def validatedTags: Vector[String] =
      val values = validateTokenList("Retrieval example", retrievalId, "tags", tags)
      ensure(!values.exists(isTruthBearingLabel), s"Retrieval example $retrievalId tags contain truth-bearing labels")
      ensure(!values.exists(containsForbiddenRetrievalWording), s"Retrieval example $retrievalId tags contain forbidden truth wording")
      values

    def validatedNegativeBoundaries: Vector[String] =
      val boundaries = validateTokenList("Retrieval example", retrievalId, "negativeBoundaries", negativeBoundaries)
      ensure(boundaries.contains("retrieval_non_authoritative"), s"Retrieval example $retrievalId negativeBoundaries must include retrieval_non_authoritative")
      boundaries

    def validateDuplicateSourceRef(existingRows: Iterable[RetrievalExample]): Unit =
      val duplicate = existingRows.exists(row => row.retrievalId != retrievalId && row.sourceRef == sourceRef)
      ensure(!duplicate, s"Retrieval example $retrievalId duplicates sourceRef $sourceRef")

  final case class RetrievalSourceReject(
      retrievalId: String,
      sourceId: Option[String],
      sourceRef: Option[String],
      exampleKind: Option[String],
      fen: Option[String],
      positionKey: Option[String],
      sideToMove: Option[String],
      similarityKey: Option[RetrievalSimilarityKey],
      similarityScore: Option[Double],
      similarityKind: Option[String],
      matchedFeatures: List[String],
      sourceQuality: Option[String],
      gameMetadata: Option[RetrievalCitation],
      licenseNotice: Option[String],
      attributionText: Option[String],
      snippetRole: Option[String],
      currentPositionClaim: Option[Boolean],
      authority: Option[String],
      tags: List[String],
      negativeBoundaries: List[String],
      expectation: String,
      rejectReason: String
  ):
    def validatedRejectReason: String =
      require(expectation == "rejected", s"Retrieval reject fixture $retrievalId must be rejected")
      requireToken("Retrieval reject fixture", rejectReason, "rejectReason")

    def asExampleCandidate: RetrievalExample =
      RetrievalExample(
        retrievalId = retrievalId,
        sourceId = sourceId.getOrElse(""),
        sourceRef = sourceRef.getOrElse(""),
        exampleKind = exampleKind.getOrElse("curated_reference"),
        fen = fen.getOrElse(""),
        positionKey = positionKey.getOrElse(""),
        sideToMove = sideToMove.getOrElse("white"),
        similarityKey = similarityKey.getOrElse(RetrievalSimilarityKey(Json.obj())),
        similarityScore = similarityScore.getOrElse(0.0),
        similarityKind = similarityKind.getOrElse("mixed_feature_context"),
        matchedFeatures = matchedFeatures,
        sourceQuality = sourceQuality.getOrElse("local_curated"),
        gameMetadata = gameMetadata,
        licenseNotice = licenseNotice,
        attributionText = attributionText,
        snippetRole = snippetRole.getOrElse("non_authoritative_context"),
        currentPositionClaim = currentPositionClaim.getOrElse(false),
        authority = authority.getOrElse("retrieval_example"),
        tags = tags,
        negativeBoundaries = negativeBoundaries
      )

  private given Reads[SourceManifestRow] with
    def reads(json: JsValue): JsResult[SourceManifestRow] =
      for
        sourceId <- (json \ "sourceId").validate[String]
        sourceFamily <- (json \ "sourceFamily").validate[String]
        sourceType <- (json \ "sourceType").validate[String]
        sourceUse <- (json \ "sourceUse").validateOpt[String]
        license <- (json \ "license").validate[String]
        redistribution <- (json \ "redistribution").validate[String]
        derivedData <- (json \ "derivedData").validate[String]
        attributionRequired <- (json \ "attributionRequired").validateOpt[Boolean].map(_.getOrElse(false))
        shareAlikeRequired <- (json \ "shareAlikeRequired").validateOpt[Boolean]
        attributionText <- (json \ "attributionText").validateOpt[String]
        licenseNotice <- (json \ "licenseNotice").validateOpt[String]
        sourceUrl <- (json \ "sourceUrl").validate[String]
        notes <- (json \ "notes").validateOpt[String]
        aggregateUse <- (json \ "aggregateUse").validateOpt[String]
        sourceScope <- (json \ "sourceScope").validateOpt[String]
        playEnvironment <- (json \ "playEnvironment").validateOpt[String]
        playerLevel <- (json \ "playerLevel").validateOpt[String]
        ratingSystem <- (json \ "ratingSystem").validateOpt[String]
        minElo <- (json \ "minElo").validateOpt[Int]
        titlePolicy <- (json \ "titlePolicy").validateOpt[String]
        timeScope <- (json \ "timeScope").validateOpt[String]
        periodStart <- (json \ "periodStart").validateOpt[String]
        periodEnd <- (json \ "periodEnd").validateOpt[String]
        timeControlScope <- (json \ "timeControlScope").validateOpt[String]
        perPositionSampleSizeThreshold <- (json \ "perPositionSampleSizeThreshold").validateOpt[Int]
        dedupePolicy <- (json \ "dedupePolicy").validateOpt[String]
        annotationPolicy <- (json \ "annotationPolicy").validateOpt[String]
        sourceYear <- (json \ "sourceYear").validateOpt[Int]
        sourceMonth <- (json \ "sourceMonth").validateOpt[Int]
        yearMonth <- (json \ "yearMonth").validateOpt[String]
        ratingBucket <- (json \ "ratingBucket").validateOpt[String]
        timeControlBucket <- (json \ "timeControlBucket").validateOpt[String]
        variant <- (json \ "variant").validateOpt[String]
        ratedFilter <- (json \ "ratedFilter").validateOpt[String]
        ratedOnly <- (json \ "ratedOnly").validateOpt[Boolean]
        sampleSizeThreshold <- (json \ "sampleSizeThreshold").validateOpt[Int]
        parserVersion <- (json \ "parserVersion").validateOpt[String]
        rawStoragePolicy <- (json \ "rawStoragePolicy").validateOpt[String]
        generatedStoragePolicy <- (json \ "generatedStoragePolicy").validateOpt[String]
        sourceChecksum <- (json \ "sourceChecksum").validateOpt[String]
      yield SourceManifestRow(
        sourceId,
        sourceFamily,
        sourceType,
        sourceUse,
        license,
        redistribution,
        derivedData,
        attributionRequired,
        shareAlikeRequired,
        attributionText,
        licenseNotice,
        sourceUrl,
        notes,
        aggregateUse,
        sourceScope,
        playEnvironment,
        playerLevel,
        ratingSystem,
        minElo,
        titlePolicy,
        timeScope,
        periodStart,
        periodEnd,
        timeControlScope,
        perPositionSampleSizeThreshold,
        dedupePolicy,
        annotationPolicy,
        sourceYear,
        sourceMonth,
        yearMonth,
        ratingBucket,
        timeControlBucket,
        variant,
        ratedFilter,
        ratedOnly,
        sampleSizeThreshold,
        parserVersion,
        rawStoragePolicy,
        generatedStoragePolicy,
        sourceChecksum
      )

  private given Reads[OpeningLine] =
    (
      (__ \ "id").read[String] and
        (__ \ "sourceId").read[String] and
        (__ \ "eco").read[String] and
        (__ \ "family").read[String] and
        (__ \ "variation").read[String] and
        (__ \ "names").read[List[String]] and
        (__ \ "moveOrder").read[List[String]] and
        (__ \ "lineKey").read[String] and
        (__ \ "authority").read[String] and
        (__ \ "negativeBoundaries").read[List[String]]
    )(OpeningLine.apply)

  private given Reads[OpeningPosition] =
    (
      (__ \ "id").read[String] and
        (__ \ "sourceId").read[String] and
        (__ \ "lineId").read[String] and
        (__ \ "eco").read[String] and
        (__ \ "name").read[String] and
        (__ \ "positionKey").read[String] and
        (__ \ "fen").read[String] and
        (__ \ "ply").read[Int] and
        (__ \ "moveOrder").read[List[String]] and
        (__ \ "transpositionState").read[String] and
        (__ \ "status").read[String] and
        (__ \ "authority").read[String] and
        (__ \ "contextRefs").read[List[String]]
    )(OpeningPosition.apply)

  private given Reads[OpeningMoveStat] =
    (
      (__ \ "id").read[String] and
        (__ \ "sourceId").read[String] and
        (__ \ "positionKey").read[String] and
        (__ \ "move").read[String] and
        (__ \ "sampleSize").read[Int] and
        (__ \ "whiteWins").read[Int] and
        (__ \ "draws").read[Int] and
        (__ \ "blackWins").read[Int] and
        (__ \ "frequency").read[Double] and
        (__ \ "candidateKind").read[String] and
        (__ \ "authority").read[String] and
        (__ \ "negativeBoundaries").read[List[String]]
    )(OpeningMoveStat.apply)

  private given Reads[OpeningTheme] =
    (
      (__ \ "id").read[String] and
        (__ \ "sourceId").read[String] and
        (__ \ "positionKey").read[String] and
        (__ \ "themeId").read[String] and
        (__ \ "family").read[String] and
        (__ \ "contextTags").read[List[String]] and
        (__ \ "planRefs").read[List[String]] and
        (__ \ "breakRefs").read[List[String]] and
        (__ \ "authority").read[String] and
        (__ \ "negativeBoundaries").read[List[String]]
    )(OpeningTheme.apply)

  private given Reads[OpeningAlias] =
    (
      (__ \ "id").read[String] and
        (__ \ "sourceId").read[String] and
        (__ \ "sourceName").read[String] and
        (__ \ "canonicalName").read[String] and
        (__ \ "openingFamily").read[String] and
        (__ \ "openingVariation").read[String] and
        (__ \ "displayName").read[String] and
        (__ \ "contextName").read[String] and
        (__ \ "aliasKind").read[String] and
        (__ \ "sourceRefs").read[List[String]] and
        (__ \ "negativeBoundaries").read[List[String]]
    )(OpeningAlias.apply)

  private given Reads[MotifCarrierRef] = Reads: json =>
    rejectUnknownFields(json, motifCarrierFields, "Motif detector carrier")
    (
      (__ \ "ref").read[String] and
        (__ \ "descriptorId").read[String] and
        (__ \ "anchor").read[String] and
        (__ \ "carrierKind").read[String]
    )(MotifCarrierRef.apply).reads(json)

  private given Reads[MotifExample] =
    (
      (__ \ "id").read[String] and
        (__ \ "motifId").read[String] and
        (__ \ "displayName").read[String] and
        (__ \ "sourceId").read[String] and
        (__ \ "fen").read[String] and
        (__ \ "detectorCarrier").readNullable[MotifCarrierRef] and
        (__ \ "involvedSquares").readWithDefault[List[String]](Nil) and
        (__ \ "sourceTags").readWithDefault[List[String]](Nil) and
        (__ \ "authority").read[String] and
        (__ \ "currentPositionClaim").read[Boolean] and
        (__ \ "sourceRefs").readWithDefault[List[String]](Nil) and
        (__ \ "negativeBoundaries").read[List[String]]
    )(MotifExample.apply)

  private given Reads[MotifRejectFixture] =
    (
      (__ \ "id").read[String] and
        (__ \ "sourceId").read[String] and
        (__ \ "motifId").read[String] and
        (__ \ "fen").read[String] and
        (__ \ "displayName").readNullable[String] and
        (__ \ "detectorCarrier").readNullable[MotifCarrierRef] and
        (__ \ "involvedSquares").readWithDefault[List[String]](Nil) and
        (__ \ "sourceTags").readWithDefault[List[String]](Nil) and
        (__ \ "authority").readNullable[String] and
        (__ \ "currentPositionClaim").readNullable[Boolean] and
        (__ \ "sourceRefs").readWithDefault[List[String]](Nil) and
        (__ \ "expectation").read[String] and
        (__ \ "rejectReason").read[String] and
        (__ \ "negativeBoundaries").readWithDefault[List[String]](Nil)
    )(MotifRejectFixture.apply)

  private given Reads[EndgameStudy] =
    (
      (__ \ "studyId").read[String] and
        (__ \ "displayName").read[String] and
        (__ \ "names").read[List[String]] and
        (__ \ "materialClass").read[String] and
        (__ \ "sideToMove").read[List[String]] and
        (__ \ "placementRules").read[List[String]] and
        (__ \ "relationRules").read[List[String]] and
        (__ \ "allowedTransforms").read[List[String]] and
        (__ \ "candidatePlans").read[List[String]] and
        (__ \ "sourceRefs").read[List[String]] and
        (__ \ "authority").read[String] and
        (__ \ "outcomeClaim").read[String] and
        (__ \ "negativeBoundaries").read[List[String]]
    )(EndgameStudy.apply)

  private given Reads[EndgameStudyFixture] =
    (
      (__ \ "id").read[String] and
        (__ \ "studyId").read[String] and
        (__ \ "fen").read[String] and
        (__ \ "materialClass").read[String] and
        (__ \ "sideToMove").read[String] and
        (__ \ "placementEvidence").read[List[String]] and
        (__ \ "relationEvidence").read[List[String]] and
        (__ \ "contextStatus").read[String] and
        (__ \ "outcomeClaim").read[String] and
        (__ \ "sourceRefs").read[List[String]]
    )(EndgameStudyFixture.apply)

  private given Reads[EndgameStudyRejectFixture] =
    (
      (__ \ "id").read[String] and
        (__ \ "studyId").read[String] and
        (__ \ "fen").read[String] and
        (__ \ "materialClass").read[String] and
        (__ \ "expectation").read[String] and
        (__ \ "rejectReason").read[String] and
        (__ \ "negativeBoundaries").read[List[String]]
    )(EndgameStudyRejectFixture.apply)

  private given Reads[RetrievalSimilarityKey] = Reads: json =>
    json.asOpt[JsObject] match
      case Some(obj) => JsSuccess(RetrievalSimilarityKey(obj))
      case None      => JsError("retrieval similarityKey must be an object")

  private given Reads[RetrievalCitation] = Reads: json =>
    rejectUnknownFields(json, retrievalCitationFields, "Retrieval citation")
    (
      (__ \ "players").readWithDefault[List[String]](Nil) and
        (__ \ "event").readNullable[String] and
        (__ \ "date").readNullable[String] and
        (__ \ "round").readNullable[String] and
        (__ \ "result").readNullable[String] and
        (__ \ "url").readNullable[String]
    )(RetrievalCitation.apply).reads(json)

  private given Reads[RetrievalExample] with
    def reads(json: JsValue): JsResult[RetrievalExample] =
      for
        retrievalId <- (json \ "retrievalId").validate[String]
        sourceId <- (json \ "sourceId").validate[String]
        sourceRef <- (json \ "sourceRef").validate[String]
        exampleKind <- (json \ "exampleKind").validate[String]
        fen <- (json \ "fen").validate[String]
        positionKey <- (json \ "positionKey").validate[String]
        sideToMove <- (json \ "sideToMove").validate[String]
        similarityKey <- (json \ "similarityKey").validate[RetrievalSimilarityKey]
        similarityScore <- (json \ "similarityScore").validate[Double]
        similarityKind <- (json \ "similarityKind").validate[String]
        matchedFeatures <- (json \ "matchedFeatures").validate[List[String]]
        sourceQuality <- (json \ "sourceQuality").validate[String]
        gameMetadata <- (json \ "gameMetadata").validateOpt[RetrievalCitation]
        licenseNotice <- (json \ "licenseNotice").validateOpt[String]
        attributionText <- (json \ "attributionText").validateOpt[String]
        snippetRole <- (json \ "snippetRole").validate[String]
        currentPositionClaim <- (json \ "currentPositionClaim").validate[Boolean]
        authority <- (json \ "authority").validate[String]
        tags <- (json \ "tags").validate[List[String]]
        negativeBoundaries <- (json \ "negativeBoundaries").validate[List[String]]
      yield RetrievalExample(
        retrievalId,
        sourceId,
        sourceRef,
        exampleKind,
        fen,
        positionKey,
        sideToMove,
        similarityKey,
        similarityScore,
        similarityKind,
        matchedFeatures,
        sourceQuality,
        gameMetadata,
        licenseNotice,
        attributionText,
        snippetRole,
        currentPositionClaim,
        authority,
        tags,
        negativeBoundaries
      )

  private given Reads[RetrievalSourceReject] with
    def reads(json: JsValue): JsResult[RetrievalSourceReject] =
      for
        retrievalId <- (json \ "retrievalId").validate[String]
        sourceId <- (json \ "sourceId").validateOpt[String]
        sourceRef <- (json \ "sourceRef").validateOpt[String]
        exampleKind <- (json \ "exampleKind").validateOpt[String]
        fen <- (json \ "fen").validateOpt[String]
        positionKey <- (json \ "positionKey").validateOpt[String]
        sideToMove <- (json \ "sideToMove").validateOpt[String]
        similarityKey <- (json \ "similarityKey").validateOpt[RetrievalSimilarityKey]
        similarityScore <- (json \ "similarityScore").validateOpt[Double]
        similarityKind <- (json \ "similarityKind").validateOpt[String]
        matchedFeatures <- (json \ "matchedFeatures").validateOpt[List[String]].map(_.getOrElse(Nil))
        sourceQuality <- (json \ "sourceQuality").validateOpt[String]
        gameMetadata <- (json \ "gameMetadata").validateOpt[RetrievalCitation]
        licenseNotice <- (json \ "licenseNotice").validateOpt[String]
        attributionText <- (json \ "attributionText").validateOpt[String]
        snippetRole <- (json \ "snippetRole").validateOpt[String]
        currentPositionClaim <- (json \ "currentPositionClaim").validateOpt[Boolean]
        authority <- (json \ "authority").validateOpt[String]
        tags <- (json \ "tags").validateOpt[List[String]].map(_.getOrElse(Nil))
        negativeBoundaries <- (json \ "negativeBoundaries").validateOpt[List[String]].map(_.getOrElse(Nil))
        expectation <- (json \ "expectation").validate[String]
        rejectReason <- (json \ "rejectReason").validate[String]
      yield RetrievalSourceReject(
        retrievalId,
        sourceId,
        sourceRef,
        exampleKind,
        fen,
        positionKey,
        sideToMove,
        similarityKey,
        similarityScore,
        similarityKind,
        matchedFeatures,
        sourceQuality,
        gameMetadata,
        licenseNotice,
        attributionText,
        snippetRole,
        currentPositionClaim,
        authority,
        tags,
        negativeBoundaries,
        expectation,
        rejectReason
      )

  def parseManifest(json: JsValue): SourceManifestRow = parseJson[SourceManifestRow](json, "source manifest")
  def parseOpeningPosition(json: JsValue): OpeningPosition = parseJson[OpeningPosition](json, "opening position")
  def parseOpeningMoveStat(json: JsValue): OpeningMoveStat =
    rejectUnknownFields(json, openingMoveStatFields, "Opening move stat")
    parseJson[OpeningMoveStat](json, "opening move stat")
  def parseOpeningAlias(json: JsValue): OpeningAlias =
    rejectUnknownFields(json, openingAliasFields, "Opening alias")
    parseJson[OpeningAlias](json, "opening alias")
  def parseMotifExample(json: JsValue): MotifExample =
    rejectUnknownFields(json, motifExampleFields, "Motif example")
    parseJson[MotifExample](json, "motif example")
  def parseMotifRejectFixture(json: JsValue): MotifRejectFixture =
    rejectUnknownFields(json, motifRejectFields, "Motif reject fixture")
    parseJson[MotifRejectFixture](json, "motif reject")
  def parseEndgameStudy(json: JsValue): EndgameStudy =
    rejectUnknownFields(json, endgameStudyFields, "Endgame study")
    parseJson[EndgameStudy](json, "endgame study")
  def parseRetrievalExample(json: JsValue): RetrievalExample =
    rejectUnknownFields(json, retrievalFields, "Retrieval example")
    parseJson[RetrievalExample](json, "retrieval example")
  def parseRetrievalRejectFixture(json: JsValue): RetrievalSourceReject =
    rejectUnknownFields(json, retrievalRejectFields, "Retrieval reject fixture")
    parseJson[RetrievalSourceReject](json, "retrieval reject")

  def loadManifests(): Vector[SourceManifestRow] = loadJsonl[SourceManifestRow](manifestPath, "source manifest")
  def loadOpeningLines(): Vector[OpeningLine] = loadJsonl[OpeningLine](openingLinePath, "opening line")
  def loadOpeningPositions(): Vector[OpeningPosition] = loadJsonl[OpeningPosition](openingPositionPath, "opening position")
  def loadOpeningMoveStats(): Vector[OpeningMoveStat] = loadJsonl[OpeningMoveStat](openingMoveStatPath, "opening move stat")
  def loadOpeningThemes(): Vector[OpeningTheme] = loadJsonl[OpeningTheme](openingThemePath, "opening theme")
  def loadOpeningAliases(): Vector[OpeningAlias] =
    loadJsonValues(openingAliasPath, "opening alias").map(parseOpeningAlias)
  def loadMotifExamples(): Vector[MotifExample] =
    loadJsonValues(motifExamplePath, "motif example").map(parseMotifExample)
  def loadMotifRejectFixtures(): Vector[MotifRejectFixture] =
    loadJsonValues(motifRejectPath, "motif reject").map(parseMotifRejectFixture)
  def loadEndgameStudies(): Vector[EndgameStudy] = loadJsonl[EndgameStudy](endgameStudyPath, "endgame study")
  def loadEndgameStudyFixtures(): Vector[EndgameStudyFixture] =
    loadJsonl[EndgameStudyFixture](endgameStudyFixturePath, "endgame study fixture")
  def loadEndgameStudyRejectFixtures(): Vector[EndgameStudyRejectFixture] =
    loadJsonl[EndgameStudyRejectFixture](endgameStudyRejectPath, "endgame study reject")
  def loadRetrievalExamples(): Vector[RetrievalExample] =
    loadJsonValues(retrievalExamplePath, "retrieval example").map(parseRetrievalExample)
  def loadRetrievalRejectFixtures(): Vector[RetrievalSourceReject] =
    loadJsonValues(retrievalRejectPath, "retrieval reject").map(parseRetrievalRejectFixture)

  def resourceExists(fileName: String): Boolean =
    Option(getClass.getResourceAsStream(s"/commentary-corpus/$fileName")).exists: stream =>
      stream.close()
      true

  private def parseJson[A: Reads](json: JsValue, label: String): A =
    json.validate[A].fold(
      errors => throw IllegalArgumentException(s"Invalid $label row: ${JsError.toJson(errors)}"),
      identity
    )

  private def rejectUnknownFields(json: JsValue, allowed: Set[String], label: String): Unit =
    val obj = json.asOpt[JsObject].getOrElse(throw IllegalArgumentException(s"$label row must be a JSON object"))
    val id = (obj \ "id").asOpt[String].orElse((obj \ "studyId").asOpt[String]).orElse((obj \ "retrievalId").asOpt[String]).getOrElse("unknown")
    val extra = obj.keys.filterNot(allowed.contains).toVector.sorted
    if extra.nonEmpty then throw IllegalArgumentException(s"$label $id uses unsupported fields: ${extra.mkString(", ")}")

  private def loadJsonl[A: Reads](resourcePath: String, label: String): Vector[A] =
    loadJsonValues(resourcePath, label).map: json =>
      json.validate[A].fold(
        errors => throw IllegalArgumentException(s"Invalid $label row: ${JsError.toJson(errors)}"),
        identity
      )

  private def loadJsonValues(resourcePath: String, label: String): Vector[JsValue] =
    val source =
      scala.io.Source.fromInputStream(
        Option(getClass.getResourceAsStream(resourcePath))
          .getOrElse(throw IllegalStateException(s"Missing test resource $resourcePath"))
      )
    try
      source
        .getLines()
        .filter(_.trim.nonEmpty)
        .zipWithIndex
        .map: (line, index) =>
          try Json.parse(line)
          catch
            case error: Throwable =>
              throw IllegalArgumentException(s"Invalid $label row ${index + 1}: ${error.getMessage}")
        .toVector
    finally source.close()

  private def requireSource(sourceId: String, family: String, manifests: Iterable[SourceManifestRow]): String =
    val manifest =
      manifests.find(_.sourceId == sourceId).getOrElse(throw IllegalArgumentException(s"Unknown sourceId $sourceId"))
    require(manifest.validatedSourceFamily == family, s"Source $sourceId must be family $family")
    manifest.validatedLicense
    if family == "retrieval" then manifest.validatedRetrievalBoundary
    sourceId

  private def requirePresent(value: Option[String], message: String): String =
    value.map(_.trim).filter(_.nonEmpty).getOrElse(throw IllegalArgumentException(message))

  private def validateMoveOrder(label: String, id: String, moves: List[String]): Vector[String] =
    val values = moves.map(_.trim).filter(_.nonEmpty).toVector
    require(values.nonEmpty, s"$label $id must declare moveOrder")
    values.foreach(requireUciMove(label, id, _))
    values

  private def requireUciMove(label: String, id: String, move: String): String =
    require(
      move.matches("^[a-h][1-8][a-h][1-8][qrbn]?$"),
      s"$label $id uses invalid UCI move $move"
    )
    move

  private def validateTokenList(label: String, id: String, field: String, values: List[String]): Vector[String] =
    val normalized = values.map(_.trim).filter(_.nonEmpty).toVector
    require(normalized.nonEmpty, s"$label $id must declare $field")
    require(
      normalized.distinct == normalized,
      s"$label $id declares duplicate $field entries: ${normalized.mkString(", ")}"
    )
    normalized.foreach(value => requireToken(label, value, field))
    normalized

  private def requireToken(label: String, value: String, field: String): String =
    require(value.trim.nonEmpty, s"$label $field must not be empty")
    require(
      value.matches("^[A-Za-z][A-Za-z0-9_:-]*$"),
      s"$label uses invalid $field $value"
    )
    value

  private def requireDisplayText(label: String, id: String, field: String, value: String, forbidden: String => Boolean): String =
    val normalized = value.trim
    ensure(normalized.nonEmpty, s"$label $id must declare $field")
    ensure(!forbidden(normalized), s"$label $id $field contains forbidden truth wording")
    normalized

  private def requireJsonString(fields: JsObject, field: String, message: String): String =
    fields.value.get(field).flatMap(_.asOpt[String]).filter(_.trim.nonEmpty).getOrElse(throw IllegalArgumentException(message))

  private def requireJsonStringArray(fields: JsObject, field: String, message: String): Vector[String] =
    fields.value.get(field).flatMap(_.asOpt[Vector[String]]).filter(_.nonEmpty).getOrElse(throw IllegalArgumentException(message))

  private def materialClassFromFen(fen: String): String =
    val board = fen.takeWhile(_ != ' ')
    val pieces = board.filter(_.isLetter)
    if pieces.size >= 24 then return "opening_full_material"
    val nonKings = pieces.filter(ch => ch.toLower != 'k')
    val rookCount = nonKings.count(ch => ch.toLower == 'r')
    val pawnCount = nonKings.count(ch => ch.toLower == 'p')
    val bishopCount = nonKings.count(ch => ch.toLower == 'b')
    val knightCount = nonKings.count(ch => ch.toLower == 'n')
    val otherCount = nonKings.count(ch => !Set('r', 'p', 'b', 'n').contains(ch.toLower))
    if rookCount == 2 && pawnCount == 1 && otherCount == 0 then "rook_pawn_vs_rook"
    else if rookCount == 0 && bishopCount == 0 && pawnCount == 1 && otherCount == 0 then "king_pawn_vs_king"
    else if rookCount == 0 && bishopCount == 1 && pawnCount == 1 && otherCount == 0 then "bishop_rook_pawn_vs_king"
    else if knightCount > 0 || bishopCount > 0 then "minor_piece"
    else "unsupported"

  private def positionKeyFromFen(fen: String): String =
    val fields = Fen.Full.clean(fen).toString.split(" ").take(4).mkString(" ")
    s"std:$fields"

  private def sideToMoveFromFen(fen: String): String =
    if Fen.Full.clean(fen).toString.split(" ")(1) == "w" then "white" else "black"

  private def endgameSideToMoveRoleFromFen(fen: String): String =
    val side = if sideToMoveFromFen(fen) == "white" then 'w' else 'b'
    val pieces = EndgameBoardEvidence.piecesFromFen(fen)
    val pawns = pieces.filter(_.role == 'p')
    val stronger =
      pawns.headOption
        .map(_.color)
        .orElse:
          val nonKings = pieces.filterNot(_.role == 'k')
          val whiteMaterial = nonKings.count(_.color == 'w')
          val blackMaterial = nonKings.count(_.color == 'b')
          if whiteMaterial > blackMaterial then Some('w')
          else if blackMaterial > whiteMaterial then Some('b')
          else None
        .getOrElse(throw IllegalArgumentException(s"Endgame FEN lacks stronger-side material evidence: $fen"))
    if side == stronger then "stronger" else "defender"

  private def pawnStructureEvidenceFromFen(fen: String): Set[String] =
    val pieces = EndgameBoardEvidence.piecesFromFen(fen)
    val whitePawnD4 = pieces.exists(piece => piece.role == 'p' && piece.color == 'w' && piece.file == 4 && piece.rank == 4)
    val blackPawnC4 = pieces.exists(piece => piece.role == 'p' && piece.color == 'b' && piece.file == 3 && piece.rank == 4)
    val evidence = scala.collection.mutable.Set.empty[String]
    if whitePawnD4 && blackPawnC4 then evidence += "queenside_tension"
    evidence.toSet

  private def openingFamilyEvidenceFromFen(fen: String): Set[String] =
    if pawnStructureEvidenceFromFen(fen).contains("queenside_tension") then Set("Catalan Opening")
    else Set.empty

  private def isTruthBearingLabel(value: String): Boolean =
    val normalized = value.toLowerCase
    truthBearingLabels.contains(normalized) ||
      normalized.matches("^s[0-9]{2}$") ||
      normalized.contains("truth") ||
      normalized.contains("claim")

  private def containsForbiddenAliasWording(value: String): Boolean =
    val normalized = value.toLowerCase
    normalized.contains("kid attack") ||
      forbiddenAliasWords.exists(word => normalized.matches(s".*(^|[^a-z0-9])${java.util.regex.Pattern.quote(word)}([^a-z0-9]|$$).*"))

  private def containsForbiddenMotifWording(value: String): Boolean =
    val normalized = value.toLowerCase
    Vector("best", "forced", "result", "engine", "oracle", "winning", "win", "draw", "loss", "truth").exists(normalized.contains) ||
      normalized.matches(".*(^|[^a-z0-9])s[0-9]{2}([^a-z0-9]|$).*")

  private def containsForbiddenEndgameWording(value: String): Boolean =
    val normalized = value.toLowerCase
    resultWords.exists(normalized.contains) ||
      normalized.matches(".*(^|[^a-z0-9])s[0-9]{2}([^a-z0-9]|$).*")

  private def containsForbiddenRetrievalWording(value: String): Boolean =
    val normalized = value.toLowerCase
    Vector(
      "best",
      "theory",
      "forced",
      "result",
      "outcome",
      "engine",
      "oracle",
      "tablebase",
      "winning",
      "win",
      "draw",
      "loss",
      "won",
      "lost",
      "proof",
      "current position",
      "current-position",
      "current_position",
      "current position proof",
      "current-position proof",
      "current_position_truth",
      "truth",
      "famous player recommendation"
    ).exists(normalized.contains) ||
      normalized.matches(".*(^|[^a-z0-9])s[0-9]{2}([^a-z0-9]|$).*") ||
      normalized.matches(".*(^|[^a-z0-9])(1-0|0-1|1/2-1/2|wdl|dtz|dtm)([^a-z0-9]|$).*")

  private def jsonContainsForbiddenRetrievalWording(value: JsValue): Boolean =
    value match
      case JsString(text)  => containsForbiddenRetrievalWording(text)
      case JsArray(values) => values.exists(jsonContainsForbiddenRetrievalWording)
      case obj: JsObject =>
        obj.fields.exists((key, nested) => containsForbiddenRetrievalWording(key) || jsonContainsForbiddenRetrievalWording(nested))
      case _ => false

  private final case class BoardPiece(role: Char, color: Char, file: Int, rank: Int)

  private final case class EndgameBoardEvidence(
      placementEvidence: Set[String],
      relationEvidence: Set[String]
  )

  private object EndgameBoardEvidence:
    def fromFen(fen: String): EndgameBoardEvidence =
      val pieces = piecesFromFen(fen)
      val nonKings = pieces.filterNot(_.role == 'k')
      val pawns = nonKings.filter(_.role == 'p')
      val rooks = nonKings.filter(_.role == 'r')
      val bishops = nonKings.filter(_.role == 'b')
      val placement = scala.collection.mutable.Set.empty[String]
      val relations = scala.collection.mutable.Set.empty[String]

      if pawns.size == 1 then
        placement += "single_pawn_endgame"
        placement += "single_passed_pawn"

      val kings = pieces.filter(_.role == 'k')
      if kings.size == 2 then
        val first = kings.head
        val second = kings(1)
        if sameLine(first, second) then
          val gap = math.max(math.abs(first.file - second.file), math.abs(first.rank - second.rank)) - 1
          val clear = interveningSquares(first, second).forall(square => !pieces.exists(piece => piece.file == square._1 && piece.rank == square._2))
          if gap == 1 && clear then relations += "kings_in_basic_opposition"
          if gap >= 3 && gap % 2 == 1 && clear then relations += "kings_in_distant_opposition"

      if pawns.size == 1 && rooks.size == 2 then
        val pawn = pawns.head
        val attacker = pawn.color
        val defender = if attacker == 'w' then 'b' else 'w'
        val attackerKing = pieces.find(piece => piece.role == 'k' && piece.color == attacker)
        val defenderKing = pieces.find(piece => piece.role == 'k' && piece.color == defender)
        val defenderRook = rooks.find(_.color == defender)

        if (attacker == 'w' && pawn.rank == 7) || (attacker == 'b' && pawn.rank == 2) then
          placement += "pawn_on_seventh"
        if (pawn.file == 1 || pawn.file == 8) &&
          ((attacker == 'w' && Set(6, 7).contains(pawn.rank)) ||
            (attacker == 'b' && Set(2, 3).contains(pawn.rank)))
        then placement += "rook_pawn_on_sixth_or_seventh"
        if (attacker == 'w' && pawn.rank != 6) || (attacker == 'b' && pawn.rank != 3) then
          placement += "attacker_pawn_not_on_sixth"
        attackerKing.foreach: king =>
          if distance(king, pawn) <= 1 then placement += "strong_king_near_pawn"
        defenderKing.foreach: king =>
          if distance(king, pawn) > 1 then placement += "defender_king_cut_off"
          if king.file == pawn.file &&
            ((attacker == 'w' && king.rank > pawn.rank) || (attacker == 'b' && king.rank < pawn.rank))
          then placement += "defender_king_in_front"
          val promotionRank = if attacker == 'w' then 8 else 1
          val promotionCornerFile = pawn.file
          if pawn.file == 1 || pawn.file == 8 then
            val promotionCorner = BoardPiece('k', defender, promotionCornerFile, promotionRank)
            if distance(king, promotionCorner) <= 1 then placement += "defender_king_near_corner"
        val attackerRook = rooks.find(_.color == attacker)
        if rooks.size == 2 && attackerRook.exists(rook => math.abs(rook.file - pawn.file) == 1) then
          relations += "rook_check_shelter"
        defenderRook.foreach: rook =>
          if (defender == 'b' && rook.rank == 3) || (defender == 'w' && rook.rank == 6) then
            relations += "defender_rook_on_third_rank"
          if rook.rank == pawn.rank && rook.file != pawn.file &&
            attackerKing.exists(king => king.rank == rook.rank && isBetween(rook.file, pawn.file, king.file))
          then
            relations += "defender_rook_lateral_checking_file"
        attackerRook.foreach: rook =>
          if rook.file == pawn.file &&
            ((attacker == 'w' && rook.rank < pawn.rank) || (attacker == 'b' && rook.rank > pawn.rank))
          then relations += "rook_behind_passed_pawn"

      if pawns.size == 1 && bishops.size == 1 && rooks.isEmpty then
        val pawn = pawns.head
        val attacker = pawn.color
        val defender = if attacker == 'w' then 'b' else 'w'
        val attackerBishop = bishops.find(_.color == attacker)
        val attackerKing = pieces.find(piece => piece.role == 'k' && piece.color == attacker)
        val defenderKing = pieces.find(piece => piece.role == 'k' && piece.color == defender)
        if pawn.file == 1 || pawn.file == 8 then
          if (attacker == 'w' && pawn.rank == 7) || (attacker == 'b' && pawn.rank == 2) then
            placement += "rook_pawn_on_seventh"
          attackerKing.foreach: king =>
            if distance(king, pawn) <= 1 then placement += "strong_king_near_pawn"
          val promotionCorner = BoardPiece('k', defender, pawn.file, if attacker == 'w' then 8 else 1)
          defenderKing.foreach: king =>
            if distance(king, promotionCorner) <= 1 then placement += "defender_king_near_rook_pawn_corner"
          if attackerBishop.exists(bishop => squareColor(bishop.file, bishop.rank) != squareColor(promotionCorner.file, promotionCorner.rank)) then
            relations += "wrong_bishop_promotion_corner"

      EndgameBoardEvidence(placement.toSet, relations.toSet)

    def piecesFromFen(fen: String): Vector[BoardPiece] =
      val board = fen.takeWhile(_ != ' ')
      board
        .split("/")
        .zipWithIndex
        .flatMap: (rankText, rankIndex) =>
          var file = 1
          val rank = 8 - rankIndex
          rankText.flatMap:
            case digit if digit.isDigit =>
              file += digit.asDigit
              None
            case pieceChar =>
              val piece = BoardPiece(
                role = pieceChar.toLower,
                color = if pieceChar.isUpper then 'w' else 'b',
                file = file,
                rank = rank
              )
              file += 1
              Some(piece)
        .toVector

    private def distance(a: BoardPiece, b: BoardPiece): Int =
      math.max(math.abs(a.file - b.file), math.abs(a.rank - b.rank))

    private def sameLine(a: BoardPiece, b: BoardPiece): Boolean =
      a.file == b.file || a.rank == b.rank

    private def squareColor(file: Int, rank: Int): Int =
      (file + rank) % 2

    private def interveningSquares(a: BoardPiece, b: BoardPiece): Vector[(Int, Int)] =
      val fileStep = math.signum(b.file - a.file).toInt
      val rankStep = math.signum(b.rank - a.rank).toInt
      Iterator
        .iterate((a.file + fileStep, a.rank + rankStep))((file, rank) => (file + fileStep, rank + rankStep))
        .takeWhile(square => square != (b.file, b.rank))
        .toVector

    private def isBetween(left: Int, right: Int, value: Int): Boolean =
      value > math.min(left, right) && value < math.max(left, right)

  private def validateOpeningAliasSupport(
      id: String,
      family: String,
      displayName: String,
      contextName: String,
      variation: String
  ): Unit =
    val text = s"$displayName $contextName".toLowerCase
    val normalizedVariation = variation.toLowerCase
    if text.contains("closed spanish") then
      ensure(normalizedVariation.contains("closed"), s"Opening alias $id requires a Closed Spanish taxonomy variation")
    if text.contains("qga") || text.contains("queen's gambit accepted") then
      ensure(family == "Queen's Gambit Accepted", s"Opening alias $id requires Queen's Gambit Accepted family")
    if text.contains("pirc") then
      ensure(family == "Pirc Defense", s"Opening alias $id requires Pirc Defense family")
    if text.contains("blackmar-diemer gambit accepted") || text.contains("blackmar-diemer accepted") then
      ensure(family == "Blackmar-Diemer Gambit Accepted", s"Opening alias $id requires Blackmar-Diemer Gambit Accepted family")

    val requirements = Vector(
      "open sicilian" -> ("Sicilian", "Open"),
      "french exchange" -> ("French", "Exchange"),
      "closed catalan" -> ("Catalan", "Closed"),
      "anti-meran" -> ("Semi-Slav", "Anti-Meran"),
      "grünfeld exchange" -> ("Grünfeld", "Exchange"),
      "english reversed sicilian" -> ("English", "Reversed Sicilian"),
      "reversed sicilian" -> ("English", "Reversed Sicilian"),
      "budapest gambit" -> ("Indian Defense", "Gambit")
    )
    requirements.collectFirst:
      case (phrase, (label, required)) if text.contains(phrase) && !normalizedVariation.contains(required.toLowerCase) =>
        throw IllegalArgumentException(s"Opening alias $id requires $label variation containing $required")

  private def ensure(condition: Boolean, message: => String): Unit =
    if !condition then throw IllegalArgumentException(message)
