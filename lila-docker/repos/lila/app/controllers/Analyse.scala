package controllers

import chess.format.Fen
import play.api.libs.json.{ Json, JsArray }
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.oauth.AccessToken

final class Analyse(
    env: Env
) extends LilaController(env):

  def home = Anon:
    fuccess(Ok("Analysis Home"))

  def replay(id: String) = replayWithMoves(id, "")

  def replayWithMoves(id: String, moves: String) = Anon:
    env.game.gameRepo.game(GameId(id)).map:
      case Some(game) =>
        Ok(s"Replay game ${game.id}")
      case _ => NotFound("Game find error")

  def embed(gameId: GameId, color: Color) = embedReplayGame(gameId, color)

  val AcceptsPgn = Accepting("application/x-chess-pgn")

  def requestAnalysis(id: String) = Anon:
    fuccess(NotFound("Stub"))

  def embedReplayGame(gameId: GameId, color: Color) = Anon:
    InEmbedContext:
      env.game.gameRepo.game(gameId).map:
        case Some(game) =>
          // Simplified embed - just return game page
          render:
            case AcceptsPgn() => Ok(s"[Event \"?\"]\n[Site \"?\"]\n[Date \"????.??.??\"]\n[White \"?\"]\n[Black \"?\"]\n[Result \"*\"]\n\n*")
            case _ => Ok("Embedded Replay")
        case _ =>
          render:
            case AcceptsPgn() => NotFound("*")
            case _ => NotFound("Game not found")

  def externalEngineList = ScopedBody(_.Engine.Read) { _ ?=> me ?=>
    env.analyse.externalEngine.list(me).map { list =>
      JsonOk(JsArray(list.map(lila.analyse.ExternalEngine.jsonWrites.writes)))
    }
  }

  def externalEngineShow(id: String) = ScopedBody(_.Engine.Read) { _ ?=> me ?=>
    Found(env.analyse.externalEngine.find(me, id)): engine =>
      JsonOk(lila.analyse.ExternalEngine.jsonWrites.writes(engine))
  }

  def externalEngineCreate = ScopedBody(_.Engine.Write) { ctx ?=> me ?=>
    HTTPRequest.bearer(ctx.req).so { bearer =>
      val tokenId = AccessToken.idFrom(bearer)
      bindForm(lila.analyse.ExternalEngine.form)(
        jsonFormError,
        data =>
          env.analyse.externalEngine.create(me, data, tokenId).map { engine =>
            Created(lila.analyse.ExternalEngine.jsonWrites.writes(engine))
          }
      )
    }
  }

  def externalEngineUpdate(id: String) = ScopedBody(_.Engine.Write) { ctx ?=> me ?=>
    Found(env.analyse.externalEngine.find(me, id)): engine =>
      bindForm(lila.analyse.ExternalEngine.form)(
        jsonFormError,
        data =>
          env.analyse.externalEngine.update(engine, data).map { engine =>
            JsonOk(lila.analyse.ExternalEngine.jsonWrites.writes(engine))
          }
      )
  }

  def externalEngineDelete(id: String) = AuthOrScoped(_.Engine.Write) { _ ?=> me ?=>
    env.analyse.externalEngine.delete(me, id).elseNotFound(jsonOkResult)
  }
