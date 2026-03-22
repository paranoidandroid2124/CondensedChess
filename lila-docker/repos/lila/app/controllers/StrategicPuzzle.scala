package controllers

import play.api.libs.json.*
import play.api.mvc.*

import lila.app.*
import lila.llm.UserFacingPayloadSanitizer
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
        _.fold(NotFound(Json.obj("error" -> "No live strategic puzzle is ready right now.")))(payload =>
          JsonOk(Json.toJson(UserFacingPayloadSanitizer.sanitize(payload)))
        )

  def complete(id: String) = OpenBodyOf(parse.json): (ctx: BodyContext[JsValue]) ?=>
    ctx.body.body.validate[CompleteRequest].fold(
      err => BadRequest(JsError.toJson(err)).toFuccess,
      payload =>
        env.strategicPuzzle.api
          .complete(id, ctx.me.map(_.userId), payload)
          .map:
            case CompleteOutcome.Invalid =>
              BadRequest(Json.obj("error" -> "We could not record that puzzle result."))
            case CompleteOutcome.MissingPuzzle =>
              NotFound(Json.obj("error" -> "That strategic puzzle is no longer available."))
            case CompleteOutcome.Success(response, _) =>
              JsonOk(Json.toJson(response))
    )

  private def renderPage(payload: BootstrapPayload)(using Context): Fu[Result] =
    Ok.page(views.pages.strategicPuzzlePage(UserFacingPayloadSanitizer.sanitize(payload)))

  private def renderEmptyPage(using Context): Fu[Result] =
    Ok.page(views.pages.strategicPuzzleEmpty())
