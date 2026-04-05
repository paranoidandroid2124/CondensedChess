package lila.llm.strategicobject

enum StrategicObjectFamily:
  case PawnStructureRegime
  case KingSafetyShell
  case DevelopmentCoordinationState
  case PieceRoleFitness
  case SpaceClamp
  case CriticalSquareComplex
  case FixedTargetComplex
  case BreakAxis
  case AccessNetwork
  case CounterplayAxis
  case RestrictionShell
  case MobilityCage
  case RedeploymentRoute
  case DefenderDependencyNetwork
  case TradeInvariant
  case TensionState
  case AttackScaffold
  case MaterialInvestmentContract
  case InitiativeWindow
  case PlanRace
  case TransitionBridge
  case ConversionFunnel
  case PasserComplex
  case FortressHoldingShell

object StrategicObjectFamily:

  val boardDirectFamilies: Set[StrategicObjectFamily] = Set(
    StrategicObjectFamily.PawnStructureRegime,
    StrategicObjectFamily.KingSafetyShell,
    StrategicObjectFamily.DevelopmentCoordinationState,
    StrategicObjectFamily.PieceRoleFitness,
    StrategicObjectFamily.SpaceClamp,
    StrategicObjectFamily.CriticalSquareComplex,
    StrategicObjectFamily.FixedTargetComplex,
    StrategicObjectFamily.BreakAxis,
    StrategicObjectFamily.AccessNetwork,
    StrategicObjectFamily.CounterplayAxis,
    StrategicObjectFamily.RestrictionShell,
    StrategicObjectFamily.MobilityCage,
    StrategicObjectFamily.RedeploymentRoute,
    StrategicObjectFamily.PasserComplex
  )

  val graphDerivedFamilies: Set[StrategicObjectFamily] =
    values.toSet -- boardDirectFamilies
