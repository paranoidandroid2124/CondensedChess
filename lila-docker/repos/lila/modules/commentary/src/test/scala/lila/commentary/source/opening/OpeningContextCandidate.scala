package lila.commentary.source.opening

import lila.commentary.source.{ OpeningSequenceContext, OpeningSequenceContextRole }
import play.api.libs.json.*

enum OpeningConfidence(val key: String):
  case High extends OpeningConfidence("high")
  case Usable extends OpeningConfidence("usable")
  case Low extends OpeningConfidence("low")
  case Suppressed extends OpeningConfidence("suppressed")
  case SecondaryOnly extends OpeningConfidence("secondary_only")

object OpeningConfidence:
  def fromKey(key: String): Either[String, OpeningConfidence] =
    OpeningConfidence.values.find(_.key == key).toRight(s"unsupported opening confidence $key")

final case class OpeningIdentity(
    lineId: String,
    sourceId: String,
    ecoCode: String,
    sourceName: String,
    canonicalName: String,
    openingFamily: String,
    openingVariation: String
)

object OpeningIdentity:
  private val allowedFields = Set("lineId", "sourceId", "eco", "sourceName", "canonicalName", "openingFamily", "openingVariation")

  def fromLine(line: OpeningLine): OpeningIdentity =
    OpeningIdentity(
      lineId = line.lineId,
      sourceId = line.sourceId,
      ecoCode = line.ecoCode,
      sourceName = line.names.headOption.getOrElse(""),
      canonicalName = line.names.headOption.getOrElse(""),
      openingFamily = line.openingFamily,
      openingVariation = line.openingVariation
    )

  def fromJson(json: JsValue): OpeningIdentity =
    rejectUnknownFields(json, allowedFields, "Opening identity")
    OpeningIdentity(
      lineId = textValue(json, "lineId").getOrElse(""),
      sourceId = textValue(json, "sourceId").getOrElse(""),
      ecoCode = textValue(json, "eco").getOrElse(""),
      sourceName = textValue(json, "sourceName").getOrElse(""),
      canonicalName = textValue(json, "canonicalName").getOrElse(""),
      openingFamily = textValue(json, "openingFamily").getOrElse(""),
      openingVariation = textValue(json, "openingVariation").getOrElse("")
    )

final case class OpeningContextAlias(
    aliasId: String,
    displayName: String,
    contextName: String,
    aliasKind: String,
    sourceRefs: Vector[String]
)

object OpeningContextAlias:
  private val allowedFields = Set("aliasId", "displayName", "contextName", "aliasKind", "sourceRefs")

  def fromAlias(alias: OpeningAlias): OpeningContextAlias =
    OpeningContextAlias(
      aliasId = alias.aliasId,
      displayName = alias.displayName,
      contextName = alias.contextName,
      aliasKind = alias.aliasKind,
      sourceRefs = alias.sourceRefs
    )

  def fromJson(json: JsValue): OpeningContextAlias =
    rejectUnknownFields(json, allowedFields, "Opening context alias")
    OpeningContextAlias(
      aliasId = textValue(json, "aliasId").getOrElse(""),
      displayName = textValue(json, "displayName").getOrElse(""),
      contextName = textValue(json, "contextName").getOrElse(""),
      aliasKind = textValue(json, "aliasKind").getOrElse(""),
      sourceRefs = stringValues(json, "sourceRefs")
    )

final case class OpeningReferenceStats(
    statId: String,
    sourceId: String,
    sourceUse: String,
    aggregateUse: String,
    move: String,
    sampleSize: Int,
    frequency: Double,
    candidateKind: String,
    authority: String,
    confidence: OpeningConfidence
)

