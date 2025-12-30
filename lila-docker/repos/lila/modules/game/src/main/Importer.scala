package lila.game

import chess.Board
import lila.core.game.{ Game, Player }

object Importer:
  def apply(game: Game): Progress = Progress(game)
