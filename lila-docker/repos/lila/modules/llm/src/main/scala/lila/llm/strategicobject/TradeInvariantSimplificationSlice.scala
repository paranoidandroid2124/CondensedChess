package lila.llm.strategicobject

import chess.{ Color, Square }

private[strategicobject] object TradeInvariantSimplificationSlice:

  private val packetOwner: Color =
    Color.White

  private val packetAnchor: Square =
    Square.fromKey("e6").getOrElse(
      throw new IllegalStateException("missing packet anchor e6")
    )

  def isPacketOwnedPrimarySimplificationObject(
      obj: StrategicObject
  ): Boolean =
    obj.family == StrategicObjectFamily.TradeInvariant &&
      obj.owner == packetOwner &&
      obj.sector == ObjectSector.Center &&
      obj.anchors.exists(anchor => anchor.squares.contains(packetAnchor)) &&
      (
        obj.profile match
          case StrategicObjectProfile.TradeInvariant(exchangeSquares, invariantSquares, _, preservedFamilies, features) =>
            exchangeSquares.nonEmpty &&
              invariantSquares.nonEmpty &&
              preservedFamilies.nonEmpty &&
              !preservedFamilies.contains(StrategicObjectFamily.PasserComplex) &&
              !features.contains(TradeInvariantFeature.PasserAnchor) &&
              features.intersect(
                Set(
                  TradeInvariantFeature.FixedTargetAnchor,
                  TradeInvariantFeature.BreakAnchor,
                  TradeInvariantFeature.AccessAnchor
                )
              ).nonEmpty
          case _ =>
            false
      )

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
        owner == packetOwner &&
          changedAnchors.exists(_.squares.contains(packetAnchor)) &&
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
