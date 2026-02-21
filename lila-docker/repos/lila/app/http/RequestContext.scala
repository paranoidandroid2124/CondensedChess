package lila.app
package http

import play.api.mvc.*

import lila.api.{ PageData, PageContext }
import lila.common.HTTPRequest

trait RequestContext(using Executor):

  val env: Env

  def makeContext(using req: RequestHeader): Fu[Context] = for
    userCtx <- makeUserContext(req)
    pref <- env.pref.api.get(Me.raw(userCtx.me), req)
  yield Context(req, userCtx, pref)

  def makeBodyContext[A](using req: Request[A]): Fu[BodyContext[A]] = for
    userCtx <- makeUserContext(req)
    pref <- env.pref.api.get(Me.raw(userCtx.me), req)
  yield BodyContext(req, userCtx, pref)


  private def pageDataBuilder(using ctx: Context): Fu[PageData] =
    if HTTPRequest.isSynchronousHttp(ctx.req)
    then
      val nonce = lila.ui.Nonce.random.some
      if env.mode.isDev then env.web.manifest.update()
      ctx.me.foldUse(fuccess(PageData.anon(nonce))): me ?=>
        env.user.lightUserApi.preloadUser(me)
        fuccess(PageData(
          teamNbRequests = 0,
          nbNotifications = 0,
          nonce = nonce
        ))
    else fuccess(PageData.anon(none))

  def pageContext(using ctx: Context): Fu[PageContext] =
    pageDataBuilder.dmap(PageContext(ctx, _))

  def InEmbedContext[A](f: EmbedContext ?=> A)(using ctx: Context): A =
    if env.mode.isDev then env.web.manifest.update()
    f(using EmbedContext(ctx))

  private def makeUserContext(req: RequestHeader): Fu[LoginContext] =
    env.security.api
      .restoreUser(req)
      .map:
        case Some(user) =>
           LoginContext(Some(lila.api.Me(user)), needsFp = false, none)
        case None => LoginContext.anon
