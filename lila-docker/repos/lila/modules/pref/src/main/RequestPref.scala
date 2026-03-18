package lila.pref

import monocle.syntax.all.*
import play.api.mvc.RequestHeader

import lila.common.CookieConsent

object RequestPref:

  import Pref.default

  private val bgCookieName = "bg"

  private def queryBg(req: RequestHeader): Option[Int] =
    queryParam(req.queryString, "bg")
      .flatMap(Pref.Bg.fromString.get)

  private def cookieBg(req: RequestHeader): Option[Int] =
    req.cookies
      .get(bgCookieName)
      .map(_.value)
      .flatMap(Pref.Bg.fromString.get)

  private def applyBg(pref: Pref, bg: Int): Pref =
    val updated = pref.copy(bg = bg)
    if updated.bg == Pref.Bg.DARKBOARD then
      updated.copy(bg = Pref.Bg.DARK).focus(_.board.brightness).replace(60)
    else updated

  def queryParamOverride(req: RequestHeader)(pref: Pref): Pref =
    queryBg(req).fold(pref)(applyBg(pref, _))

  def fromRequest(req: RequestHeader): Pref =
    val qs = req.queryString
    val consent = CookieConsent.fromRequest(req)
    val consentedBg = if consent.preferencesAllowed then cookieBg(req) else None
    if qs.isEmpty && req.session.isEmpty && consentedBg.isEmpty then default
    else
      def paramOrSession(name: String): Option[String] = queryParam(qs, name).orElse(req.session.get(name))
      val pref = default.copy(
        bg = queryBg(req).orElse(consentedBg) | default.bg,
        theme = paramOrSession("theme") | default.theme,
        theme3d = paramOrSession("theme3d") | default.theme3d,
        pieceSet = paramOrSession("pieceSet") | default.pieceSet,
        pieceSet3d = paramOrSession("pieceSet3d") | default.pieceSet3d,
        soundSet = paramOrSession("soundSet") | default.soundSet,
        bgImg = paramOrSession("bgImg"),
        is3d = paramOrSession("is3d").has("true"),
        board = default.board.copy(
          opacity = paramOrSession("boardOpacity").flatMap(_.toIntOption) | default.board.opacity,
          brightness = paramOrSession("boardBrightness").flatMap(_.toIntOption) | default.board.brightness,
          hue = paramOrSession("boardHue").flatMap(_.toIntOption) | default.board.hue
        )
      )
      applyBg(pref, pref.bg)

  private def queryParam(queryString: Map[String, Seq[String]], name: String): Option[String] =
    queryString
      .get(name)
      .flatMap(_.headOption)
      .filter: v =>
        v.nonEmpty && v != "auto"
