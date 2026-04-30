package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import play.api.libs.json.*

final case class LowerDiagnosticTraceRow(
    row: LowerDiagnosticLargeCorpus.Row,
    trace: LowerLayerDiagnostic.Trace,
    layerBreak: String,
    tacticalSchemas: Vector[String],
    falsePositiveRisks: Vector[String],
    handoffGroup: String
)

final case class LowerDiagnosticReportSummary(
    total: Int,
    countsBySourceFile: Map[String, Int],
    countsBySourceKind: Map[String, Int],
    countsByLayerBreak: Map[String, Int],
    countsByTacticalSchema: Map[String, Int],
    countsByFalsePositiveRisk: Map[String, Int],
    countsByHandoffGroup: Map[String, Int]
)

final case class LowerDiagnosticAuditSample(
    rowId: String,
    sourceFile: String,
    sourceKind: String,
    sourceSchema: Option[String],
    caseType: Option[String],
    currentFen: String,
    beforeFen: Option[String],
    playedMove: Option[String],
    nodeId: String,
    ply: Int,
    layerBreak: String,
    tacticalSchemas: Vector[String],
    falsePositiveRisks: Vector[String],
    handoffGroup: String,
    selectedClaimId: Option[String],
    selectedEvidenceIds: Vector[String],
    breaks: Vector[String],
    auditQuestion: String
)

final case class LowerDiagnosticHandoffGroup(
    key: String,
    count: Int,
    layerBreak: String,
    tacticalSchema: String,
    falsePositiveRisk: String,
    exampleRowIds: Vector[String],
    nextPhaseQuestion: String
)

final case class LowerDiagnosticReport(
    traces: Vector[LowerDiagnosticTraceRow],
    summary: LowerDiagnosticReportSummary,
    auditSample: Vector[LowerDiagnosticAuditSample],
    handoffGroups: Vector[LowerDiagnosticHandoffGroup]
)

