package controllers

import chess.format.Fen
import play.api.libs.json.{ Json, JsArray }
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.oauth.AccessToken

final class Analyse(
    env: Env,
    gameC: => Game,
    roundC: => Round
) extends LilaController(env):

  def requestAnalysis(id: GameId) = AuthOrScoped(_.Web.Mobile) { ctx ?=> me ?=>
    Found(env.game.gameRepo.game(id)): game =>
      env.fishnet
        .analyser(
          game,
          lila.fishnet.Work.Sender(
            userId = me,
            ip = ctx.ip.some,
            mod = isGranted(_.UserEvaluate) || isGranted(_.Relay),
            system = false
          )
        )
        .map:
          _.error.fold(NoContent)(BadRequest(_))
  }

  def replay(pov: Pov, userTv: Option[lila.user.User])(using ctx: Context) =
    // Simplified: redirect to game round page
    fuccess(Redirect(routes.Round.watcher(pov.gameId, pov.color)))

  def embed(gameId: GameId, color: Color) = embedReplayGame(gameId, color)

  val AcceptsPgn = Accepting("application/x-chess-pgn")

  def embedReplayGame(gameId: GameId, color: Color) = Anon:
    InEmbedContext:
      env.game.gameRepo.game(gameId).map:
        case Some(game) =>
          // Simplified embed - just return game page
          render:
            case AcceptsPgn() => Ok(s"[Event \"?\"]\n[Site \"?\"]\n[Date \"????.??.??\"]\n[White \"?\"]\n[Black \"?\"]\n[Result \"*\"]\n\n*")
            case _ =>
              Ok.snip:
                views.analyse.embed.lpv(
                  chess.format.pgn.PgnStr(""),
                  getPgn = false,
                  title = "Lichess PGN viewer",
                  Json.obj("orientation" -> color.name, "gameId" -> gameId.value)
                )
        case _ =>
          render:
            case AcceptsPgn() => NotFound("*")
            case _ => NotFound.snip(views.analyse.embed.notFound)

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
