package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import chess.format.Fen
import play.api.libs.json.*

import lila.commentary.CommentaryCore
import lila.commentary.strategic.StrategicObject
import lila.commentary.witness.WitnessValue

final case class AttackScaffoldImpactRow(
    rowId: String,
    sourceFile: String,
    sourceKind: String,
    sourceSchema: Option[String],
    currentFen: String,
    hasAttackScaffold: Boolean,
    attackScaffoldCount: Int,
    attackScaffoldColors: Vector[String],
    supportFragmentIds: Vector[String],
    carrierFragmentIds: Vector[String],
    smellSupportIds: Vector[String],
    extractionError: Option[String]
)

final case class AttackScaffoldImpactSummary(
    total: Int,
    extracted: Int,
    extractionErrors: Int,
    currentAttackScaffoldRows: Int,
    currentAttackScaffoldUniqueFen: Int,
    currentAttackScaffoldObjects: Int,
    currentSmellSupportedAttackScaffoldRows: Int,
    currentSmellSupportedAttackScaffoldUniqueFen: Int,
    sourceSchemaAttackScaffoldRows: Int,
    sourceSchemaAttackScaffoldUniqueFen: Int,
    sourceSchemaAttackScaffoldStillCurrent: Int,
    sourceSchemaAttackScaffoldStillCurrentUniqueFen: Int,
    sourceSchemaAttackScaffoldDropped: Int,
    sourceSchemaAttackScaffoldDroppedUniqueFen: Int,
    sourceSchemaS01Rows: Int,
    sourceSchemaS01WithAttackScaffold: Int,
    sourceSchemaS02Rows: Int,
    sourceSchemaS02WithAttackScaffold: Int,
    sourceSchemaS03Rows: Int,
    sourceSchemaS03WithAttackScaffold: Int,
    supportFragmentCounts: Map[String, Int],
    carrierFragmentCounts: Map[String, Int],
    smellSupportCounts: Map[String, Int],
    attackScaffoldRowsBySourceSchema: Map[String, Int]
)

final case class AttackScaffoldFenAuditRow(
    auditKind: String,
    currentFen: String,
    rowCount: Int,
    rowIds: Vector[String],
    sourceKinds: Vector[String],
    sourceSchemas: Vector[String],
    sideToMove: Option[String],
    fullmoveNumber: Option[Int],
    currentStatus: String,
    attackScaffoldColors: Vector[String],
    supportFragmentIds: Vector[String],
    carrierFragmentIds: Vector[String],
    smellSupportIds: Vector[String],
    extractionErrors: Vector[String],
    auditConclusion: String
)

final case class AttackScaffoldImpactReport(
    summary: AttackScaffoldImpactSummary,
    rows: Vector[AttackScaffoldImpactRow]
)

