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
  case PasserComplex(
      passerSquare: Square,
      promotionSquare: Square,
      relativeRank: Int,
      protectedByPawn: Boolean,
      escortSquares: List[Square]
  )
