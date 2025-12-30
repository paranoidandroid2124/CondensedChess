package lila.game

import lila.core.game.LightPlayer
import lila.core.userId.UserName
import lila.db.dsl.Bdoc

case class Player(
    id: GamePlayerId,
    color: Color,
    userId: Option[UserId] = None,
    aiLevel: Option[Int] = None,
    name: Option[UserName] = None
):
  def isAi = aiLevel.isDefined

object Player:
  def from(light: lila.core.game.LightGame, color: Color, playerIds: String, doc: Bdoc) =
    Player(
      id = GamePlayerId(playerIds.take(8)),
      color = color,
      userId = light.player(color).userId,
      aiLevel = None,
      name = None
    )