object LowerDiagnosticReport:

  private val NoSchema = "none"
  private val NoRisk = "none"

  def fromRows(rows: Vector[LowerDiagnosticLargeCorpus.Row]): LowerDiagnosticReport =
    val traces = rows.map: row =>
      val trace = LowerLayerDiagnostic.trace(row.input)
      val schemas = tacticalSchemas(row, trace)
      val layerBreak = primaryLayerBreak(row, trace)
      val risks = falsePositiveRisks(row, trace, schemas)
      LowerDiagnosticTraceRow(
        row = row,
        trace = trace,
        layerBreak = layerBreak,
        tacticalSchemas = schemas,
        falsePositiveRisks = risks,
        handoffGroup = handoffGroup(layerBreak, schemas, risks, row)
      )
    LowerDiagnosticReport(
      traces = traces,
      summary = summary(traces),
      auditSample = auditSample(traces),
      handoffGroups = handoffGroups(traces)
    )

  private def summary(traces: Vector[LowerDiagnosticTraceRow]): LowerDiagnosticReportSummary =
    LowerDiagnosticReportSummary(
      total = traces.size,
      countsBySourceFile = countBy(traces.map(_.row.sourceFile)),
      countsBySourceKind = countBy(traces.map(_.row.sourceKind)),
      countsByLayerBreak = countBy(traces.map(_.layerBreak)),
      countsByTacticalSchema = countBy(traces.flatMap(_.tacticalSchemas)),
      countsByFalsePositiveRisk = countBy(traces.flatMap(_.falsePositiveRisks)),
      countsByHandoffGroup = countBy(traces.map(_.handoffGroup))
    )

  private def primaryLayerBreak(row: LowerDiagnosticLargeCorpus.Row, trace: LowerLayerDiagnostic.Trace): String =
    if row.metadata.contains("playedUci") && row.input.beforeFen.isEmpty then "transition:missing_before_for_played_move"
    else if trace.input.status != "valid" then s"input:${trace.input.status}"
    else if trace.transition.status != "none" && trace.transition.status != "valid" then s"transition:${trace.transition.status}"
    else if trace.breaks.contains("extraction:no_tactical_roots") then "extraction:no_tactical_roots"
    else if trace.breaks.contains("claim:no_claims") then "claim:no_claims"
    else if trace.breaks.contains("admission:standing_tactical_only") then "admission:standing_tactical_only"
    else if trace.breaks.contains("selection:no_move_local_tactical_lead") then "selection:no_move_local_tactical_lead"
    else if trace.breaks.contains("selection:no_lead") then "selection:no_lead"
    else if trace.preRendererVerdict == "selected_move_local_tactical_claim" then "selected:move_local_tactical"
    else s"selected:${trace.preRendererVerdict}"

  private def tacticalSchemas(
      row: LowerDiagnosticLargeCorpus.Row,
      trace: LowerLayerDiagnostic.Trace
  ): Vector[String] =
    val rootSchemas = trace.extraction.rootFacts.map(_.id)
    val evidenceSchemas =
      trace.claims.produced.flatMap(_.evidenceIds).filter(isTacticalSchema)
    val sourceSchema = row.sourceSchema.filter(isTacticalSchema)
    (rootSchemas ++ evidenceSchemas ++ sourceSchema.toVector).distinct.sorted

  private def isTacticalSchema(value: String): Boolean =
    val tacticalIds = Set(
      "loose_piece",
      "pinned_piece",
      "overloaded_piece",
      "trapped_piece",
      "xray_target",
      "royal_fork",
      "fork",
      "pin",
      "skewer",
      "overload",
      "duty_bound_defender",
      "loose_piece_target_state"
    )
    tacticalIds.contains(value) ||
      value.contains("fork") ||
      value.contains("pin") ||
      value.contains("skewer") ||
      value.contains("overload") ||
      value.contains("xray") ||
      value.contains("trapped") ||
      value.contains("loose") ||
      value.contains("liability")

  private def falsePositiveRisks(trace: LowerLayerDiagnostic.Trace, schemas: Vector[String]): Vector[String] =
    Vector(
      Option.when(trace.preRendererVerdict == "selected_position_local_tactical_claim")(
        "position_local_tactical_selected_without_move_causality"
      ),
      Option.when(trace.breaks.contains("selection:no_move_local_tactical_lead") && trace.selection.lead.nonEmpty)(
        "non_tactical_lead_masks_tactical_gap"
      ),
      Option.when(trace.breaks.contains("selection:only_generic_transition"))("generic_transition_as_public_reason"),
      Option.when(trace.transition.status == "valid" && trace.tacticalVerdict == "standing_tactical_only")(
        "standing_tactic_in_transition_requires_manual_audit"
      ),
      Option.when(trace.tacticalVerdict == "no_tactical_root" && schemas.nonEmpty)("source_tactical_schema_without_root_fact")
    ).flatten.distinct

  private def falsePositiveRisks(
      row: LowerDiagnosticLargeCorpus.Row,
      trace: LowerLayerDiagnostic.Trace,
      schemas: Vector[String]
  ): Vector[String] =
    (falsePositiveRisks(trace, schemas) ++
      Option.when(row.metadata.contains("playedUci") && row.input.beforeFen.isEmpty)(
        "transition_identity_incomplete_for_move_claim"
      )).distinct

  private def handoffGroup(
      layerBreak: String,
      schemas: Vector[String],
      risks: Vector[String],
      row: LowerDiagnosticLargeCorpus.Row
  ): String =
    val schema = schemas.headOption.orElse(row.sourceSchema).getOrElse(NoSchema)
    val risk = risks.headOption.getOrElse(NoRisk)
    s"$layerBreak|$schema|$risk|${row.sourceKind}"

  private def auditSample(traces: Vector[LowerDiagnosticTraceRow]): Vector[LowerDiagnosticAuditSample] =
    traces
      .filter(_.falsePositiveRisks.nonEmpty)
      .groupBy(_.handoffGroup)
      .toVector
      .sortBy(_._1)
      .flatMap: (_, groupRows) =>
        groupRows.sortBy(_.row.id).distinctBy(_.row.id).take(3).map(auditSampleFor)

  private def auditSampleFor(row: LowerDiagnosticTraceRow): LowerDiagnosticAuditSample =
    LowerDiagnosticAuditSample(
      rowId = row.row.id,
      sourceFile = row.row.sourceFile,
      sourceKind = row.row.sourceKind,
      sourceSchema = row.row.sourceSchema,
      caseType = row.row.caseType,
      currentFen = row.row.input.currentFen,
      beforeFen = row.row.input.beforeFen,
      playedMove = row.row.input.playedMove,
      nodeId = row.row.input.nodeId,
      ply = row.row.input.ply,
      layerBreak = row.layerBreak,
      tacticalSchemas = row.tacticalSchemas,
      falsePositiveRisks = row.falsePositiveRisks,
      handoffGroup = row.handoffGroup,
      selectedClaimId = row.trace.selection.lead.map(_.id),
      selectedEvidenceIds = row.trace.selection.lead.toVector.flatMap(_.evidenceIds),
      breaks = row.trace.breaks,
      auditQuestion = nextQuestion(row)
    )

  private def handoffGroups(traces: Vector[LowerDiagnosticTraceRow]): Vector[LowerDiagnosticHandoffGroup] =
    traces
      .groupBy(_.handoffGroup)
      .toVector
      .sortBy((_, rows) => -rows.size)
      .map: (key, rows) =>
        val first = rows.minBy(_.row.id)
        LowerDiagnosticHandoffGroup(
          key = key,
          count = rows.size,
          layerBreak = first.layerBreak,
          tacticalSchema = first.tacticalSchemas.headOption.orElse(first.row.sourceSchema).getOrElse(NoSchema),
          falsePositiveRisk = first.falsePositiveRisks.headOption.getOrElse(NoRisk),
          exampleRowIds = rows.map(_.row.id).distinct.sorted.take(5),
          nextPhaseQuestion = nextQuestion(first)
        )

  private def nextQuestion(row: LowerDiagnosticTraceRow): String =
    if row.layerBreak.startsWith("input:") then "Fix or exclude malformed exact-board input before admission analysis."
    else if row.layerBreak.startsWith("transition:") then "Check whether before/current/move identity is stale or whether replay support is missing."
    else if row.layerBreak == "extraction:no_tactical_roots" then "Determine whether a missing root/witness schema is required or whether the source row is non-tactical."
    else if row.layerBreak == "admission:standing_tactical_only" then "Decide whether a narrow move-local transition slice can prove causality without using standing tactics as public move truth."
    else if row.layerBreak == "selection:no_move_local_tactical_lead" then "Inspect whether selection is correctly preferring non-tactical exact truth or hiding an admitted tactical claim."
    else if row.layerBreak == "selected:move_local_tactical" then "Use as a positive control when expanding adjacent tactical slices."
    else "Inspect the selected claim and renderer path before assigning this row to product-note coverage."

  private def countBy(values: Vector[String]): Map[String, Int] =
    values.groupMapReduce(identity)(_ => 1)(_ + _).toVector.sortBy(_._1).toMap.withDefaultValue(0)

