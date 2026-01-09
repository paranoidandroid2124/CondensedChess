package lila.api

import play.api.libs.json.*
import play.api.mvc.*

final class Api:
  def toHttp(res: Api.ApiResult): Result = res match
    case Api.ApiResult.Data(js) => Results.Ok(js)
    case Api.ApiResult.Limited => Results.TooManyRequests

object Api:
  enum ApiResult:
    case Data(js: JsValue)
    case Limited
