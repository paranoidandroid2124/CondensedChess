package lila.user

import reactivemongo.api.bson.*

import lila.core.user.User
import lila.core.LightUser
import lila.core.userId.*
import lila.db.dsl.{ *, given }
import lila.memo.{ CacheApi, Syncache }

import BSONFields as F

final class LightUserApi(repo: UserRepo, cacheApi: CacheApi)(using Executor)
    extends lila.core.user.LightUserApi:

  val async = LightUser.Getter: id =>
    if id.isGhost then fuccess(LightUser.ghost.some) else cache.async(id)

  val sync = LightUser.GetterSync: id =>
    if id.isGhost then LightUser.ghost.some else cache.sync(id)


  def invalidate(id: UserId): Unit = cache.invalidate(id)

  private val cache: Syncache[UserId, Option[LightUser]] = cacheApi.sync[UserId, Option[LightUser]](
    name = "user.light",
    initialCapacity = 512 * 1024,
    compute = id =>
      if id.isGhost then fuccess(LightUser.ghost.some)
      else
        repo.coll
          .find($id(id), projection)
          .one[LightUser]
          .recover:
            case _: reactivemongo.api.bson.exceptions.BSONValueNotFoundException => LightUser.ghost.some
    ,
    default = id => LightUser.ghost.some,
    strategy = Syncache.Strategy.WaitAfterUptime(10.millis),
    expireAfter = Syncache.ExpireAfter.Write(15.minutes)
  )

  private given BSONDocumentReader[LightUser] with
    def readDocument(doc: BSONDocument) =
      doc
        .getAsTry[UserName](F.username)
        .map: name =>
          LightUser(
            id = UserId(name.value.toLowerCase),
            name = name
          )

  private val projection =
    $doc(
      "_id" -> false,
      F.username -> true
    ).some