object LowerDiagnosticReportJson:

  def summaryJson(summary: LowerDiagnosticReportSummary): JsObject =
    Json.obj(
      "total" -> summary.total,
      "countsBySourceFile" -> summary.countsBySourceFile,
      "countsBySourceKind" -> summary.countsBySourceKind,
      "countsByLayerBreak" -> summary.countsByLayerBreak,
      "countsByTacticalSchema" -> summary.countsByTacticalSchema,
      "countsByFalsePositiveRisk" -> summary.countsByFalsePositiveRisk,
      "countsByHandoffGroup" -> summary.countsByHandoffGroup
    )

  def traceJson(row: LowerDiagnosticTraceRow): JsObject =
    Json.obj(
      "rowId" -> row.row.id,
      "sourceFile" -> row.row.sourceFile,
      "sourceKind" -> row.row.sourceKind,
      "sourceSchema" -> row.row.sourceSchema,
      "caseType" -> row.row.caseType,
      "expectation" -> row.row.expectation,
      "currentFen" -> row.row.input.currentFen,
      "beforeFen" -> row.row.input.beforeFen,
      "playedMove" -> row.row.input.playedMove,
      "nodeId" -> row.row.input.nodeId,
      "ply" -> row.row.input.ply,
      "inputStatus" -> row.trace.input.status,
      "transitionStatus" -> row.trace.transition.status,
      "transitionReason" -> row.trace.transition.reason,
      "rootFacts" -> row.trace.extraction.rootFacts.map(rootFactJson),
      "producedClaims" -> row.trace.claims.produced.map(claimJson),
      "selectedLead" -> row.trace.selection.lead.map(claimJson),
      "tacticalVerdict" -> row.trace.tacticalVerdict,
      "preRendererVerdict" -> row.trace.preRendererVerdict,
      "breaks" -> row.trace.breaks,
      "layerBreak" -> row.layerBreak,
      "tacticalSchemas" -> row.tacticalSchemas,
      "falsePositiveRisks" -> row.falsePositiveRisks,
      "handoffGroup" -> row.handoffGroup
    )

  def auditSampleJson(sample: LowerDiagnosticAuditSample): JsObject =
    Json.obj(
      "rowId" -> sample.rowId,
      "sourceFile" -> sample.sourceFile,
      "sourceKind" -> sample.sourceKind,
      "sourceSchema" -> sample.sourceSchema,
      "caseType" -> sample.caseType,
      "currentFen" -> sample.currentFen,
      "beforeFen" -> sample.beforeFen,
      "playedMove" -> sample.playedMove,
      "nodeId" -> sample.nodeId,
      "ply" -> sample.ply,
      "layerBreak" -> sample.layerBreak,
      "tacticalSchemas" -> sample.tacticalSchemas,
      "falsePositiveRisks" -> sample.falsePositiveRisks,
      "handoffGroup" -> sample.handoffGroup,
      "selectedClaimId" -> sample.selectedClaimId,
      "selectedEvidenceIds" -> sample.selectedEvidenceIds,
      "breaks" -> sample.breaks,
      "auditQuestion" -> sample.auditQuestion
    )

  def handoffGroupJson(group: LowerDiagnosticHandoffGroup): JsObject =
    Json.obj(
      "key" -> group.key,
      "count" -> group.count,
      "layerBreak" -> group.layerBreak,
      "tacticalSchema" -> group.tacticalSchema,
      "falsePositiveRisk" -> group.falsePositiveRisk,
      "exampleRowIds" -> group.exampleRowIds,
      "nextPhaseQuestion" -> group.nextPhaseQuestion
    )

  private def rootFactJson(root: LowerLayerDiagnostic.RootFact): JsObject =
    Json.obj(
      "id" -> root.id,
      "owner" -> root.owner,
      "anchor" -> root.anchor,
      "atom" -> root.atom
    )

  private def claimJson(claim: LowerLayerDiagnostic.ClaimSummary): JsObject =
    Json.obj(
      "id" -> claim.id,
      "layer" -> claim.layer,
      "status" -> claim.status,
      "owner" -> claim.owner,
      "beneficiary" -> claim.beneficiary,
      "defender" -> claim.defender,
      "anchor" -> claim.anchor,
      "route" -> claim.route,
      "scope" -> claim.scope,
      "evidenceIds" -> claim.evidenceIds,
      "lowerCarrierIds" -> claim.lowerCarrierIds
    )

