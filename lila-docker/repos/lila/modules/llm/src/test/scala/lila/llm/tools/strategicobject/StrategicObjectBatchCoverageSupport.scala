package lila.llm.tools.strategicobject

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import chess.*
import chess.format.Fen
import chess.format.pgn.{ Parser, PgnStr }
import play.api.libs.json.*

import scala.jdk.CollectionConverters.*

import lila.llm.analysis.DecisiveTruth.toContract
import lila.llm.analysis.TruthVisibilityRole
import lila.llm.strategicobject.*

object StrategicObjectBatchCoverageSupport:

  final case class BatchInputRow(
      sampleId: String,
      source: String,
      sourceKind: String,
      gameKey: Option[String],
      fen: String,
      playedUci: Option[String],
      ply: Option[Int],
      tags: List[String],
      opening: Option[String],
      mixBucket: Option[String]
  )
  object BatchInputRow:
    given Writes[BatchInputRow] = Json.writes[BatchInputRow]

  final case class InputLoadSummary(
      sourceKinds: List[String],
      sourcePaths: List[String],
      rawGameCount: Int,
      evaluatedSampleCount: Int,
      samplesWithPlayedMove: Int,
      skippedGameCount: Int,
      skippedGames: List[String]
  )
  object InputLoadSummary:
    given Writes[InputLoadSummary] = Json.writes[InputLoadSummary]

  final case class PassActivation(
      pass: String,
      axis: String,
      stage: String,
      admission: String,
      certificationStatuses: List[String],
      objectCount: Int,
      deltaCount: Int,
      claimCount: Int
  )
  object PassActivation:
    given Writes[PassActivation] = Json.writes[PassActivation]

  final case class SampleFamilyActivation(
      sampleId: String,
      source: String,
      sourceKind: String,
      gameKey: Option[String],
      ply: Option[Int],
      playedUci: Option[String],
      tags: List[String],
      opening: Option[String],
      mixBucket: Option[String],
      family: String,
      capabilityMaturity: StrategicObjectCapabilityScorecardSupport.Maturity,
      bestStage: String,
      bestAdmission: String,
      axes: List[String],
      certificationStatuses: List[String],
      passes: List[PassActivation]
  )
  object SampleFamilyActivation:
    given Writes[SampleFamilyActivation] = Json.writes[SampleFamilyActivation]

  final case class AxisActivationSummary(
      axis: String,
      familySampleCount: Int,
      primaryCount: Int,
      supportCount: Int,
      highestStage: String
  )
  object AxisActivationSummary:
    given Writes[AxisActivationSummary] = Json.writes[AxisActivationSummary]

  final case class BatchFamilySummary(
      family: String,
      capabilityMaturity: StrategicObjectCapabilityScorecardSupport.Maturity,
      capabilityObjectStage: String,
      capabilityDeliveryStage: String,
      sampleCount: Int,
      gameCount: Int,
      sampleHitRate: Double,
      primaryCount: Int,
      supportCount: Int,
      plannerNoneCount: Int,
      certificationOnlyCount: Int,
      deltaOnlyCount: Int,
      objectOnlyCount: Int,
      highestBatchStage: String,
      byAxis: List[AxisActivationSummary]
  )
  object BatchFamilySummary:
    given Writes[BatchFamilySummary] = Json.writes[BatchFamilySummary]

  final case class BatchSystemSummary(
      evaluatedSampleCount: Int,
      activatedFamilyCount: Int,
      primaryActivationCount: Int,
      supportActivationCount: Int,
      objectOnlyActivationCount: Int
  )
  object BatchSystemSummary:
    given Writes[BatchSystemSummary] = Json.writes[BatchSystemSummary]

  final case class BatchCoverageReport(
      schema: String,
      input: InputLoadSummary,
      system: BatchSystemSummary,
      families: List[BatchFamilySummary]
  )
  object BatchCoverageReport:
    given Writes[BatchCoverageReport] = Json.writes[BatchCoverageReport]

  final case class LoadConfig(
      maxGames: Option[Int],
      plyStep: Int,
      maxPliesPerGame: Option[Int]
  )

  private final case class PgnGame(
      gameId: String,
      source: String,
      sourceKind: String,
      pgn: String,
      tags: List[String],
      opening: Option[String],
      mixBucket: Option[String]
  )

  private final case class PgnPlyData(
      ply: Int,
      fen: String,
      playedUci: String
  )

  private final case class PassOutcome(
      axis: String,
      activations: Map[String, PassActivation]
  )

  val schema: String = "chesstory.strategicObject.batchCoverage.v1"

  def loadFenJsonl(
      path: Path
  ): (InputLoadSummary, List[BatchInputRow]) =
    val lines =
      Files.readAllLines(path, StandardCharsets.UTF_8).asScala.toList.map(_.trim).filter(_.nonEmpty)
    val rows =
      lines.zipWithIndex.map { case (line, idx) =>
        val js = Json.parse(line).as[JsObject]
        val sampleId =
          stringField(js, "sampleId")
            .orElse(stringField(js, "id"))
            .orElse(stringField(js, "gameKey"))
            .getOrElse(s"${path.getFileName.toString}:line:${idx + 1}")
        BatchInputRow(
          sampleId = sampleId,
          source = stringField(js, "source").getOrElse(path.toString),
          sourceKind = "fen_jsonl",
          gameKey = stringField(js, "gameKey"),
          fen = stringField(js, "fen").getOrElse(throw new IllegalArgumentException(s"$sampleId: missing fen")),
          playedUci =
            stringField(js, "playedUci")
              .orElse(stringField(js, "uci"))
              .orElse(stringField(js, "playedMoveUci")),
          ply = intField(js, "ply"),
          tags = stringListField(js, "tags") ++ stringListField(js, "familyTags"),
          opening = stringField(js, "opening"),
          mixBucket = stringField(js, "mixBucket")
        )
      }
    (
      InputLoadSummary(
        sourceKinds = List("fen_jsonl"),
        sourcePaths = List(path.toString),
        rawGameCount = rows.size,
        evaluatedSampleCount = rows.size,
        samplesWithPlayedMove = rows.count(_.playedUci.nonEmpty),
        skippedGameCount = 0,
        skippedGames = Nil
      ),
      rows
    )

  def loadEmbeddedGamesJson(
      path: Path,
      config: LoadConfig
  ): (InputLoadSummary, List[BatchInputRow]) =
    val root = Json.parse(Files.readString(path, StandardCharsets.UTF_8)).as[JsObject]
    val games = (root \ "games").asOpt[List[JsObject]].getOrElse(Nil)
    val selectedGames = config.maxGames.fold(games)(games.take)
    val (rows, skipped) =
      selectedGames.foldLeft((List.empty[BatchInputRow], List.empty[String])) { case ((acc, skippedAcc), game) =>
        val gameId = stringField(game, "id").getOrElse(s"${path.getFileName}:game:${acc.size + skippedAcc.size + 1}")
        val pgn = stringField(game, "pgn")
        pgn match
          case Some(text) =>
            extractPlyDataStrict(text) match
              case Right(plyData) =>
                (
                  acc ++ selectPgnPlies(plyData, config).map { ply =>
                    BatchInputRow(
                      sampleId = s"$gameId:ply:${ply.ply}",
                      source = gameId,
                      sourceKind = "embedded_games_json",
                      gameKey = Some(gameId),
                      fen = ply.fen,
                      playedUci = Some(ply.playedUci),
                      ply = Some(ply.ply),
                      tags = stringListField(game, "expectedThemes") ++ stringListField(game, "notes"),
                      opening = None,
                      mixBucket = stringField(game, "tier")
                    )
                  },
                  skippedAcc
                )
              case Left(err) =>
                (acc, skippedAcc :+ s"$gameId: $err")
          case None =>
            (acc, skippedAcc :+ s"$gameId: missing pgn")
      }
    (
      InputLoadSummary(
        sourceKinds = List("embedded_games_json"),
        sourcePaths = List(path.toString),
        rawGameCount = selectedGames.size,
        evaluatedSampleCount = rows.size,
        samplesWithPlayedMove = rows.count(_.playedUci.nonEmpty),
        skippedGameCount = skipped.size,
        skippedGames = skipped
      ),
      rows
    )

  def loadCatalogJsonl(
      path: Path,
      config: LoadConfig
  ): (InputLoadSummary, List[BatchInputRow]) =
    val lines =
      Files.readAllLines(path, StandardCharsets.UTF_8).asScala.toList.map(_.trim).filter(_.nonEmpty)
    val catalogRows = config.maxGames.fold(lines)(lines.take)
    val (rows, skipped) =
      catalogRows.foldLeft((List.empty[BatchInputRow], List.empty[String])) { case ((acc, skippedAcc), line) =>
        val js = Json.parse(line).as[JsObject]
        val gameKey = stringField(js, "gameKey").getOrElse(s"${path.getFileName}:row:${acc.size + skippedAcc.size + 1}")
        val pgnPath =
          stringField(js, "pgnPath")
            .map(Path.of(_))
            .filter(Files.exists(_))

        pgnPath match
          case Some(file) =>
            val pgn = Files.readString(file, StandardCharsets.UTF_8)
            extractPlyDataStrict(pgn) match
              case Right(plyData) =>
                (
                  acc ++ selectPgnPlies(plyData, config).map { ply =>
                    BatchInputRow(
                      sampleId = s"$gameKey:ply:${ply.ply}",
                      source = gameKey,
                      sourceKind = "catalog_jsonl",
                      gameKey = Some(gameKey),
                      fen = ply.fen,
                      playedUci = Some(ply.playedUci),
                      ply = Some(ply.ply),
                      tags = stringListField(js, "familyTags"),
                      opening = stringField(js, "opening"),
                      mixBucket = stringField(js, "mixBucket")
                    )
                  },
                  skippedAcc
                )
              case Left(err) =>
                (acc, skippedAcc :+ s"$gameKey: $err")
          case None =>
            (acc, skippedAcc :+ s"$gameKey: missing pgnPath")
      }
    (
      InputLoadSummary(
        sourceKinds = List("catalog_jsonl"),
        sourcePaths = List(path.toString),
        rawGameCount = catalogRows.size,
        evaluatedSampleCount = rows.size,
        samplesWithPlayedMove = rows.count(_.playedUci.nonEmpty),
        skippedGameCount = skipped.size,
        skippedGames = skipped.take(25)
      ),
      rows
    )

  def report(
      inputSummary: InputLoadSummary,
      rows: List[BatchInputRow],
      familiesFilter: Set[String] = Set.empty
  ): (BatchCoverageReport, List[SampleFamilyActivation]) =
    val capabilityRef =
      StrategicObjectCapabilityScorecardSupport.scorecard().families.map(summary => summary.family -> summary).toMap
    val activations =
      rows
        .flatMap(sampleActivationsFor(_, capabilityRef))
        .filter(activation => familiesFilter.isEmpty || familiesFilter.contains(activation.family))
    val familySummaries =
      activations
        .groupBy(_.family)
        .toList
        .sortBy(_._1)
        .map { case (family, familyRows) =>
          summarizeFamily(family, familyRows, rows.size, capabilityRef(family))
        }

    (
      BatchCoverageReport(
        schema = schema,
        input = inputSummary,
        system =
          BatchSystemSummary(
            evaluatedSampleCount = rows.size,
            activatedFamilyCount = familySummaries.size,
            primaryActivationCount = activations.count(_.bestAdmission == "primary"),
            supportActivationCount = activations.count(_.bestAdmission == "support"),
            objectOnlyActivationCount = activations.count(_.bestStage == "object")
          ),
        families = familySummaries
      ),
      activations
    )

  def renderJsonl(
      rows: List[SampleFamilyActivation]
  ): String =
    rows.map(row => Json.stringify(Json.toJson(row))).mkString("", "\n", "\n")

  private def sampleActivationsFor(
      row: BatchInputRow,
      capabilityRef: Map[String, StrategicObjectCapabilityScorecardSupport.FamilySummary]
  ): List[SampleFamilyActivation] =
    val outcomes =
      List(positionPass(row), comparativePass(row)) ++ row.playedUci.toList.map(_ => movePass(row))
    val families = outcomes.flatMap(_.activations.keySet).toSet

    families.toList.sorted.flatMap { family =>
      val passes = outcomes.flatMap(_.activations.get(family))
      capabilityRef.get(family).map { capability =>
        SampleFamilyActivation(
          sampleId = row.sampleId,
          source = row.source,
          sourceKind = row.sourceKind,
          gameKey = row.gameKey,
          ply = row.ply,
          playedUci = row.playedUci,
          tags = row.tags,
          opening = row.opening,
          mixBucket = row.mixBucket,
          family = family,
          capabilityMaturity = capability.maturity,
          bestStage = passes.maxBy(pass => stageRank(pass.stage)).stage,
          bestAdmission = passes.maxBy(pass => admissionRank(pass.admission)).admission,
          axes = passes.map(_.axis).distinct.sorted,
          certificationStatuses = passes.flatMap(_.certificationStatuses).distinct.sorted,
          passes = passes.sortBy(pass => stageRank(pass.stage))
        )
      }
    }

  private def summarizeFamily(
      family: String,
      activations: List[SampleFamilyActivation],
      totalSamples: Int,
      capability: StrategicObjectCapabilityScorecardSupport.FamilySummary
  ): BatchFamilySummary =
    BatchFamilySummary(
      family = family,
      capabilityMaturity = capability.maturity,
      capabilityObjectStage = capability.objectStage,
      capabilityDeliveryStage = capability.deliveryStage,
      sampleCount = activations.map(_.sampleId).distinct.size,
      gameCount = activations.flatMap(_.gameKey).distinct.size,
      sampleHitRate = percent(activations.map(_.sampleId).distinct.size, totalSamples),
      primaryCount = activations.count(_.bestAdmission == "primary"),
      supportCount = activations.count(_.bestAdmission == "support"),
      plannerNoneCount = activations.count(_.bestStage == "planner_none"),
      certificationOnlyCount = activations.count(_.bestStage == "certification"),
      deltaOnlyCount = activations.count(_.bestStage == "delta"),
      objectOnlyCount = activations.count(_.bestStage == "object"),
      highestBatchStage = activations.maxBy(row => stageRank(row.bestStage)).bestStage,
      byAxis =
        activations
          .flatMap(_.axes)
          .distinct
          .sorted
          .map(axis => summarizeAxis(axis, activations.filter(_.axes.contains(axis))))
    )

  private def summarizeAxis(
      axis: String,
      rows: List[SampleFamilyActivation]
  ): AxisActivationSummary =
    AxisActivationSummary(
      axis = axis,
      familySampleCount = rows.map(_.sampleId).distinct.size,
      primaryCount = rows.count(_.bestAdmission == "primary"),
      supportCount = rows.count(_.bestAdmission == "support"),
      highestStage = rows.maxBy(row => stageRank(row.bestStage)).bestStage
    )

  private def positionPass(
      row: BatchInputRow
  ): PassOutcome =
    evaluatePass(
      axis = "WhatMattersHere",
      fen = row.fen,
      truth = PrimitiveExtractionTest.neutralTruthFrame,
      contract = PrimitiveExtractionTest.neutralContract
    )

  private def comparativePass(
      row: BatchInputRow
  ): PassOutcome =
    val truth = PrimitiveExtractionTest.neutralTruthFrame.copy(visibilityRole = TruthVisibilityRole.PrimaryVisible)
    evaluatePass(
      axis = "WhatChanged",
      fen = row.fen,
      truth = truth,
      contract = truth.toContract
    )

  private def movePass(
      row: BatchInputRow
  ): PassOutcome =
    val playedUci = row.playedUci.getOrElse(throw new IllegalArgumentException(s"${row.sampleId}: missing playedUci"))
    evaluatePass(
      axis = "WhyThis",
      fen = row.fen,
      truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor(playedUci),
      contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor(playedUci)
    )

  private def evaluatePass(
      axis: String,
      fen: String,
      truth: lila.llm.analysis.MoveTruthFrame,
      contract: lila.llm.analysis.DecisiveTruthContract
  ): PassOutcome =
    val objects = StrategicObjectSynthesizerTest.objectsForFen(fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val planned = CanonicalQuestionPlanner.plan(contract, claims)

    val objectsByFamily = objects.groupBy(_.family.toString)
    val objectIdsByFamily = objectsByFamily.view.mapValues(_.map(_.id).toSet).toMap
    val allFamilies =
      objectsByFamily.keySet ++
        deltas.map(_.family.toString) ++
        claims.flatMap(claim =>
          objects.find(_.id == claim.objectId).map(_.family.toString).orElse(claim.delta.map(_.family.toString))
        )

    val activations =
      allFamilies.toList.sorted.map { family =>
        val objectIds = objectIdsByFamily.getOrElse(family, Set.empty)
        val familyDeltas = deltas.filter(delta => delta.family.toString == family)
        val familyClaims =
          claims.filter(claim =>
            objectIds.contains(claim.objectId) ||
              claim.delta.exists(_.family.toString == family)
          )
        val admission = admissionFor(planned, familyClaims)
        val stage = stageFor(admission, familyClaims, familyDeltas, objectIds)

        family ->
          PassActivation(
            pass = axis,
            axis = planned.axis.toString,
            stage = stage,
            admission = admission,
            certificationStatuses = familyClaims.map(_.status.toString).distinct.sorted,
            objectCount = objectIds.size,
            deltaCount = familyDeltas.size,
            claimCount = familyClaims.size
          )
      }.toMap

    PassOutcome(axis = planned.axis.toString, activations = activations)

  private def stageFor(
      admission: String,
      claims: List[CertifiedClaim],
      deltas: List[StrategicObjectDelta],
      objectIds: Set[String]
  ): String =
    if admission == "primary" then "planner_primary"
    else if admission == "support" then "planner_support"
    else if claims.exists(_.status == ClaimStatus.Certified) then "planner_none"
    else if claims.nonEmpty then "certification"
    else if deltas.nonEmpty then "delta"
    else if objectIds.nonEmpty then "object"
    else "absent"

  private def admissionFor(
      planned: PlannedQuestion,
      claims: List[CertifiedClaim]
  ): String =
    val claimIds = claims.map(_.id).toSet
    if planned.claimIds.exists(claimIds.contains) then "primary"
    else if planned.supportClaimIds.exists(claimIds.contains) then "support"
    else "none"

  private def stageRank(
      stage: String
  ): Int =
    stage match
      case "planner_primary" => 7
      case "planner_support" => 6
      case "planner_none"    => 5
      case "certification"   => 4
      case "delta"           => 3
      case "object"          => 2
      case "absent"          => 1
      case _                  => 0

  private def admissionRank(
      admission: String
  ): Int =
    admission match
      case "primary" => 3
      case "support" => 2
      case "none"    => 1
      case _          => 0

  private def percent(
      part: Int,
      total: Int
  ): Double =
    if total == 0 then 0.0 else part.toDouble / total.toDouble

  private def selectPgnPlies(
      rows: List[PgnPlyData],
      config: LoadConfig
  ): List[PgnPlyData] =
    val stepped =
      rows.zipWithIndex.collect {
        case (row, idx) if config.plyStep <= 1 || idx % config.plyStep == 0 => row
      }
    config.maxPliesPerGame.fold(stepped)(stepped.take)

  private def extractPlyDataStrict(
      pgn: String
  ): Either[String, List[PgnPlyData]] =
    Parser.mainline(PgnStr(pgn)) match
      case Left(err) => Left(s"PGN parse error: $err")
      case Right(parsed) =>
        val game = parsed.toGame
        val result = Replay.makeReplay(game, parsed.moves)
        if result.failure.nonEmpty || result.replay.chronoMoves.size != parsed.moves.size then
          val reason = result.failure.map(_.toString).getOrElse("replay truncated")
          Left(s"PGN replay truncated after ${result.replay.chronoMoves.size}/${parsed.moves.size} plies: $reason")
        else
          Right(
            result.replay.chronoMoves.zipWithIndex.map { case (moveOrDrop, idx) =>
              val plyNum = game.ply.value + idx + 1
              val positionBefore =
                if idx == 0 then game.position
                else result.replay.chronoMoves.take(idx).last.after
              val fenBefore = Fen.write(positionBefore, Ply(plyNum - 1).fullMoveNumber).value
              PgnPlyData(
                ply = plyNum,
                fen = fenBefore,
                playedUci = moveOrDrop.toUci.uci
              )
            }
          )

  private def stringField(
      js: JsObject,
      key: String
  ): Option[String] =
    (js \ key).asOpt[String].map(_.trim).filter(_.nonEmpty)

  private def intField(
      js: JsObject,
      key: String
  ): Option[Int] =
    (js \ key).asOpt[Int]

  private def stringListField(
      js: JsObject,
      key: String
  ): List[String] =
    (js \ key).asOpt[List[String]].getOrElse(Nil).map(_.trim).filter(_.nonEmpty)
