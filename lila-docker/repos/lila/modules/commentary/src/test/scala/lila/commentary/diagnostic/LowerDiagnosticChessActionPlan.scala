package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import play.api.libs.json.*

final case class LowerDiagnosticChessActionSummary(
    totalRows: Int,
    inputBlockedRows: Int,
    replayableRows: Int,
    standingRows: Int,
    immediateCaptureStaticFacts: Int,
    exactTransitionSliceRows: Int,
    rootWitnessSchemaRows: Int,
    countsByActionType: Map[String, Int]
)

final case class LowerDiagnosticChessActionItem(
    priority: Int,
    actionId: String,
    actionType: String,
    target: String,
    candidateCount: Int,
    publicAdmission: String,
    rationale: String,
    requiredEvidence: Vector[String],
    falsePositiveControl: String,
    exampleRowIds: Vector[String],
    outputArtifact: String
)

final case class LowerDiagnosticChessActionPlan(
    summary: LowerDiagnosticChessActionSummary,
    actions: Vector[LowerDiagnosticChessActionItem]
)

object LowerDiagnosticChessActionPlan:

  def fromRows(rows: Vector[LowerDiagnosticLargeCorpus.Row]): LowerDiagnosticChessActionPlan =
    val diagnostic = LowerDiagnosticReport.fromRows(rows)
    val reconstruction = LowerDiagnosticReconstructionReport.fromDiagnostic(diagnostic)
    val bottleneck = LowerDiagnosticBottleneckReport.fromDiagnostic(diagnostic)
    val standing = LowerDiagnosticStandingAuditReport.fromDiagnostic(diagnostic)
    val actions = buildActions(reconstruction, bottleneck, standing)
    LowerDiagnosticChessActionPlan(
      summary = LowerDiagnosticChessActionSummary(
        totalRows = rows.size,
        inputBlockedRows = reconstruction.rows.count(row =>
          row.reconstructionState == "unreconstructable" && row.reason != "no_transition_claim_requested"
        ),
        replayableRows = reconstruction.summary.replayableRows,
        standingRows = standing.summary.total,
        immediateCaptureStaticFacts = standing.summary.countsByTag("immediate_capture_static_fact"),
        exactTransitionSliceRows = bottleneck.summary.countsByOwnerLayer("exact_transition_admission"),
        rootWitnessSchemaRows = bottleneck.summary.countsByOwnerLayer("root_witness_schema"),
        countsByActionType = countByWeighted(actions.map(action => action.actionType -> action.candidateCount))
      ),
      actions = actions.sortBy(action => (action.priority, -action.candidateCount, action.actionId))
    )

  private def buildActions(
      reconstruction: LowerDiagnosticReconstructionReport,
      bottleneck: LowerDiagnosticBottleneckReport,
      standing: LowerDiagnosticStandingAuditReport
  ): Vector[LowerDiagnosticChessActionItem] =
    Vector(
      inputReconstructionAction(reconstruction),
      exactTransitionSliceAction(bottleneck),
      rootWitnessSchemaAction(bottleneck),
      immediateCaptureAction(standing),
      supportOnlyStandingAction(standing),
      positiveControlAction(bottleneck)
    ).flatten

  private def inputReconstructionAction(
      report: LowerDiagnosticReconstructionReport
  ): Option[LowerDiagnosticChessActionItem] =
    val rows = report.rows.filter(row =>
      row.reconstructionState == "unreconstructable" &&
        row.reason != "no_transition_claim_requested"
    )
    Option.when(rows.nonEmpty):
      LowerDiagnosticChessActionItem(
        priority = 1,
        actionId = "recover-transition-identity",
        actionType = "input_reconstruction",
        target = "beforeFen/currentFen/playedMove replay identity",
        candidateCount = rows.size,
        publicAdmission = "blocked",
        rationale = "Move-causal tactical explanation is impossible until the row replays from beforeFen through playedMove to currentFen.",
        requiredEvidence = Vector("beforeFen", "currentFen", "playedMove", "nodeId", "ply or PGN move index", "legal replay matching current board"),
        falsePositiveControl = "Do not infer a missed tactic from current-board tactical roots or engine/PV rows without replay identity.",
        exampleRowIds = rows.map(_.rowId).distinct.sorted.take(8),
        outputArtifact = "reconstruction/reconstruction-traces.jsonl"
      )

  private def exactTransitionSliceAction(
      report: LowerDiagnosticBottleneckReport
  ): Option[LowerDiagnosticChessActionItem] =
    val slices = report.fourthPhaseSlices.filter(_.ownerLayer == "exact_transition_admission")
    val count = slices.map(_.candidateCount).sum
    Option.when(count > 0):
      LowerDiagnosticChessActionItem(
        priority = 2,
        actionId = "open-narrow-exact-transition-tactical-slices",
        actionType = "exact_transition_slice",
        target = slices.map(_.tacticalSchema).distinct.sorted.mkString(","),
        candidateCount = count,
        publicAdmission = "candidate_after_slice_tests",
        rationale = "These rows are replayable and expose standing tactical facts after a move, so they are the only immediate pool for move-local tactical admission work.",
        requiredEvidence = Vector("beforeFen", "currentFen", "playedMove", "move-local positive", "pre-existing anti-case", "wrong-owner anti-case", "standing-only anti-case"),
        falsePositiveControl = "Open one slice at a time and require exact transition positives plus near-miss/nasty-negative fixtures.",
        exampleRowIds = slices.flatMap(_.exampleRowIds).distinct.sorted.take(8),
        outputArtifact = "phase3/transition-slice-handoff.jsonl"
      )

  private def rootWitnessSchemaAction(
      report: LowerDiagnosticBottleneckReport
  ): Option[LowerDiagnosticChessActionItem] =
    val slices = report.fourthPhaseSlices.filter(_.ownerLayer == "root_witness_schema")
    val count = slices.map(_.candidateCount).sum
    Option.when(count > 0):
      LowerDiagnosticChessActionItem(
        priority = 3,
        actionId = "settle-root-witness-tactical-schema-contracts",
        actionType = "root_witness_contract",
        target = slices.map(_.tacticalSchema).distinct.sorted.mkString(","),
        candidateCount = count,
        publicAdmission = "no_public_claim_yet",
        rationale = "The source schema is tactical, but current extraction has no corresponding tracked tactical root/witness fact.",
        requiredEvidence = Vector("exact FEN positive", "near-miss", "nasty negative", "owner/source contract", "support-only or admission decision"),
        falsePositiveControl = "Do not create public claims while deciding whether the missing concept belongs at root, U-witness, or support-only.",
        exampleRowIds = slices.flatMap(_.exampleRowIds).distinct.sorted.take(8),
        outputArtifact = "phase3/fourth-phase-slices.jsonl"
      )

  private def immediateCaptureAction(
      report: LowerDiagnosticStandingAuditReport
  ): Option[LowerDiagnosticChessActionItem] =
    val rows = report.rows.filter(_.tags.contains("immediate_capture_static_fact"))
    Option.when(rows.nonEmpty):
      LowerDiagnosticChessActionItem(
        priority = 4,
        actionId = "harden-current-board-immediate-capture-contract",
        actionType = "current_board_claim_contract",
        target = "immediate_capture",
        candidateCount = rows.size,
        publicAdmission = "current_board_only_not_move_causal",
        rationale = "The side to move has a legal capture on a locally loose piece, but this still does not prove the last move caused or missed the tactic.",
        requiredEvidence = Vector("currentFen", "side-to-move legal capture", "captured target", "same-square loose_piece carrier", "material/mate context suppression check"),
        falsePositiveControl = "Keep loose_piece itself support-only; suppress if mate/result/material context makes the local capture misleading.",
        exampleRowIds = rows.map(_.rowId).distinct.sorted.take(8),
        outputArtifact = "standing-audit/standing-tactical-ledger.jsonl"
      )

  private def supportOnlyStandingAction(
      report: LowerDiagnosticStandingAuditReport
  ): Option[LowerDiagnosticChessActionItem] =
    Option.when(report.rows.nonEmpty):
      LowerDiagnosticChessActionItem(
        priority = 5,
        actionId = "keep-standing-tactical-smells-support-only",
        actionType = "support_only_boundary",
        target = "loose_piece,pinned_piece,xray_target,overloaded_piece,trapped_piece and witness tactical smells",
        candidateCount = report.rows.size,
        publicAdmission = "support_only",
        rationale = "These rows have current-board tactical facts without replayable transition identity.",
        requiredEvidence = Vector("beforeFen and playedMove before move-causal promotion", "row-level ledger class", "false-positive tag"),
        falsePositiveControl = "Incidental source-unrelated, negative fixture, material-skew, and context-missing rows stay out of public move-local claims.",
        exampleRowIds = report.rows.map(_.rowId).distinct.sorted.take(8),
        outputArtifact = "standing-audit/standing-tactical-ledger.jsonl"
      )

  private def positiveControlAction(
      report: LowerDiagnosticBottleneckReport
  ): Option[LowerDiagnosticChessActionItem] =
    val slices = report.fourthPhaseSlices.filter(_.ownerLayer == "positive_control")
    val count = slices.map(_.candidateCount).sum
    Option.when(count > 0):
      LowerDiagnosticChessActionItem(
        priority = 9,
        actionId = "preserve-existing-move-local-positive-controls",
        actionType = "regression_control",
        target = slices.map(_.tacticalSchema).distinct.sorted.mkString(","),
        candidateCount = count,
        publicAdmission = "already_admitted_control",
        rationale = "Existing selected move-local tactical rows should remain green while adjacent slices are expanded.",
        requiredEvidence = Vector("existing exact transition positive", "existing anti-case coverage"),
        falsePositiveControl = "Any new slice must keep these controls selected and keep standing/pre-existing anti-cases closed.",
        exampleRowIds = slices.flatMap(_.exampleRowIds).distinct.sorted.take(8),
        outputArtifact = "phase3/fourth-phase-slices.jsonl"
      )

  private def countByWeighted(values: Vector[(String, Int)]): Map[String, Int] =
    values.groupMapReduce(_._1)(_._2)(_ + _).toVector.sortBy(_._1).toMap.withDefaultValue(0)

