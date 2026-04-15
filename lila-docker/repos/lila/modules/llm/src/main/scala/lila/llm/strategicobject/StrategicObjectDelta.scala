package lila.llm.strategicobject

import chess.{ Color, File, Role, Square }

enum StrategicDeltaScope:
  case MoveLocal
  case PositionLocal
  case Comparative

enum ComparativeStanding:
  case Ahead
  case Behind
  case Balanced
  case Contested

enum StrategicDeltaTag:
  case RegimeFixed
  case RegimeReleased
  case ChainIdentityShift
  case BreakDrivenShift
  case ShellOpened
  case ShellClosed
  case ShellIntensified
  case ShellRelieved
  case CoordinationImproved
  case CoordinationWorsened
  case LagReduced
  case LagIncreased
  case RoleRepaired
  case RoleDegraded
  case SpaceExpanded
  case SpaceContracted
  case CriticalCreated
  case CriticalReinforced
  case CriticalLost
  case TargetFixed
  case TargetPressureIntensified
  case TargetRelieved
  case BreakOpened
  case BreakClosed
  case BreakAccelerated
  case BreakDelayed
  case AccessOpened
  case AccessDenied
  case AccessImproved
  case AccessWorsened
  case CounterplayLive
  case CounterplayReduced
  case CounterplayOpened
  case CounterplayDenied
  case RestrictionTightened
  case RestrictionLoosened
  case CageCreated
  case CageTightened
  case CageBroken
  case RouteOpened
  case RouteBlocked
  case RouteShortened
  case PasserCreated
  case PasserAccelerated
  case PasserBlocked
  case PasserSupported
  case TradePreserved
  case ComparativeEdge
  case ComparativeBalance

final case class StrategicPlayedMoveTrace(
    from: Square,
    to: Square,
    promotion: Option[Role] = None
):
  def touchedSquares: List[Square] =
    List(from, to).distinct.sortBy(_.key)

  def touchedFiles: List[File] =
    touchedSquares.map(_.file).distinct.sortBy(_.char.toString)

enum StrategicMoveTransitionAxis:
  case PawnBreakContact
  case PawnFixation
  case PawnPasserPush
  case KingEntryPressure
  case DevelopmentCoordinationShift
  case RoleRepairOrTrap
  case SpaceClampAdvance
  case CriticalSquareOccupation
  case FixedTargetFixation
  case BreakActivation
  case AccessRouteActivation
  case CounterplayResourceShift
  case RestrictionImposition
  case MobilityRestriction
  case RedeploymentActivation
  case PasserAdvance
  case TradeSimplification

final case class StrategicRivalLegEvidence(
    objectId: String,
    family: StrategicObjectFamily,
    operator: StrategicRelationOperator,
    witnessSquares: List[Square] = Nil,
    witnessFiles: List[File] = Nil
):
  def normalized: StrategicRivalLegEvidence =
    copy(
      witnessSquares = witnessSquares.distinct.sortBy(_.key),
      witnessFiles = witnessFiles.distinct.sortBy(_.char.toString)
    )

final case class StrategicCounterplayRivalEvidence(
    matchedRivalLegs: List[StrategicRivalLegEvidence] = Nil,
    admittedRivalLegs: List[StrategicRivalLegEvidence] = Nil
):
  def normalized: StrategicCounterplayRivalEvidence =
    copy(
      matchedRivalLegs =
        matchedRivalLegs
          .map(_.normalized)
          .distinct
          .sortBy(leg =>
            s"${leg.objectId}:${leg.family}:${leg.operator}:${leg.witnessSquares.map(_.key).mkString(",")}:${leg.witnessFiles.map(_.char).mkString}"
          ),
      admittedRivalLegs =
        admittedRivalLegs
          .map(_.normalized)
          .distinct
          .sortBy(leg =>
            s"${leg.objectId}:${leg.family}:${leg.operator}:${leg.witnessSquares.map(_.key).mkString(",")}:${leg.witnessFiles.map(_.char).mkString}"
          )
    )

final case class StrategicMoveTransitionWitness(
    move: StrategicPlayedMoveTrace,
    axis: StrategicMoveTransitionAxis,
    matchedSquares: List[Square] = Nil,
    matchedFiles: List[File] = Nil,
    relationWitnesses: Set[StrategicRelationOperator] = Set.empty,
    primitiveKinds: Set[PrimitiveKind] = Set.empty,
    counterplayRivalEvidence: Option[StrategicCounterplayRivalEvidence] = None
):
  def normalized: StrategicMoveTransitionWitness =
    copy(
      matchedSquares = matchedSquares.distinct.sortBy(_.key),
      matchedFiles = matchedFiles.distinct.sortBy(_.char.toString),
      counterplayRivalEvidence = counterplayRivalEvidence.map(_.normalized)
    )

  def hasAnchoredEvidence: Boolean =
    matchedSquares.nonEmpty && primitiveKinds.nonEmpty

  def isTransitionAware: Boolean =
    relationWitnesses.nonEmpty || hasAnchoredEvidence

