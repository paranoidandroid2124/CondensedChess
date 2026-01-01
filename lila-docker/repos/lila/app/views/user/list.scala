package views.user

import lila.app.UiEnv.{ *, given }

def list(args: Any*)(using Context) = Page("Users").wrap(_ => div("User list"))