object LowerDiagnosticChessActionJson:

  def summaryJson(summary: LowerDiagnosticChessActionSummary): JsObject =
    Json.obj(
      "totalRows" -> summary.totalRows,
      "inputBlockedRows" -> summary.inputBlockedRows,
      "replayableRows" -> summary.replayableRows,
      "standingRows" -> summary.standingRows,
      "immediateCaptureStaticFacts" -> summary.immediateCaptureStaticFacts,
      "exactTransitionSliceRows" -> summary.exactTransitionSliceRows,
      "rootWitnessSchemaRows" -> summary.rootWitnessSchemaRows,
      "countsByActionType" -> summary.countsByActionType
    )

  def actionJson(action: LowerDiagnosticChessActionItem): JsObject =
    Json.obj(
      "priority" -> action.priority,
      "actionId" -> action.actionId,
      "actionType" -> action.actionType,
      "target" -> action.target,
      "candidateCount" -> action.candidateCount,
      "publicAdmission" -> action.publicAdmission,
      "rationale" -> action.rationale,
      "requiredEvidence" -> action.requiredEvidence,
      "falsePositiveControl" -> action.falsePositiveControl,
      "exampleRowIds" -> action.exampleRowIds,
      "outputArtifact" -> action.outputArtifact
    )

object LowerDiagnosticChessActionRunner:

  private val defaultOutputDir = Paths.get("tmp/commentary-diagnostic/lower-layer/tracked/actions")

  def main(args: Array[String]): Unit =
    val options = RunnerOptions.parse(args.toVector)
    val rows =
      options.inputPath match
        case Some(path) => LowerDiagnosticLargeCorpus.loadExternalRows(path, options.limit)
        case None       => LowerDiagnosticLargeCorpus.loadTrackedRows()
    write(LowerDiagnosticChessActionPlan.fromRows(rows), options.outputDir)

  def write(plan: LowerDiagnosticChessActionPlan, outputDir: Path): Unit =
    Files.createDirectories(outputDir)
    Files.writeString(
      outputDir.resolve("chess-action-summary.json"),
      Json.prettyPrint(LowerDiagnosticChessActionJson.summaryJson(plan.summary)) + System.lineSeparator(),
      StandardCharsets.UTF_8
    )
    writeJsonl(outputDir.resolve("chess-action-items.jsonl"), plan.actions.map(LowerDiagnosticChessActionJson.actionJson))
    writeJsonl(
      outputDir.resolve("transition-slice-handoff.jsonl"),
      plan.actions.filter(_.actionType == "exact_transition_slice").map(LowerDiagnosticChessActionJson.actionJson)
    )
    Files.writeString(outputDir.resolve("chess-action-report.md"), markdown(plan), StandardCharsets.UTF_8)

  private def markdown(plan: LowerDiagnosticChessActionPlan): String =
    val header = Vector(
      "# Lower Diagnostic Chess Action Plan",
      "",
      s"- total rows: ${plan.summary.totalRows}",
      s"- input blocked rows: ${plan.summary.inputBlockedRows}",
      s"- replayable rows: ${plan.summary.replayableRows}",
      s"- standing current-board tactical rows: ${plan.summary.standingRows}",
      s"- immediate-capture static facts: ${plan.summary.immediateCaptureStaticFacts}",
      s"- exact transition slice rows: ${plan.summary.exactTransitionSliceRows}",
      s"- root/witness schema rows: ${plan.summary.rootWitnessSchemaRows}",
      ""
    )
    val actions = plan.actions.map: action =>
      Vector(
        s"## P${action.priority} ${action.actionId}",
        "",
        s"- type: `${action.actionType}`",
        s"- target: `${action.target}`",
        s"- candidates: ${action.candidateCount}",
        s"- public admission: `${action.publicAdmission}`",
        s"- rationale: ${action.rationale}",
        s"- false-positive control: ${action.falsePositiveControl}",
        s"- required evidence: ${action.requiredEvidence.map(e => s"`$e`").mkString(", ")}",
        s"- examples: ${action.exampleRowIds.map(id => s"`$id`").mkString(", ")}",
        s"- artifact: `${action.outputArtifact}`",
        ""
      ).mkString(System.lineSeparator)
    header.mkString(System.lineSeparator) + actions.mkString(System.lineSeparator)

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
