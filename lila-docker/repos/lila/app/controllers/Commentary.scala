package controllers

import play.api.libs.json.*

import lila.app.*
import lila.commentary.api.CommentaryLocalProbeJsonTransport
import lila.commentary.api.CommentaryPublicJsonTransport

final class Commentary(env: Env) extends LilaController(env):

  def renderCommentary = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    CommentaryPublicJsonTransport.renderJson(ctx.body.body) match
      case Right(json) => Ok(json).toFuccess
      case Left(error) => BadRequest(error).toFuccess

  def renderLocalProbeCommentary =
    if env.mode.isProd then Open:
      authorizationFailed
    else
      OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
        CommentaryLocalProbeJsonTransport.renderJson(ctx.body.body) match
          case Right(json) => Ok(json).toFuccess
          case Left(error) => BadRequest(error).toFuccess