object OpeningReferenceStats:
  private val allowedFields = Set(
    "statId",
    "sourceId",
    "sourceUse",
    "aggregateUse",
    "move",
    "sampleSize",
    "frequency",
    "candidateKind",
    "authority",
    "confidence"
  )

  def fromMoveStat(stat: OpeningMoveStat, manifest: OpeningSourceManifest): Either[String, OpeningReferenceStats] =
    for
      sourceUse <- manifest.sourceUse.toRight(s"source ${manifest.sourceId} must declare sourceUse")
      aggregateUse <- manifest.aggregateUse.toRight(s"source ${manifest.sourceId} must declare aggregateUse")
      _ <- validateStatAuthority(stat)
    yield
      OpeningReferenceStats(
        statId = stat.statId,
        sourceId = stat.sourceId,
        sourceUse = sourceUse,
        aggregateUse = aggregateUse,
        move = stat.move,
        sampleSize = stat.sampleSize,
        frequency = stat.frequency,
        candidateKind = stat.candidateKind,
        authority = stat.authority,
        confidence = rowConfidence(stat, manifest)
      )

  def fromJson(json: JsValue): OpeningReferenceStats =
    rejectUnknownFields(json, allowedFields, "Opening reference stats")
    val confidence =
      OpeningConfidence
        .fromKey(textValue(json, "confidence").getOrElse(""))
        .fold(message => throw IllegalArgumentException(message), identity)
    OpeningReferenceStats(
      statId = textValue(json, "statId").getOrElse(""),
      sourceId = textValue(json, "sourceId").getOrElse(""),
      sourceUse = textValue(json, "sourceUse").getOrElse(""),
      aggregateUse = textValue(json, "aggregateUse").getOrElse(""),
      move = textValue(json, "move").getOrElse(""),
      sampleSize = (json \ "sampleSize").asOpt[Int].getOrElse(0),
      frequency = (json \ "frequency").asOpt[Double].getOrElse(Double.NaN),
      candidateKind = textValue(json, "candidateKind").getOrElse(""),
      authority = textValue(json, "authority").getOrElse(""),
      confidence = confidence
    )

  private def rowConfidence(stat: OpeningMoveStat, manifest: OpeningSourceManifest): OpeningConfidence =
    manifest.sourceUse match
      case Some("master_reference") =>
        val threshold = manifest.perPositionSampleSizeThreshold.getOrElse(Int.MaxValue)
        if stat.sampleSize >= threshold * 5 then OpeningConfidence.High
        else if stat.sampleSize >= threshold then OpeningConfidence.Usable
        else OpeningConfidence.Low
      case Some("online_trend") => OpeningConfidence.SecondaryOnly
      case _                    => OpeningConfidence.Suppressed

  private def validateStatAuthority(stat: OpeningMoveStat): Either[String, Unit] =
    val text = s"${stat.candidateKind} ${stat.authority}".toLowerCase
    val forbidden = Vector("best", "theory", "truth", "forced", "result", "engine", "oracle")
    if forbidden.exists(word => text.contains(word)) then
      Left(s"opening reference stat ${stat.statId} contains truth-bearing wording")
    else if stat.authority != "opening_statistic" then
      Left(s"opening reference stat ${stat.statId} must stay opening_statistic")
    else OpeningCandidate.fromMoveStat(stat).map(_ => ())

final case class OpeningSourceSelection(
    primarySourceUse: Option[String],
    secondarySourceUses: Vector[String],
    suppressedSourceUses: Vector[String],
    mergedRankings: Boolean
)

object OpeningSourceSelection:
  val empty: OpeningSourceSelection =
    OpeningSourceSelection(None, Vector.empty, Vector.empty, mergedRankings = false)

final case class OpeningContextBoundary(value: String)
final case class OpeningConsumptionReject(reason: String)

final case class OpeningContextCandidate(
    candidateId: String,
    positionKey: OpeningPositionKey,
    openingIdentity: OpeningIdentity,
    displayAlias: Option[OpeningContextAlias],
    sourceSelection: OpeningSourceSelection,
    primaryReferenceStats: Vector[OpeningReferenceStats],
    secondaryTrendStats: Vector[OpeningReferenceStats],
    sequenceContexts: Vector[OpeningSequenceContext] = Vector.empty,
    confidence: OpeningConfidence,
    boundaries: Vector[OpeningContextBoundary],
    sourceRefs: Vector[String]
)

