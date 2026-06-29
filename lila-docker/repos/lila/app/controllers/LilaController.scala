package controllers

import java.net.URI
import scala.util.Try
import play.api.data.{ Form, FormBinding }
import play.api.http.*
import play.api.mvc.*
import play.api.Mode

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.perm.Permission
import lila.ui.{ Page, Snippet }

abstract private[controllers] class LilaController(val env: Env)
    extends BaseController
    with lila.web.RequestGetter
    with lila.web.ResponseBuilder(using env.executor)
    with http.ResponseBuilder(using env.executor)
    with lila.web.ResponseHeaders
    with lila.web.ResponseWriter
    with lila.web.CtrlExtensions
    with http.CtrlFilters(using env.executor)
    with http.CtrlPage(using env.executor)
    with http.RequestContext(using env.executor)
    with lila.web.CtrlErrors:

  def controllerComponents = env.controllerComponents
  given Executor = env.executor
  given Scheduler = env.scheduler
  given FormBinding = parse.formBinding(parse.DefaultMaxTextLength)
  given reqBody(using r: BodyContext[?]): Request[?] = r.body

  given (using codec: Codec, pc: lila.api.PageContext): Writeable[Page] =
    Writeable(page => codec.encode(views.base.page(page)(using pc).html))

  given Conversion[Page, Fu[Page]] = fuccess(_)
  given Conversion[Snippet, Fu[Snippet]] = fuccess(_)

  given netDomain: lila.core.config.NetDomain = env.net.domain

  inline def ctx(using it: Context) = it // `ctx` is shorter and nicer than `summon[Context]`
  inline def req(using it: RequestHeader) = it // `req` is shorter and nicer than `summon[RequestHeader]`

  val limit = lila.web.Limiters(using env.executor, env.net.rateLimit)

  protected def secureCookie(using RequestHeader): Boolean =
    env.mode == Mode.Prod || env.net.baseUrl.value.startsWith("https://") || req.secure

  private lazy val csrfAllowedOrigins: Set[String] =
    val domain = env.net.domain.value
    originOf(env.net.baseUrl.value).toSet ++ Set(s"https://$domain", s"http://$domain")

  private def originOf(raw: String): Option[String] =
    Try(URI.create(raw)).toOption.flatMap: uri =>
      for
        scheme <- Option(uri.getScheme)
        host <- Option(uri.getHost)
      yield
        val port = uri.getPort
        val authority = if port == -1 then host else s"$host:$port"
        s"$scheme://$authority"

  private def csrfOriginAllowed(origin: String)(using RequestHeader): Boolean =
    csrfAllowedOrigins(origin) ||
      HTTPRequest.appOrigin(req, allowDevOrigin = env.mode != Mode.Prod).contains(origin)

  private def hasValidCsrfOrigin(using RequestHeader): Boolean =
    HTTPRequest.isSafe(req) ||
      HTTPRequest.origin(req).exists(csrfOriginAllowed) ||
      HTTPRequest.referer(req).flatMap(originOf).exists(csrfOriginAllowed) ||
      (env.mode != Mode.Prod && HTTPRequest.origin(req).isEmpty && HTTPRequest.referer(req).isEmpty)

  private def withCsrfCheck(next: => Fu[Result])(using RequestHeader): Fu[Result] =
    if hasValidCsrfOrigin then next
    else fuccess(Forbidden("Invalid request origin"))

  /* Anonymous requests */
  def Anon(f: Context ?=> Fu[Result]): EssentialAction =
    action(parse.empty)(req ?=> f(using Context.minimal(req)))

  /* Anonymous requests, with a body */
  def AnonBody(f: BodyContext[?] ?=> Fu[Result]): EssentialAction =
    action(parse.anyContent)(req ?=> f(using Context.minimalBody(req)))

  /* Anonymous requests, with a body */
  def AnonBodyOf[A](parser: BodyParser[A])(f: BodyContext[A] ?=> A => Fu[Result]): EssentialAction =
    action(parser)(req ?=> f(using Context.minimalBody(req))(req.body))

  /* Anonymous and authenticated requests */
  def Open(f: Context ?=> Fu[Result]): EssentialAction =
    OpenOf(parse.empty)(f)

  def OpenOf[A](parser: BodyParser[A])(f: Context ?=> Fu[Result]): EssentialAction =
    action(parser)(handleOpen(f))

  /* Anonymous and authenticated requests, with a body */
  def OpenBody(f: BodyContext[?] ?=> Fu[Result]): EssentialAction =
    OpenBodyOf(parse.anyContent)(f)

  /* Anonymous and authenticated requests, with a body */
  def OpenBodyOf[A](parser: BodyParser[A])(f: BodyContext[A] ?=> Fu[Result]): EssentialAction =
    action(parser)(handleOpenBody(f))

  private def handleOpenBody[A](f: BodyContext[A] ?=> Fu[Result])(using Request[A]): Fu[Result] =
    withCsrfCheck:
      makeBodyContext.flatMap:
        f(using _)

  private def handleOpen(f: Context ?=> Fu[Result])(using RequestHeader): Fu[Result] =
    withCsrfCheck:
      makeContext.flatMap:
        f(using _)

  /* Authenticated requests */
  def Auth(f: Context ?=> Me ?=> Fu[Result]): EssentialAction =
    Auth(parse.empty)(f)

  /* Authenticated requests */
  def Auth[A](parser: BodyParser[A])(f: Context ?=> Me ?=> Fu[Result]): EssentialAction =
    action(parser)(handleAuth(f))

  private def handleAuth(f: Context ?=> Me ?=> Fu[Result])(using RequestHeader): Fu[Result] =
    withCsrfCheck:
      makeContext.flatMap: ctx =>
        ctx.me.fold(authenticationFailed(using ctx))(f(using ctx)(using _))

  /* Authenticated requests with a body */
  def AuthBody(f: BodyContext[?] ?=> Me ?=> Fu[Result]): EssentialAction =
    AuthBody(parse.anyContent)(f)

  /* Authenticated requests with a body */
  def AuthBody[A](
      parser: BodyParser[A]
  )(f: BodyContext[A] ?=> Me ?=> Fu[Result]): EssentialAction =
    action(parser)(handleAuthBody(f))

  private def handleAuthBody[A](f: BodyContext[A] ?=> Me ?=> Fu[Result])(using Request[A]): Fu[Result] =
    withCsrfCheck:
      makeBodyContext.flatMap: ctx =>
        ctx.me.fold(authenticationFailed(using ctx))(f(using ctx)(using _))

  /* Authenticated requests requiring certain permissions */
  def Secure(perm: Permission.Selector)(
      f: Context ?=> Me ?=> Fu[Result]
  ): EssentialAction =
    Secure(parse.anyContent)(perm)(f)

  /* Authenticated requests requiring certain permissions */
  def Secure[A](
      parser: BodyParser[A]
  )(perm: Permission.Selector)(f: Context ?=> Me ?=> Fu[Result]): EssentialAction =
    Auth(parser): me ?=>
      withSecure(perm)(f)

  /* Authenticated requests requiring certain permissions, with a body */
  def SecureBody[A](
      parser: BodyParser[A]
  )(perm: Permission.Selector)(f: BodyContext[A] ?=> Me ?=> Fu[Result]): EssentialAction =
    AuthBody(parser): me ?=>
      withSecure(perm)(f)

  /* Authenticated requests requiring certain permissions, with a body */
  def SecureBody(
      perm: Permission.Selector
  )(f: BodyContext[?] ?=> Me ?=> Fu[Result]): EssentialAction =
    SecureBody(parse.anyContent)(perm)(f)

  private def withSecure[C <: Context](perm: Permission.Selector)(
      f: C ?=> Me ?=> Fu[Result]
  )(using C, Me): Fu[Result] =
    if isGranted(perm)
    then f.map(preventModCache(perm))
    else authorizationFailed

  private def preventModCache(perm: Permission.Selector)(res: Result) =
    if Permission.modPermissions(perm(Permission))
    then res.hasPersonalData
    else res

  /* everyone on dev/stage, beta perm on prod */
  def Beta[A](f: Context ?=> Fu[Result]): EssentialAction =
    Open: ctx ?=>
      if env.mode.notProd || isGrantedOpt(_.Beta) then f else authorizationFailed

  def FormFuResult[A, B: Writeable](
      form: Form[A]
  )(err: Form[A] => Fu[B])(op: A => Fu[Result])(using Request[?]): Fu[Result] =
    bindForm(form)(
      form => err(form).dmap { BadRequest(_) },
      op
    )

  def HeadLastModifiedAt(updatedAt: Instant)(f: => Fu[Result])(using RequestHeader): Fu[Result] =
    if req.method == "HEAD" then NoContent.withDateHeaders(lastModified(updatedAt))
    else f

  def bindForm[T, R](form: Form[T])(error: Form[T] => R, success: T => R)(using Request[?], FormBinding): R =
    val bound =
      if getBool("patch")
      then bindPatchForm(form)
      else form.bindFromRequest()
    bound.fold(error, success)

  private def bindPatchForm[T](form: Form[T])(using req: Request[?], formBinding: FormBinding): Form[T] =
    form.bind:
      // combine pre-filled data with request data
      formBinding(req).foldLeft(form.data) { case (s, (key, values)) =>
        if key.endsWith("[]") then
          val k = key.dropRight(2)
          s ++ values.zipWithIndex.map { (v, i) => s"$k[$i]" -> v }
        else s + (key -> ~values.headOption)
      }
