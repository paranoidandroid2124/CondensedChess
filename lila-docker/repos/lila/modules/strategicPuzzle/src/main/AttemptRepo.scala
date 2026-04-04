package lila.strategicPuzzle

import play.api.libs.json.Json
import reactivemongo.api.indexes.{ Index, IndexType }

import lila.core.userId.UserId
import lila.db.JSON
import lila.db.dsl.*
import lila.strategicPuzzle.StrategicPuzzle.*

final class AttemptRepo(
    coll: Coll
)(using Executor):

  import lila.db.recoverDuplicateKey

  private object F:
    val userId = "userId"
    val puzzleId = "puzzleId"
    val status = "status"
    val completedAt = "completedAt"

  ensureIndexes()

  def insert(attempt: AttemptDoc): Fu[Boolean] =
    coll
      .insert
      .one(JSON.bdoc(Json.toJsObject(attempt)))
      .map(_ => true)
      .recover(recoverDuplicateKey(_ => false))

  def recent(userId: UserId, limit: Int = 12): Fu[List[AttemptDoc]] =
    coll
      .find($doc(F.userId -> userId.value), none[Bdoc])
      .sort($sort.desc(F.completedAt))
      .cursor[Bdoc]()
      .list(limit)
      .map(_.flatMap(readAttempt))

  def all(userId: UserId): Fu[List[AttemptDoc]] =
    coll
      .find($doc(F.userId -> userId.value), none[Bdoc])
      .sort($sort.desc(F.completedAt))
      .cursor[Bdoc]()
      .listAll()
      .map(_.flatMap(readAttempt))

  def distinctUserIds(): Fu[List[UserId]] =
    coll
      .distinctEasy[String, List](F.userId, $empty)
      .map(_.map(UserId(_)))

  private[strategicPuzzle] def readAttempt(doc: Bdoc): Option[AttemptDoc] =
    Json.fromJson[AttemptDoc](JSON.jval(doc)).asOpt

  private def ensureIndexes(): Unit =
    coll.indexesManager.ensure(
      Index(
        key = Seq(F.userId -> IndexType.Ascending, F.completedAt -> IndexType.Descending),
        name = Some("user_completed")
      )
    )
    coll.indexesManager.ensure(
      Index(
        key = Seq(F.userId -> IndexType.Ascending, F.puzzleId -> IndexType.Ascending),
        name = Some("user_puzzle_full_unique"),
        unique = true,
        options = $doc(
          "partialFilterExpression" -> $doc(
            F.status -> StatusFull
          )
        )
      )
    )
