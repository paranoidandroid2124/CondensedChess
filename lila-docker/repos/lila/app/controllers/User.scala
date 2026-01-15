package controllers

import play.api.mvc.*
import lila.app.*

// Chesstory: Bare minimum User controller - most features deleted with modules
final class User(
    override val env: Env
) extends LilaController(env):

  def show(username: UserStr) = Open:
    Ok(s"User profile for ${username.value} - features simplified").toFuccess

  def myself = Auth { _ ?=> me ?=>
    Redirect(routes.User.show(me.username))
  }

  def redirect(path: String) = Open:
    staticRedirect(path).getOrElse(notFound)