object OpeningContextCandidate:
  private val allowedFields = Set(
    "id",
    "positionKey",
    "openingIdentity",
    "displayAlias",
    "sourceSelection",
    "primaryReferenceStats",
    "secondaryTrendStats",
    "sequenceContexts",
    "confidence",
    "boundaries",
    "sourceRefs"
  )
  private val sourceSelectionFields = Set("primarySourceUse", "secondarySourceUses", "suppressedSourceUses", "mergedRankings")
  private val sequenceContextFields = Set("role", "ref", "linkedVariationProofIds", "boundaries")

  def fromJson(json: JsValue): OpeningContextCandidate =
    rejectUnknownFields(json, allowedFields, "Opening context candidate")
    val confidence =
      OpeningConfidence
        .fromKey(textValue(json, "confidence").getOrElse(""))
        .fold(message => throw IllegalArgumentException(message), identity)
    OpeningContextCandidate(
      candidateId = textValue(json, "id").getOrElse(""),
      positionKey = OpeningPositionKey(textValue(json, "positionKey").getOrElse("")),
      openingIdentity = OpeningIdentity.fromJson((json \ "openingIdentity").getOrElse(Json.obj())),
      displayAlias = (json \ "displayAlias").toOption.map(OpeningContextAlias.fromJson),
      sourceSelection = parseSourceSelection((json \ "sourceSelection").getOrElse(Json.obj())),
      primaryReferenceStats = (json \ "primaryReferenceStats").asOpt[Vector[JsValue]].getOrElse(Vector.empty).map(OpeningReferenceStats.fromJson),
      secondaryTrendStats = (json \ "secondaryTrendStats").asOpt[Vector[JsValue]].getOrElse(Vector.empty).map(OpeningReferenceStats.fromJson),
      sequenceContexts = (json \ "sequenceContexts").asOpt[Vector[JsValue]].getOrElse(Vector.empty).map(parseSequenceContext),
      confidence = confidence,
      boundaries = stringValues(json, "boundaries").map(OpeningContextBoundary.apply),
      sourceRefs = stringValues(json, "sourceRefs")
    )

  private def parseSourceSelection(json: JsValue): OpeningSourceSelection =
    rejectUnknownFields(json, sourceSelectionFields, "Opening source selection")
    OpeningSourceSelection(
      primarySourceUse = textValue(json, "primarySourceUse"),
      secondarySourceUses = stringValues(json, "secondarySourceUses"),
      suppressedSourceUses = stringValues(json, "suppressedSourceUses"),
      mergedRankings = (json \ "mergedRankings").asOpt[Boolean].getOrElse(false)
    )

  private def parseSequenceContext(json: JsValue): OpeningSequenceContext =
    rejectUnknownFields(json, sequenceContextFields, "Opening sequence context")
    val role =
      OpeningSequenceContextRole
        .fromKey(textValue(json, "role").getOrElse(""))
        .getOrElse(throw IllegalArgumentException("unsupported opening sequence context role"))
    OpeningSequenceContext(
      role = role,
      ref = textValue(json, "ref").getOrElse(""),
      linkedVariationProofIds = stringValues(json, "linkedVariationProofIds"),
      boundaries = stringValues(json, "boundaries")
    )

