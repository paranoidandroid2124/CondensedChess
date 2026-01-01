package lila.core
package game

import scalalib.ThreadLocalRandom
import lila.core.id.{ GameId, GamePlayerId }

trait IdGenerator:
  def game: Fu[GameId]
  def games(nb: Int): Fu[List[GameId]]
  def withUniqueId(sloppy: NewGame): Fu[Game]

object IdGenerator:
  def uncheckedGame: GameId = GameId(ThreadLocalRandom.nextString(GameId.size))
  def uncheckedPlayer: GamePlayerId = GamePlayerId(ThreadLocalRandom.nextString(GamePlayerId.size))
