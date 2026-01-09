package lila.game
package ui

import lila.api.Context
import scalatags.Text.all.*
import lila.core.id.GameId
import lila.core.game.{ Game, Pov }

final class GameUi(helpers: lila.ui.Helpers):
  import helpers.*

  def mini(game: Game)(using Context): Frag = span()
  
  def crosstable(cross: lila.game.Crosstable.WithMatchup, gameId: Option[GameId])(using Context): Frag = span()
  
  def widgets(game: Game, notes: Option[String], user: Option[lila.user.User], ownerLink: Boolean)(content: Frag)(using Context): Frag = span()
  
  def gameEndStatus(game: Game): Frag = span("Finished")

object GameUi:
  def apply(helpers: lila.ui.Helpers) = new GameUi(helpers)
