package lila.llm.tools.review

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import scala.util.control.NonFatal

import play.api.libs.json.*

import lila.llm.*

object CommentarySceneCoverageCollectionMaterializer:

  import CommentaryPlayerQcSupport.*

  private val DateStamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
  private val DefaultCollectionManifestDir = DefaultManifestDir.resolve(s"cqf_track4_collection_$DateStamp")
  private val DefaultCollectionReportDir = DefaultReportDir.resolve(s"cqf_track4_collection_$DateStamp")

  final case class Config(
      catalogPath: Path = DefaultCatalogDir.resolve("catalog.jsonl"),
      manifestPath: Path = DefaultCollectionManifestDir.resolve("scene_coverage_collection_manifest.jsonl"),
      openingPlanClashRowsPath: Path =
        DefaultCollectionReportDir.resolve("opening_deviation_after_middlegame_plan_clash_rows.jsonl"),
      curatedOpeningPlanClashRowsPath: Path =
        DefaultCollectionReportDir.resolve("opening_deviation_after_middlegame_plan_clash_curated_rows.jsonl"),
      prophylaxisRowsPath: Path =
        DefaultCollectionReportDir.resolve("prophylaxis_restraint_rows.jsonl"),
      curatedOpeningRowsPath: Path =
        DefaultCollectionReportDir.resolve("opening_deviation_after_stable_development_curated_rows.jsonl"),
      fixturePackPath: Path = DefaultCollectionReportDir.resolve("first_curated_fixture_pack.jsonl"),
      fixturePackMarkdownPath: Path = DefaultCollectionReportDir.resolve("first_curated_fixture_pack.md"),
      bucketPlanPath: Path = DefaultCollectionReportDir.resolve("scene_coverage_bucket_plan.jsonl"),
      summaryPath: Path = DefaultCollectionReportDir.resolve("scene_coverage_materialization_summary.md"),
      acceptedManifestPath: Path = DefaultManifestDir.resolve("cqf_real16_20260330").resolve("slice_manifest_real16.jsonl"),
      legacyManifestPath: Path = DefaultManifestDir.resolve("slice_manifest.jsonl"),
      quietRichRowsPath: Path =
        DefaultReportDir.resolve("cqf_track3_real16_20260330").resolve("cqf_track3_quiet_rich_rows.jsonl"),
      depth: Int = 8,
      multiPv: Int = 2,
      maxGames: Int = 120,
      targetOpeningPlanClashRows: Int = 18,
      targetCuratedOpeningPlanClashRows: Int = 6,
      targetProphylaxisRows: Int = 18,
      targetCuratedOpeningStableRows: Int = 6,
      enginePath: Path
  )

  final case class MaterializedOpeningPlanClashRow(
      sampleId: String,
      gameKey: String,
      surface: String,
      targetPly: Int,
      playedSan: String,
      bucket: String,
      score: Int,
      baselineOpeningPrecedentAvailable: Boolean,
      baselineOpeningRelationClaimPresent: Boolean,
      collectionOpeningPrecedentAvailable: Boolean,
      collectionOpeningRelationClaimPresent: Boolean,
      collectionOpeningEvent: Option[String],
      reasons: List[String],
      whyMaterialized: String
  )
  object MaterializedOpeningPlanClashRow:
    given Writes[MaterializedOpeningPlanClashRow] = Json.writes[MaterializedOpeningPlanClashRow]

  final case class QuietRichRow(
      schemaVersion: String,
      sampleId: String,
      gameKey: String,
      bucket: String,
      sliceKind: String,
      targetPly: Int,
      playedSan: String,
      commentary: String,
      realShard: Boolean,
      status: String,
      lane: String,
      statusReasons: List[String],
      plannerSceneType: Option[String],
      bookmakerFallbackMode: Option[String],
      quietnessVerified: Option[Boolean],
      quietnessSpreadCp: Option[Int],
      quietnessNotes: List[String],
      directSources: List[String],
      supportSources: List[String]
  )
  object QuietRichRow:
    given Format[QuietRichRow] = Json.format[QuietRichRow]

  final case class CollectionLaneRow(
      sampleId: String,
      sourceSampleId: String,
      gameKey: String,
      surface: String,
      targetPly: Int,
      playedSan: String,
      bucket: String,
      sourceSliceKind: String,
      sourceStatus: String,
      selectionMode: String,
      bookmakerFallbackMode: Option[String],
      plannerSceneType: Option[String],
      quietnessSpreadCp: Option[Int],
      directSources: List[String],
      supportSources: List[String],
      score: Option[Int],
      reasons: List[String],
      whySelected: String,
      likelyFailureMode: String
  )
  object CollectionLaneRow:
    given Writes[CollectionLaneRow] = Json.writes[CollectionLaneRow]

  final case class CuratedFixtureSeedRow(
      candidateName: String,
      sampleId: String,
      gameKey: String,
      targetPly: Int,
      playedSan: String,
      bucket: String,
      whyThisIsStable: String,
      likelyFailureMode: String,
      sourceManifestPath: String
  )
  object CuratedFixtureSeedRow:
    given Writes[CuratedFixtureSeedRow] = Json.writes[CuratedFixtureSeedRow]

  final case class SceneCoverageBucketPlanRow(
      bucket: String,
      collectionStatus: String,
      collectionPriority: Int,
      targetCandidateRowsMin: Int = 15,
      targetCandidateRowsMax: Int = 25,
      targetCuratedRowsMin: Int = 5,
      targetCuratedRowsMax: Int = 8,
      targetStableFixturesMin: Int = 2,
      targetStableFixturesMax: Int = 4,
      note: String
  )
  object SceneCoverageBucketPlanRow:
    given Writes[SceneCoverageBucketPlanRow] = Json.writes[SceneCoverageBucketPlanRow]

  private final case class MaterializedOpeningPlanClashSelection(
      snapshot: SliceSnapshot,
      reportRow: MaterializedOpeningPlanClashRow
  )

  private final case class CollectionLaneSelection(
      row: CollectionLaneRow,
      manifestEntries: List[SliceManifestEntry]
  )

  private final case class CuratedFixtureSeed(
      row: CuratedFixtureSeedRow,
      manifestEntries: List[SliceManifestEntry]
  )

  private final case class FixtureSeedSpec(
      candidateName: String,
      sampleId: String,
      bucket: String,
      whyThisIsStable: String,
      likelyFailureMode: String,
      sourceManifestPath: Path
  )

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val catalog =
      readJsonLines[CatalogEntry](config.catalogPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[scene-coverage] failed to read catalog `${config.catalogPath}`: $err")
          sys.exit(1)
    val quietRichRows =
      readJsonLines[QuietRichRow](config.quietRichRowsPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[scene-coverage] failed to read quiet-rich rows `${config.quietRichRowsPath}`: $err")
          sys.exit(1)

    if catalog.isEmpty then
      System.err.println(s"[scene-coverage] catalog `${config.catalogPath}` is empty")
      sys.exit(1)

    ensureDir(config.manifestPath.getParent)
    ensureDir(config.openingPlanClashRowsPath.getParent)
    ensureDir(config.curatedOpeningPlanClashRowsPath.getParent)
    ensureDir(config.prophylaxisRowsPath.getParent)
    ensureDir(config.curatedOpeningRowsPath.getParent)
    ensureDir(config.fixturePackPath.getParent)
    ensureDir(config.bucketPlanPath.getParent)

    val engine = new LocalUciEngine(config.enginePath, timeoutMs = 30000L)
    val openingIndex = buildOpeningIndex(catalog)
    val (acceptedEntries, legacyEntries) = readManifestInputs(config)

    try
      val materializedRows =
        collectOpeningPlanClashRows(
          catalog = catalog,
          config = config,
          openingIndex = openingIndex,
          engine = engine
        )

      if materializedRows.isEmpty then
        System.err.println(
          "[scene-coverage] no opening_deviation_after_middlegame_plan_clash rows were materialized; widen `--max-games` or inspect explorer availability."
        )
        sys.exit(1)

      val curatedOpeningPlanClashRows =
        buildCuratedOpeningPlanClashRows(
          config = config,
          materializedRows = materializedRows
        )

      val prophylaxisRows =
        buildProphylaxisRestraintRows(
          catalog = catalog,
          config = config,
          quietRichRows = quietRichRows,
          acceptedEntries = acceptedEntries,
          legacyEntries = legacyEntries,
          engine = engine
        )

      val curatedOpeningRows =
        buildCuratedOpeningStableDevelopmentRows(
          config = config,
          quietRichRows = quietRichRows,
          acceptedEntries = acceptedEntries,
          legacyEntries = legacyEntries
        )

      val fixtureSeeds = buildFixtureSeedPack(acceptedEntries, legacyEntries)
      val collectionManifest =
        (materializedRows.flatMap(selection =>
          manifestEntriesFor(
            SliceKind.OpeningDeviationAfterMiddlegamePlanClash,
            selection.snapshot
          )
        ) ++ prophylaxisRows.flatMap(_.manifestEntries)
          ++ curatedOpeningRows.flatMap(_.manifestEntries))
          .groupBy(_.sampleId)
          .values
          .map(_.head)
          .toList
          .sortBy(entry => (entry.gameKey, entry.targetPly, entry.surface))

      val bucketPlan =
        buildBucketPlan(
          materializedOpeningPlanClashRows = materializedRows.size,
          curatedOpeningPlanClashRows = curatedOpeningPlanClashRows.size,
          materializedProphylaxisRows = prophylaxisRows.size,
          curatedOpeningRows = curatedOpeningRows.size
        )

      writeJsonLines(config.manifestPath, collectionManifest)
      writeJsonLines(config.openingPlanClashRowsPath, materializedRows.map(_.reportRow))
      writeJsonLines(config.curatedOpeningPlanClashRowsPath, curatedOpeningPlanClashRows)
      writeJsonLines(config.prophylaxisRowsPath, prophylaxisRows.map(_.row))
      writeJsonLines(config.curatedOpeningRowsPath, curatedOpeningRows.map(_.row))
      writeJsonLines(config.fixturePackPath, fixtureSeeds.map(_.row))
      writeText(config.fixturePackMarkdownPath, renderFixturePackMarkdown(fixtureSeeds.map(_.row)))
      writeJsonLines(config.bucketPlanPath, bucketPlan)
      writeText(
        config.summaryPath,
        renderSummary(
          config = config,
          materializedRows = materializedRows.map(_.reportRow),
          curatedOpeningPlanClashRows = curatedOpeningPlanClashRows,
          prophylaxisRows = prophylaxisRows.map(_.row),
          curatedOpeningRows = curatedOpeningRows.map(_.row),
          fixtureSeeds = fixtureSeeds.map(_.row),
          manifestEntries = collectionManifest,
          bucketPlan = bucketPlan
        )
      )

      println(
        s"[scene-coverage] wrote `${config.manifestPath}` (manifestEntries=${collectionManifest.size}, openingPlanClashRows=${materializedRows.size}, curatedOpeningPlanClashRows=${curatedOpeningPlanClashRows.size}, prophylaxisRows=${prophylaxisRows.size}, curatedOpeningRows=${curatedOpeningRows.size}, fixtureSeeds=${fixtureSeeds.size})"
      )
    finally
      engine.close()

  private def collectOpeningPlanClashRows(
      catalog: List[CatalogEntry],
      config: Config,
      openingIndex: Map[String, List[CatalogEntry]],
      engine: LocalUciEngine
  ): List[MaterializedOpeningPlanClashSelection] =
    val prioritizedCatalog =
      catalog
        .filter(entry => entry.opening.nonEmpty && entry.totalPlies >= 40)
        .sortBy(entry =>
          (
            mixPriority(entry.mixBucket),
            -entry.totalPlies,
            entry.gameKey
          )
        )
        .take(config.maxGames.max(config.targetOpeningPlanClashRows))

    val results = scala.collection.mutable.ListBuffer.empty[MaterializedOpeningPlanClashSelection]
    var idx = 0
    while idx < prioritizedCatalog.size && results.size < config.targetOpeningPlanClashRows do
      val entry = prioritizedCatalog(idx)
      if idx % 10 == 0 then
        println(
          s"[scene-coverage] scanning ${idx + 1}/${prioritizedCatalog.size}: ${entry.gameKey} (${entry.mixBucket})"
        )
      collectOpeningPlanClashRowsForGame(entry, config, openingIndex, engine).headOption.foreach(results += _)
      idx += 1
    results.toList

  private def collectOpeningPlanClashRowsForGame(
      entry: CatalogEntry,
      config: Config,
      openingIndex: Map[String, List[CatalogEntry]],
      engine: LocalUciEngine
  ): List[MaterializedOpeningPlanClashSelection] =
    val pgn = Files.readString(Paths.get(entry.pgnPath))
    val plyData =
      PgnAnalysisHelper.extractPlyDataStrict(pgn) match
        case Right(value) => value
        case Left(err) =>
          throw new IllegalArgumentException(s"${entry.gameKey}: invalid PGN in catalog: $err")

    val openingRef = localOpeningReference(entry, openingIndex)
    val snapshotsByPly = scala.collection.mutable.Map.empty[Int, (SliceSnapshot, SliceSnapshot)]

    plyData
      .filter(pd => pd.ply >= 18 && pd.ply <= 36)
      .foreach { pd =>
        engine.newGame()
        val beforeVars = engine.analyze(pd.fen, config.depth, config.multiPv)
        val afterFen = lila.llm.analysis.NarrativeUtils.uciListToFen(pd.fen, List(pd.playedUci))
        engine.newGame()
        val afterVars = engine.analyze(afterFen, config.depth, config.multiPv)
        val afterEval =
          afterVars.headOption.map(best =>
            MoveEval(
              ply = pd.ply,
              cp = best.scoreCp,
              mate = best.mate,
              pv = best.moves,
              variations = afterVars
            )
          )
        val baselineSnapshot = analyzePly(entry, pd, beforeVars, afterEval)
        val enrichedSnapshot = analyzePlyWithOpeningRef(entry, pd, beforeVars, afterEval, openingRef)
        for
          base <- baselineSnapshot
          enriched <- enrichedSnapshot
        do snapshotsByPly.update(pd.ply, base -> enriched)
      }

    val enrichedCandidates =
      collectOpeningPlanClashCandidates(
        snapshotsByPly.valuesIterator.map(_._2).toList,
        perGameLimit = 1
      )

    enrichedCandidates.flatMap { candidate =>
      snapshotsByPly.get(candidate.snapshot.plyData.ply).map { case (baseline, enriched) =>
        val sampleId =
          manifestEntriesFor(
            SliceKind.OpeningDeviationAfterMiddlegamePlanClash,
            enriched
          ).find(_.surface == "bookmaker").map(_.sampleId)
            .getOrElse(
              s"${entry.gameKey}:${SliceKind.OpeningDeviationAfterMiddlegamePlanClash}:${enriched.plyData.ply}:bookmaker"
            )
        MaterializedOpeningPlanClashSelection(
          snapshot = enriched,
          reportRow =
            MaterializedOpeningPlanClashRow(
              sampleId = sampleId,
              gameKey = entry.gameKey,
              surface = "bookmaker",
              targetPly = enriched.plyData.ply,
              playedSan = enriched.plyData.playedMove,
              bucket = SliceKind.OpeningDeviationAfterMiddlegamePlanClash,
              score = candidate.score,
              baselineOpeningPrecedentAvailable =
                baseline.ctx.openingData.exists(ref => ref.sampleGames.nonEmpty || ref.topMoves.nonEmpty),
              baselineOpeningRelationClaimPresent =
                baseline.signalDigest.exists(_.openingRelationClaim.exists(_.trim.nonEmpty)),
              collectionOpeningPrecedentAvailable =
                enriched.ctx.openingData.exists(ref => ref.sampleGames.nonEmpty || ref.topMoves.nonEmpty),
              collectionOpeningRelationClaimPresent =
                enriched.signalDigest.exists(_.openingRelationClaim.exists(_.trim.nonEmpty)),
              collectionOpeningEvent = enriched.ctx.openingEvent.map(_.toString),
              reasons = candidate.reasons,
              whyMaterialized = materializationReason(baseline, enriched, candidate.reasons)
            )
        )
      }
    }

  private def collectProphylaxisRestraintRows(
      catalog: List[CatalogEntry],
      config: Config,
      engine: LocalUciEngine,
      existingKeys: Set[(String, Int, String)],
      targetRows: Int
  ): List[CollectionLaneSelection] =
    val prioritizedCatalog =
      catalog
        .filter(entry => entry.totalPlies >= 50)
        .sortBy(entry =>
          (
            mixPriority(entry.mixBucket),
            -entry.totalPlies,
            entry.gameKey
          )
        )
        .take(config.maxGames.max(targetRows * 12))

    val results = scala.collection.mutable.ListBuffer.empty[CollectionLaneSelection]
    val seenKeys = scala.collection.mutable.Set.empty[(String, Int, String)] ++ existingKeys
    var idx = 0
    while idx < prioritizedCatalog.size && results.size < targetRows do
      val entry = prioritizedCatalog(idx)
      if idx % 10 == 0 then
        println(
          s"[scene-coverage] prophylaxis scan ${idx + 1}/${prioritizedCatalog.size}: ${entry.gameKey} (${entry.mixBucket})"
        )
      collectProphylaxisRestraintRowsForGame(entry, config, engine).foreach { selection =>
        val key = (selection.row.gameKey, selection.row.targetPly, selection.row.playedSan)
        if seenKeys.add(key) && results.size < targetRows then results += selection
      }
      idx += 1
    results.toList

  private def collectProphylaxisRestraintRowsForGame(
      entry: CatalogEntry,
      config: Config,
      engine: LocalUciEngine
  ): List[CollectionLaneSelection] =
    val pgn = Files.readString(Paths.get(entry.pgnPath))
    val plyData =
      PgnAnalysisHelper.extractPlyDataStrict(pgn) match
        case Right(value) => value
        case Left(err) =>
          throw new IllegalArgumentException(s"${entry.gameKey}: invalid PGN in catalog: $err")

    val snapshots =
      plyData
        .filter(pd => pd.ply >= 16 && pd.ply <= 72)
        .flatMap { pd =>
          engine.newGame()
          val beforeVars = engine.analyze(pd.fen, config.depth, config.multiPv)
          val afterFen = lila.llm.analysis.NarrativeUtils.uciListToFen(pd.fen, List(pd.playedUci))
          engine.newGame()
          val afterVars = engine.analyze(afterFen, config.depth, config.multiPv)
          val afterEval =
            afterVars.headOption.map(best =>
              MoveEval(
                ply = pd.ply,
                cp = best.scoreCp,
                mate = best.mate,
                pv = best.moves,
                variations = afterVars
              )
            )
          analyzePly(entry, pd, beforeVars, afterEval)
        }

    collectProphylaxisRestraintCandidates(
      snapshots = snapshots,
      perGameLimit = 1
    ).map { candidate =>
      val snapshot = candidate.snapshot
      val sampleId =
        manifestEntriesFor(
          SliceKind.ProphylaxisRestraint,
          snapshot
        ).find(_.surface == "bookmaker").map(_.sampleId)
          .getOrElse(
            s"${entry.gameKey}:${SliceKind.ProphylaxisRestraint}:${snapshot.plyData.ply}:bookmaker"
          )
      CollectionLaneSelection(
        row =
          CollectionLaneRow(
            sampleId = sampleId,
            sourceSampleId = sampleId,
            gameKey = entry.gameKey,
            surface = "bookmaker",
            targetPly = snapshot.plyData.ply,
            playedSan = snapshot.plyData.playedMove,
            bucket = SliceKind.ProphylaxisRestraint,
            sourceSliceKind = SliceKind.ProphylaxisRestraint,
            sourceStatus = "collected",
            selectionMode = "collected_real_row",
            bookmakerFallbackMode = None,
            plannerSceneType = None,
            quietnessSpreadCp = None,
            directSources = Nil,
            supportSources = candidate.reasons,
            score = Some(candidate.score),
            reasons = candidate.reasons,
            whySelected =
              "Raw corpus scan found a quiet restraint scene with explicit prophylaxis/counterplay suppression evidence, so the lane can expand without relying only on overlap rows.",
            likelyFailureMode = prophylaxisFailureMode(candidate.reasons)
          ),
        manifestEntries = manifestEntriesFor(SliceKind.ProphylaxisRestraint, snapshot)
      )
    }

  private def buildOpeningIndex(catalog: List[CatalogEntry]): Map[String, List[CatalogEntry]] =
    catalog
      .flatMap(entry =>
        openingLookupKeys(entry).map(key => key -> entry)
      )
      .groupMap(_._1)(_._2)

  private def localOpeningReference(
      entry: CatalogEntry,
      openingIndex: Map[String, List[CatalogEntry]]
  ): Option[lila.llm.model.OpeningReference] =
    val peers =
      openingLookupKeys(entry)
        .flatMap(key => openingIndex.getOrElse(key, Nil))
        .filterNot(_.gameKey == entry.gameKey)
        .distinct
        .sortBy(other => (-other.totalPlies, other.gameKey))
        .take(5)

    val sampleEntries =
      if peers.nonEmpty then peers
      else List(entry)

    val sampleGames =
      sampleEntries.flatMap(catalogEntryToExplorerGame)

    Option.when(sampleGames.nonEmpty) {
      lila.llm.model.OpeningReference(
        eco = entry.eco,
        name = entry.opening.orElse(entry.openingMacroFamily),
        totalGames = sampleEntries.size,
        topMoves = Nil,
        sampleGames = sampleGames,
        description = entry.variation
      )
    }.orElse(minimalOpeningReference(entry))

  private def readManifestInputs(config: Config): (List[SliceManifestEntry], List[SliceManifestEntry]) =
    val acceptedEntries =
      readJsonLines[SliceManifestEntry](config.acceptedManifestPath) match
        case Right(value) => value
        case Left(err) =>
          throw new IllegalStateException(
            s"failed to read accepted manifest `${config.acceptedManifestPath}`: $err"
          )
    val legacyEntries =
      readJsonLines[SliceManifestEntry](config.legacyManifestPath) match
        case Right(value) => value
        case Left(err) =>
          throw new IllegalStateException(
            s"failed to read legacy manifest `${config.legacyManifestPath}`: $err"
          )
    (acceptedEntries, legacyEntries)

  private def buildCuratedOpeningPlanClashRows(
      config: Config,
      materializedRows: List[MaterializedOpeningPlanClashSelection]
  ): List[CollectionLaneRow] =
    val preferredSampleIds =
      List(
        "2024_10_20_1_16_bryakin_m_diermair_a_twic_master_classical_19:opening_deviation_after_middlegame_plan_clash:28:bookmaker",
        "2024_10_22_3_3_tsolakidou_stavroula_goryachkina_a_twic_master_classical_26:opening_deviation_after_middlegame_plan_clash:22:bookmaker",
        "maghsoodloo_parham_rapport_richard_lichess_broadcast_master_classical_58:opening_deviation_after_middlegame_plan_clash:23:bookmaker",
        "praggnanandhaa_r_gukesh_d_lichess_broadcast_master_classical_141:opening_deviation_after_middlegame_plan_clash:36:bookmaker",
        "2_3_domalchuk_jonasson_aleksandr_laurusas_tomas_lichess_broadcast_master_classical_25:opening_deviation_after_middlegame_plan_clash:26:bookmaker",
        "tarhan_adar_puranik_abhimanyu_lichess_broadcast_master_classical_118:opening_deviation_after_middlegame_plan_clash:21:bookmaker"
      )
    val preferredOrder = preferredSampleIds.zipWithIndex.toMap
    materializedRows
      .sortBy { selection =>
        val row = selection.reportRow
        (
          preferredOrder.getOrElse(row.sampleId, Int.MaxValue),
          row.playedSan.contains("x"),
          row.playedSan.contains("+"),
          row.targetPly,
          row.gameKey
        )
      }
      .take(config.targetCuratedOpeningPlanClashRows)
      .map { selection =>
        val row = selection.reportRow
        CollectionLaneRow(
          sampleId = row.sampleId,
          sourceSampleId = row.sampleId,
          gameKey = row.gameKey,
          surface = row.surface,
          targetPly = row.targetPly,
          playedSan = row.playedSan,
          bucket = row.bucket,
          sourceSliceKind = row.bucket,
          sourceStatus = "collected",
          selectionMode = "curated_signoff_candidate",
          bookmakerFallbackMode = None,
          plannerSceneType = None,
          quietnessSpreadCp = None,
          directSources = Nil,
          supportSources = row.reasons,
          score = Some(row.score),
          reasons = row.reasons,
          whySelected =
            "Collection-restored openingRelationClaim survived into a quiet middlegame plan-clash scene without depending on immediate tactical forcing, so this row is stable enough for signoff trimming.",
          likelyFailureMode = openingPlanClashFailureMode(row)
        )
      }

  private def buildProphylaxisRestraintRows(
      catalog: List[CatalogEntry],
      config: Config,
      quietRichRows: List[QuietRichRow],
      acceptedEntries: List[SliceManifestEntry],
      legacyEntries: List[SliceManifestEntry],
      engine: LocalUciEngine
  ): List[CollectionLaneSelection] =
    val seeded =
      selectQuietRichRowsForCollection(
        quietRichRows = quietRichRows,
        bucket = SliceKind.ProphylaxisRestraint,
        limit = 25,
        preferredSliceKinds =
          List(
            SliceKind.LongStructuralSqueeze,
            SliceKind.TransitionHeavyEndgames,
            SliceKind.EndgameConversion
          ),
        acceptedStatuses = Set("upstream_blocked", "non_eligible")
      ).map { quietRow =>
        materializeQuietRichRow(
          quietRow = quietRow,
          targetBucket = SliceKind.ProphylaxisRestraint,
          selectionMode = "materialized_real_row",
          acceptedEntries = acceptedEntries,
          legacyEntries = legacyEntries,
          whySelected =
            "Accepted quiet-rich real row already carries restraint/pressure support, but the exact prophylaxis_restraint lane never appeared in accepted artifacts; collection re-targets the same move into the missing bucket.",
          likelyFailureMode = prophylaxisFailureMode(quietRow)
        )
      }
    val seededKeys =
      seeded.iterator.map(selection =>
        (selection.row.gameKey, selection.row.targetPly, selection.row.playedSan)
      ).toSet
    val needed =
      if seeded.size >= 15 then 0
      else (config.targetProphylaxisRows - seeded.size).max(0)
    val collected =
      if needed == 0 then Nil
      else
        collectProphylaxisRestraintRows(
          catalog = catalog,
          config = config,
          engine = engine,
          existingKeys = seededKeys,
          targetRows = needed
        )
    (seeded ++ collected)
      .sortBy(selection => (selection.row.gameKey, selection.row.targetPly, selection.row.playedSan))
      .take(config.targetProphylaxisRows)

  private def buildCuratedOpeningStableDevelopmentRows(
      config: Config,
      quietRichRows: List[QuietRichRow],
      acceptedEntries: List[SliceManifestEntry],
      legacyEntries: List[SliceManifestEntry]
  ): List[CollectionLaneSelection] =
    selectQuietRichRowsForCollection(
      quietRichRows = quietRichRows,
      bucket = "opening_deviation_after_stable_development",
      limit = config.targetCuratedOpeningStableRows,
      preferredSliceKinds = List(SliceKind.OpeningTransition)
    ).map { quietRow =>
      materializeQuietRichRow(
        quietRow = quietRow,
        targetBucket = "opening_deviation_after_stable_development",
        selectionMode = "curated_signoff_candidate",
        acceptedEntries = acceptedEntries,
        legacyEntries = legacyEntries,
        whySelected =
          "Low-noise real row from the accepted quiet-rich slice; keep it as a stable opening translator-gap signoff candidate instead of collecting more raw opening-transition volume.",
        likelyFailureMode = "translator_missing"
      )
    }

  private[tools] def selectQuietRichRowsForCollection(
      quietRichRows: List[QuietRichRow],
      bucket: String,
      limit: Int,
      preferredSliceKinds: List[String],
      acceptedStatuses: Set[String] = Set("upstream_blocked")
  ): List[QuietRichRow] =
    val sliceKindOrder = preferredSliceKinds.zipWithIndex.toMap
    val statusOrder = List("upstream_blocked", "non_eligible").zipWithIndex.toMap
    val dedupeKeys = scala.collection.mutable.Set.empty[(String, Int, String)]
    quietRichRows
      .filter(row =>
        row.bucket == bucket &&
          row.realShard &&
          acceptedStatuses.contains(row.status)
      )
      .sortBy { row =>
        (
          statusOrder.getOrElse(row.status, Int.MaxValue),
          row.bookmakerFallbackMode.contains("planner_owned"),
          row.directSources.isEmpty,
          !row.quietnessVerified.getOrElse(false),
          row.quietnessSpreadCp.getOrElse(Int.MaxValue),
          sliceKindOrder.getOrElse(row.sliceKind, preferredSliceKinds.size),
          row.gameKey,
          row.targetPly,
          row.playedSan
        )
      }
      .filter { row =>
        dedupeKeys.add((row.gameKey, row.targetPly, row.playedSan))
      }
      .take(limit)

  private def materializeQuietRichRow(
      quietRow: QuietRichRow,
      targetBucket: String,
      selectionMode: String,
      acceptedEntries: List[SliceManifestEntry],
      legacyEntries: List[SliceManifestEntry],
      whySelected: String,
      likelyFailureMode: String
  ): CollectionLaneSelection =
    val sourceEntries = resolveManifestEntriesForSample(quietRow.sampleId, acceptedEntries, legacyEntries)
    val retargetedEntries = retargetManifestEntries(sourceEntries, targetBucket)
    val bookmakerEntry =
      retargetedEntries.find(_.surface == "bookmaker").getOrElse {
        throw new IllegalStateException(
          s"collection row `${quietRow.sampleId}` resolved no bookmaker entry while targeting `$targetBucket`"
        )
      }
    CollectionLaneSelection(
      row =
        CollectionLaneRow(
          sampleId = bookmakerEntry.sampleId,
          sourceSampleId = quietRow.sampleId,
          gameKey = quietRow.gameKey,
          surface = "bookmaker",
          targetPly = quietRow.targetPly,
          playedSan = quietRow.playedSan,
          bucket = targetBucket,
          sourceSliceKind = quietRow.sliceKind,
          sourceStatus = quietRow.status,
          selectionMode = selectionMode,
          bookmakerFallbackMode = quietRow.bookmakerFallbackMode,
          plannerSceneType = quietRow.plannerSceneType,
          quietnessSpreadCp = quietRow.quietnessSpreadCp,
          directSources = quietRow.directSources,
          supportSources = quietRow.supportSources,
          score = None,
          reasons = Nil,
          whySelected = whySelected,
          likelyFailureMode = likelyFailureMode
        ),
      manifestEntries = retargetedEntries
    )

  private[tools] def retargetManifestEntries(
      entries: List[SliceManifestEntry],
      targetBucket: String
  ): List[SliceManifestEntry] =
    entries.map { entry =>
      entry.copy(
        sampleId = s"${entry.gameKey}:$targetBucket:${entry.targetPly}:${entry.surface}",
        sliceKind = targetBucket,
        tags = (entry.tags.filterNot(_ == entry.sliceKind) :+ targetBucket).distinct
      )
    }

  private def prophylaxisFailureMode(quietRow: QuietRichRow): String =
    if quietRow.bookmakerFallbackMode.contains("planner_owned") then "owner_starvation"
    else if quietRow.directSources.isEmpty then "support_only_overuse"
    else "scene_misclassification"

  private def prophylaxisFailureMode(reasons: List[String]): String =
    if !reasons.contains("prevented_plan_now") && reasons.contains("counterplay_score_drop") then
      "scene_misclassification"
    else if !reasons.contains("prophylaxis_plan") && !reasons.contains("prophylaxis_threat") then
      "support_only_overuse"
    else "owner_starvation"

  private def openingPlanClashFailureMode(row: MaterializedOpeningPlanClashRow): String =
    if row.playedSan.contains("x") then "tactical_shadowing"
    else if row.playedSan == "O-O" || row.playedSan == "O-O-O" then "scene_misclassification"
    else "owner_starvation"

  private def buildFixtureSeedPack(
      acceptedEntries: List[SliceManifestEntry],
      legacyEntries: List[SliceManifestEntry]
  ): List[CuratedFixtureSeed] =

    fixtureSeedSpecs.flatMap { spec =>
      val entries = resolveFixtureManifestEntries(spec, acceptedEntries, legacyEntries)
      entries.find(_.surface == "bookmaker").map { bookmakerEntry =>
        CuratedFixtureSeed(
          row =
            CuratedFixtureSeedRow(
              candidateName = spec.candidateName,
              sampleId = bookmakerEntry.sampleId,
              gameKey = bookmakerEntry.gameKey,
              targetPly = bookmakerEntry.targetPly,
              playedSan = bookmakerEntry.playedSan,
              bucket = spec.bucket,
              whyThisIsStable = spec.whyThisIsStable,
              likelyFailureMode = spec.likelyFailureMode,
              sourceManifestPath = spec.sourceManifestPath.toAbsolutePath.normalize.toString
            ),
          manifestEntries = entries
        )
      }
    }

  private def resolveFixtureManifestEntries(
      spec: FixtureSeedSpec,
      acceptedEntries: List[SliceManifestEntry],
      legacyEntries: List[SliceManifestEntry]
  ): List[SliceManifestEntry] =
    resolveManifestEntriesForSample(spec.sampleId, acceptedEntries, legacyEntries)

  private def resolveManifestEntriesForSample(
      sampleId: String,
      acceptedEntries: List[SliceManifestEntry],
      legacyEntries: List[SliceManifestEntry]
  ): List[SliceManifestEntry] =
    val targetBase = sampleBaseId(sampleId)
    val accepted = acceptedEntries.filter(entry => sampleBaseId(entry.sampleId) == targetBase)
    val legacy = legacyEntries.filter(entry => sampleBaseId(entry.sampleId) == targetBase)
    val resolved = if accepted.nonEmpty then accepted else legacy
    if resolved.isEmpty then
      throw new IllegalStateException(s"fixture seed `$sampleId` not found in accepted or legacy manifests")
    resolved.sortBy(_.surface)

  private def buildBucketPlan(
      materializedOpeningPlanClashRows: Int,
      curatedOpeningPlanClashRows: Int,
      materializedProphylaxisRows: Int,
      curatedOpeningRows: Int
  ): List[SceneCoverageBucketPlanRow] =
    List(
      SceneCoverageBucketPlanRow(
        bucket = SliceKind.OpeningDeviationAfterMiddlegamePlanClash,
        collectionStatus =
          if materializedOpeningPlanClashRows >= 15 && curatedOpeningPlanClashRows >= 5 then "curate_and_fix"
          else if materializedOpeningPlanClashRows >= 15 then "expand_existing_lane"
          else if materializedOpeningPlanClashRows > 0 then "collection_needed"
          else "hard_collection_blocker",
        collectionPriority = 1,
        note =
          if materializedOpeningPlanClashRows >= 15 && curatedOpeningPlanClashRows >= 5 then
            s"Collection lane now holds $materializedOpeningPlanClashRows bookmaker rows plus $curatedOpeningPlanClashRows curated signoff rows; next work is fixing 2-4 stable fixtures, not widening the candidate pool."
          else if materializedOpeningPlanClashRows >= 15 then
            s"Collection lane now holds $materializedOpeningPlanClashRows bookmaker rows, which reaches the 15-25 candidate band; next work is curated signoff trimming rather than raw expansion."
          else if materializedOpeningPlanClashRows > 0 then
            s"Materialized into the new collection manifest with $materializedOpeningPlanClashRows bookmaker rows; widen toward the 15-25 candidate target next."
          else "Still blocked until explorer-backed opening anchors or equivalent opening-branch evidence are wired into collection."
      ),
      SceneCoverageBucketPlanRow(
        bucket = SliceKind.ProphylaxisRestraint,
        collectionStatus = if materializedProphylaxisRows >= 15 then "curate_and_fix" else if materializedProphylaxisRows > 0 then "expand_existing_lane" else "collection_needed",
        collectionPriority = 2,
        note =
          if materializedProphylaxisRows >= 15 then
            s"Real-row materialization now holds $materializedProphylaxisRows bookmaker rows, which reaches the candidate band; next work is narrowing them into 5-8 curated signoff rows."
          else if materializedProphylaxisRows > 0 then
            s"Real-row materialization now exists with $materializedProphylaxisRows bookmaker rows; expand toward 15-25 distinct restraint rows next."
          else "Current coverage is still indirect; collect real rows that carry move-linked restriction evidence, not only support-only clamps."
      ),
      SceneCoverageBucketPlanRow(
        bucket = "opening_deviation_after_stable_development",
        collectionStatus = "curate_and_fix",
        collectionPriority = 3,
        note =
          if curatedOpeningRows > 0 then
            s"Curated signoff pack now holds $curatedOpeningRows real rows; hold new collection and keep this lane focused on stable translator-gap fixtures."
          else "Rows already exist, but the first real-row fixture pack still needs 5-8 curated signoff rows behind the opening translator gap."
      ),
      SceneCoverageBucketPlanRow(
        bucket = "complex_sacrifice",
        collectionStatus = "proxy_triage_needed",
        collectionPriority = 4,
        note = "Keep one broad-proxy stress row in the first pack, then split cleaner complex-sac cases from generic forcing compensation rows."
      ),
      SceneCoverageBucketPlanRow(
        bucket = SliceKind.TransitionHeavyEndgames,
        collectionStatus = "expand_existing_lane",
        collectionPriority = 5,
        note = "Exact slice exists, but the lane still needs additional quiet transition rows that do not collapse into tactical-only captures."
      ),
      SceneCoverageBucketPlanRow(
        bucket = SliceKind.LongStructuralSqueeze,
        collectionStatus = "curate_and_fix",
        collectionPriority = 6,
        note = "Use the stable squeeze anchors already fixed here to grow toward a 5-8 row curated signoff pack."
      ),
      SceneCoverageBucketPlanRow(
        bucket = "pressure_maintenance_without_immediate_tactic",
        collectionStatus = "curate_and_fix",
        collectionPriority = 7,
        note = "Raw volume is already decent; the next pass should convert overlap rows into a stable real-row fixture pack."
      ),
      SceneCoverageBucketPlanRow(
        bucket = "slow_route_improvement",
        collectionStatus = "collection_needed",
        collectionPriority = 8,
        note = "Route-heavy rows are still indirect; collect clean middlegame and technical-ending examples before expanding wording review."
      ),
      SceneCoverageBucketPlanRow(
        bucket = "long_structural_squeeze_extended",
        collectionStatus = "collision_monitoring_needed",
        collectionPriority = 9,
        note = "This extension lane is already large enough to monitor cross-bucket spillover and tactical shadowing."
      ),
      SceneCoverageBucketPlanRow(
        bucket = "transition_heavy_endgames_extended",
        collectionStatus = "collision_monitoring_needed",
        collectionPriority = 10,
        note = "Keep monitoring only; this lane already has enough raw volume and mostly needs collision control."
      )
    )

  private def renderFixturePackMarkdown(rows: List[CuratedFixtureSeedRow]): String =
    val header =
      """# First Curated Fixture Pack
        |
        || candidate name | sampleId | gameKey | targetPly | playedSan | bucket | why this is stable | likely failure mode |
        || --- | --- | --- | --- | --- | --- | --- | --- |
        |""".stripMargin
    val body =
      rows.map { row =>
        s"| ${row.candidateName} | ${row.sampleId} | ${row.gameKey} | ${row.targetPly} | ${row.playedSan} | ${row.bucket} | ${row.whyThisIsStable} | ${row.likelyFailureMode} |"
      }.mkString("\n")
    s"$header$body\n"

  private def renderSummary(
      config: Config,
      materializedRows: List[MaterializedOpeningPlanClashRow],
      curatedOpeningPlanClashRows: List[CollectionLaneRow],
      prophylaxisRows: List[CollectionLaneRow],
      curatedOpeningRows: List[CollectionLaneRow],
      fixtureSeeds: List[CuratedFixtureSeedRow],
      manifestEntries: List[SliceManifestEntry],
      bucketPlan: List[SceneCoverageBucketPlanRow]
  ): String =
    val materializedPreview =
      materializedRows.map { row =>
        s"- `${row.sampleId}` | score=${row.score} | baselineOpeningPrecedentAvailable=${row.baselineOpeningPrecedentAvailable} | collectionOpeningPrecedentAvailable=${row.collectionOpeningPrecedentAvailable} | baselineOpeningRelationClaimPresent=${row.baselineOpeningRelationClaimPresent} | collectionOpeningRelationClaimPresent=${row.collectionOpeningRelationClaimPresent} | collectionOpeningEvent=${row.collectionOpeningEvent.getOrElse("none")}"
      }.mkString("\n")
    val curatedOpeningPlanClashPreview =
      curatedOpeningPlanClashRows.map { row =>
        s"- `${row.sampleId}` | score=${row.score.getOrElse(0)} | playedSan=${row.playedSan} | likelyFailure=${row.likelyFailureMode}"
      }.mkString("\n")
    val prophylaxisPreview =
      prophylaxisRows.map { row =>
        s"- `${row.sampleId}` <- `${row.sourceSampleId}` | sourceSlice=${row.sourceSliceKind} | fallback=${row.bookmakerFallbackMode.getOrElse("none")} | score=${row.score.map(_.toString).getOrElse("na")} | plannerScene=${row.plannerSceneType.getOrElse("none")} | likelyFailure=${row.likelyFailureMode}"
      }.mkString("\n")
    val curatedOpeningPreview =
      curatedOpeningRows.map { row =>
        s"- `${row.sampleId}` <- `${row.sourceSampleId}` | fallback=${row.bookmakerFallbackMode.getOrElse("none")} | spread=${row.quietnessSpreadCp.map(_.toString).getOrElse("na")}cp | likelyFailure=${row.likelyFailureMode}"
      }.mkString("\n")
    val fixturePreview =
      fixtureSeeds.map(row => s"- `${row.candidateName}` -> `${row.sampleId}`").mkString("\n")
    val bucketPreview =
      bucketPlan.sortBy(_.collectionPriority).map(row =>
        s"- `${row.collectionPriority}. ${row.bucket}` -> `${row.collectionStatus}`"
      ).mkString("\n")
    s"""# Scene Coverage Materialization Summary
       |
       |Materialization fix:
       |- The old collection path built `NarrativeContext` with `minimalOpeningReference(...)`, which sets `totalGames=0`, `topMoves=[]`, and `sampleGames=[]`.
       |- That meant `QuestionFirstCommentaryPlanner.openingRelationReplayClaim(...)` could not produce `openingRelationClaim`, so `opening_deviation_after_middlegame_plan_clash` never survived the strict `selectSlices` predicate into accepted manifests.
       |- The new collection path injects a corpus-backed opening precedent reference through `analyzePlyWithOpeningRef(...)`, using peer PGNs from the same opening / ECO family instead of the empty minimal reference.
       |- This materializes real rows into a new Track 4 output path without changing runtime, selector, legality, rubric, or the accepted Track 3 baseline.
       |
       |Artifacts:
       |- collection manifest: `${config.manifestPath}`
       |- opening-plan-clash rows: `${config.openingPlanClashRowsPath}`
       |- curated opening-plan-clash rows: `${config.curatedOpeningPlanClashRowsPath}`
       |- prophylaxis-restraint rows: `${config.prophylaxisRowsPath}`
       |- curated opening-stable-development rows: `${config.curatedOpeningRowsPath}`
       |- fixture seed pack jsonl: `${config.fixturePackPath}`
       |- fixture seed pack markdown: `${config.fixturePackMarkdownPath}`
       |- updated bucket plan: `${config.bucketPlanPath}`
       |
       |Materialized opening-plan-clash rows:
       |$materializedPreview
       |
       |Curated opening-plan-clash rows:
       |$curatedOpeningPlanClashPreview
       |
       |Materialized prophylaxis-restraint rows:
       |$prophylaxisPreview
       |
       |Curated opening-stable-development rows:
       |$curatedOpeningPreview
       |
       |First curated fixture pack:
       |$fixturePreview
       |
       |Updated bucket priorities:
       |$bucketPreview
       |
       |Sizing guardrail:
       |- candidate real rows per bucket: `15-25`
       |- curated signoff rows per bucket: `5-8`
       |- stable fixtures per bucket: `2-4`
       |- stop when another `20-30` rows do not widen the taxonomy
       |
       |Manifest entry count written: `${manifestEntries.size}`
       |""".stripMargin

  private def materializationReason(
      baseline: SliceSnapshot,
      enriched: SliceSnapshot,
      reasons: List[String]
  ): String =
    val baselineClaim = baseline.signalDigest.exists(_.openingRelationClaim.exists(_.trim.nonEmpty))
    val enrichedClaim = enriched.signalDigest.exists(_.openingRelationClaim.exists(_.trim.nonEmpty))
    val baselinePrecedent = baseline.ctx.openingData.exists(ref => ref.sampleGames.nonEmpty || ref.topMoves.nonEmpty)
    val enrichedPrecedent = enriched.ctx.openingData.exists(ref => ref.sampleGames.nonEmpty || ref.topMoves.nonEmpty)
    if !baselineClaim && enrichedClaim then
      "Corpus-backed opening precedent generated an openingRelationClaim on the collection path, which the baseline minimal opening reference could not produce."
    else if !baselinePrecedent && enrichedPrecedent then
      "Corpus-backed opening precedent supplied opening sample games to the collection path, so the row no longer disappears before opening-plan-clash curation."
    else if enriched.ctx.openingEvent.nonEmpty then
      "Collection path kept the row because an opening branch event and plan-clash support survived into the middlegame window."
    else
      s"Collection path kept the row on combined opening/plan evidence: ${reasons.mkString(", ")}."

  private def writeText(path: Path, text: String): Unit =
    ensureDir(path.getParent)
    Files.writeString(
      path,
      text,
      StandardCharsets.UTF_8
    )

  private def mixPriority(mixBucket: String): Int =
    mixBucket match
      case MixBucket.MasterClassical => 0
      case MixBucket.TitledPractical => 1
      case MixBucket.Club            => 2
      case MixBucket.EdgeCase        => 3
      case _                         => 4

  private def sampleBaseId(sampleId: String): String =
    sampleId.split(':').dropRight(1).mkString(":")

  private def openingLookupKeys(entry: CatalogEntry): List[String] =
    List(entry.eco, entry.opening, entry.openingMacroFamily)
      .flatten
      .map(_.trim.toLowerCase)
      .filter(_.nonEmpty)
      .distinct

  private def catalogEntryToExplorerGame(entry: CatalogEntry): Option[lila.llm.model.ExplorerGame] =
    val pgn =
      try Some(Files.readString(Paths.get(entry.pgnPath)))
      catch case NonFatal(_) => None
    val (year, month) = parseYearMonth(entry.date)
    pgn.map { raw =>
      lila.llm.model.ExplorerGame(
        id = entry.gameKey,
        winner =
          entry.result.flatMap {
            case "1-0"     => Some(chess.White)
            case "0-1"     => Some(chess.Black)
            case _         => None
          },
        white = lila.llm.model.ExplorerPlayer(entry.white.getOrElse("?"), entry.whiteElo.getOrElse(0)),
        black = lila.llm.model.ExplorerPlayer(entry.black.getOrElse("?"), entry.blackElo.getOrElse(0)),
        year = year,
        month = month,
        event = entry.event.map(_.trim).filter(_.nonEmpty),
        pgn = Some(raw)
      )
    }

  private def parseYearMonth(date: Option[String]): (Int, Int) =
    date match
      case Some(raw) =>
        val normalized = raw.trim.replace('-', '.')
        val parts = normalized.split("\\.").toList
        val year = parts.headOption.flatMap(_.toIntOption).getOrElse(0)
        val month = parts.lift(1).flatMap(_.toIntOption).getOrElse(1)
        (year, month)
      case None => (0, 1)

  private def fixtureSeedSpecs: List[FixtureSeedSpec] =
    List(
      FixtureSeedSpec(
        candidateName = "bochnicka_ke7_squeeze54",
        sampleId =
          "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67:long_structural_squeeze:54:bookmaker",
        bucket = SliceKind.LongStructuralSqueeze,
        whyThisIsStable = "Already behaves as a real-row quality seed and consistently collides with the same squeeze/restraint failure pattern.",
        likelyFailureMode = "tactical_shadowing",
        sourceManifestPath = DefaultManifestDir.resolve("cqf_real16_20260330").resolve("slice_manifest_real16.jsonl")
      ),
      FixtureSeedSpec(
        candidateName = "albornoz_a5_transition67",
        sampleId =
          "2023_07_30_1_11_albornoz_cabrera_carlos_daniel_cub_suleymenov_alisher_kaz_fide_endgame_heavy_29:transition_heavy_endgames:67:bookmaker",
        bucket = SliceKind.TransitionHeavyEndgames,
        whyThisIsStable = "It is the cleanest accepted overlap row between technical endgame transition and pressure maintenance.",
        likelyFailureMode = "cross_surface_divergence",
        sourceManifestPath = DefaultManifestDir.resolve("cqf_real16_20260330").resolve("slice_manifest_real16.jsonl")
      ),
      FixtureSeedSpec(
        candidateName = "yankelevich_qa5_squeeze55",
        sampleId =
          "2024_10_20_1_12_yankelevich_l_rodshtein_m_twic_master_classical_11:long_structural_squeeze:55:bookmaker",
        bucket = "pressure_maintenance_without_immediate_tactic",
        whyThisIsStable = "Quiet queen reroute with low tactical noise; it reproduces support-only overuse and exact-factual fallback cleanly.",
        likelyFailureMode = "support_only_overuse",
        sourceManifestPath = DefaultManifestDir.resolve("cqf_real16_20260330").resolve("slice_manifest_real16.jsonl")
      ),
      FixtureSeedSpec(
        candidateName = "bochnicka_a6_opening10",
        sampleId =
          "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67:opening_transition:10:bookmaker",
        bucket = "opening_deviation_after_stable_development",
        whyThisIsStable = "Simple and repeated opening deviation row whose current blocker is stable translator absence rather than tactical ambiguity.",
        likelyFailureMode = "translator_missing",
        sourceManifestPath = DefaultManifestDir.resolve("cqf_real16_20260330").resolve("slice_manifest_real16.jsonl")
      ),
      FixtureSeedSpec(
        candidateName = "complex_proxy_qxf3_akkarakaran17",
        sampleId =
          "2024_10_27_2_1_akkarakaran_john_veny_laurent_paoli_pierre_twic_master_classical_37:compensation_or_exchange_sac:17:bookmaker",
        bucket = "complex_sacrifice",
        whyThisIsStable = "Broad forcing proxy row that stays useful precisely because it exposes how unstable the current complex-sac proxy remains.",
        likelyFailureMode = "proxy_too_broad",
        sourceManifestPath = DefaultManifestDir.resolve("slice_manifest.jsonl")
      )
    )

  private def parseConfig(args: List[String]): Config =
    val enginePath =
      optionString(args, "--engine")
        .orElse(sys.env.get("LLM_ACTIVE_CORPUS_ENGINE_PATH").map(_.trim).filter(_.nonEmpty))
        .orElse(sys.env.get("STOCKFISH_BIN").map(_.trim).filter(_.nonEmpty))
        .map(Paths.get(_))
        .getOrElse {
          System.err.println("[scene-coverage] missing engine path. Pass `--engine /path/to/uci-engine`.")
          sys.exit(1)
        }
    Config(
      catalogPath = optionString(args, "--catalog").map(Paths.get(_)).getOrElse(DefaultCatalogDir.resolve("catalog.jsonl")),
      manifestPath =
        optionString(args, "--manifest-out").map(Paths.get(_))
          .getOrElse(DefaultCollectionManifestDir.resolve("scene_coverage_collection_manifest.jsonl")),
      openingPlanClashRowsPath =
        optionString(args, "--materialized-out").map(Paths.get(_))
          .getOrElse(DefaultCollectionReportDir.resolve("opening_deviation_after_middlegame_plan_clash_rows.jsonl")),
      curatedOpeningPlanClashRowsPath =
        optionString(args, "--curated-plan-clash-out").map(Paths.get(_))
          .getOrElse(DefaultCollectionReportDir.resolve("opening_deviation_after_middlegame_plan_clash_curated_rows.jsonl")),
      prophylaxisRowsPath =
        optionString(args, "--prophylaxis-out").map(Paths.get(_))
          .getOrElse(DefaultCollectionReportDir.resolve("prophylaxis_restraint_rows.jsonl")),
      curatedOpeningRowsPath =
        optionString(args, "--curated-opening-out").map(Paths.get(_))
          .getOrElse(DefaultCollectionReportDir.resolve("opening_deviation_after_stable_development_curated_rows.jsonl")),
      fixturePackPath =
        optionString(args, "--fixture-out").map(Paths.get(_))
          .getOrElse(DefaultCollectionReportDir.resolve("first_curated_fixture_pack.jsonl")),
      fixturePackMarkdownPath =
        optionString(args, "--fixture-md-out").map(Paths.get(_))
          .getOrElse(DefaultCollectionReportDir.resolve("first_curated_fixture_pack.md")),
      bucketPlanPath =
        optionString(args, "--bucket-plan-out").map(Paths.get(_))
          .getOrElse(DefaultCollectionReportDir.resolve("scene_coverage_bucket_plan.jsonl")),
      summaryPath =
        optionString(args, "--summary-out").map(Paths.get(_))
          .getOrElse(DefaultCollectionReportDir.resolve("scene_coverage_materialization_summary.md")),
      acceptedManifestPath =
        optionString(args, "--accepted-manifest").map(Paths.get(_))
          .getOrElse(DefaultManifestDir.resolve("cqf_real16_20260330").resolve("slice_manifest_real16.jsonl")),
      legacyManifestPath =
        optionString(args, "--legacy-manifest").map(Paths.get(_))
          .getOrElse(DefaultManifestDir.resolve("slice_manifest.jsonl")),
      quietRichRowsPath =
        optionString(args, "--quiet-rich-rows").map(Paths.get(_))
          .getOrElse(DefaultReportDir.resolve("cqf_track3_real16_20260330").resolve("cqf_track3_quiet_rich_rows.jsonl")),
      depth = optionInt(args, "--depth").getOrElse(8).max(8),
      multiPv = optionInt(args, "--multi-pv").orElse(optionInt(args, "--multiPv")).getOrElse(2).max(1),
      maxGames = optionInt(args, "--max-games").getOrElse(120).max(10),
      targetOpeningPlanClashRows = optionInt(args, "--target-rows").getOrElse(18).max(1),
      targetCuratedOpeningPlanClashRows = optionInt(args, "--curated-plan-clash-rows").getOrElse(6).max(1),
      targetProphylaxisRows = optionInt(args, "--target-prophylaxis-rows").getOrElse(18).max(1),
      targetCuratedOpeningStableRows = optionInt(args, "--curated-opening-rows").getOrElse(6).max(1),
      enginePath = enginePath
    )

  private def optionString(args: List[String], name: String): Option[String] =
    args.sliding(2).collectFirst { case List(flag, value) if flag == name => value.trim }.filter(_.nonEmpty)

  private def optionInt(args: List[String], name: String): Option[Int] =
    optionString(args, name).flatMap(_.toIntOption)
