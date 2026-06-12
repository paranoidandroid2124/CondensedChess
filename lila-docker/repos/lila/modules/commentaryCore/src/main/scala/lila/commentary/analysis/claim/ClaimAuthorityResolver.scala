package lila.commentary.analysis.claim

import lila.commentary.analysis.*
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }
import lila.commentary.model.*
import lila.commentary.model.authoring.AuthorQuestionKind

private[commentary] object ClaimAuthorityResolver:

  final case class SupportedLocalNeutralizeKeyBreakAdmission(
      packet: PlayerFacingClaimPacket,
      decision: ClaimAuthorityDecision
  )

  final case class SupportedLocalCentralBreakTimingAdmission(
      packet: PlayerFacingClaimPacket,
      witness: CentralBreakTimingWitness.Witness,
      decision: ClaimAuthorityDecision
  )

  final case class PromotedLocalFactAdmission(
      packet: PlayerFacingClaimPacket,
      decision: ClaimAuthorityDecision,
      claim: Option[MainPathScopedClaim],
      family: MoveReviewLocalFact.Family,
      source: MoveReviewLocalFact.Source,
      subject: MoveReviewLocalFact.Subject,
      strictFallbackCandidate: Boolean,
      lineBinding: MoveReviewLocalFact.LineBinding,
      evidenceRefs: List[String],
      anchors: List[MoveReviewLocalFact.Anchor],
      guardrails: List[String],
      supportText: Option[String]
  ):
    def localFactCandidate: MoveReviewLocalFact.Candidate =
      MoveReviewLocalFact.Candidate(
        family = family,
        source = source,
        producer = MoveReviewLocalFact.Producer.ClaimAuthorityPromotion,
        subject = subject,
        strictFallbackCandidate = strictFallbackCandidate,
        anchors = anchors,
        lineBinding = lineBinding,
        evidenceRefs = evidenceRefs,
        guardrails = guardrails
      )

  def promotedLocalFactAdmissions(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      plan: QuestionPlan
  ): List[PromotedLocalFactAdmission] =
    val claimPackets = claimsWithPackets(inputs)
    val claimAdmissions =
      claimPackets.flatMap { case (claim, packet) =>
        promotedDecisionForClaim(ctx, inputs, truthContract, plan, claim, packet)
          .filter(_.admitted)
          .flatMap(decision => promotedLocalFactAdmission(packet, decision, Some(claim)))
      }
    val neutralizeAdmissions =
      supportedLocalNeutralizeKeyBreakTimingAdmission(ctx, inputs, truthContract, plan).toList
        .filter(_.decision.admitted)
        .flatMap(admission =>
          promotedLocalFactAdmission(
            packet = admission.packet,
            decision = admission.decision,
            claim = claimPackets.find(_._2 == admission.packet).map(_._1)
          )
        )
    (claimAdmissions ++ neutralizeAdmissions).distinctBy(admission =>
      (
        admission.packet.proofSource,
        admission.packet.proofFamily,
        admission.family,
        admission.source,
        admission.claim.map(_.scope),
        admission.claim.map(_.sourceKind),
        admission.decision.tier
      )
    )

  def namedRouteNetworkSurfaceDecision(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[ClaimAuthorityDecision] =
    inputs.namedRouteNetworkSurface.map { surface =>
      val tacticalReasons = tacticalVetoReasons(ctx, inputs, truthContract)
      if tacticalReasons.nonEmpty then ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, tacticalReasons)
      else if inputs.heavyPieceLocalBindBlocked then
        ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, List("heavy_piece_local_bind_blocked"))
      else if surface.intermediateSquare.nonEmpty then
        ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, List("route_chain_backend_only"))
      else ClaimAuthorityDecision(ClaimAuthorityTier.SupportedLocal)
    }

  def dualAxisBindSurfaceDecision(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[ClaimAuthorityDecision] =
    inputs.dualAxisBindSurface.map { contract =>
      val tacticalReasons = tacticalVetoReasons(ctx, inputs, truthContract)
      if tacticalReasons.nonEmpty then ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, tacticalReasons)
      else if inputs.heavyPieceLocalBindBlocked then
        ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, List("heavy_piece_local_bind_blocked"))
      else if dualAxisBindSurfaceAdmissible(contract) then ClaimAuthorityDecision(ClaimAuthorityTier.SupportedLocal)
      else ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, contract.failsIf)
    }

  def restrictedDefenseConversionSurfaceDecision(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[ClaimAuthorityDecision] =
    inputs.restrictedDefenseConversionSurface.map { contract =>
      val tacticalReasons = tacticalVetoReasons(ctx, inputs, truthContract)
      if tacticalReasons.nonEmpty then ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, tacticalReasons)
      else if restrictedDefenseConversionSurfaceAdmissible(contract) then ClaimAuthorityDecision(ClaimAuthorityTier.SupportedLocal)
      else ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, contract.failsIf)
    }

  def decidePositionProbe(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      packet: PlayerFacingClaimPacket
  ): ClaimAuthorityDecision =
    val tacticalReasons = tacticalVetoReasons(ctx, inputs, truthContract)
    if tacticalReasons.nonEmpty && isSupportedPositionProbeFamily(packet.proofFamily) then
      ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, tacticalReasons)
    else if PlayerFacingTruthModePolicy.certifiedPositionProbePacket(packet) &&
        ProofContractRules.certifiedOwnerAdmissible(packet)
    then
      ClaimAuthorityDecision(ClaimAuthorityTier.CertifiedOwner)
    else if supportsLocalPositionProbe(packet) then
      ClaimAuthorityDecision(ClaimAuthorityTier.SupportedLocal)
    else if ProofContractRules.contractForPacket(packet).exists(_.status == ProofContractStatus.Deferred) then
      ClaimAuthorityDecision(ClaimAuthorityTier.DiagnosticOnly, authorityFailureCodes(packet))
    else
      ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, authorityFailureCodes(packet))

  def planAuthorityDecision(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      plan: QuestionPlan
  ): Option[ClaimAuthorityDecision] =
    shouldTacticalVetoPlan(ctx, inputs, truthContract, plan)
      .orElse(decideSupportedMoveDelta(ctx, inputs, truthContract, plan))
      .orElse(decideSupportedNeutralizeKeyBreakTiming(ctx, inputs, truthContract, plan))

  def supportedLocalNeutralizeKeyBreakTimingDecision(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      plan: QuestionPlan
  ): Option[ClaimAuthorityDecision] =
    supportedLocalNeutralizeKeyBreakTimingAdmission(ctx, inputs, truthContract, plan).map(_.decision)

  def supportedLocalNeutralizeKeyBreakTimingAdmission(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      plan: QuestionPlan
  ): Option[SupportedLocalNeutralizeKeyBreakAdmission] =
    if !isNeutralizeKeyBreakTimingPlan(plan) then None
    else
      matchingNeutralizeKeyBreakTimingPacket(inputs, plan).map { packet =>
        val tacticalReasons = tacticalVetoReasons(ctx, inputs, truthContract)
        val decision =
          if tacticalReasons.nonEmpty then
            ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, tacticalReasons)
          else ClaimAuthorityDecision(ClaimAuthorityTier.SupportedLocal)
        SupportedLocalNeutralizeKeyBreakAdmission(packet, decision)
      }

  def supportedLocalNeutralizeKeyBreakPacketDecision(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      packet: PlayerFacingClaimPacket
  ): ClaimAuthorityDecision =
    if packet.proofSource != ProofSourceId.CounterplayAxisSuppression.wireKey ||
        packet.proofFamily != ProofFamilyId.NeutralizeKeyBreak.wireKey
    then ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, authorityFailureCodes(packet))
    else
      val tacticalReasons = tacticalVetoReasons(ctx, inputs, truthContract)
      if tacticalReasons.nonEmpty then ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, tacticalReasons)
      else if supportsLocalMoveDelta(packet) && hasExactOwnerPath(packet) then
        ClaimAuthorityDecision(ClaimAuthorityTier.SupportedLocal)
      else ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, authorityFailureCodes(packet))

  def supportedLocalCentralBreakTimingPacketDecision(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      packet: PlayerFacingClaimPacket
  ): ClaimAuthorityDecision =
    supportedLocalCentralBreakTimingAdmission(ctx, inputs, truthContract, packet)
      .map(_.decision)
      .getOrElse(ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, authorityFailureCodes(packet)))

  def supportedLocalMoveDeltaPacketDecision(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      packet: PlayerFacingClaimPacket
  ): ClaimAuthorityDecision =
    if !ProofContractRules.supportsMoveDeltaProofFamily(packet.proofFamily) then
      ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, authorityFailureCodes(packet))
    else
      val tacticalReasons = tacticalVetoReasons(ctx, inputs, truthContract)
      if tacticalReasons.nonEmpty then ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, tacticalReasons)
      else if supportsLocalMoveDelta(packet) &&
          (!hasExactOwnerPath(packet) || exactMoveDeltaSupportedLocal(packet))
      then ClaimAuthorityDecision(ClaimAuthorityTier.SupportedLocal)
      else ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, authorityFailureCodes(packet))

  def supportedLocalCentralBreakTimingAdmission(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      packet: PlayerFacingClaimPacket
  ): Option[SupportedLocalCentralBreakTimingAdmission] =
    if packet.proofSource != CentralBreakTimingWitness.ProofSource ||
        packet.proofFamily != CentralBreakTimingWitness.ProofFamily
    then None
    else
      for
        narrativeCtx <- ctx
        witness <- CentralBreakTimingWitness.exact(narrativeCtx)
        if supportsLocalMoveDelta(packet)
        if centralBreakTimingWitnessMatchesPacket(witness, packet)
      yield
        val tacticalReasons = tacticalVetoReasons(ctx, inputs, truthContract)
        val decision =
          if tacticalReasons.nonEmpty then ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, tacticalReasons)
          else ClaimAuthorityDecision(ClaimAuthorityTier.SupportedLocal)
        SupportedLocalCentralBreakTimingAdmission(packet, witness, decision)

  def shouldTacticalVetoPlan(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      plan: QuestionPlan
  ): Option[ClaimAuthorityDecision] =
    val tacticalReasons = tacticalVetoReasons(ctx, inputs, truthContract)
    if tacticalReasons.nonEmpty && isSupportedPositionProbePlan(plan) then
      Some(ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, tacticalReasons))
    else None

  def supportedLocalSurface(raw: String): String =
    val rendered =
      PlayerFacingClaimPrefixKind.SupportedLocal.render(
        Option(raw).getOrElse("").trim.stripSuffix(".")
      )
    if rendered.endsWith(".") then rendered else s"$rendered."

  private def tacticalVetoReasons(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): List[String] =
    if allowSoftVeto(ctx, inputs, truthContract) then Nil
    else
      val reasons = List.newBuilder[String]
      if ctx.isEmpty then
        reasons += "tactical_context_missing"
        if truthContract.isEmpty then reasons += "truth_contract_missing"
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
      ctx.foreach { narrativeCtx =>
        val tactical = TacticalTensionPolicy.evaluate(narrativeCtx, truthContract)
        if tactical.severeCounterfactual then reasons += "context_severe_counterfactual"
      }
      reasons.result().distinct

  private def allowSoftVeto(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    if inputs.truthMode == PlayerFacingTruthMode.Tactical ||
      inputs.mainBundle.flatMap(_.mainClaim).exists(_.mode == PlayerFacingTruthMode.Tactical)
    then false
    else if ctx.isEmpty || truthContract.isEmpty then false
    else
      val cpLoss = truthContract.map(_.cpLoss)
        .orElse(inputs.counterfactual.map(_.cpLoss))
        .orElse(inputs.decisionComparison.flatMap(_.cpLossVsChosen))
      val winPercentLoss = cpLoss.map(cp => DecisiveTruth.winPercentFromCp(cp) - 50.0).getOrElse(0.0)
      val highRiskGate = truthContract.exists { contract =>
        contract.blocksStrategicSupport
      }
      val severeCounterfactual =
        ctx.exists(narrativeCtx => TacticalTensionPolicy.evaluate(narrativeCtx, truthContract).severeCounterfactual)

      if highRiskGate || severeCounterfactual then
        false
      else if winPercentLoss < 10.0 then
        true
      else if winPercentLoss < 20.0 then
        val isForcingOrOnlyMove =
          truthContract.exists(c =>
            c.reasonFamily == DecisiveReasonKind.OnlyMoveDefense ||
            c.reasonFamily == DecisiveReasonKind.TacticalRefutation
          )
        !isForcingOrOnlyMove
      else
        false

  private def supportsLocalPositionProbe(packet: PlayerFacingClaimPacket): Boolean =
    supportsLocalPacket(packet, PlayerFacingPacketScope.PositionLocal)

  private def decideSupportedMoveDelta(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      plan: QuestionPlan
  ): Option[ClaimAuthorityDecision] =
    matchingMoveDeltaPacket(inputs, plan)
      .map(packet => supportedLocalMoveDeltaPacketDecision(ctx, inputs, truthContract, packet))

  private def decideSupportedNeutralizeKeyBreakTiming(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      plan: QuestionPlan
  ): Option[ClaimAuthorityDecision] =
    supportedLocalNeutralizeKeyBreakTimingAdmission(ctx, inputs, truthContract, plan).map(_.decision)

  private def isNeutralizeKeyBreakTimingPlan(plan: QuestionPlan): Boolean =
    plan.plannerOwnerKind == PlannerOwnerKind.ForcingDefense &&
      (plan.questionKind == AuthorQuestionKind.WhyNow ||
        plan.questionKind == AuthorQuestionKind.WhatMustBeStopped) &&
      (plan.plannerSource == "threat" || plan.plannerSource == "prevented_plan")

  private def matchingNeutralizeKeyBreakTimingPacket(
      inputs: QuestionPlannerInputs,
      plan: QuestionPlan
  ): Option[PlayerFacingClaimPacket] =
    mainPathPackets(inputs).find(packet =>
      packet.proofSource == ProofSourceId.CounterplayAxisSuppression.wireKey &&
        packet.proofFamily == ProofFamilyId.NeutralizeKeyBreak.wireKey &&
        supportsLocalMoveDelta(packet) &&
        hasExactOwnerPath(packet) &&
        timingWitnessMatchesPacket(plan, packet)
    )

  private def mainPathPackets(inputs: QuestionPlannerInputs): List[PlayerFacingClaimPacket] =
    inputs.mainBundle.toList.flatMap { bundle =>
      (bundle.mainClaim.toList ++ bundle.lineScopedClaim.toList).flatMap(_.packet)
    }

  private def claimsWithPackets(inputs: QuestionPlannerInputs): List[(MainPathScopedClaim, PlayerFacingClaimPacket)] =
    inputs.mainBundle.toList.flatMap { bundle =>
      (bundle.mainClaim.toList ++ bundle.lineScopedClaim.toList).flatMap(claim => claim.packet.map(packet => claim -> packet))
    }

  private def promotedLocalFactAdmission(
      packet: PlayerFacingClaimPacket,
      decision: ClaimAuthorityDecision,
      claim: Option[MainPathScopedClaim]
  ): Option[PromotedLocalFactAdmission] =
    promotedFamily(packet, claim).map { family =>
      val lineBinding = packetLineBinding(packet)
      PromotedLocalFactAdmission(
        packet = packet,
        decision = decision,
        claim = claim,
        family = family,
        source = promotedLocalFactSource(lineBinding),
        subject = promotedLocalFactSubject(family),
        strictFallbackCandidate = promotedStrictFallbackCandidate(decision, family),
        lineBinding = lineBinding,
        evidenceRefs = packetEvidenceRefs(packet, decision, claim),
        anchors = packetAnchors(packet),
        guardrails = packetGuardrails(packet, decision, family),
        supportText = claim.flatMap(c => nonEmpty(c.claimText))
      )
    }

  private def promotedDecisionForClaim(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      plan: QuestionPlan,
      claim: MainPathScopedClaim,
      packet: PlayerFacingClaimPacket
  ): Option[ClaimAuthorityDecision] =
    if plan.plannerOwnerKind == PlannerOwnerKind.MoveDelta &&
        claim.scope == PlayerFacingClaimScope.MoveLocal &&
        claim.sourceKind == plan.plannerSource
    then Some(supportedLocalMoveDeltaPacketDecision(ctx, inputs, truthContract, packet))
    else if plan.plannerOwnerKind == PlannerOwnerKind.PositionProbe &&
        claim.scope == PlayerFacingClaimScope.PositionLocal
    then Some(decidePositionProbe(ctx, inputs, truthContract, packet))
    else None

  private def promotedFamily(
      packet: PlayerFacingClaimPacket,
      claim: Option[MainPathScopedClaim]
  ): Option[MoveReviewLocalFact.Family] =
    claim.flatMap(_.deltaClass.map(MoveReviewLocalFact.familyForMoveDeltaClass))
      .orElse(promotedFamilyFromExactProof(packet.proofPathWitness.exactSliceProof))
      .orElse {
        claim.collect {
          case scopedClaim if scopedClaim.scope == PlayerFacingClaimScope.PositionLocal =>
            MoveReviewLocalFact.Family.Pressure
        }
      }

  private def promotedFamilyFromExactProof(
      proof: Option[PlayerFacingExactSliceProof]
  ): Option[MoveReviewLocalFact.Family] =
    proof.map {
      case _: PlayerFacingExactSliceProof.CounterplayAxisSuppression =>
        MoveReviewLocalFact.Family.Defense
      case _: PlayerFacingExactSliceProof.ProphylacticRestraint =>
        MoveReviewLocalFact.Family.Defense
      case _: PlayerFacingExactSliceProof.QueenTradeShield =>
        MoveReviewLocalFact.Family.LineConsequence
      case _: PlayerFacingExactSliceProof.DefenderTrade =>
        MoveReviewLocalFact.Family.LineConsequence
      case _: PlayerFacingExactSliceProof.BadPieceLiquidation =>
        MoveReviewLocalFact.Family.LineConsequence
      case _: PlayerFacingExactSliceProof.SimplificationWindow =>
        MoveReviewLocalFact.Family.LineConsequence
      case _: PlayerFacingExactSliceProof.CentralBreakTiming =>
        MoveReviewLocalFact.Family.Timing
      case _: PlayerFacingExactSliceProof.ExactTargetFixation =>
        MoveReviewLocalFact.Family.Pressure
      case _: PlayerFacingExactSliceProof.CarlsbadFixedTarget =>
        MoveReviewLocalFact.Family.Pressure
      case _: PlayerFacingExactSliceProof.TargetFocusedCoordination =>
        MoveReviewLocalFact.Family.Pressure
      case _: PlayerFacingExactSliceProof.ColorComplexSqueeze =>
        MoveReviewLocalFact.Family.Pressure
      case _: PlayerFacingExactSliceProof.LocalFileEntryBind =>
        MoveReviewLocalFact.Family.Pressure
      case _: PlayerFacingExactSliceProof.OutpostOccupation =>
        MoveReviewLocalFact.Family.Pressure
      case _: PlayerFacingExactSliceProof.IqpInducement =>
        MoveReviewLocalFact.Family.Pressure
    }

  private def promotedLocalFactSource(
      lineBinding: MoveReviewLocalFact.LineBinding
  ): MoveReviewLocalFact.Source =
    if lineBinding != MoveReviewLocalFact.LineBinding.None then MoveReviewLocalFact.Source.PvCoupledLine
    else MoveReviewLocalFact.Source.CertifiedStrategy

  private def promotedLocalFactSubject(family: MoveReviewLocalFact.Family): MoveReviewLocalFact.Subject =
    family match
      case MoveReviewLocalFact.Family.Defense | MoveReviewLocalFact.Family.Threat =>
        MoveReviewLocalFact.Subject.LineOrReply
      case MoveReviewLocalFact.Family.Attack | MoveReviewLocalFact.Family.Pressure |
          MoveReviewLocalFact.Family.PlanSupport =>
        MoveReviewLocalFact.Subject.PlanResource
      case MoveReviewLocalFact.Family.LineConsequence =>
        MoveReviewLocalFact.Subject.PlayedMove
      case _ =>
        MoveReviewLocalFact.Subject.PlayedMove

  private def promotedStrictFallbackCandidate(
      decision: ClaimAuthorityDecision,
      family: MoveReviewLocalFact.Family
  ): Boolean =
    decision.tier == ClaimAuthorityTier.CertifiedOwner &&
      Set(MoveReviewLocalFact.Family.Defense, MoveReviewLocalFact.Family.Threat, MoveReviewLocalFact.Family.LineConsequence)
        .contains(family)

  private def packetLineBinding(packet: PlayerFacingClaimPacket): MoveReviewLocalFact.LineBinding =
    if packet.scope == PlayerFacingPacketScope.LineScoped then MoveReviewLocalFact.LineBinding.BranchScoped
    else if packet.bestDefenseBranchKey.nonEmpty ||
        packet.proofPathWitness.hasContinuation ||
        packet.proofPathWitness.hasExactSlice
    then MoveReviewLocalFact.LineBinding.PvCoupled
    else MoveReviewLocalFact.LineBinding.None

  private def packetEvidenceRefs(
      packet: PlayerFacingClaimPacket,
      decision: ClaimAuthorityDecision,
      claim: Option[MainPathScopedClaim]
  ): List[String] =
    (
      List(
        s"authority_tier:${decision.tier.toString}",
        s"packet_scope:${packet.scope.toString}",
        s"packet_fallback:${packet.fallbackMode.toString}",
        s"proof_family:${packet.proofFamily}",
        s"proof_source:${packet.proofSource}"
      ) ++
        claim.map(c => s"claim_scope:${c.scope.toString}") ++
        claim.map(c => s"claim_source:${c.sourceKind}") ++
        packet.bestDefenseMove.map(value => s"best_defense_move:$value") ++
        packet.bestDefenseBranchKey.map(value => s"best_defense_branch:$value") ++
        packet.proofPathWitness.ownerSeedTerms.map(value => s"owner_seed:$value") ++
        packet.proofPathWitness.continuationTerms.map(value => s"continuation:$value") ++
        packet.anchorTerms.map(value => s"anchor:$value")
    ).map(_.trim).filter(_.nonEmpty).distinct

  private def packetAnchors(packet: PlayerFacingClaimPacket): List[MoveReviewLocalFact.Anchor] =
    (
      packet.anchorTerms.map(value => MoveReviewLocalFact.Anchor("packet_anchor", value)) ++
        packet.proofPathWitness.ownerSeedTerms.map(value => MoveReviewLocalFact.Anchor("owner_seed", value)) ++
        packet.proofPathWitness.continuationTerms.map(value => MoveReviewLocalFact.Anchor("continuation", value)) ++
        packet.bestDefenseMove.map(value => MoveReviewLocalFact.Anchor("best_defense_move", value)).toList ++
        packet.bestDefenseBranchKey.map(value => MoveReviewLocalFact.Anchor("best_defense_branch", value)).toList
    ).distinct

  private def packetGuardrails(
      packet: PlayerFacingClaimPacket,
      decision: ClaimAuthorityDecision,
      family: MoveReviewLocalFact.Family
  ): List[String] =
    (
      List(
        s"promoted_authority_tier:${decision.tier.toString}",
        s"promoted_family:${family.key}",
        s"packet_scope:${packet.scope.toString}",
        s"packet_fallback:${packet.fallbackMode.toString}",
        s"packet_persistence:${packet.persistence.toString}",
        s"packet_same_branch:${packet.sameBranchState.toString}"
      ) ++
        packet.suppressionReasons.map(reason => s"packet_suppression:$reason") ++
        packet.releaseRisks.map(risk => s"packet_release_risk:$risk") ++
        decision.failureCodes.map(code => s"authority_failure:$code")
    ).map(_.trim).filter(_.nonEmpty).distinct

  private def exactMoveDeltaSupportedLocal(packet: PlayerFacingClaimPacket): Boolean =
    val simplificationWindowFamily =
      ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.SimplificationWindow).map(_.wireKey).getOrElse(
        PlanTaxonomy.PlanKind.SimplificationWindow.id
      )
    val defenderTradeFamily =
      ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.DefenderTrade).map(_.wireKey).getOrElse(
        PlanTaxonomy.PlanKind.DefenderTrade.id
      )
    val queenTradeShieldFamily =
      ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.QueenTradeShield).map(_.wireKey).getOrElse(
        PlanTaxonomy.PlanKind.QueenTradeShield.id
      )
    val badPieceLiquidationFamily =
      ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.BadPieceLiquidation).map(_.wireKey).getOrElse(
        PlanTaxonomy.PlanKind.BadPieceLiquidation.id
      )
    (packet.proofSource == ProofSourceId.CounterplayAxisSuppression.wireKey &&
      packet.proofFamily == ProofFamilyId.NeutralizeKeyBreak.wireKey) ||
      (packet.proofSource == ProofSourceId.ProphylacticMove.wireKey &&
        packet.proofFamily == ProofFamilyId.CounterplayRestraint.wireKey) ||
      (packet.proofSource == ProofSourceId.LocalFileEntryBind.wireKey &&
        packet.proofFamily == ProofFamilyId.HalfOpenFilePressure.wireKey) ||
      (packet.proofSource == simplificationWindowFamily &&
        packet.proofFamily == simplificationWindowFamily) ||
      (packet.proofSource == defenderTradeFamily &&
        packet.proofFamily == defenderTradeFamily) ||
      (packet.proofSource == queenTradeShieldFamily &&
        packet.proofFamily == queenTradeShieldFamily) ||
      (packet.proofSource == badPieceLiquidationFamily &&
        packet.proofFamily == badPieceLiquidationFamily)

  private def matchingMoveDeltaPacket(
      inputs: QuestionPlannerInputs,
      plan: QuestionPlan
  ): Option[PlayerFacingClaimPacket] =
    if plan.plannerOwnerKind == PlannerOwnerKind.MoveDelta then
      inputs.mainBundle.flatMap(_.mainClaim).filter(claim =>
        claim.scope == PlayerFacingClaimScope.MoveLocal &&
          claim.sourceKind == plan.plannerSource
      ).flatMap(_.packet).filter(ProofContractRules.supportedLocalAdmissible)
    else None

  private def supportsLocalMoveDelta(packet: PlayerFacingClaimPacket): Boolean =
    supportsLocalPacket(packet, PlayerFacingPacketScope.MoveLocal)

  private def supportsLocalPacket(packet: PlayerFacingClaimPacket, scope: PlayerFacingPacketScope): Boolean =
    packet.scope == scope &&
      packet.fallbackMode == PlayerFacingClaimFallbackMode.WeakMain &&
      packet.suppressionReasons.isEmpty &&
      packet.releaseRisks.isEmpty &&
      ProofContractRules.supportedLocalAdmissible(packet) &&
      PlayerFacingClaimProof.allowsWeakMainClaim(packet)

  private def hasExactOwnerPath(packet: PlayerFacingClaimPacket): Boolean =
    ProofContractRules.certifiedEligible(packet.proofFamily) &&
      packet.bestDefenseBranchKey.nonEmpty &&
      packet.sameBranchState == PlayerFacingSameBranchState.Proven &&
      packet.persistence == PlayerFacingClaimPersistence.Stable

  private def isSupportedPositionProbePlan(plan: QuestionPlan): Boolean =
    plan.plannerOwnerKind == PlannerOwnerKind.PositionProbe &&
      (
        isSupportedPositionProbeFamily(plan.plannerSource) ||
          plan.sourceKinds.exists(isSupportedPositionProbeFamily) ||
          plan.admissibilityReasons.exists(reason =>
            reason == "strategic_claim_supported_local" ||
              reason == "certified_position_probe"
          )
      )

  private def isSupportedPositionProbeFamily(proofFamily: String): Boolean =
    ProofContractRules.supportsPositionProbeProofFamily(proofFamily)

  private def dualAxisBindSurfaceAdmissible(contract: TwoAxisBindProof.Contract): Boolean =
    contract.certified &&
      normalize(contract.claimScope) == normalize("dual_axis_local") &&
      normalize(contract.bindArchetype) == normalize("break_plus_entry") &&
      normalize(contract.counterplayReinflationRisk) == normalize("bounded_dual_axis_only") &&
      contract.primaryAxis.exists(axis => normalize(axis.kind) == normalize("break_axis")) &&
      contract.corroboratingAxes.exists(axis => normalize(axis.kind) == normalize("entry_axis"))

  private def restrictedDefenseConversionSurfaceAdmissible(
      contract: RestrictedDefenseConversionProof.Contract
  ): Boolean =
    val evidence = contract.restrictedDefenseEvidence
    val route = contract.routePersistence
    contract.certified &&
      contract.bestDefenseFound.nonEmpty &&
      contract.bestDefenseBranchKey.nonEmpty &&
      evidence.defenderResourceCount > 0 &&
      evidence.defenderResourceCount <= 2 &&
      evidence.moveQualityCompression &&
      evidence.preventedResourcePressure &&
      route.bestDefenseStable &&
      route.futureSnapshotPersistent &&
      route.counterplayStillCompressed &&
      route.directBestDefensePresent &&
      route.sameDefendedBranch &&
      !contract.moveOrderFragility.fragile

  private def timingWitnessMatchesPacket(
      plan: QuestionPlan,
      packet: PlayerFacingClaimPacket
  ): Boolean =
    if packet.proofSource == ProofSourceId.CounterplayAxisSuppression.wireKey &&
        packet.proofFamily == ProofFamilyId.NeutralizeKeyBreak.wireKey
    then
      val packetToken = counterplayAxisSuppressionToken(packet)
      plan.timingWitness.exists { witness =>
        witness.proofFamily == packet.proofFamily &&
          witness.source == plan.plannerSource &&
          witness.namedBreak.flatMap(BreakSurfaceToken.canonical).exists(packetToken.contains)
      }
    else
      false

  private def centralBreakTimingWitnessMatchesPacket(
      witness: CentralBreakTimingWitness.Witness,
      packet: PlayerFacingClaimPacket
  ): Boolean =
    packet.proofPathWitness.exactSliceProof.exists {
      case PlayerFacingExactSliceProof.CentralBreakTiming(breakMove, breakSquare, breakToken) =>
        val packetToken = BreakSurfaceToken.canonical(breakToken)
        val witnessToken = BreakSurfaceToken.canonical(witness.breakToken)
        normalize(breakMove) == normalize(witness.breakMove) &&
          normalize(breakSquare) == normalize(witness.breakSquare) &&
          packetToken.nonEmpty &&
          packetToken == witnessToken
      case _ => false
    }

  private def counterplayAxisSuppressionToken(packet: PlayerFacingClaimPacket): Option[String] =
    packet.proofPathWitness.exactSliceProof.collect {
      case PlayerFacingExactSliceProof.CounterplayAxisSuppression(breakToken) => breakToken
    }.flatMap(BreakSurfaceToken.canonical)

  private def authorityFailureCodes(packet: PlayerFacingClaimPacket): List[String] =
    val taxonomy =
      ProofContractRules
        .contractForPacket(packet)
        .map(_.defaultFailureTaxonomy)
        .filter(_.nonEmpty)
        .toList
    (taxonomy ++ ProofContractRules.failureCodes(packet)).distinct

  private def nonEmpty(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty)

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").toLowerCase.replaceAll("""[^a-z0-9\s]""", " ").replaceAll("\\s+", " ").trim
