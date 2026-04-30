package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import chess.format.Fen
import chess.variant
import chess.{ Bishop, Bitboard, Color, King, Knight, Pawn, Piece, Position, Queen, Role, Rook, Square }
import play.api.libs.json.*

final case class LowerDiagnosticStandingAuditSummary(
    total: Int,
    countsByTag: Map[String, Int],
    countsByPrimaryTag: Map[String, Int],
    countsByLedgerClass: Map[String, Int],
    countsByPublicDisposition: Map[String, Int],
    countsBySourceKind: Map[String, Int],
    countsByTacticalSchema: Map[String, Int]
)

final case class LowerDiagnosticStandingAuditRow(
    rowId: String,
    sourceKind: String,
    sourceSchema: Option[String],
    caseType: Option[String],
    expectation: Option[String],
    currentFen: String,
    tacticalSchemas: Vector[String],
    rootFacts: Vector[LowerLayerDiagnostic.RootFact],
    selectedClaimId: Option[String],
    selectedOwner: Option[String],
    sideToMove: Option[String],
    tags: Vector[String],
    primaryTag: String,
    ledgerClass: String,
    publicDisposition: String,
    nextChessAction: String,
    auditNote: String
)

final case class LowerDiagnosticStandingAuditReport(
    summary: LowerDiagnosticStandingAuditSummary,
    rows: Vector[LowerDiagnosticStandingAuditRow]
)

