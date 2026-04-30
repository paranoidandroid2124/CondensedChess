package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import chess.{ Bishop, Board, Color, King, Knight, Pawn, Position, Queen, Role, Rook, Square }
import chess.format.Fen
import chess.variant
import play.api.libs.json.*

final case class LowerDiagnosticHighRiskTacticalAuditSummary(
    totalRows: Int,
    highRiskRows: Int,
    uniqueTransitions: Int,
    uniqueTransitionKeys: Int,
    multiSchemaTransitionKeys: Int,
    rowAmplification: Int,
    countsBySchema: Map[String, Int],
    countsByAuditDisposition: Map[String, Int],
    countsByLogicalDefect: Map[String, Int],
    countsBySourceVerificationState: Map[String, Int],
    countsByPublicClaimReadiness: Map[String, Int]
)

final case class LowerDiagnosticHighRiskTacticalAuditRow(
    schema: String,
    transitionKey: String,
    sourceRowIds: Vector[String],
    duplicateRows: Int,
    gameKey: Option[String],
    ply: Int,
    playedMove: Option[String],
    beforeFen: Option[String],
    currentFen: String,
    beforeFacts: Vector[String],
    afterFacts: Vector[String],
    createdFacts: Vector[String],
    removedFacts: Vector[String],
    touchedAnchorSquares: Vector[String],
    touchedStandingAnchorSquares: Vector[String],
    sourceVerificationState: String,
    sourceVerificationReason: String,
    loosePayloads: Vector[LowerDiagnosticLoosePayload],
    pinnedPayloads: Vector[LowerDiagnosticPinPayload],
    xrayPayloads: Vector[LowerDiagnosticXrayPayload],
    auditDisposition: String,
    logicalDefects: Vector[String],
    publicClaimReadiness: String,
    requiredNextEvidence: Vector[String],
    auditQuestion: String
)

final case class LowerDiagnosticLoosePayload(
    anchor: String,
    owner: String,
    role: String,
    pieceValue: Int,
    attackers: Vector[String],
    defenders: Vector[String],
    legalCaptures: Vector[String],
    immediateLegalCapture: Boolean,
    touchedByPlayedMove: Boolean,
    bestExchangeGainCp: Int,
    sideToMoveCanCapture: Boolean
)

final case class LowerDiagnosticPinPayload(
    pinnedSquare: String,
    pinnedOwner: String,
    pinnedRole: String,
    pinnerSquare: String,
    pinnerOwner: String,
    pinnerRole: String,
    anchorSquare: String,
    anchorOwner: String,
    anchorRole: String,
    pinKind: String,
    geometry: String,
    lineSquares: Vector[String],
    moveRelation: Vector[String],
    beforeLineState: String
)

final case class LowerDiagnosticXrayPayload(
    targetSquare: String,
    targetOwner: String,
    targetRole: String,
    attacker: String,
    sliderSquare: String,
    sliderRole: String,
    blockerSquare: String,
    blockerOwner: String,
    blockerRole: String,
    geometry: String,
    lineSquares: Vector[String],
    moveRelation: Vector[String],
    beforeLineState: String
)

final case class LowerDiagnosticHighRiskTacticalAuditReport(
    summary: LowerDiagnosticHighRiskTacticalAuditSummary,
    rows: Vector[LowerDiagnosticHighRiskTacticalAuditRow]
)

