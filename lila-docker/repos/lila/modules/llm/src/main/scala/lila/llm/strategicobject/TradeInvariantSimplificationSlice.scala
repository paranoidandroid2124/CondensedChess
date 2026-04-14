package lila.llm.strategicobject

import chess.{ Color, Square }

private[strategicobject] object TradeInvariantSimplificationSlice:

  def allowsPacketOwnedPrimarySimplification(
      owner: Color,
      exchangeSquares: List[Square],
      invariantSquares: List[Square],
      preservedFamilies: Set[StrategicObjectFamily],
      features: Set[TradeInvariantFeature]
  ): Boolean =
    TradeInvariantPrimaryDescriptor
      .describe(
        owner = owner,
        exchangeSquares = exchangeSquares,
        invariantSquares = invariantSquares,
        preservedFamilies = preservedFamilies,
        features = features
      )
      .packetPrimaryEligible

  def isPacketOwnedPrimarySimplificationObject(
      obj: StrategicObject
  ): Boolean =
    obj.family == StrategicObjectFamily.TradeInvariant &&
      TradeInvariantPrimaryDescriptor.fromObject(obj).exists(_.packetPrimaryEligible)

  def isPacketOwnedPrimarySimplificationDelta(
      delta: StrategicObjectDelta
  ): Boolean =
    delta match
      case StrategicObjectDelta(
            _,
            StrategicObjectFamily.TradeInvariant,
            owner,
            StrategicDeltaScope.MoveLocal,
            _,
            StrategicDeltaProjection.MoveLocal(StrategicDeltaTag.TradePreserved, witness),
            changedAnchors,
            supportingObjectIds,
            _,
            evidenceRefs
          ) =>
        owner == TradeInvariantPrimaryDescriptor.packetOwner &&
          TradeInvariantPrimaryDescriptor.fromDelta(delta).exists(_.packetPrimaryEligible) &&
          changedAnchors.exists(_.squares.contains(TradeInvariantPrimaryDescriptor.packetAnchor)) &&
          supportingObjectIds.nonEmpty &&
          witness.isTransitionAware &&
          witness.hasAnchoredEvidence &&
          evidenceRefs.nonEmpty
      case _ =>
        false

  def isPacketOwnedPrimarySimplificationClaim(
      claim: CertifiedClaim
  ): Boolean =
    claim.status == ClaimStatus.Certified &&
      claim.hasTypedDelta &&
      claim.delta.exists(isPacketOwnedPrimarySimplificationDelta)
