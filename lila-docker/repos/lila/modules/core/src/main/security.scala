package lila.core
package security

import play.api.data.{ Form, Mapping }
import play.api.mvc.RequestHeader

import lila.core.email.EmailAddress
import lila.core.net.{ ApiVersion }
import lila.core.user.{ User }
import lila.core.userId.{ UserId, UserName }

case class GarbageCollect(userId: UserId)
case class CloseAccount(userId: UserId)
case class ReopenAccount(user: User)

trait LilaCookie:
  import play.api.mvc.*
  def cookie(
      name: String,
      value: String,
      maxAge: Option[Int] = None,
      httpOnly: Option[Boolean] = None
  ): Cookie

object LilaCookie:
  val sessionId = "sid"
  val noRemember = "noRemember"
  def sid(req: RequestHeader): Option[String] = req.session.get(sessionId)

case class ClearPassword(value: String) extends AnyVal:
  override def toString = "ClearPassword(****)"

case class HashedPassword(bytes: Array[Byte])

trait Authenticator:
  def passEnc(p: ClearPassword): HashedPassword
  def setPassword(id: UserId, p: ClearPassword): Funit

trait SignupForm:
  val emailField: Mapping[EmailAddress]
  val username: Mapping[UserName]

case class UserSignup(
    user: User,
    email: EmailAddress,
    req: RequestHeader,
    apiVersion: Option[ApiVersion]
)

trait SecurityApi:
  def shareAnIpOrFp(u1: UserId, u2: UserId): Fu[Boolean]
