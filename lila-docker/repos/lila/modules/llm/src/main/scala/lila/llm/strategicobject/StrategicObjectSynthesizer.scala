package lila.llm.strategicobject

import chess.{ Color, File, Knight, Pawn, Role, Rook, Square }
import lila.llm.analysis.MoveTruthFrame

trait StrategicObjectSynthesizer:
  def synthesize(
      primitives: PrimitiveBank,
      truth: MoveTruthFrame
  ): List[StrategicObject]

object CanonicalStrategicObjectSynthesizer extends StrategicObjectSynthesizer:

  private val Owners = List(Color.White, Color.Black)

  def synthesize(
      primitives: PrimitiveBank,
      truth: MoveTruthFrame
  ): List[StrategicObject] =
    val bank = primitives.normalized
    val refs = bank.all.map(PrimitiveReference.fromPrimitive).map(_.normalized)
    val boardDirect =
      boardDirectObjects(bank, refs)
    val graphDerived =
      graphDerivedObjects(refs, truth, boardDirect)
    val objects = boardDirect ++ graphDerived

    attachRelations(objects).map(_.normalized).sortBy(o => (o.family.ordinal, colorIndex(o.owner), o.id))

  private def boardDirectObjects(
      bank: PrimitiveBank,
      refs: List[PrimitiveReference]
  ): List[StrategicObject] =
      pawnStructureObjects(bank, refs) ++
        kingSafetyObjects(bank, refs) ++
        developmentObjects(bank, refs) ++
        pieceRoleObjects(bank, refs) ++
        spaceClampObjects(bank, refs) ++
        criticalSquareObjects(bank, refs) ++
        fixedTargetObjects(bank, refs) ++
        breakAxisObjects(bank, refs) ++
        accessNetworkObjects(bank, refs) ++
        counterplayAxisObjects(bank, refs) ++
        restrictionShellObjects(bank, refs) ++
        mobilityCageObjects(bank, refs) ++
        redeploymentRouteObjects(bank, refs) ++
        passerObjects(bank, refs)

  private def graphDerivedObjects(
      refs: List[PrimitiveReference],
      truth: MoveTruthFrame,
      boardDirect: List[StrategicObject]
  ): List[StrategicObject] =
    val stage0 = attachRelations(boardDirect).map(_.normalized)
    val foundation =
      defenderDependencyObjects(refs, stage0) ++
        tradeInvariantObjects(refs, stage0) ++
        tensionStateObjects(refs, stage0) ++
        attackScaffoldObjects(refs, stage0)
    val stage1 = attachRelations(stage0 ++ foundation).map(_.normalized)
    val operational =
      materialInvestmentContractObjects(refs, truth, stage1) ++
        initiativeWindowObjects(refs, truth, stage1) ++
        conversionFunnelObjects(refs, stage1) ++
        fortressHoldingShellObjects(refs, stage1)
    val stage2 = attachRelations(stage1 ++ operational).map(_.normalized)
    val cross =
      planRaceObjects(refs, stage2) ++
        transitionBridgeObjects(refs, stage2)

    (foundation ++ operational ++ cross).groupBy(_.id).values.map(_.head).toList

  private def pawnStructureObjects(
      bank: PrimitiveBank,
      refs: List[PrimitiveReference]
  ): List[StrategicObject] =
    Owners.flatMap { owner =>
      val targets = bank.targetSquares.filter(p => p.owner == owner && p.occupant.contains(Pawn))
      val breaks = bank.breakCandidates.filter(_.owner == owner)
      val levers = bank.leverContactSeeds.filter(_.owner == owner)
      val hooks = bank.hookContactSeeds.filter(_.owner == owner)
      val tensions = bank.tensionContactSeeds.filter(_.owner == owner)
      val passers = bank.passerSeeds.filter(_.owner == owner)
      Option.when(targets.nonEmpty || breaks.nonEmpty || levers.nonEmpty || hooks.nonEmpty || tensions.nonEmpty || passers.nonEmpty) {
        val squares =
          distinctSquares(
            targets.map(_.square) ++
              breaks.flatMap(p => p.sourceSquare :: p.breakSquare :: p.targetSquares) ++
              levers.flatMap(p => List(p.from, p.target)) ++
              hooks.flatMap(p => List(p.from, p.createSquare, p.target)) ++
              tensions.flatMap(p => List(p.from, p.target)) ++
              passers.map(_.square)
          )
        val files = distinctFiles(breaks.map(_.file) ++ squares.map(_.file))
        val support = breaks.map(p => p.supportCount - p.resistanceCount).sum + levers.map(p => p.supportCount - p.resistanceCount).sum
        val pressure = targets.map(p => p.attackerCount - p.defenderCount).sum
        val primitivesForObject = refsFrom(targets ++ breaks ++ levers ++ hooks ++ tensions ++ passers)
        val pieces =
          pawnPieces(owner, breaks.map(_.sourceSquare) ++ levers.map(_.from) ++ hooks.map(_.from) ++ tensions.map(_.from) ++ passers.map(_.square))
        mkObject(
          id = objectId(StrategicObjectFamily.PawnStructureRegime, owner, Some(dominantSector(squares, files)), squares.headOption, files.toSet),
          family = StrategicObjectFamily.PawnStructureRegime,
          owner = owner,
          locus = StrategicObjectLocus(squares = squares, files = files),
          sector = Some(dominantSector(squares, files)),
          anchors = primarySquareAnchors(squares) ++ fileAnchors(files, StrategicAnchorRole.Secondary) ++ pieceAnchors(pieces, StrategicAnchorRole.Support),
          profile =
            StrategicObjectProfile.PawnStructureRegime(
              identity =
                Set.newBuilder[PawnStructureFeature]
                  .addAll(Option.when(targets.exists(_.fixed))(PawnStructureFeature.FixedTargets))
                  .addAll(Option.when(breaks.nonEmpty)(PawnStructureFeature.BreakPressure))
                  .addAll(Option.when(levers.nonEmpty)(PawnStructureFeature.LeverContact))
                  .addAll(Option.when(hooks.nonEmpty)(PawnStructureFeature.HookContact))
                  .addAll(Option.when(tensions.exists(_.maintainable))(PawnStructureFeature.MaintainedTension))
                  .addAll(Option.when(passers.nonEmpty)(PawnStructureFeature.PassedPawn))
                  .addAll(Option.when(squares.exists(isCentral))(PawnStructureFeature.CentralPresence))
                  .addAll(Option.when(squares.exists(square => !isCentral(square)))(PawnStructureFeature.FlankPresence))
                  .result(),
              breakFiles = breaks.map(_.file).toSet,
              fixedTargets = targets.filter(_.fixed).map(_.square),
              passerSquares = passers.map(_.square),
              contactSquares = distinctSquares(levers.map(_.target) ++ hooks.map(_.target) ++ tensions.map(_.target))
            ),
          supportingPrimitives = primitivesForObject,
          supportingPieces = mergePieces(pieces, primitivesForObject),
          rivals = rivalsFrom(refs, !owner, squares.toSet, files.toSet, Set(PrimitiveKind.BreakCandidate, PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.ReleaseCandidate, PrimitiveKind.PasserSeed)),
          horizonClass = ObjectHorizonClass.Structural,
          supportBalance = support,
          pressureBalance = pressure,
          mobilityGain = 0,
          tags =
            Set(StrategicEvidenceTag.Supported) ++
              Option.when(targets.exists(_.fixed))(StrategicEvidenceTag.Fixed) ++
              Option.when(passers.exists(_.protectedByPawn))(StrategicEvidenceTag.Protected) ++
              Option.when(squares.exists(isCentral))(StrategicEvidenceTag.Central) ++
              Option.when(squares.exists(square => !isCentral(square)))(StrategicEvidenceTag.Flank)
        )
      }
    }

  private def kingSafetyObjects(
      bank: PrimitiveBank,
      refs: List[PrimitiveReference]
  ): List[StrategicObject] =
    Owners.flatMap { owner =>
      val attacker = !owner
      val accessRoutes = bank.accessRoutes.filter(_.owner == attacker)
      val routeSeeds = bank.routeContestSeeds.filter(_.owner == attacker)
      val diagonalSeeds = bank.diagonalLaneSeeds.filter(_.owner == attacker)
      val liftSeeds = bank.liftCorridorSeeds.filter(_.owner == attacker)
      val knightSeeds = bank.knightRouteSeeds.filter(_.owner == attacker)
      val redeploySeeds = bank.redeploymentPathSeeds.filter(_.owner == attacker)
      val hooks = bank.hookContactSeeds.filter(_.owner == attacker)
      val targets = bank.targetSquares.filter(p => p.owner == attacker && p.targetOwner == owner)
      val pressureSquares =
        distinctSquares(
          routeSeeds.map(_.square) ++
            diagonalSeeds.map(_.target) ++
            liftSeeds.map(_.target) ++
            knightSeeds.map(_.target) ++
            redeploySeeds.map(_.target) ++
            hooks.flatMap(p => List(p.createSquare, p.target)) ++
            targets.map(_.square)
        )
      val directEntryWitnessCount =
        accessRoutes.size + diagonalSeeds.size + liftSeeds.size + knightSeeds.size + redeploySeeds.size + hooks.size + targets.size
      val kingSafetyPrimitiveKinds =
        Set.newBuilder[PrimitiveKind]
          .addAll(Option.when(accessRoutes.nonEmpty)(PrimitiveKind.AccessRoute))
          .addAll(Option.when(diagonalSeeds.nonEmpty)(PrimitiveKind.DiagonalLaneSeed))
          .addAll(Option.when(liftSeeds.nonEmpty)(PrimitiveKind.LiftCorridorSeed))
          .addAll(Option.when(knightSeeds.nonEmpty)(PrimitiveKind.KnightRouteSeed))
          .addAll(Option.when(redeploySeeds.nonEmpty)(PrimitiveKind.RedeploymentPathSeed))
          .addAll(Option.when(hooks.nonEmpty)(PrimitiveKind.HookContactSeed))
          .addAll(Option.when(targets.nonEmpty)(PrimitiveKind.TargetSquare))
          .result()
      Option.when(
        contractAllows(
          StrategicObjectFamily.KingSafetyShell,
          FamilyGenerationEvidence(
            primitiveKinds = kingSafetyPrimitiveKinds,
            anchorSquares = pressureSquares.toSet,
            contestedSquares = pressureSquares.toSet,
            files = pressureSquares.map(_.file).toSet,
            metrics =
              FamilyGenerationMetrics(
                pressureSquareCount = pressureSquares.size,
                entryWitnessCount = directEntryWitnessCount,
                targetWitnessCount = pressureSquares.size,
                contestedOverlapCount = pressureSquares.size
              )
          )
        )
      ) {
        val files = distinctFiles(accessRoutes.map(_.file) ++ routeSeeds.map(_.lane) ++ pressureSquares.map(_.file))
        val sector = dominantSector(pressureSquares, files)
        val routes = diagonalSeeds.map(routeGeometry) ++ liftSeeds.map(routeGeometry) ++ knightSeeds.map(routeGeometry) ++ redeploySeeds.map(routeGeometry)
        val condition =
          if hooks.nonEmpty || routes.nonEmpty then KingSafetyCondition.Infiltrated
          else if accessRoutes.nonEmpty then KingSafetyCondition.Fractured
          else KingSafetyCondition.Pressured
        val primitivesForObject =
          selectRefs(refs, Some(attacker), pressureSquares.toSet, files.toSet, Set(PrimitiveKind.AccessRoute, PrimitiveKind.RouteContestSeed, PrimitiveKind.DiagonalLaneSeed, PrimitiveKind.LiftCorridorSeed, PrimitiveKind.KnightRouteSeed, PrimitiveKind.RedeploymentPathSeed, PrimitiveKind.HookContactSeed, PrimitiveKind.TargetSquare))
        val pieces =
          pieceGroups(
            pieceGroup(attacker, accessRoutes.flatMap(_.carrierSquares), accessRoutes.flatMap(_.roles).toSet),
            pieceGroup(attacker, diagonalSeeds.map(_.origin), diagonalSeeds.map(_.role).toSet),
            pieceGroup(attacker, liftSeeds.map(_.origin), Set(Rook)),
            pieceGroup(attacker, knightSeeds.map(_.origin), Set(Knight)),
            pieceGroup(attacker, redeploySeeds.map(_.origin), redeploySeeds.map(_.role).toSet),
            pieceGroup(attacker, hooks.map(_.from), Set(Pawn))
          )
        mkObject(
          id = objectId(StrategicObjectFamily.KingSafetyShell, owner, Some(sector), pressureSquares.headOption, files.toSet),
          family = StrategicObjectFamily.KingSafetyShell,
          owner = owner,
          locus = StrategicObjectLocus(squares = pressureSquares.take(4), files = files),
          sector = Some(sector),
          anchors = pressureSquareAnchors(pressureSquares) ++ fileAnchors(files, StrategicAnchorRole.Pressure) ++ routeAnchors(routes, StrategicAnchorRole.Entry) ++ pieceAnchors(pieces, StrategicAnchorRole.Support),
          profile =
            StrategicObjectProfile.KingSafetyShell(
              condition = condition,
              accessFiles = accessRoutes.map(_.file).toSet,
              stressedSquares = pressureSquares,
              pressureSquares = pressureSquares
            ),
          supportingPrimitives = primitivesForObject,
          supportingPieces = mergePieces(pieces, primitivesForObject),
          rivals = rivalsFrom(refs, owner, pressureSquares.toSet, files.toSet, Set(PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.RouteContestSeed, PrimitiveKind.AccessRoute, PrimitiveKind.DefendedResource)),
          horizonClass = ObjectHorizonClass.Operational,
          supportBalance = accessRoutes.size + routeSeeds.size + hooks.size,
          pressureBalance = pressureSquares.size,
          mobilityGain = 0,
          tags = Set(StrategicEvidenceTag.ShellPressure, StrategicEvidenceTag.Exposed) ++ Option.when(accessRoutes.nonEmpty)(StrategicEvidenceTag.OpenFile)
        )
      }
    }

  private def developmentObjects(
      bank: PrimitiveBank,
      refs: List[PrimitiveReference]
  ): List[StrategicObject] =
    Owners.flatMap { owner =>
      val issues = bank.pieceRoleIssues.filter(_.owner == owner)
      val accessRoutes = bank.accessRoutes.filter(_.owner == owner)
      val routeSeeds = bank.routeContestSeeds.filter(_.owner == owner)
      val diagonalSeeds = bank.diagonalLaneSeeds.filter(_.owner == owner)
      val liftSeeds = bank.liftCorridorSeeds.filter(_.owner == owner)
      val knightSeeds = bank.knightRouteSeeds.filter(_.owner == owner)
      val redeploySeeds = bank.redeploymentPathSeeds.filter(_.owner == owner)
      val squares = distinctSquares(routeSeeds.map(_.square) ++ diagonalSeeds.map(_.target) ++ liftSeeds.map(_.target) ++ knightSeeds.map(_.target) ++ redeploySeeds.map(_.target) ++ issues.map(_.square))
      Option.when(squares.nonEmpty || accessRoutes.nonEmpty) {
        val files = distinctFiles(accessRoutes.map(_.file) ++ routeSeeds.map(_.lane))
        val active = accessRoutes.size + routeSeeds.size + diagonalSeeds.size + liftSeeds.size + knightSeeds.size
        val lag = issues.size + redeploySeeds.size
        val status = if lag > active then CoordinationStatus.Lagging else if active > lag then CoordinationStatus.Leading else CoordinationStatus.Balanced
        val pieces =
          pieceGroups(
            pieceGroup(owner, accessRoutes.flatMap(_.carrierSquares), accessRoutes.flatMap(_.roles).toSet),
            pieceGroup(owner, diagonalSeeds.map(_.origin), diagonalSeeds.map(_.role).toSet),
            pieceGroup(owner, liftSeeds.map(_.origin), Set(Rook)),
            pieceGroup(owner, knightSeeds.map(_.origin), Set(Knight)),
            pieceGroup(owner, redeploySeeds.map(_.origin), redeploySeeds.map(_.role).toSet)
          ) ++ issues.map(issue => pieceRef(issue.owner, issue.square, issue.role))
        val primitivesForObject = refsFrom(issues ++ accessRoutes ++ routeSeeds ++ diagonalSeeds ++ liftSeeds ++ knightSeeds ++ redeploySeeds)
        mkObject(
          id = objectId(StrategicObjectFamily.DevelopmentCoordinationState, owner, Some(dominantSector(squares, files)), squares.headOption, files.toSet),
          family = StrategicObjectFamily.DevelopmentCoordinationState,
          owner = owner,
          locus = StrategicObjectLocus(squares = squares, files = files),
          sector = Some(dominantSector(squares, files)),
          anchors = primarySquareAnchors(squares) ++ fileAnchors(files, StrategicAnchorRole.Secondary) ++ pieceAnchors(pieces, StrategicAnchorRole.Support),
          profile =
            StrategicObjectProfile.DevelopmentCoordinationState(
              status = status,
              laggingPieces = issues.map(issue => pieceRef(issue.owner, issue.square, issue.role)),
              activeFiles = accessRoutes.map(_.file).toSet,
              coordinationSquares = squares
            ),
          supportingPrimitives = primitivesForObject,
          supportingPieces = mergePieces(pieces, primitivesForObject),
          rivals = rivalsFrom(refs, !owner, squares.toSet, files.toSet, Set(PrimitiveKind.AccessRoute, PrimitiveKind.RouteContestSeed, PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.TargetSquare)),
          horizonClass = ObjectHorizonClass.Operational,
          supportBalance = active,
          pressureBalance = active - lag,
          mobilityGain = redeploySeeds.map(_.mobilityGain).sum,
          tags = Set(if status == CoordinationStatus.Lagging then StrategicEvidenceTag.DevelopmentLag else StrategicEvidenceTag.DevelopmentLead)
        )
      }
    }

  private def pieceRoleObjects(
      bank: PrimitiveBank,
      refs: List[PrimitiveReference]
  ): List[StrategicObject] =
    bank.pieceRoleIssues.map { issue =>
      val repairRoutes =
        bank.redeploymentPathSeeds.filter(seed => seed.owner == issue.owner && seed.origin == issue.square) ++
          bank.diagonalLaneSeeds.filter(seed => seed.owner == issue.owner && seed.origin == issue.square) ++
          bank.knightRouteSeeds.filter(seed => seed.owner == issue.owner && seed.origin == issue.square)
      val repairTargets =
        distinctSquares(
          repairRoutes.collect {
            case seed: RedeploymentPathSeed => seed.target
            case seed: DiagonalLaneSeed     => seed.target
            case seed: KnightRouteSeed      => seed.target
          }
        )
      val primitivesForObject = refsFrom(issue :: repairRoutes)
      mkObject(
        id = objectId(StrategicObjectFamily.PieceRoleFitness, issue.owner, sec(issue.square), Some(issue.square), Set(issue.square.file)),
        family = StrategicObjectFamily.PieceRoleFitness,
        owner = issue.owner,
        locus = StrategicObjectLocus(squares = issue.square :: repairTargets),
        sector = sec(issue.square),
        anchors = pieceAnchors(List(pieceRef(issue.owner, issue.square, issue.role)), StrategicAnchorRole.Primary) ++ exitSquareAnchors(repairTargets),
        profile =
          StrategicObjectProfile.PieceRoleFitness(
            issue = issue.issue,
            affectedPiece = pieceRef(issue.owner, issue.square, issue.role),
            repairTargets = repairTargets
          ),
        supportingPrimitives = primitivesForObject,
        supportingPieces = mergePieces(List(pieceRef(issue.owner, issue.square, issue.role)), primitivesForObject),
        rivals = rivalsFrom(refs, !issue.owner, (repairTargets :+ issue.square).toSet, Set(issue.square.file), Set(PrimitiveKind.TargetSquare, PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.RouteContestSeed)),
        horizonClass = if issue.issue == PieceRoleIssueKind.BadBishop then ObjectHorizonClass.Structural else ObjectHorizonClass.Maneuver,
        supportBalance = repairTargets.size,
        pressureBalance = repairTargets.size,
        mobilityGain = repairRoutes.collect { case seed: RedeploymentPathSeed => seed.mobilityGain }.sum,
        tags = Option.when(issue.issue == PieceRoleIssueKind.TrappedPiece)(StrategicEvidenceTag.Trapped).toSet
      )
    }

  private def spaceClampObjects(
      bank: PrimitiveBank,
      refs: List[PrimitiveReference]
  ): List[StrategicObject] =
    Owners.flatMap { owner =>
      val clampSquares =
        distinctSquares(
          bank.criticalSquares.collect { case square if square.owner == owner && square.kind == CriticalSquareKind.Outpost => square.square } ++
            bank.routeContestSeeds.filter(_.owner == owner).map(_.square) ++
            bank.targetSquares.filter(_.owner == owner).map(_.square) ++
            bank.breakCandidates.filter(_.owner == owner).flatMap(_.targetSquares)
        )
      clampSquares.groupBy(sectorOfSquare).toList.flatMap { case (sector, squares) =>
        Option.when(squares.size >= 2) {
          val files = distinctFiles(squares.map(_.file))
          mkObject(
            id = objectId(StrategicObjectFamily.SpaceClamp, owner, Some(sector), squares.headOption, files.toSet),
            family = StrategicObjectFamily.SpaceClamp,
            owner = owner,
            locus = StrategicObjectLocus(squares = squares, files = files),
            sector = Some(sector),
            anchors = constraintSquareAnchors(squares) ++ fileAnchors(files, StrategicAnchorRole.Constraint),
            profile =
              StrategicObjectProfile.SpaceClamp(
                mode =
                  sector match
                    case ObjectSector.Queenside => SpaceClampMode.QueensideClamp
                    case ObjectSector.Center    => if files.size >= 3 then SpaceClampMode.BroadClamp else SpaceClampMode.CentralClamp
                    case ObjectSector.Kingside  => SpaceClampMode.KingsideClamp
                    case _                      => SpaceClampMode.BroadClamp,
                clampSquares = squares,
                pressureFiles = files.toSet
              ),
            supportingPrimitives = selectRefs(refs, Some(owner), squares.toSet, files.toSet, Set(PrimitiveKind.CriticalSquare, PrimitiveKind.RouteContestSeed, PrimitiveKind.TargetSquare, PrimitiveKind.BreakCandidate)),
            supportingPieces = Nil,
            rivals = rivalsFrom(refs, !owner, squares.toSet, files.toSet, Set(PrimitiveKind.BreakCandidate, PrimitiveKind.ReleaseCandidate, PrimitiveKind.CounterplayResourceSeed)),
            horizonClass = ObjectHorizonClass.Structural,
            supportBalance = files.size,
            pressureBalance = squares.size,
            mobilityGain = 0,
            tags = Set(if sector == ObjectSector.Center then StrategicEvidenceTag.Central else StrategicEvidenceTag.Flank)
          )
        }
      }
    }

  private def criticalSquareObjects(
      bank: PrimitiveBank,
      refs: List[PrimitiveReference]
  ): List[StrategicObject] =
          bank.criticalSquares.groupBy(square => (square.owner, sectorOfSquare(square.square))).toList.map { case ((owner, sector), group) =>
      val squares = distinctSquares(group.map(_.square))
      val files = distinctFiles(squares.map(_.file))
      mkObject(
        id = objectId(StrategicObjectFamily.CriticalSquareComplex, owner, Some(sector), squares.headOption, files.toSet),
        family = StrategicObjectFamily.CriticalSquareComplex,
        owner = owner,
        locus = StrategicObjectLocus(squares = squares, files = files),
        sector = Some(sector),
        anchors = primarySquareAnchors(squares),
        profile =
          StrategicObjectProfile.CriticalSquareComplex(
            criticalKinds = group.map(_.kind).toSet,
            focalSquares = squares,
            pressure = group.map(_.pressure).sum
          ),
        supportingPrimitives = selectRefs(refs, Some(owner), squares.toSet, files.toSet, Set(PrimitiveKind.CriticalSquare, PrimitiveKind.RouteContestSeed, PrimitiveKind.BreakCandidate, PrimitiveKind.PasserSeed)),
        supportingPieces = Nil,
        rivals = rivalsFrom(refs, !owner, squares.toSet, files.toSet, Set(PrimitiveKind.BreakCandidate, PrimitiveKind.TargetSquare, PrimitiveKind.CounterplayResourceSeed)),
        horizonClass = if group.exists(_.kind == CriticalSquareKind.PromotionSquare) then ObjectHorizonClass.Promotion else ObjectHorizonClass.Structural,
        supportBalance = group.map(_.pressure).sum,
        pressureBalance = group.map(_.pressure).sum,
        mobilityGain = 0,
        tags =
          Option.when(group.exists(_.kind == CriticalSquareKind.PromotionSquare))(StrategicEvidenceTag.Promotion).toSet ++
            Option.when(group.exists(_.kind == CriticalSquareKind.Outpost))(StrategicEvidenceTag.RouteAccess)
      )
    }

  private def fixedTargetObjects(
      bank: PrimitiveBank,
      refs: List[PrimitiveReference]
  ): List[StrategicObject] =
    bank.targetSquares.map { target =>
      val squares = target.square :: bank.breakCandidates.filter(axis => axis.owner == target.owner && (axis.breakSquare == target.square || axis.targetSquares.contains(target.square))).flatMap(_.targetSquares)
      mkObject(
        id = objectId(StrategicObjectFamily.FixedTargetComplex, target.owner, sec(target.square), Some(target.square), Set(target.square.file)),
        family = StrategicObjectFamily.FixedTargetComplex,
        owner = target.owner,
        locus = StrategicObjectLocus(squares = distinctSquares(squares), files = List(target.square.file)),
        sector = sec(target.square),
        anchors = primarySquareAnchors(List(target.square)) ++ fileAnchors(List(target.square.file), StrategicAnchorRole.Constraint),
        profile =
          StrategicObjectProfile.FixedTargetComplex(
            targetSquare = target.square,
            targetOwner = target.targetOwner,
            occupantRoles = target.occupant.toSet,
            fixed = target.fixed,
            defended = bank.defendedResources.exists(resource => resource.owner == target.targetOwner && resource.square == target.square)
          ),
        supportingPrimitives = selectRefs(refs, None, Set(target.square), Set(target.square.file), Set(PrimitiveKind.TargetSquare, PrimitiveKind.DefendedResource, PrimitiveKind.BreakCandidate, PrimitiveKind.RouteContestSeed, PrimitiveKind.AccessRoute, PrimitiveKind.CounterplayResourceSeed)),
        supportingPieces = Nil,
        rivals = rivalsFrom(refs, target.targetOwner, Set(target.square), Set(target.square.file), Set(PrimitiveKind.DefendedResource, PrimitiveKind.ReleaseCandidate, PrimitiveKind.BreakCandidate, PrimitiveKind.CounterplayResourceSeed)),
        horizonClass = if target.fixed then ObjectHorizonClass.Structural else ObjectHorizonClass.Operational,
        supportBalance = target.attackerCount,
        pressureBalance = target.attackerCount - target.defenderCount,
        mobilityGain = 0,
        tags = Option.when(target.fixed)(StrategicEvidenceTag.Fixed).toSet
      )
    }

  private def breakAxisObjects(
      bank: PrimitiveBank,
      refs: List[PrimitiveReference]
  ): List[StrategicObject] =
    bank.breakCandidates.map { axis =>
      mkObject(
        id = objectId(StrategicObjectFamily.BreakAxis, axis.owner, sec(axis.breakSquare), Some(axis.breakSquare), Set(axis.file)),
        family = StrategicObjectFamily.BreakAxis,
        owner = axis.owner,
        locus = StrategicObjectLocus(route = Some(routeGeometry(axis)), squares = axis.targetSquares, files = List(axis.file)),
        sector = sec(axis.breakSquare),
        anchors = routeAnchors(List(routeGeometry(axis)), StrategicAnchorRole.Primary) ++ exitSquareAnchors(axis.targetSquares),
        profile =
          StrategicObjectProfile.BreakAxis(
            sourceSquare = axis.sourceSquare,
            breakSquare = axis.breakSquare,
            targetSquares = axis.targetSquares,
            mode = axis.mode,
            supportBalance = axis.supportCount - axis.resistanceCount
          ),
        supportingPrimitives = selectRefs(refs, Some(axis.owner), (axis.sourceSquare :: axis.breakSquare :: axis.targetSquares).toSet, Set(axis.file), Set(PrimitiveKind.BreakCandidate, PrimitiveKind.ReleaseCandidate, PrimitiveKind.CriticalSquare, PrimitiveKind.TargetSquare)),
        supportingPieces = pawnPieces(axis.owner, List(axis.sourceSquare)),
        rivals = rivalsFrom(refs, !axis.owner, (axis.breakSquare :: axis.targetSquares).toSet, Set(axis.file), Set(PrimitiveKind.DefendedResource, PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.RouteContestSeed)),
        horizonClass = ObjectHorizonClass.Operational,
        supportBalance = axis.supportCount,
        pressureBalance = axis.supportCount - axis.resistanceCount,
        mobilityGain = 0,
        tags = Set(StrategicEvidenceTag.Contested)
      )
    }

  private def accessNetworkObjects(
      bank: PrimitiveBank,
      refs: List[PrimitiveReference]
  ): List[StrategicObject] =
    fileAccessObjects(bank, refs) ++
      bank.diagonalLaneSeeds.map(seed => routeAccessObject(seed.owner, routeGeometry(seed), Set(seed.role), refs, "diag")) ++
      bank.liftCorridorSeeds.map(seed => routeAccessObject(seed.owner, routeGeometry(seed), Set(Rook), refs, "lift")) ++
      bank.knightRouteSeeds.map(seed => routeAccessObject(seed.owner, routeGeometry(seed), Set(Knight), refs, "knight")) ++
      bank.redeploymentPathSeeds.map(seed => routeAccessObject(seed.owner, routeGeometry(seed), Set(seed.role), refs, "redeploy"))

  private def fileAccessObjects(
      bank: PrimitiveBank,
      refs: List[PrimitiveReference]
  ): List[StrategicObject] =
    bank.accessRoutes.map { route =>
      val contested = bank.routeContestSeeds.filter(seed => seed.owner == route.owner && seed.lane == route.file).map(_.square)
      val pieces = pieceGroups(pieceGroup(route.owner, route.carrierSquares, route.roles))
      mkObject(
        id = objectId(StrategicObjectFamily.AccessNetwork, route.owner, fileSec(route.file), route.carrierSquares.headOption.orElse(contested.headOption), Set(route.file)),
        family = StrategicObjectFamily.AccessNetwork,
        owner = route.owner,
        locus = StrategicObjectLocus(squares = route.carrierSquares ++ contested, files = List(route.file)),
        sector = fileSec(route.file),
        anchors = fileAnchors(List(route.file), StrategicAnchorRole.Entry) ++ supportSquareAnchors(route.carrierSquares) ++ exitSquareAnchors(contested) ++ pieceAnchors(pieces, StrategicAnchorRole.Support),
        profile =
          StrategicObjectProfile.AccessNetwork(
            lane = Some(route.file),
            route = None,
            roles = route.roles,
            contestedSquares = contested
          ),
        supportingPrimitives = refsFrom(route :: bank.routeContestSeeds.filter(seed => seed.owner == route.owner && seed.lane == route.file)),
        supportingPieces = pieces,
        rivals = rivalsFrom(refs, !route.owner, contested.toSet, Set(route.file), Set(PrimitiveKind.AccessRoute, PrimitiveKind.RouteContestSeed, PrimitiveKind.TargetSquare)),
        horizonClass = ObjectHorizonClass.Maneuver,
        supportBalance = route.carrierSquares.size,
        pressureBalance = contested.size,
        mobilityGain = 0,
        tags = Set(StrategicEvidenceTag.OpenFile, StrategicEvidenceTag.RouteAccess)
      )
    }

  private def routeAccessObject(
      owner: Color,
      route: StrategicRouteGeometry,
      roles: Set[Role],
      refs: List[PrimitiveReference],
      extra: String
  ): StrategicObject =
    mkObject(
      id = objectId(StrategicObjectFamily.AccessNetwork, owner, sec(route.target), Some(route.target), Set(route.target.file), extra = extra),
      family = StrategicObjectFamily.AccessNetwork,
      owner = owner,
      locus = StrategicObjectLocus(route = Some(route), squares = route.allSquares, files = List(route.target.file)),
      sector = sec(route.target),
      anchors = routeAnchors(List(route), StrategicAnchorRole.Entry) ++ exitSquareAnchors(List(route.target)),
      profile =
        StrategicObjectProfile.AccessNetwork(
          lane = None,
          route = Some(route),
          roles = roles,
          contestedSquares = List(route.target)
        ),
      supportingPrimitives = selectRefs(refs, Some(owner), route.allSquares.toSet, Set(route.target.file), Set(PrimitiveKind.DiagonalLaneSeed, PrimitiveKind.LiftCorridorSeed, PrimitiveKind.KnightRouteSeed, PrimitiveKind.RedeploymentPathSeed, PrimitiveKind.RouteContestSeed, PrimitiveKind.CriticalSquare)),
      supportingPieces = pieceGroups(pieceGroup(owner, List(route.origin), roles)),
      rivals = rivalsFrom(refs, !owner, (route.via :+ route.target).toSet, Set(route.target.file), Set(PrimitiveKind.RouteContestSeed, PrimitiveKind.TargetSquare, PrimitiveKind.CriticalSquare, PrimitiveKind.CounterplayResourceSeed)),
      horizonClass = ObjectHorizonClass.Maneuver,
      supportBalance = route.via.size + 1,
      pressureBalance = 1,
      mobilityGain = 0,
      tags = Set(StrategicEvidenceTag.RouteAccess)
    )

  private def counterplayAxisObjects(
      bank: PrimitiveBank,
      refs: List[PrimitiveReference]
  ): List[StrategicObject] =
    Owners.flatMap { owner =>
      val resourceSeeds = bank.counterplayResourceSeeds.filter(_.owner == owner)
      Option.when(resourceSeeds.nonEmpty) {
        val resourceSquares =
          distinctSquares(resourceSeeds.map(_.square) ++ resourceSeeds.flatMap(_.pressureSquares))
        val resourceFiles = resourceSquares.map(_.file).toSet
        val relatedBreaks =
          bank.breakCandidates.filter { axis =>
            axis.owner == owner && (
              resourceFiles.contains(axis.file) ||
                resourceSquares.contains(axis.breakSquare) ||
                axis.targetSquares.exists(resourceSquares.contains)
            )
          }
        val relatedReleases =
          bank.releaseCandidates.filter { release =>
            release.owner == owner && (
              resourceSquares.contains(release.from) ||
                resourceSquares.contains(release.target) ||
                resourceFiles.contains(release.from.file) ||
                resourceFiles.contains(release.target.file)
            )
          }
        val squares =
          distinctSquares(
            resourceSquares ++
              relatedBreaks.map(_.breakSquare) ++
              relatedBreaks.flatMap(_.targetSquares) ++
              relatedReleases.flatMap(release => List(release.from, release.target))
          )
        squares.groupBy(sectorOfSquare).toList.flatMap { case (sector, focus) =>
          val focusSeeds =
            resourceSeeds.filter(seed => focus.contains(seed.square) || seed.pressureSquares.exists(focus.contains))
          val continuationWitnessCount =
            relatedBreaks.count(axis =>
              focus.contains(axis.breakSquare) || axis.targetSquares.exists(focus.contains)
            ) +
              relatedReleases.count(release => focus.contains(release.from) || focus.contains(release.target)) +
              focusSeeds.count(_.pressureSquares.exists(focus.contains))
          Option.when(
            contractAllows(
              StrategicObjectFamily.CounterplayAxis,
              FamilyGenerationEvidence(
                primitiveKinds =
                  Set.newBuilder[PrimitiveKind]
                    .addOne(PrimitiveKind.CounterplayResourceSeed)
                    .addAll(Option.when(relatedBreaks.nonEmpty)(PrimitiveKind.BreakCandidate))
                    .addAll(Option.when(relatedReleases.nonEmpty)(PrimitiveKind.ReleaseCandidate))
                    .result(),
                anchorSquares = focus.toSet,
                contestedSquares = focus.toSet,
                files = focus.map(_.file).toSet,
                pieceRoles = focusSeeds.map(_.role).toSet,
                metrics =
                  FamilyGenerationMetrics(
                    pressureSquareCount = focus.size,
                    entryWitnessCount = continuationWitnessCount,
                    targetWitnessCount = focus.size,
                    contestedOverlapCount = focus.size
                  )
              )
            )
          ) {
            val files = distinctFiles(focus.map(_.file))
            mkObject(
              id = objectId(StrategicObjectFamily.CounterplayAxis, owner, Some(sector), focus.headOption, files.toSet),
              family = StrategicObjectFamily.CounterplayAxis,
              owner = owner,
              locus = StrategicObjectLocus(squares = focus, files = files),
              sector = Some(sector),
              anchors = primarySquareAnchors(focus) ++ fileAnchors(files, StrategicAnchorRole.Entry),
              profile =
                StrategicObjectProfile.CounterplayAxis(
                  resourceSquares = resourceSeeds.map(_.square).filter(focus.contains),
                  breakSquares = relatedBreaks.map(_.breakSquare).filter(focus.contains),
                  pressureSquares = focus
                ),
              supportingPrimitives =
                selectRefs(
                  refs,
                  Some(owner),
                  focus.toSet,
                  files.toSet,
                  Set(
                    PrimitiveKind.CounterplayResourceSeed,
                    PrimitiveKind.BreakCandidate,
                    PrimitiveKind.RouteContestSeed,
                    PrimitiveKind.ReleaseCandidate,
                    PrimitiveKind.HookContactSeed
                  )
                ),
              supportingPieces =
                focusSeeds.map(seed => pieceRef(seed.owner, seed.square, seed.role)),
              rivals = rivalsFrom(refs, !owner, focus.toSet, files.toSet, Set(PrimitiveKind.TargetSquare, PrimitiveKind.CriticalSquare, PrimitiveKind.AccessRoute, PrimitiveKind.RouteContestSeed)),
              horizonClass = ObjectHorizonClass.Operational,
              supportBalance = resourceSeeds.size,
              pressureBalance = focus.size,
              mobilityGain = 0,
              tags = Set(StrategicEvidenceTag.Contested)
            )
          }
        }
      }.getOrElse(Nil)
    }

  private def restrictionShellObjects(
      bank: PrimitiveBank,
      refs: List[PrimitiveReference]
  ): List[StrategicObject] =
    Owners.flatMap { owner =>
      val restrictionSquares =
        distinctSquares(
          bank.targetSquares.filter(_.owner == owner).map(_.square) ++
            bank.criticalSquares.filter(_.owner == owner).map(_.square) ++
            bank.routeContestSeeds.filter(_.owner == owner).map(_.square)
        )
      restrictionSquares.groupBy(sectorOfSquare).toList.flatMap { case (sector, focus) =>
        val enemy =
          distinctSquares(
            bank.counterplayResourceSeeds.filter(_.owner == !owner).map(_.square) ++
              bank.counterplayResourceSeeds.filter(_.owner == !owner).flatMap(_.pressureSquares) ++
              bank.breakCandidates.filter(_.owner == !owner).map(_.breakSquare)
          ).filter(square => sectorOfSquare(square) == sector)
        val sectorTargets =
          bank.targetSquares.filter(p => p.owner == owner && sectorOfSquare(p.square) == sector)
        val sectorCritical =
          bank.criticalSquares.filter(p => p.owner == owner && sectorOfSquare(p.square) == sector)
        val restrictionPrimitiveKinds =
          Set.newBuilder[PrimitiveKind]
            .addAll(Option.when(sectorTargets.nonEmpty)(PrimitiveKind.TargetSquare))
            .addAll(Option.when(sectorCritical.nonEmpty)(PrimitiveKind.CriticalSquare))
            .addAll(Option.when(bank.routeContestSeeds.exists(seed => seed.owner == owner && sectorOfSquare(seed.square) == sector))(PrimitiveKind.RouteContestSeed))
            .addAll(Option.when(enemy.nonEmpty)(PrimitiveKind.CounterplayResourceSeed))
            .result()
        Option.when(
          contractAllows(
            StrategicObjectFamily.RestrictionShell,
            FamilyGenerationEvidence(
              primitiveKinds = restrictionPrimitiveKinds,
              anchorSquares = focus.toSet,
              contestedSquares = enemy.toSet,
              files = (focus ++ enemy).map(_.file).toSet,
              metrics =
                FamilyGenerationMetrics(
                  pressureSquareCount = enemy.size,
                  targetWitnessCount = focus.size,
                  contestedOverlapCount = enemy.size
                )
            )
          )
        ) {
          val files = distinctFiles((focus ++ enemy).map(_.file))
          mkObject(
            id = objectId(StrategicObjectFamily.RestrictionShell, owner, Some(sector), enemy.headOption.orElse(focus.headOption), files.toSet),
            family = StrategicObjectFamily.RestrictionShell,
            owner = owner,
            locus = StrategicObjectLocus(squares = focus ++ enemy, files = files),
            sector = Some(sector),
            anchors = constraintSquareAnchors(focus) ++ pressureSquareAnchors(enemy),
            profile =
              StrategicObjectProfile.RestrictionShell(
                restrictedSquares = enemy,
                contestedSquares = focus,
                constraintSquares = focus
              ),
            supportingPrimitives = selectRefs(refs, Some(owner), focus.toSet, files.toSet, Set(PrimitiveKind.TargetSquare, PrimitiveKind.CriticalSquare, PrimitiveKind.RouteContestSeed, PrimitiveKind.AccessRoute)),
            supportingPieces = Nil,
            rivals = rivalsFrom(refs, !owner, enemy.toSet, files.toSet, Set.empty),
            horizonClass = ObjectHorizonClass.Structural,
            supportBalance = focus.size,
            pressureBalance = enemy.size,
            mobilityGain = 0,
            tags = Set(StrategicEvidenceTag.Restricted)
          )
        }
      }
    }

  private def mobilityCageObjects(
      bank: PrimitiveBank,
      refs: List[PrimitiveReference]
  ): List[StrategicObject] =
    bank.pieceRoleIssues.filter(_.issue == PieceRoleIssueKind.TrappedPiece).map { issue =>
      val denied =
        distinctSquares(
          bank.targetSquares.filter(p => p.owner == !issue.owner && p.square == issue.square).map(_.square) ++
            bank.counterplayResourceSeeds.filter(_.owner == !issue.owner).flatMap(_.pressureSquares).filter(_ == issue.square) ++
            bank.routeContestSeeds.filter(_.owner == !issue.owner).map(_.square).filter(square => sectorOfSquare(square) == sectorOfSquare(issue.square))
        )
      val repairs =
        distinctSquares(
          bank.redeploymentPathSeeds.filter(seed => seed.owner == issue.owner && seed.origin == issue.square).map(_.target) ++
            bank.knightRouteSeeds.filter(seed => seed.owner == issue.owner && seed.origin == issue.square).map(_.target) ++
            bank.diagonalLaneSeeds.filter(seed => seed.owner == issue.owner && seed.origin == issue.square).map(_.target)
        )
      mkObject(
        id = objectId(StrategicObjectFamily.MobilityCage, issue.owner, sec(issue.square), Some(issue.square), Set(issue.square.file)),
        family = StrategicObjectFamily.MobilityCage,
        owner = issue.owner,
        locus = StrategicObjectLocus(squares = issue.square :: denied ++ repairs, files = List(issue.square.file)),
        sector = sec(issue.square),
        anchors = pieceAnchors(List(pieceRef(issue.owner, issue.square, issue.role)), StrategicAnchorRole.Primary) ++ constraintSquareAnchors(denied) ++ exitSquareAnchors(repairs),
        profile =
          StrategicObjectProfile.MobilityCage(
            affectedPiece = pieceRef(issue.owner, issue.square, issue.role),
            deniedSquares = denied,
            repairTargets = repairs
          ),
        supportingPrimitives = selectRefs(refs, None, (issue.square :: denied ++ repairs).toSet, Set(issue.square.file), Set(PrimitiveKind.PieceRoleIssue, PrimitiveKind.TargetSquare, PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.RouteContestSeed, PrimitiveKind.RedeploymentPathSeed, PrimitiveKind.KnightRouteSeed, PrimitiveKind.DiagonalLaneSeed)),
        supportingPieces = List(pieceRef(issue.owner, issue.square, issue.role)),
        rivals = rivalsFrom(refs, !issue.owner, denied.toSet, Set(issue.square.file), Set(PrimitiveKind.TargetSquare, PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.RouteContestSeed)),
        horizonClass = ObjectHorizonClass.Maneuver,
        supportBalance = repairs.size,
        pressureBalance = denied.size - repairs.size,
        mobilityGain = 0,
        tags = Set(StrategicEvidenceTag.Trapped)
      )
    }

  private def redeploymentRouteObjects(
      bank: PrimitiveBank,
      refs: List[PrimitiveReference]
  ): List[StrategicObject] =
    bank.diagonalLaneSeeds.map(seed => routeObject(seed.owner, routeGeometry(seed), seed.role, None, refs, "diag")) ++
      bank.liftCorridorSeeds.map(seed => routeObject(seed.owner, routeGeometry(seed), Rook, None, refs, "lift")) ++
      bank.knightRouteSeeds.map(seed => routeObject(seed.owner, routeGeometry(seed), Knight, None, refs, "knight")) ++
      bank.redeploymentPathSeeds.map(seed => routeObject(seed.owner, routeGeometry(seed), seed.role, Some(seed.mobilityGain), refs, "redeploy"))

  private def routeObject(
      owner: Color,
      route: StrategicRouteGeometry,
      role: Role,
      mobilityGain: Option[Int],
      refs: List[PrimitiveReference],
      extra: String
  ): StrategicObject =
    mkObject(
      id = objectId(StrategicObjectFamily.RedeploymentRoute, owner, sec(route.target), Some(route.target), Set(route.target.file), extra = extra),
      family = StrategicObjectFamily.RedeploymentRoute,
      owner = owner,
      locus = StrategicObjectLocus(route = Some(route), squares = route.allSquares, files = List(route.target.file)),
      sector = sec(route.target),
      anchors = routeAnchors(List(route), StrategicAnchorRole.Entry) ++ exitSquareAnchors(List(route.target)),
      profile =
        StrategicObjectProfile.RedeploymentRoute(
          route = route,
          role = role,
          mobilityGain = mobilityGain
        ),
      supportingPrimitives = selectRefs(refs, Some(owner), route.allSquares.toSet, Set(route.target.file), Set(PrimitiveKind.DiagonalLaneSeed, PrimitiveKind.LiftCorridorSeed, PrimitiveKind.KnightRouteSeed, PrimitiveKind.RedeploymentPathSeed, PrimitiveKind.CriticalSquare, PrimitiveKind.RouteContestSeed, PrimitiveKind.TargetSquare, PrimitiveKind.PieceRoleIssue)),
      supportingPieces = List(pieceRef(owner, route.origin, role)),
      rivals = rivalsFrom(refs, !owner, (route.via :+ route.target).toSet, Set(route.target.file), Set(PrimitiveKind.RouteContestSeed, PrimitiveKind.TargetSquare, PrimitiveKind.CriticalSquare, PrimitiveKind.CounterplayResourceSeed)),
      horizonClass = ObjectHorizonClass.Maneuver,
      supportBalance = mobilityGain.getOrElse(route.via.size + 1),
      pressureBalance = 1,
      mobilityGain = mobilityGain.getOrElse(0),
      tags = mobilityGain.map(_ => StrategicEvidenceTag.MobilityGain).toSet ++ Set(StrategicEvidenceTag.RouteAccess)
    )

  private def passerObjects(
      bank: PrimitiveBank,
      refs: List[PrimitiveReference]
  ): List[StrategicObject] =
    bank.passerSeeds.flatMap { seed =>
      bank.criticalSquares.find(p => p.owner == seed.owner && p.kind == CriticalSquareKind.PromotionSquare && p.square.file == seed.square.file).map { promotion =>
        val escorts =
          distinctSquares(
            bank.routeContestSeeds.filter(route => route.owner == seed.owner && route.lane == seed.square.file).map(_.square) ++
              bank.redeploymentPathSeeds.filter(route => route.owner == seed.owner && route.target.file == seed.square.file).map(_.target)
          )
        mkObject(
          id = objectId(StrategicObjectFamily.PasserComplex, seed.owner, sec(seed.square), Some(seed.square), Set(seed.square.file)),
          family = StrategicObjectFamily.PasserComplex,
          owner = seed.owner,
          locus = StrategicObjectLocus(squares = seed.square :: promotion.square :: escorts, files = List(seed.square.file)),
          sector = sec(seed.square),
          anchors = primarySquareAnchors(List(seed.square)) ++ exitSquareAnchors(List(promotion.square)) ++ supportSquareAnchors(escorts),
          profile =
            StrategicObjectProfile.PasserComplex(
              passerSquare = seed.square,
              promotionSquare = promotion.square,
              relativeRank = seed.relativeRank,
              protectedByPawn = seed.protectedByPawn,
              escortSquares = escorts
            ),
          supportingPrimitives = selectRefs(refs, Some(seed.owner), (seed.square :: promotion.square :: escorts).toSet, Set(seed.square.file), Set(PrimitiveKind.PasserSeed, PrimitiveKind.CriticalSquare, PrimitiveKind.RouteContestSeed, PrimitiveKind.RedeploymentPathSeed)),
          supportingPieces = pawnPieces(seed.owner, List(seed.square)),
          rivals = rivalsFrom(refs, !seed.owner, (promotion.square :: escorts).toSet, Set(seed.square.file), Set(PrimitiveKind.TargetSquare, PrimitiveKind.RouteContestSeed, PrimitiveKind.AccessRoute, PrimitiveKind.BreakCandidate)),
          horizonClass = ObjectHorizonClass.Promotion,
          supportBalance = seed.relativeRank,
          pressureBalance = seed.relativeRank,
          mobilityGain = 0,
          tags = Set(StrategicEvidenceTag.Promotion) ++ Option.when(seed.protectedByPawn)(StrategicEvidenceTag.Protected)
        )
      }
    }

  private def defenderDependencyObjects(
      refs: List[PrimitiveReference],
      objects: List[StrategicObject]
  ): List[StrategicObject] =
    Owners.flatMap { owner =>
      val defendedRefs = refs.filter(ref => ref.owner == owner && ref.kind == PrimitiveKind.DefendedResource)
      defendedRefs.groupBy(ref => dominantSector(ref.allSquares, ref.lane.toList)).toList.flatMap { case (sector, group) =>
        val defendedSquares = distinctSquares(group.flatMap(_.allSquares))
        val files = distinctFiles(group.flatMap(_.lane) ++ defendedSquares.map(_.file))
        val pressureObjects =
          familyObjects(
            objects,
            !owner,
            Set(StrategicObjectFamily.FixedTargetComplex, StrategicObjectFamily.CounterplayAxis, StrategicObjectFamily.RestrictionShell)
          ).filter(obj => overlapsGroup(obj, defendedSquares.toSet, files.toSet, Some(sector)))
        val constrainedObjects =
          familyObjects(
            objects,
            owner,
            Set(StrategicObjectFamily.MobilityCage, StrategicObjectFamily.RestrictionShell, StrategicObjectFamily.CounterplayAxis)
          ).filter(obj => overlapsGroup(obj, defendedSquares.toSet, files.toSet, Some(sector)))
        val pressureSquares = distinctSquares(pressureObjects.flatMap(objectSquares))
        Option.when(
          contractAllows(
            StrategicObjectFamily.DefenderDependencyNetwork,
            FamilyGenerationEvidence(
              primitiveKinds = group.map(_.kind).toSet,
              sourceFamilies = constrainedObjects.map(_.family).toSet,
              rivalFamilies = pressureObjects.map(_.family).toSet,
              anchorSquares = defendedSquares.toSet,
              contestedSquares = pressureSquares.toSet,
              files = (files ++ pressureObjects.flatMap(objectFiles)).toSet,
              pieceRoles = group.flatMap(_.roles).toSet,
              metrics =
                FamilyGenerationMetrics(
                  sourceCount = constrainedObjects.size,
                  rivalCount = pressureObjects.size,
                  defendedSquareCount = defendedSquares.size,
                  defenderPieceCount = group.count(_.roles.nonEmpty),
                  pressureSquareCount = pressureSquares.size,
                  contestedOverlapCount = pressureSquares.size
                )
            )
          )
        ) {
          val sourceObjects = uniqueById(pressureObjects ++ constrainedObjects)
          val locusSquares = distinctSquares(defendedSquares ++ pressureSquares ++ constrainedObjects.flatMap(objectSquares))
          val locusFiles = distinctFiles(files ++ sourceObjects.flatMap(objectFiles))
          val supportingPrimitives =
            combinedPrimitives(
              sourceObjects,
              refs,
              locusSquares.toSet,
              locusFiles.toSet,
              Set(PrimitiveKind.DefendedResource, PrimitiveKind.TargetSquare, PrimitiveKind.RouteContestSeed, PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.PieceRoleIssue)
            )
          val supportingPieces = combinedPieces(sourceObjects, supportingPrimitives)
          mkObject(
            id = objectId(StrategicObjectFamily.DefenderDependencyNetwork, owner, Some(sector), defendedSquares.headOption, locusFiles.toSet),
            family = StrategicObjectFamily.DefenderDependencyNetwork,
            owner = owner,
            locus = StrategicObjectLocus(squares = locusSquares, files = locusFiles),
            sector = Some(sector),
            anchors =
              primarySquareAnchors(defendedSquares) ++
                pressureSquareAnchors(pressureSquares) ++
                pieceAnchors(supportingPieces, StrategicAnchorRole.Constraint),
            profile =
              StrategicObjectProfile.DefenderDependencyNetwork(
                defendedSquares = defendedSquares,
                defenderSquares = distinctSquares(group.flatMap(_.anchorSquares)),
                pressureSquares = pressureSquares,
                defenderRoles = group.flatMap(_.roles).toSet,
                features =
                  Set.newBuilder[DefenderDependencyFeature]
                    .addAll(Option.when(pressureObjects.exists(_.family == StrategicObjectFamily.FixedTargetComplex))(DefenderDependencyFeature.FixedAnchor))
                    .addAll(Option.when(constrainedObjects.exists(_.family == StrategicObjectFamily.RestrictionShell))(DefenderDependencyFeature.RestrictedDefender))
                    .addAll(Option.when(constrainedObjects.exists(_.family == StrategicObjectFamily.CounterplayAxis) || pressureObjects.exists(_.family == StrategicObjectFamily.CounterplayAxis))(DefenderDependencyFeature.CounterplayBound))
                    .addAll(Option.when(constrainedObjects.exists(_.family == StrategicObjectFamily.MobilityCage))(DefenderDependencyFeature.MobilityBound))
                    .result()
              ),
            supportingPrimitives = supportingPrimitives,
            supportingPieces = supportingPieces,
            rivals =
              combinedRivals(
                pressureObjects,
                refs,
                !owner,
                locusSquares.toSet,
                locusFiles.toSet,
                Set(PrimitiveKind.TargetSquare, PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.RouteContestSeed, PrimitiveKind.ReleaseCandidate)
              ),
            horizonClass = ObjectHorizonClass.Structural,
            supportBalance = group.size + constrainedObjects.size,
            pressureBalance = pressureObjects.size + pressureSquares.size,
            mobilityGain = 0,
            tags = Set(StrategicEvidenceTag.Dependency, StrategicEvidenceTag.Contested)
          )
        }
      }
    }

  private def tradeInvariantObjects(
      refs: List[PrimitiveReference],
      objects: List[StrategicObject]
  ): List[StrategicObject] =
    Owners.flatMap { owner =>
      val tradeRefs =
        refs.filter(ref =>
          ref.owner == owner && Set(PrimitiveKind.ReleaseCandidate, PrimitiveKind.ExchangeSquare).contains(ref.kind)
        )
      tradeRefs.groupBy(ref => dominantSector(ref.allSquares, ref.lane.toList)).toList.flatMap { case (sector, group) =>
        val tradeSquares = distinctSquares(group.flatMap(_.allSquares))
        val files = distinctFiles(group.flatMap(_.lane) ++ tradeSquares.map(_.file))
        val roots =
          familyObjects(
            objects,
            owner,
            Set(StrategicObjectFamily.BreakAxis, StrategicObjectFamily.AccessNetwork, StrategicObjectFamily.FixedTargetComplex, StrategicObjectFamily.PasserComplex)
          ).filter(obj => overlapsGroup(obj, tradeSquares.toSet, files.toSet, Some(sector)))
        val preserved =
          uniqueById(
            roots ++
              roots.flatMap(obj =>
                relatedObjects(
                  obj,
                  objects,
                  Set(StrategicRelationOperator.Enables, StrategicRelationOperator.Preserves),
                  Set(StrategicObjectFamily.FixedTargetComplex, StrategicObjectFamily.BreakAxis, StrategicObjectFamily.AccessNetwork, StrategicObjectFamily.PasserComplex)
                  )
              )
          )
        val tradeRelationOperators =
          relationOperatorsBetween(roots, preserved.map(_.id).toSet)
        Option.when(
          contractAllows(
            StrategicObjectFamily.TradeInvariant,
            FamilyGenerationEvidence(
              primitiveKinds = group.map(_.kind).toSet ++ preserved.flatMap(_.supportingPrimitives.map(_.kind)),
              sourceFamilies = preserved.map(_.family).toSet,
              relationOperators = tradeRelationOperators,
              anchorSquares = tradeSquares.toSet,
              contestedSquares = tradeSquares.toSet,
              files = (files ++ preserved.flatMap(objectFiles)).toSet,
              metrics =
                FamilyGenerationMetrics(
                  sourceCount = roots.size,
                  sharedAnchorCount = tradeSquares.size,
                  contestedOverlapCount = tradeSquares.size,
                  preservedSourceCount = preserved.size,
                  goalWitnessCount = tradeSquares.size
                )
            )
          )
        ) {
          val invariantSquares = distinctSquares(preserved.flatMap(objectSquares))
          val locusSquares = distinctSquares(tradeSquares ++ invariantSquares)
          val locusFiles = distinctFiles(files ++ preserved.flatMap(objectFiles))
          val supportingPrimitives =
            combinedPrimitives(
              preserved,
              refs,
              locusSquares.toSet,
              locusFiles.toSet,
              Set(PrimitiveKind.ReleaseCandidate, PrimitiveKind.ExchangeSquare, PrimitiveKind.BreakCandidate, PrimitiveKind.TargetSquare, PrimitiveKind.PasserSeed, PrimitiveKind.AccessRoute)
            )
          val supportingPieces = combinedPieces(preserved, supportingPrimitives)
          mkObject(
            id = objectId(StrategicObjectFamily.TradeInvariant, owner, Some(sector), tradeSquares.headOption.orElse(invariantSquares.headOption), locusFiles.toSet),
            family = StrategicObjectFamily.TradeInvariant,
            owner = owner,
            locus = StrategicObjectLocus(squares = locusSquares, files = locusFiles),
            sector = Some(sector),
            anchors =
              primarySquareAnchors(tradeSquares) ++
                supportSquareAnchors(invariantSquares) ++
                fileAnchors(locusFiles, StrategicAnchorRole.Secondary),
            profile =
              StrategicObjectProfile.TradeInvariant(
                exchangeSquares = tradeSquares,
                invariantSquares = invariantSquares,
                preservedFiles = locusFiles.toSet,
                preservedFamilies = preserved.map(_.family).toSet,
                features =
                  Set.newBuilder[TradeInvariantFeature]
                    .addAll(Option.when(preserved.exists(_.family == StrategicObjectFamily.FixedTargetComplex))(TradeInvariantFeature.FixedTargetAnchor))
                    .addAll(Option.when(preserved.exists(_.family == StrategicObjectFamily.BreakAxis))(TradeInvariantFeature.BreakAnchor))
                    .addAll(Option.when(preserved.exists(_.family == StrategicObjectFamily.AccessNetwork))(TradeInvariantFeature.AccessAnchor))
                    .addAll(Option.when(preserved.exists(_.family == StrategicObjectFamily.PasserComplex))(TradeInvariantFeature.PasserAnchor))
                    .addAll(Option.when(group.exists(_.releaseKind.nonEmpty))(TradeInvariantFeature.ReleaseOverlap))
                    .result()
              ),
            supportingPrimitives = supportingPrimitives,
            supportingPieces = supportingPieces,
            rivals =
              combinedRivals(
                familyObjects(objects, !owner, Set(StrategicObjectFamily.CounterplayAxis, StrategicObjectFamily.RestrictionShell, StrategicObjectFamily.FixedTargetComplex))
                  .filter(obj => overlapsGroup(obj, locusSquares.toSet, locusFiles.toSet, Some(sector))),
                refs,
                !owner,
                locusSquares.toSet,
                locusFiles.toSet,
                Set(PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.DefendedResource, PrimitiveKind.ReleaseCandidate)
              ),
            horizonClass = ObjectHorizonClass.Structural,
            supportBalance = preserved.size,
            pressureBalance = tradeSquares.size,
            mobilityGain = 0,
            tags = Set(StrategicEvidenceTag.Exchange, StrategicEvidenceTag.Contested)
          )
        }
      }
    }

  private def tensionStateObjects(
      refs: List[PrimitiveReference],
      objects: List[StrategicObject]
  ): List[StrategicObject] =
    Owners.flatMap { owner =>
      val contactRefs =
        refs.filter(ref => ref.owner == owner && Set(PrimitiveKind.TensionContactSeed, PrimitiveKind.LeverContactSeed).contains(ref.kind))
      val releaseRefs = refs.filter(ref => ref.owner == owner && ref.kind == PrimitiveKind.ReleaseCandidate)
      contactRefs.groupBy(ref => dominantSector(ref.allSquares, ref.lane.toList)).toList.flatMap { case (sector, group) =>
        val contactSquares = distinctSquares(group.flatMap(_.allSquares))
        val files = distinctFiles(group.flatMap(_.lane) ++ releaseRefs.flatMap(_.lane) ++ contactSquares.map(_.file))
        val ownAxes =
          familyObjects(objects, owner, Set(StrategicObjectFamily.BreakAxis, StrategicObjectFamily.CounterplayAxis, StrategicObjectFamily.RestrictionShell))
            .filter(obj => overlapsGroup(obj, contactSquares.toSet, files.toSet, Some(sector)))
        val rivalAxes =
          familyObjects(objects, !owner, Set(StrategicObjectFamily.BreakAxis, StrategicObjectFamily.CounterplayAxis, StrategicObjectFamily.RestrictionShell))
            .filter(obj => overlapsGroup(obj, contactSquares.toSet, files.toSet, Some(sector)))
        val releaseSquares = distinctSquares(releaseRefs.flatMap(_.allSquares).filter(square => sectorOfSquare(square) == sector))
        val tensionPrimitiveKinds =
          Set.newBuilder[PrimitiveKind]
            .addAll(Option.when(group.exists(_.kind == PrimitiveKind.TensionContactSeed))(PrimitiveKind.TensionContactSeed))
            .addAll(Option.when(group.exists(_.kind == PrimitiveKind.LeverContactSeed))(PrimitiveKind.LeverContactSeed))
            .addAll(Option.when(releaseSquares.nonEmpty)(PrimitiveKind.ReleaseCandidate))
            .result()
        Option.when(
          contractAllows(
            StrategicObjectFamily.TensionState,
            FamilyGenerationEvidence(
              primitiveKinds = tensionPrimitiveKinds,
              sourceFamilies = ownAxes.map(_.family).toSet,
              rivalFamilies = rivalAxes.map(_.family).toSet,
              anchorSquares = contactSquares.toSet,
              contestedSquares = (contactSquares ++ releaseSquares).toSet,
              files = files.toSet,
              metrics =
                FamilyGenerationMetrics(
                  sourceCount = ownAxes.size,
                  rivalCount = rivalAxes.size,
                  pressureSquareCount = releaseSquares.size + rivalAxes.flatMap(objectSquares).size,
                  contestedOverlapCount = (contactSquares ++ releaseSquares).size
                )
            )
          )
        ) {
          val locusSquares = distinctSquares(contactSquares ++ releaseSquares ++ ownAxes.flatMap(objectSquares) ++ rivalAxes.flatMap(objectSquares))
          val locusFiles = distinctFiles(files ++ ownAxes.flatMap(objectFiles) ++ rivalAxes.flatMap(objectFiles))
          val supportingPrimitives =
            combinedPrimitives(
              uniqueById(ownAxes ++ rivalAxes),
              refs,
              locusSquares.toSet,
              locusFiles.toSet,
              Set(PrimitiveKind.TensionContactSeed, PrimitiveKind.LeverContactSeed, PrimitiveKind.ReleaseCandidate, PrimitiveKind.BreakCandidate, PrimitiveKind.CounterplayResourceSeed)
            )
          val supportingPieces = combinedPieces(ownAxes, supportingPrimitives)
          mkObject(
            id = objectId(StrategicObjectFamily.TensionState, owner, Some(sector), contactSquares.headOption, locusFiles.toSet),
            family = StrategicObjectFamily.TensionState,
            owner = owner,
            locus = StrategicObjectLocus(squares = locusSquares, files = locusFiles),
            sector = Some(sector),
            anchors =
              primarySquareAnchors(contactSquares) ++
                pressureSquareAnchors(releaseSquares) ++
                fileAnchors(locusFiles, StrategicAnchorRole.Constraint),
            profile =
              StrategicObjectProfile.TensionState(
                contactSquares = contactSquares,
                releaseSquares = releaseSquares,
                pressureSquares = distinctSquares(rivalAxes.flatMap(objectSquares) ++ releaseSquares),
                breakSquares =
                  distinctSquares(
                    ownAxes.collect {
                      case obj if obj.family == StrategicObjectFamily.BreakAxis => objectSquares(obj)
                    }.flatten
                  ),
                features =
                  Set.newBuilder[TensionFeature]
                    .addAll(Option.when(group.exists(_.flags.contains(PrimitiveEvidenceFlag.Maintainable)))(TensionFeature.MaintainableContact))
                    .addAll(Option.when(releaseSquares.nonEmpty)(TensionFeature.ReleasePressure))
                    .addAll(Option.when(ownAxes.exists(_.family == StrategicObjectFamily.BreakAxis))(TensionFeature.BreakPressure))
                    .addAll(Option.when((ownAxes ++ rivalAxes).exists(_.family == StrategicObjectFamily.CounterplayAxis))(TensionFeature.CounterplayPressure))
                    .addAll(Option.when((ownAxes ++ rivalAxes).exists(_.family == StrategicObjectFamily.RestrictionShell))(TensionFeature.RestrictionOverlay))
                    .result()
              ),
            supportingPrimitives = supportingPrimitives,
            supportingPieces = supportingPieces,
            rivals =
              combinedRivals(
                rivalAxes,
                refs,
                !owner,
                locusSquares.toSet,
                locusFiles.toSet,
                Set(PrimitiveKind.TensionContactSeed, PrimitiveKind.ReleaseCandidate, PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.BreakCandidate)
              ),
            horizonClass = ObjectHorizonClass.Operational,
            supportBalance = ownAxes.size + group.size,
            pressureBalance = rivalAxes.size + releaseSquares.size,
            mobilityGain = 0,
            tags = Set(StrategicEvidenceTag.Tension, StrategicEvidenceTag.Contested)
          )
        }
      }
    }

  private def attackScaffoldObjects(
      refs: List[PrimitiveReference],
      objects: List[StrategicObject]
  ): List[StrategicObject] =
    Owners.flatMap { owner =>
      val hookRefs = refs.filter(ref => ref.owner == owner && ref.kind == PrimitiveKind.HookContactSeed)
      val ownRoutes =
        familyObjects(objects, owner, Set(StrategicObjectFamily.AccessNetwork, StrategicObjectFamily.RedeploymentRoute))
      val ownCritical = familyObjects(objects, owner, Set(StrategicObjectFamily.CriticalSquareComplex))
      val enemyShells = familyObjects(objects, !owner, Set(StrategicObjectFamily.KingSafetyShell))
      enemyShells.flatMap { shell =>
        val routeParticipants = ownRoutes.filter(obj => overlapsObject(obj, shell))
        val enabledCritical =
          uniqueById(
            ownCritical.filter(obj => overlapsObject(obj, shell)) ++
              routeParticipants.flatMap(obj =>
                relatedObjects(
                  obj,
                  objects,
                  Set(StrategicRelationOperator.Enables),
                  Set(StrategicObjectFamily.CriticalSquareComplex, StrategicObjectFamily.FixedTargetComplex, StrategicObjectFamily.PasserComplex)
                )
              )
          )
        val hookSquares =
          distinctSquares(
            hookRefs.flatMap(_.allSquares).filter(square => shell.sector == sectorOfSquare(square))
          )
        val participants = uniqueById(routeParticipants ++ enabledCritical :+ shell)
        Option.when(
          contractAllows(
            StrategicObjectFamily.AttackScaffold,
            FamilyGenerationEvidence(
              primitiveKinds =
                routeParticipants.flatMap(_.supportingPrimitives.map(_.kind)).toSet ++
                  enabledCritical.flatMap(_.supportingPrimitives.map(_.kind)) ++
                  Option.when(hookSquares.nonEmpty)(PrimitiveKind.HookContactSeed),
              sourceFamilies = (routeParticipants ++ enabledCritical).map(_.family).toSet,
              rivalFamilies = Set(shell.family),
              anchorSquares = hookSquares.toSet ++ distinctSquares(objectSquares(shell)).toSet,
              contestedSquares = distinctSquares(objectSquares(shell)).toSet,
              files = participants.flatMap(objectFiles).toSet,
              metrics =
                FamilyGenerationMetrics(
                  sourceCount = routeParticipants.size + enabledCritical.size,
                  rivalCount = 1,
                  pressureSquareCount = distinctSquares(objectSquares(shell)).size,
                  entryWitnessCount = routeParticipants.size + hookSquares.size,
                  targetWitnessCount = enabledCritical.size + hookSquares.size,
                  contestedOverlapCount = distinctSquares(objectSquares(shell)).size
                )
            )
          )
        ) {
          val targetSquares = distinctSquares(objectSquares(shell))
          val scaffoldSquares = distinctSquares(enabledCritical.flatMap(objectSquares))
          val routes = distinctRoutes(routeParticipants.flatMap(objectRoutes))
          val locusSquares = distinctSquares(targetSquares ++ scaffoldSquares ++ hookSquares ++ routes.flatMap(_.allSquares))
          val locusFiles = distinctFiles(participants.flatMap(objectFiles) ++ locusSquares.map(_.file))
          val supportingPrimitives =
            combinedPrimitives(
              participants,
              refs,
              locusSquares.toSet,
              locusFiles.toSet,
              Set(PrimitiveKind.AccessRoute, PrimitiveKind.RouteContestSeed, PrimitiveKind.RedeploymentPathSeed, PrimitiveKind.DiagonalLaneSeed, PrimitiveKind.LiftCorridorSeed, PrimitiveKind.KnightRouteSeed, PrimitiveKind.CriticalSquare, PrimitiveKind.HookContactSeed, PrimitiveKind.TargetSquare)
            )
          val supportingPieces = combinedPieces(participants, supportingPrimitives)
          mkObject(
            id = objectId(StrategicObjectFamily.AttackScaffold, owner, Some(shell.sector), targetSquares.headOption.orElse(hookSquares.headOption), locusFiles.toSet),
            family = StrategicObjectFamily.AttackScaffold,
            owner = owner,
            locus = StrategicObjectLocus(squares = locusSquares, files = locusFiles, route = routes.headOption),
            sector = Some(shell.sector),
            anchors =
              pressureSquareAnchors(targetSquares) ++
                supportSquareAnchors(scaffoldSquares) ++
                pressureSquareAnchors(hookSquares) ++
                routeAnchors(routes, StrategicAnchorRole.Entry) ++
                pieceAnchors(supportingPieces, StrategicAnchorRole.Support),
            profile =
              StrategicObjectProfile.AttackScaffold(
                targetOwner = shell.owner,
                scaffoldSquares = distinctSquares(targetSquares ++ scaffoldSquares ++ hookSquares),
                entryFiles = locusFiles.toSet,
                entryRoutes = routes,
                features =
                  Set.newBuilder[AttackScaffoldFeature]
                    .addAll(Option.when(routeParticipants.exists(_.family == StrategicObjectFamily.AccessNetwork) && routeParticipants.exists(obj => objectFiles(obj).nonEmpty))(AttackScaffoldFeature.FileAccess))
                    .addAll(Option.when(routeParticipants.exists(_.family == StrategicObjectFamily.AccessNetwork) && routes.nonEmpty)(AttackScaffoldFeature.RouteAccess))
                    .addAll(Option.when(routeParticipants.exists(_.family == StrategicObjectFamily.RedeploymentRoute))(AttackScaffoldFeature.RedeploymentLift))
                    .addAll(Option.when(enabledCritical.nonEmpty)(AttackScaffoldFeature.CriticalEntry))
                    .addAll(Option.when(hookSquares.nonEmpty)(AttackScaffoldFeature.HookPressure))
                    .result()
              ),
            supportingPrimitives = supportingPrimitives,
            supportingPieces = supportingPieces,
            rivals =
              combinedRivals(
                List(shell),
                refs,
                !owner,
                locusSquares.toSet,
                locusFiles.toSet,
                Set(PrimitiveKind.DefendedResource, PrimitiveKind.RouteContestSeed, PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.TargetSquare)
              ),
            horizonClass = ObjectHorizonClass.Operational,
            supportBalance = routeParticipants.size + enabledCritical.size,
            pressureBalance = targetSquares.size + hookSquares.size,
            mobilityGain = routes.map(_.via.size).sum,
          tags = Set(StrategicEvidenceTag.Attack, StrategicEvidenceTag.ShellPressure)
          )
        }
      }
    }

  private def materialInvestmentContractObjects(
      refs: List[PrimitiveReference],
      truth: MoveTruthFrame,
      objects: List[StrategicObject]
  ): List[StrategicObject] =
    Option.when(hasMaterialInvestment(truth)) {
      Owners.flatMap { owner =>
        val contractObjects =
          familyObjects(
            objects,
            owner,
            Set(
              StrategicObjectFamily.FixedTargetComplex,
              StrategicObjectFamily.AccessNetwork,
              StrategicObjectFamily.CounterplayAxis,
              StrategicObjectFamily.AttackScaffold
            )
          )
        Option.when(contractObjects.exists(_.family == StrategicObjectFamily.AttackScaffold) || contractObjects.map(_.family).distinct.size >= 2) {
          val compensationSquares = distinctSquares(contractObjects.flatMap(objectSquares))
          val files = distinctFiles(contractObjects.flatMap(objectFiles))
          val supportingPrimitives =
            combinedPrimitives(
              contractObjects,
              refs,
              compensationSquares.toSet,
              files.toSet,
              Set(PrimitiveKind.TargetSquare, PrimitiveKind.AccessRoute, PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.HookContactSeed, PrimitiveKind.BreakCandidate)
            )
          val supportingPieces = combinedPieces(contractObjects, supportingPrimitives)
          val investedCp =
            truth.materialEconomics.investedMaterialCp.getOrElse(math.max(0, truth.materialEconomics.afterDeficit - truth.materialEconomics.beforeDeficit))
          mkObject(
            id = objectId(StrategicObjectFamily.MaterialInvestmentContract, owner, Some(dominantSector(compensationSquares, files)), compensationSquares.headOption, files.toSet),
            family = StrategicObjectFamily.MaterialInvestmentContract,
            owner = owner,
            locus = StrategicObjectLocus(squares = compensationSquares, files = files),
            sector = Some(dominantSector(compensationSquares, files)),
            anchors =
              primarySquareAnchors(compensationSquares) ++
                pieceAnchors(supportingPieces, StrategicAnchorRole.Support) ++
                fileAnchors(files, StrategicAnchorRole.Secondary),
            profile =
              StrategicObjectProfile.MaterialInvestmentContract(
                investedMaterialCp = investedCp,
                beforeDeficit = truth.materialEconomics.beforeDeficit,
                afterDeficit = truth.materialEconomics.afterDeficit,
                compensationSquares = compensationSquares,
                compensationFiles = files.toSet,
                features =
                  Set.newBuilder[MaterialInvestmentFeature]
                    .addOne(MaterialInvestmentFeature.InvestedMaterial)
                    .addAll(Option.when(truth.materialEconomics.afterDeficit > truth.materialEconomics.beforeDeficit)(MaterialInvestmentFeature.IncreasedDeficit))
                    .addAll(Option.when(contractObjects.exists(_.family == StrategicObjectFamily.CounterplayAxis))(MaterialInvestmentFeature.CounterplayCompensation))
                    .addAll(Option.when(contractObjects.exists(_.family == StrategicObjectFamily.AccessNetwork))(MaterialInvestmentFeature.AccessCompensation))
                    .addAll(Option.when(contractObjects.exists(_.family == StrategicObjectFamily.FixedTargetComplex))(MaterialInvestmentFeature.TargetCompensation))
                    .addAll(Option.when(contractObjects.exists(_.family == StrategicObjectFamily.AttackScaffold))(MaterialInvestmentFeature.AttackCompensation))
                    .result()
              ),
            supportingPrimitives = supportingPrimitives,
            supportingPieces = supportingPieces,
            rivals =
              combinedRivals(
                familyObjects(objects, !owner, Set(StrategicObjectFamily.CounterplayAxis, StrategicObjectFamily.RestrictionShell, StrategicObjectFamily.KingSafetyShell))
                  .filter(obj => overlapsGroup(obj, compensationSquares.toSet, files.toSet, None)),
                refs,
                !owner,
                compensationSquares.toSet,
                files.toSet,
                Set(PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.ReleaseCandidate, PrimitiveKind.DefendedResource)
              ),
            horizonClass = ObjectHorizonClass.Operational,
            supportBalance = contractObjects.size + math.max(1, investedCp / 100),
            pressureBalance = truth.materialEconomics.afterDeficit - truth.materialEconomics.beforeDeficit,
            mobilityGain = 0,
            tags = Set(StrategicEvidenceTag.Investment)
          )
        }
      }
    }.getOrElse(Nil)

  private def initiativeWindowObjects(
      refs: List[PrimitiveReference],
      truth: MoveTruthFrame,
      objects: List[StrategicObject]
  ): List[StrategicObject] =
    val _ = truth
    Owners.flatMap { owner =>
      val activators =
        familyObjects(
          objects,
          owner,
          Set(
            StrategicObjectFamily.AttackScaffold,
            StrategicObjectFamily.CounterplayAxis,
            StrategicObjectFamily.BreakAxis,
            StrategicObjectFamily.AccessNetwork
          )
        )
      val rivalObjects =
        familyObjects(
          objects,
          !owner,
          Set(
            StrategicObjectFamily.KingSafetyShell,
            StrategicObjectFamily.CounterplayAxis,
            StrategicObjectFamily.RestrictionShell,
            StrategicObjectFamily.TensionState,
            StrategicObjectFamily.AttackScaffold
          )
        )
      activators.groupBy(_.sector).toList.flatMap { case (sector, ownGroup) =>
        val ownRelated =
          uniqueById(
            ownGroup ++
              ownGroup.flatMap(obj =>
                relatedObjects(
                  obj,
                  objects,
                  Set(StrategicRelationOperator.Enables, StrategicRelationOperator.OverloadsOrUndermines, StrategicRelationOperator.Denies),
                  Set(StrategicObjectFamily.KingSafetyShell, StrategicObjectFamily.CounterplayAxis, StrategicObjectFamily.RestrictionShell)
                )
              )
          )
        val response =
          rivalObjects.filter(obj => sector == obj.sector || ownRelated.exists(overlapsObject(_, obj)))
        val sharedSquares =
          distinctSquares(ownRelated.flatMap(objectSquares).intersect(response.flatMap(objectSquares)))
        Option.when(
          contractAllows(
            StrategicObjectFamily.InitiativeWindow,
            FamilyGenerationEvidence(
              primitiveKinds =
                ownRelated.flatMap(_.supportingPrimitives.map(_.kind)).toSet ++
                  response.flatMap(_.supportingPrimitives.map(_.kind)),
              sourceFamilies = ownRelated.map(_.family).toSet,
              rivalFamilies = response.map(_.family).toSet,
              relationOperators = relationOperatorsBetween(ownRelated, response.map(_.id).toSet),
              anchorSquares = sharedSquares.toSet,
              contestedSquares = sharedSquares.toSet,
              files = (ownRelated.flatMap(objectFiles) ++ response.flatMap(objectFiles)).toSet,
              metrics =
                FamilyGenerationMetrics(
                  sourceCount = ownRelated.size,
                  rivalCount = response.size,
                  sharedAnchorCount = sharedSquares.size,
                  contestedOverlapCount = sharedSquares.size,
                  pressureSquareCount = response.flatMap(objectSquares).size
                )
            )
          )
        ) {
          val windowSquares = distinctSquares(ownRelated.flatMap(objectSquares) ++ response.flatMap(objectSquares))
          val files = distinctFiles(ownRelated.flatMap(objectFiles) ++ response.flatMap(objectFiles))
          val supportingPrimitives =
            combinedPrimitives(
              uniqueById(ownRelated ++ response),
              refs,
              windowSquares.toSet,
              files.toSet,
              Set(PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.BreakCandidate, PrimitiveKind.AccessRoute, PrimitiveKind.HookContactSeed, PrimitiveKind.TensionContactSeed, PrimitiveKind.ReleaseCandidate, PrimitiveKind.TargetSquare)
            )
          val supportingPieces = combinedPieces(ownRelated, supportingPrimitives)
          mkObject(
            id = objectId(StrategicObjectFamily.InitiativeWindow, owner, Some(sector), windowSquares.headOption, files.toSet),
            family = StrategicObjectFamily.InitiativeWindow,
            owner = owner,
            locus = StrategicObjectLocus(squares = windowSquares, files = files),
            sector = Some(sector),
            anchors =
              primarySquareAnchors(windowSquares) ++
                fileAnchors(files, StrategicAnchorRole.Pressure) ++
                pieceAnchors(supportingPieces, StrategicAnchorRole.Support),
            profile =
              StrategicObjectProfile.InitiativeWindow(
                windowSquares = windowSquares,
                triggerFiles = files.toSet,
                rivalPressureSquares = distinctSquares(response.flatMap(objectSquares)),
                catalystFamilies = ownRelated.map(_.family).toSet,
                features =
                  Set.newBuilder[InitiativeWindowFeature]
                    .addAll(Option.when(ownRelated.exists(_.family == StrategicObjectFamily.BreakAxis))(InitiativeWindowFeature.BreakTiming))
                    .addAll(Option.when(ownRelated.exists(_.family == StrategicObjectFamily.CounterplayAxis))(InitiativeWindowFeature.CounterplayTiming))
                    .addAll(Option.when(ownRelated.exists(_.family == StrategicObjectFamily.AccessNetwork))(InitiativeWindowFeature.AccessTiming))
                    .addAll(Option.when(ownRelated.exists(_.family == StrategicObjectFamily.AttackScaffold))(InitiativeWindowFeature.AttackTiming))
                    .addAll(Option.when(response.exists(_.family == StrategicObjectFamily.KingSafetyShell))(InitiativeWindowFeature.ShellStress))
                    .addAll(Option.when(truth.tactical.forcingLine || truth.difficultyNovelty.depthSensitive || truth.moveQuality.swingSeverity > 0)(InitiativeWindowFeature.ForcingHint))
                    .result()
              ),
            supportingPrimitives = supportingPrimitives,
            supportingPieces = supportingPieces,
            rivals =
              combinedRivals(
                response,
                refs,
                !owner,
                windowSquares.toSet,
                files.toSet,
                Set(PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.ReleaseCandidate, PrimitiveKind.TargetSquare, PrimitiveKind.DefendedResource)
              ),
            horizonClass = ObjectHorizonClass.Operational,
            supportBalance = ownRelated.size,
            pressureBalance = response.size + Option.when(truth.tactical.forcingLine)(1).getOrElse(0),
            mobilityGain = 0,
            tags = Set(StrategicEvidenceTag.Initiative)
          )
        }
      }
    }

  private def conversionFunnelObjects(
      refs: List[PrimitiveReference],
      objects: List[StrategicObject]
  ): List[StrategicObject] =
    Owners.flatMap { owner =>
      val entryObjects =
        familyObjects(objects, owner, Set(StrategicObjectFamily.FixedTargetComplex, StrategicObjectFamily.TradeInvariant))
      val channelObjects =
        familyObjects(objects, owner, Set(StrategicObjectFamily.AccessNetwork, StrategicObjectFamily.RestrictionShell, StrategicObjectFamily.TradeInvariant))
      val exitObjects =
        familyObjects(objects, owner, Set(StrategicObjectFamily.PasserComplex, StrategicObjectFamily.TradeInvariant))
      val connectedChannels =
        channelObjects.filter(channel => entryObjects.exists(overlapsObject(channel, _)) || exitObjects.exists(overlapsObject(channel, _)))
      val connectedEntries =
        entryObjects.filter(entry => connectedChannels.exists(overlapsObject(entry, _)) || exitObjects.exists(overlapsObject(entry, _)))
      val connectedExits =
        exitObjects.filter(exit => connectedChannels.exists(overlapsObject(exit, _)) || connectedEntries.exists(overlapsObject(exit, _)))
      val participants = uniqueById(connectedEntries ++ connectedChannels ++ connectedExits)
      Option.when(connectedEntries.nonEmpty && connectedChannels.nonEmpty && participants.size >= 2) {
        val entrySquares = distinctSquares(connectedEntries.flatMap(objectSquares))
        val channelSquares = distinctSquares(connectedChannels.flatMap(objectSquares))
        val exitSquares = distinctSquares(connectedExits.flatMap(objectSquares))
        val locusSquares = distinctSquares(entrySquares ++ channelSquares ++ exitSquares)
        val files = distinctFiles(participants.flatMap(objectFiles))
        val supportingPrimitives =
          combinedPrimitives(
            participants,
            refs,
            locusSquares.toSet,
            files.toSet,
            Set(PrimitiveKind.TargetSquare, PrimitiveKind.AccessRoute, PrimitiveKind.RouteContestSeed, PrimitiveKind.ReleaseCandidate, PrimitiveKind.PasserSeed)
          )
        val supportingPieces = combinedPieces(participants, supportingPrimitives)
        mkObject(
          id = objectId(StrategicObjectFamily.ConversionFunnel, owner, Some(dominantSector(locusSquares, files)), entrySquares.headOption.orElse(channelSquares.headOption), files.toSet),
          family = StrategicObjectFamily.ConversionFunnel,
          owner = owner,
          locus = StrategicObjectLocus(squares = locusSquares, files = files),
          sector = Some(dominantSector(locusSquares, files)),
          anchors =
            primarySquareAnchors(entrySquares) ++
              supportSquareAnchors(channelSquares) ++
              exitSquareAnchors(exitSquares) ++
              pieceAnchors(supportingPieces, StrategicAnchorRole.Support),
          profile =
            StrategicObjectProfile.ConversionFunnel(
              entrySquares = entrySquares,
              channelSquares = channelSquares,
              exitSquares = exitSquares,
              funnelFiles = files.toSet,
              features =
                Set.newBuilder[ConversionFunnelFeature]
                  .addAll(Option.when(connectedEntries.exists(_.family == StrategicObjectFamily.FixedTargetComplex))(ConversionFunnelFeature.TargetEntry))
                  .addAll(Option.when(connectedChannels.exists(_.family == StrategicObjectFamily.RestrictionShell))(ConversionFunnelFeature.RestrictionGate))
                  .addAll(Option.when(connectedEntries.exists(_.family == StrategicObjectFamily.TradeInvariant) || connectedExits.exists(_.family == StrategicObjectFamily.TradeInvariant))(ConversionFunnelFeature.TradeChannel))
                  .addAll(Option.when(connectedChannels.exists(_.family == StrategicObjectFamily.AccessNetwork))(ConversionFunnelFeature.AccessChannel))
                  .addAll(Option.when(connectedExits.exists(_.family == StrategicObjectFamily.PasserComplex))(ConversionFunnelFeature.PasserExit))
                  .result()
            ),
          supportingPrimitives = supportingPrimitives,
          supportingPieces = supportingPieces,
          rivals =
            combinedRivals(
              familyObjects(objects, !owner, Set(StrategicObjectFamily.CounterplayAxis, StrategicObjectFamily.RestrictionShell, StrategicObjectFamily.PasserComplex))
                .filter(obj => overlapsGroup(obj, locusSquares.toSet, files.toSet, None)),
              refs,
              !owner,
              locusSquares.toSet,
              files.toSet,
              Set(PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.DefendedResource, PrimitiveKind.ReleaseCandidate)
            ),
          horizonClass = ObjectHorizonClass.Operational,
          supportBalance = connectedEntries.size + connectedChannels.size,
          pressureBalance = connectedExits.size,
          mobilityGain = 0,
          tags = Set(StrategicEvidenceTag.Conversion)
        )
      }
    }

  private def fortressHoldingShellObjects(
      refs: List[PrimitiveReference],
      objects: List[StrategicObject]
  ): List[StrategicObject] =
    Owners.flatMap { owner =>
      val holdingObjects =
        familyObjects(
          objects,
          owner,
          Set(
            StrategicObjectFamily.RestrictionShell,
            StrategicObjectFamily.MobilityCage,
            StrategicObjectFamily.CriticalSquareComplex,
            StrategicObjectFamily.KingSafetyShell
          )
        )
      val invadingObjects =
        familyObjects(objects, !owner, Set(StrategicObjectFamily.PasserComplex, StrategicObjectFamily.AccessNetwork, StrategicObjectFamily.AttackScaffold))
      val threatened =
        invadingObjects.filter(invader => holdingObjects.exists(overlapsObject(invader, _)))
      val holdSquares = distinctSquares(holdingObjects.flatMap(objectSquares))
      val blockadeSquares = distinctSquares(threatened.flatMap(objectSquares))
      val noEntrySquares =
        distinctSquares(
          familyObjects(
            objects,
            owner,
            Set(
              StrategicObjectFamily.RestrictionShell,
              StrategicObjectFamily.MobilityCage,
              StrategicObjectFamily.CriticalSquareComplex
            )
          )
            .flatMap(objectSquares)
        )
      val guardedSquares =
        distinctSquares(familyObjects(objects, owner, Set(StrategicObjectFamily.KingSafetyShell)).flatMap(objectSquares))
      Option.when(
        contractAllows(
          StrategicObjectFamily.FortressHoldingShell,
          FamilyGenerationEvidence(
            primitiveKinds =
              holdingObjects.flatMap(_.supportingPrimitives.map(_.kind)).toSet ++
                threatened.flatMap(_.supportingPrimitives.map(_.kind)),
            sourceFamilies = holdingObjects.map(_.family).toSet,
            rivalFamilies = threatened.map(_.family).toSet,
            anchorSquares = holdSquares.toSet,
            contestedSquares = blockadeSquares.toSet,
            files = (holdingObjects.flatMap(objectFiles) ++ threatened.flatMap(objectFiles)).toSet,
            metrics =
              FamilyGenerationMetrics(
                sourceCount = holdingObjects.size,
                rivalCount = threatened.size,
                deniedEntryCount = noEntrySquares.size,
                blockadeCount = blockadeSquares.size,
                pressureSquareCount = threatened.flatMap(objectSquares).size,
                contestedOverlapCount = blockadeSquares.size
              )
          )
        )
      ) {
        val locusSquares = distinctSquares(holdSquares ++ blockadeSquares ++ noEntrySquares ++ guardedSquares)
        val files = distinctFiles(holdingObjects.flatMap(objectFiles) ++ threatened.flatMap(objectFiles))
        val supportingPrimitives =
          combinedPrimitives(
            uniqueById(holdingObjects ++ threatened),
            refs,
            locusSquares.toSet,
            files.toSet,
            Set(PrimitiveKind.TargetSquare, PrimitiveKind.CriticalSquare, PrimitiveKind.RouteContestSeed, PrimitiveKind.AccessRoute, PrimitiveKind.PasserSeed)
          )
        val supportingPieces = combinedPieces(holdingObjects, supportingPrimitives)
        mkObject(
          id = objectId(StrategicObjectFamily.FortressHoldingShell, owner, Some(dominantSector(locusSquares, files)), holdSquares.headOption.orElse(blockadeSquares.headOption), files.toSet),
          family = StrategicObjectFamily.FortressHoldingShell,
          owner = owner,
          locus = StrategicObjectLocus(squares = locusSquares, files = files),
          sector = Some(dominantSector(locusSquares, files)),
          anchors =
            constraintSquareAnchors(holdSquares) ++
              pressureSquareAnchors(blockadeSquares) ++
              supportSquareAnchors(noEntrySquares) ++
              pieceAnchors(supportingPieces, StrategicAnchorRole.Support),
          profile =
            StrategicObjectProfile.FortressHoldingShell(
              holdSquares = holdSquares,
              entryDeniedSquares = distinctSquares(blockadeSquares ++ noEntrySquares),
              blockadeSquares = blockadeSquares,
              shellFiles = files.toSet,
              features =
                Set.newBuilder[HoldingShellFeature]
                  .addAll(Option.when(holdingObjects.exists(_.family == StrategicObjectFamily.RestrictionShell))(HoldingShellFeature.RestrictionWall))
                  .addAll(Option.when(holdingObjects.exists(_.family == StrategicObjectFamily.MobilityCage))(HoldingShellFeature.MobilityWall))
                  .addAll(Option.when(holdingObjects.exists(_.family == StrategicObjectFamily.CriticalSquareComplex))(HoldingShellFeature.CriticalHold))
                  .addAll(Option.when(threatened.exists(_.family == StrategicObjectFamily.PasserComplex))(HoldingShellFeature.PasserBlockade))
                  .addAll(Option.when(holdingObjects.exists(_.family == StrategicObjectFamily.KingSafetyShell))(HoldingShellFeature.ShellCover))
                  .result()
            ),
          supportingPrimitives = supportingPrimitives,
          supportingPieces = supportingPieces,
          rivals =
            combinedRivals(
              threatened,
              refs,
              !owner,
              locusSquares.toSet,
              files.toSet,
              Set(PrimitiveKind.PasserSeed, PrimitiveKind.AccessRoute, PrimitiveKind.HookContactSeed, PrimitiveKind.CounterplayResourceSeed)
            ),
          horizonClass = ObjectHorizonClass.Structural,
          supportBalance = holdingObjects.size,
          pressureBalance = threatened.size,
          mobilityGain = 0,
          tags = Set(StrategicEvidenceTag.Holding, StrategicEvidenceTag.Restricted)
        )
      }
    }

  private def planRaceObjects(
      refs: List[PrimitiveReference],
      objects: List[StrategicObject]
  ): List[StrategicObject] =
    Owners.flatMap { owner =>
      val ownRaceObjects =
        familyObjects(
          objects,
          owner,
          Set(
            StrategicObjectFamily.CounterplayAxis,
            StrategicObjectFamily.AccessNetwork,
            StrategicObjectFamily.PasserComplex,
            StrategicObjectFamily.InitiativeWindow,
            StrategicObjectFamily.TensionState
          )
        )
      val rivalRaceObjects =
        familyObjects(
          objects,
          !owner,
          Set(
            StrategicObjectFamily.CounterplayAxis,
            StrategicObjectFamily.AccessNetwork,
            StrategicObjectFamily.PasserComplex,
            StrategicObjectFamily.InitiativeWindow,
            StrategicObjectFamily.TensionState
          )
        )
      val linkedRivals =
        uniqueById(
          ownRaceObjects.flatMap(obj =>
            relatedObjects(
              obj,
              objects,
              Set(StrategicRelationOperator.RacesWith, StrategicRelationOperator.OverloadsOrUndermines, StrategicRelationOperator.Denies),
              Set(
                StrategicObjectFamily.CounterplayAxis,
                StrategicObjectFamily.AccessNetwork,
                StrategicObjectFamily.PasserComplex,
                StrategicObjectFamily.InitiativeWindow,
                StrategicObjectFamily.TensionState
              )
            )
          ).filter(_.owner == !owner)
        )
      val raceFamilyMatches =
        rivalRaceObjects.filter(rival => ownRaceObjects.exists(obj => obj.family == rival.family))
      val actualRivals =
        uniqueById(rivalRaceObjects.filter(rival => ownRaceObjects.exists(overlapsObject(rival, _))) ++ raceFamilyMatches ++ linkedRivals)
      val ownPressureSquares = distinctSquares(ownRaceObjects.flatMap(objectSquares))
      val rivalPressureSquares = distinctSquares(actualRivals.flatMap(objectSquares))
      val sharedSquares = distinctSquares(ownPressureSquares.intersect(rivalPressureSquares))
      val planRaceCore = Set(
        StrategicObjectFamily.CounterplayAxis,
        StrategicObjectFamily.AccessNetwork,
        StrategicObjectFamily.PasserComplex,
        StrategicObjectFamily.InitiativeWindow
      )
      val typedRaceOverlap =
        ownRaceObjects.map(_.family).toSet.intersect(actualRivals.map(_.family).toSet).intersect(planRaceCore).size
      Option.when(
        contractAllows(
          StrategicObjectFamily.PlanRace,
          FamilyGenerationEvidence(
            primitiveKinds =
              ownRaceObjects.flatMap(_.supportingPrimitives.map(_.kind)).toSet ++
                actualRivals.flatMap(_.supportingPrimitives.map(_.kind)),
            sourceFamilies = ownRaceObjects.map(_.family).toSet,
            rivalFamilies = actualRivals.map(_.family).toSet,
            relationOperators = relationOperatorsBetween(ownRaceObjects, actualRivals.map(_.id).toSet),
            anchorSquares = sharedSquares.toSet,
            contestedSquares = sharedSquares.toSet,
            files = (ownRaceObjects.flatMap(objectFiles) ++ actualRivals.flatMap(objectFiles)).toSet,
            metrics =
              FamilyGenerationMetrics(
                sourceCount = ownRaceObjects.size,
                rivalCount = actualRivals.size,
                sharedAnchorCount = sharedSquares.size,
                contestedOverlapCount = sharedSquares.size,
                typedOverlapCount = typedRaceOverlap,
                pressureSquareCount = ownPressureSquares.size + rivalPressureSquares.size,
                goalWitnessCount =
                  Option.when(ownPressureSquares.nonEmpty)(1).getOrElse(0) +
                    Option.when(rivalPressureSquares.nonEmpty)(1).getOrElse(0)
              )
          )
        )
      ) {
        val files = distinctFiles(ownRaceObjects.flatMap(objectFiles) ++ actualRivals.flatMap(objectFiles))
        val locusSquares = distinctSquares(ownPressureSquares ++ rivalPressureSquares)
        val supportingPrimitives =
          combinedPrimitives(
            uniqueById(ownRaceObjects ++ actualRivals),
            refs,
            locusSquares.toSet,
            files.toSet,
            Set(PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.AccessRoute, PrimitiveKind.PasserSeed, PrimitiveKind.TensionContactSeed, PrimitiveKind.ReleaseCandidate)
          )
        val supportingPieces = combinedPieces(ownRaceObjects, supportingPrimitives)
        mkObject(
          id = objectId(StrategicObjectFamily.PlanRace, owner, Some(dominantSector(locusSquares, files)), ownPressureSquares.headOption.orElse(rivalPressureSquares.headOption), files.toSet),
          family = StrategicObjectFamily.PlanRace,
          owner = owner,
          locus = StrategicObjectLocus(squares = locusSquares, files = files),
          sector = Some(dominantSector(locusSquares, files)),
          anchors =
            primarySquareAnchors(ownPressureSquares) ++
              pressureSquareAnchors(rivalPressureSquares) ++
              supportSquareAnchors(sharedSquares) ++
              pieceAnchors(supportingPieces, StrategicAnchorRole.Support),
          profile =
            StrategicObjectProfile.PlanRace(
              rivalOwner = !owner,
              raceSquares = locusSquares,
              raceFiles = files.toSet,
              ownGoalSquares = ownPressureSquares,
              rivalGoalSquares = rivalPressureSquares,
              features =
                Set.newBuilder[PlanRaceFeature]
                  .addAll(Option.when(ownRaceObjects.exists(_.family == StrategicObjectFamily.CounterplayAxis) && actualRivals.exists(_.family == StrategicObjectFamily.CounterplayAxis))(PlanRaceFeature.BilateralCounterplay))
                  .addAll(Option.when(ownRaceObjects.exists(_.family == StrategicObjectFamily.AccessNetwork) && actualRivals.exists(_.family == StrategicObjectFamily.AccessNetwork))(PlanRaceFeature.BilateralAccess))
                  .addAll(Option.when(ownRaceObjects.exists(_.family == StrategicObjectFamily.PasserComplex) && actualRivals.exists(_.family == StrategicObjectFamily.PasserComplex))(PlanRaceFeature.BilateralPassers))
                  .addAll(Option.when(ownRaceObjects.exists(_.family == StrategicObjectFamily.InitiativeWindow) && actualRivals.exists(_.family == StrategicObjectFamily.InitiativeWindow))(PlanRaceFeature.BilateralInitiative))
                  .addAll(Option.when(ownRaceObjects.exists(_.family == StrategicObjectFamily.TensionState) || actualRivals.exists(_.family == StrategicObjectFamily.TensionState))(PlanRaceFeature.SharedTension))
                  .result()
            ),
          supportingPrimitives = supportingPrimitives,
          supportingPieces = supportingPieces,
          rivals =
            combinedRivals(
              actualRivals,
              refs,
              !owner,
              locusSquares.toSet,
              files.toSet,
              Set(PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.AccessRoute, PrimitiveKind.PasserSeed, PrimitiveKind.TensionContactSeed)
            ),
            horizonClass = ObjectHorizonClass.Operational,
            supportBalance = ownRaceObjects.size,
            pressureBalance = actualRivals.size,
            mobilityGain = 0,
            tags = Set(StrategicEvidenceTag.Race)
          )
      }
    }

  private def transitionBridgeObjects(
      refs: List[PrimitiveReference],
      objects: List[StrategicObject]
  ): List[StrategicObject] =
    Owners.flatMap { owner =>
      val structure = familyObjects(objects, owner, Set(StrategicObjectFamily.PawnStructureRegime))
      val bridgeSources =
        familyObjects(
          objects,
          owner,
          Set(
            StrategicObjectFamily.PasserComplex,
            StrategicObjectFamily.TradeInvariant,
            StrategicObjectFamily.AccessNetwork,
            StrategicObjectFamily.ConversionFunnel
          )
        )
      val connectedSources =
        bridgeSources.filter(source =>
          structure.exists(overlapsObject(source, _)) ||
            relatedObjects(
              source,
              objects,
              Set(StrategicRelationOperator.Preserves, StrategicRelationOperator.TransformsTo),
              Set(StrategicObjectFamily.ConversionFunnel, StrategicObjectFamily.PasserComplex, StrategicObjectFamily.TradeInvariant)
            ).nonEmpty
        )
      val destinationObjects =
        familyObjects(objects, owner, Set(StrategicObjectFamily.PasserComplex, StrategicObjectFamily.ConversionFunnel))
      val effectiveDestinationObjects =
        if destinationObjects.nonEmpty then destinationObjects
        else connectedSources.filter(obj =>
          obj.family == StrategicObjectFamily.PasserComplex || obj.family == StrategicObjectFamily.ConversionFunnel
        )
      val destinationSquares = distinctSquares(effectiveDestinationObjects.flatMap(objectSquares))
      val transitionSharedSquares =
        distinctSquares(connectedSources.flatMap(source => objectSquares(source).intersect(destinationSquares)))
      Option.when(
        contractAllows(
          StrategicObjectFamily.TransitionBridge,
          FamilyGenerationEvidence(
            primitiveKinds =
              (structure ++ connectedSources ++ effectiveDestinationObjects).flatMap(_.supportingPrimitives.map(_.kind)).toSet,
            sourceFamilies = (structure ++ connectedSources).map(_.family).toSet,
            destinationFamilies = effectiveDestinationObjects.map(_.family).toSet,
            relationOperators = relationOperatorsBetween(connectedSources, effectiveDestinationObjects.map(_.id).toSet),
            anchorSquares = transitionSharedSquares.toSet,
            contestedSquares = transitionSharedSquares.toSet,
            files = (structure.flatMap(objectFiles) ++ connectedSources.flatMap(objectFiles) ++ effectiveDestinationObjects.flatMap(objectFiles)).toSet,
            metrics =
              FamilyGenerationMetrics(
                sourceCount = structure.size + connectedSources.size,
                sharedAnchorCount = transitionSharedSquares.size,
                contestedOverlapCount = transitionSharedSquares.size,
                pressureSquareCount = destinationSquares.size
              )
          )
        )
      ) {
        val continuitySquares = distinctSquares(structure.flatMap(objectSquares) ++ connectedSources.flatMap(objectSquares))
        val files = distinctFiles(structure.flatMap(objectFiles) ++ connectedSources.flatMap(objectFiles))
        val supportingPrimitives =
          combinedPrimitives(
            uniqueById(structure ++ connectedSources),
            refs,
            continuitySquares.toSet,
            files.toSet,
            Set(PrimitiveKind.BreakCandidate, PrimitiveKind.TargetSquare, PrimitiveKind.PasserSeed, PrimitiveKind.ReleaseCandidate, PrimitiveKind.AccessRoute)
          )
        val supportingPieces = combinedPieces(structure ++ connectedSources, supportingPrimitives)
        mkObject(
          id = objectId(StrategicObjectFamily.TransitionBridge, owner, Some(dominantSector(continuitySquares, files)), continuitySquares.headOption, files.toSet),
          family = StrategicObjectFamily.TransitionBridge,
          owner = owner,
          locus = StrategicObjectLocus(squares = continuitySquares ++ destinationSquares, files = files),
          sector = Some(dominantSector(continuitySquares, files)),
          anchors =
            primarySquareAnchors(continuitySquares) ++
              exitSquareAnchors(destinationSquares) ++
              fileAnchors(files, StrategicAnchorRole.Secondary) ++
              pieceAnchors(supportingPieces, StrategicAnchorRole.Support),
          profile =
            StrategicObjectProfile.TransitionBridge(
              bridgeSquares = distinctSquares(continuitySquares ++ destinationSquares),
              bridgeFiles = files.toSet,
              sourceFamilies = (structure ++ connectedSources).map(_.family).toSet,
              destinationFamilies = effectiveDestinationObjects.map(_.family).toSet,
              features =
                Set.newBuilder[TransitionBridgeFeature]
                  .addAll(Option.when(structure.nonEmpty)(TransitionBridgeFeature.StructureBridge))
                  .addAll(Option.when(connectedSources.exists(_.family == StrategicObjectFamily.TradeInvariant))(TransitionBridgeFeature.TradeBridge))
                  .addAll(Option.when(connectedSources.exists(_.family == StrategicObjectFamily.AccessNetwork))(TransitionBridgeFeature.AccessBridge))
                  .addAll(Option.when(connectedSources.exists(_.family == StrategicObjectFamily.ConversionFunnel))(TransitionBridgeFeature.ConversionBridge))
                  .addAll(Option.when(connectedSources.exists(_.family == StrategicObjectFamily.PasserComplex) || destinationSquares.nonEmpty)(TransitionBridgeFeature.PasserBridge))
                  .result()
            ),
          supportingPrimitives = supportingPrimitives,
          supportingPieces = supportingPieces,
          rivals =
            combinedRivals(
              familyObjects(objects, !owner, Set(StrategicObjectFamily.CounterplayAxis, StrategicObjectFamily.RestrictionShell, StrategicObjectFamily.PlanRace))
                .filter(obj => overlapsGroup(obj, continuitySquares.toSet, files.toSet, None)),
              refs,
              !owner,
              continuitySquares.toSet,
              files.toSet,
              Set(PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.ReleaseCandidate, PrimitiveKind.DefendedResource)
            ),
          horizonClass = ObjectHorizonClass.Structural,
          supportBalance = structure.size + connectedSources.size,
          pressureBalance = destinationSquares.size,
          mobilityGain = 0,
          tags = Set(StrategicEvidenceTag.Transition)
        )
      }
    }

  private def familyObjects(
      objects: List[StrategicObject],
      owner: Color,
      families: Set[StrategicObjectFamily]
  ): List[StrategicObject] =
    objects.filter(obj => obj.owner == owner && families.contains(obj.family))

  private def relatedObjects(
      source: StrategicObject,
      objects: List[StrategicObject],
      operators: Set[StrategicRelationOperator],
      families: Set[StrategicObjectFamily]
  ): List[StrategicObject] =
    source.relations.flatMap { relation =>
      Option.when(operators.contains(relation.operator) && families.contains(relation.target.family)) {
        objects.find(_.id == relation.target.objectId)
      }.flatten
    }

  private def combinedPrimitives(
      sourceObjects: List[StrategicObject],
      refs: List[PrimitiveReference],
      squares: Set[Square],
      files: Set[File],
      kinds: Set[PrimitiveKind]
  ): List[PrimitiveReference] =
    (sourceObjects.flatMap(_.supportingPrimitives) ++ selectRefs(refs, None, squares, files, kinds)).map(_.normalized).distinct

  private def combinedPieces(
      sourceObjects: List[StrategicObject],
      supportingPrimitives: List[PrimitiveReference]
  ): List[StrategicPieceRef] =
    mergePieces(
      sourceObjects.flatMap(_.supportingPieces) ++ sourceObjects.flatMap(_.anchors.flatMap(_.piece)),
      supportingPrimitives
    )

  private def combinedRivals(
      rivalObjects: List[StrategicObject],
      refs: List[PrimitiveReference],
      owner: Color,
      squares: Set[Square],
      files: Set[File],
      kinds: Set[PrimitiveKind]
  ): List[StrategicRivalReference] =
    (rivalObjects.map(rivalReferenceFromObject) ++ rivalsFrom(refs, owner, squares, files, kinds)).map(_.normalized).distinct

  private def rivalReferenceFromObject(obj: StrategicObject): StrategicRivalReference =
    StrategicRivalReference(
      kind = RivalReferenceKind.Object,
      owner = obj.owner,
      squares = objectSquares(obj),
      file = Option.when(objectFiles(obj).size == 1)(objectFiles(obj).head),
      roles = obj.supportingPieces.flatMap(_.roles).toSet,
      objectId = Some(obj.id),
      objectFamily = Some(obj.family)
    )

  private def overlapsObject(left: StrategicObject, right: StrategicObject): Boolean =
    overlapsGroup(left, objectSquares(right).toSet, objectFiles(right).toSet, Some(right.sector))

  private def overlapsGroup(
      obj: StrategicObject,
      squares: Set[Square],
      files: Set[File],
      sector: Option[ObjectSector]
  ): Boolean =
    objectSquares(obj).exists(squares.contains) ||
      objectFiles(obj).exists(files.contains) ||
      sector.contains(obj.sector)

  private def objectSquares(obj: StrategicObject): List[Square] =
    distinctSquares(
      obj.locus.allSquares ++
        obj.anchors.flatMap(_.squares) ++
        obj.anchors.flatMap(_.route.toList.flatMap(_.allSquares))
    )

  private def objectFiles(obj: StrategicObject): List[File] =
    distinctFiles(obj.locus.files ++ obj.anchors.flatMap(_.file))

  private def objectRoutes(obj: StrategicObject): List[StrategicRouteGeometry] =
    distinctRoutes(obj.locus.route.toList ++ obj.anchors.flatMap(_.route))

  private def distinctRoutes(routes: List[StrategicRouteGeometry]): List[StrategicRouteGeometry] =
    routes.map(_.normalized).distinct.sortBy(route => route.allSquares.map(_.key).mkString("-"))

  private def uniqueById(objects: List[StrategicObject]): List[StrategicObject] =
    objects.groupBy(_.id).values.map(_.head).toList

  private def relationOperatorsBetween(
      sourceObjects: List[StrategicObject],
      targetIds: Set[String]
  ): Set[StrategicRelationOperator] =
    sourceObjects.flatMap(_.relations).collect {
      case relation if targetIds.contains(relation.target.objectId) => relation.operator
    }.toSet

  private def contractAllows(
      family: StrategicObjectFamily,
      evidence: FamilyGenerationEvidence
  ): Boolean =
    StrategicObjectFamilyContract.forFamily(family).accepts(evidence)

  private def hasMaterialInvestment(truth: MoveTruthFrame): Boolean =
    truth.materialEconomics.investedMaterialCp.nonEmpty ||
      truth.materialEconomics.sacrificeKind.nonEmpty ||
      truth.materialEconomics.afterDeficit != truth.materialEconomics.beforeDeficit

  private def attachRelations(objects: List[StrategicObject]): List[StrategicObject] =
    objects.map { current =>
      current.copy(
        relations =
          objects.flatMap { other =>
            relation(current, other).map(op => StrategicRelation(op, StrategicRelationTarget(other.id, other.family, other.owner)))
          }
      )
    }

  private def relation(current: StrategicObject, other: StrategicObject): Option[StrategicRelationOperator] =
    if current.id == other.id then None
    else
      val sameOwner = current.owner == other.owner
      val opposing = current.owner != other.owner
      val sharedSquares = objectSquares(current).intersect(objectSquares(other)).nonEmpty
      val sharedFiles = objectFiles(current).intersect(objectFiles(other)).nonEmpty
      val sameSector = current.sector == other.sector
      (current.family, other.family) match
        case (StrategicObjectFamily.BreakAxis, StrategicObjectFamily.FixedTargetComplex) if sameOwner && (sharedSquares || sharedFiles) => Some(StrategicRelationOperator.Enables)
        case (StrategicObjectFamily.AccessNetwork, StrategicObjectFamily.FixedTargetComplex | StrategicObjectFamily.CriticalSquareComplex | StrategicObjectFamily.PasserComplex)
            if sameOwner && (sharedSquares || sharedFiles) => Some(StrategicRelationOperator.Enables)
        case (StrategicObjectFamily.AccessNetwork | StrategicObjectFamily.RedeploymentRoute, StrategicObjectFamily.AttackScaffold) if sameOwner && (sharedSquares || sharedFiles || sameSector) =>
          Some(StrategicRelationOperator.Enables)
        case (StrategicObjectFamily.CounterplayAxis, StrategicObjectFamily.BreakAxis | StrategicObjectFamily.InitiativeWindow) if sameOwner && (sharedSquares || sharedFiles || sameSector) =>
          Some(StrategicRelationOperator.Enables)
        case (StrategicObjectFamily.CriticalSquareComplex, StrategicObjectFamily.AttackScaffold | StrategicObjectFamily.FortressHoldingShell) if (sharedSquares || sharedFiles || sameSector) =>
          Some(StrategicRelationOperator.Enables)
        case (StrategicObjectFamily.PieceRoleFitness, StrategicObjectFamily.RedeploymentRoute) if sameOwner && sharedSquares => Some(StrategicRelationOperator.DependsOn)
        case (StrategicObjectFamily.FixedTargetComplex | StrategicObjectFamily.RestrictionShell, StrategicObjectFamily.DefenderDependencyNetwork) if opposing && (sharedSquares || sharedFiles || sameSector) =>
          Some(StrategicRelationOperator.OverloadsOrUndermines)
        case (StrategicObjectFamily.DefenderDependencyNetwork, StrategicObjectFamily.FixedTargetComplex | StrategicObjectFamily.RestrictionShell | StrategicObjectFamily.CounterplayAxis)
            if sameOwner && (sharedSquares || sharedFiles || sameSector) => Some(StrategicRelationOperator.DependsOn)
        case (StrategicObjectFamily.AttackScaffold | StrategicObjectFamily.CounterplayAxis | StrategicObjectFamily.BreakAxis, StrategicObjectFamily.InitiativeWindow) if sameOwner && (sharedSquares || sharedFiles || sameSector) =>
          Some(StrategicRelationOperator.DependsOn)
        case (StrategicObjectFamily.InitiativeWindow, StrategicObjectFamily.AttackScaffold | StrategicObjectFamily.CounterplayAxis | StrategicObjectFamily.BreakAxis | StrategicObjectFamily.AccessNetwork)
            if sameOwner && (sharedSquares || sharedFiles || sameSector) => Some(StrategicRelationOperator.DependsOn)
        case (StrategicObjectFamily.FixedTargetComplex | StrategicObjectFamily.AccessNetwork | StrategicObjectFamily.CounterplayAxis | StrategicObjectFamily.AttackScaffold, StrategicObjectFamily.MaterialInvestmentContract)
            if sameOwner && (sharedSquares || sharedFiles || sameSector) => Some(StrategicRelationOperator.DependsOn)
        case (StrategicObjectFamily.MaterialInvestmentContract, StrategicObjectFamily.FixedTargetComplex | StrategicObjectFamily.AccessNetwork | StrategicObjectFamily.CounterplayAxis | StrategicObjectFamily.AttackScaffold)
            if sameOwner && (sharedSquares || sharedFiles || sameSector) => Some(StrategicRelationOperator.DependsOn)
        case (StrategicObjectFamily.FixedTargetComplex | StrategicObjectFamily.AccessNetwork | StrategicObjectFamily.RestrictionShell | StrategicObjectFamily.PasserComplex | StrategicObjectFamily.TradeInvariant, StrategicObjectFamily.ConversionFunnel)
            if sameOwner && (sharedSquares || sharedFiles || sameSector) => Some(StrategicRelationOperator.DependsOn)
        case (StrategicObjectFamily.PawnStructureRegime | StrategicObjectFamily.TradeInvariant | StrategicObjectFamily.ConversionFunnel | StrategicObjectFamily.PasserComplex, StrategicObjectFamily.TransitionBridge)
            if sameOwner && (sharedSquares || sharedFiles || sameSector) => Some(StrategicRelationOperator.Preserves)
        case (StrategicObjectFamily.RestrictionShell, StrategicObjectFamily.CounterplayAxis) if opposing && (sharedSquares || sharedFiles) => Some(StrategicRelationOperator.Denies)
        case (StrategicObjectFamily.MobilityCage, StrategicObjectFamily.CounterplayAxis) if opposing && sharedSquares => Some(StrategicRelationOperator.Denies)
        case (StrategicObjectFamily.FortressHoldingShell, StrategicObjectFamily.PasserComplex | StrategicObjectFamily.AccessNetwork | StrategicObjectFamily.AttackScaffold) if opposing && (sharedSquares || sharedFiles || sameSector) =>
          Some(StrategicRelationOperator.Denies)
        case (StrategicObjectFamily.PawnStructureRegime, StrategicObjectFamily.BreakAxis | StrategicObjectFamily.PasserComplex) if sameOwner && (sharedSquares || sharedFiles) => Some(StrategicRelationOperator.Preserves)
        case (StrategicObjectFamily.TradeInvariant, StrategicObjectFamily.FixedTargetComplex | StrategicObjectFamily.AccessNetwork | StrategicObjectFamily.PasserComplex) if sameOwner && (sharedSquares || sharedFiles || sameSector) =>
          Some(StrategicRelationOperator.Preserves)
        case (StrategicObjectFamily.TransitionBridge, StrategicObjectFamily.ConversionFunnel | StrategicObjectFamily.PasserComplex) if sameOwner && (sharedSquares || sharedFiles || sameSector) =>
          Some(StrategicRelationOperator.TransformsTo)
        case (StrategicObjectFamily.ConversionFunnel, StrategicObjectFamily.PasserComplex | StrategicObjectFamily.TradeInvariant) if sameOwner && (sharedSquares || sharedFiles || sameSector) =>
          Some(StrategicRelationOperator.TransformsTo)
        case (StrategicObjectFamily.PasserComplex, StrategicObjectFamily.PasserComplex) if opposing => Some(StrategicRelationOperator.RacesWith)
        case (StrategicObjectFamily.PlanRace, StrategicObjectFamily.PlanRace | StrategicObjectFamily.InitiativeWindow | StrategicObjectFamily.PasserComplex) if opposing && (sharedSquares || sharedFiles || sameSector) =>
          Some(StrategicRelationOperator.RacesWith)
        case (StrategicObjectFamily.KingSafetyShell, StrategicObjectFamily.CounterplayAxis) if opposing && current.sector == other.sector => Some(StrategicRelationOperator.OverloadsOrUndermines)
        case (StrategicObjectFamily.AttackScaffold, StrategicObjectFamily.KingSafetyShell) if opposing && (sharedSquares || sharedFiles || sameSector) =>
          Some(StrategicRelationOperator.OverloadsOrUndermines)
        case (StrategicObjectFamily.TensionState, StrategicObjectFamily.CounterplayAxis | StrategicObjectFamily.BreakAxis) if opposing && (sharedSquares || sharedFiles || sameSector) =>
          Some(StrategicRelationOperator.OverloadsOrUndermines)
        case _ => None

  private def mkObject(
      id: String,
      family: StrategicObjectFamily,
      owner: Color,
      locus: StrategicObjectLocus,
      sector: Option[ObjectSector],
      anchors: List[StrategicObjectAnchor],
      profile: StrategicObjectProfile,
      supportingPrimitives: List[PrimitiveReference],
      supportingPieces: List[StrategicPieceRef],
      rivals: List[StrategicRivalReference],
      horizonClass: ObjectHorizonClass,
      supportBalance: Int,
      pressureBalance: Int,
      mobilityGain: Int,
      tags: Set[StrategicEvidenceTag]
  ): StrategicObject =
    StrategicObject(
      id = id,
      family = family,
      owner = owner,
      locus = locus.normalized,
      sector = sector.getOrElse(dominantSector(locus.allSquares, locus.files)),
      anchors = anchors.map(_.normalized).distinct,
      profile = profile,
      supportingPrimitives = supportingPrimitives.map(_.normalized).distinct,
      supportingPieces = supportingPieces.map(_.normalized).distinct,
      rivalResourcesOrObjects = rivals.map(_.normalized).distinct,
      relations = Nil,
      stateStrength =
        StrategicObjectStateStrength(
          band =
            if supportingPrimitives.size >= 5 || pressureBalance >= 2 then StrategicStrengthBand.Dominant
            else if supportingPrimitives.size >= 2 then StrategicStrengthBand.Established
            else StrategicStrengthBand.Emerging,
          coverage = supportingPrimitives.size,
          supportBalance = supportBalance,
          pressureBalance = pressureBalance
        ),
      readiness = StrategicObjectFamilyContract.forFamily(family).defaultReadiness,
      horizonClass = horizonClass,
      evidenceFootprint =
        StrategicObjectEvidenceFootprint(
          primitiveKinds = supportingPrimitives.map(_.kind).toSet,
          primitiveCount = supportingPrimitives.size,
          anchorSquares = distinctSquares(supportingPrimitives.flatMap(_.anchorSquares)),
          contestedSquares = distinctSquares(supportingPrimitives.flatMap(_.contestedSquares)),
          lanes = distinctFiles(supportingPrimitives.flatMap(_.lane)),
          supportingPieceCount = supportingPieces.size,
          rivalCount = rivals.size,
          supportBalance = supportBalance,
          pressureBalance = pressureBalance,
          mobilityGain = mobilityGain,
          tags = tags
        )
    )

  private def refsFrom(primitives: List[Primitive]): List[PrimitiveReference] =
    primitives.map(PrimitiveReference.fromPrimitive).map(_.normalized).distinct

  private def selectRefs(
      refs: List[PrimitiveReference],
      owner: Option[Color] = None,
      squares: Set[Square] = Set.empty,
      files: Set[File] = Set.empty,
      kinds: Set[PrimitiveKind] = Set.empty
  ): List[PrimitiveReference] =
    refs.filter { ref =>
      owner.forall(_ == ref.owner) &&
      (kinds.isEmpty || kinds.contains(ref.kind)) &&
      (
        squares.isEmpty && files.isEmpty ||
          ref.allSquares.exists(squares.contains) ||
          ref.lane.exists(files.contains)
      )
    }

  private def rivalsFrom(
      refs: List[PrimitiveReference],
      owner: Color,
      squares: Set[Square] = Set.empty,
      files: Set[File] = Set.empty,
      kinds: Set[PrimitiveKind] = Set.empty
  ): List[StrategicRivalReference] =
    selectRefs(refs, Some(owner), squares, files, kinds).map { ref =>
      StrategicRivalReference(
        kind = RivalReferenceKind.Primitive,
        owner = ref.owner,
        squares = ref.allSquares,
        file = ref.lane,
        roles = ref.roles,
        primitiveKind = Some(ref.kind)
      )
    }

  private def mergePieces(
      explicitPieces: List[StrategicPieceRef],
      refs: List[PrimitiveReference]
  ): List[StrategicPieceRef] =
    (explicitPieces ++ refs.flatMap { ref =>
      Option.when(ref.anchorSquares.nonEmpty && ref.roles.nonEmpty) {
        StrategicPieceRef(ref.owner, ref.anchorSquares.take(1), ref.roles)
      }
    }).distinct

  private def pieceRef(owner: Color, square: Square, role: Role): StrategicPieceRef =
    StrategicPieceRef(owner, List(square), Set(role))

  private def pieceGroup(owner: Color, squares: List[Square], roles: Set[Role]): Option[StrategicPieceRef] =
    Option.when(squares.nonEmpty)(StrategicPieceRef(owner, distinctSquares(squares), roles))

  private def pieceGroups(groups: Option[StrategicPieceRef]*): List[StrategicPieceRef] =
    groups.toList.flatten

  private def pawnPieces(owner: Color, squares: List[Square]): List[StrategicPieceRef] =
    distinctSquares(squares).map(square => pieceRef(owner, square, Pawn))

  private def primarySquareAnchors(squares: List[Square]): List[StrategicObjectAnchor] =
    squareAnchor(squares, StrategicAnchorRole.Primary).toList

  private def pressureSquareAnchors(squares: List[Square]): List[StrategicObjectAnchor] =
    squareAnchor(squares, StrategicAnchorRole.Pressure).toList

  private def constraintSquareAnchors(squares: List[Square]): List[StrategicObjectAnchor] =
    squareAnchor(squares, StrategicAnchorRole.Constraint).toList

  private def supportSquareAnchors(squares: List[Square]): List[StrategicObjectAnchor] =
    squareAnchor(squares, StrategicAnchorRole.Support).toList

  private def exitSquareAnchors(squares: List[Square]): List[StrategicObjectAnchor] =
    squareAnchor(squares, StrategicAnchorRole.Exit).toList

  private def squareAnchor(squares: List[Square], role: StrategicAnchorRole): Option[StrategicObjectAnchor] =
    Option.when(squares.nonEmpty)(StrategicObjectAnchor(StrategicAnchorKind.Square, role, squares = distinctSquares(squares)))

  private def fileAnchors(files: List[File], role: StrategicAnchorRole): List[StrategicObjectAnchor] =
    distinctFiles(files).map(file => StrategicObjectAnchor(StrategicAnchorKind.File, role, file = Some(file)))

  private def pieceAnchors(pieces: List[StrategicPieceRef], role: StrategicAnchorRole): List[StrategicObjectAnchor] =
    pieces.map(piece => StrategicObjectAnchor(StrategicAnchorKind.Piece, role, squares = piece.squares, piece = Some(piece)))

  private def routeAnchors(routes: List[StrategicRouteGeometry], role: StrategicAnchorRole): List[StrategicObjectAnchor] =
    routes.map(route => StrategicObjectAnchor(StrategicAnchorKind.Route, role, route = Some(route.normalized)))

  private def routeGeometry(seed: BreakCandidate): StrategicRouteGeometry =
    StrategicRouteGeometry(seed.sourceSquare, target = seed.breakSquare)

  private def routeGeometry(seed: DiagonalLaneSeed): StrategicRouteGeometry =
    StrategicRouteGeometry(seed.origin, target = seed.target)

  private def routeGeometry(seed: LiftCorridorSeed): StrategicRouteGeometry =
    StrategicRouteGeometry(seed.origin, List(seed.liftSquare), seed.target)

  private def routeGeometry(seed: KnightRouteSeed): StrategicRouteGeometry =
    StrategicRouteGeometry(seed.origin, List(seed.via), seed.target)

  private def routeGeometry(seed: RedeploymentPathSeed): StrategicRouteGeometry =
    StrategicRouteGeometry(seed.origin, List(seed.via), seed.target)

  private def objectId(
      family: StrategicObjectFamily,
      owner: Color,
      sector: Option[ObjectSector],
      anchor: Option[Square],
      files: Set[File],
      extra: String = ""
  ): String =
    List(
      family.toString,
      if owner.white then "white" else "black",
      sector.getOrElse(ObjectSector.Mixed).toString.toLowerCase,
      anchor.map(_.key).getOrElse("cluster"),
      distinctFiles(files.toList).map(_.char).mkString,
      extra
    ).filter(_.nonEmpty).mkString("-")

  private def sec(square: Square): Option[ObjectSector] = Some(sectorOfSquare(square))
  private def fileSec(file: File): Option[ObjectSector] = Some(sectorOfFile(file))

  private def dominantSector(squares: List[Square], files: List[File]): ObjectSector =
    val counts = (squares.map(sectorOfSquare) ++ files.map(sectorOfFile)).groupBy(identity).view.mapValues(_.size).toMap
    if counts.size >= 3 then ObjectSector.WholeBoard
    else
      counts.toList.sortBy { case (_, count) => -count } match
        case Nil => ObjectSector.Mixed
        case (_, a) :: (_, b) :: Nil if a == b => ObjectSector.Mixed
        case (sector, _) :: _                  => sector

  private def sectorOfSquare(square: Square): ObjectSector =
    sectorOfFile(square.file)

  private def sectorOfFile(file: File): ObjectSector =
    file match
      case File.A | File.B | File.C => ObjectSector.Queenside
      case File.D | File.E          => ObjectSector.Center
      case File.F | File.G | File.H => ObjectSector.Kingside

  private def isCentral(square: Square): Boolean =
    sectorOfSquare(square) == ObjectSector.Center

  private def distinctSquares(squares: List[Square]): List[Square] =
    squares.distinct.sortBy(_.key)

  private def distinctFiles(files: List[File]): List[File] =
    files.distinct.sortBy(_.char.toString)

  private def colorIndex(color: Color): Int =
    if color.white then 0 else 1
