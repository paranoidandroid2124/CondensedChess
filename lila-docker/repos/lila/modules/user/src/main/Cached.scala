package lila.user

import reactivemongo.api.bson.*
import lila.core.userId.UserSearch
import lila.db.dsl.*

final class Cached(
    userRepo: UserRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler):

  // Removed onlineBotIdsCache to drop socket dependency
  // Removed onlineUserIds dependency

  def getBotIds: Fu[Set[UserId]] = fuccess(Set.empty)

  private def userIdsLikeFetch(text: UserSearch) =
    userRepo.filterExists(Set(UserId(text.value.toLowerCase))).map(_.toList)

  def userIdsLike(text: UserSearch): Fu[List[UserId]] =
    userIdsLikeFetch(text)

  def getTop50Online: Fu[List[lila.core.user.User]] =
    fuccess(Nil) // Feature removed
