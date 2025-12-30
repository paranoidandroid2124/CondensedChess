package lila.ui

import play.api.i18n.Lang
import java.time.Month
import java.time.format.TextStyle
import scalatags.Text.all.*

// Minimal helpers trait for UI components
trait Helpers:
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

object Helpers extends Helpers
