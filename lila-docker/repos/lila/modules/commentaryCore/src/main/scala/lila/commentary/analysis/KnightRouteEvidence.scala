package lila.commentary.analysis

import _root_.chess.{ Knight, Position }
import _root_.chess.format.{ Fen, Uci }
import _root_.chess.variant.Standard
import lila.commentary.model.NarrativeContext

private[commentary] object KnightRouteEvidence:

  final case class Evidence(
      route: OpeningRouteCatalog.Route,
      playedMove: String,
      pvMoves: List[String]
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
    if route.role != "knight" || route.path.size < 2 then None
    else
      for
        position <- Fen.read(Standard, Fen.Full(fen))
        played <- playedUci.map(NarrativeUtils.normalizeUciMove).filter(isUciMove)
        playedMove <- legalUciMove(position, played)
        if playedMove.piece.color == position.color &&
          playedMove.piece.role == Knight &&
          playedMove.orig.key == route.path.head &&
          playedMove.dest.key == route.path(1)
        replayMoves = if pvMoves.headOption.contains(played) then pvMoves.tail else pvMoves
        if replaySupportsRoute(playedMove.after, replayMoves.take(route.maxReplayPlies), position.color, route.path.drop(1))
      yield Evidence(route, played, pvMoves)

  private def normalizedTopUciMoves(ctx: NarrativeContext): List[String] =
    ctx.engineEvidence.toList.flatMap(_.variations).headOption.toList
      .flatMap(line =>
        val parsedUciMoves =
          line.parsedMoves
            .flatMap(move => clean(move.uci))
            .map(NarrativeUtils.normalizeUciMove)
            .filter(isUciMove)
        if parsedUciMoves.nonEmpty then parsedUciMoves
        else line.moves.map(NarrativeUtils.normalizeUciMove).filter(isUciMove)
      )

  private def replaySupportsRoute(
      position: Position,
      moves: List[String],
      side: _root_.chess.Color,
      path: List[String]
  ): Boolean =
    path match
      case _ :: Nil => true
      case current :: next :: remainingPath =>
        moves match
          case uci :: rest =>
            legalUciMove(position, uci).exists { move =>
              val playsRouteLeg =
                position.color == side &&
                  move.piece.role == Knight &&
                  move.orig.key == current &&
                  move.dest.key == next
              if playsRouteLeg then
                remainingPath.isEmpty || replaySupportsRoute(move.after, rest, side, next :: remainingPath)
              else
                legalKnightMove(position, side, current, next) match
                  case Some(routeMove) =>
                    remainingPath.isEmpty || replaySupportsRoute(routeMove.after, moves, side, next :: remainingPath)
                  case None => replaySupportsRoute(move.after, rest, side, path)
            }
          case Nil =>
            legalKnightMove(position, side, current, next).exists(routeMove =>
              remainingPath.isEmpty || replaySupportsRoute(routeMove.after, Nil, side, next :: remainingPath)
            )
      case Nil => false

  private def legalKnightMove(
      position: Position,
      side: _root_.chess.Color,
      from: String,
      to: String
  ): Option[_root_.chess.Move] =
    Option.when(position.color == side)(()).flatMap { _ =>
      legalUciMove(position, s"$from$to").filter(move =>
        move.piece.color == side &&
          move.piece.role == Knight &&
          move.orig.key == from &&
          move.dest.key == to
      )
    }

  private def legalUciMove(position: Position, raw: String): Option[_root_.chess.Move] =
    Uci(raw).collect { case move: Uci.Move => move }.flatMap(position.move(_).toOption)

  private def isUciMove(move: String): Boolean =
    move.matches("""[a-h][1-8][a-h][1-8][qrbn]?""")

  private def clean(value: String): Option[String] =
    Option(value).map(_.trim).filter(_.nonEmpty)