enum StrategicComparativeAxis:
  case PawnBreakPressureContrast
  case PawnFixationContrast
  case PawnPasserPotentialContrast
  case KingShellIntegrityContrast
  case KingEntryPressureContrast
  case DevelopmentLeadContrast
  case PieceRoleLiabilityContrast
  case SpaceClampCoverageContrast
  case CriticalSquareControlContrast
  case FixedTargetPressureContrast
  case FixedTargetDefenseContrast
  case BreakAvailabilityContrast
  case BreakSupportRace
  case AccessRouteContestContrast
  case AccessEntryUsabilityContrast
  case CounterplayBreakPressureContrast
  case CounterplayReliefContrast
  case RestrictionContainmentContrast
  case MobilityRestrictionContrast
  case RedeploymentTempoContrast
  case RedeploymentRouteClarityContrast
  case PasserPromotionRouteContrast
  case PasserEscortContrast

enum StrategicCounterpartWitnessKind:
  case SharedSquare
  case SharedFile
  case SharedRoute
  case SharedTarget
  case SharedPiece
  case DirectRivalReference

final case class StrategicComparativeWitness(
    axis: StrategicComparativeAxis,
    counterpartFamily: StrategicObjectFamily,
    matchedSquares: List[Square] = Nil,
    matchedFiles: List[File] = Nil,
    relationWitnesses: Set[StrategicRelationOperator] = Set.empty,
    rivalPrimitiveKinds: Set[PrimitiveKind] = Set.empty,
    counterpartWitnessKinds: Set[StrategicCounterpartWitnessKind] = Set.empty
):
  def normalized: StrategicComparativeWitness =
    copy(
      matchedSquares = matchedSquares.distinct.sortBy(_.key),
      matchedFiles = matchedFiles.distinct.sortBy(_.char.toString)
    )

  def hasExactCounterpartWitness: Boolean =
    matchedSquares.nonEmpty || matchedFiles.nonEmpty || counterpartWitnessKinds.nonEmpty

  def isFamilyAware: Boolean =
    hasExactCounterpartWitness || relationWitnesses.nonEmpty

final case class StrategicComparativeBalance(
    ownerPressure: Int,
    rivalPressure: Int,
    ownerSupport: Int,
    rivalSupport: Int,
    standing: ComparativeStanding
)

enum StrategicComparativeMetric:
  case BreakPressure(owner: Int, rival: Int)
  case FixationCount(owner: Int, rival: Int)
  case PasserPotential(owner: Int, rival: Int)
  case ShellStress(owner: Int, rival: Int)
  case EntryAccess(owner: Int, rival: Int)
  case ShellIntegrity(owner: Int, rival: Int)
  case StressDistribution(owner: Int, rival: Int)
  case LaggingPieceCount(owner: Int, rival: Int)
  case ActiveFileCount(owner: Int, rival: Int)
  case CoordinationCoherence(owner: Int, rival: Int)
  case LiabilitySeverity(owner: Int, rival: Int)
  case RepairSquareCount(owner: Int, rival: Int)
  case EscapeAvailability(owner: Int, rival: Int)
  case ClampSquareCount(owner: Int, rival: Int)
  case PressureFileCount(owner: Int, rival: Int)
  case UsableCounterspace(owner: Int, rival: Int)
  case CriticalControl(owner: Int, rival: Int)
  case CriticalPressure(owner: Int, rival: Int)
  case TargetPressure(owner: Int, rival: Int)
  case TargetDefense(owner: Int, rival: Int)
  case BreakAvailability(owner: Int, rival: Int)
  case BreakSupport(owner: Int, rival: Int)
  case EntryUsability(owner: Int, rival: Int)
  case RouteContest(owner: Int, rival: Int)
  case ReliefResource(owner: Int, rival: Int)
  case EntryViability(owner: Int, rival: Int)
  case RestrictionCoverage(owner: Int, rival: Int)
  case ConstraintCount(owner: Int, rival: Int)
  case ReopenedSquareCount(owner: Int, rival: Int)
  case DeniedSquareCount(owner: Int, rival: Int)
  case RepairCount(owner: Int, rival: Int)
  case EffectiveMobilityLoss(owner: Int, rival: Int)
  case RouteClarity(owner: Int, rival: Int)
  case MobilityGain(owner: Int, rival: Int)
  case DestinationQuality(owner: Int, rival: Int)
  case PromotionRouteCleanliness(owner: Int, rival: Int)
  case EscortCoverage(owner: Int, rival: Int)

