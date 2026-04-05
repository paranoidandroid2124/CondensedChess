package lila.llm.strategicobject

import lila.llm.analysis.DecisiveTruthContract

enum ClaimStatus:
  case Certified
  case SupportOnly
  case Deferred
  case Rejected

final case class CertifiedClaim(
    id: String,
    objectId: String,
    deltaScope: StrategicDeltaScope,
    status: ClaimStatus,
    deltaObjectId: String,
    readiness: StrategicObjectReadiness,
    supportingObjectIds: List[String] = Nil
)

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
          CertifiedClaim(
            id = claimId(obj.id, delta.scope),
            objectId = obj.id,
            deltaScope = delta.scope,
            status = claimStatus(obj.readiness),
            deltaObjectId = obj.id,
            readiness = obj.readiness,
            supportingObjectIds = obj.relations.map(_.target.objectId).distinct.take(4)
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
            deltaObjectId = obj.id,
            readiness = obj.readiness,
            supportingObjectIds = obj.relations.map(_.target.objectId).distinct.take(4)
          )
        }

    (claimsFromDeltas ++ deferredClaims)
      .groupBy(_.id)
      .values
      .map(_.head)
      .toList
      .sortBy(claim => (claim.objectId, claim.deltaScope.ordinal, claim.status.ordinal))

  private def claimId(
      objectId: String,
      scope: StrategicDeltaScope
  ): String =
    s"$objectId:${scope.toString.toLowerCase}"

  private def claimStatus(
      readiness: StrategicObjectReadiness
  ): ClaimStatus =
    readiness match
      case StrategicObjectReadiness.Stable           => ClaimStatus.Certified
      case StrategicObjectReadiness.Provisional      => ClaimStatus.SupportOnly
      case StrategicObjectReadiness.DeferredForDelta => ClaimStatus.Deferred

trait ClaimCertification:
  def certify(
      contract: DecisiveTruthContract,
      objects: List[StrategicObject],
      deltas: List[StrategicObjectDelta]
  ): List[CertifiedClaim]
