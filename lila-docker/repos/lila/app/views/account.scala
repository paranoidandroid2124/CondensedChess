package views

import lila.app.UiEnv.{ *, given }

object account:
  object pref:
    def apply(user: User, form: Any, categ: String)(using Context) = 
      Page("Preferences").wrap(_ => div("Preferences"))
    def network(args: Any*)(using Context) = 
      Page("Network").wrap(_ => div("Network"))

object oAuth:
  def authorize(prompt: lila.oauth.AuthorizationRequest.Prompt, user: User, url: String)(using Context) = 
    Page("Authorize").wrap(_ => div("Authorize"))
  object token:
    def index(tokens: Any)(using Context) = 
      Page("OAuth Tokens").wrap(_ => div("OAuth Tokens"))
    def create(form: Any, me: User)(using Context) = 
      Page("Create OAuth Token").wrap(_ => div("Create OAuth Token"))


