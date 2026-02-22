package lila.llm.analysis.structure

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import chess.Color
import chess.format.Fen
import chess.variant.Standard
import lila.llm.analysis.PositionAnalyzer
import lila.llm.model.structure.*
import play.api.libs.json.Json

object PawnStructureQualityEvaluator:

  final case class PredictedRow(
      row: StructureGoldRow,
      predictedPrimary: StructureId,
      predictedTopPlanId: Option[String]
  )

  def loadRows(path: Path): Either[String, List[StructureGoldRow]] =
    if !Files.exists(path) then Left(s"goldset not found: $path")
    else
      val lines = Files.readAllLines(path, StandardCharsets.UTF_8)
      val parsed = lines.toArray(new Array[String](lines.size())).toList.zipWithIndex
        .map { case (raw, idx) =>
          val line = raw.stripPrefix("\uFEFF").trim
          if line.isEmpty then Right(None)
          else
            Json
              .parse(line)
              .validate[StructureGoldRow]
              .asEither
              .left
              .map(err => s"${path.toString}:${idx + 1} parse error: $err")
              .map(Some(_))
        }

      val failures = parsed.collect { case Left(err) => err }
      if failures.nonEmpty then Left(failures.mkString("\n"))
      else Right(parsed.collect { case Right(Some(row)) => row })

  def predictRows(
      rows: List[StructureGoldRow],
      minConfidence: Double = 0.72,
      minMargin: Double = 0.10
  ): List[PredictedRow] =
    rows.map(predictRow(_, minConfidence, minMargin))

  def evaluate(
      rows: List[StructureGoldRow],
      minConfidence: Double = 0.72,
      minMargin: Double = 0.10,
      thresholds: GateThresholds = GateThresholds()
  ): StructureQualityReport =
    val predictions = predictRows(rows, minConfidence, minMargin)
    val labelNames = StructureId.values.toList.map(_.toString)
    val byGt = predictions.groupBy(_.row.primary.toString)

    val confusionMatrix: Map[String, Map[String, Int]] =
      labelNames.map { gt =>
        val perPred = predictions
          .filter(_.row.primary.toString == gt)
          .groupBy(_.predictedPrimary.toString)
          .view
          .mapValues(_.size)
          .toMap
        gt -> perPred
      }.toMap

    val perClass = labelNames.map { label =>
      val tp = predictions.count(r => r.row.primary.toString == label && r.predictedPrimary.toString == label)
      val fp = predictions.count(r => r.row.primary.toString != label && r.predictedPrimary.toString == label)
      val fn = predictions.count(r => r.row.primary.toString == label && r.predictedPrimary.toString != label)
      val support = byGt.getOrElse(label, Nil).size
      val precision = if tp + fp == 0 then 0.0 else tp.toDouble / (tp + fp).toDouble
      val recall = if tp + fn == 0 then 0.0 else tp.toDouble / (tp + fn).toDouble
      val f1 = if precision + recall == 0.0 then 0.0 else 2.0 * precision * recall / (precision + recall)
      label -> PerClassMetrics(precision = precision, recall = recall, f1 = f1, support = support)
    }.toMap

    val macroF1 =
      if perClass.isEmpty then 0.0
      else perClass.values.map(_.f1).sum / perClass.size.toDouble

    val unknownRows = predictions.filter(_.row.primary == StructureId.Unknown)
    val unknownMisclassified = unknownRows.count(_.predictedPrimary != StructureId.Unknown)
    val unknownFalsePositiveRate =
      if unknownRows.isEmpty then 0.0
      else unknownMisclassified.toDouble / unknownRows.size.toDouble

    val alignRows = predictions.filter(_.row.expectedTopPlanIds.nonEmpty)
    val alignHits = alignRows.count { p =>
      p.predictedTopPlanId.exists(id => p.row.expectedTopPlanIds.map(_.toString).contains(id))
    }
    val alignmentTop1 =
      if alignRows.isEmpty then 0.0
      else alignHits.toDouble / alignRows.size.toDouble

    val structure = StructureEvalMetrics(
      macroF1 = macroF1,
      unknownFalsePositiveRate = unknownFalsePositiveRate,
      perClass = perClass,
      confusionMatrix = confusionMatrix,
      evaluatedRows = predictions.size
    )
    val alignment = AlignmentEvalMetrics(
      top1Accuracy = alignmentTop1,
      evaluatedRows = alignRows.size,
      hitRows = alignHits
    )
    val gate = GateVerdict(
      macroF1Pass = macroF1 >= thresholds.macroF1,
      alignmentTop1Pass = alignmentTop1 >= thresholds.alignmentTop1,
      unknownFalsePositivePass = unknownFalsePositiveRate <= thresholds.unknownFalsePositiveRate,
      overallPass =
        macroF1 >= thresholds.macroF1 &&
          alignmentTop1 >= thresholds.alignmentTop1 &&
          unknownFalsePositiveRate <= thresholds.unknownFalsePositiveRate,
      thresholds = thresholds
    )

    StructureQualityReport(structure = structure, alignment = alignment, gate = gate)

  private def predictRow(
      row: StructureGoldRow,
      minConfidence: Double,
      minMargin: Double
  ): PredictedRow =
    val features = PositionAnalyzer.extractFeatures(row.fen, plyCount = row.sourcePly.getOrElse(1))
    val position = Fen.read(Standard, Fen.Full(row.fen))
    (for
      f <- features
      pos <- position
    yield
      val profile = PawnStructureClassifier.classify(
        features = f,
        board = pos.board,
        sideToMove = pos.color,
        minConfidence = minConfidence,
        minMargin = minMargin
      )
      val topPlan = topPlanFor(profile.primary, pos.color)
      PredictedRow(row = row, predictedPrimary = profile.primary, predictedTopPlanId = topPlan))
      .getOrElse(
        PredictedRow(
          row = row,
          predictedPrimary = StructureId.Unknown,
          predictedTopPlanId = None
        )
      )

  private def topPlanFor(structureId: StructureId, sideToMove: Color): Option[String] =
    StructuralPlaybook
      .lookup(structureId)
      .flatMap { entry =>
        val canonicalSide =
          structureId match
            case StructureId.IQPBlack | StructureId.HangingPawnsBlack => Color.Black
            case _ => Color.White
        val side = if structureId == StructureId.Unknown then sideToMove else canonicalSide
        StructuralPlaybook.expectedPlans(entry, side).headOption
      }
      .map(_.toString)
