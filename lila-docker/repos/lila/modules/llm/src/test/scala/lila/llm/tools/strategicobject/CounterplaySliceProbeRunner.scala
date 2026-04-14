package lila.llm.tools.strategicobject

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import play.api.libs.json.*

import chess.{ File, Square }

import lila.llm.strategicobject.*

object CounterplaySliceProbeRunner:

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

  final case class RelationSummary(
      operator: String,
      targetId: String,
      targetFamily: String,
      targetOwner: String
  )

  final case class RivalReferenceSummary(
      kind: String,
      owner: String,
      objectId: Option[String],
      objectFamily: Option[String],
      primitiveKind: Option[String],
      squares: List[String],
      file: Option[String]
  )

  final case class RivalRelationWitnessSummary(
      targetId: String,
      operator: String,
      sharedSquares: List[String],
      sharedFiles: List[String]
  )

  final case class ExactRivalAdmissionSummary(
      admitted: Boolean,
      typedAxisSupported: Boolean,
      rivalIds: List[String],
      relationOperators: List[String],
      rivalFamilies: List[String],
      witnesses: List[RivalRelationWitnessSummary]
  )

  final case class RelationMatchSummary(
      targetId: String,
      operator: String,
      touchedSquares: List[String],
      touchedFiles: List[String]
  )

  final case class MoveLocalAssessmentSummary(
      exactRivalAdmitted: Boolean,
      moveTouchesCore: Boolean,
      relationTouch: Boolean,
      moveWitnessSatisfied: Boolean,
      blockedByProvisionalScope: Boolean,
      blockedByCertification: Boolean,
      blocker: Option[String],
      matchedSquares: List[String],
      matchedFiles: List[String],
      relationWitnesses: List[String],
      relationMatches: List[RelationMatchSummary]
  )

  final case class DeltaSummary(
      scope: String,
      tag: String,
      relationWitnesses: List[String],
      primitiveKinds: List[String],
      matchedSquares: List[String],
      matchedFiles: List[String]
  )

  final case class ClaimSummary(
      id: String,
      deltaScope: Option[String],
      status: String
  )

  final case class PrimitiveSummary(
      kind: String,
      owner: String,
      anchorSquares: List[String],
      contestedSquares: List[String],
      lane: Option[String],
      releaseKind: Option[String],
      breakMode: Option[String]
  )

  final case class CounterplayObjectProbe(
      objectId: String,
      owner: String,
      readiness: String,
      typedAxes: List[String],
      resourceSquares: List[String],
      breakSquares: List[String],
      pressureSquares: List[String],
      supportingPrimitiveKinds: List[String],
      touchedPrimitiveKinds: List[String],
      touchedPrimitives: List[PrimitiveSummary],
      relations: List[RelationSummary],
      rivalReferences: List[RivalReferenceSummary],
      exactRivalAdmission: ExactRivalAdmissionSummary,
      moveLocalAssessment: Option[MoveLocalAssessmentSummary],
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
      counterplayObjects: List[CounterplayObjectProbe]
  )

  object SampleProbe:
    given Writes[SampleProbe] = Json.writes[SampleProbe]

  object CounterplayObjectProbe:
    given Writes[CounterplayObjectProbe] = Json.writes[CounterplayObjectProbe]

  object ClaimSummary:
    given Writes[ClaimSummary] = Json.writes[ClaimSummary]

  object PrimitiveSummary:
    given Writes[PrimitiveSummary] = Json.writes[PrimitiveSummary]

  object DeltaSummary:
    given Writes[DeltaSummary] = Json.writes[DeltaSummary]

  object MoveLocalAssessmentSummary:
    given Writes[MoveLocalAssessmentSummary] = Json.writes[MoveLocalAssessmentSummary]

  object RelationMatchSummary:
    given Writes[RelationMatchSummary] = Json.writes[RelationMatchSummary]

  object ExactRivalAdmissionSummary:
    given Writes[ExactRivalAdmissionSummary] = Json.writes[ExactRivalAdmissionSummary]

  object RivalRelationWitnessSummary:
    given Writes[RivalRelationWitnessSummary] = Json.writes[RivalRelationWitnessSummary]

  object RivalReferenceSummary:
    given Writes[RivalReferenceSummary] = Json.writes[RivalReferenceSummary]

  object RelationSummary:
    given Writes[RelationSummary] = Json.writes[RelationSummary]

  object MoveSummary:
    given Writes[MoveSummary] = Json.writes[MoveSummary]

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[counterplay-slice-probe] $err")
        sys.exit(2)
      case Right(config) =>
        run(config)

  private def run(config: Config): Unit =
    val rows = loadRows(config.inputJsonl)
    val rowsById = rows.groupBy(_.sampleId).view.mapValues(_.head).toMap
    val missing = config.sampleIds.filterNot(rowsById.contains)

    if missing.nonEmpty then
      System.err.println(
        s"[counterplay-slice-probe] missing sample ids in ${config.inputJsonl}: ${missing.mkString(", ")}"
      )
      sys.exit(2)

    val probes = config.sampleIds.map(sampleId => sampleProbe(rowsById(sampleId)))
    val rendered = Json.prettyPrint(Json.toJson(probes))

    config.outputPath match
      case Some(path) =>
        Option(path.getParent).foreach(Files.createDirectories(_))
        Files.writeString(path, rendered, StandardCharsets.UTF_8)
        println(s"[counterplay-slice-probe] wrote ${probes.size} samples to $path")
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
    val planned = CanonicalQuestionPlanner.plan(contract, claims)

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
      counterplayObjects =
        objects
          .filter(_.family == StrategicObjectFamily.CounterplayAxis)
          .sortBy(_.id)
          .map(obj =>
            counterplayObjectProbe(
              obj = obj,
              move = move,
              objectsById = objectsById,
              deltas = deltas.filter(delta => delta.family == StrategicObjectFamily.CounterplayAxis && delta.objectId == obj.id),
              claims = claims.filter(claim =>
                claim.objectId == obj.id ||
                  claim.delta.exists(delta =>
                    delta.family == StrategicObjectFamily.CounterplayAxis &&
                      delta.objectId == obj.id
                  )
              )
            )
          )
    )

  private def counterplayObjectProbe(
      obj: StrategicObject,
      move: StrategicPlayedMoveTrace,
      objectsById: Map[String, StrategicObject],
      deltas: List[StrategicObjectDelta],
      claims: List[CertifiedClaim]
  ): CounterplayObjectProbe =
    val exactAdmission = CounterplayAxisRivalRelationBoundary.exactRivalAdmission(obj, objectsById)
    val assessment = CounterplayMoveLocalBoundary.assess(obj, move, objectsById)

    val (typedAxes, resourceSquares, breakSquares, pressureSquares) =
      obj.profile match
        case StrategicObjectProfile.CounterplayAxis(resources, breaks, pressure, typed) =>
          (
            typed.toList.map(_.toString).sorted,
            resources.map(_.key).sorted,
            breaks.map(_.key).sorted,
            pressure.map(_.key).sorted
          )
        case other =>
          throw new IllegalArgumentException(s"expected CounterplayAxis profile for ${obj.id}, found ${other.family}")

    CounterplayObjectProbe(
      objectId = obj.id,
      owner = obj.owner.name,
      readiness = obj.readiness.toString,
      typedAxes = typedAxes,
      resourceSquares = resourceSquares,
      breakSquares = breakSquares,
      pressureSquares = pressureSquares,
      supportingPrimitiveKinds =
        obj.supportingPrimitives.map(_.kind.toString).distinct.sorted,
      touchedPrimitiveKinds =
        obj.supportingPrimitives.collect {
          case primitive if touchesPrimitive(move, primitive) => primitive.kind.toString
        }.distinct.sorted,
      touchedPrimitives =
        obj.supportingPrimitives.collect {
          case primitive if touchesPrimitive(move, primitive) =>
            PrimitiveSummary(
              kind = primitive.kind.toString,
              owner = primitive.owner.name,
              anchorSquares = primitive.anchorSquares.map(_.key).sorted,
              contestedSquares = primitive.contestedSquares.map(_.key).sorted,
              lane = primitive.lane.map(fileKey),
              releaseKind = primitive.releaseKind.map(_.toString),
              breakMode = primitive.breakMode.map(_.toString)
            )
        }.sortBy(primitive =>
          s"${primitive.kind}:${primitive.anchorSquares.mkString("-")}:${primitive.contestedSquares.mkString("-")}:${primitive.lane.getOrElse("-")}"
        ),
      relations =
        obj.relations.map(relation =>
          RelationSummary(
            operator = relation.operator.toString,
            targetId = relation.target.objectId,
            targetFamily = relation.target.family.toString,
            targetOwner = relation.target.owner.name
          )
        ).sortBy(item => s"${item.operator}:${item.targetId}"),
      rivalReferences =
        obj.rivalResourcesOrObjects.map(rival =>
          RivalReferenceSummary(
            kind = rival.kind.toString,
            owner = rival.owner.name,
            objectId = rival.objectId,
            objectFamily = rival.objectFamily.map(_.toString),
            primitiveKind = rival.primitiveKind.map(_.toString),
            squares = rival.squares.map(_.key).sorted,
            file = rival.file.map(fileKey)
          )
        ).sortBy(item => s"${item.objectId.getOrElse("-")}:${item.objectFamily.getOrElse("-")}"),
      exactRivalAdmission =
        ExactRivalAdmissionSummary(
          admitted = exactAdmission.admitted,
          typedAxisSupported = exactAdmission.typedAxisSupported,
          rivalIds = exactAdmission.rivalIds.toList.sorted,
          relationOperators = exactAdmission.relationOperators.toList.map(_.toString).sorted,
          rivalFamilies = exactAdmission.rivalFamilies.toList.map(_.toString).sorted,
          witnesses =
            exactAdmission.witnesses.map(witness =>
              RivalRelationWitnessSummary(
                targetId = witness.targetId,
                operator = witness.operator.toString,
                sharedSquares = witness.sharedSquares.map(_.key).sorted,
                sharedFiles = witness.sharedFiles.map(fileKey).sorted
              )
            ).sortBy(item => s"${item.targetId}:${item.operator}")
        ),
      moveLocalAssessment =
        assessment.map(result =>
          MoveLocalAssessmentSummary(
            exactRivalAdmitted = result.exactRivalAdmitted,
            moveTouchesCore = result.moveTouchesCore,
            relationTouch = result.relationTouch,
            moveWitnessSatisfied = result.moveWitnessSatisfied,
            blockedByProvisionalScope = result.blockedByProvisionalScope,
            blockedByCertification = result.blockedByCertification,
            blocker = result.blocker.map(_.toString),
            matchedSquares = result.matchedSquares.map(_.key),
            matchedFiles = result.matchedFiles.map(fileKey),
            relationWitnesses = result.relationWitnesses.toList.map(_.toString).sorted,
            relationMatches =
              result.relationMatches.map(matchItem =>
                RelationMatchSummary(
                  targetId = matchItem.targetId,
                  operator = matchItem.operator.toString,
                  touchedSquares = matchItem.touchedSquares.map(_.key).sorted,
                  touchedFiles = matchItem.touchedFiles.map(fileKey).sorted
                )
              ).sortBy(item => s"${item.targetId}:${item.operator}")
          )
        ),
      deltas =
        deltas.map(delta =>
          DeltaSummary(
            scope = delta.scope.toString,
            tag =
              delta.projection match
                case StrategicDeltaProjection.MoveLocal(change, _)  => change.toString
                case StrategicDeltaProjection.PositionLocal(state, _) => state.toString
                case StrategicDeltaProjection.Comparative(contrast, _, _, _, _) => contrast.toString,
            primitiveKinds =
              delta.projection match
                case StrategicDeltaProjection.MoveLocal(_, witness) => witness.primitiveKinds.toList.map(_.toString).sorted
                case StrategicDeltaProjection.Comparative(_, _, witness, _, _) => witness.rivalPrimitiveKinds.toList.map(_.toString).sorted
                case _                                              => Nil,
            relationWitnesses =
              delta.projection match
                case StrategicDeltaProjection.MoveLocal(_, witness) => witness.relationWitnesses.toList.map(_.toString).sorted
                case _                                             => Nil,
            matchedSquares =
              delta.projection match
                case StrategicDeltaProjection.MoveLocal(_, witness) => witness.matchedSquares.map(_.key)
                case _                                             => Nil,
            matchedFiles =
              delta.projection match
                case StrategicDeltaProjection.MoveLocal(_, witness) => witness.matchedFiles.map(fileKey)
                case _                                             => Nil
          )
        ).sortBy(item => s"${item.scope}:${item.tag}"),
      claims =
        claims.map(claim =>
          ClaimSummary(
            id = claim.id,
            deltaScope = Some(claim.deltaScope.toString),
            status = claim.status.toString
          )
        ).sortBy(_.id)
    )

  private def parseArgs(
      args: List[String]
  ): Either[String, Config] =
    def loop(
        rest: List[String],
        inputJsonl: Option[Path],
        sampleIds: List[String],
        outputPath: Option[Path]
    ): Either[String, Config] =
      rest match
        case Nil =>
          for
            input <- inputJsonl.toRight("missing --input-jsonl")
            ids <- Either.cond(sampleIds.nonEmpty, sampleIds, "missing --sample-ids")
          yield Config(inputJsonl = input, sampleIds = ids, outputPath = outputPath)
        case head :: tail if head.startsWith("--input-jsonl=") =>
          loop(tail, Some(Path.of(head.stripPrefix("--input-jsonl=")).toAbsolutePath.normalize), sampleIds, outputPath)
        case "--input-jsonl" :: value :: tail =>
          loop(tail, Some(Path.of(value).toAbsolutePath.normalize), sampleIds, outputPath)
        case head :: tail if head.startsWith("--sample-ids=") =>
          loop(tail, inputJsonl, parseSampleIds(head.stripPrefix("--sample-ids=")), outputPath)
        case "--sample-ids" :: value :: tail =>
          loop(tail, inputJsonl, parseSampleIds(value), outputPath)
        case head :: tail if head.startsWith("--output=") =>
          loop(tail, inputJsonl, sampleIds, Some(Path.of(head.stripPrefix("--output=")).toAbsolutePath.normalize))
        case "--output" :: value :: tail =>
          loop(tail, inputJsonl, sampleIds, Some(Path.of(value).toAbsolutePath.normalize))
        case unknown :: _ =>
          Left(s"unknown argument: $unknown")

    loop(args, None, Nil, None)

  private def parseSampleIds(
      raw: String
  ): List[String] =
    raw.split(",").toList.map(_.trim).filter(_.nonEmpty)

  private def parseMoveTrace(
      playedUci: String
  ): StrategicPlayedMoveTrace =
    StrategicPlayedMoveTrace(
      from = Square.fromKey(playedUci.take(2)).getOrElse(throw new IllegalArgumentException(s"invalid move: $playedUci")),
      to = Square.fromKey(playedUci.slice(2, 4)).getOrElse(throw new IllegalArgumentException(s"invalid move: $playedUci"))
    )

  private def requiredString(
      js: JsObject,
      field: String
  ): String =
    stringField(js, field).getOrElse(throw new IllegalArgumentException(s"missing $field in $js"))

  private def stringField(
      js: JsObject,
      field: String
  ): Option[String] =
    (js \ field).asOpt[String].map(_.trim).filter(_.nonEmpty)

  private def fileKey(
      file: File
  ): String =
    file.char.toString

  private def touchesPrimitive(
      move: StrategicPlayedMoveTrace,
      primitive: PrimitiveReference
  ): Boolean =
    move.touchedSquares.exists(primitive.allSquares.contains) ||
      move.touchedFiles.exists(file =>
        primitive.lane.contains(file) || primitive.allSquares.exists(_.file == file)
      )
