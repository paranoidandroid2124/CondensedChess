package lila.llm.strategicobject

import chess.{ Color, File, Role, Square }

enum PawnStructureFeature:
  case FixedTargets
  case BreakPressure
  case LeverContact
  case HookContact
  case MaintainedTension
  case PassedPawn
  case CentralPresence
  case FlankPresence

enum KingSafetyCondition:
  case Pressured
  case Fractured
  case Infiltrated

enum CoordinationStatus:
  case Lagging
  case Balanced
  case Leading

enum SpaceClampMode:
  case QueensideClamp
  case CentralClamp
  case KingsideClamp
  case BroadClamp

enum DefenderDependencyFeature:
  case FixedAnchor
  case RestrictedDefender
  case CounterplayBound
  case MobilityBound

enum TradeInvariantFeature:
  case FixedTargetAnchor
  case BreakAnchor
  case AccessAnchor
  case PasserAnchor
  case ReleaseOverlap

enum TensionFeature:
  case MaintainableContact
  case ReleasePressure
  case BreakPressure
  case CounterplayPressure
  case RestrictionOverlay

enum AttackScaffoldFeature:
  case FileAccess
  case RouteAccess
  case RedeploymentLift
  case CriticalEntry
  case HookPressure

enum MaterialInvestmentFeature:
  case InvestedMaterial
  case IncreasedDeficit
  case CounterplayCompensation
  case AccessCompensation
  case TargetCompensation
  case AttackCompensation

enum InitiativeWindowFeature:
  case BreakTiming
  case CounterplayTiming
  case AccessTiming
  case AttackTiming
  case ShellStress
  case ForcingHint

enum PlanRaceFeature:
  case BilateralCounterplay
  case BilateralAccess
  case BilateralPassers
  case BilateralInitiative
  case SharedTension

enum TransitionBridgeFeature:
  case StructureBridge
  case TradeBridge
  case AccessBridge
  case ConversionBridge
  case PasserBridge

enum ConversionFunnelFeature:
  case TargetEntry
  case RestrictionGate
  case TradeChannel
  case AccessChannel
  case PasserExit

enum HoldingShellFeature:
  case RestrictionWall
  case MobilityWall
  case CriticalHold
  case PasserBlockade
  case ShellCover

