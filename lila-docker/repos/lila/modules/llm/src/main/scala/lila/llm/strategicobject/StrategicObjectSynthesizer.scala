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
    val _ = truth
    val bank = primitives.normalized
    val refs = bank.all.map(PrimitiveReference.fromPrimitive).map(_.normalized)
    val objects =
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

    attachRelations(objects).map(_.normalized).sortBy(o => (o.family.ordinal, colorIndex(o.owner), o.id))

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
      Option.when(pressureSquares.nonEmpty) {
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
          Option.when(focus.nonEmpty) {
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
                resourceSeeds
                  .filter(seed => focus.contains(seed.square) || seed.pressureSquares.exists(focus.contains))
                  .map(seed => pieceRef(seed.owner, seed.square, seed.role)),
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
        Option.when(focus.nonEmpty && enemy.nonEmpty) {
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
      val sharedSquares = current.locus.allSquares.intersect(other.locus.allSquares).nonEmpty
      val sharedFiles = current.locus.files.intersect(other.locus.files).nonEmpty
      (current.family, other.family) match
        case (StrategicObjectFamily.BreakAxis, StrategicObjectFamily.FixedTargetComplex) if sameOwner && (sharedSquares || sharedFiles) => Some(StrategicRelationOperator.Enables)
        case (StrategicObjectFamily.AccessNetwork, StrategicObjectFamily.FixedTargetComplex | StrategicObjectFamily.CriticalSquareComplex | StrategicObjectFamily.PasserComplex)
            if sameOwner && (sharedSquares || sharedFiles) => Some(StrategicRelationOperator.Enables)
        case (StrategicObjectFamily.PieceRoleFitness, StrategicObjectFamily.RedeploymentRoute) if sameOwner && sharedSquares => Some(StrategicRelationOperator.DependsOn)
        case (StrategicObjectFamily.RestrictionShell, StrategicObjectFamily.CounterplayAxis) if opposing && (sharedSquares || sharedFiles) => Some(StrategicRelationOperator.Denies)
        case (StrategicObjectFamily.MobilityCage, StrategicObjectFamily.CounterplayAxis) if opposing && sharedSquares => Some(StrategicRelationOperator.Denies)
        case (StrategicObjectFamily.PawnStructureRegime, StrategicObjectFamily.BreakAxis | StrategicObjectFamily.PasserComplex) if sameOwner && (sharedSquares || sharedFiles) => Some(StrategicRelationOperator.Preserves)
        case (StrategicObjectFamily.PasserComplex, StrategicObjectFamily.PasserComplex) if opposing => Some(StrategicRelationOperator.RacesWith)
        case (StrategicObjectFamily.KingSafetyShell, StrategicObjectFamily.CounterplayAxis) if opposing && current.sector == other.sector => Some(StrategicRelationOperator.OverloadsOrUndermines)
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
