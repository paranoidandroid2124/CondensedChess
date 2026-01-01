package lila.core
package game

import _root_.chess.{ Color, PlayerName }
import cats.kernel.Eq

import lila.core.id.GamePlayerId
import lila.core.userId.{ UserId, UserIdOf }

case class Player(
    id: GamePlayerId,
    color: Color,
    aiLevel: Option[Int],
    isWinner: Option[Boolean] = None,
    userId: Option[UserId] = None,
    name: Option[PlayerName] = None
):
  def isAi = aiLevel.isDefined

  def hasUser = userId.isDefined

  def isUser[U: UserIdOf](u: U) = userId.has(u.id)

  def light = LightPlayer(color, aiLevel, userId)

object Player:
  given Eq[Player] = Eq.by(p => (p.id, p.userId))

  def make(color: Color, aiLevel: Option[Int]): Player =
    Player(
      id = IdGenerator.uncheckedPlayer,
      color = color,
      aiLevel = aiLevel
    )


trait NewPlayer:
  def apply(color: Color, user: Option[lila.core.user.User]): Player
  def anon(color: Color, aiLevel: Option[Int] = None): Player
