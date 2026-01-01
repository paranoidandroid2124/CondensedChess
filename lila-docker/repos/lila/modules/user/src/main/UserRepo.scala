package lila.user

import reactivemongo.api.bson.*
import lila.db.dsl.*
import lila.core.user.*
import lila.core.userId.*

final class UserRepo(val coll: Coll)(using ec: Executor) extends lila.core.user.UserRepo:

  import BSONHandlers.given

  override def byId[U: UserIdOf](u: U): Fu[Option[User]] =
    coll.byId(u.id)

  def byIds[U: UserIdOf](us: Iterable[U]): Fu[List[User]] =
    coll.byIds(us.map(_.id))

  def me(id: UserId): Fu[Option[Me]] =
    byId(id).map(_.map(Me.apply))

  def isEnabled(id: UserId): Fu[Boolean] =
    coll.exists($doc("_id" -> id, BSONFields.enabled -> true))

  def userIdsWithRoles(roles: List[RoleDbKey]): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, List]("_id", $doc(BSONFields.roles $in roles)).map(_.toSet)

  def filterExists(ids: Set[UserId]): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, List]("_id", $doc("_id" $in ids)).map(_.toSet)

  def exists(id: UserId): Fu[Boolean] = coll.exists($id(id))

  def autocomplete(term: String): Fu[List[String]] =
    coll
      .find($doc(BSONFields.username $regex s"^$term"), $doc(BSONFields.username -> true).some)
      .cursor[BSONDocument]()
      .collect[List](10, reactivemongo.api.Cursor.FailOnError[List[BSONDocument]]())
      .map(_.flatMap(_.getAsOpt[String](BSONFields.username)))
