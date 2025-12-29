package lila.user

import reactivemongo.api.bson.*
import lila.core.user.{ User, UserEnabled, RoleDbKey }
import lila.core.userId.*
import lila.db.dsl.{ *, given }

final class UserRepo(val coll: Coll)(using ec: Executor):

  import lila.user.BSONHandlers.given

  def byId[U: UserIdOf](u: U): Fu[Option[User]] =
    u.id.noGhost.so(coll.byId[User](u.id))

  def byIds[U: UserIdOf](us: Iterable[U]): Fu[List[User]] =
    val ids = us.map(_.id).filter(_.noGhost)
    ids.nonEmpty.so(coll.byIds[User, UserId](ids))

  def me(id: UserId): Fu[Option[lila.core.user.Me]] = byId(id).map(_.map(lila.core.user.Me(_)))

  def isEnabled(id: UserId): Fu[Boolean] =
    id.noGhost.so(coll.exists($doc("enabled" -> true) ++ $id(id)))

  def userIdsWithRoles(roles: List[RoleDbKey]): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set]("_id", $doc("roles".$in(roles)))

  def filterExists(ids: Set[UserId]): Fu[List[UserId]] =
    coll.primitive[UserId]($inIds(ids), "_id")

  def countRecentByCreatedAt(since: java.time.Instant): Fu[Int] =
    coll.countSel($doc("createdAt".$gt(since)))

  def create(
      name: UserName,
      passwordHash: lila.core.security.HashedPassword,
      email: lila.core.email.EmailAddress
  ): Fu[Unit] =
    val now = java.time.Instant.now()
    val doc = $doc(
      "_id" -> name.id.value,
      "username" -> name.value,
      "email" -> email.value,
      "bpass" -> passwordHash.bytes,
      "enabled" -> true,
      "roles" -> List.empty[String],
      "createdAt" -> now,
      "seenAt" -> now
    )
    coll.insert.one(doc).void
