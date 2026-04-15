package lila.llm.strategicobject

import chess.{ File, Square }

import lila.llm.analysis.DecisiveTruthContract

enum ClaimStatus:
  case Certified
  case SupportOnly
  case Deferred
  case Rejected

private enum DeltaCertificationBurden:
  case Primary
  case SupportOnly
  case Insufficient

enum CertifiedBoundaryWitness:
  case SharedTargetContinuity(targetSquare: Square)
  case FixedTargetCluster(witness: FixedTargetClusterWitness)
  case CoordinationProbe(witness: CoordinationProbeWitness)

enum CertifiedCurrentPositionProbeKind:
  case FixedTarget
  case Coordination

enum CertifiedResidualSpecificityClass:
  case TradeInvariantPrimaryExact
  case CounterplayExact
  case PlanRaceExact
  case ConversionFunnelExact

final case class CertifiedRivalLeg(
    family: StrategicObjectFamily,
    objectId: String,
    operator: StrategicRelationOperator,
    matchedSquares: List[Square] = Nil,
    matchedFiles: List[File] = Nil
):
  def normalized: CertifiedRivalLeg =
    copy(
      matchedSquares = matchedSquares.distinct.sortBy(_.key),
      matchedFiles = matchedFiles.distinct.sortBy(_.char.toString)
    )

final case class CertifiedCounterplayRivalBurden(
    coEqualRivalLegs: List[CertifiedRivalLeg] = Nil,
    admittedButUnmatchedRivalLegs: List[CertifiedRivalLeg] = Nil,
    allMatchedRivalLegsMustCertify: Boolean = true
):
  def normalized: CertifiedCounterplayRivalBurden =
    copy(
      coEqualRivalLegs =
        coEqualRivalLegs
          .map(_.normalized)
          .distinct
          .sortBy(leg =>
            s"${leg.objectId}:${leg.family}:${leg.operator}:${leg.matchedSquares.map(_.key).mkString(",")}:${leg.matchedFiles.map(_.char).mkString}"
          ),
      admittedButUnmatchedRivalLegs =
        admittedButUnmatchedRivalLegs
          .map(_.normalized)
          .distinct
          .sortBy(leg =>
            s"${leg.objectId}:${leg.family}:${leg.operator}:${leg.matchedSquares.map(_.key).mkString(",")}:${leg.matchedFiles.map(_.char).mkString}"
          )
    )

final case class CertifiedPlannerMetadata(
    sharedTargetContinuity: Boolean = false,
    currentPositionProbeKind: Option[CertifiedCurrentPositionProbeKind] = None,
    tradeInvariantPrimaryClass: Option[TradeInvariantPrimaryReason] = None,
    residualSpecificityClass: Option[CertifiedResidualSpecificityClass] = None,
    counterplayRivalBurden: Option[CertifiedCounterplayRivalBurden] = None
)

final case class CertifiedClaim(
    id: String,
    objectId: String,
    deltaScope: StrategicDeltaScope,
    status: ClaimStatus,
    readiness: StrategicObjectReadiness,
    delta: Option[StrategicObjectDelta] = None,
    supportingObjectIds: List[String] = Nil,
    boundaryWitnesses: Set[CertifiedBoundaryWitness] = Set.empty,
    plannerMetadata: CertifiedPlannerMetadata = CertifiedPlannerMetadata()
):
  def primaryTag: Option[StrategicDeltaTag] =
    delta.map(_.primaryTag)

  def hasTypedDelta: Boolean =
    delta.nonEmpty

