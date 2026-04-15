package lila.llm.strategicobject

private[strategicobject] object CurrentPositionProbeSlice:

  private val coordinationProbeObjectIds: Set[String] =
    Set(
      "DevelopmentCoordinationState-white-wholeboard-a7-cd"
    )

  def probeKind(
      delta: StrategicObjectDelta
  ): Option[CertifiedCurrentPositionProbeKind] =
    if isCoordinationProbeDelta(delta) then
      Some(CertifiedCurrentPositionProbeKind.Coordination)
    else None

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
            StrategicDeltaProjection.PositionLocal(StrategicDeltaTag.CoordinationImproved, focalAnchorCount, _),
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
