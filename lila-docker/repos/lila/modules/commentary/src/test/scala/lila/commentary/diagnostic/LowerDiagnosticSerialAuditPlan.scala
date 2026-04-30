package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import play.api.libs.json.*

final case class LowerDiagnosticSerialAuditSummary(
    totalHighRiskTransitions: Int,
    uniqueTransitionKeys: Int,
    sourceUnverifiedTransitions: Int,
    sourceUnverifiedUniqueTransitionKeys: Int,
    nextAction: String
)

final case class LowerDiagnosticSerialAuditSlice(
    order: Int,
    actionId: String,
    label: String,
    candidateTransitions: Int,
    sourceVerifiedEligibleTransitions: Int,
    sourceBlockedTransitions: Int,
    outputArtifact: String,
    completionGate: Vector[String],
    nextHandoff: String
)

final case class LowerDiagnosticSerialAuditPlanReport(
    summary: LowerDiagnosticSerialAuditSummary,
    slices: Vector[LowerDiagnosticSerialAuditSlice],
    rowsByActionId: Map[String, Vector[LowerDiagnosticHighRiskTacticalAuditRow]]
)

object LowerDiagnosticSerialAuditPlan:

  private val OrderedSpecs: Vector[SliceSpec] =
    Vector(
      SliceSpec(
        actionId = "source-pgn-verification",
        label = "Recover and verify source PGN identity",
        outputArtifact = "00-source-pgn-verification-blockers.jsonl",
        completionGate = Vector("source PGN files available", "beforeFen/full-FEN matches source node", "playedMove matches source node", "currentFen/full-FEN matches source replay"),
        nextHandoff = "loose-touched-anchor-capture-audit"
      ),
      SliceSpec(
        actionId = "loose-touched-anchor-capture-audit",
        label = "Loose piece created on the played move anchor",
        outputArtifact = "01-loose-touched-anchor-capture-audit.jsonl",
        completionGate = Vector("source PGN verified", "moved or captured piece relation verified", "attacker/defender exchange payload present", "immediate capture separated from loose smell", "material/mate suppression checked"),
        nextHandoff = "loose-non-touch-created-causality-audit",
        predicate = row => row.auditDisposition == "candidate_created_loose_piece_requires_capture_audit"
      ),
      SliceSpec(
        actionId = "loose-non-touch-created-causality-audit",
        label = "Loose piece appears after the move without touching the loose anchor",
        outputArtifact = "02-loose-non-touch-created-causality-audit.jsonl",
        completionGate = Vector("source PGN verified", "removed defender or opened attacker line identified", "unrelated board smell rejected", "material/mate suppression checked"),
        nextHandoff = "pinned-created-pin-geometry-audit",
        predicate = row => row.auditDisposition == "candidate_created_loose_piece_requires_causality_audit"
      ),
      SliceSpec(
        actionId = "pinned-created-pin-geometry-audit",
        label = "Pinned piece created after the move",
        outputArtifact = "03-pinned-created-pin-geometry-audit.jsonl",
        completionGate = Vector("source PGN verified", "pinner identity present", "pinned piece identity present", "king or higher-value anchor present", "absolute/relative pin classified", "move-created or move-released line proof present"),
        nextHandoff = "xray-created-geometry-audit",
        predicate = row => row.auditDisposition == "candidate_created_pinned_piece_requires_pin_geometry_audit"
      ),
      SliceSpec(
        actionId = "xray-created-geometry-audit",
        label = "X-ray target created after the move",
        outputArtifact = "04-xray-created-geometry-audit.jsonl",
        completionGate = Vector("source PGN verified", "slider identity present", "blocker identity present", "target value and owner present", "before/after line geometry present", "move relation verified"),
        nextHandoff = "pre-existing-anti-case-guard",
        predicate = row => row.auditDisposition == "candidate_created_xray_requires_geometry_audit"
      ),
      SliceSpec(
        actionId = "pre-existing-anti-case-guard",
        label = "Pre-existing high-risk tactical facts",
        outputArtifact = "05-pre-existing-anti-case-guard.jsonl",
        completionGate = Vector("source PGN verified when available", "same fact exists before the move", "selection/public admission remains blocked", "anti-case regression fixture created"),
        nextHandoff = "trapped-overloaded-followup",
        predicate = row => row.auditDisposition.contains("preexisting_or_standing")
      ),
      SliceSpec(
        actionId = "trapped-overloaded-followup",
        label = "Follow-up audit for trapped_piece and overloaded_piece",
        outputArtifact = "06-trapped-overloaded-followup.jsonl",
        completionGate = Vector("separate trapped/overloaded audit runner exists", "escape set or defender-duty payload present", "material/mate suppression checked"),
        nextHandoff = "done"
      )
    )

  def fromRows(rows: Vector[LowerDiagnosticLargeCorpus.Row], pgnRoots: Vector[Path] = Vector.empty): LowerDiagnosticSerialAuditPlanReport =
    fromHighRisk(LowerDiagnosticHighRiskTacticalAuditReport.fromRows(rows, pgnRoots))

  def fromHighRisk(highRisk: LowerDiagnosticHighRiskTacticalAuditReport): LowerDiagnosticSerialAuditPlanReport =
    val sourceRows = highRisk.rows.filter(_.sourceVerificationState != "verified")
    val rowsByAction = OrderedSpecs.map: spec =>
      val sliceRows =
        if spec.actionId == "source-pgn-verification" then sourceRows
        else highRisk.rows.filter(spec.predicate)
      spec.actionId -> sliceRows
    .toMap
    val slices = OrderedSpecs.zipWithIndex.map: (spec, index) =>
      LowerDiagnosticSerialAuditSlice(
        order = index,
        actionId = spec.actionId,
        label = spec.label,
        candidateTransitions = rowsByAction(spec.actionId).size,
        sourceVerifiedEligibleTransitions = rowsByAction(spec.actionId).count(_.sourceVerificationState == "verified"),
        sourceBlockedTransitions = rowsByAction(spec.actionId).count(_.sourceVerificationState != "verified"),
        outputArtifact = spec.outputArtifact,
        completionGate = spec.completionGate,
        nextHandoff = spec.nextHandoff
      )
    LowerDiagnosticSerialAuditPlanReport(
      summary = LowerDiagnosticSerialAuditSummary(
        totalHighRiskTransitions = highRisk.summary.uniqueTransitions,
        uniqueTransitionKeys = highRisk.summary.uniqueTransitionKeys,
        sourceUnverifiedTransitions = sourceRows.size,
        sourceUnverifiedUniqueTransitionKeys = sourceRows.map(_.transitionKey).distinct.size,
        nextAction =
          if sourceRows.nonEmpty then "recover_source_pgn_then_run_loose_touched_anchor_capture_audit"
          else "run_loose_touched_anchor_capture_audit"
      ),
      slices = slices,
      rowsByActionId = rowsByAction
    )

  private final case class SliceSpec(
      actionId: String,
      label: String,
      outputArtifact: String,
      completionGate: Vector[String],
      nextHandoff: String,
      predicate: LowerDiagnosticHighRiskTacticalAuditRow => Boolean = _ => false
  )

