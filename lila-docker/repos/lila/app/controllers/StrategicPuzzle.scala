package controllers

import play.api.libs.json.*
import play.api.mvc.*

import lila.app.*
import lila.strategicPuzzle.StrategicPuzzle.*

final class StrategicPuzzle(
    env: Env
) extends LilaController(env):

  def home = Open:
    env.strategicPuzzle.api
      .nextBootstrap(ctx.me.map(_.userId), none[String])
      .flatMap:
        _.fold(renderEmptyPage):
          renderPage

  def show(id: String) = Open:
    env.strategicPuzzle.api
      .bootstrapById(id, ctx.me.map(_.userId))
      .flatMap:
        _.fold(notFound):
          renderPage

  def next = Open:
    val excludeId = get("after").filter(_.nonEmpty)
    env.strategicPuzzle.api
      .nextBootstrap(ctx.me.map(_.userId), excludeId)
      .map:
        _.fold(NotFound(Json.obj("error" -> "No puzzle found")))(payload => JsonOk(Json.toJson(payload)))

  def complete(id: String) = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    ctx.body.body.validate[CompleteRequest].fold(
      err => BadRequest(JsError.toJson(err)).toFuccess,
      payload =>
        env.strategicPuzzle.api
          .complete(id, ctx.me.map(_.userId), payload)
          .map:
            case CompleteOutcome.Invalid =>
              BadRequest(Json.obj("error" -> "Invalid strategic puzzle completion"))
            case CompleteOutcome.MissingPuzzle =>
              NotFound(Json.obj("error" -> "Strategic puzzle not found"))
            case CompleteOutcome.Success(response, _) =>
              JsonOk(Json.toJson(response))
    )

  private def renderPage(payload: BootstrapPayload)(using Context): Fu[Result] =
    Ok.page(views.pages.strategicPuzzlePage(payload))

  private def renderEmptyPage(using Context): Fu[Result] =
    Ok.page(views.pages.strategicPuzzleEmpty())
