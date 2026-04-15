package lila.llm.strategicobject

private[strategicobject] object CurrentPositionProbeSlice:

  def probeKind(
      claim: CertifiedClaim
  ): Option[CertifiedCurrentPositionProbeKind] =
    Option.when(coordinationProbeWitnesses(claim).nonEmpty)(
      CertifiedCurrentPositionProbeKind.Coordination
    )

  def probeKind(
      delta: StrategicObjectDelta
  ): Option[CertifiedCurrentPositionProbeKind] =
    Option.when(coordinationProbeWitnesses(delta).nonEmpty)(
      CertifiedCurrentPositionProbeKind.Coordination
    )

  def hasCoordinationProbeWitness(
      delta: StrategicObjectDelta
  ): Boolean =
    coordinationProbeWitnesses(delta).nonEmpty

  def sharesCoordinationProbeWitness(
      left: CertifiedClaim,
      right: CertifiedClaim
  ): Boolean =
    coordinationProbeWitnesses(left).intersect(coordinationProbeWitnesses(right)).nonEmpty

  private def coordinationProbeWitnesses(
      delta: StrategicObjectDelta
  ): Set[CoordinationProbeWitness] =
    delta.positionLocalWitnesses.collect {
      case StrategicPositionLocalWitness.CoordinationProbe(witness) => witness
    }

  private def coordinationProbeWitnesses(
      claim: CertifiedClaim
  ): Set[CoordinationProbeWitness] =
    claim.boundaryWitnesses.collect {
      case CertifiedBoundaryWitness.CoordinationProbe(witness) => witness
    }