object LowerDiagnosticReportRunner:

  private val defaultOutputDir = Paths.get("tmp/commentary-diagnostic/lower-layer")

  def main(args: Array[String]): Unit =
    val options = RunnerOptions.parse(args.toVector)
    val rows =
      options.inputPath match
        case Some(path) => LowerDiagnosticLargeCorpus.loadExternalRows(path, options.limit)
        case None       => LowerDiagnosticLargeCorpus.loadTrackedRows()
    write(LowerDiagnosticReport.fromRows(rows), options.outputDir)

  def write(report: LowerDiagnosticReport, outputDir: Path): Unit =
    Files.createDirectories(outputDir)
    Files.writeString(
      outputDir.resolve("summary.json"),
      Json.prettyPrint(LowerDiagnosticReportJson.summaryJson(report.summary)) + System.lineSeparator(),
      StandardCharsets.UTF_8
    )
    writeJsonl(outputDir.resolve("traces.jsonl"), report.traces.map(LowerDiagnosticReportJson.traceJson))
    writeJsonl(outputDir.resolve("false-positive-audit-sample.jsonl"), report.auditSample.map(LowerDiagnosticReportJson.auditSampleJson))
    writeJsonl(outputDir.resolve("handoff-groups.jsonl"), report.handoffGroups.map(LowerDiagnosticReportJson.handoffGroupJson))

  private def writeJsonl(path: Path, rows: Vector[JsObject]): Unit =
    Files.writeString(
      path,
      rows.map(Json.stringify).mkString("", System.lineSeparator(), System.lineSeparator()),
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
