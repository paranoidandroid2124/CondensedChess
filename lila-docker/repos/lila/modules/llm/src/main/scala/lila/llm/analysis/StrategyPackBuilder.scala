package lila.llm.analysis

import _root_.chess.{ Color, Role }
import _root_.chess.format.Fen
import lila.llm.{ StrategyPack, StrategyPieceRoute, StrategySidePlan }
import lila.llm.model.{ ExtendedAnalysisData, NarrativeContext }
import lila.llm.model.authoring.PlanHypothesis
import lila.llm.model.strategic.PieceActivity

object StrategyPackBuilder:

  def build(
      data: ExtendedAnalysisData,
      ctx: NarrativeContext
  ): Option[StrategyPack] =
    val sideToMoveColor = if data.isWhiteToMove then Color.White else Color.Black
    val sideToMove = sideName(sideToMoveColor)
    val plans = buildPlans(ctx, sideToMoveColor)
    val routes = buildRoutes(data, sideToMoveColor)
    val longTermFocus =
      (
        ctx.semantic.map(_.conceptSummary).getOrElse(Nil) ++
          ctx.mainStrategicPlans.take(2).map(_.planName)
      ).map(_.trim).filter(_.nonEmpty).distinct.take(5)
    val evidence =
      (
        ctx.mainStrategicPlans.flatMap(_.evidenceSources) ++
          ctx.whyAbsentFromTopMultiPV
      ).map(_.trim).filter(_.nonEmpty).distinct.take(6)

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
      s"${r.piece} route ${r.route.mkString("-")} (${r.purpose})"
    }
    val focusHints = pack.longTermFocus.take(2).map(v => s"long-term focus: $v")
    (planHints ++ routeHints ++ focusHints).map(_.trim).filter(_.nonEmpty).distinct.take(8)

  private def buildPlans(
      ctx: NarrativeContext,
      sideToMoveColor: Color
  ): List[StrategySidePlan] =
    val mover = sideName(sideToMoveColor)
    val opponent = sideName(!sideToMoveColor)

    val moverPlan =
      ctx.mainStrategicPlans.headOption
        .map(h => fromHypothesis(mover, h))
        .orElse(ctx.plans.top5.headOption.map(p => fromPlanRow(mover, p.name, p.supports ++ p.evidence, p.blockers ++ p.missingPrereqs)))

    val opponentPlan =
      ctx.opponentPlan.map { p =>
        fromPlanRow(opponent, p.name, p.supports ++ p.evidence, p.blockers ++ p.missingPrereqs)
      }

    (moverPlan.toList ++ opponentPlan.toList).distinctBy(p => s"${p.side}|${p.planName}").take(2)

  private def buildRoutes(
      data: ExtendedAnalysisData,
      sideToMoveColor: Color
  ): List[StrategyPieceRoute] =
    val boardOpt =
      Fen.read(_root_.chess.variant.Standard, Fen.Full(data.fen)).map(_.board)

    data.pieceActivity
      .flatMap(pa => toPieceRoute(pa, boardOpt, sideToMoveColor))
      .sortBy(r => -r.confidence)
      .take(3)

  private def toPieceRoute(
      pa: PieceActivity,
      boardOpt: Option[_root_.chess.Board],
      sideToMoveColor: Color
  ): Option[StrategyPieceRoute] =
    if pa.keyRoutes.isEmpty then None
    else
      boardOpt
        .flatMap(_.pieceAt(pa.square))
        .filter(_.color == sideToMoveColor)
        .flatMap { piece =>
          val route = (pa.square :: pa.keyRoutes).map(_.key).distinct
          Option.when(route.size >= 2) {
            StrategyPieceRoute(
              piece = pieceLabel(piece.role),
              from = pa.square.key,
              route = route,
              purpose = routePurpose(pa),
              confidence = routeConfidence(pa),
              evidence = routeEvidence(pa)
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

  private def routePurpose(pa: PieceActivity): String =
    if pa.isBadBishop then "bad bishop reroute"
    else if pa.isTrapped then "piece liberation"
    else if pa.mobilityScore < 0.4 then "mobility recovery"
    else "coordination improvement"

  private def routeConfidence(pa: PieceActivity): Double =
    val mobilityGainSignal = (0.55 - pa.mobilityScore).max(0.0) * 0.45
    val trappedBonus = if pa.isTrapped then 0.22 else 0.0
    val bishopBonus = if pa.isBadBishop then 0.16 else 0.0
    val lengthPenalty = pa.keyRoutes.size.toDouble * 0.04
    (0.38 + mobilityGainSignal + trappedBonus + bishopBonus - lengthPenalty).max(0.22).min(0.92)

  private def routeEvidence(pa: PieceActivity): List[String] =
    List(
      Option.when(pa.isBadBishop)("bishop_quality_signal"),
      Option.when(pa.isTrapped)("trapped_piece_signal"),
      Option.when(pa.mobilityScore < 0.4)("low_mobility_signal"),
      Some("piece_activity")
    ).flatten

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
