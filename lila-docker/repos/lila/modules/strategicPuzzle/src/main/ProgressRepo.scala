package lila.strategicPuzzle

import play.api.libs.json.Json

import lila.core.userId.UserId
import lila.db.JSON
import lila.db.dsl.*
import lila.strategicPuzzle.StrategicPuzzle.*

final class ProgressRepo(
    coll: Coll
)(using Executor):

  def byUser(userId: UserId): Fu[Option[ProgressDoc]] =
    coll
      .find($id(userId.value))
      .one[Bdoc]
      .map(_.flatMap(readProgress))

  def upsert(progress: ProgressDoc): Funit =
    coll
      .update
      .one(
        $id(progress._id),
        JSON.bdoc(Json.toJsObject(progress)),
        upsert = true
      )
      .void

  private[strategicPuzzle] def readProgress(doc: Bdoc): Option[ProgressDoc] =
    Json.fromJson[ProgressDoc](JSON.jval(doc)).asOpt
