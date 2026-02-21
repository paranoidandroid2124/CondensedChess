package lila.ui

import play.api.mvc.RequestHeader
import scalalib.model.Language

import lila.core.net.IpAddress
import lila.core.pref.Pref

/* Data available in every HTTP request */
trait Context:
  val req: RequestHeader
  def isAuth: Boolean
  def isTokenAuth: Boolean
  def me: Option[Me]
  def user: Option[User]
  def userId: Option[UserId]
  def pref: Pref
  def ip: IpAddress
  def blind: Boolean
  def troll: Boolean
  def isBot: Boolean

  def is[U: UserIdOf](u: U): Boolean = me.exists(_.is(u))
  def isnt[U: UserIdOf](u: U): Boolean = !is(u)
  def myId: Option[MyId] = me.map(_.myId)
  def noBlind = !blind
  def flash(name: String): Option[String] = req.flash.get(name)
  def speechSynthesis = pref.hasSpeech || blind
  inline def noBot = !isBot
  lazy val acceptLanguages: Set[Language] = Set(Language("en"))

import lila.core.user.Me
object Context:
  given ctxMe(using ctx: Context): Option[Me] = ctx.me

/* data necessary to render the lichess website layout */
trait PageContext extends Context:
  val me: Option[Me]
  val needsFp: Boolean
  val impersonatedBy: Option[lila.core.userId.ModId]
  def teamNbRequests: Int
  def hasInquiry: Boolean
  def nonce: Option[Nonce]
  def error: Boolean
