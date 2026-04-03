package lila.llm.analysis

private[llm] enum PlayerFacingCertificateStatus:
  case Valid
  case WeaklyValid
  case Invalid
  case StaleOrMismatched

private[llm] enum PlayerFacingClaimQuantifier:
  case Universal
  case BestResponse
  case Existential
  case LineConditioned

private[llm] enum PlayerFacingClaimModalityTier:
  case Available
  case Supports
  case Advances
  case Forces
  case Removes

private[llm] enum PlayerFacingClaimAttributionGrade:
  case Distinctive
  case AnchoredButShared
  case StateOnly

private[llm] enum PlayerFacingClaimStabilityGrade:
  case Stable
  case Unstable
  case Unknown

private[llm] enum PlayerFacingClaimProvenanceClass:
  case ProbeBacked
  case StructuralOnly
  case PvCoupled
  case Deferred

private[llm] enum PlayerFacingClaimOntologyFamily:
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

private[llm] enum PlayerFacingClaimTaintFlag:
  case Latent
  case PvCoupled
  case Deferred
  case StructuralOnly
  case BranchConditioned

private[llm] object PlayerFacingClaimCertification:

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

  def allowsStrongMainClaim(
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

  def allowsStrongMainClaim(packet: PlayerFacingClaimPacket): Boolean =
    packet.scope == PlayerFacingPacketScope.MoveLocal &&
      packet.fallbackMode == PlayerFacingClaimFallbackMode.WeakMain &&
      packet.suppressionReasons.isEmpty &&
      packet.releaseRisks.isEmpty &&
      allowsStrongMainClaim(
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
    (certificateStatus == PlayerFacingCertificateStatus.Valid ||
      certificateStatus == PlayerFacingCertificateStatus.WeaklyValid) &&
      provenance == PlayerFacingClaimProvenanceClass.ProbeBacked &&
      !blocksMainClaim(taintFlags) &&
      quantifier != PlayerFacingClaimQuantifier.Existential &&
      quantifier != PlayerFacingClaimQuantifier.LineConditioned &&
      attribution != PlayerFacingClaimAttributionGrade.StateOnly &&
      stability != PlayerFacingClaimStabilityGrade.Unstable

  def allowsWeakMainClaim(packet: PlayerFacingClaimPacket): Boolean =
    packet.scope == PlayerFacingPacketScope.MoveLocal &&
      packet.fallbackMode == PlayerFacingClaimFallbackMode.WeakMain &&
      packet.suppressionReasons.isEmpty &&
      !packet.claimGate.alternativeDominance &&
      allowsWeakMainClaim(
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
      allowsLineEvidenceHook(
        certificateStatus = packet.claimGate.certificateStatus,
        provenance = packet.claimGate.provenanceClass,
        taintFlags = packet.claimGate.taintFlags.toSet
      )
