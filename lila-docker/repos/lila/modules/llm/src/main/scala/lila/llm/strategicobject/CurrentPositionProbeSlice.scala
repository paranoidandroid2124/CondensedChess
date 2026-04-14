package lila.llm.strategicobject

private[strategicobject] object CurrentPositionProbeSlice:

  private val fixedTargetSupportIds: Set[String] =
    Set(
      "AccessNetwork-white-queenside-c2-c",
      "DefenderDependencyNetwork-white-center-d4-de"
    )

  private final case class FixedTargetProbeDescriptor(
      targetSquare: String,
      supportObjectIds: Set[String]
  ):
    def matches(
        delta: StrategicObjectDelta,
        supportingObjectIds: Set[String]
    ): Boolean =
      delta.profile match
        case StrategicObjectProfile.FixedTargetComplex(targetSquare, _, _, _, _) =>
          this.targetSquare == targetSquare.key &&
            supportObjectIds == supportingObjectIds
        case _ =>
          false

  private val fixedTargetProbeDescriptors: List[FixedTargetProbeDescriptor] =
    List(
      FixedTargetProbeDescriptor("c6", fixedTargetSupportIds),
      FixedTargetProbeDescriptor(
        "d6",
        Set(
          "AccessNetwork-white-center-d1-d",
          "AccessNetwork-white-center-d6-d-diag",
          "AccessNetwork-white-center-d7-d-knight",
          "AccessNetwork-white-queenside-b6-b-diag"
        )
      )
    )

  private val packetOwnedD6ProbeDescriptor: FixedTargetProbeDescriptor =
    fixedTargetProbeDescriptors.find(_.targetSquare == "d6").getOrElse(
      throw new IllegalStateException("missing packet-owned d6 fixed-target probe descriptor")
    )

  private val coordinationProbeObjectIds: Set[String] =
    Set(
      "DevelopmentCoordinationState-white-wholeboard-a7-cd"
    )

  def isFixedTargetProbeDelta(
      delta: StrategicObjectDelta
  ): Boolean =
    matchesFixedTargetProbeDelta(delta, fixedTargetProbeDescriptors)

  def isPacketOwnedD6FixedTargetProbeDelta(
      delta: StrategicObjectDelta
  ): Boolean =
    matchesFixedTargetProbeDelta(delta, List(packetOwnedD6ProbeDescriptor))

  private def matchesFixedTargetProbeDelta(
      delta: StrategicObjectDelta,
      descriptors: List[FixedTargetProbeDescriptor]
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
          descriptors.exists(_.matches(delta, supportingObjectIds.toSet)) &&
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
