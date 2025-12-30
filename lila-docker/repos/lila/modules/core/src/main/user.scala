package lila.core

import scalalib.model.{ Days, Max }
import lila.core.userId.*
import lila.core.email.EmailAddress
import play.api.libs.json.JsObject

object user:

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

  case class UserDelete(requested: Instant, done: Boolean = false)
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
    override def toString = code
  trait FlagApi

  case class User(
      id: UserId,
      username: UserName,
      enabled: UserEnabled,
      roles: List[RoleDbKey],
      createdAt: Instant,
      seenAt: Option[Instant] = None,
      lang: Option[String] = None,
      totpSecret: Option[TotpSecret] = None,
      email: Option[EmailAddress] = None
  ):

    def light = LightUser(id, username)
    
    def myId: MyId = MyId(id.value)
    def lightMe: LightUser.Me = LightUser.Me(light)


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
    def isCreatedSince(id: UserId, since: Instant): Fu[Boolean]
    def accountAge(id: UserId): Fu[Days]

  trait LightUserApi:
    val async: LightUser.Getter
    val sync: LightUser.GetterSync
    def invalidate(id: UserId): Unit

  abstract class UserRepo:
    def byId[U: UserIdOf](u: U): Fu[Option[lila.core.user.User]]

  object BSONFields:
    val enabled = "enabled"
    val roles = "roles"
    val username = "username"
    val createdAt = "createdAt"
    val seenAt = "seenAt"
    val bpass = "bpass"
    val salt = "salt"
    val sha512 = "sha512"

  /* User who is currently logged in */
  opaque type Me = lila.core.user.User
  object Me extends TotalWrapper[Me, lila.core.user.User]:
    given UserIdOf[Me] = _.id
    given Conversion[Me, lila.core.user.User] = identity
    given Conversion[Me, UserId] = _.id
    extension (me: Me)
      def userId: UserId = me.id
      def myId: MyId = MyId(me.id.value)
      def lightMe: LightUser.Me = LightUser.Me(me.light)
