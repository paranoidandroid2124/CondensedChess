package lila.commentary.analysis.claim

import lila.commentary.analysis.*
import lila.commentary.model.*

private[commentary] enum ClaimAuthorityTier:
  case CertifiedOwner
  case SupportedLocal
  case Suppressed

private[commentary] final case class ClaimAuthorityDecision(
    tier: ClaimAuthorityTier,
    vetoReasons: List[String] = Nil
):
  def admitted: Boolean =
    tier != ClaimAuthorityTier.Suppressed

private[commentary] object ClaimAuthorityPolicy:

  def decidePositionProbe(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      packet: PlayerFacingClaimPacket
  ): ClaimAuthorityDecision =
    val tacticalReasons = tacticalVetoReasons(ctx, inputs, truthContract)
    if tacticalReasons.nonEmpty && isSupportedPositionProbeFamily(packet.ownerFamily) then
      ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed, tacticalReasons)
    else if PlayerFacingTruthModePolicy.certifiedPositionProbePacket(packet) &&
        OwnerProofRules.certifiedEligible(packet.ownerFamily)
    then
      ClaimAuthorityDecision(ClaimAuthorityTier.CertifiedOwner)
    else if supportsLocalPositionProbe(packet) then
      ClaimAuthorityDecision(ClaimAuthorityTier.SupportedLocal)
    else
      ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed)

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

  def planAuthorityDecision(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      plan: QuestionPlan
  ): Option[ClaimAuthorityDecision] =
    shouldTacticalVetoPlan(ctx, inputs, truthContract, plan)
      .orElse(decideSupportedMoveDelta(inputs, plan))

  def supportedLocalSurface(raw: String): String =
    val stripped =
      stripPrefix(raw, "The key strategic fact here is that ")
        .orElse(stripPrefix(raw, "The strategic point is that "))
        .orElse(stripPrefix(raw, "This shows that "))
        .getOrElse(raw.trim)
        .stripSuffix(".")
        .trim
    val lowered =
      stripped.headOption match
        case Some(head) => s"${head.toLower}${stripped.drop(1)}"
        case None       => stripped
    s"A local reading is that $lowered."

  private def tacticalVetoReasons(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): List[String] =
    val contractReasons =
      truthContract.toList.flatMap { contract =>
        List(
          Option.when(contract.truthClass == DecisiveTruthClass.Blunder)("truth_contract_blunder"),
          Option.when(contract.truthClass == DecisiveTruthClass.MissedWin)("truth_contract_missed_win"),
          Option.when(contract.reasonFamily == DecisiveReasonFamily.TacticalRefutation && contract.isBad)(
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

  private def supportsLocalPositionProbe(packet: PlayerFacingClaimPacket): Boolean =
    packet.scope == PlayerFacingPacketScope.PositionLocal &&
      packet.fallbackMode == PlayerFacingClaimFallbackMode.WeakMain &&
      packet.suppressionReasons.isEmpty &&
      packet.releaseRisks.isEmpty &&
      isSupportedPositionProbeFamily(packet.ownerFamily) &&
      OwnerProofRules.supportedLocalEligible(packet.ownerFamily) &&
      PlayerFacingClaimProof.allowsWeakMainClaim(packet)

  private def decideSupportedMoveDelta(
      inputs: QuestionPlannerInputs,
      plan: QuestionPlan
  ): Option[ClaimAuthorityDecision] =
    matchingMoveDeltaPacket(inputs, plan)
      .filter(packet =>
        supportsLocalMoveDelta(packet) &&
          (!hasExactOwnerPath(packet) || exactBreakMoveDeltaSupportedLocal(packet))
      )
      .map(_ => ClaimAuthorityDecision(ClaimAuthorityTier.SupportedLocal))

  private def exactBreakMoveDeltaSupportedLocal(packet: PlayerFacingClaimPacket): Boolean =
    packet.ownerSource == "counterplay_axis_suppression" &&
      packet.ownerFamily == "neutralize_key_break"

  private def matchingMoveDeltaPacket(
      inputs: QuestionPlannerInputs,
      plan: QuestionPlan
  ): Option[PlayerFacingClaimPacket] =
    Option.when(plan.ownerFamily == OwnerFamily.MoveDelta) {
      inputs.mainBundle.flatMap(_.mainClaim).filter(claim =>
        claim.scope == PlayerFacingClaimScope.MoveLocal &&
          claim.sourceKind == plan.ownerSource &&
          sameText(claim.claimText, plan.claim)
      ).flatMap(_.packet)
    }.flatten

  private def supportsLocalMoveDelta(packet: PlayerFacingClaimPacket): Boolean =
    packet.scope == PlayerFacingPacketScope.MoveLocal &&
      packet.fallbackMode == PlayerFacingClaimFallbackMode.WeakMain &&
      packet.suppressionReasons.isEmpty &&
      packet.releaseRisks.isEmpty &&
      OwnerProofRules.supportsMoveDeltaFamily(packet.ownerFamily) &&
      OwnerProofRules.supportedLocalEligible(packet.ownerFamily) &&
      PlayerFacingClaimProof.allowsWeakMainClaim(packet)

  private def hasExactOwnerPath(packet: PlayerFacingClaimPacket): Boolean =
    OwnerProofRules.certifiedEligible(packet.ownerFamily) &&
      packet.bestDefenseBranchKey.nonEmpty &&
      packet.sameBranchState == PlayerFacingSameBranchState.Proven &&
      packet.persistence == PlayerFacingClaimPersistence.Stable

  private def isSupportedPositionProbePlan(plan: QuestionPlan): Boolean =
    plan.ownerFamily == OwnerFamily.PositionProbe &&
      (
        isSupportedPositionProbeFamily(plan.ownerSource) ||
          plan.sourceKinds.exists(isSupportedPositionProbeFamily) ||
          plan.admissibilityReasons.exists(reason =>
            reason == "strategic_claim_supported_local" ||
              reason == "certified_position_probe"
          )
      )

  private def isSupportedPositionProbeFamily(ownerFamily: String): Boolean =
    OwnerProofRules.supportsPositionProbeFamily(ownerFamily)

  private def stripPrefix(raw: String, prefix: String): Option[String] =
    Option(raw).map(_.trim).filter(_.startsWith(prefix)).map(_.drop(prefix.length))

  private def sameText(left: String, right: String): Boolean =
    normalize(left) == normalize(right)

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").toLowerCase.replaceAll("""[^a-z0-9\s]""", " ").replaceAll("\\s+", " ").trim
