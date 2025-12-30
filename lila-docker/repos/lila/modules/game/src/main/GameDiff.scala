package lila.game

import lila.core.game.{ Game, Player }

final class GameDiff:
  def apply(origin: Game, updated: Game): (List[(String, lila.db.dsl.Bdoc)], List[String]) =
    (Nil, Nil)
