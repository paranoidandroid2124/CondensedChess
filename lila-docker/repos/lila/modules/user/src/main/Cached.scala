package lila.user

import reactivemongo.api.bson.*
import lila.core.userId.UserSearch
import lila.db.dsl.*

final class Cached(
    userRepo: UserRepo,
    userApi: lila.core.user.UserApi,
    onlineUserIds: lila.core.socket.OnlineIds,
    mongoCache: lila.memo.MongoCache.Api,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler):

  import BSONHandlers.given

  private val onlineBotIdsCache = cacheApi.unit[Set[UserId]]:
    _.refreshAfterWrite(5.minutes).buildAsyncTimeout()(_ => userRepo.isBot(UserId("irwin")).map(_ => Set.empty)) // Minimal bot logic for now

  def getBotIds: Fu[Set[UserId]] = onlineBotIdsCache.getUnit

  private def userIdsLikeFetch(text: UserSearch) =
    userRepo.filterExists(Set(UserId(text.value.toLowerCase))).map(_.toList)

  private val userIdsLikeCache = cacheApi[UserSearch, List[UserId]](1024, "user.like"):
    _.expireAfterWrite(5.minutes).buildAsyncTimeout()(userIdsLikeFetch)

  def userIdsLike(text: UserSearch): Fu[List[UserId]] =
    if text.value.lengthIs < 5 then userIdsLikeCache.get(text)
    else userIdsLikeFetch(text)

  def getTop50Online: Fu[List[lila.core.user.User]] =
    userApi.byIds(onlineUserIds.exec()).map(_.take(50))
