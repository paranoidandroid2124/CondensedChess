package lila.core

import java.time.Instant
import scalalib.model.Days
import lila.core.userId.*
import lila.core.email.EmailAddress

object user:

  opaque type UserEnabled = Boolean
  object UserEnabled extends TotalWrapper[UserEnabled, Boolean]:
    extension (e: UserEnabled)
      def yes: Boolean = e.value

  case class UserDelete(requested: Instant, done: Boolean = false)

  opaque type RoleDbKey = String
  object RoleDbKey extends TotalWrapper[RoleDbKey, String]

  case class User(
      id: UserId,
      username: UserName,
      enabled: UserEnabled,
      roles: List[RoleDbKey],
      createdAt: Instant,
      seenAt: Option[Instant] = None,
      lang: Option[String] = None
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

  /* User who is currently logged in */
  opaque type Me = lila.core.user.User
  object Me extends TotalWrapper[Me, lila.core.user.User]:
    given UserIdOf[Me] = _.id
    given Conversion[Me, lila.core.user.User] = identity
    given Conversion[Me, UserId] = _.id
    extension (me: Me)
      def myId: MyId = MyId(me.id.value)
      def lightMe: LightUser.Me = LightUser.Me(me.light)
