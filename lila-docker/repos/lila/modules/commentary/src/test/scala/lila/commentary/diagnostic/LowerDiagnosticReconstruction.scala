package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import play.api.libs.json.*

final case class LowerDiagnosticReconstructionSummary(
    total: Int,
    countsByState: Map[String, Int],
    countsByReason: Map[String, Int],
    replayableRows: Int,
    unreconstructableRows: Int,
    ambiguousRows: Int
)

final case class LowerDiagnosticReconstructionRow(
    rowId: String,
    sourceFile: String,
    sourceKind: String,
    sourceSchema: Option[String],
    currentFen: String,
    beforeFen: Option[String],
    playedMove: Option[String],
    playedUci: Option[String],
    nodeId: String,
    ply: Int,
    inputStatus: String,
    transitionStatus: String,
    transitionReason: Option[String],
    metadata: Map[String, String],
    reconstructionState: String,
    reason: String,
    requiredInputs: Vector[String],
    nextAction: String
)

final case class LowerDiagnosticReconstructionReport(
    summary: LowerDiagnosticReconstructionSummary,
    rows: Vector[LowerDiagnosticReconstructionRow]
)

object LowerDiagnosticReconstructionReport:

  def fromDiagnostic(report: LowerDiagnosticReport): LowerDiagnosticReconstructionReport =
    val rows = report.traces.map(rowFor)
    LowerDiagnosticReconstructionReport(
      summary = LowerDiagnosticReconstructionSummary(
        total = rows.size,
        countsByState = countLabels(rows.map(_.reconstructionState)),
        countsByReason = countLabels(rows.map(_.reason)),
        replayableRows = rows.count(_.reconstructionState == "replayable"),
        unreconstructableRows = rows.count(_.reconstructionState == "unreconstructable"),
        ambiguousRows = rows.count(_.reconstructionState == "ambiguous")
      ),
      rows = rows.sortBy(row => (stateRank(row.reconstructionState), row.reason, row.rowId))
    )

  private def rowFor(row: LowerDiagnosticTraceRow): LowerDiagnosticReconstructionRow =
    val playedUci = row.row.metadata.get("playedUci")
    val state = reconstructionState(row, playedUci)
    val reason = reconstructionReason(row, playedUci, state)
    LowerDiagnosticReconstructionRow(
      rowId = row.row.id,
      sourceFile = row.row.sourceFile,
      sourceKind = row.row.sourceKind,
      sourceSchema = row.row.sourceSchema,
      currentFen = row.row.input.currentFen,
      beforeFen = row.row.input.beforeFen,
      playedMove = row.row.input.playedMove,
      playedUci = playedUci,
      nodeId = row.row.input.nodeId,
      ply = row.row.input.ply,
      inputStatus = row.trace.input.status,
      transitionStatus = row.trace.transition.status,
      transitionReason = row.trace.transition.reason,
      metadata = row.row.metadata,
      reconstructionState = state,
      reason = reason,
      requiredInputs = requiredInputs(state, reason),
      nextAction = nextAction(state, reason)
    )

  private def reconstructionState(row: LowerDiagnosticTraceRow, playedUci: Option[String]): String =
    if row.trace.input.status != "valid" then "unreconstructable"
    else if row.trace.transition.status == "valid" then "replayable"
    else if playedUci.nonEmpty && row.row.input.beforeFen.isEmpty then "unreconstructable"
    else if row.trace.transition.status == "missing_pair" then "unreconstructable"
    else if Set("invalid", "invalid_clock", "after_mismatch").contains(row.trace.transition.status) then "unreconstructable"
    else if row.row.input.beforeFen.isEmpty && row.row.input.playedMove.isEmpty then "current_board_only"
    else "ambiguous"

  private def reconstructionReason(
      row: LowerDiagnosticTraceRow,
      playedUci: Option[String],
      state: String
  ): String =
    if row.trace.input.status != "valid" then "invalid_current_fen"
    else if row.trace.transition.status == "valid" then "legal_replay_matches_current"
    else if playedUci.nonEmpty && row.row.input.beforeFen.isEmpty then "missing_before_fen_for_played_uci"
    else if row.trace.transition.status == "missing_pair" then "before_fen_and_played_move_must_be_supplied_together"
    else if row.trace.transition.status == "invalid_clock" then "stale_or_mismatched_full_fen_clock"
    else if row.trace.transition.status == "after_mismatch" then "replay_after_state_mismatch"
    else if row.trace.transition.status == "invalid" then "illegal_or_invalid_transition"
    else if state == "current_board_only" then "no_transition_claim_requested"
    else "manual_reconstruction_review"

  private def requiredInputs(state: String, reason: String): Vector[String] =
    (state, reason) match
      case ("replayable", _) =>
        Vector("beforeFen", "currentFen", "playedMove", "legal replay after-state")
      case (_, "missing_before_fen_for_played_uci") =>
        Vector("beforeFen", "currentFen", "playedUci", "nodeId", "ply or PGN move index")
      case (_, "stale_or_mismatched_full_fen_clock") =>
        Vector("clock-correct beforeFen", "clock-correct currentFen", "playedMove")
      case (_, "replay_after_state_mismatch") =>
        Vector("board-exact beforeFen", "board-exact currentFen", "playedMove")
      case ("current_board_only", _) =>
        Vector("beforeFen and playedMove only if a move-causal claim is requested")
      case _ =>
        Vector("valid currentFen", "beforeFen", "playedMove")

  private def nextAction(state: String, reason: String): String =
    (state, reason) match
      case ("replayable", _) =>
        "Eligible for exact lower diagnostics and narrow transition-slice review."
      case (_, "missing_before_fen_for_played_uci") =>
        "Recover beforeFen from PGN/node replay before any move-causal tactical admission."
      case (_, "no_transition_claim_requested") =>
        "Keep as current-board evidence only; do not treat as last-move explanation."
      case (_, "stale_or_mismatched_full_fen_clock") =>
        "Fix full-FEN clocks or exclude from transition admission."
      case (_, "replay_after_state_mismatch") =>
        "Resolve board mismatch before lower claim analysis."
      case _ =>
        "Exclude from public admission until exact input identity is repaired."

  private def stateRank(state: String): Int =
    state match
      case "unreconstructable" => 1
      case "ambiguous"         => 2
      case "current_board_only" => 3
      case "replayable"        => 4
      case _                   => 9

  private def countLabels(values: Vector[String]): Map[String, Int] =
    values.groupMapReduce(identity)(_ => 1)(_ + _).toVector.sortBy(_._1).toMap.withDefaultValue(0)

