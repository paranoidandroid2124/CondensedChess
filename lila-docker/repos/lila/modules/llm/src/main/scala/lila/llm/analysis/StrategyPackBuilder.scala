package lila.llm.analysis

import _root_.chess.Color
import _root_.chess.{ Bishop, Board, File, Knight, Queen, Rank, Rook, Role, Square }
import _root_.chess.Bitboard
import _root_.chess.format.Fen
import lila.llm.{ DirectionalTargetReadiness, StrategyDirectionalTarget, StrategyPack, StrategyPieceMoveRef, StrategyPieceRoute, StrategySidePlan }
import lila.llm.model.{ ExtendedAnalysisData, NarrativeContext }
import lila.llm.model.authoring.PlanHypothesis
import lila.llm.model.strategic.PieceActivity
import lila.llm.model.strategic.PositionalTag

object StrategyPackBuilder:

  private val MaxPlans = 4
  private val MaxRoutes = 4
  private val MaxFocus = 6
  private val MaxEvidence = 8

  def build(
      data: ExtendedAnalysisData,
      ctx: NarrativeContext
  ): Option[StrategyPack] =
    val sideToMoveColor = if data.isWhiteToMove then Color.White else Color.Black
    val sideToMove = sideName(sideToMoveColor)
    val structureArc = StructurePlanArcBuilder.build(ctx)
    val plans = buildPlans(ctx, sideToMoveColor)
    val boardOpt =
      Fen.read(_root_.chess.variant.Standard, Fen.Full(data.fen)).map(_.board)
    val semanticContext = StrategicIdeaSemanticContext.from(data, ctx, boardOpt)
    val routes = buildRoutes(data, boardOpt, sideToMoveColor, plans, structureArc)
    val moveRefs = buildMoveRefs(data, boardOpt, sideToMoveColor, plans)
    val directionalTargets = buildDirectionalTargets(data, boardOpt, sideToMoveColor, plans)
    val longTermFocus = buildLongTermFocus(ctx, plans, routes, moveRefs, structureArc)
    val evidence = buildEvidence(ctx, routes, moveRefs, structureArc)
    val signalDigest = NarrativeSignalDigestBuilder.build(ctx)

    Option.when(plans.nonEmpty || routes.nonEmpty || moveRefs.nonEmpty || directionalTargets.nonEmpty || longTermFocus.nonEmpty) {
      val basePack =
        StrategyPack(
          sideToMove = sideToMove,
          plans = plans,
          pieceRoutes = routes,
          pieceMoveRefs = moveRefs,
          directionalTargets = directionalTargets,
          longTermFocus = longTermFocus,
          evidence = evidence,
          signalDigest = signalDigest
        )
      val enrichedPack = StrategicIdeaSelector.enrich(basePack, semanticContext)
      refreshDominantThesis(ctx, enrichedPack)
    }

  def promptHints(pack: StrategyPack): List[String] =
    val planHints = pack.plans.take(2).map { p =>
      s"${p.side} long plan: ${p.planName}"
    }
    val routeHints = pack.pieceRoutes.take(2).map { r =>
      val routeLead =
        if r.surfaceMode == lila.llm.RouteSurfaceMode.Exact then r.route.mkString("-")
        else r.route.lastOption.getOrElse(r.from)
      s"${r.ownerSide} ${r.piece} route $routeLead (${r.purpose}; ${r.surfaceMode})"
    }
    val directionalHints = pack.directionalTargets.take(2).map { target =>
      s"${target.ownerSide} ${target.piece} objective ${target.targetSquare} (${target.readiness})"
    }
    val ideaHints = pack.strategicIdeas.take(2).map { idea =>
      s"${idea.ownerSide} idea ${StrategicIdeaSelector.humanizedKind(idea.kind)} (${StrategicIdeaSelector.focusSummary(idea)})"
    }
    val moveRefHints = pack.pieceMoveRefs.take(2).map(m => s"${m.ownerSide} ${m.piece} move-ref ${m.target} (${m.idea})")
    val focusHints = pack.longTermFocus.take(2).map(v => s"long-term focus: $v")
    val digestHints =
      pack.signalDigest.toList.flatMap { digest =>
        List(
          digest.opening.map(v => s"opening: $v"),
          digest.latentPlan.map(v => s"latent plan: $v"),
          digest.authoringEvidence.map(v => s"authoring evidence: $v"),
          digest.practicalVerdict.map(v => s"practical: $v"),
          Option.when(digest.practicalFactors.nonEmpty)(s"practical factors: ${digest.practicalFactors.mkString("; ")}"),
          digest.structureProfile.map(v => s"structural profile: $v"),
          digest.alignmentBand.map(v => s"plan fit: $v"),
          digest.deploymentPiece.map { piece =>
            val route =
              if digest.deploymentRoute.nonEmpty then digest.deploymentRoute.mkString("-")
              else "n/a"
            s"deployment: $piece $route"
          },
          digest.deploymentPurpose.map(v => s"deployment purpose: $v"),
          digest.prophylaxisPlan.map(v => s"prophylaxis: $v"),
          digest.decision.map(v => s"decision: $v"),
          digest.decisionComparison.flatMap(_.engineBestMove).map(v => s"engine best: $v"),
          digest.decisionComparison.flatMap(_.deferredMove).map { v =>
            val label = if digest.decisionComparison.exists(_.practicalAlternative) then "deferred practical" else "deferred"
            s"$label: $v"
          },
          digest.opponentPlan.map(v => s"opponent plan: $v")
        ).flatten
      }
    (planHints ++ ideaHints ++ directionalHints ++ routeHints ++ moveRefHints ++ focusHints ++ digestHints)
      .map(_.trim)
      .filter(_.nonEmpty)
      .distinct
      .take(8)

  private def buildPlans(
      ctx: NarrativeContext,
      sideToMoveColor: Color
  ): List[StrategySidePlan] =
    val mover = sideName(sideToMoveColor)
    val opponent = sideName(!sideToMoveColor)

    val moverFromHypothesis =
      ctx.mainStrategicPlans
        .sortBy(h => (-h.score, h.rank))
        .map(h => fromHypothesis(mover, h))

    val moverFromPlanTable =
      ctx.plans.top5
        .sortBy(r => (r.rank, -r.score))
        .map(p => fromPlanRow(mover, p.name, p.supports ++ p.evidence, p.blockers ++ p.missingPrereqs))

    val moverPlans =
      (moverFromHypothesis ++ moverFromPlanTable)
        .distinctBy(_.planName.trim.toLowerCase)
        .take(3)

    val opponentPlan =
      ctx.opponentPlan.map { p =>
        fromPlanRow(opponent, p.name, p.supports ++ p.evidence, p.blockers ++ p.missingPrereqs)
      }

    (moverPlans ++ opponentPlan.toList).distinctBy(p => s"${p.side}|${p.planName.trim.toLowerCase}").take(MaxPlans)

  private def buildRoutes(
      data: ExtendedAnalysisData,
      boardOpt: Option[_root_.chess.Board],
      sideToMoveColor: Color,
      plans: List[StrategySidePlan],
      structureArc: Option[StructurePlanArc]
  ): List[StrategyPieceRoute] =
    val sides = List(sideToMoveColor, !sideToMoveColor)
    val sideToMove = sideName(sideToMoveColor)
    val preferredRouteKey =
      structureArc.map(arc => s"${arc.primaryDeployment.piece}|${arc.primaryDeployment.from}|${arc.primaryDeployment.route.lastOption.getOrElse(arc.primaryDeployment.from)}")

    sides
      .flatMap { routeColor =>
        val routeSide = sideName(routeColor)
        val sidePlans = plans.filter(_.side == routeSide)
        data.pieceActivity.flatMap(pa => toPieceRoute(pa, boardOpt, routeColor, data.positionalFeatures, sidePlans))
      }
      .sortBy { r =>
        val routeKey = s"${r.piece}|${r.from}|${r.route.lastOption.getOrElse(r.from)}"
        val preferred = preferredRouteKey.contains(routeKey)
        (if preferred then 0 else 1, if r.ownerSide == sideToMove then 0 else 1, -r.surfaceConfidence)
      }
      .distinctBy(r => s"${r.ownerSide}|${r.piece}|${r.from}|${r.route.lastOption.getOrElse(r.from)}")
      .take(MaxRoutes)

  private def buildMoveRefs(
      data: ExtendedAnalysisData,
      boardOpt: Option[_root_.chess.Board],
      sideToMoveColor: Color,
      plans: List[StrategySidePlan]
  ): List[StrategyPieceMoveRef] =
    val sides = List(sideToMoveColor, !sideToMoveColor)
    val sideToMove = sideName(sideToMoveColor)
    sides
      .flatMap { routeColor =>
        val routeSide = sideName(routeColor)
        val sidePlans = plans.filter(_.side == routeSide)
        data.pieceActivity.flatMap(pa => toPieceMoveRefs(pa, boardOpt, routeColor, sidePlans))
      }
      .sortBy(ref => (if ref.ownerSide == sideToMove then 0 else 1, ref.piece, ref.target))
      .distinctBy(ref => s"${ref.ownerSide}|${ref.piece}|${ref.from}|${ref.target}")
      .take(MaxRoutes)

  private def buildDirectionalTargets(
      data: ExtendedAnalysisData,
      boardOpt: Option[Board],
      sideToMoveColor: Color,
      plans: List[StrategySidePlan]
  ): List[StrategyDirectionalTarget] =
    val sides = List(sideToMoveColor, !sideToMoveColor)
    val sideToMove = sideName(sideToMoveColor)
    sides
      .flatMap { routeColor =>
        val routeSide = sideName(routeColor)
        val sidePlans = plans.filter(_.side == routeSide)
        data.pieceActivity.flatMap(pa => toDirectionalTargets(pa, boardOpt, routeColor, data.positionalFeatures, sidePlans))
      }
      .sortBy(target => (if target.ownerSide == sideToMove then 0 else 1, readinessRank(target.readiness), target.targetSquare, target.piece))
      .distinctBy(target => s"${target.ownerSide}|${target.piece}|${target.from}|${target.targetSquare}")
      .take(MaxRoutes)

  private def toPieceRoute(
      pa: PieceActivity,
      boardOpt: Option[_root_.chess.Board],
      routeColor: Color,
      positionalFeatures: List[PositionalTag],
      plans: List[StrategySidePlan]
  ): Option[StrategyPieceRoute] =
    StructurePlanArcBuilder
      .cueFromStrategicActivity(
        activity = pa,
        boardOpt = boardOpt,
        routeColor = routeColor,
        positionalFeatures = positionalFeatures,
        planLabels = plans.map(_.planName)
      )
      .map { cue =>
        StrategyPieceRoute(
          ownerSide = sideName(routeColor),
          piece = cue.piece,
          from = cue.from,
          route = cue.routeSquares,
          purpose = cue.purpose,
          strategicFit = cue.strategicFit,
          tacticalSafety = cue.tacticalSafety,
          surfaceConfidence = cue.surfaceConfidence,
          surfaceMode = cue.surfaceMode,
          evidence = StructurePlanArcBuilder.evidenceFromStrategicActivity(pa, cue.lastSquare.getOrElse(cue.from))
        )
      }

  private def toPieceMoveRefs(
      pa: PieceActivity,
      boardOpt: Option[_root_.chess.Board],
      routeColor: Color,
      plans: List[StrategySidePlan]
  ): List[StrategyPieceMoveRef] =
    boardOpt.toList.flatMap { board =>
      board
        .pieceAt(pa.square)
        .filter(_.color == routeColor)
        .toList
        .flatMap { piece =>
          pa.concreteTargets
            .map(_.key)
            .distinct
            .flatMap { target =>
              board.pieceAt(_root_.chess.Square.all.find(_.key == target).getOrElse(pa.square))
                .filter(_.color != routeColor)
                .map { victim =>
                  val idea = concreteIdea(piece.role, victim.role, target, plans.map(_.planName))
                  StrategyPieceMoveRef(
                    ownerSide = sideName(routeColor),
                    piece = pieceToken(piece.role),
                    from = pa.square.key,
                    target = target,
                    idea = idea,
                    tacticalTheme = Some("capture_or_exchange"),
                    evidence = List("enemy_occupied_endpoint", s"target_${victim.role.name.toLowerCase}", "piece_activity")
                  )
                }
            }
            .take(2)
        }
    }

  private def toDirectionalTargets(
      pa: PieceActivity,
      boardOpt: Option[Board],
      routeColor: Color,
      positionalFeatures: List[PositionalTag],
      plans: List[StrategySidePlan]
  ): List[StrategyDirectionalTarget] =
    boardOpt.toList.flatMap { board =>
      board
        .pieceAt(pa.square)
        .filter(_.color == routeColor)
        .toList
        .flatMap { piece =>
          pa.directionalTargets
            .map(_.key)
            .distinct
            .flatMap { target =>
              Square.all
                .find(_.key == target)
                .filter(sq => board.pieceAt(sq).isEmpty && !pa.keyRoutes.lastOption.contains(sq))
                .map { targetSquare =>
                  val readiness = directionalTargetReadiness(board, piece.role, pa, targetSquare, routeColor)
                  val reasons = directionalTargetReasons(board, piece.role, routeColor, targetSquare, plans.map(_.planName), positionalFeatures)
                  val prerequisites = directionalTargetPrerequisites(readiness, piece.role, targetSquare, routeColor)
                  StrategyDirectionalTarget(
                    targetId = s"target_${sideName(routeColor)}_${pieceToken(piece.role).toLowerCase}_${pa.square.key}_${targetSquare.key}",
                    ownerSide = sideName(routeColor),
                    piece = pieceToken(piece.role),
                    from = pa.square.key,
                    targetSquare = targetSquare.key,
                    readiness = readiness,
                    strategicReasons = reasons.take(3),
                    prerequisites = prerequisites.take(3),
                    evidence = List(
                      Some("directional_target"),
                      Some(s"readiness_$readiness"),
                      Option.when(pa.isTrapped)("trapped_piece_signal"),
                      Option.when(pa.mobilityScore < 0.4)("low_mobility_signal"),
                      Option.when(pa.coordinationLinks.contains(targetSquare))("coordination_link"),
                      Some("piece_activity")
                    ).flatten
                  )
                }
            }
            .take(2)
        }
    }

  private def fromHypothesis(side: String, h: PlanHypothesis): StrategySidePlan =
    StrategySidePlan(
      side = side,
      horizon = if h.score >= 0.68 then "long" else "medium",
      planName = h.planName,
      priorities = h.executionSteps.take(3),
      riskTriggers = h.failureModes.take(3)
    )

  private def fromPlanRow(
      side: String,
      name: String,
      priorities: List[String],
      risks: List[String]
  ): StrategySidePlan =
    StrategySidePlan(
      side = side,
      horizon = "medium",
      planName = name,
      priorities = priorities.take(3),
      riskTriggers = risks.take(3)
    )

  private def buildLongTermFocus(
      ctx: NarrativeContext,
      plans: List[StrategySidePlan],
      routes: List[StrategyPieceRoute],
      moveRefs: List[StrategyPieceMoveRef],
      structureArc: Option[StructurePlanArc]
  ): List[String] =
    val scored = scala.collection.mutable.HashMap.empty[String, Double]
    val dominantThesis = StrategicThesisBuilder.build(ctx).map(_.claim).map(_.trim).filter(_.nonEmpty)
    val planNameKeys = plans.map(_.planName.trim.toLowerCase).toSet

    def lowered(raw: String): String = raw.trim.toLowerCase

    def push(raw: String, weight: Double): Unit =
      val clean = raw.trim
      if clean.nonEmpty then
        val key = clean.toLowerCase
        val existing = scored.getOrElse(key, Double.MinValue)
        if weight > existing then scored.update(key, weight)

    plans.take(3).zipWithIndex.foreach { case (plan, idx) =>
      val baseWeight = 2.30 - (idx * 0.20)
      push(s"${plan.side} plan: ${plan.planName}", baseWeight)
      plan.priorities.take(2).zipWithIndex.foreach { case (priority, i) =>
        push(s"${plan.side} execution: ${priority}", baseWeight - 0.35 - (i * 0.10))
      }
      plan.riskTriggers.take(1).foreach { risk =>
        push(s"${plan.side} risk trigger: ${risk}", baseWeight - 0.65)
      }
    }

    ctx.mainStrategicPlans.take(3).foreach { hypothesis =>
      val nameKey = hypothesis.planName.trim.toLowerCase
      if !planNameKeys.contains(nameKey) then
        push(s"hypothesis: ${hypothesis.planName}", 1.55 + hypothesis.score * 0.25)
        hypothesis.executionSteps.take(1).foreach(step => push(s"follow-up: $step", 1.35 + hypothesis.score * 0.20))
    }

    ctx.planContinuity.foreach { continuity =>
      val phase = continuity.phase.toString.toLowerCase
      push(
        s"continuity: ${continuity.planName} (${continuity.consecutivePlies} plies, phase $phase)",
        1.70 + continuity.commitmentScore * 0.30
      )
    }

    routes.take(2).foreach { route =>
      push(
        s"${route.ownerSide} ${route.piece} route ${route.route.mkString("-")} for ${route.purpose}",
        1.95 + route.surfaceConfidence * 0.35
      )
    }

    val centeredOpenFileRoutes =
      routes.count { route =>
        lowered(route.purpose) match
          case purpose if
              List("open-file occupation", "file occupation", "file pressure", "open file").exists(purpose.contains) =>
            route.route.lastOption.exists { square =>
              lowered(square).headOption.exists(ch => ch == 'd' || ch == 'e')
            }
          case _ => false
      }
    val repeatedTargetPawnRefs =
      moveRefs.count { moveRef =>
        val loweredIdea = lowered(moveRef.idea)
        moveRef.evidence.map(lowered).contains("target_pawn") ||
        loweredIdea.contains("contest the pawn")
      }
    val fixedTargetPlanSignal =
      ctx.mainStrategicPlans.exists { hypothesis =>
        val loweredPlanName = lowered(hypothesis.planName)
        loweredPlanName.contains("attacking fixed pawn") ||
        hypothesis.evidenceSources.exists { source =>
          val loweredSource = lowered(source)
          loweredSource.contains("fixed pawn") ||
          loweredSource.contains("backward pawn") ||
          loweredSource.contains("weakness_fixation")
        }
      }
    if
      (centeredOpenFileRoutes >= 2 && repeatedTargetPawnRefs >= 2) ||
      (fixedTargetPlanSignal && centeredOpenFileRoutes >= 1 && repeatedTargetPawnRefs >= 1)
    then
      push("keep the fixed central targets under pressure before recovering material", 2.38)

    structureArc.foreach { arc =>
      push(s"structure deployment: ${arc.structureLabel} asks for ${StructurePlanArcBuilder.focusLine(arc)}", 2.18)
      push(s"move contribution: ${arc.moveContribution}", 1.92)
    }
    dominantThesis.foreach(thesis => push(s"dominant thesis: $thesis", 2.42))

    ctx.semantic.foreach { semantic =>
      semantic.conceptSummary.take(4).zipWithIndex.foreach { case (concept, idx) =>
        push(concept, 1.10 - (idx * 0.05))
      }
      semantic.planAlignment.foreach { alignment =>
        push(s"plan alignment: ${alignment.band}", 1.25 + (alignment.score.toDouble / 100.0))
        alignment.narrativeIntent.foreach(intent => push(s"alignment intent: $intent", 1.20))
      }
    }

    scored.toList
      .sortBy { case (text, weight) => (-weight, text) }
      .map(_._1)
      .take(MaxFocus)

  private def buildEvidence(
      ctx: NarrativeContext,
      routes: List[StrategyPieceRoute],
      moveRefs: List[StrategyPieceMoveRef],
      structureArc: Option[StructurePlanArc]
  ): List[String] =
    val dominantThesis = StrategicThesisBuilder.build(ctx).map(_.claim).map(_.trim).filter(_.nonEmpty)
    val routeEvidence =
      routes.flatMap(route => route.evidence.map(signal => s"route:${route.ownerSide}:$signal"))
    val moveRefEvidence =
      moveRefs.flatMap(ref => ref.evidence.map(signal => s"move_ref:${ref.ownerSide}:$signal"))
    val structureEvidence =
      structureArc.toList.flatMap { arc =>
        List(
          Some(s"structure_arc:${arc.structureLabel}:${arc.planLabel}"),
          Some(s"deployment:${arc.primaryDeployment.piece}:${arc.primaryDeployment.routeSquares.mkString("-")}:${arc.primaryDeployment.purpose}"),
          Some(s"deployment_contribution:${arc.moveContribution}")
        ).flatten
        }
    (
        ctx.mainStrategicPlans.flatMap(_.evidenceSources) ++
        routeEvidence ++
        moveRefEvidence ++
        structureEvidence ++
        dominantThesis.toList.map(v => s"dominant_thesis:$v")
    ).map(_.trim).filter(_.nonEmpty).distinct.take(MaxEvidence)

  private def refreshDominantThesis(
      ctx: NarrativeContext,
      pack: StrategyPack
  ): StrategyPack =
    val surface = StrategyPackSurface.from(Some(pack))
    val normalizedFocusLead = surface.normalizedLongTermFocusText.map(_.trim).filter(_.nonEmpty)
    StrategicThesisBuilder
      .build(ctx, Some(pack))
      .map(_.claim.trim)
      .filter(_.nonEmpty)
      .map { thesis =>
        val refreshedFocus =
          (
            normalizedFocusLead.toList ++
              List(s"dominant thesis: $thesis") ++
              pack.longTermFocus.filterNot { focus =>
                val lowered = focus.trim.toLowerCase
                lowered.startsWith("dominant thesis:") ||
                normalizedFocusLead.exists(_.equalsIgnoreCase(focus.trim))
              }
          )
            .map(_.trim)
            .filter(_.nonEmpty)
            .distinct
            .take(MaxFocus)
        val refreshedEvidence =
          (s"dominant_thesis:$thesis" :: pack.evidence.filterNot(_.trim.toLowerCase.startsWith("dominant_thesis:")))
            .map(_.trim)
            .filter(_.nonEmpty)
            .distinct
            .take(MaxEvidence)
        pack.copy(longTermFocus = refreshedFocus, evidence = refreshedEvidence)
      }
      .getOrElse(pack)

  private def sideName(color: Color): String =
    if color.white then "white" else "black"

  private def pieceToken(role: _root_.chess.Role): String =
    role match
      case _root_.chess.Pawn   => "P"
      case _root_.chess.Knight => "N"
      case _root_.chess.Bishop => "B"
      case _root_.chess.Rook   => "R"
      case _root_.chess.Queen  => "Q"
      case _root_.chess.King   => "K"

  private def concreteIdea(
      attacker: _root_.chess.Role,
      victim: _root_.chess.Role,
      target: String,
      planLabels: List[String]
  ): String =
    val attackerName = attacker.name.toLowerCase
    val victimName = victim.name.toLowerCase
    val planHint = planLabels.headOption.map(_.trim.toLowerCase).getOrElse("")
    if planHint.contains("attack") then s"keep the $attackerName pointed at $target"
    else if planHint.contains("file") && attacker == _root_.chess.Rook then s"contest $target along the file"
    else s"contest the $victimName on $target"

  private def directionalTargetReadiness(
      board: Board,
      role: Role,
      activity: PieceActivity,
      target: Square,
      color: Color
  ): String =
    val routeOpt = shortestRedeploymentRoute(role, activity.square, target, board, color, maxDepth = if role == Knight then 4 else 3)
    val defenders = board.attackers(target, color).count
    val attackers = board.attackers(target, !color).count
    if routeOpt.isEmpty then DirectionalTargetReadiness.Blocked
    else if attackers >= defenders && attackers > 0 then DirectionalTargetReadiness.Contested
    else if activity.isTrapped || activity.mobilityScore < 0.30 || routeOpt.exists(_.size > (if role == Knight then 3 else 2)) then
      DirectionalTargetReadiness.Premature
    else DirectionalTargetReadiness.Build

  private def directionalTargetReasons(
      board: Board,
      role: Role,
      color: Color,
      target: Square,
      planLabels: List[String],
      positionalFeatures: List[PositionalTag]
  ): List[String] =
    val planHint =
      planLabels.headOption.map(_.trim.toLowerCase).filter(_.nonEmpty).map(plan => s"supports $plan")
    val central =
      Option.when(Set(File.C, File.D, File.E, File.F).contains(target.file) && Set(Rank.Third, Rank.Fourth, Rank.Fifth, Rank.Sixth).contains(target.rank))(
        "central foothold"
      )
    val outpost =
      Option.when(role == Knight && isOutpostSquare(board, target, color))("outpost pressure")
    val lineAccess =
      Option.when((role == Rook || role == Queen) && (isOpenFile(board, target.file) || isSemiOpenFileFor(board, target.file, color)))(
        "line access"
      )
    val structuralTag =
      positionalFeatures.collectFirst {
        case PositionalTag.Outpost(square, owner) if square == target && owner == color => "outpost anchor"
        case PositionalTag.WeakSquare(square, owner) if square == target && owner != color => "pressure on a fixed weakness"
        case PositionalTag.OpenFile(file, owner) if file == target.file && owner == color => "open-file pressure"
      }
    (List(planHint, central, outpost, lineAccess, structuralTag, Some("improves piece placement"))).flatten.distinct

  private def directionalTargetPrerequisites(
      readiness: String,
      role: Role,
      target: Square,
      color: Color
  ): List[String] =
    readiness match
      case DirectionalTargetReadiness.Blocked =>
        List("clear the route first", s"make ${target.key} available before committing the ${role.name.toLowerCase}")
      case DirectionalTargetReadiness.Contested =>
        List(s"add more cover to ${target.key}", "reduce the current contest on the target square")
      case DirectionalTargetReadiness.Premature =>
        List("finish the preparatory regrouping first", zoneObjective(target, color))
      case _ =>
        List(zoneObjective(target, color), s"improve access to ${target.key}")

  private def zoneObjective(target: Square, color: Color): String =
    if target.rank == (if color.white then Rank.Sixth else Rank.Third) then "prepare the forward entry squares"
    else "prepare the supporting squares first"

  private def readinessRank(readiness: String): Int =
    readiness match
      case DirectionalTargetReadiness.Build     => 0
      case DirectionalTargetReadiness.Contested => 1
      case DirectionalTargetReadiness.Premature => 2
      case DirectionalTargetReadiness.Blocked   => 3
      case _                                    => 4

  private def shortestRedeploymentRoute(
      role: Role,
      from: Square,
      to: Square,
      board: Board,
      color: Color,
      maxDepth: Int
  ): Option[List[Square]] =
    val queue = scala.collection.mutable.Queue((from, List(from)))
    val visited = scala.collection.mutable.Set(from)
    var found: Option[List[Square]] = None

    while queue.nonEmpty && found.isEmpty do
      val (curr, path) = queue.dequeue()
      val depth = path.size - 1
      if depth < maxDepth then
        val nextSquares =
          pseudoMoves(role, curr, board.occupied).squares.filter { sq =>
            sq == to || board.pieceAt(sq).isEmpty
          }
        nextSquares.foreach { sq =>
          val nextPath = path :+ sq
          if sq == to && found.isEmpty then found = Some(nextPath)
          if found.isEmpty && !visited.contains(sq) then
            visited += sq
            queue.enqueue((sq, nextPath))
        }
    found

  private def pseudoMoves(role: Role, sq: Square, occupied: Bitboard): Bitboard =
    role match
      case Knight => sq.knightAttacks
      case Bishop => sq.bishopAttacks(occupied)
      case Rook   => sq.rookAttacks(occupied)
      case Queen  => sq.queenAttacks(occupied)
      case _      => Bitboard.empty

  private def isOutpostSquare(board: Board, sq: Square, color: Color): Boolean =
    val supportedByPawn = board.attackers(sq, color).intersects(board.byPiece(color, _root_.chess.Pawn))
    val attackedByEnemyPawn = board.attackers(sq, !color).intersects(board.byPiece(!color, _root_.chess.Pawn))
    supportedByPawn && !attackedByEnemyPawn

  private def isOpenFile(board: Board, file: File): Boolean =
    (board.pawns & Bitboard.file(file)).isEmpty

  private def isSemiOpenFileFor(board: Board, file: File, color: Color): Boolean =
    val mask = Bitboard.file(file)
    val ours = board.pawns & board.byColor(color) & mask
    val theirs = board.pawns & board.byColor(!color) & mask
    ours.isEmpty && theirs.nonEmpty
