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
      pgnPath: Option[String],
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
      pgnPath: Option[String],
      fen: String,
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
      objectCount: Int,
      deltaCount: Int,
      claimCount: Int,
      familyTriggerSummary: String,
      passes: List[PassActivation]
  )
  object SampleFamilyActivation:
    given Writes[SampleFamilyActivation] = Json.writes[SampleFamilyActivation]

  final case class AxisDiversitySummary(
      combinationCount: Int,
      entropy: Double,
      dominantCombination: List[String],
      dominantShare: Double
  )
  object AxisDiversitySummary:
    given Writes[AxisDiversitySummary] = Json.writes[AxisDiversitySummary]

  final case class StageConcentrationSummary(
      entropy: Double,
      dominantStage: String,
      dominantShare: Double
  )
  object StageConcentrationSummary:
    given Writes[StageConcentrationSummary] = Json.writes[StageConcentrationSummary]

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
      ownerRate: Double,
      shadowRate: Double,
      uniqueOwnerRate: Double,
      plannerNoneCount: Int,
      certificationOnlyCount: Int,
      deltaOnlyCount: Int,
      objectOnlyCount: Int,
      accessNetworkResidualCount: Int,
      highestBatchStage: String,
      axisDiversity: AxisDiversitySummary,
      stageConcentration: StageConcentrationSummary,
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

  final case class FamilyAuditRow(
      auditBucket: String,
      auditBucketReason: String,
      sampleId: String,
      source: String,
      sourceKind: String,
      gameKey: Option[String],
      pgnPath: Option[String],
      fen: String,
      ply: Option[Int],
      playedUci: Option[String],
      opening: Option[String],
      tags: List[String],
      mixBucket: Option[String],
      family: String,
      bestStage: String,
      bestAdmission: String,
      axes: List[String],
      objectCount: Int,
      deltaCount: Int,
      claimCount: Int,
      familyTriggerSummary: String,
      accessNetworkCoactive: Boolean,
      accessNetworkStage: Option[String],
      accessNetworkAdmission: Option[String],
      boardTruthVerdict: Option[String] = None,
      familyNameFit: Option[String] = None,
      stageFit: Option[String] = None,
      notes: Option[String] = None
  )
  object FamilyAuditRow:
    private val generatedWrites = Json.writes[FamilyAuditRow]

    given Writes[FamilyAuditRow] with
      def writes(row: FamilyAuditRow): JsValue =
        generatedWrites.writes(row).as[JsObject] ++ Json.obj(
          "boardTruthVerdict" -> row.boardTruthVerdict.fold[JsValue](JsNull)(JsString(_)),
          "familyNameFit" -> row.familyNameFit.fold[JsValue](JsNull)(JsString(_)),
          "stageFit" -> row.stageFit.fold[JsValue](JsNull)(JsString(_)),
          "notes" -> row.notes.fold[JsValue](JsNull)(JsString(_))
        )

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
      activations: Map[String, PassFamilyObservation]
  )

  private final case class PassFamilyObservation(
      activation: PassActivation,
      objects: List[StrategicObject],
      deltas: List[StrategicObjectDelta],
      claims: List[CertifiedClaim]
  )

  val schema: String = "chesstory.strategicObject.batchCoverage.v2"

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
          pgnPath = stringField(js, "pgnPath"),
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
                      pgnPath = None,
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
                      pgnPath = Some(file.toString),
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
  ): (BatchCoverageReport, List[SampleFamilyActivation], List[FamilyAuditRow]) =
    val capabilityRef =
      StrategicObjectCapabilityScorecardSupport.scorecard().families.map(summary => summary.family -> summary).toMap
    val observations =
      rows
        .flatMap(sampleObservationsFor(_, capabilityRef))
        .filter(observation => familiesFilter.isEmpty || familiesFilter.contains(observation.family))
    val activations = observations.filterNot(_.bestStage == "absent")
    val observationsByFamily = observations.groupBy(_.family)
    val accessBySample =
      observations
        .collect {
          case observation if observation.family == StrategicObjectFamily.AccessNetwork.toString &&
              observation.bestStage != "absent" =>
            observation.sampleId -> observation
        }
        .toMap
    val familySummaries =
      observationsByFamily
        .toList
        .sortBy(_._1)
        .map { case (family, familyRows) =>
          summarizeFamily(family, familyRows, rows.size, capabilityRef(family), accessBySample)
        }
        .filterNot(_.sampleCount == 0)
    val auditRows =
      buildAuditRows(
        observations = observations,
        activatedFamilies = familySummaries.map(_.family).toSet,
        accessBySample = accessBySample
      )

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
      activations,
      auditRows
    )

  def renderJsonl(
      rows: List[SampleFamilyActivation]
  ): String =
    rows.map(row => Json.stringify(Json.toJson(row))).mkString("", "\n", "\n")

  def renderAuditJsonl(
      rows: List[FamilyAuditRow]
  ): String =
    rows.map(row => Json.stringify(Json.toJson(row))).mkString("", "\n", "\n")

  private def sampleObservationsFor(
      row: BatchInputRow,
      capabilityRef: Map[String, StrategicObjectCapabilityScorecardSupport.FamilySummary]
  ): List[SampleFamilyActivation] =
    val outcomes =
      List(
        positionPass(row, capabilityRef.keySet),
        comparativePass(row, capabilityRef.keySet)
      ) ++ row.playedUci.toList.map(_ => movePass(row, capabilityRef.keySet))

    capabilityRef.toList.sortBy(_._1).map { case (family, capability) =>
      val passContexts = outcomes.flatMap(_.activations.get(family))
      val activePasses = passContexts.map(_.activation).filterNot(_.stage == "absent")
      val bestStage = activePasses.map(_.stage).foldLeft("absent")((best, stage) => if stageRank(stage) > stageRank(best) then stage else best)
      val bestAdmission = activePasses.map(_.admission).foldLeft("none")((best, admission) => if admissionRank(admission) > admissionRank(best) then admission else best)
      val representativeContext =
        Option.when(activePasses.nonEmpty) {
          passContexts.maxBy(context => passObservationRank(context.activation))
        }

      val (objectCount, deltaCount, claimCount) =
        if activePasses.nonEmpty
        then
          (
            activePasses.map(_.objectCount).max,
            activePasses.map(_.deltaCount).max,
            activePasses.map(_.claimCount).max
          )
        else (0, 0, 0)

      SampleFamilyActivation(
        sampleId = row.sampleId,
        source = row.source,
        sourceKind = row.sourceKind,
        gameKey = row.gameKey,
        pgnPath = row.pgnPath,
        fen = row.fen,
        ply = row.ply,
        playedUci = row.playedUci,
        tags = row.tags,
        opening = row.opening,
        mixBucket = row.mixBucket,
        family = family,
        capabilityMaturity = capability.maturity,
        bestStage = bestStage,
        bestAdmission = bestAdmission,
        axes = activePasses.map(_.axis).distinct.sorted,
        certificationStatuses = activePasses.flatMap(_.certificationStatuses).distinct.sorted,
        objectCount = objectCount,
        deltaCount = deltaCount,
        claimCount = claimCount,
        familyTriggerSummary =
          representativeContext.map(context =>
            familyTriggerSummary(
              family = family,
              objects = context.objects,
              deltas = context.deltas,
              claims = context.claims
            )
          ).getOrElse("absent_on_sample"),
        passes = activePasses.sortBy(pass => stageRank(pass.stage))
      )
    }

  private def summarizeFamily(
      family: String,
      observations: List[SampleFamilyActivation],
      totalSamples: Int,
      capability: StrategicObjectCapabilityScorecardSupport.FamilySummary,
      accessBySample: Map[String, SampleFamilyActivation]
  ): BatchFamilySummary =
    val activations = observations.filterNot(_.bestStage == "absent")
    val sampleCount = activations.map(_.sampleId).distinct.size
    val accessOverlapCount =
      activations.count(activation => accessBySample.contains(activation.sampleId))
    val uniqueOwnerCount =
      activations.count(activation =>
        activation.bestAdmission == "primary" &&
          accessBySample.get(activation.sampleId).forall(_.bestAdmission != "primary")
      )
    val accessResidualCount =
      activations.count(activation =>
        accessBySample.get(activation.sampleId).forall(_.bestAdmission != "primary")
      )

    BatchFamilySummary(
      family = family,
      capabilityMaturity = capability.maturity,
      capabilityObjectStage = capability.objectStage,
      capabilityDeliveryStage = capability.deliveryStage,
      sampleCount = sampleCount,
      gameCount = activations.flatMap(_.gameKey).distinct.size,
      sampleHitRate = percent(sampleCount, totalSamples),
      primaryCount = activations.count(_.bestAdmission == "primary"),
      supportCount = activations.count(_.bestAdmission == "support"),
      ownerRate = percent(activations.count(_.bestAdmission == "primary"), sampleCount),
      shadowRate = percent(accessOverlapCount, sampleCount),
      uniqueOwnerRate = percent(uniqueOwnerCount, sampleCount),
      plannerNoneCount = activations.count(_.bestStage == "planner_none"),
      certificationOnlyCount = activations.count(_.bestStage == "certification"),
      deltaOnlyCount = activations.count(_.bestStage == "delta"),
      objectOnlyCount = activations.count(_.bestStage == "object"),
      accessNetworkResidualCount = accessResidualCount,
      highestBatchStage =
        if activations.nonEmpty then activations.maxBy(row => stageRank(row.bestStage)).bestStage else "absent",
      axisDiversity = summarizeAxisDiversity(activations),
      stageConcentration = summarizeStageConcentration(activations),
      byAxis =
        if activations.isEmpty then Nil
        else
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

  private def summarizeAxisDiversity(
      rows: List[SampleFamilyActivation]
  ): AxisDiversitySummary =
    val combinations =
      rows
        .map(row => row.axes.sorted)
        .filter(_.nonEmpty)
        .groupBy(identity)
        .view
        .mapValues(_.size)
        .toMap
    val dominant =
      combinations.toList.sortBy { case (axes, count) => (-count, axes.mkString("|")) }.headOption

    AxisDiversitySummary(
      combinationCount = combinations.size,
      entropy = entropy(combinations.values),
      dominantCombination = dominant.map(_._1).getOrElse(Nil),
      dominantShare = dominant.map { case (_, count) => percent(count, rows.size) }.getOrElse(0.0)
    )

  private def summarizeStageConcentration(
      rows: List[SampleFamilyActivation]
  ): StageConcentrationSummary =
    val stageCounts =
      rows
        .groupBy(_.bestStage)
        .view
        .mapValues(_.size)
        .toMap
    val dominant =
      stageCounts.toList.sortBy { case (stage, count) => (-count, stage) }.headOption

    StageConcentrationSummary(
      entropy = entropy(stageCounts.values),
      dominantStage = dominant.map(_._1).getOrElse("absent"),
      dominantShare = dominant.map { case (_, count) => percent(count, rows.size) }.getOrElse(0.0)
    )

  private def positionPass(
      row: BatchInputRow,
      families: Set[String]
  ): PassOutcome =
    evaluatePass(
      axis = "WhatMattersHere",
      fen = row.fen,
      truth = PrimitiveExtractionTest.neutralTruthFrame,
      contract = PrimitiveExtractionTest.neutralContract,
      families = families
    )

  private def comparativePass(
      row: BatchInputRow,
      families: Set[String]
  ): PassOutcome =
    val truth = PrimitiveExtractionTest.neutralTruthFrame.copy(visibilityRole = TruthVisibilityRole.PrimaryVisible)
    evaluatePass(
      axis = "WhatChanged",
      fen = row.fen,
      truth = truth,
      contract = truth.toContract,
      families = families
    )

  private def movePass(
      row: BatchInputRow,
      families: Set[String]
  ): PassOutcome =
    val playedUci = row.playedUci.getOrElse(throw new IllegalArgumentException(s"${row.sampleId}: missing playedUci"))
    evaluatePass(
      axis = "WhyThis",
      fen = row.fen,
      truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor(playedUci),
      contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor(playedUci),
      families = families
    )

  private def positionPass(
      row: BatchInputRow
  ): PassOutcome =
    positionPass(row, Set.empty)

  private def comparativePass(
      row: BatchInputRow
  ): PassOutcome =
    comparativePass(row, Set.empty)

  private def movePass(
      row: BatchInputRow
  ): PassOutcome =
    movePass(row, Set.empty)

  private def evaluatePass(
      axis: String,
      fen: String,
      truth: lila.llm.analysis.MoveTruthFrame,
      contract: lila.llm.analysis.DecisiveTruthContract,
      families: Set[String]
  ): PassOutcome =
    val objects = StrategicObjectSynthesizerTest.objectsForFen(fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val planned = CanonicalQuestionPlanner.plan(contract, claims)

    val objectsByFamily = objects.groupBy(_.family.toString)
    val objectIdsByFamily = objectsByFamily.view.mapValues(_.map(_.id).toSet).toMap
    val allFamilies =
      families ++
        objectsByFamily.keySet ++
        deltas.map(_.family.toString) ++
        claims.flatMap(claim =>
          objects.find(_.id == claim.objectId).map(_.family.toString).orElse(claim.delta.map(_.family.toString))
        )

    val activations =
      allFamilies.toList.sorted.map { family =>
        val objectIds = objectIdsByFamily.getOrElse(family, Set.empty)
        val familyObjects = objectsByFamily.getOrElse(family, Nil)
        val familyDeltas = deltas.filter(delta => delta.family.toString == family)
        val familyClaims =
          claims.filter(claim =>
            objectIds.contains(claim.objectId) ||
              claim.delta.exists(_.family.toString == family)
          )
        val admission = admissionFor(planned, familyClaims)
        val stage = stageFor(admission, familyClaims, familyDeltas, objectIds)

        family ->
          PassFamilyObservation(
            activation =
              PassActivation(
                pass = axis,
                axis = planned.axis.toString,
                stage = stage,
                admission = admission,
                certificationStatuses = familyClaims.map(_.status.toString).distinct.sorted,
                objectCount = objectIds.size,
                deltaCount = familyDeltas.size,
                claimCount = familyClaims.size
              ),
            objects = familyObjects,
            deltas = familyDeltas,
            claims = familyClaims
          )
      }.toMap

    PassOutcome(axis = planned.axis.toString, activations = activations)

  private def familyTriggerSummary(
      family: String,
      objects: List[StrategicObject],
      deltas: List[StrategicObjectDelta],
      claims: List[CertifiedClaim]
  ): String =
    val summary =
      objects.sortBy(_.id).headOption.map(_.profile match
        case StrategicObjectProfile.AccessNetwork(lane, route, roles, contestedSquares) =>
          s"lane=${lane.map(_.char).getOrElse('-')};route=${route.map(_.allSquares.size).getOrElse(0)};contested=${contestedSquares.size};roles=${roles.toList.map(_.forsyth).sorted.mkString}"
        case StrategicObjectProfile.TradeInvariant(exchangeSquares, invariantSquares, preservedFiles, preservedFamilies, features) =>
          s"exchange=${exchangeSquares.size};persistent=${invariantSquares.diff(exchangeSquares).distinct.size};preserved=${preservedFamilies.toList.map(_.toString).sorted.mkString("+")};features=${features.toList.map(_.toString).sorted.mkString("+")}"
        case StrategicObjectProfile.CounterplayAxis(resourceSquares, breakSquares, pressureSquares, typedAxes) =>
          s"typedAxes=${typedAxes.toList.map(_.toString).sorted.mkString("+")};resources=${resourceSquares.size};breaks=${breakSquares.size};pressure=${pressureSquares.size}"
        case StrategicObjectProfile.ConversionFunnel(entrySquares, channelSquares, exitSquares, funnelFiles, features) =>
          s"entry=${entrySquares.size};channel=${channelSquares.size};exit=${exitSquares.size};files=${funnelFiles.toList.map(_.char).sorted.mkString};features=${features.toList.map(_.toString).sorted.mkString("+")}"
        case StrategicObjectProfile.PlanRace(rivalOwner, raceSquares, raceFiles, ownGoalSquares, rivalGoalSquares, features) =>
          s"rival=${rivalOwner.name};raceSquares=${raceSquares.size};raceFiles=${raceFiles.toList.map(_.char).sorted.mkString};ownGoals=${ownGoalSquares.size};rivalGoals=${rivalGoalSquares.size};features=${features.toList.map(_.toString).sorted.mkString("+")}"
        case StrategicObjectProfile.BreakAxis(sourceSquare, breakSquare, targetSquares, mode, supportBalance) =>
          s"source=${sourceSquare.key};break=${breakSquare.key};targets=${targetSquares.size};mode=${mode.toString};support=${supportBalance}"
        case StrategicObjectProfile.FixedTargetComplex(targetSquare, targetOwner, originSquares, fixed, defended) =>
          s"target=${targetSquare.key};owner=${targetOwner.name};origins=${originSquares.size};fixed=$fixed;defended=$defended"
        case StrategicObjectProfile.PasserComplex(passerSquare, promotionSquare, relativeRank, protectedByPawn, escortSquares) =>
          s"passer=${passerSquare.key};promotion=${promotionSquare.key};rank=$relativeRank;protected=$protectedByPawn;escorts=${escortSquares.size}"
        case other =>
          s"profile=${other.family.toString};objects=${objects.size};deltas=${deltas.size};claims=${claims.size}"
      )

    summary.getOrElse(s"profile=$family;objects=${objects.size};deltas=${deltas.size};claims=${claims.size}")

  private def buildAuditRows(
      observations: List[SampleFamilyActivation],
      activatedFamilies: Set[String],
      accessBySample: Map[String, SampleFamilyActivation]
  ): List[FamilyAuditRow] =
    val observationIndex =
      observations.groupBy(_.family).view.mapValues(_.sortBy(_.sampleId)).toMap

    activatedFamilies.toList.sorted.flatMap { family =>
      val familyRows = observationIndex.getOrElse(family, Nil)
      val positives = familyRows.filterNot(_.bestStage == "absent")
      val negatives = familyRows.filter(_.bestStage == "absent")

      val topRows = selectTopAuditRows(positives, 50)
      val middleRows = selectMiddleAuditRows(positives.diff(topRows), 20)
      val randomRows = selectRandomAuditRows(positives.diff(topRows).diff(middleRows), 20, family)
      val hardNegativeRows = selectHardNegativeRows(negatives, 20, accessBySample)

      buildBucketRows(family, "top_50", topRows, accessBySample, (row, index) =>
        s"top_rank=${index + 1};stage=${row.bestStage};admission=${row.bestAdmission};score=${auditRowRank(row)}"
      ) ++
        buildBucketRows(family, "middle_20", middleRows, accessBySample, (row, index) =>
          s"middle_rank=${index + 1};stage=${row.bestStage};admission=${row.bestAdmission};score=${auditRowRank(row)}"
        ) ++
        buildBucketRows(family, "random_20", randomRows, accessBySample, (row, index) =>
          s"deterministic_random_rank=${index + 1};seed=${stableAuditHash(family, row.sampleId)}"
        ) ++
        buildBucketRows(family, "hard_negative_20", hardNegativeRows, accessBySample, (row, index) =>
          val access = accessBySample.get(row.sampleId)
          s"family_absent_rank=${index + 1};accessStage=${access.map(_.bestStage).getOrElse("absent")};accessAdmission=${access.map(_.bestAdmission).getOrElse("none")}"
        )
    }

  private def buildBucketRows(
      family: String,
      bucket: String,
      rows: List[SampleFamilyActivation],
      accessBySample: Map[String, SampleFamilyActivation],
      reason: (SampleFamilyActivation, Int) => String
  ): List[FamilyAuditRow] =
    rows.zipWithIndex.map { case (row, index) =>
      val access = accessBySample.get(row.sampleId)
      FamilyAuditRow(
        auditBucket = bucket,
        auditBucketReason = reason(row, index),
        sampleId = row.sampleId,
        source = row.source,
        sourceKind = row.sourceKind,
        gameKey = row.gameKey,
        pgnPath = row.pgnPath,
        fen = row.fen,
        ply = row.ply,
        playedUci = row.playedUci,
        opening = row.opening,
        tags = row.tags,
        mixBucket = row.mixBucket,
        family = family,
        bestStage = row.bestStage,
        bestAdmission = row.bestAdmission,
        axes = row.axes,
        objectCount = row.objectCount,
        deltaCount = row.deltaCount,
        claimCount = row.claimCount,
        familyTriggerSummary = row.familyTriggerSummary,
        accessNetworkCoactive = access.nonEmpty,
        accessNetworkStage = access.map(_.bestStage),
        accessNetworkAdmission = access.map(_.bestAdmission)
      )
    }

  private def selectTopAuditRows(
      rows: List[SampleFamilyActivation],
      limit: Int
  ): List[SampleFamilyActivation] =
    rows.sortBy(row => -auditRowRank(row)).take(limit)

  private def selectMiddleAuditRows(
      rows: List[SampleFamilyActivation],
      limit: Int
  ): List[SampleFamilyActivation] =
    if rows.isEmpty then Nil
    else
      val sorted = rows.sortBy(row => -auditRowRank(row))
      val middleStart = math.max((sorted.size - limit) / 2, 0)
      sorted.slice(middleStart, middleStart + limit)

  private def selectRandomAuditRows(
      rows: List[SampleFamilyActivation],
      limit: Int,
      family: String
  ): List[SampleFamilyActivation] =
    rows.sortBy(row => stableAuditHash(family, row.sampleId)).take(limit)

  private def selectHardNegativeRows(
      rows: List[SampleFamilyActivation],
      limit: Int,
      accessBySample: Map[String, SampleFamilyActivation]
  ): List[SampleFamilyActivation] =
    rows
      .sortBy(row =>
        (
          -accessBySample.get(row.sampleId).map(access => stageRank(access.bestStage)).getOrElse(0),
          -accessBySample.get(row.sampleId).map(access => admissionRank(access.bestAdmission)).getOrElse(0),
          stableAuditHash(row.family, row.sampleId)
        )
      )
      .take(limit)

  private def passObservationRank(
      activation: PassActivation
  ): Int =
    stageRank(activation.stage) * 1000 +
      admissionRank(activation.admission) * 100 +
      activation.claimCount * 10 +
      activation.deltaCount * 3 +
      activation.objectCount

  private def auditRowRank(
      row: SampleFamilyActivation
  ): Int =
    stageRank(row.bestStage) * 1000 +
      admissionRank(row.bestAdmission) * 100 +
      row.claimCount * 10 +
      row.deltaCount * 3 +
      row.objectCount +
      row.axes.size

  private def stableAuditHash(
      family: String,
      sampleId: String
  ): Int =
    math.abs(s"$family::$sampleId".hashCode)

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

  private def entropy(
      counts: Iterable[Int]
  ): Double =
    val total = counts.sum.toDouble
    if total <= 0 then 0.0
    else
      counts.iterator
        .filter(_ > 0)
        .map { count =>
          val p = count.toDouble / total
          -(p * (math.log(p) / math.log(2)))
        }
        .sum

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
