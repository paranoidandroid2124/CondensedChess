package lila.user

import reactivemongo.api.bson.*
import lila.db.dsl.{ *, given }
import lila.core.user.{ User, Me, Note }
import scalalib.model.Max

// Simplified NoteApi for Analysis Only mode
// Bus subscriptions and complex mod features removed
final class NoteApi(coll: Coll)(using Executor) extends lila.core.user.NoteApi:

  case class NoteImpl(
    _id: String,
    from: String,
    to: String,
    text: String,
    mod: Boolean,
    dox: Boolean,
    date: java.time.Instant
  )

  private given BSONDocumentHandler[NoteImpl] = Macros.handler[NoteImpl]

  def getForMyPermissions(user: User, max: Max = Max(30))(using me: Me): Fu[List[Note]] =
    fuccess(Nil) // Simplified

  def toUserForMod(id: UserId, max: Max = Max(50)): Fu[List[Note]] =
    fuccess(Nil)

  def recentToUserForMod(id: UserId): Fu[Option[Note]] =
    fuccess(None)

  def byUsersForMod(ids: List[UserId]): Fu[List[Note]] =
    fuccess(Nil)

  def write(to: UserId, text: String, modOnly: Boolean, dox: Boolean)(using me: MyId): Funit =
    fuccess(()) // Disabled

  def lichessWrite(to: User, text: String): Funit =
    fuccess(())

  def byId(id: String): Fu[Option[Note]] =
    fuccess(None)

  def delete(id: String): Funit =
    fuccess(())

  def setDox(id: String, dox: Boolean): Funit =
    fuccess(())