object LowerDiagnosticHighRiskTacticalAuditReport:

  private val HighRiskSchemas = Set("xray_target", "pinned_piece", "loose_piece")

  def fromRows(
      rows: Vector[LowerDiagnosticLargeCorpus.Row],
      pgnRoots: Vector[Path] = Vector.empty
  ): LowerDiagnosticHighRiskTacticalAuditReport =
    val diagnostic = LowerDiagnosticReport.fromRows(rows)
    val sourceByTransitionKey =
      LowerDiagnosticSourceVerificationReport
        .fromRows(rows, pgnRoots)
        .rows
        .map(row => row.transitionKey -> row)
        .toMap
    val keyedRows =
      diagnostic.traces.flatMap: traceRow =>
        schemasFor(traceRow).map(schema => (schema, transitionKey(traceRow.row)) -> traceRow)
    val auditRows =
      keyedRows
        .groupMap(_._1)(_._2)
        .toVector
        .map:
          case ((schema, _), grouped) =>
            auditGroup(schema, grouped, sourceByTransitionKey.get(transitionKey(grouped.minBy(_.row.id).row)))
        .sortBy(row => (row.schema, row.transitionKey))
    val transitionKeyCounts = auditRows.groupMapReduce(_.transitionKey)(_ => 1)(_ + _)
    LowerDiagnosticHighRiskTacticalAuditReport(
      summary = LowerDiagnosticHighRiskTacticalAuditSummary(
        totalRows = rows.size,
        highRiskRows = keyedRows.size,
        uniqueTransitions = auditRows.size,
        uniqueTransitionKeys = transitionKeyCounts.size,
        multiSchemaTransitionKeys = transitionKeyCounts.count(_._2 > 1),
        rowAmplification = keyedRows.size - auditRows.size,
        countsBySchema = countLabels(auditRows.map(_.schema)),
        countsByAuditDisposition = countLabels(auditRows.map(_.auditDisposition)),
        countsByLogicalDefect = countLabels(auditRows.flatMap(_.logicalDefects)),
        countsBySourceVerificationState = countLabels(auditRows.map(_.sourceVerificationState)),
        countsByPublicClaimReadiness = countLabels(auditRows.map(_.publicClaimReadiness))
      ),
      rows = auditRows
    )

  private def schemasFor(row: LowerDiagnosticTraceRow): Vector[String] =
    row.tacticalSchemas.filter(HighRiskSchemas).distinct.sorted

  private def auditGroup(
      schema: String,
      rows: Vector[LowerDiagnosticTraceRow],
      sourceVerification: Option[LowerDiagnosticSourceVerificationRow]
  ): LowerDiagnosticHighRiskTacticalAuditRow =
    val first = rows.minBy(_.row.id)
    val afterFacts = facts(first.trace, schema)
    val beforeFacts =
      first.row.input.beforeFen.toVector.flatMap: beforeFen =>
        val beforeTrace = LowerLayerDiagnostic.trace(
          first.row.input.copy(currentFen = beforeFen, beforeFen = None, playedMove = None)
        )
        facts(beforeTrace, schema)
    val created = afterFacts.diff(beforeFacts)
    val removed = beforeFacts.diff(afterFacts)
    val touched = touchedAnchorSquares(first.row.input.playedMove, created)
    val touchedStanding = touchedAnchorSquares(first.row.input.playedMove, afterFacts.diff(created))
    val loosePayloads = if schema == "loose_piece" then loosePayload(first, afterFacts) else Vector.empty
    val pinnedPayloads = if schema == "pinned_piece" then pinPayload(first, afterFacts) else Vector.empty
    val xrayPayloads = if schema == "xray_target" then xrayPayload(first, afterFacts) else Vector.empty
    val disposition = auditDisposition(schema, first, afterFacts, created, touched)
    val sourceState = sourceVerification.map(_.sourceState).getOrElse("missing_source_verification_row")
    val sourceReason = sourceVerification.map(_.reason).getOrElse("source verification was not run for this transition key")
    val defects = logicalDefects(schema, first, created, touched, sourceState, loosePayloads, pinnedPayloads, xrayPayloads)
    LowerDiagnosticHighRiskTacticalAuditRow(
      schema = schema,
      transitionKey = transitionKey(first.row),
      sourceRowIds = rows.map(_.row.id).distinct.sorted,
      duplicateRows = rows.size,
      gameKey = first.row.metadata.get("gameKey"),
      ply = first.row.input.ply,
      playedMove = first.row.input.playedMove,
      beforeFen = first.row.input.beforeFen,
      currentFen = first.row.input.currentFen,
      beforeFacts = beforeFacts.sorted,
      afterFacts = afterFacts.sorted,
      createdFacts = created.sorted,
      removedFacts = removed.sorted,
      touchedAnchorSquares = touched.sorted,
      touchedStandingAnchorSquares = touchedStanding.sorted,
      sourceVerificationState = sourceState,
      sourceVerificationReason = sourceReason,
      loosePayloads = loosePayloads,
      pinnedPayloads = pinnedPayloads,
      xrayPayloads = xrayPayloads,
      auditDisposition = disposition,
      logicalDefects = defects,
      publicClaimReadiness = "not_ready_for_public_claim",
      requiredNextEvidence = requiredNextEvidence(schema),
      auditQuestion = auditQuestion(schema, disposition)
    )

  private def facts(trace: LowerLayerDiagnostic.Trace, schema: String): Vector[String] =
    trace.extraction.rootFacts.filter(_.id == schema).map(_.atom).distinct

  private def auditDisposition(
      schema: String,
      row: LowerDiagnosticTraceRow,
      afterFacts: Vector[String],
      createdFacts: Vector[String],
      touchedAnchors: Vector[String]
  ): String =
    if afterFacts.isEmpty then s"reject_no_after_$schema"
    else if row.row.input.beforeFen.isEmpty || row.row.input.playedMove.isEmpty then s"reject_current_board_only_$schema"
    else if createdFacts.isEmpty then s"reject_preexisting_or_standing_$schema"
    else
      schema match
        case "xray_target"  => "candidate_created_xray_requires_geometry_audit"
        case "pinned_piece" => "candidate_created_pinned_piece_requires_pin_geometry_audit"
        case "loose_piece" if touchedAnchors.nonEmpty =>
          "candidate_created_loose_piece_requires_capture_audit"
        case "loose_piece" =>
          "candidate_created_loose_piece_requires_causality_audit"
        case _ => s"candidate_created_${schema}_requires_audit"

  private def logicalDefects(
      schema: String,
      row: LowerDiagnosticTraceRow,
      createdFacts: Vector[String],
      touchedAnchors: Vector[String],
      sourceVerificationState: String,
      loosePayloads: Vector[LowerDiagnosticLoosePayload],
      pinnedPayloads: Vector[LowerDiagnosticPinPayload],
      xrayPayloads: Vector[LowerDiagnosticXrayPayload]
  ): Vector[String] =
    (
      Vector(
        Option.when(row.row.input.beforeFen.isEmpty || row.row.input.playedMove.isEmpty)("missing_transition_identity"),
        Option.when(sourceVerificationState != "verified")("source_pgn_unverified"),
        Option.when(sourceVerificationState != "verified")(s"source_pgn_$sourceVerificationState"),
        Option.when(createdFacts.isEmpty)("preexisting_or_standing_tactical_fact"),
        Option.when(createdFacts.nonEmpty && touchedAnchors.isEmpty)(s"move_does_not_touch_${schema}_anchor")
      ).flatten ++ schemaSpecificDefects(schema, createdFacts, loosePayloads, pinnedPayloads, xrayPayloads)
    ).distinct

  private def schemaSpecificDefects(
      schema: String,
      createdFacts: Vector[String],
      loosePayloads: Vector[LowerDiagnosticLoosePayload],
      pinnedPayloads: Vector[LowerDiagnosticPinPayload],
      xrayPayloads: Vector[LowerDiagnosticXrayPayload]
  ): Vector[String] =
    schema match
      case "xray_target" =>
        Vector[Option[String]](
          Option.when(xrayPayloads.isEmpty)("xray_root_lacks_slider_blocker_identity"),
          Option.when(createdFacts.nonEmpty)("created_root_does_not_prove_move_causality")
        ).flatten
      case "pinned_piece" =>
        Vector[Option[String]](
          Option.when(pinnedPayloads.isEmpty)("pinned_root_lacks_pinner_anchor_identity"),
          Some("pin_requires_absolute_relative_and_value_gate"),
          Option.when(createdFacts.nonEmpty)("created_pin_does_not_prove_move_causality")
        ).flatten
      case "loose_piece" =>
        Vector[Option[String]](
          Some("loose_root_does_not_prove_immediate_capture"),
          Option.when(loosePayloads.exists(_.bestExchangeGainCp <= 0))("loose_exchange_payload_not_positive"),
          Some("material_or_mate_context_unchecked"),
          Option.when(createdFacts.nonEmpty)("created_loose_does_not_prove_public_tactic")
        ).flatten
      case _ => Vector("high_risk_root_requires_schema_specific_payload")

  private def touchedAnchorSquares(playedMove: Option[String], facts: Vector[String]): Vector[String] =
    val touched = playedMove.toVector.flatMap(move => Vector(move.take(2), move.slice(2, 4)))
    facts.flatMap(anchorSquare).filter(touched.contains).distinct

  private def anchorSquare(atom: String): Option[String] =
    atom.dropWhile(_ != '(').drop(1).takeWhile(_ != ')').split(',').toVector.lift(1).filter(_.nonEmpty)

  private def loosePayload(row: LowerDiagnosticTraceRow, createdFacts: Vector[String]): Vector[LowerDiagnosticLoosePayload] =
    positionFromFen(row.row.input.currentFen).toVector.flatMap: position =>
      createdFacts.flatMap: atom =>
        for
          ownerKey <- ownerKey(atom)
          owner <- colorFromKey(ownerKey)
          anchorKey <- anchorSquare(atom)
          anchor <- Square.fromKey(anchorKey)
          piece <- position.pieceAt(anchor)
        yield
          val attackers = attackerSquares(position, anchor, !owner)
          val defenders = defenderSquares(position, anchor, owner)
          val legalCaptures = legalMoves(position, !owner)
            .filter(move => move.dest == anchor && move.captures)
            .map(move => s"${move.orig.key}${move.dest.key}")
            .sorted
          LowerDiagnosticLoosePayload(
            anchor = anchor.key,
            owner = ownerKey,
            role = roleKey(piece.role),
            pieceValue = pieceValue(piece.role),
            attackers = attackers,
            defenders = defenders,
            legalCaptures = legalCaptures,
            immediateLegalCapture = position.color == !owner && legalCaptures.nonEmpty,
            touchedByPlayedMove = row.row.input.playedMove.exists(move => move.take(2) == anchor.key || move.slice(2, 4) == anchor.key),
            bestExchangeGainCp = opponentBestExchangeNet(position.board, anchor, !owner),
            sideToMoveCanCapture = position.color == !owner && legalCaptures.nonEmpty
          )

  private def pinPayload(row: LowerDiagnosticTraceRow, createdFacts: Vector[String]): Vector[LowerDiagnosticPinPayload] =
    positionFromFen(row.row.input.currentFen).toVector.flatMap: position =>
      createdFacts.flatMap: atom =>
        for
          ownerKey <- ownerKey(atom)
          owner <- colorFromKey(ownerKey)
          pinnedKey <- anchorSquare(atom)
          pinned <- Square.fromKey(pinnedKey)
          pinnedPiece <- position.pieceAt(pinned)
          if pinnedPiece.color == owner
        yield pinLines(position, owner, pinned, pinnedPiece, row.row.input.beforeFen, row.row.input.playedMove)
      .flatten

  private def pinLines(
      position: Position,
      pinnedOwner: Color,
      pinned: Square,
      pinnedPiece: chess.Piece,
      beforeFen: Option[String],
      playedMove: Option[String]
  ): Vector[LowerDiagnosticPinPayload] =
    val anchors =
      position.board.kingPosOf(pinnedOwner).toVector.map(_ -> ("absolute", King)) ++
        Square.all.toVector.flatMap: anchor =>
          position.pieceAt(anchor).collect:
            case piece if piece.color == pinnedOwner && piece.role != King && pieceValue(piece.role) > pieceValue(pinnedPiece.role) =>
              anchor -> ("relative", piece.role)
    sliderSquaresOf(position, !pinnedOwner).flatMap: pinner =>
      position.pieceAt(pinner).toVector.flatMap: pinnerPiece =>
        anchors.flatMap:
          case (anchor, (pinKind, anchorRole)) =>
            val blockers = blockersBetween(position.board, pinner, anchor)
            Option
              .when(
                pinnerPiece.color == !pinnedOwner &&
                  sliderCanUseLine(pinnerPiece.role, pinner, anchor) &&
                  blockers == Vector(pinned)
              )(
                LowerDiagnosticPinPayload(
                  pinnedSquare = pinned.key,
                  pinnedOwner = colorKey(pinnedOwner),
                  pinnedRole = roleKey(pinnedPiece.role),
                  pinnerSquare = pinner.key,
                  pinnerOwner = colorKey(!pinnedOwner),
                  pinnerRole = roleKey(pinnerPiece.role),
                  anchorSquare = anchor.key,
                  anchorOwner = colorKey(pinnedOwner),
                  anchorRole = roleKey(anchorRole),
                  pinKind = pinKind,
                  geometry = geometryBetween(pinner, anchor),
                  lineSquares = lineSquaresBetween(pinner, anchor),
                  moveRelation = moveRelation(playedMove, Map("pinner" -> pinner, "pinned" -> pinned, "anchor" -> anchor), lineSquaresBetween(pinner, anchor)),
                  beforeLineState = beforePinLineState(beforeFen, pinnedOwner, pinned, pinner, anchor)
                )
              )

  private def xrayPayload(row: LowerDiagnosticTraceRow, createdFacts: Vector[String]): Vector[LowerDiagnosticXrayPayload] =
    positionFromFen(row.row.input.currentFen).toVector.flatMap: position =>
      createdFacts.flatMap: atom =>
        for
          attackerKey <- ownerKey(atom)
          attacker <- colorFromKey(attackerKey)
          targetKey <- anchorSquare(atom)
          target <- Square.fromKey(targetKey)
          targetPiece <- position.pieceAt(target)
          if targetPiece.color == !attacker
        yield xrayLines(position, attacker, target, targetPiece, row.row.input.beforeFen, row.row.input.playedMove)
      .flatten

  private def xrayLines(
      position: Position,
      attacker: Color,
      target: Square,
      targetPiece: chess.Piece,
      beforeFen: Option[String],
      playedMove: Option[String]
  ): Vector[LowerDiagnosticXrayPayload] =
    sliderSquaresOf(position, attacker).flatMap: slider =>
      position.pieceAt(slider).toVector.flatMap: sliderPiece =>
        val blockers = blockersBetween(position.board, slider, target)
        blockers match
          case Vector(blocker) if sliderCanUseLine(sliderPiece.role, slider, target) =>
            position.pieceAt(blocker).toVector.map: blockerPiece =>
              LowerDiagnosticXrayPayload(
                targetSquare = target.key,
                targetOwner = colorKey(!attacker),
                targetRole = roleKey(targetPiece.role),
                attacker = colorKey(attacker),
                sliderSquare = slider.key,
                sliderRole = roleKey(sliderPiece.role),
                blockerSquare = blocker.key,
                blockerOwner = colorKey(blockerPiece.color),
                blockerRole = roleKey(blockerPiece.role),
                geometry = geometryBetween(slider, target),
                lineSquares = lineSquaresBetween(slider, target),
                moveRelation = moveRelation(playedMove, Map("slider" -> slider, "blocker" -> blocker, "target" -> target), lineSquaresBetween(slider, target)),
                beforeLineState = beforeXrayLineState(beforeFen, attacker, slider, target)
              )
          case _ => Vector.empty

  private def ownerKey(atom: String): Option[String] =
    atom.dropWhile(_ != '(').drop(1).takeWhile(_ != ')').split(',').toVector.headOption.filter(_.nonEmpty)

  private def attackerSquares(position: Position, square: Square, color: Color): Vector[String] =
    Square.all
      .filter(origin => position.withColor(color).pieceAt(origin).exists(_.color == color))
      .filter(origin => position.withColor(color).generateMovesAt(origin).exists(_.dest == square))
      .map(_.key)
      .toVector
      .sorted

  private def defenderSquares(position: Position, square: Square, color: Color): Vector[String] =
    position.board
      .attackers(square, color)
      .filter(origin => position.pieceAt(origin).exists(_.color == color))
      .map(_.key)
      .toVector
      .sorted

  private def sliderSquaresOf(position: Position, color: Color): Vector[Square] =
    Square.all
      .filter(square => position.pieceAt(square).exists(piece => piece.color == color && Set(Bishop, Rook, Queen).contains(piece.role)))
      .toVector

  private def sliderCanUseLine(role: Role, from: Square, to: Square): Boolean =
    role match
      case Bishop => from.onSameDiagonal(to)
      case Rook   => from.onSameLine(to)
      case Queen  => from.onSameDiagonal(to) || from.onSameLine(to)
      case _      => false

  private def blockersBetween(board: Board, from: Square, to: Square): Vector[Square] =
    (chess.Bitboard.between(from, to) & board.occupied).squares.toVector

  private def lineSquaresBetween(from: Square, to: Square): Vector[String] =
    chess.Bitboard.between(from, to).squares.toVector.map(_.key).sorted

  private def geometryBetween(from: Square, to: Square): String =
    if from.file == to.file then "file"
    else if from.rank == to.rank then "rank"
    else if from.onSameDiagonal(to) then "diagonal"
    else "none"

  private def moveRelation(playedMove: Option[String], namedSquares: Map[String, Square], lineSquares: Vector[String]): Vector[String] =
    val origin = playedMove.map(_.take(2))
    val dest = playedMove.map(_.slice(2, 4))
    (
      namedSquares.toVector.flatMap: (label, square) =>
        Vector(
          Option.when(origin.contains(square.key))(s"move_origin_$label"),
          Option.when(dest.contains(square.key))(s"move_dest_$label")
        ).flatten
      ++ Vector(
        Option.when(origin.exists(lineSquares.contains))("move_origin_on_line"),
        Option.when(dest.exists(lineSquares.contains))("move_dest_on_line")
      ).flatten
    ).distinct.sorted

  private def beforePinLineState(
      beforeFen: Option[String],
      pinnedOwner: Color,
      pinned: Square,
      pinner: Square,
      anchor: Square
  ): String =
    beforeFen.flatMap(positionFromFen) match
      case None => "missing_before_fen"
      case Some(position) =>
        position.pieceAt(pinner) match
          case Some(piece) if piece.color == !pinnedOwner && sliderCanUseLine(piece.role, pinner, anchor) =>
            val blockers = blockersBetween(position.board, pinner, anchor)
            if blockers == Vector(pinned) then "before_matching_pin_line_present"
            else if blockers.contains(pinned) then "before_pinned_piece_on_line_with_extra_blockers"
            else "before_no_matching_pin_line"
          case _ => "before_pinner_absent_or_not_aligned"

  private def beforeXrayLineState(beforeFen: Option[String], attacker: Color, slider: Square, target: Square): String =
    beforeFen.flatMap(positionFromFen) match
      case None => "missing_before_fen"
      case Some(position) =>
        if position.pieceAt(target).forall(_.color == attacker) then "before_target_absent_or_wrong_owner"
        else if position.pieceAt(slider).forall(piece => piece.color != attacker || !sliderCanUseLine(piece.role, slider, target)) then
          "before_slider_absent_or_not_aligned"
        else
          val blockers = blockersBetween(position.board, slider, target)
          if blockers.size == 1 then "before_matching_xray_line_present"
          else s"before_blocker_count_${blockers.size}"

  private def legalMoves(position: Position, color: Color): Vector[chess.Move] =
    val colored = position.withColor(color)
    Square.all
      .filter(square => colored.pieceAt(square).exists(_.color == color))
      .flatMap(colored.generateMovesAt)
      .toVector

  private def positionFromFen(fen: String): Option[Position] =
    Fen.read(variant.Standard, Fen.Full.clean(fen))

  private def colorFromKey(key: String): Option[Color] =
    key match
      case "white" => Some(Color.White)
      case "black" => Some(Color.Black)
      case _       => None

  private def colorKey(color: Color): String =
    color.fold("white", "black")

  private def roleKey(role: Role): String =
    role match
      case Pawn   => "pawn"
      case Knight => "knight"
      case Bishop => "bishop"
      case Rook   => "rook"
      case Queen  => "queen"
      case King   => "king"

  private def pieceValue(role: Role): Int =
    role match
      case Pawn            => 100
      case Knight | Bishop => 300
      case Rook            => 500
      case Queen           => 900
      case King            => 0

  private def opponentBestExchangeNet(boardState: Board, square: Square, attacker: Color): Int =
    boardState.pieceAt(square).fold(0): occupant =>
      legalAttackers(boardState, square, attacker)
        .map: origin =>
          val afterCapture = boardState.taking(origin, square).get
          pieceValue(occupant.role) - opponentBestExchangeNet(afterCapture, square, !attacker)
        .foldLeft(0)(math.max)

  private def legalAttackers(boardState: Board, square: Square, attacker: Color): Vector[Square] =
    val attackerPosition = Position(boardState, variant.Standard, attacker)
    boardState
      .attackers(square, attacker)
      .filter(origin => attackerPosition.generateMovesAt(origin).exists(_.dest == square))
      .toVector

  private def requiredNextEvidence(schema: String): Vector[String] =
    schema match
      case "xray_target" =>
        Vector(
          "source PGN verification",
          "slider identity",
          "blocker identity",
          "target value and owner",
          "before/after line geometry",
          "moved or captured piece relation",
          "standing/pre-existing anti-case"
        )
      case "pinned_piece" =>
        Vector(
          "source PGN verification",
          "pinner identity",
          "pinned piece identity",
          "king or higher-value anchor behind pinned piece",
          "absolute vs relative pin classification",
          "move-created or move-released line proof",
          "legal-move/usefulness gate"
        )
      case "loose_piece" =>
        Vector(
          "source PGN verification",
          "attacker identity",
          "defender identity",
          "local exchange payload",
          "immediate legal capture separation",
          "move-created loose proof",
          "material/mate context suppression"
        )
      case _ => Vector("source PGN verification", "schema-specific root payload")

  private def auditQuestion(schema: String, disposition: String): String =
    if disposition.contains("preexisting_or_standing") then
      s"This $schema fact already existed before the move; keep it out of move-causal public claims."
    else if disposition.contains("current_board_only") then
      s"This $schema fact has no transition identity; keep it as current-board support only."
    else
      schema match
        case "xray_target" =>
          "Root appeared after the move, but slider/blocker identity is missing; verify exact geometry before admission."
        case "pinned_piece" =>
          "Root appeared after the move, but pinner/anchor identity and pin strength are missing; verify pin geometry before admission."
        case "loose_piece" =>
          "Root appeared after the move, but loose does not imply immediate capture; verify exchange and context before admission."
        case _ =>
          "Verify schema-specific payload before any public move-causal admission."

  private def transitionKey(row: LowerDiagnosticLargeCorpus.Row): String =
    val gameKey = row.metadata.getOrElse("gameKey", row.metadata.getOrElse("sourceId", row.id))
    val move = row.input.playedMove.orElse(row.metadata.get("playedUci")).getOrElse("none")
    s"$gameKey|ply:${row.input.ply}|move:$move"

  private def countLabels(values: Vector[String]): Map[String, Int] =
    values.groupMapReduce(identity)(_ => 1)(_ + _).toVector.sortBy(_._1).toMap.withDefaultValue(0)

