package controllers

import akka.stream.scaladsl.*
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json.*
import play.api.mvc.*
import scalalib.paginator.Paginator

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.common.Json.given
import lila.core.user.LightPerf
import lila.core.userId.UserSearch
import lila.game.GameFilter
import lila.rating.PerfType
import lila.rating.UserPerfsExt.best8Perfs
import lila.user.WithPerfsAndEmails

final class User(
    override val env: Env,
    gameC: => Game
) extends LilaController(env):

  import env.user.lightUserApi

  def show(username: UserStr) = OpenBody:
    EnabledUser(username): u =>
      negotiate(
        renderShow(u),
        apiGames(u, GameFilter.All.name, 1)
      )

  def search(term: String) = Open: _ ?=>
    UserStr.read(term) match
      case Some(username) => Redirect(routes.User.show(username)).toFuccess
      case _ => notFound

  private def renderShow(u: UserModel, status: Results.Status = Results.Ok)(using Context): Fu[Result] =
    WithProxy: proxy ?=>
      limit.enumeration.userProfile(rateLimited):
        if HTTPRequest.isSynchronousHttp(ctx.req)
        then
          for
            nbs <- env.userNbGames(u, withCrosstable = false)
            info <- env.userInfo.fetch(u, nbs)
            page <- renderPage:
              lila.mon.chronoSync(_.user.segment("renderSync")):
                views.user.show.page.activity(Vector.empty, info)
          yield status(page).withCanonical(routes.User.show(u.username))
        else
          Ok("User info").toFuccess

  def download(username: UserStr) = OpenBody:
    val user =
      meOrFetch(username).dmap(_.filter(u => u.enabled.yes || ctx.is(u)))
    FoundPage(user):
      views.user.download(_)

  def gamesAll(username: UserStr, page: Int) = games(username, GameFilter.All.name, page)

  def games(username: UserStr, filter: String, page: Int) = OpenBody:
    Reasonable(page):
      WithProxy: proxy ?=>
        limit.enumeration.userProfile(rateLimited):
          EnabledUser(username): u =>
            negotiate(
              html = for
                nbs <- env.userNbGames(u, withCrosstable = true)
                filters = lila.app.mashup.GameFilterMenu(u, nbs, filter, ctx.isAuth)
                pag <- env.gamePaginator(
                  user = u,
                  nbs = nbs.some,
                  filter = filters.current,
                  me = ctx.me,
                  page = page
                )
                _ <- lightUserApi.preloadMany(pag.currentPageResults.flatMap(_.userIds))
                res <-
                  if HTTPRequest.isSynchronousHttp(ctx.req) then
                    for
                      info <- env.userInfo.fetch(u, nbs, withUblog = false)
                      res <- Ok.page:
                        views.user.show.page.games(info, pag, filters, None, notes = Map.empty)
                    yield res
                  else Ok.snip(views.user.show.gamesContent(u, nbs, pag, filters, filter, notes = Map.empty)).toFuccess
              yield res.withCanonical(routes.User.games(u.username, filters.current.name)),
              json = apiGames(u, filter, page)
            )

  private def EnabledUser(username: UserStr)(f: UserModel => Fu[Result])(using ctx: Context): Fu[Result] =
    if username.id.isGhost
    then
      negotiate(
        Ok.page(views.user.show.page.deleted(false)),
        notFoundJson("Deleted user")
      )
    else
      def notFound(canCreate: Boolean) = negotiate(
        NotFound.page(views.user.show.page.deleted(canCreate)),
        NotFound(jsonError("No such player, or account closed"))
      )
      meOrFetch(username).flatMap:
        case None => notFound(true)
        case Some(u) if u.enabled.yes || isGrantedOpt(_.UserModView) => f(u)
        case u => notFound(u.isEmpty)

  def showMini(username: UserStr) = Open:
    Found(env.user.api.withPerfs(username)): user =>
      if user.enabled.yes || isGrantedOpt(_.UserModView)
      then
        ctx.userId.traverse(env.game.crosstableApi(user.id, _)).flatMap: crosstable =>
          negotiate(
            html = for
              snip <- Ok.snip(views.user.mini(user, None, blocked = false, followable = false, relation = None, ping = None, crosstable))
            yield snip.headerCacheSeconds(5),
            json =
              import lila.game.JsonView.given
              Ok:
                Json.obj(
                  "crosstable" -> crosstable,
                  "perfs" -> lila.user.JsonView.perfsJson(user.perfs, user.perfs.best8Perfs)
                )
          )
      else Ok(views.user.bits.miniClosed(user.user, relation = None))

  def online = Anon:
    val max = 50
    negotiateJson:
      env.user.cached.getTop50Online.map: users =>
        Ok:
          Json.toJson:
            users
              .take(getInt("nb").fold(10)(_.min(max)))
              .map: u =>
                env.user.jsonView.full(u.user, u.perfs.some, withProfile = true)

  def ratingHistory(username: UserStr) = Open:
    Ok(lila.core.data.SafeJsonStr("[]")).as(JSON).toFuccess

  private def apiGames(u: UserModel, filter: String, page: Int)(using BodyContext[?]) =
    userGames(u, filter, page).flatMap(env.game.userGameApi.jsPaginator).map { res =>
      Ok(res ++ Json.obj("filter" -> GameFilter.All.name))
    }

  private def userGames(
      u: UserModel,
      filterName: String,
      page: Int
  )(using ctx: BodyContext[?]): Fu[Paginator[GameModel]] =
    limit.userGames(
      ctx.ip,
      fuccess(Paginator.empty[GameModel]),
      cost = page,
      msg = s"on ${u.username}"
    ):
      lila.mon.http.userGamesCost.increment(page.toLong)
      for
        pag <- env.gamePaginator(
          user = u,
          nbs = none,
          filter = lila.app.mashup.GameFilterMenu.currentOf(GameFilter.all, filterName),
          me = ctx.me,
          page = page
        )
        _ <- lightUserApi.preloadMany(pag.currentPageResults.flatMap(_.userIds))
      yield pag

  def list = Open:
    env.user.cached.top10.get {}.flatMap { leaderboards =>
      negotiate(
        html = for
          nbAllTime <- env.user.cached.top10NbGame.get {}
          topOnline <- env.user.cached.getTop50Online
          page <- renderPage:
            views.user.list(List.empty, topOnline, leaderboards, nbAllTime)
        yield Ok(page),
        json =
          given OWrites[LightPerf] = OWrites(env.user.jsonView.lightPerfIsOnline)
          import lila.user.JsonView.leaderboardsWrites
          JsonOk(leaderboards)
      )
    }

  def apiList = Anon:
    env.user.cached.top10.get {}.map { leaderboards =>
      import env.user.jsonView.lightPerfIsOnlineWrites
      import lila.user.JsonView.leaderboardsWrites
      JsonOk(leaderboards)
    }

  def top(perfKey: PerfKey, page: Int) = Open:
    Reasonable(page, Max(20)):
      env.user.cached
        .topPerfPager(perfKey, page)
        .flatMap: pager =>
          negotiate(
            Ok.page(views.user.list.top(perfKey, pager)),
            topNbJson(pager.currentPageResults)
          )

  def topNbApi(nb: Int, perfKey: PerfKey) = Anon:
    if nb == 1 && perfKey == PerfKey.standard then
      env.user.cached.top10.get {}.map { leaderboards =>
        import env.user.jsonView.lightPerfIsOnlineWrites
        import lila.user.JsonView.leaderboardStandardTopOneWrites
        JsonOk(leaderboards)
      }
    else env.user.cached.firstPageOf(perfKey).dmap(_.take(nb)).map(topNbJson)

  private def topNbJson(users: Seq[LightPerf]) =
    given OWrites[LightPerf] = OWrites(env.user.jsonView.lightPerfIsOnline)
    Ok(Json.obj("users" -> users))

  def writeNote(username: UserStr) = AuthBody: ctx ?=> me ?=>
    bindForm(lila.user.UserForm.note)(
      err => BadRequest(err.errors.toString).toFuccess,
      data =>
        doWriteNote(username, data): user =>
          Ok.snipAsync:
            env.user.noteApi.getForMyPermissions(user).map {
              views.user.noteUi.zone(user, _)
            }
    )

  def apiReadNote(username: UserStr) = Scoped() { _ ?=> me ?=>
    Found(meOrFetch(username)):
      env.user.noteApi
        .getForMyPermissions(_)
        .flatMap:
          lila.user.JsonView.notes(_)(using lightUserApi)
        .map(JsonOk)
  }

  def apiWriteNote(username: UserStr) = ScopedBody() { ctx ?=> me ?=>
    bindForm(lila.user.UserForm.apiNote)(
      doubleJsonFormError,
      data => doWriteNote(username, data)(_ => jsonOkResult)
    )
  }

  private def doWriteNote(
      username: UserStr,
      data: lila.user.UserForm.NoteData
  )(f: UserModel => Fu[Result])(using Context, Me) =
    Found(meOrFetch(username)): user =>
      val isMod = data.mod && isGranted(_.ModNote)
      for
        _ <- env.user.noteApi.write(user.id, data.text, isMod, dox = isMod && data.dox)
        result <- f(user)
      yield result

  def deleteNote(id: String) = Auth { ctx ?=> me ?=>
    Found(env.user.noteApi.byId(id)): note =>
      (note.isFrom(me) && !note.mod).so:
        env.user.noteApi.delete(note._id).inject(Redirect(routes.User.show(note.to).url + "?note"))
  }

  def myself = Auth { _ ?=> me ?=>
    Redirect(routes.User.show(me.username))
  }

  def redirect(path: String) = Open:
    staticRedirect(path) |
      UserStr.read(path).so(tryRedirect).getOrElse(notFound)

  def tryRedirect(username: UserStr)(using Context): Fu[Option[Result]] =
    meOrFetch(username).map:
      _.map: user =>
        Redirect(routes.User.show(user.username))