final case class StrategicComparativeProfile(
    axis: StrategicComparativeAxis,
    counterpartFamily: StrategicObjectFamily,
    metrics: List[StrategicComparativeMetric]
):
  def normalized: StrategicComparativeProfile =
    copy(metrics = metrics.distinct)

final case class FixedTargetClusterWitness(
    focalTargetSquare: Square,
    clusterSquares: Set[Square],
    matchingAccessRoutes: Set[String],
    matchingRestrictionShells: Set[String],
    matchingDefenderDependencies: Set[String],
    disambiguation: String
)

final case class CoordinationProbeWitness(
    focalCoordinationSquare: Square,
    coordinationSquares: Set[Square],
    activeFiles: Set[File],
    disambiguation: String
)

enum StrategicPositionLocalWitness:
  case FixedTargetCluster(witness: FixedTargetClusterWitness)
  case CoordinationProbe(witness: CoordinationProbeWitness)

enum StrategicDeltaProjection:
  case MoveLocal(
      change: StrategicDeltaTag,
      witness: StrategicMoveTransitionWitness
  )
  case PositionLocal(
      state: StrategicDeltaTag,
      focalAnchorCount: Int,
      witnesses: Set[StrategicPositionLocalWitness] = Set.empty
  )
  case Comparative(
      contrast: StrategicDeltaTag,
      balance: StrategicComparativeBalance,
      witness: StrategicComparativeWitness,
      counterpartObjectIds: List[String],
      profile: StrategicComparativeProfile
  )

  def primaryTag: StrategicDeltaTag =
    this match
      case StrategicDeltaProjection.MoveLocal(change, _)         => change
      case StrategicDeltaProjection.PositionLocal(state, _, _)   => state
      case StrategicDeltaProjection.Comparative(contrast, _, _, _, _) => contrast

  def moveWitness: Option[StrategicMoveTransitionWitness] =
    this match
      case StrategicDeltaProjection.MoveLocal(_, witness) => Some(witness)
      case _                                              => None

  def comparativeWitness: Option[StrategicComparativeWitness] =
    this match
      case StrategicDeltaProjection.Comparative(_, _, witness, _, _) => Some(witness)
      case _                                                      => None

  def comparativeProfile: Option[StrategicComparativeProfile] =
    this match
      case StrategicDeltaProjection.Comparative(_, _, _, _, profile) => Some(profile)
      case _                                                         => None

  def positionLocalWitnesses: Set[StrategicPositionLocalWitness] =
    this match
      case StrategicDeltaProjection.PositionLocal(_, _, witnesses) => witnesses
      case _                                                       => Set.empty

final case class StrategicDeltaEvidenceRef(
    primitiveKind: PrimitiveKind,
    anchorSquares: List[Square] = Nil,
    contestedSquares: List[Square] = Nil,
    lane: Option[File] = None
):
  def normalized: StrategicDeltaEvidenceRef =
    copy(
      anchorSquares = anchorSquares.distinct.sortBy(_.key),
      contestedSquares = contestedSquares.distinct.sortBy(_.key)
    )

final case class StrategicObjectDelta(
    objectId: String,
    family: StrategicObjectFamily,
    owner: Color,
    scope: StrategicDeltaScope,
    profile: StrategicObjectProfile,
    projection: StrategicDeltaProjection,
    changedAnchors: List[StrategicObjectAnchor] = Nil,
    supportingObjectIds: List[String] = Nil,
    rivalObjectIds: List[String] = Nil,
    evidenceRefs: List[StrategicDeltaEvidenceRef] = Nil
):
  require(objectId.nonEmpty, "strategic delta object id must be non-empty")
  require(profile.family == family, s"strategic delta profile ${profile.family} must match family $family")

  def primaryTag: StrategicDeltaTag = projection.primaryTag

  def moveTransition: Option[StrategicMoveTransitionWitness] = projection.moveWitness

  def comparativeWitness: Option[StrategicComparativeWitness] = projection.comparativeWitness

  def comparativeProfile: Option[StrategicComparativeProfile] = projection.comparativeProfile

  def positionLocalWitnesses: Set[StrategicPositionLocalWitness] = projection.positionLocalWitnesses