object LowerDiagnosticReconstructionJson:

  def summaryJson(summary: LowerDiagnosticReconstructionSummary): JsObject =
    Json.obj(
      "total" -> summary.total,
      "countsByState" -> summary.countsByState,
      "countsByReason" -> summary.countsByReason,
      "replayableRows" -> summary.replayableRows,
      "unreconstructableRows" -> summary.unreconstructableRows,
      "ambiguousRows" -> summary.ambiguousRows
    )

  def rowJson(row: LowerDiagnosticReconstructionRow): JsObject =
    Json.obj(
      "rowId" -> row.rowId,
      "sourceFile" -> row.sourceFile,
      "sourceKind" -> row.sourceKind,
      "sourceSchema" -> row.sourceSchema,
      "currentFen" -> row.currentFen,
      "beforeFen" -> row.beforeFen,
      "playedMove" -> row.playedMove,
      "playedUci" -> row.playedUci,
      "nodeId" -> row.nodeId,
      "ply" -> row.ply,
      "inputStatus" -> row.inputStatus,
      "transitionStatus" -> row.transitionStatus,
      "transitionReason" -> row.transitionReason,
      "metadata" -> row.metadata,
      "reconstructionState" -> row.reconstructionState,
      "reason" -> row.reason,
      "requiredInputs" -> row.requiredInputs,
      "nextAction" -> row.nextAction
    )

object LowerDiagnosticReconstructionRunner:

  private val defaultOutputDir = Paths.get("tmp/commentary-diagnostic/lower-layer/tracked/reconstruction")

  def main(args: Array[String]): Unit =
    val options = RunnerOptions.parse(args.toVector)
    val rows =
      options.inputPath match
        case Some(path) => LowerDiagnosticLargeCorpus.loadExternalRows(path, options.limit)
        case None       => LowerDiagnosticLargeCorpus.loadTrackedRows()
    val diagnostic = LowerDiagnosticReport.fromRows(rows)
    write(LowerDiagnosticReconstructionReport.fromDiagnostic(diagnostic), options.outputDir)

  def write(report: LowerDiagnosticReconstructionReport, outputDir: Path): Unit =
    Files.createDirectories(outputDir)
    Files.writeString(
      outputDir.resolve("reconstruction-summary.json"),
      Json.prettyPrint(LowerDiagnosticReconstructionJson.summaryJson(report.summary)) + System.lineSeparator(),
      StandardCharsets.UTF_8
    )
    writeJsonl(outputDir.resolve("reconstruction-traces.jsonl"), report.rows.map(LowerDiagnosticReconstructionJson.rowJson))
    writeJsonl(
      outputDir.resolve("replayable-transitions.jsonl"),
      report.rows.filter(_.reconstructionState == "replayable").map(LowerDiagnosticReconstructionJson.rowJson)
    )
    writeJsonl(
      outputDir.resolve("unreconstructable-rows.jsonl"),
      report.rows.filter(_.reconstructionState == "unreconstructable").map(LowerDiagnosticReconstructionJson.rowJson)
    )
    writeJsonl(
      outputDir.resolve("ambiguous-transitions.jsonl"),
      report.rows.filter(_.reconstructionState == "ambiguous").map(LowerDiagnosticReconstructionJson.rowJson)
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
