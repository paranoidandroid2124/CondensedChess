package controllers
import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.common.Json.given
import lila.core.id.{ GameFullId, ImageId }
import lila.web.{ StaticContent, WebForms }

/* Chesstory: Analysis-only main controller
 * Removed: round, puzzle, challenge dependencies
 */
final class Main(
    env: Env,
    assetsC: ExternalAssets
) extends LilaController(env):

  def toggleBlindMode = OpenBody:
    bindForm(WebForms.blind)(
      _ => BadRequest,
      (enable, redirect) =>
        Redirect(redirect).withCookies:
          lila.web.WebConfig.blindCookie.make(env.security.lilaCookie)(enable != "0")
    )

  def handlerNotFound(msg: Option[String])(using RequestHeader) =
    fuccess(NotFound(msg.getOrElse("Not Found")))

  def captchaCheck(id: GameId) = Anon:
    env.game.captchaApi.validate(id, ~get("solution")).map { valid =>
      Ok(if valid then 1 else 0)
    }

  def webmasters = Open:
    Ok("Webmasters")

  def robots = Anon:
    Ok:
      if env.net.crawlable && req.domain == env.net.domain.value && env.mode.isProd
      then StaticContent.robotsTxt
      else "User-agent: *\nDisallow: /"

  def manifest = Anon:
    JsonOk:
      StaticContent.manifest(env.net)

  def contact = Open:
    Ok("Contact")

  def faq = Open:
    Ok("FAQ")

  def instantChess = Open:
    Redirect(routes.UserAnalysis.index)

  def prometheusMetrics(key: String) = Anon:
    if key == env.web.config.prometheusKey
    then
      lila.web.PrometheusReporter
        .latestScrapeData()
        .fold(NotFound("No metrics found")): data =>
          Ok(data)
    else NotFound("Invalid prometheus key")

  def devAsset(@annotation.nowarn v: String, path: String, file: String) = assetsC.at(path, file)

  def uploadImage(rel: String) = AuthBody(lila.web.HashedMultiPart(parse)) { ctx ?=> me ?=>
    lila.core.security
      .canUploadImages(rel)
      .so:
        limit.imageUpload(rateLimited):
          ctx.body.body.file("image") match
            case None => JsonBadRequest("Image content only")
            case Some(image) =>
              val meta = lila.memo.PicfitApi.form.upload.bindFromRequest().value
              for
                image <- env.memo.picfitApi.uploadFile(image, me, none, meta)
                maxWidth = lila.ui.bits.imageDesignWidth(rel)
                url = meta match
                  case Some(info) if maxWidth.exists(dw => info.dim.width > dw) =>
                    maxWidth.map(dw => env.memo.picfitUrl.resize(image.id, Left(dw)))
                  case _ => env.memo.picfitUrl.raw(image.id).some
              yield JsonOk(Json.obj("imageUrl" -> url))
  }

  def imageUrl(id: ImageId, width: Int) = Auth { _ ?=> _ ?=>
    if width < 1 then JsonBadRequest("Invalid width")
    else
      JsonOk(
        Json.obj(
          "imageUrl" -> env.memo.picfitUrl
            .resize(id, Left(width.min(lila.ui.bits.imageDesignWidth(id.value).getOrElse(1920))))
        )
      )
  }
