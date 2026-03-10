package lila.user

import java.time.Instant
import scala.util.Try

import reactivemongo.api.bson.*

import lila.core.user.UserDelete
import lila.core.userId.UserId
import lila.db.dsl.{ *, given }

final class DeleteRequestRepo(coll: Coll)(using Executor):

  import lila.user.BSONHandlers.given

  private given BSONDocumentReader[UserDelete] = BSONDocumentReader.from[UserDelete]: doc =>
    for
      id <- doc.getAsTry[UserId]("_id")
      requested <- doc.getAsTry[Instant]("requested")
      done = doc.getAsOpt[Boolean]("done").getOrElse(false)
    yield UserDelete(id, requested, done)

  private given BSONDocumentWriter[UserDelete] = BSONDocumentWriter.from[UserDelete]: request =>
    Try:
      BSONDocument(
        "_id" -> request.id,
        "requested" -> request.requested,
        "done" -> request.done
      )

  def byId(id: UserId): Fu[Option[UserDelete]] =
    coll.one[UserDelete]($id(id))

  def isPending(id: UserId): Fu[Boolean] =
    coll.exists($doc("_id" -> id, "done" -> false))

  def schedule(id: UserId, requestedAt: Instant = Instant.now()): Funit =
    coll.update.one($id(id), UserDelete(id, requestedAt, done = false), upsert = true).void

  def markDone(id: UserId): Funit =
    coll.update.one($id(id), $set("done" -> true)).void
