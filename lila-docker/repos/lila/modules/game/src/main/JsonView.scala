package lila.game

import play.api.libs.json.*
import lila.core.game.{ Game, Pov }

final class JsonView:
  def apply(game: Game): JsObject = Json.obj("id" -> game.id.value)
  def pov(pov: Pov): JsObject = Json.obj("game" -> apply(pov.game))
