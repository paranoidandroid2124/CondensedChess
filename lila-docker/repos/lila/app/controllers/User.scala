package controllers

import play.api.libs.json.*
import play.api.mvc.*
import lila.app.{ *, given }
import lila.common.HTTPRequest
import scalatags.Text.all.*
import lila.ui.Page
import lila.llm.LlmApi

final class User(
    env: Env
) extends LilaController(env):

  def show(username: UserStr) = Open:
    meOrFetch(username).flatMap:
      case Some(u) =>
        negotiate(
          html = Ok.page(Page(u.username.value).wrap(_ => div(s"User ${u.username}"))),
          json = Ok(env.user.jsonView.full(u))
        )
      case None => notFound

  def list = Open:
    negotiate(
      html = Ok.page(Page("Users").wrap(_ => div("User list"))),
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

  def gamesAll(username: UserStr, page: Int) = Open { _ ?=> fuccess(NotFound) }
  def download(username: UserStr) = Open { _ ?=> fuccess(NotFound) }
  def apiList = Open { _ ?=> JsonOk(Json.arr()) }
  def online = Open { _ ?=> fuccess(NotFound) }
  def redirect(path: String) = Open { _ ?=> Redirect(s"/$path") }

  def note(username: String) = Auth { _ ?=> _ ?=>
    fuccess(NotFound)
  }
