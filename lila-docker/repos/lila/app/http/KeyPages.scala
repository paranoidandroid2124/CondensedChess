package lila.app
package http

import play.api.mvc.*

final class KeyPages(val env: Env)
    extends Results with lila.web.ResponseWriter:

  def home(status: Results.Status): Fu[Result] =
    fuccess(status("Home"))

  def homeHtml: Fu[lila.ui.RenderedPage] =
    fuccess(lila.ui.RenderedPage("Home"))

  def notFound(msg: Option[String]): Fu[Result] =
    fuccess(NotFound(msg | "Not Found"))

  def notFoundEmbed(msg: Option[String]): Result =
    NotFound(msg | "Not Found")

  def blacklisted: Result =
    Unauthorized("Blacklisted")
