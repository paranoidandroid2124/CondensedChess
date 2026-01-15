package lila.game
package ui

import lila.api.Context
import scalatags.Text.all.*
import lila.core.id.GameId
import lila.core.game.Game

final class GameUi(helpers: lila.ui.Helpers):
  import helpers.*

  def mini(_game: Game)(using _ctx: Context): Frag = span()
  
  def crosstable(_cross: lila.game.Crosstable.WithMatchup, _gameId: Option[GameId])(using _ctx: Context): Frag = span()
  
  def widgets(_game: Game, _notes: Option[String], _user: Option[lila.user.User], _ownerLink: Boolean)(content: Frag)(using _ctx: Context): Frag =
    span(content)
  
  def gameEndStatus(game: Game): Frag = span("Finished")

object GameUi:
  def apply(helpers: lila.ui.Helpers) = new GameUi(helpers)
