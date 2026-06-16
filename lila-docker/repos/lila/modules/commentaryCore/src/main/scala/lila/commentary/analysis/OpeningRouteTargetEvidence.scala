package lila.commentary.analysis

import _root_.chess.{ Board, Color, Square }
import lila.commentary.analysis.structure.PawnStructureTargets

private[commentary] object OpeningRouteTargetEvidence:

  def checkRouteBoard(
      board: Board,
      pressureSide: Color,
      route: OpeningRouteCatalog.Route
  ): Boolean =
    route.targetMode match
      case OpeningRouteCatalog.AttackWeakPawn =>
        pieceAttacksTarget(board, pressureSide, route) &&
          PawnStructureTargets.boardHasEnemyPawn(board, pressureSide, route.targetSquare) &&
          PawnStructureTargets.weakPawnTargetsForPressure(board, pressureSide).contains(route.targetSquare)
      case OpeningRouteCatalog.OccupyTarget =>
        route.to == route.targetSquare &&
          pieceOccupiesRouteSquare(board, pressureSide, route)
      case _ => false

  def checkRouteEvidence(evidence: PieceRouteEvidence.Evidence): Boolean =
    val board = evidence.terminalPosition.board
    (for
      to <- Square.fromKey(evidence.route.to)
      piece <- board.pieceAt(to)
    yield checkRouteBoard(board, piece.color, evidence.route)).getOrElse(false)

  def ownerSeedTerms(route: OpeningRouteCatalog.Route): List[String] =
    List(
      route.targetSquare,
      s"fixed_target:${route.targetSquare}",
      s"route_target:${route.routeId}",
      route.targetMode
    ).distinct

  def structureTransitionTerms(
      route: OpeningRouteCatalog.Route,
      evidence: PieceRouteEvidence.Evidence
  ): List[String] =
    (
      List(
        s"opening_route:${route.routeId}",
        s"opening_family:${route.family}",
        s"target_mode:${route.targetMode}",
        s"route_target:${route.targetSquare}",
        s"${route.role}_route:${route.path.mkString("-")}",
        s"played:${evidence.playedMove}"
      ) ++ List(
        Option.when(route.targetMode == OpeningRouteCatalog.AttackWeakPawn)(
          s"target_persistent_after_line:${route.targetSquare}"
        ),
        Option.when(route.targetMode == OpeningRouteCatalog.AttackWeakPawn)(
          s"target_attacked_after_line:${route.targetSquare}"
        )
      ).flatten ++ evidence.pvMoves.take(route.maxReplayPlies)
    ).filter(_.nonEmpty).distinct

  private def pieceAttacksTarget(
      board: Board,
      pressureSide: Color,
      route: OpeningRouteCatalog.Route
  ): Boolean =
    (for
      from <- Square.fromKey(route.to)
      target <- Square.fromKey(route.targetSquare)
      role <- PieceRouteEvidence.roleFromRoute(route)
      piece <- board.pieceAt(from)
    yield
      piece.color == pressureSide &&
        piece.role == role &&
        board.attackers(target, pressureSide).contains(from)
    ).getOrElse(false)

  private def pieceOccupiesRouteSquare(
      board: Board,
      pressureSide: Color,
      route: OpeningRouteCatalog.Route
  ): Boolean =
    (for
      square <- Square.fromKey(route.to)
      role <- PieceRouteEvidence.roleFromRoute(route)
      piece <- board.pieceAt(square)
    yield piece.color == pressureSide && piece.role == role).getOrElse(false)
