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

object Helpers extends Helpers
