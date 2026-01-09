package lila.pref

import chess.{ ByColor, Color }
import play.api.mvc.RequestHeader
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*

// Simplified PrefApi for analysis-only system
// Removed: getMessage, getInsightShare, getChallenge, getStudyInvite, followable, mentionable, isolate, setBot, agree
final class PrefApi(
    val coll: Coll,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  def agree(user: User): Funit = funit

  import PrefHandlers.given

  // UserDelete event subscription removed (not needed for analysis-only)

  private def fetchPref(id: UserId): Fu[Option[Pref]] = coll.find($id(id)).one[Pref]

  private val cache = cacheApi[UserId, Option[Pref]](200_000, "pref.fetchPref"):
    _.expireAfterAccess(10.minutes).buildAsyncFuture(fetchPref)

  export cache.get as getPrefById

  def saveTag(user: User, tag: Pref.Tag.type => String, value: Boolean) =
    for _ <-
        if value
        then coll.update.one($id(user.id), $set(s"tags.${tag(Pref.Tag)}" -> "1"), upsert = true)
        else coll.update.one($id(user.id), $unset(s"tags.${tag(Pref.Tag)}"))
    yield cache.invalidate(user.id)

  def get(user: User): Fu[Pref] = cache
    .get(user.id)
    .dmap:
      _ | Pref.create(user)

  def get[A](user: User, pref: Pref => A): Fu[A] = get(user).dmap(pref)

  def get[A](userId: UserId, pref: Pref => A): Fu[A] =
    getPrefById(userId).dmap(p => pref(p | Pref.default))

  def get(user: User, req: RequestHeader): Fu[Pref] =
    get(user).dmap(RequestPref.queryParamOverride(req))

  def get(user: Option[User], req: RequestHeader): Fu[Pref] = user match
    case Some(u) => get(u).dmap(RequestPref.queryParamOverride(req))
    case None => fuccess(RequestPref.fromRequest(req))

  def byId(userId: UserId): Fu[Pref] = cache
    .get(userId)
    .dmap:
      _ | Pref.create(userId)

  def byId(both: ByColor[Option[UserId]]): Fu[ByColor[Pref]] =
    both.traverse(_.fold(fuccess(Pref.default))(byId))

  def get(both: ByColor[Option[User]], myPov: Color, myPref: Pref): Fu[ByColor[Pref]] =
    both(!myPov)
      .so(get)
      .map: opponent =>
        myPov.fold(ByColor(myPref, opponent), ByColor(opponent, myPref))

  def setPref(user: User, pref: Pref): Funit =
    for _ <- coll.update.one($id(pref.id), pref, upsert = true)
    yield cache.put(pref.id, fuccess(pref.some))

  def setPref(user: User, change: Pref => Pref): Funit =
    get(user).map(change).flatMap(setPref(user, _))

  def set(user: User, name: String, value: String): Funit =
    setPref(user, _.set(name, value))

  def saveNewUserPrefs(user: User, req: RequestHeader): Funit =
    val reqPref = RequestPref.fromRequest(req)
    (reqPref != Pref.default).so(setPref(user, reqPref.copy(id = user.id)))

  def followable(userId: UserId): Fu[Boolean] = Future.successful(true)
