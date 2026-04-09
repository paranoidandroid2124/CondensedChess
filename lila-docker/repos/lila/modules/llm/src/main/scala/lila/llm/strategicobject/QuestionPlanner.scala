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
      score: Int
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
          -candidate.score,
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
    Option.when(isExactSharedTargetComparativeSupport(primaryClaim, supportClaim)) {
      ComparativeSupportCandidate(
        primaryClaimId = primaryClaim.id,
        supportClaimId = supportClaim.id,
        supportClaim = supportClaim,
        score =
          comparativeStandingScore(primaryClaim.delta) +
            comparativeCentralityScore(primaryClaim.delta) +
            sharedComparativeTargetSquares(primaryClaim.delta, supportClaim.delta).size
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
          sharedComparativeTargetSquares(Some(primaryDelta), Some(supportDelta)).nonEmpty
      case _ =>
        false

  private def sharedComparativeTargetSquares(
      primaryDelta: Option[StrategicObjectDelta],
      supportDelta: Option[StrategicObjectDelta]
  ): Set[String] =
    (primaryDelta, supportDelta) match
      case (Some(primary), Some(support)) =>
        primaryTargetSquares(primary).intersect(comparativeSquares(support))
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

  private def comparativeSquares(
      delta: StrategicObjectDelta
  ): Set[String] =
    (
      delta.changedAnchors.flatMap(_.squares) ++
        delta.evidenceRefs.flatMap(_.anchorSquares) ++
        delta.evidenceRefs.flatMap(_.contestedSquares)
    ).map(_.key).toSet

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
