package lila.user

import reactivemongo.api.bson.*
import lila.db.dsl.*
import lila.core.email.EmailAddress
import lila.core.user.*
import lila.core.userId.*

import java.time.Instant
import scala.concurrent.Future

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

  def disable(id: UserId): Funit =
    coll.update.one($id(id), $set(BSONFields.enabled -> false)).void

  def delete(id: UserId): Funit =
    coll.delete.one($id(id)).void

  def updateUsername(id: UserId, newName: UserName): Funit =
    coll.update.one($id(id), $set(BSONFields.username -> newName)).void

  def autocomplete(term: String): Fu[List[String]] =
    coll
      .find($doc(BSONFields.username $regex s"^$term"), $doc(BSONFields.username -> true).some)
      .cursor[BSONDocument]()
      .collect[List](10, reactivemongo.api.Cursor.FailOnError[List[BSONDocument]]())
      .map(_.flatMap(_.getAsOpt[String](BSONFields.username)))

  def byEmail(email: EmailAddress): Fu[Option[User]] =
    coll.one[User]($doc(BSONFields.email -> EmailAddress(email.normalize.value)))

  def upsertEmailUser(email: EmailAddress): Fu[User] =
    val normalized = EmailAddress(email.normalize.value)
    byEmail(normalized).flatMap:
      case Some(u) => fuccess(u)
      case None =>
        createEmailUser(normalized).recoverWith: _ =>
          byEmail(normalized).flatMap:
            case Some(u) => fuccess(u)
            case None    => Future.failed(Exception("User creation failed"))

  private def createEmailUser(email: EmailAddress): Fu[User] =
    val base = usernameBaseFromEmail(email)
    def attempt(n: Int): Fu[User] =
      val suffix = if n == 0 then "" else "-" + scalalib.SecureRandom.nextString(4).toLowerCase
      val candidate = (base + suffix).take(30).dropWhile(_ == '-').stripSuffix("-")
      val value = if candidate.sizeIs >= 2 then candidate else s"user-${scalalib.SecureRandom.nextString(6).toLowerCase}"
      val username = UserName(value)
      val user = User(
        id = username.id,
        username = username,
        enabled = UserEnabled(true),
        roles = Nil,
        createdAt = Instant.now(),
        email = email.some
      )
      val doc = lila.user.BSONHandlers.userBSONWriter.writeTry(user).get
      coll.insert.one(doc).map(_ => user).recoverWith:
        case e if isDuplicateKey(e) && n < 8 => attempt(n + 1)
    attempt(0)

  private def usernameBaseFromEmail(email: EmailAddress): String =
    val local = email.username
    val cleaned = local.map:
      case c if c.isLetterOrDigit => c
      case '_'                    => '_'
      case '-'                    => '-'
      case _                      => '-'
    val collapsed = cleaned.replaceAll("-{2,}", "-").stripPrefix("-").stripSuffix("-")
    val base = if collapsed.sizeIs >= 2 then collapsed else "user"
    base.take(30)

  private def isDuplicateKey(e: Throwable): Boolean =
    Option(e.getMessage).exists(_.contains("E11000"))

