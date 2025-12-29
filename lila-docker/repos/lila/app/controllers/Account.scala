package controllers

import cats.mtl.Handle.*
import play.api.data.Form
import play.api.libs.json.*
import play.api.mvc.*
import views.account.pages

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.id.SessionId
import lila.web.AnnounceApi
import lila.core.user.KidMode
import lila.security.IsPwned
import lila.core.security.ClearPassword

final class Account(
    env: Env,
    auth: Auth,
    apiC: => Api
) extends LilaController(env):

  def profile = Auth { _ ?=> me ?=>
    Ok.page:
      pages.profile(me, env.user.forms.profileOf(me))
  }

  def username = Auth { _ ?=> me ?=>
    Ok.page:
      pages.username(me, env.user.forms.usernameOf(me))
  }

  def profileApply = AuthOrScopedBody(_.Web.Mobile) { _ ?=> me ?=>
    bindForm(env.user.forms.profile)(
      err =>
        negotiate(
          BadRequest.page(pages.profile(me, err)),
          jsonFormError(err)
        ),
      profile =>
        for
          _ <- env.user.repo.setProfile(me, profile)
          flairForm = env.user.forms.flair(asMod = isGranted(_.LichessTeam))
          _ <- bindForm(flairForm)(_ => funit, env.user.repo.setFlair(me, _))
        yield
          env.user.lightUserApi.invalidate(me)
          Redirect(routes.User.show(me.username)).flashSuccess
    )
  }

  def usernameApply = AuthBody { _ ?=> me ?=>
    FormFuResult(env.user.forms.username(me))(err => renderPage(pages.username(me, err))): username =>
      env.user.repo
        .setUsernameCased(me, username)
        .inject(Redirect(routes.User.show(me.username)).flashSuccess)
        .recover: e =>
          Redirect(routes.Account.username).flashFailure(e.getMessage)
  }

  def info = Auth { ctx ?=> me ?=>
    negotiateJson:
      Ok(
        env.user.jsonView
          .full(me, None, withProfile = false) ++ Json
          .obj(
            "prefs" -> Json.obj(), // pref module deleted
            "nowPlaying" -> JsArray(), // round module deleted
            "online" -> true
          )
          .add("kid" -> ctx.kid)
          .add("troll" -> me.marks.troll)
          .add("announce" -> AnnounceApi.get.map(_.json))
      ).headerCacheSeconds(15)
  }

  def nowPlaying = Auth { _ ?=> _ ?=>
    negotiateJson(JsonOk(Json.obj("nowPlaying" -> JsArray())))
  }

  val apiMe = Scoped() { ctx ?=> me ?=>
    def limited = rateLimited:
      "Please don't poll this endpoint. Stream https://lichess.org/api#tag/board/get/apistreamevent instead."
    limit.apiMe(me, limited):
      JsonOk(env.user.jsonView.full(me.value, None, withProfile = true))
  }

  def apiNowPlaying = Scoped()(JsonOk(Json.obj("nowPlaying" -> JsArray())))

  def dasher = Auth { _ ?=> me ?=>
    negotiateJson:
      Ok:
        lila.common.Json.lightUser.write(me.light) ++ Json.obj(
            "prefs" -> Json.obj()
        )
  }

  def passwd = Auth { _ ?=> me ?=>
    env.security.forms.passwdChange.flatMap: form =>
      Ok.page(pages.password(form))
  }

  def passwdApply = AuthBody { ctx ?=> me ?=>
    auth.HasherRateLimit:
      env.security.forms.passwdChange.flatMap: form =>
        FormFuResult(form)(err => renderPage(pages.password(err))): data =>
          val newPass = ClearPassword(data.newPasswd1)
          for
            _ <- env.security.authenticator.setPassword(me, newPass)
            pwned <- env.security.pwned.isPwned(newPass)
            res <- refreshSessionId(Redirect(routes.Account.passwd).flashSuccess, pwned)
          yield res
  }

  private def refreshSessionId(result: Result, pwned: IsPwned)(using ctx: Context, me: Me): Fu[Result] = for
    _ <- env.security.store.closeAllSessionsOf(me)
    // push removed
    sessionId <- env.security.api.saveAuthentication(me, ctx.mobileApiVersion, pwned)
  yield result.withCookies(env.security.lilaCookie.session(env.security.api.sessionIdKey, sessionId.value))

  // Email methods removed (module deleted)

  def twoFactor = Auth { _ ?=> me ?=>
    if me.totpSecret.isDefined
    then
      env.security.forms.disableTwoFactor.flatMap: f =>
        Ok.page(views.account.twoFactor.disable(f))
    else
      env.security.forms.setupTwoFactor.flatMap: f =>
        Ok.page(views.account.twoFactor.setup(f))
  }

  def setupTwoFactor = AuthBody { ctx ?=> me ?=>
    auth.HasherRateLimit:
      env.security.forms.setupTwoFactor.flatMap: form =>
        FormFuResult(form)(err => renderPage(views.account.twoFactor.setup(err))): data =>
          for
            _ <- env.user.repo.setupTwoFactor(me, lila.user.TotpSecret.decode(data.secret))
            res <- refreshSessionId(Redirect(routes.Account.twoFactor).flashSuccess, pwned = IsPwned.No)
          yield res
  }

  def disableTwoFactor = AuthBody { ctx ?=> me ?=>
    auth.HasherRateLimit:
      env.security.forms.disableTwoFactor.flatMap: form =>
        FormFuResult(form)(err => renderPage(views.account.twoFactor.disable(err))): _ =>
          env.user.repo.disableTwoFactor(me).inject(Redirect(routes.Account.twoFactor).flashSuccess)
  }

  def close = Auth { _ ?=> me ?=>
    for
      form <- env.security.forms.closeAccount
      res <- Ok.page(pages.close(form, managed = false))
    yield res
  }

  def closeConfirm = AuthBody { ctx ?=> me ?=>
    auth.HasherRateLimit:
      env.security.forms.closeAccount.flatMap: form =>
        FormFuResult(form)(err => renderPage(pages.close(err, managed = false))): forever =>
          for _ <- env.api.accountTermination.disable(me.value, forever = forever)
          yield Redirect(routes.UserAnalysis.index).withCookies(env.security.lilaCookie.newSession)
  }

  def delete = Auth { _ ?=> me ?=>
    for
      form <- env.security.forms.deleteAccount
      res <- Ok.page(pages.delete(form, managed = false))
    yield res
  }

  def deleteConfirm = AuthBody { ctx ?=> me ?=>
    auth.HasherRateLimit:
      env.security.forms.deleteAccount.flatMap: form =>
        FormFuResult(form)(err => renderPage(pages.delete(err, managed = false))): _ =>
          for _ <- env.api.accountTermination.scheduleDelete(me.value)
          yield Redirect(routes.Account.deleteDone).withCookies(env.security.lilaCookie.newSession)
  }

  def deleteDone = Open { ctx ?=>
    if ctx.isAuth then Redirect(routes.UserAnalysis.index)
    else Ok("Your account has been scheduled for deletion.")
  }

  def kid = Auth { _ ?=> me ?=>
    for
      form <- env.security.forms.toggleKid
      page <- Ok.page(pages.kid(me, form, managed = false, none))
    yield page
  }
  def apiKid = Scoped(_.Preference.Read) { _ ?=> me ?=>
    JsonOk(Json.obj("kid" -> me.kid.yes))
  }

  def kidPost = AuthBody { ctx ?=> me ?=>
    env.security.forms.toggleKid.flatMap: form =>
      bindForm(form)(
        err =>
          negotiate(
            BadRequest.page(pages.kid(me, err, managed = false, none)),
            BadRequest(errorsAsJson(err))
          ),
        _ =>
          for
            _ <- env.user.api.setKid(me, getBoolAs[KidMode]("v"))
            res <- negotiate(Redirect(routes.Account.kid).flashSuccess, jsonOkResult)
          yield res
      )
  }

  def apiKidPost = Scoped(_.Preference.Write) { ctx ?=> me ?=>
    getBoolOptAs[KidMode]("v") match
      case None => BadRequest(jsonError("Missing v parameter"))
      case Some(v) => env.user.api.setKid(me, v).inject(jsonOkResult)
  }

  def security = Auth { _ ?=> me ?=>
    for
      _ <- env.security.api.dedup(me, req)
      sessions <- env.security.api.locatedOpenSessions(me, 50)
      clients <- env.oAuth.tokenApi.listClients(50)
      personalAccessTokens <- env.oAuth.tokenApi.countPersonal
      currentSessionId = env.security.api.reqSessionId(req)
      page <- Ok.async:
        views.account.security(me, sessions, currentSessionId, clients, personalAccessTokens)
    yield page.hasPersonalData
  }

  def signout(sessionId: String) = Auth { _ ?=> me ?=>
    if sessionId == "all"
    then refreshSessionId(Redirect(routes.Account.security).flashSuccess, pwned = IsPwned.No)
    else
      for
        _ <- env.security.store.closeUserAndSessionId(me, SessionId(sessionId))
        _ <- env.push.webSubscriptionApi.unsubscribeBySession(SessionId(sessionId))
      yield NoContent
  }

  private def renderReopen(form: Option[Form[Reopen]], msg: Option[String])(using Context) =
    env.security.forms.reopen.map: baseForm =>
      pages.reopen.form(form.foldLeft(baseForm)(_.withForm(_)), msg)

  def reopen = Open:
    auth.RedirectToProfileIfLoggedIn:
      Ok.async(renderReopen(none, none))

  def reopenApply = OpenBody:
    env.security.hcaptcha.verify().flatMap { captcha =>
      if captcha.ok then
        env.security.forms.reopen.flatMap:
          _.form
            .bindFromRequest()
            .fold(
              err => BadRequest.async(renderReopen(err.some, none)),
              data =>
                allow:
                  env.security.reopen
                    .prepare(data.username, data.email, _ => fuccess(false))
                    .flatMap: user =>
                      env.security.loginToken.rateLimit[Result](user, data.email, ctx.req, rateLimited):
                        lila.mon.user.auth.reopenRequest("success").increment()
                        env.security.reopen
                          .send(user, data.email)
                          .inject(Redirect(routes.Account.reopenSent))
                .rescue: (code, msg) =>
                  lila.mon.user.auth.reopenRequest(code).increment()
                  BadRequest.async(renderReopen(none, msg.some))
            )
      else BadRequest.async(renderReopen(none, none))
    }

  def reopenSent = Open:
    Ok.page(pages.reopen.sent)

  def reopenLogin(token: String) = Open:
    env.security.reopen
      .confirm(token)
      .flatMap:
        case None =>
          lila.mon.user.auth.reopenConfirm("token_fail").increment()
          notFound
        case Some(user) =>
          for
            result <- auth.authenticateUser(user, remember = true, pwned = IsPwned.No)
            _ = lila.mon.user.auth.reopenConfirm("success").increment()
          yield result

  def data = Auth { _ ?=> me ?=>
    Ok.page(pages.data(me.value))
  }
