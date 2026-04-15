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

  private final case class PrimaryClaimSelection(
      primaryClaims: List[CertifiedClaim],
      demotedClaims: List[CertifiedClaim] = Nil
  )

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
        primaryAllowed = _.isPrimaryVisible,
        primaryClaim = isCertifiedTypedComparative,
        supportClaim = isSupportOnlyTypedComparative
      ),
      QuestionAdmission(
        axis = QuestionAxis.WhatMattersHere,
        primaryClaim = isCertifiedCurrentPositionProbe,
        supportClaim = isSupportOnlyCurrentPositionProbe
      )
    )

  def plan(
      contract: DecisiveTruthContract,
      claims: List[CertifiedClaim]
  ): PlannedQuestion =
    val axis = chooseAxis(contract, claims)
    val primarySelection = claimsForAxis(claims, axis)
    val supportClaims =
      if primarySelection.primaryClaims.nonEmpty || primarySelection.demotedClaims.nonEmpty
      then supportClaimsForAxis(claims, axis, primarySelection.primaryClaims, primarySelection.demotedClaims)
      else Nil
    PlannedQuestion(
      axis = axis,
      claimIds = primarySelection.primaryClaims.map(_.id),
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
  ): PrimaryClaimSelection =
    axis match
      case QuestionAxis.WhyThis =>
        val packetOwnedPrimaryClaims =
          claims.filter(isTradeInvariantPacketPrimaryClaim)

        if packetOwnedPrimaryClaims.nonEmpty then
          PrimaryClaimSelection(primaryClaims = packetOwnedPrimaryClaims.sortBy(_.id).take(1))
        else
          arbitratePrimaryClaims(
            axis,
            basePrimaryClaimsForAxis(claims, axis),
            baseSupportClaimsForAxis(claims, axis)
          )
      case _ =>
        arbitratePrimaryClaims(
          axis,
          basePrimaryClaimsForAxis(claims, axis),
          baseSupportClaimsForAxis(claims, axis)
        )

  private def basePrimaryClaimsForAxis(
      claims: List[CertifiedClaim],
      axis: QuestionAxis
  ): List[CertifiedClaim] =
    admissionFor(axis)
      .toList
      .flatMap { admission =>
        claims.filter(admission.primaryClaim)
      }
      .sortBy(_.id)

  private def baseSupportClaimsForAxis(
      claims: List[CertifiedClaim],
      axis: QuestionAxis
  ): List[CertifiedClaim] =
    admissionFor(axis)
      .toList
      .flatMap { admission =>
        claims.filter(admission.supportClaim)
      }
      .sortBy(_.id)

  private def admissionFor(
      axis: QuestionAxis
  ): Option[QuestionAdmission] =
    admissionMatrix.find(_.axis == axis)

  private def supportClaimsForAxis(
      claims: List[CertifiedClaim],
      axis: QuestionAxis,
      primaryClaims: List[CertifiedClaim],
      demotedClaims: List[CertifiedClaim]
  ): List[CertifiedClaim] =
    admissionFor(axis)
      .toList
      .flatMap { _ =>
        val candidateSupportClaims =
          dedupeClaims(baseSupportClaimsForAxis(claims, axis) ++ demotedClaims)
        axis match
          case QuestionAxis.WhatChanged =>
            exactSharedTargetComparativeSupport(primaryClaims, candidateSupportClaims).toList
          case QuestionAxis.WhatMattersHere =>
            exactCurrentPositionProbeSupport(primaryClaims, candidateSupportClaims)
          case _ =>
            exactMoveLocalSupportClaims(claims, candidateSupportClaims)
      }

  private val moveLocalPrimaryAxes =
    Set(
      QuestionAxis.WhatMustBeStopped,
      QuestionAxis.WhyNow,
      QuestionAxis.WhyThis
    )

  private def arbitratePrimaryClaims(
      axis: QuestionAxis,
      primaryClaims: List[CertifiedClaim],
      supportClaims: List[CertifiedClaim]
  ): PrimaryClaimSelection =
    if !moveLocalPrimaryAxes.contains(axis) then PrimaryClaimSelection(primaryClaims = primaryClaims)
    else
      val residualCandidates =
        dedupeClaims(primaryClaims.filterNot(isAccessNetworkClaim) ++ supportClaims)
      val demotedClaims =
        primaryClaims.filter(claim =>
          isAccessNetworkClaim(claim) &&
            residualCandidates.exists(other => provesSpecificResidualExplanation(claim, other))
        )
      PrimaryClaimSelection(
        primaryClaims = primaryClaims.filterNot(demotedClaims.contains),
        demotedClaims = demotedClaims
      )

  private def provesSpecificResidualExplanation(
      accessClaim: CertifiedClaim,
      other: CertifiedClaim
  ): Boolean =
    isSpecificResidualDemotionCandidate(other) &&
      AccessNetworkBridgeAdmissionBoundary.admits(accessClaim, other)

  private def isAccessNetworkClaim(
      claim: CertifiedClaim
  ): Boolean =
    claim.delta.exists(_.family == StrategicObjectFamily.AccessNetwork)

  private def isTradeInvariantPacketPrimaryClaim(
      claim: CertifiedClaim
  ): Boolean =
    claim.status == ClaimStatus.Certified &&
      claim.deltaScope == StrategicDeltaScope.MoveLocal &&
      isTypedMoveLocal(claim) &&
      claim.plannerMetadata.tradeInvariantPrimaryClass.contains(
        TradeInvariantPrimaryReason.PacketOwnedFixedTargetSlice
      )

  private def isSpecificResidualDemotionCandidate(
      claim: CertifiedClaim
  ): Boolean =
    claim.plannerMetadata.residualSpecificityClass.nonEmpty

  private def dedupeClaims(
      claims: List[CertifiedClaim]
  ): List[CertifiedClaim] =
    claims.groupBy(_.id).toList.sortBy(_._1).map(_._2.head)

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

  private def exactCurrentPositionProbeSupport(
      primaryClaims: List[CertifiedClaim],
      supportClaims: List[CertifiedClaim]
  ): List[CertifiedClaim] =
    val fixedTargetSupport =
      supportClaims.filter(supportClaim =>
        primaryClaims.exists(primaryClaim =>
          currentPositionProbeKind(primaryClaim).contains(CertifiedCurrentPositionProbeKind.FixedTarget) &&
            FixedTargetClusterWitnessBoundary.sharesClusterWitness(primaryClaim, supportClaim)
        )
      )
    val coordinationSupport =
      supportClaims.filter(supportClaim =>
        primaryClaims.exists(primaryClaim =>
          currentPositionProbeKind(primaryClaim).contains(CertifiedCurrentPositionProbeKind.Coordination) &&
            currentPositionProbeKind(supportClaim).contains(CertifiedCurrentPositionProbeKind.Coordination) &&
            CurrentPositionProbeSlice.sharesCoordinationProbeWitness(primaryClaim, supportClaim)
          )
      )

    dedupeClaims(fixedTargetSupport ++ coordinationSupport)

  private def currentPositionProbeKind(
      claim: CertifiedClaim
  ): Option[CertifiedCurrentPositionProbeKind] =
    claim.plannerMetadata.currentPositionProbeKind

  private def exactSharedTargetComparativeSupport(
      primaryClaims: List[CertifiedClaim],
      supportClaims: List[CertifiedClaim]
  ): Option[CertifiedClaim] =
    val continuityPrimaryClaims =
      primaryClaims.filter(_.plannerMetadata.sharedTargetContinuity)

    Option.when(continuityPrimaryClaims.nonEmpty) {
      supportClaims
        .filter(supportClaim =>
          continuityPrimaryClaims.exists(primaryClaim =>
            SharedTargetContinuityBoundary.sharesPacketContinuity(primaryClaim, supportClaim)
          )
        )
        .sortBy(_.id)
        .headOption
    }.flatten

  private def exactMoveLocalSupportClaims(
      claims: List[CertifiedClaim],
      supportClaims: List[CertifiedClaim]
  ): List[CertifiedClaim] =
    supportClaims.filter(supportClaim =>
      CounterplayRivalBurdenBoundary.plannerSupportEligible(claims, supportClaim)
    )

  private def isCertifiedCurrentPositionProbe(
      claim: CertifiedClaim
  ): Boolean =
    isCurrentPositionProbe(claim, ClaimStatus.Certified)

  private def isSupportOnlyCurrentPositionProbe(
      claim: CertifiedClaim
  ): Boolean =
    isCurrentPositionProbe(claim, ClaimStatus.SupportOnly)

  private def isCurrentPositionProbe(
      claim: CertifiedClaim,
      status: ClaimStatus
  ): Boolean =
    claim.status == status &&
      claim.hasTypedDelta &&
      (
        isFixedTargetCurrentPositionProbe(claim, status) ||
          isCoordinationCurrentPositionProbe(claim)
      )

  private def isFixedTargetCurrentPositionProbe(
      claim: CertifiedClaim,
      status: ClaimStatus
  ): Boolean =
    currentPositionProbeKind(claim).contains(CertifiedCurrentPositionProbeKind.FixedTarget) &&
      claim.delta.exists(delta =>
        delta.scope == StrategicDeltaScope.PositionLocal &&
          (
            (status == ClaimStatus.Certified && delta.family == StrategicObjectFamily.FixedTargetComplex) ||
              (status == ClaimStatus.SupportOnly && delta.family == StrategicObjectFamily.RestrictionShell)
          )
      )

  private def isCoordinationCurrentPositionProbe(
      claim: CertifiedClaim
  ): Boolean =
    currentPositionProbeKind(claim).contains(CertifiedCurrentPositionProbeKind.Coordination)
