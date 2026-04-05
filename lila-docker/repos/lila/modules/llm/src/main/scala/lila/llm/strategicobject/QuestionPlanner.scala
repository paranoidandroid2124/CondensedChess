package lila.llm.strategicobject

import lila.llm.analysis.DecisiveTruthContract

enum QuestionAxis:
  case WhyThis
  case WhatChanged
  case WhatMattersHere
  case WhyNow
  case WhatMustBeStopped

final case class PlannedQuestion(
    axis: QuestionAxis,
    claimIds: List[String],
    supportClaimIds: List[String] = Nil
)

trait QuestionPlanner:
  def plan(
      contract: DecisiveTruthContract,
      claims: List[CertifiedClaim]
  ): PlannedQuestion

object CanonicalQuestionPlanner extends QuestionPlanner:

  def plan(
      contract: DecisiveTruthContract,
      claims: List[CertifiedClaim]
  ): PlannedQuestion =
    val primaryClaims =
      claims.filter(claim =>
        claim.status == ClaimStatus.Certified &&
          claim.readiness == StrategicObjectReadiness.Stable &&
          claim.hasTypedDelta
      )
    val supportClaims =
      claims.filter(claim =>
        claim.status == ClaimStatus.SupportOnly &&
          claim.readiness == StrategicObjectReadiness.Provisional &&
          claim.hasTypedDelta
      )
    val axis = chooseAxis(contract, primaryClaims, supportClaims)
    PlannedQuestion(
      axis = axis,
      claimIds = claimsForAxis(primaryClaims, axis).map(_.id),
      supportClaimIds = claimsForAxis(supportClaims, axis).map(_.id)
    )

  private def chooseAxis(
      contract: DecisiveTruthContract,
      primaryClaims: List[CertifiedClaim],
      supportClaims: List[CertifiedClaim]
  ): QuestionAxis =
    val pool = if primaryClaims.nonEmpty then primaryClaims else supportClaims
    if pool.exists(isTypedMoveLocal) && contract.isBad then QuestionAxis.WhatMustBeStopped
    else if pool.exists(isTypedMoveLocal) then QuestionAxis.WhyThis
    else if pool.exists(isTypedComparative) then QuestionAxis.WhatChanged
    else if pool.exists(isTypedPositionLocal) then QuestionAxis.WhatMattersHere
    else QuestionAxis.WhatMattersHere

  private def claimsForAxis(
      claims: List[CertifiedClaim],
      axis: QuestionAxis
  ): List[CertifiedClaim] =
    val matching = claims.filter(claimMatchesAxis(_, axis))
    if matching.nonEmpty then matching else claims

  private def claimMatchesAxis(
      claim: CertifiedClaim,
      axis: QuestionAxis
  ): Boolean =
    axis match
      case QuestionAxis.WhyThis | QuestionAxis.WhyNow | QuestionAxis.WhatMustBeStopped =>
        isTypedMoveLocal(claim)
      case QuestionAxis.WhatChanged =>
        isTypedComparative(claim)
      case QuestionAxis.WhatMattersHere =>
        isTypedPositionLocal(claim)

  private def isTypedMoveLocal(
      claim: CertifiedClaim
  ): Boolean =
    claim.delta.exists(_.projection match
      case StrategicDeltaProjection.MoveLocal(_, transition) =>
        transition.isTransitionAware
      case _ =>
        false
    )

  private def isTypedComparative(
      claim: CertifiedClaim
  ): Boolean =
    claim.delta.exists(_.projection match
      case StrategicDeltaProjection.Comparative(_, _, witness, counterpartObjectIds, profile) =>
        witness.isFamilyAware &&
          counterpartObjectIds.nonEmpty &&
          profile.metrics.nonEmpty
      case _ =>
        false
    )

  private def isTypedPositionLocal(
      claim: CertifiedClaim
  ): Boolean =
    claim.delta.exists(_.projection match
      case StrategicDeltaProjection.PositionLocal(_, focalAnchorCount) =>
        focalAnchorCount > 0
      case _ =>
        false
    )