object AttackScaffoldImpactReport:

  private val TacticalSmellSupportIds = Set("loose_piece", "pinned_piece", "xray_target")

  def fromRows(rows: Vector[LowerDiagnosticLargeCorpus.Row]): AttackScaffoldImpactReport =
    val impactRows = rows.map(rowFromCorpusRow)
    fromImpactRows(impactRows)

  def fromImpactRows(rows: Vector[AttackScaffoldImpactRow]): AttackScaffoldImpactReport =
    AttackScaffoldImpactReport(summary(rows), rows)

  def sourceAttackScaffoldFenLedger(rows: Vector[AttackScaffoldImpactRow]): Vector[AttackScaffoldFenAuditRow] =
    rows
      .filter(_.sourceSchema.contains("AttackScaffold"))
      .groupBy(_.currentFen)
      .toVector
      .sortBy(_._1)
      .map { case (fen, fenRows) =>
        val errors = fenRows.flatMap(_.extractionError).distinct.sorted
        val hasCurrent = fenRows.exists(_.hasAttackScaffold)
        val currentStatus =
          if errors.nonEmpty then "extraction_error"
          else if hasCurrent then "current_contract_still_current"
          else "current_contract_dropped"
        fenAuditRow(
          auditKind = "source_attack_scaffold_status",
          fen = fen,
          rows = fenRows,
          currentStatus = currentStatus,
          auditConclusion =
            currentStatus match
              case "current_contract_dropped"       => "current_contract_dropped_not_false_positive_proof"
              case "current_contract_still_current" => "current_contract_still_requires_exact_board_audit"
              case _                                => "input_invalid_not_chess_audit"
        )
      }

  def currentAttackScaffoldFenLedger(rows: Vector[AttackScaffoldImpactRow]): Vector[AttackScaffoldFenAuditRow] =
    rows
      .filter(_.hasAttackScaffold)
      .groupBy(_.currentFen)
      .toVector
      .sortBy(_._1)
      .map { case (fen, fenRows) =>
        val smellSupported = fenRows.exists(_.smellSupportIds.nonEmpty)
        fenAuditRow(
          auditKind = "current_attack_scaffold_residual",
          fen = fen,
          rows = fenRows,
          currentStatus = if smellSupported then "current_residual_smell_supported" else "current_residual_no_smell_support",
          auditConclusion =
            if smellSupported then "current_residual_requires_exact_board_audit"
            else "current_residual_non_smell_scaffold"
        )
      }

  private def rowFromCorpusRow(row: LowerDiagnosticLargeCorpus.Row): AttackScaffoldImpactRow =
    CommentaryCore.extractStrategicObjectsFailClosed(Fen.Full.clean(row.input.currentFen)) match
      case Left(reason) =>
        AttackScaffoldImpactRow(
          rowId = row.id,
          sourceFile = row.sourceFile,
          sourceKind = row.sourceKind,
          sourceSchema = row.sourceSchema,
          currentFen = row.input.currentFen,
          hasAttackScaffold = false,
          attackScaffoldCount = 0,
          attackScaffoldColors = Vector.empty,
          supportFragmentIds = Vector.empty,
          carrierFragmentIds = Vector.empty,
          smellSupportIds = Vector.empty,
          extractionError = Some(reason)
        )
      case Right(extraction) =>
        val scaffolds = extraction.objects.forFamilyId("AttackScaffold")
        val supportIds = tokenListValues(scaffolds, "support_fragment_ids")
        val carrierIds = tokenListValues(scaffolds, "carrier_fragment_ids")
        val smellIds = supportIds.filter(TacticalSmellSupportIds.contains).distinct.sorted
        AttackScaffoldImpactRow(
          rowId = row.id,
          sourceFile = row.sourceFile,
          sourceKind = row.sourceKind,
          sourceSchema = row.sourceSchema,
          currentFen = row.input.currentFen,
          hasAttackScaffold = scaffolds.nonEmpty,
          attackScaffoldCount = scaffolds.size,
          attackScaffoldColors = scaffolds.flatMap(_.color.map(color => if color.white then "white" else "black")).distinct.sorted,
          supportFragmentIds = supportIds.distinct.sorted,
          carrierFragmentIds = carrierIds.distinct.sorted,
          smellSupportIds = smellIds,
          extractionError = None
        )

  private def tokenListValues(objects: Vector[StrategicObject], key: String): Vector[String] =
    objects.flatMap(_.payload.get(key)).flatMap:
      case WitnessValue.TokenListValue(values) => values
      case WitnessValue.Token(value)           => Vector(value)
      case _                                   => Vector.empty

  private def summary(rows: Vector[AttackScaffoldImpactRow]): AttackScaffoldImpactSummary =
    val sourceAttackScaffold = rows.filter(_.sourceSchema.contains("AttackScaffold"))
    val sourceS01 = rows.filter(_.sourceSchema.contains("S01"))
    val sourceS02 = rows.filter(_.sourceSchema.contains("S02"))
    val sourceS03 = rows.filter(_.sourceSchema.contains("S03"))
    val currentAttackScaffold = rows.filter(_.hasAttackScaffold)
    val currentSmellSupported = rows.filter(row => row.hasAttackScaffold && row.smellSupportIds.nonEmpty)
    val sourceStillCurrent = sourceAttackScaffold.filter(_.hasAttackScaffold)
    val sourceDropped = sourceAttackScaffold.filter(row => !row.hasAttackScaffold && row.extractionError.isEmpty)
    AttackScaffoldImpactSummary(
      total = rows.size,
      extracted = rows.count(_.extractionError.isEmpty),
      extractionErrors = rows.count(_.extractionError.nonEmpty),
      currentAttackScaffoldRows = currentAttackScaffold.size,
      currentAttackScaffoldUniqueFen = uniqueFenCount(currentAttackScaffold),
      currentAttackScaffoldObjects = rows.map(_.attackScaffoldCount).sum,
      currentSmellSupportedAttackScaffoldRows = currentSmellSupported.size,
      currentSmellSupportedAttackScaffoldUniqueFen = uniqueFenCount(currentSmellSupported),
      sourceSchemaAttackScaffoldRows = sourceAttackScaffold.size,
      sourceSchemaAttackScaffoldUniqueFen = uniqueFenCount(sourceAttackScaffold),
      sourceSchemaAttackScaffoldStillCurrent = sourceStillCurrent.size,
      sourceSchemaAttackScaffoldStillCurrentUniqueFen = uniqueFenCount(sourceStillCurrent),
      sourceSchemaAttackScaffoldDropped = sourceDropped.size,
      sourceSchemaAttackScaffoldDroppedUniqueFen = uniqueFenCount(sourceDropped),
      sourceSchemaS01Rows = sourceS01.size,
      sourceSchemaS01WithAttackScaffold = sourceS01.count(_.hasAttackScaffold),
      sourceSchemaS02Rows = sourceS02.size,
      sourceSchemaS02WithAttackScaffold = sourceS02.count(_.hasAttackScaffold),
      sourceSchemaS03Rows = sourceS03.size,
      sourceSchemaS03WithAttackScaffold = sourceS03.count(_.hasAttackScaffold),
      supportFragmentCounts = countBy(rows.flatMap(_.supportFragmentIds)),
      carrierFragmentCounts = countBy(rows.flatMap(_.carrierFragmentIds)),
      smellSupportCounts = countBy(rows.flatMap(_.smellSupportIds)),
      attackScaffoldRowsBySourceSchema = countBy(rows.filter(_.hasAttackScaffold).flatMap(_.sourceSchema))
    )

  private def countBy(values: Vector[String]): Map[String, Int] =
    values.groupMapReduce(identity)(_ => 1)(_ + _).toVector.sortBy(_._1).toMap

  private def uniqueFenCount(rows: Vector[AttackScaffoldImpactRow]): Int =
    rows.map(_.currentFen).distinct.size

  private def fenAuditRow(
      auditKind: String,
      fen: String,
      rows: Vector[AttackScaffoldImpactRow],
      currentStatus: String,
      auditConclusion: String
  ): AttackScaffoldFenAuditRow =
    AttackScaffoldFenAuditRow(
      auditKind = auditKind,
      currentFen = fen,
      rowCount = rows.size,
      rowIds = rows.map(_.rowId).distinct.sorted,
      sourceKinds = rows.map(_.sourceKind).distinct.sorted,
      sourceSchemas = rows.flatMap(_.sourceSchema).distinct.sorted,
      sideToMove = sideToMove(fen),
      fullmoveNumber = fullmoveNumber(fen),
      currentStatus = currentStatus,
      attackScaffoldColors = rows.flatMap(_.attackScaffoldColors).distinct.sorted,
      supportFragmentIds = rows.flatMap(_.supportFragmentIds).distinct.sorted,
      carrierFragmentIds = rows.flatMap(_.carrierFragmentIds).distinct.sorted,
      smellSupportIds = rows.flatMap(_.smellSupportIds).distinct.sorted,
      extractionErrors = rows.flatMap(_.extractionError).distinct.sorted,
      auditConclusion = auditConclusion
    )

  private def sideToMove(fen: String): Option[String] =
    fen.split("\\s+").lift(1).collect:
      case "w" => "white"
      case "b" => "black"

  private def fullmoveNumber(fen: String): Option[Int] =
    fen.split("\\s+").lift(5).flatMap(_.toIntOption)

