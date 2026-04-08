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

  private final case class QuestionAdmission(
      axis: QuestionAxis,
      primaryAllowed: DecisiveTruthContract => Boolean = _ => true,
      primaryClaim: CertifiedClaim => Boolean,
      supportClaim: CertifiedClaim => Boolean
  )

  private val admissionMatrix: List[QuestionAdmission] =
    List(
      QuestionAdmission(
        axis = QuestionAxis.WhatMustBeStopped,
        primaryAllowed = _.isBad,
        primaryClaim = isCertifiedTypedMoveLocal,
        supportClaim = isSupportOnlyTypedMoveLocal
      ),
      QuestionAdmission(
        axis = QuestionAxis.WhyNow,
        primaryAllowed = contract => !contract.isBad,
        primaryClaim = isCertifiedTimingSensitiveMoveLocal,
        supportClaim = isSupportOnlyTimingSensitiveMoveLocal
      ),
      QuestionAdmission(
        axis = QuestionAxis.WhyThis,
        primaryAllowed = contract => !contract.isBad,
        primaryClaim = isCertifiedTypedMoveLocal,
        supportClaim = isSupportOnlyTypedMoveLocal
      ),
      QuestionAdmission(
        axis = QuestionAxis.WhatChanged,
        primaryClaim = isCertifiedTypedComparative,
        supportClaim = isSupportOnlyTypedComparative
      ),
      QuestionAdmission(
        axis = QuestionAxis.WhatMattersHere,
        primaryClaim = isCertifiedTypedPositionLocal,
        supportClaim = isSupportOnlyTypedPositionLocal
      )
    )

  def plan(
      contract: DecisiveTruthContract,
      claims: List[CertifiedClaim]
  ): PlannedQuestion =
    val axis = chooseAxis(contract, claims)
    val primaryClaims = claimsForAxis(claims, axis)
    val supportClaims =
      if primaryClaims.nonEmpty then supportClaimsForAxis(claims, axis)
      else Nil
    PlannedQuestion(
      axis = axis,
      claimIds = primaryClaims.map(_.id),
      supportClaimIds = supportClaims.map(_.id)
    )

  private def chooseAxis(
      contract: DecisiveTruthContract,
      claims: List[CertifiedClaim]
  ): QuestionAxis =
    admissionMatrix
      .collectFirst {
        case admission
            if admission.primaryAllowed(contract) &&
              claims.exists(admission.primaryClaim) =>
          admission.axis
      }
      .getOrElse(QuestionAxis.WhatMattersHere)

  private def claimsForAxis(
      claims: List[CertifiedClaim],
      axis: QuestionAxis
  ): List[CertifiedClaim] =
    admissionFor(axis)
      .toList
      .flatMap { admission =>
        claims.filter(admission.primaryClaim)
      }

  private def admissionFor(
      axis: QuestionAxis
  ): Option[QuestionAdmission] =
    admissionMatrix.find(_.axis == axis)

  private def supportClaimsForAxis(
      claims: List[CertifiedClaim],
      axis: QuestionAxis
  ): List[CertifiedClaim] =
    admissionFor(axis)
      .toList
      .flatMap(admission => claims.filter(admission.supportClaim))

  private def isCertifiedTypedMoveLocal(
      claim: CertifiedClaim
  ): Boolean =
    claim.status == ClaimStatus.Certified &&
      claim.hasTypedDelta &&
      isTypedMoveLocal(claim)

  private def isSupportOnlyTypedMoveLocal(
      claim: CertifiedClaim
  ): Boolean =
    claim.status == ClaimStatus.SupportOnly &&
      claim.hasTypedDelta &&
      isTypedMoveLocal(claim)

  private def isCertifiedTimingSensitiveMoveLocal(
      claim: CertifiedClaim
  ): Boolean =
    claim.status == ClaimStatus.Certified &&
      claim.hasTypedDelta &&
      isTimingSensitiveMoveLocal(claim)

  private def isSupportOnlyTimingSensitiveMoveLocal(
      claim: CertifiedClaim
  ): Boolean =
    claim.status == ClaimStatus.SupportOnly &&
      claim.hasTypedDelta &&
      isTimingSensitiveMoveLocal(claim)

  private def isTypedMoveLocal(
      claim: CertifiedClaim
  ): Boolean =
    claim.delta.exists(_.projection match
      case StrategicDeltaProjection.MoveLocal(_, transition) =>
        transition.isTransitionAware
      case _ =>
        false
    )

  private val timingSensitiveMoveLocalTags =
    Set(
      StrategicDeltaTag.BreakAccelerated,
      StrategicDeltaTag.BreakDelayed,
      StrategicDeltaTag.RouteShortened,
      StrategicDeltaTag.PasserAccelerated
    )

  private def isTimingSensitiveMoveLocal(
      claim: CertifiedClaim
  ): Boolean =
    claim.delta.exists(_.projection match
      case StrategicDeltaProjection.MoveLocal(change, witness) =>
        timingSensitiveMoveLocalTags.contains(change) ||
          witness.primitiveKinds.contains(PrimitiveKind.ReleaseCandidate)
      case _ =>
        false
    )

  private def isCertifiedTypedComparative(
      claim: CertifiedClaim
  ): Boolean =
    claim.status == ClaimStatus.Certified &&
      claim.hasTypedDelta &&
      isTypedComparative(claim)

  private def isSupportOnlyTypedComparative(
      claim: CertifiedClaim
  ): Boolean =
    claim.status == ClaimStatus.SupportOnly &&
      claim.hasTypedDelta &&
      isTypedComparative(claim)

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

  private def isCertifiedTypedPositionLocal(
      claim: CertifiedClaim
  ): Boolean =
    claim.status == ClaimStatus.Certified &&
      claim.hasTypedDelta &&
      isTypedPositionLocal(claim)

  private def isSupportOnlyTypedPositionLocal(
      claim: CertifiedClaim
  ): Boolean =
    claim.status == ClaimStatus.SupportOnly &&
      claim.hasTypedDelta &&
      isTypedPositionLocal(claim)

  private def isTypedPositionLocal(
      claim: CertifiedClaim
  ): Boolean =
    claim.delta.exists(_.projection match
      case StrategicDeltaProjection.PositionLocal(_, focalAnchorCount) =>
        focalAnchorCount > 0
      case _ =>
        false
    )
