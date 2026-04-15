package lila.llm.tools.strategicobject

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import play.api.libs.json.*

import chess.{ File, Square }

import lila.llm.strategicobject.*

object QuietPositionalProbeRunner:

  final case class Config(
      inputJsonl: Path,
      sampleIds: List[String],
      families: Set[String],
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

  final case class StrategicObjectSummary(
      objectId: String,
      family: String,
      owner: String,
      readiness: String,
      anchors: List[String],
      locusSquares: List[String],
      locusFiles: List[String],
      relationTargets: List[String],
      profileSummary: String
  )

  final case class DeltaSummary(
      objectId: String,
      scope: String,
      tag: String,
      supportingObjectIds: List[String],
      rivalObjectIds: List[String],
      changedAnchorSquares: List[String],
      evidenceAnchorSquares: List[String],
      evidenceContestedSquares: List[String],
      evidenceFiles: List[String]
  )

  final case class ClaimSummary(
      id: String,
      objectId: String,
      status: String,
      scope: String,
      primaryTag: Option[String],
      supportingObjectIds: List[String],
      currentPositionProbeKind: Option[String],
      sharedTargetContinuity: Boolean,
      plannerPrimary: Boolean,
      plannerSupport: Boolean
  )

  final case class FamilyProbe(
      family: String,
      objects: List[StrategicObjectSummary],
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
      families: List[FamilyProbe]
  )

  object SampleProbe:
    given Writes[SampleProbe] = Json.writes[SampleProbe]

  object FamilyProbe:
    given Writes[FamilyProbe] = Json.writes[FamilyProbe]

  object ClaimSummary:
    given Writes[ClaimSummary] = Json.writes[ClaimSummary]

  object DeltaSummary:
    given Writes[DeltaSummary] = Json.writes[DeltaSummary]

  object StrategicObjectSummary:
    given Writes[StrategicObjectSummary] = Json.writes[StrategicObjectSummary]

  object MoveSummary:
    given Writes[MoveSummary] = Json.writes[MoveSummary]

  private val DefaultFamilies: Set[String] =
    Set(
      StrategicObjectFamily.FixedTargetComplex.toString,
      StrategicObjectFamily.RestrictionShell.toString,
      StrategicObjectFamily.KingSafetyShell.toString,
      StrategicObjectFamily.TensionState.toString
    )

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[quiet-positional-probe] $err")
        sys.exit(2)
      case Right(config) =>
        run(config)

  private def run(config: Config): Unit =
    val rows = loadRows(config.inputJsonl)
    val rowsById = rows.groupBy(_.sampleId).view.mapValues(_.head).toMap
    val missing = config.sampleIds.filterNot(rowsById.contains)

    if missing.nonEmpty then
      System.err.println(
        s"[quiet-positional-probe] missing sample ids in ${config.inputJsonl}: ${missing.mkString(", ")}"
      )
      sys.exit(2)

    val probes = config.sampleIds.map(sampleId => sampleProbe(rowsById(sampleId), config.families))
    val rendered = Json.prettyPrint(Json.toJson(probes))

    config.outputPath match
      case Some(path) =>
        Option(path.getParent).foreach(Files.createDirectories(_))
        Files.writeString(path, rendered, StandardCharsets.UTF_8)
        println(s"[quiet-positional-probe] wrote ${probes.size} samples to $path")
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
      .map { line =>
        val js = Json.parse(line).as[JsObject]
        ProbeInput(
          sampleId = requiredString(js, "sampleId"),
          fen = requiredString(js, "fen"),
          playedUci = requiredString(js, "playedUci"),
          source = stringField(js, "source"),
          pgnPath = stringField(js, "pgnPath"),
          opening = stringField(js, "opening")
        )
      }

  private def sampleProbe(
      input: ProbeInput,
      families: Set[String]
  ): SampleProbe =
    val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor(input.playedUci)
    val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor(input.playedUci)
    val objects = StrategicObjectSynthesizerTest.objectsForFen(input.fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val planned = CanonicalQuestionPlanner.plan(contract, claims)
    val plannerPrimaryIds = planned.claimIds.toSet
    val plannerSupportIds = planned.supportClaimIds.toSet

    SampleProbe(
      sampleId = input.sampleId,
      fen = input.fen,
      source = input.source,
      pgnPath = input.pgnPath,
      opening = input.opening,
      move = moveSummary(input.playedUci),
      plannerAxis = planned.axis.toString,
      plannerClaimIds = planned.claimIds,
      plannerSupportClaimIds = planned.supportClaimIds,
      families =
        families.toList.sorted.map(family =>
          familyProbe(
            family = family,
            objects = objects,
            deltas = deltas,
            claims = claims,
            plannerPrimaryIds = plannerPrimaryIds,
            plannerSupportIds = plannerSupportIds
          )
        )
    )

  private def familyProbe(
      family: String,
      objects: List[StrategicObject],
      deltas: List[StrategicObjectDelta],
      claims: List[CertifiedClaim],
      plannerPrimaryIds: Set[String],
      plannerSupportIds: Set[String]
  ): FamilyProbe =
    val familyObjects =
      objects.filter(_.family.toString == family).sortBy(_.id)
    val familyObjectIds = familyObjects.map(_.id).toSet
    val familyDeltas =
      deltas.filter(delta => delta.family.toString == family).sortBy(delta => (delta.objectId, delta.scope.ordinal))
    val familyClaims =
      claims
        .filter(claim =>
          familyObjectIds.contains(claim.objectId) ||
            claim.delta.exists(_.family.toString == family)
        )
        .sortBy(claim => (claim.objectId, claim.deltaScope.ordinal, claim.id))

    FamilyProbe(
      family = family,
      objects = familyObjects.map(objectSummary),
      deltas = familyDeltas.map(deltaSummary),
      claims =
        familyClaims.map(claim =>
          claimSummary(claim, plannerPrimaryIds, plannerSupportIds)
        )
    )

  private def moveSummary(
      playedUci: String
  ): MoveSummary =
    val move = parseMoveTrace(playedUci)
    MoveSummary(
      playedUci = playedUci,
      touchedSquares = move.touchedSquares.map(_.key),
      touchedFiles = move.touchedFiles.map(fileKey)
    )

  private def objectSummary(
      obj: StrategicObject
  ): StrategicObjectSummary =
    StrategicObjectSummary(
      objectId = obj.id,
      family = obj.family.toString,
      owner = obj.owner.name,
      readiness = obj.readiness.toString,
      anchors =
        obj.anchors.flatMap(anchor =>
          anchor.squares.map(_.key) ++
            anchor.file.map(fileKey) ++
            anchor.route.toList.flatMap(_.allSquares.map(_.key))
        ).distinct.sorted,
      locusSquares = obj.locus.allSquares.map(_.key).distinct.sorted,
      locusFiles =
        (
          obj.locus.files ++
            obj.locus.route.toList.flatMap(_.allSquares.map(_.file))
        ).distinct.sortBy(_.char.toString).map(fileKey),
      relationTargets =
        obj.relations.map(relation => s"${relation.operator}:${relation.target.objectId}").distinct.sorted,
      profileSummary = profileSummary(obj.profile)
    )

  private def deltaSummary(
      delta: StrategicObjectDelta
  ): DeltaSummary =
    DeltaSummary(
      objectId = delta.objectId,
      scope = delta.scope.toString,
      tag = delta.primaryTag.toString,
      supportingObjectIds = delta.supportingObjectIds.sorted,
      rivalObjectIds = delta.rivalObjectIds.sorted,
      changedAnchorSquares =
        delta.changedAnchors.flatMap(anchor =>
          anchor.squares.map(_.key) ++
            anchor.route.toList.flatMap(_.allSquares.map(_.key))
        ).distinct.sorted,
      evidenceAnchorSquares = delta.evidenceRefs.flatMap(_.anchorSquares.map(_.key)).distinct.sorted,
      evidenceContestedSquares = delta.evidenceRefs.flatMap(_.contestedSquares.map(_.key)).distinct.sorted,
      evidenceFiles = delta.evidenceRefs.flatMap(_.lane.map(_.char.toString)).distinct.sorted
    )

  private def claimSummary(
      claim: CertifiedClaim,
      plannerPrimaryIds: Set[String],
      plannerSupportIds: Set[String]
  ): ClaimSummary =
    ClaimSummary(
      id = claim.id,
      objectId = claim.objectId,
      status = claim.status.toString,
      scope = claim.deltaScope.toString,
      primaryTag = claim.primaryTag.map(_.toString),
      supportingObjectIds = claim.supportingObjectIds.sorted,
      currentPositionProbeKind = claim.plannerMetadata.currentPositionProbeKind.map(_.toString),
      sharedTargetContinuity = claim.plannerMetadata.sharedTargetContinuity,
      plannerPrimary = plannerPrimaryIds.contains(claim.id),
      plannerSupport = plannerSupportIds.contains(claim.id)
    )

  private def profileSummary(
      profile: StrategicObjectProfile
  ): String =
    profile match
      case StrategicObjectProfile.FixedTargetComplex(targetSquare, targetOwner, occupantRoles, fixed, defended) =>
        s"target=${targetSquare.key};targetOwner=${targetOwner.name};occupants=${occupantRoles.toList.map(_.forsyth).sorted.mkString("[", ",", "]")};fixed=$fixed;defended=$defended"
      case StrategicObjectProfile.RestrictionShell(restrictedSquares, contestedSquares, constraintSquares) =>
        s"restricted=${restrictedSquares.map(_.key).sorted.mkString("[", ",", "]")};contested=${contestedSquares.map(_.key).sorted.mkString("[", ",", "]")};constraint=${constraintSquares.map(_.key).sorted.mkString("[", ",", "]")}"
      case StrategicObjectProfile.KingSafetyShell(condition, accessFiles, stressedSquares, pressureSquares) =>
        s"condition=$condition;access=${accessFiles.toList.map(fileKey).sorted.mkString("[", ",", "]")};stressed=${stressedSquares.map(_.key).sorted.mkString("[", ",", "]")};pressure=${pressureSquares.map(_.key).sorted.mkString("[", ",", "]")}"
      case StrategicObjectProfile.TensionState(contactSquares, releaseSquares, pressureSquares, breakSquares, features) =>
        s"contact=${contactSquares.map(_.key).sorted.mkString("[", ",", "]")};release=${releaseSquares.map(_.key).sorted.mkString("[", ",", "]")};pressure=${pressureSquares.map(_.key).sorted.mkString("[", ",", "]")};breaks=${breakSquares.map(_.key).sorted.mkString("[", ",", "]")};features=${features.toList.map(_.toString).sorted.mkString("[", ",", "]")}"
      case other =>
        other.toString

  private def parseArgs(
      args: List[String]
  ): Either[String, Config] =
    def loop(
        rest: List[String],
        inputJsonl: Option[Path],
        sampleIds: List[String],
        families: Set[String],
        outputPath: Option[Path]
    ): Either[String, Config] =
      rest match
        case Nil =>
          for
            input <- inputJsonl.toRight("missing --input-jsonl <path>")
            _ <- Either.cond(sampleIds.nonEmpty, (), "missing at least one --sample-id <id>")
          yield Config(
            inputJsonl = input,
            sampleIds = sampleIds,
            families = if families.nonEmpty then families else DefaultFamilies,
            outputPath = outputPath
          )
        case "--input-jsonl" :: value :: tail =>
          loop(tail, Some(Path.of(value)), sampleIds, families, outputPath)
        case "--sample-id" :: value :: tail =>
          loop(tail, sampleIds = sampleIds :+ value, inputJsonl = inputJsonl, families = families, outputPath = outputPath)
        case "--family" :: value :: tail =>
          loop(tail, inputJsonl, sampleIds, families + value, outputPath)
        case "--output" :: value :: tail =>
          loop(tail, inputJsonl, sampleIds, families, Some(Path.of(value)))
        case option :: _ =>
          Left(s"unsupported argument: $option")

    loop(args, None, Nil, Set.empty, None)

  private def stringField(
      js: JsObject,
      field: String
  ): Option[String] =
    (js \ field).asOpt[String].map(_.trim).filter(_.nonEmpty)

  private def requiredString(
      js: JsObject,
      field: String
  ): String =
    stringField(js, field).getOrElse(
      throw new IllegalArgumentException(s"missing required field: $field")
    )

  private def parseMoveTrace(
      playedUci: String
  ): StrategicPlayedMoveTrace =
    StrategicPlayedMoveTrace(
      from =
        Square
          .fromKey(playedUci.take(2))
          .getOrElse(throw new IllegalArgumentException(s"invalid move: $playedUci")),
      to =
        Square
          .fromKey(playedUci.slice(2, 4))
          .getOrElse(throw new IllegalArgumentException(s"invalid move: $playedUci"))
    )

  private def fileKey(
      file: File
  ): String =
    file.char.toString