object LowerDiagnosticSerialAuditPlanJson:

  def summaryJson(summary: LowerDiagnosticSerialAuditSummary): JsObject =
    Json.obj(
      "totalHighRiskTransitions" -> summary.totalHighRiskTransitions,
      "uniqueTransitionKeys" -> summary.uniqueTransitionKeys,
      "sourceUnverifiedTransitions" -> summary.sourceUnverifiedTransitions,
      "sourceUnverifiedUniqueTransitionKeys" -> summary.sourceUnverifiedUniqueTransitionKeys,
      "nextAction" -> summary.nextAction
    )

  def sliceJson(slice: LowerDiagnosticSerialAuditSlice): JsObject =
    Json.obj(
      "order" -> slice.order,
      "actionId" -> slice.actionId,
      "label" -> slice.label,
      "candidateTransitions" -> slice.candidateTransitions,
      "sourceVerifiedEligibleTransitions" -> slice.sourceVerifiedEligibleTransitions,
      "sourceBlockedTransitions" -> slice.sourceBlockedTransitions,
      "outputArtifact" -> slice.outputArtifact,
      "completionGate" -> slice.completionGate,
      "nextHandoff" -> slice.nextHandoff
    )

object LowerDiagnosticSerialAuditPlanRunner:

  private val defaultOutputDir = Paths.get("tmp/commentary-diagnostic/lower-layer/serial-audit-plan")

  def main(args: Array[String]): Unit =
    val options = RunnerOptions.parse(args.toVector)
    val rows =
      options.inputPath match
        case Some(path) => LowerDiagnosticLargeCorpus.loadExternalRows(path, options.limit)
        case None       => LowerDiagnosticLargeCorpus.loadTrackedRows()
    write(LowerDiagnosticSerialAuditPlan.fromRows(rows, options.pgnRoots), options.outputDir)

  def write(report: LowerDiagnosticSerialAuditPlanReport, outputDir: Path): Unit =
    Files.createDirectories(outputDir)
    Files.writeString(
      outputDir.resolve("serial-audit-summary.json"),
      Json.prettyPrint(LowerDiagnosticSerialAuditPlanJson.summaryJson(report.summary)) + System.lineSeparator(),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      outputDir.resolve("serial-audit-slices.jsonl"),
      report.slices.map(LowerDiagnosticSerialAuditPlanJson.sliceJson).map(Json.stringify).mkString("", System.lineSeparator(), System.lineSeparator()),
      StandardCharsets.UTF_8
    )
    report.slices.foreach: slice =>
      val rows = report.rowsByActionId.getOrElse(slice.actionId, Vector.empty)
      Files.writeString(
        outputDir.resolve(slice.outputArtifact),
        rows.map(LowerDiagnosticHighRiskTacticalAuditJson.rowJson).map(Json.stringify).mkString("", System.lineSeparator(), System.lineSeparator()),
        StandardCharsets.UTF_8
      )

  private final case class RunnerOptions(outputDir: Path, inputPath: Option[Path], limit: Option[Int], pgnRoots: Vector[Path])

  private object RunnerOptions:
    def parse(args: Vector[String]): RunnerOptions =
      def valueAfter(flag: String): Option[String] =
        args.sliding(2).collectFirst { case Vector(`flag`, value) => value }
      RunnerOptions(
        outputDir = valueAfter("--out").map(Paths.get(_)).getOrElse(defaultOutputDir),
        inputPath = valueAfter("--input").map(Paths.get(_)),
        limit = valueAfter("--limit").flatMap(_.toIntOption),
        pgnRoots = args.zipWithIndex.collect {
          case ("--pgn-root", index) if args.lift(index + 1).nonEmpty => Paths.get(args(index + 1))
        }
      )
