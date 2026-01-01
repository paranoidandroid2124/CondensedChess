package controllers

import play.api.libs.json.*
import play.api.mvc.*
import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.ui.Page

final class User(
    env: Env
) extends LilaController(env):

  def show(username: String) = Open:
    meOrFetch(UserStr(username)).flatMap:
      case Some(u) =>
        negotiate(
          html = Ok.page(Page(u.username.value).wrap(_ => scalatags.Text.all.div(s"User ${u.username}"))),
          json = Ok(env.user.jsonView.full(u))
        )
      case None => notFound

  def list = Open:
    negotiate(
      html = Ok.page(Page("Users").wrap(_ => scalatags.Text.all.div("User list"))),
      json = Ok(Json.arr())
    )

  def autocomplete = Open:
    val term = get("term") | ""
    if term.length < 3 then fuccess(BadRequest)
    else
      env.user.repo.autocomplete(term).map: names =>
        Ok(Json.toJson(names)).as(JSON)

  def mod(username: String) = Open:
    fuccess(NotFound)

  def note(username: String) = Auth { _ ?=> _ ?=>
    fuccess(NotFound)
  }