object CanonicalClaimCertification extends ClaimCertification:

  def certify(
      contract: DecisiveTruthContract,
      objects: List[StrategicObject],
      deltas: List[StrategicObjectDelta]
  ): List[CertifiedClaim] =
    val objectsById = objects.map(obj => obj.id -> obj).toMap
    val claimsFromDeltas =
      deltas.flatMap { delta =>
        objectsById.get(delta.objectId).map { obj =>
          val burden = certificationBurden(delta)
          CertifiedClaim(
            id = claimId(obj.id, delta.scope),
            objectId = obj.id,
            deltaScope = delta.scope,
            status = claimStatus(obj.readiness, burden, delta),
            readiness = obj.readiness,
            delta = Some(delta),
            supportingObjectIds = delta.supportingObjectIds,
            boundaryWitnesses = certifiedBoundaryWitnesses(delta)
          )
        }
      }

    val deferredClaims =
      objects
        .filter(_.readiness == StrategicObjectReadiness.DeferredForDelta)
        .map { obj =>
          CertifiedClaim(
            id = claimId(obj.id, StrategicDeltaScope.PositionLocal),
            objectId = obj.id,
            deltaScope = StrategicDeltaScope.PositionLocal,
            status = ClaimStatus.Deferred,
            readiness = obj.readiness,
            delta = None,
            supportingObjectIds = obj.relations.map(_.target.objectId).distinct.take(4)
          )
        }

    enrichPlannerMetadata(
      SharedTargetContinuityBoundary.certify(
        (claimsFromDeltas ++ deferredClaims)
          .groupBy(_.id)
          .values
          .map(_.head)
          .toList
          .sortBy(claim => (claim.objectId, claim.deltaScope.ordinal, claim.status.ordinal))
      )
    )

  private def claimId(
      objectId: String,
      scope: StrategicDeltaScope
  ): String =
    s"$objectId:${scope.toString.toLowerCase}"

  private def claimStatus(
      readiness: StrategicObjectReadiness,
      burden: DeltaCertificationBurden,
      delta: StrategicObjectDelta
  ): ClaimStatus =
    readiness match
      case StrategicObjectReadiness.Stable =>
        burden match
          case DeltaCertificationBurden.Primary      => ClaimStatus.Certified
          case DeltaCertificationBurden.SupportOnly  => ClaimStatus.SupportOnly
          case DeltaCertificationBurden.Insufficient => ClaimStatus.Deferred
      case StrategicObjectReadiness.Provisional =>
        burden match
          case DeltaCertificationBurden.Primary if CurrentPositionProbeSlice.hasCoordinationProbeWitness(delta) =>
            ClaimStatus.Certified
          case DeltaCertificationBurden.Primary | DeltaCertificationBurden.SupportOnly =>
            ClaimStatus.SupportOnly
          case DeltaCertificationBurden.Insufficient =>
            ClaimStatus.Deferred
      case StrategicObjectReadiness.DeferredForDelta => ClaimStatus.Deferred

  private def certificationBurden(
      delta: StrategicObjectDelta
  ): DeltaCertificationBurden =
    val exactBoardSupport = hasExactBoardSupport(delta)
    delta.projection match
      case StrategicDeltaProjection.MoveLocal(_, witness) =>
        if exactBoardSupport && witness.hasAnchoredEvidence then DeltaCertificationBurden.Primary
        else if exactBoardSupport && witness.isTransitionAware then DeltaCertificationBurden.SupportOnly
        else DeltaCertificationBurden.Insufficient
      case StrategicDeltaProjection.PositionLocal(_, focalAnchorCount, _) =>
        if exactBoardSupport && focalAnchorCount > 0 then DeltaCertificationBurden.Primary
        else if delta.changedAnchors.nonEmpty && focalAnchorCount > 0 then DeltaCertificationBurden.SupportOnly
        else DeltaCertificationBurden.Insufficient
      case StrategicDeltaProjection.Comparative(_, _, witness, counterpartObjectIds, profile) =>
        val comparativeSupport =
          exactBoardSupport &&
            witness.isFamilyAware &&
            witness.hasExactCounterpartWitness &&
            counterpartObjectIds.nonEmpty &&
            delta.rivalObjectIds.nonEmpty &&
            profile.metrics.nonEmpty
        if comparativeSupport && profile.metrics.size >= 2 then DeltaCertificationBurden.Primary
        else if comparativeSupport then DeltaCertificationBurden.SupportOnly
        else DeltaCertificationBurden.Insufficient

  private def hasExactBoardSupport(
      delta: StrategicObjectDelta
  ): Boolean =
    delta.changedAnchors.nonEmpty &&
      delta.evidenceRefs.exists(ref =>
        ref.anchorSquares.nonEmpty || ref.contestedSquares.nonEmpty || ref.lane.nonEmpty
      )

  private def certifiedBoundaryWitnesses(
      delta: StrategicObjectDelta
  ): Set[CertifiedBoundaryWitness] =
    delta.positionLocalWitnesses.collect {
      case StrategicPositionLocalWitness.FixedTargetCluster(witness) =>
        CertifiedBoundaryWitness.FixedTargetCluster(witness)
      case StrategicPositionLocalWitness.CoordinationProbe(witness) =>
        CertifiedBoundaryWitness.CoordinationProbe(witness)
    }

  private def enrichPlannerMetadata(
      claims: List[CertifiedClaim]
  ): List[CertifiedClaim] =
    claims.map(claim =>
      claim.copy(
        plannerMetadata =
          CertifiedPlannerMetadata(
            sharedTargetContinuity = SharedTargetContinuityBoundary.hasPacketContinuity(claim),
            currentPositionProbeKind = currentPositionProbeKind(claim),
            tradeInvariantPrimaryClass =
              TradeInvariantPrimaryDescriptor.fromClaim(claim).flatMap(_.primaryReason),
            residualSpecificityClass = residualSpecificityClass(claim),
            counterplayRivalBurden = CounterplayRivalBurdenBoundary.metadata(claim)
          )
      )
    )

  private def currentPositionProbeKind(
      claim: CertifiedClaim
  ): Option[CertifiedCurrentPositionProbeKind] =
    Option
      .when(FixedTargetClusterWitnessBoundary.hasClusterWitness(claim))(
        CertifiedCurrentPositionProbeKind.FixedTarget
      )
      .orElse(CurrentPositionProbeSlice.probeKind(claim))

  private def residualSpecificityClass(
      claim: CertifiedClaim
  ): Option[CertifiedResidualSpecificityClass] =
    claim.delta.flatMap { delta =>
      delta.family match
        case StrategicObjectFamily.TradeInvariant
            if TradeInvariantSimplificationSlice.isPacketOwnedPrimarySimplificationClaim(claim) &&
              isExactTypedResidualClaim(claim) =>
          Some(CertifiedResidualSpecificityClass.TradeInvariantPrimaryExact)
        case StrategicObjectFamily.CounterplayAxis
            if (claim.status == ClaimStatus.Certified || claim.status == ClaimStatus.SupportOnly) &&
              isExactTypedResidualClaim(claim) =>
          Some(CertifiedResidualSpecificityClass.CounterplayExact)
        case StrategicObjectFamily.PlanRace
            if (claim.status == ClaimStatus.Certified || claim.status == ClaimStatus.SupportOnly) &&
              isExactTypedResidualClaim(claim) =>
          Some(CertifiedResidualSpecificityClass.PlanRaceExact)
        case StrategicObjectFamily.ConversionFunnel
            if claim.status == ClaimStatus.Certified &&
              isExactTypedResidualClaim(claim) =>
          Some(CertifiedResidualSpecificityClass.ConversionFunnelExact)
        case _ =>
          None
    }

  private[strategicobject] def isExactTypedResidualClaim(
      claim: CertifiedClaim
  ): Boolean =
    claim.hasTypedDelta &&
      claim.delta.exists(_.projection match
        case StrategicDeltaProjection.MoveLocal(_, transition) =>
          transition.relationWitnesses.nonEmpty &&
            (
              transition.matchedSquares.nonEmpty ||
                transition.matchedFiles.nonEmpty
            )
        case _ =>
          false
      )

