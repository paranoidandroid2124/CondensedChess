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
      .orElse(decideSupportedMoveDelta(inputs, plan))
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
    Option.when(tacticalReasons.nonEmpty && isSupportedPositionProbePlan(plan)) {
      ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, tacticalReasons)
    }

  def supportedLocalSurface(raw: String): String =
    if normalize(raw).startsWith("a key idea is that ") then raw.trim
    else
      val stripped =
        stripPrefix(raw, "The key strategic fact here is that ")
          .orElse(stripPrefix(raw, "The strategic point is that "))
          .orElse(stripPrefix(raw, "This shows that "))
          .orElse(stripPrefix(raw, "A local reading is that "))
          .orElse(stripPrefix(raw, "a local reading is that "))
          .orElse(stripPrefix(raw, "A key idea is that "))
          .orElse(stripPrefix(raw, "a key idea is that "))
          .getOrElse(raw.trim)
          .stripSuffix(".")
          .trim
      val lowered =
        stripped.headOption match
          case Some(head) => s"${head.toLower}${stripped.drop(1)}"
          case None       => stripped
      s"A key idea is that $lowered."

  private def tacticalVetoReasons(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): List[String] =
    if allow_soft_veto(ctx, inputs, truthContract) then Nil
    else
      val contractReasons =
        truthContract.toList.flatMap { contract =>
          List(
            Option.when(contract.truthClass == DecisiveTruthClass.Blunder)("truth_contract_blunder"),
            Option.when(contract.truthClass == DecisiveTruthClass.MissedWin)("truth_contract_missed_win"),
            Option.when(contract.reasonFamily == DecisiveReasonKind.TacticalRefutation && contract.isBad)(
              "truth_contract_tactical_refutation"
            ),
            Option.when(contract.failureMode == FailureInterpretationMode.TacticalRefutation)(
              "truth_contract_tactical_failure_mode"
            )
          ).flatten
        }
      val inputReasons =
        List(
          Option.when(inputs.truthMode == PlayerFacingTruthMode.Tactical)("planner_truth_mode_tactical"),
          Option.when(inputs.mainBundle.flatMap(_.mainClaim).exists(_.mode == PlayerFacingTruthMode.Tactical))(
            "main_claim_tactical"
          )
        ).flatten
      val ctxReasons =
        ctx.toList.flatMap { narrativeCtx =>
          val tactical = TacticalTensionPolicy.evaluate(narrativeCtx, truthContract)
          List(
            Option.when(tactical.severeCounterfactual)("context_severe_counterfactual")
          ).flatten
        }
      (contractReasons ++ inputReasons ++ ctxReasons).distinct

  private def allow_soft_veto(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    if inputs.truthMode == PlayerFacingTruthMode.Tactical ||
      inputs.mainBundle.flatMap(_.mainClaim).exists(_.mode == PlayerFacingTruthMode.Tactical)
    then false
    else
      val cpLoss = truthContract.map(_.cpLoss)
        .orElse(inputs.counterfactual.map(_.cpLoss))
        .orElse(inputs.decisionComparison.flatMap(_.cpLossVsChosen))
      val isTacticalFailure = truthContract.exists { contract =>
        contract.truthClass == DecisiveTruthClass.Blunder ||
          contract.truthClass == DecisiveTruthClass.MissedWin ||
          (contract.reasonFamily == DecisiveReasonKind.TacticalRefutation && contract.isBad) ||
          contract.failureMode == FailureInterpretationMode.TacticalRefutation
      }
      val severeCounterfactual =
        ctx.exists(narrativeCtx => TacticalTensionPolicy.evaluate(narrativeCtx, truthContract).severeCounterfactual)
      !isTacticalFailure && !severeCounterfactual && cpLoss.exists(_ <= 30)

  private def supportsLocalPositionProbe(packet: PlayerFacingClaimPacket): Boolean =
    packet.scope == PlayerFacingPacketScope.PositionLocal &&
      packet.fallbackMode == PlayerFacingClaimFallbackMode.WeakMain &&
      packet.suppressionReasons.isEmpty &&
      packet.releaseRisks.isEmpty &&
      supportedLocalPositionProbeAdmissible(packet) &&
      PlayerFacingClaimProof.allowsWeakMainClaim(packet)

  private def supportedLocalPositionProbeAdmissible(packet: PlayerFacingClaimPacket): Boolean =
    ProofContractRules.contractForPacket(packet).exists { contract =>
      contract.supportedLocalEligible &&
        contract.accepts(packet) &&
        ProofContractRules.failureCodes(packet, Some(contract)).isEmpty
    }

  private def decideSupportedMoveDelta(
      inputs: QuestionPlannerInputs,
      plan: QuestionPlan
  ): Option[ClaimAuthorityDecision] =
    matchingMoveDeltaPacket(inputs, plan)
      .filter(packet =>
        supportsLocalMoveDelta(packet) &&
          (!hasExactOwnerPath(packet) || exactMoveDeltaSupportedLocal(packet))
      )
      .map(_ => ClaimAuthorityDecision(ClaimAuthorityTier.SupportedLocal))

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
      List(bundle.mainClaim, bundle.lineScopedClaim).flatten.flatMap(_.packet)
    }

  private def exactMoveDeltaSupportedLocal(packet: PlayerFacingClaimPacket): Boolean =
    (packet.proofSource == ProofSourceId.CounterplayAxisSuppression.wireKey &&
      packet.proofFamily == ProofFamilyId.NeutralizeKeyBreak.wireKey) ||
      (packet.proofSource == ProofSourceId.ProphylacticMove.wireKey &&
        packet.proofFamily == ProofFamilyId.CounterplayRestraint.wireKey) ||
      (packet.proofSource == ProofSourceId.LocalFileEntryBind.wireKey &&
        packet.proofFamily == ProofFamilyId.HalfOpenFilePressure.wireKey)

  private def matchingMoveDeltaPacket(
      inputs: QuestionPlannerInputs,
      plan: QuestionPlan
  ): Option[PlayerFacingClaimPacket] =
    Option.when(plan.plannerOwnerKind == PlannerOwnerKind.MoveDelta) {
      inputs.mainBundle.flatMap(_.mainClaim).filter(claim =>
        claim.scope == PlayerFacingClaimScope.MoveLocal &&
          claim.sourceKind == plan.plannerSource &&
          sameText(claim.claimText, plan.claim)
      ).flatMap(_.packet)
    }.flatten

  private def supportsLocalMoveDelta(packet: PlayerFacingClaimPacket): Boolean =
    packet.scope == PlayerFacingPacketScope.MoveLocal &&
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

  private def timingWitnessMatchesPacket(
      plan: QuestionPlan,
      packet: PlayerFacingClaimPacket
  ): Boolean =
    val packetTokens = timingWitnessTokens(packet)
    plan.timingWitness.exists { witness =>
      witness.proofFamily == packet.proofFamily &&
        witness.source == plan.plannerSource &&
        timingWitnessTokens(witness).exists(packetTokens.contains)
    }

  private def timingWitnessTokens(witness: QuestionPlanTimingWitness): Set[String] =
    (
      witness.namedBreak.toList ++
        witness.continuationMove.toList ++
        witness.branchKey.toList ++
        witness.witnessTokens
    ).flatMap(witnessTokenVariants).filter(validTimingWitnessToken).toSet

  private def timingWitnessTokens(packet: PlayerFacingClaimPacket): Set[String] =
    (
      packet.anchorTerms ++
        packet.bestDefenseBranchKey.toList ++
        packet.proofPathWitness.ownerSeedTerms ++
        packet.proofPathWitness.continuationTerms ++
        packet.proofPathWitness.structureTransitionTerms
    ).flatMap(witnessTokenVariants).filter(validTimingWitnessToken).toSet

  private def centralBreakTimingWitnessMatchesPacket(
      witness: CentralBreakTimingWitness.Witness,
      packet: PlayerFacingClaimPacket
  ): Boolean =
    val witnessTokens =
      (
        witness.ownerSeedTerms ++
          witness.structureTransitionTerms ++
          List(witness.breakMove, witness.breakSquare, witness.breakToken)
      ).flatMap(witnessTokenVariants).filter(validTimingWitnessToken).toSet
    timingWitnessTokens(packet).exists(witnessTokens.contains)

  private def witnessTokenVariants(raw: String): List[String] =
    val trimmed = Option(raw).map(_.trim).filter(_.nonEmpty).toList
    (trimmed ++ trimmed.flatMap(_.split("""[^A-Za-z0-9]+""").toList)).map(normalize)

  private def validTimingWitnessToken(token: String): Boolean =
    val normalized = normalize(token)
    normalized.matches("""[a-h][1-8][a-h][1-8][nbrq]?""") ||
      normalized.matches("""[a-h][1-8]""") ||
      normalized.matches("""[nbrqk][a-h][1-8]""")

  private def stripPrefix(raw: String, prefix: String): Option[String] =
    Option(raw).map(_.trim).filter(_.startsWith(prefix)).map(_.drop(prefix.length))

  private def authorityFailureCodes(packet: PlayerFacingClaimPacket): List[String] =
    val taxonomy =
      ProofContractRules
        .contractForPacket(packet)
        .map(_.defaultFailureTaxonomy)
        .filter(_.nonEmpty)
        .toList
    (taxonomy ++ ProofContractRules.failureCodes(packet)).distinct

  private def sameText(left: String, right: String): Boolean =
    normalize(left) == normalize(right)

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").toLowerCase.replaceAll("""[^a-z0-9\s]""", " ").replaceAll("\\s+", " ").trim
