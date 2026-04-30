package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import play.api.libs.json.*

final case class LowerDiagnosticBottleneckSummary(
    total: Int,
    inputBlockedRows: Int,
    replayableRows: Int,
    currentOnlyRows: Int,
    countsByOwnerLayer: Map[String, Int],
    countsByResolutionState: Map[String, Int],
    countsByDeferredTo: Map[String, Int]
)

final case class LowerDiagnosticBottleneck(
    key: String,
    count: Int,
    ownerLayer: String,
    resolutionState: String,
    deferredTo: Option[String],
    layerBreak: String,
    tacticalSchema: String,
    sourceKind: String,
    priority: Int,
    replayable: Boolean,
    countsByCaseType: Map[String, Int],
    countsByExpectation: Map[String, Int],
    exampleRowIds: Vector[String],
    rationale: String,
    nextAction: String
)

final case class LowerDiagnosticFourthPhaseSlice(
    key: String,
    priority: Int,
    ownerLayer: String,
    resolutionState: String,
    deferredTo: Option[String],
    candidateCount: Int,
    replayableCount: Int,
    tacticalSchema: String,
    sourceKind: String,
    countsByCaseType: Map[String, Int],
    countsByExpectation: Map[String, Int],
    minimumEvidence: Vector[String],
    completionCondition: String,
    exampleRowIds: Vector[String]
)

final case class LowerDiagnosticBottleneckReport(
    summary: LowerDiagnosticBottleneckSummary,
    bottlenecks: Vector[LowerDiagnosticBottleneck],
    fourthPhaseSlices: Vector[LowerDiagnosticFourthPhaseSlice]
)

