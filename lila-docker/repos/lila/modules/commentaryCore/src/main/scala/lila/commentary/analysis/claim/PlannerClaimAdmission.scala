package lila.commentary.analysis.claim

import lila.commentary.analysis.*
import lila.commentary.model.NarrativeContext

private[commentary] object PlannerClaimAdmission:

  def decidePositionProbe(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      packet: PlayerFacingClaimPacket
  ): ClaimAuthorityDecision =
    ClaimAuthorityResolver.decidePositionProbe(ctx, inputs, truthContract, packet)

  def planAuthorityDecision(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      plan: QuestionPlan
  ): Option[ClaimAuthorityDecision] =
    ClaimAuthorityResolver.planAuthorityDecision(ctx, inputs, truthContract, plan)

  def supportedLocalSurface(raw: String): String =
    ClaimAuthorityResolver.supportedLocalSurface(raw)
