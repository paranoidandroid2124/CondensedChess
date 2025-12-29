package lila.user

import reactivemongo.api.bson.*
import lila.core.user.{ User, UserEnabled, RoleDbKey }
import lila.db.dsl.{ *, given }

object BSONFields:
  val id = "_id"
  val username = "username"
  val enabled = "enabled"
  val roles = "roles"
  val createdAt = "createdAt"
  val seenAt = "seenAt"
  val lang = "lang"
  val title = "title"

object BSONHandlers:


  given userHandler: BSONDocumentHandler[User] = new BSON[User]:
    import BSONFields.*

    def reads(r: BSON.Reader): User =
      new User(
        id = r.get[UserId](id),
        username = r.get[UserName](username),
        enabled = r.get[UserEnabled](enabled),
        roles = ~r.getO[List[RoleDbKey]](roles),
        createdAt = r.date(createdAt),
        seenAt = r.dateO(seenAt),
        lang = r.getO[String](lang)
      )

    def writes(w: BSON.Writer, o: User) =
      BSONDocument(
        id -> o.id,
        username -> o.username,
        enabled -> o.enabled,
        roles -> o.roles.some.filter(_.nonEmpty),
        createdAt -> o.createdAt,
        seenAt -> o.seenAt,
        lang -> o.lang
      )

  private[user] given BSONDocumentHandler[lila.user.LightCount] = Macros.handler
