package views

import lila.app.UiEnv.*

object account:
  object pref:
    def apply(_user: User, _form: Any, _categ: String)(using _ctx: Context) =
      Page("Preferences").wrap(_ => div("Preferences"))
    def network(_args: Any*)(using _ctx: Context) =
      Page("Network").wrap(_ => div("Network"))

object oAuth:
  def authorize(_prompt: lila.oauth.AuthorizationRequest.Prompt, _user: User, _url: String)(using _ctx: Context) =
    Page("Authorize").wrap(_ => div("Authorize"))
  object token:
    def index(_tokens: Any)(using _ctx: Context) =
      Page("OAuth Tokens").wrap(_ => div("OAuth Tokens"))
    def create(_form: Any, _me: User)(using _ctx: Context) =
      Page("Create OAuth Token").wrap(_ => div("Create OAuth Token"))
