package lila.commentary.diagnostic

import chess.Pawn
import chess.format.Fen

import lila.commentary.CommentaryCore
import lila.commentary.claim.{ EvidenceClaimHandoff, EvidenceClaimProducer }
import lila.commentary.delta.StrategicDeltaExtraction
import lila.commentary.root.RootPositionSupport
import lila.commentary.selection.*
import lila.commentary.strategic.StrategicObjectExtraction

object LowerLayerDiagnostic:

  final case class Input(
      id: String,
      currentFen: String,
      beforeFen: Option[String],
      playedMove: Option[String],
      nodeId: String,
      ply: Int
  )

  final case class StageStatus(status: String, reason: Option[String] = None)

  final case class InputIdentity(
      currentFen: String,
      beforeFen: Option[String],
      playedMove: Option[String],
      nodeId: String,
      ply: Int
  )

  final case class RootFact(
      id: String,
      owner: Option[String],
      anchor: Option[String],
      atom: String
  )

  final case class ExtractionTrace(rootFacts: Vector[RootFact])

  final case class ClaimSummary(
      id: String,
      layer: String,
      status: String,
      owner: Option[String],
      beneficiary: Option[String],
      defender: Option[String],
      anchor: Option[String],
      route: Option[String],
      scope: Option[String],
      evidenceIds: Vector[String],
      lowerCarrierIds: Vector[String]
  )

  final case class ClaimsTrace(produced: Vector[ClaimSummary])

  final case class SelectionTrace(
      lead: Option[ClaimSummary],
      support: Vector[ClaimSummary],
      context: Vector[ClaimSummary],
      suppressed: Vector[(ClaimSummary, Vector[String])]
  )

  final case class Trace(
      id: String,
      identity: InputIdentity,
      input: StageStatus,
      transition: StageStatus,
      extraction: ExtractionTrace,
      claims: ClaimsTrace,
      selection: SelectionTrace,
      tacticalVerdict: String,
      preRendererVerdict: String,
      breaks: Vector[String]
  )

  private val TacticalRootIds: Set[String] =
    Set("loose_piece", "pinned_piece", "overloaded_piece", "trapped_piece", "xray_target")

  private val MoveLocalTacticalEvidenceIds: Set[String] =
    TacticalRootIds ++ Set("moved_piece_left_loose_transition", "royal_fork")

  private val GenericTransitionRoutes: Set[String] =
    Set("last_move_transition", "pawn_structure_transition")

  def trace(input: Input, handoff: EvidenceClaimHandoff = EvidenceClaimHandoff.empty): Trace =
    val currentFen = Fen.Full.clean(input.currentFen): Fen.Full
    CommentaryCore.extractStrategicObjectsFailClosed(currentFen) match
      case Left(reason) =>
        Trace(
          id = input.id,
          identity = identity(input),
          input = StageStatus("invalid", Some(reason)),
          transition = StageStatus("not_run"),
          extraction = ExtractionTrace(Vector.empty),
          claims = ClaimsTrace(Vector.empty),
          selection = SelectionTrace(None, Vector.empty, Vector.empty, Vector.empty),
          tacticalVerdict = "not_run",
          preRendererVerdict = "input_invalid",
          breaks = Vector("input:invalid_current_fen")
        )
      case Right(currentExtraction) =>
        transition(input, currentExtraction) match
          case Left(status) =>
            Trace(
              id = input.id,
              identity = identity(input),
              input = StageStatus("valid"),
              transition = status,
              extraction = ExtractionTrace(rootFacts(currentExtraction)),
              claims = ClaimsTrace(Vector.empty),
              selection = SelectionTrace(None, Vector.empty, Vector.empty, Vector.empty),
              tacticalVerdict = tacticalVerdict(rootFacts(currentExtraction), Vector.empty),
              preRendererVerdict = "transition_invalid",
              breaks = Vector(s"transition:${status.status}")
            )
          case Right((status, deltaExtraction)) =>
            val producedClaims = EvidenceClaimProducer.produce(currentExtraction, deltaExtraction, handoff)
            val outline = ClaimSelector.select(producedClaims)
            val extraction = ExtractionTrace(rootFacts(currentExtraction))
            val summaries = producedClaims.map(claimSummary)
            val selection = SelectionTrace(
              lead = outline.lead.map(selected => claimSummary(selected.claim)),
              support = outline.support.map(selected => claimSummary(selected.claim)),
              context = outline.context.map(selected => claimSummary(selected.claim)),
              suppressed = outline.suppressedClaims.map(suppressed =>
                claimSummary(suppressed.claim) -> suppressed.reasons.map(_.key)
              )
            )
            val breaks = diagnosticBreaks(extraction.rootFacts, producedClaims, selection.lead)
            Trace(
              id = input.id,
              identity = identity(input),
              input = StageStatus("valid"),
              transition = status,
              extraction = extraction,
              claims = ClaimsTrace(summaries),
              selection = selection,
              tacticalVerdict = tacticalVerdict(extraction.rootFacts, producedClaims),
              preRendererVerdict = preRendererVerdict(selection.lead),
              breaks = breaks
            )

  private def transition(
      input: Input,
      currentExtraction: StrategicObjectExtraction
  ): Either[StageStatus, (StageStatus, Option[StrategicDeltaExtraction])] =
    (input.beforeFen, input.playedMove) match
      case (None, None) =>
        Right(StageStatus("none") -> None)
      case (Some(beforeFen), Some(playedMove)) =>
        CommentaryCore.extractStrategicDeltasFromFensFailClosed(beforeFen, playedMove, input.currentFen) match
          case Left(reason) =>
            Left(StageStatus("invalid", Some(reason)))
          case Right(delta) if delta.after.rootState != currentExtraction.rootState =>
            Left(StageStatus("after_mismatch", Some("delta after-state differs from current extraction")))
          case Right(delta) =>
            validateTransitionClocks(beforeFen, input.currentFen, delta) match
              case Left(reason) => Left(StageStatus("invalid_clock", Some(reason)))
              case Right(_)     => Right(StageStatus("valid") -> Some(delta))
      case _ =>
        Left(StageStatus("missing_pair", Some("beforeFen and playedMove must be supplied together")))

  private def validateTransitionClocks(
      beforeFen: String,
      currentFen: String,
      delta: StrategicDeltaExtraction
  ): Either[String, Unit] =
    for
      beforeClock <- fenClock(Fen.Full.clean(beforeFen))
      afterClock <- fenClock(Fen.Full.clean(currentFen))
      beforePosition <- RootPositionSupport.exactPosition(delta.before.rootState)
      exactMove <- beforePosition.move(delta.playedMove).left.map(_.toString)
      movingPiece <- beforePosition.board.pieceAt(delta.playedMove.orig).toRight("missing transition moving piece")
      expectedHalfMove = if movingPiece.role == Pawn || exactMove.capture.nonEmpty then 0 else beforeClock.halfMove + 1
      expectedFullMove = if beforeClock.sideToMove == "b" then beforeClock.fullMove + 1 else beforeClock.fullMove
      _ <- Either.cond(
        afterClock.halfMove == expectedHalfMove && afterClock.fullMove == expectedFullMove,
        (),
        "transition full-FEN clock mismatch"
      )
    yield ()

  private final case class FenClock(sideToMove: String, halfMove: Int, fullMove: Int)

  private def fenClock(fen: Fen.Full): Either[String, FenClock] =
    fen.value.split("\\s+").toList match
      case _ :: side :: _ :: _ :: halfMove :: fullMove :: Nil =>
        for
          half <- halfMove.toIntOption.toRight("invalid FEN halfmove clock")
          full <- fullMove.toIntOption.toRight("invalid FEN fullmove number")
        yield FenClock(side, half, full)
      case _ => Left("full FEN must include side, castling, en-passant, halfmove, and fullmove fields")

  private def identity(input: Input): InputIdentity =
    InputIdentity(
      currentFen = input.currentFen,
      beforeFen = input.beforeFen,
      playedMove = input.playedMove,
      nodeId = input.nodeId,
      ply = input.ply
    )

  private def rootFacts(currentExtraction: StrategicObjectExtraction): Vector[RootFact] =
    currentExtraction.rootState.activeAtomIds.flatMap: atom =>
      val id = atom.takeWhile(_ != '(').takeWhile(_ != ':')
      Option.when(TacticalRootIds.contains(id)):
        val args =
          atom.dropWhile(_ != '(').drop(1).takeWhile(_ != ')').split(',').toVector
        RootFact(
          id = id,
          owner = args.headOption.filter(_.nonEmpty),
          anchor = args.lift(1).filter(_.nonEmpty),
          atom = atom
        )

  private def claimSummary(claim: CommentaryClaim): ClaimSummary =
    ClaimSummary(
      id = claim.id,
      layer = claim.layer.key,
      status = claim.status.key,
      owner = claim.owner,
      beneficiary = claim.beneficiary,
      defender = claim.defender,
      anchor = claim.anchor,
      route = claim.route,
      scope = claim.scope,
      evidenceIds = claim.evidenceRefs.map(_.id),
      lowerCarrierIds = claim.lowerCarrierRefs.map(_.id)
    )

  private def diagnosticBreaks(
      roots: Vector[RootFact],
      claims: Vector[CommentaryClaim],
      lead: Option[ClaimSummary]
  ): Vector[String] =
    Vector(
      Option.when(roots.isEmpty)("extraction:no_tactical_roots"),
      Option.when(claims.isEmpty)("claim:no_claims"),
      Option.when(lead.isEmpty)("selection:no_lead"),
      Option.when(lead.exists(summary => !summaryHasMoveLocalTacticalEvidence(summary)))("selection:no_move_local_tactical_lead"),
      Option.when(roots.nonEmpty && !hasMoveLocalTacticalClaim(claims))("admission:standing_tactical_only"),
      Option.when(lead.exists(summary => summary.route.exists(GenericTransitionRoutes.contains)))("selection:only_generic_transition")
    ).flatten

  private def tacticalVerdict(
      roots: Vector[RootFact],
      claims: Vector[CommentaryClaim]
  ): String =
    if hasMoveLocalTacticalClaim(claims) then "move_local_tactical_claim"
    else if roots.nonEmpty then "standing_tactical_only"
    else "no_tactical_root"

  private def preRendererVerdict(lead: Option[ClaimSummary]): String =
    lead match
      case None => "no_selected_claim"
      case Some(summary) if summaryHasMoveLocalTacticalEvidence(summary) =>
        "selected_move_local_tactical_claim"
      case Some(summary) if summary.route.exists(GenericTransitionRoutes.contains) =>
        "selected_generic_transition"
      case Some(summary) if summary.scope.contains("move_local") =>
        "selected_move_local_non_tactical_claim"
      case Some(summary) if summaryHasTacticalEvidence(summary) && summary.scope.contains("position_local") =>
        "selected_position_local_tactical_claim"
      case Some(summary) if summary.scope.contains("position_local") =>
        "selected_position_local_non_tactical_claim"
      case Some(_) =>
        "selected_exact_claim"

  private def hasMoveLocalTacticalClaim(claims: Vector[CommentaryClaim]): Boolean =
    claims.exists: claim =>
      claim.status == ClaimStatus.Admitted &&
        claim.scope.contains("move_local") &&
        claim.evidenceRefs.exists(ref => MoveLocalTacticalEvidenceIds.contains(ref.id))

  private def summaryHasMoveLocalTacticalEvidence(summary: ClaimSummary): Boolean =
    summary.scope.contains("move_local") &&
      summary.evidenceIds.exists(MoveLocalTacticalEvidenceIds.contains)

  private def summaryHasTacticalEvidence(summary: ClaimSummary): Boolean =
    summary.evidenceIds.exists(MoveLocalTacticalEvidenceIds.contains)