object OpeningConsumptionContract:
  private val productRejectedSourceUses = Set("pipeline_smoke")
  private val truthFields = Set(
    "bestMove",
    "bestLine",
    "theoryTruth",
    "forcedLine",
    "forcedContinuation",
    "objectiveResult",
    "objectiveVerdict",
    "engineVerdict",
    "engineProof",
    "oracleVerdict",
    "gameId",
    "gameUrl",
    "playerUrl",
    "eventUrl",
    "event",
    "player",
    "white",
    "black"
  )

  def buildCandidate(
      candidateId: String,
      positionKey: OpeningPositionKey,
      line: Option[OpeningLine],
      alias: Option[OpeningAlias],
      stats: Vector[OpeningMoveStat],
      manifests: Vector[OpeningSourceManifest],
      lines: Vector[OpeningLine]
  ): Either[OpeningConsumptionReject, OpeningContextCandidate] =
    val manifestById = manifests.map(manifest => manifest.sourceId -> manifest).toMap
    for
      _ <- requirePositionKey(positionKey)
      selectedLine <- line.toRight(OpeningConsumptionReject("no taxonomy row -> no opening context candidate"))
      _ <- validateLineIdentity(selectedLine, manifests)
      contextAlias <- validateAlias(alias, selectedLine, lines, manifests)
      referenceStats <- stats.foldLeft[Either[OpeningConsumptionReject, Vector[OpeningReferenceStats]]](Right(Vector.empty)): (acc, stat) =>
        acc.flatMap: collected =>
          if stat.positionKey != positionKey then Left(OpeningConsumptionReject(s"stat ${stat.statId} does not match exact positionKey"))
          else
            manifestById.get(stat.sourceId) match
              case None => Left(OpeningConsumptionReject(s"stat ${stat.statId} references unknown source ${stat.sourceId}"))
              case Some(manifest) =>
                if manifest.sourceUse.exists(productRejectedSourceUses.contains) then Right(collected)
                else
                  OpeningReferenceStats
                    .fromMoveStat(stat, manifest)
                    .left
                    .map(OpeningConsumptionReject.apply)
                    .map(collected :+ _)
      candidate <- assembleCandidate(candidateId, positionKey, selectedLine, contextAlias, referenceStats, stats, manifests)
    yield candidate

  def validateCandidate(
      candidate: OpeningContextCandidate,
      manifests: Vector[OpeningSourceManifest],
      lines: Vector[OpeningLine],
      aliases: Vector[OpeningAlias],
      moveStats: Vector[OpeningMoveStat]
  ): Either[OpeningConsumptionReject, OpeningContextCandidate] =
    for
      _ <- requirePositionKey(candidate.positionKey)
      line <- lines.find(_.lineId == candidate.openingIdentity.lineId).toRight(OpeningConsumptionReject("no taxonomy row -> no opening context candidate"))
      _ <- validateLineIdentity(line, manifests)
      _ <- validateIdentity(candidate.openingIdentity, line)
      _ <- validateCandidateAlias(candidate.displayAlias, candidate.openingIdentity, aliases, lines, manifests)
      _ <- validateStats(candidate.primaryReferenceStats, candidate.secondaryTrendStats, manifests, candidate.positionKey, moveStats)
      _ <- validateSequenceContexts(candidate.sequenceContexts)
      _ <- validateSourceSelection(candidate, moveStats, manifests)
      _ <- Either.cond(
        candidate.confidence == chooseCandidateConfidence(candidate.primaryReferenceStats, candidate.secondaryTrendStats),
        (),
        OpeningConsumptionReject("opening confidence must match source sample policy")
      )
      _ <- Either.cond(!candidate.sourceSelection.mergedRankings, (), OpeningConsumptionReject("master_reference and online_trend rankings must not be merged"))
      _ <- Either.cond(
        candidate.boundaries.exists(_.value == "no_specific_game_citation"),
        (),
        OpeningConsumptionReject("specific game citation belongs to retrieval")
      )
      _ <- validateSourceRefs(candidate)
      _ <- rejectTruthWording(candidate)
    yield candidate

  def rejectTruthFields(json: JsValue): Either[OpeningConsumptionReject, Unit] =
    val obj = json.asOpt[JsObject].getOrElse(Json.obj())
    val extra = obj.keys.filter(truthFields.contains).toVector.sorted
    Either.cond(extra.isEmpty, (), OpeningConsumptionReject(s"opening consumption row carries deferred/truth fields: ${extra.mkString(", ")}"))

  private def assembleCandidate(
      candidateId: String,
      positionKey: OpeningPositionKey,
      line: OpeningLine,
      alias: Option[OpeningContextAlias],
      referenceStats: Vector[OpeningReferenceStats],
      originalStats: Vector[OpeningMoveStat],
      manifests: Vector[OpeningSourceManifest]
  ): Either[OpeningConsumptionReject, OpeningContextCandidate] =
    val sourceUseById = manifests.flatMap(manifest => manifest.sourceUse.map(manifest.sourceId -> _)).toMap
    val suppressedUses = originalStats.flatMap(stat => sourceUseById.get(stat.sourceId)).filter(productRejectedSourceUses.contains).distinct
    val primary = referenceStats.filter(stat => stat.sourceUse == "master_reference").sortBy(stat => (-stat.frequency, stat.move))
    val secondary = referenceStats.filter(stat => stat.sourceUse == "online_trend").sortBy(stat => (-stat.frequency, stat.move))
    val unsupported = referenceStats.filterNot(stat => Set("master_reference", "online_trend").contains(stat.sourceUse))
    if unsupported.nonEmpty then Left(OpeningConsumptionReject(s"unsupported sourceUse for product consumption: ${unsupported.map(_.sourceUse).distinct.mkString(", ")}"))
    else if primary.isEmpty && secondary.isEmpty then Left(OpeningConsumptionReject("only pipeline_smoke or unsupported sources -> no product candidate"))
    else
      val confidence = chooseCandidateConfidence(primary, secondary)
      val boundaries = Vector.newBuilder[OpeningContextBoundary]
      boundaries += OpeningContextBoundary("source_context_only")
      boundaries += OpeningContextBoundary("move_stats_are_reference_only")
      boundaries += OpeningContextBoundary("no_specific_game_citation")
      if primary.nonEmpty && primary.forall(_.confidence == OpeningConfidence.Low) then boundaries += OpeningContextBoundary("master_reference_below_threshold")
      if primary.isEmpty && secondary.nonEmpty then boundaries += OpeningContextBoundary("online_trend_secondary_only")
      if topMove(primary) != topMove(secondary) && primary.nonEmpty && secondary.nonEmpty then boundaries += OpeningContextBoundary("source_disagreement_context_only")
      suppressedUses.foreach(use => boundaries += OpeningContextBoundary(s"${use}_not_product_source"))
      Right(
        OpeningContextCandidate(
          candidateId = candidateId,
          positionKey = positionKey,
          openingIdentity = OpeningIdentity.fromLine(line),
          displayAlias = alias,
          sourceSelection = OpeningSourceSelection(
            primarySourceUse = if primary.nonEmpty then Some("master_reference") else None,
            secondarySourceUses = if secondary.nonEmpty then Vector("online_trend") else Vector.empty,
            suppressedSourceUses = suppressedUses,
            mergedRankings = false
          ),
          primaryReferenceStats = primary,
          secondaryTrendStats = secondary,
          sequenceContexts = Vector.empty,
          confidence = confidence,
          boundaries = boundaries.result().distinctBy(_.value),
          sourceRefs =
            (Vector(line.lineId, line.sourceId) ++ alias.map(_.aliasId) ++ referenceStats.map(_.sourceId) ++ suppressedUses).distinct
        )
      )

  private def chooseCandidateConfidence(
      primary: Vector[OpeningReferenceStats],
      secondary: Vector[OpeningReferenceStats]
  ): OpeningConfidence =
    if primary.exists(_.confidence == OpeningConfidence.High) then OpeningConfidence.High
    else if primary.exists(_.confidence == OpeningConfidence.Usable) then OpeningConfidence.Usable
    else if primary.nonEmpty then OpeningConfidence.Suppressed
    else if secondary.nonEmpty then OpeningConfidence.SecondaryOnly
    else OpeningConfidence.Suppressed

  private def topMove(stats: Vector[OpeningReferenceStats]): Option[String] =
    stats.sortBy(stat => (-stat.frequency, stat.move)).headOption.map(_.move)

  private def requirePositionKey(positionKey: OpeningPositionKey): Either[OpeningConsumptionReject, Unit] =
    Either.cond(positionKey.value.startsWith("std:"), (), OpeningConsumptionReject("no exact positionKey -> no opening context candidate"))

  private def validateLineIdentity(line: OpeningLine, manifests: Vector[OpeningSourceManifest]): Either[OpeningConsumptionReject, Unit] =
    for
      manifest <- manifests.find(_.sourceId == line.sourceId).toRight(OpeningConsumptionReject(s"unknown sourceId ${line.sourceId}"))
      _ <- Either.cond(manifest.sourceUse.contains("taxonomy_reference"), (), OpeningConsumptionReject("opening identity rows must use taxonomy_reference source"))
      _ <- OpeningContextValidator.validateLine(line, manifests).left.map(OpeningConsumptionReject.apply).map(_ => ())
    yield ()

  private def validateIdentity(identity: OpeningIdentity, line: OpeningLine): Either[OpeningConsumptionReject, Unit] =
    val expected = OpeningIdentity.fromLine(line)
    Either.cond(identity == expected, (), OpeningConsumptionReject("opening identity must preserve source taxonomy"))

  private def validateAlias(
      alias: Option[OpeningAlias],
      line: OpeningLine,
      lines: Vector[OpeningLine],
      manifests: Vector[OpeningSourceManifest]
  ): Either[OpeningConsumptionReject, Option[OpeningContextAlias]] =
    alias match
      case None => Right(None)
      case Some(value) =>
        for
          _ <- OpeningContextValidator.validateAlias(value, lines, manifests).left.map(OpeningConsumptionReject.apply).map(_ => ())
          _ <- Either.cond(value.sourceRefs == Vector(line.lineId), (), OpeningConsumptionReject("alias must reference the selected taxonomy row"))
        yield Some(OpeningContextAlias.fromAlias(value))

  private def validateCandidateAlias(
      alias: Option[OpeningContextAlias],
      identity: OpeningIdentity,
      aliases: Vector[OpeningAlias],
      lines: Vector[OpeningLine],
      manifests: Vector[OpeningSourceManifest]
  ): Either[OpeningConsumptionReject, Unit] =
    alias match
      case None => Right(())
      case Some(value) =>
        aliases.find(_.aliasId == value.aliasId) match
          case None => Left(OpeningConsumptionReject(s"alias ${value.aliasId} references unknown alias row"))
          case Some(fullAlias) =>
            for
              _ <- OpeningContextValidator.validateAlias(fullAlias, lines, manifests).left.map(OpeningConsumptionReject.apply).map(_ => ())
              _ <- Either.cond(fullAlias.sourceRefs == Vector(identity.lineId), (), OpeningConsumptionReject("alias must preserve taxonomy sourceRef"))
              _ <- Either.cond(value == OpeningContextAlias.fromAlias(fullAlias), (), OpeningConsumptionReject("display/context alias must preserve the alias row"))
              _ <- Either.cond(fullAlias.sourceName == identity.sourceName, (), OpeningConsumptionReject("alias sourceName must not rewrite taxonomy"))
              _ <- Either.cond(fullAlias.canonicalName == identity.canonicalName, (), OpeningConsumptionReject("alias canonicalName must not rewrite taxonomy"))
            yield ()

  private def validateStats(
      primary: Vector[OpeningReferenceStats],
      secondary: Vector[OpeningReferenceStats],
      manifests: Vector[OpeningSourceManifest],
      positionKey: OpeningPositionKey,
      moveStats: Vector[OpeningMoveStat]
  ): Either[OpeningConsumptionReject, Unit] =
    val manifestById = manifests.map(manifest => manifest.sourceId -> manifest).toMap
    val primaryCheck =
      primary.foldLeft[Either[OpeningConsumptionReject, Unit]](Right(())): (acc, stat) =>
        acc.flatMap: _ =>
          for
            manifest <- manifestById.get(stat.sourceId).toRight(OpeningConsumptionReject(s"unknown source ${stat.sourceId}"))
            _ <- Either.cond(manifest.sourceUse.contains("master_reference"), (), OpeningConsumptionReject("primaryReferenceStats must use master_reference"))
            _ <- Either.cond(stat.sourceUse == "master_reference", (), OpeningConsumptionReject("primaryReferenceStats sourceUse must be master_reference"))
            _ <- Either.cond(stat.aggregateUse == "master_reference_stat", (), OpeningConsumptionReject("primaryReferenceStats must use master_reference_stat"))
            _ <- validatePersistedStat(stat, positionKey, moveStats)
            _ <- Either.cond(
              stat.confidence == expectedReferenceConfidence(stat, manifest),
              (),
              OpeningConsumptionReject(s"opening stat ${stat.statId} confidence must match master threshold")
            )
            _ <- validateReferenceStat(stat)
          yield ()
    primaryCheck.flatMap: _ =>
      secondary.foldLeft[Either[OpeningConsumptionReject, Unit]](Right(())): (acc, stat) =>
        acc.flatMap: _ =>
          for
            manifest <- manifestById.get(stat.sourceId).toRight(OpeningConsumptionReject(s"unknown source ${stat.sourceId}"))
            _ <- Either.cond(manifest.sourceUse.contains("online_trend"), (), OpeningConsumptionReject("secondaryTrendStats must use online_trend"))
            _ <- Either.cond(stat.sourceUse == "online_trend", (), OpeningConsumptionReject("secondaryTrendStats sourceUse must be online_trend"))
            _ <- Either.cond(stat.aggregateUse == "online_trend_stat", (), OpeningConsumptionReject("secondaryTrendStats must use online_trend_stat"))
            _ <- validatePersistedStat(stat, positionKey, moveStats)
            _ <- Either.cond(stat.confidence == OpeningConfidence.SecondaryOnly, (), OpeningConsumptionReject(s"opening stat ${stat.statId} online trend confidence must be secondary_only"))
            _ <- validateReferenceStat(stat)
          yield ()

  private def validateSourceSelection(
      candidate: OpeningContextCandidate,
      moveStats: Vector[OpeningMoveStat],
      manifests: Vector[OpeningSourceManifest]
  ): Either[OpeningConsumptionReject, Unit] =
    val expectedPrimary = if candidate.primaryReferenceStats.nonEmpty then Some("master_reference") else None
    val expectedSecondary = if candidate.secondaryTrendStats.nonEmpty then Vector("online_trend") else Vector.empty
    val manifestById = manifests.map(manifest => manifest.sourceId -> manifest).toMap
    val expectedSuppressed =
      moveStats
        .filter(_.positionKey == candidate.positionKey)
        .flatMap(stat => manifestById.get(stat.sourceId).flatMap(_.sourceUse))
        .filter(_ == "pipeline_smoke")
        .distinct
    for
      _ <- Either.cond(
        candidate.primaryReferenceStats.nonEmpty || candidate.secondaryTrendStats.nonEmpty,
        (),
        OpeningConsumptionReject("only pipeline_smoke or unsupported sources -> no product candidate")
      )
      _ <- Either.cond(
        candidate.sourceSelection.primarySourceUse == expectedPrimary,
        (),
        OpeningConsumptionReject("source selection primary must match primaryReferenceStats")
      )
      _ <- Either.cond(
        candidate.sourceSelection.secondarySourceUses == expectedSecondary,
        (),
        OpeningConsumptionReject("source selection secondary must match secondaryTrendStats")
      )
      _ <- Either.cond(
        candidate.sourceSelection.suppressedSourceUses.forall(_ == "pipeline_smoke"),
        (),
        OpeningConsumptionReject("source selection may suppress only pipeline_smoke in this contract")
      )
      _ <- Either.cond(
        candidate.sourceSelection.suppressedSourceUses == expectedSuppressed,
        (),
        OpeningConsumptionReject("source selection suppressed sources must match same-position pipeline_smoke rows")
      )
    yield ()

  private def expectedReferenceConfidence(
      stat: OpeningReferenceStats,
      manifest: OpeningSourceManifest
  ): OpeningConfidence =
    val threshold = manifest.perPositionSampleSizeThreshold.getOrElse(Int.MaxValue)
    if stat.sampleSize >= threshold * 5 then OpeningConfidence.High
    else if stat.sampleSize >= threshold then OpeningConfidence.Usable
    else OpeningConfidence.Low

  private def validatePersistedStat(
      stat: OpeningReferenceStats,
      positionKey: OpeningPositionKey,
      moveStats: Vector[OpeningMoveStat]
  ): Either[OpeningConsumptionReject, Unit] =
    moveStats.find(row => row.statId == stat.statId && row.sourceId == stat.sourceId) match
      case None => Left(OpeningConsumptionReject(s"opening stat ${stat.statId} must reference a committed move-stat fixture row"))
      case Some(row) =>
        for
          _ <- Either.cond(row.positionKey == positionKey, (), OpeningConsumptionReject(s"opening stat ${stat.statId} positionKey must match candidate"))
          _ <- Either.cond(row.move == stat.move, (), OpeningConsumptionReject(s"opening stat ${stat.statId} move must match fixture"))
          _ <- Either.cond(row.sampleSize == stat.sampleSize, (), OpeningConsumptionReject(s"opening stat ${stat.statId} sampleSize must match fixture"))
          _ <- Either.cond(math.abs(row.frequency - stat.frequency) < 0.0000001, (), OpeningConsumptionReject(s"opening stat ${stat.statId} frequency must match fixture"))
          _ <- Either.cond(row.candidateKind == stat.candidateKind, (), OpeningConsumptionReject(s"opening stat ${stat.statId} candidateKind must match fixture"))
          _ <- Either.cond(row.authority == stat.authority, (), OpeningConsumptionReject(s"opening stat ${stat.statId} authority must match fixture"))
        yield ()

  private def validateReferenceStat(stat: OpeningReferenceStats): Either[OpeningConsumptionReject, Unit] =
    val allowedCandidateKinds = Set("statistical_reference", "context_reference")
    for
      _ <- Either.cond(allowedCandidateKinds.contains(stat.candidateKind), (), OpeningConsumptionReject(s"opening stat ${stat.statId} is not a reference candidate"))
      _ <- Either.cond(stat.authority == "opening_statistic", (), OpeningConsumptionReject(s"opening stat ${stat.statId} must stay opening_statistic"))
      _ <- Either.cond(stat.sampleSize > 0, (), OpeningConsumptionReject(s"opening stat ${stat.statId} must carry positive sample size"))
      _ <- Either.cond(stat.frequency >= 0.0 && stat.frequency <= 1.0, (), OpeningConsumptionReject(s"opening stat ${stat.statId} frequency out of range"))
      _ <- rejectStatTruthWording(stat)
    yield ()

  private def rejectStatTruthWording(stat: OpeningReferenceStats): Either[OpeningConsumptionReject, Unit] =
    val text = s"${stat.statId} ${stat.candidateKind} ${stat.authority}".toLowerCase
    val forbidden = Vector("best", "theory", "forced", "result", "objective", "engine", "oracle", "winning", "drawn")
    Either.cond(!forbidden.exists(text.contains), (), OpeningConsumptionReject(s"opening stat ${stat.statId} contains truth wording"))

  private def validateSequenceContexts(sequenceContexts: Vector[OpeningSequenceContext]): Either[OpeningConsumptionReject, Unit] =
    val allowedBoundaries = Set(
      "opening_sequence_context_only",
      "line_test_link_is_not_proof",
      "source_disagreement_context_only",
      "compensation_context_only"
    )
    sequenceContexts.foldLeft[Either[OpeningConsumptionReject, Unit]](Right(())): (acc, context) =>
      acc.flatMap: _ =>
        val values = Vector(context.ref) ++ context.linkedVariationProofIds ++ context.boundaries
        val forbidden = values.exists(containsForbiddenSequenceToken)
        for
          _ <- Either.cond(context.ref.trim.nonEmpty, (), OpeningConsumptionReject("opening sequence context must carry a ref"))
          _ <- Either.cond(!forbidden, (), OpeningConsumptionReject("opening sequence context contains truth wording"))
          _ <- Either.cond(
            context.boundaries.contains("opening_sequence_context_only"),
            (),
            OpeningConsumptionReject("opening sequence context must stay context-only")
          )
          _ <- Either.cond(
            context.linkedVariationProofIds.isEmpty || context.boundaries.contains("line_test_link_is_not_proof"),
            (),
            OpeningConsumptionReject("opening sequence context line-test link must be marked not proof")
          )
          _ <- Either.cond(
            context.boundaries.forall(allowedBoundaries.contains),
            (),
            OpeningConsumptionReject("opening sequence context uses unsupported boundary")
          )
        yield ()

  private def containsForbiddenSequenceToken(value: String): Boolean =
    val normalized = value.toLowerCase.replace('-', '_').replace(':', '_').replace(' ', '_')
    Vector("best", "theory", "truth", "forced", "result", "engine", "oracle", "winning", "drawing", "drawn", "loss", "wdl", "dtz", "dtm")
      .exists(normalized.contains)

  private def validateSourceRefs(candidate: OpeningContextCandidate): Either[OpeningConsumptionReject, Unit] =
    val forbiddenPrefixes = Vector(
      "game:",
      "gameurl:",
      "game_url:",
      "gameid:",
      "game_id:",
      "player:",
      "playerurl:",
      "player_url:",
      "event:",
      "eventurl:",
      "event_url:",
      "white:",
      "black:"
    )
    val forbiddenTokens = Vector(
      "http://",
      "https://",
      "gameid=",
      "game_id=",
      "gameurl=",
      "game_url=",
      "player=",
      "playerurl=",
      "player_url=",
      "event=",
      "eventurl=",
      "event_url=",
      "white=",
      "black="
    )
    val leaked = candidate.sourceRefs.find: ref =>
      val normalized = ref.toLowerCase
      forbiddenPrefixes.exists(normalized.startsWith) || forbiddenTokens.exists(normalized.contains)
    Either.cond(leaked.isEmpty, (), OpeningConsumptionReject(s"specific game citation belongs to retrieval: ${leaked.getOrElse("")}"))

  private def rejectTruthWording(candidate: OpeningContextCandidate): Either[OpeningConsumptionReject, Unit] =
    val text =
      (
        Vector(
          candidate.openingIdentity.sourceName,
          candidate.openingIdentity.canonicalName,
          candidate.openingIdentity.openingFamily,
          candidate.openingIdentity.openingVariation
        ) ++ candidate.displayAlias.toVector.flatMap(alias => Vector(alias.displayName, alias.contextName)) ++
          candidate.boundaries.map(_.value)
      ).mkString(" ").toLowerCase
    val forbidden = Vector("best", "theory", "forced", "objective", "engine", "oracle", "winning", "drawn", "result")
    val leak = forbidden.exists: word =>
      text.matches(s".*(^|[^a-z])${java.util.regex.Pattern.quote(word)}([^a-z]|$$).*")
    Either.cond(!leak, (), OpeningConsumptionReject("opening context candidate contains truth wording"))

private def textValue(json: JsValue, field: String): Option[String] =
  (json \ field).asOpt[String].map(_.trim).filter(_.nonEmpty)

private def stringValues(json: JsValue, field: String): Vector[String] =
  (json \ field).asOpt[Vector[String]].getOrElse(Vector.empty).map(_.trim).filter(_.nonEmpty)

private def rejectUnknownFields(json: JsValue, allowed: Set[String], label: String): Unit =
  val obj = json.asOpt[JsObject].getOrElse(throw IllegalArgumentException(s"$label row must be a JSON object"))
  val extra = obj.keys.filterNot(allowed.contains).toVector.sorted
  if extra.nonEmpty then throw IllegalArgumentException(s"$label row uses unsupported fields: ${extra.mkString(", ")}")
