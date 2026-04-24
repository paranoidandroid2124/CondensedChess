package lila.commentary.source.opening

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Paths }

import chess.Ply
import chess.format.{ Fen, Uci }
import chess.variant

import play.api.libs.json.*

final case class OpeningSourceArtifact(
    path: String,
    storagePolicy: String,
    checksum: Option[String],
    rowCount: Option[Int]
)

object OpeningSourceArtifact:
  def fromJson(json: JsValue): OpeningSourceArtifact =
    OpeningSourceArtifact(
      path = text(json, "path").getOrElse(""),
      storagePolicy = text(json, "storagePolicy").getOrElse(""),
      checksum = text(json, "checksum"),
      rowCount = (json \ "rowCount").asOpt[Int]
    )

final case class OpeningSourceManifest(
    sourceId: String,
    sourceFamily: String,
    sourceType: String,
    sourceUse: Option[String],
    sourceName: Option[String],
    sourceVersion: Option[String],
    parserVersion: Option[String],
    sourceUrl: Option[String],
    licenseName: Option[String],
    licenseUrl: Option[String],
    licenseVerifiedAt: Option[String],
    redistribution: Option[String],
    derivedData: Option[String],
    attributionRequired: Boolean,
    shareAlikeRequired: Option[Boolean],
    attributionText: Option[String],
    licenseNotice: Option[String],
    rawStoragePolicy: Option[String],
    generatedStoragePolicy: Option[String],
    sourceChecksum: Option[String],
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
    rawArtifacts: Vector[OpeningSourceArtifact],
    generatedArtifacts: Vector[OpeningSourceArtifact],
    sourceYear: Option[Int],
    sourceMonth: Option[Int],
    yearMonth: Option[String],
    ratingBucket: Option[String],
    timeControlBucket: Option[String],
    variant: Option[String],
    ratedFilter: Option[String],
    ratedOnly: Option[Boolean],
    sampleSizeThreshold: Option[Int]
)

object OpeningSourceManifest:
  def fromJson(json: JsValue): OpeningSourceManifest =
    OpeningSourceManifest(
      sourceId = text(json, "sourceId").getOrElse(""),
      sourceFamily = text(json, "sourceFamily").getOrElse(""),
      sourceType = text(json, "sourceType").getOrElse(""),
      sourceUse = text(json, "sourceUse"),
      sourceName = text(json, "sourceName"),
      sourceVersion = text(json, "sourceVersion"),
      parserVersion = text(json, "parserVersion"),
      sourceUrl = text(json, "sourceUrl"),
      licenseName = text(json, "licenseName"),
      licenseUrl = text(json, "licenseUrl"),
      licenseVerifiedAt = text(json, "licenseVerifiedAt"),
      redistribution = text(json, "redistribution"),
      derivedData = text(json, "derivedData"),
      attributionRequired = (json \ "attributionRequired").asOpt[Boolean].getOrElse(false),
      shareAlikeRequired = (json \ "shareAlikeRequired").asOpt[Boolean],
      attributionText = text(json, "attributionText"),
      licenseNotice = text(json, "licenseNotice"),
      rawStoragePolicy = text(json, "rawStoragePolicy"),
      generatedStoragePolicy = text(json, "generatedStoragePolicy"),
      sourceChecksum = text(json, "sourceChecksum"),
      aggregateUse = text(json, "aggregateUse"),
      sourceScope = text(json, "sourceScope"),
      playEnvironment = text(json, "playEnvironment"),
      playerLevel = text(json, "playerLevel"),
      ratingSystem = text(json, "ratingSystem"),
      minElo = (json \ "minElo").asOpt[Int],
      titlePolicy = text(json, "titlePolicy"),
      timeScope = text(json, "timeScope"),
      periodStart = text(json, "periodStart"),
      periodEnd = text(json, "periodEnd"),
      timeControlScope = text(json, "timeControlScope"),
      perPositionSampleSizeThreshold = (json \ "perPositionSampleSizeThreshold").asOpt[Int],
      dedupePolicy = text(json, "dedupePolicy"),
      annotationPolicy = text(json, "annotationPolicy"),
      rawArtifacts = artifactVector(json, "rawArtifacts"),
      generatedArtifacts = artifactVector(json, "generatedArtifacts"),
      sourceYear = (json \ "sourceYear").asOpt[Int],
      sourceMonth = (json \ "sourceMonth").asOpt[Int],
      yearMonth = text(json, "yearMonth"),
      ratingBucket = text(json, "ratingBucket"),
      timeControlBucket = text(json, "timeControlBucket"),
      variant = text(json, "variant"),
      ratedFilter = text(json, "ratedFilter"),
      ratedOnly = (json \ "ratedOnly").asOpt[Boolean],
      sampleSizeThreshold = (json \ "sampleSizeThreshold").asOpt[Int]
    )

final case class OpeningLine(
    lineId: String,
    sourceId: String,
    ecoCode: String,
    openingFamily: String,
    openingVariation: String,
    names: Vector[String],
    moveOrder: Vector[String],
    lineKey: String,
    authority: String,
    negativeBoundaries: Vector[String]
)

object OpeningLine:
  def fromJson(json: JsValue): OpeningLine =
    OpeningLine(
      lineId = text(json, "id").getOrElse(""),
      sourceId = text(json, "sourceId").getOrElse(""),
      ecoCode = text(json, "eco").getOrElse(""),
      openingFamily = text(json, "family").getOrElse(""),
      openingVariation = text(json, "variation").getOrElse(""),
      names = stringVector(json, "names"),
      moveOrder = stringVector(json, "moveOrder"),
      lineKey = text(json, "lineKey").getOrElse(""),
      authority = text(json, "authority").getOrElse(""),
      negativeBoundaries = stringVector(json, "negativeBoundaries")
    )

final case class OpeningPositionKey(value: String)

object OpeningPositionKey:
  def fromFen(fen: String): Either[String, OpeningPositionKey] =
    try
      val fields = Fen.Full.clean(fen).toString.split(" ").take(4).mkString(" ")
      Right(OpeningPositionKey(s"std:$fields"))
    catch case error: Throwable => Left(s"invalid FEN for opening position key: ${error.getMessage}")

  def fromPosition(position: chess.Position, ply: Int): Either[String, OpeningPositionKey] =
    fromFen(Fen.write(position, Ply(ply).fullMoveNumber).value)

final case class OpeningPosition(
    positionId: String,
    sourceId: String,
    lineId: String,
    ecoCode: String,
    name: String,
    positionKey: OpeningPositionKey,
    fen: String,
    ply: Int,
    moveOrder: Vector[String],
    transpositionState: String,
    status: String,
    authority: String,
    contextRefs: Vector[String]
)

object OpeningPosition:
  def fromJson(json: JsValue): OpeningPosition =
    OpeningPosition(
      positionId = text(json, "id").getOrElse(""),
      sourceId = text(json, "sourceId").getOrElse(""),
      lineId = text(json, "lineId").getOrElse(""),
      ecoCode = text(json, "eco").getOrElse(""),
      name = text(json, "name").getOrElse(""),
      positionKey = OpeningPositionKey(text(json, "positionKey").getOrElse("")),
      fen = text(json, "fen").getOrElse(""),
      ply = (json \ "ply").asOpt[Int].getOrElse(-1),
      moveOrder = stringVector(json, "moveOrder"),
      transpositionState = text(json, "transpositionState").getOrElse(""),
      status = text(json, "status").getOrElse(""),
      authority = text(json, "authority").getOrElse(""),
      contextRefs = stringVector(json, "contextRefs")
    )