object LowerDiagnosticBottleneckReport:

  def fromDiagnostic(report: LowerDiagnosticReport): LowerDiagnosticBottleneckReport =
    val bottlenecks = report.traces
      .groupBy(bottleneckKey)
      .toVector
      .map: (_, rows) =>
        bottleneckFor(rows)
      .sortBy(bottleneck => (-bottleneck.count, bottleneck.priority, bottleneck.key))
    val slices = bottlenecks.map(sliceFor)
    LowerDiagnosticBottleneckReport(
      summary = summary(report.traces, bottlenecks),
      bottlenecks = bottlenecks,
      fourthPhaseSlices = slices
    )

  private def summary(
      traces: Vector[LowerDiagnosticTraceRow],
      bottlenecks: Vector[LowerDiagnosticBottleneck]
  ): LowerDiagnosticBottleneckSummary =
    LowerDiagnosticBottleneckSummary(
      total = traces.size,
      inputBlockedRows = traces.count(isInputBlocked),
      replayableRows = traces.count(isReplayable),
      currentOnlyRows = traces.count(row => !isInputBlocked(row) && !isReplayable(row)),
      countsByOwnerLayer = countByWeighted(bottlenecks.map(b => b.ownerLayer -> b.count)),
      countsByResolutionState = countByWeighted(bottlenecks.map(b => b.resolutionState -> b.count)),
      countsByDeferredTo = countByWeighted(bottlenecks.flatMap(b => b.deferredTo.map(_ -> b.count)))
    )

  private def bottleneckKey(row: LowerDiagnosticTraceRow): String =
    val owner = ownerLayer(row)
    val schema = tacticalSchema(row)
    val state = resolutionState(row)
    s"$owner|$state|${row.layerBreak}|$schema|${row.row.sourceKind}"

  private def bottleneckFor(rows: Vector[LowerDiagnosticTraceRow]): LowerDiagnosticBottleneck =
    val first = rows.minBy(_.row.id)
    val owner = ownerLayer(first)
    val state = resolutionState(first)
    val deferred = deferredTo(first)
    val replayable = rows.exists(isReplayable)
    LowerDiagnosticBottleneck(
      key = bottleneckKey(first),
      count = rows.size,
      ownerLayer = owner,
      resolutionState = state,
      deferredTo = deferred,
      layerBreak = first.layerBreak,
      tacticalSchema = tacticalSchema(first),
      sourceKind = first.row.sourceKind,
      priority = priority(first),
      replayable = replayable,
      countsByCaseType = countLabels(rows.flatMap(_.row.caseType)),
      countsByExpectation = countLabels(rows.flatMap(_.row.expectation)),
      exampleRowIds = rows.map(_.row.id).distinct.sorted.take(8),
      rationale = rationale(first),
      nextAction = nextAction(first)
    )

  private def sliceFor(bottleneck: LowerDiagnosticBottleneck): LowerDiagnosticFourthPhaseSlice =
    LowerDiagnosticFourthPhaseSlice(
      key = bottleneck.key,
      priority = bottleneck.priority,
      ownerLayer = bottleneck.ownerLayer,
      resolutionState = bottleneck.resolutionState,
      deferredTo = bottleneck.deferredTo,
      candidateCount = bottleneck.count,
      replayableCount = if bottleneck.replayable then bottleneck.count else 0,
      tacticalSchema = bottleneck.tacticalSchema,
      sourceKind = bottleneck.sourceKind,
      countsByCaseType = bottleneck.countsByCaseType,
      countsByExpectation = bottleneck.countsByExpectation,
      minimumEvidence = minimumEvidence(bottleneck),
      completionCondition = completionCondition(bottleneck),
      exampleRowIds = bottleneck.exampleRowIds
    )

  private def isInputBlocked(row: LowerDiagnosticTraceRow): Boolean =
    row.layerBreak.startsWith("input:") ||
      row.layerBreak.startsWith("transition:missing_before") ||
      row.layerBreak == "transition:invalid" ||
      row.layerBreak == "transition:invalid_clock" ||
      row.layerBreak == "transition:after_mismatch"

  private def isReplayable(row: LowerDiagnosticTraceRow): Boolean =
    row.trace.transition.status == "valid" &&
      row.row.input.beforeFen.nonEmpty &&
      row.row.input.playedMove.nonEmpty

  private def ownerLayer(row: LowerDiagnosticTraceRow): String =
    if isInputBlocked(row) then "input_reconstruction"
    else if row.layerBreak == "extraction:no_tactical_roots" && hasSourceTacticalSchema(row) then "root_witness_schema"
    else if row.layerBreak == "extraction:no_tactical_roots" then "non_tactical_or_untracked_schema"
    else if row.layerBreak == "admission:standing_tactical_only" && !isReplayable(row) then "standing_board_fact_only"
    else if row.layerBreak == "admission:standing_tactical_only" then "exact_transition_admission"
    else if row.layerBreak.startsWith("selection:") then "selection_line"
    else if row.layerBreak == "selected:move_local_tactical" then "positive_control"
    else "diagnostic_review"

  private def resolutionState(row: LowerDiagnosticTraceRow): String =
    ownerLayer(row) match
      case "input_reconstruction"            => "input_blocked"
      case "root_witness_schema"             => "schema_needed"
      case "non_tactical_or_untracked_schema" => "outside_tactical_scope"
      case "standing_board_fact_only"        => "current_board_fact_only"
      case "exact_transition_admission"      => "slice_needed"
      case "selection_line"                  => "selection_blocked"
      case "positive_control"                => "fixed_positive_control"
      case _                                 => "needs_manual_review"

  private def deferredTo(row: LowerDiagnosticTraceRow): Option[String] =
    ownerLayer(row) match
      case "input_reconstruction"             => Some("input_reconstruction")
      case "root_witness_schema"              => Some("root_witness_schema")
      case "non_tactical_or_untracked_schema" => Some("domain_specific_lower_layer")
      case "standing_board_fact_only"         => Some("transition_context_required")
      case "exact_transition_admission"       => Some("exact_transition_slice")
      case "selection_line"                   => Some("selection_line")
      case "positive_control"                 => None
      case _                                  => Some("manual_audit")

  private def priority(row: LowerDiagnosticTraceRow): Int =
    ownerLayer(row) match
      case "input_reconstruction"       => 1
      case "exact_transition_admission" => 2
      case "root_witness_schema"        => 3
      case "selection_line"             => 4
      case "standing_board_fact_only"   => 5
      case "positive_control"           => 9
      case _                            => 6

  private def tacticalSchema(row: LowerDiagnosticTraceRow): String =
    row.tacticalSchemas.headOption.orElse(row.row.sourceSchema).getOrElse("none")

  private def hasSourceTacticalSchema(row: LowerDiagnosticTraceRow): Boolean =
    row.row.sourceSchema.exists(schema =>
      Set(
        "fork",
        "pin",
        "skewer",
        "overload",
        "duty_bound_defender",
        "loose_piece_target_state",
        "short_run_slider_gate_restriction"
      ).contains(schema) ||
        schema.contains("fork") ||
        schema.contains("pin") ||
        schema.contains("skewer") ||
        schema.contains("overload") ||
        schema.contains("liability")
    )

  private def rationale(row: LowerDiagnosticTraceRow): String =
    ownerLayer(row) match
      case "input_reconstruction" =>
        "The row cannot prove move-local causality until exact before/current/move replay identity is available."
      case "root_witness_schema" =>
        "The source schema is tactical, but the diagnostic found no current tactical root fact that can feed a public claim."
      case "non_tactical_or_untracked_schema" =>
        "The row has no tracked tactical root signal; it should not be counted as a tactical admission gap."
      case "standing_board_fact_only" =>
        "A tactical board fact exists, but the row has no replayable transition identity and cannot prove move-local causality."
      case "exact_transition_admission" =>
        "A replayable tactical board fact exists, but admission has not certified it as a validated move-local slice."
      case "selection_line" =>
        "Lower claims exist, but selection or line handoff prevents the expected public tactical lead."
      case "positive_control" =>
        "A narrow move-local tactical claim is already selected and should be used as a regression control."
      case _ =>
        "The row requires manual diagnostic review before assigning a lower-layer owner."

  private def nextAction(row: LowerDiagnosticTraceRow): String =
    ownerLayer(row) match
      case "input_reconstruction" =>
        "Recover beforeFen through PGN/node replay, then rerun lower diagnostics before touching admission."
      case "root_witness_schema" =>
        "Decide whether the schema needs a root, a U witness bridge, or an explicit non-public support-only role."
      case "non_tactical_or_untracked_schema" =>
        "Route to its domain lower layer or keep outside the tactical 4th-phase backlog."
      case "standing_board_fact_only" =>
        "Keep as a current-board fact, or rerun with beforeFen and playedMove before opening transition admission work."
      case "exact_transition_admission" =>
        "Open one narrow exact-board transition slice with positive and false-positive anti-case tests."
      case "selection_line" =>
        "Inspect selector suppression and candidate-line decisions for selected claim binding."
      case "positive_control" =>
        "Keep as fixed control while adjacent slices are expanded."
      case _ =>
        "Read the trace manually and assign an owner before implementation."

  private def minimumEvidence(bottleneck: LowerDiagnosticBottleneck): Vector[String] =
    bottleneck.ownerLayer match
      case "input_reconstruction" =>
        Vector("beforeFen", "currentFen", "playedMove", "nodeId", "ply", "legal replay matching current full FEN")
      case "root_witness_schema" =>
        Vector("exact FEN positive", "exact FEN near-miss", "exact FEN nasty-negative", "root or U-witness owner contract")
      case "standing_board_fact_only" =>
        Vector("currentFen", "selected position-local claim", "explicit no move-causality claim", "beforeFen and playedMove if promoted")
      case "exact_transition_admission" =>
        Vector("beforeFen", "currentFen", "playedMove", "move-local positive", "pre-existing/standing anti-case")
      case "selection_line" =>
        Vector("admitted claim id", "selected/suppressed reason", "candidate-line defender-resource decision")
      case "positive_control" =>
        Vector("existing exact transition positive", "existing anti-case coverage")
      case _ =>
        Vector("exact board row", "owner-layer decision", "false-positive rationale")

  private def completionCondition(bottleneck: LowerDiagnosticBottleneck): String =
    bottleneck.resolutionState match
      case "input_blocked" =>
        "Rows are replayable or remain explicitly input-blocked; no lower admission change is made from current-position smell alone."
      case "schema_needed" =>
        "A root/witness contract is added with exact positives and negatives, or the schema is documented as support-only/deferred."
      case "current_board_fact_only" =>
        "Rows remain current-board only, or exact beforeFen/playedMove replay is added before any move-local admission work."
      case "slice_needed" =>
        "A narrow move-local slice admits only exact transition positives and rejects standing/pre-existing anti-cases."
      case "selection_blocked" =>
        "Selector/line handoff exposes why an admitted lower claim does or does not become public."
      case "fixed_positive_control" =>
        "Positive control remains green after adjacent slice work."
      case _ =>
        "The row group is assigned to an owner layer with explicit next evidence requirements."

  private def countByWeighted(values: Vector[(String, Int)]): Map[String, Int] =
    values.groupMapReduce(_._1)(_._2)(_ + _).toVector.sortBy(_._1).toMap.withDefaultValue(0)

  private def countLabels(values: Vector[String]): Map[String, Int] =
    values.groupMapReduce(identity)(_ => 1)(_ + _).toVector.sortBy(_._1).toMap.withDefaultValue(0)

