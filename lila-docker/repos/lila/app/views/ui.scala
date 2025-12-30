package views

import lila.app.UiEnv.{ *, given }
export lila.web.ui.bits

// Chesstory: Analysis-only UI - removed chat, setup, gathering, learn, coordinate,
// dgt, relation, plan, feed, practice, forum, timeline, opening, video, gameSearch, storm, dev

val captcha = lila.web.ui.CaptchaUi(helpers)

val boardEditor = lila.web.ui.BoardEditorUi(helpers)

val atomUi = lila.ui.AtomUi(helpers.routeUrl)

object oAuth:
  val token = lila.oauth.ui.TokenUi(helpers)(lila.security.ui.AccountSecurity.AccountPage, env.mode)
  val authorize = lila.oauth.ui.AuthorizeUi(helpers)(lightUserFallback)

val auth = lila.web.ui.AuthUi(helpers)
