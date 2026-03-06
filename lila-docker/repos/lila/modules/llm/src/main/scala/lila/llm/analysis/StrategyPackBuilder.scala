package lila.llm.analysis

import _root_.chess.{ Color, Role }
import _root_.chess.format.Fen
import lila.llm.{ StrategyPack, StrategyPieceRoute, StrategySidePlan }
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
    val plans = buildPlans(ctx, sideToMoveColor)
    val routes = buildRoutes(data, sideToMoveColor, plans)
    val longTermFocus = buildLongTermFocus(ctx, plans, routes)
    val evidence = buildEvidence(ctx, routes)

    Option.when(plans.nonEmpty || routes.nonEmpty || longTermFocus.nonEmpty)(
      StrategyPack(
        sideToMove = sideToMove,
        plans = plans,
        pieceRoutes = routes,
        longTermFocus = longTermFocus,
        evidence = evidence
      )
    )

  def promptHints(pack: StrategyPack): List[String] =
    val planHints = pack.plans.take(2).map { p =>
      s"${p.side} long plan: ${p.planName}"
    }
    val routeHints = pack.pieceRoutes.take(2).map { r =>
      s"${r.side} ${r.piece} route ${r.route.mkString("-")} (${r.purpose})"
    }
    val focusHints = pack.longTermFocus.take(2).map(v => s"long-term focus: $v")
    (planHints ++ routeHints ++ focusHints).map(_.trim).filter(_.nonEmpty).distinct.take(8)

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
      sideToMoveColor: Color,
      plans: List[StrategySidePlan]
  ): List[StrategyPieceRoute] =
    val boardOpt =
      Fen.read(_root_.chess.variant.Standard, Fen.Full(data.fen)).map(_.board)
    val sides = List(sideToMoveColor, !sideToMoveColor)
    val sideToMove = sideName(sideToMoveColor)

    sides
      .flatMap { routeColor =>
        val routeSide = sideName(routeColor)
        val sidePlans = plans.filter(_.side == routeSide)
        data.pieceActivity.flatMap(pa => toPieceRoute(pa, boardOpt, routeColor, data.positionalFeatures, sidePlans))
      }
      .sortBy(r => (if r.side == sideToMove then 0 else 1, -r.confidence))
      .distinctBy(r => s"${r.side}|${r.piece}|${r.from}|${r.route.lastOption.getOrElse(r.from)}")
      .take(MaxRoutes)

  private def toPieceRoute(
      pa: PieceActivity,
      boardOpt: Option[_root_.chess.Board],
      routeColor: Color,
      positionalFeatures: List[PositionalTag],
      plans: List[StrategySidePlan]
  ): Option[StrategyPieceRoute] =
    if pa.keyRoutes.isEmpty then None
    else
      boardOpt
        .flatMap(_.pieceAt(pa.square))
        .filter(_.color == routeColor)
        .flatMap { piece =>
          val route = (pa.square :: pa.keyRoutes).map(_.key).distinct
          Option.when(route.size >= 2) {
            val destination = route.lastOption.getOrElse(pa.square.key)
            StrategyPieceRoute(
              side = sideName(routeColor),
              piece = pieceLabel(piece.role),
              from = pa.square.key,
              route = route,
              purpose = routePurpose(
                role = piece.role,
                pa = pa,
                destination = destination,
                boardOpt = boardOpt,
                routeColor = routeColor,
                positionalFeatures = positionalFeatures,
                plans = plans
              ),
              confidence = routeConfidence(pa),
              evidence = routeEvidence(pa, destination)
            )
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

  private def routePurpose(
      role: Role,
      pa: PieceActivity,
      destination: String,
      boardOpt: Option[_root_.chess.Board],
      routeColor: Color,
      positionalFeatures: List[PositionalTag],
      plans: List[StrategySidePlan]
  ): String =
    val destinationSquare = _root_.chess.Square.all.find(_.key == destination)
    val outpostSignal =
      positionalFeatures.exists {
        case PositionalTag.Outpost(square, color) =>
          color == routeColor && square.key == destination
        case PositionalTag.StrongKnight(square, color) =>
          color == routeColor && square.key == destination
        case _ => false
      }
    val rookFileSignal =
      role == _root_.chess.Rook &&
        destinationSquare.exists(sq => boardOpt.exists(board => isOpenOrSemiOpenFileFor(board, sq.file, routeColor)))
    val centerSignal = destinationSquare.exists(isCentralSquare)
    val planActivationSignal =
      plans.exists { p =>
        val name = p.planName.trim.toLowerCase
        (name.contains("attack") && role == _root_.chess.Queen) ||
        (name.contains("file") && role == _root_.chess.Rook) ||
        (name.contains("activation") && (role == _root_.chess.Knight || role == _root_.chess.Bishop))
      }

    if pa.isBadBishop then "bad bishop reroute"
    else if pa.isTrapped then "piece liberation"
    else if outpostSignal then "outpost reinforcement"
    else if rookFileSignal then "open-file occupation"
    else if pa.coordinationLinks.nonEmpty then "coordination lift"
    else if planActivationSignal then "plan activation lane"
    else if centerSignal then "centralization route"
    else if pa.mobilityScore < 0.4 then "mobility recovery"
    else "coordination improvement"

  private def routeConfidence(pa: PieceActivity): Double =
    val mobilityGainSignal = (0.55 - pa.mobilityScore).max(0.0) * 0.45
    val trappedBonus = if pa.isTrapped then 0.22 else 0.0
    val bishopBonus = if pa.isBadBishop then 0.16 else 0.0
    val coordinationBonus = pa.coordinationLinks.size.min(3) * 0.04
    val lengthPenalty = pa.keyRoutes.size.toDouble * 0.04
    (0.38 + mobilityGainSignal + trappedBonus + bishopBonus + coordinationBonus - lengthPenalty).max(0.22).min(0.92)

  private def routeEvidence(pa: PieceActivity, destination: String): List[String] =
    List(
      Option.when(pa.isBadBishop)("bishop_quality_signal"),
      Option.when(pa.isTrapped)("trapped_piece_signal"),
      Option.when(pa.mobilityScore < 0.4)("low_mobility_signal"),
      Option.when(pa.keyRoutes.size >= 2)("multi_hop_route"),
      Option.when(pa.coordinationLinks.nonEmpty)(s"coordination_links_${pa.coordinationLinks.size.min(4)}"),
      Option.when(pa.keyRoutes.lastOption.exists(_.key == destination))("destination_from_piece_activity"),
      Some("piece_activity")
    ).flatten

  private def buildLongTermFocus(
      ctx: NarrativeContext,
      plans: List[StrategySidePlan],
      routes: List[StrategyPieceRoute]
  ): List[String] =
    val scored = scala.collection.mutable.HashMap.empty[String, Double]
    val planNameKeys = plans.map(_.planName.trim.toLowerCase).toSet

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
        s"${route.side} ${route.piece} route ${route.route.mkString("-")} for ${route.purpose}",
        1.95 + route.confidence * 0.35
      )
    }

    ctx.semantic.foreach { semantic =>
      semantic.conceptSummary.take(4).zipWithIndex.foreach { case (concept, idx) =>
        push(concept, 1.10 - (idx * 0.05))
      }
      semantic.planAlignment.foreach { alignment =>
        push(s"plan alignment: ${alignment.band}", 1.25 + (alignment.score.toDouble / 100.0))
        alignment.narrativeIntent.foreach(intent => push(s"alignment intent: $intent", 1.20))
      }
    }

    ctx.whyAbsentFromTopMultiPV.take(2).foreach { reason =>
      push(s"engine-line gap: $reason", 1.45)
    }

    scored.toList
      .sortBy { case (text, weight) => (-weight, text) }
      .map(_._1)
      .take(MaxFocus)

  private def buildEvidence(
      ctx: NarrativeContext,
      routes: List[StrategyPieceRoute]
  ): List[String] =
    val routeEvidence =
      routes.flatMap(route => route.evidence.map(signal => s"route:${route.side}:$signal"))
    (
      ctx.mainStrategicPlans.flatMap(_.evidenceSources) ++
        ctx.whyAbsentFromTopMultiPV ++
        routeEvidence
    ).map(_.trim).filter(_.nonEmpty).distinct.take(MaxEvidence)

  private def sideName(color: Color): String =
    if color.white then "white" else "black"

  private def pieceLabel(role: Role): String =
    role match
      case _root_.chess.Pawn   => "P"
      case _root_.chess.Knight => "N"
      case _root_.chess.Bishop => "B"
      case _root_.chess.Rook   => "R"
      case _root_.chess.Queen  => "Q"
      case _root_.chess.King   => "K"

  private def isOpenOrSemiOpenFileFor(
      board: _root_.chess.Board,
      file: _root_.chess.File,
      color: Color
  ): Boolean =
    val mask = _root_.chess.Bitboard.file(file)
    val ownPawns = board.pawns & board.byColor(color) & mask
    ownPawns.isEmpty

  private def isCentralSquare(square: _root_.chess.Square): Boolean =
    Set(_root_.chess.File.C, _root_.chess.File.D, _root_.chess.File.E, _root_.chess.File.F).contains(square.file) &&
      Set(_root_.chess.Rank.Third, _root_.chess.Rank.Fourth, _root_.chess.Rank.Fifth, _root_.chess.Rank.Sixth).contains(square.rank)
