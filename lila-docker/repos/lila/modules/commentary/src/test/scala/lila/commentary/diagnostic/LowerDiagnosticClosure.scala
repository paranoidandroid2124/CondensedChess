package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import play.api.libs.json.*

final case class LowerDiagnosticClosureSummary(
    totalBottlenecks: Int,
    totalCandidates: Int,
    countsByClosureStatus: Map[String, Int],
    countsByExistingLayer: Map[String, Int],
    countsByWorkType: Map[String, Int]
)

final case class LowerDiagnosticClosure(
    key: String,
    candidateCount: Int,
    existingLayer: String,
    ownerPath: String,
    closureStatus: String,
    workType: String,
    implementationBoundary: String,
    falsePositiveControl: String,
    blockedBy: Option[String],
    action: String,
    tacticalSchema: String,
    sourceKind: String,
    countsByCaseType: Map[String, Int],
    countsByExpectation: Map[String, Int],
    exampleRowIds: Vector[String]
)

final case class LowerDiagnosticClosureReport(
    summary: LowerDiagnosticClosureSummary,
    closures: Vector[LowerDiagnosticClosure]
)

object LowerDiagnosticClosureReport:

  def fromBottlenecks(report: LowerDiagnosticBottleneckReport): LowerDiagnosticClosureReport =
    val closures = report.bottlenecks.map(closureFor)
    LowerDiagnosticClosureReport(
      summary = LowerDiagnosticClosureSummary(
        totalBottlenecks = closures.size,
        totalCandidates = closures.map(_.candidateCount).sum,
        countsByClosureStatus = countByWeighted(closures.map(c => c.closureStatus -> c.candidateCount)),
        countsByExistingLayer = countByWeighted(closures.map(c => c.existingLayer -> c.candidateCount)),
        countsByWorkType = countByWeighted(closures.map(c => c.workType -> c.candidateCount))
      ),
      closures = closures.sortBy(c => (sortRank(c), -c.candidateCount, c.key))
    )

  private def closureFor(bottleneck: LowerDiagnosticBottleneck): LowerDiagnosticClosure =
    val route = routeFor(bottleneck)
    LowerDiagnosticClosure(
      key = bottleneck.key,
      candidateCount = bottleneck.count,
      existingLayer = route.existingLayer,
      ownerPath = route.ownerPath,
      closureStatus = closureStatus(bottleneck),
      workType = workType(bottleneck),
      implementationBoundary = route.implementationBoundary,
      falsePositiveControl = falsePositiveControl(bottleneck),
      blockedBy = blockedBy(bottleneck),
      action = action(bottleneck, route),
      tacticalSchema = bottleneck.tacticalSchema,
      sourceKind = bottleneck.sourceKind,
      countsByCaseType = bottleneck.countsByCaseType,
      countsByExpectation = bottleneck.countsByExpectation,
      exampleRowIds = bottleneck.exampleRowIds
    )

  private def closureStatus(bottleneck: LowerDiagnosticBottleneck): String =
    bottleneck.ownerLayer match
      case "input_reconstruction" => "input_reconstruction_required"
      case "standing_board_fact_only" => "current_board_only_closed"
      case "exact_transition_admission" if hasPositiveExpectation(bottleneck) =>
        "existing_transition_slice_candidate"
      case "exact_transition_admission" => "anti_case_only_no_expansion"
      case "root_witness_schema" if hasPositiveExpectation(bottleneck) =>
        "root_witness_contract_needed"
      case "root_witness_schema" => "fail_closed_schema_guard"
      case "non_tactical_or_untracked_schema" => "routed_to_domain_lower_layer"
      case "selection_line" => "selection_handoff_audit"
      case "positive_control" => "closed_regression_control"
      case _ => "manual_review_required"

  private def workType(bottleneck: LowerDiagnosticBottleneck): String =
    closureStatus(bottleneck) match
      case "input_reconstruction_required" => "input_replay_reconstruction"
      case "current_board_only_closed"     => "no_admission_change"
      case "anti_case_only_no_expansion"   => "negative_guard"
      case "closed_regression_control"     => "regression_control"
      case "routed_to_domain_lower_layer"  => "existing_layer_routing"
      case _                               => "existing_layer_reinforcement"

  private def blockedBy(bottleneck: LowerDiagnosticBottleneck): Option[String] =
    closureStatus(bottleneck) match
      case "input_reconstruction_required" => Some("missing_or_invalid_exact_transition_identity")
      case "current_board_only_closed"     => Some("no_move_local_causality")
      case "anti_case_only_no_expansion"   => Some("no_positive_row_in_this_slice")
      case _                               => None

  private def falsePositiveControl(bottleneck: LowerDiagnosticBottleneck): String =
    closureStatus(bottleneck) match
      case "input_reconstruction_required" =>
        "Do not use currentFen tactical roots as move-causal evidence until legal replay proves before/current/move identity."
      case "current_board_only_closed" =>
        "Keep standing tactical facts position-local/support-only; a public move-local claim needs a replayable transition slice."
      case "existing_transition_slice_candidate" =>
        "The slice must pair each positive with pre-existing, after-only, wrong-owner, and near-miss anti-cases before admission changes."
      case "anti_case_only_no_expansion" =>
        "Use as a negative regression guard; do not add admission behavior from an absent-only bucket."
      case "root_witness_contract_needed" =>
        "Root or U-witness contracts must define positives and negatives before any public claim consumes the schema."
      case "fail_closed_schema_guard" =>
        "Preserve the absent/near-miss behavior and document that no public tactical root is expected."
      case "routed_to_domain_lower_layer" =>
        "Keep outside tactical admission metrics; validate in its existing domain layer before any selection work."
      case "selection_handoff_audit" =>
        "Only inspect selector binding after admitted lower evidence already exists."
      case "closed_regression_control" =>
        "Keep as green control while adjacent slices change."
      case _ =>
        "Manual audit required before implementation."

  private def action(bottleneck: LowerDiagnosticBottleneck, route: LayerRoute): String =
    closureStatus(bottleneck) match
      case "input_reconstruction_required" =>
        "Restore beforeFen/playedMove/node identity through corpus or PGN replay, rerun diagnostics, then reclassify."
      case "current_board_only_closed" =>
        "No runtime expansion; retain current-board/support-only behavior unless exact transition context is added."
      case "existing_transition_slice_candidate" =>
        s"Open a narrow slice in ${route.ownerPath}; use existing claim/delta boundaries, not a new runtime layer."
      case "anti_case_only_no_expansion" =>
        s"Add or preserve negative coverage in ${route.ownerPath}; do not admit a new public move-causal claim."
      case "root_witness_contract_needed" =>
        s"Strengthen the existing ${route.existingLayer} contract with exact positives and fail-closed negatives."
      case "fail_closed_schema_guard" =>
        s"Record as a negative guard for ${route.existingLayer}; no new schema admission is justified."
      case "routed_to_domain_lower_layer" =>
        s"Evaluate under ${route.existingLayer}; it is not a tactical-root bottleneck."
      case "selection_handoff_audit" =>
        "Trace selected/suppressed claim binding in the existing selection layer."
      case "closed_regression_control" =>
        "Keep the row group as a non-regression control."
      case _ =>
        "Assign by direct exact-board audit before changing runtime behavior."

  private final case class LayerRoute(
      existingLayer: String,
      ownerPath: String,
      implementationBoundary: String
  )

  private def routeFor(bottleneck: LowerDiagnosticBottleneck): LayerRoute =
    bottleneck.ownerLayer match
      case "input_reconstruction" =>
        LayerRoute(
          "diagnostic_input",
          "lila.commentary.diagnostic.LowerDiagnosticLargeCorpus",
          "test/tooling corpus loader and upstream PGN/node replay"
        )
      case "standing_board_fact_only" =>
        LayerRoute(
          "claim_current_board_support",
          "lila.commentary.claim.EvidenceClaimProducer",
          "existing support-only current-board tactical claim boundary"
        )
      case "exact_transition_admission" =>
        LayerRoute(
          "delta_claim_admission",
          "lila.commentary.claim.ExactBoardClaimProducer",
          "existing exact transition admission and claim producer boundary"
        )
      case "root_witness_schema" =>
        routeBySourceOrSchema(bottleneck)
      case "non_tactical_or_untracked_schema" =>
        routeBySourceOrSchema(bottleneck)
      case "selection_line" =>
        LayerRoute(
          "selection",
          "lila.commentary.selection.CommentarySelection",
          "existing claim selection and line handoff boundary"
        )
      case "positive_control" =>
        LayerRoute(
          "diagnostic_regression_control",
          "lila.commentary.diagnostic.LowerLayerDiagnostic",
          "test/tooling positive-control corpus"
        )
      case _ =>
        LayerRoute("manual_audit", "lila.commentary.diagnostic", "manual exact-board diagnostic review")

  private def routeBySourceOrSchema(bottleneck: LowerDiagnosticBottleneck): LayerRoute =
    bottleneck.sourceKind match
      case "root" =>
        LayerRoute("root", "lila.commentary.root.RootExtractor", "existing lower-root extraction boundary")
      case "witness" =>
        LayerRoute("witness", witnessOwnerPath(bottleneck.tacticalSchema), "existing U-witness/support-seed boundary")
      case "object" =>
        LayerRoute("strategic_object", "lila.commentary.strategic", "existing strategic object rule boundary")
      case "delta" =>
        LayerRoute("delta", "lila.commentary.delta", "existing strategic delta rule boundary")
      case "certification" =>
        LayerRoute("certification", "lila.commentary.certification", "existing certification rule boundary")
      case "projection" =>
        LayerRoute("projection", "lila.commentary.projection.StrategyProjectionAdmission", "existing projection admission boundary")
      case "engine_probe" =>
        LayerRoute("validation_probe", "lila.commentary.validation", "test-only engine confound validation boundary")
      case _ if bottleneck.tacticalSchema.startsWith("S") =>
        LayerRoute("projection", "lila.commentary.projection.StrategyProjectionAdmission", "existing projection admission boundary")
      case _ =>
        LayerRoute("domain_lower_layer", "lila.commentary", "existing domain-specific lower layer")

  private def witnessOwnerPath(schema: String): String =
    if schema.contains("seed") || schema.contains("support") then "lila.commentary.witness.seed"
    else "lila.commentary.witness.u"

  private def hasPositiveExpectation(bottleneck: LowerDiagnosticBottleneck): Boolean =
    bottleneck.countsByExpectation.exists: (key, count) =>
      count > 0 && Set("present", "admitted", "certified").contains(key)

  private def sortRank(closure: LowerDiagnosticClosure): Int =
    closure.closureStatus match
      case "input_reconstruction_required"         => 1
      case "existing_transition_slice_candidate"   => 2
      case "anti_case_only_no_expansion"           => 3
      case "root_witness_contract_needed"          => 4
      case "fail_closed_schema_guard"              => 5
      case "current_board_only_closed"             => 6
      case "routed_to_domain_lower_layer"          => 7
      case "selection_handoff_audit"               => 8
      case "closed_regression_control"             => 9
      case _                                       => 10

  private def countByWeighted(values: Vector[(String, Int)]): Map[String, Int] =
    values.groupMapReduce(_._1)(_._2)(_ + _).toVector.sortBy(_._1).toMap.withDefaultValue(0)

