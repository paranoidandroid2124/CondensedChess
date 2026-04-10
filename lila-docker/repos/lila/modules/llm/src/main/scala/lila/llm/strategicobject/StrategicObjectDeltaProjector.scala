package lila.llm.strategicobject

import chess.{ Color, File, Square }
import lila.llm.analysis.{ DecisiveTruthContract, MoveTruthFrame }

trait StrategicObjectDeltaProjector:
  def project(
      contract: DecisiveTruthContract,
      truth: MoveTruthFrame,
      objects: List[StrategicObject]
  ): List[StrategicObjectDelta]

object CanonicalStrategicObjectDeltaProjector extends StrategicObjectDeltaProjector:

  private val MoveTransitionRelationOps = Set(
    StrategicRelationOperator.Enables,
    StrategicRelationOperator.Preserves,
    StrategicRelationOperator.TransformsTo,
    StrategicRelationOperator.DependsOn,
    StrategicRelationOperator.Denies,
    StrategicRelationOperator.OverloadsOrUndermines
  )

  private final case class ComparativeCandidate(
      rival: StrategicObject,
      witness: StrategicComparativeWitness
  )

  private final case class ComparativeSelection(
      witness: StrategicComparativeWitness,
      rivals: List[StrategicObject],
      balance: StrategicComparativeBalance,
      profile: StrategicComparativeProfile
  )

  private final case class DeltaContext(
      supportObjects: List[StrategicObject],
      comparative: Option[ComparativeSelection]
  ):
    def rivalObjects: List[StrategicObject] =
      comparative.map(_.rivals).getOrElse(Nil)

  def project(
      contract: DecisiveTruthContract,
      truth: MoveTruthFrame,
      objects: List[StrategicObject]
  ): List[StrategicObjectDelta] =
    val objectsById = objects.map(obj => obj.id -> obj).toMap
    objects
      .filter(deltaFamilyEligible)
      .flatMap { obj =>
        val context = deltaContext(obj, objects, objectsById)
        eligibleScopes(contract, truth, obj).flatMap { scope =>
          projectionFor(contract, truth, obj, scope, context).map { projection =>
            StrategicObjectDelta(
              objectId = obj.id,
              family = obj.family,
              owner = obj.owner,
              scope = scope,
              profile = obj.profile,
              projection = projection,
              changedAnchors = changedAnchors(obj, scope, projection),
              supportingObjectIds = context.supportObjects.map(_.id).distinct.sorted.take(4),
              rivalObjectIds = context.rivalObjects.map(_.id).distinct.sorted.take(4),
              evidenceRefs = evidenceRefs(obj)
            )
          }
        }
      }
      .sortBy(delta => (delta.family.ordinal, colorIndex(delta.owner), delta.objectId, delta.scope.ordinal))

  private def eligibleScopes(
      contract: DecisiveTruthContract,
      truth: MoveTruthFrame,
      obj: StrategicObject
  ): List[StrategicDeltaScope] =
    if TradeInvariantSimplificationSlice.isPacketOwnedPrimarySimplificationObject(obj) then
      Option.when(hasTransitionTruth(contract, truth))(StrategicDeltaScope.MoveLocal).toList
    else
      obj.readiness match
        case StrategicObjectReadiness.Stable =>
          Option.when(hasTransitionTruth(contract, truth))(StrategicDeltaScope.MoveLocal).toList ++
            List(StrategicDeltaScope.PositionLocal, StrategicDeltaScope.Comparative)
        case StrategicObjectReadiness.Provisional =>
          List(StrategicDeltaScope.PositionLocal) ++
            Option.when(contract.hasVisibleTruth || contract.prefersDecisivePromotion)(
              StrategicDeltaScope.Comparative
            )
        case StrategicObjectReadiness.DeferredForDelta =>
          Nil

  private def deltaFamilyEligible(
      obj: StrategicObject
  ): Boolean =
    StrategicObjectFamily.directDeltaOwners.contains(obj.family) ||
      TradeInvariantSimplificationSlice.isPacketOwnedPrimarySimplificationObject(obj)

  private def projectionFor(
      contract: DecisiveTruthContract,
      truth: MoveTruthFrame,
      obj: StrategicObject,
      scope: StrategicDeltaScope,
      context: DeltaContext
  ): Option[StrategicDeltaProjection] =
    scope match
      case StrategicDeltaScope.MoveLocal =>
        for
          move <- moveTrace(contract, truth)
          axis <- transitionAxisFor(obj.profile)
          tag <- moveLocalTag(obj.profile, axis)
          witness <- moveTransitionWitness(move, obj, axis, context)
        yield StrategicDeltaProjection.MoveLocal(change = tag, witness = witness)
      case StrategicDeltaScope.PositionLocal =>
        Some(
          StrategicDeltaProjection.PositionLocal(
            state = positionLocalTag(obj),
            focalAnchorCount = changedAnchors(obj, scope, positionProjection(obj)).size
          )
        )
      case StrategicDeltaScope.Comparative =>
        context.comparative.map(selection =>
          StrategicDeltaProjection.Comparative(
            contrast = comparativeTag(selection.balance),
            balance = selection.balance,
            witness = selection.witness,
            counterpartObjectIds = selection.rivals.map(_.id).distinct.sorted.take(4),
            profile = selection.profile
          )
        )

  private def positionProjection(
      obj: StrategicObject
  ): StrategicDeltaProjection =
    StrategicDeltaProjection.PositionLocal(
      state = positionLocalTag(obj),
      focalAnchorCount = 1
    )

  private def transitionAxisFor(
      profile: StrategicObjectProfile
  ): Option[StrategicMoveTransitionAxis] =
    profile match
      case StrategicObjectProfile.PawnStructureRegime(_, breakFiles, fixedTargets, passerSquares, contactSquares) =>
        Some(
          if breakFiles.nonEmpty || contactSquares.nonEmpty then StrategicMoveTransitionAxis.PawnBreakContact
          else if fixedTargets.nonEmpty then StrategicMoveTransitionAxis.PawnFixation
          else if passerSquares.nonEmpty then StrategicMoveTransitionAxis.PawnPasserPush
          else StrategicMoveTransitionAxis.PawnBreakContact
        )
      case _: StrategicObjectProfile.KingSafetyShell              => Some(StrategicMoveTransitionAxis.KingEntryPressure)
      case _: StrategicObjectProfile.DevelopmentCoordinationState => Some(StrategicMoveTransitionAxis.DevelopmentCoordinationShift)
      case _: StrategicObjectProfile.PieceRoleFitness             => Some(StrategicMoveTransitionAxis.RoleRepairOrTrap)
      case _: StrategicObjectProfile.SpaceClamp                   => Some(StrategicMoveTransitionAxis.SpaceClampAdvance)
      case _: StrategicObjectProfile.CriticalSquareComplex        => Some(StrategicMoveTransitionAxis.CriticalSquareOccupation)
      case _: StrategicObjectProfile.FixedTargetComplex           => Some(StrategicMoveTransitionAxis.FixedTargetFixation)
      case _: StrategicObjectProfile.BreakAxis                    => Some(StrategicMoveTransitionAxis.BreakActivation)
      case _: StrategicObjectProfile.AccessNetwork                => Some(StrategicMoveTransitionAxis.AccessRouteActivation)
      case _: StrategicObjectProfile.CounterplayAxis              => Some(StrategicMoveTransitionAxis.CounterplayResourceShift)
      case _: StrategicObjectProfile.RestrictionShell             => Some(StrategicMoveTransitionAxis.RestrictionImposition)
      case _: StrategicObjectProfile.MobilityCage                => Some(StrategicMoveTransitionAxis.MobilityRestriction)
      case _: StrategicObjectProfile.RedeploymentRoute           => Some(StrategicMoveTransitionAxis.RedeploymentActivation)
      case _: StrategicObjectProfile.PasserComplex               => Some(StrategicMoveTransitionAxis.PasserAdvance)
      case _: StrategicObjectProfile.TradeInvariant              => Some(StrategicMoveTransitionAxis.TradeSimplification)
      case _ =>
        None

  private def moveLocalTag(
      profile: StrategicObjectProfile,
      axis: StrategicMoveTransitionAxis
  ): Option[StrategicDeltaTag] =
    (profile, axis) match
      case (StrategicObjectProfile.PawnStructureRegime(_, _, fixedTargets, _, _), StrategicMoveTransitionAxis.PawnFixation) =>
        Some(if fixedTargets.nonEmpty then StrategicDeltaTag.RegimeFixed else StrategicDeltaTag.BreakDrivenShift)
      case (StrategicObjectProfile.PawnStructureRegime(_, _, _, passerSquares, _), StrategicMoveTransitionAxis.PawnPasserPush) =>
        Some(if passerSquares.nonEmpty then StrategicDeltaTag.ChainIdentityShift else StrategicDeltaTag.BreakDrivenShift)
      case (_: StrategicObjectProfile.PawnStructureRegime, StrategicMoveTransitionAxis.PawnBreakContact) =>
        Some(StrategicDeltaTag.BreakDrivenShift)
      case (StrategicObjectProfile.KingSafetyShell(condition, accessFiles, _, _), StrategicMoveTransitionAxis.KingEntryPressure) =>
        Some(
          if condition == KingSafetyCondition.Infiltrated || accessFiles.nonEmpty then StrategicDeltaTag.ShellOpened
          else StrategicDeltaTag.ShellIntensified
        )
      case (StrategicObjectProfile.DevelopmentCoordinationState(status, laggingPieces, activeFiles, _), StrategicMoveTransitionAxis.DevelopmentCoordinationShift) =>
        Some(
          status match
            case CoordinationStatus.Leading if activeFiles.nonEmpty   => StrategicDeltaTag.CoordinationImproved
            case CoordinationStatus.Lagging if laggingPieces.nonEmpty => StrategicDeltaTag.CoordinationWorsened
            case CoordinationStatus.Lagging                           => StrategicDeltaTag.LagIncreased
            case _                                                    => StrategicDeltaTag.LagReduced
        )
      case (StrategicObjectProfile.PieceRoleFitness(issue, _, repairTargets), StrategicMoveTransitionAxis.RoleRepairOrTrap) =>
        Some(
          if issue == PieceRoleIssueKind.TrappedPiece then StrategicDeltaTag.RoleDegraded
          else if repairTargets.nonEmpty then StrategicDeltaTag.RoleRepaired
          else StrategicDeltaTag.RoleDegraded
        )
      case (_: StrategicObjectProfile.SpaceClamp, StrategicMoveTransitionAxis.SpaceClampAdvance) =>
        Some(StrategicDeltaTag.SpaceExpanded)
      case (StrategicObjectProfile.CriticalSquareComplex(criticalKinds, focalSquares, pressure), StrategicMoveTransitionAxis.CriticalSquareOccupation) =>
        Some(
          if pressure >= 2 || criticalKinds.contains(CriticalSquareKind.PromotionSquare) || criticalKinds.contains(CriticalSquareKind.BreakContact)
          then StrategicDeltaTag.CriticalReinforced
          else if focalSquares.nonEmpty then StrategicDeltaTag.CriticalCreated
          else StrategicDeltaTag.CriticalLost
        )
      case (StrategicObjectProfile.FixedTargetComplex(_, _, _, fixed, _), StrategicMoveTransitionAxis.FixedTargetFixation) =>
        Option.when(fixed)(StrategicDeltaTag.TargetFixed)
      case (StrategicObjectProfile.BreakAxis(_, _, _, _, supportBalance), StrategicMoveTransitionAxis.BreakActivation) =>
        Some(if supportBalance >= 2 then StrategicDeltaTag.BreakAccelerated else if supportBalance >= 0 then StrategicDeltaTag.BreakOpened else StrategicDeltaTag.BreakClosed)
      case (StrategicObjectProfile.AccessNetwork(_, route, _, contestedSquares), StrategicMoveTransitionAxis.AccessRouteActivation) =>
        Some(if route.nonEmpty then StrategicDeltaTag.AccessOpened else if contestedSquares.nonEmpty then StrategicDeltaTag.AccessImproved else StrategicDeltaTag.AccessDenied)
      case (StrategicObjectProfile.CounterplayAxis(resourceSquares, breakSquares, _), StrategicMoveTransitionAxis.CounterplayResourceShift) =>
        Some(if breakSquares.nonEmpty then StrategicDeltaTag.CounterplayOpened else if resourceSquares.nonEmpty then StrategicDeltaTag.CounterplayLive else StrategicDeltaTag.CounterplayReduced)
      case (_: StrategicObjectProfile.RestrictionShell, StrategicMoveTransitionAxis.RestrictionImposition) =>
        Some(StrategicDeltaTag.RestrictionTightened)
      case (StrategicObjectProfile.MobilityCage(_, deniedSquares, repairTargets), StrategicMoveTransitionAxis.MobilityRestriction) =>
        Some(if deniedSquares.nonEmpty && repairTargets.isEmpty then StrategicDeltaTag.CageCreated else if deniedSquares.size > repairTargets.size then StrategicDeltaTag.CageTightened else StrategicDeltaTag.CageBroken)
      case (StrategicObjectProfile.RedeploymentRoute(route, _, mobilityGain), StrategicMoveTransitionAxis.RedeploymentActivation) =>
        Some(if mobilityGain.exists(_ > 1) then StrategicDeltaTag.RouteShortened else if mobilityGain.nonEmpty || route.via.nonEmpty then StrategicDeltaTag.RouteOpened else StrategicDeltaTag.RouteBlocked)
      case (StrategicObjectProfile.PasserComplex(_, _, relativeRank, protectedByPawn, escortSquares), StrategicMoveTransitionAxis.PasserAdvance) =>
        Some(if relativeRank >= 5 then StrategicDeltaTag.PasserAccelerated else if protectedByPawn || escortSquares.nonEmpty then StrategicDeltaTag.PasserSupported else StrategicDeltaTag.PasserCreated)
      case (StrategicObjectProfile.TradeInvariant(_, _, _, preservedFamilies, features), StrategicMoveTransitionAxis.TradeSimplification) =>
        Option.when(
          features.intersect(
            Set(
              TradeInvariantFeature.FixedTargetAnchor,
              TradeInvariantFeature.BreakAnchor,
              TradeInvariantFeature.AccessAnchor
            )
          ).nonEmpty &&
            preservedFamilies.nonEmpty
        )(StrategicDeltaTag.TradePreserved)
      case _ =>
        None

  private def positionLocalTag(
      obj: StrategicObject
  ): StrategicDeltaTag =
    obj.profile match
      case StrategicObjectProfile.PawnStructureRegime(_, breakFiles, fixedTargets, passerSquares, contactSquares) =>
        if fixedTargets.nonEmpty then StrategicDeltaTag.RegimeFixed
        else if passerSquares.nonEmpty then StrategicDeltaTag.ChainIdentityShift
        else if breakFiles.nonEmpty || contactSquares.nonEmpty then StrategicDeltaTag.BreakDrivenShift
        else StrategicDeltaTag.RegimeReleased
      case StrategicObjectProfile.KingSafetyShell(condition, accessFiles, _, pressureSquares) =>
        if condition == KingSafetyCondition.Infiltrated || pressureSquares.size >= 4 then StrategicDeltaTag.ShellIntensified
        else if accessFiles.nonEmpty then StrategicDeltaTag.ShellOpened
        else StrategicDeltaTag.ShellRelieved
      case StrategicObjectProfile.DevelopmentCoordinationState(status, laggingPieces, _, _) =>
        status match
          case CoordinationStatus.Lagging if laggingPieces.nonEmpty => StrategicDeltaTag.LagIncreased
          case CoordinationStatus.Leading                           => StrategicDeltaTag.CoordinationImproved
          case _ if laggingPieces.nonEmpty                          => StrategicDeltaTag.LagReduced
          case _                                                    => StrategicDeltaTag.CoordinationImproved
      case StrategicObjectProfile.PieceRoleFitness(issue, _, repairTargets) =>
        if issue == PieceRoleIssueKind.TrappedPiece then StrategicDeltaTag.RoleDegraded
        else if repairTargets.nonEmpty then StrategicDeltaTag.RoleRepaired
        else StrategicDeltaTag.RoleDegraded
      case StrategicObjectProfile.SpaceClamp(_, clampSquares, _) =>
        if clampSquares.size >= 3 then StrategicDeltaTag.SpaceExpanded else StrategicDeltaTag.SpaceContracted
      case StrategicObjectProfile.CriticalSquareComplex(_, focalSquares, pressure) =>
        if pressure >= 2 then StrategicDeltaTag.CriticalReinforced
        else if focalSquares.nonEmpty then StrategicDeltaTag.CriticalCreated
        else StrategicDeltaTag.CriticalLost
      case StrategicObjectProfile.FixedTargetComplex(_, _, _, fixed, _) =>
        if fixed then StrategicDeltaTag.TargetFixed
        else if obj.stateStrength.pressureBalance > 0 then StrategicDeltaTag.TargetPressureIntensified
        else StrategicDeltaTag.TargetRelieved
      case StrategicObjectProfile.BreakAxis(_, _, _, _, supportBalance) =>
        if supportBalance > 0 then StrategicDeltaTag.BreakOpened
        else if supportBalance == 0 then StrategicDeltaTag.BreakDelayed
        else StrategicDeltaTag.BreakClosed
      case StrategicObjectProfile.AccessNetwork(_, route, _, contestedSquares) =>
        if contestedSquares.nonEmpty || route.nonEmpty then StrategicDeltaTag.AccessImproved
        else StrategicDeltaTag.AccessOpened
      case StrategicObjectProfile.CounterplayAxis(resourceSquares, breakSquares, _) =>
        if breakSquares.nonEmpty || resourceSquares.nonEmpty then StrategicDeltaTag.CounterplayLive
        else StrategicDeltaTag.CounterplayReduced
      case StrategicObjectProfile.RestrictionShell(restrictedSquares, _, _) =>
        if restrictedSquares.nonEmpty then StrategicDeltaTag.RestrictionTightened
        else StrategicDeltaTag.RestrictionLoosened
      case StrategicObjectProfile.MobilityCage(_, deniedSquares, repairTargets) =>
        if deniedSquares.size > repairTargets.size then StrategicDeltaTag.CageTightened
        else StrategicDeltaTag.CageBroken
      case StrategicObjectProfile.RedeploymentRoute(route, _, mobilityGain) =>
        if mobilityGain.exists(_ > 1) then StrategicDeltaTag.RouteShortened
        else if mobilityGain.exists(_ > 0) || route.via.nonEmpty then StrategicDeltaTag.RouteOpened
        else StrategicDeltaTag.RouteBlocked
      case StrategicObjectProfile.PasserComplex(_, _, relativeRank, protectedByPawn, escortSquares) =>
        if relativeRank >= 5 then StrategicDeltaTag.PasserAccelerated
        else if protectedByPawn || escortSquares.nonEmpty then StrategicDeltaTag.PasserSupported
        else StrategicDeltaTag.PasserCreated
      case _ =>
        StrategicDeltaTag.ComparativeBalance

  private def comparativeTag(
      balance: StrategicComparativeBalance
  ): StrategicDeltaTag =
    balance.standing match
      case ComparativeStanding.Ahead | ComparativeStanding.Behind => StrategicDeltaTag.ComparativeEdge
      case ComparativeStanding.Balanced | ComparativeStanding.Contested => StrategicDeltaTag.ComparativeBalance

  private def deltaContext(
      obj: StrategicObject,
      objects: List[StrategicObject],
      objectsById: Map[String, StrategicObject]
  ): DeltaContext =
    val supportObjects =
      (
        obj.relations.collect {
          case relation if relation.target.owner == obj.owner => objectsById.get(relation.target.objectId)
        }.flatten ++
          objects.filter(other =>
            other.owner == obj.owner &&
              other.relations.exists(relation => relation.target.objectId == obj.id)
          )
      ).distinct.sortBy(_.id)

    DeltaContext(
      supportObjects = supportObjects,
      comparative = comparativeSelection(
        obj,
        supportObjects,
        objects.filter(other => other.id != obj.id)
      )
    )

  private def comparativeSelection(
      obj: StrategicObject,
      supportObjects: List[StrategicObject],
      rivalPool: List[StrategicObject]
  ): Option[ComparativeSelection] =
    val candidates =
      rivalPool
        .flatMap(rival => comparativeWitness(obj, rival).map(ComparativeCandidate(rival, _)))
        .sortBy(candidate => candidateRank(obj, candidate))

    candidates.headOption.flatMap { primary =>
      val grouped =
        candidates
          .filter(candidate =>
            candidate.witness.axis == primary.witness.axis &&
              candidate.witness.counterpartFamily == primary.witness.counterpartFamily
          )
          .take(4)
      val aggregatedWitness =
        StrategicComparativeWitness(
          axis = primary.witness.axis,
          counterpartFamily = primary.witness.counterpartFamily,
          matchedSquares = distinctSquares(grouped.flatMap(_.witness.matchedSquares)),
          matchedFiles = distinctFiles(grouped.flatMap(_.witness.matchedFiles)),
          relationWitnesses = grouped.flatMap(_.witness.relationWitnesses).toSet,
          rivalPrimitiveKinds = grouped.flatMap(_.witness.rivalPrimitiveKinds).toSet,
          counterpartWitnessKinds = grouped.flatMap(_.witness.counterpartWitnessKinds).toSet
        ).normalized
      val rivals = grouped.map(_.rival).distinct.sortBy(_.id)
      val balance = comparativeBalance(obj, supportObjects, rivals)
      val profile = comparativeProfile(obj, primary.rival, aggregatedWitness)
      Option.when(comparativeProfileAdmissible(obj, rivals, aggregatedWitness, profile))(
        ComparativeSelection(
          witness = aggregatedWitness,
          rivals = rivals,
          balance = balance,
          profile = profile
        )
      )
    }

  private def comparativeWitness(
      current: StrategicObject,
      other: StrategicObject
  ): Option[StrategicComparativeWitness] =
    if
      !StrategicObjectFamilyContract.comparativeFamiliesCompatible(current.family, other.family) ||
        !StrategicObjectFamilyContract.comparativeOwnersCompatible(
          current.family,
          other.family,
          sameOwner = current.owner == other.owner
        )
    then None
    else
      val sharedSquares = sharedComparableSquares(current, other)
      val sharedFiles = sharedComparableFiles(current, other)
      val directRivalSquares = directRivalReferenceSquaresBetween(current, other)
      val directRivalFiles = directRivalReferenceFilesBetween(current, other)
      val witness =
        StrategicComparativeWitness(
          axis = comparativeAxisFor(current.profile),
          counterpartFamily = other.family,
          matchedSquares = distinctSquares(sharedSquares ++ directRivalSquares),
          matchedFiles = distinctFiles(sharedFiles ++ directRivalFiles),
          relationWitnesses = relationWitnessesBetween(current, other),
          rivalPrimitiveKinds = matchedRivalPrimitiveKinds(current, other),
          counterpartWitnessKinds =
            counterpartWitnessKinds(
              current,
              other,
              sharedSquares,
              sharedFiles,
              directRivalSquares,
              directRivalFiles
            )
        ).normalized

      Option.when(comparativeAdmissible(current, other, witness))(witness)

  private def comparativeAdmissible(
      current: StrategicObject,
      other: StrategicObject,
      witness: StrategicComparativeWitness
  ): Boolean =
    StrategicObjectFamilyContract.comparativeCounterpartAdmissible(
      current.family,
      other.family,
      witness,
      sameOwner = current.owner == other.owner
    ) &&
      StrategicObjectFamilyContract.comparativeAxisSatisfied(current.family, witness)

  private def candidateRank(
      current: StrategicObject,
      candidate: ComparativeCandidate
  ): (Int, Int, Int, Int, String) =
    (
      if candidate.rival.family == current.family then 0 else 1,
      -candidate.witness.relationWitnesses.size,
      -candidate.witness.matchedSquares.size,
      -candidate.witness.matchedFiles.size,
      candidate.rival.id
    )

  private def comparativeBalance(
      obj: StrategicObject,
      supportObjects: List[StrategicObject],
      rivalObjects: List[StrategicObject]
  ): StrategicComparativeBalance =
    val ownerPressure = obj.stateStrength.pressureBalance + supportObjects.map(_.stateStrength.pressureBalance).sum
    val rivalPressure = rivalObjects.map(_.stateStrength.pressureBalance).sum
    val ownerSupport = obj.stateStrength.supportBalance + supportObjects.map(_.stateStrength.supportBalance).sum
    val rivalSupport = rivalObjects.map(_.stateStrength.supportBalance).sum
    val ownerTotal = ownerPressure + ownerSupport
    val rivalTotal = rivalPressure + rivalSupport
    val standing =
      if ownerTotal > rivalTotal then ComparativeStanding.Ahead
      else if rivalTotal > ownerTotal then ComparativeStanding.Behind
      else if ownerTotal > 0 || rivalTotal > 0 then ComparativeStanding.Contested
      else ComparativeStanding.Balanced
    StrategicComparativeBalance(
      ownerPressure = ownerPressure,
      rivalPressure = rivalPressure,
      ownerSupport = ownerSupport,
      rivalSupport = rivalSupport,
      standing = standing
    )

  private def comparativeProfile(
      obj: StrategicObject,
      rival: StrategicObject,
      witness: StrategicComparativeWitness
  ): StrategicComparativeProfile =
    StrategicComparativeProfile(
      axis = witness.axis,
      counterpartFamily = witness.counterpartFamily,
      metrics = comparativeMetrics(obj.profile, rival.profile)
    ).normalized

  private def comparativeProfileAdmissible(
      current: StrategicObject,
      rivals: List[StrategicObject],
      witness: StrategicComparativeWitness,
      profile: StrategicComparativeProfile
  ): Boolean =
    val readiness = StrategicObjectFamilyContract.forFamily(current.family).defaultReadiness
    val provisionalMetricFloor =
      readiness != StrategicObjectReadiness.Provisional || profile.metrics.size >= 3
    val counterpartGate =
      rivals.nonEmpty &&
        rivals.forall(rival =>
          StrategicObjectFamilyContract.comparativeCounterpartAdmissible(
            current.family,
            rival.family,
            witness,
            sameOwner = current.owner == rival.owner
          )
        )

    profile.metrics.nonEmpty && provisionalMetricFloor && counterpartGate

  private def comparativeMetrics(
      current: StrategicObjectProfile,
      rival: StrategicObjectProfile
  ): List[StrategicComparativeMetric] =
    (current, rival) match
      case (StrategicObjectProfile.PawnStructureRegime(_, breakFiles, fixedTargets, passerSquares, contactSquares), StrategicObjectProfile.PawnStructureRegime(_, rivalBreakFiles, rivalFixedTargets, rivalPasserSquares, rivalContactSquares)) =>
        List(
          StrategicComparativeMetric.BreakPressure(breakFiles.size + contactSquares.size, rivalBreakFiles.size + rivalContactSquares.size),
          StrategicComparativeMetric.FixationCount(fixedTargets.size, rivalFixedTargets.size),
          StrategicComparativeMetric.PasserPotential(passerSquares.size, rivalPasserSquares.size)
        )
      case (StrategicObjectProfile.KingSafetyShell(_, accessFiles, stressedSquares, pressureSquares), StrategicObjectProfile.KingSafetyShell(_, rivalAccessFiles, rivalStressedSquares, rivalPressureSquares)) =>
        List(
          StrategicComparativeMetric.ShellStress(stressedSquares.size + pressureSquares.size, rivalStressedSquares.size + rivalPressureSquares.size),
          StrategicComparativeMetric.EntryAccess(accessFiles.size, rivalAccessFiles.size),
          StrategicComparativeMetric.ShellIntegrity(
            kingShellIntegrityScore(accessFiles, stressedSquares, pressureSquares),
            kingShellIntegrityScore(rivalAccessFiles, rivalStressedSquares, rivalPressureSquares)
          ),
          StrategicComparativeMetric.StressDistribution(
            stressDistributionScore(stressedSquares, pressureSquares),
            stressDistributionScore(rivalStressedSquares, rivalPressureSquares)
          )
        )
      case (StrategicObjectProfile.DevelopmentCoordinationState(_, laggingPieces, activeFiles, _), StrategicObjectProfile.DevelopmentCoordinationState(_, rivalLaggingPieces, rivalActiveFiles, _)) =>
        List(
          StrategicComparativeMetric.LaggingPieceCount(laggingPieces.size, rivalLaggingPieces.size),
          StrategicComparativeMetric.ActiveFileCount(activeFiles.size, rivalActiveFiles.size),
          StrategicComparativeMetric.CoordinationCoherence(
            coordinationCoherenceScore(laggingPieces, activeFiles),
            coordinationCoherenceScore(rivalLaggingPieces, rivalActiveFiles)
          )
        )
      case (StrategicObjectProfile.PieceRoleFitness(issue, _, repairTargets), StrategicObjectProfile.PieceRoleFitness(rivalIssue, _, rivalRepairTargets)) =>
        List(
          StrategicComparativeMetric.LiabilitySeverity(pieceRoleSeverity(issue), pieceRoleSeverity(rivalIssue)),
          StrategicComparativeMetric.RepairSquareCount(repairTargets.size, rivalRepairTargets.size),
          StrategicComparativeMetric.EscapeAvailability(
            distinctFiles(repairTargets.map(_.file)).size,
            distinctFiles(rivalRepairTargets.map(_.file)).size
          )
        )
      case (StrategicObjectProfile.SpaceClamp(_, clampSquares, pressureFiles), StrategicObjectProfile.SpaceClamp(_, rivalClampSquares, rivalPressureFiles)) =>
        List(
          StrategicComparativeMetric.ClampSquareCount(clampSquares.size, rivalClampSquares.size),
          StrategicComparativeMetric.PressureFileCount(pressureFiles.size, rivalPressureFiles.size),
          StrategicComparativeMetric.UsableCounterspace(
            distinctFiles(clampSquares.map(_.file)).size,
            distinctFiles(rivalClampSquares.map(_.file)).size
          )
        )
      case (StrategicObjectProfile.CriticalSquareComplex(_, focalSquares, pressure), StrategicObjectProfile.CriticalSquareComplex(_, rivalFocalSquares, rivalPressure)) =>
        List(
          StrategicComparativeMetric.CriticalControl(focalSquares.size, rivalFocalSquares.size),
          StrategicComparativeMetric.CriticalPressure(pressure, rivalPressure)
        )
      case (StrategicObjectProfile.FixedTargetComplex(_, _, _, _, defended), StrategicObjectProfile.FixedTargetComplex(_, _, _, _, rivalDefended)) =>
        List(
          StrategicComparativeMetric.TargetPressure(if defended then 0 else 1, if rivalDefended then 0 else 1),
          StrategicComparativeMetric.TargetDefense(if defended then 1 else 0, if rivalDefended then 1 else 0)
        )
      case (StrategicObjectProfile.FixedTargetComplex(_, _, _, _, defended), StrategicObjectProfile.RestrictionShell(rivalRestrictedSquares, _, rivalConstraintSquares)) =>
        List(
          StrategicComparativeMetric.TargetPressure(if defended then 0 else 1, rivalConstraintSquares.size),
          StrategicComparativeMetric.TargetDefense(if defended then 1 else 0, rivalRestrictedSquares.size)
        )
      case (StrategicObjectProfile.FixedTargetComplex(_, _, _, _, defended), StrategicObjectProfile.BreakAxis(_, _, rivalTargetSquares, _, rivalSupportBalance)) =>
        List(
          StrategicComparativeMetric.TargetPressure(if defended then 0 else 1, rivalTargetSquares.size),
          StrategicComparativeMetric.TargetDefense(if defended then 1 else 0, math.max(rivalSupportBalance, 0))
        )
      case (StrategicObjectProfile.BreakAxis(_, _, targetSquares, _, supportBalance), StrategicObjectProfile.BreakAxis(_, _, rivalTargetSquares, _, rivalSupportBalance)) =>
        List(
          StrategicComparativeMetric.BreakAvailability(targetSquares.size, rivalTargetSquares.size),
          StrategicComparativeMetric.BreakSupport(supportBalance, rivalSupportBalance)
        )
      case (StrategicObjectProfile.BreakAxis(_, _, targetSquares, _, supportBalance), StrategicObjectProfile.CounterplayAxis(_, rivalBreakSquares, rivalPressureSquares)) =>
        List(
          StrategicComparativeMetric.BreakAvailability(targetSquares.size, rivalBreakSquares.size),
          StrategicComparativeMetric.BreakSupport(supportBalance, rivalPressureSquares.size)
        )
      case (StrategicObjectProfile.AccessNetwork(_, route, _, contestedSquares), StrategicObjectProfile.AccessNetwork(_, rivalRoute, _, rivalContestedSquares)) =>
        List(
          StrategicComparativeMetric.EntryUsability(route.toList.flatMap(_.allSquares).size, rivalRoute.toList.flatMap(_.allSquares).size),
          StrategicComparativeMetric.RouteContest(contestedSquares.size, rivalContestedSquares.size)
        )
      case (StrategicObjectProfile.AccessNetwork(_, route, _, contestedSquares), StrategicObjectProfile.KingSafetyShell(_, rivalAccessFiles, rivalStressedSquares, rivalPressureSquares)) =>
        List(
          StrategicComparativeMetric.EntryUsability(route.toList.flatMap(_.allSquares).size, rivalAccessFiles.size),
          StrategicComparativeMetric.RouteContest(contestedSquares.size, rivalStressedSquares.size + rivalPressureSquares.size)
        )
      case (StrategicObjectProfile.CounterplayAxis(resourceSquares, breakSquares, pressureSquares), StrategicObjectProfile.CounterplayAxis(rivalResourceSquares, rivalBreakSquares, rivalPressureSquares)) =>
        List(
          StrategicComparativeMetric.ReliefResource(resourceSquares.size + pressureSquares.size, rivalResourceSquares.size + rivalPressureSquares.size),
          StrategicComparativeMetric.BreakPressure(breakSquares.size, rivalBreakSquares.size),
          StrategicComparativeMetric.EntryViability(
            distinctFiles((resourceSquares ++ breakSquares).map(_.file)).size,
            distinctFiles((rivalResourceSquares ++ rivalBreakSquares).map(_.file)).size
          )
        )
      case (StrategicObjectProfile.CounterplayAxis(resourceSquares, breakSquares, pressureSquares), StrategicObjectProfile.BreakAxis(_, _, rivalTargetSquares, _, rivalSupportBalance)) =>
        List(
          StrategicComparativeMetric.ReliefResource(resourceSquares.size + pressureSquares.size, rivalTargetSquares.size + math.max(rivalSupportBalance, 0)),
          StrategicComparativeMetric.BreakPressure(breakSquares.size, rivalTargetSquares.size),
          StrategicComparativeMetric.EntryViability(
            distinctFiles((resourceSquares ++ breakSquares).map(_.file)).size,
            distinctFiles(rivalTargetSquares.map(_.file)).size
          )
        )
      case (StrategicObjectProfile.RestrictionShell(restrictedSquares, _, constraintSquares), StrategicObjectProfile.RestrictionShell(rivalRestrictedSquares, _, rivalConstraintSquares)) =>
        List(
          StrategicComparativeMetric.RestrictionCoverage(restrictedSquares.size, rivalRestrictedSquares.size),
          StrategicComparativeMetric.ConstraintCount(constraintSquares.size, rivalConstraintSquares.size),
          StrategicComparativeMetric.ReopenedSquareCount(constraintSquares.size, rivalConstraintSquares.size)
        )
      case (StrategicObjectProfile.RestrictionShell(restrictedSquares, _, constraintSquares), StrategicObjectProfile.FixedTargetComplex(_, _, _, _, rivalDefended)) =>
        List(
          StrategicComparativeMetric.RestrictionCoverage(restrictedSquares.size, if rivalDefended then 1 else 0),
          StrategicComparativeMetric.ConstraintCount(constraintSquares.size, 1),
          StrategicComparativeMetric.ReopenedSquareCount(constraintSquares.size, if rivalDefended then 0 else 1)
        )
      case (StrategicObjectProfile.MobilityCage(_, deniedSquares, repairTargets), StrategicObjectProfile.MobilityCage(_, rivalDeniedSquares, rivalRepairTargets)) =>
        List(
          StrategicComparativeMetric.DeniedSquareCount(deniedSquares.size, rivalDeniedSquares.size),
          StrategicComparativeMetric.RepairCount(repairTargets.size, rivalRepairTargets.size),
          StrategicComparativeMetric.EffectiveMobilityLoss(
            math.max(deniedSquares.size - repairTargets.size, 0),
            math.max(rivalDeniedSquares.size - rivalRepairTargets.size, 0)
          )
        )
      case (StrategicObjectProfile.MobilityCage(_, deniedSquares, repairTargets), StrategicObjectProfile.PieceRoleFitness(rivalIssue, _, rivalRepairTargets)) =>
        List(
          StrategicComparativeMetric.DeniedSquareCount(deniedSquares.size, pieceRoleSeverity(rivalIssue)),
          StrategicComparativeMetric.RepairCount(repairTargets.size, rivalRepairTargets.size),
          StrategicComparativeMetric.EffectiveMobilityLoss(
            math.max(deniedSquares.size - repairTargets.size, 0),
            math.max(pieceRoleSeverity(rivalIssue) - rivalRepairTargets.size, 0)
          )
        )
      case (StrategicObjectProfile.RedeploymentRoute(route, _, mobilityGain), StrategicObjectProfile.RedeploymentRoute(rivalRoute, _, rivalMobilityGain)) =>
        List(
          StrategicComparativeMetric.RouteClarity(route.allSquares.size, rivalRoute.allSquares.size),
          StrategicComparativeMetric.MobilityGain(mobilityGain.getOrElse(0), rivalMobilityGain.getOrElse(0)),
          StrategicComparativeMetric.DestinationQuality(squareQuality(route.target), squareQuality(rivalRoute.target))
        )
      case (StrategicObjectProfile.RedeploymentRoute(route, _, mobilityGain), StrategicObjectProfile.DevelopmentCoordinationState(_, rivalLaggingPieces, rivalActiveFiles, rivalCoordinationSquares)) =>
        List(
          StrategicComparativeMetric.RouteClarity(route.allSquares.size, rivalLaggingPieces.size + rivalCoordinationSquares.size),
          StrategicComparativeMetric.MobilityGain(mobilityGain.getOrElse(0), rivalActiveFiles.size),
          StrategicComparativeMetric.DestinationQuality(squareQuality(route.target), rivalCoordinationSquares.size)
        )
      case (StrategicObjectProfile.PasserComplex(_, _, relativeRank, _, escortSquares), StrategicObjectProfile.PasserComplex(_, _, rivalRelativeRank, _, rivalEscortSquares)) =>
        List(
          StrategicComparativeMetric.PromotionRouteCleanliness(relativeRank, rivalRelativeRank),
          StrategicComparativeMetric.EscortCoverage(escortSquares.size, rivalEscortSquares.size)
        )
      case (StrategicObjectProfile.PasserComplex(_, _, relativeRank, _, escortSquares), StrategicObjectProfile.CriticalSquareComplex(_, rivalFocalSquares, rivalPressure)) =>
        List(
          StrategicComparativeMetric.PromotionRouteCleanliness(relativeRank, rivalPressure),
          StrategicComparativeMetric.EscortCoverage(escortSquares.size, rivalFocalSquares.size)
        )
      case (StrategicObjectProfile.PieceRoleFitness(issue, _, repairTargets), StrategicObjectProfile.MobilityCage(_, rivalDeniedSquares, rivalRepairTargets)) =>
        List(
          StrategicComparativeMetric.LiabilitySeverity(pieceRoleSeverity(issue), rivalDeniedSquares.size),
          StrategicComparativeMetric.RepairSquareCount(repairTargets.size, rivalRepairTargets.size),
          StrategicComparativeMetric.EscapeAvailability(
            distinctFiles(repairTargets.map(_.file)).size,
            math.max(rivalDeniedSquares.size - rivalRepairTargets.size, 0)
          )
        )
      case (StrategicObjectProfile.DevelopmentCoordinationState(_, laggingPieces, activeFiles, _), StrategicObjectProfile.RedeploymentRoute(rivalRoute, _, rivalMobilityGain)) =>
        List(
          StrategicComparativeMetric.LaggingPieceCount(laggingPieces.size, rivalRoute.allSquares.size),
          StrategicComparativeMetric.ActiveFileCount(activeFiles.size, rivalMobilityGain.getOrElse(0)),
          StrategicComparativeMetric.CoordinationCoherence(
            coordinationCoherenceScore(laggingPieces, activeFiles),
            squareQuality(rivalRoute.target)
          )
        )
      case (StrategicObjectProfile.CriticalSquareComplex(_, focalSquares, pressure), StrategicObjectProfile.PasserComplex(_, _, rivalRelativeRank, _, rivalEscortSquares)) =>
        List(
          StrategicComparativeMetric.CriticalControl(focalSquares.size, rivalRelativeRank),
          StrategicComparativeMetric.CriticalPressure(pressure, rivalEscortSquares.size)
        )
      case _ =>
        Nil

  private def samePieceCounterpart(
      current: StrategicObject,
      rival: StrategicObject
  ): Boolean =
    (current.profile, rival.profile) match
      case (StrategicObjectProfile.PieceRoleFitness(_, affectedPiece, _), StrategicObjectProfile.MobilityCage(cagedPiece, _, _)) =>
        affectedPiece.squares.intersect(cagedPiece.squares).nonEmpty
      case (StrategicObjectProfile.MobilityCage(cagedPiece, _, _), StrategicObjectProfile.PieceRoleFitness(_, affectedPiece, _)) =>
        cagedPiece.squares.intersect(affectedPiece.squares).nonEmpty
      case _ =>
        false

  private def sharedRouteCounterpart(
      current: StrategicObject,
      rival: StrategicObject
  ): Boolean =
    (current.profile, rival.profile) match
      case (StrategicObjectProfile.DevelopmentCoordinationState(_, laggingPieces, _, coordinationSquares), StrategicObjectProfile.RedeploymentRoute(route, _, _)) =>
        route.allSquares.intersect(laggingPieces.flatMap(_.squares) ++ coordinationSquares).size >= 2
      case (StrategicObjectProfile.RedeploymentRoute(route, _, _), StrategicObjectProfile.DevelopmentCoordinationState(_, laggingPieces, _, coordinationSquares)) =>
        route.allSquares.intersect(laggingPieces.flatMap(_.squares) ++ coordinationSquares).size >= 2
      case _ =>
        false

  private def sharedTargetCounterpart(
      current: StrategicObject,
      rival: StrategicObject
  ): Boolean =
    (current.profile, rival.profile) match
      case (StrategicObjectProfile.RestrictionShell(restrictedSquares, _, constraintSquares), StrategicObjectProfile.FixedTargetComplex(targetSquare, _, _, _, _)) =>
        (restrictedSquares ++ constraintSquares).contains(targetSquare)
      case (StrategicObjectProfile.FixedTargetComplex(targetSquare, _, _, _, _), StrategicObjectProfile.RestrictionShell(restrictedSquares, _, constraintSquares)) =>
        (restrictedSquares ++ constraintSquares).contains(targetSquare)
      case _ =>
        false

  private def kingShellIntegrityScore(
      accessFiles: Set[File],
      stressedSquares: List[Square],
      pressureSquares: List[Square]
  ): Int =
    math.max(0, 6 - accessFiles.size - distinctFiles((stressedSquares ++ pressureSquares).map(_.file)).size)

  private def stressDistributionScore(
      stressedSquares: List[Square],
      pressureSquares: List[Square]
  ): Int =
    distinctFiles((stressedSquares ++ pressureSquares).map(_.file)).size

  private def coordinationCoherenceScore(
      laggingPieces: List[StrategicPieceRef],
      activeFiles: Set[File]
  ): Int =
    math.max(activeFiles.size - laggingPieces.size, 0)

  private def squareQuality(
      square: Square
  ): Int =
    val fileDistance = math.abs(square.file.char.toInt - 'd'.toInt)
    val rankDistance = math.abs(square.rank.value - 4)
    math.max(0, 8 - fileDistance - rankDistance)

  private def pieceRoleSeverity(
      issue: PieceRoleIssueKind
  ): Int =
    issue match
      case PieceRoleIssueKind.TrappedPiece => 3
      case PieceRoleIssueKind.BadBishop    => 1

  private def moveTransitionWitness(
      move: StrategicPlayedMoveTrace,
      obj: StrategicObject,
      axis: StrategicMoveTransitionAxis,
      context: DeltaContext
  ): Option[StrategicMoveTransitionWitness] =
    val anchoredSquares =
      moveTouchedSquares(
        move,
        moveTransitionWitnessSquares(obj) ++
          obj.evidenceFootprint.anchorSquares ++
          obj.evidenceFootprint.contestedSquares ++
          obj.supportingPrimitives.flatMap(_.allSquares)
      )
    val anchoredFiles =
      moveTouchedFiles(
        move,
        transitionAnchorFiles(obj) ++
          obj.evidenceFootprint.lanes ++
          obj.supportingPrimitives.flatMap(ref => ref.lane.toList ++ ref.allSquares.map(_.file))
      )
    val anchoredPrimitiveKinds =
      obj.supportingPrimitives.collect {
        case primitive if moveTouchesPrimitive(move, primitive) => primitive.kind
      }.toSet
    val coreSquares = moveTouchedSquares(move, moveTransitionCoreSquares(obj))
    val coreFiles = moveTouchedFiles(move, comparableFiles(obj))
    val relatedObjects =
      (context.supportObjects ++ context.rivalObjects)
        .map(other => other.id -> other)
        .toMap
    val relationWitnessTriples =
      obj.relations.collect {
        case relation
            if MoveTransitionRelationOps.contains(relation.operator) &&
              relatedObjects.contains(relation.target.objectId) =>
          val related = relatedObjects(relation.target.objectId)
          val relationSquares = moveTouchedSquares(move, relationComparableSquares(obj, related))
          val relationFiles = moveTouchedFiles(move, relationComparableFiles(obj, related))
          Option.when(
            relationSquares.nonEmpty ||
              (relationFiles.nonEmpty && hasDirectRivalReferenceBetween(obj, related))
          )(
            (relation.operator, relationSquares, relationFiles)
          )
      }.flatten
    val relationWitnesses = relationWitnessTriples.map(_._1).toSet
    val witness =
      StrategicMoveTransitionWitness(
        move = move,
        axis = axis,
        matchedSquares =
          distinctSquares(
            anchoredSquares ++
              relationWitnessTriples.flatMap(_._2)
          ).take(6),
        matchedFiles =
          distinctFiles(
            anchoredFiles ++
              relationWitnessTriples.flatMap(_._3)
          ).take(4),
        relationWitnesses = relationWitnesses,
        primitiveKinds = anchoredPrimitiveKinds
      ).normalized

    Option.when(
      witness.isTransitionAware &&
        moveTransitionCoreSatisfied(obj, coreSquares, coreFiles, anchoredPrimitiveKinds, relationWitnesses)
    )(witness)

  private def moveTrace(
      contract: DecisiveTruthContract,
      truth: MoveTruthFrame
  ): Option[StrategicPlayedMoveTrace] =
    truth.playedMove
      .orElse(contract.playedMove)
      .flatMap(parseMove)

  private def parseMove(
      raw: String
  ): Option[StrategicPlayedMoveTrace] =
    Option(raw).map(_.trim).filter(_.length >= 4).flatMap { move =>
      for
        from <- Square.fromKey(move.slice(0, 2))
        to <- Square.fromKey(move.slice(2, 4))
      yield StrategicPlayedMoveTrace(from = from, to = to, promotion = None)
    }

  private def hasTransitionTruth(
      contract: DecisiveTruthContract,
      truth: MoveTruthFrame
  ): Boolean =
    moveTrace(contract, truth).nonEmpty &&
      (
        truth.strategicOwnership.currentMoveEvidence ||
          truth.strategicOwnership.currentConcreteCarrier ||
          truth.strategicOwnership.currentSemanticAnchorMatch ||
          truth.surfacedMoveOwnsTruth ||
          truth.verifiedBestMove.nonEmpty ||
          contract.surfacedMoveOwnsTruth ||
          contract.verifiedBestMove.nonEmpty
      )

  private def changedAnchors(
      obj: StrategicObject,
      scope: StrategicDeltaScope,
      projection: StrategicDeltaProjection
  ): List[StrategicObjectAnchor] =
    val roleFilter =
      scope match
        case StrategicDeltaScope.MoveLocal =>
          Set(
            StrategicAnchorRole.Primary,
            StrategicAnchorRole.Entry,
            StrategicAnchorRole.Exit,
            StrategicAnchorRole.Pressure,
            StrategicAnchorRole.Constraint
          )
        case StrategicDeltaScope.PositionLocal =>
          Set(
            StrategicAnchorRole.Primary,
            StrategicAnchorRole.Support,
            StrategicAnchorRole.Pressure,
            StrategicAnchorRole.Constraint
          )
        case StrategicDeltaScope.Comparative =>
          Set(
            StrategicAnchorRole.Primary,
            StrategicAnchorRole.Entry,
            StrategicAnchorRole.Exit,
            StrategicAnchorRole.Pressure,
            StrategicAnchorRole.Constraint
          )

    val focusSquares =
      projection match
        case StrategicDeltaProjection.MoveLocal(_, witness)         => witness.matchedSquares
        case StrategicDeltaProjection.PositionLocal(_, _)           => distinctSquares(obj.evidenceFootprint.anchorSquares ++ obj.evidenceFootprint.contestedSquares ++ obj.locus.allSquares)
        case StrategicDeltaProjection.Comparative(_, _, witness, _, _) => witness.matchedSquares

    val focusFiles =
      projection match
        case StrategicDeltaProjection.MoveLocal(_, witness)         => witness.matchedFiles
        case StrategicDeltaProjection.PositionLocal(_, _)           => objectFiles(obj)
        case StrategicDeltaProjection.Comparative(_, _, witness, _, _) => witness.matchedFiles

    val filtered =
      obj.anchors
        .map(_.normalized)
        .filter(anchor => roleFilter.contains(anchor.role))
        .filter(anchor => anchorMatches(anchor, focusSquares.toSet, focusFiles.toSet))

    if filtered.nonEmpty then filtered.distinct.take(4)
    else obj.anchors.map(_.normalized).distinct.take(4)

  private def anchorMatches(
      anchor: StrategicObjectAnchor,
      focusSquares: Set[Square],
      focusFiles: Set[File]
  ): Boolean =
    focusSquares.isEmpty && focusFiles.isEmpty ||
      anchor.squares.exists(focusSquares.contains) ||
      anchor.route.exists(_.allSquares.exists(focusSquares.contains)) ||
      anchor.file.exists(focusFiles.contains) ||
      anchor.piece.exists(_.squares.exists(focusSquares.contains))

  private def evidenceRefs(
      obj: StrategicObject
  ): List[StrategicDeltaEvidenceRef] =
    obj.supportingPrimitives
      .map { ref =>
        StrategicDeltaEvidenceRef(
          primitiveKind = ref.kind,
          anchorSquares = ref.anchorSquares,
          contestedSquares = ref.contestedSquares,
          lane = ref.lane
        ).normalized
      }
      .distinct
      .sortBy(ref => s"${ref.primitiveKind}-${ref.anchorSquares.map(_.key).mkString("-")}-${ref.lane.map(_.char).getOrElse('-')}")
      .take(4)

  private def comparativeAxisFor(
      profile: StrategicObjectProfile
  ): StrategicComparativeAxis =
    profile match
      case StrategicObjectProfile.PawnStructureRegime(_, _, fixedTargets, passerSquares, _) =>
        if fixedTargets.nonEmpty then StrategicComparativeAxis.PawnFixationContrast
        else if passerSquares.nonEmpty then StrategicComparativeAxis.PawnPasserPotentialContrast
        else StrategicComparativeAxis.PawnBreakPressureContrast
      case StrategicObjectProfile.KingSafetyShell(condition, accessFiles, _, _) =>
        if condition == KingSafetyCondition.Infiltrated || accessFiles.nonEmpty then StrategicComparativeAxis.KingEntryPressureContrast
        else StrategicComparativeAxis.KingShellIntegrityContrast
      case _: StrategicObjectProfile.DevelopmentCoordinationState =>
        StrategicComparativeAxis.DevelopmentLeadContrast
      case _: StrategicObjectProfile.PieceRoleFitness =>
        StrategicComparativeAxis.PieceRoleLiabilityContrast
      case _: StrategicObjectProfile.SpaceClamp =>
        StrategicComparativeAxis.SpaceClampCoverageContrast
      case _: StrategicObjectProfile.CriticalSquareComplex =>
        StrategicComparativeAxis.CriticalSquareControlContrast
      case StrategicObjectProfile.FixedTargetComplex(_, _, _, _, defended) =>
        if defended then StrategicComparativeAxis.FixedTargetDefenseContrast
        else StrategicComparativeAxis.FixedTargetPressureContrast
      case StrategicObjectProfile.BreakAxis(_, _, _, _, supportBalance) =>
        if supportBalance > 0 then StrategicComparativeAxis.BreakSupportRace
        else StrategicComparativeAxis.BreakAvailabilityContrast
      case StrategicObjectProfile.AccessNetwork(_, _, _, contestedSquares) =>
        if contestedSquares.nonEmpty then StrategicComparativeAxis.AccessRouteContestContrast
        else StrategicComparativeAxis.AccessEntryUsabilityContrast
      case StrategicObjectProfile.CounterplayAxis(_, breakSquares, _) =>
        if breakSquares.nonEmpty then StrategicComparativeAxis.CounterplayBreakPressureContrast
        else StrategicComparativeAxis.CounterplayReliefContrast
      case _: StrategicObjectProfile.RestrictionShell =>
        StrategicComparativeAxis.RestrictionContainmentContrast
      case _: StrategicObjectProfile.MobilityCage =>
        StrategicComparativeAxis.MobilityRestrictionContrast
      case StrategicObjectProfile.RedeploymentRoute(_, _, mobilityGain) =>
        if mobilityGain.exists(_ > 1) then StrategicComparativeAxis.RedeploymentTempoContrast
        else StrategicComparativeAxis.RedeploymentRouteClarityContrast
      case StrategicObjectProfile.PasserComplex(_, _, _, protectedByPawn, escortSquares) =>
        if protectedByPawn || escortSquares.nonEmpty then StrategicComparativeAxis.PasserEscortContrast
        else StrategicComparativeAxis.PasserPromotionRouteContrast
      case _ =>
        StrategicComparativeAxis.BreakAvailabilityContrast

  private def relationWitnessesBetween(
      current: StrategicObject,
      other: StrategicObject
  ): Set[StrategicRelationOperator] =
    (
      current.relations.collect {
        case relation if relation.target.objectId == other.id => relation.operator
      } ++
        other.relations.collect {
          case relation if relation.target.objectId == current.id => relation.operator
        }
    ).toSet

  private def rivalReferenceSquaresBetween(
      current: StrategicObject,
      other: StrategicObject
  ): List[Square] =
    current.rivalResourcesOrObjects.collect {
      case rival if rivalMatchesObject(rival, other) =>
        rival.squares
    }.flatten

  private def rivalReferenceFilesBetween(
      current: StrategicObject,
      other: StrategicObject
  ): List[File] =
    current.rivalResourcesOrObjects.collect {
      case rival if rivalMatchesObject(rival, other) =>
        rival.file
    }.flatten

  private def matchedRivalPrimitiveKinds(
      current: StrategicObject,
      other: StrategicObject
  ): Set[PrimitiveKind] =
    current.evidenceFootprint.primitiveKinds
      .intersect(other.evidenceFootprint.primitiveKinds)
      .union(directRivalReferencePrimitiveKindsBetween(current, other))

  private def comparableSquares(
      obj: StrategicObject
  ): List[Square] =
    obj.profile match
      case StrategicObjectProfile.PawnStructureRegime(_, _, fixedTargets, passerSquares, contactSquares) =>
        distinctSquares(fixedTargets ++ passerSquares ++ contactSquares)
      case StrategicObjectProfile.KingSafetyShell(_, _, stressedSquares, pressureSquares) =>
        distinctSquares(stressedSquares ++ pressureSquares)
      case StrategicObjectProfile.DevelopmentCoordinationState(_, laggingPieces, _, coordinationSquares) =>
        distinctSquares(laggingPieces.flatMap(_.squares) ++ coordinationSquares)
      case StrategicObjectProfile.PieceRoleFitness(_, affectedPiece, repairTargets) =>
        distinctSquares(affectedPiece.squares ++ repairTargets)
      case StrategicObjectProfile.SpaceClamp(_, clampSquares, _) =>
        distinctSquares(clampSquares)
      case StrategicObjectProfile.CriticalSquareComplex(_, focalSquares, _) =>
        distinctSquares(focalSquares)
      case StrategicObjectProfile.FixedTargetComplex(targetSquare, _, _, _, _) =>
        List(targetSquare)
      case StrategicObjectProfile.BreakAxis(sourceSquare, breakSquare, targetSquares, _, _) =>
        distinctSquares(sourceSquare :: breakSquare :: targetSquares)
      case StrategicObjectProfile.AccessNetwork(_, route, _, contestedSquares) =>
        distinctSquares(route.toList.flatMap(_.allSquares) ++ contestedSquares)
      case StrategicObjectProfile.CounterplayAxis(resourceSquares, breakSquares, pressureSquares) =>
        distinctSquares(resourceSquares ++ breakSquares ++ pressureSquares)
      case StrategicObjectProfile.RestrictionShell(restrictedSquares, contestedSquares, constraintSquares) =>
        distinctSquares(restrictedSquares ++ contestedSquares ++ constraintSquares)
      case StrategicObjectProfile.MobilityCage(affectedPiece, deniedSquares, repairTargets) =>
        distinctSquares(affectedPiece.squares ++ deniedSquares ++ repairTargets)
      case StrategicObjectProfile.RedeploymentRoute(route, _, _) =>
        distinctSquares(route.allSquares)
      case StrategicObjectProfile.TradeInvariant(exchangeSquares, invariantSquares, _, _, _) =>
        distinctSquares(exchangeSquares ++ invariantSquares)
      case StrategicObjectProfile.PasserComplex(passerSquare, promotionSquare, _, _, escortSquares) =>
        distinctSquares(passerSquare :: promotionSquare :: escortSquares)
      case _ =>
        objectSquares(obj)

  private def comparableFiles(
      obj: StrategicObject
  ): List[File] =
    obj.profile match
      case StrategicObjectProfile.PawnStructureRegime(_, breakFiles, fixedTargets, passerSquares, contactSquares) =>
        distinctFiles(breakFiles.toList ++ (fixedTargets ++ passerSquares ++ contactSquares).map(_.file))
      case StrategicObjectProfile.KingSafetyShell(_, accessFiles, stressedSquares, pressureSquares) =>
        distinctFiles(accessFiles.toList ++ (stressedSquares ++ pressureSquares).map(_.file))
      case StrategicObjectProfile.DevelopmentCoordinationState(_, laggingPieces, activeFiles, coordinationSquares) =>
        distinctFiles(activeFiles.toList ++ laggingPieces.flatMap(_.squares.map(_.file)) ++ coordinationSquares.map(_.file))
      case StrategicObjectProfile.PieceRoleFitness(_, affectedPiece, repairTargets) =>
        distinctFiles(affectedPiece.squares.map(_.file) ++ repairTargets.map(_.file))
      case StrategicObjectProfile.SpaceClamp(_, clampSquares, pressureFiles) =>
        distinctFiles(pressureFiles.toList ++ clampSquares.map(_.file))
      case StrategicObjectProfile.CriticalSquareComplex(_, focalSquares, _) =>
        distinctFiles(focalSquares.map(_.file))
      case StrategicObjectProfile.FixedTargetComplex(targetSquare, _, _, _, _) =>
        List(targetSquare.file)
      case StrategicObjectProfile.BreakAxis(sourceSquare, breakSquare, targetSquares, _, _) =>
        distinctFiles(List(sourceSquare.file, breakSquare.file) ++ targetSquares.map(_.file))
      case StrategicObjectProfile.AccessNetwork(lane, route, _, contestedSquares) =>
        distinctFiles(lane.toList ++ route.toList.flatMap(_.allSquares.map(_.file)) ++ contestedSquares.map(_.file))
      case StrategicObjectProfile.CounterplayAxis(resourceSquares, breakSquares, pressureSquares) =>
        distinctFiles((resourceSquares ++ breakSquares ++ pressureSquares).map(_.file))
      case StrategicObjectProfile.RestrictionShell(restrictedSquares, contestedSquares, constraintSquares) =>
        distinctFiles((restrictedSquares ++ contestedSquares ++ constraintSquares).map(_.file))
      case StrategicObjectProfile.MobilityCage(affectedPiece, deniedSquares, repairTargets) =>
        distinctFiles(affectedPiece.squares.map(_.file) ++ deniedSquares.map(_.file) ++ repairTargets.map(_.file))
      case StrategicObjectProfile.RedeploymentRoute(route, _, _) =>
        distinctFiles(route.allSquares.map(_.file))
      case StrategicObjectProfile.TradeInvariant(exchangeSquares, invariantSquares, preservedFiles, _, _) =>
        distinctFiles(preservedFiles.toList ++ (exchangeSquares ++ invariantSquares).map(_.file))
      case StrategicObjectProfile.PasserComplex(passerSquare, promotionSquare, _, _, escortSquares) =>
        distinctFiles((passerSquare :: promotionSquare :: escortSquares).map(_.file))
      case _ =>
        objectFiles(obj)

  private def sharedComparableSquares(
      current: StrategicObject,
      other: StrategicObject
  ): List[Square] =
    distinctSquares(
      comparableSquares(current).intersect(comparableSquares(other)) ++
        transitionAnchorSquares(current).intersect(transitionAnchorSquares(other))
    )

  private def sharedComparableFiles(
      current: StrategicObject,
      other: StrategicObject
  ): List[File] =
    distinctFiles(
      comparableFiles(current).intersect(comparableFiles(other)) ++
        transitionAnchorFiles(current).intersect(transitionAnchorFiles(other))
    )

  private def counterpartWitnessKinds(
      current: StrategicObject,
      other: StrategicObject,
      sharedSquares: List[Square],
      sharedFiles: List[File],
      directRivalSquares: List[Square],
      directRivalFiles: List[File]
  ): Set[StrategicCounterpartWitnessKind] =
    List(
      Option.when(sharedSquares.nonEmpty)(StrategicCounterpartWitnessKind.SharedSquare),
      Option.when(sharedFiles.nonEmpty)(StrategicCounterpartWitnessKind.SharedFile),
      Option.when(sharedRouteCounterpart(current, other))(StrategicCounterpartWitnessKind.SharedRoute),
      Option.when(sharedTargetCounterpart(current, other))(StrategicCounterpartWitnessKind.SharedTarget),
      Option.when(samePieceCounterpart(current, other))(StrategicCounterpartWitnessKind.SharedPiece),
      Option.when(directRivalSquares.nonEmpty || directRivalFiles.nonEmpty)(
        StrategicCounterpartWitnessKind.DirectRivalReference
      )
    ).flatten.toSet

  private def transitionAnchorSquares(
      obj: StrategicObject
  ): List[Square] =
    distinctSquares(
      obj.anchors.flatMap(_.squares) ++
        obj.anchors.flatMap(_.route.toList.flatMap(_.allSquares)) ++
        obj.anchors.flatMap(_.piece.toList.flatMap(_.squares))
    )

  private def transitionAnchorFiles(
      obj: StrategicObject
  ): List[File] =
    distinctFiles(
      obj.anchors.flatMap(_.file) ++
        obj.anchors.flatMap(_.squares.map(_.file)) ++
        obj.anchors.flatMap(_.route.toList.flatMap(_.allSquares.map(_.file))) ++
        obj.anchors.flatMap(_.piece.toList.flatMap(_.squares.map(_.file)))
    )

  private def moveTouchedSquares(
      move: StrategicPlayedMoveTrace,
      squares: List[Square]
  ): List[Square] =
    distinctSquares(move.touchedSquares.intersect(squares))

  private def moveTouchedFiles(
      move: StrategicPlayedMoveTrace,
      files: List[File]
  ): List[File] =
    distinctFiles(move.touchedFiles.intersect(files))

  private def moveTransitionWitnessSquares(
      obj: StrategicObject
  ): List[Square] =
    distinctSquares(
      transitionAnchorSquares(obj) ++ moveTransitionCoreSquares(obj)
    )

  private def moveTransitionCoreSquares(
      obj: StrategicObject
  ): List[Square] =
    obj.profile match
      case StrategicObjectProfile.FixedTargetComplex(targetSquare, targetOwner, _, fixed, _) =>
        Option.when(fixed)(fixationWitnessSquare(targetSquare, targetOwner)).toList.flatten
      case StrategicObjectProfile.TradeInvariant(exchangeSquares, _, _, _, _) =>
        distinctSquares(exchangeSquares)
      case _ =>
        comparableSquares(obj)

  private def fixationWitnessSquare(
      targetSquare: Square,
      targetOwner: Color
  ): Option[Square] =
    val forwardDelta = if targetOwner.white then 1 else -1
    Square.at(targetSquare.file.value, targetSquare.rank.value + forwardDelta)

  private def moveTouchesPrimitive(
      move: StrategicPlayedMoveTrace,
      primitive: PrimitiveReference
  ): Boolean =
    moveTouchedSquares(move, primitive.allSquares).nonEmpty ||
      moveTouchedFiles(move, primitive.lane.toList ++ primitive.allSquares.map(_.file)).nonEmpty

  private def moveTransitionCoreSatisfied(
      obj: StrategicObject,
      coreSquares: List[Square],
      coreFiles: List[File],
      primitiveKinds: Set[PrimitiveKind],
      relationWitnesses: Set[StrategicRelationOperator]
  ): Boolean =
    obj.profile match
      case _: StrategicObjectProfile.KingSafetyShell | _: StrategicObjectProfile.AccessNetwork =>
        coreSquares.nonEmpty ||
          (coreFiles.nonEmpty && primitiveKinds.nonEmpty) ||
          relationWitnesses.nonEmpty
      case _: StrategicObjectProfile.PawnStructureRegime =>
        (coreSquares.nonEmpty && primitiveKinds.nonEmpty) || relationWitnesses.nonEmpty
      case _: StrategicObjectProfile.DevelopmentCoordinationState =>
        coreSquares.nonEmpty &&
          primitiveKinds.intersect(Set(PrimitiveKind.PieceRoleIssue, PrimitiveKind.AccessRoute, PrimitiveKind.RedeploymentPathSeed)).nonEmpty &&
          (coreSquares.size >= 2 || relationWitnesses.nonEmpty)
      case _: StrategicObjectProfile.PieceRoleFitness =>
        coreSquares.nonEmpty &&
          primitiveKinds.intersect(Set(PrimitiveKind.PieceRoleIssue, PrimitiveKind.RedeploymentPathSeed, PrimitiveKind.KnightRouteSeed, PrimitiveKind.DiagonalLaneSeed)).nonEmpty
      case _: StrategicObjectProfile.SpaceClamp =>
        coreSquares.size >= 2 &&
          primitiveKinds.intersect(Set(PrimitiveKind.CriticalSquare, PrimitiveKind.TargetSquare, PrimitiveKind.BreakCandidate, PrimitiveKind.RouteContestSeed)).nonEmpty
      case _: StrategicObjectProfile.CounterplayAxis =>
        coreSquares.nonEmpty &&
          primitiveKinds.contains(PrimitiveKind.CounterplayResourceSeed) &&
          (
            primitiveKinds.contains(PrimitiveKind.BreakCandidate) ||
              primitiveKinds.contains(PrimitiveKind.ReleaseCandidate) ||
              relationWitnesses.nonEmpty
          )
      case _: StrategicObjectProfile.RestrictionShell =>
        coreSquares.size >= 2 &&
          primitiveKinds.intersect(Set(PrimitiveKind.TargetSquare, PrimitiveKind.CriticalSquare, PrimitiveKind.RouteContestSeed)).nonEmpty &&
          (coreFiles.nonEmpty || relationWitnesses.nonEmpty)
      case StrategicObjectProfile.FixedTargetComplex(_, _, _, fixed, _) =>
        fixed &&
          coreSquares.nonEmpty &&
          primitiveKinds.contains(PrimitiveKind.TargetSquare)
      case _: StrategicObjectProfile.MobilityCage =>
        coreSquares.nonEmpty &&
          primitiveKinds.contains(PrimitiveKind.PieceRoleIssue) &&
          (
            primitiveKinds.intersect(Set(PrimitiveKind.TargetSquare, PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.RouteContestSeed)).nonEmpty ||
              relationWitnesses.nonEmpty
          )
      case _: StrategicObjectProfile.RedeploymentRoute =>
        coreSquares.size >= 2 &&
          primitiveKinds.intersect(Set(PrimitiveKind.DiagonalLaneSeed, PrimitiveKind.LiftCorridorSeed, PrimitiveKind.KnightRouteSeed, PrimitiveKind.RedeploymentPathSeed)).nonEmpty
      case StrategicObjectProfile.TradeInvariant(exchangeSquares, _, _, preservedFamilies, features) =>
        coreSquares.intersect(exchangeSquares).nonEmpty &&
          primitiveKinds.contains(PrimitiveKind.ExchangeSquare) &&
          preservedFamilies.nonEmpty &&
          !features.contains(TradeInvariantFeature.PasserAnchor) &&
          features.intersect(
            Set(
              TradeInvariantFeature.FixedTargetAnchor,
              TradeInvariantFeature.BreakAnchor,
              TradeInvariantFeature.AccessAnchor
            )
          ).nonEmpty
      case _ =>
        coreSquares.nonEmpty || relationWitnesses.nonEmpty

  private def hasDirectRivalReferenceBetween(
      current: StrategicObject,
      other: StrategicObject
  ): Boolean =
    directRivalReferenceSquaresBetween(current, other).nonEmpty ||
      directRivalReferenceFilesBetween(current, other).nonEmpty ||
      directRivalReferencePrimitiveKindsBetween(current, other).nonEmpty

  private def directRivalReferenceSquaresBetween(
      current: StrategicObject,
      other: StrategicObject
  ): List[Square] =
    distinctSquares(
      rivalReferenceSquaresBetween(current, other) ++
        rivalReferenceSquaresBetween(other, current)
    )

  private def directRivalReferenceFilesBetween(
      current: StrategicObject,
      other: StrategicObject
  ): List[File] =
    distinctFiles(
      rivalReferenceFilesBetween(current, other) ++
        rivalReferenceFilesBetween(other, current)
    )

  private def directRivalReferencePrimitiveKindsBetween(
      current: StrategicObject,
      other: StrategicObject
  ): Set[PrimitiveKind] =
    (
      current.rivalResourcesOrObjects.collect {
        case rival if rivalMatchesObject(rival, other) => rival.primitiveKind
      }.flatten ++
        other.rivalResourcesOrObjects.collect {
          case rival if rivalMatchesObject(rival, current) => rival.primitiveKind
        }.flatten
    ).toSet

  private def rivalMatchesObject(
      rival: StrategicRivalReference,
      other: StrategicObject
  ): Boolean =
    rival.objectId.contains(other.id) ||
      (
        rival.objectFamily.contains(other.family) &&
          (
            rival.squares.exists(square => objectSquares(other).contains(square)) ||
              rival.file.exists(file => objectFiles(other).contains(file))
          )
      )

  private def relationComparableSquares(
      current: StrategicObject,
      other: StrategicObject
  ): List[Square] =
    distinctSquares(
      comparableSquares(current).intersect(comparableSquares(other)) ++
        transitionAnchorSquares(current).intersect(transitionAnchorSquares(other)) ++
        directRivalReferenceSquaresBetween(current, other)
    )

  private def relationComparableFiles(
      current: StrategicObject,
      other: StrategicObject
  ): List[File] =
    distinctFiles(
      comparableFiles(current).intersect(comparableFiles(other)) ++
        transitionAnchorFiles(current).intersect(transitionAnchorFiles(other)) ++
        directRivalReferenceFilesBetween(current, other)
    )

  private def objectSquares(
      obj: StrategicObject
  ): List[Square] =
    distinctSquares(
      obj.locus.allSquares ++
        obj.anchors.flatMap(_.squares) ++
        obj.anchors.flatMap(_.route.toList.flatMap(_.allSquares)) ++
        obj.anchors.flatMap(_.piece.toList.flatMap(_.squares)) ++
        obj.supportingPrimitives.flatMap(_.allSquares) ++
        obj.supportingPieces.flatMap(_.squares) ++
        obj.evidenceFootprint.anchorSquares ++
        obj.evidenceFootprint.contestedSquares
    )

  private def objectFiles(
      obj: StrategicObject
  ): List[File] =
    distinctFiles(
      obj.locus.files ++
        obj.anchors.flatMap(_.file) ++
        obj.anchors.flatMap(anchor => anchor.squares.map(_.file)) ++
        obj.anchors.flatMap(_.route.toList.flatMap(_.allSquares.map(_.file))) ++
        obj.supportingPrimitives.flatMap(ref => ref.lane.toList ++ ref.allSquares.map(_.file)) ++
        obj.supportingPieces.flatMap(_.squares.map(_.file)) ++
        obj.evidenceFootprint.lanes ++
        obj.evidenceFootprint.anchorSquares.map(_.file) ++
        obj.evidenceFootprint.contestedSquares.map(_.file)
    )

  private def distinctSquares(
      squares: List[Square]
  ): List[Square] =
    squares.distinct.sortBy(_.key)

  private def distinctFiles(
      files: List[File]
  ): List[File] =
    files.distinct.sortBy(_.char.toString)

  private def colorIndex(
      color: Color
  ): Int =
    if color.white then 0 else 1