object AttackScaffoldImpactReportJson:

  def summaryJson(summary: AttackScaffoldImpactSummary): JsObject =
    Json.obj(
      "total" -> summary.total,
      "extracted" -> summary.extracted,
      "extractionErrors" -> summary.extractionErrors,
      "currentAttackScaffoldRows" -> summary.currentAttackScaffoldRows,
      "currentAttackScaffoldUniqueFen" -> summary.currentAttackScaffoldUniqueFen,
      "currentAttackScaffoldObjects" -> summary.currentAttackScaffoldObjects,
      "currentSmellSupportedAttackScaffoldRows" -> summary.currentSmellSupportedAttackScaffoldRows,
      "currentSmellSupportedAttackScaffoldUniqueFen" -> summary.currentSmellSupportedAttackScaffoldUniqueFen,
      "sourceSchemaAttackScaffoldRows" -> summary.sourceSchemaAttackScaffoldRows,
      "sourceSchemaAttackScaffoldUniqueFen" -> summary.sourceSchemaAttackScaffoldUniqueFen,
      "sourceSchemaAttackScaffoldStillCurrent" -> summary.sourceSchemaAttackScaffoldStillCurrent,
      "sourceSchemaAttackScaffoldStillCurrentUniqueFen" -> summary.sourceSchemaAttackScaffoldStillCurrentUniqueFen,
      "sourceSchemaAttackScaffoldDropped" -> summary.sourceSchemaAttackScaffoldDropped,
      "sourceSchemaAttackScaffoldDroppedUniqueFen" -> summary.sourceSchemaAttackScaffoldDroppedUniqueFen,
      "sourceSchemaS01Rows" -> summary.sourceSchemaS01Rows,
      "sourceSchemaS01WithAttackScaffold" -> summary.sourceSchemaS01WithAttackScaffold,
      "sourceSchemaS02Rows" -> summary.sourceSchemaS02Rows,
      "sourceSchemaS02WithAttackScaffold" -> summary.sourceSchemaS02WithAttackScaffold,
      "sourceSchemaS03Rows" -> summary.sourceSchemaS03Rows,
      "sourceSchemaS03WithAttackScaffold" -> summary.sourceSchemaS03WithAttackScaffold,
      "supportFragmentCounts" -> summary.supportFragmentCounts,
      "carrierFragmentCounts" -> summary.carrierFragmentCounts,
      "smellSupportCounts" -> summary.smellSupportCounts,
      "attackScaffoldRowsBySourceSchema" -> summary.attackScaffoldRowsBySourceSchema
    )

  def rowJson(row: AttackScaffoldImpactRow): JsObject =
    Json.obj(
      "rowId" -> row.rowId,
      "sourceFile" -> row.sourceFile,
      "sourceKind" -> row.sourceKind,
      "sourceSchema" -> row.sourceSchema,
      "currentFen" -> row.currentFen,
      "hasAttackScaffold" -> row.hasAttackScaffold,
      "attackScaffoldCount" -> row.attackScaffoldCount,
      "attackScaffoldColors" -> row.attackScaffoldColors,
      "supportFragmentIds" -> row.supportFragmentIds,
      "carrierFragmentIds" -> row.carrierFragmentIds,
      "smellSupportIds" -> row.smellSupportIds,
      "extractionError" -> row.extractionError
    )

  def fenAuditRowJson(row: AttackScaffoldFenAuditRow): JsObject =
    Json.obj(
      "auditKind" -> row.auditKind,
      "currentFen" -> row.currentFen,
      "rowCount" -> row.rowCount,
      "rowIds" -> row.rowIds,
      "sourceKinds" -> row.sourceKinds,
      "sourceSchemas" -> row.sourceSchemas,
      "sideToMove" -> row.sideToMove,
      "fullmoveNumber" -> row.fullmoveNumber,
      "currentStatus" -> row.currentStatus,
      "attackScaffoldColors" -> row.attackScaffoldColors,
      "supportFragmentIds" -> row.supportFragmentIds,
      "carrierFragmentIds" -> row.carrierFragmentIds,
      "smellSupportIds" -> row.smellSupportIds,
      "extractionErrors" -> row.extractionErrors,
      "auditConclusion" -> row.auditConclusion
    )