object LowerDiagnosticClosureJson:

  def summaryJson(summary: LowerDiagnosticClosureSummary): JsObject =
    Json.obj(
      "totalBottlenecks" -> summary.totalBottlenecks,
      "totalCandidates" -> summary.totalCandidates,
      "countsByClosureStatus" -> summary.countsByClosureStatus,
      "countsByExistingLayer" -> summary.countsByExistingLayer,
      "countsByWorkType" -> summary.countsByWorkType
    )

  def closureJson(closure: LowerDiagnosticClosure): JsObject =
    Json.obj(
      "key" -> closure.key,
      "candidateCount" -> closure.candidateCount,
      "existingLayer" -> closure.existingLayer,
      "ownerPath" -> closure.ownerPath,
      "closureStatus" -> closure.closureStatus,
      "workType" -> closure.workType,
      "implementationBoundary" -> closure.implementationBoundary,
      "falsePositiveControl" -> closure.falsePositiveControl,
      "blockedBy" -> closure.blockedBy,
      "action" -> closure.action,
      "tacticalSchema" -> closure.tacticalSchema,
      "sourceKind" -> closure.sourceKind,
      "countsByCaseType" -> closure.countsByCaseType,
      "countsByExpectation" -> closure.countsByExpectation,
      "exampleRowIds" -> closure.exampleRowIds
    )

  def table(report: LowerDiagnosticClosureReport): String =
    val header =
      "| status | candidates | existing layer | work type | action |\n" +
        "| --- | ---: | --- | --- | --- |"
    val rows = report.summary.countsByClosureStatus.toVector
      .sortBy((status, count) => (statusRank(status), -count))
      .map: (status, count) =>
        val sample = report.closures.find(_.closureStatus == status).get
        s"| $status | $count | ${sample.existingLayer} | ${sample.workType} | ${escape(sample.action)} |"
    (Vector(header) ++ rows).mkString(System.lineSeparator()) + System.lineSeparator()

  private def statusRank(status: String): Int =
    status match
      case "input_reconstruction_required"       => 1
      case "existing_transition_slice_candidate" => 2
      case "anti_case_only_no_expansion"         => 3
      case "root_witness_contract_needed"        => 4
      case "fail_closed_schema_guard"            => 5
      case "current_board_only_closed"           => 6
      case "routed_to_domain_lower_layer"        => 7
      case "selection_handoff_audit"             => 8
      case "closed_regression_control"           => 9
      case _                                     => 10

  private def escape(value: String): String =
    value.replace("|", "\\|")