enum StrategicObjectProfile:
  case PawnStructureRegime(
      identity: Set[PawnStructureFeature],
      breakFiles: Set[File],
      fixedTargets: List[Square],
      passerSquares: List[Square],
      contactSquares: List[Square]
  )
  case KingSafetyShell(
      condition: KingSafetyCondition,
      accessFiles: Set[File],
      stressedSquares: List[Square],
      pressureSquares: List[Square]
  )
  case DevelopmentCoordinationState(
      status: CoordinationStatus,
      laggingPieces: List[StrategicPieceRef],
      activeFiles: Set[File],
      coordinationSquares: List[Square]
  )
  case PieceRoleFitness(
      issue: PieceRoleIssueKind,
      affectedPiece: StrategicPieceRef,
      repairTargets: List[Square]
  )
  case SpaceClamp(
      mode: SpaceClampMode,
      clampSquares: List[Square],
      pressureFiles: Set[File]
  )
  case CriticalSquareComplex(
      criticalKinds: Set[CriticalSquareKind],
      focalSquares: List[Square],
      pressure: Int
  )
  case FixedTargetComplex(
      targetSquare: Square,
      targetOwner: Color,
      occupantRoles: Set[Role],
      fixed: Boolean,
      defended: Boolean
  )
  case BreakAxis(
      sourceSquare: Square,
      breakSquare: Square,
      targetSquares: List[Square],
      mode: BreakMode,
      supportBalance: Int
  )
  case AccessNetwork(
      lane: Option[File],
      route: Option[StrategicRouteGeometry],
      roles: Set[Role],
      contestedSquares: List[Square]
  )
  case CounterplayAxis(
      resourceSquares: List[Square],
      breakSquares: List[Square],
      pressureSquares: List[Square]
  )
  case RestrictionShell(
      restrictedSquares: List[Square],
      contestedSquares: List[Square],
      constraintSquares: List[Square]
  )
  case MobilityCage(
      affectedPiece: StrategicPieceRef,
      deniedSquares: List[Square],
      repairTargets: List[Square]
  )
  case RedeploymentRoute(
      route: StrategicRouteGeometry,
      role: Role,
      mobilityGain: Option[Int]
  )
  case DefenderDependencyNetwork(
      defendedSquares: List[Square],
      defenderSquares: List[Square],
      pressureSquares: List[Square],
      defenderRoles: Set[Role],
      features: Set[DefenderDependencyFeature]
  )
  case TradeInvariant(
      exchangeSquares: List[Square],
      invariantSquares: List[Square],
      preservedFiles: Set[File],
      preservedFamilies: Set[StrategicObjectFamily],
      features: Set[TradeInvariantFeature]
  )
  case TensionState(
      contactSquares: List[Square],
      releaseSquares: List[Square],
      pressureSquares: List[Square],
      breakSquares: List[Square],
      features: Set[TensionFeature]
  )
  case AttackScaffold(
      targetOwner: Color,
      scaffoldSquares: List[Square],
      entryFiles: Set[File],
      entryRoutes: List[StrategicRouteGeometry],
      features: Set[AttackScaffoldFeature]
  )
  case MaterialInvestmentContract(
      investedMaterialCp: Int,
      beforeDeficit: Int,
      afterDeficit: Int,
      compensationSquares: List[Square],
      compensationFiles: Set[File],
      features: Set[MaterialInvestmentFeature]
  )
  case InitiativeWindow(
      windowSquares: List[Square],
      triggerFiles: Set[File],
      rivalPressureSquares: List[Square],
      catalystFamilies: Set[StrategicObjectFamily],
      features: Set[InitiativeWindowFeature]
  )
  case PlanRace(
      rivalOwner: Color,
      raceSquares: List[Square],
      raceFiles: Set[File],
      ownGoalSquares: List[Square],
      rivalGoalSquares: List[Square],
      features: Set[PlanRaceFeature]
  )
  case TransitionBridge(
      bridgeSquares: List[Square],
      bridgeFiles: Set[File],
      sourceFamilies: Set[StrategicObjectFamily],
      destinationFamilies: Set[StrategicObjectFamily],
      features: Set[TransitionBridgeFeature]
  )
  case ConversionFunnel(
      entrySquares: List[Square],
      channelSquares: List[Square],
      exitSquares: List[Square],
      funnelFiles: Set[File],
      features: Set[ConversionFunnelFeature]
  )
  case PasserComplex(
      passerSquare: Square,
      promotionSquare: Square,
      relativeRank: Int,
      protectedByPawn: Boolean,
      escortSquares: List[Square]
  )
  case FortressHoldingShell(
      holdSquares: List[Square],
      entryDeniedSquares: List[Square],
      blockadeSquares: List[Square],
      shellFiles: Set[File],
      features: Set[HoldingShellFeature]
  )

  def family: StrategicObjectFamily =
    this match
      case StrategicObjectProfile.PawnStructureRegime(_, _, _, _, _)           => StrategicObjectFamily.PawnStructureRegime
      case StrategicObjectProfile.KingSafetyShell(_, _, _, _)                  => StrategicObjectFamily.KingSafetyShell
      case StrategicObjectProfile.DevelopmentCoordinationState(_, _, _, _)     => StrategicObjectFamily.DevelopmentCoordinationState
      case StrategicObjectProfile.PieceRoleFitness(_, _, _)                    => StrategicObjectFamily.PieceRoleFitness
      case StrategicObjectProfile.SpaceClamp(_, _, _)                          => StrategicObjectFamily.SpaceClamp
      case StrategicObjectProfile.CriticalSquareComplex(_, _, _)               => StrategicObjectFamily.CriticalSquareComplex
      case StrategicObjectProfile.FixedTargetComplex(_, _, _, _, _)            => StrategicObjectFamily.FixedTargetComplex
      case StrategicObjectProfile.BreakAxis(_, _, _, _, _)                     => StrategicObjectFamily.BreakAxis
      case StrategicObjectProfile.AccessNetwork(_, _, _, _)                    => StrategicObjectFamily.AccessNetwork
      case StrategicObjectProfile.CounterplayAxis(_, _, _)                     => StrategicObjectFamily.CounterplayAxis
      case StrategicObjectProfile.RestrictionShell(_, _, _)                    => StrategicObjectFamily.RestrictionShell
      case StrategicObjectProfile.MobilityCage(_, _, _)                        => StrategicObjectFamily.MobilityCage
      case StrategicObjectProfile.RedeploymentRoute(_, _, _)                   => StrategicObjectFamily.RedeploymentRoute
      case StrategicObjectProfile.DefenderDependencyNetwork(_, _, _, _, _)     => StrategicObjectFamily.DefenderDependencyNetwork
      case StrategicObjectProfile.TradeInvariant(_, _, _, _, _)                => StrategicObjectFamily.TradeInvariant
      case StrategicObjectProfile.TensionState(_, _, _, _, _)                  => StrategicObjectFamily.TensionState
      case StrategicObjectProfile.AttackScaffold(_, _, _, _, _)                => StrategicObjectFamily.AttackScaffold
      case StrategicObjectProfile.MaterialInvestmentContract(_, _, _, _, _, _) => StrategicObjectFamily.MaterialInvestmentContract
      case StrategicObjectProfile.InitiativeWindow(_, _, _, _, _)              => StrategicObjectFamily.InitiativeWindow
      case StrategicObjectProfile.PlanRace(_, _, _, _, _, _)                   => StrategicObjectFamily.PlanRace
      case StrategicObjectProfile.TransitionBridge(_, _, _, _, _)              => StrategicObjectFamily.TransitionBridge
      case StrategicObjectProfile.ConversionFunnel(_, _, _, _, _)              => StrategicObjectFamily.ConversionFunnel
      case StrategicObjectProfile.PasserComplex(_, _, _, _, _)                 => StrategicObjectFamily.PasserComplex
      case StrategicObjectProfile.FortressHoldingShell(_, _, _, _, _)          => StrategicObjectFamily.FortressHoldingShell
