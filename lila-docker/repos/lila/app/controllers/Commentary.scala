package controllers

import play.api.libs.json.*

import lila.app.*

final class Commentary(env: Env) extends LilaController(env):

  private val unavailable = Json.obj(
    "status" -> "unavailable",
    "noCommentary" -> true,
    "render" -> JsNull
  )

  def renderCommentary = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    ServiceUnavailable(unavailable).toFuccess

  def renderLocalProbeCommentary =
    OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
      ServiceUnavailable(unavailable).toFuccess
