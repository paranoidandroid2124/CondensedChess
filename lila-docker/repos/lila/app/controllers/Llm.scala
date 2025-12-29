package controllers

import play.api.libs.json.*
import play.api.mvc.*
import lila.app.{ *, given }
import lila.llm.{CommentRequest, CommentResponse}

final class Llm(env: lila.app.Env) extends LilaController(env):

  def comment = Action.async(parse.json) { req =>
    import lila.llm.CommentRequest.given
    import lila.llm.CommentResponse.given

    req.body.validate[CommentRequest] match
      case JsSuccess(data, _) =>
        env.llm.api.commentPosition(
          fen = data.fen,
          lastMove = data.lastMove,
          eval = data.eval,
          opening = data.context.opening,
          phase = data.context.phase,
          ply = data.context.ply
        ).map {
          case Some(res) => Ok(Json.toJson(res))
          case None => NotFound(jsonError("No comment generated"))
        }(env.executor)
      case JsError(errors) =>
        scala.concurrent.Future.successful(BadRequest(jsonError(s"Invalid JSON: ${errors.toString}")))
  }
