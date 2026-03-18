package lila.ui

import play.api.i18n.Lang
import java.time.Month
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import scala.annotation.unused
import scalatags.Text.all.*

// Minimal helpers trait for UI components
trait Helpers:
  private val timeDatetime = attr("datetime")
  private val absoluteDateFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC)
  private def classNames(values: Option[String]*): String = values.flatten.mkString(" ")

  def iconTag(icon: Icon): Tag = i(attr("data-icon") := icon.value)
  def iconTag(icon: Icon, text: Frag): Tag = i(attr("data-icon") := icon.value, cls := "text")(text)
  def showMonth(m: Month)(using lang: Lang): String =
    m.getDisplayName(TextStyle.FULL, lang.locale)

  private def timeagoTag(d: java.time.Instant, preload: Boolean): Tag =
    tag("time")(
      cls := classNames(Some("timeago"), preload.option("timeago--preload")),
      timeDatetime := d.toString
    )(showDate(d))

  def momentFromNowWithPreload(d: java.time.Instant): Frag = timeagoTag(d, preload = true)
  def chessgroundMini(fen: String | chess.format.BoardFen, color: chess.Color, lastMove: Option[chess.format.Uci]): Tag => Tag = t =>
    t(
      cls := "mini-board pv-line",
      attr("data-fen") := fen.toString,
      attr("data-color") := color.name,
      attr("data-lastmove") := lastMove.map(_.uci).getOrElse("")
    )
  def userIdLink(
      userId: Option[lila.core.userId.UserId],
      withOnline: Boolean = true,
      klass: Option[String] = None
  ): Tag =
    userId.fold[Tag](span(cls := "user-link user-link--anonymous")("Anonymous")): userId =>
      val baseClasses = userClass(userId, withOnline).split(' ').filter(_.nonEmpty).toList
      val classes = (baseClasses ++ klass.toList).mkString(" ")
      a(cls := classes, href := s"/@/${userId.value}")(userId.value)
  def showRatingDiff(diff: Int): Frag = frag(s"${if (diff > 0) "+" else ""}$diff")
  def aiNameFrag(level: Int): Frag = frag(s"AI level $level")
  def berserkIconSpan: Tag = span(cls := "berserk")("B")
  def ratedName(rated: Boolean): String = if (rated) "Rated" else "Casual"
  def esmInitBit(name: String): Esm = Esm(name)
  def routeUrl(call: play.api.mvc.Call): String = call.url

  // Missing helpers added for stabilization
  def submitButton(mods: Modifier*): Tag = button(tpe := "submit", mods)
  def postForm(mods: Modifier*): Tag = form(method := "post", mods)
  def copyMeInput(v: String): Tag = input(cls := "copyable", value := v, readonly)
  def copyMeLink(url: String, text: String): Tag = a(href := url, cls := "copyable")(text)
  def copyMeContent(url: String, text: String): Tag = a(href := url, cls := "copyable-content")(text)
  def cdnUrl(url: String): String = url
  def pathUrl(url: String): String = url

  def momentFromNow(d: java.time.Instant): Frag = timeagoTag(d, preload = false)
  def titleNameOrId(userId: lila.core.userId.UserId): Frag = userIdLink(Some(userId), withOnline = false)
  def pagerNext(pager: scalalib.paginator.Paginator[?], url: Int => String): Frag =
    if pager.currentPage < pager.nbPages then
      val nextPage = pager.currentPage + 1
      div(cls := "pager", attr("aria-label") := s"Pagination, page ${pager.currentPage} of ${pager.nbPages}")(
        span(cls := "pager__status")(s"Page ${pager.currentPage} of ${pager.nbPages}"),
        a(cls := "button button-empty pager__next", href := url(nextPage), rel := "next")("Load more")
      )
    else frag()
  def addQueryParam(url: String, name: String, value: String): String = 
    val separator = if (url.contains("?")) "&" else "?"
    s"$url$separator$name=$value"
  
  def chessgroundBoard: Tag = div(cls := "cg-wrap")(raw(""))

  private def playerNameTag(p: lila.core.game.Player): Tag =
    p.aiLevel match
      case Some(level) => span(cls := "player-link player-link--ai")(aiNameFrag(level))
      case None =>
        p.userId match
          case Some(id) => userIdLink(Some(id), withOnline = false)
          case None =>
            p.name.fold(span(cls := "player-link player-link--anonymous")("Anonymous")): name =>
              span(cls := "player-link")(name.value)

  def playerText(p: lila.core.game.Player): Tag = playerNameTag(p)
  def playerLink(
      p: lila.core.game.Player,
      withOnline: Boolean = true,
      @unused withDiff: Boolean = false,
      withBerserk: Boolean = false
  ): Tag =
    p.aiLevel match
      case Some(level) =>
        span(cls := classNames(Some("player-link"), Some("player-link--ai"), withBerserk.option("player-link--berserk")))(
          aiNameFrag(level)
        )
      case None =>
        p.userId match
          case Some(id) =>
            userIdLink(Some(id), withOnline, withBerserk.option("player-link--berserk"))
          case None =>
            p.name.fold(span(cls := "player-link player-link--anonymous")("Anonymous")): name =>
              span(cls := classNames(Some("player-link"), withBerserk.option("player-link--berserk")))(name.value)
  def gameEndStatus(game: lila.core.game.Game): Frag = frag(game.status.name)

  def globalErrorNamed(form: play.api.data.Form[?], name: String): Frag = 
    form.globalError.filter(_.message == name).map(e => div(cls := "error")(e.message)).getOrElse(frag())
  def errMsg(field: play.api.data.Field): Frag = 
    field.errors.map(e => div(cls := "error")(e.message)).headOption.getOrElse(frag())
  
  def langHref(url: play.api.mvc.Call)(using @unused ctx: Context): String = url.url
  def userClass(@unused id: lila.core.userId.UserId, withOnline: Boolean = false): String =
    classNames(Some("user-link"), withOnline.option("user-link--online"))
  def lineIcon: Tag = span(cls := "line-icon")
  def userUrl(username: lila.core.userId.UserName): String = s"/@/${username.value}"
  def showDate(d: java.time.Instant): String = absoluteDateFormatter.format(d)

  object form3:
    def submit(text: Frag, icon: Option[Icon] = None): Tag = 
      button(tpe := "submit", cls := "button")(icon.fold(frag())(iconTag), text)
    def globalError(form: play.api.data.Form[?]): Frag = 
      form.globalErrors.map(e => div(cls := "error")(e.message)).headOption.getOrElse(frag())
    def group(field: play.api.data.Field, labelText: Frag, help: Option[Frag] = None)(mods: play.api.data.Field => Frag): Tag = 
      div(cls := "form-group")(label(labelText), mods(field), help)
    def input(field: play.api.data.Field, typ: String = "text")(mods: Modifier*): Tag = 
      scalatags.Text.all.input(tpe := typ, name := field.name, value := field.value.getOrElse(""), mods)
    def action(content: Frag): Tag = div(cls := "form-actions")(content)
    def actions(mods: Modifier*): Tag = div(cls := "form-actions", mods)
    def hidden(field: play.api.data.Field): Tag = 
      scalatags.Text.all.input(tpe := "hidden", name := field.name, value := field.value.getOrElse(""))
    def passwordModified(field: play.api.data.Field, labelText: Frag)(mods: Modifier*): Tag = 
      group(field, labelText)(f => input(f, "password")(mods))
    def passwordComplexityMeter(labelText: Frag): Tag = div(cls := "password-complexity-meter")(labelText)
    def checkbox(field: play.api.data.Field, labelText: Frag): Tag = 
      label(scalatags.Text.all.input(tpe := "checkbox", name := field.name, checked := field.value.contains("true")), labelText)

  extension (pk: lila.core.perf.PerfKey)
    def perfIcon: Icon = Icon.CrownElite

  extension (v: chess.variant.Variant)
    def variantTrans(): String = v.name

object Helpers extends Helpers
