package controllers

import chess.format.Fen
import play.api.libs.json.*

import lila.app.{ *, given }
import lila.common.Json.given

final class Editor(env: Env) extends LilaController(env):

  private lazy val positionsJson =
    JsArray(chess.StartingPosition.all.map { p =>
      Json.obj(
        "eco" -> p.eco,
        "name" -> p.name,
        "fen" -> p.fen
      )
    })

  private lazy val endgamePositionsJson =
    JsArray(chess.EndgamePosition.positions.map { p =>
      Json.obj(
        "name" -> p.name,
        "fen" -> p.fen
      )
    })

  def index = load("")

  def load(urlFen: String) = Open:
    val fen: Option[Fen.Full] = lila.common.String
      .decodeUriPath(urlFen)
      .filter(_.nonEmpty)
      .map(Fen.Full.clean)
    Ok.page(views.boardEditor(fen))

  def data = Open:
    JsonOk(positionsJson ++ endgamePositionsJson)