object LowerDiagnosticBottleneckJson:

  def summaryJson(summary: LowerDiagnosticBottleneckSummary): JsObject =
    Json.obj(
      "total" -> summary.total,
      "inputBlockedRows" -> summary.inputBlockedRows,
      "replayableRows" -> summary.replayableRows,
      "currentOnlyRows" -> summary.currentOnlyRows,
      "countsByOwnerLayer" -> summary.countsByOwnerLayer,
      "countsByResolutionState" -> summary.countsByResolutionState,
      "countsByDeferredTo" -> summary.countsByDeferredTo
    )

  def bottleneckJson(bottleneck: LowerDiagnosticBottleneck): JsObject =
    Json.obj(
      "key" -> bottleneck.key,
      "count" -> bottleneck.count,
      "ownerLayer" -> bottleneck.ownerLayer,
      "resolutionState" -> bottleneck.resolutionState,
      "deferredTo" -> bottleneck.deferredTo,
      "layerBreak" -> bottleneck.layerBreak,
      "tacticalSchema" -> bottleneck.tacticalSchema,
      "sourceKind" -> bottleneck.sourceKind,
      "priority" -> bottleneck.priority,
      "replayable" -> bottleneck.replayable,
      "countsByCaseType" -> bottleneck.countsByCaseType,
      "countsByExpectation" -> bottleneck.countsByExpectation,
      "exampleRowIds" -> bottleneck.exampleRowIds,
      "rationale" -> bottleneck.rationale,
      "nextAction" -> bottleneck.nextAction
    )

  def sliceJson(slice: LowerDiagnosticFourthPhaseSlice): JsObject =
    Json.obj(
      "key" -> slice.key,
      "priority" -> slice.priority,
      "ownerLayer" -> slice.ownerLayer,
      "resolutionState" -> slice.resolutionState,
      "deferredTo" -> slice.deferredTo,
      "candidateCount" -> slice.candidateCount,
      "replayableCount" -> slice.replayableCount,
      "tacticalSchema" -> slice.tacticalSchema,
      "sourceKind" -> slice.sourceKind,
      "countsByCaseType" -> slice.countsByCaseType,
      "countsByExpectation" -> slice.countsByExpectation,
      "minimumEvidence" -> slice.minimumEvidence,
      "completionCondition" -> slice.completionCondition,
      "exampleRowIds" -> slice.exampleRowIds
    )

