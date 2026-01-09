package lila.app
package http

import play.api.mvc.*
import lila.app.Context

final class KeyPages(val env: Env)(using Executor)
    extends Results with lila.web.ResponseWriter:

  def home(status: Results.Status)(using ctx: Context): Fu[Result] =
    fuccess(status("Home"))

  def homeHtml(using ctx: Context): Fu[lila.ui.RenderedPage] =
    fuccess(lila.ui.RenderedPage("Home"))

  def notFound(msg: Option[String])(using Context): Fu[Result] =
    fuccess(NotFound(msg | "Not Found"))

  def notFoundEmbed(msg: Option[String])(using RequestHeader): Result =
    NotFound(msg | "Not Found")

  def blacklisted(using ctx: Context): Result =
    Unauthorized("Blacklisted")
