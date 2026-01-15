package lila.app
package http

import play.api.mvc.*

import lila.api.{ PageData, PageContext }
import lila.common.HTTPRequest
import lila.common.extensions.*
import lila.oauth.OAuthScope

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

  def oauthContext(scoped: OAuthScope.Scoped)(using req: RequestHeader): Fu[Context] =
    val userCtx = LoginContext(scoped.me.some, false, none, scoped.scopes.some)
    env.pref.api
      .get(Me.raw(scoped.me), req)
      .map:
        Context(req, userCtx, _)

  def oauthBodyContext[A](scoped: OAuthScope.Scoped)(using req: Request[A]): Fu[BodyContext[A]] =
    val userCtx = LoginContext(scoped.me.some, false, none, scoped.scopes.some)
    env.pref.api
      .get(Me.raw(scoped.me), req)
      .map:
        BodyContext(req, userCtx, _)


  private def pageDataBuilder(using ctx: Context): Fu[PageData] =
    if HTTPRequest.isSynchronousHttp(ctx.req)
    then
      val nonce = lila.ui.Nonce.random.some
      if env.mode.isDev then env.web.manifest.update()
      ctx.me.foldUse(fuccess(PageData.anon(nonce))): me ?=>
        env.user.lightUserApi.preloadUser(me)
        val enabledId = me.enabled.yes.option(me.userId)
        // Removed clas, team, challenge, notify check if simplified?
        // Let's assume team, challenge, notify modules exist or are stubbed.
        // I will keep team/challenge/notify logic if they exist, but remove Clas.
        // Env.scala has: team (NOT in explicit list I read in Step 35? Checked Step 35: env.team is NOT there).
        // Env.scala Step 35: has user, mailer, oAuth, security, pref, game, evalCache, analyse, study, llm, web, api.
        // MISSING: team, challenge, notifyM, clas.
        // I must stub these out in PageData as well.
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
           LoginContext(Some(lila.api.Me(user)), needsFp = false, none, none)
        case None => LoginContext.anon
