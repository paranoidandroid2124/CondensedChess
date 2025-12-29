package controllers

import play.api.mvc.*

import lila.app.{ *, given }
import lila.core.id.GameAnyId

final class Game(env: Env, apiC: => Api) extends LilaController(env):

  def bookmark(gameId: GameId) = AuthOrScopedBody(_.Web.Mobile) { _ ?=> me ?=>
    env.bookmark.api
      .toggle(env.round.gameProxy.updateIfPresent)(gameId, me, getBoolOpt("v"))
      .inject(NoContent)
  }

  def delete(gameId: GameId) = Auth { _ ?=> me ?=>
    Found(env.game.gameRepo.game(gameId)): game =>
      if game.pgnImport.flatMap(_.user).exists(me.is(_)) then
        for
          _ <- env.bookmark.api.removeByGameId(game.id)
          _ <- env.game.gameRepo.remove(game.id)
          _ <- env.analyse.analysisRepo.remove(game.id)
          _ <- env.game.cached.clearNbImportedByCache(me)
        yield Redirect(routes.User.show(me.username))
      else Redirect(routes.Round.watcher(game.id, game.naturalOrientation))
  }

  def exportOne(id: GameAnyId) = AnonOrScoped()(_ ?=> exportGame(id.gameId))

  private[controllers] def exportGame(gameId: GameId)(using Context): Fu[Result] =
    Found(env.round.proxyRepo.gameIfPresentOrFetch(gameId)): game =>
      // Simplified: return PGN directly using gameApi
      env.api.gameApi.one(gameId, lila.api.GameApi.WithFlags()).map:
        case Some(json) => Ok(json).as(JSON)
        case None => NotFound

  def exportByUser(username: UserStr) = Open(_ ?=> notImplemented)
  def apiExportByUser(username: UserStr) = Open(_ ?=> notImplemented)
  def apiExportByUserImportedGames() = Auth(_ ?=> _ ?=> notImplemented)
  def apiExportByUserBookmarks() = Scoped()(_ ?=> _ ?=> notImplemented)
  def exportByIds = AnonOrScopedBody(parse.tolerantText)()(_ ?=> notImplemented)

  private def notImplemented: Fu[Result] =
    fuccess(NotImplemented("This endpoint requires GameApiV2 which has been removed"))

  private[controllers] def requestPgnFlags(extended: Boolean)(using RequestHeader) =
    lila.game.PgnDump.WithFlags(
      moves = getBoolOpt("moves") | true,
      tags = getBoolOpt("tags") | true,
      clocks = getBoolOpt("clocks") | extended,
      evals = getBoolOpt("evals") | extended,
      opening = getBoolOpt("opening") | extended,
      literate = getBool("literate"),
      pgnInJson = getBool("pgnInJson"),
      delayMoves = delayMovesFromReq,
      lastFen = getBool("lastFen"),
      accuracy = getBool("accuracy"),
      division = getBoolOpt("division") | extended,
      bookmark = getBool("withBookmarked")
    )

  private[controllers] def delayMovesFromReq(using RequestHeader) =
    !get("key").exists(env.web.settings.noDelaySecret.get().value.contains)

  private[controllers] def gameContentType(format: String) =
    if format == "pgn" then pgnContentType else ndJson.contentType

  private[controllers] def preloadUsers(game: lila.core.game.Game): Funit =
    env.user.lightUserApi.preloadMany(game.userIds)
  private[controllers] def preloadUsers(users: lila.core.user.GameUsers): Unit =
    env.user.lightUserApi.preloadUsers(users.all.collect:
      case Some(lila.core.user.WithPerf(u, _)) => u)
