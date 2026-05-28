package lila.commentary.analysis

import _root_.chess.{ Board, Color }
import lila.commentary.analysis.structure.PawnStructureTargets

private[commentary] object OpeningRouteTargetEvidence:

  def checkRouteBoard(
      board: Board,
      pressureSide: Color,
      route: OpeningRouteCatalog.Route
  ): Boolean =
    route.targetMode match
      case OpeningRouteCatalog.AttackWeakPawn =>
        route.role == "knight" &&
          knightAttacks(route.to, route.targetSquare) &&
          PawnStructureTargets.boardHasEnemyPawn(board, pressureSide, route.targetSquare) &&
          PawnStructureTargets.weakPawnTargetsForPressure(board, pressureSide).contains(route.targetSquare)
      case OpeningRouteCatalog.OccupyTarget =>
        route.role == "knight" &&
          route.to == route.targetSquare
      case _ => false

  def ownerSeedTerms(route: OpeningRouteCatalog.Route): List[String] =
    List(
      route.targetSquare,
      s"fixed_target:${route.targetSquare}",
      s"route_target:${route.routeId}",
      route.targetMode
    ).distinct

  def structureTransitionTerms(
      route: OpeningRouteCatalog.Route,
      evidence: KnightRouteEvidence.Evidence
  ): List[String] =
    (
      List(
        s"opening_route:${route.routeId}",
        s"opening_family:${route.family}",
        s"target_mode:${route.targetMode}",
        s"route_target:${route.targetSquare}",
        s"${route.role}_route:${route.path.mkString("-")}",
        s"played:${evidence.playedMove}"
      ) ++ evidence.pvMoves.take(route.maxReplayPlies)
    ).filter(_.nonEmpty).distinct

  private def knightAttacks(from: String, target: String): Boolean =
    (for
      fromFile <- fileIndex(from)
      fromRank <- rankIndex(from)
      targetFile <- fileIndex(target)
      targetRank <- rankIndex(target)
    yield
      val df = math.abs(fromFile - targetFile)
      val dr = math.abs(fromRank - targetRank)
      (df == 1 && dr == 2) || (df == 2 && dr == 1)
    ).getOrElse(false)

  private def fileIndex(square: String): Option[Int] =
    square.headOption.map(_.toLower - 'a').filter(index => index >= 0 && index < 8)

  private def rankIndex(square: String): Option[Int] =
    square.lift(1).map(_.asDigit).filter(rank => rank >= 1 && rank <= 8)
