package lila.strategicPuzzle

import play.api.libs.json.Json

import lila.core.userId.UserId
import lila.db.JSON
import lila.db.dsl.*
import lila.strategicPuzzle.StrategicPuzzle.*

final class ProgressRepo(
    coll: Coll
)(using Executor):

  private object F:
    val userId = "userId"
    val puzzleId = "puzzleId"
    val status = "status"
    val completedAt = "completedAt"

  def insert(attempt: AttemptDoc): Funit =
    coll.insert.one(JSON.bdoc(Json.toJsObject(attempt))).void

  def recent(userId: UserId, limit: Int = 12): Fu[List[AttemptDoc]] =
    coll
      .find($doc(F.userId -> userId.value), none[Bdoc])
      .sort($sort.desc(F.completedAt))
      .cursor[Bdoc]()
      .list(limit)
      .map(_.flatMap(readAttempt))

  def clearedPuzzleIds(userId: UserId, limit: Int = 4000): Fu[List[String]] =
    recent(userId, limit).map(_.collect { case attempt if attempt.status == StatusFull => attempt.puzzleId }.distinct)

  def hasCleared(userId: UserId, puzzleId: String): Fu[Boolean] =
    coll.exists($doc(F.userId -> userId.value, F.puzzleId -> puzzleId, F.status -> StatusFull))

  def currentStreak(userId: UserId, limit: Int = 200): Fu[Int] =
    recent(userId, limit).map(computeStreak)

  private[strategicPuzzle] def readAttempt(doc: Bdoc): Option[AttemptDoc] =
    Json.fromJson[AttemptDoc](JSON.jval(doc)).asOpt
