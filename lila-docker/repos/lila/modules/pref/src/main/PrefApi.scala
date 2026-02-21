package lila.pref

import play.api.mvc.RequestHeader
import reactivemongo.api.bson.*
import scala.concurrent.Future
import scala.annotation.unused

import lila.db.dsl.{ *, given }

// Simplified PrefApi for analysis-only system
// Removed: getMessage, getInsightShare, getChallenge, getStudyInvite, followable, mentionable, isolate, setBot, agree
final class PrefApi(
    val coll: Coll,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  import PrefHandlers.given

  // UserDelete event subscription removed (not needed for analysis-only)

  private def fetchPref(id: UserId): Fu[Option[Pref]] = coll.find($id(id)).one[Pref]

  private val cache = cacheApi[UserId, Option[Pref]](200_000, "pref.fetchPref"):
    _.expireAfterAccess(10.minutes).buildAsyncFuture(fetchPref)

  def get(user: User): Fu[Pref] = cache
    .get(user.id)
    .dmap:
      _ | Pref.create(user)

  def get(user: User, req: RequestHeader): Fu[Pref] =
    get(user).dmap(RequestPref.queryParamOverride(req))

  def get(user: Option[User], req: RequestHeader): Fu[Pref] = user match
    case Some(u) => get(u).dmap(RequestPref.queryParamOverride(req))
    case None => fuccess(RequestPref.fromRequest(req))

  def setPref(@unused user: User, pref: Pref): Funit =
    for _ <- coll.update.one($id(pref.id), pref, upsert = true)
    yield cache.put(pref.id, fuccess(pref.some))

  def setPref(user: User, change: Pref => Pref): Funit =
    get(user).map(change).flatMap(setPref(user, _))

  def set(user: User, name: String, value: String): Funit =
    Pref
      .validatedUpdate(name, value)
      .fold[Funit](Future.failed(IllegalArgumentException(s"Invalid preference update: $name"))):
        setPref(user, _)