object LowerDiagnosticClosureRunner:

  private val defaultOutputDir = Paths.get("tmp/commentary-diagnostic/lower-layer/tracked/closure")

  def main(args: Array[String]): Unit =
    val options = RunnerOptions.parse(args.toVector)
    val rows =
      options.inputPath match
        case Some(path) => LowerDiagnosticLargeCorpus.loadExternalRows(path, options.limit)
        case None       => LowerDiagnosticLargeCorpus.loadTrackedRows()
    val diagnostic = LowerDiagnosticReport.fromRows(rows)
    val bottlenecks = LowerDiagnosticBottleneckReport.fromDiagnostic(diagnostic)
    write(LowerDiagnosticClosureReport.fromBottlenecks(bottlenecks), options.outputDir)

  def write(report: LowerDiagnosticClosureReport, outputDir: Path): Unit =
    Files.createDirectories(outputDir)
    Files.writeString(
      outputDir.resolve("closure-summary.json"),
      Json.prettyPrint(LowerDiagnosticClosureJson.summaryJson(report.summary)) + System.lineSeparator(),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      outputDir.resolve("bottleneck-closures.jsonl"),
      report.closures.map(LowerDiagnosticClosureJson.closureJson).map(Json.stringify).mkString("", System.lineSeparator(), System.lineSeparator()),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      outputDir.resolve("closure-table.md"),
      LowerDiagnosticClosureJson.table(report),
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