trait ClaimCertification:
  def certify(
      contract: DecisiveTruthContract,
      objects: List[StrategicObject],
      deltas: List[StrategicObjectDelta]
  ): List[CertifiedClaim]

object CounterplayRivalBurdenBoundary:

  def metadata(
      claim: CertifiedClaim
  ): Option[CertifiedCounterplayRivalBurden] =
    Option.when(counterplayNarrowSliceClaim(claim)) {
      claim.delta
        .flatMap(_.moveTransition)
        .flatMap(_.counterplayRivalEvidence)
        .flatMap(evidence => certifiedBurden(evidence))
    }.flatten

  def plannerSupportEligible(
      claims: List[CertifiedClaim],
      supportClaim: CertifiedClaim
  ): Boolean =
    supportClaim.plannerMetadata.counterplayRivalBurden.forall { burden =>
      !burden.allMatchedRivalLegsMustCertify ||
      burden.coEqualRivalLegs.forall(leg => matchedRivalLegCertified(claims, leg))
    }

  private def certifiedBurden(
      evidence: StrategicCounterplayRivalEvidence
  ): Option[CertifiedCounterplayRivalBurden] =
    val matchedLegs =
      evidence.matchedRivalLegs.map(toCertifiedRivalLeg)
    val matchedKeys = matchedLegs.map(legKey).toSet
    val admittedButUnmatchedLegs =
      evidence.admittedRivalLegs
        .map(toCertifiedRivalLeg)
        .filterNot(leg => matchedKeys.contains(legKey(leg)))

    Option.when(matchedLegs.nonEmpty) {
      CertifiedCounterplayRivalBurden(
        coEqualRivalLegs = matchedLegs,
        admittedButUnmatchedRivalLegs = admittedButUnmatchedLegs,
        allMatchedRivalLegsMustCertify = true
      ).normalized
    }

  private def matchedRivalLegCertified(
      claims: List[CertifiedClaim],
      leg: CertifiedRivalLeg
  ): Boolean =
    claims.exists(claim =>
      claim.objectId == leg.objectId &&
        claim.hasTypedDelta &&
        (claim.status == ClaimStatus.Certified || claim.status == ClaimStatus.SupportOnly) &&
        claim.delta.exists(_.family == leg.family)
    )

  private def counterplayNarrowSliceClaim(
      claim: CertifiedClaim
  ): Boolean =
    (claim.status == ClaimStatus.Certified || claim.status == ClaimStatus.SupportOnly) &&
      claim.delta.exists(_.family == StrategicObjectFamily.CounterplayAxis) &&
      CanonicalClaimCertification.isExactTypedResidualClaim(claim)

  private def toCertifiedRivalLeg(
      evidence: StrategicRivalLegEvidence
  ): CertifiedRivalLeg =
    CertifiedRivalLeg(
      family = evidence.family,
      objectId = evidence.objectId,
      operator = evidence.operator,
      matchedSquares = evidence.witnessSquares,
      matchedFiles = evidence.witnessFiles
    ).normalized

  private def legKey(
      leg: CertifiedRivalLeg
  ): (String, StrategicObjectFamily, StrategicRelationOperator) =
    (leg.objectId, leg.family, leg.operator)
