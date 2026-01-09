package lila.app
package http

import play.api.i18n.Lang
import play.api.mvc.*

import lila.api.{ PageData, PageContext }
import lila.common.HTTPRequest
import lila.common.extensions.*
import lila.core.i18n.LangPicker
import lila.oauth.OAuthScope

trait RequestContext(using Executor):

  val env: Env

  def makeContext(using req: RequestHeader): Fu[Context] = for
    userCtx <- makeUserContext(req)
    lang = getAndSaveLang(req, userCtx.me)
    pref <- env.pref.api.get(Me.raw(userCtx.me), req)
  yield Context(req, lang, userCtx, pref)

  def makeBodyContext[A](using req: Request[A]): Fu[BodyContext[A]] = for
    userCtx <- makeUserContext(req)
    lang = getAndSaveLang(req, userCtx.me)
    pref <- env.pref.api.get(Me.raw(userCtx.me), req)
  yield BodyContext(req, lang, userCtx, pref)

  def oauthContext(scoped: OAuthScope.Scoped)(using req: RequestHeader): Fu[Context] =
    val lang = getAndSaveLang(req, scoped.me.some)
    val userCtx = LoginContext(scoped.me.some, false, none, scoped.scopes.some)
    env.pref.api
      .get(Me.raw(scoped.me), req)
      .map:
        Context(req, lang, userCtx, _)

  def oauthBodyContext[A](scoped: OAuthScope.Scoped)(using req: Request[A]): Fu[BodyContext[A]] =
    val lang = getAndSaveLang(req, scoped.me.some)
    val userCtx = LoginContext(scoped.me.some, false, none, scoped.scopes.some)
    env.pref.api
      .get(Me.raw(scoped.me), req)
      .map:
        BodyContext(req, lang, userCtx, _)

  private def getAndSaveLang(req: RequestHeader, me: Option[Me]): Lang =
    val lang = LangPicker(req) // Simplified: LangPicker.apply(req) only extracts from req, merging user lang if needed manually or if LangPicker supports it? logic check: LangPicker(req, me.flatMap(_.lang)) was original.
    // user lang handling:
    // Original: Val lang = LangPicker(req, me.flatMap(_.lang))
    // I need to check if LangPicker has the 2-arg apply.
    // core/i18n.scala showed: def apply(req: play.api.mvc.RequestHeader): Lang
    // It DOES NOT have the 2-arg apply in the file I viewed (Step 28).
    // So I use single arg.
    me.filter(_.lang.forall(_ != lang.code)).foreach { env.user.repo.setLang(_, lang) }
    lang

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
