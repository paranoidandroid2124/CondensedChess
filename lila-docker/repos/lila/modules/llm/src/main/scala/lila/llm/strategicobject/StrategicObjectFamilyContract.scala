package lila.llm.strategicobject

import chess.{ File, Role, Square }

enum MinimumAnchorPattern:
  case NoneRequired
  case SharedContestedAnchor
  case SharedOrTypedAnchor
  case SourceDestinationChain
  case DefendedPressureTriad
  case DeniedEntryHold
  case ShellEntryScaffold

enum ForbiddenLoosePattern:
  case BroadOverlapOnly
  case BilateralPressureOnly
  case ShellPressureOnly
  case RestrictionOnly
  case TradeSquareOnly
  case SourceOnlyWithoutDestination

final case class FamilyGenerationMetrics(
    sourceCount: Int = 0,
    rivalCount: Int = 0,
    sharedAnchorCount: Int = 0,
    contestedOverlapCount: Int = 0,
    typedOverlapCount: Int = 0,
    deniedEntryCount: Int = 0,
    blockadeCount: Int = 0,
    defendedSquareCount: Int = 0,
    defenderPieceCount: Int = 0,
    pressureSquareCount: Int = 0,
    entryWitnessCount: Int = 0,
    targetWitnessCount: Int = 0,
    preservedSourceCount: Int = 0,
    goalWitnessCount: Int = 0
)

final case class FamilyGenerationEvidence(
    primitiveKinds: Set[PrimitiveKind] = Set.empty,
    sourceFamilies: Set[StrategicObjectFamily] = Set.empty,
    rivalFamilies: Set[StrategicObjectFamily] = Set.empty,
    destinationFamilies: Set[StrategicObjectFamily] = Set.empty,
    relationOperators: Set[StrategicRelationOperator] = Set.empty,
    anchorSquares: Set[Square] = Set.empty,
    contestedSquares: Set[Square] = Set.empty,
    files: Set[File] = Set.empty,
    pieceRoles: Set[Role] = Set.empty,
    metrics: FamilyGenerationMetrics = FamilyGenerationMetrics()
)

final case class StrategicObjectFamilyContract(
    family: StrategicObjectFamily,
    requiredPrimitiveKinds: Set[PrimitiveKind] = Set.empty,
    requiredSourceFamilies: Set[StrategicObjectFamily] = Set.empty,
    requiredRivalFamilies: Set[StrategicObjectFamily] = Set.empty,
    requiredRelationPatterns: Set[StrategicRelationOperator] = Set.empty,
    minimumAnchorPattern: MinimumAnchorPattern = MinimumAnchorPattern.NoneRequired,
    forbiddenLoosePatterns: Set[ForbiddenLoosePattern] = Set.empty,
    defaultReadiness: StrategicObjectReadiness
):
  def accepts(evidence: FamilyGenerationEvidence): Boolean =
    StrategicObjectFamilyContract.accepts(this, evidence)

