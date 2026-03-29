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

  def allowsLineEvidenceHook(
      certificateStatus: PlayerFacingCertificateStatus,
      provenance: PlayerFacingClaimProvenanceClass,
      taintFlags: Set[PlayerFacingClaimTaintFlag]
  ): Boolean =
    (certificateStatus == PlayerFacingCertificateStatus.Valid ||
      certificateStatus == PlayerFacingCertificateStatus.WeaklyValid) &&
      provenance == PlayerFacingClaimProvenanceClass.ProbeBacked &&
      !blocksMainClaim(taintFlags)
