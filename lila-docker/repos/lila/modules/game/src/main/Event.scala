package lila.game

import lila.core.userId.UserId
import lila.db.dsl.Bdoc
import lila.core.game.Game

object Event:
  def apply(game: Game): String = ""
  case class CorrespondenceClock(days: Int)
