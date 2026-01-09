package lila.app
package http

import alleycats.Zero
import scalatags.Text.all.*
import lila.i18n.trans
import play.api.http.*
import play.api.mvc.*

import lila.common.HTTPRequest
import lila.core.perm.{ Granter, Permission }
import lila.core.security.IsProxy

// Chesstory: Simplified CtrlFilters for analysis-only system
// Removed: NoCurrentGame, NoPlayban, NoPlaybanOrCurrent (game/playban dependencies)
trait CtrlFilters(using Executor) extends ControllerHelpers with ResponseBuilder:

  export Granter.{ apply as isGranted, opt as isGrantedOpt }

  def IfGranted(perm: Permission.Selector)(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    if isGrantedOpt(perm) then f
    else negotiate(authorizationFailed, authorizationFailed)

  def Firewall(a: => Fu[Result])(using ctx: Context): Fu[Result] =
    if env.security.firewall.accepts(ctx.req) then a
    else keyPages.blacklisted

  def WithProxy[A](res: IsProxy ?=> Fu[A])(using req: RequestHeader): Fu[A] =
    env.security.ip2proxy.ofIp(req.ipAddress).flatMap(res(using _))

  def NoTor(res: => Fu[Result])(using ctx: Context): Fu[Result] =
    env.security.ipTrust
      .isPubOrTor(ctx.req)
      .flatMap:
        if _ then Unauthorized.page(lila.ui.Page("Unauthorized").body(views.auth.pubOrTor))
        else res

  def NoBooster[A <: Result](a: => Fu[A])(using ctx: Context): Fu[Result] = a

  def NoLame[A <: Result](a: => Fu[A])(using Context): Fu[Result] = a

  def NoShadowban[A <: Result](a: => Fu[A])(using ctx: Context): Fu[Result] = a

  def XhrOrRedirectHome(res: => Fu[Result])(using ctx: Context): Fu[Result] =
    if HTTPRequest.isXhr(ctx.req) then res
    else Redirect("/")

  def Reasonable(
      page: Int,
      max: Max = Max(40),
      errorPage: => Fu[Result] = BadRequest("resource too old")
  )(result: => Fu[Result]): Fu[Result] =
    if page <= max.value && page > 0 then result else errorPage

  def NotForKids(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    if ctx.kid.no then f else notFound

  def NoCrawlers(result: => Fu[Result])(using ctx: Context): Fu[Result] =
    if HTTPRequest.isCrawler(ctx.req).yes then notFound else result

  def NoCrawlersUnlessPreview(result: => Fu[Result])(using ctx: Context): Fu[Result] =
    if HTTPRequest.isCrawler(ctx.req).yes && HTTPRequest.isImagePreviewCrawler(ctx.req).no
    then notFound
    else result

  def NoCrawlers[A](computation: => A)(using ctx: Context, default: Zero[A]): A =
    if HTTPRequest.isCrawler(ctx.req).yes then default.zero else computation

  def NotManaged(result: => Fu[Result])(using ctx: Context): Fu[Result] =
    result  // Always allow - clas module removed
