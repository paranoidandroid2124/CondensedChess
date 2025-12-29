package lila.core
package game

import _root_.chess.format.UciDump
import _root_.chess.variant.{ Variant }
import _root_.chess.{
  ByColor,
  Centis,
  Color,
  Clock,
  Game as ChessGame,
  Ply,
  Speed,
  Status,
  Outcome
}

import lila.core.id.{ GameFullId, GameId, GamePlayerId }
import lila.core.perf.PerfKey
import lila.core.userId.{ UserId, UserIdOf }
import lila.core.game.ClockHistory.*

case class Game(
    id: GameId,
    players: ByColor[Player],
    chess: ChessGame,
    loadClockHistory: Clock => Option[ClockHistory] = _ => ClockHistory.empty.some,
    status: Status,
    binaryMoveTimes: Option[Array[Byte]] = None,
    createdAt: Instant = nowInstant,
    movedAt: Instant = nowInstant,
    metadata: GameMetadata
):

  export chess.{ position, ply, clock, sans, startedAtPly, player as turnColor, history, variant }
  export metadata.{ source, pgnImport }
  export players.{ white as whitePlayer, black as blackPlayer, apply as player }

  lazy val clockHistory = chess.clock.flatMap(loadClockHistory)

  def player[U: UserIdOf](user: U): Option[Player] = players.find(_.isUser(user))
  def opponentOf[U: UserIdOf](user: U): Option[Player] = player(user).map(opponent)

  def player: Player = players(turnColor)
  def playerById(playerId: GamePlayerId): Option[Player] = players.find(_.id == playerId)

  def hasUserId(userId: UserId) = players.exists(_.userId.has(userId))

  def opponent(p: Player): Player = opponent(p.color)
  def opponent(c: Color): Player = player(!c)

  def turnOf(p: Player): Boolean = p == player
  def turnOf(c: Color): Boolean = c == turnColor

  def playedPlies: Ply = ply - startedAtPly

  def fullIdOf(player: Player): Option[GameFullId] =
    players.contains(player).option(GameFullId(s"$id${player.id}"))

  def fullIdOf(color: Color) = GameFullId(s"$id${player(color).id}")

  def bothClockStates: Option[Vector[Centis]] = clockHistory.map(_.bothClockStates(startColor))

  def lastMoveKeys: Option[String] =
    history.lastMove.map(UciDump.lastMove(_, variant))

  def perfKey: PerfKey = PerfKey(variant, speed)

  def started = status >= Status.Started
  def aborted = status == Status.Aborted
  def finished = status >= Status.Mate
  def finishedOrAborted = finished || aborted

  def playable = status < Status.Aborted && !sourceIs(_.Import)

  def aiLevel: Option[Int] = players.find(_.aiLevel)
  def hasAi: Boolean = players.exists(_.isAi)

  def winner: Option[Player] = players.find(_.isWinner | false)
  def loser: Option[Player] = winner.map(opponent)
  def winnerColor: Option[Color] = winner.map(_.color)
  def outcome: Option[Outcome] = finished.option(Outcome(winnerColor))
  def winnerUserId: Option[UserId] = winner.flatMap(_.userId)

  def drawn = finished && winner.isEmpty

  def speed = Speed(clock.map(_.config))

  def bothPlayersHaveMoved = playedPlies > 1
  def startColor = startedAtPly.turn

  def userIds: List[UserId] = players.flatMap(_.userId)

  def sourceIs(f: Source.type => Source): Boolean = source contains f(Source)

  def pov(c: Color) = Pov(this, c)
  def povs: ByColor[Pov] = ByColor(pov)

  override def toString = s"""Game($id)"""
end Game
