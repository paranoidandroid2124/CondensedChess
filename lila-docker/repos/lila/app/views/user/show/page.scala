package views.user.show

import lila.app.UiEnv.{ *, given }

object page:
  def apply(u: User, rel: Any, info: Any, follow: Any)(using Context) = 
    lila.ui.Page(u.username.value).wrap(_ => div(s"User ${u.username}"))
  def deleted(canCreate: Boolean)(using Context) = 
    lila.ui.Page("Deleted").wrap(_ => div("Deleted"))
