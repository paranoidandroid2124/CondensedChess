package lila.app
package http

import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.*
import play.api.mvc.Results.*
import play.api.routing.*
import play.api.{ Configuration, Environment, UsefulException }

import lila.api.{ LoginContext, PageContext }
import lila.common.HTTPRequest

final class ErrorHandler(
    environment: Environment,
    config: Configuration,
    router: => Router,
    mainC: => controllers.Main
)(using Executor)
    extends DefaultHttpErrorHandler(environment, config, router.some)
    with lila.web.ResponseWriter:

  override def onProdServerError(req: RequestHeader, exception: UsefulException) =
    fuccess(InternalServerError("Sorry, something went wrong."))

  override def onClientError(req: RequestHeader, statusCode: Int, msg: String): Fu[Result] =
    fuccess(Status(statusCode)(msg))