object LowerDiagnosticHighRiskTacticalAuditJson:

  def summaryJson(summary: LowerDiagnosticHighRiskTacticalAuditSummary): JsObject =
    Json.obj(
      "totalRows" -> summary.totalRows,
      "highRiskRows" -> summary.highRiskRows,
      "uniqueTransitions" -> summary.uniqueTransitions,
      "rowAmplification" -> summary.rowAmplification,
      "countsBySchema" -> summary.countsBySchema,
      "uniqueTransitionKeys" -> summary.uniqueTransitionKeys,
      "multiSchemaTransitionKeys" -> summary.multiSchemaTransitionKeys,
      "countsByAuditDisposition" -> summary.countsByAuditDisposition,
      "countsByLogicalDefect" -> summary.countsByLogicalDefect,
      "countsBySourceVerificationState" -> summary.countsBySourceVerificationState,
      "countsByPublicClaimReadiness" -> summary.countsByPublicClaimReadiness
    )

  def rowJson(row: LowerDiagnosticHighRiskTacticalAuditRow): JsObject =
    Json.obj(
      "schema" -> row.schema,
      "transitionKey" -> row.transitionKey,
      "sourceRowIds" -> row.sourceRowIds,
      "duplicateRows" -> row.duplicateRows,
      "gameKey" -> row.gameKey,
      "ply" -> row.ply,
      "playedMove" -> row.playedMove,
      "beforeFen" -> row.beforeFen,
      "currentFen" -> row.currentFen,
      "beforeFacts" -> row.beforeFacts,
      "afterFacts" -> row.afterFacts,
      "createdFacts" -> row.createdFacts,
      "removedFacts" -> row.removedFacts,
      "touchedAnchorSquares" -> row.touchedAnchorSquares,
      "touchedStandingAnchorSquares" -> row.touchedStandingAnchorSquares,
      "sourceVerificationState" -> row.sourceVerificationState,
      "sourceVerificationReason" -> row.sourceVerificationReason,
      "loosePayloads" -> row.loosePayloads.map(loosePayloadJson),
      "pinnedPayloads" -> row.pinnedPayloads.map(pinPayloadJson),
      "xrayPayloads" -> row.xrayPayloads.map(xrayPayloadJson),
      "auditDisposition" -> row.auditDisposition,
      "logicalDefects" -> row.logicalDefects,
      "publicClaimReadiness" -> row.publicClaimReadiness,
      "requiredNextEvidence" -> row.requiredNextEvidence,
      "auditQuestion" -> row.auditQuestion
    )

  private def loosePayloadJson(payload: LowerDiagnosticLoosePayload): JsObject =
    Json.obj(
      "anchor" -> payload.anchor,
      "owner" -> payload.owner,
      "role" -> payload.role,
      "pieceValue" -> payload.pieceValue,
      "attackers" -> payload.attackers,
      "defenders" -> payload.defenders,
      "legalCaptures" -> payload.legalCaptures,
      "immediateLegalCapture" -> payload.immediateLegalCapture,
      "touchedByPlayedMove" -> payload.touchedByPlayedMove,
      "bestExchangeGainCp" -> payload.bestExchangeGainCp,
      "sideToMoveCanCapture" -> payload.sideToMoveCanCapture
    )

  private def pinPayloadJson(payload: LowerDiagnosticPinPayload): JsObject =
    Json.obj(
      "pinnedSquare" -> payload.pinnedSquare,
      "pinnedOwner" -> payload.pinnedOwner,
      "pinnedRole" -> payload.pinnedRole,
      "pinnerSquare" -> payload.pinnerSquare,
      "pinnerOwner" -> payload.pinnerOwner,
      "pinnerRole" -> payload.pinnerRole,
      "anchorSquare" -> payload.anchorSquare,
      "anchorOwner" -> payload.anchorOwner,
      "anchorRole" -> payload.anchorRole,
      "pinKind" -> payload.pinKind,
      "geometry" -> payload.geometry,
      "lineSquares" -> payload.lineSquares,
      "moveRelation" -> payload.moveRelation,
      "beforeLineState" -> payload.beforeLineState
    )

  private def xrayPayloadJson(payload: LowerDiagnosticXrayPayload): JsObject =
    Json.obj(
      "targetSquare" -> payload.targetSquare,
      "targetOwner" -> payload.targetOwner,
      "targetRole" -> payload.targetRole,
      "attacker" -> payload.attacker,
      "sliderSquare" -> payload.sliderSquare,
      "sliderRole" -> payload.sliderRole,
      "blockerSquare" -> payload.blockerSquare,
      "blockerOwner" -> payload.blockerOwner,
      "blockerRole" -> payload.blockerRole,
      "geometry" -> payload.geometry,
      "lineSquares" -> payload.lineSquares,
      "moveRelation" -> payload.moveRelation,
      "beforeLineState" -> payload.beforeLineState
    )