object StrategicObjectFamilyContract:

  private val PlanRaceFamilies = Set(
    StrategicObjectFamily.CounterplayAxis,
    StrategicObjectFamily.AccessNetwork,
    StrategicObjectFamily.PasserComplex,
    StrategicObjectFamily.InitiativeWindow
  )

  private val InitiativeRelationPatterns = Set(
    StrategicRelationOperator.DependsOn,
    StrategicRelationOperator.OverloadsOrUndermines,
    StrategicRelationOperator.Denies
  )

  private val TensionFamilies = Set(
    StrategicObjectFamily.BreakAxis,
    StrategicObjectFamily.CounterplayAxis,
    StrategicObjectFamily.RestrictionShell
  )

  val all: Map[StrategicObjectFamily, StrategicObjectFamilyContract] =
    StrategicObjectFamily.values.map(family => family -> contractFor(family)).toMap

  def forFamily(family: StrategicObjectFamily): StrategicObjectFamilyContract =
    all(family)

  private def contractFor(family: StrategicObjectFamily): StrategicObjectFamilyContract =
    family match
      case StrategicObjectFamily.PawnStructureRegime =>
        StrategicObjectFamilyContract(family, defaultReadiness = StrategicObjectReadiness.Stable)
      case StrategicObjectFamily.KingSafetyShell =>
        StrategicObjectFamilyContract(
          family,
          minimumAnchorPattern = MinimumAnchorPattern.ShellEntryScaffold,
          forbiddenLoosePatterns = Set(ForbiddenLoosePattern.BroadOverlapOnly),
          defaultReadiness = StrategicObjectReadiness.Provisional
        )
      case StrategicObjectFamily.DevelopmentCoordinationState =>
        StrategicObjectFamilyContract(family, defaultReadiness = StrategicObjectReadiness.Provisional)
      case StrategicObjectFamily.PieceRoleFitness =>
        StrategicObjectFamilyContract(family, defaultReadiness = StrategicObjectReadiness.Provisional)
      case StrategicObjectFamily.SpaceClamp =>
        StrategicObjectFamilyContract(family, defaultReadiness = StrategicObjectReadiness.Provisional)
      case StrategicObjectFamily.CriticalSquareComplex =>
        StrategicObjectFamilyContract(family, defaultReadiness = StrategicObjectReadiness.Stable)
      case StrategicObjectFamily.FixedTargetComplex =>
        StrategicObjectFamilyContract(family, defaultReadiness = StrategicObjectReadiness.Stable)
      case StrategicObjectFamily.BreakAxis =>
        StrategicObjectFamilyContract(family, defaultReadiness = StrategicObjectReadiness.Stable)
      case StrategicObjectFamily.AccessNetwork =>
        StrategicObjectFamilyContract(family, defaultReadiness = StrategicObjectReadiness.Stable)
      case StrategicObjectFamily.CounterplayAxis =>
        StrategicObjectFamilyContract(
          family,
          requiredPrimitiveKinds = Set(PrimitiveKind.CounterplayResourceSeed),
          forbiddenLoosePatterns = Set(ForbiddenLoosePattern.BroadOverlapOnly),
          defaultReadiness = StrategicObjectReadiness.Provisional
        )
      case StrategicObjectFamily.RestrictionShell =>
        StrategicObjectFamilyContract(
          family,
          minimumAnchorPattern = MinimumAnchorPattern.SharedContestedAnchor,
          forbiddenLoosePatterns = Set(ForbiddenLoosePattern.BroadOverlapOnly),
          defaultReadiness = StrategicObjectReadiness.Provisional
        )
      case StrategicObjectFamily.MobilityCage =>
        StrategicObjectFamilyContract(family, defaultReadiness = StrategicObjectReadiness.Provisional)
      case StrategicObjectFamily.RedeploymentRoute =>
        StrategicObjectFamilyContract(family, defaultReadiness = StrategicObjectReadiness.Provisional)
      case StrategicObjectFamily.DefenderDependencyNetwork =>
        StrategicObjectFamilyContract(
          family,
          requiredPrimitiveKinds = Set(PrimitiveKind.DefendedResource),
          minimumAnchorPattern = MinimumAnchorPattern.DefendedPressureTriad,
          forbiddenLoosePatterns = Set(ForbiddenLoosePattern.BroadOverlapOnly),
          defaultReadiness = StrategicObjectReadiness.DeferredForDelta
        )
      case StrategicObjectFamily.TradeInvariant =>
        StrategicObjectFamilyContract(
          family,
          requiredPrimitiveKinds = Set(PrimitiveKind.ExchangeSquare),
          minimumAnchorPattern = MinimumAnchorPattern.SharedContestedAnchor,
          forbiddenLoosePatterns = Set(ForbiddenLoosePattern.TradeSquareOnly),
          defaultReadiness = StrategicObjectReadiness.Stable
        )
      case StrategicObjectFamily.TensionState =>
        StrategicObjectFamilyContract(
          family,
          minimumAnchorPattern = MinimumAnchorPattern.SharedContestedAnchor,
          forbiddenLoosePatterns = Set(ForbiddenLoosePattern.BroadOverlapOnly),
          defaultReadiness = StrategicObjectReadiness.Provisional
        )
      case StrategicObjectFamily.AttackScaffold =>
        StrategicObjectFamilyContract(
          family,
          requiredRivalFamilies = Set(StrategicObjectFamily.KingSafetyShell),
          minimumAnchorPattern = MinimumAnchorPattern.ShellEntryScaffold,
          forbiddenLoosePatterns = Set(ForbiddenLoosePattern.ShellPressureOnly),
          defaultReadiness = StrategicObjectReadiness.Provisional
        )
      case StrategicObjectFamily.MaterialInvestmentContract =>
        StrategicObjectFamilyContract(family, defaultReadiness = StrategicObjectReadiness.DeferredForDelta)
      case StrategicObjectFamily.InitiativeWindow =>
        StrategicObjectFamilyContract(
          family,
          requiredRelationPatterns = InitiativeRelationPatterns,
          minimumAnchorPattern = MinimumAnchorPattern.SharedContestedAnchor,
          forbiddenLoosePatterns = Set(ForbiddenLoosePattern.BroadOverlapOnly),
          defaultReadiness = StrategicObjectReadiness.DeferredForDelta
        )
      case StrategicObjectFamily.PlanRace =>
        StrategicObjectFamilyContract(
          family,
          minimumAnchorPattern = MinimumAnchorPattern.SharedOrTypedAnchor,
          forbiddenLoosePatterns = Set(ForbiddenLoosePattern.BroadOverlapOnly, ForbiddenLoosePattern.BilateralPressureOnly),
          defaultReadiness = StrategicObjectReadiness.DeferredForDelta
        )
      case StrategicObjectFamily.TransitionBridge =>
        StrategicObjectFamilyContract(
          family,
          requiredSourceFamilies = Set(StrategicObjectFamily.PawnStructureRegime),
          minimumAnchorPattern = MinimumAnchorPattern.SourceDestinationChain,
          forbiddenLoosePatterns = Set(ForbiddenLoosePattern.BroadOverlapOnly, ForbiddenLoosePattern.SourceOnlyWithoutDestination),
          defaultReadiness = StrategicObjectReadiness.DeferredForDelta
        )
      case StrategicObjectFamily.ConversionFunnel =>
        StrategicObjectFamilyContract(family, defaultReadiness = StrategicObjectReadiness.Provisional)
      case StrategicObjectFamily.PasserComplex =>
        StrategicObjectFamilyContract(family, defaultReadiness = StrategicObjectReadiness.Stable)
      case StrategicObjectFamily.FortressHoldingShell =>
        StrategicObjectFamilyContract(
          family,
          requiredSourceFamilies = Set(StrategicObjectFamily.RestrictionShell, StrategicObjectFamily.MobilityCage),
          minimumAnchorPattern = MinimumAnchorPattern.DeniedEntryHold,
          forbiddenLoosePatterns = Set(ForbiddenLoosePattern.RestrictionOnly, ForbiddenLoosePattern.BroadOverlapOnly),
          defaultReadiness = StrategicObjectReadiness.DeferredForDelta
        )

  private def accepts(
      contract: StrategicObjectFamilyContract,
      evidence: FamilyGenerationEvidence
  ): Boolean =
    val primitiveGate = contract.requiredPrimitiveKinds.subsetOf(evidence.primitiveKinds)
    val sourceGate = contract.requiredSourceFamilies.subsetOf(evidence.sourceFamilies)
    val rivalGate = contract.requiredRivalFamilies.subsetOf(evidence.rivalFamilies)
    val relationGate =
      contract.requiredRelationPatterns.isEmpty ||
        contract.requiredRelationPatterns.intersect(evidence.relationOperators).nonEmpty
    val anchorGate =
      contract.minimumAnchorPattern match
        case MinimumAnchorPattern.NoneRequired => true
        case MinimumAnchorPattern.SharedContestedAnchor =>
          evidence.metrics.sharedAnchorCount > 0 || evidence.metrics.contestedOverlapCount > 0
        case MinimumAnchorPattern.SharedOrTypedAnchor =>
          evidence.metrics.sharedAnchorCount > 0 ||
            evidence.metrics.contestedOverlapCount > 0 ||
            evidence.metrics.typedOverlapCount > 0
        case MinimumAnchorPattern.SourceDestinationChain =>
          evidence.metrics.sharedAnchorCount > 0 ||
            evidence.relationOperators.exists(op =>
              op == StrategicRelationOperator.Preserves || op == StrategicRelationOperator.TransformsTo
            )
        case MinimumAnchorPattern.DefendedPressureTriad =>
          evidence.metrics.defendedSquareCount > 0 &&
            evidence.metrics.defenderPieceCount > 0 &&
            evidence.metrics.pressureSquareCount > 0
        case MinimumAnchorPattern.DeniedEntryHold =>
          evidence.metrics.deniedEntryCount > 0 && evidence.metrics.blockadeCount > 0
        case MinimumAnchorPattern.ShellEntryScaffold =>
          evidence.metrics.entryWitnessCount > 0 &&
            evidence.metrics.targetWitnessCount > 0 &&
            evidence.metrics.pressureSquareCount > 0
    val looseGate =
      !contract.forbiddenLoosePatterns.exists {
        case ForbiddenLoosePattern.BroadOverlapOnly =>
          evidence.metrics.sharedAnchorCount == 0 &&
            evidence.metrics.contestedOverlapCount == 0 &&
            evidence.relationOperators.isEmpty
        case ForbiddenLoosePattern.BilateralPressureOnly =>
          evidence.metrics.typedOverlapCount == 0 &&
            evidence.metrics.sharedAnchorCount == 0 &&
            evidence.metrics.contestedOverlapCount == 0
        case ForbiddenLoosePattern.ShellPressureOnly =>
          evidence.metrics.entryWitnessCount == 0 || evidence.metrics.targetWitnessCount == 0
        case ForbiddenLoosePattern.RestrictionOnly =>
          evidence.sourceFamilies == Set(StrategicObjectFamily.RestrictionShell)
        case ForbiddenLoosePattern.TradeSquareOnly =>
          evidence.metrics.preservedSourceCount < 2
        case ForbiddenLoosePattern.SourceOnlyWithoutDestination =>
          evidence.sourceFamilies.nonEmpty && evidence.destinationFamilies.isEmpty
      }

    primitiveGate && sourceGate && rivalGate && relationGate && anchorGate && looseGate && familyGate(contract.family, evidence)

  private def familyGate(
      family: StrategicObjectFamily,
      evidence: FamilyGenerationEvidence
  ): Boolean =
    family match
      case StrategicObjectFamily.PlanRace =>
        val ownCore = evidence.sourceFamilies.intersect(PlanRaceFamilies)
        val rivalCore = evidence.rivalFamilies.intersect(PlanRaceFamilies)
        ownCore.nonEmpty &&
          rivalCore.nonEmpty &&
          evidence.metrics.goalWitnessCount >= 2 &&
          (
            evidence.metrics.typedOverlapCount > 0 ||
              evidence.metrics.sharedAnchorCount > 0 ||
              evidence.metrics.contestedOverlapCount > 0
          )
      case StrategicObjectFamily.TransitionBridge =>
        evidence.sourceFamilies.contains(StrategicObjectFamily.PawnStructureRegime) &&
          evidence.sourceFamilies.nonEmpty &&
          evidence.destinationFamilies.nonEmpty &&
          (
            evidence.destinationFamilies.contains(StrategicObjectFamily.PasserComplex) ||
              evidence.relationOperators.exists(op =>
                op == StrategicRelationOperator.Preserves || op == StrategicRelationOperator.TransformsTo
              )
          ) &&
          (
            evidence.metrics.sharedAnchorCount > 0 ||
              evidence.relationOperators.exists(op =>
                op == StrategicRelationOperator.Preserves || op == StrategicRelationOperator.TransformsTo
              )
          )
      case StrategicObjectFamily.FortressHoldingShell =>
        evidence.sourceFamilies.contains(StrategicObjectFamily.RestrictionShell) &&
          evidence.sourceFamilies.contains(StrategicObjectFamily.MobilityCage) &&
          evidence.rivalFamilies.contains(StrategicObjectFamily.PasserComplex) &&
          evidence.metrics.deniedEntryCount > 0 &&
          evidence.metrics.blockadeCount > 0
      case StrategicObjectFamily.DefenderDependencyNetwork =>
        evidence.primitiveKinds.contains(PrimitiveKind.DefendedResource) &&
          evidence.metrics.defendedSquareCount > 0 &&
          evidence.metrics.defenderPieceCount > 0 &&
          evidence.metrics.pressureSquareCount > 0
      case StrategicObjectFamily.InitiativeWindow =>
        val strongActivator =
          evidence.sourceFamilies.exists(family =>
            family == StrategicObjectFamily.AttackScaffold ||
              family == StrategicObjectFamily.BreakAxis ||
              family == StrategicObjectFamily.CounterplayAxis
          )
        val accessBackedTiming =
          evidence.sourceFamilies.contains(StrategicObjectFamily.AccessNetwork) &&
            evidence.primitiveKinds.exists(kind =>
              Set(
                PrimitiveKind.BreakCandidate,
                PrimitiveKind.ReleaseCandidate,
                PrimitiveKind.HookContactSeed,
                PrimitiveKind.TensionContactSeed
              ).contains(kind)
            )
        (strongActivator || accessBackedTiming) &&
          evidence.rivalFamilies.nonEmpty &&
          (
            evidence.metrics.sharedAnchorCount > 0 ||
              evidence.metrics.contestedOverlapCount > 0 ||
              evidence.relationOperators.intersect(InitiativeRelationPatterns).nonEmpty
          )
      case StrategicObjectFamily.AttackScaffold =>
        val routedAttack =
          evidence.primitiveKinds.exists(kind =>
            Set(
              PrimitiveKind.HookContactSeed,
              PrimitiveKind.DiagonalLaneSeed,
              PrimitiveKind.LiftCorridorSeed,
              PrimitiveKind.KnightRouteSeed,
              PrimitiveKind.RedeploymentPathSeed
            ).contains(kind)
          )
        evidence.rivalFamilies.contains(StrategicObjectFamily.KingSafetyShell) &&
          routedAttack &&
          evidence.metrics.entryWitnessCount > 0 &&
          evidence.metrics.targetWitnessCount > 0
      case StrategicObjectFamily.KingSafetyShell =>
        evidence.metrics.entryWitnessCount > 0 &&
          evidence.metrics.pressureSquareCount > 0
      case StrategicObjectFamily.RestrictionShell =>
        evidence.metrics.pressureSquareCount > 0 &&
          evidence.primitiveKinds.intersect(Set(PrimitiveKind.TargetSquare, PrimitiveKind.CriticalSquare)).nonEmpty
      case StrategicObjectFamily.CounterplayAxis =>
        evidence.primitiveKinds.contains(PrimitiveKind.CounterplayResourceSeed) &&
          (evidence.metrics.entryWitnessCount > 0 || evidence.metrics.pressureSquareCount > 1)
      case StrategicObjectFamily.TensionState =>
        evidence.primitiveKinds.exists(kind =>
          kind == PrimitiveKind.TensionContactSeed || kind == PrimitiveKind.LeverContactSeed
        ) &&
          (
            evidence.metrics.pressureSquareCount > 0 ||
              evidence.sourceFamilies.intersect(TensionFamilies).nonEmpty ||
              evidence.rivalFamilies.intersect(TensionFamilies).nonEmpty
          )
      case StrategicObjectFamily.TradeInvariant =>
        evidence.metrics.preservedSourceCount >= 2 &&
          evidence.metrics.goalWitnessCount > 0
      case _ =>
        true