object AttackScaffoldImpactReportRunner:

  private val defaultOutputDir = Paths.get("tmp/commentary-diagnostic/attack-scaffold-impact")

  def main(args: Array[String]): Unit =
    val options = RunnerOptions.parse(args.toVector)
    val rows =
      options.inputPath match
        case Some(path) => LowerDiagnosticLargeCorpus.loadExternalRows(path, options.limit)
        case None       => LowerDiagnosticLargeCorpus.loadTrackedRows()
    write(AttackScaffoldImpactReport.fromRows(rows), options.outputDir)

  def write(report: AttackScaffoldImpactReport, outputDir: Path): Unit =
    Files.createDirectories(outputDir)
    Files.writeString(
      outputDir.resolve("attack-scaffold-impact-summary.json"),
      Json.prettyPrint(AttackScaffoldImpactReportJson.summaryJson(report.summary)) + System.lineSeparator(),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      outputDir.resolve("attack-scaffold-impact-rows.jsonl"),
      report.rows.map(AttackScaffoldImpactReportJson.rowJson).map(Json.stringify).mkString("", System.lineSeparator(), System.lineSeparator()),
      StandardCharsets.UTF_8
    )
    val sourceLedger = AttackScaffoldImpactReport.sourceAttackScaffoldFenLedger(report.rows)
    Files.writeString(
      outputDir.resolve("attack-scaffold-source-status-ledger.jsonl"),
      sourceLedger.map(AttackScaffoldImpactReportJson.fenAuditRowJson).map(Json.stringify).mkString("", System.lineSeparator(), System.lineSeparator()),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      outputDir.resolve("attack-scaffold-dropped-source-ledger.jsonl"),
      sourceLedger
        .filter(_.currentStatus == "current_contract_dropped")
        .map(AttackScaffoldImpactReportJson.fenAuditRowJson)
        .map(Json.stringify)
        .mkString("", System.lineSeparator(), System.lineSeparator()),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      outputDir.resolve("attack-scaffold-current-residual-ledger.jsonl"),
      AttackScaffoldImpactReport
        .currentAttackScaffoldFenLedger(report.rows)
        .map(AttackScaffoldImpactReportJson.fenAuditRowJson)
        .map(Json.stringify)
        .mkString("", System.lineSeparator(), System.lineSeparator()),
      StandardCharsets.UTF_8
    )
    val dropped =
      report.rows
        .filter(row => row.sourceSchema.contains("AttackScaffold") && !row.hasAttackScaffold && row.extractionError.isEmpty)
        .take(50)
    Files.writeString(
      outputDir.resolve("attack-scaffold-dropped-source-sample.jsonl"),
      dropped.map(AttackScaffoldImpactReportJson.rowJson).map(Json.stringify).mkString("", System.lineSeparator(), System.lineSeparator()),
      StandardCharsets.UTF_8
    )

  private final case class RunnerOptions(
      outputDir: Path,
      inputPath: Option[Path],
      limit: Option[Int]
  )

  private object RunnerOptions:
    def parse(args: Vector[String]): RunnerOptions =
      def valueAfter(flag: String): Option[String] =
        args.sliding(2).collectFirst { case Vector(`flag`, value) => value }
      RunnerOptions(
        outputDir = valueAfter("--out").map(Paths.get(_)).getOrElse(defaultOutputDir),
        inputPath = valueAfter("--input").map(Paths.get(_)),
        limit = valueAfter("--limit").flatMap(_.toIntOption)
      )
