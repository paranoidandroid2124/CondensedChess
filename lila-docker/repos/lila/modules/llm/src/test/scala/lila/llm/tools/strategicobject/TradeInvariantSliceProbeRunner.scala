package lila.llm.tools.strategicobject

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import play.api.libs.json.*

import chess.{ File, Square }

import lila.llm.strategicobject.*

object TradeInvariantSliceProbeRunner:

  final case class Config(
      inputJsonl: Path,
      sampleIds: List[String],
      outputPath: Option[Path]
  )

  final case class ProbeInput(
      sampleId: String,
      fen: String,
      playedUci: String,
      source: Option[String],
      pgnPath: Option[String],
      opening: Option[String]
  )

  final case class MoveSummary(
      playedUci: String,
      touchedSquares: List[String],
      touchedFiles: List[String]
  )

  final case class PrimaryDescriptorSummary(
      primaryEligible: Boolean,
      packetPrimaryEligible: Boolean,
      timingPrimaryEligible: Boolean,
      primaryReason: Option[String],
      primaryRelevantPreservedFamilies: List[String]
  )

  final case class PreservedObjectSummary(
      objectId: String,
      family: String,
      anchorSquares: List[String],
      locusSquares: List[String],
      profileSummary: String
  )

  final case class DeltaSummary(
      scope: String,
      tag: String,
      axis: Option[String],
      matchedSquares: List[String],
      matchedFiles: List[String],
      relationWitnesses: List[String],
      primitiveKinds: List[String],
      changedAnchorSquares: List[String],
      supportingObjectIds: List[String],
      evidenceAnchorSquares: List[String],
      evidenceContestedSquares: List[String],
      evidenceLanes: List[String],
      descriptor: Option[PrimaryDescriptorSummary]
  )

  final case class ClaimSummary(
      id: String,
      status: String,
      deltaScope: Option[String],
      primaryTag: Option[String],
      supportingObjectIds: List[String],
      boundaryWitnesses: List[String],
      sharedTargetContinuity: Boolean,
      tradeInvariantPrimaryClass: Option[String],
      residualSpecificityClass: Option[String],
      plannerPrimary: Boolean,
      plannerSupport: Boolean
  )

  final case class CoactiveClaimSummary(
      id: String,
      family: String,
      status: String,
      scope: String,
      primaryTag: Option[String],
      anchorSquares: List[String],
      plannerPrimary: Boolean,
      plannerSupport: Boolean
  )

  final case class TradeInvariantObjectProbe(
      objectId: String,
      owner: String,
      readiness: String,
      exchangeSquares: List[String],
      invariantSquares: List[String],
      preservedFiles: List[String],
      preservedFamilies: List[String],
      features: List[String],
      anchors: List[String],
      supportingPrimitiveKinds: List[String],
      descriptor: PrimaryDescriptorSummary,
      preservedObjects: List[PreservedObjectSummary],
      deltas: List[DeltaSummary],
      claims: List[ClaimSummary]
  )

  final case class SampleProbe(
      sampleId: String,
      fen: String,
      source: Option[String],
      pgnPath: Option[String],
      opening: Option[String],
      move: MoveSummary,
      plannerAxis: String,
      plannerClaimIds: List[String],
      plannerSupportClaimIds: List[String],
      primaryFamilies: List[String],
      supportFamilies: List[String],
      coactiveMoveLocalClaims: List[CoactiveClaimSummary],
      tradeInvariantObjects: List[TradeInvariantObjectProbe]
  )

  object SampleProbe:
    given Writes[SampleProbe] = Json.writes[SampleProbe]

  object TradeInvariantObjectProbe:
    given Writes[TradeInvariantObjectProbe] = Json.writes[TradeInvariantObjectProbe]

  object CoactiveClaimSummary:
    given Writes[CoactiveClaimSummary] = Json.writes[CoactiveClaimSummary]

  object ClaimSummary:
    given Writes[ClaimSummary] = Json.writes[ClaimSummary]

  object DeltaSummary:
    given Writes[DeltaSummary] = Json.writes[DeltaSummary]

  object PreservedObjectSummary:
    given Writes[PreservedObjectSummary] = Json.writes[PreservedObjectSummary]

  object PrimaryDescriptorSummary:
    given Writes[PrimaryDescriptorSummary] = Json.writes[PrimaryDescriptorSummary]

  object MoveSummary:
    given Writes[MoveSummary] = Json.writes[MoveSummary]

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[trade-invariant-slice-probe] $err")
        sys.exit(2)
      case Right(config) =>
        run(config)

  private def run(config: Config): Unit =
    val rows = loadRows(config.inputJsonl)
    val rowsById = rows.groupBy(_.sampleId).view.mapValues(_.head).toMap
    val missing = config.sampleIds.filterNot(rowsById.contains)

    if missing.nonEmpty then
      System.err.println(
        s"[trade-invariant-slice-probe] missing sample ids in ${config.inputJsonl}: ${missing.mkString(", ")}"
      )
      sys.exit(2)

    val probes = config.sampleIds.map(sampleId => sampleProbe(rowsById(sampleId)))
    val rendered = Json.prettyPrint(Json.toJson(probes))

    config.outputPath match
      case Some(path) =>
        Option(path.getParent).foreach(Files.createDirectories(_))
        Files.writeString(path, rendered, StandardCharsets.UTF_8)
        println(s"[trade-invariant-slice-probe] wrote ${probes.size} samples to $path")
      case None =>
        println(rendered)

  private def loadRows(
      path: Path
  ): List[ProbeInput] =
    Files
      .readAllLines(path, StandardCharsets.UTF_8)
      .toArray
      .toList
      .map(_.toString.trim)
      .filter(_.nonEmpty)
      .map(line =>
        val js = Json.parse(line).as[JsObject]
        ProbeInput(
          sampleId = requiredString(js, "sampleId"),
          fen = requiredString(js, "fen"),
          playedUci = requiredString(js, "playedUci"),
          source = stringField(js, "source"),
          pgnPath = stringField(js, "pgnPath"),
          opening = stringField(js, "opening")
        )
      )

  private def sampleProbe(
      input: ProbeInput
  ): SampleProbe =
    val move = parseMoveTrace(input.playedUci)
    val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor(input.playedUci)
    val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor(input.playedUci)
    val objects = StrategicObjectSynthesizerTest.objectsForFen(input.fen, truth)
    val objectsById = objects.map(obj => obj.id -> obj).toMap
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val claimsById = claims.map(claim => claim.id -> claim).toMap
    val planned = CanonicalQuestionPlanner.plan(contract, claims)
    val plannerPrimaryIds = planned.claimIds.toSet
    val plannerSupportIds = planned.supportClaimIds.toSet

    val coactiveMoveLocalClaims =
      claims
        .filter(_.deltaScope == StrategicDeltaScope.MoveLocal)
        .sortBy(_.id)
        .map(claim => coactiveClaimSummary(claim, plannerPrimaryIds, plannerSupportIds))

    SampleProbe(
      sampleId = input.sampleId,
      fen = input.fen,
      source = input.source,
      pgnPath = input.pgnPath,
      opening = input.opening,
      move =
        MoveSummary(
          playedUci = input.playedUci,
          touchedSquares = move.touchedSquares.map(_.key),
          touchedFiles = move.touchedFiles.map(fileKey)
        ),
      plannerAxis = planned.axis.toString,
      plannerClaimIds = planned.claimIds,
      plannerSupportClaimIds = planned.supportClaimIds,
      primaryFamilies =
        planned.claimIds.flatMap(id => claimsById.get(id).flatMap(_.delta.map(_.family.toString))).distinct.sorted,
      supportFamilies =
        planned.supportClaimIds.flatMap(id => claimsById.get(id).flatMap(_.delta.map(_.family.toString))).distinct.sorted,
      coactiveMoveLocalClaims = coactiveMoveLocalClaims,
      tradeInvariantObjects =
        objects
          .filter(_.family == StrategicObjectFamily.TradeInvariant)
          .sortBy(_.id)
          .flatMap(obj => tradeInvariantObjectProbe(obj, objectsById, deltas, claims, plannerPrimaryIds, plannerSupportIds))
    )

  private def tradeInvariantObjectProbe(
      obj: StrategicObject,
      objectsById: Map[String, StrategicObject],
      deltas: List[StrategicObjectDelta],
      claims: List[CertifiedClaim],
      plannerPrimaryIds: Set[String],
      plannerSupportIds: Set[String]
  ): Option[TradeInvariantObjectProbe] =
    obj.profile match
      case StrategicObjectProfile.TradeInvariant(
            exchangeSquares,
            invariantSquares,
            preservedFiles,
            preservedFamilies,
            features
          ) =>
        val objectDeltas =
          deltas.filter(_.objectId == obj.id).sortBy(_.scope.ordinal)
        val supportingObjectIds =
          objectDeltas.flatMap(_.supportingObjectIds).distinct
        val preservedObjects =
          uniqueObjectIds(
            supportingObjectIds ++
              objectsById.values
                .filter(other =>
                  other.owner == obj.owner &&
                    other.id != obj.id &&
                    preservedFamilies.contains(other.family) &&
                    overlapsTradeInvariantObject(obj, other)
                )
                .map(_.id)
                .toList
          ).flatMap(objectsById.get)

        Some(
          TradeInvariantObjectProbe(
            objectId = obj.id,
            owner = obj.owner.name,
            readiness = obj.readiness.toString,
            exchangeSquares = exchangeSquares.map(_.key).sorted,
            invariantSquares = invariantSquares.map(_.key).sorted,
            preservedFiles = preservedFiles.toList.map(fileKey).sorted,
            preservedFamilies = preservedFamilies.toList.map(_.toString).sorted,
            features = features.toList.map(_.toString).sorted,
            anchors = obj.anchors.flatMap(_.squares.map(_.key)).distinct.sorted,
            supportingPrimitiveKinds = obj.supportingPrimitives.map(_.kind.toString).distinct.sorted,
            descriptor = descriptorSummary(TradeInvariantPrimaryDescriptor.fromObject(obj).getOrElse(
              TradeInvariantPrimaryDescriptor(
                primaryEligible = false,
                packetPrimaryEligible = false,
                timingPrimaryEligible = false,
                primaryReason = None,
                primaryRelevantPreservedFamilies = Set.empty
              )
            )),
            preservedObjects = preservedObjects.sortBy(_.id).map(preservedObjectSummary),
            deltas = objectDeltas.map(deltaSummary),
            claims =
              claims
                .filter(_.objectId == obj.id)
                .sortBy(_.id)
                .map(claim =>
                  claimSummary(
                    claim,
                    plannerPrimaryIds = plannerPrimaryIds,
                    plannerSupportIds = plannerSupportIds
                  )
                )
          )
        )
      case _ =>
        None

  private def descriptorSummary(
      descriptor: TradeInvariantPrimaryDescriptor
  ): PrimaryDescriptorSummary =
    PrimaryDescriptorSummary(
      primaryEligible = descriptor.primaryEligible,
      packetPrimaryEligible = descriptor.packetPrimaryEligible,
      timingPrimaryEligible = descriptor.timingPrimaryEligible,
      primaryReason = descriptor.primaryReason.map(_.toString),
      primaryRelevantPreservedFamilies =
        descriptor.primaryRelevantPreservedFamilies.toList.map(_.toString).sorted
    )

  private def preservedObjectSummary(
      obj: StrategicObject
  ): PreservedObjectSummary =
    PreservedObjectSummary(
      objectId = obj.id,
      family = obj.family.toString,
      anchorSquares = obj.anchors.flatMap(_.squares.map(_.key)).distinct.sorted,
      locusSquares = obj.locus.allSquares.map(_.key).distinct.sorted,
      profileSummary = profileSummary(obj.profile)
    )

  private def deltaSummary(
      delta: StrategicObjectDelta
  ): DeltaSummary =
    DeltaSummary(
      scope = delta.scope.toString,
      tag = delta.primaryTag.toString,
      axis = delta.moveTransition.map(_.axis.toString),
      matchedSquares = delta.moveTransition.toList.flatMap(_.matchedSquares.map(_.key)).distinct.sorted,
      matchedFiles = delta.moveTransition.toList.flatMap(_.matchedFiles.map(fileKey)).distinct.sorted,
      relationWitnesses = delta.moveTransition.toList.flatMap(_.relationWitnesses.map(_.toString)).distinct.sorted,
      primitiveKinds = delta.moveTransition.toList.flatMap(_.primitiveKinds.map(_.toString)).distinct.sorted,
      changedAnchorSquares = delta.changedAnchors.flatMap(_.squares.map(_.key)).distinct.sorted,
      supportingObjectIds = delta.supportingObjectIds.distinct.sorted,
      evidenceAnchorSquares = delta.evidenceRefs.flatMap(_.anchorSquares.map(_.key)).distinct.sorted,
      evidenceContestedSquares = delta.evidenceRefs.flatMap(_.contestedSquares.map(_.key)).distinct.sorted,
      evidenceLanes = delta.evidenceRefs.flatMap(_.lane.map(fileKey)).distinct.sorted,
      descriptor = TradeInvariantPrimaryDescriptor.fromDelta(delta).map(descriptorSummary)
    )

  private def claimSummary(
      claim: CertifiedClaim,
      plannerPrimaryIds: Set[String],
      plannerSupportIds: Set[String]
  ): ClaimSummary =
    ClaimSummary(
      id = claim.id,
      status = claim.status.toString,
      deltaScope = Some(claim.deltaScope.toString),
      primaryTag = claim.primaryTag.map(_.toString),
      supportingObjectIds = claim.supportingObjectIds.distinct.sorted,
      boundaryWitnesses = claim.boundaryWitnesses.toList.map(_.toString).sorted,
      sharedTargetContinuity = claim.plannerMetadata.sharedTargetContinuity,
      tradeInvariantPrimaryClass = claim.plannerMetadata.tradeInvariantPrimaryClass.map(_.toString),
      residualSpecificityClass = claim.plannerMetadata.residualSpecificityClass.map(_.toString),
      plannerPrimary = plannerPrimaryIds.contains(claim.id),
      plannerSupport = plannerSupportIds.contains(claim.id)
    )

  private def coactiveClaimSummary(
      claim: CertifiedClaim,
      plannerPrimaryIds: Set[String],
      plannerSupportIds: Set[String]
  ): CoactiveClaimSummary =
    CoactiveClaimSummary(
      id = claim.id,
      family = claim.delta.map(_.family.toString).getOrElse("unknown"),
      status = claim.status.toString,
      scope = claim.deltaScope.toString,
      primaryTag = claim.primaryTag.map(_.toString),
      anchorSquares = claim.delta.toList.flatMap(_.changedAnchors.flatMap(_.squares.map(_.key))).distinct.sorted,
      plannerPrimary = plannerPrimaryIds.contains(claim.id),
      plannerSupport = plannerSupportIds.contains(claim.id)
    )

  private def overlapsTradeInvariantObject(
      tradeInvariant: StrategicObject,
      other: StrategicObject
  ): Boolean =
    val tradeSquares = tradeInvariant.locus.allSquares.toSet ++ tradeInvariant.anchors.flatMap(_.squares)
    val otherSquares = other.locus.allSquares.toSet ++ other.anchors.flatMap(_.squares)
    tradeSquares.intersect(otherSquares).nonEmpty ||
      tradeInvariant.locus.files.intersect(other.locus.files).nonEmpty

  private def profileSummary(
      profile: StrategicObjectProfile
  ): String =
    profile match
      case StrategicObjectProfile.FixedTargetComplex(targetSquare, targetOwner, originSquares, fixed, defended) =>
        s"target=${targetSquare.key};owner=${targetOwner.name};origins=${originSquares.size};fixed=$fixed;defended=$defended"
      case StrategicObjectProfile.BreakAxis(sourceSquare, breakSquare, targetSquares, mode, supportBalance) =>
        s"source=${sourceSquare.key};break=${breakSquare.key};targets=${targetSquares.size};mode=$mode;support=$supportBalance"
      case StrategicObjectProfile.AccessNetwork(lane, route, roles, contestedSquares) =>
        s"lane=${lane.map(fileKey).getOrElse("-")};route=${route.map(_.allSquares.size).getOrElse(0)};contested=${contestedSquares.size};roles=${roles.toList.map(_.forsyth).sorted.mkString}"
      case StrategicObjectProfile.PasserComplex(passerSquare, promotionSquare, relativeRank, protectedByPawn, escortSquares) =>
        s"passer=${passerSquare.key};promotion=${promotionSquare.key};rank=$relativeRank;protected=$protectedByPawn;escorts=${escortSquares.size}"
      case other =>
        s"profile=${other.family}"

  private def parseArgs(
      args: List[String]
  ): Either[String, Config] =
    @annotation.tailrec
    def loop(rest: List[String], inputJsonl: Option[Path], outputPath: Option[Path], sampleIds: List[String]): Either[String, Config] =
      rest match
        case Nil =>
          inputJsonl match
            case None => Left("missing --input-jsonl <path>")
            case Some(_) if sampleIds.isEmpty => Left("provide at least one --sample-id <id>")
            case Some(path) =>
              Right(
                Config(
                  inputJsonl = path,
                  sampleIds = sampleIds,
                  outputPath = outputPath
                )
              )
        case "--input-jsonl" :: value :: tail =>
          loop(tail, Some(Path.of(value).toAbsolutePath.normalize), outputPath, sampleIds)
        case head :: tail if head.startsWith("--input-jsonl=") =>
          loop(tail, Some(Path.of(head.stripPrefix("--input-jsonl=")).toAbsolutePath.normalize), outputPath, sampleIds)
        case "--output" :: value :: tail =>
          loop(tail, inputJsonl, Some(Path.of(value).toAbsolutePath.normalize), sampleIds)
        case head :: tail if head.startsWith("--output=") =>
          loop(tail, inputJsonl, Some(Path.of(head.stripPrefix("--output=")).toAbsolutePath.normalize), sampleIds)
        case "--sample-id" :: value :: tail =>
          loop(tail, inputJsonl, outputPath, sampleIds :+ value)
        case head :: tail if head.startsWith("--sample-id=") =>
          loop(tail, inputJsonl, outputPath, sampleIds :+ head.stripPrefix("--sample-id="))
        case unknown :: _ =>
          Left(s"unknown argument: $unknown")

    loop(args, None, None, Nil)

  private def parseMoveTrace(
      playedUci: String
  ): StrategicPlayedMoveTrace =
    StrategicPlayedMoveTrace(
      from = Square.fromKey(playedUci.take(2)).getOrElse(invalidMove(playedUci)),
      to = Square.fromKey(playedUci.slice(2, 4)).getOrElse(invalidMove(playedUci))
    )

  private def fileKey(
      file: File
  ): String =
    file.char.toString

  private def stringField(
      js: JsObject,
      key: String
  ): Option[String] =
    (js \ key).asOpt[String].filter(_.nonEmpty)

  private def requiredString(
      js: JsObject,
      key: String
  ): String =
    stringField(js, key).getOrElse(
      throw new IllegalArgumentException(s"missing required field: $key")
    )

  private def invalidMove(
      playedUci: String
  ): Nothing =
    throw new IllegalArgumentException(s"invalid move: $playedUci")

  private def uniqueObjectIds(
      objectIds: List[String]
  ): List[String] =
    objectIds.distinct.sorted
