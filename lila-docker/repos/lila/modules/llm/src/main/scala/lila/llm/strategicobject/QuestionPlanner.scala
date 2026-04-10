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
    val primaryClaims = claimsForAxis(claims, axis)
    val supportClaims =
      if primaryClaims.nonEmpty then supportClaimsForAxis(claims, axis, primaryClaims)
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
    axis match
      case QuestionAxis.WhyThis =>
        val packetOwnedPrimaryClaims =
          claims.filter(TradeInvariantSimplificationSlice.isPacketOwnedPrimarySimplificationClaim)

        if packetOwnedPrimaryClaims.nonEmpty then packetOwnedPrimaryClaims.sortBy(_.id).take(1)
        else
          admissionFor(axis)
            .toList
            .flatMap { admission =>
              claims.filter(admission.primaryClaim)
            }
      case _ =>
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
      axis: QuestionAxis,
      primaryClaims: List[CertifiedClaim]
  ): List[CertifiedClaim] =
    admissionFor(axis)
      .toList
      .flatMap { admission =>
        val candidateSupportClaims = claims.filter(admission.supportClaim)
        axis match
          case QuestionAxis.WhatChanged =>
            exactSharedTargetComparativeSupport(primaryClaims, candidateSupportClaims).toList
          case QuestionAxis.WhatMattersHere =>
            exactCurrentPositionProbeSupport(primaryClaims, candidateSupportClaims)
          case _ =>
            candidateSupportClaims
      }

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

  private final case class ComparativeSupportCandidate(
      primaryClaimId: String,
      supportClaimId: String,
      supportClaim: CertifiedClaim,
      primaryTargetRankScore: Int,
      supportRestrictionScore: Int,
      sharedTargetCount: Int,
      supportWitnessScore: Int,
      supportTargetOverlapScore: Int,
      primaryStandingScore: Int,
      primaryCentralityScore: Int
  )

  private enum CurrentPositionProbeKind:
    case FixedTarget
    case Coordination

  private def exactCurrentPositionProbeSupport(
      primaryClaims: List[CertifiedClaim],
      supportClaims: List[CertifiedClaim]
  ): List[CertifiedClaim] =
    val primaryKinds = primaryClaims.flatMap(currentPositionProbeKind).toSet
    supportClaims.filter(claim =>
      currentPositionProbeKind(claim).exists(primaryKinds.contains)
    )

  private def currentPositionProbeKind(
      claim: CertifiedClaim
  ): Option[CurrentPositionProbeKind] =
    claim.delta.flatMap { delta =>
      if CurrentPositionProbeSlice.isFixedTargetProbeDelta(delta) then Some(CurrentPositionProbeKind.FixedTarget)
      else if CurrentPositionProbeSlice.isCoordinationProbeDelta(delta) then Some(CurrentPositionProbeKind.Coordination)
      else None
    }

  private def exactSharedTargetComparativeSupport(
      primaryClaims: List[CertifiedClaim],
      supportClaims: List[CertifiedClaim]
  ): Option[CertifiedClaim] =
    supportClaims
      .flatMap(supportClaim =>
        primaryClaims.flatMap(primaryClaim =>
          exactSharedTargetComparativeSupportCandidate(primaryClaim, supportClaim)
        )
      )
      .sortBy(candidate =>
        (
          -candidate.primaryCentralityScore,
          -candidate.primaryTargetRankScore,
          -candidate.supportRestrictionScore,
          -candidate.sharedTargetCount,
          -candidate.supportTargetOverlapScore,
          -candidate.supportWitnessScore,
          -candidate.primaryStandingScore,
          candidate.primaryClaimId,
          candidate.supportClaimId
        )
      )
      .headOption
      .map(_.supportClaim)

  private def exactSharedTargetComparativeSupportCandidate(
      primaryClaim: CertifiedClaim,
      supportClaim: CertifiedClaim
  ): Option[ComparativeSupportCandidate] =
    val sharedTargetSquares =
      sharedComparativeTargetSquares(primaryClaim.delta, supportClaim.delta)
    Option.when(isExactSharedTargetComparativeSupport(primaryClaim, supportClaim)) {
      ComparativeSupportCandidate(
        primaryClaimId = primaryClaim.id,
        supportClaimId = supportClaim.id,
        supportClaim = supportClaim,
        primaryTargetRankScore = primaryTargetRankScore(primaryClaim.delta),
        supportRestrictionScore = restrictionSupportOverlapScore(primaryClaim.delta, supportClaim.delta),
        sharedTargetCount = sharedTargetSquares.size,
        supportWitnessScore = comparativeWitnessSpecificityScore(supportClaim.delta),
        supportTargetOverlapScore = supportTargetOverlapScore(supportClaim.delta, sharedTargetSquares),
        primaryStandingScore = comparativeStandingScore(primaryClaim.delta),
        primaryCentralityScore = comparativeCentralityScore(primaryClaim.delta)
      )
    }

  private def isExactSharedTargetComparativeSupport(
      primaryClaim: CertifiedClaim,
      supportClaim: CertifiedClaim
  ): Boolean =
    (primaryClaim.delta, supportClaim.delta) match
      case (Some(primaryDelta), Some(supportDelta)) =>
        primaryDelta.scope == StrategicDeltaScope.Comparative &&
          supportDelta.scope == StrategicDeltaScope.Comparative &&
          primaryDelta.family == StrategicObjectFamily.FixedTargetComplex &&
          supportDelta.family == StrategicObjectFamily.RestrictionShell &&
          primaryDelta.owner == supportDelta.owner &&
          primaryDelta.comparativeProfile.exists(profile =>
            Set(
              StrategicComparativeAxis.FixedTargetPressureContrast,
              StrategicComparativeAxis.FixedTargetDefenseContrast
            ).contains(profile.axis)
          ) &&
          supportDelta.comparativeProfile.exists(_.axis == StrategicComparativeAxis.RestrictionContainmentContrast) &&
          comparativeSupportCorroborationCount(supportDelta) >= 2 &&
          sharedComparativeTargetSquares(Some(primaryDelta), Some(supportDelta)).nonEmpty
      case _ =>
        false

  private def sharedComparativeTargetSquares(
      primaryDelta: Option[StrategicObjectDelta],
      supportDelta: Option[StrategicObjectDelta]
  ): Set[String] =
    (primaryDelta, supportDelta) match
      case (Some(primary), Some(support)) =>
        primaryTargetSquares(primary).intersect(exactComparativeSupportSquares(support))
      case _ =>
        Set.empty

  private def primaryTargetSquares(
      delta: StrategicObjectDelta
  ): Set[String] =
    delta.changedAnchors
      .flatMap(_.squares)
      .map(_.key)
      .toSet

  private def comparativeStandingScore(
      delta: Option[StrategicObjectDelta]
  ): Int =
    delta match
      case Some(
            StrategicObjectDelta(
              _,
              StrategicObjectFamily.FixedTargetComplex,
              _,
              StrategicDeltaScope.Comparative,
              _,
              StrategicDeltaProjection.Comparative(_, balance, _, _, _),
              _,
              _,
              _,
              _
            )
          ) =>
        balance.standing match
          case ComparativeStanding.Ahead     => 8
          case ComparativeStanding.Contested => 4
          case ComparativeStanding.Balanced  => 2
          case ComparativeStanding.Behind    => 0
      case _ =>
        0

  private def comparativeCentralityScore(
      delta: Option[StrategicObjectDelta]
  ): Int =
    delta
      .toList
      .flatMap(primaryTargetSquares)
      .flatMap(squareKey => chess.Square.fromKey(squareKey).toList)
      .map(square => 3 - math.abs(square.file.char.toInt - 'd'.toInt))
      .maxOption
      .getOrElse(0)

  private def primaryTargetRankScore(
      delta: Option[StrategicObjectDelta]
  ): Int =
    delta
      .toList
      .flatMap(primaryTargetSquares)
      .flatMap(squareKey => squareKey.drop(1).toIntOption)
      .maxOption
      .getOrElse(0)

  private def exactComparativeSupportSquares(
      delta: StrategicObjectDelta
  ): Set[String] =
    delta.profile match
      case StrategicObjectProfile.RestrictionShell(restrictedSquares, _, constraintSquares) =>
        (restrictedSquares ++ constraintSquares).map(_.key).toSet
      case _ =>
        delta.changedAnchors
          .flatMap(_.squares)
          .map(_.key)
          .toSet

  private def restrictionSupportOverlapScore(
      primaryDelta: Option[StrategicObjectDelta],
      supportDelta: Option[StrategicObjectDelta]
  ): Int =
    (primaryDelta, supportDelta) match
      case (Some(primary), Some(StrategicObjectDelta(_, _, _, _, StrategicObjectProfile.RestrictionShell(restrictedSquares, _, constraintSquares), _, _, _, _, _))) =>
        val primaryTargets = primaryTargetSquares(primary)
        if primaryTargets.intersect(restrictedSquares.map(_.key).toSet).nonEmpty then 2
        else if primaryTargets.intersect(constraintSquares.map(_.key).toSet).nonEmpty then 1
        else 0
      case _ =>
        0

  private def comparativeWitnessSpecificityScore(
      delta: Option[StrategicObjectDelta]
  ): Int =
    delta.flatMap(_.comparativeWitness).map { witness =>
      witness.counterpartWitnessKinds.toList.map {
        case StrategicCounterpartWitnessKind.SharedTarget         => 16
        case StrategicCounterpartWitnessKind.SharedSquare         => 8
        case StrategicCounterpartWitnessKind.SharedFile           => 4
        case StrategicCounterpartWitnessKind.SharedRoute          => 2
        case StrategicCounterpartWitnessKind.DirectRivalReference => 2
        case StrategicCounterpartWitnessKind.SharedPiece          => 1
      }.sum + witness.matchedSquares.size + witness.matchedFiles.size + witness.relationWitnesses.size
    }.getOrElse(0)

  private def supportTargetOverlapScore(
      delta: Option[StrategicObjectDelta],
      sharedTargetSquares: Set[String]
  ): Int =
    delta
      .map(exactComparativeSupportSquares)
      .map(_.intersect(sharedTargetSquares).size)
      .getOrElse(0)

  private def comparativeSupportCorroborationCount(
      delta: StrategicObjectDelta
  ): Int =
    delta.supportingObjectIds.distinct.size

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
      claim.delta.exists(delta =>
        CurrentPositionProbeSlice.isFixedTargetProbeDelta(delta) ||
          CurrentPositionProbeSlice.isCoordinationProbeDelta(delta)
      )
