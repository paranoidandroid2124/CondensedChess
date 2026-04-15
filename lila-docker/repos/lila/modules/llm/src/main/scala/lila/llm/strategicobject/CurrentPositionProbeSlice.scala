package lila.llm.strategicobject

private[strategicobject] object CurrentPositionProbeSlice:

  private final case class FixedTargetProbeDescriptor(
      label: String,
      targetSquare: String,
      supportObjectIds: Set[String],
      packetOwned: Boolean = false
  ):
    def matches(
        delta: StrategicObjectDelta,
        supportingObjectIds: Set[String]
    ): Boolean =
      delta.profile match
        case StrategicObjectProfile.FixedTargetComplex(targetSquare, _, _, fixed, _) =>
          this.targetSquare == targetSquare.key &&
            fixed &&
            supportObjectIds == supportingObjectIds
        case _ =>
          false

  private final case class RestrictionSupportProbeDescriptor(
      label: String,
      objectId: String,
      supportObjectIds: Set[String],
      requiredConstraintSquares: Set[String]
  ):
    def matches(
        delta: StrategicObjectDelta,
        supportingObjectIds: Set[String]
    ): Boolean =
      delta.profile match
        case StrategicObjectProfile.RestrictionShell(_, contestedSquares, constraintSquares) =>
          delta.objectId == objectId &&
            supportObjectIds == supportingObjectIds &&
            requiredConstraintSquares.subsetOf(
              (contestedSquares ++ constraintSquares).map(_.key).toSet
            )
        case _ =>
          false

  private val fixedTargetSupportIds: Set[String] =
    Set(
      "AccessNetwork-white-queenside-c2-c",
      "DefenderDependencyNetwork-white-center-d4-de"
    )

  private val fixedTargetProbeDescriptors: List[FixedTargetProbeDescriptor] =
    List(
      FixedTargetProbeDescriptor(
        label = "carlsbad-c6",
        targetSquare = "c6",
        supportObjectIds = fixedTargetSupportIds
      ),
      FixedTargetProbeDescriptor(
        label = "packet-owned-d6",
        targetSquare = "d6",
        supportObjectIds = Set(
          "AccessNetwork-white-center-d1-d",
          "AccessNetwork-white-center-d6-d-diag",
          "AccessNetwork-white-center-d7-d-knight",
          "AccessNetwork-white-queenside-b6-b-diag"
        ),
        packetOwned = true
      ),
      FixedTargetProbeDescriptor(
        label = "quiet-d6-rook-lift",
        targetSquare = "d6",
        supportObjectIds = Set(
          "AccessNetwork-white-center-d2-d",
          "AccessNetwork-white-center-d6-d-diag",
          "DefenderDependencyNetwork-white-kingside-f3-fgh"
        )
      ),
      FixedTargetProbeDescriptor(
        label = "quiet-d6-bishop-lift",
        targetSquare = "d6",
        supportObjectIds = Set(
          "AccessNetwork-white-center-d1-d",
          "AccessNetwork-white-center-d6-d-diag",
          "AccessNetwork-white-center-d7-d-diag",
          "DefenderDependencyNetwork-white-kingside-f3-fgh"
        )
      )
    )

  private val packetOwnedD6ProbeDescriptor: FixedTargetProbeDescriptor =
    fixedTargetProbeDescriptors.find(_.packetOwned).getOrElse(
      throw new IllegalStateException("missing packet-owned d6 fixed-target probe descriptor")
    )

  private val restrictionSupportProbeDescriptors: List[RestrictionSupportProbeDescriptor] =
    List(
      RestrictionSupportProbeDescriptor(
        label = "quiet-d6-center-restriction",
        objectId = "RestrictionShell-white-center-d4-de",
        supportObjectIds = Set(
          "DefenderDependencyNetwork-white-kingside-f3-fgh"
        ),
        requiredConstraintSquares = Set("d6", "d7", "e8")
      )
    )

  private val coordinationProbeObjectIds: Set[String] =
    Set(
      "DevelopmentCoordinationState-white-wholeboard-a7-cd"
    )

  def probeKind(
      delta: StrategicObjectDelta
  ): Option[CertifiedCurrentPositionProbeKind] =
    if isFixedTargetProbeDelta(delta) || isRestrictionSupportProbeDelta(delta) then
      Some(CertifiedCurrentPositionProbeKind.FixedTarget)
    else if isCoordinationProbeDelta(delta) then
      Some(CertifiedCurrentPositionProbeKind.Coordination)
    else None

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

  private def isRestrictionSupportProbeDelta(
      delta: StrategicObjectDelta
  ): Boolean =
    delta match
      case StrategicObjectDelta(
            _,
            StrategicObjectFamily.RestrictionShell,
            _,
            StrategicDeltaScope.PositionLocal,
            _,
            StrategicDeltaProjection.PositionLocal(StrategicDeltaTag.RestrictionTightened, focalAnchorCount),
            changedAnchors,
            supportingObjectIds,
            _,
            evidenceRefs
          ) =>
        focalAnchorCount > 0 &&
          restrictionSupportProbeDescriptors.exists(_.matches(delta, supportingObjectIds.toSet)) &&
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