object LowerDiagnosticBottleneckRunner:

  private val defaultOutputDir = Paths.get("tmp/commentary-diagnostic/lower-layer/tracked/phase3")

  def main(args: Array[String]): Unit =
    val options = RunnerOptions.parse(args.toVector)
    val rows =
      options.inputPath match
        case Some(path) => LowerDiagnosticLargeCorpus.loadExternalRows(path, options.limit)
        case None       => LowerDiagnosticLargeCorpus.loadTrackedRows()
    val diagnostic = LowerDiagnosticReport.fromRows(rows)
    write(LowerDiagnosticBottleneckReport.fromDiagnostic(diagnostic), options.outputDir)

  def write(report: LowerDiagnosticBottleneckReport, outputDir: Path): Unit =
    Files.createDirectories(outputDir)
    Files.writeString(
      outputDir.resolve("phase3-summary.json"),
      Json.prettyPrint(LowerDiagnosticBottleneckJson.summaryJson(report.summary)) + System.lineSeparator(),
      StandardCharsets.UTF_8
    )
    writeJsonl(outputDir.resolve("bottlenecks.jsonl"), report.bottlenecks.map(LowerDiagnosticBottleneckJson.bottleneckJson))
    writeJsonl(outputDir.resolve("fourth-phase-slices.jsonl"), report.fourthPhaseSlices.map(LowerDiagnosticBottleneckJson.sliceJson))
    writeJsonl(
      outputDir.resolve("transition-slice-handoff.jsonl"),
      report.fourthPhaseSlices
        .filter(slice => slice.ownerLayer == "exact_transition_admission" || slice.ownerLayer == "positive_control")
        .map(LowerDiagnosticBottleneckJson.sliceJson)
    )

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
