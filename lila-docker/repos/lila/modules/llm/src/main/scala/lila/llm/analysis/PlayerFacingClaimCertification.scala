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

  private val ExactOwnerPathFamilies =
    Set(
      "half_open_file_pressure",
      "neutralize_key_break",
      "counterplay_restraint",
      "trade_key_defender",
      ThemeTaxonomy.SubplanId.SimplificationWindow.id,
      ThemeTaxonomy.ThemeL1.WeaknessFixation.id,
      ThemeTaxonomy.SubplanId.StaticWeaknessFixation.id,
      ThemeTaxonomy.SubplanId.BackwardPawnTargeting.id,
      ThemeTaxonomy.SubplanId.MinorityAttackFixation.id,
      ThemeTaxonomy.SubplanId.IQPInducement.id
    )

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
    packet.ownerPathWitness.hasOwnerSeed &&
      (packet.anchorTerms.nonEmpty || packet.ownerPathWitness.hasStructureTransition)

  def hasContinuationWitness(packet: PlayerFacingClaimPacket): Boolean =
    packet.sameBranchState == PlayerFacingSameBranchState.Proven ||
      packet.ownerPathWitness.hasContinuation

  def hasStructureTransitionWitness(packet: PlayerFacingClaimPacket): Boolean =
    packet.ownerPathWitness.hasStructureTransition

  def exactOwnerPathFamily(packet: PlayerFacingClaimPacket): Boolean =
    ExactOwnerPathFamilies.contains(packet.ownerFamily)

  def exactOwnerPathFamily(ownerFamily: String): Boolean =
    ExactOwnerPathFamilies.contains(ownerFamily)

  private def packetWitnessSatisfiesClaim(packet: PlayerFacingClaimPacket): Boolean =
    hasConcreteOwnerSeed(packet) &&
      (
        !exactOwnerPathFamily(packet) ||
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