object LowerDiagnosticStandingAuditReport:

  private val tacticalSourceSchemas: Set[String] =
    Set(
      "fork",
      "pin",
      "skewer",
      "overload",
      "duty_bound_defender",
      "loose_piece_target_state",
      "short_run_slider_gate_restriction"
    )

  private val tacticalRootSchemas: Set[String] =
    Set("loose_piece", "pinned_piece", "overloaded_piece", "trapped_piece", "xray_target")

  def fromDiagnostic(report: LowerDiagnosticReport): LowerDiagnosticStandingAuditReport =
    val rows = report.traces
      .filter(row => row.layerBreak == "admission:standing_tactical_only")
      .filter(row => row.trace.transition.status == "none")
      .map(auditRow)
    LowerDiagnosticStandingAuditReport(
      summary = LowerDiagnosticStandingAuditSummary(
        total = rows.size,
        countsByTag = countLabels(rows.flatMap(_.tags)),
        countsByPrimaryTag = countLabels(rows.map(_.primaryTag)),
        countsByLedgerClass = countLabels(rows.map(_.ledgerClass)),
        countsByPublicDisposition = countLabels(rows.map(_.publicDisposition)),
        countsBySourceKind = countLabels(rows.map(_.sourceKind)),
        countsByTacticalSchema = countLabels(rows.flatMap(_.tacticalSchemas))
      ),
      rows = rows.sortBy(row => (row.ledgerClass, row.primaryTag, row.rowId))
    )

  private def auditRow(row: LowerDiagnosticTraceRow): LowerDiagnosticStandingAuditRow =
    val position = positionFromFen(row.row.input.currentFen)
    val tags = auditTags(row, position).distinct.sorted
    LowerDiagnosticStandingAuditRow(
      rowId = row.row.id,
      sourceKind = row.row.sourceKind,
      sourceSchema = row.row.sourceSchema,
      caseType = row.row.caseType,
      expectation = row.row.expectation,
      currentFen = row.row.input.currentFen,
      tacticalSchemas = row.tacticalSchemas,
      rootFacts = row.trace.extraction.rootFacts,
      selectedClaimId = row.trace.selection.lead.map(_.id),
      selectedOwner = row.trace.selection.lead.flatMap(_.owner),
      sideToMove = position.map(pos => colorKey(pos.color)),
      tags = tags,
      primaryTag = primaryTag(tags),
      ledgerClass = ledgerClass(tags),
      publicDisposition = publicDisposition(tags),
      nextChessAction = nextChessAction(tags),
      auditNote = auditNote(tags)
    )

  private def auditTags(row: LowerDiagnosticTraceRow, position: Option[Position]): Vector[String] =
    Vector(
      Some("current_board_only_no_transition"),
      Option.when(sourceNegative(row))("source_negative_smell"),
      Option.when(sourceSchemaMismatch(row))("off_source_schema_smell"),
      Option.when(tacticalSourcePositive(row))("context_missing_plausible_static_tactic"),
      Option.when(position.exists(pos => hasHomePawnXray(row, pos)))("overbroad_home_pawn_xray"),
      Option.when(position.exists(pos => hasImmediateLooseCapture(row, pos)))("immediate_capture_static_fact"),
      Option.when(position.exists(pos => selectedStaticOwnerNotSideToMove(row, pos)))("selected_static_owner_not_side_to_move"),
      Option.when(position.exists(severeMaterialSkew))("severe_material_skew_context")
    ).flatten

  private def sourceNegative(row: LowerDiagnosticTraceRow): Boolean =
    row.row.expectation.contains("absent") ||
      row.row.caseType.exists(caseType => Set("near_miss", "nasty_negative", "false_rival", "shortcut_negative").contains(caseType) || caseType.contains("negative"))

  private def sourceSchemaMismatch(row: LowerDiagnosticTraceRow): Boolean =
    row.row.sourceSchema.exists: schema =>
      !tacticalRootSchemas.contains(schema) &&
        !tacticalSourceSchemas.contains(schema) &&
        row.trace.extraction.rootFacts.nonEmpty

  private def tacticalSourcePositive(row: LowerDiagnosticTraceRow): Boolean =
    row.row.sourceSchema.exists(schema => tacticalSourceSchemas.contains(schema) || schema.contains("fork") || schema.contains("pin")) &&
      row.row.expectation.forall(_ != "absent") &&
      !sourceNegative(row)

  private def hasHomePawnXray(row: LowerDiagnosticTraceRow, position: Position): Boolean =
    row.trace.extraction.rootFacts.exists: fact =>
      fact.id == "xray_target" &&
        fact.owner.flatMap(colorFromKey).exists: attacker =>
          fact.anchor.flatMap(Square.fromKey).exists: target =>
            position.pieceAt(target).exists(targetPiece => targetPiece.color == !attacker && targetPiece.role == Pawn) &&
              xrayLines(position, attacker, target).exists: line =>
                line.blockers match
                  case Vector(blocker) =>
                    position.pieceAt(blocker).exists: blockerPiece =>
                      blockerPiece.color == attacker &&
                        blockerPiece.role == Pawn &&
                        isHomePawn(attacker, blocker) &&
                        isHomePawn(!attacker, target)
                  case _ => false

  private def hasImmediateLooseCapture(row: LowerDiagnosticTraceRow, position: Position): Boolean =
    row.trace.extraction.rootFacts.exists: fact =>
      fact.id == "loose_piece" &&
        fact.owner.flatMap(colorFromKey).exists: vulnerable =>
          fact.anchor.flatMap(Square.fromKey).exists: target =>
            val attacker = !vulnerable
            position.color == attacker &&
              legalMoves(position, attacker).exists(move => move.dest == target && move.captures)

  private def selectedStaticOwnerNotSideToMove(row: LowerDiagnosticTraceRow, position: Position): Boolean =
    row.trace.selection.lead.exists: lead =>
      lead.route.contains("tactical_liability") &&
        lead.scope.contains("position_local") &&
        lead.owner.exists(_ != colorKey(position.color))

  private def severeMaterialSkew(position: Position): Boolean =
    val white = material(position, Color.White)
    val black = material(position, Color.Black)
    math.abs(white - black) >= 900

  private def primaryTag(tags: Vector[String]): String =
    Vector(
      "overbroad_home_pawn_xray",
      "selected_static_owner_not_side_to_move",
      "source_negative_smell",
      "off_source_schema_smell",
      "immediate_capture_static_fact",
      "context_missing_plausible_static_tactic",
      "severe_material_skew_context",
      "current_board_only_no_transition"
    ).find(tags.contains).getOrElse("manual_review")

  private def auditNote(tags: Vector[String]): String =
    primaryTag(tags) match
      case "overbroad_home_pawn_xray" =>
        "X-ray root is triggered by a home-pawn blocker and pawn target; audit RootExtractor xray_target before using this as tactical evidence."
      case "selected_static_owner_not_side_to_move" =>
        "Selector chose a static tactical liability for the side not to move; audit current-board tactical selection before public use."
      case "source_negative_smell" =>
        "The row is a negative/near-miss fixture with standing tactical roots; preserve as false-positive audit material."
      case "off_source_schema_smell" =>
        "The tactical root is incidental to a different source schema; do not treat it as source coverage."
      case "immediate_capture_static_fact" =>
        "The board has an immediate legal capture on a loose piece; this may need a stronger current-board fact, but still has no move causality."
      case "context_missing_plausible_static_tactic" =>
        "The source tactical fixture is plausible as a static fact, but needs beforeFen/playedMove before becoming a move explanation."
      case "severe_material_skew_context" =>
        "The tactical motif exists inside a large material-skew context; audit whether the local motif is pedagogically meaningful."
      case _ =>
        "Current-board tactical fact without transition context; keep out of move-causal admission."

  private def ledgerClass(tags: Vector[String]): String =
    primaryTag(tags) match
      case "overbroad_home_pawn_xray"              => "too_broad_xray_smell"
      case "selected_static_owner_not_side_to_move" => "side_to_move_mismatch_smell"
      case "source_negative_smell"                 => "schema_false_positive_or_negative_fixture"
      case "off_source_schema_smell"               => "source_unrelated_smell"
      case "immediate_capture_static_fact"         => "immediate_capture_available"
      case "context_missing_plausible_static_tactic" => "true_static_tactical_fact_context_missing"
      case "severe_material_skew_context"          => "static_fact_context_suppression_needed"
      case _                                       => "current_board_smell_context_missing"

  private def publicDisposition(tags: Vector[String]): String =
    primaryTag(tags) match
      case "immediate_capture_static_fact" =>
        "current_board_claim_candidate_not_move_causal"
      case "context_missing_plausible_static_tactic" =>
        "support_only_until_transition_context"
      case "severe_material_skew_context" =>
        "support_only_context_suppression_review"
      case "current_board_only_no_transition" =>
        "support_only_until_transition_context"
      case _ =>
        "false_positive_or_support_only"

  private def nextChessAction(tags: Vector[String]): String =
    primaryTag(tags) match
      case "immediate_capture_static_fact" =>
        "Audit whether the legal capture is pedagogically meaningful as a current-board opportunity; do not describe it as caused by the last move."
      case "context_missing_plausible_static_tactic" =>
        "Add beforeFen and playedMove before opening a move-causal tactical slice."
      case "source_negative_smell" =>
        "Keep as a regression anti-case; no admission expansion."
      case "off_source_schema_smell" =>
        "Route to the source schema owner; do not count incidental tactical roots as coverage."
      case "severe_material_skew_context" =>
        "Check mate/result/material context before allowing local tactic wording."
      case _ =>
        "Keep current-board root support-only unless exact transition evidence is supplied."

  private final case class XrayLine(slider: Square, target: Square, blockers: Vector[Square])

  private def xrayLines(position: Position, attacker: Color, target: Square): Vector[XrayLine] =
    position.board.pieceMap.toVector.collect:
      case (sliderSquare, piece)
          if piece.color == attacker &&
            Set(Bishop, Rook, Queen).contains(piece.role) &&
            sliderCanUseLine(piece.role, sliderSquare, target) =>
        val blockers = (Bitboard.between(sliderSquare, target) & position.board.occupied).squares.toVector
        Option.when(blockers.size == 1)(XrayLine(sliderSquare, target, blockers))
    .flatten

  private def sliderCanUseLine(role: Role, from: Square, to: Square): Boolean =
    role match
      case Bishop => from.onSameDiagonal(to)
      case Rook   => from.onSameLine(to)
      case Queen  => from.onSameDiagonal(to) || from.onSameLine(to)
      case _      => false

  private def legalMoves(position: Position, color: Color): Vector[chess.Move] =
    val colored = position.withColor(color)
    Square.all
      .filter(square => colored.pieceAt(square).exists(_.color == color))
      .flatMap(colored.generateMovesAt)
      .toVector

  private def material(position: Position, color: Color): Int =
    position.board.pieceMap.values.collect:
      case Piece(`color`, role) if role != King => pieceValue(role)
    .sum

  private def pieceValue(role: Role): Int =
    role match
      case Pawn            => 100
      case Knight | Bishop => 300
      case Rook            => 500
      case Queen           => 900
      case King            => 0

  private def isHomePawn(color: Color, square: Square): Boolean =
    color.fold(square.rank.value == 1, square.rank.value == 6)

  private def positionFromFen(fen: String): Option[Position] =
    Fen.read(variant.Standard, Fen.Full.clean(fen))

  private def colorFromKey(key: String): Option[Color] =
    key match
      case "white" => Some(Color.White)
      case "black" => Some(Color.Black)
      case _       => None

  private def colorKey(color: Color): String =
    if color.white then "white" else "black"

  private def countLabels(values: Vector[String]): Map[String, Int] =
    values.groupMapReduce(identity)(_ => 1)(_ + _).toVector.sortBy(_._1).toMap.withDefaultValue(0)

