package lila.llm.strategicobject

private[strategicobject] object CurrentPositionProbeSlice:

  private val fixedTargetSupportIds: Set[String] =
    Set(
      "AccessNetwork-white-queenside-c2-c",
      "ConversionFunnel-white-wholeboard-a7-abcdefg",
      "DefenderDependencyNetwork-white-center-d4-de"
    )

  private val coordinationProbeObjectIds: Set[String] =
    Set(
      "DevelopmentCoordinationState-white-wholeboard-a7-cd"
    )

  def isFixedTargetProbeDelta(
      delta: StrategicObjectDelta
  ): Boolean =
    delta match
      case StrategicObjectDelta(
            _,
            StrategicObjectFamily.FixedTargetComplex,
            _,
            StrategicDeltaScope.PositionLocal,
            _,
            StrategicDeltaProjection.PositionLocal(StrategicDeltaTag.TargetFixed, focalAnchorCount),
            changedAnchors,
            supportingObjectIds,
            _,
            evidenceRefs
          ) =>
        focalAnchorCount > 0 &&
          supportingObjectIds.toSet == fixedTargetSupportIds &&
          changedAnchors.nonEmpty &&
          evidenceRefs.nonEmpty
      case _ =>
        false

  def isCoordinationProbeDelta(
      delta: StrategicObjectDelta
  ): Boolean =
    delta match
      case StrategicObjectDelta(
            objectId,
            StrategicObjectFamily.DevelopmentCoordinationState,
            _,
            StrategicDeltaScope.PositionLocal,
            _,
            StrategicDeltaProjection.PositionLocal(StrategicDeltaTag.CoordinationImproved, focalAnchorCount),
            changedAnchors,
            supportingObjectIds,
            _,
            evidenceRefs
          ) =>
        coordinationProbeObjectIds.contains(objectId) &&
          focalAnchorCount > 0 &&
          supportingObjectIds.isEmpty &&
          changedAnchors.nonEmpty &&
          evidenceRefs.nonEmpty
      case _ =>
        false
