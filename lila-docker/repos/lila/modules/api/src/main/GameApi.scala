package lila.api

import chess.format.Fen
import play.api.libs.json.*

import lila.analyse.{ Analysis, JsonView as analysisJson }
import lila.common.Json.given
import lila.core.config.*

/* Chesstory: Simplified GameApi for analysis-only system
 * Removed: real-time game queries, rating fields, opening detection
 */
final private[api] class GameApi(
    net: NetConfig,
    apiToken: Secret,
    gameRepo: lila.game.GameRepo,
    analysisRepo: lila.analyse.AnalysisRepo
)(using Executor):

  import GameApi.WithFlags

  def one(id: GameId, withFlags: WithFlags): Fu[Option[JsObject]] =
    gameRepo
      .game(id)
      .flatMapz: g =>
        gamesJson(withFlags)(List(g)).map(_.headOption)

  private def makeUrl(game: Game) = s"${net.baseUrl}/${game.id}/white"

  private def gamesJson(withFlags: WithFlags)(games: Seq[Game]): Fu[Seq[JsObject]] =
    val allAnalysis =
      if withFlags.analysis then analysisRepo.byIds(games.map(g => Analysis.Id(g.id)))
      else fuccess(List.fill(games.size)(none[Analysis]))
    allAnalysis.flatMap { analysisOptions =>
      (games.map(gameRepo.initialFen)).parallel.map { initialFens =>
        games.zip(analysisOptions).zip(initialFens).map { case ((g, analysisOption), initialFen) =>
          gameToJson(g, analysisOption, initialFen, checkToken(withFlags))
        }
      }
    }

  private def checkToken(withFlags: WithFlags) = withFlags.applyToken(apiToken.value)

  private def gameToJson(
      g: Game,
      analysisOption: Option[Analysis],
      initialFen: Option[Fen.Full],
      withFlags: WithFlags
  ) =
    Json
      .obj(
        "id" -> g.id,
        "initialFen" -> initialFen,
        "variant" -> g.variant.key,
        "speed" -> g.speed.key,
        "perf" -> g.perfKey,
        "createdAt" -> g.createdAt,
        "lastMoveAt" -> g.movedAt,
        "turns" -> g.ply,
        "color" -> g.turnColor.name,
        "status" -> g.status.name,
        "clock" -> g.clock.map { clock =>
          Json.obj(
            "initial" -> clock.limitSeconds,
            "increment" -> clock.incrementSeconds,
            "totalTime" -> clock.estimateTotalSeconds
          )
        },
        "players" -> JsObject(g.players.mapList { p =>
          p.color.name -> Json.obj(
            "userId" -> p.userId,
            "name" -> p.name.map(_.value)
          )
        }),
        "analysis" -> analysisOption.ifTrue(withFlags.analysis).map(analysisJson.moves(_)),
        "moves" -> withFlags.moves.option(g.sans.mkString(" ")),
        "fens" -> ((withFlags.fens && g.finished).so {
          chess
            .Position(g.variant, initialFen)
            .playPositions(g.sans)
            .toOption
            .map(boards => JsArray(boards.map(chess.format.Fen.writeBoard).map(Json.toJson)))
        }: Option[JsArray]),
        "winner" -> g.winnerColor.map(_.name),
        "url" -> makeUrl(g)
      )
      .noNull

object GameApi:

  case class WithFlags(
      analysis: Boolean = false,
      moves: Boolean = false,
      fens: Boolean = false,
      opening: Boolean = false,
      moveTimes: Boolean = false,
      blurs: Boolean = false,
      token: Option[String] = none
  ):
    def applyToken(validToken: String) = copy(blurs = token.has(validToken))