object LowerDiagnosticStandingAuditJson:

  def summaryJson(summary: LowerDiagnosticStandingAuditSummary): JsObject =
    Json.obj(
      "total" -> summary.total,
      "countsByTag" -> summary.countsByTag,
      "countsByPrimaryTag" -> summary.countsByPrimaryTag,
      "countsByLedgerClass" -> summary.countsByLedgerClass,
      "countsByPublicDisposition" -> summary.countsByPublicDisposition,
      "countsBySourceKind" -> summary.countsBySourceKind,
      "countsByTacticalSchema" -> summary.countsByTacticalSchema
    )

  def rowJson(row: LowerDiagnosticStandingAuditRow): JsObject =
    Json.obj(
      "rowId" -> row.rowId,
      "sourceKind" -> row.sourceKind,
      "sourceSchema" -> row.sourceSchema,
      "caseType" -> row.caseType,
      "expectation" -> row.expectation,
      "currentFen" -> row.currentFen,
      "tacticalSchemas" -> row.tacticalSchemas,
      "rootFacts" -> row.rootFacts.map(rootFactJson),
      "selectedClaimId" -> row.selectedClaimId,
      "selectedOwner" -> row.selectedOwner,
      "sideToMove" -> row.sideToMove,
      "tags" -> row.tags,
      "primaryTag" -> row.primaryTag,
      "ledgerClass" -> row.ledgerClass,
      "publicDisposition" -> row.publicDisposition,
      "nextChessAction" -> row.nextChessAction,
      "auditNote" -> row.auditNote
    )

  private def rootFactJson(root: LowerLayerDiagnostic.RootFact): JsObject =
    Json.obj(
      "id" -> root.id,
      "owner" -> root.owner,
      "anchor" -> root.anchor,
      "atom" -> root.atom
    )

