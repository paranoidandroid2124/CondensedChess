package controllers

import chess.format.Fen
import chess.variant.{ Standard, Variant, Chess960 }
import chess.{ ByColor }
import play.api.libs.json.Json
import play.api.mvc.*
import scalatags.Text.all.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.id.GameFullId
import lila.tree.ExportOptions
import lila.ui.Page
import lila.core.game.{ Game, Pov, Player, Source, IdGenerator, newMetadata }

final class UserAnalysis(
    env: Env
) extends LilaController(env)
    with lila.web.TheftPrevention:

  def index = load(none, Standard)

  def parseArg(arg: String) =
    arg.split("/", 2) match
      case Array(key) => load(none, Variant.orDefault(Variant.LilaKey(key)))
      case Array(key, fen) =>
        Variant(Variant.LilaKey(key)) match
          case Some(variant) if variant != Standard => load(fen.some, variant)
          case _ if Fen.Full.clean(fen) == Standard.initialFen => load(none, Standard)
          case Some(Standard) => load(fen.some, chess.variant.FromPosition)
          case _ => load(arg.some, chess.variant.FromPosition)
      case _ => load(none, Standard)
  

  private def load(urlFen: Option[String], variant: Variant) = Open:
    val inputFen: Option[Fen.Full] = urlFen.orElse(get("fen")).flatMap(readFen)
    val chess960PositionNum: Option[Int] = variant.chess960.so:
      getInt("position").orElse:
        Chess960.positionNumber(inputFen | variant.initialFen)
    val decodedFen: Option[Fen.Full] = chess960PositionNum.flatMap(Chess960.positionToFen).orElse(inputFen)
    val pov = makePov(decodedFen, variant)
    val orientation = get("color").flatMap(chess.Color.fromName) | pov.color
    
    val analyseUi = new lila.analyse.ui.AnalyseUi(lila.ui.Helpers)(env.web.analyseEndpoints)
    
    val data = Json.obj(
      "game" -> Json.obj(
        "fen" -> (decodedFen | variant.initialFen).value,
        "variant" -> variant.key.value
      ),
      "orientation" -> orientation.name,
      "pref" -> Json.obj()
    )

    // For better UX, we could fetch narrative asynchronously, 
    // but for now let's try to provide it if possible
    env.llm.api.commentPosition(
      fen = (decodedFen | variant.initialFen).value,
      lastMove = None,
      eval = None,
      opening = None,
      phase = "middlegame",
      ply = pov.game.ply.value
    ).flatMap: narrativeOpt =>
      val narrativeFrag = narrativeOpt.map: res =>
        lila.analyse.ui.BookmakerRenderer.render(res.commentary, res.variations, (decodedFen | variant.initialFen).value)
      
      Ok.page:
        analyseUi.userAnalysis(
          data = data,
          pov = pov,
          chess960PositionNum = chess960PositionNum,
          narrative = narrativeFrag
        )

  def pgn(pgn: String) = index

  def embed = index

  def readFen(from: String): Option[Fen.Full] = lila.common.String
    .decodeUriPath(from)
    .filter(_.trim.nonEmpty)
    .map(Fen.Full.clean)

  private[controllers] def makePov(fen: Option[Fen.Full], variant: Variant): Pov =
    Pov(Game.make(variant, fen), chess.Color.White)

  def game(id: GameId, color: Color) = Open:
    fuccess(NotFound)

  def forecastsPost(fullId: GameFullId) = Open:
    fuccess(NotFound)

  def forecastsGet(fullId: GameFullId) = Open:
    fuccess(NotFound)

  def forecastsOnMyTurn(fullId: GameFullId, uci: String) = Open:
    fuccess(NotFound)

  def help = Open:
    Ok.page:
      Page("Analysis Help").wrap(_ => div("Help"))
