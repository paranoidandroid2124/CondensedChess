package lila.commentary.tools.moveReview

import lila.commentary.{ MoveReviewMoveRef, MoveReviewRefs, StrategyPack }
import lila.commentary.analysis.*
import lila.commentary.analysis.claim.ProofContractRules
import lila.commentary.analysis.semantic.StrategicObservationIds.ProofFamilyId
import lila.commentary.model.*

object MoveReviewCoverageDiagnostics:

  object BasicEvidenceStatus:
    val Emitted = "emitted"
    val PlannerPreempted = "planner_preempted"
    val Blocked = "blocked"

  final case class BasicEvidenceDiagnostic(status: String, rejectReasons: List[String])

  final case class SupportedLocalDiagnostic(
      candidateFamilies: List[String] = Nil,
      admittedFamilies: List[String] = Nil,
      rejectReasons: List[String] = Nil
  )

  final case class Result(
      moveReviewSourceKind: Option[String] = None,
      basicEvidenceStatus: Option[String] = None,
      basicEvidenceRejectReasons: List[String] = Nil,
      supportedLocalCandidateFamilies: List[String] = Nil,
      supportedLocalAdmittedFamilies: List[String] = Nil,
      supportedLocalRejectReasons: List[String] = Nil
  )

  object Result:
    val empty: Result = Result()

  def build(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract],
      slots: MoveReviewPolishSlots,
      plannerInputs: QuestionPlannerInputs
  ): Result =
    val basic = basicEvidence(ctx, refs, strategyPack, truthContract, slots.sourceKind)
    val supportedLocal =
      supportedLocalFromInputs(
        ctx,
        plannerInputs,
        tacticalVetoReasons(plannerInputs, truthContract)
      )
    Result(
      moveReviewSourceKind = Some(slots.sourceKind),
      basicEvidenceStatus = Some(basic.status),
      basicEvidenceRejectReasons = basic.rejectReasons,
      supportedLocalCandidateFamilies = supportedLocal.candidateFamilies,
      supportedLocalAdmittedFamilies = supportedLocal.admittedFamilies,
      supportedLocalRejectReasons = supportedLocal.rejectReasons
    )

  private def basicEvidence(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract],
      sourceKind: String
  ): BasicEvidenceDiagnostic =
    sourceKind match
      case MoveReviewPolishSlots.Source.BasicMoveExplanation =>
        BasicEvidenceDiagnostic(BasicEvidenceStatus.Emitted, Nil)
      case MoveReviewPolishSlots.Source.Planner =>
        BasicEvidenceDiagnostic(BasicEvidenceStatus.PlannerPreempted, Nil)
      case _ =>
        val played = MoveReviewExplanationBuilder.current(ctx)
        val lineFacts = played.flatMap(move => MoveReviewPvLine.firstCoupled(ctx.fen, move.uci, refs))
        val explanation = MoveReviewExplanationBuilder.build(ctx, refs, truthContract, strategyPack)
        val blockerReasons = List.newBuilder[String]
        if played.isEmpty then blockerReasons += "missing_current_move"
        if played.nonEmpty && lineFacts.isEmpty then blockerReasons += coupledPvMissingReason(played.get, refs)
        if played.nonEmpty && lineFacts.nonEmpty && explanation.isEmpty then blockerReasons += "no_descriptor_rule_matched"
        val detailReasons = played.toList.flatMap(move => basicDetailReasons(ctx, move, lineFacts, explanation))
        val reasons =
          (blockerReasons.result() ++ detailReasons).distinct match
            case Nil => List("basic_builder_not_emitted")
            case xs  => xs
        BasicEvidenceDiagnostic(BasicEvidenceStatus.Blocked, reasons)

  private def coupledPvMissingReason(
      played: CommentaryIdeaSurface.PlayedMove,
      refs: Option[MoveReviewRefs]
  ): String =
    if firstMoveMatchesPlayed(refs, played.uci) then "coupled_pv_replay_failed"
    else "missing_coupled_pv_line"

  private def basicDetailReasons(
      ctx: NarrativeContext,
      played: CommentaryIdeaSurface.PlayedMove,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      explanation: Option[lila.commentary.MoveReviewExplanation]
  ): List[String] =
    val facts = moveFacts(ctx)
    val motifs = moveMotifs(ctx)
    val reasons = List.newBuilder[String]
    if tacticalEvidencePresent(facts, motifs) && lineFacts.nonEmpty && explanation.isEmpty then
      reasons += "tactical_not_current_move_owned"
    if played.isCapture &&
      motifs.exists(_.isInstanceOf[Motif.Capture]) &&
      !isImmediateRecapture(played.toKey, lineFacts.flatMap(_.reply))
    then reasons += "capture_not_immediate_recapture"
    reasons.result()

  private def supportedLocalFromInputs(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      tacticalVetoReasons: List[String]
  ): SupportedLocalDiagnostic =
    supportedLocalFromPackets(mainPathPackets(inputs), tacticalVetoReasons, Some(ctx))

  private[moveReview] def supportedLocalFromPackets(
      packets: List[PlayerFacingClaimPacket],
      tacticalVetoReasons: List[String] = Nil,
      ctx: Option[NarrativeContext] = None
  ): SupportedLocalDiagnostic =
    val candidates =
      packets.filter(packet => ProofContractRules.supportedLocalEligible(packet.proofFamily))
    val (admitted, rejected) =
      candidates.partition(packet =>
        supportedLocalPolicyAdmitted(packet) &&
          !tacticalVetoApplies(packet, tacticalVetoReasons) &&
          supportedLocalSurfaceAdmitted(packet, ctx)
      )
    val rejectReasons =
      rejected
        .flatMap { packet =>
          val trace = ProofContractRules.traceFor(packet)
          val failures =
            (
              trace.failureCodes ++
                policyFailureCodes(packet) ++
                tacticalFailureCodes(packet, tacticalVetoReasons) ++
                surfaceFailureCodes(packet, ctx)
            ).distinct match
              case Nil => List("policy:not_supported_local_admitted")
              case xs  => xs
          failures.map(code => s"${packet.proofFamily}:$code")
        }
        .distinct
        .sorted

    SupportedLocalDiagnostic(
      candidateFamilies = candidates.map(_.proofFamily).distinct.sorted,
      admittedFamilies = admitted.map(_.proofFamily).distinct.sorted,
      rejectReasons = rejectReasons
    )

  private def supportedLocalPolicyAdmitted(packet: PlayerFacingClaimPacket): Boolean =
    ProofContractRules.supportedLocalAdmissible(packet) &&
      PlayerFacingClaimProof.allowsWeakMainClaim(packet) &&
      packet.releaseRisks.isEmpty

  private def policyFailureCodes(packet: PlayerFacingClaimPacket): List[String] =
    val reasons = List.newBuilder[String]
    if packet.scope != PlayerFacingPacketScope.MoveLocal && packet.scope != PlayerFacingPacketScope.PositionLocal then
      reasons += "policy:scope_not_local"
    if packet.fallbackMode != PlayerFacingClaimFallbackMode.WeakMain then reasons += "policy:fallback_not_weak_main"
    if packet.suppressionReasons.nonEmpty then reasons += "policy:suppressed"
    if packet.releaseRisks.nonEmpty then reasons += "policy:release_risk"
    if !PlayerFacingClaimProof.allowsWeakMainClaim(packet) then reasons += "policy:weak_main_gate_blocked"
    reasons.result()

  private def tacticalFailureCodes(
      packet: PlayerFacingClaimPacket,
      tacticalVetoReasons: List[String]
  ): List[String] =
    if tacticalVetoApplies(packet, tacticalVetoReasons) then
      tacticalVetoReasons.distinct.map(reason => s"tactical_veto:$reason")
    else Nil

  private def tacticalVetoApplies(
      packet: PlayerFacingClaimPacket,
      tacticalVetoReasons: List[String]
  ): Boolean =
    tacticalVetoReasons.nonEmpty &&
      (
        packet.proofFamily == ProofFamilyId.NeutralizeKeyBreak.wireKey ||
          packet.proofFamily == CentralBreakTimingWitness.ProofFamily
      )

  private def supportedLocalSurfaceAdmitted(
      packet: PlayerFacingClaimPacket,
      ctx: Option[NarrativeContext]
  ): Boolean =
    if packet.proofFamily == ProofFamilyId.NeutralizeKeyBreak.wireKey then
      NeutralizeKeyBreakSurfaceGate.decideForPacket(packet, ctx).admitted
    else if packet.proofFamily == CentralBreakTimingWitness.ProofFamily then
      ctx.flatMap(CentralBreakTimingWitness.exact).exists(CentralBreakTimingSurfaceGate.decide(_).admitted)
    else true

  private def surfaceFailureCodes(
      packet: PlayerFacingClaimPacket,
      ctx: Option[NarrativeContext]
  ): List[String] =
    if packet.proofFamily == ProofFamilyId.NeutralizeKeyBreak.wireKey then
      NeutralizeKeyBreakSurfaceGate.decideForPacket(packet, ctx).rejectReason.toList
    else if packet.proofFamily == CentralBreakTimingWitness.ProofFamily then
      ctx
        .flatMap(CentralBreakTimingWitness.exact)
        .map(CentralBreakTimingSurfaceGate.decide(_).rejectReason.toList)
        .getOrElse(List(CentralBreakTimingSurfaceGate.MissingExactWitness))
    else Nil

  private def tacticalVetoReasons(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): List[String] =
    val reasons = List.newBuilder[String]
    truthContract.foreach { contract =>
      if contract.truthClass == DecisiveTruthClass.Blunder then reasons += "truth_contract_blunder"
      if contract.truthClass == DecisiveTruthClass.MissedWin then reasons += "truth_contract_missed_win"
      if contract.reasonFamily == DecisiveReasonKind.TacticalRefutation && contract.isBad then
        reasons += "truth_contract_tactical_refutation"
      if contract.failureMode == FailureInterpretationMode.TacticalRefutation then
        reasons += "truth_contract_tactical_failure_mode"
    }
    if inputs.truthMode == PlayerFacingTruthMode.Tactical then reasons += "planner_truth_mode_tactical"
    if inputs.mainBundle.flatMap(_.mainClaim).exists(_.mode == PlayerFacingTruthMode.Tactical) then
      reasons += "main_claim_tactical"
    reasons.result().distinct

  private def mainPathPackets(inputs: QuestionPlannerInputs): List[PlayerFacingClaimPacket] =
    inputs.mainBundle.toList.flatMap { bundle =>
      (bundle.mainClaim.toList ++ bundle.lineScopedClaim.toList).flatMap(_.packet)
    }

  private def moveFacts(ctx: NarrativeContext): List[Fact] =
    (
      ctx.candidates.flatMap(_.facts) ++
        ctx.facts ++
        ctx.mainPvFacts ++
        ctx.threatLineFacts ++
        ctx.counterfactualFacts
      ).distinct

  private def moveMotifs(ctx: NarrativeContext): List[Motif] =
    ctx.candidates.flatMap(_.lineMotifs).distinct

  private def tacticalEvidencePresent(facts: List[Fact], motifs: List[Motif]): Boolean =
    facts.exists {
      case _: Fact.Fork | _: Fact.Pin | _: Fact.Skewer => true
      case _                                           => false
    } || motifs.exists {
      case _: Motif.Fork | _: Motif.Pin | _: Motif.Skewer => true
      case _                                              => false
    }

  private def isImmediateRecapture(playedToKey: String, reply: Option[MoveReviewMoveRef]): Boolean =
    reply.exists { move =>
      MoveReviewPvLine.normalizeUci(move.uci).slice(2, 4) == playedToKey
    }

  private def firstMoveMatchesPlayed(refs: Option[MoveReviewRefs], playedUci: String): Boolean =
    val normalized = MoveReviewPvLine.normalizeUci(playedUci)
    refs.exists(_.variations.exists(_.moves.headOption.exists(move => MoveReviewPvLine.normalizeUci(move.uci) == normalized)))
