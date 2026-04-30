package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import play.api.libs.json.*

final case class LowerDiagnosticXrayAuditSummary(
    totalRows: Int,
    xrayRows: Int,
    uniqueXrayTransitions: Int,
    rowAmplification: Int,
    countsByAuditDisposition: Map[String, Int],
    countsByLogicalDefect: Map[String, Int],
    countsByPublicClaimReadiness: Map[String, Int]
)

final case class LowerDiagnosticXrayAuditRow(
    transitionKey: String,
    sourceRowIds: Vector[String],
    duplicateRows: Int,
    gameKey: Option[String],
    ply: Int,
    playedMove: Option[String],
    beforeFen: Option[String],
    currentFen: String,
    beforeXrayFacts: Vector[String],
    afterXrayFacts: Vector[String],
    createdXrayFacts: Vector[String],
    removedXrayFacts: Vector[String],
    touchedTargetSquares: Vector[String],
    auditDisposition: String,
    logicalDefects: Vector[String],
    publicClaimReadiness: String,
    requiredNextEvidence: Vector[String],
    auditQuestion: String
)

final case class LowerDiagnosticXrayAuditReport(
    summary: LowerDiagnosticXrayAuditSummary,
    rows: Vector[LowerDiagnosticXrayAuditRow]
)

object LowerDiagnosticXrayAuditReport:

  def fromRows(rows: Vector[LowerDiagnosticLargeCorpus.Row]): LowerDiagnosticXrayAuditReport =
    val diagnostic = LowerDiagnosticReport.fromRows(rows)
    val xrayTraces = diagnostic.traces.filter(row =>
      row.trace.transition.status == "valid" && row.tacticalSchemas.contains("xray_target")
    )
    val auditRows = xrayTraces.groupBy(row => transitionKey(row.row)).toVector.map((_, grouped) => auditGroup(grouped)).sortBy(_.transitionKey)
    LowerDiagnosticXrayAuditReport(
      summary = LowerDiagnosticXrayAuditSummary(
        totalRows = rows.size,
        xrayRows = xrayTraces.size,
        uniqueXrayTransitions = auditRows.size,
        rowAmplification = xrayTraces.size - auditRows.size,
        countsByAuditDisposition = countLabels(auditRows.map(_.auditDisposition)),
        countsByLogicalDefect = countLabels(auditRows.flatMap(_.logicalDefects)),
        countsByPublicClaimReadiness = countLabels(auditRows.map(_.publicClaimReadiness))
      ),
      rows = auditRows
    )

  private def auditGroup(rows: Vector[LowerDiagnosticTraceRow]): LowerDiagnosticXrayAuditRow =
    val first = rows.minBy(_.row.id)
    val afterFacts = xrayFacts(first.trace)
    val beforeFacts =
      first.row.input.beforeFen.toVector.flatMap: beforeFen =>
        val beforeTrace = LowerLayerDiagnostic.trace(
          first.row.input.copy(currentFen = beforeFen, beforeFen = None, playedMove = None)
        )
        xrayFacts(beforeTrace)
    val created = afterFacts.diff(beforeFacts)
    val removed = beforeFacts.diff(afterFacts)
    val touched = touchedTargetSquares(first.row.input.playedMove, afterFacts)
    val disposition = auditDisposition(afterFacts, created)
    val defects = logicalDefects(first, created, touched)
    LowerDiagnosticXrayAuditRow(
      transitionKey = transitionKey(first.row),
      sourceRowIds = rows.map(_.row.id).distinct.sorted,
      duplicateRows = rows.size,
      gameKey = first.row.metadata.get("gameKey"),
      ply = first.row.input.ply,
      playedMove = first.row.input.playedMove,
      beforeFen = first.row.input.beforeFen,
      currentFen = first.row.input.currentFen,
      beforeXrayFacts = beforeFacts.sorted,
      afterXrayFacts = afterFacts.sorted,
      createdXrayFacts = created.sorted,
      removedXrayFacts = removed.sorted,
      touchedTargetSquares = touched.sorted,
      auditDisposition = disposition,
      logicalDefects = defects,
      publicClaimReadiness = "not_ready_for_public_claim",
      requiredNextEvidence = Vector(
        "slider identity",
        "blocker identity",
        "target value and owner",
        "before/after line geometry",
        "moved or captured piece relation",
        "source PGN verification",
        "standing/pre-existing anti-case"
      ),
      auditQuestion = auditQuestion(disposition)
    )

  private def xrayFacts(trace: LowerLayerDiagnostic.Trace): Vector[String] =
    trace.extraction.rootFacts.filter(_.id == "xray_target").map(_.atom).distinct

  private def auditDisposition(afterFacts: Vector[String], createdFacts: Vector[String]): String =
    if afterFacts.isEmpty then "reject_no_after_xray"
    else if createdFacts.isEmpty then "reject_preexisting_or_standing_xray"
    else "candidate_created_xray_requires_geometry_audit"

  private def logicalDefects(
      row: LowerDiagnosticTraceRow,
      createdFacts: Vector[String],
      touchedTargetSquares: Vector[String]
  ): Vector[String] =
    Vector(
      Some("xray_root_lacks_slider_blocker_identity"),
      Option.when(createdFacts.nonEmpty)("created_root_does_not_prove_move_causality"),
      Option.when(createdFacts.nonEmpty && touchedTargetSquares.isEmpty)("move_does_not_touch_xray_target_square"),
      Option.when(row.row.metadata.get("pgnPath").forall(path => !Files.exists(Paths.get(path))))("source_pgn_unverified")
    ).flatten.distinct

  private def touchedTargetSquares(playedMove: Option[String], facts: Vector[String]): Vector[String] =
    val touched = playedMove.toVector.flatMap(move => Vector(move.take(2), move.slice(2, 4)))
    facts.flatMap(targetSquare).filter(touched.contains).distinct

  private def targetSquare(atom: String): Option[String] =
    atom.dropWhile(_ != '(').drop(1).takeWhile(_ != ')').split(',').toVector.lift(1).filter(_.nonEmpty)

  private def auditQuestion(disposition: String): String =
    disposition match
      case "reject_preexisting_or_standing_xray" =>
        "This xray_target already existed before the move; keep it out of move-causal public claims."
      case "candidate_created_xray_requires_geometry_audit" =>
        "The root appeared after the move, but the root lacks slider/blocker identity; verify exact geometry before any admission."
      case _ =>
        "Do not use this row for xray admission unless the after-board root and transition identity are repaired."

  private def transitionKey(row: LowerDiagnosticLargeCorpus.Row): String =
    val gameKey = row.metadata.getOrElse("gameKey", row.metadata.getOrElse("sourceId", row.id))
    val move = row.input.playedMove.orElse(row.metadata.get("playedUci")).getOrElse("none")
    s"$gameKey|ply:${row.input.ply}|move:$move"

  private def countLabels(values: Vector[String]): Map[String, Int] =
    values.groupMapReduce(identity)(_ => 1)(_ + _).toVector.sortBy(_._1).toMap.withDefaultValue(0)

