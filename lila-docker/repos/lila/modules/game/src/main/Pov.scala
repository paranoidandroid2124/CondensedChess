package lila.game

import scalalib.model.Seconds
import chess.Ply
import lila.core.game.*

object Pov:

  def list(game: Game): List[Pov] = game.players.mapList(lila.core.game.Pov(game, _))

  extension (pov: Pov)
    def hasMoved: Boolean = PlayerExt.hasMoved(pov.game.player(pov.color), pov.game.playedPlies)
    def remainingSeconds: Option[Seconds] = pov.game.clock.map(_.remainingTime(pov.color).roundSeconds)
    def isBeingPlayed: Boolean = pov.game.playable

  private def isFresher(a: Pov, b: Pov) = a.game.movedAt.isAfter(b.game.movedAt)
  private inline def orInf(inline sec: Option[Seconds]) = sec.getOrElse(Seconds(Int.MaxValue))
  private val someTime = Seconds(30)

  def priority(a: Pov, b: Pov) =
    if !a.isMyTurn && !b.isMyTurn then isFresher(a, b)
    else if !a.isMyTurn && b.isMyTurn then false
    else if a.isMyTurn && !b.isMyTurn then true
    else if orInf(a.remainingSeconds) < someTime && orInf(b.remainingSeconds) > someTime then true
    else if orInf(b.remainingSeconds) < someTime && orInf(a.remainingSeconds) > someTime then false
    else if !a.hasMoved && b.hasMoved then true
    else if !b.hasMoved && a.hasMoved then false
    else orInf(a.remainingSeconds) < orInf(b.remainingSeconds)

object PlayerExt:
  def hasMoved(p: Player, plies: Ply): Boolean = plies.value >= 2 || (p.color.white && plies.value >= 1)