final case class OpeningMoveStat(
    statId: String,
    sourceId: String,
    positionKey: OpeningPositionKey,
    move: String,
    sampleSize: Int,
    whiteWins: Int,
    draws: Int,
    blackWins: Int,
    frequency: Double,
    candidateKind: String,
    authority: String,
    negativeBoundaries: Vector[String]
)

object OpeningMoveStat:
  private val allowedFields = Set(
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

  def fromJson(json: JsValue): OpeningMoveStat =
    rejectUnknownFields(json)
    OpeningMoveStat(
      statId = text(json, "id").getOrElse(""),
      sourceId = text(json, "sourceId").getOrElse(""),
      positionKey = OpeningPositionKey(text(json, "positionKey").getOrElse("")),
      move = text(json, "move").getOrElse(""),
      sampleSize = (json \ "sampleSize").asOpt[Int].getOrElse(0),
      whiteWins = (json \ "whiteWins").asOpt[Int].getOrElse(0),
      draws = (json \ "draws").asOpt[Int].getOrElse(0),
      blackWins = (json \ "blackWins").asOpt[Int].getOrElse(0),
      frequency = (json \ "frequency").asOpt[Double].getOrElse(Double.NaN),
      candidateKind = text(json, "candidateKind").getOrElse(""),
      authority = text(json, "authority").getOrElse(""),
      negativeBoundaries = stringVector(json, "negativeBoundaries")
    )

  private def rejectUnknownFields(json: JsValue): Unit =
    val obj = json.asOpt[JsObject].getOrElse(throw IllegalArgumentException("Opening move stat row must be a JSON object"))
    val id = text(json, "id").getOrElse("unknown")
    val extra = obj.keys.filterNot(allowedFields.contains).toVector.sorted
    if extra.nonEmpty then throw IllegalArgumentException(s"Opening move stat $id uses unsupported fields: ${extra.mkString(", ")}")

final case class OpeningAlias(
    aliasId: String,
    sourceId: String,
    sourceName: String,
    canonicalName: String,
    openingFamily: String,
    openingVariation: String,
    displayName: String,
    contextName: String,
    aliasKind: String,
    sourceRefs: Vector[String],
    negativeBoundaries: Vector[String]
)

object OpeningAlias:
  private val allowedFields = Set(
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

  def fromJson(json: JsValue): OpeningAlias =
    rejectUnknownFields(json)
    OpeningAlias(
      aliasId = text(json, "id").getOrElse(""),
      sourceId = text(json, "sourceId").getOrElse(""),
      sourceName = text(json, "sourceName").getOrElse(""),
      canonicalName = text(json, "canonicalName").getOrElse(""),
      openingFamily = text(json, "openingFamily").getOrElse(""),
      openingVariation = text(json, "openingVariation").getOrElse(""),
      displayName = text(json, "displayName").getOrElse(""),
      contextName = text(json, "contextName").getOrElse(""),
      aliasKind = text(json, "aliasKind").getOrElse(""),
      sourceRefs = stringVector(json, "sourceRefs"),
      negativeBoundaries = stringVector(json, "negativeBoundaries")
    )

  private def rejectUnknownFields(json: JsValue): Unit =
    val obj = json.asOpt[JsObject].getOrElse(throw IllegalArgumentException("Opening alias row must be a JSON object"))
    val id = text(json, "id").getOrElse("unknown")
    val extra = obj.keys.filterNot(allowedFields.contains).toVector.sorted
    if extra.nonEmpty then throw IllegalArgumentException(s"Opening alias $id uses unsupported fields: ${extra.mkString(", ")}")

final case class OpeningCandidate(
    sourceId: String,
    positionKey: OpeningPositionKey,
    move: String,
    candidateRole: String,
    currentPositionTruth: Boolean
)

object OpeningCandidate:
  private val allowedRoles = Set("statistical_reference", "context_reference")
  private val forbiddenWords = Vector("best", "theory", "truth", "forced", "objective", "oracle")

  def fromMoveStat(stat: OpeningMoveStat): Either[String, OpeningCandidate] =
    val normalized = stat.candidateKind.toLowerCase
    if forbiddenWords.exists(normalized.contains) then
      Left(s"opening candidate ${stat.statId} must remain statistic/reference only")
    else if !allowedRoles.contains(stat.candidateKind) then
      Left(s"opening candidate ${stat.statId} uses unsupported candidateRole ${stat.candidateKind}")
    else
      Right(
        OpeningCandidate(
          sourceId = stat.sourceId,
          positionKey = stat.positionKey,
          move = stat.move,
          candidateRole = stat.candidateKind,
          currentPositionTruth = false
        )
      )

final case class OpeningRejectFixture(
    id: String,
    rejectKind: String,
    rowType: String,
    payload: JsValue
)

object OpeningRejectFixture:
  def fromJson(json: JsValue): OpeningRejectFixture =
    OpeningRejectFixture(
      id = text(json, "id").getOrElse(""),
      rejectKind = text(json, "rejectKind").getOrElse(""),
      rowType = text(json, "rowType").getOrElse(""),
      payload = (json \ "payload").getOrElse(Json.obj())
    )

final case class OpeningLineReplay(
    finalPositionKey: OpeningPositionKey,
    finalFen: Fen.Full,
    finalPly: Int
)

object OpeningLineReplay:
  private val initialFen = Fen.Full.clean("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")

  def replay(moveOrder: Vector[String]): Either[String, OpeningLineReplay] =
    if moveOrder.isEmpty then Left("opening line must declare moveOrder")
    else
      val start = Fen.read(variant.Standard, initialFen).toRight("standard initial position failed to parse")
      val finalPosition =
        moveOrder.zipWithIndex.foldLeft(start):
          case (acc, (rawMove, index)) =>
            acc.flatMap: position =>
              parseUci(rawMove).flatMap: move =>
                position
                  .move(move)
                  .map(_.after.position)
                  .left
                  .map(error => s"illegal move $rawMove at ply ${index + 1}: $error")

      finalPosition.flatMap: position =>
        val ply = moveOrder.size
        val fen = Fen.write(position, Ply(ply).fullMoveNumber)
        OpeningPositionKey.fromFen(fen.value).map(key => OpeningLineReplay(key, fen, ply))

  private def parseUci(rawMove: String): Either[String, Uci.Move] =
    Uci(rawMove) match
      case Some(move: Uci.Move) => Right(move)
      case Some(_)              => Left(s"unsupported non-move UCI $rawMove")
      case None                 => Left(s"invalid UCI move $rawMove")

final case class OpeningPositionIndex(
    positionsByKey: Map[OpeningPositionKey, Vector[OpeningPosition]],
    downgradedPositions: Vector[OpeningPosition],
    moveStatsByKey: Map[OpeningPositionKey, Vector[OpeningMoveStat]]
)

object OpeningIndexBuilder:
  def build(
      lines: Vector[OpeningLine],
      positions: Vector[OpeningPosition],
      moveStats: Vector[OpeningMoveStat],
      manifests: Vector[OpeningSourceManifest]
  ): Either[String, OpeningPositionIndex] =
    for
      _ <- validateAll(lines)(OpeningContextValidator.validateLine(_, manifests))
      _ <- validateAll(positions)(OpeningContextValidator.validatePosition(_, lines, manifests))
      _ <- validateAll(moveStats)(OpeningContextValidator.validateMoveStat(_, positions, manifests))
      indexed = positions.filter(_.status == "indexed")
      _ <- rejectDuplicateIndexedKeys(indexed)
    yield OpeningPositionIndex(
      positionsByKey = indexed.groupBy(_.positionKey).view.mapValues(_.sortBy(_.positionId)).toMap,
      downgradedPositions = positions.filter(_.status == "downgraded").sortBy(_.positionId),
      moveStatsByKey = moveStats.groupBy(_.positionKey)
    )

  def validateOutputPath(path: String): Either[String, String] =
    val workspace = Paths.get("").toAbsolutePath.normalize()
    val resolved = workspace.resolve(path).normalize()
    val allowedOutputRoots = Vector(
      workspace.resolve("tmp/commentary-opening").normalize(),
      workspace.resolve("modules/commentary/.local/opening").normalize(),
      workspace.resolve("modules/commentary/src/test/resources/commentary-corpus/generated-opening").normalize()
    )
    if allowedOutputRoots.exists(root => resolved == root || resolved.startsWith(root)) then
      Right(workspace.relativize(resolved).toString.replace('\\', '/'))
    else Left(s"Opening output path $path is outside ignored opening local output roots")

  private def rejectDuplicateIndexedKeys(positions: Vector[OpeningPosition]): Either[String, Unit] =
    val duplicate =
      positions
        .groupBy(_.positionKey)
        .collectFirst:
          case (key, rows) if rows.map(_.lineId).distinct.size > 1 => key -> rows
    duplicate match
      case Some((key, rows)) =>
        Left(s"Opening position key ${key.value} is indexed by ambiguous lineIds ${rows.map(_.lineId).distinct.mkString(", ")}")
      case None => Right(())

object OpeningContextValidator:
  private val allowedSourceTypes =
    Set("ecoTaxonomy", "masterGameDb", "publicPgn", "aggregateGameDb", "trend_stat", "polyglotBook", "curatedLineSet")
  private val allowedLicenses = Set("CC0-1.0", "CC-BY-SA-4.0", "public-domain-facts")
  private val allowedRedistribution = Set("allowed", "derived_only")
  private val allowedDerivedData = Set("allowed")
  private val allowedRawStorage = Set("localOnly", "externalOnly")
  private val allowedGeneratedStorage = Set("localOnly", "localCache")
  private val allowedCandidateKinds = Set("statistical_reference", "context_reference")
  private val allowedAliasKinds = Set("display_alias", "context_alias", "structure_alias")
  private val allowedSourceUses = Set("taxonomy_reference", "pipeline_smoke", "online_trend", "master_reference")
  private val allowedAggregateUses = Set("pipeline_smoke", "online_trend_stat", "master_reference_stat")
  private val allowedSourceScopes = Set(
    "single_month",
    "date_range",
    "historical_corpus",
    "recent_broadcast",
    "full_month",
    "partial_month",
    "streamed_sample",
    "mixed"
  )
  private val allowedPlayEnvironments = Set("otb", "online", "mixed")
  private val allowedPlayerLevels = Set("master", "titled", "elite")
  private val allowedRatingSystems = Set("fide", "lichess", "chesscom", "mixed")
  private val allowedTimeScopes = Set("single_month", "date_range", "all_time")
  private val allowedTimeControlScopes = Set("classical_rapid", "classical", "rapid", "fast", "mixed")
  private val allowedDedupePolicies = Set("stable_game_id_or_normalized_pgn_hash")
  private val allowedAnnotationPolicies = Set("strip_comments_report_engine_eval", "strip_comments_reject_engine_eval")
  private val forbiddenCandidateWords = Vector("best", "theory", "truth", "forced", "objective", "oracle")
  private val forbiddenAliasWords =
    Vector(
      "best",
      "theory",
      "truth",
      "forced",
      "result",
      "win",
      "winning",
      "draw",
      "drawn",
      "loss",
      "won",
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

  def validateManifest(manifest: OpeningSourceManifest): Either[String, OpeningSourceManifest] =
    for
      _ <- requireNonEmpty(manifest.sourceId, "sourceId")
      _ <- requireExact(manifest.sourceFamily, "opening", s"source ${manifest.sourceId} must be opening family")
      _ <- requireAllowed(manifest.sourceType, allowedSourceTypes, s"source ${manifest.sourceId} sourceType")
      _ <- validateOpeningSourceUse(manifest)
      _ <- requirePresent(manifest.sourceName, "sourceName")
      _ <- requirePresent(manifest.sourceVersion, "sourceVersion")
      _ <- requirePresent(manifest.parserVersion, "parserVersion")
      _ <- requirePresent(manifest.sourceUrl, "sourceUrl")
      _ <- requirePresent(manifest.licenseName, "licenseName")
      _ <- requireAllowed(manifest.licenseName.getOrElse(""), allowedLicenses, s"source ${manifest.sourceId} licenseName")
      _ <- requirePresent(manifest.licenseUrl, "licenseUrl")
      _ <- requirePresent(manifest.licenseVerifiedAt, "licenseVerifiedAt")
      _ <- requireAllowed(manifest.redistribution.getOrElse(""), allowedRedistribution, s"source ${manifest.sourceId} redistribution")
      _ <- requireAllowed(manifest.derivedData.getOrElse(""), allowedDerivedData, s"source ${manifest.sourceId} derivedData")
      _ <- requireAllowed(manifest.rawStoragePolicy.getOrElse(""), allowedRawStorage, s"source ${manifest.sourceId} rawStoragePolicy")
      _ <- requireAllowed(
        manifest.generatedStoragePolicy.getOrElse(""),
        allowedGeneratedStorage,
        s"source ${manifest.sourceId} generatedStoragePolicy"
      )
      _ <- rejectCommittedRawPolicy(manifest)
      _ <- validateArtifacts(manifest)
      _ <- validateOpeningAggregateUse(manifest)
    yield manifest

  def validateTrendSource(manifest: OpeningSourceManifest): Either[String, Option[String]] =
    if manifest.sourceType == "trend_stat" || manifest.aggregateUse.contains("online_trend_stat") then
      for
        _ <- requireExact(
          manifest.sourceUse.getOrElse(""),
          "online_trend",
          s"source ${manifest.sourceId} online trend sourceType must use sourceUse online_trend"
        )
        _ <-
          if manifest.sourceType == "trend_stat" then
            requireExact(
              manifest.aggregateUse.getOrElse(""),
              "online_trend_stat",
              s"source ${manifest.sourceId} trend_stat sourceType must use aggregateUse online_trend_stat"
            )
          else Right(())
        _ <- requirePresent(manifest.sourceYear.map(_.toString), "sourceYear")
        year <- manifest.sourceYear.toRight("missing sourceYear")
        _ <- Either.cond(year >= 2024, (), s"source ${manifest.sourceId} trend_stat sourceYear must be recent")
        _ <- requirePresent(manifest.sourceMonth.map(_.toString), "sourceMonth")
        month <- manifest.sourceMonth.toRight("missing sourceMonth")
        _ <- Either.cond(month >= 1 && month <= 12, (), s"source ${manifest.sourceId} trend_stat sourceMonth out of range")
        _ <- requirePresent(manifest.yearMonth, "yearMonth")
        _ <- Either.cond(
          manifest.yearMonth.contains(f"$year%04d-$month%02d"),
          (),
          s"source ${manifest.sourceId} trend_stat yearMonth must match sourceYear/sourceMonth"
        )
        _ <- requirePresent(manifest.ratingBucket, "ratingBucket")
        _ <- requirePresent(manifest.timeControlBucket, "timeControlBucket")
        _ <- requireExact(manifest.variant.getOrElse(""), "standard", s"source ${manifest.sourceId} trend_stat variant must be standard")
        _ <- requirePresent(manifest.ratedFilter, "ratedFilter")
        _ <- Either.cond(
          manifest.ratedOnly.contains(true),
          (),
          s"source ${manifest.sourceId} trend_stat must declare ratedOnly true"
        )
        _ <- Either.cond(
          manifest.sampleSizeThreshold.exists(_ > 0),
          (),
          s"source ${manifest.sourceId} trend_stat must declare positive sampleSizeThreshold"
        )
        _ <- requirePresent(manifest.parserVersion, "parserVersion")
        _ <- Either.cond(
          manifest.generatedStoragePolicy.exists(allowedGeneratedStorage.contains),
          (),
          s"source ${manifest.sourceId} trend output must stay local-only"
        )
        _ <- requirePresent(manifest.sourceChecksum, "sourceChecksum")
      yield Some("online_trend_stat")
    else Right(None)

  def validateMasterReferenceSource(manifest: OpeningSourceManifest): Either[String, Option[String]] =
    if manifest.sourceUse.contains("master_reference") || manifest.aggregateUse.contains("master_reference_stat") then
      for
        _ <- requireExact(manifest.sourceType, "masterGameDb", s"source ${manifest.sourceId} master_reference must use sourceType masterGameDb")
        _ <- requireExact(
          manifest.aggregateUse.getOrElse(""),
          "master_reference_stat",
          s"source ${manifest.sourceId} master_reference must use aggregateUse master_reference_stat"
        )
        _ <- requireExact(manifest.sourceUse.getOrElse(""), "master_reference", s"source ${manifest.sourceId} must declare sourceUse master_reference")
        _ <- requireExact(manifest.licenseName.getOrElse(""), "CC-BY-SA-4.0", s"source ${manifest.sourceId} master_reference must use CC-BY-SA-4.0")
        _ <- Either.cond(manifest.attributionRequired, (), s"source ${manifest.sourceId} master_reference must require attribution")
        _ <- Either.cond(manifest.shareAlikeRequired.contains(true), (), s"source ${manifest.sourceId} master_reference must require shareAlike")
        _ <- requirePresent(manifest.attributionText, "attributionText")
        _ <- requirePresent(manifest.licenseNotice, "licenseNotice")
        _ <- requireAllowed(manifest.sourceScope.getOrElse(""), allowedSourceScopes, s"source ${manifest.sourceId} sourceScope")
        _ <- requireAllowed(manifest.playEnvironment.getOrElse(""), allowedPlayEnvironments, s"source ${manifest.sourceId} playEnvironment")
        _ <-
          if manifest.playEnvironment.contains("mixed") then
            requireExact(manifest.sourceScope.getOrElse(""), "mixed", s"source ${manifest.sourceId} mixed playEnvironment requires explicit mixed scope")
          else Right(())
        _ <- requireAllowed(manifest.playerLevel.getOrElse(""), allowedPlayerLevels, s"source ${manifest.sourceId} playerLevel")
        _ <- requireAllowed(manifest.ratingSystem.getOrElse(""), allowedRatingSystems, s"source ${manifest.sourceId} ratingSystem")
        _ <- Either.cond(
          manifest.minElo.exists(_ > 0) || manifest.titlePolicy.exists(_.trim.nonEmpty),
          (),
          s"source ${manifest.sourceId} master_reference must declare titlePolicy or minElo"
        )
        _ <- manifest.titlePolicy.fold(Right(()))(value => requireExact(value, "title_or_min_elo", s"source ${manifest.sourceId} titlePolicy uses unsupported value $value"))
        _ <- requireAllowed(manifest.timeScope.getOrElse(""), allowedTimeScopes, s"source ${manifest.sourceId} timeScope")
        _ <- requirePresent(manifest.periodStart, "periodStart")
        _ <- requirePresent(manifest.periodEnd, "periodEnd")
        _ <- requireAllowed(manifest.timeControlScope.getOrElse(""), allowedTimeControlScopes, s"source ${manifest.sourceId} timeControlScope")
        _ <- requireExact(
          manifest.timeControlScope.getOrElse(""),
          "classical_rapid",
          s"source ${manifest.sourceId} master_reference timeControlScope must be classical_rapid"
        )
        _ <- Either.cond(
          manifest.perPositionSampleSizeThreshold.exists(_ > 0),
          (),
          s"source ${manifest.sourceId} master_reference must declare positive perPositionSampleSizeThreshold"
        )
        _ <- requireAllowed(manifest.dedupePolicy.getOrElse(""), allowedDedupePolicies, s"source ${manifest.sourceId} dedupePolicy")
        _ <- requireAllowed(manifest.annotationPolicy.getOrElse(""), allowedAnnotationPolicies, s"source ${manifest.sourceId} annotationPolicy")
        _ <- requirePresent(manifest.parserVersion, "parserVersion")
        _ <- requirePresent(manifest.sourceChecksum, "sourceChecksum")
        _ <- requireExact(manifest.rawStoragePolicy.getOrElse(""), "localOnly", s"source ${manifest.sourceId} master_reference raw artifacts must stay local-only")
        _ <- requireExact(manifest.generatedStoragePolicy.getOrElse(""), "localOnly", s"source ${manifest.sourceId} master_reference generated artifacts must stay local-only")
      yield Some("master_reference_stat")
    else Right(None)

  def validateLine(
      line: OpeningLine,
      manifests: Vector[OpeningSourceManifest]
  ): Either[String, OpeningLine] =
    for
      manifest <- requireOpeningSource(line.sourceId, manifests)
      _ <- requireExact(
        manifest.sourceUse.getOrElse(""),
        "taxonomy_reference",
        s"opening line ${line.lineId} source must use taxonomy_reference"
      )
      _ <- requireNonEmpty(line.lineId, "lineId")
      _ <- requireEco(line.ecoCode, line.lineId)
      _ <- requireNonEmpty(line.openingFamily, s"opening line ${line.lineId} family")
      _ <- requireNonEmpty(line.openingVariation, s"opening line ${line.lineId} variation")
      _ <- requireVector(line.names, s"opening line ${line.lineId} names")
      _ <- requireExact(line.authority, "opening_reference", s"opening line ${line.lineId} authority")
      _ <- OpeningLineReplay.replay(line.moveOrder)
    yield line

  def validatePosition(
      position: OpeningPosition,
      lines: Vector[OpeningLine],
      manifests: Vector[OpeningSourceManifest]
  ): Either[String, OpeningPosition] =
    for
      _ <- requireOpeningSource(position.sourceId, manifests)
      line <- lines.find(_.lineId == position.lineId).toRight(s"opening position ${position.positionId} references unknown lineId ${position.lineId}")
      _ <- requireExact(position.ecoCode, line.ecoCode, s"opening position ${position.positionId} ECO must match line")
      _ <- Either.cond(line.names.contains(position.name), (), s"opening position ${position.positionId} name must match line")
      _ <- requireExact(position.authority, "opening_context", s"opening position ${position.positionId} authority")
      _ <- requireNonEmpty(position.positionKey.value, s"opening position ${position.positionId} must declare exact positionKey")
      keyFromFen <- OpeningPositionKey.fromFen(position.fen)
      _ <- requireExact(position.positionKey.value, keyFromFen.value, s"opening position ${position.positionId} positionKey must match FEN")
      replay <- OpeningLineReplay.replay(position.moveOrder)
      _ <- requireExact(position.ply, replay.finalPly, s"opening position ${position.positionId} ply must match moveOrder length")
      _ <- requireExact(position.positionKey.value, replay.finalPositionKey.value, s"opening position ${position.positionId} positionKey must match replay")
      _ <- requireAllowed(position.transpositionState, Set("canonical", "transposed", "ambiguous"), s"opening position ${position.positionId} transpositionState")
      _ <- requireAllowed(position.status, Set("indexed", "downgraded"), s"opening position ${position.positionId} status")
      _ <-
        if position.transpositionState == "ambiguous" && position.status != "downgraded" then
          Left(s"opening position ${position.positionId} ambiguous transposition must be downgraded")
        else Right(())
    yield position

  def validateMoveStat(
      stat: OpeningMoveStat,
      positions: Vector[OpeningPosition],
      manifests: Vector[OpeningSourceManifest]
  ): Either[String, OpeningMoveStat] =
    for
      _ <- requireOpeningSource(stat.sourceId, manifests)
      _ <- requireNonEmpty(stat.statId, "statId")
      _ <- requireUciMove(stat.move, s"opening move stat ${stat.statId} move")
      _ <- requireExact(stat.authority, "opening_statistic", s"opening move stat ${stat.statId} authority")
      _ <- validateCandidateKind(stat)
      _ <- requireIndexedPosition(stat.positionKey, positions, stat.statId)
      _ <- validateCounts(stat)
    yield stat

  def validateAlias(
      alias: OpeningAlias,
      lines: Vector[OpeningLine],
      manifests: Vector[OpeningSourceManifest]
  ): Either[String, OpeningAlias] =
    for
      _ <- requireOpeningSource(alias.sourceId, manifests)
      _ <- requireNonEmpty(alias.aliasId, "aliasId")
      _ <- requireAllowed(alias.aliasKind, allowedAliasKinds, s"opening alias ${alias.aliasId} aliasKind")
      _ <- requireVector(alias.sourceRefs, s"opening alias ${alias.aliasId} sourceRefs")
      _ <- Either.cond(alias.sourceRefs.size == 1, (), s"opening alias ${alias.aliasId} must reference exactly one taxonomy row")
      line <- lines.find(_.lineId == alias.sourceRefs.head).toRight(s"opening alias ${alias.aliasId} references unknown taxonomy row ${alias.sourceRefs.head}")
      _ <- requireExact(line.sourceId, alias.sourceId, s"opening alias ${alias.aliasId} sourceId must match taxonomy row")
      _ <- Either.cond(line.names.contains(alias.sourceName), (), s"opening alias ${alias.aliasId} sourceName must match source taxonomy")
      _ <- requireExact(alias.canonicalName, line.names.head, s"opening alias ${alias.aliasId} canonicalName must match source taxonomy")
      _ <- requireExact(alias.openingFamily, line.openingFamily, s"opening alias ${alias.aliasId} openingFamily must match source taxonomy")
      _ <- requireExact(alias.openingVariation, line.openingVariation, s"opening alias ${alias.aliasId} openingVariation must match source taxonomy")
      _ <-
        if Vector(alias.displayName, alias.contextName).exists(_.toLowerCase.contains("closed spanish")) &&
          !line.openingVariation.toLowerCase.contains("closed")
        then Left(s"opening alias ${alias.aliasId} requires a Closed Spanish taxonomy variation")
        else Right(())
      _ <- validateOpeningAliasSupport(alias, line)
      _ <- validateAliasText(alias)
      _ <- requireVector(alias.negativeBoundaries, s"opening alias ${alias.aliasId} negativeBoundaries")
    yield alias

  private def requireOpeningSource(
      sourceId: String,
      manifests: Vector[OpeningSourceManifest]
  ): Either[String, OpeningSourceManifest] =
    manifests.find(_.sourceId == sourceId) match
      case None => Left(s"unknown sourceId $sourceId")
      case Some(manifest) =>
        validateManifest(manifest).flatMap: valid =>
          if valid.sourceFamily == "opening" then Right(valid)
          else Left(s"sourceId $sourceId is not an opening source")

  private def validateCandidateKind(stat: OpeningMoveStat): Either[String, Unit] =
    val normalized = stat.candidateKind.toLowerCase
    if forbiddenCandidateWords.exists(normalized.contains) then
      Left(s"opening move stat ${stat.statId} must not claim best/theory/truth authority")
    else requireAllowed(stat.candidateKind, allowedCandidateKinds, s"opening move stat ${stat.statId} candidateKind")

  private def validateAliasText(alias: OpeningAlias): Either[String, Unit] =
    val text = s"${alias.displayName} ${alias.contextName}".toLowerCase
    val familyScopedForbiddenWords =
      if indianDefenseFamilies.contains(alias.openingFamily) then forbiddenAliasWords ++ Vector("attack", "pressure", "counterplay")
      else forbiddenAliasWords
    val leak = familyScopedForbiddenWords.exists: word =>
      text.matches(s".*(^|[^a-z])${java.util.regex.Pattern.quote(word)}([^a-z]|$$).*")
    if leak then Left(s"opening alias ${alias.aliasId} contains authority wording")
    else
      for
        _ <- requireNonEmpty(alias.displayName, s"opening alias ${alias.aliasId} displayName")
        _ <- requireNonEmpty(alias.contextName, s"opening alias ${alias.aliasId} contextName")
      yield ()

  private def validateOpeningAliasSupport(alias: OpeningAlias, line: OpeningLine): Either[String, Unit] =
    val text = s"${alias.displayName} ${alias.contextName}".toLowerCase
    val familyError: Option[Either[String, Unit]] =
      if (text.contains("qga") || text.contains("queen's gambit accepted")) && alias.openingFamily != "Queen's Gambit Accepted" then
        Some(Left(s"opening alias ${alias.aliasId} requires Queen's Gambit Accepted family"))
      else if (text.contains("qgd") || text.contains("queen's gambit declined")) && alias.openingFamily != "Queen's Gambit Declined" then
        Some(Left(s"opening alias ${alias.aliasId} requires Queen's Gambit Declined family"))
      else if text.contains("pirc") && alias.openingFamily != "Pirc Defense" then
        Some(Left(s"opening alias ${alias.aliasId} requires Pirc Defense family"))
      else if text.contains("modern defense") && alias.openingFamily != "Modern Defense" then
        Some(Left(s"opening alias ${alias.aliasId} requires Modern Defense family"))
      else if text.contains("king's gambit accepted") && alias.openingFamily != "King's Gambit Accepted" then
        Some(Left(s"opening alias ${alias.aliasId} requires King's Gambit Accepted family"))
      else if text.contains("king's gambit declined") && alias.openingFamily != "King's Gambit Declined" then
        Some(Left(s"opening alias ${alias.aliasId} requires King's Gambit Declined family"))
      else if text.contains("danish gambit accepted") && alias.openingFamily != "Danish Gambit Accepted" then
        Some(Left(s"opening alias ${alias.aliasId} requires Danish Gambit Accepted family"))
      else if text.contains("latvian gambit accepted") && alias.openingFamily != "Latvian Gambit Accepted" then
        Some(Left(s"opening alias ${alias.aliasId} requires Latvian Gambit Accepted family"))
      else if text.contains("englund gambit declined") && alias.openingFamily != "Englund Gambit Declined" then
        Some(Left(s"opening alias ${alias.aliasId} requires Englund Gambit Declined family"))
      else if text.contains("blackmar-diemer gambit accepted") && alias.openingFamily != "Blackmar-Diemer Gambit Accepted" then
        Some(Left(s"opening alias ${alias.aliasId} requires Blackmar-Diemer Gambit Accepted family"))
      else None

    familyError.getOrElse:
      val rules = alias.openingFamily match
        case "Sicilian Defense" =>
          "Sicilian" -> Vector(
            "open sicilian"         -> "Open",
            "closed sicilian"       -> "Closed",
            "najdorf"               -> "Najdorf",
            "accelerated dragon"    -> "Accelerated Dragon",
            "dragon"                -> "Dragon",
            "scheveningen"          -> "Scheveningen",
            "classical sicilian"    -> "Classical",
            "sveshnikov"            -> "Sveshnikov",
            "taimanov"              -> "Taimanov",
            "kan sicilian"          -> "Kan",
            "rossolimo"             -> "Rossolimo",
            "alapin"                -> "Alapin",
            "grand prix"            -> "Grand Prix",
            "smith-morra"           -> "Smith-Morra",
            "moscow"                -> "Moscow"
          )
        case "French Defense" =>
          "French" -> Vector(
            "french advance"    -> "Advance",
            "french tarrasch"   -> "Tarrasch",
            "french winawer"    -> "Winawer",
            "french classical"  -> "Classical",
            "french exchange"   -> "Exchange",
            "french rubinstein" -> "Rubinstein",
            "mccutcheon french" -> "McCutcheon"
          )
        case "Caro-Kann Defense" =>
          "Caro-Kann" -> Vector(
            "caro-kann advance"   -> "Advance",
            "caro-kann exchange"  -> "Exchange",
            "panov caro-kann"     -> "Panov",
            "classical caro-kann" -> "Classical"
          )
        case "Scandinavian Defense" =>
          "Scandinavian" -> Vector(
            "modern scandinavian"        -> "Modern",
            "classical scandinavian"     -> "Classical",
            "portuguese scandinavian"    -> "Portuguese",
            "mieses-kotroc scandinavian" -> "Mieses-Kotroc",
            "icelandic-palme"            -> "Icelandic-Palme"
          )
        case "Alekhine Defense" =>
          "Alekhine" -> Vector(
            "alekhine exchange"   -> "Exchange",
            "alekhine four pawns" -> "Four Pawns",
            "alekhine modern"     -> "Modern"
          )
        case "Queen's Gambit Declined" =>
          "Queen's Gambit Declined" -> Vector(
            "qgd exchange"        -> "Exchange",
            "qgd orthodox"        -> "Orthodox",
            "orthodox qgd"        -> "Orthodox",
            "qgd lasker"          -> "Lasker",
            "lasker qgd"          -> "Lasker",
            "qgd tartakower"      -> "Tartakower",
            "tartakower qgd"      -> "Tartakower",
            "cambridge springs"   -> "Cambridge Springs",
            "ragozin"             -> "Ragozin",
            "albin countergambit" -> "Albin"
          )
        case "Queen's Gambit Accepted" =>
          "Queen's Gambit Accepted" -> Vector(
            "qga central"   -> "Central",
            "qga normal"    -> "Normal",
            "qga classical" -> "Classical"
          )
        case "Slav Defense" =>
          "Slav" -> Vector(
            "exchange slav" -> "Exchange",
            "chebanenko"    -> "Chebanenko",
            "schallopp"     -> "Schallopp",
            "dutch slav"    -> "Dutch",
            "slav dutch"    -> "Dutch",
            "slav carlsbad" -> "Carlsbad"
          )
        case "Semi-Slav Defense" =>
          "Semi-Slav" -> Vector(
            "anti-moscow" -> "Anti-Moscow",
            "anti-meran"  -> "Anti-Meran",
            "moscow"      -> "Moscow",
            "botvinnik"   -> "Botvinnik",
            "semi-meran"  -> "Semi-Meran",
            "meran"       -> "Meran"
          )
        case "Catalan Opening" =>
          "Catalan" -> Vector(
            "open catalan"           -> "Open",
            "catalan with ...dxc4"   -> "Open",
            "catalan with dxc4"      -> "Open",
            "closed catalan"         -> "Closed"
          )
        case "Tarrasch Defense" =>
          "Tarrasch" -> Vector(
            "classical tarrasch" -> "Classical",
            "dubov tarrasch"     -> "Dubov"
          )
        case "King's Indian Defense" =>
          "King's Indian" -> Vector(
            "kid fianchetto"             -> "Fianchetto",
            "king's indian fianchetto"   -> "Fianchetto",
            "kid averbakh"               -> "Averbakh",
            "king's indian averbakh"     -> "Averbakh",
            "kid sämisch"                -> "Sämisch",
            "king's indian sämisch"      -> "Sämisch",
            "kid classical"              -> "Classical",
            "king's indian classical"    -> "Classical"
          )
        case "Nimzo-Indian Defense" =>
          "Nimzo-Indian" -> Vector(
            "nimzo sämisch"      -> "Sämisch",
            "nimzo-indian sämisch" -> "Sämisch",
            "nimzo leningrad"    -> "Leningrad",
            "nimzo classical"    -> "Classical",
            "nimzo rubinstein"   -> "Rubinstein"
          )
        case "Queen's Indian Defense" =>
          "Queen's Indian" -> Vector(
            "qid petrosian"             -> "Petrosian",
            "queen's indian petrosian"  -> "Petrosian",
            "qid averbakh"              -> "Averbakh",
            "queen's indian averbakh"   -> "Averbakh",
            "qid fianchetto"            -> "Fianchetto",
            "queen's indian fianchetto" -> "Fianchetto",
            "qid classical"             -> "Classical",
            "queen's indian classical"  -> "Classical",
            "qid hedgehog"              -> "Hedgehog",
            "queen's indian hedgehog"   -> "Hedgehog"
          )
        case "Bogo-Indian Defense" =>
          "Bogo-Indian" -> Vector(
            "bogo exchange"     -> "Exchange",
            "bogo nimzowitsch"  -> "Nimzowitsch",
            "bogo wade-smyslov" -> "Wade-Smyslov"
          )
        case "Grünfeld Defense" =>
          "Grünfeld" -> Vector(
            "grünfeld exchange"  -> "Exchange",
            "russian grünfeld"   -> "Russian",
            "grünfeld russian"   -> "Russian",
            "stockholm grünfeld" -> "Stockholm",
            "grünfeld stockholm" -> "Stockholm",
            "brinckmann grünfeld" -> "Brinckmann",
            "grünfeld brinckmann" -> "Brinckmann"
          )
        case "Neo-Grünfeld Defense" =>
          "Neo-Grünfeld" -> Vector(
            "neo-grünfeld with nf3" -> "with Nf3",
            "neo-grünfeld nf3"      -> "with Nf3",
            "neo-grünfeld exchange" -> "Exchange",
            "neo-grünfeld classical" -> "Classical"
          )
        case "Old Indian Defense" =>
          "Old Indian" -> Vector(
            "old indian czech"     -> "Czech",
            "old indian janowski"  -> "Janowski",
            "old indian ukrainian" -> "Ukrainian"
          )
        case "Benoni Defense" =>
          "Benoni" -> Vector(
            "benoni-indian"   -> "Benoni-Indian",
            "czech benoni"    -> "Czech",
            "modern benoni"   -> "Modern",
            "classical benoni" -> "Classical"
          )
        case "Benko Gambit" =>
          "Benko" -> Vector(
            "benko fianchetto" -> "Fianchetto",
            "benko zaitsev"    -> "Zaitsev"
          )
        case "English Opening" =>
          "English" -> Vector(
            "english reversed sicilian"        -> "Reversed Sicilian",
            "king's english reversed sicilian" -> "Reversed Sicilian",
            "king's english"                   -> "King's English",
            "symmetrical english"              -> "Symmetrical",
            "english symmetrical"              -> "Symmetrical",
            "botvinnik english"                -> "Botvinnik",
            "english botvinnik"                -> "Botvinnik",
            "four knights english"             -> "Four Knights",
            "english four knights"             -> "Four Knights"
          )
        case "Réti Opening" =>
          "Réti" -> Vector(
            "réti accepted"    -> "Accepted",
            "reti accepted"    -> "Accepted",
            "réti anglo-slav"  -> "Anglo-Slav",
            "reti anglo-slav"  -> "Anglo-Slav"
          )
        case "King's Indian Attack" =>
          "King's Indian Attack" -> Vector(
            "kia double fianchetto"             -> "Double Fianchetto",
            "king's indian attack double fianchetto" -> "Double Fianchetto",
            "kia french"                        -> "French",
            "king's indian attack french"       -> "French",
            "kia sicilian"                      -> "Sicilian",
            "king's indian attack sicilian"     -> "Sicilian"
          )
        case "Nimzo-Larsen Attack" =>
          "Nimzo-Larsen" -> Vector(
            "nimzo-larsen classical"   -> "Classical",
            "nimzo-larsen dutch"       -> "Dutch",
            "nimzo-larsen symmetrical" -> "Symmetrical"
          )
        case "Bird Opening" =>
          "Bird" -> Vector(
            "from's gambit" -> "From's Gambit",
            "bird dutch"    -> "Dutch"
          )
        case "Dutch Defense" =>
          "Dutch" -> Vector(
            "stonewall dutch"  -> "Stonewall",
            "dutch stonewall"  -> "Stonewall",
            "leningrad dutch"  -> "Leningrad",
            "dutch leningrad"  -> "Leningrad",
            "classical dutch"  -> "Classical",
            "dutch classical"  -> "Classical"
          )
        case "Modern Defense" =>
          "Modern Defense" -> Vector(
            "modern averbakh"         -> "Averbakh",
            "modern defense averbakh" -> "Averbakh",
            "modern standard"         -> "Standard",
            "modern defense standard" -> "Standard"
          )
        case "Pirc Defense" =>
          "Pirc" -> Vector(
            "classical pirc"       -> "Classical",
            "pirc classical"       -> "Classical",
            "pirc austrian"        -> "Austrian",
            "pirc defense austrian" -> "Austrian"
          )
        case "Polish Opening" =>
          "Polish" -> Vector(
            "polish czech"     -> "Czech",
            "polish zukertort" -> "Zukertort"
          )
        case "Trompowsky Attack" =>
          "Trompowsky" -> Vector(
            "trompowsky classical"     -> "Classical",
            "trompowsky poisoned pawn" -> "Poisoned Pawn"
          )
        case "London System" =>
          "London" -> Vector(
            "london poisoned pawn" -> "Poisoned Pawn"
          )
        case "Queen's Pawn Game" =>
          "Queen's Pawn" -> Vector(
            "qpg london"           -> "London",
            "queen's pawn london"  -> "London",
            "qpg torre"            -> "Torre",
            "queen's pawn torre"   -> "Torre",
            "qpg colle"            -> "Colle",
            "queen's pawn colle"   -> "Colle"
          )
        case "Zukertort Opening" =>
          "Zukertort" -> Vector(
            "zukertort fianchetto"      -> "Fianchetto",
            "zukertort queen's gambit"  -> "Queen's Gambit",
            "zukertort nimzo-larsen"    -> "Nimzo-Larsen"
          )
        case "King's Gambit Declined" =>
          "King's Gambit Declined" -> Vector(
            "kgd classical"                    -> "Classical",
            "king's gambit declined classical" -> "Classical"
          )
        case "Vienna Game" =>
          "Vienna Game" -> Vector(
            "vienna gambit" -> "Vienna Gambit"
          )
        case "Indian Defense" =>
          "Indian Defense" -> Vector(
            "budapest defense" -> "Budapest Defense",
            "budapest gambit"  -> "Gambit"
          )
        case _ => "" -> Vector.empty

      val (familyLabel, requirements) = rules
      val variation = line.openingVariation.toLowerCase
      requirements.collectFirst:
        case (phrase, required) if text.contains(phrase) && !variation.contains(required.toLowerCase) =>
          Left(s"opening alias ${alias.aliasId} requires $familyLabel variation containing $required")
      .getOrElse(Right(()))

  private val indianDefenseFamilies = Set(
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

  private def validateOpeningSourceUse(manifest: OpeningSourceManifest): Either[String, Unit] =
    if manifest.sourceFamily == "opening" then
      for
        use <- manifest.sourceUse.toRight(s"source ${manifest.sourceId} must declare sourceUse")
        _ <- requireAllowed(use, allowedSourceUses, s"source ${manifest.sourceId} sourceUse")
        _ <-
          use match
            case "online_trend" =>
              requireExact(manifest.sourceType, "trend_stat", s"source ${manifest.sourceId} online_trend sourceUse must use sourceType trend_stat")
            case "master_reference" =>
              requireExact(manifest.sourceType, "masterGameDb", s"source ${manifest.sourceId} master_reference sourceUse must use sourceType masterGameDb")
            case "pipeline_smoke" =>
              requireExact(manifest.sourceType, "aggregateGameDb", s"source ${manifest.sourceId} pipeline_smoke sourceUse must use sourceType aggregateGameDb")
            case _ => Right(())
      yield ()
    else Right(())

  private def validateOpeningAggregateUse(manifest: OpeningSourceManifest): Either[String, Unit] =
    if Set("aggregateGameDb", "trend_stat", "masterGameDb").contains(manifest.sourceType) then
      for
        use <- manifest.aggregateUse.toRight(s"source ${manifest.sourceId} must declare aggregateUse")
        _ <- requireAllowed(use, allowedAggregateUses, s"source ${manifest.sourceId} aggregateUse")
        _ <-
          if manifest.sourceType == "trend_stat" then
            requireExact(use, "online_trend_stat", s"source ${manifest.sourceId} trend_stat sourceType must use aggregateUse online_trend_stat")
          else Right(())
        _ <-
          if manifest.sourceType == "aggregateGameDb" then
            for
              _ <- requireExact(manifest.sourceUse.getOrElse(""), "pipeline_smoke", s"source ${manifest.sourceId} aggregateGameDb sourceType must use sourceUse pipeline_smoke")
              _ <- requireExact(use, "pipeline_smoke", s"source ${manifest.sourceId} aggregateGameDb sourceType must use aggregateUse pipeline_smoke")
            yield ()
          else Right(())
        _ <- validateTrendSource(manifest).map(_ => ())
        _ <- validateMasterReferenceSource(manifest).map(_ => ())
      yield ()
    else Right(())

  private def validateArtifacts(manifest: OpeningSourceManifest): Either[String, Unit] =
    val artifacts = manifest.rawArtifacts ++ manifest.generatedArtifacts
    artifacts.foldLeft[Either[String, Unit]](Right(())): (result, artifact) =>
      result.flatMap(_ => validateArtifact(manifest.sourceId, artifact))

  private def validateArtifact(sourceId: String, artifact: OpeningSourceArtifact): Either[String, Unit] =
    for
      _ <- requireAllowed(artifact.storagePolicy, Set("localOnly", "localCache"), s"source $sourceId artifact storagePolicy")
      _ <- validateLocalArtifactPath(sourceId, artifact.path)
    yield ()

  private def validateLocalArtifactPath(sourceId: String, path: String): Either[String, Unit] =
    val workspace = Paths.get("").toAbsolutePath.normalize()
    val resolved = workspace.resolve(path).normalize()
    val allowed = Vector(
      workspace.resolve("tmp/commentary-opening").normalize(),
      workspace.resolve("modules/commentary/.local/opening").normalize(),
      workspace.resolve("modules/commentary/src/test/resources/commentary-corpus/generated-opening").normalize()
    )
    Either.cond(
      allowed.exists(root => resolved == root || resolved.startsWith(root)),
      (),
      s"source $sourceId artifact path $path is outside ignored opening local output roots"
    )

  private def requireIndexedPosition(
      key: OpeningPositionKey,
      positions: Vector[OpeningPosition],
      statId: String
  ): Either[String, Unit] =
    Either.cond(
      positions.exists(position => position.positionKey == key && position.status == "indexed"),
      (),
      s"opening move stat $statId references no indexed positionKey"
    )

  private def validateCounts(stat: OpeningMoveStat): Either[String, Unit] =
    for
      _ <- Either.cond(stat.sampleSize > 0, (), s"opening move stat ${stat.statId} must have positive sampleSize")
      _ <- Either.cond(
        stat.whiteWins >= 0 && stat.draws >= 0 && stat.blackWins >= 0,
        (),
        s"opening move stat ${stat.statId} result counts cannot be negative"
      )
      _ <- Either.cond(
        stat.whiteWins + stat.draws + stat.blackWins == stat.sampleSize,
        (),
        s"opening move stat ${stat.statId} result counts must sum to sampleSize"
      )
      _ <- Either.cond(
        stat.frequency >= 0.0 && stat.frequency <= 1.0,
        (),
        s"opening move stat ${stat.statId} frequency out of range"
      )
    yield ()

  private def rejectCommittedRawPolicy(manifest: OpeningSourceManifest): Either[String, Unit] =
    Either.cond(
      manifest.rawStoragePolicy.exists(Set("localOnly", "externalOnly").contains),
      (),
      s"source ${manifest.sourceId} raw source data must not be commit-allowed"
    )

  private def requireEco(eco: String, lineId: String): Either[String, Unit] =
    Either.cond(eco.matches("^[A-E][0-9]{2}$"), (), s"opening line $lineId has invalid ECO $eco")

  private def requireUciMove(move: String, label: String): Either[String, Unit] =
    Either.cond(move.matches("^[a-h][1-8][a-h][1-8][qrbn]?$"), (), s"$label has invalid UCI $move")

  private def requirePresent(value: Option[String], field: String): Either[String, Unit] =
    value.filter(_.trim.nonEmpty).fold[Either[String, Unit]](Left(s"missing $field"))(_ => Right(()))

  private def requireVector(values: Vector[String], label: String): Either[String, Unit] =
    Either.cond(values.exists(_.trim.nonEmpty), (), s"$label must not be empty")

  private def requireNonEmpty(value: String, label: String): Either[String, Unit] =
    Either.cond(value.trim.nonEmpty, (), s"$label must not be empty")

  private def requireAllowed(value: String, allowed: Set[String], label: String): Either[String, Unit] =
    Either.cond(allowed.contains(value), (), s"$label uses unsupported value $value")

  private def requireExact[A](value: A, expected: A, message: String): Either[String, Unit] =
    Either.cond(value == expected, (), message)

object OpeningSourceCorpus:
  private val sourcePath = "/commentary-corpus/opening-sources.jsonl"
  private val linePath = "/commentary-corpus/opening-lines.jsonl"
  private val positionPath = "/commentary-corpus/opening-positions.jsonl"
  private val moveStatPath = "/commentary-corpus/opening-move-stats.jsonl"
  private val aliasPath = "/commentary-corpus/opening-aliases.jsonl"
  private val consumptionCandidatePath = "/commentary-corpus/opening-context-candidates.jsonl"
  private val rejectPath = "/commentary-corpus/opening-reject-fixtures.jsonl"

  def loadManifests(): Vector[OpeningSourceManifest] =
    loadJsonl(sourcePath, "opening source manifest").map(OpeningSourceManifest.fromJson)

  def loadLines(): Vector[OpeningLine] =
    loadJsonl(linePath, "opening line").map(OpeningLine.fromJson)

  def loadPositions(): Vector[OpeningPosition] =
    loadJsonl(positionPath, "opening position").map(OpeningPosition.fromJson)

  def loadMoveStats(): Vector[OpeningMoveStat] =
    loadJsonl(moveStatPath, "opening move stat").map(OpeningMoveStat.fromJson)

  def loadAliases(): Vector[OpeningAlias] =
    loadJsonl(aliasPath, "opening alias").map(OpeningAlias.fromJson)

  def loadConsumptionCandidates(): Vector[OpeningContextCandidate] =
    loadJsonl(consumptionCandidatePath, "opening context candidate").map(OpeningContextCandidate.fromJson)

  def loadRejectFixtures(): Vector[OpeningRejectFixture] =
    loadJsonl(rejectPath, "opening reject fixture").map(OpeningRejectFixture.fromJson)

  def readWorkspaceFile(path: String): String =
    Files.readString(Paths.get(path), StandardCharsets.UTF_8)

  private def loadJsonl(resourcePath: String, label: String): Vector[JsValue] =
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

private def text(json: JsValue, field: String): Option[String] =
  (json \ field).asOpt[String].map(_.trim).filter(_.nonEmpty)

private def stringVector(json: JsValue, field: String): Vector[String] =
  (json \ field)
    .asOpt[Vector[String]]
    .getOrElse(Vector.empty)
    .map(_.trim)
    .filter(_.nonEmpty)

private def artifactVector(json: JsValue, field: String): Vector[OpeningSourceArtifact] =
  (json \ field)
    .asOpt[Vector[JsValue]]
    .getOrElse(Vector.empty)
    .map(OpeningSourceArtifact.fromJson)

private def validateAll[A](values: Vector[A])(validate: A => Either[String, A]): Either[String, Unit] =
  values.foldLeft[Either[String, Unit]](Right(())):
    case (acc, value) => acc.flatMap(_ => validate(value).map(_ => ()))