object LowerDiagnosticStandingAuditRunner:

  private val defaultOutputDir = Paths.get("tmp/commentary-diagnostic/lower-layer/tracked/standing-audit")

  def main(args: Array[String]): Unit =
    val options = RunnerOptions.parse(args.toVector)
    val rows =
      options.inputPath match
        case Some(path) => LowerDiagnosticLargeCorpus.loadExternalRows(path, options.limit)
        case None       => LowerDiagnosticLargeCorpus.loadTrackedRows()
    val diagnostic = LowerDiagnosticReport.fromRows(rows)
    write(LowerDiagnosticStandingAuditReport.fromDiagnostic(diagnostic), options.outputDir)

  def write(report: LowerDiagnosticStandingAuditReport, outputDir: Path): Unit =
    Files.createDirectories(outputDir)
    Files.writeString(
      outputDir.resolve("standing-audit-summary.json"),
      Json.prettyPrint(LowerDiagnosticStandingAuditJson.summaryJson(report.summary)) + System.lineSeparator(),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      outputDir.resolve("standing-tactical-summary.json"),
      Json.prettyPrint(LowerDiagnosticStandingAuditJson.summaryJson(report.summary)) + System.lineSeparator(),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      outputDir.resolve("standing-audit-rows.jsonl"),
      report.rows.map(LowerDiagnosticStandingAuditJson.rowJson).map(Json.stringify).mkString("", System.lineSeparator(), System.lineSeparator()),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      outputDir.resolve("standing-tactical-ledger.jsonl"),
      report.rows.map(LowerDiagnosticStandingAuditJson.rowJson).map(Json.stringify).mkString("", System.lineSeparator(), System.lineSeparator()),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      outputDir.resolve("standing-tactical-samples.md"),
      samplesMarkdown(report),
      StandardCharsets.UTF_8
    )

  private def samplesMarkdown(report: LowerDiagnosticStandingAuditReport): String =
    val sections =
      report.rows
        .groupBy(_.ledgerClass)
        .toVector
        .sortBy((ledgerClass, rows) => (ledgerClass, -rows.size))
        .map: (ledgerClass, rows) =>
          val examples = rows.sortBy(_.rowId).take(5).map: row =>
            s"- `${row.rowId}` | `${row.sourceSchema.getOrElse("none")}` | `${row.publicDisposition}` | ${row.currentFen}"
          s"## $ledgerClass (${rows.size})${System.lineSeparator}${examples.mkString(System.lineSeparator)}"
    (Vector(
      "# Standing Tactical Samples",
      "",
      "Diagnostic-only exact-board samples. These rows are current-board facts unless a valid `beforeFen` and `playedMove` are supplied."
    ) ++ sections).mkString(System.lineSeparator, System.lineSeparator, System.lineSeparator)

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
