package lila.core.user

import scalalib.model.Days
import scala.annotation.unused
import lila.core.lilaism.Core.*
import lila.core.userId.*
import lila.core.email.EmailAddress
import lila.core.LightUser
import play.api.libs.json.JsObject

trait Note
trait NoteApi:
  def getForMyPermissions(user: User, max: Max = Max(30))(using me: Me): Fu[List[Note]]
  def toUserForMod(id: UserId, max: Max = Max(50)): Fu[List[Note]]
  def recentToUserForMod(id: UserId): Fu[Option[Note]]
  def byUsersForMod(ids: List[UserId]): Fu[List[Note]]
  def write(to: UserId, text: String, modOnly: Boolean, dox: Boolean)(using me: MyId): Funit
  def lichessWrite(to: User, text: String): Funit
  def byId(id: String): Fu[Option[Note]]
  def delete(id: String): Funit
  def setDox(id: String, dox: Boolean): Funit
  // def search(query: String, page: Int, withDox: Boolean): Fu[scalalib.paginator.Paginator[Note]]

trait JsonView:
  def full(u: User): JsObject
  def disabled(u: LightUser): JsObject
  def ghost: JsObject

opaque type UserEnabled = Boolean
object UserEnabled extends TotalWrapper[UserEnabled, Boolean]:
  extension (e: UserEnabled)
    def yes: Boolean = e.value
    def no: Boolean = !e.value

opaque type KidMode = Boolean
object KidMode extends TotalWrapper[KidMode, Boolean]:
  extension (k: KidMode)
    def yes: Boolean = k.value
    def no: Boolean = !k.value

case class UserDelete(id: UserId, requested: Instant, done: Boolean = false)
case class TotpToken(value: String) extends AnyVal

opaque type RoleDbKey = String
object RoleDbKey extends TotalWrapper[RoleDbKey, String]

opaque type TotpSecret = Array[Byte]
object TotpSecret extends TotalWrapper[TotpSecret, Array[Byte]]:
  extension (s: TotpSecret)
    def verify(token: TotpToken): Boolean = false

opaque type FlagCode = String
object FlagCode extends TotalWrapper[FlagCode, String]:
  def from(codes: Iterable[String]): List[FlagCode] = codes.toList.map(FlagCode.apply)

type FlagName = String

case class Flag(code: FlagCode, name: FlagName, shortName: Option[String]):
  override def toString = code.value
trait FlagApi

case class Marks(troll: Boolean = false, engine: Boolean = false, boost: Boolean = false, alt: Boolean = false, reportban: Boolean = false)

case class NbGames(
    all: Int = 0,
    rated: Int = 0,
    playing: Int = 0,
    draw: Int = 0,
    loss: Int = 0,
    win: Int = 0,
    imported: Int = 0,
    ai: Int = 0,
    correspondence: Int = 0,
    crosstable: Option[Int] = None
)

case class User(
    id: UserId,
    username: UserName,
    enabled: UserEnabled,
    roles: List[RoleDbKey],
    createdAt: Instant,
    seenAt: Option[Instant] = None,
    totpSecret: Option[TotpSecret] = None,
    email: Option[EmailAddress] = None,
    tier: UserTier = UserTier.Free,
    expiresAt: Option[Instant] = None
):

  def titleUsername: String = username.value
  def marks = Marks()

  def light = LightUser(id, username)
  
  def myId: MyId = MyId(id.value)
  def lightMe: LightUser.Me = LightUser.Me(light)
  def is[U: UserIdOf](u: U): Boolean = id == summon[UserIdOf[U]](u)
  def isnt[U: UserIdOf](u: U): Boolean = !is(u)


object User:
  given UserIdOf[User] = _.id


trait UserApi:
  def byId[U: UserIdOf](u: U): Fu[Option[lila.core.user.User]]
  def enabledById[U: UserIdOf](u: U): Fu[Option[lila.core.user.User]]
  def byIds[U: UserIdOf](us: Iterable[U]): Fu[List[lila.core.user.User]]
  def me(id: UserId): Fu[Option[Me]]
  def email(id: UserId): Fu[Option[EmailAddress]]
  def isEnabled(id: UserId): Fu[Boolean]
  def userIdsWithRoles(roles: List[RoleDbKey]): Fu[Set[UserId]]
  def disable(id: UserId): Funit
  def delete(id: UserId): Funit
  def updateUsername(id: UserId, newName: UserName): Funit
  def isCreatedSince(id: UserId, since: Instant): Fu[Boolean]
  def accountAge(id: UserId): Fu[Days]

trait LightUserApi:
  val async: LightUser.Getter
  val sync: LightUser.GetterSync
  val fallback: LightUser.GetterSyncFallback
  def invalidate(id: UserId): Unit
  def preloadMany(@unused _ids: Seq[UserId]): Fu[Unit] = scala.concurrent.Future.unit

abstract class UserRepo:
  def byId[U: UserIdOf](u: U): Fu[Option[lila.core.user.User]]
  def autocomplete(term: String): Fu[List[String]]

object BSONFields:
  val enabled = "enabled"
  val roles = "roles"
  val username = "username"
  val createdAt = "createdAt"
  val seenAt = "seenAt"
  val bpass = "bpass"
  val salt = "salt"
  val sha512 = "sha512"
  val email = "email"
  val tier = "tier"
  val expiresAt = "expiresAt"
  val totpSecret = "totp"

/* User who is currently logged in */
opaque type Me <: lila.core.user.User = lila.core.user.User
object Me extends TotalWrapper[Me, lila.core.user.User]:
  given Conversion[Me, UserId] = _.id
  extension (me: Me)
    def userId: UserId = me.id
