package lila.analyse

import reactivemongo.api.bson.{ BSONBoolean, BSONInteger }
import java.time.format.DateTimeFormatter
import lila.db.dsl.{ *, given }
import lila.core.lilaism.Core.{ *, given }

final class RequesterApi(coll: Coll)(using Executor):

  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(java.time.ZoneOffset.UTC)

  def add(requester: UserId, ownGame: Boolean): Funit =
    coll.update
      .one(
        $id(requester),
        $inc(
          "total" -> 1,
          formatter.format(nowInstant) -> (if ownGame then 1 else 2)
        ),
        upsert = true
      )
      .void

  def countTodayAndThisWeek(userId: UserId): Fu[(Int, Int)] =
    val now = nowInstant
    coll
      .one(
        $id(userId),
        $doc:
          (7 to 0 by -1).toList.map(now.minusDays).map(formatter.format).map(_ -> BSONBoolean(true))
      )
      .map: doc =>
        val daily = doc.flatMap(_.int(formatter.format(now)))
        val weekly = doc.fold(0): d =>
          d.values.foldLeft(0):
            case (acc, BSONInteger(v)) => acc + v
            case (acc, _) => acc
        (~daily, weekly)

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    coll.delete.one($id(del.id)).void
