package lila.commentary.analysis


import lila.commentary.analysis.claim.*
private[commentary] enum PlayerFacingCertificateStatus:
  case Valid
  case WeaklyValid
  case Invalid
  case StaleOrMismatched

private[commentary] enum PlayerFacingClaimQuantifier:
  case Universal
  case BestResponse
  case Existential
  case LineConditioned

private[commentary] enum PlayerFacingClaimModalityTier:
  case Available
  case Supports
  case Advances
  case Forces
  case Removes

private[commentary] enum PlayerFacingClaimAttributionGrade:
  case Distinctive
  case AnchoredButShared
  case StateOnly

private[commentary] enum PlayerFacingClaimStabilityGrade:
  case Stable
  case Unstable
  case Unknown

private[commentary] enum PlayerFacingClaimProvenanceClass:
  case ProbeBacked
  case StructuralOnly
  case PvCoupled
  case Deferred

private[commentary] enum PlayerFacingClaimOntologyKind:
  case Access
  case Pressure
  case Exchange
  case CounterplayRestraint
  case ResourceRemoval
  case PlanAdvance
  case RouteDenial
  case ColorComplexSqueeze
  case LongTermRestraint
  case PieceImprovement
  case KingSafety
  case TechnicalConversion
  case Unknown

private[commentary] enum PlayerFacingClaimTaintFlag:
  case Latent
  case PvCoupled
  case Deferred
  case StructuralOnly
  case BranchConditioned

