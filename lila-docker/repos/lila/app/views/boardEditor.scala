package views

import lila.app.UiEnv
import play.twirl.api.Html

object boardEditor:
  def apply(fen: String, positionsJson: play.api.libs.json.JsValue, endgamePositionsJson: play.api.libs.json.JsValue)(using lila.api.Context): Html = Html("Board Editor Stub")
  
  object jsData:
    def apply()(using lila.api.Context): play.api.libs.json.JsValue = play.api.libs.json.Json.obj()