object LowerDiagnosticXrayAuditJson:

  def summaryJson(summary: LowerDiagnosticXrayAuditSummary): JsObject =
    Json.obj(
      "totalRows" -> summary.totalRows,
      "xrayRows" -> summary.xrayRows,
      "uniqueXrayTransitions" -> summary.uniqueXrayTransitions,
      "rowAmplification" -> summary.rowAmplification,
      "countsByAuditDisposition" -> summary.countsByAuditDisposition,
      "countsByLogicalDefect" -> summary.countsByLogicalDefect,
      "countsByPublicClaimReadiness" -> summary.countsByPublicClaimReadiness
    )

  def rowJson(row: LowerDiagnosticXrayAuditRow): JsObject =
    Json.obj(
      "transitionKey" -> row.transitionKey,
      "sourceRowIds" -> row.sourceRowIds,
      "duplicateRows" -> row.duplicateRows,
      "gameKey" -> row.gameKey,
      "ply" -> row.ply,
      "playedMove" -> row.playedMove,
      "beforeFen" -> row.beforeFen,
      "currentFen" -> row.currentFen,
      "beforeXrayFacts" -> row.beforeXrayFacts,
      "afterXrayFacts" -> row.afterXrayFacts,
      "createdXrayFacts" -> row.createdXrayFacts,
      "removedXrayFacts" -> row.removedXrayFacts,
      "touchedTargetSquares" -> row.touchedTargetSquares,
      "auditDisposition" -> row.auditDisposition,
      "logicalDefects" -> row.logicalDefects,
      "publicClaimReadiness" -> row.publicClaimReadiness,
      "requiredNextEvidence" -> row.requiredNextEvidence,
      "auditQuestion" -> row.auditQuestion
    )

object LowerDiagnosticXrayAuditRunner:

  private val defaultOutputDir = Paths.get("tmp/commentary-diagnostic/lower-layer/xray-audit")

  def main(args: Array[String]): Unit =
    val options = RunnerOptions.parse(args.toVector)
    val rows =
      options.inputPath match
        case Some(path) => LowerDiagnosticLargeCorpus.loadExternalRows(path, options.limit)
        case None       => LowerDiagnosticLargeCorpus.loadTrackedRows()
    write(LowerDiagnosticXrayAuditReport.fromRows(rows), options.outputDir)

  def write(report: LowerDiagnosticXrayAuditReport, outputDir: Path): Unit =
    Files.createDirectories(outputDir)
    Files.writeString(
      outputDir.resolve("xray-audit-summary.json"),
      Json.prettyPrint(LowerDiagnosticXrayAuditJson.summaryJson(report.summary)) + System.lineSeparator(),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      outputDir.resolve("xray-audit-transitions.jsonl"),
      report.rows.map(LowerDiagnosticXrayAuditJson.rowJson).map(Json.stringify).mkString("", System.lineSeparator(), System.lineSeparator()),
      StandardCharsets.UTF_8
    )

  private final case class RunnerOptions(outputDir: Path, inputPath: Option[Path], limit: Option[Int])

  private object RunnerOptions:
    def parse(args: Vector[String]): RunnerOptions =
      def valueAfter(flag: String): Option[String] =
        args.sliding(2).collectFirst { case Vector(`flag`, value) => value }
      RunnerOptions(
        outputDir = valueAfter("--out").map(Paths.get(_)).getOrElse(defaultOutputDir),
        inputPath = valueAfter("--input").map(Paths.get(_)),
        limit = valueAfter("--limit").flatMap(_.toIntOption)
      )
