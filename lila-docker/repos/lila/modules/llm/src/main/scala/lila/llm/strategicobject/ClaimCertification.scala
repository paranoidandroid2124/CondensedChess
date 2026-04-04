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
    summary: String,
    support: List[String] = Nil
)

trait ClaimCertification:
  def certify(
      contract: DecisiveTruthContract,
      objects: List[StrategicObject],
      deltas: List[StrategicObjectDelta]
  ): List[CertifiedClaim]