private[commentary] object PlayerFacingClaimProof:

  def blocksMainClaim(taintFlags: Set[PlayerFacingClaimTaintFlag]): Boolean =
    taintFlags.exists {
      case PlayerFacingClaimTaintFlag.Latent           => true
      case PlayerFacingClaimTaintFlag.PvCoupled        => true
      case PlayerFacingClaimTaintFlag.Deferred         => true
      case PlayerFacingClaimTaintFlag.StructuralOnly   => true
      case PlayerFacingClaimTaintFlag.BranchConditioned => true
    }

  def blocksMainClaim(packet: PlayerFacingClaimPacket): Boolean =
    blocksMainClaim(packet.claimGate.taintFlags.toSet) ||
      packet.claimGate.alternativeDominance ||
      packet.scope == PlayerFacingPacketScope.BackendOnly ||
      packet.fallbackMode == PlayerFacingClaimFallbackMode.Suppress

  def hasConcreteOwnerSeed(packet: PlayerFacingClaimPacket): Boolean =
    packet.proofPathWitness.hasOwnerSeed &&
      (packet.anchorTerms.nonEmpty || packet.proofPathWitness.hasStructureTransition)

  def hasContinuationWitness(packet: PlayerFacingClaimPacket): Boolean =
    packet.sameBranchState == PlayerFacingSameBranchState.Proven ||
      packet.proofPathWitness.hasContinuation

  def hasStructureTransitionWitness(packet: PlayerFacingClaimPacket): Boolean =
    packet.proofPathWitness.hasStructureTransition

  def exactProofFamily(packet: PlayerFacingClaimPacket): Boolean =
    ProofContractRules.exactProofFamily(packet.proofFamily)

  def exactProofFamily(proofFamily: String): Boolean =
    ProofContractRules.exactProofFamily(proofFamily)

  private def packetWitnessSatisfiesClaim(packet: PlayerFacingClaimPacket): Boolean =
    hasConcreteOwnerSeed(packet) &&
      (
        !exactProofFamily(packet) ||
          hasContinuationWitness(packet)
      )

  private def packetWitnessSatisfiesLineHook(packet: PlayerFacingClaimPacket): Boolean =
    hasConcreteOwnerSeed(packet) ||
      hasStructureTransitionWitness(packet)

  private def primaryClaimScopeAllowed(packet: PlayerFacingClaimPacket): Boolean =
    packet.scope == PlayerFacingPacketScope.MoveLocal ||
      packet.scope == PlayerFacingPacketScope.PositionLocal

  private def allowsStrongByGate(
      certificateStatus: PlayerFacingCertificateStatus,
      quantifier: PlayerFacingClaimQuantifier,
      attribution: PlayerFacingClaimAttributionGrade,
      stability: PlayerFacingClaimStabilityGrade,
      provenance: PlayerFacingClaimProvenanceClass,
      taintFlags: Set[PlayerFacingClaimTaintFlag]
  ): Boolean =
    certificateStatus == PlayerFacingCertificateStatus.Valid &&
      provenance == PlayerFacingClaimProvenanceClass.ProbeBacked &&
      !blocksMainClaim(taintFlags) &&
      quantifier != PlayerFacingClaimQuantifier.Existential &&
      quantifier != PlayerFacingClaimQuantifier.LineConditioned &&
      attribution == PlayerFacingClaimAttributionGrade.Distinctive &&
      stability != PlayerFacingClaimStabilityGrade.Unstable

  private def allowsWeakByGate(
      certificateStatus: PlayerFacingCertificateStatus,
      quantifier: PlayerFacingClaimQuantifier,
      attribution: PlayerFacingClaimAttributionGrade,
      stability: PlayerFacingClaimStabilityGrade,
      provenance: PlayerFacingClaimProvenanceClass,
      taintFlags: Set[PlayerFacingClaimTaintFlag]
  ): Boolean =
    (certificateStatus == PlayerFacingCertificateStatus.Valid ||
      certificateStatus == PlayerFacingCertificateStatus.WeaklyValid) &&
      provenance == PlayerFacingClaimProvenanceClass.ProbeBacked &&
      !blocksMainClaim(taintFlags) &&
      quantifier != PlayerFacingClaimQuantifier.Existential &&
      quantifier != PlayerFacingClaimQuantifier.LineConditioned &&
      attribution != PlayerFacingClaimAttributionGrade.StateOnly &&
      stability != PlayerFacingClaimStabilityGrade.Unstable

  def allowsStrongMainClaim(
      certificateStatus: PlayerFacingCertificateStatus,
      quantifier: PlayerFacingClaimQuantifier,
      attribution: PlayerFacingClaimAttributionGrade,
      stability: PlayerFacingClaimStabilityGrade,
      provenance: PlayerFacingClaimProvenanceClass,
      taintFlags: Set[PlayerFacingClaimTaintFlag]
  ): Boolean =
    allowsStrongByGate(
      certificateStatus = certificateStatus,
      quantifier = quantifier,
      attribution = attribution,
      stability = stability,
      provenance = provenance,
      taintFlags = taintFlags
    )

  def allowsStrongMainClaim(packet: PlayerFacingClaimPacket): Boolean =
    primaryClaimScopeAllowed(packet) &&
      packet.fallbackMode == PlayerFacingClaimFallbackMode.WeakMain &&
      packet.suppressionReasons.isEmpty &&
      packet.releaseRisks.isEmpty &&
      packetWitnessSatisfiesClaim(packet) &&
      allowsStrongByGate(
        certificateStatus = packet.claimGate.certificateStatus,
        quantifier = packet.claimGate.quantifier,
        attribution = packet.claimGate.attributionGrade,
        stability = packet.claimGate.stabilityGrade,
        provenance = packet.claimGate.provenanceClass,
        taintFlags = packet.claimGate.taintFlags.toSet
      )

  def allowsWeakMainClaim(
      certificateStatus: PlayerFacingCertificateStatus,
      quantifier: PlayerFacingClaimQuantifier,
      attribution: PlayerFacingClaimAttributionGrade,
      stability: PlayerFacingClaimStabilityGrade,
      provenance: PlayerFacingClaimProvenanceClass,
      taintFlags: Set[PlayerFacingClaimTaintFlag]
  ): Boolean =
    allowsWeakByGate(
      certificateStatus = certificateStatus,
      quantifier = quantifier,
      attribution = attribution,
      stability = stability,
      provenance = provenance,
      taintFlags = taintFlags
    )

  def allowsWeakMainClaim(packet: PlayerFacingClaimPacket): Boolean =
    primaryClaimScopeAllowed(packet) &&
      packet.fallbackMode == PlayerFacingClaimFallbackMode.WeakMain &&
      packet.suppressionReasons.isEmpty &&
      !packet.claimGate.alternativeDominance &&
      packetWitnessSatisfiesClaim(packet) &&
      allowsWeakByGate(
        certificateStatus = packet.claimGate.certificateStatus,
        quantifier = packet.claimGate.quantifier,
        attribution = packet.claimGate.attributionGrade,
        stability = packet.claimGate.stabilityGrade,
        provenance = packet.claimGate.provenanceClass,
        taintFlags = packet.claimGate.taintFlags.toSet
      )

  def allowsLineEvidenceHook(
      certificateStatus: PlayerFacingCertificateStatus,
      provenance: PlayerFacingClaimProvenanceClass,
      taintFlags: Set[PlayerFacingClaimTaintFlag]
  ): Boolean =
    (certificateStatus == PlayerFacingCertificateStatus.Valid ||
      certificateStatus == PlayerFacingCertificateStatus.WeaklyValid) &&
      provenance == PlayerFacingClaimProvenanceClass.ProbeBacked &&
      !blocksMainClaim(taintFlags)

  def allowsLineEvidenceHook(packet: PlayerFacingClaimPacket): Boolean =
    packet.scope != PlayerFacingPacketScope.BackendOnly &&
      packet.allowsLineEvidence &&
      !packet.claimGate.alternativeDominance &&
      packetWitnessSatisfiesLineHook(packet) &&
      allowsLineEvidenceHook(
        certificateStatus = packet.claimGate.certificateStatus,
        provenance = packet.claimGate.provenanceClass,
        taintFlags = packet.claimGate.taintFlags.toSet
      )
