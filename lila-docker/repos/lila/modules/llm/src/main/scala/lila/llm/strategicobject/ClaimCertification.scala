package lila.llm.strategicobject

import chess.Square

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

final case class CertifiedClaim(
    id: String,
    objectId: String,
    deltaScope: StrategicDeltaScope,
    status: ClaimStatus,
    readiness: StrategicObjectReadiness,
    delta: Option[StrategicObjectDelta] = None,
    supportingObjectIds: List[String] = Nil,
    boundaryWitnesses: Set[CertifiedBoundaryWitness] = Set.empty
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
            supportingObjectIds = delta.supportingObjectIds
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

    SharedTargetContinuityBoundary.certify(
      (claimsFromDeltas ++ deferredClaims)
        .groupBy(_.id)
        .values
        .map(_.head)
        .toList
        .sortBy(claim => (claim.objectId, claim.deltaScope.ordinal, claim.status.ordinal))
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
          case DeltaCertificationBurden.Primary if CurrentPositionProbeSlice.isCoordinationProbeDelta(delta) =>
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
      case StrategicDeltaProjection.PositionLocal(_, focalAnchorCount) =>
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

trait ClaimCertification:
  def certify(
      contract: DecisiveTruthContract,
      objects: List[StrategicObject],
      deltas: List[StrategicObjectDelta]
  ): List[CertifiedClaim]
