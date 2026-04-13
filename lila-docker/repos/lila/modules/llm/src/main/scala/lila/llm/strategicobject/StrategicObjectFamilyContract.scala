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
    typedAxisCount: Int = 0,
    bridgeWitnessCount: Int = 0,
    continuationWitnessCount: Int = 0,
    exitWitnessCount: Int = 0,
    entryLinkCount: Int = 0,
    exitLinkCount: Int = 0,
    roleOverlapCount: Int = 0,
    nonTradeExitCount: Int = 0,
    deniedEntryCount: Int = 0,
    blockadeCount: Int = 0,
    defendedSquareCount: Int = 0,
    defenderPieceCount: Int = 0,
    pressureSquareCount: Int = 0,
    entryWitnessCount: Int = 0,
    targetWitnessCount: Int = 0,
    timingWitnessCount: Int = 0,
    preservedSourceCount: Int = 0,
    exchangeWitnessCount: Int = 0,
    persistenceWitnessCount: Int = 0,
    goalWitnessCount: Int = 0,
    ownGoalWitnessCount: Int = 0,
    rivalGoalWitnessCount: Int = 0,
    dualClockCount: Int = 0,
    orderingWitnessCount: Int = 0,
    activeFileCount: Int = 0,
    laggingPieceCount: Int = 0,
    coordinationSquareCount: Int = 0,
    repairSquareCount: Int = 0,
    escapeSquareCount: Int = 0,
    clampSquareCount: Int = 0,
    counterspaceSquareCount: Int = 0,
    deniedSquareCount: Int = 0,
    routeSquareCount: Int = 0,
    routeWaypointCount: Int = 0,
    routeTempoGain: Int = 0
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

  private val ComparativeCounterpartFamilies: Map[StrategicObjectFamily, Set[StrategicObjectFamily]] = Map(
    StrategicObjectFamily.PawnStructureRegime -> Set(StrategicObjectFamily.BreakAxis),
    StrategicObjectFamily.KingSafetyShell -> Set(StrategicObjectFamily.AccessNetwork),
    StrategicObjectFamily.DevelopmentCoordinationState -> Set(StrategicObjectFamily.RedeploymentRoute),
    StrategicObjectFamily.PieceRoleFitness -> Set(StrategicObjectFamily.MobilityCage),
    StrategicObjectFamily.SpaceClamp -> Set(StrategicObjectFamily.RestrictionShell),
    StrategicObjectFamily.CriticalSquareComplex -> Set(StrategicObjectFamily.PasserComplex),
    StrategicObjectFamily.FixedTargetComplex -> Set(StrategicObjectFamily.RestrictionShell, StrategicObjectFamily.BreakAxis),
    StrategicObjectFamily.BreakAxis -> Set(StrategicObjectFamily.PawnStructureRegime, StrategicObjectFamily.CounterplayAxis),
    StrategicObjectFamily.AccessNetwork -> Set(StrategicObjectFamily.KingSafetyShell, StrategicObjectFamily.RedeploymentRoute),
    StrategicObjectFamily.CounterplayAxis -> Set(StrategicObjectFamily.BreakAxis),
    StrategicObjectFamily.RestrictionShell -> Set(
      StrategicObjectFamily.SpaceClamp,
      StrategicObjectFamily.FixedTargetComplex,
      StrategicObjectFamily.MobilityCage
    ),
    StrategicObjectFamily.MobilityCage -> Set(StrategicObjectFamily.PieceRoleFitness, StrategicObjectFamily.RestrictionShell),
    StrategicObjectFamily.RedeploymentRoute -> Set(StrategicObjectFamily.DevelopmentCoordinationState, StrategicObjectFamily.AccessNetwork),
    StrategicObjectFamily.PasserComplex -> Set(StrategicObjectFamily.CriticalSquareComplex)
  )

  private val SameOwnerComparativePairs: Set[Set[StrategicObjectFamily]] = Set(
    Set(StrategicObjectFamily.PieceRoleFitness, StrategicObjectFamily.MobilityCage),
    Set(StrategicObjectFamily.DevelopmentCoordinationState, StrategicObjectFamily.RedeploymentRoute),
    Set(StrategicObjectFamily.FixedTargetComplex, StrategicObjectFamily.RestrictionShell)
  )

  val all: Map[StrategicObjectFamily, StrategicObjectFamilyContract] =
    StrategicObjectFamily.values.map(family => family -> contractFor(family)).toMap

  def forFamily(family: StrategicObjectFamily): StrategicObjectFamilyContract =
    all(family)

  def comparativeFamiliesCompatible(
      current: StrategicObjectFamily,
      other: StrategicObjectFamily
  ): Boolean =
    current == other || ComparativeCounterpartFamilies.getOrElse(current, Set.empty).contains(other)

  def comparativeOwnersCompatible(
      current: StrategicObjectFamily,
      other: StrategicObjectFamily,
      sameOwner: Boolean
  ): Boolean =
    !sameOwner || SameOwnerComparativePairs.contains(Set(current, other))

  def comparativeCounterpartAdmissible(
      current: StrategicObjectFamily,
      other: StrategicObjectFamily,
      witness: StrategicComparativeWitness,
      sameOwner: Boolean
  ): Boolean =
    comparativeFamiliesCompatible(current, other) &&
      comparativeOwnersCompatible(current, other, sameOwner) &&
      witness.hasExactCounterpartWitness &&
      counterpartPairGate(current, other, witness)

  def comparativeAxisSatisfied(
      family: StrategicObjectFamily,
      witness: StrategicComparativeWitness
  ): Boolean =
    family match
      case StrategicObjectFamily.KingSafetyShell =>
        witness.matchedFiles.nonEmpty || witness.matchedSquares.nonEmpty
      case StrategicObjectFamily.FixedTargetComplex =>
        witness.matchedSquares.nonEmpty
      case StrategicObjectFamily.BreakAxis =>
        witness.matchedSquares.nonEmpty ||
          witness.relationWitnesses.contains(StrategicRelationOperator.RacesWith) ||
          witness.relationWitnesses.contains(StrategicRelationOperator.Denies)
      case StrategicObjectFamily.AccessNetwork =>
        witness.matchedFiles.nonEmpty || witness.relationWitnesses.nonEmpty
      case StrategicObjectFamily.CounterplayAxis =>
        witness.matchedSquares.nonEmpty
      case StrategicObjectFamily.RestrictionShell =>
        witness.matchedSquares.nonEmpty || witness.relationWitnesses.nonEmpty
      case StrategicObjectFamily.PasserComplex =>
        witness.matchedSquares.nonEmpty || witness.matchedFiles.nonEmpty
      case _ =>
        witness.isFamilyAware

  private def contractFor(family: StrategicObjectFamily): StrategicObjectFamilyContract =
    family match
      case StrategicObjectFamily.PawnStructureRegime =>
        StrategicObjectFamilyContract(family, defaultReadiness = StrategicObjectReadiness.Stable)
      case StrategicObjectFamily.KingSafetyShell =>
        StrategicObjectFamilyContract(
          family,
          minimumAnchorPattern = MinimumAnchorPattern.ShellEntryScaffold,
          forbiddenLoosePatterns = Set(ForbiddenLoosePattern.BroadOverlapOnly, ForbiddenLoosePattern.ShellPressureOnly),
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
        StrategicObjectFamilyContract(
          family,
          minimumAnchorPattern = MinimumAnchorPattern.SharedOrTypedAnchor,
          forbiddenLoosePatterns = Set(ForbiddenLoosePattern.BroadOverlapOnly),
          defaultReadiness = StrategicObjectReadiness.Stable
        )
      case StrategicObjectFamily.CounterplayAxis =>
        StrategicObjectFamilyContract(
          family,
          requiredPrimitiveKinds = Set(PrimitiveKind.CounterplayResourceSeed),
          minimumAnchorPattern = MinimumAnchorPattern.SharedOrTypedAnchor,
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
        StrategicObjectFamilyContract(
          family,
          minimumAnchorPattern = MinimumAnchorPattern.SourceDestinationChain,
          forbiddenLoosePatterns = Set(ForbiddenLoosePattern.BroadOverlapOnly),
          defaultReadiness = StrategicObjectReadiness.Provisional
        )
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

  private def counterpartPairGate(
      current: StrategicObjectFamily,
      other: StrategicObjectFamily,
      witness: StrategicComparativeWitness
  ): Boolean =
    (current, other) match
      case (StrategicObjectFamily.PieceRoleFitness, StrategicObjectFamily.MobilityCage) |
          (StrategicObjectFamily.MobilityCage, StrategicObjectFamily.PieceRoleFitness) =>
        witness.counterpartWitnessKinds.contains(StrategicCounterpartWitnessKind.SharedPiece)
      case (StrategicObjectFamily.DevelopmentCoordinationState, StrategicObjectFamily.RedeploymentRoute) |
          (StrategicObjectFamily.RedeploymentRoute, StrategicObjectFamily.DevelopmentCoordinationState) =>
        witness.counterpartWitnessKinds.contains(StrategicCounterpartWitnessKind.SharedRoute)
      case (StrategicObjectFamily.RestrictionShell, StrategicObjectFamily.FixedTargetComplex) |
          (StrategicObjectFamily.FixedTargetComplex, StrategicObjectFamily.RestrictionShell) =>
        witness.counterpartWitnessKinds.contains(StrategicCounterpartWitnessKind.SharedTarget)
      case (StrategicObjectFamily.CounterplayAxis, StrategicObjectFamily.BreakAxis) |
          (StrategicObjectFamily.BreakAxis, StrategicObjectFamily.CounterplayAxis) =>
        (
          witness.counterpartWitnessKinds.contains(StrategicCounterpartWitnessKind.SharedSquare) ||
            witness.counterpartWitnessKinds.contains(StrategicCounterpartWitnessKind.DirectRivalReference)
        ) && witness.rivalPrimitiveKinds.contains(PrimitiveKind.BreakCandidate)
      case _ =>
        true

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
              op == StrategicRelationOperator.Preserves ||
                op == StrategicRelationOperator.TransformsTo ||
                op == StrategicRelationOperator.DependsOn ||
                op == StrategicRelationOperator.Enables
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
        val timedRaceFamilies =
          Set(
            StrategicObjectFamily.CounterplayAxis,
            StrategicObjectFamily.PasserComplex,
            StrategicObjectFamily.InitiativeWindow
          )
        val linkedRace =
          evidence.relationOperators.exists(op =>
            op == StrategicRelationOperator.RacesWith ||
              op == StrategicRelationOperator.Denies ||
              op == StrategicRelationOperator.OverloadsOrUndermines
          )
        ownCore.nonEmpty &&
          rivalCore.nonEmpty &&
          evidence.sourceFamilies.intersect(timedRaceFamilies).nonEmpty &&
          evidence.rivalFamilies.intersect(timedRaceFamilies).nonEmpty &&
          evidence.metrics.ownGoalWitnessCount > 0 &&
          evidence.metrics.rivalGoalWitnessCount > 0 &&
          evidence.metrics.dualClockCount >= 2 &&
          evidence.metrics.orderingWitnessCount > 0 &&
          evidence.metrics.goalWitnessCount > 0 &&
          (
            evidence.metrics.sharedAnchorCount > 0 ||
              evidence.metrics.contestedOverlapCount > 0 ||
              linkedRace
          )
      case StrategicObjectFamily.TransitionBridge =>
        val chainedTransition =
          evidence.relationOperators.exists(op =>
            op == StrategicRelationOperator.Preserves || op == StrategicRelationOperator.TransformsTo
          )
        evidence.sourceFamilies.contains(StrategicObjectFamily.PawnStructureRegime) &&
          evidence.destinationFamilies.nonEmpty &&
          evidence.metrics.bridgeWitnessCount > 0 &&
          (
            chainedTransition ||
              (
                evidence.destinationFamilies.contains(StrategicObjectFamily.PasserComplex) &&
                  evidence.metrics.sharedAnchorCount > 0
              )
          ) &&
          (
            evidence.metrics.sharedAnchorCount > 0 ||
              chainedTransition
          )
      case StrategicObjectFamily.FortressHoldingShell =>
        evidence.sourceFamilies.contains(StrategicObjectFamily.RestrictionShell) &&
          evidence.sourceFamilies.contains(StrategicObjectFamily.MobilityCage) &&
          evidence.rivalFamilies.contains(StrategicObjectFamily.PasserComplex) &&
          evidence.metrics.deniedEntryCount > 0 &&
          evidence.metrics.blockadeCount > 0
      case StrategicObjectFamily.DefenderDependencyNetwork =>
        val constrainedDefender =
          evidence.sourceFamilies.exists(family =>
            family == StrategicObjectFamily.RestrictionShell ||
              family == StrategicObjectFamily.MobilityCage
          )
        val dependencyLink =
          evidence.relationOperators.exists(op =>
            op == StrategicRelationOperator.Denies ||
              op == StrategicRelationOperator.OverloadsOrUndermines ||
              op == StrategicRelationOperator.DependsOn
          )
        constrainedDefender &&
          dependencyLink &&
          evidence.metrics.sourceCount > 0 &&
          evidence.metrics.rivalCount > 0 &&
          evidence.primitiveKinds.contains(PrimitiveKind.DefendedResource) &&
          evidence.metrics.sharedAnchorCount > 0 &&
          evidence.metrics.defendedSquareCount > 0 &&
          evidence.metrics.defenderPieceCount > 0 &&
          evidence.metrics.pressureSquareCount > 0
      case StrategicObjectFamily.InitiativeWindow =>
        val strongActivator =
          evidence.sourceFamilies.exists(family =>
            family == StrategicObjectFamily.AttackScaffold
          )
        val stressedRival =
          evidence.rivalFamilies.contains(StrategicObjectFamily.KingSafetyShell) ||
            evidence.rivalFamilies.contains(StrategicObjectFamily.AttackScaffold)
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
        evidence.metrics.timingWitnessCount > 0 &&
          (strongActivator || accessBackedTiming) &&
          stressedRival &&
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
          evidence.metrics.pressureSquareCount > 0 &&
          evidence.metrics.targetWitnessCount > 1
      case StrategicObjectFamily.DevelopmentCoordinationState =>
        val activityBurden =
          evidence.metrics.activeFileCount + evidence.metrics.entryWitnessCount
        evidence.metrics.coordinationSquareCount >= 2 &&
          (
            (evidence.metrics.laggingPieceCount > 0 && activityBurden > 0) ||
              evidence.metrics.laggingPieceCount >= 2 ||
              evidence.metrics.activeFileCount >= 2
          )
      case StrategicObjectFamily.PieceRoleFitness =>
        evidence.primitiveKinds.contains(PrimitiveKind.PieceRoleIssue) &&
          evidence.metrics.escapeSquareCount <= 3
      case StrategicObjectFamily.SpaceClamp =>
        evidence.metrics.clampSquareCount >= 3 &&
          evidence.metrics.activeFileCount >= 2
      case StrategicObjectFamily.AccessNetwork =>
        evidence.metrics.entryWitnessCount > 0 &&
          (
            evidence.metrics.targetWitnessCount > 0 ||
              evidence.metrics.routeSquareCount >= 3
          )
      case StrategicObjectFamily.RestrictionShell =>
        evidence.metrics.pressureSquareCount > 0 &&
          evidence.metrics.targetWitnessCount > 1 &&
          evidence.primitiveKinds.intersect(Set(PrimitiveKind.TargetSquare, PrimitiveKind.CriticalSquare)).nonEmpty
      case StrategicObjectFamily.CounterplayAxis =>
        evidence.primitiveKinds.contains(PrimitiveKind.CounterplayResourceSeed) &&
          evidence.metrics.typedAxisCount == 1 &&
          (
            evidence.metrics.rivalCount > 0 ||
              evidence.metrics.rivalGoalWitnessCount > 0 ||
              evidence.rivalFamilies.nonEmpty
          ) &&
          evidence.relationOperators.nonEmpty &&
          evidence.metrics.entryWitnessCount > 0 &&
          evidence.metrics.targetWitnessCount > 0 &&
          evidence.metrics.goalWitnessCount > 0
      case StrategicObjectFamily.MobilityCage =>
        evidence.primitiveKinds.contains(PrimitiveKind.PieceRoleIssue) &&
          evidence.metrics.deniedSquareCount > 0 &&
          evidence.metrics.deniedSquareCount > evidence.metrics.repairSquareCount
      case StrategicObjectFamily.RedeploymentRoute =>
        evidence.metrics.routeSquareCount >= 3 &&
          (
            evidence.metrics.routeWaypointCount > 0 ||
              evidence.metrics.routeTempoGain > 0
          )
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
        evidence.metrics.exchangeWitnessCount > 0 &&
          evidence.metrics.persistenceWitnessCount > 0 &&
        evidence.metrics.preservedSourceCount >= 2 &&
          evidence.metrics.goalWitnessCount > 0
      case StrategicObjectFamily.ConversionFunnel =>
        evidence.sourceFamilies.intersect(
          Set(StrategicObjectFamily.FixedTargetComplex, StrategicObjectFamily.TradeInvariant)
        ).nonEmpty &&
          evidence.destinationFamilies.contains(StrategicObjectFamily.PasserComplex) &&
          evidence.metrics.roleOverlapCount == 0 &&
          evidence.metrics.nonTradeExitCount > 0 &&
          evidence.metrics.sourceCount > 0 &&
          evidence.metrics.bridgeWitnessCount > 0 &&
          evidence.metrics.continuationWitnessCount >= 2 &&
          evidence.metrics.entryLinkCount > 0 &&
          evidence.metrics.exitLinkCount > 0 &&
          evidence.metrics.exitWitnessCount > 0 &&
          evidence.metrics.goalWitnessCount > 0 &&
          evidence.relationOperators.exists(op =>
            op == StrategicRelationOperator.DependsOn ||
              op == StrategicRelationOperator.TransformsTo ||
              op == StrategicRelationOperator.Preserves ||
              op == StrategicRelationOperator.Enables
          )
      case _ =>
        true
