package lila.commentary.analysis

import _root_.chess.{ Bishop, Knight, Position, Role, Rook }
import _root_.chess.format.{ Fen, Uci }
import _root_.chess.variant.Standard
import lila.commentary.model.NarrativeContext

private[commentary] object PieceRouteEvidence:

  final case class Evidence(
      route: OpeningRouteCatalog.Route,
      playedMove: String,
      pvMoves: List[String],
      terminalPosition: Position
  )

  def fromContext(
      ctx: NarrativeContext,
      route: OpeningRouteCatalog.Route
  ): Option[Evidence] =
    val pvMoves = normalizedTopUciMoves(ctx)
    val played = ctx.playedMove.map(NarrativeUtils.normalizeUciMove).filter(isUciMove).orElse(pvMoves.headOption)
    evaluate(ctx.fen, played, pvMoves, route)

  def evaluate(
      fen: String,
      playedUci: Option[String],
      pvMoves: List[String],
      route: OpeningRouteCatalog.Route
  ): Option[Evidence] =
    if route.path.size < 2 then None
    else
      for
        routeRole <- roleFromRoute(route)
        position <- Fen.read(Standard, Fen.Full(fen))
        played <- playedUci.map(NarrativeUtils.normalizeUciMove).filter(isUciMove)
        playedMove <- legalUciMove(position, played)
        if playedMove.piece.color == position.color &&
          playedMove.piece.role == routeRole &&
          playedMove.orig.key == route.path.head &&
          playedMove.dest.key == route.path(1)
        replayMoves = if pvMoves.headOption.contains(played) then pvMoves.tail else pvMoves
        terminal <- replayRouteTerminal(
          playedMove.after,
          replayMoves.take(route.maxReplayPlies),
          position.color,
          routeRole,
          route.path.drop(1)
        )
      yield Evidence(route, played, pvMoves, terminal)

  private def normalizedTopUciMoves(ctx: NarrativeContext): List[String] =
    ctx.engineEvidence.toList.flatMap(_.variations).headOption.toList
      .flatMap(line =>
        val rawUciMoves = line.moves.map(NarrativeUtils.normalizeUciMove).filter(isUciMove)
        if rawUciMoves.nonEmpty then rawUciMoves
        else
          line.parsedMoves
            .flatMap(move => clean(move.uci))
            .map(NarrativeUtils.normalizeUciMove)
            .filter(isUciMove)
      )

  private def replayRouteTerminal(
      position: Position,
      moves: List[String],
      side: _root_.chess.Color,
      role: Role,
      path: List[String]
  ): Option[Position] =
    path match
      case _ :: Nil => Some(position)
      case current :: next :: remainingPath =>
        moves match
          case uci :: rest =>
            legalUciMove(position, uci).flatMap { move =>
              val playsRouteLeg =
                position.color == side &&
                  move.piece.role == role &&
                  move.orig.key == current &&
                  move.dest.key == next
              if playsRouteLeg then
                if remainingPath.isEmpty then Some(move.after)
                else replayRouteTerminal(move.after, rest, side, role, next :: remainingPath)
              else
                legalPieceMove(position, side, role, current, next) match
                  case Some(routeMove) =>
                    if remainingPath.isEmpty then Some(routeMove.after)
                    else replayRouteTerminal(routeMove.after, moves, side, role, next :: remainingPath)
                  case None => replayRouteTerminal(move.after, rest, side, role, path)
            }
          case Nil =>
            legalPieceMove(position, side, role, current, next).flatMap(routeMove =>
              if remainingPath.isEmpty then Some(routeMove.after)
              else replayRouteTerminal(routeMove.after, Nil, side, role, next :: remainingPath)
            )
      case Nil => None

  private def legalPieceMove(
      position: Position,
      side: _root_.chess.Color,
      role: Role,
      from: String,
      to: String
  ): Option[_root_.chess.Move] =
    Option.when(position.color == side)(()).flatMap { _ =>
      legalUciMove(position, s"$from$to").filter(move =>
        move.piece.color == side &&
          move.piece.role == role &&
          move.orig.key == from &&
          move.dest.key == to
      )
    }

  private[analysis] def roleFromRoute(route: OpeningRouteCatalog.Route): Option[Role] =
    route.role match
      case "knight" => Some(Knight)
      case "bishop" => Some(Bishop)
      case "rook" => Some(Rook)
      case _ => None

  private def legalUciMove(position: Position, raw: String): Option[_root_.chess.Move] =
    Uci(raw).collect { case move: Uci.Move => move }.flatMap(position.move(_).toOption)

  private def isUciMove(move: String): Boolean =
    move.matches("""[a-h][1-8][a-h][1-8][qrbn]?""")

  private def clean(value: String): Option[String] =
    Option(value).map(_.trim).filter(_.nonEmpty)

private[commentary] object KnightRouteEvidence:

  type Evidence = PieceRouteEvidence.Evidence

  def fromContext(
      ctx: NarrativeContext,
      route: OpeningRouteCatalog.Route
  ): Option[Evidence] =
    Option.when(route.role == "knight")(()).flatMap(_ => PieceRouteEvidence.fromContext(ctx, route))

  def evaluate(
      fen: String,
      playedUci: Option[String],
      pvMoves: List[String],
      route: OpeningRouteCatalog.Route
  ): Option[Evidence] =
    Option.when(route.role == "knight")(()).flatMap(_ => PieceRouteEvidence.evaluate(fen, playedUci, pvMoves, route))
