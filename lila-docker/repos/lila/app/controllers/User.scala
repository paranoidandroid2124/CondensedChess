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
import lila.core.userId.UserSearch
import lila.game.GameFilter

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
    Found(env.user.repo.byId(username.id)): user =>
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
                  "crosstable" -> crosstable
                )
          )
      else Ok(views.user.bits.miniClosed(user, relation = None))

  def online = Anon:
    JsonOk(JsArray())

  def ratingHistory(username: UserStr) = Open:
    Ok(lila.core.data.SafeJsonStr("[]")).as(JSON).toFuccess

  def list = Open:
    negotiate(
      html = Ok(views.user.list(List.empty, List.empty, Nil, List.empty)),
      json = JsonOk(JsArray())
    )

  def apiList = Anon:
    JsonOk(JsArray())
 
  // top and topNbApi removed
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
