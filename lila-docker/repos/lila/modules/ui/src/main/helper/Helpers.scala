package lila.ui

import play.api.i18n.Lang
import java.time.Month
import java.time.format.TextStyle
import scalatags.Text.all.*

// Minimal helpers trait for UI components
trait Helpers:
  def iconTag(icon: Icon): Tag = i(attr("data-icon") := icon.value)
  def iconTag(icon: Icon, text: Frag): Tag = i(attr("data-icon") := icon.value, cls := "text")(text)
  def showMonth(m: Month)(using lang: Lang): String =
    m.getDisplayName(TextStyle.FULL, lang.locale)

  // UI Helper Stubs
  def momentFromNowWithPreload(d: java.time.Instant): Frag = frag(d.toString)
  def chessgroundMini(fen: String | chess.format.BoardFen, color: chess.Color, lastMove: Option[chess.format.Uci]): Tag => Tag = t => t
  def cgWrapContent: Frag = raw("")
  def gameLink(g: lila.core.game.Game, color: chess.Color, ownerLink: Boolean = false, tv: Boolean = false): String = s"/${g.id}"
  def variantLink(v: chess.variant.Variant, perf: Any = None): Tag = span(v.name)
  def playerUsername(light: Any, user: Option[Any], withRating: Boolean): Tag = span("User")
  def userIdLink(userId: Option[lila.core.userId.UserId], withOnline: Boolean = true, klass: Option[String] = None): Tag = 
    a(href := userId.fold("#")(id => s"/@/${id.value}"))(userId.fold("Anonymous")(_.value))
  def showRatingDiff(diff: Int): Frag = frag(s"${if (diff > 0) "+" else ""}$diff")
  def aiNameFrag(level: Int): Frag = frag(s"AI level $level")
  def berserkIconSpan: Tag = span(cls := "berserk")("B")
  def ratedName(rated: Boolean): String = if (rated) "Rated" else "Casual"
  def esmInitBit(name: String): Esm = Esm(name)
  def routeUrl(call: play.api.mvc.Call): String = call.url
  def standardFlash: Frag = raw("")
  def analyseNvuiTag: Esm = Esm("analyse.nvui")

  // Missing helpers added for stabilization
  def submitButton(mods: Modifier*): Tag = button(tpe := "submit", mods)
  def postForm(mods: Modifier*): Tag = form(method := "post", mods)
  def copyMeInput(v: String): Tag = input(cls := "copyable", value := v, readonly)
  def copyMeLink(url: String, text: String): Tag = a(href := url, cls := "copyable")(text)
  def copyMeContent(url: String, text: String): Tag = a(href := url, cls := "copyable-content")(text)
  def cdnUrl(url: String): String = url
  def pathUrl(url: String): String = url
  def fenThumbnailUrl(fen: String, color: Option[chess.Color], variant: chess.variant.Variant): String = 
    s"/fen/${fen.replace("/", "_")}.png"

  def momentFromNow(d: java.time.Instant): Frag = frag(d.toString)
  def titleNameOrId(userId: lila.core.userId.UserId): Frag = frag(userId.value)
  def pagerNext(pager: scalalib.paginator.Paginator[?], url: Int => String): Frag = raw("")
  def iconFlair(flair: String): Tag = iconTag(Icon.StudyBoard)
  def addQueryParam(url: String, name: String, value: String): String = 
    val separator = if (url.contains("?")) "&" else "?"
    s"$url$separator$name=$value"
  
  def chessgroundBoard: Tag = div(cls := "cg-wrap")(raw(""))
  def playerText(p: lila.core.game.Player): Tag = span(p.userId.fold("Anonymous")(_.value))

  def globalErrorNamed(form: play.api.data.Form[?], name: String): Frag = 
    form.globalError.filter(_.message == name).map(e => div(cls := "error")(e.message)).getOrElse(frag())
  def errMsg(field: play.api.data.Field): Frag = 
    field.errors.map(e => div(cls := "error")(e.message)).headOption.getOrElse(frag())
  
  def langHref(url: play.api.mvc.Call)(using Context): String = url.url
  def userLink(user: Any, withOnline: Boolean = true): Tag = span("User")
  def fingerprintTag: Frag = raw("")
  def hcaptchaScript(form: Any): Frag = raw("")

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