object LowerDiagnosticHighRiskTacticalAuditRunner:

  private val defaultOutputDir = Paths.get("tmp/commentary-diagnostic/lower-layer/high-risk-tactical-audit")

  def main(args: Array[String]): Unit =
    val options = RunnerOptions.parse(args.toVector)
    val rows =
      options.inputPath match
        case Some(path) => LowerDiagnosticLargeCorpus.loadExternalRows(path, options.limit)
        case None       => LowerDiagnosticLargeCorpus.loadTrackedRows()
    write(LowerDiagnosticHighRiskTacticalAuditReport.fromRows(rows, options.pgnRoots), options.outputDir)

  def write(report: LowerDiagnosticHighRiskTacticalAuditReport, outputDir: Path): Unit =
    Files.createDirectories(outputDir)
    Files.writeString(
      outputDir.resolve("high-risk-tactical-audit-summary.json"),
      Json.prettyPrint(LowerDiagnosticHighRiskTacticalAuditJson.summaryJson(report.summary)) + System.lineSeparator(),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      outputDir.resolve("high-risk-tactical-audit-transitions.jsonl"),
      report.rows.map(LowerDiagnosticHighRiskTacticalAuditJson.rowJson).map(Json.stringify).mkString("", System.lineSeparator(), System.lineSeparator()),
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
