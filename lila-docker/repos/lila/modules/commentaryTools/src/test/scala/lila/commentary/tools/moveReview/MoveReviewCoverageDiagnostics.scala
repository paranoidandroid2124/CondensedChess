package lila.commentary.tools.moveReview

import lila.commentary.{ MoveReviewMoveRef, MoveReviewRefs, StrategyPack }
import lila.commentary.analysis.*
import lila.commentary.analysis.claim.{ ClaimAuthorityResolver, ProofContractRules }
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

  final case class LocalFactDiagnostic(
      status: Option[String] = None,
      families: List[String] = Nil,
      authorities: List[String] = Nil,
      producers: List[String] = Nil,
      evidenceRefs: List[String] = Nil,
      strictFallbackEligible: Option[Boolean] = None,
      rejectReasons: List[String] = Nil
  )

  final case class Result(
      moveReviewSourceKind: Option[String] = None,
      basicEvidenceStatus: Option[String] = None,
      basicEvidenceRejectReasons: List[String] = Nil,
      supportedLocalCandidateFamilies: List[String] = Nil,
      supportedLocalAdmittedFamilies: List[String] = Nil,
      supportedLocalRejectReasons: List[String] = Nil,
      moveReviewLocalFactStatus: Option[String] = None,
      moveReviewLocalFactFamilies: List[String] = Nil,
      moveReviewLocalFactAuthorities: List[String] = Nil,
      moveReviewLocalFactProducers: List[String] = Nil,
      moveReviewLocalFactEvidenceRefs: List[String] = Nil,
      moveReviewLocalFactStrictFallbackEligible: Option[Boolean] = None,
      moveReviewLocalFactRejectReasons: List[String] = Nil
  )

  object Result:
    val empty: Result = Result()

  def build(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract],
      slots: MoveReviewPolishSlots,
      plannerInputs: QuestionPlannerInputs,
      causalTrace: Option[MoveReviewCompressionPolicy.CausalClaimTrace] = None
  ): Result =
    val basic = basicEvidence(ctx, refs, strategyPack, truthContract, plannerInputs, slots.sourceKind)
    val supportedLocal =
      supportedLocalFromInputs(
        ctx,
        plannerInputs,
        tacticalVetoReasons(plannerInputs, truthContract)
      )
    val localFact =
      localFactDiagnostic(ctx, refs, strategyPack, truthContract, slots, causalTrace)
    Result(
      moveReviewSourceKind = Some(slots.sourceKind),
      basicEvidenceStatus = Some(basic.status),
      basicEvidenceRejectReasons = basic.rejectReasons,
      supportedLocalCandidateFamilies = supportedLocal.candidateFamilies,
      supportedLocalAdmittedFamilies = supportedLocal.admittedFamilies,
      supportedLocalRejectReasons = supportedLocal.rejectReasons,
      moveReviewLocalFactStatus = localFact.status,
      moveReviewLocalFactFamilies = localFact.families,
      moveReviewLocalFactAuthorities = localFact.authorities,
      moveReviewLocalFactProducers = localFact.producers,
      moveReviewLocalFactEvidenceRefs = localFact.evidenceRefs,
      moveReviewLocalFactStrictFallbackEligible = localFact.strictFallbackEligible,
      moveReviewLocalFactRejectReasons = localFact.rejectReasons
    )

  private def basicEvidence(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract],
      plannerInputs: QuestionPlannerInputs,
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
        if explanation.exists(expl => MoveReviewCompressionPolicy.positiveBasicExplanationBlockedByTruth(expl.source, truthContract))
        then blockerReasons += "positive_basic_blocked_by_truth_contract"
        val detailReasons = played.toList.flatMap(move => basicDetailReasons(ctx, plannerInputs, move, lineFacts, explanation))
        val reasons =
          (blockerReasons.result() ++ detailReasons).distinct match
            case Nil => List("basic_builder_not_emitted")
            case xs  => xs
        BasicEvidenceDiagnostic(BasicEvidenceStatus.Blocked, reasons)

  private def localFactDiagnostic(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract],
      slots: MoveReviewPolishSlots,
      causalTrace: Option[MoveReviewCompressionPolicy.CausalClaimTrace]
  ): LocalFactDiagnostic =
    val renderedPlannerFact =
      Option.when(slots.sourceKind == MoveReviewPolishSlots.Source.Planner) {
        causalTrace
          .filter(_.status == "accepted")
          .flatMap(trace =>
            trace.localFactFamily.map { family =>
              LocalFactDiagnostic(
                status = Some("emitted"),
                families = List(family),
                authorities = trace.localFactAuthority.toList,
                producers = trace.localFactProducer.toList,
                evidenceRefs = trace.localFactEvidenceRefs,
                strictFallbackEligible = trace.localFactStrictFallbackEligible,
                rejectReasons = Nil
              )
            }
          )
      }.flatten
    renderedPlannerFact.getOrElse {
      val emitted = slots.moveReviewExplanation
      val candidate =
        slots.localFact
          .filter(_ => emitted.nonEmpty)
          .map(localFact => MoveReviewExplanationBuilder.Result(emitted.get, localFact))
          .orElse(MoveReviewExplanationBuilder.buildWithLocalFact(ctx, refs, truthContract, strategyPack))
      val strictCandidate =
        MoveReviewExplanationBuilder.buildWithLocalFact(ctx, refs, truthContract, strategyPack, strictLocalFacts = true)
      val localFact = candidate.map(_.localFact)
      val families = localFact.map(_.family.key).toList
      val authorities = localFact.map(_.authority.key).toList
      val producers = localFact.map(_.producer.key).toList
      val evidenceRefs = localFact.map(_.evidenceRefs).getOrElse(Nil)
      val strictEligible = localFact.map(_.strictFallbackEligible)
      val status =
        if emitted.nonEmpty && localFact.nonEmpty then Some("emitted")
        else if candidate.nonEmpty && strictCandidate.isEmpty then Some("blocked")
        else if candidate.nonEmpty && localFact.nonEmpty then Some("candidate")
        else None
      val rejectReasons =
        if candidate.nonEmpty && strictCandidate.isEmpty then
          families.map(family => s"strict_fallback_rejected:$family") match
            case Nil => List("strict_fallback_rejected")
            case xs  => xs
        else Nil
      LocalFactDiagnostic(
        status = status,
        families = families,
        authorities = authorities,
        producers = producers,
        evidenceRefs = evidenceRefs,
        strictFallbackEligible = strictEligible,
        rejectReasons = rejectReasons
      )
    }

  private def coupledPvMissingReason(
      played: CommentaryIdeaSurface.PlayedMove,
      refs: Option[MoveReviewRefs]
  ): String =
    if firstMoveMatchesPlayed(refs, played.uci) then "coupled_pv_replay_failed"
    else "missing_coupled_pv_line"

  private def basicDetailReasons(
      ctx: NarrativeContext,
      plannerInputs: QuestionPlannerInputs,
      played: CommentaryIdeaSurface.PlayedMove,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      explanation: Option[lila.commentary.MoveReviewExplanation]
  ): List[String] =
    val facts = moveFacts(ctx)
    val motifs = moveMotifs(ctx)
    val reasons = List.newBuilder[String]
    if tacticalEvidencePresent(facts, motifs) && lineFacts.nonEmpty && explanation.isEmpty then
      reasons += "tactical_not_current_move_owned"
      reasons ++= CommentaryIdeaSurface.tacticalOwnershipRejectReasons(
        played,
        CommentaryIdeaSurface.MoveReviewEvidence(
          facts = facts,
          motifs = motifs,
          openingGoal = None,
          openingName = None
        ),
        lineFacts
      )
    if played.isCapture &&
      motifs.exists(_.isInstanceOf[Motif.Capture]) &&
      !isImmediateRecapture(played.toKey, lineFacts.flatMap(_.reply))
    then reasons += "capture_not_immediate_recapture"
    if evalGapWithoutConcreteDescriptor(plannerInputs, lineFacts, explanation) then
      reasons += "eval_gap_without_concrete_descriptor"
    reasons.result()

  private def evalGapWithoutConcreteDescriptor(
      inputs: QuestionPlannerInputs,
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      explanation: Option[lila.commentary.MoveReviewExplanation]
  ): Boolean =
    explanation.isEmpty &&
      lineFacts.nonEmpty &&
      inputs.decisionComparison.exists(comparison =>
        !comparison.chosenMatchesBest &&
          comparison.cpLossVsChosen.exists(loss => math.abs(loss) >= 60)
      ) &&
      inputs.localFactResult.isEmpty &&
      inputs.lineConsequence.forall(evidence =>
        evidence.kind == LineConsequenceKind.PreviewOnly || !evidence.surfaceReady
      ) &&
      inputs.pvDelta.forall(pvDeltaEmpty) &&
      inputs.pvCoupledPlanSupport.forall(!_.anchorMatched)

  private def pvDeltaEmpty(delta: PVDelta): Boolean =
    delta.resolvedThreats.isEmpty &&
      delta.newOpportunities.isEmpty &&
      delta.planAdvancements.isEmpty &&
      delta.concessions.isEmpty

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
                tacticalRiskGateCodes(packet, tacticalVetoReasons) ++
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

  private def tacticalRiskGateCodes(
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
    else if packet.proofFamily == ProofFamilyId.ColorComplexSqueeze.wireKey then
      ClaimAuthorityResolver.colorComplexPositionProbeOwnershipFailure(ctx, packet).isEmpty
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
    else if packet.proofFamily == ProofFamilyId.ColorComplexSqueeze.wireKey then
      ClaimAuthorityResolver.colorComplexPositionProbeOwnershipFailure(ctx, packet).toList
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
        reasons += "truth_contract_high_risk_failure_mode"
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
