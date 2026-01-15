package lila.app
package http

import play.api.mvc.*
import lila.app.*
import scala.annotation.unused

trait CtrlFilters(using @unused executor: Executor) extends ControllerHelpers with ResponseBuilder:

  export lila.core.perm.Granter.{ apply as isGranted, opt as isGrantedOpt }

  def IfGranted(perm: lila.core.perm.Permission.Selector)(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    if isGrantedOpt(perm) then f
    else negotiate(authorizationFailed, authorizationFailed)

  def Firewall(a: => Fu[Result])(using ctx: Context): Fu[Result] =
    if env.security.firewall.accepts(ctx.req) then a
    else keyPages.blacklisted

  def AuthOrTrustedIp(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    if ctx.isAuth then f
    else Redirect(routes.Auth.login).toFuccess

  def XhrOnly(res: => Fu[Result])(using ctx: Context): Fu[Result] =
    if lila.common.HTTPRequest.isXhr(ctx.req) then res else notFound

  def Reasonable(
      page: Int,
      max: Max = Max(40),
      errorPage: => Fu[Result] = BadRequest("resource too old")
  )(result: => Fu[Result]): Fu[Result] =
    if page <= max.value && page > 0 then result else errorPage

  def NotForKids(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    if ctx.kid.no then f else notFound

  def NoCrawlers(result: => Fu[Result])(using ctx: Context): Fu[Result] =
    if lila.common.HTTPRequest.isCrawler(ctx.req).yes then notFound else result

  def NotManaged(result: => Fu[Result]): Fu[Result] =
    result
